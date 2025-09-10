package com.feduwacomm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 虚拟机控制请求DTO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmControlDTO {

    /**
     * 控制操作类型：start, stop, restart
     */
    private String operation;

    /**
     * 是否强制执行，默认false
     */
    @Builder.Default
    private Boolean force = false;

    /**
     * 操作超时时间（秒），默认300秒
     */
    @Min(value = 10, message = "超时时间至少10秒")
    @Max(value = 3600, message = "超时时间不能超过3600秒")
    @Builder.Default
    private Integer timeout = 300;

    /**
     * 是否优雅停止（仅stop操作有效）
     */
    @Builder.Default
    private Boolean graceful = true;

    /**
     * 是否保存状态（仅stop操作有效）
     */
    @Builder.Default
    private Boolean saveState = true;

    /**
     * 启动配置（仅start和restart操作有效）
     */
    private Map<String, Object> config;

    /**
     * 环境变量配置
     */
    private Map<String, Object> environment;

    /**
     * 操作原因/备注
     */
    private String reason;

    /**
     * 预计操作时间（秒）
     */
    private Integer estimatedTime;
}