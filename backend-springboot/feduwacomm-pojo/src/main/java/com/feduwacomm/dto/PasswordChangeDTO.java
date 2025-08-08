package com.feduwacomm.dto;

/**
 * 密码修改数据传输对象
 */
public class PasswordChangeDTO {

    private String userId;
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;

    // 构造函数
    public PasswordChangeDTO() {
    }

    public PasswordChangeDTO(String userId, String oldPassword, String newPassword, String confirmPassword) {
        this.userId = userId;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    // Getter和Setter方法
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @Override
    public String toString() {
        return "PasswordChangeDTO{" +
                "userId='" + userId + '\'' +
                '}';
    }
}