# 联邦学习任务管理 API 参考文档

## 1. 概述

本文档定义了水声联邦学习系统的联邦学习任务管理API接口，包括任务创建、配置、控制、监控等功能。

### 1.1 基础信息
- **基础URL**: `http://localhost:8080/api/federated`
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

## 2. 任务状态定义

### 2.1 任务状态枚举
| 状态 | 说明 |
|------|------|
| CREATED | 已创建，等待配置 |
| CONFIGURED | 已配置，等待启动 |
| RUNNING | 运行中 |
| PAUSED | 已暂停 |
| STOPPED | 已停止 |
| COMPLETED | 已完成 |
| FAILED | 执行失败 |
| CANCELLED | 已取消 |

### 2.2 任务类型枚举
| 类型 | 说明 |
|------|------|
| CLASSIFICATION | 分类任务 |
| REGRESSION | 回归任务 |
| CLUSTERING | 聚类任务 |
| ANOMALY_DETECTION | 异常检测 |

## 3. API接口定义

### 3.1 任务创建接口

**接口地址**: `POST /api/federated/tasks`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "taskName": "水声传播特征分类任务",
  "taskType": "CLASSIFICATION",
  "description": "基于声学传播特征的水声目标分类",
  "algorithm": "FEDERATED_AVERAGING",
  "participants": [
    {
      "vmId": "vm-001",
      "role": "PARTICIPANT",
      "dataSource": "bellhop_features_001.csv"
    },
    {
      "vmId": "vm-002", 
      "role": "PARTICIPANT",
      "dataSource": "bellhop_features_002.csv"
    }
  ],
  "hyperparameters": {
    "learningRate": 0.01,
    "batchSize": 32,
    "epochs": 100,
    "rounds": 10,
    "minParticipants": 2
  },
  "modelConfig": {
    "modelType": "RANDOM_FOREST",
    "featureColumns": ["feature_1", "feature_2", "feature_3"],
    "targetColumn": "target_class",
    "testSize": 0.2,
    "randomState": 42
  },
  "schedule": {
    "startTime": "2024-01-01T10:00:00",
    "endTime": "2024-01-01T18:00:00",
    "timeout": 3600
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "任务创建成功",
  "data": {
    "taskId": "task_1234567890",
    "taskName": "水声传播特征分类任务",
    "status": "CREATED",
    "createdAt": "2024-01-01T09:00:00.000Z",
    "createdBy": "admin",
    "participantCount": 2,
    "estimatedDuration": 3600
  }
}
```

### 3.2 任务配置接口

**接口地址**: `PUT /api/federated/tasks/{taskId}/config`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "algorithm": "FEDERATED_AVERAGING",
  "hyperparameters": {
    "learningRate": 0.01,
    "batchSize": 64,
    "epochs": 150,
    "rounds": 15,
    "minParticipants": 2,
    "aggregationMethod": "WEIGHTED_AVERAGE"
  },
  "modelConfig": {
    "modelType": "RANDOM_FOREST",
    "nEstimators": 100,
    "maxDepth": 10,
    "minSamplesSplit": 2,
    "minSamplesLeaf": 1
  },
  "dataConfig": {
    "preprocessing": {
      "normalization": "STANDARD_SCALER",
      "featureSelection": "VARIANCE_THRESHOLD",
      "outlierRemoval": true
    },
    "validation": {
      "crossValidation": "K_FOLD",
      "kFolds": 5,
      "stratified": true
    }
  },
  "securityConfig": {
    "encryption": "AES_256",
    "differentialPrivacy": {
      "enabled": true,
      "epsilon": 1.0,
      "delta": 0.0001
    },
    "secureAggregation": true
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "任务配置成功",
  "data": {
    "taskId": "task_1234567890",
    "status": "CONFIGURED",
    "updatedAt": "2024-01-01T09:30:00.000Z",
    "configVersion": "v1.1"
  }
}
```

### 3.3 任务启动接口

**接口地址**: `POST /api/federated/tasks/{taskId}/start`

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "任务启动成功",
  "data": {
    "taskId": "task_1234567890",
    "status": "RUNNING",
    "startedAt": "2024-01-01T10:00:00.000Z",
    "currentRound": 0,
    "participants": [
      {
        "vmId": "vm-001",
        "status": "CONNECTED",
        "lastHeartbeat": "2024-01-01T10:00:00.000Z"
      },
      {
        "vmId": "vm-002",
        "status": "CONNECTED", 
        "lastHeartbeat": "2024-01-01T10:00:00.000Z"
      }
    ]
  }
}
```

### 3.4 任务暂停接口

**接口地址**: `POST /api/federated/tasks/{taskId}/pause`

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "任务暂停成功",
  "data": {
    "taskId": "task_1234567890",
    "status": "PAUSED",
    "pausedAt": "2024-01-01T12:00:00.000Z",
    "currentRound": 5,
    "resumePoint": {
      "round": 5,
      "step": "AGGREGATION"
    }
  }
}
```

### 3.5 任务恢复接口

**接口地址**: `POST /api/federated/tasks/{taskId}/resume`

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "任务恢复成功",
  "data": {
    "taskId": "task_1234567890",
    "status": "RUNNING",
    "resumedAt": "2024-01-01T13:00:00.000Z",
    "currentRound": 5
  }
}
```

### 3.6 任务停止接口

**接口地址**: `POST /api/federated/tasks/{taskId}/stop`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```json
{
  "reason": "用户主动停止",
  "saveCheckpoint": true
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "任务停止成功",
  "data": {
    "taskId": "task_1234567890",
    "status": "STOPPED",
    "stoppedAt": "2024-01-01T14:00:00.000Z",
    "finalRound": 8,
    "checkpointSaved": true,
    "checkpointPath": "/checkpoints/task_1234567890_round_8.pkl"
  }
}
```

### 3.7 任务取消接口

**接口地址**: `POST /api/federated/tasks/{taskId}/cancel`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```json
{
  "reason": "任务配置错误"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "任务取消成功",
  "data": {
    "taskId": "task_1234567890",
    "status": "CANCELLED",
    "cancelledAt": "2024-01-01T15:00:00.000Z",
    "reason": "任务配置错误"
  }
}
```

### 3.8 任务状态查询接口

**接口地址**: `GET /api/federated/tasks/{taskId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "task_1234567890",
    "taskName": "水声传播特征分类任务",
    "taskType": "CLASSIFICATION",
    "status": "RUNNING",
    "algorithm": "FEDERATED_AVERAGING",
    "createdAt": "2024-01-01T09:00:00.000Z",
    "startedAt": "2024-01-01T10:00:00.000Z",
    "currentRound": 5,
    "totalRounds": 15,
    "progress": 33.33,
    "participants": [
      {
        "vmId": "vm-001",
        "role": "PARTICIPANT",
        "status": "TRAINING",
        "lastHeartbeat": "2024-01-01T12:30:00.000Z",
        "currentEpoch": 45,
        "loss": 0.234,
        "accuracy": 0.876
      },
      {
        "vmId": "vm-002",
        "role": "PARTICIPANT", 
        "status": "TRAINING",
        "lastHeartbeat": "2024-01-01T12:30:00.000Z",
        "currentEpoch": 42,
        "loss": 0.256,
        "accuracy": 0.854
      }
    ],
    "metrics": {
      "globalLoss": 0.245,
      "globalAccuracy": 0.865,
      "communicationRounds": 5,
      "dataProcessed": 15000,
      "estimatedTimeRemaining": 1800
    }
  }
}
```

### 3.9 任务列表查询接口

**接口地址**: `GET /api/federated/tasks`

**请求头**:
```
Authorization: Bearer {token}
```

**查询参数**:
- `page`: 页码 (默认: 1)
- `size`: 每页大小 (默认: 20)
- `status`: 任务状态过滤
- `type`: 任务类型过滤
- `startDate`: 开始日期
- `endDate`: 结束日期
- `keyword`: 关键词搜索

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 25,
    "page": 1,
    "size": 20,
    "tasks": [
      {
        "taskId": "task_1234567890",
        "taskName": "水声传播特征分类任务",
        "taskType": "CLASSIFICATION",
        "status": "RUNNING",
        "createdAt": "2024-01-01T09:00:00.000Z",
        "startedAt": "2024-01-01T10:00:00.000Z",
        "participantCount": 2,
        "currentRound": 5,
        "totalRounds": 15,
        "progress": 33.33
      },
      {
        "taskId": "task_1234567891",
        "taskName": "声学传播回归分析",
        "taskType": "REGRESSION",
        "status": "COMPLETED",
        "createdAt": "2024-01-01T08:00:00.000Z",
        "startedAt": "2024-01-01T08:30:00.000Z",
        "completedAt": "2024-01-01T11:00:00.000Z",
        "participantCount": 3,
        "finalAccuracy": 0.892
      }
    ]
  }
}
```

### 3.10 任务结果查询接口

**接口地址**: `GET /api/federated/tasks/{taskId}/results`

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "task_1234567890",
    "taskName": "水声传播特征分类任务",
    "status": "COMPLETED",
    "finalMetrics": {
      "accuracy": 0.892,
      "precision": 0.885,
      "recall": 0.901,
      "f1Score": 0.893,
      "confusionMatrix": [
        [45, 5],
        [3, 47]
      ]
    },
    "trainingHistory": {
      "rounds": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
      "loss": [0.456, 0.345, 0.289, 0.234, 0.198, 0.167, 0.145, 0.123, 0.108, 0.098],
      "accuracy": [0.654, 0.723, 0.789, 0.834, 0.867, 0.889, 0.901, 0.912, 0.918, 0.925]
    },
    "participantResults": [
      {
        "vmId": "vm-001",
        "finalAccuracy": 0.889,
        "finalLoss": 0.102,
        "dataProcessed": 8000,
        "trainingTime": 1800
      },
      {
        "vmId": "vm-002",
        "finalAccuracy": 0.895,
        "finalLoss": 0.094,
        "dataProcessed": 7000,
        "trainingTime": 1650
      }
    ],
    "modelArtifacts": {
      "modelPath": "/models/task_1234567890_final.pkl",
      "modelSize": "2.5MB",
      "featureImportance": [
        {"feature": "feature_1", "importance": 0.234},
        {"feature": "feature_2", "importance": 0.189},
        {"feature": "feature_3", "importance": 0.156}
      ]
    },
    "completedAt": "2024-01-01T16:00:00.000Z",
    "totalDuration": 3600
  }
}
```

### 3.11 任务日志查询接口

**接口地址**: `GET /api/federated/tasks/{taskId}/logs`

**请求头**:
```
Authorization: Bearer {token}
```

**查询参数**:
- `level`: 日志级别 (INFO, WARN, ERROR)
- `startTime`: 开始时间
- `endTime`: 结束时间
- `page`: 页码
- `size`: 每页大小

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 150,
    "page": 1,
    "size": 50,
    "logs": [
      {
        "timestamp": "2024-01-01T10:00:00.000Z",
        "level": "INFO",
        "message": "任务启动成功",
        "source": "TASK_MANAGER",
        "details": {
          "participants": 2,
          "algorithm": "FEDERATED_AVERAGING"
        }
      },
      {
        "timestamp": "2024-01-01T10:05:00.000Z",
        "level": "INFO",
        "message": "第1轮训练开始",
        "source": "ROUND_MANAGER",
        "details": {
          "round": 1,
          "participants": ["vm-001", "vm-002"]
        }
      },
      {
        "timestamp": "2024-01-01T10:15:00.000Z",
        "level": "WARN",
        "message": "虚拟机vm-001响应超时",
        "source": "PARTICIPANT_MANAGER",
        "details": {
          "vmId": "vm-001",
          "timeout": 300
        }
      }
    ]
  }
}
```

### 3.12 任务删除接口

**接口地址**: `DELETE /api/federated/tasks/{taskId}`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```json
{
  "deleteData": true,
  "deleteModel": false
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "任务删除成功",
  "data": {
    "taskId": "task_1234567890",
    "deletedAt": "2024-01-01T17:00:00.000Z",
    "dataDeleted": true,
    "modelPreserved": true
  }
}
```

## 4. 错误码定义

### 4.1 任务相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| TASK_NOT_FOUND | 404 | 任务不存在 |
| TASK_ALREADY_EXISTS | 409 | 任务已存在 |
| TASK_INVALID_STATUS | 400 | 任务状态错误 |
| TASK_CONFIG_ERROR | 400 | 任务配置错误 |
| TASK_START_FAILED | 500 | 任务启动失败 |
| TASK_PAUSE_FAILED | 500 | 任务暂停失败 |
| TASK_RESUME_FAILED | 500 | 任务恢复失败 |
| TASK_STOP_FAILED | 500 | 任务停止失败 |
| TASK_CANCEL_FAILED | 500 | 任务取消失败 |
| TASK_DELETE_FAILED | 500 | 任务删除失败 |

### 4.2 参与者相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| PARTICIPANT_NOT_FOUND | 404 | 参与者不存在 |
| PARTICIPANT_OFFLINE | 503 | 参与者离线 |
| PARTICIPANT_TIMEOUT | 408 | 参与者响应超时 |
| PARTICIPANT_ERROR | 500 | 参与者执行错误 |
| INSUFFICIENT_PARTICIPANTS | 400 | 参与者数量不足 |

### 4.3 模型相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| MODEL_NOT_FOUND | 404 | 模型不存在 |
| MODEL_LOAD_ERROR | 500 | 模型加载失败 |
| MODEL_SAVE_ERROR | 500 | 模型保存失败 |
| MODEL_VERSION_ERROR | 400 | 模型版本错误 |
| MODEL_COMPATIBILITY_ERROR | 400 | 模型兼容性错误 |

### 4.4 数据相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| DATA_NOT_FOUND | 404 | 数据不存在 |
| DATA_FORMAT_ERROR | 400 | 数据格式错误 |
| DATA_SIZE_ERROR | 400 | 数据大小错误 |
| DATA_PREPROCESSING_ERROR | 500 | 数据预处理失败 |
| DATA_VALIDATION_ERROR | 400 | 数据验证失败 |

## 5. 安全规范

### 5.1 认证授权
- 所有API接口都需要JWT Token认证
- Token过期时间为24小时
- 支持Token刷新机制
- 任务操作需要相应权限

### 5.2 数据验证
- 所有输入参数都需要进行格式验证
- 任务配置参数需要类型和范围验证
- 对敏感数据进行加密存储
- 支持数据完整性校验

### 5.3 访问控制
- 基于角色的访问控制(RBAC)
- 任务创建需要管理员权限
- 任务操作需要相应权限
- 记录所有操作日志

### 5.4 网络安全
- 使用HTTPS协议传输
- 支持API限流和防DDoS攻击
- 定期更新安全补丁
- 支持安全聚合协议

## 6. 性能规范

### 6.1 响应时间
- 查询接口响应时间 < 200ms
- 创建接口响应时间 < 1000ms
- 控制接口响应时间 < 2000ms
- 结果查询接口响应时间 < 500ms

### 6.2 并发处理
- 支持100个并发任务
- 每个任务支持50个参与者
- 使用连接池管理数据库连接
- 异步处理非关键操作

### 6.3 缓存策略
- 使用Redis缓存任务状态
- 任务状态缓存10秒
- 配置信息缓存1小时
- 结果数据缓存24小时

## 7. 相关文档

- [HTTP接口导览.md](./HTTP接口导览.md) - 系统整体API接口
- [用户管理API参考文档](./user-api-reference.md) - 用户管理相关接口
- [虚拟机API参考文档](./vm-api-reference.md) - 虚拟机管理相关接口
- [数据库表结构文档](../../database/database_schema.md) - 联邦学习任务相关数据库设计
- [WebSocket协议文档](../WebSocket/) - 实时通信协议 