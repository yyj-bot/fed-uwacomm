package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 权限授予响应视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGrantResponseVO {

    private String permissionId;
    private String userId;
    private String resourceType;
    private String resourceId;
    private String permission;
    private String grantedBy;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
}