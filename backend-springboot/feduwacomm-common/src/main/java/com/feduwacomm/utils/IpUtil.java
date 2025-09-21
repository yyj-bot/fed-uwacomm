package com.feduwacomm.utils;

import com.feduwacomm.common.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP地址工具类
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class IpUtil {

    private static final Logger log = LoggerFactory.getLogger(IpUtil.class);

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String SEPARATOR = ",";
    private static final String SYSTEM_INTERNAL = "system";

    /**
     * 获取客户端真实IP地址
     */
    public static String getClientIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return UNKNOWN;
        }

        HttpServletRequest request = attributes.getRequest();
        return getClientIpAddress(request);
    }

    /**
     * 获取客户端真实IP地址
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (LOCALHOST.equals(ip)) {
            // 根据网卡取本机配置的IP
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                log.error("获取本机IP地址失败", e);
            }
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.length() > 15) {
            if (ip.indexOf(SEPARATOR) > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }

        log.debug("获取客户端IP地址: {}", ip);
        return ip;
    }

    /**
     * 检查IP地址是否为内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.length() == 0) {
            return false;
        }

        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);

            // 内网IP范围
            return first == 10 ||
                    (first == 172 && second >= 16 && second <= 31) ||
                    (first == 192 && second == 168);
        } catch (NumberFormatException e) {
            log.warn("IP地址格式错误: {}", ip);
            return false;
        }
    }

    /**
     * 获取本机IP地址
     */
    public static String getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("获取本机IP地址失败", e);
            return LOCALHOST;
        }
    }

    /**
     * 获取本机主机名
     */
    public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("获取本机主机名失败", e);
            return "unknown";
        }
    }

    /**
     * 获取当前上下文中的客户端IP，如果不存在则返回系统标识
     * 用于系统内部操作的日志记录
     */
    public static String getCurrentIpOrDefault() {
        String ip = BaseContext.getClientIp();
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }

        // 尝试从当前请求上下文获取IP
        try {
            String currentIp = getClientIpAddress();
            if (currentIp != null && !UNKNOWN.equals(currentIp)) {
                return currentIp;
            }
        } catch (Exception e) {
            log.debug("无法从请求上下文获取IP地址: {}", e.getMessage());
        }

        // 如果都获取不到，返回系统内部操作标识
        return SYSTEM_INTERNAL;
    }

    /**
     * 获取当前请求的主机名（用于WebSocket等场景）
     */
    public static String getCurrentHostOrDefault() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String serverName = request.getServerName();
            if (serverName != null && !serverName.isEmpty() && !"localhost".equals(serverName)) {
                return serverName;
            }
        }

        // 如果无法获取请求主机名，返回本机主机名
        return getLocalHostName();
    }
}