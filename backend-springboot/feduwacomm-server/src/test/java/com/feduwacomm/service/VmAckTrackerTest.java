package com.feduwacomm.service;

import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.entity.ModelDistribution;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.entity.VmAckTracking;
import com.feduwacomm.enums.GlobalModelStatus;
import com.feduwacomm.enums.ParticipantStatus;
import com.feduwacomm.mapper.GlobalModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.service.cache.model.AckProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VmAckTracker单元测试
 *
 * 测试VM确认跟踪器的功能，包括ACK记录、确认状态检查、
 * 超时处理和分发记录初始化
 */
@ExtendWith(MockitoExtension.class)
class VmAckTrackerTest {

    @Mock
    private ModelDistributionMapper modelDistributionMapper;

    @Mock
    private GlobalModelMapper globalModelMapper;

    @Mock
    private TaskParticipantsMapper taskParticipantsMapper;

    @Mock
    private AckCacheService ackCacheService;

    @InjectMocks
    private VmAckTracker vmAckTracker;

    private String testTaskId;
    private String testVmId;
    private Integer testRoundNumber;
    private GlobalModel mockModel;
    private List<TaskParticipant> mockParticipants;

    @BeforeEach
    void setUp() {
        testTaskId = "test-task-001";
        testVmId = "vm-001";
        testRoundNumber = 3;

        // 模拟全局模型
        mockModel = new GlobalModel();
        mockModel.setId("model-001");
        mockModel.setTaskId(testTaskId);
        mockModel.setRoundNumber(testRoundNumber);
        mockModel.setStatus(GlobalModelStatus.COMPLETED);

        // 模拟任务参与者
        mockParticipants = Arrays.asList(
                createParticipant("vm-001", "CONNECTED"),
                createParticipant("vm-002", "CONNECTED"),
                createParticipant("vm-003", "FAILED"), // 失败的VM应被排除
                createParticipant("vm-004", "DISCONNECTED") // 断连的VM应被排除
        );
    }

    private TaskParticipant createParticipant(String vmId, String status) {
        TaskParticipant participant = new TaskParticipant();
        participant.setVmId(vmId);
        participant.setStatus(ParticipantStatus.fromCode(status));
        participant.setTaskId(testTaskId);
        return participant;
    }

    private ModelDistribution createDistribution(String vmId, String status) {
        ModelDistribution distribution = new ModelDistribution();
        distribution.setId(UUID.randomUUID().toString().replace("-", ""));
        distribution.setModelId(mockModel.getId());
        distribution.setVmId(vmId);
        distribution.setDistributionStatus(status);
        distribution.setCreatedAt(LocalDateTime.now());
        return distribution;
    }

    @Test
    void testRecordAck_NewDistribution_ShouldCreateRecordAndUpdateCache() {
        // Given
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);
        when(modelDistributionMapper.selectByModelIdAndVmId(mockModel.getId(), testVmId))
                .thenReturn(null); // 没有现有记录
        when(modelDistributionMapper.insertModelDistribution(any(ModelDistribution.class)))
                .thenReturn(1);
        doNothing().when(ackCacheService).updateAckStatus(anyString(), anyString(),
                any(VmAckTracking.AckType.class), any(VmAckTracking.AckStatus.class));
        doNothing().when(ackCacheService).updateAckProgress(anyString(), any(VmAckTracking.AckType.class));

        // When
        boolean result = vmAckTracker.recordAck(testTaskId, testVmId, testRoundNumber);

        // Then
        assertTrue(result);
        verify(modelDistributionMapper).insertModelDistribution(any(ModelDistribution.class));
        verify(modelDistributionMapper, never()).updateDistributionStatus(anyString(),
                anyString(), any(), any());
        // 验证缓存更新
        verify(ackCacheService).updateAckStatus(eq(testTaskId), eq(testVmId),
                eq(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST), eq(VmAckTracking.AckStatus.SUCCESS));
        verify(ackCacheService).updateAckProgress(eq(testTaskId), eq(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST));
    }

    @Test
    void testRecordAck_ExistingDistribution_ShouldUpdateRecordAndCache() {
        // Given
        ModelDistribution existingDistribution = createDistribution(testVmId, "PENDING");
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);
        when(modelDistributionMapper.selectByModelIdAndVmId(mockModel.getId(), testVmId))
                .thenReturn(existingDistribution);
        when(modelDistributionMapper.updateDistributionStatus(
                eq(existingDistribution.getId()), eq("COMPLETED"), any(LocalDateTime.class), isNull()))
                .thenReturn(1);
        when(modelDistributionMapper.updateVerificationStatus(
                eq(existingDistribution.getId()), eq(true), any(LocalDateTime.class)))
                .thenReturn(1);
        doNothing().when(ackCacheService).updateAckStatus(anyString(), anyString(),
                any(VmAckTracking.AckType.class), any(VmAckTracking.AckStatus.class));
        doNothing().when(ackCacheService).updateAckProgress(anyString(), any(VmAckTracking.AckType.class));

        // When
        boolean result = vmAckTracker.recordAck(testTaskId, testVmId, testRoundNumber);

        // Then
        assertTrue(result);
        verify(modelDistributionMapper).updateDistributionStatus(
                eq(existingDistribution.getId()), eq("COMPLETED"), any(LocalDateTime.class), isNull());
        verify(modelDistributionMapper).updateVerificationStatus(
                eq(existingDistribution.getId()), eq(true), any(LocalDateTime.class));
        // 验证缓存更新
        verify(ackCacheService).updateAckStatus(eq(testTaskId), eq(testVmId),
                eq(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST), eq(VmAckTracking.AckStatus.SUCCESS));
        verify(ackCacheService).updateAckProgress(eq(testTaskId), eq(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST));
    }

    @Test
    void testRecordAck_ModelNotFound_ShouldFailWithoutCacheUpdate() {
        // Given
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(null);

        // When
        boolean result = vmAckTracker.recordAck(testTaskId, testVmId, testRoundNumber);

        // Then
        assertFalse(result);
        verify(modelDistributionMapper, never()).insertModelDistribution(any());
        verify(modelDistributionMapper, never()).updateDistributionStatus(anyString(),
                anyString(), any(), any());
        // 验证缓存不会被更新
        verify(ackCacheService, never()).updateAckStatus(anyString(), anyString(),
                any(VmAckTracking.AckType.class), any(VmAckTracking.AckStatus.class));
        verify(ackCacheService, never()).updateAckProgress(anyString(), any(VmAckTracking.AckType.class));
    }

    @Test
    void testAllVmsAcked_AllConfirmed_ShouldReturnTrue() {
        // Given
        when(taskParticipantsMapper.selectParticipantsByTaskId(testTaskId))
                .thenReturn(mockParticipants);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);

        Map<String, Object> progress = new HashMap<>();
        progress.put("completed", 2L); // 2个VM已确认
        progress.put("total", 3L);
        when(modelDistributionMapper.getDistributionProgress(mockModel.getId()))
                .thenReturn(progress);

        // When
        boolean result = vmAckTracker.allVmsAcked(testTaskId, testRoundNumber);

        // Then
        assertTrue(result); // 2个活跃VM都已确认（vm-001, vm-002）
    }

    @Test
    void testAllVmsAcked_NotAllConfirmed_ShouldReturnFalse() {
        // Given
        when(taskParticipantsMapper.selectParticipantsByTaskId(testTaskId))
                .thenReturn(mockParticipants);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);

        Map<String, Object> progress = new HashMap<>();
        progress.put("completed", 1L); // 只有1个VM确认
        progress.put("total", 2L);
        when(modelDistributionMapper.getDistributionProgress(mockModel.getId()))
                .thenReturn(progress);

        // When
        boolean result = vmAckTracker.allVmsAcked(testTaskId, testRoundNumber);

        // Then
        assertFalse(result); // 还有VM未确认
    }

    @Test
    void testAllVmsAcked_NoActiveParticipants_ShouldReturnFalse() {
        // Given - 只有失败和断连的参与者
        List<TaskParticipant> inactiveParticipants = Arrays.asList(
                createParticipant("vm-003", "FAILED"),
                createParticipant("vm-004", "DISCONNECTED")
        );
        when(taskParticipantsMapper.selectParticipantsByTaskId(testTaskId))
                .thenReturn(inactiveParticipants);

        // When
        boolean result = vmAckTracker.allVmsAcked(testTaskId, testRoundNumber);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetPendingVms_SomeUnconfirmed_ShouldReturnPendingList() {
        // Given
        when(taskParticipantsMapper.selectParticipantsByTaskId(testTaskId))
                .thenReturn(mockParticipants);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);

        List<Map<String, Object>> distributionsWithVm = Arrays.asList(
                createDistributionRecord("vm-001", "COMPLETED"), // 已确认
                createDistributionRecord("vm-002", "PENDING")    // 未确认
        );
        when(modelDistributionMapper.selectByModelIdWithVmInfo(mockModel.getId()))
                .thenReturn(distributionsWithVm);

        // When
        List<String> result = vmAckTracker.getPendingVms(testTaskId, testRoundNumber);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.contains("vm-002"));
        assertFalse(result.contains("vm-001")); // 已确认的不应在列表中
    }

    private Map<String, Object> createDistributionRecord(String vmId, String status) {
        Map<String, Object> record = new HashMap<>();
        record.put("vm_id", vmId);
        record.put("distribution_status", status);
        return record;
    }

    @Test
    void testGetPendingVms_AllConfirmed_ShouldReturnEmptyList() {
        // Given
        when(taskParticipantsMapper.selectParticipantsByTaskId(testTaskId))
                .thenReturn(mockParticipants);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);

        List<Map<String, Object>> distributionsWithVm = Arrays.asList(
                createDistributionRecord("vm-001", "COMPLETED"),
                createDistributionRecord("vm-002", "COMPLETED")
        );
        when(modelDistributionMapper.selectByModelIdWithVmInfo(mockModel.getId()))
                .thenReturn(distributionsWithVm);

        // When
        List<String> result = vmAckTracker.getPendingVms(testTaskId, testRoundNumber);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testHandleAckTimeout_WithTimeoutDistributions_ShouldMarkAsFailed() {
        // Given
        List<ModelDistribution> timeoutDistributions = Arrays.asList(
                createTimeoutDistribution("vm-001"),
                createTimeoutDistribution("vm-002")
        );
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);
        when(modelDistributionMapper.selectTimeoutDistributions(5))
                .thenReturn(timeoutDistributions);
        when(modelDistributionMapper.updateDistributionStatus(anyString(), eq("FAILED"),
                any(LocalDateTime.class), anyString())).thenReturn(1);

        // When
        vmAckTracker.handleAckTimeout(testTaskId, testRoundNumber, 5);

        // Then
        verify(modelDistributionMapper, times(2)).updateDistributionStatus(
                anyString(), eq("FAILED"), any(LocalDateTime.class), contains("ACK超时"));
    }

    private ModelDistribution createTimeoutDistribution(String vmId) {
        ModelDistribution distribution = createDistribution(vmId, "PENDING");
        distribution.setModelId(mockModel.getId()); // 确保模型ID匹配
        return distribution;
    }

    @Test
    void testInitializeRoundDistributions_Success_ShouldCreateDistributions() {
        // Given
        when(taskParticipantsMapper.selectParticipantsByTaskId(testTaskId))
                .thenReturn(mockParticipants);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);
        when(modelDistributionMapper.batchInsertModelDistributions(anyList()))
                .thenReturn(2); // 成功插入2条记录（排除失败和断连的VM）

        // When
        boolean result = vmAckTracker.initializeRoundDistributions(testTaskId, testRoundNumber);

        // Then
        assertTrue(result);
        verify(modelDistributionMapper).batchInsertModelDistributions(argThat(list ->
                list.size() == 2 && // 只为活跃的VM创建记录
                list.stream().allMatch(dist -> dist.getDistributionStatus().equals("PENDING"))
        ));
    }

    @Test
    void testGetAckProgress_WithValidModel_ShouldReturnProgress() {
        // Given
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);

        Map<String, Object> expectedProgress = new HashMap<>();
        expectedProgress.put("completed", 2L);
        expectedProgress.put("total", 3L);
        expectedProgress.put("pending", 1L);
        when(modelDistributionMapper.getDistributionProgress(mockModel.getId()))
                .thenReturn(expectedProgress);

        // When
        Map<String, Object> result = vmAckTracker.getAckProgress(testTaskId, testRoundNumber);

        // Then
        assertEquals(expectedProgress, result);
    }

    @Test
    void testResetRoundAcks_Success_ShouldResetDistributions() {
        // Given
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);
        when(modelDistributionMapper.resetDistributionStatus(mockModel.getId()))
                .thenReturn(3);

        // When
        boolean result = vmAckTracker.resetRoundAcks(testTaskId, testRoundNumber);

        // Then
        assertTrue(result);
        verify(modelDistributionMapper).resetDistributionStatus(mockModel.getId());
    }

    @Test
    void testWithNullInputs_ShouldHandleGracefully() {
        // 测试空值输入
        assertFalse(vmAckTracker.recordAck(null, testVmId, testRoundNumber));
        assertFalse(vmAckTracker.recordAck(testTaskId, null, testRoundNumber));
        assertFalse(vmAckTracker.recordAck(testTaskId, testVmId, null));

        assertFalse(vmAckTracker.allVmsAcked(null, testRoundNumber));
        assertFalse(vmAckTracker.allVmsAcked(testTaskId, null));

        assertTrue(vmAckTracker.getPendingVms(null, testRoundNumber).isEmpty());
        assertTrue(vmAckTracker.getPendingVms(testTaskId, null).isEmpty());

        assertFalse(vmAckTracker.initializeRoundDistributions(null, testRoundNumber));
        assertFalse(vmAckTracker.initializeRoundDistributions(testTaskId, null));

        assertTrue(vmAckTracker.getAckProgress(null, testRoundNumber).isEmpty());
        assertTrue(vmAckTracker.getAckProgress(testTaskId, null).isEmpty());

        assertFalse(vmAckTracker.resetRoundAcks(null, testRoundNumber));
        assertFalse(vmAckTracker.resetRoundAcks(testTaskId, null));
    }

    @Test
    void testConcurrentAckRecording_ShouldMaintainConsistency() {
        // 模拟并发ACK记录场景
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);
        when(modelDistributionMapper.selectByModelIdAndVmId(mockModel.getId(), testVmId))
                .thenReturn(null);

        // 第一次成功，第二次失败（模拟并发冲突）
        when(modelDistributionMapper.insertModelDistribution(any(ModelDistribution.class)))
                .thenReturn(1)
                .thenReturn(0);

        // When
        boolean result1 = vmAckTracker.recordAck(testTaskId, testVmId, testRoundNumber);
        boolean result2 = vmAckTracker.recordAck(testTaskId, testVmId, testRoundNumber);

        // Then
        assertTrue(result1);
        assertFalse(result2); // 第二次应该失败，避免重复记录
    }

    /**
     * 测试缓存错误回滚机制
     */
    @Test
    void testRecordAck_CacheError_ShouldTriggerRollback() {
        // Given
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);
        when(modelDistributionMapper.selectByModelIdAndVmId(mockModel.getId(), testVmId))
                .thenReturn(null);
        when(modelDistributionMapper.insertModelDistribution(any(ModelDistribution.class)))
                .thenReturn(1);

        // 模拟缓存更新失败
        doThrow(new RuntimeException("Cache update failed"))
                .when(ackCacheService).updateAckStatus(anyString(), anyString(),
                        any(VmAckTracking.AckType.class), any(VmAckTracking.AckStatus.class));
        doNothing().when(ackCacheService).clearAckTypeCache(anyString(), any(VmAckTracking.AckType.class));

        // When
        boolean result = vmAckTracker.recordAck(testTaskId, testVmId, testRoundNumber);

        // Then
        assertFalse(result); // 应该失败
        verify(ackCacheService).clearAckTypeCache(eq(testTaskId), eq(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST));
    }

    /**
     * 测试waitForAllAcknowledgments缓存查询功能
     */
    @Test
    void testWaitForAllAcknowledgments_UsingCache_ShouldSucceed() {
        // Given
        Set<String> taskParticipants = new HashSet<>(Arrays.asList("vm-001", "vm-002", "vm-003"));
        when(ackCacheService.getTaskParticipants(testTaskId))
                .thenReturn(taskParticipants);

        // 模拟进度变化：从未完成到完成
        AckProgress initialProgress = AckProgress.builder()
                .taskId(testTaskId)
                .ackType(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST)
                .totalVms(3)
                .acknowledgedVms(1)
                .allCompleted(false)
                .allSuccess(false)
                .progressPercentage(33.3)
                .build();

        AckProgress finalProgress = AckProgress.builder()
                .taskId(testTaskId)
                .ackType(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST)
                .totalVms(3)
                .acknowledgedVms(3)
                .allCompleted(true)
                .allSuccess(true)
                .progressPercentage(100.0)
                .build();

        when(ackCacheService.getAckProgress(eq(testTaskId), eq(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST)))
                .thenReturn(initialProgress)  // 第一次查询
                .thenReturn(finalProgress);   // 第二次查询

        // When
        boolean result = vmAckTracker.waitForAllAcknowledgments(testTaskId,
                VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST, 5000, 100);

        // Then
        assertTrue(result);
        verify(ackCacheService, atLeast(2)).getAckProgress(eq(testTaskId),
                eq(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST));
    }

    /**
     * 测试waitForAllAcknowledgments超时场景
     */
    @Test
    void testWaitForAllAcknowledgments_Timeout_ShouldReturnFalse() {
        // Given
        Set<String> taskParticipants = new HashSet<>(Arrays.asList("vm-001", "vm-002", "vm-003"));
        when(ackCacheService.getTaskParticipants(testTaskId))
                .thenReturn(taskParticipants);

        // 模拟始终未完成的进度
        AckProgress progress = AckProgress.builder()
                .taskId(testTaskId)
                .ackType(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST)
                .totalVms(3)
                .acknowledgedVms(2)
                .allCompleted(false)
                .allSuccess(false)
                .progressPercentage(66.7)
                .build();

        when(ackCacheService.getAckProgress(eq(testTaskId), eq(VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST)))
                .thenReturn(progress);

        // When
        long startTime = System.currentTimeMillis();
        boolean result = vmAckTracker.waitForAllAcknowledgments(testTaskId,
                VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST, 500, 100); // 500ms超时
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertFalse(result);
        assertTrue(duration >= 500, "应该等待至少500ms");
        assertTrue(duration < 1000, "不应该等待超过1秒");
    }

    @Test
    void testFilterActiveParticipants_ShouldExcludeFailedAndDisconnected() {
        // Given
        List<TaskParticipant> mixedParticipants = Arrays.asList(
                createParticipant("vm-001", "CONNECTED"),
                createParticipant("vm-002", "TRAINING"),
                createParticipant("vm-003", "FAILED"),      // 应被排除
                createParticipant("vm-004", "DISCONNECTED"), // 应被排除
                createParticipant("vm-005", "COMPLETED")
        );

        when(taskParticipantsMapper.selectParticipantsByTaskId(testTaskId))
                .thenReturn(mixedParticipants);
        when(globalModelMapper.selectByTaskIdAndRound(testTaskId, testRoundNumber))
                .thenReturn(mockModel);

        List<Map<String, Object>> distributionsWithVm = Arrays.asList(
                createDistributionRecord("vm-001", "COMPLETED"),
                createDistributionRecord("vm-002", "PENDING")
                // vm-005 没有分发记录，应在pending列表中
        );
        when(modelDistributionMapper.selectByModelIdWithVmInfo(mockModel.getId()))
                .thenReturn(distributionsWithVm);

        // When
        List<String> pendingVms = vmAckTracker.getPendingVms(testTaskId, testRoundNumber);

        // Then
        assertEquals(2, pendingVms.size());
        assertTrue(pendingVms.contains("vm-002"));
        assertTrue(pendingVms.contains("vm-005"));
        assertFalse(pendingVms.contains("vm-003")); // FAILED应被排除
        assertFalse(pendingVms.contains("vm-004")); // DISCONNECTED应被排除
    }
}