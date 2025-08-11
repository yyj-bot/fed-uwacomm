# 数据库重构说明

## 概述

本次数据库重构主要解决了日志表命名冲突和ID字段规范化的问题。

## 重构内容

### 1. 日志表分离

**问题**: 原来的`system_logs`表存在命名冲突，SpringBoot系统日志和虚拟机运行日志混用同一个表。

**解决方案**: 
- 保留`system_logs`表专门用于SpringBoot系统日志
- 新增`vm_runtime_logs`表专门用于虚拟机运行日志

### 2. ID字段规范化

**问题**: SpringBoot系统日志表使用`BIGINT AUTO_INCREMENT`作为主键，不符合项目的UUID规范。

**解决方案**: 将`system_logs`表的ID字段改为`VARCHAR(32)`，使用UUID格式。

## 表结构对比

### SpringBoot系统日志表 (system_logs)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) | 日志唯一标识(32位UUID) |
| timestamp | DATETIME | 日志时间 |
| level | VARCHAR(10) | 日志级别 |
| logger | VARCHAR(100) | 日志记录器 |
| message | TEXT | 日志消息 |
| thread | VARCHAR(100) | 线程名 |
| user_id | VARCHAR(50) | 用户ID |
| username | VARCHAR(100) | 用户名 |
| request_uri | VARCHAR(500) | 请求URI |
| client_ip | VARCHAR(50) | 客户端IP |
| environment | VARCHAR(20) | 环境 |
| exception | TEXT | 异常信息 |
| created_at | TIMESTAMP | 创建时间 |

### 虚拟机运行日志表 (vm_runtime_logs)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) | 日志唯一标识(32位UUID) |
| level | ENUM | 日志级别(INFO/WARN/ERROR/DEBUG) |
| category | VARCHAR(50) | 日志类别 |
| vm_id | VARCHAR(32) | 虚拟机ID |
| task_id | VARCHAR(32) | 任务ID |
| message | TEXT | 日志消息 |
| details | JSON | 详细信息 |
| created_at | TIMESTAMP | 创建时间 |

### 模型版本表 (model_versions)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) | 版本唯一标识(32位UUID) |
| task_id | VARCHAR(32) | 关联任务ID(32位UUID) |
| round_number | INT | 训练轮数 |
| accuracy | DECIMAL(5,4) | 准确率 |
| loss | DECIMAL(10,6) | 损失值 |
| created_at | TIMESTAMP | 创建时间 |
| parameters | JSON | 模型参数(JSON，记录所有模型相关信息) |

## 代码变更

### LogService更新

- 更新了`LogServiceImpl.java`中的SQL语句，添加了ID字段
- 添加了UUID生成逻辑
- 导入了`java.util.UUID`包

### 文档更新

- 更新了`database_schema.md`中的表结构说明
- 更新了`LOGGING_GUIDE.md`中的日志表结构说明
- 更新了API文档中的日志类型说明

## 文件清单

### 新增文件
- `docs/shared/database/vm_runtime_logs.sql` - 虚拟机运行日志表结构
- `docs/shared/database/init_database.sql` - 完整数据库初始化脚本
- `docs/shared/database/README.md` - 本说明文档

### 修改文件
- `docs/shared/database/system_logs.sql` - 更新ID字段类型
- `docs/shared/database/database_schema.md` - 更新表结构说明
- `backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/service/impl/LogServiceImpl.java` - 更新日志服务实现
- `docs/backend-springboot/LOGGING_GUIDE.md` - 更新日志指南
- `docs/shared/api/HTTP/system-log-api-reference.md` - 更新API文档

## 部署说明

### 数据库迁移

如果已有数据库，需要执行以下迁移脚本：

```sql
-- 1. 备份现有数据
CREATE TABLE system_logs_backup AS SELECT * FROM system_logs;

-- 2. 删除旧表
DROP TABLE system_logs;

-- 3. 创建新表结构
-- 执行 system_logs.sql 中的CREATE TABLE语句

-- 4. 创建虚拟机运行日志表
-- 执行 vm_runtime_logs.sql 中的CREATE TABLE语句

-- 5. 恢复数据（如果需要）
-- INSERT INTO system_logs SELECT * FROM system_logs_backup;
```

### 新部署

直接执行`init_database.sql`脚本即可完成数据库初始化。

## 注意事项

1. **数据迁移**: 如果生产环境已有数据，请先备份再执行迁移
2. **ID格式**: 所有日志ID现在都使用`log_`前缀的UUID格式
3. **日志分类**: 明确区分SpringBoot系统日志和虚拟机运行日志
4. **索引优化**: 两个日志表都添加了相应的索引以提高查询性能

## 验证

部署后可以通过以下方式验证：

1. 检查数据库表结构是否正确创建
2. 验证LogService是否能正常记录日志
3. 确认日志ID格式符合UUID规范
4. 测试日志查询功能是否正常 