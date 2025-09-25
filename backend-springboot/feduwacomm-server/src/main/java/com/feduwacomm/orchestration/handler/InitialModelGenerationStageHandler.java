package com.feduwacomm.orchestration.handler;

import com.feduwacomm.dto.InitialModelGenerationDTO;
import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import com.feduwacomm.service.InitialModelGenerationService;
import com.feduwacomm.vo.InitialModelInfoVO;
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
                modelType = "NEURAL_NETWORK";
            }
            String generationMethod = (String) context.getVariable("generationMethod");
            if (generationMethod == null) {
                generationMethod = "RANDOM";
            }
            Map<String, Object> architectureParams = getArchitectureParams(context, modelType);
            
            // 构造初始模型生成请求
            InitialModelGenerationDTO generationDTO = InitialModelGenerationDTO.builder()
                    .taskId(taskId)
                    .modelType(modelType)
                    .generationMethod(generationMethod)
                    .architectureParams(architectureParams)
                    .autoDistribute(false) // 后续阶段处理分发
                    .build();
            
            // 调用真实的模型生成服务
            log.info("开始真实的初始模型生成: taskId={}, modelType={}, method={}", taskId, modelType, generationMethod);
            InitialModelInfoVO modelInfo = initialModelGenerationService.generateInitialModel(generationDTO, createdBy);
            
            // 保存模型信息到上下文
            context.setVariable("initialModelId", modelInfo.getId());
            context.setVariable("initialModelType", modelInfo.getModelType());
            context.setVariable("initialModelStatus", modelInfo.getStatus());
            context.setVariable("initialModelFilePath", modelInfo.getFilePath());
            context.setVariable("initialModelChecksum", modelInfo.getChecksum());
            
            log.info("初始模型生成完成: orchestrationId={}, taskId={}, modelId={}, status={}", 
                orchestrationId, taskId, modelInfo.getId(), modelInfo.getStatus());
            
            return StageResult.success()
                .addOutput("modelId", modelInfo.getId())
                .addOutput("modelType", modelInfo.getModelType())
                .addOutput("generationMethod", modelInfo.getGenerationMethod())
                .addOutput("modelSize", modelInfo.getModelSize())
                .addOutput("filePath", modelInfo.getFilePath())
                .addOutput("checksum", modelInfo.getChecksum())
                .addOutput("status", modelInfo.getStatus());
                
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