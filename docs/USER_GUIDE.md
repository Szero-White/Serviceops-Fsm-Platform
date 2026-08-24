# Hướng dẫn sử dụng bản demo

## 1. Chọn tài khoản theo vai trò

- `owner`: quản trị tổng thể các module được cấp: người dùng, Customer/Asset, Service Request, kênh, Work Order/điều phối, đội kỹ thuật, kho/kiểm kê/lịch sử biến động và audit; không giả lập tiến độ hiện trường hoặc consume thay Technician.
- `dispatcher`: quản lý bảng điều phối tuần, gán kỹ thuật viên và xếp lịch work order.
- `customer-service`: tạo khách hàng, thiết bị và yêu cầu dịch vụ.
- `technician`: tài khoản cá nhân của Phạm Quốc; chỉ xem lịch và công việc được giao cho chính mình.
- `technician-2`: tài khoản cá nhân của Võ Hoàng; dùng để kiểm tra dữ liệu lịch không bị lẫn giữa kỹ thuật viên.
- `warehouse`: vào thẳng **Kho phụ tùng**; quản lý catalog/import, kiểm kê chênh lệch, xác nhận hoàn trả phụ tùng theo Work Order và tra cứu lịch sử biến động; không có Work Order operational dashboard.

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
6. Ghi nhận phụ tùng đã sử dụng. Hệ thống không cho số lượng tồn âm; sau khi ghi nhận thành công, mở tab **Tiến trình** của phiếu để thấy ngay tên/SKU, số lượng, người thao tác, thời gian và ghi chú của phụ tùng đã dùng.
7. Nhập chẩn đoán và giải pháp, sau đó chuyển sang `COMPLETED`.


### Bước 3.5 — Kho xác nhận và đối soát vật tư

1. Đăng nhập `warehouse`.
2. Mở **Lịch sử biến động** để xem giao dịch `CONSUME` vừa phát sinh từ Work Order.
3. Trong **Kho phụ tùng**, OWNER/WAREHOUSE_STAFF có thể dùng **Sửa ngưỡng** để cập nhật **Ngưỡng tồn tối thiểu**. Đây là mốc cảnh báo tồn thấp, không phải số lượng đặt mua; thay đổi được audit và nếu ngưỡng mới làm tồn hiện tại mới rơi vào trạng thái tồn thấp thì hệ thống phát notification sau commit.
4. Nếu kỹ thuật viên trả lại phần chưa dùng, bấm **Hoàn trả**, nhập số lượng và lý do; backend chặn tổng RETURN vượt tổng CONSUME của cùng part/Work Order. Giao dịch RETURN cũng xuất hiện trong **Tiến trình** của Work Order để người xem biết số lượng nào đã được trả lại trước khi đối soát hóa đơn.
5. Mở **Kiểm kê tồn kho** khi cần đối chiếu số đếm thực tế với hệ thống; chênh lệch tự tạo `ADJUSTMENT_IN` hoặc `ADJUSTMENT_OUT`. Sau khi giao dịch thành công, Owner nhận thông báo chênh lệch; Warehouse nhận cảnh báo nếu tồn chạm hoặc thấp hơn ngưỡng tồn tối thiểu.
6. Quay lại **Lịch sử biến động** để kiểm tra actor, thời gian, số lượng, tồn sau giao dịch và Work Order liên quan.

### Bước 4 — Khách xác nhận và đóng phiếu

1. Sau khi kỹ thuật viên chuyển phiếu sang `COMPLETED`, giữ nguyên tài khoản Technician được giao hoặc đăng nhập `owner` nếu cần admin override.
2. Mở chi tiết phiếu bằng icon mắt. Trong hàng thao tác cạnh **Tải ảnh / PDF**, khi khách đã đồng ý kết quả sẽ có nút **Khách xác nhận**.
3. Bấm **Khách xác nhận** để chuyển `COMPLETED → CUSTOMER_ACCEPTED`.
4. Sau đó nút **Đóng phiếu** xuất hiện. Bấm để chuyển `CUSTOMER_ACCEPTED → CLOSED`; giao diện tự chuyển sang **Lịch sử phiếu công việc** và mở đúng phiếu vừa đóng.
5. Nếu cùng sự cố vẫn còn trước khi đóng, dùng **Khách yêu cầu xử lý lại** để chuyển `COMPLETED/CUSTOMER_ACCEPTED → REOPENED`.
6. Khi phiếu đã `CLOSED`, không mở lại nữa. Nếu khách báo lỗi phát sinh sau đó, Customer Service tiếp nhận Service Request mới để tạo Work Order mới, giữ lịch sử cũ nguyên vẹn.

## 3. Quy tắc người dùng cần biết

- Phản hồi biểu mẫu: các trường bắt buộc có dấu đánh dấu. Nếu bấm Lưu/Hoàn thành khi còn thiếu dữ liệu, hệ thống không gửi request; form cuộn tới lỗi đầu tiên và hiển thị cảnh báo ngắn để biết cần bổ sung gì. Các nút xác nhận dùng tên hành động cụ thể thay cho “Đồng ý” ở các flow chính.
- Hoàn thành Work Order: kỹ thuật viên phải nhập **Chẩn đoán / nguyên nhân** và **Giải pháp đã thực hiện**. Thành công thì kỹ thuật viên nhận success feedback và có thể bấm **Khách xác nhận** ngay trong phiếu khi khách đồng ý; Owner nhận notification **Chờ khách xác nhận** như fallback quản trị.
- Notification drawer: tiêu đề nói rõ **chuyện gì vừa xảy ra hoặc việc cần làm**, dòng mô tả nói **bước tiếp theo**. Ví dụ: **Bạn được giao công việc mới: WO-...** → “Mở phiếu để xem nội dung, khách hàng và thời gian thực hiện”; **Chờ khách xác nhận** → Owner có thể mở phiếu và hỗ trợ ghi nhận nếu Technician chưa thực hiện. Mã WO/SKU được giữ để tra cứu, còn enum nội bộ, chuỗi test hoặc mô tả kỹ thuật khó hiểu không dùng làm nội dung chính. Notification cũ được giao diện đổi sang cách đọc thân thiện khi hiển thị.
- Bấm biểu tượng chuông để xem; thông báo chưa đọc có nền nổi bật. Bấm dòng chưa đọc để chuyển sang đã đọc. Mỗi dòng có nút trạng thái ở ngoài cùng bên phải để chuyển Đã đọc ↔ Chưa đọc; dùng Đánh dấu chưa đọc khi cần giữ một thông báo để theo dõi lại.
- Work order phải đi đúng vòng đời; không thể nhảy trạng thái tùy ý.
- Work order đã đóng hoặc hủy không được dùng thêm phụ tùng.
- Mỗi kỹ thuật viên có tài khoản riêng liên kết 1-1 với `technician_profile`; lịch cá nhân được backend suy ra từ JWT và không thể đổi ID để xem lịch người khác.
- Kỹ thuật viên chỉ nhận thông tin khách hàng cần thiết trong Work Order được giao; không thể dùng Work Order/My Schedule để đọc job của kỹ thuật viên khác.
- Kỹ thuật viên chỉ thao tác Work Order được giao: tiến độ hiện trường và, sau `COMPLETED`, **Khách xác nhận / Đóng phiếu / Mở lại** trước khi đóng. Owner có admin override cho các bước hậu xử lý và hủy. Customer Service tiếp nhận follow-up để mở lại/hủy khi khách thay đổi nhu cầu. Dispatcher phụ trách điều phối/schedule/reschedule và operational cancellation.
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
