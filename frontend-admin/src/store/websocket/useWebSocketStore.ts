/**
 * WebSocket Hook - 封装 WebSocket 连接状态和操作
 * 为组件层提供简洁的 WebSocket 功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback, useEffect, useRef } from 'react'
import { useWebSocketStore } from './websocketStore'
import { ConnectionState } from '@/services'
import type { 
  WebSocketMessage,
  ConnectionStatus,
  WebSocketConfig,
  MessageHandler
} from '@/services'

// ==================== Hook 实现 ====================

export const useWebSocket = () => {
  // 获取状态
  const isConnected = useWebSocketStore((state) => state.isConnected)
  const connectionStatus = useWebSocketStore((state) => state.connectionStatus)
  const connectionError = useWebSocketStore((state) => state.connectionError)
  const reconnectAttempts = useWebSocketStore((state) => state.reconnectAttempts)
  const maxReconnectAttempts = useWebSocketStore((state) => state.maxReconnectAttempts)
  const connectionStats = useWebSocketStore((state) => state.connectionStats)
  const lastMessage = useWebSocketStore((state) => state.lastMessage)
  const messageHistory = useWebSocketStore((state) => state.messageHistory)
  const realtimeData = useWebSocketStore((state) => state.realtimeData)
  const config = useWebSocketStore((state) => state.config)

  // 获取操作方法
  const connectAction = useWebSocketStore((state) => state.connect)
  const disconnectAction = useWebSocketStore((state) => state.disconnect)
  const reconnectAction = useWebSocketStore((state) => state.reconnect)
  const sendMessageAction = useWebSocketStore((state) => state.sendMessage)
  const addMessageHandlerAction = useWebSocketStore((state) => state.addMessageHandler)
  const removeMessageHandlerAction = useWebSocketStore((state) => state.removeMessageHandler)
  const clearMessageHandlersAction = useWebSocketStore((state) => state.clearMessageHandlers)
  const updateVMStatusAction = useWebSocketStore((state) => state.updateVMStatus)
  const updateTaskProgressAction = useWebSocketStore((state) => state.updateTaskProgress)
  const updateSystemMetricsAction = useWebSocketStore((state) => state.updateSystemMetrics)
  const addNotificationAction = useWebSocketStore((state) => state.addNotification)
  const clearNotificationsAction = useWebSocketStore((state) => state.clearNotifications)
  const clearMessageHistoryAction = useWebSocketStore((state) => state.clearMessageHistory)
  const resetStateAction = useWebSocketStore((state) => state.resetState)

  // 用于存储处理器引用的 ref
  const handlersRef = useRef<Map<string, Set<MessageHandler>>>(new Map())

  // ==================== 封装操作方法 ====================

  /**
   * 连接 WebSocket
   */
  const connect = useCallback(async (config?: WebSocketConfig) => {
    try {
      await connectAction(config)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'WebSocket 连接失败'
      return { success: false, error: errorMessage }
    }
  }, [connectAction])

  /**
   * 断开 WebSocket 连接
   */
  const disconnect = useCallback(async () => {
    try {
      await disconnectAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'WebSocket 断开失败'
      return { success: false, error: errorMessage }
    }
  }, [disconnectAction])

  /**
   * 重新连接 WebSocket
   */
  const reconnect = useCallback(async () => {
    try {
      await reconnectAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'WebSocket 重连失败'
      return { success: false, error: errorMessage }
    }
  }, [reconnectAction])

  /**
   * 发送消息
   */
  const sendMessage = useCallback(async (message: WebSocketMessage) => {
    try {
      await sendMessageAction(message)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '发送消息失败'
      return { success: false, error: errorMessage }
    }
  }, [sendMessageAction])

  /**
   * 添加消息处理器
   */
  const addMessageHandler = useCallback((type: string, handler: MessageHandler) => {
    // 存储处理器引用
    if (!handlersRef.current.has(type)) {
      handlersRef.current.set(type, new Set())
    }
    handlersRef.current.get(type)!.add(handler)
    
    addMessageHandlerAction(type, handler)
  }, [addMessageHandlerAction])

  /**
   * 移除消息处理器
   */
  const removeMessageHandler = useCallback((type: string, handler: MessageHandler) => {
    // 移除处理器引用
    const handlers = handlersRef.current.get(type)
    if (handlers) {
      handlers.delete(handler)
      if (handlers.size === 0) {
        handlersRef.current.delete(type)
      }
    }
    
    removeMessageHandlerAction(type, handler)
  }, [removeMessageHandlerAction])

  /**
   * 清除消息处理器
   */
  const clearMessageHandlers = useCallback((type?: string) => {
    if (type) {
      handlersRef.current.delete(type)
    } else {
      handlersRef.current.clear()
    }
    
    clearMessageHandlersAction(type)
  }, [clearMessageHandlersAction])

  /**
   * 更新虚拟机状态
   */
  const updateVMStatus = useCallback((vmId: string, status: any) => {
    updateVMStatusAction(vmId, status)
  }, [updateVMStatusAction])

  /**
   * 更新任务进度
   */
  const updateTaskProgress = useCallback((taskId: string, progress: any) => {
    updateTaskProgressAction(taskId, progress)
  }, [updateTaskProgressAction])

  /**
   * 更新系统指标
   */
  const updateSystemMetrics = useCallback((metrics: any) => {
    updateSystemMetricsAction(metrics)
  }, [updateSystemMetricsAction])

  /**
   * 添加通知
   */
  const addNotification = useCallback((notification: any) => {
    addNotificationAction(notification)
  }, [addNotificationAction])

  /**
   * 清除通知
   */
  const clearNotifications = useCallback(() => {
    clearNotificationsAction()
  }, [clearNotificationsAction])

  /**
   * 清除消息历史
   */
  const clearMessageHistory = useCallback(() => {
    clearMessageHistoryAction()
  }, [clearMessageHistoryAction])

  /**
   * 重置状态
   */
  const resetState = useCallback(() => {
    handlersRef.current.clear()
    resetStateAction()
  }, [resetStateAction])

  // ==================== 计算属性 ====================

  /**
   * 获取虚拟机状态
   */
  const getVMStatus = useCallback((vmId: string) => {
    return realtimeData.vmStatus[vmId] || null
  }, [realtimeData.vmStatus])

  /**
   * 获取任务进度
   */
  const getTaskProgress = useCallback((taskId: string) => {
    return realtimeData.taskProgress[taskId] || null
  }, [realtimeData.taskProgress])

  /**
   * 获取系统指标
   */
  const getSystemMetrics = useCallback(() => {
    return realtimeData.systemMetrics
  }, [realtimeData.systemMetrics])

  /**
   * 获取通知列表
   */
  const getNotifications = useCallback(() => {
    return realtimeData.notifications
  }, [realtimeData.notifications])

  /**
   * 获取未读通知数量
   */
  const getUnreadNotificationCount = useCallback(() => {
    return realtimeData.notifications.filter(n => !n.read).length
  }, [realtimeData.notifications])

  /**
   * 检查是否正在连接
   */
  const isConnecting = connectionStatus === ConnectionState.CONNECTING

  /**
   * 检查是否已断开连接
   */
  const isDisconnected = connectionStatus === ConnectionState.DISCONNECTED

  /**
   * 检查是否可以重连
   */
  const canReconnect = !isConnected && !isConnecting && reconnectAttempts < maxReconnectAttempts

  /**
   * 获取连接时长
   */
  const getConnectionUptime = useCallback(() => {
    if (!connectionStats.connectedAt) return 0
    if (connectionStats.disconnectedAt && connectionStats.disconnectedAt > connectionStats.connectedAt) {
      return connectionStats.disconnectedAt - connectionStats.connectedAt
    }
    return Date.now() - connectionStats.connectedAt
  }, [connectionStats])

  /**
   * 获取连接质量
   */
  const getConnectionQuality = useCallback(() => {
    const uptime = getConnectionUptime()
    const errorRate = connectionStats.totalMessages > 0 ? connectionStats.totalErrors / connectionStats.totalMessages : 0
    
    if (errorRate > 0.1 || uptime < 10000) return 'poor'
    if (errorRate > 0.05 || uptime < 60000) return 'fair'
    if (errorRate > 0.01 || uptime < 300000) return 'good'
    return 'excellent'
  }, [connectionStats, getConnectionUptime])

  // ==================== 自动重连逻辑 ====================

  useEffect(() => {
    let reconnectTimer: NodeJS.Timeout | null = null
    
    if (connectionStatus === ConnectionState.DISCONNECTED && connectionError && canReconnect) {
      // 指数退避策略
      const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000)
      
      reconnectTimer = setTimeout(() => {
        console.log(`[WebSocket] 尝试自动重连 (第${reconnectAttempts + 1}次)...`)
        reconnect().catch(error => {
          console.error('[WebSocket] 自动重连失败:', error)
        })
      }, delay)
    }
    
    return () => {
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
      }
    }
  }, [connectionStatus, connectionError, canReconnect, reconnectAttempts, reconnect])

  // ==================== 组件卸载时清理 ====================

  useEffect(() => {
    return () => {
      // 清理所有处理器
      handlersRef.current.clear()
    }
  }, [])

  // ==================== 返回接口 ====================

  return {
    // 状态
    isConnected,
    connectionStatus,
    connectionError,
    reconnectAttempts,
    maxReconnectAttempts,
    connectionStats,
    lastMessage,
    messageHistory,
    realtimeData,
    config,
    
    // 计算属性
    isConnecting,
    isDisconnected,
    canReconnect,
    
    // 操作方法
    connect,
    disconnect,
    reconnect,
    sendMessage,
    addMessageHandler,
    removeMessageHandler,
    clearMessageHandlers,
    updateVMStatus,
    updateTaskProgress,
    updateSystemMetrics,
    addNotification,
    clearNotifications,
    clearMessageHistory,
    resetState,
    
    // 工具方法
    getVMStatus,
    getTaskProgress,
    getSystemMetrics,
    getNotifications,
    getUnreadNotificationCount,
    getConnectionUptime,
    getConnectionQuality
  }
}

// ==================== 专用 Hooks ====================

/**
 * 监听特定类型的消息
 */
export const useWebSocketMessage = (type: string, handler: MessageHandler) => {
  const { addMessageHandler, removeMessageHandler } = useWebSocket()
  
  useEffect(() => {
    addMessageHandler(type, handler)
    
    return () => {
      removeMessageHandler(type, handler)
    }
  }, [type, handler, addMessageHandler, removeMessageHandler])
}

/**
 * 监听虚拟机状态更新
 */
export const useVMStatusUpdate = (vmId: string, callback: (status: any) => void) => {
  const { getVMStatus } = useWebSocket()
  
  useEffect(() => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'VM_STATUS_UPDATE' && message.data && typeof message.data === 'object') {
        const data = message.data as any
        if (data.vmId === vmId) {
          callback(data.status)
        }
      }
    }
    
    useWebSocketMessage('VM_STATUS_UPDATE', handler)
  }, [vmId, callback])
  
  return getVMStatus(vmId)
}

/**
 * 监听任务进度更新
 */
export const useTaskProgressUpdate = (taskId: string, callback: (progress: any) => void) => {
  const { getTaskProgress } = useWebSocket()
  
  useEffect(() => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'TASK_PROGRESS_UPDATE' && message.data && typeof message.data === 'object') {
        const data = message.data as any
        if (data.taskId === taskId) {
          callback(data.progress)
        }
      }
    }
    
    useWebSocketMessage('TASK_PROGRESS_UPDATE', handler)
  }, [taskId, callback])
  
  return getTaskProgress(taskId)
}

// ==================== 导出默认 Hook ====================
export default useWebSocket
