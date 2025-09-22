/**
 * 通用表格组件
 * 基于 Ant Design Table 封装，提供统一的表格功能和样式
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useCallback } from 'react'
import { Table as AntdTable, TableProps as AntdTableProps, Space, Tooltip } from 'antd'
import { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import { 
  ReloadOutlined, 
  SettingOutlined, 
  FullscreenOutlined,
  DownloadOutlined 
} from '@ant-design/icons'
import Button from '../Button'
import './Table.module.css'

export interface FedTableProps<T = any> extends Omit<AntdTableProps<T>, 'columns' | 'title'> {
  /** 表格列配置 */
  columns: ColumnsType<T>
  /** 表格数据 */
  dataSource?: T[]
  /** 是否显示工具栏 */
  showToolbar?: boolean
  /** 是否显示刷新按钮 */
  showRefresh?: boolean
  /** 是否显示列设置 */
  showColumnSetting?: boolean
  /** 是否显示全屏按钮 */
  showFullscreen?: boolean
  /** 是否显示导出按钮 */
  showExport?: boolean
  /** 刷新回调 */
  onRefresh?: () => void
  /** 导出回调 */
  onExport?: () => void
  /** 列设置回调 */
  onColumnSetting?: () => void
  /** 全屏回调 */
  onFullscreen?: () => void
  /** 表格标题 */
  title?: React.ReactNode
  /** 是否显示序号列 */
  showIndex?: boolean
  /** 序号列标题 */
  indexTitle?: string
  /** 序号列宽度 */
  indexWidth?: number
}

const Table = <T extends Record<string, any>>({
  columns,
  dataSource = [],
  showToolbar = true,
  showRefresh = true,
  showColumnSetting = true,
  showFullscreen = false,
  showExport = false,
  onRefresh,
  onExport,
  onColumnSetting,
  onFullscreen,
  title,
  showIndex = false,
  indexTitle = '序号',
  indexWidth = 60,
  pagination,
  loading = false,
  ...props
}: FedTableProps<T>) => {
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  // 处理分页变化
  const handlePaginationChange = useCallback((page: number, size?: number) => {
    setCurrentPage(page)
    if (size) {
      setPageSize(size)
    }
  }, [])

  // 构建最终的列配置
  const finalColumns = React.useMemo(() => {
    const cols = [...columns]
    
    // 添加序号列
    if (showIndex) {
      cols.unshift({
        title: indexTitle,
        key: 'index',
        width: indexWidth,
        fixed: 'left',
        render: (_, __, index) => {
          const current = typeof pagination === 'object' && pagination.current ? pagination.current : currentPage
          const size = typeof pagination === 'object' && pagination.pageSize ? pagination.pageSize : pageSize
          return (current - 1) * size + index + 1
        }
      })
    }
    
    return cols
  }, [columns, showIndex, indexTitle, indexWidth, pagination, currentPage, pageSize])

  // 默认分页配置
  const defaultPagination: TablePaginationConfig = {
    current: currentPage,
    pageSize: pageSize,
    total: dataSource.length,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total, range) => `显示 ${range[0]}-${range[1]} 条，共 ${total} 条`,
    pageSizeOptions: ['10', '20', '50', '100'],
    onChange: handlePaginationChange,
    onShowSizeChange: handlePaginationChange
  }

  const finalPagination = pagination === false ? false : {
    ...defaultPagination,
    ...(typeof pagination === 'object' ? pagination : {})
  }

  // 渲染工具栏
  const renderToolbar = () => {
    if (!showToolbar) return null

    const hasActions = showRefresh || showColumnSetting || showFullscreen || showExport

    if (!hasActions && !title) return null

    return (
      <div className="fed-table-toolbar">
        <div className="fed-table-toolbar-left">
          {title && <div className="fed-table-title">{title}</div>}
        </div>
        {hasActions && (
          <div className="fed-table-toolbar-right">
            <Space>
              {showRefresh && (
                <Tooltip title="刷新">
                  <Button 
                    variant="ghost" 
                    size="small" 
                    icon={<ReloadOutlined />}
                    onClick={onRefresh}
                  />
                </Tooltip>
              )}
              {showColumnSetting && (
                <Tooltip title="列设置">
                  <Button 
                    variant="ghost" 
                    size="small" 
                    icon={<SettingOutlined />}
                    onClick={onColumnSetting}
                  />
                </Tooltip>
              )}
              {showExport && (
                <Tooltip title="导出">
                  <Button 
                    variant="ghost" 
                    size="small" 
                    icon={<DownloadOutlined />}
                    onClick={onExport}
                  />
                </Tooltip>
              )}
              {showFullscreen && (
                <Tooltip title="全屏">
                  <Button 
                    variant="ghost" 
                    size="small" 
                    icon={<FullscreenOutlined />}
                    onClick={onFullscreen}
                  />
                </Tooltip>
              )}
            </Space>
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="fed-table-container">
      {renderToolbar()}
      <AntdTable<T>
        {...props}
        columns={finalColumns}
        dataSource={dataSource}
        pagination={finalPagination}
        loading={loading}
        className={`fed-table ${props.className || ''}`}
        scroll={props.scroll}
      />
    </div>
  )
}

export default Table
export type { FedTableProps as TableProps }
