/**
 * 系统日志模拟数据
 * 基于 system-log-api-reference.md 文档设计
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { ApiResponse, SystemLog, PaginatedResponse } from '@/types'
import type {
  SystemLogPaginatedResponse,
  RealtimeLogsResponse,
  LogStatisticsResponse,
  LogExportResponse,
  ExportTask,
  LogCleanupResponse,
  CleanupTask,
  SystemMonitor,
  LogMonitor,
  PerformanceMonitor,
  AlertConfig,
  LogConfig,
  LogConfigUpdateResponse
} from '@/services/system-log/type'

// 模拟系统日志数据
const mockSystemLogs: SystemLog[] = [
  {
    logId: 'c3d4e5f6789012345678901234567890',
    level: 'INFO',
    category: 'SYSTEM',
    vmId: 'a1b2c3d4e5f678901234567890123456',
    taskId: 'b2c3d4e5f67890123456789012345678',
    message: '系统启动成功',
    details: {
      version: '1.0.0',
      startupTime: 5000
    },
    createdAt: '2024-01-01T10:00:00'
  },
  {
    logId: 'd4e5f67890123456789012345678901a',
    level: 'ERROR',
    category: 'TASK',
    vmId: 'a1b2c3d4e5f678901234567890123456',
    taskId: 'b2c3d4e5f67890123456789012345678',
    message: '联邦学习任务执行失败',
    details: {
      errorCode: 'TASK_EXECUTION_FAILED',
      errorMessage: '模型训练过程中出现异常',
      stackTrace: 'java.lang.Exception: ...',
      context: {
        round: 5,
        algorithm: 'FEDAVG',
        participants: 3
      }
    },
    createdAt: '2024-01-01T10:05:00'
  },
  {
    logId: 'e5f678901234567890123456789012bc',
    level: 'WARN',
    category: 'VM',
    vmId: 'a1b2c3d4e5f678901234567890123456',
    message: '虚拟机连接不稳定',
    details: {
      connectionStatus: 'UNSTABLE',
      lastHeartbeat: '2024-01-01T09:58:00',
      latency: 1500
    },
    createdAt: '2024-01-01T10:10:00'
  },
  {
    logId: 'f67890123456789012345678901234cd',
    level: 'INFO',
    category: 'USER',
    message: '用户登录成功',
    details: {
      userId: '123456789012345678901234567890ab',
      username: 'admin',
      loginIp: '192.168.1.100',
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    },
    createdAt: '2024-01-01T10:15:00'
  },
  {
    logId: '78901234567890123456789012345def',
    level: 'DEBUG',
    category: 'DATA',
    vmId: 'c3d4e5f6789012345678901234567890',
    message: '数据预处理完成',
    details: {
      datasetId: 'dataset_1234567890',
      recordCount: 10000,
      processingTime: 2500
    },
    createdAt: '2024-01-01T10:20:00'
  },
  {
    logId: '8901234567890123456789012345ef01',
    level: 'ERROR',
    category: 'SECURITY',
    message: '认证失败',
    details: {
      reason: 'INVALID_TOKEN',
      clientIp: '192.168.1.200',
      attemptCount: 3
    },
    createdAt: '2024-01-01T10:25:00'
  },
  {
    logId: '901234567890123456789012345f0123',
    level: 'INFO',
    category: 'MODEL',
    taskId: 'b2c3d4e5f67890123456789012345678',
    message: '模型版本上传完成',
    details: {
      modelId: 'model_1234567890',
      version: '1.0.1',
      fileSize: 1048576,
      accuracy: 0.95
    },
    createdAt: '2024-01-01T10:30:00'
  },
  {
    logId: '01234567890123456789012345012345',
    level: 'WARN',
    category: 'PERFORMANCE',
    message: '系统响应时间过长',
    details: {
      endpoint: '/api/task/create',
      responseTime: 3000,
      threshold: 2000
    },
    createdAt: '2024-01-01T10:35:00'
  }
]

// 模拟导出任务数据
const mockExportTasks: ExportTask[] = [
  {
    exportId: 'export_1234567890',
    status: 'COMPLETED',
    estimatedTime: 30,
    downloadUrl: 'http://localhost:8080/api/log/export/download/export_1234567890',
    expiresAt: '2024-01-02T10:00:00',
    progress: 100,
    totalRecords: 5000,
    processedRecords: 5000,
    fileSize: 1048576,
    createdAt: '2024-01-01T10:00:00',
    completedAt: '2024-01-01T10:00:30',
    format: 'CSV'
  },
  {
    exportId: 'export_2345678901',
    status: 'PROCESSING',
    estimatedTime: 45,
    downloadUrl: 'http://localhost:8080/api/log/export/download/export_2345678901',
    progress: 60,
    totalRecords: 8000,
    processedRecords: 4800,
    createdAt: '2024-01-01T11:00:00',
    format: 'JSON'
  }
]

// 模拟清理任务数据
const mockCleanupTasks: CleanupTask[] = [
  {
    cleanupId: 'cleanup_1234567890',
    status: 'COMPLETED',
    estimatedRecords: 5000,
    estimatedSize: 104857600,
    dryRun: false,
    progress: 100,
    deletedRecords: 5000,
    freedSpace: 104857600,
    createdAt: '2024-01-01T10:00:00',
    completedAt: '2024-01-01T10:00:30',
    strategy: 'TIME_BASED'
  },
  {
    cleanupId: 'cleanup_2345678901',
    status: 'PROCESSING',
    estimatedRecords: 3000,
    estimatedSize: 62914560,
    dryRun: false,
    progress: 75,
    deletedRecords: 2250,
    freedSpace: 47185920,
    createdAt: '2024-01-01T11:00:00',
    strategy: 'LEVEL_BASED'
  }
]

export const systemLogMock = {
  // ==================== 日志查询接口 ====================
  
  getLogList: (params: any = {}): ApiResponse<SystemLogPaginatedResponse<SystemLog>> => {
    let filteredLogs = [...mockSystemLogs]

    // 应用过滤条件
    if (params.level) {
      filteredLogs = filteredLogs.filter(log => log.level === params.level)
    }
    if (params.category) {
      filteredLogs = filteredLogs.filter(log => log.category === params.category)
    }
    if (params.vmId) {
      filteredLogs = filteredLogs.filter(log => log.vmId === params.vmId)
    }
    if (params.taskId) {
      filteredLogs = filteredLogs.filter(log => log.taskId === params.taskId)
    }
    if (params.keyword) {
      filteredLogs = filteredLogs.filter(log => 
        log.message.toLowerCase().includes(params.keyword.toLowerCase())
      )
    }

    // 应用分页
    const page = params.page || 1
    const size = params.size || 10
    const start = (page - 1) * size
    const end = start + size
    const paginatedLogs = filteredLogs.slice(start, end)

    return {
      code: 200,
      message: '查询成功',
      data: {
        total: filteredLogs.length,
        pages: Math.ceil(filteredLogs.length / size),
        current: page,
        size: size,
        records: paginatedLogs
      }
    }
  },

  getLogDetail: (logId: string): ApiResponse<SystemLog> => {
    const log = mockSystemLogs.find(l => l.logId === logId)
    if (!log) {
      return {
        code: 404,
        message: '日志不存在',
        data: null
      } as any
    }
    return {
      code: 200,
      message: '查询成功',
      data: log
    }
  },

  getRealtimeLogs: (params: any = {}): ApiResponse<RealtimeLogsResponse> => {
    let filteredLogs = [...mockSystemLogs]

    // 应用过滤条件
    if (params.level) {
      filteredLogs = filteredLogs.filter(log => log.level === params.level)
    }
    if (params.category) {
      filteredLogs = filteredLogs.filter(log => log.category === params.category)
    }
    if (params.vmId) {
      filteredLogs = filteredLogs.filter(log => log.vmId === params.vmId)
    }
    if (params.taskId) {
      filteredLogs = filteredLogs.filter(log => log.taskId === params.taskId)
    }

    // 应用tail限制
    const tail = params.tail || 100
    const recentLogs = filteredLogs.slice(-tail)

    return {
      code: 200,
      message: '查询成功',
      data: {
        logs: recentLogs,
        totalCount: recentLogs.length,
        lastUpdateTime: '2024-01-01T10:00:00'
      }
    }
  },

  getLogStatistics: (params: any = {}): ApiResponse<LogStatisticsResponse> => {
    return {
      code: 200,
      message: '查询成功',
      data: {
        totalLogs: 10000,
        levelDistribution: {
          DEBUG: 2000,
          INFO: 6000,
          WARN: 1500,
          ERROR: 500
        },
        categoryDistribution: {
          SYSTEM: 3000,
          USER: 2000,
          VM: 2500,
          TASK: 1500,
          DATA: 500,
          MODEL: 300,
          SECURITY: 100,
          PERFORMANCE: 100
        },
        timeDistribution: [
          { hour: '00', count: 500 },
          { hour: '01', count: 450 },
          { hour: '02', count: 400 },
          { hour: '03', count: 350 }
        ],
        errorTrend: [
          { date: '2024-01-01', errorCount: 50 },
          { date: '2024-01-02', errorCount: 45 },
          { date: '2024-01-03', errorCount: 40 }
        ]
      }
    }
  },

  // ==================== 日志导出接口 ====================

  exportLogs: (exportData: any): ApiResponse<LogExportResponse> => {
    const exportId = `export_${Date.now()}`
    return {
      code: 200,
      message: '导出任务已创建',
      data: {
        exportId: exportId,
        status: 'PROCESSING',
        estimatedTime: 30,
        downloadUrl: `http://localhost:8080/api/log/export/download/${exportId}`
      }
    }
  },

  getExportStatus: (exportId: string): ApiResponse<ExportTask> => {
    const task = mockExportTasks.find(t => t.exportId === exportId)
    if (!task) {
      // 如果找不到预定义的任务，返回一个动态生成的任务状态
      return {
        code: 200,
        message: '查询成功',
        data: {
          exportId: exportId,
          status: 'COMPLETED',
          estimatedTime: 30,
          downloadUrl: `http://localhost:8080/api/log/export/download/${exportId}`,
          expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
          progress: 100,
          totalRecords: 1000,
          processedRecords: 1000,
          fileSize: 204800,
          createdAt: new Date(Date.now() - 60000).toISOString(),
          completedAt: new Date().toISOString(),
          format: 'CSV'
        }
      }
    }
    return {
      code: 200,
      message: '查询成功',
      data: task
    }
  },

  downloadExportFile: (exportId: string): Blob => {
    // 模拟文件下载
    return new Blob(['mock export file content'], { type: 'text/csv' })
  },

  getExportHistory: (params: any = {}): ApiResponse<PaginatedResponse<ExportTask>> => {
    let filteredTasks = [...mockExportTasks]

    if (params.status) {
      filteredTasks = filteredTasks.filter(task => task.status === params.status)
    }

    const page = params.page || 1
    const size = params.size || 10
    const start = (page - 1) * size
    const end = start + size
    const paginatedTasks = filteredTasks.slice(start, end)

    return {
      code: 200,
      message: '查询成功',
      data: {
        total: filteredTasks.length,
        page: page,
        size: size,
        pages: Math.ceil(filteredTasks.length / size),
        records: paginatedTasks
      }
    }
  },

  // ==================== 日志清理接口 ====================

  cleanupLogs: (cleanupData: any): ApiResponse<LogCleanupResponse> => {
    const cleanupId = `cleanup_${Date.now()}`
    return {
      code: 200,
      message: '清理任务已创建',
      data: {
        cleanupId: cleanupId,
        status: 'PROCESSING',
        estimatedRecords: 5000,
        estimatedSize: 104857600,
        dryRun: cleanupData.dryRun || false
      }
    }
  },

  getCleanupStatus: (cleanupId: string): ApiResponse<CleanupTask> => {
    const task = mockCleanupTasks.find(t => t.cleanupId === cleanupId)
    if (!task) {
      // 如果找不到预定义的任务，返回一个动态生成的任务状态
      return {
        code: 200,
        message: '查询成功',
        data: {
          cleanupId: cleanupId,
          status: 'COMPLETED',
          estimatedRecords: 2000,
          estimatedSize: 52428800,
          dryRun: false,
          progress: 100,
          deletedRecords: 2000,
          freedSpace: 52428800,
          createdAt: new Date(Date.now() - 60000).toISOString(),
          completedAt: new Date().toISOString(),
          strategy: 'TIME_BASED'
        }
      }
    }
    return {
      code: 200,
      message: '查询成功',
      data: task
    }
  },

  getCleanupHistory: (params: any = {}): ApiResponse<PaginatedResponse<CleanupTask>> => {
    let filteredTasks = [...mockCleanupTasks]

    if (params.status) {
      filteredTasks = filteredTasks.filter(task => task.status === params.status)
    }

    const page = params.page || 1
    const size = params.size || 10
    const start = (page - 1) * size
    const end = start + size
    const paginatedTasks = filteredTasks.slice(start, end)

    return {
      code: 200,
      message: '查询成功',
      data: {
        total: filteredTasks.length,
        page: page,
        size: size,
        pages: Math.ceil(filteredTasks.length / size),
        records: paginatedTasks
      }
    }
  },

  // ==================== 系统监控接口 ====================

  getSystemMonitor: (): ApiResponse<SystemMonitor> => {
    return {
      code: 200,
      message: '查询成功',
      data: {
        systemInfo: {
          version: '1.0.0',
          uptime: 86400,
          startTime: '2024-01-01T00:00:00',
          javaVersion: '11.0.12',
          osInfo: 'Linux 5.4.0'
        },
        resourceUsage: {
          cpuUsage: 25.5,
          memoryUsage: 60.2,
          diskUsage: 45.8,
          networkIO: {
            bytesIn: 1048576,
            bytesOut: 2097152
          }
        },
        applicationMetrics: {
          activeConnections: 150,
          requestPerSecond: 25.5,
          averageResponseTime: 200,
          errorRate: 0.5
        },
        databaseMetrics: {
          activeConnections: 20,
          queryPerSecond: 100,
          averageQueryTime: 50
        }
      }
    }
  },

  getLogMonitor: (params: any = {}): ApiResponse<LogMonitor> => {
    return {
      code: 200,
      message: '查询成功',
      data: {
        logMetrics: {
          totalLogs: 10000,
          errorCount: 50,
          warningCount: 150,
          errorRate: 0.5,
          warningRate: 1.5
        },
        levelTrend: [
          {
            timestamp: '2024-01-01T10:00:00',
            DEBUG: 100,
            INFO: 500,
            WARN: 50,
            ERROR: 10
          },
          {
            timestamp: '2024-01-01T11:00:00',
            DEBUG: 120,
            INFO: 480,
            WARN: 45,
            ERROR: 8
          }
        ],
        categoryTrend: [
          {
            timestamp: '2024-01-01T10:00:00',
            SYSTEM: 200,
            USER: 150,
            VM: 100,
            TASK: 50
          },
          {
            timestamp: '2024-01-01T11:00:00',
            SYSTEM: 180,
            USER: 160,
            VM: 120,
            TASK: 45
          }
        ],
        recentErrors: [
          {
            logId: 'c3d4e5f6789012345678901234567890',
            level: 'ERROR',
            category: 'TASK',
            message: '任务执行失败',
            createdAt: '2024-01-01T10:00:00'
          }
        ]
      }
    }
  },

  getPerformanceMonitor: (params: any = {}): ApiResponse<PerformanceMonitor> => {
    return {
      code: 200,
      message: '查询成功',
      data: {
        apiMetrics: {
          totalRequests: 50000,
          successfulRequests: 49500,
          failedRequests: 500,
          successRate: 99.0,
          averageResponseTime: 200,
          p95ResponseTime: 500,
          p99ResponseTime: 1000
        },
        endpointMetrics: [
          {
            endpoint: '/api/user/login',
            requestCount: 1000,
            successRate: 98.5,
            averageResponseTime: 150,
            errorCount: 15
          },
          {
            endpoint: '/api/task/create',
            requestCount: 500,
            successRate: 99.2,
            averageResponseTime: 300,
            errorCount: 4
          }
        ],
        responseTimeTrend: [
          {
            timestamp: '2024-01-01T10:00:00',
            average: 200,
            p95: 500,
            p99: 1000
          },
          {
            timestamp: '2024-01-01T11:00:00',
            average: 180,
            p95: 450,
            p99: 900
          }
        ]
      }
    }
  },

  getAlertConfig: (): ApiResponse<AlertConfig> => {
    return {
      code: 200,
      message: '查询成功',
      data: {
        alerts: [
          {
            alertId: 'alert_1234567890',
            name: '错误率告警',
            type: 'ERROR_RATE',
            condition: 'error_rate > 5%',
            status: 'ACTIVE',
            lastTriggered: '2024-01-01T10:00:00',
            triggerCount: 5
          },
          {
            alertId: 'alert_2345678901',
            name: '响应时间告警',
            type: 'RESPONSE_TIME',
            condition: 'avg_response_time > 1000ms',
            status: 'INACTIVE',
            lastTriggered: '2024-01-01T08:00:00',
            triggerCount: 2
          }
        ],
        alertHistory: [
          {
            alertId: 'alert_1234567890',
            triggeredAt: '2024-01-01T10:00:00',
            message: '错误率超过阈值：6.2%',
            severity: 'HIGH'
          },
          {
            alertId: 'alert_2345678901',
            triggeredAt: '2024-01-01T08:00:00',
            message: '平均响应时间超过阈值：1200ms',
            severity: 'MEDIUM'
          }
        ]
      }
    }
  },

  // ==================== 日志配置接口 ====================

  getLogConfig: (): ApiResponse<LogConfig> => {
    return {
      code: 200,
      message: '查询成功',
      data: {
        logLevel: 'INFO',
        retentionDays: 30,
        maxFileSize: 104857600,
        categories: {
          SYSTEM: {
            level: 'INFO',
            enabled: true
          },
          USER: {
            level: 'INFO',
            enabled: true
          },
          VM: {
            level: 'DEBUG',
            enabled: true
          },
          TASK: {
            level: 'INFO',
            enabled: true
          },
          DATA: {
            level: 'WARN',
            enabled: true
          },
          MODEL: {
            level: 'INFO',
            enabled: true
          },
          SECURITY: {
            level: 'DEBUG',
            enabled: true
          },
          PERFORMANCE: {
            level: 'WARN',
            enabled: true
          }
        },
        exportSettings: {
          maxRecordsPerExport: 100000,
          exportRetentionDays: 7,
          supportedFormats: ['CSV', 'JSON', 'EXCEL']
        }
      }
    }
  },

  updateLogConfig: (configData: any): ApiResponse<LogConfigUpdateResponse> => {
    return {
      code: 200,
      message: '配置更新成功',
      data: {
        updatedAt: new Date().toISOString()
      }
    }
  }
} as const

export default systemLogMock
