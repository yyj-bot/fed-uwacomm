package com.feduwacomm.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 虚拟机更新请求DTO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmUpdateDTO {

    /**
     * 虚拟机名称
     */
    @Size(max = 100, message = "虚拟机名称不能超过100个字符")
    private String name;

    /**
     * IP地址
     */
    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
            message = "IP地址格式不正确")
    private String ipAddress;

    /**
     * SSH端口
     */
    @Min(value = 1, message = "端口号必须在1-65535范围内")
    @Max(value = 65535, message = "端口号必须在1-65535范围内")
    private Integer port;

    /**
     * 操作系统类型
     */
    private String osType;

    /**
     * CPU核心数
     */
    @Min(value = 1, message = "CPU核心数必须大于0")
    private Integer cpuCores;

    /**
     * 内存大小(MB)
     */
    @Min(value = 1024, message = "内存大小必须至少1024MB")
    private Integer memoryMb;

    /**
     * 磁盘大小(GB)
     */
    @Min(value = 20, message = "磁盘大小必须至少20GB")
    private Integer diskGb;

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
     * 备注信息
     */
    @Size(max = 500, message = "备注信息不能超过500个字符")
    private String notes;
}