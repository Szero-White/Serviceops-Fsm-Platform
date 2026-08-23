# Quy trình nghiệp vụ

## 1. Từ yêu cầu khách hàng đến đóng công việc

```text
Customer/CSKH tạo Service Request
        ↓
Xác thực khách hàng + thiết bị + mức ưu tiên
        ↓
Chuyển thành Work Order
        ↓
Dispatcher chọn kỹ thuật viên và khung giờ
        ↓
Hệ thống khóa kỹ thuật viên, kiểm tra lịch chồng lấn
        ↓
Technician: ON_THE_WAY → IN_PROGRESS
        ↓
Ghi chẩn đoán, giải pháp, phụ tùng và file minh chứng
        ↓
COMPLETED → CUSTOMER_ACCEPTED → CLOSED
        ↓
Dashboard + lịch sử + audit được cập nhật
```

## 2. State transition

Luồng chính:

```text
OPEN → SCHEDULED → ASSIGNED → ON_THE_WAY → IN_PROGRESS
     → COMPLETED → CUSTOMER_ACCEPTED → CLOSED
```

Nhánh:

```text
IN_PROGRESS → WAITING_FOR_PARTS → IN_PROGRESS
COMPLETED/CUSTOMER_ACCEPTED → REOPENED → IN_PROGRESS
OPEN/SCHEDULED/ASSIGNED/ON_THE_WAY/IN_PROGRESS → CANCELLED
```

Chuyển trạng thái không hợp lệ trả `409 INVALID_STATUS_TRANSITION`.

## 3. Quy tắc xếp lịch

Hai khoảng thời gian chồng lấn khi:

```text
newStart < existingEnd AND newEnd > existingStart
```

Khi hai điều phối viên gửi request đồng thời, pessimistic lock trên technician làm tuần tự hóa thao tác. Request thứ hai sẽ nhìn thấy appointment vừa commit và nhận `409 TECHNICIAN_SCHEDULE_CONFLICT`.

## 4. Quy tắc tồn kho

- Số lượng phải lớn hơn 0.
- Work order đóng/hủy không được dùng thêm phụ tùng.
- Kỹ thuật viên chỉ được dùng phụ tùng cho work order của mình.
- Mỗi thay đổi tạo một dòng `inventory_transactions` với `balance_after`.
- Không sửa stock bằng API CRUD tùy ý.

## 5. Quyền thao tác

- OWNER: quản trị người dùng, cấu hình kênh tiếp nhận, giám sát toàn hệ thống và các thao tác quản lý/override được ủy quyền.
- DISPATCHER: xem customer/asset để phục vụ điều phối; theo dõi work order, kỹ thuật viên, phân công, xếp lịch/reschedule và nghiệp vụ điều phối được phép. Không tiếp nhận Service Request và không tạo Work Order trực tiếp.
- CUSTOMER_SERVICE: quản lý customer/asset, tiếp nhận/chỉnh sửa/hủy Service Request, chuyển Service Request sang điều phối và hủy Work Order đang hoạt động khi khách thay đổi nhu cầu theo policy.
- TECHNICIAN: xem/cập nhật công việc được giao, upload minh chứng và ghi nhận phụ tùng sử dụng cho chính công việc được giao.
- WAREHOUSE_STAFF: quản lý danh mục phụ tùng, nhập kho, tồn kho và vòng đời phụ tùng; không sở hữu luồng Work Order.


## Workspace theo vai trò

- Dispatcher/OWNER dùng **Lịch điều phối** để nhìn toàn đội, xử lý hàng đợi và thay đổi phân công.
- TECHNICIAN dùng **Lịch của tôi**. Backend lấy `userId` từ JWT, ánh xạ sang `technician_profiles.user_id` và chỉ trả appointment của hồ sơ đó.
- Hai kỹ thuật viên cùng role vẫn là hai tài khoản riêng; role xác định quyền, còn `UserAccount` xác định danh tính và audit accountability.
