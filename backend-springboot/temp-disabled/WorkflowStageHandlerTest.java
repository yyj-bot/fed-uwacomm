package com.feduwacomm.orchestration;

import com.feduwacomm.entity.OrchestrationWorkflow;
import com.feduwacomm.orchestration.handler.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * 工作流阶段处理器功能测试
 */
@DisplayName("工作流阶段处理器测试")
class WorkflowStageHandlerTest {

    private WorkflowContext context;
    private OrchestrationWorkflow workflow;

    @BeforeEach
    void setUp() {
        workflow = OrchestrationWorkflow.builder()
                .id("test-workflow-001")
                .taskId("test-task-001")
                .workflowName("测试工作流")
                .status(OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS)
                .currentStage(WorkflowStage.INITIALIZATION)
                .config("{}")
                .createdAt(LocalDateTime.now())
                .createdBy("test-user")
                .build();
                
        context = new WorkflowContext(workflow);
    }

    @Test
    @DisplayName("初始化阶段处理器执行成功")
    void testInitializationStageHandler_Success() {
        // Given
        InitializationStageHandler handler = new InitializationStageHandler();
        
        // When
        StageResult result = handler.execute(context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput("taskId")).isEqualTo("test-task-001");
        assertThat(context.getVariable("initialized")).isEqualTo(true);
    }

    @Test
    @DisplayName("初始模型生成阶段处理器执行成功")
    void testInitialModelGenerationStageHandler_Success() {
        // Given
        InitialModelGenerationStageHandler handler = new InitialModelGenerationStageHandler();
        workflow.setCurrentStage(WorkflowStage.INITIAL_MODEL_GENERATION);
        
        // When
        StageResult result = handler.execute(context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput("modelId")).isNotNull();
        assertThat(result.getOutput("modelType")).isEqualTo("NEURAL_NETWORK");
        assertThat(context.getVariable("initialModelId")).isNotNull();
    }

    @Test
    @DisplayName("数据分发阶段处理器执行成功")
    void testDataDistributionStageHandler_Success() {
        // Given
        DataDistributionStageHandler handler = new DataDistributionStageHandler();
        workflow.setCurrentStage(WorkflowStage.DATA_DISTRIBUTION);
        context.setVariable("initialModelId", "model-123");
        
        // When
        StageResult result = handler.execute(context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput("distributionId")).isNotNull();
        assertThat(result.getOutput("strategy")).isEqualTo("BALANCED");
        assertThat(context.getVariable("distributionTaskId")).isNotNull();
    }

    @Test
    @DisplayName("模型分发阶段处理器执行成功")
    void testModelDistributionStageHandler_Success() {
        // Given
        ModelDistributionStageHandler handler = new ModelDistributionStageHandler();
        workflow.setCurrentStage(WorkflowStage.MODEL_DISTRIBUTION);
        context.setVariable("initialModelId", "model-123");
        context.setVariable("distributionTaskId", "dist-123");
        
        // When
        StageResult result = handler.execute(context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput("modelDistributionId")).isNotNull();
        assertThat(result.getOutput("targetVmCount")).isEqualTo(3);
        assertThat(context.getVariable("modelDistributionId")).isNotNull();
    }

    @Test
    @DisplayName("联邦训练阶段处理器执行成功")
    void testFederatedTrainingStageHandler_Success() {
        // Given
        FederatedTrainingStageHandler handler = new FederatedTrainingStageHandler();
        workflow.setCurrentStage(WorkflowStage.FEDERATED_TRAINING);
        context.setVariable("modelDistributionId", "model-dist-123");
        context.setVariable("distributionTaskId", "dist-123");
        
        // When
        StageResult result = handler.execute(context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput("trainingTaskId")).isNotNull();
        assertThat(result.getOutput("totalRounds")).isEqualTo(10);
        assertThat(result.getOutput("completedRounds")).isEqualTo(10);
        assertThat(context.getVariable("federatedTrainingId")).isNotNull();
    }

    @Test
    @DisplayName("最终聚合阶段处理器执行成功")
    void testFinalAggregationStageHandler_Success() {
        // Given
        FinalAggregationStageHandler handler = new FinalAggregationStageHandler();
        workflow.setCurrentStage(WorkflowStage.FINAL_AGGREGATION);
        context.setVariable("federatedTrainingId", "training-123");
        context.setVariable("completedRounds", 10);
        
        // When
        StageResult result = handler.execute(context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput("finalModelId")).isNotNull();
        assertThat(result.getOutput("finalAccuracy")).isEqualTo(0.87);
        assertThat(result.getOutput("aggregationMethod")).isEqualTo("WEIGHTED_AVERAGE");
        assertThat(context.getVariable("finalModelId")).isNotNull();
    }

    @Test
    @DisplayName("工作流阶段状态机转换测试")
    void testWorkflowStageTransitions() {
        // 测试阶段转换逻辑
        assertThat(WorkflowStage.INITIALIZATION.getNext()).isEqualTo(WorkflowStage.INITIAL_MODEL_GENERATION);
        assertThat(WorkflowStage.INITIAL_MODEL_GENERATION.getNext()).isEqualTo(WorkflowStage.DATA_DISTRIBUTION);
        assertThat(WorkflowStage.DATA_DISTRIBUTION.getNext()).isEqualTo(WorkflowStage.MODEL_DISTRIBUTION);
        assertThat(WorkflowStage.MODEL_DISTRIBUTION.getNext()).isEqualTo(WorkflowStage.FEDERATED_TRAINING);
        assertThat(WorkflowStage.FEDERATED_TRAINING.getNext()).isEqualTo(WorkflowStage.FINAL_AGGREGATION);
        assertThat(WorkflowStage.FINAL_AGGREGATION.getNext()).isEqualTo(WorkflowStage.COMPLETED);
        assertThat(WorkflowStage.COMPLETED.getNext()).isEqualTo(WorkflowStage.COMPLETED); // 最终状态
        
        // 测试转换验证
        assertThat(WorkflowStage.INITIALIZATION.canTransitionTo(WorkflowStage.INITIAL_MODEL_GENERATION)).isTrue();
        assertThat(WorkflowStage.INITIALIZATION.canTransitionTo(WorkflowStage.DATA_DISTRIBUTION)).isFalse();
        
        // 测试最终状态检查
        assertThat(WorkflowStage.COMPLETED.isFinal()).isTrue();
        assertThat(WorkflowStage.INITIALIZATION.isFinal()).isFalse();
    }

    @Test
    @DisplayName("工作流上下文变量管理测试")
    void testWorkflowContextVariables() {
        // 测试变量设置和获取
        context.setVariable("testKey", "testValue");
        assertThat(context.getVariable("testKey")).isEqualTo("testValue");
        assertThat(context.hasVariable("testKey")).isTrue();
        
        // 测试变量删除
        context.removeVariable("testKey");
        assertThat(context.hasVariable("testKey")).isFalse();
        assertThat(context.getVariable("testKey")).isNull();
        
        // 测试清空所有变量
        context.setVariable("key1", "value1");
        context.setVariable("key2", "value2");
        context.clearVariables();
        assertThat(context.hasVariable("key1")).isFalse();
        assertThat(context.hasVariable("key2")).isFalse();
    }
}