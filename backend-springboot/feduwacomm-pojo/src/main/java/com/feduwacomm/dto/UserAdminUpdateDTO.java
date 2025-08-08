package com.feduwacomm.dto;

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

    private String username;
    private String email;
    private String role;
    private String status;
    private String password; // 新密码，可选，管理员可直接修改用户密码
}