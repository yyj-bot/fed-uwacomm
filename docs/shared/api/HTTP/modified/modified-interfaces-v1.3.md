# 联邦学习任务API接口修改文档 v1.3

## 文档说明
- **版本**: v1.3
- **创建时间**: 2024年
- **修改范围**: 联邦学习任务管理相关接口
- **目的**: 支持图形化任务创建和智能配置

本文档记录了从 v1.0 到 v1.3 版本中所有**新增**和**修改**的接口。

## 新增接口组

### 1. 预配置接口组

#### 1.1 获取可用虚拟机列表
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
        "connectionStatus": "CONNECTED",
        "osType": "Ubuntu 20.04",
        "resources": {
          "cpuCores": 8,
          "memoryMb": 16384,
          "diskGb": 500,
          "gpuCount": 1,
          "gpuMemoryMb": 16384
        },
        "capabilities": ["GPU", "HIGH_MEMORY", "FAST_NETWORK"],
        "supportedAlgorithms": ["FEDERATED_AVERAGING", "FEDPROX", "FEDNOVA"],
        "currentUsage": {
          "cpuUsage": 25.5,
          "memoryUsage": 45.2,
          "networkUsage": 15.8
        },
        "networkInfo": {
          "bandwidth": 1000,
          "latency": 10,
          "uploadSpeed": 500,
          "downloadSpeed": 800
        },
        "lastHeartbeat": "2024-01-01T12:30:00.000Z",
        "reliability": {
          "uptime": 99.8,
          "avgResponseTime": 150,
          "taskSuccessRate": 98.5
        }
      }
    ]
  }
}
```

#### 1.2 获取可用数据集列表
**接口地址**: `GET /api/federated/config/available-datasets`

**请求头**:
```
Authorization: Bearer {token}
```

**查询参数**:
- `dataType`: 数据类型过滤 (可选)
- `status`: 数据状态过滤 (可选)
- `minSize`: 最小数据大小 (可选)
- `maxSize`: 最大数据大小 (可选)
- `keyword`: 关键词搜索 (可选)

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
        "description": "基于BELLHOP仿真的水声传播特征数据",
        "dataType": "ACOUSTIC",
        "status": "READY",
        "statistics": {
          "totalRows": 10000,
          "totalColumns": 128,
          "fileSize": 45678123,
          "fileSizeFormatted": "43.5MB"
        },
        "features": {
          "featureColumns": ["frequency", "amplitude", "phase", "depth"],
          "targetColumn": "propagation_loss",
          "numericFeatures": 120,
          "categoricalFeatures": 8
        },
        "quality": {
          "completeness": 99.2,
          "consistency": 97.8,
          "accuracy": 98.5,
          "missingValues": 80,
          "duplicates": 15,
          "outliers": 156
        },
        "metadata": {
          "source": "BELLHOP",
          "version": "1.0",
          "sampleRate": 48000,
          "frequency": "1kHz-10kHz",
          "environment": "shallow_water"
        },
        "tags": ["acoustic", "feature", "bellhop", "simulation"],
        "uploadTime": "2024-01-01T10:00:00.000Z",
        "uploadedBy": "f6789012345678901234567890123456"
      }
    ]
  }
}
```

#### 1.3 获取角色配置选项
**接口地址**: `GET /api/federated/config/roles`

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "roles": [
      {
        "role": "PARTICIPANT",
        "name": "参与者",
        "description": "参与联邦学习训练的客户端节点",
        "requirements": {
          "minCpuCores": 2,
          "minMemoryMb": 4096,
          "requiredCapabilities": ["TRAINING"]
        },
        "compatibleAlgorithms": ["FEDERATED_AVERAGING", "FEDPROX", "FEDNOVA", "SCAFFOLD"]
      },
      {
        "role": "AGGREGATOR",
        "name": "聚合器",
        "description": "负责模型聚合的服务端节点",
        "requirements": {
          "minCpuCores": 4,
          "minMemoryMb": 8192,
          "requiredCapabilities": ["AGGREGATION"]
        },
        "compatibleAlgorithms": ["FEDERATED_AVERAGING", "FEDPROX", "FEDNOVA", "SCAFFOLD"]
      }
    ]
  }
}
```

#### 1.4 获取算法配置模板
**接口地址**: `GET /api/federated/config/algorithm-templates`

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "templates": [
      {
        "algorithm": "FEDERATED_AVERAGING",
        "name": "联邦平均算法",
        "description": "经典的联邦学习算法，适用于IID数据分布",
        "applicableTaskTypes": ["CLASSIFICATION", "REGRESSION"],
        "defaultHyperparameters": {
          "learningRate": 0.01,
          "batchSize": 32,
          "epochs": 100,
          "rounds": 10,
          "minParticipants": 2,
          "aggregationMethod": "WEIGHTED_AVERAGE"
        },
        "parameterRanges": {
          "learningRate": {
            "min": 0.0001,
            "max": 1.0,
            "recommended": [0.001, 0.01, 0.1]
          },
          "batchSize": {
            "min": 1,
            "max": 1024,
            "recommended": [16, 32, 64, 128]
          }
        }
      }
    ]
  }
}
```

### 2. 智能配置接口组

#### 2.1 数据分配预览
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
    },
    {
      "vmId": "b2c3d4e5f67890123456789012345678",
      "requestedRatio": 0.4
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
          "vmName": "水声联邦学习节点-001",
          "allocatedRatio": 0.65,
          "allocatedRows": 5200,
          "estimatedTrainingTime": 450
        },
        {
          "vmId": "b2c3d4e5f67890123456789012345678",
          "vmName": "水声联邦学习节点-002",
          "allocatedRatio": 0.35,
          "allocatedRows": 2800,
          "estimatedTrainingTime": 380
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

#### 2.2 参与者验证
**接口地址**: `POST /api/federated/tasks/validate-participants`

**请求参数**:
```json
{
  "algorithm": "FEDERATED_AVERAGING",
  "taskType": "CLASSIFICATION",
  "participants": [
    {
      "vmId": "a1b2c3d4e5f678901234567890123456",
      "role": "PARTICIPANT"
    }
  ]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "验证完成",
  "data": {
    "overallValid": true,
    "participantValidations": [
      {
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "isValid": true,
        "validationResults": {
          "connectivity": {
            "status": "PASS",
            "message": "网络连接正常"
          },
          "resources": {
            "status": "PASS",
            "message": "资源满足要求"
          }
        }
      }
    ]
  }
}
```

## 修改接口

### 1. 任务创建接口增强

**接口地址**: `POST /api/federated/tasks`

**新增请求参数**:
```json
{
  // 原有字段保持不变
  "taskName": "水声传播特征分类任务",
  "taskType": "CLASSIFICATION",
  "algorithm": "FEDERATED_AVERAGING",

  // 🆕 新增：智能数据集配置
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

  // 🆕 新增：智能参与者配置
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
        "capabilities": ["GPU"]
      }
    ]
  },

  // 原有字段保持兼容
  "hyperparameters": {
    "learningRate": 0.01,
    "batchSize": 32,
    "epochs": 100,
    "rounds": 10
  }
}
```

**增强的响应示例**:
```json
{
  "code": 200,
  "message": "任务创建成功",
  "data": {
    "taskId": "c3d4e5f6789012345678901234567890",
    "taskName": "水声传播特征分类任务",
    "status": "CREATED",
    "participantCount": 2,
    "estimatedDuration": 46500,

    // 🆕 新增：配置摘要
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
          "dataRatio": 0.6
        }
      ]
    },

    // 🆕 新增：预计性能指标
    "performanceEstimation": {
      "expectedAccuracy": 0.85,
      "convergenceRounds": 8,
      "networkTraffic": "2.3GB"
    }
  }
}
```

### 2. 增强监控接口组

#### 2.1 配置状态监控
**接口地址**: `GET /api/federated/tasks/{taskId}/config-status`

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

#### 2.2 资源使用监控
**接口地址**: `GET /api/federated/tasks/{taskId}/resource-usage`

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
        },
        "averageUsage": {
          "cpu": 72.1,
          "memory": 65.5
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

## 前端对接接口清单

### 图形化任务创建页面需要对接的所有接口：

1. **`GET /api/federated/config/available-vms`** - 虚拟机选择器组件
2. **`GET /api/federated/config/available-datasets`** - 数据集选择器组件
3. **`GET /api/federated/config/roles`** - 角色配置选项
4. **`GET /api/federated/config/algorithm-templates`** - 算法配置模板
5. **`POST /api/federated/tasks/preview-distribution`** - 实时数据分配预览
6. **`POST /api/federated/tasks/validate-participants`** - 参与者配置验证
7. **`POST /api/federated/tasks`** - 增强版任务创建接口
8. **`GET /api/federated/tasks/{taskId}/config-status`** - 配置状态实时监控
9. **`GET /api/federated/tasks/{taskId}/resource-usage`** - 资源使用监控

## 版本兼容性说明

### 向后兼容性
- 原有的 `POST /api/federated/tasks` 接口参数格式仍然支持
- 新增字段都为可选字段，不影响现有客户端
- 原有的参与者配置格式将在 v2.0 版本中废弃

### 迁移建议
1. 逐步将 `participants.dataSource` 字段迁移到 `datasetConfig`
2. 使用新的 `participantConfig` 结构替代简化的 `participants` 数组
3. 利用预配置接口提升用户体验
4. 集成实时验证和预览功能

## VM管理接口修改

### 3. 虚拟机注册接口改进

**接口地址**: `POST /api/v1/vm/register`

**修改内容**:
1. **移除vmId字段**: 不再需要客户端提供vmId，完全由后端自动生成
2. **简化请求结构**: 减少客户端复杂度，提高注册成功率
3. **增强ID安全性**: 后端生成的vmId采用加密强度的随机UUID

**修改前请求参数**:
```json
{
  "vmId": "a1b2c3d4e5f678901234567890123456",  // ❌ 需要客户端提供
  "name": "水声联邦学习节点-001",
  "ipAddress": "192.168.1.100",
  // ... 其他字段
}
```

**修改后请求参数**:
```json
{
  // ✅ vmId由后端自动生成，无需提供
  "name": "水声联邦学习节点-001",
  "ipAddress": "192.168.1.100",
  // ... 其他字段
}
```

**业务规则变更**:
- **自动生成ID**: vmId由后端自动生成，采用32位UUID格式，保证全局唯一性
- **移除重复性检查**: 不再需要客户端检查vmId唯一性
- **简化错误处理**: 消除vmId格式错误的可能性

**WebSocket认证流程确认**:
- 注册成功后，vmId包含在返回的accessToken中
- WebSocket连接时，后端从token解析vmId，无需URL参数传递
- 符合安全最佳实践，避免ID在URL中暴露

本文档详细说明了 v1.3 版本中所有新增和修改的接口，为前端图形化配置提供了完整的API支持。