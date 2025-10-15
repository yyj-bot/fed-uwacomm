package com.feduwacomm.service;

import com.feduwacomm.dto.RoundDatasetBinding;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.utils.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 联邦学习协议流程编排服务
 * 负责协调和管理完整的联邦学习流程，符合协议v1.4标准
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 */
@Service
public class FederatedLearningOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(FederatedLearningOrchestrator.class);

    private final WebSocketMessageSender messageSender;
    private final MessageBuilder messageBuilder;
    private final FederatedTasksMapper federatedTasksMapper;
    private final RoundStateManager roundStateManager;

    public FederatedLearningOrchestrator(WebSocketMessageSender messageSender,
                                        MessageBuilder messageBuilder,
                                        FederatedTasksMapper federatedTasksMapper,
                                        RoundStateManager roundStateManager) {
        this.messageSender = messageSender;
        this.messageBuilder = messageBuilder;
        this.federatedTasksMapper = federatedTasksMapper;
        this.roundStateManager = roundStateManager;
    }

    // ==================== 联邦学习流程编排方法 ====================

    /**
     * 启动联邦学习轮次
     * 符合协议v1.4标准流程
     */
    public void startFederatedRound(String taskId, int roundNumber, List<String> participantVmIds) {
        try {
            logger.info("启动联邦学习轮次 - TaskId: {}, RoundNumber: {}, Participants: {}",
                       taskId, roundNumber, participantVmIds.size());

            // 1. 获取任务配置
            FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            if (task == null) {
                logger.error("任务不存在: {}", taskId);
                return;
            }

            // 2. 构建轮次配置
            Map<String, Object> roundSpecificConfig = buildRoundConfig(task, roundNumber);
            Map<String, Object> targetMetrics = buildTargetMetrics(task);

            // 3. 准备轮次数据集上下文
            Map<String, RoundDatasetBinding> datasetBindings =
                    roundStateManager.prepareNextRoundContext(taskId, roundNumber);

            // 4. 向所有参与者发送ROUND_START消息
            for (String vmId : participantVmIds) {
                RoundDatasetBinding binding = datasetBindings.get(vmId);
                if (binding == null) {
                    logger.warn("ROUND_START 缺少数据集绑定: taskId={}, round={}, vmId={}",
                            taskId, roundNumber, vmId);
                }
                messageSender.sendRoundStart(vmId, taskId, roundNumber,
                                           roundSpecificConfig, targetMetrics,
                                           participantVmIds.size(),
                                           buildDatasetContext(binding));
            }

            logger.info("轮次启动消息已发送 - TaskId: {}, RoundNumber: {}", taskId, roundNumber);

        } catch (Exception e) {
            logger.error("启动联邦学习轮次失败 - TaskId: {}, RoundNumber: {}, Error: {}",
                        taskId, roundNumber, e.getMessage(), e);
        }
    }

    /**
     * 完成联邦学习轮次
     * 符合协议v1.4标准流程
     */
    public void completeFederatedRound(String taskId, int roundNumber, List<String> participantVmIds,
                                      Map<String, Object> aggregationResults) {
        try {
            logger.info("完成联邦学习轮次 - TaskId: {}, RoundNumber: {}, Participants: {}",
                       taskId, roundNumber, participantVmIds.size());

            // 1. 构建轮次结果
            Map<String, Object> roundResults = buildRoundResults(aggregationResults, participantVmIds.size());

            // 2. 构建下一轮次配置
            Map<String, Object> nextRound = buildNextRoundConfig(taskId, roundNumber + 1);

            // 3. 判断任务状态
            String taskStatus = determineTaskStatus(taskId, roundNumber);

            // 4. 向所有参与者发送ROUND_COMPLETE消息
            for (String vmId : participantVmIds) {
                messageSender.sendRoundComplete(vmId, taskId, roundNumber,
                                              roundResults, nextRound, taskStatus);
            }

            logger.info("轮次完成消息已发送 - TaskId: {}, RoundNumber: {}, Status: {}",
                       taskId, roundNumber, taskStatus);

        } catch (Exception e) {
            logger.error("完成联邦学习轮次失败 - TaskId: {}, RoundNumber: {}, Error: {}",
                        taskId, roundNumber, e.getMessage(), e);
        }
    }

    /**
     * 广播全局模型
     * 符合协议v1.4标准流程
     */
    public void broadcastGlobalModel(String taskId, int roundNumber, Map<String, Object> globalModel,
                                    Map<String, Object> aggregationInfo) {
        try {
            logger.info("广播全局模型 - TaskId: {}, RoundNumber: {}", taskId, roundNumber);

            // 构建下一轮次配置
            Map<String, Object> nextRoundConfig = buildNextRoundConfig(taskId, roundNumber + 1);

            // 广播全局模型到所有虚拟机
            messageSender.sendGlobalModelBroadcast(taskId, roundNumber, globalModel,
                                                  aggregationInfo, nextRoundConfig);

            logger.info("全局模型广播完成 - TaskId: {}, RoundNumber: {}", taskId, roundNumber);

        } catch (Exception e) {
            logger.error("广播全局模型失败 - TaskId: {}, RoundNumber: {}, Error: {}",
                        taskId, roundNumber, e.getMessage(), e);
        }
    }

    /**
     * 查询虚拟机状态
     * 符合协议v1.4标准
     */
    public void queryVmStatus(String vmId, boolean includeTasks) {
        try {
            logger.info("查询虚拟机状态 - VmId: {}, IncludeTasks: {}", vmId, includeTasks);

            messageSender.sendVmStatusQuery(vmId, "FULL", true, true, true, includeTasks, 10);

        } catch (Exception e) {
            logger.error("查询虚拟机状态失败 - VmId: {}, Error: {}", vmId, e.getMessage(), e);
        }
    }

    /**
     * 查询数据集状态
     * 符合协议v1.4标准
     */
    public void queryDatasetStatus(String vmId, String taskId, String datasetId) {
        try {
            logger.info("查询数据集状态 - VmId: {}, TaskId: {}, DatasetId: {}", vmId, taskId, datasetId);

            messageSender.sendDatasetStatusQuery(vmId, taskId, datasetId, "FULL",
                                               true, true, false);

        } catch (Exception e) {
            logger.error("查询数据集状态失败 - VmId: {}, TaskId: {}, DatasetId: {}, Error: {}",
                        vmId, taskId, datasetId, e.getMessage(), e);
        }
    }

    /**
     * 清理任务数据集
     * 符合协议v1.4标准
     */
    public void cleanupTaskDatasets(String vmId, String taskId, String datasetId) {
        try {
            logger.info("清理任务数据集 - VmId: {}, TaskId: {}, DatasetId: {}", vmId, taskId, datasetId);

            messageSender.sendDatasetDelete(vmId, taskId, datasetId, "TASK_COMPLETED", false, false);

        } catch (Exception e) {
            logger.error("清理任务数据集失败 - VmId: {}, TaskId: {}, DatasetId: {}, Error: {}",
                        vmId, taskId, datasetId, e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建轮次特定配置
     */
    private Map<String, Object> buildRoundConfig(FederatedTask task, int roundNumber) {
        Map<String, Object> config = new HashMap<>();

        // 根据轮次动态调整学习率
        double baseLearningRate = 0.01;
        double learningRateDecay = 0.95;
        double adjustedLearningRate = baseLearningRate * Math.pow(learningRateDecay, roundNumber - 1);

        config.put("learningRate", adjustedLearningRate);
        config.put("timeout", 600); // 10分钟超时
        config.put("epochs", 5);
        config.put("batchSize", 32);

        return config;
    }

    private Map<String, Object> buildDatasetContext(RoundDatasetBinding binding) {
        if (binding == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> context = new HashMap<>();
        context.put("vmId", binding.getVmId());
        context.put("assignedDatasetId", binding.getAssignedDatasetId());
        if (binding.getDatasetStatus() != null) {
            context.put("datasetStatus", binding.getDatasetStatus());
        }
        if (binding.getLocalPath() != null) {
            context.put("localPath", binding.getLocalPath());
        }
        return context;
    }

    /**
     * 构建目标指标
     */
    private Map<String, Object> buildTargetMetrics(FederatedTask task) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("minAccuracy", 0.85);
        metrics.put("maxLoss", 0.20);
        metrics.put("convergenceThreshold", 0.001);
        return metrics;
    }

    /**
     * 构建轮次结果
     */
    private Map<String, Object> buildRoundResults(Map<String, Object> aggregationResults, int participantCount) {
        Map<String, Object> results = new HashMap<>();
        results.put("participantCount", participantCount);
        results.put("globalAccuracy", aggregationResults.getOrDefault("accuracy", 0.0));
        results.put("globalLoss", aggregationResults.getOrDefault("loss", 1.0));
        results.put("convergenceImprovement", aggregationResults.getOrDefault("improvement", 0.0));
        results.put("duration", aggregationResults.getOrDefault("duration", 0));
        return results;
    }

    /**
     * 构建下一轮次配置
     */
    private Map<String, Object> buildNextRoundConfig(String taskId, int nextRoundNumber) {
        Map<String, Object> config = new HashMap<>();

        // 获取任务信息来判断是否有下一轮
        FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
        if (task != null && task.getTotalRounds() != null) {
            boolean hasNextRound = nextRoundNumber <= task.getTotalRounds();
            config.put("planned", hasNextRound);
            config.put("roundNumber", nextRoundNumber);

            if (hasNextRound) {
                // 下一轮开始时间设置为5分钟后
                long nextStartTime = System.currentTimeMillis() + 5 * 60 * 1000;
                config.put("scheduledStart", new java.util.Date(nextStartTime).toInstant().toString());
            }
        }

        return config;
    }

    /**
     * 判断任务状态
     */
    private String determineTaskStatus(String taskId, int completedRounds) {
        FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
        if (task != null && task.getTotalRounds() != null) {
            if (completedRounds >= task.getTotalRounds()) {
                return "COMPLETED";
            } else {
                return "CONTINUING";
            }
        }
        return "UNKNOWN";
    }
}
