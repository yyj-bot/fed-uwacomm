package com.feduwacomm.enums;

/**
 * 联邦学习轮次状态枚举
 *
 * 用于管理联邦学习任务中每个轮次的状态流转，确保轮次的严格同步性。
 * 基于现有的WebSocket协议，利用GLOBAL_MODEL_BROADCAST_ACK等确认机制。
 *
 * 状态流转顺序：
 * AGGREGATING → DISTRIBUTING → WAITING_ACK → READY → TRAINING → (下一轮次AGGREGATING)
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-27
 */
public enum RoundState {

    /**
     * 模型聚合中
     * 服务器正在聚合上一轮次的本地模型，生成新的全局模型
     */
    AGGREGATING("聚合中", "服务器正在聚合本地模型"),

    /**
     * 模型分发中
     * 服务器正在向VM发送GLOBAL_MODEL_BROADCAST消息分发全局模型
     */
    DISTRIBUTING("分发中", "服务器正在分发全局模型到虚拟机"),

    /**
     * 等待确认中
     * 服务器等待所有VM发送GLOBAL_MODEL_BROADCAST_ACK确认收到模型
     * 这是确保轮次同步的关键状态
     */
    WAITING_ACK("等待确认", "等待所有虚拟机确认接收全局模型"),

    /**
     * 轮次就绪
     * 所有VM都已确认收到全局模型，准备开始新轮次训练
     */
    READY("就绪", "所有虚拟机已确认接收模型，准备开始训练"),

    /**
     * 轮次训练中
     * 服务器发送ROUND_START指令，VM正在进行本轮次训练
     */
    TRAINING("训练中", "虚拟机正在进行本轮次训练");

    private final String description;
    private final String detail;

    RoundState(String description, String detail) {
        this.description = description;
        this.detail = detail;
    }

    /**
     * 获取状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取状态详细说明
     */
    public String getDetail() {
        return detail;
    }

    /**
     * 判断是否为终止状态（训练中是一个轮次的终止状态）
     */
    public boolean isTerminalState() {
        return this == TRAINING;
    }

    /**
     * 判断是否为等待状态（需要外部条件满足才能继续）
     */
    public boolean isWaitingState() {
        return this == WAITING_ACK;
    }

    /**
     * 获取下一个状态
     *
     * @return 下一个状态，如果是TRAINING则返回null（需要外部控制进入下一轮）
     */
    public RoundState getNextState() {
        switch (this) {
            case AGGREGATING:
                return DISTRIBUTING;
            case DISTRIBUTING:
                return WAITING_ACK;
            case WAITING_ACK:
                return READY;
            case READY:
                return TRAINING;
            case TRAINING:
                return null; // 需要外部控制进入下一轮的AGGREGATING
            default:
                throw new IllegalStateException("未知的轮次状态: " + this);
        }
    }

    /**
     * 判断是否可以转换到目标状态
     *
     * @param targetState 目标状态
     * @return 是否可以转换
     */
    public boolean canTransitionTo(RoundState targetState) {
        if (targetState == null) {
            return false;
        }

        // 允许的状态转换
        switch (this) {
            case AGGREGATING:
                return targetState == DISTRIBUTING;
            case DISTRIBUTING:
                return targetState == WAITING_ACK;
            case WAITING_ACK:
                return targetState == READY;
            case READY:
                return targetState == TRAINING;
            case TRAINING:
                // 训练完成后可以进入下一轮的聚合状态
                return targetState == AGGREGATING;
            default:
                return false;
        }
    }

    /**
     * 从字符串获取状态枚举
     *
     * @param stateName 状态名称
     * @return 对应的状态枚举
     * @throws IllegalArgumentException 如果状态名称无效
     */
    public static RoundState fromString(String stateName) {
        if (stateName == null || stateName.trim().isEmpty()) {
            throw new IllegalArgumentException("状态名称不能为空");
        }

        try {
            return RoundState.valueOf(stateName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的轮次状态: " + stateName);
        }
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), description);
    }
}