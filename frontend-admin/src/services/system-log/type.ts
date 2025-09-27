/**
 * 系统日志服务类型定义
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { PaginationParams, SortParams, SystemLog } from '@/types'
import type { 
  ExportTask,
  CleanupTask,
  SystemMonitor,
  LogMonitor,
  PerformanceMonitor,
  AlertConfig,
  LogConfig
} from '@/api/system-log'

// ==================== 系统日志相关类型 ====================

export interface SystemLogPaginatedResponse<T> {
  readonly total: number
  readonly pages: number
  readonly current: number
  readonly size: number
  readonly records: T[]
}

export interface SystemLogListParams extends PaginationParams, SortParams {
  readonly level?: string
  readonly category?: string
  readonly vmId?: string
  readonly taskId?: string
  readonly startTime?: string
  readonly endTime?: string
  readonly keyword?: string
}

export interface RealtimeLogsParams {
  readonly level?: string
  readonly category?: string
  readonly vmId?: string
  readonly taskId?: string
  readonly tail?: number
}

export interface RealtimeLogsResponse {
  readonly logs: SystemLog[]
  readonly totalCount: number
  readonly lastUpdateTime: string
}

export interface LogStatisticsParams {
  readonly startTime?: string
  readonly endTime?: string
  readonly category?: string
  readonly vmId?: string
  readonly taskId?: string
}

export interface LogStatisticsResponse {
  readonly totalLogs: number
  readonly levelDistribution: {
    readonly DEBUG: number
    readonly INFO: number
    readonly WARN: number
    readonly ERROR: number
  }
  readonly categoryDistribution: {
    readonly SYSTEM: number
    readonly USER: number
    readonly VM: number
    readonly TASK: number
    readonly DATA: number
    readonly MODEL: number
    readonly SECURITY: number
    readonly PERFORMANCE: number
  }
  readonly timeDistribution: Array<{
    readonly hour: string
    readonly count: number
  }>
  readonly errorTrend: Array<{
    readonly date: string
    readonly errorCount: number
  }>
}

// ==================== 日志导出相关类型 ====================

export interface LogExportData {
  readonly level?: string
  readonly category?: string
  readonly vmId?: string
  readonly taskId?: string
  readonly startTime?: string
  readonly endTime?: string
  readonly keyword?: string
  readonly format?: 'CSV' | 'JSON' | 'EXCEL'
  readonly includeDetails?: boolean
}

export interface LogExportResponse {
  readonly exportId: string
  readonly status: string
  readonly estimatedTime: number
  readonly downloadUrl: string
}

// ExportTask 类型现在从 @/api/system-log 导入

export interface ExportHistoryParams {
  readonly status?: string
  readonly page?: number
  readonly size?: number
}

// ==================== 日志清理相关类型 ====================

export type CleanupStrategy = 'TIME_BASED' | 'LEVEL_BASED' | 'CATEGORY_BASED'

export interface LogCleanupData {
  readonly strategy: CleanupStrategy
  readonly retentionDays?: number
  readonly level?: string
  readonly category?: string
  readonly vmId?: string
  readonly taskId?: string
  readonly startTime?: string
  readonly endTime?: string
}

export interface LogCleanupResponse {
  readonly cleanupId: string
  readonly status: string
  readonly estimatedRecords: number
  readonly estimatedSize: number
}

// CleanupTask 类型现在从 @/api/system-log 导入

export interface CleanupHistoryParams {
  readonly status?: string
  readonly page?: number
  readonly size?: number
}

// ==================== 系统监控相关类型 ====================

// SystemMonitor 类型现在从 @/api/system-log 导入

export interface LogMonitorParams {
  readonly timeRange?: '1h' | '6h' | '24h' | '7d'
  readonly level?: string
}

// LogMonitor 类型现在从 @/api/system-log 导入

export interface PerformanceMonitorParams {
  readonly timeRange?: '1h' | '6h' | '24h' | '7d'
  readonly endpoint?: string
}

// PerformanceMonitor 和 AlertConfig 类型现在从 @/api/system-log 导入

// ==================== 日志配置相关类型 ====================

// LogConfig 类型现在从 @/api/system-log 导入

// 重新导出从API层导入的类型，方便统一使用
export type {
  ExportTask,
  CleanupTask,
  SystemMonitor,
  LogMonitor,
  PerformanceMonitor,
  AlertConfig,
  LogConfig
}

export interface LogConfigUpdateData {
  readonly logLevel?: string
  readonly retentionDays?: number
  readonly maxFileSize?: number
  readonly categories?: Record<string, {
    readonly level: string
    readonly enabled: boolean
  }>
  readonly exportSettings?: {
    readonly maxRecordsPerExport?: number
    readonly exportRetentionDays?: number
  }
}

export interface LogConfigUpdateResponse {
  readonly updatedAt: string
}

// ==================== 服务操作类型 ====================

export type SystemLogOperation =
  | 'QUERY_LIST'
  | 'QUERY_DETAIL'
  | 'QUERY_REALTIME'
  | 'QUERY_STATISTICS'
  | 'EXPORT_LOGS'
  | 'QUERY_EXPORT_STATUS'
  | 'DOWNLOAD_EXPORT'
  | 'QUERY_EXPORT_HISTORY'
  | 'CLEANUP_LOGS'
  | 'QUERY_CLEANUP_STATUS'
  | 'QUERY_CLEANUP_HISTORY'
  | 'MONITOR_SYSTEM'
  | 'MONITOR_LOGS'
  | 'MONITOR_PERFORMANCE'
  | 'QUERY_ALERTS'
  | 'QUERY_CONFIG'
  | 'UPDATE_CONFIG'

export interface SystemLogServiceError {
  readonly code: string
  readonly message: string
  readonly details?: unknown
  readonly originalError?: Error
}

export interface SystemLogOperationError extends SystemLogServiceError {
  readonly operation: SystemLogOperation
  readonly failureReason:
    | 'INVALID_PARAMETER'
    | 'NOT_FOUND'
    | 'PERMISSION_DENIED'
    | 'EXPORT_FAILED'
    | 'CLEANUP_FAILED'
    | 'CONFIG_INVALID'
    | 'UNKNOWN'
}
