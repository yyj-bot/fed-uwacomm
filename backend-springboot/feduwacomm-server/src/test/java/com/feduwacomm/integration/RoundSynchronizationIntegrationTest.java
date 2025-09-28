package com.feduwacomm.integration;

import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.enums.AggregationMethod;
import com.feduwacomm.enums.ConnectionStatus;
import com.feduwacomm.enums.FederatedAlgorithm;
import com.feduwacomm.enums.FederatedTaskStatus;
import com.feduwacomm.enums.GlobalModelStatus;
import com.feduwacomm.enums.ParticipantRole;
import com.feduwacomm.enums.ParticipantStatus;
import com.feduwacomm.enums.RoundState;
import com.feduwacomm.enums.VmStatus;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.GlobalModelMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.service.RoundLockManager;
import com.feduwacomm.service.RoundStateManager;
import com.feduwacomm.service.VmAckTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 轮次同步集成测试
 *
 * 测试完整的轮次同步流程，包括：
 * 1. 轮次状态管理
 * 2. VM确认跟踪
 * 3. 并发锁控制
 * 4. 状态转换验证
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-27
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RoundSynchronizationIntegrationTest {

    @Autowired
    private RoundStateManager roundStateManager;

    @Autowired
    private VmAckTracker vmAckTracker;

    @Autowired
    private RoundLockManager roundLockManager;

    @Autowired
    private FederatedTasksMapper federatedTasksMapper;

    @Autowired
    private GlobalModelMapper globalModelMapper;

    @Autowired
    private TaskParticipantsMapper taskParticipantsMapper;

    @Autowired
    private VmInstancesMapper vmInstancesMapper;

    private String testTaskId;
    private Integer testRoundNumber;
    private FederatedTask testTask;
    private GlobalModel testModel;
    private List<String> testVmIds;

    @BeforeEach
    void setUp() {
        testTaskId = "integration-test-" + System.currentTimeMillis();
        testRoundNumber = 1;
        testVmIds = List.of("vm-001", "vm-002", "vm-003");

        // 先创建测试VM实例
        for (String vmId : testVmIds) {
            VmInstance vmInstance = new VmInstance();
            vmInstance.setId(vmId);
            vmInstance.setName("测试VM-" + vmId);
            vmInstance.setIpAddress("192.168.1." + vmId.substring(vmId.length() - 1));
            vmInstance.setPort(8080);
            vmInstance.setOsType("Ubuntu 20.04");
            vmInstance.setCpuCores(4);
            vmInstance.setMemoryMb(8192);
            vmInstance.setDiskGb(100);
            vmInstance.setStatus(VmStatus.RUNNING);
            vmInstance.setConnectionStatus(ConnectionStatus.CONNECTED);
            vmInstance.setCreatedAt(LocalDateTime.now());
            vmInstance.setUpdatedAt(LocalDateTime.now());
            vmInstancesMapper.insert(vmInstance);
        }

        // 创建测试任务
        testTask = new FederatedTask();
        testTask.setId(testTaskId);
        testTask.setTaskName("集成测试任务");
        testTask.setAlgorithm(FederatedAlgorithm.FEDERATED_AVERAGING);
        testTask.setStatus(FederatedTaskStatus.RUNNING);
        testTask.setCurrentRound(testRoundNumber);
        testTask.setTotalRounds(3);
        testTask.setBatchSize(32);
        testTask.setLearningRate(0.01);
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setUpdatedAt(LocalDateTime.now());
        federatedTasksMapper.insertTask(testTask);

        // 创建测试全局模型
        testModel = new GlobalModel();
        testModel.setId("model-" + System.currentTimeMillis());
        testModel.setTaskId(testTaskId);
        testModel.setRoundNumber(testRoundNumber);
        testModel.setAggregationMethod(AggregationMethod.FEDERATED_AVERAGING);
        testModel.setParticipantCount(testVmIds.size());
        testModel.setStatus(GlobalModelStatus.COMPLETED);
        testModel.setDistributionStatus("DISTRIBUTED");
        testModel.setCreatedAt(LocalDateTime.now());
        testModel.setUpdatedAt(LocalDateTime.now());
        globalModelMapper.insertGlobalModel(testModel);

        // 创建测试参与者
        for (String vmId : testVmIds) {
            TaskParticipant participant = new TaskParticipant();
            participant.setId("participant-" + vmId + "-" + System.currentTimeMillis());
            participant.setTaskId(testTaskId);
            participant.setVmId(vmId);
            participant.setRole(ParticipantRole.PARTICIPANT);
            participant.setStatus(ParticipantStatus.CONNECTED);
            participant.setJoinedAt(LocalDateTime.now());
            taskParticipantsMapper.insertParticipant(participant);
        }
    }

    @Test
    void testCompleteRoundSynchronizationFlow() {
        // 1. 验证初始状态
        RoundState initialState = roundStateManager.getCurrentRoundState(testTaskId);
        assertEquals(RoundState.INITIALIZING, initialState, "初始状态应该是READY");

        // 2. 测试状态转换：READY -> TRAINING
        boolean transitionResult = roundStateManager.transitionRoundState(
                testTaskId, RoundState.INITIALIZING, RoundState.TRAINING);
        assertTrue(transitionResult, "状态转换应该成功");

        // 验证状态已更新
        RoundState newState = roundStateManager.getCurrentRoundState(testTaskId);
        assertEquals(RoundState.TRAINING, newState, "状态应该已转换为TRAINING");

        // 3. 模拟训练完成，转换到聚合状态
        roundStateManager.transitionRoundState(testTaskId, RoundState.TRAINING, RoundState.AGGREGATING);
        assertEquals(RoundState.AGGREGATING, roundStateManager.getCurrentRoundState(testTaskId));

        // 4. 聚合完成，开始分发
        roundStateManager.transitionRoundState(testTaskId, RoundState.AGGREGATING, RoundState.DISTRIBUTING);
        assertEquals(RoundState.DISTRIBUTING, roundStateManager.getCurrentRoundState(testTaskId));

        // 5. 初始化分发记录
        boolean initResult = vmAckTracker.initializeRoundDistributions(testTaskId, testRoundNumber);
        assertTrue(initResult, "分发记录初始化应该成功");

        // 6. 分发完成，等待ACK
        roundStateManager.transitionRoundState(testTaskId, RoundState.DISTRIBUTING, RoundState.TRAINING);
        assertEquals(RoundState.TRAINING, roundStateManager.getCurrentRoundState(testTaskId));

        // 7. 模拟VM逐个发送ACK
        for (String vmId : testVmIds) {
            boolean ackResult = vmAckTracker.recordAck(testTaskId, vmId, testRoundNumber);
            assertTrue(ackResult, "VM " + vmId + " ACK记录应该成功");
        }

        // 8. 验证所有VM都已确认
        boolean allAcked = vmAckTracker.allVmsAcked(testTaskId, testRoundNumber);
        assertTrue(allAcked, "所有VM都应该已确认");

        // 9. 所有ACK完成，转换到READY状态
        roundStateManager.transitionRoundState(testTaskId, RoundState.TRAINING, RoundState.INITIALIZING);
        assertEquals(RoundState.INITIALIZING, roundStateManager.getCurrentRoundState(testTaskId));
    }

    @Test
    void testConcurrentRoundAdvancement() throws InterruptedException {
        // 测试并发轮次推进的安全性
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);

        boolean[] results = new boolean[threadCount];

        // 启动多个线程同时尝试推进轮次
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // 尝试获取锁并推进轮次
                    boolean lockAcquired = roundLockManager.acquireRoundLock(testTaskId);
                    if (lockAcquired) {
                        try {
                            // 模拟轮次推进操作
                            Thread.sleep(50); // 短暂持有锁
                            results[index] = roundStateManager.advanceRound(testTaskId);
                        } finally {
                            roundLockManager.releaseRoundLock(testTaskId);
                        }
                    } else {
                        results[index] = false;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // 启动所有线程
        startLatch.countDown();
        assertTrue(completeLatch.await(10, TimeUnit.SECONDS), "所有线程应该在10秒内完成");

        // 验证只有一个线程成功推进了轮次
        int successCount = 0;
        for (boolean result : results) {
            if (result) {
                successCount++;
            }
        }

        assertEquals(1, successCount, "只有一个线程应该成功推进轮次");

        // 验证轮次确实推进了
        FederatedTask updatedTask = federatedTasksMapper.selectTaskById(testTaskId);
        assertEquals(testRoundNumber + 1, updatedTask.getCurrentRound().intValue(),
                "轮次应该从 " + testRoundNumber + " 推进到 " + (testRoundNumber + 1));

        executor.shutdown();
    }

    @Test
    void testPartialAckHandling() {
        // 测试部分VM确认的处理

        // 1. 初始化分发记录
        boolean initResult = vmAckTracker.initializeRoundDistributions(testTaskId, testRoundNumber);
        assertTrue(initResult, "分发记录初始化应该成功");

        // 2. 只有部分VM发送ACK
        String firstVmId = testVmIds.get(0);
        String secondVmId = testVmIds.get(1);
        // 第三个VM不发送ACK

        vmAckTracker.recordAck(testTaskId, firstVmId, testRoundNumber);
        vmAckTracker.recordAck(testTaskId, secondVmId, testRoundNumber);

        // 3. 验证不是所有VM都已确认
        boolean allAcked = vmAckTracker.allVmsAcked(testTaskId, testRoundNumber);
        assertFalse(allAcked, "应该不是所有VM都已确认");

        // 4. 获取未确认的VM列表
        List<String> pendingVms = vmAckTracker.getPendingVms(testTaskId, testRoundNumber);
        assertEquals(1, pendingVms.size(), "应该有1个VM未确认");
        assertEquals(testVmIds.get(2), pendingVms.get(0), "未确认的应该是第三个VM");

        // 5. 第三个VM发送ACK
        vmAckTracker.recordAck(testTaskId, testVmIds.get(2), testRoundNumber);

        // 6. 现在所有VM都应该已确认
        allAcked = vmAckTracker.allVmsAcked(testTaskId, testRoundNumber);
        assertTrue(allAcked, "现在所有VM都应该已确认");
    }

    @Test
    void testAckProgressTracking() {
        // 测试ACK进度跟踪

        // 1. 初始化分发记录
        vmAckTracker.initializeRoundDistributions(testTaskId, testRoundNumber);

        // 2. 检查初始进度
        var initialProgress = vmAckTracker.getAckProgress(testTaskId, testRoundNumber);
        assertNotNull(initialProgress, "进度信息不应该为空");

        // 3. 逐个VM发送ACK并检查进度
        for (int i = 0; i < testVmIds.size(); i++) {
            vmAckTracker.recordAck(testTaskId, testVmIds.get(i), testRoundNumber);

            var progress = vmAckTracker.getAckProgress(testTaskId, testRoundNumber);
            assertNotNull(progress, "进度信息不应该为空");

            // 验证进度递增
            Long completedCount = (Long) progress.get("completed");
            assertNotNull(completedCount, "已完成数量不应该为空");
            assertEquals(i + 1, completedCount.intValue(), "已完成数量应该正确");
        }
    }

    @Test
    void testInvalidStateTransitions() {
        // 测试无效的状态转换

        // 1. 尝试无效转换：READY -> AGGREGATING（跳过TRAINING）
        boolean result1 = roundStateManager.transitionRoundState(
                testTaskId, RoundState.INITIALIZING, RoundState.AGGREGATING);
        assertFalse(result1, "无效的状态转换应该失败");

        // 2. 尝试无效转换：TRAINING -> WAITING_ACK（跳过中间状态）
        roundStateManager.transitionRoundState(testTaskId, RoundState.INITIALIZING, RoundState.TRAINING);
        boolean result2 = roundStateManager.transitionRoundState(
                testTaskId, RoundState.TRAINING, RoundState.TRAINING);
        assertFalse(result2, "无效的状态转换应该失败");

        // 3. 尝试相同状态转换
        boolean result3 = roundStateManager.transitionRoundState(
                testTaskId, RoundState.TRAINING, RoundState.TRAINING);
        assertFalse(result3, "相同状态转换应该失败");
    }

    @Test
    void testRoundModelCreation() {
        // 测试轮次模型创建

        Integer newRoundNumber = testRoundNumber + 1;

        // 1. 创建新轮次模型
        boolean createResult = roundStateManager.createRoundModel(testTaskId, newRoundNumber);
        assertTrue(createResult, "轮次模型创建应该成功");

        // 2. 验证模型已创建
        GlobalModel newModel = globalModelMapper.selectByTaskIdAndRound(testTaskId, newRoundNumber);
        assertNotNull(newModel, "新轮次模型应该存在");
        assertEquals(testTaskId, newModel.getTaskId(), "任务ID应该匹配");
        assertEquals(newRoundNumber, newModel.getRoundNumber(), "轮次号应该匹配");

        // 3. 重复创建同一轮次模型应该成功（幂等性）
        boolean duplicateResult = roundStateManager.createRoundModel(testTaskId, newRoundNumber);
        assertTrue(duplicateResult, "重复创建应该成功（幂等性）");
    }

    @Test
    void testLockCleanupAfterOperations() {
        // 测试操作完成后的锁清理

        // 1. 获取锁
        boolean lockAcquired = roundLockManager.acquireRoundLock(testTaskId);
        assertTrue(lockAcquired, "锁获取应该成功");

        // 2. 验证锁被持有
        assertTrue(roundLockManager.isLockHeld(testTaskId), "锁应该被持有");

        // 3. 释放锁
        roundLockManager.releaseRoundLock(testTaskId);

        // 4. 验证锁已释放
        assertFalse(roundLockManager.isLockHeld(testTaskId), "锁应该已释放");

        // 5. 清理空闲锁
        int initialLockCount = roundLockManager.getLockCount();
        roundLockManager.cleanupIdleLocks();
        int finalLockCount = roundLockManager.getLockCount();

        assertTrue(finalLockCount <= initialLockCount, "空闲锁应该被清理");
    }
}