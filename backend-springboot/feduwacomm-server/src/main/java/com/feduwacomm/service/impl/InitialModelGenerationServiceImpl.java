package com.feduwacomm.service.impl;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.InitialModelGenerationDTO;
import com.feduwacomm.entity.InitialModel;
import com.feduwacomm.entity.ModelDistribution;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private static final String MODEL_STORAGE_PATH = "/opt/feduwacomm/models/initial";
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
                .modelType(generationDTO.getModelType())
                .generationMethod(generationDTO.getGenerationMethod())
                .architectureParams(convertToJson(generationDTO.getArchitectureParams()))
                .status("GENERATING")
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .updatedAt(LocalDateTime.now())
                .build();

        // 插入数据库
        initialModelMapper.insertInitialModel(initialModel);

        // 异步执行模型生成
        CompletableFuture.runAsync(() -> {
            try {
                if ("RANDOM".equals(generationDTO.getGenerationMethod())) {
                    generateRandomModel(initialModel, generationDTO.getArchitectureParams());
                } else if ("CUSTOM_UPLOAD".equals(generationDTO.getGenerationMethod())) {
                    // 自定义上传模式下，等待用户上传文件
                    log.info("等待用户上传自定义模型文件: modelId={}", modelId);
                }
            } catch (Exception e) {
                log.error("模型生成失败: modelId={}, error={}", modelId, e.getMessage(), e);
                updateModelStatus(modelId, "FAILED");
            }
        });

        return convertToInfoVO(initialModel, new HashMap<>());
    }

    @Override
    @Transactional
    public InitialModelInfoVO uploadCustomModel(String taskId, String modelType, String filePath, 
                                              String architectureParams, String createdBy) {
        log.info("上传自定义初始模型: taskId={}, modelType={}, filePath={}", taskId, modelType, filePath);

        try {
            // 验证文件存在
            Path modelFilePath = Paths.get(filePath);
            if (!Files.exists(modelFilePath)) {
                throw new RuntimeException("模型文件不存在: " + filePath);
            }

            // 计算文件信息
            long fileSize = Files.size(modelFilePath);
            String checksum = calculateFileChecksum(modelFilePath);

            // 创建模型记录
            String modelId = uuidUtil.generateUuid();
            InitialModel initialModel = InitialModel.builder()
                    .id(modelId)
                    .taskId(taskId)
                    .modelType(modelType)
                    .generationMethod("CUSTOM_UPLOAD")
                    .modelSize(fileSize)
                    .architectureParams(architectureParams)
                    .filePath(filePath)
                    .checksum(checksum)
                    .status("READY")
                    .createdAt(LocalDateTime.now())
                    .createdBy(createdBy)
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 插入数据库
            initialModelMapper.insertInitialModel(initialModel);

            // 发布模型生成完成事件
            Map<String, Object> params = parseJsonToMap(architectureParams);
            publishModelGeneratedEvent(initialModel, params);

            log.info("自定义模型上传成功: modelId={}, fileSize={}, checksum={}", modelId, fileSize, checksum);

            return convertToInfoVO(initialModel, getDistributionStats(modelId));

        } catch (Exception e) {
            log.error("自定义模型上传失败: taskId={}, error={}", taskId, e.getMessage(), e);
            throw new RuntimeException("模型上传失败: " + e.getMessage(), e);
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

        if (!"FAILED".equals(model.getStatus())) {
            throw new RuntimeException("只能重新生成失败的模型");
        }

        // 重置模型状态
        initialModelMapper.updateStatus(modelId, "GENERATING");

        // 异步重新生成
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> architectureParams = parseJsonToMap(model.getArchitectureParams());
                if ("RANDOM".equals(model.getGenerationMethod())) {
                    generateRandomModel(model, architectureParams);
                }
            } catch (Exception e) {
                log.error("模型重新生成失败: modelId={}, error={}", modelId, e.getMessage(), e);
                updateModelStatus(modelId, "FAILED");
            }
        });

        model.setStatus("GENERATING");
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

        // 删除模型文件
        if (StringUtils.hasText(model.getFilePath())) {
            try {
                Files.deleteIfExists(Paths.get(model.getFilePath()));
            } catch (IOException e) {
                log.warn("删除模型文件失败: filePath={}, error={}", model.getFilePath(), e.getMessage());
            }
        }

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
        log.info("验证模型文件完整性: modelId={}", modelId);

        InitialModel model = initialModelMapper.selectById(modelId);
        if (model == null || !StringUtils.hasText(model.getFilePath())) {
            return false;
        }

        try {
            Path modelPath = Paths.get(model.getFilePath());
            if (!Files.exists(modelPath)) {
                return false;
            }

            String currentChecksum = calculateFileChecksum(modelPath);
            return currentChecksum.equals(model.getChecksum());
            
        } catch (Exception e) {
            log.error("模型完整性验证失败: modelId={}, error={}", modelId, e.getMessage());
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
            case "GENERATING":
                return 50; // 正在生成，返回50%
            case "READY":
            case "DISTRIBUTED":
                return 100;
            case "FAILED":
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
        if (model == null || !"GENERATING".equals(model.getStatus())) {
            return false;
        }

        // 更新状态为失败
        return updateModelStatus(modelId, "FAILED");
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
                initialModelMapper.selectByStatus("READY"); // 获取所有就绪模型作为全局统计

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
                .collect(Collectors.groupingBy(InitialModel::getStatus, Collectors.counting()));

        int generating = statusCounts.getOrDefault("GENERATING", 0L).intValue();
        int ready = statusCounts.getOrDefault("READY", 0L).intValue();
        int distributed = statusCounts.getOrDefault("DISTRIBUTED", 0L).intValue();
        int failed = statusCounts.getOrDefault("FAILED", 0L).intValue();

        // 计算成功率
        double successRate = models.isEmpty() ? 0.0 : 
                (double) (ready + distributed) / models.size() * 100;

        // 计算平均生成时间（仅针对已完成的模型）
        double avgGenerationTime = models.stream()
                .filter(m -> m.getCreatedAt() != null && m.getUpdatedAt() != null)
                .filter(m -> !"GENERATING".equals(m.getStatus()))
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
            log.info("开始生成随机模型: modelId={}", model.getId());

            // 模拟模型生成过程
            Thread.sleep(2000 + RANDOM.nextInt(5000)); // 2-7秒随机生成时间

            // 创建模型文件目录
            Path modelDir = Paths.get(MODEL_STORAGE_PATH);
            Files.createDirectories(modelDir);

            // 生成模型文件
            String fileName = String.format("initial_model_%s_%s.bin", 
                    model.getTaskId(), model.getId());
            Path modelFilePath = modelDir.resolve(fileName);

            // 生成随机模型数据（简化实现）
            byte[] modelData = generateRandomModelData(model.getModelType(), params);
            Files.write(modelFilePath, modelData);

            // 计算文件校验和
            String checksum = calculateFileChecksum(modelFilePath);

            // 更新模型信息
            initialModelMapper.updateFileInfo(
                    model.getId(),
                    modelFilePath.toString(),
                    checksum,
                    (long) modelData.length,
                    "READY"
            );

            // 发布模型生成完成事件
            publishModelGeneratedEvent(model, params);

            log.info("随机模型生成完成: modelId={}, filePath={}, size={}", 
                    model.getId(), modelFilePath, modelData.length);

        } catch (Exception e) {
            log.error("随机模型生成失败: modelId={}, error={}", model.getId(), e.getMessage(), e);
            throw new RuntimeException("模型生成失败", e);
        }
    }

    /**
     * 生成随机模型数据
     */
    private byte[] generateRandomModelData(String modelType, Map<String, Object> params) {
        // 基于模型类型和参数生成不同大小的随机数据
        int baseSize = 1024 * 1024; // 1MB base size
        
        // 根据模型类型调整大小
        switch (modelType.toUpperCase()) {
            case "CNN":
                baseSize *= 5; // 5MB
                break;
            case "LSTM":
                baseSize *= 3; // 3MB
                break;
            case "TRANSFORMER":
                baseSize *= 10; // 10MB
                break;
            default:
                baseSize *= 2; // 2MB
        }

        byte[] data = new byte[baseSize];
        RANDOM.nextBytes(data);
        return data;
    }

    /**
     * 计算文件校验和
     */
    private String calculateFileChecksum(Path filePath) throws IOException, NoSuchAlgorithmException {
        byte[] fileData = Files.readAllBytes(filePath);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(fileData);
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
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
                model.getModelType(),
                model.getGenerationMethod(),
                model.getModelSize(),
                model.getFilePath(),
                model.getChecksum(),
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
        String statusDescription = getStatusDescription(model.getStatus());
        
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
                .modelType(model.getModelType())
                .generationMethod(model.getGenerationMethod())
                .modelSize(model.getModelSize())
                .modelSizeFormatted(sizeFormatted)
                .architectureParams(parseJsonToMap(model.getArchitectureParams()))
                .status(model.getStatus())
                .statusDescription(statusDescription)
                .filePath(model.getFilePath())
                .checksum(model.getChecksum())
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