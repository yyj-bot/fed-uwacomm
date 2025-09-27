package com.feduwacomm.service;

import com.feduwacomm.config.AggregationConfig;
import com.feduwacomm.dto.TaskQueryDTO;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.enums.AggregationMethod;
import com.feduwacomm.enums.GlobalModelStatus;
import com.feduwacomm.enums.RoundState;
import com.feduwacomm.event.AggregationCompletedEvent;
import com.feduwacomm.event.AggregationTriggeredEvent;
import com.feduwacomm.event.ModelUploadEvent;
import com.feduwacomm.event.RoundCompleteEvent;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.GlobalModelMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.mapper.VmRoundModelsMapper;
import com.feduwacomm.aggregation.AggregationStrategy;
import com.feduwacomm.aggregation.AggregationStrategyFactory;
import com.feduwacomm.aggregation.UniversalAggregationEngine;
import com.feduwacomm.aggregation.UniversalAggregationEngine.AggregationResult;
import com.feduwacomm.enums.FederatedAlgorithm;
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
    private final TaskParticipantsMapper taskParticipantsMapper;
    private final UniversalAggregationEngine aggregationEngine;
    private final AggregationStrategyFactory strategyFactory;
    private final AggregationConfig aggregationConfig;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final UuidUtil uuidUtil;

    // 🔧 轮次同步组件
    private final RoundStateManager roundStateManager;
    private final RoundLockManager roundLockManager;
    private final VmAckTracker vmAckTracker;

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
            // 🔧 使用轮次锁管理器获取分布式锁，防止竞态条件
            if (!roundLockManager.acquireRoundLock(taskId)) {
                log.warn("获取轮次锁失败，跳过聚合检查: 任务ID={}, 轮次={}", taskId, roundNumber);
                return;
            }

            try {
                // 记录轮次开始时间（如果还没记录）
                roundStartTimes.putIfAbsent(aggregationKey, LocalDateTime.now());

                // 🔧 增强轮次状态检查，确保轮次同步
                RoundState currentState = roundStateManager.getCurrentRoundState(taskId);
                log.debug("当前轮次状态: 任务ID={}, 状态={}", taskId, currentState);

                // 检查聚合条件
                if (shouldTriggerAggregation(taskId, roundNumber)) {
                    triggerAggregation(taskId, roundNumber, "MODEL_UPLOAD_COMPLETE");
                }

            } finally {
                // 🔧 确保轮次锁被正确释放
                roundLockManager.releaseRoundLock(taskId);
            }

        } catch (Exception e) {
            log.error("处理模型上传事件失败: 任务ID={}, 轮次={}, 错误={}",
                    taskId, roundNumber, e.getMessage(), e);
            // 异常情况下也要释放锁
            try {
                roundLockManager.releaseRoundLock(taskId);
            } catch (Exception lockEx) {
                log.error("释放轮次锁失败: 任务ID={}", taskId, lockEx);
            }
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
     * 采用动态轮次检测策略：检测参与者的最新完成轮次并触发相应的聚合
     */
    private boolean shouldTriggerAggregation(String taskId, Integer roundNumber) {
        try {
            // 获取任务总参与者数量
            int totalParticipants = taskParticipantsMapper.countTotalParticipants(taskId);
            if (totalParticipants == 0) {
                log.warn("任务{}没有参与者，无法进行聚合", taskId);
                return false;
            }

            // 🔧 增强调试：安全地获取参与者详细状态信息
            List<Map<String, Object>> participantDetails = null;
            try {
                participantDetails = taskParticipantsMapper.getParticipantStatusDetails(taskId);
                log.debug("参与者状态详情: 任务ID={}, 参与者数量={}", taskId,
                         participantDetails != null ? participantDetails.size() : "null");
            } catch (Exception e) {
                log.error("获取参与者详情失败: 任务ID={}, 错误={}", taskId, e.getMessage());
                participantDetails = new ArrayList<>(); // 使用空列表避免后续null检查
            }

            // 首先检查传入的roundNumber是否有完成的参与者
            int completedParticipants = taskParticipantsMapper.countCompletedParticipants(taskId, roundNumber);
            log.debug("初始检查: 任务ID={}, 检查轮次={}, 已完成参与者={}",
                    taskId, roundNumber, completedParticipants);

            // 如果传入轮次没有完成参与者，检查参与者实际完成的轮次
            if (completedParticipants == 0) {
                // 查询参与者实际完成的最新轮次
                Integer actualCompletedRound = getLatestCompletedRound(taskId);
                log.debug("实际完成轮次检查: 任务ID={}, 最新完成轮次={}", taskId, actualCompletedRound);

                if (actualCompletedRound != null && !actualCompletedRound.equals(roundNumber)) {
                    log.warn("❌ 轮次同步异常检测：期望轮次({}) != 当前轮次({})", roundNumber, actualCompletedRound);

                    // 使用参与者实际完成的轮次重新检查
                    completedParticipants = taskParticipantsMapper.countCompletedParticipants(taskId, actualCompletedRound);
                    roundNumber = actualCompletedRound; // 更新为实际轮次

                    log.info("🔄 轮次修正: 任务ID={}, 修正轮次={}, 重新统计已完成参与者={}",
                            taskId, actualCompletedRound, completedParticipants);
                }
            }

            // 计算等待时间
            String aggregationKey = buildAggregationKey(taskId, roundNumber);
            LocalDateTime startTime = roundStartTimes.get(aggregationKey);
            long waitTimeSeconds = startTime != null ?
                    ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()) : 0;

            // 严格的同步策略：所有参与者必须完成当前轮次
            boolean allParticipantsCompleted = (completedParticipants == totalParticipants);

            // 超时处理：如果等待时间过长，允许部分参与者的聚合
            boolean isTimeout = waitTimeSeconds >= aggregationConfig.getMaxWaitTimeSeconds();
            boolean hasMinParticipants = completedParticipants >= aggregationConfig.getMinParticipants();

            boolean shouldTrigger = allParticipantsCompleted || (isTimeout && hasMinParticipants);

            // 🔧 增强日志：提供更详细的聚合决策信息
            if (completedParticipants == 0 && totalParticipants > 0) {
                log.warn("📊 聚合服务计数错误: 已完成参与者=0/{}, 等待时间={}秒, 满足最少参与者={}, 是否触发={}",
                        totalParticipants, waitTimeSeconds, hasMinParticipants, shouldTrigger);

                // 🔧 修复：安全地输出每个参与者的状态
                if (participantDetails != null && !participantDetails.isEmpty()) {
                    log.debug("参与者详情数量: {}", participantDetails.size());
                    for (int i = 0; i < participantDetails.size(); i++) {
                        Map<String, Object> participant = participantDetails.get(i);
                        if (participant != null) {
                            log.debug("参与者状态[{}]: VM={}, 轮次={}, 状态={}, 更新时间={}",
                                    i, participant.get("vm_id"), participant.get("current_epoch"),
                                    participant.get("status"), participant.get("updated_at"));
                        } else {
                            log.warn("⚠️ 发现null参与者元素[{}]，跳过处理", i);
                        }
                    }
                } else {
                    log.warn("⚠️ 参与者详情列表为空或null: participantDetails={}", participantDetails);
                }
            } else {
                log.info("聚合条件检查: 任务ID={}, 轮次={}, 已完成参与者={}/{}, 等待时间={}秒, " +
                          "所有完成={}, 超时={}, 满足最少参与者={}, 是否触发={}",
                        taskId, roundNumber, completedParticipants, totalParticipants, waitTimeSeconds,
                        allParticipantsCompleted, isTimeout, hasMinParticipants, shouldTrigger);
            }

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
        FederatedAlgorithm algorithm = task.getAlgorithm();

        log.info("执行聚合计算: 任务ID={}, 算法={}, 模型数量={}",
                taskId, algorithm, localModels.size());

        try {
            // 构建任务配置参数
            Map<String, Object> taskConfig = buildTaskConfig(task, roundNumber);

            // 执行聚合
            AggregationResult result = aggregationEngine.aggregate(localModels, algorithm, taskConfig);

            if (result.isSuccess()) {
                // 保存全局模型
                GlobalModel globalModel = saveGlobalModel(task, roundNumber, result);

                // 分发全局模型
                distributeGlobalModel(task, roundNumber, result.getGlobalParameters());

                // 更新任务进度
                updateTaskProgress(taskId, roundNumber + 1);

                // 发布成功事件 - 转换Map<String, Double>为Map<String, BigDecimal>
                final Map<String, BigDecimal> metricsAsBigDecimal;
                if (result.getGlobalMetrics() != null) {
                    metricsAsBigDecimal = new HashMap<>();
                    result.getGlobalMetrics().forEach((key, value) ->
                        metricsAsBigDecimal.put(key, BigDecimal.valueOf(value)));
                } else {
                    metricsAsBigDecimal = null;
                }

                AggregationCompletedEvent successEvent = AggregationCompletedEvent.success(
                        this, taskId, roundNumber, globalModel.getId(),
                        metricsAsBigDecimal, result.getParticipantCount(),
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
     * 构建任务配置参数
     */
    private Map<String, Object> buildTaskConfig(FederatedTask task, Integer roundNumber) {
        Map<String, Object> config = new HashMap<>();

        // 基本任务信息
        config.put("taskId", task.getId());
        config.put("roundNumber", roundNumber);
        config.put("algorithm", task.getAlgorithm().getCode());

        // 任务超参数
        config.put("learningRate", task.getLearningRate());
        config.put("batchSize", task.getBatchSize());
        config.put("epochs", task.getEpochs());
        config.put("totalRounds", task.getTotalRounds());

        // 聚合配置
        config.put("minParticipants", task.getMinParticipants());

        // 模型配置
        config.put("modelType", task.getModelType());
        config.put("featureColumns", task.getFeatureColumns());
        config.put("targetColumn", task.getTargetColumn());
        config.put("testSize", task.getTestSize());
        config.put("randomState", task.getRandomState());

        return config;
    }

    /**
     * 保存全局模型到数据库
     */
    private GlobalModel saveGlobalModel(FederatedTask task, Integer roundNumber,
                                      AggregationResult result) {
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
                Double loss = result.getGlobalMetrics().get("average_loss");
                Double accuracy = result.getGlobalMetrics().get("average_accuracy");
                if (loss != null) {
                    globalModel.setGlobalLoss(BigDecimal.valueOf(loss));
                }
                if (accuracy != null) {
                    globalModel.setGlobalAccuracy(BigDecimal.valueOf(accuracy));
                }
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
                                     Map<String, Object> aggregatedModel) {
        log.info("全局模型分发将通过AggregationCompletedEvent自动触发: 任务ID={}, 轮次={}",
                task.getId(), roundNumber);
        // 分发逻辑已通过GlobalModelDistributionService的事件监听器实现
    }

    /**
     * 更新任务进度并重置参与者状态准备新轮次
     * 🔧 使用RoundStateManager确保轮次推进的原子性和一致性
     */
    @Transactional(rollbackFor = Exception.class)
    private void updateTaskProgress(String taskId, Integer nextRound) {
        try {
            log.info("🔄 开始原子性轮次推进: 任务ID={}, 目标轮次={}", taskId, nextRound);

            // 🔧 使用RoundStateManager安全推进轮次
            boolean roundAdvanced = roundStateManager.advanceRound(taskId);
            if (!roundAdvanced) {
                throw new RuntimeException("轮次推进失败: 任务ID=" + taskId);
            }

            // 🔧 验证轮次推进成功
            FederatedTask afterUpdate = federatedTasksMapper.selectTaskById(taskId);
            if (afterUpdate == null) {
                throw new RuntimeException("任务不存在: " + taskId);
            }

            Integer currentRound = afterUpdate.getCurrentRound();
            if (!nextRound.equals(currentRound)) {
                throw new RuntimeException(String.format(
                    "轮次推进验证失败: 任务ID=%s, 期望轮次=%d, 实际轮次=%s",
                    taskId, nextRound, currentRound));
            }

            // 🔧 创建新轮次的全局模型记录
            boolean modelCreated = roundStateManager.createRoundModel(taskId, nextRound);
            if (!modelCreated) {
                log.warn("新轮次模型创建失败，但继续处理: 任务ID={}, 轮次={}", taskId, nextRound);
            }

            // 🔧 统一参与者状态同步：确保所有参与者为新轮次做好准备
            int resetCount = taskParticipantsMapper.resetParticipantsForNewRound(taskId, nextRound);

            // 🔧 增强验证：确保参与者状态与任务轮次同步
            if (resetCount > 0) {
                // 验证参与者状态确实重置
                List<Map<String, Object>> participantStatus = taskParticipantsMapper.getParticipantStatusDetails(taskId);
                long trainingCount = participantStatus.stream()
                    .filter(p -> "TRAINING".equals(p.get("status")))
                    .count();

                log.info("✅ 轮次推进完成: 任务ID={}, 当前轮次={}, 重置参与者数量={}, 训练中参与者={}",
                         taskId, currentRound, resetCount, trainingCount);

                if (trainingCount != resetCount) {
                    log.warn("⚠️ 参与者状态不一致: 重置数量={}, 实际训练中数量={}", resetCount, trainingCount);
                }

                // 🔧 轮次状态验证：确保轮次状态同步
                RoundState newRoundState = roundStateManager.getCurrentRoundState(taskId);
                log.info("轮次状态同步完成: 任务ID={}, 轮次={}, 状态={}",
                        taskId, currentRound, newRoundState);

            } else {
                log.warn("⚠️ 没有参与者状态被重置: 任务ID={}, 轮次={}", taskId, nextRound);
            }

        } catch (Exception e) {
            log.error("❌ 更新任务进度失败: 任务ID={}, 目标轮次={}, 错误={}",
                     taskId, nextRound, e.getMessage(), e);
            // 重新抛出异常以触发事务回滚
            throw new RuntimeException("更新任务进度失败", e);
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
     * 获取指定任务中参与者实际完成的最新轮次
     */
    private Integer getLatestCompletedRound(String taskId) {
        try {
            return taskParticipantsMapper.getLatestCompletedRound(taskId);
        } catch (Exception e) {
            log.error("获取最新完成轮次失败: 任务ID={}, 错误={}", taskId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建聚合键（任务ID + 轮次）
     */
    private String buildAggregationKey(String taskId, Integer roundNumber) {
        return taskId + "_" + roundNumber;
    }

}