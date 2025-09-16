package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 训练数据文本信息上传请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataTextDTO {

    @NotBlank(message = "虚拟机ID不能为空")
    private String vmId;

    @NotBlank(message = "数据类型不能为空")
    private String dataType;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String datasetDescription;

    private List<String> tags;

    private Map<String, Object> metadata;
}