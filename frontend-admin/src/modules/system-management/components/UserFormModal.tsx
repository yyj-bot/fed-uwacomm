/**
 * 用户表单模态框
 * 用于添加和编辑用户
 */

import React, { useEffect } from 'react'
import { 
  Modal, 
  Form, 
  Input, 
  Select, 
  Switch, 
  Button, 
  Space, 
  message,
  Spin 
} from 'antd'
import { UserOutlined, MailOutlined, LockOutlined } from '@ant-design/icons'
import { useAdmin } from '@/store'
import type { User } from '@/types'

const { Option } = Select
const { Password } = Input

interface UserFormModalProps {
  visible: boolean
  onCancel: () => void
  onSuccess?: () => void
  editUser?: User | null // 编辑用户时传入用户数据
  mode: 'create' | 'edit'
}

const UserFormModal: React.FC<UserFormModalProps> = ({
  visible,
  onCancel,
  onSuccess,
  editUser,
  mode
}) => {
  const [form] = Form.useForm()
  const { createUser, updateUser, createUserLoading, operationLoading } = useAdmin()

  const isLoading = createUserLoading || operationLoading[`update-${editUser?.userId}`] || false

  // 当模态框打开时，设置表单数据
  useEffect(() => {
    if (visible) {
      if (mode === 'edit' && editUser) {
        form.setFieldsValue({
          username: editUser.username,
          email: editUser.email,
          role: editUser.role,
          status: editUser.status === 'ACTIVE'
        })
      } else {
        form.resetFields()
        form.setFieldsValue({
          status: true // 默认激活状态
        })
      }
    }
  }, [visible, mode, editUser, form])

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      
      const userData = {
        username: values.username,
        email: values.email,
        role: values.role,
        status: values.status ? 'ACTIVE' : 'INACTIVE',
        ...(mode === 'create' ? { password: values.password } : {})
      }

      if (mode === 'create') {
        await createUser(userData)
        message.success('用户创建成功')
      } else if (editUser) {
        // 编辑模式下，如果提供了新密码才传递
        const updateData = {
          ...userData,
          ...(values.password ? { password: values.password } : {})
        }
        await updateUser(editUser.userId, updateData)
        message.success('用户更新成功')
      }

      form.resetFields()
      onCancel()
      onSuccess?.()
    } catch (error) {
      console.error('用户操作失败:', error)
      // 错误消息已在store中处理，这里不需要额外显示
    }
  }

  const handleCancel = () => {
    form.resetFields()
    onCancel()
  }

  return (
    <Modal
      title={mode === 'create' ? '添加用户' : '编辑用户'}
      open={visible}
      onCancel={handleCancel}
      footer={[
        <Button key="cancel" onClick={handleCancel}>
          取消
        </Button>,
        <Button 
          key="submit" 
          type="primary" 
          loading={isLoading}
          onClick={handleOk}
        >
          {mode === 'create' ? '创建' : '更新'}
        </Button>
      ]}
      destroyOnClose
      width={600}
    >
      <Spin spinning={isLoading}>
        <Form
          form={form}
          layout="vertical"
          requiredMark={false}
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, message: '用户名至少3个字符' },
              { max: 50, message: '用户名不能超过50个字符' },
              { pattern: /^[a-zA-Z0-9_-]+$/, message: '用户名只能包含字母、数字、下划线和连字符' }
            ]}
          >
            <Input 
              prefix={<UserOutlined />}
              placeholder="请输入用户名" 
              autoComplete="off"
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
              prefix={<MailOutlined />}
              placeholder="请输入邮箱地址" 
              autoComplete="off"
            />
          </Form.Item>

          <Form.Item
            label={mode === 'create' ? '密码' : '新密码'}
            name="password"
            rules={mode === 'create' ? [
              { required: true, message: '请输入密码' },
              { min: 6, message: '密码至少6个字符' }
            ] : [
              { min: 6, message: '密码至少6个字符' }
            ]}
            extra={mode === 'edit' ? '留空则不修改密码' : undefined}
          >
            <Password 
              prefix={<LockOutlined />}
              placeholder={mode === 'create' ? '请输入密码' : '留空则不修改密码'} 
              autoComplete="new-password"
            />
          </Form.Item>

          <Form.Item
            label="角色"
            name="role"
            rules={[{ required: true, message: '请选择用户角色' }]}
          >
            <Select placeholder="请选择用户角色">
              <Option value="ADMIN">管理员</Option>
              <Option value="RESEARCHER">研究员</Option>
              <Option value="OPERATOR">操作员</Option>
              <Option value="VIEWER">观察者</Option>
            </Select>
          </Form.Item>

          <Form.Item
            label="账户状态"
            name="status"
            valuePropName="checked"
          >
            <Switch 
              checkedChildren="激活" 
              unCheckedChildren="禁用"
            />
          </Form.Item>
        </Form>
      </Spin>
    </Modal>
  )
}

export default UserFormModal

