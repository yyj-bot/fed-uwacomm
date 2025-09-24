# 水声联邦学习系统 虚拟机 API 参考文档

## 1. 概述

本文档定义了水声联邦学习系统的虚拟机管理相关HTTP REST API接口，包括虚拟机注册、查询、更新、删除、控制和状态监控等功能。

### 1.1 基础信息
- **基础URL**: `http://localhost:8080/api`
- **API版本**: v1.1
- **认证方式**:
  - VM自身操作（注册、Token刷新、心跳、健康检查）：无需JWT认证或使用特殊认证
  - 用户管理操作：用户JWT Token认证
- **数据格式**: JSON

### 1.2 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 1.3 数据库表结构

详细的数据库表结构设计请参考：[数据库表结构文档](../../database/database_schema.md)

本文档涉及的主要数据库表：
- **虚拟机表 (vm_instances)**: 存储虚拟机基本信息和连接状态

## 2. 接口分类说明

根据系统架构和认证需求，虚拟机接口分为两类：

### 2.1 VM自身操作接口 (`/api/v1/vm`)
由虚拟机端调用，用于虚拟机自身的生命周期管理：
- 虚拟机注册
- Token刷新
- 心跳上报
- 健康检查

这类接口不需要用户JWT认证，由VM自身调用。

### 2.2 用户管理操作接口 (`/api/vm`)
由用户通过Web界面调用，用于管理和控制虚拟机：
- 虚拟机列表查询
- 虚拟机详情查看
- 虚拟机配置更新
- 虚拟机删除
- 虚拟机控制（启动、停止、重启）
- 虚拟机状态查询

这类接口需要用户JWT认证。

## 3. 虚拟机端接口

### 3.1 虚拟机注册接口

**接口描述**: 注册新的虚拟机到系统中，提交虚拟机的所有基本信息。注册成功后，返回 `accessToken` 与 `secretId`（长期刷新凭证）。虚拟机应使用 `accessToken` 通过 WebSocket/STOMP 建立连接；到期前使用 `secretId` 通过 HTTP 刷新。

**实现方**: 虚拟机端

**请求信息**:
- **URL**: `POST /api/v1/vm/register`
- **方法**: POST
- **Content-Type**: application/json
- **认证**: 不需要预先JWT（注册成功后由服务端签发 Token）

**请求参数**:
```json
{
  "name": "水声联邦学习节点-001",
  "ipAddress": "192.168.1.100",
  "port": 22,
  "osType": "Ubuntu 20.04",
  "cpuCores": 4,
  "memoryMb": 8192,
  "diskGb": 100,
  "systemInfo": {"os": "Ubuntu 20.04 LTS"},
  "capabilities": {"supportedAlgorithms": ["FEDAVG"]}
}
```

**注意**: vmId由后端自动生成，客户端无需提供

**成功响应** (200):
```json
{
  "code": 200,
  "message": "虚拟机注册成功",
  "data": {
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "name": "水声联邦学习节点-001",
    "status": "OFFLINE",
    "connectionStatus": "DISCONNECTED",
    "createdAt": "2024-01-01T00:00:00.000Z",
    "sessionId": "session-123456",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "secretId": "s3cr3t_8f14e45fceea167a5a36dedd4bea2543",
    "tokenExpireSeconds": 86400,
    "websocket": {
      "sockjs": "http://localhost:8080/ws",
      "native": "ws://localhost:8080/ws-native"
    },
    "apiEndpoints": {
      "status": "/api/v1/vm/a1b2c3d4e5f678901234567890123456/status",
      "control": "/api/v1/vm/a1b2c3d4e5f678901234567890123456/control"
    }
  }
}
```

**错误响应** (400):
```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": {
    "errors": [
      {
        "field": "name",
        "message": "虚拟机名称不能为空"
      },
      {
        "field": "cpuCores",
        "message": "CPU核心数必须大于0"
      }
    ]
  }
}
```

**错误响应** (409):
```json
{
  "code": 409,
  "message": "虚拟机已存在",
  "data": {
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "existingName": "水声联邦学习节点-001"
  }
}
```

**错误响应** (500):
```json
{
  "code": 500,
  "message": "服务器内部错误",
  "data": {
    "error": "数据库连接失败",
    "requestId": "req-123456"
  }
}
```

**业务规则**:
1. **自动生成ID**: vmId由后端自动生成，采用32位UUID格式，保证全局唯一性
2. **资源限制**: CPU核心数≥1，内存≥1024MB，磁盘≥20GB
3. **IP地址验证**: 必须是有效的IPv4或IPv6地址格式
4. **端口范围**: SSH端口必须在1-65535范围内
5. **名称长度**: 虚拟机名称不能超过100个字符
6. **算法支持**: 至少支持一种联邦学习算法
7. **安全要求**: 如果提供SSH密钥，必须是有效的公钥格式

**使用示例**:
```bash
curl -X POST http://localhost:8080/api/v1/vm/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "水声联邦学习节点-001",
    "ipAddress": "192.168.1.100",
    "port": 22,
    "osType": "Ubuntu 20.04",
    "cpuCores": 4,
    "memoryMb": 8192,
    "diskGb": 100,
    "systemInfo": {"os": "Ubuntu 20.04 LTS"},
    "capabilities": {"supportedAlgorithms": ["FEDAVG"]}
  }'
```

### 3.2 WebSocket 建连指引（配合注册返回的 accessToken）
- SockJS + STOMP（推荐）：在 STOMP CONNECT 头携带 `Authorization: Bearer <accessToken>` 与 `vmId`
- 原生 WS + STOMP（可选）：将 token、vmId 置于查询参数，但不如 CONNECT 头安全
- 生产环境请使用 `wss://.../ws-native`

```javascript
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

const socket = new SockJS('http://localhost:8080/ws');
const client = Stomp.over(socket);
client.connect(
  { Authorization: 'Bearer <accessToken>', vmId: '<vmId>' },
  () => {/* onConnected */},
  (err) => {/* onError */}
);
```

### 3.2 Token 刷新接口

**接口描述**: 使用长期刷新凭证 `secretId` 刷新 `accessToken`。支持凭证旋转：每次刷新可返回新的 `secretId`，旧凭证立即失效或在短暂宽限窗口后失效。

**请求信息**:
- **URL**: `POST /api/v1/vm/token/refresh`
- **方法**: POST
- **Content-Type**: application/json
- **认证**: 无需 accessToken；使用 `vmId + secretId` 进行认证（可选挑战应答方式见说明）

**请求参数**:
```json
{
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "secretId": "s3cr3t_8f14e45fceea167a5a36dedd4bea2543"
}
```

> 可选强化：先 `GET /api/v1/vm/token/refresh/nonce?vmId=...` 获取 `nonce`，客户端提交 `hmac = HMAC_SHA256(secretId, nonce)`，避免明文 secretId 直接传输。

**成功响应** (200):
```json
{
  "code": 200,
  "message": "刷新成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenExpireSeconds": 86400,
    "secretId": "s3cr3t_new_7c222fb2927d828af22f592134e8932480637c0d" 
  }
}
```

**错误响应**:
- 400: 参数错误
- 401: 凭证无效/过期/被撤销
- 429: 刷新频率过高

**客户端流程建议**:
1. 记录 `tokenExpireSeconds`，在到期前 60s 刷新
2. 刷新成功后，断开并使用新 `accessToken` 重连 WebSocket（在 STOMP CONNECT 头携带 `Authorization` 与 `vmId`）
3. 若启用凭证旋转，更新本地存储的 `secretId`

## 4. 用户管理操作接口

### 4.1 虚拟机列表查询接口

**接口描述**: 获取系统中所有虚拟机的列表信息

**实现方**: 用户管理界面

**说明**: 返回的status字段通过WebSocket实时查询虚拟机获取，确保状态信息的准确性。

**请求信息**:
- **URL**: `GET /api/vm/list`
- **方法**: GET
- **认证**: 需要用户JWT Token

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页大小，默认20，最大100 |
| status | String | 否 | 状态过滤，可选值：RUNNING, STOPPED, STARTING, STOPPING, ERROR, OFFLINE |
| osType | String | 否 | 操作系统类型过滤 |
| keyword | String | 否 | 关键词搜索（名称、IP地址） |

**成功响应** (200):
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 10,
    "page": 1,
    "size": 20,
    "pages": 1,
    "list": [
      {
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "name": "水声联邦学习节点-001",
        "ipAddress": "192.168.1.100",
        "port": 22,
        "status": "RUNNING",
        "osType": "Ubuntu 20.04",
        "cpuCores": 4,
        "memoryMb": 8192,
        "diskGb": 100,
        "connectionStatus": "CONNECTED",
        "lastHeartbeat": "2024-01-01T00:00:00.000Z",
        "createdAt": "2024-01-01T00:00:00.000Z",
        "updatedAt": "2024-01-01T00:00:00.000Z"
      }
    ]
  }
}
```

### 4.2 虚拟机详情查询接口

**接口描述**: 获取指定虚拟机的详细信息

**实现方**: 用户管理界面

**说明**: 返回的status字段通过WebSocket实时查询虚拟机获取，确保状态信息的准确性。

**请求信息**:
- **URL**: `GET /api/vm/{vmId}`
- **方法**: GET
- **认证**: 需要用户JWT Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vmId | String | 是 | 虚拟机ID |

**成功响应** (200):
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "name": "水声联邦学习节点-001",
    "ipAddress": "192.168.1.100",
    "port": 22,
    "status": "RUNNING",
    "osType": "Ubuntu 20.04",
    "cpuCores": 4,
    "memoryMb": 8192,
    "diskGb": 100,
    "connectionStatus": "CONNECTED",
    "lastHeartbeat": "2024-01-01T00:00:00.000Z",
    "wsSessionId": "session-123456",
    "createdAt": "2024-01-01T00:00:00.000Z",
    "updatedAt": "2024-01-01T00:00:00.000Z",
    "systemInfo": {
      "os": "Ubuntu 20.04 LTS",
      "kernel": "5.4.0-42-generic",
      "python": "3.8.10",
      "gpu": "NVIDIA Tesla V100",
      "cuda": "11.0",
      "cudnn": "8.0.5"
    },
    "capabilities": {
      "supportedAlgorithms": ["FEDAVG", "FEDPROX", "FEDNOVA", "SCAFFOLD"],
      "maxBatchSize": 128,
      "maxMemoryUsage": 6144,
      "gpuMemory": 16384,
      "networkSpeed": 1000
    },
    "networkConfig": {
      "uploadSpeed": 100,
      "downloadSpeed": 200,
      "latency": 50,
      "bandwidth": 1000
    },
    "metadata": {
      "description": "水声联邦学习专用虚拟机节点",
      "location": "实验室A-机架01",
      "owner": "张三",
      "department": "水声工程学院",
      "tags": ["水声", "联邦学习", "GPU节点"]
    }
  }
}
```

### 4.3 虚拟机更新接口

**接口描述**: 更新虚拟机的配置信息

**实现方**: 用户管理界面

**请求信息**:
- **URL**: `PUT /api/vm/{vmId}`
- **方法**: PUT
- **Content-Type**: application/json
- **认证**: 需要用户JWT Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vmId | String | 是 | 虚拟机ID |

**请求参数**:
```json
{
  "name": "水声联邦学习节点-001-更新",
  "ipAddress": "192.168.1.101",
  "port": 2222,
  "osType": "Ubuntu 20.04",
  "cpuCores": 8,
  "memoryMb": 16384,
  "diskGb": 200,
  "systemInfo": {
    "os": "Ubuntu 20.04 LTS",
    "kernel": "5.4.0-42-generic",
    "python": "3.8.10",
    "gpu": "NVIDIA Tesla V100",
    "cuda": "11.0",
    "cudnn": "8.0.5"
  },
  "capabilities": {
    "supportedAlgorithms": ["FEDAVG", "FEDPROX", "FEDNOVA", "SCAFFOLD"],
    "maxBatchSize": 256,
    "maxMemoryUsage": 12288,
    "gpuMemory": 16384,
    "networkSpeed": 1000
  },
  "networkConfig": {
    "uploadSpeed": 200,
    "downloadSpeed": 400,
    "latency": 30,
    "bandwidth": 1000
  },
  "metadata": {
    "description": "水声联邦学习专用虚拟机节点-更新版",
    "location": "实验室A-机架01",
    "owner": "张三",
    "department": "水声工程学院",
    "tags": ["水声", "联邦学习", "GPU节点", "高性能"]
  }
}
```

**成功响应** (200):
```json
{
  "code": 200,
  "message": "虚拟机更新成功",
  "data": {
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "name": "水声联邦学习节点-001-更新",
    "updatedAt": "2024-01-01T00:00:00.000Z"
  }
}
```

### 4.4 虚拟机删除接口

**接口描述**: 从系统中删除指定的虚拟机

**实现方**: 用户管理界面

**请求信息**:
- **URL**: `DELETE /api/vm/{vmId}`
- **方法**: DELETE
- **认证**: 需要用户JWT Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vmId | String | 是 | 虚拟机ID |

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| force | Boolean | 否 | 是否强制删除，默认false |

**成功响应** (200):
```json
{
  "code": 200,
  "message": "虚拟机删除成功",
  "data": {
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "deletedAt": "2024-01-01T00:00:00.000Z"
  }
}
```

## 5. 虚拟机控制接口

### 5.1 虚拟机启动接口

**接口描述**: 启动指定的虚拟机

**实现方**: 用户管理界面

**说明**: 返回的status字段表示命令执行后的预期状态，实际状态需要通过WebSocket实时查询。

**请求信息**:
- **URL**: `POST /api/vm/{vmId}/start`
- **方法**: POST
- **Content-Type**: application/json
- **认证**: 需要用户JWT Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vmId | String | 是 | 虚拟机ID |

**请求参数**:
```json
{
  "timeout": 300,
  "config": {
    "memory": "8GB",
    "cpu": "8cores",
    "disk": "100GB",
    "network": "bridge"
  },
  "environment": {
    "variables": {
      "PYTHONPATH": "/app",
      "CUDA_VISIBLE_DEVICES": "0"
    }
  }
}
```

**成功响应** (200):
```json
{
  "code": 200,
  "message": "虚拟机启动命令已发送",
  "data": {
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "status": "STARTING",
    "commandId": "cmd-123456",
    "estimatedTime": 60
  }
}
```

### 5.2 虚拟机停止接口

**接口描述**: 停止指定的虚拟机

**实现方**: 用户管理界面

**说明**: 返回的status字段表示命令执行后的预期状态，实际状态需要通过WebSocket实时查询。

**请求信息**:
- **URL**: `POST /api/vm/{vmId}/stop`
- **方法**: POST
- **Content-Type**: application/json
- **认证**: 需要用户JWT Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vmId | String | 是 | 虚拟机ID |

**请求参数**:
```json
{
  "force": false,
  "timeout": 60,
  "saveState": true
}
```

**成功响应** (200):
```json
{
  "code": 200,
  "message": "虚拟机停止命令已发送",
  "data": {
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "status": "STOPPING",
    "commandId": "cmd-123457",
    "estimatedTime": 30
  }
}
```

### 5.3 虚拟机重启接口

**接口描述**: 重启指定的虚拟机

**实现方**: 用户管理界面

**说明**: 返回的status字段表示命令执行后的预期状态，实际状态需要通过WebSocket实时查询。

**请求信息**:
- **URL**: `POST /api/vm/{vmId}/restart`
- **方法**: POST
- **Content-Type**: application/json
- **认证**: 需要用户JWT Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vmId | String | 是 | 虚拟机ID |

**请求参数**:
```json
{
  "timeout": 300,
  "graceful": true,
  "config": {
    "memory": "8GB",
    "cpu": "8cores"
  }
}
```

**成功响应** (200):
```json
{
  "code": 200,
  "message": "虚拟机重启命令已发送",
  "data": {
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "status": "STARTING",
    "commandId": "cmd-123458",
    "estimatedTime": 120
  }
}
```

## 6. 虚拟机状态接口

### 6.1 虚拟机状态查询接口

**接口描述**: 获取指定虚拟机的实时状态信息（通过WebSocket实时查询）

**实现方**: 用户管理界面

**说明**: 此接口通过WebSocket连接向虚拟机发送状态查询请求，获取实时的虚拟机状态信息，而不是从数据库读取静态状态。

**请求信息**:
- **URL**: `GET /api/vm/{vmId}/status`
- **方法**: GET
- **认证**: 需要用户JWT Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vmId | String | 是 | 虚拟机ID |

**成功响应** (200):
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "status": "RUNNING",
    "connectionStatus": "CONNECTED",
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
      "user": 15
    },
    "lastHeartbeat": "2024-01-01T00:00:00.000Z",
    "wsSessionId": "session-123456"
  }
}
```

## 7. 错误码定义

### 7.1 通用错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| 200 | 200 | 成功 |
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未授权，需要登录 |
| 403 | 403 | 禁止访问，权限不足 |
| 404 | 404 | 资源不存在 |
| 409 | 409 | 资源冲突，已存在 |
| 422 | 422 | 请求参数验证失败 |
| 500 | 500 | 服务器内部错误 |
| 502 | 502 | 网关错误 |
| 503 | 503 | 服务不可用 |

### 7.2 业务错误码
| 错误码 | 说明 |
|--------|------|
| VM_NOT_FOUND | 虚拟机不存在 |
| VM_ALREADY_EXISTS | 虚拟机已存在 |
| VM_INVALID_ID | 虚拟机ID格式错误 |
| VM_INVALID_STATUS | 虚拟机状态错误 |
| VM_CONNECTION_FAILED | 虚拟机连接失败 |
| VM_OPERATION_TIMEOUT | 虚拟机操作超时 |
| VM_INSUFFICIENT_RESOURCES | 资源不足 |
| VM_SECURITY_ERROR | 安全验证失败 |

## 8. 安全规范

### 8.1 认证授权
- 所有API接口都需要JWT Token认证
- Token过期时间为24小时
- 支持Token刷新机制

### 8.2 数据验证
- 所有输入参数都需要进行格式验证
- 使用正则表达式验证特殊格式字段
- 对敏感数据进行加密存储

### 8.3 访问控制
- 基于角色的访问控制(RBAC)
- 虚拟机操作需要相应权限
- 记录所有操作日志

### 8.4 网络安全
- 使用HTTPS协议传输
- 支持API限流和防DDoS攻击
- 定期更新安全补丁

## 9. 性能规范

### 9.1 响应时间
- 查询接口响应时间 < 200ms
- 注册接口响应时间 < 500ms
- 控制接口响应时间 < 1000ms

### 9.2 并发处理
- 支持1000并发连接
- 使用连接池管理数据库连接
- 异步处理非关键操作

### 9.3 缓存策略
- 使用Redis缓存热点数据
- 虚拟机状态缓存5秒
- 配置信息缓存1小时 