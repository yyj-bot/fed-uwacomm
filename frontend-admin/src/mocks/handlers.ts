/**
 * Mock API 处理器
 * 定义所有 API 的模拟响应
 */

import { http, HttpResponse } from 'msw'
import { federatedTaskHandlers } from './handlers/federatedTaskHandlers'
import { modelManagementHandlers } from './handlers/modelManagementHandlers'
import { vmHandlers } from './handlers/vmHandlers'

export const handlers = [
  // 用户登录 - 支持绝对路径和相对路径
  http.post('http://localhost:5173/api/user/login', async ({ request }) => {
    const body = await request.json() as any
    
    // 简单的登录验证
    if (body.loginIdentifier === 'admin' && body.password === 'admin123') {
      return HttpResponse.json({
        code: 200,
        message: '登录成功',
        data: {
          token: 'mock-jwt-token',
          refreshToken: 'mock-refresh-token',
          expiresIn: 3600,
          user: {
            userId: '1',
            username: 'admin',
            email: 'admin@feduwacomm.com',
            role: 'ADMIN',
            status: 'ACTIVE',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          }
        }
      })
    }
    
    return HttpResponse.json({
      code: 401,
      message: '用户名或密码错误',
      data: null
    }, { status: 401 })
  }),

  // 获取用户信息
  http.get('http://localhost:5173/api/user/profile', () => {
    return HttpResponse.json({
      code: 200,
      message: '获取成功',
      data: {
        userId: '1',
        username: 'admin',
        email: 'admin@feduwacomm.com',
        role: 'ADMIN',
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    })
  }),

  // 仪表盘概览数据
  http.get('http://localhost:5173/api/dashboard/overview', () => {
    return HttpResponse.json({
      code: 200,
      message: '获取成功',
      data: {
        vmStats: {
          totalVMs: 12,
          runningVMs: 8,
          stoppedVMs: 3,
          errorVMs: 1,
          cpuUsage: 65,
          memoryUsage: 72,
          diskUsage: 45
        },
        taskStats: {
          totalTasks: 25,
          runningTasks: 5,
          completedTasks: 18,
          failedTasks: 2,
          tasksToday: 3,
          averageTrainingTime: 45.2
        },
        dataStats: {
          totalDatasets: 48,
          totalDataSize: 1024.5,
          processedData: 856.3,
          pendingData: 168.2,
          uploadedToday: 12
        }
      }
    })
  }),

  // 虚拟机列表
  http.get('http://localhost:5173/api/vms', () => {
    return HttpResponse.json({
      code: 200,
      message: '获取成功',
      data: {
        items: [
          {
            vmId: '1',
            name: 'VM-001',
            status: 'RUNNING',
            cpuUsage: 45,
            memoryUsage: 60,
            ipAddress: '192.168.1.101',
            createdAt: new Date().toISOString()
          },
          {
            vmId: '2',
            name: 'VM-002',
            status: 'RUNNING',
            cpuUsage: 32,
            memoryUsage: 48,
            ipAddress: '192.168.1.102',
            createdAt: new Date().toISOString()
          },
          {
            vmId: '3',
            name: 'VM-003',
            status: 'STOPPED',
            cpuUsage: 0,
            memoryUsage: 0,
            ipAddress: '192.168.1.103',
            createdAt: new Date().toISOString()
          }
        ],
        total: 3,
        page: 1,
        size: 10
      }
    })
  }),

  // 管理员用户列表
  http.get('http://localhost:5173/api/admin/user/list', () => {
    return HttpResponse.json({
      code: 200,
      message: '获取成功',
      data: {
        items: [
          {
            userId: '1',
            username: 'admin',
            email: 'admin@feduwacomm.com',
            role: 'ADMIN',
            status: 'ACTIVE',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          },
          {
            userId: '2',
            username: 'researcher',
            email: 'researcher@feduwacomm.com',
            role: 'RESEARCHER',
            status: 'ACTIVE',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          },
          {
            userId: '3',
            username: 'operator',
            email: 'operator@feduwacomm.com',
            role: 'OPERATOR',
            status: 'ACTIVE',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          }
        ],
        total: 3,
        page: 1,
        size: 10
      }
    })
  }),

  // 系统日志
  http.get('http://localhost:5173/api/log/list', () => {
    return HttpResponse.json({
      code: 200,
      message: '获取成功',
      data: {
        items: [
          {
            logId: '1',
            level: 'INFO',
            message: '系统启动成功',
            timestamp: new Date().toISOString(),
            source: 'SYSTEM'
          },
          {
            logId: '2',
            level: 'WARN',
            message: '磁盘空间不足',
            timestamp: new Date().toISOString(),
            source: 'STORAGE'
          }
        ],
        total: 2,
        page: 1,
        size: 10
      }
    })
  }),

  // ==================== 虚拟机管理 API ====================
  ...vmHandlers,

  // ==================== 联邦学习任务管理 API ====================
  ...federatedTaskHandlers,

  // ==================== 模型管理 API ====================
  ...modelManagementHandlers
]