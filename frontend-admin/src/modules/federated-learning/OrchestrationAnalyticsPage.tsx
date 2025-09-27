/**
 * 联邦学习流程编排性能分析页面
 * 显示编排任务的性能分析报告和优化建议
 * 
 * @author FedUWAComm Team
 * @version 1.4.0
 */

import React, { useEffect, useState, useCallback } from 'react'
import { 
  Card, 
  Row, 
  Col, 
  Button, 
  Space, 
  Progress, 
  Tag, 
  Statistic, 
  Alert, 
  List,
  Descriptions,
  message,
  Spin,
  Empty,
  Tooltip,
  Badge,
  Tabs,
  Table
} from 'antd'
import { 
  ArrowLeftOutlined,
  ReloadOutlined,
  TrophyOutlined,
  ThunderboltOutlined,
  SafetyOutlined,
  RocketOutlined,
  LineChartOutlined,
  BarChartOutlined,
  DashboardOutlined,
  BulbOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { useOrchestration } from '@/store/federated-task/useFederatedTaskStore'

// 临时类型定义
interface WorkflowPerformanceAnalysis {
  orchestrationId: string
  analysisTimestamp: string
  overallPerformance: {
    score: number
    grade: string
    efficiency: number
    reliability: number
    scalability: number
  }
  stagePerformance: Record<string, {
    duration: string
    efficiency: number
    resourceUtilization: number
  }>
  resourceMetrics: {
    cpu: {
      averageUtilization: number
      peakUtilization: number
      efficiency: number
    }
    memory: {
      averageUtilization: number
      peakUtilization: number
      efficiency: number
    }
    network: {
      averageUtilization: number
      peakBandwidth: string
      efficiency: number
    }
  }
  qualityMetrics: {
    modelAccuracy: {
      initial: number
      final: number
      improvement: number
      convergenceRounds: number
    }
    trainingStability: number
    aggregationQuality: number
  }
  recommendations: Array<{
    category: string
    priority: string
    title: string
    description: string
    expectedImprovement: string
    implementation: {
      parameter: string
      currentValue: any
      recommendedValue: any
    }
  }>
  comparisons: {
    similarTasks: {
      averageDuration: string
      performanceRanking: string
      efficiencyRanking: string
    }
    historicalTrends: {
      improvementRate: number
      consistencyScore: number
    }
  }
}
import './OrchestrationAnalyticsPage.css'

const { TabPane } = Tabs

// 性能等级配置
const PERFORMANCE_GRADE_CONFIG = {
  'A+': { color: '#52c41a', text: '优秀', icon: <TrophyOutlined /> },
  'A': { color: '#52c41a', text: '良好', icon: <CheckCircleOutlined /> },
  'B+': { color: '#1890ff', text: '较好', icon: <ThunderboltOutlined /> },
  'B': { color: '#faad14', text: '一般', icon: <ExclamationCircleOutlined /> },
  'C': { color: '#ff4d4f', text: '较差', icon: <ExclamationCircleOutlined /> },
  'D': { color: '#ff4d4f', text: '差', icon: <ExclamationCircleOutlined /> }
}

// 建议优先级配置
const PRIORITY_CONFIG = {
  HIGH: { color: 'red', text: '高' },
  MEDIUM: { color: 'orange', text: '中' },
  LOW: { color: 'green', text: '低' }
}

// 建议分类配置
const CATEGORY_CONFIG = {
  PERFORMANCE: { icon: <RocketOutlined />, text: '性能优化', color: '#1890ff' },
  EFFICIENCY: { icon: <ThunderboltOutlined />, text: '效率提升', color: '#52c41a' },
  RELIABILITY: { icon: <SafetyOutlined />, text: '可靠性', color: '#faad14' },
  SCALABILITY: { icon: <DashboardOutlined />, text: '扩展性', color: '#722ed1' }
}

const OrchestrationAnalyticsPage: React.FC = () => {
  const { orchestrationId } = useParams<{ orchestrationId: string }>()
  const navigate = useNavigate()
  
  // 使用Hook获取状态和操作
  const {
    orchestrationAnalytics,
    orchestrationAnalyticsLoading,
    orchestrationAnalyticsError,
    fetchOrchestrationAnalytics,
    getOrchestrationAnalytics
  } = useOrchestration()

  // 本地状态
  const [activeTab, setActiveTab] = useState<string>('overview')

  // 获取分析数据
  const analytics = orchestrationId ? getOrchestrationAnalytics(orchestrationId) : null
  const isLoading = orchestrationId ? orchestrationAnalyticsLoading[orchestrationId] : false
  const error = orchestrationId ? orchestrationAnalyticsError[orchestrationId] : null

  // 初始化加载分析数据
  useEffect(() => {
    if (orchestrationId) {
      fetchOrchestrationAnalytics(orchestrationId, {
        includeRecommendations: true,
        metricsLevel: 'DETAILED'
      })
    }
  }, [orchestrationId, fetchOrchestrationAnalytics])

  // 刷新分析数据
  const handleRefresh = useCallback(() => {
    if (orchestrationId) {
      fetchOrchestrationAnalytics(orchestrationId, {
        includeRecommendations: true,
        metricsLevel: 'DETAILED'
      })
    }
  }, [orchestrationId, fetchOrchestrationAnalytics])

  // 获取资源使用率数据
  const getResourceData = useCallback(() => {
    if (!analytics?.resourceMetrics) return []
    
    const { cpu, memory, network } = analytics.resourceMetrics
    return [
      { resource: 'CPU', average: cpu?.averageUtilization || 0, peak: cpu?.peakUtilization || 0, efficiency: cpu?.efficiency || 0 },
      { resource: '内存', average: memory?.averageUtilization || 0, peak: memory?.peakUtilization || 0, efficiency: memory?.efficiency || 0 },
      { resource: '网络', average: network?.averageUtilization || 0, peak: network?.peakBandwidth ? parseFloat(network.peakBandwidth.replace('MB/s', '')) : 0, efficiency: network?.efficiency || 0 }
    ]
  }, [analytics])

  // 获取阶段性能数据
  const getStagePerformanceData = useCallback(() => {
    if (!analytics?.stagePerformance) return []
    
    return Object.entries(analytics.stagePerformance).map(([stage, perf]: [string, any]) => ({
      stage: stage.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()),
      duration: perf.duration || '00:00:00',
      efficiency: perf.efficiency || 0,
      resourceUtilization: perf.resourceUtilization || 0
    }))
  }, [analytics])

  // 获取性能雷达图数据
  const getRadarData = useCallback(() => {
    if (!analytics?.overallPerformance) return []
    
    const { efficiency, reliability, scalability } = analytics.overallPerformance
    return [
      { metric: '效率', value: efficiency || 0 },
      { metric: '可靠性', value: reliability || 0 },
      { metric: '扩展性', value: scalability || 0 },
      { metric: '整体评分', value: analytics.overallPerformance.score || 0 }
    ]
  }, [analytics])

  // 建议表格列定义
  const recommendationColumns = [
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
      render: (priority: keyof typeof PRIORITY_CONFIG) => (
        <Tag color={PRIORITY_CONFIG[priority]?.color}>
          {PRIORITY_CONFIG[priority]?.text}
        </Tag>
      )
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      render: (category: keyof typeof CATEGORY_CONFIG) => {
        const config = CATEGORY_CONFIG[category]
        return (
          <Tag color={config?.color}>
            {config?.icon} {config?.text}
          </Tag>
        )
      }
    },
    {
      title: '建议标题',
      dataIndex: 'title',
      key: 'title',
      width: 200,
      ellipsis: true
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true
    },
    {
      title: '预期提升',
      dataIndex: 'expectedImprovement',
      key: 'expectedImprovement',
      width: 150,
      render: (improvement: string) => (
        <Tag color="green">{improvement}</Tag>
      )
    }
  ]

  if (isLoading && !analytics) {
    return (
      <div className="orchestration-analytics-page">
        <Card>
          <div style={{ textAlign: 'center', padding: '50px 0' }}>
            <Spin size="large" />
            <div style={{ marginTop: 16 }}>加载性能分析数据中...</div>
          </div>
        </Card>
      </div>
    )
  }

  if (error || !analytics) {
    return (
      <div className="orchestration-analytics-page">
        <Card>
          <div style={{ textAlign: 'center', padding: '50px 0' }}>
            <Empty 
              description={error || '性能分析数据不存在'}
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            >
              <Button type="primary" onClick={() => navigate(`/federated-learning/orchestrations/${orchestrationId}`)}>
                返回编排详情
              </Button>
            </Empty>
          </div>
        </Card>
      </div>
    )
  }

  const resourceData = getResourceData()
  const stagePerformanceData = getStagePerformanceData()
  const radarData = getRadarData()
  const gradeConfig = PERFORMANCE_GRADE_CONFIG[analytics.overallPerformance?.grade as keyof typeof PERFORMANCE_GRADE_CONFIG]

  return (
    <div className="orchestration-analytics-page">
      {/* 页面头部 */}
      <Card className="header-card">
        <div className="page-header">
          <div className="header-left">
            <Button 
              type="text" 
              icon={<ArrowLeftOutlined />} 
              onClick={() => navigate(`/federated-learning/orchestrations/${orchestrationId}`)}
            >
              返回详情
            </Button>
            <div className="page-title">
              <h2>性能分析报告</h2>
              <span className="page-description">
                编排ID: {orchestrationId} | 分析时间: {new Date(analytics.analysisTimestamp).toLocaleString('zh-CN')}
              </span>
            </div>
          </div>
          
          <div className="header-actions">
            <Space>
              <Button 
                icon={<ReloadOutlined />} 
                onClick={handleRefresh}
                loading={isLoading}
              >
                刷新分析
              </Button>
            </Space>
          </div>
        </div>
      </Card>

      {/* 总体性能概览 */}
      <Card style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 16 }}>
          <h3>总体性能评估</h3>
        </div>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} md={6}>
            <Card size="small">
              <Statistic
                title="综合评分"
                value={analytics.overallPerformance?.score || 0}
                suffix="分"
                prefix={gradeConfig?.icon}
                valueStyle={{ color: gradeConfig?.color }}
              />
              <div style={{ textAlign: 'center', marginTop: 8 }}>
                <Tag color={gradeConfig?.color} style={{ fontSize: '14px', padding: '4px 8px' }}>
                  {gradeConfig?.text} ({analytics.overallPerformance?.grade})
                </Tag>
              </div>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card size="small">
              <Statistic
                title="效率评分"
                value={analytics.overallPerformance?.efficiency || 0}
                suffix="分"
                prefix={<ThunderboltOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
              <Progress 
                percent={analytics.overallPerformance?.efficiency || 0}
                size="small"
                showInfo={false}
                style={{ marginTop: 8 }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card size="small">
              <Statistic
                title="可靠性评分"
                value={analytics.overallPerformance?.reliability || 0}
                suffix="分"
                prefix={<SafetyOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
              <Progress 
                percent={analytics.overallPerformance?.reliability || 0}
                size="small"
                showInfo={false}
                style={{ marginTop: 8 }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card size="small">
              <Statistic
                title="扩展性评分"
                value={analytics.overallPerformance?.scalability || 0}
                suffix="分"
                prefix={<RocketOutlined />}
                valueStyle={{ color: '#722ed1' }}
              />
              <Progress 
                percent={analytics.overallPerformance?.scalability || 0}
                size="small"
                showInfo={false}
                style={{ marginTop: 8 }}
              />
            </Card>
          </Col>
        </Row>
      </Card>

      {/* 详细分析标签页 */}
      <Card>
        <Tabs activeKey={activeTab} onChange={setActiveTab} size="large">
          {/* 资源分析 */}
          <TabPane tab="资源分析" key="resources">
            <Row gutter={[24, 24]}>
              {/* 资源使用率图表 */}
              <Col xs={24} lg={14}>
                <Card title="资源使用率分析" size="small">
                  {resourceData.length > 0 ? (
                    <div style={{ padding: '16px 0' }}>
                      {resourceData.map((item, index) => (
                        <div key={index} style={{ marginBottom: '24px' }}>
                          <h4 style={{ marginBottom: '12px' }}>{item.resource}</h4>
                          <div style={{ marginBottom: '8px' }}>
                            <span>平均使用率: </span>
                            <Progress percent={item.average} strokeColor="#1890ff" style={{ width: '60%', display: 'inline-block' }} />
                            <span style={{ marginLeft: '8px' }}>{item.average}%</span>
                          </div>
                          <div style={{ marginBottom: '8px' }}>
                            <span>峰值使用率: </span>
                            <Progress percent={item.peak} strokeColor="#52c41a" style={{ width: '60%', display: 'inline-block' }} />
                            <span style={{ marginLeft: '8px' }}>{item.peak}%</span>
                          </div>
                          <div>
                            <span>使用效率: </span>
                            <Progress percent={item.efficiency} strokeColor="#faad14" style={{ width: '60%', display: 'inline-block' }} />
                            <span style={{ marginLeft: '8px' }}>{item.efficiency}%</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <Empty description="暂无资源数据" />
                  )}
                </Card>
              </Col>
              
              {/* 资源详细指标 */}
              <Col xs={24} lg={10}>
                <Card title="资源详细指标" size="small">
                  <div className="resource-metrics">
                    {analytics.resourceMetrics && (
                      <>
                        <div className="metric-item">
                          <div className="metric-header">
                            <span className="metric-name">CPU</span>
                            <Badge status="processing" />
                          </div>
                          <Descriptions size="small" column={1}>
                            <Descriptions.Item label="平均使用率">
                              {analytics.resourceMetrics.cpu?.averageUtilization || 0}%
                            </Descriptions.Item>
                            <Descriptions.Item label="峰值使用率">
                              {analytics.resourceMetrics.cpu?.peakUtilization || 0}%
                            </Descriptions.Item>
                            <Descriptions.Item label="使用效率">
                              {analytics.resourceMetrics.cpu?.efficiency || 0}%
                            </Descriptions.Item>
                          </Descriptions>
                        </div>
                        
                        <div className="metric-item">
                          <div className="metric-header">
                            <span className="metric-name">内存</span>
                            <Badge status="success" />
                          </div>
                          <Descriptions size="small" column={1}>
                            <Descriptions.Item label="平均使用率">
                              {analytics.resourceMetrics.memory?.averageUtilization || 0}%
                            </Descriptions.Item>
                            <Descriptions.Item label="峰值使用率">
                              {analytics.resourceMetrics.memory?.peakUtilization || 0}%
                            </Descriptions.Item>
                            <Descriptions.Item label="使用效率">
                              {analytics.resourceMetrics.memory?.efficiency || 0}%
                            </Descriptions.Item>
                          </Descriptions>
                        </div>
                        
                        <div className="metric-item">
                          <div className="metric-header">
                            <span className="metric-name">网络</span>
                            <Badge status="warning" />
                          </div>
                          <Descriptions size="small" column={1}>
                            <Descriptions.Item label="平均使用率">
                              {analytics.resourceMetrics.network?.averageUtilization || 0}%
                            </Descriptions.Item>
                            <Descriptions.Item label="峰值带宽">
                              {analytics.resourceMetrics.network?.peakBandwidth || '0MB/s'}
                            </Descriptions.Item>
                            <Descriptions.Item label="传输效率">
                              {analytics.resourceMetrics.network?.efficiency || 0}%
                            </Descriptions.Item>
                          </Descriptions>
                        </div>
                      </>
                    )}
                  </div>
                </Card>
              </Col>
            </Row>
          </TabPane>

          {/* 阶段分析 */}
          <TabPane tab="阶段分析" key="stages">
            <Row gutter={[24, 24]}>
              {/* 阶段性能图表 */}
              <Col xs={24} lg={16}>
                <Card title="各阶段性能表现" size="small">
                  {stagePerformanceData.length > 0 ? (
                    <div style={{ padding: '16px 0' }}>
                      {stagePerformanceData.map((item, index) => (
                        <div key={index} style={{ marginBottom: '20px', padding: '16px', border: '1px solid #f0f0f0', borderRadius: '6px' }}>
                          <h4 style={{ marginBottom: '12px' }}>{item.stage}</h4>
                          <div style={{ marginBottom: '8px' }}>
                            <span>执行时长: </span>
                            <Tag color="blue">{item.duration}</Tag>
                          </div>
                          <div style={{ marginBottom: '8px' }}>
                            <span>效率评分: </span>
                            <Progress percent={item.efficiency} strokeColor="#1890ff" style={{ width: '70%', display: 'inline-block' }} />
                            <span style={{ marginLeft: '8px' }}>{item.efficiency}%</span>
                          </div>
                          <div>
                            <span>资源利用率: </span>
                            <Progress percent={item.resourceUtilization} strokeColor="#52c41a" style={{ width: '70%', display: 'inline-block' }} />
                            <span style={{ marginLeft: '8px' }}>{item.resourceUtilization}%</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <Empty description="暂无阶段数据" />
                  )}
                </Card>
              </Col>
              
              {/* 阶段详细信息 */}
              <Col xs={24} lg={8}>
                <Card title="阶段详细信息" size="small">
                  <div className="stage-details">
                    {stagePerformanceData.map((stage, index) => (
                      <div key={index} className="stage-detail-item">
                        <div className="stage-name">{stage.stage}</div>
                        <div className="stage-metrics">
                          <div className="stage-metric">
                            <span>执行时长:</span>
                            <span>{stage.duration}</span>
                          </div>
                          <div className="stage-metric">
                            <span>效率评分:</span>
                            <Progress 
                              percent={stage.efficiency}
                              size="small"
                              format={(percent) => `${percent}%`}
                            />
                          </div>
                          <div className="stage-metric">
                            <span>资源利用:</span>
                            <Progress 
                              percent={stage.resourceUtilization}
                              size="small"
                              format={(percent) => `${percent}%`}
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </Card>
              </Col>
            </Row>
          </TabPane>

          {/* 质量分析 */}
          <TabPane tab="质量分析" key="quality">
            <Row gutter={[24, 24]}>
              {/* 质量指标雷达图 */}
              <Col xs={24} lg={12}>
                <Card title="质量指标分析" size="small">
                  {radarData.length > 0 ? (
                    <div style={{ padding: '16px 0' }}>
                      {radarData.map((item, index) => (
                        <div key={index} style={{ marginBottom: '16px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                            <span style={{ fontWeight: '500' }}>{item.metric}</span>
                            <span style={{ color: '#1890ff', fontWeight: '600' }}>{item.value}%</span>
                          </div>
                          <Progress 
                            percent={item.value} 
                            strokeColor={{
                              '0%': '#1890ff',
                              '100%': '#52c41a'
                            }}
                            showInfo={false}
                          />
                        </div>
                      ))}
                    </div>
                  ) : (
                    <Empty description="暂无质量数据" />
                  )}
                </Card>
              </Col>
              
              {/* 模型质量指标 */}
              <Col xs={24} lg={12}>
                <Card title="模型质量指标" size="small">
                  {analytics.qualityMetrics ? (
                    <div className="quality-metrics">
                      <div className="quality-item">
                        <div className="quality-header">
                          <span className="quality-name">模型准确率</span>
                          <Tag color="green">
                            {analytics.qualityMetrics.modelAccuracy?.improvement > 0 ? '提升' : '下降'}
                          </Tag>
                        </div>
                        <Descriptions size="small" column={1}>
                          <Descriptions.Item label="初始准确率">
                            {(analytics.qualityMetrics.modelAccuracy?.initial * 100).toFixed(2)}%
                          </Descriptions.Item>
                          <Descriptions.Item label="最终准确率">
                            {(analytics.qualityMetrics.modelAccuracy?.final * 100).toFixed(2)}%
                          </Descriptions.Item>
                          <Descriptions.Item label="准确率提升">
                            {(analytics.qualityMetrics.modelAccuracy?.improvement * 100).toFixed(2)}%
                          </Descriptions.Item>
                          <Descriptions.Item label="收敛轮次">
                            {analytics.qualityMetrics.modelAccuracy?.convergenceRounds}轮
                          </Descriptions.Item>
                        </Descriptions>
                      </div>
                      
                      <div className="quality-item">
                        <div className="quality-header">
                          <span className="quality-name">训练稳定性</span>
                          <Badge status="success" />
                        </div>
                        <div style={{ textAlign: 'center', padding: '16px 0' }}>
                          <Progress
                            type="circle"
                            percent={analytics.qualityMetrics.trainingStability}
                            format={(percent) => `${percent}%`}
                            size={80}
                          />
                        </div>
                      </div>
                      
                      <div className="quality-item">
                        <div className="quality-header">
                          <span className="quality-name">聚合质量</span>
                          <Badge status="processing" />
                        </div>
                        <div style={{ textAlign: 'center', padding: '16px 0' }}>
                          <Progress
                            type="circle"
                            percent={analytics.qualityMetrics.aggregationQuality}
                            format={(percent) => `${percent}%`}
                            size={80}
                          />
                        </div>
                      </div>
                    </div>
                  ) : (
                    <Empty description="暂无质量指标数据" />
                  )}
                </Card>
              </Col>
            </Row>
          </TabPane>

          {/* 优化建议 */}
          <TabPane tab="优化建议" key="recommendations">
            <Row gutter={[24, 24]}>
              {/* 建议列表 */}
              <Col xs={24} lg={16}>
                <Card title="优化建议" size="small">
                  {analytics.recommendations && analytics.recommendations.length > 0 ? (
                    <Table
                      columns={recommendationColumns}
                      dataSource={analytics.recommendations}
                      rowKey={(record, index) => index || 0}
                      pagination={false}
                      size="small"
                      scroll={{ x: 800 }}
                      expandable={{
                        expandedRowRender: (record: any) => (
                          <div className="recommendation-detail">
                            <Descriptions size="small" column={1}>
                              <Descriptions.Item label="实施方法">
                                <div>
                                  <strong>参数:</strong> {record.implementation?.parameter} <br/>
                                  <strong>当前值:</strong> {String(record.implementation?.currentValue)} <br/>
                                  <strong>建议值:</strong> {String(record.implementation?.recommendedValue)}
                                </div>
                              </Descriptions.Item>
                            </Descriptions>
                          </div>
                        ),
                        rowExpandable: (record: any) => !!record.implementation
                      }}
                    />
                  ) : (
                    <Empty description="暂无优化建议" />
                  )}
                </Card>
              </Col>
              
              {/* 建议统计 */}
              <Col xs={24} lg={8}>
                <Card title="建议统计" size="small">
                  {analytics.recommendations && analytics.recommendations.length > 0 ? (
                    <div className="recommendation-stats">
                      {/* 优先级分布 */}
                      <div className="stat-section">
                        <h4>优先级分布</h4>
                        <div className="priority-stats">
                          {Object.entries(PRIORITY_CONFIG).map(([priority, config]) => {
                            const count = analytics.recommendations?.filter(r => r.priority === priority).length || 0
                            return (
                              <div key={priority} className="priority-item">
                                <Tag color={config.color}>{config.text}</Tag>
                                <span className="count">{count}个</span>
                              </div>
                            )
                          })}
                        </div>
                      </div>
                      
                      {/* 分类分布 */}
                      <div className="stat-section">
                        <h4>分类分布</h4>
                        <div className="category-stats">
                          {Object.entries(CATEGORY_CONFIG).map(([category, config]) => {
                            const count = analytics.recommendations?.filter(r => r.category === category).length || 0
                            return (
                              <div key={category} className="category-item">
                                <Tag color={config.color}>
                                  {config.icon} {config.text}
                                </Tag>
                                <span className="count">{count}个</span>
                              </div>
                            )
                          })}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <Empty description="暂无建议统计" />
                  )}
                </Card>
              </Col>
            </Row>
          </TabPane>

          {/* 对比分析 */}
          <TabPane tab="对比分析" key="comparisons">
            <Row gutter={[24, 24]}>
              <Col xs={24} md={12}>
                <Card title="同类任务对比" size="small">
                  {analytics.comparisons?.similarTasks ? (
                    <div className="comparison-metrics">
                      <Descriptions size="small" column={1}>
                        <Descriptions.Item label="平均执行时长">
                          {analytics.comparisons.similarTasks.averageDuration}
                        </Descriptions.Item>
                        <Descriptions.Item label="性能排名">
                          <Tag color="blue">
                            {analytics.comparisons.similarTasks.performanceRanking}
                          </Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="效率排名">
                          <Tag color="green">
                            {analytics.comparisons.similarTasks.efficiencyRanking}
                          </Tag>
                        </Descriptions.Item>
                      </Descriptions>
                    </div>
                  ) : (
                    <Empty description="暂无对比数据" />
                  )}
                </Card>
              </Col>
              
              <Col xs={24} md={12}>
                <Card title="历史趋势" size="small">
                  {analytics.comparisons?.historicalTrends ? (
                    <div className="trend-metrics">
                      <Descriptions size="small" column={1}>
                        <Descriptions.Item label="改进速度">
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <Progress 
                              percent={analytics.comparisons.historicalTrends.improvementRate}
                              size="small"
                              format={(percent) => `${percent}%`}
                              style={{ flex: 1 }}
                            />
                            <Tag color="green">提升</Tag>
                          </div>
                        </Descriptions.Item>
                        <Descriptions.Item label="一致性评分">
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <Progress 
                              percent={analytics.comparisons.historicalTrends.consistencyScore}
                              size="small"
                              format={(percent) => `${percent}%`}
                              style={{ flex: 1 }}
                            />
                            <Tag color="blue">稳定</Tag>
                          </div>
                        </Descriptions.Item>
                      </Descriptions>
                    </div>
                  ) : (
                    <Empty description="暂无趋势数据" />
                  )}
                </Card>
              </Col>
            </Row>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  )
}

export default OrchestrationAnalyticsPage
