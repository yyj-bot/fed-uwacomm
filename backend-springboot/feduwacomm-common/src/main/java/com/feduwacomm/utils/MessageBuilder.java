package com.feduwacomm.utils;

import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.entity.FederatedTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
 * @version 1.4.0
 */
@Component
@RequiredArgsConstructor
public class MessageBuilder {

    private final MessageIdGenerator messageIdGenerator;

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
                .timestamp(Instant.now().toString())
                .vmId(vmId)
                .data(data != null ? data : new HashMap<>())
                .signature(generateSignature(type, vmId, data)) // TODO: 实现签名生成
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
                .timestamp(Instant.now().toString())
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
    private ProtocolType getAckType(ProtocolType originalType) {
        switch (originalType) {
            case CONNECT:
                return ProtocolType.CONNECT_ACK;
            case HEARTBEAT:
                return ProtocolType.HEARTBEAT_ACK;
            case TRAINING_START:
                return ProtocolType.TRAINING_START_ACK;
            case TRAINING_START_RESPONSE:
                return ProtocolType.TRAINING_START_RESPONSE_ACK;
            case TRAINING_STOP:
                return ProtocolType.TRAINING_STOP_ACK;
            case TRAINING_PROGRESS:
                return ProtocolType.TRAINING_PROGRESS_ACK;
            case TRAINING_PROGRESS_RESPONSE:
                return ProtocolType.TRAINING_PROGRESS_RESPONSE_ACK;
            case MODEL_UPLOAD:
            case MODEL_DOWNLOAD:
            case GLOBAL_MODEL_UPDATE:
                return ProtocolType.MODEL_UPDATE_ACK;
            case GRADIENT_UPLOAD:
                return ProtocolType.GRADIENT_UPLOAD_ACK;
            case AGGREGATION_START:
                return ProtocolType.AGGREGATION_START_ACK;
            case AGGREGATION_COMPLETE:
                return ProtocolType.AGGREGATION_COMPLETE_ACK;
            case GLOBAL_MODEL_BROADCAST:
                return ProtocolType.GLOBAL_MODEL_BROADCAST_ACK;
            case ROUND_START:
                return ProtocolType.ROUND_START_ACK;
            case ROUND_COMPLETE:
                return ProtocolType.ROUND_COMPLETE_ACK;
            case MODEL_TYPE_NEGOTIATION:
                return ProtocolType.MODEL_TYPE_NEGOTIATION_ACK;
            case ALGORITHM_CONFIG:
                return ProtocolType.ALGORITHM_CONFIG_ACK;
            case GRADIENT_UPLOAD_PREPARE:
                return ProtocolType.GRADIENT_UPLOAD_PREPARE_ACK;
            case STRATEGY_SWITCH_NOTIFICATION:
                return ProtocolType.STRATEGY_SWITCH_ACK;
            case TASK_START:
                return ProtocolType.TASK_START_ACK;
            case FEDERATED_TASK_START:
                return ProtocolType.FEDERATED_TASK_START_ACK;
            case DATASET_CREATE:
                return ProtocolType.DATASET_CREATE_ACK;
            case DATASET_APPEND_ROWS:
                return ProtocolType.DATASET_APPEND_ROWS_ACK;
            case DATASET_COMPLETE:
                return ProtocolType.DATASET_COMPLETE_ACK;
            case DATASET_DELETE:
                return ProtocolType.DATASET_DELETE_ACK;
            default:
                // 如果没有对应的ACK类型，返回通用的响应类型
                return ProtocolType.STATUS_RESPONSE;
        }
    }

    /**
     * 生成消息签名（占位实现）
     * TODO: 实现真实的数字签名算法
     *
     * @param type 消息类型
     * @param vmId 虚拟机ID
     * @param data 消息数据
     * @return 签名字符串
     */
    private String generateSignature(ProtocolType type, String vmId, Map<String, Object> data) {
        // 临时实现：生成简单的哈希值作为签名
        // 生产环境中应该实现真实的数字签名算法
        String content = type.name() + vmId + (data != null ? data.toString() : "");
        return "sig_" + Math.abs(content.hashCode());
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
     * 构建标准TRAINING_START消息
     * 符合协议v1.4标准
     *
     * @param vmId 虚拟机ID
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param mlAlgorithm ML算法
     * @param hyperparameters 超参数对象
     * @param globalModel 全局模型对象
     * @param message 消息内容
     * @return 标准TRAINING_START消息
     */
    public static ProtocolMessage buildTrainingStartMessage(
            String vmId,
            String taskId,
            int roundNumber,
            String mlAlgorithm,
            Map<String, Object> hyperparameters,
            Map<String, Object> globalModel,
            String message) {

        String messageId = generateStandardId("cmd");

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("mlAlgorithm", mlAlgorithm);
        data.put("hyperparameters", hyperparameters);
        data.put("globalModel", globalModel);
        data.put("message", message);
        data.put("timestamp", Instant.now().toString());

        return ProtocolMessage.builder()
                .type(ProtocolType.TRAINING_START)
                .id(messageId)
                .vmId(vmId)
                .data(data)
                .signature(addSignature(data))
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * 构建标准ROUND_START消息
     * 符合协议v1.4标准
     *
     * @param vmId 虚拟机ID
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param trainingConfig 训练配置对象
     * @param targetMetrics 目标指标对象
     * @param expectedParticipants 预期参与者数量
     * @return 标准ROUND_START消息
     */
    public static ProtocolMessage buildRoundStartMessage(
            String vmId,
            String taskId,
            int roundNumber,
            Map<String, Object> trainingConfig,
            Map<String, Object> targetMetrics,
            int expectedParticipants) {

        String messageId = generateStandardId("server");

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("trainingConfig", trainingConfig);
        data.put("targetMetrics", targetMetrics);
        data.put("expectedParticipants", expectedParticipants);
        data.put("timestamp", Instant.now().toString());

        return ProtocolMessage.builder()
                .type(ProtocolType.ROUND_START)
                .id(messageId)
                .vmId(vmId)
                .data(data)
                .signature(addSignature(data))
                .timestamp(Instant.now().toString())
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
     * 构建标准TRAINING_START响应消息
     *
     * @param vmId 虚拟机ID
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param status 状态
     * @param message 消息内容
     * @return TRAINING_START响应消息
     */
    public static ProtocolMessage buildTrainingStartResponse(
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
                .type(ProtocolType.TRAINING_START_RESPONSE)
                .id(messageId)
                .vmId(vmId)
                .data(data)
                .signature(addSignature(data))
                .timestamp(Instant.now().toString())
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
                .signature(addSignature(data))
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * 添加消息签名（当前实现为空签名）
     * TODO: 实现真实的消息签名算法
     *
     * @param data 消息数据
     * @return 签名字符串
     */
    public static String addSignature(Map<String, Object> data) {
        // TODO: 实现真实的消息签名算法
        return ""; // 暂时返回空签名
    }
}