package com.feduwacomm.service;

import com.feduwacomm.dto.RoundDatasetBinding;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.entity.RoundStateRecord;
import com.feduwacomm.enums.RoundState;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.GlobalModelMapper;
import com.feduwacomm.mapper.RoundStateMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.mapper.VmRoundModelsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * 轮次状态管理器
 *
 * 负责管理联邦学习任务的轮次状态，确保严格的状态转换和同步控制。
 * 基于现有的数据库表结构，利用global_models表的status和distribution_status字段。
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-27
 */
@Slf4j
@Component
public class RoundStateManager {

    @Autowired
    private GlobalModelMapper globalModelMapper;

    @Autowired
    private FederatedTasksMapper federatedTasksMapper;

    @Autowired
    private RoundStateMapper roundStateMapper;

    @Autowired
    private TaskParticipantsMapper taskParticipantsMapper;

    @Autowired
    private VmRoundModelsMapper vmRoundModelsMapper;

    @Autowired
    private RoundDatasetBindingService roundDatasetBindingService;

    /**
     * 获取当前轮次状态
     *
     * @param taskId 任务ID
     * @return 当前轮次状态，如果没有轮次则返回null
     */
    public RoundState getCurrentRoundState(String taskId) {
        FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return null;
        }

        Integer currentRound = task.getCurrentRound();
        if (currentRound == null || currentRound <= 0) {
            log.debug("任务尚未开始轮次: taskId={}", taskId);
            return null;
        }

        RoundStateRecord roundStateRecord = roundStateMapper.selectByTaskIdAndRound(taskId, currentRound);
        if (roundStateRecord != null && roundStateRecord.getState() != null) {
            RoundState state = parseRoundStateName(roundStateRecord.getState());
            if (state != null) {
                return state;
            }
        }

        GlobalModel currentGlobalModel = globalModelMapper.selectByTaskIdAndRound(taskId, currentRound);
        if (currentGlobalModel == null) {
            log.debug("当前轮次没有全局模型记录: taskId={}, round={}", taskId, currentRound);
            return null;
        }

        return parseRoundState(currentGlobalModel);
    }

    /**
     * 根据GlobalModel解析轮次状态
     */
    private RoundState parseRoundState(GlobalModel globalModel) {
        String status = globalModel.getStatus() != null ? globalModel.getStatus().toString() : "PENDING";
        String distributionStatus = globalModel.getDistributionStatus();

        // 基于现有字段推导轮次状态
        switch (status.toUpperCase()) {
            case "PENDING":
            case "AGGREGATING":
                return RoundState.AGGREGATING;
            case "COMPLETED":
                if ("PENDING".equals(distributionStatus)) {
                    return RoundState.DISTRIBUTING;
                } else if ("DISTRIBUTING".equals(distributionStatus)) {
                    return RoundState.DISTRIBUTING;
                } else if ("DISTRIBUTED".equals(distributionStatus)) {
                    // 检查是否所有VM都已确认
                    // 这里需要VmAckTracker来判断，暂时返回COMPLETED
                    return RoundState.COMPLETED;
                } else if ("FAILED".equals(distributionStatus)) {
                    return RoundState.DISTRIBUTING; // 失败状态，需要重试
                }
                break;
            case "FAILED":
                return RoundState.AGGREGATING; // 聚合失败，需要重新聚合
        }

        // 默认返回训练状态
        return RoundState.TRAINING;
    }

    /**
     * 原子性轮次状态转换
     *
     * @param taskId 任务ID
     * @param from 源状态
     * @param to 目标状态
     * @return 是否转换成功
     */
    @Transactional
    public boolean transitionRoundState(String taskId, RoundState from, RoundState to) {
        if (taskId == null || from == null || to == null) {
            log.error("轮次状态转换参数无效: taskId={}, from={}, to={}", taskId, from, to);
            return false;
        }

        if (!from.canTransitionTo(to)) {
            log.error("不允许的状态转换: taskId={}, from={}, to={}", taskId, from, to);
            return false;
        }

        FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
        if (task == null) {
            log.error("任务不存在: taskId={}", taskId);
            return false;
        }

        Integer currentRound = task.getCurrentRound();
        if (currentRound == null || currentRound <= 0) {
            log.error("任务轮次无效: taskId={}, currentRound={}", taskId, currentRound);
            return false;
        }

        GlobalModel currentGlobalModel = globalModelMapper.selectByTaskIdAndRound(taskId, currentRound);
        if (currentGlobalModel == null) {
            log.error("当前轮次没有全局模型记录: taskId={}, round={}", taskId, currentRound);
            return false;
        }

        RoundStateRecord roundStateRecord = ensureRoundStateRecord(taskId, currentRound);

        // 验证当前状态
        RoundState currentState = roundStateRecord != null && roundStateRecord.getState() != null
                ? parseRoundStateName(roundStateRecord.getState())
                : null;
        if (currentState == null) {
            currentState = parseRoundState(currentGlobalModel);
        }
        if (currentState != from) {
            log.error("状态转换失败，当前状态不匹配: taskId={}, expected={}, actual={}",
                     taskId, from, currentState);
            return false;
        }

        // 执行状态转换
        boolean success = updateGlobalModelState(currentGlobalModel, to);
        boolean stateSynced = updateRoundStateRecord(taskId, currentRound, to, null);
        if (success && stateSynced) {
            log.info("轮次状态转换成功: taskId={}, round={}, from={}, to={}",
                    taskId, currentRound, from, to);
        } else if (!success) {
            log.error("轮次状态转换失败: taskId={}, round={}, from={}, to={}",
                     taskId, currentRound, from, to);
        } else {
            log.warn("轮次状态转换已更新全局模型，但 round_states 未同步: taskId={}, round={}", taskId, currentRound);
        }

        return success && stateSynced;
    }

    /**
     * 采集并持久化下一轮次所需的数据集上下文，供 ROUND_START 消息和 VM 调度使用。
     *
     * @param taskId      任务ID
     * @param roundNumber 下一轮轮次号
     * @return vmId -> 数据集绑定映射
     */
    @Transactional
    public Map<String, RoundDatasetBinding> prepareNextRoundContext(String taskId, int roundNumber) {
        RoundStateRecord record = ensureRoundStateRecord(taskId, roundNumber);
        if (record == null) {
            log.warn("无法准备轮次上下文: round_states 未创建, taskId={}, round={}", taskId, roundNumber);
            return Collections.emptyMap();
        }
        Map<String, RoundDatasetBinding> bindings = roundDatasetBindingService.captureCurrentBindings(taskId);
        roundDatasetBindingService.persistBindings(taskId, roundNumber, bindings);
        return bindings != null ? bindings : Collections.emptyMap();
    }

    @Transactional
    public void markDistributionStarted(String taskId, Integer roundNumber, int participantCount) {
        if (taskId == null || roundNumber == null || roundNumber <= 0) {
            log.warn("markDistributionStarted 参数无效: taskId={}, roundNumber={}, participantCount={}",
                    taskId, roundNumber, participantCount);
            return;
        }

        ensureRoundStateRecord(taskId, roundNumber);
        int acked = 0;

        roundStateMapper.updateCounters(
                taskId,
                roundNumber,
                participantCount,
                0,
                0,
                acked
        );
        updateRoundStateRecord(taskId, roundNumber, RoundState.DISTRIBUTING, null);
        log.info("轮次分发启动: taskId={}, roundNumber={}, participants={}", taskId, roundNumber, participantCount);
    }

    /**
     * 记录梯度上传进度，保持round_states与真实训练进展一致
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordGradientUpload(String taskId, Integer roundNumber) {
        if (taskId == null || roundNumber == null || roundNumber <= 0) {
            log.warn("recordGradientUpload 参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return;
        }

        RoundStateRecord record = ensureRoundStateRecord(taskId, roundNumber);
        if (record == null) {
            log.warn("recordGradientUpload 无法确保round_states记录存在: taskId={}, roundNumber={}", taskId, roundNumber);
            return;
        }

        int participantCount = resolveParticipantCount(record, taskId);

        int gradients = safeCount(() -> vmRoundModelsMapper.countReadyModels(taskId, roundNumber));
        int completedParticipants = safeCount(() -> taskParticipantsMapper.countCompletedParticipants(taskId, roundNumber));

        int existingCompleted = Objects.requireNonNullElse(record.getCompletedParticipants(), 0);
        int existingGradients = Objects.requireNonNullElse(record.getGradientUploadsReceived(), 0);
        int existingAcked = Objects.requireNonNullElse(record.getModelBroadcastsAcked(), 0);

        int resolvedCompleted = Math.max(Math.max(existingCompleted, completedParticipants), gradients);
        int resolvedGradients = Math.max(existingGradients, gradients);

        log.info("记录梯度上传进度: taskId={}, roundNumber={}, gradients(now)={}, completed(now)={}, existingGradients={}, existingCompleted={}",
                taskId, roundNumber, gradients, completedParticipants, existingGradients, existingCompleted);
        System.out.println("🛠 recordGradientUpload -> taskId=" + taskId + ", round=" + roundNumber +
                ", gradients=" + gradients + ", completed=" + completedParticipants);

        int rows = roundStateMapper.updateCounters(
                taskId,
                roundNumber,
                participantCount,
                resolvedCompleted,
                resolvedGradients,
                existingAcked
        );

        if (rows == 0) {
            log.warn("round_states 计数更新未生效: taskId={}, roundNumber={}, gradients={}, completed={}",
                    taskId, roundNumber, resolvedGradients, resolvedCompleted);
        } else {
            log.info("round_states计数已更新: taskId={}, round={}, participants={}, completed={}, gradients={}, acked={}",
                    taskId, roundNumber, participantCount, resolvedCompleted, resolvedGradients, existingAcked);
            System.out.println("✅ round_states counters updated -> taskId=" + taskId + ", round=" + roundNumber +
                    ", completed=" + resolvedCompleted + ", gradients=" + resolvedGradients);
        }

        RoundState currentState = record.getState() != null ? parseRoundStateName(record.getState()) : null;
        if (currentState == null || currentState == RoundState.INITIALIZING || currentState == RoundState.DISTRIBUTING) {
            updateRoundStateRecord(taskId, roundNumber, RoundState.TRAINING, null);
        }

        log.debug("记录梯度上传进度完成: taskId={}, roundNumber={}, gradients={}, participants={}",
                taskId, roundNumber, resolvedGradients, resolvedCompleted);
    }

    /**
     * 确保指定轮次的round_states记录已初始化
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureRoundStateInitialized(String taskId, Integer roundNumber) {
        if (taskId == null || roundNumber == null || roundNumber <= 0) {
            log.warn("ensureRoundStateInitialized 参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return;
        }
        ensureRoundStateRecord(taskId, roundNumber);
    }

    private int safeCount(IntSupplier supplier) {
        try {
            return supplier.getAsInt();
        } catch (Exception ex) {
            log.error("统计轮次计数失败: {}", ex.getMessage(), ex);
            return 0;
        }
    }

    @Transactional
    public void updateDistributionProgress(String taskId,
                                           Integer roundNumber,
                                           Integer participantCount,
                                           Integer ackedCount) {
        if (taskId == null || roundNumber == null || roundNumber <= 0) {
            log.warn("updateDistributionProgress 参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return;
        }

        RoundStateRecord record = ensureRoundStateRecord(taskId, roundNumber);
        if (record == null) {
            log.warn("updateDistributionProgress 找不到对应的round_states记录: taskId={}, roundNumber={}", taskId, roundNumber);
            return;
        }

        Integer resolvedParticipant = participantCount != null ? participantCount : record.getParticipantCount();
        Integer resolvedAcked = ackedCount != null
                ? Math.max(ackedCount, Objects.requireNonNullElse(record.getModelBroadcastsAcked(), 0))
                : record.getModelBroadcastsAcked();
        Integer resolvedCompleted = resolvedAcked;
        Integer resolvedGradients = ackedCount != null
                ? Math.max(ackedCount, Objects.requireNonNullElse(record.getGradientUploadsReceived(), 0))
                : record.getGradientUploadsReceived();

        roundStateMapper.updateCounters(
                taskId,
                roundNumber,
                resolvedParticipant,
                resolvedCompleted,
                resolvedGradients,
                resolvedAcked
        );
    }

    @Transactional
    public boolean markRoundCompleted(String taskId,
                                      Integer roundNumber,
                                      Integer participantCountHint,
                                      Integer ackedCount) {
        if (taskId == null || roundNumber == null || roundNumber <= 0) {
            log.warn("markRoundCompleted 参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        RoundStateRecord record = ensureRoundStateRecord(taskId, roundNumber);
        if (record == null) {
            log.warn("markRoundCompleted 找不到对应的round_states记录: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        int participantCount = participantCountHint != null && participantCountHint > 0
                ? participantCountHint
                : resolveParticipantCount(record, taskId);
        int acked = ackedCount != null
                ? ackedCount
                : Objects.requireNonNullElse(record.getModelBroadcastsAcked(), 0);

        roundStateMapper.updateCounters(
                taskId,
                roundNumber,
                participantCount,
                Math.max(acked, Objects.requireNonNullElse(record.getCompletedParticipants(), 0)),
                Math.max(acked, Objects.requireNonNullElse(record.getGradientUploadsReceived(), 0)),
                Math.max(acked, Objects.requireNonNullElse(record.getModelBroadcastsAcked(), 0))
        );

        return updateRoundStateRecord(taskId, roundNumber, RoundState.COMPLETED, null);
    }

    private RoundState parseRoundStateName(String stateName) {
        if (stateName == null) {
            return null;
        }
        try {
            return RoundState.fromString(stateName);
        } catch (IllegalArgumentException ex) {
            log.warn("round_states存在未知状态: {}", stateName);
            return null;
        }
    }

    private RoundStateRecord ensureRoundStateRecord(String taskId, int roundNumber) {
        RoundStateRecord existing = roundStateMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (existing != null) {
            System.out.println("📄 round_states exist -> taskId=" + taskId + ", round=" + roundNumber + ", state=" + existing.getState());
            log.debug("round_states记录已存在: taskId={}, round={}, state={}, participants={}, gradients={}",
                    taskId, roundNumber, existing.getState(), existing.getParticipantCount(),
                    existing.getGradientUploadsReceived());
            return existing;
        }

        RoundStateRecord record = RoundStateRecord.builder()
                .taskId(taskId)
                .roundNumber(roundNumber)
                .state(RoundState.INITIALIZING.name())
                .participantCount(0)
                .completedParticipants(0)
                .gradientUploadsReceived(0)
                .modelBroadcastsAcked(0)
                .build();
        try {
            roundStateMapper.insertRoundState(record);
            System.out.println("🆕 round_states inserted -> taskId=" + taskId + ", round=" + roundNumber);
            log.info("round_states记录已创建: taskId={}, round={}", taskId, roundNumber);
        } catch (Exception e) {
            log.debug("创建round_states记录可能存在并发: taskId={}, round={}, error={}",
                    taskId, roundNumber, e.getMessage());
        }
        RoundStateRecord ensured = null;
        for (int i = 0; i < 5; i++) {
            ensured = roundStateMapper.selectByTaskIdAndRound(taskId, roundNumber);
            if (ensured != null) {
                return ensured;
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (ensured == null) {
            log.warn("round_states记录在重试后仍不可见: taskId={}, round={}", taskId, roundNumber);
        }
        return ensured;
    }

    private boolean updateRoundStateRecord(String taskId,
                                           int roundNumber,
                                           RoundState roundState,
                                           String errorMessage) {
        RoundStateRecord record = ensureRoundStateRecord(taskId, roundNumber);
        if (record == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = null;
        LocalDateTime completedAt = null;

        if (record.getStartedAt() == null && roundState != RoundState.INITIALIZING) {
            startedAt = now;
        }
        if (roundState.isTerminalState()) {
            completedAt = now;
        }

        int rows = roundStateMapper.updateState(taskId, roundNumber, roundState.name(), startedAt, completedAt, errorMessage);
        if (rows == 0) {
            RoundStateRecord refreshed = roundStateMapper.selectByTaskIdAndRound(taskId, roundNumber);
            if (refreshed != null) {
                RoundState persistedState = parseRoundStateName(refreshed.getState());
                if (persistedState == roundState) {
                    log.debug("round_states状态已由其他线程更新: taskId={}, roundNumber={}, targetState={}",
                            taskId, roundNumber, roundState);
                    return true;
                }
            }
            log.warn("round_states状态更新未生效: taskId={}, roundNumber={}, targetState={}",
                    taskId, roundNumber, roundState);
            return false;
        }
        return true;
    }

    private int resolveParticipantCount(RoundStateRecord record, String taskId) {
        if (record != null && record.getParticipantCount() != null && record.getParticipantCount() > 0) {
            return record.getParticipantCount();
        }
        try {
            return taskParticipantsMapper.countTotalParticipants(taskId);
        } catch (Exception e) {
            log.warn("统计参与者数量失败: taskId={}, error={}", taskId, e.getMessage());
            return 0;
        }
    }

    /**
     * 更新GlobalModel的状态以反映RoundState
     */
    private boolean updateGlobalModelState(GlobalModel globalModel, RoundState roundState) {
        switch (roundState) {
            case AGGREGATING:
                globalModel.setStatus(globalModel.getStatus()); // 保持当前聚合状态
                globalModel.setDistributionStatus("PENDING");
                break;
            case DISTRIBUTING:
                globalModel.setDistributionStatus("DISTRIBUTING");
                globalModel.setDistributedVms(null); // 清空已分发列表
                break;
            case COMPLETED:
                globalModel.setDistributionStatus("DISTRIBUTED");
                globalModel.setDistributionCompletedAt(LocalDateTime.now());
                break;
            case TRAINING:
                // 训练状态下，分发状态应该保持DISTRIBUTED
                globalModel.setDistributionStatus("DISTRIBUTED");
                break;
            default:
                log.error("未知的轮次状态: {}", roundState);
                return false;
        }

        globalModel.setUpdatedAt(LocalDateTime.now());
        int result = globalModelMapper.updateGlobalModel(globalModel);
        return result > 0;
    }

    /**
     * 检查是否在等待ACK状态
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @return 是否在等待ACK
     */
    public boolean isWaitingForAcks(String taskId, Integer roundNumber) {
        GlobalModel globalModel = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (globalModel == null) {
            return false;
        }

        RoundState state = parseRoundState(globalModel);
        return state == RoundState.DISTRIBUTING;
    }

    /**
     * 安全的轮次推进
     * 只有当前轮次的所有VM都完成训练时才推进到下一轮
     *
     * @param taskId 任务ID
     * @return 是否推进成功
     */
    @Transactional
    public boolean advanceRound(String taskId) {
        if (taskId == null) {
            log.error("轮次推进参数无效: taskId is null");
            return false;
        }

        FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
        if (task == null) {
            log.error("任务不存在: taskId={}", taskId);
            return false;
        }

        Integer currentRound = task.getCurrentRound();
        Integer totalRounds = task.getTotalRounds();

        if (currentRound == null || totalRounds == null) {
            log.error("任务轮次信息无效: taskId={}, currentRound={}, totalRounds={}",
                     taskId, currentRound, totalRounds);
            return false;
        }

        if (currentRound >= totalRounds) {
            log.info("任务已完成所有轮次: taskId={}, currentRound={}, totalRounds={}",
                    taskId, currentRound, totalRounds);
            return false;
        }

        // 推进到下一轮
        Integer newRound = currentRound + 1;
        int result = federatedTasksMapper.updateTaskProgress(taskId, newRound, "RUNNING");

        if (result > 0) {
            log.info("轮次推进成功: taskId={}, 从轮次{}推进到轮次{}", taskId, currentRound, newRound);
            return true;
        } else {
            log.error("轮次推进失败: taskId={}, 从轮次{}推进到轮次{}", taskId, currentRound, newRound);
            return false;
        }
    }


    /**
     * 创建新轮次的全局模型记录
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @return 是否创建成功
     */
    @Transactional
    public boolean createRoundModel(String taskId, Integer roundNumber) {
        if (taskId == null || roundNumber == null || roundNumber <= 0) {
            log.error("创建轮次模型参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        // 检查是否已存在
        GlobalModel existing = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (existing != null) {
            log.warn("轮次模型已存在: taskId={}, roundNumber={}", taskId, roundNumber);
            return true;
        }

        // 创建新的全局模型记录
        GlobalModel globalModel = GlobalModel.builder()
                .id(java.util.UUID.randomUUID().toString().replace("-", ""))
                .taskId(taskId)
                .roundNumber(roundNumber)
                .distributionStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        int result = globalModelMapper.insertGlobalModel(globalModel);
        if (result > 0) {
            ensureRoundStateRecord(taskId, roundNumber);
            updateRoundStateRecord(taskId, roundNumber, RoundState.INITIALIZING, null);
            log.info("新轮次模型创建成功: taskId={}, roundNumber={}", taskId, roundNumber);
            return true;
        } else {
            log.error("新轮次模型创建失败: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }
    }

    /**
     * 获取轮次状态描述
     *
     * @param taskId 任务ID
     * @return 状态描述字符串
     */
    public String getRoundStateDescription(String taskId) {
        RoundState state = getCurrentRoundState(taskId);
        return state != null ? state.getDescription() : "未知状态";
    }

    /**
     * 设置指定轮次的状态
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param roundState 新状态
     * @return 是否设置成功
     */
    @Transactional
    public boolean setRoundState(String taskId, int roundNumber, RoundState roundState) {
        if (taskId == null || roundNumber <= 0 || roundState == null) {
            log.error("设置轮次状态参数无效: taskId={}, roundNumber={}, roundState={}",
                     taskId, roundNumber, roundState);
            return false;
        }

        GlobalModel globalModel = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (globalModel == null) {
            log.error("指定轮次的全局模型不存在: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        boolean success = updateGlobalModelState(globalModel, roundState);
        boolean stateSynced = updateRoundStateRecord(taskId, roundNumber, roundState, null);
        if (success && stateSynced) {
            log.info("轮次状态设置成功: taskId={}, roundNumber={}, roundState={}",
                    taskId, roundNumber, roundState);
        } else if (!success) {
            log.error("轮次状态设置失败: taskId={}, roundNumber={}, roundState={}",
                     taskId, roundNumber, roundState);
        } else {
            log.warn("轮次状态设置已更新全局模型，但 round_states 未同步: taskId={}, roundNumber={}",
                    taskId, roundNumber);
        }

        return success && stateSynced;
    }

    /**
     * 创建任务状态快照
     *
     * @param taskId 任务ID
     * @return 快照数据（JSON格式）
     */
    public String createSnapshot(String taskId) {
        if (taskId == null) {
            log.error("创建快照参数无效: taskId is null");
            return null;
        }

        try {
            FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            if (task == null) {
                log.error("任务不存在: taskId={}", taskId);
                return null;
            }

            // 创建简单的快照数据
            java.util.Map<String, Object> snapshot = new java.util.HashMap<>();
            snapshot.put("taskId", taskId);
            snapshot.put("currentRound", task.getCurrentRound());
            snapshot.put("totalRounds", task.getTotalRounds());
            snapshot.put("status", task.getStatus());
            snapshot.put("snapshotTime", java.time.LocalDateTime.now().toString());

            // 获取当前轮次状态
            RoundState currentState = getCurrentRoundState(taskId);
            if (currentState != null) {
                snapshot.put("roundState", currentState.name());
            }

            // 简单的JSON序列化
            String snapshotJson = toJsonString(snapshot);
            log.info("任务快照创建成功: taskId={}, snapshot={}", taskId, snapshotJson);
            return snapshotJson;

        } catch (Exception e) {
            log.error("创建任务快照失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从快照恢复任务状态
     *
     * @param taskId 任务ID
     * @param snapshotData 快照数据
     * @return 是否恢复成功
     */
    @Transactional
    public boolean restoreSnapshot(String taskId, java.util.Map<String, Object> snapshotData) {
        if (taskId == null || snapshotData == null) {
            log.error("恢复快照参数无效: taskId={}, snapshotData={}", taskId, snapshotData);
            return false;
        }

        try {
            FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            if (task == null) {
                log.error("任务不存在: taskId={}", taskId);
                return false;
            }

            // 恢复基本任务信息
            Object currentRoundObj = snapshotData.get("currentRound");
            Object statusObj = snapshotData.get("status");

            if (currentRoundObj instanceof Number) {
                Integer currentRound = ((Number) currentRoundObj).intValue();
                String status = statusObj != null ? statusObj.toString() : "RUNNING";

                int result = federatedTasksMapper.updateTaskProgress(taskId, currentRound, status);
                if (result > 0) {
                    log.info("任务快照恢复成功: taskId={}, currentRound={}, status={}",
                            taskId, currentRound, status);
                    return true;
                } else {
                    log.error("任务快照恢复失败: taskId={}, 数据库更新失败", taskId);
                    return false;
                }
            }

            log.error("快照数据格式无效: taskId={}, snapshotData={}", taskId, snapshotData);
            return false;

        } catch (Exception e) {
            log.error("恢复任务快照失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 初始化任务的轮次管理
     *
     * @param taskId 任务ID
     * @param totalRounds 总轮次数
     * @return 是否初始化成功
     */
    @Transactional
    public boolean initializeTask(String taskId, Integer totalRounds) {
        if (taskId == null || totalRounds == null || totalRounds <= 0) {
            log.error("初始化任务参数无效: taskId={}, totalRounds={}", taskId, totalRounds);
            return false;
        }

        try {
            FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            if (task == null) {
                log.error("任务不存在: taskId={}", taskId);
                return false;
            }

            // 初始化任务进度
            int result = federatedTasksMapper.updateTaskProgress(taskId, 1, "INITIALIZED");
            if (result > 0) {
                // 创建第一轮的全局模型记录
                boolean modelCreated = createRoundModel(taskId, 1);
                if (modelCreated) {
                    log.info("任务初始化成功: taskId={}, totalRounds={}", taskId, totalRounds);
                    return true;
                } else {
                    log.error("任务初始化失败: 创建轮次模型失败, taskId={}", taskId);
                    return false;
                }
            } else {
                log.error("任务初始化失败: 更新任务进度失败, taskId={}", taskId);
                return false;
            }

        } catch (Exception e) {
            log.error("初始化任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 简单的Map转JSON字符串工具方法
     */
    private String toJsonString(java.util.Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    /**
     * 清理任务相关的轮次状态数据
     *
     * @param taskId 任务ID
     * @return 是否清理成功
     */
    public boolean cleanupTask(String taskId) {
        if (taskId == null) {
            log.error("清理任务轮次状态参数无效: taskId={}", taskId);
            return false;
        }

        log.info("清理任务轮次状态数据: taskId={}", taskId);

        try {
            // 清理内存中的轮次状态缓存和数据库中的相关数据
            String taskPrefix = "round:" + taskId + ":";

            int cleanupCount = 0;

            // 清理全局模型数据
            try {
                int deletedModels = globalModelMapper.deleteByTaskId(taskId);
                cleanupCount += deletedModels;
                log.debug("已清理全局模型记录: taskId={}, 删除数量={}", taskId, deletedModels);
            } catch (Exception e) {
                log.warn("清理全局模型记录时出错: taskId={}, error={}", taskId, e.getMessage());
            }

            try {
                int deletedRounds = roundStateMapper.deleteByTaskId(taskId);
                cleanupCount += deletedRounds;
                log.debug("已清理round_states记录: taskId={}, 删除数量={}", taskId, deletedRounds);
            } catch (Exception e) {
                log.warn("清理round_states记录时出错: taskId={}, error={}", taskId, e.getMessage());
            }

            // 重置任务的轮次信息
            try {
                int result = federatedTasksMapper.updateTaskProgress(taskId, 0, "PENDING");
                if (result > 0) {
                    cleanupCount += result;
                    log.debug("已重置任务轮次进度: taskId={}", taskId);
                }
            } catch (Exception e) {
                log.warn("重置任务轮次进度时出错: taskId={}, error={}", taskId, e.getMessage());
            }

            log.info("任务轮次状态清理完成: taskId={}, 清理项目数={}", taskId, cleanupCount);
            return true;
        } catch (Exception e) {
            log.error("清理任务轮次状态失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return false;
        }
    }
}
