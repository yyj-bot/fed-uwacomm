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
  Typography 
} from 'antd'
import { 
  UserOutlined, 
  MailOutlined, 
  SafetyOutlined,
  ClockCircleOutlined,
  EditOutlined,
  LockOutlined,
  UnlockOutlined,
  KeyOutlined
} from '@ant-design/icons'
import { StatusIndicator } from '@/components'
import { useAdmin } from '@/store'
import type { User } from '@/types'

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
          <UserOutlined />
          用户详情
        </Space>
      }
      open={visible}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          关闭
        </Button>,
        <Button 
          key="edit" 
          type="primary" 
          icon={<EditOutlined />}
          onClick={handleEdit}
          disabled={!currentUser}
        >
          编辑
        </Button>
      ]}
      width={700}
      destroyOnClose
    >
      <Spin spinning={isLoading}>
        {currentUser ? (
          <div style={{ padding: '16px 0' }}>
            <Descriptions 
              column={2} 
              bordered
              size="small"
              labelStyle={{ width: '120px', fontWeight: 'bold' }}
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
            <div style={{ marginTop: 24, borderTop: '1px solid #f0f0f0', paddingTop: 16 }}>
              <Space>
                <Text strong>快速操作：</Text>
                {isLocked ? (
                  <Button 
                    type="primary" 
                    icon={<UnlockOutlined />}
                    loading={unlockLoading}
                    onClick={handleUnlockUser}
                    size="small"
                  >
                    解锁用户
                  </Button>
                ) : (
                  <Button 
                    danger 
                    icon={<LockOutlined />}
                    loading={lockLoading}
                    onClick={handleLockUser}
                    size="small"
                  >
                    锁定用户
                  </Button>
                )}
                <Button 
                  icon={<KeyOutlined />}
                  onClick={() => {
                    if (currentUser) {
                      onResetPassword?.(currentUser)
                    }
                  }}
                  size="small"
                >
                  重置密码
                </Button>
              </Space>
            </div>
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
