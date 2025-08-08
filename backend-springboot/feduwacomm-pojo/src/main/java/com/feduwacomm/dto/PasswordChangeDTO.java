package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 密码修改数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeDTO {

    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}