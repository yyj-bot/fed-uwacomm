# FedUWAComm

联邦学习水声通信优化系统 (Federated Learning for Underwater Acoustic Communication Optimization)

## 项目概述

FedUWAComm 是一个基于联邦学习和BELLHOP水声传播模型的水声通信系统优化平台。该项目采用monorepo架构，包含Python虚拟机、管理端前端和SpringBoot后端三个主要模块，通过机器学习技术分析水声环境特征，优化通信参数，提升水声通信质量。

## 项目架构

本项目采用monorepo开发模式，包含以下三个主要模块：

### 🐍 Python虚拟机模块 (`python-vm/`)
- **功能**: 核心的机器学习算法、BELLHOP仿真、特征提取和模型训练
- **技术栈**: Python 3.8+, scikit-learn, pandas, numpy, MySQL
- **主要组件**:
  - BELLHOP仿真环境生成
  - 声学特征提取
  - 机器学习模型训练
  - 数据库管理

### 🖥️ 管理端前端 (`frontend-admin/`)
- **功能**: 系统管理界面，提供可视化的配置和监控功能
- **技术栈**: 待定
- **主要功能**:
  - 系统配置管理
  - 实时监控面板
  - 数据可视化
  - 用户权限管理

### ⚙️ SpringBoot后端 (`backend-springboot/`)
- **功能**: RESTful API服务，提供数据接口和业务逻辑处理
- **技术栈**: 待开发 (Spring Boot + Java)
- **主要功能**:
  - RESTful API接口
  - 用户认证授权
  - 数据持久化
  - 微服务架构

## 核心功能

- **BELLHOP仿真环境生成**: 自动生成多样化的水声传播环境
- **特征提取**: 从仿真结果中提取声学传播特征
- **机器学习训练**: 使用随机森林等算法进行模型训练
- **数据库管理**: MySQL数据库存储和管理特征数据
- **模型评估**: 完整的模型性能评估体系
- **联邦学习**: 分布式机器学习算法实现
- **Web管理界面**: 可视化的系统管理平台

## 快速开始

🚀 **新用户必读**: [合作者快速上手指南](docs/python-vm/GETTING_STARTED.md)

### 1. 环境准备

```bash
# 克隆项目
git clone <repository-url>
cd FedUWAComm

# 安装Python虚拟机模块依赖
cd python-vm
pip install -r requirements.txt
```

### 2. 配置数据库

```bash
# 在python-vm目录下创建.env文件
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=bellhop_data
```

### 3. 安装Python模块

```bash
# 在python-vm目录下安装
cd python-vm
pip install -e .
```

### 4. 运行完整工作流

```bash
# 运行最终版机器学习工作流
cd python-vm
python scripts/complete_workflow.py
```

### 5. 验证数据库

```bash
# 验证数据库状态
cd python-vm
python scripts/verify_database.py
```

### 6. 运行测试

```bash
# 运行所有测试
cd python-vm
python -m pytest tests/
```

## 项目结构

```
FedUWAComm/                          # Monorepo根目录
├── python-vm/                       # Python虚拟机模块
│   ├── src/                        # 源代码目录
│   │   └── feduwacomm/            # 主包
│   │       ├── __init__.py
│   │       ├── core/              # 核心功能模块
│   │       ├── ml/                # 机器学习模块
│   │       │   ├── feature_extractor.py    # 特征提取器
│   │       │   ├── model_evaluator.py      # 模型评估
│   │       │   └── random_forest_trainer.py # 随机森林训练
│   │       ├── database/          # 数据库管理模块
│   │       │   ├── database.py             # 数据库操作
│   │       │   └── database_v2.py          # 数据库v2
│   │       ├── acoustic/          # 声学模拟模块
│   │       │   ├── generate_environments.py # 环境生成
│   │       │   └── run_bellhop.py          # BELLHOP运行
│   │       └── utils/             # 工具函数模块
│   ├── scripts/                   # 脚本文件
│   │   ├── final_database_ml.py   # 最终ML工作流
│   │   ├── verify_database.py     # 数据库验证
│   │   └── run_bellhop_batch.py   # 批量运行脚本
│   ├── config/                    # 配置文件
│   │   └── settings.py            # 项目设置
│   ├── tests/                     # 测试文件
│   │   ├── test_database.py       # 数据库测试
│   │   └── test_ml.py             # 机器学习测试
│   ├── data/                      # 数据文件
│   │   ├── bellhop/               # BELLHOP仿真数据
│   │   └── example/               # 示例数据
│   ├── setup.py                   # 安装配置
│   ├── requirements.txt           # Python依赖
│   └── bellhop_features_final.csv # 特征数据
├── frontend-admin/                 # 管理端前端模块
│   ├── src/                       # 前端源代码
│   ├── public/                    # 静态资源
│   ├── package.json               # 前端依赖配置
│   └── README.md                  # 前端说明文档
├── backend-springboot/            # SpringBoot后端模块
│   ├── src/                       # Java源代码
│   ├── pom.xml                    # Maven配置
│   └── README.md                  # 后端说明文档
├── docs/                          # 项目文档
│   ├── python-vm/                 # Python模块文档
│   ├── frontend-admin/            # 前端模块文档
│   ├── backend-springboot/        # 后端模块文档
│   └── shared/                    # 共享文档
│       └── api/                   # API文档
├── README.md                      # 项目主文档
└── .gitignore                     # Git忽略文件
```

## 技术栈

### Python虚拟机模块
- **机器学习**: scikit-learn, pandas, numpy
- **数据库**: MySQL, PyMySQL
- **声学仿真**: BELLHOP
- **开发语言**: Python 3.8+

### 管理端前端模块
- **框架**: 待定 (React/Vue.js)
- **语言**: TypeScript
- **UI库**: 待定
- **状态管理**: 待定

### SpringBoot后端模块
- **框架**: Spring Boot
- **语言**: Java 11+
- **数据库**: MySQL/PostgreSQL
- **安全**: Spring Security

## 数据集信息

- **环境样本**: 318个BELLHOP环境文件
- **特征维度**: 103个声学传播特征
- **数据完整性**: > 95%

## 模型性能

- **随机森林回归**: R² > 0.85
- **随机森林分类**: 准确率 > 0.90

## 开发指南

### Python虚拟机模块开发
```bash
cd python-vm
# 开发环境设置
pip install -e .
# 运行测试
python -m pytest tests/
```

### 前端开发
```bash
cd frontend-admin
# 待开发
```

### 后端开发
```bash
cd backend-springboot
# 待开发
```

## 使用文档

详细使用说明请参考：
- 🚀 [合作者快速上手指南](docs/shared/GETTING_STARTED.md) - 新用户必读
- 📖 [项目概述](docs/shared/project_overview.md) - 系统架构说明
- 📊 [项目总结](docs/shared/project_summary.md) - 技术总结报告
- 🐍 [Python模块文档](docs/python-vm/) - Python虚拟机模块详细文档
- 🖥️ [前端模块文档](docs/frontend-admin/) - 管理端前端文档
- ⚙️ [后端模块文档](docs/backend-springboot/) - SpringBoot后端文档

## 贡献

欢迎提交Issue和Pull Request来改进项目。

### 开发规范
- 每个模块独立开发和测试
- 遵循模块化设计原则
- 保持代码文档的完整性

## 许可证

本项目采用MIT许可证，详见LICENSE文件。

## 联系方式

如有问题，请通过GitHub Issues联系我们。