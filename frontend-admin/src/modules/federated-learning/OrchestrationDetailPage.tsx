/**
 * 联邦学习流程编排详情页面
 * 显示编排的详细信息、实时状态、阶段进度、性能指标等
 * 
 * @author FedUWAComm Team
 * @version 1.4.0
 */

import React, { useEffect, useState, useCallback } from 'react'
import { 
  Card, 
  Row, 
  Col, 
  Descriptions, 
  Tag, 
  Progress, 
  Button, 
  Space, 
  Tabs, 
  Table,
  Statistic,
  Alert,
  Timeline,
  message,
  Spin,
  Empty,
  Modal,
  Tooltip,
  Badge,
  Steps
} from 'antd'
import { 
  PlayCircleOutlined,
  PauseCircleOutlined,
  StopOutlined,
  ReloadOutlined,
  ArrowLeftOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  LoadingOutlined,
  LineChartOutlined,
  BarChartOutlined,
  DatabaseOutlined,
  CloudServerOutlined,
  TeamOutlined,
  ThunderboltOutlined
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { useOrchestration } from '@/store/federated-task/useFederatedTaskStore'

// 临时类型定义
interface WorkflowStage {
  name: string
  status: string
  startedAt?: string
  completedAt?: string
  duration?: string
  result?: any
  currentRound?: {
    roundNumber: number
    status: string
    participatingVms: number
    submittedModels: number
    aggregationProgress: number
  }
}

interface OrchestrationWorkflow {
  orchestrationId: string
  taskId: string
  status: string
  startedAt: string
  completedAt?: string
  lastUpdated: string
  currentStage: string
  participatingVms?: number
  duration?: string
  progress?: {
    overallProgress: number
    stageProgress: number
    currentRound: number
    totalRounds: number
  }
  stageDetails?: WorkflowStage[]
  performanceMetrics?: {
    averageRoundDuration: string
    dataTransferSpeed: string
    aggregationEfficiency: number
    resourceUtilization: {
      cpu: number
      memory: number
      network: number
    }
  }
  resourceAllocation?: {
    allocatedMemory: string
    allocatedCpuCores: number
    allocatedBandwidth: string
    participatingVms: string[]
  }
}
import './OrchestrationDetailPage.css'

const { TabPane } = Tabs
const { Step } = Steps

// 编排状态配置
const ORCHESTRATION_STATUS_CONFIG = {
  CREATED: { color: 'blue', text: '已创建', icon: <ClockCircleOutlined /> },
  STARTED: { color: 'cyan', text: '已启动', icon: <PlayCircleOutlined /> },
  IN_PROGRESS: { color: 'green', text: '执行中', icon: <LoadingOutlined spin /> },
  PAUSED: { color: 'orange', text: '已暂停', icon: <PauseCircleOutlined /> },
  COMPLETED: { color: 'success', text: '已完成', icon: <CheckCircleOutlined /> },
  FAILED: { color: 'error', text: '执行失败', icon: <ExclamationCircleOutlined /> },
  TERMINATED: { color: 'default', text: '已终止', icon: <StopOutlined /> }
}

// 工作流阶段配置
const WORKFLOW_STAGE_CONFIG = {
  INITIAL_MODEL_GENERATION: { name: '初始模型生成', icon: <ThunderboltOutlined />, color: '#1890ff' },
  DATA_DISTRIBUTION: { name: '数据分发', icon: <DatabaseOutlined />, color: '#52c41a' },
  MODEL_DISTRIBUTION: { name: '模型分发', icon: <CloudServerOutlined />, color: '#faad14' },
  FEDERATED_TRAINING: { name: '联邦训练', icon: <TeamOutlined />, color: '#722ed1' },
  FINAL_AGGREGATION: { name: '最终聚合', icon: <BarChartOutlined />, color: '#eb2f96' }
}

const OrchestrationDetailPage: React.FC = () => {
  const { orchestrationId } = useParams<{ orchestrationId: string }>()
  const navigate = useNavigate()
  
  // 使用Hook获取状态和操作
  const {
    currentOrchestration,
    currentOrchestrationLoading,
    currentOrchestrationError,
    fetchOrchestrationStatus,
    pauseOrchestration,
    resumeOrchestration,
    terminateOrchestration,
    setCurrentOrchestration,
    canPauseOrchestration,
    canResumeOrchestration,
    canTerminateOrchestration,
    isOrchestrationOperating,
    getOrchestrationTimeline,
    getOrchestrationAnalytics
  } = useOrchestration()

  // 本地状态
  const [activeTab, setActiveTab] = useState<string>('overview')
  const [autoRefresh, setAutoRefresh] = useState<boolean>(false)
  const [refreshInterval, setRefreshInterval] = useState<NodeJS.Timeout | null>(null)

  // 初始化加载编排详情
  useEffect(() => {
    if (orchestrationId) {
      fetchOrchestrationStatus(orchestrationId, { includeDetails: true, includeMetrics: true })
    }
  }, [orchestrationId, fetchOrchestrationStatus])

  // 自动刷新逻辑
  useEffect(() => {
    if (autoRefresh && orchestrationId && currentOrchestration?.status === 'IN_PROGRESS') {
      const interval = setInterval(() => {
        fetchOrchestrationStatus(orchestrationId, { includeDetails: true, includeMetrics: true, refresh: true })
      }, 10000) // 每10秒刷新一次
      
      setRefreshInterval(interval)
      
      return () => {
        if (interval) {
          clearInterval(interval)
        }
      }
    } else if (refreshInterval) {
      clearInterval(refreshInterval)
      setRefreshInterval(null)
    }
  }, [autoRefresh, orchestrationId, currentOrchestration?.status, fetchOrchestrationStatus])

  // 手动刷新
  const handleRefresh = useCallback(() => {
    if (orchestrationId) {
      fetchOrchestrationStatus(orchestrationId, { includeDetails: true, includeMetrics: true, refresh: true })
    }
  }, [orchestrationId, fetchOrchestrationStatus])

  // 编排操作处理函数
  const handlePauseOrchestration = useCallback(async () => {
    if (!currentOrchestration) return
    
    try {
      const result = await pauseOrchestration(currentOrchestration.orchestrationId, {
        reason: '用户主动暂停',
        pauseMode: 'GRACEFUL',
        waitForCurrentRound: true,
        preserveState: true,
        notifyParticipants: true
      })
      if (result.success) {
        message.success('编排暂停成功')
        handleRefresh()
      } else {
        message.error(result.error || '编排暂停失败')
      }
    } catch (error) {
      message.error('编排暂停失败')
    }
  }, [currentOrchestration, pauseOrchestration, handleRefresh])

  const handleResumeOrchestration = useCallback(async () => {
    if (!currentOrchestration) return
    
    try {
      const result = await resumeOrchestration(currentOrchestration.orchestrationId, {
        resumeFromSnapshot: true,
        validateState: true,
        notifyParticipants: true
      })
      if (result.success) {
        message.success('编排恢复成功')
        handleRefresh()
      } else {
        message.error(result.error || '编排恢复失败')
      }
    } catch (error) {
      message.error('编排恢复失败')
    }
  }, [currentOrchestration, resumeOrchestration, handleRefresh])

  const handleTerminateOrchestration = useCallback(async () => {
    if (!currentOrchestration) return
    
    Modal.confirm({
      title: '确认终止编排',
      icon: <ExclamationCircleOutlined />,
      content: `确定要终止编排"${currentOrchestration.orchestrationId}"吗？终止后可以查看部分结果。`,
      okText: '确认终止',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const result = await terminateOrchestration(currentOrchestration.orchestrationId, {
            force: false,
            cleanup: true,
            saveResults: true
          })
          if (result.success) {
            message.success('编排终止成功')
            handleRefresh()
          } else {
            message.error(result.error || '编排终止失败')
          }
        } catch (error) {
          message.error('编排终止失败')
        }
      }
    })
  }, [currentOrchestration, terminateOrchestration, handleRefresh])

  // 获取阶段进度数据
  const getStageProgressData = useCallback(() => {
    if (!currentOrchestration?.stageDetails) return []
    
    return currentOrchestration.stageDetails.map((stage, index) => ({
      stage: WORKFLOW_STAGE_CONFIG[stage.name as keyof typeof WORKFLOW_STAGE_CONFIG]?.name || stage.name,
      progress: stage.status === 'COMPLETED' ? 100 : 
               stage.status === 'IN_PROGRESS' ? 50 : 0,
      status: stage.status,
      duration: stage.duration || '00:00:00'
    }))
  }, [currentOrchestration])

  // 获取性能指标数据
  const getPerformanceData = useCallback(() => {
    if (!currentOrchestration?.performanceMetrics) return []
    
    const metrics = currentOrchestration.performanceMetrics
    return [
      { metric: 'CPU使用率', value: metrics.resourceUtilization?.cpu || 0, unit: '%' },
      { metric: '内存使用率', value: metrics.resourceUtilization?.memory || 0, unit: '%' },
      { metric: '网络使用率', value: metrics.resourceUtilization?.network || 0, unit: '%' },
      { metric: '聚合效率', value: metrics.aggregationEfficiency || 0, unit: '%' }
    ]
  }, [currentOrchestration])

  if (currentOrchestrationLoading) {
    return (
      <div className="orchestration-detail-page">
        <Card>
          <div style={{ textAlign: 'center', padding: '50px 0' }}>
            <Spin size="large" />
            <div style={{ marginTop: 16 }}>加载编排详情中...</div>
          </div>
        </Card>
      </div>
    )
  }

  if (currentOrchestrationError || !currentOrchestration) {
    return (
      <div className="orchestration-detail-page">
        <Card>
          <div style={{ textAlign: 'center', padding: '50px 0' }}>
            <Empty 
              description={currentOrchestrationError || '编排不存在'}
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            >
              <Button type="primary" onClick={() => navigate('/federated-learning/orchestrations')}>
                返回编排列表
              </Button>
            </Empty>
          </div>
        </Card>
      </div>
    )
  }

  const statusConfig = ORCHESTRATION_STATUS_CONFIG[currentOrchestration.status as keyof typeof ORCHESTRATION_STATUS_CONFIG]
  const operating = isOrchestrationOperating(currentOrchestration.orchestrationId)
  const stageProgressData = getStageProgressData()
  const performanceData = getPerformanceData()

  return (
    <div className="orchestration-detail-page">
      {/* 页面头部 */}
      <Card className="header-card">
        <div className="page-header">
          <div className="header-left">
            <Button 
              type="text" 
              icon={<ArrowLeftOutlined />} 
              onClick={() => navigate('/federated-learning/orchestrations')}
            >
              返回列表
            </Button>
            <div className="orchestration-title">
              <h2>流程编排详情</h2>
              <div className="orchestration-meta">
                <Tag color={statusConfig.color}>
                  {statusConfig.icon} {statusConfig.text}
                </Tag>
                <span className="orchestration-id">ID: {currentOrchestration.orchestrationId}</span>
              </div>
            </div>
          </div>
          
          <div className="header-actions">
            <Space>
              <Tooltip title={autoRefresh ? '关闭自动刷新' : '开启自动刷新'}>
                <Button
                  type={autoRefresh ? 'primary' : 'default'}
                  icon={<ReloadOutlined spin={autoRefresh} />}
                  onClick={() => setAutoRefresh(!autoRefresh)}
                >
                  {autoRefresh ? '自动刷新' : '手动刷新'}
                </Button>
              </Tooltip>
              
              <Button 
                icon={<ReloadOutlined />} 
                onClick={handleRefresh}
                loading={currentOrchestrationLoading}
              >
                刷新
              </Button>
              
              {canPauseOrchestration(currentOrchestration) && (
                <Button
                  icon={<PauseCircleOutlined />}
                  onClick={handlePauseOrchestration}
                  loading={operating}
                >
                  暂停编排
                </Button>
              )}
              
              {canResumeOrchestration(currentOrchestration) && (
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={handleResumeOrchestration}
                  loading={operating}
                >
                  恢复编排
                </Button>
              )}
              
              {canTerminateOrchestration(currentOrchestration) && (
                <Button
                  danger
                  icon={<StopOutlined />}
                  onClick={handleTerminateOrchestration}
                  loading={operating}
                >
                  终止编排
                </Button>
              )}
            </Space>
          </div>
        </div>
      </Card>

      {/* 编排概览统计 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="总体进度"
              value={currentOrchestration.progress?.overallProgress || 0}
              suffix="%"
              prefix={<LineChartOutlined />}
            />
            <Progress 
              percent={currentOrchestration.progress?.overallProgress || 0}
              size="small" 
              status={currentOrchestration.status === 'IN_PROGRESS' ? 'active' : 'normal'}
              style={{ marginTop: 8 }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="当前轮次"
              value={currentOrchestration.progress?.currentRound || 0}
              suffix={`/ ${currentOrchestration.progress?.totalRounds || 0}`}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
                    <Statistic
                      title="参与节点"
                      value={(currentOrchestration as any).participatingVms || 0}
                      suffix="个"
                      prefix={<TeamOutlined />}
                    />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="执行时长"
              value={(currentOrchestration as any).duration || '00:00:00'}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* 工作流阶段进度 */}
      <Card style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 16 }}>
          <h3>工作流阶段进度</h3>
        </div>
        <Steps 
          current={currentOrchestration.stageDetails?.findIndex(s => s.status === 'IN_PROGRESS') || 0}
          size="small"
        >
          {currentOrchestration.stageDetails?.map((stage, index) => {
            const stageConfig = WORKFLOW_STAGE_CONFIG[stage.name as keyof typeof WORKFLOW_STAGE_CONFIG]
            return (
              <Step
                key={index}
                title={stageConfig?.name || stage.name}
                description={stage.duration}
                status={
                  stage.status === 'COMPLETED' ? 'finish' :
                  stage.status === 'IN_PROGRESS' ? 'process' :
                  stage.status === 'FAILED' ? 'error' : 'wait'
                }
                icon={stageConfig?.icon}
              />
            )
          })}
        </Steps>
      </Card>

      {/* 详细内容标签页 */}
      <Card>
        <Tabs activeKey={activeTab} onChange={setActiveTab} size="large">
          {/* 基本信息 */}
          <TabPane tab="基本信息" key="overview">
            <Row gutter={[24, 24]}>
              <Col xs={24} lg={12}>
                <Card title="编排信息" size="small">
                  <Descriptions column={1} size="small">
                    <Descriptions.Item label="编排ID">{currentOrchestration.orchestrationId}</Descriptions.Item>
                    <Descriptions.Item label="关联任务ID">
                      <Button 
                        type="link" 
                        onClick={() => navigate(`/federated-learning/tasks/${currentOrchestration.taskId}`)}
                        style={{ padding: 0, height: 'auto' }}
                      >
                        {currentOrchestration.taskId}
                      </Button>
                    </Descriptions.Item>
                    <Descriptions.Item label="当前阶段">
                      <Tag color="blue">
                        {WORKFLOW_STAGE_CONFIG[currentOrchestration.currentStage as keyof typeof WORKFLOW_STAGE_CONFIG]?.name || currentOrchestration.currentStage}
                      </Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="开始时间">{new Date(currentOrchestration.startedAt).toLocaleString('zh-CN')}</Descriptions.Item>
                    <Descriptions.Item label="完成时间">
                      {currentOrchestration.completedAt ? new Date(currentOrchestration.completedAt).toLocaleString('zh-CN') : '未完成'}
                    </Descriptions.Item>
                    <Descriptions.Item label="最后更新">{new Date(currentOrchestration.lastUpdated).toLocaleString('zh-CN')}</Descriptions.Item>
                  </Descriptions>
                </Card>
              </Col>
              
              <Col xs={24} lg={12}>
                <Card title="资源分配" size="small">
                  <Descriptions column={1} size="small">
                    <Descriptions.Item label="分配内存">{currentOrchestration.resourceAllocation?.allocatedMemory || 'N/A'}</Descriptions.Item>
                    <Descriptions.Item label="分配CPU核心">{currentOrchestration.resourceAllocation?.allocatedCpuCores || 'N/A'}核</Descriptions.Item>
                    <Descriptions.Item label="分配带宽">{currentOrchestration.resourceAllocation?.allocatedBandwidth || 'N/A'}</Descriptions.Item>
                    <Descriptions.Item label="参与虚拟机">
                      <div>
                        {currentOrchestration.resourceAllocation?.participatingVms?.map(vmId => (
                          <Tag key={vmId} style={{ margin: '2px' }}>{vmId}</Tag>
                        ))}
                      </div>
                    </Descriptions.Item>
                  </Descriptions>
                </Card>
              </Col>
            </Row>
          </TabPane>

          {/* 阶段详情 */}
          <TabPane tab="阶段详情" key="stages">
            <div className="stages-detail">
              {currentOrchestration.stageDetails?.map((stage, index) => {
                const stageConfig = WORKFLOW_STAGE_CONFIG[stage.name as keyof typeof WORKFLOW_STAGE_CONFIG]
                return (
                  <Card key={index} size="small" style={{ marginBottom: 16 }}>
                    <div className="stage-header">
                      <div className="stage-info">
                        <h4>
                          {stageConfig?.icon}
                          <span style={{ marginLeft: 8 }}>{stageConfig?.name || stage.name}</span>
                        </h4>
                        <Tag color={
                          stage.status === 'COMPLETED' ? 'success' :
                          stage.status === 'IN_PROGRESS' ? 'processing' :
                          stage.status === 'FAILED' ? 'error' : 'default'
                        }>
                          {stage.status === 'COMPLETED' ? '已完成' :
                           stage.status === 'IN_PROGRESS' ? '进行中' :
                           stage.status === 'FAILED' ? '失败' : '等待中'}
                        </Tag>
                      </div>
                      <div className="stage-metrics">
                        {stage.duration && <span>耗时: {stage.duration}</span>}
                      </div>
                    </div>
                    
                    {stage.result && (
                      <div className="stage-result">
                        <Descriptions size="small" column={2}>
                          {Object.entries(stage.result).map(([key, value]) => (
                            <Descriptions.Item key={key} label={key}>
                              {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                            </Descriptions.Item>
                          ))}
                        </Descriptions>
                      </div>
                    )}
                    
                    {stage.name === 'FEDERATED_TRAINING' && (stage as any).currentRound && (
                      <div className="training-rounds">
                        <h5>当前训练轮次</h5>
                        <Timeline>
                          <Timeline.Item
                            color="blue"
                          >
                            <div>
                              <strong>轮次 {(stage as any).currentRound.roundNumber}</strong>
                              <Tag style={{ marginLeft: 8 }}>
                                {(stage as any).currentRound.status === 'MODEL_AGGREGATION' ? '模型聚合中' : (stage as any).currentRound.status}
                              </Tag>
                            </div>
                            <div>参与节点: {(stage as any).currentRound.participatingVms}个</div>
                            <div>已提交模型: {(stage as any).currentRound.submittedModels}个</div>
                            <div>聚合进度: {(stage as any).currentRound.aggregationProgress}%</div>
                          </Timeline.Item>
                        </Timeline>
                      </div>
                    )}
                  </Card>
                )
              })}
            </div>
          </TabPane>

          {/* 性能指标 */}
          <TabPane tab="性能指标" key="performance">
            <Row gutter={[24, 24]}>
              {/* 实时性能指标 */}
              <Col xs={24}>
                <Card title="实时性能指标" size="small">
                  {currentOrchestration.performanceMetrics ? (
                    <Row gutter={[16, 16]}>
                      <Col xs={12} sm={8} md={6}>
                        <Statistic
                          title="平均轮次时长"
                          value={currentOrchestration.performanceMetrics.averageRoundDuration || '00:00:00'}
                        />
                      </Col>
                      <Col xs={12} sm={8} md={6}>
                        <Statistic
                          title="数据传输速度"
                          value={currentOrchestration.performanceMetrics.dataTransferSpeed || '0MB/s'}
                        />
                      </Col>
                      <Col xs={12} sm={8} md={6}>
                        <Statistic
                          title="聚合效率"
                          value={currentOrchestration.performanceMetrics.aggregationEfficiency || 0}
                          suffix="%"
                        />
                      </Col>
                    </Row>
                  ) : (
                    <Empty description="暂无性能指标数据" />
                  )}
                </Card>
              </Col>

              {/* 资源使用率图表 */}
              {performanceData.length > 0 && (
                <Col xs={24}>
                  <Card title="资源使用率" size="small">
                    <div style={{ padding: '16px 0' }}>
                      {performanceData.map((item, index) => (
                        <div key={index} style={{ marginBottom: '16px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                            <span style={{ fontWeight: '500' }}>{item.metric}</span>
                            <span style={{ color: '#1890ff', fontWeight: '600' }}>{item.value}%</span>
                          </div>
                          <Progress 
                            percent={item.value} 
                            strokeColor={
                              item.metric === 'CPU使用率' ? '#1890ff' :
                              item.metric === '内存使用率' ? '#52c41a' :
                              item.metric === '网络使用率' ? '#faad14' : '#722ed1'
                            }
                            showInfo={false}
                          />
                        </div>
                      ))}
                    </div>
                    {/* Column component was replaced with Progress bars above */}
                  </Card>
                </Col>
              )}
            </Row>
          </TabPane>

          {/* 快捷操作 */}
          <TabPane tab="快捷操作" key="actions">
            <Row gutter={[16, 16]}>
              <Col xs={24} md={12}>
                <Card title="编排控制" size="small">
                  <Space direction="vertical" style={{ width: '100%' }}>
                    {canPauseOrchestration(currentOrchestration) && (
                      <Button 
                        block 
                        icon={<PauseCircleOutlined />}
                        onClick={handlePauseOrchestration}
                        loading={operating}
                      >
                        暂停编排
                      </Button>
                    )}
                    
                    {canResumeOrchestration(currentOrchestration) && (
                      <Button 
                        block 
                        type="primary"
                        icon={<PlayCircleOutlined />}
                        onClick={handleResumeOrchestration}
                        loading={operating}
                      >
                        恢复编排
                      </Button>
                    )}
                    
                    {canTerminateOrchestration(currentOrchestration) && (
                      <Button 
                        block 
                        danger
                        icon={<StopOutlined />}
                        onClick={handleTerminateOrchestration}
                        loading={operating}
                      >
                        终止编排
                      </Button>
                    )}
                  </Space>
                </Card>
              </Col>
              
              <Col xs={24} md={12}>
                <Card title="分析工具" size="small">
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <Button 
                      block 
                      icon={<LineChartOutlined />}
                      onClick={() => navigate(`/federated-learning/orchestrations/${currentOrchestration.orchestrationId}/timeline`)}
                    >
                      查看时间线
                    </Button>
                    
                    <Button 
                      block 
                      icon={<BarChartOutlined />}
                      onClick={() => navigate(`/federated-learning/orchestrations/${currentOrchestration.orchestrationId}/analytics`)}
                    >
                      性能分析
                    </Button>
                    
                    <Button 
                      block 
                      icon={<ReloadOutlined />}
                      onClick={handleRefresh}
                      loading={currentOrchestrationLoading}
                    >
                      刷新状态
                    </Button>
                  </Space>
                </Card>
              </Col>
            </Row>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  )
}

export default OrchestrationDetailPage
