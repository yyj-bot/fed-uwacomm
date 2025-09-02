/**
 * 仪表盘模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

export { useDashboardStore } from './dashboardStore'
export { useDashboard } from './useDashboardStore'
export type { 
  DashboardState, 
  DashboardActions, 
  DashboardStore,
  DashboardOverview,
  DashboardChartData
} from './dashboardStore'

// 默认导出 hook
export { useDashboard as default } from './useDashboardStore'
