package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 虚拟机权限更新响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmPermissionUpdateResponseVO {

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 虚拟机名称
     */
    private String vmName;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 更新前的权限
     */
    private Set<String> oldPermissions;

    /**
     * 更新后的权限
     */
    private Set<String> newPermissions;

    /**
     * 更新状态
     */
    private String status;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 更新者ID
     */
    private String updatedBy;

    /**
     * 更新者用户名
     */
    private String updatedByUsername;

    /**
     * 原分配时间
     */
    private LocalDateTime originalAssignedAt;

    /**
     * 更新备注
     */
    private String notes;
}