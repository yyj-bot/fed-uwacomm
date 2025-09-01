/**
 * VM本地模型 Mock 数据
 * 基于接口文档提供一致的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { ApiResponse } from '@/types'

// ==================== Mock 数据定义 ====================

// 模拟VM本地模型数据
const mockVMRoundModels = [
  {
    vmRoundModelId: 'vmrm-001',
    taskId: 'a1b2c3d4e5f678901234567890123456',
    roundNumber: 1,
    vmId: 'b2c3d4e5f678901234567890123456cd',
    modelJson: { weights: [0.1, 0.2, 0.3], biases: [0.01, 0.02] },
    metrics: {
      accuracy: 0.75,
      loss: 0.25,
      precision: 0.73,
      recall: 0.77,
      f1: 0.75
    },
    createdAt: '2024-01-01T10:00:00.000Z'
  },
  {
    vmRoundModelId: 'vmrm-002',
    taskId: 'a1b2c3d4e5f678901234567890123456',
    roundNumber: 2,
    vmId: 'b2c3d4e5f678901234567890123456cd',
    modelJson: { weights: [0.15, 0.25, 0.35], biases: [0.015, 0.025] },
    metrics: {
      accuracy: 0.80,
      loss: 0.20,
      precision: 0.78,
      recall: 0.82,
      f1: 0.80
    },
    createdAt: '2024-01-01T11:00:00.000Z'
  },
  {
    vmRoundModelId: 'vmrm-003',
    taskId: 'a1b2c3d4e5f678901234567890123456',
    roundNumber: 3,
    vmId: 'b2c3d4e5f678901234567890123456cd',
    modelJson: { weights: [0.18, 0.28, 0.38], biases: [0.018, 0.028] },
    metrics: {
      accuracy: 0.85,
      loss: 0.15,
      precision: 0.83,
      recall: 0.87,
      f1: 0.85
    },
    createdAt: '2024-01-01T12:00:00.000Z'
  },
  {
    vmRoundModelId: 'vmrm-004',
    taskId: 'a1b2c3d4e5f678901234567890123456',
    roundNumber: 1,
    vmId: 'c3d4e5f6789012345678901234567890',
    modelJson: { weights: [0.12, 0.22, 0.32], biases: [0.012, 0.022] },
    metrics: {
      accuracy: 0.78,
      loss: 0.22,
      precision: 0.76,
      recall: 0.80,
      f1: 0.78
    },
    createdAt: '2024-01-01T10:05:00.000Z'
  },
  {
    vmRoundModelId: 'vmrm-005',
    taskId: 'a1b2c3d4e5f678901234567890123456',
    roundNumber: 2,
    vmId: 'c3d4e5f6789012345678901234567890',
    modelJson: { weights: [0.16, 0.26, 0.36], biases: [0.016, 0.026] },
    metrics: {
      accuracy: 0.82,
      loss: 0.18,
      precision: 0.80,
      recall: 0.84,
      f1: 0.82
    },
    createdAt: '2024-01-01T11:05:00.000Z'
  },
  {
    vmRoundModelId: 'vmrm-006',
    taskId: 'a1b2c3d4e5f678901234567890123456',
    roundNumber: 25,
    vmId: 'b2c3d4e5f678901234567890123456cd',
    modelJson: { weights: [0.5, 0.6, 0.7], biases: [0.05, 0.06] },
    metrics: {
      accuracy: 0.88,
      loss: 0.12,
      precision: 0.86,
      recall: 0.90,
      f1: 0.88
    },
    createdAt: '2024-01-01T20:00:00.000Z'
  }
]

// ==================== Mock 函数实现 ====================

export const vmRoundModelsMock = {
  // ==================== 2.1 本地模型结果分页查询 ====================
  getVMRoundModels: (params: any = {}): ApiResponse<any> => {
    let filteredModels = [...mockVMRoundModels]

    // 应用过滤条件
    if (params.taskId) {
      filteredModels = filteredModels.filter(m => m.taskId === params.taskId)
    }
    if (params.roundNumber) {
      filteredModels = filteredModels.filter(m => m.roundNumber === params.roundNumber)
    }
    if (params.vmId) {
      filteredModels = filteredModels.filter(m => m.vmId === params.vmId)
    }

    // 应用分页
    const page = params.page || 1
    const size = params.size || 10
    const start = (page - 1) * size
    const end = start + size
    const paginatedModels = filteredModels.slice(start, end)

    return {
      code: 200,
      message: '查询成功',
      data: {
        total: filteredModels.length,
        pages: Math.ceil(filteredModels.length / size),
        current: page,
        size: size,
        records: paginatedModels
      }
    }
  },

  // ==================== 2.2 本地模型结果详情查询 ====================
  getVMRoundModelDetail: (vmRoundModelId: string): ApiResponse<any> => {
    const model = mockVMRoundModels.find(m => m.vmRoundModelId === vmRoundModelId)
    
    if (!model) {
      return {
        code: 404,
        message: 'VM本地模型不存在',
        data: null
      } as any
    }

    return {
      code: 200,
      message: '查询成功',
      data: model
    }
  },

  // ==================== 2.3 本地模型训练指标趋势 ====================
  getVMModelTrend: (params: {
    taskId: string
    vmId: string
    metric: string
  }): ApiResponse<{
    taskId: string
    vmId: string
    metric: string
    trend: Array<{ roundNumber: number; value: number }>
  }> => {
    // 验证参数
    if (!params.taskId || !params.vmId || !params.metric) {
      return {
        code: 400,
        message: '参数不完整',
        data: null
      } as any
    }

    // 查找匹配的模型数据
    const vmModels = mockVMRoundModels.filter(m => 
      m.taskId === params.taskId && m.vmId === params.vmId
    )

    if (vmModels.length === 0) {
      return {
        code: 404,
        message: '未找到匹配的VM模型数据',
        data: null
      } as any
    }

    // 构建趋势数据
    const trend = vmModels
      .sort((a, b) => a.roundNumber - b.roundNumber)
      .map(model => ({
        roundNumber: model.roundNumber,
        value: (model.metrics as any)[params.metric] || 0
      }))
      .filter(item => item.value > 0) // 过滤掉不存在的指标

    if (trend.length === 0) {
      return {
        code: 404,
        message: `未找到指标 ${params.metric} 的数据`,
        data: null
      } as any
    }

    return {
      code: 200,
      message: '查询成功',
      data: {
        taskId: params.taskId,
        vmId: params.vmId,
        metric: params.metric,
        trend: trend
      }
    }
  },

  // ==================== 2.4 本地模型最佳/离群查询 ====================
  getVMModelBest: (params: {
    taskId: string
    metric: string
    type: 'best' | 'outlier'
  }): ApiResponse<{
    taskId: string
    metric: string
    type: 'best' | 'outlier'
    result: {
      vmRoundModelId: string
      roundNumber: number
      vmId: string
      value: number
    }
  }> => {
    // 验证参数
    if (!params.taskId || !params.metric || !params.type) {
      return {
        code: 400,
        message: '参数不完整',
        data: null
      } as any
    }

    if (!['best', 'outlier'].includes(params.type)) {
      return {
        code: 400,
        message: '查询类型必须为best或outlier',
        data: null
      } as any
    }

    // 查找匹配任务的模型
    const taskModels = mockVMRoundModels.filter(m => m.taskId === params.taskId)

    if (taskModels.length === 0) {
      return {
        code: 404,
        message: '未找到匹配的任务数据',
        data: null
      } as any
    }

    // 获取指标值并排序
    const modelsWithMetric = taskModels
      .map(model => ({
        ...model,
        metricValue: (model.metrics as any)[params.metric]
      }))
      .filter(model => model.metricValue !== undefined)

    if (modelsWithMetric.length === 0) {
      return {
        code: 404,
        message: `未找到指标 ${params.metric} 的数据`,
        data: null
      } as any
    }

    // 根据指标类型选择最佳或离群
    let targetModel
    if (params.type === 'best') {
      // 对于accuracy、precision、recall、f1，值越大越好
      // 对于loss，值越小越好
      if (params.metric === 'loss') {
        targetModel = modelsWithMetric.reduce((min, current) => 
          current.metricValue < min.metricValue ? current : min
        )
      } else {
        targetModel = modelsWithMetric.reduce((max, current) => 
          current.metricValue > max.metricValue ? current : max
        )
      }
    } else { // outlier
      // 简化处理：找到与平均值差距最大的
      const avgValue = modelsWithMetric.reduce((sum, model) => sum + model.metricValue, 0) / modelsWithMetric.length
      targetModel = modelsWithMetric.reduce((outlier, current) => {
        const currentDiff = Math.abs(current.metricValue - avgValue)
        const outlierDiff = Math.abs(outlier.metricValue - avgValue)
        return currentDiff > outlierDiff ? current : outlier
      })
    }

    return {
      code: 200,
      message: '查询成功',
      data: {
        taskId: params.taskId,
        metric: params.metric,
        type: params.type,
        result: {
          vmRoundModelId: targetModel.vmRoundModelId,
          roundNumber: targetModel.roundNumber,
          vmId: targetModel.vmId,
          value: targetModel.metricValue
        }
      }
    }
  }
} as const

export default vmRoundModelsMock
