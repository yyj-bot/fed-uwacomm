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

### 5. 训练数据集元信息表 (training_dataset)

存储训练数据集的元信息，如名称、描述、类型、状态等。

```sql
CREATE TABLE training_dataset (
    id VARCHAR(32) PRIMARY KEY COMMENT '数据集唯一标识(32位UUID)',
    vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
    name VARCHAR(255) NOT NULL COMMENT '数据集名称',
    description TEXT COMMENT '数据集描述',
    data_type ENUM('ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'OTHER') NOT NULL COMMENT '数据类型',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('UPLOADING', 'PROCESSING', 'READY', 'ERROR') DEFAULT 'UPLOADING',
    metadata JSON COMMENT '数据集元信息'
);
```

### 6. 训练数据明细表 (training_dataset_row)

存储每个数据集的明细数据，每行为一条JSON格式记录。

```sql
CREATE TABLE training_dataset_row (
    id VARCHAR(32) PRIMARY KEY COMMENT '数据行唯一标识(32位UUID)',
    dataset_id VARCHAR(32) NOT NULL COMMENT '所属数据集ID',
    row_data JSON NOT NULL COMMENT '原始CSV行数据（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '插入时间',
    FOREIGN KEY (dataset_id) REFERENCES training_dataset(id) ON DELETE CASCADE,
    INDEX idx_dataset_id (dataset_id)
);
```

### 7. 模型版本表 (model_versions)

存储模型文件的版本信息和性能指标。

```sql
CREATE TABLE model_versions (
    id VARCHAR(32) PRIMARY KEY COMMENT '版本唯一标识(32位UUID)',
    task_id VARCHAR(32) NOT NULL COMMENT '关联任务ID(32位UUID)',
    round_number INT NOT NULL COMMENT '训练轮数',
    accuracy DECIMAL(5,4) COMMENT '准确率',
    loss DECIMAL(10,6) COMMENT '损失值',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parameters JSON COMMENT '模型参数(JSON记录所有模型相关信息)'
);
```



### 8. SpringBoot系统日志表 (system_logs)

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

### 9. 虚拟机运行日志表 (vm_runtime_logs)

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
    INDEX idx_level (level),
    INDEX idx_category (category),
    INDEX idx_vm_id (vm_id),
    INDEX idx_task_id (task_id),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '虚拟机运行日志表';
```

### 10. 虚拟机轮次模型结果表 (vm_round_models)

存储虚拟机在每轮训练中的模型结果和性能指标。

```sql
CREATE TABLE vm_round_models (
    id VARCHAR(32) PRIMARY KEY COMMENT '唯一标识(32位UUID)',
    task_id VARCHAR(32) NOT NULL COMMENT '任务ID(32位UUID)',
    vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
    round_number INT NOT NULL COMMENT '训练轮数',
    accuracy DECIMAL(5,4) NULL COMMENT '准确率',
    loss DECIMAL(10,6) NULL COMMENT '损失值',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parameters JSON COMMENT '本地模型参数/元信息(JSON，仅记录，不存文件路径)'
);
```

外键约束：
- `vm_round_models.task_id` → `federated_tasks(id)` (ON DELETE CASCADE)
- `vm_round_models.vm_id` → `vm_instances(id)` (ON DELETE CASCADE)

索引建议：
```sql
CREATE UNIQUE INDEX uq_vm_round_models_task_vm_round ON vm_round_models(task_id, vm_id, round_number);
CREATE INDEX idx_vm_round_models_task_id ON vm_round_models(task_id);
CREATE INDEX idx_vm_round_models_vm_id ON vm_round_models(vm_id);
CREATE INDEX idx_vm_round_models_round_number ON vm_round_models(round_number);
```

### 11. 虚拟机刷新凭证表 (vm_secrets)

存储虚拟机的长期刷新凭证，用于刷新 accessToken。

```sql
CREATE TABLE vm_secrets (
    id VARCHAR(32) PRIMARY KEY COMMENT '凭证唯一标识(32位UUID)',
    vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
    secret_hash VARCHAR(128) NOT NULL COMMENT 'secretId 哈希(如SHA-256)',
    salt VARCHAR(32) NULL COMMENT '哈希盐值',
    status ENUM('ACTIVE','REVOKED','EXPIRED') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    last_used_at TIMESTAMP NULL COMMENT '最后使用时间',
    rotated_at TIMESTAMP NULL COMMENT '最近旋转时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_vm_secrets_active (vm_id, status),
    INDEX idx_vm_secrets_vm_id (vm_id),
    INDEX idx_vm_secrets_status (status)
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

#### 训练数据集元信息表外键约束
- `training_dataset.vm_id` → `vm_instances.id` (ON DELETE CASCADE)

#### 训练数据明细表外键约束
- `training_dataset_row.dataset_id` → `training_dataset.id` (ON DELETE CASCADE)

#### 模型版本表约束
- `model_versions.task_id` → `federated_tasks.id` (ON DELETE CASCADE)

#### 虚拟机运行日志表约束
- `vm_runtime_logs.vm_id` → `vm_instances.id` (ON DELETE SET NULL)
- `vm_runtime_logs.task_id` → `federated_tasks.id` (ON DELETE SET NULL)

#### 虚拟机轮次模型结果表约束
- `vm_round_models.task_id` → `federated_tasks.id` (ON DELETE CASCADE)
- `vm_round_models.vm_id` → `vm_instances.id` (ON DELETE CASCADE)

#### 虚拟机刷新凭证表约束
- `vm_secrets.vm_id` → `vm_instances.id` (ON DELETE CASCADE)

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

-- 训练数据集元信息表索引
CREATE INDEX idx_training_dataset_vm_id ON training_dataset(vm_id);
CREATE INDEX idx_training_dataset_data_type ON training_dataset(data_type);
CREATE INDEX idx_training_dataset_status ON training_dataset(status);

-- 训练数据明细表索引
CREATE INDEX idx_training_dataset_row_dataset_id ON training_dataset_row(dataset_id);

-- 模型版本表索引
CREATE INDEX idx_model_versions_task_id ON model_versions(task_id);
CREATE INDEX idx_model_versions_round_number ON model_versions(round_number);

-- 虚拟机轮次模型结果表索引
CREATE UNIQUE INDEX uq_vm_round_models_task_vm_round ON vm_round_models(task_id, vm_id, round_number);
CREATE INDEX idx_vm_round_models_task_id ON vm_round_models(task_id);
CREATE INDEX idx_vm_round_models_vm_id ON vm_round_models(vm_id);
CREATE INDEX idx_vm_round_models_round_number ON vm_round_models(round_number);

-- 虚拟机刷新凭证表索引
CREATE UNIQUE INDEX uq_vm_secrets_active ON vm_secrets(vm_id, status);
CREATE INDEX idx_vm_secrets_vm_id ON vm_secrets(vm_id);
CREATE INDEX idx_vm_secrets_status ON vm_secrets(status);

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

## 表结构总结

本系统共包含 **11个核心数据表**：

1. **users** - 用户表：存储系统用户基本信息和身份控制
2. **user_permissions** - 用户权限表：存储用户详细权限配置
3. **vm_instances** - 虚拟机表：存储虚拟机基本信息和连接状态
4. **federated_tasks** - 联邦学习任务表：存储任务配置和状态
5. **training_dataset** - 训练数据集元信息表：存储数据集元信息
6. **training_dataset_row** - 训练数据明细表：存储具体训练数据行
7. **model_versions** - 模型版本表：存储模型版本信息和性能指标
8. **system_logs** - SpringBoot系统日志表：存储应用运行日志
9. **vm_runtime_logs** - 虚拟机运行日志表：存储虚拟机运行日志
10. **vm_round_models** - 虚拟机轮次模型结果表：存储每轮训练结果
11. **vm_secrets** - 虚拟机刷新凭证表：存储VM的长期认证凭证

**外键关系**: 共12个外键约束，确保数据完整性和关联性

## 注意事项

1. **字符集**: 建议使用 `utf8mb4` 字符集以支持完整的Unicode字符
2. **时区**: 建议使用 `UTC` 时区存储时间戳
3. **JSON字段**: 使用JSON类型存储灵活的配置和元数据信息
4. **外键约束**: 确保数据完整性，建议启用外键约束
5. **索引优化**: 根据查询模式优化索引设计
6. **备份策略**: 定期备份数据库，建议使用增量备份
7. **UUID生成**: 所有主键ID使用32位UUID格式，确保全局唯一性 