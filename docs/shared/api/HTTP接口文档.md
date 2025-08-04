# 水声联邦学习系统 HTTP 接口文档

## 1. 概述

本文档定义了水声联邦学习系统的HTTP REST API接口，包括虚拟机管理、训练数据管理、联邦学习任务管理等功能。

### 1.1 基础信息
- **基础URL**: `http://localhost:8080/api`
- **API版本**: v1.0
- **认证方式**: JWT Token
- **数据格式**: JSON

### 1.2 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2024-01-01T00:00:00.000Z"
}
```

## 2. 数据库表结构设计

### 2.1 虚拟机表 (vm_instances)
```sql
CREATE TABLE vm_instances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vm_id VARCHAR(64) UNIQUE NOT NULL COMMENT '虚拟机唯一标识',
    name VARCHAR(100) NOT NULL COMMENT '虚拟机名称',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    port INT DEFAULT 22 COMMENT 'SSH端口',
    status ENUM('RUNNING', 'STOPPED', 'STARTING', 'STOPPING', 'ERROR', 'OFFLINE') DEFAULT 'STOPPED',
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

### 2.2 联邦学习任务表 (federated_tasks)
```sql
CREATE TABLE federated_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) UNIQUE NOT NULL COMMENT '任务唯一标识',
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

### 2.3 训练数据表 (training_data)
```sql
CREATE TABLE training_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_id VARCHAR(64) UNIQUE NOT NULL COMMENT '数据唯一标识',
    vm_id VARCHAR(64) NOT NULL COMMENT '虚拟机ID',
    filename VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    data_type ENUM('ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'OTHER') NOT NULL COMMENT '数据类型',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('UPLOADING', 'PROCESSING', 'READY', 'ERROR') DEFAULT 'UPLOADING',
    metadata JSON COMMENT '数据元信息',
    FOREIGN KEY (vm_id) REFERENCES vm_instances(vm_id)
);
```

### 2.4 模型版本表 (model_versions)
```sql
CREATE TABLE model_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id VARCHAR(64) UNIQUE NOT NULL COMMENT '版本唯一标识',
    task_id VARCHAR(64) NOT NULL COMMENT '关联任务ID',
    vm_id VARCHAR(64) NULL COMMENT '虚拟机ID(本地模型)',
    round_number INT NOT NULL COMMENT '训练轮数',
    model_type ENUM('GLOBAL', 'LOCAL') NOT NULL COMMENT '模型类型',
    model_path VARCHAR(500) NOT NULL COMMENT '模型文件路径',
    model_size BIGINT COMMENT '模型大小(字节)',
    accuracy DECIMAL(5,4) COMMENT '准确率',
    loss DECIMAL(10,6) COMMENT '损失值',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parameters JSON COMMENT '模型参数',
    FOREIGN KEY (task_id) REFERENCES federated_tasks(task_id),
    FOREIGN KEY (vm_id) REFERENCES vm_instances(vm_id)
);
```

### 2.5 系统日志表 (system_logs)
```sql
CREATE TABLE system_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_id VARCHAR(64) UNIQUE NOT NULL COMMENT '日志唯一标识',
    level ENUM('INFO', 'WARN', 'ERROR', 'DEBUG') NOT NULL,
    category VARCHAR(50) NOT NULL COMMENT '日志类别',
    vm_id VARCHAR(64) NULL COMMENT '虚拟机ID',
    task_id VARCHAR(64) NULL COMMENT '任务ID',
    message TEXT NOT NULL COMMENT '日志消息',
    details JSON COMMENT '详细信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vm_id) REFERENCES vm_instances(vm_id),
    FOREIGN KEY (task_id) REFERENCES federated_tasks(task_id)
);
```