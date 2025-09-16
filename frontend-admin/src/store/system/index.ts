/**
 * 系统监控模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

export { useSystemStore } from './systemStore'
export { useSystem } from './useSystemStore'
export type { SystemState, SystemActions, SystemStore } from './systemStore'

// 默认导出 hook
export { useSystem as default } from './useSystemStore'
