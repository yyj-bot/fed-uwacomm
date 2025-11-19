/**
 * 水下机器人管理状态 Store
 * 管理水下机器人列表、详情、操作状态等相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { vmService } from '@/services'
import type { 
  VirtualMachine, 
  VMStatus,
  VMRoundModel,
  VMModelTrend,
  VMModelBest
} from '@/api/vm'
import type { 
  VMListParams,
  VMUpdateRequest,
  VMStartRequest,
  VMStopRequest,
  VMRestartRequest,
  VMRoundModelListParams,
  VMModelTrendParams,
  VMModelBestParams,
  VMRoundModelPaginatedResponse
} from '@/services'

// ==================== 状态类型定义 ====================

interface VMState {
  // 水下机器人列表状态
  vmList: VirtualMachine[]
  vmListTotal: number
  vmListLoading: boolean
  vmListError: string | null
  
  // 当前选中的水下机器人
  currentVM: VirtualMachine | null
  currentVMLoading: boolean
  currentVMError: string | null
  
  // 水下机器人状态
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
  
  // VM本地模型状态
  vmRoundModels: VMRoundModel[]
  vmRoundModelsTotal: number
  vmRoundModelsLoading: boolean
  vmRoundModelsError: string | null
  
  // 当前选中的VM本地模型
  currentVMRoundModel: VMRoundModel | null
  currentVMRoundModelLoading: boolean
  currentVMRoundModelError: string | null
  
  // VM模型趋势数据
  vmModelTrends: Record<string, VMModelTrend>
  vmModelTrendsLoading: Record<string, boolean>
  vmModelTrendsError: Record<string, string | null>
  
  // VM模型最佳/离群数据
  vmModelBests: Record<string, VMModelBest>
  vmModelBestsLoading: Record<string, boolean>
  vmModelBestsError: Record<string, string | null>
  
  // VM本地模型分页参数
  vmRoundModelsPagination: {
    current: number
    size: number
    total: number
  }
  
  // VM本地模型查询参数
  vmRoundModelsQueryParams: VMRoundModelListParams
}

interface VMActions {
  // 水下机器人列表操作
  fetchVMList: (params?: VMListParams) => Promise<void>
  refreshVMList: () => Promise<void>
  
  // 水下机器人详情操作
  fetchVMDetail: (vmId: string) => Promise<void>
  setCurrentVM: (vm: VirtualMachine | null) => void
  
  // 水下机器人状态操作
  fetchVMStatus: (vmId: string) => Promise<void>
  fetchAllVMStatus: () => Promise<void>
  
  // 水下机器人管理操作
  updateVM: (vmId: string, data: VMUpdateRequest) => Promise<void>
  deleteVM: (vmId: string, force?: boolean) => Promise<void>
  
  // 水下机器人控制操作
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
  
  // VM本地模型操作
  fetchVMRoundModels: (params?: VMRoundModelListParams) => Promise<void>
  refreshVMRoundModels: () => Promise<void>
  
  // VM本地模型详情操作
  fetchVMRoundModelDetail: (vmRoundModelId: string) => Promise<void>
  setCurrentVMRoundModel: (model: VMRoundModel | null) => void
  
  // VM模型趋势操作
  fetchVMModelTrend: (params: VMModelTrendParams) => Promise<void>
  
  // VM模型最佳/离群操作
  fetchVMModelBest: (params: VMModelBestParams) => Promise<void>
  
  // VM本地模型分页操作
  setVMRoundModelsPagination: (current: number, size?: number) => void
  
  // VM本地模型查询参数操作
  setVMRoundModelsQueryParams: (params: VMRoundModelListParams) => void
  resetVMRoundModelsQueryParams: () => void
  
  // VM本地模型错误处理
  clearVMRoundModelsError: () => void
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
  
  queryParams: {},
  
  // VM本地模型初始状态
  vmRoundModels: [],
  vmRoundModelsTotal: 0,
  vmRoundModelsLoading: false,
  vmRoundModelsError: null,
  
  currentVMRoundModel: null,
  currentVMRoundModelLoading: false,
  currentVMRoundModelError: null,
  
  vmModelTrends: {},
  vmModelTrendsLoading: {},
  vmModelTrendsError: {},
  
  vmModelBests: {},
  vmModelBestsLoading: {},
  vmModelBestsError: {},
  
  vmRoundModelsPagination: {
    current: 1,
    size: 10,
    total: 0
  },
  
  vmRoundModelsQueryParams: {}
}

// ==================== Store 实现 ====================

export const useVMStore = create<VMStore>((set, get) => ({
  ...initialState,

  // ==================== 水下机器人列表操作 ====================
  
  /**
   * 获取水下机器人列表
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
      const errorMessage = error instanceof Error ? error.message : '获取水下机器人列表失败'
      set({
        vmListLoading: false,
        vmListError: errorMessage
      })
      throw error
    }
  },

  /**
   * 刷新水下机器人列表
   */
  refreshVMList: async () => {
    const { fetchVMList, queryParams, pagination } = get()
    await fetchVMList({
      ...queryParams,
      page: pagination.page,
      size: pagination.size
    })
  },

  // ==================== 水下机器人详情操作 ====================
  
  /**
   * 获取水下机器人详情
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
      const errorMessage = error instanceof Error ? error.message : '获取水下机器人详情失败'
      set({
        currentVMLoading: false,
        currentVMError: errorMessage
      })
      throw error
    }
  },

  /**
   * 设置当前水下机器人
   */
  setCurrentVM: (vm: VirtualMachine | null) => {
    set({ currentVM: vm })
  },

  // ==================== 水下机器人状态操作 ====================
  
  /**
   * 获取水下机器人状态
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
      console.error(`获取水下机器人状态失败 (${vmId}):`, error)
    }
  },

  /**
   * 获取所有水下机器人状态
   */
  fetchAllVMStatus: async () => {
    const { vmList } = get()
    
    const promises = vmList.map(vm => 
      get().fetchVMStatus(vm.vmId).catch(console.error)
    )
    
    await Promise.allSettled(promises)
  },

  // ==================== 水下机器人管理操作 ====================
  
  /**
   * 更新水下机器人信息
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
      
      // 更新列表中的水下机器人信息
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
      const errorMessage = error instanceof Error ? error.message : '更新水下机器人失败'
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
   * 删除水下机器人
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
      const errorMessage = error instanceof Error ? error.message : '删除水下机器人失败'
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

  // ==================== 水下机器人控制操作 ====================
  
  /**
   * 启动水下机器人
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
      const errorMessage = error instanceof Error ? error.message : '启动水下机器人失败'
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
   * 停止水下机器人
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
      const errorMessage = error instanceof Error ? error.message : '停止水下机器人失败'
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
   * 重启水下机器人
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
      const errorMessage = error instanceof Error ? error.message : '重启水下机器人失败'
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
   * 清除特定水下机器人的错误信息
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
  },

  // ==================== VM本地模型操作 ====================
  
  /**
   * 获取VM本地模型列表
   */
  fetchVMRoundModels: async (params?: VMRoundModelListParams) => {
    const { vmRoundModelsPagination, vmRoundModelsQueryParams } = get()
    const finalParams = {
      ...vmRoundModelsQueryParams,
      ...params,
      page: params?.page || vmRoundModelsPagination.current,
      size: params?.size || vmRoundModelsPagination.size
    }
    
    set({ vmRoundModelsLoading: true, vmRoundModelsError: null })
    
    try {
      const response = await vmService.getVMRoundModels(finalParams)
      
      set({
        vmRoundModels: response.records,
        vmRoundModelsTotal: response.total,
        vmRoundModelsLoading: false,
        vmRoundModelsError: null,
        vmRoundModelsPagination: {
          current: response.current,
          size: response.size,
          total: response.total
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM本地模型列表失败'
      set({
        vmRoundModelsLoading: false,
        vmRoundModelsError: errorMessage
      })
      throw error
    }
  },

  /**
   * 刷新VM本地模型列表
   */
  refreshVMRoundModels: async () => {
    const { fetchVMRoundModels, vmRoundModelsQueryParams, vmRoundModelsPagination } = get()
    await fetchVMRoundModels({
      ...vmRoundModelsQueryParams,
      page: vmRoundModelsPagination.current,
      size: vmRoundModelsPagination.size
    })
  },

  /**
   * 获取VM本地模型详情
   */
  fetchVMRoundModelDetail: async (vmRoundModelId: string) => {
    set({ currentVMRoundModelLoading: true, currentVMRoundModelError: null })
    
    try {
      const model = await vmService.getVMRoundModelDetail(vmRoundModelId)
      
      set({
        currentVMRoundModel: model,
        currentVMRoundModelLoading: false,
        currentVMRoundModelError: null
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM本地模型详情失败'
      set({
        currentVMRoundModelLoading: false,
        currentVMRoundModelError: errorMessage
      })
      throw error
    }
  },

  /**
   * 设置当前VM本地模型
   */
  setCurrentVMRoundModel: (model: VMRoundModel | null) => {
    set({ currentVMRoundModel: model })
  },

  /**
   * 获取VM模型训练指标趋势
   */
  fetchVMModelTrend: async (params: VMModelTrendParams) => {
    const trendKey = `${params.taskId}-${params.vmId}-${params.metric}`
    
    set((state) => ({
      vmModelTrendsLoading: {
        ...state.vmModelTrendsLoading,
        [trendKey]: true
      },
      vmModelTrendsError: {
        ...state.vmModelTrendsError,
        [trendKey]: null
      }
    }))
    
    try {
      const trend = await vmService.getVMModelTrend(params)
      
      set((state) => ({
        vmModelTrends: {
          ...state.vmModelTrends,
          [trendKey]: trend
        },
        vmModelTrendsLoading: {
          ...state.vmModelTrendsLoading,
          [trendKey]: false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM模型训练指标趋势失败'
      set((state) => ({
        vmModelTrendsLoading: {
          ...state.vmModelTrendsLoading,
          [trendKey]: false
        },
        vmModelTrendsError: {
          ...state.vmModelTrendsError,
          [trendKey]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 获取VM模型最佳/离群结果
   */
  fetchVMModelBest: async (params: VMModelBestParams) => {
    const bestKey = `${params.taskId}-${params.metric}-${params.type}`
    
    set((state) => ({
      vmModelBestsLoading: {
        ...state.vmModelBestsLoading,
        [bestKey]: true
      },
      vmModelBestsError: {
        ...state.vmModelBestsError,
        [bestKey]: null
      }
    }))
    
    try {
      const best = await vmService.getVMModelBest(params)
      
      set((state) => ({
        vmModelBests: {
          ...state.vmModelBests,
          [bestKey]: best
        },
        vmModelBestsLoading: {
          ...state.vmModelBestsLoading,
          [bestKey]: false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM模型最佳/离群结果失败'
      set((state) => ({
        vmModelBestsLoading: {
          ...state.vmModelBestsLoading,
          [bestKey]: false
        },
        vmModelBestsError: {
          ...state.vmModelBestsError,
          [bestKey]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 设置VM本地模型分页参数
   */
  setVMRoundModelsPagination: (current: number, size?: number) => {
    set((state) => ({
      vmRoundModelsPagination: {
        ...state.vmRoundModelsPagination,
        current,
        size: size || state.vmRoundModelsPagination.size
      }
    }))
  },

  /**
   * 设置VM本地模型查询参数
   */
  setVMRoundModelsQueryParams: (params: VMRoundModelListParams) => {
    set({ vmRoundModelsQueryParams: params })
  },

  /**
   * 重置VM本地模型查询参数
   */
  resetVMRoundModelsQueryParams: () => {
    set({ vmRoundModelsQueryParams: {} })
  },

  /**
   * 清除VM本地模型错误信息
   */
  clearVMRoundModelsError: () => {
    set({
      vmRoundModelsError: null,
      currentVMRoundModelError: null,
      vmModelTrendsError: {},
      vmModelBestsError: {}
    })
  }
}))

// ==================== 导出类型 ====================
export type { VMState, VMActions, VMStore }
