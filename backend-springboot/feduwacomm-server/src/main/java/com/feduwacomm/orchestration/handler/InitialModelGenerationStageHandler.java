package com.feduwacomm.orchestration.handler;

import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 初始模型生成阶段处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialModelGenerationStageHandler extends AbstractStageHandler {

    @Override
    public WorkflowStage getSupportedStage() {
        return WorkflowStage.INITIAL_MODEL_GENERATION;
    }

    @Override
    protected StageResult doExecute(WorkflowContext context) {
        String orchestrationId = context.getOrchestrationId();
        String taskId = context.getTaskId();
        
        log.info("执行初始模型生成阶段: orchestrationId={}, taskId={}", orchestrationId, taskId);
        
        try {
            // 模拟初始模型生成过程
            log.info("开始模拟初始模型生成...");
            
            // 生成模拟的模型ID和相关信息
            String modelId = "model-" + System.currentTimeMillis();
            String modelType = "NEURAL_NETWORK";
            
            // 保存模型信息到上下文
            context.setVariable("initialModelId", modelId);
            context.setVariable("initialModelType", modelType);
            
            log.info("初始模型生成完成（模拟）: orchestrationId={}, taskId={}, modelId={}", 
                orchestrationId, taskId, modelId);
            
            return StageResult.success()
                .addOutput("modelId", modelId)
                .addOutput("modelType", modelType)
                .addOutput("generationMethod", "RANDOM")
                .addOutput("modelSize", 1024000L)
                .addOutput("filePath", "/models/" + modelId + ".bin");
                
        } catch (Exception e) {
            log.error("初始模型生成阶段执行失败: orchestrationId={}, taskId={}", orchestrationId, taskId, e);
            return StageResult.failure("初始模型生成异常: " + e.getMessage(), e);
        }
    }


    @Override
    public boolean validateInput(WorkflowContext context) {
        if (!super.validateInput(context)) {
            return false;
        }
        
        String taskId = context.getTaskId();
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("任务ID为空，无法生成初始模型");
            return false;
        }
        
        return true;
    }

    @Override
    public void cleanup(WorkflowContext context) {
        // 清理临时资源
        context.removeVariable("generationTempData");
        log.debug("初始模型生成阶段清理完成: orchestrationId={}", context.getOrchestrationId());
    }
}