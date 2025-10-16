package com.feduwacomm.model.dto.initial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 自定义初始模型上传元数据请求体
 * 对应 v1.5 `POST /api/model/initial/upload`（multipart form 内的 JSON 字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitialModelUploadRequest {

    /**
     * 模型类型：RANDOM_FOREST / NEURAL_NETWORK
     */
    @NotBlank(message = "模型类型不能为空")
    private String modelType;

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
     * 模型相关元数据，例如架构、框架、版本信息
     */
    private Map<String, Object> metadata;

    /**
     * 文件校验和，用于上传后完整性校验
     */
    @Size(max = 128, message = "校验和长度不能超过128个字符")
    private String checksum;

    /**
     * 模型参数数量，可选
     */
    private Integer parametersCount;

    /**
     * 随机种子，可选（供自动生成模型回填提示使用）
     */
    private Integer randomSeed;
}
