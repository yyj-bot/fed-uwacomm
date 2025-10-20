/**
 * 联邦学习任务管理 API Mock Handlers
 * 严格按照 federated-task-api-reference.md 规范实现
 * 
 * @author FedUWAComm Team
 * @version 1.4.0
 */

import { http, HttpResponse } from 'msw'
import { 
  mockFederatedTasks,
  mockFederatedTaskDetails,
  mockTaskResults,
  mockTaskLogs,
  mockAvailableVMs,
  mockAvailableDatasets,
  mockRoleConfigs,
  mockAlgorithmTemplates,
  mockAggregationEngineStatus,
  mockAggregationStrategies,
  createMockTaskId
} from '../data/federatedTaskMockData'

// ==================== 联邦学习任务管理 API Mock Handlers ====================

export const federatedTaskHandlers = [
  
  // ==================== 3.0 图形化配置接口组 (v1.3 新增) ====================
  
  // 3.0.1 获取可用虚拟机列表
  http.get('/api/federated/config/available-vms', async ({ request }) => {
    const url = new URL(request.url)
    const algorithm = url.searchParams.get('algorithm')
    const minCpuCores = url.searchParams.get('minCpuCores')
    const minMemoryMb = url.searchParams.get('minMemoryMb')
    const status = url.searchParams.get('status')
    const capabilities = url.searchParams.get('capabilities')
    
    let filteredVMs = [...mockAvailableVMs]
    
    if (status) {
      filteredVMs = filteredVMs.filter(vm => vm.status === status)
    }
    if (minCpuCores) {
      filteredVMs = filteredVMs.filter(vm => vm.resources.cpuCores >= parseInt(minCpuCores))
    }
    if (minMemoryMb) {
      filteredVMs = filteredVMs.filter(vm => vm.resources.memoryMb >= parseInt(minMemoryMb))
    }
    if (capabilities) {
      filteredVMs = filteredVMs.filter(vm => 
        vm.capabilities.some(cap => cap.toLowerCase().includes(capabilities.toLowerCase()))
      )
    }
    
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        total: filteredVMs.length,
        availableVms: filteredVMs
      }
    })
  }),

  // 3.0.2 获取可用数据集列表
  http.get('/api/federated/config/available-datasets', async ({ request }) => {
    const url = new URL(request.url)
    const dataType = url.searchParams.get('dataType')
    const status = url.searchParams.get('status')
    const keyword = url.searchParams.get('keyword')
    
    let filteredDatasets = [...mockAvailableDatasets]
    
    if (dataType) {
      filteredDatasets = filteredDatasets.filter(dataset => dataset.dataType === dataType)
    }
    if (status) {
      filteredDatasets = filteredDatasets.filter(dataset => dataset.status === status)
    }
    if (keyword) {
      filteredDatasets = filteredDatasets.filter(dataset => 
        dataset.name.toLowerCase().includes(keyword.toLowerCase()) ||
        dataset.description.toLowerCase().includes(keyword.toLowerCase())
      )
    }
    
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        total: filteredDatasets.length,
        availableDatasets: filteredDatasets
      }
    })
  }),

  // 3.0.3 数据分配预览
  http.post('/api/federated/tasks/preview-distribution', async ({ request }) => {
    const body = await request.json() as any
    
    const { datasetId, distributionStrategy, participants } = body
    
    // 模拟数据分配计算
    const totalRows = 10000 // 假设数据集总行数
    let allocatedParticipants = participants.map((p: any, index: number) => {
      const allocatedRatio = distributionStrategy === 'BALANCED' 
        ? 1 / participants.length 
        : p.requestedRatio
      
      return {
        vmId: p.vmId,
        vmName: `水声联邦学习节点-${String(index + 1).padStart(3, '0')}`,
        allocatedRatio,
        allocatedRows: Math.floor(totalRows * allocatedRatio),
        estimatedTrainingTime: Math.floor(300 + Math.random() * 300) // 5-10分钟
      }
    })
    
    return HttpResponse.json({
      code: 200,
      message: '预览生成成功',
      data: {
        distributionResult: {
          participants: allocatedParticipants
        },
        qualityMetrics: {
          iidScore: 0.85,
          balanceScore: 0.92
        }
      }
    })
  }),

  // 3.0.4 参与者验证
  http.post('/api/federated/tasks/validate-participants', async ({ request }) => {
    const body = await request.json() as any
    
    const { algorithm, taskType, participants } = body
    
    const participantValidations = participants.map((p: any) => ({
      vmId: p.vmId,
      isValid: true,
      validationResults: {
        connectivity: {
          status: 'PASS' as const,
          message: '网络连接正常'
        },
        resources: {
          status: 'PASS' as const,
          message: '资源满足要求'
        }
      }
    }))
    
    return HttpResponse.json({
      code: 200,
      message: '验证完成',
      data: {
        overallValid: true,
        participantValidations
      }
    })
  }),

  // 3.0.5 获取角色配置选项
  http.get('/api/federated/config/roles', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        roles: mockRoleConfigs
      }
    })
  }),

  // 3.0.6 获取算法配置模板
  http.get('/api/federated/config/algorithm-templates', () => {
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        templates: mockAlgorithmTemplates
      }
    })
  }),

  // ==================== 3.1-3.16 任务管理接口 ====================
  
  // 3.1 任务创建接口 (v1.3 增强)
  http.post('/api/federated/tasks', async ({ request }) => {
    const body = await request.json() as any
    
    const taskId = createMockTaskId()
    const hasNewFormat = body.datasetConfig || body.participantConfig
    
    // 检查是否使用了废弃格式
    const warnings = []
    if (!hasNewFormat && body.participants) {
      warnings.push({
        code: 'SIMPLE_PARTICIPANT_CONFIG_DEPRECATED',
        message: '简化的参与者配置格式已废弃，建议使用新的participantConfig结构',
        details: {
          deprecationVersion: 'v1.3',
          removalVersion: 'v2.0',
          migrationGuide: '/docs/api/migration-guide-v1.3.md'
        }
      })
    }
    
    const participantCount = body.participantConfig?.participants?.length || body.participants?.length || 2
    
    const responseData = {
      taskId,
      taskName: body.taskName || '水声传播特征分类任务',
      status: 'CREATED',
      createdAt: new Date().toISOString(),
      createdBy: 'd4e5f678901234567890123456789012',
      participantCount,
      estimatedDuration: 46500,
      // v1.3 新增字段
      ...(hasNewFormat && {
        configSummary: {
          dataset: body.datasetConfig ? {
            datasetId: body.datasetConfig.datasetId,
            totalRows: 10000,
            distributionStrategy: body.datasetConfig.distributionStrategy || 'BALANCED'
          } : undefined,
          participants: (body.participantConfig?.participants || body.participants || []).map((p: any, index: number) => ({
            vmId: p.vmId,
            vmName: `水声联邦学习节点-${String(index + 1).padStart(3, '0')}`,
            role: p.role || 'PARTICIPANT',
            dataRatio: p.dataRatio || (1 / participantCount)
          }))
        },
        performanceEstimation: {
          expectedAccuracy: 0.85,
          convergenceRounds: 8,
          networkTraffic: '2.3GB'
        }
      })
    }
    
    return HttpResponse.json({
      code: 200,
      message: '任务创建成功',
      data: responseData,
      ...(warnings.length > 0 && { warnings })
    })
  }),

  // 3.2 任务配置接口
  http.put('/api/federated/tasks/:taskId/config', async ({ params, request }) => {
    const taskId = params.taskId as string
    const body = await request.json() as any
    
    return HttpResponse.json({
      code: 200,
      message: '任务配置成功',
      data: {
        taskId,
        status: 'CONFIGURED',
        updatedAt: new Date().toISOString(),
        configVersion: 'v1.1'
      }
    })
  }),

  // 3.3 任务启动接口
  http.post('/api/federated/tasks/:taskId/start', async ({ params }) => {
    const taskId = params.taskId as string
    
    return HttpResponse.json({
      code: 200,
      message: '任务启动成功',
      data: {
        taskId,
        status: 'RUNNING',
        startedAt: new Date().toISOString(),
        currentRound: 0,
        participants: [
          {
            vmId: 'a1b2c3d4e5f678901234567890123456',
            status: 'CONNECTED',
            dataSource: 'bellhop_features_001.csv'
          },
          {
            vmId: 'b2c3d4e5f67890123456789012345678',
            status: 'CONNECTED',
            dataSource: 'bellhop_features_002.csv'
          }
        ]
      }
    })
  }),

  // 3.4 任务暂停接口
  http.post('/api/federated/tasks/:taskId/pause', async ({ params }) => {
    const taskId = params.taskId as string
    
    return HttpResponse.json({
      code: 200,
      message: '任务暂停成功',
      data: {
        taskId,
        status: 'PAUSED',
        pausedAt: new Date().toISOString(),
        currentRound: 5,
        resumePoint: {
          round: 5,
          step: 'AGGREGATION'
        }
      }
    })
  }),

  // 3.5 任务恢复接口
  http.post('/api/federated/tasks/:taskId/resume', async ({ params }) => {
    const taskId = params.taskId as string
    
    return HttpResponse.json({
      code: 200,
      message: '任务恢复成功',
      data: {
        taskId,
        status: 'RUNNING',
        resumedAt: new Date().toISOString(),
        currentRound: 5
      }
    })
  }),

  // 3.6 任务停止接口
  http.post('/api/federated/tasks/:taskId/stop', async ({ params, request }) => {
    const taskId = params.taskId as string
    const body = await request.json() as any
    
    return HttpResponse.json({
      code: 200,
      message: '任务停止成功',
      data: {
        taskId,
        status: 'STOPPED',
        stoppedAt: new Date().toISOString(),
        finalRound: 8,
        checkpointSaved: body.saveCheckpoint !== false,
        checkpointPath: body.saveCheckpoint !== false ? `/checkpoints/task_${taskId}_round_8.pkl` : undefined
      }
    })
  }),

  // 3.7 任务取消接口
  http.post('/api/federated/tasks/:taskId/cancel', async ({ params, request }) => {
    const taskId = params.taskId as string
    const body = await request.json() as any
    
    return HttpResponse.json({
      code: 200,
      message: '任务取消成功',
      data: {
        taskId,
        status: 'CANCELLED',
        cancelledAt: new Date().toISOString(),
        reason: body.reason || '用户取消'
      }
    })
  }),

  // 3.8 任务状态查询接口
  http.get('/api/federated/tasks/:taskId', async ({ params }) => {
    const taskId = params.taskId as string
    
    // 查找对应的任务详情
    const taskDetail = mockFederatedTaskDetails.find(task => task.taskId === taskId) || mockFederatedTaskDetails[0]
    
    // 查找对应的任务结果，用于获取训练历史
    const taskResults = mockTaskResults.find(result => result.taskId === taskId)
    
    // 从 roundResults 生成训练历史数据
    const trainingHistory = taskResults?.roundResults?.map(round => ({
      round: round.round,
      accuracy: round.accuracy,
      loss: round.loss
    })) || []
    
    console.log('🔍 [Mock] 任务详情查询:', {
      taskId,
      hasTaskResults: !!taskResults,
      roundResultsCount: taskResults?.roundResults?.length || 0,
      trainingHistoryCount: trainingHistory.length,
      trainingHistory
    })
    
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        ...taskDetail,
        taskId, // 确保使用请求的taskId
        trainingHistory // 添加训练历史数据
      }
    })
  }),

  // 3.9 任务列表查询接口
  http.get('http://localhost:5173/api/federated/tasks', async ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '20')
    const status = url.searchParams.get('status')
    const type = url.searchParams.get('type')
    const keyword = url.searchParams.get('keyword')
    const startDate = url.searchParams.get('startDate')
    const endDate = url.searchParams.get('endDate')
    
    // 使用 mockFederatedTaskDetails 而不是 mockFederatedTasks，以包含 participants 信息
    let filteredTasks = [...mockFederatedTaskDetails]
    
    // 应用过滤条件
    if (status) {
      filteredTasks = filteredTasks.filter(task => task.status === status)
    }
    if (type) {
      filteredTasks = filteredTasks.filter(task => task.taskType === type)
    }
    if (keyword) {
      filteredTasks = filteredTasks.filter(task => 
        task.taskName.toLowerCase().includes(keyword.toLowerCase())
      )
    }
    if (startDate) {
      filteredTasks = filteredTasks.filter(task => {
        const taskDate = new Date(task.createdAt)
        const filterStartDate = new Date(startDate)
        return taskDate >= filterStartDate
      })
    }
    if (endDate) {
      filteredTasks = filteredTasks.filter(task => {
        const taskDate = new Date(task.createdAt)
        // 将结束日期设为当天的23:59:59
        const filterEndDate = new Date(endDate)
        filterEndDate.setHours(23, 59, 59, 999)
        return taskDate <= filterEndDate
      })
    }
    
    // 分页
    const startIndex = (page - 1) * size
    const endIndex = startIndex + size
    const paginatedTasks = filteredTasks.slice(startIndex, endIndex)
    
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        total: filteredTasks.length,
        page,
        size,
        tasks: paginatedTasks
      }
    })
  }),

  // 3.10 任务结果查询接口
  http.get('/api/federated/tasks/:taskId/results', async ({ params }) => {
    const taskId = params.taskId as string
    
    const taskResults = mockTaskResults.find(result => result.taskId === taskId) || mockTaskResults[0]
    
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        ...taskResults,
        taskId
      }
    })
  }),

  // 3.11 配置状态监控接口 (v1.3 新增)
  http.get('/api/federated/tasks/:taskId/config-status', async ({ params }) => {
    const taskId = params.taskId as string
    
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        taskId,
        configStatus: 'READY',
        configurationSteps: [
          {
            step: 'DATASET_DISTRIBUTION',
            status: 'COMPLETED',
            completedAt: new Date(Date.now() - 900000).toISOString() // 15分钟前
          },
          {
            step: 'PARTICIPANT_VALIDATION',
            status: 'COMPLETED',
            completedAt: new Date(Date.now() - 300000).toISOString() // 5分钟前
          },
          {
            step: 'MODEL_INITIALIZATION',
            status: 'COMPLETED',
            completedAt: new Date().toISOString()
          }
        ],
        participantStatuses: [
          {
            vmId: 'a1b2c3d4e5f678901234567890123456',
            configStatus: 'READY',
            dataDistributed: true,
            modelInitialized: true
          },
          {
            vmId: 'b2c3d4e5f67890123456789012345678',
            configStatus: 'READY',
            dataDistributed: true,
            modelInitialized: true
          }
        ]
      }
    })
  }),

  // 3.12 资源使用监控接口 (v1.3 新增)
  http.get('/api/federated/tasks/:taskId/resource-usage', async ({ params }) => {
    const taskId = params.taskId as string
    
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        taskId,
        participantMetrics: [
          {
            vmId: 'a1b2c3d4e5f678901234567890123456',
            currentUsage: {
              cpu: 75.5,
              memory: 68.2,
              network: {
                inbound: 15.6,
                outbound: 12.3
              }
            },
            averageUsage: {
              cpu: 72.1,
              memory: 65.8
            }
          },
          {
            vmId: 'b2c3d4e5f67890123456789012345678',
            currentUsage: {
              cpu: 72.1,
              memory: 65.4,
              network: {
                inbound: 14.2,
                outbound: 11.8
              }
            },
            averageUsage: {
              cpu: 69.8,
              memory: 62.3
            }
          }
        ],
        aggregatedMetrics: {
          totalCpuUsage: 73.8,
          totalMemoryUsage: 66.8,
          taskProgress: 55.6
        }
      }
    })
  }),

  // 3.13 任务日志查询接口
  http.get('/api/federated/tasks/:taskId/logs', async ({ params, request }) => {
    const taskId = params.taskId as string
    const url = new URL(request.url)
    const level = url.searchParams.get('level')
    const startTime = url.searchParams.get('startTime')
    const endTime = url.searchParams.get('endTime')
    const keyword = url.searchParams.get('keyword')
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '10')
    
    // 获取对应任务的日志，如果没有就返回空数组
    let filteredLogs = [...(mockTaskLogs[taskId] || [])]
    
    // 应用过滤条件
    if (level) {
      filteredLogs = filteredLogs.filter(log => log.level === level)
    }
    if (startTime) {
      filteredLogs = filteredLogs.filter(log => 
        new Date(log.timestamp) >= new Date(startTime)
      )
    }
    if (endTime) {
      filteredLogs = filteredLogs.filter(log => 
        new Date(log.timestamp) <= new Date(endTime)
      )
    }
    if (keyword) {
      filteredLogs = filteredLogs.filter(log => 
        log.message.toLowerCase().includes(keyword.toLowerCase())
      )
    }
    
    // 分页
    const startIndex = (page - 1) * size
    const endIndex = startIndex + size
    const paginatedLogs = filteredLogs.slice(startIndex, endIndex)
    
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        taskId,
        total: filteredLogs.length,
        page,
        size,
        logs: paginatedLogs
      }
    })
  }),

  // 3.14 聚合引擎状态查询接口 (v1.4 新增)
  http.get('http://localhost:5173/api/federated/engine/status', async ({ request }) => {
    const url = new URL(request.url)
    const taskId = url.searchParams.get('taskId')
    const engineId = url.searchParams.get('engineId')
    
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: mockAggregationEngineStatus
    })
  }),

  // 3.15 可用聚合策略查询接口 (v1.4 新增)
  http.get('/api/federated/strategies/available', async ({ request }) => {
    const url = new URL(request.url)
    const modelType = url.searchParams.get('modelType')
    const category = url.searchParams.get('category')
    
    let filteredStrategies = [...mockAggregationStrategies.strategies]
    
    if (modelType) {
      filteredStrategies = filteredStrategies.filter(strategy => 
        strategy.supportedModelTypes.includes(modelType)
      )
    }
    if (category) {
      filteredStrategies = filteredStrategies.filter(strategy => 
        strategy.category === category
      )
    }
    
    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        total: filteredStrategies.length,
        strategies: filteredStrategies,
        categories: mockAggregationStrategies.categories
      }
    })
  }),

  // 3.16 任务删除接口
  http.delete('/api/federated/tasks/:taskId', async ({ params, request }) => {
    const taskId = params.taskId as string
    const body = await request.json() as any
    
    return HttpResponse.json({
      code: 200,
      message: '任务删除成功',
      data: {
        taskId,
        deletedAt: new Date().toISOString(),
        dataDeleted: body.deleteData !== false,
        modelPreserved: body.deleteModel === false
      }
    })
  })
]
