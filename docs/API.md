# Danh mục API

Base URL local: `http://localhost:8080/api/v1`

## Authentication
- `POST /auth/login`

## Dashboard
- `GET /dashboard`

## Customers
- `GET /customers`
- `POST /customers`
- `GET /customers/{id}`
- `PUT /customers/{id}`

## Assets
- `GET /assets`
- `POST /assets`
- `GET /assets/{id}`
- `PUT /assets/{id}`

## Service requests
- `GET /service-requests`
- `POST /service-requests`
- `POST /work-orders/from-service-request/{serviceRequestId}`
- `POST /service-requests/{id}/cancel`

## Work orders
- `GET /work-orders`
- `POST /work-orders`
- `GET /work-orders/{id}`
- `POST /work-orders/{id}/schedule`
- `POST /work-orders/{id}/transition`

## Technicians
- `GET /technicians`

## Inventory
- `GET /spare-parts`
- `POST /spare-parts`
- `POST /spare-parts/{id}/import`
- `POST /work-orders/{workOrderId}/parts/consume`

## Files
- `POST /attachments`
- `GET /attachments?referenceType={type}&referenceId={id}`
- `GET /attachments/{id}/download`

## Audit và notification
- `GET /audit-logs`
- `GET /notifications`
- `GET /notifications/unread-count`
- `PATCH /notifications/{id}/read`

Swagger là tài liệu request/response chính xác nhất khi backend chạy: `http://localhost:8080/swagger-ui.html`.
