import React from 'react'
import { Button as AntButton, ButtonProps } from 'antd'

interface CustomButtonProps extends ButtonProps {
  variant?: 'primary' | 'secondary' | 'danger' | 'success'
}

const Button: React.FC<CustomButtonProps> = ({ variant = 'primary', ...props }) => {
  const getButtonType = () => {
    switch (variant) {
      case 'primary':
        return 'primary'
      case 'danger':
        return 'primary'
      case 'success':
        return 'primary'
      default:
        return 'default'
    }
  }

  const getDanger = () => {
    return variant === 'danger'
  }

  return (
    <AntButton
      type={getButtonType()}
      danger={getDanger()}
      {...props}
    />
  )
}

export default Button 