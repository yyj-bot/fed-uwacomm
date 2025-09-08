-- 创建测试数据库和表结构
-- 基于原始数据库架构文档，适配H2内存数据库测试环境
-- H2数据库在MySQL模式下运行，支持大部分MySQL语法

    
-- H2数据库不需要创建数据库和USE语句
-- 数据库已通过JDBC URL自动创建

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(32) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'RESEARCHER', 'OPERATOR', 'VIEWER') NOT NULL DEFAULT 'VIEWER',
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    last_login_time TIMESTAMP NULL,
    last_login_ip VARCHAR(45),
    login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(32) NULL,
    updated_by VARCHAR(32) NULL
);

-- 用户权限表
CREATE TABLE IF NOT EXISTS user_permissions (
    id VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL,
    resource_type ENUM('VM', 'TASK', 'DATA', 'MODEL', 'SYSTEM', 'USER') NOT NULL,
    resource_id VARCHAR(32) NULL,
    permission ENUM('read', 'WRITE', 'DELETE', 'EXECUTE', 'ADMIN') NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP NULL
);

-- 虚拟机表
CREATE TABLE IF NOT EXISTS vm_instances (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    port INT DEFAULT 22,
    os_type VARCHAR(50),
    cpu_cores INT DEFAULT 1,
    memory_mb INT DEFAULT 1024,
    disk_gb INT DEFAULT 20,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_heartbeat TIMESTAMP NULL,
    connection_status ENUM('CONNECTED', 'DISCONNECTED', 'CONNECTING') DEFAULT 'DISCONNECTED',
    ws_session_id VARCHAR(100) NULL
);

-- 联邦学习任务表
CREATE TABLE IF NOT EXISTS federated_tasks (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    algorithm ENUM('FEDAVG', 'FEDPROX', 'FEDNOVA', 'SCAFFOLD') NOT NULL,
    status ENUM('PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED', 'STOPPED') DEFAULT 'PENDING',
    total_rounds INT DEFAULT 100,
    current_round INT DEFAULT 0,
    batch_size INT DEFAULT 32,
    learning_rate DECIMAL(10,6) DEFAULT 0.001,
    mu DECIMAL(10,6) DEFAULT 0.001,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    config JSON
);

-- 训练数据集元信息表
CREATE TABLE IF NOT EXISTS training_dataset (
    id VARCHAR(32) PRIMARY KEY,
    vm_id VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    data_type ENUM('ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'OTHER') NOT NULL,
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('UPLOADING', 'PROCESSING', 'READY', 'ERROR') DEFAULT 'UPLOADING',
    metadata JSON
);

-- 训练数据明细表
CREATE TABLE IF NOT EXISTS training_dataset_row (
    id VARCHAR(32) PRIMARY KEY,
    dataset_id VARCHAR(32) NOT NULL,
    row_data JSON NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 模型版本表
CREATE TABLE IF NOT EXISTS model_versions (
    id VARCHAR(32) PRIMARY KEY,
    task_id VARCHAR(32) NOT NULL,
    round_number INT NOT NULL,
    accuracy DECIMAL(5,4),
    loss DECIMAL(10,6),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parameters JSON
);

-- SpringBoot系统日志表
CREATE TABLE IF NOT EXISTS system_logs (
    id VARCHAR(32) PRIMARY KEY,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    level VARCHAR(10) NOT NULL,
    logger VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    thread VARCHAR(100),
    user_id VARCHAR(50),
    username VARCHAR(100),
    request_uri VARCHAR(500),
    client_ip VARCHAR(50),
    environment VARCHAR(20),
    exception TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 虚拟机运行日志表
CREATE TABLE IF NOT EXISTS vm_runtime_logs (
    id VARCHAR(32) PRIMARY KEY,
    level ENUM('INFO', 'WARN', 'ERROR', 'DEBUG') NOT NULL,
    category VARCHAR(50) NOT NULL,
    vm_id VARCHAR(32) NULL,
    task_id VARCHAR(32) NULL,
    message TEXT NOT NULL,
    details JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 虚拟机轮次模型结果表
CREATE TABLE IF NOT EXISTS vm_round_models (
    id VARCHAR(32) PRIMARY KEY,
    task_id VARCHAR(32) NOT NULL,
    vm_id VARCHAR(32) NOT NULL,
    round_number INT NOT NULL,
    accuracy DECIMAL(5,4) NULL,
    loss DECIMAL(10,6) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parameters JSON
);

-- 虚拟机刷新凭证表
CREATE TABLE IF NOT EXISTS vm_secrets (
    id VARCHAR(32) PRIMARY KEY,
    vm_id VARCHAR(32) NOT NULL,
    secret_hash VARCHAR(128) NOT NULL,
    salt VARCHAR(32) NULL,
    status ENUM('ACTIVE','REVOKED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    rotated_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

CREATE INDEX IF NOT EXISTS idx_user_permissions_user_id ON user_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_permissions_resource_type ON user_permissions(resource_type);

CREATE INDEX IF NOT EXISTS idx_vm_instances_connection_status ON vm_instances(connection_status);

CREATE INDEX IF NOT EXISTS idx_federated_tasks_status ON federated_tasks(status);

CREATE INDEX IF NOT EXISTS idx_training_dataset_vm_id ON training_dataset(vm_id);
CREATE INDEX IF NOT EXISTS idx_training_dataset_row_dataset_id ON training_dataset_row(dataset_id);

CREATE INDEX IF NOT EXISTS idx_model_versions_task_id ON model_versions(task_id);

CREATE INDEX IF NOT EXISTS idx_system_logs_timestamp ON system_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_system_logs_level ON system_logs(level);

CREATE INDEX IF NOT EXISTS idx_vm_runtime_logs_vm_id ON vm_runtime_logs(vm_id);
CREATE INDEX IF NOT EXISTS idx_vm_runtime_logs_task_id ON vm_runtime_logs(task_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vm_round_models_task_vm_round ON vm_round_models(task_id, vm_id, round_number);
CREATE INDEX IF NOT EXISTS idx_vm_round_models_task_id ON vm_round_models(task_id);
CREATE INDEX IF NOT EXISTS idx_vm_round_models_vm_id ON vm_round_models(vm_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vm_secrets_active ON vm_secrets(vm_id, status);
CREATE INDEX IF NOT EXISTS idx_vm_secrets_vm_id ON vm_secrets(vm_id);