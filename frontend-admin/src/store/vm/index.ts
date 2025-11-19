/**
 * 水下机器人管理模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

export { useVMStore } from './vmStore'
export { useVM } from './useVMStore'
export type { VMState, VMActions, VMStore } from './vmStore'

export { useAdminVmStore } from './adminVmStore'
export { useAdminVM } from './useAdminVM'
export type { AdminVmState, AdminVmActions, AdminVmStore } from './adminVmStore'

// 默认导出 hook
export { useVM as default } from './useVMStore'
