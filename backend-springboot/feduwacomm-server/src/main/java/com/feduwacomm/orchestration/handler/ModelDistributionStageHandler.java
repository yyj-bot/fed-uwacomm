package com.feduwacomm.orchestration.handler;

import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import com.feduwacomm.service.GlobalModelDistributionService;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.entity.ModelDistribution;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.dto.VmQueryDTO;
import com.feduwacomm.vo.VmListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型分发阶段处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelDistributionStageHandler extends AbstractStageHandler {

    private final GlobalModelDistributionService globalModelDistributionService;
    private final ModelDistributionMapper modelDistributionMapper;
    private final UuidUtil uuidUtil;
    private final VmInstanceService vmInstanceService;
    private final TaskParticipantsMapper taskParticipantsMapper;

    @Override
    public WorkflowStage getSupportedStage() {
        return WorkflowStage.MODEL_DISTRIBUTION;
    }

    @Override
    protected StageResult doExecute(WorkflowContext context) {
        String orchestrationId = context.getOrchestrationId();
        String taskId = context.getTaskId();
        
        log.info("执行模型分发阶段: orchestrationId={}, taskId={}", orchestrationId, taskId);
        
        try {
            // 从上下文获取初始模型信息
            String initialModelId = (String) context.getVariable("initialModelId");
            String distributionId = (String) context.getVariable("distributionTaskId");
            Integer roundNumber = (Integer) context.getVariable("roundNumber");
            if (roundNumber == null) {
                roundNumber = 1;
            }
            
            log.info("开始真实模型分发: modelId={}, distributionId={}, roundNumber={}", 
                    initialModelId, distributionId, roundNumber);
            
            if (initialModelId == null || initialModelId.trim().isEmpty()) {
                throw new IllegalArgumentException("初始模型ID为空，无法进行模型分发");
            }
            
            // 获取目标虚拟机ID列表（从数据分发阶段或上下文获取）
            @SuppressWarnings("unchecked")
            List<String> targetVmIds = (List<String>) context.getVariable("targetVmIds");
            if (targetVmIds == null || targetVmIds.isEmpty()) {
                targetVmIds = taskParticipantsMapper.selectParticipantsByTaskId(taskId).stream()
                        .filter(participant -> participant.getAssignedDatasetId() != null)
                        .filter(participant -> participant.getDatasetStatus() != null
                                && participant.getDatasetStatus().equalsIgnoreCase("COMPLETED"))
                        .map(participant -> participant.getVmId())
                        .toList();
                log.info("从任务参与者中筛选模型分发目标: taskId={}, count={}", taskId, targetVmIds.size());
            }

            if (targetVmIds == null || targetVmIds.isEmpty()) {
                log.warn("任务参与者没有可用的目标VM，尝试退回VM服务查询");
                try {
                    VmQueryDTO queryDTO = VmQueryDTO.builder()
                        .page(1)
                        .size(50)
                        .build();
                    List<VmListVO> availableVms = vmInstanceService.queryVmList(queryDTO).getList();
                    targetVmIds = availableVms.stream()
                        .map(VmListVO::getVmId)
                        .toList();
                    log.info("退回VM服务获取目标: {}", targetVmIds);
                } catch (Exception e) {
                    log.error("获取虚拟机列表失败，模型分发无法继续", e);
                    throw new RuntimeException("无法获取虚拟机列表进行模型分发: " + e.getMessage(), e);
                }
            }

            if (targetVmIds.isEmpty()) {
                throw new RuntimeException("没有可用的虚拟机进行模型分发");
            }
            
            // 创建模型分发记录
            List<ModelDistribution> distributionRecords = createDistributionRecords(
                    initialModelId, targetVmIds, taskId);
            
            // 批量插入分发记录
            if (!distributionRecords.isEmpty()) {
                modelDistributionMapper.batchInsertModelDistributions(distributionRecords);
            }
            
            // 构建全局指标（如果有的话）
            Map<String, Object> globalMetrics = buildGlobalMetrics(context);
            
            // 使用真实的全局模型分发服务进行分发
            globalModelDistributionService.distributeGlobalModel(
                    taskId, roundNumber, initialModelId, globalMetrics, targetVmIds);
            
            // 更新分发记录状态为进行中
            for (ModelDistribution record : distributionRecords) {
                modelDistributionMapper.updateDistributionStatus(
                        record.getId(), "IN_PROGRESS", LocalDateTime.now(), null);
            }
            
            // 保存分发信息到上下文
            String modelDistributionId = "model-dist-" + System.currentTimeMillis();
            context.setVariable("modelDistributionId", modelDistributionId);
            context.setVariable("modelDistributionStatus", "IN_PROGRESS");
            context.setVariable("modelDistributionTargets", targetVmIds.size());
            context.setVariable("modelDistributionRecords", distributionRecords.size());
            
            log.info("模型分发阶段完成: orchestrationId={}, taskId={}, modelId={}, targetCount={}", 
                    orchestrationId, taskId, initialModelId, targetVmIds.size());
            
            return StageResult.success()
                .addOutput("modelDistributionId", modelDistributionId)
                .addOutput("modelId", initialModelId)
                .addOutput("targetVmCount", targetVmIds.size())
                .addOutput("distributionRecords", distributionRecords.size())
                .addOutput("roundNumber", roundNumber)
                .addOutput("status", "IN_PROGRESS")
                .addOutput("distributionMethod", "WEBSOCKET_BROADCAST");
                
        } catch (Exception e) {
            log.error("模型分发阶段执行失败: orchestrationId={}, taskId={}", orchestrationId, taskId, e);
            return StageResult.failure("模型分发异常: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateInput(WorkflowContext context) {
        if (!super.validateInput(context)) {
            return false;
        }
        
        String taskId = context.getTaskId();
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("任务ID为空，无法执行模型分发");
            return false;
        }
        
        // 验证是否有初始模型
        String initialModelId = (String) context.getVariable("initialModelId");
        if (initialModelId == null || initialModelId.trim().isEmpty()) {
            log.error("初始模型ID为空，无法执行模型分发: taskId={}", taskId);
            return false;
        }
        
        return true;
    }

    @Override
    public void cleanup(WorkflowContext context) {
        // 清理临时资源
        context.removeVariable("modelDistributionTempData");
        log.debug("模型分发阶段清理完成: orchestrationId={}", context.getOrchestrationId());
    }
    
    /**
     * 创建模型分发记录
     */
    private List<ModelDistribution> createDistributionRecords(String modelId, List<String> targetVmIds, String taskId) {
        List<ModelDistribution> records = new ArrayList<>();
        
        for (String vmId : targetVmIds) {
            ModelDistribution distribution = ModelDistribution.builder()
                    .id(uuidUtil.generateUuid())
                    .modelId(modelId)
                    .vmId(vmId)
                    .distributionStatus("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            
            records.add(distribution);
        }
        
        return records;
    }
    
    /**
     * 构建全局指标
     */
    private Map<String, Object> buildGlobalMetrics(WorkflowContext context) {
        Map<String, Object> metrics = new HashMap<>();
        
        // 从上下文获取可用的指标信息
        Object accuracy = context.getVariable("modelAccuracy");
        Object loss = context.getVariable("modelLoss");
        Object modelSize = context.getVariable("initialModelSize");
        
        if (accuracy != null) {
            metrics.put("accuracy", accuracy);
        }
        if (loss != null) {
            metrics.put("loss", loss);
        }
        if (modelSize != null) {
            metrics.put("modelSize", modelSize);
        }
        
        // 添加分发相关指标
        metrics.put("distributionTime", LocalDateTime.now().toString());
        metrics.put("distributionMethod", "WEBSOCKET");
        
        return metrics;
    }
}
