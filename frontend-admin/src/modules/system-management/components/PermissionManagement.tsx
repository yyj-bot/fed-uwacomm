/**
 * 角色权限说明组件
 * 显示系统中各角色的权限范围，不支持修改
 */

import React from 'react'
import { Card, Table, Tag, Typography, Alert, Space, Row, Col } from 'antd'
import { 
  CrownOutlined, 
  ExperimentOutlined, 
  ToolOutlined, 
  EyeOutlined,
  InfoCircleOutlined,
  SafetyOutlined,
  CheckCircleOutlined
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
      {/* 页面标题和说明 */}
      <Card 
        style={{ 
          marginBottom: 24,
          background: 'linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%)',
          border: '2px solid #91d5ff',
          borderRadius: '16px',
          boxShadow: '0 4px 16px rgba(24, 144, 255, 0.1)'
        }}
        bodyStyle={{ padding: '24px 32px' }}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Title level={4} style={{ margin: 0, color: '#1890ff' }}>
            <Space>
              <SafetyOutlined style={{ fontSize: '24px' }} />
              角色权限管理（RBAC）
            </Space>
          </Title>
          <Paragraph style={{ margin: 0, fontSize: '15px', color: '#666' }}>
            本系统采用基于角色的权限管理（Role-Based Access Control），通过用户角色来控制权限。
            角色权限是预定义的，不可修改。要修改用户权限，请在"用户管理"中修改用户角色。
          </Paragraph>
        </Space>
      </Card>

      {/* 操作说明 */}
      <Alert
        message={
          <span style={{ fontSize: '16px', fontWeight: 600 }}>
            <InfoCircleOutlined style={{ marginRight: '8px' }} />
            权限管理操作指南
          </span>
        }
        description={
          <div style={{ fontSize: '14px', lineHeight: '28px' }}>
            <Row gutter={[16, 12]}>
              <Col span={24}>
                <CheckCircleOutlined style={{ color: '#52c41a', marginRight: '8px' }} />
                <Text strong>修改用户权限</Text>：通过在"用户管理"页面修改用户角色实现
              </Col>
              <Col span={24}>
                <CheckCircleOutlined style={{ color: '#52c41a', marginRight: '8px' }} />
                <Text strong>权限检查</Text>：服务层自动根据用户角色进行权限验证
              </Col>
              <Col span={24}>
                <CheckCircleOutlined style={{ color: '#52c41a', marginRight: '8px' }} />
                <Text strong>权限查询</Text>：通过获取用户详情查看当前角色
              </Col>
            </Row>
          </div>
        }
        type="info"
        showIcon
        style={{ 
          marginBottom: 32,
          borderRadius: '12px',
          border: '2px solid #b7eb8f',
          background: 'linear-gradient(135deg, #f6ffed 0%, #fcffe6 100%)'
        }}
      />

      {/* 角色权限对应关系 */}
      <Card 
        title={
          <span style={{ fontSize: '16px', fontWeight: 600 }}>
            <CrownOutlined style={{ marginRight: '8px', color: '#1890ff' }} />
            角色权限对应关系
          </span>
        }
        style={{
          borderRadius: '16px',
          boxShadow: '0 4px 16px rgba(0, 0, 0, 0.06)',
          border: '1px solid #e8e8e8'
        }}
        headStyle={{
          background: 'linear-gradient(135deg, #fafafa 0%, #ffffff 100%)',
          borderBottom: '2px solid #f0f0f0',
          borderRadius: '16px 16px 0 0'
        }}
      >
        <Table
          columns={columns}
          dataSource={rolePermissions}
          pagination={false}
          size="middle"
          style={{
            borderRadius: '12px',
            overflow: 'hidden'
          }}
        />
      </Card>

      {/* 权限详细说明 */}
      <Card 
        title={
          <span style={{ fontSize: '16px', fontWeight: 600 }}>
            <InfoCircleOutlined style={{ marginRight: '8px', color: '#722ed1' }} />
            权限详细说明
          </span>
        }
        style={{ 
          marginTop: 24,
          borderRadius: '16px',
          boxShadow: '0 4px 16px rgba(0, 0, 0, 0.06)',
          border: '1px solid #e8e8e8'
        }}
        headStyle={{
          background: 'linear-gradient(135deg, #fafafa 0%, #ffffff 100%)',
          borderBottom: '2px solid #f0f0f0',
          borderRadius: '16px 16px 0 0'
        }}
        bodyStyle={{ padding: '24px 32px' }}
      >
        <Row gutter={[24, 20]}>
          <Col xs={24} sm={12}>
            <Card
              style={{
                background: 'linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%)',
                border: '1px solid #91d5ff',
                borderRadius: '12px'
              }}
              bodyStyle={{ padding: '16px 20px' }}
            >
              <Space direction="vertical" size={8}>
                <Tag color="blue" style={{ fontSize: '14px', padding: '4px 12px' }}>
                  READ - 读取权限
                </Tag>
                <Text style={{ fontSize: '14px', color: '#666' }}>
                  可以查看和读取系统中的信息、数据和配置
                </Text>
              </Space>
            </Card>
          </Col>
          <Col xs={24} sm={12}>
            <Card
              style={{
                background: 'linear-gradient(135deg, #f6ffed 0%, #fcffe6 100%)',
                border: '1px solid #b7eb8f',
                borderRadius: '12px'
              }}
              bodyStyle={{ padding: '16px 20px' }}
            >
              <Space direction="vertical" size={8}>
                <Tag color="green" style={{ fontSize: '14px', padding: '4px 12px' }}>
                  WRITE - 写入权限
                </Tag>
                <Text style={{ fontSize: '14px', color: '#666' }}>
                  可以创建和修改系统中的数据和资源
                </Text>
              </Space>
            </Card>
          </Col>
          <Col xs={24} sm={12}>
            <Card
              style={{
                background: 'linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%)',
                border: '1px solid #ffd591',
                borderRadius: '12px'
              }}
              bodyStyle={{ padding: '16px 20px' }}
            >
              <Space direction="vertical" size={8}>
                <Tag color="orange" style={{ fontSize: '14px', padding: '4px 12px' }}>
                  EXECUTE - 执行权限
                </Tag>
                <Text style={{ fontSize: '14px', color: '#666' }}>
                  可以执行系统操作，如启动任务、控制虚拟机等
                </Text>
              </Space>
            </Card>
          </Col>
          <Col xs={24} sm={12}>
            <Card
              style={{
                background: 'linear-gradient(135deg, #fff1f0 0%, #fff2e8 100%)',
                border: '1px solid #ffccc7',
                borderRadius: '12px'
              }}
              bodyStyle={{ padding: '16px 20px' }}
            >
              <Space direction="vertical" size={8}>
                <Tag color="red" style={{ fontSize: '14px', padding: '4px 12px' }}>
                  DELETE - 删除权限
                </Tag>
                <Text style={{ fontSize: '14px', color: '#666' }}>
                  可以删除系统中的数据和资源（高危操作）
                </Text>
              </Space>
            </Card>
          </Col>
        </Row>
      </Card>
    </div>
  )
}

export default PermissionManagement
