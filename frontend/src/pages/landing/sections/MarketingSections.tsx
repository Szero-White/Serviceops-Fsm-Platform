import { ArrowRightOutlined, StarFilled } from '@ant-design/icons'
import { Avatar, Button, Col, Row, Typography } from 'antd'
import { Link } from 'react-router-dom'
import {
  FEATURE_COLORS,
  FEATURES,
  HOW_IT_WORKS,
  INTEGRATIONS,
  STATS,
  TESTIMONIALS,
} from '../content/landingData'
import { backgroundStyle, featureIconStyle, SectionHeader } from '../shared/landingShared'

const { Title, Text, Paragraph } = Typography

export function StatsSection() {
  return (
    <section className="lp-stats" id="stats" aria-labelledby="stats-heading">
      <h2 id="stats-heading" className="lp-visually-hidden">Số liệu nổi bật</h2>
      <div className="lp-container">
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
          title="Mọi thứ cần thiết để vận hành dịch vụ hiện trường"
          sub="Từ tiếp nhận đến nghiệm thu, ServiceOps thay thế spreadsheet, chat và email rời rạc bằng một quy trình thống nhất."
        />

        <Row gutter={[24, 24]}>
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
          title="Hoạt động như thế nào?"
          sub="4 bước đơn giản từ lúc tiếp nhận đến khi hoàn thành, minh bạch và truy vết được."
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

export function TestimonialsSection() {
  return (
    <section className="lp-testimonials" id="testimonials" aria-labelledby="testi-heading">
      <div className="lp-container">
        <SectionHeader tag="Khách hàng nói gì" title="Kết quả thực tế, không phải lời hứa" />

        <Row gutter={[24, 24]}>
          {TESTIMONIALS.map((testimonial) => (
            <Col key={testimonial.name} xs={24} md={8}>
              <figure className="lp-testi-card">
                <div className="lp-testi-stars" aria-label={`${testimonial.rating} sao`}>
                  {Array.from({ length: testimonial.rating }).map((_, index) => (
                    <StarFilled key={index} className="lp-star" aria-hidden="true" />
                  ))}
                </div>
                <blockquote className="lp-testi-quote">
                  <Paragraph className="lp-testi-text">"{testimonial.text}"</Paragraph>
                </blockquote>
                <figcaption className="lp-testi-author">
                  <Avatar size={44} className="lp-testi-avatar" style={backgroundStyle(testimonial.avatarColor)}>
                    {testimonial.avatar}
                  </Avatar>
                  <div>
                    <Text strong className="lp-testi-name">{testimonial.name}</Text>
                    <Text className="lp-testi-role">
                      {testimonial.role} . {testimonial.company}
                    </Text>
                  </div>
                </figcaption>
              </figure>
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
              tag="Tích hợp"
              title="Kết nối hệ thống hiện có của bạn"
              sub="API mở giúp ServiceOps kết nối ERP, CRM và công cụ kế toán khi sản phẩm cần mở rộng."
              align="left"
            />
            <Link to="/login">
              <Button type="primary" size="large" icon={<ArrowRightOutlined />}>
                Xem tài liệu API
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
