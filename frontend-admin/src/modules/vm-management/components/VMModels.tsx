/**
 * VM本地模型管理组件
 * 显示和管理虚拟机的本地模型数据，包括模型列表、趋势分析、最佳/离群模型等
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Input, 
  Select, 
  Tabs,
  Row,
  Col,
  Statistic,
  Tag,
  message,
  Modal,
  Descriptions,
  Empty
} from 'antd'
import { 
  ArrowLeftOutlined,
  ReloadOutlined,
  SearchOutlined,
  LineChartOutlined,
  TrophyOutlined,
  EyeOutlined
} from '@ant-design/icons'
import { Line } from '@ant-design/plots'
import { useVM } from '@/store/vm'
import type { VirtualMachine, VMRoundModel } from '@/api/vm'
import type { ColumnsType } from 'antd/es/table'

const { Search } = Input
const { Option } = Select
const { TabPane } = Tabs

interface VMModelsProps {
  vm: VirtualMachine
  onBack: () => void
}

const VMModels: React.FC<VMModelsProps> = ({ vm, onBack }) => {
  const [activeTab, setActiveTab] = useState('list')
  const [taskIdFilter, setTaskIdFilter] = useState<string>('')
  const [roundNumberFilter, setRoundNumberFilter] = useState<number | undefined>()
  const [selectedModel, setSelectedModel] = useState<VMRoundModel | null>(null)
  const [modelDetailVisible, setModelDetailVisible] = useState(false)
  const [trendMetric, setTrendMetric] = useState<string>('accuracy')
  const [bestMetric, setBestMetric] = useState<string>('accuracy')
  const [bestType, setBestType] = useState<'best' | 'outlier'>('best')

  const {
    vmRoundModels,
    vmRoundModelsLoading,
    vmRoundModelsError,
    vmRoundModelsPagination,
    currentVMRoundModel,
    currentVMRoundModelLoading,
    vmModelTrends,
    vmModelBests,
    fetchVMRoundModels,
    fetchVMRoundModelDetail,
    fetchVMModelTrend,
    fetchVMModelBest,
    setVMRoundModelsPagination,
    setVMRoundModelsQueryParams,
    getVMModelTrend,
    getVMModelBest,
    isVMModelTrendLoading,
    isVMModelBestLoading
  } = useVM()

  // 初始化数据
  useEffect(() => {
    loadVMModels()
  }, [vm.vmId])

  // 加载VM本地模型数据
  const loadVMModels = async () => {
    try {
      await fetchVMRoundModels({ vmId: vm.vmId })
    } catch (error) {
      message.error('加载VM本地模型数据失败')
    }
  }

  // 处理搜索和筛选
  const handleSearch = () => {
    setVMRoundModelsQueryParams({
      vmId: vm.vmId,
      taskId: taskIdFilter || undefined,
      roundNumber: roundNumberFilter
    })
    fetchVMRoundModels({ page: 1 })
  }

  // 处理分页
  const handleTableChange = (page: number, pageSize: number) => {
    setVMRoundModelsPagination(page, pageSize)
    fetchVMRoundModels({ page, size: pageSize })
  }

  // 查看模型详情
  const handleViewDetail = async (model: VMRoundModel) => {
    try {
      await fetchVMRoundModelDetail(model.vmRoundModelId)
      setSelectedModel(model)
      setModelDetailVisible(true)
    } catch (error) {
      message.error('获取模型详情失败')
    }
  }

  // 加载趋势数据
  const loadTrendData = async (taskId: string, metric: string) => {
    if (!taskId) {
      message.warning('请先选择任务ID')
      return
    }
    
    try {
      await fetchVMModelTrend({
        taskId,
        vmId: vm.vmId,
        metric
      })
    } catch (error) {
      message.error('获取趋势数据失败')
    }
  }

  // 加载最佳/离群数据
  const loadBestData = async (taskId: string, metric: string, type: 'best' | 'outlier') => {
    if (!taskId) {
      message.warning('请先选择任务ID')
      return
    }
    
    try {
      await fetchVMModelBest({
        taskId,
        metric,
        type
      })
    } catch (error) {
      message.error('获取最佳/离群数据失败')
    }
  }

  // 表格列定义
  const columns: ColumnsType<VMRoundModel> = [
    {
      title: '模型ID',
      dataIndex: 'vmRoundModelId',
      key: 'vmRoundModelId',
      width: 200,
      ellipsis: true,
      render: (text: string) => (
        <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>{text}</span>
      )
    },
    {
      title: '任务ID',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 200,
      ellipsis: true,
      render: (text: string) => (
        <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>{text}</span>
      )
    },
    {
      title: '训练轮数',
      dataIndex: 'roundNumber',
      key: 'roundNumber',
      width: 100,
      sorter: (a, b) => a.roundNumber - b.roundNumber
    },
    {
      title: '准确率',
      key: 'accuracy',
      width: 100,
      render: (_, record: VMRoundModel) => (
        <span>{(record.metrics.accuracy * 100).toFixed(2)}%</span>
      ),
      sorter: (a, b) => a.metrics.accuracy - b.metrics.accuracy
    },
    {
      title: '损失值',
      key: 'loss',
      width: 100,
      render: (_, record: VMRoundModel) => (
        <span>{record.metrics.loss.toFixed(4)}</span>
      ),
      sorter: (a, b) => a.metrics.loss - b.metrics.loss
    },
    {
      title: '其他指标',
      key: 'otherMetrics',
      width: 200,
      render: (_, record: VMRoundModel) => {
        const otherMetrics = Object.entries(record.metrics)
          .filter(([key]) => !['accuracy', 'loss'].includes(key))
        
        if (otherMetrics.length === 0) return '-'
        
        return (
          <Space wrap>
            {otherMetrics.map(([key, value]) => (
              <Tag key={key} color="blue">
                {key}: {typeof value === 'number' ? value.toFixed(4) : value}
              </Tag>
            ))}
          </Space>
        )
      }
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (text: string) => new Date(text).toLocaleString(),
      sorter: (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      fixed: 'right',
      render: (_, record: VMRoundModel) => (
        <Button 
          type="text" 
          icon={<EyeOutlined />} 
          onClick={() => handleViewDetail(record)}
        >
          详情
        </Button>
      )
    }
  ]

  // 获取唯一的任务ID列表
  const taskIds = Array.from(new Set(vmRoundModels.map(model => model.taskId)))

  // 渲染趋势图表
  const renderTrendChart = () => {
    const trendData = getVMModelTrend(taskIdFilter, vm.vmId, trendMetric)
    
    if (!trendData) {
      return <Empty description="暂无趋势数据" />
    }

    const chartData = trendData.trend.map(item => ({
      roundNumber: item.roundNumber,
      value: item.value,
      metric: trendMetric
    }))

    const config = {
      data: chartData,
      xField: 'roundNumber',
      yField: 'value',
      point: {
        size: 5,
        shape: 'diamond'
      },
      label: {
        style: {
          fill: '#aaa'
        }
      },
      smooth: true,
      animation: {
        appear: {
          animation: 'path-in',
          duration: 1000
        }
      }
    }

    return <Line {...config} />
  }

  // 渲染最佳/离群结果
  const renderBestResult = () => {
    const bestData = getVMModelBest(taskIdFilter, bestMetric, bestType)
    
    if (!bestData) {
      return <Empty description="暂无数据" />
    }

    return (
      <Card>
        <Descriptions column={1} bordered>
          <Descriptions.Item label="模型ID">
            <span style={{ fontFamily: 'monospace' }}>{bestData.result.vmRoundModelId}</span>
          </Descriptions.Item>
          <Descriptions.Item label="训练轮数">{bestData.result.roundNumber}</Descriptions.Item>
          <Descriptions.Item label="指标值">{bestData.result.value.toFixed(4)}</Descriptions.Item>
          <Descriptions.Item label="查询类型">
            <Tag color={bestType === 'best' ? 'green' : 'orange'}>
              {bestType === 'best' ? '最佳' : '离群'}
            </Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>
    )
  }

  return (
    <div className="vm-models">
      {/* 头部操作栏 */}
      <Card className="vm-models-header" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Button 
              icon={<ArrowLeftOutlined />} 
              onClick={onBack}
              style={{ marginRight: 16 }}
            >
              返回列表
            </Button>
            <h3 style={{ display: 'inline', margin: 0 }}>
              {vm.name} - 本地模型管理
            </h3>
          </div>
          <Space>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={loadVMModels}
              loading={vmRoundModelsLoading}
            >
              刷新
            </Button>
          </Space>
        </div>
      </Card>

      {/* 标签页内容 */}
      <Card>
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          {/* 模型列表 */}
          <TabPane 
            tab={
              <span>
                <SearchOutlined />
                模型列表
              </span>
            } 
            key="list"
          >
            {/* 搜索和筛选 */}
            <Card className="vm-models-filters" style={{ marginBottom: 16 }}>
              <Row gutter={16} align="middle">
                <Col span={8}>
                  <Select
                    placeholder="选择任务ID"
                    allowClear
                    style={{ width: '100%' }}
                    value={taskIdFilter}
                    onChange={setTaskIdFilter}
                  >
                    {taskIds.map(taskId => (
                      <Option key={taskId} value={taskId}>
                        <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>
                          {taskId}
                        </span>
                      </Option>
                    ))}
                  </Select>
                </Col>
                <Col span={6}>
                  <Input
                    placeholder="训练轮数"
                    type="number"
                    value={roundNumberFilter}
                    onChange={(e) => setRoundNumberFilter(e.target.value ? Number(e.target.value) : undefined)}
                  />
                </Col>
                <Col span={4}>
                  <Button type="primary" onClick={handleSearch}>
                    搜索
                  </Button>
                </Col>
              </Row>
            </Card>

            {/* 模型列表表格 */}
            <Table
              columns={columns}
              dataSource={vmRoundModels}
              rowKey="vmRoundModelId"
              loading={vmRoundModelsLoading}
              pagination={{
                current: vmRoundModelsPagination.current,
                pageSize: vmRoundModelsPagination.size,
                total: vmRoundModelsPagination.total,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total, range) => 
                  `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
                onChange: handleTableChange,
                onShowSizeChange: handleTableChange
              }}
              scroll={{ x: 1000 }}
              size="middle"
            />
          </TabPane>

          {/* 趋势分析 */}
          <TabPane 
            tab={
              <span>
                <LineChartOutlined />
                趋势分析
              </span>
            } 
            key="trend"
          >
            <Card className="vm-trend-controls" style={{ marginBottom: 16 }}>
              <Row gutter={16} align="middle">
                <Col span={8}>
                  <Select
                    placeholder="选择任务ID"
                    style={{ width: '100%' }}
                    value={taskIdFilter}
                    onChange={setTaskIdFilter}
                  >
                    {taskIds.map(taskId => (
                      <Option key={taskId} value={taskId}>
                        <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>
                          {taskId}
                        </span>
                      </Option>
                    ))}
                  </Select>
                </Col>
                <Col span={6}>
                  <Select
                    placeholder="选择指标"
                    style={{ width: '100%' }}
                    value={trendMetric}
                    onChange={setTrendMetric}
                  >
                    <Option value="accuracy">准确率</Option>
                    <Option value="loss">损失值</Option>
                    <Option value="precision">精确率</Option>
                    <Option value="recall">召回率</Option>
                    <Option value="f1">F1分数</Option>
                  </Select>
                </Col>
                <Col span={4}>
                  <Button 
                    type="primary" 
                    onClick={() => loadTrendData(taskIdFilter, trendMetric)}
                    loading={isVMModelTrendLoading(taskIdFilter, vm.vmId, trendMetric)}
                  >
                    加载趋势
                  </Button>
                </Col>
              </Row>
            </Card>

            <Card title={`${trendMetric} 趋势图`}>
              {renderTrendChart()}
            </Card>
          </TabPane>

          {/* 最佳/离群分析 */}
          <TabPane 
            tab={
              <span>
                <TrophyOutlined />
                最佳/离群
              </span>
            } 
            key="best"
          >
            <Card className="vm-best-controls" style={{ marginBottom: 16 }}>
              <Row gutter={16} align="middle">
                <Col span={6}>
                  <Select
                    placeholder="选择任务ID"
                    style={{ width: '100%' }}
                    value={taskIdFilter}
                    onChange={setTaskIdFilter}
                  >
                    {taskIds.map(taskId => (
                      <Option key={taskId} value={taskId}>
                        <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>
                          {taskId}
                        </span>
                      </Option>
                    ))}
                  </Select>
                </Col>
                <Col span={6}>
                  <Select
                    placeholder="选择指标"
                    style={{ width: '100%' }}
                    value={bestMetric}
                    onChange={setBestMetric}
                  >
                    <Option value="accuracy">准确率</Option>
                    <Option value="loss">损失值</Option>
                    <Option value="precision">精确率</Option>
                    <Option value="recall">召回率</Option>
                    <Option value="f1">F1分数</Option>
                  </Select>
                </Col>
                <Col span={6}>
                  <Select
                    placeholder="选择类型"
                    style={{ width: '100%' }}
                    value={bestType}
                    onChange={setBestType}
                  >
                    <Option value="best">最佳</Option>
                    <Option value="outlier">离群</Option>
                  </Select>
                </Col>
                <Col span={4}>
                  <Button 
                    type="primary" 
                    onClick={() => loadBestData(taskIdFilter, bestMetric, bestType)}
                    loading={isVMModelBestLoading(taskIdFilter, bestMetric, bestType)}
                  >
                    查询
                  </Button>
                </Col>
              </Row>
            </Card>

            {renderBestResult()}
          </TabPane>
        </Tabs>
      </Card>

      {/* 模型详情模态框 */}
      <Modal
        title="模型详情"
        open={modelDetailVisible}
        onCancel={() => setModelDetailVisible(false)}
        footer={null}
        width={800}
      >
        {currentVMRoundModel && (
          <div>
            <Descriptions column={1} bordered>
              <Descriptions.Item label="模型ID">
                <span style={{ fontFamily: 'monospace' }}>
                  {currentVMRoundModel.vmRoundModelId}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="任务ID">
                <span style={{ fontFamily: 'monospace' }}>
                  {currentVMRoundModel.taskId}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="虚拟机ID">
                <span style={{ fontFamily: 'monospace' }}>
                  {currentVMRoundModel.vmId}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="训练轮数">
                {currentVMRoundModel.roundNumber}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {new Date(currentVMRoundModel.createdAt).toLocaleString()}
              </Descriptions.Item>
            </Descriptions>

            <Card title="评估指标" style={{ marginTop: 16 }}>
              <Row gutter={16}>
                {Object.entries(currentVMRoundModel.metrics).map(([key, value]) => (
                  <Col span={8} key={key}>
                    <Statistic
                      title={key}
                      value={typeof value === 'number' ? value.toFixed(4) : value}
                      precision={4}
                    />
                  </Col>
                ))}
              </Row>
            </Card>

            {currentVMRoundModel.modelJson && (
              <Card title="模型数据" style={{ marginTop: 16 }}>
                <pre style={{ 
                  background: '#f5f5f5', 
                  padding: '12px', 
                  borderRadius: '4px',
                  maxHeight: '300px',
                  overflow: 'auto',
                  fontSize: '12px'
                }}>
                  {JSON.stringify(currentVMRoundModel.modelJson, null, 2)}
                </pre>
              </Card>
            )}
          </div>
        )}
      </Modal>
    </div>
  )
}

export default VMModels










