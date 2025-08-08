package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 权限授予数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGrantDTO {

    private String resourceType;
    private String resourceId;
    private String permission;
    private LocalDateTime expiresAt;
}