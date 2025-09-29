/**
 * 图表错误边界组件
 * 专门处理图表组件的错误
 */

import React from 'react'
import { Alert } from 'antd'

interface ChartErrorBoundaryState {
  hasError: boolean
  error?: Error
}

interface ChartErrorBoundaryProps {
  children: React.ReactNode
  fallback?: React.ReactNode
}

class ChartErrorBoundary extends React.Component<ChartErrorBoundaryProps, ChartErrorBoundaryState> {
  constructor(props: ChartErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(error: Error): ChartErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Chart Error Boundary caught an error:', error, errorInfo)
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <Alert
          message="图表加载失败"
          description="图表组件遇到错误，请刷新页面重试"
          type="error"
          showIcon
          action={
            <button 
              onClick={() => this.setState({ hasError: false, error: undefined })}
              style={{
                border: 'none',
                background: 'transparent',
                color: '#1890ff',
                cursor: 'pointer',
                textDecoration: 'underline'
              }}
            >
              重试
            </button>
          }
        />
      )
    }

    return this.props.children
  }
}

export default ChartErrorBoundary

