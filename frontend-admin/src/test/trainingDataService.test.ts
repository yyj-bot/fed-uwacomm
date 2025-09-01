/**
 * 训练数据服务单元测试
 * 使用Vitest测试所有训练数据服务接口方法
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { TrainingDataService } from '@/services/training-data/trainingDataService'
import { trainingData } from '@/api/training-data'
import { mockTrainingDatasets, mockDatasetDetail, mockDataStatistics, mockPreprocessTask, mockValidationResult, mockBatchOperationResult, mockExportTask, mockTrainingDataApi } from '../mocks/trainingDataMock'
import type { 
  DataStatistics,
  PreprocessTask,
  ValidationResult,
  BatchOperationResult,
  ExportTask,
  DatasetDetail
} from '@/api/training-data'
import type { 
  UploadTextRequest,
  DataListParams,
  PreprocessRequest,
  ValidationRequest,
  UpdateDataRequest,
  DeleteDataRequest,
  BatchOperationRequest,
  DataStatisticsParams,
  ExportDataRequest
} from '@/services/training-data/type'

// Mock trainingData API
vi.mock('@/api/training-data', () => ({
  trainingData: {
    uploadFile: vi.fn(),
    uploadText: vi.fn(),
    getDataList: vi.fn(),
    getDataDetail: vi.fn(),
    downloadData: vi.fn(),
    preprocessData: vi.fn(),
    validateData: vi.fn(),
    updateData: vi.fn(),
    deleteData: vi.fn(),
    batchOperation: vi.fn(),
    getDataStatistics: vi.fn(),
    exportData: vi.fn()
  }
}))

describe('TrainingDataService', () => {
  let trainingDataService: TrainingDataService
  
  beforeEach(() => {
    trainingDataService = new TrainingDataService()
    vi.clearAllMocks()
  })
  
  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('uploadFile', () => {
    it('应该成功上传文件', async () => {
      // 准备测试数据
      const formData = new FormData()
      formData.append('vmId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('dataType', 'ACOUSTIC')
      formData.append('description', '测试声学数据')
      formData.append('file', new File(['test content'], 'test.wav', { type: 'audio/wav' }))
      
      const mockResponse = mockTrainingDataApi.uploadFile(formData).data
      
      // Mock API调用
      vi.mocked(trainingData.uploadFile).mockResolvedValue(mockResponse)
      
      // 执行测试
      const result = await trainingDataService.uploadFile(formData)
      
      // 验证结果
      expect(trainingData.uploadFile).toHaveBeenCalledWith(formData)
      expect(result.datasetId).toBeDefined()
      expect(result.datasetType).toBe('ACOUSTIC')
      expect(result.vmId).toBe('a1b2c3d4e5f678901234567890123456')
      expect(result.status).toBe('UPLOADING')
      expect(result.progress).toBe(0)
    })

    it('应该验证虚拟机ID', async () => {
      const formData = new FormData()
      formData.append('vmId', '') // 空虚拟机ID
      formData.append('dataType', 'ACOUSTIC')
      formData.append('file', new File(['test'], 'test.wav'))
      
      await expect(trainingDataService.uploadFile(formData)).rejects.toThrow('虚拟机ID不能为空')
    })

    it('应该验证虚拟机ID格式', async () => {
      const formData = new FormData()
      formData.append('vmId', 'invalid-id') // 无效格式
      formData.append('dataType', 'ACOUSTIC')
      formData.append('file', new File(['test'], 'test.wav'))
      
      await expect(trainingDataService.uploadFile(formData)).rejects.toThrow('虚拟机ID格式不正确')
    })

    it('应该验证数据类型', async () => {
      const formData = new FormData()
      formData.append('vmId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('dataType', 'INVALID_TYPE') // 无效类型
      formData.append('file', new File(['test'], 'test.wav'))
      
      await expect(trainingDataService.uploadFile(formData)).rejects.toThrow('数据类型无效')
    })

    it('应该验证文件存在', async () => {
      const formData = new FormData()
      formData.append('vmId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('dataType', 'ACOUSTIC')
      // 没有添加文件
      
      await expect(trainingDataService.uploadFile(formData)).rejects.toThrow('上传文件不能为空')
    })

    it('应该验证文件大小', async () => {
      const formData = new FormData()
      formData.append('vmId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('dataType', 'ACOUSTIC')
      // 创建超大文件 (101MB)
      const largeFile = new File(['x'.repeat(101 * 1024 * 1024)], 'large.wav')
      formData.append('file', largeFile)
      
      await expect(trainingDataService.uploadFile(formData)).rejects.toThrow('文件大小不能超过100MB')
    })

    it('应该处理上传失败', async () => {
      const formData = new FormData()
      formData.append('vmId', 'a1b2c3d4e5f678901234567890123456')
      formData.append('dataType', 'ACOUSTIC')
      formData.append('file', new File(['test'], 'test.wav'))
      
      const error = new Error('上传失败')
      vi.mocked(trainingData.uploadFile).mockRejectedValue(error)
      
      await expect(trainingDataService.uploadFile(formData)).rejects.toThrow('文件上传失败')
    })
  })

  describe('uploadText', () => {
    it('应该成功上传文本信息', async () => {
      const textData: UploadTextRequest = {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        dataType: 'ENVIRONMENT',
        title: '声学传播环境配置',
        content: '声学传播环境配置文件内容...',
        description: '声学传播环境配置描述',
        tags: ['environment', 'acoustic'],
        metadata: {
          source: 'bellhop',
          version: '1.0',
          author: '张三'
        }
      }
      
      const mockResponse = mockTrainingDataApi.uploadText(textData).data
      vi.mocked(trainingData.uploadText).mockResolvedValue(mockResponse)
      
      const result = await trainingDataService.uploadText(textData)
      
      expect(trainingData.uploadText).toHaveBeenCalledWith(textData)
      expect(result.datasetId).toBeDefined()
      expect(result.datasetType).toBe('ENVIRONMENT')
      expect(result.vmId).toBe(textData.vmId)
      expect(result.status).toBe('READY')
    })

    it('应该验证标题', async () => {
      const textData: UploadTextRequest = {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        dataType: 'ENVIRONMENT',
        title: '', // 空标题
        content: '测试内容'
      }
      
      await expect(trainingDataService.uploadText(textData)).rejects.toThrow('标题不能为空')
    })

    it('应该验证标题长度', async () => {
      const textData: UploadTextRequest = {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        dataType: 'ENVIRONMENT',
        title: 'a'.repeat(201), // 超长标题
        content: '测试内容'
      }
      
      await expect(trainingDataService.uploadText(textData)).rejects.toThrow('标题不能超过200个字符')
    })

    it('应该验证内容', async () => {
      const textData: UploadTextRequest = {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        dataType: 'ENVIRONMENT',
        title: '测试标题',
        content: '' // 空内容
      }
      
      await expect(trainingDataService.uploadText(textData)).rejects.toThrow('内容不能为空')
    })

    it('应该验证内容大小', async () => {
      const textData: UploadTextRequest = {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        dataType: 'ENVIRONMENT',
        title: '测试标题',
        content: 'x'.repeat(1000001) // 超过1MB
      }
      
      await expect(trainingDataService.uploadText(textData)).rejects.toThrow('内容不能超过1MB')
    })

    it('应该验证标签数量', async () => {
      const textData: UploadTextRequest = {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        dataType: 'ENVIRONMENT',
        title: '测试标题',
        content: '测试内容',
        tags: Array(21).fill('tag') // 超过20个标签
      }
      
      await expect(trainingDataService.uploadText(textData)).rejects.toThrow('标签数量不能超过20个')
    })
  })

  describe('getDataList', () => {
    it('应该成功获取数据列表', async () => {
      const params: DataListParams = { page: 1, size: 10 }
      const mockResponse = mockTrainingDataApi.getDataList(params).data
      
      vi.mocked(trainingData.getDataList).mockResolvedValue(mockResponse)
      
      const result = await trainingDataService.getDataList(params)
      
      expect(trainingData.getDataList).toHaveBeenCalledWith(params)
      expect(result.total).toBeGreaterThanOrEqual(0)
      expect(result.page).toBe(1)
      expect(result.size).toBe(10)
      expect(Array.isArray(result.dataList)).toBe(true)
    })

    it('应该支持虚拟机ID过滤', async () => {
      const params: DataListParams = { vmId: 'a1b2c3d4e5f678901234567890123456' }
      const mockResponse = mockTrainingDataApi.getDataList(params).data
      
      vi.mocked(trainingData.getDataList).mockResolvedValue(mockResponse)
      
      const result = await trainingDataService.getDataList(params)
      
      expect(trainingData.getDataList).toHaveBeenCalledWith(params)
      expect(result.dataList.every(item => item.vmId === params.vmId || result.dataList.length === 0)).toBe(true)
    })

    it('应该支持数据类型过滤', async () => {
      const params: DataListParams = { dataType: 'ACOUSTIC' }
      const mockResponse = mockTrainingDataApi.getDataList(params).data
      
      vi.mocked(trainingData.getDataList).mockResolvedValue(mockResponse)
      
      const result = await trainingDataService.getDataList(params)
      
      expect(trainingData.getDataList).toHaveBeenCalledWith(params)
      expect(result.dataList.every(item => item.datasetType === 'ACOUSTIC' || result.dataList.length === 0)).toBe(true)
    })

    it('应该支持状态过滤', async () => {
      const params: DataListParams = { status: 'READY' }
      const mockResponse = mockTrainingDataApi.getDataList(params).data
      
      vi.mocked(trainingData.getDataList).mockResolvedValue(mockResponse)
      
      const result = await trainingDataService.getDataList(params)
      
      expect(trainingData.getDataList).toHaveBeenCalledWith(params)
      expect(result.dataList.every(item => item.status === 'READY' || result.dataList.length === 0)).toBe(true)
    })

    it('应该验证分页参数', async () => {
      const invalidParams: DataListParams = { page: 0 } // 无效页码
      
      await expect(trainingDataService.getDataList(invalidParams)).rejects.toThrow('页码必须大于0')
    })

    it('应该验证每页大小', async () => {
      const invalidParams: DataListParams = { size: 101 } // 超出限制
      
      await expect(trainingDataService.getDataList(invalidParams)).rejects.toThrow('每页大小必须在1-100范围内')
    })

    it('应该验证虚拟机ID格式', async () => {
      const invalidParams: DataListParams = { vmId: 'invalid-id' }
      
      await expect(trainingDataService.getDataList(invalidParams)).rejects.toThrow('虚拟机ID格式不正确')
    })

    it('应该验证数据类型参数', async () => {
      const invalidParams: DataListParams = { dataType: 'INVALID_TYPE' as any }
      
      await expect(trainingDataService.getDataList(invalidParams)).rejects.toThrow('数据类型参数无效')
    })

    it('应该验证状态参数', async () => {
      const invalidParams: DataListParams = { status: 'INVALID_STATUS' as any }
      
      await expect(trainingDataService.getDataList(invalidParams)).rejects.toThrow('状态参数无效')
    })

    it('应该验证日期范围', async () => {
      const invalidParams: DataListParams = {
        startDate: '2024-01-02',
        endDate: '2024-01-01' // 结束日期早于开始日期
      }
      
      await expect(trainingDataService.getDataList(invalidParams)).rejects.toThrow('开始日期不能晚于结束日期')
    })
  })

  describe('getDataDetail', () => {
    it('应该成功获取数据详情', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const mockDetail = mockDatasetDetail
      
      vi.mocked(trainingData.getDataDetail).mockResolvedValue(mockDetail)
      
      const result = await trainingDataService.getDataDetail(datasetId)
      
      expect(trainingData.getDataDetail).toHaveBeenCalledWith(datasetId)
      expect(result.datasetId).toBe(datasetId)
      expect(result.datasetDescription).toBeDefined()
      expect(result.datasetType).toBeDefined()
      expect(result.status).toBeDefined()
      expect(result.metadata).toBeDefined()
      expect(result.validation).toBeDefined()
      expect(result.preprocessing).toBeDefined()
    })

    it('应该验证数据集ID', async () => {
      await expect(trainingDataService.getDataDetail('')).rejects.toThrow('数据集ID不能为空')
    })

    it('应该验证数据集ID格式', async () => {
      await expect(trainingDataService.getDataDetail('invalid-id')).rejects.toThrow('数据集ID格式不正确')
    })

    it('应该处理数据不存在', async () => {
      const error = new Error('数据不存在')
      vi.mocked(trainingData.getDataDetail).mockRejectedValue(error)
      
      await expect(trainingDataService.getDataDetail('a1b2c3d4e5f678901234567890123456')).rejects.toThrow('获取训练数据详情失败')
    })
  })

  describe('downloadData', () => {
    it('应该成功下载数据', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const mockBlob = new Blob(['test data'], { type: 'application/octet-stream' })
      
      vi.mocked(trainingData.downloadData).mockResolvedValue(mockBlob)
      
      const result = await trainingDataService.downloadData(datasetId)
      
      expect(trainingData.downloadData).toHaveBeenCalledWith(datasetId)
      expect(result).toBeInstanceOf(Blob)
    })

    it('应该验证数据集ID格式', async () => {
      await expect(trainingDataService.downloadData('invalid-id')).rejects.toThrow('数据集ID格式不正确')
    })
  })

  describe('preprocessData', () => {
    it('应该成功预处理数据', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const preprocessData: PreprocessRequest = {
        methods: ['normalization', 'feature_selection'],
        parameters: {
          normalization: {
            method: 'standard_scaler',
            columns: ['feature_1', 'feature_2']
          }
        },
        outputFormat: 'csv'
      }
      
      const mockTask = mockPreprocessTask
      vi.mocked(trainingData.preprocessData).mockResolvedValue(mockTask)
      
      const result = await trainingDataService.preprocessData(datasetId, preprocessData)
      
      expect(trainingData.preprocessData).toHaveBeenCalledWith(datasetId, preprocessData)
      expect(result.datasetId).toBe(datasetId)
      expect(result.taskId).toBeDefined()
      expect(result.status).toBeDefined()
      expect(Array.isArray(result.methods)).toBe(true)
    })

    it('应该验证预处理方法', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const invalidData: PreprocessRequest = {
        methods: [], // 空方法列表
        parameters: {}
      }
      
      await expect(trainingDataService.preprocessData(datasetId, invalidData)).rejects.toThrow('预处理方法不能为空')
    })

    it('应该验证预处理方法有效性', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const invalidData: PreprocessRequest = {
        methods: ['invalid_method'], // 无效方法
        parameters: {}
      }
      
      await expect(trainingDataService.preprocessData(datasetId, invalidData)).rejects.toThrow('预处理方法 invalid_method 无效')
    })

    it('应该验证输出格式', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const invalidData: PreprocessRequest = {
        methods: ['normalization'],
        parameters: {},
        outputFormat: 'invalid_format' // 无效格式
      }
      
      await expect(trainingDataService.preprocessData(datasetId, invalidData)).rejects.toThrow('输出格式无效')
    })
  })

  describe('validateData', () => {
    it('应该成功验证数据', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const validationRules: ValidationRequest = {
        dataType: 'ACOUSTIC',
        requiredColumns: ['feature_1', 'feature_2'],
        qualityChecks: ['missing_values', 'duplicates']
      }
      
      const mockResult = mockValidationResult
      vi.mocked(trainingData.validateData).mockResolvedValue(mockResult)
      
      const result = await trainingDataService.validateData(datasetId, validationRules)
      
      expect(trainingData.validateData).toHaveBeenCalledWith(datasetId, validationRules)
      expect(result.datasetId).toBe(datasetId)
      expect(typeof result.isValid).toBe('boolean')
      expect(result.validationTime).toBeDefined()
      expect(result.results).toBeDefined()
      expect(Array.isArray(result.errors)).toBe(true)
      expect(Array.isArray(result.warnings)).toBe(true)
    })

    it('应该支持无验证规则', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const mockResult = mockValidationResult
      
      vi.mocked(trainingData.validateData).mockResolvedValue(mockResult)
      
      const result = await trainingDataService.validateData(datasetId)
      
      expect(trainingData.validateData).toHaveBeenCalledWith(datasetId, undefined)
      expect(result.datasetId).toBe(datasetId)
    })

    it('应该验证数据类型', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const invalidRules: ValidationRequest = {
        dataType: 'INVALID_TYPE' as any
      }
      
      await expect(trainingDataService.validateData(datasetId, invalidRules)).rejects.toThrow('数据类型无效')
    })
  })

  describe('updateData', () => {
    it('应该成功更新数据', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const updateData: UpdateDataRequest = {
        datasetDescription: '更新后的数据描述',
        tags: ['feature', 'acoustic', 'updated'],
        metadata: {
          source: 'bellhop',
          version: '1.1',
          updatedBy: 'researcher'
        }
      }
      
      const mockResponse = mockTrainingDataApi.updateData(datasetId, updateData).data
      vi.mocked(trainingData.updateData).mockResolvedValue(mockResponse)
      
      const result = await trainingDataService.updateData(datasetId, updateData)
      
      expect(trainingData.updateData).toHaveBeenCalledWith(datasetId, updateData)
      expect(result.datasetId).toBe(datasetId)
      expect(result.updatedAt).toBeDefined()
      expect(result.updatedBy).toBeDefined()
    })

    it('应该验证描述长度', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const invalidData: UpdateDataRequest = {
        datasetDescription: 'a'.repeat(1001) // 超长描述
      }
      
      await expect(trainingDataService.updateData(datasetId, invalidData)).rejects.toThrow('数据集描述不能超过1000个字符')
    })

    it('应该验证标签数量', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const invalidData: UpdateDataRequest = {
        tags: Array(21).fill('tag') // 超过20个标签
      }
      
      await expect(trainingDataService.updateData(datasetId, invalidData)).rejects.toThrow('标签数量不能超过20个')
    })
  })

  describe('deleteData', () => {
    it('应该成功删除数据', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const deleteParams: DeleteDataRequest = {
        reason: '数据已过期',
        deleteFile: true,
        deleteMetadata: false
      }
      
      const mockResponse = mockTrainingDataApi.deleteData(datasetId, deleteParams).data
      vi.mocked(trainingData.deleteData).mockResolvedValue(mockResponse)
      
      const result = await trainingDataService.deleteData(datasetId, deleteParams)
      
      expect(trainingData.deleteData).toHaveBeenCalledWith(datasetId, deleteParams)
      expect(result.datasetId).toBe(datasetId)
      expect(result.deletedAt).toBeDefined()
      expect(result.deletedBy).toBeDefined()
      expect(typeof result.fileDeleted).toBe('boolean')
      expect(typeof result.metadataPreserved).toBe('boolean')
    })

    it('应该支持无参数删除', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const mockResponse = mockTrainingDataApi.deleteData(datasetId).data
      
      vi.mocked(trainingData.deleteData).mockResolvedValue(mockResponse)
      
      const result = await trainingDataService.deleteData(datasetId)
      
      expect(trainingData.deleteData).toHaveBeenCalledWith(datasetId, undefined)
      expect(result.datasetId).toBe(datasetId)
    })
  })

  describe('batchOperation', () => {
    it('应该成功执行批量删除', async () => {
      const batchData: BatchOperationRequest = {
        operation: 'DELETE',
        datasetIds: ['e5f67890123456789012345678901234', 'f6789012345678901234567890123456'],
        parameters: {
          reason: '批量清理过期数据',
          deleteFile: true
        }
      }
      
      const mockResult = mockBatchOperationResult
      vi.mocked(trainingData.batchOperation).mockResolvedValue(mockResult)
      
      const result = await trainingDataService.batchOperation(batchData)
      
      expect(trainingData.batchOperation).toHaveBeenCalledWith(batchData)
      expect(result.operation).toBe('DELETE')
      expect(result.total).toBe(2)
      expect(result.success).toBe(2)
      expect(result.failed).toBe(0)
      expect(Array.isArray(result.results)).toBe(true)
    })

    it('应该验证操作类型', async () => {
      const invalidData: BatchOperationRequest = {
        operation: 'INVALID_OPERATION' as any,
        datasetIds: ['test1', 'test2']
      }
      
      await expect(trainingDataService.batchOperation(invalidData)).rejects.toThrow('操作类型无效')
    })

    it('应该验证数据集ID列表', async () => {
      const invalidData: BatchOperationRequest = {
        operation: 'DELETE',
        datasetIds: [] // 空列表
      }
      
      await expect(trainingDataService.batchOperation(invalidData)).rejects.toThrow('数据集ID列表不能为空')
    })

    it('应该验证批量操作数量限制', async () => {
      const invalidData: BatchOperationRequest = {
        operation: 'DELETE',
        datasetIds: Array(101).fill('a1b2c3d4e5f678901234567890123456') // 超过100个
      }
      
      await expect(trainingDataService.batchOperation(invalidData)).rejects.toThrow('批量操作数据集数量不能超过100个')
    })

    it('应该验证数据集ID格式', async () => {
      const invalidData: BatchOperationRequest = {
        operation: 'DELETE',
        datasetIds: ['invalid-id', 'a1b2c3d4e5f678901234567890123456']
      }
      
      await expect(trainingDataService.batchOperation(invalidData)).rejects.toThrow('数据集ID invalid-id 格式不正确')
    })
  })

  describe('getDataStatistics', () => {
    it('应该成功获取数据统计', async () => {
      const params: DataStatisticsParams = { 
        vmId: 'a1b2c3d4e5f678901234567890123456',
        dataType: 'ACOUSTIC'
      }
      
      const mockStats = mockDataStatistics
      vi.mocked(trainingData.getDataStatistics).mockResolvedValue(mockStats)
      
      const result = await trainingDataService.getDataStatistics(params)
      
      expect(trainingData.getDataStatistics).toHaveBeenCalledWith(params)
      expect(result.totalCount).toBeGreaterThanOrEqual(0)
      expect(result.totalSize).toBeGreaterThanOrEqual(0)
      expect(typeof result.dataTypeDistribution).toBe('object')
      expect(typeof result.statusDistribution).toBe('object')
      expect(typeof result.vmDistribution).toBe('object')
      expect(result.uploadTrend).toBeDefined()
      expect(Array.isArray(result.topDataTypes)).toBe(true)
    })

    it('应该支持无参数统计', async () => {
      const mockStats = mockDataStatistics
      vi.mocked(trainingData.getDataStatistics).mockResolvedValue(mockStats)
      
      const result = await trainingDataService.getDataStatistics()
      
      expect(trainingData.getDataStatistics).toHaveBeenCalledWith({})
      expect(result.totalCount).toBeGreaterThanOrEqual(0)
    })

    it('应该验证虚拟机ID格式', async () => {
      const invalidParams: DataStatisticsParams = { vmId: 'invalid-id' }
      
      await expect(trainingDataService.getDataStatistics(invalidParams)).rejects.toThrow('虚拟机ID格式不正确')
    })

    it('应该验证数据类型参数', async () => {
      const invalidParams: DataStatisticsParams = { dataType: 'INVALID_TYPE' as any }
      
      await expect(trainingDataService.getDataStatistics(invalidParams)).rejects.toThrow('数据类型参数无效')
    })

    it('应该验证日期范围', async () => {
      const invalidParams: DataStatisticsParams = {
        startDate: '2024-01-02',
        endDate: '2024-01-01'
      }
      
      await expect(trainingDataService.getDataStatistics(invalidParams)).rejects.toThrow('开始日期不能晚于结束日期')
    })
  })

  describe('exportData', () => {
    it('应该成功导出数据', async () => {
      const exportData: ExportDataRequest = {
        exportType: 'CSV',
        filters: {
          dataType: 'ACOUSTIC',
          vmId: 'a1b2c3d4e5f678901234567890123456',
          status: 'READY'
        },
        fields: ['datasetId', 'datasetDescription', 'datasetType'],
        format: 'ZIP'
      }
      
      const mockTask = mockExportTask
      vi.mocked(trainingData.exportData).mockResolvedValue(mockTask)
      
      const result = await trainingDataService.exportData(exportData)
      
      expect(trainingData.exportData).toHaveBeenCalledWith(exportData)
      expect(result.taskId).toBeDefined()
      expect(result.status).toBeDefined()
      expect(result.format).toBe('CSV')
      expect(result.startedAt).toBeDefined()
      expect(result.downloadUrl).toBeDefined()
    })

    it('应该验证导出类型', async () => {
      const invalidData: ExportDataRequest = {
        exportType: 'INVALID_TYPE' as any
      }
      
      await expect(trainingDataService.exportData(invalidData)).rejects.toThrow('导出类型无效')
    })

    it('应该验证导出格式', async () => {
      const invalidData: ExportDataRequest = {
        exportType: 'CSV',
        format: 'INVALID_FORMAT' as any
      }
      
      await expect(trainingDataService.exportData(invalidData)).rejects.toThrow('导出格式无效')
    })

    it('应该验证过滤条件中的虚拟机ID', async () => {
      const invalidData: ExportDataRequest = {
        exportType: 'CSV',
        filters: {
          vmId: 'invalid-id'
        }
      }
      
      await expect(trainingDataService.exportData(invalidData)).rejects.toThrow('过滤虚拟机ID格式不正确')
    })

    it('应该验证过滤条件中的数据类型', async () => {
      const invalidData: ExportDataRequest = {
        exportType: 'CSV',
        filters: {
          dataType: 'INVALID_TYPE' as any
        }
      }
      
      await expect(trainingDataService.exportData(invalidData)).rejects.toThrow('过滤数据类型无效')
    })

    it('应该验证过滤条件中的状态', async () => {
      const invalidData: ExportDataRequest = {
        exportType: 'CSV',
        filters: {
          status: 'INVALID_STATUS' as any
        }
      }
      
      await expect(trainingDataService.exportData(invalidData)).rejects.toThrow('过滤状态无效')
    })
  })

  describe('错误处理', () => {
    it('应该正确处理API响应错误', async () => {
      const datasetId = 'e5f67890123456789012345678901234'
      const error = {
        response: {
          data: {
            message: 'API错误信息'
          }
        }
      }
      
      vi.mocked(trainingData.getDataDetail).mockRejectedValue(error)
      
      await expect(trainingDataService.getDataDetail(datasetId)).rejects.toThrow('获取训练数据详情失败 (ID: e5f67890123456789012345678901234): API错误信息')
    })

    it('应该正确处理通用错误', async () => {
      const datasetId = 'e5f67890123456789012345678901234'
      const error = new Error('网络连接失败')
      
      vi.mocked(trainingData.getDataDetail).mockRejectedValue(error)
      
      await expect(trainingDataService.getDataDetail(datasetId)).rejects.toThrow('获取训练数据详情失败 (ID: e5f67890123456789012345678901234): 网络连接失败')
    })

    it('应该处理未知错误', async () => {
      const datasetId = 'e5f67890123456789012345678901234'
      vi.mocked(trainingData.getDataDetail).mockRejectedValue('未知错误')
      
      await expect(trainingDataService.getDataDetail(datasetId)).rejects.toThrow('获取训练数据详情失败')
    })
  })

  describe('数据转换', () => {
    it('应该正确转换数据列表', async () => {
      const params: DataListParams = { page: 1, size: 10 }
      const mockResponse = mockTrainingDataApi.getDataList(params).data
      
      vi.mocked(trainingData.getDataList).mockResolvedValue(mockResponse)
      
      const result = await trainingDataService.getDataList(params)
      
      expect(result.dataList).toEqual(mockResponse.dataList)
      result.dataList.forEach(item => {
        expect(item.datasetId).toBeDefined()
        expect(item.datasetDescription).toBeDefined()
        expect(item.datasetType).toBeDefined()
        expect(item.vmId).toBeDefined()
        expect(item.status).toBeDefined()
      })
    })

    it('应该正确转换数据详情', async () => {
      const datasetId = mockTrainingDatasets[0].datasetId
      const mockDetail = mockDatasetDetail
      
      vi.mocked(trainingData.getDataDetail).mockResolvedValue(mockDetail)
      
      const result = await trainingDataService.getDataDetail(datasetId)
      
      expect(result.datasetId).toBe(mockDetail.datasetId)
      expect(result.metadata).toEqual(mockDetail.metadata)
      expect(result.validation).toEqual(mockDetail.validation)
      expect(result.preprocessing).toEqual(mockDetail.preprocessing)
    })

    it('应该正确转换统计数据', async () => {
      const mockStats = mockDataStatistics
      vi.mocked(trainingData.getDataStatistics).mockResolvedValue(mockStats)
      
      const result = await trainingDataService.getDataStatistics()
      
      expect(result).toEqual(mockStats)
      expect(result.totalCount).toBe(mockStats.totalCount)
      expect(result.totalSize).toBe(mockStats.totalSize)
      expect(result.dataTypeDistribution).toEqual(mockStats.dataTypeDistribution)
      expect(result.statusDistribution).toEqual(mockStats.statusDistribution)
      expect(result.vmDistribution).toEqual(mockStats.vmDistribution)
      expect(result.uploadTrend).toEqual(mockStats.uploadTrend)
      expect(result.topDataTypes).toEqual(mockStats.topDataTypes)
    })
  })

  describe('参数验证', () => {
    it('应该验证数据集ID格式 - UUID', () => {
      const validIds = ['e5f67890123456789012345678901234', 'a1b2c3d4e5f678901234567890123456']
      const invalidIds = ['invalid-id', '12345', 'e5f67890123456789012345678901234567', '']
      
      validIds.forEach(id => {
        expect(() => trainingDataService['validateDatasetId'](id)).not.toThrow()
      })
      
      invalidIds.forEach(id => {
        expect(() => trainingDataService['validateDatasetId'](id)).toThrow()
      })
    })

    it('应该验证FormData结构', () => {
      const validFormData = new FormData()
      validFormData.append('vmId', 'a1b2c3d4e5f678901234567890123456')
      validFormData.append('dataType', 'ACOUSTIC')
      validFormData.append('file', new File(['test'], 'test.wav'))
      
      expect(() => trainingDataService['validateUploadFile'](validFormData)).not.toThrow()
      
      const invalidFormData = new FormData()
      expect(() => trainingDataService['validateUploadFile'](invalidFormData)).toThrow()
    })

    it('应该验证预处理方法有效性', async () => {
      const datasetId = 'e5f67890123456789012345678901234'
      
      const validMethods = ['normalization', 'feature_selection', 'outlier_removal', 'data_cleaning', 'dimensionality_reduction']
      const invalidMethods = ['invalid_method', 'unknown_process']
      
      for (const method of validMethods) {
        const validData: PreprocessRequest = {
          methods: [method],
          parameters: {}
        }
        
        const mockTask = mockPreprocessTask
        vi.mocked(trainingData.preprocessData).mockResolvedValue(mockTask)
        
        await expect(trainingDataService.preprocessData(datasetId, validData)).resolves.toBeDefined()
      }
      
      for (const method of invalidMethods) {
        const invalidData: PreprocessRequest = {
          methods: [method],
          parameters: {}
        }
        
        await expect(trainingDataService.preprocessData(datasetId, invalidData)).rejects.toThrow(`预处理方法 ${method} 无效`)
      }
    })

    it('应该验证导出字段格式', () => {
      const validData: ExportDataRequest = {
        exportType: 'CSV',
        fields: ['datasetId', 'datasetDescription']
      }
      
      expect(() => trainingDataService['validateExportDataRequest'](validData)).not.toThrow()
      
      const invalidData: ExportDataRequest = {
        exportType: 'CSV',
        fields: 'invalid_format' as any
      }
      
      expect(() => trainingDataService['validateExportDataRequest'](invalidData)).toThrow('导出字段必须为数组格式')
    })
  })
})
