/**
 * VM本地模型服务类型定义
 * 定义VM本地模型管理相关的请求和响应类型
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { PaginationParams } from '@/types'

// ==================== 基础类型定义 ====================

/**
 * VM本地模型专用分页响应类型（符合 vm-round-models-api-reference.md）
 */
export interface VMRoundModelPaginatedResponse<T> {
  readonly total: number
  readonly current: number  // 使用 current 而不是 page
  readonly size: number
  readonly pages: number
  readonly records: T[]
}

/**
 * VM本地模型结果类型
 */
export interface VMRoundModel {
  /** VM本地模型ID */
  readonly vmRoundModelId: string
  /** 任务ID */
  readonly taskId: string
  /** 训练轮数 */
  readonly roundNumber: number
  /** 虚拟机ID */
  readonly vmId: string
  /** 模型JSON数据（可选） */
  readonly modelJson?: Record<string, unknown>
  /** 评估指标 */
  readonly metrics: {
    readonly accuracy: number
    readonly loss: number
    readonly [key: string]: number
  }
  /** 创建时间 */
  readonly createdAt: string
}

/**
 * VM本地模型训练指标趋势类型
 */
export interface VMModelTrend {
  /** 任务ID */
  readonly taskId: string
  /** 虚拟机ID */
  readonly vmId: string
  /** 指标名称 */
  readonly metric: string
  /** 趋势数据 */
  readonly trend: Array<{
    readonly roundNumber: number
    readonly value: number
  }>
}

/**
 * VM本地模型最佳/离群查询结果类型
 */
export interface VMModelBest {
  /** 任务ID */
  readonly taskId: string
  /** 指标名称 */
  readonly metric: string
  /** 查询类型 */
  readonly type: 'best' | 'outlier'
  /** 查询结果 */
  readonly result: {
    readonly vmRoundModelId: string
    readonly roundNumber: number
    readonly vmId: string
    readonly value: number
  }
}

// ==================== 请求参数类型 ====================

/**
 * VM本地模型列表查询参数
 */
export interface VMRoundModelListParams extends PaginationParams {
  /** 任务ID过滤 */
  readonly taskId?: string
  /** 训练轮数过滤 */
  readonly roundNumber?: number
  /** 虚拟机ID过滤 */
  readonly vmId?: string
}

/**
 * VM模型趋势查询参数
 */
export interface VMModelTrendParams {
  /** 任务ID */
  readonly taskId: string
  /** 虚拟机ID */
  readonly vmId: string
  /** 指标名称 */
  readonly metric: string
}

/**
 * VM模型最佳/离群查询参数
 */
export interface VMModelBestParams {
  /** 任务ID */
  readonly taskId: string
  /** 指标名称 */
  readonly metric: string
  /** 查询类型 */
  readonly type: 'best' | 'outlier'
}

// ==================== 常量类型 ====================

/**
 * 支持的指标类型
 */
export type MetricType = 'accuracy' | 'loss' | 'precision' | 'recall' | 'f1'

/**
 * 查询类型
 */
export type QueryType = 'best' | 'outlier'

// ==================== 常量定义 ====================

/**
 * 支持的指标常量
 */
export const SUPPORTED_METRICS = {
  ACCURACY: 'accuracy',
  LOSS: 'loss',
  PRECISION: 'precision',
  RECALL: 'recall',
  F1: 'f1'
} as const

/**
 * 查询类型常量
 */
export const QUERY_TYPES = {
  BEST: 'best',
  OUTLIER: 'outlier'
} as const

// ==================== 工具类型 ====================

/**
 * 深度只读类型
 */
export type DeepReadonly<T> = {
  readonly [P in keyof T]: T[P] extends object ? DeepReadonly<T[P]> : T[P]
}

/**
 * 可选字段类型
 */
export type PartialBy<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>

/**
 * 必需字段类型
 */
export type RequiredBy<T, K extends keyof T> = Omit<T, K> & Required<Pick<T, K>>
