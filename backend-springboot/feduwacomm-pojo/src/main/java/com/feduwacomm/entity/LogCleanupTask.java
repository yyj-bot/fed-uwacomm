package com.feduwacomm.entity;

import com.feduwacomm.enums.LogCleanupStrategy;
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
public class LogCleanupTask {

    private String id;
    private String cleanupId;
    private TaskStatus status;
    private LogCleanupStrategy strategy;
    private String cleanupConditions;
    private Integer progress;
    private Long estimatedRecords;
    private Long deletedRecords;
    private Long estimatedSize;
    private Long freedSpace;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String createdBy;
    private String errorMessage;
}