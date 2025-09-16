package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Set;

/**
 * 虚拟机批量分配DTO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmBatchAssignmentDTO {

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 虚拟机ID列表
     */
    @NotEmpty(message = "虚拟机ID列表不能为空")
    private List<String> vmIds;

    /**
     * 统一权限列表（可选，如果不提供则使用默认权限READ）
     */
    private Set<String> permissions;

    /**
     * 批量分配备注
     */
    private String notes;
}