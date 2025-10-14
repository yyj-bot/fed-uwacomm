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
  SafetyOutlined,
  TeamOutlined,
  SecurityScanOutlined
} from '@ant-design/icons'

const { Title, Text, Paragraph } = Typography

import { useAdmin } from '@/store'
import { Table, StatusIndicator } from '@/components'
import { UserManagement, PermissionManagement } from './components'
import styles from './SystemManagementPage.module.css'

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
  const stats: Array<{
    title: string
    value: number | string
    icon: React.ReactNode
    color: string
    suffix?: string
  }> = [
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
      title: '管理员',
      value: userStatistics?.adminUsers || userList?.filter(user => user.role === 'ADMIN').length || 0,
      icon: <SafetyOutlined />,
      color: '#722ed1'
    },
    {
      title: '锁定用户',
      value: userStatistics?.lockedUsers || userList?.filter(user => user.status === 'LOCKED').length || 0,
      icon: <SecurityScanOutlined />,
      color: '#fa8c16'
    }
  ]

  return (
    <div className={styles['fed-system-management-page']}>
      {/* 页面标题 - 参考联邦学习样式 */}
      <div className={styles['fed-system-management-header']}>
        <div className={styles['fed-system-management-title']}>
          <Title level={2} style={{ 
            margin: 0,
            fontSize: '26px',
            fontWeight: 700,
            background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text'
          }}>
            系统管理
          </Title>
          <Paragraph style={{ 
            margin: '8px 0 0 0', 
            color: '#595959',
            fontSize: '14px',
            fontWeight: 400
          }}>
            管理用户和权限
          </Paragraph>
        </div>
        <div className={styles['fed-system-management-status']}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            <span style={{
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              background: '#52c41a',
              boxShadow: '0 0 8px rgba(82, 196, 26, 0.6)',
              animation: 'pulse 2s ease-in-out infinite'
            }} />
            <span style={{ 
              color: '#262626', 
              fontWeight: 500,
              fontSize: '14px'
            }}>系统正常运行</span>
          </div>
        </div>
      </div>

      {/* 统计卡片 - 精简版 */}
      <Row gutter={[16, 16]} className={styles['fed-system-management-stats']}>
        {stats.map((stat, index) => (
          <Col xs={12} sm={6} md={6} key={index}>
            <Card 
              className={styles['fed-stat-card-compact']}
              bordered={false}
              hoverable
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div 
                  className={styles['fed-stat-icon-compact']} 
                  style={{ 
                    color: stat.color,
                    fontSize: '28px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: '48px',
                    height: '48px',
                    borderRadius: '12px',
                    background: `linear-gradient(135deg, ${stat.color}18 0%, ${stat.color}08 100%)`,
                    flexShrink: 0
                  }}
                >
                  {stat.icon}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: '11px', color: '#8c8c8c', marginBottom: '4px', fontWeight: 500 }}>
                    {stat.title}
                  </div>
                  <div style={{ fontSize: '24px', fontWeight: 'bold', color: stat.color, lineHeight: 1 }}>
                    {stat.value}
                    {stat.suffix && <span style={{ fontSize: '14px', marginLeft: '2px' }}>{stat.suffix}</span>}
                  </div>
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* 管理功能选项卡 */}
      <Card className={styles['fed-system-management-content']}>
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
