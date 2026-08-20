# Danh mục API

Base URL local: `http://localhost:8080/api/v1`

## Quy ước tra cứu và phân trang
- Các danh sách nghiệp vụ lớn dùng **server-side search + pagination**; frontend không tải toàn bộ dữ liệu rồi mới lọc.
- `page` bắt đầu từ `0`; UI chính dùng `20` dòng/trang. Backend chuẩn hóa `page >= 0`; `size <= 0` quay về mặc định `20` và mọi request bị giới hạn tối đa `100` dòng/trang để tránh tải quá lớn.
- `search` được trim và so khớp không phân biệt hoa/thường trên các trường nghiệp vụ có ý nghĩa:
  - Khách hàng: tên, mã, số điện thoại, email.
  - Thiết bị: serial, loại, hãng, model, mã/tên khách hàng.
  - Yêu cầu dịch vụ: tiêu đề, mô tả, khách hàng, serial thiết bị.
  - Phiếu công việc/lịch sử phiếu: mã phiếu, tóm tắt, mô tả, khách hàng, serial, tên/username kỹ thuật viên.
  - Kho phụ tùng: SKU, tên, đơn vị.
  - Audit: người thao tác, hành động, loại đối tượng, chi tiết và UUID đối tượng; có thêm bộ lọc ngày/actor/action/entity.
- Search text trên UI debounce ngắn trước khi gọi API; khi search/filter đổi, trang quay về trang đầu. Lỗi API được hiển thị rõ và không bị trình bày nhầm thành “không có dữ liệu”.
- Các danh sách nhỏ/bị chặn tự nhiên như Người dùng, Kỹ thuật viên và Kênh tiếp nhận vẫn tìm ngay trên dữ liệu đã tải để UX phản hồi tức thời; chúng dùng cùng page size và error state nhưng không ép thêm API pagination khi chưa có nhu cầu thực tế.

## Authentication
- `POST /auth/login`

## Dashboard
- `GET /dashboard`

## Customers
- `GET /customers?search={text}&page={n}&size={n}`
- `POST /customers`
- `GET /customers/{id}`
- `PUT /customers/{id}`

## Assets
- `GET /assets?search={text}&customerId={uuid}&page={n}&size={n}` — `customerId` là bộ lọc tùy chọn cho các form chọn thiết bị theo khách hàng
- `POST /assets`
- `GET /assets/{id}`
- `PUT /assets/{id}`

## Service requests
- `GET /service-requests?search={text}&status={status}&page={n}&size={n}`
- `POST /service-requests`
- `POST /work-orders/from-service-request/{serviceRequestId}`
- `POST /service-requests/{id}/cancel`

## Work orders
- `GET /work-orders?search={text}&status={status}&page={n}&size={n}`
- `GET /work-orders/history?search={text}&status={CLOSED|CANCELLED}&page={n}&size={n}`
- `POST /work-orders`
- `GET /work-orders/{id}`
- `POST /work-orders/{id}/schedule`
- `POST /work-orders/{id}/transition`

## Scheduling
- `GET /schedule-board?from={instant}&to={instant}` — OWNER/DISPATCHER; lịch đội kỹ thuật và hàng đợi điều phối, tối đa 31 ngày mỗi lần tải
- `GET /my-schedule?from={instant}&to={instant}` — TECHNICIAN; backend tự suy ra hồ sơ kỹ thuật viên từ JWT, không nhận `technicianId` từ client

## Technicians
- `GET /technicians`

## Inventory
- `GET /spare-parts?search={text}&page={n}&size={n}`
- `POST /spare-parts`
- `POST /spare-parts/{id}/import`
- `POST /work-orders/{workOrderId}/parts/consume`

## Files
- `POST /attachments`
- `GET /attachments?referenceType={type}&referenceId={id}`
- `GET /attachments/{id}/download`

## Audit và notification
- `GET /audit-logs?page=0&size=20&q={keyword}&actor={username}&action={action}&entityType={type}&from={ISO-8601}&to={ISO-8601}`
  - Tenant-scoped, sắp xếp mới nhất trước, tối đa 100 dòng/trang.
  - `q` tìm trên người thao tác, hành động, loại đối tượng, chi tiết và UUID đối tượng.
  - UI mặc định lọc 30 ngày gần nhất và tải 20 dòng/trang; bỏ khoảng ngày để tra cứu toàn bộ lịch sử.
- `GET /notifications`
- `GET /notifications/unread-count`
- `PATCH /notifications/{id}/read`

Swagger là tài liệu request/response chính xác nhất khi backend chạy: `http://localhost:8080/swagger-ui.html`.
