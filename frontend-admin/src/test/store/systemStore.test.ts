/**
 * System Store 测试
 * 测试系统监控状态管理的各种功能，包括日志管理、系统监控、错误处理等
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useSystemStore } from '@/store/system/systemStore'
import { systemLogService } from '@/services'
import {
  mockSystemLogList,
  mockSystemLog,
  mockRealtimeLogs,
  mockLogStatistics,
  mockSystemErrors,
  mockInitialSystemState
} from '@/mocks/store/systemStoreMock'

// Mock systemLogService
vi.mock('@/services', () => ({
  systemLogService: {
    getLogList: vi.fn(),
    getLogDetail: vi.fn(),
    getRealtimeLogs: vi.fn(),
    getLogStatistics: vi.fn(),
    exportLogs: vi.fn(),
    getExportStatus: vi.fn(),
    downloadExportFile: vi.fn(),
    cleanupLogs: vi.fn(),
    getCleanupStatus: vi.fn(),
    getSystemMonitor: vi.fn(),
    getLogMonitor: vi.fn(),
    getPerformanceMonitor: vi.fn(),
    getAlertConfig: vi.fn(),
    getLogConfig: vi.fn(),
    updateLogConfig: vi.fn()
  }
}))

const mocked = vi.mocked

describe('SystemStore', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 重置store状态
    const store = useSystemStore.getState()
    store.resetState()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      const state = useSystemStore.getState()
      
      expect(state.logList).toEqual([])
      expect(state.logListTotal).toBe(0)
      expect(state.logListLoading).toBe(false)
      expect(state.logListError).toBe(null)
      expect(state.realtimeLogs).toEqual([])
      expect(state.realtimeLogsLoading).toBe(false)
      expect(state.logStatistics).toBe(null)
      expect(state.logStatisticsLoading).toBe(false)
      expect(state.systemMonitor).toBe(null)
      expect(state.systemMonitorLoading).toBe(false)
      expect(state.logMonitor).toBe(null)
      expect(state.logMonitorLoading).toBe(false)
      expect(state.performanceMonitor).toBe(null)
      expect(state.performanceMonitorLoading).toBe(false)
      expect(state.alertConfig).toBe(null)
      expect(state.alertConfigLoading).toBe(false)
      expect(state.logConfig).toBe(null)
      expect(state.logConfigLoading).toBe(false)
      expect(state.exportTasks).toEqual({})
      expect(state.cleanupTasks).toEqual({})
      expect(state.operationLoading).toEqual({})
      expect(state.operationError).toEqual({})
      expect(state.pagination).toEqual({
        page: 1,
        size: 50,
        total: 0
      })
      expect(state.queryParams).toEqual({})
      expect(state.autoRefresh).toBe(false)
      expect(state.refreshInterval).toBe(30000)
    })
  })

  describe('fetchLogList', () => {
    it('应该成功获取日志列表', async () => {
      const mockResponse = {
        total: mockSystemLogList.length,
        pages: Math.ceil(mockSystemLogList.length / 50),
        current: 1,
        size: 50,
        records: mockSystemLogList
      }
      
      mocked(systemLogService.getLogList).mockResolvedValue(mockResponse)
      
      const store = useSystemStore.getState()
      await store.fetchLogList()
      
      const state = useSystemStore.getState()
      expect(state.logList).toEqual(mockSystemLogList)
      expect(state.logListTotal).toBe(mockSystemLogList.length)
      expect(state.logListLoading).toBe(false)
      expect(state.logListError).toBe(null)
      expect(state.pagination.page).toBe(1)
      expect(state.pagination.total).toBe(mockSystemLogList.length)
    })

    it('应该处理加载状态', async () => {
      const pendingPromise = new Promise<any>(() => {}) // Never resolves
      mocked(systemLogService.getLogList).mockReturnValue(pendingPromise)
      
      const store = useSystemStore.getState()
      store.fetchLogList()
      
      const state = useSystemStore.getState()
      expect(state.logListLoading).toBe(true)
      expect(state.logListError).toBe(null)
    })

    it('应该处理错误情况', async () => {
      const error = mockSystemErrors.FETCH_LOG_LIST_ERROR
      mocked(systemLogService.getLogList).mockRejectedValue(error)
      
      const store = useSystemStore.getState()
      
      await expect(store.fetchLogList()).rejects.toThrow(error)
      
      const state = useSystemStore.getState()
      expect(state.logListLoading).toBe(false)
      expect(state.logListError).toBe(error.message)
    })
  })

  describe('refreshLogList', () => {
    it('应该刷新日志列表', async () => {
      const mockResponse = {
        total: mockSystemLogList.length,
        pages: Math.ceil(mockSystemLogList.length / 50),
        current: 1,
        size: 50,
        records: mockSystemLogList
      }
      
      mocked(systemLogService.getLogList).mockResolvedValue(mockResponse)
      
      const store = useSystemStore.getState()
      await store.refreshLogList()
      
      const state = useSystemStore.getState()
      expect(state.logList).toEqual(mockSystemLogList)
      expect(systemLogService.getLogList).toHaveBeenCalledWith({
        page: 1,
        size: 50
      })
    })
  })

  describe('fetchLogDetail', () => {
    it('应该成功获取日志详情', async () => {
      mocked(systemLogService.getLogDetail).mockResolvedValue(mockSystemLog)
      
      const store = useSystemStore.getState()
      const result = await store.fetchLogDetail('log-001')
      
      expect(result).toEqual(mockSystemLog)
      expect(systemLogService.getLogDetail).toHaveBeenCalledWith('log-001')
    })

    it('应该处理获取详情失败', async () => {
      const error = mockSystemErrors.FETCH_LOG_DETAIL_ERROR
      mocked(systemLogService.getLogDetail).mockRejectedValue(error)
      
      const store = useSystemStore.getState()
      
      await expect(store.fetchLogDetail('log-001')).rejects.toThrow(error)
    })
  })

  describe('fetchRealtimeLogs', () => {
    it('应该成功获取实时日志', async () => {
      const mockResponse = {
        logs: mockRealtimeLogs,
        totalCount: mockRealtimeLogs.length,
        lastUpdateTime: '2024-01-15T10:00:00Z'
      }
      mocked(systemLogService.getRealtimeLogs).mockResolvedValue(mockResponse)
      
      const store = useSystemStore.getState()
      await store.fetchRealtimeLogs()
      
      const state = useSystemStore.getState()
      expect(state.realtimeLogs).toEqual(mockRealtimeLogs)
      expect(state.realtimeLogsLoading).toBe(false)
    })

    it('应该处理加载状态', async () => {
      const pendingPromise = new Promise<any>(() => {})
      mocked(systemLogService.getRealtimeLogs).mockReturnValue(pendingPromise)
      
      const store = useSystemStore.getState()
      store.fetchRealtimeLogs()
      
      const state = useSystemStore.getState()
      expect(state.realtimeLogsLoading).toBe(true)
    })
  })

  describe('fetchLogStatistics', () => {
    it('应该成功获取日志统计', async () => {
      mocked(systemLogService.getLogStatistics).mockResolvedValue(mockLogStatistics)
      
      const store = useSystemStore.getState()
      await store.fetchLogStatistics()
      
      const state = useSystemStore.getState()
      expect(state.logStatistics).toEqual(mockLogStatistics)
      expect(state.logStatisticsLoading).toBe(false)
    })

    it('应该处理加载状态', async () => {
      const pendingPromise = new Promise<any>(() => {})
      mocked(systemLogService.getLogStatistics).mockReturnValue(pendingPromise)
      
      const store = useSystemStore.getState()
      store.fetchLogStatistics()
      
      const state = useSystemStore.getState()
      expect(state.logStatisticsLoading).toBe(true)
    })
  })

  describe('exportLogs', () => {
    it('应该成功导出日志', async () => {
      const mockExportResponse = {
        exportId: 'export-001',
        status: 'PROCESSING',
        estimatedTime: 300,
        downloadUrl: null
      }
      
      mocked(systemLogService.exportLogs).mockResolvedValue(mockExportResponse)
      
      const store = useSystemStore.getState()
      const exportData = { format: 'JSON' as const, startDate: '2024-01-01' }
      
      const exportId = await store.exportLogs(exportData)
      
      expect(exportId).toBe('export-001')
      const state = useSystemStore.getState()
      expect(state.exportTasks['export-001']).toBeDefined()
      expect(state.operationLoading['export-logs']).toBe(false)
    })

    it('应该处理导出失败', async () => {
      const error = mockSystemErrors.EXPORT_LOGS_ERROR
      mocked(systemLogService.exportLogs).mockRejectedValue(error)
      
      const store = useSystemStore.getState()
      const exportData = { format: 'JSON' as const }
      
      await expect(store.exportLogs(exportData)).rejects.toThrow(error)
      
      const state = useSystemStore.getState()
      expect(state.operationLoading['export-logs']).toBe(false)
      expect(state.operationError['export-logs']).toBe(error.message)
    })
  })

  describe('cleanupLogs', () => {
    it('应该成功清理日志', async () => {
      const mockCleanupResponse = {
        cleanupId: 'cleanup-001',
        status: 'PROCESSING',
        estimatedRecords: 1000,
        estimatedSize: 10485760,
        dryRun: false
      }
      
      mocked(systemLogService.cleanupLogs).mockResolvedValue(mockCleanupResponse)
      
      const store = useSystemStore.getState()
      const cleanupData = { strategy: 'TIME_BASED' as const, retentionDays: 30 }
      
      const cleanupId = await store.cleanupLogs(cleanupData)
      
      expect(cleanupId).toBe('cleanup-001')
      const state = useSystemStore.getState()
      expect(state.cleanupTasks['cleanup-001']).toBeDefined()
      expect(state.operationLoading['cleanup-logs']).toBe(false)
    })
  })

  describe('系统监控', () => {
    it('应该成功获取系统监控数据', async () => {
      const mockMonitorData = {
        systemInfo: {
          version: '1.0.0',
          uptime: 3600,
          startTime: '2024-01-15T08:00:00Z',
          javaVersion: '11.0.2',
          osInfo: 'Ubuntu 20.04 LTS'
        },
        resourceUsage: {
          cpuUsage: 45.5,
          memoryUsage: 68.2,
          diskUsage: 35.8,
          networkIO: {
            bytesIn: 1000000,
            bytesOut: 800000
          }
        },
        applicationMetrics: {
          activeConnections: 150,
          requestPerSecond: 25.5,
          averageResponseTime: 120,
          errorRate: 0.5
        },
        databaseMetrics: {
          activeConnections: 10,
          queryPerSecond: 45.2,
          averageQueryTime: 15
        }
      }
      mocked(systemLogService.getSystemMonitor).mockResolvedValue(mockMonitorData)
      
      const store = useSystemStore.getState()
      await store.fetchSystemMonitor()
      
      const state = useSystemStore.getState()
      expect(state.systemMonitor).toEqual(mockMonitorData)
      expect(state.systemMonitorLoading).toBe(false)
    })

    it('应该成功获取日志监控数据', async () => {
      const mockLogMonitorData = {
        logMetrics: {
          totalLogs: 10000,
          errorCount: 250,
          warningCount: 830,
          errorRate: 2.5,
          warningRate: 8.3
        },
        levelTrend: [
          {
            timestamp: '2024-01-15T10:00:00Z',
            DEBUG: 100,
            INFO: 500,
            WARN: 50,
            ERROR: 10
          }
        ],
        categoryTrend: [
          {
            timestamp: '2024-01-15T10:00:00Z',
            SYSTEM: 100,
            USER: 200,
            VM: 150,
            TASK: 80
          }
        ],
        recentErrors: [
          {
            logId: 'log-error-001',
            level: 'ERROR',
            category: 'SYSTEM',
            message: 'Test error',
            createdAt: '2024-01-15T10:00:00Z'
          }
        ]
      }
      mocked(systemLogService.getLogMonitor).mockResolvedValue(mockLogMonitorData)
      
      const store = useSystemStore.getState()
      await store.fetchLogMonitor()
      
      const state = useSystemStore.getState()
      expect(state.logMonitor).toEqual(mockLogMonitorData)
      expect(state.logMonitorLoading).toBe(false)
    })

    it('应该成功获取性能监控数据', async () => {
      const mockPerformanceData = {
        apiMetrics: {
          totalRequests: 1000,
          successfulRequests: 950,
          failedRequests: 50,
          successRate: 95.0,
          averageResponseTime: 150,
          p95ResponseTime: 250,
          p99ResponseTime: 400
        },
        endpointMetrics: [
          {
            endpoint: '/api/logs',
            requestCount: 500,
            successRate: 98.0,
            averageResponseTime: 120,
            errorCount: 10
          }
        ],
        responseTimeTrend: [
          {
            timestamp: '2024-01-15T10:00:00Z',
            average: 150,
            p95: 250,
            p99: 400
          }
        ]
      }
      mocked(systemLogService.getPerformanceMonitor).mockResolvedValue(mockPerformanceData)
      
      const store = useSystemStore.getState()
      await store.fetchPerformanceMonitor()
      
      const state = useSystemStore.getState()
      expect(state.performanceMonitor).toEqual(mockPerformanceData)
      expect(state.performanceMonitorLoading).toBe(false)
    })
  })

  describe('配置管理', () => {
    it('应该成功获取告警配置', async () => {
      const mockAlertConfig = {
        alerts: [
          {
            alertId: 'alert-001',
            name: 'CPU使用率过高',
            type: 'CPU_HIGH',
            condition: 'cpu > 80%',
            status: 'ACTIVE',
            lastTriggered: '2024-01-15T10:00:00Z',
            triggerCount: 5
          }
        ],
        alertHistory: [
          {
            alertId: 'alert-001',
            triggeredAt: '2024-01-15T10:00:00Z',
            message: 'CPU使用率超过80%',
            severity: 'HIGH'
          }
        ]
      }
      mocked(systemLogService.getAlertConfig).mockResolvedValue(mockAlertConfig)
      
      const store = useSystemStore.getState()
      await store.fetchAlertConfig()
      
      const state = useSystemStore.getState()
      expect(state.alertConfig).toEqual(mockAlertConfig)
      expect(state.alertConfigLoading).toBe(false)
    })

    it('应该成功获取日志配置', async () => {
      const mockLogConfig = {
        logLevel: 'INFO',
        retentionDays: 30,
        maxFileSize: 1048576,
        categories: {
          SYSTEM: { level: 'INFO', enabled: true },
          USER: { level: 'WARN', enabled: true },
          VM: { level: 'ERROR', enabled: false }
        },
        exportSettings: {
          maxRecordsPerExport: 10000,
          exportRetentionDays: 7,
          supportedFormats: ['JSON', 'CSV', 'EXCEL']
        }
      }
      mocked(systemLogService.getLogConfig).mockResolvedValue(mockLogConfig)
      
      const store = useSystemStore.getState()
      await store.fetchLogConfig()
      
      const state = useSystemStore.getState()
      expect(state.logConfig).toEqual(mockLogConfig)
      expect(state.logConfigLoading).toBe(false)
    })

    it('应该成功更新日志配置', async () => {
      const mockLogConfig = {
        logLevel: 'DEBUG',
        retentionDays: 60,
        maxFileSize: 2097152,
        categories: {
          SYSTEM: { level: 'DEBUG', enabled: true },
          USER: { level: 'INFO', enabled: true },
          VM: { level: 'WARN', enabled: true },
          TASK: { level: 'ERROR', enabled: false }
        },
        exportSettings: {
          maxRecordsPerExport: 20000,
          exportRetentionDays: 14,
          supportedFormats: ['JSON', 'CSV', 'EXCEL']
        }
      }
      mocked(systemLogService.updateLogConfig).mockResolvedValue(undefined)
      mocked(systemLogService.getLogConfig).mockResolvedValue(mockLogConfig)
      
      const store = useSystemStore.getState()
      const configData = { logLevel: 'DEBUG', retentionDays: 60 }
      
      await store.updateLogConfig(configData)
      
      const state = useSystemStore.getState()
      expect(state.operationLoading['update-log-config']).toBe(false)
      expect(systemLogService.updateLogConfig).toHaveBeenCalledWith(configData)
    })
  })

  describe('分页操作', () => {
    it('应该设置分页参数', () => {
      const store = useSystemStore.getState()
      
      store.setPagination(2, 100)
      
      const state = useSystemStore.getState()
      expect(state.pagination.page).toBe(2)
      expect(state.pagination.size).toBe(100)
    })
  })

  describe('查询参数操作', () => {
    it('应该设置查询参数', () => {
      const store = useSystemStore.getState()
      const params = { level: 'ERROR', startTime: '2024-01-01' }
      
      store.setQueryParams(params)
      
      const state = useSystemStore.getState()
      expect(state.queryParams).toEqual(params)
    })

    it('应该重置查询参数', () => {
      const store = useSystemStore.getState()
      
      // 先设置一些参数
      store.setQueryParams({ level: 'ERROR' })
      
      store.resetQueryParams()
      
      const state = useSystemStore.getState()
      expect(state.queryParams).toEqual({})
    })
  })

  describe('自动刷新控制', () => {
    it('应该设置自动刷新', () => {
      const store = useSystemStore.getState()
      
      store.setAutoRefresh(true)
      
      const state = useSystemStore.getState()
      expect(state.autoRefresh).toBe(true)
    })

    it('应该设置刷新间隔', () => {
      const store = useSystemStore.getState()
      
      store.setRefreshInterval(60000)
      
      const state = useSystemStore.getState()
      expect(state.refreshInterval).toBe(60000)
    })
  })

  describe('错误处理', () => {
    it('应该清除错误信息', () => {
      const store = useSystemStore.getState()
      
      // 设置一些错误状态
      useSystemStore.setState({
        logListError: 'test error',
        operationError: { 'export-logs': 'export error' }
      })
      
      store.clearError()
      
      const state = useSystemStore.getState()
      expect(state.logListError).toBe(null)
      expect(state.operationError).toEqual({})
    })
  })

  describe('状态重置', () => {
    it('应该重置所有状态', () => {
      const store = useSystemStore.getState()
      
      // 修改一些状态
      useSystemStore.setState({
        logList: mockSystemLogList,
        logListTotal: 10,
        realtimeLogs: mockRealtimeLogs,
        logStatistics: mockLogStatistics,
        autoRefresh: true,
        refreshInterval: 60000
      })
      
      store.resetState()
      
      const state = useSystemStore.getState()
      expect(state.logList).toEqual([])
      expect(state.logListTotal).toBe(0)
      expect(state.realtimeLogs).toEqual([])
      expect(state.logStatistics).toBe(null)
      expect(state.autoRefresh).toBe(false)
      expect(state.refreshInterval).toBe(30000)
    })
  })
})
