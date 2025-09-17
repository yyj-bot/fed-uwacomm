/**
 * 系统监控状态管理 Store
 * 管理系统日志、监控数据、配置等系统相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { systemLogService } from '@/services'
import type { SystemLog, PaginatedResponse } from '@/types'
import type { 
  SystemLogListParams,
  RealtimeLogsParams,
  LogExportData,
  LogCleanupData
} from '@/services'

// 简化的类型定义
interface LogStatisticsParams {
  startDate?: string
  endDate?: string
  level?: string
  category?: string
}

interface LogConfigUpdateData {
  logLevel?: string
  retentionDays?: number
  maxFileSize?: number
  enableRotation?: boolean
}

// 使用 any 类型来避免类型冲突
type SystemMonitor = any
type LogMonitor = any
type PerformanceMonitor = any
type AlertConfig = any
type LogConfig = any
type ExportTask = any
type CleanupTask = any

// ==================== 状态类型定义 ====================

interface SystemState {
  // 系统日志状态
  logList: SystemLog[]
  logListTotal: number
  logListLoading: boolean
  logListError: string | null
  
  // 实时日志
  realtimeLogs: SystemLog[]
  realtimeLogsLoading: boolean
  
  // 日志统计
  logStatistics: any | null
  logStatisticsLoading: boolean
  
  // 系统监控数据
  systemMonitor: SystemMonitor | null
  systemMonitorLoading: boolean
  
  // 日志监控
  logMonitor: LogMonitor | null
  logMonitorLoading: boolean
  
  // 性能监控
  performanceMonitor: PerformanceMonitor | null
  performanceMonitorLoading: boolean
  
  // 告警配置
  alertConfig: AlertConfig | null
  alertConfigLoading: boolean
  
  // 日志配置
  logConfig: LogConfig | null
  logConfigLoading: boolean
  
  // 导出任务
  exportTasks: Record<string, ExportTask>
  
  // 清理任务
  cleanupTasks: Record<string, CleanupTask>
  
  // 操作状态
  operationLoading: Record<string, boolean>
  operationError: Record<string, string | null>
  
  // 分页参数
  pagination: {
    page: number
    size: number
    total: number
  }
  
  // 查询参数
  queryParams: SystemLogListParams
  
  // 自动刷新
  autoRefresh: boolean
  refreshInterval: number
}

interface SystemActions {
  // 日志查询操作
  fetchLogList: (params?: SystemLogListParams) => Promise<void>
  refreshLogList: () => Promise<void>
  fetchLogDetail: (logId: string) => Promise<SystemLog>
  
  // 实时日志
  fetchRealtimeLogs: (params?: RealtimeLogsParams) => Promise<void>
  
  // 日志统计
  fetchLogStatistics: (params?: LogStatisticsParams) => Promise<void>
  
  // 日志导出
  downloadLogs: (exportData: LogExportData) => Promise<void>
  
  // 日志清理
  cleanupLogs: (cleanupData: LogCleanupData) => Promise<string>
  getCleanupStatus: (cleanupId: string) => Promise<void>
  
  // 系统监控
  fetchSystemMonitor: () => Promise<void>
  fetchLogMonitor: (params?: any) => Promise<void>
  fetchPerformanceMonitor: (params?: any) => Promise<void>
  
  // 配置管理
  fetchAlertConfig: () => Promise<void>
  fetchLogConfig: () => Promise<void>
  updateLogConfig: (configData: LogConfigUpdateData) => Promise<void>
  
  // 分页操作
  setPagination: (page: number, size?: number) => void
  
  // 查询参数操作
  setQueryParams: (params: SystemLogListParams) => void
  resetQueryParams: () => void
  
  // 自动刷新控制
  setAutoRefresh: (enabled: boolean) => void
  setRefreshInterval: (interval: number) => void
  
  // 错误处理
  clearError: () => void
  
  // 状态重置
  resetState: () => void
}

type SystemStore = SystemState & SystemActions

// ==================== 初始状态 ====================

const initialState: SystemState = {
  logList: [],
  logListTotal: 0,
  logListLoading: false,
  logListError: null,
  
  realtimeLogs: [],
  realtimeLogsLoading: false,
  
  logStatistics: null,
  logStatisticsLoading: false,
  
  systemMonitor: null,
  systemMonitorLoading: false,
  
  logMonitor: null,
  logMonitorLoading: false,
  
  performanceMonitor: null,
  performanceMonitorLoading: false,
  
  alertConfig: null,
  alertConfigLoading: false,
  
  logConfig: null,
  logConfigLoading: false,
  
  exportTasks: {},
  cleanupTasks: {},
  
  operationLoading: {},
  operationError: {},
  
  pagination: {
    page: 1,
    size: 50,
    total: 0
  },
  
  queryParams: {},
  
  autoRefresh: false,
  refreshInterval: 30000 // 30秒
}

// ==================== Store 实现 ====================

export const useSystemStore = create<SystemStore>((set, get) => ({
  ...initialState,

  // ==================== 日志查询操作 ====================
  
  /**
   * 获取系统日志列表
   */
  fetchLogList: async (params?: SystemLogListParams) => {
    const { pagination, queryParams } = get()
    const finalParams = {
      ...queryParams,
      ...params,
      page: params?.page || pagination.page,
      size: params?.size || pagination.size
    }
    
    set({ logListLoading: true, logListError: null })
    
    try {
      const response = await systemLogService.getLogList(finalParams)
      
      set({
        logList: response.records,
        logListTotal: response.total,
        logListLoading: false,
        logListError: null,
        pagination: {
          page: response.current,
          size: response.size,
          total: response.total
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取日志列表失败'
      set({
        logListLoading: false,
        logListError: errorMessage
      })
      throw error
    }
  },

  /**
   * 刷新日志列表
   */
  refreshLogList: async () => {
    const { fetchLogList, queryParams, pagination } = get()
    await fetchLogList({
      ...queryParams,
      page: pagination.page,
      size: pagination.size
    })
  },

  /**
   * 获取日志详情
   */
  fetchLogDetail: async (logId: string) => {
    try {
      const logDetail = await systemLogService.getLogDetail(logId)
      return logDetail
    } catch (error) {
      throw error
    }
  },

  // ==================== 实时日志 ====================
  
  /**
   * 获取实时日志
   */
  fetchRealtimeLogs: async (params?: RealtimeLogsParams) => {
    set({ realtimeLogsLoading: true })
    
    try {
      const response = await systemLogService.getRealtimeLogs(params)
      
      set({
        realtimeLogs: response.logs,
        realtimeLogsLoading: false
      })
    } catch (error) {
      set({ realtimeLogsLoading: false })
      throw error
    }
  },

  // ==================== 日志统计 ====================
  
  /**
   * 获取日志统计
   */
  fetchLogStatistics: async (params?: LogStatisticsParams) => {
    set({ logStatisticsLoading: true })
    
    try {
      const statistics = await systemLogService.getLogStatistics(params)
      
      set({
        logStatistics: statistics,
        logStatisticsLoading: false
      })
    } catch (error) {
      set({ logStatisticsLoading: false })
      throw error
    }
  },

  // ==================== 日志导出 ====================
  
  /**
   * 同步下载日志
   */
  downloadLogs: async (exportData: LogExportData) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        'download-logs': true
      },
      operationError: {
        ...state.operationError,
        'download-logs': null
      }
    }))
    
    try {
      await systemLogService.downloadLogs(exportData)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'download-logs': false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '下载日志失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'download-logs': false
        },
        operationError: {
          ...state.operationError,
          'download-logs': errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 日志清理 ====================
  
  /**
   * 清理日志
   */
  cleanupLogs: async (cleanupData: LogCleanupData) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        'cleanup-logs': true
      },
      operationError: {
        ...state.operationError,
        'cleanup-logs': null
      }
    }))
    
    try {
      const response = await systemLogService.cleanupLogs(cleanupData)
      
      set((state) => ({
        cleanupTasks: {
          ...state.cleanupTasks,
          [response.cleanupId]: {
            cleanupId: response.cleanupId,
            status: response.status,
            estimatedRecords: response.estimatedRecords,
            estimatedSize: response.estimatedSize,
            createdAt: new Date().toISOString(),
            strategy: cleanupData.strategy
          }
        },
        operationLoading: {
          ...state.operationLoading,
          'cleanup-logs': false
        }
      }))
      
      return response.cleanupId
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '清理日志失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'cleanup-logs': false
        },
        operationError: {
          ...state.operationError,
          'cleanup-logs': errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 获取清理状态
   */
  getCleanupStatus: async (cleanupId: string) => {
    try {
      const task = await systemLogService.getCleanupStatus(cleanupId)
      
      set((state) => ({
        cleanupTasks: {
          ...state.cleanupTasks,
          [cleanupId]: task
        }
      }))
    } catch (error) {
      console.error(`获取清理状态失败 (${cleanupId}):`, error)
    }
  },

  // ==================== 系统监控 ====================
  
  /**
   * 获取系统监控数据
   */
  fetchSystemMonitor: async () => {
    set({ systemMonitorLoading: true })
    
    try {
      const monitor = await systemLogService.getSystemMonitor()
      
      set({
        systemMonitor: monitor,
        systemMonitorLoading: false
      })
    } catch (error) {
      set({ systemMonitorLoading: false })
      throw error
    }
  },

  /**
   * 获取日志监控数据
   */
  fetchLogMonitor: async (params?: any) => {
    set({ logMonitorLoading: true })
    
    try {
      const monitor = await systemLogService.getLogMonitor(params)
      
      set({
        logMonitor: monitor,
        logMonitorLoading: false
      })
    } catch (error) {
      set({ logMonitorLoading: false })
      throw error
    }
  },

  /**
   * 获取性能监控数据
   */
  fetchPerformanceMonitor: async (params?: any) => {
    set({ performanceMonitorLoading: true })
    
    try {
      const monitor = await systemLogService.getPerformanceMonitor(params)
      
      set({
        performanceMonitor: monitor,
        performanceMonitorLoading: false
      })
    } catch (error) {
      set({ performanceMonitorLoading: false })
      throw error
    }
  },

  // ==================== 配置管理 ====================
  
  /**
   * 获取告警配置
   */
  fetchAlertConfig: async () => {
    set({ alertConfigLoading: true })
    
    try {
      const config = await systemLogService.getAlertConfig()
      
      set({
        alertConfig: config,
        alertConfigLoading: false
      })
    } catch (error) {
      set({ alertConfigLoading: false })
      throw error
    }
  },

  /**
   * 获取日志配置
   */
  fetchLogConfig: async () => {
    set({ logConfigLoading: true })
    
    try {
      const config = await systemLogService.getLogConfig()
      
      set({
        logConfig: config,
        logConfigLoading: false
      })
    } catch (error) {
      set({ logConfigLoading: false })
      throw error
    }
  },

  /**
   * 更新日志配置
   */
  updateLogConfig: async (configData: LogConfigUpdateData) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        'update-log-config': true
      },
      operationError: {
        ...state.operationError,
        'update-log-config': null
      }
    }))
    
    try {
      await systemLogService.updateLogConfig(configData)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'update-log-config': false
        }
      }))
      
      // 重新获取配置
      await get().fetchLogConfig()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新日志配置失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'update-log-config': false
        },
        operationError: {
          ...state.operationError,
          'update-log-config': errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 分页操作 ====================
  
  /**
   * 设置分页参数
   */
  setPagination: (page: number, size?: number) => {
    set((state) => ({
      pagination: {
        ...state.pagination,
        page,
        size: size || state.pagination.size
      }
    }))
  },

  // ==================== 查询参数操作 ====================
  
  /**
   * 设置查询参数
   */
  setQueryParams: (params: SystemLogListParams) => {
    set({ queryParams: params })
  },

  /**
   * 重置查询参数
   */
  resetQueryParams: () => {
    set({ queryParams: {} })
  },

  // ==================== 自动刷新控制 ====================
  
  /**
   * 设置自动刷新
   */
  setAutoRefresh: (enabled: boolean) => {
    set({ autoRefresh: enabled })
  },

  /**
   * 设置刷新间隔
   */
  setRefreshInterval: (interval: number) => {
    set({ refreshInterval: interval })
  },

  // ==================== 错误处理 ====================
  
  /**
   * 清除错误信息
   */
  clearError: () => {
    set({
      logListError: null,
      operationError: {}
    })
  },

  // ==================== 状态重置 ====================
  
  /**
   * 重置状态
   */
  resetState: () => {
    set(initialState)
  }
}))

// ==================== 导出类型 ====================
export type { SystemState, SystemActions, SystemStore }
