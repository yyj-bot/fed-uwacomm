-- =====================================================
-- 水声联邦学习系统 数据库卸载脚本
-- =====================================================
-- 作者: FedUWAComm Team
-- 版本: 1.0.0
-- 描述: 删除完整的数据库结构，包括所有表、索引、外键约束
-- 使用说明: 谨慎执行此脚本，将永久删除所有数据
-- 警告: 此操作不可逆，请确保已备份重要数据
-- =====================================================

-- 使用数据库
USE feduwacomm;

-- =====================================================
-- 删除外键约束
-- =====================================================
-- 注意: 删除表之前需要先删除外键约束，避免删除失败

-- 删除用户权限表的外键约束
ALTER TABLE user_permissions
DROP FOREIGN KEY IF EXISTS fk_user_permissions_user_id;
-- user_id -> users(id)
ALTER TABLE user_permissions
DROP FOREIGN KEY IF EXISTS fk_user_permissions_granted_by;
-- granted_by -> users(id)

-- 删除用户表的外键约束（自引用）
ALTER TABLE users DROP FOREIGN KEY IF EXISTS fk_users_created_by;
-- created_by -> users(id)
ALTER TABLE users DROP FOREIGN KEY IF EXISTS fk_users_updated_by;
-- updated_by -> users(id)

-- 删除训练数据集元信息表和明细表的外键约束
ALTER TABLE training_dataset_row
DROP FOREIGN KEY IF EXISTS fk_training_dataset_row_dataset_id;

ALTER TABLE training_dataset
DROP FOREIGN KEY IF EXISTS fk_training_dataset_vm_id;

-- 删除模型版本表外键约束
ALTER TABLE model_versions
DROP FOREIGN KEY IF EXISTS fk_model_versions_task_id;

-- 删除虚拟机运行日志表的外键约束
ALTER TABLE vm_runtime_logs
DROP FOREIGN KEY IF EXISTS fk_vm_runtime_logs_vm_id;
-- vm_id -> vm_instances(id)
ALTER TABLE vm_runtime_logs
DROP FOREIGN KEY IF EXISTS fk_vm_runtime_logs_task_id;
-- task_id -> federated_tasks(id)

-- 删除虚拟机轮次模型结果表外键约束
ALTER TABLE vm_round_models
DROP FOREIGN KEY IF EXISTS fk_vm_round_models_task_id;
ALTER TABLE vm_round_models
DROP FOREIGN KEY IF EXISTS fk_vm_round_models_vm_id;

-- 删除虚拟机刷新凭证表外键约束
ALTER TABLE vm_secrets
DROP FOREIGN KEY IF EXISTS fk_vm_secrets_vm_id;

-- =====================================================
-- 删除索引
-- =====================================================
-- 注意: 删除表时会自动删除索引，但为了安全起见，先显式删除索引

-- 删除用户表索引
DROP INDEX IF EXISTS idx_users_username ON users;
DROP INDEX IF EXISTS idx_users_email ON users;
DROP INDEX IF EXISTS idx_users_role ON users;
DROP INDEX IF EXISTS idx_users_status ON users;
DROP INDEX IF EXISTS idx_users_created_at ON users;

-- 删除用户权限表索引
DROP INDEX IF EXISTS idx_user_permissions_user_id ON user_permissions;

DROP INDEX IF EXISTS idx_user_permissions_resource_type ON user_permissions;

DROP INDEX IF EXISTS idx_user_permissions_resource_id ON user_permissions;

DROP INDEX IF EXISTS idx_user_permissions_permission ON user_permissions;

-- 删除虚拟机表索引
DROP INDEX IF EXISTS idx_vm_status ON vm_instances;
DROP INDEX IF EXISTS idx_vm_connection_status ON vm_instances;
DROP INDEX IF EXISTS idx_vm_ip_address ON vm_instances;
DROP INDEX IF EXISTS idx_vm_secret_id ON vm_instances;
DROP INDEX IF EXISTS idx_vm_created_at ON vm_instances;
DROP INDEX IF EXISTS idx_vm_instances_connection_status ON vm_instances;
DROP INDEX IF EXISTS idx_vm_instances_last_heartbeat ON vm_instances;

-- 删除联邦学习任务表索引
DROP INDEX IF EXISTS idx_federated_tasks_status ON federated_tasks;

DROP INDEX IF EXISTS idx_federated_tasks_algorithm ON federated_tasks;

-- 删除训练数据集元信息表和明细表的索引
DROP INDEX IF EXISTS idx_training_dataset_vm_id ON training_dataset;

DROP INDEX IF EXISTS idx_training_dataset_data_type ON training_dataset;

DROP INDEX IF EXISTS idx_training_dataset_status ON training_dataset;

DROP INDEX IF EXISTS idx_training_dataset_row_dataset_id ON training_dataset_row;

-- 删除模型版本表索引
DROP INDEX IF EXISTS idx_model_versions_task_id ON model_versions;

DROP INDEX IF EXISTS idx_model_versions_round_number ON model_versions;

-- 删除虚拟机轮次模型结果表索引
DROP INDEX IF EXISTS uq_vm_round_models_task_vm_round ON vm_round_models;
DROP INDEX IF EXISTS idx_vm_round_models_task_id ON vm_round_models;
DROP INDEX IF EXISTS idx_vm_round_models_vm_id ON vm_round_models;
DROP INDEX IF EXISTS idx_vm_round_models_round_number ON vm_round_models;

-- 删除虚拟机刷新凭证表索引
DROP INDEX IF EXISTS uq_vm_secrets_active ON vm_secrets;
DROP INDEX IF EXISTS idx_vm_secrets_vm_id ON vm_secrets;
DROP INDEX IF EXISTS idx_vm_secrets_status ON vm_secrets;

-- 删除SpringBoot系统日志表索引
DROP INDEX IF EXISTS idx_timestamp ON system_logs;
DROP INDEX IF EXISTS idx_level ON system_logs;
DROP INDEX IF EXISTS idx_user_id ON system_logs;
DROP INDEX IF EXISTS idx_request_uri ON system_logs;

-- 删除虚拟机运行日志表索引
DROP INDEX IF EXISTS idx_level ON vm_runtime_logs;
DROP INDEX IF EXISTS idx_category ON vm_runtime_logs;
DROP INDEX IF EXISTS idx_vm_id ON vm_runtime_logs;
DROP INDEX IF EXISTS idx_task_id ON vm_runtime_logs;
DROP INDEX IF EXISTS idx_created_at ON vm_runtime_logs;

-- =====================================================
-- 删除表
-- =====================================================
-- 按照依赖关系顺序删除表（先删除被引用的表）

-- 删除虚拟机刷新凭证表
DROP TABLE IF EXISTS vm_secrets;

-- 删除虚拟机轮次模型结果表
DROP TABLE IF EXISTS vm_round_models;

-- 删除虚拟机运行日志表（依赖vm_instances和federated_tasks）
DROP TABLE IF EXISTS vm_runtime_logs;

-- 删除SpringBoot系统日志表
DROP TABLE IF EXISTS system_logs;

-- 删除模型版本表（依赖federated_tasks和vm_instances）
DROP TABLE IF EXISTS model_versions;

-- 删除训练数据集明细表
DROP TABLE IF EXISTS training_dataset_row;
-- 删除训练数据集元信息表
DROP TABLE IF EXISTS training_dataset;

-- 删除联邦学习任务表
DROP TABLE IF EXISTS federated_tasks;

-- 删除虚拟机表
DROP TABLE IF EXISTS vm_instances;

-- 删除用户权限表（依赖users）
DROP TABLE IF EXISTS user_permissions;

-- 删除用户表（最后删除，因为其他表可能依赖它）
DROP TABLE IF EXISTS users;

-- =====================================================
-- 删除数据库
-- =====================================================
-- 注意: 删除数据库会删除所有相关对象，包括表、视图、存储过程等

-- 切换到系统数据库
USE mysql;

-- 删除数据库
DROP DATABASE IF EXISTS feduwacomm;

-- =====================================================
-- 验证删除结果
-- =====================================================
-- 检查数据库是否已删除
SELECT
    '数据库删除验证' AS '验证项目',
    COUNT(*) AS '剩余数据库数量',
    0 AS '期望数量',
    CASE
        WHEN COUNT(*) = 0 THEN '✓ 成功'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.schemata
WHERE
    schema_name = 'feduwacomm';

-- =====================================================
-- 卸载完成
-- =====================================================
-- 数据库卸载脚本执行完成
-- 已删除以下对象:
-- 1. 所有外键约束 (共12个)
-- 2. 所有索引
-- 3. 所有表 (共11个):
--    - vm_secrets (虚拟机刷新凭证表)
--    - vm_round_models (虚拟机轮次模型结果表)
--    - vm_runtime_logs (虚拟机运行日志表)
--    - system_logs (SpringBoot系统日志表)
--    - model_versions (模型版本表)
--    - training_dataset_row (训练数据明细表)
--    - training_dataset (训练数据集元信息表)
--    - federated_tasks (联邦学习任务表)
--    - vm_instances (虚拟机表)
--    - user_permissions (用户权限表)
--    - users (用户表)
-- 4. feduwacomm数据库
-- =====================================================