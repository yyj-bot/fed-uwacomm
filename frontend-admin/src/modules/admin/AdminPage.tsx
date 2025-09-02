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
import { UserManagement, SystemSettings, PermissionManagement } from './components'
import './AdminPage.module.css'

const { Title, Text } = Typography
const { TabPane } = Tabs

const AdminPage: React.FC = () => {
  const { 
    userList,
    userListTotal,
    fetchUserList
  } = useAdmin()

  useEffect(() => {
    fetchUserList()
  }, [fetchUserList])

  // 统计数据
  const stats = [
    {
      title: '总用户数',
      value: userListTotal,
      icon: <UserOutlined />,
      color: '#1890ff'
    },
    {
      title: '活跃用户',
      value: userList?.filter(user => user.status === 'ACTIVE').length || 0,
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
    <div className="fed-admin-page">
      {/* 页面标题 */}
      <div className="fed-admin-header">
        <div className="fed-admin-title">
          <Title level={2}>系统管理</Title>
          <Text type="secondary">管理用户、权限和系统配置</Text>
        </div>
        <div className="fed-admin-actions">
          <Space>
            <StatusIndicator
              status="running"
              text="系统正常"
              variant="badge"
            />
            <Button type="primary" icon={<SettingOutlined />}>
              系统设置
            </Button>
          </Space>
        </div>
      </div>

      {/* 统计卡片 */}
      <Row gutter={[16, 16]} className="fed-admin-stats">
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
      <Card className="fed-admin-content">
        <Tabs defaultActiveKey="users" size="large">
          <TabPane 
            tab={
              <span>
                <UserOutlined />
                用户管理
              </span>
            } 
            key="users"
          >
            <UserManagement />
          </TabPane>

          <TabPane 
            tab={
              <span>
                <SafetyOutlined />
                权限管理
              </span>
            } 
            key="permissions"
          >
            <PermissionManagement />
          </TabPane>

          <TabPane 
            tab={
              <span>
                <SettingOutlined />
                系统设置
              </span>
            } 
            key="settings"
          >
            <SystemSettings />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  )
}

export default AdminPage
