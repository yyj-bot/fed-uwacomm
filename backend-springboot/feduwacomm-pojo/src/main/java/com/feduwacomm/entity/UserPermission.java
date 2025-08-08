package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户权限实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermission {

    private String id;
    private String userId;
    private String resourceType;
    private String resourceId;
    private String permission;
    private LocalDateTime grantedAt;
    private String grantedBy;
    private LocalDateTime expiresAt;
}