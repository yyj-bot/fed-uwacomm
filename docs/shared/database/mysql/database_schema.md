# 水声联邦学习系统 数据库表结构设计

## 概述

本文档定义了水声联邦学习系统的完整数据库表结构，包括用户管理、虚拟机管理、联邦学习任务、训练数据、模型版本和系统日志等核心功能的数据存储设计。

## 数据库表结构

### 1. 用户表 (users)

存储系统用户的基本信息、身份和权限控制。

```sql
CREATE TABLE users (
    id VARCHAR(32) PRIMARY KEY COMMENT '用户唯一标识(32位UUID)',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱地址',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    role ENUM('ADMIN', 'RESEARCHER', 'OPERATOR', 'VIEWER') NOT NULL DEFAULT 'VIEWER' COMMENT '用户角色',
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED') NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
    last_login_time TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(45) COMMENT '最后登录IP',
    login_attempts INT DEFAULT 0 COMMENT '登录失败次数',
    locked_until TIMESTAMP NULL COMMENT '锁定截止时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(32) NULL COMMENT '创建者ID(32位UUID)',
    updated_by VARCHAR(32) NULL COMMENT '更新者ID(32位UUID)'
);
```

### 2. 用户权限表 (user_permissions)

存储用户的详细权限配置。

```sql
CREATE TABLE user_permissions (
    id VARCHAR(32) PRIMARY KEY COMMENT '权限唯一标识(32位UUID)',
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID(32位UUID)',
    resource_type ENUM('VM', 'TASK', 'DATA', 'MODEL', 'SYSTEM', 'USER') NOT NULL COMMENT '资源类型',
    resource_id VARCHAR(32) NULL COMMENT '资源ID(32位UUID，NULL表示所有资源)',
    permission ENUM('READ', 'WRITE', 'DELETE', 'EXECUTE', 'ADMIN') NOT NULL COMMENT '权限类型',
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by VARCHAR(32) NOT NULL COMMENT '授权者ID(32位UUID)',
    expires_at TIMESTAMP NULL COMMENT '权限过期时间'
);
```

### 3. 虚拟机表 (vm_instances)

存储虚拟机的基本信息和连接状态。

```sql
CREATE TABLE vm_instances (
    id VARCHAR(32) PRIMARY KEY COMMENT '虚拟机唯一标识(32位UUID)',
    name VARCHAR(100) NOT NULL COMMENT '虚拟机名称',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    port INT DEFAULT 22 COMMENT 'SSH端口',
    os_type VARCHAR(50) COMMENT '操作系统类型',
    cpu_cores INT DEFAULT 1 COMMENT 'CPU核心数',
    memory_mb INT DEFAULT 1024 COMMENT '内存大小(MB)',
    disk_gb INT DEFAULT 20 COMMENT '磁盘大小(GB)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_heartbeat TIMESTAMP NULL COMMENT '最后心跳时间',
    connection_status ENUM('CONNECTED', 'DISCONNECTED', 'CONNECTING') DEFAULT 'DISCONNECTED',
    ws_session_id VARCHAR(100) NULL COMMENT 'WebSocket会话ID'
);
```

**说明**: 虚拟机状态（如RUNNING、STOPPED等）不存储在数据库中，而是通过WebSocket连接实时查询虚拟机获取。API响应中的status字段通过实时查询获取，确保状态信息的实时性和准确性。查询接口支持按status过滤，过滤逻辑基于实时查询结果。

### 4. 联邦学习任务表 (federated_tasks)

存储联邦学习任务的基本信息和配置参数。

```sql
CREATE TABLE federated_tasks (
    id VARCHAR(32) PRIMARY KEY COMMENT '任务唯一标识(32位UUID)',
    name VARCHAR(100) NOT NULL COMMENT '任务名称',
    algorithm ENUM('FEDAVG', 'FEDPROX', 'FEDNOVA', 'SCAFFOLD') NOT NULL COMMENT '联邦学习算法',
    status ENUM('PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED', 'STOPPED') DEFAULT 'PENDING',
    total_rounds INT DEFAULT 100 COMMENT '总训练轮数',
    current_round INT DEFAULT 0 COMMENT '当前轮数',
    batch_size INT DEFAULT 32 COMMENT '批次大小',
    learning_rate DECIMAL(10,6) DEFAULT 0.001 COMMENT '学习率',
    mu DECIMAL(10,6) DEFAULT 0.001 COMMENT 'FedProx参数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    config JSON COMMENT '算法配置参数'
);
```

### 5. 训练数据表 (training_data)

存储训练数据文件的基本信息和元数据。

```sql
CREATE TABLE training_data (
    id VARCHAR(32) PRIMARY KEY COMMENT '数据唯一标识(32位UUID)',
    vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
    filename VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    data_type ENUM('ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'OTHER') NOT NULL COMMENT '数据类型',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('UPLOADING', 'PROCESSING', 'READY', 'ERROR') DEFAULT 'UPLOADING',
    metadata JSON COMMENT '数据元信息'
);
```

### 6. 模型版本表 (model_versions)

存储模型文件的版本信息和性能指标。

```sql
CREATE TABLE model_versions (
    id VARCHAR(32) PRIMARY KEY COMMENT '版本唯一标识(32位UUID)',
    task_id VARCHAR(32) NOT NULL COMMENT '关联任务ID(32位UUID)',
    vm_id VARCHAR(32) NULL COMMENT '虚拟机ID(32位UUID，本地模型)',
    round_number INT NOT NULL COMMENT '训练轮数',
    model_type ENUM('GLOBAL', 'LOCAL') NOT NULL COMMENT '模型类型',
    model_path VARCHAR(500) NOT NULL COMMENT '模型文件路径',
    model_size BIGINT COMMENT '模型大小(字节)',
    accuracy DECIMAL(5,4) COMMENT '准确率',
    loss DECIMAL(10,6) COMMENT '损失值',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parameters JSON COMMENT '模型参数'
);
```

### 7. SpringBoot系统日志表 (system_logs)

存储SpringBoot应用运行过程中的日志信息。

```sql
CREATE TABLE system_logs (
    id VARCHAR(32) PRIMARY KEY COMMENT '日志唯一标识(32位UUID)',
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志时间',
    level VARCHAR(10) NOT NULL COMMENT '日志级别',
    logger VARCHAR(100) NOT NULL COMMENT '日志记录器',
    message TEXT NOT NULL COMMENT '日志消息',
    thread VARCHAR(100) COMMENT '线程名',
    user_id VARCHAR(50) COMMENT '用户ID',
    username VARCHAR(100) COMMENT '用户名',
    request_uri VARCHAR(500) COMMENT '请求URI',
    client_ip VARCHAR(50) COMMENT '客户端IP',
    environment VARCHAR(20) COMMENT '环境',
    exception TEXT COMMENT '异常信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_timestamp (timestamp),
    INDEX idx_level (level),
    INDEX idx_user_id (user_id),
    INDEX idx_request_uri (request_uri)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'SpringBoot系统日志表';
```

### 8. 虚拟机运行日志表 (vm_runtime_logs)

存储虚拟机运行过程中的日志信息。

```sql
CREATE TABLE vm_runtime_logs (
    id VARCHAR(32) PRIMARY KEY COMMENT '日志唯一标识(32位UUID)',
    level ENUM('INFO', 'WARN', 'ERROR', 'DEBUG') NOT NULL,
    category VARCHAR(50) NOT NULL COMMENT '日志类别',
    vm_id VARCHAR(32) NULL COMMENT '虚拟机ID(32位UUID)',
    task_id VARCHAR(32) NULL COMMENT '任务ID(32位UUID)',
    message TEXT NOT NULL COMMENT '日志消息',
    details JSON COMMENT '详细信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

);
```

## 表关系说明

### 外键关系
以下外键约束在表创建后单独添加，以确保MySQL 8.0+兼容性：

#### 用户表自引用约束
- `users.created_by` → `users(id)` (ON DELETE SET NULL)
- `users.updated_by` → `users(id)` (ON DELETE SET NULL)

#### 用户权限表约束
- `user_permissions.user_id` → `users(id)` (ON DELETE CASCADE)
- `user_permissions.granted_by` → `users(id)` (ON DELETE RESTRICT)

#### 训练数据表约束
- `training_data.vm_id` → `vm_instances.id` (ON DELETE CASCADE)

#### 模型版本表约束
- `model_versions.task_id` → `federated_tasks.id` (ON DELETE CASCADE)
- `model_versions.vm_id` → `vm_instances.id` (ON DELETE SET NULL)

#### 虚拟机运行日志表约束
- `vm_runtime_logs.vm_id` → `vm_instances.id` (ON DELETE SET NULL)
- `vm_runtime_logs.task_id` → `federated_tasks.id` (ON DELETE SET NULL)

### 索引建议
```sql
-- 用户表索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- 用户权限表索引
CREATE INDEX idx_user_permissions_user_id ON user_permissions(user_id);
CREATE INDEX idx_user_permissions_resource_type ON user_permissions(resource_type);
CREATE INDEX idx_user_permissions_resource_id ON user_permissions(resource_id);
CREATE INDEX idx_user_permissions_permission ON user_permissions(permission);

-- 虚拟机表索引
CREATE INDEX idx_vm_instances_connection_status ON vm_instances(connection_status);
CREATE INDEX idx_vm_instances_last_heartbeat ON vm_instances(last_heartbeat);

-- 联邦学习任务表索引
CREATE INDEX idx_federated_tasks_status ON federated_tasks(status);
CREATE INDEX idx_federated_tasks_algorithm ON federated_tasks(algorithm);

-- 训练数据表索引
CREATE INDEX idx_training_data_vm_id ON training_data(vm_id);
CREATE INDEX idx_training_data_data_type ON training_data(data_type);
CREATE INDEX idx_training_data_status ON training_data(status);

-- 模型版本表索引
CREATE INDEX idx_model_versions_task_id ON model_versions(task_id);
CREATE INDEX idx_model_versions_vm_id ON model_versions(vm_id);
CREATE INDEX idx_model_versions_model_type ON model_versions(model_type);
CREATE INDEX idx_model_versions_round_number ON model_versions(round_number);

-- SpringBoot系统日志表索引
CREATE INDEX idx_system_logs_timestamp ON system_logs(timestamp);
CREATE INDEX idx_system_logs_level ON system_logs(level);
CREATE INDEX idx_system_logs_user_id ON system_logs(user_id);
CREATE INDEX idx_system_logs_request_uri ON system_logs(request_uri);

-- 虚拟机运行日志表索引
CREATE INDEX idx_vm_runtime_logs_level ON vm_runtime_logs(level);
CREATE INDEX idx_vm_runtime_logs_category ON vm_runtime_logs(category);
CREATE INDEX idx_vm_runtime_logs_vm_id ON vm_runtime_logs(vm_id);
CREATE INDEX idx_vm_runtime_logs_task_id ON vm_runtime_logs(task_id);
CREATE INDEX idx_vm_runtime_logs_created_at ON vm_runtime_logs(created_at);
```

## 数据字典

### 枚举值说明

#### 用户角色 (role)
- `ADMIN`: 系统管理员
- `RESEARCHER`: 研究人员
- `OPERATOR`: 操作员
- `VIEWER`: 查看者

#### 用户状态 (status)
- `ACTIVE`: 活跃
- `INACTIVE`: 非活跃
- `LOCKED`: 已锁定
- `DELETED`: 已删除

#### 资源类型 (resource_type)
- `VM`: 虚拟机
- `TASK`: 任务
- `DATA`: 数据
- `MODEL`: 模型
- `SYSTEM`: 系统
- `USER`: 用户

#### 权限类型 (permission)
- `READ`: 读取权限
- `WRITE`: 写入权限
- `DELETE`: 删除权限
- `EXECUTE`: 执行权限
- `ADMIN`: 管理权限

#### 虚拟机连接状态 (connection_status)
- `CONNECTED`: 已连接
- `DISCONNECTED`: 未连接
- `CONNECTING`: 连接中

#### 联邦学习算法 (algorithm)
- `FEDAVG`: 联邦平均算法
- `FEDPROX`: 联邦近端算法
- `FEDNOVA`: 联邦NOVA算法
- `SCAFFOLD`: SCAFFOLD算法

#### 任务状态 (status)
- `PENDING`: 等待中
- `RUNNING`: 运行中
- `PAUSED`: 已暂停
- `COMPLETED`: 已完成
- `FAILED`: 失败
- `STOPPED`: 已停止

#### 数据类型 (data_type)
- `ACOUSTIC`: 声学数据
- `ENVIRONMENT`: 环境数据
- `MODEL`: 模型数据
- `OTHER`: 其他数据

#### 数据状态 (status)
- `UPLOADING`: 上传中
- `PROCESSING`: 处理中
- `READY`: 就绪
- `ERROR`: 错误

#### 模型类型 (model_type)
- `GLOBAL`: 全局模型
- `LOCAL`: 本地模型

#### 日志级别 (level)
- `INFO`: 信息
- `WARN`: 警告
- `ERROR`: 错误
- `DEBUG`: 调试

## 数据库初始化脚本

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS feduwacomm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE feduwacomm;

-- 创建表结构
-- (上述所有CREATE TABLE语句)

-- 创建索引
-- (上述所有CREATE INDEX语句)

-- 插入初始数据（可选）
INSERT INTO system_logs (id, level, logger, message) 
VALUES ('log_init_001', 'INFO', 'com.feduwacomm', 'SpringBoot系统日志表初始化完成');

INSERT INTO vm_runtime_logs (id, level, category, message) 
VALUES ('vm_log_init_001', 'INFO', 'SYSTEM', '虚拟机运行日志表初始化完成');
```

## 注意事项

1. **字符集**: 建议使用 `utf8mb4` 字符集以支持完整的Unicode字符
2. **时区**: 建议使用 `UTC` 时区存储时间戳
3. **JSON字段**: 使用JSON类型存储灵活的配置和元数据信息
4. **外键约束**: 确保数据完整性，建议启用外键约束
5. **索引优化**: 根据查询模式优化索引设计
6. **备份策略**: 定期备份数据库，建议使用增量备份
7. **UUID生成**: 所有主键ID使用32位UUID格式，确保全局唯一性 