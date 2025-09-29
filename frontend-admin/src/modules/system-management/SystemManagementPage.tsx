/**
 * 管理员页面
 * 提供系统管理功能，包括用户管理、权限配置、系统设置等
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect } from 'react'
import { Row, Col, Card, Typography, Space, Button, Tabs, Statistic } from 'antd'
import { 
  UserOutlined, 
  SettingOutlined, 
  SafetyOutlined,
  DatabaseOutlined,
  TeamOutlined,
  SecurityScanOutlined
} from '@ant-design/icons'

import { useAdmin } from '@/store'
import { Table, StatusIndicator } from '@/components'
import { UserManagement, PermissionManagement } from './components'
import './SystemManagementPage.module.css'

const { Title, Text } = Typography

const SystemManagementPage: React.FC = () => {
  const { 
    userList,
    userListTotal,
    userStatistics,
    fetchUserList,
    fetchUserStatistics
  } = useAdmin()

  useEffect(() => {
    fetchUserList()
    fetchUserStatistics()
  }, [fetchUserList, fetchUserStatistics])

  // 统计数据
  const stats = [
    {
      title: '总用户数',
      value: userStatistics?.totalUsers || userListTotal,
      icon: <UserOutlined />,
      color: '#1890ff'
    },
    {
      title: '活跃用户',
      value: userStatistics?.activeUsers || userList?.filter(user => user.status === 'ACTIVE').length || 0,
      icon: <TeamOutlined />,
      color: '#52c41a'
    },
    {
      title: '系统模块',
      value: 8,
      icon: <DatabaseOutlined />,
      color: '#722ed1'
    },
    {
      title: '安全等级',
      value: 'A',
      suffix: '级',
      icon: <SecurityScanOutlined />,
      color: '#fa8c16'
    }
  ]

  return (
    <div className="fed-system-management-page">
      {/* 页面标题 */}
      <div className="fed-system-management-header">
        <div className="fed-system-management-title">
          <Title level={2}>系统管理</Title>
          <Text type="secondary">管理用户和权限</Text>
        </div>
        <div className="fed-system-management-actions">
          <Space>
            <StatusIndicator
              status="running"
              text="系统正常"
              variant="badge"
            />
          </Space>
        </div>
      </div>

      {/* 统计卡片 */}
      <Row gutter={[16, 16]} className="fed-system-management-stats">
        {stats.map((stat, index) => (
          <Col xs={12} sm={6} key={index}>
            <Card className="fed-stat-card">
              <Statistic
                title={stat.title}
                value={stat.value}
                suffix={stat.suffix}
                prefix={
                  <div 
                    className="fed-stat-icon" 
                    style={{ color: stat.color }}
                  >
                    {stat.icon}
                  </div>
                }
                valueStyle={{ color: stat.color }}
              />
            </Card>
          </Col>
        ))}
      </Row>

      {/* 管理功能选项卡 */}
      <Card className="fed-system-management-content">
        <Tabs 
          defaultActiveKey="users" 
          size="large"
          items={[
            {
              key: 'users',
              label: (
                <span>
                  <UserOutlined />
                  用户管理
                </span>
              ),
              children: <UserManagement />
            },
            {
              key: 'permissions',
              label: (
                <span>
                  <SafetyOutlined />
                  权限管理
                </span>
              ),
              children: <PermissionManagement />
            }
          ]}
        />
      </Card>
    </div>
  )
}

export default SystemManagementPage
