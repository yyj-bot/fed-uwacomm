/**
 * 账户设置页面
 * 用户密码修改和安全设置管理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect, useState } from 'react'
import {
  Card,
  Form,
  Input,
  Button,
  Space,
  Typography,
  Divider,
  Alert,
  Row,
  Col,
  message,
  Progress
} from 'antd'
import {
  LockOutlined,
  SafetyOutlined,
  EyeInvisibleOutlined,
  EyeTwoTone,
  CheckCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'

import { useAuth } from '@/store/auth'
import type { ChangePasswordRequest } from '@/services/user'

const { Title, Text } = Typography

interface PasswordFormData {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

// 密码强度检查
interface PasswordStrengthCheck {
  score: number
  level: 'weak' | 'medium' | 'strong' | 'very-strong'
  checks: {
    length: boolean
    lowercase: boolean
    uppercase: boolean
    number: boolean
    special: boolean
  }
}

const AccountSettingsPage: React.FC = () => {
  const {
    user,
    changePassword,
    isChangingPassword,
    isChangePasswordSuccess,
    error,
    clearError
  } = useAuth()

  const [form] = Form.useForm<PasswordFormData>()
  const [passwordStrength, setPasswordStrength] = useState<PasswordStrengthCheck | null>(null)

  // 监听密码修改成功
  useEffect(() => {
    if (isChangePasswordSuccess) {
      message.success('密码修改成功，请使用新密码重新登录')
      form.resetFields()
      setPasswordStrength(null)
    }
  }, [isChangePasswordSuccess, form])

  // 清除错误
  useEffect(() => {
    if (error) {
      message.error(error)
      clearError()
    }
  }, [error, clearError])

  // 检查密码强度
  const checkPasswordStrength = (password: string): PasswordStrengthCheck => {
    const checks = {
      length: password.length >= 8,
      lowercase: /[a-z]/.test(password),
      uppercase: /[A-Z]/.test(password),
      number: /\d/.test(password),
      special: /[!@#$%^&*(),.?":{}|<>]/.test(password)
    }

    const score = Object.values(checks).filter(Boolean).length
    
    let level: PasswordStrengthCheck['level'] = 'weak'
    if (score >= 5) level = 'very-strong'
    else if (score >= 4) level = 'strong'
    else if (score >= 3) level = 'medium'

    return { score, level, checks }
  }

  // 密码输入变化时检查强度
  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const password = e.target.value
    if (password) {
      setPasswordStrength(checkPasswordStrength(password))
    } else {
      setPasswordStrength(null)
    }
  }

  // 提交密码修改
  const handlePasswordSubmit = async () => {
    try {
      const values = await form.validateFields()
      
      const passwordData: ChangePasswordRequest = {
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword
      }

      await changePassword(passwordData)
    } catch (error) {
      console.error('修改密码失败:', error)
    }
  }

  // 获取密码强度颜色
  const getPasswordStrengthColor = (level: string) => {
    switch (level) {
      case 'weak': return '#ff4d4f'
      case 'medium': return '#faad14'
      case 'strong': return '#52c41a'
      case 'very-strong': return '#1890ff'
      default: return '#d9d9d9'
    }
  }

  // 获取密码强度文本
  const getPasswordStrengthText = (level: string) => {
    switch (level) {
      case 'weak': return '弱'
      case 'medium': return '中等'
      case 'strong': return '强'
      case 'very-strong': return '很强'
      default: return ''
    }
  }

  return (
    <div style={{ padding: '24px', maxWidth: '800px', margin: '0 auto' }}>
      {/* 密码修改区域 */}
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <LockOutlined style={{ fontSize: '20px', color: '#1890ff' }} />
            <Title level={4} style={{ margin: 0 }}>修改密码</Title>
          </div>
        }
        style={{ marginBottom: '24px' }}
      >
        <Alert
          message="密码安全提示"
          description="为了账户安全，建议定期更换密码。新密码应包含大小写字母、数字和特殊字符，长度至少6位。"
          type="info"
          showIcon
          style={{ marginBottom: '24px' }}
        />

        <Form
          form={form}
          layout="vertical"
          onFinish={handlePasswordSubmit}
        >
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item
                label="当前密码"
                name="oldPassword"
                rules={[
                  { required: true, message: '请输入当前密码' }
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="请输入当前密码"
                  size="large"
                  iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={24}>
              <Form.Item
                label="新密码"
                name="newPassword"
                rules={[
                  { required: true, message: '请输入新密码' },
                  { min: 6, max: 20, message: '密码长度应在6-20字符之间' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      const oldPassword = getFieldValue('oldPassword')
                      if (value && oldPassword && value === oldPassword) {
                        return Promise.reject(new Error('新密码不能与当前密码相同'))
                      }
                      return Promise.resolve()
                    }
                  })
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="请输入新密码"
                  size="large"
                  onChange={handlePasswordChange}
                  iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
                />
              </Form.Item>

              {/* 密码强度指示器 */}
              {passwordStrength && (
                <div style={{ marginTop: '8px', marginBottom: '16px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                    <Text>密码强度：</Text>
                    <Text style={{ color: getPasswordStrengthColor(passwordStrength.level) }}>
                      {getPasswordStrengthText(passwordStrength.level)}
                    </Text>
                    <Progress
                      percent={(passwordStrength.score / 5) * 100}
                      strokeColor={getPasswordStrengthColor(passwordStrength.level)}
                      showInfo={false}
                      size="small"
                      style={{ flex: 1, maxWidth: '200px' }}
                    />
                  </div>
                  
                  <div style={{ fontSize: '12px', color: '#666' }}>
                    <Row gutter={[8, 4]}>
                      <Col span={12}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          {passwordStrength.checks.length ? 
                            <CheckCircleOutlined style={{ color: '#52c41a' }} /> : 
                            <ExclamationCircleOutlined style={{ color: '#faad14' }} />
                          }
                          <span>至少8个字符</span>
                        </div>
                      </Col>
                      <Col span={12}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          {passwordStrength.checks.lowercase ? 
                            <CheckCircleOutlined style={{ color: '#52c41a' }} /> : 
                            <ExclamationCircleOutlined style={{ color: '#faad14' }} />
                          }
                          <span>包含小写字母</span>
                        </div>
                      </Col>
                      <Col span={12}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          {passwordStrength.checks.uppercase ? 
                            <CheckCircleOutlined style={{ color: '#52c41a' }} /> : 
                            <ExclamationCircleOutlined style={{ color: '#faad14' }} />
                          }
                          <span>包含大写字母</span>
                        </div>
                      </Col>
                      <Col span={12}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          {passwordStrength.checks.number ? 
                            <CheckCircleOutlined style={{ color: '#52c41a' }} /> : 
                            <ExclamationCircleOutlined style={{ color: '#faad14' }} />
                          }
                          <span>包含数字</span>
                        </div>
                      </Col>
                      <Col span={24}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          {passwordStrength.checks.special ? 
                            <CheckCircleOutlined style={{ color: '#52c41a' }} /> : 
                            <ExclamationCircleOutlined style={{ color: '#faad14' }} />
                          }
                          <span>包含特殊字符 (!@#$%^&*等)</span>
                        </div>
                      </Col>
                    </Row>
                  </div>
                </div>
              )}
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={24}>
              <Form.Item
                label="确认新密码"
                name="confirmPassword"
                dependencies={['newPassword']}
                rules={[
                  { required: true, message: '请确认新密码' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('newPassword') === value) {
                        return Promise.resolve()
                      }
                      return Promise.reject(new Error('两次输入的密码不一致'))
                    }
                  })
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="请再次输入新密码"
                  size="large"
                  iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row>
            <Col span={24}>
              <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
                <Button
                  onClick={() => {
                    form.resetFields()
                    setPasswordStrength(null)
                  }}
                >
                  重置
                </Button>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={isChangingPassword}
                  icon={<SafetyOutlined />}
                >
                  修改密码
                </Button>
              </Space>
            </Col>
          </Row>
        </Form>
      </Card>

      {/* 安全提示 */}
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <SafetyOutlined style={{ fontSize: '20px', color: '#52c41a' }} />
            <Title level={4} style={{ margin: 0 }}>安全建议</Title>
          </div>
        }
      >
        <div style={{ lineHeight: '1.8' }}>
          <div style={{ marginBottom: '12px' }}>
            <Text strong>密码安全要求：</Text>
          </div>
          <ul style={{ paddingLeft: '20px', margin: '0 0 16px 0' }}>
            <li>密码长度至少6个字符，建议8个字符以上</li>
            <li>包含大小写字母、数字和特殊字符的组合</li>
            <li>避免使用个人信息（生日、姓名等）作为密码</li>
            <li>不要与最近使用过的密码相同</li>
            <li>定期更换密码，建议3-6个月更换一次</li>
          </ul>
          
          <div style={{ marginBottom: '12px' }}>
            <Text strong>其他安全建议：</Text>
          </div>
          <ul style={{ paddingLeft: '20px', margin: 0 }}>
            <li>不要在公共场所或不安全的网络环境下输入密码</li>
            <li>不要将密码告诉他人或写在容易被发现的地方</li>
            <li>发现账户异常活动时请立即联系管理员</li>
            <li>退出系统时请确保完全登出</li>
          </ul>
        </div>
      </Card>
    </div>
  )
}

export default AccountSettingsPage








