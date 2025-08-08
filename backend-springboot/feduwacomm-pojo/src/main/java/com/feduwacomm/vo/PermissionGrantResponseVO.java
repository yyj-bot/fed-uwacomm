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
    private LocalDateTime grantedAt;
}