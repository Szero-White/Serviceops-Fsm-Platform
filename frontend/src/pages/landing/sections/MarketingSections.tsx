import { ArrowRightOutlined } from '@ant-design/icons'
import { Button, Col, Row, Typography } from 'antd'
import { Link } from 'react-router-dom'
import {
  FEATURE_COLORS,
  FEATURES,
  HOW_IT_WORKS,
  INTEGRATIONS,
  OPERATIONAL_SCENARIOS,
  STATS,
} from '../content/landingData'
import { featureIconStyle, SectionHeader } from '../shared/landingShared'

const { Title, Text } = Typography

export function StatsSection() {
  return (
    <section className="lp-stats" id="stats" aria-labelledby="stats-heading">
      <div className="lp-container">
        <SectionHeader
          tag="Năng lực nền tảng"
          title="Đủ chiều sâu kỹ thuật để review như một sản phẩm thật"
          sub="Các con số dưới đây phản ánh chính repository và acceptance gate hiện tại, không phải số liệu marketing giả lập."
        />
        <h2 id="stats-heading" className="lp-visually-hidden">Năng lực nền tảng</h2>
        <dl className="lp-stats-grid">
          {STATS.map((stat) => (
            <div key={stat.label} className="lp-stat-item">
              <div className="lp-stat-icon" aria-hidden="true">{stat.icon}</div>
              <dt className="lp-stat-value">{stat.value}</dt>
              <dd className="lp-stat-label">{stat.label}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  )
}

export function FeaturesSection() {
  return (
    <section className="lp-features" id="features" aria-labelledby="features-heading">
      <div className="lp-container">
        <SectionHeader
          tag="Tính năng"
          title="Một luồng vận hành thống nhất thay vì các màn CRUD rời rạc"
          sub="ServiceOps liên kết tiếp nhận, tài sản, phiếu công việc, lịch kỹ thuật viên, phụ tùng, audit và phân quyền trong cùng một domain flow."
        />

        <Row gutter={[20, 20]}>
          {FEATURES.map((feature) => {
            const colors = FEATURE_COLORS[feature.colorKey]
            return (
              <Col key={feature.title} xs={24} sm={12} lg={8}>
                <article className="lp-feature-card">
                  <div className="lp-feature-icon" style={featureIconStyle(colors)} aria-hidden="true">
                    {feature.icon}
                  </div>
                  <Title level={5} className="lp-feature-title">{feature.title}</Title>
                  <Text className="lp-feature-desc">{feature.desc}</Text>
                </article>
              </Col>
            )
          })}
        </Row>
      </div>
    </section>
  )
}

export function HowItWorksSection() {
  return (
    <section className="lp-how" id="how" aria-labelledby="how-heading">
      <div className="lp-container">
        <SectionHeader
          tag="Quy trình"
          title="Từ yêu cầu khách hàng đến đóng phiếu có truy vết"
          sub="Mỗi bước có chủ thể rõ ràng, trạng thái rõ ràng và dữ liệu liên quan để tiếp tục mở rộng mà không phá vỡ luồng nghiệp vụ."
        />

        <ol className="lp-how-steps" aria-label="Các bước thực hiện">
          {HOW_IT_WORKS.map((step) => (
            <li key={step.step} className="lp-how-step">
              <div className="lp-how-step-num" aria-label={`Bước ${step.step}`}>{step.step}</div>
              <div className="lp-how-step-body">
                <Title level={5} className="lp-how-step-title">{step.title}</Title>
                <Text className="lp-how-step-desc">{step.desc}</Text>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}

export function OperationalScenariosSection() {
  return (
    <section className="lp-testimonials" id="scenarios" aria-labelledby="scenarios-heading">
      <div className="lp-container">
        <SectionHeader
          tag="Kịch bản sử dụng"
          title="Mỗi vai trò nhìn thấy đúng phần việc của mình"
          sub="Thay vì dùng testimonial hoặc số liệu khách hàng chưa được kiểm chứng, landing page mô tả trực tiếp các kịch bản có thể chạy trong demo."
        />
        <h2 id="scenarios-heading" className="lp-visually-hidden">Kịch bản sử dụng</h2>

        <Row gutter={[20, 20]}>
          {OPERATIONAL_SCENARIOS.map((scenario) => (
            <Col key={scenario.title} xs={24} md={8}>
              <article className="lp-testi-card">
                <div className="lp-scenario-icon" aria-hidden="true">{scenario.icon}</div>
                <Text className="lp-scenario-audience">{scenario.audience}</Text>
                <Title level={5} className="lp-testi-name">{scenario.title}</Title>
                <Text className="lp-testi-text">{scenario.text}</Text>
              </article>
            </Col>
          ))}
        </Row>
      </div>
    </section>
  )
}

export function IntegrationsSection() {
  return (
    <section className="lp-integrations" id="integrations" aria-labelledby="integrations-heading">
      <div className="lp-container">
        <div className="lp-integrations-grid">
          <div className="lp-integrations-copy">
            <SectionHeader
              tag="Nền tảng kỹ thuật"
              title="Tích hợp những gì repository thực sự đang có"
              sub="REST API, PostgreSQL, Docker/Nginx và observability được trình bày đúng phạm vi hiện tại; connector bên thứ ba chỉ nên thêm khi có use case thật."
              align="left"
            />
            <Link to="/login">
              <Button type="primary" size="large" icon={<ArrowRightOutlined />}>
                Vào ứng dụng demo
              </Button>
            </Link>
          </div>

          <div className="lp-integrations-cards">
            {INTEGRATIONS.map((item) => (
              <div key={item.name} className="lp-integration-card">
                <div className="lp-integration-icon" aria-hidden="true">{item.icon}</div>
                <Text strong className="lp-integration-name">{item.name}</Text>
                <Text className="lp-integration-desc">{item.desc}</Text>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
