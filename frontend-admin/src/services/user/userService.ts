/**
 * 用户服务层 - 企业级规范实现
 * 提供用户认证和个人信息管理相关的业务逻辑处理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { userApi } from '@/api/user'
import type { 
  User, 
  LoginRequest,
  LoginResponse
} from '@/types'
import type { 
  RegisterUserRequest,
  UpdateProfileRequest,
  ChangePasswordRequest,
  RefreshTokenResponse
} from './type'

/**
 * 用户服务类
 */
export class UserService {
  /**
   * 用户注册
   * @param userData 注册数据
   * @returns 注册的用户信息
   */
  async register(userData: RegisterUserRequest): Promise<User> {
    try {
      this.validateRegisterRequest(userData)
      
      const user = await userApi.register({
        username: userData.username,
        email: userData.email,
        password: userData.password,
        confirmPassword: userData.confirmPassword
      })
      
      return this.transformUser(user)
    } catch (error) {
      throw this.handleServiceError(error, '用户注册失败')
    }
  }

  /**
   * 用户登录
   * @param loginData 登录数据
   * @returns 登录响应信息
   */
  async login(loginData: LoginRequest): Promise<LoginResponse> {
    try {
      console.log('🔑 userService.login开始:', loginData.loginIdentifier)
      this.validateLoginRequest(loginData)
      
      console.log('📡 调用userApi.login')
      const response = await userApi.login(loginData)
      console.log('✅ userApi.login成功，响应:', {
        hasToken: !!response.token,
        hasRefreshToken: !!response.refreshToken,
        hasUser: !!response.user,
        tokenPreview: response.token ? '***' + response.token.slice(-10) : 'null'
      })
      
      // 保存认证信息到本地存储
      console.log('💾 保存token到localStorage')
      this.saveAuthTokens(response.token, response.refreshToken)
      
      // 验证保存是否成功
      const savedToken = localStorage.getItem('access_token')
      const savedRefresh = localStorage.getItem('refresh_token')
      console.log('🔍 验证localStorage保存状态:', {
        accessTokenSaved: !!savedToken,
        refreshTokenSaved: !!savedRefresh,
        tokenMatches: savedToken === response.token,
        tokenPreview: savedToken ? '***' + savedToken.slice(-10) : 'null'
      })
      
      const result = {
        token: response.token,
        refreshToken: response.refreshToken,
        expiresIn: response.expiresIn,
        user: this.transformUser(response.user)
      }
      
      console.log('🎯 userService.login完成，返回结果')
      return result
    } catch (error) {
      console.error('❌ userService.login失败:', error)
      throw this.handleServiceError(error, '用户登录失败')
    }
  }

  /**
   * 刷新访问令牌
   * @returns 新的令牌信息
   */
  async refreshToken(): Promise<RefreshTokenResponse> {
    try {
      const response = await userApi.refreshToken()
      
      // 更新本地存储的令牌
      this.saveAuthTokens(response.token, response.refreshToken)
      
      return {
        token: response.token,
        refreshToken: response.refreshToken,
        expiresIn: response.expiresIn
      }
    } catch (error) {
      // 刷新失败，清除本地认证信息
      this.clearAuthTokens()
      throw this.handleServiceError(error, '刷新令牌失败')
    }
  }

  /**
   * 用户登出
   */
  async logout(): Promise<void> {
    try {
      await userApi.logout()
    } catch (error) {
      // 即使API调用失败，也要清除本地认证信息
      console.warn('登出API调用失败，但继续清除本地认证信息:', error)
    } finally {
      // 清除本地认证信息
      this.clearAuthTokens()
    }
  }

  /**
   * 获取当前用户信息
   * @returns 用户信息
   */
  async getProfile(): Promise<User> {
    try {
      const user = await userApi.getProfile()
      return this.transformUser(user)
    } catch (error) {
      throw this.handleServiceError(error, '获取用户信息失败')
    }
  }

  /**
   * 更新用户信息
   * @param userData 更新数据
   * @returns 更新后的用户信息
   */
  async updateProfile(userData: UpdateProfileRequest): Promise<User> {
    try {
      this.validateUpdateProfileRequest(userData)
      
      const user = await userApi.updateProfile(userData)
      return this.transformUser(user)
    } catch (error) {
      throw this.handleServiceError(error, '更新用户信息失败')
    }
  }

  /**
   * 修改密码
   * @param passwordData 密码修改数据
   */
  async changePassword(passwordData: ChangePasswordRequest): Promise<void> {
    try {
      this.validateChangePasswordRequest(passwordData)
      
      await userApi.changePassword({
        oldPassword: passwordData.oldPassword,
        newPassword: passwordData.newPassword,
        confirmPassword: passwordData.confirmPassword
      })
    } catch (error) {
      throw this.handleServiceError(error, '修改密码失败')
    }
  }

  // ==================== 认证状态管理 ====================

  /**
   * 检查是否已登录
   * @returns 是否已登录
   */
  isAuthenticated(): boolean {
    const token = localStorage.getItem('access_token')
    if (!token) {
      return false
    }
    
    // 检查token格式和过期状态
    if (this.isTokenExpired(token)) {
      // token过期或格式错误，清除认证信息
      this.clearAuthTokens()
      return false
    }
    
    return true
  }

  /**
   * 获取当前访问令牌
   * @returns 访问令牌
   */
  getAccessToken(): string | null {
    return localStorage.getItem('access_token')
  }

  /**
   * 获取刷新令牌
   * @returns 刷新令牌
   */
  getRefreshToken(): string | null {
    return localStorage.getItem('refresh_token')
  }

  // ==================== 私有方法 ====================

  /**
   * 验证注册请求
   */
  private validateRegisterRequest(request: RegisterUserRequest): void {
    if (!request.username || request.username.trim().length < 3 || request.username.trim().length > 50) {
      throw new Error('用户名长度必须在3-50字符之间')
    }
    
    if (!request.email || !this.isValidEmail(request.email)) {
      throw new Error('邮箱格式不正确')
    }
    
    if (!request.password || request.password.length < 6 || request.password.length > 20) {
      throw new Error('密码长度必须在6-20字符之间')
    }
    
    if (request.password !== request.confirmPassword) {
      throw new Error('两次输入的密码不一致')
    }
  }

  /**
   * 验证登录请求
   */
  private validateLoginRequest(request: LoginRequest): void {
    if (!request.loginIdentifier || request.loginIdentifier.trim().length === 0) {
      throw new Error('登录标识符不能为空')
    }
    
    if (!request.password || request.password.length === 0) {
      throw new Error('密码不能为空')
    }
  }

  /**
   * 验证更新个人信息请求
   */
  private validateUpdateProfileRequest(request: UpdateProfileRequest): void {
    if (request.username !== undefined) {
      if (!request.username || request.username.trim().length < 3 || request.username.trim().length > 50) {
        throw new Error('用户名长度必须在3-50字符之间')
      }
    }
    
    if (request.email !== undefined) {
      if (!request.email || !this.isValidEmail(request.email)) {
        throw new Error('邮箱格式不正确')
      }
    }
  }

  /**
   * 验证修改密码请求
   */
  private validateChangePasswordRequest(request: ChangePasswordRequest): void {
    if (!request.oldPassword || request.oldPassword.length === 0) {
      throw new Error('旧密码不能为空')
    }
    
    if (!request.newPassword || request.newPassword.length < 6 || request.newPassword.length > 20) {
      throw new Error('新密码长度必须在6-20字符之间')
    }
    
    if (request.newPassword !== request.confirmPassword) {
      throw new Error('两次输入的新密码不一致')
    }
    
    if (request.oldPassword === request.newPassword) {
      throw new Error('新密码不能与旧密码相同')
    }
  }

  /**
   * 验证邮箱格式
   */
  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
  }

  /**
   * 检查令牌是否过期
   */
  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      const currentTime = Math.floor(Date.now() / 1000)
      return payload.exp < currentTime
    } catch (error) {
      // 如果无法解析令牌，认为已过期
      return true
    }
  }

  /**
   * 保存认证令牌到本地存储
   */
  private saveAuthTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem('access_token', accessToken)
    localStorage.setItem('refresh_token', refreshToken)
  }

  /**
   * 清除本地存储的认证信息
   */
  private clearAuthTokens(): void {
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
  }

  /**
   * 转换用户数据
   */
  private transformUser(user: any): User {
    return {
      userId: user.userId,
      username: user.username,
      email: user.email,
      role: user.role,
      status: user.status,
      lastLoginTime: user.lastLoginTime,
      lastLoginIp: user.lastLoginIp,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt
    }
  }

  /**
   * 统一错误处理
   */
  private handleServiceError(error: any, message: string): Error {
    console.error(`[UserService] ${message}:`, error)
    
    if (error.response?.data?.message) {
      return new Error(`${message}: ${error.response.data.message}`)
    }
    
    if (error.message) {
      return new Error(`${message}: ${error.message}`)
    }
    
    return new Error(message)
  }
}

// 导出服务实例
export const userService = new UserService()

// 导出默认实例
export default userService
