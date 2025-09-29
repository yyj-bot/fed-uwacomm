package com.feduwacomm.config;

import com.feduwacomm.common.Result;
import com.feduwacomm.exception.UserException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 拦截器异常处理器
 * 专门处理拦截器中抛出的异常，确保正确设置HTTP状态码
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class InterceptorExceptionResolver implements HandlerExceptionResolver {

    private static final Logger log = LoggerFactory.getLogger(InterceptorExceptionResolver.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Exception ex) {

        log.debug("=== InterceptorExceptionResolver 处理异常 ===");
        log.debug("异常类型: {}", ex.getClass().getName());
        log.debug("异常消息: {}", ex.getMessage());
        log.debug("请求URI: {}", request.getRequestURI());

        // 只处理UserException
        if (ex instanceof UserException) {
            UserException userEx = (UserException) ex;

            log.info("处理UserException - 状态码: {}, 消息: {}", userEx.getCode(), userEx.getMessage());

            try {
                // 设置响应状态码和内容类型
                response.setStatus(userEx.getCode());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());

                // 创建统一的响应格式
                Result<String> result = Result.failure(userEx.getCode(), userEx.getMessage(), userEx.getMessage());

                // 写入响应体
                String jsonResponse = objectMapper.writeValueAsString(result);
                response.getWriter().write(jsonResponse);

                log.debug("响应已写入 - 状态码: {}, 响应体: {}", userEx.getCode(), jsonResponse);

                // 返回空的ModelAndView表示异常已处理
                return new ModelAndView();

            } catch (IOException ioEx) {
                log.error("写入响应时发生错误", ioEx);
                return null; // 让其他异常处理器处理
            }
        }

        log.debug("=== 非UserException，不处理 ===");
        // 返回null让其他异常处理器处理
        return null;
    }
}