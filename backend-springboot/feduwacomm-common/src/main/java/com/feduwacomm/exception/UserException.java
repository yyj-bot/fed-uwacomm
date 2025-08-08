package com.feduwacomm.exception;

/**
 * 用户相关异常
 */
public class UserException extends RuntimeException {

    private final int code;

    public UserException(String message) {
        super(message);
        this.code = 400;
    }

    public UserException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    // 预定义异常
    public static UserException userNotFound() {
        return new UserException(404, "用户不存在");
    }

    public static UserException userAlreadyExists() {
        return new UserException(409, "用户已存在");
    }

    public static UserException emailExists() {
        return new UserException(409, "邮箱已存在");
    }

    public static UserException usernameExists() {
        return new UserException(409, "用户名已存在");
    }

    public static UserException passwordError() {
        return new UserException(401, "密码错误");
    }

    public static UserException accountLocked() {
        return new UserException(423, "账号已锁定");
    }

    public static UserException statusInvalid() {
        return new UserException(400, "用户状态无效");
    }

    public static UserException roleInvalid() {
        return new UserException(400, "用户角色无效");
    }

    public static UserException permissionDenied() {
        return new UserException(403, "权限不足");
    }

    public static UserException tokenExpired() {
        return new UserException(401, "Token已过期");
    }

    public static UserException tokenInvalid() {
        return new UserException(401, "Token无效");
    }

    public static UserException tokenMissing() {
        return new UserException(401, "Token缺失");
    }

    public static UserException refreshTokenExpired() {
        return new UserException(401, "刷新Token已过期");
    }

    public static UserException refreshTokenInvalid() {
        return new UserException(401, "刷新Token无效");
    }

    public static UserException paramValidationError(String field, String error) {
        return new UserException(400, field + ": " + error);
    }

    public static UserException passwordTooShort() {
        return new UserException(400, "密码长度不足");
    }

    public static UserException passwordTooLong() {
        return new UserException(400, "密码长度过长");
    }

    public static UserException passwordMismatch() {
        return new UserException(400, "密码不匹配");
    }

    public static UserException passwordModificationDenied() {
        return new UserException(403, "只有管理员或用户本人可以修改密码");
    }

    public static UserException emailFormatError() {
        return new UserException(400, "邮箱格式错误");
    }

    public static UserException usernameFormatError() {
        return new UserException(400, "用户名格式错误");
    }
}