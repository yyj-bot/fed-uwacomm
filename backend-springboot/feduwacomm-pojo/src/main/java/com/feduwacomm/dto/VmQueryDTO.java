package com.feduwacomm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

/**
 * 虚拟机查询请求DTO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmQueryDTO {

    /**
     * 页码，默认1
     */
    @Min(value = 1, message = "页码必须大于0")
    @Builder.Default
    private Integer page = 1;

    /**
     * 每页大小，默认20，最大100
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    @Builder.Default
    private Integer size = 20;

    /**
     * 状态过滤，可选值：RUNNING, STOPPED, STARTING, STOPPING, ERROR, OFFLINE
     */
    private String status;

    /**
     * 操作系统类型过滤
     */
    private String osType;

    /**
     * 关键词搜索（名称、IP地址）
     */
    private String keyword;

    /**
     * 连接状态过滤：CONNECTED, DISCONNECTED
     */
    private String connectionStatus;

    /**
     * 用户ID，用于权限控制
     */
    private String userId;

    /**
     * 排序字段，默认创建时间
     */
    @Builder.Default
    private String sortField = "created_at";

    /**
     * 排序方向，默认降序
     */
    @Builder.Default
    private String sortOrder = "desc";
}