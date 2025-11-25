/**
 * 系统管理页面
 * 提供系统日志查看、监控面板、配置管理等功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { 
  Typography, 
  Space
} from 'antd'
import { 
  FileTextOutlined
} from '@ant-design/icons'

import { LogList } from './components'

const { Title } = Typography

const SystemPage: React.FC = () => {

  return (
    <div style={{ 
      padding: '0 24px',
      height: '100%',
      display: 'flex',
      flexDirection: 'column'
    }}>
      <div style={{ margin: '16px 0 24px', flexShrink: 0 }}>
        <Title level={2}>
          <Space>
            <FileTextOutlined />
            系统日志
          </Space>
        </Title>
      </div>

      <div style={{ flex: 1, minHeight: 0, height: '100%' }}>
        <LogList height="100%" />
      </div>
    </div>
  )
}

export default SystemPage
