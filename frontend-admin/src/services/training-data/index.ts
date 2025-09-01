/**
 * 训练数据服务模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// 导出服务类和实例
export { TrainingDataService, trainingDataService } from './trainingDataService'

// 导出类型定义
export type {
  UploadFileRequest,
  UploadFileResponse,
  UploadTextRequest,
  UploadTextResponse,
  DataListParams,
  DataListResponse,
  PreprocessRequest,
  ValidationRequest,
  UpdateDataRequest,
  UpdateDataResponse,
  DeleteDataResponse,
  BatchOperationRequest,
  BatchOperationResultItem,
  DataStatisticsParams,
  ExportDataRequest,
  TrainingDataServiceError,
  DataOperationError,
  FileValidationError,
  TrainingDataServiceConfig
} from './type'