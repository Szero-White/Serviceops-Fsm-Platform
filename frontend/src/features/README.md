# Cấu trúc feature của frontend

Frontend bám theo ranh giới nghiệp vụ của backend. Mỗi feature sở hữu API, page và component thuộc phạm vi của mình.

- `auth`: đăng nhập, session hiện tại, auth provider và route guard.
- `dashboard`: tổng quan vận hành.
- `users`: quản lý tài khoản và vai trò.
- `customers`: khách hàng.
- `assets`: thiết bị/tài sản của khách hàng.
- `service-requests`: tiếp nhận yêu cầu dịch vụ.
- `service-channels`: cấu hình kênh tiếp nhận.
- `work-orders`: Work Order, lifecycle, scheduling action, completion và parts workflow.
- `scheduling`: schedule board và lịch cá nhân; tái sử dụng scheduling action do `work-orders` sở hữu.
- `technicians`: hồ sơ kỹ thuật viên.
- `inventory`: danh mục phụ tùng, ngưỡng tồn, kiểm kê, trả phụ tùng và lịch sử biến động kho.
- `attachments`: upload, preview, download và giao diện tệp đính kèm.
- `audit`: nhật ký hệ thống.
- `notifications`: thông báo trong ứng dụng.

Quy ước:

- API theo domain đặt tại `features/<domain>/api.ts`.
- Route page đặt tại `features/<domain>/pages/`.
- HTTP transport dùng chung đặt tại `api/http.ts`.
- UI thuộc domain đặt tại `features/<domain>/components/`.
- Chỉ component thực sự dùng chung mới đặt tại `components/`.
- Shared API contract đặt trong các file có phạm vi rõ dưới `types/`; `types/index.ts` chỉ là stable barrel.
- Feature phải import API từ module sở hữu; không tạo cross-domain API barrel.
- Selector dùng API phân trang phải search server-side, không âm thầm giới hạn dữ liệu ở trang đầu.
