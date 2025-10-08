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

// 虚拟机服务
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


// ==================== WebSocket服务 ====================
export { 
  wsService,
  websocketService,
  WebSocketService,
  ConnectionManager,
  WSMessageHandler,
  createWebSocketService,
  WebSocketUtils,
  ConnectionState,
  type WebSocketMessage,
  type ConnectionStatus,
  type WebSocketConfig,
  type MessageHandler,
  type ConnectionStateHandler,
  type ErrorHandler,
  type ConnectMessage,
  type HeartbeatMessage,
  type StatusQueryMessage,
  type TrainingStartMessage,
  type TrainingStopMessage,
  type TrainingProgressMessage,
  type ModelUploadMessage,
  type ModelDownloadMessage,
  type VMStartMessage,
  type VMStopMessage,
  type DatasetCreateMessage,
  type DatasetAppendRowsMessage,
  type DatasetCompleteMessage,
  type ErrorMessage,
  type IWebSocketService,
  type WebSocketEvent,
  type WebSocketEventListener
} from './websocket'

// ==================== 服务实例集合 ====================
// 提供所有服务实例的集合，方便统一管理
import { wsService } from './websocket'
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
  systemLog: systemLogService,
  
  // WebSocket服务
  websocket: wsService,
  ws: wsService // 别名
} as const

// ==================== 服务初始化和销毁 ====================
/**
 * 初始化所有服务
 */
export async function initializeServices(): Promise<void> {
  console.log('[Services] 开始初始化服务...')
  
  try {
    // WebSocket服务需要在用户登录后手动连接
    console.log('[Services] WebSocket服务已准备就绪')
    
    console.log('[Services] 所有服务初始化完成')
  } catch (error) {
    console.error('[Services] 服务初始化失败:', error)
    throw error
  }
}

/**
 * 销毁所有服务
 */
export function destroyServices(): void {
  console.log('[Services] 开始销毁服务...')
  
  try {
    // 销毁WebSocket服务
    wsService.destroy()
    console.log('[Services] WebSocket服务已销毁')
    
    console.log('[Services] 所有服务销毁完成')
  } catch (error) {
    console.error('[Services] 服务销毁失败:', error)
  }
}

/**
 * 获取服务状态
 */
export function getServicesStatus() {
  return {
    websocket: {
      isConnected: wsService.isConnected(),
      state: wsService.getState(),
      status: wsService.getStatus(),
      stats: wsService.getConnectionStats()
    }
  }
}

// ==================== 基础类型导出 ====================
// 重新导出基础类型，方便其他模块使用
export type { User, LoginRequest, LoginResponse } from '@/types'

// 服务实例类型
export type ServicesType = typeof services

// ==================== 工具函数 ====================
/**
 * 检查WebSocket服务是否就绪
 */
export function isWebSocketReady(): boolean {
  return wsService.isConnected()
}

/**
 * 等待WebSocket服务就绪
 */
export function waitForWebSocket(timeout = 10000): Promise<void> {
  return new Promise((resolve, reject) => {
    const startTime = Date.now()
    
    const checkWebSocket = () => {
      if (isWebSocketReady()) {
        resolve()
        return
      }
      
      if (Date.now() - startTime > timeout) {
        reject(new Error('等待WebSocket服务就绪超时'))
        return
      }
      
      setTimeout(checkWebSocket, 500)
    }
    
    checkWebSocket()
  })
}

/**
 * 连接WebSocket服务（带认证检查）
 */
export async function connectWebSocketService(): Promise<void> {
  // 检查是否有认证token
  const token = localStorage.getItem('access_token')
  if (!token) {
    throw new Error('未找到认证Token，请先登录')
  }
  
  // 连接WebSocket
  await wsService.connect()
}
