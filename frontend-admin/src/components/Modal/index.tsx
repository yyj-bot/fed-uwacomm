/**
 * 通用弹窗组件
 * 基于 Ant Design Modal 封装，提供统一的弹窗功能和样式
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { Modal as AntdModal, ModalProps as AntdModalProps } from 'antd'
import { CloseOutlined } from '@ant-design/icons'
import Button from '../Button'
import './Modal.module.css'

export interface ModalProps extends AntdModalProps {
  /** 弹窗类型 */
  type?: 'default' | 'form' | 'confirm' | 'info' | 'success' | 'warning' | 'error'
  /** 是否显示关闭图标 */
  showCloseIcon?: boolean
  /** 是否可通过点击遮罩关闭 */
  maskClosable?: boolean
  /** 是否可通过 ESC 关闭 */
  keyboard?: boolean
  /** 确认按钮文本 */
  okText?: string
  /** 取消按钮文本 */
  cancelText?: string
  /** 确认按钮加载状态 */
  confirmLoading?: boolean
  /** 是否显示取消按钮 */
  showCancelButton?: boolean
  /** 自定义底部按钮 */
  footer?: React.ReactNode | null
  /** 弹窗尺寸 */
  size?: 'small' | 'middle' | 'large' | 'fullscreen'
}

const Modal: React.FC<ModalProps> = ({
  type = 'default',
  showCloseIcon = true,
  maskClosable = true,
  keyboard = true,
  okText = '确定',
  cancelText = '取消',
  confirmLoading = false,
  showCancelButton = true,
  footer,
  size = 'middle',
  width,
  className,
  children,
  onOk,
  onCancel,
  ...props
}) => {
  // 根据尺寸确定宽度
  const getModalWidth = () => {
    if (width) return width
    
    switch (size) {
      case 'small':
        return 400
      case 'middle':
        return 600
      case 'large':
        return 800
      case 'fullscreen':
        return '100vw'
      default:
        return 600
    }
  }

  // 根据类型确定图标
  const getModalIcon = () => {
    switch (type) {
      case 'confirm':
        return null // Ant Design 会自动添加确认图标
      case 'info':
        return null
      case 'success':
        return null
      case 'warning':
        return null
      case 'error':
        return null
      default:
        return null
    }
  }

  // 构建类名
  const modalClassName = `fed-modal fed-modal--${type} fed-modal--${size} ${className || ''}`

  // 渲染自定义底部
  const renderFooter = () => {
    if (footer === null) return null
    if (footer) return footer

    return (
      <div className="fed-modal-footer">
        {showCancelButton && (
          <Button
            variant="secondary"
            onClick={onCancel}
          >
            {cancelText}
          </Button>
        )}
        <Button
          variant="primary"
          loading={confirmLoading}
          onClick={onOk}
        >
          {okText}
        </Button>
      </div>
    )
  }

  // 全屏模式的特殊处理
  if (size === 'fullscreen') {
    return (
      <AntdModal
        {...props}
        open={props.open}
        onOk={onOk}
        onCancel={onCancel}
        width="100vw"
        style={{ top: 0, paddingBottom: 0, maxWidth: '100vw' }}
        bodyStyle={{ height: 'calc(100vh - 110px)', padding: 0 }}
        className={modalClassName}
        maskClosable={maskClosable}
        keyboard={keyboard}
        closeIcon={showCloseIcon ? <CloseOutlined /> : null}
        footer={renderFooter()}
      >
        <div className="fed-modal-fullscreen-content">
          {children}
        </div>
      </AntdModal>
    )
  }

  return (
    <AntdModal
      {...props}
      width={getModalWidth()}
      className={modalClassName}
      maskClosable={maskClosable}
      keyboard={keyboard}
      closeIcon={showCloseIcon ? <CloseOutlined /> : null}
      footer={renderFooter()}
      onOk={onOk}
      onCancel={onCancel}
    >
      {children}
    </AntdModal>
  )
}

// 静态方法
const ModalWithMethods = Modal as typeof Modal & {
  confirm: typeof AntdModal.confirm
  info: typeof AntdModal.info
  success: typeof AntdModal.success
  warning: typeof AntdModal.warning
  error: typeof AntdModal.error
  destroyAll: typeof AntdModal.destroyAll
}

ModalWithMethods.confirm = AntdModal.confirm
ModalWithMethods.info = AntdModal.info
ModalWithMethods.success = AntdModal.success
ModalWithMethods.warning = AntdModal.warning
ModalWithMethods.error = AntdModal.error
ModalWithMethods.destroyAll = AntdModal.destroyAll

export default ModalWithMethods
