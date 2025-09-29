/**
 * React错误边界组件
 * 用于捕获和处理组件渲染错误
 */

import React, { Component, ErrorInfo, ReactNode } from 'react'
import { Result, Button } from 'antd'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error?: Error
  errorInfo?: ErrorInfo
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(error: Error): State {
    // 更新 state 使下一次渲染能够显示降级后的 UI
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // 记录错误到控制台
    console.error('ErrorBoundary caught an error:', error, errorInfo)
    
    this.setState({
      error,
      errorInfo
    })
  }

  render() {
    if (this.state.hasError) {
      // 自定义降级 UI
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <Result
          status="error"
          title="组件渲染出错"
          subTitle="抱歉，此组件遇到了问题。请刷新页面重试。"
          extra={[
            <Button type="primary" key="refresh" onClick={() => window.location.reload()}>
              刷新页面
            </Button>,
            <Button key="reset" onClick={() => this.setState({ hasError: false })}>
              重试
            </Button>
          ]}
        >
          {process.env.NODE_ENV === 'development' && (
            <div style={{ textAlign: 'left', background: '#f5f5f5', padding: '16px', borderRadius: '4px' }}>
              <h4>错误详情（开发模式）:</h4>
              <pre style={{ fontSize: '12px', overflow: 'auto' }}>
                {this.state.error?.toString()}
                {this.state.errorInfo?.componentStack}
              </pre>
            </div>
          )}
        </Result>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary


