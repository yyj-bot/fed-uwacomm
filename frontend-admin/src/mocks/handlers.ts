/**
 * Mock API 处理器
 * 定义所有 API 的模拟响应
 */

import { http, HttpResponse } from 'msw'
import { federatedTaskHandlers } from './handlers/federatedTaskHandlers'
import { modelManagementHandlers } from './handlers/modelManagementHandlers'
import { vmHandlers } from './handlers/vmHandlers'
import { userHandlers } from './handlers/userHandlers'
import { systemLogHandlers } from './handlers/systemLogHandlers'
import { adminVmHandlers } from './handlers/adminVmHandlers'

export const handlers = [
  // ==================== 用户管理 API ====================
  ...userHandlers,

  // ==================== 系统日志管理 API ====================
  ...systemLogHandlers,

  // ==================== 管理员虚拟机管理 API ====================
  ...adminVmHandlers,

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