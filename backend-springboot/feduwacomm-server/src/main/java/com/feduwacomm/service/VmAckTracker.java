package com.feduwacomm.service;

import com.feduwacomm.entity.ModelDistribution;
import com.feduwacomm.mapper.GlobalModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VM确认跟踪器
 *
 * 负责跟踪和管理VM对GLOBAL_MODEL_BROADCAST消息的确认状态。
 * 基于model_distributions表记录每个VM的模型接收确认状态，
 * 确保所有VM都确认收到模型后才能进入下一阶段。
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-27
 */
@Slf4j
@Component
public class VmAckTracker {

    @Autowired
    private ModelDistributionMapper modelDistributionMapper;

    @Autowired
    private GlobalModelMapper globalModelMapper;

    @Autowired
    private TaskParticipantsMapper taskParticipantsMapper;

    /**
     * 记录VM发送的GLOBAL_MODEL_BROADCAST_ACK
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param roundNumber 轮次号
     * @return 是否记录成功
     */
    @Transactional
    public boolean recordAck(String taskId, String vmId, Integer roundNumber) {
        if (taskId == null || vmId == null || roundNumber == null) {
            log.error("记录ACK参数无效: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
            return false;
        }

        // 查找当前轮次的全局模型
        var globalModel = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (globalModel == null) {
            log.error("未找到对应的全局模型: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        // 查找对应的分发记录
        ModelDistribution distribution = modelDistributionMapper
                .selectByModelIdAndVmId(globalModel.getId(), vmId);

        if (distribution == null) {
            log.warn("未找到分发记录，创建新记录: modelId={}, vmId={}", globalModel.getId(), vmId);
            // 创建新的分发记录
            distribution = new ModelDistribution();
            distribution.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
            distribution.setModelId(globalModel.getId());
            distribution.setVmId(vmId);
            distribution.setDistributionStatus("COMPLETED");
            distribution.setDistributedAt(LocalDateTime.now());
            distribution.setVerifiedAt(LocalDateTime.now());
            distribution.setChecksumVerified(true);
            distribution.setCreatedAt(LocalDateTime.now());

            int result = modelDistributionMapper.insertModelDistribution(distribution);
            if (result > 0) {
                log.info("VM ACK记录成功: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
                return true;
            } else {
                log.error("VM ACK记录失败: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
                return false;
            }
        } else {
            // 更新现有记录
            int result = modelDistributionMapper.updateDistributionStatus(
                    distribution.getId(),
                    "COMPLETED",
                    LocalDateTime.now(),
                    null
            );

            if (result > 0) {
                // 同时更新验证状态
                modelDistributionMapper.updateVerificationStatus(
                        distribution.getId(),
                        true,
                        LocalDateTime.now()
                );
                log.info("VM ACK更新成功: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
                return true;
            } else {
                log.error("VM ACK更新失败: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
                return false;
            }
        }
    }

    /**
     * 检查所有VM是否都已确认
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @return 是否所有VM都已确认
     */
    public boolean allVmsAcked(String taskId, Integer roundNumber) {
        if (taskId == null || roundNumber == null) {
            log.error("检查ACK参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        // 获取任务的所有活跃参与者
        List<String> activeVmIds = taskParticipantsMapper.selectParticipantsByTaskId(taskId)
                .stream()
                .filter(participant -> !"FAILED".equals(participant.getStatus()) &&
                                     !"DISCONNECTED".equals(participant.getStatus()))
                .map(participant -> participant.getVmId())
                .collect(Collectors.toList());

        if (activeVmIds.isEmpty()) {
            log.warn("任务没有活跃的参与者: taskId={}", taskId);
            return false;
        }

        // 查找当前轮次的全局模型
        var globalModel = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (globalModel == null) {
            log.error("未找到对应的全局模型: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        // 统计已确认的VM数量
        Map<String, Object> progress = modelDistributionMapper.getDistributionProgress(globalModel.getId());
        Long completedCount = (Long) progress.get("completed");
        Long totalCount = (Long) progress.get("total");

        if (completedCount == null || totalCount == null) {
            log.debug("没有分发记录: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        boolean allAcked = completedCount.intValue() >= activeVmIds.size();

        log.debug("VM ACK状态检查: taskId={}, roundNumber={}, 已确认={}, 活跃VM数={}, 总分发记录={}, 全部确认={}",
                 taskId, roundNumber, completedCount, activeVmIds.size(), totalCount, allAcked);

        return allAcked;
    }

    /**
     * 获取未确认的VM列表
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @return 未确认的VM ID列表
     */
    public List<String> getPendingVms(String taskId, Integer roundNumber) {
        if (taskId == null || roundNumber == null) {
            log.error("获取待确认VM参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return List.of();
        }

        // 获取任务的所有活跃参与者
        List<String> activeVmIds = taskParticipantsMapper.selectParticipantsByTaskId(taskId)
                .stream()
                .filter(participant -> !"FAILED".equals(participant.getStatus()) &&
                                     !"DISCONNECTED".equals(participant.getStatus()))
                .map(participant -> participant.getVmId())
                .collect(Collectors.toList());

        if (activeVmIds.isEmpty()) {
            log.debug("任务没有活跃的参与者: taskId={}", taskId);
            return List.of();
        }

        // 查找当前轮次的全局模型
        var globalModel = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (globalModel == null) {
            log.error("未找到对应的全局模型: taskId={}, roundNumber={}", taskId, roundNumber);
            return activeVmIds; // 返回所有VM作为待确认
        }

        // 获取已确认的VM列表
        List<Map<String, Object>> distributionsWithVm = modelDistributionMapper
                .selectByModelIdWithVmInfo(globalModel.getId());

        List<String> ackedVmIds = distributionsWithVm.stream()
                .filter(record -> "COMPLETED".equals(record.get("distribution_status")))
                .map(record -> (String) record.get("vm_id"))
                .collect(Collectors.toList());

        // 返回未确认的VM列表
        List<String> pendingVmIds = activeVmIds.stream()
                .filter(vmId -> !ackedVmIds.contains(vmId))
                .collect(Collectors.toList());

        log.debug("未确认VM列表: taskId={}, roundNumber={}, 总VM数={}, 已确认={}, 待确认={}",
                 taskId, roundNumber, activeVmIds.size(), ackedVmIds.size(), pendingVmIds.size());

        return pendingVmIds;
    }

    /**
     * 处理ACK超时
     * 对超时未确认的VM进行处理，可能包括重新发送或标记失败
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param timeoutMinutes 超时分钟数
     */
    @Transactional
    public void handleAckTimeout(String taskId, Integer roundNumber, int timeoutMinutes) {
        if (taskId == null || roundNumber == null) {
            log.error("处理ACK超时参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return;
        }

        // 查找当前轮次的全局模型
        var globalModel = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (globalModel == null) {
            log.error("未找到对应的全局模型: taskId={}, roundNumber={}", taskId, roundNumber);
            return;
        }

        // 查找超时的分发记录
        List<ModelDistribution> timeoutDistributions = modelDistributionMapper
                .selectTimeoutDistributions(timeoutMinutes);

        List<ModelDistribution> relevantTimeouts = timeoutDistributions.stream()
                .filter(dist -> dist.getModelId().equals(globalModel.getId()))
                .collect(Collectors.toList());

        if (relevantTimeouts.isEmpty()) {
            log.debug("没有超时的分发记录: taskId={}, roundNumber={}", taskId, roundNumber);
            return;
        }

        log.warn("发现{}个超时的ACK: taskId={}, roundNumber={}, 超时阈值={}分钟",
                relevantTimeouts.size(), taskId, roundNumber, timeoutMinutes);

        // 对于超时的分发记录，标记为失败
        for (ModelDistribution distribution : relevantTimeouts) {
            modelDistributionMapper.updateDistributionStatus(
                    distribution.getId(),
                    "FAILED",
                    LocalDateTime.now(),
                    "ACK超时: " + timeoutMinutes + "分钟"
            );

            log.warn("VM ACK超时，标记为失败: vmId={}, modelId={}, taskId={}, roundNumber={}",
                    distribution.getVmId(), distribution.getModelId(), taskId, roundNumber);
        }
    }

    /**
     * 初始化轮次的分发记录
     * 为所有活跃的VM创建分发记录，状态为PENDING
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @return 是否初始化成功
     */
    @Transactional
    public boolean initializeRoundDistributions(String taskId, Integer roundNumber) {
        if (taskId == null || roundNumber == null) {
            log.error("初始化分发记录参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        // 获取任务的所有活跃参与者
        List<String> activeVmIds = taskParticipantsMapper.selectParticipantsByTaskId(taskId)
                .stream()
                .filter(participant -> !"FAILED".equals(participant.getStatus()) &&
                                     !"DISCONNECTED".equals(participant.getStatus()))
                .map(participant -> participant.getVmId())
                .collect(Collectors.toList());

        if (activeVmIds.isEmpty()) {
            log.warn("任务没有活跃的参与者: taskId={}", taskId);
            return false;
        }

        // 查找当前轮次的全局模型
        var globalModel = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (globalModel == null) {
            log.error("未找到对应的全局模型: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        // 创建分发记录
        List<ModelDistribution> distributions = activeVmIds.stream()
                .map(vmId -> {
                    ModelDistribution distribution = new ModelDistribution();
                    distribution.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
                    distribution.setModelId(globalModel.getId());
                    distribution.setVmId(vmId);
                    distribution.setDistributionStatus("PENDING");
                    distribution.setChecksumVerified(false);
                    distribution.setCreatedAt(LocalDateTime.now());
                    return distribution;
                })
                .collect(Collectors.toList());

        int result = modelDistributionMapper.batchInsertModelDistributions(distributions);

        if (result > 0) {
            log.info("轮次分发记录初始化成功: taskId={}, roundNumber={}, VM数量={}",
                    taskId, roundNumber, activeVmIds.size());
            return true;
        } else {
            log.error("轮次分发记录初始化失败: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }
    }

    /**
     * 获取ACK进度统计
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @return ACK进度信息
     */
    public Map<String, Object> getAckProgress(String taskId, Integer roundNumber) {
        if (taskId == null || roundNumber == null) {
            log.error("获取ACK进度参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return Map.of();
        }

        // 查找当前轮次的全局模型
        var globalModel = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (globalModel == null) {
            log.error("未找到对应的全局模型: taskId={}, roundNumber={}", taskId, roundNumber);
            return Map.of();
        }

        return modelDistributionMapper.getDistributionProgress(globalModel.getId());
    }

    /**
     * 重置轮次的ACK状态
     * 用于重新分发模型时清除之前的ACK记录
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @return 是否重置成功
     */
    @Transactional
    public boolean resetRoundAcks(String taskId, Integer roundNumber) {
        if (taskId == null || roundNumber == null) {
            log.error("重置ACK状态参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        // 查找当前轮次的全局模型
        var globalModel = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (globalModel == null) {
            log.error("未找到对应的全局模型: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        int result = modelDistributionMapper.resetDistributionStatus(globalModel.getId());

        if (result > 0) {
            log.info("轮次ACK状态重置成功: taskId={}, roundNumber={}, 重置记录数={}",
                    taskId, roundNumber, result);
            return true;
        } else {
            log.warn("轮次ACK状态重置完成，但没有记录被重置: taskId={}, roundNumber={}",
                    taskId, roundNumber);
            return true; // 没有记录也算成功
        }
    }
}