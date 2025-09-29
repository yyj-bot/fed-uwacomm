package com.feduwacomm.service;

import java.util.Map;

/**
 * 通知服务接口
 * 提供统一的通知发送能力
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
public interface NotificationService {

    /**
     * 通知类型枚举
     */
    enum NotificationType {
        ALERT,           // 告警通知
        WORKFLOW,        // 工作流通知
        TASK_COMPLETE,   // 任务完成通知
        MODEL_READY,     // 模型就绪通知
        DISTRIBUTION,    // 分发通知
        SYSTEM_STATUS    // 系统状态通知
    }

    /**
     * 通知渠道枚举
     */
    enum NotificationChannel {
        WEBSOCKET,  // WebSocket推送
        EMAIL,      // 邮件
        SMS,        // 短信
        WEBHOOK,    // Webhook
        LOG         // 日志记录
    }

    /**
     * 通知优先级枚举
     */
    enum NotificationPriority {
        LOW,     // 低优先级
        MEDIUM,  // 中等优先级
        HIGH,    // 高优先级
        URGENT   // 紧急
    }

    /**
     * 发送通知
     *
     * @param type 通知类型
     * @param title 通知标题
     * @param message 通知内容
     * @param recipient 接收者（可以是用户ID、邮箱、手机号等）
     * @param priority 优先级
     * @param channels 发送渠道
     * @param metadata 附加数据
     * @return 是否发送成功
     */
    boolean sendNotification(NotificationType type, String title, String message,
                           String recipient, NotificationPriority priority,
                           NotificationChannel[] channels, Map<String, Object> metadata);

    /**
     * 发送广播通知
     *
     * @param type 通知类型
     * @param title 通知标题
     * @param message 通知内容
     * @param priority 优先级
     * @param channels 发送渠道
     * @param metadata 附加数据
     * @return 是否发送成功
     */
    boolean sendBroadcastNotification(NotificationType type, String title, String message,
                                    NotificationPriority priority, NotificationChannel[] channels,
                                    Map<String, Object> metadata);

    /**
     * 发送告警通知
     *
     * @param alertId 告警ID
     * @param alertName 告警名称
     * @param message 告警信息
     * @param severity 严重程度
     * @param currentValue 当前值
     * @return 是否发送成功
     */
    boolean sendAlertNotification(String alertId, String alertName, String message,
                                String severity, double currentValue);

    /**
     * 发送工作流状态通知
     *
     * @param orchestrationId 工作流ID
     * @param stage 当前阶段
     * @param status 状态
     * @param message 消息
     * @return 是否发送成功
     */
    boolean sendWorkflowNotification(String orchestrationId, String stage,
                                   String status, String message);

    /**
     * 发送模型分发通知
     *
     * @param modelId 模型ID
     * @param distributionType 分发类型
     * @param status 分发状态
     * @param details 详细信息
     * @return 是否发送成功
     */
    boolean sendModelDistributionNotification(String modelId, String distributionType,
                                            String status, Map<String, Object> details);

    /**
     * 配置通知偏好
     *
     * @param userId 用户ID
     * @param preferences 通知偏好设置
     */
    void configureNotificationPreferences(String userId, Map<String, Object> preferences);

    /**
     * 获取通知历史
     *
     * @param recipient 接收者
     * @param limit 限制数量
     * @return 通知历史
     */
    java.util.List<Map<String, Object>> getNotificationHistory(String recipient, int limit);

    /**
     * 标记通知已读
     *
     * @param notificationId 通知ID
     * @param recipient 接收者
     * @return 是否成功
     */
    boolean markNotificationAsRead(String notificationId, String recipient);

    /**
     * 发送WebSocket通知
     *
     * @param taskId 任务ID
     * @param eventType 事件类型
     * @param level 级别
     * @param message 消息
     * @param metadata 元数据
     * @return 是否发送成功
     */
    boolean sendWebSocketNotification(String taskId, String eventType, String level,
                                    String message, Map<String, Object> metadata);

    /**
     * 发送通知（简化版本）
     *
     * @param channel 渠道
     * @param title 标题
     * @param message 消息
     * @param recipient 接收者
     * @param priority 优先级
     * @param metadata 元数据
     * @return 是否发送成功
     */
    boolean sendNotification(String channel, String title, String message,
                           String recipient, String priority,
                           Map<String, Object> metadata);

    /**
     * 发送系统通知
     *
     * @param level 级别
     * @param title 标题
     * @param message 消息
     * @param metadata 元数据
     * @return 是否发送成功
     */
    boolean sendSystemNotification(String level, String title, String message,
                                 Map<String, Object> metadata);

    /**
     * 发送审计通知
     *
     * @param auditType 审计类型
     * @param status 状态
     * @param source 来源
     * @param details 详情
     * @param metadata 元数据
     * @return 是否发送成功
     */
    boolean sendAuditNotification(String auditType, String status, String source,
                                String details, Map<String, Object> metadata);
}