-- =====================================================
-- 水声联邦学习系统 安全数据库卸载脚本
-- =====================================================
-- 作者: FedUWAComm Team
-- 版本: 1.0.0
-- 描述: 提供不同级别的数据库卸载选项
-- 使用说明: 根据需求选择相应的卸载级别
-- 警告: 请谨慎执行，确保已备份重要数据
-- =====================================================

-- 使用数据库
USE feduwacomm;

-- =====================================================
-- 卸载级别说明
-- =====================================================
-- 级别1: 仅删除数据，保留表结构
-- 级别2: 删除表和数据，保留数据库
-- 级别3: 完全删除数据库（最彻底）

-- =====================================================
-- 级别1: 仅删除数据（保留表结构）
-- =====================================================
-- 执行以下语句将删除所有数据但保留表结构
-- 适用于需要清空数据但保留表结构的场景

/*
-- 删除所有表数据（按依赖关系顺序）
DELETE FROM vm_secrets;
DELETE FROM vm_round_models;
DELETE FROM vm_runtime_logs;
DELETE FROM system_logs;
DELETE FROM model_versions;
DELETE FROM training_dataset_row;
DELETE FROM training_dataset;
DELETE FROM federated_tasks;
DELETE FROM vm_instances;
DELETE FROM user_permissions;
DELETE FROM users;

-- 重置自增序列（如果有的话）
-- ALTER TABLE table_name AUTO_INCREMENT = 1;
*/

-- =====================================================
-- 级别2: 删除表和数据（保留数据库）
-- =====================================================
-- 执行以下语句将删除所有表和数据，但保留数据库
-- 适用于需要重新创建表结构的场景

/*
-- 删除外键约束
ALTER TABLE vm_secrets DROP FOREIGN KEY IF EXISTS fk_vm_secrets_vm_id;
ALTER TABLE vm_round_models DROP FOREIGN KEY IF EXISTS fk_vm_round_models_task_id;
ALTER TABLE vm_round_models DROP FOREIGN KEY IF EXISTS fk_vm_round_models_vm_id;
ALTER TABLE vm_runtime_logs DROP FOREIGN KEY IF EXISTS fk_vm_runtime_logs_vm_id;
ALTER TABLE vm_runtime_logs DROP FOREIGN KEY IF EXISTS fk_vm_runtime_logs_task_id;
ALTER TABLE model_versions DROP FOREIGN KEY IF EXISTS fk_model_versions_task_id;
ALTER TABLE training_dataset_row DROP FOREIGN KEY IF EXISTS fk_training_dataset_row_dataset_id;
ALTER TABLE training_dataset DROP FOREIGN KEY IF EXISTS fk_training_dataset_vm_id;
ALTER TABLE user_permissions DROP FOREIGN KEY IF EXISTS fk_user_permissions_user_id;
ALTER TABLE user_permissions DROP FOREIGN KEY IF EXISTS fk_user_permissions_granted_by;
ALTER TABLE users DROP FOREIGN KEY IF EXISTS fk_users_created_by;
ALTER TABLE users DROP FOREIGN KEY IF EXISTS fk_users_updated_by;

-- 删除表（按依赖关系顺序）
DROP TABLE IF EXISTS vm_secrets;
DROP TABLE IF EXISTS vm_round_models;
DROP TABLE IF EXISTS vm_runtime_logs;
DROP TABLE IF EXISTS system_logs;
DROP TABLE IF EXISTS model_versions;
DROP TABLE IF EXISTS training_dataset_row;
DROP TABLE IF EXISTS training_dataset;
DROP TABLE IF EXISTS federated_tasks;
DROP TABLE IF EXISTS vm_instances;
DROP TABLE IF EXISTS user_permissions;
DROP TABLE IF EXISTS users;
*/

-- =====================================================
-- 级别3: 完全删除数据库
-- =====================================================
-- 执行以下语句将完全删除数据库
-- 适用于需要彻底清理的场景

/*
-- 切换到系统数据库
USE mysql;

-- 删除数据库
DROP DATABASE IF EXISTS feduwacomm;
*/

-- =====================================================
-- 验证脚本
-- =====================================================
-- 执行以下查询来验证当前数据库状态

-- 检查数据库是否存在
SELECT
    '数据库存在性检查' AS '检查项目',
    COUNT(*) AS '数据库数量',
    CASE
        WHEN COUNT(*) > 0 THEN '存在'
        ELSE '不存在'
    END AS '状态'
FROM information_schema.schemata
WHERE
    schema_name = 'feduwacomm';

-- 检查表数量
SELECT
    '表数量检查' AS '检查项目',
    COUNT(*) AS '表数量',
    CASE
        WHEN COUNT(*) = 0 THEN '已清空'
        WHEN COUNT(*) = 11 THEN '完整'
        ELSE '部分存在'
    END AS '状态'
FROM information_schema.tables
WHERE
    table_schema = 'feduwacomm'
    AND table_type = 'BASE TABLE';

-- 列出所有表
SELECT
    '表列表' AS '检查项目',
    table_name AS '表名',
    table_comment AS '表注释',
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS '大小(MB)'
FROM information_schema.tables
WHERE
    table_schema = 'feduwacomm'
    AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- 检查每个表的数据行数
SELECT
    '表数据统计' AS '检查项目',
    table_name AS '表名',
    table_rows AS '估计行数',
    CASE
        WHEN table_rows = 0 THEN '空表'
        WHEN table_rows < 100 THEN '少量数据'
        WHEN table_rows < 1000 THEN '中量数据'
        ELSE '大量数据'
    END AS '数据状态'
FROM information_schema.tables
WHERE
    table_schema = 'feduwacomm'
    AND table_type = 'BASE TABLE'
ORDER BY table_rows DESC;

-- 检查外键约束
SELECT
    '外键约束统计' AS '检查项目',
    COUNT(*) AS '外键数量',
    CASE
        WHEN COUNT(*) = 12 THEN '完整'
        WHEN COUNT(*) = 0 THEN '已清理'
        ELSE '部分存在'
    END AS '状态'
FROM information_schema.key_column_usage
WHERE
    table_schema = 'feduwacomm'
    AND referenced_table_name IS NOT NULL;

-- =====================================================
-- 使用说明
-- =====================================================
-- 1. 仅删除数据: 取消注释级别1的代码块
-- 2. 删除表结构: 取消注释级别2的代码块
-- 3. 完全删除: 取消注释级别3的代码块
-- 4. 验证结果: 执行验证脚本检查结果

-- =====================================================
-- 表结构说明
-- =====================================================
-- 本系统共包含 11 个核心数据表:
-- 1. users - 用户表
-- 2. user_permissions - 用户权限表
-- 3. vm_instances - 虚拟机表
-- 4. federated_tasks - 联邦学习任务表
-- 5. training_dataset - 训练数据集元信息表
-- 6. training_dataset_row - 训练数据明细表
-- 7. model_versions - 模型版本表
-- 8. system_logs - SpringBoot系统日志表
-- 9. vm_runtime_logs - 虚拟机运行日志表
-- 10. vm_round_models - 虚拟机轮次模型结果表
-- 11. vm_secrets - 虚拟机刷新凭证表
--
-- 外键约束: 共 12 个，确保数据完整性和关联性
-- =====================================================

-- =====================================================
-- 数据备份提醒
-- =====================================================
-- 在执行任何删除操作之前，建议执行以下备份命令:
-- mysqldump -u root -p feduwacomm > feduwacomm_backup_$(date +%Y%m%d_%H%M%S).sql
-- =====================================================