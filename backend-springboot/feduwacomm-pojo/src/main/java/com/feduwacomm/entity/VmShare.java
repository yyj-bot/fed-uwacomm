package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 虚拟机共享实体类
 * 记录虚拟机的共享配置和权限
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmShare {

    /**
     * 共享记录ID
     */
    private String id;

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 被共享用户ID
     */
    private String sharedUserId;

    /**
     * 共享状态 (ACTIVE, INACTIVE, EXPIRED)
     */
    private String status;

    /**
     * 共享权限级别 (READ, WRITE, CONTROL)
     */
    private String permissionLevel;

    /**
     * 共享开始时间
     */
    private LocalDateTime startTime;

    /**
     * 共享结束时间
     */
    private LocalDateTime endTime;

    /**
     * 共享原因/备注
     */
    private String reason;

    /**
     * 共享者ID
     */
    private String sharedBy;

    /**
     * 是否允许再次共享
     */
    private Boolean allowReshare;

    /**
     * 访问次数限制
     */
    private Integer accessLimit;

    /**
     * 已访问次数
     */
    private Integer accessCount;

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