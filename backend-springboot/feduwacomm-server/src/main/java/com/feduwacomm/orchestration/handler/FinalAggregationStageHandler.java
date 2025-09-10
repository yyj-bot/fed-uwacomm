package com.feduwacomm.orchestration.handler;

import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 最终聚合阶段处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinalAggregationStageHandler extends AbstractStageHandler {

    @Override
    public WorkflowStage getSupportedStage() {
        return WorkflowStage.FINAL_AGGREGATION;
    }

    @Override
    protected StageResult doExecute(WorkflowContext context) {
        String orchestrationId = context.getOrchestrationId();
        String taskId = context.getTaskId();
        
        log.info("执行最终聚合阶段: orchestrationId={}, taskId={}", orchestrationId, taskId);
        
        try {
            // 从上下文获取训练结果信息
            String trainingTaskId = (String) context.getVariable("federatedTrainingId");
            Integer completedRounds = (Integer) context.getVariable("completedRounds");
            
            log.info("开始最终聚合过程: trainingTaskId={}, completedRounds={}", 
                trainingTaskId, completedRounds);
            
            // 模拟最终聚合过程
            String finalModelId = "final-model-" + System.currentTimeMillis();
            String aggregationMethod = "WEIGHTED_AVERAGE";
            
            // 模拟聚合计算
            simulateFinalAggregation(context, trainingTaskId);
            
            // 生成最终模型和评估结果
            context.setVariable("finalModelId", finalModelId);
            context.setVariable("aggregationMethod", aggregationMethod);
            context.setVariable("aggregationStatus", "COMPLETED");
            
            // 模拟模型评估指标
            double finalAccuracy = 0.87;
            double finalLoss = 0.25;
            
            context.setVariable("finalAccuracy", finalAccuracy);
            context.setVariable("finalLoss", finalLoss);
            
            log.info("最终聚合阶段完成（模拟）: orchestrationId={}, taskId={}, finalModelId={}, accuracy={}", 
                orchestrationId, taskId, finalModelId, finalAccuracy);
            
            return StageResult.success()
                .addOutput("finalModelId", finalModelId)
                .addOutput("aggregationMethod", aggregationMethod)
                .addOutput("finalAccuracy", finalAccuracy)
                .addOutput("finalLoss", finalLoss)
                .addOutput("totalRounds", completedRounds)
                .addOutput("status", "COMPLETED")
                .addOutput("modelSize", 2048000L) // 模拟模型大小
                .addOutput("evaluationMetrics", createEvaluationMetrics());
                
        } catch (Exception e) {
            log.error("最终聚合阶段执行失败: orchestrationId={}, taskId={}", orchestrationId, taskId, e);
            return StageResult.failure("最终聚合异常: " + e.getMessage(), e);
        }
    }

    /**
     * 模拟最终聚合计算过程
     */
    private void simulateFinalAggregation(WorkflowContext context, String trainingTaskId) {
        log.debug("执行最终模型聚合计算: trainingTaskId={}, orchestrationId={}", 
            trainingTaskId, context.getOrchestrationId());
        
        // 模拟：
        // 1. 收集各参与者的最终本地模型
        // 2. 计算权重（基于数据量）
        // 3. 执行加权平均聚合
        // 4. 生成全局最终模型
        // 5. 模型验证和评估
    }

    /**
     * 创建模拟的评估指标
     */
    private Object createEvaluationMetrics() {
        return new Object() {
            public final double precision = 0.86;
            public final double recall = 0.88;
            public final double f1Score = 0.87;
            public final double auc = 0.92;
            public final int testSamples = 1000;
        };
    }

    @Override
    public boolean validateInput(WorkflowContext context) {
        if (!super.validateInput(context)) {
            return false;
        }
        
        String taskId = context.getTaskId();
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("任务ID为空，无法执行最终聚合");
            return false;
        }
        
        // 验证是否有联邦训练结果
        String trainingTaskId = (String) context.getVariable("federatedTrainingId");
        if (trainingTaskId == null || trainingTaskId.trim().isEmpty()) {
            log.error("联邦训练任务ID为空，无法执行最终聚合: taskId={}", taskId);
            return false;
        }
        
        Integer completedRounds = (Integer) context.getVariable("completedRounds");
        if (completedRounds == null || completedRounds <= 0) {
            log.error("训练轮次信息无效，无法执行最终聚合: taskId={}, rounds={}", taskId, completedRounds);
            return false;
        }
        
        return true;
    }

    @Override
    public void cleanup(WorkflowContext context) {
        // 清理聚合计算临时数据
        context.removeVariable("aggregationTempData");
        context.removeVariable("intermediateModels");
        log.debug("最终聚合阶段清理完成: orchestrationId={}", context.getOrchestrationId());
    }
}