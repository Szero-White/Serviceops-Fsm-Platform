import { DownOutlined, DownloadOutlined, FileExcelOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons'
import { Button, Dropdown, Space, Upload } from 'antd'

type AssetPageActionsProps = {
  importing: boolean
  onExport: () => void
  onDownloadTemplate: () => void
  onImportFile: (file: File) => void
  onCreate: () => void
}

export function AssetPageActions({ importing, onExport, onDownloadTemplate, onImportFile, onCreate }: AssetPageActionsProps) {
  return (
    <Space size={10} wrap>
      <Dropdown
        trigger={['click']}
        menu={{
          items: [
            { key: 'export', icon: <DownloadOutlined />, label: 'Xuất CSV', onClick: onExport },
            { key: 'template', icon: <FileExcelOutlined />, label: 'Tải mẫu import', onClick: onDownloadTemplate },
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
                  <span>Nhập CSV</span>
                </Upload>
              ),
            },
          ],
        }}
      >
        <Button icon={<FileExcelOutlined />} loading={importing}>Dữ liệu <DownOutlined /></Button>
      </Dropdown>
      <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>Thêm thiết bị</Button>
    </Space>
  )
}
