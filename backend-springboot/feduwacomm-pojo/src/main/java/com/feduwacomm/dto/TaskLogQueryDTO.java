package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDateTime;

/**
 * 联邦学习任务日志查询请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskLogQueryDTO {

    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能大于100")
    private Integer size = 10;

    private String level; // 日志级别过滤
    private LocalDateTime startTime; // 开始时间
    private LocalDateTime endTime; // 结束时间
    private String keyword; // 关键词搜索
    private String source; // 日志来源过滤
    private String vmId; // 虚拟机ID过滤
}