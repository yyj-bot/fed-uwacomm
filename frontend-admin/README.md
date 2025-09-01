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

## 📁 项目结构

```
src/
├── api/                  # ✅ 新的模块化API结构
├── components/           # ✅ 通用组件 (Button等)
├── layouts/              # ✅ 布局组件 (MainLayout)
├── modules/              # ✅ 业务模块
│   ├── dashboard/        # ✅ 仪表盘模块
│   ├── federated-learning/ # ✅ 联邦学习模块
│   ├── model-management/ # ✅ 模型管理模块
│   ├── system-logs/      # ✅ 系统日志模块
│   ├── underwater-optimization/ # ✅ 水声优化模块
│   ├── environment-analysis/ # ✅ 环境分析模块
│   └── login/            # ✅ 登录模块
├── store/                # ✅ 全局状态管理
├── types/                # ✅ 类型定义
├── utils/                # ✅ 工具函数
├── services/             # ✅ 保留WebSocket和Auth服务
├── mocks/                # ✅ Mock数据
└── test/                 # ✅ 测试配置
```