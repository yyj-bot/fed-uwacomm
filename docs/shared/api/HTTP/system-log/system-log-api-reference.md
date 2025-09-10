# 水声联邦学习系统 日志管理 API 接口文档

## 1. 概述

本文档定义了水声联邦学习系统的日志管理相关HTTP REST API接口，包括SpringBoot系统日志和虚拟机运行日志的查询、导出、清理、系统监控等功能。

系统包含两种日志类型：
1. **SpringBoot系统日志** (system_logs): 记录应用运行日志，包括请求日志、错误日志等
2. **虚拟机运行日志** (vm_runtime_logs): 记录虚拟机运行日志，包括任务执行、状态变更等

### 1.1 基础信息
- **基础URL**: `http://localhost:8080/api/log`
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

## 2. 日志级别说明

### 2.1 日志级别定义
| 级别 | 说明 | 颜色 |
|------|------|------|
| DEBUG | 调试信息，用于开发调试 | 灰色 |
| INFO | 一般信息，记录系统正常运行状态 | 蓝色 |
| WARN | 警告信息，系统可能出现问题 | 黄色 |
| ERROR | 错误信息，系统出现错误 | 红色 |

### 2.2 日志类别定义
| 类别 | 说明 |
|------|------|
| SYSTEM | 系统级日志，如启动、关闭、配置变更等 |
| USER | 用户操作日志，如登录、注册、权限变更等 |
| VM | 虚拟机相关日志，如连接、断开、状态变更等 |
| TASK | 联邦学习任务日志，如任务创建、执行、完成等 |
| DATA | 数据管理日志，如数据上传、下载、处理等 |
| MODEL | 模型管理日志，如模型上传、部署、评估等 |
| SECURITY | 安全相关日志，如认证失败、权限拒绝等 |
| PERFORMANCE | 性能监控日志，如响应时间、资源使用等 |

## 3. 日志查询接口

### 3.1 日志列表查询

**接口地址**: `GET /api/log/list`

**请求参数**:
```
?level=INFO&category=SYSTEM&vmId=a1b2c3d4e5f678901234567890123456&taskId=b2c3d4e5f67890123456789012345678&startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00&keyword=string&page=1&size=10&sort=createdAt&order=desc
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| level | string | 否 | 日志级别过滤，DEBUG/INFO/WARN/ERROR |
| category | string | 否 | 日志类别过滤 |
| vmId | string | 否 | 虚拟机ID过滤，32位UUID格式 |
| taskId | string | 否 | 任务ID过滤，32位UUID格式 |
| startTime | string | 否 | 开始时间，ISO 8601格式 |
| endTime | string | 否 | 结束时间，ISO 8601格式 |
| keyword | string | 否 | 关键词搜索（消息内容） |
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页大小，默认10，最大100 |
| sort | string | 否 | 排序字段，默认createdAt |
| order | string | 否 | 排序方向，desc/asc |

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 1000,
    "pages": 100,
    "current": 1,
    "size": 10,
    "records": [
      {
        "logId": "c3d4e5f6789012345678901234567890",
        "level": "INFO",
        "category": "SYSTEM",
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "taskId": "b2c3d4e5f67890123456789012345678",
        "message": "系统启动成功",
        "details": {
          "version": "1.0.0",
          "startupTime": 5000
        },
        "createdAt": "2024-01-01T10:00:00"
      }
    ]
  }
}
```

### 3.2 日志详情查询

**接口地址**: `GET /api/log/detail/{logId}`

**路径参数**:
- logId: 日志ID

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "logId": "c3d4e5f6789012345678901234567890",
    "level": "ERROR",
    "category": "TASK",
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "taskId": "b2c3d4e5f67890123456789012345678",
    "message": "联邦学习任务执行失败",
    "details": {
      "errorCode": "TASK_EXECUTION_FAILED",
      "errorMessage": "模型训练过程中出现异常",
      "stackTrace": "java.lang.Exception: ...",
      "context": {
        "round": 5,
        "algorithm": "FEDAVG",
        "participants": 3
      }
    },
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 3.3 实时日志查询

**接口地址**: `GET /api/log/realtime`

**请求参数**:
```
?level=INFO&category=SYSTEM&vmId=a1b2c3d4e5f678901234567890123456&taskId=b2c3d4e5f67890123456789012345678&tail=100
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| level | string | 否 | 日志级别过滤 |
| category | string | 否 | 日志类别过滤 |
| vmId | string | 否 | 虚拟机ID过滤，32位UUID格式 |
| taskId | string | 否 | 任务ID过滤，32位UUID格式 |
| tail | int | 否 | 返回最近N条日志，默认100 |

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "logs": [
      {
        "logId": "c3d4e5f6789012345678901234567890",
        "level": "INFO",
        "category": "SYSTEM",
        "message": "系统运行正常",
        "createdAt": "2024-01-01T10:00:00"
      }
    ],
    "totalCount": 100,
    "lastUpdateTime": "2024-01-01T10:00:00"
  }
}
```

### 3.4 日志统计查询

**接口地址**: `GET /api/log/statistics`

**请求参数**:
```
?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00&category=SYSTEM&vmId=a1b2c3d4e5f678901234567890123456&taskId=b2c3d4e5f67890123456789012345678
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "totalLogs": 10000,
    "levelDistribution": {
      "DEBUG": 2000,
      "INFO": 6000,
      "WARN": 1500,
      "ERROR": 500
    },
    "categoryDistribution": {
      "SYSTEM": 3000,
      "USER": 2000,
      "VM": 2500,
      "TASK": 1500,
      "DATA": 500,
      "MODEL": 300,
      "SECURITY": 100,
      "PERFORMANCE": 100
    },
    "timeDistribution": [
      {
        "hour": "00",
        "count": 500
      },
      {
        "hour": "01",
        "count": 450
      }
    ],
    "errorTrend": [
      {
        "date": "2024-01-01",
        "errorCount": 50
      },
      {
        "date": "2024-01-02",
        "errorCount": 45
      }
    ]
  }
}
```

## 4. 日志导出接口

### 4.1 日志导出

**接口地址**: `POST /api/log/export`

**请求参数**:
```json
{
  "level": "INFO",                    // 日志级别过滤，可选
  "category": "SYSTEM",               // 日志类别过滤，可选
  "vmId": "a1b2c3d4e5f678901234567890123456",           // 虚拟机ID过滤，可选
  "taskId": "b2c3d4e5f67890123456789012345678",       // 任务ID过滤，可选
  "startTime": "2024-01-01T00:00:00", // 开始时间，可选
  "endTime": "2024-01-02T00:00:00",   // 结束时间，可选
  "keyword": "string",                // 关键词搜索，可选
  "format": "CSV",                    // 导出格式，CSV/JSON/EXCEL，默认CSV
  "includeDetails": true              // 是否包含详细信息，可选，默认true
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "导出任务已创建",
  "data": {
    "exportId": "export_1234567890",
    "status": "PROCESSING",
    "estimatedTime": 30,
    "downloadUrl": "http://localhost:8080/api/log/export/download/export_1234567890"
  }
}
```

### 4.2 导出状态查询

**接口地址**: `GET /api/log/export/status/{exportId}`

**路径参数**:
- exportId: 导出任务ID

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "exportId": "export_1234567890",
    "status": "COMPLETED",
    "progress": 100,
    "totalRecords": 5000,
    "processedRecords": 5000,
    "fileSize": 1048576,
    "downloadUrl": "http://localhost:8080/api/log/export/download/export_1234567890",
    "expiresAt": "2024-01-02T10:00:00",
    "createdAt": "2024-01-01T10:00:00",
    "completedAt": "2024-01-01T10:00:30"
  }
}
```

### 4.3 导出文件下载

**接口地址**: `GET /api/log/export/download/{exportId}`

**路径参数**:
- exportId: 导出任务ID

**响应**: 文件流

### 4.4 导出历史查询

**接口地址**: `GET /api/log/export/history`

**请求参数**:
```
?status=COMPLETED&page=1&size=10
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
        "exportId": "export_1234567890",
        "status": "COMPLETED",
        "format": "CSV",
        "totalRecords": 5000,
        "fileSize": 1048576,
        "createdAt": "2024-01-01T10:00:00",
        "completedAt": "2024-01-01T10:00:30"
      }
    ]
  }
}
```

## 5. 日志清理接口

### 5.1 日志清理

**接口地址**: `POST /api/log/cleanup`

**请求参数**:
```json
{
  "strategy": "TIME_BASED",           // 清理策略，TIME_BASED/LEVEL_BASED/SIZE_BASED
  "retentionDays": 30,                // 保留天数（时间策略），可选
  "level": "DEBUG",                   // 清理级别（级别策略），可选
  "maxSizeGB": 10,                    // 最大大小GB（大小策略），可选
  "category": "SYSTEM",               // 日志类别过滤，可选
  "vmId": "a1b2c3d4e5f678901234567890123456",           // 虚拟机ID过滤，可选
  "taskId": "b2c3d4e5f67890123456789012345678",       // 任务ID过滤，可选
  "dryRun": false                     // 试运行模式，可选，默认false
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "清理任务已创建",
  "data": {
    "cleanupId": "cleanup_1234567890",
    "status": "PROCESSING",
    "estimatedRecords": 5000,
    "estimatedSize": 104857600,
    "dryRun": false
  }
}
```

### 5.2 清理状态查询

**接口地址**: `GET /api/log/cleanup/status/{cleanupId}`

**路径参数**:
- cleanupId: 清理任务ID

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "cleanupId": "cleanup_1234567890",
    "status": "COMPLETED",
    "progress": 100,
    "deletedRecords": 5000,
    "freedSpace": 104857600,
    "createdAt": "2024-01-01T10:00:00",
    "completedAt": "2024-01-01T10:00:30"
  }
}
```

### 5.3 清理历史查询

**接口地址**: `GET /api/log/cleanup/history`

**请求参数**:
```
?status=COMPLETED&page=1&size=10
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
        "cleanupId": "cleanup_1234567890",
        "strategy": "TIME_BASED",
        "status": "COMPLETED",
        "deletedRecords": 5000,
        "freedSpace": 104857600,
        "createdAt": "2024-01-01T10:00:00",
        "completedAt": "2024-01-01T10:00:30"
      }
    ]
  }
}
```

## 6. 系统监控接口

### 6.1 系统状态监控

**接口地址**: `GET /api/log/monitor/system`

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "systemInfo": {
      "version": "1.0.0",
      "uptime": 86400,
      "startTime": "2024-01-01T00:00:00",
      "javaVersion": "11.0.12",
      "osInfo": "Linux 5.4.0"
    },
    "resourceUsage": {
      "cpuUsage": 25.5,
      "memoryUsage": 60.2,
      "diskUsage": 45.8,
      "networkIO": {
        "bytesIn": 1048576,
        "bytesOut": 2097152
      }
    },
    "applicationMetrics": {
      "activeConnections": 150,
      "requestPerSecond": 25.5,
      "averageResponseTime": 200,
      "errorRate": 0.5
    },
    "databaseMetrics": {
      "activeConnections": 20,
      "queryPerSecond": 100,
      "averageQueryTime": 50
    }
  }
}
```

### 6.2 日志监控

**接口地址**: `GET /api/log/monitor/logs`

**请求参数**:
```
?timeRange=1h&level=ERROR
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| timeRange | string | 否 | 时间范围，1h/6h/24h/7d，默认1h |
| level | string | 否 | 日志级别过滤 |

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "logMetrics": {
      "totalLogs": 10000,
      "errorCount": 50,
      "warningCount": 150,
      "errorRate": 0.5,
      "warningRate": 1.5
    },
    "levelTrend": [
      {
        "timestamp": "2024-01-01T10:00:00",
        "DEBUG": 100,
        "INFO": 500,
        "WARN": 50,
        "ERROR": 10
      }
    ],
    "categoryTrend": [
      {
        "timestamp": "2024-01-01T10:00:00",
        "SYSTEM": 200,
        "USER": 150,
        "VM": 100,
        "TASK": 50
      }
    ],
    "recentErrors": [
      {
        "logId": "c3d4e5f6789012345678901234567890",
        "level": "ERROR",
        "category": "TASK",
        "message": "任务执行失败",
        "createdAt": "2024-01-01T10:00:00"
      }
    ]
  }
}
```

### 6.3 性能监控

**接口地址**: `GET /api/log/monitor/performance`

**请求参数**:
```
?timeRange=1h&endpoint=string
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "apiMetrics": {
      "totalRequests": 50000,
      "successfulRequests": 49500,
      "failedRequests": 500,
      "successRate": 99.0,
      "averageResponseTime": 200,
      "p95ResponseTime": 500,
      "p99ResponseTime": 1000
    },
    "endpointMetrics": [
      {
        "endpoint": "/api/user/login",
        "requestCount": 1000,
        "successRate": 98.5,
        "averageResponseTime": 150,
        "errorCount": 15
      }
    ],
    "responseTimeTrend": [
      {
        "timestamp": "2024-01-01T10:00:00",
        "average": 200,
        "p95": 500,
        "p99": 1000
      }
    ]
  }
}
```

### 6.4 告警配置

**接口地址**: `GET /api/log/monitor/alerts`

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "alerts": [
      {
        "alertId": "alert_1234567890",
        "name": "错误率告警",
        "type": "ERROR_RATE",
        "condition": "error_rate > 5%",
        "status": "ACTIVE",
        "lastTriggered": "2024-01-01T10:00:00",
        "triggerCount": 5
      }
    ],
    "alertHistory": [
      {
        "alertId": "alert_1234567890",
        "triggeredAt": "2024-01-01T10:00:00",
        "message": "错误率超过阈值：6.2%",
        "severity": "HIGH"
      }
    ]
  }
}
```

## 7. 日志配置接口

### 7.1 日志配置查询

**接口地址**: `GET /api/log/config`

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "logLevel": "INFO",
    "retentionDays": 30,
    "maxFileSize": 104857600,
    "categories": {
      "SYSTEM": {
        "level": "INFO",
        "enabled": true
      },
      "USER": {
        "level": "INFO",
        "enabled": true
      },
      "VM": {
        "level": "DEBUG",
        "enabled": true
      }
    },
    "exportSettings": {
      "maxRecordsPerExport": 100000,
      "exportRetentionDays": 7,
      "supportedFormats": ["CSV", "JSON", "EXCEL"]
    }
  }
}
```

### 7.2 日志配置更新

**接口地址**: `PUT /api/log/config`

**请求参数**:
```json
{
  "logLevel": "INFO",                 // 全局日志级别，可选
  "retentionDays": 30,                // 保留天数，可选
  "maxFileSize": 104857600,           // 最大文件大小，可选
  "categories": {                     // 类别配置，可选
    "SYSTEM": {
      "level": "INFO",
      "enabled": true
    }
  },
  "exportSettings": {                 // 导出设置，可选
    "maxRecordsPerExport": 100000,
    "exportRetentionDays": 7
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "配置更新成功",
  "data": {
    "updatedAt": "2024-01-01T10:00:00"
  }
}
```

## 8. 错误码定义

### 8.1 系统日志管理错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| LOG_NOT_FOUND | 404 | 日志不存在 |
| LOG_QUERY_FAILED | 500 | 日志查询失败 |
| LOG_EXPORT_FAILED | 500 | 日志导出失败 |
| LOG_CLEANUP_FAILED | 500 | 日志清理失败 |
| LOG_CONFIG_INVALID | 400 | 日志配置无效 |
| EXPORT_NOT_FOUND | 404 | 导出任务不存在 |
| EXPORT_EXPIRED | 410 | 导出文件已过期 |
| CLEANUP_NOT_FOUND | 404 | 清理任务不存在 |
| MONITOR_DATA_UNAVAILABLE | 503 | 监控数据不可用 |

## 9. 安全规范

### 9.1 访问控制
- 日志查询权限：VIEWER及以上角色
- 日志导出权限：OPERATOR及以上角色
- 日志清理权限：ADMIN角色
- 配置修改权限：ADMIN角色

### 9.2 数据保护
- 敏感信息脱敏：密码、Token等敏感信息在日志中脱敏显示
- 日志加密：重要日志文件加密存储
- 访问审计：记录所有日志访问操作

### 9.3 隐私保护
- 用户隐私：不记录用户个人敏感信息
- 数据最小化：只记录必要的系统运行信息
- 合规性：符合数据保护法规要求

## 10. 性能规范

### 10.1 查询性能
- 日志列表查询响应时间 < 1000ms
- 实时日志查询响应时间 < 500ms
- 统计查询响应时间 < 2000ms

### 10.2 导出性能
- 支持异步导出处理
- 大文件分片下载
- 导出任务队列管理

### 10.3 清理性能
- 异步清理处理
- 分批删除避免锁表
- 清理进度实时反馈

## 11. 相关文档

- [HTTP接口导览](../HTTP接口导览.md) - 系统API接口总览
- [用户管理API参考文档](../user/user-api-reference.md) - 用户管理相关接口
- [联邦学习任务管理API参考文档](../federated-task/federated-task-api-reference.md) - 联邦学习任务管理相关接口
- [训练数据管理API参考文档](../train-data/training-data-api-reference.md) - 训练数据管理相关接口
- [模型版本管理API参考文档](../model/model-version-api-reference.md) - 模型版本管理相关接口
- [数据库表结构文档](../../database/database_schema.md) - 数据库设计
- [WebSocket协议文档](../WebSocket/) - 实时通信协议 