// 严格的数据类型定义 - 遵循KISS原则

// 环境特征数据类型
export interface EnvironmentFeature {
  readonly envId: string
  readonly timestamp: string
  readonly frequency: number
  readonly maxDepth: number
  readonly soundSpeedProfile: {
    readonly minSpeed: number
    readonly maxSpeed: number
    readonly meanSpeed: number
    readonly stdSpeed: number
  }
  readonly sourceDepths: readonly number[]
  readonly receiverDepths: readonly number[]
  readonly ranges: readonly number[]
}

// 模型性能指标类型
export interface ModelMetrics {
  readonly modelName: string
  readonly timestamp: string
  readonly regression?: {
    readonly r2: number
    readonly rmse: number
    readonly mae: number
    readonly mape: number
  }
  readonly classification?: {
    readonly accuracy: number
    readonly precision: number
    readonly recall: number
    readonly f1Score: number
  }
}

// 联邦学习轮次数据类型
export interface FederatedRound {
  readonly roundNumber: number
  readonly timestamp: string
  readonly globalMetrics: {
    readonly globalLoss: number
    readonly globalAccuracy: number
    readonly r2: number
  }
  readonly clientCount: number
  readonly clientMetrics: {
    readonly avgLoss: number
    readonly stdLoss: number
    readonly avgAccuracy: number
    readonly stdAccuracy: number
    readonly totalSamples: number
  }
}

// OFDM性能数据类型
export interface OFDMPerformance {
  readonly snrRange: readonly number[]
  readonly berResults: readonly number[]
  readonly constellation: {
    readonly real: readonly number[]
    readonly imag: readonly number[]
  }
  readonly frequency: {
    readonly spectrum: readonly number[]
    readonly frequencies: readonly number[]
  }
}

// API响应类型
export interface ApiResponse<T> {
  readonly success: boolean
  readonly data: T
  readonly message: string
  readonly timestamp: string
}

// WebSocket消息类型
export interface WebSocketMessage {
  readonly type: 'federated_update' | 'training_progress' | 'system_status'
  readonly data: unknown
  readonly timestamp: string
}

// 图表配置类型
export interface ChartConfig {
  readonly title: string
  readonly width: number
  readonly height: number
  readonly responsive: boolean
} 