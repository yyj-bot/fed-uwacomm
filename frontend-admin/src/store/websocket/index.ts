/**
 * WebSocket 模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

export { useWebSocketStore } from './websocketStore'
export { 
  useWebSocket, 
  useWebSocketMessage, 
  useVMStatusUpdate, 
  useTaskProgressUpdate 
} from './useWebSocketStore'
export type { WebSocketState, WebSocketActions, WebSocketStore } from './websocketStore'

// 默认导出 hook
export { useWebSocket as default } from './useWebSocketStore'
