package com.feduwacomm.handler;

import com.feduwacomm.common.Result;
import com.feduwacomm.exception.ResourceNotFoundException;
import com.feduwacomm.exception.UserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
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
     * 处理HTTP方法不支持异常
     *
     * @param ex HTTP方法不支持异常
     * @return 错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<String> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        String message = String.format("HTTP方法 '%s' 不被支持，支持的方法: %s",
            ex.getMethod(), String.join(", ", ex.getSupportedMethods()));
        return Result.failure(405, "方法不被允许", message);
    }

    /**
     * 处理不支持的媒体类型异常
     *
     * @param ex 媒体类型不支持异常
     * @return 错误响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<String> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        String message = String.format("媒体类型 '%s' 不被支持，支持的类型: %s",
            ex.getContentType(), ex.getSupportedMediaTypes());
        return Result.failure(415, "不支持的媒体类型", message);
    }

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
    public ResponseEntity<Result<String>> handleRuntimeException(RuntimeException ex) {
        // 如果是UserException，跳过此处理器
        if (ex instanceof UserException) {
            throw ex; // 重新抛出，让UserException处理器处理
        }

        // 添加详细的异常日志
        System.err.println("=== 全局异常处理器捕获到RuntimeException ===");
        System.err.println("异常类型: " + ex.getClass().getName());
        System.err.println("异常消息: " + ex.getMessage());
        ex.printStackTrace();
        System.err.println("=== 异常处理结束 ===");

        Result<String> result = Result.failure(500, "服务器内部错误", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 处理认证异常
     *
     * @param ex 认证异常
     * @return 错误响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<String>> handleAuthenticationException(AuthenticationException ex) {
        Result<String> result = Result.failure(401, "认证失败", ex.getMessage());
        return ResponseEntity.status(401).body(result);
    }

    /**
     * 处理权限异常
     *
     * @param ex 权限异常
     * @return 错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<String>> handleAccessDeniedException(AccessDeniedException ex) {
        Result<String> result = Result.failure(403, "权限不足", ex.getMessage());
        return ResponseEntity.status(403).body(result);
    }

    /**
     * 处理用户相关异常
     *
     * @param ex 用户异常
     * @return 错误响应
     */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<Result<String>> handleUserException(UserException ex) {
        Result<String> result = Result.failure(ex.getCode(), ex.getMessage(), ex.getMessage());
        return ResponseEntity.status(ex.getCode()).body(result);
    }

    /**
     * 处理资源不存在异常
     *
     * @param ex 资源不存在异常
     * @return 错误响应
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return Result.failure(404, "资源不存在", ex.getMessage());
    }

    /**
     * 处理所有未捕获的异常
     *
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleAllExceptions(Exception ex) {
        // 记录异常日志
        System.err.println("全局异常处理器捕获异常: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        return Result.failure(500, "服务器内部错误", ex.getMessage());
    }

}