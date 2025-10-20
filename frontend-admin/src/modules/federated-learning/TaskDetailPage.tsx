/**
 * 联邦学习任务详情页面
 * 显示任务的详细信息、实时状态、参与者信息、训练进度等
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
  Tooltip
} from 'antd'
import { 
  PlayCircleOutlined,
  PauseCircleOutlined,
  StopOutlined,
  ReloadOutlined,
  ArrowLeftOutlined,
  DownloadOutlined,
  EyeOutlined,
  SettingOutlined,
  LineChartOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  LoadingOutlined
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { useTask } from '@/store/federated-task/useFederatedTaskStore'
import './TaskDetailPage.css'

const { TabPane } = Tabs

// 任务状态配置
const TASK_STATUS_CONFIG = {
  CREATED: { color: 'blue', text: '已创建', icon: <ClockCircleOutlined /> },
  CONFIGURED: { color: 'cyan', text: '已配置', icon: <SettingOutlined /> },
  RUNNING: { color: 'green', text: '运行中', icon: <LoadingOutlined spin /> },
  PAUSED: { color: 'orange', text: '已暂停', icon: <PauseCircleOutlined /> },
  STOPPED: { color: 'red', text: '已停止', icon: <StopOutlined /> },
  COMPLETED: { color: 'success', text: '已完成', icon: <CheckCircleOutlined /> },
  FAILED: { color: 'error', text: '执行失败', icon: <ExclamationCircleOutlined /> },
  CANCELLED: { color: 'default', text: '已取消', icon: <ExclamationCircleOutlined /> }
}

const TaskDetailPage: React.FC = () => {
  const { taskId } = useParams<{ taskId: string }>()
  const navigate = useNavigate()
  
  // 使用Hook获取状态和操作
  const {
    currentTask,
    currentTaskLoading,
    currentTaskError,
    taskResults,
    taskLogs,
    fetchTaskDetail,
    fetchTaskResults,
    fetchTaskLogs,
    startTask,
    pauseTask,
    resumeTask,
    stopTask,
    getTaskResults,
    getTaskLogs,
    isTaskLogsLoading,
    canStartTask,
    canPauseTask,
    canResumeTask,
    canStopTask,
    getTaskProgress,
    isTaskOperating
  } = useTask()

  // 本地状态
  const [activeTab, setActiveTab] = useState<string>('overview')
  const [autoRefresh, setAutoRefresh] = useState<boolean>(false)
  const [refreshInterval, setRefreshInterval] = useState<NodeJS.Timeout | null>(null)

  // 初始化加载任务详情
  useEffect(() => {
    if (taskId) {
      fetchTaskDetail(taskId)
      fetchTaskResults(taskId)
      fetchTaskLogs(taskId)
    }
  }, [taskId, fetchTaskDetail, fetchTaskResults, fetchTaskLogs])

  // 自动刷新逻辑
  useEffect(() => {
    if (autoRefresh && taskId && currentTask?.status === 'RUNNING') {
      const interval = setInterval(() => {
        fetchTaskDetail(taskId)
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
  }, [autoRefresh, taskId, currentTask?.status, fetchTaskDetail])

  // 手动刷新
  const handleRefresh = useCallback(() => {
    if (taskId) {
      fetchTaskDetail(taskId)
      fetchTaskResults(taskId)
    }
  }, [taskId, fetchTaskDetail, fetchTaskResults])

  // 任务操作处理函数
  const handleStartTask = useCallback(async () => {
    if (!currentTask) return
    
    try {
      const result = await startTask(currentTask.taskId)
      if (result.success) {
        message.success('任务启动成功')
        handleRefresh()
      } else {
        message.error(result.error || '任务启动失败')
      }
    } catch (error) {
      message.error('任务启动失败')
    }
  }, [currentTask, startTask, handleRefresh])

  const handlePauseTask = useCallback(async () => {
    if (!currentTask) return
    
    try {
      const result = await pauseTask(currentTask.taskId)
      if (result.success) {
        message.success('任务暂停成功')
        handleRefresh()
      } else {
        message.error(result.error || '任务暂停失败')
      }
    } catch (error) {
      message.error('任务暂停失败')
    }
  }, [currentTask, pauseTask, handleRefresh])

  const handleResumeTask = useCallback(async () => {
    if (!currentTask) return
    
    try {
      const result = await resumeTask(currentTask.taskId)
      if (result.success) {
        message.success('任务恢复成功')
        handleRefresh()
      } else {
        message.error(result.error || '任务恢复失败')
      }
    } catch (error) {
      message.error('任务恢复失败')
    }
  }, [currentTask, resumeTask, handleRefresh])

  const handleStopTask = useCallback(async () => {
    if (!currentTask) return
    
    Modal.confirm({
      title: '确认停止任务',
      icon: <ExclamationCircleOutlined />,
      content: `确定要停止任务"${currentTask.taskName}"吗？停止后可以查看部分结果。`,
      okText: '确认停止',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const result = await stopTask(currentTask.taskId, {
            reason: '用户主动停止',
            saveCheckpoint: true
          })
          if (result.success) {
            message.success('任务停止成功')
            handleRefresh()
          } else {
            message.error(result.error || '任务停止失败')
          }
        } catch (error) {
          message.error('任务停止失败')
        }
      }
    })
  }, [currentTask, stopTask, handleRefresh])

  // 获取训练轮次数据
  const getTrainingRoundsData = useCallback(() => {
    if (!currentTask) return []
    
    // 从任务状态中获取真实的训练轮次数据
    // 如果任务对象包含 trainingHistory 或类似字段，从中提取数据
    // 否则返回空数组，等待后端返回真实数据
    if ('trainingHistory' in currentTask && Array.isArray((currentTask as any).trainingHistory)) {
      return (currentTask as any).trainingHistory
    }
    
    return []
  }, [currentTask])

  // 参与者表格列定义
  const participantColumns = [
    {
      title: '虚拟机ID',
      dataIndex: 'vmId',
      key: 'vmId',
      width: 200,
      ellipsis: true
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 100,
      render: (role: string) => (
        <Tag color="blue">{role === 'PARTICIPANT' ? '参与者' : role}</Tag>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const statusConfig = {
          CONNECTED: { color: 'success', text: '已连接' },
          TRAINING: { color: 'processing', text: '训练中' },
          WAITING: { color: 'warning', text: '等待中' },
          DISCONNECTED: { color: 'error', text: '已断开' }
        }
        const config = statusConfig[status as keyof typeof statusConfig] || { color: 'default', text: status }
        return <Tag color={config.color}>{config.text}</Tag>
      }
    },
    {
      title: '当前轮次',
      dataIndex: 'currentEpoch',
      key: 'currentEpoch',
      width: 100
    },
    {
      title: '准确率',
      dataIndex: 'accuracy',
      key: 'accuracy',
      width: 100,
      render: (accuracy: number) => accuracy ? `${(accuracy * 100).toFixed(2)}%` : '-'
    },
    {
      title: '损失值',
      dataIndex: 'loss',
      key: 'loss',
      width: 100,
      render: (loss: number) => loss ? loss.toFixed(4) : '-'
    },
    {
      title: '最后心跳',
      dataIndex: 'lastHeartbeat',
      key: 'lastHeartbeat',
      width: 150,
      render: (time: string) => time ? new Date(time).toLocaleString('zh-CN') : '-'
    }
  ]

  if (currentTaskLoading) {
    return (
      <div className="task-detail-page">
        <Card>
          <div style={{ textAlign: 'center', padding: '50px 0' }}>
            <Spin size="large" />
            <div style={{ marginTop: 16 }}>加载任务详情中...</div>
          </div>
        </Card>
      </div>
    )
  }

  if (currentTaskError || !currentTask) {
    return (
      <div className="task-detail-page">
        <Card>
          <Alert
            message="加载失败"
            description={currentTaskError || '任务不存在'}
            type="error"
            showIcon
            action={
              <Button size="small" danger onClick={() => navigate('/federated-learning/tasks')}>
                返回任务列表
              </Button>
            }
          />
        </Card>
      </div>
    )
  }

  const statusConfig = TASK_STATUS_CONFIG[currentTask.status as keyof typeof TASK_STATUS_CONFIG]
  
  // 从currentTask中提取进度信息
  const progress = {
    overallProgress: currentTask.progress || 0,
    currentRound: currentTask.currentRound || 0,
    totalRounds: currentTask.totalRounds || 0
  }
  
  const isOperating = false  // 简化处理
  // 根据taskId获取对应的结果和日志数据
  const results = taskId ? taskResults[taskId] : null
  const logs = taskId ? taskLogs[taskId] : null
  
  // 调试信息
  const trainingData = getTrainingRoundsData()
  console.log('TaskDetailPage Debug:', {
    taskId,
    currentTask: currentTask?.taskName,
    status: currentTask?.status,
    hasTrainingHistory: 'trainingHistory' in (currentTask || {}),
    trainingHistoryLength: (currentTask as any)?.trainingHistory?.length || 0,
    trainingHistory: (currentTask as any)?.trainingHistory,
    trainingDataLength: trainingData.length,
    trainingData,
    taskResults,
    taskLogs,
    results,
    logs,
    hasResults: !!results,
    hasLogs: !!logs,
    finalResults: results ? (results as any).finalResults : null
  })

  return (
    <div className="task-detail-page">
      {/* 页面头部 */}
      <Card className="header-card" bordered={false}>
        <div className="page-header">
          <div className="header-left">
            <Button 
              icon={<ArrowLeftOutlined />} 
              onClick={() => navigate('/federated-learning/tasks')}
              size="large"
              style={{ marginRight: 16 }}
            >
              返回
            </Button>
            <div className="task-title">
              <h2>{currentTask.taskName}</h2>
              <div className="task-meta">
                {statusConfig?.icon}
                <Tag color={statusConfig?.color} style={{ 
                  padding: '4px 12px', 
                  borderRadius: '6px',
                  fontWeight: 500 
                }}>
                  {statusConfig?.text}
                </Tag>
                <span className="task-id">ID: {currentTask.taskId}</span>
              </div>
            </div>
          </div>
          <div className="header-actions">
            <Space size="middle">
              <Tooltip title="启用后每10秒自动刷新">
                <Button
                  type={autoRefresh ? 'primary' : 'default'}
                  icon={<ReloadOutlined spin={autoRefresh} />}
                  onClick={() => setAutoRefresh(!autoRefresh)}
                  size="large"
                >
                  自动刷新
                </Button>
              </Tooltip>
              <Button
                icon={<ReloadOutlined />}
                onClick={handleRefresh}
                loading={currentTaskLoading}
                size="large"
              >
                刷新
              </Button>
              {canStartTask && canStartTask(currentTask) && (
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={handleStartTask}
                  loading={isOperating}
                  size="large"
                  style={{
                    background: 'linear-gradient(135deg, #52c41a 0%, #389e0d 100%)',
                    border: 'none'
                  }}
                >
                  启动任务
                </Button>
              )}
              {canPauseTask && canPauseTask(currentTask) && (
                <Button
                  icon={<PauseCircleOutlined />}
                  onClick={handlePauseTask}
                  loading={isOperating}
                  size="large"
                  style={{
                    background: 'linear-gradient(135deg, #faad14 0%, #d48806 100%)',
                    border: 'none',
                    color: 'white'
                  }}
                >
                  暂停任务
                </Button>
              )}
              {canResumeTask && canResumeTask(currentTask) && (
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={handleResumeTask}
                  loading={isOperating}
                  size="large"
                >
                  恢复任务
                </Button>
              )}
              {canStopTask && canStopTask(currentTask) && (
                <Button
                  icon={<StopOutlined />}
                  onClick={handleStopTask}
                  loading={isOperating}
                  size="large"
                  style={{
                    background: 'linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%)',
                    border: 'none',
                    color: 'white'
                  }}
                >
                  停止任务
                </Button>
              )}
            </Space>
          </div>
        </div>
      </Card>

      {/* 主要内容 */}
      <Card bordered={false} style={{ 
        background: '#ffffff',
        borderRadius: '12px',
        boxShadow: '0 4px 20px rgba(0, 0, 0, 0.08)'
      }}>
        <Tabs activeKey={activeTab} onChange={setActiveTab} size="large">
          {/* 概览 */}
          <TabPane tab="概览" key="overview">
            <Row gutter={[24, 24]}>
              {/* 基本信息 */}
              <Col xs={24} lg={16}>
                <Card 
                  title={<span style={{ fontSize: '16px', fontWeight: 600 }}>基本信息</span>}
                  bordered={false}
                  style={{ 
                    background: 'linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%)',
                    borderRadius: '12px',
                    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
                  }}
                  headStyle={{
                    borderBottom: '2px solid #f0f0f0',
                    background: 'transparent'
                  }}
                >
                  <Descriptions column={2} size="middle" bordered>
                    <Descriptions.Item label="任务名称" labelStyle={{ fontWeight: 600 }}>
                      {currentTask.taskName}
                    </Descriptions.Item>
                    <Descriptions.Item label="任务ID" labelStyle={{ fontWeight: 600 }}>
                      <code style={{ 
                        background: '#f5f5f5', 
                        padding: '2px 8px', 
                        borderRadius: '4px',
                        fontSize: '12px'
                      }}>
                        {currentTask.taskId}
                      </code>
                    </Descriptions.Item>
                    <Descriptions.Item label="创建时间" labelStyle={{ fontWeight: 600 }}>
                      {new Date(currentTask.createdAt).toLocaleString('zh-CN')}
                    </Descriptions.Item>
                    <Descriptions.Item label="更新时间" labelStyle={{ fontWeight: 600 }}>
                      {new Date(currentTask.createdAt).toLocaleString('zh-CN')}
                    </Descriptions.Item>
                    <Descriptions.Item label="算法" labelStyle={{ fontWeight: 600 }}>
                      <Tag color="blue" style={{ borderRadius: '6px' }}>
                        {currentTask.algorithm}
                      </Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="参与者数量" labelStyle={{ fontWeight: 600 }}>
                      <Tag color="geekblue" style={{ borderRadius: '6px', fontSize: '14px' }}>
                        {currentTask.participantCount || 0}个
                      </Tag>
                    </Descriptions.Item>
                  </Descriptions>
                </Card>
              </Col>

              {/* 进度统计 */}
              <Col xs={24} lg={8}>
                <Card 
                  title={<span style={{ fontSize: '16px', fontWeight: 600 }}>进度统计</span>}
                  bordered={false}
                  style={{ 
                    background: 'linear-gradient(135deg, #e6f7ff 0%, #ffffff 100%)',
                    borderRadius: '12px',
                    boxShadow: '0 2px 8px rgba(24, 144, 255, 0.1)'
                  }}
                  headStyle={{
                    borderBottom: '2px solid #91d5ff',
                    background: 'transparent'
                  }}
                >
                  <Space direction="vertical" style={{ width: '100%' }} size="large">
                    <div>
                      <div style={{ 
                        marginBottom: 12, 
                        fontSize: '14px', 
                        color: '#595959',
                        fontWeight: 500 
                      }}>
                        整体进度
                      </div>
                      <Progress 
                        percent={progress.overallProgress || 0}
                        strokeColor={{
                          '0%': '#1890ff',
                          '100%': '#52c41a'
                        }}
                        strokeWidth={12}
                        trailColor="#f0f0f0"
                        format={(percent) => (
                          <span style={{ fontSize: '16px', fontWeight: 600, color: '#1890ff' }}>
                            {percent}%
                          </span>
                        )}
                      />
                    </div>
                    <Row gutter={16}>
                      <Col span={12}>
                        <div style={{ 
                          background: '#ffffff', 
                          padding: '16px', 
                          borderRadius: '8px',
                          border: '1px solid #f0f0f0',
                          textAlign: 'center'
                        }}>
                          <div style={{ color: '#8c8c8c', fontSize: '12px', marginBottom: 8 }}>
                            当前轮次
                          </div>
                          <div style={{ fontSize: '24px', fontWeight: 600, color: '#1890ff' }}>
                            {progress.currentRound || 0}
                          </div>
                          <div style={{ color: '#8c8c8c', fontSize: '12px', marginTop: 4 }}>
                            / {progress.totalRounds || 0}
                          </div>
                        </div>
                      </Col>
                      <Col span={12}>
                        <div style={{ 
                          background: '#ffffff', 
                          padding: '16px', 
                          borderRadius: '8px',
                          border: '1px solid #f0f0f0',
                          textAlign: 'center'
                        }}>
                          <div style={{ color: '#8c8c8c', fontSize: '12px', marginBottom: 8 }}>
                            参与节点
                          </div>
                          <div style={{ fontSize: '24px', fontWeight: 600, color: '#52c41a' }}>
                            {currentTask.participantCount || 0}
                          </div>
                          <div style={{ color: '#8c8c8c', fontSize: '12px', marginTop: 4 }}>
                            个
                          </div>
                        </div>
                      </Col>
                    </Row>
                  </Space>
                </Card>
              </Col>
            </Row>

            {/* 配置信息 */}
            {/* 移除超参数配置和资源配置，因为接口3.8不提供这些数据 */}
          </TabPane>

          {/* 参与者 */}
          <TabPane tab="参与者" key="participants">
            <Card>
              <Table
                columns={participantColumns}
                dataSource={currentTask.participants || []}
                rowKey="vmId"
                pagination={false}
                loading={currentTaskLoading}
                scroll={{ x: 1000 }}
                size="small"
              />
            </Card>
          </TabPane>

          {/* 训练进度 */}
          <TabPane tab="训练进度" key="training">
            <Row gutter={[24, 24]}>
              <Col xs={24} lg={16}>
                <Card title="训练曲线" size="small">
                  {trainingData.length > 0 ? (
                    <div style={{ padding: '20px 0' }}>
                      <h4>准确率趋势</h4>
                      {trainingData.map((data, index) => (
                        <div key={index} style={{ marginBottom: '12px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                            <span>轮次 {data.round}</span>
                            <span>{(data.accuracy * 100).toFixed(2)}%</span>
                          </div>
                          <Progress percent={data.accuracy * 100} strokeColor="#1890ff" />
                        </div>
                      ))}
                      
                      <h4 style={{ marginTop: '24px' }}>损失值趋势</h4>
                      {trainingData.map((data, index) => (
                        <div key={index} style={{ marginBottom: '12px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                            <span>轮次 {data.round}</span>
                            <span>{data.loss.toFixed(4)}</span>
                          </div>
                          <Progress 
                            percent={Math.max(0, (1 - data.loss) * 100)} 
                            strokeColor="#52c41a" 
                          />
                        </div>
                      ))}
                    </div>
                  ) : (
                    <Empty description="暂无训练数据" />
                  )}
                </Card>
              </Col>

              <Col xs={24} lg={8}>
                <Card title="训练统计" size="small">
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <Statistic
                      title="最佳准确率"
                      value={trainingData.length > 0 ? Math.max(...trainingData.map(d => d.accuracy)) * 100 : 0}
                      precision={2}
                      suffix="%"
                      valueStyle={{ color: '#3f8600' }}
                    />
                    <Statistic
                      title="最低损失"
                      value={trainingData.length > 0 ? Math.min(...trainingData.map(d => d.loss)) : 0}
                      precision={4}
                      valueStyle={{ color: '#cf1322' }}
                    />
                    <Statistic
                      title="训练轮次"
                      value={trainingData.length}
                      suffix="轮"
                    />
                  </Space>
                </Card>
              </Col>
            </Row>
          </TabPane>

          {/* 结果 */}
          <TabPane tab="结果" key="results">
            <Card>
              {results ? (
                <div>
                  {/* 最终结果 */}
                  {(results as any).finalResults ? (
                    <div>
                      <h4>最终结果</h4>
                      <Descriptions column={2} size="small">
                        <Descriptions.Item label="最终准确率">
                          {`${((results as any).finalResults.accuracy * 100).toFixed(2)}%`}
                        </Descriptions.Item>
                        <Descriptions.Item label="最终损失">
                          {(results as any).finalResults.loss.toFixed(4)}
                        </Descriptions.Item>
                        <Descriptions.Item label="精确率">
                          {(results as any).finalResults.precision ? `${((results as any).finalResults.precision * 100).toFixed(2)}%` : 'N/A'}
                        </Descriptions.Item>
                        <Descriptions.Item label="召回率">
                          {(results as any).finalResults.recall ? `${((results as any).finalResults.recall * 100).toFixed(2)}%` : 'N/A'}
                        </Descriptions.Item>
                      </Descriptions>
                    </div>
                  ) : (
                    <div>
                      <h4>训练进展</h4>
                      <p>任务正在运行中，最终结果将在完成后显示。</p>
                    </div>
                  )}
                  
                  {/* 轮次结果 */}
                  {(results as any).roundResults && (results as any).roundResults.length > 0 && (
                    <div style={{ marginTop: 16 }}>
                      <h4>轮次结果</h4>
                      <Table
                        columns={[
                          { title: '轮次', dataIndex: 'round', key: 'round' },
                          { 
                            title: '准确率', 
                            dataIndex: 'accuracy', 
                            key: 'accuracy',
                            render: (acc: number) => `${(acc * 100).toFixed(2)}%`
                          },
                          { 
                            title: '损失', 
                            dataIndex: 'loss', 
                            key: 'loss',
                            render: (loss: number) => loss.toFixed(4)
                          }
                        ]}
                        dataSource={(results as any).roundResults}
                        rowKey="round"
                        pagination={false}
                        size="small"
                      />
                    </div>
                  )}
                </div>
              ) : (
                <Empty description="暂无结果数据" />
              )}
            </Card>
          </TabPane>

          {/* 日志 */}
          <TabPane tab="日志" key="logs">
            <Card>
              {logs && Array.isArray(logs) && logs.length > 0 ? (
                <Timeline mode="left">
                  {logs.map((log: any, index: number) => (
                    <Timeline.Item
                      key={index}
                      color={log.level === 'ERROR' ? 'red' : log.level === 'WARN' ? 'orange' : 'blue'}
                      label={new Date(log.timestamp).toLocaleString('zh-CN')}
                    >
                      <div>
                        <Tag color={log.level === 'ERROR' ? 'red' : log.level === 'WARN' ? 'orange' : 'blue'}>
                          {log.level}
                        </Tag>
                        <span>{log.message}</span>
                      </div>
                      {log.details && (
                        <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                          {JSON.stringify(log.details, null, 2)}
                        </div>
                      )}
                    </Timeline.Item>
                  ))}
                </Timeline>
              ) : (
                <Empty description="暂无日志数据" />
              )}
            </Card>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  )
}

export default TaskDetailPage