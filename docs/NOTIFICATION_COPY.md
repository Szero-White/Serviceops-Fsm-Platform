# Notification Copy Catalog

Notification chuông trong ServiceOps là **hàng đợi chú ý theo vai trò**, không phải bản sao của Audit hay Timeline. Mỗi dòng phải giúp người nhận trả lời được ngay ba câu hỏi:

1. **Việc gì vừa xảy ra hoặc đang cần tôi xử lý?**
2. **Đang nói tới phiếu/khách hàng/phụ tùng nào và ai vừa thực hiện hành động liên quan?**
3. **Tôi cần mở đâu hoặc làm gì tiếp theo?**

## Quy tắc viết thống nhất

- **Title = hành động/sự kiện quan trọng + mã tra cứu.** Ví dụ: `Cần phân công kỹ thuật viên: WO-2026-001245`.
- **Body = actor + business context + next action.** Với Work Order, ưu tiên tên khách hàng và tiêu đề công việc; với kho, ưu tiên SKU, tên phụ tùng và số lượng/ngưỡng.
- Body chỉ 1–2 câu ngắn, không lặp lại title và không biến notification thành audit dump.
- Dùng đúng thuật ngữ UI: **Phiếu công việc**, **Lịch điều phối**, **Lịch của tôi**, **Kho phụ tùng**, **Lịch sử biến động**, **Lịch sử phiếu**.
- Không dùng enum nội bộ (`ON_THE_WAY`, `CUSTOMER_ACCEPTED`), raw timestamp ISO, tên class/API, chuỗi test hoặc technical summary khó hiểu làm nội dung chính.
- Lý do nghiệp vụ quan trọng như **mở lại/hủy phiếu** được giữ trong body, nhưng được cắt gọn để không vượt giới hạn persistence.
- `NotificationCopy` là nơi duy nhất tạo copy runtime cho bell. Service nghiệp vụ chỉ cung cấp context; không tự ghép title/message rải rác.
- Giới hạn persistence hiện tại là title 180 ký tự và message 500 ký tự; `NotificationCopy.Copy` chịu trách nhiệm normalize/cắt an toàn.

### Ví dụ chuẩn

**Dispatcher**

- Title: `Cần phân công kỹ thuật viên: WO-2026-001245`
- Body: `Chăm sóc khách hàng Trần Mai CSKH đã chuyển phiếu "Máy rửa chén không cấp nước" (WO-2026-001245) của khách Trần Minh Anh sang bộ phận điều phối. Mở Lịch điều phối để chọn kỹ thuật viên và thời gian thực hiện.`

**Technician**

- Title: `Bạn có công việc mới: WO-2026-001245`
- Body: `Điều phối viên Lê Thu Điều phối đã giao cho bạn phiếu "Máy rửa chén không cấp nước" (WO-2026-001245) của khách Trần Minh Anh. Mở Lịch của tôi để xem lịch và bắt đầu công việc.`

**Reopen**

- Title: `Phiếu cần xử lý lại: WO-2026-001245`
- Body phải có người mở lại, khách hàng/công việc và **Lý do** trước khi hướng dẫn bước tiếp theo.

## Ma trận notification theo vai trò

| Trigger | Người nhận | Title chuẩn | Business context bắt buộc / bước tiếp theo |
|---|---|---|---|
| Service Request → Work Order | Dispatcher | **Cần phân công kỹ thuật viên: WO-...** | Actor chuyển phiếu + summary + khách hàng; mở Lịch điều phối |
| Phân công lần đầu | Technician được giao | **Bạn có công việc mới: WO-...** | Dispatcher/Owner + summary + khách hàng; mở Lịch của tôi |
| Đổi Technician | Technician cũ | **Bạn không còn phụ trách: WO-...** | Actor + người nhận mới + khách hàng; dừng theo dõi job cũ |
| Đổi Technician | Technician mới | **Bạn có công việc mới: WO-...** | Actor + summary + khách hàng; mở Lịch của tôi |
| Chỉ đổi thời gian | Technician hiện tại | **Lịch của bạn đã thay đổi: WO-...** | Actor + summary + khách hàng + lịch cũ → lịch mới + lý do; xem Lịch của tôi |
| Work Order → WAITING_FOR_PARTS | Dispatcher | **Phiếu đang chờ phụ tùng: WO-...** | Technician + summary + khách hàng + ghi chú nếu có; phối hợp xử lý |
| Work Order → REOPENED | Owner + Dispatcher, trừ actor | **Phiếu cần xử lý lại: WO-...** | Actor + summary + khách hàng + lý do; điều phối bước tiếp theo |
| Work Order → REOPENED | Assigned Technician, nếu không phải actor | **Công việc cần xử lý lại: WO-...** | Actor + summary + khách hàng + lý do; tiếp tục theo phân công |
| Technician → COMPLETED | Customer Service | **Cần theo dõi khách sau sửa chữa: WO-...** | Technician + summary + khách hàng; theo dõi phản hồi, reopen nếu sự cố còn |
| Work Order → CLOSED bởi người khác | Assigned Technician | **Phiếu đã đóng: WO-...** | Actor + summary + khách hàng; không cần thao tác thêm |
| Work Order → CANCELLED | Owner, trừ actor | **Phiếu đã hủy: WO-...** | Actor + summary + khách hàng + lý do; tra Lịch sử phiếu khi cần |
| Work Order → CANCELLED | Assigned Technician, nếu không phải actor | **Công việc đã hủy: WO-...** | Actor + summary + khách hàng + lý do; dừng job và xem Lịch của tôi |
| Consume làm stock cross threshold | Owner + Warehouse | **Tồn kho thấp: SKU** | Technician + WO + tên phụ tùng + tồn hiện tại + ngưỡng; mở Kho phụ tùng |
| Đổi reorder level làm stock thành low | Owner + Warehouse khác actor | **Tồn kho thấp theo ngưỡng mới: SKU** | Người đổi ngưỡng + tên part + tồn/ngưỡng mới; mở Kho phụ tùng |
| Stocktake có chênh lệch | Owner khác actor | **Kiểm kê có chênh lệch: SKU** | Người kiểm kê + system/actual/difference + lý do; mở Lịch sử biến động |
| Stocktake kết thúc ở mức low | Warehouse | **Tồn kho thấp sau kiểm kê: SKU** | Người kiểm kê + tên part + actual + threshold; mở Kho phụ tùng |

## Những việc cố ý không tạo bell

Các thao tác dưới đây có success/error feedback tại màn hình và có nguồn truy vết phù hợp, nên **không broadcast notification**:

- Tạo/sửa/xóa/import Customer.
- Tạo/sửa/xóa/import Asset.
- Tạo/sửa/hủy/xóa Service Request thông thường.
- Tạo/sửa/xóa Service Channel.
- Cập nhật Technician profile.
- Upload attachment.
- Tạo/import catalog phụ tùng hoặc nhập kho bình thường.
- Technician `ON_THE_WAY`, `IN_PROGRESS`, từng lần CONSUME bình thường, `CUSTOMER_ACCEPTED`.
- Completion/closure bình thường không broadcast cho Owner.

Dùng **Timeline** cho câu chuyện của một Work Order, **Inventory Movements** cho ledger kho và **Audit** cho truy vết system-wide. Bell chỉ chứa việc người nhận thực sự cần biết hoặc cần hành động.

## Legacy notification

`V7__notification_feed_cleanup.sql` xóa các row bell cũ thuộc nhóm CRUD/import/generic-status từng được persist ở các release trước. Đây là dữ liệu notification dư thừa, không phải audit history; Audit/Timeline/Inventory Movements vẫn giữ nguồn truy vết.

Các notification cũ còn giá trị hành động như assignment/reschedule/reopen/cancel vẫn được giữ. `frontend/src/features/notifications/presentation.ts` chỉ làm compatibility cho các title cũ này để tránh lộ enum hoặc chuỗi kỹ thuật. Không đặt logic legacy trong `AppLayout` và không tiếp tục mở rộng mapper bằng routine CRUD đã bị migration loại bỏ.
