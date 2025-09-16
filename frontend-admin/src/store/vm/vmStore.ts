/**
 * 虚拟机管理状态 Store
 * 管理虚拟机列表、详情、操作状态等相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { vmService } from '@/services'
import type { VirtualMachine, VMStatus } from '@/api/vm'
import type { 
  VMListParams,
  VMUpdateRequest,
  VMStartRequest,
  VMStopRequest
} from '@/services'

// ==================== 状态类型定义 ====================

interface VMState {
  // 虚拟机列表状态
  vmList: VirtualMachine[]
  vmListTotal: number
  vmListLoading: boolean
  vmListError: string | null
  
  // 当前选中的虚拟机
  currentVM: VirtualMachine | null
  currentVMLoading: boolean
  currentVMError: string | null
  
  // 虚拟机状态
  vmStatusMap: Record<string, VMStatus>
  
  // 操作状态
  operationLoading: Record<string, boolean>
  operationError: Record<string, string | null>
  
  // 分页参数
  pagination: {
    page: number
    size: number
    total: number
  }
  
  // 查询参数
  queryParams: VMListParams
}

interface VMActions {
  // 虚拟机列表操作
  fetchVMList: (params?: VMListParams) => Promise<void>
  refreshVMList: () => Promise<void>
  
  // 虚拟机详情操作
  fetchVMDetail: (vmId: string) => Promise<void>
  setCurrentVM: (vm: VirtualMachine | null) => void
  
  // 虚拟机状态操作
  fetchVMStatus: (vmId: string) => Promise<void>
  fetchAllVMStatus: () => Promise<void>
  
  // 虚拟机管理操作
  updateVM: (vmId: string, data: VMUpdateRequest) => Promise<void>
  deleteVM: (vmId: string, force?: boolean) => Promise<void>
  
  // 虚拟机控制操作
  startVM: (vmId: string, data?: VMStartRequest) => Promise<void>
  stopVM: (vmId: string, data?: VMStopRequest) => Promise<void>
  restartVM: (vmId: string) => Promise<void>
  
  // 分页操作
  setPagination: (page: number, size?: number) => void
  
  // 查询参数操作
  setQueryParams: (params: VMListParams) => void
  resetQueryParams: () => void
  
  // 错误处理
  clearError: () => void
  clearVMError: (vmId: string) => void
  
  // 状态重置
  resetState: () => void
}

type VMStore = VMState & VMActions

// ==================== 初始状态 ====================

const initialState: VMState = {
  vmList: [],
  vmListTotal: 0,
  vmListLoading: false,
  vmListError: null,
  
  currentVM: null,
  currentVMLoading: false,
  currentVMError: null,
  
  vmStatusMap: {},
  
  operationLoading: {},
  operationError: {},
  
  pagination: {
    page: 1,
    size: 20,
    total: 0
  },
  
  queryParams: {}
}

// ==================== Store 实现 ====================

export const useVMStore = create<VMStore>((set, get) => ({
  ...initialState,

  // ==================== 虚拟机列表操作 ====================
  
  /**
   * 获取虚拟机列表
   */
  fetchVMList: async (params?: VMListParams) => {
    const { pagination, queryParams } = get()
    const finalParams = {
      ...queryParams,
      ...params,
      page: params?.page || pagination.page,
      size: params?.size || pagination.size
    }
    
    set({ vmListLoading: true, vmListError: null })
    
    try {
      const response = await vmService.getVMList(finalParams)
      
      set({
        vmList: response.list,
        vmListTotal: response.total,
        vmListLoading: false,
        vmListError: null,
        pagination: {
          page: response.page,
          size: response.size,
          total: response.total
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取虚拟机列表失败'
      set({
        vmListLoading: false,
        vmListError: errorMessage
      })
      throw error
    }
  },

  /**
   * 刷新虚拟机列表
   */
  refreshVMList: async () => {
    const { fetchVMList, queryParams, pagination } = get()
    await fetchVMList({
      ...queryParams,
      page: pagination.page,
      size: pagination.size
    })
  },

  // ==================== 虚拟机详情操作 ====================
  
  /**
   * 获取虚拟机详情
   */
  fetchVMDetail: async (vmId: string) => {
    set({ currentVMLoading: true, currentVMError: null })
    
    try {
      const vm = await vmService.getVMDetail(vmId)
      
      set({
        currentVM: vm,
        currentVMLoading: false,
        currentVMError: null
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取虚拟机详情失败'
      set({
        currentVMLoading: false,
        currentVMError: errorMessage
      })
      throw error
    }
  },

  /**
   * 设置当前虚拟机
   */
  setCurrentVM: (vm: VirtualMachine | null) => {
    set({ currentVM: vm })
  },

  // ==================== 虚拟机状态操作 ====================
  
  /**
   * 获取虚拟机状态
   */
  fetchVMStatus: async (vmId: string) => {
    try {
      const status = await vmService.getVMStatus(vmId)
      
      set((state) => ({
        vmStatusMap: {
          ...state.vmStatusMap,
          [vmId]: status
        }
      }))
    } catch (error) {
      console.error(`获取虚拟机状态失败 (${vmId}):`, error)
    }
  },

  /**
   * 获取所有虚拟机状态
   */
  fetchAllVMStatus: async () => {
    const { vmList } = get()
    
    const promises = vmList.map(vm => 
      get().fetchVMStatus(vm.vmId).catch(console.error)
    )
    
    await Promise.allSettled(promises)
  },

  // ==================== 虚拟机管理操作 ====================
  
  /**
   * 更新虚拟机信息
   */
  updateVM: async (vmId: string, data: VMUpdateRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`update-${vmId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`update-${vmId}`]: null
      }
    }))
    
    try {
      await vmService.updateVM(vmId, data)
      
      // 更新列表中的虚拟机信息
      set((state) => ({
        vmList: state.vmList.map(vm => 
          vm.vmId === vmId ? { ...vm, ...data } : vm
        ),
        currentVM: state.currentVM?.vmId === vmId 
          ? { ...state.currentVM, ...data }
          : state.currentVM,
        operationLoading: {
          ...state.operationLoading,
          [`update-${vmId}`]: false
        }
      }))
      
      // 重新获取详情
      await get().fetchVMDetail(vmId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新虚拟机失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`update-${vmId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`update-${vmId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 删除虚拟机
   */
  deleteVM: async (vmId: string, force = false) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`delete-${vmId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`delete-${vmId}`]: null
      }
    }))
    
    try {
      await vmService.deleteVM(vmId, force)
      
      // 从列表中移除
      set((state) => ({
        vmList: state.vmList.filter(vm => vm.vmId !== vmId),
        vmListTotal: state.vmListTotal - 1,
        currentVM: state.currentVM?.vmId === vmId ? null : state.currentVM,
        operationLoading: {
          ...state.operationLoading,
          [`delete-${vmId}`]: false
        }
      }))
      
      // 删除状态映射
      set((state) => {
        const newStatusMap = { ...state.vmStatusMap }
        delete newStatusMap[vmId]
        return { vmStatusMap: newStatusMap }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '删除虚拟机失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`delete-${vmId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`delete-${vmId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 虚拟机控制操作 ====================
  
  /**
   * 启动虚拟机
   */
  startVM: async (vmId: string, data?: VMStartRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`start-${vmId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`start-${vmId}`]: null
      }
    }))
    
    try {
      await vmService.startVM(vmId, data)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`start-${vmId}`]: false
        }
      }))
      
      // 更新状态
      await get().fetchVMStatus(vmId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '启动虚拟机失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`start-${vmId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`start-${vmId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 停止虚拟机
   */
  stopVM: async (vmId: string, data?: VMStopRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`stop-${vmId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`stop-${vmId}`]: null
      }
    }))
    
    try {
      await vmService.stopVM(vmId, data)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`stop-${vmId}`]: false
        }
      }))
      
      // 更新状态
      await get().fetchVMStatus(vmId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '停止虚拟机失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`stop-${vmId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`stop-${vmId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 重启虚拟机
   */
  restartVM: async (vmId: string) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`restart-${vmId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`restart-${vmId}`]: null
      }
    }))
    
    try {
      await vmService.restartVM(vmId)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`restart-${vmId}`]: false
        }
      }))
      
      // 更新状态
      await get().fetchVMStatus(vmId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '重启虚拟机失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`restart-${vmId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`restart-${vmId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 分页操作 ====================
  
  /**
   * 设置分页参数
   */
  setPagination: (page: number, size?: number) => {
    set((state) => ({
      pagination: {
        ...state.pagination,
        page,
        size: size || state.pagination.size
      }
    }))
  },

  // ==================== 查询参数操作 ====================
  
  /**
   * 设置查询参数
   */
  setQueryParams: (params: VMListParams) => {
    set({ queryParams: params })
  },

  /**
   * 重置查询参数
   */
  resetQueryParams: () => {
    set({ queryParams: {} })
  },

  // ==================== 错误处理 ====================
  
  /**
   * 清除错误信息
   */
  clearError: () => {
    set({
      vmListError: null,
      currentVMError: null,
      operationError: {}
    })
  },

  /**
   * 清除特定虚拟机的错误信息
   */
  clearVMError: (vmId: string) => {
    set((state) => {
      const newOperationError = { ...state.operationError }
      Object.keys(newOperationError).forEach(key => {
        if (key.includes(vmId)) {
          delete newOperationError[key]
        }
      })
      return { operationError: newOperationError }
    })
  },

  // ==================== 状态重置 ====================
  
  /**
   * 重置状态
   */
  resetState: () => {
    set(initialState)
  }
}))

// ==================== 导出类型 ====================
export type { VMState, VMActions, VMStore }
