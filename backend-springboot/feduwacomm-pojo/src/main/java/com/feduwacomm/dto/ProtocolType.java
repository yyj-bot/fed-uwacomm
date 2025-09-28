package com.feduwacomm.dto;

/**
 * WebSocket协议类型枚举 - v1.4简化版
 *
 * 本枚举定义了WebSocket协议v1.4的34个核心消息类型，
 * 采用中心化架构设计，移除了冗余和复杂的协商机制。
 *
 * 协议分层：
 * - 连接管理层: CONNECT, HEARTBEAT (4个协议)
 * - 任务管理层: FEDERATED_TASK_* (10个协议)
 * - 轮次管理层: ROUND_*, GRADIENT_*, GLOBAL_MODEL_* (9个协议)
 * - 状态监控层: VM_STATUS_*, ERROR (3个协议)
 * - 虚拟机控制层: VM_START, VM_STOP (4个协议)
 * - 数据集管理层: DATASET_* (4个协议)
 */
public enum ProtocolType {

    // ==================== 连接管理层 (4个协议) ====================
    /** 客户端连接请求 */
    CONNECT,
    /** 服务器连接确认 */
    CONNECT_ACK,
    /** 客户端心跳 */
    HEARTBEAT,
    /** 服务器心跳响应 */
    HEARTBEAT_ACK,

    // ==================== 任务管理层 (10个协议) ====================
    /** 联邦学习任务启动 */
    FEDERATED_TASK_START,
    /** 联邦学习任务启动确认 */
    FEDERATED_TASK_START_ACK,
    /** 联邦学习任务停止 */
    FEDERATED_TASK_STOP,
    /** 联邦学习任务停止确认 */
    FEDERATED_TASK_STOP_ACK,
    /** 联邦学习任务恢复 */
    FEDERATED_TASK_RESUME,
    /** 联邦学习任务恢复确认 */
    FEDERATED_TASK_RESUME_ACK,
    /** 联邦学习任务删除 */
    FEDERATED_TASK_DELETE,
    /** 联邦学习任务删除确认 */
    FEDERATED_TASK_DELETE_ACK,
    /** 联邦学习任务状态查询 */
    FEDERATED_TASK_STATUS_QUERY,
    /** 联邦学习任务状态响应 */
    FEDERATED_TASK_STATUS_RESPONSE,

    // ==================== 轮次管理层 (9个协议) ====================
    /** 轮次开始通知 */
    ROUND_START,
    /** 轮次开始确认 */
    ROUND_START_ACK,
    /** 轮次中止 */
    ROUND_ABORT,
    /** 梯度上传 */
    GRADIENT_UPLOAD,
    /** 梯度上传确认 */
    GRADIENT_UPLOAD_ACK,
    /** 全局模型广播 */
    GLOBAL_MODEL_BROADCAST,
    /** 全局模型广播确认 */
    GLOBAL_MODEL_BROADCAST_ACK,
    /** 轮次完成通知 */
    ROUND_COMPLETE,
    /** 轮次完成确认 */
    ROUND_COMPLETE_ACK,

    // ==================== 状态监控层 (3个协议) ====================
    /** 虚拟机状态查询 */
    VM_STATUS_QUERY,
    /** 虚拟机状态响应 */
    VM_STATUS_RESPONSE,
    /** 错误消息 */
    ERROR,

    // ==================== 虚拟机控制层 (4个协议) ====================
    /** 虚拟机启动 */
    VM_START,
    /** 虚拟机启动确认 */
    VM_START_ACK,
    /** 虚拟机停止 */
    VM_STOP,
    /** 虚拟机停止确认 */
    VM_STOP_ACK,

    // ==================== 数据集管理层 (4个协议) ====================
    /** 数据集创建 */
    DATASET_CREATE,
    /** 数据集创建确认 */
    DATASET_CREATE_ACK,
    /** 数据集追加行 */
    DATASET_APPEND_ROWS,
    /** 数据集追加行确认 */
    DATASET_APPEND_ROWS_ACK,
    /** 数据集完成 */
    DATASET_COMPLETE,
    /** 数据集完成确认 */
    DATASET_COMPLETE_ACK,
    /** 数据集状态查询 */
    DATASET_STATUS_QUERY,
    /** 数据集状态响应 */
    DATASET_STATUS_RESPONSE,
    /** 数据集删除 */
    DATASET_DELETE,
    /** 数据集删除确认 */
    DATASET_DELETE_ACK,

    // ==================== 向后兼容性错误类型 ====================
    /** 连接错误 */
    CONNECTION_ERROR,
    /** 消息错误 */
    MESSAGE_ERROR
} 