package com.feduwacomm.model.dto.federated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 联邦任务中的初始模型配置（v1.5）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitialModelConfigDTO {

    /**
     * 配置模式：CUSTOM（引用现有模型）或 AUTO（由后端自动生成）
     */
    @NotBlank(message = "初始模型模式不能为空")
    private String mode;

    /**
     * CUSTOM 模式下引用的模型ID
     */
    @Size(max = 32, message = "初始模型ID长度不能超过32个字符")
    private String initialModelId;

    /**
     * AUTO 模式下所需的生成配置
     */
    @Valid
    private AutoGenerateConfig autoGenerateConfig;

    /**
     * 自动生成配置定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AutoGenerateConfig {

        @NotBlank(message = "模型类型不能为空")
        private String modelType;

        @NotNull(message = "模型架构参数不能为空")
        private Map<String, Object> architecture;

        @Size(max = 255, message = "模型描述长度不能超过255个字符")
        private String description;

        private Integer randomSeed;

        @Size(max = 20, message = "标签数量不能超过20个")
        private List<@Size(max = 32, message = "单个标签长度不能超过32个字符") String> labels;

        private Map<String, Object> metadata;
    }
}
