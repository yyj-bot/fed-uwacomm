# Python VM 架构设计变化

本文档详细描述 Python VM 在新架构下的设计变化，包括角色定位、模块重构和多任务管理。

## 1. 角色定位变化

### 1.1 设计理念转变

#### 从复杂协商到被动执行

**旧架构：复杂协商模式**
```python
# 旧的复杂决策逻辑
class OldVMClient:
    def handle_training_request(self, request):
        # VM 自己做决策
        if self.should_accept_task(request):
            if self.negotiate_parameters(request):
                if self.validate_resources(request):
                    self.start_training(request)
                    self.manage_training_lifecycle()
                else:
                    self.reject_with_reason("insufficient_resources")
            else:
                self.reject_with_reason("parameter_mismatch")
        else:
            self.reject_with_reason("task_queue_full")

    def should_accept_task(self, request):
        # 问题：VM不应该做这样的决策
        return len(self.active_tasks) < self.max_tasks

    def negotiate_parameters(self, request):
        # 问题：复杂的协商过程
        return self.match_capabilities(request.requirements)
```

**新架构：被动执行模式**
```python
# 新的被动响应逻辑
class NewVMClient:
    def handle_task_start(self, message):
        # VM 只执行，不决策
        task_data = message["data"]
        task_id = task_data["taskId"]

        # 直接执行后端指令
        try:
            task_context = self.create_task_context(task_id, task_data)
            success = task_context.initialize()

            # 简单的状态报告
            self.send_task_start_ack(task_id, "SUCCESS" if success else "FAILED")

        except Exception as e:
            # 错误报告，不做恢复决策
            self.send_error_report(task_id, str(e))
```

### 1.2 职责对比

#### 旧架构职责（过重）
```
Python VM (旧):
├── 复杂状态管理
│   ├── 维护训练状态机
│   ├── 管理任务优先级
│   └── 处理状态冲突
├── 协商决策逻辑
│   ├── 参数协商
│   ├── 资源分配决策
│   └── 任务调度决策
├── 主动任务调度
│   ├── 任务队列管理
│   ├── 执行顺序决策
│   └── 资源竞争处理
└── 分布式状态同步
    ├── 与其他VM同步
    ├── 状态一致性保证
    └── 冲突解决机制

问题：职责过重，容易出错，难以调试
```

#### 新架构职责（简化）
```
Python VM (新):
├── 被动响应指令
│   ├── 接收后端指令
│   ├── 执行指定操作
│   └── 报告执行结果
├── 简单状态报告
│   ├── 定期发送心跳
│   ├── 报告任务状态
│   └── 上报异常情况
├── 专注训练执行
│   ├── 本地模型训练
│   ├── 梯度计算上传
│   └── 模型更新应用
└── 多任务隔离管理
    ├── 基于taskId隔离
    ├── 资源独立分配
    └── 并发执行管理

优势：职责清晰，稳定可靠，易于维护
```

## 2. 模块设计重构

### 2.1 现有模块分析

#### 当前模块结构的问题
```python
# 现有模块结构
feduwacomm.ml.api/
├── websocket_client.py      # STOMP 协议客户端
├── message_handler.py       # 复杂消息处理逻辑
└── vm_api_client.py         # API 客户端

feduwacomm.ml.federated/
├── client.py                # 联邦学习客户端
├── coordinator.py           # 协调器（问题：职责过重）
└── config.py                # 配置管理

问题分析：
1. coordinator.py 承担了过多决策职责
2. message_handler.py 处理逻辑复杂
3. 缺乏多任务管理机制
4. 状态管理分散在各个模块
```

### 2.2 新模块设计

#### 重构后的模块结构
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

feduwacomm.ml.legacy/        # 保留旧代码
├── stomp_client.py          # 原STOMP客户端
└── old_coordinator.py       # 原协调器

优势：
1. 职责分离更清晰
2. 支持多任务并发
3. 便于测试和维护
4. 支持渐进式迁移
```

#### 核心模块详细设计

**1. WebSocket客户端 (client.py)**
```python
class FederatedLearningClient:
    """
    v1.4协议WebSocket客户端
    职责：连接管理、消息收发、状态维护
    """
    def __init__(self, server_url, access_token, vm_id):
        self.server_url = server_url
        self.vm_id = vm_id
        self.active_tasks = {}  # taskId -> TaskContext
        self.message_router = MessageRouter(self)

    def connect(self):
        """建立WebSocket连接"""
        # 简化的连接逻辑，无需协商

    def handle_message(self, message):
        """消息处理统一入口"""
        return self.message_router.route_message(message)

    def send_heartbeat(self):
        """发送心跳，报告所有任务状态"""
```

**2. 消息路由器 (message_router.py)**
```python
class MessageRouter:
    """
    消息路由器
    职责：根据消息类型和taskId路由到正确的处理器
    """
    def __init__(self, client):
        self.client = client
        self.handlers = {
            "FEDERATED_TASK_START": self._handle_task_start,
            "ROUND_START": self._handle_round_start,
            "GLOBAL_MODEL_BROADCAST": self._handle_model_broadcast,
            # ... 其他34个消息类型
        }

    def route_message(self, message):
        """根据消息类型路由"""
        msg_type = message.get("type")
        task_id = message.get("data", {}).get("taskId")

        if task_id:
            # 任务级消息路由
            return self._route_to_task(task_id, message)
        else:
            # 全局消息处理
            return self.handlers.get(msg_type, self._handle_unknown)(message)
```

**3. 多任务管理器 (task_manager.py)**
```python
class TaskManager:
    """
    多任务管理器
    职责：任务隔离、并发控制、资源管理
    """
    def __init__(self, max_concurrent_tasks=3):
        self.max_concurrent_tasks = max_concurrent_tasks
        self.active_tasks = {}  # taskId -> TaskContext
        self.task_executor = ThreadPoolExecutor(max_workers=max_concurrent_tasks)

    def can_accept_task(self):
        """检查是否可以接受新任务"""
        return len(self.active_tasks) < self.max_concurrent_tasks

    def create_task(self, task_id, config):
        """创建新任务"""
        if not self.can_accept_task():
            return False

        task_context = TaskContext(task_id, config)
        self.active_tasks[task_id] = task_context
        return True

    def execute_training(self, task_id, round_config):
        """提交训练任务到线程池"""
        if task_id in self.active_tasks:
            future = self.task_executor.submit(
                self.active_tasks[task_id].execute_training,
                round_config
            )
            return future
        return None
```

**4. 任务执行器 (task_executor.py)**
```python
class TaskExecutor:
    """
    任务执行器（简化版）
    职责：纯粹的训练执行，移除决策逻辑
    """
    def __init__(self, task_id, algorithm, config):
        self.task_id = task_id
        self.algorithm = algorithm
        self.config = config
        self.trainer = None

    def initialize(self):
        """初始化训练器"""
        # 根据算法类型创建训练器
        if self.algorithm == "FEDERATED_AVERAGING":
            self.trainer = FedAvgTrainer(self.config)
        # ... 其他算法

    def execute_round(self, round_config):
        """执行一轮训练"""
        # 简化的训练执行，无决策逻辑
        return self.trainer.train(round_config)

    def extract_gradients(self):
        """提取梯度"""
        return self.trainer.get_gradients()
```

### 2.3 任务上下文设计

#### TaskContext 类设计
```python
class TaskContext:
    """
    任务上下文
    职责：单个任务的状态管理和执行控制
    """
    def __init__(self, task_id, config):
        self.task_id = task_id
        self.config = config
        self.status = "INITIALIZING"
        self.current_round = 0
        self.progress = 0.0

        # 执行组件
        self.executor = None
        self.training_thread = None

        # 状态管理
        self.last_activity = time.time()
        self.error_count = 0

    def initialize(self):
        """初始化任务"""
        try:
            self.executor = TaskExecutor(
                self.task_id,
                self.config["federatedAlgorithm"],
                self.config["localTrainingConfig"]
            )
            success = self.executor.initialize()
            self.status = "READY" if success else "FAILED"
            return success
        except Exception as e:
            self.status = "FAILED"
            self.error_count += 1
            return False

    def start_round(self, round_number, round_config):
        """开始新轮次"""
        self.current_round = round_number
        self.status = "TRAINING"
        self.last_activity = time.time()

        # 异步执行训练
        self.training_thread = threading.Thread(
            target=self._execute_training,
            args=(round_config,),
            daemon=True
        )
        self.training_thread.start()

    def _execute_training(self, round_config):
        """执行训练（在独立线程中）"""
        try:
            result = self.executor.execute_round(round_config)
            self.status = "GRADIENT_READY"
            self.progress = 100.0

            # 通知主线程上传梯度
            self._notify_gradient_ready(result)

        except Exception as e:
            self.status = "ERROR"
            self.error_count += 1
            self._notify_error(e)

    def update_global_model(self, model_data):
        """更新全局模型"""
        try:
            success = self.executor.update_model(model_data)
            if success:
                self.status = "READY"
                self.progress = 0.0
                self.last_activity = time.time()
            return success
        except Exception as e:
            self.error_count += 1
            return False

    def cleanup(self):
        """清理资源"""
        if self.training_thread and self.training_thread.is_alive():
            # 等待训练线程完成
            self.training_thread.join(timeout=10)

        if self.executor:
            self.executor.cleanup()

        self.status = "CLEANED"
```

## 3. 多任务管理架构

### 3.1 任务隔离策略

#### 资源隔离设计
```python
class ResourceManager:
    """资源管理器"""
    def __init__(self):
        self.cpu_allocations = {}  # taskId -> cpu_percentage
        self.memory_limits = {}    # taskId -> memory_mb
        self.io_quotas = {}        # taskId -> io_quota

    def allocate_resources(self, task_id, requirements):
        """为任务分配资源"""
        # 基于任务要求分配资源
        cpu_needed = requirements.get("cpu_percentage", 25)
        memory_needed = requirements.get("memory_mb", 512)

        if self._can_allocate(cpu_needed, memory_needed):
            self.cpu_allocations[task_id] = cpu_needed
            self.memory_limits[task_id] = memory_needed
            return True
        return False

    def _can_allocate(self, cpu_needed, memory_needed):
        """检查是否有足够资源"""
        total_cpu = sum(self.cpu_allocations.values())
        total_memory = sum(self.memory_limits.values())

        return (total_cpu + cpu_needed <= 90 and  # 保留10% CPU
                total_memory + memory_needed <= self._get_available_memory() * 0.8)
```

#### 数据隔离设计
```python
class DataManager:
    """数据管理器"""
    def __init__(self):
        self.task_datasets = {}  # taskId -> dataset_info
        self.task_models = {}    # taskId -> model_info

    def load_task_data(self, task_id, dataset_id):
        """为特定任务加载数据"""
        # 每个任务使用独立的数据空间
        dataset = self._load_dataset(dataset_id)
        self.task_datasets[task_id] = {
            "data": dataset,
            "access_time": time.time(),
            "usage_count": 0
        }

    def get_task_data(self, task_id):
        """获取任务专用数据"""
        if task_id in self.task_datasets:
            self.task_datasets[task_id]["usage_count"] += 1
            return self.task_datasets[task_id]["data"]
        return None

    def cleanup_task_data(self, task_id):
        """清理任务数据"""
        if task_id in self.task_datasets:
            del self.task_datasets[task_id]
        if task_id in self.task_models:
            del self.task_models[task_id]
```

### 3.2 并发控制机制

#### 线程池管理
```python
import concurrent.futures
from threading import Semaphore

class ConcurrencyController:
    """并发控制器"""
    def __init__(self, max_concurrent_tasks=3):
        self.max_concurrent_tasks = max_concurrent_tasks

        # 线程池管理
        self.training_executor = concurrent.futures.ThreadPoolExecutor(
            max_workers=max_concurrent_tasks,
            thread_name_prefix="training"
        )

        # 信号量控制
        self.training_semaphore = Semaphore(max_concurrent_tasks)

        # 任务状态跟踪
        self.active_futures = {}  # taskId -> Future

    def submit_training_task(self, task_id, training_func, *args, **kwargs):
        """提交训练任务"""
        if task_id in self.active_futures:
            return None  # 任务已在执行

        def wrapped_training():
            with self.training_semaphore:
                try:
                    return training_func(*args, **kwargs)
                finally:
                    # 清理完成的任务
                    if task_id in self.active_futures:
                        del self.active_futures[task_id]

        future = self.training_executor.submit(wrapped_training)
        self.active_futures[task_id] = future
        return future

    def cancel_task(self, task_id):
        """取消任务"""
        if task_id in self.active_futures:
            future = self.active_futures[task_id]
            success = future.cancel()
            if success:
                del self.active_futures[task_id]
            return success
        return False

    def get_active_task_count(self):
        """获取活跃任务数量"""
        return len(self.active_futures)

    def shutdown(self, wait=True):
        """关闭并发控制器"""
        self.training_executor.shutdown(wait=wait)
```

#### 消息队列管理
```python
import queue
import threading

class MessageQueueManager:
    """消息队列管理器"""
    def __init__(self):
        self.task_queues = {}  # taskId -> queue
        self.global_queue = queue.Queue()

        # 消息处理线程
        self.message_processor = threading.Thread(
            target=self._process_messages,
            daemon=True
        )
        self.message_processor.start()

    def route_message(self, message):
        """路由消息到对应队列"""
        task_id = message.get("data", {}).get("taskId")

        if task_id:
            # 任务级消息
            if task_id not in self.task_queues:
                self.task_queues[task_id] = queue.Queue()
            self.task_queues[task_id].put(message)
        else:
            # 全局消息
            self.global_queue.put(message)

    def _process_messages(self):
        """消息处理循环"""
        while True:
            try:
                # 处理全局消息
                try:
                    message = self.global_queue.get(timeout=0.1)
                    self._handle_global_message(message)
                except queue.Empty:
                    pass

                # 处理任务消息
                for task_id, task_queue in self.task_queues.items():
                    try:
                        message = task_queue.get(timeout=0.1)
                        self._handle_task_message(task_id, message)
                    except queue.Empty:
                        continue

            except Exception as e:
                logging.error(f"消息处理错误: {e}")
```

### 3.3 状态同步机制

#### 统一状态管理
```python
class StateManager:
    """状态管理器"""
    def __init__(self):
        self.vm_state = {
            "status": "DISCONNECTED",
            "last_heartbeat": None,
            "resource_usage": {},
            "capabilities": {}
        }

        self.task_states = {}  # taskId -> task_state
        self._state_lock = threading.RLock()

    def update_vm_state(self, state_updates):
        """更新VM状态"""
        with self._state_lock:
            self.vm_state.update(state_updates)
            self.vm_state["last_update"] = time.time()

    def update_task_state(self, task_id, state_updates):
        """更新任务状态"""
        with self._state_lock:
            if task_id not in self.task_states:
                self.task_states[task_id] = {}
            self.task_states[task_id].update(state_updates)
            self.task_states[task_id]["last_update"] = time.time()

    def get_full_state(self):
        """获取完整状态"""
        with self._state_lock:
            return {
                "vm_state": self.vm_state.copy(),
                "task_states": {k: v.copy() for k, v in self.task_states.items()},
                "timestamp": time.time()
            }

    def cleanup_task_state(self, task_id):
        """清理任务状态"""
        with self._state_lock:
            if task_id in self.task_states:
                del self.task_states[task_id]
```

## 4. 架构优势分析

### 4.1 可维护性提升

#### 模块职责清晰
- **单一职责**: 每个模块只负责一个核心功能
- **低耦合**: 模块间依赖关系简单明确
- **高内聚**: 相关功能集中在同一模块

#### 测试友好
- **单元测试**: 每个类都可以独立测试
- **集成测试**: 模块间接口清晰，易于集成测试
- **模拟测试**: 易于创建Mock对象

### 4.2 扩展性增强

#### 算法扩展
```python
# 新增算法只需实现训练器接口
class FedNovaTrainer(TrainerInterface):
    def train(self, config):
        # FedNova算法实现
        pass

    def get_gradients(self):
        # 梯度提取实现
        pass

# 在TaskExecutor中注册
ALGORITHM_TRAINERS = {
    "FEDERATED_AVERAGING": FedAvgTrainer,
    "FEDERATED_NOVA": FedNovaTrainer,  # 新增
}
```

#### 协议扩展
```python
# 新增消息类型只需在路由器中添加处理器
class MessageRouter:
    def __init__(self):
        self.handlers = {
            "FEDERATED_TASK_START": self._handle_task_start,
            "NEW_MESSAGE_TYPE": self._handle_new_message,  # 新增
        }
```

### 4.3 性能优化

#### 并发性能
- **真正的并发**: 多个任务可以真正并行执行
- **资源优化**: 智能的资源分配和管理
- **队列管理**: 高效的消息队列处理

#### 内存优化
- **按需加载**: 只在需要时加载数据和模型
- **资源回收**: 及时清理不再使用的资源
- **内存隔离**: 任务间内存使用独立

---

**下一步**: 继续阅读 [WebSocket客户端实现](./03-websocket-client.md) 了解具体的客户端实现方案。