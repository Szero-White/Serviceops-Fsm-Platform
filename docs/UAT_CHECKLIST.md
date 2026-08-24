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
| SCH-08 | Reschedule một appointment đã tồn tại | Schedule/My Schedule cập nhật lịch mới; audit action là `RESCHEDULE`, không tạo timeline trạng thái `ASSIGNED → ASSIGNED` giả |
| WO-02 | Chuyển trạng thái hợp lệ | Timeline lưu người thao tác và thời gian |
| WO-03 | Nhảy trạng thái không hợp lệ | HTTP 409 INVALID_STATUS_TRANSITION |
| WO-03A | Customer Service thử `CANCELLED → REOPENED` | HTTP 409 `INVALID_STATUS_TRANSITION`; phiếu giữ `CANCELLED`; nếu khách có nhu cầu mới thì tạo yêu cầu/phiếu mới |
| WO-04 | Technician bấm Hoàn thành nhưng bỏ trống Chẩn đoán hoặc Giải pháp | Form đánh dấu trường lỗi, cuộn tới lỗi đầu tiên và hiện cảnh báo rõ; không gọi API, không đổi trạng thái |
| WO-05 | Technician nhập đủ Chẩn đoán + Giải pháp rồi hoàn thành | WO chuyển COMPLETED; người thao tác thấy phản hồi thành công và nút **Khách xác nhận**; Owner nhận notification **Chờ khách xác nhận: WO-...** như fallback |
| USER-01 | Owner sửa user và thử đổi username | Bị chặn `USER_USERNAME_CHANGE_BLOCKED`; username lịch sử/ownership giữ ổn định |
| DASH-01 | Có WO ở SCHEDULED/ON_THE_WAY/REOPENED/CUSTOMER_ACCEPTED rồi mở dashboard | Tỷ lệ hoàn tất tính đủ các trạng thái active/completed chính, không bỏ sót các state này |
| INV-01 | Nhập kho số lượng dương | Tồn và ledger tăng đúng |
| INV-02 | Dùng phụ tùng đủ tồn | Tồn giảm, ledger gắn work order |
| INV-02A | Technician bấm Dùng phụ tùng trong Work Order rồi mở tab Tiến trình | Thấy activity **Đã sử dụng phụ tùng** đúng tên/SKU, số lượng, người thao tác, thời gian và ghi chú; không cần chờ tới lúc xuất hóa đơn |
| INV-02B | Warehouse hoàn trả một phần phụ tùng của Work Order | Tiến trình của Work Order có thêm activity **Đã hoàn trả phụ tùng** đúng số lượng, người thao tác và thời gian; invoice vẫn tính net `CONSUME - RETURN` |
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
| WO-NOTIF-01 | Technician hoàn thành Work Order | Owner (trừ actor) nhận **Chờ khách xác nhận: WO-...**; Technician vừa hoàn thành dùng success feedback + action tại phiếu, không nhận chuông trùng lặp |
| WO-NOTIF-02 | Work Order sang `WAITING_FOR_PARTS` | Dispatcher nhận **Cần xử lý phụ tùng: WO-...** |
| WO-NOTIF-03 | Work Order `CLOSED` | Owner nhận **Phiếu đã đóng: WO-...** nếu không phải actor; assigned Technician cũng nhận khi Owner đóng |
| WO-NOTIF-04 | Mở notification cũ có title dạng `Cập nhật WO-...: ON_THE_WAY → CANCELLED` | UI hiển thị title + mô tả tiếng Việt thân thiện, không lộ enum nội bộ |
| WO-NOTIF-05 | Mở notification cũ `Công việc mới: WO-...` có message là summary/test text khó hiểu | UI hiển thị **Bạn được giao công việc mới: WO-...** và hướng dẫn mở phiếu; không dùng summary/test text làm nội dung chính |
| NOTIF-01 | Mark Read/Unread một notification khi API thành công | Dòng và badge/tab Chưa đọc cập nhật ngay sau invalidate |
| NOTIF-02 | Làm API Mark Read/Unread lỗi | Hiện thông báo lỗi; nút không bị treo; trạng thái hiển thị không giả vờ đã đổi |
| AI-01 | Technician/Warehouse mở Trợ lý AI và gửi câu hỏi hướng dẫn | Nhận hướng dẫn phù hợp role, không bị HTTP 403 |
| AI-02 | Warehouse hỏi về kiểm kê, lịch sử biến động và hoàn trả phụ tùng | AI điều hướng lần lượt tới `/inventory-stocktake` hoặc `/inventory-movements`; không gợi ý operational dashboard/Work Order route |
| BUILD-01 | Chạy backend test | Build success |
| BUILD-02 | Chạy frontend lint/build | Build success |
