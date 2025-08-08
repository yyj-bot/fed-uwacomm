package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginDTO {

    private String loginIdentifier; // 登录标识符（用户名或邮箱）
    private String password;
    private String captcha;
    private String captchaKey;
    private Boolean rememberMe;
}