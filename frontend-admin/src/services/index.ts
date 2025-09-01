/**
 * 服务层统一导出
 * 提供项目中所有服务的统一访问入口
 */

// ==================== 认证服务 ====================
export { 
  authService, 
  AuthService, 
  TokenManager,
  type AuthState,
  type AuthEvent,
  type AuthEventListener,
  type TokenInfo,
  type PermissionCheckResult,
  type RolePermissions
} from './auth'

// 默认导出认证服务（兼容旧代码）
export { default as authService } from './auth'

// ==================== WebSocket服务 ====================
export { 
  wsService,
  websocketService,
  WebSocketService,
  ConnectionManager,
  WSMessageHandler,
  type WebSocketMessage,
  type ConnectionStatus,
  type ConnectionState,
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
  type IWebSocketService
} from './websocket'

// 默认导出WebSocket服务（兼容旧代码）
export { default as websocketService } from './websocket'

// ==================== 服务实例集合 ====================
// 提供所有服务实例的集合，方便统一管理
import { authService } from './auth'
import { wsService } from './websocket'

export const services = {
  auth: authService,
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
    // 认证服务会在实例化时自动初始化
    console.log('[Services] 认证服务已初始化')
    
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
    
    // 销毁认证服务
    authService.destroy()
    console.log('[Services] 认证服务已销毁')
    
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
    auth: {
      isAuthenticated: authService.isAuthenticated(),
      user: authService.getCurrentUser(),
      state: authService.getState()
    },
    websocket: {
      isConnected: wsService.isConnected(),
      state: wsService.getState(),
      status: wsService.getStatus(),
      stats: wsService.getConnectionStats()
    }
  }
}

// ==================== 类型导出 ====================
// 重新导出所有相关类型，方便其他模块使用
export type { User, LoginRequest, LoginResponse } from '@/types'

// ==================== 工具函数 ====================
/**
 * 检查服务是否就绪
 */
export function isServicesReady(): boolean {
  return authService.isAuthenticated() && wsService.isConnected()
}

/**
 * 等待服务就绪
 */
export function waitForServices(timeout = 10000): Promise<void> {
  return new Promise((resolve, reject) => {
    const startTime = Date.now()
    
    const checkServices = () => {
      if (isServicesReady()) {
        resolve()
        return
      }
      
      if (Date.now() - startTime > timeout) {
        reject(new Error('等待服务就绪超时'))
        return
      }
      
      setTimeout(checkServices, 500)
    }
    
    checkServices()
  })
}
