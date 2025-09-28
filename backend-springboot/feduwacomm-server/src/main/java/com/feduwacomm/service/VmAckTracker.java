package com.feduwacomm.service;

import com.feduwacomm.entity.ModelDistribution;
import com.feduwacomm.entity.VmAckTracking;
import com.feduwacomm.mapper.GlobalModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.mapper.VmAckTrackingMapper;
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

    @Autowired
    private VmAckTrackingMapper vmAckTrackingMapper;

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

    // ==================== v1.4协议任务级别ACK跟踪方法 ====================

    /**
     * 记录任务启动确认
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param status 确认状态
     * @return 是否记录成功
     */
    public boolean recordTaskStartAck(String taskId, String vmId, String status) {
        if (taskId == null || vmId == null || status == null) {
            log.error("记录任务启动ACK参数无效: taskId={}, vmId={}, status={}", taskId, vmId, status);
            return false;
        }

        log.info("记录任务启动确认: taskId={}, vmId={}, status={}", taskId, vmId, status);

        try {
            VmAckTracking ackTracking = VmAckTracking.builder()
                .taskId(taskId)
                .vmId(vmId)
                .roundNumber(null) // 任务级别确认
                .ackType(VmAckTracking.AckType.TASK_START)
                .status("SUCCESS".equals(status) ? VmAckTracking.AckStatus.SUCCESS : VmAckTracking.AckStatus.FAILED)
                .acknowledgedAt(LocalDateTime.now())
                .build();

            int result = vmAckTrackingMapper.insertAckTracking(ackTracking);
            boolean success = result > 0 && "SUCCESS".equals(status);

            if (success) {
                log.info("任务启动确认记录成功: taskId={}, vmId={}, ackId={}", taskId, vmId, ackTracking.getId());
            } else {
                log.warn("任务启动确认记录失败或状态非成功: taskId={}, vmId={}, status={}", taskId, vmId, status);
            }

            return success;
        } catch (Exception e) {
            log.error("记录任务启动确认失败: taskId={}, vmId={}, status={}", taskId, vmId, status, e);
            return false;
        }
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
    public boolean recordTaskStartFailure(String taskId, String vmId, String status, String reason) {
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
            tracking.setMessageType("FEDERATED_TASK_STOP_FAILURE");
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
            tracking.setMessageType("FEDERATED_TASK_RESUME_ACK");
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
     * @param messageType 消息类型
     * @return 是否跟踪成功
     */
    public boolean trackMessage(String messageId, String vmId, String messageType) {
        if (messageId == null || vmId == null || messageType == null) {
            log.error("跟踪消息参数无效: messageId={}, vmId={}, messageType={}", messageId, vmId, messageType);
            return false;
        }

        log.debug("开始跟踪消息: messageId={}, vmId={}, messageType={}", messageId, vmId, messageType);

        try {
            // 创建消息跟踪记录
            VmAckTracking tracking = new VmAckTracking();
            tracking.setTaskId("MESSAGE_TRACKING"); // 用于区分消息跟踪
            tracking.setVmId(vmId);
            tracking.setMessageType(messageType);
            tracking.setMessageId(messageId);
            tracking.setStatus(VmAckTracking.AckStatus.PENDING);
            tracking.setCreatedAt(LocalDateTime.now());
            tracking.setUpdatedAt(LocalDateTime.now());

            vmAckTrackingMapper.insert(tracking);

            log.debug("消息跟踪已创建: messageId={}, vmId={}, messageType={}", messageId, vmId, messageType);
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
     * 等待所有确认
     *
     * @param taskId 任务ID
     * @param messageType 消息类型
     * @param timeoutSeconds 超时秒数
     * @return 是否所有确认都已收到
     */
    public boolean waitForAllAcknowledgments(String taskId, String messageType, int timeoutSeconds) {
        if (taskId == null || messageType == null || timeoutSeconds <= 0) {
            log.error("等待确认参数无效: taskId={}, messageType={}, timeout={}", taskId, messageType, timeoutSeconds);
            return false;
        }

        log.info("等待所有确认: taskId={}, messageType={}, timeout={}s", taskId, messageType, timeoutSeconds);

        try {
            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;

            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                // 查询当前任务的确认状态
                List<VmAckTracking> trackings = vmAckTrackingMapper.findByTaskIdAndMessageType(taskId, messageType);

                if (trackings.isEmpty()) {
                    log.debug("尚无确认记录，继续等待: taskId={}, messageType={}", taskId, messageType);
                } else {
                    // 检查是否所有VM都已确认
                    long successCount = trackings.stream()
                        .filter(t -> "SUCCESS".equals(t.getStatus()))
                        .count();

                    long totalCount = trackings.size();
                    log.debug("确认进度: {}/{} taskId={}, messageType={}", successCount, totalCount, taskId, messageType);

                    if (successCount == totalCount && totalCount > 0) {
                        log.info("所有VM确认完成: taskId={}, messageType={}, count={}", taskId, messageType, totalCount);
                        return true;
                    }
                }

                // 短暂等待后重试
                Thread.sleep(1000); // 每秒检查一次
            }

            log.warn("等待确认超时: taskId={}, messageType={}, timeout={}s", taskId, messageType, timeoutSeconds);
            return false;
        } catch (Exception e) {
            log.error("等待确认时出错: taskId={}, messageType={}, error={}", taskId, messageType, e.getMessage(), e);
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
                return true;
            } else {
                log.error("梯度上传成功记录失败: taskId={}, vmId={}, roundNumber={}", taskId, vmId, roundNumber);
                return false;
            }
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
}