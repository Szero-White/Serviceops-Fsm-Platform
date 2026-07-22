import { MenuOutlined, RocketOutlined } from '@ant-design/icons'
import { Button, Divider, Drawer, Space, Tag } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { NAV_LINKS } from '../content/landingData'
import { BrandLockup } from '../shared/landingShared'

export function LandingNavbar() {
  const [drawerOpen, setDrawerOpen] = useState(false)
  const closeDrawer = () => setDrawerOpen(false)

  return (
    <>
      <nav className="lp-nav" role="navigation" aria-label="Main navigation">
        <div className="lp-nav-inner">
          <Link to="/landing" className="lp-nav-brand" aria-label="ServiceOps home">
            <BrandLockup />
            <Tag color="blue" className="lp-brand-tag">FSM</Tag>
          </Link>

          <div className="lp-nav-links" role="list">
            {NAV_LINKS.map((link) => (
              <a key={link.href} href={link.href} className="lp-nav-link" role="listitem">
                {link.label}
              </a>
            ))}
          </div>

          <div className="lp-nav-cta">
            <Link to="/login">
              <Button size="middle">Đăng nhập</Button>
            </Link>
            <Link to="/login">
              <Button type="primary" size="middle" icon={<RocketOutlined />}>
                Dùng thử miễn phí
              </Button>
            </Link>
          </div>

          <button
            className="lp-nav-burger"
            onClick={() => setDrawerOpen(true)}
            aria-label="Mở menu"
            aria-expanded={drawerOpen}
          >
            <MenuOutlined />
          </button>
        </div>
      </nav>

      <Drawer open={drawerOpen} onClose={closeDrawer} placement="right" size="default" title={<BrandLockup compact />}>
        <Space orientation="vertical" size={4} className="lp-stack-full">
          {NAV_LINKS.map((link) => (
            <a key={link.href} href={link.href} className="lp-drawer-link" onClick={closeDrawer}>
              {link.label}
            </a>
          ))}
        </Space>
        <Divider />
        <Space orientation="vertical" size={10} className="lp-stack-full">
          <Link to="/login" onClick={closeDrawer}>
            <Button block size="large">Đăng nhập</Button>
          </Link>
          <Link to="/login" onClick={closeDrawer}>
            <Button type="primary" block size="large" icon={<RocketOutlined />}>
              Dùng thử miễn phí
            </Button>
          </Link>
        </Space>
      </Drawer>
    </>
  )
}
