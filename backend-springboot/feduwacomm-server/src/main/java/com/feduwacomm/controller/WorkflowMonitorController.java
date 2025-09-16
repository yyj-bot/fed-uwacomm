package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import com.feduwacomm.entity.OrchestrationWorkflow;
import com.feduwacomm.entity.WorkflowStageExecution;
import com.feduwacomm.mapper.OrchestrationWorkflowMapper;
import com.feduwacomm.mapper.WorkflowStageExecutionMapper;
import com.feduwacomm.service.FederatedOrchestrationService;
import com.feduwacomm.vo.WorkflowStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流监控控制器
 * 提供任务状态监控、工作流进度查询等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/monitor")
@RequiredArgsConstructor
public class WorkflowMonitorController {

    private final FederatedOrchestrationService orchestrationService;
    private final OrchestrationWorkflowMapper orchestrationMapper;
    private final WorkflowStageExecutionMapper stageExecutionMapper;

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESEARCHER') or hasRole('OPERATOR')")
    public Result<WorkflowStatusVO> getWorkflowStatusByTaskId(@PathVariable String taskId) {
        
        log.info("获取任务工作流状态: taskId={}", taskId);
        
        try {
            OrchestrationWorkflow workflow = orchestrationService.getWorkflowByTaskId(taskId);
            if (workflow == null) {
                return Result.error("未找到该任务的工作流");
            }
            
            WorkflowStatusVO statusVO = buildWorkflowStatusVO(workflow);
            
            log.info("获取工作流状态成功: taskId={}, status={}, stage={}", 
                taskId, workflow.getStatus(), workflow.getCurrentStage());
            
            return Result.success(statusVO);
            
        } catch (Exception e) {
            log.error("获取工作流状态失败: taskId={}", taskId, e);
            return Result.error("获取工作流状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/{orchestrationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESEARCHER') or hasRole('OPERATOR')")
    public Result<WorkflowStatusVO> getWorkflowStatus(@PathVariable String orchestrationId) {
        
        log.info("获取工作流详细状态: orchestrationId={}", orchestrationId);
        
        try {
            OrchestrationWorkflow workflow = orchestrationMapper.selectById(orchestrationId);
            if (workflow == null) {
                return Result.error("工作流不存在");
            }
            
            WorkflowStatusVO statusVO = buildWorkflowStatusVO(workflow);
            
            return Result.success(statusVO);
            
        } catch (Exception e) {
            log.error("获取工作流状态失败: orchestrationId={}", orchestrationId, e);
            return Result.error("获取工作流状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/{orchestrationId}/stages")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESEARCHER') or hasRole('OPERATOR')")
    public Result<List<WorkflowStageExecution>> getWorkflowStageHistory(@PathVariable String orchestrationId) {
        
        log.info("获取工作流阶段执行历史: orchestrationId={}", orchestrationId);
        
        try {
            List<WorkflowStageExecution> stages = stageExecutionMapper.selectByOrchestrationId(orchestrationId);
            
            return Result.success(stages);
            
        } catch (Exception e) {
            log.error("获取工作流阶段历史失败: orchestrationId={}", orchestrationId, e);
            return Result.error("获取阶段历史失败: " + e.getMessage());
        }
    }

    @GetMapping("/{orchestrationId}/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESEARCHER') or hasRole('OPERATOR')")
    public Result<Map<String, Object>> getWorkflowStats(@PathVariable String orchestrationId) {
        
        log.info("获取工作流统计信息: orchestrationId={}", orchestrationId);
        
        try {
            Map<String, Integer> statusCounts = stageExecutionMapper.countByStatus(orchestrationId);
            List<WorkflowStageExecution> stages = stageExecutionMapper.selectByOrchestrationId(orchestrationId);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("statusCounts", statusCounts);
            stats.put("totalStages", stages.size());
            stats.put("completedStages", statusCounts.getOrDefault("COMPLETED", 0));
            stats.put("failedStages", statusCounts.getOrDefault("FAILED", 0));
            stats.put("inProgressStages", statusCounts.getOrDefault("IN_PROGRESS", 0));
            
            // 计算总体进度百分比
            int totalStages = stages.size();
            int completedStages = statusCounts.getOrDefault("COMPLETED", 0);
            double progressPercent = totalStages > 0 ? (double) completedStages / totalStages * 100 : 0;
            stats.put("progressPercent", Math.round(progressPercent * 100.0) / 100.0);
            
            return Result.success(stats);
            
        } catch (Exception e) {
            log.error("获取工作流统计失败: orchestrationId={}", orchestrationId, e);
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RESEARCHER') and #userId == authentication.name)")
    public Result<List<OrchestrationWorkflow>> getUserWorkflows(@PathVariable String userId) {
        
        log.info("获取用户工作流列表: userId={}", userId);
        
        try {
            List<OrchestrationWorkflow> workflows = orchestrationMapper.selectByCreatedBy(userId);
            
            return Result.success(workflows);
            
        } catch (Exception e) {
            log.error("获取用户工作流列表失败: userId={}", userId, e);
            return Result.error("获取工作流列表失败: " + e.getMessage());
        }
    }

    /**
     * 构建工作流状态VO
     */
    private WorkflowStatusVO buildWorkflowStatusVO(OrchestrationWorkflow workflow) {
        // 获取阶段执行记录
        List<WorkflowStageExecution> stages = stageExecutionMapper.selectByOrchestrationId(workflow.getId());
        
        // 获取最新阶段
        WorkflowStageExecution latestStage = stageExecutionMapper.selectLatestByOrchestrationId(workflow.getId());
        
        // 计算进度
        Map<String, Integer> statusCounts = stageExecutionMapper.countByStatus(workflow.getId());
        int totalStages = stages.size();
        int completedStages = statusCounts.getOrDefault("COMPLETED", 0);
        double progressPercent = totalStages > 0 ? (double) completedStages / totalStages * 100 : 0;
        
        return WorkflowStatusVO.builder()
                .orchestrationId(workflow.getId())
                .taskId(workflow.getTaskId())
                .workflowName(workflow.getWorkflowName())
                .status(workflow.getStatus().name())
                .currentStage(workflow.getCurrentStage().name())
                .currentStageDescription(workflow.getCurrentStage().getDescription())
                .progressPercent(Math.round(progressPercent * 100.0) / 100.0)
                .createdAt(workflow.getCreatedAt())
                .startedAt(workflow.getStartedAt())
                .completedAt(workflow.getCompletedAt())
                .stages(stages)
                .latestStageExecution(latestStage)
                .totalStages(totalStages)
                .completedStages(completedStages)
                .failedStages(statusCounts.getOrDefault("FAILED", 0))
                .inProgressStages(statusCounts.getOrDefault("IN_PROGRESS", 0))
                .build();
    }
}