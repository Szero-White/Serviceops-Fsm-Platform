import { Alert, Form, Input, InputNumber, Modal, Space, Table } from 'antd'
import type { FormInstance } from 'antd'
import type { SparePart, SparePartImportResult, SparePartImportRowResult } from '../../../types'
import { MetaBadge } from '../../../components/PresentationBadge'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'
import { compareNumber, compareText } from '../../../utils/tableSort'
import { formatCompactDecimalInput, formatQuantityWithUnit } from '../../../utils/format'

type CreateSparePartModalProps = {
  open: boolean
  form: FormInstance
  loading: boolean
  onCancel: () => void
  onSubmit: (values: Record<string, unknown>) => void
}

export function CreateSparePartModal({ open, form, loading, onCancel, onSubmit }: CreateSparePartModalProps) {
  const onFinishFailed = useFormValidationFeedback()
  return (
    <Modal title="Thêm phụ tùng" open={open} onCancel={onCancel} onOk={() => form.submit()} confirmLoading={loading} okText="Thêm phụ tùng" width={680} destroyOnHidden>
      <Form form={form} layout="vertical" onFinish={onSubmit} onFinishFailed={onFinishFailed} scrollToFirstError requiredMark>
        <div className="form-grid two-cols">
          <Form.Item label="Mã phụ tùng" name="sku" rules={[{ required: true, message: 'Nhập mã phụ tùng' }]}><Input /></Form.Item>
          <Form.Item label="Tên phụ tùng" name="name" rules={[{ required: true, message: 'Nhập tên phụ tùng' }]}><Input /></Form.Item>
          <Form.Item label="Đơn vị" name="unit" rules={[{ required: true, message: 'Nhập đơn vị' }]}><Input /></Form.Item>
          <Form.Item label="Tồn ban đầu" name="initialStock" rules={[{ required: true, message: 'Nhập tồn ban đầu' }]}><InputNumber min={0} precision={3} formatter={formatCompactDecimalInput} style={{ width: '100%' }} /></Form.Item>
          <Form.Item label="Ngưỡng tồn tối thiểu" name="reorderLevel" rules={[{ required: true, message: 'Nhập ngưỡng tồn tối thiểu' }]}><InputNumber min={0} precision={3} formatter={formatCompactDecimalInput} style={{ width: '100%' }} /></Form.Item>
          <Form.Item label="Đơn giá" name="unitPrice" rules={[{ required: true, message: 'Nhập đơn giá' }]}><InputNumber min={0} precision={0} style={{ width: '100%' }} addonAfter="VND" /></Form.Item>
        </div>
      </Form>
    </Modal>
  )
}

type ReorderLevelModalProps = {
  part?: SparePart
  form: FormInstance<{ reorderLevel: number }>
  loading: boolean
  onCancel: () => void
  onSubmit: (values: { reorderLevel: number }) => void
}

export function ReorderLevelModal({ part, form, loading, onCancel, onSubmit }: ReorderLevelModalProps) {
  const onFinishFailed = useFormValidationFeedback()
  return (
    <Modal
      title={`Ngưỡng tồn tối thiểu · ${part?.sku ?? ''}`}
      open={Boolean(part)}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={loading}
      okText="Lưu ngưỡng"
      destroyOnHidden
    >
      <Alert
        type="info"
        showIcon
        message="Ngưỡng tồn tối thiểu là mốc cảnh báo, không phải số lượng đặt mua."
        description={part
          ? `Tồn hiện tại: ${formatQuantityWithUnit(part.stockQuantity, part.unit)}. Khi tồn chạm hoặc thấp hơn ngưỡng này, phụ tùng được xem là tồn thấp.`
          : undefined}
        style={{ marginBottom: 16 }}
      />
      <Form form={form} layout="vertical" onFinish={onSubmit} onFinishFailed={onFinishFailed} scrollToFirstError requiredMark>
        <Form.Item label="Ngưỡng tồn tối thiểu" name="reorderLevel" rules={[{ required: true, message: 'Nhập ngưỡng tồn tối thiểu' }]}>
          <InputNumber min={0} precision={3} formatter={formatCompactDecimalInput} style={{ width: '100%' }} addonAfter={part?.unit} />
        </Form.Item>
      </Form>
    </Modal>
  )
}

type ImportStockModalProps = {
  part?: SparePart
  form: FormInstance<{ quantity: number; note: string }>
  loading: boolean
  onCancel: () => void
  onSubmit: (values: { quantity: number; note: string }) => void
}

export function ImportStockModal({ part, form, loading, onCancel, onSubmit }: ImportStockModalProps) {
  const onFinishFailed = useFormValidationFeedback()
  return (
    <Modal title={`Nhập kho · ${part?.sku ?? ''}`} open={Boolean(part)} onCancel={onCancel} onOk={() => form.submit()} confirmLoading={loading} okText="Xác nhận nhập kho" destroyOnHidden>
      <Form form={form} layout="vertical" onFinish={onSubmit} onFinishFailed={onFinishFailed} scrollToFirstError requiredMark>
        <Form.Item label="Số lượng" name="quantity" rules={[{ required: true, message: 'Nhập số lượng' }]}><InputNumber min={0.001} precision={3} formatter={formatCompactDecimalInput} style={{ width: '100%' }} addonAfter={part?.unit} /></Form.Item>
        <Form.Item label="Ghi chú" name="note" rules={[{ required: true, message: 'Nhập ghi chú' }]}><Input /></Form.Item>
      </Form>
    </Modal>
  )
}

type BulkImportPreviewModalProps = {
  open: boolean
  result?: SparePartImportResult
  loading: boolean
  onCancel: () => void
  onCommit: () => void
}

export function BulkImportPreviewModal({ open, result, loading, onCancel, onCommit }: BulkImportPreviewModalProps) {
  return (
    <Modal
      title="Kiểm tra tệp nhập phụ tùng"
      open={open}
      onCancel={onCancel}
      onOk={onCommit}
      okText="Xác nhận nhập"
      cancelText="Đóng"
      confirmLoading={loading}
      okButtonProps={{ disabled: !result || result.errorRows > 0 }}
      width={820}
      destroyOnHidden
    >
      {result && (
        <Space direction="vertical" size={14} style={{ width: '100%' }}>
          <Alert
            type={result.errorRows > 0 ? 'warning' : 'success'}
            showIcon
            message={`${result.validRows}/${result.totalRows} dòng hợp lệ`}
            description={result.errorRows > 0 ? 'Tệp còn dòng lỗi, hệ thống chưa ghi dữ liệu vào kho.' : 'Tệp hợp lệ, bạn có thể xác nhận để ghi dữ liệu vào kho.'}
          />
          <Table<SparePartImportRowResult>
            rowKey="rowNumber"
            size="small"
            dataSource={result.rows}
            pagination={{ pageSize: 8, showSizeChanger: false }}
            columns={[
              { title: 'Dòng', dataIndex: 'rowNumber', width: 80, sorter: (a, b) => compareNumber(a.rowNumber, b.rowNumber) },
              { title: 'Mã phụ tùng', dataIndex: 'sku', width: 150, sorter: (a, b) => compareText(a.sku, b.sku) },
              { title: 'Tên phụ tùng', dataIndex: 'name', ellipsis: true, sorter: (a, b) => compareText(a.name, b.name) },
              {
                title: 'Kết quả', dataIndex: 'valid', width: 130,
                sorter: (a, b) => compareNumber(Number(a.valid), Number(b.valid)),
                render: (valid: boolean) => <MetaBadge tone={valid ? 'success' : 'danger'}>{valid ? 'Hợp lệ' : 'Lỗi'}</MetaBadge>,
              },
              { title: 'Ghi chú', dataIndex: 'message', ellipsis: true, sorter: (a, b) => compareText(a.message, b.message) },
            ]}
          />
        </Space>
      )}
    </Modal>
  )
}
