package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

/**
 * 虚拟机分配DTO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmAssignmentDTO {

    /**
     * 虚拟机ID
     */
    @NotBlank(message = "虚拟机ID不能为空")
    private String vmId;

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 权限列表
     */
    @NotEmpty(message = "权限列表不能为空")
    private Set<String> permissions;

    /**
     * 分配备注
     */
    private String notes;
}