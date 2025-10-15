package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 联邦学习任务详细信息响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailVO {

    private String taskId;
    private String taskName;
    private String taskType;
    private String description;
    private String status;
    private String algorithm;
    
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime resumedAt;
    private LocalDateTime stoppedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    
    private Integer currentRound;
    private Integer totalRounds;
    private Double progress;
    
    private List<ParticipantVO> participants;
    private MetricsVO metrics;
    private TaskOperationVO.ConfigSummaryVO configSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantVO {
        private String vmId;
        private String role;
        private String status;
        private LocalDateTime lastHeartbeat;
        private Integer currentEpoch;
        private Double loss;
        private Double accuracy;
        private String dataSource;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsVO {
        private Double globalLoss;
        private Double globalAccuracy;
        private Integer communicationRounds;
        private Integer dataProcessed;
        private Integer estimatedTimeRemaining; // 预估剩余时间(秒)
    }
}
