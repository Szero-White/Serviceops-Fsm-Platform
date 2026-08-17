import { Typography } from 'antd'
import { Link } from 'react-router-dom'
import { FOOTER_LINKS } from '../content/landingData'
import { BrandLockup } from '../shared/landingShared'

const { Paragraph } = Typography

export function LandingFooter() {
  return (
    <footer className="lp-footer">
      <div className="lp-container">
        <div className="lp-footer-body">
          <div className="lp-footer-brand">
            <div className="lp-footer-logo">
              <BrandLockup compact />
            </div>
            <Paragraph className="lp-footer-tagline">
              Nền tảng quản lý dịch vụ hiện trường được xây dựng cho doanh nghiệp cần quy trình rõ ràng.
            </Paragraph>
          </div>

          {Object.entries(FOOTER_LINKS).map(([heading, links]) => (
            <nav key={heading} className="lp-footer-col" aria-label={heading}>
              <span className="lp-footer-col-heading">{heading}</span>
              {links.map((link) =>
                link.href.startsWith('/') ? (
                  <Link key={link.label} to={link.href} className="lp-footer-link">{link.label}</Link>
                ) : (
                  <a key={link.label} href={link.href} className="lp-footer-link">{link.label}</a>
                ),
              )}
            </nav>
          ))}
        </div>

        <div className="lp-footer-bottom">
          <span className="lp-footer-copy">© {new Date().getFullYear()} ServiceOps · Production-oriented portfolio project.</span>
          <div className="lp-footer-legal">
            <Link to="/login" className="lp-footer-link">Demo</Link>
            <a href="https://github.com/Szero-White/Serviceops-Fsm-Platform" target="_blank" rel="noreferrer" className="lp-footer-link">GitHub</a>
          </div>
        </div>
      </div>
    </footer>
  )
}
