/**
 * 管理员服务模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// 导出服务类和实例
export { AdminUserService, adminUserService } from './adminService'

// 导出类型定义
// export type {
//   Permission,
//   UserListParams,
//   CreateUserRequest,
//   UpdateUserRequest,
//   LockUserRequest,
//   ResetPasswordRequest,
//   GrantPermissionRequest,
//   LockUserResponse,
//   UnlockUserResponse,
//   ServiceError,
//   ValidationError,
//   UserOperation,
//   UserOperationLog,
//   UserStatistics,
//   AdminServiceConfig
// } from './type'

export * from './type'
