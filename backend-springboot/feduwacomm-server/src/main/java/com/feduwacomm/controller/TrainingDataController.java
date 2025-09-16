package com.feduwacomm.controller;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.common.Result;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.TrainingDataService;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 训练数据管理控制器
 * 提供训练数据的完整生命周期管理功能
 */
@RestController
@RequestMapping("/api/training-data")
@CrossOrigin(origins = "*")
public class TrainingDataController {

    private static final Logger log = LoggerFactory.getLogger(TrainingDataController.class);
    private static final Logger accessLog = LoggerFactory.getLogger("ACCESS_LOG");

    @Autowired
    private TrainingDataService trainingDataService;

    /**
     * 3.1 文件上传接口
     * POST /api/training-data/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<TrainingDataUploadVO> uploadFile(
            @Valid TrainingDataUploadDTO uploadDTO,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到文件上传请求: dataType={}, fileName={}, ip={}",
                uploadDTO.getDataType(), file.getOriginalFilename(), clientIp);
        accessLog.info("训练数据文件上传: dataType={}, fileName={}, userId={}, ip={}",
                uploadDTO.getDataType(), file.getOriginalFilename(), userId, clientIp);

        try {
            TrainingDataUploadVO result = trainingDataService.uploadFile(uploadDTO, file, userId);
            log.info("文件上传成功: datasetId={}, userId={}", result.getDatasetId(), userId);
            return Result.success("文件上传成功", result);
        } catch (Exception e) {
            log.error("文件上传失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.failure(500, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 3.2 文本信息上传接口
     * POST /api/training-data/text
     */
    @PostMapping("/text")
    public Result<TrainingDataUploadVO> uploadText(
            @Valid @RequestBody TrainingDataTextDTO textDTO,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到文本上传请求: dataType={}, title={}, ip={}",
                textDTO.getDataType(), textDTO.getTitle(), clientIp);
        accessLog.info("训练数据文本上传: dataType={}, title={}, userId={}, ip={}",
                textDTO.getDataType(), textDTO.getTitle(), userId, clientIp);

        try {
            TrainingDataUploadVO result = trainingDataService.uploadText(textDTO, userId);
            log.info("文本上传成功: datasetId={}, userId={}", result.getDatasetId(), userId);
            return Result.success("文本信息上传成功", result);
        } catch (Exception e) {
            log.error("文本上传失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.failure(500, "文本上传失败: " + e.getMessage());
        }
    }

    /**
     * 3.3 数据列表查询接口
     * GET /api/training-data
     */
    @GetMapping
    public Result<TrainingDataListVO> getDataList(TrainingDataQueryDTO queryDTO, HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到数据列表查询请求: page={}, size={}, userId={}, ip={}", 
                queryDTO.getPage(), queryDTO.getSize(), userId, clientIp);
        accessLog.info("训练数据列表查询: page={}, size={}, userId={}, ip={}",
                queryDTO.getPage(), queryDTO.getSize(), userId, clientIp);

        try {
            TrainingDataListVO result = trainingDataService.queryDataList(queryDTO);
            log.info("数据列表查询成功: total={}, userId={}", result.getTotal(), userId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("数据列表查询失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.failure(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 3.4 数据详情查询接口
     * GET /api/training-data/{datasetId}
     */
    @GetMapping("/{datasetId}")
    public Result<TrainingDataVO> getDataDetail(@PathVariable String datasetId, HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到数据详情查询请求: datasetId={}, userId={}, ip={}", datasetId, userId, clientIp);
        accessLog.info("训练数据详情查询: datasetId={}, userId={}, ip={}", datasetId, userId, clientIp);

        try {
            TrainingDataVO result = trainingDataService.getDataDetail(datasetId);
            log.info("数据详情查询成功: datasetId={}, userId={}", datasetId, userId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("数据详情查询失败: datasetId={}, userId={}, error={}", datasetId, userId, e.getMessage(), e);
            return Result.failure(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 3.5 数据下载接口
     * GET /api/training-data/{datasetId}/download
     */
    @GetMapping("/{datasetId}/download")
    public void downloadData(@PathVariable String datasetId, 
                           HttpServletRequest request, 
                           HttpServletResponse response) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到数据下载请求: datasetId={}, userId={}, ip={}", datasetId, userId, clientIp);
        accessLog.info("训练数据下载: datasetId={}, userId={}, ip={}", datasetId, userId, clientIp);

        try {
            byte[] fileData = trainingDataService.downloadData(datasetId);
            
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + datasetId + ".data\"");
            response.setContentLength(fileData.length);
            
            response.getOutputStream().write(fileData);
            response.getOutputStream().flush();
            
            log.info("数据下载成功: datasetId={}, size={}, userId={}", datasetId, fileData.length, userId);
            
        } catch (Exception e) {
            log.error("数据下载失败: datasetId={}, userId={}, error={}", datasetId, userId, e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"code\":500,\"message\":\"下载失败: " + e.getMessage() + "\"}");
            } catch (IOException ioException) {
                log.error("响应写入失败", ioException);
            }
        }
    }

    /**
     * 3.6 数据预处理接口
     * POST /api/training-data/{datasetId}/preprocess
     */
    @PostMapping("/{datasetId}/preprocess")
    public Result<TrainingDataPreprocessVO> preprocessData(
            @PathVariable String datasetId,
            @Valid @RequestBody TrainingDataPreprocessDTO preprocessDTO,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到数据预处理请求: datasetId={}, methods={}, userId={}, ip={}", 
                datasetId, preprocessDTO.getMethods(), userId, clientIp);
        accessLog.info("训练数据预处理: datasetId={}, methods={}, userId={}, ip={}",
                datasetId, preprocessDTO.getMethods(), userId, clientIp);

        try {
            TrainingDataPreprocessVO result = trainingDataService.preprocessData(datasetId, preprocessDTO);
            log.info("数据预处理任务已启动: datasetId={}, taskId={}, userId={}", 
                    datasetId, result.getTaskId(), userId);
            return Result.success("预处理任务已启动", result);
        } catch (Exception e) {
            log.error("数据预处理失败: datasetId={}, userId={}, error={}", datasetId, userId, e.getMessage(), e);
            return Result.failure(500, "预处理失败: " + e.getMessage());
        }
    }

    /**
     * 3.7 数据验证接口
     * POST /api/training-data/{datasetId}/validate
     */
    @PostMapping("/{datasetId}/validate")
    public Result<TrainingDataValidateVO> validateData(
            @PathVariable String datasetId,
            @Valid @RequestBody TrainingDataValidateDTO validateDTO,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到数据验证请求: datasetId={}, userId={}, ip={}", datasetId, userId, clientIp);
        accessLog.info("训练数据验证: datasetId={}, userId={}, ip={}", datasetId, userId, clientIp);

        try {
            TrainingDataValidateVO result = trainingDataService.validateData(datasetId, validateDTO);
            log.info("数据验证完成: datasetId={}, isValid={}, userId={}", 
                    datasetId, result.getIsValid(), userId);
            return Result.success("验证完成", result);
        } catch (Exception e) {
            log.error("数据验证失败: datasetId={}, userId={}, error={}", datasetId, userId, e.getMessage(), e);
            return Result.failure(500, "验证失败: " + e.getMessage());
        }
    }

    /**
     * 3.8 数据更新接口
     * PUT /api/training-data/{datasetId}
     */
    @PutMapping("/{datasetId}")
    public Result<TrainingDataUpdateVO> updateData(
            @PathVariable String datasetId,
            @Valid @RequestBody TrainingDataUpdateDTO updateDTO,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到数据更新请求: datasetId={}, userId={}, ip={}", datasetId, userId, clientIp);
        accessLog.info("训练数据更新: datasetId={}, userId={}, ip={}", datasetId, userId, clientIp);

        try {
            TrainingDataUpdateVO result = trainingDataService.updateData(datasetId, updateDTO, userId);
            log.info("数据更新成功: datasetId={}, userId={}", datasetId, userId);
            return Result.success("数据更新成功", result);
        } catch (Exception e) {
            log.error("数据更新失败: datasetId={}, userId={}, error={}", datasetId, userId, e.getMessage(), e);
            return Result.failure(500, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 3.9 数据删除接口
     * DELETE /api/training-data/{datasetId}
     */
    @DeleteMapping("/{datasetId}")
    public Result<TrainingDataDeleteVO> deleteData(
            @PathVariable String datasetId,
            @RequestBody(required = false) TrainingDataDeleteDTO deleteDTO,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        if (deleteDTO == null) {
            deleteDTO = TrainingDataDeleteDTO.builder()
                    .deleteFile(true)
                    .deleteMetadata(false)
                    .reason("用户删除")
                    .build();
        }

        log.info("收到数据删除请求: datasetId={}, deleteFile={}, userId={}, ip={}", 
                datasetId, deleteDTO.getDeleteFile(), userId, clientIp);
        accessLog.info("训练数据删除: datasetId={}, deleteFile={}, userId={}, ip={}",
                datasetId, deleteDTO.getDeleteFile(), userId, clientIp);

        try {
            TrainingDataDeleteVO result = trainingDataService.deleteData(datasetId, deleteDTO, userId);
            log.info("数据删除成功: datasetId={}, userId={}", datasetId, userId);
            return Result.success("数据删除成功", result);
        } catch (Exception e) {
            log.error("数据删除失败: datasetId={}, userId={}, error={}", datasetId, userId, e.getMessage(), e);
            return Result.failure(500, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 3.10 批量数据操作接口
     * POST /api/training-data/batch
     */
    @PostMapping("/batch")
    public Result<TrainingDataBatchVO> batchOperation(
            @Valid @RequestBody TrainingDataBatchDTO batchDTO,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到批量操作请求: operation={}, count={}, userId={}, ip={}", 
                batchDTO.getOperation(), batchDTO.getDatasetIds().size(), userId, clientIp);
        accessLog.info("训练数据批量操作: operation={}, count={}, userId={}, ip={}",
                batchDTO.getOperation(), batchDTO.getDatasetIds().size(), userId, clientIp);

        try {
            TrainingDataBatchVO result = trainingDataService.batchOperation(batchDTO);
            log.info("批量操作完成: operation={}, success={}, failed={}, userId={}", 
                    result.getOperation(), result.getSuccess(), result.getFailed(), userId);
            return Result.success("批量操作成功", result);
        } catch (Exception e) {
            log.error("批量操作失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.failure(500, "批量操作失败: " + e.getMessage());
        }
    }

    /**
     * 3.11 数据统计接口
     * GET /api/training-data/statistics
     */
    @GetMapping("/statistics")
    public Result<TrainingDataStatisticsVO> getStatistics(
            TrainingDataStatisticsDTO statisticsDTO,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到数据统计查询请求: userId={}, ip={}", userId, clientIp);
        accessLog.info("训练数据统计查询: userId={}, ip={}", userId, clientIp);

        try {
            TrainingDataStatisticsVO result = trainingDataService.getStatistics(statisticsDTO);
            log.info("数据统计查询成功: totalCount={}, userId={}", result.getTotalCount(), userId);
            return Result.success("统计查询成功", result);
        } catch (Exception e) {
            log.error("数据统计查询失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.failure(500, "统计查询失败: " + e.getMessage());
        }
    }

    /**
     * 3.12 数据导出接口
     * POST /api/training-data/export
     */
    @PostMapping("/export")
    public Result<TrainingDataExportVO> exportData(
            @Valid @RequestBody TrainingDataExportDTO exportDTO,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到数据导出请求: exportType={}, format={}, userId={}, ip={}", 
                exportDTO.getExportType(), exportDTO.getFormat(), userId, clientIp);
        accessLog.info("训练数据导出: exportType={}, format={}, userId={}, ip={}",
                exportDTO.getExportType(), exportDTO.getFormat(), userId, clientIp);

        try {
            TrainingDataExportVO result = trainingDataService.exportData(exportDTO);
            log.info("数据导出任务已启动: taskId={}, userId={}", result.getTaskId(), userId);
            return Result.success("导出任务已启动", result);
        } catch (Exception e) {
            log.error("数据导出失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.failure(500, "导出失败: " + e.getMessage());
        }
    }

    /**
     * 导出文件下载接口
     * GET /api/training-data/export/download/{taskId}
     */
    @GetMapping("/export/download/{taskId}")
    public void downloadExportFile(@PathVariable String taskId,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userId = BaseContext.getCurrentId();

        log.info("收到导出文件下载请求: taskId={}, userId={}, ip={}", taskId, userId, clientIp);
        accessLog.info("训练数据导出文件下载: taskId={}, userId={}, ip={}", taskId, userId, clientIp);

        try {
            byte[] fileData = trainingDataService.downloadExportFile(taskId);
            
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"training_data_export_" + taskId + ".zip\"");
            response.setContentLength(fileData.length);
            
            response.getOutputStream().write(fileData);
            response.getOutputStream().flush();
            
            log.info("导出文件下载成功: taskId={}, size={}, userId={}", taskId, fileData.length, userId);
            
        } catch (Exception e) {
            log.error("导出文件下载失败: taskId={}, userId={}, error={}", taskId, userId, e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"code\":500,\"message\":\"下载失败: " + e.getMessage() + "\"}");
            } catch (IOException ioException) {
                log.error("响应写入失败", ioException);
            }
        }
    }
}