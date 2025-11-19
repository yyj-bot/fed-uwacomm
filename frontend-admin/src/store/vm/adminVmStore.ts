/**
 * 管理员水下机器人管理状态管理 Store
 * 管理水下机器人分配、权限管理、概况统计等管理员VM功能相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { adminVmService } from '@/services/admin'
import type { PaginatedResponse } from '@/types'
import type {
  VirtualMachine,
  UserVm,
  VmAssignment,
  VmAssignmentDetail,
  VmAssignmentOverview,
  VmControlResponse,
  VmPermission,
  VmStatus,
  VmControlAction,
  AdminVmListParams,
  UnassignedVmListParams
} from '@/services/admin/type'

// ==================== 状态类型定义 ====================

interface AdminVmState {
  // 管理员VM列表状态
  adminVmList: VirtualMachine[]
  adminVmListTotal: number
  adminVmListLoading: boolean
  adminVmListError: string | null
  
  // VM分配信息（按vmId索引）
  vmAssignments: Map<string, {
    vmName: string
    assignments: VmAssignmentDetail[]
    totalAssignments: number
  }>
  vmAssignmentsLoading: Map<string, boolean>
  vmAssignmentsError: Map<string, string | null>
  
  // 用户VM列表（按userId索引）
  userVmMap: Map<string, {
    vms: UserVm[]
    total: number
    username: string
  }>
  userVmMapLoading: Map<string, boolean>
  userVmMapError: Map<string, string | null>
  
  // 未分配VM列表
  unassignedVms: VirtualMachine[]
  unassignedVmsTotal: number
  unassignedVmsLoading: boolean
  unassignedVmsError: string | null
  
  // VM分配概况
  assignmentOverview: VmAssignmentOverview | null
  assignmentOverviewLoading: boolean
  assignmentOverviewError: string | null
  
  // 操作状态（通用）
  operationLoading: Record<string, boolean>
  operationError: Record<string, string | null>
  
  // 分页参数
  adminVmPagination: {
    page: number
    size: number
    total: number
  }
  
  // 查询参数
  adminVmQueryParams: AdminVmListParams
}

interface AdminVmActions {
  // 管理员VM列表操作
  fetchAdminVmList: (params?: AdminVmListParams) => Promise<void>
  refreshAdminVmList: () => Promise<void>
  
  // VM分配管理操作
  assignVmToUser: (vmId: string, userId: string, params?: {
    permissions?: VmPermission[]
    notes?: string
  }) => Promise<void>
  unassignVmFromUser: (vmId: string, userId: string) => Promise<void>
  fetchVmAssignments: (vmId: string) => Promise<void>
  updateUserVmPermissions: (vmId: string, userId: string, permissions: VmPermission[]) => Promise<void>
  
  // 用户VM管理操作
  fetchUserVmList: (userId: string, params?: { status?: VmStatus; page?: number; size?: number }) => Promise<void>
  batchAssignVmsToUser: (userId: string, vmIds: string[], params?: {
    permissions?: VmPermission[]
    notes?: string
  }) => Promise<void>
  batchRemoveUserVms: (userId: string, vmIds: string[]) => Promise<void>
  
  // 未分配VM列表操作
  fetchUnassignedVmList: (params?: UnassignedVmListParams) => Promise<void>
  
  // VM分配概况操作
  fetchVmAssignmentOverview: () => Promise<void>
  
  // VM控制操作
  forceControlVm: (vmId: string, action: VmControlAction, reason: string, timeout?: number) => Promise<void>
  
  // 分页操作
  setAdminVmPagination: (page: number, size?: number) => void
  
  // 查询参数操作
  setAdminVmQueryParams: (params: AdminVmListParams) => void
  resetAdminVmQueryParams: () => void
  
  // 错误处理
  clearError: () => void
  clearVmError: (vmId: string) => void
  clearUserError: (userId: string) => void
  
  // 状态重置
  resetState: () => void
}

type AdminVmStore = AdminVmState & AdminVmActions

// ==================== 初始状态 ====================

const initialState: AdminVmState = {
  adminVmList: [],
  adminVmListTotal: 0,
  adminVmListLoading: false,
  adminVmListError: null,
  
  vmAssignments: new Map(),
  vmAssignmentsLoading: new Map(),
  vmAssignmentsError: new Map(),
  
  userVmMap: new Map(),
  userVmMapLoading: new Map(),
  userVmMapError: new Map(),
  
  unassignedVms: [],
  unassignedVmsTotal: 0,
  unassignedVmsLoading: false,
  unassignedVmsError: null,
  
  assignmentOverview: null,
  assignmentOverviewLoading: false,
  assignmentOverviewError: null,
  
  operationLoading: {},
  operationError: {},
  
  adminVmPagination: {
    page: 1,
    size: 20,
    total: 0
  },
  
  adminVmQueryParams: {}
}

// ==================== Store 实现 ====================

export const useAdminVmStore = create<AdminVmStore>((set, get) => ({
  ...initialState,

  // ==================== 管理员VM列表操作 ====================
  
  /**
   * 获取管理员VM列表
   */
  fetchAdminVmList: async (params?: AdminVmListParams) => {
    const { adminVmPagination, adminVmQueryParams } = get()
    const finalParams = {
      ...adminVmQueryParams,
      ...params,
      page: params?.page || adminVmPagination.page,
      size: params?.size || adminVmPagination.size
    }
    
    set({ adminVmListLoading: true, adminVmListError: null })
    
    try {
      const response = await adminVmService.getAdminVmList(finalParams)
      
      const adminVmList = response.records || response.list || []
      
      set({
        adminVmList,
        adminVmListTotal: response.total,
        adminVmListLoading: false,
        adminVmListError: null,
        adminVmPagination: {
          page: response.page,
          size: response.size,
          total: response.total
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取管理员水下机器人列表失败'
      set({
        adminVmListLoading: false,
        adminVmListError: errorMessage
      })
      throw error
    }
  },

  /**
   * 刷新管理员VM列表
   */
  refreshAdminVmList: async () => {
    const { fetchAdminVmList, adminVmQueryParams, adminVmPagination } = get()
    await fetchAdminVmList({
      ...adminVmQueryParams,
      page: adminVmPagination.page,
      size: adminVmPagination.size
    })
  },

  // ==================== VM分配管理操作 ====================
  
  /**
   * 分配VM给用户
   */
  assignVmToUser: async (vmId: string, userId: string, params = {}) => {
    const operationKey = `assign-${vmId}-${userId}`
    
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [operationKey]: true
      },
      operationError: {
        ...state.operationError,
        [operationKey]: null
      }
    }))
    
    try {
      await adminVmService.assignVmToUser(vmId, userId, params)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        }
      }))
      
      // 刷新相关数据
      await get().fetchVmAssignments(vmId)
      await get().refreshAdminVmList()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '分配水下机器人失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        },
        operationError: {
          ...state.operationError,
          [operationKey]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 取消VM分配
   */
  unassignVmFromUser: async (vmId: string, userId: string) => {
    const operationKey = `unassign-${vmId}-${userId}`
    
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [operationKey]: true
      },
      operationError: {
        ...state.operationError,
        [operationKey]: null
      }
    }))
    
    try {
      await adminVmService.unassignVmFromUser(vmId, userId)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        }
      }))
      
      // 刷新相关数据
      await get().fetchVmAssignments(vmId)
      await get().refreshAdminVmList()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '取消分配失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        },
        operationError: {
          ...state.operationError,
          [operationKey]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 获取VM分配情况
   */
  fetchVmAssignments: async (vmId: string) => {
    set((state) => {
      const newLoading = new Map(state.vmAssignmentsLoading)
      const newError = new Map(state.vmAssignmentsError)
      newLoading.set(vmId, true)
      newError.set(vmId, null)
      return {
        vmAssignmentsLoading: newLoading,
        vmAssignmentsError: newError
      }
    })
    
    try {
      const result = await adminVmService.getVmAssignments(vmId)
      
      set((state) => {
        const newAssignments = new Map(state.vmAssignments)
        const newLoading = new Map(state.vmAssignmentsLoading)
        
        newAssignments.set(vmId, {
          vmName: result.vmName,
          assignments: result.assignments,
          totalAssignments: result.totalAssignments
        })
        newLoading.set(vmId, false)
        
        return {
          vmAssignments: newAssignments,
          vmAssignmentsLoading: newLoading
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM分配情况失败'
      set((state) => {
        const newLoading = new Map(state.vmAssignmentsLoading)
        const newError = new Map(state.vmAssignmentsError)
        newLoading.set(vmId, false)
        newError.set(vmId, errorMessage)
        return {
          vmAssignmentsLoading: newLoading,
          vmAssignmentsError: newError
        }
      })
      throw error
    }
  },

  /**
   * 更新用户VM权限
   */
  updateUserVmPermissions: async (vmId: string, userId: string, permissions: VmPermission[]) => {
    const operationKey = `update-permissions-${vmId}-${userId}`
    
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [operationKey]: true
      },
      operationError: {
        ...state.operationError,
        [operationKey]: null
      }
    }))
    
    try {
      await adminVmService.updateUserVmPermissions(vmId, userId, permissions)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        }
      }))
      
      // 刷新VM分配情况
      await get().fetchVmAssignments(vmId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新权限失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        },
        operationError: {
          ...state.operationError,
          [operationKey]: errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 用户VM管理操作 ====================
  
  /**
   * 获取用户VM列表
   */
  fetchUserVmList: async (userId: string, params = {}) => {
    set((state) => {
      const newLoading = new Map(state.userVmMapLoading)
      const newError = new Map(state.userVmMapError)
      newLoading.set(userId, true)
      newError.set(userId, null)
      return {
        userVmMapLoading: newLoading,
        userVmMapError: newError
      }
    })
    
    try {
      const result = await adminVmService.getUserVmList(userId, params)
      
      const vms = result.records || result.list || []
      
      set((state) => {
        const newMap = new Map(state.userVmMap)
        const newLoading = new Map(state.userVmMapLoading)
        
        newMap.set(userId, {
          vms,
          total: result.total,
          username: result.username
        })
        newLoading.set(userId, false)
        
        return {
          userVmMap: newMap,
          userVmMapLoading: newLoading
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取用户VM列表失败'
      set((state) => {
        const newLoading = new Map(state.userVmMapLoading)
        const newError = new Map(state.userVmMapError)
        newLoading.set(userId, false)
        newError.set(userId, errorMessage)
        return {
          userVmMapLoading: newLoading,
          userVmMapError: newError
        }
      })
      throw error
    }
  },

  /**
   * 批量分配VM给用户
   */
  batchAssignVmsToUser: async (userId: string, vmIds: string[], params = {}) => {
    const operationKey = `batch-assign-${userId}`
    
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [operationKey]: true
      },
      operationError: {
        ...state.operationError,
        [operationKey]: null
      }
    }))
    
    try {
      await adminVmService.batchAssignVmsToUser(userId, { vmIds, ...params })
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        }
      }))
      
      // 刷新用户VM列表
      await get().fetchUserVmList(userId)
      await get().refreshAdminVmList()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '批量分配失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        },
        operationError: {
          ...state.operationError,
          [operationKey]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 批量移除用户VM
   */
  batchRemoveUserVms: async (userId: string, vmIds: string[]) => {
    const operationKey = `batch-remove-${userId}`
    
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [operationKey]: true
      },
      operationError: {
        ...state.operationError,
        [operationKey]: null
      }
    }))
    
    try {
      await adminVmService.batchRemoveUserVms(userId, { vmIds })
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        }
      }))
      
      // 刷新用户VM列表
      await get().fetchUserVmList(userId)
      await get().refreshAdminVmList()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '批量移除失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        },
        operationError: {
          ...state.operationError,
          [operationKey]: errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 未分配VM列表操作 ====================
  
  /**
   * 获取未分配VM列表
   */
  fetchUnassignedVmList: async (params = {}) => {
    set({ unassignedVmsLoading: true, unassignedVmsError: null })
    
    try {
      const result = await adminVmService.getUnassignedVmList(params)
      
      set({
        unassignedVms: result.unassignedVms,
        unassignedVmsTotal: result.total,
        unassignedVmsLoading: false,
        unassignedVmsError: null
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取未分配VM列表失败'
      set({
        unassignedVmsLoading: false,
        unassignedVmsError: errorMessage
      })
      throw error
    }
  },

  // ==================== VM分配概况操作 ====================
  
  /**
   * 获取VM分配概况
   */
  fetchVmAssignmentOverview: async () => {
    set({ assignmentOverviewLoading: true, assignmentOverviewError: null })
    
    try {
      const overview = await adminVmService.getVmAssignmentOverview()
      
      set({
        assignmentOverview: overview,
        assignmentOverviewLoading: false,
        assignmentOverviewError: null
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM分配概况失败'
      set({
        assignmentOverviewLoading: false,
        assignmentOverviewError: errorMessage
      })
      throw error
    }
  },

  // ==================== VM控制操作 ====================
  
  /**
   * 强制控制VM
   */
  forceControlVm: async (vmId: string, action: VmControlAction, reason: string, timeout?: number) => {
    const operationKey = `force-control-${vmId}-${action}`
    
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [operationKey]: true
      },
      operationError: {
        ...state.operationError,
        [operationKey]: null
      }
    }))
    
    try {
      await adminVmService.forceControlVm(vmId, { action, reason, timeout })
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        }
      }))
      
      // 刷新VM列表以更新状态
      await get().refreshAdminVmList()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '强制控制VM失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [operationKey]: false
        },
        operationError: {
          ...state.operationError,
          [operationKey]: errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 分页操作 ====================
  
  /**
   * 设置分页参数
   */
  setAdminVmPagination: (page: number, size?: number) => {
    set((state) => ({
      adminVmPagination: {
        ...state.adminVmPagination,
        page,
        size: size || state.adminVmPagination.size
      }
    }))
  },

  // ==================== 查询参数操作 ====================
  
  /**
   * 设置查询参数
   */
  setAdminVmQueryParams: (params: AdminVmListParams) => {
    set({ adminVmQueryParams: params })
  },

  /**
   * 重置查询参数
   */
  resetAdminVmQueryParams: () => {
    set({ adminVmQueryParams: {} })
  },

  // ==================== 错误处理 ====================
  
  /**
   * 清除所有错误信息
   */
  clearError: () => {
    set({
      adminVmListError: null,
      unassignedVmsError: null,
      assignmentOverviewError: null,
      operationError: {},
      vmAssignmentsError: new Map(),
      userVmMapError: new Map()
    })
  },

  /**
   * 清除特定VM的错误信息
   */
  clearVmError: (vmId: string) => {
    set((state) => {
      const newOperationError = { ...state.operationError }
      Object.keys(newOperationError).forEach(key => {
        if (key.includes(vmId)) {
          delete newOperationError[key]
        }
      })
      
      const newVmAssignmentsError = new Map(state.vmAssignmentsError)
      newVmAssignmentsError.delete(vmId)
      
      return {
        operationError: newOperationError,
        vmAssignmentsError: newVmAssignmentsError
      }
    })
  },

  /**
   * 清除特定用户的错误信息
   */
  clearUserError: (userId: string) => {
    set((state) => {
      const newOperationError = { ...state.operationError }
      Object.keys(newOperationError).forEach(key => {
        if (key.includes(userId)) {
          delete newOperationError[key]
        }
      })
      
      const newUserVmMapError = new Map(state.userVmMapError)
      newUserVmMapError.delete(userId)
      
      return {
        operationError: newOperationError,
        userVmMapError: newUserVmMapError
      }
    })
  },

  // ==================== 状态重置 ====================
  
  /**
   * 重置所有状态
   */
  resetState: () => {
    set(initialState)
  }
}))

// ==================== 导出类型 ====================
export type { AdminVmState, AdminVmActions, AdminVmStore }

