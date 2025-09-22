/**
 * 通用按钮组件
 * 基于 Ant Design Button 封装，提供统一的按钮样式和行为
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { Button as AntdButton, ButtonProps as AntdButtonProps } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import classNames from 'classnames'
import './Button.module.css'

export interface ButtonProps extends Omit<AntdButtonProps, 'loading' | 'variant' | 'iconPosition'> {
  /** 按钮变体 */
  variant?: 'primary' | 'secondary' | 'success' | 'warning' | 'danger' | 'ghost'
  /** 按钮尺寸 */
  size?: 'small' | 'middle' | 'large'
  /** 是否显示加载状态 */
  loading?: boolean
  /** 加载状态文本 */
  loadingText?: string
  /** 是否为块级按钮 */
  block?: boolean
  /** 自定义类名 */
  className?: string
  /** 图标位置 */
  iconPosition?: 'left' | 'right'
}

const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'middle',
  loading = false,
  loadingText,
  block = false,
  className,
  iconPosition = 'left',
  children,
  disabled,
  icon,
  ...props
}) => {
  // 根据变体确定按钮类型
  const getButtonType = (): 'primary' | 'default' | 'dashed' | 'text' | 'link' => {
    switch (variant) {
      case 'primary':
        return 'primary'
      case 'danger':
        return 'primary'
      case 'ghost':
        return 'default'
      default:
        return 'default'
    }
  }

  // 根据变体确定是否为危险按钮
  const isDanger = variant === 'danger'

  // 构建类名
  const buttonClassName = classNames(
    'fed-button',
    `fed-button--${variant}`,
    `fed-button--${size}`,
    {
      'fed-button--loading': loading,
      'fed-button--block': block,
      'fed-button--icon-right': iconPosition === 'right'
    },
    className
  )

  // 渲染加载图标
  const renderLoadingIcon = () => {
    if (loading) {
      return <LoadingOutlined />
    }
    return icon
  }

  // 渲染按钮内容
  const renderContent = () => {
    if (loading && loadingText) {
      return loadingText
    }
    return children
  }

  return (
    <AntdButton
      {...props}
      type={getButtonType()}
      size={size}
      loading={false} // 使用自定义加载状态
      block={block}
      danger={isDanger}
      disabled={disabled || loading}
      className={buttonClassName}
      icon={renderLoadingIcon()}
    >
      {renderContent()}
    </AntdButton>
  )
}

export default Button