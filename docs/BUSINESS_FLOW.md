# Quy trình nghiệp vụ ServiceOps

Tài liệu này tóm tắt business rule đang được thực thi trong backend/frontend hiện tại.

## 1. Ownership theo vai trò

### OWNER

Quản trị người dùng, cấu hình thanh toán, giám sát dashboard, customer/asset, Service Request, Work Order, scheduling, inventory, history và audit. Owner có quyền quản trị rộng nhưng không thay role hiện trường để ghi tiến độ/actual-used/settlement routine.

### CUSTOMER_SERVICE

Quản lý customer/asset, tiếp nhận Service Request, chuyển Service Request thành Work Order, theo dõi sau dịch vụ, xử lý payment reconciliation, phát hành receipt và đóng hồ sơ khi đủ điều kiện.

### DISPATCHER

Xem ngữ cảnh customer/asset cần cho điều phối, xem Work Order/Technician, assign/schedule/reschedule và hủy theo policy. Không tiếp nhận Service Request hoặc settlement payment.

### TECHNICIAN

Chỉ thao tác Work Order được giao: xem lịch cá nhân, cập nhật field progress, request part, ghi actual-used, diagnosis/resolution, hoàn thành công việc, xác nhận khách và tạo payment handoff phù hợp.

### WAREHOUSE_STAFF

Xử lý yêu cầu phụ tùng, danh mục/tồn kho, stocktake, inventory movement, ISSUE/RETURN. Không tham gia operational Work Order dashboard.

## 2. Service Request → Work Order

1. Customer Service chọn/tạo Customer và Asset phù hợp.
2. Tạo Service Request với nội dung, priority và channel.
3. Service Request ở `OPEN`; không tự sinh Work Order.
4. Khi hồ sơ đủ điều kiện, Customer Service thực hiện convert.
5. Backend tạo Work Order và chuyển Service Request sang `CONVERTED`.
6. Nếu không tiếp tục xử lý, Service Request chuyển `CANCELLED` thay vì hard delete.

Asset phải thuộc đúng Customer; backend kiểm invariant này, không chỉ dựa UI.

Customer inactive vẫn được giữ để bảo toàn lịch sử nhưng không được dùng cho yêu cầu/asset mới.

## 3. Điều phối và thực hiện Work Order

Luồng vận hành thông thường:

```text
OPEN
 → SCHEDULED / ASSIGNED
 → ON_THE_WAY
 → IN_PROGRESS
 → WAITING_FOR_PARTS (khi cần)
 → IN_PROGRESS
 → COMPLETED
 → CUSTOMER_ACCEPTED
 → CLOSED
```

`REOPENED` và `CANCELLED` là nhánh ngoại lệ có policy riêng.

- Dispatcher assign/schedule Technician và khoảng thời gian thực hiện.
- Hệ thống kiểm overlap để tránh double-book Technician.
- Khi field work đã bắt đầu, redispatch/reschedule bị giới hạn theo business rule.
- Technician chỉ được chuyển sang các field status: `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`.
- Reopen/cancel yêu cầu role đúng và lý do khi policy bắt buộc.
- `CLOSED` không dùng generic transition button; closure đi qua use case riêng sau settlement.

## 4. Phụ tùng

Flow hiện tại:

```text
Technician REQUESTED
        ↓
Warehouse ISSUE
        ↓
Technician USED
        ↓
Warehouse RETURN phần dư (nếu có)
```

Quy tắc:

- `REQUESTED` không thay đổi stock.
- `ISSUE` là stock-out thực tế.
- `USED` chỉ ghi lượng dùng cho công việc, không trừ stock lần nữa.
- `RETURN` là stock-in và không được vượt lượng còn outstanding.
- Không cho stock âm, double issue hoặc dùng part chưa được cấp.
- Part catalog dùng active/inactive; không hard delete record đang cần cho lịch sử.
- `CONSUME` chỉ còn để đọc dữ liệu lịch sử, UI/API hiện hành không tạo transaction này.

## 5. Hoàn thành, billing và xác nhận khách

Trước `COMPLETED`, Technician phải hoàn thiện thông tin kết quả theo form hiện tại (diagnosis/resolution và dữ liệu liên quan).

Billing được tính từ labor/part/fee theo dữ liệu đã ghi cho Work Order. Khi thực hiện customer acceptance:

1. hệ thống kiểm billing hiện tại;
2. tạo snapshot số tiền và dòng chi tiết;
3. chuyển Work Order sang `CUSTOMER_ACCEPTED`;
4. các thay đổi catalog hoặc return vật tư sau đó không làm thay đổi snapshot đã chấp nhận.

Nếu Work Order được reopen và hoàn thành lại, history giữ snapshot các repair cycle thay vì ghi đè lịch sử cũ.

## 6. Payment, receipt và closure

Ba flow payment:

```text
TRANSFER_PENDING_VERIFICATION
  → Customer Service verify
  → SETTLED

CASH_PENDING_HANDOVER
  → Customer Service xác nhận nhận tiền
  → SETTLED

COUNTER_PAYMENT_PENDING
  → Customer Service xác nhận thu tiền tại quầy
  → SETTLED
```

Quy tắc:

- Payment chỉ được tạo trong đúng lifecycle Work Order.
- Settlement action kiểm đúng pending state tương ứng.
- Receipt chỉ phát hành khi payment `SETTLED` và có settlement data hợp lệ.
- Closure yêu cầu Work Order `CUSTOMER_ACCEPTED` và payment `SETTLED`.
- Receipt issuance/closure được thiết kế idempotent để tránh phát hành lặp khi request được gửi lại.

Business code receipt có dạng `BN-YYYYMMDD-NNN`.

## 7. History và audit

Work Order history tách nhóm hồ sơ:

- Chờ hoàn tất hồ sơ: `CUSTOMER_ACCEPTED`;
- Đã đóng: `CLOSED`;
- Đã hủy: `CANCELLED`.

Summary count lấy từ backend query, không hard-code ở frontend. Technician chỉ thấy scope được phân công theo policy backend.

Audit giữ actor, role, action, entity và detail. UI ưu tiên tên nghiệp vụ + code; raw UUID/enum chỉ là fallback kỹ thuật khi không còn dữ liệu presentation phù hợp.

## 8. Notification

Notification được gửi theo hành động cần chú ý, không phải mọi CRUD:

- Dispatcher: cần phân công, chờ phụ tùng, reopen, overdue điều phối;
- Technician: assignment, schedule change, reopen/cancel/close liên quan công việc của mình;
- Customer Service: completed follow-up, payment handoff, overdue kéo dài cần liên hệ khách;
- Warehouse: part request và low-stock/stocktake event;
- Owner: một số kết quả cuối hoặc ngoại lệ quản trị, không nhận toàn bộ noise vận hành.

Copy runtime được tạo tập trung bởi `NotificationCopy`; frontend tiếp tục normalize một số legacy notification còn tồn tại.

## 9. AI boundary

- Service Request AI chỉ gợi ý `title` và `description`.
- AI Help chỉ hướng dẫn theo role đã xác thực; không đọc dữ liệu live và không mutation.
- Gemini lỗi hoặc chưa cấu hình thì fallback nội bộ.
- AI không được thay quyết định của business service, không được bypass RBAC/state machine.

## 10. Business code

Các mã do backend sinh, client không tự nhập:

- Customer: `KH-YYYYMMDD-NNN`
- Work Order: `WO-YYYYMMDD-NNN`
- Spare Part: `PT-YYYYMMDD-NNN`
- Payment Receipt: `BN-YYYYMMDD-NNN`

Sequence độc lập theo tenant + loại mã + business date và được cấp bằng counter atomic trong PostgreSQL.

Các field như serial number, `ServiceChannel.code`, tenant code, UUID, role/status enum không được chuẩn hóa theo convention business code này.
