# FedUWAComm 数据库使用指南

## ✅ 数据库状态

您的数据库已经成功配置并填充了BELLHOP特征数据：

- **数据库**: bellhop_data
- **表**: features  
- **记录数**: 6条（B01-B06环境）
- **特征数**: 103个特征列
- **数据完整性**: 85.44%
- **ML就绪特征**: 63个数值特征

## 🚀 如何运行

### 1. 快速验证数据库状态
```bash
python verify_database.py
```

### 2. 运行完整的数据库ML工作流程
```bash
# 使用现有数据库数据训练模型
python database_ml_workflow.py

# 强制重新提取特征并存入数据库
python database_ml_workflow.py --force-reextract
```

### 3. 运行演示版本（推荐用于小数据集）
```bash
python demo_ml_workflow.py
```

## 📊 数据库中的数据

### 环境信息
- **B01**: freq=1000Hz, arrivals=54659
- **B02**: freq=2000Hz, arrivals=336186  
- **B03**: freq=1500Hz, arrivals=116204
- **B04**: freq=800Hz, arrivals=90841
- **B05**: freq=3000Hz, arrivals=158765
- **B06**: freq=1200Hz, arrivals=88942

### 主要特征类型
- `env_*` - 环境文件特征
- `prt_*` - 声速剖面和深度特征
- `arr_*` - 到达时间特征
- `ray_*` - 射线路径特征
- `shd_*` - 传播损失特征

## 💻 编程方式访问数据库

### 基本用法
```python
from ml_modules.database_v2 import DatabaseManagerV2

# 连接数据库
db = DatabaseManagerV2()
if db.connect():
    # 获取所有特征数据
    df = db.get_features_for_training()
    print(f"获取到 {len(df)} 条记录，{len(df.columns)} 个特征")
    
    # 关闭连接
    db.disconnect()
```

### 获取特定环境数据
```python
# 获取指定环境的数据
env_ids = ['B01', 'B02', 'B03']
df = db.get_features_for_training(env_ids)
```

### 获取数值特征用于ML
```python
import pandas as pd
import numpy as np

# 获取数据
df = db.get_features_for_training()

# 选择数值特征
numeric_cols = df.select_dtypes(include=[np.number]).columns
exclude_cols = ['id', 'timestamp']
ml_features = [col for col in numeric_cols if col not in exclude_cols]

X = df[ml_features].fillna(0)  # 填充缺失值
print(f"ML特征矩阵: {X.shape}")
```

## 🔧 数据库管理

### 重新构建数据库表
```python
from ml_modules.database_v2 import DatabaseManagerV2

db = DatabaseManagerV2()
if db.connect():
    # 从CSV文件重新创建和填充表
    csv_file = "results/features/bellhop_features_latest.csv"
    success = db.create_and_populate_from_csv(csv_file)
    db.disconnect()
```

### 查看表结构
```python
db = DatabaseManagerV2()
if db.connect():
    info = db.get_table_info()
    print(f"表有 {len(info['columns'])} 列")
    print(f"包含 {info['row_count']} 条记录")
    db.disconnect()
```

## 📈 机器学习工作流程

### 1. 从数据库获取数据
```python
from ml_modules.database_v2 import DatabaseManagerV2
from ml_modules.random_forest_trainer import RandomForestTrainer

# 获取训练数据
db = DatabaseManagerV2()
if db.connect():
    df = db.get_features_for_training()
    db.disconnect()

# 准备特征和目标
trainer = RandomForestTrainer()
X, _ = trainer.prepare_data(df)
y = trainer.create_synthetic_targets(X)
```

### 2. 训练模型
```python
# 训练模型
results = trainer.train_multiple_targets(X, y)
trainer.save_models("_from_database")
```

### 3. 评估和预测
```python
from ml_modules.model_evaluator import ModelEvaluator

evaluator = ModelEvaluator()
# 进行模型评估和预测...
```

## ⚠️ 注意事项

### 数据量限制
- 当前只有6个环境样本，适合演示和测试
- 对于生产环境，建议至少有50+个样本
- 可以使用数据增强技术扩展训练数据

### 特征说明
- 部分SHD特征提取失败（文件解析问题），已用默认值填充
- 数据完整性85.44%，足够进行基础ML训练
- 主要特征（频率、到达时间、射线路径）完整可用

### 性能优化
- 数据库连接使用连接池可以提高性能
- 对于大数据集，考虑批量查询和处理
- 可以添加更多索引来优化查询速度

## 📝 常见问题

**Q: 如何添加新的环境数据？**
A: 运行 `python database_ml_workflow.py --force-reextract` 会自动检测新的BELLHOP文件并更新数据库。

**Q: 数据库连接失败怎么办？**
A: 检查 `.env` 文件中的数据库配置，确保MySQL服务运行正常。

**Q: 如何备份数据库数据？**
A: 数据会自动备份为CSV文件在 `results/features/` 目录下。

**Q: 如何扩展更多特征？**
A: 修改 `ml_modules/feature_extractor.py` 中的特征提取逻辑，然后重新运行工作流程。

---

## 🎯 下一步建议

1. **数据扩展**: 收集更多BELLHOP仿真环境数据
2. **特征工程**: 添加更多领域相关的派生特征
3. **模型优化**: 尝试不同的机器学习算法
4. **联邦学习**: 实现分布式训练框架
5. **可视化**: 开发Web界面展示结果

您的数据库现在已经完全可用，可以支持各种机器学习实验和应用开发！