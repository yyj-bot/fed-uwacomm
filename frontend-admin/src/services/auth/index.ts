/**
 * 认证服务统一导出
 */

import { AuthService } from './auth-service'

// 导出服务实例
export const authService = AuthService.getInstance()

// 导出类型
export type { 
  AuthState,
  AuthEvent,
  AuthEventListener,
  TokenInfo,
  PermissionCheckResult,
  RolePermissions
} from './types'

// 导出类
export { AuthService } from './auth-service'
export { TokenManager } from './token-manager'

// 默认导出
export default authService
