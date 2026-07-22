import { PhoneOutlined, SafetyCertificateOutlined, ToolOutlined, UserOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Avatar, Card, Col, Empty, Row, Skeleton, Space, Tag, Typography } from 'antd'
import { techniciansApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'

export function TechniciansPage() {
  const { data, isLoading } = useQuery({ queryKey: ['technicians'], queryFn: techniciansApi.list })

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Field workforce"
        title="Đội ngũ kỹ thuật"
        description="Danh sách kỹ thuật viên, năng lực phục vụ và trạng thái sẵn sàng tại hiện trường."
        meta={<Tag color="blue">{data?.length ?? 0} kỹ thuật viên</Tag>}
      />

      {isLoading ? <Skeleton active paragraph={{ rows: 8 }} /> : data?.length ? (
        <Row gutter={[16, 16]}>
          {data.map((technician) => (
            <Col xs={24} md={12} xl={8} key={technician.id}>
              <Card className="technician-card" bordered={false}>
                <div className="technician-heading">
                  <Avatar size={54} icon={<UserOutlined />} />
                  <div>
                    <Typography.Title level={4}>{technician.name}</Typography.Title>
                    <Typography.Text type="secondary">@{technician.username}</Typography.Text>
                  </div>
                  <Tag color={technician.active ? 'green' : 'default'}>{technician.active ? 'Sẵn sàng' : 'Ngừng'}</Tag>
                </div>
                <Space orientation="vertical" size={12} className="technician-details">
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
