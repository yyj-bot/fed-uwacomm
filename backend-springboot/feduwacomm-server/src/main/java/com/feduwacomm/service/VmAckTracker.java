package com.feduwacomm.service;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.entity.InitialModel;
import com.feduwacomm.entity.ModelDistribution;
import com.feduwacomm.entity.VmAckTracking;
import com.feduwacomm.enums.InitialModelStatus;
import com.feduwacomm.enums.ParticipantStatus;
import com.feduwacomm.enums.RoundState;
import com.feduwacomm.mapper.GlobalModelMapper;
import com.feduwacomm.mapper.InitialModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.mapper.VmAckTrackingMapper;
import com.feduwacomm.service.AckCacheService;
import com.feduwacomm.service.RoundStateManager;
import com.feduwacomm.service.WebSocketMessageSender;
import com.feduwacomm.service.cache.model.AckProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private InitialModelMapper initialModelMapper;

    @Autowired
    private TaskParticipantsMapper taskParticipantsMapper;

    @Autowired
    private VmAckTrackingMapper vmAckTrackingMapper;

    @Autowired
    private AckCacheService ackCacheService;

    @Autowired
    private RoundStateManager roundStateManager;

    @Autowired
    private WebSocketMessageSender messageSender;

    @org.springframework.beans.factory.annotation.Value("${federated.datasetAck.timeoutSeconds:120}")
    private int datasetAckTimeoutSeconds;

    /**
     * 记录VM发送的GLOBAL_MODEL_BROADCAST_ACK（重构版本 - 数据库+缓存双写）
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

        try {
            // 1. 数据库操作 - 保持原有的模型分发记录逻辑
            boolean dbResult = recordAckToDatabase(taskId, vmId, roundNumber);
            if (!dbResult) {
                log.error("数据库ACK记录失败: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
                return false;
            }

            // 2. 缓存操作 - 更新ACK状态缓存
            try {
                ackCacheService.updateAckStatus(taskId, vmId, VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST,
                        VmAckTracking.AckStatus.SUCCESS);

                // 更新进度缓存
                ackCacheService.updateAckProgress(taskId, VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST, roundNumber);

                if (roundNumber != null) {
                    updateRoundStateByAckProgress(taskId, roundNumber);
                }

                log.info("VM ACK双写成功: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
                return true;

            } catch (Exception cacheException) {
                log.warn("缓存更新失败，但数据库操作成功: taskId={}, vmId={}, error={}",
                        taskId, vmId, cacheException.getMessage());
                // 缓存失败不影响整体结果，因为数据库已成功
                if (roundNumber != null) {
                    updateRoundStateByAckProgress(taskId, roundNumber);
                }
                return true;
            }

        } catch (Exception e) {
            log.error("记录ACK时发生异常: taskId={}, vmId={}, roundNumber={}, error={}",
                    taskId, vmId, roundNumber, e.getMessage(), e);

            // 异常情况下清理可能的缓存状态
            try {
                ackCacheService.clearAckTypeCache(taskId, VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST);
            } catch (Exception cleanupEx) {
                log.warn("清理缓存失败: {}", cleanupEx.getMessage());
            }

            return false;
        }
    }

    private AckProgress updateRoundStateByAckProgress(String taskId, Integer roundNumber) {
        try {
            if (roundNumber == null) {
                return null;
            }

            AckProgress progress = ackCacheService.getAckProgress(taskId, VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST);
            if (progress == null) {
                return null;
            }

            List<String> activeVmIds = taskParticipantsMapper.selectParticipantsByTaskId(taskId)
                    .stream()
                    .filter(participant -> participant != null)
                    .filter(participant -> StringUtils.hasText(participant.getVmId()))
                    .filter(participant -> participant.getStatus() != ParticipantStatus.FAILED &&
                            participant.getStatus() != ParticipantStatus.DISCONNECTED)
                    .map(participant -> participant.getVmId())
                    .collect(Collectors.toList());

            int total = activeVmIds.size();
            int completed = progress.getSuccessVmCount();

            roundStateManager.updateDistributionProgress(taskId, roundNumber, total, completed);
            if (total > 0 && completed >= total) {
                boolean updated = roundStateManager.markRoundCompleted(taskId, roundNumber, total, completed);
                log.info("轮次ACK进度触发状态更新: taskId={}, round={}, total={}, completed={}, result={}",
                        taskId, roundNumber, total, completed, updated);
                if (updated) {
                    triggerRoundCompleteFallback(taskId, roundNumber);
                }
            }
            return progress;
        } catch (Exception ex) {
            log.warn("根据ACK缓存更新轮次状态失败: taskId={}, round={}, error={}",
                    taskId, roundNumber, ex.getMessage());
            return null;
        }
    }

    /**
     * 数据库ACK记录操作（从原方法提取）
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param roundNumber 轮次号
     * @return 是否成功
     */
    private boolean recordAckToDatabase(String taskId, String vmId, Integer roundNumber) {
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
            log.debug("轮次{}未找到模型分发记录，使用缓存追踪ACK: taskId={}, vmId={}, modelId={}",
                    roundNumber, taskId, vmId, globalModel.getId());
            return true;
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
                log.debug("数据库ACK更新成功: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
                return true;
            } else {
                log.error("数据库ACK更新失败: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
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

    // ==================== v1.4协议任务级别ACK跟踪方法 ====================

    /**
     * 记录任务启动确认（重构版本 - 数据库+缓存双写）
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param status 确认状态
     * @return 是否记录成功
     */
    @Transactional
    public boolean recordTaskStartAck(String taskId, String vmId, String status, Map<String, Object> ackData) {
        if (taskId == null || vmId == null || status == null) {
            log.error("记录任务启动ACK参数无效: taskId={}, vmId={}, status={}", taskId, vmId, status);
            return false;
        }

        log.info("记录任务启动确认: taskId={}, vmId={}, status={}", taskId, vmId, status);

        try {
            boolean successStatus = "SUCCESS".equalsIgnoreCase(status) || "READY".equalsIgnoreCase(status);

            // 1. 数据库操作
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(null) // 任务级别确认
                .ackType(VmAckTracking.AckType.TASK_START)
                .status(successStatus ? VmAckTracking.AckStatus.SUCCESS : VmAckTracking.AckStatus.FAILED)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            boolean success = result > 0 && successStatus;

            if (!success) {
                log.warn("任务启动确认记录失败或状态非成功: taskId={}, vmId={}, status={}", taskId, vmId, status);
                return false;
            }

            // 2. 缓存操作
            try {
                VmAckTracking.AckStatus ackStatus = successStatus ?
                    VmAckTracking.AckStatus.SUCCESS : VmAckTracking.AckStatus.FAILED;

                ackCacheService.updateAckStatus(taskId, vmId, VmAckTracking.AckType.TASK_START, ackStatus);
                ackCacheService.updateAckProgress(taskId, VmAckTracking.AckType.TASK_START);

                log.info("任务启动确认双写成功: taskId={}, vmId={}, ackId={}", taskId, vmId, ackTracking.getId());

                if (ackStatus == VmAckTracking.AckStatus.SUCCESS) {
                    handleInitialModelAck(taskId, vmId, ackData, true);
                }
                return true;

            } catch (Exception cacheException) {
                log.warn("缓存更新失败，但数据库操作成功: taskId={}, vmId={}, error={}",
                        taskId, vmId, cacheException.getMessage());
                if (successStatus) {
                    handleInitialModelAck(taskId, vmId, ackData, true);
                }
                return true; // 缓存失败不影响整体结果
            }

        } catch (Exception e) {
            log.error("记录任务启动确认失败: taskId={}, vmId={}, status={}", taskId, vmId, status, e);
            return false;
        }
    }

    /**
     * 处理初始模型接收ACK（v1.5协议）
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param ackData ACK携带的数据
     * @param success 是否成功
     */
    public void processInitialModelAck(String taskId,
                                       String vmId,
                                       Map<String, Object> ackData,
                                       boolean success) {
        handleInitialModelAck(taskId, vmId, ackData, success);
    }

    private void handleInitialModelAck(String taskId,
                                       String vmId,
                                       Map<String, Object> ackData,
                                       boolean success) {
        String distributionId = extractDistributionId(ackData);
        Integer ackRound = extractRoundNumber(ackData);
        boolean initialFlow = ackRound == null || ackRound <= 0;

        InitialModel initialModel = null;
        GlobalModel globalModel = null;
        String modelIdForAck = null;

        if (initialFlow) {
            initialModel = initialModelMapper.selectByTaskId(taskId);
            if (initialModel == null) {
                log.debug("任务未绑定初始模型，跳过分发状态更新: taskId={}", taskId);
                return;
            }
            modelIdForAck = initialModel.getId();
        } else {
            globalModel = globalModelMapper.selectByTaskIdAndRound(taskId, ackRound);
            if (globalModel != null) {
                modelIdForAck = globalModel.getId();
            } else {
                log.warn("未找到轮次{}的全局模型，跳过ACK处理: taskId={}, vmId={}", ackRound, taskId, vmId);
                return;
            }
        }

        if (!StringUtils.hasText(modelIdForAck)) {
            log.warn("ACK无法关联任何模型ID，跳过: taskId={}, vmId={}", taskId, vmId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        log.info("处理模型ACK: taskId={}, vmId={}, round={}, success={}, distributionId={}, modelId={}",
                taskId, vmId, ackRound, success, distributionId, modelIdForAck);

        if (!initialFlow) {
            VmAckTracking.AckStatus ackStatus = success ?
                    VmAckTracking.AckStatus.SUCCESS : VmAckTracking.AckStatus.FAILED;
            try {
                ackCacheService.updateAckStatus(taskId, vmId, VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST, ackStatus);
                ackCacheService.updateAckProgress(taskId, VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST);
            } catch (Exception cacheEx) {
                log.warn("更新轮次ACK缓存失败: taskId={}, vmId={}, error={}", taskId, vmId, cacheEx.getMessage());
            }

            AckProgress progress = updateRoundStateByAckProgress(taskId, ackRound);
            int total = progress != null && progress.getTotalVms() > 0 ? progress.getTotalVms()
                    : taskParticipantsMapper.countTotalParticipants(taskId);
            int completed = progress != null ? progress.getSuccessVmCount() : 0;

            if (success && total > 0 && completed >= total) {
                GlobalModel persisted = globalModelMapper.selectById(modelIdForAck);
                String distributedVmsJson = persisted != null ? persisted.getDistributedVms() : null;
                globalModelMapper.updateDistributionStatus(
                        modelIdForAck,
                        "DISTRIBUTED",
                        distributedVmsJson,
                        now,
                        now);
                log.info("全局模型分发确认完成: taskId={}, round={}, modelId={}, total={}, completed={}",
                        taskId, ackRound, modelIdForAck, total, completed);
            } else if (!success) {
                globalModelMapper.updateDistributionStatus(
                        modelIdForAck,
                        "FAILED",
                        null,
                        null,
                        now);
                log.warn("全局模型分发存在失败: taskId={}, round={}, modelId={}, vmId={}",
                        taskId, ackRound, modelIdForAck, vmId);
            }
            return;
        }

        ModelDistribution distribution = null;
        if (StringUtils.hasText(distributionId)) {
            distribution = modelDistributionMapper.selectById(distributionId);
        }
        if (distribution == null) {
            distribution = modelDistributionMapper.selectByModelIdAndVmId(modelIdForAck, vmId);
        }
        if (distribution == null) {
            log.warn("未找到模型分发记录: modelId={}, vmId={}, taskId={}, distributionId={}",
                    modelIdForAck, vmId, taskId, distributionId);
            return;
        }
        log.info("匹配到模型分发记录: id={}, modelId={}, vmId={}, status={}",
                distribution.getId(), distribution.getModelId(), distribution.getVmId(), distribution.getDistributionStatus());

        String errorMessage = success ? null : extractErrorMessage(ackData);
        Boolean checksumVerified = extractChecksumVerified(ackData);

        int updatedRows = modelDistributionMapper.updateDistributionStatus(
                distribution.getId(),
                success ? "COMPLETED" : "FAILED",
                now,
                errorMessage);
        log.info("更新模型分发状态: id={}, success={}, updatedRows={}", distribution.getId(), success, updatedRows);
        if (success) {
            boolean verified = checksumVerified == null || Boolean.TRUE.equals(checksumVerified);
            int verificationUpdated = modelDistributionMapper.updateVerificationStatus(
                    distribution.getId(),
                    verified,
                    verified ? now : null
            );
            log.info("更新模型分发校验状态: id={}, verified={}, updatedRows={}", distribution.getId(), verified, verificationUpdated);
        } else {
            int verificationUpdated = modelDistributionMapper.updateVerificationStatus(
                    distribution.getId(),
                    Boolean.FALSE,
                    null
            );
            log.info("更新模型分发校验状态: id={}, verified=false, updatedRows={}", distribution.getId(), verificationUpdated);
        }

        List<ModelDistribution> rawDistributions = modelDistributionMapper.selectEntitiesByModelId(modelIdForAck);
        if (rawDistributions != null) {
            log.info("模型分发记录快照: taskId={}, modelId={}, statuses={}",
                    taskId, modelIdForAck,
                    rawDistributions.stream()
                            .map(record -> record.getVmId() + ":" + record.getDistributionStatus())
                            .toList());
        }
        long total = rawDistributions != null ? rawDistributions.size() : 0;
        long completed = rawDistributions != null ? rawDistributions.stream()
                .filter(record -> "COMPLETED".equalsIgnoreCase(record.getDistributionStatus()))
                .count() : 0;
        long failed = rawDistributions != null ? rawDistributions.stream()
                .filter(record -> "FAILED".equalsIgnoreCase(record.getDistributionStatus()))
                .count() : 0;
        long inProgress = rawDistributions != null ? rawDistributions.stream()
                .filter(record -> "IN_PROGRESS".equalsIgnoreCase(record.getDistributionStatus()))
                .count() : 0;
        long pending = total - completed - failed - inProgress;

        Map<String, Object> progress = new HashMap<>();
        progress.put("total", total);
        progress.put("completed", completed);
        progress.put("failed", failed);
        progress.put("inProgress", inProgress);
        progress.put("pending", pending);

        List<Map<String, Object>> statusBreakdown = modelDistributionMapper.countDistributionStatusByModelId(modelIdForAck);
        log.info("模型分发进度: taskId={}, modelId={}, progress={}, statusBreakdown={}, recordCount={}",
                taskId, modelIdForAck, progress, statusBreakdown, total);

        int totalInt = (int) Math.min(total, Integer.MAX_VALUE);
        int completedInt = (int) Math.min(completed, Integer.MAX_VALUE);

        String operator = resolveOperatorId();
        if (success && total > 0 && completed == total) {
            initialModelMapper.updateStatus(modelIdForAck, InitialModelStatus.DISTRIBUTED.getCode(), operator);
            log.info("初始模型全部分发完成: modelId={}, taskId={}, total={}, completed={}", modelIdForAck, taskId, total, completed);
        } else if (!success && total > 0) {
            initialModelMapper.updateStatus(modelIdForAck, InitialModelStatus.READY.getCode(), operator);
            log.warn("初始模型分发存在失败: modelId={}, taskId={}, completed={}, failed={}",
                    modelIdForAck, taskId, completed, failed);
        }
    }

    private String extractDistributionId(Map<String, Object> ackData) {
        if (ackData == null) {
            return null;
        }
        Object direct = ackData.get("distributionId");
        if (direct instanceof String directId && StringUtils.hasText(directId)) {
            return directId;
        }
        Map<String, Object> receipt = asMap(ackData.get("initialModelReceipt"));
        if (receipt != null) {
            Object receiptId = receipt.get("distributionId");
            if (receiptId instanceof String receiptStr && StringUtils.hasText(receiptStr)) {
                return receiptStr;
            }
        }
        return null;
    }

    private Integer extractRoundNumber(Map<String, Object> ackData) {
        if (ackData == null) {
            return null;
        }
        Object roundObj = ackData.get("round");
        if (roundObj == null) {
            roundObj = ackData.get("roundNumber");
        }
        if (roundObj instanceof Number number) {
            return number.intValue();
        }
        if (roundObj instanceof String roundStr && StringUtils.hasText(roundStr)) {
            try {
                return Integer.parseInt(roundStr);
            } catch (NumberFormatException ignore) {
                log.debug("无法解析轮次字段: {}", roundStr);
            }
        }
        return null;
    }

    private Boolean extractChecksumVerified(Map<String, Object> ackData) {
        Map<String, Object> receipt = ackData != null ? asMap(ackData.get("initialModelReceipt")) : null;
        if (receipt != null && receipt.containsKey("checksumVerified")) {
            Object value = receipt.get("checksumVerified");
            if (value instanceof Boolean bool) {
                return bool;
            }
        }
        return null;
    }

    private String extractErrorMessage(Map<String, Object> ackData) {
        if (ackData == null) {
            return null;
        }
        Object error = ackData.get("errorMessage");
        if (error instanceof String err && StringUtils.hasText(err)) {
            return err;
        }
        Map<String, Object> receipt = asMap(ackData.get("initialModelReceipt"));
        if (receipt != null) {
            Object receiptError = receipt.get("errorMessage");
            if (receiptError instanceof String receiptErr && StringUtils.hasText(receiptErr)) {
                return receiptErr;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private long safeLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private String resolveOperatorId() {
        String operatorId = BaseContext.getCurrentId();
        return StringUtils.hasText(operatorId) ? operatorId : "SYSTEM";
    }

    private void triggerRoundCompleteFallback(String taskId, int roundNumber) {
        try {
            Map<String, Object> roundResults = Map.of(
                    "status", "COMPLETED",
                    "completedAt", Instant.now().toString()
            );
            messageSender.broadcastRoundComplete(taskId, roundNumber, roundResults);
            log.info("触发轮次完成兜底广播: taskId={}, round={}", taskId, roundNumber);
        } catch (Exception e) {
            log.warn("触发轮次完成兜底广播失败: taskId={}, round={}, error={}",
                    taskId, roundNumber, e.getMessage());
        }
    }

    /**
     * 记录数据集分发完成ACK
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param assignedDatasetId 分配的数据集ID
     * @param success 是否成功
     * @param ackData ACK附加数据（JSON）
     * @param errorMessage 错误信息
     */
    @Transactional
    public void recordDatasetAck(String taskId,
                                 String vmId,
                                 String assignedDatasetId,
                                 boolean success,
                                 String ackData,
                                 String errorMessage) {
        if (taskId == null || vmId == null) {
            log.error("记录数据集ACK参数无效: taskId={}, vmId={}, assignedDatasetId={}", taskId, vmId, assignedDatasetId);
            return;
        }

        VmAckTracking.AckStatus status = success
                ? VmAckTracking.AckStatus.SUCCESS
                : VmAckTracking.AckStatus.FAILED;

        VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .ackType(VmAckTracking.AckType.DATASET_COMPLETE)
                .status(status)
                .ackData(ackData)
                .errorMessage(errorMessage)
                .acknowledgedAt(LocalDateTime.now())
                .build();

        vmAckTrackingMapper.insertAckTracking(ackTracking);
        ackCacheService.updateAckStatus(taskId, vmId,
                VmAckTracking.AckType.DATASET_COMPLETE,
                status,
                errorMessage);
        ackCacheService.updateAckProgress(taskId, VmAckTracking.AckType.DATASET_COMPLETE);

        log.info("数据集ACK记录完成: taskId={}, vmId={}, assignedDatasetId={}, status={}",
                taskId, vmId, assignedDatasetId, status);
    }

    /**
     * 记录任务启动失败
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param status 失败状态
     * @param reason 失败原因
     * @return 是否记录成功
     */
    public boolean recordTaskStartFailure(String taskId,
                                          String vmId,
                                          String status,
                                          String reason,
                                          Map<String, Object> ackData) {
        if (taskId == null || vmId == null) {
            log.error("记录任务启动失败参数无效: taskId={}, vmId={}", taskId, vmId);
            return false;
        }

        log.warn("记录任务启动失败: taskId={}, vmId={}, status={}, reason={}", taskId, vmId, status, reason);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(null) // 任务级别确认
                .ackType(VmAckTracking.AckType.TASK_START)
                .status(VmAckTracking.AckStatus.FAILED)
                .errorMessage(reason)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            if (result > 0) {
                log.info("任务启动失败记录成功: taskId={}, vmId={}, ackId={}", taskId, vmId, ackTracking.getId());
                handleInitialModelAck(taskId, vmId, ackData, false);
                return true;
            } else {
                log.error("任务启动失败记录失败: taskId={}, vmId={}", taskId, vmId);
                return false;
            }
        } catch (Exception e) {
            log.error("记录任务启动失败异常: taskId={}, vmId={}, reason={}", taskId, vmId, reason, e);
            return false;
        }
    }

    /**
     * 记录任务停止确认
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @return 是否记录成功
     */
    public boolean recordTaskStopAck(String taskId, String vmId) {
        if (taskId == null || vmId == null) {
            log.error("记录任务停止ACK参数无效: taskId={}, vmId={}", taskId, vmId);
            return false;
        }

        log.info("记录任务停止确认: taskId={}, vmId={}", taskId, vmId);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(null) // 任务级别确认
                .ackType(VmAckTracking.AckType.TASK_STOP)
                .status(VmAckTracking.AckStatus.SUCCESS)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            if (result > 0) {
                log.info("任务停止确认记录成功: taskId={}, vmId={}, ackId={}", taskId, vmId, ackTracking.getId());
                return true;
            } else {
                log.error("任务停止确认记录失败: taskId={}, vmId={}", taskId, vmId);
                return false;
            }
        } catch (Exception e) {
            log.error("记录任务停止确认异常: taskId={}, vmId={}", taskId, vmId, e);
            return false;
        }
    }

    /**
     * 记录任务停止失败
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param status 失败状态
     * @param reason 失败原因
     * @return 是否记录成功
     */
    public boolean recordTaskStopFailure(String taskId, String vmId, String status, String reason) {
        if (taskId == null || vmId == null) {
            log.error("记录任务停止失败参数无效: taskId={}, vmId={}", taskId, vmId);
            return false;
        }

        log.warn("记录任务停止失败: taskId={}, vmId={}, status={}, reason={}", taskId, vmId, status, reason);

        try {
            // 创建确认跟踪记录
            VmAckTracking tracking = new VmAckTracking();
            tracking.setTaskId(taskId);
            tracking.setVmId(vmId);
            tracking.setAckType(VmAckTracking.AckType.TASK_STOP);
            tracking.setStatus(VmAckTracking.AckStatus.FAILED);
            tracking.setErrorMessage(reason);
            tracking.setCreatedAt(LocalDateTime.now());
            tracking.setUpdatedAt(LocalDateTime.now());

            vmAckTrackingMapper.insert(tracking);

            log.info("已记录任务停止失败: taskId={}, vmId={}, reason={}", taskId, vmId, reason);
            return true;
        } catch (Exception e) {
            log.error("记录任务停止失败时出错: taskId={}, vmId={}, error={}", taskId, vmId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 记录任务恢复确认
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @return 是否记录成功
     */
    public boolean recordTaskResumeAck(String taskId, String vmId) {
        if (taskId == null || vmId == null) {
            log.error("记录任务恢复ACK参数无效: taskId={}, vmId={}", taskId, vmId);
            return false;
        }

        log.info("记录任务恢复确认: taskId={}, vmId={}", taskId, vmId);

        try {
            // 创建任务恢复确认跟踪记录
            VmAckTracking tracking = new VmAckTracking();
            tracking.setTaskId(taskId);
            tracking.setVmId(vmId);
            tracking.setAckType(VmAckTracking.AckType.TASK_RESUME);
            tracking.setStatus(VmAckTracking.AckStatus.SUCCESS);
            tracking.setAckTime(LocalDateTime.now());
            tracking.setCreatedAt(LocalDateTime.now());
            tracking.setUpdatedAt(LocalDateTime.now());

            vmAckTrackingMapper.insert(tracking);

            log.info("任务恢复确认已记录: taskId={}, vmId={}", taskId, vmId);
            return true;
        } catch (Exception e) {
            log.error("记录任务恢复确认时出错: taskId={}, vmId={}, error={}", taskId, vmId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 记录任务恢复失败
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param status 失败状态
     * @param reason 失败原因
     * @return 是否记录成功
     */
    public boolean recordTaskResumeFailure(String taskId, String vmId, String status, String reason) {
        if (taskId == null || vmId == null) {
            log.error("记录任务恢复失败参数无效: taskId={}, vmId={}", taskId, vmId);
            return false;
        }

        log.warn("记录任务恢复失败: taskId={}, vmId={}, status={}, reason={}", taskId, vmId, status, reason);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(null) // 任务级别确认
                .ackType(VmAckTracking.AckType.TASK_RESUME)
                .status(VmAckTracking.AckStatus.FAILED)
                .errorMessage(reason)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            if (result > 0) {
                log.info("任务恢复失败记录成功: taskId={}, vmId={}, ackId={}", taskId, vmId, ackTracking.getId());
                return true;
            } else {
                log.error("任务恢复失败记录失败: taskId={}, vmId={}", taskId, vmId);
                return false;
            }
        } catch (Exception e) {
            log.error("记录任务恢复失败异常: taskId={}, vmId={}, reason={}", taskId, vmId, reason, e);
            return false;
        }
    }

    /**
     * 记录任务删除确认
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @return 是否记录成功
     */
    public boolean recordTaskDeleteAck(String taskId, String vmId) {
        if (taskId == null || vmId == null) {
            log.error("记录任务删除ACK参数无效: taskId={}, vmId={}", taskId, vmId);
            return false;
        }

        log.info("记录任务删除确认: taskId={}, vmId={}", taskId, vmId);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(null) // 任务级别确认
                .ackType(VmAckTracking.AckType.TASK_DELETE)
                .status(VmAckTracking.AckStatus.SUCCESS)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            if (result > 0) {
                log.info("任务删除确认记录成功: taskId={}, vmId={}, ackId={}", taskId, vmId, ackTracking.getId());
                return true;
            } else {
                log.error("任务删除确认记录失败: taskId={}, vmId={}", taskId, vmId);
                return false;
            }
        } catch (Exception e) {
            log.error("记录任务删除确认异常: taskId={}, vmId={}", taskId, vmId, e);
            return false;
        }
    }

    /**
     * 记录任务删除失败
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param status 失败状态
     * @param reason 失败原因
     * @return 是否记录成功
     */
    public boolean recordTaskDeleteFailure(String taskId, String vmId, String status, String reason) {
        if (taskId == null || vmId == null) {
            log.error("记录任务删除失败参数无效: taskId={}, vmId={}", taskId, vmId);
            return false;
        }

        log.warn("记录任务删除失败: taskId={}, vmId={}, status={}, reason={}", taskId, vmId, status, reason);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(null) // 任务级别确认
                .ackType(VmAckTracking.AckType.TASK_DELETE)
                .status(VmAckTracking.AckStatus.FAILED)
                .errorMessage(reason)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            if (result > 0) {
                log.info("任务删除失败记录成功: taskId={}, vmId={}, ackId={}", taskId, vmId, ackTracking.getId());
                return true;
            } else {
                log.error("任务删除失败记录失败: taskId={}, vmId={}", taskId, vmId);
                return false;
            }
        } catch (Exception e) {
            log.error("记录任务删除失败异常: taskId={}, vmId={}, reason={}", taskId, vmId, reason, e);
            return false;
        }
    }

    /**
     * 跟踪消息发送状态
     *
     * @param messageId 消息ID
     * @param vmId 目标VM ID
     * @param ackType 确认类型
     * @return 是否跟踪成功
     */
    public boolean trackMessage(String messageId, String vmId, VmAckTracking.AckType ackType) {
        if (messageId == null || vmId == null || ackType == null) {
            log.error("跟踪消息参数无效: messageId={}, vmId={}, ackType={}", messageId, vmId, ackType);
            return false;
        }

        log.debug("开始跟踪消息: messageId={}, vmId={}, ackType={}", messageId, vmId, ackType);

        try {
            // 创建消息跟踪记录
            VmAckTracking tracking = new VmAckTracking();
            tracking.setTaskId("MESSAGE_TRACKING"); // 用于区分消息跟踪
            tracking.setVmId(vmId);
            tracking.setMessageId(messageId);
            tracking.setAckType(ackType);
            tracking.setStatus(VmAckTracking.AckStatus.PENDING);
            tracking.setCreatedAt(LocalDateTime.now());
            tracking.setUpdatedAt(LocalDateTime.now());

            vmAckTrackingMapper.insert(tracking);

            log.debug("消息跟踪已创建: messageId={}, vmId={}, ackType={}", messageId, vmId, ackType);
            return true;
        } catch (Exception e) {
            log.error("创建消息跟踪时出错: messageId={}, vmId={}, error={}", messageId, vmId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 初始化任务跟踪
     *
     * @param taskId 任务ID
     * @param vmIds 参与的VM ID列表
     * @return 是否初始化成功
     */
    public boolean initializeTask(String taskId, List<String> vmIds) {
        if (taskId == null || vmIds == null || vmIds.isEmpty()) {
            log.error("初始化任务跟踪参数无效: taskId={}, vmIds={}", taskId, vmIds);
            return false;
        }

        log.info("初始化任务跟踪: taskId={}, vmCount={}", taskId, vmIds.size());

        try {
            // 为每个VM创建初始跟踪记录
            for (String vmId : vmIds) {
                VmAckTracking ackTracking = VmAckTracking.builder()
                    .taskId(taskId)
                    .vmId(vmId)
                    .roundNumber(null) // 任务级别初始化
                    .ackType(VmAckTracking.AckType.TASK_INIT)
                    .status(VmAckTracking.AckStatus.PENDING)
                    .acknowledgedAt(LocalDateTime.now())
                    .build();

                vmAckTrackingMapper.insertAckTracking(ackTracking);
                log.debug("任务跟踪初始化: taskId={}, vmId={}, ackId={}", taskId, vmId, ackTracking.getId());
            }

            log.info("任务跟踪初始化成功: taskId={}, 初始化VM数={}", taskId, vmIds.size());
            return true;
        } catch (Exception e) {
            log.error("初始化任务跟踪失败: taskId={}, vmCount={}", taskId, vmIds.size(), e);
            return false;
        }
    }

    /**
     * 初始化数据集分发ACK跟踪
     *
     * @param taskId 任务ID
     * @param vmIds 参与的VM ID列表
     */
    public void initializeDatasetAck(String taskId, List<String> vmIds) {
        if (taskId == null || vmIds == null || vmIds.isEmpty()) {
            log.warn("初始化数据集ACK参数无效: taskId={}, vmIds={}", taskId, vmIds);
            return;
        }

        try {
            Set<String> vmIdSet = new HashSet<>(vmIds);
            ackCacheService.setTaskParticipants(taskId, vmIdSet);

            for (String vmId : vmIdSet) {
                VmAckTracking ackTracking = VmAckTracking.builder()
                        .taskId(taskId)
                        .vmId(vmId)
                        .ackType(VmAckTracking.AckType.DATASET_COMPLETE)
                        .status(VmAckTracking.AckStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                vmAckTrackingMapper.insertAckTracking(ackTracking);
                ackCacheService.updateAckStatus(taskId, vmId,
                        VmAckTracking.AckType.DATASET_COMPLETE,
                        VmAckTracking.AckStatus.PENDING);

                try {
                    ackCacheService.setAckTimeout(taskId, vmId,
                            VmAckTracking.AckType.DATASET_COMPLETE,
                            LocalDateTime.now().plusSeconds(datasetAckTimeoutSeconds));
                } catch (Exception ignore) {}
            }

            ackCacheService.updateAckProgress(taskId, VmAckTracking.AckType.DATASET_COMPLETE);
            log.info("数据集ACK跟踪初始化完成: taskId={}, vmCount={}", taskId, vmIdSet.size());
        } catch (Exception e) {
            log.error("初始化数据集ACK跟踪失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 重新初始化任务跟踪
     *
     * @param taskId 任务ID
     * @param vmIds 参与的VM ID列表
     * @return 是否重新初始化成功
     */
    public boolean reinitializeTask(String taskId, List<String> vmIds) {
        if (taskId == null || vmIds == null || vmIds.isEmpty()) {
            log.error("重新初始化任务跟踪参数无效: taskId={}, vmIds={}", taskId, vmIds);
            return false;
        }

        log.info("重新初始化任务跟踪: taskId={}, vmCount={}", taskId, vmIds.size());

        try {
            // 首先清理旧的跟踪记录
            vmAckTrackingMapper.deleteByTaskId(taskId);
            log.debug("已清理旧的跟踪记录: taskId={}", taskId);

            // 重新创建跟踪记录
            for (String vmId : vmIds) {
                VmAckTracking ackTracking = VmAckTracking.builder()
                    .taskId(taskId)
                    .vmId(vmId)
                    .roundNumber(null) // 任务级别初始化
                    .ackType(VmAckTracking.AckType.TASK_REINIT)
                    .status(VmAckTracking.AckStatus.PENDING)
                    .acknowledgedAt(LocalDateTime.now())
                    .build();

                vmAckTrackingMapper.insertAckTracking(ackTracking);
                log.debug("任务重新初始化跟踪: taskId={}, vmId={}, ackId={}", taskId, vmId, ackTracking.getId());
            }

            log.info("任务重新初始化成功: taskId={}, 初始化VM数={}", taskId, vmIds.size());
            return true;
        } catch (Exception e) {
            log.error("重新初始化任务跟踪失败: taskId={}, vmCount={}", taskId, vmIds.size(), e);
            return false;
        }
    }

    /**
     * 等待所有确认（重构版本 - 使用纯缓存查询）
     *
     * @param taskId 任务ID
     * @param ackType 确认类型
     * @param timeoutSeconds 超时秒数
     * @return 是否所有确认都已收到
     */
    public boolean waitForAllAcknowledgments(String taskId, String ackType, int timeoutSeconds) {
        if (taskId == null || ackType == null || timeoutSeconds <= 0) {
            log.error("等待确认参数无效: taskId={}, ackType={}, timeout={}", taskId, ackType, timeoutSeconds);
            return false;
        }

        log.info("等待所有确认: taskId={}, ackType={}, timeout={}s", taskId, ackType, timeoutSeconds);

        try {
            // 将字符串类型的ackType转换为枚举类型
            VmAckTracking.AckType enumAckType;
            try {
                enumAckType = VmAckTracking.AckType.valueOf(ackType);
            } catch (IllegalArgumentException e) {
                log.error("无效的ACK类型: {}", ackType);
                return false;
            }

            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;

            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                // 使用缓存服务检查ACK进度
                if (ackCacheService.isAllVmsAcked(taskId, enumAckType)) {
                    log.info("所有VM确认完成（缓存查询）: taskId={}, ackType={}", taskId, ackType);
                    return true;
                }

                // 获取详细进度信息用于调试（并在全部完成但存在失败时尽早退出）
                var progress = ackCacheService.getAckProgress(taskId, enumAckType);
                if (progress != null) {
                    if (progress.isAllCompleted() && !progress.isAllSuccess()) {
                        log.warn("确认完成但存在失败: taskId={}, ackType={}, success={}, failed={}, timeout={}",
                                taskId, ackType, progress.getSuccessVmCount(),
                                progress.getFailedVmCount(), progress.getTimeoutVmCount());
                        return false;
                    }
                    log.debug("确认进度（缓存）: {}/{} taskId={}, ackType={}, 详情={}",
                            progress.getAcknowledgedVms(), progress.getTotalVms(),
                            taskId, ackType, progress.getDetailedStatusDescription());
                } else {
                    log.debug("尚无确认进度信息: taskId={}, ackType={}", taskId, ackType);
                }

                // 短暂等待后重试
                Thread.sleep(500); // 提高检查频率到0.5秒一次，因为缓存查询更快
            }

            log.warn("等待确认超时: taskId={}, ackType={}, timeout={}s", taskId, ackType, timeoutSeconds);
            try {
                // 将仍处于等待中的VM标记为TIMEOUT，便于上层进度与告警准确展示
                Set<String> pending = ackCacheService.getPendingVms(taskId, enumAckType);
                if (pending != null && !pending.isEmpty()) {
                    for (String vmId : pending) {
                        ackCacheService.updateAckStatus(taskId, vmId, enumAckType,
                                VmAckTracking.AckStatus.TIMEOUT, "超时");
                    }
                }
            } catch (Exception ignore) {}
            return false;
        } catch (Exception e) {
            log.error("等待确认时出错: taskId={}, ackType={}, error={}", taskId, ackType, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 清理任务跟踪数据
     *
     * @param taskId 任务ID
     * @return 是否清理成功
     */
    public boolean cleanupTask(String taskId) {
        if (taskId == null) {
            log.error("清理任务跟踪参数无效: taskId={}", taskId);
            return false;
        }

        log.info("清理任务跟踪数据: taskId={}", taskId);

        try {
            // 删除该任务的所有跟踪记录
            int deletedCount = vmAckTrackingMapper.deleteByTaskId(taskId);

            if (deletedCount > 0) {
                log.info("任务跟踪数据清理成功: taskId={}, 删除记录数={}", taskId, deletedCount);
            } else {
                log.debug("任务没有跟踪数据需要清理: taskId={}", taskId);
            }

            return true;
        } catch (Exception e) {
            log.error("清理任务跟踪数据失败: taskId={}", taskId, e);
            return false;
        }
    }

    // ==================== v1.4协议新增轮次ACK跟踪方法 ====================

    /**
     * 记录VM发送的ROUND_START_ACK
     * v1.4协议新增
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param vmId VM ID
     * @param status 确认状态
     * @return 是否记录成功
     */
    public boolean recordRoundStartAck(String taskId, Integer roundNumber, String vmId, String status) {
        if (taskId == null || roundNumber == null || vmId == null) {
            log.error("记录ROUND_START_ACK参数无效: taskId={}, roundNumber={}, vmId={}",
                     taskId, roundNumber, vmId);
            return false;
        }

        log.info("记录ROUND_START_ACK: taskId={}, round={}, vmId={}, status={}",
                taskId, roundNumber, vmId, status);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(roundNumber)
                .ackType(VmAckTracking.AckType.ROUND_START)
                .status("SUCCESS".equals(status) ? VmAckTracking.AckStatus.SUCCESS : VmAckTracking.AckStatus.FAILED)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            if (result > 0) {
                log.info("轮次开始确认记录成功: taskId={}, roundNumber={}, vmId={}, ackId={}",
                        taskId, roundNumber, vmId, ackTracking.getId());
                return true;
            } else {
                log.error("轮次开始确认记录失败: taskId={}, roundNumber={}, vmId={}", taskId, roundNumber, vmId);
                return false;
            }
        } catch (Exception e) {
            log.error("记录轮次开始确认异常: taskId={}, roundNumber={}, vmId={}", taskId, roundNumber, vmId, e);
            return false;
        }
    }

    /**
     * 记录VM发送的ROUND_COMPLETE_ACK
     * v1.4协议新增
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @param vmId VM ID
     * @param status 确认状态
     * @return 是否记录成功
     */
    public boolean recordRoundCompleteAck(String taskId, Integer roundNumber, String vmId, String status) {
        if (taskId == null || roundNumber == null || vmId == null) {
            log.error("记录ROUND_COMPLETE_ACK参数无效: taskId={}, roundNumber={}, vmId={}",
                     taskId, roundNumber, vmId);
            return false;
        }

        log.info("记录ROUND_COMPLETE_ACK: taskId={}, round={}, vmId={}, status={}",
                taskId, roundNumber, vmId, status);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(roundNumber)
                .ackType(VmAckTracking.AckType.ROUND_COMPLETE)
                .status("SUCCESS".equals(status) ? VmAckTracking.AckStatus.SUCCESS : VmAckTracking.AckStatus.FAILED)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            if (result > 0) {
                log.info("轮次完成确认记录成功: taskId={}, roundNumber={}, vmId={}, ackId={}",
                        taskId, roundNumber, vmId, ackTracking.getId());
                return true;
            } else {
                log.error("轮次完成确认记录失败: taskId={}, roundNumber={}, vmId={}", taskId, roundNumber, vmId);
                return false;
            }
        } catch (Exception e) {
            log.error("记录轮次完成确认异常: taskId={}, roundNumber={}, vmId={}", taskId, roundNumber, vmId, e);
            return false;
        }
    }

    /**
     * 检查指定轮次的所有VM是否都已发送ROUND_START_ACK
     * v1.4协议新增
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @return 是否所有VM都已确认
     */
    public boolean areAllVmsReadyForRound(String taskId, Integer roundNumber) {
        if (taskId == null || roundNumber == null) {
            log.error("检查轮次就绪状态参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        log.debug("检查轮次就绪状态: taskId={}, round={}", taskId, roundNumber);

        try {
            // 查询参与该任务的VM总数（基于task_participants表）
            int totalVms = taskParticipantsMapper.countActiveParticipants(taskId);
            if (totalVms == 0) {
                log.warn("任务没有活跃的参与VM: taskId={}", taskId);
                return false;
            }

            // 查询已发送ROUND_START_ACK的VM数量
            int ackedVms = vmAckTrackingMapper.countVmsByAckStatus(
                taskId, VmAckTracking.AckType.ROUND_START, VmAckTracking.AckStatus.SUCCESS, roundNumber);

            boolean allReady = (ackedVms >= totalVms);
            log.info("轮次就绪状态检查完成: taskId={}, round={}, totalVms={}, ackedVms={}, allReady={}",
                    taskId, roundNumber, totalVms, ackedVms, allReady);

            return allReady;
        } catch (Exception e) {
            log.error("检查轮次就绪状态失败: taskId={}, round={}", taskId, roundNumber, e);
            return false;
        }
    }

    /**
     * 检查指定轮次的所有VM是否都已发送ROUND_COMPLETE_ACK
     * v1.4协议新增
     *
     * @param taskId 任务ID
     * @param roundNumber 轮次号
     * @return 是否所有VM都已确认轮次完成
     */
    public boolean areAllVmsCompletedRound(String taskId, Integer roundNumber) {
        if (taskId == null || roundNumber == null) {
            log.error("检查轮次完成状态参数无效: taskId={}, roundNumber={}", taskId, roundNumber);
            return false;
        }

        log.debug("检查轮次完成状态: taskId={}, round={}", taskId, roundNumber);

        try {
            // 查询参与该任务的VM总数
            int totalVms = taskParticipantsMapper.countActiveParticipants(taskId);
            if (totalVms == 0) {
                log.warn("任务没有活跃的参与VM: taskId={}", taskId);
                return false;
            }

            // 查询已发送ROUND_COMPLETE_ACK的VM数量
            int completedVms = vmAckTrackingMapper.countVmsByAckStatus(
                taskId, VmAckTracking.AckType.ROUND_COMPLETE, VmAckTracking.AckStatus.SUCCESS, roundNumber);

            boolean allCompleted = (completedVms >= totalVms);
            log.info("轮次完成状态检查完成: taskId={}, round={}, totalVms={}, completedVms={}, allCompleted={}",
                    taskId, roundNumber, totalVms, completedVms, allCompleted);

            return allCompleted;
        } catch (Exception e) {
            log.error("检查轮次完成状态失败: taskId={}, round={}", taskId, roundNumber, e);
            return false;
        }
    }

    // ==================== v1.4协议梯度上传相关ACK跟踪方法 ====================

    /**
     * 记录VM梯度上传成功确认
     * v1.4协议新增
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param roundNumber 轮次号
     * @return 是否记录成功
     */
    @Transactional
    public boolean recordGradientUploadSuccess(String taskId, String vmId, Integer roundNumber) {
        if (taskId == null || vmId == null || roundNumber == null) {
            log.error("记录梯度上传成功参数无效: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
            return false;
        }

        log.info("记录梯度上传成功: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(roundNumber)
                .ackType(VmAckTracking.AckType.GRADIENT_UPLOAD)
                .status(VmAckTracking.AckStatus.SUCCESS)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            if (result > 0) {
                log.info("梯度上传成功记录完成: taskId={}, vmId={}, roundNumber={}, ackId={}",
                        taskId, vmId, roundNumber, ackTracking.getId());
            } else {
                log.error("梯度上传成功记录失败: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
            }

            try {
                roundStateManager.recordGradientUpload(taskId, roundNumber);
            } catch (Exception syncEx) {
                log.warn("同步梯度上传进度到round_states失败: taskId={}, vmId={}, roundNumber={}, error={}",
                        taskId, vmId, roundNumber, syncEx.getMessage(), syncEx);
            }

            return result > 0;
        } catch (Exception e) {
            log.error("记录梯度上传成功异常: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber, e);
            return false;
        }
    }

    /**
     * 记录VM梯度上传失败确认
     * v1.4协议新增
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param roundNumber 轮次号
     * @param errorMessage 错误信息
     * @return 是否记录成功
     */
    @Transactional
    public boolean recordGradientUploadFailure(String taskId, String vmId, Integer roundNumber, String errorMessage) {
        if (taskId == null || vmId == null || roundNumber == null) {
            log.error("记录梯度上传失败参数无效: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
            return false;
        }

        log.warn("记录梯度上传失败: taskId={}, vmId={}, roundNumber={}, error={}", taskId, vmId, roundNumber, errorMessage);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(roundNumber)
                .ackType(VmAckTracking.AckType.GRADIENT_UPLOAD)
                .status(VmAckTracking.AckStatus.FAILED)
                .errorMessage(errorMessage)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            if (result > 0) {
                log.info("梯度上传失败记录完成: taskId={}, vmId={}, roundNumber={}, ackId={}",
                        taskId, vmId, roundNumber, ackTracking.getId());
                return true;
            } else {
                log.error("梯度上传失败记录失败: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
                return false;
            }
        } catch (Exception e) {
            log.error("记录梯度上传失败异常: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber, e);
            return false;
        }
    }

    /**
     * 记录VM准备就绪可以开始梯度上传
     * v1.4协议新增
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param roundNumber 轮次号
     * @return 是否记录成功
     */
    @Transactional
    public boolean recordVmReadyForGradientUpload(String taskId, String vmId, Integer roundNumber) {
        if (taskId == null || vmId == null || roundNumber == null) {
            log.error("记录VM梯度上传就绪参数无效: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
            return false;
        }

        log.info("记录VM准备就绪开始梯度上传: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(roundNumber)
                .ackType(VmAckTracking.AckType.GRADIENT_READY)
                .status(VmAckTracking.AckStatus.SUCCESS)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            if (result > 0) {
                log.info("VM梯度上传就绪记录完成: taskId={}, vmId={}, roundNumber={}, ackId={}",
                        taskId, vmId, roundNumber, ackTracking.getId());
                return true;
            } else {
                log.error("VM梯度上传就绪记录失败: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
                return false;
            }
        } catch (Exception e) {
            log.error("记录VM梯度上传就绪异常: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber, e);
            return false;
        }
    }

    // ==================== 缓存版本的等待确认方法 ====================

    /**
     * 等待所有VM确认完成（缓存版本）
     * 使用缓存进行轮询，提供更好的性能
     *
     * @param taskId 任务ID
     * @param ackType ACK类型
     * @param timeoutMs 超时时间（毫秒）
     * @param pollIntervalMs 轮询间隔（毫秒）
     * @return 是否所有VM都确认完成
     */
    public boolean waitForAllAcknowledgments(String taskId, VmAckTracking.AckType ackType,
                                           long timeoutMs, long pollIntervalMs) {
        if (taskId == null || ackType == null || timeoutMs <= 0) {
            log.error("等待确认参数无效: taskId={}, ackType={}, timeout={}ms", taskId, ackType, timeoutMs);
            return false;
        }

        log.info("开始等待所有VM确认完成: taskId={}, ackType={}, timeout={}ms", taskId, ackType, timeoutMs);

        long startTime = System.currentTimeMillis();
        long deadline = startTime + timeoutMs;

        try {
            // 首先检查是否有参与者
            Set<String> participants = ackCacheService.getTaskParticipants(taskId);
            if (participants == null || participants.isEmpty()) {
                log.warn("任务没有参与者，无需等待确认: taskId={}", taskId);
                return false;
            }

            while (System.currentTimeMillis() < deadline) {
                // 从缓存获取最新进度
                AckProgress progress = ackCacheService.getAckProgress(taskId, ackType);

                if (progress == null) {
                    log.debug("ACK进度为空，继续等待: taskId={}, ackType={}", taskId, ackType);
                } else if (progress.isAllCompleted()) {
                    if (progress.isAllSuccess()) {
                        log.info("所有VM确认完成且全部成功: taskId={}, ackType={}, 耗时={}ms",
                                taskId, ackType, System.currentTimeMillis() - startTime);
                        return true;
                    } else {
                        log.warn("所有VM确认完成但有失败: taskId={}, ackType={}, success={}, failed={}, timeout={}",
                                taskId, ackType, progress.getSuccessVmCount(),
                                progress.getFailedVmCount(), progress.getTimeoutVmCount());
                        return false;
                    }
                } else {
                    log.debug("等待VM确认中: taskId={}, ackType={}, progress={}/{}",
                            taskId, ackType, progress.getAcknowledgedVms(), progress.getTotalVms());
                }

                // 短暂等待后重试
                Thread.sleep(pollIntervalMs);
            }

            log.warn("等待确认超时: taskId={}, ackType={}, timeout={}ms", taskId, ackType, timeoutMs);
            return false;
        } catch (Exception e) {
            log.error("等待确认时出错: taskId={}, ackType={}, error={}", taskId, ackType, e.getMessage(), e);
            return false;
        }
    }
}
