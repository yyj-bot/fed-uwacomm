package com.feduwacomm.listener;

import com.feduwacomm.dto.DataDistributionDTO;
import com.feduwacomm.event.FederatedTaskCreatedEvent;
import com.feduwacomm.service.DataDistributionService;
import com.feduwacomm.service.LogService;
import com.feduwacomm.vo.DataDistributionTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 联邦学习任务事件监听器
 * 处理任务创建相关的事件，自动触发数据分发流程
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FederatedTaskEventListener {

    private final DataDistributionService dataDistributionService;
    private final LogService logService;

    /**
     * 处理联邦学习任务启动事件（v1.5.1修正版）
     * 任务启动时触发数据切片和分发流程
     *
     * @param event 任务启动事件
     */
    @EventListener
    // @Async  // 暂时禁用异步，用于调试
    public void handleTaskStarted(com.feduwacomm.event.FederatedTaskStartedEvent event) {
        System.out.println("🔥🔥🔥 FederatedTaskEventListener.handleTaskStarted() 被调用");
        log.info("🎯 处理联邦学习任务启动事件 (v1.5.1修正版): {}", event);
        System.out.println("🔥🔥🔥 事件内容: taskId=" + event.getTaskId() + ", datasetId=" + event.getDatasetId());

        try {
            String taskId = event.getTaskId();
            String datasetId = event.getDatasetId();
            List<String> participantVmIds = event.getParticipantVmIds();
            String distributionStrategy = event.getDistributionStrategy();

            // 验证必要参数
            if (datasetId == null || datasetId.isEmpty()) {
                log.warn("⚠️ 任务没有配置数据集，跳过自动数据分发: taskId={}", taskId);
                return;
            }

            if (participantVmIds == null || participantVmIds.isEmpty()) {
                log.warn("⚠️ 任务没有配置参与者，跳过自动数据分发: taskId={}", taskId);
                return;
            }

            log.info("🔥 开始自动触发数据分发 (v1.5.1): taskId={}, datasetId={}, vmCount={}, strategy={}",
                    taskId, datasetId, participantVmIds.size(), distributionStrategy);

            // 记录日志
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("taskId", taskId);
            logDetails.put("datasetId", datasetId);
            logDetails.put("participantVmIds", participantVmIds);
            logDetails.put("distributionStrategy", distributionStrategy);
            logDetails.put("eventTime", event.getEventTime());

            logService.logTask(taskId, "INFO",
                    String.format("任务启动，自动触发数据分发: 数据集=%s, 参与者数=%d, 策略=%s",
                            datasetId, participantVmIds.size(), distributionStrategy),
                    "FederatedTaskEventListener", null, logDetails);

            // 构建数据分发请求DTO
            DataDistributionDTO distributionDTO = DataDistributionDTO.builder()
                    .taskId(taskId)
                    .datasetIds(List.of(datasetId))  // 单个数据集
                    .distributionStrategy(distributionStrategy != null ? distributionStrategy : "BALANCED")
                    .targetVmIds(participantVmIds)
                    .distributionConfig(new HashMap<>())
                    .shardCount(Math.min(participantVmIds.size() * 2, 10))
                    .enableShuffle(false)  // 不打乱数据顺序，保持切片的连续性
                    .enableCompression(false)
                    .verifyIntegrity(true)
                    .timeoutSeconds(600)
                    .maxRetries(3)
                    .build();

            // 创建数据分发任务
            log.info("📦 创建数据分发任务: taskId={}, datasetId={}", taskId, datasetId);
            DataDistributionTaskVO distributionTask = dataDistributionService.createDistributionTask(
                    distributionDTO, event.getStartedBy());

            if (distributionTask == null) {
                log.error("❌ 创建数据分发任务失败: taskId={}, datasetId={}", taskId, datasetId);
                logService.logTask(taskId, "ERROR",
                        "自动触发数据分发失败：创建分发任务返回null",
                        "FederatedTaskEventListener", null, logDetails);
                return;
            }

            log.info("✅ 数据分发任务创建成功: distributionId={}", distributionTask.getDistributionId());

            // 启动数据分发
            log.info("🚀 启动数据分发任务: distributionId={}", distributionTask.getDistributionId());
            DataDistributionTaskVO startedTask = dataDistributionService.startDistribution(
                    distributionTask.getDistributionId(), event.getStartedBy());

            if (startedTask != null) {
                log.info("✅ 数据分发任务启动成功 (v1.5.1): taskId={}, distributionId={}, status={}",
                        taskId, startedTask.getDistributionId(), startedTask.getStatus());

                logDetails.put("distributionId", startedTask.getDistributionId());
                logDetails.put("distributionStatus", startedTask.getStatus());

                logService.logTask(taskId, "INFO",
                        String.format("数据分发任务启动成功: distributionId=%s, status=%s",
                                startedTask.getDistributionId(), startedTask.getStatus()),
                        "FederatedTaskEventListener", null, logDetails);
            } else {
                log.error("❌ 数据分发任务启动失败: distributionId={}", distributionTask.getDistributionId());
                logService.logTask(taskId, "ERROR",
                        "自动触发数据分发失败：启动分发任务返回null",
                        "FederatedTaskEventListener", null, logDetails);
            }

        } catch (Exception e) {
            log.error("❌ 处理任务启动事件失败，自动数据分发异常: taskId={}, error={}",
                    event.getTaskId(), e.getMessage(), e);

            try {
                logService.logTask(event.getTaskId(), "ERROR",
                        "处理任务启动事件失败: " + e.getMessage(),
                        "FederatedTaskEventListener", null,
                        Map.of("errorMessage", e.getMessage(), "errorType", e.getClass().getSimpleName()));
            } catch (Exception logEx) {
                log.error("记录错误日志失败", logEx);
            }
        }
    }
}
