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
  Alert
} from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { useAdmin } from '@/store'
import type { User } from '@/types'

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
      
      await resetUserPassword(user.userId, {
        newPassword: values.newPassword
      })
      
      message.success('密码重置成功')
      form.resetFields()
      onCancel()
      onSuccess?.()
    } catch (error) {
      console.error('重置密码失败:', error)
      // 错误消息已在store中处理
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
          <LockOutlined />
          重置用户密码
        </Space>
      }
      open={visible}
      onCancel={handleCancel}
      footer={[
        <Button key="cancel" onClick={handleCancel}>
          取消
        </Button>,
        <Button 
          key="submit" 
          type="primary" 
          danger
          loading={isLoading}
          onClick={handleOk}
        >
          重置密码
        </Button>
      ]}
      destroyOnClose
      width={500}
    >
      {user && (
        <div style={{ marginBottom: 16 }}>
          <Alert
            message="重置密码操作"
            description={
              <div>
                <Text>您将要为用户 </Text>
                <Text strong>
                  <UserOutlined /> {user.username} ({user.email})
                </Text>
                <Text> 重置密码。</Text>
                <br />
                <Text type="warning">重置后，用户需要使用新密码重新登录。</Text>
              </div>
            }
            type="warning"
            showIcon
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
        >
          <Password 
            prefix={<LockOutlined />}
            placeholder="请输入新密码"
            autoComplete="new-password"
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
            prefix={<LockOutlined />}
            placeholder="请确认新密码"
            autoComplete="new-password"
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default ResetPasswordModal

