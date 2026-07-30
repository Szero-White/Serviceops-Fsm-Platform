import { Alert, Modal, Space, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'

export interface CsvImportRow {
  rowNumber: number
  valid: boolean
  message: string
}

export interface CsvImportResult<T extends CsvImportRow> {
  totalRows: number
  validRows: number
  errorRows: number
  importedRows: number
  committed: boolean
  rows: T[]
}

interface CsvImportPreviewModalProps<T extends CsvImportRow> {
  open: boolean
  title: string
  result?: CsvImportResult<T>
  columns: ColumnsType<T>
  committing: boolean
  onCancel: () => void
  onCommit: () => void
}

export function CsvImportPreviewModal<T extends CsvImportRow>({
  open,
  title,
  result,
  columns,
  committing,
  onCancel,
  onCommit,
}: CsvImportPreviewModalProps<T>) {
  return (
    <Modal
      title={title}
      open={open}
      onCancel={onCancel}
      onOk={onCommit}
      okText="Xác nhận nhập"
      cancelText="Đóng"
      confirmLoading={committing}
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
            description={result.errorRows > 0 ? 'File còn dòng lỗi, hệ thống chưa ghi dữ liệu.' : 'File hợp lệ, bạn có thể xác nhận để ghi dữ liệu.'}
          />
          <Table<T>
            rowKey="rowNumber"
            size="small"
            dataSource={result.rows}
            pagination={{ pageSize: 8, showSizeChanger: false }}
            columns={[
              { title: 'Dòng', dataIndex: 'rowNumber', width: 80 },
              ...columns,
              {
                title: 'Kết quả',
                dataIndex: 'valid',
                width: 130,
                render: (valid: boolean) => <Tag color={valid ? 'green' : 'red'}>{valid ? 'Hợp lệ' : 'Lỗi'}</Tag>,
              },
              { title: 'Ghi chú', dataIndex: 'message', ellipsis: true },
            ]}
          />
        </Space>
      )}
    </Modal>
  )
}
