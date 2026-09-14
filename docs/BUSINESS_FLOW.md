# Quy trình nghiệp vụ ServiceOps

Tài liệu này mô tả cách các bộ phận sử dụng ServiceOps và các quy tắc chính mà backend đang kiểm soát.

## 1. Vai trò trong hệ thống

### Chủ hệ thống (`OWNER`)

- Theo dõi Dashboard và hoạt động chung.
- Quản lý người dùng.
- Xem khách hàng, thiết bị, Yêu cầu dịch vụ, Phiếu công việc, kho và lịch sử.
- Xem Audit Log và cấu hình thanh toán.

`OWNER` có quyền quản trị rộng nhưng không thay kỹ thuật viên để ghi tiến độ công việc hoặc thay nhân viên chăm sóc khách hàng để xác nhận các bước thanh toán thường ngày.

### Chăm sóc khách hàng (`CUSTOMER_SERVICE`)

- Quản lý khách hàng và thiết bị.
- Tiếp nhận Yêu cầu dịch vụ.
- Chuyển Yêu cầu dịch vụ thành Phiếu công việc.
- Theo dõi công việc sau khi kỹ thuật viên hoàn thành.
- Xác nhận thanh toán, phát hành biên nhận và đóng hồ sơ khi đủ điều kiện.

### Điều phối viên (`DISPATCHER`)

- Xem Phiếu công việc và danh sách kỹ thuật viên.
- Phân công kỹ thuật viên.
- Sắp xếp hoặc thay đổi lịch làm việc.
- Theo dõi các công việc cần điều phối.

Điều phối viên không tiếp nhận Yêu cầu dịch vụ và không xác nhận thanh toán.

### Kỹ thuật viên (`TECHNICIAN`)

- Chỉ thao tác trên Phiếu công việc được giao cho mình.
- Xem lịch cá nhân.
- Cập nhật tiến độ.
- Yêu cầu phụ tùng.
- Ghi số lượng phụ tùng thực tế đã dùng.
- Ghi chẩn đoán và kết quả sửa chữa.
- Hoàn thành công việc và ghi nhận thông tin liên quan đến việc khách hàng xác nhận/thanh toán theo luồng được phép.

### Nhân viên kho (`WAREHOUSE_STAFF`)

- Xem và xử lý yêu cầu phụ tùng.
- Cấp phụ tùng.
- Nhận phụ tùng trả lại.
- Quản lý danh mục, tồn kho, kiểm kê và lịch sử biến động.

Nhân viên kho không xử lý các bước điều phối Phiếu công việc.

## 2. Từ Yêu cầu dịch vụ đến Phiếu công việc

Quy trình:

1. Nhân viên chăm sóc khách hàng chọn hoặc tạo Khách hàng.
2. Chọn hoặc tạo Thiết bị thuộc đúng khách hàng đó.
3. Tạo Yêu cầu dịch vụ với nội dung, mức ưu tiên và kênh tiếp nhận.
4. Yêu cầu mới có trạng thái `OPEN`.
5. Khi thông tin đã đầy đủ, nhân viên chăm sóc khách hàng thực hiện chuyển đổi.
6. Backend tạo Phiếu công việc và chuyển Yêu cầu dịch vụ sang `CONVERTED`.
7. Nếu không tiếp tục xử lý, Yêu cầu dịch vụ chuyển sang `CANCELLED` và vẫn được giữ trong lịch sử.

Yêu cầu dịch vụ không tự tạo Phiếu công việc. Backend cũng kiểm tra Thiết bị có thật sự thuộc Khách hàng đã chọn hay không.

Khách hàng không còn hoạt động vẫn được giữ để xem lịch sử, nhưng không được dùng để tạo Thiết bị hoặc Yêu cầu dịch vụ mới.

## 3. Phân công và thực hiện Phiếu công việc

Luồng trạng thái thường dùng:

```text
OPEN
 → SCHEDULED / ASSIGNED
 → ON_THE_WAY
 → IN_PROGRESS
 → WAITING_FOR_PARTS (nếu cần phụ tùng)
 → IN_PROGRESS
 → COMPLETED
 → CUSTOMER_ACCEPTED
 → CLOSED
```

`REOPENED` và `CANCELLED` dùng cho các trường hợp đặc biệt.

Quy tắc chính:

- Điều phối viên chọn kỹ thuật viên và thời gian thực hiện.
- Hệ thống không cho xếp hai lịch bị trùng cho cùng một kỹ thuật viên.
- Khi kỹ thuật viên đã bắt đầu làm việc, việc đổi người hoặc đổi lịch bị hạn chế theo trạng thái hiện tại.
- Kỹ thuật viên chỉ cập nhật các trạng thái thuộc quá trình thực hiện công việc của mình.
- Kỹ thuật viên khác không được thao tác Phiếu công việc không được giao cho họ.
- Việc mở lại hoặc hủy Phiếu công việc phải đúng vai trò và đúng điều kiện.
- Không đóng Phiếu công việc chỉ bằng cách đổi trạng thái trực tiếp; hệ thống kiểm tra thanh toán trước khi đóng.

## 4. Yêu cầu, cấp, sử dụng và trả phụ tùng

Quy trình:

```text
Kỹ thuật viên yêu cầu phụ tùng
        ↓
Kho cấp phụ tùng
        ↓
Kỹ thuật viên ghi số lượng đã dùng
        ↓
Kho nhận phần còn dư (nếu có)
```

Các trạng thái kỹ thuật tương ứng:

```text
REQUESTED → ISSUE → USED → RETURN
```

Quy tắc chính:

- `REQUESTED`: chỉ ghi nhu cầu, chưa làm thay đổi tồn kho.
- `ISSUE`: kho thực sự xuất hàng, tồn kho giảm tại bước này.
- `USED`: ghi số lượng đã dùng cho công việc, không trừ tồn kho lần thứ hai.
- `RETURN`: trả phần chưa dùng về kho, tồn kho tăng lại.
- Không được xuất nhiều hơn số lượng đang có trong kho.
- Không được trả nhiều hơn số lượng còn có thể trả.
- Danh mục phụ tùng dùng trạng thái active/inactive thay vì xóa dữ liệu đang cần cho lịch sử.

## 5. Hoàn thành công việc và khách hàng xác nhận

Trước khi chuyển sang `COMPLETED`, kỹ thuật viên phải nhập các thông tin kết quả mà form hiện tại yêu cầu, gồm chẩn đoán và cách xử lý.

Chi phí của Phiếu công việc được tính từ công lao động, phụ tùng và các khoản phí đã ghi nhận.

Khi khách hàng xác nhận hoàn thành:

1. Hệ thống kiểm tra chi phí hiện tại.
2. Lưu một bản chốt chi phí và các dòng chi tiết.
3. Chuyển Phiếu công việc sang `CUSTOMER_ACCEPTED`.
4. Những thay đổi giá hoặc việc trả phụ tùng về sau không làm thay đổi số tiền đã chốt.

Nếu Phiếu công việc được mở lại và sửa tiếp, lịch sử các lần xử lý trước vẫn được giữ.

## 6. Thanh toán, biên nhận và đóng hồ sơ

Hệ thống hỗ trợ ba cách thanh toán.

### Chuyển khoản

```text
TRANSFER_PENDING_VERIFICATION
 → Chăm sóc khách hàng xác nhận
 → SETTLED
```

### Tiền mặt do kỹ thuật viên nhận

```text
CASH_PENDING_HANDOVER
 → Chăm sóc khách hàng xác nhận đã nhận tiền
 → SETTLED
```

### Thanh toán tại quầy

```text
COUNTER_PAYMENT_PENDING
 → Chăm sóc khách hàng xác nhận đã thu tiền
 → SETTLED
```

Quy tắc chính:

- Chỉ tạo hoặc xác nhận thanh toán ở đúng bước của Phiếu công việc.
- Mỗi cách thanh toán phải đi qua đúng trạng thái chờ tương ứng.
- Chỉ phát hành biên nhận khi thanh toán đã ở trạng thái `SETTLED`.
- Chỉ đóng Phiếu công việc khi khách hàng đã xác nhận và thanh toán đã hoàn tất.
- Nếu cùng một request được gửi lại, hệ thống không được tạo thêm biên nhận hoặc xác nhận thanh toán trùng.

Mã biên nhận có dạng `BN-YYYYMMDD-NNN`.

## 7. Lịch sử và Audit Log

Hệ thống giữ lịch sử trạng thái và thao tác để có thể trả lời các câu hỏi như:

- Ai đã phân công kỹ thuật viên?
- Khi nào lịch làm việc được thay đổi?
- Ai đã cấp hoặc trả phụ tùng?
- Ai đã xác nhận thanh toán?
- Phiếu công việc đã đi qua những trạng thái nào?

Giao diện ưu tiên hiển thị tên, mã nghiệp vụ và nội dung dễ hiểu. UUID hoặc enum chỉ dùng khi cần cho mục đích kỹ thuật.

## 8. Thông báo

Hệ thống chỉ gửi thông báo khi có việc cần người khác chú ý hoặc xử lý, ví dụ:

- Điều phối viên nhận thông báo khi có công việc cần phân công hoặc có thay đổi cần xử lý.
- Kỹ thuật viên nhận thông báo khi được giao việc hoặc lịch thay đổi.
- Chăm sóc khách hàng nhận thông báo khi công việc đã hoàn thành hoặc có thanh toán cần xác nhận.
- Nhân viên kho nhận thông báo khi có yêu cầu phụ tùng hoặc tồn kho thấp.

Không phải mọi thao tác thêm/sửa dữ liệu đều tạo thông báo.

## 9. AI trong hệ thống

AI chỉ đóng vai trò hỗ trợ:

- Có thể gợi ý tiêu đề và mô tả cho Yêu cầu dịch vụ.
- Có thể hướng dẫn người dùng dựa trên vai trò đã đăng nhập.
- Không tự đổi mức ưu tiên hoặc kênh tiếp nhận.
- Không tự thay đổi dữ liệu nghiệp vụ.
- Không được bỏ qua phân quyền hoặc quy tắc trạng thái.
- Không được trả về secret, token hoặc cấu hình nội bộ.
- Nếu Gemini không hoạt động, hệ thống dùng phần hướng dẫn nội bộ.

## 10. Mã nghiệp vụ

Các mã sau do backend/database tự sinh; người dùng không nhập bằng tay:

- Khách hàng: `KH-YYYYMMDD-NNN`
- Phiếu công việc: `WO-YYYYMMDD-NNN`
- Phụ tùng: `PT-YYYYMMDD-NNN`
- Biên nhận: `BN-YYYYMMDD-NNN`

Mỗi tenant có bộ đếm riêng theo loại mã và ngày nghiệp vụ. Backend dùng counter trong PostgreSQL để tránh sinh trùng mã khi có nhiều request chạy cùng lúc.

Các giá trị như serial number, mã kênh tiếp nhận, tenant code, UUID và enum trạng thái không dùng quy tắc mã nghiệp vụ trên.
