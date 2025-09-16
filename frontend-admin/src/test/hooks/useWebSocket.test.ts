/**
 * useWebSocket Hook 测试
 * 测试WebSocket hook的错误处理封装和组件接口
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useWebSocket } from '@/store/websocket/useWebSocketStore'
import { useWebSocketStore } from '@/store/websocket/websocketStore'
// ConnectionState 将在 mock 中定义

// Mock the store
vi.mock('@/store/websocket/websocketStore')

const ConnectionState = {
  DISCONNECTED: 'DISCONNECTED',
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  RECONNECTING: 'RECONNECTING',
  ERROR: 'ERROR'
}

const mockStore = {
  isConnected: false,
  connectionStatus: ConnectionState.DISCONNECTED,
  connectionError: null,
  reconnectAttempts: 0,
  maxReconnectAttempts: 5,
  connectionStats: {
    connectedAt: null,
    disconnectedAt: null,
    reconnectCount: 0,
    messagesSent: 0,
    messagesReceived: 0
  },
  lastMessage: null,
  messageHistory: [],
  realtimeData: {},
  config: {
    url: 'ws://localhost:8080/ws',
    protocols: [],
    reconnectInterval: 3000,
    maxReconnectAttempts: 5,
    heartbeatInterval: 30000,
    messageQueueSize: 100
  },
  connect: vi.fn(),
  disconnect: vi.fn(),
  reconnect: vi.fn(),
  sendMessage: vi.fn(),
  addMessageHandler: vi.fn(),
  removeMessageHandler: vi.fn(),
  clearMessageHandlers: vi.fn(),
  updateVMStatus: vi.fn(),
  updateTaskProgress: vi.fn(),
  updateSystemMetrics: vi.fn(),
  clearRealtimeData: vi.fn(),
  resetConnectionStats: vi.fn(),
  setMaxHistorySize: vi.fn(),
  resetState: vi.fn()
}

describe('useWebSocket', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock the store selector function
    const mockUseWebSocketStore = vi.mocked(useWebSocketStore)
    mockUseWebSocketStore.mockImplementation((selector: any) => {
      if (typeof selector === 'function') {
        return selector(mockStore)
      }
      return mockStore
    })
  })

  describe('状态访问', () => {
    it('应该正确暴露WebSocket状态', () => {
      const { result } = renderHook(() => useWebSocket())
      
      expect(result.current.isConnected).toBe(false)
      expect(result.current.connectionStatus).toBe(ConnectionState.DISCONNECTED)
      expect(result.current.connectionError).toBe(null)
      expect(result.current.reconnectAttempts).toBe(0)
      expect(result.current.maxReconnectAttempts).toBe(5)
      expect(result.current.connectionStats).toEqual(mockStore.connectionStats)
      expect(result.current.lastMessage).toBe(null)
      expect(result.current.messageHistory).toEqual([])
      expect(result.current.realtimeData).toEqual({})
      expect(result.current.config).toEqual(mockStore.config)
    })
  })

  describe('连接操作', () => {
    it('应该成功连接WebSocket', async () => {
      mockStore.connect.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useWebSocket())
      
      let connectResult
      await act(async () => {
        connectResult = await result.current.connect()
      })
      
      expect(connectResult).toEqual({ success: true, error: null })
      expect(mockStore.connect).toHaveBeenCalled()
    })

    it('应该处理连接失败', async () => {
      const error = new Error('连接失败')
      mockStore.connect.mockRejectedValue(error)
      
      const { result } = renderHook(() => useWebSocket())
      
      let connectResult
      await act(async () => {
        connectResult = await result.current.connect()
      })
      
      expect(connectResult).toEqual({ success: false, error: '连接失败' })
    })

    it('应该成功断开连接', async () => {
      mockStore.disconnect.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useWebSocket())
      
      let disconnectResult
      await act(async () => {
        disconnectResult = await result.current.disconnect()
      })
      
      expect(disconnectResult).toEqual({ success: true, error: null })
      expect(mockStore.disconnect).toHaveBeenCalled()
    })

    it('应该处理断开连接失败', async () => {
      const error = new Error('断开连接失败')
      mockStore.disconnect.mockRejectedValue(error)
      
      const { result } = renderHook(() => useWebSocket())
      
      let disconnectResult
      await act(async () => {
        disconnectResult = await result.current.disconnect()
      })
      
      expect(disconnectResult).toEqual({ success: false, error: '断开连接失败' })
    })

    it('应该成功重新连接', async () => {
      mockStore.reconnect.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useWebSocket())
      
      let reconnectResult
      await act(async () => {
        reconnectResult = await result.current.reconnect()
      })
      
      expect(reconnectResult).toEqual({ success: true, error: null })
      expect(mockStore.reconnect).toHaveBeenCalled()
    })
  })

  describe('消息操作', () => {
    it('应该成功发送消息', async () => {
      mockStore.sendMessage.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useWebSocket())
      
      const message = {
        id: 'msg-001',
        type: 'test',
        data: { test: 'data' },
        timestamp: '2024-01-15T10:00:00Z',
        vmId: 'vm-001'
      }
      
      let sendResult
      await act(async () => {
        sendResult = await result.current.sendMessage(message)
      })
      
      expect(sendResult).toEqual({ success: true, error: null })
      expect(mockStore.sendMessage).toHaveBeenCalledWith(message)
    })

    it('应该处理发送消息失败', async () => {
      const error = new Error('发送失败')
      mockStore.sendMessage.mockRejectedValue(error)
      
      const { result } = renderHook(() => useWebSocket())
      
      const message = {
        id: 'msg-002',
        type: 'test',
        data: { test: 'data' },
        timestamp: '2024-01-15T10:00:00Z',
        vmId: 'vm-001'
      }
      
      let sendResult
      await act(async () => {
        sendResult = await result.current.sendMessage(message)
      })
      
      expect(sendResult).toEqual({ success: false, error: '发送失败' })
    })
  })

  describe('消息处理器管理', () => {
    it('应该成功添加消息处理器', () => {
      const { result } = renderHook(() => useWebSocket())
      
      const handler = vi.fn()
      
      act(() => {
        result.current.addMessageHandler('test', handler)
      })
      
      expect(mockStore.addMessageHandler).toHaveBeenCalledWith('test', handler)
    })

    it('应该成功移除消息处理器', () => {
      const { result } = renderHook(() => useWebSocket())
      
      const handler = vi.fn()
      
      act(() => {
        result.current.removeMessageHandler('test', handler)
      })
      
      expect(mockStore.removeMessageHandler).toHaveBeenCalledWith('test', handler)
    })

    it('应该成功清除消息处理器', () => {
      const { result } = renderHook(() => useWebSocket())
      
      act(() => {
        result.current.clearMessageHandlers('test')
      })
      
      expect(mockStore.clearMessageHandlers).toHaveBeenCalledWith('test')
    })
  })

  describe('实时数据更新', () => {
    it('应该成功更新VM状态', () => {
      const { result } = renderHook(() => useWebSocket())
      
      const vmStatus = {
        vmId: 'vm-001',
        status: 'RUNNING',
        connectionStatus: 'CONNECTED',
        resourceUsage: { cpu: 50, memory: 60, disk: 40 },
        network: { ip: '192.168.1.100', port: 22 },
        processes: []
      }
      
      act(() => {
        result.current.updateVMStatus('vm-001', vmStatus)
      })
      
      expect(mockStore.updateVMStatus).toHaveBeenCalledWith('vm-001', vmStatus)
    })

    it('应该成功更新任务进度', () => {
      const { result } = renderHook(() => useWebSocket())
      
      const taskProgress = {
        taskId: 'task-001',
        progress: 75,
        status: 'RUNNING',
        currentRound: 8,
        totalRounds: 10
      }
      
      act(() => {
        result.current.updateTaskProgress('task-001', taskProgress)
      })
      
      expect(mockStore.updateTaskProgress).toHaveBeenCalledWith('task-001', taskProgress)
    })

    it('应该成功更新系统指标', () => {
      const { result } = renderHook(() => useWebSocket())
      
      const systemMetrics = {
        cpu: 65.5,
        memory: 78.2,
        disk: 45.8,
        network: { in: 1024, out: 2048 }
      }
      
      act(() => {
        result.current.updateSystemMetrics(systemMetrics)
      })
      
      expect(mockStore.updateSystemMetrics).toHaveBeenCalledWith(systemMetrics)
    })
  })

  describe('状态管理', () => {
    it('应该能够访问实时数据', () => {
      const { result } = renderHook(() => useWebSocket())
      
      // 只测试状态访问，不测试不存在的方法
      expect(result.current.realtimeData).toEqual({})
    })

    it('应该能够访问连接统计', () => {
      const { result } = renderHook(() => useWebSocket())
      
      // 只测试状态访问，不测试不存在的方法
      expect(result.current.connectionStats).toEqual(mockStore.connectionStats)
    })

    it('应该成功重置状态', () => {
      const { result } = renderHook(() => useWebSocket())
      
      act(() => {
        result.current.resetState()
      })
      
      expect(mockStore.resetState).toHaveBeenCalled()
    })
  })

  describe('错误处理', () => {
    it('应该处理非Error类型的异常', async () => {
      mockStore.connect.mockRejectedValue('string error')
      
      const { result } = renderHook(() => useWebSocket())
      
      let connectResult
      await act(async () => {
        connectResult = await result.current.connect()
      })
      
      expect(connectResult).toEqual({ success: false, error: 'WebSocket 连接失败' })
    })

    it('应该处理undefined异常', async () => {
      mockStore.sendMessage.mockRejectedValue(undefined)
      
      const { result } = renderHook(() => useWebSocket())
      
      let sendResult
      await act(async () => {
        sendResult = await result.current.sendMessage({ 
          id: 'msg-003',
          type: 'test', 
          data: {},
          timestamp: '2024-01-15T10:00:00Z',
          vmId: 'vm-001'
        })
      })
      
      expect(sendResult).toEqual({ success: false, error: '发送消息失败' })
    })
  })
})