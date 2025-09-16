/**
 * 通用加载组件
 * 提供多种加载样式和状态
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { Spin, SpinProps } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import './Loading.module.css'

export interface LoadingProps extends SpinProps {
  /** 加载类型 */
  type?: 'default' | 'dots' | 'spinner' | 'pulse'
  /** 加载文本 */
  text?: string
  /** 是否显示遮罩 */
  overlay?: boolean
  /** 自定义类名 */
  className?: string
}

const Loading: React.FC<LoadingProps> = ({
  type = 'default',
  text,
  overlay = false,
  className = '',
  children,
  ...props
}) => {
  const getIndicator = () => {
    switch (type) {
      case 'spinner':
        return <LoadingOutlined style={{ fontSize: 24 }} spin />
      case 'dots':
        return (
          <div className="fed-loading-dots">
            <div className="fed-dot"></div>
            <div className="fed-dot"></div>
            <div className="fed-dot"></div>
          </div>
        )
      case 'pulse':
        return <div className="fed-loading-pulse"></div>
      default:
        return undefined
    }
  }

  const loadingClassName = `fed-loading fed-loading--${type} ${overlay ? 'fed-loading--overlay' : ''} ${className}`

  if (children) {
    return (
      <Spin
        {...props}
        indicator={getIndicator()}
        tip={text}
        className={loadingClassName}
      >
        {children}
      </Spin>
    )
  }

  return (
    <div className={loadingClassName}>
      <Spin
        {...props}
        indicator={getIndicator()}
        tip={text}
      />
    </div>
  )
}

export default Loading
