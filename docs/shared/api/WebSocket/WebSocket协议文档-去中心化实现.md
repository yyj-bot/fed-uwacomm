# 水声联邦学习系统 WebSocket 通信协议 - 去中心化实现

## 0. 架构设计

### 0.1 去中心化架构模式

本系统采用去中心化WebSocket架构模式：

```
可视化客户端 ←→ WebSocket ←→ 虚拟机（作为WebSocket服务器）
```

**特点**:
- 虚拟机直接提供WebSocket服务
- 减少延迟，提高实时性
- 降低后端服务器负载
- 简化消息传递路径
- 适用于内网环境

### 0.2 虚拟机WebSocket服务器实现

#### 0.2.1 虚拟机端WebSocket服务器配置
- 虚拟机直接运行WebSocket服务器
- 监听指定端口（默认8081）
- 处理客户端连接和消息
- 提供状态查询和控制功能

### 0.3 网络配置要求

#### 0.3.1 虚拟机端配置
- 开放WebSocket端口（默认8081）
- 配置防火墙规则
- 检查端口监听状态

#### 0.3.2 客户端网络要求
- 能够访问虚拟机的IP地址和端口
- 支持WebSocket协议
- 网络延迟 < 50ms（推荐，因为直连）

### 0.4 安全考虑

#### 0.4.1 认证机制
- 使用JWT Token进行身份验证
- 虚拟机端验证客户端身份和权限
- 支持Token过期和刷新机制

#### 0.4.2 网络安全
- 使用WSS协议（TLS加密）
- 实现IP白名单
- 限制连接频率
- 监控异常连接

## 1. 概述

本文档定义了水声联邦学习系统的去中心化WebSocket通信协议，用于实现客户端与虚拟机之间的直接实时双向通信，支持虚拟机控制、学习控制、状态监控等功能。

### 1.1 基础信息
- **WebSocket URL**: `ws://{vm_ip}:8081/ws/vm/{vmId}` (开发环境)
- **WebSocket Secure URL**: `wss://{vm_ip}:8081/ws/vm/{vmId}` (生产环境)
- **协议版本**: v1.0
- **认证方式**: JWT Token（可选）
- **数据格式**: JSON
- **编码**: UTF-8
- **TLS版本**: TLS 1.2及以上（WSS连接）

### 1.2 连接参数
- `vmId`: 虚拟机唯一标识（必需）
- `token`: JWT认证令牌（可选）
- `version`: 客户端版本号（可选）

### 1.3 连接示例
```javascript
// 开发环境 - 基础连接
const ws = new WebSocket('ws://192.168.1.100:8081/ws/vm/vm-001');

// 开发环境 - 带认证的连接
const ws = new WebSocket('ws://192.168.1.100:8081/ws/vm/vm-001?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...');

// 生产环境 - WSS连接
const ws = new WebSocket('wss://vm-001.example.com:8081/ws/vm/vm-001');

// 生产环境 - 带认证的WSS连接
const ws = new WebSocket('wss://vm-001.example.com:8081/ws/vm/vm-001?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...');
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
- `signature`: 数字签名（可选，用于安全验证）

### 2.3 消息ID生成规则
- 客户端消息：`client-{timestamp}-{random}`
- 虚拟机消息：`vm-{timestamp}-{random}`
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
    "capabilities": ["STATUS_QUERY", "VM_CONTROL", "TRAINING_CONTROL"],
    "clientInfo": {
      "type": "ADMIN_PANEL",
      "version": "1.0.0",
      "user": "admin"
    }
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.1.2 虚拟机连接确认 (CONNECT_ACK)
```json
{
  "type": "CONNECT_ACK",
  "id": "vm-1704067200000-123456",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "sessionId": "session-123456",
    "serverTime": "2024-01-01T00:00:00.000Z",
    "heartbeatInterval": 30,
    "maxMessageSize": 10485760,
    "supportedFeatures": ["STATUS_QUERY", "VM_CONTROL", "TRAINING_CONTROL"],
    "vmInfo": {
      "os": "Ubuntu 20.04",
      "python": "3.8.10",
      "memory": "4GB",
      "cpu": "Intel Xeon E5-2680",
      "gpu": "NVIDIA Tesla V100"
    }
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
    "clientTime": "2024-01-01T00:00:00.000Z",
    "latency": 50
  },
  "signature": "base64_encoded_signature"
}
```

#### 3.2.2 虚拟机心跳响应 (HEARTBEAT_ACK)
```json
{
  "type": "HEARTBEAT_ACK",
  "id": "vm-1704067200000-123458",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
  "data": {
    "serverTime": "2024-01-01T00:00:00.000Z",
    "nextHeartbeat": 30,
    "systemStatus": "NORMAL",
    "resourceUsage": {
      "cpu": 25.5,
      "memory": 60.2,
      "disk": 45.8,
      "gpu": 15.3
    }
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
      "downloadUrl": "http://server.example.com/api/v1/model/download/model-123456"
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

### 3.5 训练进度消息

#### 3.5.1 训练进度报告 (TRAINING_PROGRESS)
```json
{
  "type": "TRAINING_PROGRESS",
  "id": "vm-1704067200000-123468",
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

### 3.6 状态查询消息

#### 3.6.1 状态查询请求 (STATUS_QUERY)
```json
{
  "type": "STATUS_QUERY",
  "id": "cmd-1704067200000-123476",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
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

#### 3.6.2 状态查询响应 (STATUS_RESPONSE)
```json
{
  "type": "STATUS_RESPONSE",
  "id": "vm-1704067200000-123477",
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

### 3.7 错误和状态消息

#### 3.7.1 错误报告 (ERROR)
```json
{
  "type": "ERROR",
  "id": "vm-1704067200000-123472",
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

## 4. 消息处理流程

### 4.1 连接建立流程
1. **客户端连接**: 客户端直接连接到虚拟机WebSocket服务器
2. **发送连接请求**: 客户端发送CONNECT消息
3. **身份验证**: 虚拟机验证客户端身份和权限
4. **连接确认**: 虚拟机发送CONNECT_ACK确认连接
5. **开始心跳**: 启动心跳机制保持连接活跃

### 4.2 心跳机制
- **心跳间隔**: 客户端每30秒发送一次HEARTBEAT消息
- **虚拟机响应**: 虚拟机收到后立即回复HEARTBEAT_ACK
- **超时检测**: 如果虚拟机30秒内未收到心跳，标记客户端为离线
- **客户端重连**: 如果客户端60秒内未收到响应，尝试重新连接
- **指数退避**: 重连失败时使用指数退避算法

### 4.3 命令处理流程
1. **命令发送**: 客户端发送命令消息到目标虚拟机
2. **命令接收**: 虚拟机接收并验证命令签名
3. **命令执行**: 虚拟机执行相应操作
4. **结果反馈**: 虚拟机发送执行结果或状态更新
5. **状态同步**: 虚拟机更新本地状态

### 4.4 训练流程
1. **任务启动**: 客户端发送TRAINING_START命令
2. **模型下载**: 虚拟机从外部服务器下载全局模型
3. **本地训练**: 虚拟机开始本地训练过程
4. **进度报告**: 虚拟机定期发送TRAINING_PROGRESS
5. **模型上传**: 训练完成后虚拟机上传模型到外部服务器
6. **状态更新**: 虚拟机向客户端报告训练完成状态

### 4.5 状态查询流程
1. **查询请求**: 客户端发送STATUS_QUERY消息到目标虚拟机
2. **状态收集**: 虚拟机收集当前状态信息（CPU、内存、磁盘、网络等）
3. **状态响应**: 虚拟机发送STATUS_RESPONSE包含完整状态信息
4. **状态处理**: 客户端处理状态信息并更新缓存
5. **状态显示**: 客户端将状态信息显示给用户
6. **监控通知**: 如果配置了监控，发送状态变更通知

### 4.6 批量状态查询流程
1. **批量查询**: 客户端发送BATCH_STATUS_QUERY到多个虚拟机
2. **并行收集**: 多个虚拟机并行收集状态信息
3. **汇总响应**: 客户端汇总所有虚拟机的状态信息
4. **批量响应**: 发送BATCH_STATUS_RESPONSE包含汇总结果
5. **统计分析**: 生成状态统计摘要信息

## 5. 客户端连接管理

### 5.1 客户端连接管理器
- 管理到虚拟机的WebSocket连接
- 处理连接建立和断开
- 实现消息发送和接收
- 提供状态查询和命令发送功能

### 5.2 虚拟机端负载均衡
- 最大连接数限制
- 每秒最大消息数限制
- 连接超时时间设置
- 消息频率限制检查

### 5.3 客户端连接池
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
  "id": "vm-1704067200000-123477",
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

### 6.3 状态查询错误
```json
{
  "type": "STATUS_QUERY_ERROR",
  "id": "vm-1704067200000-123484",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "vm-001",
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
- 使用Python基础镜像
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

### 8.1 虚拟机端监控
- 系统指标收集
- 告警阈值检查
- 告警消息发送
- 监控任务管理

### 8.2 客户端监控面板
- 连接状态监控
- 性能指标展示
- 实时数据更新
- 告警处理

## 9. 架构选择建议

### 9.1 开发环境
- **推荐**: 去中心化架构
- **原因**: 便于调试，减少网络复杂性

### 9.2 测试环境
- **推荐**: 去中心化架构
- **原因**: 内网环境，网络穿透问题较少

### 9.3 生产环境
- **推荐**: 中心化架构
- **原因**: 更好的安全控制和负载均衡

## 10. 总结

这个去中心化实现文档提供了：

1. **完整的去中心化WebSocket架构设计**
2. **虚拟机端WebSocket服务器实现**
3. **客户端连接管理和多虚拟机管理**
4. **负载均衡和连接池管理**
5. **错误处理和重连策略**
6. **Docker部署和系统服务配置**
7. **监控和日志系统**

去中心化架构的优势在于：
- 减少延迟，提高实时性
- 降低后端服务器负载
- 简化消息传递路径
- 适用于内网环境
- 便于调试和开发

去中心化架构的适用场景：
- 内网环境部署
- 对实时性要求较高的场景
- 虚拟机数量较少的场景
- 开发和测试环境 