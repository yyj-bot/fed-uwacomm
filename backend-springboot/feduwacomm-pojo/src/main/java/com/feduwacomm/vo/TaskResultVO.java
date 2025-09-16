package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 联邦学习任务结果响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultVO {

    private String taskId;
    private String taskName;
    private String status;
    
    private FinalResultsVO finalResults;
    private List<RoundResultVO> roundResults;
    private List<ParticipantResultVO> participantResults;
    private ModelInfoVO modelInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalResultsVO {
        private Double accuracy;
        private Double loss;
        private Double precision;
        private Double recall;
        private Double f1Score;
        private int[][] confusionMatrix; // 混淆矩阵
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundResultVO {
        private Integer round;
        private Double accuracy;
        private Double loss;
        private List<String> participants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantResultVO {
        private String vmId;
        private Double finalAccuracy;
        private Double finalLoss;
        private Long trainingTime; // 训练时间(秒)
        private Integer dataSize;
        private ParametersVO parameters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParametersVO {
        private ArtifactVO artifact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtifactVO {
        private String format; // pickle, json, etc.
        private String checksum; // sha256:...
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfoVO {
        private ParametersVO parameters;
        private MetaVO meta;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaVO {
        private String version;
        private String modelType;
    }
}