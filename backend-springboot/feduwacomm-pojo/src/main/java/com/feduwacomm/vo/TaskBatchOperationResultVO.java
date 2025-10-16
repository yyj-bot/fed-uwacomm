package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务批量操作结果VO
 * 返回批量操作的执行结果和详细信息
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 * @since 2025-09-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskBatchOperationResultVO {

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 总任务数
     */
    private Integer totalTasks;

    /**
     * 成功操作的任务数
     */
    private Integer successCount;

    /**
     * 失败操作的任务数
     */
    private Integer failureCount;

    /**
     * 跳过操作的任务数
     */
    private Integer skippedCount;

    /**
     * 操作执行时间
     */
    private LocalDateTime executedAt;

    /**
     * 操作耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 是否异步执行
     */
    private Boolean isAsync;

    /**
     * 异步任务ID（如果是异步执行）
     */
    private String asyncTaskId;

    /**
     * 详细操作结果
     */
    private List<TaskOperationDetail> details;

    /**
     * 整体操作结果摘要
     */
    private String summary;

    /**
     * 单个任务操作详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskOperationDetail {

        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 任务名称
         */
        private String taskName;

        /**
         * 操作前状态
         */
        private String previousStatus;

        /**
         * 操作后状态
         */
        private String currentStatus;

        /**
         * 操作结果
         * SUCCESS - 成功
         * FAILURE - 失败
         * SKIPPED - 跳过
         */
        private String result;

        /**
         * 操作消息（成功/失败/跳过的原因）
         */
        private String message;

        /**
         * 错误代码（如果失败）
         */
        private String errorCode;

        /**
         * 操作时间
         */
        private LocalDateTime operationTime;
    }
}