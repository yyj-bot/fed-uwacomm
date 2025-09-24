package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.ModelVersionService;
import com.feduwacomm.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 模型版本管理控制器
 * 提供模型版本管理相关的API接口
 */
@RestController
@RequestMapping("/api/model")
@CrossOrigin(origins = "*")
public class ModelVersionController {

    private static final Logger log = LoggerFactory.getLogger(ModelVersionController.class);
    
    @Autowired
    private ModelVersionService modelVersionService;
    
    @Autowired
    private ObjectMapper objectMapper;

    // 模型上传接口
    /**
     * 模型文件上传
     * POST /api/model/upload
     */
    @PostMapping("/upload")
    public Result<ModelUploadResponseVO> uploadModel(
            @RequestParam("taskId") String taskId,
            @RequestParam("roundNumber") Integer roundNumber,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "parameters", required = false) String parameters,
            @RequestParam("file") MultipartFile file) {
        
        log.info("收到模型上传请求: taskId={}, roundNumber={}", taskId, roundNumber);
        
        try {
            ModelUploadResponseVO response = modelVersionService.uploadModel(
                taskId, roundNumber, description, parameters, file);
            
            log.info("模型上传成功: modelId={}", response.getModelId());
            return Result.success("模型上传成功", response);
            
        } catch (Exception e) {
            log.error("模型上传失败: taskId={}, roundNumber={}, error={}", 
                     taskId, roundNumber, e.getMessage());
            throw e;
        }
    }

    /**
     * 批量模型上传
     * POST /api/model/upload/batch
     */
    @PostMapping("/upload/batch")
    public Result<ModelBatchUploadResponseVO> batchUploadModels(@Valid @RequestBody ModelBatchUploadDTO batchUploadDTO) {
        log.info("收到批量模型上传请求: taskId={}, modelCount={}", 
                batchUploadDTO.getTaskId(), batchUploadDTO.getModels().size());
        
        try {
            ModelBatchUploadResponseVO response = modelVersionService.batchUploadModels(batchUploadDTO);
            log.info("批量模型上传完成: successCount={}, failedCount={}", 
                    response.getSuccessCount(), response.getFailedCount());
            return Result.success("批量上传成功", response);
            
        } catch (Exception e) {
            log.error("批量模型上传失败: error={}", e.getMessage());
            throw e;
        }
    }

    // 模型版本查询接口
    /**
     * 模型版本列表查询
     * GET /api/model/versions
     */
    @GetMapping("/versions")
    public Result<PageResponseDTO<ModelVersionVO>> getModelVersions(
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "roundNumber", required = false) Integer roundNumber,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", defaultValue = "created_at") String sort,
            @RequestParam(value = "order", defaultValue = "desc") String order) {
        
        log.info("查询模型版本列表: taskId={}, page={}, size={}", taskId, page, size);
        
        ModelQueryDTO queryDTO = ModelQueryDTO.builder()
            .taskId(taskId)
            .roundNumber(roundNumber)
            .status(status)
            .page(page)
            .size(size)
            .sort(sort)
            .order(order)
            .build();
            
        PageResponseDTO<ModelVersionVO> response = modelVersionService.getModelVersions(queryDTO);
        return Result.success("查询成功", response);
    }

    /**
     * 模型版本详情查询
     * GET /api/model/versions/{modelId}
     */
    @GetMapping("/versions/{modelId}")
    public Result<ModelVersionVO> getModelVersionById(@PathVariable String modelId) {
        log.info("查询模型版本详情: modelId={}", modelId);
        
        ModelVersionVO response = modelVersionService.getModelVersionById(modelId);
        return Result.success("查询成功", response);
    }

    /**
     * 任务模型版本查询
     * GET /api/model/versions/task/{taskId}
     */
    @GetMapping("/versions/task/{taskId}")
    public Result<ModelTaskVersionsVO> getModelVersionsByTaskId(
            @PathVariable String taskId,
            @RequestParam(value = "roundNumber", required = false) Integer roundNumber,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sort", defaultValue = "roundNumber") String sort,
            @RequestParam(value = "order", defaultValue = "asc") String order) {
        
        log.info("查询任务模型版本: taskId={}", taskId);
        
        ModelTaskVersionsVO response = modelVersionService.getModelVersionsByTaskId(
            taskId, roundNumber, status, sort, order);
        return Result.success("查询成功", response);
    }

    // 模型性能评估接口
    /**
     * 模型性能评估
     * POST /api/model/evaluate
     */
    @PostMapping("/evaluate")
    public Result<ModelEvaluateResponseVO> evaluateModel(@Valid @RequestBody ModelEvaluateDTO evaluateDTO) {
        log.info("收到模型评估请求: modelId={}", evaluateDTO.getModelId());
        
        ModelEvaluateResponseVO response = modelVersionService.evaluateModel(evaluateDTO);
        return Result.success("评估完成", response);
    }

    /**
     * 批量模型评估
     * POST /api/model/evaluate/batch
     */
    @PostMapping("/evaluate/batch")
    public Result<List<ModelEvaluateResponseVO>> batchEvaluateModels(@Valid @RequestBody ModelBatchEvaluateDTO batchEvaluateDTO) {
        log.info("收到批量模型评估请求: taskId={}", batchEvaluateDTO.getTaskId());
        
        List<ModelEvaluateResponseVO> response = modelVersionService.batchEvaluateModels(batchEvaluateDTO);
        return Result.success("批量评估完成", response);
    }

    /**
     * 评估结果查询
     * GET /api/model/evaluate/results
     */
    @GetMapping("/evaluate/results")
    public Result<PageResponseDTO<ModelEvaluateResponseVO>> getEvaluationResults(
            @RequestParam(value = "modelId", required = false) String modelId,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "evaluationId", required = false) String evaluationId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        
        log.info("查询评估结果: modelId={}, taskId={}", modelId, taskId);
        
        PageResponseDTO<ModelEvaluateResponseVO> response = modelVersionService.getEvaluationResults(
            modelId, taskId, evaluationId, page, size);
        return Result.success("查询成功", response);
    }

    // 模型部署接口
    /**
     * 模型部署
     * POST /api/model/deploy
     */
    @PostMapping("/deploy")
    public Result<ModelDeployResponseVO> deployModel(@Valid @RequestBody ModelDeployDTO deployDTO) {
        log.info("收到模型部署请求: modelId={}", deployDTO.getModelId());
        
        ModelDeployResponseVO response = modelVersionService.deployModel(deployDTO);
        return Result.success("部署成功", response);
    }

    /**
     * 部署状态查询
     * GET /api/model/deploy/status/{deploymentId}
     */
    @GetMapping("/deploy/status/{deploymentId}")
    public Result<Map<String, Object>> getDeploymentStatus(@PathVariable String deploymentId) {
        log.info("查询部署状态: deploymentId={}", deploymentId);
        
        Map<String, Object> response = modelVersionService.getDeploymentStatus(deploymentId);
        return Result.success("查询成功", response);
    }

    /**
     * 部署列表查询
     * GET /api/model/deploy/list
     */
    @GetMapping("/deploy/list")
    public Result<PageResponseDTO<Map<String, Object>>> getDeploymentList(
            @RequestParam(value = "modelId", required = false) String modelId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        
        log.info("查询部署列表: modelId={}, status={}", modelId, status);
        
        PageResponseDTO<Map<String, Object>> response = modelVersionService.getDeploymentList(
            modelId, status, page, size);
        return Result.success("查询成功", response);
    }

    // 模型回滚接口
    /**
     * 模型回滚
     * POST /api/model/rollback
     */
    @PostMapping("/rollback")
    public Result<Map<String, Object>> rollbackModel(@Valid @RequestBody ModelRollbackDTO rollbackDTO) {
        log.info("收到模型回滚请求: deploymentId={}, targetModelId={}", 
                rollbackDTO.getDeploymentId(), rollbackDTO.getTargetModelId());
        
        Map<String, Object> response = modelVersionService.rollbackModel(rollbackDTO);
        return Result.success("回滚成功", response);
    }

    /**
     * 回滚历史查询
     * GET /api/model/rollback/history
     */
    @GetMapping("/rollback/history")
    public Result<PageResponseDTO<Map<String, Object>>> getRollbackHistory(
            @RequestParam(value = "deploymentId", required = false) String deploymentId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        
        log.info("查询回滚历史: deploymentId={}", deploymentId);
        
        PageResponseDTO<Map<String, Object>> response = modelVersionService.getRollbackHistory(
            deploymentId, page, size);
        return Result.success("查询成功", response);
    }

    // 模型下载接口
    /**
     * 模型文件下载
     * GET /api/model/download/{modelId}
     */
    @GetMapping("/download/{modelId}")
    public void downloadModel(
            @PathVariable String modelId,
            @RequestParam(value = "format", defaultValue = "original") String format,
            @RequestParam(value = "compressed", defaultValue = "true") Boolean compressed,
            HttpServletResponse response) throws IOException {
        
        log.info("收到模型下载请求: modelId={}, format={}, compressed={}", 
                modelId, format, compressed);
        
        try {
            byte[] fileData = modelVersionService.downloadModel(modelId, format, compressed);
            
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                              "attachment; filename=model_" + modelId + ".zip");
            response.setContentLength(fileData.length);
            
            response.getOutputStream().write(fileData);
            response.getOutputStream().flush();
            
            log.info("模型下载完成: modelId={}", modelId);
            
        } catch (Exception e) {
            log.error("模型下载失败: modelId={}, error={}", modelId, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"下载失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 批量模型下载
     * POST /api/model/download/batch
     */
    @PostMapping("/download/batch")
    public void batchDownloadModels(@Valid @RequestBody ModelBatchDownloadDTO batchDownloadDTO,
                                   HttpServletResponse response) throws IOException {
        
        log.info("收到批量模型下载请求: modelCount={}", batchDownloadDTO.getModelIds().size());
        
        try {
            byte[] zipData = modelVersionService.batchDownloadModels(batchDownloadDTO);
            
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                              "attachment; filename=models_batch.zip");
            response.setContentLength(zipData.length);
            
            response.getOutputStream().write(zipData);
            response.getOutputStream().flush();
            
            log.info("批量模型下载完成: modelCount={}", batchDownloadDTO.getModelIds().size());
            
        } catch (Exception e) {
            log.error("批量模型下载失败: error={}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"下载失败: " + e.getMessage() + "\"}");
        }
    }

    // 模型删除接口
    /**
     * 模型版本删除
     * DELETE /api/model/versions/{modelId}
     */
    @DeleteMapping("/versions/{modelId}")
    public Result<Map<String, Object>> deleteModelVersion(
            @PathVariable String modelId,
            @RequestParam(value = "force", defaultValue = "false") Boolean force,
            @RequestParam(value = "deleteFile", defaultValue = "true") Boolean deleteFile) {
        
        log.info("收到模型删除请求: modelId={}, force={}, deleteFile={}", 
                modelId, force, deleteFile);
        
        Map<String, Object> response = modelVersionService.deleteModelVersion(modelId, force, deleteFile);
        return Result.success("删除成功", response);
    }

    /**
     * 批量模型删除
     * DELETE /api/model/versions/batch
     */
    @DeleteMapping("/versions/batch")
    public Result<Map<String, Object>> batchDeleteModels(@Valid @RequestBody ModelBatchDeleteDTO batchDeleteDTO) {
        log.info("收到批量模型删除请求: modelCount={}", batchDeleteDTO.getModelIds().size());
        
        Map<String, Object> response = modelVersionService.batchDeleteModels(batchDeleteDTO);
        return Result.success("批量删除成功", response);
    }

    // 模型统计接口
    /**
     * 模型统计信息
     * GET /api/model/statistics
     */
    @GetMapping("/statistics")
    public Result<ModelStatisticsVO> getModelStatistics(
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "timeRange", defaultValue = "7d") String timeRange) {
        
        log.info("查询模型统计信息: taskId={}, timeRange={}", taskId, timeRange);
        
        ModelStatisticsVO response = modelVersionService.getModelStatistics(taskId, timeRange);
        return Result.success("查询成功", response);
    }

    /**
     * 任务模型统计
     * GET /api/model/statistics/task/{taskId}
     */
    @GetMapping("/statistics/task/{taskId}")
    public Result<Map<String, Object>> getTaskModelStatistics(@PathVariable String taskId) {
        log.info("查询任务模型统计: taskId={}", taskId);
        
        Map<String, Object> response = modelVersionService.getTaskModelStatistics(taskId);
        return Result.success("查询成功", response);
    }
}