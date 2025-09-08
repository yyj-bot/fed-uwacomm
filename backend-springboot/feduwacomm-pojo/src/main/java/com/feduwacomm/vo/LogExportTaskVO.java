package com.feduwacomm.vo;

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
public class LogExportTaskVO {

    private String exportId;
    private TaskStatus status;
    private LogExportFormat format;
    private Integer progress;
    private Long totalRecords;
    private Long processedRecords;
    private Long fileSize;
    private String downloadUrl;
    private Integer estimatedTime;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}