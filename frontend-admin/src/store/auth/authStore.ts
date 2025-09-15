/**
 * 认证状态管理 Store
 * 管理用户登录、注册、个人信息等认证相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import { userService } from '@/services'
import type { 
  User, 
  LoginRequest, 
  LoginResponse,
  RegisterUserRequest,
  UpdateProfileRequest,
  ChangePasswordRequest
} from '@/services'

// ==================== 状态类型定义 ====================

interface AuthState {
  // 基础状态
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
  
  // 用户信息
  user: User | null
  token: string | null
  refreshToken: string | null
  
  // 登录状态
  loginStatus: 'idle' | 'logging' | 'success' | 'failed'
  
  // 注册状态
  registerStatus: 'idle' | 'registering' | 'success' | 'failed'
  
  // 操作状态
  updateProfileStatus: 'idle' | 'updating' | 'success' | 'failed'
  changePasswordStatus: 'idle' | 'changing' | 'success' | 'failed'
}

interface AuthActions {
  // 登录相关
  login: (loginData: LoginRequest) => Promise<void>
  logout: () => Promise<void>
  refreshAuthToken: () => Promise<void>
  
  // 注册相关
  register: (userData: RegisterUserRequest) => Promise<void>
  
  // 用户信息相关
  fetchUserProfile: () => Promise<void>
  updateProfile: (userData: UpdateProfileRequest) => Promise<void>
  changePassword: (passwordData: ChangePasswordRequest) => Promise<void>
  
  // 状态管理
  clearError: () => void
  resetStatus: () => void
  setUser: (user: User | null) => void
  
  // 认证检查
  checkAuthStatus: () => boolean
  initializeAuth: () => Promise<void>
}

type AuthStore = AuthState & AuthActions

// ==================== Store 实现 ====================

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      // ==================== 初始状态 ====================
      isAuthenticated: false,
      isLoading: false,
      error: null,
      user: null,
      token: null,
      refreshToken: null,
      loginStatus: 'idle',
      registerStatus: 'idle',
      updateProfileStatus: 'idle',
      changePasswordStatus: 'idle',

      // ==================== 登录相关操作 ====================
      
      /**
       * 用户登录
       */
      login: async (loginData: LoginRequest) => {
        set({ isLoading: true, loginStatus: 'logging', error: null })
        
        try {
          const response: LoginResponse = await userService.login(loginData)
          
          set({
            isAuthenticated: true,
            user: response.user,
            token: response.token,
            refreshToken: response.refreshToken,
            loginStatus: 'success',
            isLoading: false,
            error: null
          })
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : '登录失败'
          set({
            isAuthenticated: false,
            user: null,
            token: null,
            refreshToken: null,
            loginStatus: 'failed',
            isLoading: false,
            error: errorMessage
          })
          throw error
        }
      },

      /**
       * 用户登出
       */
      logout: async () => {
        set({ isLoading: true })
        
        try {
          await userService.logout()
        } catch (error) {
          console.warn('登出API调用失败:', error)
        } finally {
          // 无论API是否成功，都清除本地状态
          set({
            isAuthenticated: false,
            user: null,
            token: null,
            refreshToken: null,
            loginStatus: 'idle',
            registerStatus: 'idle',
            updateProfileStatus: 'idle',
            changePasswordStatus: 'idle',
            isLoading: false,
            error: null
          })
        }
      },

      /**
       * 刷新访问令牌
       */
      refreshAuthToken: async () => {
        const { refreshToken: currentRefreshToken } = get()
        
        if (!currentRefreshToken) {
          throw new Error('无刷新令牌，请重新登录')
        }
        
        try {
          const response = await userService.refreshToken()
          
          set({
            token: response.token,
            refreshToken: response.refreshToken,
            error: null
          })
        } catch (error) {
          // 刷新失败，清除认证状态
          set({
            isAuthenticated: false,
            user: null,
            token: null,
            refreshToken: null,
            error: '令牌刷新失败，请重新登录'
          })
          throw error
        }
      },

      // ==================== 注册相关操作 ====================
      
      /**
       * 用户注册
       */
      register: async (userData: RegisterUserRequest) => {
        set({ isLoading: true, registerStatus: 'registering', error: null })
        
        try {
          const user = await userService.register(userData)
          
          set({
            registerStatus: 'success',
            isLoading: false,
            error: null
          })
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : '注册失败'
          set({
            registerStatus: 'failed',
            isLoading: false,
            error: errorMessage
          })
          throw error
        }
      },

      // ==================== 用户信息相关操作 ====================
      
      /**
       * 获取用户信息
       */
      fetchUserProfile: async () => {
        const { isAuthenticated } = get()
        
        if (!isAuthenticated) {
          throw new Error('用户未登录')
        }
        
        set({ isLoading: true, error: null })
        
        try {
          const user = await userService.getProfile()
          
          set({
            user,
            isLoading: false,
            error: null
          })
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : '获取用户信息失败'
          set({
            isLoading: false,
            error: errorMessage
          })
          throw error
        }
      },

      /**
       * 更新用户信息
       */
      updateProfile: async (userData: UpdateProfileRequest) => {
        set({ updateProfileStatus: 'updating', error: null })
        
        try {
          const updatedUser = await userService.updateProfile(userData)
          
          set({
            user: updatedUser,
            updateProfileStatus: 'success',
            error: null
          })
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : '更新用户信息失败'
          set({
            updateProfileStatus: 'failed',
            error: errorMessage
          })
          throw error
        }
      },

      /**
       * 修改密码
       */
      changePassword: async (passwordData: ChangePasswordRequest) => {
        set({ changePasswordStatus: 'changing', error: null })
        
        try {
          await userService.changePassword(passwordData)
          
          set({
            changePasswordStatus: 'success',
            error: null
          })
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : '修改密码失败'
          set({
            changePasswordStatus: 'failed',
            error: errorMessage
          })
          throw error
        }
      },

      // ==================== 状态管理操作 ====================
      
      /**
       * 清除错误信息
       */
      clearError: () => {
        set({ error: null })
      },

      /**
       * 重置操作状态
       */
      resetStatus: () => {
        set({
          loginStatus: 'idle',
          registerStatus: 'idle',
          updateProfileStatus: 'idle',
          changePasswordStatus: 'idle'
        })
      },

      /**
       * 设置用户信息
       */
      setUser: (user: User | null) => {
        set({ user })
      },

      // ==================== 认证检查 ====================
      
      /**
       * 检查认证状态
       */
      checkAuthStatus: () => {
        const { token } = get()
        const isValid = userService.isAuthenticated() && !!token
        
        if (!isValid) {
          set({
            isAuthenticated: false,
            user: null,
            token: null,
            refreshToken: null
          })
        }
        
        return isValid
      },

      /**
       * 初始化认证状态
       */
      initializeAuth: async () => {
        const { token, checkAuthStatus } = get()
        
        if (!token || !checkAuthStatus()) {
          return
        }
        
        try {
          // 尝试获取用户信息来验证token有效性
          const user = await userService.getProfile()
          set({
            isAuthenticated: true,
            user,
            error: null
          })
        } catch (error) {
          console.warn('初始化认证失败，清除本地认证状态:', error)
          set({
            isAuthenticated: false,
            user: null,
            token: null,
            refreshToken: null,
            error: null
          })
        }
      }
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => localStorage),
      // 只持久化必要的状态
      partialize: (state) => ({
        isAuthenticated: state.isAuthenticated,
        user: state.user,
        token: state.token,
        refreshToken: state.refreshToken
      })
    }
  )
)

// ==================== 导出类型 ====================
export type { AuthState, AuthActions, AuthStore }
