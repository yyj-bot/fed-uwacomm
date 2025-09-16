/**
 * 仪表盘 Hook - 封装仪表盘状态和操作
 * 为组件层提供简洁的仪表盘功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback, useEffect } from 'react'
import { useDashboardStore } from './dashboardStore'
import type { DashboardOverview, DashboardChartData } from './dashboardStore'

// ==================== Hook 实现 ====================

export const useDashboard = () => {
  // 获取状态
  const overview = useDashboardStore((state) => state.overview)
  const overviewLoading = useDashboardStore((state) => state.overviewLoading)
  const overviewError = useDashboardStore((state) => state.overviewError)
  
  const chartData = useDashboardStore((state) => state.chartData)
  const chartDataLoading = useDashboardStore((state) => state.chartDataLoading)
  const chartDataError = useDashboardStore((state) => state.chartDataError)
  
  const recentActivities = useDashboardStore((state) => state.recentActivities)
  const recentActivitiesLoading = useDashboardStore((state) => state.recentActivitiesLoading)
  
  const systemAlerts = useDashboardStore((state) => state.systemAlerts)
  const systemAlertsLoading = useDashboardStore((state) => state.systemAlertsLoading)
  
  const quickActionLoading = useDashboardStore((state) => state.quickActionLoading)
  const quickActionError = useDashboardStore((state) => state.quickActionError)
  
  const lastRefreshTime = useDashboardStore((state) => state.lastRefreshTime)
  const autoRefresh = useDashboardStore((state) => state.autoRefresh)
  const refreshInterval = useDashboardStore((state) => state.refreshInterval)
  const timeRange = useDashboardStore((state) => state.timeRange)

  // 获取操作方法
  const fetchOverviewAction = useDashboardStore((state) => state.fetchOverview)
  const fetchChartDataAction = useDashboardStore((state) => state.fetchChartData)
  const fetchRecentActivitiesAction = useDashboardStore((state) => state.fetchRecentActivities)
  const fetchSystemAlertsAction = useDashboardStore((state) => state.fetchSystemAlerts)
  const refreshAllDataAction = useDashboardStore((state) => state.refreshAllData)
  const quickStartTaskAction = useDashboardStore((state) => state.quickStartTask)
  const quickCreateVMAction = useDashboardStore((state) => state.quickCreateVM)
  const quickUploadDataAction = useDashboardStore((state) => state.quickUploadData)
  const setTimeRangeAction = useDashboardStore((state) => state.setTimeRange)
  const setAutoRefreshAction = useDashboardStore((state) => state.setAutoRefresh)
  const setRefreshIntervalAction = useDashboardStore((state) => state.setRefreshInterval)
  const clearErrorAction = useDashboardStore((state) => state.clearError)
  const resetStateAction = useDashboardStore((state) => state.resetState)

  // ==================== 封装操作方法 ====================

  /**
   * 获取概览数据
   */
  const fetchOverview = useCallback(async () => {
    try {
      await fetchOverviewAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取概览数据失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchOverviewAction])

  /**
   * 获取图表数据
   */
  const fetchChartData = useCallback(async (timeRange?: string) => {
    try {
      await fetchChartDataAction(timeRange)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取图表数据失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchChartDataAction])

  /**
   * 获取最近活动
   */
  const fetchRecentActivities = useCallback(async () => {
    try {
      await fetchRecentActivitiesAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取最近活动失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchRecentActivitiesAction])

  /**
   * 获取系统告警
   */
  const fetchSystemAlerts = useCallback(async () => {
    try {
      await fetchSystemAlertsAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取系统告警失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchSystemAlertsAction])

  /**
   * 刷新所有数据
   */
  const refreshAllData = useCallback(async () => {
    try {
      await refreshAllDataAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新数据失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshAllDataAction])

  /**
   * 快速启动任务
   */
  const quickStartTask = useCallback(async (taskConfig: any) => {
    try {
      const taskId = await quickStartTaskAction(taskConfig)
      return { success: true, error: null, data: taskId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '快速启动任务失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [quickStartTaskAction])

  /**
   * 快速创建虚拟机
   */
  const quickCreateVM = useCallback(async (vmConfig: any) => {
    try {
      const vmId = await quickCreateVMAction(vmConfig)
      return { success: true, error: null, data: vmId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '快速创建虚拟机失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [quickCreateVMAction])

  /**
   * 快速上传数据
   */
  const quickUploadData = useCallback(async (file: File) => {
    try {
      const dataId = await quickUploadDataAction(file)
      return { success: true, error: null, data: dataId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '快速上传数据失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [quickUploadDataAction])

  /**
   * 设置时间范围
   */
  const setTimeRange = useCallback((range: '1d' | '7d' | '30d' | '90d') => {
    setTimeRangeAction(range)
  }, [setTimeRangeAction])

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
   * 获取系统健康状态
   */
  const getSystemHealth = useCallback((): 'healthy' | 'warning' | 'critical' => {
    if (!overview) return 'warning'
    
    const criticalAlerts = systemAlerts.filter(alert => alert.level === 'error').length
    const warningAlerts = systemAlerts.filter(alert => alert.level === 'warning').length
    
    if (criticalAlerts > 0 || overview.vmStats.errorVMs > 2) {
      return 'critical'
    }
    
    if (warningAlerts > 0 || overview.systemStats.systemLoad > 0.8) {
      return 'warning'
    }
    
    return 'healthy'
  }, [overview, systemAlerts])

  /**
   * 获取活跃告警数量
   */
  const getActiveAlertsCount = useCallback((): number => {
    return systemAlerts.filter(alert => !alert.resolved).length
  }, [systemAlerts])

  /**
   * 获取今日关键指标
   */
  const getTodayMetrics = useCallback(() => {
    if (!overview) return null
    
    return {
      newUsers: overview.userStats.newUsersToday,
      newTasks: overview.taskStats.tasksToday,
      newModels: overview.modelStats.modelsToday,
      uploadedData: overview.dataStats.uploadedToday,
      errorLogs: overview.systemStats.errorLogsToday,
      warningLogs: overview.systemStats.warningLogsToday
    }
  }, [overview])

  /**
   * 获取资源使用率
   */
  const getResourceUsage = useCallback(() => {
    if (!overview) return null
    
    return {
      cpu: overview.vmStats.cpuUsage,
      memory: overview.vmStats.memoryUsage,
      disk: overview.vmStats.diskUsage,
      network: overview.systemStats.networkTraffic
    }
  }, [overview])

  /**
   * 获取任务成功率
   */
  const getTaskSuccessRate = useCallback((): number => {
    if (!overview) return 0
    
    const { completedTasks, failedTasks } = overview.taskStats
    const totalFinishedTasks = completedTasks + failedTasks
    
    if (totalFinishedTasks === 0) return 0
    
    return Math.round((completedTasks / totalFinishedTasks) * 100)
  }, [overview])

  /**
   * 获取虚拟机运行率
   */
  const getVMRunningRate = useCallback((): number => {
    if (!overview) return 0
    
    const { totalVMs, runningVMs } = overview.vmStats
    
    if (totalVMs === 0) return 0
    
    return Math.round((runningVMs / totalVMs) * 100)
  }, [overview])

  /**
   * 检查是否有任何数据正在加载
   */
  const isAnyDataLoading = overviewLoading || chartDataLoading || recentActivitiesLoading || systemAlertsLoading

  /**
   * 检查是否有任何快速操作正在进行
   */
  const isAnyQuickActionLoading = Object.values(quickActionLoading).some(loading => loading)

  /**
   * 获取最近活动数量（按类型）
   */
  const getRecentActivityCountByType = useCallback((type: string): number => {
    return recentActivities.filter(activity => activity.type === type).length
  }, [recentActivities])

  /**
   * 获取未解决的告警
   */
  const getUnresolvedAlerts = useCallback(() => {
    return systemAlerts.filter(alert => !alert.resolved)
  }, [systemAlerts])

  /**
   * 获取不同级别的告警数量
   */
  const getAlertCountByLevel = useCallback((level: string): number => {
    return systemAlerts.filter(alert => alert.level === level && !alert.resolved).length
  }, [systemAlerts])

  // ==================== 自动刷新逻辑 ====================

  useEffect(() => {
    let intervalId: NodeJS.Timeout | null = null
    
    if (autoRefresh && refreshInterval > 0) {
      intervalId = setInterval(() => {
        refreshAllData().catch(console.error)
      }, refreshInterval)
    }
    
    return () => {
      if (intervalId) {
        clearInterval(intervalId)
      }
    }
  }, [autoRefresh, refreshInterval, refreshAllData])

  // ==================== 初始化数据加载 ====================

  useEffect(() => {
    // 组件挂载时加载初始数据
    refreshAllData().catch(console.error)
  }, [refreshAllData])

  // ==================== 返回接口 ====================

  return {
    // 状态
    overview,
    overviewLoading,
    overviewError,
    chartData,
    chartDataLoading,
    chartDataError,
    recentActivities,
    recentActivitiesLoading,
    systemAlerts,
    systemAlertsLoading,
    quickActionLoading,
    quickActionError,
    lastRefreshTime,
    autoRefresh,
    refreshInterval,
    timeRange,
    
    // 操作方法
    fetchOverview,
    fetchChartData,
    fetchRecentActivities,
    fetchSystemAlerts,
    refreshAllData,
    quickStartTask,
    quickCreateVM,
    quickUploadData,
    setTimeRange,
    setAutoRefresh,
    setRefreshInterval,
    clearError,
    resetState,
    
    // 计算属性和工具方法
    getSystemHealth,
    getActiveAlertsCount,
    getTodayMetrics,
    getResourceUsage,
    getTaskSuccessRate,
    getVMRunningRate,
    isAnyDataLoading,
    isAnyQuickActionLoading,
    getRecentActivityCountByType,
    getUnresolvedAlerts,
    getAlertCountByLevel
  }
}

// ==================== 导出默认 Hook ====================
export default useDashboard
