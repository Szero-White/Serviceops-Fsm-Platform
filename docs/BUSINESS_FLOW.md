# Quy trình nghiệp vụ

## 1. Từ yêu cầu khách hàng đến đóng công việc

```text
Customer/CSKH tạo Service Request
        ↓
Xác thực khách hàng + thiết bị + mức ưu tiên
        ↓
Customer Service chuyển Service Request thành Work Order
        ↓
Dispatcher chọn kỹ thuật viên và khung giờ
        ↓
Hệ thống khóa kỹ thuật viên, kiểm tra lịch chồng lấn
        ↓
Technician: ON_THE_WAY → IN_PROGRESS
        ↓
Ghi chẩn đoán, giải pháp, phụ tùng và file minh chứng
        ↓
COMPLETED
        ↓
Technician được giao hoặc Owner ghi nhận khi khách đồng ý
        ↓
CUSTOMER_ACCEPTED
        ↓
Đóng phiếu → CLOSED → Lịch sử phiếu
        ↓
Dashboard + lịch sử + audit + notification được cập nhật
```

Work Order public flow **không có generic direct-create API**. Nguồn tạo chuẩn là `Service Request → Work Order`; demo seed cũng giữ source Service Request cho các Work Order mẫu.

## 2. State transition

Luồng chính:

```text
OPEN → SCHEDULED → ASSIGNED → ON_THE_WAY → IN_PROGRESS
     → COMPLETED → CUSTOMER_ACCEPTED → CLOSED
```

Nhánh:

```text
IN_PROGRESS → WAITING_FOR_PARTS → IN_PROGRESS
COMPLETED/CUSTOMER_ACCEPTED → REOPENED → IN_PROGRESS hoặc SCHEDULED
CANCELLED là trạng thái kết thúc; nếu khách phát sinh nhu cầu mới sau khi hủy thì tạo yêu cầu/phiếu mới thay vì mở lại phiếu đã hủy.
```

Các trạng thái active có thể hủy theo policy:

```text
OPEN / SCHEDULED / ASSIGNED / ON_THE_WAY /
IN_PROGRESS / WAITING_FOR_PARTS / REOPENED → CANCELLED
```

Role ownership của transition:

- `TECHNICIAN`: trên Work Order được giao cho chính mình có thể `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`; sau hoàn thành có thể ghi nhận `CUSTOMER_ACCEPTED`, `CLOSED` hoặc `REOPENED` nếu cùng sự cố cần xử lý lại trước khi đóng.
- `OWNER`: quyền quản trị cho bước hậu xử lý Work Order: `CUSTOMER_ACCEPTED`, `CLOSED`, `REOPENED`, `CANCELLED`. Owner không dùng generic transition để giả lập tiến độ hiện trường và không consume phụ tùng thay Technician.
- `CUSTOMER_SERVICE`: tiếp nhận phản hồi khách; có thể `REOPENED` hoặc `CANCELLED` khi khách yêu cầu thay đổi, nhưng không bấm Khách xác nhận/Đóng phiếu.
- `DISPATCHER`: điều phối/schedule/reschedule và operational cancellation; không thực hiện field progress, customer acceptance hoặc close.

Transition không hợp lệ về state trả `409 INVALID_STATUS_TRANSITION`; transition đúng state nhưng sai role trả `403 WORK_ORDER_TRANSITION_FORBIDDEN`.

## 3. Quy tắc xếp lịch

Hai khoảng thời gian chồng lấn khi:

```text
newStart < existingEnd AND newEnd > existingStart
```

Khi hai điều phối viên gửi request đồng thời, pessimistic lock trên technician làm tuần tự hóa thao tác. Request thứ hai nhìn thấy appointment vừa commit và nhận `409 TECHNICIAN_SCHEDULE_CONFLICT`.

Kỹ thuật viên đang bị tạm ngưng, account inactive hoặc không còn role `TECHNICIAN` không thể nhận lịch mới. Owner cũng không được tạm ngưng account/profile kỹ thuật viên khi còn Work Order operational đang gán cho người đó; phải điều phối lại hoặc hủy công việc trước.

Dispatcher hoặc Owner có thể **điều phối lại** kỹ thuật viên/lịch khi phiếu vẫn ở `OPEN`, `SCHEDULED`, `ASSIGNED` hoặc `REOPENED`, tức trước khi kỹ thuật viên bắt đầu di chuyển/thực hiện. Lịch hẹn đã kết thúc nhưng phiếu vẫn ở nhóm trạng thái này được đánh dấu **Quá hạn** trên Lịch điều phối và vẫn có thể dời sang một khoảng thời gian tương lai hợp lệ. Điều phối lại bắt buộc có lý do, được ghi audit `RESCHEDULE` và xuất hiện trong tab **Tiến trình** như một activity điều phối riêng. Nếu đổi kỹ thuật viên, người cũ nhận thông báo đã được điều chuyển khỏi phiếu và người mới nhận thông báo công việc mới; nếu chỉ đổi lịch, kỹ thuật viên hiện tại nhận thông báo gồm lịch cũ, lịch mới và lý do thay đổi. Khi WO đã `ON_THE_WAY` hoặc `IN_PROGRESS`, endpoint schedule/reschedule từ chối để tránh bàn giao ngầm trong khi field work đang diễn ra. Việc đổi kỹ thuật viên, đổi thời gian hoặc đổi cả hai từ trang **Lịch điều phối** và từ chi tiết Work Order đều đi qua cùng endpoint, vì vậy cùng tạo một event `RESCHEDULE`; UI Tiến trình chỉ trình bày phần thay đổi theo giờ địa phương, còn audit vẫn giữ timestamp ISO để truy vết.

## 4. Quy tắc tồn kho

- Số lượng phải lớn hơn 0.
- Chỉ Work Order `ASSIGNED`, `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS` hoặc `REOPENED` mới được ghi nhận CONSUME mới.
- `COMPLETED`, `CUSTOMER_ACCEPTED`, `CLOSED`, `CANCELLED`, cũng như các trạng thái trước khi thực thi, không được dùng thêm phụ tùng.
- Technician chỉ consume phụ tùng cho Work Order được giao cho mình; backend kiểm tra cả role, ownership và trạng thái phiếu.
- Warehouse quản lý catalog/import/lifecycle stock nhưng không consume thay Technician trong operational Work Order flow. OWNER/WAREHOUSE_STAFF có thể chỉnh **ngưỡng tồn tối thiểu** (`reorderLevel`) của phụ tùng; thao tác này không đổi stock, được ghi audit và chỉ phát cảnh báo khi ngưỡng mới làm current stock mới rơi vào trạng thái tồn thấp.
- Warehouse kiểm kê bằng số lượng thực tế; chênh lệch tạo `ADJUSTMENT_IN`/`ADJUSTMENT_OUT` có lý do và actor. Sau commit, Owner nhận thông báo chênh lệch; nếu tồn sau kiểm kê `<= reorderLevel` (ngưỡng tồn tối thiểu), Warehouse nhận cảnh báo tồn thấp. Technician không nhận broadcast kiểm kê vì hiện chưa có part-request/reservation để xác định WO bị ảnh hưởng.
- Warehouse có thể xác nhận nhận lại phụ tùng đã ghi nhận cho Work Order trước khi phiếu CLOSED/CANCELLED; tổng RETURN không được vượt tổng CONSUME cùng part trên cùng Work Order.
- Mỗi thay đổi tạo `inventory_transactions` với `balance_after`; màn Lịch sử biến động là stock ledger chuyên biệt, khác audit log toàn hệ thống. Work Order detail dùng read model `activities` để ghép status history, điều phối và `CONSUME` hợp lệ của Technician theo thời gian. Warehouse `RETURN` vẫn được giữ đầy đủ ở stock ledger và dùng cho đối soát/invoice, nhưng không hiển thị như tiến trình hiện trường của Work Order để tránh làm Dispatcher hiểu nhầm Warehouse là người thực hiện job.
- Invoice tính phụ tùng theo net `CONSUME - RETURN`, tránh tính phí phần đã hoàn lại.
- Locking + validation + transaction ngăn stock âm và serialize thay đổi cùng một SKU.

## 5. Quyền thao tác

- `OWNER`: quản trị tài khoản/cấu hình và có quyền quản lý trên các module nghiệp vụ dành cho Owner: Customer/Asset, Service Request (kể cả chuyển sang Work Order), Channel, điều phối, kỹ thuật viên, kho/kiểm kê/lịch sử biến động, Work Order history và audit. Trong Work Order, Owner là admin override cho điều phối và hậu xử lý nhưng không giả lập field progress hoặc consume phụ tùng thay Technician.
- `DISPATCHER`: Customer/Asset read-only; xem Work Order, kỹ thuật viên; assign/schedule/reschedule; operational cancellation; xem lịch sử/audit. Không tiếp nhận Service Request hoặc xác nhận/đóng phiếu.
- `CUSTOMER_SERVICE`: Customer/Asset create-update-delete theo guard; Service Request intake/update/cancel/delete; chuyển Service Request sang Work Order; tiếp nhận phản hồi sau dịch vụ và có thể mở lại/hủy phiếu theo policy.
- `TECHNICIAN`: My Schedule + Work Order được giao; field transitions; evidence; consume spare part cho chính job; sau `COMPLETED` có thể ghi nhận Khách xác nhận, Đóng phiếu hoặc Mở lại cùng job trước khi đóng.
- `OWNER`: quản trị/giám sát và có admin override cho Khách xác nhận, Đóng phiếu, Mở lại/Hủy phiếu.
- `WAREHOUSE_STAFF`: `/inventory`, `/inventory-stocktake`, `/inventory-movements` và API kho; không có Work Order operational API hoặc operational dashboard.

## 6. Delete / cancel / deactivate

- Service Request và Work Order nghiệp vụ ưu tiên state (`CANCELLED`) thay cho hard delete khi đã có lịch sử vận hành.
- Work Order `CLOSED`/`CANCELLED` chỉ Owner được ẩn khỏi lịch sử tra cứu; audit vẫn được giữ.
- Asset/Service Request chưa có operational reference vẫn không được hard-delete nếu còn attachment; phải xử lý attachment trước để tránh orphan metadata/file.
- Customer `active=false` vẫn giữ trong danh mục và toàn bộ lịch sử cũ, nhưng không được dùng để tạo Service Request hoặc đăng ký Asset mới. Backend áp cùng invariant để API trực tiếp không thể bypass UI. Record đã tồn tại vẫn được phép hoàn thiện/chỉnh sửa với chính khách hàng cũ để không phá hồ sơ đang xử lý.
- Technician có assignment operational không được deactivate.

## 7. Workspace theo vai trò

- Owner/Dispatcher dùng **Lịch điều phối**.
- Technician dùng **Lịch của tôi**; backend suy ra `TechnicianProfile` từ JWT `userId`, client không gửi `technicianId`.
- Warehouse đăng nhập/điều hướng mặc định vào **Kho phụ tùng**, không vào operational dashboard.
- Hai Technician cùng role vẫn là hai identity riêng; role quyết định quyền, `UserAccount` quyết định ownership/audit accountability.

- Inventory movement history snapshots actor name/role and requires a purpose when parts are consumed for a Work Order.

### Trợ lý AI theo vai trò

- Role của AI Help được backend suy ra từ JWT; client không được tự chọn role để mở rộng phạm vi hướng dẫn.
- Câu hỏi tổng quát như “Tôi được làm gì?” trả overview đúng workspace của role hiện tại. OWNER được mô tả toàn bộ phạm vi quản trị; các role khác chỉ nhận hướng dẫn thuộc trách nhiệm được cấp.
- Knowledge base tách nghiệp vụ dễ nhầm quyền: Dispatcher có điều phối/reschedule nhưng không User Management/Service Request intake/kho; Customer Service có intake/convert/follow-up nhưng không điều phối/accept/close; Technician chỉ job được giao/My Schedule/phụ tùng cho job, không quản trị kho; Warehouse không có operational Work Order/dashboard.
- AI chỉ hướng dẫn thao tác; không đọc runtime database và không tự thực hiện mutation.

## 8. Chính sách phản hồi người dùng và notification

- Validation phía client: trường bắt buộc hiển thị rõ; submit thiếu dữ liệu không gọi API, cuộn tới lỗi đầu tiên và hiện cảnh báo ngắn.
- Mutation đã gửi: người thao tác luôn nhận phản hồi thành công hoặc lỗi tại màn hình hiện tại; không để nút bấm thất bại im lặng.
- Query/dữ liệu phụ trợ lỗi: hiển thị trạng thái lỗi + `Thử lại`, không render dữ liệu rỗng như thể tải thành công.
- Notification chuông dành cho thay đổi liên vai trò hoặc sự kiện cần người nhận chú ý/hành động; không broadcast mọi bước tiến độ để tránh spam. OWNER là audience quản trị ngoại lệ, không phải recipient mặc định của mọi module.
- Copy notification theo cấu trúc **title = việc cần chú ý + mã tra cứu**, **body = ai + đối tượng nghiệp vụ + chuyện gì xảy ra + bước tiếp theo**. Work Order notification ưu tiên summary + khách hàng; inventory notification ưu tiên SKU + tên phụ tùng + số lượng/ngưỡng. Enum nội bộ, chuỗi test, raw timestamp và audit detail không được dùng làm nội dung chính.
- Notification được route theo **việc cần hành động**, không theo quyền xem rộng. Dispatcher nhận **Cần phân công kỹ thuật viên**, **Phiếu đang chờ phụ tùng**, **Phiếu cần xử lý lại** và cảnh báo **Phiếu đã quá lịch thực hiện** khi một appointment kết thúc mà field work chưa bắt đầu. Assigned Technician nhận cảnh báo **Công việc đã quá lịch** cho chính lịch của mình. Overdue alert được quét định kỳ ở backend và dedupe theo recipient + appointment window, nên refresh UI hoặc các lần quét sau không tạo spam; nếu lịch được dời sang một window mới và window mới lại quá hạn thì đó là một alert mới hợp lệ. Customer Service nhận **Cần theo dõi khách sau sửa chữa** mà không nhận quyền Khách xác nhận/Đóng phiếu. Technician nhận **Bạn có công việc mới**, **Lịch của bạn đã thay đổi**, chuyển giao, mở lại, hủy hoặc đóng phiếu khi sự kiện do người khác thực hiện. Warehouse nhận cảnh báo tồn kho; Owner không nhận từng overdue vận hành để tránh biến bell thành dashboard SLA.
- CRUD thường ngày của Customer/Asset/Service Request/Channel, cập nhật Technician profile, attachment, tạo/import catalog và nhập kho **không tạo bell notification** cho bất kỳ role nào. Người thao tác đã có success/error feedback tại màn hình; người cần truy vết dùng workspace/Audit. Low-stock do Technician consume chỉ phát khi stock chuyển từ trên ngưỡng xuống chạm/thấp hơn `reorderLevel`; các lần consume tiếp theo khi part đã low-stock không lặp lại cùng cảnh báo.
- Copy runtime được gom về `NotificationCopy`; `AppLayout` không tự ghép nội dung notification. `V7__notification_feed_cleanup.sql` loại khỏi bell các row CRUD/import/generic-status cũ vốn đã có Audit/Timeline làm nguồn truy vết, nhờ đó unread count không còn tính cả noise. Các actionable legacy assignment/reschedule/reopen/cancel còn lại được normalize riêng tại `features/notifications/presentation.ts`.
- Notification read/unread là trạng thái của đúng recipient; lỗi đổi trạng thái phải được báo và không giả vờ cập nhật badge/list.
