package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * v1.3 图形化配置预览响应VO
 */
@Data
@Builder
public class ConfigPreviewVO {

    // 可用虚拟机列表响应
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableVmsVO {
        private Integer total;
        private List<VmInfoVO> availableVms;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class VmInfoVO {
            private String vmId;
            private String name;
            private String ipAddress;
            private String status;
            private String connectionStatus;
            private String osType;
            private ResourcesVO resources;
            private List<String> capabilities;
            private List<String> supportedAlgorithms;
            private UsageVO currentUsage;
            private NetworkInfoVO networkInfo;
            private String lastHeartbeat;
            private ReliabilityVO reliability;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class ResourcesVO {
                private Integer cpuCores;
                private Integer memoryMb;
                private Integer diskGb;
                private Integer gpuCount;
                private Integer gpuMemoryMb;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class UsageVO {
                private Double cpuUsage;
                private Double memoryUsage;
                private Double networkUsage;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class NetworkInfoVO {
                private Integer bandwidth;
                private Integer latency;
                private Integer uploadSpeed;
                private Integer downloadSpeed;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class ReliabilityVO {
                private Double uptime;
                private Integer avgResponseTime;
                private Double taskSuccessRate;
            }
        }
    }

    // 可用数据集列表响应
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableDatasetsVO {
        private Integer total;
        private List<DatasetInfoVO> availableDatasets;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DatasetInfoVO {
            private String datasetId;
            private String name;
            private String description;
            private String dataType;
            private String status;
            private StatisticsVO statistics;
            private FeaturesVO features;
            private QualityVO quality;
            private MetadataVO metadata;
            private List<String> tags;
            private String uploadTime;
            private String uploadedBy;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class StatisticsVO {
                private Integer totalRows;
                private Integer totalColumns;
                private Long fileSize;
                private String fileSizeFormatted;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class FeaturesVO {
                private List<String> featureColumns;
                private String targetColumn;
                private Integer numericFeatures;
                private Integer categoricalFeatures;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class QualityVO {
                private Double completeness;
                private Double consistency;
                private Double accuracy;
                private Integer missingValues;
                private Integer duplicates;
                private Integer outliers;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class MetadataVO {
                private String source;
                private String version;
                private Integer sampleRate;
                private String frequency;
                private String environment;
            }
        }
    }

    // 数据分配预览响应
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionPreviewVO {
        private DistributionResultVO distributionResult;
        private QualityMetricsVO qualityMetrics;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DistributionResultVO {
            private List<ParticipantAllocationVO> participants;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class ParticipantAllocationVO {
                private String vmId;
                private String vmName;
                private Double allocatedRatio;
                private Integer allocatedRows;
                private Integer estimatedTrainingTime;
            }
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class QualityMetricsVO {
            private Double iidScore;
            private Double balanceScore;
        }
    }

    // 参与者验证响应
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantValidationVO {
        private Boolean overallValid;
        private List<ValidationResultVO> participantValidations;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ValidationResultVO {
            private String vmId;
            private Boolean isValid;
            private java.util.Map<String, ValidationItemVO> validationResults;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class ValidationItemVO {
                private String status;
                private String message;
            }
        }
    }

    // 算法模板响应
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlgorithmTemplatesVO {
        private List<AlgorithmTemplateVO> templates;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AlgorithmTemplateVO {
            private String algorithm;
            private String name;
            private String description;
            private List<String> applicableTaskTypes;
            private java.util.Map<String, Object> defaultHyperparameters;
            private java.util.Map<String, ParameterRangeVO> parameterRanges;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class ParameterRangeVO {
                private Double min;
                private Double max;
                private List<Object> recommended;
            }
        }
    }

    // 角色配置响应
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleConfigVO {
        private List<RoleInfoVO> roles;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RoleInfoVO {
            private String role;
            private String name;
            private String description;
            private RequirementsVO requirements;
            private List<String> compatibleAlgorithms;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class RequirementsVO {
                private Integer minCpuCores;
                private Integer minMemoryMb;
                private List<String> requiredCapabilities;
            }
        }
    }
}