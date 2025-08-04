# FedUWAComm 快速上手指南

欢迎加入 FedUWAComm 项目！这是一个基于联邦学习和BELLHOP水声传播模型的水声通信系统优化平台，采用monorepo架构开发。

## 📋 项目概述

FedUWAComm 通过机器学习技术分析水声环境特征，优化通信参数，提升水声通信质量。项目采用monorepo架构，包含三个主要模块：

- **🐍 Python虚拟机模块**: 核心的机器学习算法、BELLHOP仿真、特征提取和模型训练
- **🖥️ 管理端前端**: 系统管理界面，提供可视化的配置和监控功能
- **⚙️ SpringBoot后端**: RESTful API服务，提供数据接口和业务逻辑处理

**主要功能：**
- 🌊 BELLHOP仿真环境生成
- 🔍 声学传播特征提取  
- 🤖 机器学习模型训练
- 💾 MySQL数据库管理
- 📊 模型性能评估
- 🔐 联邦学习隐私保护
- 🌐 Web管理界面

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

# 3. 安装Python虚拟机模块依赖
cd python-vm
pip install -r requirements.txt

# 4. 开发模式安装项目
pip install -e .
```

### 2. 配置数据库

```bash
# 1. 在python-vm目录下复制环境配置文件
cd python-vm
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
cd python-vm
python scripts/init_database.py

# 运行数据库验证
python scripts/verify_database.py
```

### 4. 运行完整工作流

```bash
# 运行机器学习工作流
cd python-vm
python scripts/complete_workflow.py
```

## 📁 项目结构说明

```
FedUWAComm/                          # Monorepo根目录
├── python-vm/                       # 🐍 Python虚拟机模块
│   ├── src/feduwacomm/            # 📦 主源代码包
│   │   ├── core/                  # ⚙️ 核心功能模块
│   │   ├── ml/                    # 🤖 机器学习模块
│   │   ├── database/              # 💾 数据库管理
│   │   ├── acoustic/              # 🌊 声学模拟
│   │   └── utils/                 # 🛠️ 工具函数
│   ├── scripts/                   # 🎯 可执行脚本
│   ├── config/                    # ⚙️ 配置文件
│   ├── tests/                     # 🧪 测试文件
│   ├── data/                      # 📊 数据文件
│   ├── setup.py                   # 📦 安装配置
│   └── requirements.txt           # 📋 Python依赖
├── frontend-admin/                 # 🖥️ 管理端前端模块
│   ├── src/                       # 📱 前端源代码
│   ├── public/                    # 🌐 静态资源
│   ├── package.json               # 📦 前端依赖配置
│   └── README.md                  # 📖 前端说明文档
├── backend-springboot/            # ⚙️ SpringBoot后端模块
│   ├── src/                       # ☕ Java源代码
│   ├── pom.xml                    # 📦 Maven配置
│   └── README.md                  # 📖 后端说明文档
├── docs/                          # 📚 项目文档
│   ├── python-vm/                 # 🐍 Python模块文档
│   ├── frontend-admin/            # 🖥️ 前端模块文档
│   ├── backend-springboot/        # ⚙️ 后端模块文档
│   └── shared/                    # 📋 共享文档
│       └── api/                   # 🔌 API文档
├── README.md                      # 📖 项目主文档
└── .gitignore                     # 🚫 Git忽略文件
```

## 🛠️ 模块开发指南

### Python虚拟机模块开发

```bash
cd python-vm

# 开发环境设置
pip install -e .

# 运行测试
python -m pytest tests/

# 运行完整工作流
python scripts/complete_workflow.py
```

### 前端开发（待开发）

```bash
cd frontend-admin

# 安装依赖（待开发）
npm install

# 启动开发服务器（待开发）
npm run dev
```

### 后端开发（待开发）

```bash
cd backend-springboot

# 编译项目（待开发）
mvn compile

# 运行项目（待开发）
mvn spring-boot:run
```

## 📚 详细文档

- 📖 [项目概述](../shared/project_overview.md)
- 📊 [项目总结](../shared/project_summary.md)  
- 🐍 [Python模块文档](../python-vm/) - Python虚拟机模块详细文档
- 🖥️ [前端模块文档](../frontend-admin/) - 管理端前端文档
- ⚙️ [后端模块文档](../backend-springboot/) - SpringBoot后端文档

## 🤝 贡献指南

1. Fork 项目仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`) 
5. 打开 Pull Request

### 开发规范
- 每个模块独立开发和测试
- 遵循模块化设计原则
- 保持代码文档的完整性

## 📝 注意事项

- 🔒 不要提交 `.env` 文件到版本控制
- 📦 大型模型文件会被 `.gitignore` 忽略
- 📝 重要更改请更新相应模块的文档
- 🏗️ 遵循monorepo开发规范

## 🆘 常见问题

**Q: 数据库连接失败怎么办？**
A: 检查 `python-vm/.env` 文件中的数据库配置，确保MySQL服务正在运行。

**Q: 导入模块失败？**  
A: 确保在 `python-vm` 目录下已安装项目：`pip install -e .`

**Q: BELLHOP文件路径错误？**
A: 检查 `python-vm/data/bellhop/` 目录下是否有相应的 `.env` 文件。

**Q: 如何切换模块开发？**
A: 使用 `cd` 命令切换到相应模块目录，每个模块都有独立的开发环境。

## 📞 联系方式

如有问题，请通过以下方式联系：
- 📧 Email: [project-email]
- 💬 GitHub Issues: [repository-issues]
- 📱 团队聊天: [team-chat]

---

🎉 现在你已经准备好开始开发了！祝你编码愉快！ 