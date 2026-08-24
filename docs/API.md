# Danh mục API

Base URL local: `http://localhost:8080/api/v1`

Swagger (`http://localhost:8080/swagger-ui.html`) là nguồn request/response runtime chi tiết nhất. File này mô tả catalogue và ownership chính của source hiện tại.

## Quy ước chung

- API nghiệp vụ yêu cầu JWT trừ `/auth/login` và các endpoint public được SecurityConfig cho phép.
- Backend authorization là lớp quyết định; frontend route/action chỉ là UX.
- Danh sách lớn dùng server-side search + pagination; `page` bắt đầu từ `0`, `size` được chuẩn hóa và giới hạn.
- Mọi repository nghiệp vụ phải giữ tenant scope.

## Authentication

- `POST /auth/login`

## Dashboard

- `GET /dashboard` — OWNER / DISPATCHER / CUSTOMER_SERVICE / TECHNICIAN.
- WAREHOUSE_STAFF không nhận operational dashboard; frontend mặc định đưa Warehouse tới `/inventory`.

## Users — OWNER only

- `GET /users`
- `POST /users`
- `PUT /users/{id}`
- `DELETE /users/{id}`

Technician account không thể bị deactivate khi còn operational Work Order assignment.

## Customers

Read — OWNER / CUSTOMER_SERVICE / DISPATCHER:

- `GET /customers?search={text}&page={n}&size={n}`
- `GET /customers/{id}`

Write/import/export — OWNER / CUSTOMER_SERVICE:

- `POST /customers`
- `PUT /customers/{id}`
- `DELETE /customers/{id}`
- `GET /customers/export`
- `GET /customers/import-template`
- `POST /customers/import?commit={bool}`

## Assets

Read — OWNER / CUSTOMER_SERVICE / DISPATCHER:

- `GET /assets?search={text}&customerId={uuid}&page={n}&size={n}`
- `GET /assets/{id}`

Write/import/export — OWNER / CUSTOMER_SERVICE:

- `POST /assets`
- `PUT /assets/{id}`
- `DELETE /assets/{id}`
- `GET /assets/export`
- `GET /assets/import-template`
- `POST /assets/import?commit={bool}`

Hard delete bị chặn khi Asset đã được Service Request/Work Order tham chiếu hoặc còn attachment.

## Service Requests — OWNER / CUSTOMER_SERVICE

- `GET /service-requests?search={text}&status={status}&page={n}&size={n}`
- `GET /service-requests/{id}`
- `POST /service-requests`
- `PUT /service-requests/{id}` — chỉ request còn `OPEN`
- `POST /service-requests/{id}/cancel`
- `DELETE /service-requests/{id}` — bị chặn khi đã convert/có Work Order hoặc còn attachment
- `POST /work-orders/from-service-request/{serviceRequestId}` — đường tạo Work Order chuẩn

Không có public `POST /work-orders` generic direct-create.

## Service Channels

Read — OWNER / CUSTOMER_SERVICE:

- `GET /service-channels`

Write — OWNER only:

- `POST /service-channels`
- `PUT /service-channels/{id}`
- `DELETE /service-channels/{id}`

Channel đang được historical Service Request tham chiếu không được xóa phá lịch sử.

## Work Orders

Read — OWNER / DISPATCHER / CUSTOMER_SERVICE / TECHNICIAN:

- `GET /work-orders?search={text}&status={status}&page={n}&size={n}`
- `GET /work-orders/history?search={text}&status={CLOSED|CANCELLED}&page={n}&size={n}`
- `GET /work-orders/{id}`
- `GET /work-orders/{id}/invoice` — service guard chỉ cho invoice khi Work Order `CLOSED`

TECHNICIAN read được giới hạn tiếp theo ở service/repository vào Work Order được assign cho identity hiện tại.

Mutations:

- `POST /work-orders/from-service-request/{serviceRequestId}` — OWNER / CUSTOMER_SERVICE
- `POST /work-orders/{id}/schedule` — OWNER / DISPATCHER
- `POST /work-orders/{id}/transition` — endpoint cho OWNER / DISPATCHER / CUSTOMER_SERVICE / TECHNICIAN, nhưng service áp role-specific target-status policy
- `DELETE /work-orders/{id}` — OWNER only; chỉ soft-hide/archive Work Order `CLOSED`/`CANCELLED` khỏi history query

Transition ownership:

- TECHNICIAN: `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED` cho assigned Work Order.
- CUSTOMER_SERVICE: `CANCELLED` với cancellation reason.
- DISPATCHER: `CANCELLED` theo operational policy; không field progress / acceptance / close / reopen.
- OWNER: management transitions được state machine cho phép.

## Scheduling

- `GET /schedule-board?from={instant}&to={instant}` — OWNER / DISPATCHER; tối đa 31 ngày/range.
- `GET /my-schedule?from={instant}&to={instant}` — TECHNICIAN; backend suy ra TechnicianProfile từ signed-in `userId`, không nhận `technicianId` từ client.

## Technicians

- `GET /technicians?activeOnly={bool}` — OWNER / DISPATCHER.
- `PUT /technicians/{id}` — OWNER only.

Profile không thể chuyển inactive khi còn operational Work Order assignment. Account active/inactive được quản lý tại `/users`.

## Inventory

Read:

- `GET /spare-parts?search={text}&page={n}&size={n}` — OWNER / WAREHOUSE_STAFF / TECHNICIAN
- `GET /spare-parts/export` — OWNER / WAREHOUSE_STAFF / TECHNICIAN

Catalog/import/lifecycle — OWNER / WAREHOUSE_STAFF:

- `POST /spare-parts`
- `PATCH /spare-parts/{id}/active`
- `DELETE /spare-parts/{id}`
- `GET /spare-parts/import-template`
- `POST /spare-parts/import?commit={bool}`
- `POST /spare-parts/{id}/import`

Operational consumption:

- `POST /work-orders/{workOrderId}/parts/consume` — OWNER / TECHNICIAN; Technician chỉ cho assigned Work Order.

Warehouse không consume thay technician qua Work Order flow.

## Attachments

- `POST /attachments`
- `GET /attachments?referenceType={WORK_ORDER|ASSET|SERVICE_REQUEST}&referenceId={id}`
- `GET /attachments/{id}/download`
- `PATCH /attachments/{id}` — rename
- `DELETE /attachments/{id}`

Authorization nằm trong AttachmentService theo reference:

- WORK_ORDER: OWNER / DISPATCHER / CUSTOMER_SERVICE; TECHNICIAN chỉ assigned Work Order.
- ASSET: OWNER / DISPATCHER / CUSTOMER_SERVICE.
- SERVICE_REQUEST: OWNER / CUSTOMER_SERVICE.
- WAREHOUSE_STAFF bị chặn khỏi operational references.
- Rename/delete: OWNER hoặc uploader, sau khi reference access đã hợp lệ.

## Audit

- `GET /audit-logs?page={n}&size={n}&q={keyword}&actor={username}&action={action}&entityType={type}&from={ISO-8601}&to={ISO-8601}` — OWNER / DISPATCHER.

## Notifications

Authenticated user chỉ thao tác notification của chính identity trong tenant:

- `GET /notifications`
- `GET /notifications/unread-count`
- `PATCH /notifications/{id}/read`

## AI assistance

- `POST /ai/service-request-draft` — OWNER / CUSTOMER_SERVICE.
- `POST /ai/help` — tất cả năm business roles; guidance vẫn phải tuân theo role ownership backend.
