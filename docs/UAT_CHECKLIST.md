# UAT Checklist

Checklist này dùng để kiểm tra nhanh trước khi release hoặc sau khi deploy. Nó không thay thế automated test.

## 1. Đăng nhập và phân quyền

- [ ] Đăng nhập được bằng tài khoản hợp lệ.
- [ ] Tài khoản đã bị khóa không thể tiếp tục dùng JWT cũ.
- [ ] `OWNER` xem được khu vực quản trị và Audit Log.
- [ ] `CUSTOMER_SERVICE` xem được Khách hàng, Thiết bị, Yêu cầu dịch vụ và Thanh toán.
- [ ] `DISPATCHER` xem được Phiếu công việc, Lịch và Kỹ thuật viên.
- [ ] `TECHNICIAN` chỉ xem/thao tác công việc được giao cho mình.
- [ ] `WAREHOUSE_STAFF` xem được Yêu cầu phụ tùng và các màn quản lý kho.
- [ ] Mở URL không đúng quyền bị backend từ chối, không chỉ bị frontend ẩn menu.

## 2. Khách hàng, thiết bị và Yêu cầu dịch vụ

- [ ] Tạo Khách hàng mới; mã dạng `KH-YYYYMMDD-NNN` được hệ thống tự sinh.
- [ ] Tạo Thiết bị đúng Khách hàng.
- [ ] Không tạo Thiết bị mới cho Khách hàng đã inactive.
- [ ] Tạo Yêu cầu dịch vụ mới ở trạng thái `OPEN`.
- [ ] Nếu dùng AI gợi ý, AI chỉ thay tiêu đề/mô tả; không tự đổi mức ưu tiên hoặc kênh tiếp nhận.
- [ ] Chuyển Yêu cầu dịch vụ thành đúng một Phiếu công việc; Yêu cầu chuyển sang `CONVERTED`.
- [ ] Hủy Yêu cầu dịch vụ vẫn giữ record lịch sử.

## 3. Phân công và lịch làm việc

- [ ] Phiếu công việc có mã `WO-YYYYMMDD-NNN`.
- [ ] Điều phối viên phân công được kỹ thuật viên và lịch hợp lệ.
- [ ] Không thể xếp hai lịch bị trùng cho cùng một kỹ thuật viên.
- [ ] Đổi lịch/đổi người hiển thị đúng thông tin mới.
- [ ] Khi kỹ thuật viên đã bắt đầu làm việc, các thao tác điều phối bị giới hạn đúng quy tắc.
- [ ] Kỹ thuật viên khác không thao tác được Phiếu công việc không thuộc mình.

## 4. Kỹ thuật viên và phụ tùng

- [ ] Kỹ thuật viên chuyển được `ON_THE_WAY → IN_PROGRESS` khi đúng điều kiện.
- [ ] Tạo yêu cầu phụ tùng không làm giảm tồn kho.
- [ ] Kho `ISSUE` làm giảm đúng số lượng tồn kho một lần.
- [ ] Không thể `ISSUE` vượt số lượng tồn hoặc cấp trùng.
- [ ] Kỹ thuật viên ghi `USED` không làm giảm tồn kho lần thứ hai.
- [ ] `RETURN` phần còn dư làm tăng tồn kho và không vượt số lượng có thể trả.
- [ ] Thông báo tồn kho thấp chỉ gửi khi đúng điều kiện, không lặp vô nghĩa.

## 5. Hoàn thành công việc và khách hàng xác nhận

- [ ] Kỹ thuật viên nhập chẩn đoán/kết quả và hoàn thành Phiếu công việc.
- [ ] Chi phí hiển thị đúng công, phụ tùng và các khoản phí hiện có.
- [ ] Khách hàng xác nhận làm Phiếu công việc chuyển sang `CUSTOMER_ACCEPTED`.
- [ ] Hệ thống lưu bản chốt chi phí tại thời điểm xác nhận.
- [ ] Thay đổi giá danh mục hoặc trả phụ tùng sau đó không làm thay đổi số tiền đã chốt.
- [ ] Nếu mở lại Phiếu công việc, lịch sử lần xử lý trước vẫn còn.

## 6. Thanh toán, biên nhận và đóng Phiếu công việc

Nên kiểm tra ít nhất một cách thanh toán trong smoke test. Trước release lớn nên kiểm cả ba.

### Chuyển khoản

- [ ] Ghi nhận chuyển khoản → `TRANSFER_PENDING_VERIFICATION`.
- [ ] Chăm sóc khách hàng xác nhận → `SETTLED`.

### Tiền mặt

- [ ] Kỹ thuật viên ghi nhận đã nhận tiền → `CASH_PENDING_HANDOVER`.
- [ ] Chăm sóc khách hàng xác nhận đã nhận tiền → `SETTLED`.

### Thanh toán tại quầy

- [ ] Ghi nhận thanh toán tại quầy → `COUNTER_PAYMENT_PENDING`.
- [ ] Chăm sóc khách hàng xác nhận đã thu tiền → `SETTLED`.

Sau khi thanh toán hoàn tất:

- [ ] Chỉ phát hành biên nhận khi payment là `SETTLED`.
- [ ] Biên nhận có mã `BN-YYYYMMDD-NNN`.
- [ ] Gửi lại cùng thao tác không tạo thêm biên nhận hoặc thanh toán trùng.
- [ ] Chỉ đóng Phiếu công việc khi trạng thái là `CUSTOMER_ACCEPTED` và payment là `SETTLED`.
- [ ] Phiếu công việc chuyển sang `CLOSED`.

## 7. Danh mục và tồn kho

- [ ] Tạo Phụ tùng mới có mã `PT-YYYYMMDD-NNN`.
- [ ] Người dùng không nhập mã nghiệp vụ bằng tay.
- [ ] Import CSV báo rõ dòng lỗi và không lưu dữ liệu nửa chừng ngoài hành vi đã thiết kế.
- [ ] Active/inactive hoạt động đúng và không làm mất lịch sử.
- [ ] Kiểm kê tạo đúng lịch sử biến động và Audit Log khi có chênh lệch.

## 8. Lịch sử, Audit Log và thông báo

- [ ] Màn lịch sử đếm đúng `CUSTOMER_ACCEPTED`, `CLOSED`, `CANCELLED` theo bộ lọc.
- [ ] Timeline hiển thị đúng người thao tác, trạng thái và ghi chú.
- [ ] Audit Log ưu tiên hiển thị tên/mã nghiệp vụ thay vì chỉ UUID hoặc enum.
- [ ] File đính kèm hiển thị mục đích bằng nội dung dễ hiểu.
- [ ] Thông báo đến đúng người/đúng vai trò.
- [ ] Trạng thái đã đọc/chưa đọc cập nhật đúng.
- [ ] Các thao tác CRUD thông thường không tạo quá nhiều thông báo không cần thiết.

## 9. AI Help

- [ ] Câu hỏi thuộc phạm vi hệ thống trả hướng dẫn phù hợp với vai trò đang đăng nhập.
- [ ] Câu hỏi ngoài phạm vi được từ chối hoặc hướng dẫn an toàn.
- [ ] AI không trả system prompt, API key, JWT, secret, biến môi trường hoặc dữ liệu live không có trong context.
- [ ] Khi Gemini không cấu hình hoặc bị lỗi, hướng dẫn nội bộ vẫn hoạt động.
- [ ] Giao diện cho biết câu trả lời đến từ Gemini hay nội bộ nếu chức năng hiện tại có hiển thị nguồn.

## 10. Lỗi và giao diện

- [ ] Form chỉ rõ trường nào nhập sai.
- [ ] Lỗi 403/409 hiển thị thông báo dễ hiểu, không lộ SQL hoặc stack trace.
- [ ] URL/API không tồn tại trả 404 phù hợp.
- [ ] Sau thao tác thành công/thất bại có thông báo rõ ràng.
- [ ] Trạng thái loading/empty/error không gây hiểu nhầm.
- [ ] Bảng dữ liệu không bị chồng chữ ở kích thước desktop thông thường.
- [ ] Search/filter/pagination vẫn đúng sau khi thêm hoặc cập nhật dữ liệu.

## 11. Kiểm tra sau deploy production

- [ ] Health endpoint trả trạng thái tốt.
- [ ] Frontend tải được qua HTTPS.
- [ ] Đăng nhập được bằng các vai trò cần kiểm tra.
- [ ] Tạo một Khách hàng/Yêu cầu dịch vụ/Phiếu công việc mới để kiểm tra database và mã nghiệp vụ.
- [ ] Chạy một luồng ngắn đến ít nhất `IN_PROGRESS` và kiểm tra Audit Log/Notification.
- [ ] Nếu release có thay kho hoặc thanh toán, chạy đầy đủ luồng đến `CLOSED`.
- [ ] Kiểm tra log không có lỗi migration, lỗi lặp liên tục hoặc secret bị ghi ra log.

Khi CI xanh và checklist cho phần chức năng thay đổi đã đạt, release có thể được xem là ổn định. Không tiếp tục sửa/refactor nếu không có bug hoặc yêu cầu mới.
