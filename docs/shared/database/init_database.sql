-- =====================================================
-- 水声联邦学习系统 数据库初始化脚本
-- =====================================================
-- 作者: FedUWAComm Team
-- 版本: 1.0.0
-- 描述: 创建完整的数据库结构，包括所有表、索引、外键约束
-- 使用说明: 直接执行此脚本即可完成数据库初始化
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS feduwacomm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE feduwacomm;

-- =====================================================
-- 表结构创建
-- =====================================================

-- 1. 用户表 (users)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(32) PRIMARY KEY COMMENT '用户唯一标识(32位UUID)',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱地址',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    role ENUM(
        'ADMIN',
        'RESEARCHER',
        'OPERATOR',
        'VIEWER'
    ) NOT NULL DEFAULT 'VIEWER' COMMENT '用户角色',
    status ENUM(
        'ACTIVE',
        'INACTIVE',
        'LOCKED',
        'DELETED'
    ) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
    last_login_time TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(45) COMMENT '最后登录IP',
    login_attempts INT DEFAULT 0 COMMENT '登录失败次数',
    locked_until TIMESTAMP NULL COMMENT '锁定截止时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(32) NULL COMMENT '创建者ID(32位UUID)',
    updated_by VARCHAR(32) NULL COMMENT '更新者ID(32位UUID)'
);

-- 添加用户表自引用外键约束（在表创建后单独添加）
ALTER TABLE users
ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE users
ADD CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL;

-- 2. 用户权限表 (user_permissions)
CREATE TABLE IF NOT EXISTS user_permissions (
    id VARCHAR(32) PRIMARY KEY COMMENT '权限唯一标识(32位UUID)',
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID(32位UUID)',
    resource_type ENUM(
        'VM',
        'TASK',
        'DATA',
        'MODEL',
        'SYSTEM',
        'USER'
    ) NOT NULL COMMENT '资源类型',
    resource_id VARCHAR(32) NULL COMMENT '资源ID(32位UUID，NULL表示所有资源)',
    permission ENUM(
        'READ',
        'WRITE',
        'DELETE',
        'EXECUTE',
        'ADMIN'
    ) NOT NULL COMMENT '权限类型',
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by VARCHAR(32) NOT NULL COMMENT '授权者ID(32位UUID)',
    expires_at TIMESTAMP NULL COMMENT '权限过期时间'
);

-- 添加用户权限表外键约束（在表创建后单独添加）
ALTER TABLE user_permissions
ADD CONSTRAINT fk_user_permissions_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_permissions
ADD CONSTRAINT fk_user_permissions_granted_by FOREIGN KEY (granted_by) REFERENCES users (id) ON DELETE RESTRICT;

-- 3. 虚拟机表 (vm_instances)
CREATE TABLE IF NOT EXISTS vm_instances (
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
    connection_status ENUM(
        'CONNECTED',
        'DISCONNECTED',
        'CONNECTING'
    ) DEFAULT 'DISCONNECTED',
    ws_session_id VARCHAR(100) NULL COMMENT 'WebSocket会话ID'
);

-- 4. 联邦学习任务表 (federated_tasks)
CREATE TABLE IF NOT EXISTS federated_tasks (
    id VARCHAR(32) PRIMARY KEY COMMENT '任务唯一标识(32位UUID)',
    name VARCHAR(100) NOT NULL COMMENT '任务名称',
    algorithm ENUM(
        'FEDAVG',
        'FEDPROX',
        'FEDNOVA',
        'SCAFFOLD'
    ) NOT NULL COMMENT '联邦学习算法',
    status ENUM(
        'PENDING',
        'RUNNING',
        'PAUSED',
        'COMPLETED',
        'FAILED',
        'STOPPED'
    ) DEFAULT 'PENDING',
    total_rounds INT DEFAULT 100 COMMENT '总训练轮数',
    current_round INT DEFAULT 0 COMMENT '当前轮数',
    batch_size INT DEFAULT 32 COMMENT '批次大小',
    learning_rate DECIMAL(10, 6) DEFAULT 0.001 COMMENT '学习率',
    mu DECIMAL(10, 6) DEFAULT 0.001 COMMENT 'FedProx参数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    config JSON COMMENT '算法配置参数'
);

-- 5. 训练数据表 (training_data)
CREATE TABLE IF NOT EXISTS training_data (
    id VARCHAR(32) PRIMARY KEY COMMENT '数据唯一标识(32位UUID)',
    vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
    filename VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    data_type ENUM(
        'ACOUSTIC',
        'ENVIRONMENT',
        'MODEL',
        'OTHER'
    ) NOT NULL COMMENT '数据类型',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM(
        'UPLOADING',
        'PROCESSING',
        'READY',
        'ERROR'
    ) DEFAULT 'UPLOADING',
    metadata JSON COMMENT '数据元信息'
);

-- 添加训练数据表外键约束（在表创建后单独添加）
ALTER TABLE training_data
ADD CONSTRAINT fk_training_data_vm_id FOREIGN KEY (vm_id) REFERENCES vm_instances (id) ON DELETE CASCADE;

-- 6. 模型版本表 (model_versions)
CREATE TABLE IF NOT EXISTS model_versions (
    id VARCHAR(32) PRIMARY KEY COMMENT '版本唯一标识(32位UUID)',
    task_id VARCHAR(32) NOT NULL COMMENT '关联任务ID(32位UUID)',
    vm_id VARCHAR(32) NULL COMMENT '虚拟机ID(32位UUID，本地模型)',
    round_number INT NOT NULL COMMENT '训练轮数',
    model_type ENUM('GLOBAL', 'LOCAL') NOT NULL COMMENT '模型类型',
    model_path VARCHAR(500) NOT NULL COMMENT '模型文件路径',
    model_size BIGINT COMMENT '模型大小(字节)',
    accuracy DECIMAL(5, 4) COMMENT '准确率',
    loss DECIMAL(10, 6) COMMENT '损失值',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parameters JSON COMMENT '模型参数'
);

-- 添加外键约束（在表创建后单独添加，避免NULL约束问题）
ALTER TABLE model_versions
ADD CONSTRAINT fk_model_versions_task_id FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE;

ALTER TABLE model_versions
ADD CONSTRAINT fk_model_versions_vm_id FOREIGN KEY (vm_id) REFERENCES vm_instances (id) ON DELETE SET NULL;

-- 7. SpringBoot系统日志表 (system_logs)
CREATE TABLE IF NOT EXISTS system_logs (
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

-- 8. 虚拟机运行日志表 (vm_runtime_logs)
CREATE TABLE IF NOT EXISTS vm_runtime_logs (
    id VARCHAR(32) PRIMARY KEY COMMENT '日志唯一标识(32位UUID)',
    level ENUM(
        'INFO',
        'WARN',
        'ERROR',
        'DEBUG'
    ) NOT NULL,
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

-- 添加外键约束（在表创建后单独添加，避免NULL约束问题）
ALTER TABLE vm_runtime_logs
ADD CONSTRAINT fk_vm_runtime_logs_vm_id FOREIGN KEY (vm_id) REFERENCES vm_instances (id) ON DELETE SET NULL;

ALTER TABLE vm_runtime_logs
ADD CONSTRAINT fk_vm_runtime_logs_task_id FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE SET NULL;

-- =====================================================
-- 索引创建
-- =====================================================
-- 注意: system_logs 和 vm_runtime_logs 表的索引已在表定义中包含
-- 用户表索引
CREATE INDEX idx_users_username ON users (username);

CREATE INDEX idx_users_email ON users (email);

CREATE INDEX idx_users_role ON users (role);

CREATE INDEX idx_users_status ON users (status);

CREATE INDEX idx_users_created_at ON users (created_at);

-- 用户权限表索引
CREATE INDEX idx_user_permissions_user_id ON user_permissions (user_id);

CREATE INDEX idx_user_permissions_resource_type ON user_permissions (resource_type);

CREATE INDEX idx_user_permissions_resource_id ON user_permissions (resource_id);

CREATE INDEX idx_user_permissions_permission ON user_permissions (permission);

-- 虚拟机表索引
CREATE INDEX idx_vm_instances_connection_status ON vm_instances (connection_status);

CREATE INDEX idx_vm_instances_last_heartbeat ON vm_instances (last_heartbeat);

-- 联邦学习任务表索引
CREATE INDEX idx_federated_tasks_status ON federated_tasks (status);

CREATE INDEX idx_federated_tasks_algorithm ON federated_tasks (algorithm);

-- 训练数据表索引
CREATE INDEX idx_training_data_vm_id ON training_data (vm_id);

CREATE INDEX idx_training_data_data_type ON training_data (data_type);

CREATE INDEX idx_training_data_status ON training_data (status);

-- 模型版本表索引
CREATE INDEX idx_model_versions_task_id ON model_versions (task_id);

CREATE INDEX idx_model_versions_vm_id ON model_versions (vm_id);

CREATE INDEX idx_model_versions_model_type ON model_versions (model_type);

CREATE INDEX idx_model_versions_round_number ON model_versions (round_number);

-- SpringBoot系统日志表索引（已在表定义中包含）
-- CREATE INDEX idx_system_logs_timestamp ON system_logs (timestamp);
-- CREATE INDEX idx_system_logs_level ON system_logs (level);
-- CREATE INDEX idx_system_logs_user_id ON system_logs (user_id);
-- CREATE INDEX idx_system_logs_request_uri ON system_logs (request_uri);

-- 虚拟机运行日志表索引（已在表定义中包含）
-- CREATE INDEX idx_vm_runtime_logs_level ON vm_runtime_logs (level);
-- CREATE INDEX idx_vm_runtime_logs_category ON vm_runtime_logs (category);
-- CREATE INDEX idx_vm_runtime_logs_vm_id ON vm_runtime_logs (vm_id);
-- CREATE INDEX idx_vm_runtime_logs_task_id ON vm_runtime_logs (task_id);
-- CREATE INDEX idx_vm_runtime_logs_created_at ON vm_runtime_logs (created_at);

-- =====================================================
-- 初始数据插入
-- =====================================================

-- 插入系统日志初始化记录
INSERT INTO
    system_logs (
        id,
        level,
        logger,
        message,
        thread,
        environment
    )
VALUES (
        '00000000000000000000000000000001',
        'INFO',
        'com.feduwacomm',
        'SpringBoot系统日志表初始化完成',
        'Database-Init',
        'PRODUCTION'
    );

-- 插入虚拟机运行日志初始化记录
INSERT INTO
    vm_runtime_logs (id, level, category, message)
VALUES (
        '00000000000000000000000000000002',
        'INFO',
        'SYSTEM',
        '虚拟机运行日志表初始化完成'
    );

-- =====================================================
-- 可选配置
-- =====================================================

-- 注意：上面的password_hash需替换为实际由BCrypt生成的"password"的哈希

-- =====================================================
-- 初始化完成
-- =====================================================
-- 数据库初始化脚本执行完成
-- 共创建了 8 个表:
-- 1. users - 用户表
-- 2. user_permissions - 用户权限表
-- 3. vm_instances - 虚拟机表
-- 4. federated_tasks - 联邦学习任务表
-- 5. training_data - 训练数据表
-- 6. model_versions - 模型版本表
-- 7. system_logs - SpringBoot系统日志表
-- 8. vm_runtime_logs - 虚拟机运行日志表
-- =====================================================