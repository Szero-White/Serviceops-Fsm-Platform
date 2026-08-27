import { useQuery } from '@tanstack/react-query'
import { Empty, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { attachmentsApi } from '../../attachments/api'

export function PaymentQrImage({ attachmentId }: { attachmentId?: string }) {
  const [url, setUrl] = useState<string>()
  const query = useQuery({
    queryKey: ['payment-qr', attachmentId],
    queryFn: () => attachmentsApi.download(attachmentId!),
    enabled: Boolean(attachmentId),
    staleTime: 60_000,
  })

  useEffect(() => {
    if (!query.data) {
      setUrl(undefined)
      return
    }
    const next = URL.createObjectURL(query.data)
    setUrl(next)
    return () => URL.revokeObjectURL(next)
  }, [query.data])

  if (!attachmentId) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa cấu hình ảnh QR" />
  if (query.isLoading) return <Spin size="small" />
  if (!url) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa tải được ảnh QR" />
  return <img src={url} alt="QR thanh toán công ty" style={{ width: 220, maxWidth: '100%', borderRadius: 12 }} />
}
