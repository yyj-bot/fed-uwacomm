/**
 * WebSocket服务统一导出
 * 严格遵循 WebSocket协议文档-中心化实现.md
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { WebSocketService } from './websocket-service'

// 导出服务实例（使用默认配置）
export const wsService = WebSocketService.getInstance()

// 导出WebSocket服务实例（兼容旧代码）
export const websocketService = wsService

// 导出类型
export type {
  WebSocketMessage,
  ConnectionStatus,
  WebSocketConfig,
  MessageHandler,
  ConnectionStateHandler,
  ErrorHandler,
  ConnectMessage,
  HeartbeatMessage,
  StatusQueryMessage,
  TrainingStartMessage,
  TrainingStopMessage,
  TrainingProgressMessage,
  ModelUploadMessage,
  ModelDownloadMessage,
  VMStartMessage,
  VMStopMessage,
  DatasetCreateMessage,
  DatasetAppendRowsMessage,
  DatasetCompleteMessage,
  ErrorMessage,
  IWebSocketService,
  WebSocketEvent,
  WebSocketEventListener
} from './types'

// 导出类
export { WebSocketService } from './websocket-service'
export { ConnectionManager } from './connection-manager'
export { MessageHandler as WSMessageHandler } from './message-handler'

// 导出枚举
export { ConnectionState } from './types'

// 默认导出
export default wsService

/**
 * 快速创建WebSocket服务实例
 * @param config 配置选项
 * @returns WebSocket服务实例
 */
export function createWebSocketService(config?: Partial<import('./types').WebSocketConfig>) {
  return WebSocketService.getInstance(config)
}

/**
 * WebSocket工具函数
 */
export const WebSocketUtils = {
  /**
   * 检查WebSocket是否支持
   */
  isSupported(): boolean {
    return typeof WebSocket !== 'undefined' || typeof window !== 'undefined'
  },

  /**
   * 获取推荐的WebSocket URL
   * @param baseUrl 基础URL
   * @param secure 是否使用安全连接
   */
  getRecommendedUrl(baseUrl: string = 'localhost:8080', secure: boolean = false): string {
    const protocol = secure ? 'wss:' : 'ws:'
    const httpProtocol = secure ? 'https:' : 'http:'
    
    if (baseUrl.startsWith('http')) {
      return baseUrl.replace(/^https?:/, httpProtocol) + '/ws'
    }
    
    return `${httpProtocol}//${baseUrl}/ws`
  },

  /**
   * 验证vmId格式
   * @param vmId 虚拟机ID
   */
  isValidVmId(vmId: string): boolean {
    // 按照协议文档，vmId为32位UUID格式
    return /^[a-f0-9]{32}$/i.test(vmId) || vmId === 'admin-client'
  }
}
