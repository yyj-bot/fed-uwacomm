/**
 * 通用组件统一导出文件
 * 提供项目中所有通用 UI 组件的统一访问入口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// ==================== 基础组件 ====================
export { default as Button } from './Button'
export { default as Table } from './Table'
export { default as Modal } from './Modal'
export { default as Card } from './Card'
export { default as Loading } from './Loading'

// ==================== 业务组件 ====================
export { default as StatusIndicator } from './StatusIndicator'
export { ErrorBoundary } from './ErrorBoundary'

// ==================== 组件类型导出 ====================
export type { ButtonProps } from './Button'
export type { TableProps } from './Table'
export type { ModalProps } from './Modal'
export type { CardProps } from './Card'
export type { LoadingProps } from './Loading'
export type { StatusIndicatorProps, StatusType } from './StatusIndicator'

// ==================== 组件配置 ====================
export interface ComponentConfig {
  // 默认主题
  theme?: 'light' | 'dark'
  // 默认尺寸
  size?: 'small' | 'middle' | 'large'
  // 国际化
  locale?: string
}

export const defaultComponentConfig: ComponentConfig = {
  theme: 'light',
  size: 'middle',
  locale: 'zh-CN'
}