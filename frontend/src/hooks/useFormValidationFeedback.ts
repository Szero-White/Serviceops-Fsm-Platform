import { App } from 'antd'
import type { FormProps } from 'antd'

function actionableValidationMessage(value: string) {
  const normalized = value.trim()
  if (!normalized) return 'Vui lòng kiểm tra các trường bắt buộc'

  if (normalized.startsWith('Nhập ') || normalized.startsWith('Chọn ')) {
    return `Vui lòng ${normalized.charAt(0).toLowerCase()}${normalized.slice(1)}`
  }

  return normalized
}

export function useFormValidationFeedback<Values extends object = Record<string, unknown>>() {
  const { message } = App.useApp()

  const handleFinishFailed: NonNullable<FormProps<Values>['onFinishFailed']> = ({ errorFields }) => {
    const firstError = errorFields[0]?.errors?.[0]
    const detail = typeof firstError === 'string' ? firstError : ''
    void message.warning(actionableValidationMessage(detail))
  }

  return handleFinishFailed
}
