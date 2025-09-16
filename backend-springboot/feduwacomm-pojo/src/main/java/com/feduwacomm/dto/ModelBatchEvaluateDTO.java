package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 批量模型评估请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelBatchEvaluateDTO {

    /**
     * 任务ID，必填，32位UUID格式
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /**
     * 测试数据路径，必填
     */
    @NotBlank(message = "测试数据路径不能为空")
    private String testDataPath;

    /**
     * 评估轮数，可选
     */
    private List<Integer> roundNumbers;

    /**
     * 评估指标，可选
     */
    private List<String> metrics;

    /**
     * 批次大小，可选
     */
    @Min(value = 1, message = "批次大小必须大于0")
    private Integer batchSize = 32;
}