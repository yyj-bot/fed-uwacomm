/**
 * WebSocket 连接状态管理 Store
 * 管理 WebSocket 连接、消息处理、实时数据更新等相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { wsService } from '@/services'
import { ConnectionState } from '@/services'
import type { 
  WebSocketMessage,
  ConnectionStatus,
  WebSocketConfig,
  MessageHandler,
  ConnectionStateHandler,
  ErrorHandler
} from '@/services'

// ==================== 状态类型定义 ====================

interface WebSocketState {
  // 连接状态
  isConnected: boolean
  connectionStatus: ConnectionState
  connectionError: string | null
  reconnectAttempts: number
  maxReconnectAttempts: number
  
  // 连接统计
  connectionStats: {
    connectedAt: number | null
    disconnectedAt: number | null
    totalMessages: number
    totalErrors: number
    uptime: number
  }
  
  // 消息处理
  messageHandlers: Record<string, MessageHandler[]>
  lastMessage: WebSocketMessage | null
  messageHistory: WebSocketMessage[]
  maxHistorySize: number
  
  // 实时数据
  realtimeData: {
    vmStatus: Record<string, any>
    taskProgress: Record<string, any>
    systemMetrics: any
    notifications: any[]
  }
  
  // 配置
  config: WebSocketConfig | null
}

interface WebSocketActions {
  // 连接管理
  connect: (config?: WebSocketConfig) => Promise<void>
  disconnect: () => Promise<void>
  reconnect: () => Promise<void>
  
  // 消息处理
  sendMessage: (message: WebSocketMessage) => Promise<void>
  addMessageHandler: (type: string, handler: MessageHandler) => void
  removeMessageHandler: (type: string, handler: MessageHandler) => void
  clearMessageHandlers: (type?: string) => void
  processMessage: (message: WebSocketMessage) => void
  
  // 实时数据更新
  updateVMStatus: (vmId: string, status: any) => void
  updateTaskProgress: (taskId: string, progress: any) => void
  updateSystemMetrics: (metrics: any) => void
  addNotification: (notification: any) => void
  clearNotifications: () => void
  
  // 状态管理
  setConnectionStatus: (status: ConnectionState) => void
  setConnectionError: (error: string | null) => void
  incrementReconnectAttempts: () => void
  resetReconnectAttempts: () => void
  
  // 配置管理
  setConfig: (config: WebSocketConfig) => void
  
  // 消息历史
  addMessageToHistory: (message: WebSocketMessage) => void
  clearMessageHistory: () => void
  
  // 状态重置
  resetState: () => void
}

type WebSocketStore = WebSocketState & WebSocketActions

// ==================== 初始状态 ====================

const initialState: WebSocketState = {
  isConnected: false,
  connectionStatus: ConnectionState.DISCONNECTED,
  connectionError: null,
  reconnectAttempts: 0,
  maxReconnectAttempts: 5,
  
  connectionStats: {
    connectedAt: null,
    disconnectedAt: null,
    totalMessages: 0,
    totalErrors: 0,
    uptime: 0
  },
  
  messageHandlers: {},
  lastMessage: null,
  messageHistory: [],
  maxHistorySize: 100,
  
  realtimeData: {
    vmStatus: {},
    taskProgress: {},
    systemMetrics: {},
    notifications: []
  },
  
  config: null
}

// ==================== Store 实现 ====================

export const useWebSocketStore = create<WebSocketStore>((set, get) => ({
  ...initialState,

  // ==================== 连接管理 ====================
  
  /**
   * 连接 WebSocket
   */
  connect: async (config?: WebSocketConfig) => {
    const { setConnectionStatus, setConnectionError, resetReconnectAttempts, setConfig } = get()
    
    try {
      setConnectionStatus(ConnectionState.CONNECTING)
      setConnectionError(null)
      
      if (config) {
        setConfig(config)
      }
      
      // 设置连接状态处理器
      const connectionStateHandler: ConnectionStateHandler = (status) => {
        setConnectionStatus(status)
        
        if (status === ConnectionState.CONNECTED) {
          set((state) => ({
            isConnected: true,
            connectionStats: {
              ...state.connectionStats,
              connectedAt: Date.now(),
              disconnectedAt: null
            }
          }))
          resetReconnectAttempts()
        } else if (status === ConnectionState.DISCONNECTED) {
          set((state) => ({
            isConnected: false,
            connectionStats: {
              ...state.connectionStats,
              disconnectedAt: Date.now()
            }
          }))
        }
      }
      
      // 设置消息处理器
      const messageHandler: MessageHandler = (message) => {
        const { addMessageToHistory, processMessage, messageHandlers } = get()
        
        // 添加到消息历史
        addMessageToHistory({
          ...message,
          timestamp: Date.now().toString(),
          direction: 'incoming'
        } as WebSocketMessage)
        
        // 更新统计
        set((state) => ({
          lastMessage: message,
          connectionStats: {
            ...state.connectionStats,
            totalMessages: state.connectionStats.totalMessages + 1
          }
        }))
        
        // 处理特定类型的消息
        processMessage(message)
        
        // 调用注册的处理器
        const handlers = messageHandlers[message.type] || []
        handlers.forEach(handler => {
          try {
            handler(message)
          } catch (error) {
            console.error(`[WebSocket] 消息处理器执行失败 (${message.type}):`, error)
          }
        })
      }
      
      // 设置错误处理器
      const errorHandler: ErrorHandler = (error) => {
        console.error('[WebSocket] 连接错误:', error)
        setConnectionError(error.message)
        set((state) => ({
          connectionStats: {
            ...state.connectionStats,
            totalErrors: state.connectionStats.totalErrors + 1
          }
        }))
      }
      
      // 连接 WebSocket
      await wsService.connect()
      
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'WebSocket 连接失败'
      setConnectionError(errorMessage)
      setConnectionStatus(ConnectionState.DISCONNECTED)
      throw error
    }
  },

  /**
   * 断开 WebSocket 连接
   */
  disconnect: async () => {
    try {
      await wsService.disconnect()
      
      set({
        isConnected: false,
        connectionStatus: ConnectionState.DISCONNECTED,
        connectionError: null
      })
    } catch (error) {
      console.error('[WebSocket] 断开连接失败:', error)
    }
  },

  /**
   * 重新连接 WebSocket
   */
  reconnect: async () => {
    const { connect, config, incrementReconnectAttempts, reconnectAttempts, maxReconnectAttempts } = get()
    
    if (reconnectAttempts >= maxReconnectAttempts) {
      throw new Error('超过最大重连次数')
    }
    
    incrementReconnectAttempts()
    
    try {
      await connect(config || undefined)
    } catch (error) {
      console.error(`[WebSocket] 重连失败 (第${reconnectAttempts + 1}次):`, error)
      throw error
    }
  },

  // ==================== 消息处理 ====================
  
  /**
   * 发送消息
   */
  sendMessage: async (message: WebSocketMessage) => {
    const { isConnected } = get()
    
    if (!isConnected) {
      throw new Error('WebSocket 未连接')
    }
    
    try {
      // 使用 WebSocket 服务发送消息
      // 注意：实际的发送方法需要根据 wsService 的实现来调整
      console.log('发送消息:', message)
      // await wsService.send(JSON.stringify(message))
      
      // 添加到消息历史
      get().addMessageToHistory({
        ...message,
        timestamp: Date.now().toString(),
        direction: 'outgoing'
      } as WebSocketMessage)
    } catch (error) {
      console.error('[WebSocket] 发送消息失败:', error)
      throw error
    }
  },

  /**
   * 添加消息处理器
   */
  addMessageHandler: (type: string, handler: MessageHandler) => {
    set((state) => ({
      messageHandlers: {
        ...state.messageHandlers,
        [type]: [...(state.messageHandlers[type] || []), handler]
      }
    }))
  },

  /**
   * 移除消息处理器
   */
  removeMessageHandler: (type: string, handler: MessageHandler) => {
    set((state) => ({
      messageHandlers: {
        ...state.messageHandlers,
        [type]: (state.messageHandlers[type] || []).filter(h => h !== handler)
      }
    }))
  },

  /**
   * 清除消息处理器
   */
  clearMessageHandlers: (type?: string) => {
    if (type) {
      set((state) => ({
        messageHandlers: {
          ...state.messageHandlers,
          [type]: []
        }
      }))
    } else {
      set({ messageHandlers: {} })
    }
  },

  /**
   * 处理消息内容
   */
  processMessage: (message: WebSocketMessage) => {
    const { updateVMStatus, updateTaskProgress, updateSystemMetrics, addNotification } = get()
    
    switch (message.type) {
      case 'VM_STATUS_UPDATE':
        if (message.data && typeof message.data === 'object') {
          const data = message.data as any
          if (data.vmId && data.status) {
            updateVMStatus(data.vmId, data.status)
          }
        }
        break
        
      case 'TASK_PROGRESS_UPDATE':
        if (message.data && typeof message.data === 'object') {
          const data = message.data as any
          if (data.taskId && data.progress) {
            updateTaskProgress(data.taskId, data.progress)
          }
        }
        break
        
      case 'SYSTEM_METRICS_UPDATE':
        if (message.data && typeof message.data === 'object') {
          const data = message.data as any
          if (data.metrics) {
            updateSystemMetrics(data.metrics)
          }
        }
        break
        
      case 'NOTIFICATION':
        if (message.data && typeof message.data === 'object') {
          addNotification({
            id: Date.now().toString(),
            timestamp: Date.now(),
            ...(message.data as object)
          })
        }
        break
        
      default:
        // 其他类型的消息
        console.log('[WebSocket] 收到未处理的消息类型:', message.type)
        break
    }
  },

  // ==================== 实时数据更新 ====================
  
  /**
   * 更新虚拟机状态
   */
  updateVMStatus: (vmId: string, status: any) => {
    set((state) => ({
      realtimeData: {
        ...state.realtimeData,
        vmStatus: {
          ...state.realtimeData.vmStatus,
          [vmId]: {
            ...status,
            lastUpdated: Date.now()
          }
        }
      }
    }))
  },

  /**
   * 更新任务进度
   */
  updateTaskProgress: (taskId: string, progress: any) => {
    set((state) => ({
      realtimeData: {
        ...state.realtimeData,
        taskProgress: {
          ...state.realtimeData.taskProgress,
          [taskId]: {
            ...progress,
            lastUpdated: Date.now()
          }
        }
      }
    }))
  },

  /**
   * 更新系统指标
   */
  updateSystemMetrics: (metrics: any) => {
    set((state) => ({
      realtimeData: {
        ...state.realtimeData,
        systemMetrics: {
          ...metrics,
          lastUpdated: Date.now()
        }
      }
    }))
  },

  /**
   * 添加通知
   */
  addNotification: (notification: any) => {
    set((state) => ({
      realtimeData: {
        ...state.realtimeData,
        notifications: [notification, ...state.realtimeData.notifications].slice(0, 50) // 保留最新50条
      }
    }))
  },

  /**
   * 清除通知
   */
  clearNotifications: () => {
    set((state) => ({
      realtimeData: {
        ...state.realtimeData,
        notifications: []
      }
    }))
  },

  // ==================== 状态管理 ====================
  
  /**
   * 设置连接状态
   */
  setConnectionStatus: (status: ConnectionState) => {
    set({ connectionStatus: status })
  },

  /**
   * 设置连接错误
   */
  setConnectionError: (error: string | null) => {
    set({ connectionError: error })
  },

  /**
   * 增加重连尝试次数
   */
  incrementReconnectAttempts: () => {
    set((state) => ({
      reconnectAttempts: state.reconnectAttempts + 1
    }))
  },

  /**
   * 重置重连尝试次数
   */
  resetReconnectAttempts: () => {
    set({ reconnectAttempts: 0 })
  },

  // ==================== 配置管理 ====================
  
  /**
   * 设置配置
   */
  setConfig: (config: WebSocketConfig) => {
    set({ config })
  },

  // ==================== 消息历史 ====================
  
  /**
   * 添加消息到历史
   */
  addMessageToHistory: (message: WebSocketMessage) => {
    set((state) => ({
      messageHistory: [
        message,
        ...state.messageHistory
      ].slice(0, state.maxHistorySize)
    }))
  },

  /**
   * 清除消息历史
   */
  clearMessageHistory: () => {
    set({ messageHistory: [] })
  },

  // ==================== 状态重置 ====================
  
  /**
   * 重置状态
   */
  resetState: () => {
    set(initialState)
  }
}))

// ==================== 导出类型 ====================
export type { WebSocketState, WebSocketActions, WebSocketStore }