package com.feduwacomm.orchestration.handler;

import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.orchestration.StageResult;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 初始化阶段处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitializationStageHandler extends AbstractStageHandler {

    private final FederatedTasksMapper federatedTasksMapper;
    private final TaskParticipantsMapper taskParticipantsMapper;

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
            // 1. 从数据库读取任务信息
            FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            if (task == null) {
                return StageResult.failure("任务不存在: " + taskId);
            }
            System.out.println("🔥🔥🔥 InitializationStageHandler读取任务: taskId=" + taskId + ", datasetId=" + task.getDatasetId() + ", strategy=" + task.getDistributionStrategy());

            // 2. 初始化工作流上下文变量
            context.setVariable("startTime", System.currentTimeMillis());
            context.setVariable("initialized", true);

            // 3. 🆕 v1.5.1: 设置数据集ID到上下文（用于数据分发阶段）
            if (task.getDatasetId() != null && !task.getDatasetId().trim().isEmpty()) {
                List<String> datasetIds = Arrays.asList(task.getDatasetId());
                context.setVariable("datasetIds", datasetIds);
                log.info("✅ 已设置数据集ID到工作流上下文: datasetIds={}", datasetIds);
            } else {
                log.warn("⚠️ 任务未关联数据集ID，数据分发阶段将使用默认策略");
            }

            // 4. 🆕 v1.5.1: 设置分发策略到上下文
            if (task.getDistributionStrategy() != null) {
                context.setVariable("distributionStrategy", task.getDistributionStrategy());
                log.info("✅ 已设置分发策略到工作流上下文: strategy={}", task.getDistributionStrategy());
            }

            // 5. 🆕 v1.5.1: 设置目标VM列表到上下文
            List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
            if (!participants.isEmpty()) {
                List<String> targetVmIds = participants.stream()
                        .map(TaskParticipant::getVmId)
                        .collect(Collectors.toList());
                context.setVariable("targetVmIds", targetVmIds);
                log.info("✅ 已设置目标VM列表到工作流上下文: vmCount={}, vmIds={}", targetVmIds.size(), targetVmIds);
            } else {
                log.warn("⚠️ 任务没有参与者，数据分发阶段将无法执行");
            }

            // 6. 验证任务配置
            if (taskId == null || taskId.trim().isEmpty()) {
                return StageResult.failure("任务ID不能为空");
            }

            // 7. 设置工作流状态为进行中
            context.getWorkflow().setStatus(context.getWorkflow().getStatus());

            log.info("初始化阶段完成: orchestrationId={}, datasetId={}, participants={}",
                orchestrationId, task.getDatasetId(), participants.size());

            return StageResult.success()
                .addOutput("taskId", taskId)
                .addOutput("initTime", System.currentTimeMillis())
                .addOutput("datasetId", task.getDatasetId())
                .addOutput("participantCount", participants.size());

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