package com.feduwacomm.utils;

import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.service.DigitalSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket消息构建工具类
 * 符合协议v1.4标准，集成MessageIdGenerator
 * 提供统一的消息构建接口，确保类型安全和一致性
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageBuilder {

    private final MessageIdGenerator messageIdGenerator;
    private final DigitalSignatureService digitalSignatureService;

    /**
     * 创建服务端标准协议消息
     * 符合协议v1.4标准格式
     *
     * @param type 消息类型
     * @param vmId 虚拟机ID
     * @param data 消息数据
     * @return 标准ProtocolMessage对象
     */
    public ProtocolMessage buildServerMessage(ProtocolType type, String vmId, Map<String, Object> data) {
        return ProtocolMessage.builder()
                .type(type)
                .id(messageIdGenerator.generateServerMessageId())
                .timestamp(Instant.now())
                .vmId(vmId)
                .data(data != null ? data : new HashMap<>())
                .signature(generateSignature(type, vmId, data))
                .build();
    }

    /**
     * 创建命令消息
     * 符合协议v1.4标准格式
     *
     * @param type 消息类型
     * @param vmId 虚拟机ID
     * @param data 消息数据
     * @return 标准ProtocolMessage对象
     */
    public ProtocolMessage buildCommandMessage(ProtocolType type, String vmId, Map<String, Object> data) {
        return ProtocolMessage.builder()
                .type(type)
                .id(messageIdGenerator.generateCommandMessageId())
                .timestamp(Instant.now())
                .vmId(vmId)
                .data(data != null ? data : new HashMap<>())
                .signature(generateSignature(type, vmId, data))
                .build();
    }


    /**
     * 创建ACK响应消息
     * 根据原始消息类型自动生成对应的ACK类型
     *
     * @param originalType 原始消息类型
     * @param vmId 虚拟机ID
     * @param status 状态信息
     * @param message 响应消息
     * @return ACK响应消息
     */
    public ProtocolMessage buildAckMessage(ProtocolType originalType, String vmId, String status, String message) {
        ProtocolType ackType = getAckType(originalType);
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("message", message);
        data.put("originalType", originalType.name());

        return buildServerMessage(ackType, vmId, data);
    }

    /**
     * 创建成功ACK响应
     *
     * @param originalType 原始消息类型
     * @param vmId 虚拟机ID
     * @param message 成功消息
     * @return 成功ACK响应
     */
    public ProtocolMessage buildSuccessAck(ProtocolType originalType, String vmId, String message) {
        return buildAckMessage(originalType, vmId, "SUCCESS", message);
    }

    /**
     * 创建失败ACK响应
     *
     * @param originalType 原始消息类型
     * @param vmId 虚拟机ID
     * @param errorMessage 错误消息
     * @return 失败ACK响应
     */
    public ProtocolMessage buildErrorAck(ProtocolType originalType, String vmId, String errorMessage) {
        return buildAckMessage(originalType, vmId, "ERROR", errorMessage);
    }

    // ==================== 辅助方法 ====================

    /**
     * 根据原始消息类型获取对应的ACK类型
     *
     * @param originalType 原始消息类型
     * @return 对应的ACK消息类型
     */
    /**
     * 获取对应的ACK类型 - v1.4协议版本
     * 只包含v1.4标准支持的34个核心协议类型
     */
    private ProtocolType getAckType(ProtocolType originalType) {
        switch (originalType) {
            // 连接管理层
            case CONNECT:
                return ProtocolType.CONNECT_ACK;
            case HEARTBEAT:
                return ProtocolType.HEARTBEAT_ACK;

            // 任务管理层
            case FEDERATED_TASK_START:
                return ProtocolType.FEDERATED_TASK_START_ACK;
            case FEDERATED_TASK_STOP:
                return ProtocolType.FEDERATED_TASK_STOP_ACK;
            case FEDERATED_TASK_RESUME:
                return ProtocolType.FEDERATED_TASK_RESUME_ACK;
            case FEDERATED_TASK_DELETE:
                return ProtocolType.FEDERATED_TASK_DELETE_ACK;
            case FEDERATED_TASK_STATUS_QUERY:
                return ProtocolType.FEDERATED_TASK_STATUS_RESPONSE;

            // 轮次管理层
            case ROUND_START:
                return ProtocolType.ROUND_START_ACK;
            case GRADIENT_UPLOAD:
                return ProtocolType.GRADIENT_UPLOAD_ACK;
            case GLOBAL_MODEL_BROADCAST:
                return ProtocolType.GLOBAL_MODEL_BROADCAST_ACK;
            case ROUND_COMPLETE:
                return ProtocolType.ROUND_COMPLETE_ACK;

            // 虚拟机控制层
            case VM_START:
                return ProtocolType.VM_START_ACK;
            case VM_STOP:
                return ProtocolType.VM_STOP_ACK;

            // 状态监控层
            case VM_STATUS_QUERY:
                return ProtocolType.VM_STATUS_RESPONSE;

            // 数据集管理层
            case DATASET_CREATE:
                return ProtocolType.DATASET_CREATE_ACK;
            case DATASET_APPEND_ROWS:
                return ProtocolType.DATASET_APPEND_ROWS_ACK;
            case DATASET_COMPLETE:
                return ProtocolType.DATASET_COMPLETE_ACK;
            case DATASET_STATUS_QUERY:
                return ProtocolType.DATASET_STATUS_RESPONSE;
            case DATASET_DELETE:
                return ProtocolType.DATASET_DELETE_ACK;

            default:
                // v1.4协议不支持的类型，返回ERROR
                return ProtocolType.ERROR;
        }
    }

    /**
     * 生成消息签名
     * 使用数字签名服务实现真实的消息签名算法
     *
     * @param type 消息类型
     * @param vmId 虚拟机ID
     * @param data 消息数据
     * @return 签名字符串
     */
    private String generateSignature(ProtocolType type, String vmId, Map<String, Object> data) {
        try {
            // 构建待签名的消息内容
            String content = buildSignatureContent(type, vmId, data);

            // 使用数字签名服务生成签名
            return digitalSignatureService.signMessage(content, DigitalSignatureService.SignatureAlgorithm.RSA_SHA256);
        } catch (Exception e) {
            // 签名失败时记录日志并返回空签名，避免消息发送中断
            log.warn("消息签名生成失败: type={}, vmId={}, error={}", type, vmId, e.getMessage());
            return "";
        }
    }

    /**
     * 构建待签名的消息内容
     * 按照协议v1.4标准格式组织消息内容用于签名
     */
    private String buildSignatureContent(ProtocolType type, String vmId, Map<String, Object> data) {
        StringBuilder content = new StringBuilder();
        content.append("type:").append(type != null ? type.name() : "");
        content.append("|vmId:").append(vmId != null ? vmId : "");

        if (data != null && !data.isEmpty()) {
            content.append("|data:");
            data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // 确保签名一致性
                .forEach(entry -> content.append(entry.getKey()).append("=").append(entry.getValue()).append(";"));
        }

        return content.toString();
    }

    /**
     * 验证消息格式是否符合协议标准
     *
     * @param message 协议消息
     * @return 验证结果
     */
    public boolean validateMessage(ProtocolMessage message) {
        if (message == null) {
            return false;
        }

        // 检查必需字段
        if (message.getType() == null ||
            message.getId() == null ||
            message.getTimestamp() == null ||
            message.getVmId() == null ||
            message.getData() == null) {
            return false;
        }

        // 验证消息ID格式
        return messageIdGenerator.validateMessageId(message.getId());
    }

    // ==================== 协议标准化方法 v1.4 ====================

    /**
     * 生成符合协议的ID格式
     * 格式: {prefix}-{timestamp}-{random}
     *
     * @param prefix ID前缀
     * @return 标准格式的ID
     */
    public static String generateStandardId(String prefix) {
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s-%d-%s", prefix, timestamp, random);
    }

    /**
     * 构建标准FEDERATED_TASK_START消息
     * 符合协议v1.5.1标准
     *
     * @param vmId 虚拟机ID
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param mlAlgorithm ML算法
     * @param hyperparameters 超参数对象
     * @param globalModel 全局模型对象
     * @param message 消息内容
     * @param assignedDatasetId 分配给VM的数据集ID (v1.5.1必需)
     * @param dataPath 数据集本地路径 (v1.5.1必需)
     * @return 标准FEDERATED_TASK_START消息
     */
    public static ProtocolMessage buildTrainingStartMessage(
            String vmId,
            String taskId,
            int roundNumber,
            String mlAlgorithm,
            Map<String, Object> hyperparameters,
            Map<String, Object> globalModel,
            String message,
            String assignedDatasetId,
            String dataPath,
            Map<String, Object> initialModel,
            Map<String, Object> trainingPlan) {

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("mlAlgorithm", mlAlgorithm);
        data.put("hyperparameters", hyperparameters);
        data.put("globalModel", globalModel);
        data.put("message", message);

        if (initialModel != null && !initialModel.isEmpty()) {
            data.put("initialModel", initialModel);
        }

        if (trainingPlan != null && !trainingPlan.isEmpty()) {
            data.put("trainingPlan", trainingPlan);
            Object algorithm = trainingPlan.get("algorithm");
            if (algorithm != null) {
                data.put("federatedAlgorithm", algorithm);
            }
            Object totalRounds = trainingPlan.get("totalRounds");
            if (totalRounds != null) {
                data.put("totalRounds", totalRounds);
            }
        }

        // v1.5.1新增：dataConfig必需字段
        Map<String, Object> dataConfig = new HashMap<>();
        dataConfig.put("assignedDatasetId", assignedDatasetId);
        dataConfig.put("dataPath", dataPath);
        data.put("dataConfig", dataConfig);

        return ProtocolMessage.builder()
                .type(ProtocolType.FEDERATED_TASK_START)
                .id(generateStandardId("federated-task-start"))
                .timestamp(Instant.now())
                .vmId(vmId)
                .data(data)
                .signature("")
                .build();
    }

    /**
     * 构建标准FEDERATED_TASK_START消息 (别名方法)
     * 符合协议v1.5.1标准
     *
     * @param vmId 虚拟机ID
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param mlAlgorithm ML算法
     * @param hyperparameters 超参数对象
     * @param globalModel 全局模型对象
     * @param message 消息内容
     * @param assignedDatasetId 分配给VM的数据集ID (v1.5.1必需)
     * @param dataPath 数据集本地路径 (v1.5.1必需)
     * @return 标准FEDERATED_TASK_START消息
     */
    public static ProtocolMessage buildFederatedTaskStartMessage(
            String vmId,
            String taskId,
            int roundNumber,
            String mlAlgorithm,
            Map<String, Object> hyperparameters,
            Map<String, Object> globalModel,
            String message,
            String assignedDatasetId,
            String dataPath) {

        String messageId = generateStandardId("cmd");

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("mlAlgorithm", mlAlgorithm);
        data.put("hyperparameters", hyperparameters);
        data.put("globalModel", globalModel);
        data.put("message", message);
        data.put("timestamp", Instant.now().toString());

        // v1.5.1新增：dataConfig必需字段
        Map<String, Object> dataConfig = new HashMap<>();
        dataConfig.put("assignedDatasetId", assignedDatasetId);
        dataConfig.put("dataPath", dataPath);
        data.put("dataConfig", dataConfig);

        return ProtocolMessage.builder()
                .type(ProtocolType.FEDERATED_TASK_START)
                .id(messageId)
                .vmId(vmId)
                .data(data)
                .signature(addStaticSignature(data))
                .timestamp(Instant.now())
                .build();
    }


    /**
     * 构建超参数对象
     *
     * @param task 联邦学习任务
     * @return 超参数Map
     */
    public static Map<String, Object> buildHyperparameters(FederatedTask task) {
        Map<String, Object> hyperparameters = new HashMap<>();
        hyperparameters.put("learningRate", task.getLearningRate() != null ? task.getLearningRate() : 0.01);
        hyperparameters.put("batchSize", task.getBatchSize() != null ? task.getBatchSize() : 32);
        hyperparameters.put("epochs", task.getEpochs() != null ? task.getEpochs() : 100);
        hyperparameters.put("timeout", 300); // 5分钟超时
        return hyperparameters;
    }

    /**
     * 构建全局模型对象
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @return 全局模型Map
     */
    public static Map<String, Object> buildGlobalModel(String taskId, int roundNumber) {
        Map<String, Object> globalModel = new HashMap<>();
        globalModel.put("modelId", "global-model-" + taskId + "-round-" + roundNumber);
        globalModel.put("version", "v" + roundNumber + ".0");
        globalModel.put("downloadUrl", "/api/federated/models/" + taskId + "/global/round/" + roundNumber);
        return globalModel;
    }

    /**
     * 构建目标指标对象
     *
     * @param task 联邦学习任务
     * @return 目标指标Map
     */
    public static Map<String, Object> buildTargetMetrics(FederatedTask task) {
        Map<String, Object> targetMetrics = new HashMap<>();
        targetMetrics.put("minAccuracy", 0.85);
        targetMetrics.put("maxLoss", 0.15);
        targetMetrics.put("convergenceThreshold", 0.001);
        return targetMetrics;
    }

    /**
     * 构建训练配置对象
     *
     * @param task 联邦学习任务
     * @return 训练配置Map
     */
    public static Map<String, Object> buildTrainingConfig(FederatedTask task) {
        Map<String, Object> trainingConfig = new HashMap<>();
        trainingConfig.put("learningRate", task.getLearningRate() != null ? task.getLearningRate() : 0.01);
        trainingConfig.put("batchSize", task.getBatchSize() != null ? task.getBatchSize() : 32);
        trainingConfig.put("epochs", task.getEpochs() != null ? task.getEpochs() : 100);
        trainingConfig.put("timeout", 300);
        return trainingConfig;
    }

    /**
     * 构建标准FEDERATED_TASK_STATUS_RESPONSE消息
     * 符合协议v1.4标准
     *
     * @param vmId 虚拟机ID
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param status 状态
     * @param message 消息内容
     * @return FEDERATED_TASK_STATUS_RESPONSE消息
     */
    public static ProtocolMessage buildFederatedTaskStatusResponse(
            String vmId,
            String taskId,
            int roundNumber,
            String status,
            String message) {

        String messageId = generateStandardId("resp");

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("status", status);
        data.put("message", message);
        data.put("timestamp", Instant.now().toString());

        return ProtocolMessage.builder()
                .type(ProtocolType.FEDERATED_TASK_STATUS_RESPONSE)
                .id(messageId)
                .vmId(vmId)
                .data(data)
                .signature(addStaticSignature(data))
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 构建标准ROUND_START ACK消息
     *
     * @param vmId 虚拟机ID
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param status 状态
     * @param message 消息内容
     * @return ROUND_START ACK消息
     */
    public static ProtocolMessage buildRoundStartAck(
            String vmId,
            String taskId,
            int roundNumber,
            String status,
            String message) {

        String messageId = generateStandardId("ack");

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("status", status);
        data.put("message", message);
        data.put("timestamp", Instant.now().toString());

        return ProtocolMessage.builder()
                .type(ProtocolType.ROUND_START_ACK)
                .id(messageId)
                .vmId(vmId)
                .data(data)
                .signature(addStaticSignature(data))
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 添加消息签名
     * 使用数字签名服务为消息数据生成签名
     *
     * @param data 消息数据
     * @return 签名字符串
     */
    public String addSignature(Map<String, Object> data) {
        try {
            // 将Map数据转换为待签名的字符串格式
            String content = buildDataSignatureContent(data);

            // 使用数字签名服务生成签名
            return digitalSignatureService.signMessage(content, DigitalSignatureService.SignatureAlgorithm.RSA_SHA256);
        } catch (Exception e) {
            log.warn("数据签名生成失败: data={}, error={}", data, e.getMessage());
            return "";
        }
    }

    /**
     * 添加静态签名（用于静态方法）
     * 使用SHA-256为消息数据生成签名
     *
     * @param data 消息数据
     * @return 签名字符串
     */
    public static String addStaticSignature(Map<String, Object> data) {
        try {
            // 将Map数据转换为待签名的字符串格式
            String content = buildStaticDataContent(data);

            // 使用SHA-256生成签名
            return java.util.Base64.getEncoder().encodeToString(
                java.security.MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            System.err.println("静态数据签名生成失败: data=" + data + ", error=" + e.getMessage());
            return "";
        }
    }

    /**
     * 构建数据签名内容
     * 将Map数据按照统一格式转换为字符串用于签名
     */
    private String buildDataSignatureContent(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        data.entrySet().stream()
            .sorted(Map.Entry.comparingByKey()) // 确保签名一致性
            .forEach(entry -> content.append(entry.getKey()).append("=").append(entry.getValue()).append(";"));

        return content.toString();
    }

    /**
     * 构建静态数据内容（静态方法使用）
     */
    private static String buildStaticDataContent(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        data.entrySet().stream()
            .sorted(Map.Entry.comparingByKey()) // 确保签名一致性
            .forEach(entry -> content.append(entry.getKey()).append("=").append(entry.getValue()).append(";"));

        return content.toString();
    }

    /**
     * 为消息生成签名（实例方法使用）
     */
    private String generateSignatureForMessage(String messageId, Instant timestamp, Map<String, Object> data) {
        try {
            // 构建待签名内容：messageId + timestamp + data
            String content = messageId + "|" + timestamp.toString() + "|" + buildDataSignatureContent(data);
            return digitalSignatureService.signMessage(content, DigitalSignatureService.SignatureAlgorithm.RSA_SHA256);
        } catch (Exception e) {
            log.warn("消息签名生成失败: messageId={}, error={}", messageId, e.getMessage());
            return "";
        }
    }

    /**
     * 为静态消息生成签名（静态方法使用）
     */
    private static String generateStaticSignature(String messageId, Instant timestamp, Map<String, Object> data) {
        try {
            // 静态方法使用简化的签名算法，基于SHA-256哈希
            String content = messageId + "|" + timestamp.toString() + "|" + buildStaticDataContent(data);
            return java.util.Base64.getEncoder().encodeToString(
                java.security.MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            // 静态方法无法访问logger，使用System.err
            System.err.println("静态消息签名生成失败: messageId=" + messageId + ", error=" + e.getMessage());
            return "";
        }
    }

    /**
     * 构建标准消息（实例方法）
     * 为FederatedTaskServiceImpl提供的兼容性方法
     *
     * @return ProtocolMessage构建器
     */
    public ProtocolMessage.ProtocolMessageBuilder buildMessage() {
        Map<String, Object> data = new HashMap<>();
        String messageId = messageIdGenerator.generateServerMessageId();
        Instant timestamp = Instant.now();

        // 生成真实签名
        String signature = generateSignatureForMessage(messageId, timestamp, data);

        return ProtocolMessage.builder()
                .id(messageId)
                .timestamp(timestamp)
                .data(data)
                .signature(signature);
    }

    /**
     * 构建标准消息（静态方法）
     * 为静态上下文提供的兼容性方法
     *
     * @return ProtocolMessage构建器
     */
    public static ProtocolMessage.ProtocolMessageBuilder buildStaticMessage() {
        Map<String, Object> data = new HashMap<>();
        String messageId = generateStandardId("msg");
        Instant timestamp = Instant.now();

        // 静态方法使用简化签名（或者使用标准签名算法）
        String signature = generateStaticSignature(messageId, timestamp, data);

        return ProtocolMessage.builder()
                .id(messageId)
                .timestamp(timestamp)
                .data(data)
                .signature(signature);
    }

    // ==================== v1.4协议专用消息构建方法 ====================

    /**
     * 构建ROUND_START消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildRoundStartMessage(String vmId, String taskId, int roundNumber,
                                                  Map<String, Object> roundSpecificConfig,
                                                  Map<String, Object> targetMetrics,
                                                  int expectedParticipants,
                                                  Map<String, Object> datasetContext) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("roundSpecificConfig", roundSpecificConfig);
        data.put("targetMetrics", targetMetrics);
        data.put("expectedParticipants", expectedParticipants);
        if (datasetContext != null && !datasetContext.isEmpty()) {
            data.put("datasetContext", datasetContext);
        }

        return buildServerMessage(ProtocolType.ROUND_START, vmId, data);
    }

    /**
     * 构建ROUND_COMPLETE消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildRoundCompleteMessage(String vmId, String taskId, int roundNumber,
                                                     Map<String, Object> roundResults,
                                                     Map<String, Object> nextRound,
                                                     String taskStatus) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("roundResults", roundResults);
        data.put("nextRound", nextRound);
        data.put("taskStatus", taskStatus);

        return buildServerMessage(ProtocolType.ROUND_COMPLETE, vmId, data);
    }

    /**
     * 构建GLOBAL_MODEL_BROADCAST消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildGlobalModelBroadcastMessage(String vmId, String taskId, int roundNumber,
                                                            Map<String, Object> globalModel,
                                                            Map<String, Object> aggregationInfo,
                                                            Map<String, Object> nextRoundConfig) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("globalModel", globalModel);
        data.put("aggregationInfo", aggregationInfo);
        data.put("nextRoundConfig", nextRoundConfig);

        return buildServerMessage(ProtocolType.GLOBAL_MODEL_BROADCAST, vmId, data);
    }

    /**
     * 构建DATASET_STATUS_QUERY消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildDatasetStatusQueryMessage(String vmId, String taskId, String datasetId,
                                                          String queryType, boolean includeStatistics,
                                                          boolean includeMetadata, boolean includeSampleData) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("datasetId", datasetId);
        data.put("queryType", queryType);
        data.put("includeStatistics", includeStatistics);
        data.put("includeMetadata", includeMetadata);
        data.put("includeSampleData", includeSampleData);

        return buildServerMessage(ProtocolType.DATASET_STATUS_QUERY, vmId, data);
    }

    /**
     * 构建DATASET_DELETE消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildDatasetDeleteMessage(String vmId, String taskId, String datasetId,
                                                     String reason, boolean backup, boolean force) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("datasetId", datasetId);
        data.put("reason", reason);
        data.put("backup", backup);
        data.put("force", force);

        return buildServerMessage(ProtocolType.DATASET_DELETE, vmId, data);
    }

    /**
     * 构建CONNECT_ACK消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildConnectAckMessage(String vmId, String sessionId,
                                                  int heartbeatInterval, long maxMessageSize) {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("serverTime", Instant.now().toString());
        data.put("heartbeatInterval", heartbeatInterval);
        data.put("maxMessageSize", maxMessageSize);

        return buildServerMessage(ProtocolType.CONNECT_ACK, vmId, data);
    }

    /**
     * 构建HEARTBEAT_ACK消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildHeartbeatAckMessage(String vmId, int nextHeartbeat, String systemStatus) {
        Map<String, Object> data = new HashMap<>();
        data.put("serverTime", Instant.now().toString());
        data.put("nextHeartbeat", nextHeartbeat);
        data.put("systemStatus", systemStatus);

        return buildServerMessage(ProtocolType.HEARTBEAT_ACK, vmId, data);
    }

    /**
     * 构建VM_STATUS_QUERY消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildVmStatusQueryMessage(String vmId, String queryType,
                                                     boolean includeResources, boolean includeProcesses,
                                                     boolean includeNetwork, boolean includeTasks,
                                                     int timeout) {
        Map<String, Object> data = new HashMap<>();
        data.put("queryType", queryType);
        data.put("includeResources", includeResources);
        data.put("includeProcesses", includeProcesses);
        data.put("includeNetwork", includeNetwork);
        data.put("includeTasks", includeTasks);
        data.put("timeout", timeout);

        return buildServerMessage(ProtocolType.VM_STATUS_QUERY, vmId, data);
    }

    /**
     * 构建ROUND_START_ACK消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildRoundStartAckMessage(String vmId, String taskId, int roundNumber,
                                                     String status, int estimatedTrainingTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("status", status);
        data.put("estimatedTrainingTime", estimatedTrainingTime);

        return buildServerMessage(ProtocolType.ROUND_START_ACK, vmId, data);
    }

    /**
     * 构建ROUND_COMPLETE_ACK消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildRoundCompleteAckMessage(String vmId, String taskId, int roundNumber,
                                                        String status, boolean readyForNextRound) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("status", status);
        data.put("readyForNextRound", readyForNextRound);

        return buildServerMessage(ProtocolType.ROUND_COMPLETE_ACK, vmId, data);
    }

    /**
     * 构建DATASET_STATUS_RESPONSE消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildDatasetStatusResponseMessage(String vmId, String taskId, String datasetId,
                                                             String status, int rowCount, long sizeBytes,
                                                             Map<String, Object> usage, Map<String, Object> integrity,
                                                             Map<String, Object> statistics) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("datasetId", datasetId);
        data.put("status", status);
        data.put("rowCount", rowCount);
        data.put("sizeBytes", sizeBytes);
        data.put("createdAt", Instant.now().toString());
        data.put("lastModified", Instant.now().toString());
        data.put("usage", usage != null ? usage : new HashMap<>());
        data.put("integrity", integrity != null ? integrity : new HashMap<>());
        data.put("statistics", statistics != null ? statistics : new HashMap<>());

        return buildServerMessage(ProtocolType.DATASET_STATUS_RESPONSE, vmId, data);
    }

    /**
     * 构建DATASET_DELETE_ACK消息
     * 符合协议v1.4标准
     */
    public ProtocolMessage buildDatasetDeleteAckMessage(String vmId, String taskId, String datasetId,
                                                        String status, Map<String, Object> cleanupProgress,
                                                        String estimatedCleanupTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("datasetId", datasetId);
        data.put("status", status);
        data.put("cleanupProgress", cleanupProgress != null ? cleanupProgress : new HashMap<>());
        data.put("estimatedCleanupTime", estimatedCleanupTime != null ? estimatedCleanupTime : Instant.now().toString());

        return buildServerMessage(ProtocolType.DATASET_DELETE_ACK, vmId, data);
    }
}
