/**
 * 用户虚拟机管理组件
 * 提供查看和管理特定用户的虚拟机分配
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import {
  Select,
  Card,
  Empty,
  Spin,
  Alert,
  Space,
  Button,
  message
} from 'antd'
import {
  UserOutlined,
  ReloadOutlined
} from '@ant-design/icons'

import { useAdmin } from '@/store'
import { UserVmList } from './'

const { Option } = Select

const UserVmManagement: React.FC = () => {
  const [selectedUserId, setSelectedUserId] = useState<string>('')
  const [selectedUserName, setSelectedUserName] = useState<string>('')
  
  const {
    userList,
    userListLoading,
    fetchUserList
  } = useAdmin()

  // 初始化加载用户列表
  useEffect(() => {
    fetchUserList({ page: 1, size: 100 })
  }, [])

  // 处理用户选择
  const handleUserChange = (userId: string) => {
    setSelectedUserId(userId)
    const user = userList.find(u => u.userId === userId)
    setSelectedUserName(user?.username || '')
  }

  // 刷新用户列表
  const handleRefresh = async () => {
    try {
      await fetchUserList({ page: 1, size: 100 })
      message.success('用户列表刷新成功')
    } catch (error) {
      message.error('刷新失败')
    }
  }

  return (
    <div style={{ padding: '20px' }}>
      <Card
        title={
          <Space>
            <UserOutlined />
            <span>用户虚拟机管理</span>
          </Space>
        }
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={handleRefresh}
            loading={userListLoading}
          >
            刷新用户列表
          </Button>
        }
      >
        {/* 用户选择器 */}
        <div style={{ marginBottom: 20 }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <div>
              <span style={{ marginRight: 10, fontWeight: 500 }}>选择用户：</span>
              <Select
                showSearch
                placeholder="请选择要管理的用户"
                style={{ width: 300 }}
                value={selectedUserId || undefined}
                onChange={handleUserChange}
                loading={userListLoading}
                filterOption={(input, option) =>
                  (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                }
                options={userList.map(user => ({
                  label: `${user.username} (${user.email || user.userId})`,
                  value: user.userId
                }))}
              />
            </div>
            {selectedUserId && (
              <Alert
                message={`当前管理用户: ${selectedUserName} (ID: ${selectedUserId})`}
                type="info"
                showIcon
              />
            )}
          </Space>
        </div>

        {/* 用户VM列表 */}
        {selectedUserId ? (
          <UserVmList
            userId={selectedUserId}
            username={selectedUserName}
          />
        ) : (
          <Empty
            description="请先选择一个用户以查看和管理其虚拟机"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            style={{ padding: '60px 0' }}
          />
        )}
      </Card>
    </div>
  )
}

export default UserVmManagement

