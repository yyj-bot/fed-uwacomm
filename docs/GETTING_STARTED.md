# FedUWAComm 快速上手指南

欢迎加入 FedUWAComm 项目！这是一个基于联邦学习和BELLHOP水声传播模型的水声通信系统优化平台。

## 📋 项目概述

FedUWAComm 通过机器学习技术分析水声环境特征，优化通信参数，提升水声通信质量。

**主要功能：**
- 🌊 BELLHOP仿真环境生成
- 🔍 声学传播特征提取  
- 🤖 机器学习模型训练
- 💾 MySQL数据库管理
- 📊 模型性能评估

## 🚀 快速开始（5分钟上手）

### 1. 环境准备

```bash
# 1. 克隆项目
git clone <repository-url>
cd FedUWAComm

# 2. 创建Python虚拟环境（可选）
python -m venv venv
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# 3. 安装依赖
pip install -r requirements.txt

# 4. 开发模式安装项目
pip install -e .
```

### 2. 配置数据库

```bash
# 1. 复制环境配置文件
copy .env.example .env  # Windows
# cp .env.example .env    # Linux/Mac

# 2. 编辑 .env 文件，填入你的数据库信息
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=bellhop_data
```

### 3. 验证安装

```bash
# 初始化数据库
python scripts/init_database.py

# 运行数据库验证
python scripts/verify_database.py
```

### 4. 运行完整工作流

```bash
# 运行机器学习工作流
python scripts/complete_workflow.py
```

## 📁 项目结构说明

```
FedUWAComm/
├── src/feduwacomm/          # 📦 主源代码包
│   ├── core/                # ⚙️ 核心功能模块
│   ├── ml/                  # 🤖 机器学习模块
│   ├── database/            # 💾 数据库管理
│   ├── acoustic/            # 🌊 声学模拟
│   └── utils/               # 🛠️ 工具函数
├── scripts/                 # 🎯 可执行脚本
├── config/                  # ⚙️ 配置文件
├── tests/                   # 🧪 测试文件
├── data/                    # 📊 数据文件
├── docs/                    # 📚 文档
├── models/                  # 🎯 训练好的模型
└── results/                 # 📈 实验结果
```

## 📚 详细文档

- 📖 [项目概述](project_overview.md)
- 📊 [项目总结](project_summary.md)  

## 🤝 贡献指南

1. Fork 项目仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`) 
5. 打开 Pull Request

## 📝 注意事项

- 🔒 不要提交 `.env` 文件到版本控制
- 📦 大型模型文件会被 `.gitignore` 忽略
- 📝 重要更改请更新 `README.md、.gitignore、.env.example` 等文件

## 🆘 常见问题

**Q: 数据库连接失败怎么办？**
A: 检查 `.env` 文件中的数据库配置，确保MySQL服务正在运行。

**Q: 导入模块失败？**  
A: 确保已安装项目：`pip install -e .`

**Q: BELLHOP文件路径错误？**
A: 检查 `data/bellhop/` 目录下是否有相应的 `.env` 文件。

## 📞 联系方式

如有问题，请通过以下方式联系：
- 📧 Email: [project-email]
- 💬 GitHub Issues: [repository-issues]
- 📱 团队聊天: [team-chat]

---

🎉 现在你已经准备好开始开发了！祝你编码愉快！ 