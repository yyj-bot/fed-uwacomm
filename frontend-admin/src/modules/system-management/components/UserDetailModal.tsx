/**
 * 用户详情查看模态框
 */

import React, { useEffect } from 'react'
import { 
  Modal, 
  Descriptions, 
  Tag, 
  Button, 
  Space, 
  Spin,
  message,
  Typography,
  Divider,
  Card 
} from 'antd'
import { 
  UserOutlined, 
  MailOutlined, 
  SafetyOutlined,
  ClockCircleOutlined,
  EditOutlined,
  LockOutlined,
  UnlockOutlined,
  KeyOutlined,
  IdcardOutlined
} from '@ant-design/icons'
import { StatusIndicator } from '@/components'
import { useAdmin } from '@/store'
import type { User } from '@/types'
import './SystemManagementModals.css'

const { Text } = Typography

interface UserDetailModalProps {
  visible: boolean
  onCancel: () => void
  onEdit?: (user: User) => void
  onResetPassword?: (user: User) => void
  userId?: string
}

const UserDetailModal: React.FC<UserDetailModalProps> = ({
  visible,
  onCancel,
  onEdit,
  onResetPassword,
  userId
}) => {
  const { 
    currentUser, 
    currentUserLoading, 
    fetchUserDetail,
    lockUser,
    unlockUser,
    operationLoading
  } = useAdmin()

  // 当模态框打开时获取用户详情
  useEffect(() => {
    if (visible && userId) {
      fetchUserDetail(userId).catch(error => {
        console.error('获取用户详情失败:', error)
        message.error('获取用户详情失败')
      })
    }
  }, [visible, userId, fetchUserDetail])

  const handleLockUser = async () => {
    if (!currentUser) return
    
    try {
      await lockUser(currentUser.userId)
      message.success('用户已锁定')
      // 重新获取用户详情以更新状态
      await fetchUserDetail(currentUser.userId)
    } catch (error) {
      console.error('锁定用户失败:', error)
      message.error('锁定用户失败')
    }
  }

  const handleUnlockUser = async () => {
    if (!currentUser) return
    
    try {
      await unlockUser(currentUser.userId)
      message.success('用户已解锁')
      // 重新获取用户详情以更新状态
      await fetchUserDetail(currentUser.userId)
    } catch (error) {
      console.error('解锁用户失败:', error)
      message.error('解锁用户失败')
    }
  }

  const handleEdit = () => {
    if (currentUser) {
      onEdit?.(currentUser)
    }
  }

  const getRoleConfig = (role: string) => {
    const roleConfig: Record<string, { color: string; text: string }> = {
      'ADMIN': { color: 'red', text: '管理员' },
      'RESEARCHER': { color: 'blue', text: '研究员' },
      'OPERATOR': { color: 'green', text: '操作员' },
      'VIEWER': { color: 'default', text: '观察者' }
    }
    return roleConfig[role] || { color: 'default', text: role }
  }

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-'
    return new Date(dateString).toLocaleString('zh-CN')
  }

  const isLocked = currentUser?.status === 'LOCKED'
  const isLoading = currentUserLoading
  const lockLoading = operationLoading[`lock-${currentUser?.userId}`] || false
  const unlockLoading = operationLoading[`unlock-${currentUser?.userId}`] || false

  return (
    <Modal
      title={
        <Space>
          <IdcardOutlined style={{ color: '#1890ff', fontSize: '20px' }} />
          <span style={{ fontSize: '18px', fontWeight: 600 }}>用户详细信息</span>
        </Space>
      }
      open={visible}
      onCancel={onCancel}
      centered
      footer={[
        <Button key="cancel" onClick={onCancel} size="large">
          关闭
        </Button>,
        <Button 
          key="edit" 
          type="primary" 
          icon={<EditOutlined />}
          onClick={handleEdit}
          disabled={!currentUser}
          size="large"
        >
          编辑用户
        </Button>
      ]}
      width={750}
      destroyOnHidden
    >
      <Spin spinning={isLoading}>
        {currentUser ? (
          <div style={{ padding: '8px 0' }}>
            <Descriptions 
              column={2} 
              bordered
              size="middle"
              labelStyle={{ 
                width: '140px', 
                fontWeight: 600,
                fontSize: '14px',
                background: 'linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%)'
              }}
              contentStyle={{
                fontSize: '14px',
                padding: '12px 16px'
              }}
            >
              <Descriptions.Item 
                label={
                  <Space>
                    <UserOutlined />
                    用户名
                  </Space>
                }
                span={1}
              >
                <Text strong>{currentUser.username}</Text>
              </Descriptions.Item>

              <Descriptions.Item 
                label={
                  <Space>
                    <MailOutlined />
                    邮箱
                  </Space>
                }
                span={1}
              >
                <Text copyable>{currentUser.email}</Text>
              </Descriptions.Item>

              <Descriptions.Item 
                label={
                  <Space>
                    <SafetyOutlined />
                    角色
                  </Space>
                }
                span={1}
              >
                {(() => {
                  const config = getRoleConfig(currentUser.role)
                  return <Tag color={config.color}>{config.text}</Tag>
                })()}
              </Descriptions.Item>

              <Descriptions.Item 
                label="状态"
                span={1}
              >
                <StatusIndicator
                  status={currentUser.status === 'ACTIVE' ? 'active' : 'inactive'}
                  text={
                    currentUser.status === 'ACTIVE' ? '活跃' : 
                    currentUser.status === 'LOCKED' ? '已锁定' : '禁用'
                  }
                  variant="badge"
                  size="small"
                />
              </Descriptions.Item>

              <Descriptions.Item 
                label={
                  <Space>
                    <ClockCircleOutlined />
                    创建时间
                  </Space>
                }
                span={1}
              >
                {formatDate(currentUser.createdAt)}
              </Descriptions.Item>

              <Descriptions.Item 
                label="更新时间"
                span={1}
              >
                {formatDate(currentUser.updatedAt)}
              </Descriptions.Item>

              <Descriptions.Item 
                label="最后登录时间"
                span={1}
              >
                {formatDate(currentUser.lastLoginTime)}
              </Descriptions.Item>

              <Descriptions.Item 
                label="最后登录IP"
                span={1}
              >
                {currentUser.lastLoginIp || '-'}
              </Descriptions.Item>

              <Descriptions.Item 
                label="用户ID"
                span={2}
              >
                <Text code copyable>{currentUser.userId}</Text>
              </Descriptions.Item>
            </Descriptions>

            {/* 操作按钮区域 */}
            <Card 
              className="user-quick-actions"
              style={{ 
                marginTop: 24, 
                background: '#ffffff',
                border: '1px solid #e8e8e8',
                borderRadius: '12px',
                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
              }}
              bodyStyle={{ padding: '16px 20px' }}
            >
              <Space size="middle" className="quick-action-buttons">
                <Text strong style={{ color: '#262626', fontSize: '15px' }}>快速操作：</Text>
                {isLocked ? (
                  <Button 
                    type="primary" 
                    loading={unlockLoading}
                    onClick={handleUnlockUser}
                    style={{
                      minWidth: '110px',
                      display: 'inline-flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '6px',
                      background: 'linear-gradient(135deg, #52c41a 0%, #389e0d 100%)',
                      border: 'none',
                      boxShadow: '0 2px 8px rgba(82, 196, 26, 0.3)'
                    }}
                  >
                    <UnlockOutlined />
                    <span>解锁用户</span>
                  </Button>
                ) : (
                  <Button 
                    danger 
                    loading={lockLoading}
                    onClick={handleLockUser}
                    style={{
                      minWidth: '110px',
                      display: 'inline-flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '6px',
                      background: 'linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%)',
                      border: 'none',
                      color: 'white',
                      boxShadow: '0 2px 8px rgba(255, 77, 79, 0.3)'
                    }}
                  >
                    <LockOutlined />
                    <span>锁定用户</span>
                  </Button>
                )}
                <Button 
                  onClick={() => {
                    if (currentUser) {
                      onResetPassword?.(currentUser)
                    }
                  }}
                  style={{
                    minWidth: '110px',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                    background: 'linear-gradient(135deg, #faad14 0%, #d48806 100%)',
                    border: 'none',
                    color: 'white',
                    boxShadow: '0 2px 8px rgba(250, 173, 20, 0.3)'
                  }}
                >
                  <KeyOutlined />
                  <span>重置密码</span>
                </Button>
              </Space>
            </Card>
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Text type="secondary">暂无用户数据</Text>
          </div>
        )}
      </Spin>
    </Modal>
  )
}

export default UserDetailModal
