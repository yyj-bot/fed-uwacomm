package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户权限视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionVO {

    private String id;
    private String permissionId; // API文档要求的字段名，映射到id
    private String userId;
    private String resourceType;
    private String resourceId;
    private String permission;
    private String permissionName; // API文档要求的字段名，映射到permission
    private String description; // API文档要求的权限描述
    private LocalDateTime grantedAt;
    private String grantedBy;
    private LocalDateTime expiresAt;
}