# 水声联邦学习系统 WebSocket 通信协议

## 1. 概述

本文档定义了水声联邦学习系统的WebSocket通信协议，用于实现服务器与虚拟机之间的实时双向通信，支持虚拟机控制、学习控制、状态监控等功能。

### 1.1 基础信息
- **WebSocket URL**: `ws://localhost:8080/ws/vm/{vmId}`
- **协议版本**: v1.0
- **认证方式**: JWT Token（可选）
- **数据格式**: JSON
- **编码**: UTF-8

### 1.2 连接参数
- `vmId`: 虚拟机唯一标识（必需）
- `token`: JWT认证令牌（可选）
- `version`: 客户端版本号（可选）

### 1.3 连接示例
```javascript
// 基础连接
const ws = new WebSocket('ws://localhost:8080/ws/vm/vm-001');

// 带认证的连接
const ws = new WebSocket('ws://localhost:8080/ws/vm/vm-001?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...');
```

## 2. 消息格式

### 2.1 标准消息格式
所有WebSocket消息都采用JSON格式，包含以下字段：

```json
{
  "type": "MESSAGE_TYPE",
  "id": "unique_message_id",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {},
  "signature": "base64_encoded_signature"
}
```

### 2.2 字段说明
- `type`: 消息类型（必需）
- `id`: 消息唯一标识（必需）
- `timestamp`: 消息时间戳（必需）
- `vmId`: 虚拟机ID（必需）
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
  "vmId": "vm-001",
  "data": {
    "version": "1.0.0",
    "capabilities": ["FEDAVG", "FEDPROX", "FEDNOVA", "SCAFFOLD"],
    "systemInfo": {
      "os": "Ubuntu 20.04",
      "python": "3.8.10",
      "memory": "4GB",
      "cpu": "Intel Xeon E5-2680",
      "gpu": "NVIDIA Tesla V100"
    },
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
  "vmId": "vm-001",
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

#### 3.1.3 连接断开通知 (DISCONNECT)
```json
{
  "type": "DISCONNECT",
  "id": "client-1704067200000-123457",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "reason": "SHUTDOWN",
    "duration": 3600,
    "lastActivity": "2024-01-01T00:00:00.000Z"
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
  "vmId": "vm-001",
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
  "vmId": "vm-001",
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
  "vmId": "vm-001",
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
  "vmId": "vm-001",
  "data": {
    "force": false,
    "timeout": 60,
    "saveState": true
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.3.3 重启虚拟机命令 (VM_RESTART)
```json
{
  "type": "VM_RESTART",
  "id": "cmd-1704067200000-123461",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "timeout": 300,
    "graceful": true,
    "config": {
      "memory": "4GB",
      "cpu": "4cores"
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.3.4 虚拟机状态更新 (VM_STATUS_UPDATE)
```json
{
  "type": "VM_STATUS_UPDATE",
  "id": "client-1704067200000-123462",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
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
      "port": 22
    },
    "processes": {
      "total": 150,
      "active": 25,
      "system": 10,
      "user": 15
    }
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
  "vmId": "vm-001",
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
  "vmId": "vm-001",
  "data": {
    "taskId": "task-123456",
    "reason": "MANUAL_STOP",
    "saveCheckpoint": true,
    "cleanup": true
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.4.3 暂停训练命令 (TRAINING_PAUSE)
```json
{
  "type": "TRAINING_PAUSE",
  "id": "cmd-1704067200000-123465",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "taskId": "task-123456",
    "saveState": true,
    "reason": "PAUSE_REQUESTED"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.4.4 恢复训练命令 (TRAINING_RESUME)
```json
{
  "type": "TRAINING_RESUME",
  "id": "cmd-1704067200000-123466",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "taskId": "task-123456",
    "checkpointPath": "/checkpoints/task-123456/round-25.pth",
    "resumeFrom": 25
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.4.5 算法切换命令 (ALGORITHM_SWITCH)
```json
{
  "type": "ALGORITHM_SWITCH",
  "id": "cmd-1704067200000-123467",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "taskId": "task-123456",
    "newAlgorithm": "FEDPROX",
    "config": {
      "mu": 0.001,
      "batchSize": 32,
      "learningRate": 0.001,
      "epochsPerRound": 5
    },
    "reason": "PERFORMANCE_OPTIMIZATION",
    "restartTraining": false
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
  "vmId": "vm-001",
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

#### 3.5.2 训练完成报告 (TRAINING_COMPLETE)
```json
{
  "type": "TRAINING_COMPLETE",
  "id": "client-1704067200000-123469",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "taskId": "task-123456",
    "round": 25,
    "finalMetrics": {
      "accuracy": 0.88,
      "loss": 0.12,
      "valAccuracy": 0.85,
      "valLoss": 0.15,
      "precision": 0.89,
      "recall": 0.86,
      "f1Score": 0.87
    },
    "modelPath": "/models/local_model_round_25.pth",
    "modelSize": 1024000,
    "trainingTime": 1800,
    "checkpointPath": "/checkpoints/task-123456/round-25.pth"
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
  "vmId": "vm-001",
  "data": {
    "taskId": "task-123456",
    "round": 25,
    "modelType": "LOCAL",
    "modelPath": "/models/local_model_round_25.pth",
    "modelSize": 1024000,
    "parameters": {
      "layers": 3,
      "neurons": [784, 256, 128, 10],
      "activation": "relu",
      "optimizer": "adam",
      "learningRate": 0.001
    },
    "metrics": {
      "accuracy": 0.88,
      "loss": 0.12,
      "valAccuracy": 0.85,
      "valLoss": 0.15
    },
    "uploadUrl": "/api/v1/model/upload",
    "checksum": "sha256:abc123...",
    "compression": "gzip"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.6.2 全局模型下载 (MODEL_DOWNLOAD)
```json
{
  "type": "MODEL_DOWNLOAD",
  "id": "server-1704067200000-123471",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "taskId": "task-123456",
    "round": 26,
    "modelType": "GLOBAL",
    "modelPath": "/models/global_model_round_26.pth",
    "modelSize": 1024000,
    "parameters": {
      "layers": 3,
      "neurons": [784, 256, 128, 10],
      "activation": "relu",
      "optimizer": "adam",
      "learningRate": 0.001
    },
    "aggregationMethod": "FEDAVG",
    "downloadUrl": "/api/v1/model/download/model-123457",
    "checksum": "sha256:def456...",
    "compression": "gzip"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.7 错误和状态消息

#### 3.7.1 错误报告 (ERROR)
```json
{
  "type": "ERROR",
  "id": "client-1704067200000-123472",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
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

#### 3.7.2 状态更新 (STATUS_UPDATE)
```json
{
  "type": "STATUS_UPDATE",
  "id": "client-1704067200000-123473",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "status": "TRAINING",
    "currentTask": "task-123456",
    "currentRound": 25,
    "resourceUsage": {
      "cpu": 85.5,
      "memory": 75.2,
      "disk": 45.8,
      "gpu": 95.8
    },
    "network": {
      "uploadSpeed": 1024,
      "downloadSpeed": 2048,
      "latency": 50
    },
    "processes": {
      "total": 150,
      "active": 25,
      "training": 1
    },
    "lastActivity": "2024-01-01T00:00:00.000Z"
  },
  "signature": "base64_encoded_signature"
}
```

### 3.8 系统控制消息

#### 3.8.1 系统重启命令 (SYSTEM_RESTART)
```json
{
  "type": "SYSTEM_RESTART",
  "id": "cmd-1704067200000-123474",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "timeout": 300,
    "saveState": true,
    "reason": "MAINTENANCE"
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.8.2 配置更新命令 (CONFIG_UPDATE)
```json
{
  "type": "CONFIG_UPDATE",
  "id": "cmd-1704067200000-123475",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "configType": "TRAINING",
    "config": {
      "maxMemory": "8GB",
      "maxCpu": "8cores",
      "heartbeatInterval": 30,
      "logLevel": "INFO"
    },
    "restartRequired": false
  },
  "signature": "base64_encoded_signature"
}
```

## 4. 消息处理流程

### 4.1 连接建立流程
1. **客户端连接**: 客户端连接到WebSocket URL
2. **发送连接请求**: 客户端发送CONNECT消息
3. **身份验证**: 服务器验证虚拟机身份和权限
4. **连接确认**: 服务器发送CONNECT_ACK确认连接
5. **开始心跳**: 启动心跳机制保持连接活跃

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

## 5. 安全机制

### 5.1 消息签名
所有消息都包含数字签名，使用RSA算法：
```json
{
  "signature": "base64_encoded_signature"
}
```

#### 5.1.1 签名生成
```python
import hashlib
import hmac
import base64

def generate_signature(message, private_key):
    # 创建消息摘要
    message_str = json.dumps(message, sort_keys=True)
    digest = hashlib.sha256(message_str.encode()).digest()
    
    # 使用私钥签名
    signature = private_key.sign(digest, padding.PKCS1v15(), hashes.SHA256())
    
    # 返回base64编码的签名
    return base64.b64encode(signature).decode()
```

#### 5.1.2 签名验证
```python
def verify_signature(message, signature, public_key):
    try:
        # 解码签名
        signature_bytes = base64.b64decode(signature)
        
        # 创建消息摘要
        message_str = json.dumps(message, sort_keys=True)
        digest = hashlib.sha256(message_str.encode()).digest()
        
        # 验证签名
        public_key.verify(signature_bytes, digest, padding.PKCS1v15(), hashes.SHA256())
        return True
    except Exception:
        return False
```

### 5.2 消息验证
- **时间戳验证**: 验证消息时间戳，防止重放攻击
- **签名验证**: 验证消息签名，确保消息完整性
- **身份验证**: 验证虚拟机身份，确保权限
- **消息大小限制**: 限制单条消息大小（默认10MB）

### 5.3 加密传输
- **WSS协议**: 使用WSS（WebSocket Secure）进行加密传输
- **TLS版本**: 支持TLS 1.2及以上版本
- **证书验证**: 验证服务器SSL证书
- **密码套件**: 使用强密码套件

### 5.4 访问控制
- **虚拟机隔离**: 每个虚拟机只能访问自己的数据
- **权限验证**: 验证虚拟机对资源的访问权限
- **操作审计**: 记录所有操作日志
- **异常检测**: 检测异常行为并采取相应措施

## 6. 错误处理

### 6.1 连接错误
```json
{
  "type": "CONNECTION_ERROR",
  "id": "client-1704067200000-123476",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
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
  "vmId": "vm-001",
  "data": {
    "errorCode": "INVALID_SIGNATURE",
    "errorMessage": "消息签名验证失败",
    "messageId": "client-1704067200000-123456",
    "suggestion": "请检查密钥配置"
  }
}
```

### 6.3 重连策略
- **立即重连**: 连接意外断开时立即尝试重连
- **指数退避**: 重连失败时使用指数退避算法
- **最大重试**: 设置最大重试次数（默认10次）
- **重连间隔**: 重连间隔从1秒开始，最大60秒

## 7. 性能优化

### 7.1 消息压缩
- **GZIP压缩**: 对大型消息进行GZIP压缩
- **压缩阈值**: 消息大小超过1KB时启用压缩
- **压缩级别**: 使用平衡的压缩级别（6）

### 7.2 批量操作
- **批量消息**: 支持批量发送多个消息
- **消息队列**: 使用消息队列缓冲消息
- **异步处理**: 异步处理非关键消息

### 7.3 连接池
- **连接复用**: 复用WebSocket连接
- **连接限制**: 限制每个虚拟机的连接数
- **负载均衡**: 在多服务器环境下进行负载均衡

## 8. 监控和日志

### 8.1 连接监控
```json
{
  "type": "CONNECTION_STATS",
  "id": "server-1704067200000-123478",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "totalConnections": 100,
    "activeConnections": 85,
    "failedConnections": 5,
    "averageLatency": 50,
    "messageThroughput": 1000
  }
}
```

### 8.2 性能指标
- **连接数**: 当前活跃连接数
- **消息延迟**: 消息处理延迟
- **吞吐量**: 消息处理吞吐量
- **错误率**: 消息处理错误率
- **资源使用**: CPU、内存、网络使用情况

### 8.3 日志格式
```json
{
  "timestamp": "2024-01-01T00:00:00.000Z",
  "level": "INFO",
  "category": "WEBSOCKET",
  "vmId": "vm-001",
  "message": "WebSocket连接建立",
  "details": {
    "sessionId": "session-123456",
    "ipAddress": "192.168.1.100",
    "userAgent": "Python-WebSocket-Client/1.0.0"
  }
}
```

## 9. 部署配置

### 9.1 服务器配置
```yaml
websocket:
  path: /ws
  maxConnections: 10000
  heartbeatInterval: 30
  connectionTimeout: 60
  maxMessageSize: 10485760
  compression:
    enabled: true
    threshold: 1024
    level: 6
  security:
    enabled: true
    requireAuth: false
    keyPath: /path/to/keys
    algorithm: RSA
    keySize: 2048
```

### 9.2 客户端配置
```python
# WebSocket客户端配置
WEBSOCKET_CONFIG = {
    'url': 'ws://localhost:8080/ws/vm/{vm_id}',
    'heartbeat_interval': 30,
    'reconnect_interval': 5,
    'max_reconnect_attempts': 10,
    'connection_timeout': 60,
    'max_message_size': 10485760,
    'compression': {
        'enabled': True,
        'threshold': 1024,
        'level': 6
    }
}
```

这个WebSocket协议文档提供了完整的实时通信协议定义，支持虚拟机控制、学习控制、状态监控等功能，完全符合您的大创项目需求。 