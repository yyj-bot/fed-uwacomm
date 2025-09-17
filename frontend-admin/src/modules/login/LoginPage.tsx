/**
 * 登录页面
 * 提供用户登录功能，包括用户名密码登录和记住登录状态
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { Form, Input, Button, Card, Typography, Space, Divider, message } from 'antd'
import { UserOutlined, LockOutlined, EyeInvisibleOutlined, EyeTwoTone } from '@ant-design/icons'
import { useNavigate, useLocation } from 'react-router-dom'

import { useAuth } from '@/store'
import { isTokenValid, clearAllTokens } from '@/utils/auth-helper'
import './LoginPage.module.css'

const { Title, Text, Link } = Typography

interface LoginForm {
  username: string
  password: string
  remember?: boolean
}

const LoginPage: React.FC = () => {
  const [form] = Form.useForm<LoginForm>()
  const navigate = useNavigate()
  const location = useLocation()
  const { login, isLoading, isAuthenticated } = useAuth()
  
  const [loginLoading, setLoginLoading] = useState(false)

  // 获取重定向路径
  const from = (location.state as any)?.from?.pathname || '/dashboard'

  // 如果已登录，重定向到目标页面
  useEffect(() => {
    console.log('📋 LoginPage认证状态检查:', {
      isAuthenticated,
      from,
      localStorage: {
        hasAccessToken: !!localStorage.getItem('access_token'),
        hasRefreshToken: !!localStorage.getItem('refresh_token')
      }
    })
    
    // 使用store的认证状态，确保与App.tsx一致
    if (isAuthenticated) {
      console.log('✅ 检测到已登录状态，重定向到:', from)
      navigate(from, { replace: true })
    }
  }, [isAuthenticated, navigate, from])

  // 处理登录
  const handleLogin = async (values: LoginForm) => {
    console.log('🚀 LoginPage开始处理登录:', values.username)
    setLoginLoading(true)
    
    try {
      console.log('🔐 调用useAuth.login')
      const result = await login({
        loginIdentifier: values.username,
        password: values.password,
        rememberMe: values.remember
      })

      console.log('📊 登录结果:', { success: result.success, error: result.error })

      if (result.success) {
        message.success('登录成功')
        
        // 检查登录后的状态
        const tokenAfterLogin = localStorage.getItem('access_token')
        console.log('🔍 登录成功后localStorage状态:', {
          hasToken: !!tokenAfterLogin,
          storeAuthenticated: isAuthenticated,
          tokenPreview: tokenAfterLogin ? '***' + tokenAfterLogin.slice(-10) : 'null'
        })
        
        // 登录成功后直接跳转，不依赖状态同步
        console.log('🔄 登录成功，直接跳转到:', from)
        navigate(from, { replace: true })
      } else {
        message.error(result.error || '登录失败')
      }
    } catch (error) {
      message.error('登录过程中发生错误')
      console.error('❌ LoginPage登录错误:', error)
    } finally {
      setLoginLoading(false)
    }
  }

  // 处理忘记密码
  const handleForgotPassword = () => {
    message.info('请联系系统管理员重置密码')
  }

  return (
    <div className="fed-login-page">
      <div className="fed-login-background">
        <div className="fed-login-overlay" />
      </div>
      
      <div className="fed-login-container">
        <div className="fed-login-content">
          {/* Logo 和标题 */}
          <div className="fed-login-header">
            <div className="fed-login-logo">
              <div className="fed-logo-icon">F</div>
            </div>
            <Title level={2} className="fed-login-title">
              FedUWAComm
            </Title>
            <Text className="fed-login-subtitle">
              联邦学习水声通信平台
            </Text>
          </div>

          {/* 登录表单 */}
          <Card className="fed-login-card" bordered={false}>
            <div className="fed-login-form-header">
              <Title level={3}>用户登录</Title>
              <Text type="secondary">
                请输入您的账户信息登录系统
              </Text>
            </div>

            <Form
              form={form}
              name="login"
              size="large"
              onFinish={handleLogin}
              autoComplete="off"
              className="fed-login-form"
            >
              <Form.Item
                name="username"
                rules={[
                  { required: true, message: '请输入用户名' },
                  { min: 3, message: '用户名至少3个字符' }
                ]}
              >
                <Input
                  prefix={<UserOutlined className="fed-form-icon" />}
                  placeholder="用户名"
                  allowClear
                />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, message: '密码至少6个字符' }
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined className="fed-form-icon" />}
                  placeholder="密码"
                  iconRender={(visible) => 
                    visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />
                  }
                />
              </Form.Item>

              <Form.Item>
                <div className="fed-login-options">
                  <Form.Item name="remember" valuePropName="checked" noStyle>
                    <input type="checkbox" id="remember" />
                    <label htmlFor="remember" className="fed-checkbox-label">
                      记住登录状态
                    </label>
                  </Form.Item>
                  <Link 
                    className="fed-forgot-link"
                    onClick={handleForgotPassword}
                  >
                    忘记密码？
                  </Link>
                </div>
              </Form.Item>

              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loginLoading || isLoading}
                  block
                  className="fed-login-button"
                >
                  {loginLoading || isLoading ? '登录中...' : '登录'}
                </Button>
              </Form.Item>
            </Form>

            <Divider plain>
              <Text type="secondary">其他登录方式</Text>
            </Divider>

            <div className="fed-login-alternative">
              <Text type="secondary">
                暂不支持其他登录方式
              </Text>
            </div>
          </Card>

          {/* 页脚信息 */}
          <div className="fed-login-footer">
            <Space split={<Divider type="vertical" />}>
              <Link href="#" target="_blank">帮助文档</Link>
              <Link href="#" target="_blank">隐私政策</Link>
              <Link href="#" target="_blank">服务条款</Link>
            </Space>
            <div className="fed-login-copyright">
              <Text type="secondary">
                © 2024 FedUWAComm Team. All rights reserved.
              </Text>
            </div>
          </div>
        </div>

        {/* 右侧信息面板 */}
        <div className="fed-login-info">
          <div className="fed-info-content">
            <Title level={3} style={{ color: 'white', marginBottom: '20px' }}>
              欢迎使用联邦学习平台
            </Title>
            <div className="fed-info-features">
              <div className="fed-info-feature">
                <div className="fed-feature-icon">🌊</div>
                <div className="fed-feature-text">
                  <div className="fed-feature-title">水声通信优化</div>
                  <div className="fed-feature-desc">专业的水下声学信道建模与优化</div>
                </div>
              </div>
              <div className="fed-info-feature">
                <div className="fed-feature-icon">🤖</div>
                <div className="fed-feature-text">
                  <div className="fed-feature-title">联邦学习</div>
                  <div className="fed-feature-desc">分布式机器学习任务管理</div>
                </div>
              </div>
              <div className="fed-info-feature">
                <div className="fed-feature-icon">📊</div>
                <div className="fed-feature-text">
                  <div className="fed-feature-title">实时监控</div>
                  <div className="fed-feature-desc">全面的系统状态监控与分析</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage
