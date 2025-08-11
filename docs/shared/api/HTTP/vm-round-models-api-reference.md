# 水声联邦学习系统 本地模型（Local Model）接口文档

## 1. 概述
本接口文档用于查询、分析各虚拟机在联邦学习任务中每一轮的本地模型结果与训练指标，数据来源于 `vm_round_models` 表。与全局模型（model_versions）区分，主要用于单机单轮的模型追踪与对比。

---

## 2. 接口列表

### 2.1 本地模型结果分页查询
- **接口地址**: `GET /api/model/vm-round-models`
- **参数**: `taskId`, `roundNumber`, `vmId`, `page`, `size`
- **说明**: 按任务/轮次/虚拟机筛选本地模型结果，分页返回。
- **响应示例**:
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
        "vmRoundModelId": "vmrm-xxx",
        "taskId": "a1b2c3d4e5f678901234567890123456",
        "roundNumber": 25,
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "metrics": {
          "accuracy": 0.88,
          "loss": 0.12
        },
        "createdAt": "2024-01-01T00:00:00.000Z"
      }
    ]
  }
}
```

### 2.2 本地模型结果详情查询
- **接口地址**: `GET /api/model/vm-round-models/{vmRoundModelId}`
- **说明**: 查询单台虚拟机单轮的本地模型及度量。
- **响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "vmRoundModelId": "vmrm-xxx",
    "taskId": "a1b2c3d4e5f678901234567890123456",
    "roundNumber": 25,
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "modelJson": { /* ... */ },
    "metrics": {
      "accuracy": 0.88,
      "loss": 0.12
    },
    "createdAt": "2024-01-01T00:00:00.000Z"
  }
}
```

### 2.3 本地模型训练指标趋势
- **接口地址**: `GET /api/model/vm-round-models/metrics/trend`
- **参数**: `taskId`, `vmId`, `metric`
- **说明**: 查询某虚拟机在某任务下的训练指标趋势。
- **响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "a1b2c3d4e5f678901234567890123456",
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "metric": "accuracy",
    "trend": [
      {"roundNumber": 1, "value": 0.75},
      {"roundNumber": 2, "value": 0.80},
      {"roundNumber": 3, "value": 0.85}
    ]
  }
}
```

### 2.4 本地模型最佳/离群查询
- **接口地址**: `GET /api/model/vm-round-models/metrics/best`
- **参数**: `taskId`, `metric`, `type`（best/outlier）
- **说明**: 查询最佳/离群本地模型。
- **响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "a1b2c3d4e5f678901234567890123456",
    "metric": "accuracy",
    "type": "best",
    "result": {
      "vmRoundModelId": "vmrm-yyy",
      "roundNumber": 25,
      "vmId": "a1b2c3d4e5f678901234567890123456",
      "value": 0.88
    }
  }
}
```

---

## 3. 与全局模型接口的区别
- 本地模型接口数据来源于 `vm_round_models`，全局模型接口数据来源于 `model_versions`。
- 本地模型接口多了 `vmId` 字段，且每轮每机一条。
- 全局模型接口详见 model-version-api-reference.md。 