import { EditOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Descriptions, Form, Input, InputNumber, Modal, Space, Table, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import type { UserRole, WorkOrder } from '../../../types'
import { formatCurrency, formatQuantityWithUnit } from '../../../utils/format'
import { paymentsApi } from '../api'

type BillingFormValues = {
  laborFee: number
  incidentalFee: number
  incidentalReason?: string
}

export function WorkOrderBillingPanel({ workOrder, role }: { workOrder: WorkOrder; role?: UserRole }) {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const [form] = Form.useForm<BillingFormValues>()
  const [editing, setEditing] = useState(false)
  const query = useQuery({
    queryKey: ['work-order-billing', workOrder.id],
    queryFn: () => paymentsApi.billing(workOrder.id),
    enabled: Boolean(role && ['OWNER', 'CUSTOMER_SERVICE', 'TECHNICIAN'].includes(role)),
  })
  const billing = query.data
  const canEdit = role === 'TECHNICIAN'
    && !billing?.frozen
    && ['IN_PROGRESS', 'WAITING_FOR_PARTS', 'COMPLETED', 'REOPENED'].includes(workOrder.status)

  useEffect(() => {
    if (editing && billing) {
      form.setFieldsValue({
        laborFee: billing.laborFee,
        incidentalFee: billing.incidentalFee,
        incidentalReason: billing.incidentalReason,
      })
    }
  }, [billing, editing, form])

  const update = useMutation({
    mutationFn: (values: BillingFormValues) => paymentsApi.updateBilling(workOrder.id, {
      laborFee: values.laborFee ?? 0,
      incidentalFee: values.incidentalFee ?? 0,
      incidentalReason: values.incidentalReason?.trim() || undefined,
    }),
    onSuccess: () => {
      message.success('Đã cập nhật chi phí dịch vụ')
      setEditing(false)
      queryClient.invalidateQueries({ queryKey: ['work-order-billing', workOrder.id] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  if (query.isError) {
    return <QueryErrorAlert title="Chưa tải được chi phí dịch vụ" error={query.error} onRetry={() => query.refetch()} />
  }
  if (!billing) return <Typography.Text type="secondary">Đang tải chi phí...</Typography.Text>

  return (
    <Space orientation="vertical" size={16} style={{ width: '100%' }}>
      <div className="section-heading-row">
        <div>
          <Typography.Title level={5} style={{ margin: 0 }}>Chi phí khách xác nhận</Typography.Title>
          <Typography.Text type="secondary">
            {billing.frozen
              ? 'Đã khóa tại thời điểm khách xác nhận. Giá phụ tùng sau này không làm thay đổi số tiền này.'
              : 'Phụ tùng lấy theo số lượng thực tế đã dùng; kỹ thuật viên chỉ nhập phí dịch vụ và phí phát sinh.'}
          </Typography.Text>
        </div>
        {canEdit ? <Button icon={<EditOutlined />} onClick={() => setEditing(true)}>Cập nhật chi phí</Button> : null}
      </div>

      <Table
        rowKey="sparePartId"
        size="small"
        pagination={false}
        dataSource={billing.items}
        locale={{ emptyText: 'Không có phụ tùng thực tế tính cho khách' }}
        columns={[
          { title: 'Phụ tùng', render: (_, item) => <div><Typography.Text strong>{item.sparePartName}</Typography.Text><br /><Typography.Text type="secondary" code>{item.sparePartSku}</Typography.Text></div> },
          { title: 'Số lượng', width: 120, render: (_, item) => formatQuantityWithUnit(item.quantity, item.unit) },
          { title: 'Đơn giá', width: 130, align: 'right' as const, render: (_, item) => formatCurrency(item.unitPrice) },
          { title: 'Thành tiền', width: 140, align: 'right' as const, render: (_, item) => <Typography.Text strong>{formatCurrency(item.lineTotal)}</Typography.Text> },
        ]}
      />

      <Descriptions bordered size="small" column={1}>
        <Descriptions.Item label="Tổng phụ tùng">{formatCurrency(billing.partsTotal)}</Descriptions.Item>
        <Descriptions.Item label="Phí dịch vụ / tiền công">{formatCurrency(billing.laborFee)}</Descriptions.Item>
        <Descriptions.Item label="Phí phát sinh">
          {formatCurrency(billing.incidentalFee)}{billing.incidentalReason ? ` · ${billing.incidentalReason}` : ''}
        </Descriptions.Item>
        <Descriptions.Item label="Tổng khách xác nhận"><Typography.Text strong>{formatCurrency(billing.totalAmount)}</Typography.Text></Descriptions.Item>
      </Descriptions>

      {billing.frozen ? (
        <Typography.Text type="secondary">Khách được ghi nhận xác nhận bởi {billing.acceptedByDisplayName ?? 'kỹ thuật viên'}.</Typography.Text>
      ) : workOrder.status === 'COMPLETED' && role === 'TECHNICIAN' ? (
        <Typography.Text type="warning">Kiểm tra chi phí trước khi bấm “Ghi nhận khách xác nhận”. Sau bước đó số tiền sẽ được khóa.</Typography.Text>
      ) : null}

      <Modal
        title="Cập nhật chi phí dịch vụ"
        open={editing}
        onCancel={() => setEditing(false)}
        onOk={() => form.submit()}
        okText="Lưu chi phí"
        confirmLoading={update.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={(values) => update.mutate(values)}>
          <Form.Item label="Phí dịch vụ / tiền công" name="laborFee" rules={[{ required: true, message: 'Nhập phí dịch vụ' }]}>
            <InputNumber min={0} precision={0} style={{ width: '100%' }} addonAfter="₫" />
          </Form.Item>
          <Form.Item label="Phí phát sinh" name="incidentalFee" rules={[{ required: true, message: 'Nhập phí phát sinh hoặc 0' }]}>
            <InputNumber min={0} precision={0} style={{ width: '100%' }} addonAfter="₫" />
          </Form.Item>
          <Form.Item
            noStyle
            shouldUpdate={(previous, current) => previous.incidentalFee !== current.incidentalFee}
          >
            {({ getFieldValue }) => Number(getFieldValue('incidentalFee') ?? 0) > 0 ? (
              <Form.Item label="Lý do phí phát sinh" name="incidentalReason" rules={[{ required: true, whitespace: true, message: 'Nhập lý do phí phát sinh' }, { max: 500 }]}>
                <Input.TextArea rows={3} maxLength={500} showCount placeholder="Nhập lý do thực tế để khách và CSKH dễ đối chiếu." />
              </Form.Item>
            ) : null}
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
