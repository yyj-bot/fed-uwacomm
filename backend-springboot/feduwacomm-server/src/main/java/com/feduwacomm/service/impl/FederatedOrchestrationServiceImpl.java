package com.feduwacomm.service.impl;

import com.feduwacomm.entity.OrchestrationWorkflow;
import com.feduwacomm.entity.WorkflowStageExecution;
import com.feduwacomm.event.FederatedTaskCreatedEvent;
import com.feduwacomm.mapper.OrchestrationWorkflowMapper;
import com.feduwacomm.mapper.WorkflowStageExecutionMapper;
import com.feduwacomm.orchestration.*;
import com.feduwacomm.orchestration.handler.*;
import com.feduwacomm.service.FederatedOrchestrationService;
import com.feduwacomm.utils.UuidUtil;
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
        log.info("收到任务创建事件，开始自动启动工作流: taskId={}", event.getTaskId());
        
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
        log.info("开始执行工作流: workflowId={}, taskId={}", workflow.getId(), workflow.getTaskId());
        
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
            
            if (result.isSuccess()) {
                // 阶段执行成功
                updateStageExecution(stageExecution.getId(),
                    WorkflowStageExecution.StageExecutionStatus.COMPLETED,
                    LocalDateTime.now(),
                    result.getOutputData() != null ? result.getOutputData().toString() : null,
                    null);
                
                // 转换到下一阶段
                transitionToNextStage(context);
                
            } else if (result.isShouldRetry()) {
                // 需要重试
                log.warn("阶段执行失败，将重试: stage={}, orchestrationId={}, error={}", 
                    currentStage, orchestrationId, result.getError());
                // TODO: 实现重试逻辑
                
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
}