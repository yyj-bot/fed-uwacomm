# 数据库更新总结 - 联邦学习聚合功能

## 🎯 数据库更新目标

为支持联邦学习自动化聚合功能，在现有数据库基础上新增global_models表及相关视图，用于存储和管理聚合后的全局模型。

---

## 📊 数据库变更详情

### 新增表结构

#### 1. global_models表
```sql
CREATE TABLE IF NOT EXISTS global_models (
    id VARCHAR(32) PRIMARY KEY COMMENT '唯一标识(32位UUID)',
    task_id VARCHAR(32) NOT NULL COMMENT '任务ID(32位UUID)',
    round_number INT NOT NULL COMMENT '轮次编号',
    aggregation_method VARCHAR(50) NOT NULL COMMENT '聚合算法类型(FEDAVG, FEDPROX, FEDNOVA等)',
    global_parameters JSON COMMENT '全局模型参数(JSON格式)',
    global_loss DECIMAL(10, 8) COMMENT '全局损失值',
    global_accuracy DECIMAL(10, 8) COMMENT '全局准确率',
    participant_count INT NOT NULL COMMENT '参与聚合的客户端数量',
    aggregation_duration BIGINT COMMENT '聚合耗时(毫秒)',
    status ENUM('PENDING', 'AGGREGATING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '聚合状态',
    started_at TIMESTAMP COMMENT '聚合开始时间',
    completed_at TIMESTAMP COMMENT '聚合完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    metadata JSON COMMENT '聚合元数据(JSON格式)'
);
```

#### 2. 约束和索引
```sql
-- 主键和唯一约束
PRIMARY KEY (id)
UNIQUE KEY uk_global_models_task_round (task_id, round_number)

-- 外键约束
FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE

-- 索引
INDEX idx_global_models_task_round (task_id, round_number)
INDEX idx_global_models_status (status)  
INDEX idx_global_models_created_at (created_at)
INDEX idx_global_models_completed_at (completed_at)
INDEX idx_global_models_task_id (task_id)
INDEX idx_global_models_round_number (round_number)
INDEX idx_global_models_aggregation_method (aggregation_method)
```

### 新增视图

#### v_aggregation_history - 聚合历史分析视图
```sql
CREATE VIEW IF NOT EXISTS v_aggregation_history AS
SELECT 
    gm.id,
    gm.task_id,
    ft.name AS task_name,
    ft.algorithm,
    gm.round_number,
    gm.aggregation_method,
    gm.global_loss,
    gm.global_accuracy,
    gm.participant_count,
    gm.aggregation_duration,
    gm.status,
    gm.started_at,
    gm.completed_at,
    CASE 
        WHEN gm.aggregation_duration IS NULL THEN 'UNKNOWN'
        WHEN gm.aggregation_duration < 5000 THEN 'FAST'
        WHEN gm.aggregation_duration < 30000 THEN 'NORMAL'
        ELSE 'SLOW'
    END as performance_level,
    CASE
        WHEN gm.completed_at IS NOT NULL AND gm.started_at IS NOT NULL 
        THEN TIMESTAMPDIFF(SECOND, gm.started_at, gm.completed_at)
        ELSE NULL
    END as actual_duration_seconds
FROM global_models gm
LEFT JOIN federated_tasks ft ON gm.task_id = ft.id
WHERE gm.status = 'COMPLETED'
ORDER BY gm.task_id, gm.round_number DESC;
```

---

## 📈 数据库结构更新统计

### 更新前后对比
| 项目 | 更新前 | 更新后 | 增量 |
|------|--------|--------|------|
| 表数量 | 11 | 12 | +1 |
| 外键约束 | 12 | 13 | +1 |
| JSON字段 | 8+ | 10+ | +2 |
| ENUM字段 | 10+ | 11+ | +1 |
| 索引数量 | 30+ | 33+ | +3 |
| 视图数量 | 0 | 1 | +1 |

### 新增数据类型分析
- **JSON字段**: global_parameters, metadata (存储模型参数和聚合元数据)
- **ENUM字段**: status (聚合状态管理)
- **DECIMAL字段**: global_loss, global_accuracy (高精度数值存储)
- **BIGINT字段**: aggregation_duration (毫秒级时间计量)

---

## 🔧 更新的脚本文件

### 1. 初始化脚本
- **位置**: `/backend-springboot/feduwacomm-server/src/main/resources/db/init_mysql.sql`
- **位置**: `/docs/shared/database/mysql/init/init_mysql.sql`
- **变更**: 新增global_models表定义和v_aggregation_history视图

### 2. 验证脚本  
- **位置**: `/docs/shared/database/mysql/init/verify_mysql.sql`
- **变更**: 
  - 更新表数量验证 (11 → 12)
  - 更新外键数量验证 (12 → 13)
  - 更新UUID字段数量验证 (11 → 12)
  - 新增global_models表结构验证
  - 新增聚合历史视图验证
  - 新增状态枚举完整性验证

---

## 🚀 数据库重建步骤

### 方法1: 完全重建 (推荐)
```bash
# 1. 备份现有数据 (如有需要)
mysqldump -u root -p feduwacomm > feduwacomm_backup.sql

# 2. 删除现有数据库
mysql -u root -p -e "DROP DATABASE IF EXISTS feduwacomm;"

# 3. 执行新的初始化脚本
mysql -u root -p < /path/to/init_mysql.sql

# 4. 验证数据库结构
mysql -u root -p < /path/to/verify_mysql.sql
```

### 方法2: 增量更新 (保留数据)
```bash
# 仅添加新表和视图
mysql -u root -p feduwacomm << EOF
-- 添加global_models表
CREATE TABLE IF NOT EXISTS global_models (
    -- 表结构如上所示
);

-- 添加外键约束
ALTER TABLE global_models
ADD CONSTRAINT fk_global_models_task_id FOREIGN KEY (task_id) REFERENCES federated_tasks (id) ON DELETE CASCADE;

-- 添加索引
CREATE INDEX idx_global_models_task_id ON global_models (task_id);
-- 其他索引...

-- 添加聚合历史视图
CREATE VIEW IF NOT EXISTS v_aggregation_history AS
-- 视图定义如上所示
EOF
```

---

## ✅ 验证检查项

执行验证脚本后，确认以下项目都显示 "✓ 通过":

1. **数据库验证** - 确认使用feduwacomm数据库
2. **表结构验证** - 确认12个表存在
3. **外键验证** - 确认13个外键约束
4. **UUID字段验证** - 确认12个UUID字段  
5. **global_models表验证** - 确认新表存在
6. **global_models字段验证** - 确认字段数量≥14
7. **聚合历史视图验证** - 确认视图存在
8. **状态枚举验证** - 确认状态值完整
9. **JSON字段验证** - 确认JSON字段数量≥10
10. **ENUM字段验证** - 确认ENUM字段数量≥11

---

## 📋 业务影响评估

### 优势
- **完整性**: 聚合历史完整记录，支持性能分析
- **一致性**: 外键约束确保数据一致性  
- **扩展性**: JSON字段支持灵活的参数和元数据存储
- **性能**: 合理的索引设计支持高效查询

### 注意事项
- **存储空间**: JSON字段会增加存储空间需求
- **查询复杂度**: 视图查询涉及表连接，注意性能
- **并发控制**: 聚合过程中注意并发访问控制

---

## 🔄 后续维护

### 定期维护建议
1. **性能监控**: 定期检查v_aggregation_history视图查询性能
2. **存储监控**: 监控global_models表存储空间增长
3. **数据清理**: 根据业务需求制定历史数据清理策略
4. **索引优化**: 根据查询模式优化索引策略

### 备份策略
- **增量备份**: 重点关注global_models表的增量数据
- **结构备份**: 定期备份表结构和视图定义
- **性能基准**: 建立查询性能基准，定期对比分析

---

**数据库更新已准备就绪，支持完整的联邦学习聚合功能！** 🎉