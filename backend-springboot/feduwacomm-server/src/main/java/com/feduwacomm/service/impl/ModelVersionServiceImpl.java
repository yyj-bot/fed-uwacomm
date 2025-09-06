package com.feduwacomm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.exception.BusinessException;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.ModelVersion;
import com.feduwacomm.mapper.ModelVersionMapper;
import com.feduwacomm.service.ModelVersionService;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型版本管理服务实现类
 */
@Service
@Transactional
public class ModelVersionServiceImpl implements ModelVersionService {

    private static final Logger log = LoggerFactory.getLogger(ModelVersionServiceImpl.class);

    @Autowired
    private ModelVersionMapper modelVersionMapper;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UuidUtil uuidUtil;

    @Value("${app.file.upload-path:/tmp/feduwacomm/models}")
    private String uploadPath;

    // 支持的模型文件格式
    private static final Set<String> SUPPORTED_FORMATS = Set.of(
        ".pth", ".pt", ".h5", ".pb", ".savedmodel", ".onnx", ".pkl", ".pickle", ".joblib"
    );

    @Override
    public ModelUploadResponseVO uploadModel(String taskId, Integer roundNumber, String description, 
                                           String parameters, MultipartFile file) {
        log.info("开始上传模型: taskId={}, roundNumber={}", taskId, roundNumber);
        
        try {
            // 验证文件
            validateModelFile(file);
            
            // 检查是否已存在相同轮次的模型
            ModelVersion existing = modelVersionMapper.selectByTaskIdAndRound(taskId, roundNumber);
            if (existing != null) {
                throw new BusinessException("该轮次的模型已存在");
            }
            
            // 保存文件
            String filePath = saveModelFile(file, taskId, roundNumber);
            
            // 创建模型版本记录
            ModelVersion modelVersion = ModelVersion.builder()
                .id(uuidUtil.generateUuid())
                .taskId(taskId)
                .roundNumber(roundNumber)
                .status("UPLOADED")
                .description(description)
                .filePath(filePath)
                .fileSize(file.getSize())
                .fileFormat(getFileExtension(file.getOriginalFilename()))
                .parameters(parameters)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            int result = modelVersionMapper.insert(modelVersion);
            if (result == 0) {
                throw new BusinessException("模型版本保存失败");
            }
            
            log.info("模型上传成功: modelId={}", modelVersion.getId());
            
            // 构建响应
            Map<String, Object> parametersMap = null;
            if (StringUtils.hasText(parameters)) {
                try {
                    parametersMap = objectMapper.readValue(parameters, Map.class);
                } catch (Exception e) {
                    log.warn("解析parameters失败: {}", e.getMessage());
                }
            }
            
            return ModelUploadResponseVO.builder()
                .modelId(modelVersion.getId())
                .taskId(taskId)
                .roundNumber(roundNumber)
                .status("UPLOADED")
                .description(description)
                .parameters(parametersMap)
                .createdAt(modelVersion.getCreatedAt())
                .build();
                
        } catch (Exception e) {
            log.error("模型上传失败: taskId={}, roundNumber={}, error={}", taskId, roundNumber, e.getMessage());
            throw new BusinessException("模型上传失败: " + e.getMessage());
        }
    }

    @Override
    public ModelBatchUploadResponseVO batchUploadModels(ModelBatchUploadDTO batchUploadDTO) {
        log.info("开始批量上传模型: taskId={}, modelCount={}", 
                batchUploadDTO.getTaskId(), batchUploadDTO.getModels().size());
        
        List<ModelBatchUploadResponseVO.ModelBatchUploadResultVO> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        
        for (ModelUploadDTO model : batchUploadDTO.getModels()) {
            try {
                String parametersJson = null;
                if (model.getParameters() != null && !model.getParameters().isEmpty()) {
                    parametersJson = objectMapper.writeValueAsString(model.getParameters());
                }
                
                ModelUploadResponseVO response = uploadModel(
                    batchUploadDTO.getTaskId(),
                    model.getRoundNumber(),
                    model.getDescription(),
                    parametersJson,
                    model.getFile()
                );
                
                results.add(ModelBatchUploadResponseVO.ModelBatchUploadResultVO.builder()
                    .modelId(response.getModelId())
                    .status("UPLOADED")
                    .message("上传成功")
                    .build());
                successCount++;
                
            } catch (Exception e) {
                results.add(ModelBatchUploadResponseVO.ModelBatchUploadResultVO.builder()
                    .modelId(null)
                    .status("FAILED")
                    .message(e.getMessage())
                    .build());
                failedCount++;
                log.error("批量上传中单个模型失败: roundNumber={}, error={}", 
                         model.getRoundNumber(), e.getMessage());
            }
        }
        
        return ModelBatchUploadResponseVO.builder()
            .successCount(successCount)
            .failedCount(failedCount)
            .models(results)
            .build();
    }

    @Override
    public PageResponseDTO<ModelVersionVO> getModelVersions(ModelQueryDTO queryDTO) {
        log.info("查询模型版本列表: {}", queryDTO);
        
        int offset = (queryDTO.getPage() - 1) * queryDTO.getSize();
        
        List<ModelVersion> models = modelVersionMapper.selectByPage(
            offset, queryDTO.getSize(),
            queryDTO.getTaskId(), queryDTO.getRoundNumber(),
            queryDTO.getStatus(), queryDTO.getSort(), queryDTO.getOrder()
        );
        
        int total = modelVersionMapper.countByCondition(
            queryDTO.getTaskId(), queryDTO.getRoundNumber(), queryDTO.getStatus()
        );
        
        List<ModelVersionVO> modelVOs = models.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return PageResponseDTO.<ModelVersionVO>builder()
            .total((long)total)
            .pages((long)((total + queryDTO.getSize() - 1) / queryDTO.getSize()))
            .current(queryDTO.getPage())
            .size(queryDTO.getSize())
            .records(modelVOs)
            .build();
    }

    @Override
    public ModelVersionVO getModelVersionById(String modelId) {
        log.info("查询模型版本详情: modelId={}", modelId);
        
        ModelVersion modelVersion = modelVersionMapper.selectById(modelId);
        if (modelVersion == null) {
            throw new BusinessException("模型版本不存在");
        }
        
        return convertToVO(modelVersion);
    }

    @Override
    public ModelTaskVersionsVO getModelVersionsByTaskId(String taskId, Integer roundNumber, 
                                                       String status, String sort, String order) {
        log.info("查询任务模型版本: taskId={}", taskId);
        
        List<ModelVersion> models = modelVersionMapper.selectByTaskId(taskId);
        
        // TODO: 查询任务名称
        String taskName = "水声分类任务"; // 需要从任务服务获取
        
        List<ModelVersionVO> versions = models.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return ModelTaskVersionsVO.builder()
            .taskId(taskId)
            .taskName(taskName)
            .totalModels(versions.size())
            .versions(versions)
            .build();
    }

    // 其他方法的基本实现（简化版，实际应用中需要更完善的实现）
    @Override
    public ModelEvaluateResponseVO evaluateModel(ModelEvaluateDTO evaluateDTO) {
        // 模拟评估过程
        log.info("开始模型评估: modelId={}", evaluateDTO.getModelId());
        
        ModelVersion modelVersion = modelVersionMapper.selectById(evaluateDTO.getModelId());
        if (modelVersion == null) {
            throw new BusinessException("模型版本不存在");
        }
        
        // 模拟评估结果
        Map<String, BigDecimal> metrics = new HashMap<>();
        metrics.put("accuracy", new BigDecimal("0.8500"));
        metrics.put("loss", new BigDecimal("0.1234"));
        metrics.put("precision", new BigDecimal("0.8200"));
        metrics.put("recall", new BigDecimal("0.8300"));
        metrics.put("f1", new BigDecimal("0.8250"));
        
        return ModelEvaluateResponseVO.builder()
            .modelId(evaluateDTO.getModelId())
            .evaluationId("eval_" + System.currentTimeMillis())
            .metrics(metrics)
            .evaluationTime(15.5)
            .testSamples(1000)
            .status("COMPLETED")
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Override
    public List<ModelEvaluateResponseVO> batchEvaluateModels(ModelBatchEvaluateDTO batchEvaluateDTO) {
        // 简化实现
        return new ArrayList<>();
    }

    @Override
    public PageResponseDTO<ModelEvaluateResponseVO> getEvaluationResults(String modelId, String taskId, 
                                                                        String evaluationId, int page, int size) {
        // 简化实现
        return PageResponseDTO.<ModelEvaluateResponseVO>builder()
            .total(0L)
            .pages(0L)
            .current(page)
            .size(size)
            .records(new ArrayList<>())
            .build();
    }

    @Override
    public ModelDeployResponseVO deployModel(ModelDeployDTO deployDTO) {
        // 模拟部署过程
        log.info("开始模型部署: modelId={}", deployDTO.getModelId());
        
        return ModelDeployResponseVO.builder()
            .deploymentId("deploy_" + System.currentTimeMillis())
            .modelId(deployDTO.getModelId())
            .deploymentName(deployDTO.getDeploymentName())
            .targetVms(deployDTO.getTargetVms())
            .status("DEPLOYED")
            .deploymentConfig(deployDTO.getDeploymentConfig())
            .endpoints(Arrays.asList("http://vm_1:8080/predict", "http://vm_2:8080/predict"))
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Override
    public Map<String, Object> getDeploymentStatus(String deploymentId) {
        Map<String, Object> status = new HashMap<>();
        status.put("deploymentId", deploymentId);
        status.put("status", "RUNNING");
        return status;
    }

    @Override
    public PageResponseDTO<Map<String, Object>> getDeploymentList(String modelId, String status, 
                                                                 int page, int size) {
        return PageResponseDTO.<Map<String, Object>>builder()
            .total(0L)
            .pages(0L)
            .current(page)
            .size(size)
            .records(new ArrayList<>())
            .build();
    }

    @Override
    public Map<String, Object> rollbackModel(ModelRollbackDTO rollbackDTO) {
        Map<String, Object> result = new HashMap<>();
        result.put("rollbackId", "rollback_" + System.currentTimeMillis());
        result.put("status", "COMPLETED");
        return result;
    }

    @Override
    public PageResponseDTO<Map<String, Object>> getRollbackHistory(String deploymentId, int page, int size) {
        return PageResponseDTO.<Map<String, Object>>builder()
            .total(0L)
            .pages(0L)
            .current(page)
            .size(size)
            .records(new ArrayList<>())
            .build();
    }

    @Override
    public byte[] downloadModel(String modelId, String format, Boolean compressed) {
        // 简化实现，返回空数组
        return new byte[0];
    }

    @Override
    public byte[] batchDownloadModels(ModelBatchDownloadDTO batchDownloadDTO) {
        // 简化实现，返回空数组
        return new byte[0];
    }

    @Override
    public Map<String, Object> deleteModelVersion(String modelId, Boolean force, Boolean deleteFile) {
        log.info("删除模型版本: modelId={}, force={}, deleteFile={}", modelId, force, deleteFile);
        
        ModelVersion modelVersion = modelVersionMapper.selectById(modelId);
        if (modelVersion == null) {
            throw new BusinessException("模型版本不存在");
        }
        
        // 检查是否可以删除
        if (!force && "DEPLOYED".equals(modelVersion.getStatus())) {
            throw new BusinessException("已部署的模型无法删除，请先下线或使用强制删除");
        }
        
        // 删除数据库记录
        int result = modelVersionMapper.deleteById(modelId);
        if (result == 0) {
            throw new BusinessException("删除失败");
        }
        
        // 删除文件
        if (deleteFile && StringUtils.hasText(modelVersion.getFilePath())) {
            try {
                Files.deleteIfExists(Paths.get(modelVersion.getFilePath()));
            } catch (IOException e) {
                log.warn("删除模型文件失败: {}", e.getMessage());
            }
        }
        
        Map<String, Object> result_map = new HashMap<>();
        result_map.put("modelId", modelId);
        result_map.put("deletedAt", LocalDateTime.now());
        return result_map;
    }

    @Override
    public Map<String, Object> batchDeleteModels(ModelBatchDeleteDTO batchDeleteDTO) {
        List<String> successIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();
        
        for (String modelId : batchDeleteDTO.getModelIds()) {
            try {
                deleteModelVersion(modelId, batchDeleteDTO.getForce(), batchDeleteDTO.getDeleteFile());
                successIds.add(modelId);
            } catch (Exception e) {
                failedIds.add(modelId);
                log.error("批量删除模型失败: modelId={}, error={}", modelId, e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successIds.size());
        result.put("failedCount", failedIds.size());
        return result;
    }

    @Override
    public ModelStatisticsVO getModelStatistics(String taskId, String timeRange) {
        log.info("查询模型统计信息: taskId={}, timeRange={}", taskId, timeRange);
        
        // 计算时间范围
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        switch (timeRange) {
            case "30d":
                startDate = endDate.minusDays(30);
                break;
            case "90d":
                startDate = endDate.minusDays(90);
                break;
            default:
                startDate = endDate.minusDays(7);
        }
        
        // 查询统计数据
        int totalModels = StringUtils.hasText(taskId) ? 
            modelVersionMapper.countByTaskId(taskId) : 
            modelVersionMapper.countAll();
            
        String avgAccuracy = modelVersionMapper.selectAverageAccuracy(taskId);
        String avgLoss = modelVersionMapper.selectAverageLoss(taskId);
        
        // 查询趋势数据
        List<Object> uploadTrendData = modelVersionMapper.selectUploadTrendByDateRange(
            startDate.toString(), endDate.toString(), taskId);
            
        List<ModelVersion> accuracyTrendData = StringUtils.hasText(taskId) ?
            modelVersionMapper.selectAccuracyTrendByTaskId(taskId) : new ArrayList<>();
        
        // 构建响应
        List<ModelStatisticsVO.UploadTrendVO> uploadTrend = uploadTrendData.stream()
            .map(data -> {
                Map<String, Object> map = (Map<String, Object>) data;
                return ModelStatisticsVO.UploadTrendVO.builder()
                    .date(map.get("date").toString())
                    .count(((Number) map.get("count")).intValue())
                    .build();
            })
            .collect(Collectors.toList());
            
        List<ModelStatisticsVO.AccuracyTrendVO> accuracyTrend = accuracyTrendData.stream()
            .map(model -> ModelStatisticsVO.AccuracyTrendVO.builder()
                .roundNumber(model.getRoundNumber())
                .accuracy(model.getAccuracy())
                .build())
            .collect(Collectors.toList());
        
        return ModelStatisticsVO.builder()
            .totalModels(totalModels)
            .averageAccuracy(StringUtils.hasText(avgAccuracy) ? new BigDecimal(avgAccuracy) : null)
            .averageLoss(StringUtils.hasText(avgLoss) ? new BigDecimal(avgLoss) : null)
            .uploadTrend(uploadTrend)
            .accuracyTrend(accuracyTrend)
            .build();
    }

    @Override
    public Map<String, Object> getTaskModelStatistics(String taskId) {
        log.info("查询任务模型统计: taskId={}", taskId);
        
        int totalRounds = modelVersionMapper.countByTaskId(taskId);
        ModelVersion bestModel = modelVersionMapper.selectBestAccuracyByTaskId(taskId);
        String avgAccuracy = modelVersionMapper.selectAverageAccuracy(taskId);
        
        Map<String, Object> performanceMetrics = new HashMap<>();
        if (bestModel != null) {
            performanceMetrics.put("bestAccuracy", bestModel.getAccuracy());
            performanceMetrics.put("bestRound", bestModel.getRoundNumber());
        }
        if (StringUtils.hasText(avgAccuracy)) {
            performanceMetrics.put("averageAccuracy", new BigDecimal(avgAccuracy));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("taskName", "水声分类任务");
        result.put("totalRounds", totalRounds);
        result.put("completedRounds", totalRounds);
        result.put("performanceMetrics", performanceMetrics);
        
        return result;
    }

    // 私有辅助方法
    private void validateModelFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("模型文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException("模型文件名不能为空");
        }
        
        String extension = getFileExtension(originalFilename);
        if (!SUPPORTED_FORMATS.contains(extension.toLowerCase())) {
            throw new BusinessException("不支持的模型文件格式: " + extension);
        }
        
        // 检查文件大小（最大100MB）
        long maxSize = 100 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException("模型文件过大，最大支持100MB");
        }
    }
    
    private String saveModelFile(MultipartFile file, String taskId, Integer roundNumber) throws IOException {
        // 创建目录结构
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path uploadDir = Paths.get(uploadPath, taskId, dateStr);
        Files.createDirectories(uploadDir);
        
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = String.format("model_round_%d_%s%s", 
                                       roundNumber, 
                                       System.currentTimeMillis(), 
                                       extension);
        
        Path filePath = uploadDir.resolve(filename);
        file.transferTo(filePath.toFile());
        
        return filePath.toString();
    }
    
    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        return lastDotIndex >= 0 ? filename.substring(lastDotIndex) : "";
    }
    
    private ModelVersionVO convertToVO(ModelVersion modelVersion) {
        ModelVersionVO vo = new ModelVersionVO();
        BeanUtils.copyProperties(modelVersion, vo);
        vo.setModelId(modelVersion.getId());
        
        // 解析JSON字段
        if (StringUtils.hasText(modelVersion.getMetrics())) {
            try {
                Map<String, Object> metrics = objectMapper.readValue(modelVersion.getMetrics(), Map.class);
                vo.setMetrics(metrics);
            } catch (Exception e) {
                log.warn("解析metrics失败: {}", e.getMessage());
            }
        }
        
        if (StringUtils.hasText(modelVersion.getParameters())) {
            try {
                Map<String, Object> parameters = objectMapper.readValue(modelVersion.getParameters(), Map.class);
                vo.setParameters(parameters);
            } catch (Exception e) {
                log.warn("解析parameters失败: {}", e.getMessage());
            }
        }
        
        return vo;
    }
}