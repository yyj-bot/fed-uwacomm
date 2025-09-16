/**
 * WebSocket Store Mock 数据
 * 用于测试WebSocket连接相关功能的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { 
  WebSocketMessage,
  ConnectionState,
  WebSocketConfig,
  MessageHandler,
  ConnectionStateHandler,
  ErrorHandler
} from '@/services'

// ==================== 模拟WebSocket消息 ====================

export const mockWebSocketMessage: WebSocketMessage = {
  id: 'msg-001',
  type: 'vm-status-update',
  data: {
    vmId: 'vm-001',
    status: 'RUNNING',
    cpuUsage: 45.5,
    memoryUsage: 60.2
  },
  timestamp: new Date().toISOString(),
  vmId: 'vm-001'
}

export const mockTaskProgressMessage: WebSocketMessage = {
  id: 'msg-002',
  type: 'task-progress',
  data: {
    taskId: 'task-001',
    progress: 75,
    status: 'RUNNING',
    message: '正在训练模型...'
  },
  timestamp: new Date().toISOString(),
  vmId: 'vm-001'
}

export const mockSystemMetricsMessage: WebSocketMessage = {
  id: 'msg-003',
  type: 'system-metrics',
  data: {
    cpuUsage: 35.2,
    memoryUsage: 68.5,
    diskUsage: 45.8,
    networkIn: 1024000,
    networkOut: 512000,
    activeConnections: 25
  },
  timestamp: new Date().toISOString(),
  vmId: 'vm-001'
}

export const mockErrorMessage: WebSocketMessage = {
  id: 'msg-004',
  type: 'error',
  data: {
    code: 'VM_START_FAILED',
    message: '虚拟机启动失败：内存不足',
    vmId: 'vm-003'
  },
  timestamp: new Date().toISOString(),
  vmId: 'vm-003'
}

export const mockNotificationMessage: WebSocketMessage = {
  id: 'msg-005',
  type: 'notification',
  data: {
    title: '任务完成',
    message: '联邦学习任务 "MNIST训练" 已成功完成',
    level: 'success',
    taskId: 'task-001'
  },
  timestamp: new Date().toISOString(),
  vmId: 'vm-001'
}

export const mockMessageHistory = [
  mockWebSocketMessage,
  mockTaskProgressMessage,
  mockSystemMetricsMessage,
  mockErrorMessage,
  mockNotificationMessage
]

// ==================== 模拟WebSocket配置 ====================

export const mockWebSocketConfig: WebSocketConfig = {
  url: 'ws://localhost:8080/ws',
  enableSockJS: false,
  vmId: 'vm-001',
  token: 'mock-jwt-token',
  heartbeatInterval: 30000,
  reconnectDelay: 3000,
  maxReconnectAttempts: 5,
  connectTimeout: 10000,
  debug: true
}

// ==================== 模拟处理器函数 ====================

export const mockMessageHandler: MessageHandler = (message: WebSocketMessage) => {
  console.log('VM状态更新:', message.data)
}

export const mockConnectionStateHandler: ConnectionStateHandler = (state: ConnectionState) => {
  console.log('WebSocket状态变化:', state)
}

export const mockErrorHandler: ErrorHandler = (error: Error) => {
  console.error('WebSocket处理错误:', error)
}

// ==================== 模拟实时数据 ====================

export const mockRealtimeVMStatus = {
  'vm-001': {
    status: 'RUNNING',
    cpuUsage: 45.5,
    memoryUsage: 60.2,
    diskUsage: 35.8,
    networkIn: 1024000,
    networkOut: 512000,
    lastUpdated: Date.now()
  },
  'vm-002': {
    status: 'STOPPED',
    cpuUsage: 0,
    memoryUsage: 0,
    diskUsage: 25.3,
    networkIn: 0,
    networkOut: 0,
    lastUpdated: Date.now()
  }
}

export const mockRealtimeTaskProgress = {
  'task-001': {
    progress: 75,
    status: 'RUNNING',
    message: '正在训练模型...',
    currentRound: 8,
    totalRounds: 10,
    lastUpdated: Date.now()
  },
  'task-002': {
    progress: 100,
    status: 'COMPLETED',
    message: '训练完成',
    currentRound: 10,
    totalRounds: 10,
    lastUpdated: Date.now()
  }
}

export const mockRealtimeSystemMetrics = {
  cpuUsage: 35.2,
  memoryUsage: 68.5,
  diskUsage: 45.8,
  networkIn: 1024000,
  networkOut: 512000,
  activeConnections: 25,
  uptime: 86400000, // 24小时
  lastUpdated: Date.now()
}

// ==================== 模拟错误响应 ====================

export const mockWebSocketErrors = {
  CONNECTION_FAILED: new Error('WebSocket连接失败'),
  CONNECTION_TIMEOUT: new Error('连接超时'),
  AUTHENTICATION_FAILED: new Error('认证失败'),
  PROTOCOL_ERROR: new Error('协议错误'),
  MESSAGE_PARSE_ERROR: new Error('消息解析失败'),
  SEND_MESSAGE_FAILED: new Error('发送消息失败'),
  RECONNECT_FAILED: new Error('重连失败'),
  SERVER_ERROR: new Error('服务器错误')
}

// ==================== 模拟状态数据 ====================

export const mockInitialWebSocketState = {
  isConnected: false,
  connectionStatus: 'DISCONNECTED' as ConnectionState,
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
    systemMetrics: {}
  },
  subscriptions: new Set(),
  config: mockWebSocketConfig
}

export const mockConnectedWebSocketState = {
  ...mockInitialWebSocketState,
  isConnected: true,
  connectionStatus: 'CONNECTED' as ConnectionState,
  connectionStats: {
    connectedAt: Date.now(),
    disconnectedAt: null,
    totalMessages: 5,
    totalErrors: 0,
    uptime: 3600000 // 1小时
  },
  lastMessage: mockWebSocketMessage,
  messageHistory: mockMessageHistory,
  realtimeData: {
    vmStatus: mockRealtimeVMStatus,
    taskProgress: mockRealtimeTaskProgress,
    systemMetrics: mockRealtimeSystemMetrics
  }
}

export const mockConnectingWebSocketState = {
  ...mockInitialWebSocketState,
  connectionStatus: 'CONNECTING' as ConnectionState
}

export const mockReconnectingWebSocketState = {
  ...mockInitialWebSocketState,
  connectionStatus: 'RECONNECTING' as ConnectionState,
  reconnectAttempts: 2
}

export const mockErrorWebSocketState = {
  ...mockInitialWebSocketState,
  connectionStatus: 'ERROR' as ConnectionState,
  connectionError: '连接失败'
}

// ==================== 模拟工具函数 ====================

export const createMockMessage = (
  type: string, 
  data: any, 
  vmId: string = 'vm-mock-001',
  overrides: Partial<WebSocketMessage> = {}
): WebSocketMessage => ({
  id: `msg-${Date.now()}`,
  type,
  data,
  timestamp: new Date().toISOString(),
  vmId,
  ...overrides
})

export const createMockVMStatusMessage = (vmId: string, status: any) =>
  createMockMessage('vm-status-update', { vmId, ...status }, vmId)

export const createMockTaskProgressMessage = (taskId: string, progress: any) =>
  createMockMessage('task-progress', { taskId, ...progress })

export const createMockSystemMetricsMessage = (metrics: any) =>
  createMockMessage('system-metrics', metrics)

// ==================== 模拟WebSocket实例 ====================

export const createMockWebSocket = () => ({
  readyState: WebSocket.OPEN,
  url: mockWebSocketConfig.url,
  protocol: 'websocket',
  send: () => {},
  close: () => {},
  addEventListener: () => {},
  removeEventListener: () => {},
  dispatchEvent: () => true,
  onopen: null,
  onclose: null,
  onmessage: null,
  onerror: null,
  CONNECTING: 0,
  OPEN: 1,
  CLOSING: 2,
  CLOSED: 3
})

// ==================== 导出默认 Mock ====================
export default {
  mockWebSocketMessage,
  mockTaskProgressMessage,
  mockSystemMetricsMessage,
  mockErrorMessage,
  mockNotificationMessage,
  mockMessageHistory,
  mockWebSocketConfig,
  mockMessageHandler,
  mockConnectionStateHandler,
  mockErrorHandler,
  mockRealtimeVMStatus,
  mockRealtimeTaskProgress,
  mockRealtimeSystemMetrics,
  mockWebSocketErrors,
  mockInitialWebSocketState,
  mockConnectedWebSocketState,
  mockConnectingWebSocketState,
  mockReconnectingWebSocketState,
  mockErrorWebSocketState,
  createMockMessage,
  createMockVMStatusMessage,
  createMockTaskProgressMessage,
  createMockSystemMetricsMessage,
  createMockWebSocket
}
