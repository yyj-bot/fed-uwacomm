package com.feduwacomm.dto;

/**
 * 用户登录数据传输对象
 */
public class UserLoginDTO {

    private String account;
    private String password;
    private String captcha;
    private String captchaKey;
    private Boolean rememberMe;

    // 构造函数
    public UserLoginDTO() {
    }

    public UserLoginDTO(String account, String password) {
        this.account = account;
        this.password = password;
        this.rememberMe = false;
    }

    public UserLoginDTO(String account, String password, String captcha, String captchaKey) {
        this.account = account;
        this.password = password;
        this.captcha = captcha;
        this.captchaKey = captchaKey;
        this.rememberMe = false;
    }

    // Getter和Setter方法
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public String getCaptchaKey() {
        return captchaKey;
    }

    public void setCaptchaKey(String captchaKey) {
        this.captchaKey = captchaKey;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    @Override
    public String toString() {
        return "UserLoginDTO{" +
                "account='" + account + '\'' +
                ", captcha='" + captcha + '\'' +
                ", rememberMe=" + rememberMe +
                '}';
    }
}