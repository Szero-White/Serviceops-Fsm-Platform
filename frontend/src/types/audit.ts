export interface AuditLog {
  id: string
  actorUsername: string
  actorDisplayName?: string
  actorRole?: string
  action: string
  entityType: string
  entityId?: string
  details?: string
  createdAt: string
}
