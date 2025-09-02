/**
 * VM Store 测试
 * 测试虚拟机管理状态的所有功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useVMStore } from '@/store/vm/vmStore'
import { vmService } from '@/services'
import {
  mockVMList,
  mockVM1,
  mockVM2,
  mockVMListResponse,
  mockVMStatus1,
  mockVMStatusMap,
  mockVMListParams,
  mockVMUpdateRequest,
  mockVMStartRequest,
  mockVMStopRequest,
  mockVMErrors,
  mockInitialVMState,
  mockLoadedVMState
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

describe('VMStore', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 重置store状态
    const store = useVMStore.getState()
    store.resetState()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      const state = useVMStore.getState()
      
      expect(state.vmList).toEqual([])
      expect(state.vmListTotal).toBe(0)
      expect(state.vmListLoading).toBe(false)
      expect(state.vmListError).toBe(null)
      expect(state.currentVM).toBe(null)
      expect(state.currentVMLoading).toBe(false)
      expect(state.currentVMError).toBe(null)
      expect(state.vmStatusMap).toEqual({})
      expect(state.operationLoading).toEqual({})
      expect(state.operationError).toEqual({})
      expect(state.pagination).toEqual({
        page: 1,
        size: 20,
        total: 0
      })
      expect(state.queryParams).toEqual({})
    })
  })

  describe('获取虚拟机列表', () => {
    it('应该成功获取虚拟机列表', async () => {
      // 模拟成功的API响应
      vi.mocked(vmService.getVMList).mockResolvedValue(mockVMListResponse)
      
      const store = useVMStore.getState()
      
      // 执行获取列表
      await store.fetchVMList()
      
      // 验证状态更新
      const state = useVMStore.getState()
      expect(state.vmList).toEqual(mockVMListResponse.list)
      expect(state.vmListTotal).toBe(mockVMListResponse.total)
      expect(state.vmListLoading).toBe(false)
      expect(state.vmListError).toBe(null)
      expect(state.pagination).toEqual({
        page: mockVMListResponse.page,
        size: mockVMListResponse.size,
        total: mockVMListResponse.total
      })
      
      // 验证service被调用
      expect(vmService.getVMList).toHaveBeenCalledWith({
        page: 1,
        size: 20
      })
    })

    it('应该在获取过程中显示loading状态', async () => {
      // 创建一个永不resolve的Promise
      const pendingPromise = new Promise<any>(() => {})
      vi.mocked(vmService.getVMList).mockReturnValue(pendingPromise)
      
      const store = useVMStore.getState()
      
      // 开始获取但不等待
      store.fetchVMList()
      
      // 验证loading状态
      const state = useVMStore.getState()
      expect(state.vmListLoading).toBe(true)
      expect(state.vmListError).toBe(null)
    })

    it('应该处理获取列表失败', async () => {
      // 模拟API失败
      vi.mocked(vmService.getVMList).mockRejectedValue(mockVMErrors.NETWORK_ERROR)
      
      const store = useVMStore.getState()
      
      // 执行获取列表并期望抛出错误
      await expect(store.fetchVMList()).rejects.toThrow('网络连接失败')
      
      // 验证错误状态
      const state = useVMStore.getState()
      expect(state.vmListLoading).toBe(false)
      expect(state.vmListError).toBe('网络连接失败')
    })

    it('应该使用自定义参数获取列表', async () => {
      const customParams = { page: 2, size: 10, status: 'RUNNING' as const }
      
      vi.mocked(vmService.getVMList).mockResolvedValue({
        ...mockVMListResponse,
        page: 2,
        size: 10
      })
      
      const store = useVMStore.getState()
      
      // 使用自定义参数获取列表
      await store.fetchVMList(customParams)
      
      // 验证service被正确调用
      expect(vmService.getVMList).toHaveBeenCalledWith(customParams)
    })
  })

  describe('刷新虚拟机列表', () => {
    it('应该使用当前参数刷新列表', async () => {
      // 设置当前查询参数和分页
      useVMStore.setState({
        queryParams: { status: 'RUNNING' },
        pagination: { page: 2, size: 10, total: 50 }
      })
      
      vi.mocked(vmService.getVMList).mockResolvedValue(mockVMListResponse)
      
      const store = useVMStore.getState()
      
      // 执行刷新
      await store.refreshVMList()
      
      // 验证service被正确调用
      expect(vmService.getVMList).toHaveBeenCalledWith({
        status: 'RUNNING' as const,
        page: 2,
        size: 10
      })
    })
  })

  describe('获取虚拟机详情', () => {
    it('应该成功获取虚拟机详情', async () => {
      // 模拟成功的API响应
      vi.mocked(vmService.getVMDetail).mockResolvedValue(mockVM1)
      
      const store = useVMStore.getState()
      
      // 执行获取详情
      await store.fetchVMDetail('vm-001')
      
      // 验证状态更新
      const state = useVMStore.getState()
      expect(state.currentVM).toEqual(mockVM1)
      expect(state.currentVMLoading).toBe(false)
      expect(state.currentVMError).toBe(null)
      
      // 验证service被调用
      expect(vmService.getVMDetail).toHaveBeenCalledWith('vm-001')
    })

    it('应该处理获取详情失败', async () => {
      // 模拟API失败
      vi.mocked(vmService.getVMDetail).mockRejectedValue(mockVMErrors.VM_NOT_FOUND)
      
      const store = useVMStore.getState()
      
      // 执行获取详情并期望抛出错误
      await expect(store.fetchVMDetail('vm-999')).rejects.toThrow('虚拟机不存在')
      
      // 验证错误状态
      const state = useVMStore.getState()
      expect(state.currentVMLoading).toBe(false)
      expect(state.currentVMError).toBe('虚拟机不存在')
    })
  })

  describe('虚拟机状态管理', () => {
    it('应该成功获取虚拟机状态', async () => {
      // 模拟成功的API响应
      vi.mocked(vmService.getVMStatus).mockResolvedValue(mockVMStatus1)
      
      const store = useVMStore.getState()
      
      // 执行获取状态
      await store.fetchVMStatus('vm-001')
      
      // 验证状态更新
      const state = useVMStore.getState()
      expect(state.vmStatusMap['vm-001']).toEqual(mockVMStatus1)
    })

    it('应该获取所有虚拟机状态', async () => {
      // 设置虚拟机列表
      useVMStore.setState({ vmList: mockVMList })
      
      // 模拟每个VM的状态响应
      vi.mocked(vmService.getVMStatus)
        .mockResolvedValueOnce(mockVMStatus1)
        .mockResolvedValueOnce({ ...mockVMStatus1, vmId: 'vm-002', status: 'STOPPED' })
        .mockResolvedValueOnce({ ...mockVMStatus1, vmId: 'vm-003', status: 'ERROR' })
      
      const store = useVMStore.getState()
      
      // 执行获取所有状态
      await store.fetchAllVMStatus()
      
      // 验证所有VM的状态都被获取
      expect(vmService.getVMStatus).toHaveBeenCalledTimes(3)
      expect(vmService.getVMStatus).toHaveBeenCalledWith('vm-001')
      expect(vmService.getVMStatus).toHaveBeenCalledWith('vm-002')
      expect(vmService.getVMStatus).toHaveBeenCalledWith('vm-003')
    })

    it('应该处理获取状态失败但不抛出错误', async () => {
      // 模拟API失败
      vi.mocked(vmService.getVMStatus).mockRejectedValue(mockVMErrors.NETWORK_ERROR)
      
      const store = useVMStore.getState()
      
      // 执行获取状态，不应该抛出错误
      await expect(store.fetchVMStatus('vm-001')).resolves.toBeUndefined()
      
      // 验证状态映射没有更新
      const state = useVMStore.getState()
      expect(state.vmStatusMap['vm-001']).toBeUndefined()
    })
  })

  describe('虚拟机操作', () => {
    describe('更新虚拟机', () => {
      it('应该成功更新虚拟机', async () => {
        // 设置初始列表和当前VM
        useVMStore.setState({
          vmList: [mockVM1],
          currentVM: mockVM1
        })
        
        // 模拟成功的API响应
        const mockUpdateResponse = {
          vmId: 'vm-001',
          name: '更新后的虚拟机名称',
          updatedAt: new Date().toISOString()
        }
        vi.mocked(vmService.updateVM).mockResolvedValue(mockUpdateResponse)
        vi.mocked(vmService.getVMDetail).mockResolvedValue({
          ...mockVM1,
          name: '更新后的虚拟机名称'
        })
        
        const store = useVMStore.getState()
        
        // 执行更新
        await store.updateVM('vm-001', mockVMUpdateRequest)
        
        // 验证操作状态
        const state = useVMStore.getState()
        expect(state.operationLoading['update-vm-001']).toBe(false)
        
        // 验证service被调用
        expect(vmService.updateVM).toHaveBeenCalledWith('vm-001', mockVMUpdateRequest)
        expect(vmService.getVMDetail).toHaveBeenCalledWith('vm-001')
      })

      it('应该在更新过程中显示loading状态', async () => {
        const pendingPromise = new Promise<any>(() => {})
        vi.mocked(vmService.updateVM).mockReturnValue(pendingPromise)
        
        const store = useVMStore.getState()
        
        // 开始更新但不等待
        store.updateVM('vm-001', mockVMUpdateRequest)
        
        // 验证loading状态
        const state = useVMStore.getState()
        expect(state.operationLoading['update-vm-001']).toBe(true)
        expect(state.operationError['update-vm-001']).toBe(null)
      })

      it('应该处理更新失败', async () => {
        // 模拟API失败
        vi.mocked(vmService.updateVM).mockRejectedValue(mockVMErrors.VM_UPDATE_FAILED)
        
        const store = useVMStore.getState()
        
        // 执行更新并期望抛出错误
        await expect(store.updateVM('vm-001', mockVMUpdateRequest)).rejects.toThrow('更新虚拟机失败')
        
        // 验证错误状态
        const state = useVMStore.getState()
        expect(state.operationLoading['update-vm-001']).toBe(false)
        expect(state.operationError['update-vm-001']).toBe('更新虚拟机失败')
      })
    })

    describe('删除虚拟机', () => {
      it('应该成功删除虚拟机', async () => {
        // 设置初始列表
        useVMStore.setState({
          vmList: mockVMList,
          vmListTotal: mockVMList.length,
          vmStatusMap: mockVMStatusMap
        })
        
        // 模拟成功的API响应
        vi.mocked(vmService.deleteVM).mockResolvedValue(undefined)
        
        const store = useVMStore.getState()
        
        // 执行删除
        await store.deleteVM('vm-001')
        
        // 验证VM被从列表中移除
        const state = useVMStore.getState()
        expect(state.vmList.find(vm => vm.vmId === 'vm-001')).toBeUndefined()
        expect(state.vmListTotal).toBe(mockVMList.length - 1)
        expect(state.vmStatusMap['vm-001']).toBeUndefined()
        expect(state.operationLoading['delete-vm-001']).toBe(false)
        
        // 验证service被调用
        expect(vmService.deleteVM).toHaveBeenCalledWith('vm-001', false)
      })

      it('应该支持强制删除', async () => {
        vi.mocked(vmService.deleteVM).mockResolvedValue(undefined)
        
        const store = useVMStore.getState()
        
        // 执行强制删除
        await store.deleteVM('vm-001', true)
        
        // 验证service被正确调用
        expect(vmService.deleteVM).toHaveBeenCalledWith('vm-001', true)
      })

      it('应该处理删除失败', async () => {
        // 模拟API失败
        vi.mocked(vmService.deleteVM).mockRejectedValue(mockVMErrors.VM_DELETE_FAILED)
        
        const store = useVMStore.getState()
        
        // 执行删除并期望抛出错误
        await expect(store.deleteVM('vm-001')).rejects.toThrow('删除虚拟机失败')
        
        // 验证错误状态
        const state = useVMStore.getState()
        expect(state.operationLoading['delete-vm-001']).toBe(false)
        expect(state.operationError['delete-vm-001']).toBe('删除虚拟机失败')
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
        
        const store = useVMStore.getState()
        
        // 执行启动
        await store.startVM('vm-001', mockVMStartRequest)
        
        // 验证操作状态
        const state = useVMStore.getState()
        expect(state.operationLoading['start-vm-001']).toBe(false)
        
        // 验证service被调用
        expect(vmService.startVM).toHaveBeenCalledWith('vm-001', mockVMStartRequest)
        expect(vmService.getVMStatus).toHaveBeenCalledWith('vm-001')
      })

      it('应该处理启动失败', async () => {
        // 模拟API失败
        vi.mocked(vmService.startVM).mockRejectedValue(mockVMErrors.VM_START_FAILED)
        
        const store = useVMStore.getState()
        
        // 执行启动并期望抛出错误
        await expect(store.startVM('vm-001')).rejects.toThrow('虚拟机启动失败')
        
        // 验证错误状态
        const state = useVMStore.getState()
        expect(state.operationLoading['start-vm-001']).toBe(false)
        expect(state.operationError['start-vm-001']).toBe('虚拟机启动失败')
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
        
        const store = useVMStore.getState()
        
        // 执行停止
        await store.stopVM('vm-001', mockVMStopRequest)
        
        // 验证操作状态
        const state = useVMStore.getState()
        expect(state.operationLoading['stop-vm-001']).toBe(false)
        
        // 验证service被调用
        expect(vmService.stopVM).toHaveBeenCalledWith('vm-001', mockVMStopRequest)
      })

      it('应该处理停止失败', async () => {
        // 模拟API失败
        vi.mocked(vmService.stopVM).mockRejectedValue(mockVMErrors.VM_STOP_FAILED)
        
        const store = useVMStore.getState()
        
        // 执行停止并期望抛出错误
        await expect(store.stopVM('vm-001')).rejects.toThrow('虚拟机停止失败')
        
        // 验证错误状态
        const state = useVMStore.getState()
        expect(state.operationError['stop-vm-001']).toBe('虚拟机停止失败')
      })
    })

    describe('重启虚拟机', () => {
      it('应该成功重启虚拟机', async () => {
        // 模拟成功的API响应
        vi.mocked(vmService.restartVM).mockResolvedValue(undefined)
        vi.mocked(vmService.getVMStatus).mockResolvedValue(mockVMStatus1)
        
        const store = useVMStore.getState()
        
        // 执行重启
        await store.restartVM('vm-001')
        
        // 验证操作状态
        const state = useVMStore.getState()
        expect(state.operationLoading['restart-vm-001']).toBe(false)
        
        // 验证service被调用
        expect(vmService.restartVM).toHaveBeenCalledWith('vm-001')
      })
    })
  })

  describe('分页和查询参数管理', () => {
    it('应该正确设置分页参数', () => {
      const store = useVMStore.getState()
      
      // 设置分页
      store.setPagination(3, 15)
      
      // 验证分页状态
      const state = useVMStore.getState()
      expect(state.pagination.page).toBe(3)
      expect(state.pagination.size).toBe(15)
    })

    it('应该正确设置查询参数', () => {
      const queryParams = { status: 'RUNNING' as const, osType: 'Ubuntu 20.04' }
      
      const store = useVMStore.getState()
      store.setQueryParams(queryParams)
      
      // 验证查询参数
      const state = useVMStore.getState()
      expect(state.queryParams).toEqual(queryParams)
    })

    it('应该能够重置查询参数', () => {
      // 先设置查询参数
      useVMStore.setState({ queryParams: { status: 'RUNNING' } })
      
      const store = useVMStore.getState()
      store.resetQueryParams()
      
      // 验证查询参数被重置
      const state = useVMStore.getState()
      expect(state.queryParams).toEqual({})
    })
  })

  describe('错误处理', () => {
    it('应该能够清除所有错误', () => {
      // 设置各种错误状态
      useVMStore.setState({
        vmListError: '获取列表失败',
        currentVMError: '获取详情失败',
        operationError: {
          'start-vm-001': '启动失败',
          'stop-vm-002': '停止失败'
        }
      })
      
      const store = useVMStore.getState()
      store.clearError()
      
      // 验证错误被清除
      const state = useVMStore.getState()
      expect(state.vmListError).toBe(null)
      expect(state.currentVMError).toBe(null)
      expect(state.operationError).toEqual({})
    })

    it('应该能够清除特定VM的错误', () => {
      // 设置操作错误
      useVMStore.setState({
        operationError: {
          'start-vm-001': '启动失败',
          'stop-vm-001': '停止失败',
          'update-vm-001': '更新失败',
          'start-vm-002': '启动失败'
        }
      })
      
      const store = useVMStore.getState()
      store.clearVMError('vm-001')
      
      // 验证只有vm-001相关的错误被清除
      const state = useVMStore.getState()
      expect(state.operationError['start-vm-001']).toBeUndefined()
      expect(state.operationError['stop-vm-001']).toBeUndefined()
      expect(state.operationError['update-vm-001']).toBeUndefined()
      expect(state.operationError['start-vm-002']).toBe('启动失败')
    })
  })

  describe('状态重置', () => {
    it('应该能够重置所有状态', () => {
      // 设置一些状态
      useVMStore.setState({
        vmList: mockVMList,
        vmListTotal: 10,
        currentVM: mockVM1,
        vmStatusMap: mockVMStatusMap,
        operationLoading: { 'start-vm-001': true },
        operationError: { 'stop-vm-001': '错误' },
        pagination: { page: 2, size: 15, total: 30 },
        queryParams: { status: 'RUNNING' }
      })
      
      const store = useVMStore.getState()
      store.resetState()
      
      // 验证状态被重置为初始值
      const state = useVMStore.getState()
      expect(state.vmList).toEqual(mockInitialVMState.vmList)
      expect(state.vmListTotal).toBe(mockInitialVMState.vmListTotal)
      expect(state.vmListLoading).toBe(mockInitialVMState.vmListLoading)
      expect(state.vmListError).toBe(mockInitialVMState.vmListError)
      expect(state.currentVM).toBe(mockInitialVMState.currentVM)
      expect(state.currentVMLoading).toBe(mockInitialVMState.currentVMLoading)
      expect(state.currentVMError).toBe(mockInitialVMState.currentVMError)
      expect(state.vmStatusMap).toEqual(mockInitialVMState.vmStatusMap)
      expect(state.operationLoading).toEqual(mockInitialVMState.operationLoading)
      expect(state.operationError).toEqual(mockInitialVMState.operationError)
      expect(state.pagination).toEqual(mockInitialVMState.pagination)
      expect(state.queryParams).toEqual(mockInitialVMState.queryParams)
    })
  })

  describe('设置当前VM', () => {
    it('应该能够设置当前虚拟机', () => {
      const store = useVMStore.getState()
      store.setCurrentVM(mockVM1)
      
      // 验证当前VM被设置
      const state = useVMStore.getState()
      expect(state.currentVM).toEqual(mockVM1)
    })

    it('应该能够清除当前虚拟机', () => {
      // 先设置当前VM
      useVMStore.setState({ currentVM: mockVM1 })
      
      const store = useVMStore.getState()
      store.setCurrentVM(null)
      
      // 验证当前VM被清除
      const state = useVMStore.getState()
      expect(state.currentVM).toBe(null)
    })
  })
})
