/**
 * 系统日志服务类型定义
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { PaginationParams, SortParams, SystemLog } from '@/types'

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

export interface ExportTask {
  readonly exportId: string
  readonly status: string
  readonly estimatedTime?: number
  readonly downloadUrl?: string
  readonly expiresAt?: string
  readonly progress?: number
  readonly totalRecords?: number
  readonly processedRecords?: number
  readonly fileSize?: number
  readonly createdAt: string
  readonly completedAt?: string
  readonly format?: string
}

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

export interface CleanupTask {
  readonly cleanupId: string
  readonly status: string
  readonly estimatedRecords?: number
  readonly estimatedSize?: number
  readonly progress?: number
  readonly deletedRecords?: number
  readonly freedSpace?: number
  readonly createdAt: string
  readonly completedAt?: string
  readonly strategy?: string
}

export interface CleanupHistoryParams {
  readonly status?: string
  readonly page?: number
  readonly size?: number
}

// ==================== 系统监控相关类型 ====================

export interface SystemMonitor {
  readonly systemInfo: {
    readonly version: string
    readonly uptime: number
    readonly startTime: string
    readonly javaVersion: string
    readonly osInfo: string
  }
  readonly resourceUsage: {
    readonly cpuUsage: number
    readonly memoryUsage: number
    readonly diskUsage: number
    readonly networkIO: {
      readonly bytesIn: number
      readonly bytesOut: number
    }
  }
  readonly applicationMetrics: {
    readonly activeConnections: number
    readonly requestPerSecond: number
    readonly averageResponseTime: number
    readonly errorRate: number
  }
  readonly databaseMetrics: {
    readonly activeConnections: number
    readonly queryPerSecond: number
    readonly averageQueryTime: number
  }
}

export interface LogMonitorParams {
  readonly timeRange?: '1h' | '6h' | '24h' | '7d'
  readonly level?: string
}

export interface LogMonitor {
  readonly logMetrics: {
    readonly totalLogs: number
    readonly errorCount: number
    readonly warningCount: number
    readonly errorRate: number
    readonly warningRate: number
  }
  readonly levelTrend: Array<{
    readonly timestamp: string
    readonly DEBUG: number
    readonly INFO: number
    readonly WARN: number
    readonly ERROR: number
  }>
  readonly categoryTrend: Array<{
    readonly timestamp: string
    readonly SYSTEM: number
    readonly USER: number
    readonly VM: number
    readonly TASK: number
  }>
  readonly recentErrors: Array<{
    readonly logId: string
    readonly level: string
    readonly category: string
    readonly message: string
    readonly createdAt: string
  }>
}

export interface PerformanceMonitorParams {
  readonly timeRange?: '1h' | '6h' | '24h' | '7d'
  readonly endpoint?: string
}

export interface PerformanceMonitor {
  readonly apiMetrics: {
    readonly totalRequests: number
    readonly successfulRequests: number
    readonly failedRequests: number
    readonly successRate: number
    readonly averageResponseTime: number
    readonly p95ResponseTime: number
    readonly p99ResponseTime: number
  }
  readonly endpointMetrics: Array<{
    readonly endpoint: string
    readonly requestCount: number
    readonly successRate: number
    readonly averageResponseTime: number
    readonly errorCount: number
  }>
  readonly responseTimeTrend: Array<{
    readonly timestamp: string
    readonly average: number
    readonly p95: number
    readonly p99: number
  }>
}

export interface AlertConfig {
  readonly alerts: Array<{
    readonly alertId: string
    readonly name: string
    readonly type: string
    readonly condition: string
    readonly status: string
    readonly lastTriggered: string
    readonly triggerCount: number
  }>
  readonly alertHistory: Array<{
    readonly alertId: string
    readonly triggeredAt: string
    readonly message: string
    readonly severity: string
  }>
}

// ==================== 日志配置相关类型 ====================

export interface LogConfig {
  readonly logLevel: string
  readonly retentionDays: number
  readonly maxFileSize: number
  readonly categories: Record<string, {
    readonly level: string
    readonly enabled: boolean
  }>
  readonly exportSettings: {
    readonly maxRecordsPerExport: number
    readonly exportRetentionDays: number
    readonly supportedFormats: string[]
  }
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
    readonly maxRecordsPerExport: number
    readonly exportRetentionDays: number
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
