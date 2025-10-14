/**
 * 任务进度监控组件
 * 显示当前运行任务的进度和状态
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { Card, Progress, List, Typography, Space, Tag, Button } from 'antd'
import { useNavigate } from 'react-router-dom'
import { 
  PlayCircleOutlined, 
  PauseCircleOutlined, 
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons'

import { StatusIndicator, Table } from '@/components'
import styles from '../DashboardPage.module.css'
// 移除类型导入，使用 any 类型

const { Text, Title } = Typography

interface TaskProgressProps {
  tasks?: any[]
}

const TaskProgress: React.FC<TaskProgressProps> = ({ tasks = [] }) => {
  const navigate = useNavigate()
  
  // 筛选运行中的任务
  const runningTasks = tasks.filter(task => task.status === 'RUNNING')
  
  // 获取任务状态图标
  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'RUNNING':
        return <PlayCircleOutlined style={{ color: '#1890ff' }} />
      case 'PAUSED':
        return <PauseCircleOutlined style={{ color: '#faad14' }} />
      case 'COMPLETED':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />
      case 'FAILED':
        return <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />
      default:
        return <ClockCircleOutlined style={{ color: '#d9d9d9' }} />
    }
  }

  // 获取状态标签颜色
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'RUNNING':
        return 'processing'
      case 'PAUSED':
        return 'warning'
      case 'COMPLETED':
        return 'success'
      case 'FAILED':
        return 'error'
      default:
        return 'default'
    }
  }

  return (
    <Card 
      title="任务进度监控" 
      className={`${styles['fed-dashboard-card']} ${styles['fed-card-with-scroll']}`}
      style={{ background: 'white', maxHeight: '600px' }}
      styles={{ 
        body: { background: 'white', overflowY: 'auto', maxHeight: '500px' }
      }}
      extra={
        <Space>
          <Text type="secondary">运行中: {runningTasks.length}</Text>
          <Button 
            size="small" 
            type="link"
            onClick={() => navigate('/federated-learning/tasks')}
          >
            查看全部
          </Button>
        </Space>
      }
    >
      {runningTasks.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px 0', color: '#00000040' }}>
          <ClockCircleOutlined style={{ fontSize: '48px', marginBottom: '16px' }} />
          <div>暂无运行中的任务</div>
        </div>
      ) : (
        <List
          dataSource={runningTasks.slice(0, 5)}
          renderItem={(task) => (
            <List.Item
              actions={[
                <Button 
                  type="link" 
                  size="small"
                  key="detail"
                  onClick={() => navigate(`/federated-learning/tasks/${task.taskId}`)}
                >
                  查看详情
                </Button>
              ]}
            >
              <List.Item.Meta
                avatar={getStatusIcon(task.status)}
                title={
                  <Space>
                    <Text strong>{task.taskName}</Text>
                    <Tag color={getStatusColor(task.status)}>
                      {task.status === 'RUNNING' ? '运行中' : 
                       task.status === 'PAUSED' ? '已暂停' :
                       task.status === 'COMPLETED' ? '已完成' :
                       task.status === 'FAILED' ? '失败' : '等待中'}
                    </Tag>
                  </Space>
                }
                description={
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                      <Text type="secondary">
                        参与者: {task.participantCount || 0} | 
                        轮次: {task.currentRound || 0}/{task.totalRounds || 0}
                      </Text>
                    </div>
                    <Progress
                      percent={task.progress || 0}
                      size="small"
                      status={task.status === 'FAILED' ? 'exception' : 'active'}
                      format={(percent) => (
                        <Text style={{ fontSize: '12px' }}>
                          {percent}%
                        </Text>
                      )}
                    />
                    <div>
                      <Space>
                        <Text type="secondary" style={{ fontSize: '12px' }}>
                          开始时间: {task.startedAt ? new Date(task.startedAt).toLocaleString() : '-'}
                        </Text>
                        {task.completedAt && task.status === 'COMPLETED' && (
                          <Text type="secondary" style={{ fontSize: '12px' }}>
                            完成时间: {new Date(task.completedAt).toLocaleString()}
                          </Text>
                        )}
                      </Space>
                    </div>
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      )}
    </Card>
  )
}

export default TaskProgress
