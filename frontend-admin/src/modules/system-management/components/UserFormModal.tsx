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
  Spin,
  Divider 
} from 'antd'
import { UserOutlined, MailOutlined, LockOutlined, SafetyOutlined, CheckCircleOutlined, EditOutlined } from '@ant-design/icons'
import { useAdmin } from '@/store'
import { getFriendlyErrorMessage } from '@/utils'
import type { User } from '@/types'
import './SystemManagementModals.css'

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
  const { 
    createUser, 
    updateUser, 
    createUserLoading, 
    operationLoading,
    createUserError 
  } = useAdmin()

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
        // 创建模式下，让initialValue生效
      }
    }
  }, [visible, mode, editUser, form])

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      
      if (mode === 'create') {
        const createData = {
          username: values.username,
          email: values.email,
          role: values.role,
          status: values.status ? 'ACTIVE' as const : 'INACTIVE' as const,
          password: values.password
        }
        
        const result = await createUser(createData)
        
        if (result.success) {
          message.success('用户创建成功')
          form.resetFields()
          onCancel()
          onSuccess?.()
        } else {
          // 错误处理在下面的catch块中统一处理
          throw new Error(result.error || '创建用户失败')
        }
      } else if (editUser) {
        // 编辑模式下，如果提供了新密码才传递
        const updateData: any = {
          username: values.username,
          email: values.email,
          role: values.role,
          status: values.status ? 'ACTIVE' as const : 'INACTIVE' as const,
          ...(values.password ? { password: values.password } : {})
        }
        const result = await updateUser(editUser.userId, updateData)
        
        if (result.success) {
          message.success('用户更新成功')
          form.resetFields()
          onCancel()
          onSuccess?.()
        } else {
          // 错误处理在下面的catch块中统一处理
          throw new Error(result.error || '更新用户失败')
        }
      }
    } catch (error) {
      console.error('用户操作失败:', error)
      const errorMessage = getFriendlyErrorMessage(error, mode === 'create' ? 'user' : 'user')
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
            {mode === 'create' ? <UserOutlined style={{ color: '#1890ff' }} /> : <EditOutlined style={{ color: '#1890ff' }} />}
            <span style={{ fontSize: '18px', fontWeight: 600 }}>
              {mode === 'create' ? '添加新用户' : '编辑用户信息'}
            </span>
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
          loading={isLoading}
          onClick={handleOk}
          size="large"
          icon={mode === 'create' ? <CheckCircleOutlined /> : <CheckCircleOutlined />}
        >
          {mode === 'create' ? '创建用户' : '保存修改'}
        </Button>
      ]}
      destroyOnHidden
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
              prefix={<UserOutlined style={{ color: '#1890ff' }} />}
              placeholder="请输入用户名" 
              autoComplete="off"
              size="large"
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
              autoComplete="off"
              size="large"
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
              prefix={<LockOutlined style={{ color: '#1890ff' }} />}
              placeholder={mode === 'create' ? '请输入密码（至少6位）' : '留空则不修改密码'} 
              autoComplete="new-password"
              size="large"
            />
          </Form.Item>

          <Form.Item
            label="角色"
            name="role"
            rules={[{ required: true, message: '请选择用户角色' }]}
          >
            <Select placeholder="请选择用户角色" size="large">
              <Option value="ADMIN">
                <Space>
                  <SafetyOutlined style={{ color: '#ff4d4f' }} />
                  管理员 - 拥有所有权限
                </Space>
              </Option>
              <Option value="RESEARCHER">
                <Space>
                  <SafetyOutlined style={{ color: '#1890ff' }} />
                  研究员 - 读写执行权限
                </Space>
              </Option>
              <Option value="OPERATOR">
                <Space>
                  <SafetyOutlined style={{ color: '#52c41a' }} />
                  操作员 - 读取执行权限
                </Space>
              </Option>
              <Option value="VIEWER">
                <Space>
                  <SafetyOutlined style={{ color: '#d9d9d9' }} />
                  观察者 - 仅读取权限
                </Space>
              </Option>
            </Select>
          </Form.Item>

          <Form.Item
            label="账户状态"
            name="status"
            valuePropName="checked"
            initialValue={true}
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

