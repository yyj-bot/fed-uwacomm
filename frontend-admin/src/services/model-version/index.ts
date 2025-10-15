/**
 * 模型版本管理服务模块统一导出
 * 包含模型版本管理和初始模型管理功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// 导出服务类和实例
export { ModelVersionService, modelVersionService } from './modelVersionService'

// 导出所有类型定义
export * from './type'

// 重新导出API层（方便直接使用）
export { model, initialModel } from '@/api/model-version'