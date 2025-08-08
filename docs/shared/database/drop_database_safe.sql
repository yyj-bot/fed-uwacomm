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
DELETE FROM vm_runtime_logs;
DELETE FROM system_logs;
DELETE FROM model_versions;
DELETE FROM training_data;
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
ALTER TABLE user_permissions DROP FOREIGN KEY IF EXISTS fk_user_permissions_user_id;
ALTER TABLE user_permissions DROP FOREIGN KEY IF EXISTS fk_user_permissions_granted_by;
ALTER TABLE users DROP FOREIGN KEY IF EXISTS fk_users_created_by;
ALTER TABLE users DROP FOREIGN KEY IF EXISTS fk_users_updated_by;
ALTER TABLE training_data DROP FOREIGN KEY IF EXISTS fk_training_data_vm_id;
ALTER TABLE model_versions DROP FOREIGN KEY IF EXISTS fk_model_versions_task_id;
ALTER TABLE model_versions DROP FOREIGN KEY IF EXISTS fk_model_versions_vm_id;
ALTER TABLE vm_runtime_logs DROP FOREIGN KEY IF EXISTS fk_vm_runtime_logs_vm_id;
ALTER TABLE vm_runtime_logs DROP FOREIGN KEY IF EXISTS fk_vm_runtime_logs_task_id;

-- 删除表（按依赖关系顺序）
DROP TABLE IF EXISTS vm_runtime_logs;
DROP TABLE IF EXISTS system_logs;
DROP TABLE IF EXISTS model_versions;
DROP TABLE IF EXISTS training_data;
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
        ELSE '还有表存在'
    END AS '状态'
FROM information_schema.tables
WHERE
    table_schema = 'feduwacomm'
    AND table_type = 'BASE TABLE';

-- 列出剩余的表
SELECT
    '剩余表列表' AS '检查项目',
    table_name AS '表名',
    table_comment AS '表注释'
FROM information_schema.tables
WHERE
    table_schema = 'feduwacomm'
    AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- =====================================================
-- 使用说明
-- =====================================================
-- 1. 仅删除数据: 取消注释级别1的代码块
-- 2. 删除表结构: 取消注释级别2的代码块
-- 3. 完全删除: 取消注释级别3的代码块
-- 4. 验证结果: 执行验证脚本检查结果
-- =====================================================