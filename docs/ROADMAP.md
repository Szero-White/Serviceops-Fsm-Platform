# Roadmap

## Phase 1 — Local-first MVP (repository hiện tại)
- JWT và role
- Customer, asset, service request
- Work order lifecycle
- Scheduling conflict protection
- Inventory ledger
- Local file storage
- Audit, notification, dashboard
- PostgreSQL, Flyway, React UI

## Phase 2 — Production foundation
- Refresh token rotation
- MinIO/S3 adapter
- Redis cache/rate limiting
- Transactional outbox + RabbitMQ
- Email templates và SLA scheduler
- PDF biên bản nghiệm thu
- Backup/restore và observability

## Phase 3 — Productization
- Preventive maintenance plan
- Warranty contract và quotation/invoice
- Customer portal/PWA offline queue
- Import Excel, branding theo tenant
- Subscription/feature flags
- PostgreSQL RLS và object-level authorization đầy đủ

## Không làm nếu chưa có nhu cầu thật
- Microservices
- Kafka
- Kubernetes
- Elasticsearch
- AI dispatch/route optimization
