/**
 * useVM Hook 测试
 * 测试虚拟机管理Hook的封装逻辑和计算属性
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useVM } from '@/store/vm/useVMStore'
import { useVMStore } from '@/store/vm/vmStore'
import { vmService } from '@/services'
import {
  mockVMList,
  mockVM1,
  mockVM2,
  mockVM3,
  mockVMListResponse,
  mockVMStatus1,
  mockVMStatus2,
  mockVMStatus3,
  mockVMStatusMap,
  mockVMListParams,
  mockVMUpdateRequest,
  mockVMStartRequest,
  mockVMStopRequest,
  mockVMErrors
} from '@/mocks/store/vmStoreMock'

// Mock vmService
vi.mock('@/services', () => ({
  vmService: {
    getVMList: vi.fn(),
    getVMDetail: vi.fn(),
    getVMStatus: vi.fn(),
    updateVM: vi.fn(),
    deleteVM: vi.fn(),
    startVM: vi.fn(),
    stopVM: vi.fn(),
    restartVM: vi.fn()
  }
}))

describe('useVM Hook', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 重置store状态
    const store = useVMStore.getState()
    store.resetState()
  })

  describe('基本状态访问', () => {
    it('应该返回正确的初始状态', () => {
      const { result } = renderHook(() => useVM())
      
      expect(result.current.vmList).toEqual([])
      expect(result.current.vmListTotal).toBe(0)
      expect(result.current.vmListLoading).toBe(false)
      expect(result.current.vmListError).toBe(null)
      expect(result.current.currentVM).toBe(null)
      expect(result.current.currentVMLoading).toBe(false)
      expect(result.current.currentVMError).toBe(null)
      expect(result.current.vmStatusMap).toEqual({})
      expect(result.current.operationLoading).toEqual({})
      expect(result.current.operationError).toEqual({})
      expect(result.current.pagination).toEqual({
        page: 1,
        size: 20,
        total: 0
      })
      expect(result.current.queryParams).toEqual({})
    })

    it('应该正确反映已加载状态', () => {
      // 设置已加载状态
      act(() => {
        useVMStore.setState({
          vmList: mockVMList,
          vmListTotal: mockVMList.length,
          vmStatusMap: mockVMStatusMap,
          currentVM: mockVM1
        })
      })
      
      const { result } = renderHook(() => useVM())
      
      expect(result.current.vmList).toEqual(mockVMList)
      expect(result.current.vmListTotal).toBe(mockVMList.length)
      expect(result.current.vmStatusMap).toEqual(mockVMStatusMap)
      expect(result.current.currentVM).toEqual(mockVM1)
    })
  })

  describe('获取虚拟机列表', () => {
    it('应该成功获取虚拟机列表并返回成功结果', async () => {
      // 模拟成功的API响应
      vi.mocked(vmService.getVMList).mockResolvedValue(mockVMListResponse)
      
      const { result } = renderHook(() => useVM())
      
      // 执行获取列表
      let fetchResult: any
      await act(async () => {
        fetchResult = await result.current.fetchVMList()
      })
      
      // 验证返回结果
      expect(fetchResult.success).toBe(true)
      expect(fetchResult.error).toBe(null)
      
      // 验证状态更新
      expect(result.current.vmList).toEqual(mockVMListResponse.list)
      expect(result.current.vmListTotal).toBe(mockVMListResponse.total)
    })

    it('应该处理获取列表失败并返回错误结果', async () => {
      // 模拟API失败
      vi.mocked(vmService.getVMList).mockRejectedValue(mockVMErrors.NETWORK_ERROR)
      
      const { result } = renderHook(() => useVM())
      
      // 执行获取列表
      let fetchResult: any
      await act(async () => {
        fetchResult = await result.current.fetchVMList()
      })
      
      // 验证返回结果
      expect(fetchResult.success).toBe(false)
      expect(fetchResult.error).toBe('网络连接失败')
      
      // 验证错误状态
      expect(result.current.vmListError).toBe('网络连接失败')
    })

    it('应该成功刷新虚拟机列表', async () => {
      // 设置当前查询参数
      act(() => {
        useVMStore.setState({
          queryParams: { status: 'RUNNING' },
          pagination: { page: 2, size: 10, total: 50 }
        })
      })
      
      // 模拟成功的API响应
      vi.mocked(vmService.getVMList).mockResolvedValue(mockVMListResponse)
      
      const { result } = renderHook(() => useVM())
      
      // 执行刷新列表
      let refreshResult: any
      await act(async () => {
        refreshResult = await result.current.refreshVMList()
      })
      
      // 验证返回结果
      expect(refreshResult.success).toBe(true)
      expect(refreshResult.error).toBe(null)
      
      // 验证service被正确调用
      expect(vmService.getVMList).toHaveBeenCalledWith({
        status: 'RUNNING',
        page: 2,
        size: 10
      })
    })
  })

  describe('获取虚拟机详情', () => {
    it('应该成功获取虚拟机详情', async () => {
      // 模拟成功的API响应
      vi.mocked(vmService.getVMDetail).mockResolvedValue(mockVM1)
      
      const { result } = renderHook(() => useVM())
      
      // 执行获取详情
      let fetchResult: any
      await act(async () => {
        fetchResult = await result.current.fetchVMDetail('vm-001')
      })
      
      // 验证返回结果
      expect(fetchResult.success).toBe(true)
      expect(fetchResult.error).toBe(null)
      
      // 验证状态更新
      expect(result.current.currentVM).toEqual(mockVM1)
    })

    it('应该处理获取详情失败', async () => {
      // 模拟API失败
      vi.mocked(vmService.getVMDetail).mockRejectedValue(mockVMErrors.VM_NOT_FOUND)
      
      const { result } = renderHook(() => useVM())
      
      // 执行获取详情
      let fetchResult: any
      await act(async () => {
        fetchResult = await result.current.fetchVMDetail('vm-999')
      })
      
      // 验证返回结果
      expect(fetchResult.success).toBe(false)
      expect(fetchResult.error).toBe('虚拟机不存在')
    })
  })

  describe('虚拟机状态管理', () => {
    it('应该成功获取虚拟机状态', async () => {
      // 模拟成功的API响应
      vi.mocked(vmService.getVMStatus).mockResolvedValue(mockVMStatus1)
      
      const { result } = renderHook(() => useVM())
      
      // 执行获取状态
      let fetchResult: any
      await act(async () => {
        fetchResult = await result.current.fetchVMStatus('vm-001')
      })
      
      // 验证返回结果
      expect(fetchResult.success).toBe(true)
      expect(fetchResult.error).toBe(null)
      
      // 验证状态更新
      expect(result.current.vmStatusMap['vm-001']).toEqual(mockVMStatus1)
    })

    it('应该成功获取所有虚拟机状态', async () => {
      // 设置虚拟机列表
      act(() => {
        useVMStore.setState({ vmList: mockVMList })
      })
      
      // 模拟API响应
      vi.mocked(vmService.getVMStatus)
        .mockResolvedValueOnce(mockVMStatus1)
        .mockResolvedValueOnce(mockVMStatus2)
        .mockResolvedValueOnce(mockVMStatus3)
      
      const { result } = renderHook(() => useVM())
      
      // 执行获取所有状态
      let fetchResult: any
      await act(async () => {
        fetchResult = await result.current.fetchAllVMStatus()
      })
      
      // 验证返回结果
      expect(fetchResult.success).toBe(true)
      expect(fetchResult.error).toBe(null)
      
      // 验证所有状态都被获取
      expect(vmService.getVMStatus).toHaveBeenCalledTimes(3)
    })
  })

  describe('虚拟机操作', () => {
    describe('更新虚拟机', () => {
      it('应该成功更新虚拟机', async () => {
        // 模拟成功的API响应
        vi.mocked(vmService.updateVM).mockResolvedValue(undefined)
        vi.mocked(vmService.getVMDetail).mockResolvedValue({
          ...mockVM1,
          name: '更新后的虚拟机'
        })
        
        const { result } = renderHook(() => useVM())
        
        // 执行更新
        let updateResult: any
        await act(async () => {
          updateResult = await result.current.updateVM('vm-001', mockVMUpdateRequest)
        })
        
        // 验证返回结果
        expect(updateResult.success).toBe(true)
        expect(updateResult.error).toBe(null)
        
        // 验证service被调用
        expect(vmService.updateVM).toHaveBeenCalledWith('vm-001', mockVMUpdateRequest)
      })

      it('应该处理更新失败', async () => {
        // 模拟API失败
        vi.mocked(vmService.updateVM).mockRejectedValue(mockVMErrors.VM_UPDATE_FAILED)
        
        const { result } = renderHook(() => useVM())
        
        // 执行更新
        let updateResult: any
        await act(async () => {
          updateResult = await result.current.updateVM('vm-001', mockVMUpdateRequest)
        })
        
        // 验证返回结果
        expect(updateResult.success).toBe(false)
        expect(updateResult.error).toBe('更新虚拟机失败')
      })
    })

    describe('删除虚拟机', () => {
      it('应该成功删除虚拟机', async () => {
        // 模拟成功的API响应
        vi.mocked(vmService.deleteVM).mockResolvedValue(undefined)
        
        const { result } = renderHook(() => useVM())
        
        // 执行删除
        let deleteResult: any
        await act(async () => {
          deleteResult = await result.current.deleteVM('vm-001')
        })
        
        // 验证返回结果
        expect(deleteResult.success).toBe(true)
        expect(deleteResult.error).toBe(null)
        
        // 验证service被调用
        expect(vmService.deleteVM).toHaveBeenCalledWith('vm-001', false)
      })

      it('应该支持强制删除', async () => {
        // 模拟成功的API响应
        vi.mocked(vmService.deleteVM).mockResolvedValue(undefined)
        
        const { result } = renderHook(() => useVM())
        
        // 执行强制删除
        let deleteResult: any
        await act(async () => {
          deleteResult = await result.current.deleteVM('vm-001', true)
        })
        
        // 验证service被正确调用
        expect(vmService.deleteVM).toHaveBeenCalledWith('vm-001', true)
      })
    })

    describe('启动虚拟机', () => {
      it('应该成功启动虚拟机', async () => {
        // 模拟成功的API响应
        vi.mocked(vmService.startVM).mockResolvedValue(undefined)
        vi.mocked(vmService.getVMStatus).mockResolvedValue({
          ...mockVMStatus1,
          status: 'RUNNING'
        })
        
        const { result } = renderHook(() => useVM())
        
        // 执行启动
        let startResult: any
        await act(async () => {
          startResult = await result.current.startVM('vm-001', mockVMStartRequest)
        })
        
        // 验证返回结果
        expect(startResult.success).toBe(true)
        expect(startResult.error).toBe(null)
        
        // 验证service被调用
        expect(vmService.startVM).toHaveBeenCalledWith('vm-001', mockVMStartRequest)
      })

      it('应该处理启动失败', async () => {
        // 模拟API失败
        vi.mocked(vmService.startVM).mockRejectedValue(mockVMErrors.VM_START_FAILED)
        
        const { result } = renderHook(() => useVM())
        
        // 执行启动
        let startResult: any
        await act(async () => {
          startResult = await result.current.startVM('vm-001')
        })
        
        // 验证返回结果
        expect(startResult.success).toBe(false)
        expect(startResult.error).toBe('虚拟机启动失败')
      })
    })

    describe('停止虚拟机', () => {
      it('应该成功停止虚拟机', async () => {
        // 模拟成功的API响应
        vi.mocked(vmService.stopVM).mockResolvedValue(undefined)
        vi.mocked(vmService.getVMStatus).mockResolvedValue({
          ...mockVMStatus1,
          status: 'STOPPED'
        })
        
        const { result } = renderHook(() => useVM())
        
        // 执行停止
        let stopResult: any
        await act(async () => {
          stopResult = await result.current.stopVM('vm-001', mockVMStopRequest)
        })
        
        // 验证返回结果
        expect(stopResult.success).toBe(true)
        expect(stopResult.error).toBe(null)
      })
    })

    describe('重启虚拟机', () => {
      it('应该成功重启虚拟机', async () => {
        // 模拟成功的API响应
        vi.mocked(vmService.restartVM).mockResolvedValue(undefined)
        vi.mocked(vmService.getVMStatus).mockResolvedValue(mockVMStatus1)
        
        const { result } = renderHook(() => useVM())
        
        // 执行重启
        let restartResult: any
        await act(async () => {
          restartResult = await result.current.restartVM('vm-001')
        })
        
        // 验证返回结果
        expect(restartResult.success).toBe(true)
        expect(restartResult.error).toBe(null)
      })
    })
  })

  describe('计算属性和工具方法', () => {
    beforeEach(() => {
      // 设置测试数据
      act(() => {
        useVMStore.setState({
          vmList: mockVMList,
          vmStatusMap: mockVMStatusMap
        })
      })
    })

    it('应该正确获取虚拟机状态', () => {
      const { result } = renderHook(() => useVM())
      
      // 获取存在的虚拟机状态
      expect(result.current.getVMStatus('vm-001')).toEqual(mockVMStatus1)
      
      // 获取不存在的虚拟机状态
      expect(result.current.getVMStatus('vm-999')).toBe(null)
    })

    it('应该正确检查虚拟机是否正在操作', () => {
      // 设置操作loading状态
      act(() => {
        useVMStore.setState({
          operationLoading: {
            'start-vm-001': true,
            'stop-vm-002': true
          }
        })
      })
      
      const { result } = renderHook(() => useVM())
      
      // 检查特定操作
      expect(result.current.isVMOperating('vm-001', 'start')).toBe(true)
      expect(result.current.isVMOperating('vm-001', 'stop')).toBe(false)
      
      // 检查任何操作
      expect(result.current.isVMOperating('vm-001')).toBe(true)
      expect(result.current.isVMOperating('vm-003')).toBe(false)
    })

    it('应该正确获取虚拟机操作错误', () => {
      // 设置操作错误状态
      act(() => {
        useVMStore.setState({
          operationError: {
            'start-vm-001': '启动失败',
            'stop-vm-002': '停止失败'
          }
        })
      })
      
      const { result } = renderHook(() => useVM())
      
      // 获取存在的错误
      expect(result.current.getVMOperationError('vm-001', 'start')).toBe('启动失败')
      
      // 获取不存在的错误
      expect(result.current.getVMOperationError('vm-001', 'stop')).toBe(null)
    })

    it('应该正确判断虚拟机是否可以启动', () => {
      const { result } = renderHook(() => useVM())
      
      // 停止状态的虚拟机可以启动
      expect(result.current.canStartVM(mockVM2)).toBe(true)
      
      // 运行状态的虚拟机不能启动
      expect(result.current.canStartVM(mockVM1)).toBe(false)
      
      // 错误状态的虚拟机不能启动
      expect(result.current.canStartVM(mockVM3)).toBe(false)
    })

    it('应该正确判断虚拟机是否可以停止', () => {
      const { result } = renderHook(() => useVM())
      
      // 运行状态的虚拟机可以停止
      expect(result.current.canStopVM(mockVM1)).toBe(true)
      
      // 停止状态的虚拟机不能停止
      expect(result.current.canStopVM(mockVM2)).toBe(false)
      
      // 错误状态的虚拟机不能停止
      expect(result.current.canStopVM(mockVM3)).toBe(false)
    })

    it('应该正确判断虚拟机是否可以重启', () => {
      const { result } = renderHook(() => useVM())
      
      // 运行状态的虚拟机可以重启
      expect(result.current.canRestartVM(mockVM1)).toBe(true)
      
      // 停止状态的虚拟机不能重启
      expect(result.current.canRestartVM(mockVM2)).toBe(false)
      
      // 错误状态的虚拟机不能重启
      expect(result.current.canRestartVM(mockVM3)).toBe(false)
    })

    it('应该正确计算在线虚拟机数量', () => {
      const { result } = renderHook(() => useVM())
      
      // 应该有1个运行中的虚拟机
      expect(result.current.onlineVMCount()).toBe(1)
    })

    it('应该正确计算离线虚拟机数量', () => {
      const { result } = renderHook(() => useVM())
      
      // 应该有1个停止的虚拟机
      expect(result.current.offlineVMCount()).toBe(1)
    })

    it('应该正确计算异常虚拟机数量', () => {
      const { result } = renderHook(() => useVM())
      
      // 应该有1个错误状态的虚拟机
      expect(result.current.errorVMCount()).toBe(1)
    })
  })

  describe('状态管理操作', () => {
    it('应该能够设置分页参数', () => {
      const { result } = renderHook(() => useVM())
      
      // 设置分页
      act(() => {
        result.current.setPagination(3, 15)
      })
      
      // 验证分页状态
      expect(result.current.pagination.page).toBe(3)
      expect(result.current.pagination.size).toBe(15)
    })

    it('应该能够设置查询参数', () => {
      const { result } = renderHook(() => useVM())
      const queryParams = { status: 'RUNNING' as const, osType: 'Ubuntu 20.04' }
      
      // 设置查询参数
      act(() => {
        result.current.setQueryParams(queryParams)
      })
      
      // 验证查询参数
      expect(result.current.queryParams).toEqual(queryParams)
    })

    it('应该能够重置查询参数', () => {
      // 先设置查询参数
      act(() => {
        useVMStore.setState({ queryParams: { status: 'RUNNING' } })
      })
      
      const { result } = renderHook(() => useVM())
      
      // 重置查询参数
      act(() => {
        result.current.resetQueryParams()
      })
      
      // 验证查询参数被重置
      expect(result.current.queryParams).toEqual({})
    })

    it('应该能够清除错误信息', () => {
      // 设置错误状态
      act(() => {
        useVMStore.setState({
          vmListError: '获取列表失败',
          operationError: { 'start-vm-001': '启动失败' }
        })
      })
      
      const { result } = renderHook(() => useVM())
      
      // 清除错误
      act(() => {
        result.current.clearError()
      })
      
      // 验证错误被清除
      expect(result.current.vmListError).toBe(null)
      expect(result.current.operationError).toEqual({})
    })

    it('应该能够清除特定虚拟机的错误', () => {
      // 设置操作错误
      act(() => {
        useVMStore.setState({
          operationError: {
            'start-vm-001': '启动失败',
            'stop-vm-001': '停止失败',
            'start-vm-002': '启动失败'
          }
        })
      })
      
      const { result } = renderHook(() => useVM())
      
      // 清除特定虚拟机的错误
      act(() => {
        result.current.clearVMError('vm-001')
      })
      
      // 验证只有vm-001相关的错误被清除
      expect(result.current.operationError['start-vm-001']).toBeUndefined()
      expect(result.current.operationError['stop-vm-001']).toBeUndefined()
      expect(result.current.operationError['start-vm-002']).toBe('启动失败')
    })

    it('应该能够重置状态', () => {
      // 设置一些状态
      act(() => {
        useVMStore.setState({
          vmList: mockVMList,
          currentVM: mockVM1,
          vmStatusMap: mockVMStatusMap
        })
      })
      
      const { result } = renderHook(() => useVM())
      
      // 重置状态
      act(() => {
        result.current.resetState()
      })
      
      // 验证状态被重置
      expect(result.current.vmList).toEqual([])
      expect(result.current.currentVM).toBe(null)
      expect(result.current.vmStatusMap).toEqual({})
    })

    it('应该能够设置当前虚拟机', () => {
      const { result } = renderHook(() => useVM())
      
      // 设置当前虚拟机
      act(() => {
        result.current.setCurrentVM(mockVM1)
      })
      
      // 验证当前虚拟机被设置
      expect(result.current.currentVM).toEqual(mockVM1)
      
      // 清除当前虚拟机
      act(() => {
        result.current.setCurrentVM(null)
      })
      
      // 验证当前虚拟机被清除
      expect(result.current.currentVM).toBe(null)
    })
  })
})
