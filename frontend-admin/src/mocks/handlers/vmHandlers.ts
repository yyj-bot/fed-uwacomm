/**
 * 水下机器人管理 API 处理器
 * 基于 vm-api-reference.md 和 vm-round-models-api-reference.md 文档
 */

import { http, HttpResponse } from 'msw'
import { vmApiMock, vmRoundModelsApiMock, multiVmComparisonMock } from '../data/vmApiMockData'
import { vmStatusMap, deletedVmIds } from './vmState'

export const vmHandlers = [
  // ==================== VM自身操作接口 (/api/v1/vm) ====================
  
  // 3.1 水下机器人注册接口 - POST /api/v1/vm/register
  http.post('http://localhost:5173/api/v1/vm/register', async ({ request }) => {
    const body = await request.json() as any
    
    // 简单的参数验证
    if (!body.name || !body.ipAddress || !body.cpuCores) {
      return HttpResponse.json(vmApiMock.register.error400, { status: 400 })
    }
    
    // 模拟水下机器人已存在的情况（如果名称是特定值）
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
  
  // 4.1 水下机器人列表查询接口 - GET /api/vm/list
  http.get('http://localhost:5173/api/vm/list', ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '20')
    const statusFilter = url.searchParams.get('status')
    const osType = url.searchParams.get('osType')
    const keyword = url.searchParams.get('keyword')
    
    // 更新水下机器人列表的状态，并过滤掉已删除的VM
    let filteredList = vmApiMock.list.success.data.list
      .filter(vm => !deletedVmIds.has(vm.vmId)) // 过滤已删除的VM
      .map(vm => {
        const currentStatus = vmStatusMap.get(vm.vmId) || vm.status
        const connectionStatus = (currentStatus === 'RUNNING' || currentStatus === 'STARTING') 
          ? 'CONNECTED' 
          : 'DISCONNECTED'
        
        return {
          ...vm,
          status: currentStatus,
          connectionStatus
        }
      })
    
    // 状态过滤
    if (statusFilter) {
      filteredList = filteredList.filter(vm => vm.status === statusFilter)
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

  // 4.2 水下机器人详情查询接口 - GET /api/vm/{vmId}
  http.get('http://localhost:5173/api/vm/:vmId', ({ params }) => {
    const { vmId } = params as { vmId: string }
    
    // 模拟水下机器人不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "水下机器人不存在",
        data: null
      }, { status: 404 })
    }
    
    // 获取当前状态
    const currentStatus = vmStatusMap.get(vmId) || vmApiMock.detail.success.data.status
    const connectionStatus = (currentStatus === 'RUNNING' || currentStatus === 'STARTING') 
      ? 'CONNECTED' 
      : 'DISCONNECTED'
    
    return HttpResponse.json({
      code: 200,
      message: "查询成功",
      data: {
        ...vmApiMock.detail.success.data,
        vmId,
        status: currentStatus,
        connectionStatus
      }
    })
  }),

  // 4.3 水下机器人更新接口 - PUT /api/vm/{vmId}
  http.put('http://localhost:5173/api/vm/:vmId', async ({ params, request }) => {
    const { vmId } = params
    const body = await request.json() as any
    
    // 模拟水下机器人不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "水下机器人不存在",
        data: null
      }, { status: 404 })
    }
    
    return HttpResponse.json(vmApiMock.update.success)
  }),

  // 4.4 水下机器人删除接口 - DELETE /api/vm/{vmId}
  http.delete('http://localhost:5173/api/vm/:vmId', ({ params, request }) => {
    const { vmId } = params as { vmId: string }
    const url = new URL(request.url)
    const force = url.searchParams.get('force') === 'true'
    
    console.log('[Mock] 删除水下机器人请求:', { vmId, force })
    
    // 模拟水下机器人不存在
    if (vmId === 'nonexistent' || deletedVmIds.has(vmId)) {
      return HttpResponse.json({
        code: 404,
        message: "水下机器人不存在",
        data: null
      }, { status: 404 })
    }
    
    // 检查是否正在运行（非强制删除时）
    const currentStatus = vmStatusMap.get(vmId) || 'STOPPED'
    if (!force && currentStatus === 'RUNNING') {
      return HttpResponse.json({
        code: 400,
        message: "水下机器人正在运行，请先停止水下机器人或使用强制删除",
        data: null
      }, { status: 400 })
    }
    
    // 模拟正在运行的水下机器人无法删除（除非强制删除）
    if (vmId === 'running-vm' && !force) {
      return HttpResponse.json({
        code: 400,
        message: "水下机器人正在运行，无法删除",
        data: null
      }, { status: 400 })
    }
    
    // 标记为已删除
    deletedVmIds.add(vmId)
    // 从状态映射中移除
    vmStatusMap.delete(vmId)
    
    console.log('[Mock] 水下机器人已删除:', vmId)
    
    return HttpResponse.json(vmApiMock.delete.success)
  }),

  // ==================== 水下机器人控制接口 ====================
  
  // 5.1 水下机器人启动接口 - POST /api/vm/{vmId}/start
  http.post('http://localhost:5173/api/vm/:vmId/start', async ({ params, request }) => {
    const { vmId } = params as { vmId: string }
    
    // 尝试读取请求体，如果为空则使用空对象
    let body: any = {}
    try {
      const text = await request.text()
      if (text) {
        body = JSON.parse(text)
      }
    } catch (error) {
      console.log('[Mock] 启动水下机器人请求体为空或解析失败，使用默认参数')
    }
    
    console.log('[Mock] 接收到启动水下机器人请求:', { vmId, body })
    
    // 模拟水下机器人不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "水下机器人不存在",
        data: null
      }, { status: 404 })
    }
    
    // 检查当前状态
    const currentStatus = vmStatusMap.get(vmId) || 'STOPPED'
    if (currentStatus === 'RUNNING') {
      return HttpResponse.json({
        code: 400,
        message: "水下机器人已在运行",
        data: null
      }, { status: 400 })
    }
    
    // 更新状态为 STARTING，然后异步更新为 RUNNING
    vmStatusMap.set(vmId, 'STARTING')
    setTimeout(() => {
      vmStatusMap.set(vmId, 'RUNNING')
      console.log(`[Mock] 水下机器人 ${vmId} 状态已更新为 RUNNING`)
    }, 1000)
    
    return HttpResponse.json(vmApiMock.start.success)
  }),

  // 5.2 水下机器人停止接口 - POST /api/vm/{vmId}/stop
  http.post('http://localhost:5173/api/vm/:vmId/stop', async ({ params, request }) => {
    const { vmId } = params as { vmId: string }
    
    console.log('[Mock] ========== 停止水下机器人请求被拦截 ==========')
    console.log('[Mock] vmId:', vmId)
    console.log('[Mock] request.url:', request.url)
    
    // 尝试读取请求体，如果为空则使用空对象
    let body: any = {}
    try {
      const text = await request.text()
      console.log('[Mock] 请求体文本:', text)
      if (text) {
        body = JSON.parse(text)
      }
    } catch (error) {
      console.log('[Mock] 停止水下机器人请求体为空或解析失败，使用默认参数')
    }
    
    console.log('[Mock] 接收到停止水下机器人请求:', { vmId, body })
    
    // 模拟水下机器人不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "水下机器人不存在",
        data: null
      }, { status: 404 })
    }
    
    // 检查当前状态
    const currentStatus = vmStatusMap.get(vmId) || 'RUNNING'
    if (currentStatus === 'STOPPED') {
      return HttpResponse.json({
        code: 400,
        message: "水下机器人已停止",
        data: null
      }, { status: 400 })
    }
    
    // 更新状态为 STOPPING，然后异步更新为 STOPPED
    vmStatusMap.set(vmId, 'STOPPING')
    setTimeout(() => {
      vmStatusMap.set(vmId, 'STOPPED')
      console.log(`[Mock] 水下机器人 ${vmId} 状态已更新为 STOPPED`)
    }, 1000)
    
    console.log('[Mock] 返回停止水下机器人成功响应:', vmApiMock.stop.success)
    return HttpResponse.json(vmApiMock.stop.success)
  }),

  // 5.3 水下机器人重启接口 - POST /api/vm/{vmId}/restart
  http.post('http://localhost:5173/api/vm/:vmId/restart', async ({ params, request }) => {
    const { vmId } = params as { vmId: string }
    
    // 尝试读取请求体，如果为空则使用空对象
    let body: any = {}
    try {
      const text = await request.text()
      if (text) {
        body = JSON.parse(text)
      }
    } catch (error) {
      console.log('[Mock] 重启水下机器人请求体为空或解析失败，使用默认参数')
    }
    
    console.log('[Mock] 接收到重启水下机器人请求:', { vmId, body })
    
    // 模拟水下机器人不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "水下机器人不存在",
        data: null
      }, { status: 404 })
    }
    
    // 更新状态：STOPPING -> STARTING -> RUNNING
    vmStatusMap.set(vmId, 'STOPPING')
    setTimeout(() => {
      vmStatusMap.set(vmId, 'STARTING')
      console.log(`[Mock] 水下机器人 ${vmId} 状态已更新为 STARTING`)
      setTimeout(() => {
        vmStatusMap.set(vmId, 'RUNNING')
        console.log(`[Mock] 水下机器人 ${vmId} 状态已更新为 RUNNING`)
      }, 1000)
    }, 500)
    
    return HttpResponse.json(vmApiMock.restart.success)
  }),

  // ==================== 水下机器人状态接口 ====================
  
  // 6.1 水下机器人状态查询接口 - GET /api/vm/{vmId}/status
  http.get('http://localhost:5173/api/vm/:vmId/status', ({ params }) => {
    const { vmId } = params as { vmId: string }
    
    console.log('[Mock] 查询水下机器人状态:', vmId)
    
    // 模拟水下机器人不存在
    if (vmId === 'nonexistent') {
      return HttpResponse.json({
        code: 404,
        message: "水下机器人不存在",
        data: null
      }, { status: 404 })
    }
    
    // 模拟连接失败
    if (vmId === 'connection-failed') {
      return HttpResponse.json({
        code: 500,
        message: "无法连接到水下机器人",
        data: null
      }, { status: 500 })
    }
    
    // 获取当前状态，默认为 RUNNING
    const currentStatus = vmStatusMap.get(vmId) || 'RUNNING'
    console.log('[Mock] 当前水下机器人状态:', currentStatus)
    
    // 根据状态返回相应的连接状态
    const connectionStatus = (currentStatus === 'RUNNING' || currentStatus === 'STARTING') 
      ? 'CONNECTED' 
      : 'DISCONNECTED'
    
    // 返回状态信息，使用当前状态
    return HttpResponse.json({
      code: 200,
      message: "查询成功",
      data: {
        ...vmApiMock.status.success.data,
        vmId,
        status: currentStatus,
        connectionStatus
      }
    })
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
    
    // 水下机器人ID过滤
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

  // ==================== 扩展接口：多水下机器人对比 ====================
  
  // 多水下机器人同轮次对比 - GET /api/model/vm-round-models/comparison/round
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

  // 多水下机器人趋势对比 - GET /api/model/vm-round-models/comparison/trend
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
