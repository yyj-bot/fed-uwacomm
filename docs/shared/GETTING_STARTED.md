# FedUWAComm 快速上手指南

欢迎加入 FedUWAComm 项目！这是一个基于联邦学习与 BELLHOP 水声传播模型的水声通信优化平台（monorepo 架构）。

## 📋 项目概述

FedUWAComm 通过机器学习分析水声环境特征，优化通信参数，提升水声通信质量。项目包含三个主要模块：

- 🐍 Python 虚拟机模块：核心的 ML 算法、BELLHOP 仿真、特征提取、模型训练
- 🖥️ 管理端前端：系统管理界面（规划中）
- ⚙️ Spring Boot 后端：RESTful API 与 WebSocket 服务、业务逻辑、数据访问

**主要功能：**
- 🌊 BELLHOP 仿真环境生成
- 🔍 声学传播特征提取
- 🤖 机器学习模型训练
- 💾 MySQL 数据库管理
- 📊 模型性能评估
- 🔐 联邦学习与隐私保护
- 🌐 Web 管理界面（规划中）

## 🚀 快速开始（5-10 分钟）

### 1. 准备环境

```bash
# 1) 克隆项目
git clone <repository-url>
cd FedUWAComm

# 2) 可选：创建 Python 虚拟环境
python -m venv venv
# Windows:
venv\Scripts\activate
# Linux/Mac:
# source venv/bin/activate

# 3) 安装 Python 虚拟机模块依赖
cd python-vm
pip install -r requirements.txt

# 4) 开发模式安装 Python 包
pip install -e .
```

### 2. 配置数据库（MySQL）

- 启动本地 MySQL 5.7+ 或 8.x。
- 创建数据库（示例）：

```sql
CREATE DATABASE feduwacomm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

- Python 模块通过 `.env` 管理数据库连接；在 `python-vm` 目录复制并修改环境文件：

```bash
cd python-vm
copy .env.example .env  # Windows
# cp .env.example .env   # Linux/Mac
```

编辑 `.env`（示例）：

```
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=feduwacomm
```

- Spring Boot 后端通过 `backend-springboot/feduwacomm-server/src/main/resources/application.yml` 读取数据库配置。请将其中的 `username`/`password` 改为你的本地信息，或在部署环境通过外部化配置覆盖。

关键配置（默认）：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/feduwacomm?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: <your_mysql_user>
    password: <your_mysql_password>
```

### 3. 验证 Python 模块安装

```bash
# 初始化数据库结构/样例数据（如适用）
cd python-vm
python scripts/init_database.py

# 运行数据库验证
python scripts/verify_database.py
```

### 4. 启动 Spring Boot 后端

前置条件：JDK 17、Maven 3.9+

两种方式任选其一：

```bash
# 方式 A：开发模式运行（推荐）
# 从仓库根目录执行
mvn -f backend-springboot/pom.xml -pl feduwacomm-server -am spring-boot:run

# 方式 B：打包后运行
mvn -f backend-springboot/pom.xml -pl feduwacomm-server -am clean package
java -jar backend-springboot/feduwacomm-server/target/feduwacomm-server-1.0.0.jar
```

服务默认监听：`http://localhost:8080/api`

### 5. 基础验证（后端）

- 健康检查（示例）：
  - `GET http://localhost:8080/api/health`
  - 若启用了 Actuator：`GET http://localhost:8080/api/actuator/health`
- MyBatis 映射加载：启动日志中应看到 `classpath:mapper/*.xml`
- WebSocket 测试页：在浏览器访问
  - `http://localhost:8080/api/websocket-test.html`

提示：以上路径依赖于 `server.servlet.context-path=/api`，如有调整请相应更新。

### 6. 运行完整工作流（Python）

```bash
cd python-vm
python scripts/complete_workflow.py
```

## 📁 项目结构

```text
FedUWAComm/
├── python-vm/                      # 🐍 Python 虚拟机模块
│   ├── src/feduwacomm/             # 主源代码包
│   │   ├── core/                   # 核心功能
│   │   ├── ml/                     # 机器学习
│   │   ├── database/               # 数据库访问
│   │   ├── acoustic/               # 声学仿真
│   │   └── utils/                  # 工具函数
│   ├── scripts/                    # 可执行脚本
│   ├── config/                     # 配置
│   ├── tests/                      # 测试
│   ├── data/                       # 数据
│   ├── setup.py
│   └── requirements.txt
├── backend-springboot/             # ⚙️ Spring Boot 后端（多模块）
│   ├── feduwacomm-common/          # 公共模块（异常等）
│   ├── feduwacomm-pojo/            # DTO/实体
│   └── feduwacomm-server/          # 服务端（Web/WS/MyBatis/Security）
├── frontend-admin/                 # 🖥️ 管理端前端（规划中）
├── docs/                           # 📚 文档
└── README.md                       # 📖 项目主文档
```

## 🛠️ 模块开发指南

### Python 虚拟机模块

```bash
cd python-vm
pip install -e .
python -m pytest tests/
python scripts/complete_workflow.py
```

### 后端开发（Spring Boot）

```bash
# JDK 17 + Maven 3.9+
# 从仓库根目录
mvn -f backend-springboot/pom.xml -pl feduwacomm-server -am spring-boot:run

# 或打包
mvn -f backend-springboot/pom.xml -pl feduwacomm-server -am clean package
java -jar backend-springboot/feduwacomm-server/target/feduwacomm-server-1.0.0.jar
```

常见路径：
- REST 前缀：`/api`
- WebSocket 测试页：`/api/websocket-test.html`

### 前端开发（规划中）

```bash
cd frontend-admin
npm install
npm run dev
```

## 📚 详细文档

- 项目概述：`docs/shared/project_overview.md`
- 项目总结：`docs/shared/project_summary.md`
- Python 模块文档：`docs/python-vm/README.md`
- 后端模块文档：`docs/backend-springboot/README.md`
- API 文档：`docs/shared/api/`

## 🤝 贡献指南

1. Fork 仓库
2. 创建功能分支（`git checkout -b feature/AmazingFeature`）
3. 提交更改（`git commit -m 'Add some AmazingFeature'`）
4. 推送分支（`git push origin feature/AmazingFeature`）
5. 打开 Pull Request

### 开发规范
- 模块独立开发与测试
- 遵循模块化设计原则
- 同步更新文档

## 📝 注意事项
- 不要提交 `.env`/密钥到版本库
- 大型模型/数据文件请通过外部存储
- 重要更改请更新相应模块文档
- 遵循 monorepo 开发规范

## 🆘 常见问题

- 数据库连接失败：检查 `python-vm/.env` 与后端 `application.yml`，确认 MySQL 在线并账号密码正确
- Python 导入失败：确保已在 `python-vm` 下执行 `pip install -e .`
- WebSocket 无法连接：确认服务 `http://localhost:8080/api` 正常、浏览器跨域/代理未拦截

---

🎉 现在你已可以同时运行 Python 工作流与后端服务！祝编码愉快！ 