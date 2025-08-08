package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户权限数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionDTO {

    private String permissionId;
    private String resourceType;
    private String resourceId;
    private String permission;
    private LocalDateTime grantedAt;
    private String grantedBy;
    private LocalDateTime expiresAt;
}