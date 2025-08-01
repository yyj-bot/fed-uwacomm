# FedUWAComm

联邦学习水声通信优化系统 (Federated Learning for Underwater Acoustic Communication Optimization)

## 项目概述

FedUWAComm 是一个基于联邦学习和BELLHOP水声传播模型的水声通信系统优化平台。该项目通过机器学习技术分析水声环境特征，优化通信参数，提升水声通信质量。

## 核心功能

- **BELLHOP仿真环境生成**: 自动生成多样化的水声传播环境
- **特征提取**: 从仿真结果中提取声学传播特征
- **机器学习训练**: 使用随机森林等算法进行模型训练
- **数据库管理**: MySQL数据库存储和管理特征数据
- **模型评估**: 完整的模型性能评估体系

## 快速开始

### 1. 环境准备

```bash
# 安装依赖
pip install -r requirements.txt

# 配置数据库（创建.env文件）
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=bellhop_data
```

### 2. 运行完整工作流

```bash
# 运行最终版机器学习工作流
python final_database_ml.py
```

### 3. 验证数据库

```bash
# 验证数据库状态
python verify_database.py
```

## 项目结构

```
FedUWAComm/
├── ml_modules/           # 机器学习核心模块
│   ├── feature_extractor.py    # 特征提取器
│   ├── database_v2.py          # 数据库管理
│   ├── random_forest_trainer.py # 随机森林训练
│   └── model_evaluator.py      # 模型评估
├── data/
│   └── bellhop/         # BELLHOP仿真数据
├── docs/                # 项目文档
│   ├── guides/         # 使用指南
│   ├── project_overview.md     # 项目概述
│   └── project_summary.md      # 项目总结
├── results/             # 实验结果
│   ├── models/         # 训练好的模型
│   └── reports/        # 分析报告
├── tools/              # 工具脚本
├── final_database_ml.py # 最终ML工作流
└── verify_database.py  # 数据库验证
```

## 技术栈

- **机器学习**: scikit-learn, pandas, numpy
- **数据库**: MySQL, PyMySQL
- **声学仿真**: BELLHOP
- **开发语言**: Python 3.8+

## 数据集信息

- **环境样本**: 318个BELLHOP环境文件
- **特征维度**: 103个声学传播特征
- **数据完整性**: > 95%

## 模型性能

- **随机森林回归**: R² > 0.85
- **随机森林分类**: 准确率 > 0.90

## 使用文档

详细使用说明请参考：
- [快速开始指南](docs/guides/quick_start.md)
- [数据库使用指南](docs/guides/database_usage.md)
- [项目概述](docs/project_overview.md)

## 贡献

欢迎提交Issue和Pull Request来改进项目。

## 许可证

本项目采用MIT许可证，详见LICENSE文件。

## 联系方式

如有问题，请通过GitHub Issues联系我们。