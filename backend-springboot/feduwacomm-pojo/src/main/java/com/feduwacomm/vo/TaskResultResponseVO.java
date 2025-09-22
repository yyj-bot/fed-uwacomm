package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务结果查询响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultResponseVO {

    private String taskId;
    private String taskName;
    private String status;
    private LocalDateTime completionTime;
    private String totalDuration;
    private FinalMetricsVO finalMetrics;
    private List<ParticipantResultVO> participantResults;
    private GlobalModelVO globalModel;
    private ConvergenceAnalysisVO convergenceAnalysis;
    private ResourceSummaryVO resourceSummary;
    private QualityMetricsVO qualityMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalMetricsVO {
        private Double finalAccuracy;
        private Double finalLoss;
        private Integer convergenceRound;
        private Boolean targetAccuracyReached;
        private Double improvementOverInitial;
        private Double convergenceStability;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantResultVO {
        private String vmId;
        private String role;
        private Double contribution;
        private Double finalLocalAccuracy;
        private Integer averageRoundTime;
        private String totalTrainingTime;
        private Integer dataContribution;
        private Integer modelUploads;
        private Integer aggregationParticipation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalModelVO {
        private String modelId;
        private String version;
        private Double accuracy;
        private Double loss;
        private Integer parameterCount;
        private Long modelSize;
        private String framework;
        private String downloadUrl;
        private String checksumSHA256;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConvergenceAnalysisVO {
        private String convergencePattern;
        private Integer plateauRounds;
        private Integer optimalStoppingRound;
        private Boolean overfittingDetected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceSummaryVO {
        private Double totalCpuHours;
        private String totalMemoryUsage;
        private Double totalGpuHours;
        private String totalNetworkTraffic;
        private Double energyEfficiency;
        private String costEstimate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityMetricsVO {
        private Double dataUtilization;
        private Double modelConsistency;
        private Double participantReliability;
        private Double communicationEfficiency;
    }
}