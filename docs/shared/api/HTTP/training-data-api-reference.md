# 训练数据管理 API 参考文档

## 1. 概述

本文档定义了水声联邦学习系统的训练数据管理API接口，包括数据上传、文本信息管理、数据预处理、验证、删除等功能。

### 1.1 基础信息
- **基础URL**: `http://localhost:8080/api/training-data`
- **API版本**: v1.0
- **认证方式**: JWT Token
- **数据格式**: JSON / Multipart Form Data

### 1.2 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 2. 数据类型定义

### 2.1 数据类型枚举
| 类型 | 说明 | 支持格式 |
|------|------|----------|
| ACOUSTIC | 声学数据 | .wav, .mp3, .flac, .csv, .mat |
| ENVIRONMENT | 环境数据 | .env, .txt, .csv, .json |
| MODEL | 模型数据 | .pkl, .h5, .pb, .onnx |
| FEATURE | 特征数据 | .csv, .npy, .mat, .json |
| OTHER | 其他数据 | 任意格式 |

### 2.2 数据状态枚举
| 状态 | 说明 |
|------|------|
| UPLOADING | 上传中 |
| PROCESSING | 处理中 |
| VALIDATING | 验证中 |
| READY | 就绪 |
| ERROR | 错误 |
| DELETED | 已删除 |

## 3. API接口定义

### 3.1 文件上传接口

**接口地址**: `POST /api/training-data/upload`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**请求参数**:
```
vmId: vm-001 (必需) - 虚拟机ID
dataType: ACOUSTIC (必需) - 数据类型
description: 水声传播特征数据 (可选) - 数据描述
tags: ["feature", "acoustic"] (可选) - 数据标签
metadata: {"source": "bellhop", "version": "1.0"} (可选) - 元数据
file: [文件] (必需) - 上传的文件
```

**响应示例**:
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "dataId": "data_1234567890",
    "filename": "bellhop_features_001.csv",
    "filePath": "/data/acoustic/bellhop_features_001.csv",
    "fileSize": 2048576,
    "dataType": "ACOUSTIC",
    "vmId": "vm-001",
    "status": "UPLOADING",
    "uploadTime": "2024-01-01T10:00:00.000Z",
    "uploadedBy": "admin",
    "progress": 0
  }
}
```

### 3.2 文本信息上传接口

**接口地址**: `POST /api/training-data/text`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "vmId": "vm-001",
  "dataType": "ENVIRONMENT",
  "title": "声学传播环境配置",
  "content": "声学传播环境参数配置信息...",
  "description": "声学传播环境参数配置",
  "tags": ["environment", "acoustic"],
  "metadata": {
    "source": "manual",
    "version": "1.0",
    "parameters": {
      "depth": 100,
      "temperature": 15.5,
      "salinity": 35.0
    }
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "文本信息上传成功",
  "data": {
    "dataId": "data_1234567891",
    "title": "声学传播环境配置",
    "content": "声学传播环境参数配置信息...",
    "dataType": "ENVIRONMENT",
    "vmId": "vm-001",
    "status": "READY",
    "createdAt": "2024-01-01T10:30:00.000Z",
    "createdBy": "admin",
    "contentLength": 256
  }
}
```

### 3.3 数据列表查询接口

**接口地址**: `GET /api/training-data`

**请求头**:
```
Authorization: Bearer {token}
```

**查询参数**:
- `page`: 页码 (默认: 1)
- `size`: 每页大小 (默认: 20)
- `vmId`: 虚拟机ID过滤
- `dataType`: 数据类型过滤
- `status`: 状态过滤
- `keyword`: 关键词搜索
- `startDate`: 开始日期
- `endDate`: 结束日期
- `tags`: 标签过滤

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 50,
    "page": 1,
    "size": 20,
    "dataList": [
      {
        "dataId": "data_1234567890",
        "filename": "bellhop_features_001.csv",
        "dataType": "ACOUSTIC",
        "vmId": "vm-001",
        "status": "READY",
        "fileSize": 2048576,
        "uploadTime": "2024-01-01T10:00:00.000Z",
        "uploadedBy": "admin",
        "description": "水声传播特征数据",
        "tags": ["feature", "acoustic"]
      },
      {
        "dataId": "data_1234567891",
        "title": "声学传播环境配置",
        "dataType": "ENVIRONMENT",
        "vmId": "vm-001",
        "status": "READY",
        "createdAt": "2024-01-01T10:30:00.000Z",
        "createdBy": "admin",
        "description": "声学传播环境参数配置",
        "tags": ["environment", "acoustic"]
      }
    ]
  }
}
```

### 3.4 数据详情查询接口

**接口地址**: `GET /api/training-data/{dataId}`

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
    "dataId": "data_1234567890",
    "filename": "bellhop_features_001.csv",
    "filePath": "/data/acoustic/bellhop_features_001.csv",
    "dataType": "ACOUSTIC",
    "vmId": "vm-001",
    "status": "READY",
    "fileSize": 2048576,
    "uploadTime": "2024-01-01T10:00:00.000Z",
    "uploadedBy": "admin",
    "description": "水声传播特征数据",
    "tags": ["feature", "acoustic"],
    "metadata": {
      "source": "bellhop",
      "version": "1.0",
      "columns": 103,
      "rows": 318,
      "features": ["feature_1", "feature_2", "feature_3"]
    },
    "validation": {
      "isValid": true,
      "validationTime": "2024-01-01T10:05:00.000Z",
      "errors": [],
      "warnings": []
    },
    "preprocessing": {
      "isProcessed": true,
      "processTime": "2024-01-01T10:10:00.000Z",
      "methods": ["normalization", "feature_selection"],
      "parameters": {
        "normalization": "standard_scaler",
        "feature_selection": "variance_threshold"
      }
    }
  }
}
```

### 3.5 数据下载接口

**接口地址**: `GET /api/training-data/{dataId}/download`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**: 文件流下载

### 3.6 数据预处理接口

**接口地址**: `POST /api/training-data/{dataId}/preprocess`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "methods": ["normalization", "feature_selection", "outlier_removal"],
  "parameters": {
    "normalization": {
      "method": "standard_scaler",
      "columns": ["feature_1", "feature_2", "feature_3"]
    },
    "feature_selection": {
      "method": "variance_threshold",
      "threshold": 0.01
    },
    "outlier_removal": {
      "method": "isolation_forest",
      "contamination": 0.1
    }
  },
  "outputFormat": "csv"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "预处理任务已启动",
  "data": {
    "dataId": "data_1234567890",
    "taskId": "preprocess_1234567890",
    "status": "PROCESSING",
    "methods": ["normalization", "feature_selection", "outlier_removal"],
    "startedAt": "2024-01-01T11:00:00.000Z",
    "estimatedTime": 300
  }
}
```

### 3.7 数据验证接口

**接口地址**: `POST /api/training-data/{dataId}/validate`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "validationRules": {
    "dataType": "ACOUSTIC",
    "requiredColumns": ["feature_1", "feature_2", "feature_3"],
    "dataTypes": {
      "feature_1": "float",
      "feature_2": "float",
      "feature_3": "float"
    },
    "constraints": {
      "feature_1": {
        "min": 0,
        "max": 100,
        "notNull": true
      },
      "feature_2": {
        "min": -50,
        "max": 50,
        "notNull": true
      }
    },
    "qualityChecks": ["missing_values", "duplicates", "outliers"]
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "验证完成",
  "data": {
    "dataId": "data_1234567890",
    "isValid": true,
    "validationTime": "2024-01-01T11:30:00.000Z",
    "results": {
      "totalRows": 318,
      "validRows": 315,
      "invalidRows": 3,
      "missingValues": 2,
      "duplicates": 1,
      "outliers": 0
    },
    "errors": [
      {
        "row": 45,
        "column": "feature_1",
        "error": "Value out of range",
        "value": 150.5
      }
    ],
    "warnings": [
      {
        "type": "missing_values",
        "count": 2,
        "columns": ["feature_2"]
      }
    ]
  }
}
```

### 3.8 数据更新接口

**接口地址**: `PUT /api/training-data/{dataId}`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "description": "更新后的数据描述",
  "tags": ["feature", "acoustic", "updated"],
  "metadata": {
    "source": "bellhop",
    "version": "1.1",
    "updatedBy": "researcher"
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "数据更新成功",
  "data": {
    "dataId": "data_1234567890",
    "updatedAt": "2024-01-01T12:00:00.000Z",
    "updatedBy": "researcher"
  }
}
```

### 3.9 数据删除接口

**接口地址**: `DELETE /api/training-data/{dataId}`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```json
{
  "reason": "数据已过期",
  "deleteFile": true,
  "deleteMetadata": false
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "数据删除成功",
  "data": {
    "dataId": "data_1234567890",
    "deletedAt": "2024-01-01T12:30:00.000Z",
    "deletedBy": "admin",
    "fileDeleted": true,
    "metadataPreserved": true
  }
}
```

### 3.10 批量数据操作接口

**接口地址**: `POST /api/training-data/batch`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "operation": "DELETE",
  "dataIds": ["data_1234567890", "data_1234567891"],
  "parameters": {
    "reason": "批量清理过期数据",
    "deleteFile": true
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "批量操作成功",
  "data": {
    "operation": "DELETE",
    "total": 2,
    "success": 2,
    "failed": 0,
    "results": [
      {
        "dataId": "data_1234567890",
        "status": "SUCCESS",
        "message": "删除成功"
      },
      {
        "dataId": "data_1234567891",
        "status": "SUCCESS",
        "message": "删除成功"
      }
    ]
  }
}
```

### 3.11 数据统计接口

**接口地址**: `GET /api/training-data/statistics`

**请求头**:
```
Authorization: Bearer {token}
```

**查询参数**:
- `vmId`: 虚拟机ID过滤
- `dataType`: 数据类型过滤
- `startDate`: 开始日期
- `endDate`: 结束日期

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "totalCount": 150,
    "totalSize": 1073741824,
    "byType": {
      "ACOUSTIC": {
        "count": 80,
        "size": 536870912,
        "percentage": 53.33
      },
      "ENVIRONMENT": {
        "count": 40,
        "size": 268435456,
        "percentage": 26.67
      },
      "MODEL": {
        "count": 20,
        "size": 209715200,
        "percentage": 13.33
      },
      "OTHER": {
        "count": 10,
        "size": 58720256,
        "percentage": 6.67
      }
    },
    "byStatus": {
      "READY": 120,
      "PROCESSING": 15,
      "ERROR": 10,
      "UPLOADING": 5
    },
    "byVm": {
      "vm-001": {
        "count": 60,
        "size": 402653184
      },
      "vm-002": {
        "count": 50,
        "size": 335544320
      },
      "vm-003": {
        "count": 40,
        "size": 335544320
      }
    }
  }
}
```

### 3.12 数据导出接口

**接口地址**: `POST /api/training-data/export`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "format": "CSV",
  "filters": {
    "vmId": "vm-001",
    "dataType": "ACOUSTIC",
    "status": "READY",
    "startDate": "2024-01-01T00:00:00",
    "endDate": "2024-01-31T23:59:59"
  },
  "fields": ["dataId", "filename", "dataType", "status", "uploadTime", "description"]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "导出任务已启动",
  "data": {
    "taskId": "export_1234567890",
    "status": "PROCESSING",
    "format": "CSV",
    "startedAt": "2024-01-01T13:00:00.000Z",
    "estimatedTime": 60,
    "downloadUrl": "/api/training-data/export/download/export_1234567890"
  }
}
```

## 4. 错误码定义

### 4.1 数据相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| DATA_NOT_FOUND | 404 | 数据不存在 |
| DATA_ALREADY_EXISTS | 409 | 数据已存在 |
| DATA_UPLOAD_FAILED | 500 | 数据上传失败 |
| DATA_PROCESSING_FAILED | 500 | 数据处理失败 |
| DATA_VALIDATION_FAILED | 400 | 数据验证失败 |
| DATA_DELETE_FAILED | 500 | 数据删除失败 |
| DATA_DOWNLOAD_FAILED | 500 | 数据下载失败 |

### 4.2 文件相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| FILE_TOO_LARGE | 413 | 文件过大 |
| FILE_TYPE_NOT_SUPPORTED | 400 | 文件类型不支持 |
| FILE_CORRUPTED | 400 | 文件损坏 |
| FILE_UPLOAD_TIMEOUT | 408 | 文件上传超时 |
| FILE_NOT_FOUND | 404 | 文件不存在 |

### 4.3 权限相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| INSUFFICIENT_STORAGE | 507 | 存储空间不足 |
| QUOTA_EXCEEDED | 429 | 配额超限 |
| ACCESS_DENIED | 403 | 访问被拒绝 |

## 5. 安全规范

### 5.1 文件上传安全
- 文件大小限制：单个文件最大100MB
- 文件类型验证：只允许指定格式的文件
- 病毒扫描：上传文件进行病毒扫描
- 文件完整性校验：使用MD5/SHA256校验

### 5.2 数据访问控制
- 基于角色的访问控制(RBAC)
- 数据所有者权限管理
- 数据共享权限控制
- 操作日志记录

### 5.3 数据保护
- 敏感数据加密存储
- 数据传输加密(HTTPS)
- 数据备份和恢复
- 数据生命周期管理

## 6. 性能规范

### 6.1 上传性能
- 支持断点续传
- 支持分片上传
- 支持并发上传
- 上传进度实时反馈

### 6.2 处理性能
- 异步处理大文件
- 支持批量操作
- 处理进度监控
- 结果缓存机制

### 6.3 查询性能
- 分页查询优化
- 索引优化
- 缓存策略
- 查询超时控制

## 7. 相关文档

- [HTTP接口导览.md](./HTTP接口导览.md) - 系统整体API接口
- [用户管理API参考文档](./user-api-reference.md) - 用户管理相关接口
- [虚拟机API参考文档](./vm-api-reference.md) - 虚拟机管理相关接口
- [联邦学习任务管理API参考文档](./federated-task-api-reference.md) - 联邦学习任务管理相关接口
- [数据库表结构文档](../../database/database_schema.md) - 训练数据相关数据库设计
- [WebSocket协议文档](../WebSocket/) - 实时通信协议 