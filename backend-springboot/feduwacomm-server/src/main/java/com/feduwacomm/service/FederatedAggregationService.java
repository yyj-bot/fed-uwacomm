package com.feduwacomm.service;

import com.feduwacomm.config.AggregationConfig;
import com.feduwacomm.dto.TaskQueryDTO;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.enums.AggregationMethod;
import com.feduwacomm.enums.GlobalModelStatus;
import com.feduwacomm.event.AggregationCompletedEvent;
import com.feduwacomm.event.AggregationTriggeredEvent;
import com.feduwacomm.event.ModelUploadEvent;
import com.feduwacomm.event.RoundCompleteEvent;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.GlobalModelMapper;
import com.feduwacomm.mapper.VmRoundModelsMapper;
import com.feduwacomm.strategy.AggregationStrategy;
import com.feduwacomm.strategy.AggregationStrategyFactory;
import com.feduwacomm.utils.UuidUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 联邦学习聚合服务
 * 核心聚合协调器，负责触发条件检查、聚合执行、模型分发等
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FederatedAggregationService {

    private final VmRoundModelsMapper vmRoundModelsMapper;
    private final FederatedTasksMapper federatedTasksMapper;
    private final GlobalModelMapper globalModelMapper;
    private final ModelAggregatorEngine aggregatorEngine;
    private final AggregationStrategyFactory strategyFactory;
    private final AggregationConfig aggregationConfig;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final UuidUtil uuidUtil;

    // 聚合状态管理
    private final Map<String, LocalDateTime> roundStartTimes = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> aggregationLocks = new ConcurrentHashMap<>();
    private final Set<String> activeAggregations = ConcurrentHashMap.newKeySet();

    /**
     * 处理模型上传事件
     * 检查是否满足聚合条件，如满足则触发聚合
     */
    @EventListener
    @Async
    @Transactional
    public void handleModelUploadEvent(ModelUploadEvent event) {
        String taskId = event.getTaskId();
        Integer roundNumber = event.getRoundNumber();
        String aggregationKey = buildAggregationKey(taskId, roundNumber);

        log.info("处理模型上传事件: 任务ID={}, 轮次={}, 虚拟机ID={}", 
                taskId, roundNumber, event.getVmId());

        try {
            // 获取聚合锁，避免并发触发
            ReentrantLock lock = aggregationLocks.computeIfAbsent(aggregationKey, k -> new ReentrantLock());
            
            if (!lock.tryLock()) {
                log.debug("聚合已在进行中，跳过: {}", aggregationKey);
                return;
            }

            try {
                // 记录轮次开始时间（如果还没记录）
                roundStartTimes.putIfAbsent(aggregationKey, LocalDateTime.now());

                // 检查聚合条件
                if (shouldTriggerAggregation(taskId, roundNumber)) {
                    triggerAggregation(taskId, roundNumber, "MODEL_UPLOAD_COMPLETE");
                }
                
            } finally {
                lock.unlock();
            }

        } catch (Exception e) {
            log.error("处理模型上传事件失败: 任务ID={}, 轮次={}, 错误={}", 
                    taskId, roundNumber, e.getMessage(), e);
        }
    }

    /**
     * 定时检查待聚合的任务
     * 处理超时场景，强制触发符合条件的聚合
     */
    @Scheduled(fixedDelayString = "#{aggregationConfig.checkIntervalSeconds * 1000}")
    public void checkPendingAggregations() {
        if (!aggregationConfig.isEnableTimeoutAggregation()) {
            return;
        }

        log.debug("执行定时聚合检查");

        try {
            // 查询运行中的任务
            List<FederatedTask> runningTasks = federatedTasksMapper.selectTasksByQuery(
                    TaskQueryDTO.builder()
                            .status("RUNNING")
                            .page(1)
                            .size(100)
                            .build());

            for (FederatedTask task : runningTasks) {
                checkTaskForTimeoutAggregation(task);
            }

        } catch (Exception e) {
            log.error("定时聚合检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查任务是否需要超时聚合
     */
    private void checkTaskForTimeoutAggregation(FederatedTask task) {
        String taskId = task.getId();
        Integer currentRound = task.getCurrentRound();
        String aggregationKey = buildAggregationKey(taskId, currentRound);

        // 跳过正在聚合的任务
        if (activeAggregations.contains(aggregationKey)) {
            return;
        }

        LocalDateTime roundStartTime = roundStartTimes.get(aggregationKey);
        if (roundStartTime == null) {
            return;
        }

        long waitTimeSeconds = ChronoUnit.SECONDS.between(roundStartTime, LocalDateTime.now());
        
        if (waitTimeSeconds >= aggregationConfig.getMaxWaitTimeSeconds()) {
            log.info("任务{}轮次{}等待超时，检查超时聚合条件", taskId, currentRound);
            
            ReentrantLock lock = aggregationLocks.computeIfAbsent(aggregationKey, k -> new ReentrantLock());
            if (lock.tryLock()) {
                try {
                    if (shouldTriggerTimeoutAggregation(taskId, currentRound)) {
                        triggerAggregation(taskId, currentRound, "TIMEOUT");
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * 检查是否应该触发聚合
     */
    private boolean shouldTriggerAggregation(String taskId, Integer roundNumber) {
        try {
            // 获取期望参与者数量
            int expectedParticipants = getExpectedParticipantCount(taskId);
            
            // 获取当前已上传模型数量
            int currentParticipants = vmRoundModelsMapper.countReadyModels(taskId, roundNumber);
            
            // 计算等待时间
            String aggregationKey = buildAggregationKey(taskId, roundNumber);
            LocalDateTime startTime = roundStartTimes.get(aggregationKey);
            long waitTimeSeconds = startTime != null ? 
                    ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()) : 0;

            boolean shouldTrigger = aggregationConfig.shouldTriggerAggregation(
                    currentParticipants, expectedParticipants, waitTimeSeconds);

            log.debug("聚合条件检查: 任务ID={}, 轮次={}, 当前参与者={}, 期望参与者={}, " +
                      "等待时间={}秒, 是否触发={}", 
                    taskId, roundNumber, currentParticipants, expectedParticipants, 
                    waitTimeSeconds, shouldTrigger);

            return shouldTrigger;

        } catch (Exception e) {
            log.error("检查聚合条件失败: 任务ID={}, 轮次={}, 错误={}", 
                    taskId, roundNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查是否应该触发超时聚合
     */
    private boolean shouldTriggerTimeoutAggregation(String taskId, Integer roundNumber) {
        int currentParticipants = vmRoundModelsMapper.countReadyModels(taskId, roundNumber);
        return currentParticipants >= aggregationConfig.getMinParticipants();
    }

    /**
     * 触发聚合流程
     */
    @Async
    @Transactional
    public void triggerAggregation(String taskId, Integer roundNumber, String triggerReason) {
        String aggregationKey = buildAggregationKey(taskId, roundNumber);
        FederatedTask task = null; // 在try块外定义task变量
        
        // 防止重复聚合
        if (!activeAggregations.add(aggregationKey)) {
            log.debug("聚合已在进行中，跳过: {}", aggregationKey);
            return;
        }

        try {
            log.info("开始聚合流程: 任务ID={}, 轮次={}, 触发原因={}", 
                    taskId, roundNumber, triggerReason);

            // 获取任务信息
            task = federatedTasksMapper.selectTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("任务不存在: " + taskId);
            }

            // 获取本轮所有本地模型
            List<VmRoundModel> localModels = vmRoundModelsMapper
                    .selectByTaskIdAndRound(taskId, roundNumber);
            
            if (localModels.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("任务%s轮次%d没有可用的本地模型", taskId, roundNumber));
            }

            // 发布聚合触发事件
            List<String> participantVmIds = localModels.stream()
                    .map(VmRoundModel::getVmId)
                    .toList();
            
            AggregationTriggeredEvent triggeredEvent = new AggregationTriggeredEvent(
                    this, taskId, roundNumber, task.getAlgorithm().getCode(), 
                    participantVmIds, triggerReason);
            eventPublisher.publishEvent(triggeredEvent);

            // 执行聚合
            executeAggregation(task, localModels, roundNumber);

        } catch (Exception e) {
            log.error("聚合流程执行失败: 任务ID={}, 轮次={}, 错误={}", 
                    taskId, roundNumber, e.getMessage(), e);
            
            // 发布聚合失败事件
            AggregationCompletedEvent failureEvent = AggregationCompletedEvent.failure(
                    this, taskId, roundNumber, e.getMessage(), 
                    vmRoundModelsMapper.countReadyModels(taskId, roundNumber),
                    0L, task != null ? task.getAlgorithm().getCode() : "UNKNOWN");
            eventPublisher.publishEvent(failureEvent);
            
        } finally {
            activeAggregations.remove(aggregationKey);
            roundStartTimes.remove(aggregationKey);
        }
    }

    /**
     * 执行具体的聚合计算
     */
    private void executeAggregation(FederatedTask task, List<VmRoundModel> localModels, Integer roundNumber) {
        String taskId = task.getId();
        String algorithm = task.getAlgorithm().getCode();
        
        log.info("执行聚合计算: 任务ID={}, 算法={}, 模型数量={}", 
                taskId, algorithm, localModels.size());

        try {
            // 获取聚合策略
            AggregationStrategy strategy = strategyFactory.getStrategy(algorithm);
            
            // 验证前置条件
            AggregationStrategy.ValidationResult validation = 
                    strategy.validatePreconditions(localModels, task);
            
            if (!validation.isValid()) {
                throw new IllegalArgumentException("聚合前置条件验证失败: " + validation.getErrorMessage());
            }

            // 执行聚合
            ModelAggregatorEngine.AggregationResult result = 
                    strategy.aggregate(localModels, task, aggregatorEngine);

            if (result.isSuccess()) {
                // 保存全局模型
                GlobalModel globalModel = saveGlobalModel(task, roundNumber, result);
                
                // 分发全局模型
                distributeGlobalModel(task, roundNumber, result.getGlobalParameters());
                
                // 更新任务进度
                updateTaskProgress(taskId, roundNumber + 1);
                
                // 发布成功事件
                AggregationCompletedEvent successEvent = AggregationCompletedEvent.success(
                        this, taskId, roundNumber, globalModel.getId(), 
                        result.getGlobalMetrics(), result.getParticipantCount(),
                        result.getAggregationDuration(), result.getAlgorithm());
                eventPublisher.publishEvent(successEvent);
                
                log.info("聚合成功完成: 任务ID={}, 轮次={}, 参与者数量={}, 耗时={}ms", 
                        taskId, roundNumber, result.getParticipantCount(), result.getAggregationDuration());
                
            } else {
                throw new RuntimeException("聚合执行失败: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("聚合执行异常: 任务ID={}, 错误={}", taskId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 保存全局模型到数据库
     */
    private GlobalModel saveGlobalModel(FederatedTask task, Integer roundNumber, 
                                      ModelAggregatorEngine.AggregationResult result) {
        try {
            GlobalModel globalModel = GlobalModel.builder()
                    .id(uuidUtil.generateUuid())
                    .taskId(task.getId())
                    .roundNumber(roundNumber)
                    .aggregationMethod(AggregationMethod.fromCode(result.getAlgorithm()))
                    .globalParameters(objectMapper.writeValueAsString(result.getGlobalParameters()))
                    .participantCount(result.getParticipantCount())
                    .aggregationDuration(result.getAggregationDuration())
                    .status(GlobalModelStatus.COMPLETED)
                    .startedAt(LocalDateTime.now().minus(result.getAggregationDuration(), ChronoUnit.MILLIS))
                    .completedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 设置全局指标
            if (result.getGlobalMetrics() != null) {
                globalModel.setGlobalLoss(result.getGlobalMetrics().get("loss"));
                globalModel.setGlobalAccuracy(result.getGlobalMetrics().get("accuracy"));
            }

            globalModelMapper.insertGlobalModel(globalModel);
            
            log.debug("全局模型已保存: ID={}, 任务ID={}, 轮次={}", 
                    globalModel.getId(), task.getId(), roundNumber);
            
            return globalModel;

        } catch (Exception e) {
            log.error("保存全局模型失败: 任务ID={}, 轮次={}, 错误={}", 
                    task.getId(), roundNumber, e.getMessage(), e);
            throw new RuntimeException("保存全局模型失败", e);
        }
    }

    /**
     * 分发全局模型给所有参与的客户端
     * 注：实际分发由GlobalModelDistributionService通过事件监听自动处理
     */
    private void distributeGlobalModel(FederatedTask task, Integer roundNumber, 
                                     Map<String, Object> globalParameters) {
        log.info("全局模型分发将通过AggregationCompletedEvent自动触发: 任务ID={}, 轮次={}", 
                task.getId(), roundNumber);
        // 分发逻辑已通过GlobalModelDistributionService的事件监听器实现
    }

    /**
     * 更新任务进度
     */
    private void updateTaskProgress(String taskId, Integer nextRound) {
        try {
            federatedTasksMapper.updateTaskProgress(taskId, nextRound, null, "RUNNING");
            log.debug("任务进度已更新: 任务ID={}, 下一轮次={}", taskId, nextRound);
        } catch (Exception e) {
            log.error("更新任务进度失败: 任务ID={}, 错误={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 获取期望参与者数量
     */
    private int getExpectedParticipantCount(String taskId) {
        try {
            FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            return task != null && task.getMinParticipants() != null ? 
                    task.getMinParticipants() : aggregationConfig.getMinParticipants();
        } catch (Exception e) {
            log.warn("获取期望参与者数量失败，使用默认值: {}", e.getMessage());
            return aggregationConfig.getMinParticipants();
        }
    }

    /**
     * 构建聚合键（任务ID + 轮次）
     */
    private String buildAggregationKey(String taskId, Integer roundNumber) {
        return taskId + "_" + roundNumber;
    }

}