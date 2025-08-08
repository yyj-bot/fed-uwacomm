package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户列表项视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListVO {

    private String userId;
    private String username;
    private String account;
    private String email;
    private String role;
    private String status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer loginAttempts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}