/**
 * 系统监控 Hook - 封装系统监控状态和操作
 * 为组件层提供简洁的系统监控功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback, useEffect } from 'react'
import { useSystemStore } from './systemStore'
import type { SystemLog } from '@/types'
import type { 
  SystemLogListParams,
  RealtimeLogsParams,
  LogExportData,
  LogCleanupData
} from '@/services'

// 临时类型定义，应该从 services 中导入
interface LogStatisticsParams {
  startDate?: string
  endDate?: string
  level?: string
  category?: string
}

interface LogConfigUpdateData {
  logLevel?: string
  retentionDays?: number
  maxFileSize?: string
  enableRotation?: boolean
}

interface SystemMonitor {
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  networkTraffic: number
  uptime: number
  systemLoad: number
}

interface LogMonitor {
  totalLogs: number
  errorCount: number
  warningCount: number
  infoCount: number
  logRate: number
  averageResponseTime: number
}

interface PerformanceMonitor {
  responseTime: number[]
  throughput: number[]
  errorRate: number[]
  timestamps: string[]
}

interface AlertConfig {
  enabled: boolean
  rules: any[]
  notifications: any[]
}

interface LogConfig {
  level: string
  retentionDays: number
  maxFileSize: string
  enableRotation: boolean
  categories: string[]
}

interface ExportTask {
  exportId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  estimatedTime?: number
  downloadUrl?: string
  createdAt: string
  format: string
}

interface CleanupTask {
  cleanupId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  estimatedRecords?: number
  estimatedSize?: string
  dryRun?: boolean
  createdAt: string
  strategy: string
}

// ==================== Hook 实现 ====================

export const useSystem = () => {
  // 获取状态
  const logList = useSystemStore((state) => state.logList)
  const logListTotal = useSystemStore((state) => state.logListTotal)
  const logListLoading = useSystemStore((state) => state.logListLoading)
  const logListError = useSystemStore((state) => state.logListError)
  
  const realtimeLogs = useSystemStore((state) => state.realtimeLogs)
  const realtimeLogsLoading = useSystemStore((state) => state.realtimeLogsLoading)
  
  const logStatistics = useSystemStore((state) => state.logStatistics)
  const logStatisticsLoading = useSystemStore((state) => state.logStatisticsLoading)
  
  const systemMonitor = useSystemStore((state) => state.systemMonitor)
  const systemMonitorLoading = useSystemStore((state) => state.systemMonitorLoading)
  
  const logMonitor = useSystemStore((state) => state.logMonitor)
  const logMonitorLoading = useSystemStore((state) => state.logMonitorLoading)
  
  const performanceMonitor = useSystemStore((state) => state.performanceMonitor)
  const performanceMonitorLoading = useSystemStore((state) => state.performanceMonitorLoading)
  
  const alertConfig = useSystemStore((state) => state.alertConfig)
  const alertConfigLoading = useSystemStore((state) => state.alertConfigLoading)
  
  const logConfig = useSystemStore((state) => state.logConfig)
  const logConfigLoading = useSystemStore((state) => state.logConfigLoading)
  
  const exportTasks = useSystemStore((state) => state.exportTasks)
  const cleanupTasks = useSystemStore((state) => state.cleanupTasks)
  
  const operationLoading = useSystemStore((state) => state.operationLoading)
  const operationError = useSystemStore((state) => state.operationError)
  
  const pagination = useSystemStore((state) => state.pagination)
  const queryParams = useSystemStore((state) => state.queryParams)
  const autoRefresh = useSystemStore((state) => state.autoRefresh)
  const refreshInterval = useSystemStore((state) => state.refreshInterval)

  // 获取操作方法
  const fetchLogListAction = useSystemStore((state) => state.fetchLogList)
  const refreshLogListAction = useSystemStore((state) => state.refreshLogList)
  const fetchLogDetailAction = useSystemStore((state) => state.fetchLogDetail)
  const fetchRealtimeLogsAction = useSystemStore((state) => state.fetchRealtimeLogs)
  const fetchLogStatisticsAction = useSystemStore((state) => state.fetchLogStatistics)
  const downloadLogsAction = useSystemStore((state) => state.downloadLogs)
  const cleanupLogsAction = useSystemStore((state) => state.cleanupLogs)
  const getCleanupStatusAction = useSystemStore((state) => state.getCleanupStatus)
  const fetchSystemMonitorAction = useSystemStore((state) => state.fetchSystemMonitor)
  const fetchLogMonitorAction = useSystemStore((state) => state.fetchLogMonitor)
  const fetchPerformanceMonitorAction = useSystemStore((state) => state.fetchPerformanceMonitor)
  const fetchAlertConfigAction = useSystemStore((state) => state.fetchAlertConfig)
  const fetchLogConfigAction = useSystemStore((state) => state.fetchLogConfig)
  const updateLogConfigAction = useSystemStore((state) => state.updateLogConfig)
  const setPaginationAction = useSystemStore((state) => state.setPagination)
  const setQueryParamsAction = useSystemStore((state) => state.setQueryParams)
  const resetQueryParamsAction = useSystemStore((state) => state.resetQueryParams)
  const setAutoRefreshAction = useSystemStore((state) => state.setAutoRefresh)
  const setRefreshIntervalAction = useSystemStore((state) => state.setRefreshInterval)
  const clearErrorAction = useSystemStore((state) => state.clearError)
  const resetStateAction = useSystemStore((state) => state.resetState)

  // ==================== 封装操作方法 ====================

  /**
   * 获取日志列表
   */
  const fetchLogList = useCallback(async (params?: SystemLogListParams) => {
    try {
      await fetchLogListAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取日志列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchLogListAction])

  /**
   * 刷新日志列表
   */
  const refreshLogList = useCallback(async () => {
    try {
      await refreshLogListAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新日志列表失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshLogListAction])

  /**
   * 获取日志详情
   */
  const fetchLogDetail = useCallback(async (logId: string) => {
    try {
      const logDetail = await fetchLogDetailAction(logId)
      return { success: true, error: null, data: logDetail }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取日志详情失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [fetchLogDetailAction])

  /**
   * 获取实时日志
   */
  const fetchRealtimeLogs = useCallback(async (params?: RealtimeLogsParams) => {
    try {
      await fetchRealtimeLogsAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取实时日志失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchRealtimeLogsAction])

  /**
   * 获取日志统计
   */
  const fetchLogStatistics = useCallback(async (params?: LogStatisticsParams) => {
    try {
      await fetchLogStatisticsAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取日志统计失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchLogStatisticsAction])

  /**
   * 下载日志
   */
  const downloadLogs = useCallback(async (exportData: LogExportData) => {
    try {
      await downloadLogsAction(exportData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '下载日志失败'
      return { success: false, error: errorMessage }
    }
  }, [downloadLogsAction])

  /**
   * 清理日志
   */
  const cleanupLogs = useCallback(async (cleanupData: LogCleanupData) => {
    try {
      const cleanupId = await cleanupLogsAction(cleanupData)
      return { success: true, error: null, data: cleanupId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '清理日志失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [cleanupLogsAction])

  /**
   * 获取清理状态
   */
  const getCleanupStatus = useCallback(async (cleanupId: string) => {
    try {
      await getCleanupStatusAction(cleanupId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取清理状态失败'
      return { success: false, error: errorMessage }
    }
  }, [getCleanupStatusAction])

  /**
   * 获取系统监控数据
   */
  const fetchSystemMonitor = useCallback(async () => {
    try {
      await fetchSystemMonitorAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取系统监控数据失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchSystemMonitorAction])

  /**
   * 获取日志监控数据
   */
  const fetchLogMonitor = useCallback(async (params?: any) => {
    try {
      await fetchLogMonitorAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取日志监控数据失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchLogMonitorAction])

  /**
   * 获取性能监控数据
   */
  const fetchPerformanceMonitor = useCallback(async (params?: any) => {
    try {
      await fetchPerformanceMonitorAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取性能监控数据失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchPerformanceMonitorAction])

  /**
   * 获取告警配置
   */
  const fetchAlertConfig = useCallback(async () => {
    try {
      await fetchAlertConfigAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取告警配置失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchAlertConfigAction])

  /**
   * 获取日志配置
   */
  const fetchLogConfig = useCallback(async () => {
    try {
      await fetchLogConfigAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取日志配置失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchLogConfigAction])

  /**
   * 更新日志配置
   */
  const updateLogConfig = useCallback(async (configData: LogConfigUpdateData) => {
    try {
      // 确保 maxFileSize 是数字类型
      const processedConfigData = {
        ...configData,
        maxFileSize: typeof configData.maxFileSize === 'string' 
          ? parseInt(configData.maxFileSize, 10) 
          : configData.maxFileSize
      }
      await updateLogConfigAction(processedConfigData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新日志配置失败'
      return { success: false, error: errorMessage }
    }
  }, [updateLogConfigAction])

  /**
   * 设置分页参数
   */
  const setPagination = useCallback((page: number, size?: number) => {
    setPaginationAction(page, size)
  }, [setPaginationAction])

  /**
   * 设置查询参数
   */
  const setQueryParams = useCallback((params: SystemLogListParams) => {
    setQueryParamsAction(params)
  }, [setQueryParamsAction])

  /**
   * 重置查询参数
   */
  const resetQueryParams = useCallback(() => {
    resetQueryParamsAction()
  }, [resetQueryParamsAction])

  /**
   * 设置自动刷新
   */
  const setAutoRefresh = useCallback((enabled: boolean) => {
    setAutoRefreshAction(enabled)
  }, [setAutoRefreshAction])

  /**
   * 设置刷新间隔
   */
  const setRefreshInterval = useCallback((interval: number) => {
    setRefreshIntervalAction(interval)
  }, [setRefreshIntervalAction])

  /**
   * 清除错误信息
   */
  const clearError = useCallback(() => {
    clearErrorAction()
  }, [clearErrorAction])

  /**
   * 重置状态
   */
  const resetState = useCallback(() => {
    resetStateAction()
  }, [resetStateAction])

  // ==================== 计算属性 ====================

  /**
   * 获取导出任务
   */
  const getExportTask = useCallback((exportId: string): ExportTask | null => {
    return exportTasks[exportId] || null
  }, [exportTasks])

  /**
   * 获取清理任务
   */
  const getCleanupTask = useCallback((cleanupId: string): CleanupTask | null => {
    return cleanupTasks[cleanupId] || null
  }, [cleanupTasks])

  /**
   * 获取不同级别的日志数量
   */
  const getLogCountByLevel = useCallback((level: string): number => {
    return logList.filter(log => log.level === level).length
  }, [logList])

  /**
   * 获取不同类别的日志数量
   */
  const getLogCountByCategory = useCallback((category: string): number => {
    return logList.filter(log => log.category === category).length
  }, [logList])

  /**
   * 检查是否有错误日志
   */
  const hasErrorLogs = useCallback((): boolean => {
    return logList.some(log => log.level === 'ERROR')
  }, [logList])

  /**
   * 检查是否有警告日志
   */
  const hasWarningLogs = useCallback((): boolean => {
    return logList.some(log => log.level === 'WARN')
  }, [logList])

  /**
   * 获取最近的错误日志
   */
  const getRecentErrorLogs = useCallback((limit = 10): SystemLog[] => {
    return logList
      .filter(log => log.level === 'ERROR')
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, limit)
  }, [logList])

  /**
   * 检查是否有任何操作正在进行
   */
  const isAnyOperationLoading = Object.values(operationLoading).some(loading => loading)

  /**
   * 检查下载是否正在进行
   */
  const isDownloading = !!operationLoading['download-logs']

  /**
   * 检查清理是否正在进行
   */
  const isCleaning = !!operationLoading['cleanup-logs']

  /**
   * 检查配置更新是否正在进行
   */
  const isUpdatingConfig = !!operationLoading['update-log-config']

  // ==================== 自动刷新逻辑 ====================

  useEffect(() => {
    let intervalId: NodeJS.Timeout | null = null
    
    if (autoRefresh && refreshInterval > 0) {
      intervalId = setInterval(() => {
        refreshLogList().catch(console.error)
        fetchSystemMonitor().catch(console.error)
        fetchLogMonitor().catch(console.error)
        fetchPerformanceMonitor().catch(console.error)
      }, refreshInterval)
    }
    
    return () => {
      if (intervalId) {
        clearInterval(intervalId)
      }
    }
  }, [autoRefresh, refreshInterval, refreshLogList, fetchSystemMonitor, fetchLogMonitor, fetchPerformanceMonitor])

  // ==================== 返回接口 ====================

  return {
    // 状态
    logList,
    logListTotal,
    logListLoading,
    logListError,
    realtimeLogs,
    realtimeLogsLoading,
    logStatistics,
    logStatisticsLoading,
    systemMonitor,
    systemMonitorLoading,
    logMonitor,
    logMonitorLoading,
    performanceMonitor,
    performanceMonitorLoading,
    alertConfig,
    alertConfigLoading,
    logConfig,
    logConfigLoading,
    exportTasks,
    cleanupTasks,
    operationLoading,
    operationError,
    pagination,
    queryParams,
    autoRefresh,
    refreshInterval,
    
    // 操作方法
    fetchLogList,
    refreshLogList,
    fetchLogDetail,
    fetchRealtimeLogs,
    fetchLogStatistics,
    downloadLogs,
    cleanupLogs,
    getCleanupStatus,
    fetchSystemMonitor,
    fetchLogMonitor,
    fetchPerformanceMonitor,
    fetchAlertConfig,
    fetchLogConfig,
    updateLogConfig,
    setPagination,
    setQueryParams,
    resetQueryParams,
    setAutoRefresh,
    setRefreshInterval,
    clearError,
    resetState,
    
    // 计算属性和工具方法
    getExportTask,
    getCleanupTask,
    getLogCountByLevel,
    getLogCountByCategory,
    hasErrorLogs,
    hasWarningLogs,
    getRecentErrorLogs,
    isAnyOperationLoading,
    isDownloading,
    isCleaning,
    isUpdatingConfig
  }
}

// ==================== 导出默认 Hook ====================
export default useSystem
