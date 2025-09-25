package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 模型版本查询响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVersionResponseVO {

    private String modelId;
    private String taskId;
    private Integer round;
    private String version;
    private String modelType;
    private String description;
    private Double accuracy;
    private Double loss;
    private Long modelSize;
    private Integer parameterCount;
    private String framework;
    private String status;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
    private String vmId;
    private Map<String, Object> metadata;
    private String downloadUrl;
    private String checksum;
}