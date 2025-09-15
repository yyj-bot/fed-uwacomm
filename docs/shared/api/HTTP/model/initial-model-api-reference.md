# 初始模型管理 API 参考文档

## 1. 概述

初始模型管理API提供联邦学习任务的初始模型生成、上传、分发和管理功能。支持随机生成初始模型和用户自定义初始模型两种方式。

### 1.1 基础信息
- **模块**: 初始模型管理 (Initial Model Management)
- **基础URL**: `http://localhost:8080/api/model/initial`
- **认证方式**: JWT Token
- **数据格式**: JSON + 文件上传

### 1.2 功能概述
- 随机生成初始模型
- 上传自定义初始模型
- 查询任务初始模型信息
- 分发初始模型到参与虚拟机
- 初始模型版本管理

---

## 2. API 接口列表

### 2.1 随机生成初始模型
生成基于指定参数的随机初始模型

**接口信息**
- **URL**: `POST /api/model/initial/generate`
- **描述**: 根据模型架构参数生成随机初始模型
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**请求参数**
```json
{
  "taskId": "task_001",
  "modelType": "neural_network",
  "architecture": {
    "inputSize": 128,
    "hiddenLayers": [64, 32, 16],
    "outputSize": 10,
    "activationFunction": "relu",
    "optimizer": "adam",
    "learningRate": 0.001
  },
  "randomSeed": 42,
  "description": "水声信号分类初始模型"
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "初始模型生成成功",
  "data": {
    "modelId": "initial_model_001",
    "taskId": "task_001",
    "modelType": "neural_network",
    "modelSize": 1048576,
    "parametersCount": 12345,
    "architecture": {
      "inputSize": 128,
      "hiddenLayers": [64, 32, 16],
      "outputSize": 10,
      "activationFunction": "relu",
      "optimizer": "adam",
      "learningRate": 0.001
    },
    "generatedAt": "2024-09-10T10:00:00Z",
    "status": "READY",
    "checksum": "sha256:abcd1234..."
  }
}
```

### 2.2 上传自定义初始模型
上传用户预训练的初始模型文件

**接口信息**
- **URL**: `POST /api/model/initial/upload`
- **描述**: 上传自定义初始模型文件
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)
- **请求类型**: multipart/form-data

**请求参数**
```
taskId: "task_001" (required)
modelType: "neural_network" (required)
description: "预训练的水声模型" (optional)
file: [binary file] (required)
metadata: {
  "architecture": {
    "inputSize": 128,
    "outputSize": 10
  },
  "framework": "pytorch",
  "version": "1.0"
} (optional, JSON string)
```

**响应示例**
```json
{
  "code": 200,
  "message": "初始模型上传成功",
  "data": {
    "modelId": "initial_model_002",
    "taskId": "task_001",
    "modelType": "neural_network",
    "fileName": "initial_model.pth",
    "modelSize": 2048576,
    "uploadedAt": "2024-09-10T10:00:00Z",
    "status": "UPLOADED",
    "checksum": "sha256:efgh5678...",
    "metadata": {
      "architecture": {
        "inputSize": 128,
        "outputSize": 10
      },
      "framework": "pytorch",
      "version": "1.0"
    }
  }
}
```

### 2.3 获取任务初始模型
查询指定联邦学习任务的初始模型信息

**接口信息**
- **URL**: `GET /api/model/initial/{taskId}`
- **描述**: 获取任务的初始模型详情
- **认证**: 需要JWT Token

**路径参数**
- `taskId` (string, required): 联邦学习任务ID

**查询参数**
- `includeParameters` (boolean, optional): 是否包含模型参数，默认false
- `format` (string, optional): 返回格式 (json|binary)，默认json

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "modelId": "initial_model_001",
    "taskId": "task_001",
    "modelType": "neural_network",
    "modelSize": 1048576,
    "createdAt": "2024-09-10T10:00:00Z",
    "status": "DISTRIBUTED",
    "architecture": {
      "inputSize": 128,
      "hiddenLayers": [64, 32, 16],
      "outputSize": 10,
      "activationFunction": "relu",
      "optimizer": "adam",
      "learningRate": 0.001
    },
    "distributionStatus": {
      "totalVms": 5,
      "distributedVms": 5,
      "failedVms": 0,
      "distributedAt": "2024-09-10T10:05:00Z"
    },
    "checksum": "sha256:abcd1234..."
  }
}
```

### 2.4 分发初始模型
将初始模型分发到参与联邦学习的虚拟机

**接口信息**
- **URL**: `POST /api/model/initial/{taskId}/distribute`
- **描述**: 分发初始模型到指定虚拟机
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**路径参数**
- `taskId` (string, required): 联邦学习任务ID

**请求参数**
```json
{
  "vmIds": ["vm_001", "vm_002", "vm_003"],
  "distributionMode": "ASYNC",
  "timeout": 300,
  "retryAttempts": 3,
  "verifyChecksum": true,
  "notifyOnCompletion": true
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "模型分发已启动",
  "data": {
    "distributionId": "dist_001",
    "taskId": "task_001",
    "modelId": "initial_model_001",
    "targetVms": ["vm_001", "vm_002", "vm_003"],
    "distributionMode": "ASYNC",
    "status": "IN_PROGRESS",
    "startedAt": "2024-09-10T10:00:00Z",
    "estimatedCompletion": "2024-09-10T10:05:00Z",
    "progress": {
      "total": 3,
      "completed": 0,
      "failed": 0,
      "inProgress": 3
    }
  }
}
```

### 2.5 查询分发状态
查询初始模型分发的实时状态

**接口信息**
- **URL**: `GET /api/model/initial/distribution/{distributionId}`
- **描述**: 查询模型分发状态详情
- **认证**: 需要JWT Token

**路径参数**
- `distributionId` (string, required): 分发任务ID

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "distributionId": "dist_001",
    "taskId": "task_001",
    "modelId": "initial_model_001",
    "status": "COMPLETED",
    "startedAt": "2024-09-10T10:00:00Z",
    "completedAt": "2024-09-10T10:04:30Z",
    "progress": {
      "total": 3,
      "completed": 3,
      "failed": 0,
      "inProgress": 0
    },
    "vmDetails": [
      {
        "vmId": "vm_001",
        "status": "SUCCESS",
        "distributedAt": "2024-09-10T10:01:15Z",
        "verificationStatus": "VERIFIED",
        "checksum": "sha256:abcd1234..."
      },
      {
        "vmId": "vm_002",
        "status": "SUCCESS",
        "distributedAt": "2024-09-10T10:02:30Z",
        "verificationStatus": "VERIFIED",
        "checksum": "sha256:abcd1234..."
      },
      {
        "vmId": "vm_003",
        "status": "SUCCESS",
        "distributedAt": "2024-09-10T10:04:30Z",
        "verificationStatus": "VERIFIED",
        "checksum": "sha256:abcd1234..."
      }
    ]
  }
}
```

### 2.6 下载初始模型
下载初始模型文件

**接口信息**
- **URL**: `GET /api/model/initial/{taskId}/download`
- **描述**: 下载初始模型文件
- **认证**: 需要JWT Token
- **响应类型**: application/octet-stream

**路径参数**
- `taskId` (string, required): 联邦学习任务ID

**查询参数**
- `format` (string, optional): 下载格式 (binary|json)，默认binary

**响应**
- 成功时返回模型文件的二进制数据
- Content-Type: application/octet-stream
- Content-Disposition: attachment; filename="initial_model_{taskId}.pth"

### 2.7 删除初始模型
删除指定任务的初始模型

**接口信息**
- **URL**: `DELETE /api/model/initial/{taskId}`
- **描述**: 删除任务初始模型
- **认证**: 需要JWT Token (ADMIN权限)

**路径参数**
- `taskId` (string, required): 联邦学习任务ID

**查询参数**
- `force` (boolean, optional): 强制删除，即使模型已分发，默认false

**响应示例**
```json
{
  "code": 200,
  "message": "初始模型删除成功",
  "data": {
    "taskId": "task_001",
    "modelId": "initial_model_001",
    "deletedAt": "2024-09-10T10:00:00Z",
    "cleanupStatus": {
      "modelFileDeleted": true,
      "distributionRecordsCleared": true,
      "vmCachesCleared": 3
    }
  }
}
```

---

## 3. 数据模型定义

### 3.1 InitialModelInfo
```json
{
  "modelId": "string",           // 模型唯一标识
  "taskId": "string",            // 关联的任务ID
  "modelType": "string",         // 模型类型
  "modelSize": "number",         // 模型大小(字节)
  "architecture": "object",      // 模型架构参数
  "status": "string",           // 模型状态
  "createdAt": "string",        // 创建时间
  "checksum": "string"          // 模型校验和
}
```

### 3.2 DistributionProgress
```json
{
  "distributionId": "string",    // 分发任务ID
  "status": "string",           // 分发状态
  "progress": {
    "total": "number",          // 目标虚拟机总数
    "completed": "number",      // 完成分发数量
    "failed": "number",         // 失败数量
    "inProgress": "number"      // 进行中数量
  },
  "vmDetails": ["VmDistributionDetail"]
}
```

### 3.3 模型状态枚举
- `GENERATING`: 生成中
- `READY`: 就绪
- `UPLOADED`: 已上传
- `DISTRIBUTING`: 分发中
- `DISTRIBUTED`: 已分发
- `FAILED`: 失败
- `DELETED`: 已删除

### 3.4 分发状态枚举
- `PENDING`: 待开始
- `IN_PROGRESS`: 进行中
- `COMPLETED`: 已完成
- `FAILED`: 失败
- `CANCELLED`: 已取消

---

## 4. 错误码定义

### 4.1 初始模型相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| INITIAL_MODEL_NOT_FOUND | 404 | 初始模型不存在 |
| INITIAL_MODEL_GENERATION_FAILED | 500 | 模型生成失败 |
| INITIAL_MODEL_UPLOAD_FAILED | 400 | 模型上传失败 |
| INITIAL_MODEL_INVALID_FORMAT | 422 | 模型格式无效 |
| INITIAL_MODEL_ALREADY_EXISTS | 409 | 初始模型已存在 |
| INITIAL_MODEL_DISTRIBUTION_FAILED | 500 | 模型分发失败 |
| INITIAL_MODEL_CHECKSUM_MISMATCH | 400 | 模型校验失败 |
| INITIAL_MODEL_SIZE_EXCEEDED | 413 | 模型文件过大 |

### 4.2 权限相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| INSUFFICIENT_PERMISSION | 403 | 权限不足 |
| TASK_ACCESS_DENIED | 403 | 任务访问被拒绝 |

---

## 5. 使用示例

### 5.1 完整的初始模型创建流程

```javascript
// 1. 生成随机初始模型
const generateResponse = await fetch('/api/model/initial/generate', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    taskId: 'task_001',
    modelType: 'neural_network',
    architecture: {
      inputSize: 128,
      hiddenLayers: [64, 32, 16],
      outputSize: 10,
      activationFunction: 'relu',
      optimizer: 'adam',
      learningRate: 0.001
    }
  })
});

const model = await generateResponse.json();

// 2. 分发初始模型
const distributeResponse = await fetch(`/api/model/initial/${taskId}/distribute`, {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    vmIds: ['vm_001', 'vm_002', 'vm_003'],
    distributionMode: 'ASYNC',
    verifyChecksum: true
  })
});

const distribution = await distributeResponse.json();

// 3. 监控分发进度
const checkProgress = async (distributionId) => {
  const response = await fetch(`/api/model/initial/distribution/${distributionId}`, {
    method: 'GET',
    headers: {
      'Authorization': 'Bearer ' + token
    }
  });
  
  const status = await response.json();
  return status.data;
};
```

### 5.2 上传自定义初始模型

```javascript
// 上传自定义模型文件
const uploadCustomModel = async (taskId, modelFile) => {
  const formData = new FormData();
  formData.append('taskId', taskId);
  formData.append('modelType', 'neural_network');
  formData.append('description', '预训练的水声模型');
  formData.append('file', modelFile);
  formData.append('metadata', JSON.stringify({
    architecture: { inputSize: 128, outputSize: 10 },
    framework: 'pytorch',
    version: '1.0'
  }));

  const response = await fetch('/api/model/initial/upload', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token
    },
    body: formData
  });

  return await response.json();
};
```

---

## 6. 安全注意事项

### 6.1 模型文件安全
- 上传的模型文件需要进行病毒扫描
- 限制模型文件大小(默认最大100MB)
- 验证模型文件格式和完整性
- 使用SHA-256校验和确保传输完整性

### 6.2 权限控制
- 只有ADMIN和EDITOR权限的用户可以生成/上传初始模型
- 任务所有者和协作者可以查看初始模型信息
- 模型分发操作需要足够权限
- 敏感的模型架构信息需要权限控制

### 6.3 审计日志
- 记录所有初始模型的创建、分发、删除操作
- 跟踪模型访问和下载记录
- 监控异常的模型操作行为

---

## 7. 性能考虑

### 7.1 模型生成优化
- 使用异步生成大型模型
- 支持模型生成进度查询
- 缓存常用的模型架构模板

### 7.2 分发优化
- 支持并发分发到多个虚拟机
- 使用分块传输大模型文件
- 实现断点续传和失败重试
- 压缩模型文件以减少传输时间

### 7.3 存储优化
- 使用对象存储管理模型文件
- 实现模型文件的版本控制
- 定期清理过期的模型文件