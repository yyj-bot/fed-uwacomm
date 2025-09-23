package com.feduwacomm.handler;

import com.feduwacomm.common.Result;
import com.feduwacomm.exception.UserException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * 统一处理应用程序中的各种异常，确保返回格式一致的错误响应
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理验证异常
     * 
     * @param ex 验证异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return Result.failure(400, "验证失败", errors);
    }

    /**
     * 处理运行时异常
     *
     * @param ex 运行时异常
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException ex) {
        // 添加详细的异常日志
        System.err.println("=== 全局异常处理器捕获到RuntimeException ===");
        System.err.println("异常类型: " + ex.getClass().getName());
        System.err.println("异常消息: " + ex.getMessage());
        ex.printStackTrace();
        System.err.println("=== 异常处理结束 ===");
        return Result.failure(500, "服务器内部错误", ex.getMessage());
    }

    /**
     * 处理认证异常
     * 
     * @param ex 认证异常
     * @return 错误响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<String> handleAuthenticationException(AuthenticationException ex) {
        return Result.failure(401, "认证失败", ex.getMessage());
    }

    /**
     * 处理权限异常
     * 
     * @param ex 权限异常
     * @return 错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<String> handleAccessDeniedException(AccessDeniedException ex) {
        return Result.failure(403, "权限不足", ex.getMessage());
    }

    /**
     * 处理用户相关异常
     * 
     * @param ex 用户异常
     * @return 错误响应
     */
    @ExceptionHandler(UserException.class)
    public Result<String> handleUserException(UserException ex) {
        return Result.failure(ex.getCode(), ex.getMessage(), ex.getMessage());
    }

    /**
     * 处理通用异常
     * 
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleGenericException(Exception ex) {
        return Result.failure(500, "未知错误", ex.getMessage());
    }
}