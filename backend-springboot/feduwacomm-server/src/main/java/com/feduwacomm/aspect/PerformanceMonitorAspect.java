package com.feduwacomm.aspect;

import com.feduwacomm.service.PerformanceMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 性能监控切面
 * 自动记录方法执行时间和HTTP请求性能
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@Aspect
@Component
public class PerformanceMonitorAspect {

    @Autowired
    private PerformanceMonitorService performanceMonitorService;

    /**
     * 监控Controller方法执行
     */
    @Around("execution(* com.feduwacomm.controller..*(..))")
    public Object monitorControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        boolean success = true;
        Throwable exception = null;

        try {
            Object result = joinPoint.proceed();

            // 记录HTTP请求性能
            recordHttpRequestMetrics(startTime, methodName);

            return result;
        } catch (Throwable e) {
            success = false;
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // 记录方法执行性能
            performanceMonitorService.recordMethodExecution(methodName, executionTime, success);

            if (executionTime > 5000) { // 超过5秒的慢请求
                log.warn("检测到慢请求: method={}, time={}ms, success={}",
                        methodName, executionTime, success);
            }

            log.debug("方法执行完成: method={}, time={}ms, success={}",
                    methodName, executionTime, success);
        }
    }

    /**
     * 监控Service方法执行 (排除PerformanceMonitorServiceImpl以避免无限递归)
     */
    @Around("execution(* com.feduwacomm.service.impl..*(..)) && !execution(* com.feduwacomm.service.impl.PerformanceMonitorServiceImpl.*(..))")
    public Object monitorServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        boolean success = true;

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitorService.recordMethodExecution(methodName, executionTime, success);

            if (executionTime > 3000) { // 超过3秒的慢服务
                log.warn("检测到慢服务: method={}, time={}ms, success={}",
                        methodName, executionTime, success);
            }
        }
    }

    /**
     * 监控Mapper方法执行（数据库操作）
     */
    @Around("execution(* com.feduwacomm.mapper..*(..))")
    public Object monitorMapperMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        String operation = extractDatabaseOperation(methodName);
        String table = extractTableName(methodName);
        boolean success = true;

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitorService.recordDatabaseOperation(operation, table, executionTime, success);

            if (executionTime > 1000) { // 超过1秒的慢查询
                log.warn("检测到慢查询: method={}, operation={}, table={}, time={}ms, success={}",
                        methodName, operation, table, executionTime, success);
            }
        }
    }

    /**
     * 记录HTTP请求指标
     */
    private void recordHttpRequestMetrics(long startTime, String methodName) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String endpoint = request.getRequestURI();
                String method = request.getMethod();

                // 从响应状态推断（这里简化处理，实际应该从响应中获取）
                int statusCode = 200; // 默认成功状态码
                long responseTime = System.currentTimeMillis() - startTime;

                performanceMonitorService.recordHttpRequest(endpoint, method, statusCode, responseTime);

                log.debug("HTTP请求记录: {} {} - {}ms", method, endpoint, responseTime);
            }
        } catch (Exception e) {
            log.debug("记录HTTP请求指标失败: {}", e.getMessage());
        }
    }

    /**
     * 从方法名提取数据库操作类型
     */
    private String extractDatabaseOperation(String methodName) {
        String name = methodName.toLowerCase();

        if (name.contains("select") || name.contains("query") || name.contains("find") || name.contains("get")) {
            return "SELECT";
        } else if (name.contains("insert") || name.contains("add") || name.contains("create")) {
            return "INSERT";
        } else if (name.contains("update") || name.contains("modify") || name.contains("set")) {
            return "UPDATE";
        } else if (name.contains("delete") || name.contains("remove")) {
            return "DELETE";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * 从方法名提取表名
     */
    private String extractTableName(String methodName) {
        try {
            // 从Mapper类名中提取表名
            String className = methodName.substring(0, methodName.lastIndexOf('.'));
            String mapperName = className.substring(className.lastIndexOf('.') + 1);

            if (mapperName.endsWith("Mapper")) {
                String tableName = mapperName.substring(0, mapperName.length() - 6);
                // 转换驼峰命名为下划线命名
                return camelToSnake(tableName);
            }

            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 驼峰命名转下划线命名
     */
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}