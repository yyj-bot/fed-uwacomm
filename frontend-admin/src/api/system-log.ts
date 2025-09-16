import { createApiInstance } from './base'
import type { 
  ApiResponse, 
  SystemLog,
  PaginatedResponse,
  PaginationParams,
  SortParams
} from '@/types'

// 创建日志API实例
const logApiInstance = createApiInstance('LOG')

// 导出任务类型
interface ExportTask {
  exportId: string
  status: string
  estimatedTime?: number
  downloadUrl?: string
  expiresAt?: string
  progress?: number
  totalRecords?: number
  processedRecords?: number
  fileSize?: number
  createdAt: string
  completedAt?: string
  format?: string
}

// 清理任务类型
interface CleanupTask {
  cleanupId: string
  status: string
  estimatedRecords?: number
  estimatedSize?: number
  dryRun?: boolean
  progress?: number
  deletedRecords?: number
  freedSpace?: number
  createdAt: string
  completedAt?: string
  strategy?: string
}

// 系统监控类型
interface SystemMonitor {
  systemInfo: {
    version: string
    uptime: number
    startTime: string
    javaVersion: string
    osInfo: string
  }
  resourceUsage: {
    cpuUsage: number
    memoryUsage: number
    diskUsage: number
    networkIO: {
      bytesIn: number
      bytesOut: number
    }
  }
  applicationMetrics: {
    activeConnections: number
    requestPerSecond: number
    averageResponseTime: number
    errorRate: number
  }
  databaseMetrics: {
    activeConnections: number
    queryPerSecond: number
    averageQueryTime: number
  }
}

// 日志监控类型
interface LogMonitor {
  logMetrics: {
    totalLogs: number
    errorCount: number
    warningCount: number
    errorRate: number
    warningRate: number
  }
  levelTrend: Array<{
    timestamp: string
    DEBUG: number
    INFO: number
    WARN: number
    ERROR: number
  }>
  categoryTrend: Array<{
    timestamp: string
    SYSTEM: number
    USER: number
    VM: number
    TASK: number
  }>
  recentErrors: Array<{
    logId: string
    level: string
    category: string
    message: string
    createdAt: string
  }>
}

// 性能监控类型
interface PerformanceMonitor {
  apiMetrics: {
    totalRequests: number
    successfulRequests: number
    failedRequests: number
    successRate: number
    averageResponseTime: number
    p95ResponseTime: number
    p99ResponseTime: number
  }
  endpointMetrics: Array<{
    endpoint: string
    requestCount: number
    successRate: number
    averageResponseTime: number
    errorCount: number
  }>
  responseTimeTrend: Array<{
    timestamp: string
    average: number
    p95: number
    p99: number
  }>
}

// 告警配置类型
interface AlertConfig {
  alerts: Array<{
    alertId: string
    name: string
    type: string
    condition: string
    status: string
    lastTriggered: string
    triggerCount: number
  }>
  alertHistory: Array<{
    alertId: string
    triggeredAt: string
    message: string
    severity: string
  }>
}

// 日志配置类型
interface LogConfig {
  logLevel: string
  retentionDays: number
  maxFileSize: number
  categories: Record<string, {
    level: string
    enabled: boolean
  }>
  exportSettings: {
    maxRecordsPerExport: number
    exportRetentionDays: number
    supportedFormats: string[]
  }
}

// ==================== 水声联邦学习系统日志管理API ====================
export const log = {
  // ==================== 3. 日志查询接口 ====================
  
  // 3.1 日志列表查询
  async getLogList(params: PaginationParams & SortParams & {
    level?: string
    category?: string
    vmId?: string
    taskId?: string
    startTime?: string
    endTime?: string
    keyword?: string
  } = {}): Promise<any> {
    const response = await logApiInstance.get<ApiResponse<any>>('/list', { params })
    return response.data.data
  },

  // 3.2 日志详情查询
  async getLogDetail(logId: string): Promise<SystemLog> {
    const response = await logApiInstance.get<ApiResponse<SystemLog>>(`/detail/${logId}`)
    return response.data.data
  },

  // 3.3 实时日志查询
  async getRealtimeLogs(params: {
    level?: string
    category?: string
    vmId?: string
    taskId?: string
    tail?: number
  } = {}): Promise<{
    logs: SystemLog[]
    totalCount: number
    lastUpdateTime: string
  }> {
    const response = await logApiInstance.get<ApiResponse<{
      logs: SystemLog[]
      totalCount: number
      lastUpdateTime: string
    }>>('/realtime', { params })
    return response.data.data
  },

  // 3.4 日志统计查询
  async getLogStatistics(params: {
    startTime?: string
    endTime?: string
    category?: string
    vmId?: string
    taskId?: string
  } = {}): Promise<{
    totalLogs: number
    levelDistribution: {
      DEBUG: number
      INFO: number
      WARN: number
      ERROR: number
    }
    categoryDistribution: {
      SYSTEM: number
      USER: number
      VM: number
      TASK: number
      DATA: number
      MODEL: number
      SECURITY: number
      PERFORMANCE: number
    }
    timeDistribution: Array<{
      hour: string
      count: number
    }>
    errorTrend: Array<{
      date: string
      errorCount: number
    }>
  }> {
    const response = await logApiInstance.get<ApiResponse<{
      totalLogs: number
      levelDistribution: {
        DEBUG: number
        INFO: number
        WARN: number
        ERROR: number
      }
      categoryDistribution: {
        SYSTEM: number
        USER: number
        VM: number
        TASK: number
        DATA: number
        MODEL: number
        SECURITY: number
        PERFORMANCE: number
      }
      timeDistribution: Array<{
        hour: string
        count: number
      }>
      errorTrend: Array<{
        date: string
        errorCount: number
      }>
    }>>('/statistics', { params })
    return response.data.data
  },

  // ==================== 4. 日志导出接口 ====================
  
  // 4.1 日志导出
  async exportLogs(exportData: {
    level?: string
    category?: string
    vmId?: string
    taskId?: string
    startTime?: string
    endTime?: string
    keyword?: string
    format?: 'CSV' | 'JSON' | 'EXCEL'
    includeDetails?: boolean
  }): Promise<{
    exportId: string
    status: string
    estimatedTime: number
    downloadUrl: string
  }> {
    const response = await logApiInstance.post<ApiResponse<{
      exportId: string
      status: string
      estimatedTime: number
      downloadUrl: string
    }>>('/export', exportData)
    return response.data.data
  },

  // 4.2 导出状态查询
  async getExportStatus(exportId: string): Promise<ExportTask> {
    const response = await logApiInstance.get<ApiResponse<ExportTask>>(`/export/status/${exportId}`)
    return response.data.data
  },

  // 4.3 导出文件下载
  async downloadExportFile(exportId: string): Promise<Blob> {
    const response = await logApiInstance.get(`/export/download/${exportId}`, {
      responseType: 'blob'
    })
    return response.data
  },

  // 4.4 导出历史查询
  async getExportHistory(params: {
    status?: string
    page?: number
    size?: number
  } = {}): Promise<PaginatedResponse<ExportTask>> {
    const response = await logApiInstance.get<ApiResponse<PaginatedResponse<ExportTask>>>('/export/history', { params })
    return response.data.data
  },

  // ==================== 5. 日志清理接口 ====================
  
  // 5.1 日志清理
  async cleanupLogs(cleanupData: {
    strategy: 'TIME_BASED' | 'LEVEL_BASED' | 'SIZE_BASED'
    retentionDays?: number
    level?: string
    maxSizeGB?: number
    category?: string
    vmId?: string
    taskId?: string
    dryRun?: boolean
  }): Promise<{
    cleanupId: string
    status: string
    estimatedRecords: number
    estimatedSize: number
    dryRun: boolean
  }> {
    const response = await logApiInstance.post<ApiResponse<{
      cleanupId: string
      status: string
      estimatedRecords: number
      estimatedSize: number
      dryRun: boolean
    }>>('/cleanup', cleanupData)
    return response.data.data
  },

  // 5.2 清理状态查询
  async getCleanupStatus(cleanupId: string): Promise<CleanupTask> {
    const response = await logApiInstance.get<ApiResponse<CleanupTask>>(`/cleanup/status/${cleanupId}`)
    return response.data.data
  },

  // 5.3 清理历史查询
  async getCleanupHistory(params: {
    status?: string
    page?: number
    size?: number
  } = {}): Promise<PaginatedResponse<CleanupTask>> {
    const response = await logApiInstance.get<ApiResponse<PaginatedResponse<CleanupTask>>>('/cleanup/history', { params })
    return response.data.data
  },

  // ==================== 6. 系统监控接口 ====================
  
  // 6.1 系统状态监控
  async getSystemMonitor(): Promise<SystemMonitor> {
    const response = await logApiInstance.get<ApiResponse<SystemMonitor>>('/monitor/system')
    return response.data.data
  },

  // 6.2 日志监控
  async getLogMonitor(params: {
    timeRange?: '1h' | '6h' | '24h' | '7d'
    level?: string
  } = {}): Promise<LogMonitor> {
    const response = await logApiInstance.get<ApiResponse<LogMonitor>>('/monitor/logs', { params })
    return response.data.data
  },

  // 6.3 性能监控
  async getPerformanceMonitor(params: {
    timeRange?: '1h' | '6h' | '24h' | '7d'
    endpoint?: string
  } = {}): Promise<PerformanceMonitor> {
    const response = await logApiInstance.get<ApiResponse<PerformanceMonitor>>('/monitor/performance', { params })
    return response.data.data
  },

  // 6.4 告警配置
  async getAlertConfig(): Promise<AlertConfig> {
    const response = await logApiInstance.get<ApiResponse<AlertConfig>>('/monitor/alerts')
    return response.data.data
  },

  // ==================== 7. 日志配置接口 ====================
  
  // 7.1 日志配置查询
  async getLogConfig(): Promise<LogConfig> {
    const response = await logApiInstance.get<ApiResponse<LogConfig>>('/config')
    return response.data.data
  },

  // 7.2 日志配置更新
  async updateLogConfig(configData: {
    logLevel?: string
    retentionDays?: number
    maxFileSize?: number
    categories?: Record<string, {
      level: string
      enabled: boolean
    }>
    exportSettings?: {
      maxRecordsPerExport: number
      exportRetentionDays: number
    }
  }): Promise<{
    updatedAt: string
  }> {
    const response = await logApiInstance.put<ApiResponse<{
      updatedAt: string
    }>>('/config', configData)
    return response.data.data
  },
} as const

// 使用命名导出以保持一致性 