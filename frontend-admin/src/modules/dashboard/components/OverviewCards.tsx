/**
 * 概览卡片组件
 * 显示系统关键指标的统计卡片
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { Row, Col, Card, Statistic, Skeleton } from 'antd'
import { 
  CloudServerOutlined, 
  ExperimentOutlined, 
  DatabaseOutlined, 
  TeamOutlined 
} from '@ant-design/icons'

import type { DashboardOverview } from '@/store'

interface OverviewCardsProps {
  overview?: DashboardOverview
  loading?: boolean
}

const OverviewCards: React.FC<OverviewCardsProps> = ({ 
  overview, 
  loading = false 
}) => {
  const cards = [
    {
      title: '运行中的虚拟机',
      value: overview?.vmStats?.runningVMs || 0,
      total: overview?.vmStats?.totalVMs || 0,
      icon: <CloudServerOutlined />,
      type: 'primary' as const,
      suffix: `/ ${overview?.vmStats?.totalVMs || 0}`
    },
    {
      title: '活跃任务',
      value: overview?.taskStats?.runningTasks || 0,
      total: overview?.taskStats?.totalTasks || 0,
      icon: <ExperimentOutlined />,
      type: 'success' as const,
      suffix: `/ ${overview?.taskStats?.totalTasks || 0}`
    },
    {
      title: '在线参与者',
      value: Math.floor(Math.random() * 50) + 10,
      total: Math.floor(Math.random() * 100) + 50,
      icon: <TeamOutlined />,
      type: 'warning' as const,
      suffix: `/ ${Math.floor(Math.random() * 100) + 50}`
    },
    {
      title: '数据集',
      value: overview?.dataStats?.totalDatasets || 0,
      icon: <DatabaseOutlined />,
      type: 'danger' as const
    }
  ]

  if (loading) {
    return (
      <Row gutter={[16, 16]} className="fed-overview-cards">
        {cards.map((_, index) => (
          <Col xs={12} sm={12} lg={6} key={index}>
            <Card className="fed-overview-card">
              <Skeleton active paragraph={{ rows: 2 }} />
            </Card>
          </Col>
        ))}
      </Row>
    )
  }

  return (
    <Row gutter={[16, 16]} className="fed-overview-cards">
      {cards.map((card, index) => (
        <Col xs={12} sm={12} lg={6} key={index}>
          <Card 
            className={`fed-overview-card fed-overview-card--${card.type}`}
            bodyStyle={{ textAlign: 'center' }}
          >
            {card.icon}
            <Statistic
              title={card.title}
              value={card.value}
              suffix={card.suffix}
              valueStyle={{ 
                color: 'inherit',
                fontSize: '28px',
                fontWeight: 600
              }}
            />
          </Card>
        </Col>
      ))}
    </Row>
  )
}

export default OverviewCards
