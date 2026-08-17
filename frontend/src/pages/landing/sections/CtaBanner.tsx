import { ArrowRightOutlined, GithubOutlined } from '@ant-design/icons'
import { Button, Typography } from 'antd'
import { Link } from 'react-router-dom'

const { Title, Paragraph, Text } = Typography

export function CtaBanner() {
  return (
    <section className="lp-cta-banner" aria-labelledby="cta-heading">
      <div className="lp-container lp-cta-inner">
        <Title level={2} id="cta-heading" className="lp-cta-title">
          Xem ServiceOps như một luồng vận hành hoàn chỉnh
        </Title>
        <Paragraph className="lp-cta-sub">
          Chọn một vai trò demo, chạy qua quy trình nghiệp vụ và đối chiếu trực tiếp với kiến trúc, test và tài liệu trong repository.
        </Paragraph>
        <div className="lp-cta-actions">
          <Link to="/login">
            <Button size="large" className="lp-btn-cta-white" icon={<ArrowRightOutlined />}>
              Mở ứng dụng demo
            </Button>
          </Link>
          <a href="https://github.com/Szero-White/Serviceops-Fsm-Platform" target="_blank" rel="noreferrer">
            <Button size="large" className="lp-btn-cta-outline" icon={<GithubOutlined />}>
              Xem repository
            </Button>
          </a>
        </div>
        <Text className="lp-cta-note">Java 21 · Spring Boot · PostgreSQL · React · Docker · CI · Testcontainers</Text>
      </div>
    </section>
  )
}
