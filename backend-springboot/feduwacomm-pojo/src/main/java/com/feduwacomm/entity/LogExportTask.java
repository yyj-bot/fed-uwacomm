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
    
    private String exportId;
    private TaskStatus status;
    private LogExportFormat format;
    private String filterConditions;
    private Integer progress;
    private Long totalRecords;
    private Long processedRecords;
    private Long fileSize;
    private String downloadUrl;
    private String filePath;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String createdBy;
    private String errorMessage;
}