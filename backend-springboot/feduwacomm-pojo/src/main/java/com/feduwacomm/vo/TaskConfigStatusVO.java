package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务配置状态监控响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskConfigStatusVO {
    private String taskId;
    private String configStatus;
    private List<ConfigurationStepVO> configurationSteps;
    private List<ParticipantStatusVO> participantStatuses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigurationStepVO {
        private String step;
        private String status;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantStatusVO {
        private String vmId;
        private String configStatus;
        private Boolean dataDistributed;
        private Boolean modelInitialized;
    }
}