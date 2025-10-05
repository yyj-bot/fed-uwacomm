package com.feduwacomm.service.impl;

import com.feduwacomm.entity.OrchestrationWorkflow;
import com.feduwacomm.entity.WorkflowStageExecution;
import com.feduwacomm.event.FederatedTaskCreatedEvent;
import com.feduwacomm.mapper.OrchestrationWorkflowMapper;
import com.feduwacomm.mapper.WorkflowStageExecutionMapper;
import com.feduwacomm.orchestration.*;
import com.feduwacomm.orchestration.handler.*;
import com.feduwacomm.service.FederatedOrchestrationService;
import com.feduwacomm.service.RetryService;
import com.feduwacomm.utils.UuidUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 联邦学习工作流编排服务实现
 * 负责自动化编排整个联邦学习生命周期
 */
@Slf4j
@Service
public class FederatedOrchestrationServiceImpl implements FederatedOrchestrationService {

    @Autowired
    private OrchestrationWorkflowMapper orchestrationMapper;
    
    @Autowired
    private WorkflowStageExecutionMapper stageExecutionMapper;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private UuidUtil uuidUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RetryService retryService;

    // 阶段处理器映射
    private Map<WorkflowStage, StageHandler> stageHandlers;
    
    // 注入各个阶段处理器
    @Autowired
    private InitializationStageHandler initializationHandler;
    
    @Autowired
    private InitialModelGenerationStageHandler modelGenerationHandler;
    
    @Autowired
    private DataDistributionStageHandler dataDistributionHandler;
    
    @Autowired
    private ModelDistributionStageHandler modelDistributionHandler;
    
    @Autowired
    private FederatedTrainingStageHandler trainingHandler;
    
    @Autowired
    private FinalAggregationStageHandler aggregationHandler;
    
    @PostConstruct
    private void initStageHandlers() {
        // 初始化处理器映射
        this.stageHandlers = new HashMap<>();
        this.stageHandlers.put(WorkflowStage.INITIALIZATION, initializationHandler);
        this.stageHandlers.put(WorkflowStage.INITIAL_MODEL_GENERATION, modelGenerationHandler);
        this.stageHandlers.put(WorkflowStage.DATA_DISTRIBUTION, dataDistributionHandler);
        this.stageHandlers.put(WorkflowStage.MODEL_DISTRIBUTION, modelDistributionHandler);
        this.stageHandlers.put(WorkflowStage.FEDERATED_TRAINING, trainingHandler);
        this.stageHandlers.put(WorkflowStage.FINAL_AGGREGATION, aggregationHandler);
        
        log.info("工作流阶段处理器初始化完成，支持 {} 个阶段", stageHandlers.size());
    }

    /**
     * 监听任务创建事件，自动启动工作流
     */
    @Async
    @EventListener
    @Transactional
    public void onTaskCreated(FederatedTaskCreatedEvent event) {
        System.out.println("🔥🔥🔥 onTaskCreated被调用: taskId=" + event.getTaskId());
        log.info("🔥🔥🔥 收到任务创建事件，开始自动启动工作流: taskId={}", event.getTaskId());
        
        try {
            // 创建工作流实例
            OrchestrationWorkflow workflow = createWorkflow(event.getTaskId(), event.getCreatedBy());
            
            // 开始执行工作流
            startWorkflowExecution(workflow);
            
        } catch (Exception e) {
            log.error("自动启动工作流失败: taskId={}", event.getTaskId(), e);
            // 可以发布失败事件，由其他服务处理
        }
    }

    /**
     * 监听任务启动事件，自动创建并启动工作流
     * 在任务启动时（而非创建时）才开始工作流编排
     */
    @Async
    @EventListener
    @Transactional
    public void onTaskStarted(com.feduwacomm.event.FederatedTaskStartedEvent event) {
        log.info("🔥 收到任务启动事件，开始创建工作流: taskId={}", event.getTaskId());

        try {
            // 检查工作流是否已存在（避免重复创建）
            OrchestrationWorkflow existing = orchestrationMapper.selectByTaskId(event.getTaskId());
            if (existing != null) {
                log.info("工作流已存在，跳过创建: taskId={}, workflowId={}",
                    event.getTaskId(), existing.getId());
                return;
            }

            // 创建工作流实例
            OrchestrationWorkflow workflow = createWorkflow(event.getTaskId(), event.getStartedBy());

            // 开始执行工作流
            startWorkflowExecution(workflow);

        } catch (Exception e) {
            log.error("任务启动时创建工作流失败: taskId={}", event.getTaskId(), e);
        }
    }

    @Override
    @Transactional
    public OrchestrationWorkflow createWorkflow(String taskId, String createdBy) {
        String workflowId = uuidUtil.generateUuid();
        
        OrchestrationWorkflow workflow = OrchestrationWorkflow.builder()
                .id(workflowId)
                .taskId(taskId)
                .workflowName("联邦学习自动化工作流-" + taskId)
                .status(OrchestrationWorkflow.WorkflowStatus.CREATED)
                .currentStage(WorkflowStage.INITIALIZATION)
                .config("{\"autoStart\": true}")
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build();
        
        orchestrationMapper.insert(workflow);
        log.info("创建工作流成功: workflowId={}, taskId={}", workflowId, taskId);
        
        return workflow;
    }

    @Override
    @Async
    @Transactional
    public void startWorkflowExecution(OrchestrationWorkflow workflow) {
        System.out.println("🔥🔥🔥 startWorkflowExecution被调用: workflowId=" + workflow.getId() + ", taskId=" + workflow.getTaskId());
        log.info("🔥🔥🔥 开始执行工作流: workflowId={}, taskId={}", workflow.getId(), workflow.getTaskId());
        
        // 更新工作流状态为执行中
        workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS);
        workflow.setStartedAt(LocalDateTime.now());
        orchestrationMapper.update(workflow);
        
        // 创建工作流上下文
        WorkflowContext context = new WorkflowContext(workflow);
        
        // 开始执行第一个阶段
        executeNextStage(context);
    }

    /**
     * 执行下一个工作流阶段
     */
    private void executeNextStage(WorkflowContext context) {
        WorkflowStage currentStage = context.getCurrentStage();
        String orchestrationId = context.getOrchestrationId();
        
        if (currentStage.isFinal()) {
            log.info("工作流已完成: orchestrationId={}", orchestrationId);
            completeWorkflow(context);
            return;
        }
        
        log.info("执行工作流阶段: stage={}, orchestrationId={}", currentStage, orchestrationId);
        
        try {
            // 创建阶段执行记录
            WorkflowStageExecution stageExecution = createStageExecution(orchestrationId, currentStage);
            
            // 获取阶段处理器
            StageHandler handler = stageHandlers.get(currentStage);
            if (handler == null) {
                throw new RuntimeException("未找到阶段处理器: " + currentStage);
            }
            
            // 更新阶段为执行中
            updateStageExecution(stageExecution.getId(), 
                WorkflowStageExecution.StageExecutionStatus.IN_PROGRESS, null, null, null);
            
            // 执行阶段处理逻辑
            StageResult result = handler.execute(context);

            // 检查结果是否为空
            if (result == null) {
                log.error("阶段处理器返回了空结果: stage={}, orchestrationId={}", currentStage, orchestrationId);
                updateStageExecution(stageExecution.getId(),
                    WorkflowStageExecution.StageExecutionStatus.FAILED,
                    LocalDateTime.now(),
                    null,
                    "阶段处理器返回了空结果");
                failWorkflow(context, "阶段处理器返回了空结果");
                return;
            }

            if (result.isSuccess()) {
                // 阶段执行成功
                updateStageExecution(stageExecution.getId(),
                    WorkflowStageExecution.StageExecutionStatus.COMPLETED,
                    LocalDateTime.now(),
                    serializeOutputData(result.getOutputData()),
                    null);
                
                // 转换到下一阶段
                transitionToNextStage(context);
                
            } else if (result.isShouldRetry()) {
                // 需要重试
                log.warn("阶段执行失败，将重试: stage={}, orchestrationId={}, error={}",
                    currentStage, orchestrationId, result.getError());

                // 检查重试策略
                int currentAttempt = retryService.getRetryCount("workflow", orchestrationId);
                RetryService.RetryResult retryResult = retryService.shouldRetryWorkflowStage(
                    orchestrationId, currentStage, currentAttempt, result.getError());

                if (retryResult.shouldRetry()) {
                    // 安排重试
                    Map<String, Object> retryContext = new HashMap<>();
                    retryContext.put("orchestrationId", orchestrationId);
                    retryContext.put("stage", currentStage.name());
                    retryContext.put("originalContext", context.getStageData());

                    retryService.scheduleRetry("workflow", orchestrationId,
                        retryResult.getDelayMillis(), retryResult.getNextAttempt(), retryContext);

                    // 更新阶段状态为等待重试
                    updateStageExecution(stageExecution.getId(),
                        WorkflowStageExecution.StageExecutionStatus.PENDING,
                        null, null, "等待重试: " + retryResult.getReason());
                } else {
                    // 不可重试，标记为永久失败
                    log.error("阶段执行重试失败，永久失败: stage={}, orchestrationId={}, reason={}",
                        currentStage, orchestrationId, retryResult.getReason());

                    updateStageExecution(stageExecution.getId(),
                        WorkflowStageExecution.StageExecutionStatus.FAILED,
                        LocalDateTime.now(), null, "重试失败: " + retryResult.getReason());

                    failWorkflow(context, "重试失败: " + retryResult.getReason());
                }
                
            } else {
                // 永久失败
                updateStageExecution(stageExecution.getId(),
                    WorkflowStageExecution.StageExecutionStatus.FAILED,
                    LocalDateTime.now(),
                    null,
                    result.getError());
                
                failWorkflow(context, result.getError());
            }
            
        } catch (Exception e) {
            log.error("工作流阶段执行异常: stage={}, orchestrationId={}", currentStage, orchestrationId, e);
            failWorkflow(context, "阶段执行异常: " + e.getMessage());
        }
    }

    /**
     * 转换到下一阶段
     */
    private void transitionToNextStage(WorkflowContext context) {
        WorkflowStage currentStage = context.getCurrentStage();
        WorkflowStage nextStage = currentStage.getNext();
        
        if (nextStage != currentStage) {
            log.info("工作流阶段转换: {} -> {}, orchestrationId={}", 
                currentStage, nextStage, context.getOrchestrationId());
            
            // 更新当前阶段
            context.setCurrentStage(nextStage);
            
            // 更新数据库
            OrchestrationWorkflow workflow = context.getWorkflow();
            workflow.setCurrentStage(nextStage);
            orchestrationMapper.update(workflow);
            
            // 继续执行下一阶段
            executeNextStage(context);
        }
    }

    /**
     * 完成工作流
     */
    private void completeWorkflow(WorkflowContext context) {
        String orchestrationId = context.getOrchestrationId();
        
        OrchestrationWorkflow workflow = context.getWorkflow();
        workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.COMPLETED);
        workflow.setCompletedAt(LocalDateTime.now());
        orchestrationMapper.update(workflow);
        
        log.info("工作流执行完成: orchestrationId={}, taskId={}", 
            orchestrationId, workflow.getTaskId());
    }

    /**
     * 工作流执行失败
     */
    private void failWorkflow(WorkflowContext context, String errorMessage) {
        String orchestrationId = context.getOrchestrationId();
        
        OrchestrationWorkflow workflow = context.getWorkflow();
        workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.FAILED);
        workflow.setCompletedAt(LocalDateTime.now());
        orchestrationMapper.update(workflow);
        
        log.error("工作流执行失败: orchestrationId={}, taskId={}, error={}", 
            orchestrationId, workflow.getTaskId(), errorMessage);
    }

    /**
     * 创建阶段执行记录
     */
    private WorkflowStageExecution createStageExecution(String orchestrationId, WorkflowStage stage) {
        WorkflowStageExecution execution = WorkflowStageExecution.builder()
                .id(uuidUtil.generateUuid())
                .orchestrationId(orchestrationId)
                .stageName(stage.name())
                .status(WorkflowStageExecution.StageExecutionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        stageExecutionMapper.insert(execution);
        return execution;
    }

    /**
     * 更新阶段执行记录
     */
    private void updateStageExecution(String executionId, 
                                    WorkflowStageExecution.StageExecutionStatus status,
                                    LocalDateTime completedAt,
                                    String outputData,
                                    String errorMessage) {
        WorkflowStageExecution execution = WorkflowStageExecution.builder()
                .id(executionId)
                .status(status)
                .completedAt(completedAt)
                .outputData(outputData)
                .errorMessage(errorMessage)
                .build();
        
        if (status == WorkflowStageExecution.StageExecutionStatus.IN_PROGRESS) {
            execution.setStartedAt(LocalDateTime.now());
        }
        
        stageExecutionMapper.update(execution);
    }

    @Override
    public OrchestrationWorkflow getWorkflowByTaskId(String taskId) {
        return orchestrationMapper.selectByTaskId(taskId);
    }

    @Override
    public void pauseWorkflow(String orchestrationId) {
        OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
        if (workflow != null && workflow.getStatus() == OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS) {
            workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.PAUSED);
            orchestrationMapper.update(workflow);
            log.info("工作流已暂停: orchestrationId={}", orchestrationId);
        }
    }

    @Override
    public void resumeWorkflow(String orchestrationId) {
        OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
        if (workflow != null && workflow.getStatus() == OrchestrationWorkflow.WorkflowStatus.PAUSED) {
            workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS);
            orchestrationMapper.update(workflow);
            
            // 重新创建上下文并继续执行
            WorkflowContext context = new WorkflowContext(workflow);
            executeNextStage(context);
            
            log.info("工作流已恢复: orchestrationId={}", orchestrationId);
        }
    }

    @Override
    public void terminateWorkflow(String orchestrationId) {
        OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
        if (workflow != null) {
            workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.TERMINATED);
            workflow.setCompletedAt(LocalDateTime.now());
            orchestrationMapper.update(workflow);
            log.info("工作流已终止: orchestrationId={}", orchestrationId);
        }
    }

    /**
     * 序列化输出数据为JSON字符串
     */
    private String serializeOutputData(Map<String, Object> outputData) {
        if (outputData == null || outputData.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(outputData);
        } catch (Exception e) {
            log.warn("序列化输出数据失败，将返回空值: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void rollbackToStage(String taskId, String targetStage, String reason) {
        log.info("回滚到指定阶段: taskId={}, targetStage={}, reason={}", taskId, targetStage, reason);
        // TODO: 实现回滚逻辑
    }

    @Override
    public boolean updateStageStatus(String orchestrationId, String stageName, String status) {
        log.info("更新阶段状态: orchestrationId={}, stageName={}, status={}", orchestrationId, stageName, status);
        // TODO: 实现阶段状态更新逻辑
        return true;
    }

    @Override
    public boolean updateOrchestrationStatus(String orchestrationId, String status) {
        log.info("更新工作流编排状态: orchestrationId={}, status={}", orchestrationId, status);
        try {
            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow != null) {
                workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.valueOf(status));
                workflow.setUpdatedAt(LocalDateTime.now());
                return orchestrationMapper.update(workflow) > 0;
            }
        } catch (Exception e) {
            log.error("更新工作流编排状态失败: orchestrationId={}, status={}", orchestrationId, status, e);
        }
        return false;
    }

    @Override
    public boolean triggerStage(String orchestrationId, String stageName, java.util.Map<String, Object> input) {
        log.info("触发指定阶段: orchestrationId={}, stageName={}", orchestrationId, stageName);
        // TODO: 实现阶段触发逻辑
        return true;
    }

    @Override
    public void terminateOrchestration(String orchestrationId, String reason) {
        log.info("终止工作流编排: orchestrationId={}, reason={}", orchestrationId, reason);
        try {
            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow != null) {
                workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.TERMINATED);
                workflow.setCompletedAt(LocalDateTime.now());
                orchestrationMapper.update(workflow);
                log.info("工作流编排已终止: orchestrationId={}", orchestrationId);
            }
        } catch (Exception e) {
            log.error("终止工作流编排失败: orchestrationId={}, reason={}", orchestrationId, reason, e);
        }
    }

    @Override
    public void triggerNextStage(String orchestrationId, String triggeredBy,
                                java.util.Map<String, Object> context) {
        try {
            log.info("触发下一阶段: orchestrationId={}, triggeredBy={}", orchestrationId, triggeredBy);

            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow == null) {
                log.warn("工作流不存在: orchestrationId={}", orchestrationId);
                return;
            }

            // TODO: 实现触发下一阶段的逻辑
            // 1. 获取当前阶段
            // 2. 确定下一阶段
            // 3. 更新工作流状态
            // 4. 启动下一阶段执行

            log.info("下一阶段触发完成: orchestrationId={}", orchestrationId);
        } catch (Exception e) {
            log.error("触发下一阶段失败: orchestrationId={}, triggeredBy={}", orchestrationId, triggeredBy, e);
        }
    }
}