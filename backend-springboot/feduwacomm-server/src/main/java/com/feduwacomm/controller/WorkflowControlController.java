package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import com.feduwacomm.entity.OrchestrationWorkflow;
import com.feduwacomm.service.FederatedOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 工作流控制控制器
 * 提供任务暂停、恢复、终止等控制功能
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/control")
@RequiredArgsConstructor
public class WorkflowControlController {

    private final FederatedOrchestrationService orchestrationService;

    @PostMapping("/task/{taskId}/pause")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESEARCHER')")
    public Result<String> pauseWorkflowByTaskId(@PathVariable String taskId) {
        
        log.info("根据任务ID暂停工作流: taskId={}", taskId);
        
        try {
            OrchestrationWorkflow workflow = orchestrationService.getWorkflowByTaskId(taskId);
            if (workflow == null) {
                return Result.error("未找到该任务的工作流");
            }
            
            if (workflow.getStatus() != OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS) {
                return Result.error("只能暂停正在执行的工作流");
            }
            
            orchestrationService.pauseWorkflow(workflow.getId());
            
            log.info("工作流暂停成功: taskId={}, orchestrationId={}", taskId, workflow.getId());
            
            return Result.success("工作流暂停成功");
            
        } catch (Exception e) {
            log.error("暂停工作流失败: taskId={}", taskId, e);
            return Result.error("暂停工作流失败: " + e.getMessage());
        }
    }

    @PostMapping("/task/{taskId}/resume")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESEARCHER')")
    public Result<String> resumeWorkflowByTaskId(@PathVariable String taskId) {
        
        log.info("根据任务ID恢复工作流: taskId={}", taskId);
        
        try {
            OrchestrationWorkflow workflow = orchestrationService.getWorkflowByTaskId(taskId);
            if (workflow == null) {
                return Result.error("未找到该任务的工作流");
            }
            
            if (workflow.getStatus() != OrchestrationWorkflow.WorkflowStatus.PAUSED) {
                return Result.error("只能恢复已暂停的工作流");
            }
            
            orchestrationService.resumeWorkflow(workflow.getId());
            
            log.info("工作流恢复成功: taskId={}, orchestrationId={}", taskId, workflow.getId());
            
            return Result.success("工作流恢复成功");
            
        } catch (Exception e) {
            log.error("恢复工作流失败: taskId={}", taskId, e);
            return Result.error("恢复工作流失败: " + e.getMessage());
        }
    }

    @PostMapping("/task/{taskId}/terminate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESEARCHER')")
    public Result<String> terminateWorkflowByTaskId(@PathVariable String taskId) {
        
        log.info("根据任务ID终止工作流: taskId={}", taskId);
        
        try {
            OrchestrationWorkflow workflow = orchestrationService.getWorkflowByTaskId(taskId);
            if (workflow == null) {
                return Result.error("未找到该任务的工作流");
            }
            
            if (workflow.getStatus() == OrchestrationWorkflow.WorkflowStatus.COMPLETED ||
                workflow.getStatus() == OrchestrationWorkflow.WorkflowStatus.TERMINATED) {
                return Result.error("工作流已完成或已终止，无需再次终止");
            }
            
            orchestrationService.terminateWorkflow(workflow.getId());
            
            log.info("工作流终止成功: taskId={}, orchestrationId={}", taskId, workflow.getId());
            
            return Result.success("工作流终止成功");
            
        } catch (Exception e) {
            log.error("终止工作流失败: taskId={}", taskId, e);
            return Result.error("终止工作流失败: " + e.getMessage());
        }
    }

    @PostMapping("/{orchestrationId}/pause")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESEARCHER')")
    public Result<String> pauseWorkflow(@PathVariable String orchestrationId) {
        
        log.info("暂停工作流: orchestrationId={}", orchestrationId);
        
        try {
            orchestrationService.pauseWorkflow(orchestrationId);
            
            log.info("工作流暂停成功: orchestrationId={}", orchestrationId);
            
            return Result.success("工作流暂停成功");
            
        } catch (Exception e) {
            log.error("暂停工作流失败: orchestrationId={}", orchestrationId, e);
            return Result.error("暂停工作流失败: " + e.getMessage());
        }
    }

    @PostMapping("/{orchestrationId}/resume")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESEARCHER')")
    public Result<String> resumeWorkflow(@PathVariable String orchestrationId) {
        
        log.info("恢复工作流: orchestrationId={}", orchestrationId);
        
        try {
            orchestrationService.resumeWorkflow(orchestrationId);
            
            log.info("工作流恢复成功: orchestrationId={}", orchestrationId);
            
            return Result.success("工作流恢复成功");
            
        } catch (Exception e) {
            log.error("恢复工作流失败: orchestrationId={}", orchestrationId, e);
            return Result.error("恢复工作流失败: " + e.getMessage());
        }
    }

    @PostMapping("/{orchestrationId}/terminate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> terminateWorkflow(@PathVariable String orchestrationId) {
        
        log.info("终止工作流: orchestrationId={}", orchestrationId);
        
        try {
            orchestrationService.terminateWorkflow(orchestrationId);
            
            log.info("工作流终止成功: orchestrationId={}", orchestrationId);
            
            return Result.success("工作流终止成功");
            
        } catch (Exception e) {
            log.error("终止工作流失败: orchestrationId={}", orchestrationId, e);
            return Result.error("终止工作流失败: " + e.getMessage());
        }
    }
}