# UUID工具类使用指南

## 概述

`UuidUtil` 是一个统一的UUID生成工具类，使用单例设计模式，支持模块名称前缀，确保生成的UUID具有唯一性和可追溯性。

## 特性

- ✅ **单例设计模式**: 确保全局唯一实例
- ✅ **模块名称前缀**: 支持配置模块名称作为UUID前缀
- ✅ **时间戳集成**: 包含生成时间戳，便于排序和追踪
- ✅ **序列号保证**: 同一毫秒内的唯一性保证
- ✅ **类型化生成**: 针对不同业务类型提供专门的生成方法
- ✅ **格式验证**: 提供UUID格式验证功能
- ✅ **信息提取**: 支持从UUID中提取模块名称、前缀、时间戳等信息

## 配置

### 1. 配置文件设置

在 `application.yml` 中配置模块名称：

```yaml
# FedUWAComm模块配置
feduwacomm:
  module:
    name: feduwacomm # 模块名称，用于UUID前缀
```

### 2. 环境变量配置

也可以通过环境变量配置：

```bash
export FEDUWACOMM_MODULE_NAME=feduwacomm
```

## 使用方法

### 1. 依赖注入

```java
@Service
public class UserService {
    
    @Autowired
    private UuidUtil uuidUtil;
    
    // 使用uuidUtil生成UUID
}
```

### 2. 基础UUID生成

```java
// 生成基础UUID（带模块前缀）
String uuid = uuidUtil.generateUuid();

// 生成带自定义前缀的UUID
String customUuid = uuidUtil.generateUuid("custom");
```

### 3. 类型化UUID生成

```java
// 用户相关
String userId = uuidUtil.generateUserId();           // feduwacomm_user_xxx
String permissionId = uuidUtil.generatePermissionId(); // feduwacomm_perm_xxx

// 虚拟机相关
String vmId = uuidUtil.generateVmId();               // feduwacomm_vm_xxx
String vmLogId = uuidUtil.generateVmLogId();         // feduwacomm_vm_log_xxx

// 任务相关
String taskId = uuidUtil.generateTaskId();           // feduwacomm_task_xxx

// 数据相关
String dataId = uuidUtil.generateDataId();           // feduwacomm_data_xxx
String modelId = uuidUtil.generateModelId();         // feduwacomm_model_xxx

// 日志相关
String logId = uuidUtil.generateLogId();             // feduwacomm_log_xxx
```

## UUID格式说明

生成的UUID格式为：
```
{moduleName}_{prefix}_{timestamp}_{sequence}_{randomUUID}
```

### 格式详解

- **moduleName**: 模块名称（如：feduwacomm）
- **prefix**: 前缀标识（如：user、vm、task等）
- **timestamp**: 时间戳（毫秒）
- **sequence**: 序列号（4位数字，同一毫秒内递增）
- **randomUUID**: 32位随机UUID（去除连字符）

### 示例

```
feduwacomm_user_1703123456789_0001_a1b2c3d4e5f678901234567890123456
```

## 验证和解析

### 1. UUID格式验证

```java
// 验证UUID格式是否正确
boolean isValid = uuidUtil.isValidUuid(uuid);
```

### 2. 信息提取

```java
// 提取模块名称
String moduleName = uuidUtil.extractModuleName(uuid);

// 提取前缀
String prefix = uuidUtil.extractPrefix(uuid);

// 提取时间戳
Long timestamp = uuidUtil.extractTimestamp(uuid);
```

## 实际应用示例

### 1. 用户服务中的应用

```java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UuidUtil uuidUtil;
    
    @Override
    @Transactional
    public UserRegisterResponseVO register(UserRegisterDTO registerDTO) {
        // 创建用户
        User user = User.builder()
                .id(uuidUtil.generateUserId())  // 使用统一的UUID生成
                .username(registerDTO.getUsername())
                .account(registerDTO.getAccount())
                .email(registerDTO.getEmail())
                .passwordHash(PasswordUtil.encode(registerDTO.getPassword()))
                .role("VIEWER")
                .status("ACTIVE")
                .build();
        
        userMapper.insert(user);
        // ... 其他逻辑
    }
}
```

### 2. 日志服务中的应用

```java
@Service
public class LogServiceImpl implements LogService {
    
    @Autowired
    private UuidUtil uuidUtil;
    
    private void saveToDatabase(String level, String message, String userId, 
                               String username, String requestUri, String clientIp, 
                               String exception) {
        try {
            String sql = "INSERT INTO system_logs (id, timestamp, level, logger, message, " +
                    "thread, user_id, username, request_uri, client_ip, environment, exception) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(sql,
                    uuidUtil.generateLogId(),  // 使用统一的UUID生成
                    LocalDateTime.now(),
                    level,
                    "com.feduwacomm",
                    message,
                    Thread.currentThread().getName(),
                    userId,
                    username,
                    requestUri,
                    clientIp,
                    environment,
                    exception);
        } catch (Exception e) {
            logger.error("保存日志到数据库失败: {}", e.getMessage());
        }
    }
}
```

### 3. 权限管理中的应用

```java
@Service
public class UserServiceImpl implements UserService {
    
    @Override
    @Transactional
    public PermissionGrantResponseVO grantPermission(String userId, PermissionGrantDTO grantDTO) {
        // 创建权限
        UserPermission permission = UserPermission.builder()
                .id(uuidUtil.generatePermissionId())  // 使用统一的UUID生成
                .userId(userId)
                .resourceType(grantDTO.getResourceType())
                .resourceId(grantDTO.getResourceId())
                .permission(grantDTO.getPermission())
                .grantedAt(LocalDateTime.now())
                .grantedBy(BaseContext.getCurrentUserId())
                .expiresAt(grantDTO.getExpiresAt())
                .build();

        userPermissionMapper.insert(permission);
        // ... 其他逻辑
    }
}
```

## 性能考虑

### 1. 并发安全

- 使用 `AtomicLong` 确保序列号的线程安全
- 使用 `synchronized` 块确保同一毫秒内的序列号递增
- 使用 `volatile` 关键字确保时间戳的可见性

### 2. 性能优化

- UUID生成操作轻量级，不会成为性能瓶颈
- 序列号使用4位数字格式，减少字符串长度
- 时间戳使用毫秒级精度，平衡精度和性能

## 测试

### 1. 运行测试

```bash
# 运行所有测试
mvn test

# 运行UUID工具类测试
mvn test -Dtest=UuidUtilTest
```

### 2. 测试覆盖

测试用例包括：
- UUID生成功能测试
- 唯一性验证测试
- 格式验证测试
- 信息提取测试
- 并发安全测试

## 注意事项

1. **模块名称**: 确保模块名称配置正确，避免不同环境间的冲突
2. **UUID长度**: 生成的UUID较长，确保数据库字段长度足够
3. **排序性能**: 包含时间戳的UUID天然支持时间排序
4. **可读性**: UUID包含业务信息，便于调试和追踪

## 迁移指南

### 从旧UUID格式迁移

如果项目中已有使用 `UUID.randomUUID()` 的代码，可以按以下步骤迁移：

1. **添加依赖注入**:
   ```java
   @Autowired
   private UuidUtil uuidUtil;
   ```

2. **替换UUID生成**:
   ```java
   // 旧方式
   String id = "user_" + UUID.randomUUID().toString().replace("-", "");
   
   // 新方式
   String id = uuidUtil.generateUserId();
   ```

3. **更新数据库字段**: 确保数据库字段长度足够存储新的UUID格式

4. **测试验证**: 运行测试确保功能正常

## 总结

`UuidUtil` 工具类提供了统一的UUID生成解决方案，具有以下优势：

- **统一性**: 所有UUID生成都通过同一个工具类
- **可追溯性**: 包含时间戳和模块信息
- **唯一性**: 通过时间戳和序列号保证唯一性
- **可扩展性**: 支持自定义前缀和模块名称
- **易维护**: 集中管理UUID生成逻辑

通过使用 `UuidUtil`，可以确保项目中所有UUID的生成都遵循统一的规范，提高系统的可维护性和可追溯性。 