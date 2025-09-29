"""
任务上下文管理 - 单任务状态和执行控制
"""

import time
import threading
import logging
from typing import Dict, Any, Optional
from enum import Enum

logger = logging.getLogger(__name__)


class TaskStatus(Enum):
    """任务状态枚举"""
    INITIALIZING = "INITIALIZING"
    READY = "READY"
    TRAINING = "TRAINING"
    GRADIENT_READY = "GRADIENT_READY"
    WAITING = "WAITING"
    PAUSED = "PAUSED"
    ERROR = "ERROR"
    COMPLETED = "COMPLETED"
    CLEANED = "CLEANED"


class TaskContext:
    """任务上下文管理"""

    def __init__(self, task_id: str, config: Dict[str, Any]):
        self.task_id = task_id
        self.config = config
        self.status = TaskStatus.INITIALIZING
        self.current_round = 0
        self.total_rounds = config.get("totalRounds", 10)
        self.progress = 0.0
        self.last_activity = time.time()

        # 训练相关
        self.executor = None
        self.training_thread = None
        self.is_training = False
        self.is_paused = False

        # 数据和模型
        self.training_data = None
        self.current_model = None
        self.gradient_data = None

        # 错误处理
        self.error_count = 0
        self.last_error = None

        # 性能监控
        self.training_times = []
        self.resource_usage = {
            "cpu_percent": 0.0,
            "memory_mb": 0.0,
            "training_samples": 0
        }

        # 轮次结果历史
        self.round_results = {}
        self.aggregation_history = []

        # 线程同步
        self._lock = threading.RLock()
        self._pause_event = threading.Event()
        self._pause_event.set()  # 初始为非暂停状态

        logger.info(f"创建任务上下文: {task_id}")

    def initialize(self) -> bool:
        """初始化任务"""
        with self._lock:
            try:
                # 创建任务执行器
                self.executor = self._create_executor()
                if not self.executor:
                    raise ValueError("无法创建任务执行器")

                # 初始化执行器
                success = self.executor.initialize()
                if not success:
                    raise RuntimeError("任务执行器初始化失败")

                # 加载初始模型
                initial_model = self.config.get("initialGlobalModel")
                if initial_model:
                    success = self.executor.load_global_model(initial_model)
                    if not success:
                        raise RuntimeError("初始模型加载失败")

                # 加载训练数据
                dataset_id = self.config.get("localTrainingConfig", {}).get("datasetId")
                if dataset_id:
                    success = self.executor.load_training_data(dataset_id)
                    if not success:
                        raise RuntimeError("训练数据加载失败")

                self.status = TaskStatus.READY
                self.last_activity = time.time()

                logger.info(f"任务 {self.task_id} 初始化成功")
                return True

            except Exception as e:
                logger.error(f"任务 {self.task_id} 初始化失败: {e}")
                self.status = TaskStatus.ERROR
                self.error_count += 1
                self.last_error = str(e)
                return False

    def _create_executor(self):
        """创建任务执行器"""
        try:
            # 导入新的 TaskExecutor
            from ..federated.task_executor import TaskExecutor

            algorithm = self.config.get("federatedAlgorithm")
            training_config = self.config.get("localTrainingConfig", {})

            executor = TaskExecutor(
                task_id=self.task_id,
                algorithm=algorithm,
                config=training_config
            )

            return executor

        except ImportError as e:
            # 如果 TaskExecutor 导入失败，创建一个模拟执行器
            logger.warning(f"TaskExecutor 导入失败: {e}，使用模拟执行器")
            return MockTaskExecutor(self.task_id, self.config)
        except Exception as e:
            logger.error(f"创建执行器失败: {e}")
            return MockTaskExecutor(self.task_id, self.config)

    def start_round_training(self, round_number: int, round_config: Dict[str, Any]) -> bool:
        """开始轮次训练"""
        with self._lock:
            if self.is_training:
                logger.warning(f"任务 {self.task_id} 已在训练中")
                return False

            if self.status != TaskStatus.READY:
                logger.warning(f"任务 {self.task_id} 状态不正确: {self.status}")
                return False

            self.current_round = round_number
            self.status = TaskStatus.TRAINING
            self.is_training = True
            self.progress = 0.0
            self.last_activity = time.time()

            # 启动训练线程
            self.training_thread = threading.Thread(
                target=self._execute_training,
                args=(round_config,),
                daemon=True,
                name=f"training-{self.task_id}-{round_number}"
            )
            self.training_thread.start()

            logger.info(f"任务 {self.task_id} 开始第 {round_number} 轮训练")
            return True

    def _execute_training(self, round_config: Dict[str, Any]):
        """执行训练（在独立线程中）"""
        start_time = time.time()

        try:
            # 检查暂停状态
            self._pause_event.wait()

            if not self.executor:
                raise RuntimeError("任务执行器未初始化")

            # 更新进度
            self._update_progress(10.0)

            # 执行本地训练
            training_result = self.executor.train_round(round_config)

            # 检查暂停状态
            self._pause_event.wait()

            # 更新进度
            self._update_progress(80.0)

            # 提取梯度
            gradients = self.executor.extract_gradients()

            # 更新进度
            self._update_progress(100.0)

            # 记录训练时间
            training_time = time.time() - start_time
            self.training_times.append(training_time)

            # 更新状态
            with self._lock:
                self.status = TaskStatus.GRADIENT_READY
                self.is_training = False
                self.gradient_data = gradients
                self.last_activity = time.time()

            # 通知父客户端上传梯度
            self._notify_gradient_ready(gradients, training_result)

            logger.info(f"任务 {self.task_id} 第 {self.current_round} 轮训练完成，用时 {training_time:.2f}s")

        except Exception as e:
            logger.error(f"任务 {self.task_id} 训练失败: {e}")

            with self._lock:
                self.status = TaskStatus.ERROR
                self.is_training = False
                self.error_count += 1
                self.last_error = str(e)

            # 通知错误
            self._notify_training_error(str(e))

    def _update_progress(self, progress: float):
        """更新训练进度"""
        with self._lock:
            self.progress = min(progress, 100.0)
            self.last_activity = time.time()

    def _notify_gradient_ready(self, gradients: Dict[str, Any], training_result: Dict[str, Any]):
        """通知梯度准备就绪"""
        try:
            # 通过回调函数通知父客户端上传梯度
            if hasattr(self, 'gradient_ready_callback') and self.gradient_ready_callback:
                self.gradient_ready_callback(self.task_id, self.current_round, gradients, training_result)
            else:
                logger.info(f"任务 {self.task_id} 梯度准备完成，等待上传")

        except Exception as e:
            logger.error(f"通知梯度就绪失败: {e}")
    
    def set_gradient_ready_callback(self, callback):
        """设置梯度准备就绪回调函数"""
        self.gradient_ready_callback = callback

    def _notify_training_error(self, error_message: str):
        """通知训练错误"""
        try:
            logger.error(f"任务 {self.task_id} 训练错误: {error_message}")

            # 在实际实现中，这里会调用类似以下的方法：
            # self.parent_client.report_training_error(self.task_id, self.current_round, error_message)

        except Exception as e:
            logger.error(f"报告训练错误失败: {e}")

    def update_global_model(self, model_data: Dict[str, Any]) -> bool:
        """更新全局模型"""
        with self._lock:
            try:
                if not self.executor:
                    raise RuntimeError("任务执行器未初始化")

                success = self.executor.update_global_model(model_data)

                if success:
                    self.status = TaskStatus.READY
                    self.progress = 0.0
                    self.last_activity = time.time()
                    self.gradient_data = None  # 清除旧的梯度数据

                    logger.info(f"任务 {self.task_id} 全局模型更新成功")
                else:
                    logger.error(f"任务 {self.task_id} 全局模型更新失败")

                return success

            except Exception as e:
                logger.error(f"任务 {self.task_id} 全局模型更新异常: {e}")
                self.error_count += 1
                self.last_error = str(e)
                return False

    def pause(self):
        """暂停任务"""
        with self._lock:
            if not self.is_paused:
                self.is_paused = True
                self._pause_event.clear()
                logger.info(f"任务 {self.task_id} 已暂停")

    def resume(self):
        """恢复任务"""
        with self._lock:
            if self.is_paused:
                self.is_paused = False
                self._pause_event.set()
                logger.info(f"任务 {self.task_id} 已恢复")

    def stop(self):
        """停止任务"""
        with self._lock:
            if self.is_training and self.training_thread:
                self.is_training = False
                # 不强制终止线程，让其自然结束

            self.status = TaskStatus.COMPLETED
            logger.info(f"任务 {self.task_id} 已停止")

    def handle_error(self, error_message: str):
        """处理错误"""
        with self._lock:
            self.error_count += 1
            self.last_error = error_message
            self.status = TaskStatus.ERROR
            logger.error(f"任务 {self.task_id} 处理错误: {error_message}")

    def get_resource_usage(self) -> Dict[str, Any]:
        """获取资源使用情况"""
        with self._lock:
            return self.resource_usage.copy()

    def get_statistics(self) -> Dict[str, Any]:
        """获取任务统计信息"""
        with self._lock:
            avg_training_time = (
                sum(self.training_times) / len(self.training_times)
                if self.training_times else 0.0
            )

            return {
                "task_id": self.task_id,
                "status": self.status.value,
                "current_round": self.current_round,
                "total_rounds": self.total_rounds,
                "progress": self.progress,
                "completed_rounds": len(self.training_times),
                "average_training_time": avg_training_time,
                "error_count": self.error_count,
                "last_error": self.last_error,
                "last_activity": self.last_activity,
                "resource_usage": self.resource_usage,
                "is_training": self.is_training,
                "is_paused": self.is_paused
            }

    def cleanup(self):
        """清理任务资源"""
        with self._lock:
            logger.info(f"清理任务 {self.task_id}")

            # 停止训练
            if self.is_training and self.training_thread:
                self.is_training = False
                self.training_thread.join(timeout=10)

            # 清理执行器
            if self.executor:
                if hasattr(self.executor, 'cleanup'):
                    self.executor.cleanup()

            # 清理数据
            self.training_data = None
            self.current_model = None
            self.gradient_data = None

            self.status = TaskStatus.CLEANED


class MockTaskExecutor:
    """模拟任务执行器 - 用于测试和开发阶段"""
    
    def __init__(self, task_id: str, config: Dict[str, Any]):
        self.task_id = task_id
        self.config = config
        self.algorithm = config.get("federatedAlgorithm", "FEDERATED_AVERAGING")
        
    def initialize(self) -> bool:
        """初始化模拟执行器"""
        logger.info(f"模拟执行器初始化: {self.task_id}")
        return True
    
    def load_global_model(self, model_data: Dict[str, Any]) -> bool:
        """加载全局模型"""
        logger.info(f"模拟加载全局模型: {self.task_id}")
        return True
    
    def load_training_data(self, dataset_id: str) -> bool:
        """加载训练数据"""
        logger.info(f"模拟加载训练数据: {self.task_id}, dataset: {dataset_id}")
        return True
    
    def train_round(self, round_config: Dict[str, Any]) -> Dict[str, Any]:
        """执行训练轮次"""
        logger.info(f"模拟训练轮次: {self.task_id}")
        
        # 模拟训练时间
        import random
        time.sleep(random.uniform(1, 3))
        
        return {
            "final_loss": random.uniform(0.1, 0.5),
            "final_accuracy": random.uniform(0.7, 0.95),
            "training_time": random.uniform(1, 3),
            "samples_count": random.randint(100, 1000)
        }
    
    def extract_gradients(self) -> Dict[str, Any]:
        """提取梯度"""
        logger.info(f"模拟提取梯度: {self.task_id}")
        
        # 模拟梯度数据
        import random
        return {
            "model_type": "RandomForest",
            "gradients": {
                "layer1": [random.random() for _ in range(10)],
                "layer2": [random.random() for _ in range(5)]
            },
            "gradient_norm": random.uniform(0.1, 1.0)
        }
    
    def update_global_model(self, model_data: Dict[str, Any]) -> bool:
        """更新全局模型"""
        logger.info(f"模拟更新全局模型: {self.task_id}")
        return True
    
    def cleanup(self):
        """清理资源"""
        logger.info(f"模拟清理资源: {self.task_id}")
