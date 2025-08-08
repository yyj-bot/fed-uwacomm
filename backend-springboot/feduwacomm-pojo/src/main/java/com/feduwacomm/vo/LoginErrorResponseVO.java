package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录错误响应视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginErrorResponseVO {

    private Integer loginAttempts;
    private LocalDateTime lockedUntil;
}