/**
 * WebSocket消息处理器
 * 负责消息的发送、接收、处理和路由
 */

import type { 
  WebSocketMessage, 
  MessageHandler,
  ConnectMessage,
  HeartbeatMessage,
  StatusQueryMessage,
  WebSocketEvent,
  WebSocketEventListener
} from './types'
import type { ConnectionManager } from './connection-manager'

export class MessageHandler {
  private messageHandlers: Map<string, Set<MessageHandler>> = new Map()
  private eventListeners: Map<string, Set<WebSocketEventListener>> = new Map()
  private messageQueue: WebSocketMessage[] = []
  private isProcessingQueue = false

  constructor(
    private connectionManager: ConnectionManager,
    private vmId: string
  ) {
    this.setupSubscriptions()
  }

  /**
   * 发送消息
   */
  send(message: Omit<WebSocketMessage, 'id' | 'timestamp' | 'vmId'>): void {
    if (!this.connectionManager.isConnected()) {
      console.warn('[WebSocket] 未连接，消息加入队列')
      this.queueMessage(message)
      return
    }

    const fullMessage: WebSocketMessage = {
      ...message,
      id: this.generateMessageId('client'),
      timestamp: new Date().toISOString(),
      vmId: this.vmId
    }

    try {
      const stompClient = this.connectionManager.getStompClient()
      if (!stompClient) {
        throw new Error('STOMP客户端未初始化')
      }

      stompClient.publish({
        destination: '/app/message',
        body: JSON.stringify(fullMessage)
      })

      console.log('[WebSocket] 消息已发送:', fullMessage)
      
      this.emitEvent({
        type: 'MESSAGE',
        payload: { direction: 'outbound', message: fullMessage },
        timestamp: new Date().toISOString()
      })
    } catch (error) {
      console.error('[WebSocket] 发送消息失败:', error)
      this.queueMessage(message)
    }
  }

  /**
   * 发送连接消息（按照协议文档格式）
   */
  sendConnect(data: ConnectMessage): void {
    this.send({
      type: 'CONNECT',
      data
    })
  }

  /**
   * 发送心跳消息
   */
  sendHeartbeat(data: HeartbeatMessage): void {
    this.send({
      type: 'HEARTBEAT',
      data
    })
  }

  /**
   * 发送状态查询
   */
  sendStatusQuery(data: StatusQueryMessage): void {
    this.send({
      type: 'STATUS_QUERY',
      data
    })
  }

  /**
   * 发送VM控制命令
   */
  sendVMStart(data: any): void {
    this.send({
      type: 'VM_START',
      data
    })
  }

  sendVMStop(data: any): void {
    this.send({
      type: 'VM_STOP',
      data
    })
  }

  /**
   * 发送训练控制命令
   */
  sendTrainingStart(data: any): void {
    this.send({
      type: 'TRAINING_START',
      data
    })
  }

  sendTrainingStop(data: any): void {
    this.send({
      type: 'TRAINING_STOP',
      data
    })
  }

  /**
   * 发送数据集操作命令
   */
  sendDatasetCreate(data: any): void {
    this.send({
      type: 'DATASET_CREATE',
      data
    })
  }

  sendDatasetAppendRows(data: any): void {
    this.send({
      type: 'DATASET_APPEND_ROWS',
      data
    })
  }

  sendDatasetComplete(data: any): void {
    this.send({
      type: 'DATASET_COMPLETE',
      data
    })
  }

  /**
   * 注册消息处理器
   */
  onMessage(type: string, handler: MessageHandler): () => void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, new Set())
    }
    
    this.messageHandlers.get(type)!.add(handler)

    return () => {
      this.messageHandlers.get(type)?.delete(handler)
    }
  }

  /**
   * 移除消息处理器
   */
  offMessage(type: string, handler?: MessageHandler): void {
    if (handler) {
      this.messageHandlers.get(type)?.delete(handler)
    } else {
      this.messageHandlers.delete(type)
    }
  }

  /**
   * 注册事件监听器
   */
  addEventListener(eventType: string, listener: WebSocketEventListener): () => void {
    if (!this.eventListeners.has(eventType)) {
      this.eventListeners.set(eventType, new Set())
    }
    
    this.eventListeners.get(eventType)!.add(listener)

    return () => {
      this.eventListeners.get(eventType)?.delete(listener)
    }
  }

  /**
   * 设置订阅
   */
  private setupSubscriptions(): void {
    // 等待连接建立后再设置订阅
    this.connectionManager.onStateChange((state) => {
      if (state === 'CONNECTED') {
        this.subscribeToTopics()
        this.processMessageQueue()
      }
    })
  }

  /**
   * 订阅主题
   */
  private subscribeToTopics(): void {
    const stompClient = this.connectionManager.getStompClient()
    if (!stompClient) return

    // 订阅公共消息
    stompClient.subscribe('/topic/public', (message) => {
      this.handleIncomingMessage(message.body)
    })

    // 订阅系统通知
    stompClient.subscribe('/topic/notifications', (message) => {
      this.handleIncomingMessage(message.body)
    })

    // 订阅联邦学习消息
    stompClient.subscribe('/topic/federated-learning', (message) => {
      this.handleIncomingMessage(message.body)
    })

    // 订阅VM状态更新
    stompClient.subscribe('/topic/vm-status', (message) => {
      this.handleIncomingMessage(message.body)
    })

    // 订阅管理员专用频道
    stompClient.subscribe('/topic/admin', (message) => {
      this.handleIncomingMessage(message.body)
    })

    console.log('[WebSocket] 已订阅所有主题')
  }

  /**
   * 处理接收到的消息
   */
  private handleIncomingMessage(messageBody: string): void {
    try {
      const message: WebSocketMessage = JSON.parse(messageBody)
      console.log('[WebSocket] 收到消息:', message)

      // 触发消息事件
      this.emitEvent({
        type: 'MESSAGE',
        payload: { direction: 'inbound', message },
        timestamp: new Date().toISOString()
      })

      // 处理特殊消息类型
      this.handleSpecialMessages(message)

      // 调用注册的处理器
      const handlers = this.messageHandlers.get(message.type)
      if (handlers) {
        handlers.forEach(handler => {
          try {
            handler(message)
          } catch (error) {
            console.error('消息处理器执行失败:', error)
          }
        })
      }
    } catch (error) {
      console.error('[WebSocket] 解析消息失败:', error, messageBody)
    }
  }

  /**
   * 处理特殊消息类型
   */
  private handleSpecialMessages(message: WebSocketMessage): void {
    switch (message.type) {
      case 'CONNECT_ACK':
        this.handleConnectAck(message)
        break
      case 'HEARTBEAT_ACK':
        this.handleHeartbeatAck(message)
        break
      case 'ERROR':
        this.handleError(message)
        break
      default:
        // 其他消息类型由注册的处理器处理
        break
    }
  }

  /**
   * 处理连接确认
   */
  private handleConnectAck(message: WebSocketMessage): void {
    console.log('[WebSocket] 连接确认:', message.data)
    
    const data = message.data as any
    if (data?.sessionId) {
      // 更新连接状态中的sessionId
      // 这里可以通过事件通知ConnectionManager更新状态
    }
  }

  /**
   * 处理心跳确认
   */
  private handleHeartbeatAck(message: WebSocketMessage): void {
    this.connectionManager.updateHeartbeat()
    console.log('[WebSocket] 心跳确认:', new Date().toISOString())
  }

  /**
   * 处理错误消息
   */
  private handleError(message: WebSocketMessage): void {
    console.error('[WebSocket] 服务器错误:', message.data)
    
    this.emitEvent({
      type: 'ERROR',
      payload: { message },
      timestamp: new Date().toISOString()
    })
  }

  /**
   * 消息入队
   */
  private queueMessage(message: Omit<WebSocketMessage, 'id' | 'timestamp' | 'vmId'>): void {
    const fullMessage: WebSocketMessage = {
      ...message,
      id: this.generateMessageId('client'),
      timestamp: new Date().toISOString(),
      vmId: this.vmId
    }
    
    this.messageQueue.push(fullMessage)
    console.log(`[WebSocket] 消息已入队，队列长度: ${this.messageQueue.length}`)
  }

  /**
   * 处理消息队列
   */
  private async processMessageQueue(): void {
    if (this.isProcessingQueue || this.messageQueue.length === 0) {
      return
    }

    this.isProcessingQueue = true
    console.log(`[WebSocket] 开始处理消息队列，队列长度: ${this.messageQueue.length}`)

    while (this.messageQueue.length > 0 && this.connectionManager.isConnected()) {
      const message = this.messageQueue.shift()!
      
      try {
        const stompClient = this.connectionManager.getStompClient()
        if (stompClient) {
          stompClient.publish({
            destination: '/app/message',
            body: JSON.stringify(message)
          })
          console.log('[WebSocket] 队列消息已发送:', message.type)
        }
      } catch (error) {
        console.error('[WebSocket] 队列消息发送失败:', error)
        // 重新入队
        this.messageQueue.unshift(message)
        break
      }

      // 避免发送过快
      await new Promise(resolve => setTimeout(resolve, 100))
    }

    this.isProcessingQueue = false
  }

  /**
   * 生成消息ID
   */
  private generateMessageId(prefix: string): string {
    const timestamp = Date.now()
    const random = Math.random().toString(36).substr(2, 9)
    return `${prefix}-${timestamp}-${random}`
  }

  /**
   * 触发事件
   */
  private emitEvent(event: WebSocketEvent): void {
    const listeners = this.eventListeners.get(event.type)
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(event)
        } catch (error) {
          console.error('WebSocket事件监听器执行失败:', error)
        }
      })
    }
  }

  /**
   * 清空消息队列
   */
  clearMessageQueue(): void {
    this.messageQueue.length = 0
  }

  /**
   * 获取消息队列长度
   */
  getQueueLength(): number {
    return this.messageQueue.length
  }

  /**
   * 销毁消息处理器
   */
  destroy(): void {
    this.messageHandlers.clear()
    this.eventListeners.clear()
    this.clearMessageQueue()
  }
}
