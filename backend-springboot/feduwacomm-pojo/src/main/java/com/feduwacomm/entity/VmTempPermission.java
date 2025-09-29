package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 虚拟机临时权限实体类
 * 记录用户对虚拟机的临时访问权限
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmTempPermission {

    /**
     * 临时权限记录ID
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
     * 权限状态 (ACTIVE, INACTIVE, EXPIRED, REVOKED)
     */
    private String status;

    /**
     * 权限级别 (READ, WRITE, CONTROL)
     */
    private String permissionLevel;

    /**
     * 权限开始时间（时间戳）
     */
    private Long startTime;

    /**
     * 权限结束时间（时间戳）
     */
    private Long endTime;

    /**
     * 权限授予原因
     */
    private String reason;

    /**
     * 授权者ID
     */
    private String grantedBy;

    /**
     * 是否一次性权限
     */
    private Boolean oneTimeUse;

    /**
     * 已使用标记
     */
    private Boolean used;

    /**
     * 访问限制条件 (JSON格式)
     */
    private String accessConditions;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;

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