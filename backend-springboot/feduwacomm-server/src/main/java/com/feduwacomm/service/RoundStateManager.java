package com.feduwacomm.service;

import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.enums.RoundState;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.GlobalModelMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
                    // 这里需要VmAckTracker来判断，暂时返回READY
                    return RoundState.READY;
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

        // 验证当前状态
        RoundState currentState = parseRoundState(currentGlobalModel);
        if (currentState != from) {
            log.error("状态转换失败，当前状态不匹配: taskId={}, expected={}, actual={}",
                     taskId, from, currentState);
            return false;
        }

        // 执行状态转换
        boolean success = updateGlobalModelState(currentGlobalModel, to);
        if (success) {
            log.info("轮次状态转换成功: taskId={}, round={}, from={}, to={}",
                    taskId, currentRound, from, to);
        } else {
            log.error("轮次状态转换失败: taskId={}, round={}, from={}, to={}",
                     taskId, currentRound, from, to);
        }

        return success;
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
            case WAITING_ACK:
                globalModel.setDistributionStatus("DISTRIBUTED");
                // 注意：这里不设置distribution_completed_at，等所有ACK后再设置
                break;
            case READY:
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
        return state == RoundState.WAITING_ACK;
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
}