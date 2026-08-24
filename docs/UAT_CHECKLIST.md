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
| DEMO-01 | Owner/Dispatcher mở Kỹ thuật viên ở public demo | Seeded technician hiển thị “Demo cố định” và nút sửa bị khóa; user tự tạo vẫn chỉnh được |
| TENANT-01 | Truy cập ID không thuộc tenant | Không trả dữ liệu |
| CUS-01 | Tạo khách hàng hợp lệ | Khách hàng xuất hiện trong danh sách |
| AST-01 | Tạo thiết bị với serial mới | Thiết bị liên kết đúng khách hàng |
| AST-02 | Tạo trùng serial trong tenant | Bị từ chối HTTP 409 |
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
| USER-01 | Owner sửa user và thử đổi username | Bị chặn `USER_USERNAME_CHANGE_BLOCKED`; username lịch sử/ownership giữ ổn định |
| DASH-01 | Có WO ở SCHEDULED/ON_THE_WAY/REOPENED/CUSTOMER_ACCEPTED rồi mở dashboard | Tỷ lệ hoàn tất tính đủ các trạng thái active/completed chính, không bỏ sót các state này |
| INV-01 | Nhập kho số lượng dương | Tồn và ledger tăng đúng |
| INV-02 | Dùng phụ tùng đủ tồn | Tồn giảm, ledger gắn work order |
| INV-03 | Dùng vượt tồn | HTTP 409, tồn không thay đổi |
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
| SEARCH-07 | Tìm ở Người dùng / Kỹ thuật viên / Kênh tiếp nhận | Lọc tức thời trên tập dữ liệu nhỏ đã tải, page size 20 và lỗi API có trạng thái Thử lại nhất quán |
| AI-01 | Technician/Warehouse mở Trợ lý AI và gửi câu hỏi hướng dẫn | Nhận hướng dẫn phù hợp role, không bị HTTP 403 |
| BUILD-01 | Chạy backend test | Build success |
| BUILD-02 | Chạy frontend lint/build | Build success |
