package com.feduwacomm.orchestration.handler;

import com.feduwacomm.enums.GenerationMethod;
import com.feduwacomm.model.dto.initial.InitialModelGenerateRequest;
import com.feduwacomm.model.vo.initial.InitialModelDetailVO;
import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import com.feduwacomm.service.InitialModelGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 初始模型生成阶段处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialModelGenerationStageHandler extends AbstractStageHandler {

    private final InitialModelGenerationService initialModelGenerationService;

    @Override
    public WorkflowStage getSupportedStage() {
        return WorkflowStage.INITIAL_MODEL_GENERATION;
    }

    @Override
    protected StageResult doExecute(WorkflowContext context) {
        String orchestrationId = context.getOrchestrationId();
        String taskId = context.getTaskId();
        String createdBy = (String) context.getVariable("currentUserId");
        if (createdBy == null || createdBy.trim().isEmpty()) {
            createdBy = "system-orchestration";
        }
        
        log.info("执行初始模型生成阶段: orchestrationId={}, taskId={}", orchestrationId, taskId);
        
        try {
            // 获取模型配置参数（如果有的话）
            String modelType = (String) context.getVariable("modelType");
            if (modelType == null) {
                modelType = "RANDOM_FOREST";
            }
            Map<String, Object> architectureParams = getArchitectureParams(context, modelType);
            
            // 构造初始模型生成请求
            InitialModelGenerateRequest generationDTO = InitialModelGenerateRequest.builder()
                    .modelType(modelType)
                    .architecture(architectureParams)
                    .build();
            
            // 调用真实的模型生成服务
            log.info("开始真实的初始模型生成: taskId={}, modelType={}", taskId, modelType);
            InitialModelDetailVO modelDetail = initialModelGenerationService.generateInitialModel(generationDTO, createdBy);

            // 自动绑定到当前任务
            initialModelGenerationService.bindModelToTask(
                    modelDetail.getModelId(),
                    taskId,
                    GenerationMethod.AUTO.getCode(),
                    true,
                    createdBy);
            
            // 保存模型信息到上下文
            context.setVariable("initialModelId", modelDetail.getModelId());
            context.setVariable("initialModelType", modelDetail.getModelType());
            context.setVariable("initialModelStatus", modelDetail.getStatus());
            context.setVariable("initialModelFilePath", modelDetail.getStoragePath());
            context.setVariable("initialModelChecksum", modelDetail.getChecksum());
            
            log.info("初始模型生成完成: orchestrationId={}, taskId={}, modelId={}, status={}", 
                orchestrationId, taskId, modelDetail.getModelId(), modelDetail.getStatus());
            
            return StageResult.success()
                .addOutput("modelId", modelDetail.getModelId())
                .addOutput("modelType", modelDetail.getModelType())
                .addOutput("generationMethod", modelDetail.getGenerationMethod())
                .addOutput("modelSize", modelDetail.getModelSize())
                .addOutput("filePath", modelDetail.getStoragePath())
                .addOutput("checksum", modelDetail.getChecksum())
                .addOutput("status", modelDetail.getStatus());
                
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
    
    /**
     * 获取架构参数
     */
    private Map<String, Object> getArchitectureParams(WorkflowContext context, String modelType) {
        Map<String, Object> params = new HashMap<>();
        
        // 从上下文获取用户自定义参数，如果没有则使用默认参数
        Map<String, Object> userParams = (Map<String, Object>) context.getVariable("architectureParams");
        if (userParams != null) {
            params.putAll(userParams);
        }
        
        // 根据模型类型设置默认参数
        switch (modelType.toUpperCase()) {
            case "RANDOM_FOREST":
                params.putIfAbsent("n_estimators", 100);
                params.putIfAbsent("max_depth", 10);
                params.putIfAbsent("min_samples_split", 2);
                params.putIfAbsent("min_samples_leaf", 1);
                params.putIfAbsent("random_state", 42);
                break;
            case "NEURAL_NETWORK":
                params.putIfAbsent("layers", 3);
                params.putIfAbsent("filters", 32);
                params.putIfAbsent("activation", "relu");
                break;
            case "LSTM":
                params.putIfAbsent("units", 128);
                params.putIfAbsent("layers", 2);
                params.putIfAbsent("dropout", 0.2);
                break;
            case "TRANSFORMER":
                params.putIfAbsent("heads", 8);
                params.putIfAbsent("layers", 6);
                params.putIfAbsent("d_model", 512);
                break;
            default:
                params.putIfAbsent("hidden_size", 256);
                params.putIfAbsent("layers", 3);
        }
        
        return params;
    }
}
