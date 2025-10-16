package com.feduwacomm.enums;

/**
 * v1.4联邦学习轮次状态枚举
 *
 * 支持WebSocket协议v1.4的"后端大脑+VM手脚"中心化控制模式。
 * 用于管理联邦学习任务中每个轮次的状态流转，确保严格的状态转换和同步控制。
 *
 * v1.4状态流转顺序：
 * INITIALIZING → TRAINING → AGGREGATING → DISTRIBUTING → COMPLETED
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 * @since 2025-09-28
 */
public enum RoundState {

    /**
     * 轮次初始化
     * v1.4：后端正在初始化新轮次，准备发送ROUND_START消息
     */
    INITIALIZING("初始化中", "后端正在初始化新轮次"),

    /**
     * 轮次训练中
     * v1.4：VM正在进行本地训练，后端等待GRADIENT_UPLOAD消息
     */
    TRAINING("训练中", "虚拟机正在进行本轮次训练"),

    /**
     * 模型聚合中
     * v1.4：后端正在聚合接收到的梯度，生成新的全局模型
     */
    AGGREGATING("聚合中", "服务器正在聚合梯度生成全局模型"),

    /**
     * 模型分发中
     * v1.4：后端正在发送GLOBAL_MODEL_BROADCAST消息分发全局模型
     */
    DISTRIBUTING("分发中", "服务器正在分发全局模型到虚拟机"),

    /**
     * 轮次完成
     * v1.4：所有VM已确认接收模型，轮次结束
     */
    COMPLETED("已完成", "轮次已完成，所有VM已确认接收模型"),

    /**
     * 轮次失败
     * v1.4：轮次执行过程中发生错误
     */
    FAILED("失败", "轮次执行失败");

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
     * 判断是否为终止状态（完成或失败是轮次的终止状态）
     */
    public boolean isTerminalState() {
        return this == COMPLETED || this == FAILED;
    }

    /**
     * 判断是否为等待状态（需要外部条件满足才能继续）
     */
    public boolean isWaitingState() {
        return this == TRAINING || this == DISTRIBUTING;
    }

    /**
     * 获取下一个状态
     *
     * @return 下一个状态，如果是终止状态则返回null
     */
    public RoundState getNextState() {
        switch (this) {
            case INITIALIZING:
                return TRAINING;
            case TRAINING:
                return AGGREGATING;
            case AGGREGATING:
                return DISTRIBUTING;
            case DISTRIBUTING:
                return COMPLETED;
            case COMPLETED:
            case FAILED:
                return null; // 终止状态，需要外部控制进入下一轮的INITIALIZING
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

        // v1.4允许的状态转换
        switch (this) {
            case INITIALIZING:
                return targetState == TRAINING || targetState == FAILED;
            case TRAINING:
                return targetState == AGGREGATING || targetState == FAILED;
            case AGGREGATING:
                return targetState == DISTRIBUTING || targetState == FAILED;
            case DISTRIBUTING:
                return targetState == COMPLETED || targetState == FAILED;
            case COMPLETED:
                // 完成状态可以进入下一轮的初始化状态
                return targetState == INITIALIZING;
            case FAILED:
                // 失败状态可以重试或进入下一轮
                return targetState == INITIALIZING || targetState == TRAINING ||
                       targetState == AGGREGATING || targetState == DISTRIBUTING;
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