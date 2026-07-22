# UAT Checklist — Local MVP

Dùng checklist này trước mỗi bản demo hoặc bàn giao thử nghiệm.

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| AUTH-01 | Đăng nhập đúng tài khoản demo | Nhận JWT và vào dashboard |
| AUTH-02 | Đăng nhập sai mật khẩu | HTTP 401, không lộ chi tiết nội bộ |
| RBAC-01 | Technician mở trang khách hàng | Bị từ chối HTTP 403 |
| TENANT-01 | Truy cập ID không thuộc tenant | Không trả dữ liệu |
| CUS-01 | Tạo khách hàng hợp lệ | Khách hàng xuất hiện trong danh sách |
| AST-01 | Tạo thiết bị với serial mới | Thiết bị liên kết đúng khách hàng |
| AST-02 | Tạo trùng serial trong tenant | Bị từ chối HTTP 409 |
| SR-01 | Tạo service request | Trạng thái OPEN |
| WO-01 | Chuyển service request thành work order | Work order OPEN, request được đánh dấu đã chuyển |
| SCH-01 | Gán lịch hợp lệ | Work order ASSIGNED, có appointment |
| SCH-02 | Hai lịch kỹ thuật viên chồng lấn | Request thứ hai nhận HTTP 409 |
| WO-02 | Chuyển trạng thái hợp lệ | Timeline lưu người thao tác và thời gian |
| WO-03 | Nhảy trạng thái không hợp lệ | HTTP 409 INVALID_STATUS_TRANSITION |
| INV-01 | Nhập kho số lượng dương | Tồn và ledger tăng đúng |
| INV-02 | Dùng phụ tùng đủ tồn | Tồn giảm, ledger gắn work order |
| INV-03 | Dùng vượt tồn | HTTP 409, tồn không thay đổi |
| FILE-01 | Upload JPG/PNG/WEBP/PDF dưới 10 MB | File lưu và tải lại được |
| FILE-02 | Upload loại file không cho phép | HTTP 400 INVALID_FILE_TYPE |
| AUD-01 | Thực hiện thao tác quan trọng | Có audit log tương ứng |
| BUILD-01 | Chạy backend test | Build success |
| BUILD-02 | Chạy frontend lint/build | Build success |
