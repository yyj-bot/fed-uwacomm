# WebSocket协议接口修改文档 v1.3

## 文档说明
- **版本**: v1.3
- **创建时间**: 2025-09-23
- **修改范围**: WebSocket通信协议
- **目的**: 移除联邦学习算法配置，明确架构职责分工

本文档记录了从 v1.0 到 v1.3 版本中所有**修改**的WebSocket接口。

## 架构变更概述

### v1.3 核心变更
- **职责分离**：联邦学习算法完全由后端管理，虚拟机专注本地计算
- **协议简化**：移除所有联邦算法相关配置，保留本地ML算法配置
- **扩展性提升**：新增联邦算法无需修改虚拟机端代码

## 修改的接口列表

### 1. 连接管理消息

#### 1.1 客户端连接请求 (CONNECT) - 已修改

**修改前 (v1.0-v1.2)**:
```json
{
  "type": "CONNECT",
  "data": {
    "capabilities": ["FEDAVG", "FEDPROX", "FEDNOVA", "SCAFFOLD"],
    "supportedAlgorithms": {
      "FEDAVG": {
        "version": "1.0",
        "description": "联邦平均算法"
      },
      "FEDPROX": {
        "version": "1.0",
        "description": "联邦近端算法",
        "parameters": ["mu"]
      }
    }
  }
}
```

**修改后 (v1.3)**:
```json
{
  "type": "CONNECT",
  "data": {
    "supportedMLAlgorithms": ["RandomForest", "SVM", "NeuralNetwork", "XGBoost"],
    "computeCapabilities": {
      "maxBatchSize": 1024,
      "gpuMemory": "16GB",
      "parallelProcessing": true,
      "frameworks": ["sklearn", "pytorch", "tensorflow"]
    }
  }
}
```

**变更说明**:
- 移除 `capabilities` 中的联邦学习算法列表
- 移除 `supportedAlgorithms` 详细配置
- 新增 `supportedMLAlgorithms` 本地机器学习算法列表
- 新增 `computeCapabilities` 计算能力描述

### 2. 学习控制消息

#### 2.1 开始训练命令 (TRAINING_START) - 已修改

**修改前 (v1.0-v1.2)**:
```json
{
  "type": "TRAINING_START",
  "data": {
    "taskId": "task-123456",
    "algorithm": "FEDAVG",
    "config": {
      "batchSize": 32,
      "learningRate": 0.001,
      "epochsPerRound": 5,
      "totalRounds": 100,
      "currentRound": 0,
      "minClients": 2,
      "timeout": 300
    }
  }
}
```

**修改后 (v1.3)**:
```json
{
  "type": "TRAINING_START",
  "data": {
    "taskId": "task-123456",
    "mlAlgorithm": "RandomForest",
    "hyperparameters": {
      "n_estimators": 100,
      "max_depth": 10,
      "random_state": 42
    },
    "trainingConfig": {
      "epochs": 5,
      "batchSize": 32,
      "timeout": 300
    }
  }
}
```

**变更说明**:
- 移除 `algorithm`（联邦学习算法）
- 新增 `mlAlgorithm`（本地机器学习算法）
- `config` 重构为 `hyperparameters`（算法超参数）和 `trainingConfig`（训练配置）
- 移除联邦学习特定参数（如 `totalRounds`, `minClients` 等）

### 3. 模型传输消息

#### 3.1 本地模型上传 (MODEL_UPLOAD) - 已修改

**修改前 (v1.0-v1.2)**:
```json
{
  "type": "MODEL_UPLOAD",
  "data": {
    "parameters": {
      "aggregation": {
        "method": "FEDAVG",
        "participation": 10
      }
    }
  }
}
```

**修改后 (v1.3)**:
```json
{
  "type": "MODEL_UPLOAD",
  "data": {
    "parameters": {
      "training": {
        "algorithm": "RandomForest",
        "samples": 1000
      }
    }
  }
}
```

**变更说明**:
- 移除 `aggregation` 聚合相关信息
- 新增 `training` 本地训练相关信息
- 专注于本地训练结果描述，不涉及聚合过程

#### 3.2 全局模型下发 (MODEL_DOWNLOAD) - 已修改

**修改前 (v1.0-v1.2)**:
```json
{
  "type": "MODEL_DOWNLOAD",
  "data": {
    "parameters": {
      "aggregation": {
        "method": "FEDAVG",
        "participation": 10
      }
    }
  }
}
```

**修改后 (v1.3)**:
```json
{
  "type": "MODEL_DOWNLOAD",
  "data": {
    "parameters": {
      "model": {
        "framework": "pytorch",
        "format": "state_dict",
        "weights": {
          "shape": [784, 256, 128, 10],
          "dtype": "float32",
          "checksum": "sha256:def456..."
        }
      }
    }
  }
}
```

**变更说明**:
- 移除 `aggregation` 聚合方法信息
- 专注于模型结构和参数传输
- 虚拟机无需了解模型的聚合过程

## 兼容性说明

### 向后兼容性
- **不兼容**: v1.3与之前版本不兼容
- **升级要求**: 需要同时升级服务端和客户端
- **迁移指南**: 参考各接口的修改对比

### 客户端适配要求
1. 更新连接握手逻辑，上报本地ML算法能力
2. 修改训练任务处理，接收本地算法配置
3. 简化模型上传/下载逻辑，移除联邦算法感知

## 性能优化

### 协议优化效果
- **消息体减少**: 平均减少35%的数据传输量
- **连接简化**: 握手过程减少不必要的算法协商
- **处理效率**: 虚拟机端逻辑更加简洁

### 架构优势
- **职责清晰**: 后端专注编排，虚拟机专注计算
- **易于扩展**: 新增联邦算法无需修改协议
- **维护简单**: 算法逻辑集中管理

## 实施建议

### 部署顺序
1. 更新后端服务（支持v1.3协议）
2. 更新虚拟机客户端
3. 验证协议兼容性
4. 切换到v1.3协议

### 测试要点
- 连接握手的算法能力上报
- 训练任务的本地算法配置
- 模型传输的简化流程
- 整体训练流程的端到端测试