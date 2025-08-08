# 水声联邦学习系统 模型版本管理 API 接口文档

## 1. 概述

本文档定义了水声联邦学习系统的模型版本管理相关HTTP REST API接口，包括模型上传、版本查询、性能评估、模型部署、模型回滚等功能。

### 1.1 基础信息
- **基础URL**: `http://localhost:8080/api/model`
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

## 2. 模型版本说明

### 2.1 模型类型
| 类型 | 说明 |
|------|------|
| GLOBAL | 全局模型，联邦学习聚合后的模型 |
| LOCAL | 本地模型，各参与方的本地训练模型 |

### 2.2 模型状态
| 状态 | 说明 |
|------|------|
| UPLOADING | 上传中 |
| UPLOADED | 已上传 |
| VALIDATING | 验证中 |
| VALIDATED | 已验证 |
| DEPLOYED | 已部署 |
| DEPRECATED | 已废弃 |
| FAILED | 上传失败 |

### 2.3 支持的模型格式
- **PyTorch**: `.pth`, `.pt`
- **TensorFlow**: `.h5`, `.pb`, `.savedmodel`
- **ONNX**: `.onnx`
- **Pickle**: `.pkl`, `.pickle`
- **Joblib**: `.joblib`

## 3. 模型上传接口

### 3.1 模型文件上传

**接口地址**: `POST /api/model/upload`

**请求参数**:
- **Content-Type**: `multipart/form-data`

```json
{
  "taskId": "a1b2c3d4e5f678901234567890123456",           // 关联任务ID，必填
  "vmId": "b2c3d4e5f67890123456789012345678",             // 虚拟机ID（本地模型），可选
  "roundNumber": 1,             // 训练轮数，必填
  "modelType": "GLOBAL",        // 模型类型，必填，GLOBAL/LOCAL
  "description": "string",      // 模型描述，可选
  "parameters": {},             // 模型参数，可选，JSON格式
  "file": "binary"              // 模型文件，必填
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "模型上传成功",
  "data": {
    "modelId": "c3d4e5f6789012345678901234567890",
    "taskId": "a1b2c3d4e5f678901234567890123456",
    "vmId": "b2c3d4e5f67890123456789012345678",
    "roundNumber": 1,
    "modelType": "GLOBAL",
    "modelPath": "/models/a1b2c3d4e5f678901234567890123456/global_round_1.pth",
    "modelSize": 1048576,
    "status": "UPLOADED",
    "description": "第1轮全局模型",
    "parameters": {
      "learning_rate": 0.001,
      "batch_size": 32
    },
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "模型上传失败",
  "data": {
    "field": "file",
    "error": "文件格式不支持"
  }
}
```

### 3.2 批量模型上传

**接口地址**: `POST /api/model/upload/batch`

**请求参数**:
- **Content-Type**: `multipart/form-data`

```json
{
  "taskId": "a1b2c3d4e5f678901234567890123456",           // 关联任务ID，必填
  "models": [                   // 模型列表，必填
    {
      "vmId": "b2c3d4e5f67890123456789012345678",         // 虚拟机ID，可选
      "roundNumber": 1,             // 训练轮数，必填
      "modelType": "LOCAL",         // 模型类型，必填
      "description": "string",      // 模型描述，可选
      "parameters": {},             // 模型参数，可选
      "file": "binary"              // 模型文件，必填
    }
  ]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "批量上传成功",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "models": [
      {
        "modelId": "c3d4e5f6789012345678901234567890",
        "status": "UPLOADED",
        "message": "上传成功"
      }
    ]
  }
}
```

## 4. 模型版本查询接口

### 4.1 模型版本列表查询

**接口地址**: `GET /api/model/versions`

**请求参数**:
```
?taskId=a1b2c3d4e5f678901234567890123456&vmId=b2c3d4e5f67890123456789012345678&modelType=GLOBAL&roundNumber=1&status=UPLOADED&page=1&size=10&sort=createdAt&order=desc
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | string | 否 | 任务ID过滤，32位UUID格式 |
| vmId | string | 否 | 虚拟机ID过滤，32位UUID格式 |
| modelType | string | 否 | 模型类型过滤 |
| roundNumber | int | 否 | 训练轮数过滤 |
| status | string | 否 | 状态过滤 |
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页大小，默认10 |
| sort | string | 否 | 排序字段，默认createdAt |
| order | string | 否 | 排序方向，desc/asc |

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 100,
    "pages": 10,
    "current": 1,
    "size": 10,
    "records": [
      {
        "modelId": "c3d4e5f6789012345678901234567890",
        "taskId": "a1b2c3d4e5f678901234567890123456",
        "vmId": "b2c3d4e5f67890123456789012345678",
        "roundNumber": 1,
        "modelType": "GLOBAL",
        "modelPath": "/models/a1b2c3d4e5f678901234567890123456/global_round_1.pth",
        "modelSize": 1048576,
        "accuracy": 0.8500,
        "loss": 0.123456,
        "status": "UPLOADED",
        "description": "第1轮全局模型",
        "parameters": {
          "learning_rate": 0.001,
          "batch_size": 32
        },
        "createdAt": "2024-01-01T10:00:00"
      }
    ]
  }
}
```

### 4.2 模型版本详情查询

**接口地址**: `GET /api/model/versions/{modelId}`

**路径参数**:
- modelId: 模型版本ID，32位UUID格式

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "modelId": "c3d4e5f6789012345678901234567890",
    "taskId": "a1b2c3d4e5f678901234567890123456",
    "vmId": "b2c3d4e5f67890123456789012345678",
    "roundNumber": 1,
    "modelType": "GLOBAL",
    "modelPath": "/models/a1b2c3d4e5f678901234567890123456/global_round_1.pth",
    "modelSize": 1048576,
    "accuracy": 0.8500,
    "loss": 0.123456,
    "status": "UPLOADED",
    "description": "第1轮全局模型",
    "parameters": {
      "learning_rate": 0.001,
      "batch_size": 32
    },
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 4.3 任务模型版本查询

**接口地址**: `GET /api/model/versions/task/{taskId}`

**路径参数**:
- taskId: 任务ID

**请求参数**:
```
?modelType=GLOBAL&roundNumber=1&status=UPLOADED&sort=roundNumber&order=asc
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "a1b2c3d4e5f678901234567890123456",
    "taskName": "水声分类任务",
    "totalModels": 50,
    "globalModels": 10,
    "localModels": 40,
    "versions": [
      {
        "modelId": "c3d4e5f6789012345678901234567890",
        "roundNumber": 1,
        "modelType": "GLOBAL",
        "accuracy": 0.8500,
        "loss": 0.123456,
        "status": "UPLOADED",
        "createdAt": "2024-01-01T10:00:00"
      }
    ]
  }
}
```

## 5. 模型性能评估接口

### 5.1 模型性能评估

**接口地址**: `POST /api/model/evaluate`

**请求参数**:
```json
{
  "modelId": "c3d4e5f6789012345678901234567890",          // 模型ID，必填，32位UUID格式
  "testDataPath": "string",     // 测试数据路径，必填
  "metrics": ["accuracy", "loss", "precision", "recall", "f1"], // 评估指标，可选
  "batchSize": 32,              // 批次大小，可选，默认32
  "device": "cpu"               // 计算设备，可选，默认cpu
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "评估完成",
  "data": {
    "modelId": "c3d4e5f6789012345678901234567890",
    "evaluationId": "eval_1234567890",
    "metrics": {
      "accuracy": 0.8500,
      "loss": 0.123456,
      "precision": 0.8200,
      "recall": 0.8300,
      "f1": 0.8250
    },
    "evaluationTime": 15.5,
    "testSamples": 1000,
    "status": "COMPLETED",
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 5.2 批量模型评估

**接口地址**: `POST /api/model/evaluate/batch`

**请求参数**:
```json
{
  "taskId": "a1b2c3d4e5f678901234567890123456",           // 任务ID，必填，32位UUID格式
  "testDataPath": "string",     // 测试数据路径，必填
  "modelType": "GLOBAL",        // 模型类型，可选
  "roundNumbers": [1, 5, 10],   // 评估轮数，可选
  "metrics": ["accuracy", "loss"], // 评估指标，可选
  "batchSize": 32               // 批次大小，可选
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "批量评估完成",
  "data": {
    "taskId": "a1b2c3d4e5f678901234567890123456",
    "evaluatedCount": 3,
    "results": [
      {
        "modelId": "c3d4e5f6789012345678901234567890",
        "roundNumber": 1,
        "modelType": "GLOBAL",
        "accuracy": 0.8500,
        "loss": 0.123456,
        "status": "COMPLETED"
      }
    ]
  }
}
```

### 5.3 评估结果查询

**接口地址**: `GET /api/model/evaluate/results`

**请求参数**:
```
?modelId=c3d4e5f6789012345678901234567890&taskId=a1b2c3d4e5f678901234567890123456&evaluationId=eval_1234567890&page=1&size=10
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 50,
    "pages": 5,
    "current": 1,
    "size": 10,
    "records": [
      {
        "evaluationId": "eval_1234567890",
        "modelId": "c3d4e5f6789012345678901234567890",
        "taskId": "a1b2c3d4e5f678901234567890123456",
        "metrics": {
          "accuracy": 0.8500,
          "loss": 0.123456
        },
        "evaluationTime": 15.5,
        "testSamples": 1000,
        "status": "COMPLETED",
        "createdAt": "2024-01-01T10:00:00"
      }
    ]
  }
}
```

## 6. 模型部署接口

### 6.1 模型部署

**接口地址**: `POST /api/model/deploy`

**请求参数**:
```json
{
  "modelId": "c3d4e5f6789012345678901234567890",          // 模型ID，必填，32位UUID格式
  "deploymentName": "string",   // 部署名称，必填
  "targetVms": ["vm_1", "vm_2"], // 目标虚拟机列表，可选
  "deploymentConfig": {         // 部署配置，可选
    "replicas": 2,
    "resources": {
      "cpu": "1",
      "memory": "2Gi"
    },
    "environment": {
      "MODEL_PATH": "/models/deployed",
      "BATCH_SIZE": "32"
    }
  },
  "description": "string"       // 部署描述，可选
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "部署成功",
  "data": {
    "deploymentId": "deploy_1234567890",
    "modelId": "c3d4e5f6789012345678901234567890",
    "deploymentName": "水声分类模型_v1.0",
    "targetVms": ["vm_1", "vm_2"],
    "status": "DEPLOYED",
    "deploymentConfig": {
      "replicas": 2,
      "resources": {
        "cpu": "1",
        "memory": "2Gi"
      }
    },
    "endpoints": [
      "http://vm_1:8080/predict",
      "http://vm_2:8080/predict"
    ],
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 6.2 部署状态查询

**接口地址**: `GET /api/model/deploy/status/{deploymentId}`

**路径参数**:
- deploymentId: 部署ID

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "deploymentId": "deploy_1234567890",
    "modelId": "c3d4e5f6789012345678901234567890",
    "deploymentName": "水声分类模型_v1.0",
    "status": "RUNNING",
    "replicas": {
      "desired": 2,
      "available": 2,
      "ready": 2
    },
    "endpoints": [
      "http://vm_1:8080/predict",
      "http://vm_2:8080/predict"
    ],
    "healthCheck": {
      "status": "HEALTHY",
      "lastCheck": "2024-01-01T10:00:00",
      "responseTime": 50
    },
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
}
```

### 6.3 部署列表查询

**接口地址**: `GET /api/model/deploy/list`

**请求参数**:
```
?modelId=c3d4e5f6789012345678901234567890&status=RUNNING&page=1&size=10
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 20,
    "pages": 2,
    "current": 1,
    "size": 10,
    "records": [
      {
        "deploymentId": "deploy_1234567890",
        "modelId": "c3d4e5f6789012345678901234567890",
        "deploymentName": "水声分类模型_v1.0",
        "status": "RUNNING",
        "replicas": {
          "desired": 2,
          "available": 2
        },
        "createdAt": "2024-01-01T10:00:00"
      }
    ]
  }
}
```

## 7. 模型回滚接口

### 7.1 模型回滚

**接口地址**: `POST /api/model/rollback`

**请求参数**:
```json
{
  "deploymentId": "deploy_1234567890",     // 部署ID，必填
  "targetModelId": "c3d4e5f6789012345678901234567891",    // 目标模型ID，必填
  "rollbackReason": "string",   // 回滚原因，可选
  "force": false                // 强制回滚，可选，默认false
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "回滚成功",
  "data": {
    "rollbackId": "rollback_1234567890",
    "deploymentId": "deploy_1234567890",
    "fromModelId": "c3d4e5f6789012345678901234567890",
    "toModelId": "c3d4e5f6789012345678901234567891",
    "status": "COMPLETED",
    "rollbackReason": "性能下降",
    "rollbackTime": 30.5,
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 7.2 回滚历史查询

**接口地址**: `GET /api/model/rollback/history`

**请求参数**:
```
?deploymentId=deploy_1234567890&page=1&size=10
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 15,
    "pages": 2,
    "current": 1,
    "size": 10,
    "records": [
      {
        "rollbackId": "rollback_1234567890",
        "deploymentId": "deploy_1234567890",
        "fromModelId": "c3d4e5f6789012345678901234567890",
        "toModelId": "c3d4e5f6789012345678901234567891",
        "status": "COMPLETED",
        "rollbackReason": "性能下降",
        "rollbackTime": 30.5,
        "createdAt": "2024-01-01T10:00:00"
      }
    ]
  }
}
```

## 8. 模型下载接口

### 8.1 模型文件下载

**接口地址**: `GET /api/model/download/{modelId}`

**路径参数**:
- modelId: 模型ID

**请求参数**:
```
?format=original&compressed=true
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| format | string | 否 | 下载格式，original/onnx，默认original |
| compressed | boolean | 否 | 是否压缩，默认true |

**响应**: 文件流

### 8.2 批量模型下载

**接口地址**: `POST /api/model/download/batch`

**请求参数**:
```json
{
  "modelIds": ["c3d4e5f6789012345678901234567890", "d4e5f678901234567890123456789012"], // 模型ID列表，必填
  "format": "original",               // 下载格式，可选
  "compressed": true                  // 是否压缩，可选
}
```

**响应**: ZIP文件流

## 9. 模型删除接口

### 9.1 模型版本删除

**接口地址**: `DELETE /api/model/versions/{modelId}`

**路径参数**:
- modelId: 模型ID，32位UUID格式

**请求参数**:
```json
{
  "force": false,              // 强制删除，可选，默认false
  "deleteFile": true           // 是否删除文件，可选，默认true
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": {
    "modelId": "c3d4e5f6789012345678901234567890",
    "deletedAt": "2024-01-01T10:00:00"
  }
}
```

### 9.2 批量模型删除

**接口地址**: `DELETE /api/model/versions/batch`

**请求参数**:
```json
{
  "modelIds": ["c3d4e5f6789012345678901234567890", "d4e5f678901234567890123456789012"], // 模型ID列表，必填
  "force": false,                     // 强制删除，可选
  "deleteFile": true                  // 是否删除文件，可选
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": {
    "successCount": 2,
    "failedCount": 0,
    "results": [
      {
        "modelId": "c3d4e5f6789012345678901234567890",
        "status": "DELETED",
        "message": "删除成功"
      }
    ]
  }
}
```

## 10. 模型统计接口

### 10.1 模型统计信息

**接口地址**: `GET /api/model/statistics`

**请求参数**:
```
?taskId=a1b2c3d4e5f678901234567890123456&timeRange=7d
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | string | 否 | 任务ID过滤，32位UUID格式 |
| timeRange | string | 否 | 时间范围，7d/30d/90d |

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "totalModels": 150,
    "globalModels": 30,
    "localModels": 120,
    "totalSize": 1073741824,
    "averageAccuracy": 0.8500,
    "averageLoss": 0.123456,
    "uploadTrend": [
      {
        "date": "2024-01-01",
        "count": 5,
        "size": 10485760
      }
    ],
    "accuracyTrend": [
      {
        "round": 1,
        "accuracy": 0.8000,
        "loss": 0.150000
      }
    ]
  }
}
```

### 10.2 任务模型统计

**接口地址**: `GET /api/model/statistics/task/{taskId}`

**路径参数**:
- taskId: 任务ID

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "a1b2c3d4e5f678901234567890123456",
    "taskName": "水声分类任务",
    "totalRounds": 100,
    "completedRounds": 50,
    "modelCounts": {
      "global": 10,
      "local": 40
    },
    "performanceMetrics": {
      "bestAccuracy": 0.9000,
      "bestRound": 45,
      "averageAccuracy": 0.8500,
      "accuracyImprovement": 0.0500
    },
    "sizeMetrics": {
      "totalSize": 1073741824,
      "averageSize": 20971520
    }
  }
}
```

## 11. 错误码定义

### 11.1 模型版本管理错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| MODEL_NOT_FOUND | 404 | 模型不存在 |
| MODEL_UPLOAD_FAILED | 500 | 模型上传失败 |
| MODEL_FORMAT_UNSUPPORTED | 400 | 不支持的模型格式 |
| MODEL_SIZE_EXCEEDED | 400 | 模型文件过大 |
| MODEL_VALIDATION_FAILED | 422 | 模型验证失败 |
| MODEL_DEPLOYMENT_FAILED | 500 | 模型部署失败 |
| MODEL_ROLLBACK_FAILED | 500 | 模型回滚失败 |
| MODEL_DELETE_FAILED | 500 | 模型删除失败 |
| EVALUATION_FAILED | 500 | 模型评估失败 |
| DEPLOYMENT_NOT_FOUND | 404 | 部署不存在 |
| DEPLOYMENT_IN_USE | 409 | 部署正在使用中 |

## 12. 安全规范

### 12.1 文件上传安全
- 限制文件大小：最大100MB
- 验证文件格式：只允许指定格式
- 扫描恶意代码：上传前进行安全扫描
- 存储路径隔离：不同用户模型存储在不同目录

### 12.2 访问控制
- 模型访问权限：基于用户角色和任务权限
- 部署权限：只有RESEARCHER和ADMIN可以部署模型
- 删除权限：只有模型创建者和ADMIN可以删除

### 12.3 数据保护
- 模型文件加密存储
- 传输过程使用HTTPS
- 定期备份重要模型
- 敏感信息脱敏处理

## 13. 性能规范

### 13.1 上传性能
- 支持断点续传
- 大文件分片上传
- 并发上传限制：每个用户最多3个并发上传

### 13.2 查询性能
- 模型列表查询响应时间 < 500ms
- 详情查询响应时间 < 200ms
- 支持分页和缓存

### 13.3 评估性能
- 异步评估处理
- 评估结果缓存
- 支持批量评估

## 14. 相关文档

- [HTTP接口导览](./HTTP接口导览.md) - 系统API接口总览
- [用户管理API参考文档](./user-api-reference.md) - 用户管理相关接口
- [联邦学习任务管理API参考文档](./federated-task-api-reference.md) - 联邦学习任务管理相关接口
- [训练数据管理API参考文档](./training-data-api-reference.md) - 训练数据管理相关接口
- [数据库表结构文档](../../database/database_schema.md) - 数据库设计
- [WebSocket协议文档](../WebSocket/) - 实时通信协议 