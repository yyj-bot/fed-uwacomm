# FedUWAComm Admin Dashboard

> 水声联邦学习管理端 - 专业的水声通信优化可视化桌面应用

## 🚀 项目概述

FedUWAComm Admin 是一个基于 React + TypeScript + Electron 构建的现代化桌面应用，专为水声联邦学习系统设计。提供完整的数据可视化、模型监控、系统管理功能。

### ✨ 核心特性

- 🎯 **严格类型安全** - 全面的 TypeScript 类型定义，零运行时类型错误
- 📊 **35+ 专业图表** - 涵盖环境分析、模型性能、联邦学习全流程可视化
- 🖥️ **桌面应用** - 基于 Electron 的跨平台桌面软件
- ⚡ **高性能架构** - Vite 构建，代码分割，按需加载
- 🎨 **现代化UI** - Ant Design + 自定义样式，专业美观
- 🔄 **实时更新** - WebSocket 实时数据推送
- 📱 **响应式设计** - 适配不同屏幕尺寸

## 🏗️ 技术架构

### 核心技术栈

```
Frontend Framework:  React 18 + TypeScript
Desktop Platform:    Electron 27
Build Tool:          Vite 4
UI Framework:        Ant Design 5
State Management:    Zustand
Visualization:       ECharts + D3.js + Plotly.js + Three.js
HTTP Client:         Axios
Router:              React Router v6
```

### 架构设计原则

- **KISS** (Keep It Simple, Stupid) - 保持简单愚蠢
- **MMM** (Make it Work, Make it Right, Make it Fast) - 先可用，再正确，后优化
- **高内聚，低耦合** - 模块职责清晰，依赖关系简单
- **单一职责** - 每个组件/函数只做一件事
- **不可变数据** - 使用 readonly 类型，避免副作用

## 📁 项目结构

```
frontend-admin/
├── electron/                 # Electron 主进程
│   ├── main.js              # 主进程入口
│   └── preload.js           # 预加载脚本
├── src/
│   ├── components/          # 可复用组件
│   │   ├── Layout/         # 布局组件
│   │   └── Charts/         # 图表组件库
│   ├── pages/              # 页面组件
│   │   ├── Dashboard/      # 总览仪表板
│   │   ├── EnvironmentAnalysis/    # 环境分析
│   │   ├── ModelPerformance/       # 模型性能
│   │   ├── FederatedLearning/      # 联邦学习
│   │   ├── UnderwaterOptimization/ # 通信优化
│   │   └── SystemMonitor/          # 系统监控
│   ├── services/           # 数据服务层
│   │   ├── api.ts         # API 接口
│   │   └── websocket.ts   # WebSocket 服务
│   ├── stores/            # 状态管理
│   ├── types/             # TypeScript 类型定义
│   ├── utils/             # 工具函数
│   ├── App.tsx            # 根组件
│   └── main.tsx           # 应用入口
├── assets/                # 静态资源
├── dist/                  # 构建输出
└── dist-electron/         # Electron 打包输出
```

## 🛠️ 开发指南

### 环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0 或 yarn >= 1.22.0
- Git

### 快速开始

```bash
# 1. 克隆项目
git clone <repository-url>
cd FedUWAComm/frontend-admin

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev

# 4. 启动桌面应用（开发模式）
npm run electron:dev
```

### 开发命令

```bash
# 开发
npm run dev                 # 启动 Web 开发服务器
npm run electron:dev        # 启动 Electron 开发模式

# 构建
npm run build              # 构建 Web 应用
npm run electron:build     # 构建 Electron 应用
npm run electron:dist      # 打包发布版本

# 代码质量
npm run type-check         # TypeScript 类型检查
npm run lint               # ESLint 代码检查
npm run lint:fix           # 自动修复 ESLint 问题

# 预览
npm run preview            # 预览构建结果
```

## 📊 功能模块

### 1. 总览仪表板
- 系统运行状态概览
- 关键指标统计
- 快速导航入口

### 2. 环境分析模块
- BELLHOP 仿真环境特征可视化
- 声速剖面分布图
- 环境参数统计分析
- 传播损失热力图

### 3. 模型性能模块
- 机器学习模型训练效果
- 特征重要性排序
- 模型对比分析
- 预测精度评估

### 4. 联邦学习模块
- 分布式训练进度监控
- 客户端贡献度分析
- 模型聚合效果展示
- 收敛曲线可视化

### 5. 通信优化模块
- OFDM 系统性能分析
- BER 曲线对比
- 信号质量改善效果
- 优化前后对比

### 6. 系统监控模块
- 实时系统资源监控
- 服务状态检查
- 性能指标跟踪
- 告警信息管理

## 🎨 设计规范

### 代码风格

- **TypeScript**: 严格模式，所有变量必须有明确类型
- **命名规范**: 
  - 组件: PascalCase (`UserProfile`)
  - 函数/变量: camelCase (`getUserInfo`)
  - 常量: SCREAMING_SNAKE_CASE (`API_BASE_URL`)
- **文件命名**: kebab-case 目录，PascalCase 组件文件
- **导入顺序**: React → 第三方库 → 内部模块

### 组件规范

```typescript
// ✅ 好的组件示例
interface UserProfileProps {
  readonly userId: string
  readonly onUpdate?: (user: User) => void
}

const UserProfile: React.FC<UserProfileProps> = ({ userId, onUpdate }) => {
  // 组件实现
}

export default UserProfile
```

### API 调用规范

```typescript
// ✅ 严格类型的 API 调用
export const userApi = {
  async getUser(id: string): Promise<User> {
    const response = await api.get<ApiResponse<User>>(`/users/${id}`)
    return response.data.data
  }
} as const
```

## 🔧 配置说明

### 环境变量

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws

# .env.production  
VITE_API_BASE_URL=/api
VITE_WS_URL=/ws
```

### 构建配置

- **Vite**: 现代化构建工具，支持 HMR
- **TypeScript**: 严格类型检查
- **ESLint**: 代码质量检查
- **Prettier**: 代码格式化

## 📦 部署指南

### 桌面应用打包

```bash
# 构建并打包
npm run electron:dist

# 输出文件位置
dist-electron/
├── FedUWAComm Admin Setup 1.0.0.exe    # Windows 安装包
├── FedUWAComm Admin-1.0.0.dmg          # macOS 安装包
└── FedUWAComm Admin-1.0.0.AppImage     # Linux 安装包
```

### Web 版本部署

```bash
# 构建 Web 版本
npm run build

# 部署 dist/ 目录到 Web 服务器
```

## 🚨 故障排除

### 常见问题

1. **依赖安装失败**
   ```bash
   # 清除缓存重新安装
   rm -rf node_modules package-lock.json
   npm install
   ```

2. **TypeScript 类型错误**
   ```bash
   # 检查类型定义
   npm run type-check
   ```

3. **Electron 打包失败**
   ```bash
   # 重新构建
   npm run build
   npm run electron:build
   ```

## 🤝 贡献指南

1. **代码提交规范**
   ```
   feat: 新功能
   fix: Bug 修复
   docs: 文档更新
   style: 代码格式
   refactor: 重构
   test: 测试
   chore: 构建/工具链
   ```

2. **Pull Request 流程**
   - Fork 项目
   - 创建功能分支
   - 提交代码（遵循规范）
   - 发起 Pull Request

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 团队

- **项目负责人**: FedUWAComm Team
- **技术栈**: React + TypeScript + Electron
- **设计理念**: KISS + MMM 原则

---

**注意**: 该项目遵循严格的类型安全和代码质量标准，绝不允许向后兼容和技术债务。
