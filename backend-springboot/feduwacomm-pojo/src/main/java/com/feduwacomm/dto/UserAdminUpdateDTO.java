package com.feduwacomm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员用户更新数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminUpdateDTO {

    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String role;
    private String status;

    @Size(min = 6, max = 50, message = "密码长度必须在6-50个字符之间")
    private String password; // 新密码，可选，管理员可直接修改用户密码
}