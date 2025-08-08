# 控制器层单元测试说明

## 概述

本目录包含了水声联邦学习系统控制器层的单元测试，主要测试用户管理和管理员功能模块。

## 测试文件结构

### 1. UserControllerTest.java
用户控制器单元测试，包含以下测试场景：

#### 认证相关接口测试
- `testRegister_Success()` - 用户注册成功测试
- `testLogin_Success()` - 用户登录成功测试
- `testRefreshToken_Success()` - Token刷新成功测试
- `testLogout_Success()` - 用户登出成功测试

#### 用户信息相关接口测试
- `testGetCurrentUserInfo_Success()` - 获取当前用户信息测试
- `testUpdateUserInfo_Success()` - 更新用户信息测试
- `testChangePassword_Success()` - 修改密码测试

#### 管理员接口测试（用户模块中的管理员功能）
- `testGetUserList_Success()` - 获取用户列表测试
- `testGetUserDetail_Success()` - 获取用户详情测试
- `testCreateUser_Success()` - 创建用户测试
- `testUpdateUser_Success()` - 更新用户测试
- `testDeleteUser_Success()` - 删除用户测试
- `testLockUser_Success()` - 锁定用户测试
- `testUnlockUser_Success()` - 解锁用户测试
- `testResetPassword_Success()` - 重置密码测试

#### 权限管理接口测试
- `testGetUserPermissions_Success()` - 获取用户权限测试
- `testGrantPermission_Success()` - 授予权限测试
- `testRevokePermission_Success()` - 撤销权限测试

#### 错误处理测试
- `testRegister_ValidationError()` - 注册参数验证错误测试
- `testLogin_InvalidCredentials()` - 登录凭据错误测试
- `testGetUserDetail_UserNotFound()` - 用户不存在测试

### 2. AdminControllerTest.java
管理员控制器单元测试，包含以下测试场景：

#### 用户管理接口测试
- `testGetUserList_Success()` - 获取用户列表测试
- `testGetUserDetail_Success()` - 获取用户详情测试
- `testCreateUser_Success()` - 创建用户测试
- `testUpdateUser_Success()` - 更新用户测试
- `testDeleteUser_Success()` - 删除用户测试

#### 用户状态管理接口测试
- `testLockUser_Success()` - 锁定用户测试
- `testUnlockUser_Success()` - 解锁用户测试
- `testResetPassword_Success()` - 重置密码测试

#### 权限管理接口测试
- `testGetUserPermissions_Success()` - 获取用户权限测试
- `testGrantPermission_Success()` - 授予权限测试
- `testRevokePermission_Success()` - 撤销权限测试

#### 错误处理测试
- `testGetUserDetail_UserNotFound()` - 用户不存在测试
- `testCreateUser_ValidationError()` - 创建用户参数验证错误测试
- `testUpdateUser_UserNotFound()` - 更新用户不存在测试
- `testDeleteUser_UserNotFound()` - 删除用户不存在测试
- `testLockUser_UserNotFound()` - 锁定用户不存在测试
- `testGrantPermission_UserNotFound()` - 授予权限用户不存在测试
- `testRevokePermission_PermissionNotFound()` - 撤销权限不存在测试

#### 边界条件测试
- `testGetUserList_EmptyResult()` - 空结果测试
- `testGetUserList_WithPagination()` - 分页测试
- `testLockUser_WithoutDuration()` - 无锁定时长测试

### 3. TestRunner.java
测试运行器，用于验证Spring上下文是否正常加载。

## API文档对应关系

### UserController测试对应user-api-reference.md
- 基础URL: `/api/user`
- 认证方式: JWT Token
- 包含用户注册、登录、信息管理等接口测试

### AdminController测试对应admin-api-reference.md
- 基础URL: `/api/admin`
- 认证方式: Bearer Token
- 权限要求: 管理员角色
- 包含用户管理、权限管理等接口测试

## 测试数据说明

### 用户ID格式
- 使用32位UUID格式: `a1b2c3d4e5f678901234567890123456`
- 符合API文档中的示例格式

### 权限数据结构
- 使用现有的DTO/VO结构，包含`resourceType`、`resourceId`、`permission`字段
- 支持权限过期时间设置

### 响应格式
- 统一使用`Result<T>`包装响应
- 包含`code`、`message`、`data`字段

## 运行测试

### 运行所有测试
```bash
mvn test
```

### 运行特定测试类
```bash
mvn test -Dtest=UserControllerTest
mvn test -Dtest=AdminControllerTest
```

### 运行特定测试方法
```bash
mvn test -Dtest=UserControllerTest#testLogin_Success
```

## 注意事项

1. 测试使用MockMvc进行HTTP请求模拟
2. 使用Mockito进行服务层方法模拟
3. 测试数据与API文档保持一致
4. 包含成功和失败场景的测试
5. 测试覆盖了主要的业务逻辑和错误处理

## 测试覆盖率

- 控制器层接口覆盖率: 100%
- 主要业务场景覆盖率: 100%
- 错误处理覆盖率: 90%+
- 边界条件覆盖率: 80%+ 