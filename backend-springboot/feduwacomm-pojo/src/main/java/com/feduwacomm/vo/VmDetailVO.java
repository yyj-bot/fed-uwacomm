package com.feduwacomm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 虚拟机详情响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmDetailVO {

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 虚拟机名称
     */
    private String name;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * SSH端口
     */
    private Integer port;

    /**
     * 虚拟机状态：RUNNING, STOPPED, STARTING, STOPPING, ERROR, OFFLINE
     */
    private String status;

    /**
     * 操作系统类型
     */
    private String osType;

    /**
     * CPU核心数
     */
    private Integer cpuCores;

    /**
     * 内存大小(MB)
     */
    private Integer memoryMb;

    /**
     * 磁盘大小(GB)
     */
    private Integer diskGb;

    /**
     * 连接状态：CONNECTED, DISCONNECTED
     */
    private String connectionStatus;

    /**
     * 最后心跳时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHeartbeat;

    /**
     * WebSocket会话ID
     */
    private String wsSessionId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 系统信息
     */
    private Map<String, Object> systemInfo;

    /**
     * 虚拟机能力信息
     */
    private Map<String, Object> capabilities;

    /**
     * 网络配置信息
     */
    private Map<String, Object> networkConfig;

    /**
     * 虚拟机元数据
     */
    private Map<String, Object> metadata;

    /**
     * 详细资源使用情况
     */
    @Builder
    @Data
    public static class DetailedResourceUsage {
        /**
         * CPU使用率(%)
         */
        private Double cpu;

        /**
         * 内存使用率(%)
         */
        private Double memory;

        /**
         * 磁盘使用率(%)
         */
        private Double disk;

        /**
         * GPU使用率(%)
         */
        private Double gpu;

        /**
         * 网络上传速度(KB/s)
         */
        private Long uploadSpeed;

        /**
         * 网络下载速度(KB/s)
         */
        private Long downloadSpeed;

        /**
         * 网络延迟(ms)
         */
        private Integer latency;
    }

    /**
     * 详细资源使用情况
     */
    private DetailedResourceUsage resourceUsage;

    /**
     * 网络信息
     */
    @Builder
    @Data
    public static class NetworkInfo {
        /**
         * MAC地址
         */
        private String macAddress;

        /**
         * 带宽(Mbps)
         */
        private Integer bandwidth;

        /**
         * 上传速度(KB/s)
         */
        private Long uploadSpeed;

        /**
         * 下载速度(KB/s)
         */
        private Long downloadSpeed;

        /**
         * 延迟(ms)
         */
        private Integer latency;
    }

    /**
     * 网络信息
     */
    private NetworkInfo network;

    /**
     * 进程信息
     */
    @Builder
    @Data
    public static class ProcessInfo {
        /**
         * 总进程数
         */
        private Integer total;

        /**
         * 活跃进程数
         */
        private Integer active;

        /**
         * 系统进程数
         */
        private Integer system;

        /**
         * 用户进程数
         */
        private Integer user;
    }

    /**
     * 进程信息
     */
    private ProcessInfo processes;

    /**
     * 运行时长（秒）
     */
    private Long uptime;

    /**
     * 是否在线
     */
    private Boolean online;

    /**
     * 备注信息
     */
    private String notes;
}