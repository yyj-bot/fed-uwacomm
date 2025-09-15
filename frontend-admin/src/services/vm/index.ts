/**
 * 虚拟机服务模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// 导出服务类和实例
export { VMService, vmService } from './vmService'

// 导出类型定义
// export type {
//   VMListParams,
//   VMListResponse,
//   VMUpdateRequest,
//   VMUpdateResponse,
//   VMDeleteResponse,
//   VMStartRequest,
//   VMStopRequest,
//   VMRestartRequest,
//   VMStartResponse,
//   VMStopResponse,
//   VMRestartResponse,
//   VMOperation,
//   VMOperationLog,
//   VMResourceUsage,
//   VMNetworkInfo,
//   VMProcessInfo,
//   VMStatistics,
//   VMServiceError,
//   VMOperationError,
//   VMValidationError,
//   VMServiceConfig
// } from './type'

export * from './type'