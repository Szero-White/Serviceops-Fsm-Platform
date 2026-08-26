# Notification Copy Catalog

Mục tiêu của notification chuông là giúp đúng người nhận hiểu ngay **chuyện gì vừa xảy ra / việc gì cần làm** và **bước tiếp theo là gì**. Notification không thay cho Timeline hay Audit.

## Quy tắc copy

- Tiêu đề ngắn: sự kiện + mã nghiệp vụ cần tra cứu (`WO-...`, SKU).
- Mô tả nói bước tiếp theo; không dùng notification như log kỹ thuật.
- Dùng nhất quán các thuật ngữ UI: **Phiếu công việc**, **Lịch điều phối**, **Lịch của tôi**, **Tồn kho thấp**.
- Không dùng enum nội bộ (`ON_THE_WAY`, `CUSTOMER_ACCEPTED`...), raw timestamp ISO, mã test hoặc audit detail làm nội dung chính.
- CRUD/master-data/import/attachment bình thường dùng success/error feedback tại màn hình + Audit, **không tạo bell notification**.
- Copy runtime mới được gom về `NotificationCopy`; notification lịch sử trong database không bị rewrite, frontend chỉ chuyển các mẫu cũ sang câu dễ đọc khi render.

## Ma trận notification theo vai trò

| Trigger | Người nhận | Copy chuẩn / mục đích |
|---|---|---|
| Service Request chuyển thành Work Order | Dispatcher | **Phiếu mới chờ điều phối: WO-...** — mở Lịch điều phối để phân công |
| Phân công lần đầu | Technician được giao | **Bạn được phân công: WO-...** — mở Lịch của tôi |
| Đổi Technician | Technician cũ | **Bạn không còn được phân công: WO-...** — cập nhật kế hoạch |
| Đổi Technician | Technician mới | **Bạn được phân công: WO-...** — xem lịch mới |
| Chỉ đổi thời gian | Technician hiện tại | **Lịch làm việc đã thay đổi: WO-...** |
| Work Order chờ phụ tùng | Dispatcher | **Phiếu đang chờ phụ tùng: WO-...** — phối hợp với kho |
| Work Order được mở lại | Owner + Dispatcher (trừ actor) | **Phiếu cần xử lý lại: WO-...** |
| Work Order được mở lại | Assigned Technician (nếu không phải actor) | **Phiếu được mở lại: WO-...** |
| Technician hoàn thành Work Order | Customer Service | **Phiếu đã hoàn thành: WO-...** — theo dõi phản hồi khách hàng; không trao quyền Đóng phiếu |
| Work Order bị hủy | Owner (trừ actor) | **Phiếu đã hủy: WO-...** |
| Work Order bị hủy | Assigned Technician (nếu không phải actor) | **Phiếu đã hủy: WO-...** |
| Work Order được đóng bởi người khác | Assigned Technician | **Phiếu đã đóng: WO-...** |
| Consume làm stock lần đầu chạm/thấp hơn ngưỡng | Owner + Warehouse | **Tồn kho thấp: SKU** |
| Đổi reorder level làm stock chuyển sang thấp | Owner + Warehouse khác actor | **Tồn kho thấp: SKU** |
| Stocktake có chênh lệch | Owner khác actor | **Kiểm kê có chênh lệch: SKU** |
| Stocktake kết thúc với tồn thấp | Warehouse | **Tồn kho thấp: SKU** |

## Những việc cố ý không tạo chuông

- Tạo/sửa/xóa/import Customer.
- Tạo/sửa/xóa/import Asset.
- Tạo/sửa/hủy/xóa Service Request thông thường.
- Tạo/sửa/xóa Service Channel.
- Cập nhật Technician profile.
- Upload attachment.
- Tạo/import catalog phụ tùng hoặc nhập kho bình thường.
- Technician `ON_THE_WAY`, `IN_PROGRESS`, từng lần CONSUME bình thường, `CUSTOMER_ACCEPTED`.
- Owner không nhận completion/closure bình thường.

Các việc này đã có workspace, Timeline, Inventory Movements hoặc Audit phù hợp. Đưa chúng vào chuông chỉ tạo nhiễu.

## Legacy display

Ví dụ dữ liệu cũ:

`Có phiếu mới chờ điều phối: WO-2026-001010`

`Technician policy E2E 76154627`

Frontend hiển thị:

`Phiếu mới chờ điều phối: WO-2026-001010`

`Mở Lịch điều phối để phân công kỹ thuật viên.`

Tương tự, notification kênh cũ có title chứa mã kiểu `KENH_E2E_...` sẽ ưu tiên tên kênh trong mô tả hoặc câu tổng quát, không đưa mã test/kỹ thuật lên title chính.
