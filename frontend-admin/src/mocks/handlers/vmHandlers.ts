/**
 * 虚拟机管理 API 处理器
 * 基于 vm-api-reference.md 和 vm-round-models-api-reference.md 文档
 */

import { http, HttpResponse } from 'msw'
import { vmApiMock, vmRoundModelsApiMock, multiVmComparisonMock } from '../data/vmApiMockData'

export const vmHandlers = [
  // ==================== VM自身操作接口 (/api/v1/vm) ====================
  
  // 3.1 虚拟机注册接口 - POST /api/v1/vm/register
  http.post('http://localhost:5173/api/v1/vm/register', async ({ request }) => {
    const body = await request.json() as any
    
    // 简单的参数验证
    if (!body.name || !body.ipAddress || !body.cpuCores) {
      return HttpResponse.json(vmApiMock.register.error400, { status: 400 })
    }
    
    // 模拟虚拟机已存在的情况（如果名称是特定值）
    if (body.name === '水声联邦学习节点-已存在') {
      return HttpResponse.json(vmApiMock.register.error409, { status: 409 })
    }
    
    return HttpResponse.json(vmApiMock.register.success)
  }),

  // 3.2 Token刷新接口 - POST /api/v1/vm/token/refresh
  http.post('http://localhost:5173/api/v1/vm/token/refresh', async ({ request }) => {
    const body = await request.json() as any
    
    // 简单的参数验证
    if (!body.vmId || !body.secretId) {
      return HttpResponse.json({
        code: 400,
        message: "参数错误",
        data: null
      }, { status: 400 })
    }
    
    // 模拟无效凭证
    if (body.secretId === 'invalid_secret') {
      return HttpResponse.json({
        code: 401,
        message: "凭证无效",
        data: null
      }, { status: 401 })
    }
    
    return HttpResponse.json(vmApiMock.tokenRefresh.success)
  }),

  // ==================== 用户管理操作接口 (/api/vm) ====================
  
  // 4.1 虚拟机列表查询接口 - GET /api/vm/list
  http.get('http://localhost:5173/api/vm/list', ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '20')
    const status = url.searchParams.get('status')
    const osType = url.searchParams.get('osType')
    const keyword = url.searchParams.get('keyword')
    
    let filteredList = [...vmApiMock.list.success.data.list]
    
    // 状态过滤
    if (status) {
      filteredList = filteredList.filter(vm => vm.status === status)
    }
    
    // 操作系统过滤
    if (osType) {
      filteredList = filteredList.filter(vm => vm.osType.includes(osType))
    }
    
    // 关键词搜索
    if (keyword) {
      filteredList = filteredList.filter(vm => 
        vm.name.includes(keyword) || vm.ipAddress.includes(keyword)
      )
    }
    
    // 分页处理
    const startIndex = (page - 1) * size
    const endIndex = startIndex + size
    const paginatedList = filteredList.slice(startIndex, endIndex)
    
    return HttpResponse.json({
      code: 200,
      message: "查询成功",
      data: {
        total: filteredList.length,
        page,
        size,
        pages: Math.ceil(filteredList.length / size),
        list: paginatedList
      }
    })
  }),

  // 4.2 虚拟机详情查询接口 - GET /api/vm/{vmId}
  http.get('http://localhost:5173/api/vm/:vmId', ({ params }) => {
    const { vmId } = params
    
    // 模拟虚拟机不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "虚拟机不存在",
        data: null
      }, { status: 404 })
    }
    
    return HttpResponse.json(vmApiMock.detail.success)
  }),

  // 4.3 虚拟机更新接口 - PUT /api/vm/{vmId}
  http.put('http://localhost:5173/api/vm/:vmId', async ({ params, request }) => {
    const { vmId } = params
    const body = await request.json() as any
    
    // 模拟虚拟机不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "虚拟机不存在",
        data: null
      }, { status: 404 })
    }
    
    return HttpResponse.json(vmApiMock.update.success)
  }),

  // 4.4 虚拟机删除接口 - DELETE /api/vm/{vmId}
  http.delete('http://localhost:5173/api/vm/:vmId', ({ params, request }) => {
    const { vmId } = params
    const url = new URL(request.url)
    const force = url.searchParams.get('force') === 'true'
    
    // 模拟虚拟机不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "虚拟机不存在",
        data: null
      }, { status: 404 })
    }
    
    // 模拟正在运行的虚拟机无法删除（除非强制删除）
    if (vmId === 'running-vm' && !force) {
      return HttpResponse.json({
        code: 400,
        message: "虚拟机正在运行，无法删除",
        data: null
      }, { status: 400 })
    }
    
    return HttpResponse.json(vmApiMock.delete.success)
  }),

  // ==================== 虚拟机控制接口 ====================
  
  // 5.1 虚拟机启动接口 - POST /api/vm/{vmId}/start
  http.post('http://localhost:5173/api/vm/:vmId/start', async ({ params, request }) => {
    const { vmId } = params
    const body = await request.json() as any
    
    // 模拟虚拟机不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "虚拟机不存在",
        data: null
      }, { status: 404 })
    }
    
    // 模拟虚拟机已在运行
    if (vmId === 'already-running') {
      return HttpResponse.json({
        code: 400,
        message: "虚拟机已在运行",
        data: null
      }, { status: 400 })
    }
    
    return HttpResponse.json(vmApiMock.start.success)
  }),

  // 5.2 虚拟机停止接口 - POST /api/vm/{vmId}/stop
  http.post('http://localhost:5173/api/vm/:vmId/stop', async ({ params, request }) => {
    const { vmId } = params
    const body = await request.json() as any
    
    // 模拟虚拟机不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "虚拟机不存在",
        data: null
      }, { status: 404 })
    }
    
    // 模拟虚拟机已停止
    if (vmId === 'already-stopped') {
      return HttpResponse.json({
        code: 400,
        message: "虚拟机已停止",
        data: null
      }, { status: 400 })
    }
    
    return HttpResponse.json(vmApiMock.stop.success)
  }),

  // 5.3 虚拟机重启接口 - POST /api/vm/{vmId}/restart
  http.post('http://localhost:5173/api/vm/:vmId/restart', async ({ params, request }) => {
    const { vmId } = params
    const body = await request.json() as any
    
    // 模拟虚拟机不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "虚拟机不存在",
        data: null
      }, { status: 404 })
    }
    
    return HttpResponse.json(vmApiMock.restart.success)
  }),

  // ==================== 虚拟机状态接口 ====================
  
  // 6.1 虚拟机状态查询接口 - GET /api/vm/{vmId}/status
  http.get('http://localhost:5173/api/vm/:vmId/status', ({ params }) => {
    const { vmId } = params
    
    // 模拟虚拟机不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "虚拟机不存在",
        data: null
      }, { status: 404 })
    }
    
    // 模拟连接失败
    if (vmId === 'connection-failed') {
      return HttpResponse.json({
        code: 500,
        message: "无法连接到虚拟机",
        data: null
      }, { status: 500 })
    }
    
    return HttpResponse.json(vmApiMock.status.success)
  }),

  // ==================== 本地模型接口 (/api/model/vm-round-models) ====================
  
  // 2.1 本地模型结果分页查询 - GET /api/model/vm-round-models
  http.get('http://localhost:5173/api/model/vm-round-models', ({ request }) => {
    const url = new URL(request.url)
    const taskId = url.searchParams.get('taskId')
    const roundNumber = url.searchParams.get('roundNumber')
    const vmId = url.searchParams.get('vmId')
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '10')
    
    let filteredRecords = [...vmRoundModelsApiMock.list.success.data.records]
    
    // 任务ID过滤
    if (taskId) {
      filteredRecords = filteredRecords.filter(record => record.taskId === taskId)
    }
    
    // 轮次过滤
    if (roundNumber) {
      filteredRecords = filteredRecords.filter(record => record.roundNumber === parseInt(roundNumber))
    }
    
    // 虚拟机ID过滤
    if (vmId) {
      filteredRecords = filteredRecords.filter(record => record.vmId === vmId)
    }
    
    // 分页处理
    const startIndex = (page - 1) * size
    const endIndex = startIndex + size
    const paginatedRecords = filteredRecords.slice(startIndex, endIndex)
    
    return HttpResponse.json({
      code: 200,
      message: "查询成功",
      data: {
        total: filteredRecords.length,
        pages: Math.ceil(filteredRecords.length / size),
        current: page,
        size,
        records: paginatedRecords
      }
    })
  }),

  // 2.2 本地模型结果详情查询 - GET /api/model/vm-round-models/{vmRoundModelId}
  http.get('http://localhost:5173/api/model/vm-round-models/:vmRoundModelId', ({ params }) => {
    const { vmRoundModelId } = params
    
    // 模拟模型不存在
    if (vmRoundModelId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "本地模型不存在",
        data: null
      }, { status: 404 })
    }
    
    return HttpResponse.json(vmRoundModelsApiMock.detail.success)
  }),

  // 2.3 本地模型训练指标趋势 - GET /api/model/vm-round-models/metrics/trend
  http.get('http://localhost:5173/api/model/vm-round-models/metrics/trend', ({ request }) => {
    const url = new URL(request.url)
    const taskId = url.searchParams.get('taskId')
    const vmId = url.searchParams.get('vmId')
    const metric = url.searchParams.get('metric')
    
    // 参数验证
    if (!taskId || !vmId || !metric) {
      return HttpResponse.json({
        code: 400,
        message: "缺少必要参数",
        data: null
      }, { status: 400 })
    }
    
    // 根据指标类型返回不同的趋势数据
    if (metric === 'loss') {
      return HttpResponse.json(vmRoundModelsApiMock.metricsTrend.lossTrend)
    }
    
    return HttpResponse.json(vmRoundModelsApiMock.metricsTrend.success)
  }),

  // 2.4 本地模型最佳/离群查询 - GET /api/model/vm-round-models/metrics/best
  http.get('http://localhost:5173/api/model/vm-round-models/metrics/best', ({ request }) => {
    const url = new URL(request.url)
    const taskId = url.searchParams.get('taskId')
    const metric = url.searchParams.get('metric')
    const type = url.searchParams.get('type')
    
    // 参数验证
    if (!taskId || !metric || !type) {
      return HttpResponse.json({
        code: 400,
        message: "缺少必要参数",
        data: null
      }, { status: 400 })
    }
    
    // 根据指标和类型返回不同的数据
    if (metric === 'accuracy' && type === 'best') {
      return HttpResponse.json(vmRoundModelsApiMock.metricsBest.bestAccuracy)
    }
    
    if (metric === 'loss' && type === 'best') {
      return HttpResponse.json(vmRoundModelsApiMock.metricsBest.bestLoss)
    }
    
    if (metric === 'accuracy' && type === 'outlier') {
      return HttpResponse.json(vmRoundModelsApiMock.metricsBest.outlierAccuracy)
    }
    
    if (metric === 'loss' && type === 'outlier') {
      return HttpResponse.json(vmRoundModelsApiMock.metricsBest.outlierLoss)
    }
    
    // 默认返回最佳准确率
    return HttpResponse.json(vmRoundModelsApiMock.metricsBest.bestAccuracy)
  }),

  // ==================== 扩展接口：多虚拟机对比 ====================
  
  // 多虚拟机同轮次对比 - GET /api/model/vm-round-models/comparison/round
  http.get('http://localhost:5173/api/model/vm-round-models/comparison/round', ({ request }) => {
    const url = new URL(request.url)
    const taskId = url.searchParams.get('taskId')
    const roundNumber = url.searchParams.get('roundNumber')
    
    // 参数验证
    if (!taskId || !roundNumber) {
      return HttpResponse.json({
        code: 400,
        message: "缺少必要参数",
        data: null
      }, { status: 400 })
    }
    
    return HttpResponse.json(multiVmComparisonMock.roundComparison)
  }),

  // 多虚拟机趋势对比 - GET /api/model/vm-round-models/comparison/trend
  http.get('http://localhost:5173/api/model/vm-round-models/comparison/trend', ({ request }) => {
    const url = new URL(request.url)
    const taskId = url.searchParams.get('taskId')
    const metric = url.searchParams.get('metric')
    
    // 参数验证
    if (!taskId || !metric) {
      return HttpResponse.json({
        code: 400,
        message: "缺少必要参数",
        data: null
      }, { status: 400 })
    }
    
    return HttpResponse.json(multiVmComparisonMock.trendComparison)
  })
]
