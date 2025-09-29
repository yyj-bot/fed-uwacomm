# 联邦学习集成指导

本文档详细说明如何在新的 WebSocket 协议 v1.4 架构下集成和重构联邦学习功能，实现高效的多任务并发训练。

## 1. 联邦学习架构重构

### 1.1 架构设计原则

#### 中心化控制理念
```
┌─────────────────────────────────────┐
│            后端服务器 (大脑)           │
│                                     │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│ │任务编排 │ │算法选择 │ │聚合策略 │ │
│ └─────────┘ └─────────┘ └─────────┘ │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│ │轮次控制 │ │状态管理 │ │结果评估 │ │
│ └─────────┘ └─────────┘ └─────────┘ │
└─────────────┬───────────────────────┘
              │ 指令 + 配置
    ┌─────────┼─────────┐
    │         │         │
┌───▼───┐ ┌───▼───┐ ┌───▼───┐
│VM-001 │ │VM-002 │ │VM-003 │
│(执行) │ │(执行) │ │(执行) │
│       │ │       │ │       │
│训练器 │ │训练器 │ │训练器 │
│评估器 │ │评估器 │ │评估器 │
└───────┘ └───────┘ └───────┘
```

#### 职责分离
- **后端职责**: 算法选择、参数配置、轮次控制、聚合策略、全局优化
- **VM 职责**: 数据准备、模型训练、本地评估、梯度计算、结果上传

### 1.2 新架构优势

#### 与旧架构对比
| 特性 | 旧架构 (STOMP) | 新架构 (v1.4) |
|------|----------------|---------------|
| 算法配置 | VM 端决定 | 后端统一配置 |
| 训练参数 | 分散协商 | 中心化下发 |
| 聚合策略 | VM 端参与决策 | 后端完全控制 |
| 多任务支持 | 复杂状态管理 | 原生多任务隔离 |
| 错误处理 | 分布式重试 | 集中化恢复 |

## 2. 训练执行器重构

### 2.1 FederatedTrainingExecutor 类设计

#### 核心类结构
```python
class FederatedTrainingExecutor:
    """联邦学习训练执行器 - 专注执行，不做决策"""

    def __init__(self, task_id: str, config: Dict):
        self.task_id = task_id
        self.config = config
        self.algorithm = config.get('algorithm', 'FEDERATED_AVERAGING')
        self.trainer = None
        self.evaluator = None
        self.training_data = None
        self.test_data = None
        self.current_model = None
        self.local_epochs = config.get('localEpochs', 5)
        self.batch_size = config.get('batchSize', 32)
        self.learning_rate = config.get('learningRate', 0.01)

    def initialize(self):
        """初始化训练器和评估器"""
        self._load_training_data()
        self._create_trainer()
        self._create_evaluator()

    def _load_training_data(self):
        """加载任务专用训练数据"""
        data_config = self.config.get('dataConfig', {})
        data_type = data_config.get('type', 'ACOUSTIC')
        dataset_id = data_config.get('datasetId')

        # 从数据库加载训练数据
        from feduwacomm.database import DatabaseManager
        db = DatabaseManager()

        training_records = db.get_training_data_by_task(
            self.task_id,
            dataset_id
        )

        if not training_records:
            raise ValueError(f"No training data found for task {self.task_id}")

        # 转换为训练格式
        self.training_data = self._prepare_training_data(training_records)
        self.test_data = self._prepare_test_data(training_records)

    def _create_trainer(self):
        """根据算法类型创建训练器"""
        algorithm_map = {
            'FEDERATED_AVERAGING': self._create_fedavg_trainer,
            'FEDERATED_PROXIMAL': self._create_fedprox_trainer,
            'FEDERATED_NOVA': self._create_fednova_trainer,
            'SCAFFOLD': self._create_scaffold_trainer
        }

        creator = algorithm_map.get(self.algorithm)
        if not creator:
            raise ValueError(f"Unsupported algorithm: {self.algorithm}")

        self.trainer = creator()

    def _create_fedavg_trainer(self):
        """创建 FedAvg 训练器"""
        from feduwacomm.ml.algorithms import FedAvgTrainer
        return FedAvgTrainer(
            learning_rate=self.learning_rate,
            local_epochs=self.local_epochs,
            batch_size=self.batch_size
        )

    def _create_fedprox_trainer(self):
        """创建 FedProx 训练器"""
        from feduwacomm.ml.algorithms import FedProxTrainer
        mu = self.config.get('proximalTerm', 0.01)
        return FedProxTrainer(
            learning_rate=self.learning_rate,
            local_epochs=self.local_epochs,
            batch_size=self.batch_size,
            mu=mu
        )

    def _create_fednova_trainer(self):
        """创建 FedNova 训练器"""
        from feduwacomm.ml.algorithms import FedNovaTrainer
        return FedNovaTrainer(
            learning_rate=self.learning_rate,
            local_epochs=self.local_epochs,
            batch_size=self.batch_size
        )

    def _create_scaffold_trainer(self):
        """创建 SCAFFOLD 训练器"""
        from feduwacomm.ml.algorithms import ScaffoldTrainer
        return ScaffoldTrainer(
            learning_rate=self.learning_rate,
            local_epochs=self.local_epochs,
            batch_size=self.batch_size
        )
```

### 2.2 训练流程执行

#### 轮次训练实现
```python
def execute_round(self, global_model_data: bytes, round_params: Dict) -> Dict:
    """执行单轮训练 - 纯执行，不做决策"""
    try:
        # 1. 加载全局模型
        self.current_model = self._deserialize_model(global_model_data)

        # 2. 应用后端下发的训练参数
        self._apply_round_parameters(round_params)

        # 3. 执行本地训练
        training_result = self._perform_local_training()

        # 4. 计算梯度
        gradients = self._compute_gradients()

        # 5. 本地评估
        evaluation_result = self._perform_local_evaluation()

        # 6. 准备上传结果
        return {
            'gradients': gradients,
            'evaluation': evaluation_result,
            'training_stats': training_result,
            'model_size': len(global_model_data),
            'samples_count': len(self.training_data),
            'training_time': training_result.get('duration', 0)
        }

    except Exception as e:
        logger.error(f"Round execution failed for task {self.task_id}: {e}")
        raise

def _apply_round_parameters(self, round_params: Dict):
    """应用后端下发的轮次参数"""
    # 更新学习率（如果后端指定）
    if 'learningRate' in round_params:
        self.trainer.learning_rate = round_params['learningRate']

    # 更新本地epoch数
    if 'localEpochs' in round_params:
        self.trainer.local_epochs = round_params['localEpochs']

    # 更新批次大小
    if 'batchSize' in round_params:
        self.trainer.batch_size = round_params['batchSize']

    # 算法特定参数
    algorithm_params = round_params.get('algorithmParams', {})
    if algorithm_params:
        self.trainer.update_parameters(algorithm_params)

def _perform_local_training(self) -> Dict:
    """执行本地训练"""
    start_time = time.time()

    # 使用配置的训练器进行训练
    history = self.trainer.train(
        model=self.current_model,
        train_data=self.training_data,
        epochs=self.local_epochs
    )

    end_time = time.time()

    return {
        'duration': end_time - start_time,
        'loss_history': history.get('loss', []),
        'accuracy_history': history.get('accuracy', []),
        'final_loss': history.get('loss', [])[-1] if history.get('loss') else None
    }

def _compute_gradients(self) -> bytes:
    """计算并序列化梯度"""
    # 获取模型梯度
    gradients = self.trainer.get_gradients()

    # 应用梯度压缩（如果配置）
    compression_config = self.config.get('gradientCompression', {})
    if compression_config.get('enabled', False):
        gradients = self._compress_gradients(gradients, compression_config)

    # 序列化梯度
    serialized_gradients = self._serialize_gradients(gradients)

    return serialized_gradients

def _perform_local_evaluation(self) -> Dict:
    """执行本地评估"""
    if not self.test_data:
        return {'accuracy': 0.0, 'loss': 0.0, 'samples': 0}

    # 使用评估器评估当前模型
    eval_result = self.evaluator.evaluate(
        model=self.current_model,
        test_data=self.test_data
    )

    return {
        'accuracy': eval_result.get('accuracy', 0.0),
        'loss': eval_result.get('loss', 0.0),
        'samples': len(self.test_data),
        'confusion_matrix': eval_result.get('confusion_matrix'),
        'classification_report': eval_result.get('classification_report')
    }
```

### 2.3 模型和梯度处理

#### 序列化和压缩
```python
def _serialize_model(self, model) -> bytes:
    """序列化模型"""
    import pickle
    import gzip

    # 序列化模型
    model_bytes = pickle.dumps(model)

    # 压缩（可选）
    if self.config.get('modelCompression', {}).get('enabled', True):
        model_bytes = gzip.compress(model_bytes)

    return model_bytes

def _deserialize_model(self, model_data: bytes):
    """反序列化模型"""
    import pickle
    import gzip

    try:
        # 尝试解压
        if self.config.get('modelCompression', {}).get('enabled', True):
            model_data = gzip.decompress(model_data)

        # 反序列化
        model = pickle.loads(model_data)
        return model

    except Exception as e:
        logger.error(f"Model deserialization failed: {e}")
        raise

def _serialize_gradients(self, gradients) -> bytes:
    """序列化梯度"""
    import numpy as np
    import pickle

    # 转换为numpy数组格式
    if hasattr(gradients, 'numpy'):
        gradients = gradients.numpy()
    elif isinstance(gradients, dict):
        gradients = {k: v.numpy() if hasattr(v, 'numpy') else v
                    for k, v in gradients.items()}

    # 序列化
    return pickle.dumps(gradients)

def _compress_gradients(self, gradients, compression_config: Dict):
    """梯度压缩"""
    compression_type = compression_config.get('type', 'quantization')

    if compression_type == 'quantization':
        return self._quantize_gradients(gradients, compression_config)
    elif compression_type == 'sparsification':
        return self._sparsify_gradients(gradients, compression_config)
    else:
        return gradients

def _quantize_gradients(self, gradients, config: Dict):
    """梯度量化"""
    bits = config.get('bits', 8)
    # 实现梯度量化逻辑
    # ... 量化算法实现
    return gradients

def _sparsify_gradients(self, gradients, config: Dict):
    """梯度稀疏化"""
    sparsity_ratio = config.get('sparsityRatio', 0.1)
    # 实现梯度稀疏化逻辑
    # ... 稀疏化算法实现
    return gradients
```

## 3. 算法适配器实现

### 3.1 统一算法接口

#### 基础训练器接口
```python
from abc import ABC, abstractmethod
from typing import Dict, Any, Tuple

class FederatedTrainer(ABC):
    """联邦学习训练器基类"""

    def __init__(self, learning_rate: float = 0.01,
                 local_epochs: int = 5, batch_size: int = 32):
        self.learning_rate = learning_rate
        self.local_epochs = local_epochs
        self.batch_size = batch_size

    @abstractmethod
    def train(self, model, train_data, epochs: int) -> Dict:
        """执行训练，返回训练历史"""
        pass

    @abstractmethod
    def get_gradients(self):
        """获取梯度"""
        pass

    @abstractmethod
    def update_parameters(self, params: Dict):
        """更新算法特定参数"""
        pass
```

#### FedAvg 实现
```python
class FedAvgTrainer(FederatedTrainer):
    """联邦平均算法训练器"""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.model = None
        self.initial_weights = None

    def train(self, model, train_data, epochs: int) -> Dict:
        """FedAvg 训练实现"""
        self.model = model
        self.initial_weights = self._get_model_weights(model)

        history = {'loss': [], 'accuracy': []}

        for epoch in range(epochs):
            # 训练一个epoch
            epoch_loss, epoch_acc = self._train_epoch(train_data)
            history['loss'].append(epoch_loss)
            history['accuracy'].append(epoch_acc)

        return history

    def get_gradients(self):
        """计算权重差作为梯度"""
        current_weights = self._get_model_weights(self.model)
        gradients = {}

        for layer_name in current_weights:
            gradients[layer_name] = (
                self.initial_weights[layer_name] - current_weights[layer_name]
            )

        return gradients

    def update_parameters(self, params: Dict):
        """更新FedAvg参数"""
        # FedAvg 通常不需要额外参数
        pass

    def _train_epoch(self, train_data) -> Tuple[float, float]:
        """训练一个epoch"""
        # 实现具体的训练逻辑
        # 这里简化处理，实际应该使用具体的ML框架
        total_loss = 0.0
        total_accuracy = 0.0
        batches = 0

        for batch in self._create_batches(train_data):
            loss, acc = self._train_batch(batch)
            total_loss += loss
            total_accuracy += acc
            batches += 1

        return total_loss / batches, total_accuracy / batches
```

#### FedProx 实现
```python
class FedProxTrainer(FederatedTrainer):
    """联邦近似算法训练器"""

    def __init__(self, mu: float = 0.01, **kwargs):
        super().__init__(**kwargs)
        self.mu = mu  # 近似项系数
        self.global_model_weights = None

    def train(self, model, train_data, epochs: int) -> Dict:
        """FedProx 训练实现"""
        self.model = model
        self.global_model_weights = self._get_model_weights(model)

        history = {'loss': [], 'accuracy': []}

        for epoch in range(epochs):
            # FedProx: 添加近似项的训练
            epoch_loss, epoch_acc = self._train_epoch_with_prox(train_data)
            history['loss'].append(epoch_loss)
            history['accuracy'].append(epoch_acc)

        return history

    def get_gradients(self):
        """获取FedProx梯度"""
        current_weights = self._get_model_weights(self.model)
        gradients = {}

        for layer_name in current_weights:
            # FedProx梯度包含近似项
            weight_diff = (
                self.global_model_weights[layer_name] - current_weights[layer_name]
            )
            prox_term = self.mu * (
                current_weights[layer_name] - self.global_model_weights[layer_name]
            )
            gradients[layer_name] = weight_diff + prox_term

        return gradients

    def update_parameters(self, params: Dict):
        """更新FedProx参数"""
        if 'proximalTerm' in params:
            self.mu = params['proximalTerm']
        if 'mu' in params:
            self.mu = params['mu']

    def _train_epoch_with_prox(self, train_data) -> Tuple[float, float]:
        """带近似项的epoch训练"""
        # 实现FedProx特有的训练逻辑
        # 包含对全局模型的近似约束
        pass
```

### 3.2 动态算法加载

#### 算法工厂
```python
class AlgorithmFactory:
    """算法工厂 - 动态创建训练器"""

    _algorithms = {
        'FEDERATED_AVERAGING': FedAvgTrainer,
        'FEDERATED_PROXIMAL': FedProxTrainer,
        'FEDERATED_NOVA': FedNovaTrainer,
        'SCAFFOLD': ScaffoldTrainer,
    }

    @classmethod
    def create_trainer(cls, algorithm: str, config: Dict) -> FederatedTrainer:
        """创建训练器实例"""
        if algorithm not in cls._algorithms:
            raise ValueError(f"Unsupported algorithm: {algorithm}")

        trainer_class = cls._algorithms[algorithm]

        # 提取算法特定配置
        trainer_config = {
            'learning_rate': config.get('learningRate', 0.01),
            'local_epochs': config.get('localEpochs', 5),
            'batch_size': config.get('batchSize', 32),
        }

        # 添加算法特定参数
        algorithm_params = config.get('algorithmParams', {})
        trainer_config.update(algorithm_params)

        return trainer_class(**trainer_config)

    @classmethod
    def register_algorithm(cls, name: str, trainer_class):
        """注册新算法"""
        cls._algorithms[name] = trainer_class

    @classmethod
    def get_supported_algorithms(cls) -> List[str]:
        """获取支持的算法列表"""
        return list(cls._algorithms.keys())
```

## 4. 数据管理和预处理

### 4.1 任务级数据隔离

#### 数据管理器
```python
class TaskDataManager:
    """任务级数据管理器"""

    def __init__(self, task_id: str, data_config: Dict):
        self.task_id = task_id
        self.data_config = data_config
        self.training_data = None
        self.test_data = None
        self.validation_data = None

    def load_task_data(self):
        """加载任务专用数据"""
        dataset_id = self.data_config.get('datasetId')
        data_type = self.data_config.get('type', 'ACOUSTIC')

        # 从数据库加载数据
        raw_data = self._load_from_database(dataset_id, data_type)

        # 数据预处理
        processed_data = self._preprocess_data(raw_data)

        # 数据分割
        self._split_data(processed_data)

    def _load_from_database(self, dataset_id: str, data_type: str):
        """从数据库加载原始数据"""
        from feduwacomm.database import DatabaseManager

        db = DatabaseManager()

        # 根据数据类型选择加载策略
        if data_type == 'ACOUSTIC':
            return db.load_acoustic_data(dataset_id, self.task_id)
        elif data_type == 'ENVIRONMENT':
            return db.load_environment_data(dataset_id, self.task_id)
        elif data_type == 'MODEL':
            return db.load_model_data(dataset_id, self.task_id)
        else:
            return db.load_generic_data(dataset_id, self.task_id)

    def _preprocess_data(self, raw_data):
        """数据预处理"""
        preprocessing_config = self.data_config.get('preprocessing', {})

        # 特征提取
        if preprocessing_config.get('featureExtraction', {}).get('enabled'):
            raw_data = self._extract_features(raw_data, preprocessing_config['featureExtraction'])

        # 数据标准化
        if preprocessing_config.get('normalization', {}).get('enabled'):
            raw_data = self._normalize_data(raw_data, preprocessing_config['normalization'])

        # 数据增强
        if preprocessing_config.get('augmentation', {}).get('enabled'):
            raw_data = self._augment_data(raw_data, preprocessing_config['augmentation'])

        return raw_data

    def _split_data(self, data):
        """数据分割"""
        split_config = self.data_config.get('dataSplit', {
            'train': 0.7,
            'test': 0.2,
            'validation': 0.1
        })

        total_samples = len(data)
        train_size = int(total_samples * split_config['train'])
        test_size = int(total_samples * split_config['test'])

        # 随机分割
        import random
        random.shuffle(data)

        self.training_data = data[:train_size]
        self.test_data = data[train_size:train_size + test_size]
        self.validation_data = data[train_size + test_size:]

    def get_data_statistics(self) -> Dict:
        """获取数据统计信息"""
        return {
            'task_id': self.task_id,
            'total_samples': len(self.training_data) + len(self.test_data) + len(self.validation_data),
            'training_samples': len(self.training_data) if self.training_data else 0,
            'test_samples': len(self.test_data) if self.test_data else 0,
            'validation_samples': len(self.validation_data) if self.validation_data else 0,
            'data_type': self.data_config.get('type', 'UNKNOWN'),
            'features_count': self._get_features_count(),
            'classes_count': self._get_classes_count()
        }
```

### 4.2 特征提取适配

#### 动态特征提取器
```python
class FeatureExtractorAdapter:
    """特征提取适配器"""

    def __init__(self, config: Dict):
        self.config = config
        self.extractor = self._create_extractor()

    def _create_extractor(self):
        """根据配置创建特征提取器"""
        extractor_type = self.config.get('type', 'acoustic')

        if extractor_type == 'acoustic':
            from feduwacomm.ml.feature_extraction import AcousticFeatureExtractor
            return AcousticFeatureExtractor(self.config)
        elif extractor_type == 'environment':
            from feduwacomm.ml.feature_extraction import EnvironmentFeatureExtractor
            return EnvironmentFeatureExtractor(self.config)
        else:
            raise ValueError(f"Unsupported feature extractor: {extractor_type}")

    def extract_features(self, raw_data):
        """提取特征"""
        return self.extractor.extract(raw_data)

    def get_feature_dimensions(self) -> int:
        """获取特征维度"""
        return self.extractor.get_dimensions()
```

## 5. 性能优化和资源管理

### 5.1 内存管理

#### 内存优化策略
```python
class MemoryManager:
    """内存管理器"""

    def __init__(self, max_memory_mb: int = 1024):
        self.max_memory_mb = max_memory_mb
        self.current_usage = 0
        self.task_memory = {}  # task_id -> memory_usage

    def allocate_for_task(self, task_id: str, estimated_mb: int) -> bool:
        """为任务分配内存"""
        if self.current_usage + estimated_mb > self.max_memory_mb:
            # 尝试清理其他任务的内存
            if not self._cleanup_idle_tasks(estimated_mb):
                return False

        self.task_memory[task_id] = estimated_mb
        self.current_usage += estimated_mb
        return True

    def release_task_memory(self, task_id: str):
        """释放任务内存"""
        if task_id in self.task_memory:
            released = self.task_memory.pop(task_id)
            self.current_usage -= released

    def _cleanup_idle_tasks(self, needed_mb: int) -> bool:
        """清理空闲任务内存"""
        # 实现内存清理逻辑
        pass

    def get_memory_stats(self) -> Dict:
        """获取内存统计"""
        return {
            'total_mb': self.max_memory_mb,
            'used_mb': self.current_usage,
            'free_mb': self.max_memory_mb - self.current_usage,
            'usage_percentage': (self.current_usage / self.max_memory_mb) * 100,
            'tasks_memory': self.task_memory.copy()
        }
```

### 5.2 并发控制

#### 并发任务管理
```python
import threading
from concurrent.futures import ThreadPoolExecutor
from typing import Dict, Optional

class ConcurrentTaskManager:
    """并发任务管理器"""

    def __init__(self, max_concurrent_tasks: int = 3):
        self.max_concurrent_tasks = max_concurrent_tasks
        self.active_tasks = {}  # task_id -> threading.Thread
        self.task_executors = {}  # task_id -> FederatedTrainingExecutor
        self.executor_pool = ThreadPoolExecutor(max_workers=max_concurrent_tasks)
        self.lock = threading.Lock()

    def start_task(self, task_id: str, config: Dict) -> bool:
        """启动新任务"""
        with self.lock:
            if len(self.active_tasks) >= self.max_concurrent_tasks:
                logger.warning(f"Maximum concurrent tasks reached: {self.max_concurrent_tasks}")
                return False

            if task_id in self.active_tasks:
                logger.warning(f"Task {task_id} is already running")
                return False

            # 创建任务执行器
            executor = FederatedTrainingExecutor(task_id, config)
            executor.initialize()

            # 启动任务线程
            future = self.executor_pool.submit(self._run_task, task_id, executor)
            self.active_tasks[task_id] = future
            self.task_executors[task_id] = executor

            logger.info(f"Task {task_id} started successfully")
            return True

    def stop_task(self, task_id: str) -> bool:
        """停止任务"""
        with self.lock:
            if task_id not in self.active_tasks:
                return False

            # 取消任务
            future = self.active_tasks[task_id]
            future.cancel()

            # 清理资源
            self._cleanup_task(task_id)

            logger.info(f"Task {task_id} stopped")
            return True

    def get_task_executor(self, task_id: str) -> Optional[FederatedTrainingExecutor]:
        """获取任务执行器"""
        return self.task_executors.get(task_id)

    def _run_task(self, task_id: str, executor: FederatedTrainingExecutor):
        """运行任务主循环"""
        try:
            # 任务保持活跃状态，等待训练指令
            while task_id in self.active_tasks:
                time.sleep(1)  # 等待指令

        except Exception as e:
            logger.error(f"Task {task_id} execution failed: {e}")
        finally:
            self._cleanup_task(task_id)

    def _cleanup_task(self, task_id: str):
        """清理任务资源"""
        if task_id in self.active_tasks:
            del self.active_tasks[task_id]
        if task_id in self.task_executors:
            del self.task_executors[task_id]

    def get_active_tasks(self) -> List[str]:
        """获取活跃任务列表"""
        with self.lock:
            return list(self.active_tasks.keys())

    def get_task_count(self) -> int:
        """获取活跃任务数量"""
        return len(self.active_tasks)
```

### 5.3 资源监控

#### 系统资源监控
```python
import psutil
import threading
import time

class ResourceMonitor:
    """系统资源监控器"""

    def __init__(self, monitoring_interval: int = 30):
        self.monitoring_interval = monitoring_interval
        self.monitoring = False
        self.monitor_thread = None
        self.resource_history = []

    def start_monitoring(self):
        """开始监控"""
        if self.monitoring:
            return

        self.monitoring = True
        self.monitor_thread = threading.Thread(target=self._monitor_loop)
        self.monitor_thread.daemon = True
        self.monitor_thread.start()

    def stop_monitoring(self):
        """停止监控"""
        self.monitoring = False
        if self.monitor_thread:
            self.monitor_thread.join()

    def _monitor_loop(self):
        """监控主循环"""
        while self.monitoring:
            stats = self._collect_stats()
            self.resource_history.append(stats)

            # 保持历史记录在合理范围内
            if len(self.resource_history) > 1000:
                self.resource_history = self.resource_history[-500:]

            time.sleep(self.monitoring_interval)

    def _collect_stats(self) -> Dict:
        """收集系统统计信息"""
        return {
            'timestamp': time.time(),
            'cpu_percent': psutil.cpu_percent(interval=1),
            'memory_percent': psutil.virtual_memory().percent,
            'memory_used_mb': psutil.virtual_memory().used / (1024 * 1024),
            'memory_available_mb': psutil.virtual_memory().available / (1024 * 1024),
            'disk_usage_percent': psutil.disk_usage('/').percent,
            'network_sent_mb': psutil.net_io_counters().bytes_sent / (1024 * 1024),
            'network_recv_mb': psutil.net_io_counters().bytes_recv / (1024 * 1024)
        }

    def get_current_stats(self) -> Dict:
        """获取当前统计信息"""
        return self._collect_stats()

    def get_average_stats(self, minutes: int = 10) -> Dict:
        """获取平均统计信息"""
        if not self.resource_history:
            return self.get_current_stats()

        # 计算指定时间内的平均值
        cutoff_time = time.time() - (minutes * 60)
        recent_stats = [s for s in self.resource_history if s['timestamp'] > cutoff_time]

        if not recent_stats:
            return self.get_current_stats()

        return self._calculate_averages(recent_stats)

    def _calculate_averages(self, stats_list: List[Dict]) -> Dict:
        """计算平均值"""
        if not stats_list:
            return {}

        avg_stats = {}
        numeric_keys = ['cpu_percent', 'memory_percent', 'memory_used_mb',
                       'memory_available_mb', 'disk_usage_percent']

        for key in numeric_keys:
            values = [s[key] for s in stats_list if key in s]
            avg_stats[key] = sum(values) / len(values) if values else 0

        avg_stats['timestamp'] = time.time()
        avg_stats['sample_count'] = len(stats_list)

        return avg_stats
```

## 6. 集成测试和验证

### 6.1 单元测试

#### 训练执行器测试
```python
import unittest
from unittest.mock import Mock, patch
import tempfile
import os

class TestFederatedTrainingExecutor(unittest.TestCase):

    def setUp(self):
        self.task_id = "test_task_001"
        self.config = {
            'algorithm': 'FEDERATED_AVERAGING',
            'localEpochs': 3,
            'batchSize': 16,
            'learningRate': 0.01,
            'dataConfig': {
                'type': 'ACOUSTIC',
                'datasetId': 'test_dataset'
            }
        }

    def test_executor_initialization(self):
        """测试执行器初始化"""
        executor = FederatedTrainingExecutor(self.task_id, self.config)

        self.assertEqual(executor.task_id, self.task_id)
        self.assertEqual(executor.algorithm, 'FEDERATED_AVERAGING')
        self.assertEqual(executor.local_epochs, 3)

    @patch('feduwacomm.database.DatabaseManager')
    def test_data_loading(self, mock_db_manager):
        """测试数据加载"""
        # 模拟数据库返回
        mock_db = Mock()
        mock_db.get_training_data_by_task.return_value = [
            {'features': [1, 2, 3], 'label': 0},
            {'features': [4, 5, 6], 'label': 1}
        ]
        mock_db_manager.return_value = mock_db

        executor = FederatedTrainingExecutor(self.task_id, self.config)
        executor.initialize()

        self.assertIsNotNone(executor.training_data)
        mock_db.get_training_data_by_task.assert_called_once()

    def test_algorithm_creation(self):
        """测试算法创建"""
        executor = FederatedTrainingExecutor(self.task_id, self.config)
        executor._create_trainer()

        self.assertIsNotNone(executor.trainer)
        self.assertEqual(executor.trainer.__class__.__name__, 'FedAvgTrainer')

    def test_round_execution(self):
        """测试轮次执行"""
        executor = FederatedTrainingExecutor(self.task_id, self.config)

        # 模拟数据和模型
        executor.training_data = [{'features': [1, 2, 3], 'label': 0}]
        executor.test_data = [{'features': [4, 5, 6], 'label': 1}]
        executor.trainer = Mock()
        executor.evaluator = Mock()

        # 模拟训练返回
        executor.trainer.train.return_value = {'loss': [0.5], 'accuracy': [0.8]}
        executor.trainer.get_gradients.return_value = {'layer1': [0.1, 0.2]}
        executor.evaluator.evaluate.return_value = {'accuracy': 0.85, 'loss': 0.4}

        global_model_data = b'mock_model_data'
        round_params = {'learningRate': 0.02}

        result = executor.execute_round(global_model_data, round_params)

        self.assertIn('gradients', result)
        self.assertIn('evaluation', result)
        self.assertIn('training_stats', result)
```

### 6.2 集成测试

#### 完整流程测试
```python
class TestFederatedLearningIntegration(unittest.TestCase):

    def setUp(self):
        # 创建测试数据库
        self.test_db = self._create_test_database()
        self.client = FederatedLearningClient(
            server_url="ws://localhost:8080/websocket",
            access_token="test_token",
            vm_id="test_vm_001"
        )

    def test_complete_federated_task_flow(self):
        """测试完整联邦学习任务流程"""
        # 1. 连接到服务器
        self.client.connect()
        self.assertTrue(self.client.is_connected())

        # 2. 接收任务启动消息
        task_config = {
            'taskId': 'test_task_001',
            'algorithm': 'FEDERATED_AVERAGING',
            'localEpochs': 2,
            'totalRounds': 3
        }

        # 模拟接收 FEDERATED_TASK_START 消息
        task_start_msg = {
            'type': 'FEDERATED_TASK_START',
            'data': task_config
        }

        self.client._handle_message(task_start_msg)

        # 验证任务上下文创建
        self.assertIn('test_task_001', self.client.active_tasks)
        task_context = self.client.active_tasks['test_task_001']
        self.assertEqual(task_context.status, 'READY')

        # 3. 执行多轮训练
        for round_num in range(3):
            # 模拟接收轮次开始消息
            round_start_msg = {
                'type': 'ROUND_START',
                'data': {
                    'taskId': 'test_task_001',
                    'roundNumber': round_num + 1,
                    'globalModel': b'mock_global_model',
                    'parameters': {'learningRate': 0.01}
                }
            }

            self.client._handle_message(round_start_msg)

            # 等待训练完成
            time.sleep(2)

            # 验证轮次完成
            self.assertEqual(task_context.current_round, round_num + 1)

        # 4. 任务完成
        task_complete_msg = {
            'type': 'FEDERATED_TASK_COMPLETE',
            'data': {'taskId': 'test_task_001'}
        }

        self.client._handle_message(task_complete_msg)

        # 验证任务清理
        self.assertNotIn('test_task_001', self.client.active_tasks)

    def _create_test_database(self):
        """创建测试数据库"""
        # 创建临时数据库用于测试
        pass
```

## 7. 部署集成配置

### 7.1 配置文件模板

#### 联邦学习配置
```yaml
# federated_learning_config.yml
federated_learning:
  # 支持的算法配置
  algorithms:
    FEDERATED_AVERAGING:
      default_params:
        learning_rate: 0.01
        local_epochs: 5
        batch_size: 32

    FEDERATED_PROXIMAL:
      default_params:
        learning_rate: 0.01
        local_epochs: 5
        batch_size: 32
        proximal_term: 0.01

    FEDERATED_NOVA:
      default_params:
        learning_rate: 0.01
        local_epochs: 5
        batch_size: 32

    SCAFFOLD:
      default_params:
        learning_rate: 0.01
        local_epochs: 5
        batch_size: 32

  # 数据配置
  data:
    supported_types:
      - ACOUSTIC
      - ENVIRONMENT
      - MODEL
      - OTHER

    preprocessing:
      feature_extraction:
        enabled: true
        cache_features: true
      normalization:
        enabled: true
        method: "standard"
      augmentation:
        enabled: false

    split_ratios:
      train: 0.7
      test: 0.2
      validation: 0.1

  # 性能配置
  performance:
    max_concurrent_tasks: 3
    max_memory_mb: 2048
    gradient_compression:
      enabled: true
      type: "quantization"
      bits: 8
    model_compression:
      enabled: true

  # 监控配置
  monitoring:
    resource_monitoring:
      enabled: true
      interval_seconds: 30
    performance_logging:
      enabled: true
      log_level: "INFO"
```

### 7.2 启动脚本

#### 集成启动脚本
```python
#!/usr/bin/env python3
"""
联邦学习VM启动脚本
"""

import yaml
import logging
import argparse
import signal
import sys
from pathlib import Path

from feduwacomm.websocket.client import FederatedLearningClient
from feduwacomm.utils.resource_monitor import ResourceMonitor
from feduwacomm.utils.memory_manager import MemoryManager

def load_config(config_path: str) -> dict:
    """加载配置文件"""
    with open(config_path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)

def setup_logging(config: dict):
    """设置日志"""
    log_config = config.get('monitoring', {}).get('performance_logging', {})
    log_level = log_config.get('log_level', 'INFO')

    logging.basicConfig(
        level=getattr(logging, log_level),
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler('federated_learning.log'),
            logging.StreamHandler(sys.stdout)
        ]
    )

def main():
    parser = argparse.ArgumentParser(description='Federated Learning VM Client')
    parser.add_argument('--config', '-c', required=True, help='Configuration file path')
    parser.add_argument('--server-url', '-s', help='WebSocket server URL')
    parser.add_argument('--token', '-t', help='Access token')
    parser.add_argument('--vm-id', '-v', help='VM ID')

    args = parser.parse_args()

    # 加载配置
    config = load_config(args.config)
    setup_logging(config)

    logger = logging.getLogger(__name__)
    logger.info("Starting Federated Learning VM Client")

    # 创建资源管理器
    perf_config = config.get('federated_learning', {}).get('performance', {})
    memory_manager = MemoryManager(perf_config.get('max_memory_mb', 2048))

    # 创建资源监控器
    monitor_config = config.get('federated_learning', {}).get('monitoring', {})
    if monitor_config.get('resource_monitoring', {}).get('enabled', True):
        resource_monitor = ResourceMonitor(
            monitor_config.get('resource_monitoring', {}).get('interval_seconds', 30)
        )
        resource_monitor.start_monitoring()
    else:
        resource_monitor = None

    # 创建客户端
    server_url = args.server_url or "ws://localhost:8080/websocket"
    access_token = args.token or "default_token"
    vm_id = args.vm_id or "vm_001"

    client = FederatedLearningClient(
        server_url=server_url,
        access_token=access_token,
        vm_id=vm_id,
        config=config
    )

    # 设置信号处理
    def signal_handler(signum, frame):
        logger.info("Received shutdown signal")
        client.disconnect()
        if resource_monitor:
            resource_monitor.stop_monitoring()
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    try:
        # 连接并运行
        client.connect()
        client.run()

    except Exception as e:
        logger.error(f"Client error: {e}")
        return 1

    return 0

if __name__ == "__main__":
    sys.exit(main())
```

---

**下一步**: 继续阅读 [测试和调试指导](./06-testing-debugging.md) 了解完整的测试策略和调试方法。