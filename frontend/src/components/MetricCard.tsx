import type { ReactNode } from 'react'

export function MetricCard({
  label,
  value,
  helper,
  icon,
  tone = 'primary',
}: {
  label: string
  value: number | string
  helper?: string
  icon: ReactNode
  tone?: 'primary' | 'success' | 'warning' | 'danger' | 'neutral'
}) {
  return (
    <div className={`metric-card metric-card-${tone}`}>
      <div className="metric-icon" aria-hidden="true">{icon}</div>
      <div className="metric-body">
        <span className="metric-label">{label}</span>
        <strong className="metric-value">{value}</strong>
        {helper && <small>{helper}</small>}
      </div>
    </div>
  )
}
