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
- 使用初始模型ID在联邦学习任务创建时完成绑定
- 查询任务初始模型信息
- 分发初始模型到参与虚拟机
- 初始模型版本管理

### 1.3 支持的模型类型

系统支持以下机器学习模型类型：

#### 1.3.1 RANDOM_FOREST - 随机森林（推荐）
- **描述**: 基于决策树的集成学习模型
- **适用场景**: 水声信号分类、回归预测、特征重要性分析
- **架构参数**:
  ```json
  {
    "n_estimators": 100,      // 树的数量，范围：10-500，默认100
    "n_features": 5,           // 特征数量，最小值：1，默认5
    "task_type": "regression"  // 任务类型：classification（分类）或 regression（回归）
  }
  ```
- **优势**: 训练速度快、抗过拟合能力强、无需特征归一化

#### 1.3.2 NEURAL_NETWORK - 神经网络
- **描述**: 深度学习神经网络模型
- **适用场景**: 复杂特征学习、非线性关系建模
- **架构参数**:
  ```json
  {
    "inputSize": 128,                    // 输入层大小
    "hiddenLayers": [64, 32, 16],       // 隐藏层配置
    "outputSize": 10,                    // 输出层大小
    "activationFunction": "relu",        // 激活函数
    "optimizer": "adam",                 // 优化器
    "learningRate": 0.001               // 学习率
  }
  ```
- **优势**: 强大的表达能力、适合大规模数据

> **注意**: 本文档中的示例主要使用 RANDOM_FOREST 作为演示，但所有接口均支持两种模型类型。

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
  "modelType": "RANDOM_FOREST",
  "architecture": {
    "n_estimators": 100,
    "n_features": 5,
    "task_type": "regression"
  },
  "randomSeed": 42,
  "description": "水声信号分类初始模型",
  "labels": ["baseline", "v1.0"]
}
```

**参数说明**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| modelType | string | 是 | 模型类型，可选值：RANDOM_FOREST, NEURAL_NETWORK |
| architecture | object | 是 | 模型架构参数（不同模型类型参数不同，见1.3节） |
| randomSeed | number | 否 | 随机种子，用于可重复性实验 |
| description | string | 否 | 模型描述 |
| labels | array[string] | 否 | 自定义标签，便于分类检索 |

**响应示例**
```json
{
  "code": 200,
  "message": "初始模型生成成功",
  "data": {
    "modelId": "initial_model_001",
    "modelType": "RANDOM_FOREST",
    "modelSize": 8192,
    "parametersCount": 100,
    "architecture": {
      "n_estimators": 100,
      "n_features": 5,
      "task_type": "regression"
    },
    "generatedAt": "2024-09-10T10:00:00Z",
    "boundTaskId": null,
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
modelType: "RANDOM_FOREST" (required)
description: "预训练的随机森林模型" (optional)
file: [binary file] (required)
metadata: {
  "architecture": {
    "n_estimators": 100,
    "n_features": 5,
    "task_type": "regression"
  },
  "framework": "sklearn",
  "version": "1.0"
} (optional, JSON string)
labels: ["baseline", "v1.0"] (optional)
```

**响应示例**
```json
{
  "code": 200,
  "message": "初始模型上传成功",
  "data": {
    "modelId": "initial_model_002",
    "modelType": "RANDOM_FOREST",
    "fileName": "initial_model.pkl",
    "modelSize": 10240,
    "uploadedAt": "2024-09-10T10:00:00Z",
    "status": "UPLOADED",
    "boundTaskId": null,
    "checksum": "sha256:efgh5678...",
    "metadata": {
      "architecture": {
        "n_estimators": 100,
        "n_features": 5,
        "task_type": "regression"
      },
      "framework": "sklearn",
      "version": "1.0"
    }
  }
}
```

### 2.3 获取初始模型详情
查询指定初始模型的完整信息（与任务绑定状态无关）

**接口信息**
- **URL**: `GET /api/model/initial/{modelId}`
- **描述**: 获取初始模型的元数据和绑定状态
- **认证**: 需要JWT Token

**路径参数**
- `modelId` (string, required): 初始模型ID

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
    "modelType": "RANDOM_FOREST",
    "modelSize": 8192,
    "createdAt": "2024-09-10T10:00:00Z",
    "status": "READY",
    "bindingStatus": "UNBOUND",
    "architecture": {
      "n_estimators": 100,
      "n_features": 5,
      "task_type": "regression"
    },
    "bindings": [],
    "checksum": "sha256:abcd1234..."
  }
}
```

### 2.4 获取任务当前初始模型
查询指定联邦学习任务正在使用的初始模型信息

**接口信息**
- **URL**: `GET /api/model/initial/task/{taskId}`
- **描述**: 根据任务ID查询已绑定的初始模型详情
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
    "modelType": "RANDOM_FOREST",
    "modelSize": 8192,
    "boundAt": "2024-09-10T09:30:00Z",
    "status": "DISTRIBUTED",
    "architecture": {
      "n_estimators": 100,
      "n_features": 5,
      "task_type": "regression"
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

### 2.5 分发初始模型
将初始模型分发到参与联邦学习的虚拟机

**接口信息**
- **URL**: `POST /api/model/initial/task/{taskId}/distribute`
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

### 2.6 查询分发状态
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

### 2.7 下载初始模型
下载初始模型文件

**接口信息**
- **URL**: `GET /api/model/initial/{modelId}/download`
- **描述**: 下载初始模型文件
- **认证**: 需要JWT Token
- **响应类型**: application/octet-stream

**路径参数**
- `modelId` (string, required): 初始模型ID

**查询参数**
- `format` (string, optional): 下载格式 (binary|json)，默认binary

**响应**
- 成功时返回模型文件的二进制数据
- Content-Type: application/octet-stream
- Content-Disposition: attachment; filename="initial_model_{modelId}.pth"

### 2.8 删除初始模型
删除指定初始模型（需确保未绑定任务或强制删除）

**接口信息**
- **URL**: `DELETE /api/model/initial/{modelId}`
- **描述**: 删除初始模型及其文件
- **认证**: 需要JWT Token (ADMIN权限)

**路径参数**
- `modelId` (string, required): 初始模型ID

**查询参数**
- `force` (boolean, optional): 强制删除，即使模型已绑定任务，默认false

**响应示例**
```json
{
  "code": 200,
  "message": "初始模型删除成功",
  "data": {
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
  "modelType": "string",         // 模型类型（RANDOM_FOREST, NEURAL_NETWORK）
  "modelSize": "number",         // 模型大小(字节)
  "architecture": "object",      // 模型架构参数（根据modelType不同而不同）
  "labels": ["string"],         // 模型标签
  "status": "string",           // 模型状态
  "bindingStatus": "string",    // 绑定状态：UNBOUND/BOUND
  "bindings": [                 // 任务绑定记录
    {
      "taskId": "string",
      "boundAt": "string"
    }
  ],
  "createdAt": "string",        // 创建时间
  "updatedAt": "string",        // 最后更新时间
  "checksum": "string"          // 模型校验和
}
```

#### 架构参数详细定义

**RANDOM_FOREST 架构参数**
```json
{
  "n_estimators": "number",      // 树的数量，范围：10-500
  "n_features": "number",        // 特征数量，最小值：1
  "task_type": "string"          // 任务类型："classification" 或 "regression"
}
```

**NEURAL_NETWORK 架构参数**
```json
{
  "inputSize": "number",         // 输入层大小
  "hiddenLayers": "array",       // 隐藏层配置，如 [64, 32, 16]
  "outputSize": "number",        // 输出层大小
  "activationFunction": "string", // 激活函数，如 "relu", "sigmoid"
  "optimizer": "string",         // 优化器，如 "adam", "sgd"
  "learningRate": "number"       // 学习率
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

### 5.1 流程示例：创建初始模型并在任务中绑定

```javascript
// 1. 生成随机初始模型（无需提供 taskId）
const generateResponse = await fetch('/api/model/initial/generate', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    modelType: 'RANDOM_FOREST',
    architecture: {
      n_estimators: 100,
      n_features: 5,
      task_type: 'regression'
    },
    description: '水声信号分类初始模型'
  })
});

const { data: modelData } = await generateResponse.json();
const initialModelId = modelData.modelId;

// 2. 上传训练数据集（示例引用训练数据API）
const datasetResponse = await fetch('/api/training-data/upload', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  },
  body: createFormDataWithFileAndMetadata()
});
const { data: datasetData } = await datasetResponse.json();
const datasetId = datasetData.datasetId;

// 3. 创建联邦学习任务时绑定初始模型与数据集
const createTaskResponse = await fetch('/api/federated/tasks', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    taskName: '水声传播特征分类任务',
    taskType: 'CLASSIFICATION',
    algorithm: 'FEDERATED_AVERAGING',
    datasetConfig: {
      datasetId,
      distributionStrategy: 'BALANCED'
    },
    initialModelConfig: {
      mode: 'CUSTOM',
      initialModelId
    },
    participantConfig: {
      selectionMode: 'MANUAL',
      participants: [
        { vmId: 'vm_001', role: 'PARTICIPANT', dataRatio: 0.6 },
        { vmId: 'vm_002', role: 'PARTICIPANT', dataRatio: 0.4 }
      ]
    }
  })
});

const { data: taskData } = await createTaskResponse.json();
const taskId = taskData.taskId;

// 4. 需要分发时，根据任务ID触发分发
const distributeResponse = await fetch(`/api/model/initial/task/${taskId}/distribute`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    vmIds: ['vm_001', 'vm_002'],
    distributionMode: 'ASYNC',
    verifyChecksum: true
  })
});

const distribution = await distributeResponse.json();
```

### 5.2 上传自定义初始模型

```javascript
// 上传自定义模型文件
const uploadCustomModel = async (modelFile) => {
  const formData = new FormData();
  formData.append('modelType', 'RANDOM_FOREST');
  formData.append('description', '预训练的随机森林模型');
  formData.append('file', modelFile);
  formData.append('labels', ['baseline']);
  formData.append('metadata', JSON.stringify({
    architecture: {
      n_estimators: 100,
      n_features: 5,
      task_type: 'regression'
    },
    framework: 'sklearn',
    version: '1.0'
  }));

  const response = await fetch('/api/model/initial/upload', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  const { data } = await response.json();
  return data.modelId; // 在创建联邦学习任务时引用该ID
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
