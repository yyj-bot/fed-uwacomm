/**
 * 模型部署运维页面
 * 覆盖model-version-api-reference.md的评估、部署、回滚、统计相关接口
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
  message,
  Tag,
  Descriptions,
  Row,
  Col,
  Tabs,
  InputNumber,
  Popconfirm,
  Statistic,
  Progress,
  List,
  Tooltip
} from 'antd'
import {
  RocketOutlined,
  LineChartOutlined,
  RollbackOutlined,
  BarChartOutlined,
  FileTextOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useModel } from '@/store/model-version/useModelVersionStore'
import type {
  EvaluationRequest,
  RollbackRequest,
  StatisticsParams,
  EvaluationResult,
  RollbackInfo,
  ModelStatistics,
  TaskStatistics
} from '@/services'
import { Line, Column } from '@ant-design/plots'

const { TextArea } = Input
const { Option } = Select

const DeploymentPage: React.FC = () => {
  const {
    // 状态
    evaluationResults,
    evaluationLoading,
    rollbackHistory,
    modelStatistics,
    taskStatistics,
    statisticsLoading,
    
    // 操作方法
    evaluateModel,
    fetchEvaluationResults,
    rollbackModel,
    fetchRollbackHistory,
    fetchModelStatistics,
    fetchTaskStatistics,
    
    // 工具方法
    getEvaluationResults,
    isModelEvaluating,
    getRollbackHistory,
    getTaskStatistics
  } = useModel()

  const [activeTab, setActiveTab] = useState('evaluation')
  const [evaluateModalVisible, setEvaluateModalVisible] = useState(false)
  const [batchEvaluateModalVisible, setBatchEvaluateModalVisible] = useState(false)
  const [rollbackModalVisible, setRollbackModalVisible] = useState(false)
  const [evaluationDetailModalVisible, setEvaluationDetailModalVisible] = useState(false)
  const [rollbackHistoryModalVisible, setRollbackHistoryModalVisible] = useState(false)
  
  const [selectedModelId, setSelectedModelId] = useState<string>('')
  const [selectedTaskId, setSelectedTaskId] = useState<string>('')
  const [selectedDeploymentId, setSelectedDeploymentId] = useState<string>('')
  const [searchModelId, setSearchModelId] = useState<string>('')
  const [searchTaskId, setSearchTaskId] = useState<string>('')
  
  const [evaluateForm] = Form.useForm()
  const [batchEvaluateForm] = Form.useForm()
  const [rollbackForm] = Form.useForm()

  // 组件加载时获取统计数据和评估结果
  useEffect(() => {
    loadStatistics()
    loadInitialEvaluationResults()
  }, [])

  // 搜索评估结果
  const handleSearchEvaluationResults = async () => {
    try {
      if (searchModelId.trim()) {
        await fetchEvaluationResults(searchModelId.trim())
        message.success('评估结果查询完成')
      } else if (searchTaskId.trim()) {
        await fetchEvaluationResults() // 获取所有结果，然后过滤
        message.success('评估结果查询完成')
      } else {
        await fetchEvaluationResults()
        message.success('评估结果刷新完成')
      }
    } catch (error) {
      message.error('查询评估结果失败')
    }
  }

  // 清空搜索
  const handleClearSearch = () => {
    setSearchModelId('')
    setSearchTaskId('')
  }

  // 加载初始评估结果数据
  const loadInitialEvaluationResults = async () => {
    try {
      // 直接获取所有评估结果
      await fetchEvaluationResults()
    } catch (error) {
      console.error('加载初始评估结果失败:', error)
    }
  }


  // 评估结果列表数据
  const evaluationListData = Object.entries(evaluationResults)
    .flatMap(([modelId, results]) =>
      results.map(result => ({
        key: result.evaluationId || modelId,
        modelId,
        ...result
      }))
    )


  // 评估状态颜色
  const getEvaluationStatusColor = (status: string) => {
    const colorMap: Record<string, string> = {
      'COMPLETED': 'success',
      'RUNNING': 'processing',
      'FAILED': 'error',
      'PENDING': 'default'
    }
    return colorMap[status] || 'default'
  }

  // 评估结果表格列
  const evaluationColumns: ColumnsType<any> = [
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
      title: '准确率',
      dataIndex: ['metrics', 'accuracy'],
      key: 'accuracy',
      width: 100,
      render: (value: number) => value ? (value * 100).toFixed(2) + '%' : '-'
    },
    {
      title: '损失',
      dataIndex: ['metrics', 'loss'],
      key: 'loss',
      width: 100,
      render: (value: number) => value ? value.toFixed(6) : '-'
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
        <Tag color={getEvaluationStatusColor(status)}>{status}</Tag>
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
          onClick={() => handleViewEvaluationDetail(record.modelId, record.evaluationId)}
        >
          详情
        </Button>
      )
    }
  ]


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
        message.success('模型评估已启动')
        setEvaluateModalVisible(false)
        evaluateForm.resetFields()
        await fetchEvaluationResults(values.modelId)
      } else {
        message.error(result.error || '模型评估失败')
      }
    } catch (error) {
      console.error('评估模型失败:', error)
    }
  }

  // 处理批量评估
  const handleBatchEvaluate = async () => {
    try {
      const values = await batchEvaluateForm.validateFields()
      message.info('批量评估功能开发中')
      // TODO: 实现批量评估
    } catch (error) {
      console.error('批量评估失败:', error)
    }
  }


  // 处理模型回滚
  const handleRollback = async () => {
    try {
      const values = await rollbackForm.validateFields()
      
      const requestData: RollbackRequest = {
        deploymentId: selectedDeploymentId,
        targetModelId: values.targetModelId,
        rollbackReason: values.rollbackReason,
        force: values.force || false
      }

      const result = await rollbackModel(requestData)
      
      if (result.success) {
        message.success('模型回滚成功')
        setRollbackModalVisible(false)
        rollbackForm.resetFields()
        await fetchRollbackHistory({ deploymentId: selectedDeploymentId })
      } else {
        message.error(result.error || '模型回滚失败')
      }
    } catch (error) {
      console.error('回滚模型失败:', error)
    }
  }

  // 打开回滚对话框
  const handleOpenRollback = (deploymentId: string) => {
    setSelectedDeploymentId(deploymentId)
    setRollbackModalVisible(true)
  }

  // 查看评估详情
  const handleViewEvaluationDetail = (modelId: string, evaluationId: string) => {
    setSelectedModelId(modelId)
    setEvaluationDetailModalVisible(true)
  }


  // 查看回滚历史
  const handleViewRollbackHistory = async (deploymentId: string) => {
    setSelectedDeploymentId(deploymentId)
    setRollbackHistoryModalVisible(true)
    await fetchRollbackHistory({ deploymentId })
  }

  // 加载统计数据
  const loadStatistics = async (taskId?: string, timeRange?: string) => {
    if (taskId) {
      await fetchTaskStatistics(taskId)
    } else {
      await fetchModelStatistics({ taskId, timeRange } as StatisticsParams)
    }
  }

  const currentRollbackHistory = selectedDeploymentId ? getRollbackHistory(selectedDeploymentId) : []
  const currentEvaluationResults = selectedModelId ? getEvaluationResults(selectedModelId) : []

  // 准确率趋势图配置
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

  // 上传趋势图配置
  const uploadTrendConfig = {
    data: modelStatistics?.uploadTrend || [],
    xField: 'date',
    yField: 'count',
    columnWidthRatio: 0.6
  }

  return (
    <div style={{ padding: '24px' }}>
      <Tabs 
        activeKey={activeTab} 
        onChange={setActiveTab}
        items={[
          {
            key: 'evaluation',
            label: '性能评估',
            children: (
              <Card
                title="模型性能评估"
                extra={
                  <Space>
                    <Button
                      type="primary"
                      icon={<LineChartOutlined />}
                      onClick={() => setEvaluateModalVisible(true)}
                    >
                      评估模型
                    </Button>
                    <Button
                      icon={<BarChartOutlined />}
                      onClick={() => setBatchEvaluateModalVisible(true)}
                    >
                      批量评估
                    </Button>
                  </Space>
                }
              >
                {/* 搜索区域 */}
                <Card size="small" style={{ marginBottom: 16 }}>
                  <Space wrap>
                    <Input
                      placeholder="请输入模型ID查询评估结果"
                      value={searchModelId}
                      onChange={(e) => setSearchModelId(e.target.value)}
                      style={{ width: 250 }}
                      onPressEnter={handleSearchEvaluationResults}
                    />
                    <Input
                      placeholder="请输入任务ID查询评估结果"
                      value={searchTaskId}
                      onChange={(e) => setSearchTaskId(e.target.value)}
                      style={{ width: 250 }}
                      onPressEnter={handleSearchEvaluationResults}
                    />
                    <Button 
                      type="primary" 
                      onClick={handleSearchEvaluationResults}
                      loading={Object.values(evaluationLoading).some(Boolean)}
                    >
                      查询
                    </Button>
                    <Button onClick={handleClearSearch}>
                      清空
                    </Button>
                  </Space>
                  <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                    💡 提示：可以输入模型ID或任务ID查询对应的评估结果，留空查询所有结果
                  </div>
                </Card>

                <Table
                  columns={evaluationColumns}
                  dataSource={evaluationListData}
                  loading={Object.values(evaluationLoading).some(Boolean)}
                  scroll={{ x: 1600 }}
                  pagination={{
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 条`,
                    pageSize: 10
                  }}
                  locale={{
                    emptyText: (
                      <div style={{ textAlign: 'center', padding: '40px 0' }}>
                        <LineChartOutlined style={{ fontSize: '48px', color: '#d9d9d9', marginBottom: '16px' }} />
                        <p style={{ color: '#999', fontSize: '16px', marginBottom: '16px' }}>
                          暂无评估结果
                        </p>
                        <p style={{ color: '#666', fontSize: '14px', marginBottom: '24px' }}>
                          请先评估模型以查看性能数据，或检查是否有可用的模型数据
                        </p>
                        <Space>
                          <Button 
                            type="primary" 
                            icon={<LineChartOutlined />}
                            onClick={() => setEvaluateModalVisible(true)}
                          >
                            开始评估
                          </Button>
                          <Button 
                            icon={<ReloadOutlined />}
                            onClick={() => loadInitialEvaluationResults()}
                          >
                            刷新数据
                          </Button>
                        </Space>
                      </div>
                    )
                  }}
                />
              </Card>
            )
          },
          {
            key: 'statistics',
            label: '统计分析',
            children: (
              <Card
                title="模型统计"
                loading={statisticsLoading}
                extra={
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={() => loadStatistics()}
                  >
                    刷新统计
                  </Button>
                }
              >
                {modelStatistics ? (
                  <>
                    <Row gutter={16} style={{ marginBottom: 24 }}>
                      <Col span={6}>
                        <Statistic
                          title="模型总数"
                          value={modelStatistics.totalModels}
                          prefix={<CheckCircleOutlined />}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="平均准确率"
                          value={modelStatistics.averageAccuracy}
                          precision={4}
                          suffix="%"
                          valueStyle={{ color: '#3f8600' }}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="平均损失"
                          value={modelStatistics.averageLoss}
                          precision={6}
                          valueStyle={{ color: '#cf1322' }}
                        />
                      </Col>
                      <Col span={6}>
                        <Button
                          type="primary"
                          onClick={() => {
                            const taskId = prompt('请输入任务ID查看详细统计')
                            if (taskId) loadStatistics(taskId)
                          }}
                        >
                          查看任务统计
                        </Button>
                      </Col>
                    </Row>

                    <Row gutter={16}>
                      <Col span={12}>
                        <Card title="准确率趋势" size="small">
                          <Line {...accuracyTrendConfig} height={300} />
                        </Card>
                      </Col>
                      <Col span={12}>
                        <Card title="上传趋势" size="small">
                          <Column {...uploadTrendConfig} height={300} />
                        </Card>
                      </Col>
                    </Row>
                  </>
                ) : (
                  <div style={{ textAlign: 'center', padding: '40px 0' }}>
                    <CheckCircleOutlined style={{ fontSize: '48px', color: '#d9d9d9', marginBottom: '16px' }} />
                    <p style={{ color: '#999', fontSize: '16px' }}>
                      {statisticsLoading ? '正在加载统计数据...' : '暂无统计数据'}
                    </p>
                    <Button 
                      type="primary" 
                      icon={<ReloadOutlined />} 
                      onClick={() => loadStatistics()}
                      loading={statisticsLoading}
                    >
                      重新加载
                    </Button>
                  </div>
                )}
              </Card>
            )
          }
        ]}
      />

      {/* 评估模型对话框 */}
      <Modal
        title="评估模型"
        open={evaluateModalVisible}
        onOk={handleEvaluate}
        onCancel={() => {
          setEvaluateModalVisible(false)
          evaluateForm.resetFields()
        }}
        width={600}
      >
        <Form
          form={evaluateForm}
          layout="vertical"
          initialValues={{
            metrics: ['accuracy', 'loss', 'precision', 'recall', 'f1'],
            batchSize: 32,
            device: 'cpu'
          }}
        >
          <Form.Item
            label="模型ID"
            name="modelId"
            rules={[{ required: true, message: '请输入模型ID' }]}
          >
            <Input placeholder="请输入模型ID (32位UUID格式)" />
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
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="计算设备" name="device">
                <Select>
                  <Option value="cpu">CPU</Option>
                  <Option value="gpu">GPU</Option>
                  <Option value="cuda">CUDA</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* 批量评估对话框 */}
      <Modal
        title="批量评估模型"
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
            tooltip="使用逗号分隔，例如: 1,5,10"
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
            </Select>
          </Form.Item>

          <Form.Item label="批次大小" name="batchSize">
            <InputNumber min={1} style={{ width: '100%' }} placeholder="32" />
          </Form.Item>
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
            <Input placeholder="请输入要回滚到的模型ID (32位UUID格式)" />
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
    </div>
  )
}

export default DeploymentPage

