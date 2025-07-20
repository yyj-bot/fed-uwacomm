# 数据库设计文档

## 📋 概述

FedUWAComm系统使用MySQL数据库存储BELLHOP水声仿真的特征数据。数据库设计遵循规范化原则，支持高效的数据存储和查询。

## 🗄️ 数据库配置

### 基本信息
- **数据库类型**: MySQL 5.7+
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci
- **存储引擎**: InnoDB

### 连接配置
```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=bellhop_data
DB_TABLE=features
```

## 📊 表结构设计

### 主要数据表：features

#### 表信息
- **表名**: `features`
- **描述**: 存储BELLHOP水声仿真特征数据
- **主键**: `id` (自增)

#### 字段设计

| 字段名 | 类型 | 长度 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `id` | INT | - | AUTO_INCREMENT | 主键，自增ID |
| `filename` | VARCHAR | 128 | - | 文件名（如B01） |
| `success` | TINYINT | - | 0 | 仿真是否成功（0失败，1成功） |
| `freq` | FLOAT | - | 0.0 | 频率（Hz） |
| `ssp_points` | INT | - | 0 | 声速剖面点数 |
| `water_depth` | FLOAT | - | 0.0 | 水深（米） |
| `src_depth` | FLOAT | - | 0.0 | 声源深度（米） |
| `rcv_depth` | FLOAT | - | 0.0 | 接收器深度（米） |
| `max_range` | FLOAT | - | 0.0 | 最大通信距离（km） |
| `ocean_type` | VARCHAR | 32 | 'temperate' | 海洋类型（tropical/temperate/arctic） |
| `bottom_type` | VARCHAR | 32 | 'sand' | 海底类型（sand/silt/clay/rock/gravel） |
| `bottom_speed` | FLOAT | - | 0.0 | 海底声速（m/s） |
| `bottom_density` | FLOAT | - | 0.0 | 海底密度（g/cm³） |
| `bottom_atten` | FLOAT | - | 0.0 | 海底衰减（dB/λ） |
| `created_at` | TIMESTAMP | - | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | - | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

#### 声速剖面字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `ssp_depth_01` ~ `ssp_depth_25` | FLOAT | 第1-25个深度点的深度值（米） |
| `ssp_speed_01` ~ `ssp_speed_25` | FLOAT | 第1-25个深度点的声速值（m/s） |

#### 传播损失字段（预留）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `tl_min` | FLOAT | 最小传播损失（dB） |
| `tl_max` | FLOAT | 最大传播损失（dB） |
| `tl_mean` | FLOAT | 平均传播损失（dB） |
| `tl_std` | FLOAT | 传播损失标准差（dB） |
| `tl_range_01` ~ `tl_range_10` | FLOAT | 不同距离的传播损失值 |

#### 射线路径字段（预留）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `ray_count` | INT | 射线数量 |
| `ray_max_depth` | FLOAT | 射线最大深度（米） |
| `ray_min_depth` | FLOAT | 射线最小深度（米） |
| `ray_avg_length` | FLOAT | 射线平均长度（米） |
| `ray_surface_reflections` | INT | 表面反射次数 |
| `ray_bottom_reflections` | INT | 底部反射次数 |

## 🔧 数据库操作

### 创建数据库

```sql
-- 创建数据库
CREATE DATABASE bellhop_data 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE bellhop_data;
```

### 创建表

```sql
-- 创建特征表
CREATE TABLE features (
    id INT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(128) NOT NULL,
    success TINYINT DEFAULT 0,
    freq FLOAT DEFAULT 0.0,
    ssp_points INT DEFAULT 0,
    water_depth FLOAT DEFAULT 0.0,
    src_depth FLOAT DEFAULT 0.0,
    rcv_depth FLOAT DEFAULT 0.0,
    max_range FLOAT DEFAULT 0.0,
    ocean_type VARCHAR(32) DEFAULT 'temperate',
    bottom_type VARCHAR(32) DEFAULT 'sand',
    bottom_speed FLOAT DEFAULT 0.0,
    bottom_density FLOAT DEFAULT 0.0,
    bottom_atten FLOAT DEFAULT 0.0,
    
    -- 声速剖面字段
    ssp_depth_01 FLOAT DEFAULT 0.0,
    ssp_speed_01 FLOAT DEFAULT 0.0,
    ssp_depth_02 FLOAT DEFAULT 0.0,
    ssp_speed_02 FLOAT DEFAULT 0.0,
    -- ... 继续到 ssp_depth_25, ssp_speed_25
    
    -- 传播损失字段（预留）
    tl_min FLOAT DEFAULT 0.0,
    tl_max FLOAT DEFAULT 0.0,
    tl_mean FLOAT DEFAULT 0.0,
    tl_std FLOAT DEFAULT 0.0,
    
    -- 射线路径字段（预留）
    ray_count INT DEFAULT 0,
    ray_max_depth FLOAT DEFAULT 0.0,
    ray_min_depth FLOAT DEFAULT 0.0,
    ray_avg_length FLOAT DEFAULT 0.0,
    ray_surface_reflections INT DEFAULT 0,
    ray_bottom_reflections INT DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_filename (filename),
    INDEX idx_success (success),
    INDEX idx_freq (freq),
    INDEX idx_ocean_type (ocean_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 索引设计

#### 主要索引
- **主键索引**: `id` (自动创建)
- **文件名索引**: `filename` - 用于快速查找特定文件
- **成功状态索引**: `success` - 用于筛选成功/失败的仿真
- **频率索引**: `freq` - 用于按频率范围查询
- **海洋类型索引**: `ocean_type` - 用于按海洋环境分类
- **创建时间索引**: `created_at` - 用于时间范围查询

#### 复合索引（可选）
```sql
-- 文件名和成功状态的复合索引
CREATE INDEX idx_filename_success ON features(filename, success);

-- 频率和海洋类型的复合索引
CREATE INDEX idx_freq_ocean_type ON features(freq, ocean_type);
```

## 📈 数据查询示例

### 基础查询

```sql
-- 查询所有成功的仿真
SELECT * FROM features WHERE success = 1;

-- 查询特定频率范围的数据
SELECT * FROM features WHERE freq BETWEEN 1000 AND 5000;

-- 查询特定海洋类型的数据
SELECT * FROM features WHERE ocean_type = 'tropical';

-- 统计各海洋类型的数据量
SELECT ocean_type, COUNT(*) as count 
FROM features 
GROUP BY ocean_type;
```

### 特征分析查询

```sql
-- 查询声速剖面数据
SELECT filename, freq, 
       ssp_depth_01, ssp_speed_01,
       ssp_depth_02, ssp_speed_02,
       ssp_depth_03, ssp_speed_03
FROM features 
WHERE success = 1 
LIMIT 10;

-- 计算平均声速
SELECT filename, 
       AVG(ssp_speed_01) as avg_speed_01,
       AVG(ssp_speed_02) as avg_speed_02,
       AVG(ssp_speed_03) as avg_speed_03
FROM features 
WHERE success = 1 
GROUP BY filename;
```

### 性能统计查询

```sql
-- 仿真成功率统计
SELECT 
    COUNT(*) as total_simulations,
    SUM(success) as successful_simulations,
    ROUND(SUM(success) * 100.0 / COUNT(*), 2) as success_rate
FROM features;

-- 按频率统计成功率
SELECT 
    CASE 
        WHEN freq < 1000 THEN 'Low Frequency (<1kHz)'
        WHEN freq < 10000 THEN 'Medium Frequency (1-10kHz)'
        ELSE 'High Frequency (>10kHz)'
    END as freq_range,
    COUNT(*) as total,
    SUM(success) as successful,
    ROUND(SUM(success) * 100.0 / COUNT(*), 2) as success_rate
FROM features 
GROUP BY freq_range;
```

## 🔄 数据维护

### 数据清理

```sql
-- 删除失败的仿真记录
DELETE FROM features WHERE success = 0;

-- 删除重复的文件记录（保留最新的）
DELETE f1 FROM features f1
INNER JOIN features f2 
WHERE f1.id < f2.id AND f1.filename = f2.filename;
```

### 数据备份

```sql
-- 创建备份表
CREATE TABLE features_backup AS SELECT * FROM features;

-- 导出数据
mysqldump -u username -p bellhop_data features > features_backup.sql
```

### 性能优化

```sql
-- 分析表结构
ANALYZE TABLE features;

-- 优化表
OPTIMIZE TABLE features;

-- 查看表状态
SHOW TABLE STATUS LIKE 'features';
```

## 🛡️ 数据安全

### 访问控制

```sql
-- 创建专用用户
CREATE USER 'bellhop_user'@'localhost' IDENTIFIED BY 'secure_password';

-- 授予必要权限
GRANT SELECT, INSERT, UPDATE, DELETE ON bellhop_data.* TO 'bellhop_user'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;
```

### 数据验证

```sql
-- 检查数据完整性
SELECT COUNT(*) as total_records,
       COUNT(CASE WHEN success = 1 THEN 1 END) as successful_records,
       COUNT(CASE WHEN freq > 0 THEN 1 END) as valid_frequency_records
FROM features;
```

## 📊 数据统计

### 数据量统计
- **总记录数**: 约15,000条（15个文件 × 1000次仿真）
- **成功记录**: 约12,000条（80%成功率）
- **数据大小**: 约50MB

### 字段分布
- **频率范围**: 50Hz - 15kHz
- **水深范围**: 1000m - 5000m
- **声速范围**: 1430m/s - 1550m/s
- **海洋类型**: tropical, temperate, arctic
- **海底类型**: sand, silt, clay, rock, gravel

## 🔮 未来扩展

### 预留字段
- 传播损失相关字段（tl_*）
- 射线路径相关字段（ray_*）
- 模型预测结果字段（pred_*）
- 联邦学习参数字段（fl_*）

### 新表设计
- **simulation_logs**: 仿真日志表
- **model_performance**: 模型性能表
- **federated_learning**: 联邦学习记录表
- **communication_optimization**: 通信优化结果表 