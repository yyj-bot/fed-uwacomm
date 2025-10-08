/**
 * 模型管理 API Mock Handlers
 * 严格按照 initial-model-api-reference.md 和 model-version-api-reference.md 规范实现
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { http, HttpResponse } from 'msw'
import { 
  mockInitialModels,
  mockDistributionProgress,
  mockModelVersions,
  mockEvaluationResults,
  mockRollbackRecords,
  mockModelStatistics,
  mockTaskModelStatistics,
  createMockModelId,
  createMockDistributionId,
  createMockEvaluationId,
  createMockRollbackId,
  getModelsByTaskId,
  getDataByModelId,
  paginate,
  InitialModelStatus,
  DistributionStatus,
  ModelVersionStatus,
  VmDistributionStatus,
  VerificationStatus,
  ErrorCodes,
  vmIds
} from '../data/modelManagementMockData'

// ==================== 初始模型管理 API Mock Handlers ====================

export const modelManagementHandlers = [
  
  // ==================== 初始模型管理接口 ====================
  
  // 2.1 随机生成初始模型
  http.post('/api/model/initial/generate', async ({ request }) => {
    const body = await request.json() as any
    
    // 验证必填参数
    if (!body.taskId || !body.modelType) {
      return HttpResponse.json({
        code: 400,
        message: '缺少必填参数',
        data: null
      }, { status: 400 })
    }

    // 模拟生成过程
    await new Promise(resolve => setTimeout(resolve, 100))

    const modelId = `initial_model_${Date.now()}`
    const modelType = body.modelType || 'RANDOM_FOREST' // 默认使用随机森林
    
    // 根据模型类型生成对应的架构参数
    let architecture
    if (modelType === 'RANDOM_FOREST') {
      architecture = body.architecture || {
        n_estimators: 100,
        n_features: 5,
        task_type: 'regression'
      }
    } else {
      architecture = body.architecture || {
        inputSize: 128,
        hiddenLayers: [64, 32, 16],
        outputSize: 10,
        activationFunction: 'relu',
        optimizer: 'adam',
        learningRate: 0.001
      }
    }
    
    const newModel = {
      modelId,
      taskId: body.taskId,
      modelType,
      modelSize: 1048576 + Math.floor(Math.random() * 9437184), // 1-10MB
      parametersCount: modelType === 'RANDOM_FOREST' ? 
        (architecture.n_estimators || 100) : 
        10000 + Math.floor(Math.random() * 90000),
      architecture,
      generatedAt: new Date().toISOString(),
      status: InitialModelStatus.READY,
      checksum: `sha256:${Math.random().toString(36).substring(2, 66)}`,
      description: body.description || `${modelType}随机生成的初始模型`
    }

    return HttpResponse.json({
      code: 200,
      message: '初始模型生成成功',
      data: newModel
    })
  }),

  // 2.2 上传自定义初始模型
  http.post('/api/model/initial/upload', async ({ request }) => {
    // 模拟文件上传处理
    await new Promise(resolve => setTimeout(resolve, 200))

    const modelId = `initial_model_${Date.now()}`
    
    return HttpResponse.json({
      code: 200,
      message: '初始模型上传成功',
      data: {
        modelId,
        taskId: 'uploaded_task_id',
        modelType: 'RANDOM_FOREST', // 更新为随机森林
        fileName: 'initial_model.pkl', // 更新文件扩展名
        modelSize: 2048576,
        uploadedAt: new Date().toISOString(),
        status: InitialModelStatus.UPLOADED,
        checksum: `sha256:${Math.random().toString(36).substring(2, 66)}`,
        metadata: {
          architecture: {
            n_estimators: 100,
            n_features: 5,
            task_type: 'regression'
          },
          framework: 'sklearn', // 更新为sklearn
          version: '1.0'
        }
      }
    })
  }),

  // 2.3 获取任务初始模型
  http.get('/api/model/initial/:taskId', async ({ params, request }) => {
    const { taskId } = params
    const url = new URL(request.url)
    const includeParameters = url.searchParams.get('includeParameters') === 'true'
    const format = url.searchParams.get('format') || 'json'

    const model = mockInitialModels.find(m => m.taskId === taskId)
    
    if (!model) {
      return HttpResponse.json({
        code: 404,
        message: '初始模型不存在',
        data: null
      }, { status: 404 })
    }

    const responseData = {
      ...model,
      distributionStatus: {
        totalVms: 5,
        distributedVms: 5,
        failedVms: 0,
        distributedAt: new Date().toISOString()
      }
    }

    if (format === 'binary') {
      // 模拟二进制文件下载
      return new Response(new ArrayBuffer(8), {
        headers: {
          'Content-Type': 'application/octet-stream',
          'Content-Disposition': `attachment; filename="initial_model_${taskId}.pth"`
        }
      })
    }

    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: responseData
    })
  }),

  // 2.4 分发初始模型
  http.post('/api/model/initial/:taskId/distribute', async ({ params, request }) => {
    const { taskId } = params
    const body = await request.json() as any

    // 验证模型存在
    const model = mockInitialModels.find(m => m.taskId === taskId)
    if (!model) {
      return HttpResponse.json({
        code: 404,
        message: '初始模型不存在',
        data: null
      }, { status: 404 })
    }

    // 验证请求参数
    if (!body.vmIds || !Array.isArray(body.vmIds) || body.vmIds.length === 0) {
      return HttpResponse.json({
        code: 400,
        message: '请提供有效的虚拟机ID列表',
        data: null
      }, { status: 400 })
    }

    // 验证VM ID格式和有效性
    const validVmIds = vmIds // 从mock数据中获取有效的VM ID列表
    const invalidVmIds = body.vmIds.filter((vmId: string) => !validVmIds.includes(vmId))
    
    if (invalidVmIds.length > 0) {
      return HttpResponse.json({
        code: 400,
        message: `无效的虚拟机ID: ${invalidVmIds.join(', ')}。有效的VM ID: ${validVmIds.join(', ')}`,
        data: null
      }, { status: 400 })
    }

    const distributionId = createMockDistributionId()
    const vmIdList = body.vmIds

    // 创建新的分发记录并添加到mock数据中
    const newDistribution = {
      distributionId,
      taskId: taskId as string,
      modelId: model.modelId,
      targetVms: vmIdList,
      distributionMode: body.distributionMode || 'ASYNC',
      status: DistributionStatus.IN_PROGRESS,
      startedAt: new Date().toISOString(),
      estimatedCompletion: new Date(Date.now() + 5 * 60000).toISOString(), // 5分钟后
      timeout: body.timeout || 300,
      retryAttempts: 0,
      verifyChecksum: body.verifyChecksum !== false,
      notifyOnCompletion: body.notifyOnCompletion !== false,
      progress: {
        total: vmIdList.length,
        completed: 0,
        failed: 0,
        inProgress: vmIdList.length
      },
      vmDetails: vmIdList.map((vmId: string) => ({
        vmId,
        status: VmDistributionStatus.IN_PROGRESS,
        verificationStatus: VerificationStatus.PENDING
      }))
    }

    // 将新分发记录添加到mock数据中
    mockDistributionProgress.push(newDistribution)

    return HttpResponse.json({
      code: 200,
      message: '模型分发已启动',
      data: {
        distributionId,
        taskId,
        modelId: model.modelId,
        targetVms: vmIdList,
        distributionMode: body.distributionMode || 'ASYNC',
        status: DistributionStatus.IN_PROGRESS,
        startedAt: new Date().toISOString(),
        estimatedCompletion: new Date(Date.now() + 5 * 60000).toISOString(), // 5分钟后
        progress: {
          total: vmIdList.length,
          completed: 0,
          failed: 0,
          inProgress: vmIdList.length
        }
      }
    })
  }),

  // 2.5 查询分发状态
  http.get('/api/model/initial/distribution/:distributionId', async ({ params }) => {
    const { distributionId } = params
    
    const distribution = mockDistributionProgress.find(d => d.distributionId === distributionId)
    
    if (!distribution) {
      return HttpResponse.json({
        code: 404,
        message: '分发任务不存在',
        data: null
      }, { status: 404 })
    }

    // 模拟分发进度更新
    if (distribution.status === DistributionStatus.IN_PROGRESS) {
      const now = new Date()
      const startTime = new Date(distribution.startedAt)
      const elapsedMinutes = (now.getTime() - startTime.getTime()) / (1000 * 60)
      
      // 根据时间模拟进度变化
      if (elapsedMinutes > 0.5) { // 30秒后开始有进度
        const progressRatio = Math.min(elapsedMinutes / 3, 0.95) // 3分钟内完成95%
        const completedCount = Math.floor(distribution.progress.total * progressRatio)
        
        distribution.progress.completed = completedCount
        distribution.progress.inProgress = distribution.progress.total - completedCount
        distribution.progress.failed = 0
        
        // 更新VM详情
        distribution.vmDetails.forEach((vm, index) => {
          if (index < completedCount) {
            vm.status = VmDistributionStatus.SUCCESS
            vm.verificationStatus = VerificationStatus.VERIFIED
            vm.distributedAt = new Date(startTime.getTime() + (index + 1) * 30000).toISOString()
            vm.checksum = `sha256:${Array.from({ length: 64 }, () => Math.floor(Math.random() * 16).toString(16)).join('')}`
          }
        })
        
        // 如果接近完成，标记为已完成
        if (progressRatio >= 0.95 && elapsedMinutes > 2) {
          distribution.status = DistributionStatus.COMPLETED
          distribution.completedAt = now.toISOString()
          distribution.progress.completed = distribution.progress.total
          distribution.progress.inProgress = 0
          
          // 所有VM都标记为完成
          distribution.vmDetails.forEach((vm, index) => {
            vm.status = VmDistributionStatus.SUCCESS
            vm.verificationStatus = VerificationStatus.VERIFIED
            vm.distributedAt = new Date(startTime.getTime() + (index + 1) * 30000).toISOString()
            vm.checksum = `sha256:${Array.from({ length: 64 }, () => Math.floor(Math.random() * 16).toString(16)).join('')}`
          })
        }
      }
    }

    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: distribution
    })
  }),

  // 2.6 下载初始模型
  http.get('/api/model/initial/:taskId/download', async ({ params, request }) => {
    const { taskId } = params
    const url = new URL(request.url)
    const format = url.searchParams.get('format') || 'binary'

    // 查找该任务的模型（一个任务对应一个初始模型）
    const model = mockInitialModels.find(m => m.taskId === taskId)
    
    if (!model) {
      return HttpResponse.json({
        code: 404,
        message: '初始模型不存在',
        data: null
      }, { status: 404 })
    }

    if (format === 'json') {
      return HttpResponse.json({
        code: 200,
        message: '下载成功',
        data: model
      })
    }

    // 模拟二进制文件下载
    return new Response(new ArrayBuffer(model.modelSize || 1048576), {
      status: 200,
      headers: {
        'Content-Type': 'application/octet-stream',
        'Content-Disposition': `attachment; filename="${model.fileName || `initial_model_${taskId}.pth`}"`
      }
    })
  }),

  // 2.7 删除初始模型
  http.delete('/api/model/initial/:taskId', async ({ params, request }) => {
    const { taskId } = params
    
    // 根据接口文档，force参数在请求体中，不是查询参数
    let body: { force?: boolean } = {}
    try {
      const text = await request.text()
      if (text) {
        body = JSON.parse(text)
      }
    } catch (error) {
      // 如果解析失败，使用默认值
    }
    
    const force = body.force || false

    const model = mockInitialModels.find(m => m.taskId === taskId)
    
    if (!model) {
      return HttpResponse.json({
        code: 404,
        message: '初始模型不存在',
        data: null
      }, { status: 404 })
    }

    // 根据接口文档，如果模型已分发且没有force=true，应该返回错误
    if (['DISTRIBUTING', 'DISTRIBUTED'].includes(model.status) && !force) {
      return HttpResponse.json({
        code: 409,
        message: '模型已分发，需要强制删除',
        data: null
      }, { status: 409 })
    }

    return HttpResponse.json({
      code: 200,
      message: '初始模型删除成功',
      data: {
        taskId,
        modelId: model.modelId,
        deletedAt: new Date().toISOString(),
        cleanupStatus: {
          modelFileDeleted: true,
          distributionRecordsCleared: true,
          vmCachesCleared: 3
        }
      }
    })
  }),

  // ==================== 模型版本管理接口 ====================

  // 4.1 模型版本列表查询
  http.get('/api/model/versions', async ({ request }) => {
    const url = new URL(request.url)
    const taskId = url.searchParams.get('taskId')
    const roundNumber = url.searchParams.get('roundNumber')
    const status = url.searchParams.get('status')
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '10')
    const sort = url.searchParams.get('sort') || 'createdAt'
    const order = url.searchParams.get('order') || 'desc'

    let filteredVersions = [...mockModelVersions]

    // 应用过滤条件
    if (taskId) {
      filteredVersions = filteredVersions.filter(v => v.taskId === taskId)
    }
    if (roundNumber) {
      filteredVersions = filteredVersions.filter(v => v.roundNumber === parseInt(roundNumber))
    }
    if (status) {
      filteredVersions = filteredVersions.filter(v => v.status === status)
    }

    // 应用排序
    filteredVersions.sort((a, b) => {
      const aVal = (a as any)[sort]
      const bVal = (b as any)[sort]
      if (order === 'desc') {
        return bVal > aVal ? 1 : -1
      }
      return aVal > bVal ? 1 : -1
    })

    const paginatedData = paginate(filteredVersions, page, size)

    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: paginatedData
    })
  }),

  // 4.2 模型版本详情查询
  http.get('/api/model/versions/:modelId', async ({ params }) => {
    const { modelId } = params
    
    const version = mockModelVersions.find(v => v.modelId === modelId)
    
    if (!version) {
      return HttpResponse.json({
        code: 404,
        message: '模型不存在',
        data: null
      }, { status: 404 })
    }

    // 确保返回符合新数据结构的详情信息
    const detailData = {
      ...version,
      // 确保包含详情接口特有的字段
      aggregatedAt: version.aggregatedAt || new Date().toISOString()
    }

    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: detailData
    })
  }),

  // 4.3 任务模型版本查询
  http.get('/api/model/versions/task/:taskId', async ({ params, request }) => {
    const { taskId } = params
    const url = new URL(request.url)
    const roundNumber = url.searchParams.get('roundNumber')
    const status = url.searchParams.get('status')
    const sort = url.searchParams.get('sort') || 'roundNumber'
    const order = url.searchParams.get('order') || 'asc'

    const taskData = getModelsByTaskId(taskId as string)
    let versions = [...taskData.modelVersions]

    // 应用过滤条件
    if (roundNumber) {
      versions = versions.filter(v => v.roundNumber === parseInt(roundNumber))
    }
    if (status) {
      versions = versions.filter(v => v.status === status)
    }

    // 应用排序
    versions.sort((a, b) => {
      const aVal = (a as any)[sort]
      const bVal = (b as any)[sort]
      if (order === 'desc') {
        return bVal > aVal ? 1 : -1
      }
      return aVal > bVal ? 1 : -1
    })

    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: {
        taskId,
        taskName: '水声分类任务',
        totalModels: versions.length,
        versions
      }
    })
  }),

  // ==================== 模型性能评估接口 ====================

  // 5.1 模型性能评估
  http.post('/api/model/evaluate', async ({ request }) => {
    const body = await request.json() as any
    
    if (!body.modelId || !body.testDataPath) {
      return HttpResponse.json({
        code: 400,
        message: '缺少必填参数',
        data: null
      }, { status: 400 })
    }

    // 模拟评估过程
    await new Promise(resolve => setTimeout(resolve, 1000))

    const evaluationId = createMockEvaluationId()
    
    return HttpResponse.json({
      code: 200,
      message: '评估完成',
      data: {
        modelId: body.modelId,
        evaluationId,
        // ⭐ 核心评估指标作为顶级字段
        accuracy: 0.8500,
        loss: 0.123456,
        // ⭐ 其他评估指标在metrics对象内
        metrics: {
          precision: 0.8200,
          recall: 0.8300,
          f1: 0.8250
        },
        evaluationTime: 15.5,
        testSamples: 1000,
        status: 'COMPLETED',
        createdAt: new Date().toISOString()
      }
    })
  }),

  // 5.2 批量模型评估
  http.post('/api/model/evaluate/batch', async ({ request }) => {
    const body = await request.json() as any
    
    if (!body.taskId || !body.testDataPath) {
      return HttpResponse.json({
        code: 400,
        message: '缺少必填参数',
        data: null
      }, { status: 400 })
    }

    // 模拟批量评估过程
    await new Promise(resolve => setTimeout(resolve, 1500))

    const roundNumbers = body.roundNumbers || [1, 5, 10]
    
    return HttpResponse.json({
      code: 200,
      message: '批量评估完成',
      data: {
        taskId: body.taskId,
        evaluatedCount: roundNumbers.length,
        results: roundNumbers.map((round: number) => ({
          modelId: createMockModelId(),
          roundNumber: round,
          accuracy: 0.8500 + round * 0.001,
          loss: 0.123456 - round * 0.001,
          status: 'COMPLETED'
        }))
      }
    })
  }),

  // 5.3 评估结果查询
  http.get('/api/model/evaluate/results', async ({ request }) => {
    const url = new URL(request.url)
    const modelId = url.searchParams.get('modelId')
    const taskId = url.searchParams.get('taskId')
    const evaluationId = url.searchParams.get('evaluationId')
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '10')

    let filteredResults = [...mockEvaluationResults]

    // 应用过滤条件
    if (modelId) {
      filteredResults = filteredResults.filter(r => r.modelId === modelId)
    }
    if (taskId) {
      filteredResults = filteredResults.filter(r => r.taskId === taskId)
    }
    if (evaluationId) {
      filteredResults = filteredResults.filter(r => r.evaluationId === evaluationId)
    }

    const paginatedData = paginate(filteredResults, page, size)

    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: paginatedData
    })
  }),



  // ==================== 模型下载接口 ====================

  // 8.1 模型文件下载
  http.get('/api/model/download/:modelId', async ({ params, request }) => {
    const { modelId } = params
    const url = new URL(request.url)
    const format = url.searchParams.get('format') || 'original'
    const compressed = url.searchParams.get('compressed') === 'true'

    const model = mockModelVersions.find(v => v.modelId === modelId)
    
    if (!model) {
      return HttpResponse.json({
        code: 404,
        message: '模型不存在',
        data: null
      }, { status: 404 })
    }

    // 模拟文件下载
    const fileSize = model.fileSize || 1048576
    return new Response(new ArrayBuffer(fileSize), {
      headers: {
        'Content-Type': 'application/octet-stream',
        'Content-Disposition': `attachment; filename="model_${modelId}.${format === 'onnx' ? 'onnx' : 'pth'}"`
      }
    })
  }),

  // 8.2 批量模型下载
  http.post('/api/model/download/batch', async ({ request }) => {
    const body = await request.json() as any
    
    if (!body.modelIds || body.modelIds.length === 0) {
      return HttpResponse.json({
        code: 400,
        message: '缺少模型ID列表',
        data: null
      }, { status: 400 })
    }

    // 模拟批量下载（返回ZIP文件）
    return new Response(new ArrayBuffer(1048576 * body.modelIds.length), {
      headers: {
        'Content-Type': 'application/zip',
        'Content-Disposition': 'attachment; filename="models_batch.zip"'
      }
    })
  }),

  // ==================== 模型删除接口 ====================

  // 9.2 批量模型删除 (必须在 :modelId 之前，避免路由冲突)
  http.delete('/api/model/versions/batch', async ({ request }) => {
    let body: any
    try {
      const text = await request.text()
      body = text ? JSON.parse(text) : {}
    } catch (error) {
      return HttpResponse.json({
        code: 400,
        message: '请求体解析失败',
        data: null
      }, { status: 400 })
    }
    
    if (!body.modelIds || body.modelIds.length === 0) {
      return HttpResponse.json({
        code: 400,
        message: '缺少模型ID列表',
        data: null
      }, { status: 400 })
    }

    return HttpResponse.json({
      code: 200,
      message: '批量删除成功',
      data: {
        successCount: body.modelIds.length,
        failedCount: 0,
        results: body.modelIds.map((modelId: string) => ({
          modelId,
          status: 'DELETED',
          message: '删除成功'
        }))
      }
    })
  }),

  // 9.1 模型版本删除 (放在batch之后，避免路由冲突)
  http.delete('/api/model/versions/:modelId', async ({ params, request }) => {
    const { modelId } = params
    let body: any
    try {
      const text = await request.text()
      body = text ? JSON.parse(text) : {}
    } catch (error) {
      body = {}
    }

    const model = mockModelVersions.find(v => v.modelId === modelId)
    
    if (!model) {
      return HttpResponse.json({
        code: 404,
        message: '模型不存在',
        data: null
      }, { status: 404 })
    }

    return HttpResponse.json({
      code: 200,
      message: '删除成功',
      data: {
        modelId,
        deletedAt: new Date().toISOString()
      }
    })
  }),

  // ==================== 模型回滚接口 ====================

  // 7.1 模型回滚
  http.post('/api/model/rollback', async ({ request }) => {
    const body = await request.json() as {
      deploymentId: string
      targetModelId: string
      rollbackReason?: string
      force?: boolean
    }

    // 模拟回滚处理
    const rollbackId = createMockRollbackId()
    
    return HttpResponse.json({
      code: 200,
      message: '回滚请求已提交',
      data: {
        rollbackId,
        deploymentId: body.deploymentId,
        fromModelId: `model_${String(Math.floor(Math.random() * 50) + 1).padStart(3, '0')}`,
        toModelId: body.targetModelId,
        status: 'PENDING',
        rollbackReason: body.rollbackReason,
        rollbackTime: Math.floor(Math.random() * 300000) + 10000,
        createdAt: new Date().toISOString()
      }
    })
  }),

  // 7.2 回滚历史查询
  http.get('/api/model/rollback/history', async ({ request }) => {
    const url = new URL(request.url)
    const deploymentId = url.searchParams.get('deploymentId')
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '10')

    let filteredRecords = mockRollbackRecords
    if (deploymentId) {
      filteredRecords = mockRollbackRecords.filter(record => 
        record.deploymentId === deploymentId
      )
    }

    const paginatedData = paginate(filteredRecords, page, size)

    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: paginatedData
    })
  }),

  // ==================== 模型统计接口 ====================

  // 10.1 模型统计信息
  http.get('/api/model/statistics', async ({ request }) => {
    const url = new URL(request.url)
    const taskId = url.searchParams.get('taskId')
    const timeRange = url.searchParams.get('timeRange') || '7d'

    // 根据timeRange确定天数和时间范围
    let days = 7
    if (timeRange === '30d') days = 30
    else if (timeRange === '90d') days = 90
    
    // 计算时间范围的起始时间
    const startTime = new Date(Date.now() - days * 24 * 60 * 60 * 1000)
    
    // 筛选模型：1. 按任务ID筛选（可选） 2. 按时间范围筛选
    let filteredModels = mockModelVersions.filter(m => {
      // 如果指定了taskId，必须匹配
      if (taskId && m.taskId !== taskId) return false
      
      // 按时间范围筛选：只统计timeRange天数内创建的模型
      const modelCreateTime = new Date(m.createdAt)
      return modelCreateTime >= startTime
    })
    
    // 计算统计数据
    const totalModels = filteredModels.length
    const avgAccuracy = filteredModels.reduce((sum, m) => sum + (m.accuracy || 0), 0) / (totalModels || 1)
    const avgLoss = filteredModels.reduce((sum, m) => sum + (m.loss || 0), 0) / (totalModels || 1)
    
    // 生成上传趋势数据（按天统计）
    const uploadTrendMap = new Map<string, number>()
    filteredModels.forEach(m => {
      const dateStr = m.createdAt.split('T')[0]
      uploadTrendMap.set(dateStr, (uploadTrendMap.get(dateStr) || 0) + 1)
    })
    
    // 生成准确率趋势（按轮次统计）
    const accuracyByRound = filteredModels
      .filter(m => m.accuracy !== undefined)
      .sort((a, b) => a.roundNumber - b.roundNumber)
      .slice(0, 20) // 最多显示20个轮次
    
    // 生成动态统计数据
    const statistics = {
      totalModels,
      averageAccuracy: avgAccuracy || 0,
      averageLoss: avgLoss || 0,
      uploadTrend: Array.from({ length: days }, (_, i) => {
        const date = new Date(Date.now() - (days - i - 1) * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
        return {
          date,
          count: uploadTrendMap.get(date) || 0
        }
      }),
      accuracyTrend: accuracyByRound.length > 0 
        ? accuracyByRound.map(m => ({
            roundNumber: m.roundNumber,
            accuracy: m.accuracy || 0
          }))
        : [] // 没有数据时返回空数组，不要生成假数据
    }

    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: statistics
    })
  }),

  // 10.2 任务模型统计
  http.get('/api/model/statistics/task/:taskId', async ({ params }) => {
    const { taskId } = params
    
    const taskStats = mockTaskModelStatistics.find(s => s.taskId === taskId)
    
    if (!taskStats) {
      return HttpResponse.json({
        code: 404,
        message: '任务不存在',
        data: null
      }, { status: 404 })
    }

    return HttpResponse.json({
      code: 200,
      message: '查询成功',
      data: taskStats
    })
  })
]
