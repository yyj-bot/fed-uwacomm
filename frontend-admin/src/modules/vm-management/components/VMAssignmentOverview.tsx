/**
 * VM分配概况组件
 * 显示水下节点分配统计信息、状态分布和最近分配记录
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect } from 'react'
import { Card, Row, Col, Statistic, Table, Tag, Spin, Alert, List, Typography } from 'antd'
import {
  DatabaseOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  UserOutlined,
  TeamOutlined,
  ClockCircleOutlined
} from '@ant-design/icons'
import { useAdminVM } from '@/store/vm'
import type { VmStatus } from '@/services/admin/type'
import type { ColumnsType } from 'antd/es/table'

const { Title, Text } = Typography

const VMAssignmentOverview: React.FC = () => {
  const {
    assignmentOverview,
    assignmentOverviewLoading,
    assignmentOverviewError,
    fetchVmAssignmentOverview
  } = useAdminVM()

  useEffect(() => {
    fetchVmAssignmentOverview()
  }, [fetchVmAssignmentOverview])

  // VM状态标签颜色映射
  const getStatusColor = (status: VmStatus): string => {
    const colorMap: Record<VmStatus, string> = {
      'RUNNING': 'success',
      'STOPPED': 'default',
      'ERROR': 'error',
      'STARTING': 'processing',
      'STOPPING': 'warning',
      'OFFLINE': 'default'
    }
    return colorMap[status] || 'default'
  }

  // 热门VM表格列定义
  const topVmColumns: ColumnsType<any> = [
    {
      title: '排名',
      key: 'rank',
      width: 80,
      render: (_: any, __: any, index: number) => (
        <Tag color={index === 0 ? 'gold' : index === 1 ? 'silver' : index === 2 ? '#cd7f32' : 'default'}>
          {index + 1}
        </Tag>
      )
    },
    {
      title: '水下节点名称',
      dataIndex: 'vmName',
      key: 'vmName'
    },
    {
      title: '分配用户数',
      dataIndex: 'assignedUserCount',
      key: 'assignedUserCount',
      render: (count: number) => (
        <Text strong>{count}</Text>
      )
    }
  ]

  if (assignmentOverviewLoading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin tip="加载中..." />
        </div>
      </Card>
    )
  }

  if (assignmentOverviewError) {
    return (
      <Card>
        <Alert
          message="加载失败"
          description={assignmentOverviewError}
          type="error"
          showIcon
        />
      </Card>
    )
  }

  if (!assignmentOverview) {
    return (
      <Card>
        <Alert
          message="暂无数据"
          description="没有可显示的分配概况信息"
          type="info"
          showIcon
        />
      </Card>
    )
  }

  const { summary, statusDistribution, topAssignedVms, recentAssignments } = assignmentOverview

  return (
    <div className="vm-assignment-overview">
      {/* 统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={8} lg={4}>
          <Card 
            bordered={true} 
            style={{ 
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}
          >
            <Statistic
              title="总水下节点数"
              value={summary.totalVms}
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={4}>
          <Card 
            bordered={true} 
            style={{ 
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}
          >
            <Statistic
              title="已分配"
              value={summary.assignedVms}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={4}>
          <Card 
            bordered={true} 
            style={{ 
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}
          >
            <Statistic
              title="未分配"
              value={summary.unassignedVms}
              prefix={<CloseCircleOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={4}>
          <Card 
            bordered={true} 
            style={{ 
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}
          >
            <Statistic
              title="总用户数"
              value={summary.totalUsers}
              prefix={<TeamOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={4}>
          <Card 
            bordered={true} 
            style={{ 
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}
          >
            <Statistic
              title="拥有机器人的用户"
              value={summary.usersWithVms}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={4}>
          <Card 
            bordered={true} 
            style={{ 
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}
          >
            <Statistic
              title="分配率"
              value={summary.totalVms > 0 ? ((summary.assignedVms / summary.totalVms) * 100).toFixed(1) : 0}
              suffix="%"
              valueStyle={{ color: '#13c2c2' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        {/* 状态分布 */}
        <Col xs={24} lg={8}>
          <Card 
            title="水下节点状态分布" 
            bordered={true}
            style={{ 
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}
          >
            <List
              dataSource={Object.entries(statusDistribution)}
              renderItem={([status, count]) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={<Tag color={getStatusColor(status as VmStatus)}>{status}</Tag>}
                    title={`${count} 台`}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>

        {/* 热门水下节点 */}
        <Col xs={24} lg={8}>
          <Card 
            title="热门水下节点 (分配最多)" 
            bordered={true}
            style={{ 
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}
          >
            {topAssignedVms && topAssignedVms.length > 0 ? (
              <Table
                dataSource={topAssignedVms}
                columns={topVmColumns}
                pagination={false}
                size="small"
                rowKey="vmId"
              />
            ) : (
              <Alert message="暂无数据" type="info" showIcon />
            )}
          </Card>
        </Col>

        {/* 最近分配记录 */}
        <Col xs={24} lg={8}>
          <Card 
            title="最近分配记录" 
            bordered={true}
            style={{ 
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}
          >
            {recentAssignments && recentAssignments.length > 0 ? (
              <List
                dataSource={recentAssignments}
                renderItem={(item) => (
                  <List.Item>
                    <List.Item.Meta
                      avatar={<ClockCircleOutlined />}
                      title={`${item.vmName} → ${item.username}`}
                      description={new Date(item.assignedAt).toLocaleString()}
                    />
                  </List.Item>
                )}
              />
            ) : (
              <Alert message="暂无最近分配记录" type="info" showIcon />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default VMAssignmentOverview

