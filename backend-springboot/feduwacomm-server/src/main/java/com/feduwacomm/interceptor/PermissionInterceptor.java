package com.feduwacomm.interceptor;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.exception.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 权限拦截器
 * 用于检查用户权限
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PermissionInterceptor.class);

    // 管理员角色
    private static final String ADMIN_ROLE = "ADMIN";

    // 需要管理员权限的路径
    private static final List<String> ADMIN_PATHS = Arrays.asList(
            "/api/user/list",
            "/api/user/create",
            "/api/user/delete",
            "/api/user/lock",
            "/api/user/unlock",
            "/api/user/reset-password",
            // 日志管理接口 - 仅限管理员访问
            "/api/log/list",
            "/api/log/detail",
            "/api/log/realtime",
            "/api/log/statistics",
            "/api/log/download",
            "/api/log/cleanup",
            "/api/log/monitor",
            "/api/log/config");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String requestURI = request.getRequestURI();
        String userRole = BaseContext.getUserRole();

        log.debug("权限拦截器检查 - 请求路径: {}, 用户角色: {}", requestURI, userRole);

        // 检查是否需要管理员权限
        if (isAdminRequiredPath(requestURI)) {
            if (!ADMIN_ROLE.equals(userRole)) {
                log.warn("权限不足 - 用户角色: {}, 需要角色: {}", userRole, ADMIN_ROLE);
                throw UserException.permissionDenied();
            }
            log.debug("管理员权限验证通过");
        }

        return true;
    }

    /**
     * 检查路径是否需要管理员权限
     */
    private boolean isAdminRequiredPath(String requestURI) {
        return ADMIN_PATHS.stream().anyMatch(requestURI::contains);
    }
}