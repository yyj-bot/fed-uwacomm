/**
 * useDashboard Hook 测试
 * 测试仪表盘hook的错误处理封装和组件接口
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useDashboard } from '@/store/dashboard/useDashboardStore'
import { useDashboardStore } from '@/store/dashboard/dashboardStore'
import {
  mockDashboardOverview,
  mockDashboardChartData,
  mockRecentActivities,
  mockSystemAlerts
} from '@/mocks/store/dashboardStoreMock'

// Mock the store
vi.mock('@/store/dashboard/dashboardStore')

const mockStore = {
  overview: null,
  overviewLoading: false,
  overviewError: null,
  chartData: null,
  chartDataLoading: false,
  chartDataError: null,
  recentActivities: [],
  recentActivitiesLoading: false,
  systemAlerts: [],
  systemAlertsLoading: false,
  quickActionLoading: {},
  quickActionError: {},
  lastRefreshTime: null,
  autoRefresh: false,
  refreshInterval: 30000,
  timeRange: '7d',
  fetchOverview: vi.fn(),
  fetchChartData: vi.fn(),
  fetchRecentActivities: vi.fn(),
  fetchSystemAlerts: vi.fn(),
  refreshAllData: vi.fn(),
  quickStartTask: vi.fn(),
  quickCreateVM: vi.fn(),
  quickUploadData: vi.fn(),
  setTimeRange: vi.fn(),
  setAutoRefresh: vi.fn(),
  setRefreshInterval: vi.fn(),
  clearError: vi.fn(),
  resetState: vi.fn()
}

describe('useDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock the store selector function
    const mockUseDashboardStore = vi.mocked(useDashboardStore)
    mockUseDashboardStore.mockImplementation((selector: any) => {
      if (typeof selector === 'function') {
        return selector(mockStore)
      }
      return mockStore
    })
  })

  describe('状态访问', () => {
    it('应该正确暴露仪表盘状态', () => {
      const { result } = renderHook(() => useDashboard())
      
      expect(result.current.overview).toBe(null)
      expect(result.current.overviewLoading).toBe(false)
      expect(result.current.overviewError).toBe(null)
      expect(result.current.chartData).toBe(null)
      expect(result.current.chartDataLoading).toBe(false)
      expect(result.current.chartDataError).toBe(null)
      expect(result.current.recentActivities).toEqual([])
      expect(result.current.recentActivitiesLoading).toBe(false)
      expect(result.current.systemAlerts).toEqual([])
      expect(result.current.systemAlertsLoading).toBe(false)
      // recentActivitiesError 和 systemAlertsError 不在hook接口中
      expect(result.current.timeRange).toBe('7d')
      expect(result.current.autoRefresh).toBe(false)
      expect(result.current.refreshInterval).toBe(30000)
      // lastUpdated 不在hook接口中
    })
  })

  describe('数据获取操作', () => {
    it('应该成功获取概览数据', async () => {
      mockStore.fetchOverview.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useDashboard())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchOverview()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchOverview).toHaveBeenCalled()
    })

    it('应该处理获取概览数据失败', async () => {
      const error = new Error('获取概览数据失败')
      mockStore.fetchOverview.mockRejectedValue(error)
      
      const { result } = renderHook(() => useDashboard())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchOverview()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取概览数据失败' })
    })

    it('应该成功获取图表数据', async () => {
      mockStore.fetchChartData.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useDashboard())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchChartData()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchChartData).toHaveBeenCalled()
    })

    it('应该支持带参数的图表数据获取', async () => {
      mockStore.fetchChartData.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useDashboard())
      
      const params = '30d'
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchChartData(params)
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchChartData).toHaveBeenCalledWith(params)
    })

    it('应该成功获取最近活动', async () => {
      mockStore.fetchRecentActivities.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useDashboard())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchRecentActivities()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchRecentActivities).toHaveBeenCalled()
    })

    it('应该成功获取系统告警', async () => {
      mockStore.fetchSystemAlerts.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useDashboard())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchSystemAlerts()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchSystemAlerts).toHaveBeenCalled()
    })

    it('应该成功刷新所有数据', async () => {
      mockStore.refreshAllData.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useDashboard())
      
      let refreshResult
      await act(async () => {
        refreshResult = await result.current.refreshAllData()
      })
      
      expect(refreshResult).toEqual({ success: true, error: null })
      expect(mockStore.refreshAllData).toHaveBeenCalled()
    })
  })

  describe('快速操作', () => {
    it('应该成功快速启动任务', async () => {
      const mockTaskId = 'task-quick-001'
      mockStore.quickStartTask.mockResolvedValue(mockTaskId)
      
      const { result } = renderHook(() => useDashboard())
      
      const taskConfig = {
        taskName: '快速任务',
        algorithm: 'FedAvg' as const,
        rounds: 10,
        participantCount: 3
      }
      
      let startResult
      await act(async () => {
        startResult = await result.current.quickStartTask(taskConfig)
      })
      
      expect(startResult).toEqual({ success: true, error: null, data: mockTaskId })
      expect(mockStore.quickStartTask).toHaveBeenCalledWith(taskConfig)
    })

    it('应该处理快速启动任务失败', async () => {
      const error = new Error('启动任务失败')
      mockStore.quickStartTask.mockRejectedValue(error)
      
      const { result } = renderHook(() => useDashboard())
      
      const taskConfig = {
        taskName: '快速任务',
        algorithm: 'FedAvg' as const,
        rounds: 10,
        participantCount: 3
      }
      
      let startResult
      await act(async () => {
        startResult = await result.current.quickStartTask(taskConfig)
      })
      
      expect(startResult).toEqual({ success: false, error: '启动任务失败', data: null })
    })

    it('应该成功快速创建虚拟机', async () => {
      const mockVMId = 'vm-quick-001'
      mockStore.quickCreateVM.mockResolvedValue(mockVMId)
      
      const { result } = renderHook(() => useDashboard())
      
      const vmConfig = {
        name: '快速虚拟机',
        osType: 'Ubuntu 20.04' as const,
        cpu: 2,
        memory: 4096,
        disk: 20
      }
      
      let createResult
      await act(async () => {
        createResult = await result.current.quickCreateVM(vmConfig)
      })
      
      expect(createResult).toEqual({ success: true, error: null, data: mockVMId })
      expect(mockStore.quickCreateVM).toHaveBeenCalledWith(vmConfig)
    })

    it('应该成功快速上传数据', async () => {
      const mockDatasetId = 'dataset-quick-001'
      mockStore.quickUploadData.mockResolvedValue(mockDatasetId)
      
      const { result } = renderHook(() => useDashboard())
      
      const uploadFile = new File(['test data'], 'test.csv', { type: 'text/csv' })
      
      let uploadResult
      await act(async () => {
        uploadResult = await result.current.quickUploadData(uploadFile)
      })
      
      expect(uploadResult).toEqual({ success: true, error: null, data: mockDatasetId })
      expect(mockStore.quickUploadData).toHaveBeenCalledWith(uploadFile)
    })
  })

  describe('配置管理', () => {
    it('应该设置时间范围', () => {
      const { result } = renderHook(() => useDashboard())
      
      act(() => {
        result.current.setTimeRange('30d')
      })
      
      expect(mockStore.setTimeRange).toHaveBeenCalledWith('30d')
    })

    it('应该设置自动刷新', () => {
      const { result } = renderHook(() => useDashboard())
      
      act(() => {
        result.current.setAutoRefresh(true)
      })
      
      expect(mockStore.setAutoRefresh).toHaveBeenCalledWith(true)
    })

    it('应该设置刷新间隔', () => {
      const { result } = renderHook(() => useDashboard())
      
      act(() => {
        result.current.setRefreshInterval(60000)
      })
      
      expect(mockStore.setRefreshInterval).toHaveBeenCalledWith(60000)
    })
  })

  describe('状态管理', () => {
    it('应该成功清除错误', () => {
      const { result } = renderHook(() => useDashboard())
      
      act(() => {
        result.current.clearError()
      })
      
      expect(mockStore.clearError).toHaveBeenCalled()
    })

    it('应该成功重置状态', () => {
      const { result } = renderHook(() => useDashboard())
      
      act(() => {
        result.current.resetState()
      })
      
      expect(mockStore.resetState).toHaveBeenCalled()
    })
  })

  describe('错误处理', () => {
    it('应该处理非Error类型的异常', async () => {
      mockStore.fetchOverview.mockRejectedValue('string error')
      
      const { result } = renderHook(() => useDashboard())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchOverview()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取概览数据失败' })
    })

    it('应该处理undefined异常', async () => {
      mockStore.quickStartTask.mockRejectedValue(undefined)
      
      const { result } = renderHook(() => useDashboard())
      
      let startResult
      await act(async () => {
        startResult = await result.current.quickStartTask({
          taskName: '测试任务',
          algorithm: 'FedAvg' as const,
          rounds: 5,
          participantCount: 2
        })
      })
      
      expect(startResult).toEqual({ success: false, error: '快速启动任务失败', data: null })
    })
  })
})
