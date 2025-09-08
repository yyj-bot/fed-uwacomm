package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模型部署响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDeployResponseVO {

    /**
     * 部署ID
     */
    private String deploymentId;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 部署名称
     */
    private String deploymentName;

    /**
     * 目标虚拟机列表
     */
    private List<String> targetVms;

    /**
     * 状态
     */
    private String status;

    /**
     * 部署配置
     */
    private Map<String, Object> deploymentConfig;

    /**
     * 访问端点列表
     */
    private List<String> endpoints;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}