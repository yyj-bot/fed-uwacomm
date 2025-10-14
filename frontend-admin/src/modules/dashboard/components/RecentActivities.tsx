/**
 * 最近活动组件
 * 显示系统的最近操作和事件记录
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { Card, List, Avatar, Typography, Tag, Skeleton, Button } from 'antd'
import { useNavigate } from 'react-router-dom'
import { 
  UserOutlined, 
  CloudServerOutlined, 
  ExperimentOutlined, 
  SettingOutlined,
  FileTextOutlined,
  ClockCircleOutlined
} from '@ant-design/icons'

const { Text } = Typography

interface Activity {
  id: string
  type: 'user' | 'vm' | 'task' | 'system' | 'data'
  title: string
  description: string
  timestamp: string
  user?: string
  status?: 'success' | 'error' | 'warning' | 'info'
}

interface RecentActivitiesProps {
  activities?: Activity[]
  loading?: boolean
}

const RecentActivities: React.FC<RecentActivitiesProps> = ({ 
  activities = [], 
  loading = false 
}) => {
  const navigate = useNavigate()
  
  // 获取活动类型图标
  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'user':
        return <UserOutlined />
      case 'vm':
        return <CloudServerOutlined />
      case 'task':
        return <ExperimentOutlined />
      case 'system':
        return <SettingOutlined />
      case 'data':
        return <FileTextOutlined />
      default:
        return <ClockCircleOutlined />
    }
  }

  // 获取活动类型颜色
  const getActivityColor = (type: string, status?: string) => {
    if (status) {
      switch (status) {
        case 'success':
          return '#52c41a'
        case 'error':
          return '#ff4d4f'
        case 'warning':
          return '#faad14'
        default:
          return '#1890ff'
      }
    }
    
    switch (type) {
      case 'user':
        return '#722ed1'
      case 'vm':
        return '#13c2c2'
      case 'task':
        return '#1890ff'
      case 'system':
        return '#faad14'
      case 'data':
        return '#52c41a'
      default:
        return '#d9d9d9'
    }
  }

  // 获取状态标签
  const getStatusTag = (status?: string) => {
    if (!status) return null
    
    const statusConfig = {
      success: { color: 'success', text: '成功' },
      error: { color: 'error', text: '失败' },
      warning: { color: 'warning', text: '警告' },
      info: { color: 'processing', text: '信息' }
    }
    
    const config = statusConfig[status as keyof typeof statusConfig]
    return config ? <Tag color={config.color}>{config.text}</Tag> : null
  }

  // 格式化时间
  const formatTime = (timestamp: string) => {
    const now = new Date()
    const time = new Date(timestamp)
    const diff = now.getTime() - time.getTime()
    
    const minutes = Math.floor(diff / (1000 * 60))
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 7) return `${days}天前`
    return time.toLocaleDateString()
  }

  if (loading) {
    return (
      <Card 
        title="最近活动" 
        size="small"
        className="fed-dashboard-card"
      >
        <List
          dataSource={Array(5).fill(0)}
          renderItem={() => (
            <List.Item>
              <Skeleton avatar active paragraph={{ rows: 1 }} />
            </List.Item>
          )}
        />
      </Card>
    )
  }

  return (
    <Card 
      title="最近活动" 
      size="small"
      className="fed-dashboard-card"
      extra={
        <Button 
          type="link" 
          size="small"
          onClick={() => navigate('/logs')}
        >
          查看全部
        </Button>
      }
    >
      {activities.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px 0', color: '#00000040' }}>
          <ClockCircleOutlined style={{ fontSize: '32px', marginBottom: '12px' }} />
          <div>暂无活动记录</div>
        </div>
      ) : (
        <List
          className="fed-activity-list"
          dataSource={activities.slice(0, 10)}
          renderItem={(activity) => (
            <List.Item>
              <List.Item.Meta
                avatar={
                  <Avatar 
                    size="small"
                    style={{ 
                      backgroundColor: getActivityColor(activity.type, activity.status),
                      color: 'white'
                    }}
                    icon={getActivityIcon(activity.type)}
                  />
                }
                title={
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Text strong style={{ fontSize: '14px' }}>
                      {activity.title}
                    </Text>
                    {getStatusTag(activity.status)}
                  </div>
                }
                description={
                  <div>
                    <div style={{ marginBottom: '4px' }}>
                      <Text type="secondary" style={{ fontSize: '13px' }}>
                        {activity.description}
                      </Text>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      {activity.user && (
                        <Text type="secondary" style={{ fontSize: '12px' }}>
                          {activity.user}
                        </Text>
                      )}
                      <Text type="secondary" style={{ fontSize: '12px' }}>
                        {formatTime(activity.timestamp)}
                      </Text>
                    </div>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      )}
    </Card>
  )
}

export default RecentActivities
