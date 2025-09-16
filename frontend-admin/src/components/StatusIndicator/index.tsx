/**
 * 状态指示器组件
 * 用于显示各种状态（在线/离线、运行/停止等）
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { Badge, Tooltip } from 'antd'
import { 
  CheckCircleOutlined, 
  CloseCircleOutlined, 
  ExclamationCircleOutlined,
  LoadingOutlined,
  MinusCircleOutlined
} from '@ant-design/icons'
import './StatusIndicator.module.css'

export type StatusType = 
  | 'online' | 'offline' 
  | 'running' | 'stopped' | 'error' | 'pending' | 'loading'
  | 'success' | 'warning' | 'danger' | 'info'
  | 'active' | 'inactive'

export interface StatusIndicatorProps {
  /** 状态类型 */
  status: StatusType
  /** 状态文本 */
  text?: string
  /** 显示方式 */
  variant?: 'dot' | 'badge' | 'icon' | 'text'
  /** 大小 */
  size?: 'small' | 'middle' | 'large'
  /** 是否显示动画 */
  animated?: boolean
  /** 自定义颜色 */
  color?: string
  /** 工具提示 */
  tooltip?: string
  /** 自定义类名 */
  className?: string
}

const StatusIndicator: React.FC<StatusIndicatorProps> = ({
  status,
  text,
  variant = 'dot',
  size = 'middle',
  animated = false,
  color,
  tooltip,
  className = ''
}) => {
  // 状态配置映射
  const statusConfig = {
    online: { color: '#52c41a', icon: CheckCircleOutlined, text: '在线' },
    offline: { color: '#ff4d4f', icon: CloseCircleOutlined, text: '离线' },
    running: { color: '#1890ff', icon: LoadingOutlined, text: '运行中' },
    stopped: { color: '#d9d9d9', icon: MinusCircleOutlined, text: '已停止' },
    error: { color: '#ff4d4f', icon: CloseCircleOutlined, text: '错误' },
    pending: { color: '#faad14', icon: ExclamationCircleOutlined, text: '等待中' },
    loading: { color: '#1890ff', icon: LoadingOutlined, text: '加载中' },
    success: { color: '#52c41a', icon: CheckCircleOutlined, text: '成功' },
    warning: { color: '#faad14', icon: ExclamationCircleOutlined, text: '警告' },
    danger: { color: '#ff4d4f', icon: CloseCircleOutlined, text: '危险' },
    info: { color: '#1890ff', icon: ExclamationCircleOutlined, text: '信息' },
    active: { color: '#52c41a', icon: CheckCircleOutlined, text: '激活' },
    inactive: { color: '#d9d9d9', icon: MinusCircleOutlined, text: '未激活' }
  }

  const config = statusConfig[status]
  const finalColor = color || config.color
  const finalText = text || config.text
  const IconComponent = config.icon

  // 构建类名
  const containerClassName = `fed-status-indicator fed-status-indicator--${variant} fed-status-indicator--${size} ${animated ? 'fed-status-indicator--animated' : ''} ${className}`

  // 渲染不同变体
  const renderIndicator = () => {
    switch (variant) {
      case 'dot':
        return (
          <div className={containerClassName}>
            <Badge 
              color={finalColor} 
              text={finalText}
              className="fed-status-badge"
            />
          </div>
        )

      case 'badge':
        return (
          <div className={containerClassName}>
            <Badge 
              status={status as any} 
              text={finalText}
              className="fed-status-badge"
            />
          </div>
        )

      case 'icon':
        return (
          <div className={containerClassName}>
            <IconComponent 
              style={{ color: finalColor }} 
              className={`fed-status-icon ${animated && (status === 'loading' || status === 'running') ? 'fed-status-icon--spinning' : ''}`}
            />
            {finalText && <span className="fed-status-text">{finalText}</span>}
          </div>
        )

      case 'text':
        return (
          <div className={containerClassName}>
            <span 
              className="fed-status-text-only"
              style={{ color: finalColor }}
            >
              {finalText}
            </span>
          </div>
        )

      default:
        return (
          <div className={containerClassName}>
            <Badge color={finalColor} text={finalText} />
          </div>
        )
    }
  }

  const indicator = renderIndicator()

  // 如果有工具提示，包装在 Tooltip 中
  if (tooltip) {
    return (
      <Tooltip title={tooltip}>
        {indicator}
      </Tooltip>
    )
  }

  return indicator
}

export default StatusIndicator
