package com.feduwacomm.orchestration.handler;

import com.feduwacomm.dto.DataDistributionDTO;
import com.feduwacomm.dto.TrainingDataQueryDTO;
import com.feduwacomm.dto.VmQueryDTO;
import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import com.feduwacomm.service.DataDistributionService;
import com.feduwacomm.service.TrainingDataService;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.DataDistributionTaskVO;
import com.feduwacomm.vo.TrainingDataListVO;
import com.feduwacomm.vo.VmListVO;
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
    private final UuidUtil uuidUtil;
    private final VmInstanceService vmInstanceService;
    private final TrainingDataService trainingDataService;

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
            List<String> datasetIds = getDatasetIds(context);
            if (datasetIds.isEmpty()) {
                log.warn("无法执行数据分发：缺少可用的数据集，taskId={}", taskId);
                return StageResult.success()
                        .addOutput("distributionSkipped", true)
                        .addOutput("reason", "缺少可用的数据集");
            }

            List<String> targetVmIds = getTargetVmIds(context);
            if (targetVmIds.isEmpty()) {
                log.warn("无法执行数据分发：缺少参与虚拟机，taskId={}", taskId);
                return StageResult.success()
                        .addOutput("distributionSkipped", true)
                        .addOutput("reason", "缺少参与虚拟机");
            }

            String distributionStrategy = (String) context.getVariable("distributionStrategy");
            if (distributionStrategy == null) {
                distributionStrategy = "BALANCED";
            }

            Map<String, Object> distributionConfig = getDistributionConfig(context);

            DataDistributionDTO distributionDTO = DataDistributionDTO.builder()
                    .taskId(taskId)
                    .datasetIds(datasetIds)
                    .distributionStrategy(distributionStrategy)
                    .targetVmIds(targetVmIds)
                    .distributionConfig(distributionConfig)
                    .shardCount(Math.min(Math.max(targetVmIds.size(), 1) * 2, datasetIds.size()))
                    .enableShuffle(false)
                    .enableCompression(false)
                    .verifyIntegrity(true)
                    .timeoutSeconds(600)
                    .maxRetries(3)
                    .build();

            log.info("开始工作流驱动的数据分发: taskId={}, strategy={}, datasetCount={}, vmCount={}",
                    taskId, distributionStrategy, datasetIds.size(), targetVmIds.size());

            DataDistributionTaskVO distributionTask =
                    dataDistributionService.createDistributionTask(distributionDTO, createdBy);

            if (distributionTask == null) {
                throw new IllegalStateException("数据分发任务创建失败");
            }

            DataDistributionTaskVO startedTask =
                    dataDistributionService.startDistribution(distributionTask.getDistributionId(), createdBy);

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
            // 如果没有指定数据集，从训练数据服务获取可用的数据集
            log.warn("未找到数据集配置，从服务获取可用数据集");
            try {
                TrainingDataQueryDTO queryDTO = TrainingDataQueryDTO.builder()
                    .page(1)
                    .size(100)
                    .build();
                TrainingDataListVO result = trainingDataService.queryDataList(queryDTO);
                List<TrainingDataListVO.TrainingDataItemVO> availableDatasets = result.getDataList();
                datasetIds = availableDatasets.stream()
                    .filter(dataset -> "READY".equals(dataset.getStatus()))
                    .map(TrainingDataListVO.TrainingDataItemVO::getDatasetId)
                    .limit(1)  // 只取第一个可用的数据集
                    .toList();

                if (!datasetIds.isEmpty()) {
                    log.info("从服务获取到 {} 个可用数据集: {}", datasetIds.size(), datasetIds);
                } else {
                    log.warn("未找到任何可用的数据集，创建一个示例数据集ID");
                    String defaultDatasetId = uuidUtil.generateUuid();
                    datasetIds = Arrays.asList(defaultDatasetId);
                }
            } catch (Exception e) {
                log.error("获取数据集列表失败，使用默认数据集", e);
                String defaultDatasetId = uuidUtil.generateUuid();
                datasetIds = Arrays.asList(defaultDatasetId);
            }
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
            // 如果没有指定虚拟机，从虚拟机服务获取所有可用的虚拟机
            log.warn("未找到目标虚拟机配置，从服务获取所有可用虚拟机");
            try {
                VmQueryDTO queryDTO = VmQueryDTO.builder()
                    .page(1)
                    .size(100)
                    .build();
                List<VmListVO> availableVms = vmInstanceService.queryVmList(queryDTO).getList();
                vmIds = availableVms.stream()
                    .map(VmListVO::getVmId)
                    .toList();
                log.info("从服务获取到 {} 个可用虚拟机: {}", vmIds.size(), vmIds);
            } catch (Exception e) {
                log.error("获取虚拟机列表失败，使用空列表", e);
                vmIds = Arrays.asList();
            }
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
