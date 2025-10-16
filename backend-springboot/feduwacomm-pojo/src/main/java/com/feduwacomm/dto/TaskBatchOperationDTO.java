package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务批量操作请求DTO
 * 支持对多个任务进行批量操作
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 * @since 2025-09-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskBatchOperationDTO {

    /**
     * 操作类型
     * START - 批量启动
     * STOP - 批量停止
     * PAUSE - 批量暂停
     * RESUME - 批量恢复
     * CANCEL - 批量取消
     * DELETE - 批量删除
     */
    private String operation;

    /**
     * 目标任务ID列表
     */
    private List<String> taskIds;

    /**
     * 操作原因（可选）
     */
    private String reason;

    /**
     * 操作参数（可选，JSON格式）
     */
    private String parameters;

    /**
     * 是否强制执行（忽略某些验证）
     */
    private Boolean force;

    /**
     * 是否异步执行
     */
    private Boolean async;
}