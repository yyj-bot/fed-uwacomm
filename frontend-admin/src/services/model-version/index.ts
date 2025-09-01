/**
 * 模型版本管理服务模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// 导出服务类和实例
export { ModelVersionService, modelVersionService } from './modelVersionService'

// 导出类型定义
export type {
  UploadModelRequest,
  UploadModelResponse,
  BatchUploadRequest,
  BatchUploadResponse,
  ModelVersionListParams,
  EvaluationRequest,
  EvaluationResponse,
  BatchEvaluationRequest,
  BatchEvaluationResponse,
  EvaluationResult,
  DeploymentRequest,
  DeploymentResponse,
  RollbackRequest,
  RollbackResponse,
  RollbackInfo,
  DownloadRequest,
  BatchDownloadRequest,
  DeleteModelRequest,
  DeleteModelResponse,
  BatchDeleteRequest,
  BatchDeleteResponse,
  StatisticsParams,
  TaskStatistics,
  ModelStatistics,
  ModelVersionServiceError,
  ModelOperationError,
  ModelFileValidationError,
  ModelVersionServiceConfig
} from './type'