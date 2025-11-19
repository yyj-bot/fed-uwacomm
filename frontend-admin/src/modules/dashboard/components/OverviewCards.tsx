/**
 * 概览卡片组件
 * 显示系统关键指标的统计卡片
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { Card, Statistic, Skeleton } from 'antd'
import { 
  CloudServerOutlined, 
  ExperimentOutlined, 
  DatabaseOutlined, 
  TeamOutlined 
} from '@ant-design/icons'

import type { DashboardOverview } from '@/store'
import styles from '../DashboardPage.module.css'

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
      title: '运行中的水下机器人',
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
      value: overview?.participantStats?.onlineParticipants || 0,
      total: overview?.participantStats?.totalParticipants || 0,
      icon: <TeamOutlined />,
      type: 'warning' as const,
      suffix: overview?.participantStats?.totalParticipants ? `/ ${overview?.participantStats?.totalParticipants}` : undefined
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
      <div className={styles['fed-overview-cards']} style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
        {cards.map((_, index) => (
          <div key={index} style={{ flex: '1 1 calc(25% - 12px)', minWidth: '200px' }}>
            <Card className={styles['fed-overview-card']}>
              <Skeleton active paragraph={{ rows: 2 }} />
            </Card>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className={styles['fed-overview-cards']} style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
      {cards.map((card, index) => (
        <div key={index} style={{ flex: '1 1 calc(25% - 12px)', minWidth: '200px' }}>
          <Card 
            className={`${styles['fed-overview-card']} ${styles[`fed-overview-card--${card.type}`]}`}
            styles={{ 
              body: { 
                textAlign: 'center',
                background: 'white',
                padding: '20px 16px'
              },
              header: {
                background: 'white'
              }
            }}
            style={{
              background: 'white',
              borderRadius: '12px',
              border: 'none'
            }}
          >
            {card.icon}
            <Statistic
              title={card.title}
              value={card.value}
              suffix={card.suffix}
              valueStyle={{ 
                color: 'inherit',
                fontSize: '24px',
                fontWeight: 600
              }}
            />
          </Card>
        </div>
      ))}
    </div>
  )
}

export default OverviewCards
