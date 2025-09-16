/**
 * useSystem Hook 测试
 * 测试系统管理hook的错误处理封装和组件接口
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useSystem } from '@/store/system/useSystemStore'
import { useSystemStore } from '@/store/system/systemStore'
import {
  mockSystemLogList,
  mockLogStatistics,
  mockSystemMonitor,
  mockLogMonitor,
  mockAlertConfig,
  mockLogConfig
} from '@/mocks/store/systemStoreMock'

// Mock the store
vi.mock('@/store/system/systemStore')

const mockStore = {
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
  systemMonitorError: null,
  logMonitor: null,
  logMonitorLoading: false,
  logMonitorError: null,
  performanceMonitor: null,
  performanceMonitorLoading: false,
  performanceMonitorError: null,
  alertConfig: null,
  alertConfigLoading: false,
  alertConfigError: null,
  logConfig: null,
  logConfigLoading: false,
  logConfigError: null,
  exportTask: null,
  exportTaskLoading: false,
  exportTaskError: null,
  cleanupTask: null,
  cleanupTaskLoading: false,
  cleanupTaskError: null,
  operationLoading: {},
  operationError: {},
  pagination: { page: 1, size: 20, total: 0 },
  queryParams: {},
  autoRefresh: false,
  refreshInterval: 30000,
  fetchLogList: vi.fn(),
  refreshLogList: vi.fn(),
  fetchLogDetail: vi.fn(),
  fetchRealtimeLogs: vi.fn(),
  fetchLogStatistics: vi.fn(),
  exportLogs: vi.fn(),
  getExportStatus: vi.fn(),
  downloadExportFile: vi.fn(),
  cleanupLogs: vi.fn(),
  getCleanupStatus: vi.fn(),
  fetchSystemMonitor: vi.fn(),
  fetchLogMonitor: vi.fn(),
  fetchPerformanceMonitor: vi.fn(),
  fetchAlertConfig: vi.fn(),

  fetchLogConfig: vi.fn(),
  updateLogConfig: vi.fn(),
  setPagination: vi.fn(),
  setQueryParams: vi.fn(),
  resetQueryParams: vi.fn(),
  setAutoRefresh: vi.fn(),
  setRefreshInterval: vi.fn(),
  clearError: vi.fn(),
  resetState: vi.fn()
}

describe('useSystem', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock the store selector function
    const mockUseSystemStore = vi.mocked(useSystemStore)
    mockUseSystemStore.mockImplementation((selector: any) => {
      if (typeof selector === 'function') {
        return selector(mockStore)
      }
      return mockStore
    })
  })

  describe('状态访问', () => {
    it('应该正确暴露系统管理状态', () => {
      const { result } = renderHook(() => useSystem())
      
      expect(result.current.logList).toEqual([])
      expect(result.current.logListTotal).toBe(0)
      expect(result.current.logListLoading).toBe(false)
      expect(result.current.logListError).toBe(null)
      // currentLog 相关属性不在hook接口中
      expect(result.current.realtimeLogs).toEqual([])
      expect(result.current.realtimeLogsLoading).toBe(false)
      // realtimeLogsError 不在hook接口中
      expect(result.current.logStatistics).toBe(null)
      expect(result.current.logStatisticsLoading).toBe(false)
      // logStatisticsError 不在hook接口中
      expect(result.current.systemMonitor).toBe(null)
      expect(result.current.logMonitor).toBe(null)
      expect(result.current.performanceMonitor).toBe(null)
      expect(result.current.alertConfig).toBe(null)
      expect(result.current.logConfig).toBe(null)
      expect(result.current.pagination).toEqual({ page: 1, size: 20, total: 0 })
      expect(result.current.queryParams).toEqual({})
      expect(result.current.autoRefresh).toBe(false)
      expect(result.current.refreshInterval).toBe(30000)
    })
  })

  describe('日志管理操作', () => {
    it('应该成功获取日志列表', async () => {
      mockStore.fetchLogList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchLogList()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchLogList).toHaveBeenCalled()
    })

    it('应该处理获取日志列表失败', async () => {
      const error = new Error('获取日志列表失败')
      mockStore.fetchLogList.mockRejectedValue(error)
      
      const { result } = renderHook(() => useSystem())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchLogList()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取日志列表失败' })
    })

    it('应该成功刷新日志列表', async () => {
      mockStore.refreshLogList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let refreshResult
      await act(async () => {
        refreshResult = await result.current.refreshLogList()
      })
      
      expect(refreshResult).toEqual({ success: true, error: null })
      expect(mockStore.refreshLogList).toHaveBeenCalled()
    })

    it('应该支持带参数的日志列表获取', async () => {
      mockStore.fetchLogList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      const params = {
        level: 'ERROR' as const,
        source: 'API',
        startTime: '2024-01-01T00:00:00Z',
        endTime: '2024-01-02T00:00:00Z'
      }
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchLogList(params)
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchLogList).toHaveBeenCalledWith(params)
    })

    it('应该成功获取日志详情', async () => {
      mockStore.fetchLogDetail.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchLogDetail('log-001')
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchLogDetail).toHaveBeenCalledWith('log-001')
    })

    it('应该成功获取实时日志', async () => {
      mockStore.fetchRealtimeLogs.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      const params = {
        level: 'INFO' as const,
        source: 'WebSocket',
        vmId: 'vm-001'
      }
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchRealtimeLogs(params)
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchRealtimeLogs).toHaveBeenCalledWith(params)
    })
  })

  describe('统计和监控操作', () => {
    it('应该成功获取日志统计', async () => {
      mockStore.fetchLogStatistics.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchLogStatistics()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchLogStatistics).toHaveBeenCalled()
    })

    it('应该成功获取系统监控数据', async () => {
      mockStore.fetchSystemMonitor.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchSystemMonitor()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchSystemMonitor).toHaveBeenCalled()
    })

    it('应该成功获取日志监控数据', async () => {
      mockStore.fetchLogMonitor.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchLogMonitor()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchLogMonitor).toHaveBeenCalled()
    })

    it('应该成功获取性能监控数据', async () => {
      mockStore.fetchPerformanceMonitor.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchPerformanceMonitor()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchPerformanceMonitor).toHaveBeenCalled()
    })
  })

  describe('日志导出和清理操作', () => {
    it('应该成功导出日志', async () => {
      const mockTaskId = 'export-task-001'
      mockStore.exportLogs.mockResolvedValue(mockTaskId)
      
      const { result } = renderHook(() => useSystem())
      
      const exportRequest = {
        level: 'ERROR' as const,
        source: 'API',
        startTime: '2024-01-01T00:00:00Z',
        endTime: '2024-01-02T00:00:00Z',
        format: 'CSV' as const
      }
      
      let exportResult
      await act(async () => {
        exportResult = await result.current.exportLogs(exportRequest)
      })
      
      expect(exportResult).toEqual({ success: true, error: null, data: mockTaskId })
      expect(mockStore.exportLogs).toHaveBeenCalledWith(exportRequest)
    })

    it('应该成功获取导出状态', async () => {
      mockStore.getExportStatus.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let statusResult
      await act(async () => {
        statusResult = await result.current.getExportStatus('export-001')
      })
      
      expect(statusResult).toEqual({ success: true, error: null })
      expect(mockStore.getExportStatus).toHaveBeenCalledWith('export-001')
    })

    it('应该成功下载导出文件', async () => {
      mockStore.downloadExportFile.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let downloadResult
      await act(async () => {
        downloadResult = await result.current.downloadExportFile('export-001')
      })
      
      expect(downloadResult).toEqual({ success: true, error: null })
      expect(mockStore.downloadExportFile).toHaveBeenCalledWith('export-001')
    })

    it('应该成功清理日志', async () => {
      const mockTaskId = 'cleanup-task-001'
      mockStore.cleanupLogs.mockResolvedValue(mockTaskId)
      
      const { result } = renderHook(() => useSystem())
      
      const cleanupRequest = {
        strategy: 'TIME_BASED' as const,
        retentionDays: 30,
        vmIds: ['vm-001', 'vm-002']
      }
      
      let cleanupResult
      await act(async () => {
        cleanupResult = await result.current.cleanupLogs(cleanupRequest)
      })
      
      expect(cleanupResult).toEqual({ success: true, error: null, data: mockTaskId })
      expect(mockStore.cleanupLogs).toHaveBeenCalledWith(cleanupRequest)
    })

    it('应该成功获取清理状态', async () => {
      mockStore.getCleanupStatus.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let statusResult
      await act(async () => {
        statusResult = await result.current.getCleanupStatus('cleanup-001')
      })
      
      expect(statusResult).toEqual({ success: true, error: null })
      expect(mockStore.getCleanupStatus).toHaveBeenCalledWith('cleanup-001')
    })
  })

  describe('配置管理操作', () => {
    it('应该成功获取告警配置', async () => {
      mockStore.fetchAlertConfig.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchAlertConfig()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchAlertConfig).toHaveBeenCalled()
    })

    it('应该成功更新告警配置', async () => {
      mockStore.updateLogConfig.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      const configUpdate = {
        level: 'INFO' as const,
        retentionDays: 90,
        maxFileSize: '100MB',
        categories: {
          api: { level: 'DEBUG', enabled: true },
          websocket: { level: 'INFO', enabled: true }
        }
      }
      
      let updateResult
      await act(async () => {
        updateResult = await result.current.updateLogConfig(configUpdate)
      })
      
      expect(updateResult).toEqual({ success: true, error: null })
      expect(mockStore.updateLogConfig).toHaveBeenCalledWith(configUpdate)
    })

    it('应该成功获取日志配置', async () => {
      mockStore.fetchLogConfig.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchLogConfig()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchLogConfig).toHaveBeenCalled()
    })

    it('应该成功更新日志配置', async () => {
      mockStore.updateLogConfig.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      const configUpdate = {
        level: 'INFO' as const,
        retentionDays: 90,
        maxFileSize: '100MB',
        categories: {
          api: { level: 'DEBUG', enabled: true },
          websocket: { level: 'INFO', enabled: true }
        }
      }
      
      let updateResult
      await act(async () => {
        updateResult = await result.current.updateLogConfig(configUpdate)
      })
      
      expect(updateResult).toEqual({ success: true, error: null })
      expect(mockStore.updateLogConfig).toHaveBeenCalledWith(configUpdate)
    })
  })

  describe('状态管理操作', () => {
    it('应该成功设置分页参数', () => {
      const { result } = renderHook(() => useSystem())
      
      act(() => {
        result.current.setPagination(2, 50)
      })
      
      expect(mockStore.setPagination).toHaveBeenCalledWith(2, 50)
    })

    it('应该成功设置查询参数', () => {
      const { result } = renderHook(() => useSystem())
      
      const params = {
        level: 'ERROR' as const,
        source: 'API',
        startTime: '2024-01-01T00:00:00Z',
        endTime: '2024-01-02T00:00:00Z'
      }
      
      act(() => {
        result.current.setQueryParams(params)
      })
      
      expect(mockStore.setQueryParams).toHaveBeenCalledWith(params)
    })

    it('应该成功重置查询参数', () => {
      const { result } = renderHook(() => useSystem())
      
      act(() => {
        result.current.resetQueryParams()
      })
      
      expect(mockStore.resetQueryParams).toHaveBeenCalled()
    })

    it('应该成功设置自动刷新', () => {
      const { result } = renderHook(() => useSystem())
      
      act(() => {
        result.current.setAutoRefresh(true)
      })
      
      expect(mockStore.setAutoRefresh).toHaveBeenCalledWith(true)
    })

    it('应该成功设置刷新间隔', () => {
      const { result } = renderHook(() => useSystem())
      
      act(() => {
        result.current.setRefreshInterval(60000)
      })
      
      expect(mockStore.setRefreshInterval).toHaveBeenCalledWith(60000)
    })

    it('应该成功清除错误', () => {
      const { result } = renderHook(() => useSystem())
      
      act(() => {
        result.current.clearError()
      })
      
      expect(mockStore.clearError).toHaveBeenCalled()
    })

    it('应该成功重置状态', () => {
      const { result } = renderHook(() => useSystem())
      
      act(() => {
        result.current.resetState()
      })
      
      expect(mockStore.resetState).toHaveBeenCalled()
    })
  })

  describe('错误处理', () => {
    it('应该处理非Error类型的异常', async () => {
      mockStore.fetchLogList.mockRejectedValue('string error')
      
      const { result } = renderHook(() => useSystem())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchLogList()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取日志列表失败' })
    })

    it('应该处理undefined异常', async () => {
      mockStore.exportLogs.mockRejectedValue(undefined)
      
      const { result } = renderHook(() => useSystem())
      
      let exportResult
      await act(async () => {
        exportResult = await result.current.exportLogs({
          level: 'ERROR' as const,
          format: 'JSON' as const,
          startTime: '2024-01-01T00:00:00Z',
          endTime: '2024-01-02T00:00:00Z',
          // source 不在 LogExportData 中
        })
      })
      
      expect(exportResult).toEqual({ success: false, error: '导出日志失败', data: null })
    })
  })
})
