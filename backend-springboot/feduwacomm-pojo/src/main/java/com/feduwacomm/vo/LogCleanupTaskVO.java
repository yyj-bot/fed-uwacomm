package com.feduwacomm.vo;

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
public class LogCleanupTaskVO {

    private String cleanupId;
    private TaskStatus status;
    private LogCleanupStrategy strategy;
    private Integer progress;
    private Long estimatedRecords;
    private Long deletedRecords;
    private Long estimatedSize;
    private Long freedSpace;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}