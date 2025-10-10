// 系统日志管理 API Mock数据
// 基于 system-log-api-reference.md 文档

/**
 * 日志级别类型
 */
type LogLevel = "DEBUG" | "INFO" | "WARN" | "ERROR";

/**
 * 日志类别类型
 */
type LogCategory = "SYSTEM" | "USER" | "VM" | "TASK" | "DATA" | "MODEL" | "SECURITY" | "PERFORMANCE";

/**
 * 清理策略类型
 */
type CleanupStrategy = "TIME_BASED" | "LEVEL_BASED" | "CATEGORY_BASED";

/**
 * 清理状态类型
 */
type CleanupStatus = "PROCESSING" | "COMPLETED" | "FAILED";

/**
 * Mock 日志数据接口
 */
interface MockLog {
  logId: string;
  level: LogLevel;
  category: LogCategory;
  vmId?: string;
  taskId?: string;
  message: string;
  details?: Record<string, any>;
  createdAt: string;
}

/**
 * Mock 日志数据
 */
export const mockLogs: MockLog[] = [
  {
    logId: "c3d4e5f678901234567890ab12345678",
    level: "INFO",
    category: "SYSTEM",
    message: "系统启动成功",
    details: {
      version: "1.0.0",
      startupTime: 5000
    },
    createdAt: "2024-01-01T10:00:00.000Z"
  },
  {
    logId: "d4e5f6789012345678901abc23456789",
    level: "INFO",
    category: "USER",
    message: "用户登录成功",
    details: {
      userId: "a1b2c3d4e5f678901234567890123456",
      username: "admin",
      ip: "192.168.1.100"
    },
    createdAt: "2024-01-01T10:05:00.000Z"
  },
  {
    logId: "e5f6789012345678901abcd234567890",
    level: "INFO",
    category: "VM",
    vmId: "a1b2c3d4e5f678901234567890123456",
    message: "虚拟机连接成功",
    details: {
      vmName: "VM-001",
      ip: "192.168.1.101"
    },
    createdAt: "2024-01-01T10:10:00.000Z"
  },
  {
    logId: "f67890123456789012bcde3456789012",
    level: "INFO",
    category: "TASK",
    taskId: "b2c3d4e5f67890123456789012345678",
    message: "联邦学习任务创建成功",
    details: {
      taskName: "MNIST训练任务",
      algorithm: "FEDAVG",
      participants: 3
    },
    createdAt: "2024-01-01T10:15:00.000Z"
  },
  {
    logId: "a7890123456789012cdef456789012ab",
    level: "WARN",
    category: "SYSTEM",
    message: "磁盘空间不足",
    details: {
      diskUsage: 85,
      threshold: 80
    },
    createdAt: "2024-01-01T10:20:00.000Z"
  },
  {
    logId: "b890123456789012defab56789012abc",
    level: "ERROR",
    category: "TASK",
    taskId: "b2c3d4e5f67890123456789012345678",
    vmId: "a1b2c3d4e5f678901234567890123456",
    message: "联邦学习任务执行失败",
    details: {
      errorCode: "TASK_EXECUTION_FAILED",
      errorMessage: "模型训练过程中出现异常",
      stackTrace: "java.lang.Exception: Model training failed at round 5",
      context: {
        round: 5,
        algorithm: "FEDAVG",
        participants: 3
      }
    },
    createdAt: "2024-01-01T10:25:00.000Z"
  },
  {
    logId: "c90123456789012efabc6789012abcde",
    level: "INFO",
    category: "DATA",
    message: "训练数据上传成功",
    details: {
      datasetId: "dataset_123",
      datasetName: "MNIST数据集",
      size: 52428800
    },
    createdAt: "2024-01-01T10:30:00.000Z"
  },
  {
    logId: "d0123456789012fabcd789012abcdef1",
    level: "INFO",
    category: "MODEL",
    message: "模型部署成功",
    details: {
      modelId: "model_456",
      modelName: "MNIST_v1.0",
      version: "1.0.0"
    },
    createdAt: "2024-01-01T10:35:00.000Z"
  },
  {
    logId: "e123456789012abcdef89012abcdef12",
    level: "WARN",
    category: "SECURITY",
    message: "检测到异常登录行为",
    details: {
      userId: "suspicious_user_id",
      ip: "192.168.1.200",
      reason: "多次登录失败"
    },
    createdAt: "2024-01-01T10:40:00.000Z"
  },
  {
    logId: "f23456789012bcdefab9012abcdef123",
    level: "DEBUG",
    category: "PERFORMANCE",
    message: "API响应时间监控",
    details: {
      endpoint: "/api/user/login",
      responseTime: 150,
      status: 200
    },
    createdAt: "2024-01-01T10:45:00.000Z"
  }
];

/**
 * 系统日志 API Mock数据
 */
export const systemLogApiMock = {
  // 3.1 日志列表查询 - GET /api/log/list
  list: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        total: 1000,
        pages: 100,
        current: 1,
        size: 10,
        records: mockLogs
      }
    },
    error404: {
      code: 404,
      message: "日志不存在",
      data: null
    }
  },

  // 3.2 日志详情查询 - GET /api/log/detail/{logId}
  detail: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        logId: "b890123456789012defab56789012abc",
        level: "ERROR",
        category: "TASK",
        vmId: "a1b2c3d4e5f678901234567890123456",
        taskId: "b2c3d4e5f67890123456789012345678",
        message: "联邦学习任务执行失败",
        details: {
          errorCode: "TASK_EXECUTION_FAILED",
          errorMessage: "模型训练过程中出现异常",
          stackTrace: "java.lang.Exception: Model training failed at round 5",
          context: {
            round: 5,
            algorithm: "FEDAVG",
            participants: 3
          }
        },
        createdAt: "2024-01-01T10:25:00.000Z"
      }
    },
    error404: {
      code: 404,
      message: "日志不存在",
      data: null
    }
  },

  // 3.3 实时日志查询 - GET /api/log/realtime
  realtime: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        logs: mockLogs.slice(-5),
        totalCount: 5,
        lastUpdateTime: new Date().toISOString()
      }
    }
  },

  // 3.4 日志统计查询 - GET /api/log/statistics
  statistics: {
    success: {
      code: 200,
      message: "查询成功",
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
          { hour: "00", count: 500 },
          { hour: "01", count: 450 },
          { hour: "02", count: 400 },
          { hour: "03", count: 350 },
          { hour: "04", count: 300 },
          { hour: "05", count: 400 },
          { hour: "06", count: 500 },
          { hour: "07", count: 600 },
          { hour: "08", count: 700 },
          { hour: "09", count: 800 },
          { hour: "10", count: 850 },
          { hour: "11", count: 900 },
          { hour: "12", count: 800 },
          { hour: "13", count: 750 },
          { hour: "14", count: 700 },
          { hour: "15", count: 650 },
          { hour: "16", count: 600 },
          { hour: "17", count: 550 },
          { hour: "18", count: 500 },
          { hour: "19", count: 450 },
          { hour: "20", count: 400 },
          { hour: "21", count: 350 },
          { hour: "22", count: 300 },
          { hour: "23", count: 250 }
        ],
        errorTrend: [
          { date: "2024-01-01", errorCount: 50 },
          { date: "2024-01-02", errorCount: 45 },
          { date: "2024-01-03", errorCount: 52 },
          { date: "2024-01-04", errorCount: 48 },
          { date: "2024-01-05", errorCount: 43 },
          { date: "2024-01-06", errorCount: 40 },
          { date: "2024-01-07", errorCount: 38 }
        ]
      }
    }
  },

  // 4.1 日志下载 - POST /api/log/download
  download: {
    success: {
      code: 200,
      message: "导出成功",
      data: {
        downloadUrl: "http://localhost:8080/downloads/logs_20240101.csv",
        expiresAt: new Date(Date.now() + 3600000).toISOString()
      }
    },
    error500: {
      code: 500,
      message: "日志导出失败",
      data: null
    }
  },

  // 5.1 日志清理 - POST /api/log/cleanup
  cleanup: {
    success: {
      code: 200,
      message: "清理任务已创建",
      data: {
        cleanupId: "cleanup_1234567890",
        status: "PROCESSING",
        estimatedRecords: 5000,
        estimatedSize: 104857600
      }
    },
    error500: {
      code: 500,
      message: "日志清理失败",
      data: null
    }
  },

  // 5.2 清理状态查询 - GET /api/log/cleanup/status/{cleanupId}
  cleanupStatus: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        cleanupId: "cleanup_1234567890",
        status: "COMPLETED",
        progress: 100,
        deletedRecords: 5000,
        freedSpace: 104857600,
        createdAt: "2024-01-01T10:00:00.000Z",
        completedAt: "2024-01-01T10:00:30.000Z"
      }
    },
    error404: {
      code: 404,
      message: "清理任务不存在",
      data: null
    }
  },

  // 5.3 清理历史查询 - GET /api/log/cleanup/history
  cleanupHistory: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        total: 20,
        pages: 2,
        current: 1,
        size: 10,
        records: [
          {
            cleanupId: "cleanup_1234567890",
            strategy: "TIME_BASED",
            status: "COMPLETED",
            deletedRecords: 5000,
            freedSpace: 104857600,
            createdAt: "2024-01-01T10:00:00.000Z",
            completedAt: "2024-01-01T10:00:30.000Z"
          },
          {
            cleanupId: "cleanup_1234567891",
            strategy: "LEVEL_BASED",
            status: "COMPLETED",
            deletedRecords: 3000,
            freedSpace: 62914560,
            createdAt: "2023-12-25T10:00:00.000Z",
            completedAt: "2023-12-25T10:00:25.000Z"
          }
        ]
      }
    }
  },

  // 6.1 系统状态监控 - GET /api/log/monitor/system
  monitorSystem: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        systemInfo: {
          version: "1.0.0",
          uptime: 86400,
          startTime: "2024-01-01T00:00:00.000Z",
          javaVersion: "11.0.12",
          osInfo: "Linux 5.4.0"
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
    },
    error503: {
      code: 503,
      message: "监控数据不可用",
      data: null
    }
  },

  // 6.2 日志监控 - GET /api/log/monitor/logs
  monitorLogs: {
    success: {
      code: 200,
      message: "查询成功",
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
            timestamp: "2024-01-01T10:00:00.000Z",
            DEBUG: 100,
            INFO: 500,
            WARN: 50,
            ERROR: 10
          },
          {
            timestamp: "2024-01-01T11:00:00.000Z",
            DEBUG: 110,
            INFO: 520,
            WARN: 48,
            ERROR: 8
          },
          {
            timestamp: "2024-01-01T12:00:00.000Z",
            DEBUG: 95,
            INFO: 510,
            WARN: 52,
            ERROR: 12
          }
        ],
        categoryTrend: [
          {
            timestamp: "2024-01-01T10:00:00.000Z",
            SYSTEM: 200,
            USER: 150,
            VM: 100,
            TASK: 50,
            DATA: 30,
            MODEL: 20,
            SECURITY: 10,
            PERFORMANCE: 10
          },
          {
            timestamp: "2024-01-01T11:00:00.000Z",
            SYSTEM: 210,
            USER: 160,
            VM: 105,
            TASK: 55,
            DATA: 32,
            MODEL: 22,
            SECURITY: 8,
            PERFORMANCE: 12
          }
        ],
        recentErrors: mockLogs.filter(log => log.level === "ERROR").slice(-5)
      }
    }
  },

  // 6.3 性能监控 - GET /api/log/monitor/performance
  monitorPerformance: {
    success: {
      code: 200,
      message: "查询成功",
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
            endpoint: "/api/user/login",
            requestCount: 1000,
            successRate: 98.5,
            averageResponseTime: 150,
            errorCount: 15
          },
          {
            endpoint: "/api/vm/list",
            requestCount: 5000,
            successRate: 99.8,
            averageResponseTime: 120,
            errorCount: 10
          },
          {
            endpoint: "/api/task/create",
            requestCount: 500,
            successRate: 97.5,
            averageResponseTime: 300,
            errorCount: 13
          }
        ],
        responseTimeTrend: [
          {
            timestamp: "2024-01-01T10:00:00.000Z",
            average: 200,
            p95: 500,
            p99: 1000
          },
          {
            timestamp: "2024-01-01T11:00:00.000Z",
            average: 210,
            p95: 520,
            p99: 1050
          },
          {
            timestamp: "2024-01-01T12:00:00.000Z",
            average: 195,
            p95: 480,
            p99: 980
          }
        ]
      }
    }
  },

  // 6.4 告警配置 - GET /api/log/monitor/alerts
  monitorAlerts: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        alerts: [
          {
            alertId: "alert_1234567890",
            name: "错误率告警",
            type: "ERROR_RATE",
            condition: "error_rate > 5%",
            status: "ACTIVE",
            lastTriggered: "2024-01-01T10:00:00.000Z",
            triggerCount: 5
          },
          {
            alertId: "alert_1234567891",
            name: "响应时间告警",
            type: "RESPONSE_TIME",
            condition: "p95_response_time > 1000ms",
            status: "ACTIVE",
            lastTriggered: "2024-01-01T09:30:00.000Z",
            triggerCount: 3
          },
          {
            alertId: "alert_1234567892",
            name: "磁盘空间告警",
            type: "DISK_USAGE",
            condition: "disk_usage > 80%",
            status: "TRIGGERED",
            lastTriggered: "2024-01-01T10:20:00.000Z",
            triggerCount: 2
          }
        ],
        alertHistory: [
          {
            alertId: "alert_1234567890",
            triggeredAt: "2024-01-01T10:00:00.000Z",
            message: "错误率超过阈值：6.2%",
            severity: "HIGH"
          },
          {
            alertId: "alert_1234567891",
            triggeredAt: "2024-01-01T09:30:00.000Z",
            message: "P95响应时间超过阈值：1050ms",
            severity: "MEDIUM"
          },
          {
            alertId: "alert_1234567892",
            triggeredAt: "2024-01-01T10:20:00.000Z",
            message: "磁盘使用率超过阈值：85%",
            severity: "HIGH"
          }
        ]
      }
    }
  },

  // 7.1 日志配置查询 - GET /api/log/config
  config: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        logLevel: "INFO",
        retentionDays: 30,
        maxFileSize: 104857600,
        categories: {
          SYSTEM: {
            level: "INFO",
            enabled: true
          },
          USER: {
            level: "INFO",
            enabled: true
          },
          VM: {
            level: "DEBUG",
            enabled: true
          },
          TASK: {
            level: "INFO",
            enabled: true
          },
          DATA: {
            level: "INFO",
            enabled: true
          },
          MODEL: {
            level: "INFO",
            enabled: true
          },
          SECURITY: {
            level: "WARN",
            enabled: true
          },
          PERFORMANCE: {
            level: "DEBUG",
            enabled: true
          }
        },
        downloadSettings: {
          maxRecordsPerDownload: 100000,
          supportedFormats: ["CSV", "JSON", "EXCEL"]
        }
      }
    }
  },

  // 7.2 日志配置更新 - PUT /api/log/config
  updateConfig: {
    success: {
      code: 200,
      message: "配置更新成功",
      data: {
        updatedAt: new Date().toISOString()
      }
    },
    error400: {
      code: 400,
      message: "日志配置无效",
      data: {
        field: "logLevel",
        error: "日志级别必须为 DEBUG/INFO/WARN/ERROR 之一"
      }
    }
  }
};

/**
 * 请求参数示例
 */
export const systemLogApiRequestExamples = {
  // 日志列表查询参数
  list: {
    level: "INFO",
    category: "SYSTEM",
    vmId: "a1b2c3d4e5f678901234567890123456",
    taskId: "b2c3d4e5f67890123456789012345678",
    startTime: "2024-01-01T00:00:00",
    endTime: "2024-01-02T00:00:00",
    keyword: "系统",
    page: 1,
    size: 10,
    sort: "createdAt",
    order: "desc"
  },

  // 实时日志查询参数
  realtime: {
    level: "INFO",
    category: "SYSTEM",
    vmId: "a1b2c3d4e5f678901234567890123456",
    taskId: "b2c3d4e5f67890123456789012345678",
    tail: 100
  },

  // 日志统计查询参数
  statistics: {
    startTime: "2024-01-01T00:00:00",
    endTime: "2024-01-02T00:00:00",
    category: "SYSTEM",
    vmId: "a1b2c3d4e5f678901234567890123456",
    taskId: "b2c3d4e5f67890123456789012345678"
  },

  // 日志下载参数
  download: {
    level: "INFO",
    category: "SYSTEM",
    vmId: "a1b2c3d4e5f678901234567890123456",
    taskId: "b2c3d4e5f67890123456789012345678",
    startTime: "2024-01-01T00:00:00",
    endTime: "2024-01-02T00:00:00",
    keyword: "系统",
    format: "CSV",
    includeDetails: true
  },

  // 日志清理参数
  cleanup: {
    strategy: "TIME_BASED",
    retentionDays: 30,
    level: "DEBUG",
    category: "SYSTEM",
    vmId: "a1b2c3d4e5f678901234567890123456",
    taskId: "b2c3d4e5f67890123456789012345678",
    startTime: "2024-01-01T00:00:00",
    endTime: "2024-01-02T00:00:00"
  },

  // 日志配置更新参数
  updateConfig: {
    logLevel: "INFO",
    retentionDays: 30,
    maxFileSize: 104857600,
    categories: {
      SYSTEM: {
        level: "INFO",
        enabled: true
      }
    },
    downloadSettings: {
      maxRecordsPerDownload: 100000
    }
  }
};

