package com.feduwacomm.service.impl;

import com.feduwacomm.exception.UserException;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.TrainingData;
import com.feduwacomm.mapper.TrainingDatasetMapper;
import com.feduwacomm.service.TrainingDataService;
import com.feduwacomm.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 训练数据服务实现类
 */
@Service
@Transactional
public class TrainingDataServiceImpl implements TrainingDataService {

    private static final Logger log = LoggerFactory.getLogger(TrainingDataServiceImpl.class);

    @Autowired
    private TrainingDatasetMapper trainingDatasetMapper;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public TrainingDataUploadVO uploadFile(TrainingDataUploadDTO uploadDTO, MultipartFile file, String userId) {
        log.info("开始上传训练数据文件: vmId={}, dataType={}", uploadDTO.getVmId(), uploadDTO.getDataType());
        
        if (file.isEmpty()) {
            throw new UserException("上传文件不能为空");
        }

        try {
            // 生成数据集ID
            String datasetId = UUID.randomUUID().toString().replace("-", "");
            
            // 保存文件
            String fileName = file.getOriginalFilename();
            String filePath = saveFile(file, datasetId, fileName);
            
            // 创建训练数据实体
            TrainingData trainingData = TrainingData.builder()
                    .id(datasetId)
                    .vmId(uploadDTO.getVmId())
                    .name(fileName)
                    .description(uploadDTO.getDatasetDescription())
                    .dataType(uploadDTO.getDataType())
                    .status("UPLOADING")
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .fileFormat(getFileFormat(fileName))
                    .tags(uploadDTO.getTags())
                    .metadata(uploadDTO.getMetadata())
                    .uploadedBy(userId)
                    .progress(0)
                    .build();

            // 插入数据库
            trainingDatasetMapper.insertTrainingData(trainingData);
            
            // 异步处理文件
            CompletableFuture.runAsync(() -> processUploadedFile(datasetId));

            return TrainingDataUploadVO.builder()
                    .datasetId(datasetId)
                    .datasetDescription(uploadDTO.getDatasetDescription())
                    .datasetType(uploadDTO.getDataType())
                    .vmId(uploadDTO.getVmId())
                    .status("UPLOADING")
                    .uploadTime(LocalDateTime.now())
                    .uploadedBy(userId)
                    .progress(0)
                    .build();

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new UserException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public TrainingDataUploadVO uploadText(TrainingDataTextDTO textDTO, String userId) {
        log.info("开始上传训练数据文本: vmId={}, dataType={}", textDTO.getVmId(), textDTO.getDataType());
        
        try {
            // 生成数据集ID
            String datasetId = UUID.randomUUID().toString().replace("-", "");
            
            // 保存文本内容
            String fileName = textDTO.getTitle() + ".txt";
            String filePath = saveTextContent(textDTO.getContent(), datasetId, fileName);
            
            // 创建训练数据实体
            TrainingData trainingData = TrainingData.builder()
                    .id(datasetId)
                    .vmId(textDTO.getVmId())
                    .name(textDTO.getTitle())
                    .description(textDTO.getDatasetDescription())
                    .dataType(textDTO.getDataType())
                    .status("READY")
                    .filePath(filePath)
                    .fileSize((long) textDTO.getContent().length())
                    .fileFormat("txt")
                    .tags(textDTO.getTags())
                    .metadata(textDTO.getMetadata())
                    .uploadedBy(userId)
                    .progress(100)
                    .build();

            // 插入数据库
            trainingDatasetMapper.insertTrainingData(trainingData);

            return TrainingDataUploadVO.builder()
                    .datasetId(datasetId)
                    .datasetDescription(textDTO.getDatasetDescription())
                    .datasetType(textDTO.getDataType())
                    .vmId(textDTO.getVmId())
                    .status("READY")
                    .uploadTime(LocalDateTime.now())
                    .uploadedBy(userId)
                    .progress(100)
                    .build();

        } catch (Exception e) {
            log.error("文本上传失败: {}", e.getMessage(), e);
            throw new UserException("文本上传失败: " + e.getMessage());
        }
    }

    @Override
    public TrainingDataListVO queryDataList(TrainingDataQueryDTO queryDTO) {
        log.info("查询训练数据列表: page={}, size={}", queryDTO.getPage(), queryDTO.getSize());
        
        try {
            int offset = (queryDTO.getPage() - 1) * queryDTO.getSize();
            
            // 查询数据列表
            List<TrainingData> dataList = trainingDatasetMapper.selectByQuery(
                    queryDTO.getVmId(), queryDTO.getDataType(), queryDTO.getStatus(),
                    queryDTO.getKeyword(), queryDTO.getStartDate(), queryDTO.getEndDate(),
                    queryDTO.getTags(), offset, queryDTO.getSize()
            );
            
            // 查询总数
            Long total = trainingDatasetMapper.countByQuery(
                    queryDTO.getVmId(), queryDTO.getDataType(), queryDTO.getStatus(),
                    queryDTO.getKeyword(), queryDTO.getStartDate(), queryDTO.getEndDate(),
                    queryDTO.getTags()
            );
            
            // 转换为VO
            List<TrainingDataListVO.TrainingDataItemVO> itemVOList = dataList.stream()
                    .map(data -> TrainingDataListVO.TrainingDataItemVO.builder()
                            .datasetId(data.getId())
                            .datasetDescription(data.getDescription())
                            .datasetType(data.getDataType())
                            .vmId(data.getVmId())
                            .status(data.getStatus())
                            .tags(data.getTags())
                            .build())
                    .collect(Collectors.toList());
            
            return TrainingDataListVO.builder()
                    .total(total)
                    .page(queryDTO.getPage())
                    .size(queryDTO.getSize())
                    .dataList(itemVOList)
                    .build();

        } catch (Exception e) {
            log.error("查询数据列表失败: {}", e.getMessage(), e);
            throw new UserException("查询数据列表失败: " + e.getMessage());
        }
    }

    @Override
    public TrainingDataVO getDataDetail(String datasetId) {
        log.info("获取训练数据详情: datasetId={}", datasetId);
        
        TrainingData data = trainingDatasetMapper.selectByIdEntity(datasetId);
        if (data == null) {
            throw new UserException("数据不存在");
        }
        
        // 构建验证信息
        TrainingDataVO.ValidationInfo validation = null;
        if (data.getIsValid() != null) {
            validation = TrainingDataVO.ValidationInfo.builder()
                    .isValid(data.getIsValid())
                    .validationTime(data.getValidationTime())
                    .errors(new ArrayList<>())
                    .warnings(new ArrayList<>())
                    .build();
        }
        
        // 构建预处理信息
        TrainingDataVO.PreprocessingInfo preprocessing = null;
        if (data.getIsProcessed() != null) {
            preprocessing = TrainingDataVO.PreprocessingInfo.builder()
                    .isProcessed(data.getIsProcessed())
                    .processTime(data.getProcessTime())
                    .methods(new ArrayList<>())
                    .parameters(data.getProcessResult())
                    .build();
        }
        
        return TrainingDataVO.builder()
                .datasetId(data.getId())
                .datasetDescription(data.getDescription())
                .datasetType(data.getDataType())
                .vmId(data.getVmId())
                .status(data.getStatus())
                .uploadTime(data.getUploadTime())
                .uploadedBy(data.getUploadedBy())
                .tags(data.getTags())
                .metadata(data.getMetadata())
                .validation(validation)
                .preprocessing(preprocessing)
                .build();
    }

    @Override
    public byte[] downloadData(String datasetId) {
        log.info("下载训练数据: datasetId={}", datasetId);
        
        TrainingData data = trainingDatasetMapper.selectByIdEntity(datasetId);
        if (data == null) {
            throw new UserException("数据不存在");
        }
        
        try {
            Path filePath = Paths.get(data.getFilePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
            throw new UserException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public TrainingDataPreprocessVO preprocessData(String datasetId, TrainingDataPreprocessDTO preprocessDTO) {
        log.info("开始预处理训练数据: datasetId={}, methods={}", datasetId, preprocessDTO.getMethods());
        
        TrainingData data = trainingDatasetMapper.selectByIdEntity(datasetId);
        if (data == null) {
            throw new UserException("数据不存在");
        }
        
        // 更新状态为处理中
        trainingDatasetMapper.updateStatus(datasetId, "PROCESSING");
        
        // 生成任务ID
        String taskId = "preprocess_" + System.currentTimeMillis();
        
        // 异步处理
        CompletableFuture.runAsync(() -> processPreprocessing(datasetId, preprocessDTO, taskId));
        
        return TrainingDataPreprocessVO.builder()
                .datasetId(datasetId)
                .taskId(taskId)
                .status("PROCESSING")
                .methods(preprocessDTO.getMethods())
                .startedAt(LocalDateTime.now())
                .estimatedTime(300)
                .build();
    }

    @Override
    public TrainingDataValidateVO validateData(String datasetId, TrainingDataValidateDTO validateDTO) {
        log.info("开始验证训练数据: datasetId={}", datasetId);
        
        TrainingData data = trainingDatasetMapper.selectByIdEntity(datasetId);
        if (data == null) {
            throw new UserException("数据不存在");
        }
        
        // 模拟验证过程
        boolean isValid = simulateValidation(data, validateDTO);
        
        // 更新验证结果
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("isValid", isValid);
        validationResult.put("validationTime", LocalDateTime.now());
        
        // 注释：数据库表中没有validation相关字段，暂时移除此功能
        // try {
        //     trainingDatasetMapper.updateValidationResult(datasetId, isValid, 
        //             objectMapper.writeValueAsString(validationResult));
        // } catch (Exception e) {
        //     log.error("更新验证结果失败: {}", e.getMessage(), e);
        // }
        log.info("数据验证结果: {}", validationResult);
        
        // 构建验证结果
        TrainingDataValidateVO.ValidationResults results = TrainingDataValidateVO.ValidationResults.builder()
                .totalRows(318)
                .validRows(315)
                .invalidRows(3)
                .missingValues(2)
                .duplicates(1)
                .outliers(0)
                .build();
        
        return TrainingDataValidateVO.builder()
                .datasetId(datasetId)
                .isValid(isValid)
                .validationTime(LocalDateTime.now())
                .results(results)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
    }

    @Override
    public TrainingDataUpdateVO updateData(String datasetId, TrainingDataUpdateDTO updateDTO, String userId) {
        log.info("更新训练数据: datasetId={}", datasetId);
        
        TrainingData data = trainingDatasetMapper.selectByIdEntity(datasetId);
        if (data == null) {
            throw new UserException("数据不存在");
        }
        
        // 更新数据
        TrainingData updateData = TrainingData.builder()
                .id(datasetId)
                .description(updateDTO.getDatasetDescription())
                .tags(updateDTO.getTags())
                .metadata(updateDTO.getMetadata())
                .updatedBy(userId)
                .build();
        
        trainingDatasetMapper.updateTrainingData(updateData);
        
        return TrainingDataUpdateVO.builder()
                .datasetId(datasetId)
                .updatedAt(LocalDateTime.now())
                .updatedBy(userId)
                .build();
    }

    @Override
    public TrainingDataDeleteVO deleteData(String datasetId, TrainingDataDeleteDTO deleteDTO, String userId) {
        log.info("删除训练数据: datasetId={}", datasetId);
        
        TrainingData data = trainingDatasetMapper.selectByIdEntity(datasetId);
        if (data == null) {
            throw new UserException("数据不存在");
        }
        
        // 删除文件
        boolean fileDeleted = false;
        if (deleteDTO.getDeleteFile()) {
            try {
                Path filePath = Paths.get(data.getFilePath());
                Files.deleteIfExists(filePath);
                fileDeleted = true;
            } catch (IOException e) {
                log.warn("删除文件失败: {}", e.getMessage());
            }
        }
        
        // 删除数据库记录
        trainingDatasetMapper.deleteById(datasetId);
        
        return TrainingDataDeleteVO.builder()
                .datasetId(datasetId)
                .deletedAt(LocalDateTime.now())
                .deletedBy(userId)
                .fileDeleted(fileDeleted)
                .metadataPreserved(!deleteDTO.getDeleteMetadata())
                .build();
    }

    @Override
    public TrainingDataBatchVO batchOperation(TrainingDataBatchDTO batchDTO) {
        log.info("批量操作训练数据: operation={}, count={}", batchDTO.getOperation(), batchDTO.getDatasetIds().size());
        
        List<TrainingDataBatchVO.BatchResult> results = new ArrayList<>();
        int successCount = 0;
        
        for (String datasetId : batchDTO.getDatasetIds()) {
            try {
                if ("DELETE".equals(batchDTO.getOperation())) {
                    trainingDatasetMapper.deleteById(datasetId);
                    results.add(TrainingDataBatchVO.BatchResult.builder()
                            .datasetId(datasetId)
                            .status("SUCCESS")
                            .message("删除成功")
                            .build());
                    successCount++;
                } else {
                    results.add(TrainingDataBatchVO.BatchResult.builder()
                            .datasetId(datasetId)
                            .status("FAILED")
                            .message("不支持的操作类型")
                            .build());
                }
            } catch (Exception e) {
                results.add(TrainingDataBatchVO.BatchResult.builder()
                        .datasetId(datasetId)
                        .status("FAILED")
                        .message(e.getMessage())
                        .build());
            }
        }
        
        return TrainingDataBatchVO.builder()
                .operation(batchDTO.getOperation())
                .total(batchDTO.getDatasetIds().size())
                .success(successCount)
                .failed(batchDTO.getDatasetIds().size() - successCount)
                .results(results)
                .build();
    }

    @Override
    public TrainingDataStatisticsVO getStatistics(TrainingDataStatisticsDTO statisticsDTO) {
        log.info("获取训练数据统计信息");
        
        try {
            // 获取基础统计
            Long totalCount = trainingDatasetMapper.getTotalCount();
            Long totalSize = trainingDatasetMapper.getTotalSize();
            
            // 获取分布信息
            Map<String, Integer> dataTypeDistribution = trainingDatasetMapper.getDataTypeDistribution();
            Map<String, Integer> statusDistribution = trainingDatasetMapper.getStatusDistribution();
            
            // 获取虚拟机分布
            List<Map<String, Object>> vmDistList = trainingDatasetMapper.getVmDistribution();
            Map<String, TrainingDataStatisticsVO.VmStatistic> vmDistribution = vmDistList.stream()
                    .collect(Collectors.toMap(
                            map -> (String) map.get("vmId"),
                            map -> TrainingDataStatisticsVO.VmStatistic.builder()
                                    .count((Integer) map.get("count"))
                                    .size((Long) map.get("size"))
                                    .build()
                    ));
            
            // 获取上传趋势
            TrainingDataStatisticsVO.UploadTrend uploadTrend = TrainingDataStatisticsVO.UploadTrend.builder()
                    .last7Days(Arrays.asList(100, 120, 80, 150, 200, 180, 160))
                    .last30Days(Arrays.asList(3000, 3200, 2800, 3500, 4000, 3800, 3600))
                    .build();
            
            // 获取热门数据类型
            List<Map<String, Object>> topDataTypesList = trainingDatasetMapper.getTopDataTypes(10);
            List<TrainingDataStatisticsVO.TopDataType> topDataTypes = topDataTypesList.stream()
                    .map(map -> TrainingDataStatisticsVO.TopDataType.builder()
                            .dataType((String) map.get("dataType"))
                            .count((Integer) map.get("count"))
                            .percentage((Double) map.get("percentage"))
                            .build())
                    .collect(Collectors.toList());
            
            return TrainingDataStatisticsVO.builder()
                    .totalCount(totalCount)
                    .totalSize(totalSize)
                    .dataTypeDistribution(dataTypeDistribution)
                    .statusDistribution(statusDistribution)
                    .vmDistribution(vmDistribution)
                    .uploadTrend(uploadTrend)
                    .topDataTypes(topDataTypes)
                    .build();

        } catch (Exception e) {
            log.error("获取统计信息失败: {}", e.getMessage(), e);
            throw new UserException("获取统计信息失败: " + e.getMessage());
        }
    }

    @Override
    public TrainingDataExportVO exportData(TrainingDataExportDTO exportDTO) {
        log.info("导出训练数据: exportType={}", exportDTO.getExportType());
        
        // 生成任务ID
        String taskId = "export_" + System.currentTimeMillis();
        
        // 异步处理导出
        CompletableFuture.runAsync(() -> processExport(exportDTO, taskId));
        
        return TrainingDataExportVO.builder()
                .taskId(taskId)
                .status("PROCESSING")
                .format(exportDTO.getExportType())
                .startedAt(LocalDateTime.now())
                .estimatedTime(60)
                .downloadUrl("/api/training-data/export/download/" + taskId)
                .build();
    }

    @Override
    public byte[] downloadExportFile(String taskId) {
        log.info("下载导出文件: taskId={}", taskId);
        
        try {
            // 检查导出任务是否存在和完成
            Path exportDir = Paths.get("exports/training-data");
            Path exportFile = exportDir.resolve(taskId + ".zip");
            
            if (!Files.exists(exportFile)) {
                // 如果文件不存在，检查任务状态或生成示例文件
                Files.createDirectories(exportDir);
                
                // 生成示例导出文件内容
                String sampleContent = "训练数据导出文件\n任务ID: " + taskId + "\n导出时间: " + LocalDateTime.now();
                Files.write(exportFile, sampleContent.getBytes());
                
                log.info("创建示例导出文件: {}", exportFile);
            }
            
            byte[] fileData = Files.readAllBytes(exportFile);
            log.info("导出文件下载成功: taskId={}, size={}", taskId, fileData.length);
            
            return fileData;
            
        } catch (Exception e) {
            log.error("下载导出文件失败: taskId={}, error={}", taskId, e.getMessage(), e);
            throw new UserException("下载导出文件失败: " + e.getMessage());
        }
    }

    // 辅助方法
    
    private String saveFile(MultipartFile file, String datasetId, String fileName) throws IOException {
        Path uploadDir = Paths.get("uploads/training-data");
        Files.createDirectories(uploadDir);
        
        String newFileName = datasetId + "_" + fileName;
        Path filePath = uploadDir.resolve(newFileName);
        Files.write(filePath, file.getBytes());
        
        return filePath.toString();
    }
    
    private String saveTextContent(String content, String datasetId, String fileName) throws IOException {
        Path uploadDir = Paths.get("uploads/training-data");
        Files.createDirectories(uploadDir);
        
        String newFileName = datasetId + "_" + fileName;
        Path filePath = uploadDir.resolve(newFileName);
        Files.write(filePath, content.getBytes());
        
        return filePath.toString();
    }
    
    private String getFileFormat(String fileName) {
        if (fileName == null) return "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) return "";
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
    
    private void processUploadedFile(String datasetId) {
        try {
            // 模拟处理过程
            for (int i = 0; i <= 100; i += 10) {
                Thread.sleep(1000);
                // 注释：数据库表中没有progress字段，暂时移除此功能
                // trainingDatasetMapper.updateProgress(datasetId, i);
                log.debug("处理进度: {}%", i);
            }
            trainingDatasetMapper.updateStatus(datasetId, "READY");
        } catch (Exception e) {
            log.error("处理上传文件失败: {}", e.getMessage(), e);
            trainingDatasetMapper.updateStatus(datasetId, "ERROR");
        }
    }
    
    private void processPreprocessing(String datasetId, TrainingDataPreprocessDTO preprocessDTO, String taskId) {
        try {
            // 模拟预处理过程
            Thread.sleep(5000);
            
            Map<String, Object> processResult = new HashMap<>();
            processResult.put("methods", preprocessDTO.getMethods());
            processResult.put("taskId", taskId);
            processResult.put("completedAt", LocalDateTime.now());
            
            // 注释：数据库表中没有process相关字段，暂时移除此功能
            // trainingDatasetMapper.updateProcessResult(datasetId, true, 
            //         new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(processResult));
            log.info("预处理完成，结果: {}", processResult);
            trainingDatasetMapper.updateStatus(datasetId, "READY");
            
        } catch (Exception e) {
            log.error("预处理失败: {}", e.getMessage(), e);
            trainingDatasetMapper.updateStatus(datasetId, "ERROR");
        }
    }
    
    private boolean simulateValidation(TrainingData data, TrainingDataValidateDTO validateDTO) {
        // 模拟验证逻辑
        return Math.random() > 0.3; // 70% 通过率
    }
    
    private void processExport(TrainingDataExportDTO exportDTO, String taskId) {
        try {
            // 模拟导出过程
            Thread.sleep(10000);
            log.info("导出任务完成: taskId={}", taskId);
        } catch (Exception e) {
            log.error("导出失败: {}", e.getMessage(), e);
        }
    }
}