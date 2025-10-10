/**
 * 管理员虚拟机管理服务层 - 企业级规范实现
 * 提供管理员专用的虚拟机分配、管理和监控功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { admin } from '@/api/admin'
import type { PaginatedResponse } from '@/types'
import type {
  VmAssignment,
  VmAssignmentDetail,
  VirtualMachine,
  UserVm,
  VmAssignmentOverview,
  VmControlResponse,
  VmPermission,
  VmStatus,
  VmControlAction,
  AssignVmRequest,
  UnassignVmResponse,
  VmAssignmentInfoResponse,
  UpdateVmPermissionsResponse,
  UserVmListParams,
  BatchAssignVmsRequest,
  BatchAssignVmsResponse,
  BatchRemoveVmsRequest,
  BatchRemoveVmsResponse,
  AdminVmListParams,
  UnassignedVmListParams,
  UnassignedVmListResponse,
  ForceControlVmRequest
} from './type'

/**
 * 管理员虚拟机管理服务类
 */
export class AdminVmService {
  
  // ==================== 虚拟机分配管理 ====================

  /**
   * 分配虚拟机给用户
   * @param vmId 虚拟机ID
   * @param userId 用户ID
   * @param params 分配参数
   * @returns 分配结果
   */
  async assignVmToUser(
    vmId: string,
    userId: string,
    params: {
      permissions?: VmPermission[]
      notes?: string
    } = {}
  ): Promise<VmAssignment> {
    try {
      this.validateVmId(vmId)
      this.validateUserId(userId)

      const result = await admin.assignVmToUser(vmId, userId, params)
      return this.transformVmAssignment(result)
    } catch (error) {
      throw this.handleServiceError(
        error,
        `分配虚拟机失败 (VM: ${vmId}, User: ${userId})`
      )
    }
  }

  /**
   * 取消虚拟机分配
   * @param vmId 虚拟机ID
   * @param userId 用户ID
   * @returns 取消分配结果
   */
  async unassignVmFromUser(
    vmId: string,
    userId: string
  ): Promise<UnassignVmResponse> {
    try {
      this.validateVmId(vmId)
      this.validateUserId(userId)

      const result = await admin.unassignVmFromUser(vmId, userId)
      return result
    } catch (error) {
      throw this.handleServiceError(
        error,
        `取消虚拟机分配失败 (VM: ${vmId}, User: ${userId})`
      )
    }
  }

  /**
   * 查看虚拟机分配情况
   * @param vmId 虚拟机ID
   * @returns 虚拟机分配信息
   */
  async getVmAssignments(vmId: string): Promise<VmAssignmentInfoResponse> {
    try {
      this.validateVmId(vmId)

      const result = await admin.getVmAssignments(vmId)
      return {
        vmId: result.vmId,
        vmName: result.vmName,
        assignments: result.assignments.map(a => this.transformVmAssignmentDetail(a)),
        totalAssignments: result.totalAssignments
      }
    } catch (error) {
      throw this.handleServiceError(error, `获取虚拟机分配情况失败 (VM: ${vmId})`)
    }
  }

  /**
   * 修改用户虚拟机权限
   * @param vmId 虚拟机ID
   * @param userId 用户ID
   * @param permissions 新权限列表
   * @returns 权限更新结果
   */
  async updateUserVmPermissions(
    vmId: string,
    userId: string,
    permissions: VmPermission[]
  ): Promise<UpdateVmPermissionsResponse> {
    try {
      this.validateVmId(vmId)
      this.validateUserId(userId)
      this.validatePermissions(permissions)

      const result = await admin.updateUserVmPermissions(vmId, userId, permissions)
      return result
    } catch (error) {
      throw this.handleServiceError(
        error,
        `更新虚拟机权限失败 (VM: ${vmId}, User: ${userId})`
      )
    }
  }

  // ==================== 用户虚拟机管理 ====================

  /**
   * 查看用户的虚拟机列表
   * @param userId 用户ID
   * @param params 查询参数
   * @returns 用户的虚拟机列表
   */
  async getUserVmList(
    userId: string,
    params: Omit<UserVmListParams, 'userId'> = {}
  ): Promise<PaginatedResponse<UserVm> & { userId: string; username: string }> {
    try {
      this.validateUserId(userId)

      const result = await admin.getUserVmList(userId, params)
      
      return {
        ...result,
        list: result.list?.map(vm => this.transformUserVm(vm)),
        records: result.records?.map(vm => this.transformUserVm(vm))
      } as any
    } catch (error) {
      throw this.handleServiceError(error, `获取用户虚拟机列表失败 (User: ${userId})`)
    }
  }

  /**
   * 批量分配虚拟机给用户
   * @param userId 用户ID
   * @param params 批量分配参数
   * @returns 批量分配结果
   */
  async batchAssignVmsToUser(
    userId: string,
    params: Omit<BatchAssignVmsRequest, 'userId'>
  ): Promise<BatchAssignVmsResponse> {
    try {
      this.validateUserId(userId)
      this.validateVmIds(params.vmIds)

      const result = await admin.batchAssignVmsToUser(userId, params)
      return result
    } catch (error) {
      throw this.handleServiceError(error, `批量分配虚拟机失败 (User: ${userId})`)
    }
  }

  /**
   * 批量移除用户虚拟机权限
   * @param userId 用户ID
   * @param params 批量移除参数
   * @returns 批量移除结果
   */
  async batchRemoveUserVms(
    userId: string,
    params: Omit<BatchRemoveVmsRequest, 'userId'>
  ): Promise<BatchRemoveVmsResponse> {
    try {
      this.validateUserId(userId)
      this.validateVmIds(params.vmIds)

      const result = await admin.batchRemoveUserVms(userId, params)
      return result
    } catch (error) {
      throw this.handleServiceError(error, `批量移除虚拟机失败 (User: ${userId})`)
    }
  }

  // ==================== 管理员虚拟机管理视图 ====================

  /**
   * 管理员查看所有虚拟机
   * @param params 查询参数
   * @returns 虚拟机列表
   */
  async getAdminVmList(params: AdminVmListParams = {}): Promise<PaginatedResponse<VirtualMachine>> {
    try {
      const result = await admin.getAdminVmList(params)
      
      return {
        ...result,
        list: result.list?.map(vm => this.transformVirtualMachine(vm)),
        records: result.records?.map(vm => this.transformVirtualMachine(vm))
      } as any
    } catch (error) {
      throw this.handleServiceError(error, '获取管理员虚拟机列表失败')
    }
  }

  /**
   * 查看未分配虚拟机列表
   * @param params 查询参数
   * @returns 未分配虚拟机列表
   */
  async getUnassignedVmList(
    params: UnassignedVmListParams = {}
  ): Promise<UnassignedVmListResponse> {
    try {
      const result = await admin.getUnassignedVmList(params)
      
      return {
        unassignedVms: result.unassignedVms.map(vm => this.transformVirtualMachine(vm)),
        total: result.total
      }
    } catch (error) {
      throw this.handleServiceError(error, '获取未分配虚拟机列表失败')
    }
  }

  /**
   * 获取虚拟机分配概况
   * @returns 虚拟机分配概况统计
   */
  async getVmAssignmentOverview(): Promise<VmAssignmentOverview> {
    try {
      const result = await admin.getVmAssignmentOverview()
      return this.transformVmAssignmentOverview(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取虚拟机分配概况失败')
    }
  }

  /**
   * 管理员强制控制虚拟机
   * @param vmId 虚拟机ID
   * @param params 控制参数
   * @returns 控制响应
   */
  async forceControlVm(
    vmId: string,
    params: Omit<ForceControlVmRequest, 'vmId'>
  ): Promise<VmControlResponse> {
    try {
      this.validateVmId(vmId)
      this.validateControlAction(params.action)
      
      if (!params.reason || params.reason.trim().length === 0) {
        throw new Error('强制控制原因不能为空')
      }

      const result = await admin.forceControlVm(vmId, params)
      return result
    } catch (error) {
      throw this.handleServiceError(error, `强制控制虚拟机失败 (VM: ${vmId})`)
    }
  }

  // ==================== 私有方法 ====================

  /**
   * 验证虚拟机ID
   */
  private validateVmId(vmId: string): void {
    if (!vmId || typeof vmId !== 'string' || vmId.trim().length === 0) {
      throw new Error('虚拟机ID不能为空')
    }
  }

  /**
   * 验证用户ID
   */
  private validateUserId(userId: string): void {
    if (!userId || typeof userId !== 'string' || userId.trim().length === 0) {
      throw new Error('用户ID不能为空')
    }
  }

  /**
   * 验证虚拟机ID列表
   */
  private validateVmIds(vmIds: string[]): void {
    if (!Array.isArray(vmIds) || vmIds.length === 0) {
      throw new Error('虚拟机ID列表不能为空')
    }
    
    vmIds.forEach((vmId, index) => {
      if (!vmId || typeof vmId !== 'string' || vmId.trim().length === 0) {
        throw new Error(`虚拟机ID列表中第${index + 1}项无效`)
      }
    })
  }

  /**
   * 验证权限列表
   */
  private validatePermissions(permissions: VmPermission[]): void {
    if (!Array.isArray(permissions) || permissions.length === 0) {
      throw new Error('权限列表不能为空')
    }

    const validPermissions: VmPermission[] = ['READ', 'WRITE', 'EXECUTE', 'ADMIN']
    permissions.forEach((permission, index) => {
      if (!validPermissions.includes(permission)) {
        throw new Error(`权限列表中第${index + 1}项无效: ${permission}`)
      }
    })
  }

  /**
   * 验证控制操作类型
   */
  private validateControlAction(action: VmControlAction): void {
    const validActions: VmControlAction[] = ['START', 'STOP', 'RESTART', 'FORCE_STOP']
    if (!validActions.includes(action)) {
      throw new Error(`无效的控制操作类型: ${action}`)
    }
  }

  /**
   * 转换虚拟机分配信息
   */
  private transformVmAssignment(assignment: any): VmAssignment {
    return {
      vmId: assignment.vmId,
      userId: assignment.userId,
      permissions: assignment.permissions,
      assignedAt: assignment.assignedAt,
      assignedBy: assignment.assignedBy,
      notes: assignment.notes
    }
  }

  /**
   * 转换虚拟机分配详情
   */
  private transformVmAssignmentDetail(detail: any): VmAssignmentDetail {
    return {
      userId: detail.userId,
      username: detail.username,
      email: detail.email,
      permissions: detail.permissions,
      assignedAt: detail.assignedAt,
      assignedBy: detail.assignedBy
    }
  }

  /**
   * 转换虚拟机信息
   */
  private transformVirtualMachine(vm: any): VirtualMachine {
    return {
      vmId: vm.vmId,
      name: vm.name,
      ipAddress: vm.ipAddress,
      status: vm.status,
      connectionStatus: vm.connectionStatus,
      isAssigned: vm.isAssigned,
      assignedUserCount: vm.assignedUserCount,
      createdAt: vm.createdAt,
      lastHeartbeat: vm.lastHeartbeat
    }
  }

  /**
   * 转换用户虚拟机信息
   */
  private transformUserVm(vm: any): UserVm {
    return {
      vmId: vm.vmId,
      vmName: vm.vmName,
      ipAddress: vm.ipAddress,
      status: vm.status,
      permissions: vm.permissions,
      assignedAt: vm.assignedAt
    }
  }

  /**
   * 转换虚拟机分配概况
   */
  private transformVmAssignmentOverview(overview: any): VmAssignmentOverview {
    return {
      summary: {
        totalVms: overview.summary.totalVms,
        assignedVms: overview.summary.assignedVms,
        unassignedVms: overview.summary.unassignedVms,
        totalUsers: overview.summary.totalUsers,
        usersWithVms: overview.summary.usersWithVms
      },
      statusDistribution: overview.statusDistribution,
      topAssignedVms: overview.topAssignedVms,
      recentAssignments: overview.recentAssignments
    }
  }

  /**
   * 统一错误处理
   */
  private handleServiceError(error: any, message: string): Error {
    console.error(`[AdminVmService] ${message}:`, error)

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
export const adminVmService = new AdminVmService()

// 导出默认实例
export default adminVmService

