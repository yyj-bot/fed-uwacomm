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
      // 并行获取各模块的统计数据
      const [
        // 这里应该调用各个服务的统计接口，但由于示例限制，我们模拟数据
      ] = await Promise.allSettled([
        // userService.getStatistics(),
        // vmService.getStatistics(),
        // federatedTaskService.getStatistics(),
        // trainingDataService.getStatistics(),
        // modelVersionService.getStatistics(),
        // systemLogService.getStatistics()
      ])
      
      // 模拟概览数据
      const overview: DashboardOverview = {
        userStats: {
          totalUsers: 156,
          activeUsers: 89,
          newUsersToday: 12,
          onlineUsers: 34
        },
        vmStats: {
          totalVMs: 45,
          runningVMs: 28,
          stoppedVMs: 15,
          errorVMs: 2,
          cpuUsage: 68.5,
          memoryUsage: 72.3,
          diskUsage: 45.8
        },
        taskStats: {
          totalTasks: 234,
          runningTasks: 18,
          completedTasks: 198,
          failedTasks: 18,
          tasksToday: 15,
          averageTrainingTime: 2.5
        },
        dataStats: {
          totalDatasets: 89,
          totalDataSize: 15.6, // GB
          processedData: 76,
          pendingData: 13,
          uploadedToday: 8
        },
        modelStats: {
          totalModels: 167,
          deployedModels: 23,
          trainingModels: 12,
          averageAccuracy: 87.6,
          modelsToday: 6
        },
        systemStats: {
          systemUptime: 15.6, // 天
          totalLogs: 125467,
          errorLogsToday: 23,
          warningLogsToday: 156,
          systemLoad: 0.68,
          networkTraffic: 1.2 // GB/hour
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
      // 生成模拟图表数据
      const chartData: DashboardChartData = {
        userActivityTrend: {
          dates: ['2024-01-01', '2024-01-02', '2024-01-03', '2024-01-04', '2024-01-05', '2024-01-06', '2024-01-07'],
          activeUsers: [45, 52, 48, 61, 55, 67, 59],
          newUsers: [3, 5, 2, 8, 4, 6, 7],
          loginCount: [89, 102, 95, 118, 107, 125, 112]
        },
        taskExecutionTrend: {
          dates: ['2024-01-01', '2024-01-02', '2024-01-03', '2024-01-04', '2024-01-05', '2024-01-06', '2024-01-07'],
          createdTasks: [5, 8, 6, 12, 9, 15, 11],
          completedTasks: [4, 7, 5, 10, 8, 13, 9],
          failedTasks: [1, 1, 1, 2, 1, 2, 2],
          averageTrainingTime: [2.3, 2.5, 2.1, 2.8, 2.4, 2.9, 2.6]
        },
        systemPerformanceTrend: {
          timestamps: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
          cpuUsage: [45, 38, 65, 72, 68, 58, 42],
          memoryUsage: [62, 58, 75, 82, 78, 70, 65],
          diskUsage: [45, 45, 46, 47, 47, 48, 48],
          networkIn: [1.2, 0.8, 2.1, 2.5, 2.3, 1.8, 1.1],
          networkOut: [0.9, 0.6, 1.8, 2.1, 1.9, 1.5, 0.8]
        },
        dataDistribution: {
          labels: ['图像数据', '文本数据', '音频数据', '视频数据', '表格数据'],
          datasets: [35, 28, 15, 12, 23],
          categories: ['训练集', '验证集', '测试集'],
          sizes: [65, 20, 15]
        },
        modelPerformanceDistribution: {
          accuracyRanges: ['90-100%', '80-90%', '70-80%', '60-70%', '<60%'],
          modelCounts: [23, 45, 67, 28, 12],
          averageAccuracy: [95.2, 85.6, 75.3, 65.8, 52.4]
        },
        logLevelDistribution: {
          levels: ['INFO', 'WARN', 'ERROR', 'DEBUG'],
          counts: [85234, 12456, 2345, 25432],
          percentages: [68.2, 10.0, 1.9, 20.3]
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
      // 模拟最近活动数据
      const recentActivities = [
        {
          id: '1',
          type: 'task_completed',
          title: '联邦学习任务 FL-2024-001 已完成',
          description: '训练轮数: 10, 参与节点: 5, 最终准确率: 92.5%',
          timestamp: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
          icon: 'task',
          status: 'success'
        },
        {
          id: '2',
          type: 'vm_started',
          title: '虚拟机 VM-GPU-001 已启动',
          description: '配置: 8 CPU, 32GB RAM, RTX 4090',
          timestamp: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
          icon: 'vm',
          status: 'info'
        },
        {
          id: '3',
          type: 'data_uploaded',
          title: '新数据集已上传',
          description: 'CIFAR-100 数据集, 大小: 2.5GB, 样本数: 60,000',
          timestamp: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
          icon: 'data',
          status: 'info'
        },
        {
          id: '4',
          type: 'model_deployed',
          title: '模型 ResNet-50-v2 已部署',
          description: '部署环境: 生产环境, 版本: 2.1.0',
          timestamp: new Date(Date.now() - 45 * 60 * 1000).toISOString(),
          icon: 'model',
          status: 'success'
        },
        {
          id: '5',
          type: 'system_alert',
          title: '系统内存使用率过高',
          description: '当前使用率: 87%, 建议优化内存分配',
          timestamp: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
          icon: 'alert',
          status: 'warning'
        }
      ]
      
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
      // 模拟系统告警数据
      const systemAlerts = [
        {
          id: '1',
          level: 'warning',
          title: 'GPU 温度过高',
          message: 'GPU-001 温度达到 82°C，请检查散热系统',
          timestamp: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
          resolved: false
        },
        {
          id: '2',
          level: 'error',
          title: '任务执行失败',
          message: '联邦学习任务 FL-2024-002 在第 3 轮训练时失败',
          timestamp: new Date(Date.now() - 25 * 60 * 1000).toISOString(),
          resolved: false
        },
        {
          id: '3',
          level: 'info',
          title: '系统维护通知',
          message: '系统将于今晚 23:00 进行例行维护，预计耗时 2 小时',
          timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
          resolved: false
        }
      ]
      
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
