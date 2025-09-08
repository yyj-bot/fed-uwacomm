package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 批量模型上传请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelBatchUploadDTO {

    /**
     * 关联任务ID，必填
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /**
     * 模型列表，必填
     */
    @NotNull(message = "模型列表不能为空")
    @Size(min = 1, message = "至少需要上传一个模型")
    private List<ModelUploadDTO> models;
}