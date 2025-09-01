/**
 * WebSocket连接管理器
 * 负责连接建立、重连、状态管理
 */

import SockJS from 'sockjs-client'
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
   * 建立连接
   */
  async connect(token: string): Promise<void> {
    if (this.status.connecting || this.status.connected) {
      return
    }

    this.updateStatus({ connecting: true, error: null })

    try {
      // 根据配置选择连接方式
      const socket = this.config.enableSockJS 
        ? new SockJS(this.config.url)
        : new WebSocket(this.config.url.replace('http', 'ws'))

      this.stompClient = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
          'Authorization': `Bearer ${token}`,
          'vmId': this.config.vmId,
          'accept-version': '1.2',
          'host': this.getHostFromUrl(this.config.url)
        },
        debug: (str) => {
          console.log('[WebSocket Debug]', str)
        },
        reconnectDelay: this.config.reconnectDelay,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
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
   * 安排重连
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

    // 指数退避算法
    const delay = Math.min(
      this.config.reconnectDelay * Math.pow(2, this.reconnectAttempts),
      30000 // 最大30秒
    )

    console.log(`[WebSocket] ${delay}ms后尝试重连 (第${this.reconnectAttempts + 1}次)`)

    this.reconnectTimer = setTimeout(async () => {
      this.reconnectAttempts++
      // 这里需要从外部传入token，实际实现中需要获取当前有效的token
      // 为了简化，这里假设有一个方法可以获取token
      const token = this.getCurrentToken()
      if (token) {
        await this.connect(token)
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
   * 这个方法需要与AuthService集成
   */
  private getCurrentToken(): string | null {
    // 这里应该从AuthService获取token
    // 为了避免循环依赖，可以通过依赖注入或者事件机制来实现
    return localStorage.getItem('access_token')
  }

  /**
   * 销毁连接管理器
   */
  destroy(): void {
    this.disconnect()
    this.stateHandlers.clear()
  }
}
