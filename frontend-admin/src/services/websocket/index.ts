/**
 * WebSocket服务统一导出
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
  ConnectionState,
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
  IWebSocketService
} from './types'

// 导出类
export { WebSocketService } from './websocket-service'
export { ConnectionManager } from './connection-manager'
export { MessageHandler as WSMessageHandler } from './message-handler'

// 默认导出
export default wsService
