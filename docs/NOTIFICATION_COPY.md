# Chuẩn nội dung thông báo

Thông báo trong ServiceOps là **hàng đợi việc cần chú ý theo vai trò**. Thông báo không thay thế Tiến trình của phiếu công việc, Lịch sử biến động kho hoặc Nhật ký hệ thống.

Mỗi thông báo hiển thị cho người dùng phải trả lời được ba câu hỏi:

1. Việc gì vừa xảy ra hoặc đang cần xử lý?
2. Liên quan tới phiếu, khách hàng hoặc phụ tùng nào?
3. Người nhận cần làm gì tiếp theo?

## 1. Quy tắc nội dung hiển thị

- Tiêu đề ngắn, bắt đầu bằng hành động hoặc tình trạng cần chú ý và kèm mã nghiệp vụ khi có, ví dụ: **Cần phân công kỹ thuật viên: WO-2026-001245**.
- Nội dung nêu người liên quan, khách hàng/công việc/phụ tùng và bước tiếp theo. Không lặp lại nguyên tiêu đề.
- Dùng đúng tên trên giao diện: **Phiếu công việc**, **Lịch điều phối**, **Lịch của tôi**, **Yêu cầu phụ tùng**, **Kho phụ tùng**, **Xử lý thanh toán**, **Lịch sử biến động**.
- Không hiển thị mã trạng thái nội bộ, tên lớp, đường dẫn kỹ thuật, mã định danh dài, thời gian máy hoặc thông báo lỗi dành cho lập trình viên.
- Mã nghiệp vụ mà người dùng thực sự tra cứu như `WO-...` hoặc mã phụ tùng được giữ lại vì có giá trị vận hành.
- Lý do mở lại, hủy hoặc điều chỉnh lịch được giữ lại khi cần để người nhận hiểu ngữ cảnh.
- Nội dung được cắt gọn theo giới hạn lưu trữ; không biến thông báo thành bản ghi nhật ký chi tiết.

## 2. Nội dung chuẩn theo vai trò

| Tình huống nghiệp vụ | Người nhận | Tiêu đề hiển thị | Bước tiếp theo |
|---|---|---|---|
| Yêu cầu dịch vụ đã được chuyển sang phiếu công việc | Điều phối viên | **Cần phân công kỹ thuật viên: WO-...** | Mở Lịch điều phối để chọn người và thời gian |
| Phân công lần đầu | Kỹ thuật viên được giao | **Bạn có công việc mới: WO-...** | Mở Lịch của tôi để xem lịch và nội dung |
| Chuyển việc sang kỹ thuật viên khác | Kỹ thuật viên cũ | **Bạn không còn phụ trách: WO-...** | Dừng theo dõi phiếu cũ và kiểm tra lại lịch cá nhân |
| Chuyển việc sang kỹ thuật viên khác | Kỹ thuật viên mới | **Bạn có công việc mới: WO-...** | Mở Lịch của tôi để tiếp nhận công việc |
| Thay đổi thời gian thực hiện | Kỹ thuật viên hiện tại | **Lịch của bạn đã thay đổi: WO-...** | Kiểm tra lịch mới và lý do thay đổi |
| Phiếu đã quá lịch nhưng chưa bắt đầu | Điều phối viên | **Phiếu đã quá lịch thực hiện: WO-...** | Mở Lịch điều phối để kiểm tra và điều chỉnh |
| Công việc đã quá lịch nhưng chưa bắt đầu | Kỹ thuật viên | **Công việc đã quá lịch: WO-...** | Mở Lịch của tôi và liên hệ điều phối khi cần |
| Khách có thể bị ảnh hưởng vì quá lịch kéo dài | Chăm sóc khách hàng | **Khách hàng có thể cần được liên hệ: WO-...** | Kiểm tra phiếu và chủ động liên hệ khách nếu cần |
| Phiếu đang chờ phụ tùng | Điều phối viên | **Phiếu đang chờ phụ tùng: WO-...** | Theo dõi và phối hợp xử lý |
| Kỹ thuật viên tạo yêu cầu phụ tùng | Nhân viên kho | **Có yêu cầu phụ tùng mới: WO-...** | Mở Yêu cầu phụ tùng để kiểm tra và xác nhận cấp |
| Phiếu được mở lại | Điều phối viên | **Phiếu cần xử lý lại: WO-...** | Xem lý do và điều phối bước tiếp theo |
| Công việc được mở lại | Kỹ thuật viên được giao | **Công việc cần xử lý lại: WO-...** | Tiếp tục xử lý theo phân công |
| Phiếu được mở lại bởi bộ phận khác | Chăm sóc khách hàng | **Phiếu cần theo dõi lại: WO-...** | Theo dõi khách hàng và phối hợp xử lý |
| Kỹ thuật viên hoàn thành công việc | Chăm sóc khách hàng | **Cần theo dõi khách sau sửa chữa: WO-...** | Kiểm tra phản hồi và hậu xử lý nếu cần |
| Khách đã chuyển khoản | Chăm sóc khách hàng | **Cần đối soát chuyển khoản: WO-...** | Kiểm tra tiền thực tế vào tài khoản công ty |
| Kỹ thuật viên đang giữ tiền mặt của khách | Chăm sóc khách hàng | **Cần nhận bàn giao tiền mặt: WO-...** | Nhận bàn giao và đối soát |
| Khách hẹn thanh toán tại quầy | Chăm sóc khách hàng | **Khách hẹn thanh toán tại quầy: WO-...** | Thu tiền khi khách đến rồi đối soát |
| Phiếu đã đóng | Chủ sở hữu | **Phiếu đã hoàn tất: WO-...** | Giám sát kết quả cuối; không cần thao tác thường ngày |
| Phiếu đã đóng | Kỹ thuật viên được giao | **Phiếu đã đóng: WO-...** | Không cần thao tác thêm |
| Phiếu đã hủy | Chủ sở hữu | **Phiếu đã hủy: WO-...** | Tra lịch sử khi cần kiểm tra |
| Công việc đã hủy | Kỹ thuật viên được giao | **Công việc đã hủy: WO-...** | Dừng công việc và kiểm tra lịch cá nhân |
| Phiếu bị hủy bởi bộ phận khác | Chăm sóc khách hàng | **Phiếu đã hủy, cần cập nhật khách hàng: WO-...** | Kiểm tra lý do và liên hệ khách nếu cần |
| Cấp phụ tùng làm tồn xuống dưới ngưỡng | Nhân viên kho | **Tồn kho thấp: [mã phụ tùng]** | Mở Kho phụ tùng để kiểm tra và bổ sung |
| Thay đổi ngưỡng làm phụ tùng trở thành tồn thấp | Nhân viên kho khác người thao tác | **Tồn kho thấp theo ngưỡng mới: [mã phụ tùng]** | Kiểm tra tồn và ngưỡng mới |
| Kiểm kê phát hiện chênh lệch | Chủ sở hữu khác người kiểm kê | **Kiểm kê có chênh lệch: [mã phụ tùng]** | Mở Lịch sử biến động để đối chiếu |
| Kiểm kê kết thúc với tồn thấp | Nhân viên kho | **Tồn kho thấp sau kiểm kê: [mã phụ tùng]** | Kiểm tra và bổ sung nếu cần |

## 3. Những thao tác không tạo thông báo

Các thao tác dưới đây đã có phản hồi ngay trên màn hình và có nơi tra cứu phù hợp nên không tạo thêm thông báo:

- tạo, sửa hoặc nhập danh sách khách hàng và thiết bị;
- thay đổi kênh tiếp nhận thông thường;
- cập nhật hồ sơ kỹ thuật viên;
- tải tệp đính kèm;
- nhập kho thông thường;
- từng bước tiến độ hiện trường không cần bộ phận khác hành động;
- từng lần sử dụng hoặc hoàn trả phụ tùng thông thường;
- các thay đổi dữ liệu nền chỉ phục vụ quản trị.

Dùng **Tiến trình** để xem câu chuyện của một phiếu công việc, **Lịch sử biến động** để xem hàng ra/vào kho và **Nhật ký hệ thống** để truy vết các thao tác quản trị quan trọng.

## 4. Đồng bộ giữa dữ liệu mới và dữ liệu cũ

Thông báo mới luôn được tạo bằng nội dung nghiệp vụ dễ hiểu. Một số thông báo cũ đã lưu từ phiên bản trước có thể còn dùng cách gọi cũ; lớp hiển thị của giao diện chịu trách nhiệm chuyển các nội dung này sang cách gọi hiện tại trước khi người dùng nhìn thấy.

Việc tương thích dữ liệu cũ chỉ nằm ở lớp hiển thị. Không đưa logic chuyển đổi chuỗi rải rác vào bố cục chung hoặc các trang nghiệp vụ.

## 5. Quy ước triển khai nội bộ

Phần này dành cho lập trình viên và kiểm thử viên; các tên bên dưới là mã nội bộ, **không phải nội dung được hiển thị trực tiếp cho người dùng**.

- `NotificationCopy` là điểm vào duy nhất để các dịch vụ nghiệp vụ tạo nội dung thông báo.
- `WorkOrderDispatchNotificationCopy`, `PaymentNotificationCopy` và `InventoryNotificationCopy` giữ nội dung theo từng nhóm nghiệp vụ; dịch vụ nghiệp vụ chỉ truyền dữ liệu ngữ cảnh.
- Nội dung được chuẩn hóa và giới hạn độ dài tại `NotificationCopy.Copy`.
- Các lần quét công việc quá lịch dùng khóa sự kiện để tránh phát lặp cùng một thông báo cho cùng người nhận.
- Thông báo cho Chăm sóc khách hàng về lịch quá hạn dùng khóa riêng để không làm mất cảnh báo vận hành ban đầu của Điều phối viên hoặc Kỹ thuật viên.
- Dữ liệu thông báo dư thừa của các phiên bản cũ được dọn bằng migration tương ứng; lịch sử nghiệp vụ vẫn nằm ở Tiến trình, Lịch sử biến động và Nhật ký hệ thống.

Khi thêm một tình huống thông báo mới, phải cập nhật đồng thời: nội dung runtime, bảng này, hướng dẫn người dùng nếu hành vi có thay đổi và kiểm thử liên quan.
