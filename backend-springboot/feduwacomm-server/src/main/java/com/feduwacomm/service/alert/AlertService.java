package com.feduwacomm.service.alert;

import com.feduwacomm.service.LogService;
import com.feduwacomm.mapper.SystemLogMapper;
import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.utils.IpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警服务
 * 负责监控系统状态并触发相应的告警
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Service
public class AlertService {

    private static final Logger logger = LogManager.getLogger(AlertService.class);
    
    @Autowired
    private SystemLogMapper systemLogMapper;
    
    @Autowired
    private LogService logService;
    
    // 告警规则配置
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    
    // 告警状态跟踪
    private final Map<String, AlertStatus> alertStatuses = new ConcurrentHashMap<>();
    
    // 告警历史记录
    private final List<AlertRecord> alertHistory = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * 告警规则
     */
    public static class AlertRule {
        private String id;
        private String name;
        private String type;
        private String condition;
        private double threshold;
        private int timeWindowMinutes;
        private boolean enabled;
        private String severity;
        private String description;
        
        public AlertRule(String id, String name, String type, String condition, 
                        double threshold, int timeWindowMinutes, String severity, String description) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.condition = condition;
            this.threshold = threshold;
            this.timeWindowMinutes = timeWindowMinutes;
            this.enabled = true;
            this.severity = severity;
            this.description = description;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getCondition() { return condition; }
        public double getThreshold() { return threshold; }
        public int getTimeWindowMinutes() { return timeWindowMinutes; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
    }
    
    /**
     * 告警状态
     */
    public static class AlertStatus {
        private String alertId;
        private boolean active;
        private LocalDateTime lastTriggered;
        private LocalDateTime lastResolved;
        private int triggerCount;
        private double currentValue;
        
        public AlertStatus(String alertId) {
            this.alertId = alertId;
            this.active = false;
            this.triggerCount = 0;
        }
        
        // Getters and setters
        public String getAlertId() { return alertId; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public LocalDateTime getLastTriggered() { return lastTriggered; }
        public void setLastTriggered(LocalDateTime lastTriggered) { this.lastTriggered = lastTriggered; }
        public LocalDateTime getLastResolved() { return lastResolved; }
        public void setLastResolved(LocalDateTime lastResolved) { this.lastResolved = lastResolved; }
        public int getTriggerCount() { return triggerCount; }
        public void incrementTriggerCount() { this.triggerCount++; }
        public double getCurrentValue() { return currentValue; }
        public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
    }
    
    /**
     * 告警记录
     */
    public static class AlertRecord {
        private String alertId;
        private String alertName;
        private LocalDateTime triggeredAt;
        private LocalDateTime resolvedAt;
        private String message;
        private String severity;
        private double value;
        private boolean resolved;
        
        public AlertRecord(String alertId, String alertName, String message, String severity, double value) {
            this.alertId = alertId;
            this.alertName = alertName;
            this.triggeredAt = LocalDateTime.now();
            this.message = message;
            this.severity = severity;
            this.value = value;
            this.resolved = false;
        }
        
        // Getters and setters
        public String getAlertId() { return alertId; }
        public String getAlertName() { return alertName; }
        public LocalDateTime getTriggeredAt() { return triggeredAt; }
        public LocalDateTime getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; this.resolved = true; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }
        public double getValue() { return value; }
        public boolean isResolved() { return resolved; }
    }
    
    /**
     * 初始化告警规则
     */
    public void initializeAlertRules() {
        try {
            logger.info("初始化告警规则...");
            
            // 错误率告警
            AlertRule errorRateAlert = new AlertRule(
                "alert_error_rate", 
                "错误率告警", 
                "ERROR_RATE",
                "error_rate > threshold",
                5.0, // 5%
                60, // 1小时
                "HIGH",
                "当系统错误率超过5%时触发告警"
            );
            alertRules.put(errorRateAlert.getId(), errorRateAlert);
            alertStatuses.put(errorRateAlert.getId(), new AlertStatus(errorRateAlert.getId()));
            
            // 内存使用率告警
            AlertRule memoryAlert = new AlertRule(
                "alert_memory_usage", 
                "内存使用率告警", 
                "MEMORY_USAGE",
                "memory_usage > threshold",
                80.0, // 80%
                30, // 30分钟
                "MEDIUM",
                "当内存使用率超过80%时触发告警"
            );
            alertRules.put(memoryAlert.getId(), memoryAlert);
            alertStatuses.put(memoryAlert.getId(), new AlertStatus(memoryAlert.getId()));
            
            // CPU使用率告警
            AlertRule cpuAlert = new AlertRule(
                "alert_cpu_usage", 
                "CPU使用率告警", 
                "CPU_USAGE",
                "cpu_usage > threshold",
                85.0, // 85%
                30, // 30分钟
                "MEDIUM",
                "当CPU使用率超过85%时触发告警"
            );
            alertRules.put(cpuAlert.getId(), cpuAlert);
            alertStatuses.put(cpuAlert.getId(), new AlertStatus(cpuAlert.getId()));
            
            // 磁盘使用率告警
            AlertRule diskAlert = new AlertRule(
                "alert_disk_usage", 
                "磁盘使用率告警", 
                "DISK_USAGE",
                "disk_usage > threshold",
                90.0, // 90%
                60, // 1小时
                "HIGH",
                "当磁盘使用率超过90%时触发告警"
            );
            alertRules.put(diskAlert.getId(), diskAlert);
            alertStatuses.put(diskAlert.getId(), new AlertStatus(diskAlert.getId()));
            
            // 数据库连接告警
            AlertRule dbConnectionAlert = new AlertRule(
                "alert_db_connections", 
                "数据库连接告警", 
                "DB_CONNECTIONS",
                "db_connections > threshold",
                80.0, // 80个连接
                30, // 30分钟
                "MEDIUM",
                "当数据库连接数超过80时触发告警"
            );
            alertRules.put(dbConnectionAlert.getId(), dbConnectionAlert);
            alertStatuses.put(dbConnectionAlert.getId(), new AlertStatus(dbConnectionAlert.getId()));
            
            logger.info("告警规则初始化完成，共 {} 条规则", alertRules.size());
            
        } catch (Exception e) {
            logger.error("初始化告警规则失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定期检查告警规则
     */
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkAlertRules() {
        try {
            logger.debug("开始检查告警规则...");
            
            for (AlertRule rule : alertRules.values()) {
                if (rule.isEnabled()) {
                    checkSingleAlert(rule);
                }
            }
            
            // 清理过期的告警历史（保留最近30天）
            cleanupAlertHistory();
            
        } catch (Exception e) {
            logger.error("检查告警规则失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查单个告警规则
     */
    private void checkSingleAlert(AlertRule rule) {
        try {
            AlertStatus status = alertStatuses.get(rule.getId());
            if (status == null) {
                status = new AlertStatus(rule.getId());
                alertStatuses.put(rule.getId(), status);
            }
            
            double currentValue = getCurrentMetricValue(rule.getType());
            status.setCurrentValue(currentValue);
            
            boolean shouldTrigger = evaluateAlertCondition(rule, currentValue);
            
            if (shouldTrigger && !status.isActive()) {
                // 触发告警
                triggerAlert(rule, status, currentValue);
            } else if (!shouldTrigger && status.isActive()) {
                // 解除告警
                resolveAlert(rule, status);
            }
            
            logger.debug("检查告警规则: {} = {}, 阈值: {}, 当前状态: {}", 
                        rule.getName(), currentValue, rule.getThreshold(), 
                        status.isActive() ? "ACTIVE" : "INACTIVE");
                        
        } catch (Exception e) {
            logger.warn("检查告警规则失败 [{}]: {}", rule.getName(), e.getMessage());
        }
    }
    
    /**
     * 获取当前指标值
     */
    private double getCurrentMetricValue(String metricType) {
        try {
            return switch (metricType) {
                case "ERROR_RATE" -> getErrorRate();
                case "MEMORY_USAGE" -> getMemoryUsage();
                case "CPU_USAGE" -> getCpuUsage();
                case "DISK_USAGE" -> getDiskUsage();
                case "DB_CONNECTIONS" -> getDatabaseConnections();
                default -> 0.0;
            };
        } catch (Exception e) {
            logger.warn("获取指标值失败 [{}]: {}", metricType, e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 评估告警条件
     */
    private boolean evaluateAlertCondition(AlertRule rule, double currentValue) {
        return switch (rule.getCondition()) {
            case "error_rate > threshold" -> currentValue > rule.getThreshold();
            case "memory_usage > threshold" -> currentValue > rule.getThreshold();
            case "cpu_usage > threshold" -> currentValue > rule.getThreshold();
            case "disk_usage > threshold" -> currentValue > rule.getThreshold();
            case "db_connections > threshold" -> currentValue > rule.getThreshold();
            default -> false;
        };
    }
    
    /**
     * 触发告警
     */
    private void triggerAlert(AlertRule rule, AlertStatus status, double currentValue) {
        try {
            status.setActive(true);
            status.setLastTriggered(LocalDateTime.now());
            status.incrementTriggerCount();
            
            String message = String.format("%s：当前值 %.2f %s，超过阈值 %.2f", 
                                          rule.getName(), currentValue, 
                                          getMetricUnit(rule.getType()), rule.getThreshold());
            
            // 创建告警记录
            AlertRecord record = new AlertRecord(rule.getId(), rule.getName(), message, rule.getSeverity(), currentValue);
            alertHistory.add(record);
            
            // 记录到系统日志
            logService.logWarn(message, null, "system", "/alert", IpUtil.getCurrentIpOrDefault(), "ALERT");
            
            // 发送通知（这里可以扩展为邮件、短信、webhook等）
            sendAlertNotification(rule, message, currentValue);
            
            logger.warn("告警触发: {} - {}", rule.getName(), message);
            
        } catch (Exception e) {
            logger.error("触发告警失败 [{}]: {}", rule.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * 解除告警
     */
    private void resolveAlert(AlertRule rule, AlertStatus status) {
        try {
            status.setActive(false);
            status.setLastResolved(LocalDateTime.now());
            
            String message = String.format("%s已恢复正常，当前值: %.2f %s", 
                                          rule.getName(), status.getCurrentValue(),
                                          getMetricUnit(rule.getType()));
            
            // 更新最近的告警记录
            alertHistory.stream()
                    .filter(r -> r.getAlertId().equals(rule.getId()) && !r.isResolved())
                    .findFirst()
                    .ifPresent(r -> r.setResolvedAt(LocalDateTime.now()));
            
            // 记录到系统日志
            logService.logInfo(message, null, "system", "/alert", IpUtil.getCurrentIpOrDefault(), "ALERT");
            
            logger.info("告警解除: {} - {}", rule.getName(), message);
            
        } catch (Exception e) {
            logger.error("解除告警失败 [{}]: {}", rule.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送告警通知
     */
    private void sendAlertNotification(AlertRule rule, String message, double currentValue) {
        try {
            // TODO: 实现具体的通知机制
            // - 邮件通知
            // - 短信通知  
            // - 企业微信/钉钉通知
            // - Webhook通知
            // - WebSocket推送给前端
            
            logger.info("发送告警通知: {}", message);
            
        } catch (Exception e) {
            logger.warn("发送告警通知失败: {}", e.getMessage());
        }
    }
    
    // ==================== 指标获取方法 ====================
    
    private double getErrorRate() {
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            LocalDateTime now = LocalDateTime.now();
            
            long totalLogs = systemLogMapper.countByCondition(LogQueryDTO.builder()
                    .startTime(oneHourAgo)
                    .endTime(now)
                    .build());
            long errorLogs = systemLogMapper.countByLevel("ERROR", oneHourAgo, now);
            
            return totalLogs > 0 ? (double) errorLogs / totalLogs * 100 : 0.0;
        } catch (Exception e) {
            logger.debug("获取错误率失败: {}", e.getMessage());
            return 0.0;
        }
    }
    
    private double getMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            return maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private double getCpuUsage() {
        try {
            // 这里可以集成更精确的CPU监控
            // 目前返回一个模拟值
            return Math.random() * 30 + 10; // 10-40%
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private double getDiskUsage() {
        try {
            java.io.File root = new java.io.File("/");
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            long used = total - free;
            
            return total > 0 ? (double) used / total * 100 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private double getDatabaseConnections() {
        try {
            // 这里可以集成实际的数据库连接池监控
            return Math.random() * 20 + 10; // 10-30个连接
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private String getMetricUnit(String metricType) {
        return switch (metricType) {
            case "ERROR_RATE", "MEMORY_USAGE", "CPU_USAGE", "DISK_USAGE" -> "%";
            case "DB_CONNECTIONS" -> "个";
            default -> "";
        };
    }
    
    // ==================== 公共接口方法 ====================
    
    /**
     * 获取所有告警规则
     */
    public List<Map<String, Object>> getAllAlertRules() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (AlertRule rule : alertRules.values()) {
            AlertStatus status = alertStatuses.get(rule.getId());
            
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("alertId", rule.getId());
            alertData.put("name", rule.getName());
            alertData.put("type", rule.getType());
            alertData.put("condition", rule.getCondition());
            alertData.put("threshold", rule.getThreshold());
            alertData.put("status", status != null && status.isActive() ? "ACTIVE" : "INACTIVE");
            alertData.put("enabled", rule.isEnabled());
            alertData.put("severity", rule.getSeverity());
            alertData.put("description", rule.getDescription());
            alertData.put("lastTriggered", status != null ? status.getLastTriggered() : null);
            alertData.put("triggerCount", status != null ? status.getTriggerCount() : 0);
            alertData.put("currentValue", status != null ? status.getCurrentValue() : 0.0);
            
            result.add(alertData);
        }
        
        return result;
    }
    
    /**
     * 获取告警历史
     */
    public List<Map<String, Object>> getAlertHistory(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取最近的告警记录
        List<AlertRecord> recentRecords = alertHistory.stream()
                .sorted((r1, r2) -> r2.getTriggeredAt().compareTo(r1.getTriggeredAt()))
                .limit(limit)
                .toList();
        
        for (AlertRecord record : recentRecords) {
            Map<String, Object> historyData = new HashMap<>();
            historyData.put("alertId", record.getAlertId());
            historyData.put("alertName", record.getAlertName());
            historyData.put("triggeredAt", record.getTriggeredAt());
            historyData.put("resolvedAt", record.getResolvedAt());
            historyData.put("message", record.getMessage());
            historyData.put("severity", record.getSeverity());
            historyData.put("value", record.getValue());
            historyData.put("resolved", record.isResolved());
            
            result.add(historyData);
        }
        
        return result;
    }
    
    /**
     * 更新告警规则状态
     */
    public void updateAlertRuleStatus(String alertId, boolean enabled) {
        AlertRule rule = alertRules.get(alertId);
        if (rule != null) {
            rule.setEnabled(enabled);
            logger.info("告警规则 {} 已{}启用", rule.getName(), enabled ? "" : "禁");
        }
    }
    
    /**
     * 清理告警历史
     */
    private void cleanupAlertHistory() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            alertHistory.removeIf(record -> record.getTriggeredAt().isBefore(cutoffTime));
        } catch (Exception e) {
            logger.warn("清理告警历史失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取告警统计信息
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long activeAlerts = alertStatuses.values().stream()
                .mapToLong(status -> status.isActive() ? 1L : 0L)
                .sum();
        
        long totalRules = alertRules.size();
        long enabledRules = alertRules.values().stream()
                .mapToLong(rule -> rule.isEnabled() ? 1L : 0L)
                .sum();
        
        // 最近24小时的告警数量
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        long recentAlerts = alertHistory.stream()
                .mapToLong(record -> record.getTriggeredAt().isAfter(yesterday) ? 1L : 0L)
                .sum();
        
        stats.put("activeAlerts", activeAlerts);
        stats.put("totalRules", totalRules);
        stats.put("enabledRules", enabledRules);
        stats.put("recentAlerts", recentAlerts);
        stats.put("historySize", alertHistory.size());
        
        return stats;
    }
}