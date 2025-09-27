/**
 * 管理员服务层 - 企业级规范实现
 * 提供管理员用户管理相关的业务逻辑处理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { admin } from '@/api/admin'
import type { 
  User, 
  PaginatedResponse, 
  PaginationParams,
  ApiResponse 
} from '@/types'
import type { 
  CreateUserRequest,
  UpdateUserRequest,
  UserListParams,
  LockUserRequest,
  ResetPasswordRequest,
  LockUserResponse,
  UnlockUserResponse,
  UserStatistics
} from './type'

/**
 * 管理员用户管理服务类
 */
export class AdminUserService {
  /**
   * 获取用户列表
   * @param params 查询参数
   * @returns 分页用户列表
   */
  async getUserList(params: UserListParams = {}): Promise<PaginatedResponse<User>> {
    try {
      const result = await admin.getUserList(params)
      
      // 数据验证和转换
      this.validatePaginatedResponse(result)
      
      const response: any = {
        total: result.total,
        page: result.page,
        size: result.size,
        pages: result.pages
      }
      
      // 只有当原始响应中有对应字段时才添加
      if (result.records !== undefined) {
        response.records = result.records.map(user => this.transformUser(user))
      }
      
      if (result.list !== undefined) {
        response.list = result.list.map(user => this.transformUser(user))
      }
      
      return response as PaginatedResponse<User>
    } catch (error) {
      throw this.handleServiceError(error, '获取用户列表失败')
    }
  }

  /**
   * 获取用户详情
   * @param userId 用户ID
   * @returns 用户详细信息
   */
  async getUserDetail(userId: string): Promise<User> {
    try {
      this.validateUserId(userId)
      
      const user = await admin.getUserDetail(userId)
      return this.transformUser(user)
    } catch (error) {
      throw this.handleServiceError(error, `获取用户详情失败 (ID: ${userId})`)
    }
  }

  /**
   * 创建用户
   * @param userData 用户创建数据
   * @returns 创建的用户信息
   */
  async createUser(userData: CreateUserRequest): Promise<User> {
    try {
      this.validateCreateUserRequest(userData)
      
      const createRequest = {
        username: userData.username,
        email: userData.email,
        password: userData.password,
        role: userData.role,
        status: userData.status || 'ACTIVE'
      }
      
      const user = await admin.createUser(createRequest)
      
      return this.transformUser(user)
    } catch (error) {
      throw this.handleServiceError(error, '创建用户失败')
    }
  }

  /**
   * 更新用户信息
   * @param userId 用户ID
   * @param userData 更新数据
   * @returns 更新后的用户信息
   */
  async updateUser(userId: string, userData: UpdateUserRequest): Promise<User> {
    try {
      this.validateUserId(userId)
      this.validateUpdateUserRequest(userData)
      
      const user = await admin.updateUser(userId, userData)
      return this.transformUser(user)
    } catch (error) {
      throw this.handleServiceError(error, `更新用户失败 (ID: ${userId})`)
    }
  }

  /**
   * 删除用户
   * @param userId 用户ID
   */
  async deleteUser(userId: string): Promise<void> {
    try {
      this.validateUserId(userId)
      
      await admin.deleteUser(userId)
    } catch (error) {
      throw this.handleServiceError(error, `删除用户失败 (ID: ${userId})`)
    }
  }

  /**
   * 锁定用户
   * @param userId 用户ID
   * @param request 锁定请求参数
   * @returns 锁定结果
   */
  async lockUser(userId: string, request: LockUserRequest = {}): Promise<LockUserResponse> {
    try {
      this.validateUserId(userId)
      
      const result = await admin.lockUser(userId, request.duration)
      
      return {
        userId: result.userId,
        lockedUntil: result.lockedUntil,
        duration: request.duration
      }
    } catch (error) {
      throw this.handleServiceError(error, `锁定用户失败 (ID: ${userId})`)
    }
  }

  /**
   * 解锁用户
   * @param userId 用户ID
   * @returns 解锁结果
   */
  async unlockUser(userId: string): Promise<UnlockUserResponse> {
    try {
      this.validateUserId(userId)
      
      const result = await admin.unlockUser(userId)
      
      return {
        userId: result.userId,
        status: result.status as User['status']
      }
    } catch (error) {
      throw this.handleServiceError(error, `解锁用户失败 (ID: ${userId})`)
    }
  }

  /**
   * 重置用户密码
   * @param userId 用户ID
   * @param request 重置密码请求
   */
  async resetUserPassword(userId: string, request: ResetPasswordRequest): Promise<void> {
    try {
      this.validateUserId(userId)
      this.validateResetPasswordRequest(request)
      
      await admin.resetUserPassword(userId, request.newPassword)
    } catch (error) {
      throw this.handleServiceError(error, `重置用户密码失败 (ID: ${userId})`)
    }
  }

  /**
   * 获取用户统计信息
   * @returns 用户统计数据
   */
  async getUserStatistics(): Promise<UserStatistics> {
    try {
      const statistics = await admin.getUserStatistics()
      return this.transformUserStatistics(statistics)
    } catch (error) {
      throw this.handleServiceError(error, '获取用户统计信息失败')
    }
  }


  // ==================== 私有方法 ====================

  /**
   * 验证用户ID
   */
  private validateUserId(userId: string): void {
    if (!userId || typeof userId !== 'string' || userId.trim().length === 0) {
      throw new Error('用户ID不能为空')
    }
  }


  /**
   * 验证创建用户请求
   */
  private validateCreateUserRequest(request: CreateUserRequest): void {
    if (!request.username || request.username.trim().length === 0) {
      throw new Error('用户名不能为空')
    }
    if (!request.email || request.email.trim().length === 0) {
      throw new Error('邮箱不能为空')
    }
    if (!request.password || request.password.length < 6) {
      throw new Error('密码长度至少6位')
    }
    if (!request.role || !['ADMIN', 'RESEARCHER', 'OPERATOR', 'VIEWER'].includes(request.role)) {
      throw new Error('角色类型无效')
    }
    if (request.status && !['ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED'].includes(request.status)) {
      throw new Error('用户状态无效')
    }
  }

  /**
   * 验证更新用户请求
   */
  private validateUpdateUserRequest(request: UpdateUserRequest): void {
    if (request.username !== undefined && (!request.username || request.username.trim().length === 0)) {
      throw new Error('用户名不能为空')
    }
    if (request.email !== undefined && (!request.email || request.email.trim().length === 0)) {
      throw new Error('邮箱不能为空')
    }
    if (request.role !== undefined && !['ADMIN', 'RESEARCHER', 'OPERATOR', 'VIEWER'].includes(request.role)) {
      throw new Error('角色类型无效')
    }
    if (request.status !== undefined && !['ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED'].includes(request.status)) {
      throw new Error('用户状态无效')
    }
    if (request.password !== undefined && request.password.length < 6) {
      throw new Error('密码长度至少6位')
    }
  }

  /**
   * 验证重置密码请求
   */
  private validateResetPasswordRequest(request: ResetPasswordRequest): void {
    if (!request.newPassword || request.newPassword.length < 6) {
      throw new Error('新密码长度至少6位')
    }
  }


  /**
   * 验证分页响应数据
   */
  private validatePaginatedResponse<T>(response: PaginatedResponse<T>): void {
    if (typeof response.total !== 'number' || response.total < 0) {
      throw new Error('分页数据格式错误：total字段无效')
    }
    if (typeof response.page !== 'number' || response.page < 1) {
      throw new Error('分页数据格式错误：page字段无效')
    }
    if (typeof response.size !== 'number' || response.size < 1) {
      throw new Error('分页数据格式错误：size字段无效')
    }
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
   * 转换用户统计数据
   */
  private transformUserStatistics(statistics: any): UserStatistics {
    return {
      totalUsers: statistics.totalUsers,
      activeUsers: statistics.activeUsers,
      lockedUsers: statistics.lockedUsers,
      roleDistribution: statistics.roleDistribution,
      statusDistribution: statistics.statusDistribution,
      newUsersThisMonth: statistics.newUsersThisMonth,
      activeUsersThisMonth: statistics.activeUsersThisMonth
    }
  }


  /**
   * 统一错误处理
   */
  private handleServiceError(error: any, message: string): Error {
    console.error(`[AdminUserService] ${message}:`, error)
    
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
export const adminUserService = new AdminUserService()

// 导出默认实例
export default adminUserService
