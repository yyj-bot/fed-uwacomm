# 管理员接口文档

## 概述
本文档描述了管理员专用的API接口，所有接口都需要管理员权限。

## 基础信息
- **基础URL**: `/api/admin`
- **认证方式**: Bearer Token
- **权限要求**: 管理员角色

## 1. 用户管理

### 1.1 获取用户列表

**接口地址**: `GET /api/admin/user/list`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数** (Query参数):
```
page: number      // 页码，默认1
size: number      // 每页大小，默认10
username: string  // 用户名模糊查询，可选
email: string     // 邮箱模糊查询，可选
role: string      // 角色筛选，可选
status: string    // 状态筛选，可选
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "records": [
      {
        "userId": "a1b2c3d4e5f678901234567890123456",
        "username": "admin",
        "email": "admin@example.com",
        "role": "ADMIN",
        "status": "ACTIVE",
        "createdAt": "2024-01-01T10:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10
  }
}
```

### 1.2 获取用户详情

**接口地址**: `GET /api/admin/user/{userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "userId": "a1b2c3d4e5f678901234567890123456",
    "username": "admin",
    "email": "admin@example.com",
    "role": "ADMIN",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T13:00:00"
  }
}
```

### 1.3 创建用户

**接口地址**: `POST /api/admin/user/create`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```json
{
  "username": "string",      // 用户名，必填
  "email": "string",         // 邮箱地址，必填
  "password": "string",      // 密码，必填
  "role": "string",          // 角色，必填
  "status": "string"         // 状态，可选，默认ACTIVE
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "userId": "a1b2c3d4e5f678901234567890123456",
    "username": "newuser",
    "email": "newuser@example.com",
    "role": "RESEARCHER",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 1.4 更新用户

**接口地址**: `PUT /api/admin/user/{userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```json
{
  "username": "string",      // 用户名，可选
  "email": "string",         // 邮箱地址，可选
  "role": "string",          // 角色，可选
  "status": "string",        // 状态，可选
  "password": "string"       // 新密码，可选，管理员可直接修改用户密码
}
```

**权限要求**:
- 管理员可以修改任何用户信息
- 管理员可以直接修改用户密码，无需旧密码验证

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "userId": "a1b2c3d4e5f678901234567890123456",
    "username": "updateduser",
    "email": "updated@example.com",
    "role": "RESEARCHER",
    "status": "ACTIVE",
    "updatedAt": "2024-01-01T13:00:00"
  }
}
```

### 1.5 删除用户

**接口地址**: `DELETE /api/admin/user/{userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

### 1.6 锁定用户

**接口地址**: `POST /api/admin/user/{userId}/lock`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```json
{
  "duration": 3600  // 锁定时长（秒），可选，默认1小时
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "用户已锁定",
  "data": {
    "userId": "a1b2c3d4e5f678901234567890123456",
    "lockedUntil": "2024-01-01T14:00:00"
  }
}
```

### 1.7 解锁用户

**接口地址**: `POST /api/admin/user/{userId}/unlock`

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "用户已解锁",
  "data": {
    "userId": "a1b2c3d4e5f678901234567890123456",
    "status": "ACTIVE"
  }
}
```

### 1.8 重置用户密码

**接口地址**: `POST /api/admin/user/{userId}/reset-password`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```json
{
  "newPassword": "string"  // 新密码，必填
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "密码重置成功",
  "data": null
}
```

### 1.9 获取用户统计信息

**接口地址**: `GET /api/admin/user/statistics`

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "totalUsers": 25,
    "activeUsers": 20,
    "lockedUsers": 2,
    "adminUsers": 3,
    "researcherUsers": 8,
    "operatorUsers": 6,
    "viewerUsers": 8,
    "todayNewUsers": 2,
    "timestamp": 1642761600000,
    "hasUsers": true
  }
}
```

## 📋 权限管理架构说明

### 基于角色的权限管理

本系统采用基于角色的权限管理（RBAC），通过用户角色来控制权限，替代了原有的细粒度权限表管理方式。

#### 角色权限对应关系

| 角色 | 权限范围 | 说明 |
|------|----------|------|
| ADMIN | READ, WRITE, EXECUTE, DELETE | 系统管理员，拥有所有权限 |
| RESEARCHER | READ, WRITE, EXECUTE | 研究人员，可以读写执行，但不能删除 |
| OPERATOR | READ, EXECUTE | 操作员，可以读取和执行操作 |
| VIEWER | READ | 查看者，只能读取信息 |

#### 权限管理方式

- **修改用户权限**: 通过修改用户角色实现（使用1.4更新用户接口）
- **权限检查**: 服务层自动根据用户角色进行权限验证
- **权限查询**: 通过获取用户详情查看当前角色（使用1.2获取用户详情接口）

> **注意**: 原有的细粒度权限管理接口（获取用户权限、授予用户权限、撤销用户权限）已被移除。相关文档请参考：`/docs/shared/api/HTTP/removed/permission-management-removed.md`

## 2. 相关文档

- [管理员虚拟机管理API](./admin-vm-api-reference.md) - 管理员虚拟机分配和管理相关接口

## 3. 错误响应

所有接口的错误响应格式如下：

```json
{
  "code": 400,
  "message": "错误描述",
  "data": "详细错误信息"
}
```

常见错误码：
- `400`: 请求参数错误
- `401`: 认证失败
- `403`: 权限不足
- `404`: 资源不存在
- `409`: 资源冲突
- `500`: 服务器内部错误