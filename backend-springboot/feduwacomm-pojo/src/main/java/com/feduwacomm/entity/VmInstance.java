package com.feduwacomm.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 虚拟机实例实体类
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
public class VmInstance {

    /**
     * 虚拟机唯一标识（主键，32位UUID格式）
     */
    private String id;

    /**
     * 虚拟机名称
     */
    private String name;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 操作系统类型
     */
    private String osType;

    /**
     * CPU核心数
     */
    private Integer cpuCores;

    /**
     * 内存大小（MB）
     */
    private Integer memoryMb;

    /**
     * 磁盘大小（GB）
     */
    private Integer diskGb;

    /**
     * 虚拟机状态
     * OFFLINE, RUNNING, STOPPED, STARTING, STOPPING, ERROR
     */
    private String status;

    /**
     * WebSocket连接状态
     * DISCONNECTED, CONNECTED, CONNECTING, RECONNECTING
     */
    private String connectionStatus;

    /**
     * WebSocket会话ID
     */
    private String wsSessionId;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;


    /**
     * 长期刷新凭证
     */
    private String secretId;

    /**
     * 刷新凭证过期时间
     */
    private LocalDateTime secretExpireTime;

    /**
     * 系统信息（JSON字符串）
     */
    private String systemInfo;

    /**
     * 能力信息（JSON字符串）
     */
    private String capabilities;

    /**
     * 网络配置信息（JSON字符串）
     */
    private String networkConfig;

    /**
     * 元数据信息（JSON字符串）
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建者
     */
    private String createdBy;

    /**
     * 更新者
     */
    private String updatedBy;
}