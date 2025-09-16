package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务模型版本响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelTaskVersionsVO {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 模型总数
     */
    private Integer totalModels;

    /**
     * 版本列表
     */
    private List<ModelVersionVO> versions;
}