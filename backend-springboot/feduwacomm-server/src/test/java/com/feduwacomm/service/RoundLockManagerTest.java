package com.feduwacomm.service;

import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.enums.FederatedTaskStatus;
import com.feduwacomm.mapper.FederatedTasksMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RoundLockManager单元测试
 *
 * 测试轮次锁管理器的并发控制功能，包括锁获取、释放、
 * 超时处理和数据库乐观锁集成
 */
@ExtendWith(MockitoExtension.class)
class RoundLockManagerTest {

    @Mock
    private FederatedTasksMapper federatedTasksMapper;

    @InjectMocks
    private RoundLockManager roundLockManager;

    private String testTaskId;
    private FederatedTask mockTask;

    @BeforeEach
    void setUp() {
        testTaskId = "test-task-001";

        mockTask = new FederatedTask();
        mockTask.setId(testTaskId);
        mockTask.setStatus(FederatedTaskStatus.RUNNING);
        mockTask.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testAcquireRoundLock_Success_ShouldReturnTrue() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class))).thenReturn(1);

        // When
        boolean result = roundLockManager.acquireRoundLock(testTaskId);

        // Then
        assertTrue(result);
        assertTrue(roundLockManager.isLockHeld(testTaskId));
    }

    @Test
    void testAcquireRoundLock_TaskNotFound_ShouldReturnFalse() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(null);

        // When
        boolean result = roundLockManager.acquireRoundLock(testTaskId);

        // Then
        assertFalse(result);
        assertFalse(roundLockManager.isLockHeld(testTaskId));
    }

    @Test
    void testAcquireRoundLock_InvalidTaskStatus_ShouldReturnFalse() {
        // Given
        mockTask.setStatus(FederatedTaskStatus.COMPLETED);
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);

        // When
        boolean result = roundLockManager.acquireRoundLock(testTaskId);

        // Then
        assertFalse(result);
        assertFalse(roundLockManager.isLockHeld(testTaskId));
    }

    @Test
    void testAcquireRoundLock_OptimisticLockConflict_ShouldRetryAndSucceed() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class)))
                .thenReturn(0) // 第一次冲突
                .thenReturn(0) // 第二次冲突
                .thenReturn(1); // 第三次成功

        // When
        boolean result = roundLockManager.acquireRoundLock(testTaskId);

        // Then
        assertTrue(result);
        verify(federatedTasksMapper, times(4)).selectTaskById(testTaskId); // 3次重试 + 1次初始
        verify(federatedTasksMapper, times(3)).updateTaskStatus(eq(testTaskId),
                eq("RUNNING"), any(LocalDateTime.class));
    }

    @Test
    void testAcquireRoundLock_MaxRetriesExceeded_ShouldReturnFalse() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class))).thenReturn(0); // 总是冲突

        // When
        boolean result = roundLockManager.acquireRoundLock(testTaskId);

        // Then
        assertFalse(result);
        verify(federatedTasksMapper, times(3)).updateTaskStatus(eq(testTaskId),
                eq("RUNNING"), any(LocalDateTime.class)); // 最多重试3次
    }

    @Test
    void testReleaseRoundLock_Success_ShouldReleaseLock() {
        // Given - 先获取锁
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class))).thenReturn(1);
        boolean acquired = roundLockManager.acquireRoundLock(testTaskId);
        assertTrue(acquired); // 确保锁被获取

        // When
        roundLockManager.releaseRoundLock(testTaskId);

        // Then
        assertFalse(roundLockManager.isLockHeld(testTaskId));
    }

    @Test
    void testReleaseRoundLock_NotHeld_ShouldHandleGracefully() {
        // When - 释放未持有的锁
        roundLockManager.releaseRoundLock(testTaskId);

        // Then - 应该优雅处理，不抛异常
        assertFalse(roundLockManager.isLockHeld(testTaskId));
    }

    @Test
    void testIsLockHeld_WithHeldLock_ShouldReturnTrue() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class))).thenReturn(1);
        boolean acquired = roundLockManager.acquireRoundLock(testTaskId);
        assertTrue(acquired); // 确保锁被获取

        // When & Then
        assertTrue(roundLockManager.isLockHeld(testTaskId));
    }

    @Test
    void testIsLockHeld_WithoutLock_ShouldReturnFalse() {
        // When & Then
        assertFalse(roundLockManager.isLockHeld(testTaskId));
    }

    @Test
    void testForceReleaseLock_WithHeldLock_ShouldRelease() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class))).thenReturn(1);
        boolean acquired = roundLockManager.acquireRoundLock(testTaskId);
        assertTrue(acquired); // 确保锁被获取

        // When
        roundLockManager.forceReleaseLock(testTaskId);

        // Then
        assertFalse(roundLockManager.isLockHeld(testTaskId));
    }

    @Test
    void testGetLockStatus_WithHeldLock_ShouldReturnStatus() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class))).thenReturn(1);
        boolean acquired = roundLockManager.acquireRoundLock(testTaskId);
        assertTrue(acquired); // 确保锁被获取

        // When
        String status = roundLockManager.getLockStatus(testTaskId);

        // Then
        assertNotNull(status);
        assertTrue(status.contains("是否锁定=true"));
        assertTrue(status.contains("是否当前线程持有=true"));
    }

    @Test
    void testGetLockStatus_WithoutLock_ShouldReturnNotExist() {
        // When
        String status = roundLockManager.getLockStatus(testTaskId);

        // Then
        assertEquals("锁不存在", status);
    }

    @Test
    void testCleanupIdleLocks_ShouldRemoveUnusedLocks() {
        // Given - 获取并释放锁，创建空闲锁
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class))).thenReturn(1);

        boolean acquired = roundLockManager.acquireRoundLock(testTaskId);
        assertTrue(acquired); // 确保锁被获取
        roundLockManager.releaseRoundLock(testTaskId);

        int initialCount = roundLockManager.getLockCount();

        // When
        roundLockManager.cleanupIdleLocks();

        // Then
        assertEquals(initialCount - 1, roundLockManager.getLockCount());
    }

    @Test
    void testGetLockCount_ShouldReturnCorrectCount() {
        // Given
        int initialCount = roundLockManager.getLockCount();

        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class))).thenReturn(1);

        // When
        boolean acquired = roundLockManager.acquireRoundLock(testTaskId);
        assertTrue(acquired); // 确保锁被获取

        // Then
        assertEquals(initialCount + 1, roundLockManager.getLockCount());
    }

    @Test
    void testConcurrentLockAcquisition_ShouldPreventDuplicateAccess() throws InterruptedException {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class))).thenReturn(1);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        boolean[] results = new boolean[2];

        // When - 两个线程同时尝试获取锁
        executor.submit(() -> {
            try {
                startLatch.await();
                results[0] = roundLockManager.acquireRoundLock(testTaskId);
                Thread.sleep(100); // 持有锁一段时间
                if (results[0]) {
                    roundLockManager.releaseRoundLock(testTaskId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                completeLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                results[1] = roundLockManager.acquireRoundLock(testTaskId);
                if (results[1]) {
                    roundLockManager.releaseRoundLock(testTaskId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                completeLatch.countDown();
            }
        });

        startLatch.countDown();
        completeLatch.await(5, TimeUnit.SECONDS);

        // Then - 只有一个线程应该成功获取锁
        assertTrue(results[0] || results[1]); // 至少一个成功
        assertFalse(results[0] && results[1]); // 不能两个都成功

        executor.shutdown();
    }

    @Test
    void testLockTimeout_ShouldPreventDeadlock() throws InterruptedException {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class))).thenReturn(1);

        // 第一个线程获取锁但不释放
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        Future<Boolean> firstResult = executor.submit(() -> {
            boolean acquired = roundLockManager.acquireRoundLock(testTaskId);
            if (acquired) {
                firstLockAcquired.countDown();
                // 不释放锁，模拟死锁情况
                try {
                    Thread.sleep(60000); // 睡眠1分钟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return acquired;
        });

        // 等待第一个线程获取锁
        firstLockAcquired.await(5, TimeUnit.SECONDS);

        // 第二个线程尝试获取锁，应该超时
        Future<Boolean> secondResult = executor.submit(() ->
                roundLockManager.acquireRoundLock(testTaskId));

        // When & Then
        try {
            assertTrue(firstResult.get(1, TimeUnit.SECONDS));
            assertFalse(secondResult.get(35, TimeUnit.SECONDS)); // 应该在30秒超时内返回false
        } catch (ExecutionException e) {
            fail("不应该出现执行异常: " + e.getMessage());
        } catch (TimeoutException e) {
            fail("操作超时: " + e.getMessage());
        }

        executor.shutdownNow();
    }

    @Test
    void testWithNullInputs_ShouldHandleGracefully() {
        // 测试空值输入
        assertFalse(roundLockManager.acquireRoundLock(null));
        assertFalse(roundLockManager.acquireRoundLock(""));

        // 这些操作不应抛异常
        roundLockManager.releaseRoundLock(null);
        roundLockManager.releaseRoundLock("");

        assertFalse(roundLockManager.isLockHeld(null));
        assertFalse(roundLockManager.isLockHeld(""));

        roundLockManager.forceReleaseLock(null);
        roundLockManager.forceReleaseLock("");

        assertEquals("任务ID为空", roundLockManager.getLockStatus(null));
        assertEquals("任务ID为空", roundLockManager.getLockStatus(""));
    }

    @Test
    void testDatabaseException_ShouldHandleGracefully() {
        // Given
        when(federatedTasksMapper.selectTaskById(testTaskId))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // When
        boolean result = roundLockManager.acquireRoundLock(testTaskId);

        // Then
        assertFalse(result);
        assertFalse(roundLockManager.isLockHeld(testTaskId));
    }

    @Test
    void testConfiguredTaskStatus_ShouldAllowLockAcquisition() {
        // Given - CONFIGURED状态也应该允许获取锁
        mockTask.setStatus(FederatedTaskStatus.CONFIGURED);
        when(federatedTasksMapper.selectTaskById(testTaskId)).thenReturn(mockTask);
        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("CONFIGURED"),
                any(LocalDateTime.class))).thenReturn(1);

        // When
        boolean result = roundLockManager.acquireRoundLock(testTaskId);

        // Then
        assertTrue(result);
        assertTrue(roundLockManager.isLockHeld(testTaskId));
    }

    @Test
    void testVersionConflictSimulation_ShouldRetryCorrectly() {
        // Given - 模拟版本冲突：updated_at字段发生变化
        LocalDateTime originalTime = LocalDateTime.now().minusMinutes(5);
        LocalDateTime newTime = LocalDateTime.now();

        FederatedTask taskWithOldTime = new FederatedTask();
        taskWithOldTime.setId(testTaskId);
        taskWithOldTime.setStatus(FederatedTaskStatus.RUNNING);
        taskWithOldTime.setUpdatedAt(originalTime);

        FederatedTask taskWithNewTime = new FederatedTask();
        taskWithNewTime.setId(testTaskId);
        taskWithNewTime.setStatus(FederatedTaskStatus.RUNNING);
        taskWithNewTime.setUpdatedAt(newTime);

        when(federatedTasksMapper.selectTaskById(testTaskId))
                .thenReturn(taskWithOldTime)  // 第一次读取旧时间
                .thenReturn(taskWithNewTime)  // 重试时读取新时间
                .thenReturn(taskWithNewTime); // 第二次重试

        when(federatedTasksMapper.updateTaskStatus(eq(testTaskId), eq("RUNNING"),
                any(LocalDateTime.class)))
                .thenReturn(0)  // 第一次因为版本冲突失败
                .thenReturn(1); // 重试后成功

        // When
        boolean result = roundLockManager.acquireRoundLock(testTaskId);

        // Then
        assertTrue(result);
        verify(federatedTasksMapper, times(2)).updateTaskStatus(eq(testTaskId),
                eq("RUNNING"), any(LocalDateTime.class));
    }
}