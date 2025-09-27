/**
 * 角色权限说明组件
 * 显示系统中各角色的权限范围，不支持修改
 */

import React from 'react'
import { Card, Table, Tag, Typography, Alert, Space } from 'antd'
import { 
  CrownOutlined, 
  ExperimentOutlined, 
  ToolOutlined, 
  EyeOutlined,
  InfoCircleOutlined
} from '@ant-design/icons'

const { Title, Text, Paragraph } = Typography

const PermissionManagement: React.FC = () => {
  // 角色权限对应关系（来自API文档）
  const rolePermissions = [
    {
      key: 'ADMIN',
      role: 'ADMIN',
      roleName: '管理员',
      icon: <CrownOutlined style={{ color: '#ff4d4f' }} />,
      permissions: ['READ', 'WRITE', 'EXECUTE', 'DELETE'],
      description: '系统管理员，拥有所有权限',
      color: 'red'
    },
    {
      key: 'RESEARCHER',
      role: 'RESEARCHER', 
      roleName: '研究员',
      icon: <ExperimentOutlined style={{ color: '#1890ff' }} />,
      permissions: ['READ', 'WRITE', 'EXECUTE'],
      description: '研究人员，可以读写执行，但不能删除',
      color: 'blue'
    },
    {
      key: 'OPERATOR',
      role: 'OPERATOR',
      roleName: '操作员',
      icon: <ToolOutlined style={{ color: '#52c41a' }} />,
      permissions: ['READ', 'EXECUTE'],
      description: '操作员，可以读取和执行操作',
      color: 'green'
    },
    {
      key: 'VIEWER',
      role: 'VIEWER',
      roleName: '观察者',
      icon: <EyeOutlined style={{ color: '#d9d9d9' }} />,
      permissions: ['READ'],
      description: '查看者，只能读取信息',
      color: 'default'
    }
  ]

  const columns = [
    {
      title: '角色',
      dataIndex: 'roleName',
      key: 'roleName',
      render: (text: string, record: any) => (
        <Space>
          {record.icon}
          <Text strong>{text}</Text>
          <Text type="secondary">({record.role})</Text>
        </Space>
      )
    },
    {
      title: '权限范围',
      dataIndex: 'permissions',
      key: 'permissions',
      render: (permissions: string[]) => (
        <Space>
          {permissions.map(permission => {
            const permissionConfig: Record<string, { color: string; text: string }> = {
              'READ': { color: 'blue', text: '读取' },
              'WRITE': { color: 'green', text: '写入' },
              'EXECUTE': { color: 'orange', text: '执行' },
              'DELETE': { color: 'red', text: '删除' }
            }
            const config = permissionConfig[permission]
            return (
              <Tag key={permission} color={config.color}>
                {config.text}
              </Tag>
            )
          })}
        </Space>
      )
    },
    {
      title: '说明',
      dataIndex: 'description',
      key: 'description',
      render: (text: string) => <Text type="secondary">{text}</Text>
    }
  ]

  return (
    <div className="fed-permission-management">
      <div style={{ marginBottom: 24 }}>
        <Title level={4}>
          <Space>
            <InfoCircleOutlined />
            角色权限说明
          </Space>
        </Title>
        <Paragraph type="secondary">
          本系统采用基于角色的权限管理（RBAC），通过用户角色来控制权限。
          角色权限是预定义的，不可修改。要修改用户权限，请在"用户管理"中修改用户角色。
        </Paragraph>
      </div>

      <Alert
        message="权限管理说明"
        description={
          <div>
            <Paragraph style={{ marginBottom: 8 }}>
              • <Text strong>修改用户权限</Text>：通过在"用户管理"页面修改用户角色实现
            </Paragraph>
            <Paragraph style={{ marginBottom: 8 }}>
              • <Text strong>权限检查</Text>：服务层自动根据用户角色进行权限验证
            </Paragraph>
            <Paragraph style={{ marginBottom: 0 }}>
              • <Text strong>权限查询</Text>：通过获取用户详情查看当前角色
            </Paragraph>
          </div>
        }
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
      />

      <Card title="角色权限对应关系" size="small">
        <Table
          columns={columns}
          dataSource={rolePermissions}
          pagination={false}
          size="small"
        />
      </Card>

      <Card 
        title="权限详细说明" 
        size="small" 
        style={{ marginTop: 16 }}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            <Tag color="blue">读取 (READ)</Tag>
            <Text type="secondary">可以查看和读取系统中的信息</Text>
          </div>
          <div>
            <Tag color="green">写入 (WRITE)</Tag>
            <Text type="secondary">可以创建和修改系统中的数据</Text>
          </div>
          <div>
            <Tag color="orange">执行 (EXECUTE)</Tag>
            <Text type="secondary">可以执行系统操作，如启动任务、控制虚拟机等</Text>
          </div>
          <div>
            <Tag color="red">删除 (DELETE)</Tag>
            <Text type="secondary">可以删除系统中的数据和资源</Text>
          </div>
        </Space>
      </Card>
    </div>
  )
}

export default PermissionManagement
