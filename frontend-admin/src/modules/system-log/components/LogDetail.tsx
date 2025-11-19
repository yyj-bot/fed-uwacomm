/**
 * 日志详情组件
 * 显示单条日志的详细信息
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { 
  Descriptions, 
  Tag, 
  Typography, 
  Card, 
  Spin,
  Empty,
  Space
} from 'antd'
import { 
  ClockCircleOutlined,
  TagOutlined,
  AppstoreOutlined,
  MessageOutlined,
  CloudServerOutlined,
  TagsOutlined
} from '@ant-design/icons'
import type { SystemLog } from '@/types'
import { formatDateTime } from '@/utils'

const { Text, Paragraph } = Typography

// 日志级别颜色映射
const LOG_LEVEL_COLORS = {
  DEBUG: 'default',
  INFO: 'blue', 
  WARN: 'orange',
  ERROR: 'red'
} as const

// 日志类别颜色映射  
const LOG_CATEGORY_COLORS = {
  SYSTEM: 'blue',
  USER: 'green', 
  VM: 'purple',
  TASK: 'orange',
  DATA: 'cyan',
  MODEL: 'magenta',
  SECURITY: 'red',
  PERFORMANCE: 'gold'
} as const

interface LogDetailProps {
  log: SystemLog | null
  loading?: boolean
}

const LogDetail: React.FC<LogDetailProps> = ({ log, loading = false }) => {
  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '40px 0' }}>
        <Spin size="large" />
        <div style={{ marginTop: 16 }}>加载日志详情中...</div>
      </div>
    )
  }

  if (!log) {
    return (
      <Empty 
        description="暂无日志详情"
        style={{ padding: '40px 0' }}
      />
    )
  }

  return (
    <div>
      {/* 基本信息 */}
      <Card 
        title={
          <Space>
            <MessageOutlined />
            基本信息
          </Space>
        }
        size="small"
        style={{ marginBottom: 16 }}
      >
        <Descriptions column={2} size="small">
          <Descriptions.Item 
            label={
              <Space>
                <TagOutlined />
                日志ID
              </Space>
            }
          >
            <Text code>{log.logId}</Text>
          </Descriptions.Item>
          
          <Descriptions.Item 
            label={
              <Space>
                <ClockCircleOutlined />
                创建时间
              </Space>
            }
          >
            {formatDateTime(log.createdAt)}
          </Descriptions.Item>
          
          <Descriptions.Item label="日志级别">
            <Tag color={LOG_LEVEL_COLORS[log.level as keyof typeof LOG_LEVEL_COLORS] || 'default'}>
              {log.level}
            </Tag>
          </Descriptions.Item>
          
          <Descriptions.Item label="日志类别">
            <Tag color={LOG_CATEGORY_COLORS[log.category as keyof typeof LOG_CATEGORY_COLORS] || 'default'}>
              {log.category}
            </Tag>
          </Descriptions.Item>
          
          {log.vmId && (
            <Descriptions.Item 
              label={
                <Space>
                  <CloudServerOutlined />
                  水下机器人ID
                </Space>
              }
            >
              <Text code>{log.vmId}</Text>
            </Descriptions.Item>
          )}
          
          {log.taskId && (
            <Descriptions.Item 
              label={
                <Space>
                  <TagsOutlined />
                  任务ID
                </Space>
              }
            >
              <Text code>{log.taskId}</Text>
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>

      {/* 日志消息 */}
      <Card 
        title={
          <Space>
            <MessageOutlined />
            日志消息
          </Space>
        }
        size="small"
        style={{ marginBottom: 16 }}
      >
        <Paragraph>
          <Text>{log.message}</Text>
        </Paragraph>
      </Card>

      {/* 详细信息 */}
      {log.details && (
        <Card 
          title={
            <Space>
              <AppstoreOutlined />
              详细信息
            </Space>
          }
          size="small"
        >
          <div style={{ 
            backgroundColor: '#f5f5f5', 
            padding: '12px', 
            borderRadius: '4px',
            fontFamily: 'Monaco, Menlo, "Ubuntu Mono", monospace',
            fontSize: '12px',
            maxHeight: '300px',
            overflow: 'auto'
          }}>
            <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
              {typeof log.details === 'string' 
                ? log.details 
                : JSON.stringify(log.details, null, 2)
              }
            </pre>
          </div>
        </Card>
      )}
    </div>
  )
}

export { LogDetail }
export default LogDetail
