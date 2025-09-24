package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;

/**
 * 模型版本查询参数DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelQueryDTO {

    /**
     * 任务ID过滤，32位UUID格式
     */
    private String taskId;

    /**
     * 训练轮数过滤
     */
    private Integer roundNumber;

    /**
     * 状态过滤
     */
    private String status;

    /**
     * 页码，默认1
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /**
     * 每页大小，默认10
     */
    @Min(value = 1, message = "每页大小必须大于0")
    private Integer size = 10;

    /**
     * 排序字段，默认created_at
     */
    private String sort = "created_at";

    /**
     * 排序方向，desc/asc
     */
    private String order = "desc";
}