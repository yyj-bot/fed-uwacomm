-- =====================================================
-- 日志管理系统性能优化索引
-- 建议在生产环境中执行以下索引创建语句
-- =====================================================

-- 主要查询索引：时间戳（最常用的过滤条件）
CREATE INDEX idx_system_logs_timestamp ON system_logs(timestamp DESC);

-- 复合索引：时间范围 + 级别（常用组合查询）
CREATE INDEX idx_system_logs_timestamp_level ON system_logs(timestamp DESC, level);

-- 复合索引：时间范围 + 类别（logger字段前缀匹配优化）
CREATE INDEX idx_system_logs_timestamp_logger ON system_logs(timestamp DESC, logger);

-- 虚拟机ID索引（高选择性字段）
CREATE INDEX idx_system_logs_vm_id ON system_logs(vm_id);

-- 任务ID索引（高选择性字段）
CREATE INDEX idx_system_logs_task_id ON system_logs(task_id);

-- 用户ID索引（用于用户相关日志查询）
CREATE INDEX idx_system_logs_user_id ON system_logs(user_id);

-- 复合索引：VM + 时间（虚拟机日志查询优化）
CREATE INDEX idx_system_logs_vm_timestamp ON system_logs(vm_id, timestamp DESC);

-- 复合索引：任务 + 时间（任务日志查询优化）
CREATE INDEX idx_system_logs_task_timestamp ON system_logs(task_id, timestamp DESC);

-- 级别索引（错误日志查询）
CREATE INDEX idx_system_logs_level ON system_logs(level);

-- 环境索引（多环境部署时使用）
CREATE INDEX idx_system_logs_environment ON system_logs(environment);

-- 全文搜索索引（MySQL 5.6+支持，用于消息内容搜索）
-- 注意：只有在需要高性能全文搜索时才创建，会增加存储开销
-- CREATE FULLTEXT INDEX idx_system_logs_message_fulltext ON system_logs(message, details);

-- =====================================================
-- 虚拟机运行日志表索引（如果存在）
-- =====================================================

-- 假设vm_runtime_logs表结构类似，创建相应索引
-- CREATE INDEX idx_vm_logs_timestamp ON vm_runtime_logs(timestamp DESC);
-- CREATE INDEX idx_vm_logs_vm_id ON vm_runtime_logs(vm_id);
-- CREATE INDEX idx_vm_logs_vm_timestamp ON vm_runtime_logs(vm_id, timestamp DESC);

-- =====================================================
-- 导出任务表索引
-- =====================================================

-- 导出任务状态查询
-- CREATE INDEX idx_export_task_status ON log_export_tasks(status);
-- CREATE INDEX idx_export_task_created ON log_export_tasks(created_at DESC);

-- =====================================================
-- 清理任务表索引
-- =====================================================

-- 清理任务状态查询
-- CREATE INDEX idx_cleanup_task_status ON log_cleanup_tasks(status);
-- CREATE INDEX idx_cleanup_task_created ON log_cleanup_tasks(created_at DESC);

-- =====================================================
-- 索引使用说明和性能建议
-- =====================================================

/*
索引使用优先级和场景：

1. idx_system_logs_timestamp (最重要)
   - 适用于：按时间范围查询日志
   - 查询模式：WHERE timestamp BETWEEN ? AND ?
   - 使用频率：极高

2. idx_system_logs_timestamp_level (高优先级)
   - 适用于：按时间+级别查询（如查找错误日志）
   - 查询模式：WHERE timestamp >= ? AND level = 'ERROR'
   - 使用频率：高

3. idx_system_logs_vm_id (高优先级)
   - 适用于：查询特定虚拟机的日志
   - 查询模式：WHERE vm_id = ?
   - 使用频率：高

4. idx_system_logs_task_id (高优先级)
   - 适用于：查询特定任务的日志
   - 查询模式：WHERE task_id = ?
   - 使用频率：高

性能优化建议：

1. 分区表策略（大数据量时）：
   - 按月或按周分区：PARTITION BY RANGE (TO_DAYS(timestamp))
   - 可以显著提升查询性能和维护效率

2. 定期维护：
   - 定期清理过期数据以保持索引效率
   - 定期重建统计信息：ANALYZE TABLE system_logs

3. 查询优化：
   - 始终在查询中包含时间范围条件
   - 避免全表扫描，特别是在生产环境中
   - 使用EXPLAIN分析查询计划

4. 存储优化：
   - 考虑将details字段存储为JSON类型（MySQL 5.7+）
   - 对于归档数据，可以考虑压缩存储

5. 监控指标：
   - 监控慢查询日志
   - 定期检查索引使用率
   - 监控表空间增长情况
*/