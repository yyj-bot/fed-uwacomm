package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 训练数据文件上传请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataUploadDTO {

    @NotBlank(message = "数据类型不能为空")
    private String dataType;

    private String vmId;

    private String datasetDescription;

    private List<String> tags;

    private Map<String, Object> metadata;
}