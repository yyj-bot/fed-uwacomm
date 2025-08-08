package com.feduwacomm.dto;

/**
 * 用户注册数据传输对象
 */
public class UserRegisterDTO {

    private String username;
    private String account;
    private String email;
    private String password;
    private String confirmPassword;

    // 构造函数
    public UserRegisterDTO() {
    }

    public UserRegisterDTO(String username, String account, String email, String password, String confirmPassword) {
        this.username = username;
        this.account = account;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    // Getter和Setter方法
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @Override
    public String toString() {
        return "UserRegisterDTO{" +
                "username='" + username + '\'' +
                ", account='" + account + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}