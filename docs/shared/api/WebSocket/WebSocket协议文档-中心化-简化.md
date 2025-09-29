# 水声联邦学习系统 WebSocket 通信协议 - 中心化简化版 v1.5

## 0. 概述

本文档定义了水声联邦学习系统的简化WebSocket通信协议v1.5版本，专注于联邦学习的核心流程，并新增完整的数据集关联和分发机制。

### v1.5版本主要新增特性
- **数据集预查询机制**: 任务创建前主动查询虚拟机可用数据集
- **完整数据集关联流程**: 建立前端-后端-虚拟机的完整数据集分发链路
- **统一ID管理**: 所有数据集ID由后端UuidUtil统一生成，确保ID一致性
- **智能数据集分配**: 后端实现数据集分配算法，支持多种分配策略

### 0.1 设计原则
- **完整性**: 36个核心协议覆盖完整的联邦学习生态系统，包含数据集预查询机制
- **实时性**: 通过WebSocket直接分发模型和数据，确保同步
- **分层清晰**: 连接层、任务层、轮次层、监控层、控制层、数据层职责明确
- **原子性**: 任务配置和初始模型分发在同一消息中完成
- **集中控制**: 后端完全控制虚拟机生命周期和数据分发
- **多任务并发**: 通过taskId字段实现精确的任务级别控制，支持单VM运行多任务
- **协议简洁**: 移除冗余功能，统一状态监控机制
- **完整生命周期**: 支持任务的创建、执行、停止、删除完整生命周期管理
- **数据集关联**: 建立完整的前端-后端-虚拟机数据集关联机制，支持智能分配
- **ID统一管理**: 所有关键ID由后端UuidUtil生成，确保全局唯一性和一致性

### 0.2 基础信息
- **WebSocket URL**: `ws://localhost:8080/ws` (开发环境)
- **WebSocket Secure URL**: `wss://your-domain.com/ws` (生产环境)
- **协议版本**: v1.5
- **认证方式**: JWT Token（必需）
- **数据格式**: JSON
- **编码**: UTF-8

### 0.3 协议架构

```
连接管理层: CONNECT, HEARTBEAT (4个协议)
     ↓
数据集预查询层: DATASET_LIST_QUERY, DATASET_LIST_RESPONSE (2个协议) 🆕 v1.5新增
     ↓
任务管理层: FEDERATED_TASK_START/STOP/RESUME/DELETE + FEDERATED_TASK_STATUS_QUERY (10个协议)
     ↓
轮次管理层: ROUND_START/ABORT → GRADIENT_UPLOAD → GLOBAL_MODEL_BROADCAST → ROUND_COMPLETE (9个协议)
     ↓
状态监控层: VM_STATUS_QUERY, ERROR (3个协议)
     ↓
虚拟机控制层: VM_START, VM_STOP (4个协议)
     ↓
数据集管理层: DATASET_CREATE, DATASET_APPEND_ROWS, DATASET_COMPLETE, DATASET_STATUS_QUERY, DATASET_DELETE (6个协议)
```

### 任务生命周期管理
```
创建阶段: FEDERATED_TASK_START → 任务初始化和模型分发
执行阶段: ROUND_* 协议 → 训练轮次管理
监控阶段: FEDERATED_TASK_STATUS_QUERY → 实时状态监控
停止阶段: FEDERATED_TASK_STOP → 停止执行，保留数据
恢复阶段: FEDERATED_TASK_RESUME → 从停止状态恢复执行
删除阶段: FEDERATED_TASK_DELETE → 彻底清理，释放资源
```

**协议分层说明**:
- **任务管理层**: 包含FEDERATED_TASK_STATUS_QUERY/RESPONSE，提供任务级别的精确状态监控
- **状态监控层**: VM_STATUS_QUERY/RESPONSE提供虚拟机整体状态，包含所有任务概览
- **统一状态机制**: 消除了冗余的STATUS_QUERY协议，通过两层监控满足不同粒度需求

### 任务状态转换逻辑

任务状态转换遵循严格的状态机模式，确保状态变更的一致性和可预测性：

```
任务状态转换图:

    [创建]
       ↓ START
    [RUNNING] ←←←← RESUME ←←←← [STOPPED]
       ↓                        ↑
       ↓ STOP →→→→→→→→→→→→→→→→→→→→→→→↑
       ↓
       ↓ DELETE
    [DELETED]

状态说明:
- CREATING: 任务正在创建和初始化
- RUNNING: 任务正在执行训练轮次
- STOPPED: 任务已停止，保留数据和状态
- DELETED: 任务已删除，资源完全释放
```

**状态转换规则**:

1. **START转换**: `无状态 → RUNNING`
   - 条件: 任务配置有效，虚拟机资源充足
   - 操作: 初始化任务环境，分发初始模型

2. **STOP转换**: `RUNNING → STOPPED`
   - 条件: 任务正在运行
   - 操作: 保存当前进度，停止训练循环，保留数据

3. **RESUME转换**: `STOPPED → RUNNING`
   - 条件: 任务处于停止状态，检查点数据完整
   - 操作: 恢复模型状态，继续训练轮次

4. **DELETE转换**: `STOPPED → DELETED`
   - 条件: 任务已停止
   - 操作: 清理所有相关资源，释放存储空间

**状态转换限制**:
- ❌ 无法从RUNNING直接DELETE（必须先STOP）
- ❌ 无法从DELETED状态恢复
- ❌ 无法对不存在的任务执行RESUME
- ✅ 支持RUNNING ↔ STOPPED的往复转换

### 0.4 多任务并发支持

v1.4协议增强了多任务并发能力，支持一台虚拟机同时执行多个联邦学习任务：

- **任务隔离**: 每个协议消息都包含taskId字段，确保任务级别的精确控制
- **资源管理**: VM_STATUS_RESPONSE提供各任务的资源分配情况
- **数据隔离**: DATASET_*协议支持按任务ID进行数据集管理
- **状态追踪**: 支持查询单个任务或VM整体状态

**典型多任务场景**:
```
VM-001 同时执行:
├── fedtask-audio-001 (状态: TRAINING, 轮次: 5/10)
└── fedtask-video-002 (状态: PAUSED, 轮次: 2/8)
```

### 0.5 前置注册与认证流程
- 第一步（HTTP）: 虚拟机向后端发起注册请求，注册成功后返回 `accessToken`、`secretId` 和建议的 WebSocket 连接信息
- 第二步（WebSocket/STOMP）: 虚拟机使用 `accessToken` 建立 WebSocket 连接，并在 STOMP CONNECT 帧中携带 Token
- 第三步（应用层）: 连接建立后发送应用层 `CONNECT` 消息，进行能力与环境上报

## 1. 连接管理协议

### 1.1 客户端连接请求 (CONNECT) 🔵

**消息作用**: 虚拟机启动后向后端服务器发送连接请求，上报系统信息和机器学习能力。

```json
{
  "type": "CONNECT",
  "id": "client-1704067200000-123456",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "version": "2.0.0",
    "supportedMLAlgorithms": ["RandomForest", "SVM", "NeuralNetwork"],
    "systemInfo": {
      "os": "Ubuntu 20.04",
      "python": "3.8.10",
      "memory": "4GB",
      "cpu": "Intel Xeon E5-2680"
    },
    "computeCapabilities": {
      "maxBatchSize": 1024,
      "parallelProcessing": true,
      "frameworks": ["sklearn", "pytorch"]
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 1.2 服务器连接确认 (CONNECT_ACK) 🟢

**消息作用**: 后端确认虚拟机连接，提供服务器配置信息。

```json
{
  "type": "CONNECT_ACK",
  "id": "server-1704067200000-123456",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "sessionId": "session-123456",
    "serverTime": "2024-01-01T00:00:00.000Z",
    "heartbeatInterval": 30,
    "maxMessageSize": 10485760
  },
  "signature": "base64_encoded_signature"
}
```

### 1.3 客户端心跳 (HEARTBEAT) 🔵

**消息作用**: 虚拟机定期发送心跳维持连接，报告系统资源状态。

```json
{
  "type": "HEARTBEAT",
  "id": "client-1704067200000-123458",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "IDLE",
    "resourceUsage": {
      "cpu": 25.5,
      "memory": 60.2,
      "disk": 45.8
    },
    "network": {
      "latency": 50
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 1.4 服务器心跳响应 (HEARTBEAT_ACK) 🟢

**消息作用**: 后端对心跳的确认响应，提供服务器状态。

```json
{
  "type": "HEARTBEAT_ACK",
  "id": "server-1704067200000-123458",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "serverTime": "2024-01-01T00:00:00.000Z",
    "nextHeartbeat": 30,
    "systemStatus": "NORMAL"
  },
  "signature": "base64_encoded_signature"
}
```

## 2. 任务管理协议

### 2.1 联邦学习任务启动 (FEDERATED_TASK_START) 🟢

**消息作用**: 后端启动联邦学习任务，一次性分发完整的任务配置、初始模型和训练参数。

**重要改进**:
- 合并了原TRAINING_START的功能
- 通过WebSocket直接分发初始模型，确保实时性和一致性
- 包含完整的任务初始化信息
- **支持多任务并发**: 通过taskId字段实现精确的任务级别控制
- **支持多种联邦算法**: FEDERATED_AVERAGING, FEDERATED_PROXIMAL, FEDERATED_NOVA, SCAFFOLD
- **🆕 v1.5新增数据集关联**: 通过assignedDatasetId字段指定虚拟机使用的数据集

```json
{
  "type": "FEDERATED_TASK_START",
  "id": "server-1704067200000-123475",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "federatedAlgorithm": "FEDERATED_AVERAGING",
    "totalRounds": 10,
    "participants": ["vm1", "vm2", "vm3"],
    "config": {
      "aggregationMethod": "FEDERATED_AVERAGING",
      "minParticipants": 2,
      "timeout": 1800
    },
    "localTrainingConfig": {
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
    },
    "initialGlobalModel": {
      "modelId": "model-123456",
      "version": "1.0.0",
      "framework": "sklearn",
      "modelType": "RandomForest",
      "parameters": {
        "n_estimators": 100,
        "max_depth": 10,
        "feature_importances": [0.1, 0.2, 0.3],
        "tree_weights": {
          "tree_0": { /* 树结构参数 */ },
          "tree_1": { /* 树结构参数 */ }
        }
      },
      "metadata": {
        "inputDimension": 784,
        "outputClasses": 10,
        "checksum": "sha256:abc123..."
      }
    },
    "dataConfig": {
      "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：后端分配的统一数据集ID，替换原datasetId
      "dataPath": "/data/training",
      "validationSplit": 0.2,
      "shuffle": true
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 2.2 联邦学习任务启动确认 (FEDERATED_TASK_START_ACK) 🔵

**消息作用**: 虚拟机确认收到联邦学习任务配置和初始模型，表示准备就绪。

```json
{
  "type": "FEDERATED_TASK_START_ACK",
  "id": "client-1704067200000-123476",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "READY",
    "taskId": "fedtask-123456",
    "modelReceived": true,
    "configApplied": true,
    "estimatedReadyTime": "2024-01-01T00:00:30.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

### 2.3 联邦学习任务停止 (FEDERATED_TASK_STOP) 🟢

**消息作用**: 后端精确停止特定的联邦学习任务，支持多任务并发场景下的任务级别控制。

**使用场景**:
- 单独停止某个任务而保持其他任务继续运行
- 任务完成或异常时的彻底终止
- 资源优化时的选择性任务终止

```json
{
  "type": "FEDERATED_TASK_STOP",
  "id": "server-1704067200000-123481",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "reason": "MANUAL_STOP",
    "graceful": true,
    "saveProgress": true,
    "timeout": 60
  },
  "signature": "base64_encoded_signature"
}
```

### 2.4 联邦学习任务停止确认 (FEDERATED_TASK_STOP_ACK) 🔵

**消息作用**: 虚拟机确认收到任务停止命令并开始停止指定任务。

```json
{
  "type": "FEDERATED_TASK_STOP_ACK",
  "id": "client-1704067200000-123481",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "STOPPING",
    "taskId": "fedtask-123456",
    "progressSaved": true,
    "estimatedStopTime": "2024-01-01T00:01:00.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

### 2.5 联邦学习任务恢复 (FEDERATED_TASK_RESUME) 🟢

**消息作用**: 后端恢复已停止的联邦学习任务，支持从指定轮次和检查点恢复执行。

**使用场景**:
- 从停止状态恢复继续训练
- 系统维护后重启任务
- 网络中断后的任务恢复
- 资源优化后的选择性任务恢复

**与START的区别**:
- `FEDERATED_TASK_START`: 创建并启动全新的联邦学习任务
- `FEDERATED_TASK_RESUME`: 恢复已存在但处于停止状态的任务

```json
{
  "type": "FEDERATED_TASK_RESUME",
  "id": "server-1704067200000-123482",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "resumeFrom": {
      "round": 5,
      "checkpoint": "checkpoint-round-5",
      "savedAt": "2024-01-01T00:01:00.000Z"
    },
    "parameters": {
      "adjustedLearningRate": 0.001,
      "resumeTimeout": 300
    },
    "verification": {
      "expectedModelVersion": "1.5.0",
      "expectedProgress": 50.0
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 2.6 联邦学习任务恢复确认 (FEDERATED_TASK_RESUME_ACK) 🔵

**消息作用**: 虚拟机确认收到任务恢复命令并开始恢复指定任务。

```json
{
  "type": "FEDERATED_TASK_RESUME_ACK",
  "id": "client-1704067200000-123482",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "RESUMING",
    "taskId": "fedtask-123456",
    "resumeStatus": {
      "checkpointLoaded": true,
      "modelRestored": true,
      "progressVerified": true,
      "currentRound": 5
    },
    "estimatedResumeTime": "2024-01-01T00:00:30.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

### 2.7 联邦学习任务状态查询 (FEDERATED_TASK_STATUS_QUERY) 🟢

**消息作用**: 后端查询特定联邦学习任务的详细状态信息。

**核心价值**:
- **精确监控**: 获取特定任务的执行状态，而不影响其他任务
- **故障诊断**: 当任务出现问题时，获取详细的错误信息和执行上下文
- **进度跟踪**: 实时了解任务的训练进度、轮次状态、性能指标
- **资源管理**: 了解任务对系统资源的使用情况，便于负载调度
- **多任务协调**: 在多任务环境中，分别监控每个任务的独立状态

**使用场景**:
- 定期检查任务健康状态
- 任务异常时的详细诊断
- 性能监控和优化决策
- 多任务场景下的状态同步

```json
{
  "type": "FEDERATED_TASK_STATUS_QUERY",
  "id": "server-1704067200000-123484",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "includeMetrics": true,
    "includeProgress": true,
    "includeResourceUsage": true
  },
  "signature": "base64_encoded_signature"
}
```

### 2.8 联邦学习任务状态响应 (FEDERATED_TASK_STATUS_RESPONSE) 🔵

**消息作用**: 虚拟机响应联邦学习任务状态查询，提供详细的任务执行信息。

**提供的关键信息**:
- **任务执行状态**: STARTING/TRAINING/WAITING/STOPPING/ERROR等
- **训练进度**: 当前轮次、总轮次、完成百分比
- **性能指标**: 本地准确率、损失值、训练时间等
- **资源使用**: 该任务占用的CPU、内存、GPU等资源
- **预期完成时间**: 基于当前进度的时间估算

```json
{
  "type": "FEDERATED_TASK_STATUS_RESPONSE",
  "id": "client-1704067200000-123484",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "status": "TRAINING",
    "currentRound": 6,
    "totalRounds": 10,
    "progress": 60.0,
    "metrics": {
      "localAccuracy": 0.87,
      "localLoss": 0.25,
      "trainingTime": 1800,
      "samplesProcessed": 10000
    },
    "resourceUsage": {
      "cpu": 45.2,
      "memory": 2048,
      "gpu": 78.5
    },
    "estimatedCompletion": "2024-01-01T00:15:00.000Z",
    "lastActivity": "2024-01-01T00:05:30.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

### 2.9 联邦学习任务删除 (FEDERATED_TASK_DELETE) 🟢

**消息作用**: 后端删除已停止的联邦学习任务，清理任务相关的所有资源和数据。

**使用场景**:
- 任务完成后的彻底清理
- 释放任务占用的存储空间
- 清理任务相关的模型、数据集、日志等
- 多任务管理中的资源回收

**与STOP的区别**:
- `FEDERATED_TASK_STOP`: 停止任务执行，保留任务记录和数据
- `FEDERATED_TASK_DELETE`: 彻底删除任务，清理所有相关资源

```json
{
  "type": "FEDERATED_TASK_DELETE",
  "id": "server-1704067200000-123485",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "reason": "TASK_COMPLETED",
    "cleanup": {
      "models": true,
      "datasets": true,
      "logs": true,
      "cache": true
    },
    "backup": false,
    "force": false
  },
  "signature": "base64_encoded_signature"
}
```

### 2.10 联邦学习任务删除确认 (FEDERATED_TASK_DELETE_ACK) 🔵

**消息作用**: 虚拟机确认收到任务删除命令并开始清理指定任务的所有资源。

```json
{
  "type": "FEDERATED_TASK_DELETE_ACK",
  "id": "client-1704067200000-123485",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "DELETING",
    "taskId": "fedtask-123456",
    "cleanupProgress": {
      "models": "IN_PROGRESS",
      "datasets": "COMPLETED",
      "logs": "PENDING",
      "cache": "COMPLETED"
    },
    "estimatedCleanupTime": "2024-01-01T00:02:00.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

## 3. 轮次管理协议

### 3.1 轮次开始通知 (ROUND_START) 🟢

**消息作用**: 后端通知开始新的训练轮次，提供轮次特定的配置调整。

**多任务支持**: 通过taskId字段确保轮次控制的任务精确性。

```json
{
  "type": "ROUND_START",
  "id": "server-1704067200000-130040",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 6,
    "roundSpecificConfig": {
      "learningRate": 0.008,
      "timeout": 600
    },
    "targetMetrics": {
      "minAccuracy": 0.85,
      "maxLoss": 0.20
    },
    "expectedParticipants": 5
  },
  "signature": "base64_encoded_signature"
}
```

### 3.1.1 轮次中止 (ROUND_ABORT) 🟢

**消息作用**: 后端中止特定任务的当前轮次，支持多任务场景下的精确轮次控制。

**使用场景**:
- 轮次训练出现收敛问题
- 梯度异常或模型发散
- 紧急停止当前轮次

```json
{
  "type": "ROUND_ABORT",
  "id": "server-1704067200000-130043",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 6,
    "reason": "CONVERGENCE_FAILURE"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.2 轮次开始确认 (ROUND_START_ACK) 🔵

**消息作用**: 虚拟机确认开始本轮训练。

```json
{
  "type": "ROUND_START_ACK",
  "id": "client-1704067200000-130040",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "READY",
    "taskId": "fedtask-123456",
    "roundNumber": 6,
    "estimatedTrainingTime": 300
  },
  "signature": "base64_encoded_signature"
}
```

### 3.3 梯度上传 (GRADIENT_UPLOAD) 🔵

**消息作用**: 虚拟机完成本地训练后，上传梯度信息用于联邦聚合。

```json
{
  "type": "GRADIENT_UPLOAD",
  "id": "client-1704067200000-130001",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 6,
    "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：关联的数据集ID，用于验证数据溯源
    "gradientData": {
      "weights": {
        "layer1": [0.001, -0.002, 0.003],
        "layer2": [0.004, -0.005, 0.006]
      },
      "biases": {
        "layer1": [0.001],
        "layer2": [0.002]
      }
    },
    "trainingMetrics": {
      "samplesCount": 1000,
      "localLoss": 0.25,
      "localAccuracy": 0.87,
      "trainingTime": 295
    },
    "compressionMethod": "gzip"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.4 梯度上传确认 (GRADIENT_UPLOAD_ACK) 🟢

**消息作用**: 后端确认收到梯度数据。

```json
{
  "type": "GRADIENT_UPLOAD_ACK",
  "id": "server-1704067200000-130001",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "SUCCESS",
    "taskId": "fedtask-123456",
    "roundNumber": 6,
    "receivedBytes": 2048,
    "validationResult": "PASSED"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.5 全局模型广播 (GLOBAL_MODEL_BROADCAST) 🟢

**消息作用**: 后端完成联邦聚合后，向所有虚拟机广播新的全局模型。

```json
{
  "type": "GLOBAL_MODEL_BROADCAST",
  "id": "server-1704067200000-130010",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 7,
    "globalModel": {
      "modelId": "model-123456-r7",
      "version": "1.7.0",
      "parameters": {
        "weights": {
          "layer1": [0.125, -0.087, 0.234],
          "layer2": [0.156, -0.203, 0.298]
        },
        "biases": {
          "layer1": [0.045],
          "layer2": [0.078]
        }
      },
      "checksum": "sha256:def456..."
    },
    "aggregationInfo": {
      "aggregationMethod": "FEDERATED_AVERAGING",
      "participantCount": 5,
      "convergenceScore": 0.92,
      "globalAccuracy": 0.89,
      "globalLoss": 0.14
    },
    "nextRoundConfig": {
      "startTime": "2024-01-01T00:05:00.000Z",
      "learningRate": 0.007
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 3.6 全局模型广播确认 (GLOBAL_MODEL_BROADCAST_ACK) 🔵

**消息作用**: 虚拟机确认收到新的全局模型。

```json
{
  "type": "GLOBAL_MODEL_BROADCAST_ACK",
  "id": "client-1704067200000-130010",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "SUCCESS",
    "taskId": "fedtask-123456",
    "roundNumber": 7,
    "modelUpdateTime": "2024-01-01T00:00:00.000Z",
    "validationAccuracy": 0.91,
    "readyForNextRound": true
  },
  "signature": "base64_encoded_signature"
}
```

### 3.7 轮次完成通知 (ROUND_COMPLETE) 🟢

**消息作用**: 后端通知当前轮次完成，提供轮次结果。

```json
{
  "type": "ROUND_COMPLETE",
  "id": "server-1704067200000-130050",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 6,
    "roundResults": {
      "participantCount": 5,
      "globalAccuracy": 0.91,
      "globalLoss": 0.12,
      "convergenceImprovement": 0.03,
      "duration": 450
    },
    "nextRound": {
      "planned": true,
      "roundNumber": 7,
      "scheduledStart": "2024-01-01T00:10:00.000Z"
    },
    "taskStatus": "CONTINUING"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.8 轮次完成确认 (ROUND_COMPLETE_ACK) 🔵

**消息作用**: 虚拟机确认轮次完成。

```json
{
  "type": "ROUND_COMPLETE_ACK",
  "id": "client-1704067200000-130050",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "ACKNOWLEDGED",
    "taskId": "fedtask-123456",
    "roundNumber": 6,
    "resourcesCleared": true,
    "readyForNextRound": true
  },
  "signature": "base64_encoded_signature"
}
```

## 4. 状态监控协议

### 4.1 虚拟机状态查询 (VM_STATUS_QUERY) 🟢

**消息作用**: 后端向虚拟机发送状态查询请求，获取详细的运行状态、资源使用情况和系统信息。

**核心价值**:
- **虚拟机整体监控**: 获取VM的系统资源、网络状态、进程信息
- **多任务概览**: 一次查询获取VM上所有联邦学习任务的状态概览
- **负载均衡**: 为任务分配提供VM资源使用情况
- **故障诊断**: VM级别的系统健康状态检查

**使用场景**:
- 故障排查和系统诊断
- 负载均衡决策
- 任务分配前的资源评估
- 系统监控和性能分析

```json
{
  "type": "VM_STATUS_QUERY",
  "id": "server-1704067200000-123476",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "queryType": "FULL",
    "includeResources": true,
    "includeProcesses": true,
    "includeNetwork": true,
    "includeTasks": true,
    "timeout": 10
  },
  "signature": "base64_encoded_signature"
}
```

### 4.2 虚拟机状态响应 (VM_STATUS_RESPONSE) 🔵

**消息作用**: 虚拟机响应后端的状态查询请求，提供详细的系统状态信息。

**多任务支持增强**: 新增tasks字段，支持显示VM上所有任务的执行状态。

```json
{
  "type": "VM_STATUS_RESPONSE",
  "id": "client-1704067200000-123477",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "overallStatus": "BUSY",
    "uptime": 3600,
    "resourceUsage": {
      "cpu": 85.5,
      "memory": 75.2,
      "disk": 45.8,
      "gpu": 90.3
    },
    "network": {
      "ipAddress": "192.168.1.100",
      "uploadSpeed": 1024,
      "downloadSpeed": 2048,
      "latency": 50
    },
    "processes": {
      "total": 150,
      "active": 25,
      "training": 2
    },
    "tasks": [
      {
        "taskId": "fedtask-123456",
        "status": "TRAINING",
        "currentRound": 6,
        "totalRounds": 10,
        "stage": "LOCAL_TRAINING",
        "progress": 65.5,
        "estimatedCompletion": "2024-01-01T00:02:30.000Z",
        "resourceAllocation": {
          "cpu": 40.0,
          "memory": 35.0
        }
      },
      {
        "taskId": "fedtask-789012",
        "status": "PAUSED",
        "currentRound": 2,
        "totalRounds": 8,
        "stage": "WAITING",
        "progress": 25.0,
        "resourceAllocation": {
          "cpu": 0.0,
          "memory": 20.0
        }
      }
    ],
    "systemInfo": {
      "os": "Ubuntu 20.04 LTS",
      "kernel": "5.4.0-42-generic",
      "loadAverage": [1.2, 1.5, 1.8],
      "lastBoot": "2024-01-01T00:00:00.000Z"
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 4.3 错误报告 (ERROR)

**消息作用**: 报告系统错误和异常情况。

```json
{
  "type": "ERROR",
  "id": "client-1704067200000-123472",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "errorCode": "TRAINING_FAILED",
    "errorMessage": "本地训练过程中发生错误",
    "severity": "HIGH",
    "context": {
      "taskId": "fedtask-123456",
      "roundNumber": 6,
      "stage": "LOCAL_TRAINING"
    },
    "details": {
      "exception": "ValueError: Invalid input shape",
      "timestamp": "2024-01-01T00:00:00.000Z"
    },
    "recovery": {
      "automatic": false,
      "suggestions": ["检查数据格式", "验证模型输入维度"]
    }
  },
  "signature": "base64_encoded_signature"
}
```

## 5. 虚拟机控制协议

### 5.1 启动虚拟机命令 (VM_START) 🟢

**消息作用**: 后端向虚拟机发送启动命令，用于集中控制虚拟机的生命周期。

**使用场景**:
- 任务开始前批量启动虚拟机
- 故障恢复时重启特定虚拟机
- 根据负载动态扩容虚拟机
- 系统维护后重新激活虚拟机

```json
{
  "type": "VM_START",
  "id": "server-1704067200000-123459",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "timeout": 300,
    "config": {
      "memory": "4GB",
      "cpu": "4cores",
      "disk": "50GB",
      "network": "bridge"
    },
    "environment": {
      "variables": {
        "PYTHONPATH": "/app",
        "CUDA_VISIBLE_DEVICES": "0"
      }
    },
    "startupScript": "/scripts/startup.sh"
  },
  "signature": "base64_encoded_signature"
}
```

### 5.2 启动虚拟机确认 (VM_START_ACK) 🔵

**消息作用**: 虚拟机确认收到启动命令并开始启动过程。

```json
{
  "type": "VM_START_ACK",
  "id": "client-1704067200000-123459",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "STARTING",
    "estimatedStartupTime": 60,
    "configApplied": true,
    "message": "虚拟机启动中"
  },
  "signature": "base64_encoded_signature"
}
```

### 5.3 停止虚拟机命令 (VM_STOP) 🟢

**消息作用**: 后端向虚拟机发送停止命令，用于优雅关闭虚拟机。

**使用场景**:
- 任务完成后回收虚拟机资源
- 系统维护前停止虚拟机
- 异常情况下强制停止虚拟机
- 成本优化时缩减虚拟机规模

```json
{
  "type": "VM_STOP",
  "id": "server-1704067200000-123460",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "force": false,
    "timeout": 60,
    "saveState": true,
    "reason": "TASK_COMPLETED",
    "gracefulShutdown": true
  },
  "signature": "base64_encoded_signature"
}
```

### 5.4 停止虚拟机确认 (VM_STOP_ACK) 🔵

**消息作用**: 虚拟机确认收到停止命令并开始关闭过程。

```json
{
  "type": "VM_STOP_ACK",
  "id": "client-1704067200000-123460",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "STOPPING",
    "estimatedShutdownTime": 30,
    "stateSaved": true,
    "message": "虚拟机正在优雅关闭"
  },
  "signature": "base64_encoded_signature"
}
```

## 6. 数据集管理协议

### 6.1 数据集列表查询 (DATASET_LIST_QUERY) 🟢 🆕 v1.5新增

**消息作用**: 后端向虚拟机查询可用数据集列表，用于任务创建前的数据集发现和选择。

**使用场景**:
- 联邦学习任务创建前查询虚拟机可用数据集
- 数据集分配算法的数据源发现
- 前端界面展示可用数据集列表
- 数据集状态健康检查

**消息格式**:
```json
{
  "type": "DATASET_LIST_QUERY",
  "id": "server-1704067200000-300001",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "includePreview": true,
    "maxPreviewRows": 10,
    "includeStatistics": true,
    "datasetTypes": ["ACOUSTIC", "ENVIRONMENTAL", "ALL"]
  }
}
```

**字段说明**:
- `includePreview`: 是否包含数据集预览样本
- `maxPreviewRows`: 预览数据最大行数
- `includeStatistics`: 是否包含数据集统计信息
- `datasetTypes`: 查询的数据集类型过滤器

### 6.2 数据集列表响应 (DATASET_LIST_RESPONSE) 🔵 🆕 v1.5新增

**消息作用**: 虚拟机响应数据集列表查询，返回可用数据集的详细信息。

**响应格式**:
```json
{
  "type": "DATASET_LIST_RESPONSE",
  "id": "client-1704067200000-300001",
  "timestamp": "2024-01-01T00:00:00.100Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "totalDatasets": 3,
    "datasets": [
      {
        "localDatasetId": "local-dataset-001",
        "name": "acoustic_features_v1",
        "description": "水声传播特征数据集",
        "datasetType": "ACOUSTIC",
        "rowCount": 10000,
        "sizeBytes": 2048576,
        "createdAt": "2024-01-01T00:00:00.000Z",
        "lastModified": "2024-01-01T12:00:00.000Z",
        "status": "READY",
        "preview": [
          {
            "frequency": 1000,
            "amplitude": 0.85,
            "phase": 1.57,
            "snr": 25.4
          }
        ],
        "statistics": {
          "featureCount": 4,
          "labelCount": 2,
          "missingValues": 0,
          "dataQuality": "HIGH"
        }
      }
    ]
  }
}
```

**关键字段说明**:
- `localDatasetId`: 虚拟机本地数据集标识符（待后端分配新ID）
- `name`: 数据集名称
- `description`: 数据集描述
- `datasetType`: 数据集类型（ACOUSTIC/ENVIRONMENTAL等）
- `rowCount`: 数据行数
- `sizeBytes`: 数据集大小（字节）
- `status`: 数据集状态（READY/LOADING/ERROR等）
- `preview`: 数据预览样本
- `statistics`: 数据集统计信息

### 6.3 创建数据集 (DATASET_CREATE) 🔵

**消息作用**: 虚拟机向后端请求创建新的数据集，或后端通知虚拟机准备接收数据集。

**使用场景**:
- 联邦学习任务开始前分发训练数据
- 动态创建特定任务的数据子集
- **多任务场景下的数据隔离**: 通过taskId字段实现数据集的任务级别隔离
- 数据版本管理和追踪

**支持的数据类型**:
- ACOUSTIC: 水声传播数据
- ENVIRONMENT: 环境参数数据
- MODEL: 模型相关数据
- OTHER: 其他类型数据
- TEST_DATA: 测试数据
- SPECIAL_CHARS: 特殊字符数据
- LONG_TEXT: 长文本数据

```json
{
  "type": "DATASET_CREATE",
  "id": "client-1704067200000-200001",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：后端分配的统一数据集ID
    "datasetName": "acoustic_features_v1",
    "datasetDescription": "水声传播特征数据集",
    "datasetType": "ACOUSTIC",
    "expectedRows": 10000,
    "metadata": {
      "source": "bellhop_simulation",
      "version": "1.0",
      "features": ["frequency", "depth", "range", "transmission_loss"],
      "labels": ["propagation_mode"]
    },
    "schema": {
      "frequency": "float",
      "depth": "float",
      "range": "float",
      "transmission_loss": "float",
      "propagation_mode": "int"
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 6.2 追加数据行（批量）(DATASET_APPEND_ROWS) 🔵

**消息作用**: 向已创建的数据集批量添加数据行。

```json
{
  "type": "DATASET_APPEND_ROWS",
  "id": "client-1704067200000-200002",
  "timestamp": "2024-01-01T00:00:01.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：后端分配的统一数据集ID
    "batchId": "batch-001",
    "totalBatches": 10,
    "currentBatch": 1,
    "rows": [
      {
        "rowId": "row-001",
        "data": {
          "frequency": 1000.0,
          "depth": 50.0,
          "range": 1000.0,
          "transmission_loss": 65.5,
          "propagation_mode": 1
        }
      },
      {
        "rowId": "row-002",
        "data": {
          "frequency": 1500.0,
          "depth": 75.0,
          "range": 1500.0,
          "transmission_loss": 72.3,
          "propagation_mode": 2
        }
      }
    ],
    "compression": "gzip",
    "checksum": "sha256:batch001_checksum"
  },
  "signature": "base64_encoded_signature"
}
```

### 6.3 完成数据集上传 (DATASET_COMPLETE) 🔵

**消息作用**: 通知数据集上传完成，可以开始使用。

```json
{
  "type": "DATASET_COMPLETE",
  "id": "client-1704067200000-200003",
  "timestamp": "2024-01-01T00:00:10.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：后端分配的统一数据集ID
    "finalRowCount": 9856,
    "totalBatches": 10,
    "uploadDuration": 9.5,
    "dataIntegrity": {
      "checksumValid": true,
      "missingRows": 0,
      "duplicateRows": 0
    },
    "statistics": {
      "meanFrequency": 1250.5,
      "meanDepth": 62.3,
      "meanRange": 1125.8,
      "labelDistribution": {
        "mode_1": 4928,
        "mode_2": 4928
      }
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 6.4 数据集状态查询 (DATASET_STATUS_QUERY) 🟢

**消息作用**: 后端查询特定数据集的状态和统计信息。

```json
{
  "type": "DATASET_STATUS_QUERY",
  "id": "server-1704067200000-200004",
  "timestamp": "2024-01-01T00:00:15.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：后端分配的统一数据集ID
    "queryType": "FULL",
    "includeStatistics": true,
    "includeMetadata": true,
    "includeSampleData": false
  },
  "signature": "base64_encoded_signature"
}
```

### 6.2 数据集状态查询 (DATASET_STATUS_QUERY) 🟢

**消息作用**: 后端查询特定数据集的状态和统计信息。

**核心价值**:
- **数据集监控**: 实时了解数据集的状态、大小、完整性
- **多任务数据管理**: 在多任务环境中独立查询各任务的数据集状态
- **故障诊断**: 当数据集出现问题时获取详细信息
- **存储管理**: 了解数据集占用的存储空间，便于资源优化

**使用场景**:
- 任务启动前验证数据集准备状态
- 定期检查数据集完整性
- 多任务场景下的数据集状态同步
- 存储空间管理和清理决策

```json
{
  "type": "DATASET_STATUS_QUERY",
  "id": "server-1704067200000-200004",
  "timestamp": "2024-01-01T00:00:15.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：后端分配的统一数据集ID
    "queryType": "FULL",
    "includeStatistics": true,
    "includeMetadata": true,
    "includeSampleData": false
  },
  "signature": "base64_encoded_signature"
}
```

### 6.3 数据集状态响应 (DATASET_STATUS_RESPONSE) 🔵

**消息作用**: 虚拟机响应数据集状态查询，提供详细的数据集信息。

**提供的关键信息**:
- **数据集状态**: CREATING/READY/UPLOADING/ERROR等
- **数据统计**: 行数、大小、创建时间、最后修改时间
- **数据完整性**: 校验和验证、缺失行检查
- **使用情况**: 访问次数、最后访问时间

```json
{
  "type": "DATASET_STATUS_RESPONSE",
  "id": "client-1704067200000-200004",
  "timestamp": "2024-01-01T00:00:15.100Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：后端分配的统一数据集ID
    "status": "READY",
    "rowCount": 9856,
    "sizeBytes": 2048576,
    "createdAt": "2024-01-01T00:00:00.000Z",
    "lastModified": "2024-01-01T00:00:10.000Z",
    "usage": {
      "accessCount": 5,
      "lastAccessed": "2024-01-01T00:00:14.000Z"
    },
    "integrity": {
      "isValid": true,
      "lastValidated": "2024-01-01T00:00:10.000Z",
      "checksum": "sha256:abc123..."
    },
    "statistics": {
      "meanFrequency": 1250.5,
      "meanDepth": 62.3,
      "labelDistribution": {
        "mode_1": 4928,
        "mode_2": 4928
      }
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 6.4 删除数据集 (DATASET_DELETE) 🟢

**消息作用**: 后端请求删除指定的数据集。

```json
{
  "type": "DATASET_DELETE",
  "id": "server-1704067200000-200005",
  "timestamp": "2024-01-01T00:10:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：后端分配的统一数据集ID
    "reason": "TASK_COMPLETED",
    "backup": false,
    "force": false
  },
  "signature": "base64_encoded_signature"
}
```

## 7. 标准联邦学习流程

### 7.1 v1.5 完整13步联邦学习流程 🆕

#### 阶段一：环境准备（步骤1-2）

```
1. 虚拟机启动控制
   Backend → VM_START → VM
   VM → VM_START_ACK → Backend

2. 虚拟机连接建立
   VM → CONNECT → Backend
   Backend → CONNECT_ACK → VM
```

#### 阶段二：前端任务创建（步骤3-5）

```
3. 前端创建联邦学习任务
   Frontend → Backend HTTP API: POST /api/federated-tasks
   // 前端通过REST API创建任务，指定参与虚拟机和数据集要求

4. 后端生成数据集关联配置
   Backend Internal:
   - 使用 UuidUtil 生成 taskId
   - 使用 UuidUtil 生成每个虚拟机的 assignedDatasetId
   - 在 task_participants 表中建立关联关系

5. 后端任务配置确认
   Backend Internal:
   - 验证虚拟机可用性
   - 确认数据集分配策略
   - 准备任务启动参数
```

#### 阶段三：数据集分发（步骤6-8）

```
6. 数据集创建分发 🆕 v1.5 优化
   Backend → DATASET_CREATE(assignedDatasetId) → Specific VMs
   // 后端向每个虚拟机分发其被分配的数据集ID（由后端生成）

7. 数据集内容分发
   Backend → DATASET_APPEND_ROWS(assignedDatasetId, data) → Specific VMs (批量)
   Backend → DATASET_COMPLETE(assignedDatasetId) → Specific VMs
   // 虚拟机使用后端提供的assignedDatasetId保存数据集

8. 数据集创建确认 🆕 v1.5 新增
   Backend → DATASET_STATUS_QUERY(assignedDatasetId) → Specific VMs
   Specific VMs → DATASET_STATUS_RESPONSE(assignedDatasetId) → Backend
   // 后端确认指定ID的数据集已在虚拟机中成功创建
```

#### 阶段四：任务启动（步骤9-10）

```
9. 联邦学习任务启动 🆕 v1.5 改进
   Backend → FEDERATED_TASK_START(taskId, assignedDatasetId) → All VMs
   // 任务启动消息现在包含后端分配的数据集ID

10. 任务启动确认
    All VMs → FEDERATED_TASK_START_ACK(taskId, datasetStatus) → Backend
    // 虚拟机确认任务启动并报告数据集状态
```

#### 阶段五：训练轮次执行（步骤11）

```
11. 轮次循环执行 (重复执行)
    Loop for each round:
      Backend → ROUND_START(taskId, roundNumber) → All VMs
      All VMs → ROUND_START_ACK(taskId, roundNumber) → Backend

      [虚拟机使用assignedDatasetId执行本地训练...]

      All VMs → GRADIENT_UPLOAD(taskId, roundNumber, gradients) → Backend
      Backend → GRADIENT_UPLOAD_ACK(taskId, roundNumber) → All VMs

      [后端执行模型聚合...]

      Backend → GLOBAL_MODEL_BROADCAST(taskId, roundNumber, model) → All VMs
      All VMs → GLOBAL_MODEL_BROADCAST_ACK(taskId, roundNumber) → Backend

      Backend → ROUND_COMPLETE(taskId, roundNumber) → All VMs
      All VMs → ROUND_COMPLETE_ACK(taskId, roundNumber) → Backend
    End Loop
```

#### 阶段六：任务完成清理（步骤12-13）

```
12. 任务结束处理
    Backend → FEDERATED_TASK_STOP(taskId) → All VMs
    All VMs → FEDERATED_TASK_STOP_ACK(taskId) → Backend

13. 资源清理回收
    Backend → FEDERATED_TASK_DELETE(taskId) → All VMs
    All VMs → FEDERATED_TASK_DELETE_ACK(taskId) → Backend
    Backend → DATASET_DELETE(assignedDatasetId) → All VMs
    Backend → VM_STOP → All VMs
    All VMs → VM_STOP_ACK → Backend
```

#### 并行维护流程

```
心跳维持 (并行执行)
   Every 30s:
     VM → HEARTBEAT → Backend
     Backend → HEARTBEAT_ACK → VM

状态查询 (按需执行)
   Backend → VM_STATUS_QUERY → VM
   VM → VM_STATUS_RESPONSE → Backend (包含所有任务概览)
   Backend → FEDERATED_TASK_STATUS_QUERY(taskId) → VM
   VM → FEDERATED_TASK_STATUS_RESPONSE(taskId) → Backend (特定任务详情)

错误处理 (任何时候)
   VM/Backend → ERROR → Recipient

任务恢复流程 (可选)
   Backend → FEDERATED_TASK_STOP(taskId) → VM
   VM → FEDERATED_TASK_STOP_ACK(taskId) → Backend

   [任务暂停期间可进行维护或资源调整...]

   Backend → FEDERATED_TASK_RESUME(taskId, resumeFrom) → VM
   VM → FEDERATED_TASK_RESUME_ACK(taskId) → Backend

   [继续执行轮次循环，从指定轮次开始...]
```

### 7.2 多任务并发流程示例

```
VM-001 并发执行两个任务的典型场景:

时刻T1: 启动第一个任务
   Backend → FEDERATED_TASK_START(fedtask-audio-001) → VM-001
   VM-001 → FEDERATED_TASK_START_ACK(fedtask-audio-001) → Backend

时刻T2: 启动第二个任务
   Backend → FEDERATED_TASK_START(fedtask-video-002) → VM-001
   VM-001 → FEDERATED_TASK_START_ACK(fedtask-video-002) → Backend

时刻T3: 同时进行两个任务的训练
   Backend → ROUND_START(fedtask-audio-001, round=3) → VM-001
   Backend → ROUND_START(fedtask-video-002, round=1) → VM-001

   [VM-001 同时训练两个任务...]

   VM-001 → GRADIENT_UPLOAD(fedtask-audio-001, round=3) → Backend
   VM-001 → GRADIENT_UPLOAD(fedtask-video-002, round=1) → Backend

时刻T4: 停止第二个任务，继续第一个任务
   Backend → FEDERATED_TASK_STOP(fedtask-video-002) → VM-001

   [只有fedtask-audio-001继续训练...]

时刻T5: 恢复第二个任务（演示RESUME功能）
   Backend → FEDERATED_TASK_RESUME(fedtask-video-002, resumeFrom={round: 1}) → VM-001
   VM-001 → FEDERATED_TASK_RESUME_ACK(fedtask-video-002) → Backend

   [现在两个任务都在运行...]

时刻T6: 状态查询显示多任务状态
   Backend → VM_STATUS_QUERY → VM-001
   VM-001 → VM_STATUS_RESPONSE → Backend
   // 响应包含两个任务的详细状态信息

时刻T7: 精确停止第一个任务
   Backend → FEDERATED_TASK_STOP(fedtask-audio-001) → VM-001
   VM-001 → FEDERATED_TASK_STOP_ACK(fedtask-audio-001) → Backend

时刻T8: 彻底删除已完成的任务
   Backend → FEDERATED_TASK_DELETE(fedtask-audio-001) → VM-001
   VM-001 → FEDERATED_TASK_DELETE_ACK(fedtask-audio-001) → Backend

   [VM-001 彻底清理fedtask-audio-001，fedtask-video-002已停止]
```

### 7.2 时序要求

- **虚拟机启动**: 任务开始前确保所有虚拟机启动完成
- **数据集准备**: 训练前确保所有虚拟机数据集同步完成
- **任务启动**: 所有虚拟机必须确认收到FEDERATED_TASK_START
- **轮次同步**: 等待所有参与者发送ROUND_START_ACK后开始训练
- **梯度收集**: 等待最少数量的虚拟机上传梯度
- **模型分发**: 聚合完成后立即广播给所有参与者
- **任务清理**: 任务完成后及时清理数据集和停止虚拟机

## 8. 与原协议对比

### 8.1 删除的协议 (21个)

**训练控制重复 (5个):**
- ~~TRAINING_START~~ → 合并到 FEDERATED_TASK_START
- ~~TRAINING_START_ACK~~ → 合并到 FEDERATED_TASK_START_ACK
- ~~TRAINING_START_COMMAND~~
- ~~TRAINING_START_RESPONSE~~
- ~~TRAINING_START_RESPONSE_ACK~~

**模型传输重复 (1个):**
- ~~MODEL_UPLOAD~~ → 使用 GRADIENT_UPLOAD

**任务管理重复 (2个):**
- ~~TASK_START / TASK_START_ACK~~ → 使用联邦任务协议

**训练进度过度细分 (4个):**
- ~~TRAINING_PROGRESS~~ → 通过心跳报告状态
- ~~TRAINING_PROGRESS_ACK~~
- ~~TRAINING_PROGRESS_RESPONSE~~
- ~~TRAINING_PROGRESS_RESPONSE_ACK~~

**聚合过程过度细分 (6个):**
- ~~AGGREGATION_START / AGGREGATION_START_ACK~~ → 隐含在轮次流程中
- ~~AGGREGATION_COMPLETE / AGGREGATION_COMPLETE_ACK~~ → 合并到模型广播
- ~~AGGREGATION_NOTIFICATION~~
- ~~GRADIENT_UPLOAD_PREPARE / GRADIENT_UPLOAD_PREPARE_ACK~~

**协商和配置 (4个):**
- ~~MODEL_TYPE_NEGOTIATION / MODEL_TYPE_NEGOTIATION_ACK~~ → 任务启动时指定
- ~~ALGORITHM_CONFIG / ALGORITHM_CONFIG_ACK~~ → 合并到任务配置

### 8.2 v1.5 保留和新增的协议 (36个) 🆕

**连接管理 (4个): ✅**
- CONNECT / CONNECT_ACK
- HEARTBEAT / HEARTBEAT_ACK

**数据集预查询 (2个): 🆕 v1.5 新增**
- DATASET_LIST_QUERY → 后端查询虚拟机可用数据集
- DATASET_LIST_RESPONSE → 虚拟机响应数据集列表

**任务管理 (6个): ✅ v1.5 增强**
- FEDERATED_TASK_START / FEDERATED_TASK_START_ACK → 现在包含assignedDatasetId
- FEDERATED_TASK_STOP / FEDERATED_TASK_STOP_ACK
- FEDERATED_TASK_RESUME / FEDERATED_TASK_RESUME_ACK

**轮次管理 (8个): ✅**
- ROUND_START / ROUND_START_ACK
- GRADIENT_UPLOAD / GRADIENT_UPLOAD_ACK
- GLOBAL_MODEL_BROADCAST / GLOBAL_MODEL_BROADCAST_ACK
- ROUND_COMPLETE / ROUND_COMPLETE_ACK

**状态监控 (6个): ✅**
- VM_STATUS_QUERY / VM_STATUS_RESPONSE
- FEDERATED_TASK_STATUS_QUERY / FEDERATED_TASK_STATUS_RESPONSE
- ERROR

**虚拟机控制 (4个): ✅**
- VM_START / VM_START_ACK → 后端集中控制虚拟机启动
- VM_STOP / VM_STOP_ACK → 后端集中控制虚拟机停止

**数据集管理 (6个): ✅ v1.5 优化**
- DATASET_CREATE → 现在由后端分发assignedDatasetId
- DATASET_APPEND_ROWS → 批量数据同步
- DATASET_COMPLETE → 数据集上传完成通知
- DATASET_STATUS_QUERY / DATASET_STATUS_RESPONSE → 数据集状态管理
- DATASET_DELETE → 任务后清理数据集

### 8.3 v1.5协议优化效果 🆕

- **协议数量**: 46 → 36 (减少22%，新增数据集关联管理)
- **数据集关联**: 🆕 新增2个协议实现13步完整数据集关联流程
- **ID统一管理**: 🆕 所有ID由后端UuidUtil生成，确保一致性
- **多任务支持**: 通过taskId字段实现精确的任务级别控制
- **完整生命周期**: 支持START→STOP→RESUME→DELETE的完整任务生命周期管理
- **任务恢复机制**: RESUME协议支持从停止状态精确恢复任务执行
- **数据集管理增强**: 🆕 支持assignedDatasetId精确分发和状态确认
- **前端后端协调**: 🆕 完整的Frontend→Backend→VM三层协调机制
- **状态监控统一**: 双层监控机制 - VM整体状态 + 任务详细状态
- **网络往返**: 减少不必要的RTT，消除协议重复
- **实现复杂度**: 显著降低整体复杂度，增强任务管理能力
- **维护成本**: 大幅降低，提供清晰的状态转换机制
- **集中控制**: 增强后端对虚拟机和数据的完全控制
- **资源管理**: DELETE协议支持彻底的资源清理和回收
- **数据隔离**: 🆕 assignedDatasetId确保多任务数据集精确隔离

## 9. 实施建议

### 9.1 迁移策略
1. **并行支持**: 暂时同时支持新旧协议
2. **渐进迁移**: 按模块逐步切换到优化协议
3. **兼容性处理**: 提供协议版本协商机制
4. **集中控制实施**: 先实现虚拟机控制，再实现数据集管理

### 9.2 v1.5测试重点 🆕
1. **13步完整流程**: 验证从前端创建到资源清理的完整13步流程
2. **数据集关联**: 🆕 测试assignedDatasetId的精确分发和关联
3. **ID一致性**: 🆕 验证UuidUtil生成的ID在整个系统中的一致性
4. **数据集状态确认**: 🆕 测试DATASET_STATUS_QUERY/RESPONSE的确认机制
5. **模型分发一致性**: 确保所有虚拟机收到相同的初始模型
6. **轮次同步**: 验证所有参与者的轮次同步
7. **多任务并发**: 测试单VM同时运行多个联邦学习任务
8. **任务级别控制**: 验证通过taskId进行精确任务控制
9. **虚拟机生命周期**: 测试启动、停止和故障恢复
10. **数据集隔离**: 验证多任务场景下的数据集独立性
11. **状态监控**: 测试VM_STATUS_RESPONSE的多任务状态展示
12. **错误恢复**: 测试各种异常情况的处理
13. **前端后端协调**: 🆕 测试Frontend→Backend→VM的三层协调
14. **性能对比**: 与v1.4协议进行性能基准测试

### 9.3 v1.5监控指标 🆕
1. **协议覆盖率**: 确保36个协议覆盖所有场景
2. **13步流程完整性**: 🆕 监控从前端创建到资源清理的完整13步执行
3. **数据集关联准确性**: 🆕 监控assignedDatasetId的分发和关联准确率
4. **ID一致性检查**: 🆕 验证UuidUtil生成的ID在系统中的一致性
5. **数据集状态确认效率**: 🆕 监控DATASET_STATUS_QUERY/RESPONSE的响应时间
6. **多任务性能**: 监控单VM多任务执行效率
7. **任务生命周期**: 监控START→STOP→RESUME→DELETE完整流程的执行效率
8. **数据集监控**: 验证DATASET_STATUS_QUERY的数据集管理效果
9. **任务隔离度**: 监控任务间的资源隔离效果
10. **状态监控效率**: 验证双层监控机制的有效性
11. **资源清理效率**: 监控DELETE协议的资源回收效果
12. **消息传输效率**: 监控网络使用情况
13. **虚拟机管理效率**: 监控启停时间和成功率
14. **数据同步性能**: 监控数据传输速度和完整性
15. **错误率**: 跟踪协议执行的成功率
16. **同步精度**: 监控虚拟机间的时序一致性
17. **资源利用率**: 监控VM资源在多任务间的分配效率
18. **前端响应性能**: 🆕 监控Frontend→Backend API的响应时间
19. **数据集隔离效果**: 🆕 验证assignedDatasetId的多任务隔离效果

---

**协议版本**: v1.5 🆕
**文档版本**: 2.0
**最后更新**: 2025-01-29
**维护者**: FedUWAComm开发团队

## v1.5 更新记录

- **2025-01-29**: 升级到v1.5版本
  - 新增13步完整联邦学习流程，包含数据集关联管理
  - 新增2个数据集预查询协议：DATASET_LIST_QUERY/RESPONSE
  - 优化FEDERATED_TASK_START协议，新增assignedDatasetId字段
  - 强化ID统一管理，所有ID由后端UuidUtil生成
  - 增强前端→后端→虚拟机三层协调机制
  - 协议总数从34个增加到36个
  - 更新测试重点和监控指标以适应v1.5新特性