# 水声联邦学习系统 用户管理接口实现文档

## 概述

本文档详细说明了水声联邦学习系统中已完整实现的用户管理接口，包括认证、用户管理、权限管理等功能。

## 接口总览

### 1. 认证相关接口

#### 1.1 用户注册
- **接口地址**: `POST /api/user/register`
- **功能**: 新用户注册
- **请求参数**: `UserRegisterDTO` (用户名、邮箱、密码、确认密码)
- **响应**: `UserRegisterResponseVO` (用户ID、用户名、邮箱、角色、状态、创建时间)
- **实现状态**: ✅ 已完成

#### 1.2 用户登录
- **接口地址**: `POST /api/user/login`
- **功能**: 用户登录认证
- **请求参数**: `UserLoginDTO` (登录标识符、密码、记住登录等)
- **响应**: `LoginResponseVO` (Token、刷新Token、过期时间、用户信息)
- **实现状态**: ✅ 已完成

#### 1.3 刷新Token
- **接口地址**: `POST /api/user/refresh`
- **功能**: 刷新访问Token
- **请求头**: `Authorization: Bearer {refreshToken}`
- **响应**: `TokenRefreshResponseVO` (新Token、新刷新Token、过期时间)
- **实现状态**: ✅ 已完成

#### 1.4 用户登出
- **接口地址**: `POST /api/user/logout`
- **功能**: 用户登出
- **请求头**: `Authorization: Bearer {token}`
- **响应**: 成功消息
- **实现状态**: ✅ 已完成

### 2. 用户信息相关接口

#### 2.1 获取当前用户信息
- **接口地址**: `GET /api/user/profile`
- **功能**: 获取当前登录用户信息
- **请求头**: `Authorization: Bearer {token}`
- **响应**: `UserInfoVO` (用户详细信息)
- **实现状态**: ✅ 已完成

#### 2.2 更新用户信息
- **接口地址**: `PUT /api/user/profile`
- **功能**: 更新当前用户信息
- **请求头**: `Authorization: Bearer {token}`
- **请求参数**: `UserUpdateDTO` (用户名、邮箱、密码等)
- **响应**: `UserUpdateResponseVO` (更新后的用户信息)
- **实现状态**: ✅ 已完成

#### 2.3 修改密码
- **接口地址**: `PUT /api/user/password`
- **功能**: 修改用户密码
- **请求头**: `Authorization: Bearer {token}`
- **请求参数**: `PasswordChangeDTO` (旧密码、新密码、确认密码)
- **响应**: 成功消息
- **实现状态**: ✅ 已完成

### 3. 管理员功能接口

#### 3.1 获取用户列表
- **接口地址**: `GET /api/user/list`
- **功能**: 分页获取用户列表
- **请求头**: `Authorization: Bearer {token}`
- **查询参数**: `UserQueryDTO` (页码、每页大小、角色、状态、关键词)
- **响应**: `PageResponseDTO<UserListVO>` (分页用户列表)
- **实现状态**: ✅ 已完成

#### 3.2 获取用户详情
- **接口地址**: `GET /api/user/{userId}`
- **功能**: 获取指定用户详细信息
- **请求头**: `Authorization: Bearer {token}`
- **响应**: `UserDetailVO` (用户详细信息)
- **实现状态**: ✅ 已完成

#### 3.3 创建用户
- **接口地址**: `POST /api/user/create`
- **功能**: 管理员创建新用户
- **请求头**: `Authorization: Bearer {token}`
- **请求参数**: `UserCreateDTO` (用户名、邮箱、密码、角色)
- **响应**: `UserCreateResponseVO` (创建成功的用户信息)
- **实现状态**: ✅ 已完成

#### 3.4 锁定用户
- **接口地址**: `POST /api/user/{userId}/lock`
- **功能**: 锁定指定用户
- **请求头**: `Authorization: Bearer {token}`
- **请求参数**: `UserLockDTO` (锁定时长、锁定原因)
- **响应**: `UserLockResponseVO` (锁定结果信息)
- **实现状态**: ✅ 已完成

#### 3.5 解锁用户
- **接口地址**: `POST /api/user/{userId}/unlock`
- **功能**: 解锁指定用户
- **请求头**: `Authorization: Bearer {token}`
- **响应**: `UserUnlockResponseVO` (解锁结果信息)
- **实现状态**: ✅ 已完成

#### 3.6 重置用户密码
- **接口地址**: `POST /api/user/{userId}/reset-password`
- **功能**: 重置指定用户密码
- **请求头**: `Authorization: Bearer {token}`
- **请求参数**: `PasswordResetDTO` (新密码)
- **响应**: 成功消息
- **实现状态**: ✅ 已完成

#### 3.7 删除用户
- **接口地址**: `DELETE /api/user/{userId}`
- **功能**: 软删除指定用户
- **请求头**: `Authorization: Bearer {token}`
- **响应**: 成功消息
- **实现状态**: ✅ 已完成

### 4. 权限管理接口

#### 4.1 获取用户权限列表
- **接口地址**: `GET /api/user/{userId}/permissions`
- **功能**: 获取指定用户的所有权限
- **请求头**: `Authorization: Bearer {token}`
- **响应**: `List<UserPermissionVO>` (权限列表)
- **实现状态**: ✅ 已完成

#### 4.2 授予用户权限
- **接口地址**: `POST /api/user/{userId}/permissions`
- **功能**: 授予用户特定权限
- **请求头**: `Authorization: Bearer {token}`
- **请求参数**: `PermissionGrantDTO` (资源类型、资源ID、权限、过期时间)
- **响应**: `PermissionGrantResponseVO` (权限授予结果)
- **实现状态**: ✅ 已完成

#### 4.3 撤销用户权限
- **接口地址**: `DELETE /api/user/{userId}/permissions/{permissionId}`
- **功能**: 撤销用户特定权限
- **请求头**: `Authorization: Bearer {token}`
- **响应**: 成功消息
- **实现状态**: ✅ 已完成

## 核心组件实现

### 1. 数据访问层 (Mapper)

#### UserMapper
- ✅ 基础CRUD操作
- ✅ 分页查询和条件查询
- ✅ 用户锁定/解锁操作
- ✅ 密码更新操作

#### UserPermissionMapper
- ✅ 权限CRUD操作
- ✅ 权限查询和检查
- ✅ 批量权限操作

### 2. 业务逻辑层 (Service)

#### UserService
- ✅ 用户认证逻辑
- ✅ 用户管理逻辑
- ✅ 权限管理逻辑
- ✅ 密码安全处理
- ✅ 登录失败处理

### 3. 控制器层 (Controller)

#### UserController
- ✅ 所有接口端点
- ✅ 参数验证
- ✅ 响应封装
- ✅ 异常处理

### 4. 工具类

#### 安全工具
- ✅ `PasswordUtil`: 密码加密和验证
- ✅ `JwtUtil`: JWT Token生成和验证
- ✅ `UuidUtil`: UUID生成工具

#### 通用工具
- ✅ `IpUtil`: IP地址获取工具
- ✅ `BaseContext`: 用户上下文管理
- ✅ `Result`: 统一响应封装

## 数据库设计

### 用户表 (users)
```sql
CREATE TABLE users (
    id VARCHAR(32) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_time DATETIME,
    last_login_ip VARCHAR(45),
    login_attempts INT DEFAULT 0,
    locked_until DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(32),
    updated_by VARCHAR(32)
);
```

### 用户权限表 (user_permissions)
```sql
CREATE TABLE user_permissions (
    id VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(32),
    permission VARCHAR(50) NOT NULL,
    granted_at DATETIME NOT NULL,
    granted_by VARCHAR(32) NOT NULL,
    expires_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## 安全特性

### 1. 密码安全
- ✅ BCrypt加密算法
- ✅ 密码强度验证
- ✅ 密码历史检查

### 2. 登录安全
- ✅ 连续失败锁定机制
- ✅ JWT Token认证
- ✅ 刷新Token机制
- ✅ 会话管理

### 3. 权限控制
- ✅ 基于角色的访问控制(RBAC)
- ✅ 细粒度权限控制
- ✅ 权限过期机制

## 测试覆盖

### 单元测试
- ✅ `UserControllerTest`: 控制器层测试
- ✅ 所有接口功能测试
- ✅ 异常情况测试

### 集成测试
- ✅ 数据库操作测试
- ✅ 服务层集成测试

## 部署说明

### 1. 环境要求
- Java 17+
- Spring Boot 3.x
- MySQL 8.0+
- Maven 3.6+

### 2. 配置说明
- 数据库连接配置: `application.yml`
- JWT密钥配置: `JwtUtil.java`
- 密码策略配置: `PasswordUtil.java`

### 3. 启动步骤
1. 创建数据库和表结构
2. 配置数据库连接
3. 启动Spring Boot应用
4. 验证接口可用性

## 接口测试

### 使用Postman测试
1. 导入接口集合
2. 配置环境变量
3. 执行测试用例

### 使用curl测试
```bash
# 用户注册
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123","confirmPassword":"password123"}'

# 用户登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginIdentifier":"testuser","password":"password123"}'
```

## 注意事项

### 1. 安全考虑
- 生产环境需要修改JWT密钥
- 启用HTTPS
- 配置CORS策略
- 添加请求频率限制

### 2. 性能优化
- 数据库索引优化
- 缓存策略
- 连接池配置

### 3. 监控告警
- 接口响应时间监控
- 错误率监控
- 用户行为分析

## 更新日志

### v1.0.0 (2024-01-01)
- ✅ 完成所有基础用户接口
- ✅ 实现完整的权限管理系统
- ✅ 添加全面的测试覆盖
- ✅ 完善异常处理和日志记录

## 联系方式

如有问题或建议，请联系开发团队。 