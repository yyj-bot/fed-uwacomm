# 水声联邦学习系统 虚拟机 API 参考文档

## 1. 概述

本文档定义了水声联邦学习系统的虚拟机管理相关HTTP REST API接口，包括虚拟机注册、查询、更新、删除、控制和状态监控等功能。

### 1.1 基础信息
- **基础URL**: `http://localhost:8080/api`
- **API版本**: v1.0
- **认证方式**: JWT Token
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

根据系统架构，虚拟机接口分为两类：

1. **虚拟机端接口**: 由虚拟机端实现，用于虚拟机向服务器注册自身信息
2. **客户端可视化接口**: 由客户端可视化阶段实现，用于管理员通过Web界面管理虚拟机

## 3. 虚拟机端接口

### 3.1 虚拟机注册接口

**接口描述**: 注册新的虚拟机到系统中，提交虚拟机的所有基本信息

**实现方**: 虚拟机端

**请求信息**:
- **URL**: `POST /api/v1/vm/register`
- **方法**: POST
- **Content-Type**: application/json
- **认证**: 需要JWT Token

**请求参数**:
```json
{
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "name": "水声联邦学习节点-001",
  "ipAddress": "192.168.1.100",
  "port": 22,
  "osType": "Ubuntu 20.04",
  "cpuCores": 4,
  "memoryMb": 8192,
  "diskGb": 100, 
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
  "securityConfig": {
    "sshKey": "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC...",
    "certificate": "-----BEGIN CERTIFICATE-----\nMIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw\nTzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh\ncmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgQ0EwHhcNMTUwNjA0MTEwNDM4\nWhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu\nZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCB\nDQTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rV\n-----END CERTIFICATE-----",
    "encryptionEnabled": true,
    "signatureAlgorithm": "RSA-SHA256"
  },
  "metadata": {
    "description": "水声联邦学习专用虚拟机节点",
    "location": "实验室A-机架01",
    "owner": "张三",
    "department": "水声工程学院",
    "tags": ["水声", "联邦学习", "GPU节点"]
  }
}
```

**请求字段说明**:
| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vmId | String | 是 | 虚拟机唯一标识，32位UUID格式 |
| name | String | 是 | 虚拟机名称，最大100字符 |
| ipAddress | String | 是 | IP地址，IPv4或IPv6格式 |
| port | Integer | 否 | SSH端口，默认22 |
| osType | String | 是 | 操作系统类型和版本 |
| cpuCores | Integer | 是 | CPU核心数，最小1 |
| memoryMb | Integer | 是 | 内存大小(MB)，最小1024 |
| diskGb | Integer | 是 | 磁盘大小(GB)，最小20 |
| systemInfo | Object | 否 | 系统详细信息 |
| capabilities | Object | 否 | 虚拟机能力配置 |
| networkConfig | Object | 否 | 网络配置信息 |
| securityConfig | Object | 否 | 安全配置信息 |
| metadata | Object | 否 | 元数据信息 |

**systemInfo字段说明**:
| 字段名 | 类型 | 说明 |
|--------|------|------|
| os | String | 操作系统名称和版本 |
| kernel | String | 内核版本 |
| python | String | Python版本 |
| gpu | String | GPU型号 |
| cuda | String | CUDA版本 |
| cudnn | String | cuDNN版本 |

**capabilities字段说明**:
| 字段名 | 类型 | 说明 |
|--------|------|------|
| supportedAlgorithms | Array | 支持的联邦学习算法 |
| maxBatchSize | Integer | 最大批次大小 |
| maxMemoryUsage | Integer | 最大内存使用量(MB) |
| gpuMemory | Integer | GPU内存大小(MB) |
| networkSpeed | Integer | 网络速度(Mbps) |

**networkConfig字段说明**:
| 字段名 | 类型 | 说明 |
|--------|------|------|
| uploadSpeed | Integer | 上传速度(Mbps) |
| downloadSpeed | Integer | 下载速度(Mbps) |
| latency | Integer | 网络延迟(ms) |
| bandwidth | Integer | 带宽(Mbps) |

**securityConfig字段说明**:
| 字段名 | 类型 | 说明 |
|--------|------|------|
| sshKey | String | SSH公钥 |
| certificate | String | SSL证书 |
| encryptionEnabled | Boolean | 是否启用加密 |
| signatureAlgorithm | String | 签名算法 |

**metadata字段说明**:
| 字段名 | 类型 | 说明 |
|--------|------|------|
| description | String | 虚拟机描述 |
| location | String | 物理位置 |
| owner | String | 负责人 |
| department | String | 所属部门 |
| tags | Array | 标签列表 |

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
    "websocketUrl": "ws://localhost:8080/ws/vm/a1b2c3d4e5f678901234567890123456",
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
        "field": "vmId",
        "message": "虚拟机ID格式不正确，应为32位UUID格式"
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
1. **虚拟机ID唯一性**: vmId必须在系统中唯一，不能重复注册
2. **格式验证**: vmId必须符合32位UUID格式，如a1b2c3d4e5f678901234567890123456
3. **资源限制**: CPU核心数≥1，内存≥1024MB，磁盘≥20GB
4. **IP地址验证**: 必须是有效的IPv4或IPv6地址格式
5. **端口范围**: SSH端口必须在1-65535范围内
6. **名称长度**: 虚拟机名称不能超过100个字符
7. **算法支持**: 至少支持一种联邦学习算法
8. **安全要求**: 如果提供SSH密钥，必须是有效的公钥格式

**使用示例**:
```bash
curl -X POST http://localhost:8080/api/v1/vm/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "name": "水声联邦学习节点-001",
    "ipAddress": "192.168.1.100",
    "port": 22,
    "osType": "Ubuntu 20.04",
    "cpuCores": 4,
    "memoryMb": 8192,
    "diskGb": 100,
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
    "securityConfig": {
      "sshKey": "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC...",
      "encryptionEnabled": true,
      "signatureAlgorithm": "RSA-SHA256"
    },
    "metadata": {
      "description": "水声联邦学习专用虚拟机节点",
      "location": "实验室A-机架01",
      "owner": "张三",
      "department": "水声工程学院",
      "tags": ["水声", "联邦学习", "GPU节点"]
    }
  }'
```

## 4. 客户端可视化接口

### 4.1 虚拟机列表查询接口

**接口描述**: 获取系统中所有虚拟机的列表信息

**实现方**: 客户端可视化阶段

**说明**: 返回的status字段通过WebSocket实时查询虚拟机获取，确保状态信息的准确性。

**请求信息**:
- **URL**: `GET /api/v1/vm/list`
- **方法**: GET
- **认证**: 需要JWT Token

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

**实现方**: 客户端可视化阶段

**说明**: 返回的status字段通过WebSocket实时查询虚拟机获取，确保状态信息的准确性。

**请求信息**:
- **URL**: `GET /api/v1/vm/{vmId}`
- **方法**: GET
- **认证**: 需要JWT Token

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

**实现方**: 客户端可视化阶段

**请求信息**:
- **URL**: `PUT /api/v1/vm/{vmId}`
- **方法**: PUT
- **Content-Type**: application/json
- **认证**: 需要JWT Token

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

**实现方**: 客户端可视化阶段

**请求信息**:
- **URL**: `DELETE /api/v1/vm/{vmId}`
- **方法**: DELETE
- **认证**: 需要JWT Token

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

**实现方**: 客户端可视化阶段

**说明**: 返回的status字段表示命令执行后的预期状态，实际状态需要通过WebSocket实时查询。

**请求信息**:
- **URL**: `POST /api/v1/vm/{vmId}/start`
- **方法**: POST
- **Content-Type**: application/json
- **认证**: 需要JWT Token

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

**实现方**: 客户端可视化阶段

**说明**: 返回的status字段表示命令执行后的预期状态，实际状态需要通过WebSocket实时查询。

**请求信息**:
- **URL**: `POST /api/v1/vm/{vmId}/stop`
- **方法**: POST
- **Content-Type**: application/json
- **认证**: 需要JWT Token

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

**实现方**: 客户端可视化阶段

**说明**: 返回的status字段表示命令执行后的预期状态，实际状态需要通过WebSocket实时查询。

**请求信息**:
- **URL**: `POST /api/v1/vm/{vmId}/restart`
- **方法**: POST
- **Content-Type**: application/json
- **认证**: 需要JWT Token

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

**实现方**: 客户端可视化阶段

**说明**: 此接口通过WebSocket连接向虚拟机发送状态查询请求，获取实时的虚拟机状态信息，而不是从数据库读取静态状态。

**请求信息**:
- **URL**: `GET /api/v1/vm/{vmId}/status`
- **方法**: GET
- **认证**: 需要JWT Token

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