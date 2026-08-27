# Hướng dẫn sử dụng bản demo

## 1. Chọn tài khoản theo vai trò

- `owner`: quản trị tổng thể các module được cấp: người dùng, Customer/Asset, Service Request, kênh, Work Order/điều phối, đội kỹ thuật, kho/kiểm kê/lịch sử biến động và audit; chỉ giám sát yêu cầu/vật tư, không giả lập thao tác hiện trường hoặc xác nhận hàng ra/vào kho thay role nghiệp vụ.
- `dispatcher`: quản lý bảng điều phối tuần, gán kỹ thuật viên và xếp lịch work order.
- `customer-service`: tạo khách hàng, thiết bị và yêu cầu dịch vụ.
- `technician`: tài khoản cá nhân của Phạm Quốc; chỉ xem lịch và công việc được giao cho chính mình.
- `technician-2`: tài khoản cá nhân của Võ Hoàng; dùng để kiểm tra dữ liệu lịch không bị lẫn giữa kỹ thuật viên.
- `warehouse`: vào thẳng **Yêu cầu phụ tùng**; xác nhận cấp/không thể cấp, quản lý catalog/import, kiểm kê, nhận hoàn trả và tra cứu lịch sử biến động; không có Work Order operational dashboard.

Mật khẩu local/demo mặc định trong portfolio hiện tại: `Demo@2026`. Đây chỉ là credential demo; production secrets phải được cấu hình riêng.

## 2. Kịch bản demo chuẩn

### Bước 1 — Tiếp nhận khách hàng

1. Đăng nhập `customer-service`.
2. Vào **Khách hàng** và tạo hồ sơ mới.
3. Vào **Thiết bị**, chọn khách hàng, nhập loại thiết bị, hãng, model, serial và hạn bảo hành.
4. Vào **Yêu cầu dịch vụ**, tạo yêu cầu với mức ưu tiên và mô tả lỗi.

### Bước 2 — Chuyển sang điều phối và xếp lịch

1. Khi Service Request đủ thông tin, `customer-service` bấm **Chuyển sang điều phối** để tạo Work Order nguồn chuẩn.
2. Đăng nhập `dispatcher`.
3. Mở **Lịch điều phối**, chọn phiếu trong hàng đợi, kỹ thuật viên và khung thời gian.
4. Bấm một lịch đã có trên board để đổi kỹ thuật viên hoặc thời gian khi cần.
5. Nếu lịch chồng lấn, hệ thống từ chối và hiển thị lỗi nghiệp vụ.

### Bước 3 — Kỹ thuật viên thực hiện

1. Đăng nhập `technician`.
2. Mở **Lịch của tôi**; lịch được lấy theo tài khoản đang đăng nhập, không chọn `technicianId` thủ công.
3. Mở **Phiếu công việc**; chỉ các work order được giao cho tài khoản này mới xuất hiện.
4. Chuyển trạng thái lần lượt `ON_THE_WAY` và `IN_PROGRESS`.
5. Upload ảnh/PDF minh chứng.
6. Nếu cần vật tư, mở tab **Phụ tùng** và bấm **Yêu cầu phụ tùng**. Yêu cầu đang chờ có thể sửa số lượng/note hoặc hủy với lý do thực tế; bước này **không giảm tồn kho**.
7. Sau khi Warehouse cấp, tab **Phụ tùng** hiển thị lượng đã cấp. Technician ghi **Thực tế đã dùng**; thao tác này không làm giảm tồn lần nữa và có thể cập nhật đến trạng thái `COMPLETED`.
8. Nhập chẩn đoán và giải pháp, sau đó chuyển sang `COMPLETED`.


### Bước 3.5 — Kho cấp và đối soát vật tư

1. Đăng nhập `warehouse`; hệ thống mở **Yêu cầu phụ tùng**.
2. Với request `REQUESTED`, bấm **Xác nhận cấp** khi giao thực tế cho Technician. Chỉ lúc này tồn kho mới giảm và ledger tạo `ISSUE`. Nếu không thể cấp, chọn **Không thể cấp** và nhập lý do thực tế; không có stock movement.
3. Mở **Lịch sử biến động** để đối chiếu `ISSUE`, actor, Work Order và tồn sau giao dịch. Dữ liệu `CONSUME` cũ vẫn được hiển thị để tương thích lịch sử nhưng UI mới không tạo `CONSUME`.
4. Nếu Technician trả lại phần đã cấp nhưng không dùng, bấm **Hoàn trả**, nhập số lượng và lý do. Backend chặn RETURN vượt `ISSUE - USED - RETURN`; RETURN hợp lệ vẫn được phép sau khi Work Order đã `CLOSED` và không làm mở lại phiếu.
5. Trong **Kho phụ tùng**, OWNER/WAREHOUSE_STAFF có thể dùng **Sửa ngưỡng** để cập nhật **Ngưỡng tồn tối thiểu**. Thao tác này không đổi stock và có audit.
6. Mở **Kiểm kê tồn kho** khi cần đối chiếu số đếm thực tế với hệ thống; chênh lệch tạo `ADJUSTMENT_IN` hoặc `ADJUSTMENT_OUT`. Owner nhận thông báo chênh lệch; Warehouse nhận cảnh báo nếu tồn thấp.
7. Quay lại **Lịch sử biến động** để kiểm tra toàn bộ hàng thực sự ra/vào kho.

### Bước 4 — Khách xác nhận, thanh toán và đóng phiếu

1. Technician hoàn thành công việc (`COMPLETED`), nhập đủ actual-used, tiền công và phí phát sinh thực tế rồi cho khách xem kết quả/tổng tiền.
2. Khi khách đồng ý, Technician bấm **Ghi nhận khách xác nhận**. Hệ thống chuyển `COMPLETED → CUSTOMER_ACCEPTED` và freeze billing snapshot.
3. Technician ghi nhận cách khách thanh toán: **Khách báo đã chuyển khoản** vào tài khoản công ty hoặc **Đã nhận tiền mặt từ khách**. Ảnh giao dịch chỉ là bằng chứng hỗ trợ, không đồng nghĩa tiền đã SETTLED.
4. Customer Service mở **Xử lý thanh toán**. Khoản chuyển khoản/tiền mặt đang chờ có nút **Đối soát thanh toán**; bấm nút này để mở thẳng đúng Work Order ở tab **Thanh toán**. CSKH kiểm lại snapshot chi phí khách đã xác nhận, số tiền/phương thức, ảnh bằng chứng chuyển khoản nếu có hoặc tiền mặt Technician bàn giao rồi mới xác nhận. Xác nhận thành công đưa payment về `SETTLED`.
5. Sau `SETTLED`, ngay trong Work Order hiện **Phát hành / tải biên nhận** và **Đóng phiếu**. Nếu CSKH rời Work Order trước khi hoàn tất, hai action này vẫn hiện tại **Xử lý thanh toán** để tránh bỏ sót. Backend bảo đảm receipt tồn tại trước khi chuyển `CUSTOMER_ACCEPTED → CLOSED`; sau khi đóng, hàng đợi chỉ còn **Tải biên nhận** + trạng thái **Đã đóng phiếu**.
6. Vật tư outstanding không chặn closure. Warehouse vẫn được RETURN phần hợp lệ sau CLOSED; Work Order giữ nguyên `CLOSED`.
7. Nếu khách báo cùng sự cố trước customer acceptance, CSKH có thể reopen theo policy. Sau `CUSTOMER_ACCEPTED`/`CLOSED`, không reopen silent; sự cố mới đi qua Service Request/Work Order mới.

## 3. Quy tắc người dùng cần biết

- Phản hồi biểu mẫu: các trường bắt buộc có dấu đánh dấu. Nếu bấm Lưu/Hoàn thành khi còn thiếu dữ liệu, hệ thống không gửi request; form cuộn tới lỗi đầu tiên và hiển thị cảnh báo ngắn để biết cần bổ sung gì. Các nút xác nhận dùng tên hành động cụ thể thay cho “Đồng ý” ở các flow chính.
- Hoàn thành Work Order: kỹ thuật viên phải nhập **Chẩn đoán / nguyên nhân** và **Giải pháp đã thực hiện**. Sau đó Technician ghi actual-used/chi phí, ghi nhận khách xác nhận và phương thức thanh toán tại hiện trường; CSKH mới đối soát tiền, phát hành biên nhận và đóng phiếu. Owner giám sát outcome thay vì thao tác routine.
- Notification drawer là hàng đợi **việc cần chú ý**, không phải lịch sử CRUD. Title cho biết việc gì + mã `WO-...`/SKU; body cho biết **ai vừa thao tác, đang nói tới khách hàng/công việc/phụ tùng nào và cần làm gì tiếp theo**. Ví dụ Dispatcher thấy **Cần phân công kỹ thuật viên: WO-...** kèm summary + tên khách và hướng dẫn mở Lịch điều phối; Technician thấy **Bạn có công việc mới: WO-...** kèm người giao, khách hàng và hướng dẫn mở Lịch của tôi. CRUD/master-data/import/attachment bình thường không tạo chuông. Tiến độ một Work Order xem ở **Tiến trình**, ledger kho xem ở **Lịch sử biến động**, truy vết toàn hệ thống xem ở **Audit**.
- Bấm biểu tượng chuông để xem; thông báo chưa đọc có nền nổi bật. Bấm dòng chưa đọc để chuyển sang đã đọc. Mỗi dòng có nút trạng thái ở ngoài cùng bên phải để chuyển Đã đọc ↔ Chưa đọc; dùng Đánh dấu chưa đọc khi cần giữ một thông báo để theo dõi lại.
- Work order phải đi đúng vòng đời; không thể nhảy trạng thái tùy ý.
- Work order đã đóng hoặc hủy không được tạo yêu cầu mới/cấp mới/chỉnh actual-used; Warehouse vẫn có thể nhận RETURN phần outstanding hợp lệ sau CLOSED.
- Mỗi kỹ thuật viên có tài khoản riêng liên kết 1-1 với `technician_profile`; lịch cá nhân được backend suy ra từ JWT và không thể đổi ID để xem lịch người khác.
- Kỹ thuật viên chỉ nhận thông tin khách hàng cần thiết trong Work Order được giao; không thể dùng Work Order/My Schedule để đọc job của kỹ thuật viên khác.
- Kỹ thuật viên chỉ thao tác Work Order được giao: tiến độ hiện trường, phụ tùng/actual-used, billing draft, ghi nhận khách xác nhận và payment action tại hiện trường. Customer Service phụ trách reopen/cancel theo policy trước acceptance, payment reconciliation, biên nhận và normal closure. Owner giám sát/cấu hình; Dispatcher phụ trách điều phối/schedule/reschedule và operational cancellation.
- Username tài khoản được cố định sau khi tạo để giữ ổn định audit/ownership; Owner vẫn có thể đổi họ tên hiển thị, mật khẩu và trạng thái tài khoản theo policy. Trang **Người dùng** có bộ lọc **Tất cả trạng thái / Hoạt động / Tạm ngưng** kết hợp với tìm kiếm; các guard self-disable, last-owner và demo account vẫn bắt buộc.
- **Trợ lý AI** tự dùng role của tài khoản đang đăng nhập. Có thể hỏi tổng quát “Trong vai trò này tôi được làm gì?” để nhận overview; sau đó hỏi sâu từng chức năng. AI không mở rộng sang quyền role khác: ví dụ Dispatcher không được hướng dẫn quản trị user/kho, Technician không được hướng dẫn kiểm kê/sửa ngưỡng, Warehouse không được hướng dẫn Work Order hiện trường.
- Serial thiết bị, mã khách hàng, SKU phụ tùng và mã work order được kiểm soát duy nhất trong tenant.
- File local chỉ chấp nhận JPG, PNG, WEBP và PDF, tối đa 10 MB.

## 4. Reset dữ liệu demo

Nếu dùng PostgreSQL qua Docker và **thật sự muốn xóa toàn bộ dữ liệu local**:

```powershell
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml up -d
```

`down -v` xóa cả PostgreSQL volume; không dùng cách này chỉ để dọn vài record E2E/UAT. Sau reset, khởi động backend với `DEMO_PASSWORD=Demo@2026` để seed lại cùng credential với frontend.
