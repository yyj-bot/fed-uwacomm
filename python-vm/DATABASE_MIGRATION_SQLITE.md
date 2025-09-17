# 数据库迁移：从MySQL到SQLite

## 概述

数据库系统已成功从MySQL迁移到SQLite，以简化部署和管理。

## 主要变更

### 1. 依赖变更
- **之前**: 使用 `pymysql` 连接MySQL数据库
- **现在**: 使用内置的 `sqlite3` 模块

### 2. 配置变更
- **之前**: 需要配置主机、端口、用户名、密码
- **现在**: 只需要配置数据库文件路径

### 3. 配置示例

#### SQLite配置 (.env文件)
```env
# SQLite数据库文件路径
DB_PATH=data/bellhop_data.db

# 表名
DB_TABLE=features
```

#### 默认配置
如果没有配置文件，系统将使用以下默认值：
- 数据库文件: `bellhop_data.db`
- 表名: `features`

### 4. SQL语法适配
- **数据类型**: MySQL的`INT`、`FLOAT`、`VARCHAR`等转换为SQLite的`INTEGER`、`REAL`、`TEXT`
- **自增主键**: `AUTO_INCREMENT` 改为 `AUTOINCREMENT`
- **占位符**: `%s` 改为 `?`
- **表结构查询**: `DESCRIBE` 改为 `PRAGMA table_info()`

## 优势

1. **简化部署**: 无需安装和配置MySQL服务器
2. **零配置**: SQLite是文件数据库，无需额外设置
3. **便携性**: 数据库文件可以轻松复制和备份
4. **性能**: 对于单用户应用，SQLite性能优异
5. **兼容性**: 保持所有原有功能不变

## 使用方法

### 基本用法
```python
from feduwacomm.database.database import DatabaseManager

# 创建数据库管理器
db = DatabaseManager()

# 连接数据库
if db.connect():
    print("连接成功")
    
    # 从CSV创建表并导入数据
    db.create_and_populate_from_csv('data.csv')
    
    # 获取训练数据
    df = db.get_features_for_training()
    
    # 断开连接
    db.disconnect()
```

### 上下文管理器用法
```python
with DatabaseManager() as db:
    df = db.get_features_for_training()
    print(f"获取了 {len(df)} 条记录")
```

## 测试结果

✅ **数据库连接**: 正常  
✅ **表创建**: 正常  
✅ **CSV导入**: 成功导入300行54列数据  
✅ **数据查询**: 正常  
✅ **向后兼容**: 所有现有脚本正常工作  

## 数据库文件

- **文件名**: `bellhop_data.db`
- **位置**: `python-vm/` 目录
- **大小**: ~150KB (包含300条记录)
- **格式**: SQLite 3.x

## 注意事项

1. SQLite是文件数据库，确保有足够的磁盘空间
2. 对于并发写入有限制，但对于ML训练应用来说完全足够
3. 数据库文件可以使用SQLite客户端工具查看和管理

## 迁移完成时间

2025年9月17日 - 所有功能已验证正常工作
