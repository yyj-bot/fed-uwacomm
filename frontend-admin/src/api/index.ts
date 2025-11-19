// 用户认证API
export { userApi } from './user'

// 管理员API
export { admin as adminApi } from './admin'

// 水下机器人管理API
export { vmApi } from './vm'

// 联邦学习任务API
export { federatedTask as federatedTaskApi } from './federated-task'
// export { federatedTask as federatedApi } from './federated-task' // 向后兼容别名

// 模型版本管理API
export { model as modelApi } from './model-version'

// 训练数据API
export { trainingData as trainingDataApi } from './training-data'

// 系统日志API
export { log as logApi } from './system-log'
// export { log as systemApi } from './system-log' // 向后兼容别名


// 重新导出服务层
export { userService as authService } from '../services/user' 