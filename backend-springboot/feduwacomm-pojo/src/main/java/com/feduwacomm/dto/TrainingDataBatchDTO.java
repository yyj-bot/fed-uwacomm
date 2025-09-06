package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * 训练数据批量操作请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataBatchDTO {

    @NotBlank(message = "操作类型不能为空")
    private String operation;

    @NotEmpty(message = "数据集ID列表不能为空")
    private List<String> datasetIds;

    private Map<String, Object> parameters;
}