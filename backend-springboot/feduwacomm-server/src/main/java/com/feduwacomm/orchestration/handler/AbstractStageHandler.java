package com.feduwacomm.orchestration.handler;

import com.feduwacomm.orchestration.StageHandler;
import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象阶段处理器
 */
@Slf4j
public abstract class AbstractStageHandler implements StageHandler {

    @Override
    public final StageResult execute(WorkflowContext context) {
        WorkflowStage stage = getSupportedStage();
        String orchestrationId = context.getOrchestrationId();
        
        log.info("开始执行工作流阶段: stage={}, orchestrationId={}", stage, orchestrationId);
        
        try {
            // 验证输入
            if (!validateInput(context)) {
                return StageResult.failure("阶段输入验证失败: " + stage);
            }
            
            // 执行阶段逻辑
            StageResult result = doExecute(context);
            
            if (result.isSuccess()) {
                log.info("工作流阶段执行成功: stage={}, orchestrationId={}", stage, orchestrationId);
            } else {
                log.warn("工作流阶段执行失败: stage={}, orchestrationId={}, error={}", 
                    stage, orchestrationId, result.getError());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("工作流阶段执行异常: stage={}, orchestrationId={}", stage, orchestrationId, e);
            return StageResult.failure("阶段执行异常: " + e.getMessage(), e);
        }
    }

    /**
     * 子类实现具体的执行逻辑
     */
    protected abstract StageResult doExecute(WorkflowContext context);

    @Override
    public boolean validateInput(WorkflowContext context) {
        // 基础验证：检查上下文和工作流实例
        if (context == null) {
            log.error("工作流上下文为空");
            return false;
        }
        
        if (context.getWorkflow() == null) {
            log.error("工作流实例为空");
            return false;
        }
        
        if (context.getCurrentStage() != getSupportedStage()) {
            log.error("当前阶段与处理器不匹配: current={}, supported={}", 
                context.getCurrentStage(), getSupportedStage());
            return false;
        }
        
        return true;
    }
}