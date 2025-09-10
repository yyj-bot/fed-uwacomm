package com.feduwacomm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 虚拟机控制操作响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmControlResponseVO {

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 操作类型：start, stop, restart
     */
    private String operation;

    /**
     * 预期状态
     */
    private String expectedStatus;

    /**
     * 命令ID，用于跟踪操作状态
     */
    private String commandId;

    /**
     * 预计完成时间（秒）
     */
    private Integer estimatedTime;

    /**
     * 操作开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    /**
     * 是否异步操作
     */
    @Builder.Default
    private Boolean async = true;

    /**
     * 操作状态：PENDING, RUNNING, COMPLETED, FAILED
     */
    @Builder.Default
    private String operationStatus = "PENDING";

    /**
     * 进度百分比（0-100）
     */
    @Builder.Default
    private Integer progress = 0;

    /**
     * 操作描述/备注
     */
    private String message;

    /**
     * 错误信息（如果操作失败）
     */
    private String error;

    /**
     * 操作详细信息
     */
    @Builder
    @Data
    public static class OperationDetails {
        /**
         * 是否强制执行
         */
        private Boolean force;

        /**
         * 超时时间（秒）
         */
        private Integer timeout;

        /**
         * 操作原因
         */
        private String reason;

        /**
         * 配置参数
         */
        private Object config;
    }

    /**
     * 操作详细信息
     */
    private OperationDetails details;

    /**
     * 下一步建议操作
     */
    private String nextAction;

    /**
     * 监控URL（用于查询操作进度）
     */
    private String monitorUrl;
}