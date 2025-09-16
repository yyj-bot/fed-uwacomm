package com.feduwacomm.entity;

import com.feduwacomm.enums.LogLevel;
import com.feduwacomm.enums.LogCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmRuntimeLog {
    
    private String id;                  // 主键ID，32位UUID
    
    private LogLevel level;             // 日志级别：INFO, WARN, ERROR, DEBUG
    
    private String category;            // 日志分类：SYSTEM, USER, VM, TASK, DATA, MODEL, SECURITY, PERFORMANCE
    
    private String vmId;                // 虚拟机ID，32位UUID格式
    
    private String taskId;              // 任务ID，32位UUID格式
    
    private String message;             // 日志消息内容
    
    private String details;             // 详细信息，JSON格式
    
    private LocalDateTime createdAt;    // 创建时间
    
    // 别名getter方法，保持向后兼容
    public String getLogId() {
        return this.id;
    }
    
    public void setLogId(String logId) {
        this.id = logId;
    }
    
    // 获取分类枚举
    public LogCategory getCategoryEnum() {
        if (this.category == null) {
            return LogCategory.SYSTEM;
        }
        
        try {
            return LogCategory.valueOf(this.category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LogCategory.SYSTEM;
        }
    }
    
    // 设置分类枚举
    public void setCategoryEnum(LogCategory category) {
        this.category = category != null ? category.name() : LogCategory.SYSTEM.name();
    }
}