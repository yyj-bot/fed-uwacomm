/**
 * 系统日志管理 API 处理器
 * 基于 system-log-api-reference.md 文档
 */

import { http, HttpResponse } from 'msw'
import { systemLogApiMock, mockLogs } from '../data/systemLogMockData'

export const systemLogHandlers = [
  // ==================== 日志查询接口 (/api/log) ====================

  // 3.1 日志列表查询 - GET /api/log/list
  http.get('http://localhost:5173/api/log/list', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    // 获取查询参数
    const url = new URL(request.url)
    const level = url.searchParams.get('level')
    const category = url.searchParams.get('category')
    const vmId = url.searchParams.get('vmId')
    const taskId = url.searchParams.get('taskId')
    const startTime = url.searchParams.get('startTime')
    const endTime = url.searchParams.get('endTime')
    const keyword = url.searchParams.get('keyword')
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '10')
    const sort = url.searchParams.get('sort') || 'createdAt'
    const order = url.searchParams.get('order') || 'desc'

    // 过滤日志
    let filteredLogs = [...mockLogs]

    if (level) {
      filteredLogs = filteredLogs.filter(log => log.level === level)
    }

    if (category) {
      filteredLogs = filteredLogs.filter(log => log.category === category)
    }

    if (vmId) {
      filteredLogs = filteredLogs.filter(log => log.vmId === vmId)
    }

    if (taskId) {
      filteredLogs = filteredLogs.filter(log => log.taskId === taskId)
    }

    if (startTime) {
      filteredLogs = filteredLogs.filter(log => new Date(log.createdAt) >= new Date(startTime))
    }

    if (endTime) {
      filteredLogs = filteredLogs.filter(log => new Date(log.createdAt) <= new Date(endTime))
    }

    if (keyword) {
      filteredLogs = filteredLogs.filter(log => log.message.includes(keyword))
    }

    // 排序
    filteredLogs.sort((a, b) => {
      const aValue = a[sort as keyof typeof a]
      const bValue = b[sort as keyof typeof b]
      
      if (order === 'desc') {
        return bValue > aValue ? 1 : -1
      } else {
        return aValue > bValue ? 1 : -1
      }
    })

    // 分页
    const total = filteredLogs.length
    const pages = Math.ceil(total / size)
    const start = (page - 1) * size
    const end = start + size
    const records = filteredLogs.slice(start, end)

    return HttpResponse.json({
      code: 200,
      message: "查询成功",
      data: {
        total,
        pages,
        current: page,
        size,
        records
      }
    })
  }),

  // 3.2 日志详情查询 - GET /api/log/detail/:logId
  http.get('http://localhost:5173/api/log/detail/:logId', ({ request, params }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const { logId } = params

    // 查找日志
    const log = mockLogs.find(l => l.logId === logId)

    if (!log) {
      return HttpResponse.json(systemLogApiMock.detail.error404, { status: 404 })
    }

    return HttpResponse.json({
      code: 200,
      message: "查询成功",
      data: log
    })
  }),

  // 3.3 实时日志查询 - GET /api/log/realtime
  http.get('http://localhost:5173/api/log/realtime', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    // 获取查询参数
    const url = new URL(request.url)
    const level = url.searchParams.get('level')
    const category = url.searchParams.get('category')
    const vmId = url.searchParams.get('vmId')
    const taskId = url.searchParams.get('taskId')
    const tail = parseInt(url.searchParams.get('tail') || '100')

    // 过滤日志
    let filteredLogs = [...mockLogs]

    if (level) {
      filteredLogs = filteredLogs.filter(log => log.level === level)
    }

    if (category) {
      filteredLogs = filteredLogs.filter(log => log.category === category)
    }

    if (vmId) {
      filteredLogs = filteredLogs.filter(log => log.vmId === vmId)
    }

    if (taskId) {
      filteredLogs = filteredLogs.filter(log => log.taskId === taskId)
    }

    // 获取最近N条日志
    const logs = filteredLogs.slice(-tail)

    return HttpResponse.json({
      code: 200,
      message: "查询成功",
      data: {
        logs,
        totalCount: logs.length,
        lastUpdateTime: new Date().toISOString()
      }
    })
  }),

  // 3.4 日志统计查询 - GET /api/log/statistics
  http.get('http://localhost:5173/api/log/statistics', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    return HttpResponse.json(systemLogApiMock.statistics.success)
  }),

  // ==================== 日志导出接口 (/api/log) ====================

  // 4.1 日志下载 - POST /api/log/download
  http.post('http://localhost:5173/api/log/download', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const body = await request.json() as any

    // 参数验证
    const supportedFormats = ['CSV', 'JSON', 'EXCEL']
    if (body.format && !supportedFormats.includes(body.format)) {
      return HttpResponse.json({
        code: 400,
        message: "参数验证失败",
        data: {
          field: "format",
          error: "导出格式必须为 CSV/JSON/EXCEL 之一"
        }
      }, { status: 400 })
    }

    return HttpResponse.json(systemLogApiMock.download.success)
  }),

  // ==================== 日志清理接口 (/api/log/cleanup) ====================

  // 5.1 日志清理 - POST /api/log/cleanup
  http.post('http://localhost:5173/api/log/cleanup', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const body = await request.json() as any

    // 参数验证
    const supportedStrategies = ['TIME_BASED', 'LEVEL_BASED', 'CATEGORY_BASED']
    if (!body.strategy || !supportedStrategies.includes(body.strategy)) {
      return HttpResponse.json({
        code: 400,
        message: "参数验证失败",
        data: {
          field: "strategy",
          error: "清理策略必须为 TIME_BASED/LEVEL_BASED/CATEGORY_BASED 之一"
        }
      }, { status: 400 })
    }

    return HttpResponse.json(systemLogApiMock.cleanup.success)
  }),

  // 5.2 清理状态查询 - GET /api/log/cleanup/status/:cleanupId
  http.get('http://localhost:5173/api/log/cleanup/status/:cleanupId', ({ request, params }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const { cleanupId } = params

    // 检查清理任务是否存在
    if (cleanupId !== 'cleanup_1234567890') {
      return HttpResponse.json(systemLogApiMock.cleanupStatus.error404, { status: 404 })
    }

    return HttpResponse.json(systemLogApiMock.cleanupStatus.success)
  }),

  // 5.3 清理历史查询 - GET /api/log/cleanup/history
  http.get('http://localhost:5173/api/log/cleanup/history', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    // 获取查询参数
    const url = new URL(request.url)
    const status = url.searchParams.get('status')
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '10')

    // 如果有状态过滤，可以在这里处理
    let filteredRecords = systemLogApiMock.cleanupHistory.success.data.records

    if (status) {
      filteredRecords = filteredRecords.filter(record => record.status === status)
    }

    // 返回结果
    return HttpResponse.json({
      code: 200,
      message: "查询成功",
      data: {
        ...systemLogApiMock.cleanupHistory.success.data,
        current: page,
        size,
        records: filteredRecords
      }
    })
  }),

  // ==================== 系统监控接口 (/api/log/monitor) ====================

  // 6.1 系统状态监控 - GET /api/log/monitor/system
  http.get('http://localhost:5173/api/log/monitor/system', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    return HttpResponse.json(systemLogApiMock.monitorSystem.success)
  }),

  // 6.2 日志监控 - GET /api/log/monitor/logs
  http.get('http://localhost:5173/api/log/monitor/logs', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    // 获取查询参数
    const url = new URL(request.url)
    const timeRange = url.searchParams.get('timeRange') || '1h'
    const level = url.searchParams.get('level')

    // 可以根据timeRange和level参数返回不同的数据，这里简化处理
    return HttpResponse.json(systemLogApiMock.monitorLogs.success)
  }),

  // 6.3 性能监控 - GET /api/log/monitor/performance
  http.get('http://localhost:5173/api/log/monitor/performance', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    // 获取查询参数
    const url = new URL(request.url)
    const timeRange = url.searchParams.get('timeRange') || '1h'
    const endpoint = url.searchParams.get('endpoint')

    // 可以根据参数返回不同的数据，这里简化处理
    return HttpResponse.json(systemLogApiMock.monitorPerformance.success)
  }),

  // 6.4 告警配置 - GET /api/log/monitor/alerts
  http.get('http://localhost:5173/api/log/monitor/alerts', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    return HttpResponse.json(systemLogApiMock.monitorAlerts.success)
  }),

  // ==================== 日志配置接口 (/api/log/config) ====================

  // 7.1 日志配置查询 - GET /api/log/config
  http.get('http://localhost:5173/api/log/config', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    return HttpResponse.json(systemLogApiMock.config.success)
  }),

  // 7.2 日志配置更新 - PUT /api/log/config
  http.put('http://localhost:5173/api/log/config', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const body = await request.json() as any

    // 参数验证
    const supportedLevels = ['DEBUG', 'INFO', 'WARN', 'ERROR']
    if (body.logLevel && !supportedLevels.includes(body.logLevel)) {
      return HttpResponse.json(systemLogApiMock.updateConfig.error400, { status: 400 })
    }

    // 验证retentionDays
    if (body.retentionDays && (body.retentionDays < 1 || body.retentionDays > 365)) {
      return HttpResponse.json({
        code: 400,
        message: "参数验证失败",
        data: {
          field: "retentionDays",
          error: "保留天数必须在1-365之间"
        }
      }, { status: 400 })
    }

    return HttpResponse.json(systemLogApiMock.updateConfig.success)
  })
];




