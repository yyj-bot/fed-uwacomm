# WebSocket 客户端实现

本文档详细描述 Python VM 中 WebSocket 客户端的具体实现，包括连接管理、消息处理和任务上下文管理。

## 1. 核心客户端类

### 1.1 FederatedLearningClient 基础实现

```python
"""
FedUWAComm WebSocket 客户端 v1.4
基于中心化架构的被动响应式设计
"""

import json
import time
import threading
import logging
import base64
from typing import Dict, Any, Optional, Callable, List
from enum import Enum
import websocket

logger = logging.getLogger(__name__)

class ConnectionStatus(Enum):
    """连接状态枚举"""
    DISCONNECTED = "DISCONNECTED"
    CONNECTING = "CONNECTING"
    CONNECTED = "CONNECTED"
    RECONNECTING = "RECONNECTING"
    ERROR = "ERROR"

class FederatedLearningClient:
    """
    联邦学习WebSocket客户端
    实现完全被动响应式的v1.4协议
    """

    def __init__(self, server_url: str, access_token: str, vm_id: str):
        """初始化客户端

        Args:
            server_url: WebSocket服务器URL
            access_token: 访问令牌
            vm_id: 虚拟机ID
        """
        self.server_url = server_url
        self.access_token = access_token
        self.vm_id = vm_id

        # 连接管理
        self.websocket = None
        self.status = ConnectionStatus.DISCONNECTED
        self.is_connected = False
        self.reconnect_attempts = 0
        self.max_reconnect_attempts = 5

        # 多任务管理
        self.active_tasks = {}  # taskId -> TaskContext
        self.max_concurrent_tasks = 3

        # 消息处理
        self.message_id_counter = 0
        self.message_lock = threading.Lock()
        self.message_router = None

        # 心跳管理
        self.heartbeat_interval = 30  # 秒
        self.heartbeat_thread = None
        self.heartbeat_stop_event = threading.Event()

        # 性能监控
        self.metrics = {
            "messages_sent": 0,
            "messages_received": 0,
            "connection_errors": 0,
            "task_failures": 0
        }

        logger.info(f"初始化FederatedLearningClient，VM ID: {vm_id}")

    def initialize(self):
        """初始化客户端组件"""
        from .message_router import MessageRouter
        self.message_router = MessageRouter(self)

    def connect(self) -> bool:
        """建立WebSocket连接"""
        try:
            self.status = ConnectionStatus.CONNECTING
            logger.info(f"正在连接到WebSocket服务器: {self.server_url}")

            # 构建连接URL
            url = f"{self.server_url}?token={self.access_token}&vmId={self.vm_id}"

            # 创建WebSocket连接
            self.websocket = websocket.WebSocketApp(
                url,
                on_open=self._on_open,
                on_message=self._on_message,
                on_error=self._on_error,
                on_close=self._on_close
            )

            # 启动连接（阻塞方式）
            self.websocket.run_forever()

            return self.is_connected

        except Exception as e:
            logger.error(f"连接WebSocket服务器失败: {e}")
            self.status = ConnectionStatus.ERROR
            self.metrics["connection_errors"] += 1
            return False

    def disconnect(self):
        """断开WebSocket连接"""
        logger.info("正在断开WebSocket连接")

        # 停止心跳
        if self.heartbeat_thread:
            self.heartbeat_stop_event.set()
            self.heartbeat_thread.join(timeout=5)

        # 清理所有任务
        for task_id in list(self.active_tasks.keys()):
            self._cleanup_task(task_id)

        # 关闭连接
        if self.websocket:
            self.websocket.close()

        self.is_connected = False
        self.status = ConnectionStatus.DISCONNECTED

    def _on_open(self, ws):
        """连接建立回调"""
        logger.info("WebSocket连接已建立")
        self.is_connected = True
        self.status = ConnectionStatus.CONNECTED
        self.reconnect_attempts = 0

        # 发送连接确认消息
        self._send_connect_message()

        # 启动心跳
        self._start_heartbeat()

    def _on_message(self, ws, message):
        """消息接收回调"""
        try:
            msg = json.loads(message)
            self.metrics["messages_received"] += 1
            self._handle_message(msg)
        except Exception as e:
            logger.error(f"处理消息失败: {e}")

    def _on_error(self, ws, error):
        """错误回调"""
        logger.error(f"WebSocket错误: {error}")
        self.status = ConnectionStatus.ERROR
        self.metrics["connection_errors"] += 1

    def _on_close(self, ws, close_status_code, close_msg):
        """连接关闭回调"""
        logger.warning(f"WebSocket连接已关闭: {close_status_code} - {close_msg}")
        self.is_connected = False
        self.status = ConnectionStatus.DISCONNECTED

        # 停止心跳
        if self.heartbeat_thread:
            self.heartbeat_stop_event.set()

        # 尝试重连
        self._attempt_reconnect()

    def _handle_message(self, message: Dict[str, Any]):
        """消息处理统一入口"""
        if self.message_router:
            self.message_router.route_message(message)
        else:
            logger.warning("消息路由器未初始化")

    def _send_connect_message(self):
        """发送连接确认消息"""
        connect_message = {
            "type": "CONNECT",
            "id": self._generate_message_id(),
            "timestamp": self._get_current_timestamp(),
            "vmId": self.vm_id,
            "data": {
                "vmId": self.vm_id,
                "capabilities": self._get_vm_capabilities()
            }
        }

        self._send_message(connect_message)

    def _get_vm_capabilities(self) -> Dict[str, Any]:
        """获取虚拟机能力信息"""
        import platform
        try:
            import psutil
            memory_gb = psutil.virtual_memory().total // (1024**3)
            cpu_cores = psutil.cpu_count()
        except ImportError:
            memory_gb = 8  # 默认值
            cpu_cores = 4  # 默认值

        return {
            "systemInfo": {
                "os": platform.system() + " " + platform.release(),
                "python": platform.python_version(),
                "memory": f"{memory_gb}GB",
                "cpu_cores": cpu_cores
            },
            "maxConcurrentTasks": self.max_concurrent_tasks,
            "supportedAlgorithms": [
                "FEDERATED_AVERAGING",
                "FEDERATED_PROXIMAL",
                "FEDERATED_NOVA",
                "SCAFFOLD"
            ],
            "supportedDataTypes": [
                "ACOUSTIC",
                "ENVIRONMENT",
                "MODEL",
                "OTHER",
                "TEST_DATA",
                "SPECIAL_CHARS",
                "LONG_TEXT"
            ]
        }

    def _send_message(self, message: Dict[str, Any]) -> bool:
        """发送WebSocket消息"""
        if not self.is_connected or not self.websocket:
            logger.warning("WebSocket未连接，无法发送消息")
            return False

        try:
            message_str = json.dumps(message, ensure_ascii=False)
            self.websocket.send(message_str)
            self.metrics["messages_sent"] += 1
            logger.debug(f"发送消息: {message['type']}")
            return True
        except Exception as e:
            logger.error(f"发送消息失败: {e}")
            return False

    def _generate_message_id(self) -> str:
        """生成消息ID"""
        with self.message_lock:
            self.message_id_counter += 1
            return f"{self.vm_id}_{int(time.time())}_{self.message_id_counter}"

    def _get_current_timestamp(self) -> int:
        """获取当前时间戳（毫秒）"""
        return int(time.time() * 1000)
```

### 1.2 心跳机制实现

```python
def _start_heartbeat(self):
    """启动心跳机制"""
    self.heartbeat_stop_event.clear()
    self.heartbeat_thread = threading.Thread(
        target=self._heartbeat_loop,
        daemon=True
    )
    self.heartbeat_thread.start()
    logger.info("心跳机制已启动")

def _heartbeat_loop(self):
    """心跳循环"""
    while not self.heartbeat_stop_event.wait(self.heartbeat_interval):
        if self.is_connected:
            try:
                heartbeat_message = {
                    "type": "HEARTBEAT",
                    "id": self._generate_message_id(),
                    "timestamp": self._get_current_timestamp(),
                    "vmId": self.vm_id,
                    "data": {
                        "status": "ACTIVE",
                        "resourceUsage": self._get_resource_usage(),
                        "activeTasks": self._get_active_tasks_status(),
                        "metrics": self._get_performance_metrics()
                    }
                }

                self._send_message(heartbeat_message)
                logger.debug("发送心跳消息")

            except Exception as e:
                logger.error(f"发送心跳失败: {e}")

    logger.info("心跳循环已停止")

def _get_resource_usage(self) -> Dict[str, Any]:
    """获取资源使用情况"""
    try:
        import psutil
        return {
            "cpu_percent": psutil.cpu_percent(interval=1),
            "memory_percent": psutil.virtual_memory().percent,
            "disk_usage": psutil.disk_usage('/').percent,
            "network_io": {
                "bytes_sent": psutil.net_io_counters().bytes_sent,
                "bytes_recv": psutil.net_io_counters().bytes_recv
            },
            "load_average": psutil.getloadavg() if hasattr(psutil, 'getloadavg') else [0, 0, 0]
        }
    except ImportError:
        return {
            "cpu_percent": 0.0,
            "memory_percent": 0.0,
            "disk_usage": 0.0,
            "network_io": {"bytes_sent": 0, "bytes_recv": 0},
            "load_average": [0, 0, 0]
        }

def _get_active_tasks_status(self) -> List[Dict[str, Any]]:
    """获取所有活跃任务状态"""
    task_status = []
    for task_id, task_context in self.active_tasks.items():
        task_status.append({
            "taskId": task_id,
            "status": task_context.status,
            "currentRound": task_context.current_round,
            "progress": task_context.progress,
            "lastActivity": task_context.last_activity,
            "errorCount": getattr(task_context, 'error_count', 0),
            "resourceUsage": task_context.get_resource_usage()
        })
    return task_status

def _get_performance_metrics(self) -> Dict[str, Any]:
    """获取性能指标"""
    return {
        "messages_sent": self.metrics["messages_sent"],
        "messages_received": self.metrics["messages_received"],
        "connection_errors": self.metrics["connection_errors"],
        "task_failures": self.metrics["task_failures"],
        "uptime": time.time() - getattr(self, 'start_time', time.time())
    }
```

### 1.3 重连机制实现

```python
def _attempt_reconnect(self):
    """尝试重连"""
    if self.reconnect_attempts >= self.max_reconnect_attempts:
        logger.error("重连次数已达上限，停止重连")
        return

    self.reconnect_attempts += 1
    self.status = ConnectionStatus.RECONNECTING

    # 指数退避重连
    delay = min(2 ** self.reconnect_attempts, 60)
    logger.info(f"第{self.reconnect_attempts}次重连，等待{delay}秒")

    threading.Timer(delay, self._reconnect).start()

def _reconnect(self):
    """执行重连"""
    logger.info("正在重连WebSocket服务器")
    try:
        # 暂停所有活跃任务
        self._pause_all_tasks()

        # 重新连接
        self.connect()

        # 如果重连成功，恢复任务
        if self.is_connected:
            self._resume_all_tasks()

    except Exception as e:
        logger.error(f"重连失败: {e}")
        self._attempt_reconnect()

def _pause_all_tasks(self):
    """暂停所有任务"""
    for task_id, task_context in self.active_tasks.items():
        try:
            task_context.pause()
            logger.info(f"任务 {task_id} 已暂停")
        except Exception as e:
            logger.error(f"暂停任务 {task_id} 失败: {e}")

def _resume_all_tasks(self):
    """恢复所有任务"""
    for task_id, task_context in self.active_tasks.items():
        try:
            task_context.resume()
            logger.info(f"任务 {task_id} 已恢复")
        except Exception as e:
            logger.error(f"恢复任务 {task_id} 失败: {e}")
```

## 2. 任务上下文管理

### 2.1 TaskContext 类完整实现

```python
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
            from .task_executor import TaskExecutor

            algorithm = self.config.get("federatedAlgorithm")
            training_config = self.config.get("localTrainingConfig", {})

            executor = TaskExecutor(
                task_id=self.task_id,
                algorithm=algorithm,
                config=training_config
            )

            return executor

        except Exception as e:
            logger.error(f"创建执行器失败: {e}")
            return None

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
            # 这里应该调用父客户端的方法上传梯度
            # 为了简化，这里只是记录日志
            logger.info(f"任务 {self.task_id} 梯度准备完成，准备上传")

            # 在实际实现中，这里会调用类似以下的方法：
            # self.parent_client.upload_gradients(self.task_id, self.current_round, gradients, training_result)

        except Exception as e:
            logger.error(f"通知梯度就绪失败: {e}")

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
                "resource_usage": self.resource_usage
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
                self.executor.cleanup()

            # 清理数据
            self.training_data = None
            self.current_model = None
            self.gradient_data = None

            self.status = TaskStatus.CLEANED
```

### 2.2 任务生命周期管理

```python
class TaskLifecycleManager:
    """任务生命周期管理器"""

    def __init__(self, client):
        self.client = client
        self.lifecycle_handlers = {
            TaskStatus.INITIALIZING: self._handle_initializing,
            TaskStatus.READY: self._handle_ready,
            TaskStatus.TRAINING: self._handle_training,
            TaskStatus.GRADIENT_READY: self._handle_gradient_ready,
            TaskStatus.WAITING: self._handle_waiting,
            TaskStatus.ERROR: self._handle_error,
            TaskStatus.COMPLETED: self._handle_completed
        }

    def manage_task_lifecycle(self, task_id: str):
        """管理任务生命周期"""
        if task_id not in self.client.active_tasks:
            return

        task_context = self.client.active_tasks[task_id]
        current_status = task_context.status

        if current_status in self.lifecycle_handlers:
            self.lifecycle_handlers[current_status](task_context)

    def _handle_initializing(self, task_context: TaskContext):
        """处理初始化状态"""
        # 初始化状态通常不需要额外处理
        pass

    def _handle_ready(self, task_context: TaskContext):
        """处理就绪状态"""
        # 就绪状态等待后端发送ROUND_START指令
        pass

    def _handle_training(self, task_context: TaskContext):
        """处理训练状态"""
        # 检查训练是否超时
        if time.time() - task_context.last_activity > 600:  # 10分钟超时
            logger.warning(f"任务 {task_context.task_id} 训练超时")
            task_context.status = TaskStatus.ERROR
            task_context.error_count += 1

    def _handle_gradient_ready(self, task_context: TaskContext):
        """处理梯度就绪状态"""
        # 梯度就绪后自动上传（如果还未上传）
        if task_context.gradient_data:
            self._upload_gradients(task_context)

    def _handle_waiting(self, task_context: TaskContext):
        """处理等待状态"""
        # 等待状态检查是否长时间无响应
        if time.time() - task_context.last_activity > 1800:  # 30分钟无响应
            logger.warning(f"任务 {task_context.task_id} 长时间等待")

    def _handle_error(self, task_context: TaskContext):
        """处理错误状态"""
        # 错误处理：报告错误并等待后端指令
        if task_context.error_count > 3:
            logger.error(f"任务 {task_context.task_id} 错误次数过多，建议停止")
            self._suggest_task_termination(task_context)

    def _handle_completed(self, task_context: TaskContext):
        """处理完成状态"""
        # 任务完成后的清理工作
        self._schedule_cleanup(task_context)

    def _upload_gradients(self, task_context: TaskContext):
        """上传梯度"""
        # 实现梯度上传逻辑
        pass

    def _suggest_task_termination(self, task_context: TaskContext):
        """建议任务终止"""
        # 向后端发送任务终止建议
        pass

    def _schedule_cleanup(self, task_context: TaskContext):
        """调度清理任务"""
        # 延迟清理，给后端时间处理最终结果
        threading.Timer(60, task_context.cleanup).start()
```

## 3. 错误处理和恢复

### 3.1 分级错误处理

```python
class ErrorHandler:
    """错误处理器"""

    def __init__(self, client):
        self.client = client
        self.error_thresholds = {
            "connection_errors": 5,
            "task_failures": 3,
            "message_errors": 10
        }

    def handle_connection_error(self, error: Exception):
        """处理连接错误"""
        logger.error(f"连接错误: {error}")

        self.client.metrics["connection_errors"] += 1

        if self.client.metrics["connection_errors"] > self.error_thresholds["connection_errors"]:
            # 连接错误过多，进入降级模式
            self._enter_degraded_mode()
        else:
            # 尝试重连
            self.client._attempt_reconnect()

    def handle_task_error(self, task_id: str, error: Exception):
        """处理任务错误"""
        logger.error(f"任务 {task_id} 错误: {error}")

        if task_id in self.client.active_tasks:
            task_context = self.client.active_tasks[task_id]
            task_context.error_count += 1
            task_context.last_error = str(error)

            if task_context.error_count > self.error_thresholds["task_failures"]:
                # 任务错误过多，标记为失败
                self._mark_task_failed(task_id)
            else:
                # 报告错误给后端
                self._report_task_error(task_id, error)

    def handle_message_error(self, message: Dict[str, Any], error: Exception):
        """处理消息错误"""
        logger.error(f"消息处理错误: {error}")

        self.client.metrics["message_errors"] = self.client.metrics.get("message_errors", 0) + 1

        # 发送错误报告给后端
        self._send_error_report(message, error)

    def _enter_degraded_mode(self):
        """进入降级模式"""
        logger.warning("进入降级模式")

        # 暂停所有非关键任务
        for task_id, task_context in self.client.active_tasks.items():
            if task_context.status == TaskStatus.TRAINING:
                task_context.pause()

    def _mark_task_failed(self, task_id: str):
        """标记任务失败"""
        if task_id in self.client.active_tasks:
            task_context = self.client.active_tasks[task_id]
            task_context.status = TaskStatus.ERROR

            # 发送任务失败通知
            self._notify_task_failure(task_id)

    def _report_task_error(self, task_id: str, error: Exception):
        """报告任务错误"""
        error_message = {
            "type": "ERROR",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "errorType": "TASK_ERROR",
                "errorMessage": str(error),
                "severity": "ERROR"
            }
        }

        self.client._send_message(error_message)

    def _send_error_report(self, original_message: Dict[str, Any], error: Exception):
        """发送错误报告"""
        error_message = {
            "type": "ERROR",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "errorType": "MESSAGE_ERROR",
                "errorMessage": str(error),
                "originalMessage": original_message,
                "severity": "WARNING"
            }
        }

        self.client._send_message(error_message)
```

### 3.2 自动恢复机制

```python
class RecoveryManager:
    """恢复管理器"""

    def __init__(self, client):
        self.client = client
        self.recovery_strategies = {
            "connection_lost": self._recover_connection,
            "task_failed": self._recover_task,
            "memory_overflow": self._recover_memory,
            "resource_exhausted": self._recover_resources
        }

    def attempt_recovery(self, error_type: str, context: Dict[str, Any] = None):
        """尝试恢复"""
        if error_type in self.recovery_strategies:
            return self.recovery_strategies[error_type](context or {})
        else:
            logger.warning(f"未知错误类型，无法恢复: {error_type}")
            return False

    def _recover_connection(self, context: Dict[str, Any]) -> bool:
        """恢复连接"""
        logger.info("尝试恢复WebSocket连接")

        try:
            # 清理当前连接
            if self.client.websocket:
                self.client.websocket.close()

            # 重新初始化连接
            success = self.client.connect()

            if success:
                # 重新同步任务状态
                self._resync_task_states()
                logger.info("连接恢复成功")
                return True
            else:
                logger.error("连接恢复失败")
                return False

        except Exception as e:
            logger.error(f"连接恢复异常: {e}")
            return False

    def _recover_task(self, context: Dict[str, Any]) -> bool:
        """恢复任务"""
        task_id = context.get("task_id")
        if not task_id or task_id not in self.client.active_tasks:
            return False

        logger.info(f"尝试恢复任务: {task_id}")

        try:
            task_context = self.client.active_tasks[task_id]

            # 重置错误计数
            task_context.error_count = 0
            task_context.last_error = None

            # 根据当前状态决定恢复策略
            if task_context.status == TaskStatus.ERROR:
                # 尝试重新初始化
                if task_context.initialize():
                    logger.info(f"任务 {task_id} 恢复成功")
                    return True
                else:
                    logger.error(f"任务 {task_id} 恢复失败")
                    return False
            else:
                # 恢复暂停的任务
                task_context.resume()
                return True

        except Exception as e:
            logger.error(f"任务恢复异常: {e}")
            return False

    def _recover_memory(self, context: Dict[str, Any]) -> bool:
        """内存恢复"""
        logger.info("尝试释放内存")

        try:
            # 清理已完成的任务
            completed_tasks = [
                task_id for task_id, task_context in self.client.active_tasks.items()
                if task_context.status in [TaskStatus.COMPLETED, TaskStatus.ERROR]
            ]

            for task_id in completed_tasks:
                self.client._cleanup_task(task_id)

            # 强制垃圾回收
            import gc
            gc.collect()

            logger.info(f"清理了 {len(completed_tasks)} 个任务，释放内存")
            return True

        except Exception as e:
            logger.error(f"内存恢复异常: {e}")
            return False

    def _recover_resources(self, context: Dict[str, Any]) -> bool:
        """资源恢复"""
        logger.info("尝试释放系统资源")

        try:
            # 暂停低优先级任务
            paused_count = 0
            for task_id, task_context in self.client.active_tasks.items():
                if task_context.status == TaskStatus.TRAINING and not task_context.is_paused:
                    task_context.pause()
                    paused_count += 1

                    # 只暂停一半的任务
                    if paused_count >= len(self.client.active_tasks) // 2:
                        break

            logger.info(f"暂停了 {paused_count} 个任务以释放资源")
            return True

        except Exception as e:
            logger.error(f"资源恢复异常: {e}")
            return False

    def _resync_task_states(self):
        """重新同步任务状态"""
        logger.info("重新同步任务状态")

        # 向后端报告当前所有任务状态
        for task_id, task_context in self.client.active_tasks.items():
            status_message = {
                "type": "STATUS_REPORT",
                "id": self.client._generate_message_id(),
                "timestamp": self.client._get_current_timestamp(),
                "vmId": self.client.vm_id,
                "data": {
                    "taskId": task_id,
                    "status": task_context.status.value,
                    "currentRound": task_context.current_round,
                    "progress": task_context.progress
                }
            }

            self.client._send_message(status_message)
```

---

**下一步**: 继续阅读 [消息处理机制](./04-message-handling.md) 了解详细的消息路由和处理实现。