// WebSocket连接状态
export type ConnectionState = 'connecting' | 'connected' | 'disconnected' | 'error' | 'reconnecting'

// WebSocket消息类型
export interface WebSocketMessage {
  type: string
  data: unknown
  timestamp: string
}

// WebSocket配置
export interface WebSocketConfig {
  url: string
  reconnectInterval: number
  maxReconnectAttempts: number
  heartbeatInterval: number
} 