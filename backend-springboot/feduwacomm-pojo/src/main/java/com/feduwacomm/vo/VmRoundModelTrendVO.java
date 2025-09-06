package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 虚拟机轮次模型指标趋势响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VmRoundModelTrendVO {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 指标名称
     */
    private String metric;

    /**
     * 趋势数据
     */
    private List<TrendPoint> trend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        /**
         * 轮次
         */
        private Integer roundNumber;

        /**
         * 指标值
         */
        private BigDecimal value;
    }
}