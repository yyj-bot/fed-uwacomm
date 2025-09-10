package com.feduwacomm.orchestration;

import com.feduwacomm.entity.OrchestrationWorkflow;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流执行上下文
 */
@Data
public class WorkflowContext {

    /**
     * 工作流实例
     */
    private OrchestrationWorkflow workflow;

    /**
     * 上下文变量
     */
    private Map<String, Object> variables;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 是否需要暂停
     */
    private boolean pauseRequested;

    /**
     * 是否需要终止
     */
    private boolean terminateRequested;

    public WorkflowContext(OrchestrationWorkflow workflow) {
        this.workflow = workflow;
        this.variables = new HashMap<>();
    }

    /**
     * 获取工作流ID
     */
    public String getOrchestrationId() {
        return workflow != null ? workflow.getId() : null;
    }

    /**
     * 获取当前阶段
     */
    public WorkflowStage getCurrentStage() {
        return workflow != null ? workflow.getCurrentStage() : null;
    }

    /**
     * 设置当前阶段
     */
    public void setCurrentStage(WorkflowStage stage) {
        if (workflow != null) {
            workflow.setCurrentStage(stage);
        }
    }

    /**
     * 获取任务ID
     */
    public String getTaskId() {
        return workflow != null ? workflow.getTaskId() : null;
    }

    /**
     * 获取上下文变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 设置上下文变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 移除上下文变量
     */
    public void removeVariable(String key) {
        variables.remove(key);
    }

    /**
     * 检查是否包含变量
     */
    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }

    /**
     * 清空所有变量
     */
    public void clearVariables() {
        variables.clear();
    }
}