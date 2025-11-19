/**
 * 服务层统一导出
 * 提供项目中所有服务的统一访问入口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// ==================== 业务服务 ====================
// 管理员服务
export { adminUserService as adminService } from './admin'
export type {
  AdminUserService,
  Permission,
  UserListParams,
  CreateUserRequest,
  UpdateUserRequest,
  LockUserRequest,
  ResetPasswordRequest,
  GrantPermissionRequest,
  LockUserResponse,
  UnlockUserResponse,
  UserStatistics,
  AdminServiceConfig
} from './admin'

// 用户服务
export { userService } from './user'
export type {
  UserService,
  RegisterUserRequest,
  RefreshTokenResponse,
  UpdateProfileRequest,
  ChangePasswordRequest,
  AuthState,
  LoginStatus,
  UserServiceError,
  AuthError,
  PasswordStrength,
  PasswordPolicy,
  LoginSecurityPolicy,
  UserActivityStats,
  UserPreferences,
  UserServiceConfig
} from './user'

// 水下机器人服务
export { vmService } from './vm'
export type {
  VMService,
  VMListParams,
  VMUpdateRequest,
  VMStartRequest,
  VMStopRequest,
  VMRestartRequest,
  VMListResponse,
  VMUpdateResponse,
  VMDeleteResponse,
  VMStartResponse,
  VMStopResponse,
  VMRestartResponse,
  VMResourceUsage,
  VMNetworkInfo,
  VMServiceConfig,
  // VM本地模型相关类型
  VMRoundModel,
  VMModelTrend,
  VMModelBest,
  VMRoundModelPaginatedResponse,
  VMRoundModelListParams,
  VMModelTrendParams,
  VMModelBestParams,
  MetricType,
  QueryType
} from './vm'

// 联邦任务服务
export { federatedTaskService } from './federated-task'
export type {
  FederatedTaskService,
  CreateTaskRequest,
  ConfigTaskRequest,
  TaskListParams,
  TaskLogsParams,
  DeleteTaskRequest,
  StopTaskRequest,
  CancelTaskRequest,
  FederatedTaskServiceConfig
} from './federated-task'

// 模型版本服务
export { modelVersionService } from './model-version'
export type {
  ModelVersionService,
  ModelVersionListParams,
  EvaluationRequest,
  BatchEvaluationRequest,
  RollbackRequest,
  ModelVersionServiceConfig,
  ModelVersionDetail,
  TaskModelVersions,
  EvaluationResult,
  RollbackInfo,
  ModelStatistics,
  TaskStatistics,
  DownloadRequest,
  DeleteModelRequest,
  StatisticsParams,
  // 初始模型管理相关类型
  InitialModelInfo,
  InitialModelGenerationRequest,
  InitialModelGenerationResponse,
  InitialModelUploadRequest,
  InitialModelUploadResponse,
  ModelDistributionRequest,
  ModelDistributionResponse,
  DistributionStatusDetail,
  InitialModelDeleteRequest,
  InitialModelDeleteResponse
} from './model-version'

// 训练数据服务
export { trainingDataService } from './training-data'
export type {
  TrainingDataService,
  UploadFileRequest,
  UploadTextRequest,
  DataListParams,
  PreprocessRequest,
  ValidationRequest,
  UpdateDataRequest,
  BatchOperationRequest,
  ExportDataRequest,
  TrainingDataServiceConfig,
  DataStatisticsParams
} from './training-data'

// 系统日志服务
export { systemLogService } from './system-log'
export type {
  SystemLogService,
  SystemLogPaginatedResponse,
  SystemLogListParams,
  RealtimeLogsParams,
  RealtimeLogsResponse,
  LogStatisticsParams,
  LogStatisticsResponse,
  LogExportData,
  LogExportResponse,
  ExportTask,
  ExportHistoryParams,
  LogCleanupData,
  LogCleanupResponse,
  CleanupTask,
  CleanupHistoryParams,
  SystemMonitor,
  LogMonitorParams,
  LogMonitor,
  PerformanceMonitorParams,
  PerformanceMonitor,
  AlertConfig,
  LogConfig,
  LogConfigUpdateData,
  LogConfigUpdateResponse,
  CleanupStrategy,
  SystemLogOperation,
  SystemLogServiceError,
  SystemLogOperationError
} from './system-log'


// ==================== 服务实例集合 ====================
// 提供所有服务实例的集合，方便统一管理
import { adminUserService } from './admin'
import { userService } from './user'
import { vmService } from './vm'
import { federatedTaskService } from './federated-task'
import { modelVersionService } from './model-version'
import { trainingDataService } from './training-data'
import { systemLogService } from './system-log'

export const services = {
  // 业务服务
  admin: adminUserService,
  user: userService,
  vm: vmService,
  federatedTask: federatedTaskService,
  modelVersion: modelVersionService,
  trainingData: trainingDataService,
  systemLog: systemLogService
} as const

// ==================== 服务初始化和销毁 ====================
// ==================== 基础类型导出 ====================
// 重新导出基础类型，方便其他模块使用
export type { User, LoginRequest, LoginResponse } from '@/types'

// 服务实例类型
export type ServicesType = typeof services
