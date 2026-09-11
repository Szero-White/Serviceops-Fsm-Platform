import { useQuery } from '@tanstack/react-query'
import { Alert, App, Button, Checkbox, Descriptions, Input, Modal, Space, Steps, Table, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import type { WorkOrder } from '../../../types'
import { formatCurrency, formatQuantityWithUnit } from '../../../utils/format'
import { compareNumber, compareText } from '../../../utils/tableSort'
import { paymentsApi, type CustomerAcceptancePayload } from '../../payments/api'


export function CustomerAcceptanceModal({
  workOrder,
  open,
  pending,
  onClose,
  onConfirm,
}: {
  workOrder?: WorkOrder
  open: boolean
  pending: boolean
  onClose: () => void
  onConfirm: (payload: CustomerAcceptancePayload) => void
}) {
  const [step, setStep] = useState(0)
  const [technicianReviewed, setTechnicianReviewed] = useState(false)
  const [customerConfirmed, setCustomerConfirmed] = useState(false)
  const [note, setNote] = useState('')
  const { message } = App.useApp()

  const billingQuery = useQuery({
    queryKey: ['work-order-billing', workOrder?.id],
    queryFn: () => paymentsApi.billing(workOrder!.id),
    enabled: open && Boolean(workOrder?.id),
    staleTime: 0,
  })
  const billing = billingQuery.data

  useEffect(() => {
    if (open) {
      setStep(0)
      setTechnicianReviewed(false)
      setCustomerConfirmed(false)
      setNote('')
    }
  }, [open, workOrder?.id])

  const continueToCustomer = async () => {
    if (!billing) return
    const reviewedToken = billing.reviewToken
    const result = await billingQuery.refetch()
    if (!result.data) return

    if (result.data.reviewToken !== reviewedToken) {
      setTechnicianReviewed(false)
      setCustomerConfirmed(false)
      message.warning('Phụ tùng hoặc chi phí vừa thay đổi. Kỹ thuật viên vui lòng kiểm tra lại trước khi giao khách xác nhận.')
      return
    }

    setStep(1)
    setCustomerConfirmed(false)
  }

  const submit = () => {
    if (!billing || !technicianReviewed || !customerConfirmed) return
    onConfirm({
      technicianReviewed: true,
      customerConfirmed: true,
      reviewedTotalAmount: billing.totalAmount,
      reviewToken: billing.reviewToken,
      note: note.trim() || undefined,
    })
  }

  return (
    <Modal
      title="Kiểm tra trước khi ghi nhận khách xác nhận"
      open={open}
      onCancel={onClose}
      width={760}
      destroyOnHidden
      maskClosable={!pending}
      closable={!pending}
      footer={step === 0 ? (
        <Space>
          <Button onClick={onClose} disabled={pending}>Đóng</Button>
          <Button
            type="primary"
            disabled={!technicianReviewed || !billing || billingQuery.isFetching}
            loading={billingQuery.isFetching}
            onClick={() => void continueToCustomer()}
          >
            Chuyển cho khách hàng xác nhận
          </Button>
        </Space>
      ) : (
        <Space>
          <Button onClick={() => setStep(0)} disabled={pending}>Quay lại kiểm tra</Button>
          <Button type="primary" disabled={!customerConfirmed || !billing} loading={pending} onClick={submit}>
            Ghi nhận khách xác nhận
          </Button>
        </Space>
      )}
    >
      <Steps
        size="small"
        current={step}
        items={[{ title: 'Kỹ thuật viên kiểm tra' }, { title: 'Khách hàng xác nhận' }]}
        style={{ marginBottom: 20 }}
      />

      {billingQuery.isError ? (
        <QueryErrorAlert
          title="Chưa tải được thông tin phụ tùng và chi phí"
          error={billingQuery.error}
          onRetry={() => billingQuery.refetch()}
        />
      ) : !billing || !workOrder ? (
        <Typography.Text type="secondary">Đang tải thông tin xác nhận...</Typography.Text>
      ) : step === 0 ? (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Alert
            type="warning"
            showIcon
            message="Dữ liệu chưa bị khóa"
            description="Hãy kiểm tra đúng phụ tùng thực tế, kết quả sửa chữa và toàn bộ chi phí trước khi đưa khách hàng xác nhận."
          />

          <Descriptions bordered size="small" column={1}>
            <Descriptions.Item label="Phiếu công việc">{workOrder.code} · {workOrder.summary}</Descriptions.Item>
            <Descriptions.Item label="Chẩn đoán">{workOrder.diagnosis || '—'}</Descriptions.Item>
            <Descriptions.Item label="Giải pháp / công việc đã thực hiện">{workOrder.resolution || '—'}</Descriptions.Item>
          </Descriptions>

          <Table
            rowKey="sparePartId"
            size="small"
            pagination={false}
            className="content-table"
            dataSource={billing.items}
            locale={{ emptyText: 'Không có phụ tùng thực tế tính cho khách' }}
            columns={[
              { title: 'Phụ tùng', dataIndex: 'sparePartName', sorter: (a, b) => compareText(a.sparePartName, b.sparePartName), render: (_, item) => <div><Typography.Text strong>{item.sparePartName}</Typography.Text><br /><Typography.Text type="secondary" code>{item.sparePartSku}</Typography.Text></div> },
              { title: 'Số lượng', dataIndex: 'quantity', width: 120, sorter: (a, b) => compareNumber(a.quantity, b.quantity), render: (_, item) => formatQuantityWithUnit(item.quantity, item.unit) },
              { title: 'Đơn giá', dataIndex: 'unitPrice', width: 130, align: 'right' as const, sorter: (a, b) => compareNumber(a.unitPrice, b.unitPrice), render: (value) => formatCurrency(value) },
              { title: 'Thành tiền', dataIndex: 'lineTotal', width: 140, align: 'right' as const, sorter: (a, b) => compareNumber(a.lineTotal, b.lineTotal), render: (value) => <Typography.Text strong>{formatCurrency(value)}</Typography.Text> },
            ]}
          />

          <Descriptions bordered size="small" column={1}>
            <Descriptions.Item label="Tổng phụ tùng">{formatCurrency(billing.partsTotal)}</Descriptions.Item>
            <Descriptions.Item label="Phí dịch vụ / tiền công">{formatCurrency(billing.laborFee)}</Descriptions.Item>
            <Descriptions.Item label="Phí phát sinh">{formatCurrency(billing.incidentalFee)}{billing.incidentalReason ? ` · ${billing.incidentalReason}` : ''}</Descriptions.Item>
            <Descriptions.Item label="Tổng thanh toán"><Typography.Text strong>{formatCurrency(billing.totalAmount)}</Typography.Text></Descriptions.Item>
          </Descriptions>

          <Checkbox checked={technicianReviewed} onChange={(event) => setTechnicianReviewed(event.target.checked)}>
            Tôi đã kiểm tra kết quả sửa chữa, phụ tùng thực tế và toàn bộ chi phí; thông tin trên là chính xác để bàn giao khách hàng.
          </Checkbox>
        </Space>
      ) : (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message={`Thông tin xác nhận của ${workOrder.customerName}`}
            description="Vui lòng để khách hàng kiểm tra nội dung dưới đây trước khi ghi nhận xác nhận."
          />

          <Descriptions bordered size="small" column={1}>
            <Descriptions.Item label="Công việc">{workOrder.summary}</Descriptions.Item>
            <Descriptions.Item label="Kết quả sửa chữa">{workOrder.resolution || '—'}</Descriptions.Item>
          </Descriptions>

          <Table
            rowKey="sparePartId"
            size="small"
            pagination={false}
            className="content-table"
            dataSource={billing.items}
            locale={{ emptyText: 'Không có phụ tùng tính cho khách' }}
            columns={[
              { title: 'Phụ tùng', dataIndex: 'sparePartName', sorter: (a, b) => compareText(a.sparePartName, b.sparePartName) },
              { title: 'Số lượng', dataIndex: 'quantity', width: 120, sorter: (a, b) => compareNumber(a.quantity, b.quantity), render: (_, item) => formatQuantityWithUnit(item.quantity, item.unit) },
              { title: 'Thành tiền', dataIndex: 'lineTotal', width: 150, align: 'right' as const, sorter: (a, b) => compareNumber(a.lineTotal, b.lineTotal), render: (value) => formatCurrency(value) },
            ]}
          />

          <Descriptions bordered size="small" column={1}>
            <Descriptions.Item label="Phụ tùng">{formatCurrency(billing.partsTotal)}</Descriptions.Item>
            <Descriptions.Item label="Tiền công">{formatCurrency(billing.laborFee)}</Descriptions.Item>
            <Descriptions.Item label="Chi phí phát sinh">{formatCurrency(billing.incidentalFee)}</Descriptions.Item>
            <Descriptions.Item label="Tổng thanh toán">
              <Typography.Title level={4} style={{ margin: 0 }}>{formatCurrency(billing.totalAmount)}</Typography.Title>
            </Descriptions.Item>
          </Descriptions>

          <Checkbox checked={customerConfirmed} onChange={(event) => setCustomerConfirmed(event.target.checked)}>
            Khách hàng đã trực tiếp kiểm tra kết quả sửa chữa, phụ tùng và tổng chi phí trên, đồng thời đồng ý xác nhận.
          </Checkbox>

          <Input.TextArea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            placeholder="Ghi chú xác nhận (không bắt buộc)"
            maxLength={1000}
            showCount
            rows={2}
          />

          <Typography.Text type="secondary">
            Sau khi xác nhận, phụ tùng và chi phí sẽ được khóa để phục vụ đối soát và thanh toán.
          </Typography.Text>
        </Space>
      )}
    </Modal>
  )
}
