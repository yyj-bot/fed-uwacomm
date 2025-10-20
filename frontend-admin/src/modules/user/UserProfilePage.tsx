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
  IdcardOutlined,
  SafetyOutlined,
  ClockCircleOutlined,
  EnvironmentOutlined,
  CheckCircleOutlined
} from '@ant-design/icons'

import { useAuth } from '@/store/auth'
import type { UpdateProfileRequest } from '@/services/user'
import styles from './UserProfilePage.module.css'

const { Title, Text } = Typography

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
      <div className={styles.profileContainer}>
        <div className={styles.contentWrapper}>
          <div className={styles.loadingWrapper}>
            <Spin size="large" />
            <div className={styles.loadingText}>
              <Text type="secondary">正在加载用户信息...</Text>
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (!user) {
    return (
      <div className={styles.profileContainer}>
        <div className={styles.contentWrapper}>
          <div className={styles.errorWrapper}>
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
        </div>
      </div>
    )
  }

  const roleDisplay = getRoleDisplay(user.role)
  const statusDisplay = getStatusDisplay(user.status)

  return (
    <div className={styles.profileContainer}>
      <div className={styles.contentWrapper}>
        <Card
          className={styles.profileCard}
          title={
            <div className={styles.cardTitle}>
              <UserOutlined className={styles.titleIcon} />
              <Title level={4} className={styles.titleText}>个人资料</Title>
            </div>
          }
          extra={
            !isEditing ? (
              <Button
                type="primary"
                icon={<EditOutlined />}
                onClick={handleEdit}
                className={styles.editButton}
              >
                编辑资料
              </Button>
            ) : (
              <Space className={styles.actionButtons}>
                <Button
                  icon={<CloseOutlined />}
                  onClick={handleCancel}
                  className={styles.cancelButton}
                >
                  取消
                </Button>
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  loading={isUpdatingProfile}
                  onClick={handleSave}
                  className={styles.saveButton}
                >
                  保存
                </Button>
              </Space>
            )
          }
        >
        <Row gutter={[24, 24]}>
          {/* 左侧头像区域 */}
          <Col xs={24} sm={24} md={8} lg={7}>
            <div className={styles.avatarSection}>
              <div className={styles.avatarWrapper}>
                <Avatar
                  size={120}
                  icon={<UserOutlined />}
                  className={styles.avatar}
                />
                <div className={styles.avatarBadge} title="在线" />
              </div>
              <div className={styles.userInfo}>
                <Title level={5} className={styles.username}>
                  {user.username}
                </Title>
                <Text className={styles.userEmail}>{user.email}</Text>
              </div>
            </div>
          </Col>

          {/* 右侧信息区域 */}
          <Col xs={24} sm={24} md={16} lg={17}>
            <div className={styles.infoSection}>
              <Form
                form={form}
                layout="vertical"
                disabled={!isEditing}
                className={styles.formWrapper}
              >
                {/* 基本信息 */}
                <Row gutter={16}>
                  <Col xs={24} sm={12}>
                    <Form.Item
                      label={
                        <span>
                          <UserOutlined style={{ marginRight: 6 }} />
                          用户名
                        </span>
                      }
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
                  <Col xs={24} sm={12}>
                    <Form.Item
                      label={
                        <span>
                          <MailOutlined style={{ marginRight: 6 }} />
                          邮箱地址
                        </span>
                      }
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

                <Divider className={styles.divider}>系统信息</Divider>

                {/* 系统信息（只读） */}
                <Row gutter={16}>
                  <Col xs={24} sm={12}>
                    <div className={styles.infoItem}>
                      <div className={styles.infoLabel}>
                        <IdcardOutlined />
                        用户ID
                      </div>
                      <div className={styles.infoValue}>
                        <Text copyable={{ text: user.userId }}>
                          {user.userId}
                        </Text>
                      </div>
                    </div>
                  </Col>
                  <Col xs={24} sm={12}>
                    <div className={styles.infoItem}>
                      <div className={styles.infoLabel}>
                        <SafetyOutlined />
                        用户角色
                      </div>
                      <div className={styles.infoValue}>
                        <Tag color={roleDisplay.color} className={styles.roleTag}>
                          {roleDisplay.name}
                        </Tag>
                      </div>
                    </div>
                  </Col>
                </Row>

                <Row gutter={16}>
                  <Col xs={24} sm={12}>
                    <div className={styles.infoItem}>
                      <div className={styles.infoLabel}>
                        <CheckCircleOutlined />
                        账户状态
                      </div>
                      <div className={styles.infoValue}>
                        <Tag color={statusDisplay.color} className={styles.statusTag}>
                          {statusDisplay.name}
                        </Tag>
                      </div>
                    </div>
                  </Col>
                  <Col xs={24} sm={12}>
                    <div className={styles.infoItem}>
                      <div className={styles.infoLabel}>
                        <ClockCircleOutlined />
                        创建时间
                      </div>
                      <div className={styles.infoValue}>
                        <Text>{formatDateTime(user.createdAt)}</Text>
                      </div>
                    </div>
                  </Col>
                </Row>

                <Row gutter={16}>
                  <Col xs={24} sm={12}>
                    <div className={styles.infoItem}>
                      <div className={styles.infoLabel}>
                        <ClockCircleOutlined />
                        最后更新
                      </div>
                      <div className={styles.infoValue}>
                        <Text>{formatDateTime(user.updatedAt)}</Text>
                      </div>
                    </div>
                  </Col>
                  <Col xs={24} sm={12}>
                    <div className={styles.infoItem}>
                      <div className={styles.infoLabel}>
                        <ClockCircleOutlined />
                        最后登录
                      </div>
                      <div className={styles.infoValue}>
                        <Text>
                          {user.lastLoginTime ? formatDateTime(user.lastLoginTime) : '未知'}
                        </Text>
                        {user.lastLoginIp && (
                          <div className={styles.ipInfo}>
                            <EnvironmentOutlined />
                            IP: {user.lastLoginIp}
                          </div>
                        )}
                      </div>
                    </div>
                  </Col>
                </Row>
              </Form>
            </div>
          </Col>
        </Row>
        </Card>
      </div>
    </div>
  )
}

export default UserProfilePage
