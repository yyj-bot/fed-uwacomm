/**
 * 联邦学习流程编排时间线页面
 * 显示编排任务的详细执行时间线和事件历史
 * 
 * @author FedUWAComm Team
 * @version 1.4.0
 */

import React, { useEffect, useState, useCallback } from 'react'
import { 
  Card, 
  Timeline, 
  Button, 
  Space, 
  Tag, 
  Select, 
  DatePicker,
  Input,
  Row,
  Col,
  Statistic,
  message,
  Spin,
  Empty,
  Tooltip,
  Badge,
  Collapse,
  Descriptions
} from 'antd'
import { 
  ArrowLeftOutlined,
  ReloadOutlined,
  SearchOutlined,
  FilterOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  BugOutlined,
  ThunderboltOutlined,
  DatabaseOutlined,
  CloudServerOutlined,
  TeamOutlined,
  BarChartOutlined
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { useOrchestration } from '@/store/federated-task/useFederatedTaskStore'
// 临时类型定义，直到@/types中添加这些类型
interface WorkflowTimelineEvent {
  eventId: string
  timestamp: string
  eventType: string
  stage?: string
  level: string
  message: string
  duration?: string
  details?: Record<string, any>
}

interface WorkflowTimeline {
  orchestrationId: string
  taskId: string
  timeline: {
    startTime: string
    endTime?: string
    totalDuration: string
    events: WorkflowTimelineEvent[]
  }
  stagesSummary?: Record<string, {
    status: string
    duration: string
    events: number
  }>
}
import './OrchestrationTimelinePage.css'

const { RangePicker } = DatePicker
const { Option } = Select
const { Panel } = Collapse

// 事件级别配置
const EVENT_LEVEL_CONFIG = {
  ALL: { text: '全部', color: 'default' },
  MAJOR: { text: '主要', color: 'blue' },
  ERROR: { text: '错误', color: 'red' },
  WARNING: { text: '警告', color: 'orange' },
  INFO: { text: '信息', color: 'green' }
}

// 事件类型配置
const EVENT_TYPE_CONFIG = {
  WORKFLOW_STARTED: { 
    icon: <ThunderboltOutlined />, 
    color: '#1890ff', 
    text: '流程启动' 
  },
  STAGE_STARTED: { 
    icon: <ClockCircleOutlined />, 
    color: '#52c41a', 
    text: '阶段开始' 
  },
  STAGE_COMPLETED: { 
    icon: <CheckCircleOutlined />, 
    color: '#52c41a', 
    text: '阶段完成' 
  },
  STAGE_FAILED: { 
    icon: <ExclamationCircleOutlined />, 
    color: '#ff4d4f', 
    text: '阶段失败' 
  },
  TRAINING_ROUND_STARTED: { 
    icon: <TeamOutlined />, 
    color: '#722ed1', 
    text: '训练轮次开始' 
  },
  TRAINING_ROUND_COMPLETED: { 
    icon: <CheckCircleOutlined />, 
    color: '#722ed1', 
    text: '训练轮次完成' 
  },
  MODEL_AGGREGATION: { 
    icon: <BarChartOutlined />, 
    color: '#eb2f96', 
    text: '模型聚合' 
  },
  DATA_DISTRIBUTION: { 
    icon: <DatabaseOutlined />, 
    color: '#faad14', 
    text: '数据分发' 
  },
  MODEL_DISTRIBUTION: { 
    icon: <CloudServerOutlined />, 
    color: '#13c2c2', 
    text: '模型分发' 
  },
  ERROR: { 
    icon: <BugOutlined />, 
    color: '#ff4d4f', 
    text: '错误' 
  },
  WARNING: { 
    icon: <WarningOutlined />, 
    color: '#faad14', 
    text: '警告' 
  },
  INFO: { 
    icon: <InfoCircleOutlined />, 
    color: '#1890ff', 
    text: '信息' 
  }
}

// 工作流阶段配置
const WORKFLOW_STAGE_CONFIG = {
  INITIALIZATION: '初始化',
  INITIAL_MODEL_GENERATION: '初始模型生成',
  DATA_DISTRIBUTION: '数据分发',
  MODEL_DISTRIBUTION: '模型分发',
  FEDERATED_TRAINING: '联邦训练',
  FINAL_AGGREGATION: '最终聚合'
}

const OrchestrationTimelinePage: React.FC = () => {
  const { orchestrationId } = useParams<{ orchestrationId: string }>()
  const navigate = useNavigate()
  
  // 使用Hook获取状态和操作
  const {
    orchestrationTimelines,
    orchestrationTimelinesLoading,
    orchestrationTimelinesError,
    fetchOrchestrationTimeline,
    getOrchestrationTimeline
  } = useOrchestration()

  // 本地状态
  const [eventLevel, setEventLevel] = useState<'ALL' | 'MAJOR' | 'ERROR'>('MAJOR')
  const [eventTypeFilter, setEventTypeFilter] = useState<string>('')
  const [stageFilter, setStageFilter] = useState<string>('')
  const [timeRange, setTimeRange] = useState<[any, any] | null>(null)
  const [searchKeyword, setSearchKeyword] = useState<string>('')
  const [filteredEvents, setFilteredEvents] = useState<WorkflowTimelineEvent[]>([])

  // 获取时间线数据
  const timeline = orchestrationId ? getOrchestrationTimeline(orchestrationId) : null
  const isLoading = orchestrationId ? orchestrationTimelinesLoading[orchestrationId] : false
  const error = orchestrationId ? orchestrationTimelinesError[orchestrationId] : null

  // 初始化加载时间线
  useEffect(() => {
    if (orchestrationId) {
      fetchOrchestrationTimeline(orchestrationId, {
        includeEvents: true,
        eventLevel: eventLevel
      })
    }
  }, [orchestrationId, eventLevel, fetchOrchestrationTimeline])

  // 过滤事件
  useEffect(() => {
    if (!timeline?.timeline?.events) {
      setFilteredEvents([])
      return
    }

    let filtered = timeline.timeline.events

    // 按事件类型过滤
    if (eventTypeFilter) {
      filtered = filtered.filter(event => event.eventType === eventTypeFilter)
    }

    // 按阶段过滤
    if (stageFilter) {
      filtered = filtered.filter(event => event.stage === stageFilter)
    }

    // 按时间范围过滤
    if (timeRange && timeRange[0] && timeRange[1]) {
      const startTime = timeRange[0].valueOf()
      const endTime = timeRange[1].valueOf()
      filtered = filtered.filter(event => {
        const eventTime = new Date(event.timestamp).valueOf()
        return eventTime >= startTime && eventTime <= endTime
      })
    }

    // 按关键词搜索
    if (searchKeyword.trim()) {
      const keyword = searchKeyword.trim().toLowerCase()
      filtered = filtered.filter(event => 
        event.message.toLowerCase().includes(keyword) ||
        event.eventType.toLowerCase().includes(keyword) ||
        (event.stage && event.stage.toLowerCase().includes(keyword))
      )
    }

    setFilteredEvents(filtered)
  }, [timeline, eventTypeFilter, stageFilter, timeRange, searchKeyword])

  // 刷新时间线
  const handleRefresh = useCallback(() => {
    if (orchestrationId) {
      fetchOrchestrationTimeline(orchestrationId, {
        includeEvents: true,
        eventLevel: eventLevel
      })
    }
  }, [orchestrationId, eventLevel, fetchOrchestrationTimeline])

  // 重置筛选条件
  const handleReset = useCallback(() => {
    setEventTypeFilter('')
    setStageFilter('')
    setTimeRange(null)
    setSearchKeyword('')
  }, [])

  // 获取事件级别统计
  const getEventLevelStats = useCallback(() => {
    if (!timeline?.timeline?.events) return { total: 0, error: 0, warning: 0, info: 0 }

    const stats = timeline.timeline.events.reduce((acc, event) => {
      acc.total++
      if (event.level === 'ERROR') acc.error++
      else if (event.level === 'MAJOR') acc.warning++  // 将MAJOR当作warning处理
      else acc.info++
      return acc
    }, { total: 0, error: 0, warning: 0, info: 0 })

    return stats
  }, [timeline])

  // 获取阶段统计
  const getStageStats = useCallback(() => {
    if (!timeline?.stagesSummary) return []

    return Object.entries(timeline.stagesSummary).map(([stage, summary]) => ({
      stage: WORKFLOW_STAGE_CONFIG[stage as keyof typeof WORKFLOW_STAGE_CONFIG] || stage,
      ...summary
    }))
  }, [timeline])

  // 渲染时间线项目
  const renderTimelineItem = (event: WorkflowTimelineEvent) => {
    const eventConfig = EVENT_TYPE_CONFIG[event.eventType as keyof typeof EVENT_TYPE_CONFIG]
    const levelConfig = EVENT_LEVEL_CONFIG[event.level as keyof typeof EVENT_LEVEL_CONFIG]
    
    return (
      <Timeline.Item
        key={event.eventId}
        color={eventConfig?.color || '#1890ff'}
        dot={eventConfig?.icon || <InfoCircleOutlined />}
      >
        <div className="timeline-event">
          <div className="event-header">
            <div className="event-info">
              <span className="event-type">{eventConfig?.text || event.eventType}</span>
              <Tag color={levelConfig?.color}>
                {levelConfig?.text || event.level}
              </Tag>
              {event.stage && (
                <Tag color="blue">
                  {WORKFLOW_STAGE_CONFIG[event.stage as keyof typeof WORKFLOW_STAGE_CONFIG] || event.stage}
                </Tag>
              )}
            </div>
            <div className="event-time">
              {new Date(event.timestamp).toLocaleString('zh-CN')}
            </div>
          </div>
          
          <div className="event-message">
            {event.message}
          </div>
          
          {event.duration && (
            <div className="event-duration">
              <ClockCircleOutlined style={{ marginRight: 4 }} />
              耗时: {event.duration}
            </div>
          )}
          
          {event.details && Object.keys(event.details).length > 0 && (
            <div className="event-details">
              <Collapse ghost size="small">
                <Panel header="详细信息" key="details">
                  <Descriptions size="small" column={1}>
                    {Object.entries(event.details).map(([key, value]) => (
                      <Descriptions.Item key={key} label={key}>
                        {typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value)}
                      </Descriptions.Item>
                    ))}
                  </Descriptions>
                </Panel>
              </Collapse>
            </div>
          )}
        </div>
      </Timeline.Item>
    )
  }

  const eventStats = getEventLevelStats()
  const stageStats = getStageStats()

  if (isLoading && !timeline) {
    return (
      <div className="orchestration-timeline-page">
        <Card>
          <div style={{ textAlign: 'center', padding: '50px 0' }}>
            <Spin size="large" />
            <div style={{ marginTop: 16 }}>加载时间线数据中...</div>
          </div>
        </Card>
      </div>
    )
  }

  if (error || !timeline) {
    return (
      <div className="orchestration-timeline-page">
        <Card>
          <div style={{ textAlign: 'center', padding: '50px 0' }}>
            <Empty 
              description={error || '时间线数据不存在'}
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

  return (
    <div className="orchestration-timeline-page">
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
              <h2>执行时间线</h2>
              <span className="page-description">编排ID: {orchestrationId}</span>
            </div>
          </div>
          
          <div className="header-actions">
            <Space>
              <Button 
                icon={<ReloadOutlined />} 
                onClick={handleRefresh}
                loading={isLoading}
              >
                刷新
              </Button>
            </Space>
          </div>
        </div>
      </Card>

      {/* 统计概览 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="总执行时长"
              value={timeline.timeline.totalDuration}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="总事件数"
              value={eventStats.total}
              prefix={<InfoCircleOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="错误事件"
              value={eventStats.error}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="警告事件"
              value={eventStats.warning}
              prefix={<WarningOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 阶段统计 */}
      {stageStats.length > 0 && (
        <Card style={{ marginBottom: 16 }}>
          <h3 style={{ marginBottom: 16 }}>阶段统计</h3>
          <Row gutter={[16, 16]}>
            {stageStats.map((stage, index) => (
              <Col key={index} xs={24} sm={12} md={8} lg={6}>
                <Card size="small">
                  <div className="stage-stat">
                    <div className="stage-name">{stage.stage}</div>
                    <div className="stage-info">
                      <Badge 
                        status={
                          stage.status === 'COMPLETED' ? 'success' :
                          stage.status === 'IN_PROGRESS' ? 'processing' :
                          stage.status === 'FAILED' ? 'error' : 'default'
                        }
                        text={
                          stage.status === 'COMPLETED' ? '已完成' :
                          stage.status === 'IN_PROGRESS' ? '进行中' :
                          stage.status === 'FAILED' ? '失败' : '等待中'
                        }
                      />
                      <div className="stage-metrics">
                        <span>耗时: {stage.duration}</span>
                        <span>事件: {stage.events}个</span>
                      </div>
                    </div>
                  </div>
                </Card>
              </Col>
            ))}
          </Row>
        </Card>
      )}

      {/* 筛选和搜索 */}
      <Card style={{ marginBottom: 16 }}>
        <div className="filter-section">
          <Row gutter={[16, 16]} align="middle">
            <Col xs={24} sm={12} md={6}>
              <Select
                placeholder="事件级别"
                value={eventLevel}
                onChange={setEventLevel}
                style={{ width: '100%' }}
              >
                {Object.entries(EVENT_LEVEL_CONFIG).map(([value, config]) => (
                  <Option key={value} value={value}>
                    <Tag color={config.color}>{config.text}</Tag>
                  </Option>
                ))}
              </Select>
            </Col>
            
            <Col xs={24} sm={12} md={6}>
              <Select
                placeholder="事件类型"
                value={eventTypeFilter}
                onChange={setEventTypeFilter}
                allowClear
                style={{ width: '100%' }}
              >
                {Object.entries(EVENT_TYPE_CONFIG).map(([value, config]) => (
                  <Option key={value} value={value}>
                    {config.icon} {config.text}
                  </Option>
                ))}
              </Select>
            </Col>
            
            <Col xs={24} sm={12} md={6}>
              <Select
                placeholder="工作流阶段"
                value={stageFilter}
                onChange={setStageFilter}
                allowClear
                style={{ width: '100%' }}
              >
                {Object.entries(WORKFLOW_STAGE_CONFIG).map(([value, text]) => (
                  <Option key={value} value={value}>{text}</Option>
                ))}
              </Select>
            </Col>
            
            <Col xs={24} sm={12} md={6}>
              <RangePicker
                value={timeRange}
                onChange={setTimeRange}
                showTime
                placeholder={['开始时间', '结束时间']}
                style={{ width: '100%' }}
              />
            </Col>
            
            <Col xs={24} md={12}>
              <Input
                placeholder="搜索事件消息"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                prefix={<SearchOutlined />}
                allowClear
              />
            </Col>
            
            <Col xs={24} md={12}>
              <Space>
                <Button 
                  type="primary" 
                  icon={<FilterOutlined />}
                  onClick={() => {/* 筛选逻辑已在useEffect中处理 */}}
                >
                  筛选 ({filteredEvents.length})
                </Button>
                <Button onClick={handleReset}>
                  重置
                </Button>
              </Space>
            </Col>
          </Row>
        </div>
      </Card>

      {/* 时间线内容 */}
      <Card>
        <div className="timeline-container">
          {filteredEvents.length > 0 ? (
            <Timeline mode="left" className="orchestration-timeline">
              {filteredEvents.map(renderTimelineItem)}
            </Timeline>
          ) : (
            <Empty 
              description="没有符合条件的事件"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          )}
        </div>
        
        {isLoading && (
          <div style={{ textAlign: 'center', padding: '20px 0' }}>
            <Spin />
            <div style={{ marginTop: 8 }}>加载更多事件...</div>
          </div>
        )}
      </Card>
    </div>
  )
}

export default OrchestrationTimelinePage
