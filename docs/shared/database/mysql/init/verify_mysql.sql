-- =====================================================
-- 水声联邦学习系统 数据库验证脚本
-- =====================================================
-- 作者: FedUWAComm Team
-- 版本: 1.0.0
-- 描述: 验证数据库初始化是否成功
-- 使用说明: 在 init_mysql.sql 执行后运行此脚本进行验证
-- =====================================================

USE feduwacomm;

-- 验证数据库是否存在
SELECT
    '数据库验证' AS '验证项目',
    DATABASE() AS '当前数据库',
    'feduwacomm' AS '期望数据库',
    CASE
        WHEN DATABASE() = 'feduwacomm' THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果';

-- 验证表是否存在
SELECT
    '表结构验证' AS '验证项目',
    COUNT(*) AS '表数量',
    11 AS '期望表数量',
    CASE
        WHEN COUNT(*) = 11 THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.tables
WHERE
    table_schema = 'feduwacomm'
    AND table_type = 'BASE TABLE';

-- 列出所有表
SELECT
    '表列表' AS '验证项目',
    table_name AS '表名',
    table_comment AS '表注释'
FROM information_schema.tables
WHERE
    table_schema = 'feduwacomm'
    AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- 验证索引是否存在
SELECT
    '索引验证' AS '验证项目',
    COUNT(*) AS '索引数量',
    '>= 30' AS '期望索引数量',
    CASE
        WHEN COUNT(*) >= 30 THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.statistics
WHERE
    table_schema = 'feduwacomm';

-- 验证外键约束是否存在
SELECT
    '外键验证' AS '验证项目',
    COUNT(*) AS '外键数量',
    '= 12' AS '期望外键数量',
    CASE
        WHEN COUNT(*) = 12 THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.key_column_usage
WHERE
    table_schema = 'feduwacomm'
    AND referenced_table_name IS NOT NULL;

-- 列出所有外键约束
SELECT
    '外键约束列表' AS '验证项目',
    CONSTRAINT_NAME AS '约束名称',
    TABLE_NAME AS '表名',
    COLUMN_NAME AS '字段名',
    REFERENCED_TABLE_NAME AS '引用表名',
    REFERENCED_COLUMN_NAME AS '引用字段名'
FROM information_schema.key_column_usage
WHERE
    table_schema = 'feduwacomm'
    AND referenced_table_name IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

-- 验证外键约束的删除行为
SELECT
    '外键删除行为验证' AS '验证项目',
    CONSTRAINT_NAME AS '约束名称',
    DELETE_RULE AS '删除行为',
    CASE
        WHEN DELETE_RULE = 'CASCADE' THEN '级联删除'
        WHEN DELETE_RULE = 'SET NULL' THEN '设为NULL'
        WHEN DELETE_RULE = 'RESTRICT' THEN '阻止删除'
        WHEN DELETE_RULE = 'NO ACTION' THEN '无动作'
        ELSE DELETE_RULE
    END AS '删除行为说明'
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE
    CONSTRAINT_SCHEMA = 'feduwacomm'
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

-- 验证system_logs表的timestamp字段默认值
SELECT
    'timestamp默认值验证' AS '验证项目',
    COLUMN_DEFAULT AS '默认值',
    'CURRENT_TIMESTAMP' AS '期望默认值',
    CASE
        WHEN COLUMN_DEFAULT = 'CURRENT_TIMESTAMP' THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND TABLE_NAME = 'system_logs'
    AND COLUMN_NAME = 'timestamp';

-- 验证字符集
SELECT
    '字符集验证' AS '验证项目',
    default_character_set_name AS '数据库字符集',
    'utf8mb4' AS '期望字符集',
    CASE
        WHEN default_character_set_name = 'utf8mb4' THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.schemata
WHERE
    schema_name = 'feduwacomm';

-- 验证排序规则
SELECT
    '排序规则验证' AS '验证项目',
    default_collation_name AS '数据库排序规则',
    'utf8mb4_unicode_ci' AS '期望排序规则',
    CASE
        WHEN default_collation_name = 'utf8mb4_unicode_ci' THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.schemata
WHERE
    schema_name = 'feduwacomm';

-- 验证UUID格式（检查表结构中的ID字段类型）
SELECT
    'UUID字段类型验证' AS '验证项目',
    COUNT(*) AS 'VARCHAR(32)字段数',
    '= 11' AS '期望字段数',
    CASE
        WHEN COUNT(*) = 11 THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND COLUMN_NAME = 'id'
    AND DATA_TYPE = 'varchar'
    AND CHARACTER_MAXIMUM_LENGTH = 32;

-- =====================================================
-- 验证完成
-- =====================================================
-- 如果所有验证项目都显示 "✓ 通过"，则数据库初始化成功
-- 如果有任何项目显示 "✗ 失败"，请检查初始化脚本执行情况

-- 验证总结
SELECT
    '验证总结' AS '项目',
    '数据库初始化验证完成' AS '状态',
    CASE
        WHEN (
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE
                table_schema = 'feduwacomm'
                AND table_type = 'BASE TABLE'
        ) = 11
        AND (
            SELECT COUNT(*)
            FROM information_schema.key_column_usage
            WHERE
                table_schema = 'feduwacomm'
                AND referenced_table_name IS NOT NULL
        ) = 12
        AND (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE
                TABLE_SCHEMA = 'feduwacomm'
                AND COLUMN_NAME = 'id'
                AND DATA_TYPE = 'varchar'
                AND CHARACTER_MAXIMUM_LENGTH = 32
        ) = 11
        AND (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE
                TABLE_SCHEMA = 'feduwacomm'
                AND DATA_TYPE = 'json'
        ) >= 8 THEN '✓ 所有验证通过'
        ELSE '✗ 部分验证失败'
    END AS '结果';

-- 验证表结构详细信息（新增）
SELECT
    '表结构详细验证' AS '验证项目',
    table_name AS '表名',
    CASE
        WHEN table_name IN (
            'users', 'user_permissions', 'vm_instances', 'federated_tasks',
            'training_dataset', 'training_dataset_row', 'model_versions',
            'system_logs', 'vm_runtime_logs', 'vm_round_models', 'vm_secrets'
        ) THEN '✓ 表存在'
        ELSE '✗ 意外表'
    END AS '验证结果'
FROM information_schema.tables
WHERE
    table_schema = 'feduwacomm'
    AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- 验证JSON字段（新增）
SELECT
    'JSON字段验证' AS '验证项目',
    COUNT(*) AS 'JSON字段数量',
    '>= 8' AS '期望数量',
    CASE
        WHEN COUNT(*) >= 8 THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND DATA_TYPE = 'json';

-- 验证ENUM字段（新增）
SELECT
    'ENUM字段验证' AS '验证项目',
    COUNT(*) AS 'ENUM字段数量',
    '>= 10' AS '期望数量',
    CASE
        WHEN COUNT(*) >= 10 THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND DATA_TYPE = 'enum';

-- =====================================================
-- 数据库结构摘要
-- =====================================================
-- 表数量: 11个
-- 外键约束: 12个
-- JSON字段: 8+个
-- ENUM字段: 10+个
-- 索引: 30+个
-- UUID字段(id): 11个
-- =====================================================