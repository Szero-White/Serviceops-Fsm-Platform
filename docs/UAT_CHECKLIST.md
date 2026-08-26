# UAT Checklist — Local MVP

Dùng checklist này trước mỗi bản demo hoặc bàn giao thử nghiệm.

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| AUTH-01 | Đăng nhập đúng tài khoản demo | Nhận JWT và vào dashboard |
| AUTH-02 | Đăng nhập sai mật khẩu | HTTP 401, không lộ chi tiết nội bộ |
| AUTH-03 | Owner mở dữ liệu rồi logout/login ngay bằng Technician/Warehouse | Không render cache dữ liệu identity trước; TanStack Query cache được clear khi đổi identity |
| AUTH-04 | Owner tạm ngưng một user đang có JWT cũ rồi user gọi API lại | JWT cũ bị từ chối; account inactive không tiếp tục thao tác đến hết 480 phút local |
| RBAC-01 | Technician mở trang khách hàng | Bị từ chối HTTP 403 |
| RBAC-02 | Role truy cập trực tiếp URL không thuộc quyền trên frontend | Bị điều hướng về workspace mặc định của role; Warehouse về `/inventory`, các operational role về dashboard; API backend vẫn là lớp bảo vệ cuối |
| RBAC-03 | Warehouse gọi trực tiếp API lịch sử phiếu | Bị từ chối HTTP 403 dù bỏ qua frontend |
| RBAC-04 | Warehouse gọi attachment của Asset/Service Request/Work Order | Bị từ chối HTTP 403; Warehouse chỉ sở hữu nghiệp vụ kho |
| RBAC-05 | Customer Service mở Kênh tiếp nhận | Xem được danh sách nhưng không thấy action thêm/sửa/xoá; cấu hình kênh là OWNER-only |
| RBAC-06 | Warehouse mở trực tiếp /work-orders hoặc gọi API Work Order/attachment | Frontend từ chối route; backend không cấp quyền Work Order cho Warehouse |
| RBAC-07 | Dispatcher gọi API Service Request hoặc Service Channel write | Bị từ chối HTTP 403; Dispatcher chỉ điều phối Work Order đã được handoff |
| RBAC-08 | Warehouse gọi `GET /api/v1/work-orders` hoặc `GET /api/v1/dashboard` | HTTP 403; Warehouse không đọc operational Work Order/dashboard data |
| RBAC-09 | Dispatcher gọi Work Order transition `ON_THE_WAY`, `COMPLETED`, `CUSTOMER_ACCEPTED`, `CLOSED` hoặc `REOPENED` | HTTP 403 `WORK_ORDER_TRANSITION_FORBIDDEN`; Dispatcher chỉ có operational cancellation trong transition endpoint |
| RBAC-10 | Dispatcher mở Đội ngũ kỹ thuật hoặc gọi `PUT /technicians/{id}` | Xem được danh sách; UI không có edit action và backend PUT trả 403 |
| RBAC-11 | Dispatcher thử xóa/ẩn Work Order khỏi history | UI không có action; backend DELETE trả 403; Owner-only archive |
| RBAC-12 | Owner mở Work Order `COMPLETED`/`CUSTOMER_ACCEPTED` | Có admin action **Khách xác nhận / Đóng phiếu / Mở lại** đúng state; Owner vẫn không consume phụ tùng thay Technician |
| RBAC-13 | Customer Service gọi `CUSTOMER_ACCEPTED` hoặc `CLOSED` | HTTP 403 `WORK_ORDER_TRANSITION_FORBIDDEN`; Customer Service chỉ `REOPENED`/`CANCELLED` khi tiếp nhận follow-up của khách |
| DEMO-01 | Owner/Dispatcher mở Kỹ thuật viên ở public demo | Seeded technician hiển thị “Demo cố định” và nút sửa bị khóa; user tự tạo vẫn chỉnh được |
| TENANT-01 | Truy cập ID không thuộc tenant | Không trả dữ liệu |
| CUS-01 | Tạo khách hàng hợp lệ | Khách hàng xuất hiện trong danh sách |
| CUS-02 | Chuyển khách hàng sang Ngừng hoạt động rồi mở form Tiếp nhận yêu cầu | Khách vẫn còn trong trang Khách hàng/lịch sử nhưng không xuất hiện trong selector tạo Service Request mới |
| CUS-03 | Trên trang Khách hàng, chuyển bộ lọc giữa Tất cả trạng thái / Hoạt động / Ngừng hoạt động | Danh sách và tổng số hồ sơ thay đổi đúng theo filter; ô tìm kiếm tiếp tục kết hợp với filter và phân trang quay về trang 1 |
| CUS-04 | Gọi API trực tiếp tạo Service Request với customerId đã ngừng hoạt động | HTTP 409 `CUSTOMER_INACTIVE`; không tạo record mới |
| CUS-05 | Chỉnh sửa Service Request đang OPEN đã tồn tại trước khi khách bị ngừng hoạt động, giữ nguyên customerId | Cho phép cập nhật nội dung hiện hữu; không phá hồ sơ đang xử lý |
| AST-01 | Tạo thiết bị với serial mới | Thiết bị liên kết đúng khách hàng |
| AST-02 | Mở form Thêm thiết bị sau khi khách hàng đã Ngừng hoạt động | Khách inactive không xuất hiện trong selector tạo mới |
| AST-03 | Gọi API/import tạo Asset mới cho khách inactive | Bị từ chối `CUSTOMER_INACTIVE` hoặc row import không hợp lệ; Asset cũ vẫn giữ nguyên |
| AST-04 | Tạo trùng serial trong tenant | Bị từ chối HTTP 409 |
| SR-01 | Tạo service request | Trạng thái OPEN |
| WO-01 | Chuyển service request thành work order | Work order OPEN, request được đánh dấu đã chuyển |
| SCH-01 | Gán lịch hợp lệ | Work order ASSIGNED, có appointment |
| SCH-02 | Hai lịch kỹ thuật viên chồng lấn | Request thứ hai nhận HTTP 409 |
| SCH-03 | Dispatcher mở Lịch điều phối tuần | Thấy kỹ thuật viên, appointment trong range và hàng đợi OPEN/REOPENED |
| SCH-04 | Technician gọi schedule-board API | Bị từ chối HTTP 403 |
| SCH-05 | Technician mở `Lịch của tôi` | Chỉ thấy appointment gắn với technician profile của tài khoản đang đăng nhập |
| SCH-06 | Đăng nhập hai tài khoản technician khác nhau | Hai lịch cá nhân không lẫn dữ liệu; client không truyền technicianId |
| SCH-07 | Owner tạm ngưng technician/profile đang còn Work Order operational | HTTP 409 `TECHNICIAN_ACTIVE_ASSIGNMENTS`; phải điều phối lại/hủy job trước |
| SCH-08 | Điều phối lại appointment đã tồn tại trước khi technician bắt đầu, từ trang Lịch điều phối hoặc chi tiết Work Order | Bắt buộc nhập lý do; đổi kỹ thuật viên, thời gian hoặc cả hai đều dùng cùng endpoint; Tiến trình ghi ngắn gọn đúng phần thay đổi, không hiển thị raw ISO `T...Z`; notification chỉ gửi kỹ thuật viên liên quan |
| SCH-09 | Dispatcher đổi technician khi WO còn ASSIGNED | Technician cũ nhận thông báo đã điều chuyển, technician mới nhận công việc; WO vẫn ASSIGNED; Tiến trình ghi rõ người cũ → người mới và lý do |
| SCH-10 | Dispatcher/Owner thử điều phối lại khi WO đã ON_THE_WAY/IN_PROGRESS | HTTP 409 `WORK_ORDER_ALREADY_STARTED`; không đổi technician/lịch, không tạo audit/notification |
| WO-02 | Chuyển trạng thái hợp lệ | Timeline lưu người thao tác và thời gian |
| WO-03 | Nhảy trạng thái không hợp lệ | HTTP 409 INVALID_STATUS_TRANSITION |
| WO-03A | Customer Service thử `CANCELLED → REOPENED` | HTTP 409 `INVALID_STATUS_TRANSITION`; phiếu giữ `CANCELLED`; nếu khách có nhu cầu mới thì tạo yêu cầu/phiếu mới |
| WO-04 | Technician bấm Hoàn thành nhưng bỏ trống Chẩn đoán hoặc Giải pháp | Form đánh dấu trường lỗi, cuộn tới lỗi đầu tiên và hiện cảnh báo rõ; không gọi API, không đổi trạng thái |
| WO-05 | Technician nhập đủ Chẩn đoán + Giải pháp rồi hoàn thành | WO chuyển COMPLETED; Tổng quan hiển thị kết quả vừa lưu; Tiến trình của lần hoàn thành giữ riêng Chẩn đoán, Giải pháp và Ghi chú bàn giao nếu có; người thao tác thấy phản hồi thành công và nút **Khách xác nhận** |
| USER-01 | Owner sửa user và thử đổi username | Bị chặn `USER_USERNAME_CHANGE_BLOCKED`; username lịch sử/ownership giữ ổn định |
| USER-02 | Owner đổi bộ lọc Người dùng giữa Tất cả trạng thái / Hoạt động / Tạm ngưng, đồng thời nhập từ khóa tìm kiếm | Danh sách áp đồng thời search + trạng thái; đổi filter/search quay về page 1; metric tổng vẫn phản ánh toàn bộ tenant |
| USER-03 | Owner tạo/cập nhật user | Success feedback ghi rõ tên, vai trò và trạng thái tài khoản; audit USER_ACCOUNT ghi trạng thái sau thao tác |
| OWNER-01 | Owner mở Service Request OPEN và bấm Chuyển sang điều phối | Cho phép như Customer Service; tạo đúng Work Order, không bypass guard customer/asset/state |
| OWNER-02 | Owner duyệt các workspace `/users`, Customer/Asset, Service Request/Channel, Work Order/Schedule/History, Technician, Inventory/Stocktake/Movements và Audit | Các trang quản trị của Owner truy cập được; My Schedule và Technician-only CONSUME vẫn không biến thành admin impersonation |
| DASH-01 | Có WO ở SCHEDULED/ON_THE_WAY/REOPENED/CUSTOMER_ACCEPTED rồi mở dashboard | Tỷ lệ hoàn tất tính đủ các trạng thái active/completed chính, không bỏ sót các state này |
| INV-01 | Nhập kho số lượng dương | Tồn và ledger tăng đúng |
| INV-02 | Dùng phụ tùng đủ tồn | Tồn giảm, ledger gắn work order |
| INV-02A | Technician bấm Dùng phụ tùng trong Work Order rồi mở tab Tiến trình | Thấy activity **Đã sử dụng phụ tùng** đúng tên/SKU, số lượng, người thao tác là Technician, thời gian và ghi chú; không cần chờ tới lúc xuất hóa đơn |
| INV-02B | Warehouse hoàn trả một phần phụ tùng của Work Order | Không thêm activity Warehouse vào tab Tiến trình operational của Work Order; RETURN xuất hiện ở Lịch sử biến động và invoice vẫn tính net `CONSUME - RETURN` |
| INV-03 | Dùng vượt tồn | HTTP 409, tồn không thay đổi |
| INV-03A | Technician thử Dùng phụ tùng khi WO đã `COMPLETED` hoặc `CUSTOMER_ACCEPTED` | UI không hiện nút Dùng phụ tùng; gọi API trực tiếp nhận HTTP 409 `WORK_ORDER_PART_CONSUMPTION_NOT_ALLOWED`; tồn và ledger không thay đổi |
| INV-04 | Warehouse kiểm kê part: system 10, actual 8 | Tồn thành 8; tạo `ADJUSTMENT_OUT 2` với reason/actor và `balanceAfter=8`; Owner nhận notification chênh lệch sau commit; nếu tồn `<= reorderLevel` (ngưỡng tồn tối thiểu), Warehouse nhận cảnh báo tồn thấp |
| INV-05 | Warehouse hoàn 1 part sau khi WO đã CONSUME 3 | Tồn tăng 1; ledger tạo `RETURN`; returnable còn 2; thử hoàn thêm >2 nhận HTTP 409 |
| INV-06 | Warehouse mở Lịch sử biến động và filter SKU/WO/type/date | Thấy IMPORT/CONSUME/RETURN/ADJUSTMENT đúng thứ tự, actor và tồn sau giao dịch; không cần quyền đọc operational Work Order |
| INV-07 | Đóng Work Order sau CONSUME 3 và RETURN 1 rồi xuất invoice | Invoice chỉ tính net 2 đơn vị, không tính phần đã hoàn lại |
| INV-08 | Warehouse/Owner sửa ngưỡng tồn tối thiểu từ 3 lên 6 khi stock hiện tại = 5 | Stock vẫn = 5; `reorderLevel=6`; không tạo inventory transaction; có audit `UPDATE_REORDER_LEVEL`; vì trạng thái chuyển từ bình thường sang tồn thấp nên OWNER/WAREHOUSE_STAFF khác người thao tác nhận notification sau commit |
| FILE-01 | Upload JPG/PNG/WEBP/PDF dưới 10 MB | File lưu và tải lại được |
| FILE-02 | Upload loại file không cho phép | HTTP 400 INVALID_FILE_TYPE |
| FILE-03 | Asset chưa có SR/WO nhưng còn attachment rồi thử hard-delete Asset | HTTP 409 `ASSET_HAS_ATTACHMENTS`; metadata/file không orphan |
| FILE-04 | Service Request chưa convert nhưng còn attachment rồi thử hard-delete | HTTP 409 `SERVICE_REQUEST_HAS_ATTACHMENTS`; metadata/file không orphan |
| AUD-01 | Thực hiện thao tác quan trọng | Có audit log tương ứng |
| AUD-02 | Mở Nhật ký hệ thống | Mặc định 30 ngày gần nhất, 20 dòng/trang, mới nhất trước |
| AUD-03 | Lọc theo ngày / người thao tác / hành động / đối tượng | Backend chỉ trả dữ liệu phù hợp và tổng số bản ghi đúng |
| AUD-04 | Tìm theo nội dung hoặc mã nghiệp vụ có trong chi tiết audit | Kết quả phù hợp, đổi bộ lọc quay về trang đầu |
| SEARCH-01 | Tìm ở Khách hàng / Thiết bị / Yêu cầu / Phiếu / Lịch sử / Kho | Chờ debounce ngắn, backend trả đúng kết quả và tổng số bản ghi; không tải toàn bộ danh sách về browser |
| SEARCH-02 | Từ trang > 1 rồi đổi từ khóa hoặc trạng thái | Tự quay về trang 1; không xuất hiện trang rỗng giả do page cũ vượt tổng trang mới |
| SEARCH-03 | Duyệt danh sách có hơn 20 bản ghi | Chỉ hiển thị 20 dòng/trang, Next/Previous lấy đúng page từ backend, tổng số bản ghi giữ chính xác |
| SEARCH-04 | Tìm theo trường mở rộng | Customer tìm được bằng email; Asset bằng loại/mã khách; Service Request bằng mô tả; Work Order bằng kỹ thuật viên; Spare Part bằng đơn vị |
| SEARCH-05 | Kết hợp keyword + status trên Yêu cầu/Phiếu/Lịch sử | Backend áp dụng đồng thời hai điều kiện, pagination và tổng số bản ghi đúng |
| SEARCH-06 | Làm API danh sách lỗi hoặc backend tạm dừng | UI hiển thị lỗi + nút Thử lại; không hiển thị nhầm thành danh sách rỗng hợp lệ |
| SEARCH-06A | Làm lỗi API dữ liệu phụ trợ (chi tiết WO, kỹ thuật viên, phụ tùng, khách hàng/kênh/thiết bị hoặc schedule board) | UI hiển thị lỗi có hướng dẫn/Thử lại thay vì render select hoặc drawer rỗng gây hiểu nhầm |
| SEARCH-07 | Tìm ở Người dùng / Kỹ thuật viên / Kênh tiếp nhận | Lọc tức thời trên tập dữ liệu nhỏ đã tải, page size 20 và lỗi API có trạng thái Thử lại nhất quán |
| WO-ROLE-01 | Owner mở Work Order `COMPLETED` bằng icon mắt | Có nút **Khách xác nhận** cạnh **Tải ảnh / PDF**; không có nút Dùng phụ tùng |
| WO-ROLE-02 | Assigned Technician mở Work Order `COMPLETED` bằng icon mắt | Có nút **Khách xác nhận** cạnh **Tải ảnh / PDF**; bấm thành công → `CUSTOMER_ACCEPTED` |
| WO-ROLE-03 | Technician hoặc Owner mở Work Order `CUSTOMER_ACCEPTED` | Có nút **Đóng phiếu**; bấm → `CLOSED`, tự chuyển sang **Lịch sử phiếu** và mở đúng phiếu vừa đóng; nếu cùng lỗi còn tồn tại trước khi đóng thì dùng `REOPENED` |
| WO-ROLE-04 | Mở bộ lọc trạng thái ở trang Phiếu công việc | Không có `Đã đóng/Đã hủy`; hai trạng thái terminal chỉ tra cứu ở **Lịch sử phiếu** |
| WO-NOTIF-01 | Technician hoàn thành Work Order | Customer Service nhận **Cần theo dõi khách sau sửa chữa: WO-...**; body có tên Technician + summary + khách hàng + bước follow-up; Owner không nhận completion bình thường |
| WO-NOTIF-02 | Work Order sang `WAITING_FOR_PARTS` | Dispatcher nhận **Phiếu đang chờ phụ tùng: WO-...** và hướng dẫn phối hợp với kho |
| WO-NOTIF-03 | Work Order `CLOSED` | Owner không nhận chuông cho closure bình thường; assigned Technician vẫn nhận khi người khác (ví dụ Owner) đóng phiếu thay |
| WO-NOTIF-03A | Work Order `REOPENED` bởi role khác CSKH | Owner + Dispatcher (trừ actor) nhận **Phiếu cần xử lý lại: WO-...**; assigned Technician nhận **Công việc cần xử lý lại: WO-...** nếu không phải actor; Customer Service nhận **Phiếu cần theo dõi lại: WO-...**; body có actor + khách hàng + lý do |
| WO-NOTIF-03B | Work Order `CANCELLED` bởi Owner/Dispatcher/Technician | Owner (trừ actor) nhận ngoại lệ; assigned Technician nhận thông báo dừng công việc nếu không phải actor; Customer Service nhận **Phiếu đã hủy, cần cập nhật khách hàng: WO-...** |
| WO-NOTIF-03C | Work Order `REOPENED`/`CANCELLED` do chính Customer Service thao tác | Không broadcast lại cho nhóm Customer Service; các role vận hành liên quan vẫn nhận theo policy |
| WO-NOTIF-03D | Appointment vừa quá hạn nhưng chưa qua grace period | Dispatcher + assigned Technician nhận overdue alert; Customer Service chưa nhận để tránh spam |
| WO-NOTIF-03E | Appointment vẫn `SCHEDULED/ASSIGNED` sau `endTime + 15 phút` | Customer Service nhận đúng một **Khách hàng có thể cần được liên hệ: WO-...**; các lần scan sau không tạo duplicate |
| WO-TIMELINE-IDENTITY-01 | Mở Tiến trình của WO có thao tác từ CSKH/Dispatcher/Technician/Owner | Actor nằm cùng dòng tiêu đề activity theo dạng `Trạng thái · Tên vai trò · Họ tên`, ví dụ **Đang mở · Chăm sóc khách hàng · Trần Mai CSKH**; dữ liệu legacy được V9 backfill khi còn đối chiếu được username |
| WO-REOPEN-COMPLETION-01 | Hoàn thành WO với kết quả A → Mở lại → vào xử lý và mở form Hoàn thành lần nữa | Form tự điền Chẩn đoán + Giải pháp gần nhất; Ghi chú bàn giao không copy sang lần mới; Technician có thể giữ nguyên hoặc sửa trước khi hoàn thành |
| WO-REOPEN-COMPLETION-02 | Sau reopen, Technician hoàn thành lần 2 với kết quả B | Tổng quan hiển thị B; Tiến trình vẫn giữ entry COMPLETED lần 1 với A và tạo entry COMPLETED lần 2 với B; mỗi entry giữ Chẩn đoán, Giải pháp, Ghi chú bàn giao tương ứng và actor riêng |
| NOTIF-SPAM-01 | Customer/Asset/Service Request/Channel, Technician profile, attachment, tạo/import catalog hoặc nhập kho thay đổi bình thường | Không role nào nhận bell notification cho CRUD routine; actor thấy success/error tại màn hình và thay đổi vẫn có Audit/workspace để truy vết |
| NOTIF-OWNER-02 | Technician consume làm stock từ trên ngưỡng xuống chạm/thấp hơn `reorderLevel`, sau đó consume tiếp khi stock vẫn thấp | Owner/Warehouse nhận cảnh báo ở lần **cross threshold**; lần consume tiếp theo không tạo low-stock notification lặp lại |
| WO-NOTIF-04 | Mở notification cũ có title dạng `Cập nhật WO-...: ON_THE_WAY → CANCELLED` | UI hiển thị title + mô tả tiếng Việt thân thiện, không lộ enum nội bộ |
| WO-NOTIF-05 | Mở actionable notification cũ `Công việc mới: WO-...` có message là summary/test text khó hiểu | UI compatibility hiển thị **Bạn có công việc mới: WO-...** và hướng dẫn mở **Lịch của tôi**; không lộ raw summary/test text |
| NOTIF-COPY-01 | Tạo SR thực tế rồi chuyển thành WO và phân công Technician | Dispatcher/Technician notification có `WO-...`, summary, tên khách, actor phù hợp và next action; đọc riêng một dòng vẫn hiểu đang nói tới ai/việc gì |
| NOTIF-REASON-01 | Reopen hoặc cancel Work Order với lý do nghiệp vụ | Notification của recipient liên quan có actor + khách hàng + **Lý do**; không hiển thị enum/raw audit transition |
| NOTIF-LEGACY-01 | Khởi động backend trên database cũ có notification Customer/Asset/SR/Channel/Attachment/import/generic `Cập nhật WO...` | Flyway V7 loại các bell row obsolete; badge/unread count giảm tương ứng; Audit/Timeline vẫn còn nguồn truy vết |
| NOTIF-01 | Mark Read/Unread một notification khi API thành công | Dòng và badge/tab Chưa đọc cập nhật ngay sau invalidate |
| NOTIF-02 | Làm API Mark Read/Unread lỗi | Hiện thông báo lỗi; nút không bị treo; trạng thái hiển thị không giả vờ đã đổi |
| AI-01 | Mỗi role hỏi “Trong vai trò này tôi được làm những gì?” | AI trả overview đúng role lấy từ backend/JWT; OWNER thấy phạm vi quản trị rộng, các role khác chỉ thấy chức năng được giao |
| AI-02 | Warehouse hỏi về kiểm kê, lịch sử biến động và hoàn trả phụ tùng | AI điều hướng lần lượt tới `/inventory-stocktake` hoặc `/inventory-movements`; không gợi ý operational dashboard/Work Order route |
| AI-03 | Dispatcher hỏi quản trị user/kho hoặc Technician hỏi sửa ngưỡng/kiểm kê | AI từ chối là ngoài phạm vi thay vì hướng dẫn thao tác của role khác |
| AI-04 | Dispatcher hỏi điều phối lại kỹ thuật viên trước khi bắt đầu | AI hướng dẫn reason + notification/timeline và nêu rõ không reschedule khi WO đã `ON_THE_WAY`/`IN_PROGRESS` |
| AI-05 | Customer Service hỏi chung về hậu xử lý | AI hướng dẫn intake/convert/reopen/cancel theo role, không khẳng định CS được Khách xác nhận/Đóng phiếu |
| BUILD-01 | Chạy backend test | Build success |
| BUILD-02 | Chạy frontend lint/build | Build success |
