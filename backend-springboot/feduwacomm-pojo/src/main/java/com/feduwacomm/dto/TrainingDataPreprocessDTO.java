package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * 训练数据预处理请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataPreprocessDTO {

    @NotEmpty(message = "预处理方法不能为空")
    private List<String> methods;

    private Map<String, Map<String, Object>> parameters;

    private String outputFormat = "csv";
}