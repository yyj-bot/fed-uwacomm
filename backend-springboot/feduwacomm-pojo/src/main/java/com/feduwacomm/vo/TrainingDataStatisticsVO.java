package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 训练数据统计响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataStatisticsVO {

    private Long totalCount;

    private Long totalSize;

    private Map<String, Integer> dataTypeDistribution;

    private Map<String, Integer> statusDistribution;

    private Map<String, VmStatistic> vmDistribution;

    private UploadTrend uploadTrend;

    private List<TopDataType> topDataTypes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VmStatistic {
        private Integer count;
        private Long size;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UploadTrend {
        private List<Integer> last7Days;
        private List<Integer> last30Days;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopDataType {
        private String dataType;
        private Integer count;
        private Double percentage;
    }
}