# 联邦学习框架使用指南

## 📋 概述

本模块实现了完整的联邦学习框架，支持FedAvg和FedProx算法，兼容PyTorch和Scikit-learn模型。

## 🚀 主要功能

### ✅ 已实现功能
- **联邦学习算法**：FedAvg、FedProx
- **模型支持**：PyTorch神经网络、Scikit-learn模型
- **参数聚合**：加权平均、正则化聚合
- **训练状态管理**：进度跟踪、错误处理
- **检查点机制**：模型保存/恢复
- **早停机制**：自动收敛检测

### 🔧 核心组件

1. **FederatedLearningClient** - 联邦学习客户端
2. **FederatedLearningCoordinator** - 联邦学习协调器
3. **FederatedAggregator** - 模型参数聚合器
4. **ModelWrapper** - 统一的模型包装器

## 📖 使用示例

### 基础用法

```python
from feduwacomm.ml import (
    FederatedLearningClient, 
    FederatedLearningCoordinator,
    FederatedConfig, 
    FederatedAlgorithm,
    VMClient
)

# 1. 创建联邦学习配置
config = FederatedConfig(
    algorithm=FederatedAlgorithm.FEDAVG,
    local_epochs=5,
    local_batch_size=32,
    learning_rate=0.01,
    max_rounds=100
)

# 2. 创建客户端（以PyTorch模型为例）
import torch.nn as nn

model = nn.Sequential(
    nn.Linear(20, 64),
    nn.ReLU(),
    nn.Linear(64, 1)
)

client = FederatedLearningClient(
    client_id="client_001",
    model=model,
    model_type="pytorch",
    config=config
)

# 3. 加载本地数据
client.load_local_data(X_train, y_train)

# 4. 创建协调器并注册客户端
coordinator = FederatedLearningCoordinator(config)
coordinator.register_client(client)

# 5. 运行联邦学习
result = coordinator.run_federated_training()
```

### Scikit-learn模型示例

```python
from sklearn.linear_model import LinearRegression

# 创建sklearn模型
model = LinearRegression()

client = FederatedLearningClient(
    client_id="sklearn_client",
    model=model,
    model_type="sklearn",
    config=config
)
```

## ⚙️ 配置参数

### FederatedConfig 参数说明

- `algorithm`: 联邦学习算法 (FEDAVG, FEDPROX)
- `local_epochs`: 本地训练轮数
- `local_batch_size`: 本地批量大小
- `learning_rate`: 学习率
- `mu`: FedProx正则化参数
- `client_fraction`: 每轮参与的客户端比例
- `max_rounds`: 最大联邦学习轮数
- `patience`: 早停耐心值
- `min_delta`: 最小改进阈值

## 📊 训练状态监控

```python
# 获取训练状态
status = client.get_training_status()
print(f"训练轮次: {status['round_num']}")
print(f"是否正在训练: {status['is_training']}")
print(f"样本数量: {status['total_samples']}")

# 保存/加载检查点
client.save_checkpoint("checkpoints/client_001.pt")
client.load_checkpoint("checkpoints/client_001.pt")
```

## 🔄 与现有模块集成

### 与特征提取器集成

```python
from feduwacomm.ml.feature_extractor import BellhopFeatureExtractor

# 提取BELLHOP特征
extractor = BellhopFeatureExtractor()
features_df = extractor.batch_extract_features()

# 准备联邦学习数据
X, y = prepare_data(features_df, target_cols=['transmission_loss'])

# 创建联邦学习客户端
client.load_local_data(X, y)
```

### 与模型评估器集成

```python
from feduwacomm.ml.model_evaluator import ModelEvaluator

# 训练完成后评估
evaluator = ModelEvaluator()
metrics = evaluator.evaluate_regressor(model, X_test, y_test)
```

## 🚨 错误处理

框架包含完整的错误处理机制：

```python
try:
    result = coordinator.run_federated_training()
except Exception as e:
    print(f"联邦学习训练失败: {e}")
    # 检查客户端状态
    for client in coordinator.participating_clients.values():
        status = client.get_training_status()
        if status['error_message']:
            print(f"客户端 {client.client_id} 错误: {status['error_message']}")
```

## 📈 性能优化建议

1. **数据预处理**：确保数据已标准化
2. **批量大小**：根据内存调整batch_size
3. **学习率**：从0.01开始调优
4. **轮次设置**：设置合理的max_rounds和patience
5. **客户端选择**：调整client_fraction以平衡效率和准确性

## 🔮 下一步开发

- WebSocket通信集成
- 更多联邦学习算法（FedNova、SCAFFOLD）
- 分布式训练优化
- 高级隐私保护机制
- 可视化训练监控界面

## 🐛 故障排除

### 常见问题

1. **导入错误**：确保安装了PyTorch或scikit-learn
2. **内存不足**：减小batch_size或模型大小
3. **收敛缓慢**：调整学习率或增加local_epochs
4. **客户端失败**：检查数据格式和模型兼容性

### 调试模式

```python
import logging
logging.basicConfig(level=logging.DEBUG)

# 启用详细日志
client.logger.setLevel(logging.DEBUG)
coordinator.logger.setLevel(logging.DEBUG)
```
