package com.feduwacomm.service;

import com.feduwacomm.config.AggregationConfig;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.entity.ModelDistribution;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.event.AggregationCompletedEvent;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.GlobalModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.mapper.InitialModelMapper;
import com.feduwacomm.mapper.VmRoundModelsMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.service.RoundStateManager;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.dto.VmQueryDTO;
import com.feduwacomm.vo.VmListVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final GlobalModelMapper globalModelMapper;
    private final ModelDistributionMapper modelDistributionMapper;
    private final AggregationConfig aggregationConfig;
    private final ObjectMapper objectMapper;
    private final VmInstanceService vmInstanceService;
    private final TaskParticipantsMapper taskParticipantsMapper;
    private final RoundStateManager roundStateManager;
    private final UuidUtil uuidUtil;
    private final InitialModelMapper initialModelMapper;

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
        distributeGlobalModel(taskId, roundNumber, globalModelId, globalMetrics, null, null);
    }

    public void distributeGlobalModel(String taskId, Integer roundNumber,
                                      String globalModelId, Map<String, ?> globalMetrics,
                                      List<String> explicitVmIds) {
        distributeGlobalModel(taskId, roundNumber, globalModelId, globalMetrics, explicitVmIds, null);
    }

    public void distributeGlobalModel(String taskId, Integer roundNumber,
                                      String globalModelId, Map<String, ?> globalMetrics,
                                      List<String> explicitVmIds,
                                      Map<String, String> distributionIdsByVm) {
        GlobalModel persistedGlobalModel = null;
        String effectiveModelId = globalModelId;
        Map<String, ProtocolMessage> messagesByVm = new LinkedHashMap<>();
        try {
            // 获取任务信息
            FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("任务不存在: " + taskId);
            }

            // 获取参与该轮的所有客户端
            List<String> participantVmIds;

            if (explicitVmIds != null && !explicitVmIds.isEmpty()) {
                participantVmIds = new ArrayList<>(explicitVmIds);
            } else if (roundNumber == null || roundNumber <= 1) {
                // 第1轮或初始化：使用任务的所有参与VM（从任务配置中获取）
                participantVmIds = taskParticipantsMapper.selectParticipantsByTaskId(taskId).stream()
                        .filter(p -> p.getDatasetStatus() != null && p.getDatasetStatus().equalsIgnoreCase("COMPLETED"))
                        .map(TaskParticipant::getVmId)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList();

                if (participantVmIds.isEmpty()) {
                    log.warn("任务{} 的参与者尚未就绪，尝试回退到任务配置", taskId);
                    participantVmIds = resolveVmIdsFromTaskConfig(task);
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

            if (roundNumber != null && roundNumber > 0) {
                roundStateManager.markDistributionStarted(taskId, roundNumber, participantVmIds.size());
            }

            if (StringUtils.hasText(effectiveModelId)) {
                persistedGlobalModel = globalModelMapper.selectById(effectiveModelId);
            } else if (roundNumber != null) {
                persistedGlobalModel = globalModelMapper.selectByTaskIdAndRound(taskId, roundNumber);
                if (persistedGlobalModel != null) {
                    effectiveModelId = persistedGlobalModel.getId();
                }
            }

            if (persistedGlobalModel != null) {
                globalModelMapper.updateDistributionStatus(
                        persistedGlobalModel.getId(),
                        "DISTRIBUTING",
                        null,
                        null,
                        LocalDateTime.now());
            }

            log.info("准备向{}个客户端分发全局模型: 任务ID={}, 轮次={}",
                    participantVmIds.size(), taskId, roundNumber);

            String downloadUrl = buildDownloadUrl(taskId, roundNumber, effectiveModelId);
            for (String vmId : participantVmIds) {
                String distributionId = resolveDistributionId(vmId, effectiveModelId, distributionIdsByVm);
                if (StringUtils.hasText(distributionId)) {
                    modelDistributionMapper.updateDistributionStatus(
                            distributionId,
                            "IN_PROGRESS",
                            LocalDateTime.now(),
                            null
                    );
                }
                ProtocolMessage vmSpecificMessage = buildGlobalModelMessage(
                        taskId,
                        roundNumber,
                        effectiveModelId,
                        globalMetrics,
                        task.getAlgorithm() != null ? task.getAlgorithm().getCode() : null,
                        downloadUrl,
                        distributionId,
                        vmId);
                messagesByVm.put(vmId, vmSpecificMessage);
            }

            // 并行分发给所有参与客户端
            if (aggregationConfig.getModelDistribution().getParallelDistributionCount() > 1) {
                distributeInParallel(messagesByVm);
            } else {
                distributeSequentially(messagesByVm);
            }

            if (persistedGlobalModel != null) {
                String distributedVmsJson = null;
                try {
                    distributedVmsJson = objectMapper.writeValueAsString(participantVmIds);
                } catch (Exception jsonException) {
                    log.warn("序列化分发目标列表失败: taskId={}, round={}, error={}",
                            taskId, roundNumber, jsonException.getMessage(), jsonException);
                }
                globalModelMapper.updateDistributionStatus(
                        persistedGlobalModel.getId(),
                        "DISTRIBUTED",
                        distributedVmsJson,
                        LocalDateTime.now(),
                        LocalDateTime.now());
            }

            log.info("全局模型分发完成: 任务ID={}, 轮次={}, 客户端数量={}",
                    taskId, roundNumber, participantVmIds.size());

        } catch (Exception e) {
            if (persistedGlobalModel != null) {
                globalModelMapper.updateDistributionStatus(
                        persistedGlobalModel.getId(),
                        "FAILED",
                        null,
                        null,
                        LocalDateTime.now());
            }
            log.error("全局模型分发失败: 任务ID={}, 轮次={}, 错误={}",
                    taskId, roundNumber, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 构建全局模型分发消息（符合v1.5.1协议规范）
     */
    private ProtocolMessage buildGlobalModelMessage(String taskId, Integer roundNumber,
                                                   String globalModelId, Map<String, ?> globalMetrics,
                                                   String algorithm, String downloadUrl,
                                                   String distributionId, String vmId) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("roundNumber", roundNumber);
        if (StringUtils.hasText(distributionId)) {
            data.put("distributionId", distributionId);
        }

        // 🆕 构建globalModel嵌套对象（符合v1.5.1协议规范）
        Map<String, Object> globalModel = new HashMap<>();
        globalModel.put("modelId", StringUtils.hasText(globalModelId)
                ? globalModelId
                : "model-" + taskId + "-r" + roundNumber);
        globalModel.put("version", "1.0.0");
        if (StringUtils.hasText(downloadUrl)) {
            globalModel.put("downloadUrl", downloadUrl);
        }
        if (StringUtils.hasText(distributionId)) {
            globalModel.put("distributionId", distributionId);
        }

        // 将globalMetrics作为模型参数
        if (globalMetrics != null && !globalMetrics.isEmpty()) {
            globalModel.put("parameters", globalMetrics);
        } else {
            globalModel.put("parameters", new HashMap<>());
        }

        // 添加校验和（可选）
        // globalModel.put("checksum", "sha256:...");

        data.put("globalModel", globalModel);

        // 🆕 添加aggregationInfo（符合v1.5.1协议规范）
        Map<String, Object> aggregationInfo = new HashMap<>();
        aggregationInfo.put("aggregationMethod", "FEDERATED_AVERAGING");

        if (globalMetrics != null && !globalMetrics.isEmpty()) {
            // 使用get()方法避免通配符类型问题
            aggregationInfo.put("participantCount",
                globalMetrics.get("participantCount") != null ? globalMetrics.get("participantCount") : 0);
            aggregationInfo.put("globalAccuracy",
                globalMetrics.get("globalAccuracy") != null ? globalMetrics.get("globalAccuracy") : 0.0);
            aggregationInfo.put("globalLoss",
                globalMetrics.get("globalLoss") != null ? globalMetrics.get("globalLoss") : 0.0);
            aggregationInfo.put("convergenceScore",
                globalMetrics.get("convergenceScore") != null ? globalMetrics.get("convergenceScore") : 0.0);
        } else {
            aggregationInfo.put("participantCount", 0);
            aggregationInfo.put("globalAccuracy", 0.0);
            aggregationInfo.put("globalLoss", 0.0);
            aggregationInfo.put("convergenceScore", 0.0);
        }

        data.put("aggregationInfo", aggregationInfo);

        // 🆕 添加nextRoundConfig（符合v1.5.1协议规范）
        Map<String, Object> nextRoundConfig = new HashMap<>();
        nextRoundConfig.put("startTime", Instant.now().plusSeconds(5).toString());
        nextRoundConfig.put("learningRate", 0.001);  // 可从任务配置获取
        data.put("nextRoundConfig", nextRoundConfig);

        return ProtocolMessage.builder()
                .type(ProtocolType.GLOBAL_MODEL_BROADCAST)
                .id("global-model-" + System.currentTimeMillis() + "-" + vmId)
                .timestamp(Instant.now())
                .vmId(vmId)
                .data(data)
                .build();
    }

    /**
     * 并行分发全局模型
     */
    private void distributeInParallel(Map<String, ProtocolMessage> messagesByVm) {
        List<String> vmIds = new ArrayList<>(messagesByVm.keySet());
        int parallelCount = aggregationConfig.getModelDistribution().getParallelDistributionCount();
        int batchSize = Math.max(1, vmIds.size() / parallelCount);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < vmIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, vmIds.size());
            List<String> batch = vmIds.subList(i, endIndex);
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (String vmId : batch) {
                    try {
                        ProtocolMessage message = messagesByVm.get(vmId);
                        if (message == null) {
                            log.warn("缺少针对VM {} 的全局模型消息", vmId);
                            continue;
                        }
                        sendGlobalModelToVm(vmId, message);
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
    private void distributeSequentially(Map<String, ProtocolMessage> messagesByVm) {
        for (Map.Entry<String, ProtocolMessage> entry : messagesByVm.entrySet()) {
            String vmId = entry.getKey();
            ProtocolMessage message = entry.getValue();
            try {
                if (message == null) {
                    log.warn("跳过缺少消息体的全局模型分发: vmId={}", vmId);
                    continue;
                }
                sendGlobalModelToVm(vmId, message);
            } catch (Exception e) {
                log.error("向客户端{}分发全局模型失败: {}", vmId, e.getMessage());
            }
        }
    }

    /**
     * 向单个客户端发送全局模型
     */
    private void sendGlobalModelToVm(String vmId, ProtocolMessage vmMessage) {
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

    private List<String> resolveVmIdsFromTaskConfig(FederatedTask task) {
        List<String> participantVmIds = new ArrayList<>();
        if (task == null) {
            return participantVmIds;
        }

        String configJson = task.getConfig();
        if (configJson != null && !configJson.trim().isEmpty()) {
            try {
                Map<String, Object> config = objectMapper.readValue(configJson, Map.class);
                Object vmIds = config.get("participantVmIds");
                if (vmIds instanceof List<?> list) {
                    participantVmIds = list.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .filter(StringUtils::hasText)
                            .distinct()
                            .collect(Collectors.toCollection(ArrayList::new));
                } else if (vmIds instanceof String vmIdString && StringUtils.hasText(vmIdString)) {
                    participantVmIds = new ArrayList<>(Arrays.asList(
                            objectMapper.readValue(vmIdString, String[].class)));
                }
            } catch (Exception e) {
                log.warn("解析任务配置中的参与VM ID失败: {}", e.getMessage());
            }
        }

        if (!participantVmIds.isEmpty()) {
            return participantVmIds;
        }

        log.info("配置中未找到参与VM，从VM服务获取前5个可用虚拟机");
        try {
            VmQueryDTO queryDTO = VmQueryDTO.builder()
                    .page(1)
                    .size(5)
                    .build();
            List<VmListVO> availableVms = vmInstanceService.queryVmList(queryDTO).getList();
            participantVmIds = availableVms.stream()
                    .map(VmListVO::getVmId)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
            log.info("从VM服务获取到 {} 个可用虚拟机: {}", participantVmIds.size(), participantVmIds);
        } catch (Exception e) {
            log.error("获取VM列表失败，使用空列表: {}", e.getMessage());
            participantVmIds = new ArrayList<>();
        }

        return participantVmIds;
    }

    private String resolveDistributionId(String vmId, String modelId, Map<String, String> distributionIdsByVm) {
        if (!StringUtils.hasText(vmId)) {
            return null;
        }
        if (distributionIdsByVm != null && distributionIdsByVm.containsKey(vmId)) {
            return distributionIdsByVm.get(vmId);
        }
        if (!StringUtils.hasText(modelId)) {
            return null;
        }
        ModelDistribution record = modelDistributionMapper.selectByModelIdAndVmId(modelId, vmId);
        if (record != null) {
            return record.getId();
        }
        // 仅对初始模型创建分发记录，避免全局模型触发外键约束错误
        if (initialModelMapper.selectById(modelId) == null) {
            log.debug("跳过模型分发记录创建: modelId={} 非初始模型，轮次分发使用内存distributionId", modelId);
            return null;
        }
        try {
            ModelDistribution newRecord = ModelDistribution.builder()
                    .id(uuidUtil.generateUuid())
                    .modelId(modelId)
                    .vmId(vmId)
                    .distributionStatus("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            modelDistributionMapper.insertModelDistribution(newRecord);
            log.info("创建轮次模型分发记录: modelId={}, vmId={}, recordId={}", modelId, vmId, newRecord.getId());
            return newRecord.getId();
        } catch (Exception ex) {
            log.warn("创建轮次模型分发记录失败，尝试查询现有记录: modelId={}, vmId={}, error={}", modelId, vmId, ex.getMessage());
            ModelDistribution fallback = modelDistributionMapper.selectByModelIdAndVmId(modelId, vmId);
            return fallback != null ? fallback.getId() : null;
        }
    }

    private String buildDownloadUrl(String taskId, Integer roundNumber, String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return null;
        }
        if (roundNumber == null || roundNumber <= 0) {
            return "/api/model/initial/" + modelId + "/download";
        }
        return "/api/federated/models/" + taskId + "/global/round/" + roundNumber;
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
