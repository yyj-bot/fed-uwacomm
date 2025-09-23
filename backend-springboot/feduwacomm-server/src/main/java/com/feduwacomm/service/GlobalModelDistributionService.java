package com.feduwacomm.service;

import com.feduwacomm.config.AggregationConfig;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.event.AggregationCompletedEvent;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.VmRoundModelsMapper;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.dto.VmQueryDTO;
import com.feduwacomm.vo.VmListVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 全局模型分发服务
 * 负责将聚合后的全局模型分发给所有参与的客户端
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalModelDistributionService {

    private final SimpMessagingTemplate messagingTemplate;
    private final VmRoundModelsMapper vmRoundModelsMapper;
    private final FederatedTasksMapper federatedTasksMapper;
    private final AggregationConfig aggregationConfig;
    private final ObjectMapper objectMapper;
    private final VmInstanceService vmInstanceService;

    /**
     * 监听聚合完成事件，自动分发全局模型
     */
    @EventListener
    @Async
    public void handleAggregationCompleted(AggregationCompletedEvent event) {
        if (!event.isSuccess()) {
            log.warn("聚合失败，跳过全局模型分发: 任务ID={}, 轮次={}, 错误={}", 
                    event.getTaskId(), event.getRoundNumber(), event.getErrorMessage());
            return;
        }

        log.info("开始分发全局模型: 任务ID={}, 轮次={}, 全局模型ID={}", 
                event.getTaskId(), event.getRoundNumber(), event.getGlobalModelId());

        try {
            distributeGlobalModel(event.getTaskId(), event.getRoundNumber(), 
                    event.getGlobalModelId(), event.getGlobalMetrics());
        } catch (Exception e) {
            log.error("全局模型分发失败: 任务ID={}, 轮次={}, 错误={}", 
                    event.getTaskId(), event.getRoundNumber(), e.getMessage(), e);
        }
    }

    /**
     * 分发全局模型给指定任务的所有参与客户端
     */
    public void distributeGlobalModel(String taskId, Integer roundNumber, 
                                     String globalModelId, Map<String, ?> globalMetrics) {
        try {
            // 获取任务信息
            FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("任务不存在: " + taskId);
            }

            // 获取参与该轮的所有客户端
            List<String> participantVmIds;

            if (roundNumber == 1) {
                // 第1轮训练：使用任务的所有参与VM（从任务配置中获取）
                String configJson = task.getConfig();
                participantVmIds = new ArrayList<>();

                if (configJson != null && !configJson.trim().isEmpty()) {
                    try {
                        // 解析配置JSON，查找参与VM列表
                        Map<String, Object> config = objectMapper.readValue(configJson, Map.class);
                        Object vmIds = config.get("participantVmIds");
                        if (vmIds instanceof List) {
                            participantVmIds = (List<String>) vmIds;
                        } else if (vmIds instanceof String) {
                            participantVmIds = Arrays.asList(objectMapper.readValue((String) vmIds, String[].class));
                        }
                    } catch (Exception e) {
                        log.warn("解析任务配置中的参与VM ID失败: {}", e.getMessage());
                    }
                }

                // 如果配置中没有找到，使用前5个可用的VM
                if (participantVmIds.isEmpty()) {
                    log.info("配置中未找到参与VM，从VM服务获取前5个可用的虚拟机");
                    try {
                        VmQueryDTO queryDTO = VmQueryDTO.builder()
                            .page(1)
                            .size(5)
                            .build();
                        List<VmListVO> availableVms = vmInstanceService.queryVmList(queryDTO).getRecords();
                        participantVmIds = availableVms.stream()
                            .map(VmListVO::getVmId)
                            .toList();
                        log.info("从VM服务获取到 {} 个可用虚拟机: {}", participantVmIds.size(), participantVmIds);
                    } catch (Exception e) {
                        log.error("获取VM列表失败，使用空列表: {}", e.getMessage());
                        participantVmIds = new ArrayList<>();
                    }
                }
            } else {
                // 后续轮次：基于上一轮次的参与记录
                participantVmIds = vmRoundModelsMapper
                        .selectByTaskIdAndRound(taskId, roundNumber - 1)
                        .stream()
                        .map(model -> model.getVmId())
                        .distinct()
                        .toList();
            }

            if (participantVmIds.isEmpty()) {
                log.warn("没有找到参与轮次{}的客户端: 任务ID={}", roundNumber, taskId);
                return;
            }

            log.info("准备向{}个客户端分发全局模型: 任务ID={}, 轮次={}", 
                    participantVmIds.size(), taskId, roundNumber);

            // 构建全局模型消息
            ProtocolMessage globalModelMessage = buildGlobalModelMessage(
                    taskId, roundNumber, globalModelId, globalMetrics, task.getAlgorithm().getCode());

            // 并行分发给所有参与客户端
            if (aggregationConfig.getModelDistribution().getParallelDistributionCount() > 1) {
                distributeInParallel(participantVmIds, globalModelMessage);
            } else {
                distributeSequentially(participantVmIds, globalModelMessage);
            }

            log.info("全局模型分发完成: 任务ID={}, 轮次={}, 客户端数量={}", 
                    taskId, roundNumber, participantVmIds.size());

        } catch (Exception e) {
            log.error("全局模型分发失败: 任务ID={}, 轮次={}, 错误={}", 
                    taskId, roundNumber, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 构建全局模型分发消息
     */
    private ProtocolMessage buildGlobalModelMessage(String taskId, Integer roundNumber, 
                                                   String globalModelId, Map<String, ?> globalMetrics, 
                                                   String algorithm) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        data.put("globalModelId", globalModelId);
        data.put("algorithm", algorithm);
        data.put("nextRound", roundNumber + 1);
        data.put("distributionTime", Instant.now().toString());

        // 添加全局指标信息
        if (globalMetrics != null && !globalMetrics.isEmpty()) {
            data.put("globalMetrics", globalMetrics);
        }

        // 添加训练指令
        data.put("instruction", "START_NEXT_ROUND");
        data.put("expectedClientsInNextRound", "ALL_PARTICIPANTS");

        return ProtocolMessage.builder()
                .type(ProtocolType.GLOBAL_MODEL_UPDATE)
                .id("global-model-" + System.currentTimeMillis())
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    /**
     * 并行分发全局模型
     */
    private void distributeInParallel(List<String> vmIds, ProtocolMessage globalModelMessage) {
        int parallelCount = aggregationConfig.getModelDistribution().getParallelDistributionCount();
        int batchSize = Math.max(1, vmIds.size() / parallelCount);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < vmIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, vmIds.size());
            List<String> batch = vmIds.subList(i, endIndex);
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (String vmId : batch) {
                    try {
                        sendGlobalModelToVm(vmId, globalModelMessage);
                    } catch (Exception e) {
                        log.error("向客户端{}分发全局模型失败: {}", vmId, e.getMessage());
                    }
                }
            });
            
            futures.add(future);
        }

        // 等待所有分发完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(aggregationConfig.getModelDistribution().getDistributionTimeoutSeconds(), 
                         TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("并行分发全局模型超时或失败: {}", e.getMessage());
        }
    }

    /**
     * 顺序分发全局模型
     */
    private void distributeSequentially(List<String> vmIds, ProtocolMessage globalModelMessage) {
        for (String vmId : vmIds) {
            try {
                sendGlobalModelToVm(vmId, globalModelMessage);
            } catch (Exception e) {
                log.error("向客户端{}分发全局模型失败: {}", vmId, e.getMessage());
            }
        }
    }

    /**
     * 向单个客户端发送全局模型
     */
    private void sendGlobalModelToVm(String vmId, ProtocolMessage globalModelMessage) {
        // 为每个客户端定制消息（设置vmId）
        ProtocolMessage vmMessage = ProtocolMessage.builder()
                .type(globalModelMessage.getType())
                .id(globalModelMessage.getId() + "-" + vmId)
                .timestamp(globalModelMessage.getTimestamp())
                .vmId(vmId)
                .data(globalModelMessage.getData())
                .build();

        // 发送到客户端专用topic
        String topic = "/topic/vm/" + vmId;
        messagingTemplate.convertAndSend(topic, vmMessage);
        
        log.debug("全局模型已发送到客户端: vmId={}, topic={}", vmId, topic);
    }

    /**
     * 主动触发全局模型分发（用于重发或手动分发）
     */
    public void triggerModelDistribution(String taskId, Integer roundNumber) {
        log.info("手动触发全局模型分发: 任务ID={}, 轮次={}", taskId, roundNumber);
        
        try {
            // 查找该轮次的全局模型
            // 这需要在GlobalModelMapper中实现相应方法
            // 暂时使用空的globalModelId和metrics
            distributeGlobalModel(taskId, roundNumber, null, null);
            
        } catch (Exception e) {
            log.error("手动触发全局模型分发失败: 任务ID={}, 轮次={}, 错误={}", 
                    taskId, roundNumber, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取分发状态统计信息
     */
    public Map<String, Object> getDistributionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("parallelDistributionEnabled", 
                aggregationConfig.getModelDistribution().getParallelDistributionCount() > 1);
        stats.put("maxParallelCount", 
                aggregationConfig.getModelDistribution().getParallelDistributionCount());
        stats.put("distributionTimeout", 
                aggregationConfig.getModelDistribution().getDistributionTimeoutSeconds());
        stats.put("compressionEnabled", 
                aggregationConfig.getModelDistribution().isEnableCompression());
        
        return stats;
    }
}