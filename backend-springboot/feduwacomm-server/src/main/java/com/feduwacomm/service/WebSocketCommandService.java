package com.feduwacomm.service;

import com.feduwacomm.dto.WebSocketMessage;

/**
 * WebSocket命令发送服务接口 - v1.4协议兼容版
 *
 * 负责向连接的虚拟机发送符合v1.4协议标准的控制命令
 * 支持虚拟机控制(启动/停止)和联邦学习任务管理
 * 支持异步发送和结果回调机制
 *
 * @author FedUWAComm Team
 * @version 1.4.0 (v1.4协议标准版)
 * @since 2025-09-28
 */
public interface WebSocketCommandService {

    /**
     * 命令类型枚举 - v1.4协议标准版
     *
     * 仅包含v1.4协议中明确定义的6个核心命令类型：
     * - 虚拟机控制: VM_START, VM_STOP
     * - 联邦学习任务管理: FEDERATED_TASK_START, FEDERATED_TASK_STOP, FEDERATED_TASK_RESUME, FEDERATED_TASK_DELETE
     *
     * 移除了v1.3中的冗余协议(VM_RESTART, VM_PAUSE等)和非标准协议(MODEL_SYNC等)
     */
    enum CommandType {
        // v1.4协议虚拟机控制层命令
        VM_START("启动虚拟机"),
        VM_STOP("停止虚拟机"),

        // v1.4协议任务管理层命令
        FEDERATED_TASK_START("启动联邦学习任务"),
        FEDERATED_TASK_STOP("停止联邦学习任务"),
        FEDERATED_TASK_RESUME("恢复联邦学习任务"),
        FEDERATED_TASK_DELETE("删除联邦学习任务");

        private final String description;

        CommandType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 命令执行结果枚举
     */
    enum CommandResult {
        SUCCESS("成功"),
        FAILED("失败"),
        TIMEOUT("超时"),
        VM_DISCONNECTED("虚拟机未连接"),
        INVALID_COMMAND("无效命令");

        private final String description;

        CommandResult(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 命令发送结果回调接口
     */
    interface CommandCallback {
        /**
         * 命令发送成功回调
         * @param commandId 命令ID
         * @param vmId 虚拟机ID
         */
        void onCommandSent(String commandId, String vmId);

        /**
         * 命令执行结果回调
         * @param commandId 命令ID
         * @param vmId 虚拟机ID
         * @param result 执行结果
         * @param resultData 结果数据
         */
        void onCommandResult(String commandId, String vmId, CommandResult result, Object resultData);

        /**
         * 命令执行错误回调
         * @param commandId 命令ID
         * @param vmId 虚拟机ID
         * @param error 错误信息
         */
        void onCommandError(String commandId, String vmId, String error);
    }

    /**
     * 向指定虚拟机发送命令
     *
     * @param vmId 虚拟机ID
     * @param commandType 命令类型
     * @param commandData 命令数据
     * @return 命令ID，用于跟踪命令执行状态
     */
    String sendCommand(String vmId, CommandType commandType, Object commandData);

    /**
     * 向指定虚拟机发送命令（带回调）
     *
     * @param vmId 虚拟机ID
     * @param commandType 命令类型
     * @param commandData 命令数据
     * @param callback 回调接口
     * @return 命令ID
     */
    String sendCommand(String vmId, CommandType commandType, Object commandData, CommandCallback callback);

    /**
     * 向指定虚拟机发送命令（带超时）
     *
     * @param vmId 虚拟机ID
     * @param commandType 命令类型
     * @param commandData 命令数据
     * @param timeoutSeconds 超时时间（秒）
     * @param callback 回调接口
     * @return 命令ID
     */
    String sendCommand(String vmId, CommandType commandType, Object commandData, int timeoutSeconds, CommandCallback callback);

    /**
     * 批量向多个虚拟机发送相同命令
     *
     * @param vmIds 虚拟机ID列表
     * @param commandType 命令类型
     * @param commandData 命令数据
     * @return 命令ID列表
     */
    java.util.List<String> sendBatchCommand(java.util.List<String> vmIds, CommandType commandType, Object commandData);

    /**
     * 检查虚拟机是否在线
     *
     * @param vmId 虚拟机ID
     * @return 是否在线
     */
    boolean isVmOnline(String vmId);

    /**
     * 获取命令执行状态
     *
     * @param commandId 命令ID
     * @return 命令执行状态
     */
    CommandResult getCommandStatus(String commandId);

    /**
     * 取消未执行的命令
     *
     * @param commandId 命令ID
     * @return 是否取消成功
     */
    boolean cancelCommand(String commandId);

    /**
     * 发送原始WebSocket消息
     * 这是一个低级接口，一般情况下应使用上面的高级接口
     *
     * @param vmId 虚拟机ID
     * @param message WebSocket消息
     * @return 是否发送成功
     */
    boolean sendRawMessage(String vmId, WebSocketMessage message);

    /**
     * 广播消息给所有在线虚拟机
     *
     * @param message WebSocket消息
     * @return 成功发送的虚拟机数量
     */
    int broadcastMessage(WebSocketMessage message);

    /**
     * 获取在线虚拟机列表
     *
     * @return 在线虚拟机ID列表
     */
    java.util.List<String> getOnlineVmIds();

    /**
     * 处理来自虚拟机的命令响应
     * 这个方法会被WebSocket消息处理器调用
     *
     * @param vmId 虚拟机ID
     * @param commandId 命令ID
     * @param response 响应数据
     */
    void handleCommandResponse(String vmId, String commandId, Object response);
}