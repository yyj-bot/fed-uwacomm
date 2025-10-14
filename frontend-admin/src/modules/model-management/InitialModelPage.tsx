/**
 * 初始模型管理页面
 * 覆盖initial-model-api-reference.md所有接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { 
  Card, 
  Button, 
  Table, 
  Space, 
  Modal, 
  Form, 
  Input, 
  Select, 
  Upload, 
  message, 
  Tag, 
  Progress, 
  Descriptions, 
  Statistic,
  Row,
  Col,
  InputNumber,
  Switch,
  Tooltip,
  Popconfirm,
  Alert,
  Spin,
  Checkbox
} from 'antd'
import {
  PlusOutlined,
  UploadOutlined,
  DownloadOutlined,
  DeleteOutlined,
  SendOutlined,
  ReloadOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  SearchOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useModel } from '@/store/model-version/useModelVersionStore'
import { federatedTaskService } from '@/services/federated-task'
import type { 
  InitialModelGenerationRequest,
  InitialModelUploadRequest,
  ModelDistributionRequest,
  InitialModelInfo,
  DistributionStatusDetail
} from '@/services'
import type { FederatedTask } from '@/types'

const { TextArea } = Input
const { Option } = Select

const InitialModelPage: React.FC = () => {
  const {
    // 状态
    initialModels,
    initialModelLoading,
    initialModelError,
    generationLoading,
    generationError,
    initialUploadLoading,
    initialUploadError,
    distributions,
    distributionLoading,
    
    // 操作方法
    generateInitialModel,
    uploadCustomInitialModel,
    fetchTaskInitialModel,
    distributeInitialModel,
    fetchDistributionStatus,
    downloadInitialModel,
    deleteInitialModel,
    
    // 工具方法
    getTaskInitialModel,
    isTaskInitialModelLoading,
    getDistributionStatus,
    isDistributing,
    getTaskInfo,
    canDistributeInitialModel,
    canDeleteInitialModel
  } = useModel()

  const [generateModalVisible, setGenerateModalVisible] = useState(false)
  const [uploadModalVisible, setUploadModalVisible] = useState(false)
  const [distributeModalVisible, setDistributeModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [distributionDetailModalVisible, setDistributionDetailModalVisible] = useState(false)
  
  const [selectedTaskId, setSelectedTaskId] = useState<string>('')
  const [selectedDistributionId, setSelectedDistributionId] = useState<string>('')
  const [fileList, setFileList] = useState<any[]>([])
  const [queryTaskId, setQueryTaskId] = useState<string>('')
  
  // 新增：模型类型选择状态
  const [selectedModelType, setSelectedModelType] = useState<string>('RANDOM_FOREST')
  const [selectedUploadModelType, setSelectedUploadModelType] = useState<string>('RANDOM_FOREST')
  
  // JSON验证状态
  const [jsonValidationStatus, setJsonValidationStatus] = useState<'success' | 'error' | ''>('')
  const [jsonValidationMessage, setJsonValidationMessage] = useState<string>('')
  
  // 联邦学习任务相关状态
  const [federatedTasks, setFederatedTasks] = useState<FederatedTask[]>([])
  const [tasksLoading, setTasksLoading] = useState(false)
  const [taskSelectModalVisible, setTaskSelectModalVisible] = useState(false)
  
  // 可用虚拟机列表
  const [availableVms, setAvailableVms] = useState<any[]>([])
  const [vmsLoading, setVmsLoading] = useState(false)
  
  const [generateForm] = Form.useForm()
  const [uploadForm] = Form.useForm()
  const [distributeForm] = Form.useForm()

  // 模型列表数据 - 显示所有任务的初始模型
  const modelListData = Object.entries(initialModels)
    .map(([taskId, model]) => ({
      key: taskId,
      taskId: taskId,
      ...model
    }))
    .filter(model => {
      // 如果有搜索关键词，则进行筛选
      if (!queryTaskId.trim()) return true
      const searchKey = queryTaskId.toLowerCase()
      return (
        model.taskId?.toLowerCase().includes(searchKey) ||
        model.modelId?.toLowerCase().includes(searchKey)
      )
    })


  // 获取联邦学习任务列表
  const loadFederatedTasks = async () => {
    try {
      setTasksLoading(true)
      const result = await federatedTaskService.getTaskList({
        page: 1,
        size: 100 // 获取前100个任务
      })
      setFederatedTasks(result.tasks)
    } catch (error) {
      console.error('获取联邦学习任务列表失败:', error)
      message.error('获取任务列表失败')
    } finally {
      setTasksLoading(false)
    }
  }

  // 加载可用虚拟机列表
  const loadAvailableVms = async () => {
    setVmsLoading(true)
    try {
      console.log('🔄 开始加载虚拟机列表...')
      const response = await federatedTaskService.getAvailableVMs({
        status: 'RUNNING'
      })
      console.log('📋 虚拟机API响应:', response)
      setAvailableVms(response.availableVms || [])
      console.log('✅ 设置虚拟机列表:', response.availableVms || [])
    } catch (error) {
      console.error('❌ 获取可用虚拟机失败:', error)
      setAvailableVms([])
      message.error('获取可用虚拟机失败，请检查网络连接或联系管理员')
    } finally {
      setVmsLoading(false)
    }
  }

  // 组件挂载时加载任务列表并自动获取所有任务的初始模型
  useEffect(() => {
    loadFederatedTasks()
    loadAllInitialModels()
  }, [])

  // 加载所有任务的初始模型
  const loadAllInitialModels = async () => {
    try {
      const result = await federatedTaskService.getTaskList({
        page: 1,
        size: 100 // 获取前100个任务
      })
      
      // 并发获取所有任务的初始模型
      const promises = result.tasks.map(task => 
        fetchTaskInitialModel(task.taskId).catch(err => {
          console.warn(`获取任务 ${task.taskId} 的初始模型失败:`, err)
          return null
        })
      )
      
      await Promise.all(promises)
    } catch (error) {
      console.error('加载初始模型列表失败:', error)
      message.error('加载初始模型列表失败')
    }
  }

  // 查询初始模型
  const handleQueryModel = async () => {
    if (!queryTaskId.trim()) {
      message.warning('请输入任务ID')
      return
    }
    await fetchTaskInitialModel(queryTaskId.trim())
  }

  // 从任务列表选择任务
  const handleSelectFromTasks = () => {
    setTaskSelectModalVisible(true)
  }

  // 选择任务并查询初始模型
  const handleTaskSelected = async (taskId: string) => {
    setQueryTaskId(taskId)
    setTaskSelectModalVisible(false)
    await fetchTaskInitialModel(taskId)
  }

  // 初始模型状态颜色
  const getStatusColor = (status: string) => {
    const colorMap: Record<string, string> = {
      'GENERATING': 'processing',
      'READY': 'success',
      'UPLOADED': 'success',
      'DISTRIBUTING': 'processing',
      'DISTRIBUTED': 'success',
      'FAILED': 'error',
      'DELETED': 'default'
    }
    return colorMap[status] || 'default'
  }

  // 分发状态颜色
  const getDistributionStatusColor = (status: string) => {
    const colorMap: Record<string, string> = {
      'PENDING': 'default',
      'IN_PROGRESS': 'processing',
      'COMPLETED': 'success',
      'FAILED': 'error',
      'CANCELLED': 'warning'
    }
    return colorMap[status] || 'default'
  }

  // 初始模型表格列定义
  const modelColumns: ColumnsType<any> = [
    {
      title: '任务ID',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 200,
      ellipsis: true
    },
    {
      title: '模型ID',
      dataIndex: 'modelId',
      key: 'modelId',
      width: 200,
      ellipsis: true
    },
    {
      title: '模型类型',
      dataIndex: 'modelType',
      key: 'modelType',
      width: 120
    },
    {
      title: '模型大小',
      dataIndex: 'modelSize',
      key: 'modelSize',
      width: 120,
      render: (size: number) => `${(size / 1024 / 1024).toFixed(2)} MB`
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>{status}</Tag>
      )
    },
    {
      title: '分发状态',
      key: 'distribution',
      width: 150,
      render: (_, record) => (
        <Space direction="vertical" size="small">
          <div>总VM: {record.distributionStatus?.totalVms || 0}</div>
          <div>已分发: {record.distributionStatus?.distributedVms || 0}</div>
          <div>失败: {record.distributionStatus?.failedVms || 0}</div>
        </Space>
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (time: string) => new Date(time).toLocaleString()
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 250,
      render: (_, record) => (
        <Space size="small" wrap>
          <Button
            type="link"
            size="small"
            icon={<FileTextOutlined />}
            onClick={() => handleViewDetail(record.taskId)}
          >
            详情
          </Button>
          <Tooltip title={getDistributeTooltip(record.taskId)}>
            <Button
              type="link"
              size="small"
              icon={<SendOutlined />}
              disabled={!canDistributeInitialModel(record.taskId)}
              onClick={() => handleOpenDistribute(record.taskId)}
            >
              分发
            </Button>
          </Tooltip>
          <Button
            type="link"
            size="small"
            icon={<DownloadOutlined />}
            onClick={() => handleDownload(record.taskId)}
          >
            下载
          </Button>
          <Tooltip 
            title={
              !canDeleteInitialModel(record.taskId) 
                ? '模型正在分发中，请稍后再试'
                : (['DISTRIBUTING', 'DISTRIBUTED'].includes(record.status) 
                    ? '该模型已分发，删除时将强制执行' 
                    : '删除此初始模型')
            }
          >
            <Popconfirm
              title={
                ['DISTRIBUTING', 'DISTRIBUTED'].includes(record.status)
                  ? "该模型已分发，删除可能影响联邦学习任务，确定要删除吗？"
                  : "确定要删除此初始模型吗？"
              }
              onConfirm={() => handleDelete(record.taskId)}
              okText="确定"
              cancelText="取消"
              disabled={!canDeleteInitialModel(record.taskId)}
            >
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                disabled={!canDeleteInitialModel(record.taskId)}
              >
                删除
              </Button>
            </Popconfirm>
          </Tooltip>
        </Space>
      )
    }
  ]


  // 处理生成初始模型
  const handleGenerate = async () => {
    try {
      const values = await generateForm.validateFields()
      
      let architecture: any
      
      if (selectedModelType === 'NEURAL_NETWORK') {
        // 神经网络架构参数
        architecture = {
          inputSize: values.inputSize,
          hiddenLayers: values.hiddenLayers.split(',').map((n: string) => parseInt(n.trim())),
          outputSize: values.outputSize,
          activationFunction: values.activationFunction,
          optimizer: values.optimizer,
          learningRate: values.learningRate
        }
      } else {
        // 随机森林架构参数
        architecture = {
          n_estimators: values.n_estimators,
          n_features: values.n_features,
          task_type: values.task_type
        }
      }
      
      const requestData: InitialModelGenerationRequest = {
        taskId: values.taskId,
        modelType: selectedModelType as any,
        architecture,
        randomSeed: values.randomSeed,
        description: values.description
      }

      const result = await generateInitialModel(requestData)
      
      if (result.success) {
        message.success('初始模型生成成功')
        setGenerateModalVisible(false)
        generateForm.resetFields()
        setSelectedModelType('RANDOM_FOREST') // 重置模型类型
        // 刷新列表
        if (values.taskId) {
          await fetchTaskInitialModel(values.taskId)
        }
      } else {
        message.error(result.error || '初始模型生成失败')
      }
    } catch (error) {
      console.error('生成初始模型失败:', error)
    }
  }

  // 处理上传自定义初始模型
  const handleUpload = async () => {
    try {
      const values = await uploadForm.validateFields()
      
      if (fileList.length === 0) {
        message.error('请选择模型文件')
        return
      }

      const file = fileList[0].originFileObj

      const requestData: InitialModelUploadRequest = {
        taskId: values.taskId,
        modelType: selectedUploadModelType as any,
        description: values.description,
        file: file,
        metadata: values.metadata ? JSON.parse(values.metadata) : undefined
      }

      const result = await uploadCustomInitialModel(requestData)
      
      if (result.success) {
        message.success('自定义初始模型上传成功')
        setUploadModalVisible(false)
        uploadForm.resetFields()
        setFileList([])
        setSelectedUploadModelType('RANDOM_FOREST') // 重置模型类型
        // 刷新列表
        if (values.taskId) {
          await fetchTaskInitialModel(values.taskId)
        }
      } else {
        message.error(result.error || '自定义初始模型上传失败')
      }
    } catch (error) {
      console.error('上传自定义初始模型失败:', error)
    }
  }

  // 处理分发初始模型
  const handleDistribute = async () => {
    try {
      const values = await distributeForm.validateFields()
      
      // 调试信息
      console.log('🔍 分发表单值:', values)
      console.log('🔍 可用虚拟机列表:', availableVms)
      
      if (!values.vmIds || values.vmIds.length === 0) {
        message.error('请选择至少一个虚拟机')
        return
      }
      
      const requestData: ModelDistributionRequest = {
        vmIds: values.vmIds, // 现在直接使用数组，无需split
        distributionMode: values.distributionMode,
        timeout: values.timeout,
        retryAttempts: values.retryAttempts,
        verifyChecksum: values.verifyChecksum,
        notifyOnCompletion: values.notifyOnCompletion
      }

      console.log('🚀 分发请求数据:', requestData)

      const result = await distributeInitialModel(selectedTaskId, requestData)
      
      if (result.success) {
        message.success('初始模型分发已启动')
        setDistributeModalVisible(false)
        distributeForm.resetFields()
      } else {
        message.error(result.error || '初始模型分发失败')
      }
    } catch (error) {
      console.error('分发初始模型失败:', error)
    }
  }

  // 打开分发对话框
  const handleOpenDistribute = (taskId: string) => {
    setSelectedTaskId(taskId)
    setDistributeModalVisible(true)
    // 加载可用虚拟机列表
    loadAvailableVms()
  }

  // 查看详情
  const handleViewDetail = async (taskId: string) => {
    setSelectedTaskId(taskId)
    setDetailModalVisible(true)
    await fetchTaskInitialModel(taskId, { includeParameters: true })
  }

  // 查看分发详情
  const handleViewDistributionDetail = async (distributionId: string) => {
    setSelectedDistributionId(distributionId)
    setDistributionDetailModalVisible(true)
    // 立即获取最新的分发详情数据
    await fetchDistributionStatus(distributionId)
  }

  // 刷新分发状态
  const handleRefreshDistribution = async (distributionId: string) => {
    await fetchDistributionStatus(distributionId)
    message.success('分发状态已刷新')
  }

  // 下载初始模型
  const handleDownload = async (taskId: string, format: 'binary' | 'json' = 'binary') => {
    try {
      const result = await downloadInitialModel(taskId, { format })
      if (result.success) {
        message.success('初始模型下载成功')
      } else {
        message.error(result.error || '初始模型下载失败')
      }
    } catch (error) {
      message.error('初始模型下载失败')
    }
  }

  // 删除初始模型
  const handleDelete = async (taskId: string) => {
    try {
      const initialModel = getTaskInitialModel(taskId)
      const needsForce = initialModel && ['DISTRIBUTING', 'DISTRIBUTED'].includes(initialModel.status)
      
      // 如果需要强制删除，显示确认对话框
      if (needsForce) {
        Modal.confirm({
          title: '确认强制删除',
          content: (
            <div>
              <p>该模型当前状态为 <Tag color="orange">{initialModel.status === 'DISTRIBUTING' ? '分发中' : '已分发'}</Tag></p>
              <p>强制删除可能会影响正在进行的联邦学习任务，确定要继续吗？</p>
            </div>
          ),
          okText: '确认删除',
          cancelText: '取消',
          okType: 'danger',
          onOk: async () => {
            await performDelete(taskId, true)
          }
        })
      } else {
        await performDelete(taskId, false)
      }
    } catch (error) {
      message.error('初始模型删除失败')
    }
  }

  // 执行删除操作
  const performDelete = async (taskId: string, force: boolean) => {
    try {
      const result = await deleteInitialModel(taskId, { force })
      if (result.success) {
        message.success('初始模型删除成功')
      } else {
        message.error(result.error || '初始模型删除失败')
      }
    } catch (error) {
      message.error('初始模型删除失败')
    }
  }

  // 获取分发按钮的提示信息
  const getDistributeTooltip = (taskId: string) => {
    const initialModel = getTaskInitialModel(taskId)
    
    if (!initialModel) {
      return '初始模型不存在'
    }
    
    if (initialModel.status !== 'READY') {
      return `模型状态为${initialModel.status}，只有READY状态的模型可以分发`
    }
    
    if (isDistributing(taskId)) {
      return '模型正在分发中，请稍后再试'
    }
    
    return '分发初始模型到各个节点'
  }

  const currentModel = selectedTaskId ? getTaskInitialModel(selectedTaskId) : null
  const currentDistribution = selectedDistributionId ? getDistributionStatus(selectedDistributionId) : null

  return (
    <div style={{ padding: '24px' }}>
      <Card
        title="初始模型管理"
        extra={
          <Space>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                setGenerateModalVisible(true)
                // 确保联邦任务列表已加载
                if (federatedTasks.length === 0) {
                  loadFederatedTasks()
                }
              }}
              loading={generationLoading}
            >
              生成初始模型
            </Button>
            <Button
              icon={<UploadOutlined />}
              onClick={() => {
                setUploadModalVisible(true)
                // 确保联邦任务列表已加载
                if (federatedTasks.length === 0) {
                  loadFederatedTasks()
                }
              }}
              loading={initialUploadLoading}
            >
              上传自定义模型
            </Button>
          </Space>
        }
      >
        {/* 筛选区域 */}
        <Card size="small" style={{ marginBottom: 16 }}>
          <Alert
            message="使用说明"
            description="初始模型管理会自动遍历所有联邦学习任务并显示相应的初始模型。你可以使用搜索框快速查找特定任务的模型，或点击刷新按钮更新列表。"
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <Space wrap>
            <Input
              placeholder="搜索任务ID或模型ID"
              value={queryTaskId}
              onChange={(e) => setQueryTaskId(e.target.value)}
              style={{ width: 300 }}
              prefix={<SearchOutlined />}
              allowClear
            />
            <Button 
              icon={<ReloadOutlined />}
              onClick={loadAllInitialModels}
              loading={Object.values(initialModelLoading).some(Boolean)}
            >
              刷新模型列表
            </Button>
          </Space>
          {federatedTasks.length > 0 && (
            <div style={{ marginTop: 12, fontSize: '12px', color: '#666' }}>
              💡 当前系统中有 <strong>{federatedTasks.length}</strong> 个联邦学习任务可供选择
            </div>
          )}
        </Card>

        <Table
          columns={modelColumns}
          dataSource={modelListData}
          loading={Object.values(initialModelLoading).some(Boolean)}
          scroll={{ x: 1400 }}
          pagination={{
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            pageSize: 10
          }}
          locale={{
            emptyText: queryTaskId ? 
              '搜索无结果，请尝试其他关键词' : 
              '暂无初始模型数据，请先创建联邦学习任务并生成或上传初始模型'
          }}
        />
      </Card>

      {/* 生成初始模型对话框 */}
      <Modal
        title="生成随机初始模型"
        open={generateModalVisible}
        onOk={handleGenerate}
        onCancel={() => {
          setGenerateModalVisible(false)
          generateForm.resetFields()
          setSelectedModelType('RANDOM_FOREST')
        }}
        width={800}
        confirmLoading={generationLoading}
      >
        <Form
          form={generateForm}
          layout="vertical"
          initialValues={{
            modelType: 'RANDOM_FOREST',
            // 随机森林默认值
            n_estimators: 100,
            n_features: 5,
            task_type: 'regression',
            // 神经网络默认值
            activationFunction: 'relu',
            optimizer: 'adam',
            learningRate: 0.001,
            distributionMode: 'ASYNC',
            timeout: 300,
            retryAttempts: 3,
            verifyChecksum: true,
            notifyOnCompletion: true
          }}
        >
          <Form.Item
            label="任务ID"
            name="taskId"
            rules={[{ required: true, message: '请选择任务ID' }]}
          >
            <Select
              placeholder="请选择任务ID"
              showSearch
              filterOption={(input, option) => {
                const taskId = option?.value as string
                const task = federatedTasks.find(t => t.taskId === taskId)
                if (task) {
                  return task.taskName.toLowerCase().includes(input.toLowerCase()) ||
                         task.taskId.toLowerCase().includes(input.toLowerCase())
                }
                return false
              }}
            >
              {federatedTasks.map(task => (
                <Option key={task.taskId} value={task.taskId}>
                  <div>
                    <div style={{ fontWeight: 'bold' }}>{task.taskName}</div>
                    <div style={{ fontSize: '12px', color: '#666' }}>ID: {task.taskId}</div>
                  </div>
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="模型类型"
            name="modelType"
            rules={[{ required: true, message: '请选择模型类型' }]}
          >
            <Select
              value={selectedModelType}
              onChange={(value) => {
                setSelectedModelType(value)
                // 清空相关字段
                generateForm.resetFields(['inputSize', 'outputSize', 'hiddenLayers', 'activationFunction', 'optimizer', 'learningRate', 'n_estimators', 'n_features', 'task_type'])
              }}
            >
              <Option value="RANDOM_FOREST">随机森林（推荐）</Option>
              <Option value="NEURAL_NETWORK">神经网络</Option>
            </Select>
          </Form.Item>

          {/* 随机森林参数 */}
          {selectedModelType === 'RANDOM_FOREST' && (
            <div style={{ border: '1px solid #e8e8e8', borderRadius: '6px', padding: '16px', backgroundColor: '#fafafa' }}>
              <h4 style={{ marginBottom: '16px', color: '#1890ff' }}>🌲 随机森林参数</h4>
              
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="树的数量"
                    name="n_estimators"
                    rules={[
                      { required: true, message: '请输入树的数量' },
                      { type: 'number', min: 10, max: 500, message: '树的数量必须在10-500之间' }
                    ]}
                    tooltip="决策树的数量，通常在10-500之间，数量越多模型越复杂"
                  >
                    <InputNumber min={10} max={500} style={{ width: '100%' }} placeholder="100" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="特征数量"
                    name="n_features"
                    rules={[
                      { required: true, message: '请输入特征数量' },
                      { type: 'number', min: 1, message: '特征数量必须大于等于1' }
                    ]}
                    tooltip="每次分割时考虑的特征数量"
                  >
                    <InputNumber min={1} style={{ width: '100%' }} placeholder="5" />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item
                label="任务类型"
                name="task_type"
                rules={[{ required: true, message: '请选择任务类型' }]}
                tooltip="选择分类任务或回归任务"
              >
                <Select placeholder="请选择任务类型">
                  <Option value="classification">分类任务</Option>
                  <Option value="regression">回归任务</Option>
                </Select>
              </Form.Item>
            </div>
          )}

          {/* 神经网络参数 */}
          {selectedModelType === 'NEURAL_NETWORK' && (
            <div style={{ border: '1px solid #e8e8e8', borderRadius: '6px', padding: '16px', backgroundColor: '#fafafa' }}>
              <h4 style={{ marginBottom: '16px', color: '#722ed1' }}>🧠 神经网络参数</h4>
              
              <Row gutter={16}>
                <Col span={8}>
                  <Form.Item
                    label="输入大小"
                    name="inputSize"
                    rules={[{ required: true, message: '请输入输入大小' }]}
                    tooltip="输入层神经元数量"
                  >
                    <InputNumber min={1} style={{ width: '100%' }} placeholder="128" />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    label="输出大小"
                    name="outputSize"
                    rules={[{ required: true, message: '请输入输出大小' }]}
                    tooltip="输出层神经元数量"
                  >
                    <InputNumber min={1} style={{ width: '100%' }} placeholder="10" />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    label="学习率"
                    name="learningRate"
                    rules={[{ required: true, message: '请输入学习率' }]}
                    tooltip="控制模型学习速度，通常在0.0001-0.1之间"
                  >
                    <InputNumber min={0} max={1} step={0.0001} style={{ width: '100%' }} placeholder="0.001" />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item
                label="隐藏层"
                name="hiddenLayers"
                rules={[{ required: true, message: '请输入隐藏层配置' }]}
                tooltip="使用逗号分隔，例如: 64,32,16"
              >
                <Input placeholder="64,32,16" />
              </Form.Item>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="激活函数"
                    name="activationFunction"
                    rules={[{ required: true, message: '请选择激活函数' }]}
                    tooltip="神经网络激活函数，根据后端支持的选项"
                  >
                    <Select placeholder="请选择激活函数">
                      <Option value="relu">ReLU</Option>
                      <Option value="sigmoid">Sigmoid</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="优化器"
                    name="optimizer"
                    rules={[{ required: true, message: '请选择优化器' }]}
                    tooltip="神经网络优化器，根据后端支持的选项"
                  >
                    <Select placeholder="请选择优化器">
                      <Option value="adam">Adam</Option>
                      <Option value="sgd">SGD</Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>
            </div>
          )}

          <Row gutter={16} style={{ marginTop: '16px' }}>
            <Col span={12}>
              <Form.Item
                label="随机种子"
                name="randomSeed"
                tooltip="用于确保结果可重现，可选参数"
              >
                <InputNumber style={{ width: '100%' }} placeholder="42" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="描述"
            name="description"
          >
            <TextArea rows={3} placeholder="请输入模型描述" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 上传自定义模型对话框 */}
      <Modal
        title="上传自定义初始模型"
        open={uploadModalVisible}
        onOk={handleUpload}
        onCancel={() => {
          setUploadModalVisible(false)
          uploadForm.resetFields()
          setFileList([])
          setSelectedUploadModelType('RANDOM_FOREST')
          setJsonValidationStatus('')
          setJsonValidationMessage('')
        }}
        width={600}
        confirmLoading={initialUploadLoading}
      >
        <Form form={uploadForm} layout="vertical">
          <Form.Item
            label="任务ID"
            name="taskId"
            rules={[{ required: true, message: '请选择任务ID' }]}
          >
            <Select
              placeholder="请选择任务ID"
              showSearch
              filterOption={(input, option) => {
                const taskId = option?.value as string
                const task = federatedTasks.find(t => t.taskId === taskId)
                if (task) {
                  return task.taskName.toLowerCase().includes(input.toLowerCase()) ||
                         task.taskId.toLowerCase().includes(input.toLowerCase())
                }
                return false
              }}
            >
              {federatedTasks.map(task => (
                <Option key={task.taskId} value={task.taskId}>
                  <div>
                    <div style={{ fontWeight: 'bold' }}>{task.taskName}</div>
                    <div style={{ fontSize: '12px', color: '#666' }}>ID: {task.taskId}</div>
                  </div>
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="模型类型"
            name="modelType"
            rules={[{ required: true, message: '请选择模型类型' }]}
          >
            <Select
              value={selectedUploadModelType}
              onChange={(value) => setSelectedUploadModelType(value)}
            >
              <Option value="RANDOM_FOREST">随机森林（推荐）</Option>
              <Option value="NEURAL_NETWORK">神经网络</Option>
            </Select>
          </Form.Item>

          {/* 根据模型类型显示不同的文件格式提示 */}
          <Form.Item
            label="模型文件"
            required
            tooltip={selectedUploadModelType === 'RANDOM_FOREST' 
              ? "随机森林模型支持: .pkl, .pickle, .joblib 等格式" 
              : "神经网络模型支持: .pth, .pt, .h5, .pb, .onnx 等格式"
            }
          >
            <Upload
              fileList={fileList}
              onChange={({ fileList }) => setFileList(fileList)}
              beforeUpload={() => false}
              maxCount={1}
              accept={selectedUploadModelType === 'RANDOM_FOREST' 
                ? ".pkl,.pickle,.joblib" 
                : ".pth,.pt,.h5,.pb,.onnx"
              }
            >
              <Button icon={<UploadOutlined />}>选择文件</Button>
            </Upload>
            <div style={{ marginTop: '8px', fontSize: '12px', color: '#666' }}>
              {selectedUploadModelType === 'RANDOM_FOREST' ? (
                <span>🌲 随机森林：建议使用 .pkl 格式（sklearn序列化）</span>
              ) : (
                <span>🧠 神经网络：建议使用 .pth 格式（PyTorch）或 .h5 格式（Keras/TensorFlow）</span>
              )}
            </div>
          </Form.Item>

          <Form.Item
            label="元数据"
            name="metadata"
            tooltip="JSON格式的模型元数据，可选字段"
            validateStatus={jsonValidationStatus}
            help={jsonValidationMessage}
            rules={[
              {
                validator: (_, value) => {
                  if (!value || value.trim() === '') {
                    setJsonValidationStatus('')
                    setJsonValidationMessage('')
                    return Promise.resolve() // 允许为空
                  }
                  try {
                    JSON.parse(value)
                    setJsonValidationStatus('success')
                    setJsonValidationMessage('JSON格式正确 ✓')
                    return Promise.resolve()
                  } catch (error) {
                    setJsonValidationStatus('error')
                    setJsonValidationMessage('JSON格式错误，请检查语法')
                    return Promise.reject(new Error('请输入有效的JSON格式'))
                  }
                }
              }
            ]}
          >
            <TextArea
              rows={4}
              placeholder={selectedUploadModelType === 'RANDOM_FOREST' 
                ? '{"n_estimators": 100, "n_features": 5, "task_type": "regression", "framework": "sklearn", "version": "1.0"}' 
                : '{"architecture": {"inputSize": 128, "outputSize": 10}, "framework": "pytorch", "version": "1.0"}'
              }
              onChange={(e) => {
                const value = e.target.value.trim()
                if (!value) {
                  setJsonValidationStatus('')
                  setJsonValidationMessage('')
                  return
                }
                
                try {
                  JSON.parse(value)
                  setJsonValidationStatus('success')
                  setJsonValidationMessage('JSON格式正确 ✓')
                } catch (error) {
                  setJsonValidationStatus('error')
                  setJsonValidationMessage(`JSON格式错误: ${(error as Error).message}`)
                }
              }}
            />
            <div style={{ marginTop: '8px', fontSize: '12px', color: '#666' }}>
              💡 提示：元数据为可选字段，用于存储模型的额外信息（如框架版本、训练参数等）
            </div>
          </Form.Item>

          <Form.Item
            label="描述"
            name="description"
          >
            <TextArea rows={3} placeholder="请输入模型描述" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 分发初始模型对话框 */}
      <Modal
        title="分发初始模型"
        open={distributeModalVisible}
        onOk={handleDistribute}
        onCancel={() => {
          setDistributeModalVisible(false)
          distributeForm.resetFields()
        }}
        width={600}
        confirmLoading={isDistributing(selectedTaskId)}
      >
        <Form
          form={distributeForm}
          layout="vertical"
          initialValues={{
            distributionMode: 'ASYNC',
            timeout: 300,
            retryAttempts: 3,
            verifyChecksum: true,
            notifyOnCompletion: true
          }}
        >
          <Form.Item
            label="目标虚拟机"
            name="vmIds"
            rules={[
              { required: true, message: '请选择至少一个虚拟机' }
            ]}
            tooltip="选择要分发初始模型的虚拟机节点"
          >
            {vmsLoading ? (
              <div style={{ textAlign: 'center', padding: 20, border: '1px solid #d9d9d9', borderRadius: 6 }}>
                <Spin size="small" /> 加载虚拟机列表...
              </div>
            ) : availableVms.length > 0 ? (
              <Checkbox.Group style={{ width: '100%' }}>
                <div style={{ maxHeight: 300, overflowY: 'auto', border: '1px solid #d9d9d9', borderRadius: 6, padding: 8 }}>
                  <Row>
                    {availableVms.map((vm) => (
                      <Col span={24} key={vm.vmId} style={{ marginBottom: 8 }}>
                        <Checkbox value={vm.vmId}>
                          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
                            <div>
                              <strong>{vm.name}</strong>
                              <br />
                              <span style={{ fontSize: '12px', color: '#666' }}>
                                ID: {vm.vmId} | IP: {vm.ipAddress}
                              </span>
                            </div>
                            <div style={{ textAlign: 'right', fontSize: '12px', color: '#999' }}>
                              <div>CPU: {vm.resources?.cpuCores}核</div>
                              <div>内存: {Math.round((vm.resources?.memoryMb || 0) / 1024)}GB</div>
                              {vm.resources?.gpuCount > 0 && <div>GPU: {vm.resources.gpuCount}个</div>}
                            </div>
                          </div>
                        </Checkbox>
                      </Col>
                    ))}
                  </Row>
                </div>
              </Checkbox.Group>
            ) : (
              <div style={{ textAlign: 'center', padding: 20, color: '#999', border: '1px solid #d9d9d9', borderRadius: 6 }}>
                暂无可用的虚拟机
                <br />
                <Button type="link" size="small" onClick={loadAvailableVms}>
                  重新加载
                </Button>
              </div>
            )}
          </Form.Item>

          <Form.Item
            label="分发模式"
            name="distributionMode"
            rules={[{ required: true, message: '请选择分发模式' }]}
            tooltip="模型将异步分发到各个虚拟机节点"
          >
            <Select disabled>
              <Option value="ASYNC">异步分发</Option>
            </Select>
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="超时时间(秒)"
                name="timeout"
                rules={[{ required: true, message: '请输入超时时间' }]}
              >
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="重试次数"
                name="retryAttempts"
                rules={[{ required: true, message: '请输入重试次数' }]}
              >
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="验证校验和"
            name="verifyChecksum"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label="完成时通知"
            name="notifyOnCompletion"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      {/* 模型详情对话框 */}
      <Modal
        title="初始模型详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={800}
      >
        {currentModel && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="模型ID" span={2}>
              {currentModel.modelId}
            </Descriptions.Item>
            <Descriptions.Item label="任务ID" span={2}>
              {currentModel.taskId}
            </Descriptions.Item>
            <Descriptions.Item label="模型类型">
              {currentModel.modelType}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={getStatusColor(currentModel.status)}>{currentModel.status}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="模型大小">
              {(currentModel.modelSize / 1024 / 1024).toFixed(2)} MB
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {new Date(currentModel.createdAt).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="校验和" span={2}>
              <code>{currentModel.checksum}</code>
            </Descriptions.Item>
            <Descriptions.Item label="架构参数" span={2}>
              <pre style={{ margin: 0, maxHeight: '200px', overflow: 'auto' }}>
                {JSON.stringify(currentModel.architecture, null, 2)}
              </pre>
            </Descriptions.Item>
            <Descriptions.Item label="分发状态" span={2}>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Row gutter={16}>
                  <Col span={8}>
                    <Statistic title="总虚拟机" value={currentModel.distributionStatus?.totalVms || 0} />
                  </Col>
                  <Col span={8}>
                    <Statistic 
                      title="已分发" 
                      value={currentModel.distributionStatus?.distributedVms || 0}
                      valueStyle={{ color: '#3f8600' }}
                    />
                  </Col>
                  <Col span={8}>
                    <Statistic 
                      title="失败" 
                      value={currentModel.distributionStatus?.failedVms || 0}
                      valueStyle={{ color: '#cf1322' }}
                    />
                  </Col>
                </Row>
              </Space>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      {/* 分发详情对话框 */}
      <Modal
        title="分发详情"
        open={distributionDetailModalVisible}
        onCancel={() => setDistributionDetailModalVisible(false)}
        footer={[
          <Button 
            key="refresh" 
            icon={<ReloadOutlined />}
            onClick={() => handleRefreshDistribution(selectedDistributionId)}
          >
            刷新
          </Button>,
          <Button key="close" onClick={() => setDistributionDetailModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={900}
      >
        {currentDistribution && (
          <>
            <Descriptions bordered column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="分发ID" span={2}>
                {currentDistribution.distributionId}
              </Descriptions.Item>
              <Descriptions.Item label="任务ID">
                {currentDistribution.taskId}
              </Descriptions.Item>
              <Descriptions.Item label="模型ID">
                {currentDistribution.modelId}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={getDistributionStatusColor(currentDistribution.status)}>
                  {currentDistribution.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="开始时间">
                {currentDistribution.startedAt ? new Date(currentDistribution.startedAt).toLocaleString() : '-'}
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 16 }}>
              <h4>分发进度</h4>
              <Progress
                percent={
                  currentDistribution.progress
                    ? Math.round((currentDistribution.progress.completed / currentDistribution.progress.total) * 100)
                    : 0
                }
                status={currentDistribution.progress?.failed > 0 ? 'exception' : 'active'}
              />
              <Row gutter={16} style={{ marginTop: 16 }}>
                <Col span={6}>
                  <Statistic title="总数" value={currentDistribution.progress?.total || 0} />
                </Col>
                <Col span={6}>
                  <Statistic 
                    title="已完成" 
                    value={currentDistribution.progress?.completed || 0}
                    valueStyle={{ color: '#3f8600' }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic 
                    title="进行中" 
                    value={currentDistribution.progress?.inProgress || 0}
                    valueStyle={{ color: '#1890ff' }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic 
                    title="失败" 
                    value={currentDistribution.progress?.failed || 0}
                    valueStyle={{ color: '#cf1322' }}
                  />
                </Col>
              </Row>
            </div>

            {currentDistribution.vmDetails && currentDistribution.vmDetails.length > 0 && (
              <div style={{ marginTop: 24 }}>
                <h4>虚拟机详情</h4>
                <Table
                  dataSource={currentDistribution.vmDetails}
                  rowKey="vmId"
                  size="small"
                  pagination={false}
                  scroll={{ y: 300 }}
                  columns={[
                    {
                      title: '虚拟机ID',
                      dataIndex: 'vmId',
                      key: 'vmId'
                    },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      key: 'status',
                      render: (status: string) => (
                        <Tag color={status === 'SUCCESS' ? 'success' : 'error'}>
                          {status}
                        </Tag>
                      )
                    },
                    {
                      title: '验证状态',
                      dataIndex: 'verificationStatus',
                      key: 'verificationStatus',
                      render: (status: string) => (
                        <Tag color={status === 'VERIFIED' ? 'success' : 'warning'}>
                          {status}
                        </Tag>
                      )
                    },
                    {
                      title: '分发时间',
                      dataIndex: 'distributedAt',
                      key: 'distributedAt',
                      render: (time: string) => time ? new Date(time).toLocaleString() : '-'
                    }
                  ]}
                />
              </div>
            )}
          </>
        )}
      </Modal>

      {/* 任务选择对话框 */}
      <Modal
        title="选择联邦学习任务"
        open={taskSelectModalVisible}
        onCancel={() => setTaskSelectModalVisible(false)}
        footer={null}
        width={1000}
      >
        <div style={{ marginBottom: 16 }}>
          <Alert
            message="请选择一个联邦学习任务来查询其初始模型"
            type="info"
            showIcon
          />
        </div>
        <Table
          dataSource={federatedTasks}
          rowKey="taskId"
          loading={tasksLoading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 个任务`
          }}
          scroll={{ y: 400 }}
          columns={[
            {
              title: '任务ID',
              dataIndex: 'taskId',
              key: 'taskId',
              width: 200,
              ellipsis: true,
              render: (taskId: string) => (
                <Tooltip title={taskId}>
                  <code>{taskId}</code>
                </Tooltip>
              )
            },
            {
              title: '任务名称',
              dataIndex: 'taskName',
              key: 'taskName',
              ellipsis: true
            },
            {
              title: '任务类型',
              dataIndex: 'taskType',
              key: 'taskType',
              width: 120
            },
            {
              title: '状态',
              dataIndex: 'status',
              key: 'status',
              width: 100,
              render: (status: string) => {
                const statusColors: Record<string, string> = {
                  'CREATED': 'default',
                  'CONFIGURED': 'processing',
                  'RUNNING': 'processing',
                  'PAUSED': 'warning',
                  'STOPPED': 'default',
                  'COMPLETED': 'success',
                  'FAILED': 'error',
                  'CANCELLED': 'default'
                }
                return <Tag color={statusColors[status] || 'default'}>{status}</Tag>
              }
            },
            {
              title: '参与者数量',
              dataIndex: 'participantCount',
              key: 'participantCount',
              width: 100
            },
            {
              title: '创建时间',
              dataIndex: 'createdAt',
              key: 'createdAt',
              width: 180,
              render: (time: string) => new Date(time).toLocaleString()
            },
            {
              title: '操作',
              key: 'action',
              width: 120,
              render: (_, record) => (
                <Button
                  type="primary"
                  size="small"
                  onClick={() => handleTaskSelected(record.taskId)}
                >
                  选择此任务
                </Button>
              )
            }
          ]}
        />
      </Modal>
    </div>
  )
}

export default InitialModelPage

