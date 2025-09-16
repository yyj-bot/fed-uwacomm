/**
 * 训练数据API Mock数据
 * 提供与接口文档一致的Mock数据，用于开发和测试
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { ApiResponse } from '@/types'
import type { 
  DataStatistics,
  PreprocessTask,
  ValidationResult,
  BatchOperationResult,
  ExportTask,
  DatasetDetail
} from '@/api/training-data'

// ==================== Mock数据生成工具 ====================

/**
 * 生成随机数据集ID（32位十六进制）
 */
const generateDatasetId = (): string => {
  return Array.from({ length: 32 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 生成随机用户ID（32位十六进制）
 */
const generateUserId = (): string => {
  return Array.from({ length: 32 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 生成随机虚拟机ID（32位十六进制）
 */
const generateVMId = (): string => {
  return Array.from({ length: 32 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 生成随机时间戳
 */
const generateTimestamp = (daysAgo: number = 0): string => {
  const date = new Date()
  date.setDate(date.getDate() - daysAgo)
  return date.toISOString()
}

/**
 * 生成随机文件大小
 */
const generateFileSize = (): number => {
  return Math.floor(Math.random() * 50 * 1024 * 1024) + 1024 * 1024 // 1MB - 50MB
}

/**
 * 生成随机进度
 */
const generateProgress = (): number => {
  return Math.floor(Math.random() * 101)
}

// ==================== Mock训练数据 ====================

/**
 * Mock训练数据集列表
 */
export const mockTrainingDatasets = [
  {
    datasetId: 'e5f67890123456789012345678901234',
    datasetDescription: '水声传播特征数据',
    datasetType: 'ACOUSTIC',
    vmId: 'a1b2c3d4e5f678901234567890123456',
    status: 'READY',
    tags: ['feature', 'acoustic']
  },
  {
    datasetId: 'f6789012345678901234567890123456',
    datasetDescription: '声学传播环境参数配置',
    datasetType: 'ENVIRONMENT',
    vmId: 'a1b2c3d4e5f678901234567890123456',
    status: 'READY',
    tags: ['environment', 'acoustic']
  },
  {
    datasetId: 'g7890123456789012345678901234567',
    datasetDescription: '深度学习模型文件',
    datasetType: 'MODEL',
    vmId: 'b2c3d4e5f67890123456789012345678',
    status: 'PROCESSING',
    tags: ['model', 'deep_learning']
  },
  {
    datasetId: 'h8901234567890123456789012345678',
    datasetDescription: '特征工程处理结果',
    datasetType: 'FEATURE',
    vmId: 'c3d4e5f6789012345678901234567890',
    status: 'VALIDATING',
    tags: ['feature', 'processed']
  }
]

/**
 * Mock数据详情
 */
export const mockDatasetDetail: DatasetDetail = {
  datasetId: 'e5f67890123456789012345678901234',
  datasetDescription: '水声传播特征数据',
  datasetType: 'ACOUSTIC',
  vmId: 'a1b2c3d4e5f678901234567890123456',
  status: 'READY',
  uploadTime: '2024-01-01T10:00:00.000Z',
  uploadedBy: 'f6789012345678901234567890123456',
  tags: ['feature', 'acoustic'],
  metadata: {
    source: 'bellhop',
    version: '1.0',
    columns: 103,
    rows: 318,
    features: ['feature_1', 'feature_2', 'feature_3']
  },
  validation: {
    isValid: true,
    validationTime: '2024-01-01T10:05:00.000Z',
    errors: [],
    warnings: []
  },
  preprocessing: {
    isProcessed: true,
    processTime: '2024-01-01T10:10:00.000Z',
    methods: ['normalization', 'feature_selection'],
    parameters: {
      normalization: 'standard_scaler',
      feature_selection: 'variance_threshold'
    }
  }
}

/**
 * Mock数据统计
 */
export const mockDataStatistics: DataStatistics = {
  totalCount: 1000,
  totalSize: 1073741824, // 1GB
  dataTypeDistribution: {
    'ACOUSTIC': 500,
    'ENVIRONMENT': 300,
    'MODEL': 150,
    'FEATURE': 40,
    'OTHER': 10
  },
  statusDistribution: {
    'READY': 800,
    'PROCESSING': 150,
    'VALIDATING': 30,
    'ERROR': 20
  },
  vmDistribution: {
    'a1b2c3d4e5f678901234567890123456': {
      count: 300,
      size: 322122547
    },
    'b2c3d4e5f67890123456789012345678': {
      count: 400,
      size: 429496730
    },
    'c3d4e5f6789012345678901234567890': {
      count: 300,
      size: 322122547
    }
  },
  uploadTrend: {
    last7Days: [100, 120, 80, 150, 200, 180, 160],
    last30Days: [3000, 3200, 2800, 3500, 4000, 3800, 3600]
  },
  topDataTypes: [
    {
      dataType: 'ACOUSTIC',
      count: 500,
      percentage: 50.0
    },
    {
      dataType: 'ENVIRONMENT',
      count: 300,
      percentage: 30.0
    },
    {
      dataType: 'MODEL',
      count: 150,
      percentage: 15.0
    }
  ]
}

/**
 * Mock预处理任务
 */
export const mockPreprocessTask: PreprocessTask = {
  datasetId: 'e5f67890123456789012345678901234',
  taskId: 'preprocess_1234567890',
  status: 'PROCESSING',
  methods: ['normalization', 'feature_selection', 'outlier_removal'],
  startedAt: '2024-01-01T11:00:00.000Z',
  estimatedTime: 300
}

/**
 * Mock验证结果
 */
export const mockValidationResult: ValidationResult = {
  datasetId: 'e5f67890123456789012345678901234',
  isValid: true,
  validationTime: '2024-01-01T11:30:00.000Z',
  results: {
    totalRows: 318,
    validRows: 315,
    invalidRows: 3,
    missingValues: 2,
    duplicates: 1,
    outliers: 0
  },
  errors: [
    {
      row: 45,
      column: 'feature_1',
      error: 'Value out of range',
      value: 150.5
    }
  ],
  warnings: [
    {
      type: 'missing_values',
      count: 2,
      columns: ['feature_2']
    }
  ]
}

/**
 * Mock批量操作结果
 */
export const mockBatchOperationResult: BatchOperationResult = {
  operation: 'DELETE',
  total: 2,
  success: 2,
  failed: 0,
  results: [
    {
      datasetId: 'e5f67890123456789012345678901234',
      status: 'SUCCESS',
      message: '删除成功'
    },
    {
      datasetId: 'f6789012345678901234567890123456',
      status: 'SUCCESS',
      message: '删除成功'
    }
  ]
}

/**
 * Mock导出任务
 */
export const mockExportTask: ExportTask = {
  taskId: 'export_1234567890',
  status: 'PROCESSING',
  format: 'CSV',
  startedAt: '2024-01-01T13:00:00.000Z',
  estimatedTime: 60,
  downloadUrl: '/api/training-data/export/download/export_1234567890'
}

// ==================== Mock API响应 ====================

/**
 * 创建成功响应
 */
export const createSuccessResponse = <T>(data: T, message: string = '操作成功'): ApiResponse<T> => ({
  code: 200,
  message,
  data
})

/**
 * 创建错误响应
 */
export const createErrorResponse = <T = null>(code: number, message: string, data?: T): ApiResponse<T> => ({
  code,
  message,
  data: data || null as T
})

// ==================== Mock服务方法 ====================

/**
 * Mock训练数据API服务
 */
export const mockTrainingDataApi = {
  /**
   * Mock文件上传
   */
  uploadFile: (formData: FormData): ApiResponse<{
    datasetId: string
    datasetDescription: string
    datasetType: string
    vmId: string
    status: string
    uploadTime: string
    uploadedBy: string
    progress: number
  }> => {
    // 参数验证
    const vmId = formData.get('vmId') as string
    const dataType = formData.get('dataType') as string
    const file = formData.get('file') as File

    if (!vmId || vmId.trim().length === 0) {
      return createErrorResponse(400, '虚拟机ID不能为空', {
        errors: [{
          field: 'vmId',
          message: '虚拟机ID不能为空'
        }]
      } as any)
    }

    if (!dataType) {
      return createErrorResponse(400, '数据类型不能为空', {
        errors: [{
          field: 'dataType',
          message: '数据类型不能为空'
        }]
      } as any)
    }

    const validDataTypes = ['ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'FEATURE', 'OTHER']
    if (!validDataTypes.includes(dataType)) {
      return createErrorResponse(400, '数据类型无效', {
        errors: [{
          field: 'dataType',
          message: '数据类型无效'
        }]
      } as any)
    }

    if (!file) {
      return createErrorResponse(400, '上传文件不能为空', {
        errors: [{
          field: 'file',
          message: '上传文件不能为空'
        }]
      } as any)
    }

    // 文件大小检查（100MB限制）
    if (file.size > 100 * 1024 * 1024) {
      return createErrorResponse(413, '文件过大', {
        errors: [{
          field: 'file',
          message: '文件大小不能超过100MB'
        }]
      } as any)
    }

    const newDatasetId = generateDatasetId()
    const uploadedBy = generateUserId()
    const description = formData.get('description') as string || `${dataType}数据文件`

    return createSuccessResponse({
      datasetId: newDatasetId,
      datasetDescription: description,
      datasetType: dataType,
      vmId: vmId,
      status: 'UPLOADING',
      uploadTime: generateTimestamp(),
      uploadedBy,
      progress: 0
    }, '文件上传成功')
  },

  /**
   * Mock文本上传
   */
  uploadText: (textData: any): ApiResponse<{
    datasetId: string
    datasetDescription: string
    datasetType: string
    vmId: string
    status: string
    uploadTime: string
    uploadedBy: string
  }> => {
    // 参数验证
    if (!textData.vmId || textData.vmId.trim().length === 0) {
      return createErrorResponse(400, '虚拟机ID不能为空', {
        errors: [{
          field: 'vmId',
          message: '虚拟机ID不能为空'
        }]
      } as any)
    }

    if (!textData.dataType) {
      return createErrorResponse(400, '数据类型不能为空', {
        errors: [{
          field: 'dataType',
          message: '数据类型不能为空'
        }]
      } as any)
    }

    if (!textData.title || textData.title.trim().length === 0) {
      return createErrorResponse(400, '标题不能为空', {
        errors: [{
          field: 'title',
          message: '标题不能为空'
        }]
      } as any)
    }

    if (!textData.content || textData.content.trim().length === 0) {
      return createErrorResponse(400, '内容不能为空', {
        errors: [{
          field: 'content',
          message: '内容不能为空'
        }]
      } as any)
    }

    const newDatasetId = generateDatasetId()
    const uploadedBy = generateUserId()

    return createSuccessResponse({
      datasetId: newDatasetId,
      datasetDescription: textData.description || textData.title,
      datasetType: textData.dataType,
      vmId: textData.vmId,
      status: 'READY',
      uploadTime: generateTimestamp(),
      uploadedBy
    }, '文本信息上传成功')
  },

  /**
   * Mock获取数据列表
   */
  getDataList: (params: {
    page?: number
    size?: number
    vmId?: string
    dataType?: string
    status?: string
    keyword?: string
    startDate?: string
    endDate?: string
    tags?: string
  } = {}): ApiResponse<{
    total: number
    page: number
    size: number
    dataList: Array<{
      datasetId: string
      datasetDescription: string
      datasetType: string
      vmId: string
      status: string
      tags?: string[]
    }>
  }> => {
    const { page = 1, size = 20, vmId, dataType, status, keyword } = params
    
    let filteredData = [...mockTrainingDatasets]
    
    // 应用过滤条件
    if (vmId) {
      filteredData = filteredData.filter(item => item.vmId === vmId)
    }
    if (dataType) {
      filteredData = filteredData.filter(item => item.datasetType === dataType)
    }
    if (status) {
      filteredData = filteredData.filter(item => item.status === status)
    }
    if (keyword) {
      filteredData = filteredData.filter(item => 
        item.datasetDescription.toLowerCase().includes(keyword.toLowerCase())
      )
    }
    
    // 分页处理
    const total = filteredData.length
    const startIndex = (page - 1) * size
    const endIndex = startIndex + size
    const dataList = filteredData.slice(startIndex, endIndex)
    
    return createSuccessResponse({
      total,
      page,
      size,
      dataList
    }, '查询成功')
  },

  /**
   * Mock获取数据详情
   */
  getDataDetail: (datasetId: string): ApiResponse<DatasetDetail> => {
    if (!mockTrainingDatasets.some(item => item.datasetId === datasetId)) {
      return createErrorResponse<DatasetDetail>(404, '数据不存在')
    }

    return createSuccessResponse(mockDatasetDetail, '查询成功')
  },

  /**
   * Mock数据预处理
   */
  preprocessData: (datasetId: string, preprocessData: any): ApiResponse<PreprocessTask> => {
    if (!mockTrainingDatasets.some(item => item.datasetId === datasetId)) {
      return createErrorResponse<PreprocessTask>(404, '数据不存在')
    }

    // 验证预处理方法
    if (!preprocessData.methods || !Array.isArray(preprocessData.methods) || preprocessData.methods.length === 0) {
      return createErrorResponse(400, '预处理方法不能为空', {
        errors: [{
          field: 'methods',
          message: '预处理方法不能为空'
        }]
      } as any)
    }

    return createSuccessResponse({
      ...mockPreprocessTask,
      datasetId,
      methods: preprocessData.methods
    }, '预处理任务已启动')
  },

  /**
   * Mock数据验证
   */
  validateData: (datasetId: string, validationRules?: any): ApiResponse<ValidationResult> => {
    if (!mockTrainingDatasets.some(item => item.datasetId === datasetId)) {
      return createErrorResponse<ValidationResult>(404, '数据不存在')
    }

    return createSuccessResponse({
      ...mockValidationResult,
      datasetId
    }, '验证完成')
  },

  /**
   * Mock更新数据
   */
  updateData: (datasetId: string, updateData: any): ApiResponse<{
    datasetId: string
    updatedAt: string
    updatedBy: string
  }> => {
    if (!mockTrainingDatasets.some(item => item.datasetId === datasetId)) {
      return createErrorResponse(404, '数据不存在', null as any)
    }

    return createSuccessResponse({
      datasetId,
      updatedAt: generateTimestamp(),
      updatedBy: 'researcher'
    }, '数据更新成功')
  },

  /**
   * Mock删除数据
   */
  deleteData: (datasetId: string, deleteParams?: any): ApiResponse<{
    datasetId: string
    deletedAt: string
    deletedBy: string
    fileDeleted: boolean
    metadataPreserved: boolean
  }> => {
    if (!mockTrainingDatasets.some(item => item.datasetId === datasetId)) {
      return createErrorResponse(404, '数据不存在', null as any)
    }

    const deleteFile = deleteParams?.deleteFile !== false
    const deleteMetadata = deleteParams?.deleteMetadata !== false

    return createSuccessResponse({
      datasetId,
      deletedAt: generateTimestamp(),
      deletedBy: 'admin',
      fileDeleted: deleteFile,
      metadataPreserved: !deleteMetadata
    }, '数据删除成功')
  },

  /**
   * Mock批量操作
   */
  batchOperation: (batchData: any): ApiResponse<BatchOperationResult> => {
    // 验证操作类型
    if (!batchData.operation) {
      return createErrorResponse(400, '操作类型不能为空', {
        errors: [{
          field: 'operation',
          message: '操作类型不能为空'
        }]
      } as any)
    }

    const validOperations = ['DELETE', 'UPDATE', 'VALIDATE']
    if (!validOperations.includes(batchData.operation)) {
      return createErrorResponse(400, '操作类型无效', {
        errors: [{
          field: 'operation',
          message: '操作类型无效'
        }]
      } as any)
    }

    // 验证数据集ID列表
    if (!batchData.datasetIds || !Array.isArray(batchData.datasetIds) || batchData.datasetIds.length === 0) {
      return createErrorResponse(400, '数据集ID列表不能为空', {
        errors: [{
          field: 'datasetIds',
          message: '数据集ID列表不能为空'
        }]
      } as any)
    }

    return createSuccessResponse({
      ...mockBatchOperationResult,
      operation: batchData.operation,
      total: batchData.datasetIds.length,
      success: batchData.datasetIds.length,
      results: batchData.datasetIds.map((id: string) => ({
        datasetId: id,
        status: 'SUCCESS',
        message: '操作成功'
      }))
    }, '批量操作成功')
  },

  /**
   * Mock获取数据统计
   */
  getDataStatistics: (params: any = {}): ApiResponse<DataStatistics> => {
    return createSuccessResponse(mockDataStatistics, '统计查询成功')
  },

  /**
   * Mock数据导出
   */
  exportData: (exportData: any): ApiResponse<ExportTask> => {
    // 验证导出类型
    if (!exportData.exportType) {
      return createErrorResponse(400, '导出类型不能为空', {
        errors: [{
          field: 'exportType',
          message: '导出类型不能为空'
        }]
      } as any)
    }

    const validExportTypes = ['CSV', 'JSON', 'EXCEL']
    if (!validExportTypes.includes(exportData.exportType)) {
      return createErrorResponse(400, '导出类型无效', {
        errors: [{
          field: 'exportType',
          message: '导出类型无效'
        }]
      } as any)
    }

    return createSuccessResponse({
      ...mockExportTask,
      format: exportData.exportType
    }, '导出任务已启动')
  }
}

// ==================== 导出Mock数据 ====================

export default mockTrainingDataApi
