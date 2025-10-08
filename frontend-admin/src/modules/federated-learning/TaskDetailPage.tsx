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
  console.log('TaskDetailPage Debug:', {
    taskId,
    currentTask: currentTask?.taskName,
    status: currentTask?.status,
    taskResults,
    taskLogs,
    results,
    logs,
    hasResults: !!results,
    hasLogs: !!logs,
    finalResults: results ? (results as any).finalResults : null
  })
  const trainingData = getTrainingRoundsData()

  return (
    <div className="task-detail-page">
      {/* 页面头部 */}
      <Card className="page-header">
        <Row justify="space-between" align="middle">
          <Col>
            <Space>
              <Button 
                icon={<ArrowLeftOutlined />} 
                onClick={() => navigate('/federated-learning/tasks')}
              >
                返回
              </Button>
              <div>
                <h2 style={{ margin: 0 }}>{currentTask.taskName}</h2>
                <Space style={{ marginTop: 4 }}>
                  {statusConfig?.icon}
                  <Tag color={statusConfig?.color}>{statusConfig?.text}</Tag>
                  <span className="task-id">ID: {currentTask.taskId}</span>
                </Space>
              </div>
            </Space>
          </Col>
          <Col>
            <Space>
              <Tooltip title="自动刷新">
                <Button
                  type={autoRefresh ? 'primary' : 'default'}
                  icon={<ReloadOutlined />}
                  onClick={() => setAutoRefresh(!autoRefresh)}
                >
                  {autoRefresh ? '停止刷新' : '自动刷新'}
                </Button>
              </Tooltip>
              <Button
                icon={<ReloadOutlined />}
                onClick={handleRefresh}
                loading={currentTaskLoading}
              >
                刷新
              </Button>
              {canStartTask && canStartTask(currentTask) && (
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={handleStartTask}
                  loading={isOperating}
                >
                  启动任务
                </Button>
              )}
              {canPauseTask && canPauseTask(currentTask) && (
                <Button
                  icon={<PauseCircleOutlined />}
                  onClick={handlePauseTask}
                  loading={isOperating}
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
                >
                  恢复任务
                </Button>
              )}
              {canStopTask && canStopTask(currentTask) && (
                <Button
                  danger
                  icon={<StopOutlined />}
                  onClick={handleStopTask}
                  loading={isOperating}
                >
                  停止任务
                </Button>
              )}
            </Space>
          </Col>
        </Row>
      </Card>

      {/* 主要内容 */}
      <Card>
        <Tabs activeKey={activeTab} onChange={setActiveTab} size="large">
          {/* 概览 */}
          <TabPane tab="概览" key="overview">
            <Row gutter={[24, 24]}>
              {/* 基本信息 */}
              <Col xs={24} lg={16}>
                <Card title="基本信息" size="small">
                  <Descriptions column={2} size="small">
                    <Descriptions.Item label="任务名称">{currentTask.taskName}</Descriptions.Item>
                    <Descriptions.Item label="任务ID">{currentTask.taskId}</Descriptions.Item>
                    <Descriptions.Item label="创建时间">
                      {new Date(currentTask.createdAt).toLocaleString('zh-CN')}
                    </Descriptions.Item>
                    <Descriptions.Item label="更新时间">
                      {new Date(currentTask.createdAt).toLocaleString('zh-CN')}
                    </Descriptions.Item>
                    <Descriptions.Item label="算法">
                      {currentTask.algorithm}
                    </Descriptions.Item>
                    <Descriptions.Item label="参与者数量">
                      {currentTask.participantCount || 0}个
                    </Descriptions.Item>
                  </Descriptions>
                </Card>
              </Col>

              {/* 进度统计 */}
              <Col xs={24} lg={8}>
                <Card title="进度统计" size="small">
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                      <div style={{ marginBottom: 8 }}>整体进度</div>
                      <Progress 
                        percent={progress.overallProgress || 0}
                        strokeColor="#52c41a"
                        trailColor="#f0f0f0"
                      />
                    </div>
                    <Row gutter={16}>
                      <Col span={12}>
                        <Statistic 
                          title="当前轮次" 
                          value={progress.currentRound || 0}
                          suffix={`/ ${progress.totalRounds || 0}`}
                        />
                      </Col>
                      <Col span={12}>
                        <Statistic 
                          title="参与节点" 
                          value={currentTask.participantCount || 0}
                          suffix="个"
                        />
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
                      value={Math.max(...trainingData.map(d => d.accuracy)) * 100}
                      precision={2}
                      suffix="%"
                      valueStyle={{ color: '#3f8600' }}
                    />
                    <Statistic
                      title="最低损失"
                      value={Math.min(...trainingData.map(d => d.loss))}
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