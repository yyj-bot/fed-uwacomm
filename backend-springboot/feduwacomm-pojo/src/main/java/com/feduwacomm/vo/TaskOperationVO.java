package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 联邦学习任务操作响应VO
 * 用于任务创建、启动、暂停、恢复、停止、取消、删除等操作的响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskOperationVO {

    private String taskId;
    private String taskName;
    private String status;
    
    // 创建操作特有字段
    private LocalDateTime createdAt;
    private String createdBy;
    private Integer participantCount;
    private Integer estimatedDuration;
    
    // 启动操作特有字段
    private LocalDateTime startedAt;
    private Integer currentRound;
    private List<ParticipantStatus> participants;
    
    // 暂停操作特有字段
    private LocalDateTime pausedAt;
    private ResumePointVO resumePoint;
    
    // 恢复操作特有字段
    private LocalDateTime resumedAt;
    
    // 停止操作特有字段
    private LocalDateTime stoppedAt;
    private Integer finalRound;
    private Boolean checkpointSaved;
    private String checkpointPath;
    
    // 取消操作特有字段
    private LocalDateTime cancelledAt;
    private String reason;
    
    // 删除操作特有字段
    private LocalDateTime deletedAt;
    private Boolean dataDeleted;
    private Boolean modelPreserved;
    
    // 配置更新特有字段
    private LocalDateTime updatedAt;
    private String configVersion;

    // 通用响应字段
    private String message;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantStatus {
        private String vmId;
        private String status;
        private String dataSource;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumePointVO {
        private Integer round;
        private String step;
    }
}