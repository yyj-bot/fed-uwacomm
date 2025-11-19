/**
 * 仪表盘页面
 * 提供系统总览、统计数据和快速操作入口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect } from 'react'
import { Row, Col, Card, Progress, Typography, Space, List, Avatar } from 'antd'
import { 
  CloudServerOutlined, 
  ExperimentOutlined
} from '@ant-design/icons'

import { useDashboard, useVM, useTask } from '@/store'
import type { VMSpeed } from '@/types'
import { StatusIndicator } from '@/components'
import { 
  OverviewCards, 
  SystemStatus, 
  TaskProgress, 
  RecentActivities
} from './components'
import styles from './DashboardPage.module.css'

const { Title, Text } = Typography

const formatSpeed = (speed?: VMSpeed): string => {
  if (speed === undefined || speed === null) {
    return '—'
  }

  if (typeof speed === 'number') {
    return `${speed.toFixed(1)} m/s`
  }

  if (typeof speed === 'string') {
    return speed
  }

  if (typeof speed.value === 'number') {
    const unit = speed.unit || 'm/s'
    const precision = typeof speed.precision === 'number' ? speed.precision : (unit === '°/s' ? 1 : 1)
    return `${speed.value.toFixed(precision)} ${unit}`
  }

  return '—'
}

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
  
  const { vmList, fetchVMList } = useVM()
  const { taskList, fetchTaskList } = useTask()
  
  // 计算水下机器人统计
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
      // 检查认证状态
      const token = localStorage.getItem('access_token')
      console.log('📋 Dashboard加载数据前检查:', {
        hasToken: !!token,
        tokenPreview: token ? '***' + token.slice(-10) : 'null',
        pathname: window.location.pathname
      })
      
      if (!token) {
        console.warn('⚠️ Dashboard没有token，跳过数据加载')
        return
      }
      
      // 延迟500ms再加载数据，确保认证状态稳定
      await new Promise(resolve => setTimeout(resolve, 500))
      
      console.log('📊 开始加载Dashboard数据')
      
      // 并行加载所有数据
      await Promise.allSettled([
        fetchOverview().then(() => console.log('✅ 概览数据加载成功')).catch((error) => console.error('❌ 仪表盘概览数据加载失败:', error)),
        fetchChartData().then(() => console.log('✅ 图表数据加载成功')).catch((error) => console.error('❌ 图表数据加载失败:', error)),
        fetchRecentActivities().then(() => console.log('✅ 活动数据加载成功')).catch((error) => console.error('❌ 最近活动数据加载失败:', error)),
        fetchVMList({ page: 1, size: 100 }).then(() => console.log('✅ 水下机器人数据加载成功')).catch((error) => console.error('❌ 水下机器人数据加载失败:', error)),
        fetchTaskList({ page: 1, size: 100 }).then(() => console.log('✅ 任务数据加载成功')).catch((error) => console.error('❌ 任务数据加载失败:', error))
      ])
      
      console.log('🎯 Dashboard数据加载流程完成（忽略API错误）')
    }
    
    loadData()
  }, [fetchOverview, fetchChartData, fetchRecentActivities, fetchVMList, fetchTaskList])

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
    <div className={styles['fed-dashboard']}>
      {/* 页面标题 */}
      <div className={styles['fed-dashboard-header']}>
        <div className={styles['fed-dashboard-title']}>
          <Title level={2}>仪表盘</Title>
          <Text type="secondary">系统运行状态一览</Text>
        </div>
      </div>

      {/* 概览卡片 */}
      <OverviewCards 
        overview={overview}
        loading={overviewLoading}
      />

      {/* 主要内容区域 */}
      <div className={styles['fed-dashboard-content']} style={{ display: 'flex', gap: '16px', alignItems: 'stretch', flexWrap: 'wrap' }}>
        {/* 左侧列 - 监控面板 */}
        <div style={{ flex: '1 1 calc(66.666% - 8px)', minWidth: '600px', display: 'flex', flexDirection: 'column' }}>
          {/* 系统状态监控 */}
          <SystemStatus />

          {/* 资源状态 */}
          <div style={{ marginTop: '16px', display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
            <div style={{ flex: '1 1 calc(50% - 8px)', minWidth: '300px' }}>
              {/* 运行中的水下机器人 */}
              <Card 
                title="运行中的水下机器人" 
                size="small"
                className={`${styles['fed-dashboard-card']} ${styles['fed-card-with-scroll']}`}
                extra={<Text type="secondary">{runningVMs}/{totalVMs}</Text>}
                style={{ height: '100%', minHeight: '320px', background: 'white', marginBottom: 0 }}
                styles={{ 
                  body: { height: 'calc(100% - 57px)', overflowY: 'auto', background: 'white' }
                }}
              >
                {vms?.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                    <CloudServerOutlined style={{ fontSize: '48px', marginBottom: '16px' }} />
                    <div>暂无数据</div>
                  </div>
                ) : (
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
                            <Space size={12} wrap>
                              <StatusIndicator
                                status={vm.status === 'RUNNING' ? 'running' : 'stopped'}
                                variant="text"
                                size="small"
                              />
                              <Text type="secondary">
                                电量: {typeof vm.batteryLevel === 'number' ? `${vm.batteryLevel}%` : '—'}
                              </Text>
                              <Text type="secondary">
                                速度: {formatSpeed(vm.speed)}
                              </Text>
                            </Space>
                          }
                        />
                      </List.Item>
                    )}
                  />
                )}
              </Card>
            </div>

            <div style={{ flex: '1 1 calc(50% - 8px)', minWidth: '300px' }}>
              {/* 活跃任务 */}
              <Card 
                title="活跃任务" 
                size="small"
                className={`${styles['fed-dashboard-card']} ${styles['fed-card-with-scroll']}`}
                extra={<Text type="secondary">{activeTasks?.length || 0}</Text>}
                style={{ height: '100%', minHeight: '320px', background: 'white', marginBottom: 0 }}
                styles={{ 
                  body: { height: 'calc(100% - 57px)', overflowY: 'auto', background: 'white' }
                }}
              >
                {activeTasks?.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                    <ExperimentOutlined style={{ fontSize: '48px', marginBottom: '16px' }} />
                    <div>暂无运行中的任务</div>
                  </div>
                ) : (
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
                          title={<Text strong>{task.taskName}</Text>}
                          description={
                            <Space direction="vertical" style={{ width: '100%' }}>
                              <Progress 
                                percent={task.progress || 0} 
                                size="small" 
                                status={task.status === 'FAILED' ? 'exception' : 'active'}
                              />
                              <Text type="secondary">
                                {task.participantCount || 0} 个参与者
                              </Text>
                            </Space>
                          }
                        />
                      </List.Item>
                    )}
                  />
                )}
              </Card>
            </div>
          </div>
          
          {/* 任务进度监控 */}
          <div style={{ marginTop: '24px' }}>
            <TaskProgress tasks={tasks} />
          </div>
        </div>

        {/* 右侧列 - 信息面板 */}
        <div style={{ flex: '1 1 calc(33.333% - 8px)', minWidth: '320px', display: 'flex', flexDirection: 'column' }}>
          {/* 最近活动 */}
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <RecentActivities 
              activities={recentActivities}
              loading={overviewLoading}
            />
          </div>
        </div>
      </div>
    </div>
  )
}

export default DashboardPage
