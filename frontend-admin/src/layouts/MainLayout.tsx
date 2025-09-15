/**
 * 主布局组件
 * 提供应用的整体布局结构，包括侧边栏、头部导航和内容区域
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, Badge, Space, Typography } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  DashboardOutlined,
  CloudServerOutlined,
  ExperimentOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  SettingOutlined,
  UserOutlined,
  LogoutOutlined,
  BellOutlined,
  SearchOutlined
} from '@ant-design/icons'

import { useAuth, useWebSocket } from '@/store'
import { StatusIndicator } from '@/components'
import './MainLayout.module.css'

const { Header, Sider, Content } = Layout
const { Text } = Typography

interface MainLayoutProps {
  children?: React.ReactNode
}

const MainLayout: React.FC<MainLayoutProps> = ({ children }) => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuth()
  const { isConnected, connectionStatus } = useWebSocket()
  
  const [collapsed, setCollapsed] = useState(false)

  // 侧边栏菜单配置
  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘'
    },
    {
      key: '/admin',
      icon: <UserOutlined />,
      label: '系统管理'
    },
    {
      key: '/federated-learning',
      icon: <ExperimentOutlined />,
      label: '联邦学习',
      children: [
        { key: '/federated-learning/tasks', label: '任务管理' },
        { key: '/federated-learning/participants', label: '参与者' },
        { key: '/federated-learning/progress', label: '进度监控' }
      ]
    },
    {
      key: '/models',
      icon: <DatabaseOutlined />,
      label: '模型管理',
      children: [
        { key: '/models/versions', label: '版本管理' },
        { key: '/models/training-data', label: '训练数据' },
        { key: '/models/evaluation', label: '模型评估' }
      ]
    },
    {
      key: '/underwater-optimization',
      icon: <CloudServerOutlined />,
      label: '水声优化',
      children: [
        { key: '/underwater-optimization/environment', label: '环境参数' },
        { key: '/underwater-optimization/simulation', label: '仿真测试' },
        { key: '/underwater-optimization/analysis', label: '结果分析' }
      ]
    },
    {
      key: '/logs',
      icon: <FileTextOutlined />,
      label: '系统日志'
    }
  ]

  // 用户下拉菜单
  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人资料'
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '账户设置'
    },
    { type: 'divider' as const },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: () => {
        logout()
        navigate('/login')
      }
    }
  ]

  // 处理菜单点击
  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  // 获取当前选中的菜单项
  const getSelectedKeys = () => {
    const path = location.pathname
    // 找到匹配的菜单项
    for (const item of menuItems) {
      if (item.children) {
        for (const child of item.children) {
          if (path.startsWith(child.key)) {
            return [child.key]
          }
        }
      } else if (path.startsWith(item.key)) {
        return [item.key]
      }
    }
    return ['/dashboard']
  }

  // 获取展开的菜单项
  const getOpenKeys = () => {
    const path = location.pathname
    const openKeys: string[] = []
    
    for (const item of menuItems) {
      if (item.children) {
        for (const child of item.children) {
          if (path.startsWith(child.key)) {
            openKeys.push(item.key)
            break
          }
        }
      }
    }
    return openKeys
  }

  return (
    <Layout className="fed-main-layout">
      {/* 侧边栏 */}
      <Sider 
        trigger={null} 
        collapsible 
        collapsed={collapsed}
        width={240}
        className="fed-layout-sider"
        theme="dark"
      >
        {/* Logo 区域 */}
        <div className="fed-layout-logo">
          <div className="fed-logo-icon">F</div>
          {!collapsed && (
            <div className="fed-logo-text">
              <div className="fed-logo-title">FedUWAComm</div>
              <div className="fed-logo-subtitle">联邦学习平台</div>
            </div>
          )}
        </div>

        {/* 菜单 */}
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={getSelectedKeys()}
          defaultOpenKeys={getOpenKeys()}
          items={menuItems}
          onClick={handleMenuClick}
          className="fed-layout-menu"
        />
      </Sider>

      <Layout className="fed-layout-main">
        {/* 头部 */}
        <Header className="fed-layout-header">
          <div className="fed-header-left">
            {/* 折叠按钮 */}
            <div 
              className="fed-header-trigger"
              onClick={() => setCollapsed(!collapsed)}
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </div>

            {/* 面包屑导航可以在这里添加 */}
          </div>

          <div className="fed-header-right">
            <Space size="middle">
              {/* WebSocket 连接状态 */}
              <div className="fed-header-status">
                <StatusIndicator
                  status={isConnected ? 'online' : 'offline'}
                  text={isConnected ? '连接正常' : '连接断开'}
                  variant="dot"
                  size="small"
                  tooltip={`WebSocket 状态: ${connectionStatus}`}
                />
              </div>

              {/* 搜索按钮 */}
              <div className="fed-header-search">
                <SearchOutlined className="fed-header-icon" />
              </div>

              {/* 通知 */}
              <div className="fed-header-notifications">
                <Badge count={3} size="small">
                  <BellOutlined className="fed-header-icon" />
                </Badge>
              </div>

              {/* 用户信息 */}
              <Dropdown
                menu={{ items: userMenuItems }}
                placement="bottomRight"
                arrow
              >
                <div className="fed-header-user">
                  <Avatar
                    size="small"
                    icon={<UserOutlined />}
                  />
                  <Text className="fed-header-username">
                    {user?.username || '未知用户'}
                  </Text>
                </div>
              </Dropdown>
            </Space>
          </div>
        </Header>

        {/* 内容区域 */}
        <Content className="fed-layout-content">
          <div className="fed-content-wrapper">
            {children || <Outlet />}
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout 