# 用户模块测试说明

## 测试结构

本测试模块包含了用户模块的完整测试覆盖，包括：

### 1. 单元测试 (Unit Tests)

#### UserControllerTest
- **位置**: `com.feduwacomm.controller.UserControllerTest`
- **说明**: 控制器层单元测试，测试所有HTTP接口
- **覆盖范围**: 
  - 用户注册、登录、登出
  - 用户信息管理
  - 管理员功能
  - 权限管理

#### UserServiceTest  
- **位置**: `com.feduwacomm.service.UserServiceTest`
- **说明**: 服务层单元测试，测试业务逻辑
- **覆盖范围**:
  - 用户认证业务逻辑
  - 用户管理业务逻辑
  - 权限管理业务逻辑
  - 异常处理

### 2. 集成测试 (Integration Tests)

#### UserIntegrationTest
- **位置**: `com.feduwacomm.integration.UserIntegrationTest`
- **说明**: 端到端集成测试，测试完整业务流程
- **覆盖范围**:
  - 用户注册和登录流程
  - 用户管理完整流程
  - 密码管理流程
  - 用户锁定和解锁流程
  - 权限管理流程
  - 错误处理和验证

### 3. 测试套件 (Test Suite)

#### UserModuleTestSuite
- **位置**: `com.feduwacomm.UserModuleTestSuite`
- **说明**: 测试套件，用于运行所有用户模块测试
- **功能**: 统一执行所有用户相关测试

## 运行测试

### 运行所有测试
```bash
# 在backend-springboot/feduwacomm-server目录下执行
mvn test
```

### 运行特定测试类
```bash
# 运行控制器测试
mvn test -Dtest=UserControllerTest

# 运行服务层测试
mvn test -Dtest=UserServiceTest

# 运行集成测试
mvn test -Dtest=UserIntegrationTest
```

### 运行测试套件
```bash
# 运行用户模块测试套件
mvn test -Dtest=UserModuleTestSuite
```

## 测试配置

### 测试环境配置
- **配置文件**: `src/test/resources/application-test.yml`
- **数据库**: H2内存数据库
- **日志级别**: DEBUG
- **端口**: 随机端口（避免冲突）

### 测试数据
- 使用H2内存数据库，每次测试都会重新创建数据库
- 测试数据在测试方法中动态创建
- 使用@Transactional注解确保测试数据隔离

## 测试覆盖范围

### 功能覆盖
- ✅ 用户注册
- ✅ 用户登录
- ✅ Token刷新
- ✅ 用户登出
- ✅ 获取用户信息
- ✅ 更新用户信息
- ✅ 修改密码
- ✅ 用户列表查询
- ✅ 用户详情查询
- ✅ 创建用户
- ✅ 更新用户
- ✅ 删除用户
- ✅ 锁定用户
- ✅ 解锁用户
- ✅ 重置密码
- ✅ 权限管理

### 异常处理覆盖
- ✅ 参数验证错误
- ✅ 用户不存在
- ✅ 账号已存在
- ✅ 邮箱已存在
- ✅ 用户名已存在
- ✅ 密码错误
- ✅ 账号锁定
- ✅ Token无效
- ✅ 权限不足

## 测试最佳实践

### 1. 测试命名规范
- 测试方法名格式：`test[方法名]_[场景]_[预期结果]`
- 例如：`testRegister_Success`、`testLogin_UserNotFound`

### 2. 测试结构
- 使用@BeforeEach进行测试数据初始化
- 每个测试方法包含：准备数据、执行测试、验证结果
- 使用断言验证测试结果

### 3. Mock使用
- 使用@Mock注解模拟依赖
- 使用when().thenReturn()设置模拟行为
- 使用verify()验证方法调用

### 4. 异常测试
- 使用assertThrows()测试异常情况
- 验证异常类型和错误信息

## 注意事项

1. **测试隔离**: 每个测试方法都是独立的，不会相互影响
2. **数据清理**: 使用@Transactional注解自动回滚测试数据（仅对DML操作有效）
3. **DDL事务限制**: ⚠️ **重要** - DDL操作（CREATE、DROP、ALTER TABLE）在MySQL和H2中都会隐式提交事务，无法通过@Transactional回滚
4. **测试数据库策略**: 使用H2内存数据库避免DDL事务问题，每个测试类使用@DirtiesContext确保上下文清理
5. **性能考虑**: 集成测试可能较慢，建议单独运行
6. **环境要求**: 需要Java 17和Maven 3.6+

### DDL操作的测试处理策略

- **数据库初始化**: DatabaseInitService中的CREATE TABLE操作不能在@Transactional中回滚
- **测试环境**: 使用H2内存数据库，在`application-test.yml`中配置自动初始化
- **测试清理**: 使用@DirtiesContext而非@Transactional来确保测试后的环境清理
- **最佳实践**: 
  - DDL相关的服务类不应使用类级别的@Transactional注解
  - 只对DML操作（INSERT、UPDATE、DELETE、SELECT）使用@Transactional
  - 在需要DDL操作的测试中使用@DirtiesContext确保测试隔离

## 故障排除

### 常见问题

1. **测试失败**: 检查测试数据是否正确设置
2. **依赖问题**: 确保所有依赖都已正确配置
3. **数据库连接**: 检查H2数据库配置
4. **端口冲突**: 测试使用随机端口，通常不会有冲突

### 调试技巧

1. 使用@DisplayName注解为测试添加描述
2. 在测试方法中添加日志输出
3. 使用IDE的调试功能单步执行测试
4. 查看测试报告了解失败原因 