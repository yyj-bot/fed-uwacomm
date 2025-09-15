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

    public static UserException permissionAlreadyGranted() {
        return new UserException(409, "权限已授权");
    }

    public static UserException permissionNotFound() {
        return new UserException(404, "权限不存在");
    }

    public static UserException permissionNotOwned() {
        return new UserException(403, "权限不属于该用户");
    }

    /**
     * 密码格式错误（通用）
     */
    public static UserException passwordFormatError(String message) {
        return new UserException(400, message);
    }

    /**
     * 密码缺少字母
     */
    public static UserException passwordMissingLetter() {
        return new UserException(400, "密码格式不正确");
    }

    /**
     * 密码缺少数字
     */
    public static UserException passwordMissingDigit() {
        return new UserException(400, "密码格式不正确");
    }

    /**
     * 密码长度太短
     */
    public static UserException passwordTooShort(int minLength) {
        return new UserException(400, "密码格式不正确");
    }

    /**
     * 密码长度太长
     */
    public static UserException passwordTooLong(int maxLength) {
        return new UserException(400, "密码格式不正确");
    }

    /**
     * 密码缺少特殊字符
     */
    public static UserException passwordMissingSpecialChar() {
        return new UserException(400, "密码格式不正确");
    }

    /**
     * 密码缺少大写字母
     */
    public static UserException passwordMissingUppercase() {
        return new UserException(400, "密码格式不正确");
    }

    /**
     * 密码缺少小写字母
     */
    public static UserException passwordMissingLowercase() {
        return new UserException(400, "密码格式不正确");
    }

    /**
     * 密码包含不允许的字符
     */
    public static UserException passwordInvalidCharacters() {
        return new UserException(400, "密码格式不正确");
    }

    /**
     * 密码强度不够
     */
    public static UserException passwordTooWeak() {
        return new UserException(400, "密码格式不正确");
    }

    /**
     * 密码为空
     */
    public static UserException passwordEmpty() {
        return new UserException(400, "密码不能为空");
    }
}
