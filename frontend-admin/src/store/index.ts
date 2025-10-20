/**
 * Store 统一导出文件
 * 提供项目中所有状态管理 store 和 hooks 的统一访问入口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// ==================== 认证模块 ====================
export { useAuth } from './auth'

// ==================== 虚拟机管理模块 ====================
export { useVM } from './vm'

// ==================== 联邦学习任务模块 ====================
export { useTask } from './federated-task'

// ==================== 训练数据模块 ====================
export { useData } from './training-data'

// ==================== 模型版本模块 ====================
export { useModel } from './model-version'

// ==================== 管理员功能模块 ====================
export { useAdmin } from './admin'

// ==================== 系统监控模块 ====================
export { useSystem } from './system'

// ==================== 仪表盘模块 ====================
export { useDashboard } from './dashboard'

// ==================== 类型导出 ====================
export type { DashboardOverview, DashboardChartData } from './dashboard'

// ==================== 默认导出主要 Hooks ====================
export { useAuth as default } from './auth'