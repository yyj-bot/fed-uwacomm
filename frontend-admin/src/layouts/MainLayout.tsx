import React from 'react'
import { Layout } from 'antd'
import { Outlet } from 'react-router-dom'

const { Header, Sider, Content } = Layout

interface MainLayoutProps {
  children?: React.ReactNode
}

const MainLayout: React.FC<MainLayoutProps> = ({ children }) => {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={200} theme="dark">
        {/* 侧边栏内容 */}
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: 0 }}>
          {/* 头部内容 */}
        </Header>
        <Content style={{ margin: '16px', padding: '24px', background: '#fff' }}>
          {children || <Outlet />}
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout 