package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 模型上传响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelUploadResponseVO {

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 训练轮数
     */
    private Integer roundNumber;

    /**
     * 状态
     */
    private String status;

    /**
     * 描述
     */
    private String description;

    /**
     * 参数
     */
    private Map<String, Object> parameters;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}