package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户虚拟机列表VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVmListVO {

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
     * 内存大小(MB)
     */
    private Integer memoryMb;

    /**
     * 磁盘大小(GB)
     */
    private Integer diskGb;

    /**
     * 虚拟机状态
     */
    private String status;

    /**
     * 连接状态
     */
    private String connectionStatus;

    /**
     * 用户对此虚拟机的权限
     */
    private Set<String> permissions;

    /**
     * 分配时间
     */
    private LocalDateTime assignedAt;

    /**
     * 分配者用户名
     */
    private String assignedByUsername;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;

    /**
     * 分配备注
     */
    private String notes;
}