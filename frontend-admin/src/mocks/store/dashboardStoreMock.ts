/**
 * Dashboard Store Mock 数据
 * 用于测试仪表盘相关功能的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { DashboardOverview, DashboardChartData } from '@/store/dashboard/dashboardStore'

// ==================== 模拟概览数据 ====================

export const mockDashboardOverview: DashboardOverview = {
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
    totalDataSize: 15.6,
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
    systemUptime: 15.6,
    totalLogs: 125467,
    errorLogsToday: 23,
    warningLogsToday: 156,
    systemLoad: 0.68,
    networkTraffic: 1.2
  }
}

// ==================== 模拟图表数据 ====================

export const mockDashboardChartData: DashboardChartData = {
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

// ==================== 模拟活动数据 ====================

export const mockRecentActivities = [
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
  }
]

// ==================== 模拟系统告警 ====================

export const mockSystemAlerts = [
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

// ==================== 模拟错误信息 ====================

export const mockDashboardErrors = {
  FETCH_OVERVIEW_ERROR: new Error('获取概览数据失败'),
  FETCH_CHART_DATA_ERROR: new Error('获取图表数据失败'),
  FETCH_ACTIVITIES_ERROR: new Error('获取最近活动失败'),
  FETCH_ALERTS_ERROR: new Error('获取系统告警失败'),
  QUICK_START_TASK_ERROR: new Error('快速启动任务失败'),
  QUICK_CREATE_VM_ERROR: new Error('快速创建虚拟机失败'),
  QUICK_UPLOAD_DATA_ERROR: new Error('快速上传数据失败'),
  NETWORK_ERROR: new Error('网络连接失败'),
  TIMEOUT_ERROR: new Error('请求超时'),
  SERVER_ERROR: new Error('服务器内部错误')
}

// ==================== 模拟初始状态 ====================

export const mockInitialDashboardState = {
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
  refreshInterval: 60000,
  timeRange: '7d' as const
}

// ==================== 模拟工具函数 ====================

export const createMockDashboardOverview = (overrides: Partial<DashboardOverview> = {}): DashboardOverview => ({
  ...mockDashboardOverview,
  ...overrides
})

export const createMockDashboardChartData = (overrides: Partial<DashboardChartData> = {}): DashboardChartData => ({
  ...mockDashboardChartData,
  ...overrides
})

export const createMockRecentActivity = (overrides: any = {}) => ({
  id: `activity-${Date.now()}`,
  type: 'system_info',
  title: '系统信息更新',
  description: '系统状态已更新',
  timestamp: new Date().toISOString(),
  icon: 'info',
  status: 'info',
  ...overrides
})

export const createMockSystemAlert = (overrides: any = {}) => ({
  id: `alert-${Date.now()}`,
  level: 'info',
  title: '系统通知',
  message: '系统运行正常',
  timestamp: new Date().toISOString(),
  resolved: false,
  ...overrides
})
