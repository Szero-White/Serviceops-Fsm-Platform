import { DownOutlined, FileExcelOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons'
import { Button, Dropdown, Space, Upload } from 'antd'

export type InventoryPageActionsProps = {
  canManageStock: boolean
  importLoading: boolean
  onExport: () => void
  onDownloadTemplate: () => void
  onImportFile: (file: File) => void
  onCreate: () => void
}

export function InventoryPageActions({
  canManageStock,
  importLoading,
  onExport,
  onDownloadTemplate,
  onImportFile,
  onCreate,
}: InventoryPageActionsProps) {
  return (
    <Space size={10} wrap>
      <Dropdown
        trigger={['click']}
        menu={{
          items: [
            { key: 'export', icon: <FileExcelOutlined />, label: 'Xuất danh sách', onClick: onExport },
            ...(canManageStock ? [
              { key: 'template', icon: <FileExcelOutlined />, label: 'Tải mẫu nhập dữ liệu', onClick: onDownloadTemplate },
              {
                key: 'import',
                icon: <UploadOutlined />,
                label: (
                  <Upload
                    accept=".csv,text/csv"
                    showUploadList={false}
                    beforeUpload={(file) => {
                      onImportFile(file)
                      return Upload.LIST_IGNORE
                    }}
                  >
                    <span>Nhập danh sách</span>
                  </Upload>
                ),
              },
            ] : []),
          ],
        }}
      >
        <Button icon={<FileExcelOutlined />} loading={importLoading}>
          Dữ liệu <DownOutlined />
        </Button>
      </Dropdown>
      {canManageStock && (
        <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>
          Thêm phụ tùng
        </Button>
      )}
    </Space>
  )
}
