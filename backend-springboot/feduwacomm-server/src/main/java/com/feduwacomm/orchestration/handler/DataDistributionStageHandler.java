package com.feduwacomm.orchestration.handler;

import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 数据分发阶段处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataDistributionStageHandler extends AbstractStageHandler {

    @Override
    public WorkflowStage getSupportedStage() {
        return WorkflowStage.DATA_DISTRIBUTION;
    }

    @Override
    protected StageResult doExecute(WorkflowContext context) {
        String orchestrationId = context.getOrchestrationId();
        String taskId = context.getTaskId();
        
        log.info("执行数据分发阶段: orchestrationId={}, taskId={}", orchestrationId, taskId);
        
        try {
            // 从上下文获取初始模型ID（如果需要）
            String initialModelId = (String) context.getVariable("initialModelId");
            log.info("关联的初始模型ID: {}", initialModelId);
            
            // 模拟数据分发过程（实际实现可以调用具体的分发服务）
            log.info("开始模拟数据分发过程...");
            
            // 模拟分发逻辑：检查任务、获取虚拟机列表、分配数据
            String distributionId = "dist-" + System.currentTimeMillis();
            context.setVariable("distributionTaskId", distributionId);
            context.setVariable("distributionStrategy", "BALANCED");
            
            log.info("数据分发阶段完成（模拟）: orchestrationId={}, taskId={}, distributionId={}", 
                orchestrationId, taskId, distributionId);
            
            return StageResult.success()
                .addOutput("distributionId", distributionId)
                .addOutput("strategy", "BALANCED")
                .addOutput("targetVmCount", 3)
                .addOutput("status", "COMPLETED");
                
        } catch (Exception e) {
            log.error("数据分发阶段执行失败: orchestrationId={}, taskId={}", orchestrationId, taskId, e);
            return StageResult.failure("数据分发异常: " + e.getMessage(), e);
        }
    }


    @Override
    public boolean validateInput(WorkflowContext context) {
        if (!super.validateInput(context)) {
            return false;
        }
        
        String taskId = context.getTaskId();
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("任务ID为空，无法执行数据分发");
            return false;
        }
        
        // 验证是否有初始模型（如果需要的话）
        String initialModelId = (String) context.getVariable("initialModelId");
        if (initialModelId == null) {
            log.warn("未找到初始模型ID，但数据分发可以继续: taskId={}", taskId);
        }
        
        return true;
    }

    @Override
    public void cleanup(WorkflowContext context) {
        // 清理临时资源
        context.removeVariable("distributionTempData");
        log.debug("数据分发阶段清理完成: orchestrationId={}", context.getOrchestrationId());
    }
}