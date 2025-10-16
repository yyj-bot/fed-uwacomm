package com.feduwacomm.service.impl;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.controller.FederatedTaskController;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.ModelDistribution;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.entity.VmAckTracking;
import com.feduwacomm.enums.*;
import com.feduwacomm.event.FederatedTaskCreatedEvent;
import com.feduwacomm.event.InitialModelDistributionStartedEvent;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.service.DataDistributionService;
import com.feduwacomm.service.FederatedTaskService;
import com.feduwacomm.service.LogService;
import com.feduwacomm.service.TrainingDataService;
import com.feduwacomm.service.WebSocketProtocolService;
import com.feduwacomm.service.FederatedOrchestrationService;
import com.feduwacomm.service.InitialModelGenerationService;
import com.feduwacomm.utils.MessageBuilder;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.*;
import com.feduwacomm.model.dto.federated.FederatedTaskConfigDTO;
import com.feduwacomm.model.dto.federated.FederatedTaskCreateRequest;
import com.feduwacomm.model.dto.federated.InitialModelConfigDTO;
import com.feduwacomm.model.dto.initial.InitialModelDistributeRequest;
import com.feduwacomm.model.dto.initial.InitialModelGenerateRequest;
import com.feduwacomm.model.vo.initial.InitialModelDetailVO;
import com.feduwacomm.model.vo.initial.InitialModelBindingVO;
import com.feduwacomm.service.cache.MetricsCacheService;
import com.feduwacomm.service.cache.model.GlobalMetrics;
import com.feduwacomm.service.cache.exception.CacheValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    @Autowired
    private com.feduwacomm.service.VmInstanceService vmInstanceService;

    @Autowired
    private UuidUtil uuidUtil;

    @Autowired
    private MetricsCacheService metricsCacheService;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    // v1.5协议新增组件
    @Autowired
    private com.feduwacomm.service.RoundStateManager roundStateManager;

    @Autowired
    private com.feduwacomm.service.VmAckTracker vmAckTracker;

    @Autowired
    private com.feduwacomm.service.RoundLockManager roundLockManager;

    // v1.5协议新增组件
    @Autowired
    private DataDistributionService dataDistributionService;

    @Autowired
    private WebSocketProtocolService webSocketProtocolService;

    @Autowired
    private TrainingDataService trainingDataService;

    @Autowired
    private com.feduwacomm.mapper.TaskParticipantsMapper taskParticipantsMapper;

    @Autowired
    private FederatedOrchestrationService orchestrationService;

    @Autowired
    private InitialModelGenerationService initialModelGenerationService;

    @Autowired
    private ModelDistributionMapper modelDistributionMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // 任务状态常量
    private static final FederatedTaskStatus STATUS_CREATED = FederatedTaskStatus.CREATED;
    private static final FederatedTaskStatus STATUS_CONFIGURED = FederatedTaskStatus.CONFIGURED;
    private static final FederatedTaskStatus STATUS_PENDING = FederatedTaskStatus.PENDING;
    private static final FederatedTaskStatus STATUS_READY = FederatedTaskStatus.READY;
    private static final FederatedTaskStatus STATUS_RUNNING = FederatedTaskStatus.RUNNING;
    private static final FederatedTaskStatus STATUS_PAUSED = FederatedTaskStatus.PAUSED;
    private static final FederatedTaskStatus STATUS_STOPPED = FederatedTaskStatus.STOPPED;
    private static final FederatedTaskStatus STATUS_COMPLETED = FederatedTaskStatus.COMPLETED;
    private static final FederatedTaskStatus STATUS_FAILED = FederatedTaskStatus.FAILED;
    private static final FederatedTaskStatus STATUS_CANCELLED = FederatedTaskStatus.CANCELLED;
    @org.springframework.beans.factory.annotation.Value("${federated.datasetAck.timeoutSeconds:120}")
    private int datasetAckTimeoutSeconds;

    @Override
    public TaskOperationVO createTask(FederatedTaskCreateRequest createDTO, String createdBy) {
        log.info("开始创建联邦学习任务: taskName={}, algorithm={}, createdBy={}", 
            createDTO.getTaskName(), createDTO.getAlgorithm(), createdBy);

        // 验证任务配置
        if (!isValidTaskConfig(createDTO)) {
            throw new UserException("任务配置无效");
        }
        if (createDTO.getInitialModelConfig() == null) {
            throw new UserException("初始模型配置不能为空");
        }

        TransactionTemplate creationTemplate = new TransactionTemplate(transactionManager);
        creationTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        TaskCreationResult creationResult = creationTemplate.execute(status ->
            createTaskTransactional(createDTO, createdBy)
        );

        if (creationResult == null || creationResult.getTask() == null) {
            throw new UserException("任务创建失败");
        }

        TaskResourcePreparationResult preparationResult;
        try {
            preparationResult = prepareTaskResources(creationResult.getTask(), createdBy);
        } catch (Exception ex) {
            log.error("任务资源准备失败: taskId={}, error={}", creationResult.getTask().getId(), ex.getMessage(), ex);
            preparationResult = TaskResourcePreparationResult.failed(
                    creationResult.getTask().getDatasetId(),
                    creationResult.getTargetVmIds(),
                    "资源准备过程中发生异常: " + ex.getMessage());
        }

        FederatedTask refreshedTask = tasksMapper.selectTaskById(creationResult.getTask().getId());
        FederatedTaskStatus latestStatus = refreshedTask != null
                ? refreshedTask.getStatus()
                : creationResult.getTask().getStatus();

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(creationResult.getTask().getId())
            .taskName(creationResult.getTask().getTaskName())
            .status(latestStatus != null ? latestStatus.getCode() : STATUS_CREATED.getCode())
            .createdAt(creationResult.getCreatedAt())
            .createdBy(createdBy)
            .participantCount(creationResult.getParticipantCount())
            .estimatedDuration(creationResult.getEstimatedDuration())
            .configSummary(creationResult.getConfigSummary())
            .build();

        if (preparationResult != null) {
            boolean triggered = preparationResult.isDatasetDistributionTriggered()
                    || preparationResult.isModelDistributionTriggered();
            response.setResourcePreparation(preparationResult.toSummary());
            if (preparationResult.isReady()) {
                response.setStatus(STATUS_PENDING.getCode());
                response.setMessage(StringUtils.hasText(preparationResult.getMessage())
                        ? "任务创建成功，" + preparationResult.getMessage()
                        : "任务创建成功，资源已准备就绪");
            } else if (preparationResult.hasError()) {
                response.setMessage("任务创建成功，但资源准备失败: " + preparationResult.getErrorMessage());
            } else if (triggered) {
                response.setStatus(STATUS_PENDING.getCode());
                if (StringUtils.hasText(preparationResult.getMessage())) {
                    response.setMessage("任务创建成功，" + preparationResult.getMessage());
                } else {
                    response.setMessage("任务创建成功，资源准备进行中");
                }
            } else {
                response.setMessage("任务创建成功，等待资源准备");
            }
        } else {
            response.setMessage("任务创建成功，等待资源准备");
        }

        log.info("任务创建响应: taskId={}, status={}, message={}, resourcePrep={}",
                response.getTaskId(), response.getStatus(), response.getMessage(), response.getResourcePreparation());

        response.setTimestamp(LocalDateTime.now());
        log.info("任务创建响应详情: taskId={}, status={}, initialModelSummary={}, resourcePrep={}",
                response.getTaskId(),
                response.getStatus(),
                response.getConfigSummary() != null ? response.getConfigSummary().getInitialModel() : null,
                response.getResourcePreparation());

        log.info("联邦学习任务创建完成: taskId={}, participantCount={}, status={}, resourceSummary={}",
            creationResult.getTask().getId(), creationResult.getParticipantCount(),
            response.getStatus(), response.getResourcePreparation());

        return response;
    }

    private TaskCreationResult createTaskTransactional(FederatedTaskCreateRequest createDTO, String createdBy) {
        LocalDateTime now = LocalDateTime.now();
        String taskId = uuidUtil.generateUuid();

        FederatedTask task = buildTaskFromCreateDTO(createDTO, taskId, createdBy, now);
        task.setStatus(STATUS_CREATED);
        task.setUpdatedAt(now);

        int inserted = tasksMapper.insertTask(task);
        if (inserted <= 0) {
            throw new UserException("任务创建失败");
        }

        List<FederatedTaskCreateRequest.ParticipantConfigDTO.SmartParticipantDTO> participants = getSmartParticipants(createDTO);
        List<String> targetVmIds = new ArrayList<>();
        for (FederatedTaskCreateRequest.ParticipantConfigDTO.SmartParticipantDTO participantDTO : participants) {
            TaskParticipant participant = buildSmartParticipantFromDTO(participantDTO, taskId, now);
            tasksMapper.insertParticipant(participant);
            if (StringUtils.hasText(participantDTO.getVmId())) {
                targetVmIds.add(participantDTO.getVmId());
            }
        }

        InitialModelBindingVO binding = bindInitialModelForTask(taskId, createDTO.getInitialModelConfig(), createdBy);

        logTask(taskId, "INFO", "任务创建成功", "TASK_MANAGER", null,
            Map.of("participantCount", participants.size()));

        return new TaskCreationResult(
            task,
            now,
            participants.size(),
            estimateTaskDuration(createDTO),
            buildConfigSummary(createDTO.getInitialModelConfig(), binding),
            targetVmIds
        );
    }

    private TaskCreationResult createSmartTaskTransactional(FederatedTaskCreateRequest createDTO, String createdBy) {
        LocalDateTime now = LocalDateTime.now();
        String taskId = uuidUtil.generateUuid();

        FederatedTask task = buildSmartTaskFromCreateDTO(createDTO, taskId, createdBy, now);
        task.setStatus(STATUS_CREATED);
        task.setUpdatedAt(now);

        int inserted = tasksMapper.insertTask(task);
        if (inserted <= 0) {
            throw new UserException("任务创建失败");
        }

        List<FederatedTaskCreateRequest.ParticipantConfigDTO.SmartParticipantDTO> participants = getSmartParticipants(createDTO);
        List<String> targetVmIds = new ArrayList<>();
        for (FederatedTaskCreateRequest.ParticipantConfigDTO.SmartParticipantDTO participantDTO : participants) {
            TaskParticipant participant = buildSmartParticipantFromDTO(participantDTO, taskId, now);
            tasksMapper.insertParticipant(participant);
            if (StringUtils.hasText(participantDTO.getVmId())) {
                targetVmIds.add(participantDTO.getVmId());
            }
        }

        InitialModelBindingVO binding = bindInitialModelForTask(taskId, createDTO.getInitialModelConfig(), createdBy);

        logTask(taskId, "INFO", "任务创建成功", "TASK_MANAGER", null,
            Map.of("datasetId", createDTO.getDatasetConfig().getDatasetId(),
                   "participants", participants.size()));

        return new TaskCreationResult(
            task,
            now,
            participants.size(),
            estimateSmartTaskDuration(createDTO),
            buildConfigSummary(createDTO.getInitialModelConfig(), binding),
            targetVmIds
        );
    }

    @Override
    @Transactional
    public TaskOperationVO configureTask(String taskId, FederatedTaskConfigDTO configDTO, String updatedBy) {
        log.info("开始配置任务: taskId={}, updatedBy={}", taskId, updatedBy);

        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new UserException("任务不存在");
        }

        if (!STATUS_CREATED.equals(task.getStatus())
                && !STATUS_CONFIGURED.equals(task.getStatus())) {
            throw new UserException("任务状态不允许配置");
        }

        // 更新任务配置
        updateTaskFromConfigDTO(task, configDTO, updatedBy);
        task.setStatus(STATUS_CONFIGURED);
        task.setUpdatedAt(LocalDateTime.now());

        InitialModelBindingVO binding = null;
        if (configDTO.getInitialModelConfig() != null) {
            binding = bindInitialModelForTask(taskId, configDTO.getInitialModelConfig(), updatedBy);
        }

        int result = tasksMapper.updateTask(task);
        if (result <= 0) {
            throw new UserException("任务配置失败");
        }

        // 记录操作日志
        logTask(taskId, "INFO", "任务配置更新", "TASK_MANAGER", null, null);

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_CONFIGURED.getCode())
            .updatedAt(LocalDateTime.now())
            .configVersion("v1.1")
            .configSummary(buildConfigSummary(configDTO.getInitialModelConfig(), binding))
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

        if (STATUS_RUNNING.equals(task.getStatus())) {
            throw new UserException("任务已在运行中");
        }

        InitialModelBindingVO binding = initialModelGenerationService.getTaskBinding(taskId);

        TaskResourcePreparationResult preparationResult = null;
        boolean readyState = STATUS_READY.equals(task.getStatus()) || STATUS_PENDING.equals(task.getStatus());
        if (!readyState) {
            if (STATUS_CREATED.equals(task.getStatus())
                    || STATUS_CONFIGURED.equals(task.getStatus())) {
                log.info("任务状态为{}，尝试执行启动前资源准备: taskId={}",
                        task.getStatus(), taskId);
                preparationResult = prepareTaskResources(task, operatorId);
                if (!preparationResult.isReady()) {
                    String reason = preparationResult.hasError()
                            ? preparationResult.getErrorMessage()
                            : "任务资源尚未准备完成，无法启动";
                    throw new UserException(reason);
                }
                task = getTaskById(taskId);
            } else {
                throw new UserException("任务状态不允许启动，当前状态: " + task.getStatus());
            }
        } else {
            preparationResult = prepareTaskResources(task, operatorId);
            if (preparationResult != null && !preparationResult.isReady()) {
                String reason = preparationResult.hasError()
                        ? preparationResult.getErrorMessage()
                        : "任务资源尚未准备完成，无法启动";
                throw new UserException(reason);
            }
        }

        List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
        if (participants.isEmpty()) {
            throw new UserException("联邦学习任务没有可用参与者，无法启动");
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(STATUS_RUNNING);
        task.setStartedAt(now);
        task.setUpdatedAt(now);
        tasksMapper.updateTask(task);

        for (TaskParticipant participant : participants) {
            participant.setStatus(ParticipantStatus.CONNECTED);
            tasksMapper.updateParticipantStatus(taskId, participant.getVmId(),
                    ParticipantStatus.CONNECTED.getCode(), now);
        }

        logTask(taskId, "INFO", "任务启动成功", "TASK_MANAGER", null,
            Map.of("participants", participants.stream()
                    .map(TaskParticipant::getVmId)
                    .collect(Collectors.toList())));

        sendTrainingStartCommand(taskId, participants);

        List<TaskOperationVO.ParticipantStatus> participantStatuses = participants.stream()
            .map(p -> TaskOperationVO.ParticipantStatus.builder()
                .vmId(p.getVmId())
                .status(ParticipantStatus.CONNECTED.getCode())
                .dataSource(p.getDataSource())
                .build())
            .collect(Collectors.toList());

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_RUNNING.getCode())
            .startedAt(now)
            .currentRound(task.getCurrentRound())
            .participants(participantStatuses)
            .message("任务启动成功")
            .timestamp(now)
            .build();
        if (preparationResult != null) {
            response.setResourcePreparation(preparationResult.toSummary());
        }
        if (binding != null) {
            response.setConfigSummary(buildConfigSummaryFromTask(task, binding));
        }

        log.info("启动任务响应概要: taskId={}, resourcePreparation={}, configSummary={}",
                taskId, response.getResourcePreparation(), response.getConfigSummary());

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
        tasksMapper.updateTaskStatus(taskId, STATUS_PAUSED.getCode(), now);

        // 记录操作日志
        logTask(taskId, "INFO", "任务暂停成功", "TASK_MANAGER", null, null);

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_PAUSED.getCode())
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
        tasksMapper.updateTaskStatus(taskId, STATUS_RUNNING.getCode(), now);

        // 记录操作日志
        logTask(taskId, "INFO", "任务恢复成功", "TASK_MANAGER", null, null);

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_RUNNING.getCode())
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
        tasksMapper.updateTaskStatus(taskId, STATUS_STOPPED.getCode(), now);

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
            .status(STATUS_STOPPED.getCode())
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
        tasksMapper.updateTaskStatus(taskId, STATUS_CANCELLED.getCode(), now);

        // 记录操作日志
        logTask(taskId, "INFO", "任务取消成功", "TASK_MANAGER", null, 
            Map.of("reason", cancelDTO.getReason()));

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .status(STATUS_CANCELLED.getCode())
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

        // 处理初始模型绑定
        InitialModelBindingVO binding = initialModelGenerationService.getTaskBinding(taskId);
        boolean modelDeleted = false;
        boolean modelUnbound = false;
        String boundModelId = null;
        if (binding != null && StringUtils.hasText(binding.getModelId())) {
            boundModelId = binding.getModelId();
            if (Boolean.TRUE.equals(deleteDTO.getDeleteModel())) {
                try {
                    modelDeleted = initialModelGenerationService.deleteModel(boundModelId, operatorId);
                    log.info("任务删除同步删除初始模型: taskId={}, modelId={}, deleted={}", taskId, boundModelId, modelDeleted);
                } catch (Exception e) {
                    log.error("删除初始模型失败: taskId={}, modelId={}, error={}", taskId, boundModelId, e.getMessage(), e);
                    throw new UserException("删除初始模型失败: " + e.getMessage());
                }
            } else {
                try {
                    initialModelGenerationService.unbindModelFromTask(boundModelId, taskId, operatorId);
                    modelUnbound = true;
                    log.info("任务删除解除初始模型绑定: taskId={}, modelId={}", taskId, boundModelId);
                } catch (Exception e) {
                    log.error("解除初始模型绑定失败: taskId={}, modelId={}, error={}", taskId, boundModelId, e.getMessage(), e);
                    throw new UserException("解除初始模型绑定失败: " + e.getMessage());
                }
            }
        }

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
        Map<String, Object> logDetails = new HashMap<>();
        logDetails.put("deleteData", deleteDTO.getDeleteData());
        logDetails.put("deleteModel", deleteDTO.getDeleteModel());
        logDetails.put("modelDeleted", modelDeleted);
        logDetails.put("modelUnbound", modelUnbound);
        logDetails.put("boundModelId", boundModelId);

        logTask(taskId, "INFO", "任务删除成功", "TASK_MANAGER", null, logDetails);

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(taskId)
            .deletedAt(now)
            .dataDeleted(deleteDTO.getDeleteData())
            .modelPreserved(!modelDeleted)
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
        List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
        List<TaskDetailVO.ParticipantVO> participantVOs = participants.stream()
            .map(this::convertToParticipantVO)
            .collect(Collectors.toList());

        // 构建指标信息
        TaskDetailVO.MetricsVO metrics = buildTaskMetrics(task, participants);

        // 初始模型摘要
        InitialModelBindingVO binding = initialModelGenerationService.getTaskBinding(taskId);
        TaskOperationVO.ConfigSummaryVO configSummary = buildConfigSummaryFromTask(task, binding);

        // 构建任务详情
        TaskDetailVO taskDetail = TaskDetailVO.builder()
            .taskId(task.getId())
            .taskName(task.getTaskName())
            .taskType(task.getTaskType())
            .description(task.getDescription())
            .status(task.getStatus().getCode())
            .algorithm(task.getAlgorithm().getCode())
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
            .configSummary(configSummary)
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
        return taskParticipantsMapper.selectParticipantsByTaskId(taskId);
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
        Map<String, Set<String>> validTransitions = new HashMap<>();
        validTransitions.put(STATUS_CREATED.getCode(), Set.of(STATUS_CONFIGURED.getCode(), STATUS_CANCELLED.getCode()));
        validTransitions.put(STATUS_PENDING.getCode(), Set.of(STATUS_RUNNING.getCode(), STATUS_CONFIGURED.getCode(), STATUS_CANCELLED.getCode()));
        validTransitions.put(STATUS_READY.getCode(), Set.of(STATUS_RUNNING.getCode(), STATUS_CANCELLED.getCode()));
        validTransitions.put(STATUS_CONFIGURED.getCode(), Set.of(STATUS_RUNNING.getCode(), STATUS_CANCELLED.getCode()));
        validTransitions.put(STATUS_RUNNING.getCode(), Set.of(STATUS_PAUSED.getCode(), STATUS_STOPPED.getCode(), STATUS_COMPLETED.getCode(), STATUS_FAILED.getCode()));
        validTransitions.put(STATUS_PAUSED.getCode(), Set.of(STATUS_RUNNING.getCode(), STATUS_STOPPED.getCode(), STATUS_CANCELLED.getCode()));
        validTransitions.put(STATUS_STOPPED.getCode(), Collections.emptySet());
        validTransitions.put(STATUS_COMPLETED.getCode(), Collections.emptySet());
        validTransitions.put(STATUS_FAILED.getCode(), Collections.emptySet());
        validTransitions.put(STATUS_CANCELLED.getCode(), Collections.emptySet());

        return validTransitions.getOrDefault(currentStatus, Collections.emptySet()).contains(targetStatus);
    }

    @Override
    public boolean isValidTaskConfig(FederatedTaskCreateRequest createDTO) {
        if (createDTO.getTaskName() == null || createDTO.getTaskName().trim().isEmpty()) {
            return false;
        }
        if (createDTO.getDatasetConfig() == null
                || !StringUtils.hasText(createDTO.getDatasetConfig().getDatasetId())) {
            return false;
        }
        if (createDTO.getParticipantConfig() == null
                || createDTO.getParticipantConfig().getParticipants() == null
                || createDTO.getParticipantConfig().getParticipants().isEmpty()) {
            return false;
        }
        if (createDTO.getInitialModelConfig() == null || !StringUtils.hasText(createDTO.getInitialModelConfig().getMode())) {
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
    public String generateConfigJson(FederatedTaskCreateRequest createDTO) {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("hyperparameters", createDTO.getHyperparameters());
            config.put("initialModelConfig", createDTO.getInitialModelConfig());
            config.put("schedule", createDTO.getSchedule());
            config.put("securityConfig", createDTO.getSecurityConfig());
            config.put("datasetConfig", createDTO.getDatasetConfig());
            config.put("participantConfig", createDTO.getParticipantConfig());
            config.put("aggregationConfig", createDTO.getAggregationConfig());
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("生成配置JSON失败: {}", e.getMessage());
            return "{}";
        }
    }

    @Override
    public String generateConfigJson(FederatedTaskConfigDTO configDTO) {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("algorithm", configDTO.getAlgorithm());
            config.put("hyperparameters", configDTO.getHyperparameters());
            config.put("initialModelConfig", configDTO.getInitialModelConfig());
            config.put("dataConfig", configDTO.getDataConfig());
            config.put("securityConfig", configDTO.getSecurityConfig());
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("生成配置JSON失败: {}", e.getMessage());
            return "{}";
        }
    }

    @Override
    public int estimateTaskDuration(FederatedTaskCreateRequest createDTO) {
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

    /**
     * 向参与者发送训练启动指令
     */
    private void sendTrainingStartCommand(String taskId, List<TaskParticipant> participants) {
        if (participants == null || participants.isEmpty()) {
            log.warn("任务{}没有参与者，跳过发送训练启动指令", taskId);
            return;
        }

        // 获取任务信息以获取算法配置
        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            log.error("任务{}不存在，无法发送训练启动指令", taskId);
            return;
        }

        String algorithmCode = task.getAlgorithm() != null ? task.getAlgorithm().getCode() : "FEDERATED_AVERAGING";
        Map<String, Object> trainingPlan = buildTrainingPlan(task);
        InitialModelBindingVO binding = initialModelGenerationService.getTaskBinding(taskId);
        InitialModelDetailVO initialModelDetail = null;
        if (binding != null && StringUtils.hasText(binding.getModelId())) {
            try {
                initialModelDetail = initialModelGenerationService.getModelDetail(binding.getModelId(), true);
            } catch (Exception ex) {
                log.warn("获取初始模型详情失败: taskId={}, modelId={}, error={}", taskId, binding.getModelId(), ex.getMessage());
            }
        } else {
            log.warn("任务{} 未绑定初始模型，FEDERATED_TASK_START消息将缺少initialModel字段", taskId);
        }
        log.info("开始向{}个参与者发送训练启动指令: taskId={}, algorithm={}", participants.size(), taskId, algorithmCode);

        for (TaskParticipant participant : participants) {
            try {
                // v1.5.1协议：获取assignedDatasetId和dataPath（必需字段）
                String assignedDatasetId = participant.getAssignedDatasetId();
                String dataPath = participant.getLocalPath();

                // 如果assignedDatasetId为null，记录错误并跳过该参与者
                if (assignedDatasetId == null || assignedDatasetId.trim().isEmpty()) {
                    log.error("参与者缺少assignedDatasetId，跳过发送: vmId={}, taskId={}",
                        participant.getVmId(), taskId);
                    continue;
                }

                if (!"COMPLETED".equalsIgnoreCase(participant.getDatasetStatus())) {
                    log.error("参与者数据集状态未就绪，跳过发送: vmId={}, taskId={}, datasetStatus={}",
                            participant.getVmId(), taskId, participant.getDatasetStatus());
                    continue;
                }

                // 如果dataPath为null，生成默认路径
                if (dataPath == null || dataPath.trim().isEmpty()) {
                    dataPath = "/data/assigned/" + assignedDatasetId;
                    log.warn("参与者dataPath为空，使用默认路径: vmId={}, dataPath={}",
                        participant.getVmId(), dataPath);
                }

                // 使用MessageBuilder构建标准TRAINING_START消息，符合协议v1.5.1标准
                Map<String, Object> initialModelPayload = null;
                if (initialModelDetail != null) {
                    ModelDistribution distribution = ensureModelDistributionRecord(initialModelDetail.getModelId(), participant.getVmId());
                    initialModelPayload = buildInitialModelPayload(initialModelDetail, distribution.getId());
                }

                ProtocolMessage startMessage = MessageBuilder.buildTrainingStartMessage(
                    participant.getVmId(),
                    taskId,
                    1, // roundNumber
                    algorithmCode, // mlAlgorithm
                    MessageBuilder.buildHyperparameters(task), // hyperparameters对象
                    MessageBuilder.buildGlobalModel(taskId, 1), // globalModel对象
                    "请开始本地ML训练任务", // message
                    assignedDatasetId, // v1.5.1新增
                    dataPath, // v1.5.1新增
                    initialModelPayload,
                    trainingPlan
                );

                // 发送到VM专用topic
                String vmTopic = "/topic/vm/" + participant.getVmId();
                messagingTemplate.convertAndSend(vmTopic, startMessage);

                log.info("训练启动指令已发送: vmId={}, taskId={}, algorithm={}, assignedDatasetId={}, dataPath={}, topic={}",
                    participant.getVmId(), taskId, algorithmCode, assignedDatasetId, dataPath, vmTopic);

            } catch (Exception e) {
                log.error("发送训练启动指令失败: vmId={}, taskId={}, 错误={}",
                    participant.getVmId(), taskId, e.getMessage(), e);
            }
        }

        log.info("训练启动指令发送完成: taskId={}, algorithm={}, 成功发送给{}个参与者",
                taskId, algorithmCode, participants.size());
    }

    private boolean waitForDatasetAcknowledgments(String taskId) {
        return vmAckTracker.waitForAllAcknowledgments(
                taskId,
                VmAckTracking.AckType.DATASET_COMPLETE.name(),
                datasetAckTimeoutSeconds);
    }

    private boolean waitForModelDistributionAcknowledgments(String modelId,
                                                            List<String> targetVmIds,
                                                            int timeoutSeconds) {
        if (!StringUtils.hasText(modelId) || CollectionUtils.isEmpty(targetVmIds)) {
            return true;
        }

        Instant deadline = Instant.now().plusSeconds(Math.max(timeoutSeconds, 1));
        while (Instant.now().isBefore(deadline)) {
            List<String> pending = collectPendingModelVmIds(modelId, targetVmIds);
            if (pending.isEmpty()) {
                return true;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return collectPendingModelVmIds(modelId, targetVmIds).isEmpty();
    }

    private FederatedTask buildTaskFromCreateDTO(FederatedTaskCreateRequest createDTO, String taskId, String createdBy, LocalDateTime now) {
        FederatedTask task = new FederatedTask();
        task.setId(taskId);
        task.setTaskName(createDTO.getTaskName());
        task.setTaskType(createDTO.getTaskType());
        task.setDescription(createDTO.getDescription());
        task.setAlgorithm(FederatedAlgorithm.fromCode(createDTO.getAlgorithm()));
        task.setStatus(STATUS_CREATED);
        
        // 设置超参数
        if (createDTO.getHyperparameters() != null) {
            task.setLearningRate(createDTO.getHyperparameters().getLearningRate());
            task.setBatchSize(createDTO.getHyperparameters().getBatchSize());
            task.setEpochs(createDTO.getHyperparameters().getEpochs());
            task.setTotalRounds(createDTO.getHyperparameters().getRounds());
            task.setMinParticipants(createDTO.getHyperparameters().getMinParticipants());
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
        // progress现在通过getProgress()方法自动计算，无需设置
        task.setParticipantCount(getSmartParticipants(createDTO).size());
        task.setEstimatedDuration(estimateTaskDuration(createDTO));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setCreatedBy(createdBy);
        task.setConfig(generateConfigJson(createDTO));
        
        return task;
    }

    private void updateTaskFromConfigDTO(FederatedTask task, FederatedTaskConfigDTO configDTO, String updatedBy) {
        if (configDTO.getAlgorithm() != null) {
            task.setAlgorithm(FederatedAlgorithm.fromCode(configDTO.getAlgorithm()));
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
            .role(participant.getRole().getCode())
            .status(participant.getStatus().getCode())
            .lastHeartbeat(participant.getLastHeartbeat())
            .currentEpoch(participant.getCurrentEpoch())
            .loss(participant.getLoss())
            .accuracy(participant.getAccuracy())
            .dataSource(participant.getDataSource())
            .build();
    }

    private TaskDetailVO.MetricsVO buildTaskMetrics(FederatedTask task, List<TaskParticipant> participants) {
        String taskId = task.getId();

        // 优先级1：尝试从缓存获取全局指标
        try {
            Optional<GlobalMetrics> cachedMetricsOpt = metricsCacheService.getGlobalMetrics(taskId);
            if (cachedMetricsOpt.isPresent() && cachedMetricsOpt.get().isValid(30)) {
                GlobalMetrics cachedMetrics = cachedMetricsOpt.get();
                log.debug("从缓存获取全局指标成功: taskId={}", taskId);
                return convertGlobalMetricsToVO(cachedMetrics);
            }
        } catch (CacheValidationException e) {
            log.warn("缓存指标验证失败: taskId={}, error={}", taskId, e.getMessage());
        } catch (Exception e) {
            log.warn("缓存获取失败，降级到数据库查询: taskId={}, error={}", taskId, e.getMessage());
        }

        // 优先级2：降级到数据库查询并更新缓存
        log.info("使用数据库查询构建度量指标并更新缓存: taskId={}", taskId);
        TaskDetailVO.MetricsVO metricsVO = buildTaskMetricsFromDatabase(task, participants);

        // 异步更新缓存（不影响主流程）
        try {
            GlobalMetrics newMetrics = convertVOToGlobalMetrics(metricsVO, taskId);
            metricsCacheService.updateGlobalMetrics(taskId, newMetrics);
            log.debug("已异步更新全局指标缓存: taskId={}", taskId);
        } catch (Exception e) {
            log.warn("更新全局指标缓存失败: taskId={}, error={}", taskId, e.getMessage());
        }

        return metricsVO;
    }

    /**
     * 将全局指标缓存对象转换为VO
     */
    private TaskDetailVO.MetricsVO convertGlobalMetricsToVO(GlobalMetrics globalMetrics) {
        if (globalMetrics == null) {
            throw new CacheValidationException("GlobalMetrics", "unknown", "缓存对象为null");
        }

        return TaskDetailVO.MetricsVO.builder()
                .globalLoss(globalMetrics.getGlobalLoss())
                .globalAccuracy(globalMetrics.getGlobalAccuracy())
                .communicationRounds(globalMetrics.getCommunicationRounds())
                .dataProcessed(globalMetrics.getDataProcessed())
                .estimatedTimeRemaining(globalMetrics.getEstimatedTimeRemaining())
                .build();
    }

    /**
     * 将VO对象转换为全局指标缓存对象
     */
    private GlobalMetrics convertVOToGlobalMetrics(TaskDetailVO.MetricsVO metricsVO, String taskId) {
        return GlobalMetrics.builder()
                .taskId(taskId)
                .globalLoss(metricsVO.getGlobalLoss())
                .globalAccuracy(metricsVO.getGlobalAccuracy())
                .communicationRounds(metricsVO.getCommunicationRounds())
                .dataProcessed(metricsVO.getDataProcessed())
                .estimatedTimeRemaining(metricsVO.getEstimatedTimeRemaining())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * 从数据库构建度量指标（原有逻辑，作为降级方案）
     */
    private TaskDetailVO.MetricsVO buildTaskMetricsFromDatabase(FederatedTask task, List<TaskParticipant> participants) {
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

        TaskDetailVO.MetricsVO metricsVO = TaskDetailVO.MetricsVO.builder()
            .globalLoss(globalLoss)
            .globalAccuracy(globalAccuracy)
            .communicationRounds(task.getCurrentRound())
            .dataProcessed(participants.stream()
                .filter(p -> p.getDataSize() != null)
                .mapToInt(TaskParticipant::getDataSize)
                .sum())
            .estimatedTimeRemaining(estimatedTimeRemaining)
            .build();

        log.debug("数据库降级查询构建度量指标完成: taskId={}, globalAccuracy={}, globalLoss={}",
                task.getId(), globalAccuracy, globalLoss);

        return metricsVO;
    }

    private TaskVO convertToTaskVO(FederatedTask task) {
        InitialModelBindingVO binding = initialModelGenerationService.getTaskBinding(task.getId());
        TaskOperationVO.InitialModelSummary initialModelSummary = extractInitialModelSummary(task, binding);
        TaskOperationVO.ConfigSummaryVO configSummary = buildConfigSummaryFromTask(task, binding);
        return TaskVO.builder()
            .taskId(task.getId())
            .taskName(task.getTaskName())
            .taskType(task.getTaskType())
            .status(task.getStatus().getCode())
            .algorithm(task.getAlgorithm().getCode())
            .createdAt(task.getCreatedAt())
            .startedAt(task.getStartedAt())
            .completedAt(task.getCompletedAt())
            .participantCount(task.getParticipantCount())
            .currentRound(task.getCurrentRound())
            .totalRounds(task.getTotalRounds())
            .progress(task.getProgress())
            .createdBy(task.getCreatedBy())
            .initialModelSummary(initialModelSummary)
            .configSummary(configSummary)
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
            .status(task.getStatus().getCode())
            .finalResults(finalResults)
            .roundResults(roundResults)
            .participantResults(participantResults)
            .modelInfo(modelInfo)
            .build();
    }

    // ========== 图形化配置接口实现 ==========

    @Override
    public ConfigPreviewVO.AvailableVmsVO getAvailableVms(String algorithm, Integer minCpuCores,
                                                         Integer minMemoryMb, String status, String capabilities) {
        log.info("查询可用虚拟机列表: algorithm={}, minCpu={}, minMemory={}", algorithm, minCpuCores, minMemoryMb);

        try {
            // 查询实际的VM数据
            VmQueryDTO queryDTO = VmQueryDTO.builder()
                .status(status)
                .size(100) // 获取所有VM，最大100个
                .userId(BaseContext.getCurrentId()) // 设置当前用户ID
                .build();

            com.feduwacomm.common.PageResult<VmListVO> vmPageResult = vmInstanceService.queryVmList(queryDTO);
            List<VmListVO> vmList = vmPageResult.getList();

            log.info("从数据库查询到 {} 个虚拟机", vmList.size());

            // 转换为ConfigPreviewVO.AvailableVmsVO.VmInfoVO格式
            List<ConfigPreviewVO.AvailableVmsVO.VmInfoVO> vms = vmList.stream()
                .map(this::convertVmListToVmInfo)
                .collect(Collectors.toList());

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

            log.info("过滤后得到 {} 个可用虚拟机", filteredVms.size());

            return ConfigPreviewVO.AvailableVmsVO.builder()
                .total(filteredVms.size())
                .availableVms(filteredVms)
                .build();

        } catch (Exception e) {
            log.error("查询可用虚拟机失败: {}", e.getMessage(), e);
            // 如果查询失败，返回空结果
            return ConfigPreviewVO.AvailableVmsVO.builder()
                .total(0)
                .availableVms(new ArrayList<>())
                .build();
        }
    }

    /**
     * 将VmListVO转换为VmInfoVO
     */
    private ConfigPreviewVO.AvailableVmsVO.VmInfoVO convertVmListToVmInfo(VmListVO vmList) {
        // 设置默认的支持算法列表 - 使用完整的算法名称
        List<String> supportedAlgorithms = Arrays.asList(
            "FEDERATED_AVERAGING",
            "FEDERATED_PROXIMAL",
            "FEDERATED_NOVA",
            "FEDERATED_SCAFFOLD"
        );

        // 设置默认的能力列表
        List<String> capabilities = new ArrayList<>();
        if (vmList.getCpuCores() != null && vmList.getCpuCores() >= 8) {
            capabilities.add("HIGH_CPU");
        }
        if (vmList.getMemoryMb() != null && vmList.getMemoryMb() >= 8192) {
            capabilities.add("HIGH_MEMORY");
        }
        capabilities.add("TRAINING"); // 所有VM都支持训练

        return ConfigPreviewVO.AvailableVmsVO.VmInfoVO.builder()
            .vmId(vmList.getVmId())
            .name(vmList.getName())
            .ipAddress(vmList.getIpAddress())
            .status(vmList.getStatus())
            .connectionStatus(vmList.getConnectionStatus())
            .osType(vmList.getOsType())
            .resources(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.ResourcesVO.builder()
                .cpuCores(vmList.getCpuCores())
                .memoryMb(vmList.getMemoryMb())
                .diskGb(vmList.getDiskGb())
                .gpuCount(0) // 默认值，实际应从VM详情获取
                .gpuMemoryMb(0)
                .build())
            .capabilities(capabilities)
            .supportedAlgorithms(supportedAlgorithms)
            .currentUsage(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.UsageVO.builder()
                .cpuUsage(vmList.getResourceUsage() != null ? vmList.getResourceUsage().getCpu() : 0.0)
                .memoryUsage(vmList.getResourceUsage() != null ? vmList.getResourceUsage().getMemory() : 0.0)
                .networkUsage(15.0) // 默认值
                .build())
            .networkInfo(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.NetworkInfoVO.builder()
                .bandwidth(1000) // 默认值
                .latency(10)
                .uploadSpeed(500)
                .downloadSpeed(800)
                .build())
            .lastHeartbeat(vmList.getLastHeartbeat() != null ? vmList.getLastHeartbeat().toString() : null)
            .reliability(ConfigPreviewVO.AvailableVmsVO.VmInfoVO.ReliabilityVO.builder()
                .uptime(99.0) // 默认值
                .avgResponseTime(150)
                .taskSuccessRate(98.0)
                .build())
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
                .description("参与联邦学习训练的客户端节点，所有虚拟机均为参与者角色，聚合由后端服务统一处理")
                .requirements(ConfigPreviewVO.RoleConfigVO.RoleInfoVO.RequirementsVO.builder()
                    .minCpuCores(2)
                    .minMemoryMb(4096)
                    .requiredCapabilities(Arrays.asList("TRAINING"))
                    .build())
                .compatibleAlgorithms(Arrays.asList("FEDERATED_AVERAGING", "FEDERATED_PROXIMAL", "FEDERATED_NOVA", "FEDERATED_SCAFFOLD"))
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

        List<FederatedTaskController.DistributionPreviewRequestDTO.ParticipantRequestDTO> participants =
            Optional.ofNullable(requestDTO.getParticipants()).orElse(Collections.emptyList());
        if (participants.isEmpty()) {
            throw new UserException("参与者列表不能为空");
        }

        double totalRatio = participants.stream()
            .mapToDouble(participant -> Optional.ofNullable(participant.getRequestedRatio()).orElse(0.0))
            .sum();
        if (totalRatio <= 0.0) {
            throw new UserException("请求比例总和必须大于0");
        }

        int totalRows = 10000;
        int index = 1;
        List<ConfigPreviewVO.DistributionPreviewVO.DistributionResultVO.ParticipantAllocationVO> allocations = new ArrayList<>();
        for (FederatedTaskController.DistributionPreviewRequestDTO.ParticipantRequestDTO participant : participants) {
            double ratio = Optional.ofNullable(participant.getRequestedRatio()).orElse(0.0) / totalRatio;
            int allocatedRows = (int) Math.round(totalRows * ratio);
            int estimatedTime = (int) Math.round(allocatedRows * 0.08 + Math.random() * 100);

            allocations.add(ConfigPreviewVO.DistributionPreviewVO.DistributionResultVO.ParticipantAllocationVO.builder()
                .vmId(participant.getVmId())
                .vmName(String.format("水声联邦学习节点-%03d", index++))
                .allocatedRatio(ratio)
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
                .iidScore(0.85 + Math.random() * 0.1)
                .balanceScore(0.90 + Math.random() * 0.08)
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
            requestDTO.getAlgorithm(), requestDTO.getTaskType(),
            requestDTO.getParticipants() == null ? 0 : requestDTO.getParticipants().size());

        List<FederatedTaskController.ParticipantValidationRequestDTO.ParticipantForValidationDTO> participants =
            Optional.ofNullable(requestDTO.getParticipants()).orElse(Collections.emptyList());
        if (participants.isEmpty()) {
            throw new UserException("参与者列表不能为空");
        }

        List<ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO> validations = new ArrayList<>();
        boolean overallValid = true;

        for (FederatedTaskController.ParticipantValidationRequestDTO.ParticipantForValidationDTO participant : participants) {
            Map<String, ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO.ValidationItemVO> results =
                new HashMap<>();

            results.put("connectivity",
                ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO.ValidationItemVO.builder()
                    .status("PASS")
                    .message("网络连接正常")
                    .build());

            results.put("resources",
                ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO.ValidationItemVO.builder()
                    .status("PASS")
                    .message("资源满足要求")
                    .build());

            results.put("algorithm_support",
                ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO.ValidationItemVO.builder()
                    .status("PASS")
                    .message("支持指定算法")
                    .build());

            boolean participantValid = results.values().stream()
                .allMatch(item -> "PASS".equals(item.getStatus()));

            validations.add(ConfigPreviewVO.ParticipantValidationVO.ValidationResultVO.builder()
                .vmId(participant.getVmId())
                .isValid(participantValid)
                .validationResults(results)
                .build());

            overallValid = overallValid && participantValid;
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
                .status(FederatedTaskStatus.RUNNING.equals(task.getStatus()) ? "COMPLETED" : "PENDING")
                .completedAt(FederatedTaskStatus.RUNNING.equals(task.getStatus()) ? LocalDateTime.now().minusMinutes(20) : null)
                .build()
        );

        List<TaskParticipant> participants = getTaskParticipants(taskId);
        List<TaskConfigStatusVO.ParticipantStatusVO> participantStatuses = participants.stream()
            .map(p -> TaskConfigStatusVO.ParticipantStatusVO.builder()
                .vmId(p.getVmId())
                .configStatus("READY")
                .dataDistributed(true)
                .modelInitialized(FederatedTaskStatus.RUNNING.equals(task.getStatus()))
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
    public TaskOperationVO createSmartTask(FederatedTaskCreateRequest createDTO, String createdBy) {
        log.info("开始创建联邦学习任务: taskName={}, format=v1.5, createdBy={}",
            createDTO.getTaskName(), createdBy);

        // 参数校验
        if (createDTO.getDatasetConfig() == null) {
            throw new UserException("数据集配置不能为空");
        }
        if (createDTO.getParticipantConfig() == null) {
            throw new UserException("参与者配置不能为空");
        }
        if (createDTO.getInitialModelConfig() == null) {
            throw new UserException("初始模型配置不能为空");
        }

        List<FederatedTaskCreateRequest.ParticipantConfigDTO.SmartParticipantDTO> participants = getSmartParticipants(createDTO);
        if (participants.isEmpty()) {
            throw new UserException("任务至少需要一个参与者");
        }

        // 检查虚拟机是否被占用（一台VM只能绑定一个任务）
        for (FederatedTaskCreateRequest.ParticipantConfigDTO.SmartParticipantDTO participant : participants) {
            if (isVmOccupied(participant.getVmId())) {
                throw new UserException("虚拟机 " + participant.getVmId() + " 已被其他任务占用");
            }
        }

        TransactionTemplate creationTemplate = new TransactionTemplate(transactionManager);
        creationTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        TaskCreationResult creationResult = creationTemplate.execute(status ->
            createSmartTaskTransactional(createDTO, createdBy)
        );

        if (creationResult == null || creationResult.getTask() == null) {
            throw new UserException("任务创建失败");
        }

        TaskResourcePreparationResult preparationResult;
        try {
            preparationResult = prepareTaskResources(creationResult.getTask(), createdBy);
        } catch (Exception ex) {
            log.error("任务资源准备失败: taskId={}, error={}", creationResult.getTask().getId(), ex.getMessage(), ex);
            preparationResult = TaskResourcePreparationResult.failed(
                    creationResult.getTask().getDatasetId(),
                    creationResult.getTargetVmIds(),
                    "资源准备过程中发生异常: " + ex.getMessage());
        }

        FederatedTask refreshedTask = tasksMapper.selectTaskById(creationResult.getTask().getId());
        FederatedTaskStatus latestStatus = refreshedTask != null
                ? refreshedTask.getStatus()
                : creationResult.getTask().getStatus();

        TaskOperationVO response = TaskOperationVO.builder()
            .taskId(creationResult.getTask().getId())
            .taskName(creationResult.getTask().getTaskName())
            .status(latestStatus != null ? latestStatus.getCode() : STATUS_CREATED.getCode())
            .createdAt(creationResult.getCreatedAt())
            .createdBy(createdBy)
            .participantCount(creationResult.getParticipantCount())
            .estimatedDuration(creationResult.getEstimatedDuration())
            .configSummary(creationResult.getConfigSummary())
            .build();

        if (preparationResult != null) {
            boolean triggered = preparationResult.isDatasetDistributionTriggered()
                    || preparationResult.isModelDistributionTriggered();
            response.setResourcePreparation(preparationResult.toSummary());
            if (preparationResult.isReady()) {
                response.setStatus(STATUS_PENDING.getCode());
                response.setMessage(StringUtils.hasText(preparationResult.getMessage())
                        ? "任务创建成功，" + preparationResult.getMessage()
                        : "任务创建成功，资源已准备就绪");
            } else if (preparationResult.hasError()) {
                response.setMessage("任务创建成功，但资源准备失败: " + preparationResult.getErrorMessage());
            } else if (triggered) {
                response.setStatus(STATUS_PENDING.getCode());
                if (StringUtils.hasText(preparationResult.getMessage())) {
                    response.setMessage("任务创建成功，" + preparationResult.getMessage());
                } else {
                    response.setMessage("任务创建成功，资源准备进行中");
                }
            } else {
                response.setMessage("任务创建成功，等待资源准备");
            }
        } else {
            response.setMessage("任务创建成功，等待资源准备");
        }
        response.setTimestamp(LocalDateTime.now());

        log.info("智能任务创建响应: taskId={}, status={}, message={}, resourcePrep={}",
                response.getTaskId(), response.getStatus(), response.getMessage(), response.getResourcePreparation());

        return response;
    }

    // ========== 私有辅助方法 ==========

    private List<FederatedTaskCreateRequest.ParticipantConfigDTO.SmartParticipantDTO> getSmartParticipants(FederatedTaskCreateRequest createDTO) {
        if (createDTO.getParticipantConfig() == null || createDTO.getParticipantConfig().getParticipants() == null) {
            return Collections.emptyList();
        }
        return createDTO.getParticipantConfig().getParticipants();
    }

    /**
     * 检查虚拟机是否被占用
     */
    private boolean isVmOccupied(String vmId) {
        // 查询正在运行或配置中的任务是否使用了该VM
        List<String> occupyingStatuses = Arrays.asList("RUNNING", "CONFIGURED", "PAUSED");
        return tasksMapper.countTasksByVmIdAndStatuses(vmId, occupyingStatuses) > 0;
    }

    /**
     * 构建联邦任务实体
     */
    private FederatedTask buildSmartTaskFromCreateDTO(FederatedTaskCreateRequest createDTO, String taskId, String createdBy, LocalDateTime now) {
        FederatedTask task = new FederatedTask();
        List<FederatedTaskCreateRequest.ParticipantConfigDTO.SmartParticipantDTO> participants = getSmartParticipants(createDTO);
        task.setId(taskId);
        task.setTaskName(createDTO.getTaskName());
        task.setTaskType(createDTO.getTaskType());
        task.setDescription(createDTO.getDescription());
        task.setAlgorithm(FederatedAlgorithm.fromCode(createDTO.getAlgorithm()));
        task.setStatus(STATUS_CREATED);
        task.setCreatedBy(createdBy);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setParticipantCount(participants.size());
        task.setCurrentRound(0);
        task.setTotalRounds(createDTO.getHyperparameters() != null ? createDTO.getHyperparameters().getRounds() : 10);
        // progress现在通过getProgress()方法自动计算，无需设置

        // 设置数据集信息
        task.setDatasetId(createDTO.getDatasetConfig().getDatasetId());
        task.setDistributionStrategy(createDTO.getDatasetConfig().getDistributionStrategy());

        // 生成配置JSON
        try {
            task.setConfig(objectMapper.writeValueAsString(createDTO));
        } catch (JsonProcessingException e) {
            throw new UserException("任务配置序列化失败");
        }

        return task;
    }

    /**
     * 构建联邦任务参与者实体
     */
    private TaskParticipant buildSmartParticipantFromDTO(FederatedTaskCreateRequest.ParticipantConfigDTO.SmartParticipantDTO participantDTO,
                                                        String taskId, LocalDateTime now) {
        TaskParticipant participant = new TaskParticipant();
        // 生成主键ID
        participant.setId(uuidUtil.generateUuid());
        participant.setTaskId(taskId);
        participant.setVmId(participantDTO.getVmId());
        participant.setRole(ParticipantRole.fromCode(participantDTO.getRole()));
        participant.setStatus(ParticipantStatus.CREATED);
        participant.setDataRatio(toThousandRatio(participantDTO.getDataRatio()));
        participant.setCreatedAt(now);
        participant.setUpdatedAt(now);

        // 设置扩展字段
        if (participantDTO.getCapabilities() != null) {
            try {
                // 将List转换为JSON数组字符串，以便正确存储到MySQL的JSON列
                participant.setCapabilities(objectMapper.writeValueAsString(participantDTO.getCapabilities()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to convert capabilities to JSON, using comma-separated string", e);
                participant.setCapabilities(String.join(",", participantDTO.getCapabilities()));
            }
        }
        if (participantDTO.getConstraints() != null) {
            participant.setMaxCpuUsage(participantDTO.getConstraints().getMaxCpuUsage());
            participant.setMaxMemoryUsage(participantDTO.getConstraints().getMaxMemoryUsage());
        }

        return participant;
    }

    private Integer toThousandRatio(Double ratio) {
        if (ratio == null) {
            return null;
        }
        int value = (int) Math.round(ratio * 1000);
        if (value < 0) {
            return 0;
        }
        if (value > 1000) {
            return 1000;
        }
        return value;
    }

    private boolean isDatasetPrepared(List<TaskParticipant> participants) {
        if (CollectionUtils.isEmpty(participants)) {
            return false;
        }
        return participants.stream()
                .filter(p -> StringUtils.hasText(p.getVmId()))
                .allMatch(p -> "COMPLETED".equalsIgnoreCase(p.getDatasetStatus()));
    }

    private boolean isInitialModelDistributed(String modelId, List<String> targetVmIds) {
        if (!StringUtils.hasText(modelId) || CollectionUtils.isEmpty(targetVmIds)) {
            return false;
        }
        for (String vmId : targetVmIds) {
            ModelDistribution record = modelDistributionMapper.selectByModelIdAndVmId(modelId, vmId);
            if (record == null || !"COMPLETED".equalsIgnoreCase(record.getDistributionStatus())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 准备任务所需的初始资源（数据集与初始模型）
     * 在任务创建阶段调用，确保只执行一次分发。
     *
     * @param task 联邦学习任务
     * @param operatorId 操作人ID
     * @return 是否完成准备
     */
    private TaskResourcePreparationResult prepareTaskResources(FederatedTask task, String operatorId) {
        TaskResourcePreparationResult result = new TaskResourcePreparationResult();
        if (task == null) {
            result.setErrorMessage("任务不存在");
            result.setMessage("任务不存在，无法准备资源");
            return result;
        }

        String taskId = task.getId();
        result.setDatasetId(task.getDatasetId());

        List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
        if (CollectionUtils.isEmpty(participants)) {
            log.warn("任务{}没有参与者，跳过资源准备", taskId);
            result.setMessage("任务没有参与者，等待参与者配置");
            return result;
        }

        List<String> targetVmIds = participants.stream()
                .map(TaskParticipant::getVmId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        result.setTargetVmIds(targetVmIds);

        if (CollectionUtils.isEmpty(targetVmIds)) {
            log.warn("任务{}的参与者VM ID缺失，无法准备资源", taskId);
            result.setErrorMessage("参与者VM ID缺失，无法执行资源准备");
            result.setMessage("参与者VM ID缺失，无法执行资源准备");
            return result;
        }
        log.info("资源准备初始状态: taskId={}, participants={}, datasetStatuses={}",
                taskId,
                targetVmIds.size(),
                participants.stream()
                        .map(p -> p.getVmId() + ":" + (p.getDatasetStatus() != null ? p.getDatasetStatus() : "NULL"))
                        .collect(Collectors.toList()));

        boolean datasetPrepared = isDatasetPrepared(participants);
        if (!datasetPrepared) {
            if (StringUtils.hasText(task.getDatasetId())) {
                log.info("任务{} 数据集未就绪，触发数据分发流程: datasetId={}, vmCount={}",
                        taskId, task.getDatasetId(), targetVmIds.size());
                result.setDatasetDistributionTriggered(true);
                String effectiveOperator = StringUtils.hasText(operatorId) ? operatorId : "SYSTEM";
                try {
                    vmAckTracker.initializeDatasetAck(taskId, targetVmIds);

                    DataDistributionDTO distributionDTO = buildAutoDistributionDTO(task, targetVmIds);
                    DataDistributionTaskVO distributionTask =
                        dataDistributionService.createDistributionTask(distributionDTO, effectiveOperator);
                    if (distributionTask != null && StringUtils.hasText(distributionTask.getDistributionId())) {
                        result.setDatasetDistributionId(distributionTask.getDistributionId());
                        log.info("数据分发任务已创建: taskId={}, distributionId={}",
                                taskId, distributionTask.getDistributionId());
                        dataDistributionService.startDistribution(distributionTask.getDistributionId(), effectiveOperator);
                        log.info("数据分发任务已启动: taskId={}, distributionId={}",
                                taskId, distributionTask.getDistributionId());
                    } else {
                        log.warn("数据分发任务创建失败或返回空ID: taskId={}", taskId);
                        result.setErrorMessage("数据分发任务创建失败");
                        result.setMessage("数据分发任务创建失败");
                        return result;
                    }

                    result.appendMessage("已触发数据分发任务");
                    boolean datasetAcked = waitForDatasetAcknowledgments(taskId);

                    participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
                    datasetPrepared = isDatasetPrepared(participants);
                    if (datasetAcked && !datasetPrepared) {
                        log.info("任务{} 数据集ACK已到达但状态未更新，执行纠正", taskId);
                        for (TaskParticipant participant : participants) {
                            if (StringUtils.hasText(participant.getVmId()) &&
                                    !"COMPLETED".equalsIgnoreCase(participant.getDatasetStatus())) {
                                participant.setDatasetStatus("COMPLETED");
                                participant.setDatasetCompletedAt(LocalDateTime.now());
                                participant.setUpdatedAt(LocalDateTime.now());
                                taskParticipantsMapper.updateParticipant(participant);
                                log.info("任务{} 数据集状态纠正: vmId={}", taskId, participant.getVmId());
                            }
                        }
                        participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
                        datasetPrepared = isDatasetPrepared(participants);
                    }
                    if (datasetAcked) {
                        if (datasetPrepared) {
                            result.appendMessage("数据分发已完成");
                        } else {
                            result.appendMessage("数据分发已确认，等待参与者状态更新");
                        }
                    } else {
                        result.appendMessage("正在等待数据分发确认");
                    }
                } 
                catch (Exception ex) {
                    log.error("任务{} 数据集分发失败: {}", taskId, ex.getMessage(), ex);
                    result.setErrorMessage("数据集分发失败: " + ex.getMessage());
                    result.setMessage("数据分发过程中发生错误");
                    participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
                    result.setPendingDatasetVmIds(collectPendingDatasetVmIds(participants));
                    return result;
                }
            } else {
                log.info("任务{} 未配置数据集，跳过数据分发准备", taskId);
                datasetPrepared = true;
                result.appendMessage("任务未配置数据集，跳过数据分发");
            }
        } else {
            result.appendMessage("数据分发已完成");
        }
        result.setDatasetPrepared(datasetPrepared);
        List<String> pendingDatasetVmIds = StringUtils.hasText(task.getDatasetId())
                ? collectPendingDatasetVmIds(participants)
                : Collections.emptyList();
        result.setPendingDatasetVmIds(pendingDatasetVmIds);
        if (!datasetPrepared) {
            List<String> datasetStatusSnapshot = participants.stream()
                    .filter(p -> StringUtils.hasText(p.getVmId()))
                    .map(p -> p.getVmId() + ":" + (StringUtils.hasText(p.getDatasetStatus()) ? p.getDatasetStatus() : "NULL"))
                    .collect(Collectors.toList());
            log.info("任务{} 数据集状态快照: {}", taskId, datasetStatusSnapshot);
        }

        InitialModelBindingVO binding = initialModelGenerationService.getTaskBinding(taskId);
        if (binding != null && StringUtils.hasText(binding.getModelId())) {
            result.setInitialModelId(binding.getModelId());
        }
        log.info("任务{} 初始模型绑定查询结果: {}", taskId, binding != null ? binding.getModelId() : "null");

        boolean modelDistributed = false;
        if (binding != null && StringUtils.hasText(binding.getModelId())) {
            modelDistributed = isInitialModelDistributed(binding.getModelId(), targetVmIds);
            if (!modelDistributed) {
                log.info("任务{} 初始模型未分发，开始执行分发流程: modelId={}, vmCount={}",
                        taskId, binding.getModelId(), targetVmIds.size());
                result.setModelDistributionTriggered(true);
                try {
                    result.appendMessage("已触发初始模型分发");
                    InitialModelDistributeRequest distributeRequest = InitialModelDistributeRequest.builder()
                            .modelId(binding.getModelId())
                            .verifyIntegrity(Boolean.TRUE)
                            .timeoutSeconds(Math.max(datasetAckTimeoutSeconds, 300))
                            .build();
                    initialModelGenerationService.distributeInitialModel(
                            taskId,
                            distributeRequest,
                            StringUtils.hasText(operatorId) ? operatorId : "SYSTEM");
                    boolean modelAcked = waitForModelDistributionAcknowledgments(
                            binding.getModelId(),
                            targetVmIds,
                            Math.max(datasetAckTimeoutSeconds, 300));
                    modelDistributed = isInitialModelDistributed(binding.getModelId(), targetVmIds);
                    if (modelAcked) {
                        if (modelDistributed) {
                            result.appendMessage("初始模型分发完成");
                        } else {
                            result.appendMessage("初始模型分发已确认，等待状态入库");
                        }
                    } else {
                        result.appendMessage("正在等待初始模型分发确认");
                    }
                } catch (Exception ex) {
                    log.error("任务{} 初始模型分发失败: {}", taskId, ex.getMessage(), ex);
                    result.setErrorMessage("初始模型分发失败: " + ex.getMessage());
                    result.setMessage("初始模型分发过程中发生错误");
                    result.setPendingModelVmIds(collectPendingModelVmIds(binding.getModelId(), targetVmIds));
                    return result;
                }
            } else {
                result.appendMessage("初始模型已完成分发");
            }
            result.setPendingModelVmIds(collectPendingModelVmIds(binding.getModelId(), targetVmIds));
        } else {
            log.warn("任务{} 未找到绑定的初始模型，跳过初始模型分发准备", taskId);
            result.appendMessage("未找到绑定的初始模型");
            result.setPendingModelVmIds(Collections.emptyList());
        }
        result.setModelDistributed(modelDistributed);

        log.info("资源准备状态: taskId={}, datasetPrepared={}, modelDistributed={}, datasetTriggered={}, modelTriggered={}, ready={}, error={}",
                taskId, datasetPrepared, modelDistributed, result.isDatasetDistributionTriggered(),
                result.isModelDistributionTriggered(), result.isReady(), result.hasError());

        boolean preparationTriggered = result.isDatasetDistributionTriggered() || result.isModelDistributionTriggered();
        if (!result.isReady() && !result.hasError() && preparationTriggered && (task.getStatus() == null || !STATUS_PENDING.equals(task.getStatus()))) {
            LocalDateTime pendingAt = LocalDateTime.now();
            task.setStatus(STATUS_PENDING);
            task.setUpdatedAt(pendingAt);
            int pendingRows = tasksMapper.updateTaskStatus(taskId, STATUS_PENDING.getCode(), pendingAt);
            System.out.println("[prepareTaskResources] pending update: taskId=" + taskId + ", rows=" + pendingRows + ", triggered=" + (result.isDatasetDistributionTriggered() || result.isModelDistributionTriggered()) + ", ready=" + result.isReady() + ", error=" + result.hasError());
            logTask(taskId, "INFO", "任务资源准备进行中，状态更新为PENDING", "TASK_MANAGER", null,
                    Map.of("datasetTriggered", result.isDatasetDistributionTriggered(),
                           "modelTriggered", result.isModelDistributionTriggered()));
            log.info("任务{} 资源准备进入PENDING: datasetTriggered={}, modelTriggered={}, rows={}", taskId,
                    result.isDatasetDistributionTriggered(), result.isModelDistributionTriggered(), pendingRows);
        }

        if (datasetPrepared && modelDistributed) {
            LocalDateTime readyAt = LocalDateTime.now();
            task.setStatus(STATUS_PENDING);
            task.setUpdatedAt(readyAt);
            int readyRows = tasksMapper.updateTaskStatus(taskId, STATUS_PENDING.getCode(), readyAt);
            System.out.println("[prepareTaskResources] ready update: taskId=" + taskId + ", rows=" + readyRows + ", datasetPrepared=" + datasetPrepared + ", modelDistributed=" + modelDistributed);

            logTask(taskId, "INFO", "任务资源准备完成，状态已更新为PENDING", "TASK_MANAGER", null,
                    Map.of("datasetPrepared", true, "modelDistributed", true));
            log.info("任务{} 资源准备完成，rows={}", taskId, readyRows);
            result.markReady(readyAt);
            if (!StringUtils.hasText(result.getMessage())) {
                result.setMessage("资源已准备完成，等待调度");
            }
        } else if (!StringUtils.hasText(result.getMessage()) && !result.hasError()) {
            result.setMessage("资源准备进行中");
        }

        return result;
    }

    private List<String> collectPendingDatasetVmIds(List<TaskParticipant> participants) {
        if (CollectionUtils.isEmpty(participants)) {
            return Collections.emptyList();
        }
        return participants.stream()
                .filter(p -> StringUtils.hasText(p.getVmId()))
                .filter(p -> !"COMPLETED".equalsIgnoreCase(p.getDatasetStatus()))
                .map(TaskParticipant::getVmId)
                .collect(Collectors.toList());
    }

    private DataDistributionDTO buildAutoDistributionDTO(FederatedTask task, List<String> targetVmIds) {
        String strategy = StringUtils.hasText(task.getDistributionStrategy())
                ? task.getDistributionStrategy()
                : "BALANCED";
        Map<String, Object> distributionConfig = new HashMap<>();
        distributionConfig.put("autoTriggered", Boolean.TRUE);
        distributionConfig.put("taskName", task.getTaskName());
        distributionConfig.put("strategy", strategy);

        return DataDistributionDTO.builder()
                .taskId(task.getId())
                .datasetIds(List.of(task.getDatasetId()))
                .distributionStrategy(strategy)
                .targetVmIds(targetVmIds)
                .distributionConfig(distributionConfig)
                .shardCount(Math.max(targetVmIds.size(), 1))
                .enableShuffle(Boolean.FALSE)
                .enableCompression(Boolean.FALSE)
                .verifyIntegrity(Boolean.TRUE)
                .timeoutSeconds(Math.max(datasetAckTimeoutSeconds, 600))
                .maxRetries(3)
                .dataType("TRAINING_DATA")
                .description("auto-preparation")
                .build();
    }

    private List<String> collectPendingModelVmIds(String modelId, List<String> targetVmIds) {
        if (!StringUtils.hasText(modelId) || CollectionUtils.isEmpty(targetVmIds)) {
            return Collections.emptyList();
        }
        List<String> pending = new ArrayList<>();
        for (String vmId : targetVmIds) {
            ModelDistribution record = modelDistributionMapper.selectByModelIdAndVmId(modelId, vmId);
            if (record == null || !"COMPLETED".equalsIgnoreCase(record.getDistributionStatus())) {
                pending.add(vmId);
            }
        }
        return pending;
    }

    private InitialModelBindingVO bindInitialModelForTask(String taskId, InitialModelConfigDTO config, String operatorId) {
        if (config == null) {
            throw new UserException("初始模型配置不能为空");
        }
        if (!StringUtils.hasText(config.getMode())) {
            throw new UserException("初始模型配置模式不能为空");
        }

        String mode = config.getMode().toUpperCase(Locale.ROOT);
        InitialModelBindingVO binding = null;
        switch (mode) {
            case "CUSTOM" -> {
                if (!StringUtils.hasText(config.getInitialModelId())) {
                    throw new UserException("CUSTOM 模式必须提供初始模型ID");
                }
                binding = initialModelGenerationService.bindModelToTask(
                        config.getInitialModelId(),
                        taskId,
                        mode,
                        false,
                        operatorId);
            }
            case "AUTO" -> {
                InitialModelConfigDTO.AutoGenerateConfig auto = config.getAutoGenerateConfig();
                if (auto == null || !StringUtils.hasText(auto.getModelType()) || CollectionUtils.isEmpty(auto.getArchitecture())) {
                    throw new UserException("AUTO 模式需要提供模型类型与架构参数");
                }
                InitialModelGenerateRequest request = InitialModelGenerateRequest.builder()
                        .modelType(auto.getModelType())
                        .architecture(auto.getArchitecture())
                        .description(auto.getDescription())
                        .randomSeed(auto.getRandomSeed())
                        .labels(auto.getLabels())
                        .metadata(auto.getMetadata())
                        .build();
                InitialModelDetailVO generated = initialModelGenerationService.generateInitialModel(request, operatorId);
                binding = initialModelGenerationService.bindModelToTask(
                        generated.getModelId(),
                        taskId,
                        mode,
                        true,
                        operatorId);
            }
            default -> throw new UserException("不支持的初始模型配置模式: " + config.getMode());
        }
        log.info("任务{} 初始模型绑定完成: mode={}, bindingModelId={}", taskId, mode, binding != null ? binding.getModelId() : "null");
        return binding;
    }

    /**
     * 估算智能任务执行时间
     */
    private int estimateSmartTaskDuration(FederatedTaskCreateRequest createDTO) {
        // 根据数据集大小、参与者数量和算法复杂度估算时间
        int baseTime = 3600; // 1小时基础时间
        int participantCount = getSmartParticipants(createDTO).size();
        int rounds = createDTO.getHyperparameters() != null ? createDTO.getHyperparameters().getRounds() : 10;

        // 复杂算法增加时间
        double algorithmMultiplier = 1.0;
        if ("FEDERATED_PROXIMAL".equals(createDTO.getAlgorithm()) || "FEDERATED_SCAFFOLD".equals(createDTO.getAlgorithm())) {
            algorithmMultiplier = 1.3;
        }

        return (int) (baseTime * participantCount * rounds * algorithmMultiplier / 10);
    }

    private TaskOperationVO.ConfigSummaryVO buildConfigSummary(InitialModelConfigDTO config, InitialModelBindingVO binding) {
        TaskOperationVO.InitialModelSummary summary = buildInitialModelSummaryFromConfig(config, binding);
        if (summary == null) {
            return null;
        }
        return TaskOperationVO.ConfigSummaryVO.builder()
                .initialModel(summary)
                .build();
    }

    private TaskOperationVO.ConfigSummaryVO buildConfigSummaryFromTask(FederatedTask task, InitialModelBindingVO binding) {
        TaskOperationVO.InitialModelSummary summary = extractInitialModelSummary(task, binding);
        if (summary == null) {
            return null;
        }
        return TaskOperationVO.ConfigSummaryVO.builder()
                .initialModel(summary)
                .build();
    }

    private TaskOperationVO.InitialModelSummary buildInitialModelSummaryFromConfig(InitialModelConfigDTO config, InitialModelBindingVO binding) {
        String mode = config != null ? config.getMode() : null;
        String configuredModelId = config != null ? config.getInitialModelId() : null;
        return mergeInitialModelSummary(mode, configuredModelId, binding);
    }

    private TaskOperationVO.InitialModelSummary extractInitialModelSummary(FederatedTask task, InitialModelBindingVO binding) {
        String mode = null;
        String configuredModelId = null;
        if (task != null && StringUtils.hasText(task.getConfig())) {
            try {
                JsonNode root = objectMapper.readTree(task.getConfig());
                JsonNode imc = root.get("initialModelConfig");
                if (imc != null) {
                    JsonNode modeNode = imc.get("mode");
                    if (modeNode != null && !modeNode.isNull()) {
                        mode = modeNode.asText();
                    }
                    JsonNode idNode = imc.get("initialModelId");
                    if (idNode != null && !idNode.isNull()) {
                        configuredModelId = idNode.asText();
                    }
                }
            } catch (Exception e) {
                log.warn("解析任务配置中的 initialModelConfig 失败: taskId={}, error={}",
                        task.getId(), e.getMessage());
            }
        }
        return mergeInitialModelSummary(mode, configuredModelId, binding);
    }

    private TaskOperationVO.InitialModelSummary mergeInitialModelSummary(String mode, String configuredModelId,
                                                                        InitialModelBindingVO binding) {
        String effectiveMode = mode;
        String effectiveModelId = configuredModelId;
        boolean autoGenerated = StringUtils.hasText(mode) && "AUTO".equalsIgnoreCase(mode);
        String bindingStatus = null;

        if (binding != null) {
            if (StringUtils.hasText(binding.getModelId())) {
                effectiveModelId = binding.getModelId();
            }
            bindingStatus = binding.getStatus();
            if (binding.getAutoGenerated() != null && binding.getAutoGenerated()) {
                autoGenerated = true;
            }
            if (!StringUtils.hasText(effectiveMode)) {
                effectiveMode = Boolean.TRUE.equals(binding.getAutoGenerated()) ? "AUTO" : "CUSTOM";
            }
        }

        if (!StringUtils.hasText(effectiveMode) && StringUtils.hasText(effectiveModelId)) {
            effectiveMode = "CUSTOM";
        }

        if (!StringUtils.hasText(effectiveMode)) {
            return null;
        }

        return TaskOperationVO.InitialModelSummary.builder()
                .mode(effectiveMode)
                .initialModelId(effectiveModelId)
                .autoGenerated(autoGenerated)
                .bindingStatus(bindingStatus)
                .build();
    }

    @Override
    public GlobalModelsVO getTaskGlobalModels(String taskId) {
        log.info("查询任务全局模型: taskId={}", taskId);

        // 验证任务存在
        FederatedTask task = getTaskById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        // 查询任务的全局模型数据
        List<GlobalModelsVO.GlobalModelInfo> globalModels = new ArrayList<>();

        try {
            // 简化实现：创建模拟的全局模型数据
            // 在实际场景中，这里应该查询真实的全局模型存储
            if ("COMPLETED".equals(task.getStatus()) || "RUNNING".equals(task.getStatus())) {
                // 创建模拟的全局模型
                for (int round = 1; round <= 3; round++) {
                    // 创建聚合的全局模型信息
                    GlobalModelsVO.GlobalModelInfo globalModel = GlobalModelsVO.GlobalModelInfo.builder()
                        .modelId(uuidUtil.generateUuid())
                        .round(round)
                        .aggregationMethod(task.getAlgorithm().name()) // 转换枚举为字符串
                        .participantCount(5) // 模拟参与者数量
                        .createdAt(LocalDateTime.now().minusHours(3 - round)) // 模拟不同时间
                        .version("1.0")
                        .build();

                    // 聚合模型参数和指标
                    Map<String, Object> aggregatedParams = new HashMap<>();
                    aggregatedParams.put("round_" + round + "_parameters", "aggregated_from_5_participants");
                    aggregatedParams.put("layer_weights", "global_weights_round_" + round);

                    Map<String, Object> aggregatedMetrics = new HashMap<>();
                    aggregatedMetrics.put("average_accuracy", 0.85 + (round * 0.02)); // 模拟递增的准确率
                    aggregatedMetrics.put("convergence_rate", 0.95);
                    aggregatedMetrics.put("participant_count", 5);
                    aggregatedMetrics.put("loss", 0.15 - (round * 0.02)); // 模拟递减的损失

                    globalModel.setParameters(aggregatedParams);
                    globalModel.setMetrics(aggregatedMetrics);
                    globalModel.setModelPath("/global/models/" + taskId + "/round_" + round + ".model");

                    globalModels.add(globalModel);
                }
            }

            // 按轮次倒序排列（最新的在前面）
            globalModels.sort((a, b) -> Integer.compare(b.getRound(), a.getRound()));

            log.info("任务全局模型查询完成: taskId={}, modelCount={}", taskId, globalModels.size());

            return GlobalModelsVO.builder()
                .taskId(taskId)
                .models(globalModels)
                .build();

        } catch (Exception e) {
            log.error("查询任务全局模型失败: taskId={}, error={}", taskId, e.getMessage(), e);
            throw new RuntimeException("查询任务全局模型失败: " + e.getMessage());
        }
    }

    // ====================== v1.5协议任务生命周期管理方法 ======================

    /**
     * v1.5启动联邦学习任务
     * 实现"后端大脑"集中控制，VM被动响应
     */
    @Transactional
    public TaskOperationVO startFederatedTask(String taskId) {
        log.info("v1.5启动联邦学习任务: taskId={}", taskId);

        // 获取轮次锁
        if (!roundLockManager.acquireRoundLock(taskId)) {
            throw new UserException("无法获取任务锁，任务可能正在被其他操作处理");
        }

        try {
            // 验证任务状态
            FederatedTask task = getTaskById(taskId);
            if (task == null) {
                throw new UserException("任务不存在");
            }

            if (!STATUS_CONFIGURED.equals(task.getStatus()) && !STATUS_STOPPED.equals(task.getStatus())) {
                throw new UserException("任务状态不允许启动操作，当前状态: " + task.getStatus());
            }

            // 初始化轮次状态管理器
            roundStateManager.initializeTask(taskId, task.getTotalRounds());

            // 设置任务为运行状态
            LocalDateTime now = LocalDateTime.now();
            task.setStatus(STATUS_RUNNING);
            task.setStartedAt(now);
            task.setUpdatedAt(now);

            // 设置v1.5协议字段
            task.setProtocolVersion("1.5");
            task.setLifecycleStatus("ACTIVE");

            // 更新任务状态
            int result = tasksMapper.updateTask(task);
            if (result <= 0) {
                throw new UserException("任务启动失败");
            }

            // 初始化VM确认跟踪器
            List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
            List<String> vmIds = participants.stream()
                .map(TaskParticipant::getVmId)
                .collect(Collectors.toList());
            vmAckTracker.initializeTask(taskId, vmIds);

            // 启动第一轮训练
            startFirstRound(taskId);

            // 记录操作日志
            logTask(taskId, "INFO", "v1.5任务启动成功", "TASK_LIFECYCLE", null,
                Map.of("protocolVersion", "1.5", "participantCount", participants.size()));

            TaskOperationVO response = TaskOperationVO.builder()
                .taskId(taskId)
                .startedAt(now)
                .build();

            log.info("v1.5任务启动完成: taskId={}", taskId);
            return response;

        } finally {
            roundLockManager.releaseRoundLock(taskId);
        }
    }

    /**
     * v1.5停止联邦学习任务
     */
    @Transactional
    public TaskOperationVO stopFederatedTask(String taskId) {
        log.info("v1.5停止联邦学习任务: taskId={}", taskId);

        if (!roundLockManager.acquireRoundLock(taskId)) {
            throw new UserException("无法获取任务锁，任务可能正在被其他操作处理");
        }

        try {
            FederatedTask task = getTaskById(taskId);
            if (task == null) {
                throw new UserException("任务不存在");
            }

            if (!STATUS_RUNNING.equals(task.getStatus()) && !STATUS_PAUSED.equals(task.getStatus())) {
                throw new UserException("任务状态不允许停止操作，当前状态: " + task.getStatus());
            }

            // 发送停止指令到所有VM
            sendStopCommandToAllVMs(taskId);

            // 等待VM确认
            boolean allAcknowledged = vmAckTracker.waitForAllAcknowledgments(taskId, "TASK_STOP", 30000);
            if (!allAcknowledged) {
                log.warn("部分VM未及时确认停止指令: taskId={}", taskId);
            }

            // 更新任务状态
            LocalDateTime now = LocalDateTime.now();
            task.setStatus(STATUS_STOPPED);
            task.setStoppedAt(now);
            task.setUpdatedAt(now);
            task.setLifecycleStatus("STOPPED");

            int result = tasksMapper.updateTask(task);
            if (result <= 0) {
                throw new UserException("任务停止失败");
            }

            // 保存轮次状态快照用于恢复
            saveRoundStateSnapshot(taskId);

            // 记录操作日志
            logTask(taskId, "INFO", "v1.5任务停止成功", "TASK_LIFECYCLE", null,
                Map.of("currentRound", task.getCurrentRound(), "acknowledged", allAcknowledged));

            TaskOperationVO response = TaskOperationVO.builder()
                .taskId(taskId)
                .stoppedAt(now)
                .build();

            log.info("v1.5任务停止完成: taskId={}", taskId);
            return response;

        } finally {
            roundLockManager.releaseRoundLock(taskId);
        }
    }

    /**
     * v1.5恢复联邦学习任务
     */
    @Transactional
    public TaskOperationVO resumeFederatedTask(String taskId) {
        log.info("v1.5恢复联邦学习任务: taskId={}", taskId);

        if (!roundLockManager.acquireRoundLock(taskId)) {
            throw new UserException("无法获取任务锁，任务可能正在被其他操作处理");
        }

        try {
            FederatedTask task = getTaskById(taskId);
            if (task == null) {
                throw new UserException("任务不存在");
            }

            if (!STATUS_PAUSED.equals(task.getStatus()) && !STATUS_STOPPED.equals(task.getStatus())) {
                throw new UserException("任务状态不允许恢复操作，当前状态: " + task.getStatus());
            }

            // 恢复轮次状态
            restoreRoundStateSnapshot(taskId);

            // 重新初始化VM确认跟踪器
            List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
            List<String> vmIds = participants.stream()
                .map(TaskParticipant::getVmId)
                .collect(Collectors.toList());
            vmAckTracker.reinitializeTask(taskId, vmIds);

            // 更新任务状态
            LocalDateTime now = LocalDateTime.now();
            task.setStatus(STATUS_RUNNING);
            task.setResumedAt(now);
            task.setUpdatedAt(now);
            task.setLifecycleStatus("ACTIVE");

            int result = tasksMapper.updateTask(task);
            if (result <= 0) {
                throw new UserException("任务恢复失败");
            }

            // 发送恢复指令到所有VM
            sendResumeCommandToAllVMs(taskId);

            // 记录操作日志
            logTask(taskId, "INFO", "v1.5任务恢复成功", "TASK_LIFECYCLE", null,
                Map.of("resumedRound", task.getCurrentRound()));

            TaskOperationVO response = TaskOperationVO.builder()
                .taskId(taskId)
                .resumedAt(now)
                .build();

            log.info("v1.5任务恢复完成: taskId={}", taskId);
            return response;

        } finally {
            roundLockManager.releaseRoundLock(taskId);
        }
    }

    /**
     * v1.5删除联邦学习任务
     */
    @Transactional
    public TaskOperationVO deleteFederatedTask(String taskId, boolean preserveData) {
        log.info("v1.5删除联邦学习任务: taskId={}, preserveData={}", taskId, preserveData);

        if (!roundLockManager.acquireRoundLock(taskId)) {
            throw new UserException("无法获取任务锁，任务可能正在被其他操作处理");
        }

        try {
            FederatedTask task = getTaskById(taskId);
            if (task == null) {
                throw new UserException("任务不存在");
            }

            // 如果任务正在运行，先停止
            if (STATUS_RUNNING.equals(task.getStatus())) {
                log.info("任务正在运行，先执行停止操作: taskId={}", taskId);
                stopFederatedTask(taskId);
            }

            // 清理v1.5协议相关状态
            roundStateManager.cleanupTask(taskId);
            vmAckTracker.cleanupTask(taskId);

            // 删除任务数据
            LocalDateTime now = LocalDateTime.now();
            if (!preserveData) {
                // 删除参与者数据
                tasksMapper.deleteParticipantsByTaskId(taskId);

                // 删除轮次状态记录
                tasksMapper.deleteRoundStatesByTaskId(taskId);

                // 删除VM确认跟踪记录
                tasksMapper.deleteVmAckTrackingByTaskId(taskId);
            }

            // 标记任务为已删除
            task.setStatus(STATUS_CANCELLED);
            task.setCancelledAt(now);
            task.setUpdatedAt(now);
            task.setLifecycleStatus("DELETED");

            int result = tasksMapper.updateTask(task);
            if (result <= 0) {
                throw new UserException("任务删除失败");
            }

            // 记录操作日志
            logTask(taskId, "INFO", "v1.5任务删除成功", "TASK_LIFECYCLE", null,
                Map.of("preserveData", preserveData));

            TaskOperationVO response = TaskOperationVO.builder()
                .taskId(taskId)
                .deletedAt(now)
                .dataDeleted(!preserveData)
                .build();

            log.info("v1.5任务删除完成: taskId={}", taskId);
            return response;

        } finally {
            roundLockManager.releaseRoundLock(taskId);
        }
    }

    // ====================== v1.5协议辅助方法 ======================

    /**
     * 启动第一轮训练
     */
    private void startFirstRound(String taskId) {
        log.info("启动第一轮训练: taskId={}", taskId);

        // 设置轮次状态为初始化
        roundStateManager.setRoundState(taskId, 1, com.feduwacomm.enums.RoundState.INITIALIZING);

        // 发送ROUND_START消息到所有VM
        sendRoundStartToAllVMs(taskId, 1);

        // 转换到训练状态
        roundStateManager.setRoundState(taskId, 1, com.feduwacomm.enums.RoundState.TRAINING);

        log.info("第一轮训练启动完成: taskId={}", taskId);
    }

    /**
     * 发送停止指令到所有VM
     */
    private void sendStopCommandToAllVMs(String taskId) {
        log.info("发送停止指令到所有VM: taskId={}", taskId);

        List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
        for (TaskParticipant participant : participants) {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "TASK_STOP");
            message.put("taskId", taskId);
            message.put("vmId", participant.getVmId());
            message.put("protocol", "1.5");
            message.put("reason", "USER_REQUESTED");
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/vm/" + participant.getVmId(), message);
            vmAckTracker.trackMessage(taskId, participant.getVmId(), VmAckTracking.AckType.TASK_STOP);
        }
    }

    /**
     * 发送恢复指令到所有VM
     */
    private void sendResumeCommandToAllVMs(String taskId) {
        log.info("发送恢复指令到所有VM: taskId={}", taskId);

        FederatedTask task = getTaskById(taskId);
        List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);

        for (TaskParticipant participant : participants) {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "TASK_RESUME");
            message.put("taskId", taskId);
            message.put("vmId", participant.getVmId());
            message.put("protocol", "1.5");
            message.put("currentRound", task.getCurrentRound());
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/vm/" + participant.getVmId(), message);
        }
    }

    /**
     * 发送轮次开始消息到所有VM
     */
    private void sendRoundStartToAllVMs(String taskId, int round) {
        log.info("发送轮次开始消息: taskId={}, round={}", taskId, round);

        List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
        for (TaskParticipant participant : participants) {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ROUND_START");
            message.put("taskId", taskId);
            message.put("vmId", participant.getVmId());
            message.put("protocol", "1.5");
            message.put("round", round);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/vm/" + participant.getVmId(), message);
            vmAckTracker.trackMessage(taskId, participant.getVmId(), VmAckTracking.AckType.ROUND_START);
        }
    }

    /**
     * 保存轮次状态快照
     */
    private void saveRoundStateSnapshot(String taskId) {
        try {
            String snapshot = roundStateManager.createSnapshot(taskId);

            FederatedTask task = getTaskById(taskId);
            task.setResumeInfo(snapshot);
            task.setUpdatedAt(LocalDateTime.now());

            tasksMapper.updateTask(task);
            log.info("轮次状态快照保存成功: taskId={}", taskId);
        } catch (Exception e) {
            log.error("保存轮次状态快照失败: taskId={}", taskId, e);
        }
    }

    /**
     * 恢复轮次状态快照
     */
    private void restoreRoundStateSnapshot(String taskId) {
        try {
            FederatedTask task = getTaskById(taskId);
            if (task.getResumeInfo() != null && !task.getResumeInfo().isEmpty()) {
                Map<String, Object> snapshot = objectMapper.readValue(task.getResumeInfo(), Map.class);
                roundStateManager.restoreSnapshot(taskId, snapshot);
                log.info("轮次状态快照恢复成功: taskId={}", taskId);
            } else {
                log.warn("未找到轮次状态快照，重新初始化: taskId={}", taskId);
                roundStateManager.initializeTask(taskId, task.getTotalRounds());
            }
        } catch (Exception e) {
            log.error("恢复轮次状态快照失败: taskId={}", taskId, e);
            throw new UserException("任务状态恢复失败");
        }
    }

    @Override
    public TaskStatisticsVO getTaskStatistics() {
        log.info("获取任务统计信息");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
            LocalDateTime weekStart = now.minusDays(7);
            LocalDateTime monthStart = now.minusDays(30);

            // 1. 获取基础统计数据
            TaskQueryDTO allTasksQuery = TaskQueryDTO.builder()
                .size(Integer.MAX_VALUE)
                .build();
            int totalTasks = tasksMapper.countTasksByQuery(allTasksQuery);

            // 2. 按状态统计
            Map<String, Integer> statusDistribution = new HashMap<>();
            statusDistribution.put("PENDING", tasksMapper.countTasksByStatus("PENDING"));
            statusDistribution.put("RUNNING", tasksMapper.countTasksByStatus("RUNNING"));
            statusDistribution.put("PAUSED", tasksMapper.countTasksByStatus("PAUSED"));
            statusDistribution.put("COMPLETED", tasksMapper.countTasksByStatus("COMPLETED"));
            statusDistribution.put("FAILED", tasksMapper.countTasksByStatus("FAILED"));
            statusDistribution.put("CANCELLED", tasksMapper.countTasksByStatus("CANCELLED"));

            // 3. 按算法统计（查询所有任务进行分组）
            List<FederatedTask> allTasks = tasksMapper.selectTasksByQuery(allTasksQuery);
            Map<String, Integer> algorithmDistribution = allTasks.stream()
                .collect(Collectors.groupingBy(
                    task -> task.getAlgorithm() != null ? task.getAlgorithm().name() : "UNKNOWN",
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));

            // 4. 时间范围统计
            TaskQueryDTO todayQuery = TaskQueryDTO.builder()
                .startDate(todayStart)
                .size(Integer.MAX_VALUE)
                .build();
            int todayNewTasks = tasksMapper.countTasksByQuery(todayQuery);

            TaskQueryDTO weekQuery = TaskQueryDTO.builder()
                .startDate(weekStart)
                .size(Integer.MAX_VALUE)
                .build();
            int weekNewTasks = tasksMapper.countTasksByQuery(weekQuery);

            TaskQueryDTO monthQuery = TaskQueryDTO.builder()
                .startDate(monthStart)
                .size(Integer.MAX_VALUE)
                .build();
            int monthNewTasks = tasksMapper.countTasksByQuery(monthQuery);

            // 5. 关键指标统计
            int runningTasks = statusDistribution.get("RUNNING");
            int completedTasks = statusDistribution.get("COMPLETED");
            int failedTasks = statusDistribution.get("FAILED");

            // 6. 计算成功率
            int finishedTasks = completedTasks + failedTasks;
            double successRate = finishedTasks > 0 ? ((double) completedTasks / finishedTasks) * 100 : 0.0;

            // 7. 计算平均完成时间（简化实现）
            List<FederatedTask> completedTasksList = allTasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .collect(Collectors.toList());

            double averageCompletionTime = completedTasksList.stream()
                .filter(task -> task.getStartedAt() != null && task.getCompletedAt() != null)
                .mapToLong(task -> Duration.between(task.getStartedAt(), task.getCompletedAt()).getSeconds())
                .average()
                .orElse(0.0);

            // 8. 获取最近7天趋势
            List<Map<String, Object>> trendData = tasksMapper.selectTaskStatsByDateRange(
                now.minusDays(6).toLocalDate().atStartOfDay(),
                now.toLocalDate().atTime(23, 59, 59)
            );

            List<TaskStatisticsVO.DailyTaskStats> recentTrend = trendData.stream()
                .map(data -> TaskStatisticsVO.DailyTaskStats.builder()
                    .date(data.get("date").toString())
                    .taskCount(((Number) data.get("task_count")).intValue())
                    .completedCount(((Number) data.get("completed_count")).intValue())
                    .failedCount(((Number) data.get("failed_count")).intValue())
                    .build())
                .collect(Collectors.toList());

            // 9. 构建结果
            TaskStatisticsVO statistics = TaskStatisticsVO.builder()
                .totalTasks(totalTasks)
                .statusDistribution(statusDistribution)
                .algorithmDistribution(algorithmDistribution)
                .todayNewTasks(todayNewTasks)
                .weekNewTasks(weekNewTasks)
                .monthNewTasks(monthNewTasks)
                .runningTasks(runningTasks)
                .completedTasks(completedTasks)
                .failedTasks(failedTasks)
                .averageCompletionTime(averageCompletionTime)
                .successRate(Math.round(successRate * 100.0) / 100.0) // 保留2位小数
                .recentTrend(recentTrend)
                .generatedAt(now)
                .build();

            log.info("任务统计信息获取成功: totalTasks={}, runningTasks={}, completedTasks={}",
                totalTasks, runningTasks, completedTasks);

            return statistics;

        } catch (Exception e) {
            log.error("获取任务统计信息失败: {}", e.getMessage(), e);
            throw new UserException("获取任务统计信息失败: " + e.getMessage());
        }
    }

    @Override
    public TaskBatchOperationResultVO batchOperateTask(TaskBatchOperationDTO batchDTO, String operatorId) {
        log.info("执行任务批量操作: operation={}, taskCount={}, operator={}",
            batchDTO.getOperation(), batchDTO.getTaskIds().size(), operatorId);

        long startTime = System.currentTimeMillis();
        LocalDateTime executedAt = LocalDateTime.now();

        try {
            // 1. 验证操作类型
            String operation = batchDTO.getOperation();
            if (!isValidBatchOperation(operation)) {
                throw new UserException("不支持的批量操作类型: " + operation);
            }

            // 2. 验证任务ID列表
            List<String> taskIds = batchDTO.getTaskIds();
            if (taskIds == null || taskIds.isEmpty()) {
                throw new UserException("任务ID列表不能为空");
            }

            if (taskIds.size() > 50) { // 限制批量操作数量
                throw new UserException("批量操作任务数量不能超过50个");
            }

            // 3. 查询所有目标任务
            List<FederatedTask> targetTasks = new ArrayList<>();
            List<String> notFoundTaskIds = new ArrayList<>();

            for (String taskId : taskIds) {
                FederatedTask task = getTaskById(taskId);
                if (task == null) {
                    notFoundTaskIds.add(taskId);
                } else {
                    targetTasks.add(task);
                }
            }

            // 4. 执行批量操作
            List<TaskBatchOperationResultVO.TaskOperationDetail> details = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;
            int skippedCount = 0;

            // 4.1 处理不存在的任务
            for (String taskId : notFoundTaskIds) {
                TaskBatchOperationResultVO.TaskOperationDetail detail = TaskBatchOperationResultVO.TaskOperationDetail.builder()
                    .taskId(taskId)
                    .taskName("未知")
                    .previousStatus("UNKNOWN")
                    .currentStatus("UNKNOWN")
                    .result("FAILURE")
                    .message("任务不存在")
                    .errorCode("TASK_NOT_FOUND")
                    .operationTime(LocalDateTime.now())
                    .build();
                details.add(detail);
                failureCount++;
            }

            // 4.2 处理存在的任务
            for (FederatedTask task : targetTasks) {
                TaskBatchOperationResultVO.TaskOperationDetail detail = processSingleTaskOperation(
                    task, operation, batchDTO, operatorId);
                details.add(detail);

                switch (detail.getResult()) {
                    case "SUCCESS":
                        successCount++;
                        break;
                    case "FAILURE":
                        failureCount++;
                        break;
                    case "SKIPPED":
                        skippedCount++;
                        break;
                }
            }

            // 5. 生成结果摘要
            long duration = System.currentTimeMillis() - startTime;
            String summary = generateBatchOperationSummary(operation, successCount, failureCount, skippedCount);

            // 6. 构建结果
            TaskBatchOperationResultVO result = TaskBatchOperationResultVO.builder()
                .operation(operation)
                .totalTasks(taskIds.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .skippedCount(skippedCount)
                .executedAt(executedAt)
                .durationMs(duration)
                .isAsync(Boolean.FALSE.equals(batchDTO.getAsync()) ? false : batchDTO.getAsync())
                .asyncTaskId(null) // 当前实现为同步操作
                .details(details)
                .summary(summary)
                .build();

            log.info("任务批量操作完成: operation={}, success={}, failure={}, skipped={}, duration={}ms",
                operation, successCount, failureCount, skippedCount, duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("任务批量操作失败: operation={}, error={}, duration={}ms",
                batchDTO.getOperation(), e.getMessage(), duration, e);
            throw new UserException("批量操作失败: " + e.getMessage());
        }
    }

    /**
     * 验证批量操作类型是否有效
     */
    private boolean isValidBatchOperation(String operation) {
        return operation != null && Arrays.asList(
            "START", "STOP", "PAUSE", "RESUME", "CANCEL", "DELETE"
        ).contains(operation.toUpperCase());
    }

    /**
     * 处理单个任务的操作
     */
    private TaskBatchOperationResultVO.TaskOperationDetail processSingleTaskOperation(
            FederatedTask task, String operation, TaskBatchOperationDTO batchDTO, String operatorId) {

        String taskId = task.getId();
        String taskName = task.getTaskName();
        String previousStatus = task.getStatus().name();
        LocalDateTime operationTime = LocalDateTime.now();

        try {
            TaskOperationVO operationResult = null;

            switch (operation.toUpperCase()) {
                case "START":
                    operationResult = startTask(taskId, operatorId);
                    break;
                case "STOP":
                    TaskStopDTO stopDTO = TaskStopDTO.builder()
                        .reason(batchDTO.getReason() != null ? batchDTO.getReason() : "批量停止操作")
                        .saveCheckpoint(!Boolean.TRUE.equals(batchDTO.getForce())) // force=true表示不保存检查点
                        .build();
                    operationResult = stopTask(taskId, stopDTO, operatorId);
                    break;
                case "PAUSE":
                    operationResult = pauseTask(taskId, operatorId);
                    break;
                case "RESUME":
                    operationResult = resumeTask(taskId, operatorId);
                    break;
                case "CANCEL":
                    TaskCancelDTO cancelDTO = TaskCancelDTO.builder()
                        .reason(batchDTO.getReason() != null ? batchDTO.getReason() : "批量取消操作")
                        .build();
                    operationResult = cancelTask(taskId, cancelDTO, operatorId);
                    break;
                case "DELETE":
                    TaskDeleteDTO deleteDTO = TaskDeleteDTO.builder()
                        .deleteData(Boolean.TRUE.equals(batchDTO.getForce())) // force=true时删除数据
                        .deleteModel(Boolean.TRUE.equals(batchDTO.getForce())) // force=true时删除模型
                        .build();
                    operationResult = deleteTask(taskId, deleteDTO, operatorId);
                    break;
                default:
                    throw new UserException("不支持的操作类型: " + operation);
            }

            // 操作成功
            String currentStatus = operationResult.getStatus() != null ?
                operationResult.getStatus() : previousStatus;

            return TaskBatchOperationResultVO.TaskOperationDetail.builder()
                .taskId(taskId)
                .taskName(taskName)
                .previousStatus(previousStatus)
                .currentStatus(currentStatus)
                .result("SUCCESS")
                .message(operationResult.getReason() != null ? operationResult.getReason() : "操作成功")
                .errorCode(null)
                .operationTime(operationTime)
                .build();

        } catch (Exception e) {
            // 操作失败
            log.warn("任务操作失败: taskId={}, operation={}, error={}", taskId, operation, e.getMessage());

            return TaskBatchOperationResultVO.TaskOperationDetail.builder()
                .taskId(taskId)
                .taskName(taskName)
                .previousStatus(previousStatus)
                .currentStatus(previousStatus) // 失败时状态不变
                .result("FAILURE")
                .message("操作失败: " + e.getMessage())
                .errorCode("OPERATION_FAILED")
                .operationTime(operationTime)
                .build();
        }
    }

    /**
     * 生成批量操作摘要
     */
    private String generateBatchOperationSummary(String operation, int successCount, int failureCount, int skippedCount) {
        String operationName = getOperationDisplayName(operation);
        int totalProcessed = successCount + failureCount + skippedCount;

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("批量%s操作完成，", operationName));
        summary.append(String.format("共处理 %d 个任务：", totalProcessed));
        summary.append(String.format("成功 %d 个", successCount));

        if (failureCount > 0) {
            summary.append(String.format("，失败 %d 个", failureCount));
        }

        if (skippedCount > 0) {
            summary.append(String.format("，跳过 %d 个", skippedCount));
        }

        return summary.toString();
    }

    /**
     * 获取操作的显示名称
     */
    private String getOperationDisplayName(String operation) {
        switch (operation.toUpperCase()) {
            case "START":
                return "启动";
            case "STOP":
                return "停止";
            case "PAUSE":
                return "暂停";
            case "RESUME":
                return "恢复";
            case "CANCEL":
                return "取消";
            case "DELETE":
                return "删除";
            default:
                return operation;
        }
    }

    // ========== v1.5标准联邦学习方法实现 ==========

    @Override
    @Transactional
    public TaskOperationVO createStandardFederatedTask(FederatedTaskCreateRequest createDTO, String createdBy) {
        // 📊 进度跟踪：步骤6-7实现
        log.info("开始标准任务创建流程: taskName={}", createDTO.getTaskName());

        // 步骤6-7：创建联邦学习任务
        String taskId = uuidUtil.generateUuid();
        log.info("生成任务ID: {}", taskId);

        // 创建FederatedTask实体
        FederatedTask task = new FederatedTask();
        task.setTaskId(taskId);
        task.setTaskName(createDTO.getTaskName());
        task.setFederatedAlgorithm(createDTO.getAlgorithm());
        task.setStatus(FederatedTaskStatus.CREATING);
        task.setCreatedBy(createdBy);
        task.setCreatedAt(LocalDateTime.now());

        // 保存到数据库
        tasksMapper.insertTask(task);
        log.info("任务创建完成: taskId={}, status=CREATING", taskId);

        // 步骤8：数据集分配
        List<String> participantVmIds = createDTO.getParticipantConfig().getParticipants()
            .stream()
            .map(FederatedTaskCreateRequest.ParticipantConfigDTO.SmartParticipantDTO::getVmId)
            .collect(Collectors.toList());
        log.info("开始数据集分配: taskId={}, participants={}", taskId, participantVmIds.size());

        DatasetAllocationResult allocationResult = allocateDatasets(
            taskId, participantVmIds, createDTO.getDatasetConfig().getDatasetId());

        if (!allocationResult.isSuccess()) {
            log.error("数据集分配失败: taskId={}, error={}", taskId, allocationResult.getErrorMessage());
            throw new RuntimeException("数据集分配失败: " + allocationResult.getErrorMessage());
        }
        log.info("数据集分配成功: taskId={}, allocations={}", taskId, allocationResult.getAllocations().size());

        // 步骤9：数据集验证
        log.info("开始数据集验证: taskId={}", taskId);
        DatasetValidationResult validationResult = validateParticipantDatasets(taskId);
        if (!validationResult.isAllDatasetReady()) {
            log.error("数据集验证失败: taskId={}, results={}", taskId, validationResult.getValidationResults());
            throw new RuntimeException("数据集验证失败，部分虚拟机数据集未就绪");
        }
        log.info("数据集验证成功: taskId={}", taskId);

        // 步骤12：启动联邦学习流程
        log.info("启动联邦学习流程: taskId={}", taskId);
        return startFederatedLearningFlow(taskId);
    }

    @Override
    @Transactional
    public DatasetAllocationResult allocateDatasets(String taskId, List<String> participantVmIds,
                                                   String originalDatasetPath) {
        // 📊 进度跟踪：步骤8实现
        log.info("执行数据集分配: taskId={}, vmCount={}", taskId, participantVmIds.size());

        try {
            // 为每个参与者生成assignedDatasetId
            List<DatasetAllocation> allocations = new ArrayList<>();

            for (String vmId : participantVmIds) {
                String assignedDatasetId = uuidUtil.generateUuid();
                log.info("为VM生成数据集ID: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);

                // 创建TaskParticipant记录
                TaskParticipant participant = new TaskParticipant();
                participant.setTaskId(taskId);
                participant.setVmId(vmId);
                participant.setAssignedDatasetId(assignedDatasetId);
                participant.setDatasetStatus("PENDING");
                participant.setJoinedAt(LocalDateTime.now());

                // 保存参与者记录
                taskParticipantsMapper.insertParticipant(participant);
                log.info("参与者记录创建完成: taskId={}, vmId={}", taskId, vmId);

                // 通过WebSocket发送数据集创建消息
                DatasetSlice datasetSlice = dataDistributionService.generateDatasetSlice(originalDatasetPath, vmId);
                sendDatasetAllocation(vmId, taskId, assignedDatasetId, datasetSlice);
                log.info("数据集分配消息发送完成: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);

                allocations.add(new DatasetAllocation(vmId, assignedDatasetId));
            }

            log.info("数据集分配完成: taskId={}, totalAllocations={}", taskId, allocations.size());
            return DatasetAllocationResult.success(allocations);

        } catch (Exception e) {
            log.error("数据集分配失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return DatasetAllocationResult.failure("数据集分配异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DatasetValidationResult validateParticipantDatasets(String taskId) {
        // 📊 进度跟踪：步骤9实现
        log.info("执行数据集验证: taskId={}", taskId);

        List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
        log.info("获取参与者列表: taskId={}, count={}", taskId, participants.size());

        Map<String, String> validationResults = new HashMap<>();
        int readyCount = 0;

        for (TaskParticipant participant : participants) {
            log.info("验证VM数据集: vmId={}, assignedDatasetId={}",
                    participant.getVmId(), participant.getAssignedDatasetId());

            // 发送DATASET_LIST_QUERY查询数据集状态
            DatasetQueryResult queryResult = queryDatasetStatus(
                participant.getVmId(), taskId, participant.getAssignedDatasetId());

            boolean datasetReady = queryResult.isSuccess()
                    && queryResult.getDatasetStatus() != null
                    && List.of("CREATED", "COMPLETED", "READY").contains(queryResult.getDatasetStatus());

            if (datasetReady) {
                String status = queryResult.getDatasetStatus();
                participant.setDatasetStatus(status);
                if ("COMPLETED".equals(status)) {
                    if (participant.getDatasetCompletedAt() == null) {
                        participant.setDatasetCompletedAt(LocalDateTime.now());
                    }
                } else {
                    if (participant.getDatasetCreatedAt() == null) {
                        participant.setDatasetCreatedAt(LocalDateTime.now());
                    }
                }
                if (queryResult.getLocalPath() != null) {
                    participant.setLocalPath(queryResult.getLocalPath());
                }
                participant.setUpdatedAt(LocalDateTime.now());
                taskParticipantsMapper.updateParticipant(participant);

                readyCount++;
                validationResults.put(participant.getVmId(), "READY");
                log.info("VM数据集验证成功: vmId={}", participant.getVmId());
            } else {
                String failureReason = queryResult.getErrorMessage() != null
                        ? queryResult.getErrorMessage()
                        : "status=" + queryResult.getDatasetStatus();
                validationResults.put(participant.getVmId(), "NOT_READY:" + failureReason);
                log.warn("VM数据集验证失败: vmId={}, status={}, reason={}",
                        participant.getVmId(), queryResult.getDatasetStatus(), failureReason);
            }
        }

        boolean allReady = readyCount == participants.size();
        log.info("数据集验证完成: taskId={}, ready={}/{}, allReady={}",
                taskId, readyCount, participants.size(), allReady);

        return new DatasetValidationResult(allReady, validationResults);
    }

    @Override
    @Transactional
    public TaskOperationVO startFederatedLearningFlow(String taskId) {
        // 📊 进度跟踪：步骤12-13实现
        log.info("启动联邦学习流程: taskId={}", taskId);

        FederatedTask task = tasksMapper.selectByTaskId(taskId);
        if (task == null) {
            throw new UserException("任务不存在或已被删除");
        }

        InitialModelBindingVO binding = initialModelGenerationService.getTaskBinding(taskId);
        if (binding == null || !StringUtils.hasText(binding.getModelId())) {
            throw new UserException("任务尚未绑定初始模型，无法启动联邦学习流程");
        }

        log.info("当前任务绑定初始模型: modelId={}", binding.getModelId());

        InitialModelDetailVO initialModelDetail = initialModelGenerationService.getModelDetail(
                binding.getModelId(), true);
        if (initialModelDetail == null) {
            throw new UserException("未找到任务绑定的初始模型，停止启动流程");
        }

        List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
        log.info("获取参与者列表，准备发送启动消息: taskId={}, count={}", taskId, participants.size());
        if (participants.isEmpty()) {
            throw new UserException("联邦学习任务没有可用参与者，无法启动");
        }

        List<String> targetVmIds = participants.stream()
                .map(TaskParticipant::getVmId)
                .filter(StringUtils::hasText)
                .toList();

        InitialModelDistributionStartedEvent.DistributionConfig distributionConfig =
                new InitialModelDistributionStartedEvent.DistributionConfig(true, datasetAckTimeoutSeconds, 0);
        eventPublisher.publishEvent(new InitialModelDistributionStartedEvent(
                this,
                initialModelDetail.getModelId(),
                taskId,
                null,
                initialModelDetail.getModelType(),
                targetVmIds,
                distributionConfig
        ));

        Map<String, Object> trainingPlan = buildTrainingPlan(task);
        for (TaskParticipant participant : participants) {
            if (!StringUtils.hasText(participant.getVmId())) {
                log.warn("跳过VM ID为空的参与者: participantId={}, taskId={}", participant.getId(), taskId);
                continue;
            }
            ModelDistribution distribution = ensureModelDistributionRecord(
                    initialModelDetail.getModelId(), participant.getVmId());

            Map<String, Object> dataConfig = buildDataConfig(participant);
            Map<String, Object> initialModelPayload = buildInitialModelPayload(
                    initialModelDetail, distribution.getId());
            log.info("初始模型载荷: taskId={}, vmId={}, payload={}", taskId, participant.getVmId(), initialModelPayload);
            log.info("准备推送FEDERATED_TASK_START: vmId={}, distributionId={}, assignedDatasetId={}, datasetStatus={}, localPath={}",
                    participant.getVmId(),
                    distribution.getId(),
                    dataConfig.get("assignedDatasetId"),
                    participant.getDatasetStatus(),
                    participant.getLocalPath());
            if (!StringUtils.hasText((String) dataConfig.get("assignedDatasetId"))) {
                log.warn("参与者缺少assignedDatasetId: participantId={}, vmId={}, taskId={}",
                        participant.getId(), participant.getVmId(), taskId);
            }

            try {
                webSocketProtocolService.sendFederatedTaskStart(
                        participant.getVmId(),
                        taskId,
                        dataConfig,
                        initialModelPayload,
                        trainingPlan);

                LocalDateTime now = LocalDateTime.now();
                modelDistributionMapper.updateDistributionStatus(distribution.getId(), "IN_PROGRESS", now, null);
                modelDistributionMapper.updateVerificationStatus(distribution.getId(), Boolean.FALSE, null);
                log.info("发送任务启动消息: vmId={}, taskId={}, distributionId={}",
                        participant.getVmId(), taskId, distribution.getId());
            } catch (Exception ex) {
                LocalDateTime failedAt = LocalDateTime.now();
                modelDistributionMapper.updateDistributionStatus(
                        distribution.getId(), "FAILED", failedAt, ex.getMessage());
                modelDistributionMapper.updateVerificationStatus(distribution.getId(), Boolean.FALSE, null);
                log.error("向VM发送启动消息失败: vmId={}, taskId={}, error={}",
                        participant.getVmId(), taskId, ex.getMessage(), ex);
                throw new UserException("发送联邦任务启动消息失败: " + ex.getMessage());
            }
        }

        // 步骤13：更新任务状态为RUNNING并开始联邦学习流程
        task.setStatus(FederatedTaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        tasksMapper.updateTask(task);
        log.info("任务状态更新完成: taskId={}, status=RUNNING", taskId);

        boolean datasetPrepared = participants.stream()
                .allMatch(participant -> "COMPLETED".equalsIgnoreCase(participant.getDatasetStatus()));
        TaskOperationVO.ResourcePreparationSummary resourceSummary = TaskOperationVO.ResourcePreparationSummary.builder()
                .datasetPrepared(datasetPrepared)
                .datasetDistributionTriggered(Boolean.TRUE)
                .modelDistributed(InitialModelStatus.DISTRIBUTED.getCode().equalsIgnoreCase(initialModelDetail.getStatus()))
                .modelDistributionTriggered(Boolean.TRUE)
                .datasetId(participants.stream()
                        .map(TaskParticipant::getAssignedDatasetId)
                        .filter(StringUtils::hasText)
                        .findFirst()
                        .orElse(null))
                .initialModelId(initialModelDetail.getModelId())
                .targetVmIds(targetVmIds)
                .readyAt(LocalDateTime.now())
                .datasetSnapshotRound(1)
                .datasetBindingsSnapshot(participants.stream()
                        .filter(participant -> StringUtils.hasText(participant.getVmId())
                                && StringUtils.hasText(participant.getAssignedDatasetId()))
                        .map(participant -> RoundDatasetBinding.builder()
                                .vmId(participant.getVmId())
                                .assignedDatasetId(participant.getAssignedDatasetId())
                                .datasetStatus(participant.getDatasetStatus())
                                .localPath(participant.getLocalPath())
                                .build())
                        .toList())
                .message("资源准备完成，任务已启动")
                .build();

        TaskOperationVO.InitialModelSummary initialModelSummary = TaskOperationVO.InitialModelSummary.builder()
                .mode(binding.getMode())
                .initialModelId(binding.getModelId())
                .autoGenerated(binding.getAutoGenerated())
                .bindingStatus(binding.getStatus())
                .build();

        TaskOperationVO.ConfigSummaryVO configSummary = TaskOperationVO.ConfigSummaryVO.builder()
                .initialModel(initialModelSummary)
                .build();

        return TaskOperationVO.builder()
            .taskId(taskId)
            .status("RUNNING")
            .message("联邦学习任务启动成功")
            .timestamp(LocalDateTime.now())
            .configSummary(configSummary)
            .resourcePreparation(resourceSummary)
            .build();
    }

    private Map<String, Object> buildTrainingPlan(FederatedTask task) {
        Map<String, Object> trainingPlan = new HashMap<>();
        if (task.getAlgorithm() != null) {
            trainingPlan.put("algorithm", task.getAlgorithm().getCode());
        }
        if (task.getTotalRounds() != null) {
            trainingPlan.put("totalRounds", task.getTotalRounds());
        }
        trainingPlan.put("roundNumber", 1);

        Map<String, Object> hyperparameters = new HashMap<>();
        if (task.getLearningRate() != null) {
            hyperparameters.put("learningRate", task.getLearningRate());
        }
        if (task.getBatchSize() != null) {
            hyperparameters.put("batchSize", task.getBatchSize());
        }
        if (task.getEpochs() != null) {
            hyperparameters.put("epochs", task.getEpochs());
        }
        if (!hyperparameters.isEmpty()) {
            trainingPlan.put("hyperparameters", hyperparameters);
        }
        trainingPlan.put("message", "任务初始化阶段已完成，将开始第一轮训练");
        trainingPlan.put("startTimestamp", Instant.now().toString());
        return trainingPlan;
    }

    private Map<String, Object> buildInitialModelPayload(InitialModelDetailVO detail, String distributionId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("distributionId", distributionId);
        payload.put("modelId", detail.getModelId());
        payload.put("modelType", detail.getModelType());
        payload.put("generationMethod", detail.getGenerationMethod());
        payload.put("modelSize", detail.getModelSize());
        payload.put("checksum", detail.getChecksum());
        payload.put("autoGenerated", detail.getAutoGenerated());
        payload.put("metadata", detail.getMetadata());
        payload.put("labels", detail.getLabels());
        payload.put("description", detail.getDescription());
        payload.put("parameters", detail.getModelParameters());
        payload.put("bindingStatus", detail.getBindingStatus());
        return payload;
    }

    private Map<String, Object> buildDataConfig(TaskParticipant participant) {
        Map<String, Object> dataConfig = new HashMap<>();
        dataConfig.put("assignedDatasetId", participant.getAssignedDatasetId());
        dataConfig.put("dataPath", StringUtils.hasText(participant.getLocalPath())
                ? participant.getLocalPath()
                : "/data/training");
        dataConfig.put("validationSplit", 0.2);
        dataConfig.put("shuffle", Boolean.TRUE);
        if (participant.getDataRatio() != null) {
            dataConfig.put("dataRatio", participant.getDataRatio());
        }
        if (StringUtils.hasText(participant.getDatasetStatus())) {
            dataConfig.put("datasetStatus", participant.getDatasetStatus());
        }
        return dataConfig;
    }

    private ModelDistribution ensureModelDistributionRecord(String modelId, String vmId) {
        ModelDistribution existing = modelDistributionMapper.selectByModelIdAndVmId(modelId, vmId);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            log.debug("复用初始模型分发记录: modelId={}, vmId={}, recordId={}", modelId, vmId, existing.getId());
            modelDistributionMapper.updateDistributionStatus(existing.getId(), "PENDING", null, null);
            modelDistributionMapper.updateVerificationStatus(existing.getId(), Boolean.FALSE, null);
            return existing;
        }
        ModelDistribution record = ModelDistribution.builder()
                .id(uuidUtil.generateUuid())
                .modelId(modelId)
                .vmId(vmId)
                .distributionStatus("PENDING")
                .createdAt(now)
                .build();
        modelDistributionMapper.insertModelDistribution(record);
        log.info("创建新的初始模型分发记录: modelId={}, vmId={}, recordId={}", modelId, vmId, record.getId());
        return record;
    }

    // ========== v1.5协议私有辅助方法 ==========

    /**
     * 发送数据集分配消息
     */
    private void sendDatasetAllocation(String vmId, String taskId, String assignedDatasetId, DatasetSlice datasetSlice) {
        // 通过WebSocket发送数据集分配消息
        // TODO: 实现实际的WebSocket消息发送逻辑
        log.info("发送数据集分配消息: vmId={}, taskId={}, assignedDatasetId={}", vmId, taskId, assignedDatasetId);
    }

    /**
     * 查询数据集状态
     */
    private DatasetQueryResult queryDatasetStatus(String vmId, String taskId, String assignedDatasetId) {
        log.info("查询数据集状态: vmId={}, taskId={}, assignedDatasetId={}", vmId, taskId, assignedDatasetId);

        if (assignedDatasetId == null || assignedDatasetId.trim().isEmpty()) {
            log.warn("查询数据集状态失败：assignedDatasetId为空, vmId={}, taskId={}", vmId, taskId);
            return DatasetQueryResult.builder()
                    .success(false)
                    .datasetStatus("UNKNOWN")
                    .errorMessage("assignedDatasetId为空")
                    .build();
        }

        try {
            DatasetQueryResult result = webSocketProtocolService.queryDatasetStatus(vmId, taskId, assignedDatasetId);
            if (result == null) {
                log.warn("数据集状态查询返回null: vmId={}, taskId={}, assignedDatasetId={}", vmId, taskId, assignedDatasetId);
                return DatasetQueryResult.builder()
                        .success(false)
                        .datasetStatus("UNKNOWN")
                        .errorMessage("查询结果为空")
                        .build();
            }
            log.info("数据集状态查询结果: vmId={}, assignedDatasetId={}, status={}, localPath={}",
                    vmId, assignedDatasetId, result.getDatasetStatus(), result.getLocalPath());
            return result;
        } catch (Exception e) {
            log.error("查询数据集状态异常: vmId={}, taskId={}, assignedDatasetId={}, error={}",
                    vmId, taskId, assignedDatasetId, e.getMessage(), e);
            return DatasetQueryResult.builder()
                    .success(false)
                    .datasetStatus("UNKNOWN")
                    .errorMessage("查询异常: " + e.getMessage())
                    .build();
        }
    }

    private static final class TaskCreationResult {
        private final FederatedTask task;
        private final LocalDateTime createdAt;
        private final int participantCount;
        private final int estimatedDuration;
        private final TaskOperationVO.ConfigSummaryVO configSummary;
        private final List<String> targetVmIds;

        TaskCreationResult(FederatedTask task,
                           LocalDateTime createdAt,
                           int participantCount,
                           int estimatedDuration,
                           TaskOperationVO.ConfigSummaryVO configSummary,
                           List<String> targetVmIds) {
            this.task = task;
            this.createdAt = createdAt;
            this.participantCount = participantCount;
            this.estimatedDuration = estimatedDuration;
            this.configSummary = configSummary;
            this.targetVmIds = targetVmIds != null ? new ArrayList<>(targetVmIds) : Collections.emptyList();
        }

        FederatedTask getTask() {
            return task;
        }

        LocalDateTime getCreatedAt() {
            return createdAt;
        }

        int getParticipantCount() {
            return participantCount;
        }

        int getEstimatedDuration() {
            return estimatedDuration;
        }

        TaskOperationVO.ConfigSummaryVO getConfigSummary() {
            return configSummary;
        }

        List<String> getTargetVmIds() {
            return targetVmIds;
        }
    }

    private static final class TaskResourcePreparationResult {
        private boolean ready;
        private boolean datasetPrepared;
        private boolean datasetDistributionTriggered;
        private boolean modelDistributed;
        private boolean modelDistributionTriggered;
        private String datasetDistributionId;
        private List<String> pendingDatasetVmIds = new ArrayList<>();
        private List<String> pendingModelVmIds = new ArrayList<>();
        private List<String> targetVmIds = new ArrayList<>();
        private String datasetId;
        private String initialModelId;
        private LocalDateTime readyAt;
        private String message;
        private String errorMessage;
        private Integer datasetSnapshotRound;
        private List<RoundDatasetBinding> datasetBindingsSnapshot = new ArrayList<>();

        static TaskResourcePreparationResult failed(String datasetId, List<String> targetVmIds, String errorMessage) {
            TaskResourcePreparationResult result = new TaskResourcePreparationResult();
            result.setDatasetId(datasetId);
            result.setTargetVmIds(targetVmIds);
            result.setErrorMessage(errorMessage);
            result.setMessage("资源准备过程中发生异常");
            return result;
        }

        boolean isReady() {
            return ready;
        }

        boolean hasError() {
            return StringUtils.hasText(errorMessage);
        }

        String getErrorMessage() {
            return errorMessage;
        }

        String getMessage() {
            return message;
        }

        void markReady(LocalDateTime readyAt) {
            this.ready = true;
            this.readyAt = readyAt;
            this.datasetPrepared = true;
            this.modelDistributed = true;
        }

        void setDatasetPrepared(boolean datasetPrepared) {
            this.datasetPrepared = datasetPrepared;
        }

        void setDatasetDistributionTriggered(boolean datasetDistributionTriggered) {
            this.datasetDistributionTriggered = datasetDistributionTriggered;
        }

        void setDatasetDistributionId(String datasetDistributionId) {
            this.datasetDistributionId = datasetDistributionId;
        }

        void setModelDistributed(boolean modelDistributed) {
            this.modelDistributed = modelDistributed;
        }

        void setModelDistributionTriggered(boolean modelDistributionTriggered) {
            this.modelDistributionTriggered = modelDistributionTriggered;
        }

        void setPendingDatasetVmIds(List<String> pendingDatasetVmIds) {
            this.pendingDatasetVmIds = pendingDatasetVmIds != null
                    ? new ArrayList<>(pendingDatasetVmIds)
                    : new ArrayList<>();
        }

        void setPendingModelVmIds(List<String> pendingModelVmIds) {
            this.pendingModelVmIds = pendingModelVmIds != null
                    ? new ArrayList<>(pendingModelVmIds)
                    : new ArrayList<>();
        }

        void setTargetVmIds(List<String> targetVmIds) {
            this.targetVmIds = targetVmIds != null
                    ? new ArrayList<>(targetVmIds)
                    : new ArrayList<>();
        }

        void setDatasetId(String datasetId) {
            this.datasetId = datasetId;
        }

        void setInitialModelId(String initialModelId) {
            this.initialModelId = initialModelId;
        }

        void setDatasetSnapshotRound(Integer datasetSnapshotRound) {
            this.datasetSnapshotRound = datasetSnapshotRound;
        }

        void setDatasetBindingsSnapshot(List<RoundDatasetBinding> datasetBindingsSnapshot) {
            this.datasetBindingsSnapshot = datasetBindingsSnapshot != null
                    ? new ArrayList<>(datasetBindingsSnapshot)
                    : new ArrayList<>();
        }

        void setMessage(String message) {
            if (StringUtils.hasText(message)) {
                this.message = message;
            }
        }

        void appendMessage(String append) {
            if (!StringUtils.hasText(append)) {
                return;
            }
            if (!StringUtils.hasText(this.message)) {
                this.message = append;
            } else if (!this.message.contains(append)) {
                this.message = this.message + "；" + append;
            }
        }

        void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        boolean isDatasetDistributionTriggered() {
            return datasetDistributionTriggered;
        }

        boolean isModelDistributionTriggered() {
            return modelDistributionTriggered;
        }

        boolean hasPendingWork() {
            return !ready && (datasetDistributionTriggered || modelDistributionTriggered);
        }

        TaskOperationVO.ResourcePreparationSummary toSummary() {
            return TaskOperationVO.ResourcePreparationSummary.builder()
                    .datasetPrepared(datasetPrepared)
                    .datasetDistributionTriggered(datasetDistributionTriggered)
                    .modelDistributed(modelDistributed)
                    .modelDistributionTriggered(modelDistributionTriggered)
                    .datasetDistributionId(datasetDistributionId)
                    .pendingDatasetVmIds(pendingDatasetVmIds)
                    .pendingModelVmIds(pendingModelVmIds)
                    .datasetId(datasetId)
                    .initialModelId(initialModelId)
                    .targetVmIds(targetVmIds)
                    .readyAt(readyAt)
                    .message(message)
                    .errorMessage(errorMessage)
                    .datasetSnapshotRound(datasetSnapshotRound)
                    .datasetBindingsSnapshot(datasetBindingsSnapshot)
                    .build();
        }
    }

}
