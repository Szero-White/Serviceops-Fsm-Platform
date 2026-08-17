import { CheckCircleFilled } from '@ant-design/icons'
import { Button, Col, Row, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { DEPLOYMENT_OPTIONS } from '../content/landingData'
import { SectionHeader } from '../shared/landingShared'

const { Paragraph } = Typography

export function DeploymentSection() {
  return (
    <section className="lp-deployment" id="deployment" aria-labelledby="deployment-heading">
      <div className="lp-container">
        <SectionHeader
          tag="Triển khai"
          title="Từ review local đến production-like theo từng mức rõ ràng"
          sub="Không hiển thị bảng giá hoặc cam kết thương mại chưa có dữ liệu thật. Thay vào đó, repository mô tả chính xác mức triển khai đang hỗ trợ và hướng mở rộng tiếp theo."
        />
        <h2 id="deployment-heading" className="lp-visually-hidden">Các mức triển khai</h2>

        <Row gutter={[20, 24]} justify="center" align="stretch">
          {DEPLOYMENT_OPTIONS.map((tier) => (
            <Col key={tier.name} xs={24} sm={20} md={12} lg={8}>
              <article className={`lp-deployment-card${tier.highlight ? ' lp-deployment-card--highlight' : ''}`}>
                {tier.badge && <div className="lp-deployment-badge">{tier.badge}</div>}
                <div className="lp-deployment-name">{tier.name}</div>
                <Paragraph className="lp-deployment-desc">{tier.desc}</Paragraph>

                <ul className="lp-deployment-features" aria-label={`Phạm vi ${tier.name}`}>
                  {tier.features.map((feature) => (
                    <li key={feature}>
                      <CheckCircleFilled className="lp-check-icon" aria-hidden="true" />
                      <span>{feature}</span>
                    </li>
                  ))}
                </ul>

                {tier.name === 'Product rollout' ? (
                  <a href="#how" className="lp-deployment-link">
                    <Button type={tier.highlight ? 'primary' : 'default'} block size="large">
                      {tier.cta}
                    </Button>
                  </a>
                ) : (
                  <Link to="/login" className="lp-deployment-link">
                    <Button type={tier.highlight ? 'primary' : 'default'} block size="large">
                      {tier.cta}
                    </Button>
                  </Link>
                )}
              </article>
            </Col>
          ))}
        </Row>
      </div>
    </section>
  )
}
