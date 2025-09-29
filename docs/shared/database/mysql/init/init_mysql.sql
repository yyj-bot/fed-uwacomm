-- =====================================================
-- 水声联邦学习系统 数据库初始化脚本
-- =====================================================
-- 作者: FedUWAComm Team
-- 版本: 1.5.0
-- 描述: 创建完整的数据库结构，包括所有表、索引、外键约束，支持v1.5协议
-- 使用说明: 直接执行此脚本即可完成数据库初始化
-- v1.5更新: 新增数据集关联管理字段，支持assignedDatasetId和13步完整流程
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


-- 2. 虚拟机表 (vm_instances)
CREATE TABLE IF NOT EXISTS vm_instances (
                                            id VARCHAR(32) PRIMARY KEY COMMENT '虚拟机唯一标识(32位UUID)',
                                            name VARCHAR(100) NOT NULL COMMENT '虚拟机名称',
                                            ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址',
                                            port INT NOT NULL DEFAULT 22 COMMENT 'SSH端口',
                                            os_type VARCHAR(50) NOT NULL COMMENT '操作系统类型',
                                            cpu_cores INT NOT NULL COMMENT 'CPU核心数',
                                            memory_mb INT NOT NULL COMMENT '内存大小(MB)',
                                            disk_gb INT NOT NULL COMMENT '磁盘大小(GB)',
                                            status ENUM('OFFLINE', 'RUNNING', 'STOPPED', 'STARTING', 'STOPPING', 'ERROR') NOT NULL DEFAULT 'OFFLINE' COMMENT '虚拟机状态',
                                            connection_status ENUM('DISCONNECTED', 'CONNECTED', 'CONNECTING', 'RECONNECTING') NOT NULL DEFAULT 'DISCONNECTED' COMMENT 'WebSocket连接状态',
                                            ws_session_id VARCHAR(128) NULL COMMENT 'WebSocket会话ID',
                                            last_heartbeat TIMESTAMP NULL COMMENT '最后心跳时间',
                                            secret_id VARCHAR(128) NULL COMMENT '长期刷新凭证',
                                            secret_expire_time TIMESTAMP NULL COMMENT '刷新凭证过期时间',
                                            system_info JSON NULL COMMENT '系统信息(JSON格式)',
                                            capabilities JSON NULL COMMENT '能力信息(JSON格式)',
                                            network_config JSON NULL COMMENT '网络配置信息(JSON格式)',
                                            metadata JSON NULL COMMENT '元数据信息(JSON格式)',
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                            created_by VARCHAR(32) NULL COMMENT '创建者ID(32位UUID)',
                                            updated_by VARCHAR(32) NULL COMMENT '更新者ID(32位UUID)',
                                            INDEX idx_vm_status (status),
                                            INDEX idx_vm_connection_status (connection_status),
                                            INDEX idx_vm_ip_address (ip_address),
                                            INDEX idx_vm_secret_id (secret_id),
                                            INDEX idx_vm_created_at (created_at)
);

-- 3. 联邦学习任务表 (federated_tasks)
CREATE TABLE IF NOT EXISTS federated_tasks (
                                               id VARCHAR(32) PRIMARY KEY COMMENT '任务唯一标识(32位UUID)',
                                               name VARCHAR(100) NOT NULL COMMENT '任务名称',
                                               algorithm ENUM(
                                                   'FEDERATED_AVERAGING',
                                                   'FEDERATED_PROXIMAL',
                                                   'FEDERATED_NOVA',
                                                   'SCAFFOLD'
                                                   ) NOT NULL COMMENT '联邦学习算法',
                                               status ENUM(
                                                   'CREATED',
                                                   'CONFIGURED',
                                                   'PENDING',
                                                   'RUNNING',
                                                   'PAUSED',
                                                   'COMPLETED',
                                                   'FAILED',
                                                   'STOPPED',
                                                   'CANCELLED'
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
                                               config JSON COMMENT '算法配置参数',
    -- v1.4/v1.5协议新增字段
                                               protocol_version VARCHAR(10) DEFAULT 'v1.5' COMMENT '协议版本',
                                               lifecycle_status ENUM('CREATED', 'RUNNING', 'STOPPED', 'RESUMED', 'DELETED') DEFAULT 'CREATED' COMMENT 'v1.4任务生命周期状态',
                                               supports_multi_task BOOLEAN DEFAULT TRUE COMMENT '是否支持多任务并发',
                                               resume_info JSON NULL COMMENT '恢复信息',
                                               version BIGINT DEFAULT 1 COMMENT '乐观锁版本号'
);

-- 4. 训练数据集元信息表 (training_dataset，原training_data)
CREATE TABLE IF NOT EXISTS training_dataset (
                                                id VARCHAR(32) PRIMARY KEY COMMENT '数据集唯一标识(32位UUID)',
                                                vm_id VARCHAR(32) NULL COMMENT '关联虚拟机ID(32位UUID)',
                                                name VARCHAR(255) NOT NULL COMMENT '数据集名称',
                                                description TEXT COMMENT '数据集描述',
                                                row_count INT DEFAULT 0 COMMENT '数据行数',
                                                data_type ENUM(
                                                    'ACOUSTIC',
                                                    'ENVIRONMENT',
                                                    'MODEL',
                                                    'OTHER',
                                                    'TEST_DATA',
                                                    'SPECIAL_CHARS',
                                                    'LONG_TEXT'
                                                    ) NOT NULL COMMENT '数据类型',
                                                status ENUM(
                                                    'UPLOADING',
                                                    'PROCESSING',
                                                    'READY',
                                                    'ERROR'
                                                    ) DEFAULT 'UPLOADING' COMMENT '处理状态',
                                                file_path VARCHAR(500) COMMENT '文件存储路径',
                                                file_size BIGINT COMMENT '文件大小(字节)',
                                                file_format VARCHAR(50) COMMENT '文件格式',
                                                tags JSON COMMENT '标签列表(JSON格式)',
                                                metadata JSON COMMENT '数据集元信息(JSON格式)',
                                                upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
                                                uploaded_by VARCHAR(32) NULL COMMENT '上传者ID(32位UUID)',
                                                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                updated_by VARCHAR(32) NULL COMMENT '更新者ID(32位UUID)',
                                                is_valid BOOLEAN DEFAULT TRUE COMMENT '数据是否有效',
                                                validation_time TIMESTAMP NULL COMMENT '验证时间',
                                                validation_result JSON COMMENT '验证结果(JSON格式)',
                                                is_processed BOOLEAN DEFAULT FALSE COMMENT '是否已处理',
                                                process_time TIMESTAMP NULL COMMENT '处理时间',
                                                process_result JSON COMMENT '处理结果(JSON格式)',
                                                progress INT DEFAULT 0 COMMENT '处理进度(0-100)',
                                                error_message TEXT COMMENT '错误信息'
);

-- 添加训练数据集表外键约束（在表创建后单独添加）
ALTER TABLE training_dataset
    ADD CONSTRAINT fk_training_dataset_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE training_dataset
    ADD CONSTRAINT fk_training_dataset_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE training_dataset
    ADD CONSTRAINT fk_training_dataset_vm_id FOREIGN KEY (vm_id) REFERENCES vm_instances (id) ON DELETE SET NULL;

-- 5. 训练数据明细表 (training_dataset_row，宽表+JSON)
CREATE TABLE IF NOT EXISTS training_dataset_row (
                                                    id VARCHAR(32) PRIMARY KEY COMMENT '数据行唯一标识(32位UUID)',
                                                    dataset_id VARCHAR(32) NOT NULL COMMENT '所属数据集ID',
                                                    row_data JSON NOT NULL COMMENT '原始CSV行数据（JSON格式）',
                                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '插入时间',
                                                    FOREIGN KEY (dataset_id) REFERENCES training_dataset (id) ON DELETE CASCADE,
                                                    INDEX idx_dataset_id (dataset_id)
);

-- 6. 模型版本表 (model_versions)
CREATE TABLE IF NOT EXISTS model_versions (
                                              id VARCHAR(32) PRIMARY KEY COMMENT '版本唯一标识(32位UUID)',
                                              task_id VARCHAR(32) NOT NULL COMMENT '关联任务ID(32位UUID)',
                                              round_number INT NOT NULL COMMENT '训练轮数',
                                              aggregation_method VARCHAR(50) COMMENT '聚合方法(如FEDAVG、FEDPROX等)',
                                              client_count INT COMMENT '参与客户端数量',
                                              model_json TEXT COMMENT '聚合后模型参数(JSON格式)',
                                              accuracy DECIMAL(5, 4) COMMENT '准确率',
                                              loss DECIMAL(10, 6) COMMENT '损失值',
                                              metrics JSON COMMENT '聚合后评估指标(JSON格式)',
                                              status VARCHAR(20) COMMENT '模型状态(UPLOADING/UPLOADED/VALIDATING/VALIDATED/DEPLOYED/DEPRECATED/FAILED)',
                                              description TEXT COMMENT '模型描述',
                                              file_path VARCHAR(500) COMMENT '模型文件路径',
                                              file_size BIGINT COMMENT '模型文件大小(字节)',
                                              file_format VARCHAR(50) COMMENT '模型文件格式',
                                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                              aggregated_at TIMESTAMP NULL COMMENT '聚合完成时间',
                                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                              created_by VARCHAR(32) COMMENT '创建者ID(32位UUID)',
                                              updated_by VARCHAR(32) COMMENT '更新者ID(32位UUID)',
                                              parameters JSON COMMENT '扩展参数(JSON格式，存储其他模型相关信息)'
);

-- 添加外键约束（在表创建后单独添加，避免NULL约束问题）
ALTER TABLE model_versions
    ADD CONSTRAINT fk_model_versions_task_id FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE;

ALTER TABLE model_versions
    ADD CONSTRAINT fk_model_versions_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE model_versions
    ADD CONSTRAINT fk_model_versions_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL;

-- 7. SpringBoot系统日志表 (system_logs)
CREATE TABLE IF NOT EXISTS system_logs (
                                           id VARCHAR(32) PRIMARY KEY COMMENT '日志唯一标识(32位UUID)',
                                           timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志时间',
                                           level ENUM('INFO', 'WARN', 'ERROR', 'DEBUG') NOT NULL COMMENT '日志级别',
                                           logger VARCHAR(100) NOT NULL COMMENT '日志记录器',
                                           message TEXT NOT NULL COMMENT '日志消息',
                                           thread VARCHAR(100) COMMENT '线程名',
                                           user_id VARCHAR(50) COMMENT '用户ID',
                                           username VARCHAR(100) COMMENT '用户名',
                                           request_uri VARCHAR(500) COMMENT '请求URI',
                                           client_ip VARCHAR(50) COMMENT '客户端IP',
                                           environment VARCHAR(20) COMMENT '环境',
                                           exception TEXT COMMENT '异常信息',
                                           vm_id VARCHAR(32) NULL COMMENT '虚拟机ID(32位UUID)',
                                           task_id VARCHAR(32) NULL COMMENT '任务ID(32位UUID)',
                                           details JSON NULL COMMENT '详细信息',
                                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           INDEX idx_timestamp (timestamp),
                                           INDEX idx_level (level),
                                           INDEX idx_user_id (user_id),
                                           INDEX idx_request_uri (request_uri),
                                           INDEX idx_vm_id (vm_id),
                                           INDEX idx_task_id (task_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'SpringBoot系统日志表';


-- 8. 虚拟机轮次模型结果表 (vm_round_models)
CREATE TABLE IF NOT EXISTS vm_round_models (
                                               id VARCHAR(32) PRIMARY KEY COMMENT '唯一标识(32位UUID)',
                                               task_id VARCHAR(32) NOT NULL COMMENT '任务ID(32位UUID)',
                                               vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
                                               round_number INT NOT NULL COMMENT '训练轮数',
                                               accuracy DECIMAL(5, 4) NULL COMMENT '准确率',
                                               loss DECIMAL(10, 6) NULL COMMENT '损失值',
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               parameters JSON COMMENT '本地模型参数/元信息(JSON，仅记录，不存文件路径)'
);

-- 添加外键约束（在表创建后单独添加，避免NULL约束问题）
ALTER TABLE vm_round_models
    ADD CONSTRAINT fk_vm_round_models_task_id FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE;

ALTER TABLE vm_round_models
    ADD CONSTRAINT fk_vm_round_models_vm_id FOREIGN KEY (vm_id) REFERENCES vm_instances (id) ON DELETE CASCADE;

-- 9. 虚拟机刷新凭证表 (vm_secrets)
CREATE TABLE IF NOT EXISTS vm_secrets (
                                          id VARCHAR(32) PRIMARY KEY COMMENT '凭证唯一标识(32位UUID)',
                                          vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
                                          secret_hash VARCHAR(128) NOT NULL COMMENT 'secretId 哈希(如SHA-256)',
                                          salt VARCHAR(32) NULL COMMENT '哈希盐值',
                                          status ENUM(
                                              'ACTIVE',
                                              'REVOKED',
                                              'EXPIRED'
                                              ) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
                                          expires_at TIMESTAMP NULL COMMENT '过期时间',
                                          last_used_at TIMESTAMP NULL COMMENT '最后使用时间',
                                          rotated_at TIMESTAMP NULL COMMENT '最近旋转时间',
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          UNIQUE KEY uq_vm_secrets_active (vm_id, status),
                                          INDEX idx_vm_secrets_vm_id (vm_id),
                                          INDEX idx_vm_secrets_status (status)
);

ALTER TABLE vm_secrets
    ADD CONSTRAINT fk_vm_secrets_vm_id FOREIGN KEY (vm_id) REFERENCES vm_instances (id) ON DELETE CASCADE;

-- 10. 全局模型表 (global_models)
CREATE TABLE IF NOT EXISTS global_models (
                                             id VARCHAR(32) PRIMARY KEY COMMENT '唯一标识(32位UUID)',
                                             task_id VARCHAR(32) NOT NULL COMMENT '任务ID(32位UUID)',
                                             round_number INT NOT NULL COMMENT '轮次编号',
                                             aggregation_method ENUM(
                                                 'FEDERATED_AVERAGING',
                                                 'FEDERATED_PROXIMAL',
                                                 'FEDERATED_NOVA',
                                                 'SCAFFOLD'
                                                 ) NOT NULL COMMENT '聚合算法类型',
                                             global_parameters JSON COMMENT '全局模型参数(JSON格式)',
                                             global_loss DECIMAL(10, 8) COMMENT '全局损失值',
                                             global_accuracy DECIMAL(10, 8) COMMENT '全局准确率',
                                             participant_count INT NOT NULL COMMENT '参与聚合的客户端数量',
                                             aggregation_duration BIGINT COMMENT '聚合耗时(毫秒)',
                                             status ENUM(
                                                 'PENDING',
                                                 'AGGREGATING',
                                                 'COMPLETED',
                                                 'FAILED'
                                                 ) NOT NULL DEFAULT 'PENDING' COMMENT '聚合状态',
                                             started_at TIMESTAMP COMMENT '聚合开始时间',
                                             completed_at TIMESTAMP COMMENT '聚合完成时间',
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             metadata JSON COMMENT '聚合元数据(JSON格式)',
    -- v1.4协议新增字段
                                             checksum VARCHAR(128) NULL COMMENT 'v1.4模型校验和',
                                             compression_type VARCHAR(32) NULL COMMENT 'v1.4压缩类型',
                                             model_version VARCHAR(64) NULL COMMENT 'v1.4模型版本号',
                                             INDEX idx_global_models_task_round (task_id, round_number),
                                             INDEX idx_global_models_status (status),
                                             INDEX idx_global_models_created_at (created_at),
                                             INDEX idx_global_models_completed_at (completed_at),
                                             UNIQUE KEY uk_global_models_task_round (task_id, round_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='全局模型表 - 存储联邦学习聚合后的全局模型';

-- 添加外键约束（在表创建后单独添加）
ALTER TABLE global_models
    ADD CONSTRAINT fk_global_models_task_id FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE;

-- 11. 日志导出任务表 (log_export_tasks)
CREATE TABLE IF NOT EXISTS log_export_tasks (
                                                id VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '主键ID，32位UUID',
                                                export_id VARCHAR(50) NOT NULL UNIQUE COMMENT '导出任务ID（用户可见）',
                                                status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
                                                format ENUM('CSV', 'JSON', 'EXCEL') NOT NULL DEFAULT 'CSV' COMMENT '导出格式',
                                                filter_conditions JSON NULL COMMENT '过滤条件，JSON格式存储查询参数',
                                                progress INT NOT NULL DEFAULT 0 COMMENT '进度百分比 0-100',
                                                total_records BIGINT NULL COMMENT '总记录数',
                                                processed_records BIGINT NULL DEFAULT 0 COMMENT '已处理记录数',
                                                file_size BIGINT NULL COMMENT '文件大小（字节）',
                                                file_path VARCHAR(500) NULL COMMENT '文件存储路径',
                                                download_url VARCHAR(500) NULL COMMENT '下载URL',
                                                estimated_time INT NULL COMMENT '预估时间（秒）',
                                                expires_at DATETIME NULL COMMENT '过期时间',
                                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                started_at DATETIME NULL COMMENT '开始处理时间',
                                                completed_at DATETIME NULL COMMENT '完成时间',
                                                error_message TEXT NULL COMMENT '错误信息',
                                                created_by VARCHAR(50) NULL COMMENT '创建用户',
                                                include_details BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否包含详细信息',

                                                INDEX idx_export_id (export_id),
                                                INDEX idx_status (status),
                                                INDEX idx_created_by (created_by),
                                                INDEX idx_created_at (created_at),
                                                INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志导出任务表';

-- 12. 日志清理任务表 (log_cleanup_tasks)
CREATE TABLE IF NOT EXISTS log_cleanup_tasks (
                                                 id VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '主键ID，32位UUID',
                                                 cleanup_id VARCHAR(50) NOT NULL UNIQUE COMMENT '清理任务ID（用户可见）',
                                                 status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
                                                 strategy ENUM('TIME_BASED', 'LEVEL_BASED', 'SIZE_BASED', 'CATEGORY_BASED') NOT NULL DEFAULT 'TIME_BASED' COMMENT '清理策略',
                                                 cleanup_conditions JSON NULL COMMENT '清理条件，JSON格式存储参数',
                                                 progress INT NOT NULL DEFAULT 0 COMMENT '进度百分比 0-100',
                                                 estimated_records BIGINT NULL COMMENT '预估删除记录数',
                                                 deleted_records BIGINT NULL DEFAULT 0 COMMENT '已删除记录数',
                                                 estimated_size BIGINT NULL COMMENT '预估释放空间（字节）',
                                                 freed_space BIGINT NULL DEFAULT 0 COMMENT '已释放空间（字节）',
                                                 dry_run BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否试运行模式',
                                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                 started_at DATETIME NULL COMMENT '开始处理时间',
                                                 completed_at DATETIME NULL COMMENT '完成时间',
                                                 error_message TEXT NULL COMMENT '错误信息',
                                                 created_by VARCHAR(50) NULL COMMENT '创建用户',

                                                 INDEX idx_cleanup_id (cleanup_id),
                                                 INDEX idx_status (status),
                                                 INDEX idx_strategy (strategy),
                                                 INDEX idx_created_by (created_by),
                                                 INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志清理任务表';

-- =====================================================
-- 索引创建
-- =====================================================
-- 注意: system_logs 表的索引已在表定义中包含
-- 用户表索引
CREATE INDEX idx_users_username ON users (username);

CREATE INDEX idx_users_email ON users (email);

CREATE INDEX idx_users_role ON users (role);

CREATE INDEX idx_users_status ON users (status);

CREATE INDEX idx_users_created_at ON users (created_at);


-- 虚拟机表索引
CREATE INDEX idx_vm_instances_connection_status ON vm_instances (connection_status);

CREATE INDEX idx_vm_instances_last_heartbeat ON vm_instances (last_heartbeat);

-- 联邦学习任务表索引
CREATE INDEX idx_federated_tasks_status ON federated_tasks (status);

CREATE INDEX idx_federated_tasks_algorithm ON federated_tasks (algorithm);

-- 训练数据集元信息表索引
CREATE INDEX idx_training_dataset_vm_id ON training_dataset (vm_id);

CREATE INDEX idx_training_dataset_uploaded_by ON training_dataset (uploaded_by);

CREATE INDEX idx_training_dataset_data_type ON training_dataset (data_type);

CREATE INDEX idx_training_dataset_status ON training_dataset (status);

CREATE INDEX idx_training_dataset_upload_time ON training_dataset (upload_time);

CREATE INDEX idx_training_dataset_update_time ON training_dataset (update_time);

CREATE INDEX idx_training_dataset_is_valid ON training_dataset (is_valid);

CREATE INDEX idx_training_dataset_is_processed ON training_dataset (is_processed);

-- 训练数据明细表索引
CREATE INDEX idx_training_dataset_row_dataset_id ON training_dataset_row (dataset_id);

-- 模型版本表索引
CREATE INDEX idx_model_versions_task_id ON model_versions (task_id);

CREATE INDEX idx_model_versions_round_number ON model_versions (round_number);

CREATE INDEX idx_model_versions_aggregation_method ON model_versions (aggregation_method);

CREATE INDEX idx_model_versions_status ON model_versions (status);

CREATE INDEX idx_model_versions_created_at ON model_versions (created_at);

CREATE INDEX idx_model_versions_updated_at ON model_versions (updated_at);

CREATE INDEX idx_model_versions_created_by ON model_versions (created_by);

CREATE INDEX idx_model_versions_task_round ON model_versions (task_id, round_number);

-- 虚拟机轮次模型结果表索引
CREATE UNIQUE INDEX uq_vm_round_models_task_vm_round ON vm_round_models (task_id, vm_id, round_number);

CREATE INDEX idx_vm_round_models_task_id ON vm_round_models (task_id);

CREATE INDEX idx_vm_round_models_vm_id ON vm_round_models (vm_id);

CREATE INDEX idx_vm_round_models_round_number ON vm_round_models (round_number);

-- 全局模型表索引
CREATE INDEX idx_global_models_task_id ON global_models (task_id);

CREATE INDEX idx_global_models_round_number ON global_models (round_number);

CREATE INDEX idx_global_models_aggregation_method ON global_models (aggregation_method);

-- 日志导出任务表索引
CREATE INDEX idx_log_export_tasks_export_id ON log_export_tasks (export_id);

CREATE INDEX idx_log_export_tasks_status ON log_export_tasks (status);

CREATE INDEX idx_log_export_tasks_created_by ON log_export_tasks (created_by);

CREATE INDEX idx_log_export_tasks_created_at ON log_export_tasks (created_at);

CREATE INDEX idx_log_export_tasks_expires_at ON log_export_tasks (expires_at);

-- 日志清理任务表索引
CREATE INDEX idx_log_cleanup_tasks_cleanup_id ON log_cleanup_tasks (cleanup_id);

CREATE INDEX idx_log_cleanup_tasks_status ON log_cleanup_tasks (status);

CREATE INDEX idx_log_cleanup_tasks_strategy ON log_cleanup_tasks (strategy);

CREATE INDEX idx_log_cleanup_tasks_created_by ON log_cleanup_tasks (created_by);

CREATE INDEX idx_log_cleanup_tasks_created_at ON log_cleanup_tasks (created_at);

-- SpringBoot系统日志表索引（已在表定义中包含）
-- CREATE INDEX idx_system_logs_timestamp ON system_logs (timestamp);
-- CREATE INDEX idx_system_logs_level ON system_logs (level);
-- CREATE INDEX idx_system_logs_user_id ON system_logs (user_id);
-- CREATE INDEX idx_system_logs_request_uri ON system_logs (request_uri);


-- =====================================================
-- 重构扩展 - 新增服务相关表
-- =====================================================

-- 13. 初始模型表
CREATE TABLE IF NOT EXISTS initial_models (
                                              id VARCHAR(32) PRIMARY KEY COMMENT '初始模型唯一标识(32位UUID)',
                                              task_id VARCHAR(32) NOT NULL COMMENT '关联任务ID(32位UUID)',
                                              model_type ENUM('NEURAL_NETWORK', 'RANDOM_FOREST') NOT NULL COMMENT '模型类型',
                                              generation_method ENUM('RANDOM', 'CUSTOM_UPLOAD') NOT NULL COMMENT '生成方式',
                                              model_size BIGINT COMMENT '模型大小(字节)',
                                              architecture_params JSON COMMENT '架构参数(JSON格式)',
                                              model_data JSON COMMENT '模型参数数据(JSON格式)',
                                              status ENUM('GENERATING', 'READY', 'DISTRIBUTED', 'FAILED') DEFAULT 'GENERATING' COMMENT '状态',
                                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                              created_by VARCHAR(32) COMMENT '创建者ID(32位UUID)',
                                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                              INDEX idx_initial_models_task_id (task_id),
                                              INDEX idx_initial_models_status (status),
                                              INDEX idx_initial_models_created_at (created_at),
                                              FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='初始模型表 - 存储联邦学习初始模型信息';

-- 14. 模型分发记录表
CREATE TABLE IF NOT EXISTS model_distributions (
                                                   id VARCHAR(32) PRIMARY KEY COMMENT '分发记录唯一标识(32位UUID)',
                                                   model_id VARCHAR(32) NOT NULL COMMENT '模型ID(32位UUID)',
                                                   vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
                                                   distribution_status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED') DEFAULT 'PENDING' COMMENT '分发状态',
                                                   distributed_at TIMESTAMP NULL COMMENT '分发时间',
                                                   verified_at TIMESTAMP NULL COMMENT '验证时间',
                                                   error_message TEXT COMMENT '错误信息',
                                                   checksum_verified BOOLEAN DEFAULT FALSE COMMENT '校验和验证状态',
                                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                   INDEX idx_model_distributions_model_id (model_id),
                                                   INDEX idx_model_distributions_vm_id (vm_id),
                                                   INDEX idx_model_distributions_status (distribution_status),
                                                   FOREIGN KEY (model_id) REFERENCES initial_models (id) ON DELETE CASCADE,
                                                   FOREIGN KEY (vm_id) REFERENCES vm_instances (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='模型分发记录表 - 跟踪模型分发到虚拟机的状态';

-- 15. 数据分发任务表
CREATE TABLE IF NOT EXISTS data_distributions (
                                                  id VARCHAR(32) PRIMARY KEY COMMENT '数据分发任务唯一标识(32位UUID)',
                                                  task_id VARCHAR(32) NOT NULL COMMENT '关联任务ID(32位UUID)',
                                                  distribution_name VARCHAR(100) COMMENT '分发任务名称',
                                                  strategy ENUM('RANDOM', 'BALANCED', 'CUSTOM', 'ROUND_ROBIN') NOT NULL DEFAULT 'BALANCED' COMMENT '分发策略',
                                                  status ENUM('CREATED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED') DEFAULT 'CREATED' COMMENT '分发状态',
                                                  config JSON COMMENT '分发配置参数(JSON格式)',
                                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                  started_at TIMESTAMP NULL COMMENT '开始时间',
                                                  completed_at TIMESTAMP NULL COMMENT '完成时间',
                                                  created_by VARCHAR(32) COMMENT '创建者ID(32位UUID)',
                                                  INDEX idx_data_distributions_task_id (task_id),
                                                  INDEX idx_data_distributions_status (status),
                                                  INDEX idx_data_distributions_created_at (created_at),
                                                  FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='数据分发任务表 - 管理训练数据分发任务';

-- 16. 数据分发详情表
CREATE TABLE IF NOT EXISTS data_distribution_details (
                                                         id VARCHAR(32) PRIMARY KEY COMMENT '分发详情唯一标识(32位UUID)',
                                                         distribution_id VARCHAR(32) NOT NULL COMMENT '分发任务ID(32位UUID)',
                                                         dataset_id VARCHAR(32) NOT NULL COMMENT '数据集ID(32位UUID)',
                                                         vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
                                                         status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED') DEFAULT 'PENDING' COMMENT '分发状态',
                                                         data_size BIGINT COMMENT '数据大小(字节)',
                                                         transferred_size BIGINT DEFAULT 0 COMMENT '已传输大小(字节)',
                                                         checksum VARCHAR(128) COMMENT '数据校验和',
                                                         distributed_at TIMESTAMP NULL COMMENT '分发时间',
                                                         verified_at TIMESTAMP NULL COMMENT '验证时间',
                                                         error_message TEXT COMMENT '错误信息',
                                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                         INDEX idx_data_distribution_details_distribution_id (distribution_id),
                                                         INDEX idx_data_distribution_details_vm_id (vm_id),
                                                         INDEX idx_data_distribution_details_dataset_id (dataset_id),
                                                         INDEX idx_data_distribution_details_status (status),
                                                         FOREIGN KEY (distribution_id) REFERENCES data_distributions (id) ON DELETE CASCADE,
                                                         FOREIGN KEY (dataset_id) REFERENCES training_dataset (id) ON DELETE CASCADE,
                                                         FOREIGN KEY (vm_id) REFERENCES vm_instances (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='数据分发详情表 - 跟踪具体数据分发到各虚拟机的状态';

-- 17. 工作流编排表
CREATE TABLE IF NOT EXISTS orchestration_workflows (
                                                       id VARCHAR(32) PRIMARY KEY COMMENT '工作流唯一标识(32位UUID)',
                                                       task_id VARCHAR(32) NOT NULL COMMENT '关联任务ID(32位UUID)',
                                                       workflow_name VARCHAR(100) COMMENT '工作流名称',
                                                       status ENUM('CREATED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'FAILED', 'TERMINATED') DEFAULT 'CREATED' COMMENT '工作流状态',
                                                       current_stage ENUM('INITIALIZATION', 'INITIAL_MODEL_GENERATION', 'DATA_DISTRIBUTION',
                                                           'MODEL_DISTRIBUTION', 'FEDERATED_TRAINING', 'FINAL_AGGREGATION', 'COMPLETED')
                                                                                                                                            DEFAULT 'INITIALIZATION' COMMENT '当前阶段',
                                                       config JSON COMMENT '工作流配置(JSON格式)',
                                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                       started_at TIMESTAMP NULL COMMENT '开始时间',
                                                       completed_at TIMESTAMP NULL COMMENT '完成时间',
                                                       created_by VARCHAR(32) COMMENT '创建者ID(32位UUID)',
                                                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                       INDEX idx_orchestration_workflows_task_id (task_id),
                                                       INDEX idx_orchestration_workflows_status (status),
                                                       INDEX idx_orchestration_workflows_stage (current_stage),
                                                       INDEX idx_orchestration_workflows_created_at (created_at),
                                                       FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='工作流编排表 - 管理联邦学习工作流执行状态';

-- 18. 工作流阶段执行记录表
CREATE TABLE IF NOT EXISTS workflow_stage_executions (
                                                         id VARCHAR(32) PRIMARY KEY COMMENT '阶段执行记录唯一标识(32位UUID)',
                                                         orchestration_id VARCHAR(32) NOT NULL COMMENT '工作流ID(32位UUID)',
                                                         stage_name VARCHAR(50) NOT NULL COMMENT '阶段名称',
                                                         status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'SKIPPED') DEFAULT 'PENDING' COMMENT '执行状态',
                                                         started_at TIMESTAMP NULL COMMENT '开始时间',
                                                         completed_at TIMESTAMP NULL COMMENT '完成时间',
                                                         input_data JSON COMMENT '输入数据(JSON格式)',
                                                         output_data JSON COMMENT '输出数据(JSON格式)',
                                                         error_message TEXT COMMENT '错误信息',
                                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                         INDEX idx_workflow_stage_executions_orchestration_id (orchestration_id),
                                                         INDEX idx_workflow_stage_executions_stage (stage_name),
                                                         INDEX idx_workflow_stage_executions_status (status),
                                                         INDEX idx_workflow_stage_executions_created_at (created_at),
                                                         FOREIGN KEY (orchestration_id) REFERENCES orchestration_workflows (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='工作流阶段执行记录表 - 跟踪每个工作流阶段的执行状态';


-- 扩展现有表结构
-- 为federated_tasks表添加新字段
ALTER TABLE federated_tasks
    ADD COLUMN orchestration_id VARCHAR(32) NULL COMMENT '工作流编排ID(32位UUID)',
    ADD COLUMN initial_model_strategy ENUM('RANDOM', 'CUSTOM_UPLOAD') DEFAULT 'RANDOM' COMMENT '初始模型策略',
    ADD COLUMN workflow_config JSON COMMENT '工作流配置(JSON格式)',
    ADD INDEX idx_federated_tasks_orchestration_id (orchestration_id);

-- 为global_models表添加分发状态字段
ALTER TABLE global_models
    ADD COLUMN distribution_status ENUM('PENDING', 'DISTRIBUTING', 'DISTRIBUTED', 'FAILED') DEFAULT 'PENDING' COMMENT '分发状态',
    ADD COLUMN distributed_vms JSON COMMENT '已分发的虚拟机列表(JSON格式)',
    ADD COLUMN distribution_completed_at TIMESTAMP NULL COMMENT '分发完成时间';

-- 19. 联邦学习任务参与者表 (task_participants)
CREATE TABLE IF NOT EXISTS task_participants (
                                                 id VARCHAR(32) PRIMARY KEY COMMENT '参与者唯一标识(32位UUID)',
                                                 task_id VARCHAR(32) NOT NULL COMMENT '任务ID(32位UUID)',
                                                 vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
                                                 role ENUM('PARTICIPANT', 'COORDINATOR') NOT NULL DEFAULT 'PARTICIPANT' COMMENT '参与者角色',
                                                 status ENUM('CREATED', 'PENDING', 'CONNECTED', 'DISCONNECTED', 'TRAINING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'CREATED' COMMENT '参与状态',
                                                 data_source VARCHAR(255) COMMENT '数据源',

    -- 训练状态字段
                                                 current_epoch INT COMMENT '当前训练轮次',
                                                 loss DECIMAL(10,8) COMMENT '当前损失值',
                                                 accuracy DECIMAL(10,8) COMMENT '当前准确率',
                                                 last_heartbeat TIMESTAMP COMMENT '最后心跳时间',

    -- 训练结果字段
                                                 final_accuracy DECIMAL(10,8) COMMENT '最终准确率',
                                                 final_loss DECIMAL(10,8) COMMENT '最终损失值',
                                                 training_time BIGINT COMMENT '训练时间(秒)',
                                                 data_size INT COMMENT '数据量大小',

    -- v1.3 扩展字段
                                                 participant_id VARCHAR(50) COMMENT '参与者标识',
                                                 data_ratio DECIMAL(5,4) COMMENT '数据比例',
                                                 capabilities JSON COMMENT '能力列表(JSON格式)',
                                                 max_cpu_usage INT COMMENT '最大CPU使用率',
                                                 max_memory_usage INT COMMENT '最大内存使用率',

    -- 时间字段
                                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                 joined_at TIMESTAMP COMMENT '加入时间',
                                                 left_at TIMESTAMP COMMENT '离开时间',

    -- 配置参数
                                                 parameters JSON COMMENT '参与者参数配置(JSON格式)',

    -- v1.4协议新增字段
                                                 task_execution_context JSON NULL COMMENT 'v1.4任务执行上下文',
                                                 vm_capabilities JSON NULL COMMENT 'VM能力信息',
                                                 concurrent_task_count INT DEFAULT 0 COMMENT '并发任务数量',

    -- v1.5协议新增字段
                                                 assigned_dataset_id VARCHAR(32) NULL COMMENT 'v1.5后端分配的数据集ID(32位UUID)',
                                                 dataset_status ENUM('PENDING', 'CREATED', 'UPLOADING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING' COMMENT 'v1.5数据集状态',
                                                 dataset_created_at TIMESTAMP NULL COMMENT 'v1.5数据集创建时间',
                                                 dataset_completed_at TIMESTAMP NULL COMMENT 'v1.5数据集完成时间',

    -- 索引
                                                 INDEX idx_task_participants_task_id (task_id),
                                                 INDEX idx_task_participants_vm_id (vm_id),
                                                 INDEX idx_task_participants_status (status),
                                                 INDEX idx_task_participants_role (role),
                                                 INDEX idx_task_participants_assigned_dataset_id (assigned_dataset_id),
                                                 INDEX idx_task_participants_dataset_status (dataset_status),
                                                 UNIQUE KEY uk_task_participants_task_vm (task_id, vm_id),

    -- 外键约束
                                                 FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE,
                                                 FOREIGN KEY (vm_id) REFERENCES vm_instances (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='联邦学习任务参与者表 - 存储虚拟机在任务中的参与信息';

-- =====================================================
-- 初始化完成
-- =====================================================
-- =====================================================
-- 轮次同步性重构：性能优化索引
-- =====================================================

-- 全局模型表轮次同步索引
CREATE INDEX idx_global_models_task_round_sync
    ON global_models(task_id, round_number, distribution_status);

-- 模型分发记录状态索引
CREATE INDEX idx_model_distributions_status_sync
    ON model_distributions(model_id, vm_id, distribution_status);

-- 模型分发记录验证时间索引（用于超时检测）
CREATE INDEX idx_model_distributions_verified_at
    ON model_distributions(verified_at);

-- 任务参与者状态索引（用于轮次推进检查）
CREATE INDEX idx_task_participants_status_round
    ON task_participants(task_id, status, current_epoch);

-- 联邦任务更新时间索引（用于乐观锁）
CREATE INDEX idx_federated_tasks_updated_at
    ON federated_tasks(id, updated_at);

-- =====================================================
-- 索引创建完成
-- =====================================================

-- =====================================================
-- v1.4协议新增表
-- =====================================================

-- 20. 轮次状态表，支持v1.4精确状态管理
CREATE TABLE IF NOT EXISTS round_states (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            task_id VARCHAR(64) NOT NULL,
                                            round_number INT NOT NULL,
                                            state ENUM('INITIALIZING', 'TRAINING', 'AGGREGATING', 'DISTRIBUTING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'INITIALIZING' COMMENT 'v1.4轮次状态',
                                            participant_count INT NOT NULL DEFAULT 0 COMMENT '参与者数量',
                                            completed_participants INT NOT NULL DEFAULT 0 COMMENT '已完成参与者数量',
                                            gradient_uploads_received INT NOT NULL DEFAULT 0 COMMENT '已收到梯度上传数量',
                                            model_broadcasts_acked INT NOT NULL DEFAULT 0 COMMENT '模型广播确认数量',
                                            started_at TIMESTAMP NULL COMMENT '轮次开始时间',
                                            completed_at TIMESTAMP NULL COMMENT '轮次完成时间',
                                            error_message TEXT NULL COMMENT '错误信息',
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                            INDEX idx_round_states_task_id (task_id),
                                            INDEX idx_round_states_task_round (task_id, round_number),
                                            INDEX idx_round_states_state (state),
                                            INDEX idx_round_states_started_at (started_at),
                                            UNIQUE KEY uk_round_states_task_round (task_id, round_number),
                                            FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT = 'v1.4协议轮次状态管理表';

-- 21. VM确认跟踪表，支持v1.4确认机制
CREATE TABLE IF NOT EXISTS vm_ack_tracking (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               task_id VARCHAR(64) NOT NULL,
                                               round_number INT NULL COMMENT 'NULL表示任务级别确认',
                                               vm_id VARCHAR(32) NOT NULL,
                                               ack_type ENUM(
                                                   'TASK_START', 'TASK_STOP', 'TASK_RESUME', 'TASK_DELETE',
                                                   'ROUND_START', 'GRADIENT_UPLOAD', 'GLOBAL_MODEL_BROADCAST', 'ROUND_COMPLETE',
                                                   'DATASET_LIST_QUERY', 'DATASET_CREATE', 'DATASET_STATUS_QUERY'
                                                   ) NOT NULL COMMENT 'v1.4/v1.5确认类型',
                                               status ENUM('PENDING', 'SUCCESS', 'FAILED', 'TIMEOUT') NOT NULL DEFAULT 'PENDING' COMMENT '确认状态',
                                               ack_data JSON NULL COMMENT '确认数据',
                                               error_message TEXT NULL COMMENT '错误信息',
                                               acknowledged_at TIMESTAMP NULL COMMENT '确认时间',
                                               timeout_at TIMESTAMP NULL COMMENT '超时时间',
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                               INDEX idx_vm_ack_tracking_task_id (task_id),
                                               INDEX idx_vm_ack_tracking_task_round (task_id, round_number),
                                               INDEX idx_vm_ack_tracking_vm_id (vm_id),
                                               INDEX idx_vm_ack_tracking_type (ack_type),
                                               INDEX idx_vm_ack_tracking_status (status),
                                               INDEX idx_vm_ack_tracking_timeout (timeout_at),
                                               FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE,
                                               FOREIGN KEY (vm_id) REFERENCES vm_instances (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT = 'v1.4协议VM确认跟踪表';

-- =====================================================

-- 数据库初始化脚本执行完成
-- 共创建了 21 个表:
-- 1. users - 用户表
-- 2. vm_instances - 虚拟机表
-- 3. federated_tasks - 联邦学习任务表（已扩展）
-- 4. training_dataset - 训练数据集元信息表
-- 5. training_dataset_row - 训练数据明细表
-- 6. model_versions - 模型版本表
-- 7. system_logs - SpringBoot系统日志表
-- 8. vm_round_models - 虚拟机轮次模型结果表
-- 9. vm_secrets - 虚拟机刷新凭证表
-- 10. global_models - 全局模型表（已扩展）
-- 11. log_export_tasks - 日志导出任务表
-- 12. log_cleanup_tasks - 日志清理任务表
-- 13. initial_models - 初始模型表（新增）
-- 14. model_distributions - 模型分发记录表（新增）
-- 15. data_distributions - 数据分发任务表（新增）
-- 16. data_distribution_details - 数据分发详情表（新增）
-- 17. orchestration_workflows - 工作流编排表（新增）
-- 18. workflow_stage_executions - 工作流阶段执行记录表（新增）
-- 19. task_participants - 联邦学习任务参与者表（新增）
-- 20. round_states - v1.4轮次状态管理表（v1.4新增）
-- 21. vm_ack_tracking - v1.4VM确认跟踪表（v1.4新增）
--
-- v1.4/v1.5协议扩展的现有表:
-- - federated_tasks: 新增协议版本(默认v1.5)、生命周期状态、多任务支持等字段
-- - task_participants: 新增任务执行上下文、VM能力信息、并发任务计数等字段，v1.5新增assignedDatasetId数据集关联字段
-- - global_models: 新增校验和、压缩类型、模型版本等字段
-- - vm_ack_tracking: 新增v1.5数据集相关确认类型：DATASET_LIST_QUERY、DATASET_CREATE、DATASET_STATUS_QUERY
--
-- v1.5协议新增特性:
-- - 13步完整联邦学习流程支持
-- - 后端统一ID管理(使用UuidUtil生成)
-- - assignedDatasetId精确数据集分发
-- - 数据集状态跟踪和确认机制
-- =====================================================