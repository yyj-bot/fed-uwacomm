package com.feduwacomm.controller;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.common.Result;
import com.feduwacomm.model.dto.initial.InitialModelGenerateRequest;
import com.feduwacomm.model.dto.initial.InitialModelUploadRequest;
import com.feduwacomm.model.vo.initial.InitialModelBindingVO;
import com.feduwacomm.model.vo.initial.InitialModelDetailVO;
import com.feduwacomm.service.InitialModelGenerationService;
import com.feduwacomm.vo.InitialModelInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 初始模型管理控制器（v1.5）
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/model/initial")
@CrossOrigin(origins = "*")
public class InitialModelController {

    private final InitialModelGenerationService initialModelGenerationService;

    /**
     * 自动生成初始模型
     */
    @PostMapping("/generate")
    public Result<InitialModelDetailVO> generateInitialModel(@Valid @RequestBody InitialModelGenerateRequest request) {
        String currentUserId = BaseContext.getCurrentId();
        if (!StringUtils.hasText(currentUserId)) {
            currentUserId = "system";
        }
        try {
            InitialModelDetailVO detail = initialModelGenerationService.generateInitialModel(request, currentUserId);
            log.info("自动生成初始模型成功: modelId={}, createdBy={}", detail.getModelId(), currentUserId);
            return Result.success("初始模型生成成功", detail);
        } catch (IllegalArgumentException ex) {
            log.warn("初始模型生成参数错误: {}", ex.getMessage());
            return Result.error(400, ex.getMessage());
        } catch (Exception ex) {
            log.error("初始模型生成失败", ex);
            return Result.error("初始模型生成失败: " + ex.getMessage());
        }
    }

    /**
     * 上传自定义初始模型（元数据）
     * 说明：文件上传流程应在外部完成，此处接收已经落盘的文件路径
     */
    @PostMapping("/upload")
    public Result<InitialModelDetailVO> uploadCustomModel(@Valid @RequestBody InitialModelUploadRequest request,
                                                          @RequestParam("filePath") String filePath) {
        String currentUserId = BaseContext.getCurrentId();
        if (!StringUtils.hasText(currentUserId)) {
            currentUserId = "system";
        }
        try {
            InitialModelDetailVO detail = initialModelGenerationService.uploadCustomModel(request, filePath, currentUserId);
            log.info("自定义初始模型上传成功: modelId={}, createdBy={}", detail.getModelId(), currentUserId);
            return Result.success("初始模型上传成功", detail);
        } catch (IllegalArgumentException ex) {
            log.warn("自定义初始模型上传参数错误: {}", ex.getMessage());
            return Result.error(400, ex.getMessage());
        } catch (Exception ex) {
            log.error("自定义初始模型上传失败", ex);
            return Result.error("初始模型上传失败: " + ex.getMessage());
        }
    }

    /**
     * 查询模型详情
     */
    @GetMapping("/{modelId}")
    public Result<InitialModelDetailVO> getInitialModelDetail(@PathVariable String modelId,
                                                              @RequestParam(value = "includeParameters", defaultValue = "false") boolean includeParameters) {
        try {
            InitialModelDetailVO detail = initialModelGenerationService.getModelDetail(modelId, includeParameters);
            return Result.success("查询成功", detail);
        } catch (NoSuchElementException ex) {
            return Result.error(404, ex.getMessage());
        } catch (Exception ex) {
            log.error("查询初始模型详情失败: modelId={}", modelId, ex);
            return Result.error("查询失败: " + ex.getMessage());
        }
    }

    /**
     * 查询任务当前绑定的初始模型
     */
    @GetMapping("/task/{taskId}")
    public Result<InitialModelDetailVO> getTaskCurrentInitialModel(@PathVariable String taskId,
                                                                   @RequestParam(value = "includeParameters", defaultValue = "false") boolean includeParameters) {
        try {
            InitialModelBindingVO binding = initialModelGenerationService.getTaskBinding(taskId);
            if (binding == null || !StringUtils.hasText(binding.getModelId())) {
                return Result.success("当前任务尚未绑定初始模型", null);
            }
            InitialModelDetailVO detail = initialModelGenerationService.getModelDetail(binding.getModelId(), includeParameters);
            return Result.success("查询成功", detail);
        } catch (Exception ex) {
            log.error("查询任务绑定初始模型失败: taskId={}", taskId, ex);
            return Result.error("查询失败: " + ex.getMessage());
        }
    }

    /**
     * 查询初始模型列表
     */
    @GetMapping
    public Result<List<InitialModelInfoVO>> listInitialModels(@RequestParam(value = "status", required = false) String status) {
        try {
            List<InitialModelInfoVO> models = initialModelGenerationService.listModels(status);
            return Result.success("查询成功", models);
        } catch (Exception ex) {
            log.error("查询初始模型列表失败", ex);
            return Result.error("查询失败: " + ex.getMessage());
        }
    }

    /**
     * 删除初始模型
     */
    @DeleteMapping("/{modelId}")
    public Result<Void> deleteInitialModel(@PathVariable String modelId) {
        String currentUserId = BaseContext.getCurrentId();
        if (!StringUtils.hasText(currentUserId)) {
            currentUserId = "system";
        }
        try {
            boolean deleted = initialModelGenerationService.deleteModel(modelId, currentUserId);
            if (!deleted) {
                return Result.error(404, "初始模型不存在或已删除");
            }
            return Result.success("初始模型删除成功", null);
        } catch (Exception ex) {
            log.error("删除初始模型失败: modelId={}", modelId, ex);
            return Result.error("删除失败: " + ex.getMessage());
        }
    }

    /**
     * 下载初始模型文件
     */
    @GetMapping("/{modelId}/download")
    public ResponseEntity<Resource> downloadInitialModel(@PathVariable String modelId) {
        try {
            var download = initialModelGenerationService.prepareModelDownload(modelId);
            MediaType mediaType = StringUtils.hasText(download.getContentType())
                ? MediaType.parseMediaType(download.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

            if (download.isFileResource()) {
                Path path = Paths.get(download.getFilePath());
                Resource resource = new FileSystemResource(path);
                if (!resource.exists()) {
                    log.warn("下载请求阶段文件不存在，返回404: modelId={}, path={}", modelId, path);
                    return ResponseEntity.status(404).build();
                }
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.getFilename() + "\"")
                    .contentType(mediaType)
                    .contentLength(download.getContentLength())
                    .body(resource);
            } else {
                ByteArrayResource resource = new ByteArrayResource(download.getInlineContent());
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.getFilename() + "\"")
                    .contentType(mediaType)
                    .contentLength(download.getContentLength())
                    .body(resource);
            }
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(404).build();
        } catch (IllegalStateException ex) {
            log.error("下载初始模型失败(状态异常): modelId={}, error={}", modelId, ex.getMessage());
            return ResponseEntity.status(410).build();
        } catch (Exception ex) {
            log.error("下载初始模型失败: modelId={}", modelId, ex);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 获取模型绑定历史
     */
    @GetMapping("/{modelId}/bindings")
    public Result<List<InitialModelBindingVO>> listModelBindings(@PathVariable String modelId) {
        try {
            List<InitialModelBindingVO> bindings = initialModelGenerationService.getModelBindings(modelId);
            return Result.success("查询成功", bindings);
        } catch (Exception ex) {
            log.error("查询模型绑定历史失败: modelId={}", modelId, ex);
            return Result.error("查询失败: " + ex.getMessage());
        }
    }

}
