/**
 * 仪表盘页面
 * 提供系统总览、统计数据和快速操作入口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect } from 'react'
import { Row, Col, Card, Statistic, Progress, Typography, Space, Button, List, Avatar } from 'antd'
import { 
  CloudServerOutlined, 
  ExperimentOutlined, 
  DatabaseOutlined, 
  TeamOutlined,
  TrophyOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined
} from '@ant-design/icons'

import { useDashboard, useVM, useTask, useWebSocket } from '@/store'
import { StatusIndicator, Table } from '@/components'
import { 
  OverviewCards, 
  SystemStatus, 
  TaskProgress, 
  RecentActivities,
  PerformanceCharts 
} from './components'
import './DashboardPage.module.css'

const { Title, Text } = Typography

const DashboardPage: React.FC = () => {
  const { 
    overview, 
    chartData, 
    recentActivities,
    overviewLoading,
    fetchOverview,
    fetchChartData,
    fetchRecentActivities
  } = useDashboard()
  
  const { vmList } = useVM()
  const { taskList } = useTask()
  const { isConnected } = useWebSocket()
  
  // 计算虚拟机统计
  const totalVMs = vmList?.length || 0
  const runningVMs = vmList?.filter(vm => vm.status === 'RUNNING')?.length || 0
  const vms = vmList || []
  
  // 计算任务统计
  const tasks = taskList || []
  const activeTasks = taskList?.filter(task => task.status === 'RUNNING') || []
  const completedTasks = taskList?.filter(task => task.status === 'COMPLETED') || []

  // 页面加载时获取数据
  useEffect(() => {
    const loadData = async () => {
      try {
        await fetchOverview()
        console.log('仪表盘概览数据加载成功')
      } catch (error) {
        console.error('仪表盘概览数据加载失败:', error)
      }
      
      try {
        await fetchChartData()
        console.log('图表数据加载成功')
      } catch (error) {
        console.error('图表数据加载失败:', error)
      }
      
      try {
        await fetchRecentActivities()
        console.log('最近活动数据加载成功')
      } catch (error) {
        console.error('最近活动数据加载失败:', error)
      }
    }
    
    loadData()
  }, [fetchOverview, fetchChartData, fetchRecentActivities])

  // 如果正在加载，显示加载状态
  if (overviewLoading) {
    return (
      <div className="fed-dashboard">
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <div>正在加载仪表盘数据...</div>
        </div>
      </div>
    )
  }

  return (
    <div className="fed-dashboard">
      {/* 页面标题 */}
      <div className="fed-dashboard-header">
        <div className="fed-dashboard-title">
          <Title level={2}>仪表盘</Title>
          <Text type="secondary">系统运行状态一览</Text>
        </div>
        <div className="fed-dashboard-actions">
          <Space>
            <StatusIndicator
              status={isConnected ? 'online' : 'offline'}
              text={isConnected ? '系统正常' : '连接异常'}
              variant="badge"
            />
            <Button type="primary" onClick={() => window.location.reload()}>
              刷新数据
            </Button>
          </Space>
        </div>
      </div>

      {/* 概览卡片 */}
      <OverviewCards 
        overview={overview}
        loading={overviewLoading}
      />

      {/* 主要内容区域 */}
      <Row gutter={[24, 24]} className="fed-dashboard-content">
        {/* 左侧列 */}
        <Col xs={24} lg={16}>
          {/* 系统状态监控 */}
          <SystemStatus />
          
          {/* 任务进度监控 */}
          <TaskProgress tasks={tasks} />
          
          {/* 性能图表 */}
          <PerformanceCharts 
            chartData={chartData}
            loading={overviewLoading}
          />
        </Col>

        {/* 右侧列 */}
        <Col xs={24} lg={8}>
          {/* 快速操作 */}
          <Card 
            title="快速操作" 
            size="small"
            className="fed-dashboard-card"
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button 
                type="primary" 
                icon={<ExperimentOutlined />} 
                block
              >
                创建联邦学习任务
              </Button>
              <Button 
                icon={<CloudServerOutlined />} 
                block
              >
                启动虚拟机
              </Button>
              <Button 
                icon={<DatabaseOutlined />} 
                block
              >
                上传训练数据
              </Button>
            </Space>
          </Card>

          {/* 最近活动 */}
          <RecentActivities 
            activities={recentActivities}
            loading={overviewLoading}
          />

          {/* 运行中的虚拟机 */}
          <Card 
            title="运行中的虚拟机" 
            size="small"
            className="fed-dashboard-card"
            extra={<Text type="secondary">{runningVMs}/{totalVMs}</Text>}
          >
            <List
              size="small"
              dataSource={vms?.slice(0, 5) || []}
              renderItem={(vm) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={
                      <Avatar 
                        size="small" 
                        icon={<CloudServerOutlined />}
                        style={{ backgroundColor: vm.status === 'RUNNING' ? '#52c41a' : '#d9d9d9' }}
                      />
                    }
                    title={<Text strong>{vm.name}</Text>}
                    description={
                      <Space>
                        <StatusIndicator
                          status={vm.status === 'RUNNING' ? 'running' : 'stopped'}
                          variant="text"
                          size="small"
                        />
                        <Text type="secondary">CPU: {(vm as any).cpuUsage || 0}%</Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>

          {/* 活跃任务 */}
          <Card 
            title="活跃任务" 
            size="small"
            className="fed-dashboard-card"
            extra={<Text type="secondary">{activeTasks?.length || 0}</Text>}
          >
            <List
              size="small"
              dataSource={activeTasks?.slice(0, 5) || []}
              renderItem={(task) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={
                      <Avatar 
                        size="small" 
                        icon={<ExperimentOutlined />}
                        style={{ backgroundColor: '#1890ff' }}
                      />
                    }
                    title={<Text strong>{(task as any).name}</Text>}
                    description={
                      <Space direction="vertical" style={{ width: '100%' }}>
                        <Progress 
                          percent={(task as any).progress || 0} 
                          size="small" 
                          status={task.status === 'FAILED' ? 'exception' : 'active'}
                        />
                        <Text type="secondary">
                          {(task as any).participantCount || 0} 个参与者
                        </Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default DashboardPage
