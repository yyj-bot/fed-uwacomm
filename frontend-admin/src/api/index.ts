// 用户认证API
export { userApi } from './user'

// 管理员API
export { default as adminApi } from './admin'

// 虚拟机管理API
export { vmApi } from './vm'

// 联邦学习任务API
export { default as federatedTaskApi } from './federated-task'

// 模型版本管理API
export { default as modelApi } from './model-version'

// 训练数据API
export { default as trainingDataApi } from './training-data'

// 系统日志API
export { default as logApi } from './system-log'

// 虚拟机轮次模块API
export { default as vmRoundModelsApi } from './vm-round-modules'

// 重新导出现有的auth和websocket服务
export { authService } from '../services'
export { websocketService } from '../services' 