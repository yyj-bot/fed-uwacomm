/**
 * 系统日志服务单元测试
 * 使用 Vitest 测试所有接口方法
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { systemLogService } from '@/services/system-log'
import { systemLogMock } from '@/mocks/systemLogMock'
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
import type { SystemLog, PaginatedResponse } from '@/types'

// Mock API 模块
vi.mock('@/api/system-log', () => ({
  log: {
    getLogList: vi.fn(),
    getLogDetail: vi.fn(),
    getRealtimeLogs: vi.fn(),
    getLogStatistics: vi.fn(),
    exportLogs: vi.fn(),
    getExportStatus: vi.fn(),
    downloadExportFile: vi.fn(),
    getExportHistory: vi.fn(),
    cleanupLogs: vi.fn(),
    getCleanupStatus: vi.fn(),
    getCleanupHistory: vi.fn(),
    getSystemMonitor: vi.fn(),
    getLogMonitor: vi.fn(),
    getPerformanceMonitor: vi.fn(),
    getAlertConfig: vi.fn(),
    getLogConfig: vi.fn(),
    updateLogConfig: vi.fn()
  }
}))

// 获取模拟的 API
const mockApiModule = await import('@/api/system-log')
const mockLog = vi.mocked(mockApiModule.log)

describe('SystemLogService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ==================== 日志查询接口测试 ====================

  describe('日志查询功能', () => {
    describe('getLogList', () => {
      it('应该成功获取日志列表', async () => {
        const params = { page: 1, size: 10, level: 'INFO' }
        const mockResponse = systemLogMock.getLogList(params)
        mockLog.getLogList.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getLogList(params)

        expect(mockLog.getLogList).toHaveBeenCalledWith(params)
        expect(result).toEqual(mockResponse.data)
        expect(result.records).toBeInstanceOf(Array)
        expect(result.current).toBe(params.page)
        expect(result.size).toBe(params.size)
      })

      it('应该验证分页参数', async () => {
        await expect(systemLogService.getLogList({ page: 0 })).rejects.toThrow('页码必须大于0')
        await expect(systemLogService.getLogList({ size: 0 })).rejects.toThrow('每页大小必须在1-100范围内')
        await expect(systemLogService.getLogList({ size: 101 })).rejects.toThrow('每页大小必须在1-100范围内')
      })

      it('应该验证日志级别', async () => {
        await expect(systemLogService.getLogList({ level: 'INVALID' })).rejects.toThrow('日志级别无效')
      })

      it('应该验证日志类别', async () => {
        await expect(systemLogService.getLogList({ category: 'INVALID' })).rejects.toThrow('日志类别无效')
      })

      it('应该验证虚拟机ID格式', async () => {
        await expect(systemLogService.getLogList({ vmId: 'invalid-id' })).rejects.toThrow('虚拟机ID格式不正确')
      })

      it('应该验证任务ID格式', async () => {
        await expect(systemLogService.getLogList({ taskId: 'invalid-id' })).rejects.toThrow('任务ID格式不正确')
      })

      it('应该验证关键词长度', async () => {
        const longKeyword = 'a'.repeat(501)
        await expect(systemLogService.getLogList({ keyword: longKeyword })).rejects.toThrow('关键词长度不能超过500字符')
      })

      it('应该支持多条件过滤', async () => {
        const params = {
          level: 'ERROR',
          category: 'TASK',
          vmId: 'a1b2c3d4e5f678901234567890123456',
          taskId: 'b2c3d4e5f67890123456789012345678',
          keyword: '失败'
        }
        const mockResponse = systemLogMock.getLogList(params)
        mockLog.getLogList.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getLogList(params)
        expect(result.records.length).toBeGreaterThan(0)
      })
    })

    describe('getLogDetail', () => {
      it('应该成功获取日志详情', async () => {
        const logId = 'c3d4e5f6789012345678901234567890'
        const mockResponse = systemLogMock.getLogDetail(logId)
        mockLog.getLogDetail.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getLogDetail(logId)

        expect(mockLog.getLogDetail).toHaveBeenCalledWith(logId)
        expect(result).toEqual(mockResponse.data)
        expect(result.logId).toBe(logId)
      })

      it('应该验证日志ID格式', async () => {
        await expect(systemLogService.getLogDetail('')).rejects.toThrow('日志ID不能为空')
        await expect(systemLogService.getLogDetail('invalid-id')).rejects.toThrow('日志ID格式不正确')
      })

      it('应该处理不存在的日志', async () => {
        const logId = 'nonexistent1234567890123456789012'
        mockLog.getLogDetail.mockRejectedValue(new Error('日志不存在'))

        await expect(systemLogService.getLogDetail(logId))
          .rejects.toThrow(`获取日志详情失败 (ID: ${logId})`)
      })
    })

    describe('getRealtimeLogs', () => {
      it('应该成功获取实时日志', async () => {
        const params = { level: 'ERROR', tail: 50 }
        const mockResponse = systemLogMock.getRealtimeLogs(params)
        mockLog.getRealtimeLogs.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getRealtimeLogs(params)

        expect(mockLog.getRealtimeLogs).toHaveBeenCalledWith(params)
        expect(result).toEqual(mockResponse.data)
        expect(result.logs).toBeInstanceOf(Array)
        expect(result).toHaveProperty('totalCount')
        expect(result).toHaveProperty('lastUpdateTime')
      })

      it('应该验证实时日志参数', async () => {
        await expect(systemLogService.getRealtimeLogs({ level: 'INVALID' })).rejects.toThrow('日志级别无效')
        await expect(systemLogService.getRealtimeLogs({ category: 'INVALID' })).rejects.toThrow('日志类别无效')
        await expect(systemLogService.getRealtimeLogs({ tail: 0 })).rejects.toThrow('tail参数必须在1-1000范围内')
        await expect(systemLogService.getRealtimeLogs({ tail: 1001 })).rejects.toThrow('tail参数必须在1-1000范围内')
      })
    })

    describe('getLogStatistics', () => {
      it('应该成功获取日志统计', async () => {
        const params = { category: 'SYSTEM' }
        const mockResponse = systemLogMock.getLogStatistics(params)
        mockLog.getLogStatistics.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getLogStatistics(params)

        expect(mockLog.getLogStatistics).toHaveBeenCalledWith(params)
        expect(result).toEqual(mockResponse.data)
        expect(result).toHaveProperty('totalLogs')
        expect(result).toHaveProperty('levelDistribution')
        expect(result).toHaveProperty('categoryDistribution')
        expect(result).toHaveProperty('timeDistribution')
        expect(result).toHaveProperty('errorTrend')
      })

      it('应该验证统计参数', async () => {
        await expect(systemLogService.getLogStatistics({ category: 'INVALID' })).rejects.toThrow('日志类别无效')
        await expect(systemLogService.getLogStatistics({ vmId: 'invalid' })).rejects.toThrow('虚拟机ID格式不正确')
      })
    })
  })

  // ==================== 日志导出接口测试 ====================

  describe('日志导出功能', () => {
    describe('exportLogs', () => {
      it('应该成功创建导出任务', async () => {
        const exportData = {
          level: 'ERROR',
          format: 'CSV' as const,
          includeDetails: true
        }
        const mockResponse = systemLogMock.exportLogs(exportData)
        mockLog.exportLogs.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.exportLogs(exportData)

        expect(mockLog.exportLogs).toHaveBeenCalledWith(exportData)
        expect(result).toEqual(mockResponse.data)
        expect(result).toHaveProperty('exportId')
        expect(result).toHaveProperty('status')
        expect(result).toHaveProperty('downloadUrl')
      })

      it('应该验证导出参数', async () => {
        await expect(systemLogService.exportLogs({ level: 'INVALID' })).rejects.toThrow('日志级别无效')
        await expect(systemLogService.exportLogs({ category: 'INVALID' })).rejects.toThrow('日志类别无效')
        await expect(systemLogService.exportLogs({ format: 'INVALID' as any })).rejects.toThrow('导出格式无效')
      })
    })

    describe('getExportStatus', () => {
      it('应该成功获取导出状态', async () => {
        const exportId = 'export_1234567890'
        const mockResponse = systemLogMock.getExportStatus(exportId)
        mockLog.getExportStatus.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getExportStatus(exportId)

        expect(mockLog.getExportStatus).toHaveBeenCalledWith(exportId)
        expect(result).toEqual(mockResponse.data)
        expect(result.exportId).toBe(exportId)
      })

      it('应该验证导出ID', async () => {
        await expect(systemLogService.getExportStatus('')).rejects.toThrow('导出任务ID不能为空')
      })
    })

    describe('downloadExportFile', () => {
      it('应该成功下载导出文件', async () => {
        const exportId = 'export_1234567890'
        const mockBlob = new Blob(['test'], { type: 'text/csv' })
        mockLog.downloadExportFile.mockResolvedValue(mockBlob)

        const result = await systemLogService.downloadExportFile(exportId)

        expect(mockLog.downloadExportFile).toHaveBeenCalledWith(exportId)
        expect(result).toBeInstanceOf(Blob)
      })
    })

    describe('getExportHistory', () => {
      it('应该成功获取导出历史', async () => {
        const params = { status: 'COMPLETED', page: 1, size: 10 }
        const mockResponse = systemLogMock.getExportHistory(params)
        mockLog.getExportHistory.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getExportHistory(params)

        expect(mockLog.getExportHistory).toHaveBeenCalledWith(params)
        expect(result).toEqual(mockResponse.data)
        expect(result.records).toBeInstanceOf(Array)
      })

      it('应该验证历史查询参数', async () => {
        await expect(systemLogService.getExportHistory({ page: 0 })).rejects.toThrow('页码必须大于0')
        await expect(systemLogService.getExportHistory({ size: 101 })).rejects.toThrow('每页大小必须在1-100范围内')
      })
    })
  })

  // ==================== 日志清理接口测试 ====================

  describe('日志清理功能', () => {
    describe('cleanupLogs', () => {
      it('应该成功创建时间策略清理任务', async () => {
        const cleanupData = {
          strategy: 'TIME_BASED' as const,
          retentionDays: 30,
          dryRun: false
        }
        const mockResponse = systemLogMock.cleanupLogs(cleanupData)
        mockLog.cleanupLogs.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.cleanupLogs(cleanupData)

        expect(mockLog.cleanupLogs).toHaveBeenCalledWith(cleanupData)
        expect(result).toEqual(mockResponse.data)
        expect(result).toHaveProperty('cleanupId')
        expect(result).toHaveProperty('status')
      })

      it('应该成功创建级别策略清理任务', async () => {
        const cleanupData = {
          strategy: 'LEVEL_BASED' as const,
          level: 'DEBUG',
          dryRun: true
        }
        const mockResponse = systemLogMock.cleanupLogs(cleanupData)
        mockLog.cleanupLogs.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.cleanupLogs(cleanupData)
        expect(result.dryRun).toBe(true)
      })

      it('应该成功创建大小策略清理任务', async () => {
        const cleanupData = {
          strategy: 'SIZE_BASED' as const,
          maxSizeGB: 10,
          dryRun: false
        }
        const mockResponse = systemLogMock.cleanupLogs(cleanupData)
        mockLog.cleanupLogs.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.cleanupLogs(cleanupData)
        expect(result).toHaveProperty('estimatedSize')
      })

      it('应该验证清理策略', async () => {
        await expect(systemLogService.cleanupLogs({ strategy: 'INVALID' as any }))
          .rejects.toThrow('清理策略无效')
      })

      it('应该验证时间策略参数', async () => {
        await expect(systemLogService.cleanupLogs({
          strategy: 'TIME_BASED',
          retentionDays: 0
        })).rejects.toThrow('保留天数必须在1-365范围内')

        await expect(systemLogService.cleanupLogs({
          strategy: 'TIME_BASED',
          retentionDays: 366
        })).rejects.toThrow('保留天数必须在1-365范围内')
      })

      it('应该验证级别策略参数', async () => {
        await expect(systemLogService.cleanupLogs({
          strategy: 'LEVEL_BASED',
          level: 'INVALID'
        })).rejects.toThrow('日志级别无效')
      })

      it('应该验证大小策略参数', async () => {
        await expect(systemLogService.cleanupLogs({
          strategy: 'SIZE_BASED',
          maxSizeGB: 0.05
        })).rejects.toThrow('最大大小必须在0.1GB-1000GB范围内')

        await expect(systemLogService.cleanupLogs({
          strategy: 'SIZE_BASED',
          maxSizeGB: 1001
        })).rejects.toThrow('最大大小必须在0.1GB-1000GB范围内')
      })
    })

    describe('getCleanupStatus', () => {
      it('应该成功获取清理状态', async () => {
        const cleanupId = 'cleanup_1234567890'
        const mockResponse = systemLogMock.getCleanupStatus(cleanupId)
        mockLog.getCleanupStatus.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getCleanupStatus(cleanupId)

        expect(mockLog.getCleanupStatus).toHaveBeenCalledWith(cleanupId)
        expect(result).toEqual(mockResponse.data)
        expect(result.cleanupId).toBe(cleanupId)
      })

      it('应该验证清理ID', async () => {
        await expect(systemLogService.getCleanupStatus('')).rejects.toThrow('清理任务ID不能为空')
      })
    })

    describe('getCleanupHistory', () => {
      it('应该成功获取清理历史', async () => {
        const params = { status: 'COMPLETED' }
        const mockResponse = systemLogMock.getCleanupHistory(params)
        mockLog.getCleanupHistory.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getCleanupHistory(params)

        expect(mockLog.getCleanupHistory).toHaveBeenCalledWith(params)
        expect(result).toEqual(mockResponse.data)
        expect(result.records).toBeInstanceOf(Array)
      })
    })
  })

  // ==================== 系统监控接口测试 ====================

  describe('系统监控功能', () => {
    describe('getSystemMonitor', () => {
      it('应该成功获取系统监控数据', async () => {
        const mockResponse = systemLogMock.getSystemMonitor()
        mockLog.getSystemMonitor.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getSystemMonitor()

        expect(mockLog.getSystemMonitor).toHaveBeenCalled()
        expect(result).toEqual(mockResponse.data)
        expect(result).toHaveProperty('systemInfo')
        expect(result).toHaveProperty('resourceUsage')
        expect(result).toHaveProperty('applicationMetrics')
        expect(result).toHaveProperty('databaseMetrics')
      })
    })

    describe('getLogMonitor', () => {
      it('应该成功获取日志监控数据', async () => {
        const params = { timeRange: '1h' as const, level: 'ERROR' }
        const mockResponse = systemLogMock.getLogMonitor(params)
        mockLog.getLogMonitor.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getLogMonitor(params)

        expect(mockLog.getLogMonitor).toHaveBeenCalledWith(params)
        expect(result).toEqual(mockResponse.data)
        expect(result).toHaveProperty('logMetrics')
        expect(result).toHaveProperty('levelTrend')
        expect(result).toHaveProperty('categoryTrend')
        expect(result).toHaveProperty('recentErrors')
      })

      it('应该验证日志监控参数', async () => {
        await expect(systemLogService.getLogMonitor({ timeRange: 'invalid' as any }))
          .rejects.toThrow('时间范围无效')
        await expect(systemLogService.getLogMonitor({ level: 'INVALID' }))
          .rejects.toThrow('日志级别无效')
      })
    })

    describe('getPerformanceMonitor', () => {
      it('应该成功获取性能监控数据', async () => {
        const params = { timeRange: '24h' as const, endpoint: '/api/task' }
        const mockResponse = systemLogMock.getPerformanceMonitor(params)
        mockLog.getPerformanceMonitor.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getPerformanceMonitor(params)

        expect(mockLog.getPerformanceMonitor).toHaveBeenCalledWith(params)
        expect(result).toEqual(mockResponse.data)
        expect(result).toHaveProperty('apiMetrics')
        expect(result).toHaveProperty('endpointMetrics')
        expect(result).toHaveProperty('responseTimeTrend')
      })

      it('应该验证性能监控参数', async () => {
        await expect(systemLogService.getPerformanceMonitor({ timeRange: 'invalid' as any }))
          .rejects.toThrow('时间范围无效')
      })
    })

    describe('getAlertConfig', () => {
      it('应该成功获取告警配置', async () => {
        const mockResponse = systemLogMock.getAlertConfig()
        mockLog.getAlertConfig.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getAlertConfig()

        expect(mockLog.getAlertConfig).toHaveBeenCalled()
        expect(result).toEqual(mockResponse.data)
        expect(result).toHaveProperty('alerts')
        expect(result).toHaveProperty('alertHistory')
        expect(result.alerts).toBeInstanceOf(Array)
        expect(result.alertHistory).toBeInstanceOf(Array)
      })
    })
  })

  // ==================== 日志配置接口测试 ====================

  describe('日志配置功能', () => {
    describe('getLogConfig', () => {
      it('应该成功获取日志配置', async () => {
        const mockResponse = systemLogMock.getLogConfig()
        mockLog.getLogConfig.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.getLogConfig()

        expect(mockLog.getLogConfig).toHaveBeenCalled()
        expect(result).toEqual(mockResponse.data)
        expect(result).toHaveProperty('logLevel')
        expect(result).toHaveProperty('retentionDays')
        expect(result).toHaveProperty('maxFileSize')
        expect(result).toHaveProperty('categories')
        expect(result).toHaveProperty('exportSettings')
      })
    })

    describe('updateLogConfig', () => {
      it('应该成功更新日志配置', async () => {
        const configData = {
          logLevel: 'DEBUG',
          retentionDays: 60,
          maxFileSize: 209715200
        }
        const mockResponse = systemLogMock.updateLogConfig(configData)
        mockLog.updateLogConfig.mockResolvedValue(mockResponse.data)

        const result = await systemLogService.updateLogConfig(configData)

        expect(mockLog.updateLogConfig).toHaveBeenCalledWith(configData)
        expect(result).toEqual(mockResponse.data)
        expect(result).toHaveProperty('updatedAt')
      })

      it('应该验证配置更新参数', async () => {
        await expect(systemLogService.updateLogConfig({ logLevel: 'INVALID' }))
          .rejects.toThrow('日志级别无效')
        
        await expect(systemLogService.updateLogConfig({ retentionDays: 0 }))
          .rejects.toThrow('保留天数必须在1-365范围内')
        
        await expect(systemLogService.updateLogConfig({ retentionDays: 366 }))
          .rejects.toThrow('保留天数必须在1-365范围内')
        
        await expect(systemLogService.updateLogConfig({ maxFileSize: 500 }))
          .rejects.toThrow('最大文件大小必须在1KB-1GB范围内')
        
        await expect(systemLogService.updateLogConfig({ maxFileSize: 2147483648 }))
          .rejects.toThrow('最大文件大小必须在1KB-1GB范围内')
      })
    })
  })

  // ==================== 错误处理测试 ====================

  describe('错误处理', () => {
    it('应该正确处理API错误', async () => {
      const error = new Error('API错误')
      mockLog.getLogList.mockRejectedValue(error)

      await expect(systemLogService.getLogList()).rejects.toThrow('获取日志列表失败: API错误')
    })

    it('应该正确处理HTTP错误响应', async () => {
      const httpError = {
        response: {
          data: {
            message: 'HTTP错误信息'
          }
        }
      }
      mockLog.getLogDetail.mockRejectedValue(httpError)

      await expect(systemLogService.getLogDetail('c3d4e5f6789012345678901234567890'))
        .rejects.toThrow('获取日志详情失败 (ID: c3d4e5f6789012345678901234567890): HTTP错误信息')
    })

    it('应该正确处理未知错误', async () => {
      mockLog.exportLogs.mockRejectedValue(new Error())

      await expect(systemLogService.exportLogs({ format: 'CSV' }))
        .rejects.toThrow('创建日志导出任务失败')
    })
  })

  // ==================== 边界条件测试 ====================

  describe('边界条件测试', () => {
    it('应该处理空结果集', async () => {
      const emptyResponse: SystemLogPaginatedResponse<SystemLog> = {
        total: 0,
        pages: 0,
        current: 1,
        size: 10,
        records: []
      }
      mockLog.getLogList.mockResolvedValue(emptyResponse)

      const result = await systemLogService.getLogList()
      
      expect(result.total).toBe(0)
      expect(result.records).toHaveLength(0)
    })

    it('应该处理大数据量分页', async () => {
      const largeResponse: SystemLogPaginatedResponse<SystemLog> = {
        total: 100000,
        pages: 10000,
        current: 1,
        size: 10,
        records: Array(10).fill(null).map((_, i) => ({
          logId: `log_${i.toString().padStart(32, '0')}`,
          level: 'INFO' as const,
          category: 'SYSTEM' as const,
          message: `系统日志 ${i}`,
          createdAt: new Date().toISOString()
        }))
      }
      mockLog.getLogList.mockResolvedValue(largeResponse)

      const result = await systemLogService.getLogList({ page: 1, size: 10 })
      
      expect(result.total).toBe(100000)
      expect(result.records).toHaveLength(10)
    })

    it('应该处理最小tail值的实时日志查询', async () => {
      const singleLogResponse: RealtimeLogsResponse = {
        logs: [{
          logId: 'c3d4e5f6789012345678901234567890',
          level: 'INFO',
          category: 'SYSTEM',
          message: '单条日志',
          createdAt: '2024-01-01T10:00:00'
        }],
        totalCount: 1,
        lastUpdateTime: '2024-01-01T10:00:00'
      }
      mockLog.getRealtimeLogs.mockResolvedValue(singleLogResponse)

      const result = await systemLogService.getRealtimeLogs({ tail: 1 })
      expect(result.logs).toHaveLength(1)
      expect(result.totalCount).toBe(1)
    })

    it('应该处理最大tail值的实时日志查询', async () => {
      const maxLogsResponse: RealtimeLogsResponse = {
        logs: Array(1000).fill(null).map((_, i) => ({
          logId: `log_${i.toString().padStart(32, '0')}`,
          level: 'INFO' as const,
          category: 'SYSTEM' as const,
          message: `日志 ${i}`,
          createdAt: new Date().toISOString()
        })),
        totalCount: 1000,
        lastUpdateTime: new Date().toISOString()
      }
      mockLog.getRealtimeLogs.mockResolvedValue(maxLogsResponse)

      const result = await systemLogService.getRealtimeLogs({ tail: 1000 })
      expect(result.logs).toHaveLength(1000)
      expect(result.totalCount).toBe(1000)
    })
  })

  // ==================== 并发测试 ====================

  describe('并发操作测试', () => {
    it('应该支持并发查询操作', async () => {
      const mockResponse = systemLogMock.getLogList()
      mockLog.getLogList.mockResolvedValue(mockResponse.data)

      // 并发执行多个查询
      const promises = Array(5).fill(null).map(() => 
        systemLogService.getLogList({ page: 1, size: 10 })
      )

      const results = await Promise.all(promises)
      
      expect(results).toHaveLength(5)
      results.forEach(result => {
        expect(result).toHaveProperty('records')
      })
    })

    it('应该支持并发监控查询', async () => {
      const systemMonitorResponse = systemLogMock.getSystemMonitor()
      const logMonitorResponse = systemLogMock.getLogMonitor()
      const performanceMonitorResponse = systemLogMock.getPerformanceMonitor()

      mockLog.getSystemMonitor.mockResolvedValueOnce(systemMonitorResponse.data)
      mockLog.getLogMonitor.mockResolvedValueOnce(logMonitorResponse.data)
      mockLog.getPerformanceMonitor.mockResolvedValueOnce(performanceMonitorResponse.data)

      const promises = [
        systemLogService.getSystemMonitor(),
        systemLogService.getLogMonitor(),
        systemLogService.getPerformanceMonitor()
      ]

      const results = await Promise.all(promises)
      
      expect(results).toHaveLength(3)
      expect(results[0]).toHaveProperty('systemInfo')
      expect(results[1]).toHaveProperty('logMetrics')
      expect(results[2]).toHaveProperty('apiMetrics')
    })
  })

  // ==================== 集成测试 ====================

  describe('集成测试', () => {
    it('应该完成完整的导出流程', async () => {
      // 1. 创建导出任务
      const exportData = { level: 'ERROR', format: 'CSV' as const }
      const exportResponse = systemLogMock.exportLogs(exportData)
      mockLog.exportLogs.mockResolvedValue(exportResponse.data)

      const exportResult = await systemLogService.exportLogs(exportData)
      expect(exportResult).toHaveProperty('exportId')

      // 2. 查询导出状态
      const statusResponse = systemLogMock.getExportStatus(exportResult.exportId)
      mockLog.getExportStatus.mockResolvedValue(statusResponse.data)

      const statusResult = await systemLogService.getExportStatus(exportResult.exportId)
      expect(statusResult.exportId).toBe(exportResult.exportId)

      // 3. 下载导出文件
      const mockBlob = new Blob(['test'], { type: 'text/csv' })
      mockLog.downloadExportFile.mockResolvedValue(mockBlob)

      const downloadResult = await systemLogService.downloadExportFile(exportResult.exportId)
      expect(downloadResult).toBeInstanceOf(Blob)
    })

    it('应该完成完整的清理流程', async () => {
      // 1. 创建清理任务
      const cleanupData = { strategy: 'TIME_BASED' as const, retentionDays: 30 }
      const cleanupResponse = systemLogMock.cleanupLogs(cleanupData)
      mockLog.cleanupLogs.mockResolvedValue(cleanupResponse.data)

      const cleanupResult = await systemLogService.cleanupLogs(cleanupData)
      expect(cleanupResult).toHaveProperty('cleanupId')

      // 2. 查询清理状态
      const statusResponse = systemLogMock.getCleanupStatus(cleanupResult.cleanupId)
      mockLog.getCleanupStatus.mockResolvedValue(statusResponse.data)

      const statusResult = await systemLogService.getCleanupStatus(cleanupResult.cleanupId)
      expect(statusResult.cleanupId).toBe(cleanupResult.cleanupId)
    })

    it('应该完成配置更新流程', async () => {
      // 1. 获取当前配置
      const configResponse = systemLogMock.getLogConfig()
      mockLog.getLogConfig.mockResolvedValue(configResponse.data)

      const currentConfig = await systemLogService.getLogConfig()
      expect(currentConfig).toHaveProperty('logLevel')

      // 2. 更新配置
      const updateData = { logLevel: 'DEBUG', retentionDays: 60 }
      const updateResponse = systemLogMock.updateLogConfig(updateData)
      mockLog.updateLogConfig.mockResolvedValue(updateResponse.data)

      const updateResult = await systemLogService.updateLogConfig(updateData)
      expect(updateResult).toHaveProperty('updatedAt')
    })
  })
})
