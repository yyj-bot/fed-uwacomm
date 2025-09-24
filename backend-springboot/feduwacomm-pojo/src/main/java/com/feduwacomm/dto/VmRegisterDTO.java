package com.feduwacomm.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

/**
 * 虚拟机注册请求DTO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
public class VmRegisterDTO {

    // vmId由后端自动生成，不需要前端传递

    @NotBlank(message = "虚拟机名称不能为空")
    @Size(max = 100, message = "虚拟机名称不能超过100个字符")
    private String name;

    @NotBlank(message = "IP地址不能为空")
    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", 
             message = "IP地址格式不正确")
    private String ipAddress;

    @NotNull(message = "端口不能为空")
    @Min(value = 1, message = "端口必须在1-65535范围内")
    @Max(value = 65535, message = "端口必须在1-65535范围内")
    private Integer port;

    @NotBlank(message = "操作系统类型不能为空")
    @Size(max = 50, message = "操作系统类型不能超过50个字符")
    private String osType;

    @NotNull(message = "CPU核心数不能为空")
    @Min(value = 1, message = "CPU核心数必须大于0")
    private Integer cpuCores;

    @NotNull(message = "内存大小不能为空")
    @Min(value = 1024, message = "内存必须≥1024MB")
    private Integer memoryMb;

    @NotNull(message = "磁盘大小不能为空")
    @Min(value = 20, message = "磁盘必须≥20GB")
    private Integer diskGb;

    /**
     * 系统信息（JSON格式）
     * 包括操作系统、内核版本、Python版本、GPU信息等
     */
    private Map<String, Object> systemInfo;

    /**
     * 虚拟机能力信息（JSON格式）
     * 包括支持的算法、最大批处理大小、GPU内存等
     */
    @NotNull(message = "能力信息不能为空")
    private Map<String, Object> capabilities;

    /**
     * 网络配置信息（JSON格式，可选）
     */
    private Map<String, Object> networkConfig;

    /**
     * 元数据信息（JSON格式，可选）
     */
    private Map<String, Object> metadata;
}