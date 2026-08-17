import {
  ArrowRightOutlined,
  CheckCircleFilled,
  PlayCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import { Button, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { CAPABILITY_LABELS, MOCK_METRICS, MOCK_NAV_ITEMS, MOCK_ROWS } from '../content/landingData'
import { backgroundStyle } from '../shared/landingShared'

const { Title, Paragraph } = Typography

function DashboardMockup() {
  return (
    <div className="lp-mockup" aria-hidden="true">
      <div className="lp-mockup-titlebar">
        <span className="lp-dot lp-dot--red" />
        <span className="lp-dot lp-dot--yellow" />
        <span className="lp-dot lp-dot--green" />
        <span className="lp-mockup-url">serviceops.app / dashboard</span>
      </div>

      <div className="lp-mockup-shell">
        <aside className="lp-mockup-sidebar">
          <div className="lp-mockup-brand">
            <SettingOutlined />
            <span>ServiceOps</span>
          </div>
          {MOCK_NAV_ITEMS.map((item, index) => (
            <div key={item} className={`lp-mockup-nav-item${index === 0 ? ' is-active' : ''}`}>
              {item}
            </div>
          ))}
        </aside>

        <main className="lp-mockup-main">
          <div className="lp-mockup-topbar">
            <span className="lp-mockup-page-title">Tổng quan hôm nay</span>
            <div className="lp-mockup-avatar" aria-label="User avatar">M</div>
          </div>

          <div className="lp-mockup-metrics">
            {MOCK_METRICS.map((metric) => (
              <div key={metric.label} className="lp-mockup-metric" style={backgroundStyle(metric.bg)}>
                <span className="lp-mockup-metric-icon">{metric.icon}</span>
                <span className="lp-mockup-metric-val">{metric.value}</span>
                <span className="lp-mockup-metric-lbl">{metric.label}</span>
              </div>
            ))}
          </div>

          <div className="lp-mockup-table-header">
            <span>Công việc gần đây</span>
            <span className="lp-mockup-view-all">Xem tất cả -&gt;</span>
          </div>

          <div className="lp-mockup-table-body">
            {MOCK_ROWS.map((row) => (
              <div key={row.id} className="lp-mockup-row">
                <span className="lp-mockup-row-id">{row.id}</span>
                <span className="lp-mockup-row-client">{row.client}</span>
                <span className="lp-mockup-row-status" style={backgroundStyle(row.statusBg)}>
                  {row.status}
                </span>
              </div>
            ))}
          </div>
        </main>
      </div>
    </div>
  )
}

export function HeroSection() {
  return (
    <section className="lp-hero" aria-labelledby="hero-heading">
      <div className="lp-container">
        <div className="lp-hero-grid">
          <div className="lp-hero-copy">
            <div className="lp-hero-eyebrow">
              <CheckCircleFilled />
              Field Service Management SaaS
            </div>

            <Title id="hero-heading" className="lp-hero-heading">
              Điều phối dịch vụ hiện trường rõ ràng từ yêu cầu đến nghiệm thu
            </Title>

            <Paragraph className="lp-hero-sub">
              ServiceOps gom khách hàng, thiết bị, phiếu công việc, lịch kỹ thuật viên, phụ tùng và audit log vào một
              luồng vận hành thống nhất, có phân quyền và khả năng truy vết rõ ràng.
            </Paragraph>

            <div className="lp-hero-actions">
              <Link to="/login">
                <Button type="primary" size="large" icon={<ArrowRightOutlined />} className="lp-btn-primary-lg">
                  Vào ứng dụng demo
                </Button>
              </Link>
              <a href="#features">
                <Button size="large" icon={<PlayCircleOutlined />} className="lp-btn-secondary-lg">
                  Xem tính năng
                </Button>
              </a>
            </div>

            <div className="lp-hero-social-proof">
              <span className="lp-social-proof-label">Phạm vi demo</span>
              {CAPABILITY_LABELS.slice(0, 3).map((name) => (
                <span key={name} className="lp-social-proof-logo">{name}</span>
              ))}
              <span className="lp-social-proof-more">Một luồng thống nhất</span>
            </div>
          </div>

          <div className="lp-hero-visual">
            <DashboardMockup />
          </div>
        </div>
      </div>
    </section>
  )
}

export function LogoBar() {
  return (
    <section className="lp-logobar" aria-label="Phạm vi nghiệp vụ">
      <div className="lp-logobar-label">Luồng nghiệp vụ được mô hình hóa end-to-end</div>
      <div className="lp-logobar-track" aria-label="Phạm vi nghiệp vụ">
        {CAPABILITY_LABELS.map((name) => (
          <span key={name} className="lp-logobar-item">{name}</span>
        ))}
      </div>
    </section>
  )
}
