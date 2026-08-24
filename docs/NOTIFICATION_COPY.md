# Notification Copy Catalog

Mục tiêu của notification chuông là giúp người nhận hiểu ngay **chuyện gì vừa xảy ra / việc gì cần làm** và **bước tiếp theo là gì**.

## Quy tắc copy

- Tiêu đề ngắn, dùng ngôn ngữ nghiệp vụ người dùng nhìn thấy trên UI.
- Mô tả nói rõ bước tiếp theo; không dùng notification như log kỹ thuật.
- Giữ mã nghiệp vụ có ích để tra cứu (`WO-...`, SKU, mã khách hàng/kênh).
- Không dùng enum nội bộ (`ON_THE_WAY`, `CUSTOMER_ACCEPTED`...), chuỗi test hoặc raw summary khó hiểu làm nội dung chính.
- Notification cũ trong database không bị rewrite; frontend chuyển các mẫu cũ sang câu thân thiện khi render.

## Audit toàn bộ nguồn tạo notification

Có **34 call-site runtime** qua `NotificationService` và **1 notification demo seed**. Danh sách dưới đây bao phủ toàn bộ nguồn tạo notification có copy cố định trong source.

| # | Module / trigger | Người nhận | Tiêu đề / ý nghĩa |
|---|---|---|---|
| 1 | Work Order được tạo từ Service Request | Dispatcher | `Phiếu mới cần điều phối: WO-...` |
| 2 | Reschedule, kỹ thuật viên cũ bị gỡ khỏi phiếu | Kỹ thuật viên cũ | `Lịch làm việc đã thay đổi: WO-...` |
| 3 | Reschedule / đổi giờ cho kỹ thuật viên hiện tại | Kỹ thuật viên | `Lịch làm việc đã thay đổi: WO-...` |
| 4 | Phân công Work Order mới | Kỹ thuật viên | `Bạn được giao công việc mới: WO-...` |
| 5 | Work Order `COMPLETED` | Owner (fallback, trừ actor) | `Chờ khách xác nhận: WO-...` |
| 6 | Work Order `WAITING_FOR_PARTS` | Dispatcher | `Cần xử lý phụ tùng: WO-...` |
| 7 | Work Order `REOPENED` | Dispatcher | `Cần điều phối xử lý lại: WO-...` |
| 8 | Work Order `CLOSED` | Owner + assigned Technician khi không phải actor | `Phiếu đã đóng: WO-...` |
| 9 | Work Order `CANCELLED` | Owner | `Phiếu đã hủy: WO-...` |
| 10 | Work Order reopen/cancel cần báo kỹ thuật viên được giao | Kỹ thuật viên | `Công việc cần xử lý lại` / `Công việc đã hủy` |
| 11 | Hồ sơ kỹ thuật viên thay đổi | Owner / Dispatcher | `Thông tin kỹ thuật viên đã thay đổi: ...` |
| 12 | Upload attachment | Vai trò liên quan | `Có tệp mới trong phiếu công việc / yêu cầu dịch vụ / thiết bị` |
| 13 | Tạo Service Request | Intake roles | `Yêu cầu mới cần tiếp nhận` |
| 14 | Cập nhật Service Request | Intake roles | `Yêu cầu dịch vụ vừa được cập nhật` |
| 15 | Hủy Service Request | Intake roles | `Yêu cầu dịch vụ đã hủy` |
| 16 | Xóa Service Request | Intake roles | `Yêu cầu dịch vụ đã được xóa` |
| 17 | Tạo Service Channel | Owner / Customer Service | `Đã thêm kênh tiếp nhận: ...` |
| 18 | Cập nhật Service Channel | Owner / Customer Service | `Thông tin kênh tiếp nhận đã thay đổi: ...` |
| 19 | Xóa Service Channel | Owner / Customer Service | `Đã xóa kênh tiếp nhận: ...` |
| 20 | Tạo Asset | Owner / Customer Service | `Đã thêm thiết bị: ...` |
| 21 | Cập nhật Asset | Owner / Customer Service | `Thông tin thiết bị đã thay đổi: ...` |
| 22 | Xóa Asset | Owner / Customer Service | `Đã xóa thiết bị: ...` |
| 23 | Import Asset | Owner / Customer Service | `Đã thêm thiết bị từ tệp` |
| 24 | Kiểm kê có chênh lệch | Owner | `Kiểm kê có chênh lệch: SKU` |
| 25 | Tồn thấp sau kiểm kê | Warehouse | `Cần bổ sung tồn kho sau kiểm kê: SKU` |
| 26 | Đổi ngưỡng làm tồn chuyển sang thấp | Owner / Warehouse | `Cần kiểm tra tồn kho: SKU` |
| 27 | Tạo phụ tùng | Owner / Warehouse | `Đã thêm phụ tùng: SKU` |
| 28 | Nhập kho | Owner / Warehouse | `Kho vừa được bổ sung: SKU` |
| 29 | Import danh mục phụ tùng | Owner / Warehouse | `Đã thêm phụ tùng từ tệp` |
| 30 | Consume làm tồn chạm/thấp hơn ngưỡng | Owner / Warehouse | `Cần bổ sung tồn kho: SKU` |
| 31 | Tạo Customer | Owner / Customer Service | `Đã thêm khách hàng: ...` |
| 32 | Cập nhật Customer | Owner / Customer Service | `Thông tin khách hàng đã thay đổi: ...` |
| 33 | Xóa Customer | Owner / Customer Service | `Đã xóa khách hàng: ...` |
| 34 | Import Customer | Owner / Customer Service | `Đã thêm khách hàng từ tệp` |
| Demo | Seed notification cho Technician | Technician demo | `Bạn được giao công việc mới: WO-...` |

## Ví dụ trước / sau

Trước:

`Công việc mới: WO-2026-001010`

`Bạn được phân công: Technician policy E2E 76154627`

Sau:

`Bạn được giao công việc mới: WO-2026-001010`

`Mở phiếu để xem nội dung, khách hàng và thời gian thực hiện.`

Người dùng không cần hiểu tên test, enum hoặc implementation detail để biết mình phải làm gì.
