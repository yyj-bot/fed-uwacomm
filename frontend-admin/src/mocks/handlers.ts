import { http, HttpResponse } from 'msw'
import type {
  ApiResponse,
  User,
  LoginRequest,
  LoginResponse,
  VirtualMachine,
  FederatedTask,
  ModelVersion,
  VMRoundModel,
  TrainingDataset,
  SystemLog,
  PaginatedResponse
} from '@/types'

// 模拟数据生成器
const mockData = {
  // 生成用户数据
  generateUser: (id: string): User => ({
    userId: id,
    username: `user_${id}`,
    email: `user_${id}@example.com`,
    role: 'RESEARCHER',
    status: 'ACTIVE',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }),

  // 生成虚拟机数据
  generateVM: (id: string): VirtualMachine => ({
    vmId: id,
    name: `vm_${id}`,
    ipAddress: '192.168.1.100',
    port: 8000,
    status: 'RUNNING',
    osType: 'Linux',
    cpuCores: 4,
    memoryMb: 8192,
    diskGb: 100,
    connectionStatus: 'CONNECTED',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }),

  // 生成联邦学习任务数据
  generateTask: (id: string): FederatedTask => ({
    taskId: id,
    taskName: `Task ${id}`,
    taskType: 'CLASSIFICATION',
    status: 'RUNNING',
    algorithm: 'FedAvg',
    createdAt: new Date().toISOString(),
    currentRound: 5,
    totalRounds: 10,
    progress: 0.5,
    participantCount: 3
  }),

  // 生成模型版本数据
  generateModelVersion: (id: string): ModelVersion => ({
    modelId: id,
    taskId: 'task_1',
    roundNumber: 5,
    accuracy: 0.85,
    loss: 0.15,
    status: 'VALIDATED',
    parameters: {},
    createdAt: new Date().toISOString()
  }),

  // 生成VM轮次模型数据
  generateVMRoundModel: (id: string): VMRoundModel => ({
    vmRoundModelId: id,
    vmId: 'vm_1',
    roundNumber: 5,
    metrics: {
      accuracy: 0.82,
      loss: 0.18
    },
    createdAt: new Date().toISOString()
  }),

  // 生成训练数据集数据
  generateDataset: (id: string): TrainingDataset => ({
    datasetId: id,
    datasetDescription: `Dataset ${id}`,
    datasetType: 'ACOUSTIC',
    vmId: 'vm_1',
    status: 'READY',
    uploadTime: new Date().toISOString(),
    uploadedBy: 'user_1'
  }),

  // 生成系统日志数据
  generateSystemLog: (id: string): SystemLog => ({
    logId: id,
    level: 'INFO',
    category: 'SYSTEM',
    message: `Log message ${id}`,
    createdAt: new Date().toISOString()
  })
}

// API处理程序
export const handlers = [
  // 用户认证
  http.post('/api/user/login', async ({ request }) => {
    const data = await request.json() as LoginRequest
    
    // 始终返回成功
    const response: ApiResponse<LoginResponse> = {
      code: 200,
      message: 'success',
      data: {
        token: 'mock_token_' + Date.now(),
        refreshToken: 'mock_refresh_token_' + Date.now(),
        expiresIn: 3600,
        user: {
          userId: '1',
          username: data.loginIdentifier,
          email: `${data.loginIdentifier}@example.com`,
          role: 'ADMIN',
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      }
    }
    return HttpResponse.json(response)
  }),

  // 用户信息
  http.get('/api/user/profile', () => {
    const response: ApiResponse<User> = {
      code: 200,
      message: 'success',
      data: mockData.generateUser('1')
    }
    return HttpResponse.json(response)
  }),

  // 刷新Token
  http.post('/api/user/refresh', () => {
    const response: ApiResponse<{ token: string; refreshToken: string; expiresIn: number }> = {
      code: 200,
      message: 'success',
      data: {
        token: 'mock_token_' + Date.now(),
        refreshToken: 'mock_refresh_token_' + Date.now(),
        expiresIn: 3600
      }
    }
    return HttpResponse.json(response)
  }),

  // ==================== 虚拟机管理API ====================
  http.get('/api/v1/vm/list', () => {
    const response: ApiResponse<{ total: number; page: number; size: number; pages: number; list: VirtualMachine[] }> = {
      code: 200,
      message: 'success',
      data: {
        total: 10,
        page: 1,
        size: 10,
        pages: 1,
        list: Array.from({ length: 10 }, (_, i) => mockData.generateVM(String(i + 1)))
      }
    }
    return HttpResponse.json(response)
  }),

  http.get('/api/v1/vm/:vmId', ({ params }) => {
    const response: ApiResponse<VirtualMachine> = {
      code: 200,
      message: 'success',
      data: mockData.generateVM(params.vmId as string)
    }
    return HttpResponse.json(response)
  }),

  // ==================== 联邦学习任务API ====================
  http.get('/api/federated/tasks', () => {
    const response: ApiResponse<{ total: number; page: number; size: number; tasks: FederatedTask[] }> = {
      code: 200,
      message: 'success',
      data: {
        total: 5,
        page: 1,
        size: 10,
        tasks: Array.from({ length: 5 }, (_, i) => mockData.generateTask(String(i + 1)))
      }
    }
    return HttpResponse.json(response)
  }),

  http.get('/api/federated/tasks/:taskId', ({ params }) => {
    const response: ApiResponse<FederatedTask> = {
      code: 200,
      message: 'success',
      data: mockData.generateTask(params.taskId as string)
    }
    return HttpResponse.json(response)
  }),

  // ==================== 模型版本管理API ====================
  http.get('/api/model/versions', () => {
    const response: ApiResponse<{ total: number; pages: number; current: number; size: number; records: ModelVersion[] }> = {
      code: 200,
      message: 'success',
      data: {
        total: 8,
        pages: 1,
        current: 1,
        size: 10,
        records: Array.from({ length: 8 }, (_, i) => mockData.generateModelVersion(String(i + 1)))
      }
    }
    return HttpResponse.json(response)
  }),

  // VM轮次模型API
  http.get('/api/model/vm-round-models', () => {
    const response: ApiResponse<{ total: number; pages: number; current: number; size: number; records: VMRoundModel[] }> = {
      code: 200,
      message: 'success',
      data: {
        total: 15,
        pages: 2,
        current: 1,
        size: 10,
        records: Array.from({ length: 15 }, (_, i) => mockData.generateVMRoundModel(String(i + 1)))
      }
    }
    return HttpResponse.json(response)
  }),

  // ==================== 训练数据管理API ====================
  http.get('/api/training-data', () => {
    const response: ApiResponse<{ total: number; page: number; size: number; dataList: TrainingDataset[] }> = {
      code: 200,
      message: 'success',
      data: {
        total: 12,
        page: 1,
        size: 10,
        dataList: Array.from({ length: 12 }, (_, i) => mockData.generateDataset(String(i + 1)))
      }
    }
    return HttpResponse.json(response)
  }),

  // ==================== 系统日志API ====================
  http.get('/api/log/list', () => {
    const response: ApiResponse<{ total: number; pages: number; current: number; size: number; records: SystemLog[] }> = {
      code: 200,
      message: 'success',
      data: {
        total: 50,
        pages: 5,
        current: 1,
        size: 10,
        records: Array.from({ length: 10 }, (_, i) => mockData.generateSystemLog(String(i + 1)))
      }
    }
    return HttpResponse.json(response)
  }),

  // ==================== 管理员用户管理API ====================
  http.get('/api/admin/user/list', () => {
    const response: ApiResponse<{ total: number; pages: number; current: number; size: number; records: User[] }> = {
      code: 200,
      message: 'success',
      data: {
        total: 20,
        pages: 2,
        current: 1,
        size: 10,
        records: Array.from({ length: 10 }, (_, i) => mockData.generateUser(String(i + 1)))
      }
    }
    return HttpResponse.json(response)
  }),

  // ==================== 系统健康状态API ====================
  http.get('/api/system/health', () => {
    const response: ApiResponse<any> = {
      code: 200,
      message: 'success',
      data: {
        status: 'healthy',
        uptime: '5d 12h 30m',
        memoryUsage: 0.65,
        cpuUsage: 0.45,
        diskUsage: 0.35,
        activeConnections: 25
      }
    }
    return HttpResponse.json(response)
  }),

  // 系统健康检查
  http.get('/api/health', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        status: 'UP',
        uptime: 86400,
        memory: 75.5,
        cpu: 45.2
      }
    })
  }),

  // ==================== 新增的Mock Handlers ====================

  // 用户管理接口 (文档第5节)
  http.get('/api/user/list', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        total: 10,
        page: 1,
        size: 10,
        list: [
          {
            userId: 'a1b2c3d4e5f678901234567890123456',
            username: 'admin',
            email: 'admin@example.com',
            role: 'ADMIN',
            status: 'ACTIVE',
            createdAt: '2024-01-01T10:00:00'
          },
          {
            userId: 'b2c3d4e5f67890123456789012345678',
            username: 'researcher',
            email: 'researcher@example.com',
            role: 'RESEARCHER',
            status: 'ACTIVE',
            createdAt: '2024-01-01T10:00:00'
          }
        ]
      }
    })
  }),

  http.get('/api/user/:userId', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        userId: 'a1b2c3d4e5f678901234567890123456',
        username: 'admin',
        email: 'admin@example.com',
        role: 'ADMIN',
        status: 'ACTIVE',
        lastLoginTime: '2024-01-01T10:00:00',
        createdAt: '2024-01-01T09:00:00'
      }
    })
  }),

  http.post('/api/user/create', () => {
    return HttpResponse.json({
      code: 200,
      message: '创建成功',
      data: {
        userId: 'c3d4e5f6789012345678901234567890',
        username: 'newuser',
        email: 'newuser@example.com',
        role: 'VIEWER',
        status: 'ACTIVE',
        createdAt: '2024-01-01T10:00:00'
      }
    })
  }),

  // VM注册和Token刷新接口
  http.post('/api/v1/vm/register', () => {
    return HttpResponse.json({
      code: 200,
      message: '虚拟机注册成功',
      data: {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        name: '水声联邦学习节点-001',
        status: 'OFFLINE',
        connectionStatus: 'DISCONNECTED',
        createdAt: '2024-01-01T00:00:00.000Z',
        sessionId: 'session-123456',
        accessToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
        secretId: 's3cr3t_8f14e45fceea167a5a36dedd4bea2543',
        tokenExpireSeconds: 86400,
        websocket: {
          sockjs: 'http://localhost:8080/ws',
          native: 'ws://localhost:8080/ws-native'
        },
        apiEndpoints: {
          status: '/api/v1/vm/a1b2c3d4e5f678901234567890123456/status',
          control: '/api/v1/vm/a1b2c3d4e5f678901234567890123456/control'
        }
      }
    })
  }),

  http.post('/api/v1/vm/token/refresh', () => {
    return HttpResponse.json({
      code: 200,
      message: '刷新成功',
      data: {
        accessToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
        tokenExpireSeconds: 86400,
        secretId: 's3cr3t_new_7c222fb2927d828af22f592134e8932480637c0d'
      }
    })
  }),

  // 模型评估接口
  http.post('/api/model/evaluate', () => {
    return HttpResponse.json({
      code: 200,
      message: '评估完成',
      data: {
        modelId: 'c3d4e5f6789012345678901234567890',
        evaluationId: 'eval_1234567890',
        metrics: {
          accuracy: 0.8500,
          loss: 0.123456,
          precision: 0.8200,
          recall: 0.8300,
          f1: 0.8250
        },
        evaluationTime: 15.5,
        testSamples: 1000,
        status: 'COMPLETED',
        createdAt: '2024-01-01T10:00:00'
      }
    })
  }),

  http.post('/api/model/evaluate/batch', () => {
    return HttpResponse.json({
      code: 200,
      message: '批量评估完成',
      data: {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        evaluatedCount: 3,
        results: [
          {
            modelId: 'c3d4e5f6789012345678901234567890',
            roundNumber: 1,
            accuracy: 0.8500,
            loss: 0.123456,
            status: 'COMPLETED'
          }
        ]
      }
    })
  }),

  http.get('/api/model/evaluate/results', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        total: 50,
        pages: 5,
        current: 1,
        size: 10,
        records: [
          {
            evaluationId: 'eval_1234567890',
            modelId: 'c3d4e5f6789012345678901234567890',
            taskId: 'a1b2c3d4e5f678901234567890123456',
            metrics: {
              accuracy: 0.8500,
              loss: 0.123456
            },
            evaluationTime: 15.5,
            testSamples: 1000,
            status: 'COMPLETED',
            createdAt: '2024-01-01T10:00:00'
          }
        ]
      }
    })
  }),

  // 模型部署接口
  http.post('/api/model/deploy', () => {
    return HttpResponse.json({
      code: 200,
      message: '部署成功',
      data: {
        deploymentId: 'deploy_1234567890',
        modelId: 'c3d4e5f6789012345678901234567890',
        deploymentName: '水声分类模型_v1.0',
        targetVms: ['vm_1', 'vm_2'],
        status: 'DEPLOYED',
        deploymentConfig: {
          replicas: 2,
          resources: {
            cpu: '1',
            memory: '2Gi'
          }
        },
        endpoints: [
          'http://vm_1:8080/predict',
          'http://vm_2:8080/predict'
        ],
        createdAt: '2024-01-01T10:00:00'
      }
    })
  }),

  http.get('/api/model/deploy/status/:deploymentId', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        deploymentId: 'deploy_1234567890',
        modelId: 'c3d4e5f6789012345678901234567890',
        deploymentName: '水声分类模型_v1.0',
        status: 'RUNNING',
        replicas: {
          desired: 2,
          available: 2,
          ready: 2
        },
        endpoints: [
          'http://vm_1:8080/predict',
          'http://vm_2:8080/predict'
        ],
        healthCheck: {
          status: 'HEALTHY',
          lastCheck: '2024-01-01T10:00:00',
          responseTime: 50
        },
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:00:00'
      }
    })
  }),

  http.get('/api/model/deploy/list', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        total: 20,
        pages: 2,
        current: 1,
        size: 10,
        records: [
          {
            deploymentId: 'deploy_1234567890',
            modelId: 'c3d4e5f6789012345678901234567890',
            deploymentName: '水声分类模型_v1.0',
            status: 'RUNNING',
            replicas: {
              desired: 2,
              available: 2
            },
            createdAt: '2024-01-01T10:00:00'
          }
        ]
      }
    })
  }),

  // 模型回滚接口
  http.post('/api/model/rollback', () => {
    return HttpResponse.json({
      code: 200,
      message: '回滚成功',
      data: {
        rollbackId: 'rollback_1234567890',
        deploymentId: 'deploy_1234567890',
        fromModelId: 'c3d4e5f6789012345678901234567890',
        toModelId: 'c3d4e5f6789012345678901234567891',
        status: 'COMPLETED',
        rollbackReason: '性能下降',
        rollbackTime: 30.5,
        createdAt: '2024-01-01T10:00:00'
      }
    })
  }),

  http.get('/api/model/rollback/history', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        total: 15,
        pages: 2,
        current: 1,
        size: 10,
        records: [
          {
            rollbackId: 'rollback_1234567890',
            deploymentId: 'deploy_1234567890',
            fromModelId: 'c3d4e5f6789012345678901234567890',
            toModelId: 'c3d4e5f6789012345678901234567891',
            status: 'COMPLETED',
            rollbackReason: '性能下降',
            rollbackTime: 30.5,
            createdAt: '2024-01-01T10:00:00'
          }
        ]
      }
    })
  }),

  // 任务模型版本查询
  http.get('/api/model/versions/task/:taskId', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        taskName: '水声分类任务',
        totalModels: 50,
        versions: [
          {
            modelId: 'c3d4e5f6789012345678901234567890',
            roundNumber: 1,
            accuracy: 0.8500,
            loss: 0.123456,
            status: 'UPLOADED',
            createdAt: '2024-01-01T10:00:00'
          }
        ]
      }
    })
  }),

  // 批量删除模型
  http.delete('/api/model/versions/batch', () => {
    return HttpResponse.json({
      code: 200,
      message: '批量删除成功',
      data: {
        successCount: 2,
        failedCount: 0,
        results: [
          {
            modelId: 'c3d4e5f6789012345678901234567890',
            status: 'DELETED',
            message: '删除成功'
          }
        ]
      }
    })
  }),

  // 模型统计接口
  http.get('/api/model/statistics', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        totalModels: 50,
        averageAccuracy: 0.85,
        averageLoss: 0.123456,
        uploadTrend: [
          {
            date: '2024-01-01',
            count: 5
          }
        ],
        accuracyTrend: [
          { roundNumber: 1, accuracy: 0.75 },
          { roundNumber: 2, accuracy: 0.80 },
          { roundNumber: 3, accuracy: 0.85 }
        ]
      }
    })
  }),

  http.get('/api/model/statistics/task/:taskId', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        taskName: '水声分类任务',
        totalRounds: 100,
        completedRounds: 50,
        performanceMetrics: {
          bestAccuracy: 0.9000,
          bestRound: 45,
          averageAccuracy: 0.8500,
          accuracyImprovement: 0.0500
        }
      }
    })
  }),

  // 训练数据管理接口
  http.put('/api/training-data/:datasetId', () => {
    return HttpResponse.json({
      code: 200,
      message: '数据更新成功',
      data: {
        datasetId: 'e5f67890123456789012345678901234',
        updatedAt: '2024-01-01T12:00:00',
        updatedBy: 'researcher'
      }
    })
  }),

  http.post('/api/training-data/batch', () => {
    return HttpResponse.json({
      code: 200,
      message: '批量操作成功',
      data: {
        operation: 'DELETE',
        total: 2,
        success: 2,
        failed: 0,
        results: [
          {
            datasetId: 'e5f67890123456789012345678901234',
            status: 'SUCCESS',
            message: '删除成功'
          }
        ]
      }
    })
  }),

  http.post('/api/training-data/export', () => {
    return HttpResponse.json({
      code: 200,
      message: '导出任务已启动',
      data: {
        taskId: 'export_1234567890',
        status: 'PROCESSING',
        format: 'CSV',
        startedAt: '2024-01-01T13:00:00',
        estimatedTime: 60,
        downloadUrl: '/api/training-data/export/download/export_1234567890'
      }
    })
  }),

  // 系统监控接口
  http.get('/api/log/monitor/system', () => {
    return HttpResponse.json({
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
    })
  }),

  http.get('/api/log/monitor/logs', () => {
    return HttpResponse.json({
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
          }
        ],
        categoryTrend: [
          {
            timestamp: '2024-01-01T10:00:00',
            SYSTEM: 200,
            USER: 150,
            VM: 100,
            TASK: 50
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
    })
  }),

  http.get('/api/log/monitor/performance', () => {
    return HttpResponse.json({
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
          }
        ],
        responseTimeTrend: [
          {
            timestamp: '2024-01-01T10:00:00',
            average: 200,
            p95: 500,
            p99: 1000
          }
        ]
      }
    })
  }),

  http.get('/api/log/monitor/alerts', () => {
    return HttpResponse.json({
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
          }
        ],
        alertHistory: [
          {
            alertId: 'alert_1234567890',
            triggeredAt: '2024-01-01T10:00:00',
            message: '错误率超过阈值：6.2%',
            severity: 'HIGH'
          }
        ]
      }
    })
  }),

  // 日志配置接口
  http.get('/api/log/config', () => {
    return HttpResponse.json({
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
          }
        },
        exportSettings: {
          maxRecordsPerExport: 100000,
          exportRetentionDays: 7,
          supportedFormats: ['CSV', 'JSON', 'EXCEL']
        }
      }
    })
  }),

  http.put('/api/log/config', () => {
    return HttpResponse.json({
      code: 200,
      message: '配置更新成功',
      data: {
        updatedAt: '2024-01-01T10:00:00'
      }
    })
  }),
] 