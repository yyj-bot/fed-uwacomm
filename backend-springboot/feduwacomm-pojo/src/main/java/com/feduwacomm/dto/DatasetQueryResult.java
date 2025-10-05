package com.feduwacomm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * v1.5数据集查询结果DTO
 * 用于WebSocket协议中数据集状态查询的响应
 */
@Data
@Builder
public class DatasetQueryResult {

    /**
     * 查询是否成功
     */
    private boolean success;

    /**
     * 数据集状态
     */
    private String datasetStatus;

    /**
     * 本地路径
     */
    private String localPath;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 查询时间
     */
    private LocalDateTime queriedAt;
}