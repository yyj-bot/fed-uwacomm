/**
 * 系统模块组件统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// 导入默认导出，然后重新导出为命名导出
import LogListComponent from './LogList'
import LogDetailComponent from './LogDetail'
import LogFiltersComponent from './LogFilters'
import SystemMonitorComponent from './SystemMonitor'
import LogCleanupComponent from './LogCleanup'
import LogStatisticsComponent from './LogStatistics'
import LogConfigComponent from './LogConfig'

// 重新导出为命名导出
export const LogList = LogListComponent
export const LogDetail = LogDetailComponent
export const LogFilters = LogFiltersComponent
export const SystemMonitor = SystemMonitorComponent
export const LogCleanup = LogCleanupComponent
export const LogStatistics = LogStatisticsComponent
export const LogConfig = LogConfigComponent

// 默认导出
export default LogListComponent
