# FedUWAComm

联邦学习水声通信优化系统 (Federated Learning for Underwater Acoustic Communication Optimization)

## 项目概述

FedUWAComm 是一个基于联邦学习和BELLHOP水声传播模型的水声通信系统优化平台。该项目采用monorepo架构，包含Python虚拟机、管理端前端、SpringBoot后端和OFDM水下通信仿真四个主要模块，通过机器学习技术分析水声环境特征，优化通信参数，提升水声通信质量。

## 项目架构

本项目采用monorepo开发模式，包含以下四个主要模块：

### 🐍 Python虚拟机模块 (`python-vm/`)
- **功能**: 核心的机器学习算法、BELLHOP仿真、特征提取、模型训练和联邦学习客户端
- **技术栈**: Python 3.8+, scikit-learn, pandas, numpy, MySQL, WebSocket, 加密通信
- **主要组件**:
  - BELLHOP仿真环境生成
  - 声学特征提取与模型训练
  - 联邦学习客户端 (FederatedClient)
  - WebSocket API客户端
  - 模型聚合与协调器
  - 系统监控与加密工具
  - SQLite本地存储

### 🖥️ 管理端前端 (`frontend-admin/`)
- **功能**: 完整的企业级管理界面，提供可视化的配置和监控功能
- **技术栈**: React 18, TypeScript, Ant Design, Vite, Zustand状态管理, Recharts图表, WebSocket实时通信
- **主要功能**:
  - 仪表板与系统概览
  - 用户管理与权限控制
  - 联邦学习任务管理
  - 模型版本管理
  - 训练数据管理
  - 系统日志与监控
  - 水下环境分析
  - WebSocket实时通信
  - Mock服务与测试支持

### ⚙️ SpringBoot后端 (`backend-springboot/`)
- **功能**: 企业级RESTful API服务，提供完整的联邦学习平台后端支持
- **技术栈**: Spring Boot 3.4.4, MyBatis 3.0.3, MySQL, JWT双套认证, WebSocket, Lombok
- **主要功能**:
  - 完整的RESTful API接口 (用户、管理员、任务、模型、数据等14个控制器)
  - JWT双套认证系统 (用户JWT + VM JWT)
  - WebSocket实时通信
  - 联邦学习任务调度与管理
  - 模型版本管理与分发
  - 训练数据管理与预处理
  - 系统日志与监控
  - 虚拟机实例管理
  - 工作流编排与监控
  - 多模块Maven架构 (common/pojo/server)
- **项目状态**: ✅ 企业级功能完整，生产就绪

### 📡 OFDM水下通信模块 (`ofdm-underwater/`)
- **功能**: MATLAB水下OFDM通信系统仿真与优化
- **技术栈**: MATLAB R2020b+, 信号处理工具箱
- **主要功能**:
  - OFDM调制解调器
  - 水下信道建模与仿真
  - 多种均衡算法 (LMS, RLS, MMSE)
  - 信道同步与估计
  - 窗函数与CP添加
  - 性能分析与可视化
  - 参数配置与验证
- **项目状态**: ✅ 完整仿真系统

## 核心功能

- **BELLHOP仿真环境生成**: 自动生成多样化的水声传播环境
- **特征提取与模型训练**: 从仿真结果中提取声学传播特征，使用随机森林等算法训练
- **联邦学习平台**: 完整的分布式机器学习框架，支持FedAvg、FedProx等算法
- **企业级后端服务**: RESTful API + WebSocket实时通信，支持用户管理、任务调度、模型分发
- **现代化管理界面**: React前端，支持仪表板、任务监控、模型管理、数据可视化
- **OFDM水下通信仿真**: MATLAB实现的完整OFDM通信系统，包含信道建模与均衡
- **数据库管理**: MySQL + SQLite混合存储，支持大规模数据管理
- **实时通信**: WebSocket支持实时状态更新和任务协调
- **安全认证**: JWT双套认证体系，用户与虚拟机分离认证

## 快速开始

🚀 **新用户必读**: [合作者快速上手指南](docs/shared/GETTING_STARTED.md)

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

**数据库设计参考**: 详细的数据库表结构设计请参考 [数据库设计文档](docs/shared/database/database_schema.md)

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

### 7. 启动Spring Boot后端

```bash
# 启动后端服务
cd backend-springboot
mvn -pl feduwacomm-server spring-boot:run

# 或使用启动脚本
run.bat
```

### 8. 启动前端管理界面

```bash
# 安装依赖
cd frontend-admin
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

### 9. 启动OFDM仿真（可选）

```matlab
% 在MATLAB中
cd ofdm-underwater
init_project
main_simulation  % 运行主仿真
```

## 项目结构

```
FedUWAComm/                          # Monorepo根目录
├── python-vm/                       # Python虚拟机模块
│   ├── src/feduwacomm/            # 主包
│   │   ├── core/                  # 核心功能模块
│   │   ├── ml/                    # 机器学习模块
│   │   │   ├── feature_extractor.py    # 特征提取器
│   │   │   ├── model_evaluator.py      # 模型评估
│   │   │   ├── random_forest_trainer.py # 随机森林训练
│   │   │   ├── federated/         # 联邦学习框架
│   │   │   │   ├── client.py            # 联邦学习客户端
│   │   │   │   ├── aggregator.py        # 模型聚合器
│   │   │   │   ├── coordinator.py       # 协调器
│   │   │   │   └── model_wrapper.py     # 模型包装器
│   │   │   ├── api/               # API客户端
│   │   │   │   ├── vm_api_client.py     # VM API客户端
│   │   │   │   ├── websocket_client.py  # WebSocket客户端
│   │   │   │   └── message_handler.py   # 消息处理器
│   │   │   ├── storage/           # 存储模块
│   │   │   │   └── sqlite_storage.py    # SQLite存储
│   │   │   └── utils/             # ML工具
│   │   │       ├── system_monitor.py    # 系统监控
│   │   │       └── crypto_utils.py      # 加密工具
│   │   ├── database/              # 数据库管理模块
│   │   ├── acoustic/              # 声学模拟模块
│   │   └── utils/                 # 工具函数模块
│   ├── scripts/                   # 脚本文件
│   ├── tests/                     # 测试文件
│   ├── data/bellhop/              # BELLHOP仿真数据 (300+文件)
│   ├── setup.py                   # 安装配置
│   └── requirements.txt           # Python依赖
├── frontend-admin/                 # 管理端前端模块 (React + TypeScript)
│   ├── src/
│   │   ├── components/            # 通用组件
│   │   ├── modules/               # 功能模块
│   │   │   ├── dashboard/         # 仪表板
│   │   │   ├── admin/             # 管理员功能
│   │   │   ├── federated-learning/ # 联邦学习管理
│   │   │   ├── model-management/  # 模型管理
│   │   │   ├── system-logs/       # 系统日志
│   │   │   └── underwater-optimization/ # 水下优化
│   │   ├── services/              # API服务
│   │   ├── mocks/                 # Mock数据
│   │   └── api/                   # API接口
│   ├── electron/                  # Electron桌面应用
│   ├── package.json               # 前端依赖
│   └── vite.config.ts             # Vite配置
├── backend-springboot/            # SpringBoot后端模块
│   ├── feduwacomm-common/         # 通用模块
│   │   ├── src/main/java/com/feduwacomm/
│   │   │   ├── common/            # 通用类
│   │   │   ├── utils/             # 工具类
│   │   │   └── exception/         # 异常类
│   ├── feduwacomm-pojo/           # 数据对象模块
│   │   └── src/main/java/com/feduwacomm/
│   │       ├── dto/               # 数据传输对象
│   │       ├── vo/                # 视图对象
│   │       └── entity/            # 实体类
│   ├── feduwacomm-server/         # 服务端模块
│   │   ├── src/main/java/com/feduwacomm/
│   │   │   ├── controller/        # 14个REST控制器
│   │   │   ├── service/           # 业务服务层
│   │   │   ├── mapper/            # MyBatis映射器
│   │   │   ├── config/            # 配置类
│   │   │   └── interceptor/       # 拦截器
│   │   ├── src/main/resources/
│   │   │   ├── application.yml    # 应用配置
│   │   │   ├── mapper/            # MyBatis XML映射
│   │   │   └── static/            # 静态资源
│   │   └── src/test/java/         # 测试代码
│   ├── pom.xml                    # Maven父项目配置
│   └── run.bat                    # Windows启动脚本
├── ofdm-underwater/               # OFDM水下通信模块 (MATLAB)
│   ├── src/
│   │   ├── core/                  # 核心算法
│   │   │   ├── ofdm_modulator.m       # OFDM调制器
│   │   │   ├── ofdm_demodulator.m     # OFDM解调器
│   │   │   └── underwater_channel.m   # 水下信道模型
│   │   ├── equalizers/            # 均衡算法
│   │   │   ├── lms_equalizer.m        # LMS均衡
│   │   │   ├── rls_equalizer.m        # RLS均衡
│   │   │   └── mmse_equalizer.m       # MMSE均衡
│   │   ├── utils/                 # 工具函数
│   │   │   ├── windowing.m            # 窗函数
│   │   │   ├── plotting.m             # 绘图工具
│   │   │   └── metrics.m              # 性能指标
│   │   └── main_simulation.m      # 主仿真脚本
│   ├── configs/                   # 配置文件
│   ├── tests/                     # 测试脚本
│   ├── init_project.m             # 项目初始化
│   └── README.md                  # OFDM模块说明
├── docs/                          # 项目文档
│   ├── shared/api/HTTP/           # HTTP API文档 (40+接口文档)
│   ├── shared/api/WebSocket/      # WebSocket协议文档
│   ├── shared/database/           # 数据库设计文档
│   ├── shared/stages/             # 开发阶段规划
│   └── backend-springboot/        # 后端技术文档
├── README.md                      # 项目主文档
├── CLAUDE.md                      # Claude开发指南
└── .gitignore                     # Git忽略文件
```

## 技术栈

### Python虚拟机模块
- **机器学习**: scikit-learn, pandas, numpy
- **数据库**: MySQL, PyMySQL
- **声学仿真**: BELLHOP
- **开发语言**: Python 3.8+

### 管理端前端模块
- **框架**: React 18, Vite
- **语言**: TypeScript
- **UI库**: Ant Design 5.0
- **状态管理**: Zustand
- **图表**: Recharts
- **通信**: Axios, WebSocket (STOMP)
- **测试**: Vitest, MSW Mock
- **桌面应用**: Electron

### SpringBoot后端模块
- **框架**: Spring Boot 3.4.4
- **语言**: Java 17
- **数据库**: MySQL
- **ORM**: MyBatis 3.0.3
- **安全**: JWT双套认证, Spring Security
- **通信**: WebSocket (STOMP), RESTful API
- **工具**: Lombok 1.18.38, Jackson
- **架构**: 多模块Maven项目
- **部署**: 支持Docker, 配置文件环境分离

### OFDM水下通信模块
- **平台**: MATLAB R2020b+
- **工具箱**: 信号处理工具箱, 通信工具箱
- **算法**: OFDM调制解调, LMS/RLS/MMSE均衡
- **仿真**: 水下信道建模, 噪声分析
- **可视化**: 频谱分析, 误码率曲线

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
# 启动开发服务器
mvn -pl feduwacomm-server spring-boot:run

# 构建项目
mvn clean package

# 运行测试
mvn test
```

## 模块开发状态

### ✅ 已完成
- **Python虚拟机模块**: 核心功能完整，包含BELLHOP仿真、特征提取、机器学习训练
- **Spring Boot后端**: 基础框架搭建完成，包含多模块架构、MyBatis配置、Spring Security

### ✅ 生产就绪
- **Spring Boot后端**: 企业级功能完整，包含完整的用户管理、任务调度、模型管理、实时通信等
- **管理端前端**: 完整的现代化Web应用，支持仪表板、管理功能、实时监控等
- **OFDM水下通信**: 完整的MATLAB仿真系统，支持多种均衡算法和性能分析

## 使用文档

详细使用说明请参考：
- 🚀 [合作者快速上手指南](docs/shared/GETTING_STARTED.md) - 新用户必读
- 🗺️ [开发阶段规划](docs/shared/stages/开发阶段规划.md) - 阶段性里程碑与优先级安排
- 📖 [项目概述](docs/shared/project_overview.md) - 系统架构说明
- 📊 [项目总结](docs/shared/project_summary.md) - 技术总结报告
- 🐍 [Python模块文档](docs/python-vm/) - Python虚拟机模块详细文档
- 🖥️ [前端模块文档](docs/frontend-admin/) - 管理端前端文档
- ⚙️ [后端模块文档](docs/backend-springboot/) - SpringBoot后端文档
- 🗄️ [数据库设计文档](docs/shared/database/database_schema.md) - 数据库表结构设计

## API接口

详细的API接口文档请参考：
- 🔌 [API接口文档](docs/shared/api/README.md) - HTTP和WebSocket接口完整文档

### 主要API接口

**基础接口**
- `GET /api/health` - 系统健康状态检查

**用户管理**
- `POST /api/user/register` - 用户注册
- `POST /api/user/login` - 用户登录
- `POST /api/user/token/refresh` - 刷新令牌

**管理员功能**
- `POST /api/admin/user/create` - 创建用户
- `GET /api/admin/users` - 用户列表查询
- `PUT /api/admin/user/lock` - 锁定用户

**联邦学习任务**
- `POST /api/federated-task/create` - 创建联邦学习任务
- `GET /api/federated-task/list` - 任务列表查询
- `POST /api/federated-task/start` - 启动任务

**模型管理**
- `POST /api/model-version/upload` - 模型上传
- `GET /api/model-version/list` - 模型版本列表
- `POST /api/model-version/deploy` - 模型部署

**虚拟机管理**
- `POST /api/vm-instance/register` - 虚拟机注册
- `GET /api/vm-instance/list` - 虚拟机列表
- `POST /api/vm/token/refresh` - VM令牌刷新

**实时通信**
- WebSocket端点: `/ws` (STOMP协议)
- 支持任务状态、模型同步等实时更新

完整API文档参见: [API接口文档](docs/shared/api/HTTP/README.md)

## 贡献

欢迎提交Issue和Pull Request来改进项目。

### 开发规范
- 每个模块独立开发和测试
- 遵循模块化设计原则
- 保持代码文档的完整性
- 遵循各模块的技术栈规范

## 许可证

本项目采用MIT许可证，详见LICENSE文件。

## 联系方式

如有问题，请通过GitHub Issues联系我们。