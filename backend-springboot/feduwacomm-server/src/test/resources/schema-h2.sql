-- =====================================================
-- 水声联邦学习系统 H2数据库初始化脚本
-- =====================================================
-- 作者: FedUWAComm Team
-- 版本: 1.0.0
-- 描述: 创建完整的H2数据库结构，用于集成测试
-- 使用说明: 此脚本专为H2数据库设计，语法兼容H2
-- =====================================================

-- 创建核心表结构（只保留集成测试必需的表）

-- 1. 用户表 (users)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(32) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_time TIMESTAMP NULL,
    last_login_ip VARCHAR(45),
    login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(32) NULL,
    updated_by VARCHAR(32) NULL
);


-- 3. 虚拟机表 (vm_instances)
CREATE TABLE IF NOT EXISTS vm_instances (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    port INT NOT NULL DEFAULT 22,
    os_type VARCHAR(50) NOT NULL,
    cpu_cores INT NOT NULL,
    memory_mb INT NOT NULL,
    disk_gb INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    connection_status VARCHAR(20) NOT NULL DEFAULT 'DISCONNECTED',
    ws_session_id VARCHAR(32) NULL,
    last_heartbeat TIMESTAMP NULL,
    secret_id VARCHAR(128) NULL,
    secret_expire_time TIMESTAMP NULL,
    system_info CLOB NULL,
    capabilities CLOB NULL,
    network_config CLOB NULL,
    metadata CLOB NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(32) NULL,
    updated_by VARCHAR(32) NULL
);

-- 4. 联邦学习任务表 (federated_tasks)
CREATE TABLE IF NOT EXISTS federated_tasks (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    algorithm VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    total_rounds INT DEFAULT 100,
    current_round INT DEFAULT 0,
    batch_size INT DEFAULT 32,
    learning_rate DECIMAL(10, 6) DEFAULT 0.001,
    mu DECIMAL(10, 6) DEFAULT 0.001,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    config CLOB
);

-- 5. 训练数据集元信息表 (training_dataset)
CREATE TABLE IF NOT EXISTS training_dataset (
    id VARCHAR(32) PRIMARY KEY,
    vm_id VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    data_type VARCHAR(20) NOT NULL,
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'UPLOADING',
    metadata CLOB
);

-- 6. 训练数据明细表 (training_dataset_row)
CREATE TABLE IF NOT EXISTS training_dataset_row (
    id VARCHAR(32) PRIMARY KEY,
    dataset_id VARCHAR(32) NOT NULL,
    row_data CLOB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. 模型版本表 (model_versions)
CREATE TABLE IF NOT EXISTS model_versions (
    id VARCHAR(32) PRIMARY KEY,
    task_id VARCHAR(32) NOT NULL,
    round_number INT NOT NULL,
    accuracy DECIMAL(5, 4),
    loss DECIMAL(10, 6),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parameters CLOB
);

-- 8. SpringBoot系统日志表 (system_logs)
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
    vm_id VARCHAR(32) NULL,
    task_id VARCHAR(32) NULL,
    details CLOB NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. 虚拟机运行日志表 (vm_runtime_logs)
CREATE TABLE IF NOT EXISTS vm_runtime_logs (
    id VARCHAR(32) PRIMARY KEY,
    level VARCHAR(10) NOT NULL,
    category VARCHAR(50) NOT NULL,
    vm_id VARCHAR(32) NULL,
    task_id VARCHAR(32) NULL,
    message TEXT NOT NULL,
    details CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 10. 虚拟机轮次模型结果表 (vm_round_models)
CREATE TABLE IF NOT EXISTS vm_round_models (
    id VARCHAR(32) PRIMARY KEY,
    task_id VARCHAR(32) NOT NULL,
    vm_id VARCHAR(32) NOT NULL,
    round_number INT NOT NULL,
    accuracy DECIMAL(5, 4) NULL,
    loss DECIMAL(10, 6) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parameters CLOB
);

-- 11. 虚拟机刷新凭证表 (vm_secrets)
CREATE TABLE IF NOT EXISTS vm_secrets (
    id VARCHAR(32) PRIMARY KEY,
    vm_id VARCHAR(32) NOT NULL,
    secret_hash VARCHAR(128) NOT NULL,
    salt VARCHAR(32) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    rotated_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 12. 全局模型表 (global_models)
CREATE TABLE IF NOT EXISTS global_models (
    id VARCHAR(32) PRIMARY KEY,
    task_id VARCHAR(32) NOT NULL,
    round_number INT NOT NULL,
    aggregation_method VARCHAR(50) NOT NULL,
    global_parameters CLOB,
    global_loss DECIMAL(10, 8),
    global_accuracy DECIMAL(10, 8),
    participant_count INT NOT NULL,
    aggregation_duration BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata CLOB
);

-- =====================================================
-- 索引创建（简化版本，只创建必要的索引）
-- =====================================================

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);


-- 虚拟机表索引
CREATE INDEX IF NOT EXISTS idx_vm_instances_status ON vm_instances (status);
CREATE INDEX IF NOT EXISTS idx_vm_instances_connection_status ON vm_instances (connection_status);
CREATE INDEX IF NOT EXISTS idx_vm_instances_ip_address ON vm_instances (ip_address);

-- 联邦学习任务表索引
CREATE INDEX IF NOT EXISTS idx_federated_tasks_status ON federated_tasks (status);
CREATE INDEX IF NOT EXISTS idx_federated_tasks_algorithm ON federated_tasks (algorithm);

-- 训练数据集表索引
CREATE INDEX IF NOT EXISTS idx_training_dataset_vm_id ON training_dataset (vm_id);
CREATE INDEX IF NOT EXISTS idx_training_dataset_status ON training_dataset (status);

-- 训练数据明细表索引
CREATE INDEX IF NOT EXISTS idx_training_dataset_row_dataset_id ON training_dataset_row (dataset_id);

-- 模型版本表索引
CREATE INDEX IF NOT EXISTS idx_model_versions_task_id ON model_versions (task_id);
CREATE INDEX IF NOT EXISTS idx_model_versions_round_number ON model_versions (round_number);

-- 虚拟机轮次模型结果表索引
CREATE INDEX IF NOT EXISTS idx_vm_round_models_task_id ON vm_round_models (task_id);
CREATE INDEX IF NOT EXISTS idx_vm_round_models_vm_id ON vm_round_models (vm_id);

-- 系统日志表索引
CREATE INDEX IF NOT EXISTS idx_system_logs_timestamp ON system_logs (timestamp);
CREATE INDEX IF NOT EXISTS idx_system_logs_level ON system_logs (level);
CREATE INDEX IF NOT EXISTS idx_system_logs_user_id ON system_logs (user_id);

-- 虚拟机运行日志表索引
CREATE INDEX IF NOT EXISTS idx_vm_runtime_logs_level ON vm_runtime_logs (level);
CREATE INDEX IF NOT EXISTS idx_vm_runtime_logs_vm_id ON vm_runtime_logs (vm_id);
CREATE INDEX IF NOT EXISTS idx_vm_runtime_logs_task_id ON vm_runtime_logs (task_id);

-- =====================================================
-- H2数据库初始化完成
-- =====================================================
-- 共创建了 12 个表（简化版本，用于集成测试）
-- 去除了复杂的外键约束和不必要的表
-- 使用CLOB代替JSON字段（H2兼容性更好）
-- 使用VARCHAR代替ENUM（H2兼容性更好）
-- =====================================================