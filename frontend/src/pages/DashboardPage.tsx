import {
  AlertOutlined,
  AppstoreOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloudServerOutlined,
  CustomerServiceOutlined,
  ReloadOutlined,
  TeamOutlined,
  ToolOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Empty, Progress, Result, Row, Skeleton, Space, Table, Tag, Typography } from 'antd'
import { dashboardApi } from '../api/services'
import { useAuth } from '../auth/AuthContext'
import { MetricCard } from '../components/MetricCard'
import { PageHeader } from '../components/PageHeader'
import { PriorityTag, StatusTag } from '../components/StatusTag'
import { formatDateTime } from '../utils/format'

export function DashboardPage() {
  const { user } = useAuth()
  const isTechnician = user?.role === 'TECHNICIAN'
  const { data, isLoading, error, refetch, isFetching } = useQuery({ queryKey: ['dashboard'], queryFn: dashboardApi.get })

  if (isLoading) {
    return (
      <div className="page-shell">
        <PageHeader
          eyebrow="Operations cockpit"
          title="Tổng quan vận hành"
          description="Theo dõi tình trạng dịch vụ và các công việc cần ưu tiên hôm nay."
          meta={<Tag color="blue">Đang tải dữ liệu</Tag>}
        />
        <Card className="content-card dashboard-hero" bordered={false}><Skeleton active paragraph={{ rows: 4 }} /></Card>
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
          eyebrow="Operations cockpit"
          title="Tổng quan vận hành"
          description="Theo dõi tình trạng dịch vụ và các công việc cần ưu tiên hôm nay."
          actions={<Button icon={<ReloadOutlined />} loading={isFetching} onClick={() => refetch()}>Tải lại</Button>}
          meta={<Space size={[8, 8]} wrap><Tag color="red">Backend offline</Tag><Tag>Local-first</Tag></Space>}
        />

        <Card className="content-card dashboard-empty-shell" bordered={false}>
          <div className="dashboard-empty-visual">
            <div className="dashboard-empty-orb" />
            <div className="dashboard-empty-orb dashboard-empty-orb-secondary" />
            <CloudServerOutlined className="dashboard-empty-icon" />
          </div>
          <Result
            status="warning"
            title="Chưa lấy được dữ liệu vận hành"
            subTitle="Backend local chưa phản hồi hoặc API URL chưa đúng. Giao diện vẫn sẵn sàng, chỉ cần kết nối API là số liệu sẽ tự hiển thị."
            extra={<Button type="primary" icon={<ReloadOutlined />} loading={isFetching} onClick={() => refetch()}>Thử lại</Button>}
          />
          <div className="dashboard-empty-hints">
            <div><strong>Start backend</strong><span>Chạy Spring Boot với profile local để mở dữ liệu seed.</span></div>
            <div><strong>Check API URL</strong><span>Frontend cần trỏ về <span className="dashboard-url-pill">http://localhost:8080/api/v1</span>.</span></div>
            <div><strong>Retry</strong><span>Tải lại sau khi backend sẵn sàng.</span></div>
          </div>
        </Card>
      </div>
    )
  }

  const activeTotal = data.openWorkOrders + data.assignedWorkOrders + data.inProgressWorkOrders + data.waitingForPartsWorkOrders
  const completedTotal = data.completedWorkOrders + data.closedWorkOrders
  const completionRate = activeTotal + completedTotal === 0 ? 0 : Math.round((completedTotal / (activeTotal + completedTotal)) * 100)
  const heroHighlights = isTechnician
    ? [
        { label: 'Đã phân công', value: data.assignedWorkOrders },
        { label: 'Đang xử lý', value: data.inProgressWorkOrders },
        { label: 'Chờ phụ tùng', value: data.waitingForPartsWorkOrders },
      ]
    : [
        { label: 'Yêu cầu mở', value: data.openServiceRequests },
        { label: 'Đang xử lý', value: data.inProgressWorkOrders },
        { label: 'Sắp hết tồn', value: data.lowStockParts },
      ]

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Operations cockpit"
        title="Tổng quan vận hành"
        description="Một màn hình điều phối tập trung cho yêu cầu dịch vụ, work order, kỹ thuật viên và tồn kho."
        actions={<Button icon={<ReloadOutlined />} loading={isFetching} onClick={() => refetch()}>Làm mới</Button>}
        meta={<Space size={[8, 8]} wrap>{isTechnician ? <Tag color="blue">Technician view</Tag> : <Tag color="cyan">Control view</Tag>}<Tag color="green">Local-first</Tag></Space>}
      />

      <Card className="content-card dashboard-hero" bordered={false}>
        <div className="dashboard-hero-copy">
          <Typography.Text className="dashboard-kicker">Live service board</Typography.Text>
          <Typography.Title level={3} className="dashboard-hero-title">
            {isTechnician ? 'Tập trung vào những phiếu đang chờ bạn xử lý.' : 'Nhìn nhanh điểm nóng vận hành trong ngày.'}
          </Typography.Title>
          <Typography.Paragraph className="dashboard-hero-text">
            Dữ liệu quan trọng được gom theo thứ tự ưu tiên: việc cần tiếp nhận, việc đang chạy, rủi ro tồn kho và tỷ lệ hoàn tất.
          </Typography.Paragraph>
        </div>
        <div className="dashboard-hero-highlights">
          {heroHighlights.map((item) => (
            <div key={item.label}>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </div>
          ))}
        </div>
      </Card>

      <Row gutter={[16, 16]}>
        {isTechnician ? (
          <>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đã phân công" value={data.assignedWorkOrders} helper="Công việc cần chuẩn bị" icon={<CustomerServiceOutlined />} tone="blue" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đang thực hiện" value={data.inProgressWorkOrders} helper="Công việc của bạn" icon={<ToolOutlined />} tone="purple" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Chờ phụ tùng" value={data.waitingForPartsWorkOrders} helper="Cần phối hợp với kho" icon={<ClockCircleOutlined />} tone="orange" /></Col>
            <Col xs={24} sm={12} xl={6}><MetricCard label="Đã hoàn thành" value={data.completedWorkOrders} helper="Chờ xác nhận hoặc đóng phiếu" icon={<CheckCircleOutlined />} tone="green" /></Col>
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
        <Col xs={24} xl={7}>
          <Card title="Sức khỏe vận hành" className="content-card operations-health" bordered={false}>
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
          <Card title="Work order gần đây" className="content-card" bordered={false}>
            <Table
              rowKey="id"
              dataSource={data.recentWorkOrders}
              pagination={false}
              className="content-table"
              scroll={{ x: 840 }}
              locale={{ emptyText: <Empty description="Chưa có work order gần đây" /> }}
              columns={[
                { title: 'Phiếu', dataIndex: 'code', width: 165, render: (value: string) => <Typography.Text strong code>{value}</Typography.Text> },
                { title: 'Nội dung', dataIndex: 'summary', width: 280, ellipsis: true },
                { title: 'Khách hàng', dataIndex: 'customerName', width: 190, ellipsis: true },
                { title: 'Ưu tiên', dataIndex: 'priority', width: 120, render: (value) => <PriorityTag priority={value} /> },
                { title: 'Trạng thái', dataIndex: 'status', width: 160, render: (value) => <StatusTag status={value} /> },
                { title: 'Lịch hẹn', dataIndex: 'scheduledStart', width: 170, render: formatDateTime },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
