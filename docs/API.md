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

- `GET /customers?search={text}&active={bool}&page={n}&size={n}`
  - `active` optional; omit để màn quản lý vẫn thấy cả khách hoạt động và ngừng hoạt động.
  - workflow tạo mới dùng `active=true` để chỉ lấy khách còn hoạt động.
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
- `GET /work-orders/{id}` — detail trả cả `history` (status history tương thích cũ) và `activities` operational đã merge theo thời gian từ status history + audit điều phối + inventory `CONSUME` hợp lệ của `TECHNICIAN`. Warehouse `RETURN` vẫn nằm ở stock ledger và invoice net, không xuất hiện như tiến trình hiện trường của Work Order; activity phụ tùng gồm SKU/tên/đơn vị/số lượng/actor/note/time và không tạo bảng timeline duplicate.
- `GET /work-orders/{id}/invoice` — service guard chỉ cho invoice khi Work Order `CLOSED`

TECHNICIAN read được giới hạn tiếp theo ở service/repository vào Work Order được assign cho identity hiện tại.

Mutations:

- `POST /work-orders/from-service-request/{serviceRequestId}` — OWNER / CUSTOMER_SERVICE
- `POST /work-orders/{id}/schedule` — OWNER/DISPATCHER; lần phân công đầu không cần `reason`, nhưng mọi lần điều phối lại kỹ thuật viên/lịch phải gửi `reason` (tối đa 500 ký tự). Chỉ cho phép khi WO còn `OPEN`, `SCHEDULED`, `ASSIGNED` hoặc `REOPENED`; sau khi field work bắt đầu thì bị từ chối.
- `POST /work-orders/{id}/transition` — OWNER / DISPATCHER / CUSTOMER_SERVICE / TECHNICIAN; service áp role-specific target-status policy và Technician vẫn bị giới hạn vào Work Order được giao.
- `DELETE /work-orders/{id}` — OWNER only; chỉ soft-hide/archive Work Order `CLOSED`/`CANCELLED` khỏi history query

Transition ownership:

- TECHNICIAN: `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`, sau đó `CUSTOMER_ACCEPTED`, `CLOSED` hoặc `REOPENED` cho assigned Work Order.
- OWNER: admin override cho `CUSTOMER_ACCEPTED`, `CLOSED`, `REOPENED`, `CANCELLED`; không dùng generic transition để giả lập tiến độ hiện trường và không consume phụ tùng thay Technician.
- CUSTOMER_SERVICE: `REOPENED`, `CANCELLED`. Không ghi nhận `CUSTOMER_ACCEPTED`/`CLOSED`.
- DISPATCHER: `CANCELLED` theo operational policy; không field progress / acceptance / close / reopen. Mọi transition sang `CANCELLED` đều bắt buộc có reason ở backend.

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
- `PATCH /spare-parts/{id}/reorder-level` — cập nhật `reorderLevel` (UI: **Ngưỡng tồn tối thiểu**). Không thay đổi stock và không tạo inventory transaction; thay đổi được audit. Nếu ngưỡng mới làm tồn hiện tại chuyển từ bình thường sang tồn thấp, backend phát low-stock notification sau commit cho OWNER/WAREHOUSE_STAFF khác người thao tác.
- `DELETE /spare-parts/{id}`
- `GET /spare-parts/import-template`
- `POST /spare-parts/import?commit={bool}`
- `POST /spare-parts/{id}/import`

Stock reconciliation and traceability — OWNER / WAREHOUSE_STAFF:

- `POST /spare-parts/{id}/stocktake` — nhập số lượng đếm thực tế; backend tạo `ADJUSTMENT_IN` hoặc `ADJUSTMENT_OUT` khi có chênh lệch. Notification được phát qua application event sau commit: Owner nhận chênh lệch, Warehouse nhận thêm cảnh báo nếu tồn xuống **ngưỡng tồn tối thiểu**.
- `GET /inventory-transactions` — phân trang/filter theo keyword, loại giao dịch và khoảng thời gian.
- `GET /work-orders/{workOrderId}/parts/{sparePartId}/returnable` — số lượng còn có thể hoàn theo net `CONSUME - RETURN`.
- `POST /work-orders/{workOrderId}/parts/{sparePartId}/return` — Warehouse xác nhận nhận lại phụ tùng chưa sử dụng; không được vượt net consumed.

Operational consumption:

- `POST /work-orders/{workOrderId}/parts/consume` — TECHNICIAN only; chỉ cho Work Order được giao cho chính kỹ thuật viên và đang ở `ASSIGNED`, `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS` hoặc `REOPENED`. Từ `COMPLETED` trở đi không ghi nhận CONSUME mới. Giao dịch thành công của Technician xuất hiện ngay trong `activities` của Work Order detail để UI hiển thị ở Tiến trình xử lý.

Warehouse không consume thay technician. Warehouse chỉ xác nhận stocktake/adjustment, xem ledger và nhận part return; Work Order `CLOSED`/`CANCELLED` không nhận return mới. RETURN là nghiệp vụ kho nên tra ở Lịch sử biến động, không được trình bày như field progress trong Tiến trình Work Order. Invoice vẫn dùng net consumed sau RETURN.

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

Authenticated user chỉ thao tác notification của chính identity trong tenant. Bell notification chỉ dùng cho sự kiện cần chú ý/hành động: Dispatcher nhận hàng chờ điều phối/chờ phụ tùng/mở lại; Customer Service nhận Work Order vừa hoàn thành để follow-up; Technician nhận phân công/thay đổi lịch/chuyển giao/mở lại/hủy/đóng khi do người khác thực hiện; Warehouse nhận low-stock; Owner chỉ nhận attention events như `REOPENED`/`CANCELLED`, low-stock threshold crossing và stocktake discrepancy. CRUD/master-data/import/attachment bình thường không tạo bell notification. Low-stock do CONSUME chỉ phát khi tồn vừa cross `reorderLevel`, không lặp lại ở mỗi lần consume khi part đã ở mức thấp. User-facing copy được chuẩn hóa tập trung và không dùng enum/raw technical strings làm nội dung chính:

- `GET /notifications`
- `GET /notifications/unread-count`
- `PATCH /notifications/{id}/read`
- `PATCH /notifications/{id}/unread` — đánh dấu lại thông báo của chính người dùng là chưa đọc.

## AI assistance

- `POST /ai/service-request-draft` — OWNER / CUSTOMER_SERVICE.
- `POST /ai/help` — tất cả năm business roles; backend suy ra role từ JWT, cung cấp role-scoped knowledge base và chặn hướng dẫn ngoài phạm vi. Câu hỏi tổng quát trả overview đúng chức năng của role hiện tại; OWNER nhận overview quản trị rộng nhưng vẫn không được hướng dẫn giả lập field progress/consume thay Technician.

### Inventory movement traceability
Inventory transaction responses include `createdBy`, `actorDisplayName`, `actorRole`, Work Order code/summary, note, quantity, and balance-after. New Work Order part consumption requires a non-blank `note` describing the usage purpose.
