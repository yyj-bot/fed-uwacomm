package com.feduwacomm.service;

import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.utils.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * WebSocket消息发送服务
 * 负责实现协议v1.4中的广播和点对点消息发送机制
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 */
@Service
public class WebSocketMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageSender.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageBuilder messageBuilder;

    public WebSocketMessageSender(SimpMessagingTemplate messagingTemplate, MessageBuilder messageBuilder) {
        this.messagingTemplate = messagingTemplate;
        this.messageBuilder = messageBuilder;
    }

    // ==================== 服务端主动协议发送方法 ====================

    /**
     * 发送ROUND_START消息到指定虚拟机或广播
     * 符合协议v1.4标准
     */
    public void sendRoundStart(String vmId, String taskId, int roundNumber,
                              Map<String, Object> roundSpecificConfig,
                              Map<String, Object> targetMetrics,
                              int expectedParticipants) {
        ProtocolMessage message = messageBuilder.buildRoundStartMessage(
                vmId, taskId, roundNumber, roundSpecificConfig, targetMetrics, expectedParticipants);

        if ("broadcast".equals(vmId)) {
            broadcastToAllVMs(message);
        } else {
            sendToVM(vmId, message);
        }

        logger.info("发送ROUND_START消息 - TaskId: {}, RoundNumber: {}, VmId: {}",
                   taskId, roundNumber, vmId);
    }

    /**
     * 发送ROUND_COMPLETE消息到指定虚拟机或广播
     * 符合协议v1.4标准
     */
    public void sendRoundComplete(String vmId, String taskId, int roundNumber,
                                 Map<String, Object> roundResults,
                                 Map<String, Object> nextRound,
                                 String taskStatus) {
        ProtocolMessage message = messageBuilder.buildRoundCompleteMessage(
                vmId, taskId, roundNumber, roundResults, nextRound, taskStatus);

        if ("broadcast".equals(vmId)) {
            broadcastToAllVMs(message);
        } else {
            sendToVM(vmId, message);
        }

        logger.info("发送ROUND_COMPLETE消息 - TaskId: {}, RoundNumber: {}, VmId: {}",
                   taskId, roundNumber, vmId);
    }

    /**
     * 发送GLOBAL_MODEL_BROADCAST消息到所有虚拟机
     * 符合协议v1.4标准
     */
    public void sendGlobalModelBroadcast(String taskId, int roundNumber,
                                        Map<String, Object> globalModel,
                                        Map<String, Object> aggregationInfo,
                                        Map<String, Object> nextRoundConfig) {
        ProtocolMessage message = messageBuilder.buildGlobalModelBroadcastMessage(
                "broadcast", taskId, roundNumber, globalModel, aggregationInfo, nextRoundConfig);

        broadcastToAllVMs(message);

        logger.info("发送GLOBAL_MODEL_BROADCAST消息 - TaskId: {}, RoundNumber: {}",
                   taskId, roundNumber);
    }

    /**
     * 发送DATASET_STATUS_QUERY消息到指定虚拟机
     * 符合协议v1.4标准
     */
    public void sendDatasetStatusQuery(String vmId, String taskId, String datasetId,
                                      String queryType, boolean includeStatistics,
                                      boolean includeMetadata, boolean includeSampleData) {
        ProtocolMessage message = messageBuilder.buildDatasetStatusQueryMessage(
                vmId, taskId, datasetId, queryType, includeStatistics, includeMetadata, includeSampleData);

        sendToVM(vmId, message);

        logger.info("发送DATASET_STATUS_QUERY消息 - VmId: {}, TaskId: {}, DatasetId: {}",
                   vmId, taskId, datasetId);
    }

    /**
     * 发送DATASET_DELETE消息到指定虚拟机
     * 符合协议v1.4标准
     */
    public void sendDatasetDelete(String vmId, String taskId, String datasetId,
                                 String reason, boolean backup, boolean force) {
        ProtocolMessage message = messageBuilder.buildDatasetDeleteMessage(
                vmId, taskId, datasetId, reason, backup, force);

        sendToVM(vmId, message);

        logger.info("发送DATASET_DELETE消息 - VmId: {}, TaskId: {}, DatasetId: {}",
                   vmId, taskId, datasetId);
    }

    /**
     * 发送CONNECT_ACK消息到指定虚拟机
     * 符合协议v1.4标准
     */
    public void sendConnectAck(String vmId, String sessionId, int heartbeatInterval, long maxMessageSize) {
        ProtocolMessage message = messageBuilder.buildConnectAckMessage(
                vmId, sessionId, heartbeatInterval, maxMessageSize);

        sendToVM(vmId, message);

        logger.info("发送CONNECT_ACK消息 - VmId: {}, SessionId: {}", vmId, sessionId);
    }

    /**
     * 发送HEARTBEAT_ACK消息到指定虚拟机
     * 符合协议v1.4标准
     */
    public void sendHeartbeatAck(String vmId, int nextHeartbeat, String systemStatus) {
        ProtocolMessage message = messageBuilder.buildHeartbeatAckMessage(
                vmId, nextHeartbeat, systemStatus);

        sendToVM(vmId, message);

        logger.debug("发送HEARTBEAT_ACK消息 - VmId: {}, SystemStatus: {}", vmId, systemStatus);
    }

    /**
     * 发送VM_STATUS_QUERY消息到指定虚拟机
     * 符合协议v1.4标准
     */
    public void sendVmStatusQuery(String vmId, String queryType,
                                 boolean includeResources, boolean includeProcesses,
                                 boolean includeNetwork, boolean includeTasks,
                                 int timeout) {
        ProtocolMessage message = messageBuilder.buildVmStatusQueryMessage(
                vmId, queryType, includeResources, includeProcesses, includeNetwork, includeTasks, timeout);

        sendToVM(vmId, message);

        logger.info("发送VM_STATUS_QUERY消息 - VmId: {}, QueryType: {}", vmId, queryType);
    }

    // ==================== v1.4协议专用发送方法 ====================

    /**
     * 广播全局模型到所有虚拟机
     * 符合协议v1.4标准
     */
    public void broadcastGlobalModel(String taskId, int roundNumber,
                                    Map<String, Object> globalModel,
                                    Map<String, Object> aggregationInfo) {
        Map<String, Object> nextRoundConfig = Map.of(
            "startTime", java.time.Instant.now().toString(),
            "learningRate", 0.01
        );

        sendGlobalModelBroadcast(taskId, roundNumber, globalModel, aggregationInfo, nextRoundConfig);
    }

    /**
     * 广播轮次完成消息到所有虚拟机
     * 符合协议v1.4标准
     */
    public void broadcastRoundComplete(String taskId, int roundNumber,
                                      Map<String, Object> roundResults) {
        Map<String, Object> nextRound = Map.of(
            "planned", true,
            "roundNumber", roundNumber + 1,
            "scheduledStart", java.time.Instant.now().toString()
        );

        sendRoundComplete("broadcast", taskId, roundNumber, roundResults, nextRound, "CONTINUING");
    }

    /**
     * 发送消息到虚拟机（别名方法）
     * 为保持与WebSocketProtocolService的兼容性
     */
    public void sendToVm(String vmId, ProtocolMessage message) {
        sendToVM(vmId, message);
    }

    // ==================== 消息发送基础方法 ====================

    /**
     * 向指定虚拟机发送消息
     * 使用VM专属的topic进行点对点通信
     */
    public void sendToVM(String vmId, ProtocolMessage message) {
        try {
            String destination = "/topic/vm/" + vmId;
            messagingTemplate.convertAndSend(destination, message);

            logger.debug("消息已发送到VM - VmId: {}, Type: {}, Destination: {}",
                        vmId, message.getType(), destination);
        } catch (Exception e) {
            logger.error("发送消息到VM失败 - VmId: {}, Type: {}, Error: {}",
                        vmId, message.getType(), e.getMessage(), e);
        }
    }

    /**
     * 向指定的多个虚拟机发送消息
     */
    public void sendToVMs(List<String> vmIds, ProtocolMessage message) {
        for (String vmId : vmIds) {
            sendToVM(vmId, message);
        }

        logger.info("消息已发送到多个VM - Count: {}, Type: {}", vmIds.size(), message.getType());
    }

    /**
     * 广播消息到所有虚拟机
     * 使用全局广播topic
     */
    public void broadcastToAllVMs(ProtocolMessage message) {
        try {
            String destination = "/topic/broadcast";
            messagingTemplate.convertAndSend(destination, message);

            logger.info("消息已广播到所有VM - Type: {}, Destination: {}",
                       message.getType(), destination);
        } catch (Exception e) {
            logger.error("广播消息失败 - Type: {}, Error: {}",
                        message.getType(), e.getMessage(), e);
        }
    }

    /**
     * 向用户发送私有消息
     * 用于回复和通知
     */
    public void sendToUser(String username, ProtocolMessage message) {
        try {
            String destination = "/queue/reply";
            messagingTemplate.convertAndSendToUser(username, destination, message);

            logger.debug("消息已发送到用户 - Username: {}, Type: {}",
                        username, message.getType());
        } catch (Exception e) {
            logger.error("发送消息到用户失败 - Username: {}, Type: {}, Error: {}",
                        username, message.getType(), e.getMessage(), e);
        }
    }

    /**
     * 发送通用协议消息
     * 根据vmId自动选择发送方式
     */
    public void sendProtocolMessage(ProtocolMessage message) {
        String vmId = message.getVmId();

        if ("broadcast".equals(vmId)) {
            broadcastToAllVMs(message);
        } else if (vmId != null && !vmId.isEmpty()) {
            sendToVM(vmId, message);
        } else {
            logger.warn("无效的vmId，无法发送消息 - Type: {}, VmId: {}",
                       message.getType(), vmId);
        }
    }
}