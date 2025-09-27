/**
 * 用户个人资料页面
 * 显示和编辑当前用户的基本信息
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
  Tag,
  Alert,
  Row,
  Col,
  Avatar,
  message,
  Spin
} from 'antd'
import {
  UserOutlined,
  MailOutlined,
  EditOutlined,
  SaveOutlined,
  CloseOutlined,
  EyeOutlined
} from '@ant-design/icons'

import { useAuth } from '@/store/auth'
import type { UpdateProfileRequest } from '@/services/user'

const { Title, Text } = Typography
const { TextArea } = Input

interface ProfileFormData {
  username: string
  email: string
}

const UserProfilePage: React.FC = () => {
  const {
    user,
    isLoading,
    error,
    fetchUserProfile,
    updateProfile,
    isUpdatingProfile,
    isUpdateProfileSuccess,
    clearError
  } = useAuth()

  const [form] = Form.useForm<ProfileFormData>()
  const [isEditing, setIsEditing] = useState(false)

  // 组件挂载时获取用户信息
  useEffect(() => {
    fetchUserProfile()
  }, [fetchUserProfile])

  // 监听更新成功
  useEffect(() => {
    if (isUpdateProfileSuccess) {
      message.success('个人信息更新成功')
      setIsEditing(false)
      // 重新获取用户信息
      fetchUserProfile()
    }
  }, [isUpdateProfileSuccess, fetchUserProfile])

  // 清除错误
  useEffect(() => {
    if (error) {
      message.error(error)
      clearError()
    }
  }, [error, clearError])

  // 当用户信息加载完成时，设置表单值
  useEffect(() => {
    if (user) {
      form.setFieldsValue({
        username: user.username,
        email: user.email
      })
    }
  }, [user, form])

  // 开始编辑
  const handleEdit = () => {
    setIsEditing(true)
  }

  // 取消编辑
  const handleCancel = () => {
    setIsEditing(false)
    // 重置表单值
    if (user) {
      form.setFieldsValue({
        username: user.username,
        email: user.email
      })
    }
  }

  // 保存修改
  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      let updateData: UpdateProfileRequest = {}
      
      // 只提交有变化的字段
      if (values.username !== user?.username) {
        updateData = { ...updateData, username: values.username }
      }
      if (values.email !== user?.email) {
        updateData = { ...updateData, email: values.email }
      }

      // 如果没有任何变化，直接取消编辑
      if (Object.keys(updateData).length === 0) {
        message.info('没有任何修改')
        setIsEditing(false)
        return
      }

      await updateProfile(updateData)
    } catch (error) {
      console.error('保存失败:', error)
    }
  }

  // 获取角色显示名称和颜色
  const getRoleDisplay = (role: string) => {
    const roleMap = {
      'ADMIN': { name: '系统管理员', color: 'red' },
      'RESEARCHER': { name: '研究人员', color: 'blue' },
      'OPERATOR': { name: '操作员', color: 'green' },
      'VIEWER': { name: '查看者', color: 'default' }
    }
    return roleMap[role as keyof typeof roleMap] || { name: role, color: 'default' }
  }

  // 获取状态显示名称和颜色
  const getStatusDisplay = (status: string) => {
    const statusMap = {
      'ACTIVE': { name: '活跃', color: 'success' },
      'INACTIVE': { name: '非活跃', color: 'warning' },
      'LOCKED': { name: '锁定', color: 'error' },
      'DELETED': { name: '已删除', color: 'default' }
    }
    return statusMap[status as keyof typeof statusMap] || { name: status, color: 'default' }
  }

  // 格式化时间显示
  const formatDateTime = (dateString?: string) => {
    if (!dateString) return '未知'
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN')
  }

  if (isLoading && !user) {
    return (
      <div style={{ padding: '20px', textAlign: 'center' }}>
        <Spin size="large" />
        <div style={{ marginTop: '16px' }}>
          <Text type="secondary">正在加载用户信息...</Text>
        </div>
      </div>
    )
  }

  if (!user) {
    return (
      <div style={{ padding: '20px' }}>
        <Alert
          message="用户信息加载失败"
          description="无法获取用户信息，请刷新页面重试"
          type="error"
          showIcon
          action={
            <Button size="small" onClick={() => fetchUserProfile()}>
              重新加载
            </Button>
          }
        />
      </div>
    )
  }

  const roleDisplay = getRoleDisplay(user.role)
  const statusDisplay = getStatusDisplay(user.status)

  return (
    <div style={{ padding: '24px', maxWidth: '800px', margin: '0 auto' }}>
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <UserOutlined style={{ fontSize: '20px', color: '#1890ff' }} />
            <Title level={4} style={{ margin: 0 }}>个人资料</Title>
          </div>
        }
        extra={
          !isEditing ? (
            <Button
              type="primary"
              icon={<EditOutlined />}
              onClick={handleEdit}
            >
              编辑资料
            </Button>
          ) : (
            <Space>
              <Button
                icon={<CloseOutlined />}
                onClick={handleCancel}
              >
                取消
              </Button>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                loading={isUpdatingProfile}
                onClick={handleSave}
              >
                保存
              </Button>
            </Space>
          )
        }
      >
        <Row gutter={[24, 24]}>
          {/* 左侧头像区域 */}
          <Col xs={24} sm={8} md={6}>
            <div style={{ textAlign: 'center' }}>
              <Avatar
                size={120}
                icon={<UserOutlined />}
                style={{
                  backgroundColor: '#1890ff',
                  marginBottom: '16px'
                }}
              />
              <div>
                <Title level={5} style={{ margin: '8px 0 4px' }}>
                  {user.username}
                </Title>
                <Text type="secondary">{user.email}</Text>
              </div>
            </div>
          </Col>

          {/* 右侧信息区域 */}
          <Col xs={24} sm={16} md={18}>
            <Form
              form={form}
              layout="vertical"
              disabled={!isEditing}
            >
              {/* 基本信息 */}
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="用户名"
                    name="username"
                    rules={[
                      { required: true, message: '请输入用户名' },
                      { min: 3, max: 50, message: '用户名长度应在3-50字符之间' }
                    ]}
                  >
                    <Input
                      prefix={<UserOutlined />}
                      placeholder="请输入用户名"
                    />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="邮箱地址"
                    name="email"
                    rules={[
                      { required: true, message: '请输入邮箱地址' },
                      { type: 'email', message: '邮箱格式不正确' }
                    ]}
                  >
                    <Input
                      prefix={<MailOutlined />}
                      placeholder="请输入邮箱地址"
                    />
                  </Form.Item>
                </Col>
              </Row>

              <Divider>系统信息</Divider>

              {/* 系统信息（只读） */}
              <Row gutter={16}>
                <Col span={12}>
                  <div style={{ marginBottom: '16px' }}>
                    <Text strong>用户ID</Text>
                    <div style={{ marginTop: '4px' }}>
                      <Text copyable={{ text: user.userId }}>
                        {user.userId}
                      </Text>
                    </div>
                  </div>
                </Col>
                <Col span={12}>
                  <div style={{ marginBottom: '16px' }}>
                    <Text strong>用户角色</Text>
                    <div style={{ marginTop: '4px' }}>
                      <Tag color={roleDisplay.color}>
                        {roleDisplay.name}
                      </Tag>
                    </div>
                  </div>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <div style={{ marginBottom: '16px' }}>
                    <Text strong>账户状态</Text>
                    <div style={{ marginTop: '4px' }}>
                      <Tag color={statusDisplay.color}>
                        {statusDisplay.name}
                      </Tag>
                    </div>
                  </div>
                </Col>
                <Col span={12}>
                  <div style={{ marginBottom: '16px' }}>
                    <Text strong>创建时间</Text>
                    <div style={{ marginTop: '4px' }}>
                      <Text>{formatDateTime(user.createdAt)}</Text>
                    </div>
                  </div>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <div style={{ marginBottom: '16px' }}>
                    <Text strong>最后更新</Text>
                    <div style={{ marginTop: '4px' }}>
                      <Text>{formatDateTime(user.updatedAt)}</Text>
                    </div>
                  </div>
                </Col>
                <Col span={12}>
                  <div style={{ marginBottom: '16px' }}>
                    <Text strong>最后登录</Text>
                    <div style={{ marginTop: '4px' }}>
                      <Text>
                        {user.lastLoginTime ? formatDateTime(user.lastLoginTime) : '未知'}
                      </Text>
                      {user.lastLoginIp && (
                        <div style={{ fontSize: '12px', color: '#999' }}>
                          IP: {user.lastLoginIp}
                        </div>
                      )}
                    </div>
                  </div>
                </Col>
              </Row>
            </Form>
          </Col>
        </Row>
      </Card>
    </div>
  )
}

export default UserProfilePage
