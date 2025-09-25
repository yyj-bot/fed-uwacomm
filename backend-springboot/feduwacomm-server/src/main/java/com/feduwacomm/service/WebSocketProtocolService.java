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
import com.feduwacomm.utils.MessageBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(WebSocketProtocolService.class);

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
            case MODEL_UPDATE_ACK:
                return onModelUpdateAck(msg);
            // 联邦学习聚合协议处理器
            case GRADIENT_UPLOAD:
                return onGradientUpload(msg);
            case GRADIENT_UPLOAD_ACK:
                return onGradientUploadAck(msg);
            case AGGREGATION_START:
                return onAggregationStart(msg);
            case AGGREGATION_START_ACK:
                return onAggregationStartAck(msg);
            case AGGREGATION_COMPLETE:
                return onAggregationComplete(msg);
            case AGGREGATION_COMPLETE_ACK:
                return onAggregationCompleteAck(msg);
            case GLOBAL_MODEL_BROADCAST:
                return onGlobalModelBroadcast(msg);
            case GLOBAL_MODEL_BROADCAST_ACK:
                return onGlobalModelBroadcastAck(msg);
            case ROUND_START:
                return onRoundStart(msg);
            case ROUND_START_ACK:
                return onRoundStartAck(msg);
            case ROUND_COMPLETE:
                return onRoundComplete(msg);
            case ROUND_COMPLETE_ACK:
                return onRoundCompleteAck(msg);
            case MODEL_TYPE_NEGOTIATION:
                return onModelTypeNegotiation(msg);
            case MODEL_TYPE_NEGOTIATION_ACK:
                return onModelTypeNegotiationAck(msg);
            // 第三阶段：增强功能协议
            case ALGORITHM_CONFIG:
                return onAlgorithmConfig(msg);
            case ALGORITHM_CONFIG_ACK:
                return onAlgorithmConfigAck(msg);
            case GRADIENT_UPLOAD_PREPARE:
                return onGradientUploadPrepare(msg);
            case GRADIENT_UPLOAD_PREPARE_ACK:
                return onGradientUploadPrepareAck(msg);
            case AGGREGATION_NOTIFICATION:
                return onAggregationNotification(msg);
            case STRATEGY_SWITCH_NOTIFICATION:
                return onStrategySwitchNotification(msg);
            case STRATEGY_SWITCH_ACK:
                return onStrategySwitchAck(msg);
            case STATUS_QUERY:
                return onStatusQuery(msg);
            case STATUS_RESPONSE:
                return onStatusResponse(msg);
            case BATCH_STATUS_QUERY:
                return onBatchStatusQuery(msg);
            case BATCH_STATUS_RESPONSE:
                return onBatchStatusResponse(msg);
            case TASK_START:
                return onTaskStart(msg);
            case FEDERATED_TASK_START:
                return onFederatedTaskStart(msg);
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
        // 移除非标准格式的消息发送，使用标准的CONNECT_ACK响应
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
        sendToVmTopic(vmId, MessageBuilder.createNotification(
                ProtocolType.DATASET_CREATE_NOTIFICATION,
                "数据集已创建",
                "数据集 " + datasetId + " 已成功创建",
                mapOf(
                        "vmId", vmId,
                        "datasetId", datasetId,
                        "description", description,
                        "dataType", dataType,
                        "status", "READY"
                )
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
        sendToVmTopic(msg.getVmId(), MessageBuilder.createNotification(
                ProtocolType.DATASET_APPEND_ROWS_NOTIFICATION,
                "数据集行已追加",
                "数据集 " + datasetId + " 已追加 " + rowsCount + " 行数据",
                mapOf(
                        "vmId", msg.getVmId(),
                        "datasetId", datasetId,
                        "rowsAdded", rowsCount,
                        "sampleData", (rows != null && rowsCount > 0) ? rows.get(0) : null
                )
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
        sendToVmTopic(msg.getVmId(), MessageBuilder.createNotification(
                ProtocolType.DATASET_COMPLETE_NOTIFICATION,
                "数据集已完成",
                "数据集 " + datasetId + " 已完成，共 " + rowCount + " 行数据",
                mapOf(
                        "vmId", msg.getVmId(),
                        "datasetId", datasetId,
                        "totalRows", rowCount,
                        "status", "READY"
                )
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
        sendToVmTopic(msg.getVmId(), MessageBuilder.createNotification(
                ProtocolType.TRAINING_START_COMMAND_NOTIFICATION,
                "训练开始指令",
                "请开始本地ML训练任务: " + taskId,
                mapOf(
                        "vmId", msg.getVmId(),
                        "taskId", taskId,
                        "mlAlgorithm", mlAlgorithm,
                        "hyperparameters", hyperparameters,
                        "trainingConfig", trainingConfig
                )
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
            sendToVmTopic(msg.getVmId(), MessageBuilder.createNotification(
                    ProtocolType.TRAINING_START_NOTIFICATION,
                    "训练已开始",
                    "训练任务 " + taskId + " 已成功开始",
                    mapOf(
                            "vmId", msg.getVmId(),
                            "taskId", taskId,
                            "status", "RUNNING"
                    )
            ));
        } else {
            // VM确认训练开始失败
            federatedTasksMapper.updateTaskStatus(taskId, "FAILED", LocalDateTime.now());

            sendToVmTopic(msg.getVmId(), MessageBuilder.createNotification(
                    ProtocolType.TRAINING_START_FAILURE_NOTIFICATION,
                    "训练启动失败",
                    "训练任务 " + taskId + " 启动失败: " + message,
                    mapOf(
                            "vmId", msg.getVmId(),
                            "taskId", taskId,
                            "status", "FAILED",
                            "error", message
                    )
            ));
        }

        return ackFor(msg, ProtocolType.TRAINING_START_RESPONSE_ACK, mapOf("status", "PROCESSED"));
    }

    private ProtocolAck onTrainingStop(ProtocolMessage msg) {
        // 训练停止：转发给VM，不立即更新数据库
        String taskId = valueAsString(msg.getData(), "taskId");

        // 转发停止指令给VM
        sendToVmTopic(msg.getVmId(), MessageBuilder.createNotification(
                ProtocolType.TRAINING_STOP_COMMAND_NOTIFICATION,
                "训练停止指令",
                "请停止训练任务: " + taskId,
                mapOf(
                        "vmId", msg.getVmId(),
                        "taskId", taskId
                )
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
        sendToVmTopic(msg.getVmId(), MessageBuilder.createNotification(
                ProtocolType.TRAINING_PROGRESS_QUERY_NOTIFICATION,
                "训练进度查询",
                "请报告训练进度: " + taskId,
                mapOf(
                        "vmId", msg.getVmId(),
                        "taskId", taskId
                )
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
        sendToVmTopic(msg.getVmId(), MessageBuilder.createNotification(
                ProtocolType.TRAINING_PROGRESS_UPDATE_NOTIFICATION,
                "训练进度更新",
                "训练任务 " + taskId + " 进度已更新，轮次: " + currentRound,
                mapOf(
                        "vmId", msg.getVmId(),
                        "taskId", taskId,
                        "currentRound", currentRound,
                        "status", status,
                        "accuracy", accuracy,
                        "loss", loss
                )
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
            sendToVmTopic(vmId, MessageBuilder.createNotification(
                    ProtocolType.MODEL_DOWNLOAD_NOTIFICATION,
                    "模型下载响应",
                    "全局模型 " + taskId + " 下载数据已准备完成",
                    mapOf(
                            "vmId", vmId,
                            "taskId", taskId,
                            "data", globalModelData
                    )
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

    private ProtocolAck onModelUpdateAck(ProtocolMessage msg) {
        // 处理VM发送的模型更新确认消息
        String vmId = msg.getVmId();
        Map<String, Object> d = msg.getData();
        Object modelVersion = d.get("modelVersion");
        String updateTime = valueAsString(d, "updateTime");

        System.out.println("【模型更新确认】VmId: " + vmId + ", ModelVersion: " + modelVersion + ", UpdateTime: " + updateTime);

        // 更新VM连接状态，表示该VM已成功接收模型更新
        if (vmId != null) {
            try {
                vmInstancesMapper.updateConnection(vmId, "CONNECTED", LocalDateTime.now().toString());
                System.out.println("【模型更新确认】VM " + vmId + " 模型更新确认已处理");
            } catch (Exception e) {
                System.err.println("【模型更新确认】处理VM " + vmId + " 确认时出错: " + e.getMessage());
            }
        }

        // 简单返回处理成功的确认
        return ackFor(msg, ProtocolType.MODEL_UPDATE_ACK, mapOf(
                "status", "PROCESSED",
                "vmId", vmId,
                "message", "模型更新确认已处理"));
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
        sendToVmTopic(vmId, MessageBuilder.createNotification(
                ProtocolType.STATUS_UPDATE_NOTIFICATION,
                "状态已更新",
                "VM " + vmId + " 状态信息已更新",
                d
        ));
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

    private ProtocolAck onTaskStart(ProtocolMessage msg) {
        // 处理任务启动消息
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        System.out.println("🚀 收到任务启动消息: vmId=" + vmId + ", data=" + data);

        // 返回确认响应
        return ackFor(msg, ProtocolType.TASK_START_ACK, mapOf(
                "status", "ACKNOWLEDGED",
                "message", "任务启动消息已接收",
                "timestamp", Instant.now().toString()
        ));
    }

    private ProtocolAck onFederatedTaskStart(ProtocolMessage msg) {
        // 处理联邦学习任务启动消息
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();
        String taskId = valueAsString(data, "taskId");

        System.out.println("🤖 收到联邦学习任务启动消息: vmId=" + vmId + ", taskId=" + taskId);

        // 返回确认响应
        return ackFor(msg, ProtocolType.FEDERATED_TASK_START_ACK, mapOf(
                "status", "ACKNOWLEDGED",
                "taskId", taskId,
                "vmId", vmId,
                "message", "联邦学习任务启动消息已接收",
                "timestamp", Instant.now().toString()
        ));
    }

    // === 新增聚合相关消息处理方法 ===



    /**
     * 处理聚合开始确认消息
     * 虚拟机确认已收到聚合开始通知
     */
    private ProtocolAck onAggregationStartAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String status = valueAsString(data, "status");

        log.info("收到聚合开始确认: vmId={}, taskId={}, round={}, status={}",
                vmId, taskId, round, status);

        try {
            if ("ACKNOWLEDGED".equals(status)) {
                log.info("虚拟机{}确认聚合开始: taskId={}, round={}", vmId, taskId, round);
                // TODO: 可以在这里更新聚合状态跟踪
            } else {
                String errorMessage = valueAsString(data, "errorMessage");
                log.warn("虚拟机{}聚合开始确认异常: taskId={}, round={}, error={}",
                        vmId, taskId, round, errorMessage);
            }

            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ACK_PROCESSED",
                "vmId", vmId,
                "taskId", taskId,
                "round", round,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理聚合开始确认失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "处理聚合开始确认失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理聚合完成确认消息
     * 虚拟机确认已收到聚合完成通知
     */
    private ProtocolAck onAggregationCompleteAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String status = valueAsString(data, "status");

        log.info("收到聚合完成确认: vmId={}, taskId={}, round={}, status={}",
                vmId, taskId, round, status);

        try {
            if ("ACKNOWLEDGED".equals(status)) {
                log.info("虚拟机{}确认聚合完成: taskId={}, round={}", vmId, taskId, round);
                // TODO: 可以在这里更新聚合完成状态跟踪
            } else {
                String errorMessage = valueAsString(data, "errorMessage");
                log.warn("虚拟机{}聚合完成确认异常: taskId={}, round={}, error={}",
                        vmId, taskId, round, errorMessage);
            }

            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ACK_PROCESSED",
                "vmId", vmId,
                "taskId", taskId,
                "round", round,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理聚合完成确认失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "处理聚合完成确认失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理轮次开始确认消息
     * 虚拟机确认已收到轮次开始通知
     */
    private ProtocolAck onRoundStartAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String status = valueAsString(data, "status");

        log.info("收到轮次开始确认: vmId={}, taskId={}, round={}, status={}",
                vmId, taskId, round, status);

        try {
            if ("ACKNOWLEDGED".equals(status)) {
                log.info("虚拟机{}确认轮次开始: taskId={}, round={}", vmId, taskId, round);
                // TODO: 可以在这里更新轮次开始状态跟踪
            } else {
                String errorMessage = valueAsString(data, "errorMessage");
                log.warn("虚拟机{}轮次开始确认异常: taskId={}, round={}, error={}",
                        vmId, taskId, round, errorMessage);
            }

            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ACK_PROCESSED",
                "vmId", vmId,
                "taskId", taskId,
                "round", round,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理轮次开始确认失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "处理轮次开始确认失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理轮次完成确认消息
     * 虚拟机确认已收到轮次完成通知
     */
    private ProtocolAck onRoundCompleteAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String status = valueAsString(data, "status");

        log.info("收到轮次完成确认: vmId={}, taskId={}, round={}, status={}",
                vmId, taskId, round, status);

        try {
            if ("ACKNOWLEDGED".equals(status)) {
                log.info("虚拟机{}确认轮次完成: taskId={}, round={}", vmId, taskId, round);
                // TODO: 可以在这里更新轮次完成状态跟踪
            } else {
                String errorMessage = valueAsString(data, "errorMessage");
                log.warn("虚拟机{}轮次完成确认异常: taskId={}, round={}, error={}",
                        vmId, taskId, round, errorMessage);
            }

            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ACK_PROCESSED",
                "vmId", vmId,
                "taskId", taskId,
                "round", round,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理轮次完成确认失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "处理轮次完成确认失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理梯度上传确认消息
     * VM确认已收到梯度上传响应
     */
    private ProtocolAck onGradientUploadAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String status = valueAsString(data, "status");

        log.info("收到梯度上传确认: vmId={}, taskId={}, round={}, status={}",
                vmId, taskId, round, status);

        try {
            if ("SUCCESS".equals(status)) {
                log.info("虚拟机{}确认梯度上传成功: taskId={}, round={}", vmId, taskId, round);
                // TODO: 可以在这里更新梯度上传状态跟踪
            } else {
                String errorMessage = valueAsString(data, "errorMessage");
                log.warn("虚拟机{}梯度上传确认异常: taskId={}, round={}, error={}",
                        vmId, taskId, round, errorMessage);
                // TODO: 处理上传失败的情况，可能需要重新上传
            }

            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ACK_PROCESSED",
                "vmId", vmId,
                "taskId", taskId,
                "round", round,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理梯度上传确认失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "处理梯度上传确认失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理聚合开始消息
     * 服务器通知VM开始聚合过程
     */
    private ProtocolAck onAggregationStart(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String algorithm = valueAsString(data, "algorithm");

        log.info("处理聚合开始请求: vmId={}, taskId={}, round={}, algorithm={}",
                vmId, taskId, round, algorithm);

        try {
            // 发送聚合开始通知给所有参与的VM
            ProtocolMessage startMsg = ProtocolMessage.builder()
                    .type(ProtocolType.AGGREGATION_START)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                        "taskId", taskId,
                        "round", round,
                        "algorithm", algorithm != null ? algorithm : "FedAvg",
                        "message", "开始第" + round + "轮聚合"
                    ))
                    .build();

            // 广播给所有VM
            messagingTemplate.convertAndSend("/topic/vm", startMsg);

            log.info("聚合开始通知已发送: taskId={}, round={}, algorithm={}",
                    taskId, round, algorithm);

            return ackFor(msg, ProtocolType.AGGREGATION_START_ACK, mapOf(
                "status", "STARTED",
                "taskId", taskId,
                "round", round,
                "algorithm", algorithm,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理聚合开始失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.AGGREGATION_START_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "聚合开始失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理聚合完成消息
     * 服务器通知VM聚合过程完成
     */
    private ProtocolAck onAggregationComplete(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String modelVersion = valueAsString(data, "modelVersion");

        log.info("处理聚合完成请求: vmId={}, taskId={}, round={}, modelVersion={}",
                vmId, taskId, round, modelVersion);

        try {
            // 发送聚合完成通知给所有参与的VM
            ProtocolMessage completeMsg = ProtocolMessage.builder()
                    .type(ProtocolType.AGGREGATION_COMPLETE)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                        "taskId", taskId,
                        "round", round,
                        "modelVersion", modelVersion,
                        "message", "第" + round + "轮聚合完成",
                        "status", "COMPLETED"
                    ))
                    .build();

            // 广播给所有VM
            messagingTemplate.convertAndSend("/topic/vm", completeMsg);

            log.info("聚合完成通知已发送: taskId={}, round={}, modelVersion={}",
                    taskId, round, modelVersion);

            return ackFor(msg, ProtocolType.AGGREGATION_COMPLETE_ACK, mapOf(
                "status", "COMPLETED",
                "taskId", taskId,
                "round", round,
                "modelVersion", modelVersion,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理聚合完成失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.AGGREGATION_COMPLETE_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "聚合完成处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理全局模型广播消息
     * 服务器向VM广播全局模型
     */
    private ProtocolAck onGlobalModelBroadcast(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        Map<String, Object> globalModel = (Map<String, Object>) data.get("globalModel");

        log.info("处理全局模型广播请求: vmId={}, taskId={}, round={}",
                vmId, taskId, round);

        try {
            // 发送全局模型广播给所有参与的VM
            ProtocolMessage broadcastMsg = ProtocolMessage.builder()
                    .type(ProtocolType.GLOBAL_MODEL_BROADCAST)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                        "taskId", taskId,
                        "round", round,
                        "globalModel", globalModel,
                        "message", "第" + round + "轮全局模型已就绪",
                        "modelSize", globalModel != null ? globalModel.size() : 0
                    ))
                    .build();

            // 广播给所有VM
            messagingTemplate.convertAndSend("/topic/vm", broadcastMsg);

            log.info("全局模型广播已发送: taskId={}, round={}, modelSize={}",
                    taskId, round, globalModel != null ? globalModel.size() : 0);

            return ackFor(msg, ProtocolType.GLOBAL_MODEL_BROADCAST_ACK, mapOf(
                "status", "BROADCASTED",
                "taskId", taskId,
                "round", round,
                "modelSize", globalModel != null ? globalModel.size() : 0,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理全局模型广播失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.GLOBAL_MODEL_BROADCAST_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "全局模型广播失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理轮次开始消息
     * 服务器通知VM开始新的训练轮次
     */
    private ProtocolAck onRoundStart(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String algorithm = valueAsString(data, "algorithm");
        Integer totalRounds = numberAsInt(data, "totalRounds");

        log.info("处理轮次开始请求: vmId={}, taskId={}, round={}/{}, algorithm={}",
                vmId, taskId, round, totalRounds, algorithm);

        try {
            // 发送轮次开始通知给所有参与的VM
            ProtocolMessage roundStartMsg = ProtocolMessage.builder()
                    .type(ProtocolType.ROUND_START)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                        "taskId", taskId,
                        "round", round,
                        "totalRounds", totalRounds,
                        "algorithm", algorithm != null ? algorithm : "FedAvg",
                        "message", "开始第" + round + "轮训练（共" + totalRounds + "轮）",
                        "roundStartTime", Instant.now().toString()
                    ))
                    .build();

            // 广播给所有VM
            messagingTemplate.convertAndSend("/topic/vm", roundStartMsg);

            log.info("轮次开始通知已发送: taskId={}, round={}/{}, algorithm={}",
                    taskId, round, totalRounds, algorithm);

            return ackFor(msg, ProtocolType.ROUND_START_ACK, mapOf(
                "status", "ROUND_STARTED",
                "taskId", taskId,
                "round", round,
                "totalRounds", totalRounds,
                "algorithm", algorithm,
                "roundStartTime", Instant.now().toString(),
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理轮次开始失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.ROUND_START_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "轮次开始失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理轮次完成消息
     * 服务器通知VM当前训练轮次已完成
     */
    private ProtocolAck onRoundComplete(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        Integer totalRounds = numberAsInt(data, "totalRounds");
        String performance = valueAsString(data, "performance");

        log.info("处理轮次完成请求: vmId={}, taskId={}, round={}/{}, performance={}",
                vmId, taskId, round, totalRounds, performance);

        try {
            // 发送轮次完成通知给所有参与的VM
            ProtocolMessage roundCompleteMsg = ProtocolMessage.builder()
                    .type(ProtocolType.ROUND_COMPLETE)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                        "taskId", taskId,
                        "round", round,
                        "totalRounds", totalRounds,
                        "performance", performance,
                        "message", "第" + round + "轮训练完成",
                        "roundEndTime", Instant.now().toString(),
                        "isLastRound", round.equals(totalRounds)
                    ))
                    .build();

            // 广播给所有VM
            messagingTemplate.convertAndSend("/topic/vm", roundCompleteMsg);

            log.info("轮次完成通知已发送: taskId={}, round={}/{}, performance={}, isLastRound={}",
                    taskId, round, totalRounds, performance, round.equals(totalRounds));

            return ackFor(msg, ProtocolType.ROUND_COMPLETE_ACK, mapOf(
                "status", "ROUND_COMPLETED",
                "taskId", taskId,
                "round", round,
                "totalRounds", totalRounds,
                "performance", performance,
                "isLastRound", round.equals(totalRounds),
                "roundEndTime", Instant.now().toString(),
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理轮次完成失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.ROUND_COMPLETE_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "轮次完成处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理模型类型协商消息
     * 服务器与VM协商使用的模型类型和参数
     */
    private ProtocolAck onModelTypeNegotiation(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        String requestedModelType = valueAsString(data, "modelType");
        Map<String, Object> modelConfig = (Map<String, Object>) data.get("modelConfig");

        log.info("处理模型类型协商: vmId={}, taskId={}, requestedModelType={}",
                vmId, taskId, requestedModelType);

        try {
            // 支持的模型类型列表
            String[] supportedModels = {"RandomForest", "SVM", "LogisticRegression", "NeuralNetwork", "XGBoost"};
            boolean isSupported = false;
            for (String model : supportedModels) {
                if (model.equalsIgnoreCase(requestedModelType)) {
                    isSupported = true;
                    break;
                }
            }

            String finalModelType;
            String negotiationStatus;

            if (isSupported) {
                finalModelType = requestedModelType;
                negotiationStatus = "ACCEPTED";
                log.info("模型类型协商成功: vmId={}, taskId={}, modelType={}", vmId, taskId, finalModelType);
            } else {
                finalModelType = "RandomForest";  // 默认模型
                negotiationStatus = "FALLBACK";
                log.warn("不支持的模型类型，使用默认模型: vmId={}, taskId={}, requested={}, fallback={}",
                        vmId, taskId, requestedModelType, finalModelType);
            }

            // 发送协商结果通知
            ProtocolMessage negotiationMsg = ProtocolMessage.builder()
                    .type(ProtocolType.MODEL_TYPE_NEGOTIATION)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                        "taskId", taskId,
                        "modelType", finalModelType,
                        "modelConfig", modelConfig,
                        "negotiationStatus", negotiationStatus,
                        "supportedModels", supportedModels,
                        "message", negotiationStatus.equals("ACCEPTED") ?
                                  "模型类型协商成功" : "已切换到默认模型类型"
                    ))
                    .build();

            // 发送给请求的VM
            messagingTemplate.convertAndSend("/topic/vm/" + vmId, negotiationMsg);

            return ackFor(msg, ProtocolType.MODEL_TYPE_NEGOTIATION_ACK, mapOf(
                "status", negotiationStatus,
                "taskId", taskId,
                "finalModelType", finalModelType,
                "modelConfig", modelConfig,
                "supportedModels", supportedModels,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("模型类型协商失败: vmId={}, taskId={}, error={}",
                     vmId, taskId, e.getMessage(), e);
            return ackFor(msg, ProtocolType.MODEL_TYPE_NEGOTIATION_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "模型类型协商失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理模型类型协商确认消息
     * VM确认收到模型类型协商结果
     */
    private ProtocolAck onModelTypeNegotiationAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        String confirmedModelType = valueAsString(data, "modelType");
        String negotiationStatus = valueAsString(data, "status");

        log.info("收到模型类型协商确认: vmId={}, taskId={}, modelType={}, status={}",
                vmId, taskId, confirmedModelType, negotiationStatus);

        try {
            if ("ACCEPTED".equals(negotiationStatus)) {
                log.info("虚拟机{}确认使用模型类型: taskId={}, modelType={}",
                        vmId, taskId, confirmedModelType);
                // TODO: 可以在这里记录VM的模型类型配置
            } else if ("FALLBACK".equals(negotiationStatus)) {
                log.info("虚拟机{}接受默认模型类型: taskId={}, modelType={}",
                        vmId, taskId, confirmedModelType);
            } else {
                String errorMessage = valueAsString(data, "errorMessage");
                log.warn("虚拟机{}模型类型协商异常: taskId={}, error={}",
                        vmId, taskId, errorMessage);
            }

            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ACK_PROCESSED",
                "vmId", vmId,
                "taskId", taskId,
                "confirmedModelType", confirmedModelType,
                "negotiationStatus", negotiationStatus,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理模型类型协商确认失败: vmId={}, taskId={}, error={}",
                     vmId, taskId, e.getMessage(), e);
            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "处理模型类型协商确认失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理算法配置消息
     * 服务器配置联邦学习算法参数
     */
    private ProtocolAck onAlgorithmConfig(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        String algorithm = valueAsString(data, "algorithm");
        Map<String, Object> algorithmParams = (Map<String, Object>) data.get("algorithmParams");

        log.info("处理算法配置: vmId={}, taskId={}, algorithm={}",
                vmId, taskId, algorithm);

        try {
            // 验证算法类型
            String[] supportedAlgorithms = {"FedAvg", "FedProx", "FedOpt", "FedNova", "SCAFFOLD"};
            boolean isSupported = false;
            for (String alg : supportedAlgorithms) {
                if (alg.equalsIgnoreCase(algorithm)) {
                    isSupported = true;
                    break;
                }
            }

            if (!isSupported) {
                log.warn("不支持的算法类型: {}, 使用默认FedAvg", algorithm);
                algorithm = "FedAvg";
            }

            // 设置默认参数
            Map<String, Object> finalParams = new HashMap<>();
            if (algorithmParams != null) {
                finalParams.putAll(algorithmParams);
            }

            // 添加算法特定的默认参数
            switch (algorithm.toLowerCase()) {
                case "fedavg":
                    finalParams.putIfAbsent("learningRate", 0.01);
                    finalParams.putIfAbsent("momentum", 0.9);
                    break;
                case "fedprox":
                    finalParams.putIfAbsent("learningRate", 0.01);
                    finalParams.putIfAbsent("proximalTerm", 0.1);
                    finalParams.putIfAbsent("momentum", 0.9);
                    break;
                case "fedopt":
                    finalParams.putIfAbsent("serverLearningRate", 1.0);
                    finalParams.putIfAbsent("clientLearningRate", 0.01);
                    finalParams.putIfAbsent("beta1", 0.9);
                    finalParams.putIfAbsent("beta2", 0.999);
                    break;
                default:
                    finalParams.putIfAbsent("learningRate", 0.01);
                    break;
            }

            // 发送算法配置给所有VM
            ProtocolMessage configMsg = ProtocolMessage.builder()
                    .type(ProtocolType.ALGORITHM_CONFIG)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                        "taskId", taskId,
                        "algorithm", algorithm,
                        "algorithmParams", finalParams,
                        "supportedAlgorithms", supportedAlgorithms,
                        "message", "算法配置已更新: " + algorithm
                    ))
                    .build();

            // 广播给所有VM
            messagingTemplate.convertAndSend("/topic/vm", configMsg);

            log.info("算法配置已发送: taskId={}, algorithm={}, params={}",
                    taskId, algorithm, finalParams);

            return ackFor(msg, ProtocolType.ALGORITHM_CONFIG_ACK, mapOf(
                "status", "CONFIG_APPLIED",
                "taskId", taskId,
                "algorithm", algorithm,
                "algorithmParams", finalParams,
                "supportedAlgorithms", supportedAlgorithms,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("算法配置失败: vmId={}, taskId={}, algorithm={}, error={}",
                     vmId, taskId, algorithm, e.getMessage(), e);
            return ackFor(msg, ProtocolType.ALGORITHM_CONFIG_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "算法配置失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理算法配置确认消息
     * VM确认已收到并应用算法配置
     */
    private ProtocolAck onAlgorithmConfigAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        String algorithm = valueAsString(data, "algorithm");
        String configStatus = valueAsString(data, "status");
        Map<String, Object> appliedParams = (Map<String, Object>) data.get("appliedParams");

        log.info("收到算法配置确认: vmId={}, taskId={}, algorithm={}, status={}",
                vmId, taskId, algorithm, configStatus);

        try {
            if ("CONFIG_APPLIED".equals(configStatus)) {
                log.info("虚拟机{}成功应用算法配置: taskId={}, algorithm={}, params={}",
                        vmId, taskId, algorithm, appliedParams);
                // TODO: 可以在这里记录VM的算法配置状态
            } else if ("CONFIG_PARTIAL".equals(configStatus)) {
                log.warn("虚拟机{}部分应用算法配置: taskId={}, algorithm={}",
                        vmId, taskId, algorithm);
            } else {
                String errorMessage = valueAsString(data, "errorMessage");
                log.error("虚拟机{}算法配置失败: taskId={}, algorithm={}, error={}",
                        vmId, taskId, algorithm, errorMessage);
            }

            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ACK_PROCESSED",
                "vmId", vmId,
                "taskId", taskId,
                "algorithm", algorithm,
                "configStatus", configStatus,
                "appliedParams", appliedParams,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理算法配置确认失败: vmId={}, taskId={}, error={}",
                     vmId, taskId, e.getMessage(), e);
            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "处理算法配置确认失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理梯度上传准备消息
     * 服务器通知VM准备梯度上传，进行预检查和资源分配
     */
    private ProtocolAck onGradientUploadPrepare(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        Integer expectedDataSize = numberAsInt(data, "expectedDataSize");
        String compressionType = valueAsString(data, "compressionType");

        log.info("处理梯度上传准备: vmId={}, taskId={}, round={}, expectedSize={}MB, compression={}",
                vmId, taskId, round, expectedDataSize != null ? expectedDataSize/1024/1024 : "未知", compressionType);

        try {
            // 预检查资源和配置
            boolean resourceAvailable = true;
            String uploadToken = "upload_" + taskId + "_" + round + "_" + System.currentTimeMillis();

            // 检查存储空间（模拟检查）
            long availableSpace = 1024 * 1024 * 1024; // 1GB 模拟可用空间
            if (expectedDataSize != null && expectedDataSize > availableSpace) {
                resourceAvailable = false;
            }

            // 支持的压缩类型
            String[] supportedCompression = {"none", "gzip", "lz4", "snappy"};
            String finalCompressionType = "none";
            if (compressionType != null) {
                for (String type : supportedCompression) {
                    if (type.equalsIgnoreCase(compressionType)) {
                        finalCompressionType = compressionType;
                        break;
                    }
                }
            }

            // 发送准备就绪通知
            ProtocolMessage prepareMsg = ProtocolMessage.builder()
                    .type(ProtocolType.GRADIENT_UPLOAD_PREPARE)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                        "taskId", taskId,
                        "round", round,
                        "uploadToken", uploadToken,
                        "resourceAvailable", resourceAvailable,
                        "maxDataSize", availableSpace,
                        "compressionType", finalCompressionType,
                        "supportedCompression", supportedCompression,
                        "uploadEndpoint", "/api/gradient-upload",
                        "timeout", 300, // 5分钟超时
                        "message", resourceAvailable ? "准备就绪，可以开始上传" : "资源不足，请稍后重试"
                    ))
                    .build();

            // 发送给请求的VM
            messagingTemplate.convertAndSend("/topic/vm/" + vmId, prepareMsg);

            log.info("梯度上传准备通知已发送: vmId={}, taskId={}, round={}, resourceAvailable={}, token={}",
                    vmId, taskId, round, resourceAvailable, uploadToken);

            return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_PREPARE_ACK, mapOf(
                "status", resourceAvailable ? "READY" : "RESOURCE_UNAVAILABLE",
                "taskId", taskId,
                "round", round,
                "uploadToken", uploadToken,
                "resourceAvailable", resourceAvailable,
                "maxDataSize", availableSpace,
                "compressionType", finalCompressionType,
                "uploadEndpoint", "/api/gradient-upload",
                "timeout", 300,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("梯度上传准备失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_PREPARE_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "梯度上传准备失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 向指定VM广播全局模型
     */
    public void broadcastGlobalModel(String taskId, Integer roundNumber, Map<String, Object> globalModel, List<String> targetVmIds) {
        try {
            ProtocolMessage broadcastMsg = ProtocolMessage.builder()
                    // TODO: 暂时使用STATUS_RESPONSE替代不存在的枚举
                    .type(ProtocolType.STATUS_RESPONSE)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "globalModel", globalModel,
                            "timestamp", Instant.now().toString()
                    ))
                    .build();

            for (String vmId : targetVmIds) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, broadcastMsg);
                System.out.println("📡 向VM " + vmId + " 广播全局模型, 任务=" + taskId + ", 轮次=" + roundNumber);
            }
        } catch (Exception e) {
            System.err.println("广播全局模型失败: " + e.getMessage());
        }
    }

    /**
     * 通知聚合开始
     */
    public void notifyAggregationStart(String taskId, Integer roundNumber, List<String> targetVmIds) {
        try {
            ProtocolMessage notifyMsg = ProtocolMessage.builder()
                    // TODO: 暂时使用STATUS_RESPONSE替代不存在的枚举
                    .type(ProtocolType.STATUS_RESPONSE)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "timestamp", Instant.now().toString()
                    ))
                    .build();

            for (String vmId : targetVmIds) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, notifyMsg);
                System.out.println("🔄 通知VM " + vmId + " 聚合开始, 任务=" + taskId + ", 轮次=" + roundNumber);
            }
        } catch (Exception e) {
            System.err.println("通知聚合开始失败: " + e.getMessage());
        }
    }

    /**
     * 通知聚合完成
     */
    public void notifyAggregationComplete(String taskId, Integer roundNumber, Map<String, Object> aggregationResults, List<String> targetVmIds) {
        try {
            ProtocolMessage notifyMsg = ProtocolMessage.builder()
                    // TODO: 暂时使用STATUS_RESPONSE替代不存在的枚举
                    .type(ProtocolType.STATUS_RESPONSE)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "aggregationResults", aggregationResults,
                            "timestamp", Instant.now().toString()
                    ))
                    .build();

            for (String vmId : targetVmIds) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, notifyMsg);
                System.out.println("✅ 通知VM " + vmId + " 聚合完成, 任务=" + taskId + ", 轮次=" + roundNumber);
            }
        } catch (Exception e) {
            System.err.println("通知聚合完成失败: " + e.getMessage());
        }
    }

    /**
     * 通知轮次开始
     */
    public void notifyRoundStart(String taskId, Integer roundNumber, Map<String, Object> roundConfig, List<String> targetVmIds) {
        try {
            ProtocolMessage notifyMsg = ProtocolMessage.builder()
                    // TODO: 暂时使用STATUS_RESPONSE替代不存在的枚举
                    .type(ProtocolType.STATUS_RESPONSE)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "roundConfig", roundConfig,
                            "timestamp", Instant.now().toString()
                    ))
                    .build();

            for (String vmId : targetVmIds) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, notifyMsg);
                System.out.println("🏁 通知VM " + vmId + " 轮次开始, 任务=" + taskId + ", 轮次=" + roundNumber);
            }
        } catch (Exception e) {
            System.err.println("通知轮次开始失败: " + e.getMessage());
        }
    }

    /**
     * 通知轮次完成
     */
    public void notifyRoundComplete(String taskId, Integer roundNumber, Map<String, Object> roundResults, List<String> targetVmIds) {
        try {
            ProtocolMessage notifyMsg = ProtocolMessage.builder()
                    // TODO: 暂时使用STATUS_RESPONSE替代不存在的枚举
                    .type(ProtocolType.STATUS_RESPONSE)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "roundResults", roundResults,
                            "timestamp", Instant.now().toString()
                    ))
                    .build();

            for (String vmId : targetVmIds) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, notifyMsg);
                System.out.println("🏆 通知VM " + vmId + " 轮次完成, 任务=" + taskId + ", 轮次=" + roundNumber);
            }
        } catch (Exception e) {
            System.err.println("通知轮次完成失败: " + e.getMessage());
        }
    }

    /**
     * 处理梯度上传消息
     * 虚拟机将本地训练后的梯度/参数上传至后端
     */
    private ProtocolAck onGradientUpload(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");

        log.info("收到梯度上传: vmId={}, taskId={}, round={}", vmId, taskId, round);

        try {
            // 提取训练结果数据
            @SuppressWarnings("unchecked")
            Map<String, Object> trainingResult = (Map<String, Object>) data.get("training_result");

            if (trainingResult != null) {
                // 提取模型参数和元数据
                @SuppressWarnings("unchecked")
                Map<String, Object> modelParams = (Map<String, Object>)
                    trainingResult.get("model_parameters");
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>)
                    trainingResult.get("training_metadata");

                // 构建存储格式
                Map<String, Object> storeParams = new HashMap<>();
                if (modelParams != null) {
                    storeParams.put("model_parameters", modelParams);
                }
                if (metadata != null) {
                    storeParams.put("training_metadata", metadata);
                }
                if (trainingResult.get("parameter_deltas") != null) {
                    storeParams.put("parameter_deltas", trainingResult.get("parameter_deltas"));
                }

                // 提取准确率和损失值
                Double accuracy = null;
                Double loss = null;
                if (metadata != null) {
                    accuracy = numberAsDouble(metadata, "local_accuracy");
                    loss = numberAsDouble(metadata, "loss");
                }

                // 序列化参数
                String parametersJson = toJsonSafe(storeParams);

                // 存储到vm_round_models表
                vmRoundModelsMapper.upsertRoundModel(
                    uuidUtil.generateUuid(),
                    taskId,
                    vmId,
                    round,
                    accuracy,
                    loss,
                    parametersJson
                );

                // 发布模型上传事件触发聚合检查
                ModelUploadEvent uploadEvent = new ModelUploadEvent(
                    this,
                    taskId,
                    round,
                    vmId,
                    (long) parametersJson.length(),
                    accuracy,
                    loss
                );
                eventPublisher.publishEvent(uploadEvent);

                log.info("梯度上传成功: vmId={}, taskId={}, round={}, 参数大小={}字节",
                        vmId, taskId, round, parametersJson.length());

                return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                    "status", "RECEIVED",
                    "taskId", taskId,
                    "round", round,
                    "timestamp", Instant.now().toString(),
                    "parametersSize", parametersJson.length()
                ));
            } else {
                log.warn("梯度上传数据为空: vmId={}, taskId={}, round={}", vmId, taskId, round);
                return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                    "status", "ERROR",
                    "errorMessage", "训练结果数据为空",
                    "taskId", taskId,
                    "round", round
                ));
            }
        } catch (Exception e) {
            log.error("梯度上传处理失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "梯度上传处理失败: " + e.getMessage(),
                "taskId", taskId,
                "round", round
            ));
        }
    }

    /**
     * 处理全局模型广播确认消息
     * 虚拟机确认已收到全局模型广播
     */
    private ProtocolAck onGlobalModelBroadcastAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String status = valueAsString(data, "status");

        log.info("收到全局模型广播确认: vmId={}, taskId={}, round={}, status={}",
                vmId, taskId, round, status);

        try {
            // 记录确认状态
            if ("RECEIVED".equals(status)) {
                log.info("虚拟机{}成功接收全局模型: taskId={}, round={}", vmId, taskId, round);
                // 这里可以记录到数据库或更新分发状态
                // TODO: 如果需要跟踪模型分发状态，可以在这里更新数据库
            } else if ("ERROR".equals(status)) {
                String errorMessage = valueAsString(data, "errorMessage");
                log.warn("虚拟机{}接收全局模型失败: taskId={}, round={}, error={}",
                        vmId, taskId, round, errorMessage);
                // TODO: 处理接收失败的情况，可能需要重新发送
            }

            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ACK_RECEIVED",
                "vmId", vmId,
                "taskId", taskId,
                "round", round,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理全局模型广播确认失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "处理确认失败: " + e.getMessage(),
                "vmId", vmId,
                "taskId", taskId,
                "round", round
            ));
        }
    }

    // ================= v1.4 联邦学习增强协议 - 剩余处理器 =================

    /**
     * 处理梯度上传准备确认消息
     * 虚拟机确认已收到梯度上传准备通知并准备好上传
     */
    private ProtocolAck onGradientUploadPrepareAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String status = valueAsString(data, "status");
        String uploadToken = valueAsString(data, "uploadToken");

        log.info("收到梯度上传准备确认: vmId={}, taskId={}, round={}, status={}, token={}",
                vmId, taskId, round, status, uploadToken);

        try {
            // 验证上传令牌
            if (uploadToken == null || uploadToken.isEmpty()) {
                log.warn("梯度上传准备确认缺少上传令牌: vmId={}, taskId={}, round={}", vmId, taskId, round);
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorType", "INVALID_TOKEN",
                    "errorMessage", "上传令牌无效或缺失",
                    "vmId", vmId,
                    "taskId", taskId,
                    "round", round
                ));
            }

            // 处理不同的确认状态
            switch (status) {
                case "READY":
                    log.info("虚拟机{}已准备就绪，可以开始梯度上传: taskId={}, round={}", vmId, taskId, round);
                    // TODO: 记录VM准备状态，可以开始接收梯度
                    // 可以在这里更新数据库状态或设置定时器等待上传

                    // 确认收到准备状态，告知VM可以开始上传
                    return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                        "status", "CONFIRMED",
                        "message", "确认收到准备状态，可以开始梯度上传",
                        "vmId", vmId,
                        "taskId", taskId,
                        "round", round,
                        "uploadToken", uploadToken,
                        "nextAction", "START_UPLOAD",
                        "timestamp", Instant.now().toString()
                    ));

                case "NOT_READY":
                    String reason = valueAsString(data, "reason");
                    log.warn("虚拟机{}未准备好进行梯度上传: vmId={}, taskId={}, round={}, reason={}",
                            vmId, taskId, round, reason);

                    return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                        "status", "NOT_READY_ACK",
                        "message", "确认收到未准备状态",
                        "vmId", vmId,
                        "taskId", taskId,
                        "round", round,
                        "reason", reason,
                        "nextAction", "RETRY_LATER",
                        "timestamp", Instant.now().toString()
                    ));

                case "ERROR":
                    String errorMessage = valueAsString(data, "errorMessage");
                    log.error("虚拟机{}梯度上传准备出错: vmId={}, taskId={}, round={}, error={}",
                             vmId, taskId, round, errorMessage);

                    return ackFor(msg, ProtocolType.ERROR, mapOf(
                        "errorType", "GRADIENT_UPLOAD_PREPARE_ERROR",
                        "errorMessage", "虚拟机梯度上传准备失败: " + errorMessage,
                        "vmId", vmId,
                        "taskId", taskId,
                        "round", round,
                        "timestamp", Instant.now().toString()
                    ));

                default:
                    log.warn("未知的梯度上传准备确认状态: vmId={}, taskId={}, round={}, status={}",
                            vmId, taskId, round, status);

                    return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                        "status", "UNKNOWN_STATUS_ACK",
                        "message", "收到未知状态确认: " + status,
                        "vmId", vmId,
                        "taskId", taskId,
                        "round", round,
                        "timestamp", Instant.now().toString()
                    ));
            }

        } catch (Exception e) {
            log.error("处理梯度上传准备确认失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorType", "PROCESSING_ERROR",
                "errorMessage", "处理梯度上传准备确认失败: " + e.getMessage(),
                "vmId", vmId,
                "taskId", taskId,
                "round", round
            ));
        }
    }

    /**
     * 处理聚合通知消息
     * 通知虚拟机聚合过程的状态更新
     */
    private ProtocolAck onAggregationNotification(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String notificationType = valueAsString(data, "notificationType");
        String message = valueAsString(data, "message");

        log.info("处理聚合通知: vmId={}, taskId={}, round={}, type={}, message={}",
                vmId, taskId, round, notificationType, message);

        try {
            // 根据通知类型进行处理
            switch (notificationType) {
                case "AGGREGATION_STARTED":
                    log.info("聚合已开始: taskId={}, round={}", taskId, round);
                    // 向所有相关VM广播聚合开始通知
                    // TODO: 获取任务相关的所有VM列表
                    // notifyAggregationStart(taskId, round, getAllVmIdsForTask(taskId));
                    break;

                case "AGGREGATION_COMPLETED":
                    log.info("聚合已完成: taskId={}, round={}", taskId, round);
                    // 处理聚合完成后的逻辑
                    @SuppressWarnings("unchecked")
                    Map<String, Object> aggregationResult = (Map<String, Object>) valueAsObject(data, "aggregationResult");
                    if (aggregationResult != null) {
                        log.info("聚合结果: {}", aggregationResult);
                        // TODO: 保存聚合结果到数据库或执行后续流程
                    }
                    break;

                case "AGGREGATION_PROGRESS":
                    Integer completedParticipants = numberAsInt(data, "completedParticipants");
                    Integer totalParticipants = numberAsInt(data, "totalParticipants");
                    log.info("聚合进度更新: taskId={}, round={}, completed={}/{}",
                            taskId, round, completedParticipants, totalParticipants);
                    break;

                case "AGGREGATION_ERROR":
                    String errorMessage = valueAsString(data, "errorMessage");
                    log.error("聚合过程出错: taskId={}, round={}, error={}", taskId, round, errorMessage);
                    // TODO: 处理聚合错误，可能需要重启聚合或通知相关VM
                    break;

                default:
                    log.warn("未知的聚合通知类型: taskId={}, round={}, type={}", taskId, round, notificationType);
                    break;
            }

            // 转发通知给相关的VM（如果这是服务端发起的通知）
            if (!"server".equals(vmId)) {
                sendToVmTopic(vmId, MessageBuilder.createNotification(
                    ProtocolType.AGGREGATION_NOTIFICATION,
                    "聚合通知",
                    message,
                    mapOf(
                        "taskId", taskId,
                        "round", round,
                        "notificationType", notificationType,
                        "timestamp", Instant.now().toString()
                    )
                ));
            }

            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "NOTIFICATION_PROCESSED",
                "message", "聚合通知已处理",
                "taskId", taskId,
                "round", round,
                "notificationType", notificationType,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理聚合通知失败: vmId={}, taskId={}, round={}, type={}, error={}",
                     vmId, taskId, round, notificationType, e.getMessage(), e);
            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorType", "AGGREGATION_NOTIFICATION_ERROR",
                "errorMessage", "处理聚合通知失败: " + e.getMessage(),
                "taskId", taskId,
                "round", round,
                "notificationType", notificationType
            ));
        }
    }

    /**
     * 处理策略切换通知消息
     * 通知虚拟机联邦学习策略已经切换
     */
    private ProtocolAck onStrategySwitchNotification(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        String oldStrategy = valueAsString(data, "oldStrategy");
        String newStrategy = valueAsString(data, "newStrategy");
        Integer switchRound = numberAsInt(data, "switchRound");
        String reason = valueAsString(data, "reason");
        @SuppressWarnings("unchecked")
        Map<String, Object> newConfig = (Map<String, Object>) valueAsObject(data, "newConfig");

        log.info("处理策略切换通知: vmId={}, taskId={}, {} -> {}, round={}, reason={}",
                vmId, taskId, oldStrategy, newStrategy, switchRound, reason);

        try {
            // 验证新策略的有效性
            String[] supportedStrategies = {"FedAvg", "FedProx", "FedOpt", "FedNova", "SCAFFOLD"};
            boolean isValidStrategy = false;
            for (String strategy : supportedStrategies) {
                if (strategy.equalsIgnoreCase(newStrategy)) {
                    isValidStrategy = true;
                    break;
                }
            }

            if (!isValidStrategy) {
                log.warn("不支持的策略: {}, 支持的策略: {}", newStrategy, java.util.Arrays.toString(supportedStrategies));
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorType", "UNSUPPORTED_STRATEGY",
                    "errorMessage", "不支持的联邦学习策略: " + newStrategy,
                    "supportedStrategies", supportedStrategies,
                    "taskId", taskId
                ));
            }

            // 准备策略切换配置
            Map<String, Object> strategyConfig = new HashMap<>();
            strategyConfig.put("algorithm", newStrategy);
            strategyConfig.put("switchRound", switchRound);
            strategyConfig.put("switchTimestamp", Instant.now().toString());
            strategyConfig.put("reason", reason);

            // 合并新配置
            if (newConfig != null) {
                strategyConfig.putAll(newConfig);
            }

            // 根据新策略设置默认参数
            switch (newStrategy.toLowerCase()) {
                case "fedavg":
                    strategyConfig.putIfAbsent("learningRate", 0.01);
                    strategyConfig.putIfAbsent("momentum", 0.9);
                    break;
                case "fedprox":
                    strategyConfig.putIfAbsent("learningRate", 0.01);
                    strategyConfig.putIfAbsent("proximalTerm", 0.01);
                    break;
                case "fedopt":
                    strategyConfig.putIfAbsent("serverLearningRate", 1.0);
                    strategyConfig.putIfAbsent("serverMomentum", 0.9);
                    break;
                case "fednova":
                    strategyConfig.putIfAbsent("learningRate", 0.01);
                    strategyConfig.putIfAbsent("normalizationFactor", 1.0);
                    break;
                case "scaffold":
                    strategyConfig.putIfAbsent("learningRate", 0.01);
                    strategyConfig.putIfAbsent("controlVariateWeight", 1.0);
                    break;
            }

            // 更新任务配置中的策略信息
            try {
                // TODO: 更新数据库中的任务策略配置
                // federatedTasksMapper.updateTaskConfig(taskId, toJsonSafe(strategyConfig));
                log.info("策略配置已准备完成: taskId={}, strategy={}, config={}", taskId, newStrategy, strategyConfig);
            } catch (Exception e) {
                log.warn("更新任务策略配置失败: taskId={}, error={}", taskId, e.getMessage());
            }

            // 向目标VM发送策略切换确认
            sendToVmTopic(vmId, MessageBuilder.createNotification(
                ProtocolType.STRATEGY_SWITCH_ACK,
                "策略切换确认",
                String.format("策略已切换: %s -> %s", oldStrategy, newStrategy),
                mapOf(
                    "taskId", taskId,
                    "oldStrategy", oldStrategy,
                    "newStrategy", newStrategy,
                    "switchRound", switchRound,
                    "strategyConfig", strategyConfig,
                    "status", "SWITCHED",
                    "timestamp", Instant.now().toString()
                )
            ));

            log.info("策略切换通知处理完成: vmId={}, taskId={}, newStrategy={}", vmId, taskId, newStrategy);

            return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                "status", "STRATEGY_SWITCH_PROCESSED",
                "message", "策略切换通知已处理",
                "taskId", taskId,
                "oldStrategy", oldStrategy,
                "newStrategy", newStrategy,
                "switchRound", switchRound,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理策略切换通知失败: vmId={}, taskId={}, strategy={}, error={}",
                     vmId, taskId, newStrategy, e.getMessage(), e);
            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorType", "STRATEGY_SWITCH_ERROR",
                "errorMessage", "处理策略切换通知失败: " + e.getMessage(),
                "taskId", taskId,
                "newStrategy", newStrategy
            ));
        }
    }

    /**
     * 处理策略切换确认消息
     * 虚拟机确认已收到策略切换通知并应用了新策略
     */
    private ProtocolAck onStrategySwitchAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        String newStrategy = valueAsString(data, "newStrategy");
        Integer switchRound = numberAsInt(data, "switchRound");
        String status = valueAsString(data, "status");
        String confirmMessage = valueAsString(data, "message");

        log.info("收到策略切换确认: vmId={}, taskId={}, strategy={}, round={}, status={}, message={}",
                vmId, taskId, newStrategy, switchRound, status, confirmMessage);

        try {
            // 处理不同的确认状态
            switch (status) {
                case "APPLIED":
                    log.info("虚拟机{}已成功应用新策略: taskId={}, strategy={}, round={}",
                            vmId, taskId, newStrategy, switchRound);

                    // TODO: 记录策略切换成功状态到数据库
                    // 可以更新VM状态或任务进度

                    return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                        "status", "SWITCH_CONFIRMED",
                        "message", "确认虚拟机已应用新策略",
                        "vmId", vmId,
                        "taskId", taskId,
                        "newStrategy", newStrategy,
                        "switchRound", switchRound,
                        "timestamp", Instant.now().toString()
                    ));

                case "FAILED":
                    String errorReason = valueAsString(data, "errorReason");
                    log.error("虚拟机{}策略切换失败: taskId={}, strategy={}, round={}, reason={}",
                             vmId, taskId, newStrategy, switchRound, errorReason);

                    // TODO: 处理策略切换失败，可能需要回滚或重试

                    return ackFor(msg, ProtocolType.ERROR, mapOf(
                        "errorType", "STRATEGY_SWITCH_FAILED",
                        "errorMessage", "虚拟机策略切换失败: " + errorReason,
                        "vmId", vmId,
                        "taskId", taskId,
                        "newStrategy", newStrategy,
                        "switchRound", switchRound,
                        "errorReason", errorReason,
                        "timestamp", Instant.now().toString()
                    ));

                case "PARTIAL":
                    String partialReason = valueAsString(data, "partialReason");
                    log.warn("虚拟机{}策略部分应用: taskId={}, strategy={}, round={}, reason={}",
                            vmId, taskId, newStrategy, switchRound, partialReason);

                    return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                        "status", "PARTIAL_SWITCH_ACK",
                        "message", "确认策略部分应用",
                        "vmId", vmId,
                        "taskId", taskId,
                        "newStrategy", newStrategy,
                        "switchRound", switchRound,
                        "partialReason", partialReason,
                        "recommendAction", "MONITOR_AND_RETRY",
                        "timestamp", Instant.now().toString()
                    ));

                default:
                    log.warn("未知的策略切换确认状态: vmId={}, taskId={}, status={}", vmId, taskId, status);

                    return ackFor(msg, ProtocolType.STATUS_RESPONSE, mapOf(
                        "status", "UNKNOWN_STATUS_ACK",
                        "message", "收到未知状态确认: " + status,
                        "vmId", vmId,
                        "taskId", taskId,
                        "newStrategy", newStrategy,
                        "switchRound", switchRound,
                        "timestamp", Instant.now().toString()
                    ));
            }

        } catch (Exception e) {
            log.error("处理策略切换确认失败: vmId={}, taskId={}, strategy={}, error={}",
                     vmId, taskId, newStrategy, e.getMessage(), e);
            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorType", "PROCESSING_ERROR",
                "errorMessage", "处理策略切换确认失败: " + e.getMessage(),
                "vmId", vmId,
                "taskId", taskId,
                "newStrategy", newStrategy
            ));
        }
    }

} 