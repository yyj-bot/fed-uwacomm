package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 联邦学习任务统计信息响应VO
 * 提供任务的总体统计和状态分布信息
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 * @since 2025-09-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatisticsVO {

    /**
     * 任务总数
     */
    private Integer totalTasks;

    /**
     * 按状态分布的任务数量
     */
    private Map<String, Integer> statusDistribution;

    /**
     * 按算法分布的任务数量
     */
    private Map<String, Integer> algorithmDistribution;

    /**
     * 今日新增任务数
     */
    private Integer todayNewTasks;

    /**
     * 本周新增任务数
     */
    private Integer weekNewTasks;

    /**
     * 本月新增任务数
     */
    private Integer monthNewTasks;

    /**
     * 正在运行的任务数
     */
    private Integer runningTasks;

    /**
     * 已完成的任务数
     */
    private Integer completedTasks;

    /**
     * 失败的任务数
     */
    private Integer failedTasks;

    /**
     * 平均任务完成时间（秒）
     */
    private Double averageCompletionTime;

    /**
     * 任务成功率（百分比）
     */
    private Double successRate;

    /**
     * 最近7天任务创建趋势
     */
    private List<DailyTaskStats> recentTrend;

    /**
     * 统计生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 每日任务统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTaskStats {

        /**
         * 日期（YYYY-MM-DD）
         */
        private String date;

        /**
         * 当日任务总数
         */
        private Integer taskCount;

        /**
         * 当日完成任务数
         */
        private Integer completedCount;

        /**
         * 当日失败任务数
         */
        private Integer failedCount;
    }
}