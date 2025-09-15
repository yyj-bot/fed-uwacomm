/**
 * 联邦学习任务服务模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// 导出服务类和实例
export { FederatedTaskService, federatedTaskService } from './federatedTaskService'

// 导出类型定义
// export type {
//   CreateTaskRequest,
//   CreateTaskResponse,
//   ConfigTaskRequest,
//   ConfigTaskResponse,
//   StartTaskResponse,
//   PauseTaskResponse,
//   ResumeTaskResponse,
//   StopTaskRequest,
//   StopTaskResponse,
//   CancelTaskRequest,
//   CancelTaskResponse,
//   TaskListParams,
//   TaskListResponse,
//   TaskLogsParams,
//   TaskLogsResponse,
//   DeleteTaskRequest,
//   DeleteTaskResponse,
//   TaskOperation,
//   TaskOperationLog,
//   TaskMetrics,
//   ParticipantStatus,
//   TaskProgress,
//   TaskStatistics,
//   FederatedTaskServiceError,
//   TaskOperationError,
//   TaskValidationError,
//   FederatedTaskServiceConfig
// } from './type'

export * from './type'