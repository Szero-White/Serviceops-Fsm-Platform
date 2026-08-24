# Quy trình nghiệp vụ

## 1. Từ yêu cầu khách hàng đến đóng công việc

```text
Customer/CSKH tạo Service Request
        ↓
Xác thực khách hàng + thiết bị + mức ưu tiên
        ↓
Customer Service/Owner chuyển Service Request thành Work Order
        ↓
Dispatcher chọn kỹ thuật viên và khung giờ
        ↓
Hệ thống khóa kỹ thuật viên, kiểm tra lịch chồng lấn
        ↓
Technician: ON_THE_WAY → IN_PROGRESS
        ↓
Ghi chẩn đoán, giải pháp, phụ tùng và file minh chứng
        ↓
COMPLETED
        ↓
Owner: CUSTOMER_ACCEPTED → CLOSED
        ↓
Dashboard + lịch sử + audit + notification được cập nhật
```

Work Order public flow **không có generic direct-create API**. Nguồn tạo chuẩn là `Service Request → Work Order`; demo seed cũng giữ source Service Request cho các Work Order mẫu.

## 2. State transition

Luồng chính:

```text
OPEN → SCHEDULED → ASSIGNED → ON_THE_WAY → IN_PROGRESS
     → COMPLETED → CUSTOMER_ACCEPTED → CLOSED
```

Nhánh:

```text
IN_PROGRESS → WAITING_FOR_PARTS → IN_PROGRESS
COMPLETED/CUSTOMER_ACCEPTED → REOPENED → IN_PROGRESS hoặc SCHEDULED
CANCELLED → REOPENED (management-controlled)
```

Các trạng thái active có thể hủy theo policy:

```text
OPEN / SCHEDULED / ASSIGNED / ON_THE_WAY /
IN_PROGRESS / WAITING_FOR_PARTS / REOPENED → CANCELLED
```

Role ownership của transition:

- `TECHNICIAN`: `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED` trên Work Order được giao cho chính mình.
- `CUSTOMER_SERVICE`: `CANCELLED` khi khách thay đổi nhu cầu; yêu cầu lý do hủy.
- `DISPATCHER`: điều phối/schedule/reschedule và operational cancellation; không thực hiện field progress, customer acceptance, close hoặc reopen.
- `OWNER`: management transitions/override được phép bởi state machine, bao gồm acceptance/close/reopen.

Transition không hợp lệ về state trả `409 INVALID_STATUS_TRANSITION`; transition đúng state nhưng sai role trả `403 WORK_ORDER_TRANSITION_FORBIDDEN`.

## 3. Quy tắc xếp lịch

Hai khoảng thời gian chồng lấn khi:

```text
newStart < existingEnd AND newEnd > existingStart
```

Khi hai điều phối viên gửi request đồng thời, pessimistic lock trên technician làm tuần tự hóa thao tác. Request thứ hai nhìn thấy appointment vừa commit và nhận `409 TECHNICIAN_SCHEDULE_CONFLICT`.

Kỹ thuật viên đang bị tạm ngưng, account inactive hoặc không còn role `TECHNICIAN` không thể nhận lịch mới. Owner cũng không được tạm ngưng account/profile kỹ thuật viên khi còn Work Order operational đang gán cho người đó; phải điều phối lại hoặc hủy công việc trước.

## 4. Quy tắc tồn kho

- Số lượng phải lớn hơn 0.
- Work Order `CLOSED`/`CANCELLED` không được dùng thêm phụ tùng.
- Technician chỉ consume phụ tùng cho Work Order được giao cho mình.
- Warehouse quản lý catalog/import/lifecycle stock nhưng không consume thay Technician trong operational Work Order flow.
- Mỗi thay đổi tạo `inventory_transactions` với `balance_after`.
- Locking + validation + transaction ngăn stock âm khi consume đồng thời.

## 5. Quyền thao tác

- `OWNER`: quản trị người dùng, cấu hình kênh tiếp nhận, hồ sơ kỹ thuật viên, dashboard, audit, visibility toàn hệ thống và management actions.
- `DISPATCHER`: Customer/Asset read-only; xem Work Order, kỹ thuật viên; assign/schedule/reschedule; operational cancellation; xem lịch sử/audit. Không tiếp nhận Service Request, không sửa hồ sơ kỹ thuật viên, không nghiệm thu/close/reopen.
- `CUSTOMER_SERVICE`: Customer/Asset create-update-delete theo guard; Service Request intake/update/cancel/delete; chuyển Service Request sang Work Order; hủy Work Order active theo policy.
- `TECHNICIAN`: My Schedule + Work Order được giao; field transitions; evidence; consume spare part cho chính job.
- `WAREHOUSE_STAFF`: `/inventory` và API kho; không có Work Order operational API hoặc operational dashboard.

## 6. Delete / cancel / deactivate

- Service Request và Work Order nghiệp vụ ưu tiên state (`CANCELLED`) thay cho hard delete khi đã có lịch sử vận hành.
- Work Order `CLOSED`/`CANCELLED` chỉ Owner được ẩn khỏi lịch sử tra cứu; audit vẫn được giữ.
- Asset/Service Request chưa có operational reference vẫn không được hard-delete nếu còn attachment; phải xử lý attachment trước để tránh orphan metadata/file.
- Technician có assignment operational không được deactivate.

## 7. Workspace theo vai trò

- Owner/Dispatcher dùng **Lịch điều phối**.
- Technician dùng **Lịch của tôi**; backend suy ra `TechnicianProfile` từ JWT `userId`, client không gửi `technicianId`.
- Warehouse đăng nhập/điều hướng mặc định vào **Kho phụ tùng**, không vào operational dashboard.
- Hai Technician cùng role vẫn là hai identity riêng; role quyết định quyền, `UserAccount` quyết định ownership/audit accountability.
