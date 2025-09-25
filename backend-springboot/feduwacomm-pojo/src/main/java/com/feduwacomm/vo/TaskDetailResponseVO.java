package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务详情查询响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailResponseVO {

    private String taskId;
    private String taskName;
    private String status;
    private ProgressVO progress;
    private List<ParticipantDetailVO> participants;
    private MetricsVO metrics;
    private ResourceUsageVO resourceUsage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressVO {
        private Integer currentRound;
        private Integer totalRounds;
        private Double completionPercentage;
        private String estimatedRemaining;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDetailVO {
        private String vmId;
        private String status;
        private Double currentAccuracy;
        private Double currentLoss;
        private LocalDateTime lastUpdate;
        private Integer roundsCompleted;
        private Integer averageRoundTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsVO {
        private Double globalAccuracy;
        private Double globalLoss;
        private Double convergenceRate;
        private String convergenceTrend;
        private String dataDistribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceUsageVO {
        private Double totalCpuHours;
        private Long totalMemoryMb;
        private Double totalGpuHours;
        private String networkTraffic;
    }
}