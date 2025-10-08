/**
 * 模型版本管理页面
 * 覆盖model-version-api-reference.md的版本管理相关接口
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
  Descriptions,
  Row,
  Col,
  Tabs,
  InputNumber,
  Popconfirm,
  Tooltip,
  Statistic,
  Progress
} from 'antd'
import {
  PlusOutlined,
  UploadOutlined,
  DownloadOutlined,
  DeleteOutlined,
  FileTextOutlined,
  ReloadOutlined,
  CloudUploadOutlined,
  RocketOutlined,
  RollbackOutlined,
  BarChartOutlined,
  LineChartOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons'
import { Line, Column } from '@ant-design/plots'
import type { ColumnsType } from 'antd/es/table'
import { useModel } from '@/store/model-version/useModelVersionStore'
import type {
  ModelVersionListParams,
  ModelVersionDetail,
  EvaluationRequest,
  RollbackRequest,
  StatisticsParams
} from '@/services'

const { TextArea } = Input
const { Option } = Select

const VersionManagementPage: React.FC = () => {
  const {
    // 状态
    modelList,
    modelListTotal,
    modelListLoading,
    modelListError,
    currentModel,
    currentModelLoading,
    taskModels,
    pagination,
    evaluationResults,
    evaluationLoading,
    evaluationResultsLoading,
    rollbackHistory,
    modelStatistics,
    statisticsLoading,
    
    // 操作方法
    fetchModelList,
    fetchModelDetail,
    fetchTaskModels,
    downloadModel,
    downloadModelBatch,
    deleteModel,
    deleteModelBatch,
    setPagination,
    evaluateModel,
    batchEvaluateModels,
    fetchEvaluationResults,
    rollbackModel,
    fetchRollbackHistory,
    fetchModelStatistics,
    fetchTaskStatistics,
    
    // 工具方法
    getTaskModels,
    canDeleteModel,
    getEvaluationResults,
    getRollbackHistory,
    getTaskStatistics
  } = useModel()

  const [activeTab, setActiveTab] = useState('models')
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [taskModelsModalVisible, setTaskModelsModalVisible] = useState(false)
  const [evaluateModalVisible, setEvaluateModalVisible] = useState(false)
  const [batchEvaluateModalVisible, setBatchEvaluateModalVisible] = useState(false)
  const [rollbackModalVisible, setRollbackModalVisible] = useState(false)
  const [rollbackHistoryModalVisible, setRollbackHistoryModalVisible] = useState(false)
  const [evaluationDetailModalVisible, setEvaluationDetailModalVisible] = useState(false)
  
  const [selectedModelId, setSelectedModelId] = useState<string>('')
  const [selectedTaskId, setSelectedTaskId] = useState<string>('')
  const [selectedEvaluationId, setSelectedEvaluationId] = useState<string>('')
  const [statisticsTaskId, setStatisticsTaskId] = useState<string>('')
  const [statisticsTimeRange, setStatisticsTimeRange] = useState<'7d' | '30d' | '90d'>('7d')
  const [statisticsMode, setStatisticsMode] = useState<'model' | 'task'>('model') // 统计模式：模型统计/任务进度
  const [fileList, setFileList] = useState<any[]>([])
  const [batchFileList, setBatchFileList] = useState<any[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  
  const [searchForm] = Form.useForm()
  const [evaluateForm] = Form.useForm()
  const [batchEvaluateForm] = Form.useForm()
  const [rollbackForm] = Form.useForm()

  // 加载模型列表
  useEffect(() => {
    fetchModelList()
  }, [])

  // 模型状态颜色
  const getStatusColor = (status: string) => {
    const colorMap: Record<string, string> = {
      'UPLOADING': 'processing',
      'UPLOADED': 'success',
      'VALIDATING': 'processing',
      'VALIDATED': 'success',
      'DEPLOYED': 'success',
      'DEPRECATED': 'warning',
      'FAILED': 'error'
    }
    return colorMap[status] || 'default'
  }

  // 模型列表表格列定义
  const columns: ColumnsType<any> = [
    {
      title: '模型ID',
      dataIndex: 'modelId',
      key: 'modelId',
      width: 200,
      ellipsis: true,
      fixed: 'left',
      render: (modelId: string) => (
        <Tooltip title={`完整模型ID: ${modelId} (点击复制)`} placement="topLeft">
          <span 
            style={{ 
              cursor: 'pointer',
              //color: '#1890ff',
              //textDecoration: 'underline'
            }}
            onClick={() => {
              navigator.clipboard.writeText(modelId).then(() => {
                message.success('模型ID已复制到剪贴板')
              }).catch(() => {
                message.error('复制失败，请手动复制')
              })
            }}
          >
            {modelId}
          </span>
        </Tooltip>
      )
    },
    {
      title: '任务ID',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 200,
      ellipsis: true,
      render: (taskId: string) => (
        <Tooltip title={`完整任务ID: ${taskId} (点击复制)`} placement="topLeft">
          <span 
            style={{ 
              cursor: 'pointer',
              //color: '#1890ff',
              //textDecoration: 'underline'
            }}
            onClick={() => {
              navigator.clipboard.writeText(taskId).then(() => {
                message.success('任务ID已复制到剪贴板')
              }).catch(() => {
                message.error('复制失败，请手动复制')
              })
            }}
          >
            {taskId}
          </span>
        </Tooltip>
      )
    },
    {
      title: '轮次',
      dataIndex: 'roundNumber',
      key: 'roundNumber',
      width: 80,
      sorter: true
    },
    {
      title: '准确率',
      dataIndex: 'accuracy',
      key: 'accuracy',
      width: 100,
      render: (value: number | undefined) => value !== undefined ? (value * 100).toFixed(2) + '%' : '-'
    },
    {
      title: '损失',
      dataIndex: 'loss',
      key: 'loss',
      width: 100,
      render: (value: number | undefined) => value !== undefined ? value.toFixed(6) : '-'
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
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      ellipsis: true
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
      width: 350,
      render: (_, record) => (
        <Space size="small" wrap>
          <Button
            type="link"
            size="small"
            icon={<FileTextOutlined />}
            onClick={() => handleViewDetail(record.modelId)}
          >
            详情
          </Button>
          <Button
            type="link"
            size="small"
            icon={<RocketOutlined />}
            onClick={() => handleOpenEvaluate(record.modelId)}
          >
            评估
          </Button>
          <Button
            type="link"
            size="small"
            icon={<RollbackOutlined />}
            onClick={() => handleOpenRollback(record.modelId)}
            disabled={record.status !== 'DEPLOYED'}
          >
            回滚
          </Button>
          <Button
            type="link"
            size="small"
            icon={<DownloadOutlined />}
            onClick={() => handleDownload(record.modelId)}
          >
            下载
          </Button>
          <Popconfirm
            title="确定要删除此模型版本吗？"
            onConfirm={() => handleDelete(record.modelId)}
            okText="确定"
            cancelText="取消"
            disabled={!canDeleteModel(record)}
          >
            <Tooltip 
              title={
                !canDeleteModel(record) 
                  ? (record.status === 'DEPLOYED' 
                      ? '已部署的模型无法删除，请先取消部署' 
                      : '模型正在操作中，请稍后再试')
                  : '删除此模型版本'
              }
            >
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                disabled={!canDeleteModel(record)}
              >
                删除
              </Button>
            </Tooltip>
          </Popconfirm>
        </Space>
      )
    }
  ]

  // 表格行选择配置
  const rowSelection = {
    selectedRowKeys,
    onChange: (keys: React.Key[]) => {
      setSelectedRowKeys(keys)
    }
  }

  // 处理搜索
  const handleSearch = async () => {
    const values = await searchForm.validateFields()
    const params: ModelVersionListParams = {
      taskId: values.taskId || undefined,
      roundNumber: values.roundNumber || undefined,
      status: values.status || undefined,
      page: 1,
      size: pagination.size,
      sort: values.sort || 'createdAt',
      order: values.order || 'desc'
    }
    await fetchModelList(params)
  }

  // 重置搜索
  const handleResetSearch = () => {
    searchForm.resetFields()
    fetchModelList({ page: 1, size: 10 })
  }


  // 查看详情
  const handleViewDetail = async (modelId: string) => {
    setSelectedModelId(modelId)
    setDetailModalVisible(true)
    await fetchModelDetail(modelId)
  }

  // 下载模型
  const handleDownload = async (modelId: string, format: 'original' | 'onnx' = 'original', compressed = true) => {
    try {
      const result = await downloadModel(modelId, { format, compressed })
      if (result.success) {
        message.success('模型下载成功')
      } else {
        message.error(result.error || '模型下载失败')
      }
    } catch (error) {
      message.error('模型下载失败')
    }
  }

  // 批量下载
  const handleBatchDownload = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要下载的模型')
      return
    }

    try {
      const result = await downloadModelBatch(selectedRowKeys as string[])
      if (result.success) {
        message.success('批量下载成功')
        setSelectedRowKeys([])
      } else {
        message.error(result.error || '批量下载失败')
      }
    } catch (error) {
      message.error('批量下载失败')
    }
  }

  // 打开评估对话框
  const handleOpenEvaluate = (modelId: string) => {
    setSelectedModelId(modelId)
    setEvaluateModalVisible(true)
    evaluateForm.setFieldsValue({ modelId })
  }

  // 处理模型评估
  const handleEvaluate = async () => {
    try {
      const values = await evaluateForm.validateFields()
      
      const requestData: EvaluationRequest = {
        modelId: values.modelId,
        testDataPath: values.testDataPath,
        metrics: values.metrics || ['accuracy', 'loss', 'precision', 'recall', 'f1'],
        batchSize: values.batchSize || 32,
        device: values.device || 'cpu'
      }

      const result = await evaluateModel(values.modelId, requestData)
      
      if (result.success) {
        message.success('模型评估已启动，请切换到"评估结果"标签页查看')
        setEvaluateModalVisible(false)
        evaluateForm.resetFields()
        // 刷新评估结果
        setTimeout(() => {
          fetchEvaluationResults()
        }, 1000)
      } else {
        message.error(result.error || '启动评估失败')
      }
    } catch (error) {
      message.error('启动评估失败')
    }
  }

  // 打开批量评估对话框
  const handleOpenBatchEvaluate = () => {
    setBatchEvaluateModalVisible(true)
  }

  // 处理批量评估
  const handleBatchEvaluate = async () => {
    try {
      const values = await batchEvaluateForm.validateFields()
      
      // 解析轮次列表
      const roundNumbers = values.roundNumbers 
        ? values.roundNumbers.split(',').map((n: string) => parseInt(n.trim())).filter((n: number) => !isNaN(n))
        : undefined

      const requestData = {
        taskId: values.taskId,
        testDataPath: values.testDataPath,
        roundNumbers,
        metrics: values.metrics || ['accuracy', 'loss'],
        batchSize: values.batchSize || 32
      }

      const result = await batchEvaluateModels(requestData)
      
      if (result.success) {
        message.success('批量评估已启动，请切换到"评估结果"标签页查看')
        setBatchEvaluateModalVisible(false)
        batchEvaluateForm.resetFields()
        // 刷新评估结果
        setTimeout(() => {
          fetchEvaluationResults()
        }, 1000)
      } else {
        message.error(result.error || '启动批量评估失败')
      }
    } catch (error) {
      message.error('启动批量评估失败')
    }
  }

  // 查看评估详情
  const handleViewEvaluationDetail = (evaluationId: string) => {
    setSelectedEvaluationId(evaluationId)
    setEvaluationDetailModalVisible(true)
  }

  // 打开回滚对话框
  const handleOpenRollback = (modelId: string) => {
    setSelectedModelId(modelId)
    setRollbackModalVisible(true)
  }

  // 处理模型回滚
  const handleRollback = async () => {
    try {
      const values = await rollbackForm.validateFields()
      
      const requestData: RollbackRequest = {
        deploymentId: selectedModelId, // 使用当前模型ID作为部署ID
        targetModelId: values.targetModelId,
        rollbackReason: values.rollbackReason,
        force: values.force || false
      }

      const result = await rollbackModel(requestData)
      
      if (result.success) {
        message.success('模型回滚成功')
        setRollbackModalVisible(false)
        rollbackForm.resetFields()
        await fetchModelList()
      } else {
        message.error(result.error || '回滚失败')
      }
    } catch (error) {
      message.error('回滚失败')
    }
  }

  // 查看回滚历史
  const handleViewRollbackHistory = async (deploymentId: string) => {
    setSelectedModelId(deploymentId)
    setRollbackHistoryModalVisible(true)
    await fetchRollbackHistory({ deploymentId })
  }

  // 加载统计数据
  useEffect(() => {
    if (activeTab === 'statistics') {
      // 始终使用10.1接口进行初始加载
      fetchModelStatistics({ timeRange: statisticsTimeRange as '7d' | '30d' | '90d' })
    }
  }, [activeTab])

  // 加载评估结果
  useEffect(() => {
    if (activeTab === 'evaluations') {
      fetchEvaluationResults()
    }
  }, [activeTab])

  // 删除模型
  const handleDelete = async (modelId: string) => {
    try {
      const result = await deleteModel(modelId, { force: false, deleteFile: true })
      if (result.success) {
        message.success('模型删除成功')
        await fetchModelList()
      } else {
        message.error(result.error || '模型删除失败')
      }
    } catch (error) {
      message.error('模型删除失败')
    }
  }

  // 批量删除
  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的模型')
      return
    }

    Modal.confirm({
      title: '批量删除确认',
      content: `确定要删除选中的 ${selectedRowKeys.length} 个模型版本吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          const result = await deleteModelBatch(selectedRowKeys as string[])
          if (result.success) {
            message.success('批量删除成功')
            setSelectedRowKeys([])
            await fetchModelList()
          } else {
            message.error(result.error || '批量删除失败')
          }
        } catch (error) {
          message.error('批量删除失败')
        }
      }
    })
  }

  // 查看任务模型版本
  const handleViewTaskModels = async (taskId: string) => {
    setSelectedTaskId(taskId)
    setTaskModelsModalVisible(true)
    await fetchTaskModels(taskId)
  }

  // 分页变化处理
  const handleTableChange = (paginationConfig: any, filters: any, sorter: any) => {
    setPagination(paginationConfig.current, paginationConfig.pageSize)
    
    const params: ModelVersionListParams = {
      page: paginationConfig.current,
      size: paginationConfig.pageSize,
      sort: sorter.field || 'createdAt',
      order: sorter.order === 'ascend' ? 'asc' : 'desc'
    }
    
    fetchModelList(params)
  }

  const currentTaskModels = selectedTaskId ? getTaskModels(selectedTaskId) : null

  const currentRollbackHistory = selectedModelId ? getRollbackHistory(selectedModelId) : []

  const currentTaskStatistics = statisticsTaskId ? getTaskStatistics(statisticsTaskId) : null

  // 评估结果列表数据
  const evaluationListData = Object.entries(evaluationResults)
    .flatMap(([modelId, results]) =>
      results.map((result, index) => ({
        key: result.evaluationId || `${modelId}-${index}`,
        modelId,
        ...result
      }))
    )

  // 获取当前选中的评估详情
  const currentEvaluationDetail = selectedEvaluationId 
    ? evaluationListData.find(item => item.evaluationId === selectedEvaluationId)
    : null

  // 准确率趋势图配置（仅用于10.1模型统计）
  const accuracyTrendConfig = {
    data: modelStatistics?.accuracyTrend || [],
    xField: 'roundNumber',
    yField: 'accuracy',
    smooth: true,
    point: {
      size: 5,
      shape: 'circle'
    },
    label: {
      style: {
        fill: '#aaa'
      }
    }
  }

  // 上传趋势图配置（仅用于10.1模型统计）
  const uploadTrendConfig = {
    data: modelStatistics?.uploadTrend || [],
    xField: 'date',
    yField: 'count',
    columnWidthRatio: 0.6
  }

  return (
    <div style={{ padding: '24px' }}>
      <Card title="模型版本管理">
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          {/* 模型列表标签页 */}
          <Tabs.TabPane tab={<span><CloudUploadOutlined /> 模型列表</span>} key="models">
            <div style={{ marginBottom: 16 }}>
              <Space>
                <Button
                  type="primary"
                  icon={<RocketOutlined />}
                  onClick={handleOpenBatchEvaluate}
                >
                  批量评估
                </Button>
                <Button
                  icon={<DownloadOutlined />}
                  onClick={handleBatchDownload}
                  disabled={selectedRowKeys.length === 0}
                >
                  批量下载
                </Button>
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  onClick={handleBatchDelete}
                  disabled={selectedRowKeys.length === 0}
                >
                  批量删除
                </Button>
              </Space>
            </div>
        {/* 搜索表单 */}
        <Form
          form={searchForm}
          layout="inline"
          style={{ marginBottom: 16 }}
          onFinish={handleSearch}
        >
          <Form.Item name="taskId" label="任务ID">
            <Input 
              placeholder="任务ID (可点击表格中的ID复制)" 
              style={{ width: 250 }} 
            />
          </Form.Item>
          <Form.Item name="roundNumber" label="轮次">
            <InputNumber placeholder="轮次" style={{ width: 120 }} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select placeholder="选择状态" style={{ width: 150 }} allowClear>
              <Option value="UPLOADING">上传中</Option>
              <Option value="UPLOADED">已上传</Option>
              <Option value="VALIDATING">验证中</Option>
              <Option value="VALIDATED">已验证</Option>
              <Option value="DEPLOYED">已部署</Option>
              <Option value="DEPRECATED">已废弃</Option>
              <Option value="FAILED">失败</Option>
            </Select>
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <Select placeholder="排序字段" style={{ width: 150 }} allowClear>
              <Option value="createdAt">创建时间</Option>
              <Option value="roundNumber">轮次</Option>
              <Option value="accuracy">准确率</Option>
              <Option value="loss">损失</Option>
            </Select>
          </Form.Item>
          <Form.Item name="order">
            <Select placeholder="排序方向" style={{ width: 100 }} allowClear>
              <Option value="desc">降序</Option>
              <Option value="asc">升序</Option>
            </Select>
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<FileTextOutlined />}>
                搜索
              </Button>
              <Button onClick={handleResetSearch} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>

        {/* 模型列表表格 */}
        <Table
          rowSelection={rowSelection}
          columns={columns}
          dataSource={modelList}
          loading={modelListLoading}
          rowKey="modelId"
          scroll={{ x: 1600 }}
          pagination={{
            current: pagination.page,
            pageSize: pagination.size,
            total: modelListTotal,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`
          }}
          onChange={handleTableChange}
        />
          </Tabs.TabPane>

          {/* 评估结果标签页 */}
          <Tabs.TabPane tab={<span><RocketOutlined /> 评估结果</span>} key="evaluations">
            <Table
              dataSource={evaluationListData}
              loading={evaluationResultsLoading}
              rowKey="key"
              scroll={{ x: 1200 }}
              pagination={{
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条评估记录`
              }}
              columns={[
                {
                  title: '评估ID',
                  dataIndex: 'evaluationId',
                  key: 'evaluationId',
                  width: 180,
                  ellipsis: true
                },
                {
                  title: '模型ID',
                  dataIndex: 'modelId',
                  key: 'modelId',
                  width: 180,
                  ellipsis: true
                },
                {
                  title: '任务ID',
                  dataIndex: 'taskId',
                  key: 'taskId',
                  width: 180,
                  ellipsis: true
                },
                {
                  title: '准确率',
                  dataIndex: 'accuracy',
                  key: 'accuracy',
                  width: 100,
                  render: (value: number) => value ? (value * 100).toFixed(2) + '%' : '-',
                  sorter: (a, b) => (a.accuracy || 0) - (b.accuracy || 0)
                },
                {
                  title: '损失',
                  dataIndex: 'loss',
                  key: 'loss',
                  width: 100,
                  render: (value: number) => value ? value.toFixed(6) : '-',
                  sorter: (a, b) => (a.loss || 0) - (b.loss || 0)
                },
                {
                  title: '精确率',
                  dataIndex: ['metrics', 'precision'],
                  key: 'precision',
                  width: 100,
                  render: (value: number) => value ? (value * 100).toFixed(2) + '%' : '-'
                },
                {
                  title: '召回率',
                  dataIndex: ['metrics', 'recall'],
                  key: 'recall',
                  width: 100,
                  render: (value: number) => value ? (value * 100).toFixed(2) + '%' : '-'
                },
                {
                  title: 'F1分数',
                  dataIndex: ['metrics', 'f1'],
                  key: 'f1',
                  width: 100,
                  render: (value: number) => value ? value.toFixed(4) : '-'
                },
                {
                  title: '状态',
                  dataIndex: 'status',
                  key: 'status',
                  width: 100,
                  render: (status: string) => (
                    <Tag color={status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'error' : 'processing'}>
                      {status}
                    </Tag>
                  )
                },
                {
                  title: '评估时间',
                  dataIndex: 'evaluationTime',
                  key: 'evaluationTime',
                  width: 100,
                  render: (time: number) => time ? `${time.toFixed(2)}s` : '-'
                },
                {
                  title: '测试样本数',
                  dataIndex: 'testSamples',
                  key: 'testSamples',
                  width: 100
                },
                {
                  title: '创建时间',
                  dataIndex: 'createdAt',
                  key: 'createdAt',
                  width: 180,
                  render: (time: string) => time ? new Date(time).toLocaleString() : '-'
                },
                {
                  title: '操作',
                  key: 'action',
                  fixed: 'right',
                  width: 100,
                  render: (_, record) => (
                    <Button
                      type="link"
                      size="small"
                      icon={<FileTextOutlined />}
                      onClick={() => handleViewEvaluationDetail(record.evaluationId)}
                    >
                      详情
                    </Button>
                  )
                }
              ]}
            />
          </Tabs.TabPane>

          {/* 统计分析标签页 */}
          <Tabs.TabPane tab={<span><BarChartOutlined /> 统计分析</span>} key="statistics">
            {/* 筛选条件 */}
            <div style={{ marginBottom: 16 }}>
              <Space size="large" wrap>
                <Space>
                  <span>统计类型:</span>
                  <Select
                    value={statisticsMode}
                    onChange={(value) => {
                      setStatisticsMode(value)
                      if (value === 'model') {
                        // 切换到模型统计模式
                        setStatisticsTaskId('')
                      }
                    }}
                    style={{ width: 140 }}
                  >
                    <Option value="model">模型统计</Option>
                    <Option value="task">任务进度</Option>
                  </Select>
                </Space>
                <Space>
                  <span>任务ID:</span>
                  <Input
                    placeholder={statisticsMode === 'task' ? '必填' : '可选（筛选）'}
                    style={{ width: 300 }}
                    value={statisticsTaskId}
                    onChange={(e) => setStatisticsTaskId(e.target.value)}
                    allowClear
                  />
                </Space>
                <Space>
                  <span>时间范围:</span>
                  <Select
                    value={statisticsTimeRange}
                    onChange={setStatisticsTimeRange}
                    style={{ width: 120 }}
                    disabled={statisticsMode === 'task'}
                  >
                    <Option value="7d">最近7天</Option>
                    <Option value="30d">最近30天</Option>
                    <Option value="90d">最近90天</Option>
                  </Select>
                  <span style={{ color: '#999', fontSize: '12px' }}>(仅在模型统计模式下可用)</span>
                </Space>
                <Button type="primary" onClick={() => {
                  if (statisticsMode === 'task') {
                    // 使用10.2接口：任务模型统计
                    if (!statisticsTaskId) {
                      message.warning('任务进度统计需要输入任务ID')
                      return
                    }
                    fetchTaskStatistics(statisticsTaskId)
                  } else {
                    // 使用10.1接口：模型统计（支持taskId和timeRange筛选）
                    fetchModelStatistics({ 
                      taskId: statisticsTaskId || undefined,
                      timeRange: statisticsTimeRange as '7d' | '30d' | '90d' 
                    })
                  }
                }}>
                  查询
                </Button>
                <Button onClick={() => {
                  setStatisticsTaskId('')
                  setStatisticsTimeRange('7d')
                  setStatisticsMode('model')
                  fetchModelStatistics({ timeRange: '7d' })
                }}>
                  重置
                </Button>
              </Space>
            </div>

            {statisticsMode === 'model' ? (
              // 10.1 模型统计布局
              <>
                <Row gutter={[16, 16]}>
                  <Col span={6}>
                    <Card>
                      <Statistic
                        title="模型总数"
                        value={modelStatistics?.totalModels || 0}
                        prefix={<CheckCircleOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col span={6}>
                    <Card>
                      <Statistic
                        title="平均准确率"
                        value={modelStatistics?.averageAccuracy ? (modelStatistics.averageAccuracy * 100).toFixed(2) : 0}
                        suffix="%"
                        valueStyle={{ color: '#3f8600' }}
                        prefix={<LineChartOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col span={6}>
                    <Card>
                      <Statistic
                        title="平均损失"
                        value={modelStatistics?.averageLoss?.toFixed(4) || 0}
                        valueStyle={{ color: '#cf1322' }}
                        prefix={<CloseCircleOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col span={6}>
                    <Card>
                      <Statistic
                        title="最高准确率"
                        value={modelStatistics?.accuracyTrend?.[0] ? (Math.max(...modelStatistics.accuracyTrend.map(t => t.accuracy)) * 100).toFixed(2) : 0}
                        suffix="%"
                        valueStyle={{ color: '#3f8600' }}
                      />
                    </Card>
                  </Col>
                </Row>

                <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                  <Col span={12}>
                    <Card title="准确率趋势" loading={statisticsLoading}>
                      {modelStatistics?.accuracyTrend && modelStatistics.accuracyTrend.length > 0 ? (
                        <Line {...accuracyTrendConfig} />
                      ) : (
                        <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                          暂无数据
                        </div>
                      )}
                    </Card>
                  </Col>
                  <Col span={12}>
                    <Card title="模型上传趋势" loading={statisticsLoading}>
                      {modelStatistics?.uploadTrend && modelStatistics.uploadTrend.length > 0 ? (
                        <Column {...uploadTrendConfig} />
                      ) : (
                        <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                          暂无数据
                        </div>
                      )}
                    </Card>
                  </Col>
                </Row>
              </>
            ) : (
              // 10.2 任务训练进度布局
              <>
                {currentTaskStatistics ? (
                  <>
                    {/* 任务基本信息 */}
                    <Card style={{ marginBottom: 16 }}>
                      <Descriptions title="任务信息" bordered column={2}>
                        <Descriptions.Item label="任务ID">{currentTaskStatistics.taskId}</Descriptions.Item>
                        <Descriptions.Item label="任务名称">{currentTaskStatistics.taskName}</Descriptions.Item>
                        <Descriptions.Item label="计划总轮次">{currentTaskStatistics.totalRounds}</Descriptions.Item>
                        <Descriptions.Item label="已完成轮次">
                          <span style={{ color: '#3f8600', fontWeight: 'bold' }}>
                            {currentTaskStatistics.completedRounds}
                          </span>
                        </Descriptions.Item>
                      </Descriptions>
                    </Card>

                    {/* 训练进度 */}
                    <Row gutter={[16, 16]}>
                      <Col span={24}>
                        <Card title="训练进度">
                          <div style={{ marginBottom: 16 }}>
                            <Progress 
                              percent={Number(((currentTaskStatistics.completedRounds / currentTaskStatistics.totalRounds) * 100).toFixed(1))}
                              status="active"
                              strokeColor={{
                                '0%': '#108ee9',
                                '100%': '#87d068',
                              }}
                            />
                          </div>
                          <Row gutter={16}>
                            <Col span={8}>
                              <Statistic
                                title="完成进度"
                                value={(currentTaskStatistics.completedRounds / currentTaskStatistics.totalRounds * 100).toFixed(1)}
                                suffix="%"
                                valueStyle={{ color: '#3f8600' }}
                              />
                            </Col>
                            <Col span={8}>
                              <Statistic
                                title="剩余轮次"
                                value={currentTaskStatistics.totalRounds - currentTaskStatistics.completedRounds}
                                suffix="轮"
                              />
                            </Col>
                            <Col span={8}>
                              <Statistic
                                title="完成率"
                                value={currentTaskStatistics.completedRounds}
                                suffix={`/ ${currentTaskStatistics.totalRounds} 轮`}
                              />
                            </Col>
                          </Row>
                        </Card>
                      </Col>
                    </Row>

                    {/* 性能指标 */}
                    <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                      <Col span={6}>
                        <Card>
                          <Statistic
                            title="平均准确率"
                            value={(currentTaskStatistics.performanceMetrics.averageAccuracy * 100).toFixed(2)}
                            suffix="%"
                            valueStyle={{ color: '#3f8600' }}
                            prefix={<LineChartOutlined />}
                          />
                        </Card>
                      </Col>
                      <Col span={6}>
                        <Card>
                          <Statistic
                            title="最高准确率"
                            value={(currentTaskStatistics.performanceMetrics.bestAccuracy * 100).toFixed(2)}
                            suffix="%"
                            valueStyle={{ color: '#52c41a' }}
                            prefix={<CheckCircleOutlined />}
                          />
                        </Card>
                      </Col>
                      <Col span={6}>
                        <Card>
                          <Statistic
                            title="最佳轮次"
                            value={currentTaskStatistics.performanceMetrics.bestRound}
                            suffix="轮"
                            valueStyle={{ color: '#1890ff' }}
                          />
                        </Card>
                      </Col>
                      <Col span={6}>
                        <Card>
                          <Statistic
                            title="准确率提升"
                            value={(currentTaskStatistics.performanceMetrics.accuracyImprovement * 100).toFixed(2)}
                            suffix="%"
                            valueStyle={{ color: '#faad14' }}
                            prefix={<RocketOutlined />}
                          />
                        </Card>
                      </Col>
                    </Row>

                    {/* 性能摘要 */}
                    <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                      <Col span={24}>
                        <Card title="性能分析">
                          <div style={{ padding: '20px 0' }}>
                            <p style={{ fontSize: '16px', marginBottom: 16 }}>
                              <strong>训练效果总结：</strong>
                            </p>
                            <ul style={{ fontSize: '14px', lineHeight: '2' }}>
                              <li>
                                任务已完成 <strong style={{ color: '#3f8600' }}>{currentTaskStatistics.completedRounds}</strong> 轮训练，
                                占计划总轮次的 <strong>{((currentTaskStatistics.completedRounds / currentTaskStatistics.totalRounds) * 100).toFixed(1)}%</strong>
                              </li>
                              <li>
                                平均准确率达到 <strong style={{ color: '#3f8600' }}>{(currentTaskStatistics.performanceMetrics.averageAccuracy * 100).toFixed(2)}%</strong>，
                                最高准确率为 <strong style={{ color: '#52c41a' }}>{(currentTaskStatistics.performanceMetrics.bestAccuracy * 100).toFixed(2)}%</strong>
                              </li>
                              <li>
                                第 <strong style={{ color: '#1890ff' }}>{currentTaskStatistics.performanceMetrics.bestRound}</strong> 轮达到最佳性能
                              </li>
                              <li>
                                相比初始轮次，准确率提升了 <strong style={{ color: '#faad14' }}>{(currentTaskStatistics.performanceMetrics.accuracyImprovement * 100).toFixed(2)}%</strong>
                              </li>
                            </ul>
                          </div>
                        </Card>
                      </Col>
                    </Row>
                  </>
                ) : (
                  <Card>
                    <div style={{ textAlign: 'center', padding: '60px 0', color: '#999' }}>
                      <p style={{ fontSize: '16px', marginBottom: 8 }}>请输入任务ID查看训练进度</p>
                      <p style={{ fontSize: '14px' }}>任务进度统计提供详细的训练状态和性能指标</p>
                    </div>
                  </Card>
                )}
              </>
            )}
          </Tabs.TabPane>
        </Tabs>
      </Card>


      {/* 模型详情对话框 */}
      <Modal
        title="模型版本详情"
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
            <Descriptions.Item label="轮次">
              {currentModel.roundNumber}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={getStatusColor(currentModel.status)}>{currentModel.status}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="聚合方式">
              {(currentModel as any)?.aggregationMethod || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="客户端数量">
              {(currentModel as any)?.clientCount || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="准确率">
              {(currentModel as any)?.accuracy !== undefined ? ((currentModel as any).accuracy * 100).toFixed(2) + '%' : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="损失">
              {(currentModel as any)?.loss !== undefined ? (currentModel as any).loss.toFixed(6) : '-'}
            </Descriptions.Item>
            {/* 文件相关信息 - 只在有文件时显示 */}
            {(currentModel as any)?.fileSize && (
              <>
                <Descriptions.Item label="文件大小">
                  {((currentModel as any).fileSize / 1024 / 1024).toFixed(2)} MB
                </Descriptions.Item>
                <Descriptions.Item label="文件格式">
                  {(currentModel as any)?.fileFormat || '-'}
                </Descriptions.Item>
              </>
            )}
            {/* 其他评估指标 - 只在有metrics时显示 */}
            {(currentModel as any)?.metrics && (
              <>
                <Descriptions.Item label="精确率">
                  {(currentModel as any).metrics.precision ? ((currentModel as any).metrics.precision * 100).toFixed(2) + '%' : '-'}
                </Descriptions.Item>
                <Descriptions.Item label="召回率">
                  {(currentModel as any).metrics.recall ? ((currentModel as any).metrics.recall * 100).toFixed(2) + '%' : '-'}
                </Descriptions.Item>
                <Descriptions.Item label="F1分数">
                  {(currentModel as any).metrics.f1_score ? (currentModel as any).metrics.f1_score.toFixed(4) : '-'}
                </Descriptions.Item>
              </>
            )}
            <Descriptions.Item label="创建时间" span={2}>
              {currentModel?.createdAt ? new Date(currentModel.createdAt).toLocaleString() : '-'}
            </Descriptions.Item>
            {/* 聚合完成时间 - 只在已聚合的状态下显示 */}
            {(currentModel as any)?.aggregatedAt && ['VALIDATED', 'DEPLOYED', 'DEPRECATED'].includes(currentModel.status) && (
              <Descriptions.Item label="聚合完成时间" span={2}>
                {new Date((currentModel as any).aggregatedAt).toLocaleString()}
              </Descriptions.Item>
            )}
            {/* 模型描述 */}
            {(currentModel as any)?.description && (
              <Descriptions.Item label="模型描述" span={2}>
                {(currentModel as any).description}
              </Descriptions.Item>
            )}
            {/* 模型数据 - 只在有parameters时显示 */}
            {(currentModel as any)?.parameters && (
              <Descriptions.Item label="模型数据" span={2}>
                <pre style={{ margin: 0, maxHeight: '300px', overflow: 'auto' }}>
                  {JSON.stringify((currentModel as any).parameters, null, 2)}
                </pre>
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>

      {/* 任务模型版本对话框 */}
      <Modal
        title={`任务模型版本 - ${selectedTaskId}`}
        open={taskModelsModalVisible}
        onCancel={() => setTaskModelsModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setTaskModelsModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={1000}
      >
        {currentTaskModels && (
          <>
            <Descriptions bordered column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="任务ID">
                {currentTaskModels.taskId}
              </Descriptions.Item>
              <Descriptions.Item label="任务名称">
                {currentTaskModels.taskName}
              </Descriptions.Item>
              <Descriptions.Item label="模型总数" span={2}>
                {currentTaskModels.totalModels}
              </Descriptions.Item>
            </Descriptions>

            <Table
              dataSource={currentTaskModels.versions}
              rowKey="modelId"
              size="small"
              scroll={{ y: 400 }}
              pagination={false}
              columns={[
                {
                  title: '轮次',
                  dataIndex: 'roundNumber',
                  key: 'roundNumber',
                  width: 80
                },
                {
                  title: '准确率',
                  dataIndex: 'accuracy',
                  key: 'accuracy',
                  width: 100,
                  render: (value: number | undefined) => value !== undefined ? (value * 100).toFixed(2) + '%' : '-'
                },
                {
                  title: '损失',
                  dataIndex: 'loss',
                  key: 'loss',
                  width: 100,
                  render: (value: number | undefined) => value !== undefined ? value.toFixed(6) : '-'
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
                  title: '创建时间',
                  dataIndex: 'createdAt',
                  key: 'createdAt',
                  render: (time: string) => new Date(time).toLocaleString()
                }
              ]}
            />
          </>
        )}
      </Modal>

      {/* 评估对话框 */}
      <Modal
        title="模型评估"
        open={evaluateModalVisible}
        onOk={handleEvaluate}
        onCancel={() => {
          setEvaluateModalVisible(false)
          evaluateForm.resetFields()
        }}
        width={600}
      >
        <Form form={evaluateForm} layout="vertical">
          <Form.Item
            label="模型ID"
            name="modelId"
            rules={[{ required: true, message: '请输入模型ID' }]}
          >
            <Input placeholder="模型ID" disabled />
          </Form.Item>

          <Form.Item
            label="测试数据路径"
            name="testDataPath"
            rules={[{ required: true, message: '请输入测试数据路径' }]}
          >
            <Input placeholder="/path/to/test/data" />
          </Form.Item>

          <Form.Item
            label="评估指标"
            name="metrics"
          >
            <Select mode="multiple" placeholder="选择评估指标">
              <Option value="accuracy">准确率</Option>
              <Option value="loss">损失</Option>
              <Option value="precision">精确率</Option>
              <Option value="recall">召回率</Option>
              <Option value="f1">F1分数</Option>
            </Select>
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="批次大小" name="batchSize">
                <InputNumber min={1} style={{ width: '100%' }} placeholder="32" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="计算设备" name="device">
                <Select placeholder="选择设备">
                  <Option value="cpu">CPU</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* 回滚对话框 */}
      <Modal
        title="模型回滚"
        open={rollbackModalVisible}
        onOk={handleRollback}
        onCancel={() => {
          setRollbackModalVisible(false)
          rollbackForm.resetFields()
        }}
        width={600}
      >
        <Form form={rollbackForm} layout="vertical">
          <Form.Item
            label="目标模型ID"
            name="targetModelId"
            rules={[{ required: true, message: '请输入目标模型ID' }]}
          >
            <Input placeholder="请输入要回滚到的模型ID" />
          </Form.Item>

          <Form.Item
            label="回滚原因"
            name="rollbackReason"
          >
            <TextArea rows={3} placeholder="请输入回滚原因" />
          </Form.Item>

          <Form.Item
            label="强制回滚"
            name="force"
            valuePropName="checked"
            tooltip="强制回滚将跳过安全检查"
          >
            <input type="checkbox" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 回滚历史对话框 */}
      <Modal
        title="回滚历史"
        open={rollbackHistoryModalVisible}
        onCancel={() => setRollbackHistoryModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setRollbackHistoryModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={900}
      >
        <Table
          dataSource={currentRollbackHistory}
          rowKey="rollbackId"
          size="small"
          scroll={{ y: 400 }}
          pagination={false}
          columns={[
            {
              title: '回滚ID',
              dataIndex: 'rollbackId',
              key: 'rollbackId',
              width: 180,
              ellipsis: true
            },
            {
              title: '源模型ID',
              dataIndex: 'fromModelId',
              key: 'fromModelId',
              width: 180,
              ellipsis: true
            },
            {
              title: '目标模型ID',
              dataIndex: 'toModelId',
              key: 'toModelId',
              width: 180,
              ellipsis: true
            },
            {
              title: '状态',
              dataIndex: 'status',
              key: 'status',
              width: 100,
              render: (status: string) => (
                <Tag color={status === 'COMPLETED' ? 'success' : 'error'}>
                  {status}
                </Tag>
              )
            },
            {
              title: '回滚原因',
              dataIndex: 'rollbackReason',
              key: 'rollbackReason',
              ellipsis: true
            },
            {
              title: '耗时',
              dataIndex: 'rollbackTime',
              key: 'rollbackTime',
              width: 100,
              render: (time: number) => time ? `${time.toFixed(2)}s` : '-'
            },
            {
              title: '创建时间',
              dataIndex: 'createdAt',
              key: 'createdAt',
              width: 180,
              render: (time: string) => time ? new Date(time).toLocaleString() : '-'
            }
          ]}
        />
      </Modal>

      {/* 批量评估对话框 */}
      <Modal
        title="批量模型评估"
        open={batchEvaluateModalVisible}
        onOk={handleBatchEvaluate}
        onCancel={() => {
          setBatchEvaluateModalVisible(false)
          batchEvaluateForm.resetFields()
        }}
        width={600}
      >
        <Form form={batchEvaluateForm} layout="vertical">
          <Form.Item
            label="任务ID"
            name="taskId"
            rules={[{ required: true, message: '请输入任务ID' }]}
            tooltip="将评估该任务下指定轮次的所有模型"
          >
            <Input placeholder="请输入任务ID (32位UUID格式)" />
          </Form.Item>

          <Form.Item
            label="测试数据路径"
            name="testDataPath"
            rules={[{ required: true, message: '请输入测试数据路径' }]}
          >
            <Input placeholder="/path/to/test/data" />
          </Form.Item>

          <Form.Item
            label="评估轮数"
            name="roundNumbers"
            tooltip="使用逗号分隔，例如: 1,5,10。留空将评估所有轮次"
          >
            <Input placeholder="1,5,10" />
          </Form.Item>

          <Form.Item
            label="评估指标"
            name="metrics"
          >
            <Select mode="multiple" placeholder="选择评估指标">
              <Option value="accuracy">准确率</Option>
              <Option value="loss">损失</Option>
              <Option value="precision">精确率</Option>
              <Option value="recall">召回率</Option>
              <Option value="f1">F1分数</Option>
            </Select>
          </Form.Item>

          <Form.Item label="批次大小" name="batchSize">
            <InputNumber min={1} style={{ width: '100%' }} placeholder="32" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 评估详情对话框 */}
      <Modal
        title="评估详情"
        open={evaluationDetailModalVisible}
        onCancel={() => {
          setEvaluationDetailModalVisible(false)
          setSelectedEvaluationId('')
        }}
        footer={[
          <Button key="close" onClick={() => {
            setEvaluationDetailModalVisible(false)
            setSelectedEvaluationId('')
          }}>
            关闭
          </Button>
        ]}
        width={700}
      >
        {currentEvaluationDetail && (
          <div style={{ padding: '16px 0' }}>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="评估ID" span={2}>
                {currentEvaluationDetail.evaluationId}
              </Descriptions.Item>
              <Descriptions.Item label="模型ID" span={2}>
                {currentEvaluationDetail.modelId}
              </Descriptions.Item>
              <Descriptions.Item label="任务ID" span={2}>
                {currentEvaluationDetail.taskId || '-'}
              </Descriptions.Item>
              
              <Descriptions.Item label="准确率" span={1}>
                <span style={{ color: '#52c41a', fontWeight: 'bold' }}>
                  {(currentEvaluationDetail.accuracy * 100).toFixed(2)}%
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="损失" span={1}>
                <span style={{ color: '#1890ff', fontWeight: 'bold' }}>
                  {currentEvaluationDetail.loss.toFixed(6)}
                </span>
              </Descriptions.Item>
              
              <Descriptions.Item label="精确率" span={1}>
                {currentEvaluationDetail.metrics?.precision 
                  ? (currentEvaluationDetail.metrics.precision * 100).toFixed(2) + '%' 
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="召回率" span={1}>
                {currentEvaluationDetail.metrics?.recall 
                  ? (currentEvaluationDetail.metrics.recall * 100).toFixed(2) + '%' 
                  : '-'}
              </Descriptions.Item>
              
              <Descriptions.Item label="F1分数" span={2}>
                {currentEvaluationDetail.metrics?.f1 
                  ? currentEvaluationDetail.metrics.f1.toFixed(4) 
                  : '-'}
              </Descriptions.Item>
              
              <Descriptions.Item label="评估状态" span={1}>
                <Tag color={currentEvaluationDetail.status === 'COMPLETED' ? 'success' : currentEvaluationDetail.status === 'FAILED' ? 'error' : 'processing'}>
                  {currentEvaluationDetail.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="评估时间" span={1}>
                {currentEvaluationDetail.evaluationTime 
                  ? `${currentEvaluationDetail.evaluationTime.toFixed(2)}秒` 
                  : '-'}
              </Descriptions.Item>
              
              <Descriptions.Item label="测试样本数" span={1}>
                {currentEvaluationDetail.testSamples?.toLocaleString() || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="批次大小" span={1}>
                {currentEvaluationDetail.batchSize || '-'}
              </Descriptions.Item>
              
              <Descriptions.Item label="测试数据路径" span={2}>
                {currentEvaluationDetail.testDataPath || '-'}
              </Descriptions.Item>
              
              <Descriptions.Item label="计算设备" span={1}>
                <Tag color="blue">{currentEvaluationDetail.device?.toUpperCase() || 'CPU'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间" span={1}>
                {currentEvaluationDetail.createdAt 
                  ? new Date(currentEvaluationDetail.createdAt).toLocaleString() 
                  : '-'}
              </Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default VersionManagementPage

