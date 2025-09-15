/**
 * Training Data Store Mock 数据
 * 用于测试训练数据管理相关功能的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { TrainingDataset, DataStatistics } from '@/types'
import type { 
  UploadFileRequest, 
  PreprocessRequest,
  UpdateDataRequest,
  BatchOperationRequest,
  ExportDataRequest
} from '@/services'

// ==================== 模拟训练数据集 ====================

export const mockTrainingDataset: TrainingDataset = {
  datasetId: 'dataset-001',
  datasetDescription: 'MNIST手写数字识别数据集',
  datasetType: 'ACOUSTIC',
  vmId: 'vm-001',
  status: 'READY',
  uploadTime: '2024-01-15T08:00:00Z',
  uploadedBy: 'user-001',
  tags: ['图像分类', '手写数字', '经典数据集'],
  metadata: {
    sampleCount: 70000,
    labelCount: 10,
    fileSize: 52428800,
    format: 'PNG',
    imageSize: '28x28',
    channels: 1
  },
  progress: 100
}

export const mockTrainingDataList: TrainingDataset[] = [
  mockTrainingDataset,
  {
    datasetId: 'dataset-002',
    datasetDescription: 'CIFAR-10图像分类数据集',
    datasetType: 'ENVIRONMENT',
    vmId: 'vm-002',
    status: 'PROCESSING',
    uploadTime: '2024-01-15T09:00:00Z',
    uploadedBy: 'user-002',
    tags: ['图像分类', '彩色图像', '多类别'],
    metadata: {
      sampleCount: 60000,
      labelCount: 10,
      fileSize: 178257920,
      format: 'PNG',
      imageSize: '32x32',
      channels: 3
    },
    progress: 75
  },
  {
    datasetId: 'dataset-003',
    datasetDescription: '文本分类数据集',
    datasetType: 'MODEL',
    vmId: 'vm-003',
    status: 'ERROR',
    uploadTime: '2024-01-15T10:00:00Z',
    uploadedBy: 'user-003',
    tags: ['文本分类', 'NLP', '情感分析'],
    metadata: {
      sampleCount: 50000,
      vocabularySize: 10000,
      maxSequenceLength: 512
    },
    progress: 0
  }
]

// ==================== 模拟数据统计 ====================

export const mockDataStatistics: DataStatistics = {
  totalDatasets: 150,
  totalSize: 2.5e9, // 2.5GB
  datasetsByType: {
    ACOUSTIC: 45,
    ENVIRONMENT: 35,
    MODEL: 40,
    FEATURE: 20,
    OTHER: 10
  },
  datasetsByStatus: {
    UPLOADING: 5,
    PROCESSING: 12,
    VALIDATING: 8,
    READY: 115,
    ERROR: 7,
    DELETED: 3
  },
  uploadsThisMonth: 25
}

// ==================== 模拟请求数据 ====================

export const mockUploadFileRequest: UploadFileRequest = {
  vmId: 'vm-001',
  dataType: 'ACOUSTIC',
  description: '新上传的训练数据集',
  tags: ['测试', '上传'],
  metadata: {
    format: 'CSV',
    expectedSamples: 1000
  },
  file: new File(['test data'], 'test.csv', { type: 'text/csv' })
}

export const mockUploadTextRequest = {
  datasetDescription: '文本数据集',
  datasetType: 'ACOUSTIC' as const,
  textData: 'sample,text,data\n1,2,3\n4,5,6',
  vmId: 'vm-001',
  tags: ['文本', '测试'],
  metadata: {
    format: 'CSV',
    delimiter: ','
  }
}

export const mockPreprocessRequest: PreprocessRequest = {
  methods: ['NORMALIZATION', 'FEATURE_SELECTION'],
  parameters: {
    normalization: { method: 'min-max', range: [0, 1] },
    featureSelection: { method: 'variance', threshold: 0.1 }
  },
  outputFormat: 'CSV'
}

export const mockValidationRequest = {
  checkDuplicates: true,
  checkMissing: true,
  checkOutliers: true,
  validationRules: [
    { field: 'value', type: 'range', min: 0, max: 100 }
  ]
}

export const mockUpdateDataRequest: UpdateDataRequest = {
  datasetDescription: '更新后的数据集描述',
  tags: ['更新', '测试'],
  metadata: {
    version: '2.0',
    lastModified: new Date().toISOString()
  }
}

export const mockBatchOperationRequest: BatchOperationRequest = {
  operation: 'DELETE',
  datasetIds: ['dataset-001', 'dataset-002'],
  parameters: {
    force: true,
    deleteFiles: true
  }
}

export const mockExportDataRequest: ExportDataRequest = {
  exportType: 'CSV',
  filters: {
    dataType: 'ACOUSTIC',
    vmId: 'vm-001'
  },
  fields: ['datasetId', 'datasetDescription', 'status'],
  format: 'ZIP'
}

// ==================== 模拟响应数据 ====================

export const mockUploadFileResponse = {
  datasetId: 'dataset-new-001',
  uploadId: 'upload-001',
  status: 'UPLOADING',
  estimatedTime: 300
}

export const mockUploadTextResponse = {
  datasetId: 'dataset-text-001',
  uploadId: 'upload-text-001',
  status: 'PROCESSING',
  estimatedTime: 60
}

export const mockPreprocessResponse = {
  processId: 'process-001',
  status: 'PROCESSING',
  estimatedTime: 600,
  outputDatasetId: 'dataset-processed-001'
}

export const mockValidationResponse = {
  valid: true,
  issues: [],
  statistics: {
    totalRecords: 1000,
    validRecords: 995,
    duplicateRecords: 3,
    missingValueRecords: 2,
    outlierRecords: 0
  }
}

export const mockBatchOperationResponse = {
  operationId: 'batch-001',
  processedCount: 2,
  failedCount: 0,
  results: [
    { datasetId: 'dataset-001', success: true },
    { datasetId: 'dataset-002', success: true }
  ]
}

export const mockExportDataResponse = {
  exportId: 'export-001',
  downloadUrl: '/api/data/export/export-001/download',
  status: 'COMPLETED',
  fileSize: 1048576
}

// ==================== 模拟错误信息 ====================

export const mockTrainingDataErrors = {
  FETCH_DATA_LIST_ERROR: new Error('获取数据列表失败'),
  FETCH_DATA_DETAIL_ERROR: new Error('获取数据详情失败'),
  UPLOAD_FILE_ERROR: new Error('文件上传失败'),
  UPLOAD_TEXT_ERROR: new Error('文本上传失败'),
  PREPROCESS_DATA_ERROR: new Error('数据预处理失败'),
  VALIDATE_DATA_ERROR: new Error('数据验证失败'),
  UPDATE_DATA_ERROR: new Error('更新数据失败'),
  DELETE_DATA_ERROR: new Error('删除数据失败'),
  BATCH_OPERATION_ERROR: new Error('批量操作失败'),
  EXPORT_DATA_ERROR: new Error('导出数据失败'),
  FETCH_STATISTICS_ERROR: new Error('获取统计信息失败'),
  NETWORK_ERROR: new Error('网络连接失败'),
  TIMEOUT_ERROR: new Error('请求超时'),
  SERVER_ERROR: new Error('服务器内部错误'),
  PERMISSION_ERROR: new Error('权限不足'),
  STORAGE_ERROR: new Error('存储空间不足')
}

// ==================== 模拟初始状态 ====================

export const mockInitialTrainingDataState = {
  dataList: [],
  dataListTotal: 0,
  dataListLoading: false,
  dataListError: null,
  currentData: null,
  currentDataLoading: false,
  currentDataError: null,
  uploadLoading: {},
  uploadProgress: {},
  uploadError: {},
  processLoading: {},
  processError: {},
  operationLoading: {},
  operationError: {},
  statistics: null,
  statisticsLoading: false,
  pagination: {
    page: 1,
    size: 20,
    total: 0
  },
  queryParams: {},
  selectedDataIds: []
}

// ==================== 模拟工具函数 ====================

export const createMockTrainingDataset = (overrides: Partial<TrainingDataset> = {}): TrainingDataset => ({
  ...mockTrainingDataset,
  datasetId: `dataset-${Date.now()}`,
  uploadTime: new Date().toISOString(),
  ...overrides
})

export const createMockDataStatistics = (overrides: Partial<DataStatistics> = {}): DataStatistics => ({
  ...mockDataStatistics,
  ...overrides
})

export const createMockUploadFileRequest = (overrides: Partial<UploadFileRequest> = {}): UploadFileRequest => ({
  ...mockUploadFileRequest,
  ...overrides
})

export const createMockPreprocessRequest = (overrides: Partial<PreprocessRequest> = {}): PreprocessRequest => ({
  ...mockPreprocessRequest,
  ...overrides
})

export const createMockValidationRequest = (overrides: any = {}) => ({
  ...mockValidationRequest,
  ...overrides
})

export const createMockBatchOperationRequest = (overrides: Partial<BatchOperationRequest> = {}): BatchOperationRequest => ({
  ...mockBatchOperationRequest,
  ...overrides
})

export const createMockExportDataRequest = (overrides: Partial<ExportDataRequest> = {}): ExportDataRequest => ({
  ...mockExportDataRequest,
  ...overrides
})
