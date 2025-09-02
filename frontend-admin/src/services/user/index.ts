/**
 * 用户服务模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// 导出服务类和实例
export { UserService, userService } from './userService'

// 导出类型定义
// export type {
//   RegisterUserRequest,
//   RefreshTokenResponse,
//   UpdateProfileRequest,
//   ChangePasswordRequest,
//   UserOperation,
//   UserOperationLog,
//   AuthState,
//   LoginStatus,
//   UserServiceError,
//   AuthError,
//   ValidationError,
//   PasswordStrength,
//   PasswordPolicy,
//   LoginSecurityPolicy,
//   UserActivityStats,
//   UserPreferences,
//   UserServiceConfig
// } from './type'

export * from './type'