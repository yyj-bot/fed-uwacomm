/**
 * 模型版本管理服务单元测试
 * 使用 Vitest 测试所有接口方法
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { modelVersionService } from '@/services/model-version'
import { modelVersionMock } from '@/mocks/services/modelVersionMock'

// Mock API 模块
vi.mock('@/api/model-version', () => ({
  model: {
    uploadModel: vi.fn(),
    uploadModelBatch: vi.fn(),
    getModelVersions: vi.fn(),
    getModelVersionDetail: vi.fn(),
    getTaskModelVersions: vi.fn(),
    evaluateModel: vi.fn(),
    evaluateModelBatch: vi.fn(),
    getEvaluationResults: vi.fn(),
    deployModel: vi.fn(),
    getDeploymentStatus: vi.fn(),
    getDeploymentList: vi.fn(),
    rollbackModel: vi.fn(),
    getRollbackHistory: vi.fn(),
    downloadModel: vi.fn(),
    downloadModelBatch: vi.fn(),
    deleteModel: vi.fn(),
    deleteModelBatch: vi.fn(),
    getModelStatistics: vi.fn(),
    getTaskModelStatistics: vi.fn()
  }
}))

// 获取模拟的 API
const mockApiModule = await import('@/api/model-version')
const mockModel = vi.mocked(mockApiModule.model)

describe('ModelVersionService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ==================== 模型上传测试 ====================
  
  describe('模型上传功能', () => {
    it('应该成功上传模型文件', async () => {
      // 准备测试数据
      const formData = new FormData()
      formData.append('taskId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('roundNumber', '1')
      formData.append('description', '测试模型')
      formData.append('file', new File(['test'], 'model.pth', { type: 'application/octet-stream' }))

      const mockResponse = modelVersionMock.uploadModel(formData)
      mockModel.uploadModel.mockResolvedValue(mockResponse.data)

      // 执行测试
      const result = await modelVersionService.uploadModel(formData)

      // 验证结果
      expect(mockModel.uploadModel).toHaveBeenCalledWith(formData)
      expect(result).toEqual(mockResponse.data)
      expect(result.taskId).toBe('a1b2c3d4e5f678901234567890123456')
      expect(result.roundNumber).toBe(1)
      expect(result.status).toBe('UPLOADED')
    })

    it('应该验证上传参数', async () => {
      // 测试空 FormData
      const emptyFormData = new FormData()
      
      await expect(modelVersionService.uploadModel(emptyFormData)).rejects.toThrow('任务ID不能为空')
    })

    it('应该验证任务ID格式', async () => {
      const formData = new FormData()
      formData.append('taskId', 'invalid-task-id')
      formData.append('roundNumber', '1')
      formData.append('file', new File(['test'], 'model.pth'))

      await expect(modelVersionService.uploadModel(formData)).rejects.toThrow('任务ID格式不正确')
    })

    it('应该验证文件大小限制', async () => {
      const formData = new FormData()
      formData.append('taskId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('roundNumber', '1')
      
      // 创建超大文件 (101MB)
      const largeFile = new File(['x'.repeat(101 * 1024 * 1024)], 'large.pth')
      formData.append('file', largeFile)

      await expect(modelVersionService.uploadModel(formData)).rejects.toThrow('模型文件大小不能超过100MB')
    })

    it('应该验证文件格式', async () => {
      const formData = new FormData()
      formData.append('taskId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('roundNumber', '1')
      formData.append('file', new File(['test'], 'model.txt')) // 不支持的格式

      await expect(modelVersionService.uploadModel(formData)).rejects.toThrow('不支持的模型文件格式')
    })

    it('应该成功批量上传模型', async () => {
      const batchData = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        models: [
          {
            roundNumber: 1,
            description: '第1轮模型',
            parameters: { learning_rate: 0.001 },
            file: new File(['test1'], 'model1.pth')
          },
          {
            roundNumber: 2,
            description: '第2轮模型',
            parameters: { learning_rate: 0.001 },
            file: new File(['test2'], 'model2.pth')
          }
        ]
      }

      const mockResponse = modelVersionMock.uploadModelBatch(batchData)
      mockModel.uploadModelBatch.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.uploadModelBatch(batchData)

      expect(mockModel.uploadModelBatch).toHaveBeenCalledWith(batchData)
      expect(result.successCount).toBeGreaterThan(0)
      expect(result.models).toHaveLength(2)
    })

    it('应该验证批量上传数量限制', async () => {
      const batchData = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        models: Array(21).fill({
          roundNumber: 1,
          file: new File(['test'], 'model.pth')
        })
      }

      await expect(modelVersionService.uploadModelBatch(batchData)).rejects.toThrow('批量上传模型数量不能超过20个')
    })
  })

  // ==================== 模型版本查询测试 ====================
  
  describe('模型版本查询功能', () => {
    it('应该成功获取模型版本列表', async () => {
      const params = {
        page: 1,
        size: 10,
        taskId: 'a1b2c3d4e5f678901234567890123456'
      }

      const mockResponse = modelVersionMock.getModelVersions(params)
      mockModel.getModelVersions.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.getModelVersions(params)

      expect(mockModel.getModelVersions).toHaveBeenCalledWith(params)
      expect(result).toHaveProperty('total')
      expect(result).toHaveProperty('records')
      expect(Array.isArray(result.records)).toBe(true)
    })

    it('应该验证分页参数', async () => {
      await expect(modelVersionService.getModelVersions({ page: 0 })).rejects.toThrow('页码必须大于0')
      await expect(modelVersionService.getModelVersions({ size: 0 })).rejects.toThrow('每页大小必须在1-100范围内')
      await expect(modelVersionService.getModelVersions({ size: 101 })).rejects.toThrow('每页大小必须在1-100范围内')
    })

    it('应该验证任务ID格式', async () => {
      await expect(modelVersionService.getModelVersions({ taskId: 'invalid-id' })).rejects.toThrow('任务ID格式不正确')
    })

    it('应该验证状态参数', async () => {
      await expect(modelVersionService.getModelVersions({ status: 'INVALID_STATUS' as any })).rejects.toThrow('状态参数无效')
    })

    it('应该验证排序参数', async () => {
      await expect(modelVersionService.getModelVersions({ sort: 'invalid_field' })).rejects.toThrow('排序字段无效')
      await expect(modelVersionService.getModelVersions({ order: 'invalid' as any })).rejects.toThrow('排序方向必须为asc或desc')
    })

    it('应该成功获取模型版本详情', async () => {
      const modelId = 'c3d4e5f6789012345678901234567890'
      const mockResponse = modelVersionMock.getModelVersionDetail(modelId)
      mockModel.getModelVersionDetail.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.getModelVersionDetail(modelId)

      expect(mockModel.getModelVersionDetail).toHaveBeenCalledWith(modelId)
      expect(result.modelId).toBe(modelId)
      expect(result).toHaveProperty('taskId')
      expect(result).toHaveProperty('roundNumber')
      expect(result).toHaveProperty('metrics')
    })

    it('应该验证模型ID格式', async () => {
      await expect(modelVersionService.getModelVersionDetail('invalid-id')).rejects.toThrow('模型ID格式不正确')
      await expect(modelVersionService.getModelVersionDetail('')).rejects.toThrow('模型ID不能为空')
    })

    it('应该成功获取任务模型版本', async () => {
      const taskId = 'a1b2c3d4e5f678901234567890123456'
      const params = { roundNumber: 1, status: 'UPLOADED' }
      
      const mockResponse = modelVersionMock.getTaskModelVersions(taskId, params)
      mockModel.getTaskModelVersions.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.getTaskModelVersions(taskId, params)

      expect(mockModel.getTaskModelVersions).toHaveBeenCalledWith(taskId, params)
      expect(result.taskId).toBe(taskId)
      expect(result).toHaveProperty('taskName')
      expect(result).toHaveProperty('versions')
      expect(Array.isArray(result.versions)).toBe(true)
    })
  })

  // ==================== 模型性能评估测试 ====================
  
  describe('模型性能评估功能', () => {
    it('应该成功评估模型性能', async () => {
      const evaluationData = {
        modelId: 'c3d4e5f6789012345678901234567890',
        testDataPath: '/data/test.csv',
        metrics: ['accuracy', 'loss'],
        batchSize: 32,
        device: 'cpu'
      }

      const mockResponse = modelVersionMock.evaluateModel(evaluationData)
      mockModel.evaluateModel.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.evaluateModel(evaluationData)

      expect(mockModel.evaluateModel).toHaveBeenCalledWith(evaluationData)
      expect(result.modelId).toBe(evaluationData.modelId)
      expect(result).toHaveProperty('evaluationId')
      expect(result).toHaveProperty('metrics')
      expect(result.status).toBe('COMPLETED')
    })

    it('应该验证评估参数', async () => {
      const invalidData = {
        modelId: 'invalid-id',
        testDataPath: '',
        batchSize: -1,
        device: 'invalid'
      }

      await expect(modelVersionService.evaluateModel(invalidData)).rejects.toThrow()
    })

    it('应该成功批量评估模型', async () => {
      const evaluationData = {
        taskId: 'a1b2c3d4e5f678901234567890123456',
        testDataPath: '/data/test.csv',
        roundNumbers: [1, 2, 3],
        metrics: ['accuracy', 'loss']
      }

      const mockResponse = modelVersionMock.evaluateModelBatch(evaluationData)
      mockModel.evaluateModelBatch.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.evaluateModelBatch(evaluationData)

      expect(mockModel.evaluateModelBatch).toHaveBeenCalledWith(evaluationData)
      expect(result.taskId).toBe(evaluationData.taskId)
      expect(result.evaluatedCount).toBeGreaterThan(0)
      expect(Array.isArray(result.results)).toBe(true)
    })

    it('应该成功获取评估结果', async () => {
      const params = {
        modelId: 'c3d4e5f6789012345678901234567890',
        page: 1,
        size: 10
      }

      const mockResponse = modelVersionMock.getEvaluationResults(params)
      mockModel.getEvaluationResults.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.getEvaluationResults(params)

      expect(mockModel.getEvaluationResults).toHaveBeenCalledWith(params)
      expect(result).toHaveProperty('total')
      expect(result).toHaveProperty('records')
    })
  })

  // ==================== 模型部署测试 ====================
  
  describe('模型部署功能', () => {
    it('应该成功部署模型', async () => {
      const deploymentData = {
        modelId: 'c3d4e5f6789012345678901234567890',
        deploymentName: '水声分类模型_v1.0',
        targetVms: ['vm_1', 'vm_2'],
        deploymentConfig: {
          replicas: 2,
          resources: {
            cpu: '1',
            memory: '2Gi'
          }
        }
      }

      const mockResponse = modelVersionMock.deployModel(deploymentData)
      mockModel.deployModel.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.deployModel(deploymentData)

      expect(mockModel.deployModel).toHaveBeenCalledWith(deploymentData)
      expect(result.modelId).toBe(deploymentData.modelId)
      expect(result.deploymentName).toBe(deploymentData.deploymentName)
      expect(result).toHaveProperty('deploymentId')
      expect(result.status).toBe('DEPLOYED')
    })

    it('应该验证部署参数', async () => {
      const invalidData = {
        modelId: 'invalid-id',
        deploymentName: ''
      }

      await expect(modelVersionService.deployModel(invalidData)).rejects.toThrow()
    })

    it('应该验证部署名称长度', async () => {
      const deploymentData = {
        modelId: 'c3d4e5f6789012345678901234567890',
        deploymentName: 'x'.repeat(101) // 超过100字符
      }

      await expect(modelVersionService.deployModel(deploymentData)).rejects.toThrow('部署名称不能超过100个字符')
    })

    it('应该成功获取部署状态', async () => {
      const deploymentId = 'deploy_1234567890'
      const mockResponse = modelVersionMock.getDeploymentStatus(deploymentId)
      mockModel.getDeploymentStatus.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.getDeploymentStatus(deploymentId)

      expect(mockModel.getDeploymentStatus).toHaveBeenCalledWith(deploymentId)
      expect(result).toHaveProperty('deploymentId')
      expect(result).toHaveProperty('status')
      expect(result).toHaveProperty('replicas')
    })

    it('应该成功获取部署列表', async () => {
      const params = { modelId: 'c3d4e5f6789012345678901234567890', page: 1, size: 10 }
      const mockResponse = modelVersionMock.getDeploymentList(params)
      mockModel.getDeploymentList.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.getDeploymentList(params)

      expect(mockModel.getDeploymentList).toHaveBeenCalledWith(params)
      expect(result).toHaveProperty('records')
    })
  })

  // ==================== 模型回滚测试 ====================
  
  describe('模型回滚功能', () => {
    it('应该成功回滚模型', async () => {
      const rollbackData = {
        deploymentId: 'deploy_1234567890',
        targetModelId: 'c3d4e5f6789012345678901234567891',
        rollbackReason: '性能下降',
        force: false
      }

      const mockResponse = modelVersionMock.rollbackModel(rollbackData)
      
      // 确保mock返回的是正确的数据结构
      if (mockResponse.code === 200) {
        mockModel.rollbackModel.mockResolvedValue(mockResponse.data)
      } else {
        mockModel.rollbackModel.mockRejectedValue(new Error(mockResponse.message))
      }

      const result = await modelVersionService.rollbackModel(rollbackData)

      expect(mockModel.rollbackModel).toHaveBeenCalledWith(rollbackData)
      expect(result.deploymentId).toBe(rollbackData.deploymentId)
      expect(result.toModelId).toBe(rollbackData.targetModelId)
      expect(result).toHaveProperty('rollbackId')
      expect(result.status).toBe('COMPLETED')
    })

    it('应该验证回滚参数', async () => {
      const invalidData = {
        deploymentId: '',
        targetModelId: 'invalid-id'
      }

      await expect(modelVersionService.rollbackModel(invalidData)).rejects.toThrow()
    })

    it('应该成功获取回滚历史', async () => {
      const params = { deploymentId: 'deploy_1234567890', page: 1, size: 10 }
      const mockResponse = modelVersionMock.getRollbackHistory(params)
      mockModel.getRollbackHistory.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.getRollbackHistory(params)

      expect(mockModel.getRollbackHistory).toHaveBeenCalledWith(params)
      expect(result).toHaveProperty('records')
    })
  })

  // ==================== 模型下载测试 ====================
  
  describe('模型下载功能', () => {
    it('应该成功下载模型文件', async () => {
      const modelId = 'c3d4e5f6789012345678901234567890'
      const params = { format: 'original' as const, compressed: true }
      
      const mockBlob = modelVersionMock.downloadModel(modelId, params)
      mockModel.downloadModel.mockResolvedValue(mockBlob)

      const result = await modelVersionService.downloadModel(modelId, params)

      expect(mockModel.downloadModel).toHaveBeenCalledWith(modelId, params)
      expect(result).toBeInstanceOf(Blob)
    })

    it('应该验证下载参数', async () => {
      await expect(modelVersionService.downloadModel('invalid-id')).rejects.toThrow('模型ID格式不正确')
    })

    it('应该成功批量下载模型', async () => {
      const downloadData = {
        modelIds: ['c3d4e5f6789012345678901234567890', 'd4e5f678901234567890123456789012'],
        format: 'original' as const,
        compressed: true
      }

      const mockBlob = modelVersionMock.downloadModelBatch(downloadData)
      mockModel.downloadModelBatch.mockResolvedValue(mockBlob)

      const result = await modelVersionService.downloadModelBatch(downloadData)

      expect(mockModel.downloadModelBatch).toHaveBeenCalledWith(downloadData)
      expect(result).toBeInstanceOf(Blob)
    })

    it('应该验证批量下载数量限制', async () => {
      const downloadData = {
        modelIds: Array(51).fill('c3d4e5f6789012345678901234567890')
      }

      await expect(modelVersionService.downloadModelBatch(downloadData)).rejects.toThrow('批量下载模型数量不能超过50个')
    })
  })

  // ==================== 模型删除测试 ====================
  
  describe('模型删除功能', () => {
    it('应该成功删除模型', async () => {
      const modelId = 'c3d4e5f6789012345678901234567890'
      const deleteData = { force: true, deleteFile: true }
      
      const mockResponse = modelVersionMock.deleteModel(modelId, deleteData)
      mockModel.deleteModel.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.deleteModel(modelId, deleteData)

      expect(mockModel.deleteModel).toHaveBeenCalledWith(modelId, deleteData)
      expect(result.modelId).toBe(modelId)
      expect(result).toHaveProperty('deletedAt')
    })

    it('应该成功批量删除模型', async () => {
      const deleteData = {
        modelIds: ['c3d4e5f6789012345678901234567890', 'd4e5f678901234567890123456789012'],
        force: true,
        deleteFile: true
      }

      const mockResponse = modelVersionMock.deleteModelBatch(deleteData)
      mockModel.deleteModelBatch.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.deleteModelBatch(deleteData)

      expect(mockModel.deleteModelBatch).toHaveBeenCalledWith(deleteData)
      expect(result).toHaveProperty('successCount')
      expect(result).toHaveProperty('results')
    })

    it('应该验证批量删除数量限制', async () => {
      const deleteData = {
        modelIds: Array(51).fill('c3d4e5f6789012345678901234567890')
      }

      await expect(modelVersionService.deleteModelBatch(deleteData)).rejects.toThrow('批量删除模型数量不能超过50个')
    })
  })

  // ==================== 模型统计测试 ====================
  
  describe('模型统计功能', () => {
    it('应该成功获取模型统计信息', async () => {
      const params = { taskId: 'a1b2c3d4e5f678901234567890123456', timeRange: '30d' as const }
      const mockResponse = modelVersionMock.getModelStatistics(params)
      mockModel.getModelStatistics.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.getModelStatistics(params)

      expect(mockModel.getModelStatistics).toHaveBeenCalledWith(params)
      expect(result).toHaveProperty('totalModels')
      expect(result).toHaveProperty('averageAccuracy')
      expect(result).toHaveProperty('uploadTrend')
      expect(result).toHaveProperty('accuracyTrend')
    })

    it('应该验证统计参数', async () => {
      await expect(modelVersionService.getModelStatistics({ timeRange: 'invalid' as any })).rejects.toThrow('时间范围参数无效')
    })

    it('应该成功获取任务模型统计', async () => {
      const taskId = 'a1b2c3d4e5f678901234567890123456'
      const mockResponse = modelVersionMock.getTaskModelStatistics(taskId)
      mockModel.getTaskModelStatistics.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.getTaskModelStatistics(taskId)

      expect(mockModel.getTaskModelStatistics).toHaveBeenCalledWith(taskId)
      expect(result.taskId).toBe(taskId)
      expect(result).toHaveProperty('taskName')
      expect(result).toHaveProperty('performanceMetrics')
    })
  })

  // ==================== 错误处理测试 ====================
  
  describe('错误处理', () => {
    it('应该正确处理API错误', async () => {
      const error = new Error('API错误')
      mockModel.getModelVersions.mockRejectedValue(error)

      await expect(modelVersionService.getModelVersions()).rejects.toThrow('获取模型版本列表失败: API错误')
    })

    it('应该正确处理HTTP错误响应', async () => {
      const httpError = {
        response: {
          data: {
            message: 'HTTP错误信息'
          }
        }
      }
      mockModel.getModelVersionDetail.mockRejectedValue(httpError)

      await expect(modelVersionService.getModelVersionDetail('c3d4e5f6789012345678901234567890'))
        .rejects.toThrow('获取模型版本详情失败 (ID: c3d4e5f6789012345678901234567890): HTTP错误信息')
    })

    it('应该正确处理未知错误', async () => {
      mockModel.uploadModel.mockRejectedValue(new Error())

      const formData = new FormData()
      formData.append('taskId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('roundNumber', '1')
      formData.append('file', new File(['test'], 'model.pth'))

      await expect(modelVersionService.uploadModel(formData))
        .rejects.toThrow('模型文件上传失败')
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
      mockModel.getModelVersions.mockResolvedValue(emptyResponse)

      const result = await modelVersionService.getModelVersions()
      
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
          modelId: `model_${i}`,
          taskId: 'task_1',
          roundNumber: i + 1,
          accuracy: 0.8 + i * 0.01,
          loss: 0.2 - i * 0.01,
          status: 'UPLOADED' as const,
          description: `第${i+1}轮模型`,
          parameters: {
            learning_rate: 0.001,
            batch_size: 32
          },
          createdAt: new Date().toISOString()
        }))
      }
      mockModel.getModelVersions.mockResolvedValue(largeResponse)

      const result = await modelVersionService.getModelVersions({ page: 1, size: 10 })
      
      expect(result.total).toBe(10000)
      expect(result.records).toHaveLength(10)
    })

    it('应该处理极小文件', async () => {
      const formData = new FormData()
      formData.append('taskId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('roundNumber', '1')
      formData.append('file', new File([''], 'empty.pth')) // 空文件

      const mockResponse = modelVersionMock.uploadModel(formData)
      mockModel.uploadModel.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.uploadModel(formData)
      expect(result).toBeDefined()
    })

    it('应该处理最大文件大小', async () => {
      const formData = new FormData()
      formData.append('taskId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('roundNumber', '1')
      
      // 创建正好100MB的文件
      const maxSizeFile = new File(['x'.repeat(100 * 1024 * 1024)], 'max.pth')
      formData.append('file', maxSizeFile)

      const mockResponse = modelVersionMock.uploadModel(formData)
      mockModel.uploadModel.mockResolvedValue(mockResponse.data)

      const result = await modelVersionService.uploadModel(formData)
      expect(result).toBeDefined()
    })
  })

  // ==================== 并发测试 ====================
  
  describe('并发操作测试', () => {
    it('应该支持并发查询操作', async () => {
      const mockResponse = modelVersionMock.getModelVersions()
      mockModel.getModelVersions.mockResolvedValue(mockResponse.data)

      // 并发执行多个查询
      const promises = Array(5).fill(null).map(() => 
        modelVersionService.getModelVersions({ page: 1, size: 10 })
      )

      const results = await Promise.all(promises)
      
      expect(results).toHaveLength(5)
      results.forEach(result => {
        expect(result).toHaveProperty('records')
      })
    })

    it('应该支持并发上传操作', async () => {
      const createFormData = (index: number) => {
        const formData = new FormData()
        formData.append('taskId', 'a1b2c3d4e5f678901234567890123456')
        formData.append('roundNumber', index.toString())
        formData.append('file', new File([`test${index}`], `model${index}.pth`))
        return formData
      }

      // 模拟并发上传
      const formDataArray = Array(3).fill(null).map((_, i) => createFormData(i + 1))
      
      formDataArray.forEach(formData => {
        const mockResponse = modelVersionMock.uploadModel(formData)
        mockModel.uploadModel.mockResolvedValueOnce(mockResponse.data)
      })

      const promises = formDataArray.map(formData => 
        modelVersionService.uploadModel(formData)
      )

      const results = await Promise.all(promises)
      
      expect(results).toHaveLength(3)
      results.forEach(result => {
        expect(result).toHaveProperty('modelId')
        expect(result.status).toBe('UPLOADED')
      })
    })
  })
})
