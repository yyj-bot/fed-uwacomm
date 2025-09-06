package com.feduwacomm.controller;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.common.Result;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.FederatedTaskService;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.vo.*;
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

    /**
     * 创建联邦学习任务
     * POST /api/federated/tasks
     */
    @PostMapping("/tasks")
    public Result<TaskOperationVO> createTask(@Valid @RequestBody TaskCreateDTO createDTO,
                                            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务创建请求: taskName={}, algorithm={}, participantCount={}, userId={}, ip={}", 
            createDTO.getTaskName(), createDTO.getAlgorithm(), 
            createDTO.getParticipants().size(), currentUserId, clientIp);
        
        accessLog.info("联邦任务创建请求: taskName={}, algorithm={}, userId={}, ip={}", 
            createDTO.getTaskName(), createDTO.getAlgorithm(), currentUserId, clientIp);

        try {
            TaskOperationVO response = federatedTaskService.createTask(createDTO, currentUserId);
            
            log.info("任务创建成功: taskId={}, taskName={}, userId={}", 
                response.getTaskId(), createDTO.getTaskName(), currentUserId);
            
            return Result.success("任务创建成功", response);
            
        } catch (Exception e) {
            log.error("任务创建失败: taskName={}, userId={}, error={}", 
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
                .createdBy(currentUserId) // 只查询当前用户创建的任务
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
            // 这里可以实现任务统计功能
            Object stats = new Object(); // 占位符
            
            log.info("任务统计查询成功: userId={}", currentUserId);
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
    public Result<Object> batchOperation(@RequestBody Object batchDTO,
                                       HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String currentUserId = BaseContext.getCurrentId();
        
        log.info("收到任务批量操作请求: userId={}, ip={}", currentUserId, clientIp);

        try {
            // 这里可以实现批量操作功能
            Object result = new Object(); // 占位符
            
            log.info("任务批量操作成功: userId={}", currentUserId);
            return Result.success("操作成功", result);
            
        } catch (Exception e) {
            log.error("任务批量操作失败: userId={}, error={}", currentUserId, e.getMessage());
            return Result.error("操作失败: " + e.getMessage());
        }
    }
}