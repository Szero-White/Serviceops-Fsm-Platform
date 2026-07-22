import { CheckCircleFilled } from '@ant-design/icons'
import { Button, Col, Divider, Row, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { PRICING } from '../content/landingData'
import { SectionHeader } from '../shared/landingShared'

const { Paragraph } = Typography

export function PricingSection() {
  return (
    <section className="lp-pricing" id="pricing" aria-labelledby="pricing-heading">
      <div className="lp-container">
        <SectionHeader
          tag="Bảng giá"
          title="Minh bạch, không ẩn phí"
          sub="Dùng thử miễn phí 14 ngày. Nâng cấp hoặc huỷ bất cứ lúc nào khi demo local-first đã ổn định."
        />

        <Row gutter={[24, 32]} justify="center" align="middle">
          {PRICING.map((tier) => (
            <Col key={tier.name} xs={24} sm={20} md={12} lg={8}>
              <div className={`lp-price-card${tier.highlight ? ' lp-price-card--highlight' : ''}`}>
                {tier.badge && <div className="lp-price-badge" aria-label="Most popular">{tier.badge}</div>}

                <div className="lp-price-tier-name">{tier.name}</div>
                <div className="lp-price-amount" aria-label={`Giá: ${tier.price}${tier.unit}`}>
                  {tier.unit ? (
                    <>
                      <span className="lp-price-currency">VND</span>
                      <span className="lp-price-value">{tier.price}</span>
                      <span className="lp-price-unit">{tier.unit}</span>
                    </>
                  ) : (
                    <span className="lp-price-value lp-price-value--contact">{tier.price}</span>
                  )}
                </div>

                <Paragraph className="lp-price-desc">{tier.desc}</Paragraph>
                <Divider className={tier.highlight ? 'lp-price-divider lp-price-divider--light' : 'lp-price-divider'} />

                <ul className="lp-price-features" aria-label={`Features for ${tier.name}`}>
                  {tier.features.map((feature) => (
                    <li key={feature}>
                      <CheckCircleFilled className="lp-check-icon" aria-hidden="true" />
                      <span>{feature}</span>
                    </li>
                  ))}
                </ul>

                <Link to="/login">
                  <Button
                    type={tier.highlight ? 'default' : 'primary'}
                    block
                    size="large"
                    className={tier.highlight ? 'lp-price-btn--white' : ''}
                  >
                    {tier.cta}
                  </Button>
                </Link>
              </div>
            </Col>
          ))}
        </Row>
      </div>
    </section>
  )
}
