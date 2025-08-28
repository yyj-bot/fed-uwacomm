package com.feduwacomm.service;

import com.feduwacomm.dto.ProtocolAck;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.TrainingDatasetMapper;
import com.feduwacomm.mapper.TrainingDatasetRowMapper;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.mapper.VmRoundModelsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketProtocolService {

    private final SimpMessagingTemplate messagingTemplate;

    private final TrainingDatasetMapper trainingDatasetMapper;
    private final TrainingDatasetRowMapper trainingDatasetRowMapper;
    private final FederatedTasksMapper federatedTasksMapper;
    private final VmRoundModelsMapper vmRoundModelsMapper;
    private final VmInstancesMapper vmInstancesMapper;
    private final ObjectMapper objectMapper;

    // in-memory VM 最新状态缓存：vmId -> STATUS_RESPONSE.data（用于快速读，不作为数据源）
    private final ConcurrentHashMap<String, Map<String, Object>> statusCache = new ConcurrentHashMap<>();

    public WebSocketProtocolService(SimpMessagingTemplate messagingTemplate,
                                    TrainingDatasetMapper trainingDatasetMapper,
                                    TrainingDatasetRowMapper trainingDatasetRowMapper,
                                    FederatedTasksMapper federatedTasksMapper,
                                    VmRoundModelsMapper vmRoundModelsMapper,
                                    VmInstancesMapper vmInstancesMapper,
                                    ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.trainingDatasetMapper = trainingDatasetMapper;
        this.trainingDatasetRowMapper = trainingDatasetRowMapper;
        this.federatedTasksMapper = federatedTasksMapper;
        this.vmRoundModelsMapper = vmRoundModelsMapper;
        this.vmInstancesMapper = vmInstancesMapper;
        this.objectMapper = objectMapper;
    }

    public ProtocolAck handle(ProtocolMessage msg) {
        if (msg == null || msg.getType() == null) {
            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                    "errorCode", "INVALID_MESSAGE",
                    "errorMessage", "缺少消息或类型"));
        }
        switch (msg.getType()) {
            case CONNECT:
                return onConnect(msg);
            case HEARTBEAT:
                return onHeartbeat(msg);
            case DATASET_CREATE:
                return onDatasetCreate(msg);
            case DATASET_APPEND_ROWS:
                return onDatasetAppendRows(msg);
            case DATASET_COMPLETE:
                return onDatasetComplete(msg);
            case DATASET_STATUS_QUERY:
                return onDatasetStatusQuery(msg);
            case DATASET_DELETE:
                return onDatasetDelete(msg);
            case VM_START:
                return onVmStart(msg);
            case VM_STOP:
                return onVmStop(msg);
            case TRAINING_START:
                return onTrainingStart(msg);
            case TRAINING_STOP:
                return onTrainingStop(msg);
            case TRAINING_PROGRESS:
                return onTrainingProgress(msg);
            case MODEL_UPLOAD:
                return onModelUpload(msg);
            case MODEL_DOWNLOAD:
                return onModelDownload(msg);
            case STATUS_QUERY:
                return onStatusQuery(msg);
            case STATUS_RESPONSE:
                return onStatusResponse(msg);
            case ERROR:
            case CONNECTION_ERROR:
            case MESSAGE_ERROR:
            case STATUS_QUERY_ERROR:
                return onErrorMessage(msg);
            default:
                return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                        "errorCode", "UNSUPPORTED_TYPE",
                        "errorMessage", "不支持的消息类型: " + msg.getType()));
        }
    }

    private ProtocolAck onConnect(ProtocolMessage msg) {
        Map<String, Object> data = mapOf(
                "sessionId", "session-" + UUID.randomUUID(),
                "serverTime", Instant.now().toString(),
                "heartbeatInterval", 30,
                "maxMessageSize", 10 * 1024 * 1024,
                "supportedFeatures", new String[]{"ENCRYPTION", "COMPRESSION", "BATCH_OPERATIONS"}
        );
        // 尝试更新 VM 连接状态为 CONNECTED
        if (msg.getVmId() != null) {
            vmInstancesMapper.updateConnection(msg.getVmId(), "CONNECTED", Instant.now().toString());
        }
        sendToVmTopic(msg.getVmId(), mapOf("event", "CONNECTED", "vmId", msg.getVmId()));
        return ackFor(msg, ProtocolType.CONNECT_ACK, data);
    }

    private ProtocolAck onHeartbeat(ProtocolMessage msg) {
        if (msg.getVmId() != null) {
            vmInstancesMapper.updateConnection(msg.getVmId(), "CONNECTED", Instant.now().toString());
        }
        Map<String, Object> data = mapOf(
                "serverTime", Instant.now().toString(),
                "nextHeartbeat", 30,
                "systemStatus", "NORMAL");
        return ackFor(msg, ProtocolType.HEARTBEAT_ACK, data);
    }

    private ProtocolAck onGenericHandled(ProtocolMessage msg) {
        sendToVmTopic(msg.getVmId(), msg);
        return ackFor(msg, deduceAckType(msg.getType()), mapOf("status", "RECEIVED"));
    }

    private ProtocolType deduceAckType(ProtocolType type) {
        switch (type) {
            case DATASET_CREATE:
                return ProtocolType.DATASET_CREATE_ACK;
            case DATASET_APPEND_ROWS:
                return ProtocolType.DATASET_APPEND_ROWS_ACK;
            case DATASET_COMPLETE:
                return ProtocolType.DATASET_COMPLETE_ACK;
            default:
                return type;
        }
    }

    // ================= 数据集处理（持久化） =================

    private ProtocolAck onDatasetCreate(ProtocolMessage msg) {
        String datasetId = valueAsString(msg.getData(), "datasetId");
        if (datasetId == null || datasetId.isBlank()) {
            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                    "errorCode", "INVALID_DATASET_ID",
                    "errorMessage", "缺少 datasetId"));
        }
        String vmId = msg.getVmId();
        String description = valueAsString(msg.getData(), "datasetDescription");
        String dataType = valueAsString(msg.getData(), "datasetType");
        String name = description != null ? description : ("dataset-" + datasetId.substring(0, Math.min(datasetId.length(), 8)));
        String metadataJson = toJsonSafe(valueAsObject(msg.getData(), "metadata"));
        trainingDatasetMapper.upsertDataset(datasetId, vmId, name, description, dataType, "READY", metadataJson);
        Map<String, Object> ackData = mapOf(
                "datasetId", datasetId,
                "status", "READY");
        return ackFor(msg, ProtocolType.DATASET_CREATE_ACK, ackData);
    }

    private ProtocolAck onDatasetAppendRows(ProtocolMessage msg) {
        String datasetId = valueAsString(msg.getData(), "datasetId");
        List<?> rows = (List<?>) valueAsObject(msg.getData(), "rows");
        int rowsCount = rows == null ? 0 : rows.size();
        if (rowsCount > 0) {
            // 将每条 rowData 对象转为 JSON 字符串
            List<String> rowsJson = rows.stream().map(this::toJsonSafe).toList();
            trainingDatasetRowMapper.insertRows(datasetId, rowsJson);
        }
        Map<String, Object> ackData = mapOf(
                "datasetId", datasetId,
                "accepted", rowsCount,
                "rejected", 0);
        return ackFor(msg, ProtocolType.DATASET_APPEND_ROWS_ACK, ackData);
    }

    private ProtocolAck onDatasetComplete(ProtocolMessage msg) {
        String datasetId = valueAsString(msg.getData(), "datasetId");
        trainingDatasetMapper.updateStatus(datasetId, "READY");
        int rowCount = trainingDatasetRowMapper.countByDataset(datasetId);
        Map<String, Object> ackData = mapOf(
                "datasetId", datasetId,
                "rowCount", rowCount,
                "status", "READY");
        return ackFor(msg, ProtocolType.DATASET_COMPLETE_ACK, ackData);
    }

    private ProtocolAck onDatasetStatusQuery(ProtocolMessage msg) {
        String datasetId = valueAsString(msg.getData(), "datasetId");
        Map<String, Object> st = trainingDatasetMapper.selectById(datasetId);
        Map<String, Object> ackData;
        if (st == null) {
            ackData = mapOf(
                    "datasetId", datasetId,
                    "status", "NOT_FOUND");
        } else {
            int rowCount = trainingDatasetRowMapper.countByDataset(datasetId);
            ackData = new HashMap<>();
            ackData.put("datasetId", datasetId);
            ackData.put("datasetDescription", st.get("description"));
            ackData.put("datasetType", st.get("data_type"));
            ackData.put("rowCount", rowCount);
            ackData.put("status", st.get("status"));
            ackData.put("metadata", st.get("metadata"));
        }
        return ackFor(msg, ProtocolType.DATASET_STATUS_RESPONSE, ackData);
    }

    private ProtocolAck onDatasetDelete(ProtocolMessage msg) {
        String datasetId = valueAsString(msg.getData(), "datasetId");
        int deletedRows = trainingDatasetRowMapper.deleteByDataset(datasetId);
        int deletedDataset = trainingDatasetMapper.deleteById(datasetId);
        boolean existed = deletedRows > 0 || deletedDataset > 0;
        Map<String, Object> ackData = mapOf(
                "datasetId", datasetId,
                "deleted", existed);
        return ackFor(msg, ProtocolType.DATASET_DELETE_ACK, ackData);
    }

    // ================= VM 控制（持久化连接状态） =================

    private ProtocolAck onVmStart(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        vmInstancesMapper.updateConnection(vmId, "CONNECTED", Instant.now().toString());
        sendToVmTopic(vmId, msg);
        return ackFor(msg, ProtocolType.VM_START, mapOf("status", "RECEIVED"));
    }

    private ProtocolAck onVmStop(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        vmInstancesMapper.updateConnection(vmId, "DISCONNECTED", Instant.now().toString());
        sendToVmTopic(vmId, msg);
        return ackFor(msg, ProtocolType.VM_STOP, mapOf("status", "RECEIVED"));
    }

    // ================= 训练控制（持久化任务状态） =================

    private ProtocolAck onTrainingStart(ProtocolMessage msg) {
        Map<String, Object> d = msg.getData();
        String taskId = valueAsString(d, "taskId");
        String algorithm = valueAsString(d, "algorithm");
        Map<String, Object> config = (Map<String, Object>) valueAsObject(d, "config");
        Integer totalRounds = numberAsInt(config, "totalRounds");
        String configJson = toJsonSafe(config);
        federatedTasksMapper.upsertTask(taskId, taskId, algorithm, "RUNNING", totalRounds, 0, configJson);
        sendToVmTopic(msg.getVmId(), msg);
        return ackFor(msg, ProtocolType.TRAINING_START, mapOf("status", "RECEIVED"));
    }

    private ProtocolAck onTrainingStop(ProtocolMessage msg) {
        String taskId = valueAsString(msg.getData(), "taskId");
        federatedTasksMapper.updateStatus(taskId, "STOPPED");
        sendToVmTopic(msg.getVmId(), msg);
        return ackFor(msg, ProtocolType.TRAINING_STOP, mapOf("status", "RECEIVED"));
    }

    private ProtocolAck onTrainingProgress(ProtocolMessage msg) {
        Map<String, Object> d = msg.getData();
        String taskId = valueAsString(d, "taskId");
        Integer currentRound = numberAsInt(d, "currentRound");
        String status = valueAsString(d, "status");
        federatedTasksMapper.updateProgress(taskId, currentRound, status != null ? status : "RUNNING");
        sendToVmTopic(msg.getVmId(), msg);
        return ackFor(msg, ProtocolType.TRAINING_PROGRESS, mapOf("status", "RECEIVED"));
    }

    // ================= 模型传输（持久化本地轮次模型） =================

    private ProtocolAck onModelUpload(ProtocolMessage msg) {
        Map<String, Object> d = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(d, "taskId");
        Integer round = numberAsInt(d, "round");
        Map<String, Object> parameters = (Map<String, Object>) valueAsObject(d, "parameters");
        Map<String, Object> metrics = (Map<String, Object>) valueAsObject(d, "metrics");
        Double acc = numberAsDouble(metrics, "accuracy");
        Double loss = numberAsDouble(metrics, "loss");
        String parametersJson = toJsonSafe(mapOf("parameters", parameters, "metrics", metrics));
        // 以 (task, vm, round) 唯一，id 使用随机UUID
        vmRoundModelsMapper.upsertRoundModel(UUID.randomUUID().toString().replace("-", ""), taskId, vmId, round, acc, loss, parametersJson);
        sendToVmTopic(vmId, msg);
        return ackFor(msg, ProtocolType.MODEL_UPLOAD, mapOf("status", "RECEIVED"));
    }

    private ProtocolAck onModelDownload(ProtocolMessage msg) {
        sendToVmTopic(msg.getVmId(), msg);
        return ackFor(msg, ProtocolType.MODEL_DOWNLOAD, mapOf("status", "RECEIVED"));
    }

    // ================= 状态查询 =================

    private ProtocolAck onStatusQuery(ProtocolMessage msg) {
        sendToVmTopic(msg.getVmId(), msg);
        return ackFor(msg, ProtocolType.STATUS_QUERY, mapOf("status", "FORWARDED"));
    }

    private ProtocolAck onStatusResponse(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> d = msg.getData();
        if (d != null) {
            statusCache.put(vmId, new HashMap<>(d));
        }
        sendToVmTopic(vmId, mapOf("type", "STATUS_UPDATED", "vmId", vmId, "data", d));
        // 更新最近心跳
        if (vmId != null) {
            vmInstancesMapper.updateConnection(vmId, "CONNECTED", Instant.now().toString());
        }
        return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf("status", "UPDATED"));
    }

    // ================= 错误处理 =================

    private ProtocolAck onErrorMessage(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        sendToVmTopic(vmId, msg);
        messagingTemplate.convertAndSend("/topic/errors", msg);
        return ackFor(msg, msg.getType(), mapOf("status", "RECEIVED"));
    }

    // ================= 公共辅助 =================

    private void sendToVmTopic(String vmId, Object payload) {
        if (vmId == null || vmId.isBlank()) {
            messagingTemplate.convertAndSend("/topic/vm/_unknown", payload);
        } else {
            messagingTemplate.convertAndSend("/topic/vm/" + vmId, payload);
        }
    }

    private ProtocolAck ackFor(ProtocolMessage msg, ProtocolType ackType, Map<String, Object> data) {
        String vmId = msg != null ? msg.getVmId() : null;
        return ProtocolAck.builder()
                .type(ackType)
                .id("server-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6))
                .timestamp(Instant.now())
                .vmId(vmId)
                .data(data)
                .signature(null)
                .build();
    }

    private Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kv.length - 1; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    private String valueAsString(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private Object valueAsObject(Map<String, Object> m, String key) {
        return m == null ? null : m.get(key);
    }

    private Integer numberAsInt(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return v == null ? null : Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private Double numberAsDouble(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return v == null ? null : Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private String toJsonSafe(Object obj) {
        try {
            return obj == null ? null : objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
} 