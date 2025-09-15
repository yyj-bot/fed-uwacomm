# 已移除的权限管理接口

## 概述
本文档记录了在系统架构升级过程中被移除的用户权限管理接口。这些接口在v1.0版本中被基于角色的权限管理系统替代。

## 移除原因
- **简化架构**: 从复杂的细粒度权限表管理简化为基于用户角色的权限控制
- **提升性能**: 减少数据库查询次数，权限判断直接基于用户角色
- **易于维护**: 权限逻辑集中化，便于理解和维护

## 已移除的接口

### 1.9 获取用户权限 (已移除)

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

### 1.10 授予用户权限 (已移除)

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
  "permission": "READ",                    // 权限类型，必填 (READ, WRITE, EXECUTE, DELETE等)
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

### 1.11 撤销用户权限 (已移除)

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

## 替代方案

### 基于角色的权限管理

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

#### 迁移指南

1. **原权限查询**:
   - 旧方式: `GET /api/admin/user/{userId}/permissions`
   - 新方式: `GET /api/admin/user/{userId}` 查看用户角色，根据角色确定权限

2. **原权限授予**:
   - 旧方式: `POST /api/admin/user/{userId}/permissions`
   - 新方式: `PUT /api/admin/user/{userId}` 修改用户角色

3. **原权限撤销**:
   - 旧方式: `DELETE /api/admin/user/{userId}/permissions/{permissionId}`
   - 新方式: `PUT /api/admin/user/{userId}` 降低用户角色或设为VIEWER

## 错误响应 (历史记录)

这些接口的常见错误响应格式：

```json
{
  "code": 400,
  "message": "权限不存在",
  "data": null
}
```

常见错误码：
- `400`: 权限参数错误
- `401`: 认证失败
- `403`: 权限不足
- `404`: 用户或权限不存在
- `409`: 权限已存在
- `500`: 服务器内部错误

## 版本信息

- **移除版本**: v1.0
- **移除日期**: 2024年系统架构升级
- **影响范围**: 管理员权限管理功能
- **向下兼容**: 不兼容，需要按照迁移指南更新客户端代码