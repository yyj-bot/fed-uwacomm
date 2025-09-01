/**
 * 认证服务
 * 职责：用户认证、权限管理、Token管理、状态管理
 */

import { userApi } from '@/api/user'
import { TokenManager } from './token-manager'
import type { 
  AuthState, 
  AuthEvent, 
  AuthEventListener, 
  TokenInfo,
  PermissionCheckResult,
  RolePermissions 
} from './types'
import type { LoginRequest, User } from '@/types'

export class AuthService {
  private static instance: AuthService
  private state: AuthState = {
    user: null,
    isAuthenticated: false,
    isLoading: false,
    error: null
  }
  
  private refreshTimer: NodeJS.Timeout | null = null
  private eventListeners: Map<string, Set<AuthEventListener>> = new Map()

  // 角色权限配置
  private readonly rolePermissions: RolePermissions = {
    ADMIN: {
      permissions: ['*'], // 管理员拥有所有权限
      description: '系统管理员'
    },
    RESEARCHER: {
      permissions: [
        'vm:read', 'vm:control',
        'task:read', 'task:create', 'task:update',
        'model:read', 'model:create',
        'data:read', 'data:upload',
        'log:read'
      ],
      description: '研究员'
    },
    OPERATOR: {
      permissions: [
        'vm:read',
        'task:read',
        'model:read',
        'data:read',
        'log:read'
      ],
      description: '操作员'
    },
    VIEWER: {
      permissions: [
        'vm:read',
        'task:read',
        'model:read',
        'log:read'
      ],
      description: '查看者'
    }
  }

  private constructor() {
    this.initializeAuth()
  }

  /**
   * 获取单例实例
   */
  public static getInstance(): AuthService {
    if (!AuthService.instance) {
      AuthService.instance = new AuthService()
    }
    return AuthService.instance
  }

  /**
   * 初始化认证状态
   */
  private initializeAuth(): void {
    this.state.isLoading = true

    try {
      const user = TokenManager.getUserInfo()
      const isTokenValid = TokenManager.isTokenValid()

      if (user && isTokenValid) {
        this.state.user = user
        this.state.isAuthenticated = true
        this.setupTokenRefresh()
        
        this.emitEvent({
          type: 'LOGIN',
          payload: { 
            user,
            timestamp: new Date().toISOString()
          }
        })
      } else if (TokenManager.getAccessToken()) {
        // Token存在但无效，尝试刷新
        this.refreshToken()
      }
    } catch (error) {
      console.error('认证初始化失败:', error)
      this.state.error = '认证初始化失败'
      this.clearAuthState()
    } finally {
      this.state.isLoading = false
    }
  }

  /**
   * 用户登录
   */
  async login(loginData: LoginRequest): Promise<User> {
    this.state.isLoading = true
    this.state.error = null

    try {
      const response = await userApi.login(loginData)
      
      // 构建Token信息
      const tokenInfo: TokenInfo = {
        accessToken: response.token,
        refreshToken: response.refreshToken,
        expiresAt: Date.now() + response.expiresIn * 1000,
        expiresIn: response.expiresIn
      }

      // 存储认证信息
      TokenManager.setTokenInfo(tokenInfo)
      TokenManager.setUserInfo(response.user)

      // 更新状态
      this.state.user = response.user
      this.state.isAuthenticated = true
      this.state.isLoading = false

      // 设置Token自动刷新
      this.setupTokenRefresh()

      // 触发登录事件
      this.emitEvent({
        type: 'LOGIN',
        payload: {
          user: response.user,
          timestamp: new Date().toISOString()
        }
      })

      return response.user
    } catch (error) {
      this.state.isLoading = false
      this.state.error = error instanceof Error ? error.message : '登录失败'
      
      this.emitEvent({
        type: 'AUTH_ERROR',
        payload: {
          error: this.state.error,
          timestamp: new Date().toISOString()
        }
      })
      
      throw error
    }
  }

  /**
   * 用户登出
   */
  async logout(): Promise<void> {
    try {
      await userApi.logout()
    } catch (error) {
      console.error('登出请求失败:', error)
    } finally {
      this.clearAuthState()
      
      this.emitEvent({
        type: 'LOGOUT',
        payload: {
          timestamp: new Date().toISOString()
        }
      })

      // 重定向到登录页
      window.location.href = '/login'
    }
  }

  /**
   * 刷新Token
   */
  private async refreshToken(): Promise<void> {
    try {
      const response = await userApi.refreshToken()
      
      const tokenInfo: TokenInfo = {
        accessToken: response.token,
        refreshToken: response.refreshToken,
        expiresAt: Date.now() + response.expiresIn * 1000,
        expiresIn: response.expiresIn
      }

      TokenManager.setTokenInfo(tokenInfo)
      this.setupTokenRefresh()

      this.emitEvent({
        type: 'TOKEN_REFRESH',
        payload: {
          timestamp: new Date().toISOString()
        }
      })

      console.log('Token刷新成功')
    } catch (error) {
      console.error('Token刷新失败:', error)
      this.clearAuthState()
      
      this.emitEvent({
        type: 'AUTH_ERROR',
        payload: {
          error: 'Token刷新失败',
          timestamp: new Date().toISOString()
        }
      })

      window.location.href = '/login'
    }
  }

  /**
   * 设置Token自动刷新
   */
  private setupTokenRefresh(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer)
    }

    const expiresAt = TokenManager.getTokenExpiresAt()
    if (!expiresAt) return

    const now = Date.now()
    const timeUntilRefresh = expiresAt - now - 5 * 60 * 1000 // 提前5分钟刷新

    if (timeUntilRefresh > 0) {
      this.refreshTimer = setTimeout(() => {
        this.refreshToken()
      }, timeUntilRefresh)
    } else {
      // 立即刷新
      this.refreshToken()
    }
  }

  /**
   * 清除认证状态
   */
  private clearAuthState(): void {
    TokenManager.clearTokens()
    TokenManager.clearUserInfo()

    this.state.user = null
    this.state.isAuthenticated = false
    this.state.error = null

    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer)
      this.refreshTimer = null
    }
  }

  /**
   * 获取当前认证状态
   */
  getState(): Readonly<AuthState> {
    return { ...this.state }
  }

  /**
   * 获取当前用户
   */
  getCurrentUser(): User | null {
    return this.state.user
  }

  /**
   * 检查是否已登录
   */
  isAuthenticated(): boolean {
    return this.state.isAuthenticated && TokenManager.isTokenValid()
  }

  /**
   * 获取访问Token
   */
  getAccessToken(): string | null {
    return TokenManager.getAccessToken()
  }

  /**
   * 检查用户权限
   */
  hasPermission(permission: string): PermissionCheckResult {
    if (!this.state.user) {
      return {
        hasPermission: false,
        reason: '用户未登录'
      }
    }

    const userRole = this.state.user.role
    const roleConfig = this.rolePermissions[userRole]

    if (!roleConfig) {
      return {
        hasPermission: false,
        reason: '未知角色'
      }
    }

    // 管理员拥有所有权限
    if (roleConfig.permissions.includes('*')) {
      return { hasPermission: true }
    }

    // 检查具体权限
    const hasPermission = roleConfig.permissions.includes(permission)
    
    return {
      hasPermission,
      reason: hasPermission ? undefined : `角色 ${userRole} 没有 ${permission} 权限`
    }
  }

  /**
   * 检查用户角色
   */
  hasRole(role: string): boolean {
    return this.state.user?.role === role
  }

  /**
   * 检查是否为管理员
   */
  isAdmin(): boolean {
    return this.hasRole('ADMIN')
  }

  /**
   * 检查是否为研究员（包含管理员）
   */
  isResearcher(): boolean {
    return this.hasRole('RESEARCHER') || this.isAdmin()
  }

  /**
   * 更新用户信息
   */
  updateUser(user: User): void {
    this.state.user = user
    TokenManager.setUserInfo(user)
  }

  /**
   * 注册事件监听器
   */
  addEventListener(eventType: string, listener: AuthEventListener): () => void {
    if (!this.eventListeners.has(eventType)) {
      this.eventListeners.set(eventType, new Set())
    }
    
    this.eventListeners.get(eventType)!.add(listener)

    // 返回取消监听的函数
    return () => {
      this.eventListeners.get(eventType)?.delete(listener)
    }
  }

  /**
   * 移除事件监听器
   */
  removeEventListener(eventType: string, listener: AuthEventListener): void {
    this.eventListeners.get(eventType)?.delete(listener)
  }

  /**
   * 触发事件
   */
  private emitEvent(event: AuthEvent): void {
    const listeners = this.eventListeners.get(event.type)
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(event)
        } catch (error) {
          console.error('认证事件监听器执行失败:', error)
        }
      })
    }
  }

  /**
   * 销毁服务
   */
  destroy(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer)
      this.refreshTimer = null
    }
    
    this.eventListeners.clear()
    this.clearAuthState()
  }
}
