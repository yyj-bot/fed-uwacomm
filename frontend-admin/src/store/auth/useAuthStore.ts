/**
 * 认证 Hook - 封装认证状态和操作
 * 为组件层提供简洁的认证功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback } from 'react'
import { useAuthStore } from './authStore'
import type { 
  LoginRequest, 
  RegisterUserRequest,
  UpdateProfileRequest,
  ChangePasswordRequest
} from '@/services'

// ==================== Hook 实现 ====================

export const useAuth = () => {
  // 获取状态
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const isLoading = useAuthStore((state) => state.isLoading)
  const error = useAuthStore((state) => state.error)
  const user = useAuthStore((state) => state.user)
  const loginStatus = useAuthStore((state) => state.loginStatus)
  const registerStatus = useAuthStore((state) => state.registerStatus)
  const updateProfileStatus = useAuthStore((state) => state.updateProfileStatus)
  const changePasswordStatus = useAuthStore((state) => state.changePasswordStatus)

  // 获取操作方法
  const loginAction = useAuthStore((state) => state.login)
  const logoutAction = useAuthStore((state) => state.logout)
  const refreshTokenAction = useAuthStore((state) => state.refreshAuthToken)
  const registerAction = useAuthStore((state) => state.register)
  const fetchUserProfileAction = useAuthStore((state) => state.fetchUserProfile)
  const updateProfileAction = useAuthStore((state) => state.updateProfile)
  const changePasswordAction = useAuthStore((state) => state.changePassword)
  const clearErrorAction = useAuthStore((state) => state.clearError)
  const resetStatusAction = useAuthStore((state) => state.resetStatus)
  const checkAuthStatusAction = useAuthStore((state) => state.checkAuthStatus)
  const initializeAuthAction = useAuthStore((state) => state.initializeAuth)

  // ==================== 封装操作方法 ====================

  /**
   * 登录
   */
  const login = useCallback(async (loginData: LoginRequest) => {
    try {
      await loginAction(loginData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '登录失败'
      return { success: false, error: errorMessage }
    }
  }, [loginAction])

  /**
   * 登出
   */
  const logout = useCallback(async () => {
    try {
      await logoutAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '登出失败'
      return { success: false, error: errorMessage }
    }
  }, [logoutAction])

  /**
   * 注册
   */
  const register = useCallback(async (userData: RegisterUserRequest) => {
    try {
      await registerAction(userData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '注册失败'
      return { success: false, error: errorMessage }
    }
  }, [registerAction])

  /**
   * 获取用户信息
   */
  const fetchUserProfile = useCallback(async () => {
    try {
      await fetchUserProfileAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取用户信息失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchUserProfileAction])

  /**
   * 更新用户信息
   */
  const updateProfile = useCallback(async (userData: UpdateProfileRequest) => {
    try {
      await updateProfileAction(userData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新用户信息失败'
      return { success: false, error: errorMessage }
    }
  }, [updateProfileAction])

  /**
   * 修改密码
   */
  const changePassword = useCallback(async (passwordData: ChangePasswordRequest) => {
    try {
      await changePasswordAction(passwordData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '修改密码失败'
      return { success: false, error: errorMessage }
    }
  }, [changePasswordAction])

  /**
   * 刷新令牌
   */
  const refreshToken = useCallback(async () => {
    try {
      await refreshTokenAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新令牌失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshTokenAction])

  /**
   * 清除错误
   */
  const clearError = useCallback(() => {
    clearErrorAction()
  }, [clearErrorAction])

  /**
   * 重置状态
   */
  const resetStatus = useCallback(() => {
    resetStatusAction()
  }, [resetStatusAction])

  /**
   * 检查认证状态
   */
  const checkAuthStatus = useCallback(() => {
    return checkAuthStatusAction()
  }, [checkAuthStatusAction])

  /**
   * 初始化认证
   */
  const initializeAuth = useCallback(async () => {
    try {
      await initializeAuthAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '初始化认证失败'
      return { success: false, error: errorMessage }
    }
  }, [initializeAuthAction])

  // ==================== 计算属性 ====================

  /**
   * 是否正在登录
   */
  const isLoggingIn = loginStatus === 'logging'

  /**
   * 是否正在注册
   */
  const isRegistering = registerStatus === 'registering'

  /**
   * 是否正在更新个人信息
   */
  const isUpdatingProfile = updateProfileStatus === 'updating'

  /**
   * 是否正在修改密码
   */
  const isChangingPassword = changePasswordStatus === 'changing'

  /**
   * 是否有任何操作正在进行
   */
  const isAnyLoading = isLoading || isLoggingIn || isRegistering || isUpdatingProfile || isChangingPassword

  /**
   * 登录是否成功
   */
  const isLoginSuccess = loginStatus === 'success'

  /**
   * 注册是否成功
   */
  const isRegisterSuccess = registerStatus === 'success'

  /**
   * 更新个人信息是否成功
   */
  const isUpdateProfileSuccess = updateProfileStatus === 'success'

  /**
   * 修改密码是否成功
   */
  const isChangePasswordSuccess = changePasswordStatus === 'success'

  /**
   * 用户角色
   */
  const userRole = user?.role || null

  /**
   * 用户权限检查
   */
  const hasPermission = useCallback((permission: string) => {
    if (!isAuthenticated || !user) {
      return false
    }

    // 管理员拥有所有权限
    if (user.role === 'ADMIN') {
      return true
    }

    // 根据角色检查权限
    const rolePermissions: Record<string, string[]> = {
      'RESEARCHER': ['vm:read', 'task:read', 'task:create', 'data:read', 'data:upload', 'model:read'],
      'OPERATOR': ['vm:read', 'vm:operate', 'task:read', 'data:read', 'model:read'],
      'VIEWER': ['vm:read', 'task:read', 'data:read', 'model:read']
    }

    const userPermissions = rolePermissions[user.role] || []
    return userPermissions.includes(permission)
  }, [isAuthenticated, user])

  // ==================== 返回接口 ====================

  return {
    // 状态
    isAuthenticated,
    isLoading: isAnyLoading,
    error,
    user,
    userRole,
    
    // 操作状态
    loginStatus,
    registerStatus,
    updateProfileStatus,
    changePasswordStatus,
    
    // 计算属性
    isLoggingIn,
    isRegistering,
    isUpdatingProfile,
    isChangingPassword,
    isLoginSuccess,
    isRegisterSuccess,
    isUpdateProfileSuccess,
    isChangePasswordSuccess,
    
    // 操作方法
    login,
    logout,
    register,
    fetchUserProfile,
    updateProfile,
    changePassword,
    refreshToken,
    clearError,
    resetStatus,
    checkAuthStatus,
    initializeAuth,
    hasPermission
  }
}

// ==================== 导出默认 Hook ====================
export default useAuth
