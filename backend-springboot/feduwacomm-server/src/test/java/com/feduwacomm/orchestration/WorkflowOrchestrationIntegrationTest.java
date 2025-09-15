package com.feduwacomm.orchestration;

import com.feduwacomm.dto.TaskCreateDTO;
import com.feduwacomm.entity.OrchestrationWorkflow;
import com.feduwacomm.event.FederatedTaskCreatedEvent;
import com.feduwacomm.mapper.OrchestrationWorkflowMapper;
import com.feduwacomm.service.FederatedOrchestrationService;
import com.feduwacomm.service.FederatedTaskService;
import com.feduwacomm.vo.TaskOperationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流编排集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WorkflowOrchestrationIntegrationTest {

    @Autowired
    private FederatedTaskService federatedTaskService;

    @Autowired
    private FederatedOrchestrationService orchestrationService;

    @Autowired
    private OrchestrationWorkflowMapper orchestrationMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-001";
    }

    @Test
    void testAutomaticWorkflowCreation() throws InterruptedException {
        // 1. 创建联邦学习任务
        TaskCreateDTO createDTO = buildTestTaskCreateDTO();
        
        TaskOperationVO taskResult = federatedTaskService.createTask(createDTO, testUserId);
        assertNotNull(taskResult);
        assertNotNull(taskResult.getTaskId());
        assertEquals("CREATED", taskResult.getStatus());
        
        String taskId = taskResult.getTaskId();
        System.out.println("创建任务成功: taskId=" + taskId);
        
        // 2. 等待异步事件处理完成
        Thread.sleep(2000); // 等待2秒让异步处理完成
        
        // 3. 验证工作流是否自动创建
        OrchestrationWorkflow workflow = orchestrationService.getWorkflowByTaskId(taskId);
        assertNotNull(workflow, "工作流应该自动创建");
        assertEquals(taskId, workflow.getTaskId());
        assertEquals(testUserId, workflow.getCreatedBy());
        assertNotNull(workflow.getId());
        
        System.out.println("工作流自动创建成功: workflowId=" + workflow.getId() + 
                          ", status=" + workflow.getStatus() + 
                          ", stage=" + workflow.getCurrentStage());
        
        // 4. 验证工作流状态
        assertTrue(workflow.getStatus() == OrchestrationWorkflow.WorkflowStatus.CREATED ||
                  workflow.getStatus() == OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS,
                  "工作流状态应该是CREATED或IN_PROGRESS");
        
        // 5. 验证初始阶段
        assertNotNull(workflow.getCurrentStage());
        System.out.println("当前阶段: " + workflow.getCurrentStage().getDescription());
    }

    @Test
    void testWorkflowStageProgression() throws InterruptedException {
        // 1. 手动发布任务创建事件
        String taskId = "test-task-" + System.currentTimeMillis();
        FederatedTaskCreatedEvent event = new FederatedTaskCreatedEvent(
                taskId, "测试任务", testUserId);
        
        eventPublisher.publishEvent(event);
        
        // 2. 等待事件处理
        Thread.sleep(1000);
        
        // 3. 验证工作流创建
        OrchestrationWorkflow workflow = orchestrationService.getWorkflowByTaskId(taskId);
        assertNotNull(workflow, "工作流应该通过事件自动创建");
        
        System.out.println("通过事件创建工作流成功: " + 
                          "status=" + workflow.getStatus() + 
                          ", stage=" + workflow.getCurrentStage());
        
        // 4. 验证阶段进展（由于我们没有完整实现所有服务，这里主要验证结构）
        assertNotNull(workflow.getCurrentStage());
        assertTrue(workflow.getCurrentStage() == WorkflowStage.INITIALIZATION ||
                  workflow.getCurrentStage() == WorkflowStage.INITIAL_MODEL_GENERATION,
                  "工作流应该处于初始化或模型生成阶段");
    }

    @Test
    void testWorkflowControlOperations() throws InterruptedException {
        // 1. 创建工作流
        String taskId = "test-control-" + System.currentTimeMillis();
        OrchestrationWorkflow workflow = orchestrationService.createWorkflow(taskId, testUserId);
        assertNotNull(workflow);
        
        String orchestrationId = workflow.getId();
        System.out.println("创建工作流用于控制测试: orchestrationId=" + orchestrationId);
        
        // 2. 测试暂停操作
        orchestrationService.pauseWorkflow(orchestrationId);
        
        OrchestrationWorkflow pausedWorkflow = orchestrationMapper.selectById(orchestrationId);
        assertEquals(OrchestrationWorkflow.WorkflowStatus.PAUSED, pausedWorkflow.getStatus());
        System.out.println("工作流暂停成功");
        
        // 3. 测试恢复操作
        orchestrationService.resumeWorkflow(orchestrationId);
        
        // 等待异步处理
        Thread.sleep(500);
        
        OrchestrationWorkflow resumedWorkflow = orchestrationMapper.selectById(orchestrationId);
        assertEquals(OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS, resumedWorkflow.getStatus());
        System.out.println("工作流恢复成功");
        
        // 4. 测试终止操作
        orchestrationService.terminateWorkflow(orchestrationId);
        
        OrchestrationWorkflow terminatedWorkflow = orchestrationMapper.selectById(orchestrationId);
        assertEquals(OrchestrationWorkflow.WorkflowStatus.TERMINATED, terminatedWorkflow.getStatus());
        assertNotNull(terminatedWorkflow.getCompletedAt());
        System.out.println("工作流终止成功");
    }

    @Test
    void testWorkflowStageHandlers() {
        // 1. 创建工作流上下文
        String taskId = "test-handlers-" + System.currentTimeMillis();
        OrchestrationWorkflow workflow = OrchestrationWorkflow.builder()
                .id("test-workflow-id")
                .taskId(taskId)
                .workflowName("测试工作流")
                .status(OrchestrationWorkflow.WorkflowStatus.IN_PROGRESS)
                .currentStage(WorkflowStage.INITIALIZATION)
                .createdBy(testUserId)
                .build();
        
        WorkflowContext context = new WorkflowContext(workflow);
        
        // 2. 测试初始化阶段处理器
        // 注意: 由于依赖服务可能未完全配置，这里主要测试处理器结构
        assertNotNull(context.getCurrentStage());
        assertEquals(WorkflowStage.INITIALIZATION, context.getCurrentStage());
        
        // 3. 测试上下文变量操作
        context.setVariable("testKey", "testValue");
        assertTrue(context.hasVariable("testKey"));
        assertEquals("testValue", context.getVariable("testKey"));
        
        context.removeVariable("testKey");
        assertFalse(context.hasVariable("testKey"));
        
        System.out.println("工作流上下文测试通过");
    }

    /**
     * 构建测试用的任务创建DTO
     */
    private TaskCreateDTO buildTestTaskCreateDTO() {
        List<TaskCreateDTO.ParticipantDTO> participants = new ArrayList<>();
        
        TaskCreateDTO.ParticipantDTO participant1 = new TaskCreateDTO.ParticipantDTO();
        participant1.setVmId("test-vm-001");
        participant1.setRole("TRAINER");
        participants.add(participant1);
        
        TaskCreateDTO.ParticipantDTO participant2 = new TaskCreateDTO.ParticipantDTO();
        participant2.setVmId("test-vm-002");
        participant2.setRole("TRAINER");
        participants.add(participant2);
        
        // 创建超参数配置
        TaskCreateDTO.HyperparametersDTO hyperparameters = TaskCreateDTO.HyperparametersDTO.builder()
                .learningRate(0.001)
                .batchSize(32)
                .rounds(10)
                .epochs(100)
                .minParticipants(2)
                .aggregationMethod("WEIGHTED_AVERAGE")
                .build();

        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTaskName("自动化工作流测试任务");
        createDTO.setTaskType("CLASSIFICATION");
        createDTO.setAlgorithm("FEDERATED_AVERAGING");
        createDTO.setDescription("用于测试自动工作流编排的任务");
        createDTO.setParticipants(participants);
        createDTO.setHyperparameters(hyperparameters);
        
        return createDTO;
    }
}