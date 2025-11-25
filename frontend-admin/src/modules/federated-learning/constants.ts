export const PATH_TO_TASK_TYPE: Record<string, string> = {
  '/federated-learning/sonar-recognition': 'CLASSIFICATION',
  '/federated-learning/sonar-recognition/list': 'CLASSIFICATION',
  '/federated-learning/acoustic-propagation': 'REGRESSION',
  '/federated-learning/acoustic-propagation/list': 'REGRESSION',
  '/federated-learning/seabed-terrain': 'ANOMALY_DETECTION',
  '/federated-learning/seabed-terrain/list': 'ANOMALY_DETECTION'
}

export const TASK_TYPE_TO_TITLE: Record<string, string> = {
  CLASSIFICATION: '水下声呐目标识别',
  REGRESSION: '声学传播回归分析',
  ANOMALY_DETECTION: '海底地形声学分析'
}

export const TASK_TYPE_DESCRIPTION: Record<string, string> = {
  CLASSIFICATION: '管理联邦水下声呐目标识别任务的执行与监控状态',
  REGRESSION: '管理声学传播回归分析任务的执行与监控状态',
  ANOMALY_DETECTION: '管理海底地形声学分析任务的执行与监控状态'
}
