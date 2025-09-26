# 联邦学习任务管理 API 参考文档

## 1. 概述

本文档定义了水声联邦学习系统的联邦学习任务管理API接口，包括任务创建、配置、控制、监控等功能。

### 1.1 基础信息
- **基础URL**: `http://localhost:8080/api/federated`
- **API版本**: v1.4
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

### ⚠️ 版本兼容性说明

**v1.3 版本更新内容**:
- 新增图形化任务创建支持的配置接口组
- 增强任务创建接口，支持智能数据集和参与者配置
- 废弃简化的参与者配置格式，推荐使用新的 `datasetConfig` 和 `participantConfig`
- 新增实时预览、验证和监控功能

**向后兼容性**:
- v1.3 版本仍支持 v1.0 的 `participants` 数组格式，但会显示废弃警告
- v2.0 版本将完全移除对旧格式的支持
- 详细的迁移指南请参考：[废弃接口文档](../removed/removed-interfaces-v1.3.md)
- 新增接口详情请参考：[修改接口文档](../modified/modified-interfaces-v1.3.md)

---

### 🆕 3.0 图形化配置接口组 (v1.3 新增)

#### 3.0.1 获取可用虚拟机列表
**接口地址**: `GET /api/federated/config/available-vms`

**请求头**:
```
Authorization: Bearer {token}
```

**查询参数**:
- `algorithm`: 算法类型过滤 (可选)
- `minCpuCores`: 最小CPU核心数 (可选)
- `minMemoryMb`: 最小内存(MB) (可选)
- `status`: 虚拟机状态过滤 (可选)
- `capabilities`: 能力要求 (可选)

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 5,
    "availableVms": [
      {
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "name": "水声联邦学习节点-001",
        "ipAddress": "192.168.1.100",
        "status": "RUNNING",
        "resources": {
          "cpuCores": 8,
          "memoryMb": 16384,
          "gpuCount": 1
        },
        "capabilities": ["GPU", "HIGH_MEMORY"]
      }
    ]
  }
}
```

#### 3.0.2 获取可用数据集列表
**接口地址**: `GET /api/federated/config/available-datasets`

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
    "total": 15,
    "availableDatasets": [
      {
        "datasetId": "e5f67890123456789012345678901234",
        "name": "水声传播特征数据集_v1.0",
        "status": "READY",
        "statistics": {
          "totalRows": 10000,
          "totalColumns": 128,
          "fileSizeFormatted": "43.5MB"
        }
      }
    ]
  }
}
```

#### 3.0.3 数据分配预览
**接口地址**: `POST /api/federated/tasks/preview-distribution`

**请求参数**:
```json
{
  "datasetId": "e5f67890123456789012345678901234",
  "distributionStrategy": "BALANCED",
  "participants": [
    {
      "vmId": "a1b2c3d4e5f678901234567890123456",
      "requestedRatio": 0.6
    }
  ]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "预览生成成功",
  "data": {
    "distributionResult": {
      "participants": [
        {
          "vmId": "a1b2c3d4e5f678901234567890123456",
          "allocatedRatio": 0.65,
          "allocatedRows": 5200,
          "estimatedTrainingTime": 450
        }
      ]
    },
    "qualityMetrics": {
      "iidScore": 0.85,
      "balanceScore": 0.92
    }
  }
}
```

---

### 3.1 任务创建接口 (v1.3 增强)

> ⚠️ **兼容性注意**: v1.3 版本同时支持新旧两种参数格式，旧格式会显示废弃警告

**接口地址**: `POST /api/federated/tasks`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数** (v1.3 推荐格式):
```json
{
  "taskName": "水声传播特征分类任务",
  "taskType": "CLASSIFICATION",
  "description": "基于声学传播特征的水声目标分类",
  "algorithm": "FEDERATED_AVERAGING",

  // 🆕 v1.3: 智能数据集配置
  "datasetConfig": {
    "datasetId": "e5f67890123456789012345678901234",
    "distributionStrategy": "BALANCED",
    "distributionRatios": {
      "a1b2c3d4e5f678901234567890123456": 0.6,
      "b2c3d4e5f67890123456789012345678": 0.4
    },
    "validationSplit": 0.2,
    "testSplit": 0.1
  },

  // 🆕 v1.3: 智能参与者配置
  "participantConfig": {
    "selectionMode": "MANUAL",
    "requirements": {
      "minParticipants": 2,
      "maxParticipants": 10,
      "minCpuCores": 4,
      "minMemoryMb": 8192
    },
    "participants": [
      {
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "role": "PARTICIPANT",
        "dataRatio": 0.6,
        "capabilities": ["GPU"],
        "constraints": {
          "maxCpuUsage": 80,
          "maxMemoryUsage": 75
        }
      },
      {
        "vmId": "b2c3d4e5f67890123456789012345678",
        "role": "PARTICIPANT",
        "dataRatio": 0.4,
        "capabilities": ["TRAINING"]
      }
    ]
  },

  // 原有字段保持兼容
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

**⚠️ 旧格式 (v1.0 兼容，已废弃)**:
```json
{
  "taskName": "水声传播特征分类任务",
  "taskType": "CLASSIFICATION",
  "algorithm": "FEDERATED_AVERAGING",
  // ⚠️ 废弃: 简化的参与者配置
  "participants": [
    {
      "vmId": "a1b2c3d4e5f678901234567890123456",
      "role": "PARTICIPANT",
      "dataSource": "bellhop_features_001.csv"  // ⚠️ 已废弃字段
    }
  ],
  "hyperparameters": { /* ... */ }
}
```

**响应示例** (v1.3 增强版):
```json
{
  "code": 200,
  "message": "任务创建成功",
  "data": {
    "taskId": "c3d4e5f6789012345678901234567890",
    "taskName": "水声传播特征分类任务",
    "status": "CREATED",
    "createdAt": "2024-01-01T09:00:00.000Z",
    "createdBy": "d4e5f678901234567890123456789012",
    "participantCount": 2,
    "estimatedDuration": 46500,

    // 🆕 v1.3: 配置摘要
    "configSummary": {
      "dataset": {
        "datasetId": "e5f67890123456789012345678901234",
        "totalRows": 10000,
        "distributionStrategy": "BALANCED"
      },
      "participants": [
        {
          "vmId": "a1b2c3d4e5f678901234567890123456",
          "vmName": "水声联邦学习节点-001",
          "role": "PARTICIPANT",
          "dataRatio": 0.6,
          "status": "PENDING"
        }
      ]
    },

    // 🆕 v1.3: 预计性能指标
    "performanceEstimation": {
      "expectedAccuracy": 0.85,
      "convergenceRounds": 8,
      "networkTraffic": "2.3GB"
    }
  },

  // ⚠️ 废弃警告 (使用旧格式时出现)
  "warnings": [
    {
      "code": "SIMPLE_PARTICIPANT_CONFIG_DEPRECATED",
      "message": "简化的参与者配置格式已废弃，建议使用新的participantConfig结构",
      "details": {
        "deprecationVersion": "v1.3",
        "removalVersion": "v2.0",
        "migrationGuide": "/docs/api/migration-guide-v1.3.md"
      }
    }
  ]
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
    "taskId": "c3d4e5f6789012345678901234567890",
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
    "taskId": "c3d4e5f6789012345678901234567890",
    "status": "RUNNING",
    "startedAt": "2024-01-01T10:00:00.000Z",
    "currentRound": 0,
    "participants": [
      {
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "status": "CONNECTED",
        "dataSource": "bellhop_features_001.csv"
      },
      {
        "vmId": "b2c3d4e5f67890123456789012345678",
        "status": "CONNECTED",
        "dataSource": "bellhop_features_002.csv"
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
    "taskId": "c3d4e5f6789012345678901234567890",
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
    "taskId": "c3d4e5f6789012345678901234567890",
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
    "taskId": "c3d4e5f6789012345678901234567890",
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
    "taskId": "c3d4e5f6789012345678901234567890",
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
    "taskId": "c3d4e5f6789012345678901234567890",
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
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "role": "PARTICIPANT",
        "status": "TRAINING",
        "lastHeartbeat": "2024-01-01T12:30:00.000Z",
        "currentEpoch": 45,
        "loss": 0.234,
        "accuracy": 0.876
      },
      {
        "vmId": "b2c3d4e5f67890123456789012345678",
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
        "taskId": "c3d4e5f6789012345678901234567890",
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
        "taskId": "d4e5f678901234567890123456789012",
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
    "taskId": "c3d4e5f6789012345678901234567890",
    "taskName": "水声传播特征分类任务",
    "status": "COMPLETED",
    "finalResults": {
      "accuracy": 0.892,
      "loss": 0.098,
      "precision": 0.885,
      "recall": 0.890,
      "f1Score": 0.887,
      "confusionMatrix": [[45, 5], [8, 42]]
    },
    "roundResults": [
      {
        "round": 1,
        "accuracy": 0.750,
        "loss": 0.250,
        "participants": ["a1b2c3d4e5f678901234567890123456", "b2c3d4e5f67890123456789012345678"]
      },
      {
        "round": 2,
        "accuracy": 0.800,
        "loss": 0.200,
        "participants": ["a1b2c3d4e5f678901234567890123456", "b2c3d4e5f67890123456789012345678"]
      }
    ],
    "participantResults": [
      {
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "finalAccuracy": 0.889,
        "finalLoss": 0.102,
        "trainingTime": 14400,
        "dataSize": 1000,
        "parameters": {
          "artifact": {
            "format": "pickle",
            "checksum": "sha256:..."
          }
        }
      },
      {
        "vmId": "b2c3d4e5f67890123456789012345678",
        "finalAccuracy": 0.895,
        "finalLoss": 0.094,
        "trainingTime": 14400,
        "dataSize": 1000,
        "parameters": {
          "artifact": {
            "format": "pickle",
            "checksum": "sha256:..."
          }
        }
      }
    ],
    "modelInfo": {
      "parameters": {
        "artifact": {
          "format": "pickle",
          "checksum": "sha256:..."
        },
        "meta": {
          "version": "1.0.0",
          "modelType": "RANDOM_FOREST"
        }
      }
    }
  }
}
```

### 3.11 配置状态监控接口 (v1.3 新增)

**接口地址**: `GET /api/federated/tasks/{taskId}/config-status`

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
    "taskId": "c3d4e5f6789012345678901234567890",
    "configStatus": "READY",
    "configurationSteps": [
      {
        "step": "DATASET_DISTRIBUTION",
        "status": "COMPLETED",
        "completedAt": "2024-01-01T09:15:00.000Z"
      },
      {
        "step": "PARTICIPANT_VALIDATION",
        "status": "COMPLETED",
        "completedAt": "2024-01-01T09:25:00.000Z"
      }
    ],
    "participantStatuses": [
      {
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "configStatus": "READY",
        "dataDistributed": true,
        "modelInitialized": true
      }
    ]
  }
}
```

### 3.12 资源使用监控接口 (v1.3 新增)

**接口地址**: `GET /api/federated/tasks/{taskId}/resource-usage`

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
    "taskId": "c3d4e5f6789012345678901234567890",
    "participantMetrics": [
      {
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "currentUsage": {
          "cpu": 75.5,
          "memory": 68.2,
          "network": {
            "inbound": 15.6,
            "outbound": 12.3
          }
        }
      }
    ],
    "aggregatedMetrics": {
      "totalCpuUsage": 73.8,
      "totalMemoryUsage": 66.8,
      "taskProgress": 55.6
    }
  }
}
```

### 3.13 任务日志查询接口

**接口地址**: `GET /api/federated/tasks/{taskId}/logs`

**请求头**:
```
Authorization: Bearer {token}
```

**查询参数**:
```
?level=INFO&startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00&keyword=string&page=1&size=10
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "c3d4e5f6789012345678901234567890",
    "total": 100,
    "page": 1,
    "size": 10,
    "logs": [
      {
        "timestamp": "2024-01-01T10:00:00.000Z",
        "level": "INFO",
        "message": "任务启动成功",
        "source": "TASK_MANAGER",
        "details": {
          "participants": ["a1b2c3d4e5f678901234567890123456", "b2c3d4e5f67890123456789012345678"]
        }
      },
      {
        "timestamp": "2024-01-01T10:30:00.000Z",
        "level": "WARN",
        "message": "虚拟机a1b2c3d4e5f678901234567890123456响应超时",
        "source": "TASK_MANAGER",
        "details": {
          "vmId": "a1b2c3d4e5f678901234567890123456",
          "timeout": 300
        }
      }
    ]
  }
}
```

### 3.14 聚合引擎状态查询接口 (v1.4 新增)

**接口地址**: `GET /api/federated/engine/status`

**请求头**:
```
Authorization: Bearer {token}
```

**查询参数**:
- `taskId`: 任务ID (可选)
- `engineId`: 引擎ID (可选)

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "engineStatus": "RUNNING",
    "currentTasks": [
      {
        "taskId": "c3d4e5f6789012345678901234567890",
        "status": "AGGREGATING",
        "currentRound": 5,
        "algorithm": "FEDERATED_AVERAGING",
        "participantCount": 3
      }
    ],
    "systemMetrics": {
      "cpuUsage": 45.2,
      "memoryUsage": 68.5,
      "diskUsage": 23.8
    },
    "aggregationMetrics": {
      "totalAggregations": 125,
      "successRate": 0.98,
      "averageAggregationTime": 2.3,
      "lastAggregationTime": "2024-01-01T12:30:00.000Z"
    },
    "supportedAlgorithms": [
      "FEDERATED_AVERAGING",
      "FEDERATED_PROXIMAL",
      "FEDERATED_NOVA",
      "FEDERATED_SCAFFOLD"
    ]
  }
}
```

### 3.15 可用聚合策略查询接口 (v1.4 新增)

**接口地址**: `GET /api/federated/strategies/available`

**请求头**:
```
Authorization: Bearer {token}
```

**查询参数**:
- `modelType`: 模型类型过滤 (可选)
- `category`: 策略分类过滤 (可选)

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 4,
    "strategies": [
      {
        "algorithm": "FEDERATED_AVERAGING",
        "name": "联邦平均算法",
        "description": "经典的FedAvg算法，适用于大多数联邦学习场景",
        "category": "AVERAGING",
        "supportedModelTypes": ["RANDOM_FOREST", "NEURAL_NETWORK"],
        "parameters": [
          {
            "name": "learningRate",
            "type": "DOUBLE",
            "description": "学习率",
            "defaultValue": 0.01,
            "range": { "min": 0.0001, "max": 1.0 }
          },
          {
            "name": "momentum",
            "type": "DOUBLE",
            "description": "动量参数",
            "defaultValue": 0.9,
            "range": { "min": 0.0, "max": 1.0 }
          },
          {
            "name": "batchSize",
            "type": "INTEGER",
            "description": "批处理大小",
            "defaultValue": 32,
            "range": { "min": 1, "max": 1024 }
          }
        ],
        "requirements": {
          "minParticipants": 2,
          "maxParticipants": 100,
          "recommendedParticipants": 5
        }
      },
      {
        "algorithm": "FEDERATED_PROXIMAL",
        "name": "联邦近端算法",
        "description": "FedProx算法，适用于非独立同分布数据的联邦学习",
        "category": "PROXIMAL",
        "supportedModelTypes": ["NEURAL_NETWORK"],
        "parameters": [
          {
            "name": "learningRate",
            "type": "DOUBLE",
            "description": "学习率",
            "defaultValue": 0.01,
            "range": { "min": 0.0001, "max": 1.0 }
          },
          {
            "name": "proximalMu",
            "type": "DOUBLE",
            "description": "近端参数μ",
            "defaultValue": 0.1,
            "range": { "min": 0.0, "max": 10.0 }
          }
        ],
        "requirements": {
          "minParticipants": 3,
          "maxParticipants": 50,
          "recommendedParticipants": 8
        }
      },
      {
        "algorithm": "FEDERATED_NOVA",
        "name": "联邦Nova算法",
        "description": "FedNova算法，解决客户端异构性问题",
        "category": "NORMALIZATION",
        "supportedModelTypes": ["NEURAL_NETWORK"],
        "parameters": [
          {
            "name": "learningRate",
            "type": "DOUBLE",
            "description": "学习率",
            "defaultValue": 0.01,
            "range": { "min": 0.0001, "max": 1.0 }
          },
          {
            "name": "momentumFactor",
            "type": "DOUBLE",
            "description": "动量因子",
            "defaultValue": 0.9,
            "range": { "min": 0.0, "max": 1.0 }
          }
        ],
        "requirements": {
          "minParticipants": 2,
          "maxParticipants": 30,
          "recommendedParticipants": 6
        }
      },
      {
        "algorithm": "FEDERATED_SCAFFOLD",
        "name": "SCAFFOLD算法",
        "description": "SCAFFOLD算法，使用控制变量减少客户端漂移",
        "category": "VARIANCE_REDUCTION",
        "supportedModelTypes": ["NEURAL_NETWORK"],
        "parameters": [
          {
            "name": "learningRate",
            "type": "DOUBLE",
            "description": "学习率",
            "defaultValue": 0.01,
            "range": { "min": 0.0001, "max": 1.0 }
          },
          {
            "name": "clientLearningRate",
            "type": "DOUBLE",
            "description": "客户端学习率",
            "defaultValue": 0.1,
            "range": { "min": 0.01, "max": 1.0 }
          }
        ],
        "requirements": {
          "minParticipants": 3,
          "maxParticipants": 20,
          "recommendedParticipants": 5
        }
      }
    ],
    "categories": [
      {
        "category": "AVERAGING",
        "name": "平均类算法",
        "description": "基于模型参数平均的联邦学习算法"
      },
      {
        "category": "PROXIMAL",
        "name": "近端类算法",
        "description": "使用近端项处理数据异构性的算法"
      },
      {
        "category": "NORMALIZATION",
        "name": "归一化类算法",
        "description": "通过归一化解决客户端差异的算法"
      },
      {
        "category": "VARIANCE_REDUCTION",
        "name": "方差减少类算法",
        "description": "通过控制变量减少训练方差的算法"
      }
    ]
  }
}
```

### 3.16 任务删除接口

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
    "taskId": "c3d4e5f6789012345678901234567890",
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
| DATASET_CONFIG_ERROR | 400 | 数据集配置错误 (v1.3新增) |
| SIMPLE_PARTICIPANT_CONFIG_DEPRECATED | 200 | 简化参与者配置已废弃警告 (v1.3新增) |

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

## 7. 版本更新历史

### v1.4 (2025年)
- ✅ 新增聚合引擎状态查询接口：`GET /api/federated/engine/status`
- ✅ 新增可用聚合策略查询接口：`GET /api/federated/strategies/available`
- ✅ 支持聚合引擎运行时监控和性能指标查询
- ✅ 提供完整的联邦学习算法参数配置信息
- ✅ 增强系统可观测性和策略配置的动态发现能力
- ✅ 完善UniversalAggregationEngine的REST接口暴露

### v1.3 (2024年)
- ✅ 新增图形化任务创建支持的9个配置接口
- ✅ 增强任务创建接口，支持智能数据集和参与者配置
- ⚠️ 废弃简化的参与者配置格式，推荐使用新的结构化配置
- ✅ 新增实时预览、验证和监控功能
- ✅ 增强响应数据，包含配置摘要和性能预估

### v1.0 (原始版本)
- 基础的联邦学习任务CRUD操作
- 简化的参与者配置格式
- 基本的任务控制和状态查询功能

## 8. 相关文档

**API 文档**:
- [HTTP接口导览.md](../HTTP接口导览.md) - 系统整体API接口
- [修改接口文档 v1.4](../modified/modified-interfaces-v1.4.md) - v1.4新增和修改的接口详情
- [修改接口文档 v1.3](../modified/modified-interfaces-v1.3.md) - v1.3新增和修改的接口详情
- [废弃接口文档 v1.3](../removed/removed-interfaces-v1.3.md) - v1.3废弃接口和迁移指南
- [用户管理API参考文档](../user/user-api-reference.md) - 用户管理相关接口
- [虚拟机API参考文档](../vm/vm-api-reference.md) - 虚拟机管理相关接口

**技术文档**:
- [数据库表结构文档](../../database/database_schema.md) - 联邦学习任务相关数据库设计
- [WebSocket协议文档](../WebSocket/) - 实时通信协议

**迁移指南**:
- 从 v1.0 到 v1.3 的详细迁移步骤，请参考废弃接口文档
- 图形化前端集成示例，请参考修改接口文档 