package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 虚拟机分配实体类
 * 记录用户和虚拟机的分配关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmAssignment {

    /**
     * 分配记录ID
     */
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 分配状态 (ACTIVE, INACTIVE, EXPIRED)
     */
    private String status;

    /**
     * 分配权限级别 (READ, WRITE, ADMIN)
     */
    private String permissionLevel;

    /**
     * 分配开始时间
     */
    private LocalDateTime startTime;

    /**
     * 分配结束时间
     */
    private LocalDateTime endTime;

    /**
     * 分配原因/备注
     */
    private String reason;

    /**
     * 分配者ID
     */
    private String assignedBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建者
     */
    private String createdBy;

    /**
     * 更新者
     */
    private String updatedBy;
}