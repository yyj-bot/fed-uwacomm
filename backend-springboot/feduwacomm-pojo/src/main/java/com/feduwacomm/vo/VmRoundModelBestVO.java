package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 虚拟机轮次模型最佳/离群响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VmRoundModelBestVO {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 指标名称
     */
    private String metric;

    /**
     * 类型（best/outlier）
     */
    private String type;

    /**
     * 结果信息
     */
    private BestResult result;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BestResult {
        /**
         * 虚拟机轮次模型ID
         */
        private String vmRoundModelId;

        /**
         * 轮数
         */
        private Integer roundNumber;

        /**
         * 虚拟机ID
         */
        private String vmId;

        /**
         * 指标值
         */
        private BigDecimal value;
    }
}