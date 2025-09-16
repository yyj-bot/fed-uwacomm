/**
 * System Store Mock 数据
 * 用于测试系统监控相关功能的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { SystemLog } from '@/types'

// ==================== 模拟系统日志 ====================

export const mockSystemLog: SystemLog = {
  logId: 'log-001',
  level: 'INFO',
  category: 'SYSTEM',
  vmId: 'vm-001',
  taskId: 'task-001',
  message: '系统启动完成',
  details: {
    component: 'system-manager',
    action: 'startup',
    duration: 5000
  },
  createdAt: '2024-01-15T10:30:00Z'
}

export const mockSystemLogList: SystemLog[] = [
  mockSystemLog,
  {
    logId: 'log-002',
    level: 'WARN',
    category: 'VM',
    vmId: 'vm-002',
    message: '虚拟机内存使用率过高',
    details: {
      memoryUsage: 85.5,
      threshold: 80.0
    },
    createdAt: '2024-01-15T10:25:00Z'
  },
  {
    logId: 'log-003',
    level: 'ERROR',
    category: 'TASK',
    taskId: 'task-002',
    message: '联邦学习任务执行失败',
    details: {
      errorCode: 'TASK_EXECUTION_FAILED',
      reason: 'Insufficient participants'
    },
    createdAt: '2024-01-15T10:20:00Z'
  },
  {
    logId: 'log-004',
    level: 'DEBUG',
    category: 'DATA',
    message: '数据预处理完成',
    details: {
      datasetId: 'dataset-001',
      recordsProcessed: 10000,
      processingTime: 2500
    },
    createdAt: '2024-01-15T10:15:00Z'
  }
]

// ==================== 模拟实时日志 ====================

export const mockRealtimeLogs: SystemLog[] = [
  {
    logId: 'realtime-001',
    level: 'INFO',
    category: 'SYSTEM',
    message: '心跳检测正常',
    createdAt: new Date().toISOString()
  },
  {
    logId: 'realtime-002',
    level: 'INFO',
    category: 'VM',
    vmId: 'vm-001',
    message: '虚拟机状态更新',
    details: {
      status: 'RUNNING',
      cpuUsage: 45.2,
      memoryUsage: 68.5
    },
    createdAt: new Date(Date.now() - 30000).toISOString()
  }
]

// ==================== 模拟日志统计 ====================

export const mockLogStatistics = {
  totalLogs: 15420,
  levelDistribution: {
    DEBUG: 8500,
    INFO: 5200,
    WARN: 1320,
    ERROR: 400
  },
  categoryDistribution: {
    SYSTEM: 4200,
    USER: 2800,
    VM: 3500,
    TASK: 2400,
    DATA: 1800,
    MODEL: 720,
    SECURITY: 0,
    PERFORMANCE: 0
  },
  timeDistribution: [
    { hour: '00:00', count: 120 },
    { hour: '06:00', count: 450 },
    { hour: '12:00', count: 680 },
    { hour: '18:00', count: 520 }
  ],
  errorTrend: [
    { date: '2024-01-10', errorCount: 25 },
    { date: '2024-01-11', errorCount: 18 },
    { date: '2024-01-12', errorCount: 32 },
    { date: '2024-01-13', errorCount: 15 },
    { date: '2024-01-14', errorCount: 28 }
  ]
}

// ==================== 模拟系统监控数据 ====================

export const mockSystemMonitor = {
  cpu: {
    usage: 45.2,
    cores: 8,
    temperature: 65.5
  },
  memory: {
    usage: 68.5,
    total: 32768,
    used: 22425,
    free: 10343
  },
  disk: {
    usage: 35.8,
    total: 1024,
    used: 366,
    free: 658
  },
  network: {
    inbound: 1250,
    outbound: 850,
    connections: 45
  },
  processes: {
    total: 156,
    running: 12,
    sleeping: 144,
    zombie: 0
  }
}

export const mockLogMonitor = {
  errorRate: 2.5,
  warningRate: 8.3,
  infoRate: 65.2,
  debugRate: 24.0,
  recentErrors: [
    {
      timestamp: '2024-01-15T10:30:00Z',
      message: 'Database connection failed',
      count: 3
    },
    {
      timestamp: '2024-01-15T10:25:00Z',
      message: 'Task execution timeout',
      count: 1
    }
  ],
  topErrorSources: [
    { source: 'database-manager', count: 15 },
    { source: 'task-scheduler', count: 8 },
    { source: 'vm-manager', count: 5 }
  ]
}

export const mockPerformanceMonitor = {
  responseTime: 150,
  throughput: 1200,
  errorRate: 0.5,
  availability: 99.95,
  trends: {
    responseTime: [120, 135, 145, 150, 148, 152, 150],
    throughput: [1100, 1150, 1180, 1200, 1220, 1190, 1200],
    errorRate: [0.2, 0.3, 0.4, 0.5, 0.6, 0.4, 0.5]
  }
}

// ==================== 模拟配置数据 ====================

export const mockAlertConfig = {
  enabled: true,
  emailNotifications: true,
  smsNotifications: false,
  webhookUrl: 'https://api.example.com/webhooks/alerts',
  thresholds: {
    cpuUsage: 80,
    memoryUsage: 85,
    diskUsage: 90,
    errorRate: 5.0
  },
  recipients: [
    'admin@example.com',
    'ops@example.com'
  ]
}

export const mockLogConfig = {
  logLevel: 'INFO',
  retentionDays: 30,
  maxFileSize: 100, // MB
  enableRotation: true,
  rotationInterval: 'daily',
  compressionEnabled: true,
  remoteLogging: {
    enabled: false,
    endpoint: '',
    apiKey: ''
  }
}

// ==================== 模拟导出任务 ====================

export const mockExportTask = {
  exportId: 'export-001',
  status: 'COMPLETED',
  estimatedTime: 300,
  downloadUrl: '/api/logs/export/export-001/download',
  createdAt: '2024-01-15T10:00:00Z',
  format: 'JSON'
}

export const mockCleanupTask = {
  cleanupId: 'cleanup-001',
  status: 'COMPLETED',
  estimatedRecords: 1000,
  estimatedSize: '10MB',
  dryRun: false,
  createdAt: '2024-01-15T09:00:00Z',
  strategy: 'BY_DATE'
}

// ==================== 模拟错误信息 ====================

export const mockSystemErrors = {
  FETCH_LOG_LIST_ERROR: new Error('获取日志列表失败'),
  FETCH_LOG_DETAIL_ERROR: new Error('获取日志详情失败'),
  FETCH_REALTIME_LOGS_ERROR: new Error('获取实时日志失败'),
  FETCH_LOG_STATISTICS_ERROR: new Error('获取日志统计失败'),
  EXPORT_LOGS_ERROR: new Error('导出日志失败'),
  CLEANUP_LOGS_ERROR: new Error('清理日志失败'),
  FETCH_SYSTEM_MONITOR_ERROR: new Error('获取系统监控数据失败'),
  FETCH_LOG_MONITOR_ERROR: new Error('获取日志监控数据失败'),
  FETCH_PERFORMANCE_MONITOR_ERROR: new Error('获取性能监控数据失败'),
  FETCH_ALERT_CONFIG_ERROR: new Error('获取告警配置失败'),
  FETCH_LOG_CONFIG_ERROR: new Error('获取日志配置失败'),
  UPDATE_LOG_CONFIG_ERROR: new Error('更新日志配置失败'),
  NETWORK_ERROR: new Error('网络连接失败'),
  TIMEOUT_ERROR: new Error('请求超时'),
  SERVER_ERROR: new Error('服务器内部错误')
}

// ==================== 模拟初始状态 ====================

export const mockInitialSystemState = {
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
  refreshInterval: 30000
}

// ==================== 模拟工具函数 ====================

export const createMockSystemLog = (overrides: Partial<SystemLog> = {}): SystemLog => ({
  ...mockSystemLog,
  logId: `log-${Date.now()}`,
  createdAt: new Date().toISOString(),
  ...overrides
})

export const createMockLogStatistics = (overrides: any = {}) => ({
  ...mockLogStatistics,
  ...overrides
})

export const createMockSystemMonitor = (overrides: any = {}) => ({
  ...mockSystemMonitor,
  ...overrides
})

export const createMockExportTask = (overrides: any = {}) => ({
  ...mockExportTask,
  exportId: `export-${Date.now()}`,
  createdAt: new Date().toISOString(),
  ...overrides
})

export const createMockCleanupTask = (overrides: any = {}) => ({
  ...mockCleanupTask,
  cleanupId: `cleanup-${Date.now()}`,
  createdAt: new Date().toISOString(),
  ...overrides
})
