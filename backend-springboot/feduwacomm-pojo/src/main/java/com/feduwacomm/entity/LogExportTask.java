package com.feduwacomm.entity;

import com.feduwacomm.enums.LogExportFormat;
import com.feduwacomm.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogExportTask {
    
    private String id;                      // 主键ID，32位UUID
    
    private String exportId;                // 导出任务ID（用户可见）
    
    private TaskStatus status;              // 任务状态：PENDING, PROCESSING, COMPLETED, FAILED
    
    private LogExportFormat format;         // 导出格式：CSV, JSON, EXCEL
    
    private String filterConditions;        // 过滤条件，JSON格式存储查询参数
    
    private Integer progress;               // 进度百分比 0-100
    
    private Long totalRecords;              // 总记录数
    
    private Long processedRecords;          // 已处理记录数
    
    private Long fileSize;                  // 文件大小（字节）
    
    private String filePath;                // 文件存储路径
    
    private String downloadUrl;             // 下载URL
    
    private Integer estimatedTime;          // 预估时间（秒）
    
    private LocalDateTime expiresAt;        // 过期时间
    
    private LocalDateTime createdAt;        // 创建时间
    
    private LocalDateTime startedAt;        // 开始处理时间
    
    private LocalDateTime completedAt;      // 完成时间
    
    private String errorMessage;            // 错误信息
    
    private String createdBy;               // 创建用户
    
    private Boolean includeDetails;         // 是否包含详细信息
}