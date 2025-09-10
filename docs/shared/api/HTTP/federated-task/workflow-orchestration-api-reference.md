# 联邦学习流程编排 API 参考文档

## 1. 概述

联邦学习流程编排API提供端到端的联邦学习任务生命周期管理，包括任务启动、流程监控、状态协调和异常处理。该API实现了完整的联邦学习工作流编排，从初始模型分发到最终模型聚合的全过程自动化管理。

### 1.1 基础信息
- **模块**: 联邦学习流程编排 (Federated Learning Workflow Orchestration)
- **基础URL**: `http://localhost:8080/api/federated/orchestration`
- **认证方式**: JWT Token
- **数据格式**: JSON

### 1.2 功能概述
- 端到端联邦学习流程自动化
- 多阶段任务状态协调
- 实时流程监控和进度跟踪
- 异常处理和故障恢复
- 流程时间线管理
- 性能分析和优化建议

---

## 2. API 接口列表

### 2.1 启动联邦学习流程
启动完整的端到端联邦学习工作流

**接口信息**
- **URL**: `POST /api/federated/orchestration/start`
- **描述**: 启动完整的联邦学习流程
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**请求参数**
```json
{
  "taskId": "task_001",
  "workflowConfig": {
    "autoStart": true,
    "stages": {
      "initialModelGeneration": {
        "enabled": true,
        "strategy": "RANDOM_GENERATION",
        "parameters": {
          "modelType": "neural_network",
          "architecture": {
            "inputSize": 128,
            "hiddenLayers": [64, 32, 16],
            "outputSize": 10
          }
        }
      },
      "dataDistribution": {
        "enabled": true,
        "strategy": "BALANCED",
        "verificationLevel": "FULL"
      },
      "modelDistribution": {
        "enabled": true,
        "timeout": 300,
        "retryAttempts": 3
      },
      "federatedTraining": {
        "maxRounds": 10,
        "convergenceThreshold": 0.001,
        "participantThreshold": 0.8,
        "roundTimeout": 1800
      },
      "modelAggregation": {
        "algorithm": "FedAvg",
        "aggregationTimeout": 600,
        "qualityThreshold": 0.85
      }
    },
    "errorHandling": {
      "autoRetry": true,
      "maxRetries": 3,
      "retryDelay": 60,
      "fallbackStrategy": "PARTIAL_CONTINUE"
    },
    "notifications": {
      "stageCompletion": true,
      "errors": true,
      "finalResult": true,
      "webhookUrl": "https://example.com/webhook"
    }
  },
  "schedulingOptions": {
    "priority": "NORMAL",
    "maxExecutionTime": 7200,
    "resourceLimits": {
      "maxMemory": "4GB",
      "maxCpuCores": 8,
      "maxBandwidth": "100MB/s"
    }
  }
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "联邦学习流程已启动",
  "data": {
    "orchestrationId": "orch_001",
    "taskId": "task_001",
    "status": "STARTED",
    "startedAt": "2024-09-10T10:00:00Z",
    "estimatedCompletion": "2024-09-10T12:00:00Z",
    "currentStage": "INITIAL_MODEL_GENERATION",
    "workflowPlan": {
      "totalStages": 5,
      "estimatedDuration": "02:00:00",
      "stages": [
        {
          "name": "INITIAL_MODEL_GENERATION",
          "status": "IN_PROGRESS",
          "estimatedDuration": "00:05:00"
        },
        {
          "name": "DATA_DISTRIBUTION",
          "status": "PENDING",
          "estimatedDuration": "00:15:00"
        },
        {
          "name": "MODEL_DISTRIBUTION",
          "status": "PENDING",
          "estimatedDuration": "00:10:00"
        },
        {
          "name": "FEDERATED_TRAINING",
          "status": "PENDING",
          "estimatedDuration": "01:20:00"
        },
        {
          "name": "FINAL_AGGREGATION",
          "status": "PENDING",
          "estimatedDuration": "00:10:00"
        }
      ]
    },
    "resourceAllocation": {
      "allocatedMemory": "4GB",
      "allocatedCpuCores": 8,
      "allocatedBandwidth": "100MB/s",
      "participatingVms": ["vm_001", "vm_002", "vm_003"]
    }
  }
}
```

### 2.2 查询流程状态
查询联邦学习流程的详细执行状态

**接口信息**
- **URL**: `GET /api/federated/orchestration/{orchestrationId}/status`
- **描述**: 查询流程执行状态
- **认证**: 需要JWT Token

**路径参数**
- `orchestrationId` (string, required): 编排任务ID

**查询参数**
- `includeDetails` (boolean, optional): 是否包含详细信息，默认true
- `includeMetrics` (boolean, optional): 是否包含性能指标，默认false
- `refresh` (boolean, optional): 是否刷新状态，默认false

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "orchestrationId": "orch_001",
    "taskId": "task_001",
    "status": "IN_PROGRESS",
    "startedAt": "2024-09-10T10:00:00Z",
    "lastUpdated": "2024-09-10T10:45:30Z",
    "currentStage": "FEDERATED_TRAINING",
    "progress": {
      "overallProgress": 65.0,
      "stageProgress": 50.0,
      "currentRound": 5,
      "totalRounds": 10
    },
    "stageDetails": [
      {
        "name": "INITIAL_MODEL_GENERATION",
        "status": "COMPLETED",
        "startedAt": "2024-09-10T10:00:00Z",
        "completedAt": "2024-09-10T10:03:15Z",
        "duration": "00:03:15",
        "result": {
          "modelId": "initial_model_001",
          "modelSize": 1048576,
          "checksum": "sha256:abc123..."
        }
      },
      {
        "name": "DATA_DISTRIBUTION",
        "status": "COMPLETED",
        "startedAt": "2024-09-10T10:03:15Z",
        "completedAt": "2024-09-10T10:18:45Z",
        "duration": "00:15:30",
        "result": {
          "distributionId": "dist_data_001",
          "distributedVms": 3,
          "totalDataSize": 419430400,
          "verificationPassed": true
        }
      },
      {
        "name": "MODEL_DISTRIBUTION",
        "status": "COMPLETED",
        "startedAt": "2024-09-10T10:18:45Z",
        "completedAt": "2024-09-10T10:25:00Z",
        "duration": "00:06:15",
        "result": {
          "distributionId": "dist_model_001",
          "distributedVms": 3,
          "verificationPassed": true
        }
      },
      {
        "name": "FEDERATED_TRAINING",
        "status": "IN_PROGRESS",
        "startedAt": "2024-09-10T10:25:00Z",
        "currentRound": {
          "roundNumber": 5,
          "status": "MODEL_AGGREGATION",
          "participatingVms": 3,
          "submittedModels": 3,
          "aggregationProgress": 75.0
        },
        "rounds": [
          {
            "roundNumber": 1,
            "status": "COMPLETED",
            "duration": "00:08:30",
            "accuracy": 0.72,
            "loss": 0.856
          },
          {
            "roundNumber": 2,
            "status": "COMPLETED", 
            "duration": "00:07:45",
            "accuracy": 0.78,
            "loss": 0.652
          },
          {
            "roundNumber": 3,
            "status": "COMPLETED",
            "duration": "00:08:00",
            "accuracy": 0.82,
            "loss": 0.543
          },
          {
            "roundNumber": 4,
            "status": "COMPLETED",
            "duration": "00:07:30",
            "accuracy": 0.85,
            "loss": 0.467
          },
          {
            "roundNumber": 5,
            "status": "IN_PROGRESS",
            "startedAt": "2024-09-10T10:42:15Z",
            "expectedCompletion": "2024-09-10T10:50:00Z"
          }
        ]
      },
      {
        "name": "FINAL_AGGREGATION",
        "status": "PENDING",
        "estimatedStart": "2024-09-10T11:45:00Z",
        "estimatedDuration": "00:10:00"
      }
    ],
    "performanceMetrics": {
      "averageRoundDuration": "00:08:01",
      "dataTransferSpeed": "18.5MB/s",
      "aggregationEfficiency": 92.3,
      "resourceUtilization": {
        "cpu": 75.2,
        "memory": 68.1,
        "network": 45.8
      }
    }
  }
}
```

### 2.3 暂停流程执行
暂停正在执行的联邦学习流程

**接口信息**
- **URL**: `POST /api/federated/orchestration/{orchestrationId}/pause`
- **描述**: 暂停流程执行
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**路径参数**
- `orchestrationId` (string, required): 编排任务ID

**请求参数**
```json
{
  "reason": "系统维护",
  "pauseMode": "GRACEFUL",
  "waitForCurrentRound": true,
  "preserveState": true,
  "notifyParticipants": true
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "流程已暂停",
  "data": {
    "orchestrationId": "orch_001",
    "status": "PAUSED",
    "pausedAt": "2024-09-10T10:45:30Z",
    "pausedStage": "FEDERATED_TRAINING",
    "pausedRound": 5,
    "reason": "系统维护",
    "canResume": true,
    "stateSnapshot": {
      "savedAt": "2024-09-10T10:45:30Z",
      "snapshotId": "snapshot_001",
      "preservedData": {
        "currentModels": true,
        "trainingState": true,
        "aggregationProgress": true
      }
    }
  }
}
```

### 2.4 恢复流程执行
恢复已暂停的联邦学习流程

**接口信息**
- **URL**: `POST /api/federated/orchestration/{orchestrationId}/resume`
- **描述**: 恢复流程执行
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**路径参数**
- `orchestrationId` (string, required): 编排任务ID

**请求参数**
```json
{
  "resumeFromSnapshot": true,
  "snapshotId": "snapshot_001",
  "validateState": true,
  "notifyParticipants": true
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "流程已恢复",
  "data": {
    "orchestrationId": "orch_001",
    "status": "IN_PROGRESS",
    "resumedAt": "2024-09-10T11:00:00Z",
    "resumedStage": "FEDERATED_TRAINING",
    "resumedRound": 5,
    "stateValidation": {
      "passed": true,
      "modelsVerified": 3,
      "stateConsistent": true
    },
    "estimatedRemainingTime": "00:45:00"
  }
}
```

### 2.5 终止流程执行
终止正在执行的联邦学习流程

**接口信息**
- **URL**: `DELETE /api/federated/orchestration/{orchestrationId}`
- **描述**: 终止流程执行
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**路径参数**
- `orchestrationId` (string, required): 编排任务ID

**查询参数**
- `force` (boolean, optional): 强制终止，默认false
- `cleanup` (boolean, optional): 清理资源，默认true
- `saveResults` (boolean, optional): 保存中间结果，默认true

**响应示例**
```json
{
  "code": 200,
  "message": "流程已终止",
  "data": {
    "orchestrationId": "orch_001",
    "status": "TERMINATED",
    "terminatedAt": "2024-09-10T10:50:00Z",
    "terminatedStage": "FEDERATED_TRAINING",
    "terminatedRound": 5,
    "completedRounds": 4,
    "partialResults": {
      "bestModel": {
        "roundNumber": 4,
        "accuracy": 0.85,
        "modelId": "model_round_4"
      },
      "savedModels": 4,
      "trainingMetrics": "available"
    },
    "cleanup": {
      "resourcesReleased": true,
      "temporaryDataCleared": true,
      "participantsNotified": true
    }
  }
}
```

### 2.6 获取流程时间线
获取联邦学习流程的详细执行时间线

**接口信息**
- **URL**: `GET /api/federated/workflow/{orchestrationId}/timeline`
- **描述**: 获取流程执行时间线
- **认证**: 需要JWT Token

**路径参数**
- `orchestrationId` (string, required): 编排任务ID

**查询参数**
- `includeEvents` (boolean, optional): 是否包含事件详情，默认true
- `eventLevel` (string, optional): 事件级别 (ALL|MAJOR|ERROR)，默认MAJOR
- `timeRange` (string, optional): 时间范围，格式：startTime-endTime

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "orchestrationId": "orch_001",
    "taskId": "task_001",
    "timeline": {
      "startTime": "2024-09-10T10:00:00Z",
      "endTime": null,
      "totalDuration": "01:23:45",
      "events": [
        {
          "eventId": "event_001",
          "timestamp": "2024-09-10T10:00:00Z",
          "eventType": "WORKFLOW_STARTED",
          "stage": "INITIALIZATION",
          "level": "MAJOR",
          "message": "联邦学习流程启动",
          "details": {
            "taskId": "task_001",
            "participantCount": 3,
            "estimatedDuration": "02:00:00"
          }
        },
        {
          "eventId": "event_002",
          "timestamp": "2024-09-10T10:00:15Z",
          "eventType": "STAGE_STARTED",
          "stage": "INITIAL_MODEL_GENERATION",
          "level": "MAJOR",
          "message": "开始生成初始模型",
          "details": {
            "modelType": "neural_network",
            "architecture": "custom"
          }
        },
        {
          "eventId": "event_003",
          "timestamp": "2024-09-10T10:03:15Z",
          "eventType": "STAGE_COMPLETED",
          "stage": "INITIAL_MODEL_GENERATION",
          "level": "MAJOR",
          "message": "初始模型生成完成",
          "duration": "00:03:00",
          "details": {
            "modelId": "initial_model_001",
            "modelSize": 1048576,
            "status": "SUCCESS"
          }
        },
        {
          "eventId": "event_004",
          "timestamp": "2024-09-10T10:03:15Z",
          "eventType": "STAGE_STARTED",
          "stage": "DATA_DISTRIBUTION",
          "level": "MAJOR",
          "message": "开始分发训练数据",
          "details": {
            "datasetCount": 2,
            "targetVms": 3,
            "distributionStrategy": "BALANCED"
          }
        },
        {
          "eventId": "event_005",
          "timestamp": "2024-09-10T10:25:00Z",
          "eventType": "TRAINING_ROUND_STARTED",
          "stage": "FEDERATED_TRAINING",
          "level": "MAJOR",
          "message": "开始训练轮次 1",
          "details": {
            "roundNumber": 1,
            "participatingVms": 3,
            "roundTimeout": 1800
          }
        },
        {
          "eventId": "event_006",
          "timestamp": "2024-09-10T10:33:30Z",
          "eventType": "TRAINING_ROUND_COMPLETED",
          "stage": "FEDERATED_TRAINING",
          "level": "MAJOR",
          "message": "训练轮次 1 完成",
          "duration": "00:08:30",
          "details": {
            "roundNumber": 1,
            "accuracy": 0.72,
            "loss": 0.856,
            "participantsCompleted": 3
          }
        }
      ]
    },
    "stagesSummary": {
      "INITIAL_MODEL_GENERATION": {
        "status": "COMPLETED",
        "duration": "00:03:00",
        "events": 2
      },
      "DATA_DISTRIBUTION": {
        "status": "COMPLETED",
        "duration": "00:15:30",
        "events": 6
      },
      "MODEL_DISTRIBUTION": {
        "status": "COMPLETED",
        "duration": "00:06:15",
        "events": 4
      },
      "FEDERATED_TRAINING": {
        "status": "IN_PROGRESS",
        "duration": "00:58:45",
        "events": 18,
        "completedRounds": 4,
        "currentRound": 5
      }
    }
  }
}
```

### 2.7 获取流程列表
获取用户的联邦学习流程执行历史

**接口信息**
- **URL**: `GET /api/federated/orchestration`
- **描述**: 获取流程列表
- **认证**: 需要JWT Token

**查询参数**
- `taskId` (string, optional): 联邦学习任务ID
- `status` (string, optional): 流程状态过滤
- `page` (integer, optional): 页码，默认1
- `size` (integer, optional): 每页大小，默认20
- `sortBy` (string, optional): 排序字段，默认startedAt
- `sortOrder` (string, optional): 排序方向，默认desc

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 35,
    "page": 1,
    "size": 20,
    "items": [
      {
        "orchestrationId": "orch_001",
        "taskId": "task_001",
        "status": "IN_PROGRESS",
        "startedAt": "2024-09-10T10:00:00Z",
        "currentStage": "FEDERATED_TRAINING",
        "progress": 65.0,
        "duration": "01:23:45",
        "participatingVms": 3,
        "completedRounds": 4,
        "totalRounds": 10
      },
      {
        "orchestrationId": "orch_002",
        "taskId": "task_002",
        "status": "COMPLETED",
        "startedAt": "2024-09-09T14:30:00Z",
        "completedAt": "2024-09-09T16:45:30Z",
        "duration": "02:15:30",
        "finalAccuracy": 0.92,
        "totalRounds": 8,
        "success": true
      }
    ]
  }
}
```

### 2.8 获取流程性能分析
获取联邦学习流程的性能分析报告

**接口信息**
- **URL**: `GET /api/federated/orchestration/{orchestrationId}/analytics`
- **描述**: 获取流程性能分析
- **认证**: 需要JWT Token

**路径参数**
- `orchestrationId` (string, required): 编排任务ID

**查询参数**
- `includeRecommendations` (boolean, optional): 是否包含优化建议，默认true
- `metricsLevel` (string, optional): 指标详细程度 (BASIC|DETAILED|FULL)，默认DETAILED

**响应示例**
```json
{
  "code": 200,
  "message": "分析成功",
  "data": {
    "orchestrationId": "orch_001",
    "analysisTimestamp": "2024-09-10T11:23:45Z",
    "overallPerformance": {
      "score": 85.5,
      "grade": "B+",
      "efficiency": 87.2,
      "reliability": 94.1,
      "scalability": 76.8
    },
    "stagePerformance": {
      "INITIAL_MODEL_GENERATION": {
        "duration": "00:03:00",
        "efficiency": 95.0,
        "resourceUtilization": 45.2
      },
      "DATA_DISTRIBUTION": {
        "duration": "00:15:30",
        "efficiency": 82.5,
        "transferSpeed": "18.5MB/s",
        "resourceUtilization": 67.3
      },
      "FEDERATED_TRAINING": {
        "averageRoundDuration": "00:08:01",
        "convergenceRate": 0.12,
        "participationRate": 100.0,
        "resourceUtilization": 78.9
      }
    },
    "resourceMetrics": {
      "cpu": {
        "averageUtilization": 72.5,
        "peakUtilization": 89.2,
        "efficiency": 81.3
      },
      "memory": {
        "averageUtilization": 65.8,
        "peakUtilization": 82.4,
        "efficiency": 79.9
      },
      "network": {
        "averageUtilization": 42.3,
        "peakBandwidth": "85.2MB/s",
        "efficiency": 88.7
      }
    },
    "qualityMetrics": {
      "modelAccuracy": {
        "initial": 0.45,
        "final": 0.85,
        "improvement": 0.40,
        "convergenceRounds": 6
      },
      "trainingStability": 92.1,
      "aggregationQuality": 94.8
    },
    "recommendations": [
      {
        "category": "PERFORMANCE",
        "priority": "HIGH",
        "title": "增加训练轮次并行度",
        "description": "当前虚拟机资源利用率较低，建议增加并行处理的训练任务",
        "expectedImprovement": "15-20%性能提升",
        "implementation": {
          "parameter": "parallelTraining",
          "currentValue": 1,
          "recommendedValue": 2
        }
      },
      {
        "category": "EFFICIENCY",
        "priority": "MEDIUM",
        "title": "优化数据分发策略",
        "description": "数据分发阶段耗时较长，建议使用更高效的分发算法",
        "expectedImprovement": "25%分发时间减少",
        "implementation": {
          "parameter": "distributionStrategy",
          "currentValue": "BALANCED",
          "recommendedValue": "ADAPTIVE"
        }
      }
    ],
    "comparisons": {
      "similarTasks": {
        "averageDuration": "01:45:30",
        "performanceRanking": "TOP_25%",
        "efficiencyRanking": "TOP_30%"
      },
      "historicalTrends": {
        "improvementRate": 8.5,
        "consistencyScore": 89.2
      }
    }
  }
}
```

---

## 3. 数据模型定义

### 3.1 OrchestrationWorkflow
```json
{
  "orchestrationId": "string",          // 编排任务唯一标识
  "taskId": "string",                   // 关联的联邦学习任务ID
  "status": "string",                   // 流程状态
  "startedAt": "string",                // 开始时间
  "completedAt": "string",              // 完成时间
  "currentStage": "string",             // 当前执行阶段
  "progress": "WorkflowProgress",       // 进度信息
  "stageDetails": ["WorkflowStage"]     // 阶段详情
}
```

### 3.2 WorkflowStage
```json
{
  "name": "string",                     // 阶段名称
  "status": "string",                   // 阶段状态
  "startedAt": "string",                // 开始时间
  "completedAt": "string",              // 完成时间
  "duration": "string",                 // 执行时长
  "result": "object",                   // 阶段结果
  "errorInfo": "object"                 // 错误信息
}
```

### 3.3 工作流状态枚举
- `CREATED`: 已创建
- `STARTED`: 已启动
- `IN_PROGRESS`: 执行中
- `PAUSED`: 已暂停
- `COMPLETED`: 已完成
- `FAILED`: 执行失败
- `TERMINATED`: 已终止

### 3.4 工作流阶段枚举
- `INITIAL_MODEL_GENERATION`: 初始模型生成
- `DATA_DISTRIBUTION`: 数据分发
- `MODEL_DISTRIBUTION`: 模型分发
- `FEDERATED_TRAINING`: 联邦训练
- `FINAL_AGGREGATION`: 最终聚合

---

## 4. 错误码定义

### 4.1 流程编排相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| ORCHESTRATION_NOT_FOUND | 404 | 编排任务不存在 |
| ORCHESTRATION_START_FAILED | 500 | 流程启动失败 |
| ORCHESTRATION_INVALID_CONFIG | 422 | 编排配置无效 |
| ORCHESTRATION_STAGE_FAILED | 500 | 阶段执行失败 |
| ORCHESTRATION_TIMEOUT | 408 | 流程执行超时 |
| ORCHESTRATION_RESOURCE_INSUFFICIENT | 503 | 资源不足 |
| ORCHESTRATION_DEPENDENCY_FAILED | 424 | 依赖服务失败 |

### 4.2 状态管理相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| INVALID_STATE_TRANSITION | 409 | 无效的状态转换 |
| STATE_SNAPSHOT_FAILED | 500 | 状态快照失败 |
| STATE_RECOVERY_FAILED | 500 | 状态恢复失败 |

---

## 5. 使用示例

### 5.1 完整的流程编排示例

```javascript
// 1. 启动联邦学习流程
const startWorkflow = async (taskId) => {
  const response = await fetch('/api/federated/orchestration/start', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      taskId: taskId,
      workflowConfig: {
        autoStart: true,
        stages: {
          initialModelGeneration: {
            enabled: true,
            strategy: 'RANDOM_GENERATION'
          },
          dataDistribution: {
            enabled: true,
            strategy: 'BALANCED',
            verificationLevel: 'FULL'
          },
          federatedTraining: {
            maxRounds: 10,
            convergenceThreshold: 0.001,
            participantThreshold: 0.8
          }
        },
        errorHandling: {
          autoRetry: true,
          maxRetries: 3
        }
      }
    })
  });
  
  return await response.json();
};

// 2. 监控流程进度
const monitorWorkflow = async (orchestrationId) => {
  const response = await fetch(`/api/federated/orchestration/${orchestrationId}/status?includeMetrics=true`, {
    method: 'GET',
    headers: {
      'Authorization': 'Bearer ' + token
    }
  });
  
  return await response.json();
};

// 3. 获取流程时间线
const getTimeline = async (orchestrationId) => {
  const response = await fetch(`/api/federated/workflow/${orchestrationId}/timeline`, {
    method: 'GET',
    headers: {
      'Authorization': 'Bearer ' + token
    }
  });
  
  return await response.json();
};
```

### 5.2 实时监控和事件处理

```javascript
// 实时监控流程状态
const trackWorkflowProgress = (orchestrationId, onUpdate, onComplete, onError) => {
  const checkStatus = async () => {
    try {
      const status = await monitorWorkflow(orchestrationId);
      onUpdate(status.data);
      
      if (status.data.status === 'COMPLETED') {
        onComplete(status.data);
        return;
      } else if (['FAILED', 'TERMINATED'].includes(status.data.status)) {
        onError(status.data);
        return;
      }
      
      // 继续监控
      setTimeout(checkStatus, 10000); // 每10秒检查一次
    } catch (error) {
      onError(error);
    }
  };
  
  checkStatus();
};

// 使用示例
trackWorkflowProgress('orch_001', 
  (status) => {
    console.log(`流程进度: ${status.progress.overallProgress}%`);
    console.log(`当前阶段: ${status.currentStage}`);
    if (status.currentStage === 'FEDERATED_TRAINING') {
      console.log(`训练轮次: ${status.progress.currentRound}/${status.progress.totalRounds}`);
    }
  },
  (finalStatus) => {
    console.log('联邦学习流程完成!');
    console.log(`最终模型准确率: ${finalStatus.finalAccuracy}`);
  },
  (error) => {
    console.error('流程执行出错:', error);
  }
);
```

---

## 6. 最佳实践

### 6.1 流程配置优化
- 根据数据集大小和虚拟机性能调整超时时间
- 设置合适的收敛阈值和参与者阈值
- 启用自动重试和故障恢复机制
- 配置适当的通知策略

### 6.2 性能监控
- 定期检查资源使用情况
- 监控训练收敛速度和模型质量
- 跟踪各阶段的执行时间和效率
- 及时发现和处理性能瓶颈

### 6.3 错误处理
- 实施多层次的错误检测和恢复
- 保存关键状态快照以支持故障恢复
- 设置合理的重试策略和回退机制
- 提供详细的错误诊断信息

### 6.4 安全考虑
- 确保所有阶段的数据传输安全
- 实施严格的访问控制和权限管理
- 记录完整的操作审计日志
- 保护敏感的模型和训练数据