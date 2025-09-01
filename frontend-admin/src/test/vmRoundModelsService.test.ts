/**
 * VM本地模型服务单元测试
 * 使用 Vitest 测试所有接口方法
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { vmRoundModelsService } from '@/services/vm-round-models'
import { vmRoundModelsMock } from '@/mocks/vmRoundModelsMock'

// Mock API 模块
vi.mock('@/api/vm-round-models', () => ({
  vmRoundModels: {
    getVMRoundModels: vi.fn(),
    getVMRoundModelDetail: vi.fn(),
    getVMModelTrend: vi.fn(),
    getVMModelBest: vi.fn()
  }
}))

// 获取模拟的 API
const mockApiModule = await import('@/api/vm-round-models')
const mockVMRoundModels = vi.mocked(mockApiModule.vmRoundModels)

describe('VMRoundModelsService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ==================== VM本地模型列表查询测试 ====================
  
  describe('VM本地模型列表查询功能', () => {
    it('应该成功获取VM本地模型列表', async () => {
      const params = {
        page: 1,
        size: 10,
        taskId: 'a1b2c3d4e5f678901234567890123456'
      }

      const mockResponse = vmRoundModelsMock.getVMRoundModels(params)
      mockVMRoundModels.getVMRoundModels.mockResolvedValue(mockResponse.data)

      const result = await vmRoundModelsService.getVMRoundModels(params)

      expect(mockVMRoundModels.getVMRoundModels).toHaveBeenCalledWith(params)
      expect(result).toHaveProperty('total')
      expect(result).toHaveProperty('current')
      expect(result).toHaveProperty('records')
      expect(Array.isArray(result.records)).toBe(true)
    })

    it('应该验证分页参数', async () => {
      await expect(vmRoundModelsService.getVMRoundModels({ page: 0 })).rejects.toThrow('页码必须大于0')
      await expect(vmRoundModelsService.getVMRoundModels({ size: 0 })).rejects.toThrow('每页大小必须在1-100范围内')
      await expect(vmRoundModelsService.getVMRoundModels({ size: 101 })).rejects.toThrow('每页大小必须在1-100范围内')
    })

    it('应该验证任务ID格式', async () => {
      await expect(vmRoundModelsService.getVMRoundModels({ taskId: 'invalid-id' })).rejects.toThrow('任务ID格式不正确')
    })

    it('应该验证虚拟机ID格式', async () => {
      await expect(vmRoundModelsService.getVMRoundModels({ vmId: 'invalid-id' })).rejects.toThrow('虚拟机ID格式不正确')
    })

    it('应该验证训练轮数', async () => {
      await expect(vmRoundModelsService.getVMRoundModels({ roundNumber: 0 })).rejects.toThrow('训练轮数必须为正整数')
      await expect(vmRoundModelsService.getVMRoundModels({ roundNumber: -1 })).rejects.toThrow('训练轮数必须为正整数')
    })

    it('应该支持按虚拟机ID过滤', async () => {
      const params = {
        vmId: 'b2c3d4e5f678901234567890123456cd',
        page: 1,
        size: 10
      }

      const mockResponse = vmRoundModelsMock.getVMRoundModels(params)
      mockVMRoundModels.getVMRoundModels.mockResolvedValue(mockResponse.data)

      const result = await vmRoundModelsService.getVMRoundModels(params)

      expect(mockVMRoundModels.getVMRoundModels).toHaveBeenCalledWith(params)
      expect(result.records.length).toBeGreaterThan(0)
    })

    it('应该支持按训练轮数过滤', async () => {
      const params = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        roundNumber: 1,
        page: 1,
        size: 10
      }

      const mockResponse = vmRoundModelsMock.getVMRoundModels(params)
      mockVMRoundModels.getVMRoundModels.mockResolvedValue(mockResponse.data)

      const result = await vmRoundModelsService.getVMRoundModels(params)

      expect(mockVMRoundModels.getVMRoundModels).toHaveBeenCalledWith(params)
      expect(result.records.every(record => record.roundNumber === 1)).toBe(true)
    })
  })

  // ==================== VM本地模型详情查询测试 ====================
  
  describe('VM本地模型详情查询功能', () => {
    it('应该成功获取VM本地模型详情', async () => {
      const vmRoundModelId = 'vmrm-001'
      const mockResponse = vmRoundModelsMock.getVMRoundModelDetail(vmRoundModelId)
      mockVMRoundModels.getVMRoundModelDetail.mockResolvedValue(mockResponse.data)

      const result = await vmRoundModelsService.getVMRoundModelDetail(vmRoundModelId)

      expect(mockVMRoundModels.getVMRoundModelDetail).toHaveBeenCalledWith(vmRoundModelId)
      expect(result.vmRoundModelId).toBe(vmRoundModelId)
      expect(result).toHaveProperty('taskId')
      expect(result).toHaveProperty('roundNumber')
      expect(result).toHaveProperty('vmId')
      expect(result).toHaveProperty('metrics')
      expect(result).toHaveProperty('createdAt')
    })

    it('应该验证VM本地模型ID格式', async () => {
      await expect(vmRoundModelsService.getVMRoundModelDetail('')).rejects.toThrow('VM本地模型ID不能为空')
      await expect(vmRoundModelsService.getVMRoundModelDetail('invalid-id')).rejects.toThrow('VM本地模型ID格式不正确')
    })

    it('应该处理不存在的VM本地模型', async () => {
      const vmRoundModelId = 'vmrm-nonexistent'
      const mockResponse = vmRoundModelsMock.getVMRoundModelDetail(vmRoundModelId)
      
      if (mockResponse.code === 404) {
        mockVMRoundModels.getVMRoundModelDetail.mockRejectedValue(new Error(mockResponse.message))
      }

      await expect(vmRoundModelsService.getVMRoundModelDetail(vmRoundModelId))
        .rejects.toThrow('获取VM本地模型详情失败')
    })

    it('应该返回完整的模型详情信息', async () => {
      const vmRoundModelId = 'vmrm-001'
      const mockResponse = vmRoundModelsMock.getVMRoundModelDetail(vmRoundModelId)
      mockVMRoundModels.getVMRoundModelDetail.mockResolvedValue(mockResponse.data)

      const result = await vmRoundModelsService.getVMRoundModelDetail(vmRoundModelId)

      expect(result.metrics).toHaveProperty('accuracy')
      expect(result.metrics).toHaveProperty('loss')
      expect(typeof result.metrics.accuracy).toBe('number')
      expect(typeof result.metrics.loss).toBe('number')
      expect(result.modelJson).toBeDefined()
    })
  })

  // ==================== VM模型训练指标趋势测试 ====================
  
  describe('VM模型训练指标趋势功能', () => {
    it('应该成功获取VM模型训练指标趋势', async () => {
      const params = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        vmId: 'b2c3d4e5f678901234567890123456cd',
        metric: 'accuracy'
      }

      const mockResponse = vmRoundModelsMock.getVMModelTrend(params)
      mockVMRoundModels.getVMModelTrend.mockResolvedValue(mockResponse.data)

      const result = await vmRoundModelsService.getVMModelTrend(params)

      expect(mockVMRoundModels.getVMModelTrend).toHaveBeenCalledWith(params)
      expect(result.taskId).toBe(params.taskId)
      expect(result.vmId).toBe(params.vmId)
      expect(result.metric).toBe(params.metric)
      expect(Array.isArray(result.trend)).toBe(true)
      expect(result.trend.length).toBeGreaterThan(0)
    })

    it('应该验证趋势查询参数', async () => {
      await expect(vmRoundModelsService.getVMModelTrend({
        taskId: '',
        vmId: 'b2c3d4e5f678901234567890123456cd',
        metric: 'accuracy'
      })).rejects.toThrow('任务ID不能为空')

      await expect(vmRoundModelsService.getVMModelTrend({
        taskId: 'a1b2c3d4e5f678901234567890123456',
        vmId: '',
        metric: 'accuracy'
      })).rejects.toThrow('虚拟机ID不能为空')

      await expect(vmRoundModelsService.getVMModelTrend({
        taskId: 'a1b2c3d4e5f678901234567890123456',
        vmId: 'b2c3d4e5f678901234567890123456cd',
        metric: ''
      })).rejects.toThrow('指标名称不能为空')
    })

    it('应该验证指标类型', async () => {
      await expect(vmRoundModelsService.getVMModelTrend({
        taskId: 'a1b2c3d4e5f678901234567890123456',
        vmId: 'b2c3d4e5f678901234567890123456cd',
        metric: 'invalid-metric'
      })).rejects.toThrow('不支持的指标类型')
    })

    it('应该支持所有有效的指标类型', async () => {
      const validMetrics = ['accuracy', 'loss', 'precision', 'recall', 'f1']
      const params = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        vmId: 'b2c3d4e5f678901234567890123456cd',
        metric: 'accuracy'
      }

      for (const metric of validMetrics) {
        const testParams = { ...params, metric }
        const mockResponse = vmRoundModelsMock.getVMModelTrend(testParams)
        mockVMRoundModels.getVMModelTrend.mockResolvedValue(mockResponse.data)

        const result = await vmRoundModelsService.getVMModelTrend(testParams)
        expect(result.metric).toBe(metric)
      }
    })

    it('应该返回按轮数排序的趋势数据', async () => {
      const params = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        vmId: 'b2c3d4e5f678901234567890123456cd',
        metric: 'accuracy'
      }

      const mockResponse = vmRoundModelsMock.getVMModelTrend(params)
      mockVMRoundModels.getVMModelTrend.mockResolvedValue(mockResponse.data)

      const result = await vmRoundModelsService.getVMModelTrend(params)

      // 验证趋势数据结构
      result.trend.forEach(item => {
        expect(item).toHaveProperty('roundNumber')
        expect(item).toHaveProperty('value')
        expect(typeof item.roundNumber).toBe('number')
        expect(typeof item.value).toBe('number')
      })

      // 验证排序（轮数应该是递增的）
      for (let i = 1; i < result.trend.length; i++) {
        expect(result.trend[i].roundNumber).toBeGreaterThan(result.trend[i - 1].roundNumber)
      }
    })
  })

  // ==================== VM模型最佳/离群查询测试 ====================
  
  describe('VM模型最佳/离群查询功能', () => {
    it('应该成功获取最佳模型', async () => {
      const params = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        metric: 'accuracy',
        type: 'best' as const
      }

      const mockResponse = vmRoundModelsMock.getVMModelBest(params)
      mockVMRoundModels.getVMModelBest.mockResolvedValue(mockResponse.data)

      const result = await vmRoundModelsService.getVMModelBest(params)

      expect(mockVMRoundModels.getVMModelBest).toHaveBeenCalledWith(params)
      expect(result.taskId).toBe(params.taskId)
      expect(result.metric).toBe(params.metric)
      expect(result.type).toBe(params.type)
      expect(result.result).toHaveProperty('vmRoundModelId')
      expect(result.result).toHaveProperty('roundNumber')
      expect(result.result).toHaveProperty('vmId')
      expect(result.result).toHaveProperty('value')
    })

    it('应该成功获取离群模型', async () => {
      const params = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        metric: 'accuracy',
        type: 'outlier' as const
      }

      const mockResponse = vmRoundModelsMock.getVMModelBest(params)
      mockVMRoundModels.getVMModelBest.mockResolvedValue(mockResponse.data)

      const result = await vmRoundModelsService.getVMModelBest(params)

      expect(result.type).toBe('outlier')
      expect(typeof result.result.value).toBe('number')
    })

    it('应该验证最佳/离群查询参数', async () => {
      await expect(vmRoundModelsService.getVMModelBest({
        taskId: '',
        metric: 'accuracy',
        type: 'best'
      })).rejects.toThrow('任务ID不能为空')

      await expect(vmRoundModelsService.getVMModelBest({
        taskId: 'a1b2c3d4e5f678901234567890123456',
        metric: '',
        type: 'best'
      })).rejects.toThrow('指标名称不能为空')

      await expect(vmRoundModelsService.getVMModelBest({
        taskId: 'a1b2c3d4e5f678901234567890123456',
        metric: 'accuracy',
        type: 'invalid' as any
      })).rejects.toThrow('查询类型必须为best或outlier')
    })

    it('应该支持不同指标的最佳查询', async () => {
      const metrics = ['accuracy', 'loss', 'precision', 'recall', 'f1']
      
      for (const metric of metrics) {
        const params = {
          taskId: 'a1b2c3d4e5f678901234567890123456',
          metric,
          type: 'best' as const
        }

        const mockResponse = vmRoundModelsMock.getVMModelBest(params)
        mockVMRoundModels.getVMModelBest.mockResolvedValue(mockResponse.data)

        const result = await vmRoundModelsService.getVMModelBest(params)
        expect(result.metric).toBe(metric)
        expect(result.type).toBe('best')
      }
    })

    it('应该处理不存在的任务数据', async () => {
      const params = {
        taskId: 'nonexistent-task-id-123456789012',
        metric: 'accuracy',
        type: 'best' as const
      }

      const mockResponse = vmRoundModelsMock.getVMModelBest(params)
      if (mockResponse.code === 404) {
        mockVMRoundModels.getVMModelBest.mockRejectedValue(new Error(mockResponse.message))
      }

      await expect(vmRoundModelsService.getVMModelBest(params))
        .rejects.toThrow('获取VM模型最佳结果失败')
    })

    it('应该处理不存在的指标数据', async () => {
      const params = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        metric: 'nonexistent-metric',
        type: 'best' as const
      }

      // 先验证指标类型
      await expect(vmRoundModelsService.getVMModelBest(params))
        .rejects.toThrow('不支持的指标类型')
    })
  })

  // ==================== 错误处理测试 ====================
  
  describe('错误处理', () => {
    it('应该正确处理API错误', async () => {
      const error = new Error('API错误')
      mockVMRoundModels.getVMRoundModels.mockRejectedValue(error)

      await expect(vmRoundModelsService.getVMRoundModels()).rejects.toThrow('获取VM本地模型列表失败: API错误')
    })

    it('应该正确处理HTTP错误响应', async () => {
      const httpError = {
        response: {
          data: {
            message: 'HTTP错误信息'
          }
        }
      }
      mockVMRoundModels.getVMRoundModelDetail.mockRejectedValue(httpError)

      await expect(vmRoundModelsService.getVMRoundModelDetail('vmrm-001'))
        .rejects.toThrow('获取VM本地模型详情失败 (ID: vmrm-001): HTTP错误信息')
    })

    it('应该正确处理未知错误', async () => {
      mockVMRoundModels.getVMModelTrend.mockRejectedValue(new Error())

      const params = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        vmId: 'b2c3d4e5f678901234567890123456cd',
        metric: 'accuracy'
      }

      await expect(vmRoundModelsService.getVMModelTrend(params))
        .rejects.toThrow('获取VM模型训练指标趋势失败')
    })
  })

  // ==================== 边界条件测试 ====================
  
  describe('边界条件测试', () => {
    it('应该处理空结果集', async () => {
      const emptyResponse = {
        total: 0,
        pages: 0,
        current: 1,
        size: 10,
        records: []
      }
      mockVMRoundModels.getVMRoundModels.mockResolvedValue(emptyResponse)

      const result = await vmRoundModelsService.getVMRoundModels()
      
      expect(result.total).toBe(0)
      expect(result.records).toHaveLength(0)
    })

    it('应该处理大数据量分页', async () => {
      const largeResponse = {
        total: 10000,
        pages: 1000,
        current: 1,
        size: 10,
        records: Array(10).fill({}).map((_, i) => ({
          vmRoundModelId: `vmrm-${i}`,
          taskId: 'a1b2c3d4e5f678901234567890123456',
          roundNumber: i + 1,
          vmId: 'b2c3d4e5f678901234567890123456cd',
          metrics: {
            accuracy: 0.8 + i * 0.01,
            loss: 0.2 - i * 0.01
          },
          createdAt: new Date().toISOString()
        }))
      }
      mockVMRoundModels.getVMRoundModels.mockResolvedValue(largeResponse)

      const result = await vmRoundModelsService.getVMRoundModels({ page: 1, size: 10 })
      
      expect(result.total).toBe(10000)
      expect(result.records).toHaveLength(10)
    })

    it('应该处理单轮数据的趋势查询', async () => {
      const singleRoundTrend = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        vmId: 'b2c3d4e5f678901234567890123456cd',
        metric: 'accuracy',
        trend: [
          { roundNumber: 1, value: 0.75 }
        ]
      }
      mockVMRoundModels.getVMModelTrend.mockResolvedValue(singleRoundTrend)

      const params = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        vmId: 'b2c3d4e5f678901234567890123456cd',
        metric: 'accuracy'
      }

      const result = await vmRoundModelsService.getVMModelTrend(params)
      expect(result.trend).toHaveLength(1)
      expect(result.trend[0].roundNumber).toBe(1)
      expect(result.trend[0].value).toBe(0.75)
    })

    it('应该处理极值数据', async () => {
      const extremeValueModel = {
        vmRoundModelId: 'vmrm-extreme',
        taskId: 'a1b2c3d4e5f678901234567890123456',
        roundNumber: 1,
        vmId: 'b2c3d4e5f678901234567890123456cd',
        metrics: {
          accuracy: 1.0,  // 极值
          loss: 0.0       // 极值
        },
        createdAt: '2024-01-01T10:00:00.000Z'
      }
      mockVMRoundModels.getVMRoundModelDetail.mockResolvedValue(extremeValueModel)

      const result = await vmRoundModelsService.getVMRoundModelDetail('vmrm-extreme')
      expect(result.metrics.accuracy).toBe(1.0)
      expect(result.metrics.loss).toBe(0.0)
    })
  })

  // ==================== 并发测试 ====================
  
  describe('并发操作测试', () => {
    it('应该支持并发查询操作', async () => {
      const mockResponse = vmRoundModelsMock.getVMRoundModels()
      mockVMRoundModels.getVMRoundModels.mockResolvedValue(mockResponse.data)

      // 并发执行多个查询
      const promises = Array(5).fill(null).map(() => 
        vmRoundModelsService.getVMRoundModels({ page: 1, size: 10 })
      )

      const results = await Promise.all(promises)
      
      expect(results).toHaveLength(5)
      results.forEach(result => {
        expect(result).toHaveProperty('records')
      })
    })

    it('应该支持并发详情查询', async () => {
      const modelIds = ['vmrm-001', 'vmrm-002', 'vmrm-003']
      
      modelIds.forEach(id => {
        const mockResponse = vmRoundModelsMock.getVMRoundModelDetail(id)
        mockVMRoundModels.getVMRoundModelDetail.mockResolvedValueOnce(mockResponse.data)
      })

      const promises = modelIds.map(id => 
        vmRoundModelsService.getVMRoundModelDetail(id)
      )

      const results = await Promise.all(promises)
      
      expect(results).toHaveLength(3)
      results.forEach((result, index) => {
        expect(result.vmRoundModelId).toBe(modelIds[index])
      })
    })
  })
})
