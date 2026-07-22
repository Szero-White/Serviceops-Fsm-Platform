import type { ReactNode } from 'react'

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
  meta,
}: {
  eyebrow?: string
  title: string
  description?: string
  actions?: ReactNode
  meta?: ReactNode
}) {
  return (
    <header className="page-header">
      <div className="page-header-copy">
        {eyebrow && <span className="page-header-eyebrow">{eyebrow}</span>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>

      {(meta || actions) && (
        <div className="page-header-actions">
          {meta && <div className="page-header-meta">{meta}</div>}
          {actions}
        </div>
      )}
    </header>
  )
}
