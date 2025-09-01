/**
 * VM轮次模型服务模块统一导出
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// 导出服务类和实例
export { VMRoundModelsService, vmRoundModelsService } from './vmRoundModelsService'

// 导出类型定义
export type {
  VMRoundModel,
  VMModelTrend,
  VMModelBest,
  VMRoundModelListParams,
  VMModelTrendParams,
  VMModelBestParams,
  VMRoundModelPaginatedResponse,
  MetricType,
  QueryType
} from './type'