import { createApiInstance } from './base'
import type { 
  ApiResponse, 
  TrainingDataset,
  PaginatedResponse,
  PaginationParams
} from '@/types'

// 创建训练数据API实例
const trainingDataApiInstance = createApiInstance('http://localhost:8080/api/training-data')

// 数据统计类型
interface DataStatistics {
  totalCount: number
  totalSize: number
  dataTypeDistribution: Record<string, number>
  statusDistribution: Record<string, number>
  vmDistribution: Record<string, {
    count: number
    size: number
  }>
  uploadTrend: {
    last7Days: number[]
    last30Days: number[]
  }
  topDataTypes: Array<{
    dataType: string
    count: number
    percentage: number
  }>
}

// 预处理任务类型
interface PreprocessTask {
  datasetId: string
  taskId: string
  status: string
  methods: string[]
  startedAt: string
  estimatedTime: number
}

// 验证结果类型
interface ValidationResult {
  datasetId: string
  isValid: boolean
  validationTime: string
  results: {
    totalRows: number
    validRows: number
    invalidRows: number
    missingValues: number
    duplicates: number
    outliers: number
  }
  errors: Array<{
    row: number
    column: string
    error: string
    value: unknown
  }>
  warnings: Array<{
    type: string
    count: number
    columns: string[]
  }>
}

// 批量操作结果类型
interface BatchOperationResult {
  operation: string
  total: number
  success: number
  failed: number
  results: Array<{
    datasetId: string
    status: string
    message: string
  }>
}

// 导出任务类型
interface ExportTask {
  taskId: string
  status: string
  format: string
  startedAt: string
  estimatedTime: number
  downloadUrl: string
}

// 数据详情类型 (扩展版本)
interface DatasetDetail extends TrainingDataset {
  metadata?: {
    source?: string
    version?: string
    columns?: number
    rows?: number
    features?: string[]
    [key: string]: unknown
  }
  validation?: {
    isValid: boolean
    validationTime: string
    errors: unknown[]
    warnings: unknown[]
  }
  preprocessing?: {
    isProcessed: boolean
    processTime: string
    methods: string[]
    parameters: Record<string, unknown>
  }
}

// ==================== 训练数据管理API ====================
export const trainingData = {
  // ==================== 3.1 文件上传接口 ====================
  async uploadFile(formData: FormData): Promise<{
    datasetId: string
    datasetDescription: string
    datasetType: string
    vmId: string
    status: string
    uploadTime: string
    uploadedBy: string
    progress: number
  }> {
    const response = await trainingDataApiInstance.post<ApiResponse<{
      datasetId: string
      datasetDescription: string
      datasetType: string
      vmId: string
      status: string
      uploadTime: string
      uploadedBy: string
      progress: number
    }>>('/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response.data.data
  },

  // ==================== 3.2 文本信息上传接口 ====================
  async uploadText(textData: {
    vmId: string
    dataType: string
    title: string
    content: string
    description?: string
    tags?: string[]
    metadata?: Record<string, unknown>
  }): Promise<{
    datasetId: string
    datasetDescription: string
    datasetType: string
    vmId: string
    status: string
    uploadTime: string
    uploadedBy: string
  }> {
    const response = await trainingDataApiInstance.post<ApiResponse<{
      datasetId: string
      datasetDescription: string
      datasetType: string
      vmId: string
      status: string
      uploadTime: string
      uploadedBy: string
    }>>('/text', textData)
    return response.data.data
  },

  // ==================== 3.3 数据列表查询接口 ====================
  async getDataList(params: PaginationParams & {
    vmId?: string
    dataType?: string
    status?: string
    keyword?: string
    startDate?: string
    endDate?: string
    tags?: string
  } = {}): Promise<{
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
  }> {
    const response = await trainingDataApiInstance.get<ApiResponse<{
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
    }>>('/', { params })
    return response.data.data
  },

  // ==================== 3.4 数据详情查询接口 ====================
  async getDataDetail(datasetId: string): Promise<DatasetDetail> {
    const response = await trainingDataApiInstance.get<ApiResponse<DatasetDetail>>(`/${datasetId}`)
    return response.data.data
  },

  // ==================== 3.5 数据下载接口 ====================
  async downloadData(datasetId: string): Promise<Blob> {
    const response = await trainingDataApiInstance.get(`/${datasetId}/download`, {
      responseType: 'blob'
    })
    return response.data
  },

  // ==================== 3.6 数据预处理接口 ====================
  async preprocessData(datasetId: string, preprocessData: {
    methods: string[]
    parameters: Record<string, unknown>
    outputFormat?: string
  }): Promise<PreprocessTask> {
    const response = await trainingDataApiInstance.post<ApiResponse<PreprocessTask>>(`/${datasetId}/preprocess`, preprocessData)
    return response.data.data
  },

  // ==================== 3.7 数据验证接口 ====================
  async validateData(datasetId: string, validationRules?: {
    dataType?: string
    requiredColumns?: string[]
    dataTypes?: Record<string, string>
    constraints?: Record<string, {
      min?: number
      max?: number
      notNull?: boolean
    }>
    qualityChecks?: string[]
  }): Promise<ValidationResult> {
    const response = await trainingDataApiInstance.post<ApiResponse<ValidationResult>>(`/${datasetId}/validate`, { validationRules })
    return response.data.data
  },

  // ==================== 3.8 数据更新接口 ====================
  async updateData(datasetId: string, updateData: {
    datasetDescription?: string
    tags?: string[]
    metadata?: Record<string, unknown>
  }): Promise<{
    datasetId: string
    updatedAt: string
    updatedBy: string
  }> {
    const response = await trainingDataApiInstance.put<ApiResponse<{
      datasetId: string
      updatedAt: string
      updatedBy: string
    }>>(`/${datasetId}`, updateData)
    return response.data.data
  },

  // ==================== 3.9 数据删除接口 ====================
  async deleteData(datasetId: string, deleteParams?: {
    reason?: string
    deleteFile?: boolean
    deleteMetadata?: boolean
  }): Promise<{
    datasetId: string
    deletedAt: string
    deletedBy: string
    fileDeleted: boolean
    metadataPreserved: boolean
  }> {
    const config = deleteParams ? { data: deleteParams } : undefined
    const response = await trainingDataApiInstance.delete<ApiResponse<{
      datasetId: string
      deletedAt: string
      deletedBy: string
      fileDeleted: boolean
      metadataPreserved: boolean
    }>>(`/${datasetId}`, config)
    return response.data.data
  },

  // ==================== 3.10 批量数据操作接口 ====================
  async batchOperation(batchData: {
    operation: 'DELETE' | 'UPDATE' | 'VALIDATE'
    datasetIds: string[]
    parameters?: Record<string, unknown>
  }): Promise<BatchOperationResult> {
    const response = await trainingDataApiInstance.post<ApiResponse<BatchOperationResult>>('/batch', batchData)
    return response.data.data
  },

  // ==================== 3.11 数据统计接口 ====================
  async getDataStatistics(params: {
    vmId?: string
    dataType?: string
    startDate?: string
    endDate?: string
  } = {}): Promise<DataStatistics> {
    const response = await trainingDataApiInstance.get<ApiResponse<DataStatistics>>('/statistics', { params })
    return response.data.data
  },

  // ==================== 3.12 数据导出接口 ====================
  async exportData(exportData: {
    exportType: 'CSV' | 'JSON' | 'EXCEL'
    filters?: {
      dataType?: string
      vmId?: string
      status?: string
      startTime?: string
      endTime?: string
    }
    fields?: string[]
    format?: 'ZIP' | 'TAR'
  }): Promise<ExportTask> {
    const response = await trainingDataApiInstance.post<ApiResponse<ExportTask>>('/export', exportData)
    return response.data.data
  },
} as const

// 使用命名导出以保持一致性 

// 导出类型定义
export type {
  DataStatistics,
  PreprocessTask,
  ValidationResult,
  BatchOperationResult,
  ExportTask,
  DatasetDetail
}