/**
 * WebSocket服务 - 重构版本
 * 严格遵循 WebSocket协议文档-中心化实现.md
 * 职责：WebSocket连接管理、消息收发、状态监控
 */

import { ConnectionManager } from './connection-manager'
import { MessageHandler } from './message-handler'
import type { 
  IWebSocketService,
  WebSocketConfig,
  ConnectionStatus,
  ConnectionState,
  WebSocketMessage,
  MessageHandler as MessageHandlerType,
  ConnectionStateHandler,
  ErrorHandler,
  ConnectMessage,
  HeartbeatMessage,
  StatusQueryMessage
} from './types'

export class WebSocketService implements IWebSocketService {
  private static instance: WebSocketService
  private connectionManager: ConnectionManager
  private messageHandler: MessageHandler
  private heartbeatTimer: NodeJS.Timeout | null = null
  private config: WebSocketConfig

  // 默认配置
  private static readonly DEFAULT_CONFIG: WebSocketConfig = {
    url: 'http://localhost:8080/ws', // 开发环境，生产环境应使用 wss://
    enableSockJS: true,
    vmId: 'admin-client', // 管理员客户端标识
    heartbeatInterval: 30000, // 30秒心跳间隔
    reconnectDelay: 5000, // 5秒重连延迟
    maxReconnectAttempts: 10, // 最大重连次数
    connectTimeout: 30000 // 30秒连接超时
  }

  private constructor(config?: Partial<WebSocketConfig>) {
    this.config = { ...WebSocketService.DEFAULT_CONFIG, ...config }
    this.connectionManager = new ConnectionManager(this.config)
    this.messageHandler = new MessageHandler(this.connectionManager, this.config.vmId)
    
    this.setupEventHandlers()
  }

  /**
   * 获取单例实例
   */
  public static getInstance(config?: Partial<WebSocketConfig>): WebSocketService {
    if (!WebSocketService.instance) {
      WebSocketService.instance = new WebSocketService(config)
    }
    return WebSocketService.instance
  }

  /**
   * 连接到WebSocket服务器
   */
  async connect(): Promise<void> {
    try {
      // 获取认证Token
      const token = this.getAuthToken()
      if (!token) {
        throw new Error('未找到认证Token，请先登录')
      }

      // 建立连接
      await this.connectionManager.connect(token)

      // 连接成功后发送CONNECT消息
      this.sendConnectMessage()

      // 启动心跳
      this.startHeartbeat()

      console.log('[WebSocket] 连接建立成功')
    } catch (error) {
      console.error('[WebSocket] 连接失败:', error)
      throw error
    }
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    this.stopHeartbeat()
    this.connectionManager.disconnect()
    console.log('[WebSocket] 连接已断开')
  }

  /**
   * 检查是否已连接
   */
  isConnected(): boolean {
    return this.connectionManager.isConnected()
  }

  /**
   * 获取连接状态
   */
  getStatus(): ConnectionStatus {
    return this.connectionManager.getStatus()
  }

  /**
   * 获取连接状态枚举
   */
  getState(): ConnectionState {
    return this.connectionManager.getState()
  }

  /**
   * 发送消息
   */
  send(message: Omit<WebSocketMessage, 'id' | 'timestamp' | 'vmId'>): void {
    this.messageHandler.send(message)
  }

  /**
   * 发送连接消息
   */
  sendConnect(data: ConnectMessage): void {
    this.messageHandler.sendConnect(data)
  }

  /**
   * 发送心跳消息
   */
  sendHeartbeat(data: HeartbeatMessage): void {
    this.messageHandler.sendHeartbeat(data)
  }

  /**
   * 发送状态查询
   */
  sendStatusQuery(data: StatusQueryMessage): void {
    this.messageHandler.sendStatusQuery(data)
  }

  /**
   * 发送VM控制命令
   */
  sendVMControl(action: 'start' | 'stop', vmId: string, options?: any): void {
    const messageType = action === 'start' ? 'VM_START' : 'VM_STOP'
    this.send({
      type: messageType,
      data: {
        vmId,
        ...options
      }
    })
  }

  /**
   * 发送训练控制命令
   */
  sendTrainingControl(action: 'start' | 'stop', taskId: string, options?: any): void {
    const messageType = action === 'start' ? 'TRAINING_START' : 'TRAINING_STOP'
    this.send({
      type: messageType,
      data: {
        taskId,
        ...options
      }
    })
  }

  /**
   * 注册消息处理器
   */
  onMessage(type: string, handler: MessageHandlerType): () => void {
    return this.messageHandler.onMessage(type, handler)
  }

  /**
   * 注册状态变化监听器
   */
  onStateChange(handler: ConnectionStateHandler): () => void {
    return this.connectionManager.onStateChange(handler)
  }

  /**
   * 注册错误处理器
   */
  onError(handler: ErrorHandler): () => void {
    return this.messageHandler.addEventListener('ERROR', (event) => {
      const error = event.payload as any
      handler(new Error(error.message?.errorMessage || '未知错误'))
    })
  }

  /**
   * 兼容性接口 - 订阅消息
   */
  subscribe(messageType: string, handler: MessageHandlerType): () => void {
    return this.onMessage(messageType, handler)
  }

  /**
   * 设置事件处理器
   */
  private setupEventHandlers(): void {
    // 监听连接状态变化
    this.onStateChange((state) => {
      console.log('[WebSocket] 连接状态变化:', state)
      
      if (state === ConnectionState.CONNECTED) {
        this.startHeartbeat()
      } else if (state === ConnectionState.DISCONNECTED || state === ConnectionState.ERROR) {
        this.stopHeartbeat()
      }
    })

    // 监听特定消息类型
    this.onMessage('CONNECT_ACK', (message) => {
      console.log('[WebSocket] 收到连接确认:', message.data)
    })

    this.onMessage('HEARTBEAT_ACK', (message) => {
      console.log('[WebSocket] 收到心跳响应')
    })

    this.onMessage('ERROR', (message) => {
      console.error('[WebSocket] 收到错误消息:', message.data)
    })
  }

  /**
   * 发送初始连接消息
   */
  private sendConnectMessage(): void {
    const connectData: ConnectMessage = {
      version: '1.0.0',
      capabilities: ['STATUS_QUERY', 'VM_CONTROL', 'TRAINING_CONTROL', 'DATASET_MANAGEMENT'],
      systemInfo: {
        os: navigator.platform,
        // 其他系统信息可以根据需要添加
      }
    }

    this.sendConnect(connectData)
  }

  /**
   * 启动心跳
   */
  private startHeartbeat(): void {
    this.stopHeartbeat()

    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected()) {
        const heartbeatData: HeartbeatMessage = {
          status: 'ACTIVE',
          resourceUsage: this.getClientResourceUsage()
        }

        this.sendHeartbeat(heartbeatData)
      }
    }, this.config.heartbeatInterval)

    console.log(`[WebSocket] 心跳已启动，间隔: ${this.config.heartbeatInterval}ms`)
  }

  /**
   * 停止心跳
   */
  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
      console.log('[WebSocket] 心跳已停止')
    }
  }

  /**
   * 获取客户端资源使用情况
   */
  private getClientResourceUsage(): any {
    // 浏览器环境中获取资源使用情况的方法有限
    // 这里返回一些基本信息
    return {
      cpu: 0, // 浏览器无法直接获取CPU使用率
      memory: (performance as any).memory ? {
        used: (performance as any).memory.usedJSHeapSize,
        total: (performance as any).memory.totalJSHeapSize,
        limit: (performance as any).memory.jsHeapSizeLimit
      } : undefined,
      timestamp: Date.now()
    }
  }

  /**
   * 获取认证Token
   */
  private getAuthToken(): string | null {
    // 这里应该从AuthService获取token
    // 为了避免循环依赖，直接从localStorage获取
    return localStorage.getItem('access_token')
  }

  /**
   * 更新配置
   */
  updateConfig(config: Partial<WebSocketConfig>): void {
    this.config = { ...this.config, ...config }
    console.log('[WebSocket] 配置已更新:', this.config)
  }

  /**
   * 获取当前配置
   */
  getConfig(): WebSocketConfig {
    return { ...this.config }
  }

  /**
   * 获取连接统计信息
   */
  getConnectionStats(): {
    queueLength: number
    reconnectAttempts: number
    lastHeartbeat: Date | null
    connectionDuration: number
  } {
    const status = this.getStatus()
    return {
      queueLength: this.messageHandler.getQueueLength(),
      reconnectAttempts: 0, // TODO: 从ConnectionManager获取
      lastHeartbeat: status.lastHeartbeat,
      connectionDuration: status.lastHeartbeat ? Date.now() - status.lastHeartbeat.getTime() : 0
    }
  }

  /**
   * 销毁服务
   */
  destroy(): void {
    this.stopHeartbeat()
    this.connectionManager.destroy()
    this.messageHandler.destroy()
    
    // 清除单例引用
    WebSocketService.instance = null as any
    
    console.log('[WebSocket] 服务已销毁')
  }
}
