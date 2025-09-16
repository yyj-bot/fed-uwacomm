package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 模型回滚请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRollbackDTO {

    /**
     * 部署ID，必填
     */
    @NotBlank(message = "部署ID不能为空")
    private String deploymentId;

    /**
     * 目标模型ID，必填
     */
    @NotBlank(message = "目标模型ID不能为空")
    private String targetModelId;

    /**
     * 回滚原因，可选
     */
    private String rollbackReason;

    /**
     * 强制回滚，可选，默认false
     */
    private Boolean force = false;
}