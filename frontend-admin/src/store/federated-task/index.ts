/**
 * 联邦学习任务模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

export { useTaskStore } from './federatedTaskStore'
export { useTask } from './useFederatedTaskStore'
export type { TaskState, TaskActions, TaskStore } from './federatedTaskStore'

// 默认导出 hook
export { useTask as default } from './useFederatedTaskStore'
