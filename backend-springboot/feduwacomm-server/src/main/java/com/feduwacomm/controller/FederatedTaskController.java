package com.feduwacomm.controller;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.common.Result;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.FederatedTaskService;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.vo.*;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.service.TrainingDataService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 联邦学习任务管理控制器
 * 提供联邦学习任务的生命周期管理功能
 */
@RestController
@RequestMapping("/api/federated")
@CrossOrigin(origins = "*")
public class FederatedTaskController {

    private static final Logger log = LoggerFactory.getLogger(FederatedTaskController.class);
    private static final Logger accessLog = LoggerFactory.getLogger("ACCESS_LOG");

    @Autowired
    private FederatedTaskService federatedTaskService;

    @Autowired
    private VmInstanceService vmInstanceService;

    @Autowired
    private TrainingDataService trainingDataService;

    // ========== v1.3 图形化配置接口 ==========

    /**
     * 获取可用虚拟机列表
     * GET /api/federated/config/available-vms
     */
    @GetMapping("/config/available-vms")
    public Result<ConfigPreviewVO.AvailableVmsVO> getAvailableVms(
            @RequestParam(required = false) String algorithm,
            @RequestParam(required = false) Integer minCpuCores,
            @RequestParam(required = false) Integer minMemoryMb,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String capabilities,
            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        log.info("收到可用VM查询请求: algorithm={}, minCpu={}, minMemory={}, userId={}, ip={}",
            algorithm, minCpuCores, minMemoryMb, currentUserId, clientIp);

        try {
            ConfigPreviewVO.AvailableVmsVO response = federatedTaskService.getAvailableVms(
                algorithm, minCpuCores, minMemoryMb, status, capabilities);

            log.info("可用VM查询成功: total={}, userId={}", response.getTotal(), currentUserId);
            return Result.success("查询成功", response);

        } catch (Exception e) {
            log.error("可用VM查询失败: userId={}, error={}", currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取可用数据集列表
     * GET /api/federated/config/available-datasets
     */
    @GetMapping("/config/available-datasets")
    public Result<ConfigPreviewVO.AvailableDatasetsVO> getAvailableDatasets(
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long minSize,
            @RequestParam(required = false) Long maxSize,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        log.info("收到可用数据集查询请求: dataType={}, status={}, userId={}, ip={}",
            dataType, status, currentUserId, clientIp);

        try {
            ConfigPreviewVO.AvailableDatasetsVO response = federatedTaskService.getAvailableDatasets(
                dataType, status, minSize, maxSize, keyword);

            log.info("可用数据集查询成功: total={}, userId={}", response.getTotal(), currentUserId);
            return Result.success("查询成功", response);

        } catch (Exception e) {
            log.error("可用数据集查询失败: userId={}, error={}", currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取角色配置选项
     * GET /api/federated/config/roles
     */
    @GetMapping("/config/roles")
    public Result<ConfigPreviewVO.RoleConfigVO> getRoleConfig(HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        log.info("收到角色配置查询请求: userId={}, ip={}", currentUserId, clientIp);

        try {
            ConfigPreviewVO.RoleConfigVO response = federatedTaskService.getRoleConfig();

            log.info("角色配置查询成功: roles={}, userId={}", response.getRoles().size(), currentUserId);
            return Result.success("查询成功", response);

        } catch (Exception e) {
            log.error("角色配置查询失败: userId={}, error={}", currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取算法配置模板
     * GET /api/federated/config/algorithm-templates
     */
    @GetMapping("/config/algorithm-templates")
    public Result<ConfigPreviewVO.AlgorithmTemplatesVO> getAlgorithmTemplates(HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        log.info("收到算法模板查询请求: userId={}, ip={}", currentUserId, clientIp);

        try {
            ConfigPreviewVO.AlgorithmTemplatesVO response = federatedTaskService.getAlgorithmTemplates();

            log.info("算法模板查询成功: templates={}, userId={}", response.getTemplates().size(), currentUserId);
            return Result.success("查询成功", response);

        } catch (Exception e) {
            log.error("算法模板查询失败: userId={}, error={}", currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 数据分配预览
     * POST /api/federated/tasks/preview-distribution
     */
    @PostMapping("/tasks/preview-distribution")
    public Result<ConfigPreviewVO.DistributionPreviewVO> previewDistribution(
            @Valid @RequestBody DistributionPreviewRequestDTO requestDTO,
            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        log.info("收到数据分配预览请求: datasetId={}, strategy={}, participantCount={}, userId={}, ip={}",
            requestDTO.getDatasetId(), requestDTO.getDistributionStrategy(),
            requestDTO.getParticipants().size(), currentUserId, clientIp);

        try {
            ConfigPreviewVO.DistributionPreviewVO response = federatedTaskService.previewDistribution(requestDTO);

            log.info("数据分配预览成功: participants={}, userId={}",
                response.getDistributionResult().getParticipants().size(), currentUserId);
            return Result.success("预览生成成功", response);

        } catch (Exception e) {
            log.error("数据分配预览失败: userId={}, error={}", currentUserId, e.getMessage());
            return Result.error("预览失败: " + e.getMessage());
        }
    }

    /**
     * 参与者验证
     * POST /api/federated/tasks/validate-participants
     */
    @PostMapping("/tasks/validate-participants")
    public Result<ConfigPreviewVO.ParticipantValidationVO> validateParticipants(
            @Valid @RequestBody ParticipantValidationRequestDTO requestDTO,
            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        log.info("收到参与者验证请求: algorithm={}, taskType={}, participantCount={}, userId={}, ip={}",
            requestDTO.getAlgorithm(), requestDTO.getTaskType(),
            requestDTO.getParticipants().size(), currentUserId, clientIp);

        try {
            ConfigPreviewVO.ParticipantValidationVO response = federatedTaskService.validateParticipants(requestDTO);

            log.info("参与者验证完成: overallValid={}, userId={}", response.getOverallValid(), currentUserId);
            return Result.success("验证完成", response);

        } catch (Exception e) {
            log.error("参与者验证失败: userId={}, error={}", currentUserId, e.getMessage());
            return Result.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 配置状态监控
     * GET /api/federated/tasks/{taskId}/config-status
     */
    @GetMapping("/tasks/{taskId}/config-status")
    public Result<TaskConfigStatusVO> getTaskConfigStatus(@PathVariable String taskId,
                                                        HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        log.info("收到任务配置状态查询请求: taskId={}, userId={}, ip={}", taskId, currentUserId, clientIp);

        try {
            TaskConfigStatusVO response = federatedTaskService.getTaskConfigStatus(taskId);

            log.info("任务配置状态查询成功: taskId={}, configStatus={}, userId={}",
                taskId, response.getConfigStatus(), currentUserId);
            return Result.success("查询成功", response);

        } catch (Exception e) {
            log.error("任务配置状态查询失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 资源使用监控
     * GET /api/federated/tasks/{taskId}/resource-usage
     */
    @GetMapping("/tasks/{taskId}/resource-usage")
    public Result<TaskResourceUsageVO> getTaskResourceUsage(@PathVariable String taskId,
                                                          HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        log.info("收到任务资源使用查询请求: taskId={}, userId={}, ip={}", taskId, currentUserId, clientIp);

        try {
            TaskResourceUsageVO response = federatedTaskService.getTaskResourceUsage(taskId);

            log.info("任务资源使用查询成功: taskId={}, participantCount={}, userId={}",
                taskId, response.getParticipantMetrics().size(), currentUserId);
            return Result.success("查询成功", response);

        } catch (Exception e) {
            log.error("任务资源使用查询失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    // ========== v1.3 增强的任务创建接口 ==========

    /**
     * 创建联邦学习任务 (v1.3 专用)
     * POST /api/federated/tasks
     * 只支持v1.3新格式，不再兼容v1.0旧格式
     */
    @PostMapping("/tasks")
    public Result<TaskOperationVO> createTask(@Valid @RequestBody TaskCreateDTO createDTO,
                                            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        // 安全获取参与者数量，避免NPE
        int participantCount = 0;
        if (createDTO.getParticipantConfig() != null &&
            createDTO.getParticipantConfig().getParticipants() != null) {
            participantCount = createDTO.getParticipantConfig().getParticipants().size();
        }

        log.info("收到v1.3任务创建请求: taskName={}, algorithm={}, participantCount={}, userId={}, ip={}",
            createDTO.getTaskName(), createDTO.getAlgorithm(), participantCount, currentUserId, clientIp);

        accessLog.info("v1.3联邦任务创建请求: taskName={}, algorithm={}, userId={}, ip={}",
            createDTO.getTaskName(), createDTO.getAlgorithm(), currentUserId, clientIp);

        try {
            // 只使用v1.3智能任务创建
            TaskOperationVO response = federatedTaskService.createSmartTask(createDTO, currentUserId);

            log.info("v1.3任务创建成功: taskId={}, taskName={}, userId={}",
                response.getTaskId(), createDTO.getTaskName(), currentUserId);

            return Result.success("任务创建成功", response);

        } catch (Exception e) {
            log.error("v1.3任务创建失败: taskName={}, userId={}, error={}",
                createDTO.getTaskName(), currentUserId, e.getMessage());
            return Result.error("任务创建失败: " + e.getMessage());
        }
    }

    /**
     * 配置任务参数
     * PUT /api/federated/tasks/{taskId}/config
     */
    @PutMapping("/tasks/{taskId}/config")
    public Result<TaskOperationVO> configureTask(@PathVariable String taskId,
                                               @Valid @RequestBody TaskConfigDTO configDTO,
                                               HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务配置请求: taskId={}, algorithm={}, userId={}, ip={}", 
            taskId, configDTO.getAlgorithm(), currentUserId, clientIp);

        try {
            TaskOperationVO response = federatedTaskService.configureTask(taskId, configDTO, currentUserId);
            
            log.info("任务配置成功: taskId={}, userId={}", taskId, currentUserId);
            return Result.success("任务配置成功", response);
            
        } catch (Exception e) {
            log.error("任务配置失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("任务配置失败: " + e.getMessage());
        }
    }

    /**
     * 启动任务
     * POST /api/federated/tasks/{taskId}/start
     */
    @PostMapping("/tasks/{taskId}/start")
    public Result<TaskOperationVO> startTask(@PathVariable String taskId,
                                           HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务启动请求: taskId={}, userId={}, ip={}", taskId, currentUserId, clientIp);

        try {
            TaskOperationVO response = federatedTaskService.startTask(taskId, currentUserId);
            
            log.info("任务启动成功: taskId={}, userId={}", taskId, currentUserId);
            return Result.success("任务启动成功", response);
            
        } catch (Exception e) {
            log.error("任务启动失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("任务启动失败: " + e.getMessage());
        }
    }

    /**
     * 暂停任务
     * POST /api/federated/tasks/{taskId}/pause
     */
    @PostMapping("/tasks/{taskId}/pause")
    public Result<TaskOperationVO> pauseTask(@PathVariable String taskId,
                                           HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务暂停请求: taskId={}, userId={}, ip={}", taskId, currentUserId, clientIp);

        try {
            TaskOperationVO response = federatedTaskService.pauseTask(taskId, currentUserId);
            
            log.info("任务暂停成功: taskId={}, userId={}", taskId, currentUserId);
            return Result.success("任务暂停成功", response);
            
        } catch (Exception e) {
            log.error("任务暂停失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("任务暂停失败: " + e.getMessage());
        }
    }

    /**
     * 恢复任务
     * POST /api/federated/tasks/{taskId}/resume
     */
    @PostMapping("/tasks/{taskId}/resume")
    public Result<TaskOperationVO> resumeTask(@PathVariable String taskId,
                                            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务恢复请求: taskId={}, userId={}, ip={}", taskId, currentUserId, clientIp);

        try {
            TaskOperationVO response = federatedTaskService.resumeTask(taskId, currentUserId);
            
            log.info("任务恢复成功: taskId={}, userId={}", taskId, currentUserId);
            return Result.success("任务恢复成功", response);
            
        } catch (Exception e) {
            log.error("任务恢复失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("任务恢复失败: " + e.getMessage());
        }
    }

    /**
     * 停止任务
     * POST /api/federated/tasks/{taskId}/stop
     */
    @PostMapping("/tasks/{taskId}/stop")
    public Result<TaskOperationVO> stopTask(@PathVariable String taskId,
                                          @RequestBody(required = false) TaskStopDTO stopDTO,
                                          HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        // 设置默认值
        if (stopDTO == null) {
            stopDTO = TaskStopDTO.builder()
                .reason("用户主动停止")
                .saveCheckpoint(true)
                .build();
        }
        
        log.info("收到任务停止请求: taskId={}, reason={}, userId={}, ip={}", 
            taskId, stopDTO.getReason(), currentUserId, clientIp);

        try {
            TaskOperationVO response = federatedTaskService.stopTask(taskId, stopDTO, currentUserId);
            
            log.info("任务停止成功: taskId={}, userId={}", taskId, currentUserId);
            return Result.success("任务停止成功", response);
            
        } catch (Exception e) {
            log.error("任务停止失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("任务停止失败: " + e.getMessage());
        }
    }

    /**
     * 取消任务
     * POST /api/federated/tasks/{taskId}/cancel
     */
    @PostMapping("/tasks/{taskId}/cancel")
    public Result<TaskOperationVO> cancelTask(@PathVariable String taskId,
                                            @Valid @RequestBody TaskCancelDTO cancelDTO,
                                            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务取消请求: taskId={}, reason={}, userId={}, ip={}", 
            taskId, cancelDTO.getReason(), currentUserId, clientIp);

        try {
            TaskOperationVO response = federatedTaskService.cancelTask(taskId, cancelDTO, currentUserId);
            
            log.info("任务取消成功: taskId={}, userId={}", taskId, currentUserId);
            return Result.success("任务取消成功", response);
            
        } catch (Exception e) {
            log.error("任务取消失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("任务取消失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务状态
     * GET /api/federated/tasks/{taskId}
     */
    @GetMapping("/tasks/{taskId}")
    public Result<TaskDetailVO> getTaskDetail(@PathVariable String taskId,
                                            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务状态查询请求: taskId={}, userId={}, ip={}", taskId, currentUserId, clientIp);

        try {
            TaskDetailVO response = federatedTaskService.getTaskDetail(taskId);
            
            log.info("任务状态查询成功: taskId={}, status={}, userId={}", 
                taskId, response.getStatus(), currentUserId);
            return Result.success("查询成功", response);
            
        } catch (Exception e) {
            log.error("任务状态查询失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务列表
     * GET /api/federated/tasks
     */
    @GetMapping("/tasks")
    public Result<TaskListVO> getTaskList(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "20") Integer size,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String algorithm,
                                        HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务列表查询请求: page={}, size={}, status={}, userId={}, ip={}", 
            page, size, status, currentUserId, clientIp);

        try {
            TaskQueryDTO queryDTO = TaskQueryDTO.builder()
                .page(page)
                .size(size)
                .status(status)
                .type(type)
                .keyword(keyword)
                .algorithm(algorithm)
                .build();
            
            // 解析日期参数
            if (startDate != null && !startDate.isEmpty()) {
                // 解析startDate字符串为LocalDateTime
            }
            if (endDate != null && !endDate.isEmpty()) {
                // 解析endDate字符串为LocalDateTime
            }

            TaskListVO response = federatedTaskService.getTaskList(queryDTO);
            
            log.info("任务列表查询成功: total={}, current={}, userId={}", 
                response.getTotal(), response.getTasks().size(), currentUserId);
            return Result.success("查询成功", response);
            
        } catch (Exception e) {
            log.error("任务列表查询失败: userId={}, error={}", currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务结果
     * GET /api/federated/tasks/{taskId}/results
     */
    @GetMapping("/tasks/{taskId}/results")
    public Result<TaskResultVO> getTaskResult(@PathVariable String taskId,
                                            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务结果查询请求: taskId={}, userId={}, ip={}", taskId, currentUserId, clientIp);

        try {
            TaskResultVO response = federatedTaskService.getTaskResult(taskId);
            
            log.info("任务结果查询成功: taskId={}, userId={}", taskId, currentUserId);
            return Result.success("查询成功", response);
            
        } catch (Exception e) {
            log.error("任务结果查询失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务全局模型列表
     * GET /api/federated/tasks/{taskId}/global-models
     */
    @GetMapping("/tasks/{taskId}/global-models")
    public Result<GlobalModelsVO> getTaskGlobalModels(@PathVariable String taskId,
                                                     HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        log.info("收到任务全局模型查询请求: taskId={}, userId={}, ip={}",
            taskId, currentUserId, clientIp);

        try {
            GlobalModelsVO globalModels = federatedTaskService.getTaskGlobalModels(taskId);

            log.info("任务全局模型查询成功: taskId={}, modelCount={}, userId={}",
                taskId, globalModels.getModels().size(), currentUserId);

            return Result.success(globalModels);
        } catch (Exception e) {
            log.error("任务全局模型查询失败: taskId={}, userId={}, error={}",
                taskId, currentUserId, e.getMessage(), e);
            return Result.error("任务全局模型查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务日志
     * GET /api/federated/tasks/{taskId}/logs
     */
    @GetMapping("/tasks/{taskId}/logs")
    public Result<TaskLogVO> getTaskLogs(@PathVariable String taskId,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       @RequestParam(required = false) String level,
                                       @RequestParam(required = false) String startTime,
                                       @RequestParam(required = false) String endTime,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String source,
                                       @RequestParam(required = false) String vmId,
                                       HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务日志查询请求: taskId={}, page={}, size={}, level={}, userId={}, ip={}", 
            taskId, page, size, level, currentUserId, clientIp);

        try {
            TaskLogQueryDTO queryDTO = TaskLogQueryDTO.builder()
                .page(page)
                .size(size)
                .level(level)
                .keyword(keyword)
                .source(source)
                .vmId(vmId)
                .build();
            
            // 解析时间参数
            if (startTime != null && !startTime.isEmpty()) {
                // 解析startTime字符串为LocalDateTime
            }
            if (endTime != null && !endTime.isEmpty()) {
                // 解析endTime字符串为LocalDateTime
            }

            TaskLogVO response = federatedTaskService.getTaskLogs(taskId, queryDTO);
            
            log.info("任务日志查询成功: taskId={}, total={}, current={}, userId={}", 
                taskId, response.getTotal(), response.getLogs().size(), currentUserId);
            return Result.success("查询成功", response);
            
        } catch (Exception e) {
            log.error("任务日志查询失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 删除任务
     * DELETE /api/federated/tasks/{taskId}
     */
    @DeleteMapping("/tasks/{taskId}")
    public Result<TaskOperationVO> deleteTask(@PathVariable String taskId,
                                            @RequestBody(required = false) TaskDeleteDTO deleteDTO,
                                            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        // 设置默认值
        if (deleteDTO == null) {
            deleteDTO = TaskDeleteDTO.builder()
                .deleteData(true)
                .deleteModel(false)
                .build();
        }
        
        log.info("收到任务删除请求: taskId={}, deleteData={}, deleteModel={}, userId={}, ip={}", 
            taskId, deleteDTO.getDeleteData(), deleteDTO.getDeleteModel(), currentUserId, clientIp);

        try {
            TaskOperationVO response = federatedTaskService.deleteTask(taskId, deleteDTO, currentUserId);
            
            log.info("任务删除成功: taskId={}, userId={}", taskId, currentUserId);
            return Result.success("任务删除成功", response);
            
        } catch (Exception e) {
            log.error("任务删除失败: taskId={}, userId={}, error={}", taskId, currentUserId, e.getMessage());
            return Result.error("任务删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务统计信息（额外功能）
     * GET /api/federated/tasks/stats
     */
    @GetMapping("/tasks/stats")
    public Result<Object> getTaskStats(HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务统计查询请求: userId={}, ip={}", currentUserId, clientIp);

        try {
            // 调用Service获取真实的任务统计信息
            TaskStatisticsVO stats = federatedTaskService.getTaskStatistics();

            log.info("任务统计查询成功: userId={}, totalTasks={}", currentUserId, stats.getTotalTasks());
            return Result.success("查询成功", stats);
            
        } catch (Exception e) {
            log.error("任务统计查询失败: userId={}, error={}", currentUserId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 批量操作任务（额外功能）
     * POST /api/federated/tasks/batch
     */
    @PostMapping("/tasks/batch")
    public Result<TaskBatchOperationResultVO> batchOperation(@RequestBody TaskBatchOperationDTO batchDTO,
                                                            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();

        log.info("收到任务批量操作请求: userId={}, ip={}, operation={}, taskCount={}",
            currentUserId, clientIp, batchDTO.getOperation(),
            batchDTO.getTaskIds() != null ? batchDTO.getTaskIds().size() : 0);

        try {
            // 调用Service执行批量操作
            TaskBatchOperationResultVO result = federatedTaskService.batchOperateTask(batchDTO, currentUserId);

            log.info("任务批量操作成功: userId={}, operation={}, success={}, failure={}",
                currentUserId, batchDTO.getOperation(), result.getSuccessCount(), result.getFailureCount());
            return Result.success("操作成功", result);

        } catch (Exception e) {
            log.error("任务批量操作失败: userId={}, operation={}, error={}",
                currentUserId, batchDTO.getOperation(), e.getMessage());
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    // ========== v1.3 支持的请求DTO类 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionPreviewRequestDTO {
        @NotBlank(message = "数据集ID不能为空")
        private String datasetId;

        @NotBlank(message = "分配策略不能为空")
        private String distributionStrategy;

        @Valid
        @NotEmpty(message = "参与者列表不能为空")
        private List<ParticipantRequestDTO> participants;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ParticipantRequestDTO {
            @NotBlank(message = "虚拟机ID不能为空")
            private String vmId;

            @DecimalMin(value = "0.0", message = "请求比例不能小于0.0")
            @DecimalMax(value = "1.0", message = "请求比例不能大于1.0")
            private Double requestedRatio;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantValidationRequestDTO {
        @NotBlank(message = "算法类型不能为空")
        private String algorithm;

        @NotBlank(message = "任务类型不能为空")
        private String taskType;

        @Valid
        @NotEmpty(message = "参与者列表不能为空")
        private List<ParticipantForValidationDTO> participants;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ParticipantForValidationDTO {
            @NotBlank(message = "虚拟机ID不能为空")
            private String vmId;

            @NotBlank(message = "参与者角色不能为空")
            private String role;
        }
    }
}