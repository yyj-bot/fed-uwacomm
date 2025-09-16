package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模型统计信息响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelStatisticsVO {

    /**
     * 模型总数
     */
    private Integer totalModels;

    /**
     * 平均准确率
     */
    private BigDecimal averageAccuracy;

    /**
     * 平均损失值
     */
    private BigDecimal averageLoss;

    /**
     * 上传趋势
     */
    private List<UploadTrendVO> uploadTrend;

    /**
     * 准确率趋势
     */
    private List<AccuracyTrendVO> accuracyTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadTrendVO {
        /**
         * 日期
         */
        private String date;

        /**
         * 数量
         */
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccuracyTrendVO {
        /**
         * 轮次
         */
        private Integer roundNumber;

        /**
         * 准确率
         */
        private BigDecimal accuracy;
    }
}