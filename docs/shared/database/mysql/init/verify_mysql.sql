-- =====================================================
-- 水声联邦学习系统 数据库验证脚本
-- =====================================================
-- 作者: FedUWAComm Team
-- 版本: 1.1.0 (更新以支持日志管理表)
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
    14 AS '期望表数量',
    CASE
        WHEN COUNT(*) = 14 THEN '✓ 通过'
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
    '>= 40' AS '期望索引数量',
    CASE
        WHEN COUNT(*) >= 40 THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.statistics
WHERE
    table_schema = 'feduwacomm';

-- 验证外键约束是否存在
SELECT
    '外键验证' AS '验证项目',
    COUNT(*) AS '外键数量',
    '= 13' AS '期望外键数量(日志表无外键)',
    CASE
        WHEN COUNT(*) = 13 THEN '✓ 通过'
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
    '= 14' AS '期望字段数',
    CASE
        WHEN COUNT(*) = 14 THEN '✓ 通过'
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
        ) = 14
        AND (
            SELECT COUNT(*)
            FROM information_schema.key_column_usage
            WHERE
                table_schema = 'feduwacomm'
                AND referenced_table_name IS NOT NULL
        ) = 13
        AND (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE
                TABLE_SCHEMA = 'feduwacomm'
                AND COLUMN_NAME = 'id'
                AND DATA_TYPE = 'varchar'
                AND CHARACTER_MAXIMUM_LENGTH = 32
        ) = 14
        AND (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE
                TABLE_SCHEMA = 'feduwacomm'
                AND DATA_TYPE = 'json'
        ) >= 10 THEN '✓ 所有验证通过'
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
            'system_logs', 'vm_runtime_logs', 'vm_round_models', 'vm_secrets',
            'global_models', 'log_export_tasks', 'log_cleanup_tasks'
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
    '>= 10' AS '期望数量',
    CASE
        WHEN COUNT(*) >= 10 THEN '✓ 通过'
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
    '>= 14' AS '期望数量',
    CASE
        WHEN COUNT(*) >= 14 THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND DATA_TYPE = 'enum';

-- =====================================================
-- 新增验证global_models表相关功能
-- =====================================================

-- 验证global_models表结构
SELECT
    'global_models表验证' AS '验证项目',
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.tables 
            WHERE table_schema = 'feduwacomm' AND table_name = 'global_models'
        ) THEN '✓ 表存在'
        ELSE '✗ 表不存在'
    END AS '结果';

-- 验证global_models表字段
SELECT
    'global_models字段验证' AS '验证项目',
    COUNT(*) AS '字段数量',
    '>= 14' AS '期望字段数',
    CASE
        WHEN COUNT(*) >= 14 THEN '✓ 通过'
        ELSE '✗ 失败'
    END AS '结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND TABLE_NAME = 'global_models';

-- 验证聚合历史视图
SELECT
    '聚合历史视图验证' AS '验证项目',
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.views 
            WHERE table_schema = 'feduwacomm' AND table_name = 'v_aggregation_history'
        ) THEN '✓ 视图存在'
        ELSE '✗ 视图不存在'
    END AS '结果';

-- 验证global_models表状态枚举
SELECT
    'global_models状态枚举验证' AS '验证项目',
    COLUMN_TYPE AS '状态值',
    CASE
        WHEN COLUMN_TYPE LIKE '%PENDING%' 
         AND COLUMN_TYPE LIKE '%AGGREGATING%'
         AND COLUMN_TYPE LIKE '%COMPLETED%'
         AND COLUMN_TYPE LIKE '%FAILED%'
        THEN '✓ 状态枚举完整'
        ELSE '✗ 状态枚举不完整'
    END AS '结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND TABLE_NAME = 'global_models'
    AND COLUMN_NAME = 'status';

-- =====================================================
-- 新增日志管理表验证
-- =====================================================

-- 验证system_logs表扩展字段
SELECT
    'system_logs扩展字段验证' AS '验证项目',
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = 'feduwacomm' 
            AND TABLE_NAME = 'system_logs' 
            AND COLUMN_NAME = 'vm_id'
        ) 
        AND EXISTS (
            SELECT 1 FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = 'feduwacomm' 
            AND TABLE_NAME = 'system_logs' 
            AND COLUMN_NAME = 'task_id'
        )
        AND EXISTS (
            SELECT 1 FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = 'feduwacomm' 
            AND TABLE_NAME = 'system_logs' 
            AND COLUMN_NAME = 'details'
        )
        THEN '✓ 扩展字段存在'
        ELSE '✗ 扩展字段缺失'
    END AS '结果';

-- 验证log_export_tasks表
SELECT
    'log_export_tasks表验证' AS '验证项目',
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.tables 
            WHERE table_schema = 'feduwacomm' AND table_name = 'log_export_tasks'
        ) THEN '✓ 表存在'
        ELSE '✗ 表不存在'
    END AS '结果';

-- 验证log_cleanup_tasks表
SELECT
    'log_cleanup_tasks表验证' AS '验证项目',
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.tables 
            WHERE table_schema = 'feduwacomm' AND table_name = 'log_cleanup_tasks'
        ) THEN '✓ 表存在'
        ELSE '✗ 表不存在'
    END AS '结果';

-- 验证日志导出任务状态枚举
SELECT
    'log_export_tasks状态枚举验证' AS '验证项目',
    COLUMN_TYPE AS '状态值',
    CASE
        WHEN COLUMN_TYPE LIKE '%PENDING%' 
         AND COLUMN_TYPE LIKE '%PROCESSING%'
         AND COLUMN_TYPE LIKE '%COMPLETED%'
         AND COLUMN_TYPE LIKE '%FAILED%'
        THEN '✓ 状态枚举完整'
        ELSE '✗ 状态枚举不完整'
    END AS '结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND TABLE_NAME = 'log_export_tasks'
    AND COLUMN_NAME = 'status';

-- 验证日志清理任务策略枚举
SELECT
    'log_cleanup_tasks策略枚举验证' AS '验证项目',
    COLUMN_TYPE AS '策略值',
    CASE
        WHEN COLUMN_TYPE LIKE '%TIME_BASED%' 
         AND COLUMN_TYPE LIKE '%LEVEL_BASED%'
         AND COLUMN_TYPE LIKE '%SIZE_BASED%'
         AND COLUMN_TYPE LIKE '%CATEGORY_BASED%'
        THEN '✓ 策略枚举完整'
        ELSE '✗ 策略枚举不完整'
    END AS '结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND TABLE_NAME = 'log_cleanup_tasks'
    AND COLUMN_NAME = 'strategy';

-- 验证日志导出格式枚举
SELECT
    'log_export_tasks格式枚举验证' AS '验证项目',
    COLUMN_TYPE AS '格式值',
    CASE
        WHEN COLUMN_TYPE LIKE '%CSV%' 
         AND COLUMN_TYPE LIKE '%JSON%'
         AND COLUMN_TYPE LIKE '%EXCEL%'
        THEN '✓ 格式枚举完整'
        ELSE '✗ 格式枚举不完整'
    END AS '结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND TABLE_NAME = 'log_export_tasks'
    AND COLUMN_NAME = 'format';

-- 验证日志清理事件调度器
SELECT
    '日志清理事件验证' AS '验证项目',
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.EVENTS 
            WHERE EVENT_SCHEMA = 'feduwacomm' 
            AND EVENT_NAME = 'cleanup_expired_export_tasks'
        ) THEN '✓ 清理事件存在'
        ELSE '✗ 清理事件不存在'
    END AS '结果';

-- 验证日志表字段完整性
SELECT
    '日志表字段完整性验证' AS '验证项目',
    table_name AS '表名',
    COUNT(*) AS '字段数量',
    CASE
        WHEN table_name = 'system_logs' AND COUNT(*) >= 16 THEN '✓ 字段完整'
        WHEN table_name = 'vm_runtime_logs' AND COUNT(*) >= 7 THEN '✓ 字段完整'
        WHEN table_name = 'log_export_tasks' AND COUNT(*) >= 20 THEN '✓ 字段完整'
        WHEN table_name = 'log_cleanup_tasks' AND COUNT(*) >= 15 THEN '✓ 字段完整'
        ELSE '✗ 字段不完整'
    END AS '验证结果'
FROM information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'feduwacomm'
    AND TABLE_NAME IN ('system_logs', 'vm_runtime_logs', 'log_export_tasks', 'log_cleanup_tasks')
GROUP BY table_name
ORDER BY table_name;

-- =====================================================
-- 数据库结构摘要
-- =====================================================
-- 表数量: 14个 (新增log_export_tasks, log_cleanup_tasks)
-- 外键约束: 13个 (日志表无外键约束)
-- JSON字段: 12+个 (新增filter_conditions, cleanup_conditions等)
-- ENUM字段: 17+个 (新增日志任务状态、策略等枚举)
-- 索引: 40+个 (新增日志管理表相关索引)
-- UUID字段(id): 14个
-- 视图: 1个 (v_aggregation_history)
-- 事件调度器: 1个 (cleanup_expired_export_tasks)
-- =====================================================

-- 最终验证摘要
SELECT
    '最终验证摘要' AS '验证类型',
    '日志管理数据库结构验证' AS '验证内容',
    CASE
        WHEN (
            -- 验证表数量
            SELECT COUNT(*) FROM information_schema.tables 
            WHERE table_schema = 'feduwacomm' AND table_type = 'BASE TABLE'
        ) = 14
        AND (
            -- 验证日志管理表存在
            SELECT COUNT(*) FROM information_schema.tables 
            WHERE table_schema = 'feduwacomm' 
            AND table_name IN ('log_export_tasks', 'log_cleanup_tasks')
        ) = 2
        AND (
            -- 验证system_logs扩展字段
            SELECT COUNT(*) FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = 'feduwacomm' AND TABLE_NAME = 'system_logs' 
            AND COLUMN_NAME IN ('vm_id', 'task_id', 'details')
        ) = 3
        AND (
            -- 验证JSON字段数量
            SELECT COUNT(*) FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = 'feduwacomm' AND DATA_TYPE = 'json'
        ) >= 10
        THEN '✓ 日志管理系统数据库结构完整'
        ELSE '✗ 日志管理系统数据库结构不完整'
    END AS '最终结果';