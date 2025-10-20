/**
 * 虚拟机状态管理
 * 共享的运行时状态，在 mock handlers 之间共享
 */

// 虚拟机状态映射（模拟运行时状态）
export const vmStatusMap = new Map<string, string>()

// 已删除的VM ID集合
export const deletedVmIds = new Set<string>()

// 初始化默认状态
vmStatusMap.set('a1b2c3d4e5f678901234567890123456', 'RUNNING')
vmStatusMap.set('b2c3d4e5f6789012345678901234567a', 'RUNNING')
vmStatusMap.set('c3d4e5f67890123456789012345678ab', 'STOPPED')

