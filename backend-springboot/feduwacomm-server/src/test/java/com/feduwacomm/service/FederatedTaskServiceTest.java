package com.feduwacomm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.enums.FederatedAlgorithm;
import com.feduwacomm.enums.FederatedTaskStatus;
import com.feduwacomm.enums.ParticipantRole;
import com.feduwacomm.enums.ParticipantStatus;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.service.impl.FederatedTaskServiceImpl;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.utils.MessageBuilder;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 联邦任务服务测试类
 * 测试任务生命周期管理、参与者管理等核心功能
 */
@ExtendWith(MockitoExtension.class)
public class FederatedTaskServiceTest {

    @Mock
    private FederatedTasksMapper tasksMapper;

    @Mock
    private LogService logService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private VmInstanceService vmInstanceService;

    @Mock
    private UuidUtil uuidUtil;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private FederatedTaskServiceImpl federatedTaskService;

    private TaskCreateDTO sampleCreateDTO;
    private TaskConfigDTO sampleConfigDTO;
    private FederatedTask sampleTask;
    private TaskParticipant sampleParticipant;

    @BeforeEach
    void setUp() {
        // 配置UuidUtil Mock
        lenient().when(uuidUtil.generateUuid()).thenReturn("task-12345");

        // 准备测试数据
        setupTestData();
    }


    private void setupTestData() {
        // 准备参与者数据
        List<TaskCreateDTO.ParticipantDTO> participants = Arrays.asList(
                TaskCreateDTO.ParticipantDTO.builder()
                        .vmId("vm-001")
                        .dataSource("/data/vm001/dataset")
                        .role("PARTICIPANT")
                        .build(),
                TaskCreateDTO.ParticipantDTO.builder()
                        .vmId("vm-002")
                        .dataSource("/data/vm002/dataset")
                        .role("PARTICIPANT")
                        .build()
        );

        // 准备创建任务DTO
        sampleCreateDTO = TaskCreateDTO.builder()
                .taskName("联邦学习测试任务")
                .taskType("CLASSIFICATION")
                .description("用于测试的联邦学习任务")
                .algorithm("FEDERATED_AVERAGING")
                .participants(participants)
                .hyperparameters(TaskCreateDTO.HyperparametersDTO.builder()
                        .learningRate(0.01)
                        .batchSize(32)
                        .epochs(5)
                        .rounds(10)
                        .minParticipants(2)
                        .build())
                .modelConfig(TaskCreateDTO.ModelConfigDTO.builder()
                        .modelType("NEURAL_NETWORK")
                        .build())
                .build();

        // 准备配置任务DTO
        sampleConfigDTO = TaskConfigDTO.builder()
                .algorithm("FEDERATED_AVERAGING")
                .hyperparameters(TaskConfigDTO.HyperparametersDTO.builder()
                        .learningRate(0.001)
                        .batchSize(64)
                        .epochs(8)
                        .rounds(15)
                        .build())
                .build();

        // 准备任务实体
        sampleTask = FederatedTask.builder()
                .id("task-12345")
                .taskName("联邦学习测试任务")
                .taskType("CLASSIFICATION")
                .description("用于测试的联邦学习任务")
                .status(FederatedTaskStatus.fromCode("CREATED"))
                .algorithm(FederatedAlgorithm.fromCode("FEDERATED_AVERAGING"))
                .modelType("NEURAL_NETWORK")
                .totalRounds(10)
                .currentRound(0)
                .minParticipants(2)
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 准备参与者实体
        sampleParticipant = TaskParticipant.builder()
                .taskId("task-12345")
                .vmId("vm-001")
                .status(ParticipantStatus.fromCode("CONNECTED"))
                .dataSource("/data/vm001/dataset")
                .role(ParticipantRole.fromCode("PARTICIPANT"))
                .joinedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 测试创建任务 - 成功场景
     */
    @Test
    void testCreateTask_Success() {
        // Mock mapper操作成功
        when(tasksMapper.insertTask(any(FederatedTask.class))).thenReturn(1);
        when(tasksMapper.insertParticipant(any(TaskParticipant.class))).thenReturn(1);
        lenient().doNothing().when(logService).logTask(anyString(), anyString(), anyString(), anyString(), anyString(), any());

        // 调用服务方法
        TaskOperationVO result = federatedTaskService.createTask(sampleCreateDTO, "admin");

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getTaskId());
        assertEquals("联邦学习测试任务", result.getTaskName());
        assertEquals("CREATED", result.getStatus());
        assertEquals("admin", result.getCreatedBy());
        assertEquals(2, result.getParticipantCount());
        assertTrue(result.getEstimatedDuration() > 0);

        // 验证mapper调用
        verify(tasksMapper).insertTask(any(FederatedTask.class));
        verify(tasksMapper, times(2)).insertParticipant(any(TaskParticipant.class));

        // 验证日志记录
        verify(logService).logTask(anyString(), eq("INFO"), eq("任务创建成功"), eq("TASK_MANAGER"), isNull(), any());
    }

    /**
     * 测试创建任务 - 无效配置
     */
    @Test
    void testCreateTask_InvalidConfig() {
        // 准备无效的创建DTO
        TaskCreateDTO invalidCreateDTO = TaskCreateDTO.builder()
                .taskName("")  // 空名称
                .taskType("CLASSIFICATION")
                .algorithm("FEDERATED_AVERAGING")
                .hyperparameters(TaskCreateDTO.HyperparametersDTO.builder()
                        .rounds(-1)  // 无效轮数
                        .minParticipants(0)  // 无效最小参与者数
                        .build())
                .participants(new ArrayList<>())  // 空参与者列表
                .build();

        // 调用服务方法应该抛出异常
        assertThrows(UserException.class, () -> {
            federatedTaskService.createTask(invalidCreateDTO, "admin");
        });

        // 验证不会调用数据库操作
        verify(tasksMapper, never()).insertTask(any(FederatedTask.class));
        verify(tasksMapper, never()).insertParticipant(any(TaskParticipant.class));
    }

    /**
     * 测试创建任务 - 数据库插入失败
     */
    @Test
    void testCreateTask_DatabaseInsertFailure() {
        // Mock mapper操作失败
        when(tasksMapper.insertTask(any(FederatedTask.class))).thenReturn(0);

        // 调用服务方法应该抛出异常
        assertThrows(UserException.class, () -> {
            federatedTaskService.createTask(sampleCreateDTO, "admin");
        });

        // 验证mapper调用
        verify(tasksMapper).insertTask(any(FederatedTask.class));
        verify(tasksMapper, never()).insertParticipant(any(TaskParticipant.class));
    }

    /**
     * 测试配置任务 - 成功场景
     */
    @Test
    void testConfigureTask_Success() {
        String taskId = "task-12345";
        
        // Mock任务存在且状态为CREATED
        when(tasksMapper.selectTaskById(taskId)).thenReturn(sampleTask);
        when(tasksMapper.updateTask(any(FederatedTask.class))).thenReturn(1);
        lenient().doNothing().when(logService).logTask(anyString(), anyString(), anyString(), anyString(), anyString(), any());

        // 调用服务方法
        TaskOperationVO result = federatedTaskService.configureTask(taskId, sampleConfigDTO, "admin");

        // 验证结果
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals("CONFIGURED", result.getStatus());
        assertNotNull(result.getUpdatedAt());
        assertEquals("v1.1", result.getConfigVersion());

        // 验证mapper调用
        verify(tasksMapper).selectTaskById(taskId);
        verify(tasksMapper).updateTask(any(FederatedTask.class));

        // 验证日志记录
        verify(logService).logTask(anyString(), eq("INFO"), eq("任务配置更新"), eq("TASK_MANAGER"), isNull(), isNull());
    }

    /**
     * 测试配置任务 - 任务不存在
     */
    @Test
    void testConfigureTask_TaskNotFound() {
        String taskId = "non-existent-task";
        
        // Mock任务不存在
        when(tasksMapper.selectTaskById(taskId)).thenReturn(null);

        // 调用服务方法应该抛出异常
        assertThrows(UserException.class, () -> {
            federatedTaskService.configureTask(taskId, sampleConfigDTO, "admin");
        });

        // 验证mapper调用
        verify(tasksMapper).selectTaskById(taskId);
        verify(tasksMapper, never()).updateTask(any(FederatedTask.class));
    }

    /**
     * 测试配置任务 - 任务状态不允许配置
     */
    @Test
    void testConfigureTask_InvalidStatus() {
        String taskId = "task-12345";
        
        // 创建状态为RUNNING的任务（不允许配置）
        FederatedTask runningTask = FederatedTask.builder()
                .id(taskId)
                .status(FederatedTaskStatus.fromCode("RUNNING"))
                .build();
        
        when(tasksMapper.selectTaskById(taskId)).thenReturn(runningTask);

        // 调用服务方法应该抛出异常
        assertThrows(UserException.class, () -> {
            federatedTaskService.configureTask(taskId, sampleConfigDTO, "admin");
        });

        // 验证mapper调用
        verify(tasksMapper).selectTaskById(taskId);
        verify(tasksMapper, never()).updateTask(any(FederatedTask.class));
    }

    /**
     * 测试启动任务 - 成功场景
     */
    @Test
    void testStartTask_Success() {
        String taskId = "task-12345";
        
        // Mock任务存在且状态为CONFIGURED
        FederatedTask configuredTask = FederatedTask.builder()
                .id(taskId)
                .status(FederatedTaskStatus.fromCode("CONFIGURED"))
                .build();
        
        List<TaskParticipant> participants = Arrays.asList(
                TaskParticipant.builder().vmId("vm-001").dataSource("/data/vm001").build(),
                TaskParticipant.builder().vmId("vm-002").dataSource("/data/vm002").build()
        );

        when(tasksMapper.selectTaskById(taskId)).thenReturn(configuredTask);
        when(tasksMapper.selectParticipantsByTaskId(taskId)).thenReturn(participants);
        when(tasksMapper.updateTaskStatus(eq(taskId), eq("RUNNING"), any(LocalDateTime.class))).thenReturn(1);
        when(tasksMapper.updateParticipantStatus(eq(taskId), anyString(), eq("CONNECTED"), any(LocalDateTime.class))).thenReturn(1);
        lenient().doNothing().when(logService).logTask(anyString(), anyString(), anyString(), anyString(), anyString(), any());

        // 调用服务方法
        TaskOperationVO result = federatedTaskService.startTask(taskId, "admin");

        // 验证结果
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals("RUNNING", result.getStatus());
        assertNotNull(result.getStartedAt());
        assertEquals(0, result.getCurrentRound());
        assertNotNull(result.getParticipants());
        assertEquals(2, result.getParticipants().size());

        // 验证所有参与者状态为CONNECTED
        for (TaskOperationVO.ParticipantStatus ps : result.getParticipants()) {
            assertEquals("CONNECTED", ps.getStatus());
        }

        // 验证mapper调用
        verify(tasksMapper).selectTaskById(taskId);
        verify(tasksMapper).selectParticipantsByTaskId(taskId);
        verify(tasksMapper).updateTaskStatus(eq(taskId), eq("RUNNING"), any(LocalDateTime.class));
        verify(tasksMapper, times(2)).updateParticipantStatus(eq(taskId), anyString(), eq("CONNECTED"), any(LocalDateTime.class));

        // 验证日志记录
        verify(logService).logTask(anyString(), eq("INFO"), eq("任务启动成功"), eq("TASK_MANAGER"), isNull(), any());
    }

    /**
     * 测试启动任务 - 任务状态不允许启动
     */
    @Test
    void testStartTask_InvalidStatus() {
        String taskId = "task-12345";
        
        // Mock任务状态为CREATED（需要先配置才能启动）
        FederatedTask createdTask = FederatedTask.builder()
                .id(taskId)
                .status(FederatedTaskStatus.fromCode("CREATED"))
                .build();
        
        when(tasksMapper.selectTaskById(taskId)).thenReturn(createdTask);

        // 调用服务方法应该抛出异常
        UserException exception = assertThrows(UserException.class, () -> {
            federatedTaskService.startTask(taskId, "admin");
        });

        assertTrue(exception.getMessage().contains("任务状态不允许启动"));
        assertTrue(exception.getMessage().contains("CREATED"));

        // 验证mapper调用
        verify(tasksMapper).selectTaskById(taskId);
        verify(tasksMapper, never()).updateTaskStatus(anyString(), anyString(), any(LocalDateTime.class));
    }

    /**
     * 测试暂停任务功能
     */
    @Test
    void testPauseTask_Success() {
        String taskId = "task-12345";
        
        // Mock任务存在且状态为RUNNING
        FederatedTask runningTask = FederatedTask.builder()
                .id(taskId)
                .status(FederatedTaskStatus.fromCode("RUNNING"))
                .build();
        
        when(tasksMapper.selectTaskById(taskId)).thenReturn(runningTask);

        // 调用服务方法
        TaskOperationVO result = federatedTaskService.pauseTask(taskId, "admin");

        // 验证基本结果
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        // 根据实际实现验证其他字段
    }

    /**
     * 测试任务详情查询
     */
    @Test
    void testGetTaskDetail() {
        String taskId = "task-12345";
        
        when(tasksMapper.selectTaskById(taskId)).thenReturn(sampleTask);
        when(tasksMapper.selectParticipantsByTaskId(taskId)).thenReturn(Arrays.asList(sampleParticipant));

        TaskDetailVO result = federatedTaskService.getTaskDetail(taskId);

        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());

        // 验证mapper调用
        verify(tasksMapper).selectTaskById(taskId);
        verify(tasksMapper).selectParticipantsByTaskId(taskId);
    }

    /**
     * 测试添加任务参与者
     */
    @Test
    void testAddParticipant() {
        String taskId = "task-12345";
        TaskParticipant participant = TaskParticipant.builder()
                .id(taskId)
                .vmId("vm-003")
                .status(ParticipantStatus.fromCode("PENDING"))
                .build();

        when(tasksMapper.insertParticipant(participant)).thenReturn(1);

        // 调用服务方法
        federatedTaskService.addParticipant(taskId, participant);

        // 验证mapper调用
        verify(tasksMapper).insertParticipant(participant);
    }

    /**
     * 测试更新参与者状态
     */
    @Test
    void testUpdateParticipantStatus() {
        String taskId = "task-12345";
        String vmId = "vm-001";
        String status = "CONNECTED";

        when(tasksMapper.updateParticipantStatus(eq(taskId), eq(vmId), eq(status), any(LocalDateTime.class))).thenReturn(1);

        // 调用服务方法
        federatedTaskService.updateParticipantStatus(taskId, vmId, status);

        // 验证mapper调用
        verify(tasksMapper).updateParticipantStatus(eq(taskId), eq(vmId), eq(status), any(LocalDateTime.class));
    }

    /**
     * 测试任务存在检查
     */
    @Test
    void testTaskExists() {
        String existingTaskId = "existing-task";
        String nonExistentTaskId = "non-existent-task";

        when(tasksMapper.selectTaskById(existingTaskId)).thenReturn(sampleTask);
        when(tasksMapper.selectTaskById(nonExistentTaskId)).thenReturn(null);

        // 测试存在的任务
        assertTrue(federatedTaskService.taskExists(existingTaskId));

        // 测试不存在的任务
        assertFalse(federatedTaskService.taskExists(nonExistentTaskId));

        // 验证mapper调用
        verify(tasksMapper).selectTaskById(existingTaskId);
        verify(tasksMapper).selectTaskById(nonExistentTaskId);
    }

    /**
     * 测试任务配置验证
     */
    @Test
    void testIsValidTaskConfig() {
        // 测试有效配置
        assertTrue(federatedTaskService.isValidTaskConfig(sampleCreateDTO));

        // 测试无效配置 - 空任务名
        TaskCreateDTO invalidDTO1 = TaskCreateDTO.builder()
                .taskName("")
                .build();
        assertFalse(federatedTaskService.isValidTaskConfig(invalidDTO1));

        // 测试无效配置 - 负数轮数
        TaskCreateDTO invalidDTO2 = TaskCreateDTO.builder()
                .taskName("Test Task")
                .taskType("CLASSIFICATION")
                .algorithm("FEDERATED_AVERAGING")
                .hyperparameters(TaskCreateDTO.HyperparametersDTO.builder()
                        .rounds(-1)
                        .build())
                .build();
        assertFalse(federatedTaskService.isValidTaskConfig(invalidDTO2));

        // 测试无效配置 - 空参与者列表
        TaskCreateDTO invalidDTO3 = TaskCreateDTO.builder()
                .taskName("Test Task")
                .taskType("CLASSIFICATION")
                .algorithm("FEDERATED_AVERAGING")
                .hyperparameters(TaskCreateDTO.HyperparametersDTO.builder()
                        .rounds(10)
                        .build())
                .participants(new ArrayList<>())
                .build();
        assertFalse(federatedTaskService.isValidTaskConfig(invalidDTO3));
    }

    /**
     * 测试状态转换验证
     */
    @Test
    void testIsValidStatusTransition() {
        // 测试有效的状态转换
        assertTrue(federatedTaskService.isValidStatusTransition("CREATED", "CONFIGURED"));
        assertTrue(federatedTaskService.isValidStatusTransition("CONFIGURED", "RUNNING"));
        assertTrue(federatedTaskService.isValidStatusTransition("RUNNING", "PAUSED"));
        assertTrue(federatedTaskService.isValidStatusTransition("PAUSED", "RUNNING"));
        assertTrue(federatedTaskService.isValidStatusTransition("RUNNING", "COMPLETED"));

        // 测试无效的状态转换
        assertFalse(federatedTaskService.isValidStatusTransition("CREATED", "RUNNING"));
        assertFalse(federatedTaskService.isValidStatusTransition("COMPLETED", "RUNNING"));
        assertFalse(federatedTaskService.isValidStatusTransition("CANCELLED", "RUNNING"));
    }

    /**
     * 测试任务进度计算
     */
    @Test
    void testCalculateTaskProgress() {
        String taskId = "task-12345";
        
        // Mock任务当前轮数为5，最大轮数为10
        FederatedTask taskWithProgress = FederatedTask.builder()
                .id(taskId)
                .currentRound(5)
                .totalRounds(10)
                .build();
        
        when(tasksMapper.selectTaskById(taskId)).thenReturn(taskWithProgress);

        double progress = federatedTaskService.calculateTaskProgress(taskId);

        assertEquals(50.0, progress, 0.001);
    }

    /**
     * 测试任务执行时间估算
     */
    @Test
    void testEstimateTaskDuration() {
        // 使用样例创建DTO进行估算
        int estimatedDuration = federatedTaskService.estimateTaskDuration(sampleCreateDTO);

        // 验证估算时间为正数
        assertTrue(estimatedDuration > 0);
        
        // 根据任务配置验证估算的合理性（轮数、epochs等影响执行时间）
        assertTrue(estimatedDuration > sampleCreateDTO.getHyperparameters().getRounds() * sampleCreateDTO.getHyperparameters().getEpochs());
    }

    /**
     * 测试记录任务日志
     */
    @Test
    void testLogTask() {
        String taskId = "task-12345";
        String level = "INFO";
        String message = "测试日志消息";
        String source = "TEST";
        String vmId = "vm-001";
        Object details = Map.of("key", "value");

        doNothing().when(logService).logTask(taskId, level, message, source, vmId, details);

        // 调用服务方法
        federatedTaskService.logTask(taskId, level, message, source, vmId, details);

        // 验证日志服务调用
        verify(logService).logTask(taskId, level, message, source, vmId, details);
    }

    // ==================== 协议v1.4标准化验证测试 ====================

    /**
     * 测试sendTrainingStartCommand使用标准消息格式
     * 验证新的MessageBuilder构建的消息是否符合协议v1.4标准
     */
    @Test
    void testSendTrainingStartCommandWithStandardFormat() {
        String taskId = "test-task-123";

        // 创建测试参与者
        List<TaskParticipant> participants = Arrays.asList(
            TaskParticipant.builder()
                .vmId("vm-001")
                .dataSource("/data/vm001")
                .role(ParticipantRole.TRAINER)
                .build(),
            TaskParticipant.builder()
                .vmId("vm-002")
                .dataSource("/data/vm002")
                .role(ParticipantRole.TRAINER)
                .build()
        );

        // 创建测试任务，包含算法配置
        FederatedTask testTask = FederatedTask.builder()
            .id(taskId)
            .taskName("标准协议测试任务")
            .algorithm(FederatedAlgorithm.FEDERATED_AVERAGING)
            .learningRate(0.01)
            .batchSize(32)
            .epochs(100)
            .status(FederatedTaskStatus.RUNNING)
            .build();

        // Mock任务查询
        when(tasksMapper.selectTaskById(taskId)).thenReturn(testTask);

        // 创建模拟的联邦任务服务实例
        FederatedTaskServiceImpl spyService = spy(federatedTaskService);
        doReturn(testTask).when(spyService).getTaskById(taskId);

        // 使用反射调用私有方法 sendTrainingStartCommand
        try {
            java.lang.reflect.Method method = FederatedTaskServiceImpl.class
                .getDeclaredMethod("sendTrainingStartCommand", String.class, List.class);
            method.setAccessible(true);
            method.invoke(spyService, taskId, participants);
        } catch (Exception e) {
            fail("反射调用sendTrainingStartCommand失败: " + e.getMessage());
        }

        // 验证发送的消息格式
        ArgumentCaptor<ProtocolMessage> messageCaptor = ArgumentCaptor.forClass(ProtocolMessage.class);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        // 验证每个参与者都收到了消息
        verify(messagingTemplate, times(participants.size())).convertAndSend(
            topicCaptor.capture(),
            messageCaptor.capture()
        );

        List<ProtocolMessage> sentMessages = messageCaptor.getAllValues();
        List<String> topics = topicCaptor.getAllValues();

        // 验证消息数量正确
        assertEquals(participants.size(), sentMessages.size(), "发送的消息数量应该等于参与者数量");

        // 验证每个消息都符合协议v1.4标准
        for (int i = 0; i < sentMessages.size(); i++) {
            ProtocolMessage message = sentMessages.get(i);
            String topic = topics.get(i);
            TaskParticipant participant = participants.get(i);

            // 验证topic格式
            assertEquals("/topic/vm/" + participant.getVmId(), topic, "Topic格式不正确");

            // 验证消息类型
            assertEquals(ProtocolType.TRAINING_START, message.getType(), "消息类型应该是TRAINING_START");

            // 验证ID格式符合协议标准：cmd-{timestamp}-{random}
            assertNotNull(message.getId(), "消息ID不能为null");
            assertTrue(message.getId().matches("cmd-\\d+-[a-f0-9]{8}"),
                "ID格式不符合标准：" + message.getId());

            // 验证vmId
            assertEquals(participant.getVmId(), message.getVmId(), "vmId不匹配");

            // 验证数据字段符合协议v1.4标准
            Map<String, Object> data = message.getData();
            assertNotNull(data, "消息数据不能为null");

            // 验证必需的标准字段
            assertEquals(taskId, data.get("taskId"), "taskId不匹配");
            assertEquals(1, data.get("roundNumber"), "roundNumber应该是1");
            assertEquals("FEDERATED_AVERAGING", data.get("mlAlgorithm"), "mlAlgorithm不匹配");
            assertEquals("请开始本地ML训练任务", data.get("message"), "message不匹配");

            // 验证复杂对象字段存在且为Map类型
            assertNotNull(data.get("hyperparameters"), "hyperparameters字段不能为null");
            assertTrue(data.get("hyperparameters") instanceof Map, "hyperparameters应该是Map对象");

            assertNotNull(data.get("globalModel"), "globalModel字段不能为null");
            assertTrue(data.get("globalModel") instanceof Map, "globalModel应该是Map对象");

            assertNotNull(data.get("timestamp"), "timestamp字段不能为null");

            // 验证不包含非标准字段
            assertFalse(data.containsKey("instruction"), "不应包含instruction字段");
            assertFalse(data.containsKey("participantId"), "不应包含participantId字段");
            assertFalse(data.containsKey("algorithm"), "不应包含algorithm字段");

            // 验证签名字段存在
            assertNotNull(message.getSignature(), "签名字段不能为null");
        }
    }

    /**
     * 测试sendTrainingStartCommand - 空参与者列表
     * 验证处理边界情况的健壮性
     */
    @Test
    void testSendTrainingStartCommand_EmptyParticipants() {
        String taskId = "test-task-empty";
        List<TaskParticipant> emptyParticipants = Collections.emptyList();

        // 使用反射调用私有方法
        try {
            java.lang.reflect.Method method = FederatedTaskServiceImpl.class
                .getDeclaredMethod("sendTrainingStartCommand", String.class, List.class);
            method.setAccessible(true);
            method.invoke(federatedTaskService, taskId, emptyParticipants);
        } catch (Exception e) {
            fail("反射调用sendTrainingStartCommand失败: " + e.getMessage());
        }

        // 验证没有发送任何消息
        verify(messagingTemplate, never()).convertAndSend(anyString(), any());
    }

    /**
     * 测试sendTrainingStartCommand - 任务不存在
     * 验证错误处理逻辑
     */
    @Test
    void testSendTrainingStartCommand_TaskNotFound() {
        String taskId = "non-existent-task";
        List<TaskParticipant> participants = Arrays.asList(
            TaskParticipant.builder().vmId("vm-001").build()
        );

        // Mock任务查询返回null
        when(tasksMapper.selectTaskById(taskId)).thenReturn(null);

        FederatedTaskServiceImpl spyService = spy(federatedTaskService);
        doReturn(null).when(spyService).getTaskById(taskId);

        // 使用反射调用私有方法
        try {
            java.lang.reflect.Method method = FederatedTaskServiceImpl.class
                .getDeclaredMethod("sendTrainingStartCommand", String.class, List.class);
            method.setAccessible(true);
            method.invoke(spyService, taskId, participants);
        } catch (Exception e) {
            fail("反射调用sendTrainingStartCommand失败: " + e.getMessage());
        }

        // 验证没有发送任何消息
        verify(messagingTemplate, never()).convertAndSend(anyString(), any());
    }

    /**
     * 测试MessageBuilder构建的超参数对象结构
     * 验证hyperparameters对象包含正确的字段
     */
    @Test
    void testMessageBuilderHyperparametersStructure() {
        // 创建测试任务
        FederatedTask task = FederatedTask.builder()
            .learningRate(0.001)
            .batchSize(64)
            .epochs(200)
            .build();

        // 构建超参数对象
        Map<String, Object> hyperparameters = MessageBuilder.buildHyperparameters(task);

        // 验证超参数对象结构
        assertNotNull(hyperparameters, "超参数对象不能为null");

        // 验证包含正确的字段和值
        assertEquals(0.001, hyperparameters.get("learningRate"), "learningRate值不正确");
        assertEquals(64, hyperparameters.get("batchSize"), "batchSize值不正确");
        assertEquals(200, hyperparameters.get("epochs"), "epochs值不正确");
        assertEquals(300, hyperparameters.get("timeout"), "timeout应该有默认值300");

        // 验证字段类型
        assertTrue(hyperparameters.get("learningRate") instanceof Number, "learningRate应该是数值类型");
        assertTrue(hyperparameters.get("batchSize") instanceof Number, "batchSize应该是数值类型");
        assertTrue(hyperparameters.get("epochs") instanceof Number, "epochs应该是数值类型");
        assertTrue(hyperparameters.get("timeout") instanceof Number, "timeout应该是数值类型");
    }

    /**
     * 测试MessageBuilder构建的全局模型对象结构
     * 验证globalModel对象包含正确的字段
     */
    @Test
    void testMessageBuilderGlobalModelStructure() {
        String taskId = "test-task-456";
        int roundNumber = 3;

        // 构建全局模型对象
        Map<String, Object> globalModel = MessageBuilder.buildGlobalModel(taskId, roundNumber);

        // 验证全局模型对象结构
        assertNotNull(globalModel, "全局模型对象不能为null");

        // 验证包含正确的字段和值
        String expectedModelId = "global-model-" + taskId + "-round-" + roundNumber;
        assertEquals(expectedModelId, globalModel.get("modelId"), "modelId格式不正确");

        String expectedVersion = "v" + roundNumber + ".0";
        assertEquals(expectedVersion, globalModel.get("version"), "version格式不正确");

        String expectedDownloadUrl = "/api/federated/models/" + taskId + "/global/round/" + roundNumber;
        assertEquals(expectedDownloadUrl, globalModel.get("downloadUrl"), "downloadUrl格式不正确");

        // 验证字段类型
        assertTrue(globalModel.get("modelId") instanceof String, "modelId应该是字符串类型");
        assertTrue(globalModel.get("version") instanceof String, "version应该是字符串类型");
        assertTrue(globalModel.get("downloadUrl") instanceof String, "downloadUrl应该是字符串类型");
    }
}