# FedUWAComm 机器学习模块运行指南

## 快速开始

### 1. 环境检查
```bash
python check_ml_setup.py
```
确保所有依赖和配置都正确。

### 2. 运行机器学习工作流程

**方式一：演示版本（推荐）**
```bash
python demo_ml_workflow.py
```
- 适合小数据集
- 包含数据增强
- 生成完整报告和可视化

**方式二：完整版本**
```bash
python ml_workflow.py
```
- 使用数据库存储
- 支持大规模数据
- 更复杂的特征工程

**方式三：简化版本**
```bash
python simple_ml_workflow.py
```
- 只使用CSV文件
- 不依赖数据库
- 基础功能

### 3. 单独运行组件

**只提取特征：**
```bash
python -c "
import sys
sys.path.append('ml_modules')
from feature_extractor import BellhopFeatureExtractor
extractor = BellhopFeatureExtractor('data/bellhop')
df = extractor.batch_extract_features()
df.to_csv('features.csv', index=False)
print('特征提取完成')
"
```

**只训练模型：**
```bash
python -c "
import sys
sys.path.append('ml_modules')
from random_forest_trainer import RandomForestTrainer
import pandas as pd
df = pd.read_csv('features.csv')
trainer = RandomForestTrainer()
X, y = trainer.prepare_data(df)
y = trainer.create_synthetic_targets(X)
results = trainer.train_multiple_targets(X, y)
trainer.save_models()
print('模型训练完成')
"
```

## 输出文件说明

### results/ 目录结构
```
results/
├── reports/           # 分析报告
├── features/          # 特征数据
├── predictions/       # 预测结果
├── plots/            # 可视化图表
└── logs/             # 运行日志
```

### models/ 目录
```
models/
├── rf_*.joblib       # 随机森林模型
├── scaler_*.joblib   # 特征标准化器
└── encoders_*.joblib # 标签编码器
```

## 结果解读

### 预测指标含义
- **comm_quality**: 通信质量评分 (0-1，越高越好)
- **channel_complexity**: 信道复杂度 (数值，越低越简单)
- **prop_efficiency**: 传播效率 (0-1，越高越好)

### 模型性能指标
- **R²**: 决定系数，越接近1越好
- **RMSE**: 均方根误差，越小越好
- **交叉验证分数**: 模型泛化能力评估

## 常见问题

### Q: 数据库连接失败
A: 检查 .env 文件中的数据库配置

### Q: 特征提取错误
A: 确保 data/bellhop 目录下有BELLHOP仿真文件

### Q: 模型训练失败
A: 检查数据量是否足够（建议至少10个样本）

### Q: SHD文件解析错误
A: 这是正常的，程序会使用默认值继续运行

## 高级用法

### 自定义目标变量
修改 `demo_ml_workflow.py` 中的目标创建部分：
```python
targets['my_target'] = your_calculation_here
```

### 调整模型参数
修改 `RandomForestTrainer` 类中的参数：
```python
self.regressor_params = {
    'n_estimators': 200,  # 增加树的数量
    'max_depth': 15,      # 增加树的深度
    ...
}
```