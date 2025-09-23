package com.feduwacomm.service;

import com.feduwacomm.dto.ProtocolAck;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.enums.FederatedAlgorithm;
import com.feduwacomm.enums.FederatedTaskStatus;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.TrainingDatasetMapper;
import com.feduwacomm.mapper.TrainingDatasetRowMapper;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.mapper.VmRoundModelsMapper;
import com.feduwacomm.event.ModelUploadEvent;
import com.feduwacomm.utils.UuidUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class WebSocketProtocolService {

    private final SimpMessagingTemplate messagingTemplate;

    private final TrainingDatasetMapper trainingDatasetMapper;
    private final TrainingDatasetRowMapper trainingDatasetRowMapper;
    private final FederatedTasksMapper federatedTasksMapper;
    private final VmRoundModelsMapper vmRoundModelsMapper;
    private final VmInstancesMapper vmInstancesMapper;
    @SuppressWarnings("unused") // 保留用于未来功能扩展
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UuidUtil uuidUtil;

    // in-memory VM 最新状态缓存：vmId -> STATUS_RESPONSE.data（用于快速读，不作为数据源）
    private final ConcurrentHashMap<String, Map<String, Object>> statusCache = new ConcurrentHashMap<>();

    public WebSocketProtocolService(SimpMessagingTemplate messagingTemplate,
                                    TrainingDatasetMapper trainingDatasetMapper,
                                    TrainingDatasetRowMapper trainingDatasetRowMapper,
                                    FederatedTasksMapper federatedTasksMapper,
                                    VmRoundModelsMapper vmRoundModelsMapper,
                                    VmInstancesMapper vmInstancesMapper,
                                    UserMapper userMapper,
                                    ObjectMapper objectMapper,
                                    ApplicationEventPublisher eventPublisher,
                                    UuidUtil uuidUtil) {
        this.messagingTemplate = messagingTemplate;
        this.trainingDatasetMapper = trainingDatasetMapper;
        this.trainingDatasetRowMapper = trainingDatasetRowMapper;
        this.federatedTasksMapper = federatedTasksMapper;
        this.vmRoundModelsMapper = vmRoundModelsMapper;
        this.vmInstancesMapper = vmInstancesMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.uuidUtil = uuidUtil;
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
            case TRAINING_START_RESPONSE:
                return onTrainingStartResponse(msg);
            case TRAINING_STOP:
                return onTrainingStop(msg);
            case TRAINING_PROGRESS:
                return onTrainingProgress(msg);
            case TRAINING_PROGRESS_RESPONSE:
                return onTrainingProgressResponse(msg);
            case MODEL_UPLOAD:
                return onModelUpload(msg);
            case MODEL_DOWNLOAD:
                return onModelDownload(msg);
            case GLOBAL_MODEL_UPDATE:
                return onGlobalModelUpdate(msg);
            case STATUS_QUERY:
                return onStatusQuery(msg);
            case STATUS_RESPONSE:
                return onStatusResponse(msg);
            case BATCH_STATUS_QUERY:
                return onBatchStatusQuery(msg);
            case BATCH_STATUS_RESPONSE:
                return onBatchStatusResponse(msg);
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
        // v1.3: 处理虚拟机的本地ML算法能力和计算能力
        Map<String, Object> clientData = msg.getData();

        // 获取客户端上报的ML算法能力和系统信息
        @SuppressWarnings("unchecked")
        List<String> supportedMLAlgorithms = (List<String>) clientData.get("supportedMLAlgorithms");
        @SuppressWarnings("unchecked")
        Map<String, Object> computeCapabilities = (Map<String, Object>) clientData.get("computeCapabilities");
        @SuppressWarnings("unchecked")
        Map<String, Object> systemInfo = (Map<String, Object>) clientData.get("systemInfo");

        String vmId = msg.getVmId();
        if (vmId != null) {
            try {
                // 存储系统信息到数据库
                if (systemInfo != null) {
                    String systemInfoJson = objectMapper.writeValueAsString(systemInfo);
                    vmInstancesMapper.updateSystemInfo(vmId, systemInfoJson);
                    System.out.println("VM " + vmId + " 系统信息已更新: " + systemInfo);
                }

                // 存储能力信息到数据库（包含ML算法和计算能力）
                if (supportedMLAlgorithms != null || computeCapabilities != null) {
                    Map<String, Object> capabilities = new HashMap<>();
                    if (supportedMLAlgorithms != null) {
                        capabilities.put("supportedMLAlgorithms", supportedMLAlgorithms);
                        System.out.println("VM " + vmId + " 支持的ML算法: " + supportedMLAlgorithms);
                    }
                    if (computeCapabilities != null) {
                        capabilities.put("computeCapabilities", computeCapabilities);
                        System.out.println("VM " + vmId + " 计算能力: " + computeCapabilities);
                    }

                    String capabilitiesJson = objectMapper.writeValueAsString(capabilities);
                    vmInstancesMapper.updateCapabilities(vmId, capabilitiesJson);
                    System.out.println("VM " + vmId + " 能力信息已更新");
                }
            } catch (Exception e) {
                // 记录错误但不影响连接建立
                System.err.println("更新VM " + vmId + " 信息失败: " + e.getMessage());
            }
        }

        Map<String, Object> data = mapOf(
                "sessionId", uuidUtil.generateUuid(), // 使用UUIDv7生成32位紧凑会话ID
                "serverTime", Instant.now().toString(),
                "heartbeatInterval", 30,
                "maxMessageSize", 10 * 1024 * 1024,
                "supportedFeatures", new String[]{"ENCRYPTION", "COMPRESSION", "BATCH_OPERATIONS"}
        );

        // 尝试更新 VM 连接状态为 CONNECTED
        if (msg.getVmId() != null) {
            vmInstancesMapper.updateConnection(msg.getVmId(), "CONNECTED", LocalDateTime.now().toString());
        }
        sendToVmTopic(msg.getVmId(), mapOf("event", "CONNECTED", "vmId", msg.getVmId()));
        return ackFor(msg, ProtocolType.CONNECT_ACK, data);
    }

    private ProtocolAck onHeartbeat(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String heartbeatTime = LocalDateTime.now().toString();

        System.out.println("【心跳处理】收到心跳消息 - VmId: " + vmId + ", Time: " + heartbeatTime);

        if (vmId != null) {
            try {
                int result = vmInstancesMapper.updateConnection(vmId, "CONNECTED", heartbeatTime);
                System.out.println("【心跳处理】数据库更新结果 - VmId: " + vmId + ", UpdateResult: " + result + ", HeartbeatTime: " + heartbeatTime);
            } catch (Exception e) {
                System.err.println("【心跳处理】数据库更新失败 - VmId: " + vmId + ", Error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("【心跳处理】警告：心跳消息中缺少VmId");
        }

        Map<String, Object> data = mapOf(
                "serverTime", Instant.now().toString(), // 返回给客户端的时间可以保持ISO格式
                "nextHeartbeat", 30,
                "systemStatus", "NORMAL");
        return ackFor(msg, ProtocolType.HEARTBEAT_ACK, data);
    }

    @SuppressWarnings("unused") // 保留用于未来的通用消息处理
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

        trainingDatasetMapper.upsertDataset(datasetId, name, description, dataType, "READY", metadataJson);

        // 转发消息到VM专属频道
        sendToVmTopic(vmId, mapOf(
                "type", "DATASET_CREATED",
                "vmId", vmId,
                "datasetId", datasetId,
                "description", description,
                "dataType", dataType,
                "status", "READY"
        ));

        Map<String, Object> ackData = mapOf(
                "datasetId", datasetId,
                "status", "READY");
        return ackFor(msg, ProtocolType.DATASET_CREATE_ACK, ackData);
    }

    private ProtocolAck onDatasetAppendRows(ProtocolMessage msg) {
        String datasetId = valueAsString(msg.getData(), "datasetId");
        List<?> rows = (List<?>) valueAsObject(msg.getData(), "rows");
        int rowsCount = rows == null ? 0 : rows.size();
        if (rows != null && rowsCount > 0) {
            // 将每条 rowData 对象转为 JSON 字符串
            List<String> rowsJson = rows.stream().map(this::toJsonSafe).toList();
            trainingDatasetRowMapper.insertRows(datasetId, rowsJson);
        }

        // 转发消息到VM专属频道
        sendToVmTopic(msg.getVmId(), mapOf(
                "type", "DATASET_ROWS_APPENDED",
                "vmId", msg.getVmId(),
                "datasetId", datasetId,
                "rowsAdded", rowsCount,
                "sampleData", (rows != null && rowsCount > 0) ? rows.get(0) : null
        ));

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

        // 转发消息到VM专属频道
        sendToVmTopic(msg.getVmId(), mapOf(
                "type", "DATASET_COMPLETED",
                "vmId", msg.getVmId(),
                "datasetId", datasetId,
                "totalRows", rowCount,
                "status", "READY"
        ));

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
        vmInstancesMapper.updateConnection(vmId, "CONNECTED", LocalDateTime.now().toString());
        sendToVmTopic(vmId, msg);
        return ackFor(msg, ProtocolType.VM_START, mapOf("status", "RECEIVED"));
    }

    private ProtocolAck onVmStop(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        vmInstancesMapper.updateConnection(vmId, "DISCONNECTED", LocalDateTime.now().toString());
        sendToVmTopic(vmId, msg);
        return ackFor(msg, ProtocolType.VM_STOP, mapOf("status", "RECEIVED"));
    }

    // ================= 训练控制（持久化任务状态） =================

    private ProtocolAck onTrainingStart(ProtocolMessage msg) {
        // 开始训练：只转发给VM，不立即更新数据库
        // 等待VM的响应确认后再更新数据库状态

        Map<String, Object> d = msg.getData();
        String taskId = valueAsString(d, "taskId");
        // v1.3: 使用mlAlgorithm替代algorithm（联邦学习算法）
        String mlAlgorithm = valueAsString(d, "mlAlgorithm");
        // v1.3: 分离超参数和训练配置
        @SuppressWarnings("unchecked")
        Map<String, Object> hyperparameters = (Map<String, Object>) valueAsObject(d, "hyperparameters");
        @SuppressWarnings("unchecked")
        Map<String, Object> trainingConfig = (Map<String, Object>) valueAsObject(d, "trainingConfig");

        // 合并配置用于存储（保持数据库兼容性）
        Map<String, Object> combinedConfig = new HashMap<>();
        if (hyperparameters != null) {
            combinedConfig.put("hyperparameters", hyperparameters);
        }
        if (trainingConfig != null) {
            combinedConfig.put("trainingConfig", trainingConfig);
        }
        Integer epochs = trainingConfig != null ? numberAsInt(trainingConfig, "epochs") : 1;

        // 创建任务记录，状态为PENDING（等待VM确认）
        // v1.3: 创建联邦学习任务记录，后端使用默认联邦算法，VM使用mlAlgorithm
        // 联邦学习算法由后端管理，这里使用默认的FEDERATED_AVERAGING
        String federatedAlgorithm = "FEDERATED_AVERAGING";

        // 将VM的mlAlgorithm信息保存到config中，供后续使用
        combinedConfig.put("mlAlgorithm", mlAlgorithm);
        String updatedConfigJson = toJsonSafe(combinedConfig);

        // 使用新的API：创建FederatedTask对象并插入
        FederatedTask task = new FederatedTask();
        task.setId(taskId);
        task.setTaskName(taskId);
        task.setAlgorithm(FederatedAlgorithm.valueOf(federatedAlgorithm));
        task.setStatus(FederatedTaskStatus.PENDING);
        task.setEpochs(epochs);
        task.setTotalRounds(epochs);
        task.setCurrentRound(0);
        task.setConfig(updatedConfigJson);
        task.setCreatedAt(java.time.LocalDateTime.now());
        task.setUpdatedAt(java.time.LocalDateTime.now());
        federatedTasksMapper.insertTask(task);

        // 转发训练开始指令给VM
        sendToVmTopic(msg.getVmId(), mapOf(
                "type", "TRAINING_START_COMMAND",
                "vmId", msg.getVmId(),
                "taskId", taskId,
                "mlAlgorithm", mlAlgorithm,
                "hyperparameters", hyperparameters,
                "trainingConfig", trainingConfig,
                "message", "请开始本地ML训练任务"
        ));

        return ackFor(msg, ProtocolType.TRAINING_START_ACK, mapOf(
                "status", "COMMAND_SENT",
                "message", "本地训练指令已发送给VM，等待VM确认"));
    }

    // VM响应训练开始确认
    private ProtocolAck onTrainingStartResponse(ProtocolMessage msg) {
        Map<String, Object> d = msg.getData();
        String taskId = valueAsString(d, "taskId");
        String status = valueAsString(d, "status");
        String message = valueAsString(d, "message");

        if ("SUCCESS".equals(status)) {
            // VM确认训练开始成功，更新数据库状态为RUNNING
            federatedTasksMapper.updateTaskStatus(taskId, "RUNNING", LocalDateTime.now());

            // 通知前端训练已开始
            sendToVmTopic(msg.getVmId(), mapOf(
                    "type", "TRAINING_STARTED",
                    "vmId", msg.getVmId(),
                    "taskId", taskId,
                    "status", "RUNNING",
                    "message", "训练已成功开始"
            ));
        } else {
            // VM确认训练开始失败
            federatedTasksMapper.updateTaskStatus(taskId, "FAILED", LocalDateTime.now());

            sendToVmTopic(msg.getVmId(), mapOf(
                    "type", "TRAINING_START_FAILED",
                    "vmId", msg.getVmId(),
                    "taskId", taskId,
                    "status", "FAILED",
                    "error", message
            ));
        }

        return ackFor(msg, ProtocolType.TRAINING_START_RESPONSE_ACK, mapOf("status", "PROCESSED"));
    }

    private ProtocolAck onTrainingStop(ProtocolMessage msg) {
        // 训练停止：转发给VM，不立即更新数据库
        String taskId = valueAsString(msg.getData(), "taskId");

        // 转发停止指令给VM
        sendToVmTopic(msg.getVmId(), mapOf(
                "type", "TRAINING_STOP_COMMAND",
                "vmId", msg.getVmId(),
                "taskId", taskId,
                "message", "请停止训练任务"
        ));

        return ackFor(msg, ProtocolType.TRAINING_STOP_ACK, mapOf(
                "status", "COMMAND_SENT",
                "message", "停止指令已发送给VM"));
    }

    private ProtocolAck onTrainingProgress(ProtocolMessage msg) {
        // 训练进度查询：发送查询指令给VM，不直接更新数据库
        Map<String, Object> d = msg.getData();
        String taskId = valueAsString(d, "taskId");

        // 转发进度查询指令给VM
        sendToVmTopic(msg.getVmId(), mapOf(
                "type", "TRAINING_PROGRESS_QUERY",
                "vmId", msg.getVmId(),
                "taskId", taskId,
                "message", "请报告训练进度"
        ));

        return ackFor(msg, ProtocolType.TRAINING_PROGRESS_ACK, mapOf(
                "status", "QUERY_SENT",
                "message", "进度查询指令已发送给VM"));
    }

    // VM响应训练进度
    private ProtocolAck onTrainingProgressResponse(ProtocolMessage msg) {
        Map<String, Object> d = msg.getData();
        String taskId = valueAsString(d, "taskId");
        Integer currentRound = numberAsInt(d, "currentRound");
        String status = valueAsString(d, "status");
        Double accuracy = numberAsDouble(d, "accuracy");
        Double loss = numberAsDouble(d, "loss");

        // 更新数据库中的进度信息
        Double progressPercent = (accuracy != null) ? accuracy : 0.0; // 使用准确率作为进度指标，如果没有则设为0
        federatedTasksMapper.updateTaskProgress(taskId, currentRound, progressPercent, status != null ? status : "RUNNING");

        // 转发进度信息给前端
        sendToVmTopic(msg.getVmId(), mapOf(
                "type", "TRAINING_PROGRESS_UPDATE",
                "vmId", msg.getVmId(),
                "taskId", taskId,
                "currentRound", currentRound,
                "status", status,
                "accuracy", accuracy,
                "loss", loss,
                "message", "训练进度已更新"
        ));

        return ackFor(msg, ProtocolType.TRAINING_PROGRESS_RESPONSE_ACK, mapOf("status", "PROCESSED"));
    }

    // ================= 模型传输（持久化本地轮次模型） =================

    private ProtocolAck onModelUpload(ProtocolMessage msg) {
        Map<String, Object> d = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(d, "taskId");
        Integer round = numberAsInt(d, "round");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) valueAsObject(d, "parameters");
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) valueAsObject(d, "metrics");
        Double acc = numberAsDouble(metrics, "accuracy");
        Double loss = numberAsDouble(metrics, "loss");
        String parametersJson = toJsonSafe(mapOf("parameters", parameters, "metrics", metrics));
        
        // 以 (task, vm, round) 唯一，id 使用随机UUID
        vmRoundModelsMapper.upsertRoundModel(uuidUtil.generateUuid(), taskId, vmId, round, acc, loss, parametersJson);
        
        // 发布模型上传事件，触发聚合检查
        try {
            ModelUploadEvent uploadEvent = new ModelUploadEvent(this, taskId, round, vmId, 
                    (long) parametersJson.length(), acc, loss);
            eventPublisher.publishEvent(uploadEvent);
        } catch (Exception e) {
            // 事件发布失败不应影响模型上传的正常流程
            System.err.println("发布模型上传事件失败: " + e.getMessage());
        }
        
        sendToVmTopic(vmId, msg);
        // v1.3: 移除聚合相关信息，专注本地训练结果接收
        return ackFor(msg, ProtocolType.MODEL_UPLOAD, mapOf("status", "RECEIVED"));
    }

    private ProtocolAck onModelDownload(ProtocolMessage msg) {
        // 模型下载请求 - VM请求下载最新的全局模型
        String vmId = msg.getVmId();
        Map<String, Object> d = msg.getData();
        String taskId = valueAsString(d, "taskId");
        Integer requestedRound = numberAsInt(d, "round");

        System.out.println("【模型下载】VmId: " + vmId + ", TaskId: " + taskId + ", RequestedRound: " + requestedRound);

        // 从数据库查询最新的全局模型版本
        try {
            // 这里可以实现从数据库查询最新模型版本的逻辑
            // 暂时返回一个模拟的全局模型信息
            Map<String, Object> globalModelData = mapOf(
                    "taskId", taskId,
                    "round", requestedRound != null ? requestedRound : 1,
                    "parameters", mapOf(
                            "model", mapOf(
                                    "framework", "pytorch",
                                    "format", "state_dict",
                                    "weights", mapOf(
                                            "shape", java.util.Arrays.asList(784, 256, 128, 10),
                                            "dtype", "float32",
                                            "checksum", "sha256:global_model_" + System.currentTimeMillis()
                                    )
                            ),
                            // v1.3: 移除聚合算法信息，专注模型结构和参数传输
                            "training", mapOf(
                                    "algorithm", "RandomForest",
                                    "samples", 1000
                            )
                    ),
                    "compression", "gzip",
                    "downloadTime", new java.util.Date().toString()
            );

            // 将模型信息发送给请求的VM
            sendToVmTopic(vmId, mapOf(
                    "type", "MODEL_DOWNLOAD_RESPONSE",
                    "vmId", vmId,
                    "taskId", taskId,
                    "data", globalModelData,
                    "message", "全局模型下载数据"
            ));

            return ackFor(msg, ProtocolType.MODEL_DOWNLOAD, mapOf(
                    "status", "MODEL_SENT",
                    "taskId", taskId,
                    "round", requestedRound != null ? requestedRound : 1,
                    "message", "全局模型数据已发送"));

        } catch (Exception e) {
            System.err.println("【模型下载错误】VmId: " + vmId + ", Error: " + e.getMessage());
            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                    "errorCode", "MODEL_DOWNLOAD_FAILED",
                    "errorMessage", "模型下载失败: " + e.getMessage()));
        }
    }

    private ProtocolAck onGlobalModelUpdate(ProtocolMessage msg) {
        // 全局模型更新 - 这通常是服务端主动推送给客户端的消息
        // 客户端收到后应该下载并应用新的全局模型
        String vmId = msg.getVmId();
        Map<String, Object> d = msg.getData();
        String taskId = valueAsString(d, "taskId");
        Integer round = numberAsInt(d, "round");

        System.out.println("【全局模型更新】VmId: " + vmId + ", TaskId: " + taskId + ", Round: " + round);

        // 转发全局模型更新到指定VM
        sendToVmTopic(vmId, mapOf(
                "type", "GLOBAL_MODEL_UPDATE",
                "vmId", vmId,
                "taskId", taskId,
                "round", round,
                "data", d,
                "message", "全局模型已更新，请下载最新版本"
        ));

        return ackFor(msg, ProtocolType.GLOBAL_MODEL_UPDATE, mapOf(
                "status", "BROADCASTED",
                "message", "全局模型更新已广播"));
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
            vmInstancesMapper.updateConnection(vmId, "CONNECTED", LocalDateTime.now().toString());
        }
        return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf("status", "UPDATED"));
    }

    private ProtocolAck onBatchStatusQuery(ProtocolMessage msg) {
        // 批量状态查询 - 向多个VM并行发送状态查询请求
        Map<String, Object> d = msg.getData();
        @SuppressWarnings("unchecked")
        java.util.List<String> vmIds = (java.util.List<String>) d.get("vmIds");
        String queryType = valueAsString(d, "queryType");
        Boolean includeResources = (Boolean) d.get("includeResources");
        Boolean includeProcesses = (Boolean) d.get("includeProcesses");
        Boolean includeNetwork = (Boolean) d.get("includeNetwork");

        if (vmIds == null || vmIds.isEmpty()) {
            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                    "errorCode", "INVALID_VM_LIST",
                    "errorMessage", "VM ID列表不能为空"));
        }

        System.out.println("【批量状态查询】VmIds: " + vmIds + ", QueryType: " + queryType);

        // 并行向所有指定的VM发送状态查询
        for (String vmId : vmIds) {
            Map<String, Object> queryData = mapOf(
                    "queryType", queryType != null ? queryType : "FULL",
                    "includeResources", includeResources != null ? includeResources : true,
                    "includeProcesses", includeProcesses != null ? includeProcesses : true,
                    "includeNetwork", includeNetwork != null ? includeNetwork : true,
                    "timeout", 10,
                    "batchQueryId", msg.getId()
            );

            sendToVmTopic(vmId, mapOf(
                    "type", "STATUS_QUERY",
                    "vmId", vmId,
                    "queryId", "batch-" + System.currentTimeMillis() + "-" + vmId,
                    "data", queryData,
                    "message", "批量状态查询请求"
            ));
        }

        return ackFor(msg, ProtocolType.BATCH_STATUS_QUERY, mapOf(
                "status", "QUERIES_SENT",
                "queriedVmCount", vmIds.size(),
                "message", "批量状态查询已发送到 " + vmIds.size() + " 个虚拟机"));
    }

    private ProtocolAck onBatchStatusResponse(ProtocolMessage msg) {
        // 批量状态查询的汇总响应
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        System.out.println("【批量状态响应】VmId: " + vmId + ", Data: " + toJsonSafe(data));

        // 这里可以实现批量状态的汇总和统计分析
        // 例如生成状态统计报告、更新集群状态等

        // 将单个VM的状态响应存储到缓存
        if (data != null) {
            statusCache.put(vmId, new HashMap<>(data));
        }

        return ackFor(msg, ProtocolType.BATCH_STATUS_RESPONSE, mapOf("status", "PROCESSED"));
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
                .id("server-" + System.currentTimeMillis() + "-" + uuidUtil.generateUuid().substring(0, 6))
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