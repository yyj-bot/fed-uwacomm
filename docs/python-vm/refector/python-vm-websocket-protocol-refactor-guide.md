# Python 虚拟机 WebSocket 协议重构综合指导

本文档提供 Python 虚拟机 WebSocket 协议从现有 STOMP 协议到 v1.4 简化协议的完整重构指导，基于中心化架构设计，确保 Python VM 作为"手脚"执行器的角色定位。

## 目录

1. [重构概述](#1-重构概述)
2. [架构设计变化](#2-架构设计变化)
3. [WebSocket 客户端实现](#3-websocket-客户端实现)
4. [消息处理机制](#4-消息处理机制)
5. [联邦学习集成](#5-联邦学习集成)
6. [测试和调试](#6-测试和调试)
7. [部署和维护](#7-部署和维护)

---

## 1. 重构概述

### 1.1 协议升级背景

#### 现有协议问题
当前 Python VM 使用基于 STOMP 的 WebSocket 协议，存在以下问题：
- **复杂性过高**: STOMP 协议带来不必要的复杂性
- **协商过多**: 大量的协商和配置消息影响性能
- **状态管理混乱**: VM 端维护复杂状态机，容易出错
- **决策分散**: VM 具有过多决策权，影响系统可控性

#### v1.4 协议优势
新的 WebSocket 协议 v1.4 采用中心化架构：
- **简化消息**: 只有34个核心消息类型，覆盖完整生命周期
- **中心化控制**: 后端作为"大脑"完全控制，VM 作为"手脚"执行
- **精确控制**: 通过 taskId 实现任务级精确控制
- **多任务支持**: 支持单 VM 同时执行多个联邦学习任务

### 1.2 重构目标

#### 核心目标
1. **简化架构**: 从复杂协商转为被动响应
2. **提高可控性**: 所有决策由后端控制
3. **增强稳定性**: 减少 VM 端状态管理复杂度
4. **支持并发**: 实现多任务并发执行能力

#### 具体收益
- **开发效率**: 减少70%的协议处理代码
- **维护成本**: 降低状态管理复杂度
- **系统稳定性**: 集中化控制减少错误
- **扩展性**: 更好的多任务支持

### 1.3 升级策略

#### 渐进式升级方案
```
阶段1: 协议适配层 (1-2周)
├── 保留现有接口
├── 新增 v1.4 协议支持
└── 兼容性测试

阶段2: 核心重构 (2-3周)
├── WebSocket 客户端重写
├── 消息处理重构
└── 状态管理简化

阶段3: 功能完善 (1-2周)
├── 多任务支持
├── 异常处理优化
└── 性能调优

阶段4: 清理和优化 (1周)
├── 移除旧代码
├── 文档更新
└── 最终测试
```

#### 兼容性考虑
- **数据库兼容**: 保持与现有数据库结构兼容
- **ML 模块兼容**: 复用现有训练和评估模块
- **配置兼容**: 保持现有配置文件格式
- **API 兼容**: 对外接口保持稳定

---

## 2. 架构设计变化

### 2.1 角色定位变化

#### 旧架构：复杂协商模式
```
Python VM (旧):
├── 复杂状态管理
├── 协商决策逻辑
├── 主动任务调度
└── 分布式状态同步

职责过重，容易出错
```

#### 新架构：被动执行模式
```
Python VM (新):
├── 被动响应指令
├── 简单状态报告
├── 专注训练执行
└── 多任务隔离管理

职责清晰，稳定可靠
```

### 2.2 模块设计对比

#### 现有模块结构
```python
feduwacomm.ml.api/
├── websocket_client.py      # STOMP 协议客户端
├── message_handler.py       # 复杂消息处理
└── vm_api_client.py         # API 客户端

feduwacomm.ml.federated/
├── client.py                # 联邦学习客户端
├── coordinator.py           # 协调器（需重构）
└── config.py                # 配置管理

问题：职责不清，协调器过于复杂
```

#### 新模块结构
```python
feduwacomm.ml.websocket/     # 新增WebSocket模块
├── client.py                # v1.4协议客户端
├── message_router.py        # 消息路由器
├── task_manager.py          # 多任务管理器
└── protocol_handler.py      # 协议处理器

feduwacomm.ml.federated/     # 重构联邦学习模块
├── task_executor.py         # 任务执行器（简化）
├── trainer_manager.py       # 训练器管理
└── model_handler.py         # 模型处理器

优势：职责清晰，模块化程度高
```

### 2.3 多任务管理架构

#### 任务隔离设计
```python
class TaskManager:
    """多任务管理器"""
    def __init__(self):
        self.active_tasks = {}  # taskId -> TaskContext
        self.max_concurrent = 3  # 最大并发任务数

    def add_task(self, task_id: str, config: Dict) -> bool:
        """添加新任务"""
        if len(self.active_tasks) >= self.max_concurrent:
            return False

        self.active_tasks[task_id] = TaskContext(task_id, config)
        return True

    def route_message(self, message: Dict) -> bool:
        """根据taskId路由消息"""
        task_id = message.get("data", {}).get("taskId")
        if task_id in self.active_tasks:
            return self.active_tasks[task_id].handle_message(message)
        return False
```

#### 资源隔离策略
- **内存隔离**: 每个任务独立的数据空间
- **计算隔离**: 训练线程池管理
- **状态隔离**: 独立的状态跟踪
- **错误隔离**: 单个任务错误不影响其他任务

---

## 3. WebSocket 客户端实现

### 3.1 核心客户端类

#### 基础客户端实现
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

        # 心跳管理
        self.heartbeat_interval = 30  # 秒
        self.heartbeat_thread = None
        self.heartbeat_stop_event = threading.Event()

        logger.info(f"初始化FederatedLearningClient，VM ID: {vm_id}")

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
            self._handle_message(msg)
        except Exception as e:
            logger.error(f"处理消息失败: {e}")

    def _on_error(self, ws, error):
        """错误回调"""
        logger.error(f"WebSocket错误: {error}")
        self.status = ConnectionStatus.ERROR

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
                            "activeTasks": self._get_active_tasks_status()
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
                }
            }
        except ImportError:
            return {
                "cpu_percent": 0.0,
                "memory_percent": 0.0,
                "disk_usage": 0.0,
                "network_io": {"bytes_sent": 0, "bytes_recv": 0}
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
                "lastActivity": task_context.last_activity
            })
        return task_status

    def _send_message(self, message: Dict[str, Any]):
        """发送WebSocket消息"""
        if not self.is_connected or not self.websocket:
            logger.warning("WebSocket未连接，无法发送消息")
            return False

        try:
            message_str = json.dumps(message, ensure_ascii=False)
            self.websocket.send(message_str)
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
            self.connect()
        except Exception as e:
            logger.error(f"重连失败: {e}")
            self._attempt_reconnect()
```

### 3.2 任务上下文管理

#### TaskContext 类实现
```python
class TaskContext:
    """任务上下文管理"""

    def __init__(self, task_id: str, config: Dict[str, Any]):
        self.task_id = task_id
        self.config = config
        self.status = "INITIALIZING"
        self.current_round = 0
        self.progress = 0.0
        self.last_activity = time.time()

        # 训练相关
        self.trainer = None
        self.training_thread = None
        self.is_training = False

        # 数据相关
        self.training_data = None
        self.current_model = None

        logger.info(f"创建任务上下文: {task_id}")

    def initialize_trainer(self) -> bool:
        """初始化训练器"""
        try:
            from feduwacomm.ml.federated.task_executor import TaskExecutor

            algorithm = self.config.get("federatedAlgorithm")
            training_config = self.config.get("localTrainingConfig", {})

            self.trainer = TaskExecutor(
                task_id=self.task_id,
                algorithm=algorithm,
                config=training_config
            )

            # 加载初始模型
            initial_model = self.config.get("initialGlobalModel")
            if initial_model:
                self.trainer.load_global_model(initial_model)

            # 加载训练数据
            dataset_id = training_config.get("datasetId")
            if dataset_id:
                self.trainer.load_training_data(dataset_id)

            self.status = "READY"
            self.last_activity = time.time()

            logger.info(f"任务 {self.task_id} 训练器初始化成功")
            return True

        except Exception as e:
            logger.error(f"任务 {self.task_id} 训练器初始化失败: {e}")
            self.status = "FAILED"
            return False

    def start_round_training(self, round_number: int, round_config: Dict[str, Any]) -> bool:
        """开始轮次训练"""
        if self.is_training:
            logger.warning(f"任务 {self.task_id} 已在训练中")
            return False

        self.current_round = round_number
        self.status = "TRAINING"
        self.is_training = True
        self.last_activity = time.time()

        # 启动训练线程
        self.training_thread = threading.Thread(
            target=self._execute_training,
            args=(round_config,),
            daemon=True
        )
        self.training_thread.start()

        logger.info(f"任务 {self.task_id} 开始第 {round_number} 轮训练")
        return True

    def _execute_training(self, round_config: Dict[str, Any]):
        """执行训练（在独立线程中）"""
        try:
            # 执行本地训练
            training_result = self.trainer.train_round(round_config)

            # 提取梯度
            gradients = self.trainer.extract_gradients()

            # 更新状态
            self.status = "GRADIENT_READY"
            self.is_training = False
            self.progress = 100.0
            self.last_activity = time.time()

            # 通知父客户端上传梯度
            from feduwacomm.ml.websocket.client import get_current_client
            client = get_current_client()
            if client:
                client._upload_gradients(
                    self.task_id,
                    self.current_round,
                    gradients,
                    training_result
                )

        except Exception as e:
            logger.error(f"任务 {self.task_id} 训练失败: {e}")
            self.status = "ERROR"
            self.is_training = False

            # 报告错误
            from feduwacomm.ml.websocket.client import get_current_client
            client = get_current_client()
            if client:
                client._report_training_error(
                    self.task_id,
                    self.current_round,
                    str(e)
                )

    def update_global_model(self, global_model: Dict[str, Any]) -> bool:
        """更新全局模型"""
        try:
            if self.trainer:
                success = self.trainer.update_global_model(global_model)
                if success:
                    self.status = "READY"
                    self.progress = 0.0
                    self.last_activity = time.time()
                    logger.info(f"任务 {self.task_id} 全局模型更新成功")
                return success
            return False
        except Exception as e:
            logger.error(f"任务 {self.task_id} 全局模型更新失败: {e}")
            return False

    def cleanup(self):
        """清理任务资源"""
        logger.info(f"清理任务 {self.task_id}")

        # 停止训练
        if self.is_training and self.training_thread:
            self.is_training = False
            self.training_thread.join(timeout=10)

        # 清理训练器
        if self.trainer:
            self.trainer.cleanup()

        self.status = "CLEANED"
```

---

## 4. 消息处理机制

### 4.1 消息路由器

#### 核心路由实现
```python
class MessageRouter:
    """消息路由器 - 负责分发v1.4协议消息"""

    def __init__(self, client):
        self.client = client
        self.handlers = {
            # 连接管理
            "CONNECT_ACK": self._handle_connect_ack,
            "HEARTBEAT_ACK": self._handle_heartbeat_ack,

            # 任务管理
            "FEDERATED_TASK_START": self._handle_task_start,
            "FEDERATED_TASK_STOP": self._handle_task_stop,
            "FEDERATED_TASK_RESUME": self._handle_task_resume,
            "FEDERATED_TASK_DELETE": self._handle_task_delete,

            # 轮次控制
            "ROUND_START": self._handle_round_start,
            "ROUND_COMPLETE": self._handle_round_complete,

            # 模型传输
            "GLOBAL_MODEL_BROADCAST": self._handle_global_model_broadcast,
            "GRADIENT_UPLOAD_ACK": self._handle_gradient_upload_ack,

            # 状态查询
            "STATUS_QUERY": self._handle_status_query,

            # 错误处理
            "ERROR": self._handle_error_message
        }

    def route_message(self, message: Dict[str, Any]) -> bool:
        """路由消息到对应处理器"""
        msg_type = message.get("type")

        if msg_type in self.handlers:
            try:
                return self.handlers[msg_type](message)
            except Exception as e:
                logger.error(f"处理消息 {msg_type} 失败: {e}")
                return False
        else:
            logger.warning(f"未知消息类型: {msg_type}")
            return False

    def _handle_connect_ack(self, message: Dict[str, Any]) -> bool:
        """处理连接确认"""
        data = message.get("data", {})
        status = data.get("status")

        if status == "SUCCESS":
            logger.info("服务器连接确认成功")
            return True
        else:
            error_msg = data.get("message", "连接失败")
            logger.error(f"服务器连接确认失败: {error_msg}")
            return False

    def _handle_heartbeat_ack(self, message: Dict[str, Any]) -> bool:
        """处理心跳确认"""
        logger.debug("收到心跳确认")
        return True

    def _handle_task_start(self, message: Dict[str, Any]) -> bool:
        """处理任务启动指令"""
        try:
            task_data = message.get("data", {})
            task_id = task_data.get("taskId")

            if not task_id:
                logger.error("任务启动消息缺少taskId")
                return False

            # 检查并发限制
            if len(self.client.active_tasks) >= self.client.max_concurrent_tasks:
                self._send_task_start_ack(task_id, "FAILED", "已达最大并发任务数")
                return False

            # 创建任务上下文
            task_context = TaskContext(task_id, task_data)
            success = task_context.initialize_trainer()

            if success:
                self.client.active_tasks[task_id] = task_context
                self._send_task_start_ack(task_id, "SUCCESS", "任务初始化完成")
                logger.info(f"任务 {task_id} 启动成功")
            else:
                self._send_task_start_ack(task_id, "FAILED", "任务初始化失败")
                logger.error(f"任务 {task_id} 启动失败")

            return success

        except Exception as e:
            logger.error(f"处理任务启动失败: {e}")
            return False

    def _handle_task_stop(self, message: Dict[str, Any]) -> bool:
        """处理任务停止指令"""
        try:
            task_data = message.get("data", {})
            task_id = task_data.get("taskId")

            if task_id in self.client.active_tasks:
                task_context = self.client.active_tasks[task_id]
                task_context.status = "STOPPING"

                # 停止训练
                if task_context.is_training:
                    task_context.is_training = False

                # 发送确认
                self._send_task_stop_ack(task_id, "SUCCESS", "任务停止成功")
                logger.info(f"任务 {task_id} 已停止")
                return True
            else:
                self._send_task_stop_ack(task_id, "FAILED", "任务不存在")
                return False

        except Exception as e:
            logger.error(f"处理任务停止失败: {e}")
            return False

    def _handle_round_start(self, message: Dict[str, Any]) -> bool:
        """处理轮次开始指令"""
        try:
            round_data = message.get("data", {})
            task_id = round_data.get("taskId")
            round_number = round_data.get("roundNumber")

            if task_id not in self.client.active_tasks:
                logger.error(f"任务 {task_id} 不存在")
                return False

            task_context = self.client.active_tasks[task_id]

            # 发送轮次开始确认
            self._send_round_start_ack(task_id, round_number, "SUCCESS", "轮次开始确认")

            # 启动训练
            success = task_context.start_round_training(round_number, round_data)

            if success:
                logger.info(f"任务 {task_id} 第 {round_number} 轮开始")
            else:
                logger.error(f"任务 {task_id} 第 {round_number} 轮启动失败")

            return success

        except Exception as e:
            logger.error(f"处理轮次开始失败: {e}")
            return False

    def _handle_global_model_broadcast(self, message: Dict[str, Any]) -> bool:
        """处理全局模型广播"""
        try:
            model_data = message.get("data", {})
            task_id = model_data.get("taskId")
            round_number = model_data.get("roundNumber")

            if task_id not in self.client.active_tasks:
                logger.error(f"任务 {task_id} 不存在")
                return False

            task_context = self.client.active_tasks[task_id]

            # 解码全局模型
            global_model_b64 = model_data.get("globalModel")
            if global_model_b64:
                global_model_bytes = base64.b64decode(global_model_b64)
                global_model = json.loads(global_model_bytes.decode('utf-8'))
            else:
                global_model = model_data.get("globalModelData", {})

            # 更新本地模型
            success = task_context.update_global_model(global_model)

            # 发送确认
            status = "SUCCESS" if success else "FAILED"
            message = "全局模型更新成功" if success else "全局模型更新失败"
            self._send_global_model_broadcast_ack(task_id, round_number, status, message)

            if success:
                logger.info(f"任务 {task_id} 全局模型更新完成")
            else:
                logger.error(f"任务 {task_id} 全局模型更新失败")

            return success

        except Exception as e:
            logger.error(f"处理全局模型广播失败: {e}")
            return False

    def _handle_status_query(self, message: Dict[str, Any]) -> bool:
        """处理状态查询"""
        try:
            query_data = message.get("data", {})
            query_type = query_data.get("queryType", "VM_STATUS")

            if query_type == "VM_STATUS":
                # 返回VM整体状态
                status_data = {
                    "vmId": self.client.vm_id,
                    "status": self.client.status.value,
                    "resourceUsage": self.client._get_resource_usage(),
                    "activeTasks": self.client._get_active_tasks_status(),
                    "capabilities": self.client._get_vm_capabilities()
                }
            elif query_type == "TASK_STATUS":
                # 返回特定任务状态
                task_id = query_data.get("taskId")
                if task_id in self.client.active_tasks:
                    task_context = self.client.active_tasks[task_id]
                    status_data = {
                        "taskId": task_id,
                        "status": task_context.status,
                        "currentRound": task_context.current_round,
                        "progress": task_context.progress,
                        "lastActivity": task_context.last_activity
                    }
                else:
                    status_data = {"error": "任务不存在"}
            else:
                status_data = {"error": "未知查询类型"}

            # 发送状态响应
            response_message = {
                "type": "STATUS_RESPONSE",
                "id": self.client._generate_message_id(),
                "timestamp": self.client._get_current_timestamp(),
                "vmId": self.client.vm_id,
                "data": status_data
            }

            return self.client._send_message(response_message)

        except Exception as e:
            logger.error(f"处理状态查询失败: {e}")
            return False

    # 确认消息发送方法
    def _send_task_start_ack(self, task_id: str, status: str, message: str):
        """发送任务启动确认"""
        ack_message = {
            "type": "FEDERATED_TASK_START_ACK",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "status": status,
                "message": message
            }
        }
        self.client._send_message(ack_message)

    def _send_round_start_ack(self, task_id: str, round_number: int, status: str, message: str):
        """发送轮次开始确认"""
        ack_message = {
            "type": "ROUND_START_ACK",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "roundNumber": round_number,
                "status": status,
                "message": message
            }
        }
        self.client._send_message(ack_message)

    def _send_global_model_broadcast_ack(self, task_id: str, round_number: int, status: str, message: str):
        """发送全局模型广播确认"""
        ack_message = {
            "type": "GLOBAL_MODEL_BROADCAST_ACK",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "roundNumber": round_number,
                "status": status,
                "message": message
            }
        }
        self.client._send_message(ack_message)
```

### 4.2 异步处理和并发控制

#### 线程池管理
```python
import concurrent.futures
from threading import Semaphore

class ConcurrencyManager:
    """并发管理器"""

    def __init__(self, max_workers: int = 3):
        self.max_workers = max_workers
        self.executor = concurrent.futures.ThreadPoolExecutor(max_workers=max_workers)
        self.training_semaphore = Semaphore(max_workers)
        self.active_futures = {}  # task_id -> Future

    def submit_training_task(self, task_id: str, training_func, *args, **kwargs):
        """提交训练任务到线程池"""
        if task_id in self.active_futures:
            logger.warning(f"任务 {task_id} 已在执行中")
            return None

        def wrapped_training():
            try:
                with self.training_semaphore:
                    return training_func(*args, **kwargs)
            finally:
                # 清理完成的任务
                if task_id in self.active_futures:
                    del self.active_futures[task_id]

        future = self.executor.submit(wrapped_training)
        self.active_futures[task_id] = future
        return future

    def cancel_task(self, task_id: str) -> bool:
        """取消训练任务"""
        if task_id in self.active_futures:
            future = self.active_futures[task_id]
            success = future.cancel()
            if not success and not future.done():
                # 任务已开始执行，无法取消
                logger.warning(f"任务 {task_id} 已开始执行，无法取消")
            return success
        return False

    def is_task_running(self, task_id: str) -> bool:
        """检查任务是否正在执行"""
        if task_id in self.active_futures:
            future = self.active_futures[task_id]
            return not future.done()
        return False

    def get_task_result(self, task_id: str, timeout: Optional[float] = None):
        """获取任务执行结果"""
        if task_id in self.active_futures:
            future = self.active_futures[task_id]
            try:
                return future.result(timeout=timeout)
            except concurrent.futures.TimeoutError:
                logger.warning(f"任务 {task_id} 执行超时")
                return None
            except Exception as e:
                logger.error(f"任务 {task_id} 执行失败: {e}")
                return None
        return None

    def shutdown(self, wait: bool = True):
        """关闭线程池"""
        self.executor.shutdown(wait=wait)
```

---

## 5. 联邦学习集成

### 5.1 任务执行器重构

#### TaskExecutor 实现
```python
"""
联邦学习任务执行器
简化版本，专注于训练执行，移除决策逻辑
"""

import json
import numpy as np
import pickle
import base64
from typing import Dict, Any, Optional, Tuple
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, precision_score, recall_score

class TaskExecutor:
    """联邦学习任务执行器 - 被动响应式设计"""

    def __init__(self, task_id: str, algorithm: str, config: Dict[str, Any]):
        self.task_id = task_id
        self.algorithm = algorithm
        self.config = config

        # 模型相关
        self.current_model = None
        self.previous_model = None

        # 数据相关
        self.training_data = None
        self.training_labels = None
        self.validation_data = None
        self.validation_labels = None

        # 训练状态
        self.is_initialized = False
        self.current_round = 0

        logger.info(f"创建任务执行器: {task_id}, 算法: {algorithm}")

    def load_global_model(self, model_data: Dict[str, Any]) -> bool:
        """加载全局模型"""
        try:
            if self.algorithm == "FEDERATED_AVERAGING":
                return self._load_fedavg_model(model_data)
            elif self.algorithm == "FEDERATED_PROXIMAL":
                return self._load_fedprox_model(model_data)
            elif self.algorithm == "FEDERATED_NOVA":
                return self._load_fednova_model(model_data)
            elif self.algorithm == "SCAFFOLD":
                return self._load_scaffold_model(model_data)
            else:
                logger.error(f"不支持的算法: {self.algorithm}")
                return False

        except Exception as e:
            logger.error(f"加载全局模型失败: {e}")
            return False

    def load_training_data(self, dataset_id: str) -> bool:
        """加载训练数据"""
        try:
            from feduwacomm.database.database import DatabaseManager

            db = DatabaseManager()

            # 查询训练数据
            query = """
            SELECT features, labels
            FROM training_dataset
            WHERE dataset_id = %s AND data_type = 'ACOUSTIC'
            """

            results = db.fetch_all(query, (dataset_id,))

            if not results:
                logger.error(f"未找到数据集: {dataset_id}")
                return False

            # 解析特征和标签
            features_list = []
            labels_list = []

            for row in results:
                features = json.loads(row['features'])
                labels = json.loads(row['labels'])
                features_list.append(features)
                labels_list.append(labels)

            self.training_data = np.array(features_list)
            self.training_labels = np.array(labels_list)

            # 划分验证集（20%）
            split_idx = int(0.8 * len(self.training_data))
            self.validation_data = self.training_data[split_idx:]
            self.validation_labels = self.training_labels[split_idx:]
            self.training_data = self.training_data[:split_idx]
            self.training_labels = self.training_labels[:split_idx]

            logger.info(f"成功加载数据集 {dataset_id}: {len(self.training_data)} 训练样本, {len(self.validation_data)} 验证样本")
            return True

        except Exception as e:
            logger.error(f"加载训练数据失败: {e}")
            return False

    def train_round(self, round_config: Dict[str, Any]) -> Dict[str, Any]:
        """执行一轮训练"""
        try:
            round_number = round_config.get("roundNumber", self.current_round + 1)
            self.current_round = round_number

            logger.info(f"任务 {self.task_id} 开始第 {round_number} 轮训练")

            if self.algorithm == "FEDERATED_AVERAGING":
                return self._train_fedavg(round_config)
            elif self.algorithm == "FEDERATED_PROXIMAL":
                return self._train_fedprox(round_config)
            elif self.algorithm == "FEDERATED_NOVA":
                return self._train_fednova(round_config)
            elif self.algorithm == "SCAFFOLD":
                return self._train_scaffold(round_config)
            else:
                raise ValueError(f"不支持的算法: {self.algorithm}")

        except Exception as e:
            logger.error(f"训练执行失败: {e}")
            raise

    def extract_gradients(self) -> Dict[str, Any]:
        """提取梯度信息"""
        try:
            if not self.current_model:
                raise ValueError("当前模型为空")

            # 对于随机森林，提取树的权重作为"梯度"
            if hasattr(self.current_model, 'estimators_'):
                # 随机森林模型
                gradients = {
                    "model_type": "RandomForest",
                    "n_estimators": len(self.current_model.estimators_),
                    "feature_importances": self.current_model.feature_importances_.tolist(),
                    "trees_data": []
                }

                # 序列化前几棵树（避免数据过大）
                max_trees = min(10, len(self.current_model.estimators_))
                for i in range(max_trees):
                    tree = self.current_model.estimators_[i]
                    tree_data = {
                        "tree_index": i,
                        "feature_importances": tree.feature_importances_.tolist(),
                        "n_classes": tree.n_classes_,
                        "n_features": tree.n_features_
                    }
                    gradients["trees_data"].append(tree_data)
            else:
                # 其他模型类型
                gradients = {
                    "model_type": type(self.current_model).__name__,
                    "model_params": self._serialize_model_params()
                }

            return gradients

        except Exception as e:
            logger.error(f"提取梯度失败: {e}")
            raise

    def update_global_model(self, global_model: Dict[str, Any]) -> bool:
        """更新全局模型"""
        try:
            # 保存上一个模型
            self.previous_model = self.current_model

            # 加载新的全局模型
            return self.load_global_model(global_model)

        except Exception as e:
            logger.error(f"更新全局模型失败: {e}")
            return False

    def cleanup(self):
        """清理资源"""
        self.current_model = None
        self.previous_model = None
        self.training_data = None
        self.training_labels = None
        self.validation_data = None
        self.validation_labels = None
        logger.info(f"任务执行器 {self.task_id} 资源已清理")

    # 算法特定实现
    def _load_fedavg_model(self, model_data: Dict[str, Any]) -> bool:
        """加载FedAvg模型"""
        try:
            if "model_params" in model_data:
                # 从序列化参数恢复模型
                params = model_data["model_params"]
                self.current_model = RandomForestClassifier(
                    n_estimators=params.get("n_estimators", 100),
                    max_depth=params.get("max_depth", None),
                    random_state=42
                )
            else:
                # 创建新模型
                self.current_model = RandomForestClassifier(
                    n_estimators=100,
                    max_depth=10,
                    random_state=42
                )

            return True

        except Exception as e:
            logger.error(f"加载FedAvg模型失败: {e}")
            return False

    def _train_fedavg(self, round_config: Dict[str, Any]) -> Dict[str, Any]:
        """执行FedAvg训练"""
        start_time = time.time()

        # 检查数据
        if self.training_data is None:
            raise ValueError("训练数据未加载")

        # 训练模型
        self.current_model.fit(self.training_data, self.training_labels)

        # 计算训练指标
        train_pred = self.current_model.predict(self.training_data)
        train_accuracy = accuracy_score(self.training_labels, train_pred)

        # 计算验证指标
        val_pred = self.current_model.predict(self.validation_data)
        val_accuracy = accuracy_score(self.validation_labels, val_pred)
        val_precision = precision_score(self.validation_labels, val_pred, average='weighted')
        val_recall = recall_score(self.validation_labels, val_pred, average='weighted')

        training_time = time.time() - start_time

        result = {
            "algorithm": "FEDERATED_AVERAGING",
            "round_number": self.current_round,
            "training_time": training_time,
            "samples_count": len(self.training_data),
            "train_accuracy": train_accuracy,
            "val_accuracy": val_accuracy,
            "val_precision": val_precision,
            "val_recall": val_recall,
            "final_loss": 1.0 - val_accuracy,  # 简化的损失计算
            "final_accuracy": val_accuracy
        }

        logger.info(f"FedAvg训练完成: 准确率={val_accuracy:.4f}, 用时={training_time:.2f}s")
        return result

    def _serialize_model_params(self) -> Dict[str, Any]:
        """序列化模型参数"""
        if hasattr(self.current_model, 'get_params'):
            return self.current_model.get_params()
        else:
            return {}
```

### 5.2 与现有ML模块集成

#### 集成适配器
```python
"""
与现有ML模块的集成适配器
保持向后兼容性，同时支持新的WebSocket协议
"""

from feduwacomm.ml.random_forest_trainer import RandomForestTrainer
from feduwacomm.ml.feature_extractor import FeatureExtractor
from feduwacomm.ml.model_evaluator import ModelEvaluator

class MLModuleAdapter:
    """ML模块适配器"""

    def __init__(self, task_id: str):
        self.task_id = task_id
        self.trainer = None
        self.feature_extractor = None
        self.evaluator = None

    def initialize_components(self, config: Dict[str, Any]) -> bool:
        """初始化ML组件"""
        try:
            # 初始化特征提取器
            self.feature_extractor = FeatureExtractor()

            # 初始化训练器
            trainer_config = config.get("trainer", {})
            self.trainer = RandomForestTrainer(
                n_estimators=trainer_config.get("n_estimators", 100),
                max_depth=trainer_config.get("max_depth", 10),
                random_state=42
            )

            # 初始化评估器
            self.evaluator = ModelEvaluator()

            logger.info(f"任务 {self.task_id} ML组件初始化完成")
            return True

        except Exception as e:
            logger.error(f"ML组件初始化失败: {e}")
            return False

    def extract_features(self, raw_data: Dict[str, Any]) -> np.ndarray:
        """提取特征"""
        if self.feature_extractor:
            return self.feature_extractor.extract_features(raw_data)
        else:
            raise ValueError("特征提取器未初始化")

    def train_model(self, features: np.ndarray, labels: np.ndarray) -> Dict[str, Any]:
        """训练模型"""
        if self.trainer:
            return self.trainer.train(features, labels)
        else:
            raise ValueError("训练器未初始化")

    def evaluate_model(self, model, test_features: np.ndarray, test_labels: np.ndarray) -> Dict[str, float]:
        """评估模型"""
        if self.evaluator:
            return self.evaluator.evaluate(model, test_features, test_labels)
        else:
            raise ValueError("评估器未初始化")
```

---

## 6. 测试和调试

### 6.1 单元测试框架

#### WebSocket客户端测试
```python
"""
WebSocket客户端单元测试
"""

import unittest
import json
import time
from unittest.mock import Mock, patch, MagicMock
from feduwacomm.ml.websocket.client import FederatedLearningClient, TaskContext

class TestFederatedLearningClient(unittest.TestCase):
    """WebSocket客户端测试"""

    def setUp(self):
        """测试设置"""
        self.server_url = "ws://localhost:8080/websocket"
        self.access_token = "test_token"
        self.vm_id = "test_vm_001"

        # 模拟WebSocket
        self.mock_websocket = Mock()

        # 创建客户端实例
        with patch('feduwacomm.ml.websocket.client.websocket'):
            self.client = FederatedLearningClient(
                self.server_url,
                self.access_token,
                self.vm_id
            )

    def test_initialization(self):
        """测试初始化"""
        self.assertEqual(self.client.vm_id, self.vm_id)
        self.assertEqual(self.client.server_url, self.server_url)
        self.assertEqual(self.client.access_token, self.access_token)
        self.assertFalse(self.client.is_connected)
        self.assertEqual(len(self.client.active_tasks), 0)

    def test_message_generation(self):
        """测试消息生成"""
        msg_id = self.client._generate_message_id()
        self.assertIsInstance(msg_id, str)
        self.assertIn(self.vm_id, msg_id)

        timestamp = self.client._get_current_timestamp()
        self.assertIsInstance(timestamp, int)
        self.assertGreater(timestamp, 0)

    def test_capabilities_info(self):
        """测试能力信息获取"""
        capabilities = self.client._get_vm_capabilities()

        self.assertIn("systemInfo", capabilities)
        self.assertIn("maxConcurrentTasks", capabilities)
        self.assertIn("supportedAlgorithms", capabilities)
        self.assertIn("supportedDataTypes", capabilities)

        # 验证支持的算法
        algorithms = capabilities["supportedAlgorithms"]
        expected_algorithms = [
            "FEDERATED_AVERAGING",
            "FEDERATED_PROXIMAL",
            "FEDERATED_NOVA",
            "SCAFFOLD"
        ]
        for alg in expected_algorithms:
            self.assertIn(alg, algorithms)

    def test_task_management(self):
        """测试任务管理"""
        # 创建任务配置
        task_config = {
            "taskId": "task_001",
            "federatedAlgorithm": "FEDERATED_AVERAGING",
            "totalRounds": 10,
            "localTrainingConfig": {
                "datasetId": "dataset_001",
                "learningRate": 0.01
            }
        }

        # 创建任务上下文
        task_context = TaskContext("task_001", task_config)

        self.assertEqual(task_context.task_id, "task_001")
        self.assertEqual(task_context.status, "INITIALIZING")
        self.assertEqual(task_context.current_round, 0)
        self.assertFalse(task_context.is_training)

    @patch('feduwacomm.ml.websocket.client.websocket.WebSocketApp')
    def test_connection_attempt(self, mock_websocket_app):
        """测试连接尝试"""
        # 模拟WebSocket应用
        mock_ws_instance = Mock()
        mock_websocket_app.return_value = mock_ws_instance

        # 尝试连接
        self.client.connect()

        # 验证WebSocket创建
        mock_websocket_app.assert_called_once()
        call_args = mock_websocket_app.call_args

        # 验证URL包含正确参数
        url = call_args[0][0]
        self.assertIn(self.access_token, url)
        self.assertIn(self.vm_id, url)

        # 验证回调函数设置
        self.assertIsNotNone(call_args[1]['on_open'])
        self.assertIsNotNone(call_args[1]['on_message'])
        self.assertIsNotNone(call_args[1]['on_error'])
        self.assertIsNotNone(call_args[1]['on_close'])

class TestTaskContext(unittest.TestCase):
    """任务上下文测试"""

    def setUp(self):
        self.task_config = {
            "taskId": "task_001",
            "federatedAlgorithm": "FEDERATED_AVERAGING",
            "totalRounds": 10,
            "initialGlobalModel": {
                "model_type": "RandomForest",
                "model_params": {"n_estimators": 100}
            },
            "localTrainingConfig": {
                "datasetId": "dataset_001",
                "learningRate": 0.01
            }
        }

        self.task_context = TaskContext("task_001", self.task_config)

    def test_initialization(self):
        """测试任务上下文初始化"""
        self.assertEqual(self.task_context.task_id, "task_001")
        self.assertEqual(self.task_context.status, "INITIALIZING")
        self.assertEqual(self.task_context.current_round, 0)
        self.assertEqual(self.task_context.progress, 0.0)
        self.assertIsNone(self.task_context.trainer)

    @patch('feduwacomm.ml.federated.task_executor.TaskExecutor')
    def test_trainer_initialization(self, mock_task_executor):
        """测试训练器初始化"""
        # 模拟训练器
        mock_trainer = Mock()
        mock_trainer.load_global_model.return_value = True
        mock_trainer.load_training_data.return_value = True
        mock_task_executor.return_value = mock_trainer

        # 初始化训练器
        success = self.task_context.initialize_trainer()

        self.assertTrue(success)
        self.assertEqual(self.task_context.status, "READY")
        self.assertIsNotNone(self.task_context.trainer)

        # 验证训练器方法调用
        mock_trainer.load_global_model.assert_called_once()
        mock_trainer.load_training_data.assert_called_once()

    def test_cleanup(self):
        """测试资源清理"""
        # 设置一些状态
        self.task_context.status = "TRAINING"
        self.task_context.is_training = True

        # 执行清理
        self.task_context.cleanup()

        # 验证状态重置
        self.assertEqual(self.task_context.status, "CLEANED")

class TestMessageRouter(unittest.TestCase):
    """消息路由器测试"""

    def setUp(self):
        self.mock_client = Mock()
        self.mock_client.vm_id = "test_vm_001"
        self.mock_client.active_tasks = {}
        self.mock_client._generate_message_id.return_value = "msg_001"
        self.mock_client._get_current_timestamp.return_value = 1234567890

        from feduwacomm.ml.websocket.client import MessageRouter
        self.router = MessageRouter(self.mock_client)

    def test_connect_ack_handling(self):
        """测试连接确认处理"""
        message = {
            "type": "CONNECT_ACK",
            "data": {"status": "SUCCESS", "message": "连接成功"}
        }

        result = self.router.route_message(message)
        self.assertTrue(result)

    def test_task_start_handling(self):
        """测试任务启动处理"""
        message = {
            "type": "FEDERATED_TASK_START",
            "data": {
                "taskId": "task_001",
                "federatedAlgorithm": "FEDERATED_AVERAGING",
                "totalRounds": 10
            }
        }

        with patch.object(TaskContext, 'initialize_trainer', return_value=True):
            result = self.router.route_message(message)
            self.assertTrue(result)

            # 验证任务已添加
            self.assertIn("task_001", self.mock_client.active_tasks)

    def test_unknown_message_type(self):
        """测试未知消息类型"""
        message = {
            "type": "UNKNOWN_MESSAGE",
            "data": {}
        }

        result = self.router.route_message(message)
        self.assertFalse(result)

if __name__ == '__main__':
    unittest.main()
```

### 6.2 集成测试策略

#### 端到端测试
```python
"""
端到端集成测试
测试完整的WebSocket通信流程
"""

import asyncio
import json
import pytest
from unittest.mock import Mock, patch
import websocket
import threading
import time

class TestIntegrationFlow:
    """集成流程测试"""

    @pytest.fixture
    def setup_test_environment(self):
        """设置测试环境"""
        # 模拟后端服务器
        self.mock_server = MockWebSocketServer()
        self.mock_server.start()

        # 创建客户端
        self.client = FederatedLearningClient(
            "ws://localhost:8081/websocket",
            "test_token",
            "test_vm_001"
        )

        yield

        # 清理
        self.mock_server.stop()
        self.client.disconnect()

    def test_complete_federated_learning_flow(self, setup_test_environment):
        """测试完整联邦学习流程"""
        # 1. 建立连接
        connection_success = self.client.connect()
        assert connection_success

        # 2. 等待连接确认
        time.sleep(1)
        assert self.client.is_connected

        # 3. 模拟任务启动
        task_start_message = {
            "type": "FEDERATED_TASK_START",
            "data": {
                "taskId": "integration_test_task",
                "federatedAlgorithm": "FEDERATED_AVERAGING",
                "totalRounds": 3,
                "initialGlobalModel": {"model_params": {}},
                "localTrainingConfig": {"datasetId": "test_dataset"}
            }
        }

        # 发送任务启动（模拟服务器发送）
        self.mock_server.send_message(task_start_message)

        # 4. 等待任务启动确认
        time.sleep(2)
        assert "integration_test_task" in self.client.active_tasks

        # 5. 模拟轮次训练流程
        for round_num in range(1, 4):
            # 发送轮次开始
            round_start_message = {
                "type": "ROUND_START",
                "data": {
                    "taskId": "integration_test_task",
                    "roundNumber": round_num,
                    "roundSpecificConfig": {}
                }
            }

            self.mock_server.send_message(round_start_message)

            # 等待训练完成和梯度上传
            time.sleep(5)

            # 发送全局模型广播
            model_broadcast_message = {
                "type": "GLOBAL_MODEL_BROADCAST",
                "data": {
                    "taskId": "integration_test_task",
                    "roundNumber": round_num,
                    "globalModel": base64.b64encode(b'{"updated": true}').decode(),
                    "aggregationInfo": {"accuracy": 0.85}
                }
            }

            self.mock_server.send_message(model_broadcast_message)
            time.sleep(1)

        # 6. 验证任务完成
        task_context = self.client.active_tasks["integration_test_task"]
        assert task_context.current_round == 3
        assert task_context.status in ["READY", "TRAINING"]

class MockWebSocketServer:
    """模拟WebSocket服务器"""

    def __init__(self):
        self.is_running = False
        self.server_thread = None
        self.connected_clients = []

    def start(self):
        """启动模拟服务器"""
        self.is_running = True
        self.server_thread = threading.Thread(target=self._run_server, daemon=True)
        self.server_thread.start()
        time.sleep(0.5)  # 等待服务器启动

    def stop(self):
        """停止模拟服务器"""
        self.is_running = False
        if self.server_thread:
            self.server_thread.join(timeout=5)

    def _run_server(self):
        """运行服务器（简化实现）"""
        # 这里应该实现一个真正的WebSocket服务器
        # 为了测试目的，使用简化版本
        pass

    def send_message(self, message: Dict[str, Any]):
        """向客户端发送消息"""
        # 在真实实现中，这里会向连接的客户端发送消息
        pass
```

### 6.3 调试工具和日志

#### 调试配置
```python
"""
调试配置和工具
"""

import logging
import sys
from datetime import datetime

class DebugConfig:
    """调试配置管理"""

    @staticmethod
    def setup_logging(level=logging.INFO, log_file=None):
        """设置日志配置"""

        # 创建formatter
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - [%(filename)s:%(lineno)d] - %(message)s'
        )

        # 设置根logger
        root_logger = logging.getLogger()
        root_logger.setLevel(level)

        # 清除现有handlers
        for handler in root_logger.handlers[:]:
            root_logger.removeHandler(handler)

        # 控制台handler
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(level)
        console_handler.setFormatter(formatter)
        root_logger.addHandler(console_handler)

        # 文件handler（如果指定）
        if log_file:
            file_handler = logging.FileHandler(log_file, encoding='utf-8')
            file_handler.setLevel(level)
            file_handler.setFormatter(formatter)
            root_logger.addHandler(file_handler)

        # 设置WebSocket库日志级别
        websocket_logger = logging.getLogger('websocket')
        websocket_logger.setLevel(logging.WARNING)

    @staticmethod
    def setup_debug_mode():
        """设置调试模式"""
        DebugConfig.setup_logging(level=logging.DEBUG, log_file='debug.log')

        # 启用详细的WebSocket日志
        websocket.enableTrace(True)

        # 设置环境变量
        import os
        os.environ['FEDERATED_LEARNING_DEBUG'] = '1'

class MessageTracer:
    """消息跟踪器"""

    def __init__(self):
        self.sent_messages = []
        self.received_messages = []
        self.logger = logging.getLogger(__name__)

    def trace_sent_message(self, message: Dict[str, Any]):
        """跟踪发送的消息"""
        timestamp = datetime.now().isoformat()
        traced_msg = {
            "timestamp": timestamp,
            "direction": "SENT",
            "message": message
        }
        self.sent_messages.append(traced_msg)
        self.logger.debug(f"发送消息: {message['type']} - {message.get('id', 'NO_ID')}")

    def trace_received_message(self, message: Dict[str, Any]):
        """跟踪接收的消息"""
        timestamp = datetime.now().isoformat()
        traced_msg = {
            "timestamp": timestamp,
            "direction": "RECEIVED",
            "message": message
        }
        self.received_messages.append(traced_msg)
        self.logger.debug(f"接收消息: {message['type']} - {message.get('id', 'NO_ID')}")

    def export_trace(self, filename: str):
        """导出跟踪信息"""
        trace_data = {
            "sent_messages": self.sent_messages,
            "received_messages": self.received_messages,
            "export_time": datetime.now().isoformat()
        }

        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(trace_data, f, indent=2, ensure_ascii=False)

        self.logger.info(f"消息跟踪导出到: {filename}")

# 性能监控
class PerformanceMonitor:
    """性能监控器"""

    def __init__(self):
        self.metrics = {}
        self.start_times = {}

    def start_timer(self, name: str):
        """开始计时"""
        self.start_times[name] = time.time()

    def end_timer(self, name: str):
        """结束计时"""
        if name in self.start_times:
            duration = time.time() - self.start_times[name]
            if name not in self.metrics:
                self.metrics[name] = []
            self.metrics[name].append(duration)
            del self.start_times[name]
            return duration
        return None

    def get_average_time(self, name: str) -> Optional[float]:
        """获取平均时间"""
        if name in self.metrics and self.metrics[name]:
            return sum(self.metrics[name]) / len(self.metrics[name])
        return None

    def export_metrics(self, filename: str):
        """导出性能指标"""
        summary = {}
        for name, times in self.metrics.items():
            summary[name] = {
                "count": len(times),
                "average": sum(times) / len(times),
                "min": min(times),
                "max": max(times),
                "total": sum(times)
            }

        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)
```

---

## 7. 部署和维护

### 7.1 配置管理

#### 配置文件结构
```python
"""
配置管理模块
支持多环境配置和动态更新
"""

import json
import os
from typing import Dict, Any, Optional
from dataclasses import dataclass, asdict
from pathlib import Path

@dataclass
class WebSocketConfig:
    """WebSocket配置"""
    server_url: str = "ws://localhost:8080/websocket"
    access_token: str = ""
    reconnect_attempts: int = 5
    heartbeat_interval: int = 30
    connection_timeout: int = 10

@dataclass
class TaskConfig:
    """任务配置"""
    max_concurrent_tasks: int = 3
    training_timeout: int = 300
    gradient_compression: bool = True
    auto_restart_failed_tasks: bool = True

@dataclass
class MLConfig:
    """机器学习配置"""
    default_algorithm: str = "FEDERATED_AVERAGING"
    model_cache_size: int = 100
    feature_extraction_threads: int = 4
    validation_split: float = 0.2

@dataclass
class LoggingConfig:
    """日志配置"""
    level: str = "INFO"
    file_path: Optional[str] = None
    max_file_size: int = 10 * 1024 * 1024  # 10MB
    backup_count: int = 5

@dataclass
class PythonVMConfig:
    """Python VM完整配置"""
    vm_id: str = ""
    environment: str = "development"
    websocket: WebSocketConfig = None
    task: TaskConfig = None
    ml: MLConfig = None
    logging: LoggingConfig = None

    def __post_init__(self):
        if self.websocket is None:
            self.websocket = WebSocketConfig()
        if self.task is None:
            self.task = TaskConfig()
        if self.ml is None:
            self.ml = MLConfig()
        if self.logging is None:
            self.logging = LoggingConfig()

class ConfigManager:
    """配置管理器"""

    def __init__(self, config_dir: str = "config"):
        self.config_dir = Path(config_dir)
        self.config_dir.mkdir(exist_ok=True)

        self._config = None
        self._config_file = None

    def load_config(self, environment: str = "development") -> PythonVMConfig:
        """加载配置"""
        config_file = self.config_dir / f"{environment}.json"

        if config_file.exists():
            with open(config_file, 'r', encoding='utf-8') as f:
                config_data = json.load(f)

            # 递归创建dataclass实例
            config = self._dict_to_dataclass(config_data, PythonVMConfig)
        else:
            # 创建默认配置
            config = PythonVMConfig(environment=environment)
            self.save_config(config, environment)

        self._config = config
        self._config_file = config_file

        return config

    def save_config(self, config: PythonVMConfig, environment: str = None):
        """保存配置"""
        if environment is None:
            environment = config.environment

        config_file = self.config_dir / f"{environment}.json"
        config_dict = asdict(config)

        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(config_dict, f, indent=2, ensure_ascii=False)

    def update_config(self, updates: Dict[str, Any]):
        """更新配置"""
        if self._config is None:
            raise ValueError("配置未加载")

        # 递归更新配置
        self._recursive_update(asdict(self._config), updates)

        # 重新加载配置
        self._config = self._dict_to_dataclass(asdict(self._config), PythonVMConfig)

        # 保存更新后的配置
        self.save_config(self._config)

    def get_config(self) -> PythonVMConfig:
        """获取当前配置"""
        if self._config is None:
            return self.load_config()
        return self._config

    def _dict_to_dataclass(self, data: Dict[str, Any], cls):
        """字典转dataclass"""
        field_types = {f.name: f.type for f in cls.__dataclass_fields__.values()}
        kwargs = {}

        for key, value in data.items():
            if key in field_types:
                field_type = field_types[key]

                # 检查是否是dataclass类型
                if hasattr(field_type, '__dataclass_fields__'):
                    kwargs[key] = self._dict_to_dataclass(value, field_type)
                else:
                    kwargs[key] = value

        return cls(**kwargs)

    def _recursive_update(self, base_dict: Dict, update_dict: Dict):
        """递归更新字典"""
        for key, value in update_dict.items():
            if key in base_dict and isinstance(base_dict[key], dict) and isinstance(value, dict):
                self._recursive_update(base_dict[key], value)
            else:
                base_dict[key] = value

# 配置文件示例
DEVELOPMENT_CONFIG = {
    "vm_id": "python_vm_dev_001",
    "environment": "development",
    "websocket": {
        "server_url": "ws://localhost:8080/websocket",
        "access_token": "dev_token_12345",
        "reconnect_attempts": 3,
        "heartbeat_interval": 15,
        "connection_timeout": 5
    },
    "task": {
        "max_concurrent_tasks": 2,
        "training_timeout": 120,
        "gradient_compression": True,
        "auto_restart_failed_tasks": True
    },
    "ml": {
        "default_algorithm": "FEDERATED_AVERAGING",
        "model_cache_size": 50,
        "feature_extraction_threads": 2,
        "validation_split": 0.2
    },
    "logging": {
        "level": "DEBUG",
        "file_path": "logs/python_vm_dev.log",
        "max_file_size": 5242880,
        "backup_count": 3
    }
}

PRODUCTION_CONFIG = {
    "vm_id": "python_vm_prod_001",
    "environment": "production",
    "websocket": {
        "server_url": "wss://federated.example.com/websocket",
        "access_token": "${FEDERATED_ACCESS_TOKEN}",
        "reconnect_attempts": 10,
        "heartbeat_interval": 30,
        "connection_timeout": 15
    },
    "task": {
        "max_concurrent_tasks": 5,
        "training_timeout": 600,
        "gradient_compression": True,
        "auto_restart_failed_tasks": False
    },
    "ml": {
        "default_algorithm": "FEDERATED_AVERAGING",
        "model_cache_size": 200,
        "feature_extraction_threads": 8,
        "validation_split": 0.15
    },
    "logging": {
        "level": "INFO",
        "file_path": "/var/log/federated/python_vm.log",
        "max_file_size": 52428800,
        "backup_count": 10
    }
}
```

### 7.2 启动脚本和服务管理

#### 启动脚本
```python
#!/usr/bin/env python3
"""
Python VM WebSocket 客户端启动脚本
"""

import sys
import os
import signal
import argparse
import logging
from pathlib import Path

# 添加项目路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root / "src"))

from feduwacomm.ml.websocket.client import FederatedLearningClient
from feduwacomm.ml.websocket.config import ConfigManager, DebugConfig

class PythonVMService:
    """Python VM 服务管理器"""

    def __init__(self, environment: str = "development"):
        self.environment = environment
        self.config_manager = ConfigManager()
        self.config = None
        self.client = None
        self.is_running = False

        # 注册信号处理
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)

    def start(self):
        """启动服务"""
        try:
            # 加载配置
            self.config = self.config_manager.load_config(self.environment)

            # 设置日志
            self._setup_logging()

            logger = logging.getLogger(__name__)
            logger.info(f"启动Python VM服务，环境: {self.environment}")
            logger.info(f"VM ID: {self.config.vm_id}")
            logger.info(f"服务器地址: {self.config.websocket.server_url}")

            # 创建WebSocket客户端
            self.client = FederatedLearningClient(
                server_url=self.config.websocket.server_url,
                access_token=self._resolve_token(),
                vm_id=self.config.vm_id
            )

            # 应用配置
            self.client.max_concurrent_tasks = self.config.task.max_concurrent_tasks
            self.client.heartbeat_interval = self.config.websocket.heartbeat_interval
            self.client.max_reconnect_attempts = self.config.websocket.reconnect_attempts

            # 启动客户端
            self.is_running = True
            logger.info("开始连接WebSocket服务器...")

            # 阻塞运行
            self.client.connect()

        except KeyboardInterrupt:
            logger.info("收到中断信号，正在停止服务...")
        except Exception as e:
            logger.error(f"服务启动失败: {e}")
            return 1
        finally:
            self.stop()

        return 0

    def stop(self):
        """停止服务"""
        if self.is_running:
            logger = logging.getLogger(__name__)
            logger.info("正在停止Python VM服务...")

            self.is_running = False

            if self.client:
                self.client.disconnect()

            logger.info("Python VM服务已停止")

    def _setup_logging(self):
        """设置日志"""
        log_config = self.config.logging

        level = getattr(logging, log_config.level.upper(), logging.INFO)

        if self.environment == "development":
            DebugConfig.setup_logging(level=level, log_file=log_config.file_path)
        else:
            # 生产环境日志设置
            logging.basicConfig(
                level=level,
                format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                handlers=[
                    logging.StreamHandler(),
                    logging.FileHandler(log_config.file_path, encoding='utf-8')
                ] if log_config.file_path else [logging.StreamHandler()]
            )

    def _resolve_token(self) -> str:
        """解析访问令牌（支持环境变量）"""
        token = self.config.websocket.access_token

        # 支持环境变量替换
        if token.startswith("${") and token.endswith("}"):
            env_var = token[2:-1]
            token = os.getenv(env_var, token)

        return token

    def _signal_handler(self, signum, frame):
        """信号处理器"""
        logger = logging.getLogger(__name__)
        logger.info(f"收到信号 {signum}，准备停止服务")
        self.stop()
        sys.exit(0)

def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="Python VM WebSocket 客户端")
    parser.add_argument(
        "--env",
        default="development",
        choices=["development", "testing", "production"],
        help="运行环境"
    )
    parser.add_argument(
        "--vm-id",
        help="虚拟机ID（覆盖配置文件）"
    )
    parser.add_argument(
        "--server-url",
        help="服务器URL（覆盖配置文件）"
    )
    parser.add_argument(
        "--debug",
        action="store_true",
        help="启用调试模式"
    )

    args = parser.parse_args()

    # 创建服务实例
    service = PythonVMService(args.env)

    # 覆盖配置（如果提供）
    if args.vm_id or args.server_url:
        config_updates = {}
        if args.vm_id:
            config_updates["vm_id"] = args.vm_id
        if args.server_url:
            config_updates["websocket"] = {"server_url": args.server_url}

        # 先加载基础配置
        service.config = service.config_manager.load_config(args.env)
        service.config_manager.update_config(config_updates)

    # 启用调试模式
    if args.debug:
        DebugConfig.setup_debug_mode()

    # 启动服务
    exit_code = service.start()
    sys.exit(exit_code)

if __name__ == "__main__":
    main()
```

#### systemd 服务文件
```ini
# /etc/systemd/system/python-vm-federated.service
[Unit]
Description=Python VM Federated Learning Client
After=network.target
Wants=network.target

[Service]
Type=simple
User=federated
Group=federated
WorkingDirectory=/opt/federated-learning/python-vm
Environment=PYTHONPATH=/opt/federated-learning/python-vm/src
Environment=FEDERATED_ACCESS_TOKEN=your_production_token_here
ExecStart=/usr/bin/python3 /opt/federated-learning/python-vm/scripts/start_vm.py --env production
ExecStop=/bin/kill -TERM $MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

# 安全设置
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/log/federated /tmp

[Install]
WantedBy=multi-user.target
```

### 7.3 监控和故障排除

#### 健康检查脚本
```python
#!/usr/bin/env python3
"""
Python VM 健康检查脚本
"""

import json
import time
import requests
import psutil
import logging
from pathlib import Path
from typing import Dict, Any, List

class HealthChecker:
    """健康检查器"""

    def __init__(self, config_file: str = "config/production.json"):
        self.config_file = Path(config_file)
        self.config = self._load_config()
        self.checks = []

        # 设置日志
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)

    def _load_config(self) -> Dict[str, Any]:
        """加载配置"""
        if self.config_file.exists():
            with open(self.config_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {}

    def add_check(self, name: str, check_func, critical: bool = True):
        """添加检查项"""
        self.checks.append({
            "name": name,
            "check_func": check_func,
            "critical": critical
        })

    def run_all_checks(self) -> Dict[str, Any]:
        """运行所有检查"""
        results = {
            "timestamp": time.time(),
            "overall_status": "HEALTHY",
            "checks": {}
        }

        critical_failed = False

        for check in self.checks:
            try:
                result = check["check_func"]()
                results["checks"][check["name"]] = {
                    "status": "PASS" if result["healthy"] else "FAIL",
                    "details": result,
                    "critical": check["critical"]
                }

                if not result["healthy"] and check["critical"]:
                    critical_failed = True

            except Exception as e:
                results["checks"][check["name"]] = {
                    "status": "ERROR",
                    "details": {"error": str(e)},
                    "critical": check["critical"]
                }

                if check["critical"]:
                    critical_failed = True

        if critical_failed:
            results["overall_status"] = "UNHEALTHY"

        return results

    def check_process_running(self) -> Dict[str, Any]:
        """检查进程是否运行"""
        vm_id = self.config.get("vm_id", "python_vm")

        for proc in psutil.process_iter(['pid', 'name', 'cmdline']):
            try:
                cmdline = ' '.join(proc.info['cmdline'] or [])
                if 'start_vm.py' in cmdline and vm_id in cmdline:
                    return {
                        "healthy": True,
                        "process_id": proc.info['pid'],
                        "command": cmdline
                    }
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                continue

        return {
            "healthy": False,
            "message": "Python VM进程未运行"
        }

    def check_websocket_connection(self) -> Dict[str, Any]:
        """检查WebSocket连接状态"""
        # 这里可以通过检查日志文件或状态文件来判断连接状态
        log_file = self.config.get("logging", {}).get("file_path")

        if not log_file or not Path(log_file).exists():
            return {
                "healthy": False,
                "message": "日志文件不存在"
            }

        # 检查最近的日志条目
        try:
            with open(log_file, 'r', encoding='utf-8') as f:
                lines = f.readlines()
                recent_lines = lines[-50:]  # 最近50行

            # 查找连接相关的日志
            connected = False
            last_heartbeat = None

            for line in reversed(recent_lines):
                if "WebSocket连接已建立" in line:
                    connected = True
                    break
                elif "发送心跳消息" in line:
                    last_heartbeat = line.split()[0:2]  # 提取时间戳
                    break
                elif "WebSocket连接已关闭" in line:
                    break

            if connected or last_heartbeat:
                return {
                    "healthy": True,
                    "connected": connected,
                    "last_heartbeat": last_heartbeat
                }
            else:
                return {
                    "healthy": False,
                    "message": "未找到活跃连接迹象"
                }

        except Exception as e:
            return {
                "healthy": False,
                "message": f"检查日志失败: {e}"
            }

    def check_system_resources(self) -> Dict[str, Any]:
        """检查系统资源"""
        cpu_percent = psutil.cpu_percent(interval=1)
        memory = psutil.virtual_memory()
        disk = psutil.disk_usage('/')

        # 资源阈值
        cpu_threshold = 90.0
        memory_threshold = 90.0
        disk_threshold = 90.0

        issues = []

        if cpu_percent > cpu_threshold:
            issues.append(f"CPU使用率过高: {cpu_percent:.1f}%")

        if memory.percent > memory_threshold:
            issues.append(f"内存使用率过高: {memory.percent:.1f}%")

        if disk.percent > disk_threshold:
            issues.append(f"磁盘使用率过高: {disk.percent:.1f}%")

        return {
            "healthy": len(issues) == 0,
            "cpu_percent": cpu_percent,
            "memory_percent": memory.percent,
            "disk_percent": disk.percent,
            "issues": issues
        }

    def check_database_connection(self) -> Dict[str, Any]:
        """检查数据库连接"""
        try:
            from feduwacomm.database.database import DatabaseManager

            db = DatabaseManager()

            # 执行简单查询测试连接
            result = db.fetch_one("SELECT 1 as test")

            if result and result.get('test') == 1:
                return {
                    "healthy": True,
                    "message": "数据库连接正常"
                }
            else:
                return {
                    "healthy": False,
                    "message": "数据库查询返回异常结果"
                }

        except Exception as e:
            return {
                "healthy": False,
                "message": f"数据库连接失败: {e}"
            }

def main():
    """主函数"""
    checker = HealthChecker()

    # 添加检查项
    checker.add_check("process", checker.check_process_running, critical=True)
    checker.add_check("websocket", checker.check_websocket_connection, critical=True)
    checker.add_check("resources", checker.check_system_resources, critical=False)
    checker.add_check("database", checker.check_database_connection, critical=True)

    # 运行检查
    results = checker.run_all_checks()

    # 输出结果
    print(json.dumps(results, indent=2, ensure_ascii=False))

    # 根据结果设置退出代码
    if results["overall_status"] == "HEALTHY":
        return 0
    else:
        return 1

if __name__ == "__main__":
    import sys
    sys.exit(main())
```

### 7.4 故障排除指南

#### 常见问题和解决方案
```markdown
# Python VM WebSocket 故障排除指南

## 1. 连接问题

### 问题：无法连接到WebSocket服务器
**症状**：
- 日志显示"连接WebSocket服务器失败"
- 客户端状态为DISCONNECTED或ERROR

**可能原因和解决方案**：

1. **网络连接问题**
   ```bash
   # 测试网络连通性
   ping websocket-server.example.com
   telnet websocket-server.example.com 8080
   ```

2. **认证令牌问题**
   ```bash
   # 检查环境变量
   echo $FEDERATED_ACCESS_TOKEN

   # 验证令牌格式
   python3 -c "import jwt; print(jwt.decode('YOUR_TOKEN', verify=False))"
   ```

3. **配置文件错误**
   ```bash
   # 验证配置文件
   python3 -c "import json; print(json.load(open('config/production.json')))"
   ```

### 问题：频繁断线重连
**症状**：
- 日志中反复出现"WebSocket连接已关闭"和重连消息
- 心跳超时

**解决方案**：
1. 检查网络稳定性
2. 调整心跳间隔：`heartbeat_interval: 60`
3. 增加重连次数：`reconnect_attempts: 10`

## 2. 任务执行问题

### 问题：任务初始化失败
**症状**：
- 收到FEDERATED_TASK_START但返回FAILED状态
- 日志显示"任务初始化失败"

**诊断步骤**：
```python
# 检查数据集是否存在
from feduwacomm.database.database import DatabaseManager
db = DatabaseManager()
result = db.fetch_all("SELECT * FROM training_dataset WHERE dataset_id = %s", ("your_dataset_id",))
print(f"数据集记录数: {len(result)}")

# 检查模型配置
import json
config = json.load(open('config/production.json'))
print(f"支持的算法: {config['ml']['default_algorithm']}")
```

### 问题：训练过程中内存不足
**症状**：
- 训练进程被杀死
- 系统日志显示OOM错误

**解决方案**：
1. 减少并发任务数：`max_concurrent_tasks: 2`
2. 增加系统内存
3. 启用梯度压缩：`gradient_compression: true`

## 3. 性能问题

### 问题：训练速度过慢
**诊断方法**：
```python
# 启用性能监控
from feduwacomm.ml.websocket.debug import PerformanceMonitor
monitor = PerformanceMonitor()

# 在训练前后添加计时
monitor.start_timer("training_round")
# ... 训练代码 ...
duration = monitor.end_timer("training_round")
print(f"训练耗时: {duration:.2f}秒")
```

**优化措施**：
1. 调整特征提取线程数：`feature_extraction_threads: 8`
2. 使用SSD存储
3. 优化数据预处理流程

## 4. 日志分析

### 关键日志模式
```bash
# 连接成功
grep "WebSocket连接已建立" /var/log/federated/python_vm.log

# 任务状态变化
grep "任务.*状态变更" /var/log/federated/python_vm.log

# 错误信息
grep "ERROR" /var/log/federated/python_vm.log | tail -20

# 性能统计
grep "训练完成.*用时" /var/log/federated/python_vm.log
```

## 5. 紧急恢复

### 服务无响应时的恢复步骤
```bash
# 1. 检查进程状态
ps aux | grep start_vm.py

# 2. 强制停止服务
sudo systemctl stop python-vm-federated

# 3. 清理僵尸进程
sudo pkill -f start_vm.py

# 4. 检查磁盘空间
df -h

# 5. 清理日志文件（如果需要）
sudo truncate -s 0 /var/log/federated/python_vm.log

# 6. 重启服务
sudo systemctl start python-vm-federated

# 7. 监控启动过程
sudo journalctl -u python-vm-federated -f
```

## 6. 联系支持

当遇到无法解决的问题时，请收集以下信息：

1. **系统信息**
   ```bash
   uname -a
   python3 --version
   pip list | grep -E "(websocket|sklearn|pandas)"
   ```

2. **配置信息**
   ```bash
   cat config/production.json | jq .
   ```

3. **日志信息**
   ```bash
   tail -100 /var/log/federated/python_vm.log
   ```

4. **资源使用情况**
   ```bash
   free -h
   df -h
   top -bn1 | head -20
   ```
```

---

## 总结

本文档提供了 Python 虚拟机 WebSocket 协议重构的全面指导，涵盖了从概念设计到具体实现，从测试验证到部署维护的完整流程。

### 重构要点回顾

1. **架构简化**：从复杂的 STOMP 协议转向简化的 v1.4 协议
2. **角色明确**：Python VM 作为"手脚"的被动执行角色
3. **多任务支持**：通过 taskId 实现精确的任务级控制
4. **集成兼容**：与现有 ML 模块保持良好兼容性

### 实施建议

- **渐进式升级**：按阶段实施，确保每个阶段的稳定性
- **充分测试**：重点测试多任务并发和异常恢复场景
- **监控完善**：建立完整的监控和日志体系
- **文档维护**：随着实施进展及时更新文档

通过遵循本指导文档，Python VM 的 WebSocket 协议重构将能够顺利完成，并为联邦学习系统提供更加稳定、高效的虚拟机支持。