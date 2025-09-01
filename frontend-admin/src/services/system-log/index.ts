/**
 * 系统日志服务模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// 导出服务类和实例
export { SystemLogService, systemLogService } from './systemLogService'

// 导出类型定义
export type {
  SystemLogPaginatedResponse,
  SystemLogListParams,
  RealtimeLogsParams,
  RealtimeLogsResponse,
  LogStatisticsParams,
  LogStatisticsResponse,
  LogExportData,
  LogExportResponse,
  ExportTask,
  ExportHistoryParams,
  LogCleanupData,
  LogCleanupResponse,
  CleanupTask,
  CleanupHistoryParams,
  SystemMonitor,
  LogMonitorParams,
  LogMonitor,
  PerformanceMonitorParams,
  PerformanceMonitor,
  AlertConfig,
  LogConfig,
  LogConfigUpdateData,
  LogConfigUpdateResponse,
  CleanupStrategy,
  SystemLogOperation,
  SystemLogServiceError,
  SystemLogOperationError
} from './type'