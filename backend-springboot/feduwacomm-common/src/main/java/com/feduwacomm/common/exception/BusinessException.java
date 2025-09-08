package com.feduwacomm.common.exception;

/**
 * 业务异常
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    // 通用业务异常工厂方法
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    public static BusinessException of(int code, String message) {
        return new BusinessException(code, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }

    public static BusinessException internalServerError(String message) {
        return new BusinessException(500, message);
    }

    public static BusinessException internalServerError(String message, Throwable cause) {
        return new BusinessException(500, message, cause);
    }
}