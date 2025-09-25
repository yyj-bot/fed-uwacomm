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

> 说明：后端提供统一的原生 STOMP 端点（`/ws`）。推荐通过 STOMP CONNECT 头部携带 `Authorization: Bearer <token>`，避免在 URL 里暴露 Token。

## 1. 概述

本文档定义了水声联邦学习系统的中心化WebSocket通信协议，用于实现服务器与虚拟机之间的实时双向通信，支持虚拟机控制、学习控制、状态监控等功能。

### 1.1 基础信息
- **WebSocket URL**: `ws://localhost:8080/ws` (开发环境)
- **WebSocket Secure URL**: `wss://your-domain.com/ws` (生产环境)
- **协议版本**: v1.4 (统一WebSocket端点，移除SockJS支持，完善ACK响应机制和任务管理消息，补充服务端通知消息)
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

WS_URL = "ws://localhost:8080/ws"  # 开发环境
# 生产环境：WS_URL = "wss://your-domain.com/ws"


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

#### 2.3.1 格式定义
消息ID采用统一格式：`{prefix}-{timestamp}-{random}`

#### 2.3.2 组成部分说明
- **prefix (前缀)**：消息来源标识
  - `client`：虚拟机端消息，固定6字符
  - `server`：后端消息，固定6字符
  - `cmd`：命令消息，固定3字符
- **timestamp (时间戳)**：Unix毫秒时间戳，固定13位数字
  - 格式：1704067200000（表示2024-01-01 00:00:00.000 GMT）
  - 范围：2000-01-01 至 2286-11-20
- **random (随机数)**：6位随机数字，范围000000-999999
  - 不足位数使用前导零补齐
  - 同一毫秒内应确保随机数唯一性

#### 2.3.3 完整示例
- 客户端消息：`client-1704067200000-123456` (总长度26字符)
- 服务器消息：`server-1704067200000-654321` (总长度26字符)
- 命令消息：`cmd-1704067200000-789012` (总长度23字符)

#### 2.3.4 生成算法
```pseudocode
function generateMessageId(prefix) {
    timestamp = getCurrentMilliseconds() // 13位Unix毫秒时间戳
    random = generateRandomNumber(0, 999999) // 6位随机数
    randomPadded = padLeft(random, 6, '0') // 左补零至6位
    return prefix + "-" + timestamp + "-" + randomPadded
}
```

#### 2.3.5 验证规则
- **格式正则表达式**：`^(client|server)-\d{13}-\d{6}$|^cmd-\d{13}-\d{6}$`
- **时间戳有效性**：时间戳应为合理的Unix毫秒值
- **随机数格式**：必须为6位数字，不足位数需补零
- **前缀有效性**：仅允许client、server、cmd三种前缀

## 3. 消息类型定义

### 3.1 连接管理消息

> **实现职责说明**:
> - 🔵 **虚拟机端实现**: 消息由虚拟机端发起或处理
> - 🟢 **后端实现**: 消息由后端服务器发起或处理
>
> **重要说明**: 本节详细说明了每个消息的具体作用，以及虚拟机端和后端应该如何实现相应的处理逻辑。

#### 3.1.1 客户端连接请求 (CONNECT) 🔵
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

**消息作用**: 虚拟机启动后向后端服务器发送连接请求，上报自身的系统信息、机器学习能力和计算资源情况。

**虚拟机端实现**:
- 在WebSocket连接建立后立即发送此消息
- 收集并上报本机的系统信息（操作系统、Python版本、硬件配置）
- 列出支持的机器学习算法（RandomForest、SVM、NeuralNetwork等）
- 上报计算能力信息（最大批次大小、GPU内存、并行处理能力）
- 包含支持的框架信息（sklearn、pytorch、tensorflow）

**后端实现**:
- 接收并验证虚拟机的连接请求
- 将虚拟机信息存储到数据库中
- 根据虚拟机能力进行资源调度和任务分配
- 返回CONNECT_ACK确认连接建立

#### 3.1.2 服务器连接确认 (CONNECT_ACK) 🟢
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

**消息作用**: 后端服务器接收到虚拟机连接请求后，发送连接确认消息，提供服务器配置信息和连接会话参数。

**后端实现**:
- 验证虚拟机的连接请求和身份信息
- 为虚拟机分配唯一的会话ID
- 配置心跳间隔、消息大小限制等连接参数
- 向虚拟机通告服务器支持的功能特性
- 在数据库中记录虚拟机连接状态为"在线"

**虚拟机端实现**:
- 接收并解析服务器的连接确认
- 保存会话ID用于后续通信
- 根据服务器返回的心跳间隔启动心跳机制
- 记录服务器支持的功能特性，用于后续协商

### 3.2 心跳消息

#### 3.2.1 客户端心跳 (HEARTBEAT) 🔵

**消息作用**: 虚拟机定期发送心跳消息以维持与后端的连接活跃状态，同时报告当前的系统资源使用情况。

**虚拟机端实现**: 每30秒（或根据服务器配置的间隔）自动发送此消息，收集当前的CPU、内存、磁盘、GPU使用率以及网络状态和进程信息。如果检测到资源使用异常或系统状态变化，可以调整心跳频率。

**后端实现**: 接收并记录虚拟机的心跳消息，更新虚拟机的在线状态和资源使用情况。如果超过设定时间（默认90秒）未收到心跳，将标记虚拟机为离线状态。用于系统监控和负载均衡决策。

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

#### 3.2.2 服务器心跳响应 (HEARTBEAT_ACK) 🟢

**消息作用**: 后端对虚拟机心跳消息的确认响应，确保双向连接正常，并提供服务器状态信息和下次心跳间隔。

**后端实现**: 收到虚拟机心跳后立即发送此响应消息，包含服务器当前时间用于时钟同步，下次心跳的建议间隔，以及服务器整体系统状态。记录虚拟机的最后心跳时间，用于连接状态监控。

**虚拟机端实现**: 接收心跳响应确认连接正常，根据服务器返回的nextHeartbeat调整心跳发送间隔。如果响应中systemStatus为异常状态，可以调整本地行为或发送告警信息。

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

#### 3.3.1 启动虚拟机命令 (VM_START) 🟢
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

#### 3.3.2 停止虚拟机命令 (VM_STOP) 🟢
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

#### 3.4.1 开始训练命令 (TRAINING_START) 🟢
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

#### 3.4.2 训练开始确认 (TRAINING_START_ACK) 🟢
```json
{
  "type": "TRAINING_START_ACK",
  "id": "server-1704067200000-123464",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "COMMAND_SENT",
    "message": "本地训练指令已发送给VM，等待VM确认"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.4.3 训练开始指令 (TRAINING_START_COMMAND) 🟢
```json
{
  "type": "TRAINING_START_COMMAND",
  "id": "server-1704067200000-123465",
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
    "message": "请开始本地ML训练任务"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.4.4 训练开始响应 (TRAINING_START_RESPONSE) 🔵
```json
{
  "type": "TRAINING_START_RESPONSE",
  "id": "client-1704067200000-123466",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "status": "SUCCESS",
    "message": "训练已成功开始",
    "actualConfig": {
      "mlAlgorithm": "RandomForest",
      "epochs": 5,
      "batchSize": 32
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.4.5 训练开始响应确认 (TRAINING_START_RESPONSE_ACK) 🟢
```json
{
  "type": "TRAINING_START_RESPONSE_ACK",
  "id": "server-1704067200000-123467",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "PROCESSED",
    "message": "训练开始响应已处理"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.4.6 停止训练命令 (TRAINING_STOP) 🟢

**消息作用**: 后端向虚拟机发送停止当前训练任务的指令，支持配置是否保存检查点和执行清理操作。

**虚拟机端实现**: 接收此消息后立即停止正在进行的训练过程，根据 saveCheckpoint 参数决定是否保存当前训练状态，根据 cleanup 参数决定是否清理临时文件，并发送相应的响应消息。

**后端实现**: 当需要停止某个训练任务时，构建并发送此消息给对应的虚拟机，等待虚拟机的确认响应，并更新任务状态记录。

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

#### 3.4.7 停止训练确认 (TRAINING_STOP_ACK) 🟢

**消息作用**: 后端对虚拟机发送的训练停止命令进行确认，表示已收到并准备处理停止请求。

**虚拟机端实现**: 无需主动发送此消息，但应处理接收到的确认消息，用于日志记录或状态更新。

**后端实现**: 在发送 TRAINING_STOP 消息后，自动生成并发送此确认消息，表示停止指令已经成功传递给虚拟机。

```json
{
  "type": "TRAINING_STOP_ACK",
  "id": "server-1704067200000-123468",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "COMMAND_SENT",
    "message": "停止指令已发送给VM"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.5 训练进度消息

#### 3.5.1 训练进度报告 (TRAINING_PROGRESS) 🔵

**消息作用**: 虚拟机主动向后端报告当前训练任务的详细进度信息，包括训练轮次、性能指标、资源使用情况等。

**虚拟机端实现**: 在训练过程中定期（例如每完成一个epoch或每隔固定时间间隔）收集并发送当前训练状态，包括准确率、损失值、资源使用率等关键指标。

**后端实现**: 接收并处理虚拟机发送的进度信息，更新数据库中的任务状态，可用于前端展示训练进度图表和系统监控。

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

#### 3.5.2 训练进度查询确认 (TRAINING_PROGRESS_ACK) 🟢

**消息作用**: 后端对进度查询命令的确认回复，表示已收到虚拟机的进度查询请求并准备处理。

**虚拟机端实现**: 无需主动发送此消息，但应处理接收到的确认消息，用于确认后端已收到进度查询请求。

**后端实现**: 当收到虚拟机的进度查询请求时，自动生成并发送此确认消息，表示查询请求已被成功接收。

```json
{
  "type": "TRAINING_PROGRESS_ACK",
  "id": "server-1704067200000-123469",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "QUERY_SENT",
    "message": "进度查询指令已发送给VM"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.5.3 训练进度响应 (TRAINING_PROGRESS_RESPONSE) 🔵

**消息作用**: 虚拟机响应后端的进度查询请求，提供当前训练任务的最新状态和关键指标信息。

**虚拟机端实现**: 当收到后端的进度查询请求时，收集当前训练状态信息并发送此响应消息，包括训练轮次、准确率、损失值等关键数据。

**后端实现**: 接收并处理虚拟机发送的进度响应信息，更新相应的任务记录，并可将信息传递给前端或其他监控系统。

```json
{
  "type": "TRAINING_PROGRESS_RESPONSE",
  "id": "client-1704067200000-123470",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "currentRound": 25,
    "status": "TRAINING",
    "accuracy": 0.88,
    "loss": 0.12,
    "progress": 50.0,
    "message": "训练进度更新"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.5.4 训练进度响应确认 (TRAINING_PROGRESS_RESPONSE_ACK) 🟢

**消息作用**: 后端对虚拟机发送的训练进度响应进行确认，表示已成功接收并处理了进度信息。

**虚拟机端实现**: 无需主动发送此消息，但应处理接收到的确认消息，用于确认进度信息已被后端成功处理。

**后端实现**: 当收到虚拟机的 TRAINING_PROGRESS_RESPONSE 消息后，自动生成并发送此确认消息，表示进度信息已被成功处理和存储。

```json
{
  "type": "TRAINING_PROGRESS_RESPONSE_ACK",
  "id": "server-1704067200000-123471",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "PROCESSED",
    "message": "训练进度响应已处理"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.6 模型传输消息

#### 3.6.1 本地模型上传 (MODEL_UPLOAD) 🔵

**消息作用**: 虚拟机完成本地训练后，将训练得到的模型参数和相关信息上传至后端，用于联邦学习的模型聚合。

**虚拟机端实现**: 在完成本轮本地训练后，序列化模型参数（权重、偏置等），收集训练指标和元数据，然后发送此消息到后端。支持模型压缩以减少传输开销。

**后端实现**: 接收虚拟机上传的模型数据，进行验证和存储到 `vm_round_models` 表中，等待所有参与节点上传完成后触发联邦聚合算法，生成新的全局模型。

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

#### 3.6.2 全局模型下发 (MODEL_DOWNLOAD) 🟢

**消息作用**: 后端完成联邦聚合后向虚拟机推送最新的全局模型参数，用于下一轮本地训练。

**虚拟机端实现**: 接收此消息后验证模型参数的完整性（通过checksum），解压并加载新的全局模型权重到本地模型中，准备开始下一轮本地训练。发送MODEL_UPDATE_ACK确认模型更新完成。

**后端实现**: 当联邦聚合完成生成新的全局模型后，构建并向所有参与的虚拟机推送此消息，包含经过聚合的模型参数。跟踪每个虚拟机的模型更新状态，确保所有节点都成功接收到最新模型。

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

> 说明：v1.4版本优化：模型下发不再包含联邦学习算法信息，只下发经过聚合处理的模型参数和结构。虚拟机无需了解聚合过程，只需接收并使用新模型参数。

#### 3.6.3 模型更新确认 (MODEL_UPDATE_ACK) 🔵

**消息作用**: 虚拟机确认已成功接收并更新全局模型，通知后端可以继续后续的训练流程。

**虚拟机端实现**: 在成功接收和应用全局模型后发送此确认消息，包含更新状态、模型版本、更新时间等信息。如果模型更新失败，应在status字段中标记为FAILED并提供错误详情。

**后端实现**: 接收虚拟机的模型更新确认，更新数据库中对应虚拟机的模型同步状态。当所有参与节点都确认模型更新完成后，可以触发下一轮训练任务的开始。

```json
{
  "type": "MODEL_UPDATE_ACK",
  "id": "client-1704067200000-123472",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "modelVersion": "v1.0.1",
    "updateTime": "2024-01-01T00:00:00.000Z",
    "status": "SUCCESS",
    "message": "全局模型更新已确认"
  },
  "signature": "base64_encoded_signature"
}
```

> 说明：虚拟机收到全局模型更新后，发送此确认消息通知服务器已成功接收并应用新模型。

### 3.7 任务管理消息

#### 3.7.1 通用任务启动 (TASK_START) 🟢

**消息作用**: 后端向虚拟机发送通用任务启动指令，用于启动各种类型的通用任务（非联邦学习特定任务）。

**虚拟机端实现**: 接收任务启动指令，根据taskType执行相应的任务逻辑，发送任务启动确认响应。根据priority调整任务执行优先级，设置timeout监控任务执行时间。

**后端实现**: 构建并发送任务启动消息给指定的虚拟机，包含任务配置信息和执行参数。跟踪任务状态，处理超时和重试逻辑。

```json
{
  "type": "TASK_START",
  "id": "server-1704067200000-123473",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "taskType": "GENERAL",
    "priority": "NORMAL",
    "timeout": 600,
    "config": {
      "maxRetries": 3,
      "requiresAck": true
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.7.2 通用任务启动确认 (TASK_START_ACK) 🟢

**消息作用**: 后端对任务启动消息的确认响应，表示已成功接收虚拟机的任务启动请求并准备处理。

**虚拟机端实现**: 无需主动发送此消息，但应处理接收到的确认消息，用于确认任务启动请求已被后端接收。

**后端实现**: 当收到虚拟机的任务启动请求时，自动生成并发送此确认消息，表示启动请求已被成功接收和处理。

```json
{
  "type": "TASK_START_ACK",
  "id": "server-1704067200000-123474",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "ACKNOWLEDGED",
    "message": "任务启动消息已接收",
    "timestamp": "2024-01-01T00:00:00.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.7.3 联邦学习任务启动 (FEDERATED_TASK_START) 🟢

**消息作用**: 后端向虚拟机发送联邦学习任务启动指令，包含完整的联邦学习配置信息和参与者列表。

**虚拟机端实现**: 接收联邦学习任务配置，准备本地训练环境，初始化联邦学习相关参数。发送任务启动确认，等待后续的训练开始指令。

**后端实现**: 当创建联邦学习任务时，向所有参与的虚拟机发送此消息，包含联邦算法配置、聚合方法、参与者信息等。协调所有参与者的任务启动过程。

```json
{
  "type": "FEDERATED_TASK_START",
  "id": "server-1704067200000-123475",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "federatedAlgorithm": "FEDERATED_AVERAGING",
    "totalRounds": 10,
    "currentRound": 1,
    "participants": ["vm1", "vm2", "vm3"],
    "config": {
      "aggregationMethod": "WEIGHTED_AVERAGE",
      "minParticipants": 2,
      "timeout": 1800
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.7.4 联邦学习任务启动确认 (FEDERATED_TASK_START_ACK) 🔵

**消息作用**: 虚拟机确认已收到联邦学习任务启动指令，表示准备就绪可以开始参与联邦学习过程。

**虚拟机端实现**: 在收到 FEDERATED_TASK_START 消息并完成本地准备工作后，发送此确认消息表示已准备好参与联邦学习任务。包含任务ID和虚拟机状态信息。

**后端实现**: 接收所有参与虚拟机的任务启动确认，统计确认状态。当所有必要的参与者都确认就绪后，可以开始启动联邦学习的第一轮训练。

```json
{
  "type": "FEDERATED_TASK_START_ACK",
  "id": "client-1704067200000-123476",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "ACKNOWLEDGED",
    "taskId": "fedtask-123456",
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "message": "联邦学习任务启动消息已接收",
    "timestamp": "2024-01-01T00:00:00.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.8 状态查询消息

#### 3.8.1 状态查询请求 (STATUS_QUERY) 🟢

**消息作用**: 后端向虚拟机发送状态查询请求，获取虚拟机的详细运行状态、资源使用情况和系统信息。

**虚拟机端实现**: 接收查询请求后收集相应的状态信息，根据queryType和include参数决定收集的信息范围，在timeout时间内发送STATUS_RESPONSE响应。

**后端实现**: 当需要监控虚拟机状态或进行系统诊断时发送此查询请求，指定需要获取的信息类型和超时时间，用于系统监控和负载均衡决策。

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

#### 3.8.2 状态查询响应 (STATUS_RESPONSE) 🔵

**消息作用**: 虚拟机响应后端的状态查询请求，提供详细的系统状态、资源使用情况、网络信息和进程信息。

**虚拟机端实现**: 收到STATUS_QUERY后，收集系统运行状态、CPU、内存、磁盘、GPU使用率，网络配置信息，进程统计和系统基本信息，然后发送此响应消息。

**后端实现**: 接收虚拟机的状态响应，解析并存储状态信息到数据库或监控系统中，用于生成监控报告、负载均衡决策和系统健康检查。

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

#### 3.8.3 批量状态查询请求 (BATCH_STATUS_QUERY) 🟢

**消息作用**: 后端向多个虚拟机同时发送状态查询请求，用于获取集群整体状态信息和统计数据。

**虚拟机端实现**: 接收批量查询请求，收集本机状态信息，在指定时间内响应查询。参与批量统计分析，提供必要的系统指标。

**后端实现**: 当需要进行集群监控或生成整体报告时发送批量查询，收集所有虚拟机的状态数据，进行汇总分析和统计计算。

```json
{
  "type": "BATCH_STATUS_QUERY",
  "id": "cmd-1704067200000-123480",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "queryId": "batch-query-001",
    "targets": ["vm-001", "vm-002", "vm-003"],
    "queryType": "SUMMARY",
    "includeMetrics": ["cpu", "memory", "network"],
    "timeout": 15
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.8.4 批量状态查询响应 (BATCH_STATUS_RESPONSE) 🟢

**消息作用**: 后端汇总多个虚拟机的状态查询结果，提供集群整体状态摘要和统计信息。

**虚拟机端实现**: 无需主动发送此消息，但应理解此消息格式用于接收集群状态汇总信息。

**后端实现**: 收集所有虚拟机的状态响应后，进行数据汇总和统计分析，生成集群整体状态报告并发送给查询发起者或监控系统。

```json
{
  "type": "BATCH_STATUS_RESPONSE",
  "id": "server-1704067200000-123481",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "query-initiator",
  "data": {
    "queryId": "batch-query-001",
    "summary": {
      "totalVms": 3,
      "onlineVms": 2,
      "offlineVms": 1,
      "averageCpuUsage": 35.7,
      "averageMemoryUsage": 65.3,
      "totalTrainingTasks": 2
    },
    "details": [
      {
        "vmId": "vm-001",
        "status": "RUNNING",
        "cpu": 25.5,
        "memory": 60.2
      },
      {
        "vmId": "vm-002",
        "status": "RUNNING",
        "cpu": 45.8,
        "memory": 70.4
      },
      {
        "vmId": "vm-003",
        "status": "OFFLINE",
        "lastSeen": "2024-01-01T00:00:00.000Z"
      }
    ]
  },
  "signature": "base64_encoded_signature"
}
```

### 3.9 联邦学习聚合消息 (v1.4新增)

#### 3.9.1 梯度上传 (GRADIENT_UPLOAD) 🔵

**消息作用**: 虚拟机将本轮训练产生的梯度信息上传至后端，用于联邦学习的梯度聚合算法。

**虚拟机端实现**: 本地训练完成后，提取模型梯度或参数更新信息，序列化并上传到后端。支持梯度压缩和差分隐私处理，确保数据传输效率和隐私保护。

**后端实现**: 接收虚拟机上传的梯度数据，进行验证和存储。当收集到足够数量的梯度后，触发联邦聚合算法执行，生成全局模型更新。

```json
{
  "type": "GRADIENT_UPLOAD",
  "id": "client-1704067200000-130001",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 5,
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
      "localAccuracy": 0.87
    },
    "compressionMethod": "gzip",
    "privacyLevel": "differential"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.2 梯度上传确认 (GRADIENT_UPLOAD_ACK) 🟢

**消息作用**: 后端确认已成功接收虚拟机上传的梯度数据，通知虚拟机可以继续后续操作。

**虚拟机端实现**: 接收此确认消息，了解梯度上传状态。如果状态为失败，可根据错误信息重新上传或调整参数。

**后端实现**: 在成功接收和验证梯度数据后发送此确认消息。包含接收状态、数据验证结果和下一步操作指示。

```json
{
  "type": "GRADIENT_UPLOAD_ACK",
  "id": "server-1704067200000-130001",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "SUCCESS",
    "taskId": "fedtask-123456",
    "roundNumber": 5,
    "receivedBytes": 2048,
    "validationResult": "PASSED",
    "nextStep": "WAIT_AGGREGATION"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.3 全局模型广播 (GLOBAL_MODEL_BROADCAST) 🟢

**消息作用**: 后端完成联邦聚合后，向所有参与的虚拟机广播最新的全局模型参数和聚合结果。

**虚拟机端实现**: 接收全局模型广播，验证模型参数完整性，更新本地全局模型副本。为下一轮本地训练做准备，发送确认消息。

**后端实现**: 联邦聚合算法完成后，构建全局模型广播消息发送给所有参与节点。包含聚合后的模型参数、聚合统计信息和下轮训练配置。

```json
{
  "type": "GLOBAL_MODEL_BROADCAST",
  "id": "server-1704067200000-130010",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 6,
    "globalModel": {
      "weights": {
        "layer1": [0.125, -0.087, 0.234],
        "layer2": [0.156, -0.203, 0.298]
      },
      "biases": {
        "layer1": [0.045],
        "layer2": [0.078]
      }
    },
    "aggregationInfo": {
      "algorithm": "FEDERATED_AVERAGING",
      "participantCount": 5,
      "convergenceScore": 0.92
    },
    "nextRoundConfig": {
      "startTime": "2024-01-01T00:05:00.000Z",
      "epochs": 3,
      "learningRate": 0.01
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.4 全局模型广播确认 (GLOBAL_MODEL_BROADCAST_ACK) 🔵

**消息作用**: 虚拟机确认已成功接收全局模型广播，表示准备就绪可以开始下一轮训练。

**虚拟机端实现**: 在成功接收和应用全局模型后发送此确认消息。包含模型更新状态、本地验证结果和准备就绪状态。

**后端实现**: 接收虚拟机的广播确认，统计确认状态。当所有参与者都确认接收后，可以启动下一轮联邦学习训练。

```json
{
  "type": "GLOBAL_MODEL_BROADCAST_ACK",
  "id": "client-1704067200000-130010",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "SUCCESS",
    "taskId": "fedtask-123456",
    "roundNumber": 6,
    "modelUpdateTime": "2024-01-01T00:00:00.000Z",
    "validationAccuracy": 0.89,
    "readyForNextRound": true
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.5 聚合开始通知 (AGGREGATION_START) 🟢

**消息作用**: 后端通知虚拟机联邦聚合过程即将开始，虚拟机应停止发送新的梯度数据并等待聚合结果。

**虚拟机端实现**: 接收此通知后停止发送模型更新，进入等待聚合状态。可以进行本地资源清理或准备下轮训练环境。

**后端实现**: 当收集到足够的梯度数据并准备开始聚合时发送此通知。通知所有参与者聚合即将开始，包含预计完成时间和聚合方法信息。

```json
{
  "type": "AGGREGATION_START",
  "id": "server-1704067200000-130020",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 5,
    "aggregationMethod": "FEDERATED_AVERAGING",
    "participantCount": 5,
    "estimatedDuration": 120,
    "startTime": "2024-01-01T00:00:00.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.6 聚合开始确认 (AGGREGATION_START_ACK) 🔵

**消息作用**: 虚拟机确认已收到聚合开始通知，表示已进入等待聚合完成状态。

**虚拟机端实现**: 收到聚合开始通知后发送此确认消息，表明已停止发送梯度数据，准备接收聚合结果。

**后端实现**: 接收虚拟机的聚合开始确认，确保所有参与者都已准备好进入聚合阶段。

```json
{
  "type": "AGGREGATION_START_ACK",
  "id": "client-1704067200000-130020",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "ACKNOWLEDGED",
    "taskId": "fedtask-123456",
    "roundNumber": 5,
    "waitingForAggregation": true
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.7 聚合完成通知 (AGGREGATION_COMPLETE) 🟢

**消息作用**: 后端通知虚拟机联邦聚合过程已完成，提供聚合结果摘要信息。

**虚拟机端实现**: 接收聚合完成通知，了解本轮聚合的结果和统计信息。准备接收新的全局模型参数。

**后端实现**: 联邦聚合算法执行完成后发送此通知，包含聚合统计信息、收敛情况和全局模型性能指标。

```json
{
  "type": "AGGREGATION_COMPLETE",
  "id": "server-1704067200000-130030",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 5,
    "aggregationResults": {
      "participantCount": 5,
      "convergenceScore": 0.92,
      "globalAccuracy": 0.88,
      "globalLoss": 0.15
    },
    "duration": 95,
    "nextStep": "GLOBAL_MODEL_BROADCAST"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.8 聚合完成确认 (AGGREGATION_COMPLETE_ACK) 🔵

**消息作用**: 虚拟机确认已收到聚合完成通知，表示准备接收新的全局模型。

**虚拟机端实现**: 收到聚合完成通知后发送此确认消息，表明已了解聚合结果，准备接收全局模型更新。

**后端实现**: 接收虚拟机的聚合完成确认，确保通知已成功传达给所有参与者。

```json
{
  "type": "AGGREGATION_COMPLETE_ACK",
  "id": "client-1704067200000-130030",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "ACKNOWLEDGED",
    "taskId": "fedtask-123456",
    "roundNumber": 5,
    "readyForGlobalModel": true
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.9 轮次开始通知 (ROUND_START) 🟢

**消息作用**: 后端通知虚拟机新的联邦学习训练轮次即将开始，提供轮次配置和训练参数。

**虚拟机端实现**: 接收轮次开始通知，根据提供的配置参数准备本地训练环境。设置训练参数，初始化数据加载器，准备开始本轮训练。

**后端实现**: 当准备启动新的联邦学习轮次时发送此通知，包含轮次编号、训练配置、超参数设置和目标性能指标。

```json
{
  "type": "ROUND_START",
  "id": "server-1704067200000-130040",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 6,
    "trainingConfig": {
      "epochs": 3,
      "batchSize": 32,
      "learningRate": 0.01,
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

#### 3.9.10 轮次开始确认 (ROUND_START_ACK) 🔵

**消息作用**: 虚拟机确认已收到轮次开始通知，表示准备就绪可以开始本轮训练。

**虚拟机端实现**: 收到轮次开始通知并完成本地准备后发送此确认消息。包含本地配置状态和预计训练开始时间。

**后端实现**: 接收虚拟机的轮次开始确认，统计参与者准备状态。当足够数量的参与者确认就绪后，可以正式开始训练轮次。

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
    "localConfigStatus": "INITIALIZED",
    "estimatedStartTime": "2024-01-01T00:00:30.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.11 轮次完成通知 (ROUND_COMPLETE) 🟢

**消息作用**: 后端通知虚拟机当前联邦学习训练轮次已完成，提供轮次结果摘要和下轮计划。

**虚拟机端实现**: 接收轮次完成通知，了解本轮训练的整体结果。清理本轮训练资源，为下轮训练或任务结束做准备。

**后端实现**: 当前轮次的所有训练和聚合过程完成后发送此通知。包含轮次统计、全局性能指标和下轮训练计划。

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
      "convergenceImprovement": 0.03
    },
    "nextRound": {
      "planned": true,
      "roundNumber": 7,
      "scheduledStart": "2024-01-01T00:10:00.000Z"
    },
    "duration": 450
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.12 轮次完成确认 (ROUND_COMPLETE_ACK) 🔵

**消息作用**: 虚拟机确认已收到轮次完成通知，表示已了解本轮结果并准备下轮训练或任务结束。

**虚拟机端实现**: 收到轮次完成通知后发送此确认消息。清理本轮资源，更新本地统计信息，为下轮训练做准备。

**后端实现**: 接收虚拟机的轮次完成确认，确保所有参与者都已了解轮次结果。用于协调下轮训练的开始时机。

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

#### 3.9.13 模型类型协商 (MODEL_TYPE_NEGOTIATION) 🟢

**消息作用**: 后端与虚拟机协商联邦学习任务中使用的模型类型和架构参数。

**虚拟机端实现**: 接收模型类型协商请求，检查本地支持的模型类型和框架，返回兼容性信息和建议配置。

**后端实现**: 在开始联邦学习任务前发送模型类型协商请求，收集所有参与者的模型支持情况，确定最佳的统一模型配置。

```json
{
  "type": "MODEL_TYPE_NEGOTIATION",
  "id": "server-1704067200000-130060",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "proposedModelTypes": ["RandomForest", "NeuralNetwork", "SVM"],
    "frameworks": ["sklearn", "pytorch", "tensorflow"],
    "modelRequirements": {
      "inputDimension": 784,
      "outputClasses": 10,
      "maxComplexity": "MEDIUM"
    },
    "negotiationTimeout": 60
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.14 模型类型协商确认 (MODEL_TYPE_NEGOTIATION_ACK) 🔵

**消息作用**: 虚拟机响应后端的模型类型协商请求，提供本地支持的模型类型和建议配置。

**虚拟机端实现**: 检查本地机器学习环境，评估提议的模型类型支持情况，返回兼容性报告和性能预估。

**后端实现**: 接收虚拟机的协商响应，汇总所有参与者的模型支持情况，确定最终的统一模型配置方案。

```json
{
  "type": "MODEL_TYPE_NEGOTIATION_ACK",
  "id": "client-1704067200000-130060",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "supportedModels": ["RandomForest", "NeuralNetwork"],
    "recommendedModel": "RandomForest",
    "supportedFrameworks": ["sklearn", "pytorch"],
    "performanceEstimate": {
      "RandomForest": "HIGH",
      "NeuralNetwork": "MEDIUM"
    },
    "resourceConstraints": {
      "maxMemoryUsage": "4GB",
      "maxTrainingTime": 300
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.15 算法配置 (ALGORITHM_CONFIG) 🟢

**消息作用**: 后端向虚拟机发送联邦学习算法的详细配置参数和超参数设置。

**虚拟机端实现**: 接收算法配置，根据配置参数调整本地训练算法设置。验证配置兼容性，如有问题及时反馈。

**后端实现**: 在开始联邦学习任务前发送统一的算法配置，确保所有参与者使用相同的算法参数和训练策略。

```json
{
  "type": "ALGORITHM_CONFIG",
  "id": "server-1704067200000-130070",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "algorithm": "FEDERATED_AVERAGING",
    "hyperparameters": {
      "learningRate": 0.01,
      "batchSize": 32,
      "localEpochs": 5,
      "momentum": 0.9
    },
    "aggregationConfig": {
      "method": "WEIGHTED_AVERAGE",
      "minParticipants": 3,
      "convergenceThreshold": 0.001
    },
    "privacyConfig": {
      "differentialPrivacy": true,
      "epsilon": 1.0,
      "delta": 0.0001
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.16 算法配置确认 (ALGORITHM_CONFIG_ACK) 🔵

**消息作用**: 虚拟机确认已收到并应用算法配置，表示本地训练环境已按配置准备就绪。

**虚拟机端实现**: 收到算法配置后验证配置参数，应用到本地训练环境，发送配置应用结果和状态确认。

**后端实现**: 接收虚拟机的算法配置确认，确保所有参与者都已正确应用算法配置。统计确认状态决定是否开始训练。

```json
{
  "type": "ALGORITHM_CONFIG_ACK",
  "id": "client-1704067200000-130070",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "SUCCESS",
    "taskId": "fedtask-123456",
    "configApplied": true,
    "validationResults": {
      "hyperparametersValid": true,
      "aggregationConfigValid": true,
      "privacyConfigValid": true
    },
    "estimatedReadyTime": "2024-01-01T00:00:30.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.17 梯度上传准备 (GRADIENT_UPLOAD_PREPARE) 🟢

**消息作用**: 后端通知虚拟机准备上传梯度，提供上传配置和优化建议。

**虚拟机端实现**: 接收上传准备通知，根据配置优化梯度数据的格式和压缩方式。准备上传通道，预估上传时间和资源需求。

**后端实现**: 在虚拟机本地训练接近完成时发送此通知，帮助优化梯度上传过程。提供网络优化建议和上传策略。

```json
{
  "type": "GRADIENT_UPLOAD_PREPARE",
  "id": "server-1704067200000-130080",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 7,
    "uploadConfig": {
      "compressionMethod": "gzip",
      "batchSize": 1024,
      "maxUploadTime": 120
    },
    "optimizationHints": {
      "networkOptimization": true,
      "prioritizeAccuracy": false
    },
    "expectedUploadStart": "2024-01-01T00:02:00.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.18 梯度上传准备确认 (GRADIENT_UPLOAD_PREPARE_ACK) 🔵

**消息作用**: 虚拟机确认已收到梯度上传准备通知，表示已优化上传配置并准备就绪。

**虚拟机端实现**: 收到上传准备通知后应用优化配置，测试上传通道，发送准备就绪确认和预估信息。

**后端实现**: 接收虚拟机的上传准备确认，了解各节点的准备状态和预估上传时间，优化全局上传调度。

```json
{
  "type": "GRADIENT_UPLOAD_PREPARE_ACK",
  "id": "client-1704067200000-130080",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "READY",
    "taskId": "fedtask-123456",
    "roundNumber": 7,
    "uploadEstimate": {
      "dataSize": 2048,
      "estimatedTime": 30,
      "networkSpeed": 1024
    },
    "optimizationApplied": true,
    "readyToUpload": true
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.19 聚合进度通知 (AGGREGATION_NOTIFICATION) 🟢

**消息作用**: 后端向虚拟机发送联邦聚合的实时进度信息和状态更新。

**虚拟机端实现**: 接收聚合进度通知，更新本地状态显示。可根据进度信息调整资源使用或准备下一步操作。

**后端实现**: 在联邦聚合执行过程中定期发送进度通知，让参与者了解聚合状态。包含进度百分比、预计完成时间和中间结果。

```json
{
  "type": "AGGREGATION_NOTIFICATION",
  "id": "server-1704067200000-130090",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "roundNumber": 7,
    "progress": {
      "percentage": 65,
      "currentStep": "GRADIENT_AGGREGATION",
      "totalSteps": 4
    },
    "intermediateResults": {
      "processedParticipants": 3,
      "totalParticipants": 5,
      "convergenceScore": 0.87
    },
    "estimatedCompletion": "2024-01-01T00:03:00.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.20 策略切换通知 (STRATEGY_SWITCH_NOTIFICATION) 🟢

**消息作用**: 后端通知虚拟机联邦学习策略即将发生变更，提供新策略的配置信息。

**虚拟机端实现**: 接收策略切换通知，评估新策略对本地环境的影响。调整本地配置以适应新策略，发送切换确认。

**后端实现**: 当系统决定切换联邦学习策略时发送此通知。可能由于性能优化、收敛问题或资源变化触发策略调整。

```json
{
  "type": "STRATEGY_SWITCH_NOTIFICATION",
  "id": "server-1704067200000-130100",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "broadcast",
  "data": {
    "taskId": "fedtask-123456",
    "currentStrategy": "FEDERATED_AVERAGING",
    "newStrategy": "FEDERATED_PROXIMAL",
    "switchReason": "CONVERGENCE_OPTIMIZATION",
    "newConfig": {
      "proximalTerm": 0.1,
      "adaptiveLearningRate": true,
      "regularizationStrength": 0.01
    },
    "effectiveRound": 8,
    "migrationGuidance": "GRADUAL_TRANSITION"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.9.21 策略切换确认 (STRATEGY_SWITCH_ACK) 🔵

**消息作用**: 虚拟机确认已收到策略切换通知，表示已准备好适应新的联邦学习策略。

**虚拟机端实现**: 收到策略切换通知后评估本地适配能力，调整算法配置，发送切换准备就绪确认。

**后端实现**: 接收虚拟机的策略切换确认，统计所有参与者的适配状态。当所有节点都确认就绪后，正式启用新策略。

```json
{
  "type": "STRATEGY_SWITCH_ACK",
  "id": "client-1704067200000-130100",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "status": "READY",
    "taskId": "fedtask-123456",
    "newStrategy": "FEDERATED_PROXIMAL",
    "adaptationResult": {
      "configurationUpdated": true,
      "performanceEstimate": "GOOD",
      "resourceImpact": "MINIMAL"
    },
    "readyForSwitch": true,
    "estimatedSwitchTime": "2024-01-01T00:01:00.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.10 错误和状态消息

#### 3.10.1 错误报告 (ERROR)
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

### 3.11 训练数据同步消息

> 自v1.1起，训练数据采用宽表+JSON存储：数据集元信息写入 `training_dataset`，数据行写入 `training_dataset_row`（`row_data` JSON，`dataset_id` 外键）。以下消息用于通过WebSocket进行数据集创建与增量同步。

#### 3.11.1 创建数据集 (DATASET_CREATE)
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

#### 3.11.2 追加数据行（批量）(DATASET_APPEND_ROWS)
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

#### 3.11.3 完成数据集上传 (DATASET_COMPLETE)
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

#### 3.11.4 数据集状态查询 (DATASET_STATUS_QUERY / DATASET_STATUS_RESPONSE)
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

#### 3.11.5 删除数据集 (DATASET_DELETE)
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

## 3.12 服务端通知消息 (v1.4新增)

> **重要说明**: 本节定义的是服务端主动发送的**通知消息**，与前述的**ACK响应消息**不同：
>
> - **ACK响应消息**: 对客户端请求的直接确认，遵循请求-响应模式，客户端期待接收
> - **通知消息**: 服务端主动推送的事件通知，用于实时状态同步，客户端可选择性处理
>
> 例如：`DATASET_CREATE` → `DATASET_CREATE_ACK` (ACK响应) + `DATASET_CREATE_NOTIFICATION` (完成通知)

### 3.12.1 数据集创建完成通知 (DATASET_CREATE_NOTIFICATION) 🟢
```json
{
  "type": "DATASET_CREATE_NOTIFICATION",
  "id": "server-1704067200000-200010",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd",
    "description": "水声传播特征数据集",
    "dataType": "ACOUSTIC",
    "status": "READY"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.12.2 数据行添加完成通知 (DATASET_APPEND_ROWS_NOTIFICATION) 🟢
```json
{
  "type": "DATASET_APPEND_ROWS_NOTIFICATION",
  "id": "server-1704067200000-200011",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd",
    "rowsAdded": 2,
    "sampleData": { "f1": 0.12, "f2": 3.4, "label": 1 }
  },
  "signature": "base64_encoded_signature"
}
```

### 3.12.3 数据集完成通知 (DATASET_COMPLETE_NOTIFICATION) 🟢
```json
{
  "type": "DATASET_COMPLETE_NOTIFICATION",
  "id": "server-1704067200000-200012",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "datasetId": "dset-1234567890abcdef1234567890abcd",
    "totalRows": 1000,
    "status": "READY"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.12.4 训练开始指令通知 (TRAINING_START_COMMAND_NOTIFICATION) 🟢
```json
{
  "type": "TRAINING_START_COMMAND_NOTIFICATION",
  "id": "server-1704067200000-200013",
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
    "message": "请开始本地ML训练任务"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.12.5 训练开始成功通知 (TRAINING_START_NOTIFICATION) 🟢
```json
{
  "type": "TRAINING_START_NOTIFICATION",
  "id": "server-1704067200000-200014",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "status": "RUNNING",
    "message": "训练已成功开始"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.12.6 训练开始失败通知 (TRAINING_START_FAILURE_NOTIFICATION) 🟢
```json
{
  "type": "TRAINING_START_FAILURE_NOTIFICATION",
  "id": "server-1704067200000-200015",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "status": "FAILED",
    "error": "模型初始化失败：参数错误"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.12.7 训练停止指令通知 (TRAINING_STOP_COMMAND_NOTIFICATION) 🟢
```json
{
  "type": "TRAINING_STOP_COMMAND_NOTIFICATION",
  "id": "server-1704067200000-200016",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "message": "请停止训练任务"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.12.8 训练进度查询指令通知 (TRAINING_PROGRESS_QUERY_NOTIFICATION) 🟢
```json
{
  "type": "TRAINING_PROGRESS_QUERY_NOTIFICATION",
  "id": "server-1704067200000-200017",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "message": "请报告训练进度"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.12.9 训练进度更新通知 (TRAINING_PROGRESS_UPDATE_NOTIFICATION) 🟢
```json
{
  "type": "TRAINING_PROGRESS_UPDATE_NOTIFICATION",
  "id": "server-1704067200000-200018",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "currentRound": 25,
    "status": "RUNNING",
    "accuracy": 0.88,
    "loss": 0.12,
    "message": "训练进度已更新"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.12.10 模型下载通知 (MODEL_DOWNLOAD_NOTIFICATION) 🟢
```json
{
  "type": "MODEL_DOWNLOAD_NOTIFICATION",
  "id": "server-1704067200000-200019",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "parameters": {
      "model": {
        "framework": "pytorch",
        "format": "state_dict",
        "weights": {
          "shape": [784, 256, 128, 10],
          "dtype": "float32",
          "checksum": "sha256:global_model_1704067200000"
        }
      },
      "training": {
        "algorithm": "RandomForest",
        "samples": 1000
      }
    },
    "compression": "gzip",
    "downloadTime": "2024-01-01T00:00:00.000Z",
    "message": "全局模型下载数据"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.12.11 状态更新通知 (STATUS_UPDATE_NOTIFICATION) 🟢
```json
{
  "type": "STATUS_UPDATE_NOTIFICATION",
  "id": "server-1704067200000-200020",
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
    "lastUpdate": "2024-01-01T00:00:00.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

> **说明**: v1.4版本新增了服务端通知消息类型，这些消息由服务器主动发送给客户端，用于实时通知状态变更、任务进展等信息。客户端应注册相应的消息处理器来处理这些通知。

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

### 4.4 通知消息处理流程 (v1.4新增)

通知消息处理遵循**发布-订阅模式**，与请求-响应模式不同：

1. **事件触发**: 服务器端业务逻辑触发特定事件（如数据集创建完成）
2. **通知广播**: 服务器向相关的虚拟机或前端客户端广播通知消息
3. **可选处理**: 客户端可选择性处理通知消息（不需要发送ACK）
4. **状态同步**: 客户端根据通知更新本地状态或UI

#### 4.4.1 双重响应模式

许多操作采用**双重响应模式**：ACK确认 + 事件通知

```
客户端请求: DATASET_CREATE
    ↓
服务器处理: 数据库操作 + 业务逻辑
    ↓
双重响应:
    1. DATASET_CREATE_ACK → 告诉请求方"已接收处理"
    2. DATASET_CREATE_NOTIFICATION → 通知所有订阅者"数据集已创建"
```

#### 4.4.2 消息路由

- **ACK消息**: 点对点发送给请求发起者
- **通知消息**: 广播发送给订阅该类型事件的所有客户端
- **指令消息**: 定向发送给特定虚拟机

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