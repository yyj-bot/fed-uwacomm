package com.feduwacomm.service.impl;

import com.feduwacomm.dto.WebSocketMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.service.WebSocketCommandService;
import com.feduwacomm.service.MessageIdGenerator;
import com.feduwacomm.service.DigitalSignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * WebSocket命令发送服务实现类 - v1.4协议兼容版
 *
 * 实现了符合v1.4协议标准的命令发送逻辑：
 * - 自动将CommandType映射到对应的ProtocolType
 * - 使用正确的v1.4协议消息类型
 * - 支持6种核心命令类型的发送和状态管理
 *
 * @author FedUWAComm Team
 * @version 1.4.0 (v1.4协议标准版)
 * @since 2025-09-28
 */
@Slf4j
@Service
public class WebSocketCommandServiceImpl implements WebSocketCommandService {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private MessageIdGenerator messageIdGenerator;

    @Autowired
    private DigitalSignatureService digitalSignatureService;

    // 存储在线虚拟机的连接状态
    private final Set<String> onlineVmIds = ConcurrentHashMap.newKeySet();

    // 存储待执行命令的回调
    private final Map<String, CommandCallback> commandCallbacks = new ConcurrentHashMap<>();

    // 存储命令状态
    private final Map<String, CommandResult> commandStatuses = new ConcurrentHashMap<>();

    // 命令超时处理线程池
    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(2);

    // 默认命令超时时间（秒）
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * 将CommandType映射到对应的v1.4协议类型
     *
     * @param commandType 命令类型
     * @return 对应的v1.4协议类型
     */
    private ProtocolType mapCommandToProtocolType(CommandType commandType) {
        switch (commandType) {
            case VM_START:
                return ProtocolType.VM_START;
            case VM_STOP:
                return ProtocolType.VM_STOP;
            case FEDERATED_TASK_START:
                return ProtocolType.FEDERATED_TASK_START;
            case FEDERATED_TASK_STOP:
                return ProtocolType.FEDERATED_TASK_STOP;
            case FEDERATED_TASK_RESUME:
                return ProtocolType.FEDERATED_TASK_RESUME;
            case FEDERATED_TASK_DELETE:
                return ProtocolType.FEDERATED_TASK_DELETE;
            default:
                throw new IllegalArgumentException("不支持的命令类型: " + commandType);
        }
    }

    @Override
    public String sendCommand(String vmId, CommandType commandType, Object commandData) {
        return sendCommand(vmId, commandType, commandData, DEFAULT_TIMEOUT_SECONDS, null);
    }

    @Override
    public String sendCommand(String vmId, CommandType commandType, Object commandData, CommandCallback callback) {
        return sendCommand(vmId, commandType, commandData, DEFAULT_TIMEOUT_SECONDS, callback);
    }

    @Override
    public String sendCommand(String vmId, CommandType commandType, Object commandData, int timeoutSeconds, CommandCallback callback) {
        if (!isVmOnline(vmId)) {
            log.warn("虚拟机未在线，无法发送命令: vmId={}, commandType={}", vmId, commandType);
            if (callback != null) {
                callback.onCommandError(null, vmId, "虚拟机未在线");
            }
            return null;
        }

        String commandId = messageIdGenerator.generateFederatedTaskMessageId();

        try {
            // 获取v1.4协议类型
            ProtocolType protocolType = mapCommandToProtocolType(commandType);
            String protocolTypeName = protocolType.name();

            // 构建命令消息
            Map<String, Object> data = new HashMap<>();
            data.put("command", commandType.name());
            data.put("commandData", commandData);
            data.put("commandId", commandId);
            data.put("timestamp", Instant.now().toString());

            // 生成数字签名
            String signature;
            try {
                signature = digitalSignatureService.signWebSocketMessage(
                    protocolTypeName, commandId, vmId, data.toString());
            } catch (DigitalSignatureService.SignatureException e) {
                log.error("生成命令消息签名失败: vmId={}, commandId={}", vmId, commandId, e);
                signature = "signature-error";
            }

            WebSocketMessage message = WebSocketMessage.builder()
                .type(protocolTypeName)
                .id(commandId)
                .vmId(vmId)
                .data(data)
                .signature(signature)
                .build();
            message.setTimestamp(Instant.now());

            // 发送消息
            String destination = "/topic/vm/" + vmId + "/commands";
            messagingTemplate.convertAndSend(destination, message);

            // 注册回调和状态
            if (callback != null) {
                commandCallbacks.put(commandId, callback);
                callback.onCommandSent(commandId, vmId);
            }
            commandStatuses.put(commandId, CommandResult.SUCCESS);

            // 设置超时处理
            scheduleTimeout(commandId, vmId, timeoutSeconds, callback);

            log.info("WebSocket命令发送成功: vmId={}, commandType={}, commandId={}", vmId, commandType, commandId);
            return commandId;

        } catch (Exception e) {
            log.error("发送WebSocket命令失败: vmId={}, commandType={}, commandId={}", vmId, commandType, commandId, e);
            if (callback != null) {
                callback.onCommandError(commandId, vmId, "发送命令失败: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public List<String> sendBatchCommand(List<String> vmIds, CommandType commandType, Object commandData) {
        List<String> commandIds = new ArrayList<>();

        for (String vmId : vmIds) {
            String commandId = sendCommand(vmId, commandType, commandData);
            if (commandId != null) {
                commandIds.add(commandId);
            }
        }

        log.info("批量发送WebSocket命令完成: commandType={}, totalVms={}, successfulCommands={}",
                commandType, vmIds.size(), commandIds.size());

        return commandIds;
    }

    @Override
    public boolean isVmOnline(String vmId) {
        return onlineVmIds.contains(vmId);
    }

    @Override
    public CommandResult getCommandStatus(String commandId) {
        return commandStatuses.getOrDefault(commandId, CommandResult.FAILED);
    }

    @Override
    public boolean cancelCommand(String commandId) {
        try {
            commandCallbacks.remove(commandId);
            commandStatuses.put(commandId, CommandResult.FAILED);
            log.info("命令已取消: commandId={}", commandId);
            return true;
        } catch (Exception e) {
            log.error("取消命令失败: commandId={}", commandId, e);
            return false;
        }
    }

    @Override
    public boolean sendRawMessage(String vmId, WebSocketMessage message) {
        if (!isVmOnline(vmId)) {
            log.warn("虚拟机未在线，无法发送原始消息: vmId={}", vmId);
            return false;
        }

        try {
            String destination = "/topic/vm/" + vmId + "/messages";
            messagingTemplate.convertAndSend(destination, message);
            log.debug("原始WebSocket消息发送成功: vmId={}, messageType={}", vmId, message.getType());
            return true;
        } catch (Exception e) {
            log.error("发送原始WebSocket消息失败: vmId={}", vmId, e);
            return false;
        }
    }

    @Override
    public int broadcastMessage(WebSocketMessage message) {
        int successCount = 0;

        for (String vmId : onlineVmIds) {
            if (sendRawMessage(vmId, message)) {
                successCount++;
            }
        }

        log.info("广播WebSocket消息完成: messageType={}, onlineVms={}, successfulSends={}",
                message.getType(), onlineVmIds.size(), successCount);

        return successCount;
    }

    @Override
    public List<String> getOnlineVmIds() {
        return new ArrayList<>(onlineVmIds);
    }

    @Override
    public void handleCommandResponse(String vmId, String commandId, Object response) {
        CommandCallback callback = commandCallbacks.get(commandId);

        if (callback != null) {
            try {
                // 解析响应判断成功或失败
                CommandResult result = parseCommandResult(response);
                callback.onCommandResult(commandId, vmId, result, response);
                commandStatuses.put(commandId, result);

                log.info("处理命令响应: vmId={}, commandId={}, result={}", vmId, commandId, result);
            } catch (Exception e) {
                log.error("处理命令响应失败: vmId={}, commandId={}", vmId, commandId, e);
                callback.onCommandError(commandId, vmId, "响应处理失败: " + e.getMessage());
            } finally {
                // 清理回调
                commandCallbacks.remove(commandId);
            }
        } else {
            log.debug("收到未知命令的响应: vmId={}, commandId={}", vmId, commandId);
        }
    }

    /**
     * 添加在线虚拟机
     */
    public void addOnlineVm(String vmId) {
        onlineVmIds.add(vmId);
        log.debug("虚拟机上线: vmId={}, 当前在线数量={}", vmId, onlineVmIds.size());
    }

    /**
     * 移除在线虚拟机
     */
    public void removeOnlineVm(String vmId) {
        onlineVmIds.remove(vmId);
        log.debug("虚拟机下线: vmId={}, 当前在线数量={}", vmId, onlineVmIds.size());

        // 清理该VM的待处理命令
        cleanupVmCommands(vmId);
    }

    /**
     * 设置命令超时处理
     */
    private void scheduleTimeout(String commandId, String vmId, int timeoutSeconds, CommandCallback callback) {
        timeoutExecutor.schedule(() -> {
            if (commandCallbacks.containsKey(commandId)) {
                log.warn("命令执行超时: vmId={}, commandId={}, timeout={}s", vmId, commandId, timeoutSeconds);

                commandStatuses.put(commandId, CommandResult.TIMEOUT);
                commandCallbacks.remove(commandId);

                if (callback != null) {
                    callback.onCommandResult(commandId, vmId, CommandResult.TIMEOUT, null);
                }
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 解析命令执行结果
     */
    private CommandResult parseCommandResult(Object response) {
        if (response == null) {
            return CommandResult.FAILED;
        }

        // 这里可以根据响应格式进行解析
        // 简单示例：如果响应包含"success"关键字则认为成功
        String responseStr = response.toString().toLowerCase();
        if (responseStr.contains("success") || responseStr.contains("ok")) {
            return CommandResult.SUCCESS;
        } else if (responseStr.contains("timeout")) {
            return CommandResult.TIMEOUT;
        } else {
            return CommandResult.FAILED;
        }
    }

    /**
     * 清理虚拟机相关的待处理命令
     */
    private void cleanupVmCommands(String vmId) {
        commandCallbacks.entrySet().removeIf(entry -> {
            String commandId = entry.getKey();
            CommandCallback callback = entry.getValue();

            // 通知命令失败（虚拟机断开连接）
            try {
                callback.onCommandResult(commandId, vmId, CommandResult.VM_DISCONNECTED, null);
            } catch (Exception e) {
                log.error("清理命令回调时发生错误: commandId={}", commandId, e);
            }

            commandStatuses.put(commandId, CommandResult.VM_DISCONNECTED);
            return true;
        });
    }
}