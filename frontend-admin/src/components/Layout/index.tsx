import React from 'react'
import { Layout as AntLayout, Menu, Typography } from 'antd'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  DashboardOutlined,
  EnvironmentOutlined,
  BarChartOutlined,
  ShareAltOutlined,
  TrophyOutlined,
  MonitorOutlined,
} from '@ant-design/icons'

const { Header, Sider, Content } = AntLayout
const { Title } = Typography

interface LayoutProps {
  children: React.ReactNode
}

// 导航菜单项配置 - 单一数据源
const menuItems = [
  {
    key: '/',
    icon: <DashboardOutlined />,
    label: '总览仪表板',
  },
  {
    key: '/environment',
    icon: <EnvironmentOutlined />,
    label: '环境分析',
  },
  {
    key: '/models',
    icon: <BarChartOutlined />,
    label: '模型性能',
  },
  {
    key: '/federated',
    icon: <ShareAltOutlined />,
    label: '联邦学习',
  },
  {
    key: '/optimization',
    icon: <TrophyOutlined />,
    label: '通信优化',
  },
  {
    key: '/monitor',
    icon: <MonitorOutlined />,
    label: '系统监控',
  },
] as const

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const navigate = useNavigate()
  const location = useLocation()

  // 简单的导航处理
  const handleMenuClick = (key: string): void => {
    navigate(key)
  }

  return (
    <AntLayout style={{ height: '100vh' }}>
      <Sider
        width={240}
        style={{
          background: '#001529',
          boxShadow: '2px 0 8px rgba(0,0,0,0.1)',
        }}
      >
        <div style={{ padding: '16px', textAlign: 'center' }}>
          <Title
            level={4}
            style={{
              color: '#fff',
              margin: 0,
              fontSize: '16px',
              fontWeight: 600,
            }}
          >
            FedUWAComm
          </Title>
          <div style={{ color: '#8c8c8c', fontSize: '12px', marginTop: '4px' }}>
            水声联邦学习管理端
          </div>
        </div>

        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => handleMenuClick(key)}
          style={{ borderRight: 0 }}
        />
      </Sider>

      <AntLayout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            boxShadow: '0 1px 4px rgba(0,21,41,0.08)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <Title level={3} style={{ margin: 0, color: '#001529' }}>
            {menuItems.find(item => item.key === location.pathname)?.label || '管理面板'}
          </Title>
          
          <div style={{ color: '#8c8c8c', fontSize: '14px' }}>
            {new Date().toLocaleString('zh-CN')}
          </div>
        </Header>

        <Content
          style={{
            margin: '16px',
            padding: '24px',
            background: '#fff',
            borderRadius: '8px',
            overflow: 'auto',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          }}
        >
          {children}
        </Content>
      </AntLayout>
    </AntLayout>
  )
}

export default Layout 