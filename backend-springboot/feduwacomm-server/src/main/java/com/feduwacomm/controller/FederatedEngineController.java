package com.feduwacomm.controller;

import com.feduwacomm.aggregation.AggregationStrategyFactory;
import com.feduwacomm.aggregation.UniversalAggregationEngine;
import com.feduwacomm.common.BaseContext;
import com.feduwacomm.common.Result;
import com.feduwacomm.enums.FederatedAlgorithm;
import com.feduwacomm.service.FederatedTaskService;
import com.feduwacomm.service.LogService;
import com.feduwacomm.utils.IpUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import com.sun.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 联邦学习聚合引擎监控控制器
 * 提供聚合引擎状态查询和性能监控功能
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 */
@Slf4j
@RestController
@RequestMapping("/api/federated/engine")
public class FederatedEngineController {

    @Autowired
    private UniversalAggregationEngine aggregationEngine;

    @Autowired
    private AggregationStrategyFactory strategyFactory;

    @Autowired
    private FederatedTaskService federatedTaskService;

    @Autowired
    private LogService logService;

    /**
     * 查询聚合引擎状态
     *
     * @param taskId 任务ID (可选)
     * @param engineId 引擎ID (可选)
     * @param request HTTP请求对象
     * @return 聚合引擎状态信息
     */
    @GetMapping("/status")
    public Result<EngineStatusVO> getEngineStatus(
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String engineId,
            HttpServletRequest request) {

        String userId = BaseContext.getCurrentId();
        String clientIp = IpUtil.getClientIpAddress(request);

        log.info("收到聚合引擎状态查询请求: taskId={}, engineId={}, userId={}, ip={}",
                taskId, engineId, userId, clientIp);

        // 记录访问日志
        logService.logInfo("聚合引擎状态查询",
                userId, null, request.getRequestURI(), clientIp, "SYSTEM.monitor");

        try {
            // 构建引擎状态响应
            EngineStatusVO.EngineStatusVOBuilder statusBuilder = EngineStatusVO.builder()
                    .engineStatus("RUNNING")
                    .systemMetrics(buildSystemMetrics())
                    .aggregationMetrics(buildAggregationMetrics())
                    .supportedAlgorithms(getSupportedAlgorithms());

            // 如果指定了taskId，查询任务相关状态
            if (taskId != null && !taskId.trim().isEmpty()) {
                statusBuilder.currentTasks(buildCurrentTasks(taskId));
            } else {
                // 返回所有运行中的任务状态
                statusBuilder.currentTasks(buildAllCurrentTasks());
            }

            EngineStatusVO status = statusBuilder.build();

            log.info("聚合引擎状态查询成功: taskId={}, engineId={}, userId={}", taskId, engineId, userId);

            return Result.success(status);

        } catch (Exception e) {
            log.error("聚合引擎状态查询失败: taskId={}, engineId={}, userId={}, error={}",
                    taskId, engineId, userId, e.getMessage(), e);
            return Result.error("聚合引擎状态查询失败: " + e.getMessage());
        }
    }

    /**
     * 构建系统性能指标
     */
    private SystemMetricsVO buildSystemMetrics() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            double cpuUsage = osBean.getProcessCpuLoad() * 100;
            if (cpuUsage < 0) cpuUsage = 0.0; // 处理无效值

            long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            double memoryUsage = totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0.0;

            return SystemMetricsVO.builder()
                    .cpuUsage(Math.round(cpuUsage * 10) / 10.0)
                    .memoryUsage(Math.round(memoryUsage * 10) / 10.0)
                    .diskUsage(25.5) // 模拟磁盘使用率
                    .build();
        } catch (Exception e) {
            log.warn("获取系统指标失败: {}", e.getMessage());
            return SystemMetricsVO.builder()
                    .cpuUsage(0.0)
                    .memoryUsage(0.0)
                    .diskUsage(0.0)
                    .build();
        }
    }

    /**
     * 构建聚合性能指标
     */
    private AggregationMetricsVO buildAggregationMetrics() {
        return AggregationMetricsVO.builder()
                .totalAggregations(125L) // 模拟数据，实际应从数据库或缓存获取
                .successRate(0.98)
                .averageAggregationTime(2.3)
                .lastAggregationTime(Instant.now().minusSeconds(300).toString())
                .build();
    }

    /**
     * 获取支持的算法列表
     */
    private List<String> getSupportedAlgorithms() {
        return Arrays.stream(FederatedAlgorithm.values())
                .map(FederatedAlgorithm::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 构建指定任务的状态信息
     */
    private List<CurrentTaskVO> buildCurrentTasks(String taskId) {
        try {
            // 这里应该调用实际的任务服务获取任务状态
            // 目前先返回模拟数据
            return List.of(
                    CurrentTaskVO.builder()
                            .taskId(taskId)
                            .status("RUNNING")
                            .currentRound(5)
                            .algorithm("FEDERATED_AVERAGING")
                            .participantCount(3)
                            .build()
            );
        } catch (Exception e) {
            log.warn("获取任务状态失败: taskId={}, error={}", taskId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 构建所有运行中任务的状态信息
     */
    private List<CurrentTaskVO> buildAllCurrentTasks() {
        // 这里应该查询所有运行中的任务
        // 目前返回空列表，实际实现时需要调用任务服务
        return List.of();
    }

    // VO类定义

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngineStatusVO {
        private String engineStatus;
        private List<CurrentTaskVO> currentTasks;
        private SystemMetricsVO systemMetrics;
        private AggregationMetricsVO aggregationMetrics;
        private List<String> supportedAlgorithms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentTaskVO {
        private String taskId;
        private String status;
        private Integer currentRound;
        private String algorithm;
        private Integer participantCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMetricsVO {
        private Double cpuUsage;
        private Double memoryUsage;
        private Double diskUsage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregationMetricsVO {
        private Long totalAggregations;
        private Double successRate;
        private Double averageAggregationTime;
        private String lastAggregationTime;
    }
}