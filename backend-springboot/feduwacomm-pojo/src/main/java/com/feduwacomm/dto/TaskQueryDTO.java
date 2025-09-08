package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDateTime;

/**
 * 联邦学习任务查询请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskQueryDTO {

    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能大于100")
    private Integer size = 20;

    private String status; // 任务状态过滤
    private String type; // 任务类型过滤
    private LocalDateTime startDate; // 开始日期
    private LocalDateTime endDate; // 结束日期
    private String keyword; // 关键词搜索
    private String createdBy; // 创建者过滤
    private String algorithm; // 算法类型过滤
}