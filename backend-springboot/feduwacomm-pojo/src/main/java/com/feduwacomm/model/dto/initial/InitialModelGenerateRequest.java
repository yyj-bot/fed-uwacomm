package com.feduwacomm.model.dto.initial;

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
 * 初始模型生成请求体
 * 对应 v1.5 `POST /api/model/initial/generate`
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitialModelGenerateRequest {

    /**
     * 模型类型：RANDOM_FOREST / NEURAL_NETWORK
     */
    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    /**
     * 模型架构参数
     */
    @NotNull(message = "模型架构参数不能为空")
    private Map<String, Object> architecture;

    /**
     * 随机种子，可选
     */
    private Integer randomSeed;

    /**
     * 模型描述，可选
     */
    @Size(max = 255, message = "模型描述长度不能超过255个字符")
    private String description;

    /**
     * 模型标签列表，可选
     */
    @Size(max = 20, message = "标签数量不能超过20个")
    private List<@Size(max = 32, message = "单个标签长度不能超过32个字符") String> labels;

    /**
     * 额外元数据，例如框架版本、作者等
     */
    private Map<String, Object> metadata;
}
