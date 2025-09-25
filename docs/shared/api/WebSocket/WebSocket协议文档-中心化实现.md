# 水声联邦学习系统 WebSocket 通信协议 - 中心化实现

## 0. 架构设计

### 0.1 中心化架构模式

本系统采用中心化WebSocket架构模式：

```
可视化客户端 ←→ WebSocket ←→ 后端服务器 ←→ WebSocket ←→ 虚拟机
```

**特点**:
- 后端服务器作为WebSocket代理
- 统一的连接管理和认证
- 更好的安全控制和负载均衡
- 支持NAT穿透和防火墙
- 推荐用于生产环境

### 0.2 网络配置要求

#### 0.2.1 服务器端配置
- 开放WebSocket端口（默认8080）
- 配置防火墙规则
- 检查端口监听状态

#### 0.2.2 客户端网络要求
- 能够访问服务器的IP地址和端口
- 支持WebSocket协议
- 网络延迟 < 100ms（推荐）

### 0.3 架构职责分工 (v1.3优化)

#### 0.3.1 后端职责
- 联邦学习算法选择和配置（FEDAVG、FEDPROX等）
- 全局模型聚合和分发
- 任务编排和工作流管理
- 虚拟机资源调度和分配

#### 0.3.2 虚拟机职责
- 本地机器学习模型训练（RandomForest、SVM等）
- 数据预处理和特征提取
- 模型参数上传和下载
- 系统状态监控和上报

#### 0.3.3 安全考虑
- 使用JWT Token进行身份验证
- WSS协议（TLS加密）通信
- IP白名单和连接频率限制

### 0.4 前置注册与认证流程
- 第一步（HTTP）: 虚拟机向后端发起注册请求，注册成功后返回 `accessToken`、`secretId` 和建议的 WebSocket 连接信息
- 第二步（WebSocket/STOMP）: 虚拟机使用 `accessToken` 建立 WebSocket 连接，并在 STOMP CONNECT 帧或 URL 查询参数中携带 Token
- 第三步（应用层）: 连接建立后发送应用层 `CONNECT` 消息，进行能力与环境上报

> 说明：后端目前提供 STOMP 端点（`/ws` 带 SockJS、`/ws-native` 原生 WebSocket）。推荐通过 STOMP CONNECT 头部携带 `Authorization: Bearer <token>`，避免在 URL 里暴露 Token。

## 1. 概述

本文档定义了水声联邦学习系统的中心化WebSocket通信协议，用于实现服务器与虚拟机之间的实时双向通信，支持虚拟机控制、学习控制、状态监控等功能。

### 1.1 基础信息
- **WebSocket (SockJS) URL**: `http://localhost:8080/ws` (开发环境)
- **WebSocket (原生) URL**: `ws://localhost:8080/ws-native` (开发环境)
- **WebSocket Secure URL**: `wss://your-domain.com/ws-native` (生产环境)
- **协议版本**: v1.3 (移除联邦算法配置，保留本地ML算法)
- **认证方式**: JWT Token（必需）
- **数据格式**: JSON
- **编码**: UTF-8
- **TLS版本**: TLS 1.2及以上（WSS连接）

### 1.2 连接参数
- `vmId`: 虚拟机唯一标识，32位UUID格式（必需）
- `token`: JWT认证令牌（必需，优先通过 STOMP CONNECT 头 `Authorization: Bearer <token>` 传递；如使用原生 WebSocket 也可用查询参数）
- `version`: 客户端版本号（可选）

### 1.3 连接示例
```python
# pip install websocket-client
import websocket
import threading
import time

ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
VM_ID = "a1b2c3d4e5f678901234567890123456"

WS_URL = "ws://localhost:8080/ws-native"  # 开发环境
# 生产环境：WS_URL = "wss://your-domain.com/ws-native"


def build_stomp_connect_frame(host: str, token: str, vm_id: str) -> str:
    # 在 STOMP CONNECT 帧中携带 Authorization 与 vmId
    headers = [
        "accept-version:1.2",
        f"host:{host}",
        f"Authorization: Bearer {token}",
        f"vmId:{vm_id}",
    ]
    return "CONNECT\n" + "\n".join(headers) + "\n\n\0"


def on_open(ws):
    frame = build_stomp_connect_frame(host="localhost", token=ACCESS_TOKEN, vm_id=VM_ID)
    ws.send(frame)


def on_message(ws, message):
    # 预期先收到 STOMP 的 CONNECTED 帧
    print("<-", message)


def on_error(ws, error):
    print("[error]", error)


def on_close(ws, close_status_code, close_msg):
    print("[closed]", close_status_code, close_msg)


ws = websocket.WebSocketApp(
    WS_URL,
    on_open=on_open,
    on_message=on_message,
    on_error=on_error,
    on_close=on_close,
)

# 心跳：可按需设置（示例）
th = threading.Thread(target=ws.run_forever, kwargs={"ping_interval": 25, "ping_timeout": 10})
th.daemon = True
th.start()

# 简单等待，观察握手与服务器响应
time.sleep(10)
```

## 2. 消息格式

### 2.1 标准消息格式
所有WebSocket消息都采用JSON格式，包含以下字段：

```json
{
  "type": "MESSAGE_TYPE",
  "id": "unique_message_id",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {},
  "signature": "base64_encoded_signature"
}
```

### 2.2 字段说明
- `type`: 消息类型（必需）
- `id`: 消息唯一标识（必需）
- `timestamp`: 消息时间戳（必需）
- `vmId`: 虚拟机ID，32位UUID格式（必需）
- `data`: 消息数据（必需）
- `signature`: 数字签名（必需，用于安全验证）

### 2.3 消息ID生成规则
- 客户端消息：`client-{timestamp}-{random}`
- 服务器消息：`server-{timestamp}-{random}`
- 命令消息：`cmd-{timestamp}-{random}`

## 3. 消息类型定义

### 3.1 连接管理消息

#### 3.1.1 客户端连接请求 (CONNECT)
```json
{
  "type": "CONNECT",
  "id": "client-1704067200000-123456",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "version": "1.0.0",
    "supportedMLAlgorithms": ["RandomForest", "SVM", "NeuralNetwork", "XGBoost"],
    "systemInfo": {
      "os": "Ubuntu 20.04",
      "python": "3.8.10",
      "memory": "4GB",
      "cpu": "Intel Xeon E5-2680",
      "gpu": "NVIDIA Tesla V100"
    },
    "computeCapabilities": {
      "maxBatchSize": 1024,
      "gpuMemory": "16GB",
      "parallelProcessing": true,
      "frameworks": ["sklearn", "pytorch", "tensorflow"]
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.1.2 服务器连接确认 (CONNECT_ACK)
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
    "maxMessageSize": 10485760,
    "supportedFeatures": ["ENCRYPTION", "COMPRESSION", "BATCH_OPERATIONS"]
  },
  "signature": "base64_encoded_signature"
}
```

### 3.2 心跳消息

#### 3.2.1 客户端心跳 (HEARTBEAT)
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
      "disk": 45.8,
      "gpu": 15.3
    },
    "network": {
      "uploadSpeed": 1024,
      "downloadSpeed": 2048,
      "latency": 50
    },
    "processes": {
      "total": 150,
      "active": 25
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.2.2 服务器心跳响应 (HEARTBEAT_ACK)
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

### 3.3 虚拟机控制消息

#### 3.3.1 启动虚拟机命令 (VM_START)
```json
{
  "type": "VM_START",
  "id": "cmd-1704067200000-123459",
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
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.3.2 停止虚拟机命令 (VM_STOP)
```json
{
  "type": "VM_STOP",
  "id": "cmd-1704067200000-123460",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "force": false,
    "timeout": 60,
    "saveState": true
  },
  "signature": "base64_encoded_signature"
}
```

### 3.4 学习控制消息

#### 3.4.1 开始训练命令 (TRAINING_START)
```json
{
  "type": "TRAINING_START",
  "id": "cmd-1704067200000-123463",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
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
    },
    "globalModel": {
      "modelId": "model-123456",
      "version": "1.0.0",
      "parameters": {
        "layers": 3,
        "neurons": [784, 256, 128, 10],
        "activation": "relu",
        "optimizer": "adam"
      },
      "downloadUrl": "/api/v1/model/download/model-123456"
    },
    "dataConfig": {
      "dataPath": "/data/training",
      "validationSplit": 0.2,
      "shuffle": true
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.4.2 停止训练命令 (TRAINING_STOP)
```json
{
  "type": "TRAINING_STOP",
  "id": "cmd-1704067200000-123464",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "reason": "MANUAL_STOP",
    "saveCheckpoint": true,
    "cleanup": true
  },
  "signature": "base64_encoded_signature"
}
```

### 3.5 训练进度消息

#### 3.5.1 训练进度报告 (TRAINING_PROGRESS)
```json
{
  "type": "TRAINING_PROGRESS",
  "id": "client-1704067200000-123468",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "currentRound": 25,
    "totalRounds": 100,
    "currentEpoch": 3,
    "epochsPerRound": 5,
    "progress": 25.5,
    "metrics": {
      "accuracy": 0.85,
      "loss": 0.15,
      "valAccuracy": 0.82,
      "valLoss": 0.18,
      "precision": 0.87,
      "recall": 0.83,
      "f1Score": 0.85
    },
    "status": "TRAINING",
    "estimatedTimeRemaining": 1800,
    "resourceUsage": {
      "cpu": 85.5,
      "memory": 75.2,
      "gpu": 95.8
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 3.6 模型传输消息

#### 3.6.1 本地模型上传 (MODEL_UPLOAD)
```json
{
  "type": "MODEL_UPLOAD",
  "id": "client-1704067200000-123470",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "round": 25,
    "parameters": {
      "model": {
        "framework": "pytorch",
        "format": "state_dict",
        "weights": {
          "shape": [784, 256, 128, 10],
          "dtype": "float32",
          "checksum": "sha256:abc123..."
        }
      },
      "training": {
        "epochs": 5,
        "batchSize": 32,
        "optimizer": "adam",
        "learningRate": 0.001
      }
    },
    "metrics": {
      "accuracy": 0.88,
      "loss": 0.12,
      "valAccuracy": 0.85,
      "valLoss": 0.15
    },
    "compression": "gzip"
  },
  "signature": "base64_encoded_signature"
}
```

> 说明：v1.3版本优化：上传消息不再包含联邦学习算法相关信息，虚拟机只负责本地训练，所有联邦聚合算法由后端统一管理。

> 补充：收到 VM 端 MODEL_UPLOAD 后，服务端根据任务配置的联邦学习算法进行聚合，将本地训练结果存入 `vm_round_models`，聚合后的全局模型存入 `model_versions`。

#### 3.6.2 全局模型下发 (MODEL_DOWNLOAD)
```json
{
  "type": "MODEL_DOWNLOAD",
  "id": "server-1704067200000-123471",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "round": 26,
    "parameters": {
      "model": {
        "framework": "pytorch",
        "format": "state_dict",
        "weights": {
          "shape": [784, 256, 128, 10],
          "dtype": "float32",
          "checksum": "sha256:def456..."
        }
      },
      "training": {
        "algorithm": "RandomForest",
        "samples": 1000
      }
    },
    "compression": "gzip"
  },
  "signature": "base64_encoded_signature"
}
```

> 说明：v1.3版本优化：模型下发不再包含联邦学习算法信息，只下发经过聚合处理的模型参数和结构。虚拟机无需了解聚合过程，只需接收并使用新模型参数。

### 3.7 状态查询消息

#### 3.7.1 状态查询请求 (STATUS_QUERY)
```json
{
  "type": "STATUS_QUERY",
  "id": "cmd-1704067200000-123476",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "queryType": "FULL",
    "includeResources": true,
    "includeProcesses": true,
    "includeNetwork": true,
    "timeout": 10
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.7.2 状态查询响应 (STATUS_RESPONSE)
```json
{
  "type": "STATUS_RESPONSE",
  "id": "client-1704067200000-123477",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "RUNNING",
    "uptime": 3600,
    "resourceUsage": {
      "cpu": 25.5,
      "memory": 60.2,
      "disk": 45.8,
      "gpu": 15.3
    },
    "network": {
      "ipAddress": "192.168.1.100",
      "macAddress": "00:11:22:33:44:55",
      "port": 22,
      "uploadSpeed": 1024,
      "downloadSpeed": 2048,
      "latency": 50
    },
    "processes": {
      "total": 150,
      "active": 25,
      "system": 10,
      "user": 15,
      "training": 1
    },
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

### 3.8 错误和状态消息

#### 3.8.1 错误报告 (ERROR)
```json
{
  "type": "ERROR",
  "id": "client-1704067200000-123472",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "errorCode": "TRAINING_FAILED",
    "errorMessage": "模型训练过程中发生错误",
    "severity": "HIGH",
    "details": {
      "exception": "ValueError: Invalid input shape",
      "stackTrace": "Traceback (most recent call last):\n  File \"train.py\", line 45, in model.fit()\nValueError: Invalid input shape",
      "context": {
        "taskId": "task-123456",
        "round": 25,
        "epoch": 3
      }
    },
    "recovery": {
      "automatic": false,
      "suggestions": ["检查数据格式", "验证模型输入维度"]
    }
  },
  "signature": "base64_encoded_signature"
}
```

### 3.9 训练数据同步消息

> 自v1.1起，训练数据采用宽表+JSON存储：数据集元信息写入 `training_dataset`，数据行写入 `training_dataset_row`（`row_data` JSON，`dataset_id` 外键）。以下消息用于通过WebSocket进行数据集创建与增量同步。

#### 3.9.1 创建数据集 (DATASET_CREATE)
```json
{
  "type": "DATASET_CREATE",
  "id": "client-1704067200000-200001",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd",
    "datasetDescription": "水声传播特征数据集",
    "datasetType": "ACOUSTIC",
    "metadata": { "source": "bellhop", "version": "1.0" }
  },
  "signature": "base64_encoded_signature"
}
```

服务器处理：在 `training_dataset` 新建或幂等创建记录（主键为 `datasetId`）。

服务器确认 (DATASET_CREATE_ACK)：
```json
{
  "type": "DATASET_CREATE_ACK",
  "id": "server-1704067200000-200001",
  "timestamp": "2024-01-01T00:00:00.100Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd",
    "status": "READY"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.2 追加数据行（批量）(DATASET_APPEND_ROWS)
```json
{
  "type": "DATASET_APPEND_ROWS",
  "id": "client-1704067200000-200002",
  "timestamp": "2024-01-01T00:00:01.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd",
    "rows": [
      { "rowData": { "f1": 0.12, "f2": 3.4, "label": 1 } },
      { "rowData": { "f1": 0.37, "f2": 2.1, "label": 0 } }
    ]
  },
  "signature": "base64_encoded_signature"
}
```

服务器处理：批量写入 `training_dataset_row`，每条记录的 `dataset_id` = `datasetId`，`row_data` = `rowData`。

服务器确认 (DATASET_APPEND_ROWS_ACK)：
```json
{
  "type": "DATASET_APPEND_ROWS_ACK",
  "id": "server-1704067200000-200002",
  "timestamp": "2024-01-01T00:00:01.120Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd",
    "accepted": 2,
    "rejected": 0
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.3 完成数据集上传 (DATASET_COMPLETE)
```json
{
  "type": "DATASET_COMPLETE",
  "id": "client-1704067200000-200003",
  "timestamp": "2024-01-01T00:00:10.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd"
  },
  "signature": "base64_encoded_signature"
}
```

服务器确认 (DATASET_COMPLETE_ACK)：
```json
{
  "type": "DATASET_COMPLETE_ACK",
  "id": "server-1704067200000-200003",
  "timestamp": "2024-01-01T00:00:10.050Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd",
    "rowCount": 2,
    "status": "READY"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.4 数据集状态查询 (DATASET_STATUS_QUERY / DATASET_STATUS_RESPONSE)
请求：
```json
{
  "type": "DATASET_STATUS_QUERY",
  "id": "cmd-1704067200000-200004",
  "timestamp": "2024-01-01T00:00:20.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": { "datasetId": "dset-1234567890abcdef1234567890abcd" },
  "signature": "base64_encoded_signature"
}
```
响应：
```json
{
  "type": "DATASET_STATUS_RESPONSE",
  "id": "server-1704067200000-200004",
  "timestamp": "2024-01-01T00:00:20.020Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd",
    "datasetDescription": "水声传播特征数据集",
    "datasetType": "ACOUSTIC",
    "rowCount": 2,
    "status": "READY",
    "metadata": { "source": "bellhop", "version": "1.0" }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.5 删除数据集 (DATASET_DELETE)
```json
{
  "type": "DATASET_DELETE",
  "id": "client-1704067200000-200005",
  "timestamp": "2024-01-01T00:00:30.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": { "datasetId": "dset-1234567890abcdef1234567890abcd" },
  "signature": "base64_encoded_signature"
}
```

服务器处理：删除 `training_dataset_row` 中所属行并级联删除 `training_dataset`（或按策略标记为已删除）。

服务器确认 (DATASET_DELETE_ACK)：
```json
{
  "type": "DATASET_DELETE_ACK",
  "id": "server-1704067200000-200005",
  "timestamp": "2024-01-01T00:00:30.030Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd",
    "deleted": true
  },
  "signature": "base64_encoded_signature"
}
```

## 4. 消息处理流程

### 4.1 连接建立流程
1. 前置注册：虚拟机通过 HTTP 注册接口获取 `accessToken`、`secretId`、`vmId`
2. STOMP 握手：使用 `Authorization: Bearer <accessToken>` 与可选 `vmId` 头发起 STOMP CONNECT
3. 身份验证：服务器验证 Token 与 `vmId` 关联关系
4. 应用层连接：客户端发送应用层 CONNECT 消息
5. 开始心跳：启动心跳机制保持连接活跃

### 4.2 心跳机制
- **心跳间隔**: 客户端每30秒发送一次HEARTBEAT消息
- **服务器响应**: 服务器收到后立即回复HEARTBEAT_ACK
- **超时检测**: 如果服务器30秒内未收到心跳，标记虚拟机为离线
- **客户端重连**: 如果客户端60秒内未收到响应，尝试重新连接
- **指数退避**: 重连失败时使用指数退避算法

### 4.3 命令处理流程
1. **命令发送**: 服务器发送命令消息到目标虚拟机
2. **命令接收**: 客户端接收并验证命令签名
3. **命令执行**: 客户端执行相应操作
4. **结果反馈**: 客户端发送执行结果或状态更新
5. **状态同步**: 服务器更新数据库中的虚拟机状态

### 4.4 训练流程
1. **任务启动**: 服务器发送TRAINING_START命令
2. **模型下载**: 客户端从服务器下载全局模型
3. **本地训练**: 客户端开始本地训练过程
4. **进度报告**: 客户端定期发送TRAINING_PROGRESS
5. **模型上传**: 训练完成后发送MODEL_UPLOAD
6. **模型聚合**: 服务器聚合所有客户端模型
7. **新模型分发**: 服务器向所有客户端发送新的全局模型

### 4.5 状态查询流程
1. **查询请求**: 服务器发送STATUS_QUERY消息到目标虚拟机
2. **状态收集**: 虚拟机收集当前状态信息（CPU、内存、磁盘、网络等）
3. **状态响应**: 虚拟机发送STATUS_RESPONSE包含完整状态信息
4. **状态处理**: 服务器处理状态信息并更新缓存
5. **状态分发**: 服务器将状态信息分发给订阅的客户端
6. **监控通知**: 如果配置了监控，发送状态变更通知

### 4.6 批量状态查询流程
1. **批量查询**: 服务器发送BATCH_STATUS_QUERY到多个虚拟机
2. **并行收集**: 多个虚拟机并行收集状态信息
3. **汇总响应**: 服务器汇总所有虚拟机的状态信息
4. **批量响应**: 发送BATCH_STATUS_RESPONSE包含汇总结果
5. **统计分析**: 生成状态统计摘要信息

## 5. 负载均衡和扩展性

### 5.1 服务器端负载均衡
- 最大连接数限制
- 每秒最大消息数限制
- 连接超时时间设置
- 消息频率限制检查

### 5.2 客户端连接池
- 连接池大小管理
- 连接队列处理
- 连接统计信息
- 连接状态监控

## 6. 错误处理

### 6.1 连接错误
```json
{
  "type": "CONNECTION_ERROR",
  "id": "client-1704067200000-123476",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "errorCode": "CONNECTION_TIMEOUT",
    "errorMessage": "连接超时",
    "retryCount": 3,
    "maxRetries": 10
  }
}
```

### 6.2 消息错误
```json
{
  "type": "MESSAGE_ERROR",
  "id": "server-1704067200000-123477",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "errorCode": "INVALID_SIGNATURE",
    "errorMessage": "消息签名验证失败",
    "messageId": "client-1704067200000-123456",
    "suggestion": "请检查密钥配置"
  }
}
```

### 6.3 状态查询错误
```json
{
  "type": "STATUS_QUERY_ERROR",
  "id": "client-1704067200000-123484",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "errorCode": "STATUS_COLLECTION_FAILED",
    "errorMessage": "状态信息收集失败",
    "queryId": "cmd-1704067200000-123476",
    "details": {
      "failedComponents": ["cpu", "memory"],
      "reason": "系统资源不足"
    },
    "suggestion": "请检查系统资源或稍后重试"
  }
}
```

**状态查询错误码**:
- `QUERY_TIMEOUT`: 查询超时
- `STATUS_COLLECTION_FAILED`: 状态收集失败
- `INVALID_QUERY_TYPE`: 无效的查询类型
- `RESOURCE_UNAVAILABLE`: 资源不可用
- `PERMISSION_DENIED`: 权限不足
- `VM_OFFLINE`: 虚拟机离线

### 6.4 重连策略
- **立即重连**: 连接意外断开时立即尝试重连
- **指数退避**: 重连失败时使用指数退避算法
- **最大重试**: 设置最大重试次数（默认10次）
- **重连间隔**: 重连间隔从1秒开始，最大60秒

## 7. 部署和运维

### 7.1 Docker部署
- 使用Java基础镜像
- 暴露WebSocket端口
- 配置健康检查
- 设置环境变量

### 7.2 系统服务配置
- 配置systemd服务
- 设置自动重启
- 配置日志输出
- 设置工作目录

### 7.3 日志配置
- 配置日志级别
- 设置日志文件路径
- 配置日志轮转
- 设置日志格式

## 8. 监控和日志系统

### 8.1 服务器端监控
- 连接数监控
- 消息统计
- 性能指标收集
- 告警机制

### 8.2 客户端监控面板
- 连接状态监控
- 性能指标展示
- 实时数据更新
- 告警处理

这个中心化实现文档提供了：

1. **完整的中心化WebSocket架构设计**
2. **详细的消息类型定义和处理流程**
3. **负载均衡和连接池管理**
4. **错误处理和重连策略**
5. **Docker部署和系统服务配置**
6. **监控和日志系统**

中心化架构的优势在于：
- 统一的连接管理和认证
- 更好的安全控制
- 支持负载均衡
- 便于监控和维护
- 适合生产环境部署 