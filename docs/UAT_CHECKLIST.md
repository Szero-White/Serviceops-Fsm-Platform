# UAT Checklist — Local MVP

Dùng checklist này trước mỗi bản demo hoặc bàn giao thử nghiệm.

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| AUTH-01 | Đăng nhập đúng tài khoản demo | Nhận JWT và vào dashboard |
| AUTH-02 | Đăng nhập sai mật khẩu | HTTP 401, không lộ chi tiết nội bộ |
| AUTH-03 | Owner mở dữ liệu rồi logout/login ngay bằng Technician/Warehouse | Không render cache dữ liệu identity trước; TanStack Query cache được clear khi đổi identity |
| AUTH-04 | Owner tạm ngưng một user đang có JWT cũ rồi user gọi API lại | JWT cũ bị từ chối; account inactive không tiếp tục thao tác đến hết 480 phút local |
| RBAC-01 | Technician mở trang khách hàng | Bị từ chối HTTP 403 |
| RBAC-02 | Role truy cập trực tiếp URL không thuộc quyền trên frontend | Bị điều hướng về workspace mặc định của role; Warehouse về `/part-requests`, các operational role về dashboard; API backend vẫn là lớp bảo vệ cuối |
| RBAC-03 | Warehouse gọi trực tiếp API lịch sử phiếu | Bị từ chối HTTP 403 dù bỏ qua frontend |
| RBAC-04 | Warehouse gọi attachment của Asset/Service Request/Work Order | Bị từ chối HTTP 403; Warehouse chỉ sở hữu nghiệp vụ kho |
| RBAC-05 | Customer Service mở Kênh tiếp nhận | Xem được danh sách nhưng không thấy action thêm/sửa/xoá; cấu hình kênh là OWNER-only |
| RBAC-06 | Warehouse mở trực tiếp /work-orders hoặc gọi API Work Order/attachment | Frontend từ chối route; backend không cấp quyền Work Order cho Warehouse |
| RBAC-07 | Dispatcher gọi API Service Request hoặc Service Channel write | Bị từ chối HTTP 403; Dispatcher chỉ điều phối Work Order đã được handoff |
| RBAC-08 | Warehouse gọi `GET /api/v1/work-orders` hoặc `GET /api/v1/dashboard` | HTTP 403; Warehouse không đọc operational Work Order/dashboard data |
| RBAC-09 | Dispatcher gọi Work Order transition `ON_THE_WAY`, `COMPLETED`, `CUSTOMER_ACCEPTED`, `CLOSED` hoặc `REOPENED` | HTTP 403 `WORK_ORDER_TRANSITION_FORBIDDEN`; Dispatcher chỉ có operational cancellation trong transition endpoint |
| RBAC-10 | Dispatcher mở Đội ngũ kỹ thuật hoặc gọi `PUT /technicians/{id}` | Xem được danh sách; UI không có edit action và backend PUT trả 403 |
| RBAC-11 | Dispatcher thử xóa/ẩn Work Order khỏi history | UI không có action; backend DELETE trả 403; Owner-only archive |
| RBAC-12 | Owner mở Work Order `COMPLETED`/`CUSTOMER_ACCEPTED` | Chỉ giám sát; không có action khách xác nhận, settlement hoặc normal closure thay role phụ trách |
| RBAC-13 | Customer Service gọi generic transition `CUSTOMER_ACCEPTED`/`CLOSED` | HTTP 403 `WORK_ORDER_TRANSITION_FORBIDDEN`; acceptance và close dùng dedicated workflow. Sau `SETTLED`, CSKH dùng action `close` riêng |
| RBAC-14 | Dispatcher mở trực tiếp `/audit` hoặc gọi API audit | Frontend điều hướng về dashboard; backend trả 403. Dispatcher dùng Lịch sử phiếu/Timeline cho truy vết nghiệp vụ điều phối; Audit toàn hệ thống là Owner-only |
| RBAC-15 | Technician mở trực tiếp `/inventory` | Frontend điều hướng về dashboard; KTV vẫn tìm/request phụ tùng từ tab Phụ tùng của Work Order được giao |
| NAV-01 | Đăng nhập từng role và đọc sidebar | Section/item đúng workflow của role; không xuất hiện menu ngoài nhiệm vụ chính; Owner vẫn thấy đủ 4 nhóm Vận hành / Khách hàng & nguồn lực / Kho & vật tư / Quản trị |
| NAV-02 | Owner cuộn sidebar đến cuối ở màn hình thấp | Menu cuộn riêng; **Thiết lập thanh toán** không bị footer che; **Đăng xuất** luôn nằm riêng ở footer |
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
| OWNER-02 | Owner duyệt các workspace `/users`, Customer/Asset, Service Request/Channel, Work Order/Schedule/History, Technician, Inventory/Stocktake/Movements và Audit | Các trang quản trị của Owner truy cập được; My Schedule và Technician-only part request/actual usage vẫn không biến thành admin impersonation |
| DASH-01 | Có WO ở SCHEDULED/ON_THE_WAY/REOPENED/CUSTOMER_ACCEPTED rồi mở dashboard | Tỷ lệ hoàn tất tính đủ các trạng thái active/completed chính, không bỏ sót các state này |
| INV-01 | Nhập kho số lượng dương | Tồn và ledger tăng đúng |
| INV-02 | Technician tạo Yêu cầu phụ tùng trong Work Order được giao | Request ở `REQUESTED`; tồn kho và inventory ledger **không đổi** |
| INV-02A | Technician sửa quantity/note của request đang `REQUESTED` | Request cập nhật đúng; audit lưu thay đổi; không tạo request active thứ hai cho cùng WO + part |
| INV-02B | Technician hủy request đang chờ và nhập lý do thực tế | Request thành `CANCELLED`, không hard-delete, không stock movement |
| INV-02C | Warehouse mở `/part-requests` | Mặc định thấy hàng đợi `REQUESTED`; Owner chỉ xem, Warehouse có **Xác nhận cấp** / **Không thể cấp** |
| INV-03 | Warehouse xác nhận cấp khi đủ tồn | Request thành `ISSUED`; tồn giảm đúng một lần; ledger tạo `ISSUE`; snapshot người giao/nhận được lưu |
| INV-03A | Double click/retry cấp cùng request | Không double-decrement; request đã terminal không được issue lần hai |
| INV-03B | Warehouse chọn Không thể cấp và nhập lý do | Request thành `UNAVAILABLE`; tồn và ledger không đổi |
| INV-03C | Technician hoàn thành/cancel WO khi còn request `REQUESTED` | Request hết hiệu lực theo lifecycle; Warehouse queue không còn request rác |
| INV-04A | Sau ISSUE 3, Technician ghi actual used = 2 | `USED=2`; tồn kho không đổi thêm; outstanding = 1 |
| INV-04B | Technician cập nhật actual used khi WO `COMPLETED` | Cho phép; sau `CUSTOMER_ACCEPTED` thì bị khóa |
| INV-05 | Warehouse hoàn 1 part sau ISSUE 3 và USED 2 | Tồn tăng 1; ledger tạo `RETURN`; outstanding về 0; thử hoàn quá outstanding nhận HTTP 409 |
| INV-05A | Warehouse hoàn outstanding sau khi WO đã `CLOSED` | RETURN thành công, stock tăng, WO vẫn `CLOSED`, không reopen |
| INV-06 | Warehouse mở Lịch sử biến động và filter SKU/WO/type/date | Thấy IMPORT/ISSUE/RETURN/CONSUME legacy/ADJUSTMENT đúng thứ tự, actor và tồn sau giao dịch |
| INV-07 | Warehouse kiểm kê part: system 10, actual 8 | Tồn thành 8; tạo `ADJUSTMENT_OUT 2` với reason/actor và `balanceAfter=8`; Owner nhận notification chênh lệch sau commit; nếu tồn `<= reorderLevel` (ngưỡng tồn tối thiểu), Warehouse nhận cảnh báo tồn thấp |
| INV-08 | Warehouse/Owner sửa ngưỡng tồn tối thiểu từ 3 lên 6 khi stock hiện tại = 5 | Stock vẫn = 5; `reorderLevel=6`; không tạo inventory transaction; có audit `UPDATE_REORDER_LEVEL`; vì trạng thái chuyển từ bình thường sang tồn thấp nên WAREHOUSE_STAFF khác người thao tác nhận notification sau commit; OWNER không nhận low-stock vận hành |
| FILE-01 | Upload JPG/PNG/WEBP/PDF dưới 10 MB | File lưu và tải lại được |
| FILE-02 | Upload loại file không cho phép | HTTP 400 INVALID_FILE_TYPE |
| FILE-03 | Asset chưa có SR/WO nhưng còn attachment rồi thử hard-delete Asset | HTTP 409 `ASSET_HAS_ATTACHMENTS`; metadata/file không orphan |
| FILE-04 | Service Request chưa convert nhưng còn attachment rồi thử hard-delete | HTTP 409 `SERVICE_REQUEST_HAS_ATTACHMENTS`; metadata/file không orphan |
| FILE-05 | Assigned Technician upload nhiều ảnh/PDF khi Work Order đang active | Tab **Hình ảnh & tài liệu** hiển thị đủ file; uploader có thể xem/tải/đổi tên/xóa file của mình |
| FILE-06 | Work Order chuyển `CUSTOMER_ACCEPTED`, sau đó thử rename/delete/upload work evidence | Bị chặn; hồ sơ sửa chữa chỉ còn read-only |
| FILE-07 | Technician tải ảnh payment evidence nhưng chưa báo chuyển khoản | Có thể chụp lại/bỏ ảnh nháp; ảnh không xuất hiện lẫn trong tab **Hình ảnh & tài liệu** |
| FILE-08 | Technician báo chuyển khoản với payment evidence rồi thử rename/delete attachment đó | Payment → `TRANSFER_PENDING_VERIFICATION`; evidence bị lock và API chặn rename/delete |
| AUD-01 | Thực hiện thao tác quan trọng | Có audit log tương ứng |
| AUD-02 | Owner mở Nhật ký hệ thống | Mặc định 30 ngày gần nhất, 20 dòng/trang, mới nhất trước; role khác không có quyền truy cập |
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
| WO-ROLE-01 | Owner mở Work Order `COMPLETED` | Chỉ giám sát dữ liệu; không có action Khách xác nhận/actual-used/payment settlement/đóng phiếu thay role phụ trách |
| WO-ROLE-02 | Assigned Technician mở Work Order `COMPLETED` bằng icon mắt | Có nút **Khách xác nhận** cạnh **Tải ảnh / PDF**; bấm thành công → `CUSTOMER_ACCEPTED` |
| WO-ROLE-03 | CSKH mở Work Order `CUSTOMER_ACCEPTED` | Nếu payment chưa `SETTLED` thì đóng phiếu bị chặn; sau `SETTLED` có thể phát hành biên nhận và **Đóng phiếu** → `CLOSED`, tự chuyển sang Lịch sử phiếu |
| WO-ROLE-04 | Mở bộ lọc trạng thái ở trang Phiếu công việc | Không có `Đã đóng/Đã hủy`; hai trạng thái terminal chỉ tra cứu ở **Lịch sử phiếu** |
| WO-NOTIF-01 | Technician hoàn thành Work Order | Customer Service nhận **Cần theo dõi khách sau sửa chữa: WO-...**; body có tên Technician + summary + khách hàng + bước follow-up; Owner không nhận completion bình thường |
| WO-NOTIF-02 | Work Order sang `WAITING_FOR_PARTS` | Dispatcher nhận **Phiếu đang chờ phụ tùng: WO-...** và hướng dẫn phối hợp với kho |
| PART-NOTIF-01 | Assigned Technician tạo `REQUEST` phụ tùng | Warehouse nhận **Có yêu cầu phụ tùng mới: WO-...** với Technician + SKU/tên part + quantity và hướng dẫn mở **Yêu cầu phụ tùng**; Owner không nhận routine part-request bell |
| WO-NOTIF-03 | Work Order `CLOSED` | Owner (trừ actor) nhận đúng 1 terminal summary **Phiếu đã hoàn tất: WO-...**; assigned Technician vẫn nhận khi người khác đóng phiếu thay |
| WO-NOTIF-03A | Work Order `REOPENED` bởi role khác CSKH | Dispatcher (trừ actor) nhận **Phiếu cần xử lý lại: WO-...**; assigned Technician nhận **Công việc cần xử lý lại: WO-...** nếu không phải actor; Customer Service nhận **Phiếu cần theo dõi lại: WO-...**; OWNER không nhận; body có actor + khách hàng + lý do |
| WO-NOTIF-03B | Work Order `CANCELLED` bởi Owner/Dispatcher/Technician | Owner (trừ actor) nhận 1 terminal summary; assigned Technician nhận thông báo dừng công việc nếu không phải actor; Customer Service nhận **Phiếu đã hủy, cần cập nhật khách hàng: WO-...** |
| WO-NOTIF-03C | Work Order `REOPENED` | Dispatcher/assigned Technician/Customer Service nhận theo routing hiện hành; OWNER không nhận operational reopen alert |
| WO-NOTIF-03D | Work Order `CLOSED` bởi role khác OWNER | OWNER nhận **Phiếu đã hoàn tất: WO-...** đúng 1 terminal summary; không cần nhận `COMPLETED` trước đó |
| WO-NOTIF-03E | Work Order `REOPENED`/`CANCELLED` do chính Customer Service thao tác | Không broadcast lại cho nhóm Customer Service; các role vận hành liên quan vẫn nhận theo policy |
| WO-NOTIF-03F | Appointment vừa quá hạn nhưng chưa qua grace period | Dispatcher + assigned Technician nhận overdue alert; Customer Service chưa nhận để tránh spam |
| WO-NOTIF-03G | Appointment vẫn `SCHEDULED/ASSIGNED` sau `endTime + 15 phút` | Customer Service nhận đúng một **Khách hàng có thể cần được liên hệ: WO-...**; các lần scan sau không tạo duplicate |
| WO-TIMELINE-IDENTITY-01 | Mở Tiến trình của WO có thao tác từ CSKH/Dispatcher/Technician/Owner | Actor nằm cùng dòng tiêu đề activity theo dạng `Trạng thái · Tên vai trò · Họ tên`, ví dụ **Đang mở · Chăm sóc khách hàng · Trần Mai CSKH**; dữ liệu legacy được V9 backfill khi còn đối chiếu được username |
| WO-REOPEN-COMPLETION-01 | Hoàn thành WO với kết quả A → Mở lại → vào xử lý và mở form Hoàn thành lần nữa | Form tự điền Chẩn đoán + Giải pháp gần nhất; Ghi chú bàn giao không copy sang lần mới; Technician có thể giữ nguyên hoặc sửa trước khi hoàn thành |
| WO-REOPEN-COMPLETION-02 | Sau reopen, Technician hoàn thành lần 2 với kết quả B | Tổng quan hiển thị B; Tiến trình vẫn giữ entry COMPLETED lần 1 với A và tạo entry COMPLETED lần 2 với B; mỗi entry giữ Chẩn đoán, Giải pháp, Ghi chú bàn giao tương ứng và actor riêng |
| NOTIF-SPAM-01 | Customer/Asset/Service Request/Channel, Technician profile, attachment, tạo/import catalog hoặc nhập kho thay đổi bình thường | Không role nào nhận bell notification cho CRUD routine; actor thấy success/error tại màn hình và thay đổi vẫn có Audit/workspace để truy vết |
| NOTIF-OWNER-02 | Warehouse ISSUE làm stock từ trên ngưỡng xuống chạm/thấp hơn `reorderLevel`, sau đó ISSUE part khác/cùng part khi stock vẫn thấp | Warehouse nhận cảnh báo ở lần **cross threshold**; OWNER không nhận; khi part đã low-stock thì không tạo low-stock notification lặp lại |
| WO-NOTIF-04 | Mở notification cũ có title dạng `Cập nhật WO-...: ON_THE_WAY → CANCELLED` | UI hiển thị title + mô tả tiếng Việt thân thiện, không lộ enum nội bộ |
| WO-NOTIF-05 | Mở actionable notification cũ `Công việc mới: WO-...` có message là summary/test text khó hiểu | UI compatibility hiển thị **Bạn có công việc mới: WO-...** và hướng dẫn mở **Lịch của tôi**; không lộ raw summary/test text |
| NOTIF-COPY-01 | Tạo SR thực tế rồi chuyển thành WO và phân công Technician | Dispatcher/Technician notification có `WO-...`, summary, tên khách, actor phù hợp và next action; đọc riêng một dòng vẫn hiểu đang nói tới ai/việc gì |
| NOTIF-REASON-01 | Reopen hoặc cancel Work Order với lý do nghiệp vụ | Notification của recipient liên quan có actor + khách hàng + **Lý do**; không hiển thị enum/raw audit transition |
| NOTIF-LEGACY-01 | Khởi động backend trên database cũ có notification Customer/Asset/SR/Channel/Attachment/import/generic `Cập nhật WO...` | Flyway V7 loại các bell row obsolete; badge/unread count giảm tương ứng; Audit/Timeline vẫn còn nguồn truy vết |
| NOTIF-01 | Mark Read/Unread một notification khi API thành công | Dòng và badge/tab Chưa đọc cập nhật ngay sau invalidate |
| NOTIF-02 | Làm API Mark Read/Unread lỗi | Hiện thông báo lỗi; nút không bị treo; trạng thái hiển thị không giả vờ đã đổi |
| AI-01 | Mỗi role hỏi “Trong vai trò này tôi được làm những gì?” | AI trả overview đúng role lấy từ backend/JWT; OWNER thấy phạm vi quản trị rộng, các role khác chỉ thấy chức năng được giao |
| AI-02 | Warehouse hỏi về yêu cầu phụ tùng, kiểm kê, lịch sử biến động và hoàn trả | AI đưa part request tới `/part-requests`, các câu kiểm kê/lịch sử tới `/inventory-stocktake` hoặc `/inventory-movements`; nêu rõ Warehouse không sửa requested quantity và không gợi ý operational dashboard |
| AI-03 | Dispatcher hỏi quản trị user/kho/audit hoặc Technician hỏi sửa ngưỡng/kiểm kê | AI từ chối là ngoài phạm vi thay vì hướng dẫn thao tác của role khác |
| AI-03A | Technician hỏi phụ tùng cho job được giao | AI hướng dẫn thao tác trong Work Order/tab Phụ tùng và không điều hướng sang `/inventory` |
| AI-03B | Mỗi role hỏi về thông báo của mình | AI chỉ mô tả attention queue của role hiện tại, không hướng dẫn action của role khác |
| AI-04 | Dispatcher hỏi điều phối lại kỹ thuật viên trước khi bắt đầu | AI hướng dẫn reason + notification/timeline và nêu rõ không reschedule khi WO đã `ON_THE_WAY`/`IN_PROGRESS` |
| AI-05 | Customer Service hỏi chung về hậu xử lý | AI hướng dẫn payment reconciliation → biên nhận → close; không khẳng định CS được ghi nhận khách xác nhận tại hiện trường |
| AI-06 | Owner hỏi cấu hình tài khoản/QR nhận tiền | AI điều hướng `/payment-settings`, nêu Owner cấu hình còn Technician chỉ xem read-only tại Work Order |
| AI-07 | Technician hỏi khách chuyển khoản/tiền mặt | AI giữ route Work Order được giao, hướng dẫn ghi nhận payment action nhưng không cho SETTLED/receipt/close |
| AI-08 | CSKH hỏi xem lại phiếu đã đóng và tiến trình thanh toán | AI điều hướng `/work-order-history` và mô tả timeline/history thay vì workflow cũ |

| PAY-01 | Technician ghi nhận khách chuyển khoản | Payment → `TRANSFER_PENDING_VERIFICATION`; ảnh giao dịch nếu có chỉ là evidence, chưa `SETTLED` |
| PAY-02 | Technician nhận tiền mặt | Payment → `CASH_PENDING_HANDOVER`; lưu KTV đang giữ tiền + thời gian |
| PAY-03 | CSKH mở **Xử lý thanh toán** với khoản transfer/cash pending | Cột Xử lý có **Đối soát thanh toán**; bấm mở đúng Work Order và tự focus tab **Thanh toán**, không settle trực tiếp từ bảng |
| PAY-04 | CSKH kiểm snapshot chi phí + payment evidence/cash handover rồi xác nhận | Payment → `SETTLED`; lưu actor/time; trong Work Order hiện **Phát hành / tải biên nhận** + **Đóng phiếu**; Owner không được settlement thay |
| PAY-05 | CSKH rời Work Order sau `SETTLED` nhưng chưa đóng | Quay lại **Xử lý thanh toán** vẫn thấy **Phát hành / tải biên nhận** + **Đóng phiếu** để tiếp tục hồ sơ |
| RECEIPT-01 | Thử phát hành biên nhận trước `SETTLED` | HTTP 409; không tạo receipt |
| RECEIPT-02 | CSKH phát hành biên nhận sau `SETTLED` | Tạo đúng 1 receipt snapshot; tải lại không tạo bản mới; Owner/CSKH tải được |
| RECEIPT-03 | Sau receipt, thay catalog price hoặc Warehouse RETURN vật tư dư | Biên nhận vẫn giữ quantity/unit price/total từ billing snapshot đã freeze |
| CLOSE-01 | CSKH đóng `CUSTOMER_ACCEPTED` khi payment pending | HTTP 409; WO vẫn `CUSTOMER_ACCEPTED` |
| CLOSE-02 | CSKH đóng sau `SETTLED` | Backend bảo đảm receipt đã phát hành/idempotent rồi WO → `CLOSED`; Owner nhận terminal summary; vật tư outstanding không chặn closure |
| TIMELINE-01 | Mở timeline của WO hoàn chỉnh | Theo thời gian thấy REQUEST → ISSUE → USED → COMPLETED → CUSTOMER_ACCEPTED → payment reported → SETTLED → receipt → CLOSED; RETURN sau CLOSED vẫn xuất hiện và WO không reopen |
| BUILD-01 | Chạy backend test | Build success |
| BUILD-02 | Chạy frontend lint/build | Build success |
