package com.feduwacomm.constants;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 系统常量类
 *
 * 集中管理系统中使用的各种常量，避免硬编码
 *
 * @author FedUWAComm Team
 * @since 1.0.0
 */
public final class SystemConstants {

    private SystemConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 网络相关常量 ====================

    /**
     * 默认服务器端口
     */
    public static final String DEFAULT_SERVER_PORT = "8080";

    /**
     * 默认主机地址
     */
    public static final String DEFAULT_HOST = "localhost";

    /**
     * 本地回环地址
     */
    public static final String LOCALHOST_IP = "127.0.0.1";

    /**
     * HTTP协议前缀
     */
    public static final String HTTP_PROTOCOL = "http://";

    /**
     * HTTPS协议前缀
     */
    public static final String HTTPS_PROTOCOL = "https://";

    /**
     * WebSocket协议前缀
     */
    public static final String WS_PROTOCOL = "ws://";

    /**
     * 安全WebSocket协议前缀
     */
    public static final String WSS_PROTOCOL = "wss://";

    // ==================== API路径常量 ====================

    /**
     * API基础路径
     */
    public static final String API_BASE_PATH = "/api";

    /**
     * API版本1路径
     */
    public static final String API_V1_PATH = "/api/v1";

    /**
     * WebSocket端点路径 (统一使用原生WebSocket协议)
     */
    public static final String WEBSOCKET_ENDPOINT = "/ws";

    /**
     * 虚拟机API路径前缀
     */
    public static final String VM_API_PREFIX = "/api/v1/vm";

    /**
     * Token刷新路径
     */
    public static final String TOKEN_REFRESH_PATH = "/api/v1/vm/token/refresh";

    // ==================== 文件相关常量 ====================

    /**
     * 字符编码
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * 字符编码名称
     */
    public static final String DEFAULT_CHARSET_NAME = "UTF-8";

    /**
     * CSV文件Content-Type
     */
    public static final String CSV_CONTENT_TYPE = "text/csv; charset=" + DEFAULT_CHARSET_NAME;

    /**
     * JSON文件Content-Type
     */
    public static final String JSON_CONTENT_TYPE_WITH_CHARSET = "application/json; charset=" + DEFAULT_CHARSET_NAME;

    /**
     * Excel文件Content-Type
     */
    public static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=" + DEFAULT_CHARSET_NAME;

    /**
     * 默认临时目录
     */
    public static final String DEFAULT_TEMP_DIR = "/tmp";

    /**
     * 日志导出目录名
     */
    public static final String LOG_EXPORT_DIR = "log-exports";

    // ==================== 分页相关常量 ====================

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUMBER = 1;

    /**
     * 默认页面大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大页面大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    // ==================== 时间相关常量 ====================

    /**
     * 默认JWT过期时间（秒）- 24小时
     */
    public static final long DEFAULT_JWT_EXPIRE_SECONDS = 86400L;

    /**
     * 默认VM密钥过期天数
     */
    public static final int DEFAULT_VM_SECRET_EXPIRE_DAYS = 30;

    /**
     * 默认日志保留天数
     */
    public static final int DEFAULT_LOG_RETENTION_DAYS = 7;

    // ==================== 虚拟机相关常量 ====================

    /**
     * 默认MAC地址前缀
     */
    public static final String DEFAULT_MAC_PREFIX = "00:FD:UW";

    /**
     * 默认SSH端口
     */
    public static final int DEFAULT_SSH_PORT = 22;

    // ==================== 日期时间格式常量 ====================

    /**
     * ISO日期时间格式示例
     */
    public static final String ISO_DATETIME_EXAMPLE = "2024-01-01T00:00:00";

    /**
     * ISO日期时间格式
     */
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    // ==================== 错误消息模板 ====================

    /**
     * 时间格式错误消息模板
     */
    public static final String TIME_FORMAT_ERROR_TEMPLATE = "请使用ISO格式如: %s";

    /**
     * 文件大小限制错误消息模板
     */
    public static final String FILE_SIZE_LIMIT_ERROR_TEMPLATE = "文件过大，超过%dMB限制";

    // ==================== 响应消息常量 ====================

    /**
     * 成功响应消息
     */
    public static final String SUCCESS_MESSAGE = "操作成功";

    /**
     * 失败响应消息
     */
    public static final String FAILURE_MESSAGE = "操作失败";

    // ==================== HTTP头常量 ====================

    /**
     * Authorization头名称
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Bearer Token前缀
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Content-Type头名称
     */
    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    /**
     * JSON Content-Type
     */
    public static final String JSON_CONTENT_TYPE = "application/json";
}