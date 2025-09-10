package com.feduwacomm.orchestration.handler;

import com.feduwacomm.dto.DataDistributionDTO;
import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import com.feduwacomm.service.DataDistributionService;
import com.feduwacomm.vo.DataDistributionTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据分发阶段处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataDistributionStageHandler extends AbstractStageHandler {

    private final DataDistributionService dataDistributionService;

    @Override
    public WorkflowStage getSupportedStage() {
        return WorkflowStage.DATA_DISTRIBUTION;
    }

    @Override
    protected StageResult doExecute(WorkflowContext context) {
        String orchestrationId = context.getOrchestrationId();
        String taskId = context.getTaskId();
        String createdBy = (String) context.getVariable("currentUserId");
        if (createdBy == null || createdBy.trim().isEmpty()) {
            createdBy = "system-orchestration";
        }
        
        log.info("执行数据分发阶段: orchestrationId={}, taskId={}", orchestrationId, taskId);
        
        try {
            // 从上下文获取初始模型ID（如果需要）
            String initialModelId = (String) context.getVariable("initialModelId");
            log.info("关联的初始模型ID: {}", initialModelId);
            
            // 获取分发配置参数
            String distributionStrategy = (String) context.getVariable("distributionStrategy");
            if (distributionStrategy == null) {
                distributionStrategy = "BALANCED";
            }
            List<String> datasetIds = getDatasetIds(context);
            List<String> targetVmIds = getTargetVmIds(context);
            Map<String, Object> distributionConfig = getDistributionConfig(context);
            
            // 构造数据分发请求
            DataDistributionDTO distributionDTO = DataDistributionDTO.builder()
                    .taskId(taskId)
                    .datasetIds(datasetIds)
                    .distributionStrategy(distributionStrategy)
                    .targetVmIds(targetVmIds)
                    .distributionConfig(distributionConfig)
                    .shardCount(Math.min(targetVmIds.size() * 2, datasetIds.size()))
                    .enableShuffle(true)
                    .enableCompression(true)
                    .verifyIntegrity(true)
                    .timeoutSeconds(600)
                    .maxRetries(3)
                    .build();
            
            // 调用真实的数据分发服务
            log.info("开始真实的数据分发: taskId={}, strategy={}, datasetCount={}, vmCount={}", 
                    taskId, distributionStrategy, datasetIds.size(), targetVmIds.size());
            DataDistributionTaskVO distributionTask = dataDistributionService.createDistributionTask(distributionDTO, createdBy);
            
            // 启动分发
            DataDistributionTaskVO startedTask = dataDistributionService.startDistribution(distributionTask.getDistributionId(), createdBy);
            
            // 保存分发信息到上下文
            context.setVariable("distributionTaskId", startedTask.getDistributionId());
            context.setVariable("distributionStrategy", startedTask.getDistributionStrategy());
            context.setVariable("distributionStatus", startedTask.getStatus());
            context.setVariable("targetVmCount", startedTask.getTargetVmCount());
            
            log.info("数据分发阶段完成: orchestrationId={}, taskId={}, distributionId={}, status={}", 
                orchestrationId, taskId, startedTask.getDistributionId(), startedTask.getStatus());
            
            return StageResult.success()
                .addOutput("distributionId", startedTask.getDistributionId())
                .addOutput("strategy", startedTask.getDistributionStrategy())
                .addOutput("targetVmCount", startedTask.getTargetVmCount())
                .addOutput("successVmCount", startedTask.getSuccessVmCount())
                .addOutput("progress", startedTask.getProgress())
                .addOutput("status", startedTask.getStatus());
                
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
    
    /**
     * 获取数据集ID列表
     */
    @SuppressWarnings("unchecked")
    private List<String> getDatasetIds(WorkflowContext context) {
        List<String> datasetIds = (List<String>) context.getVariable("datasetIds");
        if (datasetIds == null || datasetIds.isEmpty()) {
            // 如果没有指定数据集，使用默认数据集ID
            log.warn("未找到数据集配置，使用默认数据集");
            datasetIds = Arrays.asList("dataset-default-" + context.getTaskId());
        }
        return datasetIds;
    }
    
    /**
     * 获取目标虚拟机ID列表
     */
    @SuppressWarnings("unchecked")
    private List<String> getTargetVmIds(WorkflowContext context) {
        List<String> vmIds = (List<String>) context.getVariable("targetVmIds");
        if (vmIds == null || vmIds.isEmpty()) {
            // 如果没有指定虚拟机，使用默认虚拟机列表（实际应从虚拟机服务获取）
            log.warn("未找到目标虚拟机配置，使用默认虚拟机列表");
            vmIds = Arrays.asList("vm-1", "vm-2", "vm-3");
        }
        return vmIds;
    }
    
    /**
     * 获取分发配置
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getDistributionConfig(WorkflowContext context) {
        Map<String, Object> config = (Map<String, Object>) context.getVariable("distributionConfig");
        if (config == null) {
            config = new HashMap<>();
        }
        
        // 设置默认配置
        config.putIfAbsent("batchSize", 1000);
        config.putIfAbsent("compressionEnabled", true);
        config.putIfAbsent("checksumValidation", true);
        config.putIfAbsent("retryCount", 3);
        
        return config;
    }
}