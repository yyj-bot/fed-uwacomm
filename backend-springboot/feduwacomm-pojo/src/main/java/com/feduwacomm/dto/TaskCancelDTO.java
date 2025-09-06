package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 联邦学习任务取消请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCancelDTO {

    @NotBlank(message = "取消原因不能为空")
    @Size(max = 500, message = "取消原因长度不能超过500字符")
    private String reason;
}