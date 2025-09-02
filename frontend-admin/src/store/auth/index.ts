/**
 * 认证模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

export { useAuthStore } from './authStore'
export { useAuth } from './useAuthStore'
export type { AuthState, AuthActions, AuthStore } from './authStore'

// 默认导出 hook
export { useAuth as default } from './useAuthStore'
