import { CustomerServiceOutlined, RocketOutlined } from '@ant-design/icons'
import { Button, Typography } from 'antd'
import { Link } from 'react-router-dom'

const { Title, Paragraph, Text } = Typography

export function CtaBanner() {
  return (
    <section className="lp-cta-banner" aria-labelledby="cta-heading">
      <div className="lp-cta-orbs" aria-hidden="true">
        <span className="lp-cta-orb lp-cta-orb--1" />
        <span className="lp-cta-orb lp-cta-orb--2" />
      </div>

      <div className="lp-container lp-cta-inner">
        <Title level={2} id="cta-heading" className="lp-cta-title">
          Sẵn sàng nâng cấp vận hành dịch vụ?
        </Title>
        <Paragraph className="lp-cta-sub">
          Bắt đầu với bản demo local-first, sau đó mở rộng theo quy trình thực tế của doanh nghiệp.
        </Paragraph>
        <div className="lp-cta-actions">
          <Link to="/login">
            <Button size="large" className="lp-btn-cta-white" icon={<RocketOutlined />}>
              Dùng thử miễn phí 14 ngày
            </Button>
          </Link>
          <a href="mailto:sales@serviceops.vn">
            <Button size="large" className="lp-btn-cta-outline" icon={<CustomerServiceOutlined />}>
              Đặt lịch demo
            </Button>
          </a>
        </div>
        <Text className="lp-cta-note">Onboarding nhanh . Hỗ trợ tiếng Việt . Dữ liệu sẵn sàng cho demo</Text>
      </div>
    </section>
  )
}
