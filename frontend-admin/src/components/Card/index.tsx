/**
 * 通用卡片组件
 * 基于 Ant Design Card 封装，提供统一的卡片样式
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { Card as AntdCard, CardProps as AntdCardProps } from 'antd'
import classNames from 'classnames'
import './Card.module.css'

export interface CardProps extends AntdCardProps {
  /** 卡片变体 */
  variant?: 'default' | 'outlined' | 'filled' | 'elevated'
  /** 是否显示阴影 */
  shadow?: 'none' | 'small' | 'medium' | 'large'
  /** 是否可悬停 */
  hoverable?: boolean
  /** 自定义类名 */
  className?: string
}

const Card: React.FC<CardProps> = ({
  variant = 'default',
  shadow = 'small',
  hoverable = false,
  className,
  children,
  ...props
}) => {
  const cardClassName = classNames(
    'fed-card',
    `fed-card--${variant}`,
    `fed-card--shadow-${shadow}`,
    {
      'fed-card--hoverable': hoverable
    },
    className
  )

  return (
    <AntdCard
      {...props}
      hoverable={hoverable}
      className={cardClassName}
    >
      {children}
    </AntdCard>
  )
}

export default Card
