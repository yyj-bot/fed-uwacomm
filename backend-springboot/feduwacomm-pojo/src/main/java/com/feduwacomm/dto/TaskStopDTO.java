package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Size;

/**
 * 联邦学习任务停止请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStopDTO {

    @Size(max = 500, message = "停止原因长度不能超过500字符")
    private String reason;

    @Builder.Default
    private Boolean saveCheckpoint = true;
}