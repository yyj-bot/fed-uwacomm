/**
 * Store 统一导出文件
 * 提供项目中所有状态管理 store 和 hooks 的统一访问入口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// ==================== 认证模块 ====================
export {
  useAuthStore,
  useAuth
} from './auth'
export type {
  AuthState,
  AuthActions,
  AuthStore
} from './auth'

// ==================== 虚拟机管理模块 ====================
export {
  useVMStore,
  useVM
} from './vm'
export type {
  VMState,
  VMActions,
  VMStore
} from './vm'

// ==================== 联邦学习任务模块 ====================
export {
  useTaskStore,
  useTask
} from './federated-task'
export type {
  TaskState,
  TaskActions,
  TaskStore
} from './federated-task'

// ==================== 训练数据模块 ====================
export {
  useDataStore,
  useData
} from './training-data'
export type {
  DataState,
  DataActions,
  DataStore
} from './training-data'

// ==================== 模型版本模块 ====================
export {
  useModelStore,
  useModel
} from './model-version'
export type {
  ModelState,
  ModelActions,
  ModelStore
} from './model-version'

// ==================== 管理员功能模块 ====================
export {
  useAdminStore,
  useAdmin
} from './admin'
export type {
  AdminState,
  AdminActions,
  AdminStore
} from './admin'

// ==================== 系统监控模块 ====================
export {
  useSystemStore,
  useSystem
} from './system'
export type {
  SystemState,
  SystemActions,
  SystemStore
} from './system'

// ==================== 仪表盘模块 ====================
export {
  useDashboardStore,
  useDashboard
} from './dashboard'
export type {
  DashboardState,
  DashboardActions,
  DashboardStore,
  DashboardOverview,
  DashboardChartData
} from './dashboard'

// ==================== WebSocket 连接模块 ====================
export {
  useWebSocketStore,
  useWebSocket,
  useWebSocketMessage,
  useVMStatusUpdate,
  useTaskProgressUpdate
} from './websocket'
export type {
  WebSocketState,
  WebSocketActions,
  WebSocketStore
} from './websocket'

// ==================== 默认导出主要 Hooks ====================
// 为了方便使用，导出最常用的 hooks
export {
  useAuth as default,
  useVM,
  useTask,
  useData,
  useModel,
  useAdmin,
  useSystem,
  useDashboard,
  useWebSocket
}

// ==================== Store 管理工具 ====================

/**
 * 重置所有 Store 状态
 * 用于用户登出或应用重置时清理状态
 */
export const resetAllStores = () => {
  // 注意：这里需要在各个 store 实现 reset 方法
  console.log('[Store] 重置所有 store 状态')
  
  // 由于 zustand 的特性，我们需要通过各个 store 的 reset 方法来重置
  // 这里提供一个统一的接口，具体实现在各个 store 中
}

/**
 * 获取所有 Store 的状态快照
 * 用于调试和状态监控
 */
export const getStoreSnapshot = () => {
  // 这里可以收集各个 store 的状态用于调试
  return {
    timestamp: Date.now(),
    // 具体的状态快照需要在各个 store 中实现
  }
}

// ==================== Store 类型集合 ====================
// 导出所有 Store 相关的类型，方便类型推导和使用

export interface AllStores {
  auth: AuthStore
  vm: VMStore
  task: TaskStore
  data: DataStore
  model: ModelStore
  admin: AdminStore
  system: SystemStore
  dashboard: DashboardStore
  websocket: WebSocketStore
}

export interface AllStoreHooks {
  useAuth: typeof useAuth
  useVM: typeof useVM
  useTask: typeof useTask
  useData: typeof useData
  useModel: typeof useModel
  useAdmin: typeof useAdmin
  useSystem: typeof useSystem
  useDashboard: typeof useDashboard
  useWebSocket: typeof useWebSocket
}

// ==================== Store 配置 ====================

/**
 * Store 配置选项
 */
export interface StoreConfig {
  // 是否启用开发工具
  enableDevtools?: boolean
  // 是否启用持久化
  enablePersistence?: boolean
  // 持久化存储键前缀
  persistencePrefix?: string
}

/**
 * 默认 Store 配置
 */
export const defaultStoreConfig: StoreConfig = {
  enableDevtools: process.env.NODE_ENV === 'development',
  enablePersistence: true,
  persistencePrefix: 'feduwacomm-'
}

// ==================== Store 初始化和销毁 ====================

/**
 * 初始化所有 Store
 * 在应用启动时调用
 */
export const initializeStores = async (config: Partial<StoreConfig> = {}) => {
  const finalConfig = { ...defaultStoreConfig, ...config }
  
  console.log('[Store] 开始初始化所有 Store...', finalConfig)
  
  try {
    // 这里可以进行一些全局的 store 初始化工作
    // 比如从持久化存储中恢复状态、设置全局监听器等
    
    console.log('[Store] 所有 Store 初始化完成')
    return { success: true, config: finalConfig }
  } catch (error) {
    console.error('[Store] Store 初始化失败:', error)
    return { success: false, error: error instanceof Error ? error.message : 'Unknown error' }
  }
}

/**
 * 销毁所有 Store
 * 在应用关闭时调用
 */
export const destroyStores = () => {
  console.log('[Store] 开始销毁所有 Store...')
  
  try {
    // 清理所有 store 状态
    resetAllStores()
    
    // 这里可以进行一些清理工作
    // 比如移除监听器、清理定时器等
    
    console.log('[Store] 所有 Store 销毁完成')
  } catch (error) {
    console.error('[Store] Store 销毁失败:', error)
  }
}

// ==================== Store 状态监听 ====================

/**
 * Store 状态变化监听器类型
 */
export type StoreListener<T = any> = (state: T, prevState: T) => void

/**
 * 添加全局 Store 状态监听器
 * 可以用于调试、日志记录、状态同步等
 */
export const addGlobalStoreListener = <T>(
  storeName: keyof AllStores,
  listener: StoreListener<T>
) => {
  // 这里可以实现全局状态监听逻辑
  console.log(`[Store] 添加全局监听器: ${storeName}`)
}

/**
 * 移除全局 Store 状态监听器
 */
export const removeGlobalStoreListener = <T>(
  storeName: keyof AllStores,
  listener: StoreListener<T>
) => {
  // 这里可以实现移除监听器的逻辑
  console.log(`[Store] 移除全局监听器: ${storeName}`)
}

// ==================== 开发工具 ====================

/**
 * 开发环境下的 Store 调试工具
 */
export const storeDebugTools = {
  /**
   * 打印所有 Store 状态
   */
  logAllStates: () => {
    if (process.env.NODE_ENV === 'development') {
      console.group('[Store Debug] 所有 Store 状态')
      console.log('快照:', getStoreSnapshot())
      console.groupEnd()
    }
  },
  
  /**
   * 重置指定 Store
   */
  resetStore: (storeName: keyof AllStores) => {
    if (process.env.NODE_ENV === 'development') {
      console.log(`[Store Debug] 重置 Store: ${storeName}`)
      // 具体重置逻辑需要在各个 store 中实现
    }
  }
}

// 在开发环境下将调试工具挂载到全局对象
if (process.env.NODE_ENV === 'development' && typeof window !== 'undefined') {
  (window as any).__FEDUWACOMM_STORE_DEBUG__ = storeDebugTools
}