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
Technician ghi nhận khách xác nhận + freeze billing snapshot
        ↓
CUSTOMER_ACCEPTED
        ↓
Technician ghi nhận khách chuyển khoản / nhận tiền mặt
        ↓
CSKH đối soát → SETTLED → biên nhận thanh toán
        ↓
CSKH đóng phiếu → CLOSED → Lịch sử phiếu
        ↓
Dashboard + timeline + audit + notification được cập nhật
```

Work Order public flow **không có generic direct-create API**. Nguồn tạo chuẩn là `Service Request → Work Order`; demo seed cũng giữ source Service Request cho các Work Order mẫu.

## 2. State transition

Luồng chính:

```text
OPEN → SCHEDULED → ASSIGNED → ON_THE_WAY → IN_PROGRESS
     → COMPLETED → CUSTOMER_ACCEPTED
     → [payment SETTLED] → CLOSED
```

Nhánh:

```text
IN_PROGRESS → WAITING_FOR_PARTS → IN_PROGRESS
COMPLETED → REOPENED → IN_PROGRESS hoặc SCHEDULED khi CSKH tiếp nhận cùng sự cố trước customer acceptance.
Sau `CUSTOMER_ACCEPTED`, billing/payment đã freeze nên không reopen workflow cũ. `CLOSED`/`CANCELLED` là terminal; nhu cầu/sự cố mới phải đi qua Service Request/Work Order mới.
```

Các trạng thái active có thể hủy theo policy:

```text
OPEN / SCHEDULED / ASSIGNED / ON_THE_WAY /
IN_PROGRESS / WAITING_FOR_PARTS / REOPENED → CANCELLED
```

Role ownership của transition:

- `TECHNICIAN`: trên Work Order được giao cho chính mình có thể `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`; customer acceptance dùng action nghiệp vụ riêng sau COMPLETED và đồng thời freeze billing snapshot. Technician không close/reopen bằng generic transition.
- `CUSTOMER_SERVICE`: có thể `REOPENED`/`CANCELLED` theo policy trước customer acceptance; sau `SETTLED` dùng action closure riêng để chuyển `CUSTOMER_ACCEPTED → CLOSED`.
- `OWNER`: giám sát outcome, quản trị/cấu hình và cancellation theo policy; không customer-accept, settle hoặc normal-close thay role phụ trách.
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

- `REQUEST` là yêu cầu nghiệp vụ của Technician, không làm thay đổi tồn kho. Chỉ Technician được giao Work Order ở `ASSIGNED`, `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS` hoặc `REOPENED` mới được tạo request.
- Với cùng Work Order + phụ tùng, tại một thời điểm chỉ có tối đa một request `REQUESTED` active. Technician được sửa quantity/note hoặc hủy trước khi Warehouse cấp; lý do hủy phải do người thao tác nhập.
- Warehouse không sửa quantity thay Technician. Warehouse có thể **Xác nhận cấp** hoặc **Không thể cấp**; lý do không thể cấp là dữ liệu người dùng nhập.
- `ISSUE` là lúc Warehouse giao vật tư thực tế: stock giảm đúng một lần, request trở thành immutable và ledger lưu actor/snapshot giao nhận.
- Technician ghi `USED` là số lượng thực tế dùng cho khách; thao tác này không đổi stock lần nữa. Actual-used được cập nhật đến `COMPLETED` và khóa sau `CUSTOMER_ACCEPTED`.
- Outstanding theo Work Order + part = `ISSUE - USED - RETURN`. `RETURN` chỉ do Warehouse xác nhận khi nhận hàng thực tế, làm stock tăng và không được vượt outstanding. RETURN hợp lệ vẫn được phép sau `CLOSED` và không reopen Work Order.
- `CONSUME` chỉ còn là dữ liệu lịch sử legacy để đọc/đối chiếu; active API/UI không còn write path tạo CONSUME. Không rewrite destructive lịch sử cũ.
- OWNER/WAREHOUSE_STAFF có thể chỉnh **ngưỡng tồn tối thiểu** (`reorderLevel`); thao tác này không đổi stock, có audit và chỉ cảnh báo Warehouse khi stock vừa chuyển sang mức thấp.
- Warehouse kiểm kê bằng số lượng thực tế; chênh lệch tạo `ADJUSTMENT_IN`/`ADJUSTMENT_OUT` có lý do và actor. Owner nhận thông báo chênh lệch, Warehouse nhận cảnh báo tồn thấp khi phù hợp.
- `inventory_transactions` là stock authority; màn **Lịch sử biến động** dùng để truy vết hàng thực sự ra/vào kho. Work Order Timeline kể REQUEST/ISSUE/USED/RETURN cùng status/payment/receipt nhưng không thay thế ledger hay audit.
- Billing/customer charge dùng `USED`, không dùng `ISSUE - RETURN`. Khi khách xác nhận, hệ thống khóa billing snapshot; payment dùng snapshot này và biên nhận sau `SETTLED` không thay đổi bởi catalog/RETURN về sau.
- Locking + validation + transaction ngăn stock âm, double issue và serialize thay đổi cùng một SKU.

## 5. Quyền thao tác

- `OWNER`: quản trị tài khoản/cấu hình và có quyền quản lý trên các module nghiệp vụ dành cho Owner: Customer/Asset, Service Request (kể cả chuyển sang Work Order), Channel, điều phối, kỹ thuật viên, kho/kiểm kê/lịch sử biến động, Work Order history và audit. Trong Work Order, Owner là admin override cho điều phối và hậu xử lý nhưng không giả lập field progress hoặc xác nhận vật tư/actual-used thay role nghiệp vụ.
- `DISPATCHER`: Customer/Asset read-only để lấy ngữ cảnh điều phối; xem Work Order, kỹ thuật viên; assign/schedule/reschedule; operational cancellation và lịch sử phiếu. Không tiếp nhận Service Request, không xem Audit toàn hệ thống hoặc xác nhận/đóng phiếu.
- `CUSTOMER_SERVICE`: Customer/Asset create-update-delete theo guard; Service Request intake/update/cancel/delete; chuyển Service Request sang Work Order; tiếp nhận phản hồi sau dịch vụ và có thể mở lại/hủy phiếu theo policy.
- `TECHNICIAN`: My Schedule + Work Order được giao; field transitions; evidence; tạo/sửa/hủy yêu cầu phụ tùng, ghi actual-used, billing draft và ghi nhận khách xác nhận tại hiện trường.
- `CUSTOMER_SERVICE`: đối soát transfer/cash, phát hành biên nhận sau `SETTLED`, đóng phiếu; có thể reopen/cancel theo policy trước khi customer acceptance freeze billing.
- `OWNER`: quản trị/giám sát, cấu hình bank/QR công ty và xem payment/receipt; không thao tác routine settlement/closure thay role phụ trách.
- `WAREHOUSE_STAFF`: `/part-requests`, `/inventory`, `/inventory-stocktake`, `/inventory-movements` và API kho; xác nhận cấp/không thể cấp/RETURN, không có Work Order operational dashboard.

## 6. Delete / cancel / deactivate

- Service Request và Work Order nghiệp vụ ưu tiên state (`CANCELLED`) thay cho hard delete khi đã có lịch sử vận hành.
- Work Order `CLOSED`/`CANCELLED` chỉ Owner được ẩn khỏi lịch sử tra cứu; audit vẫn được giữ.
- Asset/Service Request chưa có operational reference vẫn không được hard-delete nếu còn attachment; phải xử lý attachment trước để tránh orphan metadata/file.
- Customer `active=false` vẫn giữ trong danh mục và toàn bộ lịch sử cũ, nhưng không được dùng để tạo Service Request hoặc đăng ký Asset mới. Backend áp cùng invariant để API trực tiếp không thể bypass UI. Record đã tồn tại vẫn được phép hoàn thiện/chỉnh sửa với chính khách hàng cũ để không phá hồ sơ đang xử lý.
- Technician có assignment operational không được deactivate.

## 7. Workspace theo vai trò

- Owner/Dispatcher dùng **Lịch điều phối**. Dispatcher không có menu Customer/Asset riêng dù backend vẫn cho đọc dữ liệu nền cần thiết; ngữ cảnh chính được xem từ Work Order. **Nhật ký hệ thống** là workspace quản trị của Owner.
- Technician dùng **Lịch của tôi**; backend suy ra `TechnicianProfile` từ JWT `userId`, client không gửi `technicianId`.
- Warehouse đăng nhập/điều hướng mặc định vào **Yêu cầu phụ tùng** (`/part-requests`) để xử lý request đang chờ; Kho phụ tùng/kiểm kê/lịch sử biến động là các workspace kho kế tiếp. Warehouse không vào operational dashboard.
- Hai Technician cùng role vẫn là hai identity riêng; role quyết định quyền, `UserAccount` quyết định ownership/audit accountability.

- Inventory movement history snapshots actor name/role cho stock movement (`ISSUE`, `RETURN`, import/adjustment). Mục đích sử dụng nằm ở part request/actual-used của Work Order; `CONSUME` chỉ còn compatibility lịch sử.
- Work Order Timeline snapshot `actor_display_name` + `actor_role` tại thời điểm thao tác. UI hiển thị actor ngay cùng tiêu đề activity theo dạng **Trạng thái · Tên vai trò · Họ tên**, ví dụ `Đang mở · Chăm sóc khách hàng · Trần Mai CSKH`; event hệ thống hiển thị `Hệ thống`. Username chỉ còn là technical fallback cho dữ liệu legacy không thể backfill.
- Mỗi lần chuyển sang `COMPLETED` lưu snapshot riêng của **Chẩn đoán / nguyên nhân**, **Giải pháp đã thực hiện** và ghi chú bàn giao (nếu có) trong status history. Nếu phiếu được `REOPENED`, form Hoàn thành điền sẵn chẩn đoán/giải pháp mới nhất để kỹ thuật viên giữ nguyên hoặc cập nhật; lần hoàn thành tiếp theo tạo snapshot mới, không ghi đè repair cycle trước. Phần Tổng quan tiếp tục lấy `work_orders.diagnosis`/`resolution` làm kết quả mới nhất.

### Trợ lý AI theo vai trò

- Role của AI Help được backend suy ra từ JWT; client không được tự chọn role để mở rộng phạm vi hướng dẫn.
- Câu hỏi tổng quát như “Tôi được làm gì?” trả overview đúng workspace của role hiện tại. OWNER được mô tả toàn bộ phạm vi quản trị; các role khác chỉ nhận hướng dẫn thuộc trách nhiệm được cấp.
- Knowledge base tách nghiệp vụ dễ nhầm quyền: Dispatcher có điều phối/reschedule nhưng không User Management/Service Request intake/kho; Customer Service có intake/convert/follow-up nhưng không điều phối/accept/close; Technician chỉ job được giao/My Schedule/phụ tùng ngay trong Work Order, không có workspace Kho phụ tùng và không quản trị kho; Warehouse không có operational Work Order/dashboard.
- AI chỉ hướng dẫn thao tác; không đọc runtime database và không tự thực hiện mutation.

## 8. Chính sách phản hồi người dùng và notification

- Validation phía client: trường bắt buộc hiển thị rõ; submit thiếu dữ liệu không gọi API, cuộn tới lỗi đầu tiên và hiện cảnh báo ngắn.
- Mutation đã gửi: người thao tác luôn nhận phản hồi thành công hoặc lỗi tại màn hình hiện tại; không để nút bấm thất bại im lặng.
- Query/dữ liệu phụ trợ lỗi: hiển thị trạng thái lỗi + `Thử lại`, không render dữ liệu rỗng như thể tải thành công.
- Notification chuông dành cho thay đổi liên vai trò hoặc sự kiện cần người nhận chú ý/hành động; không broadcast mọi bước tiến độ để tránh spam. OWNER là audience quản trị ngoại lệ, không phải recipient mặc định của mọi module.
- Copy notification theo cấu trúc **title = việc cần chú ý + mã tra cứu**, **body = ai + đối tượng nghiệp vụ + chuyện gì xảy ra + bước tiếp theo**. Work Order notification ưu tiên summary + khách hàng; inventory notification ưu tiên SKU + tên phụ tùng + số lượng/ngưỡng. Enum nội bộ, chuỗi test, raw timestamp và audit detail không được dùng làm nội dung chính.
- Notification được route theo **việc cần hành động**, không theo quyền xem rộng. Dispatcher nhận **Cần phân công kỹ thuật viên**, **Phiếu đang chờ phụ tùng**, **Phiếu cần xử lý lại** và cảnh báo **Phiếu đã quá lịch thực hiện** khi một appointment kết thúc mà field work chưa bắt đầu. Assigned Technician nhận cảnh báo **Công việc đã quá lịch** cho chính lịch của mình. Overdue alert được quét định kỳ ở backend và dedupe theo recipient + appointment window, nên refresh UI hoặc các lần quét sau không tạo spam; nếu lịch được dời sang một window mới và window mới lại quá hạn thì đó là một alert mới hợp lệ. Customer Service không nhận overdue ngay lập tức; nếu quá hạn kéo dài qua grace period cấu hình (mặc định 15 phút) mà WO vẫn chưa bắt đầu, CSKH nhận đúng một cảnh báo **Khách hàng có thể cần được liên hệ** để chủ động trả lời/tương tác với khách. Customer Service cũng nhận **Cần theo dõi khách sau sửa chữa**, và nhận alert khi phiếu được `REOPENED`/`CANCELLED` bởi role khác vì đây là sự kiện cần customer communication. Technician nhận **Bạn có công việc mới**, **Lịch của bạn đã thay đổi**, chuyển giao, mở lại, hủy hoặc đóng phiếu khi sự kiện do người khác thực hiện. Warehouse nhận **yêu cầu phụ tùng mới** cần xử lý và cảnh báo tồn kho; Owner không nhận reopen, overdue, waiting-for-parts hoặc low-stock vận hành. Owner chỉ nhận kết quả cuối `CLOSED`/`CANCELLED` và ngoại lệ quản trị stocktake discrepancy để bell đóng vai trò giám sát thay vì audit log.
- CRUD thường ngày của Customer/Asset/Service Request/Channel, cập nhật Technician profile, attachment, tạo/import catalog và nhập kho **không tạo bell notification** cho bất kỳ role nào. Người thao tác đã có success/error feedback tại màn hình; người cần truy vết dùng workspace/Audit. Low-stock của workflow hiện hành có thể phát khi Warehouse `ISSUE` làm stock chuyển từ trên ngưỡng xuống chạm/thấp hơn `reorderLevel`; các ISSUE tiếp theo khi part đã low-stock không lặp lại cùng cảnh báo. `CONSUME` legacy chỉ còn read compatibility; active API/UI không tạo giao dịch này.
- Copy runtime được gom về `NotificationCopy`; `AppLayout` không tự ghép nội dung notification. `V7__notification_feed_cleanup.sql` loại khỏi bell các row CRUD/import/generic-status cũ vốn đã có Audit/Timeline làm nguồn truy vết, nhờ đó unread count không còn tính cả noise. Các actionable legacy assignment/reschedule/reopen/cancel còn lại được normalize riêng tại `features/notifications/presentation.ts`.
- Notification read/unread là trạng thái của đúng recipient; lỗi đổi trạng thái phải được báo và không giả vờ cập nhật badge/list.
