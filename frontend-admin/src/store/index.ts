import { create } from 'zustand'
import type { 
  User, 
  VirtualMachine, 
  FederatedTask,
  SystemLog,
  ModelVersion,
  TrainingDataset
} from '@/types'
import { ConnectionState } from '@/types'

// 应用状态接口
interface AppState {
  // 用户状态
  user: User | null
  isAuthenticated: boolean
  
  // WebSocket连接状态
  wsState: ConnectionState
  
  // 虚拟机状态
  virtualMachines: VirtualMachine[]
  vmLoading: boolean
  
  // 联邦学习任务状态
  federatedTasks: FederatedTask[]
  tasksLoading: boolean
  
  // 系统日志状态
  systemLogs: SystemLog[]
  logsLoading: boolean
  
  // 模型版本状态
  modelVersions: ModelVersion[]
  modelsLoading: boolean
  
  // 训练数据状态
  trainingData: TrainingDataset[]
  dataLoading: boolean
  
  // 全局加载状态
  globalLoading: boolean
  
  // 错误状态
  error: string | null
}

// 状态更新actions
interface AppActions {
  // 用户actions
  setUser: (user: User | null) => void
  setAuthenticated: (isAuthenticated: boolean) => void
  
  // WebSocket actions
  setWsState: (state: ConnectionState) => void
  
  // 虚拟机actions
  setVirtualMachines: (vms: VirtualMachine[]) => void
  setVmLoading: (loading: boolean) => void
  updateVirtualMachine: (vmId: string, updates: Partial<VirtualMachine>) => void
  addVirtualMachine: (vm: VirtualMachine) => void
  removeVirtualMachine: (vmId: string) => void
  
  // 联邦学习任务actions
  setFederatedTasks: (tasks: FederatedTask[]) => void
  setTasksLoading: (loading: boolean) => void
  updateFederatedTask: (taskId: string, updates: Partial<FederatedTask>) => void
  addFederatedTask: (task: FederatedTask) => void
  removeFederatedTask: (taskId: string) => void
  
  // 系统日志actions
  setSystemLogs: (logs: SystemLog[]) => void
  setLogsLoading: (loading: boolean) => void
  addSystemLog: (log: SystemLog) => void
  clearSystemLogs: () => void
  
  // 模型版本actions
  setModelVersions: (models: ModelVersion[]) => void
  setModelsLoading: (loading: boolean) => void
  addModelVersion: (model: ModelVersion) => void
  updateModelVersion: (modelId: string, updates: Partial<ModelVersion>) => void
  
  // 训练数据actions
  setTrainingData: (data: TrainingDataset[]) => void
  setDataLoading: (loading: boolean) => void
  addTrainingData: (data: TrainingDataset) => void
  updateTrainingData: (datasetId: string, updates: Partial<TrainingDataset>) => void
  removeTrainingData: (datasetId: string) => void
  
  // 全局actions
  setGlobalLoading: (loading: boolean) => void
  setError: (error: string | null) => void
  clearError: () => void
  
  // 重置状态
  reset: () => void
}

// 初始状态
const initialState: AppState = {
  user: null,
  isAuthenticated: false,
  wsState: 'disconnected' as ConnectionState,
  virtualMachines: [],
  vmLoading: false,
  federatedTasks: [],
  tasksLoading: false,
  systemLogs: [],
  logsLoading: false,
  modelVersions: [],
  modelsLoading: false,
  trainingData: [],
  dataLoading: false,
  globalLoading: false,
  error: null
}

// 创建store
export const useAppStore = create<AppState & AppActions>((set, get) => ({
  ...initialState,

  // 用户actions
  setUser: (user) => set({ user }),
  setAuthenticated: (isAuthenticated) => set({ isAuthenticated }),

  // WebSocket actions
  setWsState: (wsState) => set({ wsState }),

  // 虚拟机actions
  setVirtualMachines: (virtualMachines) => set({ virtualMachines }),
  setVmLoading: (vmLoading) => set({ vmLoading }),
  
  updateVirtualMachine: (vmId, updates) => set((state) => ({
    virtualMachines: state.virtualMachines.map(vm => 
      vm.vmId === vmId ? { ...vm, ...updates } : vm
    )
  })),
  
  addVirtualMachine: (vm) => set((state) => ({
    virtualMachines: [...state.virtualMachines, vm]
  })),
  
  removeVirtualMachine: (vmId) => set((state) => ({
    virtualMachines: state.virtualMachines.filter(vm => vm.vmId !== vmId)
  })),

  // 联邦学习任务actions
  setFederatedTasks: (federatedTasks) => set({ federatedTasks }),
  setTasksLoading: (tasksLoading) => set({ tasksLoading }),
  
  updateFederatedTask: (taskId, updates) => set((state) => ({
    federatedTasks: state.federatedTasks.map(task => 
      task.taskId === taskId ? { ...task, ...updates } : task
    )
  })),
  
  addFederatedTask: (task) => set((state) => ({
    federatedTasks: [...state.federatedTasks, task]
  })),
  
  removeFederatedTask: (taskId) => set((state) => ({
    federatedTasks: state.federatedTasks.filter(task => task.taskId !== taskId)
  })),

  // 系统日志actions
  setSystemLogs: (systemLogs) => set({ systemLogs }),
  setLogsLoading: (logsLoading) => set({ logsLoading }),
  
  addSystemLog: (log) => set((state) => ({
    systemLogs: [log, ...state.systemLogs].slice(0, 1000) // 保持最新1000条日志
  })),
  
  clearSystemLogs: () => set({ systemLogs: [] }),

  // 模型版本actions
  setModelVersions: (modelVersions) => set({ modelVersions }),
  setModelsLoading: (modelsLoading) => set({ modelsLoading }),
  
  addModelVersion: (model) => set((state) => ({
    modelVersions: [...state.modelVersions, model]
  })),
  
  updateModelVersion: (modelId, updates) => set((state) => ({
    modelVersions: state.modelVersions.map(model => 
      model.modelId === modelId ? { ...model, ...updates } : model
    )
  })),

  // 训练数据actions
  setTrainingData: (trainingData) => set({ trainingData }),
  setDataLoading: (dataLoading) => set({ dataLoading }),
  
  addTrainingData: (data) => set((state) => ({
    trainingData: [...state.trainingData, data]
  })),
  
  updateTrainingData: (datasetId, updates) => set((state) => ({
    trainingData: state.trainingData.map(data => 
      data.datasetId === datasetId ? { ...data, ...updates } : data
    )
  })),
  
  removeTrainingData: (datasetId) => set((state) => ({
    trainingData: state.trainingData.filter(data => data.datasetId !== datasetId)
  })),

  // 全局actions
  setGlobalLoading: (globalLoading) => set({ globalLoading }),
  setError: (error) => set({ error }),
  clearError: () => set({ error: null }),

  // 重置状态
  reset: () => set(initialState)
}))

// 选择器
export const selectors = {
  // 获取在线虚拟机数量
  getOnlineVMCount: () => {
    const { virtualMachines } = useAppStore.getState()
    return virtualMachines.filter(vm => vm.status === 'RUNNING').length
  },
  
  // 获取运行中的任务数量
  getRunningTaskCount: () => {
    const { federatedTasks } = useAppStore.getState()
    return federatedTasks.filter(task => task.status === 'RUNNING').length
  },
  
  // 获取最近的错误日志
  getRecentErrorLogs: () => {
    const { systemLogs } = useAppStore.getState()
    return systemLogs.filter(log => log.level === 'ERROR').slice(0, 10)
  },
  
  // 获取指定虚拟机
  getVMById: (vmId: string) => {
    const { virtualMachines } = useAppStore.getState()
    return virtualMachines.find(vm => vm.vmId === vmId)
  },
  
  // 获取指定任务
  getTaskById: (taskId: string) => {
    const { federatedTasks } = useAppStore.getState()
    return federatedTasks.find(task => task.taskId === taskId)
  }
}

export default useAppStore 