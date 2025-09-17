package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务资源使用监控响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResourceUsageVO {
    private String taskId;
    private List<ParticipantMetricVO> participantMetrics;
    private AggregatedMetricVO aggregatedMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantMetricVO {
        private String vmId;
        private CurrentUsageVO currentUsage;
        private AverageUsageVO averageUsage;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CurrentUsageVO {
            private Double cpu;
            private Double memory;
            private NetworkUsageVO network;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class NetworkUsageVO {
                private Double inbound;
                private Double outbound;
            }
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AverageUsageVO {
            private Double cpu;
            private Double memory;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregatedMetricVO {
        private Double totalCpuUsage;
        private Double totalMemoryUsage;
        private Double taskProgress;
    }
}