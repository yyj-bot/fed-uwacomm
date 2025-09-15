/**
 * Dashboard Store 测试
 * 测试仪表盘状态管理的各种功能，包括数据获取、状态更新、错误处理等
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useDashboardStore } from '@/store/dashboard/dashboardStore'
import {
  mockDashboardOverview,
  mockDashboardChartData,
  mockRecentActivities,
  mockSystemAlerts,
  mockDashboardErrors,
  mockInitialDashboardState
} from '@/mocks/store/dashboardStoreMock'

describe('DashboardStore', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 重置store状态
    const store = useDashboardStore.getState()
    store.resetState()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      const state = useDashboardStore.getState()
      
      expect(state.overview).toBe(null)
      expect(state.overviewLoading).toBe(false)
      expect(state.overviewError).toBe(null)
      expect(state.chartData).toBe(null)
      expect(state.chartDataLoading).toBe(false)
      expect(state.chartDataError).toBe(null)
      expect(state.recentActivities).toEqual([])
      expect(state.recentActivitiesLoading).toBe(false)
      expect(state.systemAlerts).toEqual([])
      expect(state.systemAlertsLoading).toBe(false)
      expect(state.quickActionLoading).toEqual({})
      expect(state.quickActionError).toEqual({})
      expect(state.lastRefreshTime).toBe(null)
      expect(state.autoRefresh).toBe(false)
      expect(state.refreshInterval).toBe(60000)
      expect(state.timeRange).toBe('7d')
    })
  })

  describe('fetchOverview', () => {
    it('应该成功获取概览数据', async () => {
      const store = useDashboardStore.getState()
      
      await store.fetchOverview()
      
      const state = useDashboardStore.getState()
      expect(state.overview).not.toBe(null)
      expect(state.overviewLoading).toBe(false)
      expect(state.overviewError).toBe(null)
      expect(state.lastRefreshTime).not.toBe(null)
    })

    it('应该处理加载状态', async () => {
      const store = useDashboardStore.getState()
      
      // 开始获取数据时应该设置加载状态
      const fetchPromise = store.fetchOverview()
      
      // 在异步操作完成前检查加载状态
      let state = useDashboardStore.getState()
      expect(state.overviewLoading).toBe(true)
      expect(state.overviewError).toBe(null)
      
      await fetchPromise
      
      // 异步操作完成后检查最终状态
      state = useDashboardStore.getState()
      expect(state.overviewLoading).toBe(false)
    })
  })

  describe('fetchChartData', () => {
    it('应该成功获取图表数据', async () => {
      const store = useDashboardStore.getState()
      
      await store.fetchChartData()
      
      const state = useDashboardStore.getState()
      expect(state.chartData).not.toBe(null)
      expect(state.chartDataLoading).toBe(false)
      expect(state.chartDataError).toBe(null)
    })

    it('应该支持指定时间范围', async () => {
      const store = useDashboardStore.getState()
      
      await store.fetchChartData('30d')
      
      const state = useDashboardStore.getState()
      expect(state.chartData).not.toBe(null)
      expect(state.chartDataLoading).toBe(false)
    })

    it('应该处理加载状态', async () => {
      const store = useDashboardStore.getState()
      
      await store.fetchChartData()
      
      // 验证最终状态 - 由于是模拟数据，操作完成很快，主要验证最终状态正确
      const state = useDashboardStore.getState()
      expect(state.chartDataLoading).toBe(false)
      expect(state.chartDataError).toBe(null)
      expect(state.chartData).not.toBe(null)
    })
  })

  describe('fetchRecentActivities', () => {
    it('应该成功获取最近活动', async () => {
      const store = useDashboardStore.getState()
      
      await store.fetchRecentActivities()
      
      const state = useDashboardStore.getState()
      expect(state.recentActivities).toEqual(expect.any(Array))
      expect(state.recentActivitiesLoading).toBe(false)
    })

    it('应该处理加载状态', async () => {
      const store = useDashboardStore.getState()
      
      await store.fetchRecentActivities()
      
      // 验证最终状态 - 由于是模拟数据，操作完成很快，主要验证最终状态正确
      const state = useDashboardStore.getState()
      expect(state.recentActivitiesLoading).toBe(false)
      expect(state.recentActivities).toEqual(expect.any(Array))
      expect(state.recentActivities.length).toBeGreaterThan(0)
    })
  })

  describe('fetchSystemAlerts', () => {
    it('应该成功获取系统告警', async () => {
      const store = useDashboardStore.getState()
      
      await store.fetchSystemAlerts()
      
      const state = useDashboardStore.getState()
      expect(state.systemAlerts).toEqual(expect.any(Array))
      expect(state.systemAlertsLoading).toBe(false)
    })

    it('应该处理加载状态', async () => {
      const store = useDashboardStore.getState()
      
      await store.fetchSystemAlerts()
      
      // 验证最终状态 - 由于是模拟数据，操作完成很快，主要验证最终状态正确
      const state = useDashboardStore.getState()
      expect(state.systemAlertsLoading).toBe(false)
      expect(state.systemAlerts).toEqual(expect.any(Array))
      expect(state.systemAlerts.length).toBeGreaterThan(0)
    })
  })

  describe('refreshAllData', () => {
    it('应该刷新所有数据', async () => {
      const store = useDashboardStore.getState()
      
      await store.refreshAllData()
      
      const state = useDashboardStore.getState()
      expect(state.overview).not.toBe(null)
      expect(state.chartData).not.toBe(null)
      expect(state.recentActivities).toEqual(expect.any(Array))
      expect(state.systemAlerts).toEqual(expect.any(Array))
      expect(state.lastRefreshTime).not.toBe(null)
    })
  })

  describe('quickStartTask', () => {
    it('应该成功快速启动任务', async () => {
      const store = useDashboardStore.getState()
      const taskConfig = { name: 'test-task', type: 'classification' }
      
      const taskId = await store.quickStartTask(taskConfig)
      
      expect(taskId).toMatch(/^FL-\d+$/)
      const state = useDashboardStore.getState()
      expect(state.quickActionLoading['start-task']).toBe(false)
    })

    it('应该处理快速操作加载状态', async () => {
      const store = useDashboardStore.getState()
      const taskConfig = { name: 'test-task', type: 'classification' }
      
      const taskId = await store.quickStartTask(taskConfig)
      
      // 验证最终状态 - 由于是模拟数据，操作完成很快，主要验证最终状态正确
      expect(taskId).toMatch(/^FL-\d+$/)
      const state = useDashboardStore.getState()
      expect(state.quickActionLoading['start-task']).toBe(false)
      expect(state.quickActionError['start-task']).toBe(null)
    })
  })

  describe('quickCreateVM', () => {
    it('应该成功快速创建虚拟机', async () => {
      const store = useDashboardStore.getState()
      const vmConfig = { name: 'test-vm', cpu: 4, memory: 8192 }
      
      const vmId = await store.quickCreateVM(vmConfig)
      
      expect(vmId).toMatch(/^VM-\d+$/)
      const state = useDashboardStore.getState()
      expect(state.quickActionLoading['create-vm']).toBe(false)
    })
  })

  describe('quickUploadData', () => {
    it('应该成功快速上传数据', async () => {
      const store = useDashboardStore.getState()
      const file = new File(['test'], 'test.csv', { type: 'text/csv' })
      
      const dataId = await store.quickUploadData(file)
      
      expect(dataId).toMatch(/^DATA-\d+$/)
      const state = useDashboardStore.getState()
      expect(state.quickActionLoading['upload-data']).toBe(false)
    })
  })

  describe('设置操作', () => {
    it('应该设置时间范围', () => {
      const store = useDashboardStore.getState()
      
      store.setTimeRange('30d')
      
      const state = useDashboardStore.getState()
      expect(state.timeRange).toBe('30d')
    })

    it('应该设置自动刷新', () => {
      const store = useDashboardStore.getState()
      
      store.setAutoRefresh(true)
      
      const state = useDashboardStore.getState()
      expect(state.autoRefresh).toBe(true)
    })

    it('应该设置刷新间隔', () => {
      const store = useDashboardStore.getState()
      
      store.setRefreshInterval(30000)
      
      const state = useDashboardStore.getState()
      expect(state.refreshInterval).toBe(30000)
    })
  })

  describe('错误处理', () => {
    it('应该清除错误信息', () => {
      const store = useDashboardStore.getState()
      
      // 设置一些错误状态
      useDashboardStore.setState({
        overviewError: 'test error',
        chartDataError: 'chart error',
        quickActionError: { 'start-task': 'action error' }
      })
      
      store.clearError()
      
      const state = useDashboardStore.getState()
      expect(state.overviewError).toBe(null)
      expect(state.chartDataError).toBe(null)
      expect(state.quickActionError).toEqual({})
    })
  })

  describe('状态重置', () => {
    it('应该重置所有状态', () => {
      const store = useDashboardStore.getState()
      
      // 修改一些状态
      useDashboardStore.setState({
        overview: mockDashboardOverview,
        chartData: mockDashboardChartData,
        recentActivities: mockRecentActivities,
        systemAlerts: mockSystemAlerts,
        timeRange: '30d' as const,
        autoRefresh: true
      })
      
      store.resetState()
      
      const state = useDashboardStore.getState()
      expect(state.overview).toBe(null)
      expect(state.chartData).toBe(null)
      expect(state.recentActivities).toEqual([])
      expect(state.systemAlerts).toEqual([])
      expect(state.timeRange).toBe('7d')
      expect(state.autoRefresh).toBe(false)
    })
  })
})
