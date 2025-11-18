/**
 * 任务创建页面
 * 支持图形化任务创建，包括数据集配置、参与者配置、算法配置等
 * 
 * @author FedUWAComm Team
 * @version 1.4.0
 */

import React, { useEffect, useState, useCallback } from 'react'
import { 
  Card, 
  Form, 
  Input, 
  Select, 
  Button, 
  Steps, 
  Row, 
  Col, 
  InputNumber,
  Switch,
  Table,
  Tag,
  Space,
  message,
  Modal,
  Alert,
  Tooltip,
  Progress,
  Divider,
  Typography
} from 'antd'
import { 
  ArrowLeftOutlined,
  InfoCircleOutlined,
  PlayCircleOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  PlusOutlined,
  DeleteOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useTask } from '@/store/federated-task/useFederatedTaskStore'
import type { 
  AvailableVM, 
  AvailableDataset, 
  AlgorithmTemplate,
  DistributionPreview,
  ParticipantValidation
} from '@/api/federated-task'
import './TaskCreatePage.css'

const { Step } = Steps
const { Option } = Select
const { TextArea } = Input
const { Title, Paragraph, Text } = Typography

interface CreateTaskForm {
  // 基本信息
  taskName: string
  taskType: 'CLASSIFICATION' | 'REGRESSION' | 'CLUSTERING' | 'ANOMALY_DETECTION'
  description?: string
  algorithm: string
  
  // 数据集配置
  datasetConfig: {
    datasetId: string
    distributionStrategy: 'BALANCED' | 'CUSTOM'
    testSplit: number
    trainSplit: number
  }
  
  // 参与者配置
  participantConfig: {
    selectionMode: 'MANUAL' | 'AUTOMATIC'
    requirements?: {
      minParticipants: number
      maxParticipants: number
      minCpuCores: number
      minMemoryMb: number
    }
    participants: Array<{
      vmId: string
      role: 'PARTICIPANT'
      dataRatio: number
      capabilities?: string[]
      constraints?: {
        maxCpuUsage?: number
        maxMemoryUsage?: number
      }
    }>
  }
  
  // 超参数配置
  hyperparameters: {
    learningRate: number
    batchSize: number
    epochs: number
    rounds: number
    minParticipants: number
  }
  
  // 模型配置
  modelConfig: {
    modelType: string
    featureColumns?: string[]
    targetColumn?: string
    testSize: number
    randomState: number
  }
  
  // 调度配置
  schedule?: {
    startTime?: string
    endTime?: string
    timeout: number
  }
}

const TaskCreatePage: React.FC = () => {
  const navigate = useNavigate()
  const [form] = Form.useForm<CreateTaskForm>()
  
  // 使用Hook获取状态和操作
  const {
    createTaskLoading,
    createTaskError,
    createTask,
    clearError
  } = useTask()

  // 本地状态
  const [currentStep, setCurrentStep] = useState(0)
  const [availableVMs, setAvailableVMs] = useState<AvailableVM[]>([])
  const [availableDatasets, setAvailableDatasets] = useState<AvailableDataset[]>([])
  const [algorithmTemplates, setAlgorithmTemplates] = useState<AlgorithmTemplate[]>([])
  const [distributionPreview, setDistributionPreview] = useState<DistributionPreview | null>(null)
  const [participantValidation, setParticipantValidation] = useState<ParticipantValidation | null>(null)
  const [selectedVMs, setSelectedVMs] = useState<string[]>([])
  const [previewLoading, setPreviewLoading] = useState(false)
  const [validationLoading, setValidationLoading] = useState(false)
  const [distributionStrategy, setDistributionStrategy] = useState<string>('BALANCED')
  const [testSplit, setTestSplit] = useState<number>(20)
  const [trainSplit, setTrainSplit] = useState<number>(80)

  // 初始化数据
  useEffect(() => {
    loadInitialData()
    // v1.4 新增：加载可用策略
    loadAvailableStrategies()
  }, [])

  // 监听分配策略变化
  useEffect(() => {
    const strategy = form.getFieldValue(['datasetConfig', 'distributionStrategy'])
    if (strategy) {
      setDistributionStrategy(strategy)
    }
  }, [form])

  // 初始化数据集划分状态
  useEffect(() => {
    const testSplitValue = form.getFieldValue(['datasetConfig', 'testSplit']) || 20
    const trainSplitValue = form.getFieldValue(['datasetConfig', 'trainSplit']) || 80
    setTestSplit(testSplitValue)
    setTrainSplit(trainSplitValue)
  }, [])

  // v1.4 新增：加载可用聚合策略
  const loadAvailableStrategies = async () => {
    try {
      const { federatedTask } = await import('@/api/federated-task')
      const strategiesResponse = await federatedTask.getAvailableStrategies()
      
      console.log('✅ 成功加载聚合策略:', strategiesResponse.total, '个策略')
      console.log('策略详情:', strategiesResponse.strategies)
      
      // 可以在这里处理策略数据，例如更新算法模板
      // 暂时只记录日志，不影响现有逻辑
    } catch (error) {
      console.warn('⚠️ 获取聚合策略失败，使用默认算法模板:', error)
      // 失败时不影响现有功能
    }
  }

  // 从数据集中获取特征列信息的辅助函数
  const getFeatureColumnsFromDataset = useCallback((datasetId?: string): string[] => {
    if (!datasetId) return ['feature_1', 'feature_2', 'feature_3'] // 默认特征列
    
    const dataset = availableDatasets.find(d => d.datasetId === datasetId)
    if (dataset?.features?.featureColumns) {
      return dataset.features.featureColumns
    }
    
    // 如果没找到数据集或特征信息，返回默认值
    return ['feature_1', 'feature_2', 'feature_3']
  }, [availableDatasets])
  
  const getTargetColumnFromDataset = useCallback((datasetId?: string): string => {
    if (!datasetId) return 'target'
    
    const dataset = availableDatasets.find(d => d.datasetId === datasetId)
    if (dataset?.features?.targetColumn) {
      return dataset.features.targetColumn
    }
    
    return 'target'
  }, [availableDatasets])

  const loadInitialData = async () => {
    try {
      // ✅ 修复：使用联邦学习专用接口获取可用水下机器人列表
      const { federatedTask } = await import('@/api/federated-task')
      console.log('🔄 开始获取可用水下机器人列表...')
      
      const vmListResponse = await federatedTask.getAvailableVMs({ 
        status: 'RUNNING'  // 只获取运行中的VM
      })
      
      console.log('📋 水下机器人 API 原始响应:', vmListResponse)
      console.log('📋 响应数据:', vmListResponse?.availableVms)
      console.log('📋 水下机器人总数:', vmListResponse?.total)
      
      // 检查返回数据格式
      if (!vmListResponse || !Array.isArray(vmListResponse.availableVms)) {
        throw new Error(`水下机器人 API 返回数据格式错误: ${JSON.stringify(vmListResponse)}`)
      }
      
      // 检查是否有水下机器人数据
      if (vmListResponse.availableVms.length === 0) {
        console.warn('⚠️ 没有可用水下机器人')
        throw new Error('没有可用的水下机器人，请先添加并启动水下机器人')
      }
      
      // 直接使用后端返回的数据（已经是 AvailableVM 格式）
      setAvailableVMs(vmListResponse.availableVms)
      
      console.log('✅ 成功加载可用水下机器人数据:', vmListResponse.availableVms.length, '台')
      console.log('水下机器人 ID 列表:', vmListResponse.availableVms.map(vm => vm.vmId))
      
      // ✅ 使用联邦学习API获取数据集列表
      console.log('🔄 开始获取数据集列表...')
      
      let datasetsResponse = await federatedTask.getAvailableDatasets({
        status: 'READY'  // 只获取可用的数据集
      })
      
      console.log('📋 数据集API原始响应:', datasetsResponse)
      console.log('📋 availableDatasets字段:', datasetsResponse?.availableDatasets)
      console.log('📋 availableDatasets是否为数组:', Array.isArray(datasetsResponse?.availableDatasets))
      
      if (!datasetsResponse || !Array.isArray(datasetsResponse.availableDatasets)) {
        console.warn('⚠️ 数据集API返回数据格式错误，尝试直接使用响应数据')
        
        // 如果datasetsResponse本身就是数组，直接使用
        if (Array.isArray(datasetsResponse)) {
          console.log('📋 检测到datasetsResponse本身是数组，直接使用')
          datasetsResponse = { 
            total: datasetsResponse.length,
            availableDatasets: datasetsResponse 
          }
        } else {
          throw new Error(`数据集API返回数据格式错误: ${JSON.stringify(datasetsResponse)}`)
        }
      }
      
      // 检查是否有数据集数据
      if (datasetsResponse.availableDatasets.length === 0) {
        console.warn('⚠️ 后端没有可用的数据集，但接口调用成功')
        setAvailableDatasets([])  // 设置空数组，前端显示"暂无数据"
      } else {
        setAvailableDatasets(datasetsResponse.availableDatasets)
      }
      
      console.log('✅ 成功加载真实数据集数据:', datasetsResponse.availableDatasets.length, '个数据集')
      if (datasetsResponse.availableDatasets.length > 0) {
        console.log('数据集 IDs:', datasetsResponse.availableDatasets.map(ds => ds.datasetId))
      }

      // 🔧 修复：使用真实API获取算法模板，而不是模拟数据
      console.log('🔄 开始获取算法模板...')
      
      let algorithmsResponse = await federatedTask.getAlgorithmTemplates()
      
      console.log('📋 算法模板API原始响应:', algorithmsResponse)
      console.log('📋 templates字段:', algorithmsResponse?.templates)
      console.log('📋 templates是否为数组:', Array.isArray(algorithmsResponse?.templates))
      
      if (!algorithmsResponse || !Array.isArray(algorithmsResponse.templates)) {
        console.warn('⚠️ 算法模板API返回数据格式错误，尝试直接使用响应数据')
        
        // 如果algorithmsResponse本身就是数组，直接使用
        if (Array.isArray(algorithmsResponse)) {
          console.log('📋 检测到algorithmsResponse本身是数组，直接使用')
          algorithmsResponse = { 
            templates: algorithmsResponse 
          }
        } else {
          throw new Error(`算法模板API返回数据格式错误: ${JSON.stringify(algorithmsResponse)}`)
        }
      }
      
      // 检查是否有算法模板数据
      if (algorithmsResponse.templates.length === 0) {
        console.warn('⚠️ 后端没有可用的算法模板，但接口调用成功')
        setAlgorithmTemplates([])  // 设置空数组，前端显示"暂无数据"
      } else {
        setAlgorithmTemplates(algorithmsResponse.templates)
      }
      
      console.log('✅ 成功加载真实算法模板数据:', algorithmsResponse.templates.length, '个算法')
      if (algorithmsResponse.templates.length > 0) {
        console.log('算法列表:', algorithmsResponse.templates.map(alg => alg.algorithm))
      }
      
    } catch (error) {
      console.error('❌ 加载数据失败:', error)
      console.error('❌ 错误详情:', {
        message: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
        errorType: typeof error
      })
      
      // API失败时显示空列表
      console.warn('⚠️ 无法获取数据，请检查后端API接口或网络连接')
      setAvailableVMs([])
      setAvailableDatasets([])
      setAlgorithmTemplates([])
      
      message.error('加载初始数据失败，请检查网络连接或联系管理员')
    }
  }

  // 预览数据分配
  const handlePreviewDistribution = useCallback(async () => {
    const values = form.getFieldsValue()
    if (!values.datasetConfig?.datasetId || selectedVMs.length === 0) {
      message.warning('请先选择数据集和参与者')
      return
    }

    setPreviewLoading(true)
    try {
      // 🔧 修复：使用真实API获取数据分配预览
      const { federatedTask } = await import('@/api/federated-task')
      const preview = await federatedTask.previewDataDistribution({
        datasetId: values.datasetConfig.datasetId,
        distributionStrategy: values.datasetConfig.distributionStrategy || 'BALANCED',
        participants: selectedVMs.map(vmId => ({
          vmId,
          requestedRatio: 1 / selectedVMs.length
        }))
      })
      
      setDistributionPreview(preview)
      console.log('✅ 成功获取数据分配预览:', preview)
      message.success('数据分配预览生成成功')
    } catch (error) {
      message.error('生成数据分配预览失败')
    } finally {
      setPreviewLoading(false)
    }
  }, [form, selectedVMs])

  // 验证参与者配置
  const handleValidateParticipants = useCallback(async () => {
    const values = form.getFieldsValue()
    if (!values.algorithm || selectedVMs.length === 0) {
      message.warning('请先选择算法和参与者')
      return
    }

    setValidationLoading(true)
    try {
      // 🔧 修复：使用真实API进行参与者验证
      const { federatedTask } = await import('@/api/federated-task')
      const validation = await federatedTask.validateParticipants({
        algorithm: values.algorithm,
        taskType: values.taskType || 'CLASSIFICATION',
        participants: selectedVMs.map(vmId => ({
          vmId,
          role: 'PARTICIPANT' as const,
          requirements: {
            minCpuCores: 2,
            minMemoryMb: 4096,
            minDiskGb: 10
          }
        }))
      })
      
      setParticipantValidation(validation)
      console.log('✅ 成功完成参与者验证:', validation)
      message.success('参与者验证完成')
    } catch (error) {
      message.error('参与者验证失败')
    } finally {
      setValidationLoading(false)
    }
  }, [form, selectedVMs])

  // 处理算法变更
  const handleAlgorithmChange = useCallback((algorithm: string) => {
    const template = algorithmTemplates.find(t => t.algorithm === algorithm)
    if (template) {
      form.setFieldsValue({
        hyperparameters: template.defaultHyperparameters
      })
    }
  }, [form, algorithmTemplates])

  // 处理分配策略变化
  const handleDistributionStrategyChange = useCallback((strategy: string) => {
    setDistributionStrategy(strategy)
    
    // 如果是均衡分配，自动设置均等占比
    if (strategy === 'BALANCED' && selectedVMs.length > 0) {
      const equalRatio = parseFloat((100 / selectedVMs.length).toFixed(1))
      const participants = selectedVMs.map((vmId) => ({
        vmId,
        role: 'PARTICIPANT' as const,
        dataRatio: equalRatio,
        capabilities: [],
        constraints: {
          maxCpuUsage: 80,
          maxMemoryUsage: 75
        }
      }))
      
      form.setFieldsValue({
        participantConfig: {
          ...form.getFieldValue('participantConfig'),
          participants
        }
      })
    }
  }, [form, selectedVMs])

  // 处理测试集比例变化
  const handleTestSplitChange = useCallback((value: number | null) => {
    if (value !== null && value >= 1 && value <= 99) {
      setTestSplit(value)
      const newTrainSplit = parseFloat((100 - value).toFixed(1))
      setTrainSplit(newTrainSplit)
      // 同步更新表单中的训练集比例
      form.setFieldValue(['datasetConfig', 'trainSplit'], newTrainSplit)
    }
  }, [form])

  // 处理训练集比例变化
  const handleTrainSplitChange = useCallback((value: number | null) => {
    if (value !== null && value >= 1 && value <= 99) {
      setTrainSplit(value)
      const newTestSplit = parseFloat((100 - value).toFixed(1))
      setTestSplit(newTestSplit)
      // 同步更新表单中的测试集比例
      form.setFieldValue(['datasetConfig', 'testSplit'], newTestSplit)
    }
  }, [form])

  // 处理水下机器人选择
  const handleVMSelection = useCallback((selectedRowKeys: React.Key[]) => {
    setSelectedVMs(selectedRowKeys as string[])
    
    // 自动设置参与者配置
    const currentStrategy = form.getFieldValue(['datasetConfig', 'distributionStrategy']) || 'BALANCED'
    const equalRatio = selectedRowKeys.length > 0 ? parseFloat((100 / selectedRowKeys.length).toFixed(1)) : 0
    
    const participants = selectedRowKeys.map((vmId) => ({
      vmId: vmId as string,
      role: 'PARTICIPANT' as const,
      dataRatio: equalRatio,
      capabilities: [],
      constraints: {
        maxCpuUsage: 80,
        maxMemoryUsage: 75
      }
    }))
    
    form.setFieldsValue({
      participantConfig: {
        ...form.getFieldValue('participantConfig'),
        participants
      }
    })
  }, [form])

  // 验证数据占比总和
  const validateDataRatioSum = useCallback(() => {
    const participants = form.getFieldValue(['participantConfig', 'participants']) || []
    const totalRatio = participants.reduce((sum: number, p: any) => sum + (p.dataRatio || 0), 0)
    
    // 对于均衡分配，允许更大的误差范围（因为小数精度问题）
    const currentStrategy = form.getFieldValue(['datasetConfig', 'distributionStrategy']) || 'BALANCED'
    const tolerance = currentStrategy === 'BALANCED' ? 1.0 : 0.01 // 均衡分配允许1%误差，自定义分配要求更精确
    
    if (Math.abs(totalRatio - 100) > tolerance) {
      message.error(`水下机器人数据占比总和应为100%，当前为${totalRatio.toFixed(1)}%`)
      return false
    }
    return true
  }, [form])

  // 提交表单
  const handleSubmit = useCallback(async () => {
    try {
      console.log('🚀 开始创建任务流程...')
      console.log('当前步骤:', currentStep)
      console.log('选中的水下机器人列表:', selectedVMs)
      
      // 立即获取当前表单状态进行预检查
      const preCheckValues = form.getFieldsValue()
      console.log('📋 预检查表单数据:', JSON.stringify(preCheckValues, null, 2))
      
      // 先验证表单，但主要使用getFieldValue逐个获取数据
      console.log('✅ 开始表单验证...')
      await form.validateFields()
      console.log('✅ 表单验证完成')
      
      // 验证数据占比总和
      if (!validateDataRatioSum()) {
        return
      }
      
      // 由于分步表单的限制，使用getFieldValue逐个获取字段值
      const values = {
        // 基本信息
        taskName: form.getFieldValue('taskName'),
        taskType: form.getFieldValue('taskType'),
        description: form.getFieldValue('description'),
        algorithm: form.getFieldValue('algorithm'),
        
        // 数据集配置
        datasetConfig: {
          ...(form.getFieldValue('datasetConfig') || {
            distributionStrategy: 'BALANCED',
            testSplit: 20,
            trainSplit: 80
          }),
          // 将百分比转换为小数
          testSplit: (form.getFieldValue(['datasetConfig', 'testSplit']) || 20) / 100
          // 注意：trainSplit不需要发送给后端，因为后端只需要testSplit
        },
        
        // 参与者配置
        participantConfig: {
          ...(form.getFieldValue('participantConfig') || {
            selectionMode: 'MANUAL',
            participants: []
          }),
          // 将参与者数据占比从百分比转换为小数
          participants: (form.getFieldValue(['participantConfig', 'participants']) || []).map((p: any) => ({
            ...p,
            dataRatio: (p.dataRatio || 0) / 100
          }))
        },
        
        // 超参数配置
        hyperparameters: form.getFieldValue('hyperparameters') || {
          learningRate: 0.01,
          batchSize: 32,
          epochs: 10,
          rounds: 20,
          minParticipants: 2
        },
        
        // 模型配置
        modelConfig: form.getFieldValue('modelConfig') || {
          modelType: 'RANDOM_FOREST',
          testSize: 0.2,
          randomState: 42
        },
        
        // 调度配置
        schedule: form.getFieldValue('schedule') || {
          timeout: 3600
        }
      }
      
      console.log('🔧 使用getFieldValue重构后的数据:', JSON.stringify(values, null, 2))
      
      // 验证必须选择参与者 - 同时检查UI状态和表单数据
      const hasSelectedVMs = selectedVMs.length > 0
      const hasParticipantsInForm = values.participantConfig?.participants && values.participantConfig.participants.length > 0
      
      if (!hasSelectedVMs && !hasParticipantsInForm) {
        message.error('请至少选择一个参与者水下机器人')
        setCurrentStep(1) // 跳转到参与者配置步骤
        return
      }
      
      // 确保表单数据与UI选择状态同步
      if (hasSelectedVMs) {
        const participants = selectedVMs.map(vmId => ({
          vmId,
          role: 'PARTICIPANT' as const,
          dataRatio: parseFloat((100 / selectedVMs.length).toFixed(1)),
          capabilities: [],
          constraints: {
            maxCpuUsage: 80,
            maxMemoryUsage: 75
          }
        }))
        
        form.setFieldsValue({
          participantConfig: {
            ...form.getFieldValue('participantConfig'),
            participants
          }
        })
      }
      
      // 构建分配比例数据
      const distributionRatios: Record<string, number> = {}
      selectedVMs.forEach(vmId => {
        distributionRatios[vmId] = 1 / selectedVMs.length
      })
      
      // 验证关键字段（现在应该都有值了）
      console.log('=== 字段验证 ===')
      console.log('taskName:', `"${values.taskName}"`)
      console.log('taskType:', `"${values.taskType}"`)
      console.log('algorithm:', `"${values.algorithm}"`)
      
      const finalTaskName = values.taskName
      const finalTaskType = values.taskType
      const finalAlgorithm = values.algorithm
      
      // 验证关键字段
      if (!finalTaskName || finalTaskName.trim().length === 0) {
        console.log('❌ 任务名称验证失败')
        message.error('任务名称不能为空，请检查基本信息配置')
        setCurrentStep(0) // 跳转到基本信息步骤
        return
      }
      
      console.log('✅ 任务名称验证通过')
      
      // 验证任务类型
      if (!finalTaskType) {
        console.log('❌ 任务类型验证失败')
        message.error('请选择任务类型')
        setCurrentStep(0) // 跳转到基本信息步骤
        return
      }
      
      console.log('✅ 任务类型验证通过')
      
      if (!finalAlgorithm) {
        console.log('❌ 算法验证失败')
        message.error('请选择算法')
        setCurrentStep(3) // 跳转到算法配置步骤
        return
      }
      
      console.log('✅ 算法验证通过')
      
      // 构建最终数据，使用回退机制确保关键字段存在
      const finalValues = {
        ...values,
        taskName: finalTaskName,
        taskType: finalTaskType,
        algorithm: finalAlgorithm
      }
      
      console.log('5. 最终提交数据:', JSON.stringify(finalValues, null, 2))
      
      // 构建参与者数据 - 同时支持v1.0和v1.3格式的兼容性要求
      let participants = finalValues.participantConfig?.participants || []
      
      // 如果表单中没有参与者数据，但UI中有选中的VMs，则使用selectedVMs构建
      if (participants.length === 0 && selectedVMs.length > 0) {
        console.log('🔄 表单中无参与者数据，使用选中的水下机器人构建:', selectedVMs)
        participants = selectedVMs.map(vmId => ({
          vmId,
          role: 'PARTICIPANT' as const,
          dataRatio: parseFloat((100 / selectedVMs.length).toFixed(1)),
          capabilities: [],
          constraints: {
            maxCpuUsage: 80,
            maxMemoryUsage: 75
          },
          // 🔄 兼容旧格式：添加 dataSource 字段
          dataSource: 'federated_dataset'
        }))
      } else {
        // 确保参与者数据格式正确，并添加兼容字段
        participants = participants.map(p => ({
          ...p,
          role: 'PARTICIPANT' as const,
          // 🔄 兼容旧格式：确保有 dataSource 字段
          dataSource: p.dataSource || 'federated_dataset'
        }))
      }
      
      console.log('📋 构建的参与者数据（含兼容字段）:', participants)
      console.log('📊 参与者数量:', participants.length)
      
      // 显示兼容性数据结构
      const legacyParticipants = participants.map(p => ({
        vmId: p.vmId,
        role: p.role,
        dataSource: p.dataSource || 'federated_dataset'
      }))
      console.log('🔄 旧格式兼容数据:', legacyParticipants)
      console.log('🆕 新格式完整数据:', participants)
      
      // 调试特征列获取
      const selectedDatasetId = finalValues.datasetConfig?.datasetId
      const selectedDataset = availableDatasets.find(d => d.datasetId === selectedDatasetId)
      console.log('📊 数据集信息调试:')
      console.log('  - 选中的数据集ID:', selectedDatasetId)
      console.log('  - 找到的数据集:', selectedDataset?.name)
      console.log('  - 数据集特征列:', selectedDataset?.features?.featureColumns)
      console.log('  - 数据集目标列:', selectedDataset?.features?.targetColumn)
      console.log('  - 最终使用的特征列:', getFeatureColumnsFromDataset(selectedDatasetId))
      console.log('  - 最终使用的目标列:', getTargetColumnFromDataset(selectedDatasetId))
      
      // 按照 modified-interfaces-v1.3.md 要求，同时发送新旧格式以确保兼容性
      const result = await createTask({
        ...finalValues,
        
        // 🔄 兼容旧格式：直接提供 participants 数组（v1.0格式）
        participants: participants.map(p => ({
          vmId: p.vmId,
          role: p.role,
          dataSource: p.dataSource || 'federated_dataset'  // 旧格式必需字段
        })),
        
        datasetConfig: {
          ...finalValues.datasetConfig,
          distributionRatios
        },
        modelConfig: {
          ...finalValues.modelConfig,
          featureColumns: finalValues.modelConfig?.featureColumns || getFeatureColumnsFromDataset(finalValues.datasetConfig?.datasetId),
          targetColumn: finalValues.modelConfig?.targetColumn || getTargetColumnFromDataset(finalValues.datasetConfig?.datasetId)
        },
        
        // 🆕 新格式：推荐的 participantConfig 结构（v1.3格式）
        participantConfig: {
          ...finalValues.participantConfig,
          participants: participants
        }
      })
      
      if (result.success) {
        message.success('任务创建成功')
        navigate(`/federated-learning/tasks/${result.data}`)
      } else {
        message.error(result.error || '任务创建失败')
      }
    } catch (error) {
      console.error('❌ 创建任务过程中出错:', error)
      
      // 显示详细错误信息
      const errorInfo = `
创建任务失败：

错误信息: ${error instanceof Error ? error.message : String(error)}

当前状态:
- 当前步骤: ${currentStep}
- 选中VMs: ${selectedVMs.length}个
- 表单字段状态: ${JSON.stringify(form.getFieldsValue(), null, 2)}
      `.trim()
      
      Modal.error({
        title: '创建任务失败',
        content: (
          <div style={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: '12px' }}>
            {errorInfo}
          </div>
        ),
        width: 800
      })
      
      message.error('创建任务失败，请查看详细信息')
    }
  }, [form, createTask, navigate, selectedVMs, currentStep])

  // 水下机器人表格列定义
  const vmColumns = [
    {
      title: '水下机器人名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      ellipsis: true
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 120
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: string) => (
        <Tag color={status === 'RUNNING' ? 'success' : 'default'}>
          {status === 'RUNNING' ? '运行中' : status}
        </Tag>
      )
    },
    {
      title: '资源配置',
      key: 'resources',
      width: 150,
      render: (_, record: AvailableVM) => (
        <div>
          <div>CPU: {record.resources.cpuCores}核</div>
          <div>内存: {Math.floor(record.resources.memoryMb / 1024)}GB</div>
          {record.resources.gpuCount > 0 && (
            <div>GPU: {record.resources.gpuCount}个</div>
          )}
        </div>
      )
    },
    {
      title: '能力',
      dataIndex: 'capabilities',
      key: 'capabilities',
      render: (capabilities: string[]) => (
        <Space wrap>
          {capabilities.map(cap => (
            <Tag key={cap}>{cap}</Tag>
          ))}
        </Space>
      )
    }
  ]

  // 步骤配置
  const steps = [
    {
      title: '基本信息',
      description: '配置任务基本信息'
    },
    {
      title: '参与者配置',
      description: '选择参与的水下机器人'
    },
    {
      title: '数据集配置',
      description: '选择和配置数据集'
    },
    {
      title: '算法配置',
      description: '配置训练参数'
    },
    {
      title: '确认创建',
      description: '确认配置并创建任务'
    }
  ]

  return (
    <div className="task-create-page">
      {/* 页面头部 */}
      <Card className="header-card" bordered={false}>
        <div className="page-header">
          <div className="header-left">
            <Button 
              type="text" 
              icon={<ArrowLeftOutlined />} 
              onClick={() => navigate('/federated-learning/tasks')}
            >
              返回列表
            </Button>
            <div>
              <Title level={2} style={{ margin: 0 }}>创建任务</Title>
              <Paragraph style={{ margin: '8px 0 0 0', color: '#8c8c8c' }}>
                通过图形化界面配置和创建任务
              </Paragraph>
            </div>
          </div>
        </div>
      </Card>

      {/* 步骤指示器 */}
      <Card style={{ marginBottom: 16 }}>
        <Steps current={currentStep} size="small">
          {steps.map((step, index) => (
            <Step key={index} title={step.title} description={step.description} />
          ))}
        </Steps>
      </Card>

      {/* 表单内容 */}
      <Card>
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            taskName: '',
            taskType: 'CLASSIFICATION',
            algorithm: '',
            datasetConfig: {
              distributionStrategy: 'BALANCED',
              testSplit: 20,
              trainSplit: 80
            },
            participantConfig: {
              selectionMode: 'MANUAL',
              requirements: {
                minParticipants: 2,
                maxParticipants: 10,
                minCpuCores: 4,
                minMemoryMb: 8192
              },
              participants: []
            },
            hyperparameters: {
              learningRate: 0.01,
              batchSize: 32,
              epochs: 10,
              rounds: 20,
              minParticipants: 2
            },
            modelConfig: {
              modelType: 'RANDOM_FOREST',
              testSize: 0.2,
              randomState: 42
            },
            schedule: {
              timeout: 3600
            }
          }}
        >
          {/* 步骤1: 基本信息 */}
          {currentStep === 0 && (
            <div className="step-content">
              <Title level={4}>基本信息配置</Title>
              <Row gutter={[24, 16]}>
                <Col xs={24} md={12}>
                  <Form.Item
                    name="taskName"
                    label="任务名称"
                    rules={[{ required: true, message: '请输入任务名称' }]}
                  >
                    <Input placeholder="请输入任务名称" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name="taskType"
                    label="任务类型"
                    rules={[{ required: true, message: '请选择任务类型' }]}
                  >
                    <Select placeholder="请选择任务类型">
                      <Option value="CLASSIFICATION">分类任务</Option>
                      <Option value="REGRESSION">回归任务</Option>
                      <Option value="CLUSTERING">聚类任务</Option>
                      <Option value="ANOMALY_DETECTION">异常检测</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col xs={24}>
                  <Form.Item
                    name="description"
                    label="任务描述"
                  >
                    <TextArea 
                      rows={4} 
                      placeholder="请输入任务描述（可选）"
                      showCount
                      maxLength={500}
                    />
                  </Form.Item>
                </Col>
              </Row>
            </div>
          )}

          {/* 步骤2: 参与者配置 */}
          {currentStep === 1 && (
            <div className="step-content">
              <Title level={4}>参与者配置</Title>
              <div style={{ marginBottom: 16 }}>
                <Alert
                  message="选择参与训练的水下机器人"
                  description="请从下列可用的水下机器人中选择参与任务的节点"
                  type="info"
                  showIcon
                />
              </div>
              
              <Table
                columns={vmColumns}
                dataSource={availableVMs}
                rowKey="vmId"
                size="small"
                rowSelection={{
                  type: 'checkbox',
                  selectedRowKeys: selectedVMs,
                  onChange: handleVMSelection,
                  getCheckboxProps: (record) => ({
                    disabled: record.status !== 'RUNNING'
                  })
                }}
                pagination={false}
                scroll={{ x: 800 }}
              />
              
              {selectedVMs.length > 0 && (
                <>
                  <Divider>参与者验证</Divider>
                  <div style={{ marginBottom: 16 }}>
                    <Button 
                      type="primary" 
                      icon={<CheckCircleOutlined />}
                      onClick={handleValidateParticipants}
                      loading={validationLoading}
                    >
                      验证参与者配置
                    </Button>
                  </div>
                  
                  {participantValidation && (
                    <Alert
                      message="参与者验证通过"
                      description={
                        <div>
                          <div>验证通过的参与者: {participantValidation.participantValidations.filter(p => p.isValid).length}</div>
                          <div>总参与者数量: {participantValidation.participantValidations.length}</div>
                        </div>
                      }
                      type="success"
                      showIcon
                    />
                  )}
                </>
              )}
            </div>
          )}

          {/* 步骤3: 数据集配置 */}
          {currentStep === 2 && (
            <div className="step-content">
              <Title level={4}>数据集配置</Title>
              
              {/* 数据集选择 */}
              <Row gutter={[24, 16]}>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['datasetConfig', 'datasetId']}
                    label="选择数据集"
                    rules={[{ required: true, message: '请选择数据集' }]}
                  >
                    <Select placeholder="请选择数据集">
                      {availableDatasets.map(dataset => (
                        <Option key={dataset.datasetId} value={dataset.datasetId}>
                          <div>
                            <div>{dataset.name}</div>
                            <div style={{ fontSize: '12px', color: '#8c8c8c' }}>
                              {dataset.statistics.totalRows}行 × {dataset.statistics.totalColumns}列 
                              ({dataset.statistics.fileSizeFormatted})
                            </div>
                          </div>
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['datasetConfig', 'distributionStrategy']}
                    label="分配策略"
                    rules={[{ required: true, message: '请选择分配策略' }]}
                  >
                    <Select 
                      placeholder="请选择分配策略"
                      onChange={handleDistributionStrategyChange}
                    >
                      <Option value="BALANCED">均衡分配</Option>
                      <Option value="CUSTOM">自定义分配</Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>

              {/* 参与者数据分配 */}
              {selectedVMs.length > 0 && (
                <>
                  <Divider>参与者数据分配</Divider>
                  <div style={{ marginBottom: 16 }}>
                    <Alert
                      message="配置每个参与者的数据占比"
                      description={`已选择 ${selectedVMs.length} 个参与者，请为每个参与者分配数据占比（总和应为100%）`}
                      type="info"
                      showIcon
                    />
                  </div>
                  
                  <Row gutter={[16, 16]}>
                    {selectedVMs.map((vmId, index) => {
                      const vm = availableVMs.find(v => v.vmId === vmId)
                      return (
                        <Col xs={24} md={12} key={vmId}>
                          <Card size="small" style={{ marginBottom: 8 }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <div>
                                <div style={{ fontWeight: 'bold' }}>{vm?.name || vmId}</div>
                                <div style={{ fontSize: '12px', color: '#8c8c8c' }}>
                                  {vm?.resources.cpuCores}核 | {vm?.resources.memoryMb}MB | {vm?.capabilities.join(', ')}
                                </div>
                              </div>
                              <Form.Item
                                name={['participantConfig', 'participants', index, 'dataRatio']}
                                style={{ margin: 0, width: 120 }}
                                rules={[
                                  { required: true, message: '请输入占比' },
                                  { type: 'number', min: 0.1, max: 100, message: '占比范围：0.1%-100%' }
                                ]}
                              >
                                <InputNumber
                                  min={0.1}
                                  max={100}
                                  step={0.1}
                                  precision={1}
                                  placeholder="33.3"
                                  addonAfter="%"
                                  style={{ width: '100%' }}
                                  disabled={distributionStrategy === 'BALANCED'}
                                />
                              </Form.Item>
                            </div>
                          </Card>
                        </Col>
                      )
                    })}
                  </Row>
                </>
              )}

              {/* 数据集划分配置 */}
              <Divider>全局数据集划分</Divider>
              <Row gutter={[24, 16]}>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['datasetConfig', 'testSplit']}
                    label="测试集比例"
                    rules={[
                      { required: true, message: '请输入测试集比例' },
                      { type: 'number', min: 1, max: 50, message: '测试集比例范围：1%-50%' }
                    ]}
                  >
                    <InputNumber 
                      min={1} 
                      max={50} 
                      step={0.1} 
                      precision={1}
                      placeholder="20.0"
                      addonAfter="%"
                      style={{ width: '100%' }}
                      onChange={handleTestSplitChange}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['datasetConfig', 'trainSplit']}
                    label="训练集比例"
                    rules={[
                      { required: true, message: '请输入训练集比例' },
                      { type: 'number', min: 50, max: 99, message: '训练集比例范围：50%-99%' }
                    ]}
                  >
                    <InputNumber 
                      min={50} 
                      max={99} 
                      step={0.1} 
                      precision={1}
                      placeholder="80.0"
                      addonAfter="%"
                      style={{ width: '100%' }}
                      onChange={handleTrainSplitChange}
                    />
                  </Form.Item>
                </Col>
              </Row>

              {/* 数据分配预览 */}
              <Divider>数据分配预览</Divider>
              <div style={{ marginBottom: 16 }}>
                <Button 
                  type="primary" 
                  icon={<EyeOutlined />}
                  onClick={handlePreviewDistribution}
                  loading={previewLoading}
                >
                  预览数据分配
                </Button>
              </div>
              
              {distributionPreview && (
                <Alert
                  message="数据分配预览"
                  description={
                    <div>
                      <div>IID评分: {(distributionPreview.qualityMetrics.iidScore * 100).toFixed(1)}%</div>
                      <div>平衡评分: {(distributionPreview.qualityMetrics.balanceScore * 100).toFixed(1)}%</div>
                      <div style={{ marginTop: 8 }}>
                        {distributionPreview.distributionResult.participants.map(p => (
                          <div key={p.vmId} style={{ fontSize: '12px' }}>
                            {p.vmId}: {(p.allocatedRatio * 100).toFixed(1)}% ({p.allocatedRows}行)
                          </div>
                        ))}
                      </div>
                    </div>
                  }
                  type="info"
                  showIcon
                />
              )}
            </div>
          )}

          {/* 步骤4: 算法配置 */}
          {currentStep === 3 && (
            <div className="step-content">
              <Title level={4}>算法配置</Title>
              <Row gutter={[24, 16]}>
                <Col xs={24}>
                  <Form.Item
                    name="algorithm"
                    label="选择算法"
                    rules={[{ required: true, message: '请选择算法' }]}
                  >
                    <Select 
                      placeholder="请选择算法"
                      onChange={handleAlgorithmChange}
                    >
                      {algorithmTemplates.map(template => (
                        <Option key={template.algorithm} value={template.algorithm}>
                          <div>
                            <div>{template.name}</div>
                            <div style={{ fontSize: '12px', color: '#8c8c8c' }}>
                              {template.description}
                            </div>
                          </div>
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
              </Row>
              
              <Divider>超参数配置</Divider>
              <Row gutter={[24, 16]}>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['hyperparameters', 'learningRate']}
                    label="学习率"
                    rules={[{ required: true, message: '请输入学习率' }]}
                  >
                    <InputNumber 
                      min={0.0001} 
                      max={1} 
                      step={0.001} 
                      placeholder="0.01"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['hyperparameters', 'batchSize']}
                    label="批次大小"
                    rules={[{ required: true, message: '请输入批次大小' }]}
                  >
                    <InputNumber 
                      min={1} 
                      max={1024} 
                      step={1} 
                      placeholder="32"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['hyperparameters', 'epochs']}
                    label="本地训练轮次"
                    rules={[{ required: true, message: '请输入本地训练轮次' }]}
                  >
                    <InputNumber 
                      min={1} 
                      max={1000} 
                      step={1} 
                      placeholder="10"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['hyperparameters', 'rounds']}
                    label="训练轮次"
                    rules={[{ required: true, message: '请输入训练轮次' }]}
                  >
                    <InputNumber 
                      min={1} 
                      max={100} 
                      step={1} 
                      placeholder="20"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['hyperparameters', 'minParticipants']}
                    label="最小参与者数量"
                    rules={[{ required: true, message: '请输入最小参与者数量' }]}
                  >
                    <InputNumber 
                      min={1} 
                      max={selectedVMs.length || 100} 
                      step={1} 
                      placeholder="2"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                </Col>
              </Row>
              
              <Divider>模型配置</Divider>
              <Row gutter={[24, 16]}>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['modelConfig', 'modelType']}
                    label="模型类型"
                    rules={[{ required: true, message: '请选择模型类型' }]}
                  >
                    <Select placeholder="请选择模型类型">
                      <Option value="RANDOM_FOREST">随机森林</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['modelConfig', 'testSize']}
                    label="本地模型测试集比例"
                    rules={[{ required: true, message: '请输入本地模型测试集比例' }]}
                    tooltip="用于每个参与者本地模型训练时的数据划分，与前面的全局数据集划分不同"
                  >
                    <InputNumber 
                      min={0.1} 
                      max={0.5} 
                      step={0.05} 
                      placeholder="0.2"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name={['modelConfig', 'randomState']}
                    label="随机种子"
                    rules={[{ required: true, message: '请输入随机种子' }]}
                    tooltip="用于保证模型训练结果的可重现性，相同的随机种子会产生相同的结果"
                  >
                    <InputNumber 
                      min={0} 
                      max={9999} 
                      step={1} 
                      placeholder="42"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                </Col>
              </Row>
            </div>
          )}

          {/* 步骤5: 确认创建 */}
          {currentStep === 4 && (
            <div className="step-content">
              <Title level={4}>确认任务配置</Title>
              <Alert
                message="请确认以下配置信息无误后创建任务"
                type="info"
                showIcon
                style={{ marginBottom: 24 }}
              />
              
              {/* 配置摘要 */}
              <Card title="配置摘要" size="small" style={{ marginBottom: 16 }}>
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={12}>
                    <div><strong>任务名称:</strong> {form.getFieldValue('taskName')}</div>
                    <div><strong>任务类型:</strong> {form.getFieldValue('taskType')}</div>
                    <div><strong>算法:</strong> {form.getFieldValue('algorithm')}</div>
                    <div><strong>数据集:</strong> {form.getFieldValue(['datasetConfig', 'datasetId'])}</div>
                  </Col>
                  <Col xs={24} md={12}>
                    <div><strong>参与者数量:</strong> {selectedVMs.length}个</div>
                    <div><strong>训练轮次:</strong> {form.getFieldValue(['hyperparameters', 'rounds'])}轮</div>
                    <div><strong>本地轮次:</strong> {form.getFieldValue(['hyperparameters', 'epochs'])}轮</div>
                    <div><strong>学习率:</strong> {form.getFieldValue(['hyperparameters', 'learningRate'])}</div>
                  </Col>
                </Row>
              </Card>
              
              {/* 创建按钮 */}
              <div style={{ textAlign: 'center' }}>
                <Button 
                  type="primary" 
                  size="large"
                  icon={<PlayCircleOutlined />}
                  onClick={handleSubmit}
                  loading={createTaskLoading}
                >
                  创建任务
                </Button>
              </div>
            </div>
          )}
        </Form>

        {/* 步骤导航 */}
        <div className="step-navigation">
          <Space>
            <Button 
              disabled={currentStep === 0}
              onClick={() => setCurrentStep(currentStep - 1)}
            >
              上一步
            </Button>
            <Button 
              type="primary"
              disabled={currentStep === 4}
              onClick={() => setCurrentStep(currentStep + 1)}
            >
              下一步
            </Button>
          </Space>
        </div>
      </Card>
    </div>
  )
}

export default TaskCreatePage
