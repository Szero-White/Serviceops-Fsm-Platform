# Hướng dẫn sử dụng bản demo

## 1. Chọn tài khoản theo vai trò

- `owner`: xem toàn hệ thống, dùng khi demo tổng thể.
- `dispatcher`: quản lý bảng điều phối tuần, gán kỹ thuật viên và xếp lịch work order.
- `customer-service`: tạo khách hàng, thiết bị và yêu cầu dịch vụ.
- `technician`: tài khoản cá nhân của Phạm Quốc; chỉ xem lịch và công việc được giao cho chính mình.
- `technician-2`: tài khoản cá nhân của Võ Hoàng; dùng để kiểm tra dữ liệu lịch không bị lẫn giữa kỹ thuật viên.
- `warehouse`: tạo phụ tùng và nhập kho.

Mật khẩu local cho các tài khoản demo: `123456`. Đây chỉ là credential dùng chung để thuận tiện demo; mỗi username vẫn là một người dùng riêng với dữ liệu/quyền riêng.

## 2. Kịch bản demo chuẩn

### Bước 1 — Tiếp nhận khách hàng

1. Đăng nhập `customer-service`.
2. Vào **Khách hàng** và tạo hồ sơ mới.
3. Vào **Thiết bị**, chọn khách hàng, nhập loại thiết bị, hãng, model, serial và hạn bảo hành.
4. Vào **Yêu cầu dịch vụ**, tạo yêu cầu với mức ưu tiên và mô tả lỗi.

### Bước 2 — Tạo và điều phối công việc

1. Đăng nhập `dispatcher`.
2. Mở **Yêu cầu dịch vụ** và chuyển yêu cầu thành work order.
3. Mở **Lịch điều phối**, chọn phiếu trong hàng đợi, kỹ thuật viên và khung thời gian.
4. Bấm một lịch đã có trên board để đổi kỹ thuật viên hoặc thời gian khi cần.
5. Nếu lịch chồng lấn, hệ thống từ chối và hiển thị lỗi nghiệp vụ.

### Bước 3 — Kỹ thuật viên thực hiện

1. Đăng nhập `technician`.
2. Mở **Lịch của tôi**; lịch được lấy theo tài khoản đang đăng nhập, không chọn `technicianId` thủ công.
3. Mở **Phiếu công việc**; chỉ các work order được giao cho tài khoản này mới xuất hiện.
4. Chuyển trạng thái lần lượt `ON_THE_WAY` và `IN_PROGRESS`.
5. Upload ảnh/PDF minh chứng.
6. Ghi nhận phụ tùng đã sử dụng. Hệ thống không cho số lượng tồn âm.
7. Nhập chẩn đoán và giải pháp, sau đó chuyển sang `COMPLETED`.

### Bước 4 — Nghiệm thu và theo dõi

1. Đăng nhập `owner` hoặc `dispatcher`.
2. Chuyển `COMPLETED → CUSTOMER_ACCEPTED → CLOSED`.
3. Xem timeline trạng thái, dashboard, notification và audit log.

## 3. Quy tắc người dùng cần biết

- Work order phải đi đúng vòng đời; không thể nhảy trạng thái tùy ý.
- Work order đã đóng hoặc hủy không được dùng thêm phụ tùng.
- Mỗi kỹ thuật viên có tài khoản riêng liên kết 1-1 với `technician_profile`; lịch cá nhân được backend suy ra từ JWT và không thể đổi ID để xem lịch người khác.
- Kỹ thuật viên không thể xem dữ liệu khách hàng hoặc phiếu của người khác.
- Serial thiết bị, mã khách hàng, SKU phụ tùng và mã work order được kiểm soát duy nhất trong tenant.
- File local chỉ chấp nhận JPG, PNG, WEBP và PDF, tối đa 10 MB.

## 4. Reset dữ liệu demo

Nếu dùng PostgreSQL qua Docker:

```powershell
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml up -d
```

Sau đó khởi động lại backend; Flyway tạo schema và profile `local` seed lại dữ liệu.
