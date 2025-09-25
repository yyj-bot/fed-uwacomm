package com.feduwacomm.service.impl;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.InitialModelGenerationDTO;
import com.feduwacomm.entity.InitialModel;
import com.feduwacomm.entity.ModelDistribution;
import com.feduwacomm.enums.GenerationMethod;
import com.feduwacomm.enums.InitialModelStatus;
import com.feduwacomm.enums.ModelType;
import com.feduwacomm.event.InitialModelGeneratedEvent;
import com.feduwacomm.mapper.InitialModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.service.InitialModelGenerationService;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.InitialModelInfoVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 初始模型生成服务实现
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InitialModelGenerationServiceImpl implements InitialModelGenerationService {

    private final InitialModelMapper initialModelMapper;
    private final ModelDistributionMapper modelDistributionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final UuidUtil uuidUtil;

    private static final Random RANDOM = new SecureRandom();

    @Override
    @Transactional
    public InitialModelInfoVO generateInitialModel(InitialModelGenerationDTO generationDTO, String createdBy) {
        log.info("开始生成初始模型: taskId={}, modelType={}, method={}", 
                generationDTO.getTaskId(), generationDTO.getModelType(), generationDTO.getGenerationMethod());

        // 创建初始模型记录
        String modelId = uuidUtil.generateUuid();
        InitialModel initialModel = InitialModel.builder()
                .id(modelId)
                .taskId(generationDTO.getTaskId())
                .modelType(ModelType.fromCode(generationDTO.getModelType()))
                .generationMethod(GenerationMethod.fromCode(generationDTO.getGenerationMethod()))
                .architectureParams(convertToJson(generationDTO.getArchitectureParams()))
                .status(InitialModelStatus.GENERATING)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .updatedAt(LocalDateTime.now())
                .build();

        // 插入数据库
        initialModelMapper.insertInitialModel(initialModel);

        // 异步执行模型生成
        CompletableFuture.runAsync(() -> {
            try {
                if (GenerationMethod.RANDOM.getCode().equals(generationDTO.getGenerationMethod())) {
                    generateRandomModel(initialModel, generationDTO.getArchitectureParams());
                } else if (GenerationMethod.CUSTOM_UPLOAD.getCode().equals(generationDTO.getGenerationMethod())) {
                    // 自定义上传模式下，等待用户上传文件
                    log.info("等待用户上传自定义模型文件: modelId={}", modelId);
                }
            } catch (Exception e) {
                log.error("模型生成失败: modelId={}, error={}", modelId, e.getMessage(), e);
                updateModelStatus(modelId, InitialModelStatus.FAILED.getCode());
            }
        });

        return convertToInfoVO(initialModel, new HashMap<>());
    }

    @Override
    @Transactional
    public InitialModelInfoVO uploadCustomModel(String taskId, String modelType, String filePath,
                                              String architectureParams, String createdBy) {
        log.info("上传自定义初始模型: taskId={}, modelType={}", taskId, modelType);

        try {
            // 创建模型记录
            String modelId = uuidUtil.generateUuid();
            InitialModel initialModel = InitialModel.builder()
                    .id(modelId)
                    .taskId(taskId)
                    .modelType(ModelType.fromCode(modelType))
                    .generationMethod(GenerationMethod.CUSTOM_UPLOAD)
                    .architectureParams(architectureParams)
                    .status(InitialModelStatus.GENERATING) // 等待用户提供模型数据
                    .createdAt(LocalDateTime.now())
                    .createdBy(createdBy)
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 插入数据库
            initialModelMapper.insertInitialModel(initialModel);

            log.info("自定义模型记录创建成功: modelId={}", modelId);

            return convertToInfoVO(initialModel, getDistributionStats(modelId));

        } catch (Exception e) {
            log.error("自定义模型创建失败: taskId={}, error={}", taskId, e.getMessage(), e);
            throw new RuntimeException("模型创建失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InitialModelInfoVO getModelInfo(String modelId) {
        log.debug("获取模型信息: modelId={}", modelId);
        
        InitialModel model = initialModelMapper.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在: " + modelId);
        }

        Map<String, Object> distributionStats = getDistributionStats(modelId);
        return convertToInfoVO(model, distributionStats);
    }

    @Override
    public List<InitialModelInfoVO> getTaskModels(String taskId) {
        log.debug("获取任务的所有初始模型: taskId={}", taskId);
        
        List<InitialModel> models = initialModelMapper.selectByTaskId(taskId);
        return models.stream()
                .map(model -> convertToInfoVO(model, getDistributionStats(model.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<InitialModelInfoVO> getTaskModelsPaged(String taskId, String status, Integer page, Integer size) {
        log.debug("获取任务的分页模型列表: taskId={}, status={}, page={}, size={}", taskId, status, page, size);
        
        List<InitialModel> allModels = StringUtils.hasText(status) ?
                initialModelMapper.selectByTaskId(taskId).stream()
                        .filter(m -> status.equals(m.getStatus()))
                        .collect(Collectors.toList()) :
                initialModelMapper.selectByTaskId(taskId);

        // 分页处理
        int offset = (page - 1) * size;
        List<InitialModelInfoVO> pagedList = allModels.stream()
                .skip(offset)
                .limit(size)
                .map(model -> convertToInfoVO(model, getDistributionStats(model.getId())))
                .collect(Collectors.toList());

        return PageResult.of(pagedList, (long) allModels.size(), (long) page, (long) size);
    }

    @Override
    @Transactional
    public InitialModelInfoVO regenerateFailedModel(String modelId, String regeneratedBy) {
        log.info("重新生成失败的模型: modelId={}, regeneratedBy={}", modelId, regeneratedBy);

        InitialModel model = initialModelMapper.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在: " + modelId);
        }

        if (!InitialModelStatus.FAILED.equals(model.getStatus())) {
            throw new RuntimeException("只能重新生成失败的模型");
        }

        // 重置模型状态
        initialModelMapper.updateStatus(modelId, InitialModelStatus.GENERATING.getCode());

        // 异步重新生成
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> architectureParams = parseJsonToMap(model.getArchitectureParams());
                if (GenerationMethod.RANDOM.equals(model.getGenerationMethod())) {
                    generateRandomModel(model, architectureParams);
                }
            } catch (Exception e) {
                log.error("模型重新生成失败: modelId={}, error={}", modelId, e.getMessage(), e);
                updateModelStatus(modelId, InitialModelStatus.FAILED.getCode());
            }
        });

        model.setStatus(InitialModelStatus.GENERATING);
        return convertToInfoVO(model, getDistributionStats(modelId));
    }

    @Override
    @Transactional
    public boolean deleteModel(String modelId, String deletedBy) {
        log.info("删除初始模型: modelId={}, deletedBy={}", modelId, deletedBy);

        InitialModel model = initialModelMapper.selectById(modelId);
        if (model == null) {
            return false;
        }

        // 删除分发记录
        modelDistributionMapper.deleteByModelId(modelId);

        // 删除数据库记录
        return initialModelMapper.deleteById(modelId) > 0;
    }

    @Override
    @Transactional
    public int deleteTaskModels(String taskId, String deletedBy) {
        log.info("批量删除任务的所有模型: taskId={}, deletedBy={}", taskId, deletedBy);

        List<InitialModel> models = initialModelMapper.selectByTaskId(taskId);
        int deletedCount = 0;

        for (InitialModel model : models) {
            if (deleteModel(model.getId(), deletedBy)) {
                deletedCount++;
            }
        }

        return deletedCount;
    }

    @Override
    public boolean validateModelIntegrity(String modelId) {
        log.info("验证模型JSON数据完整性: modelId={}", modelId);

        InitialModel model = initialModelMapper.selectById(modelId);
        if (model == null || !StringUtils.hasText(model.getModelData())) {
            return false;
        }

        try {
            // 验证JSON数据是否可以正常解析
            objectMapper.readTree(model.getModelData());
            return true;
        } catch (Exception e) {
            log.error("模型JSON数据验证失败: modelId={}, error={}", modelId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateModelStatus(String modelId, String status) {
        log.debug("更新模型状态: modelId={}, status={}", modelId, status);
        return initialModelMapper.updateStatus(modelId, status) > 0;
    }

    @Override
    public Integer getGenerationProgress(String modelId) {
        // 简单实现：基于状态返回进度
        InitialModel model = initialModelMapper.selectById(modelId);
        if (model == null) {
            return 0;
        }

        switch (model.getStatus()) {
            case GENERATING:
                return 50; // 正在生成，返回50%
            case READY:
            case DISTRIBUTED:
                return 100;
            case FAILED:
                return 0;
            default:
                return 0;
        }
    }

    @Override
    @Transactional
    public boolean cancelGeneration(String modelId, String cancelledBy) {
        log.info("取消模型生成: modelId={}, cancelledBy={}", modelId, cancelledBy);

        InitialModel model = initialModelMapper.selectById(modelId);
        if (model == null || !InitialModelStatus.GENERATING.equals(model.getStatus())) {
            return false;
        }

        // 更新状态为失败
        return updateModelStatus(modelId, InitialModelStatus.FAILED.getCode());
    }

    @Override
    @Transactional
    public int cleanupFailedModels(int daysOld) {
        log.info("清理过期失败模型: daysOld={}", daysOld);

        List<InitialModel> failedModels = initialModelMapper.selectFailedModelsOlderThan(daysOld);
        int cleanedCount = 0;

        for (InitialModel model : failedModels) {
            if (deleteModel(model.getId(), "SYSTEM")) {
                cleanedCount++;
            }
        }

        log.info("清理完成，删除了{}个过期失败模型", cleanedCount);
        return cleanedCount;
    }

    @Override
    public InitialModelInfoVO.ModelGenerationStats getGenerationStats(String taskId) {
        log.debug("获取模型生成统计信息: taskId={}", taskId);

        List<InitialModel> models = StringUtils.hasText(taskId) ?
                initialModelMapper.selectByTaskId(taskId) :
                initialModelMapper.selectByStatus(InitialModelStatus.READY.getCode()); // 获取所有就绪模型作为全局统计

        if (models.isEmpty()) {
            return InitialModelInfoVO.ModelGenerationStats.builder()
                    .totalModels(0)
                    .generating(0)
                    .ready(0)
                    .distributed(0)
                    .failed(0)
                    .successRate(0.0)
                    .avgGenerationTime(0.0)
                    .build();
        }

        // 统计各状态数量
        Map<String, Long> statusCounts = models.stream()
                .collect(Collectors.groupingBy(m -> m.getStatus().getCode(), Collectors.counting()));

        int generating = statusCounts.getOrDefault(InitialModelStatus.GENERATING.getCode(), 0L).intValue();
        int ready = statusCounts.getOrDefault(InitialModelStatus.READY.getCode(), 0L).intValue();
        int distributed = statusCounts.getOrDefault(InitialModelStatus.DISTRIBUTED.getCode(), 0L).intValue();
        int failed = statusCounts.getOrDefault(InitialModelStatus.FAILED.getCode(), 0L).intValue();

        // 计算成功率
        double successRate = models.isEmpty() ? 0.0 : 
                (double) (ready + distributed) / models.size() * 100;

        // 计算平均生成时间（仅针对已完成的模型）
        double avgGenerationTime = models.stream()
                .filter(m -> m.getCreatedAt() != null && m.getUpdatedAt() != null)
                .filter(m -> !InitialModelStatus.GENERATING.equals(m.getStatus()))
                .mapToDouble(m -> ChronoUnit.SECONDS.between(m.getCreatedAt(), m.getUpdatedAt()))
                .average()
                .orElse(0.0);

        return InitialModelInfoVO.ModelGenerationStats.builder()
                .totalModels(models.size())
                .generating(generating)
                .ready(ready)
                .distributed(distributed)
                .failed(failed)
                .successRate(successRate)
                .avgGenerationTime(avgGenerationTime)
                .build();
    }

    /**
     * 生成随机模型
     */
    private void generateRandomModel(InitialModel model, Map<String, Object> params) {
        try {
            log.info("开始生成初始模型JSON数据: modelId={}", model.getId());

            // 模拟模型生成过程
            Thread.sleep(2000 + RANDOM.nextInt(3000)); // 2-5秒随机生成时间

            // 生成真实的机器学习模型参数JSON
            Map<String, Object> modelData = generateRealisticModelParameters(
                    model.getModelType().getCode(), params);

            String modelJsonStr = objectMapper.writeValueAsString(modelData);

            // 直接更新数据库，不涉及文件操作
            initialModelMapper.updateModelData(
                    model.getId(),
                    modelJsonStr,
                    InitialModelStatus.READY.getCode()
            );

            // 发布模型生成完成事件
            publishModelGeneratedEvent(model, params);

            log.info("初始模型JSON数据生成完成: modelId={}, size={}KB",
                    model.getId(), modelJsonStr.length() / 1024);

        } catch (Exception e) {
            log.error("初始模型生成失败: modelId={}, error={}", model.getId(), e.getMessage(), e);
            throw new RuntimeException("模型生成失败", e);
        }
    }

    /**
     * 生成真实的机器学习模型参数 (基于RandomForest结构)
     */
    private Map<String, Object> generateRealisticModelParameters(String modelType, Map<String, Object> params) {
        Map<String, Object> modelData = new HashMap<>();

        // 基础评估指标 (参考 metrics_RandomForest JSON文件)
        modelData.put("r2", -0.001 + Math.random() * 0.002);
        modelData.put("mse", 1.0 + Math.random() * 0.5);
        modelData.put("rmse", Math.sqrt((Double)modelData.get("mse")));
        modelData.put("mae", 0.4 + Math.random() * 0.3);
        modelData.put("median_ae", Math.random() * 1e-13);
        modelData.put("explained_variance", -1e-15 + Math.random() * 2e-15);
        modelData.put("mape", 2.0 + Math.random() * 2.0);
        modelData.put("max_error", 3.0 + Math.random() * 2.0);
        modelData.put("mean_residual", -0.05 + Math.random() * 0.1);
        modelData.put("std_residual", 1.0 + Math.random() * 0.3);
        modelData.put("residual_skewness", -1.0 + Math.random() * 2.0);
        modelData.put("residual_kurtosis", 3.0 + Math.random() * 2.0);

        // 模型特定参数
        Map<String, Object> modelParams = new HashMap<>();
        switch (modelType.toUpperCase()) {
            case "RANDOM_FOREST":
                modelParams.put("n_estimators", 100);
                modelParams.put("max_depth", 10);
                modelParams.put("random_state", 42);
                modelParams.put("min_samples_split", 2);
                modelParams.put("min_samples_leaf", 1);
                break;
            case "NEURAL_NETWORK":
                modelParams.put("hidden_layers", Arrays.asList(128, 64, 32));
                modelParams.put("activation", "relu");
                modelParams.put("learning_rate", 0.001);
                modelParams.put("optimizer", "adam");
                break;
        }

        modelData.put("model_parameters", modelParams);
        modelData.put("is_initial_model", true); // 标记为初始模型
        modelData.put("model_type", modelType.toLowerCase());
        modelData.put("generation_timestamp", System.currentTimeMillis());

        return modelData;
    }


    /**
     * 发布模型生成完成事件
     */
    private void publishModelGeneratedEvent(InitialModel model, Map<String, Object> params) {
        InitialModelGeneratedEvent event = new InitialModelGeneratedEvent(
                this,
                model.getId(),
                model.getTaskId(),
                null, // orchestrationId 如需要可从外部传入
                model.getModelType().getCode(),
                model.getGenerationMethod().getCode(),
                model.getModelSize(),
                null, // filePath 不再使用
                null, // checksum 不再使用
                params,
                model.getCreatedBy()
        );

        eventPublisher.publishEvent(event);
        log.debug("发布模型生成完成事件: {}", event);
    }

    /**
     * 获取模型分发状态统计
     */
    private Map<String, Object> getDistributionStats(String modelId) {
        try {
            return modelDistributionMapper.getDistributionProgress(modelId);
        } catch (Exception e) {
            log.warn("获取分发统计失败: modelId={}, error={}", modelId, e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 转换为信息VO
     */
    private InitialModelInfoVO convertToInfoVO(InitialModel model, Map<String, Object> distributionStats) {
        // 格式化模型大小
        String sizeFormatted = formatFileSize(model.getModelSize());
        
        // 状态描述
        String statusDescription = getStatusDescription(model.getStatus().getCode());
        
        // 分发状态统计
        InitialModelInfoVO.DistributionStats distStats = InitialModelInfoVO.DistributionStats.builder()
                .total((Integer) distributionStats.getOrDefault("total", 0))
                .completed((Integer) distributionStats.getOrDefault("completed", 0))
                .inProgress((Integer) distributionStats.getOrDefault("inProgress", 0))
                .failed((Integer) distributionStats.getOrDefault("failed", 0))
                .progressPercentage(calculateProgressPercentage(distributionStats))
                .build();

        return InitialModelInfoVO.builder()
                .id(model.getId())
                .taskId(model.getTaskId())
                .modelType(model.getModelType().getCode())
                .generationMethod(model.getGenerationMethod().getCode())
                .modelSize(model.getModelSize())
                .modelSizeFormatted(sizeFormatted)
                .architectureParams(parseJsonToMap(model.getArchitectureParams()))
                .status(model.getStatus().getCode())
                .statusDescription(statusDescription)
                .createdAt(model.getCreatedAt())
                .createdBy(model.getCreatedBy())
                .updatedAt(model.getUpdatedAt())
                .distributionStats(distStats)
                .build();
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long sizeInBytes) {
        if (sizeInBytes == null || sizeInBytes == 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = sizeInBytes.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * 获取状态描述
     */
    private String getStatusDescription(String status) {
        switch (status) {
            case "GENERATING":
                return "正在生成中";
            case "READY":
                return "已就绪";
            case "DISTRIBUTED":
                return "已分发";
            case "FAILED":
                return "生成失败";
            default:
                return "未知状态";
        }
    }

    /**
     * 计算分发进度百分比
     */
    private Double calculateProgressPercentage(Map<String, Object> stats) {
        Integer total = (Integer) stats.getOrDefault("total", 0);
        Integer completed = (Integer) stats.getOrDefault("completed", 0);
        
        if (total == 0) {
            return 0.0;
        }
        
        return (double) completed / total * 100;
    }

    /**
     * 转换为JSON字符串
     */
    private String convertToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("转换为JSON失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析JSON为Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析JSON失败: json={}, error={}", json, e.getMessage());
            return new HashMap<>();
        }
    }
}