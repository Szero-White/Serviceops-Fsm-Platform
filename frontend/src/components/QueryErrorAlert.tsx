import { Alert, Button } from 'antd'
import { apiErrorMessage } from '../api/http'

export function QueryErrorAlert({
  title,
  error,
  onRetry,
}: {
  title: string
  error: unknown
  onRetry: () => void
}) {
  return (
    <Alert
      showIcon
      type="error"
      message={title}
      description={apiErrorMessage(error)}
      action={<Button size="small" onClick={onRetry}>Thử lại</Button>}
      style={{ marginBottom: 14 }}
    />
  )
}
