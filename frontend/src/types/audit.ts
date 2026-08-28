export interface AuditLog {
  id: string
  actorUsername: string
  action: string
  entityType: string
  entityId?: string
  details?: string
  createdAt: string
}
