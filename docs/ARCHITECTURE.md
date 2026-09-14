# Kiến trúc ServiceOps

## 1. Tổng thể

ServiceOps là modular monolith:

```text
React / TypeScript
       ↓ REST /api/v1
Spring Boot
       ↓
Application services + Domain model
       ↓
Spring Data JPA / JdbcTemplate
       ↓
PostgreSQL
```

Các module backend chính: `identity`, `customer`, `asset`, `servicerequest`, `workorder`, `scheduling`, `inventory`, `payment`, `attachment`, `notification`, `audit`, `ai`, `dashboard`, `technician`, `tenant`.

Mỗi module tách `web`, `application`, `domain` theo nhu cầu. Controller làm HTTP boundary; rule nghiệp vụ và transaction nằm ở application/domain service; repository chỉ phụ trách persistence/query.

Frontend tổ chức theo `src/features/*`; API, component/page, types và presentation logic tách theo feature. Route/menu chỉ là presentation guard, backend vẫn xác thực quyền thật.

## 2. Authentication, authorization và tenant

- Spring Security chạy stateless JWT.
- JWT dùng HS256, issuer validation và `ActiveUserJwtValidator` để từ chối tài khoản đã bị vô hiệu hóa.
- Password dùng BCrypt cost 12.
- Role được lấy từ claim `roles`; method/application policy tiếp tục kiểm tra ownership và business state.
- Dữ liệu nghiệp vụ mang `tenantId`; repository/service query theo tenant hiện tại để tránh truy cập chéo tenant.
- Technician ownership được suy ra từ user trong JWT, không tin `technicianId` do client tự gửi cho các thao tác thuộc cá nhân kỹ thuật viên.
- CORS đọc từ `CORS_ALLOWED_ORIGINS`; production không dùng wildcard.
- Global exception handler trả message an toàn; lỗi hạ tầng không được đưa SQL/stack trace ra client.

## 3. Database và transaction

JPA chạy `ddl-auto=validate`; Flyway là nguồn thay đổi schema. Repository hiện có migration `V1` đến `V20` và migration phải append-only.

Các workflow cần bảo toàn invariant dùng transaction/locking ở application layer, đặc biệt:

- schedule/redispatch;
- cấp/hoàn kho;
- settlement/receipt/closure;
- đồng bộ trạng thái Technician account/profile;
- sinh business code.

Mã `KH`, `WO`, `PT`, `BN` được `BusinessCodeGenerator` sinh trong transaction bằng PostgreSQL `INSERT ... ON CONFLICT DO UPDATE ... RETURNING` trên counter `(tenant_id, code_type, business_date)`. Ngày nghiệp vụ dùng `BUSINESS_TIME_ZONE`.

Hibernate JDBC time zone được đặt UTC; timezone nghiệp vụ chỉ dùng tại các boundary cần ngày/giờ hiển thị hoặc sinh mã.

## 4. Work Order và billing

`WorkOrderStatus` gồm:

`DRAFT`, `OPEN`, `SCHEDULED`, `ASSIGNED`, `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`, `CUSTOMER_ACCEPTED`, `CLOSED`, `CANCELLED`, `REOPENED`.

Generic transition được giới hạn theo role bởi `WorkOrderAccessPolicy`. Customer acceptance, payment và closure là use case riêng thay vì cho phép client chuyển thẳng sang trạng thái cuối.

Khi khách xác nhận, hệ thống tạo billing snapshot. Receipt/closure dựa trên payment đã `SETTLED`, vì vậy catalog price hoặc inventory return về sau không thay đổi số tiền đã được xác nhận.

## 5. Inventory

Inventory tách ba khái niệm:

- part request: nhu cầu vật tư của Technician;
- inventory transaction: biến động stock (`ISSUE`, `RETURN`; `CONSUME` chỉ còn compatibility lịch sử);
- actual used: lượng thực tế dùng cho Work Order.

`ISSUE` là điểm stock giảm. `USED` không giảm stock lần hai. `RETURN` tăng stock và bị chặn nếu vượt outstanding. Service dùng validation + transaction/locking để tránh stock âm hoặc double issue.

## 6. Audit và notification

Audit giữ actor snapshot, action, entity và detail phục vụ truy vết. Presentation layer ưu tiên tên nghiệp vụ + business code thay vì UUID/raw enum khi có dữ liệu tương ứng.

Notification chỉ dùng cho sự kiện cần người khác chú ý/xử lý; CRUD thường ngày không broadcast. Copy runtime đi qua `NotificationCopy` thay vì ghép câu ở từng service/component. Notification failure được cô lập ở các handler đã thiết kế để không làm hỏng transaction nghiệp vụ chính.

Audit lịch sử đã ghi được xem là immutable; không rewrite text cũ chỉ để đổi wording.

## 7. AI

`AiSuggestionService` chỉ chuẩn hóa `title` và `description` của Service Request. Priority và channel vẫn do người dùng chọn.

`AiHelpService`:

- lấy role từ JWT;
- dùng knowledge base theo role/path;
- không đọc live database;
- không mutation dữ liệu;
- chặn yêu cầu tiết lộ prompt, token, secret, env hoặc cấu hình nội bộ;
- dùng Gemini khi được cấu hình, nếu provider lỗi thì fallback nội bộ;
- audit nguồn xử lý.

Frontend không nhận Gemini API key.

## 8. File/attachment

Attachment đi qua `AttachmentService` và storage abstraction. Local adapter lưu file trên filesystem, có kiểm tra ownership, loại file/signature, size và quota tenant.

Đây là thiết kế single-node. Nếu triển khai nhiều application instance, storage cần chuyển sang object storage dùng chung.

## 9. Frontend state và presentation

- TanStack Query quản lý server state.
- Mutation invalidates query liên quan thay vì duy trì bản sao state nghiệp vụ ở client.
- `routeAccess.ts` và navigation config quản lý visibility theo role; backend vẫn quyết định authorization.
- `businessText.ts`, status presentation và notification presentation chuyển technical token sang wording người dùng.
- Các field business code do server quản lý được hiển thị read-only/disabled ở form phù hợp.

## 10. Production/runtime

Production profile cấu hình Hikari pool mặc định `maximum-pool-size=10`, graceful shutdown, forwarded headers và Actuator readiness/liveness. Docker backend chạy Java 21 JRE với `MaxRAMPercentage=75`.

Production-like Compose gồm PostgreSQL 17, Spring Boot backend và Nginx frontend/reverse proxy. Nginx thêm các security header, proxy `/api`, health và tùy chọn Swagger.

CI dựng chính topology này trước khi chạy browser E2E.

## 11. Giới hạn hiện tại

Các giới hạn này là đặc điểm của phiên bản hiện tại, không phải lỗi đã che giấu:

- local filesystem attachment phù hợp single-node, chưa phù hợp horizontal scaling;
- login attempt tracking hiện phù hợp single-instance, multi-instance nên chuyển sang shared store;
- active-user JWT validation có database lookup theo request;
- một số dashboard/search query chưa được benchmark ở dữ liệu lớn;
- chưa có load-test benchmark nên không tuyên bố throughput/concurrency ở quy mô lớn.

Chỉ tối ưu các điểm trên khi có yêu cầu tải hoặc topology cụ thể.
