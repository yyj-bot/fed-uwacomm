package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.Map;

/**
 * 模型上传请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelUploadDTO {

    /**
     * 关联任务ID，必填
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /**
     * 训练轮数，必填
     */
    @NotNull(message = "训练轮数不能为空")
    @Min(value = 1, message = "训练轮数必须大于0")
    private Integer roundNumber;

    /**
     * 模型描述，可选
     */
    private String description;

    /**
     * 模型参数，可选，JSON格式的额外参数
     */
    private Map<String, Object> parameters;

    /**
     * 模型文件，必填
     */
    @NotNull(message = "模型文件不能为空")
    private MultipartFile file;
}