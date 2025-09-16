# 训练数据分发 API 参考文档

## 1. 概述

训练数据分发API提供联邦学习中训练数据的自动分发、分布式存储和一致性管理功能。支持将训练数据自动分发到参与联邦学习的虚拟机，确保数据的完整性和安全性。

### 1.1 基础信息
- **模块**: 训练数据分发 (Training Data Distribution)
- **基础URL**: `http://localhost:8080/api/training-data/distribution`
- **认证方式**: JWT Token
- **数据格式**: JSON

### 1.2 功能概述
- 训练数据自动分发到虚拟机
- 分发状态实时监控
- 数据完整性验证
- 分发策略配置
- 分发历史查询

---

## 2. API 接口列表

### 2.1 创建数据分发任务
将训练数据分发到指定的虚拟机集群

**接口信息**
- **URL**: `POST /api/training-data/distribution`
- **描述**: 创建训练数据分发任务
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**请求参数**
```json
{
  "taskId": "task_001",
  "datasetIds": ["dataset_001", "dataset_002"],
  "vmIds": ["vm_001", "vm_002", "vm_003"],
  "distributionStrategy": "RANDOM",
  "dataAllocation": {
    "mode": "BALANCED",
    "customAllocation": {
      "vm_001": ["dataset_001"],
      "vm_002": ["dataset_001", "dataset_002"],
      "vm_003": ["dataset_002"]
    }
  },
  "transferSettings": {
    "compressionEnabled": true,
    "encryptionEnabled": true,
    "chunkSize": 1048576,
    "parallelConnections": 3,
    "timeout": 300,
    "retryAttempts": 3
  },
  "validationSettings": {
    "checksumVerification": true,
    "sampleVerification": true,
    "sampleCount": 100
  },
  "notificationSettings": {
    "notifyOnCompletion": true,
    "notifyOnError": true,
    "webhookUrl": "https://example.com/webhook"
  }
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "数据分发任务创建成功",
  "data": {
    "distributionId": "dist_data_001",
    "taskId": "task_001",
    "status": "CREATED",
    "distributionStrategy": "RANDOM",
    "totalDatasets": 2,
    "totalVms": 3,
    "createdAt": "2024-09-10T10:00:00Z",
    "estimatedCompletion": "2024-09-10T10:15:00Z",
    "progress": {
      "total": 6,
      "pending": 6,
      "inProgress": 0,
      "completed": 0,
      "failed": 0
    },
    "allocationPlan": {
      "vm_001": {
        "datasets": ["dataset_001"],
        "estimatedSize": 104857600,
        "estimatedTime": "00:05:00"
      },
      "vm_002": {
        "datasets": ["dataset_001", "dataset_002"],
        "estimatedSize": 209715200,
        "estimatedTime": "00:08:00"
      },
      "vm_003": {
        "datasets": ["dataset_002"],
        "estimatedSize": 104857600,
        "estimatedTime": "00:05:00"
      }
    }
  }
}
```

### 2.2 启动数据分发
启动已创建的数据分发任务

**接口信息**
- **URL**: `POST /api/training-data/distribution/{distributionId}/start`
- **描述**: 启动数据分发任务
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**路径参数**
- `distributionId` (string, required): 分发任务ID

**请求参数**
```json
{
  "priority": "NORMAL",
  "schedule": {
    "immediate": true,
    "scheduledTime": null
  },
  "resourceLimits": {
    "maxBandwidth": "100MB/s",
    "maxConcurrentTransfers": 5
  }
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "数据分发已启动",
  "data": {
    "distributionId": "dist_data_001",
    "status": "IN_PROGRESS",
    "startedAt": "2024-09-10T10:01:00Z",
    "priority": "NORMAL",
    "activeTransfers": 3,
    "progress": {
      "total": 6,
      "pending": 3,
      "inProgress": 3,
      "completed": 0,
      "failed": 0,
      "percentage": 15.5
    }
  }
}
```

### 2.3 查询分发状态
查询数据分发任务的详细状态

**接口信息**
- **URL**: `GET /api/training-data/distribution/{distributionId}`
- **描述**: 查询数据分发任务状态
- **认证**: 需要JWT Token

**路径参数**
- `distributionId` (string, required): 分发任务ID

**查询参数**
- `includeDetails` (boolean, optional): 是否包含详细信息，默认true
- `refresh` (boolean, optional): 是否刷新状态，默认false

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "distributionId": "dist_data_001",
    "taskId": "task_001",
    "status": "IN_PROGRESS",
    "createdAt": "2024-09-10T10:00:00Z",
    "startedAt": "2024-09-10T10:01:00Z",
    "lastUpdated": "2024-09-10T10:08:30Z",
    "progress": {
      "total": 6,
      "pending": 1,
      "inProgress": 2,
      "completed": 3,
      "failed": 0,
      "percentage": 75.0
    },
    "transferStats": {
      "totalDataSize": 419430400,
      "transferredSize": 314572800,
      "averageSpeed": "12.5MB/s",
      "eta": "00:02:15"
    },
    "vmDetails": [
      {
        "vmId": "vm_001",
        "status": "COMPLETED",
        "datasets": [
          {
            "datasetId": "dataset_001",
            "status": "COMPLETED",
            "size": 104857600,
            "checksum": "sha256:abc123...",
            "transferredAt": "2024-09-10T10:03:15Z",
            "verificationStatus": "VERIFIED"
          }
        ],
        "totalSize": 104857600,
        "completedAt": "2024-09-10T10:03:15Z"
      },
      {
        "vmId": "vm_002",
        "status": "IN_PROGRESS",
        "datasets": [
          {
            "datasetId": "dataset_001",
            "status": "COMPLETED",
            "size": 104857600,
            "checksum": "sha256:abc123...",
            "transferredAt": "2024-09-10T10:04:30Z",
            "verificationStatus": "VERIFIED"
          },
          {
            "datasetId": "dataset_002",
            "status": "IN_PROGRESS",
            "size": 104857600,
            "progress": 65.0,
            "eta": "00:01:30"
          }
        ],
        "totalSize": 209715200,
        "progress": 82.5
      },
      {
        "vmId": "vm_003",
        "status": "PENDING",
        "datasets": [
          {
            "datasetId": "dataset_002",
            "status": "PENDING",
            "size": 104857600,
            "estimatedStart": "2024-09-10T10:09:00Z"
          }
        ],
        "totalSize": 104857600,
        "queuePosition": 1
      }
    ]
  }
}
```

### 2.4 获取分发任务列表
查询指定任务或用户的所有数据分发记录

**接口信息**
- **URL**: `GET /api/training-data/distribution`
- **描述**: 获取数据分发任务列表
- **认证**: 需要JWT Token

**查询参数**
- `taskId` (string, optional): 联邦学习任务ID
- `status` (string, optional): 分发状态过滤
- `page` (integer, optional): 页码，默认1
- `size` (integer, optional): 每页大小，默认20
- `sortBy` (string, optional): 排序字段，默认createdAt
- `sortOrder` (string, optional): 排序方向，默认desc

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 25,
    "page": 1,
    "size": 20,
    "items": [
      {
        "distributionId": "dist_data_001",
        "taskId": "task_001",
        "status": "COMPLETED",
        "totalDatasets": 2,
        "totalVms": 3,
        "createdAt": "2024-09-10T10:00:00Z",
        "completedAt": "2024-09-10T10:12:30Z",
        "duration": "00:12:30",
        "success": true
      },
      {
        "distributionId": "dist_data_002",
        "taskId": "task_002",
        "status": "IN_PROGRESS",
        "totalDatasets": 3,
        "totalVms": 5,
        "createdAt": "2024-09-10T09:30:00Z",
        "progress": 45.0,
        "eta": "00:08:15"
      }
    ]
  }
}
```

### 2.5 暂停分发任务
暂停正在进行的数据分发任务

**接口信息**
- **URL**: `POST /api/training-data/distribution/{distributionId}/pause`
- **描述**: 暂停数据分发任务
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**路径参数**
- `distributionId` (string, required): 分发任务ID

**请求参数**
```json
{
  "reason": "系统维护",
  "gracefulShutdown": true,
  "saveProgress": true
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "分发任务已暂停",
  "data": {
    "distributionId": "dist_data_001",
    "status": "PAUSED",
    "pausedAt": "2024-09-10T10:05:00Z",
    "reason": "系统维护",
    "canResume": true,
    "savedProgress": {
      "completedTransfers": 3,
      "partialTransfers": 1,
      "checkpointData": "base64encoded..."
    }
  }
}
```

### 2.6 恢复分发任务
恢复已暂停的数据分发任务

**接口信息**
- **URL**: `POST /api/training-data/distribution/{distributionId}/resume`
- **描述**: 恢复数据分发任务
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**路径参数**
- `distributionId` (string, required): 分发任务ID

**响应示例**
```json
{
  "code": 200,
  "message": "分发任务已恢复",
  "data": {
    "distributionId": "dist_data_001",
    "status": "IN_PROGRESS",
    "resumedAt": "2024-09-10T10:08:00Z",
    "resumeFromCheckpoint": true,
    "remainingTransfers": 3
  }
}
```

### 2.7 取消分发任务
取消数据分发任务

**接口信息**
- **URL**: `DELETE /api/training-data/distribution/{distributionId}`
- **描述**: 取消数据分发任务
- **认证**: 需要JWT Token (ADMIN或EDITOR权限)

**路径参数**
- `distributionId` (string, required): 分发任务ID

**查询参数**
- `cleanup` (boolean, optional): 是否清理已分发的数据，默认false
- `force` (boolean, optional): 强制取消，默认false

**响应示例**
```json
{
  "code": 200,
  "message": "分发任务已取消",
  "data": {
    "distributionId": "dist_data_001",
    "status": "CANCELLED",
    "cancelledAt": "2024-09-10T10:05:00Z",
    "cleanupPerformed": false,
    "completedTransfers": 2,
    "cancelledTransfers": 4
  }
}
```

### 2.8 验证分发完整性
验证已分发数据的完整性

**接口信息**
- **URL**: `POST /api/training-data/distribution/{distributionId}/verify`
- **描述**: 验证分发数据完整性
- **认证**: 需要JWT Token

**路径参数**
- `distributionId` (string, required): 分发任务ID

**请求参数**
```json
{
  "verificationLevel": "FULL",
  "vmIds": ["vm_001", "vm_002"],
  "datasetIds": ["dataset_001"],
  "verificationMethods": ["CHECKSUM", "SAMPLE_CHECK", "RECORD_COUNT"]
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "验证完成",
  "data": {
    "distributionId": "dist_data_001",
    "verificationId": "verify_001",
    "overallStatus": "VERIFIED",
    "verifiedAt": "2024-09-10T10:15:00Z",
    "summary": {
      "totalChecks": 6,
      "passed": 6,
      "failed": 0,
      "warnings": 0
    },
    "vmResults": [
      {
        "vmId": "vm_001",
        "status": "VERIFIED",
        "datasetResults": [
          {
            "datasetId": "dataset_001",
            "checksumMatch": true,
            "recordCount": 1000,
            "sampleCheckPassed": true,
            "status": "VERIFIED"
          }
        ]
      },
      {
        "vmId": "vm_002",
        "status": "VERIFIED",
        "datasetResults": [
          {
            "datasetId": "dataset_001",
            "checksumMatch": true,
            "recordCount": 1000,
            "sampleCheckPassed": true,
            "status": "VERIFIED"
          },
          {
            "datasetId": "dataset_002",
            "checksumMatch": true,
            "recordCount": 1500,
            "sampleCheckPassed": true,
            "status": "VERIFIED"
          }
        ]
      }
    ]
  }
}
```

### 2.9 获取分发统计
获取数据分发的统计信息

**接口信息**
- **URL**: `GET /api/training-data/distribution/statistics`
- **描述**: 获取数据分发统计信息
- **认证**: 需要JWT Token

**查询参数**
- `taskId` (string, optional): 联邦学习任务ID
- `timeRange` (string, optional): 时间范围 (last_hour|last_day|last_week|last_month)
- `groupBy` (string, optional): 分组方式 (task|vm|dataset|status)

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "summary": {
      "totalDistributions": 45,
      "successfulDistributions": 42,
      "failedDistributions": 2,
      "inProgressDistributions": 1,
      "successRate": 93.33,
      "totalDataTransferred": "15.6GB",
      "averageTransferSpeed": "18.5MB/s"
    },
    "byStatus": {
      "COMPLETED": 42,
      "FAILED": 2,
      "IN_PROGRESS": 1,
      "CANCELLED": 0
    },
    "byTask": [
      {
        "taskId": "task_001",
        "distributions": 15,
        "successRate": 100.0,
        "totalData": "5.2GB"
      },
      {
        "taskId": "task_002",
        "distributions": 12,
        "successRate": 91.67,
        "totalData": "4.1GB"
      }
    ],
    "performanceMetrics": {
      "averageDistributionTime": "00:08:45",
      "fastestDistribution": "00:03:15",
      "slowestDistribution": "00:15:30",
      "peakTransferSpeed": "45.2MB/s"
    }
  }
}
```

---

## 3. 数据模型定义

### 3.1 DataDistribution
```json
{
  "distributionId": "string",        // 分发任务唯一标识
  "taskId": "string",               // 关联的联邦学习任务ID
  "status": "string",               // 分发状态
  "distributionStrategy": "string",  // 分发策略
  "createdAt": "string",            // 创建时间
  "startedAt": "string",            // 开始时间
  "completedAt": "string",          // 完成时间
  "progress": "DistributionProgress",
  "allocationPlan": "object"        // 分配计划
}
```

### 3.2 DistributionProgress
```json
{
  "total": "number",                // 总传输数
  "pending": "number",              // 待传输数
  "inProgress": "number",           // 传输中数
  "completed": "number",            // 已完成数
  "failed": "number",               // 失败数
  "percentage": "number"            // 完成百分比
}
```

### 3.3 VmDistributionDetail
```json
{
  "vmId": "string",                 // 虚拟机ID
  "status": "string",               // 分发状态
  "datasets": ["DatasetDistribution"],
  "totalSize": "number",            // 总数据大小
  "progress": "number",             // 进度百分比
  "completedAt": "string"           // 完成时间
}
```

### 3.4 分发状态枚举
- `CREATED`: 已创建
- `IN_PROGRESS`: 进行中
- `PAUSED`: 已暂停
- `COMPLETED`: 已完成
- `FAILED`: 失败
- `CANCELLED`: 已取消

### 3.5 分发策略枚举
- `RANDOM`: 随机分配
- `BALANCED`: 均衡分配
- `CUSTOM`: 自定义分配
- `ROUND_ROBIN`: 轮询分配

---

## 4. 错误码定义

### 4.1 数据分发相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| DISTRIBUTION_NOT_FOUND | 404 | 分发任务不存在 |
| DISTRIBUTION_ALREADY_EXISTS | 409 | 分发任务已存在 |
| DISTRIBUTION_CREATION_FAILED | 500 | 分发任务创建失败 |
| DISTRIBUTION_START_FAILED | 500 | 分发启动失败 |
| DISTRIBUTION_TRANSFER_FAILED | 500 | 数据传输失败 |
| DISTRIBUTION_VERIFICATION_FAILED | 400 | 数据验证失败 |
| DISTRIBUTION_INVALID_STRATEGY | 422 | 分发策略无效 |
| DISTRIBUTION_INSUFFICIENT_RESOURCES | 503 | 资源不足 |

### 4.2 数据相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| DATASET_NOT_FOUND | 404 | 数据集不存在 |
| DATASET_ACCESS_DENIED | 403 | 数据集访问被拒绝 |
| DATASET_CHECKSUM_MISMATCH | 400 | 数据校验和不匹配 |
| DATASET_SIZE_EXCEEDED | 413 | 数据集过大 |

---

## 5. 使用示例

### 5.1 完整的数据分发流程

```javascript
// 1. 创建数据分发任务
const createDistribution = async (taskId, datasets, vms) => {
  const response = await fetch('/api/training-data/distribution', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      taskId: taskId,
      datasetIds: datasets,
      vmIds: vms,
      distributionStrategy: 'BALANCED',
      transferSettings: {
        compressionEnabled: true,
        encryptionEnabled: true,
        parallelConnections: 3,
        timeout: 300,
        retryAttempts: 3
      },
      validationSettings: {
        checksumVerification: true,
        sampleVerification: true
      }
    })
  });
  
  return await response.json();
};

// 2. 启动分发任务
const startDistribution = async (distributionId) => {
  const response = await fetch(`/api/training-data/distribution/${distributionId}/start`, {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      priority: 'HIGH',
      schedule: { immediate: true }
    })
  });
  
  return await response.json();
};

// 3. 监控分发进度
const monitorDistribution = async (distributionId) => {
  const response = await fetch(`/api/training-data/distribution/${distributionId}?refresh=true`, {
    method: 'GET',
    headers: {
      'Authorization': 'Bearer ' + token
    }
  });
  
  const status = await response.json();
  return status.data;
};

// 4. 验证分发完整性
const verifyDistribution = async (distributionId) => {
  const response = await fetch(`/api/training-data/distribution/${distributionId}/verify`, {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      verificationLevel: 'FULL',
      verificationMethods: ['CHECKSUM', 'SAMPLE_CHECK', 'RECORD_COUNT']
    })
  });
  
  return await response.json();
};
```

### 5.2 监控分发进度的实时更新

```javascript
const trackDistributionProgress = (distributionId, onUpdate) => {
  const checkProgress = async () => {
    try {
      const status = await monitorDistribution(distributionId);
      onUpdate(status);
      
      // 如果未完成，继续监控
      if (['CREATED', 'IN_PROGRESS', 'PAUSED'].includes(status.status)) {
        setTimeout(checkProgress, 5000); // 每5秒检查一次
      }
    } catch (error) {
      console.error('监控分发进度失败:', error);
    }
  };
  
  checkProgress();
};

// 使用示例
trackDistributionProgress('dist_data_001', (status) => {
  console.log(`分发进度: ${status.progress.percentage}%`);
  console.log(`状态: ${status.status}`);
  console.log(`ETA: ${status.transferStats?.eta}`);
});
```

---

## 6. 安全注意事项

### 6.1 数据安全
- 所有数据传输使用加密通道(TLS 1.3)
- 支持数据压缩以减少传输时间和带宽消耗
- 使用强校验和(SHA-256)确保数据完整性
- 支持断点续传和失败重试机制

### 6.2 访问控制
- 只有任务所有者和协作者可以创建分发任务
- 虚拟机需要有效的认证才能接收数据
- 支持基于角色的分发权限控制
- 记录所有分发操作的审计日志

### 6.3 资源管理
- 限制并发分发任务数量
- 实施带宽和存储配额管理
- 支持分发任务的优先级调度
- 监控系统资源使用情况

---

## 7. 性能优化

### 7.1 传输优化
- 使用多线程并发传输
- 支持数据分块和并行下载
- 实现智能重试和错误恢复
- 动态调整传输参数

### 7.2 存储优化
- 使用分布式存储系统
- 实现数据去重和增量传输
- 支持数据压缩和缓存
- 定期清理过期的分发数据

### 7.3 监控优化
- 实时监控分发性能指标
- 提供详细的进度和状态信息
- 支持分发任务的性能分析
- 实现智能的负载均衡