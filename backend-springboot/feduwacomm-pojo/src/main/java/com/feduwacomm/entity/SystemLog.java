package com.feduwacomm.entity;

import com.feduwacomm.enums.LogCategory;
import com.feduwacomm.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {
    
    // system_logs 表字段
    private String id;              // 对应 system_logs.id
    private LocalDateTime timestamp; // 对应 system_logs.timestamp
    private String level;           // 对应 system_logs.level (存储字符串)
    private String logger;          // 对应 system_logs.logger (用于存储category信息)
    private String message;         // 对应 system_logs.message
    private String thread;          // 对应 system_logs.thread
    private String userId;          // 对应 system_logs.user_id
    private String username;        // 对应 system_logs.username
    private String requestUri;      // 对应 system_logs.request_uri
    private String clientIp;        // 对应 system_logs.client_ip
    private String environment;     // 对应 system_logs.environment
    private String exception;       // 对应 system_logs.exception
    private LocalDateTime createdAt; // 对应 system_logs.created_at
    
    // 虚拟字段 - 通过解析 logger 字段获得
    private LogLevel levelEnum;     // level 字符串转换的枚举
    private LogCategory category;   // 从 logger 字段解析的分类
    private String vmId;           // 从 message 或 logger 解析的虚拟机ID
    private String taskId;         // 从 message 或 logger 解析的任务ID
    private String details;        // JSON格式详细信息，可从message解析
    
    // 别名getter方法，保持向后兼容
    public String getLogId() {
        return this.id;
    }
    
    public void setLogId(String logId) {
        this.id = logId;
    }
    
    // 解析 logger 字段获取分类信息
    public LogCategory getCategory() {
        if (this.logger == null) {
            return LogCategory.SYSTEM;
        }
        
        if (this.logger.startsWith("TASK.")) {
            return LogCategory.TASK;
        } else if (this.logger.startsWith("VM.")) {
            return LogCategory.VM;
        } else if (this.logger.startsWith("MODEL.")) {
            return LogCategory.MODEL;
        } else {
            return LogCategory.SYSTEM;
        }
    }
    
    // 获取Level枚举
    public LogLevel getLevelEnum() {
        if (this.level == null) {
            return LogLevel.INFO;
        }
        
        try {
            return LogLevel.valueOf(this.level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LogLevel.INFO;
        }
    }
}