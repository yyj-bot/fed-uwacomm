package com.feduwacomm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 虚拟机列表响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmListVO {

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
     * 资源使用率概览
     */
    @Builder
    @Data
    public static class ResourceUsage {
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
    }

    /**
     * 资源使用情况
     */
    private ResourceUsage resourceUsage;

    /**
     * 是否在线
     */
    private Boolean online;

    /**
     * 运行时长（秒）
     */
    private Long uptime;
}