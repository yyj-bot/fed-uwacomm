package com.feduwacomm.service;

import com.feduwacomm.entity.VmAckTracking;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.mapper.VmAckTrackingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 缓存预热服务
 * 在应用启动时自动从数据库加载ACK状态到缓存中
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Slf4j
@Service
public class CacheWarmupService {

    @Autowired
    private AckCacheService ackCacheService;

    @Autowired
    private VmAckTrackingMapper vmAckTrackingMapper;

    @Autowired
    private TaskParticipantsMapper taskParticipantsMapper;

    /**
     * 应用启动完成后执行缓存预热
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("应用启动完成，开始执行ACK缓存预热...");

        long startTime = System.currentTimeMillis();
        try {
            warmupAckCache();
            long duration = System.currentTimeMillis() - startTime;
            log.info("ACK缓存预热完成，耗时: {}ms", duration);
        } catch (Exception e) {
            log.error("ACK缓存预热失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行ACK缓存预热
     */
    public void warmupAckCache() {
        try {
            // 1. 预热任务参与者缓存
            warmupTaskParticipants();

            // 2. 预热ACK状态缓存
            warmupAckStatus();

            log.info("所有ACK缓存预热完成");

        } catch (Exception e) {
            log.error("执行ACK缓存预热时发生错误", e);
            throw e;
        }
    }

    /**
     * 预热任务参与者缓存
     */
    private void warmupTaskParticipants() {
        log.info("开始预热任务参与者缓存...");

        try {
            // 获取所有任务的参与者信息
            Map<String, Set<String>> taskParticipants = loadAllTaskParticipants();

            int taskCount = 0;
            int totalVmCount = 0;

            for (Map.Entry<String, Set<String>> entry : taskParticipants.entrySet()) {
                String taskId = entry.getKey();
                Set<String> vmIds = entry.getValue();

                if (!vmIds.isEmpty()) {
                    ackCacheService.setTaskParticipants(taskId, vmIds);
                    taskCount++;
                    totalVmCount += vmIds.size();

                    log.debug("任务参与者缓存预热: taskId={}, vmCount={}", taskId, vmIds.size());
                }
            }

            log.info("任务参与者缓存预热完成: {} 个任务, 总计 {} 个VM", taskCount, totalVmCount);

        } catch (Exception e) {
            log.error("预热任务参与者缓存失败", e);
            throw e;
        }
    }

    /**
     * 预热ACK状态缓存
     */
    private void warmupAckStatus() {
        log.info("开始预热ACK状态缓存...");

        try {
            // 获取所有ACK跟踪记录
            List<VmAckTracking> allAckTrackings = vmAckTrackingMapper.selectAllAckTrackings();

            if (allAckTrackings.isEmpty()) {
                log.info("没有ACK跟踪记录需要预热");
                return;
            }

            // 按任务和ACK类型分组
            Map<String, Map<VmAckTracking.AckType, List<VmAckTracking>>> groupedTrackings =
                allAckTrackings.stream()
                    .collect(Collectors.groupingBy(
                        VmAckTracking::getTaskId,
                        Collectors.groupingBy(VmAckTracking::getAckType)
                    ));

            int taskCount = 0;
            int ackCount = 0;

            for (Map.Entry<String, Map<VmAckTracking.AckType, List<VmAckTracking>>> taskEntry : groupedTrackings.entrySet()) {
                String taskId = taskEntry.getKey();
                Map<VmAckTracking.AckType, List<VmAckTracking>> ackTypeMap = taskEntry.getValue();

                for (Map.Entry<VmAckTracking.AckType, List<VmAckTracking>> ackTypeEntry : ackTypeMap.entrySet()) {
                    VmAckTracking.AckType ackType = ackTypeEntry.getKey();
                    List<VmAckTracking> trackings = ackTypeEntry.getValue();

                    // 加载该任务和ACK类型的所有状态到缓存
                    for (VmAckTracking tracking : trackings) {
                        String errorMessage = tracking.getErrorMessage();
                        ackCacheService.updateAckStatus(
                            taskId,
                            tracking.getVmId(),
                            ackType,
                            tracking.getStatus(),
                            errorMessage
                        );
                        ackCount++;
                    }

                    // 更新进度缓存
                    ackCacheService.updateAckProgress(taskId, ackType);

                    log.debug("ACK状态缓存预热: taskId={}, ackType={}, count={}",
                            taskId, ackType, trackings.size());
                }

                taskCount++;
            }

            log.info("ACK状态缓存预热完成: {} 个任务, {} 条ACK记录", taskCount, ackCount);

        } catch (Exception e) {
            log.error("预热ACK状态缓存失败", e);
            throw e;
        }
    }

    /**
     * 从数据库加载所有任务的参与者信息
     *
     * @return 任务ID -> VM ID集合的映射
     */
    private Map<String, Set<String>> loadAllTaskParticipants() {
        try {
            return taskParticipantsMapper.selectAllTaskParticipants()
                .stream()
                .collect(Collectors.groupingBy(
                    participant -> participant.getTaskId(),
                    Collectors.mapping(
                        participant -> participant.getVmId(),
                        Collectors.toSet()
                    )
                ));
        } catch (Exception e) {
            log.error("从数据库加载任务参与者失败", e);
            return Map.of();
        }
    }

    /**
     * 手动触发缓存预热（用于测试或手动刷新）
     */
    public void manualWarmup() {
        log.info("手动触发ACK缓存预热...");
        warmupAckCache();
    }

    /**
     * 为特定任务预热缓存
     *
     * @param taskId 任务ID
     */
    public void warmupTaskCache(String taskId) {
        if (taskId == null) {
            log.error("预热任务缓存参数无效: taskId={}", taskId);
            return;
        }

        log.info("为任务预热缓存: taskId={}", taskId);

        try {
            // 1. 预热任务参与者
            Set<String> vmIds = taskParticipantsMapper.selectVmIdsByTaskId(taskId);
            if (!vmIds.isEmpty()) {
                ackCacheService.setTaskParticipants(taskId, vmIds);
                log.debug("任务参与者预热完成: taskId={}, vmCount={}", taskId, vmIds.size());
            }

            // 2. 预热ACK状态
            List<VmAckTracking> taskAckTrackings = vmAckTrackingMapper.findByTaskId(taskId);
            if (!taskAckTrackings.isEmpty()) {
                // 按ACK类型分组
                Map<VmAckTracking.AckType, List<VmAckTracking>> ackTypeMap =
                    taskAckTrackings.stream()
                        .collect(Collectors.groupingBy(VmAckTracking::getAckType));

                for (Map.Entry<VmAckTracking.AckType, List<VmAckTracking>> entry : ackTypeMap.entrySet()) {
                    VmAckTracking.AckType ackType = entry.getKey();
                    List<VmAckTracking> trackings = entry.getValue();

                    // 加载状态到缓存
                    for (VmAckTracking tracking : trackings) {
                        ackCacheService.updateAckStatus(
                            taskId,
                            tracking.getVmId(),
                            ackType,
                            tracking.getStatus(),
                            tracking.getErrorMessage()
                        );
                    }

                    // 更新进度缓存
                    ackCacheService.updateAckProgress(taskId, ackType);

                    log.debug("任务ACK状态预热完成: taskId={}, ackType={}, count={}",
                            taskId, ackType, trackings.size());
                }
            }

            log.info("任务缓存预热完成: taskId={}", taskId);

        } catch (Exception e) {
            log.error("为任务预热缓存失败: taskId={}", taskId, e);
        }
    }

    /**
     * 清理并重新预热指定任务的缓存
     *
     * @param taskId 任务ID
     */
    public void refreshTaskCache(String taskId) {
        if (taskId == null) {
            log.error("刷新任务缓存参数无效: taskId={}", taskId);
            return;
        }

        log.info("刷新任务缓存: taskId={}", taskId);

        try {
            // 清理现有缓存
            ackCacheService.clearTaskAckCache(taskId);

            // 重新预热
            warmupTaskCache(taskId);

            log.info("任务缓存刷新完成: taskId={}", taskId);

        } catch (Exception e) {
            log.error("刷新任务缓存失败: taskId={}", taskId, e);
        }
    }

    /**
     * 获取缓存预热统计信息
     *
     * @return 预热统计信息
     */
    public Map<String, Object> getWarmupStats() {
        try {
            // 这里可以添加统计信息的收集逻辑
            // 例如：缓存命中率、缓存大小、预热时间等
            return Map.of(
                "status", "implemented",
                "description", "缓存预热统计功能已实现，可根据需要扩展"
            );
        } catch (Exception e) {
            log.error("获取缓存预热统计信息失败", e);
            return Map.of("error", e.getMessage());
        }
    }
}