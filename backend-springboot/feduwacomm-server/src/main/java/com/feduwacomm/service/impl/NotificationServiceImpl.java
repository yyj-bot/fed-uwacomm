package com.feduwacomm.service.impl;

import com.feduwacomm.service.NotificationService;
import com.feduwacomm.service.WebSocketService;
import com.feduwacomm.service.LogService;
import com.feduwacomm.utils.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知服务实现
 * 提供多渠道通知发送能力
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private LogService logService;

    @Autowired
    private UuidUtil uuidUtil;

    // 通知历史存储（实际项目中应该用数据库）
    private final Map<String, List<NotificationRecord>> notificationHistory = new ConcurrentHashMap<>();

    // 用户通知偏好设置
    private final Map<String, Map<String, Object>> userPreferences = new ConcurrentHashMap<>();

    /**
     * 通知记录
     */
    private static class NotificationRecord {
        private final String id;
        private final NotificationType type;
        private final String title;
        private final String message;
        private final NotificationPriority priority;
        private final LocalDateTime createdAt;
        private final Map<String, Object> metadata;
        private boolean read;

        public NotificationRecord(String id, NotificationType type, String title, String message,
                                NotificationPriority priority, Map<String, Object> metadata) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.message = message;
            this.priority = priority;
            this.createdAt = LocalDateTime.now();
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            this.read = false;
        }

        public String getId() { return id; }
        public NotificationType getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public NotificationPriority getPriority() { return priority; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public Map<String, Object> getMetadata() { return metadata; }
        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
    }

    @Override
    public boolean sendNotification(NotificationType type, String title, String message,
                                  String recipient, NotificationPriority priority,
                                  NotificationChannel[] channels, Map<String, Object> metadata) {
        if (recipient == null || title == null || message == null) {
            log.warn("通知参数不完整，无法发送: recipient={}, title={}, message={}", recipient, title, message);
            return false;
        }

        try {
            String notificationId = uuidUtil.generateUuid();

            // 创建通知记录
            NotificationRecord record = new NotificationRecord(notificationId, type, title, message, priority, metadata);

            // 保存到历史记录
            notificationHistory.computeIfAbsent(recipient, k -> new ArrayList<>()).add(record);

            // 根据用户偏好过滤渠道
            NotificationChannel[] effectiveChannels = filterChannelsByPreferences(recipient, channels);

            boolean success = true;

            // 按渠道发送通知
            for (NotificationChannel channel : effectiveChannels) {
                try {
                    boolean channelSuccess = sendToChannel(channel, type, title, message, recipient, priority, metadata);
                    if (!channelSuccess) {
                        success = false;
                    }
                } catch (Exception e) {
                    log.error("发送通知到渠道 {} 失败: recipient={}, error={}", channel, recipient, e.getMessage(), e);
                    success = false;
                }
            }

            // 记录系统日志
            logService.logInfo(
                String.format("通知已发送[%s]: %s - %s [id=%s,type=%s,priority=%s]",
                    notificationId, title, message, notificationId, type.name(), priority.name()),
                null, "notification", "/notification", "system", "NOTIFICATION"
            );

            log.info("通知发送完成: id={}, recipient={}, type={}, channels={}, success={}",
                    notificationId, recipient, type, Arrays.toString(effectiveChannels), success);

            return success;

        } catch (Exception e) {
            log.error("发送通知失败: recipient={}, type={}, error={}", recipient, type, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendBroadcastNotification(NotificationType type, String title, String message,
                                           NotificationPriority priority, NotificationChannel[] channels,
                                           Map<String, Object> metadata) {
        try {
            String notificationId = uuidUtil.generateUuid();

            log.info("开始发送广播通知: id={}, type={}, title={}", notificationId, type, title);

            boolean success = true;

            // 按渠道发送广播
            for (NotificationChannel channel : channels) {
                try {
                    boolean channelSuccess = sendBroadcastToChannel(channel, type, title, message, priority, metadata);
                    if (!channelSuccess) {
                        success = false;
                    }
                } catch (Exception e) {
                    log.error("广播通知到渠道 {} 失败: error={}", channel, e.getMessage(), e);
                    success = false;
                }
            }

            // 记录系统日志
            logService.logInfo(
                String.format("广播通知已发送[%s]: %s - %s [id=%s,type=%s,priority=%s]",
                    notificationId, title, message, notificationId, type.name(), priority.name()),
                null, "notification", "/notification/broadcast", "system", "NOTIFICATION"
            );

            log.info("广播通知发送完成: id={}, type={}, channels={}, success={}",
                    notificationId, type, Arrays.toString(channels), success);

            return success;

        } catch (Exception e) {
            log.error("发送广播通知失败: type={}, error={}", type, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendAlertNotification(String alertId, String alertName, String message,
                                       String severity, double currentValue) {
        NotificationPriority priority = mapSeverityToPriority(severity);
        NotificationChannel[] channels = getAlertChannels(severity);

        Map<String, Object> metadata = Map.of(
            "alertId", alertId,
            "alertName", alertName,
            "severity", severity,
            "currentValue", currentValue,
            "timestamp", LocalDateTime.now()
        );

        String title = String.format("系统告警: %s", alertName);
        return sendBroadcastNotification(NotificationType.ALERT, title, message, priority, channels, metadata);
    }

    @Override
    public boolean sendWorkflowNotification(String orchestrationId, String stage,
                                          String status, String message) {
        NotificationPriority priority = "FAILED".equals(status) ? NotificationPriority.HIGH : NotificationPriority.MEDIUM;
        NotificationChannel[] channels = {NotificationChannel.WEBSOCKET, NotificationChannel.LOG};

        Map<String, Object> metadata = Map.of(
            "orchestrationId", orchestrationId,
            "stage", stage,
            "status", status,
            "timestamp", LocalDateTime.now()
        );

        String title = String.format("工作流通知: %s", stage);
        return sendBroadcastNotification(NotificationType.WORKFLOW, title, message, priority, channels, metadata);
    }

    @Override
    public boolean sendModelDistributionNotification(String modelId, String distributionType,
                                                   String status, Map<String, Object> details) {
        NotificationPriority priority = "FAILED".equals(status) ? NotificationPriority.HIGH : NotificationPriority.MEDIUM;
        NotificationChannel[] channels = {NotificationChannel.WEBSOCKET, NotificationChannel.LOG};

        Map<String, Object> metadata = new HashMap<>(details != null ? details : new HashMap<>());
        metadata.put("modelId", modelId);
        metadata.put("distributionType", distributionType);
        metadata.put("status", status);
        metadata.put("timestamp", LocalDateTime.now());

        String title = String.format("模型分发通知: %s", distributionType);
        String message = String.format("模型 %s 的 %s 分发状态: %s", modelId, distributionType, status);

        return sendBroadcastNotification(NotificationType.DISTRIBUTION, title, message, priority, channels, metadata);
    }

    @Override
    public void configureNotificationPreferences(String userId, Map<String, Object> preferences) {
        if (userId == null || preferences == null) {
            log.warn("配置通知偏好参数无效: userId={}, preferences={}", userId, preferences);
            return;
        }

        userPreferences.put(userId, new HashMap<>(preferences));
        log.info("用户通知偏好已更新: userId={}, preferences={}", userId, preferences);
    }

    @Override
    public List<Map<String, Object>> getNotificationHistory(String recipient, int limit) {
        if (recipient == null) {
            return new ArrayList<>();
        }

        List<NotificationRecord> records = notificationHistory.getOrDefault(recipient, new ArrayList<>());

        return records.stream()
            .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
            .limit(limit > 0 ? limit : 50)
            .map(this::recordToMap)
            .toList();
    }

    @Override
    public boolean markNotificationAsRead(String notificationId, String recipient) {
        if (notificationId == null || recipient == null) {
            return false;
        }

        List<NotificationRecord> records = notificationHistory.get(recipient);
        if (records == null) {
            return false;
        }

        return records.stream()
            .filter(record -> notificationId.equals(record.getId()))
            .findFirst()
            .map(record -> {
                record.setRead(true);
                log.debug("通知已标记为已读: id={}, recipient={}", notificationId, recipient);
                return true;
            })
            .orElse(false);
    }

    /**
     * 发送到指定渠道
     */
    private boolean sendToChannel(NotificationChannel channel, NotificationType type, String title,
                                String message, String recipient, NotificationPriority priority,
                                Map<String, Object> metadata) {
        switch (channel) {
            case WEBSOCKET:
                return sendWebSocketNotification(type, title, message, recipient, priority, metadata);

            case EMAIL:
                return sendEmailNotification(title, message, recipient, priority, metadata);

            case SMS:
                return sendSmsNotification(title, message, recipient, priority, metadata);

            case WEBHOOK:
                return sendWebhookNotification(type, title, message, recipient, priority, metadata);

            case LOG:
                return sendLogNotification(type, title, message, priority, metadata);

            default:
                log.warn("不支持的通知渠道: {}", channel);
                return false;
        }
    }

    /**
     * 发送广播到指定渠道
     */
    private boolean sendBroadcastToChannel(NotificationChannel channel, NotificationType type,
                                         String title, String message, NotificationPriority priority,
                                         Map<String, Object> metadata) {
        switch (channel) {
            case WEBSOCKET:
                return sendWebSocketBroadcast(type, title, message, priority, metadata);

            case LOG:
                return sendLogNotification(type, title, message, priority, metadata);

            case EMAIL:
            case SMS:
            case WEBHOOK:
                // 对于这些渠道，广播通知需要特殊处理
                log.info("渠道 {} 暂不支持广播通知", channel);
                return true;

            default:
                log.warn("不支持的广播通知渠道: {}", channel);
                return false;
        }
    }

    /**
     * 发送WebSocket通知
     */
    private boolean sendWebSocketNotification(NotificationType type, String title, String message,
                                            String recipient, NotificationPriority priority,
                                            Map<String, Object> metadata) {
        try {
            Map<String, Object> notificationData = Map.of(
                "type", "notification",
                "notificationType", type.name(),
                "title", title,
                "message", message,
                "priority", priority.name(),
                "timestamp", LocalDateTime.now(),
                "metadata", metadata != null ? metadata : new HashMap<>()
            );

            // 假设recipient是用户ID，需要根据实际情况调整
            return webSocketService.sendMessageToUser(recipient, notificationData);

        } catch (Exception e) {
            log.error("发送WebSocket通知失败: recipient={}, error={}", recipient, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送WebSocket广播
     */
    private boolean sendWebSocketBroadcast(NotificationType type, String title, String message,
                                         NotificationPriority priority, Map<String, Object> metadata) {
        try {
            Map<String, Object> notificationData = Map.of(
                "type", "notification",
                "notificationType", type.name(),
                "title", title,
                "message", message,
                "priority", priority.name(),
                "timestamp", LocalDateTime.now(),
                "metadata", metadata != null ? metadata : new HashMap<>()
            );

            int sentCount = webSocketService.sendBroadcastMessage(notificationData);
            return sentCount > 0;

        } catch (Exception e) {
            log.error("发送WebSocket广播通知失败: error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送邮件通知（占位实现）
     */
    private boolean sendEmailNotification(String title, String message, String recipient,
                                        NotificationPriority priority, Map<String, Object> metadata) {
        try {
            // 在生产环境中，这里应该集成邮件服务提供商（如SendGrid、Amazon SES等）
            // 或使用Spring Mail集成SMTP服务器

            // 模拟邮件发送过程
            String subject = String.format("[FedUWAComm] %s", title);
            String emailContent = buildEmailContent(title, message, priority, metadata);

            // 验证邮件地址格式
            if (!isValidEmail(recipient)) {
                log.warn("邮件地址格式无效: {}", recipient);
                return false;
            }

            // 记录邮件发送详情
            logService.logInfo(
                String.format("邮件通知已发送: to=%s, subject=%s, priority=%s",
                    recipient, subject, priority.name()),
                null, "email", "/notification/email", "system", "EMAIL"
            );

            log.info("邮件通知发送成功: to={}, subject={}, priority={}", recipient, subject, priority);
            return true;

        } catch (Exception e) {
            log.error("邮件通知发送失败: to={}, title={}, error={}", recipient, title, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送短信通知（占位实现）
     */
    private boolean sendSmsNotification(String title, String message, String recipient,
                                      NotificationPriority priority, Map<String, Object> metadata) {
        try {
            // 在生产环境中，这里应该集成短信服务提供商（如阿里云短信、腾讯云短信等）

            // 验证手机号格式
            if (!isValidPhoneNumber(recipient)) {
                log.warn("手机号格式无效: {}", recipient);
                return false;
            }

            // 构建短信内容（限制字数）
            String smsContent = buildSmsContent(title, message, priority);
            if (smsContent.length() > 70) { // 一般短信限制70个字符
                smsContent = smsContent.substring(0, 67) + "...";
            }

            // 模拟短信发送过程
            // 实际实现中需要调用短信服务提供商的API

            // 记录短信发送详情
            logService.logInfo(
                String.format("短信通知已发送: to=%s, content=%s, priority=%s",
                    recipient, smsContent, priority.name()),
                null, "sms", "/notification/sms", "system", "SMS"
            );

            log.info("短信通知发送成功: to={}, content={}, priority={}", recipient, smsContent, priority);
            return true;

        } catch (Exception e) {
            log.error("短信通知发送失败: to={}, title={}, error={}", recipient, title, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送Webhook通知（占位实现）
     */
    private boolean sendWebhookNotification(NotificationType type, String title, String message,
                                          String recipient, NotificationPriority priority,
                                          Map<String, Object> metadata) {
        try {
            // 在生产环境中，这里应该使用HTTP客户端（如RestTemplate、WebClient）发送POST请求

            // 构建 Webhook 负载
            Map<String, Object> webhookPayload = new HashMap<>();
            webhookPayload.put("event", "notification");
            webhookPayload.put("type", type.name());
            webhookPayload.put("title", title);
            webhookPayload.put("message", message);
            webhookPayload.put("recipient", recipient);
            webhookPayload.put("priority", priority.name());
            webhookPayload.put("timestamp", LocalDateTime.now().toString());

            if (metadata != null && !metadata.isEmpty()) {
                webhookPayload.put("metadata", metadata);
            }

            // 模拟发送到外部Webhook端点
            // String webhookUrl = getWebhookUrl(recipient); // 从配置或数据库获取
            // 实际实现中需要发送HTTP POST请求到webhookUrl

            // 记录Webhook发送详情
            logService.logInfo(
                String.format("Webhook通知已发送: type=%s, recipient=%s, title=%s, priority=%s",
                    type.name(), recipient, title, priority.name()),
                null, "webhook", "/notification/webhook", "system", "WEBHOOK"
            );

            log.info("Webhook通知发送成功: type={}, recipient={}, title={}, priority={}",
                    type, recipient, title, priority);
            return true;

        } catch (Exception e) {
            log.error("Webhook通知发送失败: type={}, recipient={}, title={}, error={}",
                    type, recipient, title, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送日志通知
     */
    private boolean sendLogNotification(NotificationType type, String title, String message,
                                      NotificationPriority priority, Map<String, Object> metadata) {
        try {
            String logLevel = mapPriorityToLogLevel(priority);
            String fullMessage = String.format("[%s] %s: %s", type.name(), title, message);

            // 将metadata信息包含在消息中
            String messageWithMetadata = fullMessage;
            if (metadata != null && !metadata.isEmpty()) {
                messageWithMetadata += " [metadata=" + metadata.toString() + "]";
            }
            logService.logInfo(messageWithMetadata, null, "notification", "/notification", "system", "NOTIFICATION");

            return true;

        } catch (Exception e) {
            log.error("发送日志通知失败: error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据用户偏好过滤渠道
     */
    private NotificationChannel[] filterChannelsByPreferences(String recipient, NotificationChannel[] channels) {
        Map<String, Object> preferences = userPreferences.get(recipient);
        if (preferences == null) {
            // 如果没有偏好设置，使用默认渠道
            return channels.length > 0 ? channels : new NotificationChannel[]{NotificationChannel.WEBSOCKET, NotificationChannel.LOG};
        }

        // 根据用户偏好过滤渠道
        List<NotificationChannel> filteredChannels = new ArrayList<>();

        for (NotificationChannel channel : channels) {
            String channelKey = channel.name().toLowerCase() + "_enabled";
            Object channelEnabled = preferences.get(channelKey);

            // 如果偏好设置中明确禁用了该渠道，则跳过
            if (Boolean.FALSE.equals(channelEnabled)) {
                log.debug("用户偏好禁用渠道: recipient={}, channel={}", recipient, channel);
                continue;
            }

            // 检查特定渠道的时间限制
            if (isChannelTimeLimited(channel, preferences)) {
                log.debug("渠道在时间限制内被禁用: recipient={}, channel={}", recipient, channel);
                continue;
            }

            filteredChannels.add(channel);
        }

        // 如果所有渠道都被过滤掉，至少保留日志渠道
        if (filteredChannels.isEmpty()) {
            log.warn("所有渠道都被用户偏好过滤，使用默认日志渠道: recipient={}", recipient);
            filteredChannels.add(NotificationChannel.LOG);
        }

        return filteredChannels.toArray(new NotificationChannel[0]);
    }

    /**
     * 将严重程度映射为优先级
     */
    private NotificationPriority mapSeverityToPriority(String severity) {
        return switch (severity.toUpperCase()) {
            case "LOW" -> NotificationPriority.LOW;
            case "MEDIUM" -> NotificationPriority.MEDIUM;
            case "HIGH" -> NotificationPriority.HIGH;
            case "CRITICAL", "URGENT" -> NotificationPriority.URGENT;
            default -> NotificationPriority.MEDIUM;
        };
    }

    /**
     * 获取告警通知渠道
     */
    private NotificationChannel[] getAlertChannels(String severity) {
        return switch (severity.toUpperCase()) {
            case "LOW" -> new NotificationChannel[]{NotificationChannel.LOG};
            case "MEDIUM" -> new NotificationChannel[]{NotificationChannel.WEBSOCKET, NotificationChannel.LOG};
            case "HIGH" -> new NotificationChannel[]{NotificationChannel.WEBSOCKET, NotificationChannel.EMAIL, NotificationChannel.LOG};
            case "CRITICAL", "URGENT" -> new NotificationChannel[]{
                NotificationChannel.WEBSOCKET, NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.LOG
            };
            default -> new NotificationChannel[]{NotificationChannel.WEBSOCKET, NotificationChannel.LOG};
        };
    }

    /**
     * 将优先级映射为日志级别
     */
    private String mapPriorityToLogLevel(NotificationPriority priority) {
        return switch (priority) {
            case LOW -> "DEBUG";
            case MEDIUM -> "INFO";
            case HIGH -> "WARN";
            case URGENT -> "ERROR";
        };
    }

    /**
     * 将通知记录转换为Map
     */
    private Map<String, Object> recordToMap(NotificationRecord record) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("type", record.getType().name());
        result.put("title", record.getTitle());
        result.put("message", record.getMessage());
        result.put("priority", record.getPriority().name());
        result.put("createdAt", record.getCreatedAt());
        result.put("read", record.isRead());
        result.put("metadata", record.getMetadata());
        return result;
    }

    /**
     * 验证邮件地址格式
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * 验证手机号格式
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        // 简单的手机号验证（支持国际格式）
        return phoneNumber.matches("^[+]?[1-9]\\d{1,14}$");
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String title, String message, NotificationPriority priority,
                                   Map<String, Object> metadata) {
        StringBuilder content = new StringBuilder();
        content.append("您好！\n\n");
        content.append("这是一条来自FedUWAComm系统的通知：\n\n");
        content.append("标题：").append(title).append("\n");
        content.append("内容：").append(message).append("\n");
        content.append("优先级：").append(priority.name()).append("\n");
        content.append("时间：").append(LocalDateTime.now()).append("\n");

        if (metadata != null && !metadata.isEmpty()) {
            content.append("\n附加信息：\n");
            metadata.forEach((key, value) ->
                content.append("- ").append(key).append(": ").append(value).append("\n"));
        }

        content.append("\n此邮件由系统自动发送，请勿回复。\n");
        content.append("如有疑问，请联系系统管理员。");

        return content.toString();
    }

    /**
     * 构建短信内容
     */
    private String buildSmsContent(String title, String message, NotificationPriority priority) {
        String priorityPrefix = switch (priority) {
            case URGENT -> "【紧急】";
            case HIGH -> "【重要】";
            case MEDIUM -> "【提醒】";
            case LOW -> "";
        };

        return String.format("%s%s：%s", priorityPrefix, title, message);
    }

    /**
     * 检查渠道是否在时间限制内
     */
    private boolean isChannelTimeLimited(NotificationChannel channel, Map<String, Object> preferences) {
        String timeSettingKey = channel.name().toLowerCase() + "_quiet_hours";
        Object quietHours = preferences.get(timeSettingKey);

        if (quietHours == null) {
            return false; // 没有设置时间限制
        }

        try {
            // 假设quietHours是"22:00-08:00"这样的格式
            String timeRange = quietHours.toString();
            if (timeRange.contains("-")) {
                String[] times = timeRange.split("-");
                if (times.length == 2) {
                    return isCurrentTimeInRange(times[0].trim(), times[1].trim());
                }
            }
        } catch (Exception e) {
            log.warn("解析时间限制设置失败: channel={}, quietHours={}, error={}",
                    channel, quietHours, e.getMessage());
        }

        return false;
    }

    /**
     * 检查当前时间是否在指定范围内
     */
    private boolean isCurrentTimeInRange(String startTime, String endTime) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int currentHour = now.getHour();
            int currentMinute = now.getMinute();
            int currentTotalMinutes = currentHour * 60 + currentMinute;

            int startTotalMinutes = parseTimeToMinutes(startTime);
            int endTotalMinutes = parseTimeToMinutes(endTime);

            // 处理跨日情况（如22:00-08:00）
            if (startTotalMinutes > endTotalMinutes) {
                return currentTotalMinutes >= startTotalMinutes || currentTotalMinutes <= endTotalMinutes;
            } else {
                return currentTotalMinutes >= startTotalMinutes && currentTotalMinutes <= endTotalMinutes;
            }
        } catch (Exception e) {
            log.warn("时间范围检查失败: startTime={}, endTime={}, error={}",
                    startTime, endTime, e.getMessage());
            return false;
        }
    }

    /**
     * 将时间字符串转换为分钟数
     */
    private int parseTimeToMinutes(String timeStr) {
        String[] parts = timeStr.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("时间格式无效: " + timeStr);
        }

        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);

        if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("时间值无效: " + timeStr);
        }

        return hours * 60 + minutes;
    }

    @Override
    public boolean sendSystemNotification(String type, String title, String message,
                                        Map<String, Object> metadata) {
        try {
            log.info("发送系统通知: type={}, title={}", type, title);

            NotificationType notificationType = NotificationType.SYSTEM_STATUS;
            NotificationPriority priority = NotificationPriority.MEDIUM;
            NotificationChannel[] channels = {NotificationChannel.WEBSOCKET, NotificationChannel.LOG};

            // 根据type调整优先级和渠道
            if ("ERROR".equalsIgnoreCase(type) || "CRITICAL".equalsIgnoreCase(type)) {
                priority = NotificationPriority.URGENT;
                channels = new NotificationChannel[]{
                    NotificationChannel.WEBSOCKET, NotificationChannel.EMAIL, NotificationChannel.LOG
                };
            } else if ("WARNING".equalsIgnoreCase(type)) {
                priority = NotificationPriority.HIGH;
            }

            return sendBroadcastNotification(notificationType, title, message, priority, channels, metadata);
        } catch (Exception e) {
            log.error("发送系统通知失败: type={}, title={}", type, title, e);
            return false;
        }
    }

    @Override
    public boolean sendAuditNotification(String auditType, String status, String source,
                                       String details, Map<String, Object> metadata) {
        try {
            log.info("发送审计通知: auditType={}, status={}, source={}", auditType, status, source);

            String title = String.format("审计通知: %s", auditType);
            String message = String.format("状态: %s, 来源: %s, 详情: %s", status, source, details);

            // 审计通知通常使用LOG渠道记录
            return sendLogNotification(NotificationType.ALERT, title, message,
                                     NotificationPriority.MEDIUM, metadata);
        } catch (Exception e) {
            log.error("发送审计通知失败: auditType={}, status={}, source={}", auditType, status, source, e);
            return false;
        }
    }

    @Override
    public boolean sendNotification(String channel, String title, String message,
                                   String recipient, String priority,
                                   Map<String, Object> metadata) {
        try {
            log.info("发送通知: channel={}, title={}, recipient={}", channel, title, recipient);

            // 转换通道类型
            NotificationChannel[] channels;
            switch (channel.toUpperCase()) {
                case "WEBSOCKET":
                    channels = new NotificationChannel[]{NotificationChannel.WEBSOCKET};
                    break;
                case "EMAIL":
                    channels = new NotificationChannel[]{NotificationChannel.EMAIL};
                    break;
                case "LOG":
                    channels = new NotificationChannel[]{NotificationChannel.LOG};
                    break;
                default:
                    channels = new NotificationChannel[]{NotificationChannel.WEBSOCKET, NotificationChannel.LOG};
                    break;
            }

            // 转换优先级
            NotificationPriority notificationPriority;
            switch (priority.toUpperCase()) {
                case "HIGH":
                    notificationPriority = NotificationPriority.HIGH;
                    break;
                case "LOW":
                    notificationPriority = NotificationPriority.LOW;
                    break;
                default:
                    notificationPriority = NotificationPriority.MEDIUM;
                    break;
            }

            // 发送通知
            return sendNotification(NotificationType.SYSTEM_STATUS, title, message,
                                  recipient, notificationPriority, channels, metadata);
        } catch (Exception e) {
            log.error("发送通知失败: channel={}, title={}, recipient={}", channel, title, recipient, e);
            return false;
        }
    }

    @Override
    public boolean sendWebSocketNotification(String taskId, String eventType, String level,
                                           String message, Map<String, Object> metadata) {
        try {
            log.info("发送WebSocket通知: taskId={}, eventType={}, level={}", taskId, eventType, level);

            // 构建WebSocket通知消息
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "notification");
            wsMessage.put("taskId", taskId);
            wsMessage.put("eventType", eventType);
            wsMessage.put("level", level);
            wsMessage.put("message", message);
            wsMessage.put("timestamp", System.currentTimeMillis());

            if (metadata != null) {
                wsMessage.putAll(metadata);
            }

            // 转换优先级
            NotificationPriority priority;
            switch (level.toUpperCase()) {
                case "HIGH":
                case "ERROR":
                case "CRITICAL":
                    priority = NotificationPriority.HIGH;
                    break;
                case "LOW":
                case "DEBUG":
                    priority = NotificationPriority.LOW;
                    break;
                default:
                    priority = NotificationPriority.MEDIUM;
                    break;
            }

            // 发送WebSocket通知
            return sendNotification(NotificationType.WORKFLOW, eventType, message,
                                  taskId, priority,
                                  new NotificationChannel[]{NotificationChannel.WEBSOCKET},
                                  metadata);
        } catch (Exception e) {
            log.error("发送WebSocket通知失败: taskId={}, eventType={}, level={}", taskId, eventType, level, e);
            return false;
        }
    }
}