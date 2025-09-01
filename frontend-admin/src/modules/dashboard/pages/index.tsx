import React, { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, Typography, Spin, Alert } from 'antd'
import {
  CloudServerOutlined,
  ShareAltOutlined,
  DatabaseOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  FileTextOutlined,
  UserOutlined,
  WifiOutlined,
} from '@ant-design/icons'

import { vmApi, federatedApi, systemApi, logApi, modelApi } from '@/api'
import useAppStore from '@/store'
import type { ConnectionState } from '@/types'

const { Title, Paragraph, Text } = Typography

interface DashboardStats {
  onlineVMs: number
  totalVMs: number
  runningTasks: number
  totalTasks: number
  totalModels: number
  recentErrors: number
  systemStatus: string
  systemUptime: number
}

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats>({
    onlineVMs: 0,
    totalVMs: 0,
    runningTasks: 0,
    totalTasks: 0,
    totalModels: 0,
    recentErrors: 0,
    systemStatus: 'unknown',
    systemUptime: 0,
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const wsState = useAppStore(state => state.wsState)
  const user = useAppStore(state => state.user)

  // 获取系统统计数据
  const fetchDashboardData = async () => {
    try {
      setLoading(true)
      setError(null)

      // 并行获取各种数据
      const [vmList, taskList, modelList, systemHealth, logStats] = await Promise.all([
        vmApi.getVMList({ page: 1, size: 100 }).catch(() => ({ list: [], total: 0 })),
        federatedApi.getTaskList({ page: 1, size: 100 }).catch(() => ({ tasks: [], total: 0 })),
        modelApi.getModelVersions({ page: 1, size: 100 }).catch(() => ({ records: [], total: 0 })),
        systemApi.getSystemHealth().catch(() => ({ status: 'unknown', uptime: 0, memory: 0, cpu: 0 })),
        logApi.getLogStatistics({
          startTime: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(), // 最近24小时
          endTime: new Date().toISOString()
        }).catch(() => ({ levelDistribution: { ERROR: 0 } }))
      ])

      const newStats: DashboardStats = {
        totalVMs: vmList.total || vmList.list?.length || 0,
        onlineVMs: vmList.list?.filter(vm => vm.status === 'RUNNING').length || 0,
        totalTasks: taskList.total || taskList.tasks?.length || 0,
        runningTasks: taskList.tasks?.filter(task => task.status === 'RUNNING').length || 0,
        totalModels: modelList.total || modelList.records?.length || 0,
        recentErrors: logStats.levelDistribution?.ERROR || 0,
        systemStatus: systemHealth.status || 'unknown',
        systemUptime: systemHealth.uptime || 0,
      }

      setStats(newStats)
    } catch (err: any) {
      console.error('Failed to fetch dashboard data:', err)
      setError(err.message || '获取数据失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDashboardData()
    
    // 定期刷新数据
    const interval = setInterval(fetchDashboardData, 30000) // 每30秒刷新
    
    return () => clearInterval(interval)
  }, [])

  // 获取WebSocket状态文本和颜色
  const getWSStatusDisplay = () => {
    switch (wsState) {
      case 'connected':
        return { text: '已连接', color: '#52c41a' }
      case 'connecting':
        return { text: '连接中', color: '#1890ff' }
      case 'reconnecting':
        return { text: '重连中', color: '#faad14' }
      case 'error':
        return { text: '连接错误', color: '#ff4d4f' }
      default:
        return { text: '未连接', color: '#d9d9d9' }
    }
  }

  const wsStatus = getWSStatusDisplay()

  if (error) {
    return (
      <div>
        <Title level={2}>系统总览</Title>
        <Alert
          message="数据加载失败"
          description={error}
          type="error"
          showIcon
          action={
            <button onClick={fetchDashboardData} style={{ border: 'none', background: 'transparent', color: '#1890ff', cursor: 'pointer' }}>
              重试
            </button>
          }
        />
      </div>
    )
  }

  return (
    <div>
      <div style={{ marginBottom: '24px' }}>
        <Title level={2}>系统总览</Title>
        <Paragraph type="secondary">
          欢迎回来，{user?.username}！水声联邦学习系统运行状态和关键指标概览
        </Paragraph>
      </div>

      <Spin spinning={loading}>
        {/* 核心指标卡片 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="虚拟机状态"
                value={stats.onlineVMs}
                suffix={`/ ${stats.totalVMs}`}
                prefix={<CloudServerOutlined />}
                valueStyle={{ color: stats.onlineVMs > 0 ? '#3f8600' : '#cf1322' }}
              />
              <Text type="secondary" style={{ fontSize: '12px' }}>
                在线 / 总数
              </Text>
            </Card>
          </Col>

          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="联邦学习任务"
                value={stats.runningTasks}
                suffix={`/ ${stats.totalTasks}`}
                prefix={<ShareAltOutlined />}
                valueStyle={{ color: stats.runningTasks > 0 ? '#1890ff' : '#8c8c8c' }}
              />
              <Text type="secondary" style={{ fontSize: '12px' }}>
                运行中 / 总数
              </Text>
            </Card>
          </Col>

          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="模型版本"
                value={stats.totalModels}
                prefix={<DatabaseOutlined />}
                valueStyle={{ color: '#722ed1' }}
              />
              <Text type="secondary" style={{ fontSize: '12px' }}>
                已注册模型数量
              </Text>
            </Card>
          </Col>

          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="系统状态"
                value={stats.systemStatus === 'OK' ? '正常' : '异常'}
                prefix={
                  stats.systemStatus === 'OK' ? 
                    <CheckCircleOutlined /> : 
                    <ExclamationCircleOutlined />
                }
                valueStyle={{ 
                  color: stats.systemStatus === 'OK' ? '#52c41a' : '#ff4d4f' 
                }}
              />
              <Text type="secondary" style={{ fontSize: '12px' }}>
                运行时间: {Math.floor(stats.systemUptime / 3600)}小时
              </Text>
            </Card>
          </Col>
        </Row>

        {/* 连接状态和错误统计 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} md={12}>
            <Card>
              <Statistic
                title="WebSocket连接"
                value={wsStatus.text}
                prefix={<WifiOutlined />}
                valueStyle={{ color: wsStatus.color }}
              />
              <Text type="secondary" style={{ fontSize: '12px' }}>
                实时通信状态
              </Text>
            </Card>
          </Col>

          <Col xs={24} md={12}>
            <Card>
              <Statistic
                title="近24小时错误"
                value={stats.recentErrors}
                prefix={<FileTextOutlined />}
                valueStyle={{ color: stats.recentErrors > 0 ? '#ff4d4f' : '#52c41a' }}
              />
              <Text type="secondary" style={{ fontSize: '12px' }}>
                系统错误日志数量
              </Text>
            </Card>
          </Col>
        </Row>

        {/* 功能模块导航 */}
        <Row gutter={[16, 16]}>
          <Col span={24}>
            <Card title="功能模块" size="small">
              <Row gutter={[16, 16]}>
                <Col xs={24} md={8}>
                  <Card size="small" hoverable style={{ textAlign: 'center' }}>
                    <CloudServerOutlined style={{ fontSize: '32px', color: '#1890ff', marginBottom: '12px' }} />
                    <Title level={4} style={{ marginBottom: '8px' }}>虚拟机管理</Title>
                    <Paragraph style={{ fontSize: '13px', margin: 0 }}>
                      管理和监控分布式虚拟机节点，实时查看运行状态和资源使用情况
                    </Paragraph>
                  </Card>
                </Col>
                
                <Col xs={24} md={8}>
                  <Card size="small" hoverable style={{ textAlign: 'center' }}>
                    <ShareAltOutlined style={{ fontSize: '32px', color: '#52c41a', marginBottom: '12px' }} />
                    <Title level={4} style={{ marginBottom: '8px' }}>联邦学习</Title>
                    <Paragraph style={{ fontSize: '13px', margin: 0 }}>
                      创建和管理联邦学习任务，监控训练进度和模型聚合效果
                    </Paragraph>
                  </Card>
                </Col>
                
                <Col xs={24} md={8}>
                  <Card size="small" hoverable style={{ textAlign: 'center' }}>
                    <DatabaseOutlined style={{ fontSize: '32px', color: '#722ed1', marginBottom: '12px' }} />
                    <Title level={4} style={{ marginBottom: '8px' }}>数据与模型</Title>
                    <Paragraph style={{ fontSize: '13px', margin: 0 }}>
                      管理训练数据集和模型版本，跟踪模型性能和部署状态
                    </Paragraph>
                  </Card>
                </Col>
              </Row>
            </Card>
          </Col>
        </Row>
      </Spin>
    </div>
  )
}

export default Dashboard 