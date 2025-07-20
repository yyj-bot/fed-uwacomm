# FedUWAComm - 基于联邦学习的水声通信优化系统

[![Python](https://img.shields.io/badge/python-3.8+-blue.svg)](https://www.python.org/downloads/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Status](https://img.shields.io/badge/status-development-orange.svg)]()

基于联邦学习的水声通信优化与隐私保护系统，利用BELLHOP水声仿真模型生成训练数据，通过机器学习技术优化水声通信参数。

## ✨ 主要特性

- 🌊 **水声信道仿真**: 基于BELLHOP模型生成真实海洋环境下的水声信道特征
- 🤖 **机器学习预测**: 采用随机森林算法预测水声信道特性和最优通信参数  
- 🔒 **联邦学习框架**: 实现分布式训练，保护数据隐私
- ⚡ **通信参数优化**: 基于预测结果动态调整通信参数，提升性能
- 🔄 **完整工作流程**: 从数据生成到模型训练的端到端解决方案

## 🚀 快速开始

### 环境要求

- Python 3.8+
- MySQL 5.7+
- BELLHOP仿真软件

### 安装

```bash
# 克隆项目
git clone https://github.com/yourusername/FedUWAComm.git
cd FedUWAComm

# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate     # Windows

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp env.example .env
# 编辑.env文件，设置数据库连接参数
```

### 配置

创建 `.env` 文件并配置：

```env
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=bellhop_data

# 项目路径
BELLHOP_PATH=data/bellhop
```

### 运行

```bash
# 运行完整工作流程（生成环境文件、运行仿真、提取特征、导入数据库）
python run.py workflow --dir data/bellhop --num 10
```

## 📁 项目结构

```
FedUWAComm/
├── data/bellhop/          # BELLHOP数据和可执行文件
├── docs/                  # 项目文档
├── models/                # 模型存储目录
├── results/               # 结果输出目录
├── src/                   # 源代码
│   ├── preprocessing/     # 数据预处理
│   ├── simulation/        # 仿真模块
│   ├── utils/            # 工具函数
│   └── visualization/    # 可视化模块
├── tests/                # 测试文件
├── .env                  # 环境配置
├── requirements.txt      # 依赖列表
└── run.py               # 主入口脚本
```

## 🛠️ 使用指南

### 命令行接口

```bash
# 生成环境文件并运行仿真
python run.py workflow --dir data/bellhop --num 10

# 仅生成环境文件
python run.py generate --dir data/bellhop --num 5

# 仅运行仿真
python run.py simulate --dir data/bellhop

# 仅提取特征
python run.py extract --dir data/bellhop
```

### 参数说明

- `--dir`: 数据目录路径
- `--num`: 生成文件数量（仅用于generate和workflow）

## 🗄️ 数据库

系统使用MySQL存储BELLHOP水声仿真的特征数据，包括：

- **基础特征**: 文件名、频率、声速剖面点数等
- **声速剖面特征**: 各深度点的深度和声速值
- **传播损失特征**: 传播损失相关特征（预留）
- **射线路径特征**: 射线路径相关特征（预留）

## 🔧 BELLHOP集成

项目集成了BELLHOP水声传播模型，支持：

- 自动生成环境文件（.env）
- 批量运行仿真
- 解析输出文件（.prt, .ray, .shd）
- 特征提取和数据导入

### 支持的可执行文件

- `bellhopf.exe` - Fortran版本（推荐）
- `bellhopcxx.exe` - C++版本
- `bellhopcuda.exe` - CUDA版本（需要NVIDIA GPU）

## 🧪 测试

```bash
# 运行测试
pytest tests/

# 代码格式化
black src/
flake8 src/
```

## 📖 文档

- [项目概述](docs/project_overview.md)
- [系统架构](docs/architecture/system_design.md)
- [数据库设计](docs/database/schema.md)

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

本项目采用MIT许可证 - 详见 [LICENSE](LICENSE) 文件 