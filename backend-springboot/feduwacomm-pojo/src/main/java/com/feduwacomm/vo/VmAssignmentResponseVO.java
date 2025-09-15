package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 虚拟机分配响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmAssignmentResponseVO {

    /**
     * 分配记录ID
     */
    private String assignmentId;

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
     * 分配的权限列表
     */
    private Set<String> permissions;

    /**
     * 分配状态
     */
    private String status;

    /**
     * 分配时间
     */
    private LocalDateTime assignedAt;

    /**
     * 分配者ID
     */
    private String assignedBy;

    /**
     * 分配者用户名
     */
    private String assignedByUsername;

    /**
     * 分配备注
     */
    private String notes;
}