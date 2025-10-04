package com.feduwacomm.handler;

import com.feduwacomm.common.Result;
import com.feduwacomm.exception.ResourceNotFoundException;
import com.feduwacomm.exception.UserException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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
    public ResponseEntity<Result<String>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        String message = String.format("HTTP方法 '%s' 不被支持，支持的方法: %s",
            ex.getMethod(), String.join(", ", ex.getSupportedMethods()));
        Result<String> result = Result.failure(405, "方法不被允许", message);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(result);
    }

    /**
     * 处理不支持的媒体类型异常
     *
     * @param ex 媒体类型不支持异常
     * @return 错误响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<String>> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        String message = String.format("媒体类型 '%s' 不被支持，支持的类型: %s",
            ex.getContentType(), ex.getSupportedMediaTypes());
        Result<String> result = Result.failure(415, "不支持的媒体类型", message);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(result);
    }

    /**
     * 处理验证异常
     *
     * @param ex 验证异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Result<Map<String, String>> result = Result.failure(400, "验证失败", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理缺少必需的multipart部分异常
     *
     * @param ex 缺少multipart部分异常
     * @return 错误响应
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Result<String>> handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
        String message = String.format("缺少必需的multipart部分: %s", ex.getRequestPartName());
        Result<String> result = Result.failure(400, "缺少必需参数", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理multipart解析异常
     *
     * @param ex multipart解析异常
     * @return 错误响应
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Result<String>> handleMultipartException(MultipartException ex) {
        String message = "multipart请求解析失败: " + ex.getMessage();

        // 记录详细异常日志用于调试
        System.err.println("=== MultipartException详细信息 ===");
        System.err.println("异常类型: " + ex.getClass().getName());
        System.err.println("异常消息: " + ex.getMessage());
        ex.printStackTrace();
        System.err.println("=== MultipartException处理结束 ===");

        Result<String> result = Result.failure(400, "请求格式错误", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
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
    public ResponseEntity<Result<String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Result<String> result = Result.failure(404, "资源不存在", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * 处理HTTP消息转换异常（关键修复：multipart响应序列化问题）
     *
     * @param ex HTTP消息转换异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Result<String>> handleHttpMessageNotWritableException(
            HttpMessageNotWritableException ex,
            HttpServletRequest request) {

        System.err.println("=== HttpMessageNotWritableException 捕获 ===");
        System.err.println("请求URI: " + request.getRequestURI());
        System.err.println("异常消息: " + ex.getMessage());
        System.err.println("Content-Type: " + request.getContentType());
        ex.printStackTrace();
        System.err.println("=== HttpMessageNotWritableException处理结束 ===");

        // 🔧 关键修复：对于multipart请求的响应序列化问题，返回明确的JSON响应
        Result<String> result = Result.failure(500, "响应序列化失败",
                "无法将响应对象序列化为JSON，可能是枚举类型或复杂对象序列化配置问题");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(result);
    }

    /**
     * 处理所有未捕获的异常
     *
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleAllExceptions(Exception ex) {
        // 记录异常日志
        System.err.println("=== 全局异常处理器捕获Exception ===");
        System.err.println("异常类型: " + ex.getClass().getName());
        System.err.println("异常消息: " + ex.getMessage());
        ex.printStackTrace();
        System.err.println("=== Exception处理结束 ===");

        Result<String> result = Result.failure(500, "服务器内部错误", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

}