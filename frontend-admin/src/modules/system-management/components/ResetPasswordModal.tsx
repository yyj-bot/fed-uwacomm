/**
 * 重置密码模态框
 */

import React from 'react'
import { 
  Modal, 
  Form, 
  Input, 
  Button, 
  message,
  Typography,
  Space,
  Alert,
  Divider
} from 'antd'
import { LockOutlined, UserOutlined, SafetyCertificateOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { useAdmin } from '@/store'
import type { User } from '@/types'
import './SystemManagementModals.css'

const { Password } = Input
const { Text } = Typography

interface ResetPasswordModalProps {
  visible: boolean
  onCancel: () => void
  onSuccess?: () => void
  user?: User | null
}

const ResetPasswordModal: React.FC<ResetPasswordModalProps> = ({
  visible,
  onCancel,
  onSuccess,
  user
}) => {
  const [form] = Form.useForm()
  const { resetUserPassword, operationLoading } = useAdmin()

  const isLoading = operationLoading[`reset-password-${user?.userId}`] || false

  const handleOk = async () => {
    if (!user) return

    try {
      const values = await form.validateFields()
      
      const result = await resetUserPassword(user.userId, {
        newPassword: values.newPassword
      })
      
      if (result.success) {
        message.success('密码重置成功')
        form.resetFields()
        onCancel()
        onSuccess?.()
      } else {
        throw new Error(result.error || '重置密码失败')
      }
    } catch (error) {
      console.error('重置密码失败:', error)
      
      let errorMessage = '重置密码失败'
      if (error instanceof Error) {
        errorMessage = error.message
      } else if (typeof error === 'string') {
        errorMessage = error
      } else if (error && typeof error === 'object') {
        const apiError = error as any
        if (apiError.response?.data?.message) {
          errorMessage = apiError.response.data.message
        } else if (apiError.message) {
          errorMessage = apiError.message
        }
      }
      
      // 提供友好的错误提示
      if (errorMessage.includes('permission') || errorMessage.includes('权限')) {
        errorMessage = '您没有重置该用户密码的权限'
      } else if (errorMessage.includes('password policy') || errorMessage.includes('密码策略')) {
        errorMessage = '新密码不符合安全策略要求'
      } else if (errorMessage.includes('same password') || errorMessage.includes('相同密码')) {
        errorMessage = '新密码不能与当前密码相同'
      } else if (errorMessage.includes('not found') || errorMessage.includes('不存在')) {
        errorMessage = '用户不存在'
      } else if (errorMessage.includes('locked') || errorMessage.includes('锁定')) {
        errorMessage = '用户已被锁定，无法重置密码'
      }
      
      message.error(errorMessage)
    }
  }

  const handleCancel = () => {
    form.resetFields()
    onCancel()
  }

  return (
    <Modal
      title={
        <Space>
          <SafetyCertificateOutlined style={{ color: '#faad14', fontSize: '20px' }} />
          <span style={{ fontSize: '18px', fontWeight: 600 }}>重置用户密码</span>
        </Space>
      }
      open={visible}
      onCancel={handleCancel}
      centered
      footer={[
        <Button key="cancel" onClick={handleCancel} size="large">
          取消
        </Button>,
        <Button 
          key="submit" 
          type="primary" 
          danger
          loading={isLoading}
          onClick={handleOk}
          size="large"
          icon={<CheckCircleOutlined />}
          style={{
            background: 'linear-gradient(135deg, #faad14 0%, #d48806 100%)',
            border: 'none',
            boxShadow: '0 2px 8px rgba(250, 173, 20, 0.4)'
          }}
        >
          确认重置
        </Button>
      ]}
      destroyOnHidden
      width={540}
    >
      {user && (
        <div style={{ marginBottom: 24 }}>
          <Alert
            message={
              <span style={{ fontSize: '15px', fontWeight: 600 }}>
                ⚠️ 安全操作提示
              </span>
            }
            description={
              <div style={{ fontSize: '14px', lineHeight: '24px' }}>
                <Text>您将要为用户 </Text>
                <Text strong style={{ color: '#1890ff', fontSize: '15px' }}>
                  <UserOutlined /> {user.username}
                </Text>
                <Text> ({user.email}) 重置密码。</Text>
                <br />
                <br />
                <Text type="warning" strong>
                  ⚠️ 重置后，用户需要使用新密码重新登录，原密码将立即失效。
                </Text>
              </div>
            }
            type="warning"
            showIcon
            style={{
              borderRadius: '12px',
              border: '2px solid #ffe7ba',
              background: 'linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%)'
            }}
          />
        </div>
      )}

      <Form
        form={form}
        layout="vertical"
        requiredMark={false}
      >
        <Form.Item
          label="新密码"
          name="newPassword"
          rules={[
            { required: true, message: '请输入新密码' },
            { min: 6, message: '密码至少6个字符' },
            { max: 50, message: '密码不能超过50个字符' }
          ]}
          extra="建议使用字母、数字和特殊字符的组合，提高安全性"
        >
          <Password 
            prefix={<LockOutlined style={{ color: '#1890ff' }} />}
            placeholder="请输入新密码（至少6位）"
            autoComplete="new-password"
            size="large"
          />
        </Form.Item>

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
          <Password 
            prefix={<SafetyCertificateOutlined style={{ color: '#1890ff' }} />}
            placeholder="请再次输入新密码"
            autoComplete="new-password"
            size="large"
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default ResetPasswordModal

