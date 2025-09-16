package com.feduwacomm.orchestration.handler;

import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 初始化阶段处理器
 */
@Slf4j
@Component
public class InitializationStageHandler extends AbstractStageHandler {

    @Override
    public WorkflowStage getSupportedStage() {
        return WorkflowStage.INITIALIZATION;
    }

    @Override
    protected StageResult doExecute(WorkflowContext context) {
        String orchestrationId = context.getOrchestrationId();
        String taskId = context.getTaskId();
        
        log.info("执行初始化阶段: orchestrationId={}, taskId={}", orchestrationId, taskId);
        
        try {
            // 1. 初始化工作流上下文变量
            context.setVariable("startTime", System.currentTimeMillis());
            context.setVariable("initialized", true);
            
            // 2. 验证任务配置
            if (taskId == null || taskId.trim().isEmpty()) {
                return StageResult.failure("任务ID不能为空");
            }
            
            // 3. 设置工作流状态为进行中
            context.getWorkflow().setStatus(context.getWorkflow().getStatus());
            
            log.info("初始化阶段完成: orchestrationId={}", orchestrationId);
            
            return StageResult.success()
                .addOutput("taskId", taskId)
                .addOutput("initTime", System.currentTimeMillis());
                
        } catch (Exception e) {
            log.error("初始化阶段执行失败: orchestrationId={}", orchestrationId, e);
            return StageResult.failure("初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateInput(WorkflowContext context) {
        if (!super.validateInput(context)) {
            return false;
        }
        
        // 验证任务ID
        String taskId = context.getTaskId();
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("任务ID为空，无法执行初始化");
            return false;
        }
        
        return true;
    }
}