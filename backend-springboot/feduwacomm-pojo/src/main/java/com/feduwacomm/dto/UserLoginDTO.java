package com.feduwacomm.dto;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "登录标识符不能为空")
    private String loginIdentifier; // 登录标识符（用户名或邮箱）

    @NotBlank(message = "密码不能为空")
    private String password;
}