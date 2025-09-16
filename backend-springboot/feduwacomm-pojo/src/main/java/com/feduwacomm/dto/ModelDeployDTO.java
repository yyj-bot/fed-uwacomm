package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 模型部署请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDeployDTO {

    /**
     * 模型ID，必填，32位UUID格式
     */
    @NotBlank(message = "模型ID不能为空")
    private String modelId;

    /**
     * 部署名称，必填
     */
    @NotBlank(message = "部署名称不能为空")
    private String deploymentName;

    /**
     * 目标虚拟机列表，可选
     */
    private List<String> targetVms;

    /**
     * 部署配置，可选
     */
    private Map<String, Object> deploymentConfig;

    /**
     * 部署描述，可选
     */
    private String description;
}