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
          title="Đủ rõ ràng để đánh giá như một sản phẩm vận hành thật"
          sub="Các thông tin dưới đây phản ánh đúng phạm vi hệ thống đang vận hành, không dùng số liệu quảng bá chưa được kiểm chứng."
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
          title="Một luồng vận hành thống nhất thay vì nhiều màn hình rời rạc"
          sub="ServiceOps liên kết tiếp nhận, thiết bị, phiếu công việc, lịch kỹ thuật viên, phụ tùng, nhật ký hệ thống và phân quyền trong cùng một quy trình."
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
          sub="Phần này mô tả trực tiếp các kịch bản có thể trải nghiệm trong bản dùng thử, không dùng lời chứng thực hoặc số liệu chưa được kiểm chứng."
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
              tag="Khả năng vận hành"
              title="Những năng lực đang được hỗ trợ trong hệ thống"
              sub="Hệ thống ưu tiên kết nối dữ liệu nhất quán, lưu trữ có kiểm soát, triển khai ổn định và theo dõi tình trạng hoạt động; chỉ mở rộng khi có nhu cầu thật."
              align="left"
            />
            <Link to="/login">
              <Button type="primary" size="large" icon={<ArrowRightOutlined />}>
                Vào bản dùng thử
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
