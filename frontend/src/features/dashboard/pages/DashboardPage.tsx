import {
  AlertOutlined,
  AppstoreOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloudServerOutlined,
  CustomerServiceOutlined,
  ReloadOutlined,
  TeamOutlined,
  ToolOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Empty, Progress, Result, Row, Skeleton, Space, Table, Typography } from 'antd'
import { dashboardApi } from '../api'
import { useAuth } from '../../auth/AuthContext'
import { MetricCard } from '../../../components/MetricCard'
import { MetaBadge } from '../../../components/PresentationBadge'
import { PageHeader } from '../../../components/PageHeader'
import { PriorityTag, StatusTag } from '../../../components/StatusTag'
import { formatDateTime } from '../../../utils/format'
import { API_URL } from '../../../api/http'
import { useNavigate } from 'react-router-dom'

export function DashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const isTechnician = user?.role === 'TECHNICIAN'
  const dashboardEyebrow = isTechnician ? 'Không gian kỹ thuật viên' : 'Trung tâm vận hành'
  const dashboardTitle = isTechnician ? 'Tổng quan công việc của tôi' : 'Tổng quan vận hành'
  const dashboardDescription = isTechnician
    ? 'Theo dõi công việc được phân công, tiến độ thực hiện và lịch làm việc cá nhân.'
    : 'Một màn hình điều phối tập trung cho yêu cầu dịch vụ, phiếu công việc, kỹ thuật viên và tồn kho.'
  const { data, isLoading, error, refetch, isFetching } = useQuery({ queryKey: ['dashboard'], queryFn: dashboardApi.get })

  if (isLoading) {
    return (
      <div className="page-shell">
        <PageHeader
          eyebrow={dashboardEyebrow}
          title={dashboardTitle}
          description={dashboardDescription}
          meta={<MetaBadge tone="info">Đang tải dữ liệu</MetaBadge>}
        />
        <Row gutter={[16, 16]}>
          {Array.from({ length: 4 }).map((_, index) => (
            <Col key={index} xs={24} sm={12} xl={6}><Card className="content-card" bordered={false}><Skeleton active title={false} paragraph={{ rows: 2 }} /></Card></Col>
          ))}
        </Row>
      </div>
    )
  }

  if (error || !data) {
    return (
      <div className="page-shell">
        <PageHeader
          eyebrow={dashboardEyebrow}
          title={dashboardTitle}
          description={dashboardDescription}
          actions={<Button icon={<ReloadOutlined />} loading={isFetching} onClick={() => refetch()}>Tải lại</Button>}
          meta={<><MetaBadge tone="danger">Backend chưa kết nối</MetaBadge><MetaBadge>API chưa sẵn sàng</MetaBadge></>}
        />

        <Card className="content-card dashboard-empty-shell" bordered={false}>
          <div className="dashboard-empty-visual">
            <CloudServerOutlined className="dashboard-empty-icon" />
          </div>
          <Result
            status="warning"
            title="Chưa lấy được dữ liệu vận hành"
            subTitle="Backend chưa phản hồi hoặc API URL chưa đúng. Giao diện vẫn sẵn sàng; số liệu sẽ hiển thị khi API hoạt động trở lại."
            extra={<Button type="primary" icon={<ReloadOutlined />} loading={isFetching} onClick={() => refetch()}>Thử lại</Button>}
          />
          <div className="dashboard-empty-hints">
            <div><strong>Kiểm tra backend</strong><span>Đảm bảo Spring Boot hoặc backend container đang ở trạng thái healthy.</span></div>
            <div><strong>Kiểm tra API URL</strong><span>Frontend cần trỏ về <span className="dashboard-url-pill">{API_URL}</span>.</span></div>
            <div><strong>Thử lại</strong><span>Tải lại sau khi backend sẵn sàng.</span></div>
          </div>
        </Card>
      </div>
    )
  }

  const activeTotal = data.openWorkOrders
    + data.scheduledWorkOrders
    + data.assignedWorkOrders
    + data.onTheWayWorkOrders
    + data.inProgressWorkOrders
    + data.waitingForPartsWorkOrders
    + data.reopenedWorkOrders
  const completedTotal = data.completedWorkOrders + data.customerAcceptedWorkOrders + data.closedWorkOrders
  const completionRate = activeTotal + completedTotal === 0 ? 0 : Math.round((completedTotal / (activeTotal + completedTotal)) * 100)
  return (
    <div className="page-shell">
      <PageHeader
        eyebrow={dashboardEyebrow}
        title={dashboardTitle}
        description={dashboardDescription}
        actions={isTechnician ? (
          <Space>
            <Button type="primary" icon={<CalendarOutlined />} onClick={() => navigate('/my-schedule')}>Lịch của tôi</Button>
            <Button icon={<ReloadOutlined />} loading={isFetching} onClick={() => refetch()}>Làm mới</Button>
          </Space>
        ) : <Button icon={<ReloadOutlined />} loading={isFetching} onClick={() => refetch()}>Làm mới</Button>}
        meta={<MetaBadge>{isTechnician ? 'Dữ liệu cá nhân' : 'Chế độ điều phối'}</MetaBadge>}
      />

      <Row gutter={[16, 16]}>
        {isTechnician ? (
          <>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đã phân công" value={data.assignedWorkOrders} helper="Công việc cần chuẩn bị" icon={<CustomerServiceOutlined />} tone="primary" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đang thực hiện" value={data.inProgressWorkOrders + data.onTheWayWorkOrders + data.reopenedWorkOrders} helper="Bao gồm đang di chuyển và mở lại" icon={<ToolOutlined />} tone="primary" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Chờ phụ tùng" value={data.waitingForPartsWorkOrders} helper="Cần phối hợp với kho" icon={<ClockCircleOutlined />} tone="warning" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đã hoàn thành" value={data.completedWorkOrders} helper="Chờ xác nhận hoặc đóng phiếu" icon={<CheckCircleOutlined />} tone="success" /></Col>
          </>
        ) : (
          <>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Yêu cầu đang mở" value={data.openServiceRequests} helper="Cần tiếp nhận và xử lý" icon={<CustomerServiceOutlined />} tone="primary" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đang thực hiện" value={data.inProgressWorkOrders + data.onTheWayWorkOrders + data.reopenedWorkOrders} helper={`${data.scheduledWorkOrders + data.assignedWorkOrders} phiếu chờ / đã phân công`} icon={<ToolOutlined />} tone="primary" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Chờ phụ tùng" value={data.waitingForPartsWorkOrders} helper="Cần phối hợp với kho" icon={<ClockCircleOutlined />} tone="warning" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Phụ tùng sắp hết" value={data.lowStockParts} helper="Đã chạm ngưỡng tồn tối thiểu" icon={<AlertOutlined />} tone="danger" /></Col>
          </>
        )}
      </Row>

      <Row gutter={[16, 16]} className="section-row">
        <Col xs={24} xl={7}>
          <Card title={isTechnician ? 'Tiến độ của tôi' : 'Sức khỏe vận hành'} className="content-card operations-health" bordered={false}>
            <div className="completion-ring">
              <Progress type="dashboard" percent={completionRate} strokeWidth={10} />
              <div><strong>Tỷ lệ hoàn tất</strong><span>Trên tổng số phiếu hiện có</span></div>
            </div>
            <div className="health-list">
              {!isTechnician && <div><span><TeamOutlined /> Khách hàng</span><strong>{data.customers}</strong></div>}
              {!isTechnician && <div><span><AppstoreOutlined /> Thiết bị</span><strong>{data.assets}</strong></div>}
              {!isTechnician && <div><span><ToolOutlined /> Kỹ thuật viên</span><strong>{data.activeTechnicians}</strong></div>}
              <div><span><CheckCircleOutlined /> Đã đóng</span><strong>{data.closedWorkOrders}</strong></div>
            </div>
          </Card>
        </Col>
        <Col xs={24} xl={17}>
          <Card title={isTechnician ? 'Công việc gần đây của tôi' : 'Phiếu công việc gần đây'} className="content-card" bordered={false}>
            <Table
              rowKey="id"
              dataSource={data.recentWorkOrders}
              pagination={false}
              className="content-table"
              locale={{ emptyText: <Empty description="Chưa có phiếu công việc gần đây" /> }}
              columns={[
                { title: 'Phiếu', dataIndex: 'code', width: 145, render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
                { title: 'Nội dung', dataIndex: 'summary', ellipsis: true },
                { title: 'Ưu tiên', dataIndex: 'priority', width: 105, render: (value) => <PriorityTag priority={value} /> },
                { title: 'Trạng thái', dataIndex: 'status', width: 135, render: (value) => <StatusTag status={value} /> },
                { title: 'Lịch hẹn', dataIndex: 'scheduledStart', width: 145, render: formatDateTime },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
