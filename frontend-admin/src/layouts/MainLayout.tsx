/**
 * 主布局组件
 * 提供应用的整体布局结构，包括侧边栏、头部导航和内容区域
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { Layout, Menu, Avatar, Dropdown, Badge, Space, Typography, Button } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  DashboardOutlined,
  CloudServerOutlined,
  ExperimentOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  FolderOutlined,
  SettingOutlined,
  UserOutlined,
  LogoutOutlined,
  BellOutlined,
  SearchOutlined,
  DesktopOutlined
} from '@ant-design/icons'

import { useAuth } from '@/store'
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
        { key: '/federated-learning/tasks', label: '任务管理' }
      ]
    },
    {
      key: '/models',
      icon: <DatabaseOutlined />,
      label: '模型管理',
      children: [
        { key: '/models/initial', label: '初始模型' },
        { key: '/models/versions', label: '版本管理' }
      ]
    },
    {
      key: '/vm-management',
      icon: <DesktopOutlined />,
      label: '虚拟机管理',
      children: [
        { key: '/vm-management/list', label: '虚拟机列表' },
        { key: '/vm-management/admin', label: '虚拟机管理' }
      ]
    },
    {
      key: '/robot-control',
      icon: <CloudServerOutlined />,
      label: '机器人操控'
    },
    {
      key: '/logs',
      icon: <FileTextOutlined />,
      label: '系统日志'
    }
  ]

  // 处理用户菜单点击
  const handleUserMenuClick = ({ key }: { key: string }) => {
    switch (key) {
      case 'profile':
        navigate('/user/profile')
        break
      case 'settings':
        navigate('/user/settings')
        break
      case 'logout':
        logout()
        navigate('/login')
        break
      default:
        break
    }
  }

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
      label: '退出登录'
    }
  ]

  // 处理菜单点击
  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  // 获取当前选中的菜单项
  const getSelectedKeys = () => {
    const path = location.pathname
    
    // 如果是用户相关页面，不选中任何侧边栏菜单项
    if (path.startsWith('/user/')) {
      return []
    }
    
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
    
    // 只有在根路径或仪表盘路径时才默认选中仪表盘
    if (path === '/' || path === '/dashboard') {
      return ['/dashboard']
    }
    
    // 其他未匹配的路径不选中任何菜单项
    return []
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
      {/* 顶部栏 - 跨越整个顶部 */}
      <Header className="fed-layout-header">
        <div className="fed-header-left">
          {/* 折叠按钮 */}
          <div 
            className="fed-header-trigger"
            onClick={() => setCollapsed(!collapsed)}
          >
            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </div>
          
          {/* Logo区域 */}
          <div className="fed-header-logo" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div className="fed-logo-icon" style={{ 
              width: '32px', 
              height: '32px', 
              background: 'linear-gradient(135deg, #1890ff, #722ed1)', 
              borderRadius: '6px', 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              color: 'white', 
              fontWeight: 'bold', 
              fontSize: '16px' 
            }}>F</div>
            {!collapsed && (
              <div className="fed-logo-text" style={{ display: 'flex', flexDirection: 'column' }}>
                <div className="fed-logo-title" style={{ color: 'white', fontSize: '16px', fontWeight: '600', lineHeight: '1.2' }}>FedUWAComm</div>
                <div className="fed-logo-subtitle" style={{ color: 'rgba(255, 255, 255, 0.65)', fontSize: '12px', lineHeight: '1.2' }}>联邦学习平台</div>
              </div>
            )}
          </div>
        </div>

        <div className="fed-header-right" style={{
          position: 'absolute',
          right: '24px',
          top: '50%',
          transform: 'translateY(-50%)',
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
          color: 'white',
          zIndex: 1002
        }}>
          <Space size="middle" wrap={false}>
            {/* 用户信息 */}
            <Dropdown
              menu={{ items: userMenuItems, onClick: handleUserMenuClick }}
              placement="bottomRight"
              arrow
            >
              <div className="fed-header-user" style={{ color: 'white' }}>
                <Avatar
                  size="small"
                  icon={<UserOutlined />}
                />
                <Text className="fed-header-username" style={{ color: 'white' }}>
                  {user?.username || 'admin'}
                </Text>
              </div>
            </Dropdown>
          </Space>
        </div>
      </Header>
      
      {/* 主体区域 */}
      <Layout className="fed-layout-body">
        {/* 侧边栏 */}
        <Sider 
          trigger={null} 
          collapsible 
          collapsed={collapsed}
          width={240}
          className="fed-layout-sider"
          theme="dark"
        >
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
          {/* 内容区域 */}
          <Content className="fed-layout-content">
            <div className="fed-content-wrapper">
              {children || <Outlet />}
            </div>
          </Content>
        </Layout>
      </Layout>
    </Layout>
  )
}

export default MainLayout 