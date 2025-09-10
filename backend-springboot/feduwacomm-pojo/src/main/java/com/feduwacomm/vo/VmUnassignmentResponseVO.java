package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 虚拟机取消分配响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmUnassignmentResponseVO {

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
     * 取消分配状态
     */
    private String status;

    /**
     * 取消分配时间
     */
    private LocalDateTime unassignedAt;

    /**
     * 取消分配者ID
     */
    private String unassignedBy;

    /**
     * 取消分配者用户名
     */
    private String unassignedByUsername;

    /**
     * 原分配时间
     */
    private LocalDateTime originalAssignedAt;

    /**
     * 操作备注
     */
    private String notes;
}