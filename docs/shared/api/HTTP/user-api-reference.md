# 水声联邦学习系统 用户管理 API 接口文档

## 1. 概述

本文档定义了水声联邦学习系统的用户管理相关HTTP REST API接口，包括用户注册、登录、权限管理等功能。

### 1.1 基础信息
- **基础URL**: `http://localhost:8080/api/user`
- **API版本**: v1.0
- **认证方式**: JWT Token (除登录、注册接口外)
- **数据格式**: JSON

### 1.2 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 2. 用户角色说明

### 2.1 角色定义
| 角色 | 权限说明 |
|------|----------|
| ADMIN | 系统管理员，拥有所有权限 |
| RESEARCHER | 研究人员，可以创建和管理联邦学习任务 |
| OPERATOR | 操作员，可以执行任务和查看数据 |
| VIEWER | 查看者，只能查看系统状态和数据 |

### 2.2 用户状态
| 状态 | 说明 |
|------|------|
| ACTIVE | 活跃状态，可以正常登录 |
| INACTIVE | 非活跃状态，不能登录 |
| LOCKED | 锁定状态，不能登录 |
| DELETED | 已删除状态 |

## 3. 认证接口

### 3.1 用户注册

**接口地址**: `POST /api/user/register`

**请求参数**:
```json
{
  "username": "string",        // 用户名，必填，长度3-50字符
  "email": "string",           // 邮箱地址，必填，符合邮箱格式，唯一
  "password": "string",        // 密码，必填，长度6-20字符
  "confirmPassword": "string"  // 确认密码，必填，必须与密码一致
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": "a1b2c3d4e5f678901234567890123456",
    "username": "testuser",
    "email": "test@example.com",
    "role": "VIEWER",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "参数验证失败",
  "data": {
    "field": "email",
    "error": "邮箱格式不正确"
  }
}
```

### 3.2 用户登录

**接口地址**: `POST /api/user/login`

**请求参数**:
```json
{
  "loginIdentifier": "string", // 登录标识符（用户名或邮箱），必填
  "password": "string",        // 密码，必填
  "captcha": "string",         // 验证码，可选
  "captchaKey": "string",      // 验证码标识，可选
  "rememberMe": false          // 记住登录状态，可选，默认false
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400,
    "user": {
      "userId": "a1b2c3d4e5f678901234567890123456",
      "username": "testuser",
      "email": "test@example.com",
      "role": "VIEWER",
      "status": "ACTIVE",
      "lastLoginTime": "2024-01-01T10:00:00",
      "lastLoginIp": "192.168.1.100"
    }
  }
}
```

**错误响应**:
```json
{
  "code": 401,
  "message": "账号或密码错误",
  "data": {
    "loginAttempts": 3,
    "lockedUntil": "2024-01-01T11:00:00"
  }
}
```

### 3.3 刷新Token

**接口地址**: `POST /api/user/refresh`

**请求头**:
```
Authorization: Bearer {refreshToken}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Token刷新成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400
  }
}
```

### 3.4 用户登出

**接口地址**: `POST /api/user/logout`

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

## 4. 用户管理接口

### 4.1 获取当前用户信息

**接口地址**: `GET /api/user/profile`

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
    "username": "testuser",
    "email": "test@example.com",
    "role": "VIEWER",
    "status": "ACTIVE",
    "lastLoginTime": "2024-01-01T10:00:00",
    "lastLoginIp": "192.168.1.100",
    "createdAt": "2024-01-01T09:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
}
```

### 4.2 更新用户信息

**接口地址**: `PUT /api/user/profile`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```json
{
  "username": "string",  // 用户名，可选
  "email": "string"      // 邮箱地址，可选
}
```

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

### 4.3 修改密码

**接口地址**: `PUT /api/user/password`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```json
{
  "oldPassword": "string",        // 旧密码，必填
  "newPassword": "string",        // 新密码，必填，长度6-20字符
  "confirmPassword": "string"     // 确认新密码，必填，必须与新密码一致
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```



## 5. 错误码定义

### 5.1 用户相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| USER_NOT_FOUND | 404 | 用户不存在 |
| USER_ALREADY_EXISTS | 409 | 用户已存在 |
| USER_EMAIL_EXISTS | 409 | 邮箱已存在 |
| USER_USERNAME_EXISTS | 409 | 用户名已存在 |
| USER_PASSWORD_ERROR | 401 | 密码错误 |
| USER_ACCOUNT_LOCKED | 423 | 账号已锁定 |
| USER_PERMISSION_DENIED | 403 | 权限不足 |
| LOGIN_IDENTIFIER_INVALID | 400 | 登录标识符格式错误 |

### 5.2 认证相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| TOKEN_EXPIRED | 401 | Token已过期 |
| TOKEN_INVALID | 401 | Token无效 |
| TOKEN_MISSING | 401 | Token缺失 |
| REFRESH_TOKEN_EXPIRED | 401 | 刷新Token已过期 |
| REFRESH_TOKEN_INVALID | 401 | 刷新Token无效 |

### 5.3 参数验证错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| PARAM_VALIDATION_ERROR | 400 | 参数验证失败 |
| PASSWORD_TOO_SHORT | 400 | 密码长度不足 |
| PASSWORD_TOO_LONG | 400 | 密码长度过长 |
| PASSWORD_MISMATCH | 400 | 密码不匹配 |
| EMAIL_FORMAT_ERROR | 400 | 邮箱格式错误 |
| USERNAME_FORMAT_ERROR | 400 | 用户名格式错误 |
| LOGIN_IDENTIFIER_FORMAT_ERROR | 400 | 登录标识符格式错误 |

## 6. 安全规范

### 6.1 密码安全
- 密码长度：6-20字符
- 密码复杂度：建议包含字母、数字、特殊字符
- 密码加密：使用BCrypt算法加密存储
- 密码历史：不支持重复使用最近3次密码

### 6.2 登录安全
- 登录失败锁定：连续失败5次锁定1小时
- 会话管理：Token过期时间24小时
- 并发登录：支持多设备同时登录
- 异常检测：检测异常登录行为

### 6.3 权限控制
- 基于角色的访问控制(RBAC)
- 细粒度权限控制
- 权限继承机制
- 权限审计日志

## 7. 性能规范

### 7.1 响应时间
- 登录接口：< 500ms
- 查询接口：< 200ms
- 创建接口：< 1000ms
- 更新接口：< 500ms

### 7.2 并发处理
- 支持1000并发登录
- 用户列表分页查询
- 权限检查缓存

## 8. 相关文档

- [HTTP接口导览.md](./HTTP接口导览.md) - 系统整体API接口
- [数据库表结构文档](../../database/database_schema.md) - 用户相关数据库设计
- [WebSocket协议文档](../WebSocket/) - 实时通信协议 