"""
多任务管理器 - 任务隔离和并发控制
"""

import time
import threading
import logging
from typing import Dict, Any, Optional, List
from concurrent.futures import ThreadPoolExecutor
from threading import Semaphore

from .task_context import TaskContext, TaskStatus

logger = logging.getLogger(__name__)


class TaskManager:
    """多任务管理器"""

    def __init__(self, max_concurrent_tasks: int = 3):
        self.max_concurrent_tasks = max_concurrent_tasks
        self.active_tasks = {}  # taskId -> TaskContext
        self.task_executor = ThreadPoolExecutor(max_workers=max_concurrent_tasks)
        
        # 资源管理
        self.resource_manager = ResourceManager()
        self.concurrency_controller = ConcurrencyController(max_concurrent_tasks)
        
        # 统计信息
        self.stats = {
            "total_tasks_created": 0,
            "total_tasks_completed": 0,
            "total_tasks_failed": 0,
            "current_active_tasks": 0
        }
        
        # 线程锁
        self._lock = threading.RLock()
        
        logger.info(f"初始化任务管理器，最大并发任务数: {max_concurrent_tasks}")

    def can_accept_task(self) -> bool:
        """检查是否可以接受新任务"""
        with self._lock:
            return len(self.active_tasks) < self.max_concurrent_tasks

    def create_task(self, task_id: str, config: Dict[str, Any]) -> bool:
        """创建新任务"""
        with self._lock:
            if not self.can_accept_task():
                logger.warning(f"无法创建任务 {task_id}：已达最大并发任务数")
                return False

            if task_id in self.active_tasks:
                logger.warning(f"任务 {task_id} 已存在")
                return False

            try:
                # 分配资源
                resource_requirements = self._extract_resource_requirements(config)
                if not self.resource_manager.allocate_resources(task_id, resource_requirements):
                    logger.error(f"无法为任务 {task_id} 分配资源")
                    return False

                # 创建任务上下文
                task_context = TaskContext(task_id, config)
                
                # 初始化任务
                if not task_context.initialize():
                    logger.error(f"任务 {task_id} 初始化失败")
                    self.resource_manager.release_resources(task_id)
                    return False

                # 添加到活跃任务列表
                self.active_tasks[task_id] = task_context
                self.stats["total_tasks_created"] += 1
                self.stats["current_active_tasks"] = len(self.active_tasks)

                logger.info(f"任务 {task_id} 创建成功")
                return True

            except Exception as e:
                logger.error(f"创建任务 {task_id} 失败: {e}")
                self.resource_manager.release_resources(task_id)
                return False

    def remove_task(self, task_id: str) -> bool:
        """移除任务"""
        with self._lock:
            if task_id not in self.active_tasks:
                logger.warning(f"任务 {task_id} 不存在")
                return False

            try:
                task_context = self.active_tasks[task_id]
                
                # 停止任务
                task_context.stop()
                
                # 清理资源
                task_context.cleanup()
                self.resource_manager.release_resources(task_id)
                
                # 从活跃任务列表中移除
                del self.active_tasks[task_id]
                
                # 更新统计信息
                if task_context.status == TaskStatus.ERROR:
                    self.stats["total_tasks_failed"] += 1
                else:
                    self.stats["total_tasks_completed"] += 1
                
                self.stats["current_active_tasks"] = len(self.active_tasks)

                logger.info(f"任务 {task_id} 已移除")
                return True

            except Exception as e:
                logger.error(f"移除任务 {task_id} 失败: {e}")
                return False

    def get_task(self, task_id: str) -> Optional[TaskContext]:
        """获取任务上下文"""
        with self._lock:
            return self.active_tasks.get(task_id)

    def get_all_tasks(self) -> Dict[str, TaskContext]:
        """获取所有任务"""
        with self._lock:
            return self.active_tasks.copy()

    def get_task_count(self) -> int:
        """获取活跃任务数量"""
        with self._lock:
            return len(self.active_tasks)

    def get_tasks_by_status(self, status: TaskStatus) -> List[TaskContext]:
        """根据状态获取任务"""
        with self._lock:
            return [task for task in self.active_tasks.values() if task.status == status]

    def pause_all_tasks(self):
        """暂停所有任务"""
        with self._lock:
            for task_context in self.active_tasks.values():
                try:
                    task_context.pause()
                except Exception as e:
                    logger.error(f"暂停任务 {task_context.task_id} 失败: {e}")

    def resume_all_tasks(self):
        """恢复所有任务"""
        with self._lock:
            for task_context in self.active_tasks.values():
                try:
                    task_context.resume()
                except Exception as e:
                    logger.error(f"恢复任务 {task_context.task_id} 失败: {e}")

    def cleanup_completed_tasks(self):
        """清理已完成的任务"""
        with self._lock:
            completed_tasks = []
            for task_id, task_context in self.active_tasks.items():
                if task_context.status in [TaskStatus.COMPLETED, TaskStatus.ERROR, TaskStatus.CLEANED]:
                    completed_tasks.append(task_id)

            for task_id in completed_tasks:
                self.remove_task(task_id)

            if completed_tasks:
                logger.info(f"清理了 {len(completed_tasks)} 个已完成的任务")

    def get_statistics(self) -> Dict[str, Any]:
        """获取管理器统计信息"""
        with self._lock:
            task_status_count = {}
            for status in TaskStatus:
                task_status_count[status.value] = len(self.get_tasks_by_status(status))

            return {
                "max_concurrent_tasks": self.max_concurrent_tasks,
                "current_active_tasks": len(self.active_tasks),
                "task_status_distribution": task_status_count,
                "resource_usage": self.resource_manager.get_resource_stats(),
                "concurrency_stats": self.concurrency_controller.get_stats(),
                "total_stats": self.stats.copy()
            }

    def _extract_resource_requirements(self, config: Dict[str, Any]) -> Dict[str, Any]:
        """提取资源需求"""
        # 根据算法和配置估算资源需求
        algorithm = config.get("federatedAlgorithm", "FEDERATED_AVERAGING")
        local_config = config.get("localTrainingConfig", {})
        
        # 基础资源需求
        base_cpu = 25  # 25% CPU
        base_memory = 512  # 512MB 内存
        
        # 根据算法调整
        if algorithm in ["FEDERATED_NOVA", "SCAFFOLD"]:
            base_cpu *= 1.5
            base_memory *= 1.2
        
        # 根据数据集大小调整
        batch_size = local_config.get("batchSize", 32)
        if batch_size > 64:
            base_memory *= 1.5
        
        return {
            "cpu_percentage": min(base_cpu, 90),
            "memory_mb": min(base_memory, 2048),
            "io_quota": 100  # MB/s
        }

    def shutdown(self, wait: bool = True):
        """关闭任务管理器"""
        logger.info("正在关闭任务管理器")
        
        # 停止所有任务
        with self._lock:
            for task_context in self.active_tasks.values():
                try:
                    task_context.stop()
                    task_context.cleanup()
                except Exception as e:
                    logger.error(f"停止任务 {task_context.task_id} 失败: {e}")
        
        # 关闭线程池
        self.task_executor.shutdown(wait=wait)
        self.concurrency_controller.shutdown(wait=wait)
        
        logger.info("任务管理器已关闭")


class ResourceManager:
    """资源管理器"""
    
    def __init__(self):
        self.cpu_allocations = {}  # taskId -> cpu_percentage
        self.memory_limits = {}    # taskId -> memory_mb
        self.io_quotas = {}        # taskId -> io_quota
        self._lock = threading.Lock()

    def allocate_resources(self, task_id: str, requirements: Dict[str, Any]) -> bool:
        """为任务分配资源"""
        with self._lock:
            cpu_needed = requirements.get("cpu_percentage", 25)
            memory_needed = requirements.get("memory_mb", 512)
            io_needed = requirements.get("io_quota", 100)

            if self._can_allocate(cpu_needed, memory_needed, io_needed):
                self.cpu_allocations[task_id] = cpu_needed
                self.memory_limits[task_id] = memory_needed
                self.io_quotas[task_id] = io_needed
                
                logger.debug(f"为任务 {task_id} 分配资源: CPU {cpu_needed}%, 内存 {memory_needed}MB")
                return True
            else:
                logger.warning(f"无法为任务 {task_id} 分配足够资源")
                return False

    def release_resources(self, task_id: str):
        """释放任务资源"""
        with self._lock:
            released_cpu = self.cpu_allocations.pop(task_id, 0)
            released_memory = self.memory_limits.pop(task_id, 0)
            released_io = self.io_quotas.pop(task_id, 0)
            
            if released_cpu or released_memory or released_io:
                logger.debug(f"释放任务 {task_id} 资源: CPU {released_cpu}%, 内存 {released_memory}MB")

    def _can_allocate(self, cpu_needed: float, memory_needed: float, io_needed: float) -> bool:
        """检查是否有足够资源"""
        total_cpu = sum(self.cpu_allocations.values())
        total_memory = sum(self.memory_limits.values())
        total_io = sum(self.io_quotas.values())

        # 获取系统可用资源
        available_memory = self._get_available_memory()
        
        return (
            total_cpu + cpu_needed <= 90 and  # 保留10% CPU
            total_memory + memory_needed <= available_memory * 0.8 and  # 保留20%内存
            total_io + io_needed <= 1000  # 总IO限制
        )

    def _get_available_memory(self) -> float:
        """获取可用内存（MB）"""
        try:
            import psutil
            return psutil.virtual_memory().available / (1024 * 1024)
        except ImportError:
            return 4096  # 默认4GB

    def get_resource_stats(self) -> Dict[str, Any]:
        """获取资源统计"""
        with self._lock:
            return {
                "total_cpu_allocated": sum(self.cpu_allocations.values()),
                "total_memory_allocated": sum(self.memory_limits.values()),
                "total_io_allocated": sum(self.io_quotas.values()),
                "active_allocations": len(self.cpu_allocations),
                "cpu_allocations": self.cpu_allocations.copy(),
                "memory_allocations": self.memory_limits.copy()
            }


class ConcurrencyController:
    """并发控制器"""
    
    def __init__(self, max_concurrent_tasks: int = 3):
        self.max_concurrent_tasks = max_concurrent_tasks

        # 线程池管理
        self.training_executor = ThreadPoolExecutor(
            max_workers=max_concurrent_tasks,
            thread_name_prefix="training"
        )

        # 信号量控制
        self.training_semaphore = Semaphore(max_concurrent_tasks)

        # 任务状态跟踪
        self.active_futures = {}  # taskId -> Future
        self.stats = {
            "submitted_tasks": 0,
            "completed_tasks": 0,
            "failed_tasks": 0,
            "cancelled_tasks": 0
        }
        
        self._lock = threading.Lock()

    def submit_training_task(self, task_id: str, training_func, *args, **kwargs):
        """提交训练任务"""
        with self._lock:
            if task_id in self.active_futures:
                logger.warning(f"任务 {task_id} 已在执行中")
                return None

            def wrapped_training():
                with self.training_semaphore:
                    try:
                        result = training_func(*args, **kwargs)
                        self.stats["completed_tasks"] += 1
                        return result
                    except Exception as e:
                        self.stats["failed_tasks"] += 1
                        logger.error(f"训练任务 {task_id} 执行失败: {e}")
                        raise
                    finally:
                        # 清理完成的任务
                        with self._lock:
                            if task_id in self.active_futures:
                                del self.active_futures[task_id]

            future = self.training_executor.submit(wrapped_training)
            self.active_futures[task_id] = future
            self.stats["submitted_tasks"] += 1
            
            logger.debug(f"提交训练任务: {task_id}")
            return future

    def cancel_task(self, task_id: str) -> bool:
        """取消任务"""
        with self._lock:
            if task_id in self.active_futures:
                future = self.active_futures[task_id]
                success = future.cancel()
                if success:
                    del self.active_futures[task_id]
                    self.stats["cancelled_tasks"] += 1
                    logger.info(f"任务 {task_id} 已取消")
                else:
                    logger.warning(f"任务 {task_id} 无法取消（可能已开始执行）")
                return success
            return False

    def get_active_task_count(self) -> int:
        """获取活跃任务数量"""
        with self._lock:
            return len(self.active_futures)

    def is_task_running(self, task_id: str) -> bool:
        """检查任务是否正在运行"""
        with self._lock:
            if task_id in self.active_futures:
                future = self.active_futures[task_id]
                return not future.done()
            return False

    def get_stats(self) -> Dict[str, Any]:
        """获取并发控制统计"""
        with self._lock:
            return {
                "max_concurrent_tasks": self.max_concurrent_tasks,
                "active_tasks": len(self.active_futures),
                "available_slots": self.training_semaphore._value,
                "stats": self.stats.copy()
            }

    def shutdown(self, wait: bool = True):
        """关闭并发控制器"""
        logger.info("正在关闭并发控制器")
        
        # 取消所有未完成的任务
        with self._lock:
            for task_id in list(self.active_futures.keys()):
                self.cancel_task(task_id)
        
        # 关闭线程池
        self.training_executor.shutdown(wait=wait)
        
        logger.info("并发控制器已关闭")
