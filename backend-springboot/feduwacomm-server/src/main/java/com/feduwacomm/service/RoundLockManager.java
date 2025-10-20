package com.feduwacomm.service;

import com.feduwacomm.mapper.FederatedTasksMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 轮次锁管理器
 *
 * 提供基于内存锁和数据库乐观锁的双重保护机制，防止多线程同时推进同一任务的轮次。
 * 采用无Redis的设计，使用ConcurrentHashMap + 数据库乐观锁实现并发控制。
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-27
 */
@Slf4j
@Component
public class RoundLockManager {

    /**
     * 任务级别的内存锁映射表
     * key: taskId, value: ReentrantLock
     */
    private final ConcurrentHashMap<String, ReentrantLock> taskLocks = new ConcurrentHashMap<>();

    /**
     * 锁超时时间（秒）
     */
    private static final int LOCK_TIMEOUT_SECONDS = 30;

    /**
     * 锁获取重试次数
     */
    private static final int MAX_RETRY_COUNT = 3;

    @Autowired
    private FederatedTasksMapper federatedTasksMapper;

    /**
     * 获取轮次锁
     * 使用内存锁 + 数据库乐观锁双重保护
     *
     * @param taskId 任务ID
     * @return 是否获取锁成功
     */
    public boolean acquireRoundLock(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("任务ID不能为空");
            return false;
        }

        // 第一层：内存锁保护
        ReentrantLock lock = taskLocks.computeIfAbsent(taskId, k -> new ReentrantLock());

        try {
            // 尝试获取内存锁
            boolean acquired = lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取内存锁超时: taskId={}, timeout={}秒", taskId, LOCK_TIMEOUT_SECONDS);
                return false;
            }

            log.debug("内存锁获取成功: taskId={}", taskId);

            // 第二层：数据库乐观锁保护
            boolean dbLockAcquired = acquireDatabaseLock(taskId);
            if (!dbLockAcquired) {
                log.warn("获取数据库锁失败: taskId={}", taskId);
                lock.unlock();
                return false;
            }

            log.debug("数据库锁获取成功: taskId={}", taskId);
            return true;

        } catch (InterruptedException e) {
            log.error("获取锁被中断: taskId={}", taskId, e);
            Thread.currentThread().interrupt();
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            return false;
        } catch (Exception e) {
            log.error("获取锁异常: taskId={}", taskId, e);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            return false;
        }
    }

    /**
     * 尝试获取轮次锁，支持超时
     *
     * @param taskId 任务ID
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否获取锁成功
     */
    public boolean tryAcquireRoundLock(String taskId, int timeoutSeconds) {
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("任务ID不能为空");
            return false;
        }

        try {
            ReentrantLock lock = taskLocks.computeIfAbsent(taskId, k -> new ReentrantLock(true));

            // 尝试在指定时间内获取锁
            if (lock.tryLock(timeoutSeconds, TimeUnit.SECONDS)) {
                log.debug("内存锁获取成功: taskId={}, timeout={}s", taskId, timeoutSeconds);
                return true;
            } else {
                log.warn("内存锁获取超时: taskId={}, timeout={}s", taskId, timeoutSeconds);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取内存锁被中断: taskId={}", taskId, e);
            return false;
        } catch (Exception e) {
            log.error("获取轮次锁异常: taskId={}", taskId, e);
            return false;
        }
    }

    /**
     * 释放轮次锁
     *
     * @param taskId 任务ID
     */
    public void releaseRoundLock(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("任务ID不能为空");
            return;
        }

        try {
            ReentrantLock lock = taskLocks.get(taskId);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("内存锁释放成功: taskId={}", taskId);
            } else {
                log.warn("内存锁未被当前线程持有或不存在: taskId={}", taskId);
            }
        } catch (Exception e) {
            log.error("释放锁异常: taskId={}", taskId, e);
        }
    }

    /**
     * 获取数据库乐观锁
     * 通过比较和更新操作实现乐观锁
     *
     * @param taskId 任务ID
     * @return 是否获取成功
     */
    private boolean acquireDatabaseLock(String taskId) {
        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            try {
                // 读取当前任务状态
                var task = federatedTasksMapper.selectTaskById(taskId);
                if (task == null) {
                    log.error("任务不存在: taskId={}", taskId);
                    return false;
                }

                // 检查任务状态是否允许轮次操作
                if (!"RUNNING".equals(task.getStatus().name()) && !"CONFIGURED".equals(task.getStatus().name())) {
                    log.warn("任务状态不允许轮次操作: taskId={}, status={}", taskId, task.getStatus());
                    return false;
                }

                String statusName = task.getStatus() != null ? task.getStatus().name() : null;
                if (statusName == null) {
                    log.warn("任务状态为空，无法执行乐观锁: taskId={}", taskId);
                    return false;
                }

                int affectedRows = federatedTasksMapper.updateTaskStatusWithVersion(
                    taskId,
                    statusName,
                    java.time.LocalDateTime.now(),
                    task.getVersion()
                );

                if (affectedRows > 0) {
                    log.debug("数据库乐观锁获取成功: taskId={}, retry={}", taskId, retry);
                    return true;
                } else {
                    log.debug("数据库乐观锁冲突，准备重试: taskId={}, retry={}", taskId, retry);

                    // 短暂等待后重试
                    if (retry < MAX_RETRY_COUNT - 1) {
                        Thread.sleep(50 + retry * 50); // 递增等待时间
                    }
                }

            } catch (InterruptedException e) {
                log.error("数据库锁获取被中断: taskId={}, retry={}", taskId, retry, e);
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                log.error("数据库锁获取异常: taskId={}, retry={}", taskId, retry, e);
                return false;
            }
        }

        log.warn("数据库乐观锁获取失败，超过最大重试次数: taskId={}, maxRetry={}", taskId, MAX_RETRY_COUNT);
        return false;
    }

    /**
     * 检查是否持有锁
     *
     * @param taskId 任务ID
     * @return 是否持有锁
     */
    public boolean isLockHeld(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return false;
        }

        ReentrantLock lock = taskLocks.get(taskId);
        return lock != null && lock.isHeldByCurrentThread();
    }

    /**
     * 强制释放锁（慎用）
     * 仅在异常情况下使用，例如系统恢复时清理残留锁
     *
     * @param taskId 任务ID
     */
    public void forceReleaseLock(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("任务ID不能为空");
            return;
        }

        try {
            ReentrantLock lock = taskLocks.get(taskId);
            if (lock != null) {
                // 检查锁状态
                if (lock.isLocked()) {
                    log.warn("强制释放锁: taskId={}, 锁状态=已锁定, 等待线程数={}",
                            taskId, lock.getQueueLength());

                    // 如果是当前线程持有，正常释放
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    } else {
                        // 非当前线程持有，需要移除锁对象
                        taskLocks.remove(taskId);
                        log.warn("移除非当前线程持有的锁: taskId={}", taskId);
                    }
                } else {
                    // 锁未被持有，直接移除
                    taskLocks.remove(taskId);
                    log.debug("移除未被持有的锁: taskId={}", taskId);
                }
            } else {
                log.debug("锁不存在，无需释放: taskId={}", taskId);
            }
        } catch (Exception e) {
            log.error("强制释放锁异常: taskId={}", taskId, e);
        }
    }

    /**
     * 获取锁状态信息（用于监控和调试）
     *
     * @param taskId 任务ID
     * @return 锁状态信息
     */
    public String getLockStatus(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return "任务ID为空";
        }

        ReentrantLock lock = taskLocks.get(taskId);
        if (lock == null) {
            return "锁不存在";
        }

        return String.format("锁状态: 是否锁定=%s, 持有数=%d, 等待线程数=%d, 是否当前线程持有=%s",
                           lock.isLocked(),
                           lock.getHoldCount(),
                           lock.getQueueLength(),
                           lock.isHeldByCurrentThread());
    }

    /**
     * 清理空闲锁（定期清理，防止内存泄露）
     */
    public void cleanupIdleLocks() {
        int removedCount = 0;
        for (var entry : taskLocks.entrySet()) {
            String taskId = entry.getKey();
            ReentrantLock lock = entry.getValue();

            if (!lock.isLocked() && lock.getQueueLength() == 0) {
                taskLocks.remove(taskId);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("清理空闲锁完成: 清理数量={}, 剩余锁数量={}", removedCount, taskLocks.size());
        }
    }

    /**
     * 获取当前锁数量（用于监控）
     *
     * @return 锁数量
     */
    public int getLockCount() {
        return taskLocks.size();
    }
}
