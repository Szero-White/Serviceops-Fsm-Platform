import { CloudUploadOutlined, SaveOutlined } from '@ant-design/icons'
import type { UploadRequestOption } from '@rc-component/upload/es/interface'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Card, Form, Input, Space, Typography, Upload } from 'antd'
import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import type { AttachmentItem } from '../../../types'
import { attachmentsApi } from '../../attachments/api'
import { useAuth } from '../../auth/AuthContext'
import { paymentsApi } from '../api'
import { PaymentQrImage } from '../components/PaymentQrImage'

type ProfileFormValues = { bankName: string; accountHolder: string; accountNumber: string }

export function PaymentSettingsPage() {
  const { user } = useAuth()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const [form] = Form.useForm<ProfileFormValues>()
  const [newQr, setNewQr] = useState<AttachmentItem>()
  const query = useQuery({ queryKey: ['company-payment-profile'], queryFn: paymentsApi.companyProfile })
  const profile = query.data

  useEffect(() => {
    if (profile) form.setFieldsValue({ bankName: profile.bankName, accountHolder: profile.accountHolder, accountNumber: profile.accountNumber })
  }, [form, profile])

  const save = useMutation({
    mutationFn: (values: ProfileFormValues) => paymentsApi.updateCompanyProfile({
      bankName: values.bankName.trim(),
      accountHolder: values.accountHolder.trim(),
      accountNumber: values.accountNumber.trim(),
      qrAttachmentId: newQr?.id ?? profile?.qrAttachmentId,
    }),
    onSuccess: () => {
      message.success('Đã cập nhật tài khoản nhận thanh toán của công ty')
      setNewQr(undefined)
      queryClient.invalidateQueries({ queryKey: ['company-payment-profile'] })
      queryClient.invalidateQueries({ queryKey: ['audit'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const uploadQr = async (options: UploadRequestOption) => {
    try {
      const attachment = await attachmentsApi.upload('COMPANY_PAYMENT_PROFILE', user!.tenantId, options.file as File)
      setNewQr(attachment)
      message.success('Đã tải ảnh QR. Bấm Lưu cấu hình để sử dụng ảnh này.')
      options.onSuccess?.({})
    } catch (error) {
      message.error(apiErrorMessage(error))
      options.onError?.(error as Error)
    }
  }

  return (
    <div className="page-shell">
      <PageHeader eyebrow="Thiết lập thanh toán" title="Tài khoản nhận thanh toán" description="Chỉ chủ sở hữu cấu hình tài khoản và QR của công ty. Kỹ thuật viên chỉ được xem để hướng dẫn khách thanh toán." />
      {query.isError ? <QueryErrorAlert title="Chưa tải được cấu hình thanh toán" error={query.error} onRetry={() => query.refetch()} /> : null}
      <Card>
        <Space align="start" size={28} wrap style={{ width: '100%' }}>
          <div style={{ minWidth: 240 }}>
            <Typography.Title level={5}>QR công ty</Typography.Title>
            <PaymentQrImage attachmentId={newQr?.id ?? profile?.qrAttachmentId} />
            <div style={{ marginTop: 12 }}>
              <Upload accept="image/*" customRequest={uploadQr} showUploadList={false}>
                <Button icon={<CloudUploadOutlined />}>Chọn ảnh QR</Button>
              </Upload>
            </div>
          </div>
          <Form form={form} layout="vertical" style={{ flex: 1, minWidth: 320 }} onFinish={(values) => save.mutate(values)}>
            <Form.Item label="Tên ngân hàng" name="bankName" rules={[{ required: true, whitespace: true }, { max: 150 }]}><Input placeholder="Ví dụ: Vietcombank" /></Form.Item>
            <Form.Item label="Chủ tài khoản" name="accountHolder" rules={[{ required: true, whitespace: true }, { max: 180 }]}><Input /></Form.Item>
            <Form.Item label="Số tài khoản" name="accountNumber" rules={[{ required: true, whitespace: true }, { max: 80 }]}><Input /></Form.Item>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={save.isPending}>Lưu cấu hình</Button>
          </Form>
        </Space>
      </Card>
    </div>
  )
}
