package com.feduwacomm.service;

import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.enums.FederatedTaskStatus;
import com.feduwacomm.enums.GlobalModelStatus;
import com.feduwacomm.enums.RoundState;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.GlobalModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RoundStateManager单元测试
 *
 * 测试轮次状态管理器的核心功能，包括状态转换验证、
 * 轮次推进逻辑和全局模型创建机制
 */
@ExtendWith(MockitoExtension.class)
class RoundStateManagerTest {

    @Mock
    private GlobalModelMapper globalModelMapper;

    @Mock
    private FederatedTasksMapper federatedTasksMapper;

    @InjectMocks
    private RoundStateManager roundStateManager;

    private String testTaskId;
    private Integer testRoundNumber;
    private FederatedTask mockTask;
    private GlobalModel mockModel;

    @BeforeEach
    void setUp() {
        testTaskId = "test-task-001";
        testRoundNumber = 3;

        // 模拟任务对象
        mockTask = new FederatedTask();
        mockTask.setId(testTaskId);
        mockTask.setTaskName("测试联邦任务");
        mockTask.setStatus(FederatedTaskStatus.RUNNING);
        mockTask.setCurrentRound(testRoundNumber);
        mockTask.setTotalRounds(5);
        mockTask.setUpdatedAt(LocalDateTime.now());

        // 模拟全局模型对象
        mockModel = new GlobalModel();
        mockModel.setId("model-001");
        mockModel.setTaskId(testTaskId);
        mockModel.setRoundNumber(testRoundNumber);
        mockModel.setStatus(GlobalModelStatus.COMPLETED);
        mockModel.setDistributionStatus("DISTRIBUTED");
        mockModel.setCreatedAt(LocalDateTime.now());
        mockModel.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testGetCurrentRoundState_WithValidTask_ShouldReturnCorrectState() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);

        // When
        RoundState result = roundStateManager.getCurrentRoundState(testTaskId);

        // Then
        assertEquals(RoundState.READY, result); // COMPLETED + DISTRIBUTED = READY
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(globalModelMapper).selectByTaskIdAndRound(testTaskId, testRoundNumber);
    }

    @Test
    void testGetCurrentRoundState_TaskNotFound_ShouldReturnNull() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(null);

        // When
        RoundState result = roundStateManager.getCurrentRoundState(testTaskId);

        // Then
        assertNull(result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(globalModelMapper, never()).selectByTaskIdAndRound(anyString(), anyInt());
    }

    @Test
    void testGetCurrentRoundState_NoCurrentRound_ShouldReturnNull() {
        // Given
        mockTask.setCurrentRound(null);
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);

        // When
        RoundState result = roundStateManager.getCurrentRoundState(testTaskId);

        // Then
        assertNull(result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(globalModelMapper, never()).selectByTaskIdAndRound(anyString(), anyInt());
    }

    @Test
    void testGetCurrentRoundState_GlobalModelNotFound_ShouldReturnNull() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber)).thenReturn(null);

        // When
        RoundState result = roundStateManager.getCurrentRoundState(testTaskId);

        // Then
        assertNull(result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(globalModelMapper).selectByTaskIdAndRound(testTaskId, testRoundNumber);
    }

    @Test
    void testTransitionRoundState_ValidTransition_ShouldSucceed() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);
        when(globalModelMapper.updateGlobalModel(any(GlobalModel.class))).thenReturn(1);

        // When
        boolean result = roundStateManager.transitionRoundState(testTaskId, RoundState.READY, RoundState.TRAINING);

        // Then
        assertTrue(result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(globalModelMapper).selectByTaskIdAndRound(testTaskId, testRoundNumber);
        verify(globalModelMapper).updateGlobalModel(any(GlobalModel.class));
    }

    @Test
    void testTransitionRoundState_InvalidTransition_ShouldFail() {
        // When - 尝试无效转换：TRAINING -> AGGREGATING 是有效的，测试TRAINING -> WAITING_ACK（无效）
        boolean result = roundStateManager.transitionRoundState(testTaskId, RoundState.TRAINING, RoundState.WAITING_ACK);

        // Then
        assertFalse(result);
        verify(federatedTasksMapper, never()).selectTaskById(anyString());
    }

    @Test
    void testTransitionRoundState_NullParameters_ShouldFail() {
        // When & Then
        assertFalse(roundStateManager.transitionRoundState(null, RoundState.READY, RoundState.TRAINING));
        assertFalse(roundStateManager.transitionRoundState(testTaskId, null, RoundState.TRAINING));
        assertFalse(roundStateManager.transitionRoundState(testTaskId, RoundState.READY, null));
    }

    @Test
    void testTransitionRoundState_CurrentStateMismatch_ShouldFail() {
        // Given - 设置模型状态为PENDING (AGGREGATING)，但尝试从READY转换
        mockModel.setStatus(GlobalModelStatus.PENDING);
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);

        // When
        boolean result = roundStateManager.transitionRoundState(testTaskId, RoundState.READY, RoundState.TRAINING);

        // Then
        assertFalse(result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(globalModelMapper).selectByTaskIdAndRound(testTaskId, testRoundNumber);
        verify(globalModelMapper, never()).updateGlobalModel(any(GlobalModel.class));
    }

    @Test
    void testAdvanceRound_Success_ShouldUpdateTask() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskProgress(testTaskId, 4, "RUNNING")).thenReturn(1);

        // When
        boolean result = roundStateManager.advanceRound(testTaskId);

        // Then
        assertTrue(result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(federatedTasksMapper).updateTaskProgress(testTaskId, 4, "RUNNING");
    }

    @Test
    void testAdvanceRound_TaskNotFound_ShouldFail() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(null);

        // When
        boolean result = roundStateManager.advanceRound(testTaskId);

        // Then
        assertFalse(result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(federatedTasksMapper, never()).updateTaskProgress(anyString(), anyInt(), anyString());
    }

    @Test
    void testAdvanceRound_MaxRoundsReached_ShouldFail() {
        // Given
        mockTask.setCurrentRound(5); // 已达到最大轮次
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);

        // When
        boolean result = roundStateManager.advanceRound(testTaskId);

        // Then
        assertFalse(result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(federatedTasksMapper, never()).updateTaskProgress(anyString(), anyInt(), anyString());
    }

    @Test
    void testAdvanceRound_NullRoundInfo_ShouldFail() {
        // Given
        mockTask.setCurrentRound(null);
        mockTask.setTotalRounds(null);
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);

        // When
        boolean result = roundStateManager.advanceRound(testTaskId);

        // Then
        assertFalse(result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(federatedTasksMapper, never()).updateTaskProgress(anyString(), anyInt(), anyString());
    }

    @Test
    void testCreateRoundModel_Success_ShouldReturnTrue() {
        // Given
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, 4)).thenReturn(null);
        when(globalModelMapper.insertGlobalModel(any(GlobalModel.class))).thenReturn(1);

        // When
        boolean result = roundStateManager.createRoundModel(testTaskId, 4);

        // Then
        assertTrue(result);
        verify(globalModelMapper).selectByTaskIdAndRound(testTaskId, 4);
        verify(globalModelMapper).insertGlobalModel(any(GlobalModel.class));
    }

    @Test
    void testCreateRoundModel_AlreadyExists_ShouldReturnTrue() {
        // Given
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, 4)).thenReturn(mockModel);

        // When
        boolean result = roundStateManager.createRoundModel(testTaskId, 4);

        // Then
        assertTrue(result);
        verify(globalModelMapper).selectByTaskIdAndRound(testTaskId, 4);
        verify(globalModelMapper, never()).insertGlobalModel(any(GlobalModel.class));
    }

    @Test
    void testCreateRoundModel_InvalidParameters_ShouldFail() {
        // When & Then
        assertFalse(roundStateManager.createRoundModel(null, 4));
        assertFalse(roundStateManager.createRoundModel(testTaskId, null));
        assertFalse(roundStateManager.createRoundModel(testTaskId, 0));
        assertFalse(roundStateManager.createRoundModel(testTaskId, -1));
    }

    @Test
    void testIsWaitingForAcks_WhenDistributed_ShouldReturnFalse() {
        // Given
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);

        // When
        boolean result = roundStateManager.isWaitingForAcks(testTaskId, testRoundNumber);

        // Then
        assertFalse(result); // COMPLETED + DISTRIBUTED = READY状态，不是WAITING_ACK
        verify(globalModelMapper).selectByTaskIdAndRound(testTaskId, testRoundNumber);
    }

    @Test
    void testIsWaitingForAcks_ModelNotFound_ShouldReturnFalse() {
        // Given
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber)).thenReturn(null);

        // When
        boolean result = roundStateManager.isWaitingForAcks(testTaskId, testRoundNumber);

        // Then
        assertFalse(result);
        verify(globalModelMapper).selectByTaskIdAndRound(testTaskId, testRoundNumber);
    }

    @Test
    void testGetRoundStateDescription_ValidState_ShouldReturnDescription() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);

        // When
        String result = roundStateManager.getRoundStateDescription(testTaskId);

        // Then
        assertEquals(RoundState.READY.getDescription(), result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
        verify(globalModelMapper).selectByTaskIdAndRound(testTaskId, testRoundNumber);
    }

    @Test
    void testGetRoundStateDescription_InvalidState_ShouldReturnUnknown() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(null);

        // When
        String result = roundStateManager.getRoundStateDescription(testTaskId);

        // Then
        assertEquals("未知状态", result);
        verify(federatedTasksMapper).selectTaskById(testTaskId);
    }

    @Test
    void testStateTransitionValidation_ValidTransitions_ShouldPass() {
        // 测试所有有效的状态转换
        assertTrue(RoundState.TRAINING.canTransitionTo(RoundState.AGGREGATING));
        assertTrue(RoundState.AGGREGATING.canTransitionTo(RoundState.DISTRIBUTING));
        assertTrue(RoundState.DISTRIBUTING.canTransitionTo(RoundState.WAITING_ACK));
        assertTrue(RoundState.WAITING_ACK.canTransitionTo(RoundState.READY));
        assertTrue(RoundState.READY.canTransitionTo(RoundState.TRAINING));
    }

    @Test
    void testStateTransitionValidation_InvalidTransitions_ShouldFail() {
        // 测试无效的状态转换
        assertFalse(RoundState.TRAINING.canTransitionTo(RoundState.WAITING_ACK));
        assertFalse(RoundState.DISTRIBUTING.canTransitionTo(RoundState.READY));
        assertFalse(RoundState.AGGREGATING.canTransitionTo(RoundState.TRAINING));

        // 测试相同状态转换
        assertFalse(RoundState.TRAINING.canTransitionTo(RoundState.TRAINING));
    }
}