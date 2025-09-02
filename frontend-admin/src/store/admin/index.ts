/**
 * 管理员功能模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

export { useAdminStore } from './adminStore'
export { useAdmin } from './useAdminStore'
export type { AdminState, AdminActions, AdminStore } from './adminStore'

// 默认导出 hook
export { useAdmin as default } from './useAdminStore'
