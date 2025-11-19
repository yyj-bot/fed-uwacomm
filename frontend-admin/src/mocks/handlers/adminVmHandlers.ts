/**
 * 管理员水下机器人管理 Mock 处理器
 * 基于 admin-vm-api-reference.md 文档
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { http, HttpResponse } from 'msw'
import {
  adminVmApiMock,
  mockVmAssignmentOverview,
  mockVmAssignments,
  mockUnassignedVms,
  mockAllAdminVms,
  mockUserVms
} from '../data/adminVmMockData'
import { vmStatusMap, deletedVmIds } from './vmState'

export const adminVmHandlers = [
  // 3.1 管理员查看所有水下机器人 - GET /api/admin/vm/list
  http.get('http://localhost:5173/api/admin/vm/list', async ({ request }) => {
    console.log('[MSW] 匹配: GET /api/admin/vm/list')
    
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '20')
    const status = url.searchParams.get('status')
    const assigned = url.searchParams.get('assigned')
    const keyword = url.searchParams.get('keyword')
    
    // 使用 adminVmMockData 中的管理员VM列表，并过滤已删除的VM，更新状态
    let vmList = mockAllAdminVms
      .filter(vm => !deletedVmIds.has(vm.vmId)) // 过滤已删除的VM
      .map(vm => {
        // 更新运行时状态
        const currentStatus = vmStatusMap.get(vm.vmId) || vm.status
        const connectionStatus = (currentStatus === 'RUNNING' || currentStatus === 'STARTING') 
          ? 'CONNECTED' 
          : 'DISCONNECTED'
        
        return {
          ...vm,
          status: currentStatus,
          connectionStatus
        }
      })
    
    // 应用过滤器
    if (status) {
      vmList = vmList.filter(vm => vm.status === status)
    }
    
    if (assigned !== null) {
      const isAssigned = assigned === 'true'
      vmList = vmList.filter(vm => vm.isAssigned === isAssigned)
    }
    
    if (keyword) {
      const lowerKeyword = keyword.toLowerCase()
      vmList = vmList.filter(vm => 
        vm.name.toLowerCase().includes(lowerKeyword) ||
        vm.ipAddress.toLowerCase().includes(lowerKeyword)
      )
    }
    
    // 分页
    const total = vmList.length
    const startIndex = (page - 1) * size
    const endIndex = startIndex + size
    const paginatedList = vmList.slice(startIndex, endIndex)
    
    return HttpResponse.json({
      code: 200,
      message: '获取成功',
      data: {
        total,
        page,
        size,
        list: paginatedList
      }
    })
  }),

  // 3.2 查看未分配水下机器人列表 - GET /api/admin/vm/unassigned
  http.get('http://localhost:5173/api/admin/vm/unassigned', async ({ request }) => {
    console.log('[MSW] 匹配: GET /api/admin/vm/unassigned')
    
    const url = new URL(request.url)
    const status = url.searchParams.get('status')
    
    // 使用 adminVmMockData 中的未分配VM列表，并过滤已删除的VM，更新状态
    let unassignedList = mockUnassignedVms
      .filter(vm => !deletedVmIds.has(vm.vmId)) // 过滤已删除的VM
      .map(vm => {
        // 更新运行时状态
        const currentStatus = vmStatusMap.get(vm.vmId) || vm.status
        const connectionStatus = (currentStatus === 'RUNNING' || currentStatus === 'STARTING') 
          ? 'CONNECTED' 
          : 'DISCONNECTED'
        
        return {
          ...vm,
          status: currentStatus,
          connectionStatus
        }
      })
    
    // 状态过滤
    if (status) {
      unassignedList = unassignedList.filter(vm => vm.status === status)
    }
    
    return HttpResponse.json({
      code: 200,
      message: '获取成功',
      data: {
        unassignedVms: unassignedList,
        total: unassignedList.length
      }
    })
  }),

  // 3.3 获取VM分配概况 - GET /api/admin/vm/assignments/overview
  http.get('http://localhost:5173/api/admin/vm/assignments/overview', async () => {
    console.log('[MSW] 匹配: GET /api/admin/vm/assignments/overview')
    
    // 使用 adminVmMockData 中的管理员VM列表计算状态分布
    const statusDistribution = {
      RUNNING: 0,
      STOPPED: 0,
      ERROR: 0,
      STARTING: 0,
      STOPPING: 0
    }
    
    mockAllAdminVms.forEach(vm => {
      if (statusDistribution.hasOwnProperty(vm.status)) {
        statusDistribution[vm.status as keyof typeof statusDistribution]++
      }
    })
    
    // 统计分配情况 - 只统计真实存在的VM
    const assignedVmIds = new Set<string>()
    const usersWithVms = new Set<string>()
    const realVmIds = new Set(mockAllAdminVms.map(vm => vm.vmId))
    
    Object.values(mockVmAssignments).forEach((vmAssignment: any) => {
      // 只统计真实存在的VM且有分配记录的
      if (realVmIds.has(vmAssignment.vmId) && 
          vmAssignment.assignments && 
          vmAssignment.assignments.length > 0) {
        assignedVmIds.add(vmAssignment.vmId)
        vmAssignment.assignments.forEach((a: any) => {
          usersWithVms.add(a.userId)
        })
      }
    })
    
    // 获取用户总数
    const { mockUsers } = await import('../data/userMockData')
    
    // 动态计算热门水下机器人（按分配用户数排序）
    const topAssignedVms = Object.values(mockVmAssignments)
      .filter((vm: any) => realVmIds.has(vm.vmId) && vm.assignments.length > 0)
      .map((vm: any) => ({
        vmId: vm.vmId,
        vmName: vm.vmName,
        assignedUserCount: vm.assignments.length
      }))
      .sort((a, b) => b.assignedUserCount - a.assignedUserCount)
      .slice(0, 5)
    
    // 动态获取最近分配记录
    const recentAssignments: any[] = []
    Object.values(mockVmAssignments).forEach((vm: any) => {
      if (realVmIds.has(vm.vmId)) {
        vm.assignments.forEach((assignment: any) => {
          recentAssignments.push({
            vmId: vm.vmId,
            vmName: vm.vmName,
            userId: assignment.userId,
            username: assignment.username,
            assignedAt: assignment.assignedAt
          })
        })
      }
    })
    // 按时间排序，最近的在前
    recentAssignments.sort((a, b) => 
      new Date(b.assignedAt).getTime() - new Date(a.assignedAt).getTime()
    )
    
    // 动态生成响应数据
    const overview = {
      summary: {
        totalVms: mockAllAdminVms.length,
        assignedVms: assignedVmIds.size,
        unassignedVms: mockAllAdminVms.length - assignedVmIds.size,
        totalUsers: mockUsers.length,
        usersWithVms: usersWithVms.size
      },
      statusDistribution,
      topAssignedVms,
      recentAssignments: recentAssignments.slice(0, 10)
    }
    
    console.log('[MSW] 动态计算的VM分配概况:', {
      statusDistribution,
      assignedVms: assignedVmIds.size,
      topAssignedVms,
      recentCount: recentAssignments.length
    })
    
    return HttpResponse.json({
      code: 200,
      message: '获取成功',
      data: overview
    })
  }),

  // 获取VM分配情况
  http.get('http://localhost:5173/api/admin/vm/:vmId/assignments', ({ params }) => {
    const { vmId } = params
    console.log(`[MSW] 匹配: GET /api/admin/vm/${vmId}/assignments`)
    
    const assignments = mockVmAssignments[vmId as string] || {
      vmId,
      vmName: `水下机器人-${vmId}`,
      assignments: [],
      totalAssignments: 0
    }
    
    return HttpResponse.json({
      code: 200,
      message: '获取成功',
      data: assignments
    })
  }),

  // 分配VM给用户
  http.post('http://localhost:5173/api/admin/vm/:vmId/assign/:userId', async ({ params, request }) => {
    const { vmId, userId } = params
    const body = await request.json() as any
    
    console.log(`[MSW] 匹配: POST /api/admin/vm/${vmId}/assign/${userId}`, body)
    
    // 动态更新 mockVmAssignments 数据
    if (!mockVmAssignments[vmId as string]) {
      mockVmAssignments[vmId as string] = {
        vmId,
        vmName: `水下机器人-${vmId}`,
        assignments: [],
        totalAssignments: 0
      }
    }
    
    // 从 mockUsers 获取用户信息
    const { mockUsers } = await import('../data/userMockData')
    const user = mockUsers.find(u => u.userId === userId)
    
    // 检查是否已经分配
    const existingIndex = mockVmAssignments[vmId as string].assignments.findIndex(
      (a: any) => a.userId === userId
    )
    
    if (existingIndex !== -1) {
      return HttpResponse.json({
        code: 409,
        message: '该用户已被分配此水下机器人',
        data: null
      }, { status: 409 })
    }
    
    // 添加新的分配记录
    const newAssignment = {
      userId: userId as string,
      username: user?.username || `user-${userId}`,
      email: user?.email || `${userId}@example.com`,
      permissions: body.permissions || ['READ'],
      assignedAt: new Date().toISOString(),
      assignedBy: 'admin',
      notes: body.notes
    }
    
    mockVmAssignments[vmId as string].assignments.push(newAssignment)
    mockVmAssignments[vmId as string].totalAssignments = mockVmAssignments[vmId as string].assignments.length
    
    console.log(`[MSW] 分配成功，当前分配数: ${mockVmAssignments[vmId as string].totalAssignments}`)
    
    return HttpResponse.json({
      code: 200,
      message: '水下机器人分配成功',
      data: newAssignment
    })
  }),

  // 取消VM分配
  http.delete('http://localhost:5173/api/admin/vm/:vmId/assign/:userId', ({ params }) => {
    const { vmId, userId } = params
    console.log(`[MSW] 匹配: DELETE /api/admin/vm/${vmId}/assign/${userId}`)
    
    // 从 mockVmAssignments 中移除分配记录
    if (mockVmAssignments[vmId as string]) {
      const assignments = mockVmAssignments[vmId as string].assignments
      const index = assignments.findIndex((a: any) => a.userId === userId)
      
      if (index !== -1) {
        assignments.splice(index, 1)
        mockVmAssignments[vmId as string].totalAssignments = assignments.length
        console.log(`[MSW] 取消分配成功，剩余分配数: ${mockVmAssignments[vmId as string].totalAssignments}`)
      } else {
        return HttpResponse.json({
          code: 404,
          message: '未找到该分配记录',
          data: null
        }, { status: 404 })
      }
    }
    
    return HttpResponse.json({
      code: 200,
      message: '取消分配成功',
      data: {
        vmId,
        userId,
        unassignedAt: new Date().toISOString()
      }
    })
  }),

  // 更新用户VM权限
  http.put('http://localhost:5173/api/admin/vm/:vmId/assign/:userId/permissions', async ({ params, request }) => {
    const { vmId, userId } = params
    const body = await request.json() as any
    
    console.log(`[MSW] 匹配: PUT /api/admin/vm/${vmId}/assign/${userId}/permissions`, body)
    
    // 更新 mockVmAssignments 中的权限
    if (mockVmAssignments[vmId as string]) {
      const assignment = mockVmAssignments[vmId as string].assignments.find(
        (a: any) => a.userId === userId
      )
      
      if (assignment) {
        const oldPermissions = [...assignment.permissions]
        assignment.permissions = body.permissions
        assignment.updatedAt = new Date().toISOString()
        
        console.log(`[MSW] 权限更新成功: ${oldPermissions.join(',')} -> ${body.permissions.join(',')}`)
        
        return HttpResponse.json({
          code: 200,
          message: '权限更新成功',
          data: {
            vmId,
            userId,
            oldPermissions,
            newPermissions: body.permissions,
            updatedAt: new Date().toISOString()
          }
        })
      } else {
        return HttpResponse.json({
          code: 404,
          message: '未找到该分配记录',
          data: null
        }, { status: 404 })
      }
    }
    
    return HttpResponse.json({
      code: 404,
      message: '未找到该水下机器人',
      data: null
    }, { status: 404 })
  }),

  // 2.1 获取用户的VM列表 - GET /api/admin/user/:userId/vms
  http.get('http://localhost:5173/api/admin/user/:userId/vms', ({ params, request }) => {
    const { userId } = params
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '10')
    
    console.log(`[MSW] 匹配: GET /api/admin/user/${userId}/vms`)
    console.log(`[MSW] 查询 userId:`, userId, '当前 mockUserVms keys:', Object.keys(mockUserVms))
    
    const userVms = mockUserVms[userId as string] || []
    console.log(`[MSW] 找到的用户VM数量:`, userVms.length, 'VMs:', userVms)
    
    return HttpResponse.json({
      code: 200,
      message: '获取成功',
      data: {
        userId,
        username: `user-${userId}`,
        vms: userVms,
        list: userVms,  // 添加 list 字段，匹配 service 期望
        records: userVms,  // 添加 records 字段，作为备用
        total: userVms.length,
        page,
        size
      }
    })
  }),

  // 批量分配VM给用户
  http.post('http://localhost:5173/api/admin/user/:userId/vm/batch-assign', async ({ params, request }) => {
    const { userId } = params
    const body = await request.json() as any
    
    console.log(`[MSW] 匹配: POST /api/admin/user/${userId}/vm/batch-assign`, body)
    console.log(`[MSW] 当前 mockUserVms keys:`, Object.keys(mockUserVms))
    console.log(`[MSW] userId 类型:`, typeof userId, 'userId 值:', userId)
    
    // 实际更新 mockUserVms
    if (!mockUserVms[userId as string]) {
      mockUserVms[userId as string] = []
      console.log(`[MSW] 为用户 ${userId} 创建新的VM列表`)
    }
    
    const results = body.vmIds.map((vmId: string) => {
      // 从 mockAllAdminVms 中找到对应的 VM
      const vm = mockAllAdminVms.find(v => v.vmId === vmId)
      
      if (vm) {
        // 添加到用户的 VM 列表（避免重复）
        const existingIndex = mockUserVms[userId as string].findIndex(v => v.vmId === vmId)
        const assignedVm = {
          vmId: vm.vmId,
          vmName: vm.name,
          ipAddress: vm.ipAddress,
          status: vmStatusMap.get(vmId) || vm.status,
          permissions: body.permissions || ['READ'],
          assignedAt: new Date().toISOString(),
          notes: body.notes
        }
        
        if (existingIndex >= 0) {
          // 更新已存在的分配
          mockUserVms[userId as string][existingIndex] = assignedVm
        } else {
          // 添加新的分配
          mockUserVms[userId as string].push(assignedVm)
        }
        
        console.log(`[MSW] 批量分配：已将 VM ${vmId} 分配给用户 ${userId}`)
      }
      
      return {
        vmId,
        status: 'SUCCESS',
        assignedAt: new Date().toISOString()
      }
    })
    
    return HttpResponse.json({
      code: 200,
      message: '批量分配成功',
      data: {
        userId,
        successCount: results.length,
        failedCount: 0,
        results
      }
    })
  }),

  // 批量移除用户VM
  http.delete('http://localhost:5173/api/admin/user/:userId/vm/batch-remove', async ({ params, request }) => {
    const { userId } = params
    const body = await request.json() as any
    
    console.log(`[MSW] 匹配: DELETE /api/admin/user/${userId}/vm/batch-remove`, body)
    
    // 实际更新 mockUserVms
    if (mockUserVms[userId as string]) {
      body.vmIds.forEach((vmId: string) => {
        const index = mockUserVms[userId as string].findIndex(v => v.vmId === vmId)
        if (index >= 0) {
          mockUserVms[userId as string].splice(index, 1)
          console.log(`[MSW] 批量移除：已从用户 ${userId} 移除 VM ${vmId}`)
        }
      })
    }
    
    const results = body.vmIds.map((vmId: string) => ({
      vmId,
      status: 'SUCCESS',
      unassignedAt: new Date().toISOString()
    }))
    
    return HttpResponse.json({
      code: 200,
      message: '批量移除成功',
      data: {
        userId,
        successCount: results.length,
        failedCount: 0,
        results
      }
    })
  }),

  // 3.4 管理员强制控制VM
  http.post('http://localhost:5173/api/admin/vm/:vmId/force-control', async ({ params, request }) => {
    const { vmId } = params as { vmId: string }
    const body = await request.json() as any
    
    console.log(`[MSW] 匹配: POST /api/admin/vm/${vmId}/force-control`, body)
    
    // 根据操作类型更新水下机器人状态
    const action = body.action
    // 注意：以下延迟时间是为了模拟真实VM操作而设定的，不是从接口文档读取
    // 真实环境中，后端会立即返回命令已发送，实际状态变化通过 WebSocket 推送
    // timeout 参数（默认300秒）是操作的最大等待时间，不是实际执行时间
    switch (action) {
      case 'START':
        // 启动：模拟启动过程约3秒
        vmStatusMap.set(vmId, 'STARTING')
        setTimeout(() => {
          vmStatusMap.set(vmId, 'RUNNING')
          console.log(`[MSW] 强制控制：水下机器人 ${vmId} 已启动`)
        }, 3000)
        break
      
      case 'STOP':
        // 停止：模拟正常停止约2秒
        vmStatusMap.set(vmId, 'STOPPING')
        setTimeout(() => {
          vmStatusMap.set(vmId, 'STOPPED')
          console.log(`[MSW] 强制控制：水下机器人 ${vmId} 已停止`)
        }, 2000)
        break
      
      case 'FORCE_STOP':
        // 强制停止：立即生效（模拟强制断电）
        vmStatusMap.set(vmId, 'STOPPED')
        console.log(`[MSW] 强制控制：水下机器人 ${vmId} 已强制停止`)
        break
      
      case 'RESTART':
        // 重启：模拟完整重启流程约5秒（停止1秒 → 启动2秒 → 完成5秒）
        vmStatusMap.set(vmId, 'STOPPING')
        setTimeout(() => {
          vmStatusMap.set(vmId, 'STARTING')
          console.log(`[MSW] 强制控制：水下机器人 ${vmId} 正在重启...`)
        }, 1000)
        setTimeout(() => {
          vmStatusMap.set(vmId, 'RUNNING')
          console.log(`[MSW] 强制控制：水下机器人 ${vmId} 重启完成`)
        }, 5000)
        break
      
      default:
        console.warn(`[MSW] 未知的强制控制操作: ${action}`)
    }
    
    return HttpResponse.json({
      code: 200,
      message: '强制控制命令已发送',
      data: {
        vmId,
        action: body.action,
        commandId: `cmd-admin-${Date.now()}`,
        reason: body.reason,
        executedBy: 'admin',
        executedAt: new Date().toISOString(),
        estimatedTime: action === 'FORCE_STOP' ? 0 : (action === 'RESTART' ? 120 : 60)
      }
    })
  })
]

