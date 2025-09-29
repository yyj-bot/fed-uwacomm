package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目成员实体类
 * 记录项目和用户的成员关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMember {

    /**
     * 成员记录ID
     */
    private String id;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 成员状态 (ACTIVE, INACTIVE, PENDING)
     */
    private String status;

    /**
     * 成员角色 (LEADER, MEMBER, VIEWER)
     */
    private String role;

    /**
     * 权限列表 (JSON格式)
     */
    private String permissions;

    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;

    /**
     * 离开时间
     */
    private LocalDateTime leftAt;

    /**
     * 邀请者ID
     */
    private String invitedBy;

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