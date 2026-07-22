import { AlertOutlined, AppstoreOutlined, CheckCircleOutlined, ClockCircleOutlined, CustomerServiceOutlined, TeamOutlined, ToolOutlined } from '@ant-design/icons'
import { Card, Col, Empty, Progress, Row, Skeleton, Table, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../api/services'
import { useAuth } from '../auth/AuthContext'
import { MetricCard } from '../components/MetricCard'
import { PageHeader } from '../components/PageHeader'
import { PriorityTag, StatusTag } from '../components/StatusTag'
import { formatDateTime } from '../utils/format'

export function DashboardPage() {
  const { user } = useAuth()
  const isTechnician = user?.role === 'TECHNICIAN'
  const { data, isLoading, error } = useQuery({ queryKey: ['dashboard'], queryFn: dashboardApi.get })

  if (isLoading) return <Skeleton active paragraph={{ rows: 10 }} />
  if (error || !data) return <Card><Empty description="Không tải được dữ liệu tổng quan" /></Card>

  const activeTotal = data.openWorkOrders + data.assignedWorkOrders + data.inProgressWorkOrders + data.waitingForPartsWorkOrders
  const completedTotal = data.completedWorkOrders + data.closedWorkOrders
  const completionRate = activeTotal + completedTotal === 0 ? 0 : Math.round((completedTotal / (activeTotal + completedTotal)) * 100)

  return (
    <div>
      <PageHeader title="Tổng quan vận hành" description="Theo dõi tình trạng dịch vụ và các công việc cần ưu tiên hôm nay." />
      <Row gutter={[16, 16]}>
        {isTechnician ? (
          <>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đã phân công" value={data.assignedWorkOrders} helper="Công việc cần chuẩn bị" icon={<CustomerServiceOutlined />} tone="blue" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đang thực hiện" value={data.inProgressWorkOrders} helper="Công việc của bạn" icon={<ToolOutlined />} tone="purple" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Chờ phụ tùng" value={data.waitingForPartsWorkOrders} helper="Cần phối hợp với kho" icon={<ClockCircleOutlined />} tone="orange" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đã hoàn thành" value={data.completedWorkOrders} helper="Chờ khách xác nhận/đóng phiếu" icon={<CheckCircleOutlined />} tone="blue" /></Col>
          </>
        ) : (
          <>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Yêu cầu đang mở" value={data.openServiceRequests} helper="Cần tiếp nhận và xử lý" icon={<CustomerServiceOutlined />} tone="blue" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đang thực hiện" value={data.inProgressWorkOrders} helper={`${data.assignedWorkOrders} phiếu đã phân công`} icon={<ToolOutlined />} tone="purple" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Chờ phụ tùng" value={data.waitingForPartsWorkOrders} helper="Cần phối hợp với kho" icon={<ClockCircleOutlined />} tone="orange" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Phụ tùng sắp hết" value={data.lowStockParts} helper="Đã chạm mức đặt hàng" icon={<AlertOutlined />} tone="red" /></Col>
          </>
        )}
      </Row>

      <Row gutter={[16, 16]} className="section-row">
        <Col xs={24} xl={16}>
          <Card title="Work order gần đây" className="content-card">
            <Table
              rowKey="id"
              dataSource={data.recentWorkOrders}
              pagination={false}
              scroll={{ x: 760 }}
              columns={[
                { title: 'Mã phiếu', dataIndex: 'code', width: 150, render: (value: string) => <Typography.Text strong>{value}</Typography.Text> },
                { title: 'Nội dung', dataIndex: 'summary', width: 250, ellipsis: true },
                { title: 'Khách hàng', dataIndex: 'customerName', width: 180, ellipsis: true },
                { title: 'Ưu tiên', dataIndex: 'priority', width: 110, render: (value) => <PriorityTag priority={value} /> },
                { title: 'Trạng thái', dataIndex: 'status', width: 150, render: (value) => <StatusTag status={value} /> },
                { title: 'Lịch hẹn', dataIndex: 'scheduledStart', width: 160, render: formatDateTime },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} xl={8}>
          <Card title="Sức khỏe vận hành" className="content-card operations-health">
            <div className="completion-ring"><Progress type="dashboard" percent={completionRate} strokeWidth={10} /><div><strong>Tỷ lệ hoàn tất</strong><span>Trên tổng số phiếu hiện có</span></div></div>
            <div className="health-list">
              {!isTechnician && <div><span><TeamOutlined /> Khách hàng</span><strong>{data.customers}</strong></div>}
              {!isTechnician && <div><span><AppstoreOutlined /> Thiết bị</span><strong>{data.assets}</strong></div>}
              {!isTechnician && <div><span><ToolOutlined /> Kỹ thuật viên</span><strong>{data.activeTechnicians}</strong></div>}
              <div><span><CheckCircleOutlined /> Đã đóng</span><strong>{data.closedWorkOrders}</strong></div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
