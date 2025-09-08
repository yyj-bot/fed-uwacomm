# 用户模块日志系统说明文档

## 1. 概述

本文档描述了用户模块的日志系统配置和使用方法。日志系统基于Log4j2框架，提供了全面的日志记录功能，包括操作日志、安全审计日志、性能监控日志和访问日志。

## 2. 日志系统架构

### 2.1 日志分类

- **操作日志 (Business Log)**: 记录用户的各种操作，如注册、登录、信息更新等
- **安全审计日志 (Security Audit Log)**: 记录安全相关事件，如登录失败、账户锁定、密码修改等
- **性能监控日志 (Performance Log)**: 记录方法执行时间和性能指标
- **访问日志 (Access Log)**: 记录所有API请求的访问信息
- **系统日志 (System Log)**: 记录系统级别的信息和警告

### 2.2 日志输出目标

- **控制台输出**: 开发环境下的实时日志查看
- **文件输出**: 生产环境下的日志持久化存储
- **异步输出**: 提高日志记录性能，不影响业务逻辑执行

## 3. 配置文件说明

### 3.1 主要配置项

```xml
<!-- 日志文件路径 -->
<Property name="LOG_PATH">logs</Property>
<!-- 日志文件大小限制 -->
<Property name="MAX_FILE_SIZE">100MB</Property>
<!-- 日志文件保留数量 -->
<Property name="MAX_FILES">30</Property>
```

### 3.2 日志文件结构

```
logs/
├── feduwacomm.log          # 通用业务日志
├── security.log            # 安全审计日志
├── performance.log         # 性能监控日志
├── access.log             # 访问日志
├── error.log              # 错误日志
└── 归档文件/
    ├── feduwacomm-2024-01-01-1.log.gz
    ├── security-2024-01-01-1.log.gz
    └── ...
```

## 4. 日志记录规范

### 4.1 日志级别使用规范

- **ERROR**: 系统错误、业务异常、安全事件
- **WARN**: 警告信息、业务警告、安全警告
- **INFO**: 重要业务操作、系统状态变更
- **DEBUG**: 调试信息、详细执行过程
- **TRACE**: 最详细的执行跟踪信息

### 4.2 日志格式规范

```
时间戳 [线程名] 日志级别 日志器名称 - 日志消息
```

示例：
```
2024-01-01 10:00:00.123 [http-nio-8080-exec-1] INFO  UserServiceImpl - 用户登录成功: userId=123, username=testuser, ip=192.168.1.100
```

### 4.3 日志内容规范

每条日志应包含：
- 操作类型/事件类型
- 用户标识（userId, username）
- 客户端IP地址
- 操作结果
- 相关参数（敏感信息需要脱敏）
- 异常信息（如果有）

## 5. 代码中的日志使用

### 5.1 基本日志记录

```java
// 使用专门的日志器
private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
private static final Logger securityLog = LoggerFactory.getLogger("SECURITY_AUDIT");
private static final Logger performanceLog = LoggerFactory.getLogger("PERFORMANCE");

// 记录操作日志
log.info("用户注册成功: userId={}, username={}, email={}, role={}, ip={}", 
    user.getId(), user.getUsername(), user.getEmail(), user.getRole(), clientIp);

// 记录安全审计日志
securityLog.error("用户账户因登录失败过多被锁定: userId={}, username={}, ip={}, attempts={}, lockedUntil={}", 
    user.getId(), user.getUsername(), clientIp, user.getLoginAttempts(), user.getLockedUntil());

// 记录性能日志
performanceLog.info("用户登录完成: duration={}ms, username={}", 
    System.currentTimeMillis() - startTime, user.getUsername());
```

### 5.2 使用LogUtil工具类

```java
// 记录用户操作
LogUtil.logUserOperation("用户登录", userId, username, clientIp, "role=" + user.getRole());

// 记录安全事件
LogUtil.logSecurityEvent("账户锁定", userId, username, clientIp, "登录失败次数过多");

// 记录异常
LogUtil.logException("用户登录", userId, clientIp, e, "identifier=" + loginDTO.getLoginIdentifier());

// 记录执行时间
LogUtil.logExecutionTime("userLogin", startTime, "username=" + username);
```

## 6. 日志监控和分析

### 6.1 关键指标监控

- **登录成功率**: 通过分析登录成功/失败的日志
- **接口响应时间**: 通过性能日志分析接口性能
- **安全事件频率**: 监控异常登录、账户锁定等安全事件
- **用户活跃度**: 分析用户登录和操作频率

### 6.2 日志分析工具

推荐使用以下工具进行日志分析：
- **ELK Stack**: Elasticsearch + Logstash + Kibana
- **Grafana**: 日志可视化和监控
- **Splunk**: 企业级日志分析平台
- **自研工具**: 基于日志文件的正则表达式分析

### 6.3 告警配置

建议配置以下告警：
- 登录失败次数异常增加
- 账户锁定频率异常
- 接口响应时间超过阈值
- 系统错误率异常

## 7. 性能优化

### 7.1 异步日志记录

所有文件输出都使用异步Appender，避免日志I/O影响业务性能：

```xml
<Async name="AsyncFileAppender">
    <AppenderRef ref="FileAppender"/>
</Async>
```

### 7.2 日志级别控制

生产环境建议设置：
- 业务日志: INFO级别
- 安全日志: INFO级别
- 性能日志: INFO级别
- 访问日志: INFO级别
- 调试日志: WARN级别

### 7.3 日志文件轮转

- 按时间轮转: 每天生成新文件
- 按大小轮转: 单文件超过100MB时轮转
- 文件保留: 保留最近30个文件

## 8. 安全考虑

### 8.1 敏感信息处理

- 密码相关操作只记录操作类型，不记录具体密码
- 用户Token只记录前20个字符用于调试
- 个人隐私信息（如邮箱）在调试日志中需要脱敏

### 8.2 日志文件权限

- 日志文件只允许应用程序和系统管理员访问
- 定期备份和归档日志文件
- 敏感日志文件加密存储

## 9. 故障排查

### 9.1 常见问题

1. **日志文件过大**: 检查日志级别设置和轮转配置
2. **日志丢失**: 检查磁盘空间和文件权限
3. **性能影响**: 检查是否使用了异步日志记录
4. **日志格式错误**: 检查PatternLayout配置

### 9.2 调试方法

1. 启用DEBUG级别日志
2. 检查日志文件输出
3. 使用日志分析工具
4. 监控系统资源使用情况

## 10. 最佳实践

### 10.1 开发阶段

- 在开发环境中使用DEBUG级别
- 记录详细的执行流程
- 使用有意义的日志消息

### 10.2 测试阶段

- 验证日志记录的完整性
- 测试日志轮转功能
- 验证异步日志记录性能

### 10.3 生产阶段

- 使用INFO级别记录重要操作
- 定期检查日志文件大小
- 配置日志监控和告警
- 定期归档和清理日志文件

## 11. 总结

用户模块的日志系统提供了全面的日志记录功能，能够满足业务监控、安全审计、性能分析和故障排查的需求。通过合理的配置和使用，可以有效地提高系统的可观测性和可维护性。

建议开发团队：
1. 严格按照日志规范记录日志
2. 合理使用日志级别
3. 定期检查和优化日志配置
4. 建立日志监控和告警机制
5. 定期进行日志分析和系统优化 