package com.feduwacomm.orchestration;

import com.feduwacomm.entity.OrchestrationWorkflow;
import com.feduwacomm.entity.WorkflowStageExecution;
import com.feduwacomm.event.FederatedTaskCreatedEvent;
import com.feduwacomm.orchestration.WorkflowContext;
import com.feduwacomm.mapper.OrchestrationWorkflowMapper;
import com.feduwacomm.mapper.WorkflowStageExecutionMapper;
import com.feduwacomm.orchestration.handler.*;
import com.feduwacomm.service.impl.FederatedOrchestrationServiceImpl;
import com.feduwacomm.utils.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 联邦学习工作流编排服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("联邦学习工作流编排服务测试")
class FederatedOrchestrationServiceTest {

    @Mock
    private OrchestrationWorkflowMapper orchestrationMapper;

    @Mock
    private WorkflowStageExecutionMapper stageExecutionMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UuidUtil uuidUtil;

    @Mock
    private InitializationStageHandler initializationHandler;

    @Mock
    private InitialModelGenerationStageHandler modelGenerationHandler;

    @Mock
    private DataDistributionStageHandler dataDistributionHandler;

    @Mock
    private ModelDistributionStageHandler modelDistributionHandler;

    @Mock
    private FederatedTrainingStageHandler trainingHandler;

    @Mock
    private FinalAggregationStageHandler aggregationHandler;

    @InjectMocks
    private FederatedOrchestrationServiceImpl orchestrationService;

    private String testTaskId;
    private String testCreatedBy;
    private String testWorkflowId;

    @BeforeEach
    void setUp() {
        testTaskId = "test-task-001";
        testCreatedBy = "test-user";
        testWorkflowId = "workflow-001";

        // 模拟UUID生成
        lenient().when(uuidUtil.generateUuid()).thenReturn(testWorkflowId);

        // 初始化处理器映射（模拟@PostConstruct）
        ReflectionTestUtils.invokeMethod(orchestrationService, "initStageHandlers");
    }

    @Test
    @DisplayName("成功创建工作流实例")
    void testCreateWorkflow_Success() {
        // Given
        when(orchestrationMapper.insert(any(OrchestrationWorkflow.class))).thenReturn(1);

        // When
        OrchestrationWorkflow result = orchestrationService.createWorkflow(testTaskId, testCreatedBy);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testWorkflowId);
        assertThat(result.getTaskId()).isEqualTo(testTaskId);
        assertThat(result.getCreatedBy()).isEqualTo(testCreatedBy);
        assertThat(result.getStatus()).isEqualTo(OrchestrationWorkflow.WorkflowStatus.CREATED);
        assertThat(result.getCurrentStage()).isEqualTo(WorkflowStage.INITIALIZATION);

        verify(orchestrationMapper).insert(any(OrchestrationWorkflow.class));
    }

    @Test
    @DisplayName("成功处理任务创建事件")
    void testOnTaskCreated_Success() {
        // Given
        FederatedTaskCreatedEvent event = new FederatedTaskCreatedEvent(testTaskId, "测试任务", testCreatedBy);

        lenient().when(orchestrationMapper.insert(any(OrchestrationWorkflow.class))).thenReturn(1);
        lenient().when(orchestrationMapper.update(any(OrchestrationWorkflow.class))).thenReturn(1);
        lenient().when(stageExecutionMapper.insert(any(WorkflowStageExecution.class))).thenReturn(1);
        lenient().when(stageExecutionMapper.update(any(WorkflowStageExecution.class))).thenReturn(1);

        // 模拟初始化阶段处理器返回成功结果
        lenient().when(initializationHandler.getSupportedStage()).thenReturn(WorkflowStage.INITIALIZATION);
        lenient().when(initializationHandler.execute(any(WorkflowContext.class)))
                .thenReturn(StageResult.success("taskId", testTaskId));

        // 模拟下一阶段处理器 - 因为初始化成功会进入下一阶段
        lenient().when(modelGenerationHandler.getSupportedStage()).thenReturn(WorkflowStage.INITIAL_MODEL_GENERATION);
        lenient().when(modelGenerationHandler.execute(any(WorkflowContext.class)))
                .thenReturn(StageResult.success("modelId", "test-model"));

        // When
        orchestrationService.onTaskCreated(event);

        // Then
        verify(orchestrationMapper).insert(any(OrchestrationWorkflow.class));
    }

    @Test
    @DisplayName("根据任务ID获取工作流")
    void testGetWorkflowByTaskId() {
        // Given
        OrchestrationWorkflow mockWorkflow = createMockWorkflow();
        when(orchestrationMapper.selectByTaskId(testTaskId)).thenReturn(mockWorkflow);

        // When
        OrchestrationWorkflow result = orchestrationService.getWorkflowByTaskId(testTaskId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isEqualTo(testTaskId);
        verify(orchestrationMapper).selectByTaskId(testTaskId);
    }

    @Test
    @DisplayName("成功暂停工作流")
    void testPauseWorkflow_Success() {
        // Given
        OrchestrationWorkflow mockWorkflow = createMockWorkflow();
        mockWorkflow.setStatus(OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS);
        
        when(orchestrationMapper.selectById(testWorkflowId)).thenReturn(mockWorkflow);
        when(orchestrationMapper.update(any(OrchestrationWorkflow.class))).thenReturn(1);

        // When
        orchestrationService.pauseWorkflow(testWorkflowId);

        // Then
        verify(orchestrationMapper).selectById(testWorkflowId);
        verify(orchestrationMapper).update(argThat(workflow -> 
            workflow.getStatus() == OrchestrationWorkflow.WorkflowStatus.PAUSED));
    }

    @Test
    @DisplayName("成功恢复工作流")
    void testResumeWorkflow_Success() {
        // Given
        OrchestrationWorkflow mockWorkflow = createMockWorkflow();
        mockWorkflow.setStatus(OrchestrationWorkflow.WorkflowStatus.PAUSED);

        when(orchestrationMapper.selectById(testWorkflowId)).thenReturn(mockWorkflow);
        lenient().when(orchestrationMapper.update(any(OrchestrationWorkflow.class))).thenReturn(1);
        lenient().when(stageExecutionMapper.insert(any(WorkflowStageExecution.class))).thenReturn(1);
        lenient().when(stageExecutionMapper.update(any(WorkflowStageExecution.class))).thenReturn(1);

        // 模拟当前阶段处理器
        lenient().when(initializationHandler.getSupportedStage()).thenReturn(WorkflowStage.INITIALIZATION);
        lenient().when(initializationHandler.execute(any(WorkflowContext.class)))
                .thenReturn(StageResult.success("taskId", testTaskId));

        // When
        orchestrationService.resumeWorkflow(testWorkflowId);

        // Then
        verify(orchestrationMapper).selectById(testWorkflowId);
    }

    @Test
    @DisplayName("成功终止工作流")
    void testTerminateWorkflow_Success() {
        // Given
        OrchestrationWorkflow mockWorkflow = createMockWorkflow();
        when(orchestrationMapper.selectById(testWorkflowId)).thenReturn(mockWorkflow);
        when(orchestrationMapper.update(any(OrchestrationWorkflow.class))).thenReturn(1);

        // When
        orchestrationService.terminateWorkflow(testWorkflowId);

        // Then
        verify(orchestrationMapper).selectById(testWorkflowId);
        verify(orchestrationMapper).update(argThat(workflow -> 
            workflow.getStatus() == OrchestrationWorkflow.WorkflowStatus.TERMINATED));
    }

    private OrchestrationWorkflow createMockWorkflow() {
        return OrchestrationWorkflow.builder()
                .id(testWorkflowId)
                .taskId(testTaskId)
                .workflowName("测试工作流")
                .status(OrchestrationWorkflow.WorkflowStatus.CREATED)
                .currentStage(WorkflowStage.INITIALIZATION)
                .config("{}")
                .createdAt(LocalDateTime.now())
                .createdBy(testCreatedBy)
                .build();
    }
}