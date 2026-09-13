# Hướng dẫn sử dụng bản demo

## 1. Chọn tài khoản theo vai trò

- `owner`: quản trị tổng thể các module được cấp: người dùng, Customer/Asset, Service Request, kênh, Work Order/điều phối, đội kỹ thuật, kho/kiểm kê/lịch sử biến động và audit; chỉ giám sát yêu cầu/vật tư, không giả lập thao tác hiện trường hoặc xác nhận hàng ra/vào kho thay role nghiệp vụ.
- `dispatcher`: quản lý Work Order, đội ngũ kỹ thuật, phân công/xếp lịch và lịch sử nghiệp vụ điều phối; không có quyền Nhật ký hệ thống.
- `customer-service`: tạo khách hàng, thiết bị và yêu cầu dịch vụ.
- `technician`: tài khoản cá nhân của Phạm Quốc; chỉ xem lịch/công việc được giao và thao tác phụ tùng ngay trong Work Order, không có workspace Kho phụ tùng riêng.
- `technician-2`: tài khoản cá nhân của Võ Hoàng; dùng để kiểm tra dữ liệu lịch không bị lẫn giữa kỹ thuật viên.
- `warehouse`: vào thẳng **Yêu cầu phụ tùng**; xác nhận cấp/không thể cấp, quản lý catalog/import, kiểm kê, nhận hoàn trả và tra cứu lịch sử biến động; không có Work Order operational dashboard.

Mật khẩu local/demo mặc định trong portfolio hiện tại: `Demo@2026`. Đây chỉ là credential demo; production secrets phải được cấu hình riêng.


## 1.1. Điều hướng theo vai trò

- `OWNER`: **Vận hành** → **Khách hàng & nguồn lực** → **Kho & vật tư** → **Quản trị**. Owner nhìn rộng toàn hệ thống nhưng các action chuyên môn vẫn thuộc đúng role.
- `CUSTOMER_SERVICE`: **Công việc** (Tổng quan, Yêu cầu dịch vụ, Phiếu công việc, Xử lý thanh toán, Lịch sử phiếu) + **Khách hàng** (Khách hàng, Thiết bị, Kênh tiếp nhận).
- `DISPATCHER`: **Điều phối** (Tổng quan, Phiếu công việc, Lịch điều phối, Lịch sử phiếu) + **Nguồn lực** (Kỹ thuật viên). Customer/Asset chỉ là dữ liệu đọc hỗ trợ trong nghiệp vụ, không phải workspace chính; Audit là Owner-only.
- `TECHNICIAN`: **Công việc của tôi** (Tổng quan, Lịch của tôi, Phiếu công việc, Lịch sử phiếu). Tìm/request phụ tùng ngay trong Work Order thay vì mở Kho phụ tùng.
- `WAREHOUSE_STAFF`: **Kho & vật tư** (Yêu cầu phụ tùng, Kho phụ tùng, Kiểm kê tồn kho, Lịch sử biến động), trong đó Yêu cầu phụ tùng là queue ưu tiên.
- Menu điều hướng cuộn độc lập; **Đăng xuất** nằm ở footer cố định và không che item cuối. **Thiết lập thanh toán** nằm trong Quản trị của Owner, không nằm cạnh footer tài khoản.

## 2. Kịch bản demo chuẩn

### Bước 1 — Tiếp nhận khách hàng

1. Đăng nhập `customer-service`.
2. Vào **Khách hàng** và tạo hồ sơ mới.
3. Vào **Thiết bị**, chọn khách hàng, nhập loại thiết bị, hãng, model, serial và hạn bảo hành.
4. Vào **Yêu cầu dịch vụ**, chọn mức ưu tiên + kênh tiếp nhận và nhập tiêu đề/mô tả lỗi. Nút **AI gợi ý** chỉ chuẩn hóa **Tiêu đề** và **Mô tả**; AI không thay đổi mức ưu tiên hoặc kênh tiếp nhận mà CSKH đã chọn. Badge **Gemini** cho biết kết quả đến từ provider AI đã cấu hình; badge **Nội bộ** cho biết hệ thống đang dùng fallback an toàn. Người dùng không cần xử lý API key, HTTP status hay lỗi hạ tầng.

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
7. Sau khi Warehouse cấp, tab **Phụ tùng** hiển thị lượng đã cấp. Technician ghi **Thực tế đã dùng**; thao tác này không làm giảm tồn lần nữa và có thể cập nhật đến trạng thái `COMPLETED`. Sau khi lưu, tab **Chi phí** tự lấy lại billing draft nên số lượng/thành tiền mới hiển thị ngay, không cần F5; sau `CUSTOMER_ACCEPTED` billing đã freeze nên không tự tính lại.
8. Nhập chẩn đoán và giải pháp, sau đó chuyển sang `COMPLETED`.


### Bước 3.5 — Kho cấp và đối soát vật tư

1. Đăng nhập `warehouse`; hệ thống mở **Yêu cầu phụ tùng**.
2. Với request `REQUESTED`, bấm **Xác nhận cấp** khi giao thực tế cho Technician. Chỉ lúc này tồn kho mới giảm và ledger tạo `ISSUE`. Nếu không thể cấp, chọn **Không thể cấp** và nhập lý do thực tế; không có stock movement.
3. Trong **Yêu cầu phụ tùng**, theo dõi mục **Vật tư đang do kỹ thuật viên giữ**. Mục này chỉ hiển thị phần đã cấp nhưng chưa dùng/chưa trả hết; khi hoàn hết, dòng tự biến mất.
4. Khi kỹ thuật viên bàn giao lại vật tư thực tế, nhân viên kho bấm **Hoàn trả** ngay tại dòng tương ứng, đối chiếu Work Order/SKU/số lượng, nhập số lượng thực nhận và lý do rồi xác nhận. Backend chặn RETURN vượt `ISSUE - USED - RETURN`; RETURN hợp lệ vẫn được phép sau khi Work Order đã `CLOSED` và không làm mở lại phiếu.
5. Mở **Lịch sử biến động** để đối chiếu `ISSUE`, `RETURN`, Work Order, **Kỹ thuật viên nhận / trả**, **Người thực hiện** và tồn sau giao dịch. Đây là sổ truy vết chỉ đọc, không phải nơi bắt đầu thao tác hoàn trả. Cùng một cột hiển thị kỹ thuật viên nhận trên `ISSUE` và kỹ thuật viên trả trên `RETURN`; tên này được snapshot tại lúc giao dịch nên không bị đổi theo việc điều phối lại Work Order sau đó. Dữ liệu `CONSUME` cũ vẫn được hiển thị để tương thích lịch sử; active API/UI không còn tạo `CONSUME`.
6. Trong **Kho phụ tùng**, OWNER/WAREHOUSE_STAFF có thể dùng **Sửa ngưỡng** để cập nhật **Ngưỡng tồn tối thiểu**. Thao tác này không đổi stock và có audit.
7. Mở **Kiểm kê tồn kho** khi cần đối chiếu số đếm thực tế với hệ thống; chênh lệch tạo `ADJUSTMENT_IN` hoặc `ADJUSTMENT_OUT`. Owner nhận thông báo chênh lệch; Warehouse nhận cảnh báo nếu tồn thấp.
8. Quay lại **Lịch sử biến động** để kiểm tra toàn bộ hàng thực sự ra/vào kho.

### Hình ảnh & tài liệu trong quá trình sửa chữa

- Trong Work Order, assigned Technician có thể **Chụp ảnh** hoặc **Tải file lên** để lưu nhiều ảnh/PDF phục vụ hồ sơ sửa chữa; OWNER cũng có thể bổ sung khi phiếu còn active.
- File sửa chữa do chính uploader tải lên có thể đổi tên/xóa khi hồ sơ còn mutable; OWNER có thể quản lý work evidence active. Sau `CUSTOMER_ACCEPTED`, `CLOSED` hoặc `CANCELLED`, hồ sơ chuyển read-only và chỉ còn **Xem/Tải xuống**.
- Ảnh giao dịch chuyển khoản là `PAYMENT_EVIDENCE` riêng và chỉ hiển thị trong tab **Thanh toán**. Technician có thể chụp lại/bỏ ảnh nháp trước khi báo chuyển khoản; sau khi báo chuyển khoản, evidence bị khóa để CSKH đối soát và không thể rename/delete.

### Bước 4 — Khách xác nhận, thanh toán và đóng phiếu

1. Technician hoàn thành công việc (`COMPLETED`), nhập đủ actual-used, tiền công và phí phát sinh thực tế rồi cho khách xem kết quả/tổng tiền.
2. Khi khách đồng ý, Technician bấm **Ghi nhận khách xác nhận**. Hệ thống chuyển `COMPLETED → CUSTOMER_ACCEPTED` và freeze billing snapshot.
3. Technician chọn đúng tình huống thanh toán và xác nhận lại trước khi lưu: **Khách đã chuyển khoản**, **Đã nhận tiền mặt**, hoặc **Hẹn thanh toán tại quầy** nếu kỹ thuật viên chưa thu tiền và đã hướng dẫn khách gặp CSKH. Ảnh giao dịch chỉ là bằng chứng hỗ trợ, không đồng nghĩa tiền đã SETTLED.
4. Customer Service mở **Xử lý thanh toán**. Khoản chuyển khoản, tiền mặt bàn giao hoặc **chờ thanh toán tại quầy** đều xuất hiện trong hàng đợi. Bấm **Đối soát thanh toán** để mở đúng Work Order ở tab **Thanh toán**. Với khách hẹn thanh toán tại quầy, CSKH chỉ chọn **Đã nhận chuyển khoản tại quầy** hoặc **Đã nhận tiền mặt tại quầy** sau khi thực nhận đủ tiền và hoàn tất bước xác nhận lại. Thành công mới đưa payment về `SETTLED`.
5. Sau `SETTLED`, ngay trong Work Order hiện **Phát hành / tải biên nhận** và **Đóng phiếu**. Nếu CSKH rời Work Order trước khi đóng, phiếu không còn nằm lẫn trong danh sách **Phiếu công việc** đang vận hành mà xuất hiện tại **Lịch sử phiếu** theo đúng thứ tự sắp xếp hiện tại, với trạng thái **Chờ hoàn tất hồ sơ**. Icon hoàn tất hồ sơ tại cột **Thao tác** đưa CSKH về **Xử lý thanh toán** và tự tìm đúng mã WO để tiếp tục **Đóng phiếu**; không dùng highlight hoặc pin riêng để giữ UI đồng bộ. Backend bảo đảm receipt tồn tại trước khi chuyển `CUSTOMER_ACCEPTED → CLOSED`; sau khi đóng, hồ sơ vẫn ở Lịch sử phiếu với trạng thái **Đã đóng**.
6. Vật tư outstanding không chặn closure. Warehouse vẫn được RETURN phần hợp lệ sau CLOSED; Work Order giữ nguyên `CLOSED`.
7. Nếu khách báo cùng sự cố trước customer acceptance, CSKH có thể reopen theo policy. Sau `CUSTOMER_ACCEPTED`/`CLOSED`, không reopen silent; sự cố mới đi qua Service Request/Work Order mới.

## 3. Quy tắc người dùng cần biết

- Bộ lọc dạng danh sách: các dropdown dùng để lọc bảng hiển thị checkbox và cho phép chọn nhiều giá trị; bỏ chọn hết tương đương **Tất cả**. Bộ lọc vẫn chạy ở backend trên toàn bộ tập dữ liệu trước pagination, không chỉ lọc các dòng của trang hiện tại. Các dropdown nghiệp vụ chỉ được chọn một giá trị (khách hàng, kỹ thuật viên, mức ưu tiên, kênh tiếp nhận, role...) vẫn giữ single-select.
- Phản hồi biểu mẫu: các trường bắt buộc có dấu đánh dấu. Nếu bấm Lưu/Hoàn thành khi còn thiếu dữ liệu, hệ thống không gửi request; form cuộn tới lỗi đầu tiên và hiển thị cảnh báo ngắn để biết cần bổ sung gì. Các nút xác nhận dùng tên hành động cụ thể thay cho “Đồng ý” ở các flow chính.
- Hoàn thành Work Order: kỹ thuật viên phải nhập **Chẩn đoán / nguyên nhân** và **Giải pháp đã thực hiện**. Sau đó Technician ghi actual-used/chi phí, ghi nhận khách xác nhận và phương thức thanh toán tại hiện trường; CSKH mới đối soát tiền, phát hành biên nhận và đóng phiếu. Owner giám sát outcome thay vì thao tác routine.
- Notification drawer là hàng đợi **việc cần chú ý**, không phải lịch sử CRUD. Title cho biết việc gì + mã `WO-...`/SKU; body cho biết **ai vừa thao tác, đang nói tới khách hàng/công việc/phụ tùng nào và cần làm gì tiếp theo**. Với CSKH, chuông cũng nhắc payment handoff cần hành động (chuyển khoản, tiền mặt KTV đang giữ, khách hẹn thanh toán tại quầy); việc đối soát chi tiết vẫn thực hiện trong **Xử lý thanh toán**. Ví dụ Dispatcher thấy **Cần phân công kỹ thuật viên: WO-...** kèm summary + tên khách và hướng dẫn mở Lịch điều phối; Technician thấy **Bạn có công việc mới: WO-...** kèm người giao, khách hàng và hướng dẫn mở Lịch của tôi. CRUD/master-data/import/attachment bình thường không tạo chuông. Tiến độ một Work Order xem ở **Tiến trình**, ledger kho xem ở **Lịch sử biến động**, truy vết toàn hệ thống xem ở **Audit** (Owner-only).
- Bấm biểu tượng chuông để xem; thông báo chưa đọc có nền nổi bật. Bấm dòng chưa đọc để chuyển sang đã đọc. Mỗi dòng có nút trạng thái ở ngoài cùng bên phải để chuyển Đã đọc ↔ Chưa đọc; dùng Đánh dấu chưa đọc khi cần giữ một thông báo để theo dõi lại.
- Work order phải đi đúng vòng đời; không thể nhảy trạng thái tùy ý.
- Work order đã đóng hoặc hủy không được tạo yêu cầu mới/cấp mới/chỉnh actual-used; Warehouse vẫn có thể nhận RETURN phần outstanding hợp lệ sau CLOSED.
- Mỗi kỹ thuật viên có tài khoản riêng liên kết 1-1 với `technician_profile`; lịch cá nhân được backend suy ra từ JWT và không thể đổi ID để xem lịch người khác.
- Kỹ thuật viên chỉ nhận thông tin khách hàng cần thiết trong Work Order được giao; không thể dùng Work Order/My Schedule để đọc job của kỹ thuật viên khác.
- Kỹ thuật viên chỉ thao tác Work Order được giao: tiến độ hiện trường, phụ tùng/actual-used, billing draft, ghi nhận khách xác nhận và payment action tại hiện trường. Customer Service phụ trách reopen/cancel theo policy trước acceptance và phải nhập lý do cho hai thao tác này; CSKH cũng phụ trách payment reconciliation, biên nhận và normal closure. Owner giám sát/cấu hình; Dispatcher phụ trách điều phối/schedule/reschedule và operational cancellation.
- Username tài khoản được cố định sau khi tạo để giữ ổn định audit/ownership; Owner vẫn có thể đổi họ tên hiển thị, mật khẩu và trạng thái tài khoản theo policy. Trang **Người dùng** có bộ lọc **Tất cả trạng thái / Hoạt động / Tạm ngưng** kết hợp với tìm kiếm; các guard self-disable, last-owner và demo account vẫn bắt buộc.
- Với Kỹ thuật viên chỉ có **một trạng thái nghiệp vụ Hoạt động/Tạm ngưng**. Hai màn hình **Người dùng** và **Đội ngũ kỹ thuật** là hai điểm quản trị của cùng trạng thái và được đồng bộ hai chiều trong cùng transaction: đổi ở một màn hình thì màn hình kia phản ánh ngay sau refresh/query invalidation. **Hoạt động** = có thể đăng nhập và nhận lịch mới; **Tạm ngưng** = không đăng nhập và không nhận lịch mới. Backend vẫn giữ trường mirror ở `technician_profiles` để tương thích dữ liệu hiện tại nhưng không cho phép hai trạng thái vận hành độc lập.
- **Trợ lý AI** tự dùng role của tài khoản đang đăng nhập. Có thể hỏi tổng quát “Trong vai trò này tôi được làm gì?” để nhận overview; sau đó hỏi sâu từng chức năng. AI không mở rộng sang quyền role khác: ví dụ Dispatcher không được hướng dẫn quản trị user/kho, Technician không được hướng dẫn kiểm kê/sửa ngưỡng, Warehouse không được hướng dẫn Work Order hiện trường. Mỗi câu trả lời hiển thị badge **Gemini** hoặc **Nội bộ** nhất quán với nguồn xử lý; badge này không tiết lộ credential hay chi tiết lỗi kỹ thuật.
- Serial thiết bị, mã khách hàng, SKU phụ tùng và mã work order được kiểm soát duy nhất trong tenant.
- File local chỉ chấp nhận JPG, PNG, WEBP và PDF, tối đa 10 MB.

## 4. Reset dữ liệu demo

Khi cần làm sạch toàn bộ database local, ưu tiên script có guard + backup:

```powershell
.\scripts\reset-local-db.ps1
```

Script chỉ cho phép host local, kiểm quyền admin/role ứng dụng trước khi drop, mặc định tạo + verify backup và yêu cầu gõ lại đúng tên database. Với PostgreSQL native mà account ứng dụng không có `CREATEDB`, chạy `.\scripts\reset-local-db.ps1 -AdminUser postgres` và nhập password quản trị khi được hỏi.

Nếu PostgreSQL chạy bằng Docker và bạn **chủ động chấp nhận xóa cả volume** thay vì giữ backup:

```powershell
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml up -d
```

`down -v` xóa cả PostgreSQL volume; không dùng cách này chỉ để dọn vài record E2E/UAT. Sau reset, khởi động backend với `DEMO_PASSWORD=Demo@2026` để seed lại cùng credential với frontend.
