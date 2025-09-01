/**
 * WebSocket服务相关类型定义
 * 严格遵循 WebSocket协议文档-中心化实现.md
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// ==================== 基础消息类型 ====================
/**
 * WebSocket标准消息格式 - 严格按照协议文档2.1节
 */
export interface WebSocketMessage {
  readonly type: string
  readonly id: string
  readonly timestamp: string
  readonly vmId: string
  readonly data: unknown
  readonly signature?: string
}

// ==================== 连接状态 ====================
export interface ConnectionStatus {
  readonly connected: boolean
  readonly connecting: boolean
  readonly error: string | null
  readonly lastHeartbeat: Date | null
  readonly sessionId?: string
}

export enum ConnectionState {
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  RECONNECTING = 'RECONNECTING',
  ERROR = 'ERROR'
}

// ==================== 连接配置 ====================
export interface WebSocketConfig {
  readonly url: string
  readonly enableSockJS: boolean
  readonly vmId: string
  readonly token?: string
  readonly heartbeatInterval: number
  readonly reconnectDelay: number
  readonly maxReconnectAttempts: number
  readonly connectTimeout: number
  readonly debug?: boolean
}

// ==================== 消息处理器 ====================
export type MessageHandler = (message: WebSocketMessage) => void
export type ConnectionStateHandler = (state: ConnectionState) => void
export type ErrorHandler = (error: Error) => void

// ==================== 具体消息类型 ====================

// 连接消息 - 严格按照协议文档
export interface ConnectMessage {
  readonly version: string
  readonly capabilities: string[]
  readonly systemInfo: {
    readonly os: string
    readonly python?: string
    readonly memory?: string
    readonly cpu?: string
    readonly gpu?: string
  }
  readonly supportedAlgorithms?: Record<string, {
    readonly version: string
    readonly description: string
    readonly parameters?: string[]
  }>
}

export interface ConnectAckMessage {
  readonly sessionId: string
  readonly serverTime: string
  readonly heartbeatInterval: number
  readonly maxMessageSize: number
  readonly supportedFeatures: string[]
}

// 心跳消息 - 严格按照协议文档
export interface HeartbeatMessage {
  readonly status: string
  readonly resourceUsage: {
    readonly cpu: number
    readonly memory: number
    readonly disk: number
    readonly gpu?: number
  }
  readonly network?: {
    readonly uploadSpeed: number
    readonly downloadSpeed: number
    readonly latency: number
  }
  readonly processes?: {
    readonly total: number
    readonly active: number
  }
}

export interface HeartbeatAckMessage {
  readonly serverTime: string
  readonly nextHeartbeat: number
  readonly systemStatus: string
}

// VM控制消息
export interface VMStartMessage {
  readonly timeout: number
  readonly config: {
    readonly memory: string
    readonly cpu: string
    readonly disk: string
    readonly network: string
  }
  readonly environment?: {
    readonly variables: Record<string, string>
  }
}

export interface VMStopMessage {
  readonly force: boolean
  readonly timeout: number
  readonly saveState: boolean
}

// 训练控制消息
export interface TrainingStartMessage {
  readonly taskId: string
  readonly algorithm: string
  readonly config: {
    readonly batchSize: number
    readonly learningRate: number
    readonly epochsPerRound: number
    readonly totalRounds: number
    readonly currentRound: number
    readonly minClients: number
    readonly timeout: number
  }
  readonly globalModel?: {
    readonly modelId: string
    readonly version: string
    readonly parameters: Record<string, unknown>
    readonly downloadUrl?: string
  }
  readonly dataConfig?: {
    readonly dataPath: string
    readonly validationSplit: number
    readonly shuffle: boolean
  }
}

export interface TrainingStopMessage {
  readonly taskId: string
  readonly reason: string
  readonly saveCheckpoint: boolean
  readonly cleanup: boolean
}

// 训练进度消息
export interface TrainingProgressMessage {
  readonly taskId: string
  readonly currentRound: number
  readonly totalRounds: number
  readonly currentEpoch: number
  readonly epochsPerRound: number
  readonly progress: number
  readonly metrics: {
    readonly accuracy: number
    readonly loss: number
    readonly valAccuracy: number
    readonly valLoss: number
    readonly precision?: number
    readonly recall?: number
    readonly f1Score?: number
  }
  readonly status: string
  readonly estimatedTimeRemaining: number
  readonly resourceUsage?: {
    readonly cpu: number
    readonly memory: number
    readonly gpu: number
  }
}

// 模型传输消息
export interface ModelUploadMessage {
  readonly taskId: string
  readonly round: number
  readonly parameters: {
    readonly model: {
      readonly framework: string
      readonly format: string
      readonly weights: {
        readonly shape: number[]
        readonly dtype: string
        readonly checksum: string
      }
    }
    readonly training: {
      readonly epochs: number
      readonly batchSize: number
      readonly optimizer: string
      readonly learningRate: number
    }
  }
  readonly metrics: {
    readonly accuracy: number
    readonly loss: number
    readonly valAccuracy: number
    readonly valLoss: number
  }
  readonly compression?: string
}

export interface ModelDownloadMessage {
  readonly taskId: string
  readonly round: number
  readonly parameters: {
    readonly model: {
      readonly framework: string
      readonly format: string
      readonly weights: {
        readonly shape: number[]
        readonly dtype: string
        readonly checksum: string
      }
    }
    readonly aggregation: {
      readonly method: string
      readonly participation: number
    }
  }
  readonly compression?: string
}

// 状态查询消息
export interface StatusQueryMessage {
  readonly queryType: string
  readonly includeResources: boolean
  readonly includeProcesses: boolean
  readonly includeNetwork: boolean
  readonly timeout: number
}

export interface StatusResponseMessage {
  readonly status: string
  readonly uptime: number
  readonly resourceUsage: {
    readonly cpu: number
    readonly memory: number
    readonly disk: number
    readonly gpu?: number
  }
  readonly network: {
    readonly ipAddress: string
    readonly macAddress: string
    readonly port: number
    readonly uploadSpeed: number
    readonly downloadSpeed: number
    readonly latency: number
  }
  readonly processes: {
    readonly total: number
    readonly active: number
    readonly system: number
    readonly user: number
    readonly training?: number
  }
  readonly systemInfo?: {
    readonly os: string
    readonly kernel: string
    readonly loadAverage: number[]
    readonly lastBoot: string
  }
}

// 错误消息
export interface ErrorMessage {
  readonly errorCode: string
  readonly errorMessage: string
  readonly severity: string
  readonly details?: {
    readonly exception?: string
    readonly stackTrace?: string
    readonly context?: Record<string, unknown>
  }
  readonly recovery?: {
    readonly automatic: boolean
    readonly suggestions?: string[]
  }
}

// 数据集同步消息（v1.1新增）
export interface DatasetCreateMessage {
  readonly datasetId: string
  readonly datasetDescription: string
  readonly datasetType: string
  readonly metadata?: Record<string, unknown>
}

export interface DatasetAppendRowsMessage {
  readonly datasetId: string
  readonly rows: Array<{
    readonly rowData: Record<string, unknown>
  }>
}

export interface DatasetCompleteMessage {
  readonly datasetId: string
}

// ==================== 事件类型 ====================
export interface WebSocketEvent {
  readonly type: 'CONNECTION' | 'MESSAGE' | 'ERROR'
  readonly payload: unknown
  readonly timestamp: string
}

export type WebSocketEventListener = (event: WebSocketEvent) => void

// ==================== 服务接口 ====================
export interface IWebSocketService {
  // 连接管理
  connect(): Promise<void>
  disconnect(): void
  isConnected(): boolean
  getStatus(): ConnectionStatus
  getState(): ConnectionState

  // 消息发送
  send(message: Omit<WebSocketMessage, 'id' | 'timestamp' | 'vmId'>): void
  sendConnect(data: ConnectMessage): void
  sendHeartbeat(data: HeartbeatMessage): void
  sendStatusQuery(data: StatusQueryMessage): void
  
  // 事件监听
  onMessage(type: string, handler: MessageHandler): () => void
  onStateChange(handler: ConnectionStateHandler): () => void
  onError(handler: ErrorHandler): () => void
  
  // 兼容性接口
  subscribe(messageType: string, handler: MessageHandler): () => void
}
