/**
 * 管理员功能状态管理 Store
 * 管理用户管理、权限分配、系统配置等管理员功能相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { adminService } from '@/services'
import type { User, PaginatedResponse } from '@/types'
import type { 
  UserListParams,
  CreateUserRequest,
  UpdateUserRequest,
  LockUserRequest,
  ResetPasswordRequest,
  GrantPermissionRequest,
  Permission
} from '@/services'

// ==================== 状态类型定义 ====================

interface AdminState {
  // 用户列表状态
  userList: User[]
  userListTotal: number
  userListLoading: boolean
  userListError: string | null
  
  // 当前选中的用户
  currentUser: User | null
  currentUserLoading: boolean
  currentUserError: string | null
  
  
  // 操作状态
  operationLoading: Record<string, boolean>
  operationError: Record<string, string | null>
  
  // 创建用户状态
  createUserLoading: boolean
  createUserError: string | null
  
  // 分页参数
  pagination: {
    page: number
    size: number
    total: number
  }
  
  // 查询参数
  queryParams: UserListParams
  
  // 用户统计
  userStatistics: {
    totalUsers: number
    activeUsers: number
    lockedUsers: number
    adminUsers: number
    researcherUsers: number
    operatorUsers: number
    viewerUsers: number
  } | null
}

interface AdminActions {
  // 用户列表操作
  fetchUserList: (params?: UserListParams) => Promise<void>
  refreshUserList: () => Promise<void>
  
  // 用户详情操作
  fetchUserDetail: (userId: string) => Promise<void>
  setCurrentUser: (user: User | null) => void
  
  // 用户管理操作
  createUser: (userData: CreateUserRequest) => Promise<string>
  updateUser: (userId: string, userData: UpdateUserRequest) => Promise<void>
  deleteUser: (userId: string) => Promise<void>
  
  // 用户状态操作
  lockUser: (userId: string, request?: LockUserRequest) => Promise<void>
  unlockUser: (userId: string) => Promise<void>
  resetUserPassword: (userId: string, request: ResetPasswordRequest) => Promise<void>
  
  
  // 分页操作
  setPagination: (page: number, size?: number) => void
  
  // 查询参数操作
  setQueryParams: (params: UserListParams) => void
  resetQueryParams: () => void
  
  // 统计数据
  fetchUserStatistics: () => Promise<void>
  
  // 错误处理
  clearError: () => void
  clearUserError: (userId: string) => void
  
  // 状态重置
  resetState: () => void
}

type AdminStore = AdminState & AdminActions

// ==================== 初始状态 ====================

const initialState: AdminState = {
  userList: [],
  userListTotal: 0,
  userListLoading: false,
  userListError: null,
  
  currentUser: null,
  currentUserLoading: false,
  currentUserError: null,
  
  
  operationLoading: {},
  operationError: {},
  
  createUserLoading: false,
  createUserError: null,
  
  pagination: {
    page: 1,
    size: 20,
    total: 0
  },
  
  queryParams: {},
  
  userStatistics: null
}

// ==================== Store 实现 ====================

export const useAdminStore = create<AdminStore>((set, get) => ({
  ...initialState,

  // ==================== 用户列表操作 ====================
  
  /**
   * 获取用户列表
   */
  fetchUserList: async (params?: UserListParams) => {
    const { pagination, queryParams } = get()
    const finalParams = {
      ...queryParams,
      ...params,
      page: params?.page || pagination.page,
      size: params?.size || pagination.size
    }
    
    set({ userListLoading: true, userListError: null })
    
    try {
      const response = await adminService.getUserList(finalParams)
      
      // 处理不同的响应格式
      const userList = response.records || response.list || []
      
      set({
        userList,
        userListTotal: response.total,
        userListLoading: false,
        userListError: null,
        pagination: {
          page: response.page,
          size: response.size,
          total: response.total
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取用户列表失败'
      set({
        userListLoading: false,
        userListError: errorMessage
      })
      throw error
    }
  },

  /**
   * 刷新用户列表
   */
  refreshUserList: async () => {
    const { fetchUserList, queryParams, pagination } = get()
    await fetchUserList({
      ...queryParams,
      page: pagination.page,
      size: pagination.size
    })
  },

  // ==================== 用户详情操作 ====================
  
  /**
   * 获取用户详情
   */
  fetchUserDetail: async (userId: string) => {
    set({ currentUserLoading: true, currentUserError: null })
    
    try {
      const user = await adminService.getUserDetail(userId)
      
      set({
        currentUser: user,
        currentUserLoading: false,
        currentUserError: null
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取用户详情失败'
      set({
        currentUserLoading: false,
        currentUserError: errorMessage
      })
      throw error
    }
  },

  /**
   * 设置当前用户
   */
  setCurrentUser: (user: User | null) => {
    set({ currentUser: user })
  },

  // ==================== 用户管理操作 ====================
  
  /**
   * 创建用户
   */
  createUser: async (userData: CreateUserRequest) => {
    set({ createUserLoading: true, createUserError: null })
    
    try {
      const user = await adminService.createUser(userData)
      
      set({
        createUserLoading: false,
        createUserError: null
      })
      
      // 刷新用户列表
      await get().refreshUserList()
      
      return user.userId
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '创建用户失败'
      set({
        createUserLoading: false,
        createUserError: errorMessage
      })
      throw error
    }
  },

  /**
   * 更新用户信息
   */
  updateUser: async (userId: string, userData: UpdateUserRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`update-${userId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`update-${userId}`]: null
      }
    }))
    
    try {
      const updatedUser = await adminService.updateUser(userId, userData)
      
      // 更新列表中的用户信息
      set((state) => ({
        userList: state.userList.map(user => 
          user.userId === userId ? updatedUser : user
        ),
        currentUser: state.currentUser?.userId === userId 
          ? updatedUser
          : state.currentUser,
        operationLoading: {
          ...state.operationLoading,
          [`update-${userId}`]: false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新用户失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`update-${userId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`update-${userId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 删除用户
   */
  deleteUser: async (userId: string) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`delete-${userId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`delete-${userId}`]: null
      }
    }))
    
    try {
      await adminService.deleteUser(userId)
      
      // 从列表中移除
      set((state) => ({
        userList: state.userList.filter(user => user.userId !== userId),
        userListTotal: state.userListTotal - 1,
        currentUser: state.currentUser?.userId === userId ? null : state.currentUser,
        operationLoading: {
          ...state.operationLoading,
          [`delete-${userId}`]: false
        }
      }))
      
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '删除用户失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`delete-${userId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`delete-${userId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 用户状态操作 ====================
  
  /**
   * 锁定用户
   */
  lockUser: async (userId: string, request?: LockUserRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`lock-${userId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`lock-${userId}`]: null
      }
    }))
    
    try {
      await adminService.lockUser(userId, request)
      
      // 更新用户状态
      set((state) => ({
        userList: state.userList.map(user => 
          user.userId === userId ? { ...user, status: 'LOCKED' } : user
        ),
        currentUser: state.currentUser?.userId === userId 
          ? { ...state.currentUser, status: 'LOCKED' }
          : state.currentUser,
        operationLoading: {
          ...state.operationLoading,
          [`lock-${userId}`]: false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '锁定用户失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`lock-${userId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`lock-${userId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 解锁用户
   */
  unlockUser: async (userId: string) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`unlock-${userId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`unlock-${userId}`]: null
      }
    }))
    
    try {
      const result = await adminService.unlockUser(userId)
      
      // 更新用户状态
      set((state) => ({
        userList: state.userList.map(user => 
          user.userId === userId ? { ...user, status: result.status } : user
        ),
        currentUser: state.currentUser?.userId === userId 
          ? { ...state.currentUser, status: result.status }
          : state.currentUser,
        operationLoading: {
          ...state.operationLoading,
          [`unlock-${userId}`]: false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '解锁用户失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`unlock-${userId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`unlock-${userId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 重置用户密码
   */
  resetUserPassword: async (userId: string, request: ResetPasswordRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`reset-password-${userId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`reset-password-${userId}`]: null
      }
    }))
    
    try {
      await adminService.resetUserPassword(userId, request)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`reset-password-${userId}`]: false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '重置密码失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`reset-password-${userId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`reset-password-${userId}`]: errorMessage
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
  setQueryParams: (params: UserListParams) => {
    set({ queryParams: params })
  },

  /**
   * 重置查询参数
   */
  resetQueryParams: () => {
    set({ queryParams: {} })
  },

  // ==================== 统计数据 ====================
  
  /**
   * 获取用户统计
   */
  fetchUserStatistics: async () => {
    try {
      const statistics = await adminService.getUserStatistics()
      
      // 转换为组件需要的格式，添加安全检查
      const roleDistribution = statistics.roleDistribution || {} as Record<string, number>
      const transformedStats = {
        totalUsers: statistics.totalUsers || 0,
        activeUsers: statistics.activeUsers || 0,
        lockedUsers: statistics.lockedUsers || 0,
        adminUsers: roleDistribution['ADMIN'] || 0,
        researcherUsers: roleDistribution['RESEARCHER'] || 0,
        operatorUsers: roleDistribution['OPERATOR'] || 0,
        viewerUsers: roleDistribution['VIEWER'] || 0
      }
      
      set({ userStatistics: transformedStats })
    } catch (error) {
      console.error('获取用户统计失败:', error)
    }
  },

  // ==================== 错误处理 ====================
  
  /**
   * 清除错误信息
   */
  clearError: () => {
    set({
      userListError: null,
      currentUserError: null,
      createUserError: null,
      operationError: {}
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
export type { AdminState, AdminActions, AdminStore }
