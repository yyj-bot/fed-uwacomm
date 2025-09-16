package com.feduwacomm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 虚拟机状态查询响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmStatusVO {

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 虚拟机状态：RUNNING, STOPPED, STARTING, STOPPING, ERROR, OFFLINE
     */
    private String status;

    /**
     * 连接状态：CONNECTED, DISCONNECTED
     */
    private String connectionStatus;

    /**
     * 运行时长（秒）
     */
    private Long uptime;

    /**
     * 实时资源使用情况
     */
    @Builder
    @Data
    public static class RealTimeResourceUsage {
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
         * 温度（摄氏度）
         */
        private Double temperature;

        /**
         * 功耗（瓦特）
         */
        private Double power;
    }

    /**
     * 实时资源使用情况
     */
    private RealTimeResourceUsage resourceUsage;

    /**
     * 实时网络信息
     */
    @Builder
    @Data
    public static class RealTimeNetwork {
        /**
         * IP地址
         */
        private String ipAddress;

        /**
         * MAC地址
         */
        private String macAddress;

        /**
         * 端口号
         */
        private Integer port;

        /**
         * 当前上传速度(KB/s)
         */
        private Long uploadSpeed;

        /**
         * 当前下载速度(KB/s)
         */
        private Long downloadSpeed;

        /**
         * 网络延迟(ms)
         */
        private Integer latency;

        /**
         * 丢包率(%)
         */
        private Double packetLoss;
    }

    /**
     * 实时网络信息
     */
    private RealTimeNetwork network;

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

        /**
         * 僵尸进程数
         */
        private Integer zombie;
    }

    /**
     * 进程信息
     */
    private ProcessInfo processes;

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
     * 健康检查状态
     */
    @Builder
    @Data
    public static class HealthCheck {
        /**
         * 整体健康状态：HEALTHY, WARNING, CRITICAL, UNKNOWN
         */
        private String overall;

        /**
         * CPU健康状态
         */
        private String cpu;

        /**
         * 内存健康状态
         */
        private String memory;

        /**
         * 磁盘健康状态
         */
        private String disk;

        /**
         * 网络健康状态
         */
        private String network;

        /**
         * 服务健康状态
         */
        private String services;
    }

    /**
     * 健康检查结果
     */
    private HealthCheck healthCheck;

    /**
     * 错误信息（如果状态异常）
     */
    private String error;

    /**
     * 警告信息
     */
    private String[] warnings;

    /**
     * 状态检查时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;

    /**
     * 是否实时查询结果
     */
    @Builder.Default
    private Boolean realTime = true;

    /**
     * 数据刷新间隔（秒）
     */
    private Integer refreshInterval;
}