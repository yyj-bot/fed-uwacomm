# 已移除的接口总览 (v1.0)

## 概述
本文档记录了在v1.0系统架构升级过程中被移除的所有接口。这些接口因为架构简化、性能优化或功能重构等原因被移除或替换。

## 移除时间
- **移除版本**: v1.0
- **移除日期**: 2024年系统架构升级
- **影响范围**: 权限管理、日志导出功能

---

## 一、权限管理相关接口 (已移除)

### 移除原因
- **简化架构**: 从复杂的细粒度权限表管理简化为基于用户角色的权限控制
- **提升性能**: 减少数据库查询次数，权限判断直接基于用户角色
- **易于维护**: 权限逻辑集中化，便于理解和维护

### admin-api-reference.md-1.9 获取用户权限 (已移除)

**接口地址**: `GET /api/admin/user/{userId}/permissions`

**请求头**:
```
Authorization: Bearer {token}
```

**路径参数**:
- userId: 用户ID (string, 必填)

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": [
    {
      "id": "p1b2c3d4e5f678901234567890123456",
      "resourceType": "DATA",
      "resourceId": "data_123",
      "permission": "READ",
      "grantedAt": "2024-01-01T10:00:00",
      "grantedBy": "admin",
      "expiresAt": null
    },
    {
      "id": "p2b3c4d5e6f789012345678901234567",
      "resourceType": "VM",
      "resourceId": "vm_456",
      "permission": "WRITE",
      "grantedAt": "2024-01-01T11:00:00",
      "grantedBy": "admin",
      "expiresAt": "2024-02-01T11:00:00"
    }
  ]
}
```

### admin-api-reference.md-1.10 授予用户权限 (已移除)

**接口地址**: `POST /api/admin/user/{userId}/permissions`

**请求头**:
```
Authorization: Bearer {token}
```

**路径参数**:
- userId: 用户ID (string, 必填)

**请求参数**:
```json
{
  "resourceType": "VM",                    // 资源类型，必填 (VM, DATA, SYSTEM等)
  "resourceId": "vm_456",                  // 资源ID，必填
  "permission": "READ",                    // 权限类型，必填 (read, WRITE, EXECUTE, DELETE等)
  "expiresAt": "2024-02-01T11:00:00"      // 过期时间，可选
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "权限授予成功",
  "data": {
    "permissionId": "p1b2c3d4e5f678901234567890123456",
    "grantedAt": "2024-01-01T10:00:00"
  }
}
```

### admin-api-reference.md-1.11 撤销用户权限 (已移除)

**接口地址**: `DELETE /api/admin/user/{userId}/permissions/{permissionId}`

**请求头**:
```
Authorization: Bearer {token}
```

**路径参数**:
- userId: 用户ID (string, 必填)
- permissionId: 权限ID (string, 必填)

**响应示例**:
```json
{
  "code": 200,
  "message": "权限撤销成功",
  "data": null
}
```

---

## 二、日志导出相关接口 (已移除)

### 移除原因
- **简化架构**: 从复杂的异步任务管理模式简化为同步直接下载
- **提升性能**: 减少任务状态管理开销，避免文件存储管理复杂性
- **易于维护**: 去除异步任务队列，简化错误处理和状态追踪

### system-log-api-reference.md-4.1 创建导出任务 (已移除)

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

### system-log-api-reference.md-4.2 导出状态查询 (已移除)

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

### system-log-api-reference.md-4.3 导出文件下载 (已移除并替换)

**接口地址**: `GET /api/log/export/download/{exportId}`

**路径参数**:
- exportId: 导出任务ID

**响应**: 文件流

**注意**: 此接口已被替换为新的同步下载接口 `POST /api/log/download`

### system-log-api-reference.md-4.4 导出历史查询 (已移除)

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

---

## 三、替代方案

### 权限管理替代方案

原有的细粒度权限管理已被基于角色的权限系统替代：

#### 角色权限对应关系

| 角色 | 权限范围 | 说明 |
|------|----------|------|
| ADMIN | READ, WRITE, EXECUTE, DELETE | 系统管理员，拥有所有权限 |
| RESEARCHER | READ, WRITE, EXECUTE | 研究人员，可以读写执行，但不能删除 |
| OPERATOR | READ, EXECUTE | 操作员，可以读取和执行操作 |
| VIEWER | READ | 查看者，只能读取信息 |

#### 权限管理方式

1. **修改用户权限**: 通过修改用户角色实现
   ```
   PUT /api/admin/user/{userId}
   {
     "role": "RESEARCHER"
   }
   ```

2. **权限检查**: 在业务逻辑中直接基于用户角色判断
   - 服务层自动根据用户角色进行权限验证
   - 无需额外的权限查询接口

### 日志导出替代方案

原有的异步导出任务管理已被同步直接下载替代：

#### 新的同步下载接口

**接口地址**: `POST /api/log/download`

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

**响应**: 直接返回文件流，无需任务管理

#### 优势
- 简化操作流程，一步完成下载
- 减少服务器存储压力
- 提高响应速度
- 降低系统复杂度

---

## 四、迁移指南

### 权限管理迁移

1. **原权限查询**:
   - 旧方式: `GET /api/admin/user/{userId}/permissions`
   - 新方式: `GET /api/admin/user/{userId}` 查看用户角色，根据角色确定权限

2. **原权限授予**:
   - 旧方式: `POST /api/admin/user/{userId}/permissions`
   - 新方式: `PUT /api/admin/user/{userId}` 修改用户角色

3. **原权限撤销**:
   - 旧方式: `DELETE /api/admin/user/{userId}/permissions/{permissionId}`
   - 新方式: `PUT /api/admin/user/{userId}` 降低用户角色或设为VIEWER

### 日志导出迁移

1. **原异步导出**:
   - 旧方式: `POST /api/log/export` → `GET /api/log/export/status/{exportId}` → `GET /api/log/export/download/{exportId}`
   - 新方式: `POST /api/log/download` 直接下载

2. **导出参数保持一致**: 新接口使用相同的过滤参数

3. **客户端代码调整**: 将多步异步调用改为单步同步调用

---

## 五、常见错误响应 (历史记录)

这些接口的常见错误响应格式：

```json
{
  "code": 400,
  "message": "权限不存在/导出任务不存在",
  "data": null
}
```

常见错误码：
- `400`: 参数错误
- `401`: 认证失败
- `403`: 权限不足
- `404`: 资源不存在
- `409`: 资源已存在
- `500`: 服务器内部错误

---

## 六、版本信息

- **移除版本**: v1.0
- **移除日期**: 2024年系统架构升级
- **影响范围**: 权限管理、日志导出功能
- **向下兼容**: 不兼容，需要按照迁移指南更新客户端代码

---

## 七、相关文档

- [用户管理API参考文档](../user/user-api-reference.md) - 用户管理相关接口
- [管理员API参考文档](../admin/admin-api-reference.md) - 管理员接口（已更新）
- [系统日志API参考文档](../system-log/system-log-api-reference.md) - 日志管理接口（已更新）
- [HTTP接口导览](../HTTP接口导览.md) - 系统API接口总览