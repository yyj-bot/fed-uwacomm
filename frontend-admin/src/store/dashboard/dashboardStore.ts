/**
 * 仪表盘状态管理 Store
 * 管理仪表盘数据、统计信息、图表数据等相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { 
  userService, 
  vmService, 
  federatedTaskService, 
  trainingDataService, 
  modelVersionService,
  systemLogService 
} from '@/services'

// ==================== 状态类型定义 ====================

interface DashboardOverview {
  // 用户统计
  userStats: {
    totalUsers: number
    activeUsers: number
    newUsersToday: number
    onlineUsers: number
  }
  
  // 虚拟机统计
  vmStats: {
    totalVMs: number
    runningVMs: number
    stoppedVMs: number
    errorVMs: number
    cpuUsage: number
    memoryUsage: number
    diskUsage: number
  }
  
  // 联邦任务统计
  taskStats: {
    totalTasks: number
    runningTasks: number
    completedTasks: number
    failedTasks: number
    tasksToday: number
    averageTrainingTime: number
  }
  
  // 训练数据统计
  dataStats: {
    totalDatasets: number
    totalDataSize: number
    processedData: number
    pendingData: number
    uploadedToday: number
  }
  
  // 模型统计
  modelStats: {
    totalModels: number
    deployedModels: number
    trainingModels: number
    averageAccuracy: number
    modelsToday: number
  }
  
  // 参与者统计
  participantStats: {
    totalParticipants: number
    onlineParticipants: number
    activeParticipants: number
    newParticipantsToday: number
  }
  
  // 系统统计
  systemStats: {
    systemUptime: number
    totalLogs: number
    errorLogsToday: number
    warningLogsToday: number
    systemLoad: number
    networkTraffic: number
  }
}

interface DashboardChartData {
  // 用户活动趋势
  userActivityTrend: {
    dates: string[]
    activeUsers: number[]
    newUsers: number[]
    loginCount: number[]
  }
  
  // 任务执行趋势
  taskExecutionTrend: {
    dates: string[]
    createdTasks: number[]
    completedTasks: number[]
    failedTasks: number[]
    averageTrainingTime: number[]
  }
  
  // 系统性能趋势
  systemPerformanceTrend: {
    timestamps: string[]
    cpuUsage: number[]
    memoryUsage: number[]
    diskUsage: number[]
    networkIn: number[]
    networkOut: number[]
  }
  
  // 数据分布
  dataDistribution: {
    labels: string[]
    datasets: number[]
    categories: string[]
    sizes: number[]
  }
  
  // 模型性能分布
  modelPerformanceDistribution: {
    accuracyRanges: string[]
    modelCounts: number[]
    averageAccuracy: number[]
  }
  
  // 日志级别分布
  logLevelDistribution: {
    levels: string[]
    counts: number[]
    percentages: number[]
  }
}

interface DashboardState {
  // 概览数据
  overview: DashboardOverview | null
  overviewLoading: boolean
  overviewError: string | null
  
  // 图表数据
  chartData: DashboardChartData | null
  chartDataLoading: boolean
  chartDataError: string | null
  
  // 最近活动
  recentActivities: any[]
  recentActivitiesLoading: boolean
  
  // 系统告警
  systemAlerts: any[]
  systemAlertsLoading: boolean
  
  // 快速操作状态
  quickActionLoading: Record<string, boolean>
  quickActionError: Record<string, string | null>
  
  // 数据刷新
  lastRefreshTime: string | null
  autoRefresh: boolean
  refreshInterval: number
  
  // 时间范围
  timeRange: '1d' | '7d' | '30d' | '90d'
}

interface DashboardActions {
  // 数据获取
  fetchOverview: () => Promise<void>
  fetchChartData: (timeRange?: string) => Promise<void>
  fetchRecentActivities: () => Promise<void>
  fetchSystemAlerts: () => Promise<void>
  
  // 数据刷新
  refreshAllData: () => Promise<void>
  
  // 快速操作
  quickStartTask: (taskConfig: any) => Promise<string>
  quickCreateVM: (vmConfig: any) => Promise<string>
  quickUploadData: (file: File) => Promise<string>
  
  // 设置
  setTimeRange: (range: '1d' | '7d' | '30d' | '90d') => void
  setAutoRefresh: (enabled: boolean) => void
  setRefreshInterval: (interval: number) => void
  
  // 错误处理
  clearError: () => void
  
  // 状态重置
  resetState: () => void
}

type DashboardStore = DashboardState & DashboardActions

// ==================== 初始状态 ====================

const initialState: DashboardState = {
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
  refreshInterval: 60000, // 1分钟
  
  timeRange: '7d'
}

// ==================== Store 实现 ====================

export const useDashboardStore = create<DashboardStore>((set, get) => ({
  ...initialState,

  // ==================== 数据获取 ====================
  
  /**
   * 获取概览数据
   */
  fetchOverview: async () => {
    set({ overviewLoading: true, overviewError: null })
    
    try {
      // 调用现有的API获取数据
      const { vmApi } = await import('../../api/vm')
      const federatedTaskModule = await import('../../api/federated-task')
      const taskApi = federatedTaskModule.federatedTask
      
      // 并行获取各模块的统计数据
      const [
        vmListResult,
        taskListResult
      ] = await Promise.allSettled([
        vmApi.getVMList({ page: 1, size: 100 }),
        taskApi.getTaskList({ page: 1, size: 100 })
      ])
      
      // 从API结果计算统计数据
      const vmList = vmListResult.status === 'fulfilled' ? vmListResult.value.list : []
      const taskList = taskListResult.status === 'fulfilled' ? taskListResult.value.tasks : []
      
      // 计算VM统计
      const totalVMs = vmList.length
      const runningVMs = vmList.filter((vm: any) => vm.status === 'RUNNING').length
      const stoppedVMs = vmList.filter((vm: any) => vm.status === 'STOPPED').length
      const errorVMs = vmList.filter((vm: any) => vm.status === 'ERROR').length
      
      // 计算任务统计
      const totalTasks = taskList.length
      const runningTasks = taskList.filter((task: any) => task.status === 'RUNNING').length
      const completedTasks = taskList.filter((task: any) => task.status === 'COMPLETED').length
      const failedTasks = taskList.filter((task: any) => task.status === 'FAILED').length
      
      // 构建概览数据（仅使用真实API数据，其他设为0）
      const overview: DashboardOverview = {
        userStats: {
          totalUsers: 0, // 需要用户API支持
          activeUsers: 0,
          newUsersToday: 0,
          onlineUsers: 0
        },
        vmStats: {
          totalVMs,
          runningVMs,
          stoppedVMs,
          errorVMs,
          cpuUsage: 0, // 需要系统监控API支持
          memoryUsage: 0,
          diskUsage: 0
        },
        taskStats: {
          totalTasks,
          runningTasks,
          completedTasks,
          failedTasks,
          tasksToday: 0, // 需要根据创建时间计算
          averageTrainingTime: 0
        },
        dataStats: {
          totalDatasets: 0, // 需要训练数据API支持
          totalDataSize: 0,
          processedData: 0,
          pendingData: 0,
          uploadedToday: 0
        },
        modelStats: {
          totalModels: 0, // 需要模型版本API支持
          deployedModels: 0,
          trainingModels: 0,
          averageAccuracy: 0,
          modelsToday: 0
        },
        participantStats: {
          totalParticipants: 0, // 需要参与者API支持
          onlineParticipants: 0,
          activeParticipants: 0,
          newParticipantsToday: 0
        },
        systemStats: {
          systemUptime: 0, // 需要系统监控API支持
          totalLogs: 0,
          errorLogsToday: 0,
          warningLogsToday: 0,
          systemLoad: 0,
          networkTraffic: 0
        }
      }
      
      set({
        overview,
        overviewLoading: false,
        overviewError: null,
        lastRefreshTime: new Date().toISOString()
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取概览数据失败'
      set({
        overviewLoading: false,
        overviewError: errorMessage
      })
      throw error
    }
  },

  /**
   * 获取图表数据
   */
  fetchChartData: async (timeRange?: string) => {
    const currentTimeRange = timeRange || get().timeRange
    set({ chartDataLoading: true, chartDataError: null })
    
    try {
      // 空的图表数据，需要真实API支持
      const chartData: DashboardChartData = {
        userActivityTrend: {
          dates: [],
          activeUsers: [],
          newUsers: [],
          loginCount: []
        },
        taskExecutionTrend: {
          dates: [],
          createdTasks: [],
          completedTasks: [],
          failedTasks: [],
          averageTrainingTime: []
        },
        systemPerformanceTrend: {
          timestamps: [],
          cpuUsage: [],
          memoryUsage: [],
          diskUsage: [],
          networkIn: [],
          networkOut: []
        },
        dataDistribution: {
          labels: [],
          datasets: [],
          categories: [],
          sizes: []
        },
        modelPerformanceDistribution: {
          accuracyRanges: [],
          modelCounts: [],
          averageAccuracy: []
        },
        logLevelDistribution: {
          levels: [],
          counts: [],
          percentages: []
        }
      }
      
      set({
        chartData,
        chartDataLoading: false,
        chartDataError: null
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取图表数据失败'
      set({
        chartDataLoading: false,
        chartDataError: errorMessage
      })
      throw error
    }
  },

  /**
   * 获取最近活动
   */
  fetchRecentActivities: async () => {
    set({ recentActivitiesLoading: true })
    
    try {
      // 空的最近活动数据，需要真实API支持
      const recentActivities: any[] = []
      
      set({
        recentActivities,
        recentActivitiesLoading: false
      })
    } catch (error) {
      set({ recentActivitiesLoading: false })
      throw error
    }
  },

  /**
   * 获取系统告警
   */
  fetchSystemAlerts: async () => {
    set({ systemAlertsLoading: true })
    
    try {
      // 空的系统告警数据，需要真实API支持
      const systemAlerts: any[] = []
      
      set({
        systemAlerts,
        systemAlertsLoading: false
      })
    } catch (error) {
      set({ systemAlertsLoading: false })
      throw error
    }
  },

  // ==================== 数据刷新 ====================
  
  /**
   * 刷新所有数据
   */
  refreshAllData: async () => {
    const { fetchOverview, fetchChartData, fetchRecentActivities, fetchSystemAlerts } = get()
    
    try {
      await Promise.allSettled([
        fetchOverview(),
        fetchChartData(),
        fetchRecentActivities(),
        fetchSystemAlerts()
      ])
      
      set({ lastRefreshTime: new Date().toISOString() })
    } catch (error) {
      console.error('刷新数据失败:', error)
    }
  },

  // ==================== 快速操作 ====================
  
  /**
   * 快速启动任务
   */
  quickStartTask: async (taskConfig: any) => {
    set((state) => ({
      quickActionLoading: {
        ...state.quickActionLoading,
        'start-task': true
      },
      quickActionError: {
        ...state.quickActionError,
        'start-task': null
      }
    }))
    
    try {
      // 这里应该调用 federatedTaskService.createTask
      // const response = await federatedTaskService.createTask(taskConfig)
      
      // 模拟创建任务
      const taskId = `FL-${Date.now()}`
      
      set((state) => ({
        quickActionLoading: {
          ...state.quickActionLoading,
          'start-task': false
        }
      }))
      
      return taskId
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '快速启动任务失败'
      set((state) => ({
        quickActionLoading: {
          ...state.quickActionLoading,
          'start-task': false
        },
        quickActionError: {
          ...state.quickActionError,
          'start-task': errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 快速创建虚拟机
   */
  quickCreateVM: async (vmConfig: any) => {
    set((state) => ({
      quickActionLoading: {
        ...state.quickActionLoading,
        'create-vm': true
      },
      quickActionError: {
        ...state.quickActionError,
        'create-vm': null
      }
    }))
    
    try {
      // 这里应该调用 vmService.createVM
      // const response = await vmService.createVM(vmConfig)
      
      // 模拟创建虚拟机
      const vmId = `VM-${Date.now()}`
      
      set((state) => ({
        quickActionLoading: {
          ...state.quickActionLoading,
          'create-vm': false
        }
      }))
      
      return vmId
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '快速创建虚拟机失败'
      set((state) => ({
        quickActionLoading: {
          ...state.quickActionLoading,
          'create-vm': false
        },
        quickActionError: {
          ...state.quickActionError,
          'create-vm': errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 快速上传数据
   */
  quickUploadData: async (file: File) => {
    set((state) => ({
      quickActionLoading: {
        ...state.quickActionLoading,
        'upload-data': true
      },
      quickActionError: {
        ...state.quickActionError,
        'upload-data': null
      }
    }))
    
    try {
      // 这里应该调用 trainingDataService.uploadFile
      const formData = new FormData()
      formData.append('file', file)
      // const response = await trainingDataService.uploadFile(formData)
      
      // 模拟上传数据
      const dataId = `DATA-${Date.now()}`
      
      set((state) => ({
        quickActionLoading: {
          ...state.quickActionLoading,
          'upload-data': false
        }
      }))
      
      return dataId
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '快速上传数据失败'
      set((state) => ({
        quickActionLoading: {
          ...state.quickActionLoading,
          'upload-data': false
        },
        quickActionError: {
          ...state.quickActionError,
          'upload-data': errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 设置 ====================
  
  /**
   * 设置时间范围
   */
  setTimeRange: (range: '1d' | '7d' | '30d' | '90d') => {
    set({ timeRange: range })
    // 重新获取图表数据
    get().fetchChartData(range).catch(console.error)
  },

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
      overviewError: null,
      chartDataError: null,
      quickActionError: {}
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
export type { DashboardState, DashboardActions, DashboardStore, DashboardOverview, DashboardChartData }
