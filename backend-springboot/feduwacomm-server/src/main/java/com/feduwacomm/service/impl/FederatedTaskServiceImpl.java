package com.feduwacomm.service.impl;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.event.FederatedTaskCreatedEvent;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.service.FederatedTaskService;
import com.feduwacomm.service.LogService;
import com.feduwacomm.vo.*;
import com.feduwacomm.controller.FederatedTaskController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 联邦学习任务服务实现类
 */
@Service
public class FederatedTaskServiceImpl implements FederatedTaskService {

    private static final Logger log = LoggerFactory.getLogger(FederatedTaskServiceImpl.class);

    @Autowired
    private FederatedTasksMapper tasksMapper;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private LogService logService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // 任务状态常量
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_CONFIGURED = "CONFIGURED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_PAUSED = "PAUSED";
    private static final String STATUS_STOPPED = "STOPPED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Override
    @Transactional
    public TaskOperationVO createTask(TaskCreateDTO createDTO, String createdBy) {
        log.info("开始创建联邦学习任务: taskName={}, algorithm={}, createdBy={}", 
            createDTO.getTaskName(), createDTO.getAlgorithm(), createdBy);

        // 验证任务配置
        if (!isValidTaskConfig(createDTO)) {
            throw new UserException("任务配置无效");
        }

        // 生成任务ID
        String taskId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        // 构建任务实体
        FederatedTask task = buildTaskFromCreateDTO(createDTO, taskId, createdBy, now);

        // 插入任务记录
        int result = tasksMapper.insertTask(task);
        if (result <= 0) {
            throw new UserException("任务创建失败");
        }

        // 创建参与者记录
        if (createDTO.getParticipants() != null && !createDTO.getParticipants().isEmpty()) {
            for (TaskCreateDTO.ParticipantDTO participantDTO : createDTO.getParticipants()) {
                TaskParticipant participant = buildParticipantFromDTO(participantDTO, taskId, now);
                tasksMapper.insertParticipant(participant);
            }
        }

        // 记录操作日志
        logTask(taskId, "INFO", "任务创建成功", "TASK_MANAGER", null, 
            Map.of("participants", createDTO.getParticipants()));

        // 构建响应
        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .taskName(createDTO.getTaskName())
            .status(STATUS_CREATED)
            .createdAt(now)
            .createdBy(createdBy)
            .participantCount(createDTO.getParticipants().size())
            .estimatedDuration(estimateTaskDuration(createDTO))
            .build();

        // 发布任务创建事件，触发自动工作流
        FederatedTaskCreatedEvent taskCreatedEvent = new FederatedTaskCreatedEvent(
            taskId, 
            createDTO.getTaskName(), 
            createdBy
        );
        eventPublisher.publishEvent(taskCreatedEvent);
        
        log.info("联邦学习任务创建完成，已发布事件触发工作流: taskId={}, participantCount={}", 
            taskId, createDTO.getParticipants().size());

        return response;
    }

    @Override
    @Transactional
    public TaskOperationVO configureTask(String taskId, TaskConfigDTO configDTO, String updatedBy) {
        log.info("开始配置任务: taskId={}, updatedBy={}", taskId, updatedBy);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        if (!STATUS_CREATED.equals(task.getStatus()) && !STATUS_CONFIGURED.equals(task.getStatus())) {
            throw new UserException("任务状态不允许配置");
        }

        // 更新任务配置
        updateTaskFromConfigDTO(task, configDTO, updatedBy);
        task.setStatus(STATUS_CONFIGURED);
        task.setUpdatedAt(LocalDateTime.now());

        int result = tasksMapper.updateTask(task);
        if (result <= 0) {
            throw new UserException("任务配置失败");
        }

        // 记录操作日志
        logTask(taskId, "INFO", "任务配置更新", "TASK_MANAGER", null, null);

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_CONFIGURED)
            .updatedAt(LocalDateTime.now())
            .configVersion("v1.1")
            .build();

        log.info("任务配置完成: taskId={}", taskId);
        return response;
    }

    @Override
    @Transactional
    public TaskOperationVO startTask(String taskId, String operatorId) {
        log.info("开始启动任务: taskId={}, operatorId={}", taskId, operatorId);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        if (!STATUS_CONFIGURED.equals(task.getStatus())) {
            throw new UserException("任务状态不允许启动，当前状态: " + task.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        
        // 更新任务状态
        tasksMapper.updateTaskStatus(taskId, STATUS_RUNNING, now);

        // 更新参与者状态为连接中
        List<TaskParticipant> participants = tasksMapper.selectParticipantsByTaskId(taskId);
        for (TaskParticipant participant : participants) {
            tasksMapper.updateParticipantStatus(taskId, participant.getVmId(), "CONNECTED", now);
        }

        // 记录操作日志
        logTask(taskId, "INFO", "任务启动成功", "TASK_MANAGER", null, 
            Map.of("participants", participants.stream().map(TaskParticipant::getVmId).collect(Collectors.toList())));

        // 构建参与者状态列表
        List<TaskOperationVO.ParticipantStatus> participantStatuses = participants.stream()
            .map(p -> TaskOperationVO.ParticipantStatus.builder()
                .vmId(p.getVmId())
                .status("CONNECTED")
                .dataSource(p.getDataSource())
                .build())
            .collect(Collectors.toList());

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_RUNNING)
            .startedAt(now)
            .currentRound(0)
            .participants(participantStatuses)
            .build();

        log.info("任务启动完成: taskId={}, participantCount={}", taskId, participants.size());
        return response;
    }

    @Override
    @Transactional
    public TaskOperationVO pauseTask(String taskId, String operatorId) {
        log.info("开始暂停任务: taskId={}, operatorId={}", taskId, operatorId);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        if (!STATUS_RUNNING.equals(task.getStatus())) {
            throw new UserException("只有运行中的任务才能暂停");
        }

        LocalDateTime now = LocalDateTime.now();
        
        // 更新任务状态
        tasksMapper.updateTaskStatus(taskId, STATUS_PAUSED, now);

        // 记录操作日志
        logTask(taskId, "INFO", "任务暂停成功", "TASK_MANAGER", null, null);

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_PAUSED)
            .pausedAt(now)
            .currentRound(task.getCurrentRound())
            .resumePoint(TaskOperationVO.ResumePointVO.builder()
                .round(task.getCurrentRound())
                .step("AGGREGATION")
                .build())
            .build();

        log.info("任务暂停完成: taskId={}, currentRound={}", taskId, task.getCurrentRound());
        return response;
    }

    @Override
    @Transactional
    public TaskOperationVO resumeTask(String taskId, String operatorId) {
        log.info("开始恢复任务: taskId={}, operatorId={}", taskId, operatorId);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        if (!STATUS_PAUSED.equals(task.getStatus())) {
            throw new UserException("只有暂停的任务才能恢复");
        }

        LocalDateTime now = LocalDateTime.now();
        
        // 更新任务状态
        tasksMapper.updateTaskStatus(taskId, STATUS_RUNNING, now);

        // 记录操作日志
        logTask(taskId, "INFO", "任务恢复成功", "TASK_MANAGER", null, null);

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_RUNNING)
            .resumedAt(now)
            .currentRound(task.getCurrentRound())
            .build();

        log.info("任务恢复完成: taskId={}, currentRound={}", taskId, task.getCurrentRound());
        return response;
    }

    @Override
    @Transactional
    public TaskOperationVO stopTask(String taskId, TaskStopDTO stopDTO, String operatorId) {
        log.info("开始停止任务: taskId={}, reason={}, operatorId={}", 
            taskId, stopDTO.getReason(), operatorId);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        if (!STATUS_RUNNING.equals(task.getStatus()) && !STATUS_PAUSED.equals(task.getStatus())) {
            throw new UserException("只有运行中或暂停的任务才能停止");
        }

        LocalDateTime now = LocalDateTime.now();
        
        // 更新任务状态
        tasksMapper.updateTaskStatus(taskId, STATUS_STOPPED, now);

        // 保存检查点（如果需要）
        String checkpointPath = null;
        if (stopDTO.getSaveCheckpoint() != null && stopDTO.getSaveCheckpoint()) {
            checkpointPath = "/checkpoints/task_" + taskId + "_round_" + task.getCurrentRound() + ".pkl";
        }

        // 记录操作日志
        logTask(taskId, "INFO", "任务停止成功", "TASK_MANAGER", null, 
            Map.of("reason", stopDTO.getReason(), "saveCheckpoint", stopDTO.getSaveCheckpoint()));

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_STOPPED)
            .stoppedAt(now)
            .finalRound(task.getCurrentRound())
            .checkpointSaved(stopDTO.getSaveCheckpoint())
            .checkpointPath(checkpointPath)
            .build();

        log.info("任务停止完成: taskId={}, finalRound={}", taskId, task.getCurrentRound());
        return response;
    }

    @Override
    @Transactional
    public TaskOperationVO cancelTask(String taskId, TaskCancelDTO cancelDTO, String operatorId) {
        log.info("开始取消任务: taskId={}, reason={}, operatorId={}", 
            taskId, cancelDTO.getReason(), operatorId);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        if (STATUS_COMPLETED.equals(task.getStatus()) || STATUS_CANCELLED.equals(task.getStatus())) {
            throw new UserException("已完成或已取消的任务不能再次取消");
        }

        LocalDateTime now = LocalDateTime.now();
        
        // 更新任务状态
        tasksMapper.updateTaskStatus(taskId, STATUS_CANCELLED, now);

        // 记录操作日志
        logTask(taskId, "INFO", "任务取消成功", "TASK_MANAGER", null, 
            Map.of("reason", cancelDTO.getReason()));

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_CANCELLED)
            .cancelledAt(now)
            .reason(cancelDTO.getReason())
            .build();

        log.info("任务取消完成: taskId={}, reason={}", taskId, cancelDTO.getReason());
        return response;
    }

    @Override
    @Transactional
    public TaskOperationVO deleteTask(String taskId, TaskDeleteDTO deleteDTO, String operatorId) {
        log.info("开始删除任务: taskId={}, deleteData={}, deleteModel={}, operatorId={}", 
            taskId, deleteDTO.getDeleteData(), deleteDTO.getDeleteModel(), operatorId);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        LocalDateTime now = LocalDateTime.now();

        // 删除相关数据
        if (deleteDTO.getDeleteData() != null && deleteDTO.getDeleteData()) {
            // 删除参与者数据
            tasksMapper.deleteParticipantsByTaskId(taskId);
            // 删除任务日志
            // 任务日志已统一管理，这里不再需要删除
        }

        // 删除任务记录
        int result = tasksMapper.deleteTask(taskId);
        if (result <= 0) {
            throw new UserException("任务删除失败");
        }

        // 记录操作日志（在删除任务记录前记录）
        logTask(taskId, "INFO", "任务删除成功", "TASK_MANAGER", null, 
            Map.of("deleteData", deleteDTO.getDeleteData(), "deleteModel", deleteDTO.getDeleteModel()));

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .deletedAt(now)
            .dataDeleted(deleteDTO.getDeleteData())
            .modelPreserved(!deleteDTO.getDeleteModel())
            .build();

        log.info("任务删除完成: taskId={}", taskId);
        return response;
    }

    @Override
    public TaskDetailVO getTaskDetail(String taskId) {
        log.info("查询任务详情: taskId={}", taskId);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        // 获取参与者信息
        List<TaskParticipant> participants = tasksMapper.selectParticipantsByTaskId(taskId);
        List<TaskDetailVO.ParticipantVO> participantVOs = participants.stream()
            .map(this::convertToParticipantVO)
            .collect(Collectors.toList());

        // 构建指标信息
        TaskDetailVO.MetricsVO metrics = buildTaskMetrics(task, participants);

        // 构建任务详情
        TaskDetailVO taskDetail = TaskDetailVO.builder()
            .taskId(task.getId())
            .taskName(task.getTaskName())
            .taskType(task.getTaskType())
            .description(task.getDescription())
            .status(task.getStatus())
            .algorithm(task.getAlgorithm())
            .createdAt(task.getCreatedAt())
            .startedAt(task.getStartedAt())
            .pausedAt(task.getPausedAt())
            .resumedAt(task.getResumedAt())
            .stoppedAt(task.getStoppedAt())
            .completedAt(task.getCompletedAt())
            .cancelledAt(task.getCancelledAt())
            .currentRound(task.getCurrentRound())
            .totalRounds(task.getTotalRounds())
            .progress(task.getProgress())
            .participants(participantVOs)
            .metrics(metrics)
            .build();

        log.info("任务详情查询完成: taskId={}, status={}", taskId, task.getStatus());
        return taskDetail;
    }

    @Override
    public TaskListVO getTaskList(TaskQueryDTO queryDTO) {
        log.info("查询任务列表: page={}, size={}, status={}", 
            queryDTO.getPage(), queryDTO.getSize(), queryDTO.getStatus());

        // 计算偏移量
        int offset = (queryDTO.getPage() - 1) * queryDTO.getSize();
        queryDTO.setPage(offset);

        // 查询任务列表
        List<FederatedTask> tasks = tasksMapper.selectTasksByQuery(queryDTO);
        int total = tasksMapper.countTasksByQuery(queryDTO);

        // 转换为VO
        List<TaskVO> taskVOs = tasks.stream()
            .map(this::convertToTaskVO)
            .collect(Collectors.toList());

        TaskListVO response = TaskListVO.builder()
            .total(total)
            .page(queryDTO.getPage() / queryDTO.getSize() + 1)
            .size(queryDTO.getSize())
            .tasks(taskVOs)
            .build();

        log.info("任务列表查询完成: total={}, current={}", total, tasks.size());
        return response;
    }

    @Override
    public TaskResultVO getTaskResult(String taskId) {
        log.info("查询任务结果: taskId={}", taskId);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        if (!STATUS_COMPLETED.equals(task.getStatus())) {
            throw new UserException("任务尚未完成，无法获取结果");
        }

        // 构建任务结果（这里使用模拟数据，实际应从数据库或文件中读取）
        TaskResultVO result = buildTaskResult(task, taskId);

        log.info("任务结果查询完成: taskId={}", taskId);
        return result;
    }

    @Override
    public TaskLogVO getTaskLogs(String taskId, TaskLogQueryDTO queryDTO) {
        log.info("查询任务日志: taskId={}, page={}, size={}", 
            taskId, queryDTO.getPage(), queryDTO.getSize());

        if (!taskExists(taskId)) {
            throw new UserException("任务不存在");
        }

        // 使用统一的日志服务查询任务相关日志
        // 这里应该调用 LogService 来查询，暂时返回空结果
        TaskLogVO response = TaskLogVO.builder()
            .taskId(taskId)
            .total(0)
            .page(queryDTO.getPage())
            .size(queryDTO.getSize())
            .logs(List.of())
            .build();

        log.info("任务日志查询完成: taskId={}, total={}", taskId, 0);
        return response;
    }

    // 实现其他接口方法...
    @Override
    public void addParticipant(String taskId, TaskParticipant participant) {
        participant.setTaskId(taskId);
        participant.setCreatedAt(LocalDateTime.now());
        tasksMapper.insertParticipant(participant);
    }

    @Override
    public void updateParticipantStatus(String taskId, String vmId, String status) {
        tasksMapper.updateParticipantStatus(taskId, vmId, status, LocalDateTime.now());
    }

    @Override
    public void removeParticipant(String taskId, String vmId) {
        // 实现移除参与者逻辑
    }

    @Override
    public List<TaskParticipant> getTaskParticipants(String taskId) {
        return tasksMapper.selectParticipantsByTaskId(taskId);
    }

    @Override
    public void logTask(String taskId, String level, String message, String source, String vmId, Object details) {
        // 使用统一的日志服务记录任务日志
        logService.logTask(taskId, level, message, source, vmId, details);
    }

    @Override
    public boolean taskExists(String taskId) {
        return getTaskById(taskId) != null;
    }

    @Override
    public boolean hasTaskPermission(String userId, String taskId, String operation) {
        // 实现权限检查逻辑
        return true; // 暂时返回true
    }

    @Override
    public boolean isValidStatusTransition(String currentStatus, String targetStatus) {
        // 定义状态转换规则
        Map<String, Set<String>> validTransitions = Map.of(
            STATUS_CREATED, Set.of(STATUS_CONFIGURED, STATUS_CANCELLED),
            STATUS_CONFIGURED, Set.of(STATUS_RUNNING, STATUS_CANCELLED),
            STATUS_RUNNING, Set.of(STATUS_PAUSED, STATUS_STOPPED, STATUS_COMPLETED, STATUS_FAILED),
            STATUS_PAUSED, Set.of(STATUS_RUNNING, STATUS_STOPPED, STATUS_CANCELLED),
            STATUS_STOPPED, Set.of(),
            STATUS_COMPLETED, Set.of(),
            STATUS_FAILED, Set.of(),
            STATUS_CANCELLED, Set.of()
        );

        return validTransitions.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    @Override
    public boolean isValidTaskConfig(TaskCreateDTO createDTO) {
        if (createDTO.getTaskName() == null || createDTO.getTaskName().trim().isEmpty()) {
            return false;
        }
        if (createDTO.getParticipants() == null || createDTO.getParticipants().isEmpty()) {
            return false;
        }
        return createDTO.getAlgorithm() != null && !createDTO.getAlgorithm().trim().isEmpty();
    }

    @Override
    public FederatedTask getTaskById(String taskId) {
        return tasksMapper.selectTaskById(taskId);
    }

    @Override
    public double calculateTaskProgress(String taskId) {
        FederatedTask task = getTaskById(taskId);
        if (task == null || task.getTotalRounds() == null || task.getTotalRounds() <= 0) {
            return 0.0;
        }

        int currentRound = task.getCurrentRound() != null ? task.getCurrentRound() : 0;
        return (double) currentRound / task.getTotalRounds() * 100;
    }

    @Override
    public String generateConfigJson(TaskCreateDTO createDTO) {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("hyperparameters", createDTO.getHyperparameters());
            config.put("modelConfig", createDTO.getModelConfig());
            config.put("schedule", createDTO.getSchedule());
            config.put("securityConfig", createDTO.getSecurityConfig());
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("生成配置JSON失败: {}", e.getMessage());
            return "{}";
        }
    }

    @Override
    public String generateConfigJson(TaskConfigDTO configDTO) {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("algorithm", configDTO.getAlgorithm());
            config.put("hyperparameters", configDTO.getHyperparameters());
            config.put("modelConfig", configDTO.getModelConfig());
            config.put("dataConfig", configDTO.getDataConfig());
            config.put("securityConfig", configDTO.getSecurityConfig());
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("生成配置JSON失败: {}", e.getMessage());
            return "{}";
        }
    }

    @Override
    public int estimateTaskDuration(TaskCreateDTO createDTO) {
        // 基于超参数估算任务执行时间
        int baseTime = 3600; // 基础时间1小时
        if (createDTO.getHyperparameters() != null) {
            Integer epochs = createDTO.getHyperparameters().getEpochs();
            Integer rounds = createDTO.getHyperparameters().getRounds();
            if (epochs != null && rounds != null) {
                return baseTime * rounds * epochs / 100; // 估算公式
            }
        }
        return baseTime;
    }

    // 私有辅助方法

    private FederatedTask buildTaskFromCreateDTO(TaskCreateDTO createDTO, String taskId, String createdBy, LocalDateTime now) {
        FederatedTask task = new FederatedTask();
        task.setId(taskId);
        task.setTaskName(createDTO.getTaskName());
        task.setTaskType(createDTO.getTaskType());
        task.setDescription(createDTO.getDescription());
        task.setAlgorithm(createDTO.getAlgorithm());
        task.setStatus(STATUS_CREATED);
        
        // 设置超参数
        if (createDTO.getHyperparameters() != null) {
            task.setLearningRate(createDTO.getHyperparameters().getLearningRate());
            task.setBatchSize(createDTO.getHyperparameters().getBatchSize());
            task.setEpochs(createDTO.getHyperparameters().getEpochs());
            task.setTotalRounds(createDTO.getHyperparameters().getRounds());
            task.setMinParticipants(createDTO.getHyperparameters().getMinParticipants());
        }
        
        // 设置模型配置
        if (createDTO.getModelConfig() != null) {
            task.setModelType(createDTO.getModelConfig().getModelType());
            task.setTestSize(createDTO.getModelConfig().getTestSize());
            task.setRandomState(createDTO.getModelConfig().getRandomState());
        }
        
        // 设置调度配置
        if (createDTO.getSchedule() != null) {
            task.setStartTime(createDTO.getSchedule().getStartTime());
            task.setEndTime(createDTO.getSchedule().getEndTime());
            task.setTimeout(createDTO.getSchedule().getTimeout());
        }
        
        // 设置安全配置
        if (createDTO.getSecurityConfig() != null) {
            task.setEncryption(createDTO.getSecurityConfig().getEncryption());
            task.setSecureAggregation(createDTO.getSecurityConfig().getSecureAggregation());
            if (createDTO.getSecurityConfig().getDifferentialPrivacy() != null) {
                task.setDifferentialPrivacy(createDTO.getSecurityConfig().getDifferentialPrivacy().getEnabled());
                task.setEpsilon(createDTO.getSecurityConfig().getDifferentialPrivacy().getEpsilon());
                task.setDelta(createDTO.getSecurityConfig().getDifferentialPrivacy().getDelta());
            }
        }
        
        task.setCurrentRound(0);
        task.setProgress(0.0);
        task.setParticipantCount(createDTO.getParticipants().size());
        task.setEstimatedDuration(estimateTaskDuration(createDTO));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setCreatedBy(createdBy);
        task.setConfig(generateConfigJson(createDTO));
        
        return task;
    }

    private TaskParticipant buildParticipantFromDTO(TaskCreateDTO.ParticipantDTO participantDTO, String taskId, LocalDateTime now) {
        return TaskParticipant.builder()
            .id(UUID.randomUUID().toString().replace("-", ""))
            .taskId(taskId)
            .vmId(participantDTO.getVmId())
            .role(participantDTO.getRole())
            .status("PENDING")
            .dataSource(participantDTO.getDataSource())
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private void updateTaskFromConfigDTO(FederatedTask task, TaskConfigDTO configDTO, String updatedBy) {
        if (configDTO.getAlgorithm() != null) {
            task.setAlgorithm(configDTO.getAlgorithm());
        }
        
        if (configDTO.getHyperparameters() != null) {
            if (configDTO.getHyperparameters().getLearningRate() != null) {
                task.setLearningRate(configDTO.getHyperparameters().getLearningRate());
            }
            if (configDTO.getHyperparameters().getBatchSize() != null) {
                task.setBatchSize(configDTO.getHyperparameters().getBatchSize());
            }
            if (configDTO.getHyperparameters().getEpochs() != null) {
                task.setEpochs(configDTO.getHyperparameters().getEpochs());
            }
            if (configDTO.getHyperparameters().getRounds() != null) {
                task.setTotalRounds(configDTO.getHyperparameters().getRounds());
            }
            if (configDTO.getHyperparameters().getMinParticipants() != null) {
                task.setMinParticipants(configDTO.getHyperparameters().getMinParticipants());
            }
        }
        
        task.setUpdatedBy(updatedBy);
        task.setConfig(generateConfigJson(configDTO));
    }

    private TaskDetailVO.ParticipantVO convertToParticipantVO(TaskParticipant participant) {
        return TaskDetailVO.ParticipantVO.builder()
            .vmId(participant.getVmId())
            .role(participant.getRole())
            .status(participant.getStatus())
            .lastHeartbeat(participant.getLastHeartbeat())
            .currentEpoch(participant.getCurrentEpoch())
            .loss(participant.getLoss())
            .accuracy(participant.getAccuracy())
            .dataSource(participant.getDataSource())
            .build();
    }

    private TaskDetailVO.MetricsVO buildTaskMetrics(FederatedTask task, List<TaskParticipant> participants) {
        // 计算全局指标
        double globalLoss = participants.stream()
            .filter(p -> p.getLoss() != null)
            .mapToDouble(TaskParticipant::getLoss)
            .average()
            .orElse(0.0);

        double globalAccuracy = participants.stream()
            .filter(p -> p.getAccuracy() != null)
            .mapToDouble(TaskParticipant::getAccuracy)
            .average()
            .orElse(0.0);

        int estimatedTimeRemaining = 0;
        if (STATUS_RUNNING.equals(task.getStatus()) && task.getTotalRounds() != null) {
            int remainingRounds = task.getTotalRounds() - (task.getCurrentRound() != null ? task.getCurrentRound() : 0);
            estimatedTimeRemaining = remainingRounds * 180; // 假设每轮3分钟
        }

        return TaskDetailVO.MetricsVO.builder()
            .globalLoss(globalLoss)
            .globalAccuracy(globalAccuracy)
            .communicationRounds(task.getCurrentRound())
            .dataProcessed(participants.stream()
                .filter(p -> p.getDataSize() != null)
                .mapToInt(TaskParticipant::getDataSize)
                .sum())
            .estimatedTimeRemaining(estimatedTimeRemaining)
            .build();
    }

    private TaskVO convertToTaskVO(FederatedTask task) {
        return TaskVO.builder()
            .taskId(task.getId())
            .taskName(task.getTaskName())
            .taskType(task.getTaskType())
            .status(task.getStatus())
            .algorithm(task.getAlgorithm())
            .createdAt(task.getCreatedAt())
            .startedAt(task.getStartedAt())
            .completedAt(task.getCompletedAt())
            .participantCount(task.getParticipantCount())
            .currentRound(task.getCurrentRound())
            .totalRounds(task.getTotalRounds())
            .progress(task.getProgress())
            .createdBy(task.getCreatedBy())
            .build();
    }

    private TaskResultVO buildTaskResult(FederatedTask task, String taskId) {
        // 构建模拟的任务结果
        TaskResultVO.FinalResultsVO finalResults = TaskResultVO.FinalResultsVO.builder()
            .accuracy(0.892)
            .loss(0.098)
            .precision(0.885)
            .recall(0.890)
            .f1Score(0.887)
            .confusionMatrix(new int[][]{{45, 5}, {8, 42}})
            .build();

        List<TaskResultVO.RoundResultVO> roundResults = new ArrayList<>();
        for (int i = 1; i <= task.getCurrentRound(); i++) {
            roundResults.add(TaskResultVO.RoundResultVO.builder()
                .round(i)
                .accuracy(0.750 + i * 0.025)
                .loss(0.250 - i * 0.01)
                .participants(Arrays.asList("vm1", "vm2"))
                .build());
        }

        List<TaskParticipant> participants = getTaskParticipants(taskId);
        List<TaskResultVO.ParticipantResultVO> participantResults = participants.stream()
            .map(p -> TaskResultVO.ParticipantResultVO.builder()
                .vmId(p.getVmId())
                .finalAccuracy(p.getFinalAccuracy())
                .finalLoss(p.getFinalLoss())
                .trainingTime(p.getTrainingTime())
                .dataSize(p.getDataSize())
                .parameters(TaskResultVO.ParametersVO.builder()
                    .artifact(TaskResultVO.ArtifactVO.builder()
                        .format("pickle")
                        .checksum("sha256:...")
                        .build())
                    .build())
                .build())
            .collect(Collectors.toList());

        TaskResultVO.ModelInfoVO modelInfo = TaskResultVO.ModelInfoVO.builder()
            .parameters(TaskResultVO.ParametersVO.builder()
                .artifact(TaskResultVO.ArtifactVO.builder()
                    .format("pickle")
                    .checksum("sha256:...")
                    .build())
                .build())
            .meta(TaskResultVO.MetaVO.builder()
                .version("1.0.0")
                .modelType(task.getModelType())
                .build())
            .build();

        return TaskResultVO.builder()
            .taskId(taskId)
            .taskName(task.getTaskName())
            .status(task.getStatus())
            .finalResults(finalResults)
            .roundResults(roundResults)
            .participantResults(participantResults)
            .modelInfo(modelInfo)
            .build();
    }

    // ========== v1.3 图形化配置接口实现 ==========

    @Override
    public ConfigPreviewVO.AvailableVmsVO getAvailableVms(String algorithm, Integer minCpuCores,
                                                         Integer minMemoryMb, String status, String capabilities) {
        log.info("查询可用虚拟机列表: algorithm={}, minCpu={}, minMemory={}", algorithm, minCpuCores, minMemoryMb);

        // 模拟虚拟机数据，实际应该从数据库查询并过滤
        List<ConfigPreviewVO.AvailableVmsVO.VmInfoVO> vms = new ArrayList<>();

        // VM 1
        vms.add(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.builder()
            .vmId("a1b2c3d4e5f678901234567890123456")
            .name("水声联邦学习节点-001")
            .ipAddress("192.168.1.100")
            .status("RUNNING")
            .connectionStatus("CONNECTED")
            .osType("Ubuntu 20.04")
            .resources(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.ResourcesVO.builder()
                .cpuCores(8)
                .memoryMb(16384)
                .diskGb(500)
                .gpuCount(1)
                .gpuMemoryMb(16384)
                .build())
            .capabilities(Arrays.asList("GPU", "HIGH_MEMORY", "FAST_NETWORK"))
            .supportedAlgorithms(Arrays.asList("FEDERATED_AVERAGING", "FEDPROX", "FEDNOVA", "SCAFFOLD"))
            .currentUsage(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.UsageVO.builder()
                .cpuUsage(25.5)
                .memoryUsage(45.2)
                .networkUsage(15.8)
                .build())
            .networkInfo(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.NetworkInfoVO.builder()
                .bandwidth(1000)
                .latency(10)
                .uploadSpeed(500)
                .downloadSpeed(800)
                .build())
            .lastHeartbeat("2024-01-01T12:30:00.000Z")
            .reliability(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.ReliabilityVO.builder()
                .uptime(99.8)
                .avgResponseTime(150)
                .taskSuccessRate(98.5)
                .build())
            .build());

        // VM 2
        vms.add(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.builder()
            .vmId("b2c3d4e5f67890123456789012345678")
            .name("水声联邦学习节点-002")
            .ipAddress("192.168.1.101")
            .status("RUNNING")
            .connectionStatus("CONNECTED")
            .osType("Ubuntu 20.04")
            .resources(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.ResourcesVO.builder()
                .cpuCores(6)
                .memoryMb(12288)
                .diskGb(300)
                .gpuCount(0)
                .gpuMemoryMb(0)
                .build())
            .capabilities(Arrays.asList("HIGH_MEMORY", "FAST_NETWORK"))
            .supportedAlgorithms(Arrays.asList("FEDERATED_AVERAGING", "FEDPROX", "FEDNOVA"))
            .currentUsage(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.UsageVO.builder()
                .cpuUsage(35.2)
                .memoryUsage(55.1)
                .networkUsage(20.3)
                .build())
            .networkInfo(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.NetworkInfoVO.builder()
                .bandwidth(1000)
                .latency(12)
                .uploadSpeed(480)
                .downloadSpeed(750)
                .build())
            .lastHeartbeat("2024-01-01T12:29:00.000Z")
            .reliability(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.ReliabilityVO.builder()
                .uptime(99.5)
                .avgResponseTime(160)
                .taskSuccessRate(97.8)
                .build())
            .build());

        // 应用过滤条件
        List<ConfigPreviewVO.AvailableVmsVO.VmInfoVO> filteredVms = vms.stream()
            .filter(vm -> minCpuCores == null || vm.getResources().getCpuCores() >= minCpuCores)
            .filter(vm -> minMemoryMb == null || vm.getResources().getMemoryMb() >= minMemoryMb)
            .filter(vm -> algorithm == null || vm.getSupportedAlgorithms().contains(algorithm))
            .filter(vm -> status == null || vm.getStatus().equals(status))
            .filter(vm -> {
                if (capabilities == null || capabilities.isEmpty()) return true;
                String[] requiredCaps = capabilities.split(",");
                return Arrays.stream(requiredCaps)
                    .allMatch(cap -> vm.getCapabilities().contains(cap.trim()));
            })
            .collect(Collectors.toList());

        return ConfigPreviewVO.AvailableVmsVO.builder()
            .total(filteredVms.size())
            .availableVms(filteredVms)
            .build();
    }

    @Override
    public ConfigPreviewVO.AvailableDatasetsVO getAvailableDatasets(String dataType, String status,
                                                                   Long minSize, Long maxSize, String keyword) {
        log.info("查询可用数据集列表: dataType={}, status={}, keyword={}", dataType, status, keyword);

        // 模拟数据集数据，实际应该从数据库查询
        List<ConfigPreviewVO.AvailableDatasetsVO.DatasetInfoVO> datasets = new ArrayList<>();

        datasets.add(ConfigPreviewVO.AvailableDatasetsVO.DatasetInfoVO.builder()
            .datasetId("e5f67890123456789012345678901234")
            .name("水声传播特征数据集_v1.0")
            .description("基于BELLHOP仿真的水声传播特征数据")
            .dataType("ACOUSTIC")
            .status("READY")
            .statistics(ConfigPreviewVO.AvailableDatasetsVO.DatasetInfoVO.StatisticsVO.builder()
                .totalRows(10000)
                .totalColumns(128)
                .fileSize(45678123L)
                .fileSizeFormatted("43.5MB")
                .build())
            .features(ConfigPreviewVO.AvailableDatasetsVO.DatasetInfoVO.FeaturesVO.builder()
                .featureColumns(Arrays.asList("frequency", "amplitude", "phase", "depth"))
                .targetColumn("propagation_loss")
                .numericFeatures(120)
                .categoricalFeatures(8)
                .build())
            .quality(ConfigPreviewVO.AvailableDatasetsVO.DatasetInfoVO.QualityVO.builder()
                .completeness(99.2)
                .consistency(97.8)
                .accuracy(98.5)
                .missingValues(80)
                .duplicates(15)
                .outliers(156)
                .build())
            .metadata(ConfigPreviewVO.AvailableDatasetsVO.DatasetInfoVO.MetadataVO.builder()
                .source("BELLHOP")
                .version("1.0")
                .sampleRate(48000)
                .frequency("1kHz-10kHz")
                .environment("shallow_water")
                .build())
            .tags(Arrays.asList("acoustic", "feature", "bellhop", "simulation"))
            .uploadTime("2024-01-01T10:00:00.000Z")
            .uploadedBy("f6789012345678901234567890123456")
            .build());

        return ConfigPreviewVO.AvailableDatasetsVO.builder()
            .total(datasets.size())
            .availableDatasets(datasets)
            .build();
    }

    @Override
    public ConfigPreviewVO.RoleConfigVO getRoleConfig() {
        log.info("查询角色配置选项");

        List<ConfigPreviewVO.RoleConfigVO.RoleInfoVO> roles = Arrays.asList(
            ConfigPreviewVO.RoleConfigVO.RoleInfoVO.builder()
                .role("PARTICIPANT")
                .name("参与者")
                .description("参与联邦学习训练的客户端节点")
                .requirements(ConfigPreviewVO.RoleConfigVO.RoleInfoVO.RequirementsVO.builder()
                    .minCpuCores(2)
                    .minMemoryMb(4096)
                    .requiredCapabilities(Arrays.asList("TRAINING"))
                    .build())
                .compatibleAlgorithms(Arrays.asList("FEDERATED_AVERAGING", "FEDPROX", "FEDNOVA", "SCAFFOLD"))
                .build(),
            ConfigPreviewVO.RoleConfigVO.RoleInfoVO.builder()
                .role("AGGREGATOR")
                .name("聚合器")
                .description("负责模型聚合的服务端节点")
                .requirements(ConfigPreviewVO.RoleConfigVO.RoleInfoVO.RequirementsVO.builder()
                    .minCpuCores(4)
                    .minMemoryMb(8192)
                    .requiredCapabilities(Arrays.asList("AGGREGATION"))
                    .build())
                .compatibleAlgorithms(Arrays.asList("FEDERATED_AVERAGING", "FEDPROX", "FEDNOVA", "SCAFFOLD"))
                .build()
        );

        return ConfigPreviewVO.RoleConfigVO.builder()
            .roles(roles)
            .build();
    }

    @Override
    public ConfigPreviewVO.AlgorithmTemplatesVO getAlgorithmTemplates() {
        log.info("查询算法配置模板");

        Map<String, Object> fedAvgDefaults = new HashMap<>();
        fedAvgDefaults.put("learningRate", 0.01);
        fedAvgDefaults.put("batchSize", 32);
        fedAvgDefaults.put("epochs", 100);
        fedAvgDefaults.put("rounds", 10);
        fedAvgDefaults.put("minParticipants", 2);
        fedAvgDefaults.put("aggregationMethod", "WEIGHTED_AVERAGE");

        Map<String, ConfigPreviewVO.AlgorithmTemplatesVO.AlgorithmTemplateVO.ParameterRangeVO> ranges = new HashMap<>();
        ranges.put("learningRate", ConfigPreviewVO.AlgorithmTemplatesVO.AlgorithmTemplateVO.ParameterRangeVO.builder()
            .min(0.0001)
            .max(1.0)
            .recommended(Arrays.asList(0.001, 0.01, 0.1))
            .build());
        ranges.put("batchSize", ConfigPreviewVO.AlgorithmTemplatesVO.AlgorithmTemplateVO.ParameterRangeVO.builder()
            .min(1.0)
            .max(1024.0)
            .recommended(Arrays.asList(16, 32, 64, 128))
            .build());

        List<ConfigPreviewVO.AlgorithmTemplatesVO.AlgorithmTemplateVO> templates = Arrays.asList(
            ConfigPreviewVO.AlgorithmTemplatesVO.AlgorithmTemplateVO.builder()
                .algorithm("FEDERATED_AVERAGING")
                .name("联邦平均算法")
                .description("经典的联邦学习算法，适用于IID数据分布")
                .applicableTaskTypes(Arrays.asList("CLASSIFICATION", "REGRESSION"))
                .defaultHyperparameters(fedAvgDefaults)
                .parameterRanges(ranges)
                .build()
        );

        return ConfigPreviewVO.AlgorithmTemplatesVO.builder()
            .templates(templates)
            .build();
    }

    @Override
    public ConfigPreviewVO.DistributionPreviewVO previewDistribution(
            FederatedTaskController.DistributionPreviewRequestDTO requestDTO) {
        log.info("数据分配预览: datasetId={}, strategy={}",
            requestDTO.getDatasetId(), requestDTO.getDistributionStrategy());

        // 模拟数据分配计算，实际应该根据真实数据进行分配
        List<ConfigPreviewVO.DistributionPreviewVO.DistributionResultVO.ParticipantAllocationVO> allocations = new ArrayList<>();

        double totalRatio = requestDTO.getParticipants().stream()
            .mapToDouble(FederatedTaskController.DistributionPreviewRequestDTO.ParticipantRequestDTO::getRequestedRatio)
            .sum();

        int totalRows = 10000; // 模拟数据集行数
        int participantIndex = 1;

        for (FederatedTaskController.DistributionPreviewRequestDTO.ParticipantRequestDTO participant : requestDTO.getParticipants()) {
            double normalizedRatio = participant.getRequestedRatio() / totalRatio;
            int allocatedRows = (int) (totalRows * normalizedRatio);
            int estimatedTime = (int) (allocatedRows * 0.08 + Math.random() * 100); // 模拟训练时间

            allocations.add(ConfigPreviewVO.DistributionPreviewVO.DistributionResultVO.ParticipantAllocationVO.builder()
                .vmId(participant.getVmId())
                .vmName("水声联邦学习节点-" + String.format("%03d", participantIndex++))
                .allocatedRatio(normalizedRatio)
                .allocatedRows(allocatedRows)
                .estimatedTrainingTime(estimatedTime)
                .build());
        }

        ConfigPreviewVO.DistributionPreviewVO.DistributionResultVO distributionResult =
            ConfigPreviewVO.DistributionPreviewVO.DistributionResultVO.builder()
                .participants(allocations)
                .build();

        ConfigPreviewVO.DistributionPreviewVO.QualityMetricsVO qualityMetrics =
            ConfigPreviewVO.DistributionPreviewVO.QualityMetricsVO.builder()
                .iidScore(0.85 + Math.random() * 0.1) // 模拟IID分数
                .balanceScore(0.90 + Math.random() * 0.08) // 模拟平衡分数
                .build();

        return ConfigPreviewVO.DistributionPreviewVO.builder()
            .distributionResult(distributionResult)
            .qualityMetrics(qualityMetrics)
            .build();
    }

    @Override
    public ConfigPreviewVO.ParticipantValidationVO validateParticipants(
            FederatedTaskController.ParticipantValidationRequestDTO requestDTO) {
        log.info("参与者验证: algorithm={}, taskType={}, participantCount={}",
            requestDTO.getAlgorithm(), requestDTO.getTaskType(), requestDTO.getParticipants().size());

        List<ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO> validations = new ArrayList<>();
        boolean overallValid = true;

        for (FederatedTaskController.ParticipantValidationRequestDTO.ParticipantForValidationDTO participant :
             requestDTO.getParticipants()) {

            Map<String, ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO.ValidationItemVO> results = new HashMap<>();

            // 模拟验证结果
            results.put("connectivity", ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO.ValidationItemVO.builder()
                .status("PASS")
                .message("网络连接正常")
                .build());

            results.put("resources", ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO.ValidationItemVO.builder()
                .status("PASS")
                .message("资源满足要求")
                .build());

            results.put("algorithm_support", ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO.ValidationItemVO.builder()
                .status("PASS")
                .message("支持指定算法")
                .build());

            boolean isParticipantValid = results.values().stream()
                .allMatch(result -> "PASS".equals(result.getStatus()));

            validations.add(ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO.builder()
                .vmId(participant.getVmId())
                .isValid(isParticipantValid)
                .validationResults(results)
                .build());

            if (!isParticipantValid) {
                overallValid = false;
            }
        }

        return ConfigPreviewVO.ParticipantValidationVO.builder()
            .overallValid(overallValid)
            .participantValidations(validations)
            .build();
    }

    @Override
    public TaskConfigStatusVO getTaskConfigStatus(String taskId) {
        log.info("查询任务配置状态: taskId={}", taskId);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        List<TaskConfigStatusVO.ConfigurationStepVO> steps = Arrays.asList(
            TaskConfigStatusVO.ConfigurationStepVO.builder()
                .step("DATASET_DISTRIBUTION")
                .status("COMPLETED")
                .completedAt(LocalDateTime.now().minusMinutes(30))
                .build(),
            TaskConfigStatusVO.ConfigurationStepVO.builder()
                .step("PARTICIPANT_VALIDATION")
                .status("COMPLETED")
                .completedAt(LocalDateTime.now().minusMinutes(25))
                .build(),
            TaskConfigStatusVO.ConfigurationStepVO.builder()
                .step("MODEL_INITIALIZATION")
                .status(task.getStatus().equals("RUNNING") ? "COMPLETED" : "PENDING")
                .completedAt(task.getStatus().equals("RUNNING") ? LocalDateTime.now().minusMinutes(20) : null)
                .build()
        );

        List<TaskParticipant> participants = getTaskParticipants(taskId);
        List<TaskConfigStatusVO.ParticipantStatusVO> participantStatuses = participants.stream()
            .map(p -> TaskConfigStatusVO.ParticipantStatusVO.builder()
                .vmId(p.getVmId())
                .configStatus("READY")
                .dataDistributed(true)
                .modelInitialized(task.getStatus().equals("RUNNING"))
                .build())
            .collect(Collectors.toList());

        return TaskConfigStatusVO.builder()
            .taskId(taskId)
            .configStatus("READY")
            .configurationSteps(steps)
            .participantStatuses(participantStatuses)
            .build();
    }

    @Override
    public TaskResourceUsageVO getTaskResourceUsage(String taskId) {
        log.info("查询任务资源使用: taskId={}", taskId);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        List<TaskParticipant> participants = getTaskParticipants(taskId);
        List<TaskResourceUsageVO.ParticipantMetricVO> participantMetrics = participants.stream()
            .map(p -> TaskResourceUsageVO.ParticipantMetricVO.builder()
                .vmId(p.getVmId())
                .currentUsage(TaskResourceUsageVO.ParticipantMetricVO.CurrentUsageVO.builder()
                    .cpu(75.5 + Math.random() * 10)
                    .memory(68.2 + Math.random() * 15)
                    .network(TaskResourceUsageVO.ParticipantMetricVO.CurrentUsageVO.NetworkUsageVO.builder()
                        .inbound(15.6 + Math.random() * 5)
                        .outbound(12.3 + Math.random() * 3)
                        .build())
                    .build())
                .averageUsage(TaskResourceUsageVO.ParticipantMetricVO.AverageUsageVO.builder()
                    .cpu(72.1 + Math.random() * 8)
                    .memory(65.5 + Math.random() * 12)
                    .build())
                .build())
            .collect(Collectors.toList());

        TaskResourceUsageVO.AggregatedMetricVO aggregatedMetrics = TaskResourceUsageVO.AggregatedMetricVO.builder()
            .totalCpuUsage(73.8)
            .totalMemoryUsage(66.8)
            .taskProgress(task.getProgress())
            .build();

        return TaskResourceUsageVO.builder()
            .taskId(taskId)
            .participantMetrics(participantMetrics)
            .aggregatedMetrics(aggregatedMetrics)
            .build();
    }

    @Override
    @Transactional
    public TaskOperationVO createSmartTask(TaskCreateDTO createDTO, String createdBy) {
        log.info("开始创建智能联邦学习任务: taskName={}, format=v1.3, createdBy={}",
            createDTO.getTaskName(), createdBy);

        // 验证v1.3新格式配置
        if (createDTO.getDatasetConfig() == null) {
            throw new UserException("数据集配置不能为空");
        }
        if (createDTO.getParticipantConfig() == null) {
            throw new UserException("参与者配置不能为空");
        }

        // 检查虚拟机是否被占用（一台VM只能绑定一个任务）
        for (TaskCreateDTO.ParticipantConfigDTO.SmartParticipantDTO participant :
             createDTO.getParticipantConfig().getParticipants()) {
            if (isVmOccupied(participant.getVmId())) {
                throw new UserException("虚拟机 " + participant.getVmId() + " 已被其他任务占用");
            }
        }

        // 生成任务ID
        String taskId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        // 构建v1.3任务实体
        FederatedTask task = buildSmartTaskFromCreateDTO(createDTO, taskId, createdBy, now);

        // 插入任务记录
        int result = tasksMapper.insertTask(task);
        if (result <= 0) {
            throw new UserException("任务创建失败");
        }

        // 创建v1.3参与者记录
        for (TaskCreateDTO.ParticipantConfigDTO.SmartParticipantDTO participantDTO :
             createDTO.getParticipantConfig().getParticipants()) {
            TaskParticipant participant = buildSmartParticipantFromDTO(participantDTO, taskId, now);
            tasksMapper.insertParticipant(participant);
        }

        // 记录操作日志
        logTask(taskId, "INFO", "v1.3智能任务创建成功", "TASK_MANAGER", null,
            Map.of("datasetId", createDTO.getDatasetConfig().getDatasetId(),
                   "participants", createDTO.getParticipantConfig().getParticipants().size()));

        // 构建v1.3增强响应
        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .taskName(createDTO.getTaskName())
            .status(STATUS_CREATED)
            .createdAt(now)
            .createdBy(createdBy)
            .participantCount(createDTO.getParticipantConfig().getParticipants().size())
            .estimatedDuration(estimateSmartTaskDuration(createDTO))
            .build();

        // 发布任务创建事件
        eventPublisher.publishEvent(new FederatedTaskCreatedEvent(taskId, createDTO.getTaskName(), "v1.3"));

        log.info("智能任务创建成功: taskId={}, participantCount={}",
            taskId, createDTO.getParticipantConfig().getParticipants().size());

        return response;
    }

    // ========== v1.3 私有辅助方法 ==========

    /**
     * 检查虚拟机是否被占用
     */
    private boolean isVmOccupied(String vmId) {
        // 查询正在运行或配置中的任务是否使用了该VM
        List<String> occupyingStatuses = Arrays.asList("RUNNING", "CONFIGURED", "PAUSED");
        return tasksMapper.countTasksByVmIdAndStatuses(vmId, occupyingStatuses) > 0;
    }

    /**
     * 构建v1.3智能任务实体
     */
    private FederatedTask buildSmartTaskFromCreateDTO(TaskCreateDTO createDTO, String taskId, String createdBy, LocalDateTime now) {
        FederatedTask task = new FederatedTask();
        task.setId(taskId);
        task.setTaskName(createDTO.getTaskName());
        task.setTaskType(createDTO.getTaskType());
        task.setDescription(createDTO.getDescription());
        task.setAlgorithm(createDTO.getAlgorithm());
        task.setStatus(STATUS_CREATED);
        task.setCreatedBy(createdBy);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setParticipantCount(createDTO.getParticipantConfig().getParticipants().size());
        task.setCurrentRound(0);
        task.setTotalRounds(createDTO.getHyperparameters() != null ? createDTO.getHyperparameters().getRounds() : 10);
        task.setProgress(0.0);

        // 设置v1.3特有字段
        task.setDatasetId(createDTO.getDatasetConfig().getDatasetId());
        task.setDistributionStrategy(createDTO.getDatasetConfig().getDistributionStrategy());

        // 生成配置JSON，包含v1.3新结构
        try {
            task.setConfig(objectMapper.writeValueAsString(createDTO));
        } catch (JsonProcessingException e) {
            throw new UserException("任务配置序列化失败");
        }

        return task;
    }

    /**
     * 构建v1.3智能参与者实体
     */
    private TaskParticipant buildSmartParticipantFromDTO(TaskCreateDTO.ParticipantConfigDTO.SmartParticipantDTO participantDTO,
                                                        String taskId, LocalDateTime now) {
        TaskParticipant participant = new TaskParticipant();
        participant.setParticipantId(UUID.randomUUID().toString().replace("-", ""));
        participant.setTaskId(taskId);
        participant.setVmId(participantDTO.getVmId());
        participant.setRole(participantDTO.getRole());
        participant.setStatus("CREATED");
        participant.setDataRatio(participantDTO.getDataRatio());
        participant.setCreatedAt(now);
        participant.setUpdatedAt(now);

        // 设置v1.3特有字段
        if (participantDTO.getCapabilities() != null) {
            participant.setCapabilities(String.join(",", participantDTO.getCapabilities()));
        }
        if (participantDTO.getConstraints() != null) {
            participant.setMaxCpuUsage(participantDTO.getConstraints().getMaxCpuUsage());
            participant.setMaxMemoryUsage(participantDTO.getConstraints().getMaxMemoryUsage());
        }

        return participant;
    }

    /**
     * 估算智能任务执行时间
     */
    private int estimateSmartTaskDuration(TaskCreateDTO createDTO) {
        // 根据数据集大小、参与者数量和算法复杂度估算时间
        int baseTime = 3600; // 1小时基础时间
        int participantCount = createDTO.getParticipantConfig().getParticipants().size();
        int rounds = createDTO.getHyperparameters() != null ? createDTO.getHyperparameters().getRounds() : 10;

        // 复杂算法增加时间
        double algorithmMultiplier = 1.0;
        if ("FEDPROX".equals(createDTO.getAlgorithm()) || "SCAFFOLD".equals(createDTO.getAlgorithm())) {
            algorithmMultiplier = 1.3;
        }

        return (int) (baseTime * participantCount * rounds * algorithmMultiplier / 10);
    }

}