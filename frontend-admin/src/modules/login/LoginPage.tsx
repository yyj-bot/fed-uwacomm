/**
 * 登录页面
 * 提供用户登录功能，包括用户名密码登录和记住登录状态
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { Form, Input, Button, Card, Typography, message, Checkbox, Modal } from 'antd'
import { UserOutlined, LockOutlined, EyeInvisibleOutlined, EyeTwoTone, RocketOutlined, ThunderboltOutlined, MailOutlined } from '@ant-design/icons'
import { useNavigate, useLocation } from 'react-router-dom'

import { useAuth } from '@/store'
import { isTokenValid, clearAllTokens } from '@/utils/auth-helper'
import './LoginPage.css'

const { Title, Text } = Typography

interface LoginForm {
  username: string
  password: string
  remember?: boolean
}

interface RegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
}

const LoginPage: React.FC = () => {
  const [form] = Form.useForm<LoginForm>()
  const [registerForm] = Form.useForm<RegisterForm>()
  const navigate = useNavigate()
  const location = useLocation()
  const { login, register, isLoading, isAuthenticated, isRegistering } = useAuth()
  
  const [loginLoading, setLoginLoading] = useState(false)
  const [isRegisterModalVisible, setIsRegisterModalVisible] = useState(false)

  // 获取重定向路径
  const from = (location.state as any)?.from?.pathname || '/dashboard'

  // 设置body样式，防止滚动条
  useEffect(() => {
    document.body.style.overflow = 'hidden'
    document.body.style.height = '100vh'
    
    return () => {
      document.body.style.overflow = 'auto'
      document.body.style.height = 'auto'
    }
  }, [])

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

  // 处理注册
  const handleRegister = async (values: RegisterForm) => {
    console.log('🚀 开始用户注册:', values.username)
    
    try {
      const result = await register({
        username: values.username,
        email: values.email,
        password: values.password,
        confirmPassword: values.confirmPassword
      })

      if (result.success) {
        message.success('注册成功！请使用新账户登录')
        setIsRegisterModalVisible(false)
        registerForm.resetFields()
      } else {
        message.error(result.error || '注册失败')
      }
    } catch (error) {
      message.error('注册过程中发生错误')
      console.error('❌ 注册错误:', error)
    }
  }

  // 打开注册模态框
  const showRegisterModal = () => {
    setIsRegisterModalVisible(true)
  }

  // 关闭注册模态框
  const handleRegisterCancel = () => {
    setIsRegisterModalVisible(false)
    registerForm.resetFields()
  }

  return (
    <div 
      className="fed-login-page"
      style={{
        height: '100vh',
        background: 'linear-gradient(135deg, #1e3c72 0%, #2a5298 50%, #667eea 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '16px',
        overflow: 'hidden'
      }}
    >
      <div 
        className="fed-login-container"
        style={{
          display: 'flex',
          width: '100%',
          maxWidth: '1200px',
          height: 'calc(100vh - 32px)',
          minHeight: '600px',
          background: 'white',
          borderRadius: '16px',
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
          overflow: 'hidden'
        }}
      >
        <div 
          className="fed-login-content"
          style={{
            flex: 1,
            padding: '24px',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            overflow: 'auto'
          }}
        >
          {/* Logo 和标题 */}
          <div className="fed-login-header" style={{ textAlign: 'center', marginBottom: '20px' }}>
            <div className="fed-login-logo" style={{ display: 'flex', justifyContent: 'center', marginBottom: '16px' }}>
              <div 
                className="fed-logo-icon"
                style={{
                  width: '64px',
                  height: '64px',
                  background: 'linear-gradient(135deg, #1890ff, #722ed1)',
                  borderRadius: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  fontWeight: 'bold',
                  fontSize: '28px',
                  boxShadow: '0 8px 32px rgba(24, 144, 255, 0.3)'
                }}
              >
                F
              </div>
            </div>
            <Title 
              level={2} 
              className="fed-login-title"
              style={{
                background: 'linear-gradient(135deg, #1890ff, #722ed1)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                marginBottom: '8px',
                fontWeight: '700'
              }}
            >
              FedUWAComm
            </Title>
            <Text className="fed-login-subtitle" style={{ color: '#00000073', fontSize: '16px', marginBottom: '12px' }}>
              水下联邦调度平台
            </Text>
            <div 
              className="fed-login-slogan"
              style={{
                marginTop: '12px',
                padding: '8px 16px',
                background: 'rgba(24, 144, 255, 0.1)',
                borderRadius: '20px',
                display: 'inline-block'
              }}
            >
              <Text type="secondary">安全 · 高效 · 智能</Text>
            </div>
          </div>

          {/* 登录表单 */}
          <Card 
            className="fed-login-card" 
            variant="borderless"
            style={{
              borderRadius: '16px',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
              marginBottom: '16px',
              border: '1px solid rgba(0, 0, 0, 0.06)'
            }}
            bodyStyle={{
              padding: '20px',
              background: 'linear-gradient(180deg, #ffffff 0%, #fafbff 100%)'
            }}
          >
            <div 
              className="fed-login-form-header"
              style={{
                textAlign: 'center',
                marginBottom: '16px',
                position: 'relative'
              }}
            >
              <Title 
                level={3} 
                className="fed-form-title"
                style={{ color: '#1a1a1a', fontWeight: '600', marginBottom: '8px' }}
              >
                用户登录
              </Title>
              <Text type="secondary" className="fed-form-subtitle" style={{ fontSize: '15px' }}>
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
                style={{ marginBottom: '16px' }}
              >
                <Input
                  prefix={<UserOutlined style={{ color: '#1890ff', fontSize: '16px' }} />}
                  placeholder="用户名或邮箱"
                  allowClear
                  style={{
                    height: '48px',
                    borderRadius: '8px',
                    border: '1px solid #d9d9d9'
                  }}
                />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, message: '密码至少6个字符' }
                ]}
                style={{ marginBottom: '16px' }}
              >
                <Input.Password
                  prefix={<LockOutlined style={{ color: '#1890ff', fontSize: '16px' }} />}
                  placeholder="请输入密码"
                  iconRender={(visible) => 
                    visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />
                  }
                  style={{
                    height: '48px',
                    borderRadius: '8px',
                    border: '1px solid #d9d9d9'
                  }}
                />
              </Form.Item>

              <Form.Item style={{ marginBottom: '16px' }}>
                <div className="fed-login-options">
                  <Form.Item name="remember" valuePropName="checked" noStyle>
                    <Checkbox className="fed-checkbox-label">
                      记住登录状态
                    </Checkbox>
                  </Form.Item>
                  <Text 
                    type="secondary"
                    style={{ cursor: 'pointer', color: '#1890ff' }}
                    onClick={handleForgotPassword}
                  >
                    忘记密码？
                  </Text>
                </div>
              </Form.Item>

              <Form.Item style={{ marginBottom: '0' }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loginLoading || isLoading}
                  block
                  style={{
                    height: '52px',
                    fontSize: '16px',
                    fontWeight: '600',
                    borderRadius: '8px',
                    background: 'linear-gradient(135deg, #1890ff, #722ed1)',
                    border: 'none',
                    boxShadow: '0 4px 16px rgba(24, 144, 255, 0.3)'
                  }}
                >
                  {loginLoading || isLoading ? '登录中...' : '登录'}
                </Button>
              </Form.Item>

              <Form.Item style={{ marginBottom: '0', marginTop: '12px' }}>
                <Button
                  type="default"
                  block
                  onClick={showRegisterModal}
                  style={{
                    height: '44px',
                    fontSize: '14px',
                    borderRadius: '8px',
                    borderColor: '#1890ff',
                    color: '#1890ff'
                  }}
                >
                  还没有账户？立即注册
                </Button>
              </Form.Item>
            </Form>
          </Card>

          {/* 页脚信息 */}
          <div className="fed-login-footer">
            <div className="fed-login-copyright">
              <Text type="secondary">
                © 2024 FedUWAComm Team. All rights reserved.
              </Text>
            </div>
          </div>
        </div>

        {/* 右侧信息面板 */}
        <div 
          className="fed-login-info"
          style={{
            flex: 1,
            background: 'linear-gradient(135deg, rgba(30, 60, 114, 0.95), rgba(42, 82, 152, 0.9))',
            padding: '32px 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            position: 'relative',
            overflow: 'hidden'
          }}
        >
          <div 
            className="fed-info-content"
            style={{
              position: 'relative',
              zIndex: 1,
              textAlign: 'center',
              maxWidth: '400px'
            }}
          >
            <Title level={3} style={{ color: 'white', marginBottom: '20px' }} className="fed-welcome-title">
              欢迎使用水下联邦调度平台
            </Title>
            <Text style={{ color: 'rgba(255, 255, 255, 0.9)', fontSize: '16px', marginBottom: '40px', display: 'block' }}>
              打造下一代智能水声通信系统
            </Text>
            <div className="fed-info-features">
              <div className="fed-info-feature">
                <div className="fed-feature-icon">🌊</div>
                <div className="fed-feature-text">
                  <div className="fed-feature-title">水下机器人控制</div>
                  <div className="fed-feature-desc">专业的水下机器人控制平台</div>
                </div>
              </div>
              <div className="fed-info-feature">
                <div className="fed-feature-icon">
                  <RocketOutlined style={{ color: '#52c41a' }} />
                </div>
                <div className="fed-feature-text">
                  <div className="fed-feature-title">联邦学习</div>
                  <div className="fed-feature-desc">分布式机器学习任务智能管理</div>
                </div>
              </div>
              <div className="fed-info-feature">
                <div className="fed-feature-icon">
                  <ThunderboltOutlined style={{ color: '#faad14' }} />
                </div>
                <div className="fed-feature-text">
                  <div className="fed-feature-title">实时监控</div>
                  <div className="fed-feature-desc">全面的系统状态监控与性能分析</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 注册模态框 */}
      <Modal
        title={
          <div style={{ textAlign: 'center', paddingBottom: '16px' }}>
            <Title level={4} style={{ margin: 0, color: '#1890ff' }}>
              用户注册
            </Title>
            <Text type="secondary" style={{ fontSize: '14px' }}>
              创建您的新账户
            </Text>
          </div>
        }
        open={isRegisterModalVisible}
        onCancel={handleRegisterCancel}
        footer={null}
        width={480}
        centered
        styles={{
          body: { padding: '20px 24px' }
        }}
      >
        <Form
          form={registerForm}
          name="register"
          size="large"
          onFinish={handleRegister}
          autoComplete="off"
          layout="vertical"
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, message: '用户名至少3个字符' },
              { max: 50, message: '用户名最多50个字符' },
              { pattern: /^[a-zA-Z0-9_-]+$/, message: '用户名只能包含字母、数字、下划线和连字符' }
            ]}
          >
            <Input
              prefix={<UserOutlined style={{ color: '#1890ff' }} />}
              placeholder="请输入用户名"
              allowClear
            />
          </Form.Item>

          <Form.Item
            label="邮箱地址"
            name="email"
            rules={[
              { required: true, message: '请输入邮箱地址' },
              { type: 'email', message: '请输入有效的邮箱地址' }
            ]}
          >
            <Input
              prefix={<MailOutlined style={{ color: '#1890ff' }} />}
              placeholder="请输入邮箱地址"
              allowClear
            />
          </Form.Item>

          <Form.Item
            label="密码"
            name="password"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 6, message: '密码至少6个字符' },
              { max: 20, message: '密码最多20个字符' }
            ]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#1890ff' }} />}
              placeholder="请输入密码"
              iconRender={(visible) => 
                visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />
              }
            />
          </Form.Item>

          <Form.Item
            label="确认密码"
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请确认密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#1890ff' }} />}
              placeholder="请再次输入密码"
              iconRender={(visible) => 
                visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />
              }
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: '16px', marginTop: '24px' }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={isRegistering}
              block
              style={{
                height: '48px',
                fontSize: '16px',
                fontWeight: '600',
                borderRadius: '8px',
                background: 'linear-gradient(135deg, #1890ff, #722ed1)',
                border: 'none',
                boxShadow: '0 4px 16px rgba(24, 144, 255, 0.3)'
              }}
            >
              {isRegistering ? '注册中...' : '立即注册'}
            </Button>
          </Form.Item>

          <Form.Item style={{ marginBottom: '0', textAlign: 'center' }}>
            <Button
              type="text"
              onClick={handleRegisterCancel}
              style={{ color: '#666' }}
            >
              返回登录
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default LoginPage

