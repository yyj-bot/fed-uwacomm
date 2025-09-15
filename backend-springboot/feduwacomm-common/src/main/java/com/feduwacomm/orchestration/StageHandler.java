package com.feduwacomm.orchestration;

/**
 * 工作流阶段处理器接口
 */
public interface StageHandler {

    /**
     * 执行阶段逻辑
     *
     * @param context 工作流上下文
     * @return 执行结果
     */
    StageResult execute(WorkflowContext context);

    /**
     * 获取支持的阶段
     *
     * @return 支持的工作流阶段
     */
    WorkflowStage getSupportedStage();

    /**
     * 验证阶段输入
     *
     * @param context 工作流上下文
     * @return 验证是否通过
     */
    default boolean validateInput(WorkflowContext context) {
        return true;
    }

    /**
     * 清理阶段资源
     *
     * @param context 工作流上下文
     */
    default void cleanup(WorkflowContext context) {
        // 默认空实现
    }
}