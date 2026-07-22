import { SettingOutlined } from '@ant-design/icons'
import { Typography } from 'antd'
import type { CSSProperties, ReactNode } from 'react'

const { Title, Paragraph } = Typography

export function SectionTag({ children }: { children: ReactNode }) {
  return <span className="lp-section-tag">{children}</span>
}

export function SectionHeader({
  tag,
  title,
  sub,
  align = 'center',
}: {
  tag: string
  title: string
  sub?: string
  align?: 'center' | 'left'
}) {
  return (
    <div className={`lp-section-header lp-section-header--${align}`}>
      <SectionTag>{tag}</SectionTag>
      <Title level={2} className="lp-section-title">
        {title}
      </Title>
      {sub && <Paragraph className="lp-section-sub">{sub}</Paragraph>}
    </div>
  )
}

export function BrandLockup({ compact = false }: { compact?: boolean }) {
  return (
    <div className={compact ? 'lp-brand-lockup lp-brand-lockup--compact' : 'lp-brand-lockup'}>
      <div className={compact ? 'lp-brand-icon lp-brand-icon--sm' : 'lp-brand-icon'} aria-hidden="true">
        <SettingOutlined />
      </div>
      <span className="lp-brand-name">ServiceOps</span>
    </div>
  )
}

export const featureIconStyle = ({ bg, fg }: { bg: string; fg: string }): CSSProperties => ({
  background: bg,
  color: fg,
})

export const backgroundStyle = (background: string): CSSProperties => ({ background })
