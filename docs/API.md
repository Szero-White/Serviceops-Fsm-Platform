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
- WAREHOUSE_STAFF không nhận operational dashboard; frontend mặc định đưa Warehouse tới `/part-requests` để xử lý hàng đợi yêu cầu phụ tùng.

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
- `GET /work-orders/{id}` — detail Work Order và status history tương thích.
- `GET /work-orders/{id}/timeline` — read model business timeline hợp nhất status, điều phối, REQUEST/ISSUE/USED/RETURN, payment reconciliation và receipt theo thời gian; nguồn dữ liệu gốc vẫn nằm ở từng module, không tạo bảng timeline duplicate.
- `POST /work-orders/{id}/close` — CUSTOMER_SERVICE only; chỉ thành công khi WO `CUSTOMER_ACCEPTED` và payment `SETTLED`. Closure bảo đảm biên nhận đã được phát hành theo cơ chế idempotent trước khi chuyển sang `CLOSED`.
- `POST /work-orders/{id}/receipt` — CUSTOMER_SERVICE phát hành/tải biên nhận sau `SETTLED`; lần gọi sau idempotent dùng receipt snapshot đã có.
- `GET /work-orders/{id}/receipt` — OWNER / CUSTOMER_SERVICE tải biên nhận đã phát hành.

TECHNICIAN read được giới hạn tiếp theo ở service/repository vào Work Order được assign cho identity hiện tại.

Mutations:

- `POST /work-orders/from-service-request/{serviceRequestId}` — OWNER / CUSTOMER_SERVICE
- `POST /work-orders/{id}/schedule` — OWNER/DISPATCHER; lần phân công đầu không cần `reason`, nhưng mọi lần điều phối lại kỹ thuật viên/lịch phải gửi `reason` (tối đa 500 ký tự). Chỉ cho phép khi WO còn `OPEN`, `SCHEDULED`, `ASSIGNED` hoặc `REOPENED`; sau khi field work bắt đầu thì bị từ chối.
- `POST /work-orders/{id}/transition` — OWNER / DISPATCHER / CUSTOMER_SERVICE / TECHNICIAN; service áp role-specific target-status policy và Technician vẫn bị giới hạn vào Work Order được giao.
- `DELETE /work-orders/{id}` — OWNER only; chỉ soft-hide/archive Work Order `CLOSED`/`CANCELLED` khỏi history query

Transition ownership:

- TECHNICIAN: `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`; customer acceptance dùng action riêng sau khi Technician kiểm tra actual-used + billing draft.
- OWNER: supervisory/admin scope; không giả lập field progress, customer acceptance, payment reconciliation hoặc physical stock movement.
- CUSTOMER_SERVICE: `REOPENED`, `CANCELLED` trước customer acceptance; sau payment `SETTLED` dùng action `close` riêng để đóng phiếu.
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
- `PATCH /spare-parts/{id}/reorder-level` — cập nhật `reorderLevel` (UI: **Ngưỡng tồn tối thiểu**). Không thay đổi stock và không tạo inventory transaction; thay đổi được audit. Nếu ngưỡng mới làm tồn hiện tại chuyển từ bình thường sang tồn thấp, backend phát low-stock notification sau commit cho WAREHOUSE_STAFF khác người thao tác; OWNER không nhận cảnh báo tồn vận hành.
- `DELETE /spare-parts/{id}`
- `GET /spare-parts/import-template`
- `POST /spare-parts/import?commit={bool}`
- `POST /spare-parts/{id}/import`

Stock reconciliation and traceability — OWNER / WAREHOUSE_STAFF:

- `POST /spare-parts/{id}/stocktake` — nhập số lượng đếm thực tế; backend tạo `ADJUSTMENT_IN` hoặc `ADJUSTMENT_OUT` khi có chênh lệch. Notification được phát qua application event sau commit: Owner nhận chênh lệch, Warehouse nhận thêm cảnh báo nếu tồn xuống **ngưỡng tồn tối thiểu**.
- `GET /inventory-transactions` — phân trang/filter theo keyword, loại giao dịch và khoảng thời gian; transaction workflow mới dùng `ISSUE` khi Warehouse giao phụ tùng thực tế và `RETURN` khi Warehouse nhận lại.
- `GET /work-orders/{workOrderId}/parts/{sparePartId}/returnable` — số lượng còn có thể hoàn; workflow mới tính `ISSUE - USED - RETURN`, đồng thời vẫn đọc dữ liệu `CONSUME - RETURN` legacy để tương thích lịch sử.
- `POST /work-orders/{workOrderId}/parts/{sparePartId}/return` — WAREHOUSE_STAFF xác nhận nhận lại phụ tùng chưa sử dụng; lý do bắt buộc, số lượng không được vượt outstanding. RETURN hợp lệ vẫn được phép sau khi Work Order đã `CLOSED` và không làm mở lại phiếu.

Work Order part request / issue / actual usage:

- `GET /part-requests` — OWNER / WAREHOUSE_STAFF xem hàng đợi và lịch sử yêu cầu phụ tùng; hỗ trợ status/search/pagination.
- `GET /work-orders/{workOrderId}/part-requests` — các role vận hành được xem lịch sử yêu cầu của một Work Order.
- `POST /work-orders/{workOrderId}/part-requests` — TECHNICIAN được phân công tạo yêu cầu; **không giảm tồn kho**.
- `PATCH /part-requests/{requestId}` — TECHNICIAN sửa quantity/note khi request vẫn `REQUESTED`.
- `POST /part-requests/{requestId}/cancel` — TECHNICIAN hủy request đang chờ; reason bắt buộc, không hard-delete.
- `POST /part-requests/{requestId}/unavailable` — WAREHOUSE_STAFF ghi nhận không thể cấp; reason bắt buộc, không tạo stock movement.
- `POST /part-requests/{requestId}/issue` — WAREHOUSE_STAFF xác nhận giao đúng requested quantity; lúc này mới tạo `ISSUE` và giảm stock.
- `GET /work-orders/{workOrderId}/part-usage` — xem tổng `ISSUE`, `USED`, `RETURN`, `OUTSTANDING` theo Work Order + part.
- `PUT /work-orders/{workOrderId}/part-usage` — TECHNICIAN ghi actual used; không tạo stock movement và không được vượt `ISSUE - RETURN`. Cho phép đến trạng thái `COMPLETED`, khóa sau customer acceptance.

Legacy compatibility:

- `POST /work-orders/{workOrderId}/parts/consume` vẫn được giữ tạm để đọc/duy trì frontend hoặc dữ liệu cũ trong giai đoạn migration. UI mới **không sử dụng endpoint này**. Không xóa lịch sử `CONSUME` cũ.

## Attachments

- `POST /attachments` — multipart `referenceType`, `referenceId`, optional `purpose`, `file`
- `GET /attachments?referenceType={WORK_ORDER|ASSET|SERVICE_REQUEST}&referenceId={id}`
- `GET /attachments/{id}/download`
- `PATCH /attachments/{id}` — rename khi attachment còn mutable
- `DELETE /attachments/{id}` — xóa khi attachment còn mutable

`purpose` tách lifecycle nghiệp vụ:

- `WORK_EVIDENCE`: ảnh/PDF hồ sơ sửa chữa. OWNER hoặc assigned TECHNICIAN được upload khi Work Order chưa `CUSTOMER_ACCEPTED`/`CLOSED`/`CANCELLED`; uploader hoặc OWNER quản lý khi còn mutable. Sau khi hồ sơ finalized chỉ xem/tải.
- `PAYMENT_EVIDENCE`: ảnh JPG/PNG/WEBP do assigned TECHNICIAN chuẩn bị ở bước thanh toán. Khi `reportTransfer` liên kết ảnh với Payment, attachment bị lock và không còn rename/delete. Evidence không hiển thị lẫn trong tab hồ sơ sửa chữa.
- `GENERAL`: attachment của Asset/Service Request/Company Payment Profile.

Authorization nằm trong AttachmentService theo reference:

- WORK_ORDER: OWNER / DISPATCHER / CUSTOMER_SERVICE; TECHNICIAN chỉ assigned Work Order.
- ASSET: OWNER / DISPATCHER / CUSTOMER_SERVICE.
- SERVICE_REQUEST: OWNER / CUSTOMER_SERVICE.
- WAREHOUSE_STAFF bị chặn khỏi operational references.

## Audit

- `GET /audit-logs?page={n}&size={n}&q={keyword}&actor={username}&action={action}&entityType={type}&from={ISO-8601}&to={ISO-8601}` — OWNER only.

## Notifications

Authenticated user chỉ thao tác notification của chính identity trong tenant. Bell notification chỉ dùng cho sự kiện cần chú ý/hành động: Dispatcher nhận hàng chờ điều phối/chờ phụ tùng/mở lại; Customer Service nhận Work Order vừa hoàn thành để follow-up; Technician nhận phân công/thay đổi lịch/chuyển giao/mở lại/hủy/đóng khi do người khác thực hiện; Warehouse nhận **yêu cầu phụ tùng mới** cần xử lý và low-stock; Owner chỉ nhận terminal outcomes `CLOSED`/`CANCELLED` và stocktake discrepancy; không nhận `REOPENED`, overdue hoặc low-stock vận hành. CRUD/master-data/import/attachment bình thường không tạo bell notification. Low-stock do `ISSUE` workflow mới (và `CONSUME` legacy trong giai đoạn tương thích) chỉ phát khi tồn vừa cross `reorderLevel`, không lặp lại khi part đã ở mức thấp. User-facing copy được chuẩn hóa tập trung và không dùng enum/raw technical strings làm nội dung chính:

- `GET /notifications`
- `GET /notifications/unread-count`
- `PATCH /notifications/{id}/read`
- `PATCH /notifications/{id}/unread` — đánh dấu lại thông báo của chính người dùng là chưa đọc.

## AI assistance

- `POST /ai/service-request-draft` — OWNER / CUSTOMER_SERVICE.
- `POST /ai/help` — tất cả năm business roles; backend suy ra role từ JWT, cung cấp role-scoped knowledge base và chặn hướng dẫn ngoài phạm vi. Câu hỏi tổng quát trả overview đúng chức năng của role hiện tại; AI nhận biết các workspace mới như `/part-requests`, `/payments`, `/payment-settings`, `/work-order-history`; OWNER nhận overview giám sát/quản trị rộng nhưng vẫn không được hướng dẫn giả lập field progress, xác nhận ISSUE/RETURN, customer acceptance hoặc settlement thay role phụ trách.

### Inventory movement traceability
Inventory transaction responses include `createdBy`, `actorDisplayName`, `actorRole`, Work Order code/summary, note, quantity, and balance-after. Workflow hiện hành ghi stock movement tại `ISSUE`/`RETURN`; Technician lưu mục đích ở part request và actual `USED` được theo dõi riêng, không tạo thêm inventory transaction.
