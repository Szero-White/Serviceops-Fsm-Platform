import { PhoneOutlined, SafetyCertificateOutlined, ToolOutlined, UserOutlined } from '@ant-design/icons'
import { Avatar, Card, Col, Empty, Row, Skeleton, Space, Tag, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { techniciansApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'

export function TechniciansPage() {
  const { data, isLoading } = useQuery({ queryKey: ['technicians'], queryFn: techniciansApi.list })

  return (
    <div>
      <PageHeader title="Đội ngũ kỹ thuật" description="Danh sách kỹ thuật viên và năng lực phục vụ hiện trường." />
      {isLoading ? <Skeleton active paragraph={{ rows: 8 }} /> : data?.length ? (
        <Row gutter={[16, 16]}>
          {data.map((technician) => (
            <Col xs={24} md={12} xl={8} key={technician.id}>
              <Card className="technician-card">
                <div className="technician-heading">
                  <Avatar size={54} icon={<UserOutlined />} />
                  <div><Typography.Title level={4}>{technician.name}</Typography.Title><Typography.Text type="secondary">@{technician.username}</Typography.Text></div>
                  <Tag color={technician.active ? 'green' : 'default'}>{technician.active ? 'Sẵn sàng' : 'Ngừng'}</Tag>
                </div>
                <Space direction="vertical" size={12} className="technician-details">
                  <span><PhoneOutlined /> {technician.phone ?? 'Chưa cập nhật'}</span>
                  <span><ToolOutlined /> {technician.skills ?? 'Chưa khai báo kỹ năng'}</span>
                  <span><SafetyCertificateOutlined /> Tài khoản đã phân quyền kỹ thuật viên</span>
                </Space>
              </Card>
            </Col>
          ))}
        </Row>
      ) : <Empty description="Chưa có kỹ thuật viên" />}
    </div>
  )
}
