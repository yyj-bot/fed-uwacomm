package com.feduwacomm.service.impl;

import com.feduwacomm.entity.OrchestrationWorkflow;
import com.feduwacomm.entity.WorkflowStageExecution;
import com.feduwacomm.mapper.OrchestrationWorkflowMapper;
import com.feduwacomm.mapper.WorkflowStageExecutionMapper;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.orchestration.WorkflowStage;
import com.feduwacomm.service.FederatedOrchestrationService;
import com.feduwacomm.service.WorkflowStageTransitionService;
import com.feduwacomm.utils.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流阶段转换服务实现
 * 提供工作流阶段之间的转换逻辑和状态管理
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@Service
public class WorkflowStageTransitionServiceImpl implements WorkflowStageTransitionService {

    @Autowired
    private OrchestrationWorkflowMapper orchestrationMapper;

    @Autowired
    private WorkflowStageExecutionMapper stageExecutionMapper;

    @Autowired
    private FederatedOrchestrationService orchestrationService;

    @Autowired
    private UuidUtil uuidUtil;

    @Override
    @Transactional
    public boolean triggerNextStage(String orchestrationId, WorkflowStage currentStage,
                                   Map<String, Object> context) {
        if (orchestrationId == null || currentStage == null) {
            log.error("触发下一阶段参数无效: orchestrationId={}, currentStage={}",
                     orchestrationId, currentStage);
            return false;
        }

        try {
            // 获取工作流实例
            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow == null) {
                log.error("工作流不存在: orchestrationId={}", orchestrationId);
                return false;
            }

            // 检查工作流状态
            if (workflow.getStatus() != OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS) {
                log.warn("工作流状态不允许触发下一阶段: orchestrationId={}, status={}",
                        orchestrationId, workflow.getStatus());
                return false;
            }

            // 获取下一阶段
            WorkflowStage nextStage = currentStage.getNext();
            if (nextStage == currentStage) {
                log.info("当前阶段已是最后阶段，工作流将完成: orchestrationId={}, stage={}",
                        orchestrationId, currentStage);

                // 标记工作流为完成状态
                workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.COMPLETED);
                workflow.setCompletedAt(LocalDateTime.now());
                orchestrationMapper.update(workflow);
                return true;
            }

            // 更新工作流当前阶段
            workflow.setCurrentStage(nextStage);
            workflow.setUpdatedAt(LocalDateTime.now());
            orchestrationMapper.update(workflow);

            // 创建下一阶段的执行记录
            createStageExecution(orchestrationId, nextStage, context);

            log.info("成功触发工作流下一阶段: orchestrationId={}, from={}, to={}",
                    orchestrationId, currentStage, nextStage);

            // 这里可以异步触发下一阶段的具体执行逻辑
            // 但为了保持现有架构，我们只更新状态，具体执行由编排服务处理
            return true;

        } catch (Exception e) {
            log.error("触发工作流下一阶段失败: orchestrationId={}, currentStage={}",
                     orchestrationId, currentStage, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateStageStatus(String orchestrationId, WorkflowStage stage,
                                   String status, String message) {
        if (orchestrationId == null || stage == null || status == null) {
            log.error("更新阶段状态参数无效: orchestrationId={}, stage={}, status={}",
                     orchestrationId, stage, status);
            return false;
        }

        try {
            // 查找对应的阶段执行记录
            WorkflowStageExecution stageExecution = stageExecutionMapper
                .selectByOrchestrationIdAndStage(orchestrationId, stage.name());

            if (stageExecution == null) {
                log.warn("未找到阶段执行记录，创建新记录: orchestrationId={}, stage={}",
                        orchestrationId, stage);
                stageExecution = createStageExecution(orchestrationId, stage, null);
            }

            // 更新阶段状态
            WorkflowStageExecution.StageExecutionStatus executionStatus;
            try {
                executionStatus = WorkflowStageExecution.StageExecutionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("无效的阶段状态，使用默认状态: status={}", status);
                executionStatus = WorkflowStageExecution.StageExecutionStatus.IN_PROGRESS;
            }

            stageExecution.setStatus(executionStatus);
            stageExecution.setErrorMessage(message);
            stageExecution.setUpdatedAt(LocalDateTime.now());

            if (executionStatus == WorkflowStageExecution.StageExecutionStatus.IN_PROGRESS &&
                stageExecution.getStartedAt() == null) {
                stageExecution.setStartedAt(LocalDateTime.now());
            } else if (executionStatus == WorkflowStageExecution.StageExecutionStatus.COMPLETED ||
                      executionStatus == WorkflowStageExecution.StageExecutionStatus.FAILED) {
                stageExecution.setCompletedAt(LocalDateTime.now());
            }

            stageExecutionMapper.update(stageExecution);

            log.info("成功更新阶段状态: orchestrationId={}, stage={}, status={}, message={}",
                    orchestrationId, stage, status, message);
            return true;

        } catch (Exception e) {
            log.error("更新阶段状态失败: orchestrationId={}, stage={}, status={}",
                     orchestrationId, stage, status, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean handleWorkflowException(String orchestrationId, WorkflowStage currentStage,
                                         String errorMessage, Exception exception) {
        if (orchestrationId == null) {
            log.error("处理工作流异常参数无效: orchestrationId is null");
            return false;
        }

        try {
            // 获取工作流实例
            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow == null) {
                log.error("工作流不存在: orchestrationId={}", orchestrationId);
                return false;
            }

            // 更新工作流状态为失败
            workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.FAILED);
            workflow.setCompletedAt(LocalDateTime.now());
            workflow.setUpdatedAt(LocalDateTime.now());
            orchestrationMapper.update(workflow);

            // 更新当前阶段状态为失败
            if (currentStage != null) {
                updateStageStatus(orchestrationId, currentStage, "FAILED", errorMessage);
            }

            // 记录详细的异常信息
            String fullErrorMessage = errorMessage;
            if (exception != null) {
                fullErrorMessage += " - 异常详情: " + exception.getMessage();
            }

            log.error("工作流异常处理完成: orchestrationId={}, currentStage={}, error={}",
                     orchestrationId, currentStage, fullErrorMessage, exception);

            return true;

        } catch (Exception e) {
            log.error("处理工作流异常失败: orchestrationId={}, currentStage={}",
                     orchestrationId, currentStage, e);
            return false;
        }
    }

    @Override
    public boolean canContinueWorkflow(String orchestrationId) {
        if (orchestrationId == null) {
            return false;
        }

        try {
            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow == null) {
                log.warn("工作流不存在: orchestrationId={}", orchestrationId);
                return false;
            }

            OrchestrationWorkflow.WorkflowStatus status = workflow.getStatus();
            boolean canContinue = status == OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS ||
                                 status == OrchestrationWorkflow.WorkflowStatus.PAUSED;

            log.debug("检查工作流是否可继续: orchestrationId={}, status={}, canContinue={}",
                     orchestrationId, status, canContinue);

            return canContinue;

        } catch (Exception e) {
            log.error("检查工作流状态失败: orchestrationId={}", orchestrationId, e);
            return false;
        }
    }

    @Override
    public WorkflowStage getCurrentStage(String orchestrationId) {
        if (orchestrationId == null) {
            return null;
        }

        try {
            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow == null) {
                log.warn("工作流不存在: orchestrationId={}", orchestrationId);
                return null;
            }

            return workflow.getCurrentStage();

        } catch (Exception e) {
            log.error("获取工作流当前阶段失败: orchestrationId={}", orchestrationId, e);
            return null;
        }
    }

    @Override
    @Transactional
    public boolean pauseWorkflow(String orchestrationId, String reason) {
        if (orchestrationId == null) {
            log.error("暂停工作流参数无效: orchestrationId is null");
            return false;
        }

        try {
            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow == null) {
                log.error("工作流不存在: orchestrationId={}", orchestrationId);
                return false;
            }

            if (workflow.getStatus() != OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS) {
                log.warn("工作流状态不允许暂停: orchestrationId={}, status={}",
                        orchestrationId, workflow.getStatus());
                return false;
            }

            workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.PAUSED);
            workflow.setUpdatedAt(LocalDateTime.now());
            orchestrationMapper.update(workflow);

            log.info("工作流已暂停: orchestrationId={}, reason={}", orchestrationId, reason);
            return true;

        } catch (Exception e) {
            log.error("暂停工作流失败: orchestrationId={}, reason={}", orchestrationId, reason, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean resumeWorkflow(String orchestrationId) {
        if (orchestrationId == null) {
            log.error("恢复工作流参数无效: orchestrationId is null");
            return false;
        }

        try {
            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow == null) {
                log.error("工作流不存在: orchestrationId={}", orchestrationId);
                return false;
            }

            if (workflow.getStatus() != OrchestrationWorkflow.WorkflowStatus.PAUSED) {
                log.warn("工作流状态不允许恢复: orchestrationId={}, status={}",
                        orchestrationId, workflow.getStatus());
                return false;
            }

            workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS);
            workflow.setUpdatedAt(LocalDateTime.now());
            orchestrationMapper.update(workflow);

            log.info("工作流已恢复: orchestrationId={}", orchestrationId);
            return true;

        } catch (Exception e) {
            log.error("恢复工作流失败: orchestrationId={}", orchestrationId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean terminateWorkflow(String orchestrationId, String reason) {
        if (orchestrationId == null) {
            log.error("终止工作流参数无效: orchestrationId is null");
            return false;
        }

        try {
            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow == null) {
                log.error("工作流不存在: orchestrationId={}", orchestrationId);
                return false;
            }

            workflow.setStatus(OrchestrationWorkflow.WorkflowStatus.TERMINATED);
            workflow.setCompletedAt(LocalDateTime.now());
            workflow.setUpdatedAt(LocalDateTime.now());
            orchestrationMapper.update(workflow);

            log.info("工作流已终止: orchestrationId={}, reason={}", orchestrationId, reason);
            return true;

        } catch (Exception e) {
            log.error("终止工作流失败: orchestrationId={}, reason={}", orchestrationId, reason, e);
            return false;
        }
    }

    /**
     * 创建阶段执行记录
     */
    private WorkflowStageExecution createStageExecution(String orchestrationId, WorkflowStage stage,
                                                       Map<String, Object> context) {
        WorkflowStageExecution execution = WorkflowStageExecution.builder()
                .id(uuidUtil.generateUuid())
                .orchestrationId(orchestrationId)
                .stageName(stage.name())
                .status(WorkflowStageExecution.StageExecutionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // 如果有上下文数据，序列化保存
        if (context != null && !context.isEmpty()) {
            try {
                execution.setInputData(serializeContext(context));
            } catch (Exception e) {
                log.warn("序列化上下文数据失败: orchestrationId={}, stage={}",
                        orchestrationId, stage, e);
            }
        }

        stageExecutionMapper.insert(execution);
        log.debug("创建阶段执行记录: orchestrationId={}, stage={}, executionId={}",
                 orchestrationId, stage, execution.getId());

        return execution;
    }

    /**
     * 简单的上下文序列化
     */
    private String serializeContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }

        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value != null ? value.toString() : "null");
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}