/**
 * WebSocket连接管理器
 * 负责连接建立、重连、状态管理
 * 严格按照 WebSocket协议文档-中心化实现.md
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import * as SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import type { 
  ConnectionStatus, 
  WebSocketConfig, 
  ConnectionStateHandler 
} from './types'
import { ConnectionState } from './types'

export class ConnectionManager {
  private stompClient: Client | null = null
  private status: ConnectionStatus = {
    connected: false,
    connecting: false,
    error: null,
    lastHeartbeat: null
  }
  
  private reconnectTimer: NodeJS.Timeout | null = null
  private reconnectAttempts = 0
  private stateHandlers: Set<ConnectionStateHandler> = new Set()
  
  constructor(private config: WebSocketConfig) {}

  /**
   * 建立连接 - 严格按照协议文档连接流程
   */
  async connect(token?: string): Promise<void> {
    if (this.status.connecting || this.status.connected) {
      return
    }

    // 获取token - 优先使用参数，然后配置，最后localStorage
    const authToken = token || this.config.token || this.getCurrentToken()
    if (!authToken) {
      throw new Error('未找到认证Token，请先登录')
    }

    this.updateStatus({ connecting: true, error: null })

    try {
      // 根据配置选择连接方式 - 按照协议文档1.3节
      let socketFactory: () => any
      
      if (this.config.enableSockJS) {
        // 使用SockJS连接（推荐用于开发）
        socketFactory = () => new SockJS(this.config.url)
      } else {
        // 使用原生WebSocket连接
        const wsUrl = this.config.url.replace(/^http/, 'ws')
        socketFactory = () => new WebSocket(wsUrl)
      }

      this.stompClient = new Client({
        webSocketFactory: socketFactory,
        connectHeaders: {
          // 严格按照协议文档0.4节 - 在STOMP CONNECT头部携带Authorization
          'Authorization': `Bearer ${authToken}`,
          'vmId': this.config.vmId,
          'accept-version': '1.2',
          'host': this.getHostFromUrl(this.config.url)
        },
        debug: this.config.debug ? (str) => {
          console.log('[WebSocket Debug]', str)
        } : undefined,
        reconnectDelay: 0, // 禁用STOMP自动重连，使用自定义重连逻辑
        heartbeatIncoming: 30000, // 30秒心跳间隔，按照协议文档
        heartbeatOutgoing: 30000,
      })

      // 设置事件处理器
      this.setupEventHandlers()

      // 激活连接
      this.stompClient.activate()

      // 设置连接超时
      setTimeout(() => {
        if (this.status.connecting) {
          this.handleConnectionError(new Error('连接超时'))
        }
      }, this.config.connectTimeout)

    } catch (error) {
      this.handleConnectionError(error instanceof Error ? error : new Error('连接初始化失败'))
    }
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.stompClient) {
      this.stompClient.deactivate()
      this.stompClient = null
    }

    this.updateStatus({
      connected: false,
      connecting: false,
      error: null
    })

    this.reconnectAttempts = 0
  }

  /**
   * 获取STOMP客户端
   */
  getStompClient(): Client | null {
    return this.stompClient
  }

  /**
   * 获取连接状态
   */
  getStatus(): ConnectionStatus {
    return { ...this.status }
  }

  /**
   * 获取连接状态枚举
   */
  getState(): ConnectionState {
    if (this.status.connected) {
      return ConnectionState.CONNECTED
    } else if (this.status.connecting) {
      return this.reconnectAttempts > 0 ? ConnectionState.RECONNECTING : ConnectionState.CONNECTING
    } else if (this.status.error) {
      return ConnectionState.ERROR
    } else {
      return ConnectionState.DISCONNECTED
    }
  }

  /**
   * 是否已连接
   */
  isConnected(): boolean {
    return this.status.connected
  }

  /**
   * 更新心跳时间
   */
  updateHeartbeat(): void {
    this.updateStatus({ lastHeartbeat: new Date() })
  }

  /**
   * 注册状态变化监听器
   */
  onStateChange(handler: ConnectionStateHandler): () => void {
    this.stateHandlers.add(handler)
    return () => this.stateHandlers.delete(handler)
  }

  /**
   * 设置事件处理器
   */
  private setupEventHandlers(): void {
    if (!this.stompClient) return

    // 连接成功
    this.stompClient.onConnect = (frame) => {
      console.log('[WebSocket] 连接成功:', frame)
      
      this.updateStatus({
        connected: true,
        connecting: false,
        error: null,
        sessionId: frame.headers['session'] || undefined
      })
      
      this.reconnectAttempts = 0
    }

    // STOMP错误
    this.stompClient.onStompError = (frame) => {
      console.error('[WebSocket] STOMP错误:', frame)
      const error = new Error(frame.headers['message'] || 'STOMP连接错误')
      this.handleConnectionError(error)
    }

    // WebSocket错误
    this.stompClient.onWebSocketError = (error) => {
      console.error('[WebSocket] WebSocket错误:', error)
      this.handleConnectionError(new Error('网络连接错误'))
    }

    // 连接断开
    this.stompClient.onDisconnect = () => {
      console.log('[WebSocket] 连接断开')
      
      this.updateStatus({
        connected: false,
        connecting: false
      })
      
      this.scheduleReconnect()
    }
  }

  /**
   * 处理连接错误
   */
  private handleConnectionError(error: Error): void {
    console.error('[WebSocket] 连接错误:', error)
    
    this.updateStatus({
      connected: false,
      connecting: false,
      error: error.message
    })
    
    this.scheduleReconnect()
  }

  /**
   * 安排重连 - 按照协议文档6.4节重连策略
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.config.maxReconnectAttempts) {
      console.error('[WebSocket] 达到最大重连次数，停止重连')
      this.updateStatus({ error: '达到最大重连次数' })
      return
    }

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }

    // 指数退避算法 - 严格按照协议文档
    // 重连间隔从1秒开始，最大60秒
    const baseDelay = 1000 // 1秒基础延迟
    const delay = Math.min(
      baseDelay * Math.pow(2, this.reconnectAttempts),
      60000 // 最大60秒，按照协议文档要求
    )

    console.log(`[WebSocket] ${delay}ms后尝试重连 (第${this.reconnectAttempts + 1}次)`)

    this.reconnectTimer = setTimeout(async () => {
      this.reconnectAttempts++
      
      // 获取当前有效的token
      const token = this.getCurrentToken()
      if (token) {
        await this.connect(token)
      } else {
        console.error('[WebSocket] 重连失败：无法获取有效token')
        this.updateStatus({ error: '重连失败：认证token无效' })
      }
    }, delay)
  }

  /**
   * 更新连接状态
   */
  private updateStatus(updates: Partial<ConnectionStatus>): void {
    const prevState = this.getState()
    this.status = { ...this.status, ...updates }
    const newState = this.getState()

    // 如果状态发生变化，通知监听器
    if (prevState !== newState) {
      this.stateHandlers.forEach(handler => {
        try {
          handler(newState)
        } catch (error) {
          console.error('状态变化监听器执行失败:', error)
        }
      })
    }
  }

  /**
   * 从URL中提取主机名
   */
  private getHostFromUrl(url: string): string {
    try {
      const urlObj = new URL(url)
      return urlObj.hostname
    } catch {
      return 'localhost'
    }
  }

  /**
   * 获取当前有效的token
   * 直接从localStorage获取，避免循环依赖
   */
  private getCurrentToken(): string | null {
    return localStorage.getItem('access_token')
  }

  /**
   * 获取重连尝试次数
   */
  getReconnectAttempts(): number {
    return this.reconnectAttempts
  }

  /**
   * 销毁连接管理器
   */
  destroy(): void {
    this.disconnect()
    this.stateHandlers.clear()
  }
}
