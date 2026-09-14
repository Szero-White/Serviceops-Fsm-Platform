# Hướng dẫn sử dụng ServiceOps FSM

Tài liệu này dành cho người sử dụng hệ thống. Nội dung tập trung vào công việc cần làm trên giao diện và cố ý không sử dụng mã trạng thái, tên kỹ thuật hoặc chi tiết triển khai nội bộ.

> **Mã nghiệp vụ được tạo tự động:** khi thêm Khách hàng hoặc Phụ tùng, người dùng không nhập mã thủ công. Hệ thống cấp mã theo ngày và thứ tự trong ngày (`KH-YYYYMMDD-NNN`, `PT-YYYYMMDD-NNN`). Phiếu công việc và biên nhận cũng được cấp tương tự (`WO-...`, `BN-...`). UUID kỹ thuật không dùng làm mã giao tiếp với người dùng.

## 1. Tài khoản dùng thử theo vai trò

- `owner` — **Chủ sở hữu**: giám sát toàn hệ thống, quản lý người dùng, dữ liệu nền, điều phối, kho, thông tin nhận thanh toán và nhật ký hệ thống.
- `customer-service` — **Chăm sóc khách hàng**: quản lý khách hàng, thiết bị, yêu cầu dịch vụ và các bước thanh toán sau sửa chữa.
- `dispatcher` — **Điều phối viên**: phân công kỹ thuật viên, xếp lịch và theo dõi các phiếu cần điều phối.
- `technician` — **Kỹ thuật viên Phạm Quốc**: xử lý các công việc được giao, cập nhật tiến độ, phụ tùng, chi phí và kết quả tại hiện trường.
- `technician-2` — **Kỹ thuật viên Võ Hoàng**: tài khoản kỹ thuật viên thứ hai để kiểm tra việc tách lịch và công việc theo từng người.
- `warehouse` — **Nhân viên kho**: xử lý yêu cầu phụ tùng, quản lý tồn kho, kiểm kê, hoàn trả và lịch sử biến động.

Mật khẩu dùng thử mặc định: `Demo@2026`. Tài khoản dùng thử chỉ phục vụ trình diễn hệ thống.

## 2. Khu vực làm việc của từng vai trò

- **Chủ sở hữu**: Tổng quan, khách hàng và thiết bị, phiếu công việc, điều phối, kho, người dùng, thiết lập thanh toán và Nhật ký hệ thống. Chủ sở hữu chủ yếu giám sát và quản trị, không làm thay các bước xác nhận hiện trường, đối soát tiền hoặc giao nhận kho của nhân viên phụ trách.
- **Chăm sóc khách hàng**: Tổng quan, Yêu cầu dịch vụ, Phiếu công việc, Xử lý thanh toán, Lịch sử phiếu, Khách hàng, Thiết bị và Kênh tiếp nhận.
- Trang **Lịch sử phiếu** hiển thị bốn bộ đếm: tổng hồ sơ, chờ hoàn tất hồ sơ, đã đóng và đã hủy; các số đếm lấy trực tiếp từ backend theo cùng quyền truy cập và từ khóa tìm kiếm.
- **Điều phối viên**: Tổng quan, Phiếu công việc, Lịch điều phối, Lịch sử phiếu và Kỹ thuật viên.
- **Kỹ thuật viên**: Tổng quan, Lịch của tôi, Phiếu công việc và Lịch sử phiếu. Phụ tùng được yêu cầu trực tiếp trong phiếu đang xử lý.
- **Nhân viên kho**: Yêu cầu phụ tùng, Kho phụ tùng, Kiểm kê tồn kho và Lịch sử biến động.

## 3. Quy trình nghiệp vụ chuẩn

### Bước 1 — Tiếp nhận khách hàng và yêu cầu dịch vụ

1. Đăng nhập bằng tài khoản Chăm sóc khách hàng.
2. Mở **Khách hàng** để tìm hoặc tạo hồ sơ khách hàng.
3. Mở **Thiết bị**, chọn khách hàng và nhập loại thiết bị, hãng, mẫu thiết bị, số sê-ri và thông tin bảo hành nếu có.
4. Mở **Yêu cầu dịch vụ**, chọn mức ưu tiên và kênh tiếp nhận, sau đó nhập tiêu đề và mô tả sự cố.
5. Có thể dùng **AI gợi ý** để hỗ trợ viết lại tiêu đề và mô tả cho rõ ràng. Trợ lý không tự thay đổi mức ưu tiên hoặc kênh tiếp nhận đã chọn.
6. Khi thông tin đã đầy đủ, chọn **Chuyển sang điều phối** để tạo phiếu công việc.

### Bước 2 — Phân công và xếp lịch

1. Đăng nhập bằng tài khoản Điều phối viên.
2. Mở **Lịch điều phối** để xem các phiếu đang chờ phân công.
3. Chọn kỹ thuật viên và thời gian phù hợp.
4. Có thể điều chỉnh kỹ thuật viên hoặc thời gian trước khi công việc bắt đầu.
5. Nếu thời gian bị chồng lấn với lịch hiện có, hệ thống sẽ từ chối và giải thích lý do để người điều phối chọn lại.

### Bước 3 — Kỹ thuật viên thực hiện công việc

1. Đăng nhập bằng tài khoản Kỹ thuật viên.
2. Mở **Lịch của tôi** để xem lịch được giao cho chính tài khoản hiện tại.
3. Mở **Phiếu công việc** tương ứng và cập nhật tiến độ theo thực tế: đã nhận việc, đang di chuyển, đang thực hiện hoặc chờ phụ tùng.
4. Có thể chụp ảnh hoặc tải tài liệu minh chứng phục vụ hồ sơ sửa chữa.
5. Nếu cần phụ tùng, mở mục **Phụ tùng** và tạo yêu cầu với số lượng cần dùng. Yêu cầu đang chờ có thể được điều chỉnh hoặc hủy kèm lý do.
6. Sau khi kho đã giao phụ tùng, ghi nhận **Thực tế đã dùng**. Việc ghi số lượng đã dùng không làm trừ tồn kho lần thứ hai.
7. Nhập **Chẩn đoán / nguyên nhân** và **Giải pháp đã thực hiện**, sau đó chọn **Hoàn thành công việc**.
8. Kiểm tra tiền công, chi phí phát sinh và phụ tùng thực tế trước khi cho khách xem kết quả.

### Bước 4 — Kho xử lý phụ tùng

1. Đăng nhập bằng tài khoản Nhân viên kho; hệ thống ưu tiên màn **Yêu cầu phụ tùng**.
2. Khi đã giao hàng thực tế, chọn **Xác nhận cấp**. Chỉ tại bước này số lượng tồn kho mới giảm.
3. Nếu không thể cấp, chọn **Không thể cấp** và nhập lý do để kỹ thuật viên và các bộ phận liên quan biết tình trạng.
4. Theo dõi **Vật tư đang do kỹ thuật viên giữ** để biết phần đã giao nhưng chưa được sử dụng hoặc hoàn trả hết.
5. Khi nhận lại phụ tùng, chọn **Hoàn trả**, nhập số lượng thực nhận và lý do. Hệ thống không cho hoàn trả vượt quá số lượng còn đang được giữ.
6. Mở **Lịch sử biến động** để đối chiếu các lần nhập, cấp, sử dụng, hoàn trả, kiểm kê và điều chỉnh.
7. Mở **Kiểm kê tồn kho** khi cần so sánh số lượng thực tế với số liệu đang được hệ thống ghi nhận.

### Bước 5 — Khách xác nhận và xử lý thanh toán

1. Sau khi công việc hoàn thành, Kỹ thuật viên cho khách kiểm tra kết quả và tổng chi phí.
2. Khi khách đồng ý, chọn **Ghi nhận khách xác nhận**. Từ thời điểm này chi phí của phiếu được chốt và không còn chỉnh sửa theo quy trình thông thường.
3. Kỹ thuật viên ghi nhận đúng tình huống thanh toán: khách đã chuyển khoản, kỹ thuật viên đã nhận tiền mặt, hoặc khách hẹn thanh toán tại quầy.
4. Chăm sóc khách hàng mở **Xử lý thanh toán** để kiểm tra các khoản đang chờ.
5. Với chuyển khoản, chỉ xác nhận sau khi tiền thực tế đã vào tài khoản công ty. Với tiền mặt, chỉ xác nhận sau khi đã nhận bàn giao. Với khách thanh toán tại quầy, chỉ xác nhận sau khi đã thực nhận đủ tiền.
6. Sau khi khoản tiền đã được đối soát, phát hành hoặc tải **Biên nhận**, sau đó chọn **Đóng phiếu**.
7. Nếu đã đối soát nhưng chưa đóng phiếu, hồ sơ xuất hiện trong **Lịch sử phiếu** với trạng thái **Chờ hoàn tất hồ sơ** để Chăm sóc khách hàng tiếp tục xử lý.

## 4. Hình ảnh và tài liệu trong quá trình sửa chữa

- Kỹ thuật viên được giao phiếu có thể chụp ảnh hoặc tải tệp JPG, PNG, WEBP hoặc PDF, tối đa 10 MB mỗi tệp.
- Khi hồ sơ còn được phép chỉnh sửa, người có quyền có thể đổi tên hoặc xóa tệp do mình quản lý.
- Sau khi khách đã xác nhận, phiếu đã đóng hoặc đã hủy, hồ sơ chuyển sang chế độ chỉ xem đối với các bằng chứng đã lưu.
- Ảnh chứng minh giao dịch chuyển khoản được quản lý riêng trong mục **Thanh toán**. Sau khi kỹ thuật viên đã báo khách chuyển khoản, bằng chứng được giữ nguyên để Chăm sóc khách hàng đối chiếu.

## 5. Thông báo và nhật ký hệ thống

### Thông báo

Thông báo chỉ dành cho **việc cần chú ý hoặc cần hành động**, không phải bản sao của toàn bộ lịch sử thay đổi.

Ví dụ:

- Điều phối viên nhận **Cần phân công kỹ thuật viên: WO-...** khi có phiếu mới cần xếp lịch.
- Kỹ thuật viên nhận **Bạn có công việc mới: WO-...** khi được giao việc.
- Chăm sóc khách hàng nhận **Cần đối soát chuyển khoản: WO-...** khi kỹ thuật viên ghi nhận khách đã chuyển tiền.
- Nhân viên kho nhận **Có yêu cầu phụ tùng mới: WO-...** hoặc cảnh báo tồn kho thấp.
- Chủ sở hữu nhận các kết quả hoặc ngoại lệ cần giám sát như phiếu đã đóng, phiếu đã hủy hoặc kiểm kê có chênh lệch.

Một thông báo nên cho biết rõ việc gì xảy ra, liên quan tới phiếu/khách hàng/phụ tùng nào và bước tiếp theo cần làm. Người dùng có thể chuyển thông báo giữa **Đã đọc** và **Chưa đọc** để giữ lại việc cần theo dõi.

### Nhật ký hệ thống

**Nhật ký hệ thống** dành cho Chủ sở hữu khi cần tra cứu các thao tác quan trọng. Giao diện hiển thị:

- thời gian;
- người thực hiện và vai trò;
- hành động bằng tên nghiệp vụ dễ hiểu;
- loại dữ liệu liên quan;
- nội dung thay đổi đã được chuyển sang ngôn ngữ nghiệp vụ.

Khi một thao tác liên quan tới phụ tùng, nhật ký ưu tiên hiển thị **Tên phụ tùng (Mã phụ tùng)** để người dùng vừa đọc hiểu vừa tra cứu chính xác. Các mục đích tệp đính kèm cũng được trình bày bằng tên nghiệp vụ thay vì mã kỹ thuật. Mã của sự kiện lịch sử đã phát sinh trước khi chuẩn hóa vẫn được giữ nguyên để bảo đảm khả năng truy vết.

Mã kỹ thuật nội bộ và mã định danh dài không được dùng làm thông tin chính trên màn hình khi đã có tên hoặc mã nghiệp vụ dễ nhận biết hơn.

## 6. Trợ lý AI

- Trợ lý tự nhận biết vai trò của tài khoản đang đăng nhập và chỉ hướng dẫn các chức năng thuộc phạm vi của vai trò đó.
- Có thể hỏi: **“Trong vai trò này tôi được làm gì?”**, **“Làm sao yêu cầu phụ tùng?”**, **“Tôi cần làm gì để đóng phiếu?”** hoặc các câu hỏi nghiệp vụ tương tự.
- Nhãn **Gemini** cho biết câu trả lời được hỗ trợ bởi Gemini; nhãn **Nội bộ** cho biết hệ thống đang dùng hướng dẫn tích hợp sẵn.
- Trợ lý không cung cấp mật khẩu, thông tin truy cập, cấu hình bảo mật hoặc chỉ dẫn nội bộ của hệ thống.
- Câu trả lời được chuẩn hóa sang ngôn ngữ nghiệp vụ; mã trạng thái và thuật ngữ triển khai nội bộ không được đưa thẳng cho người sử dụng.

## 7. Quy tắc quan trọng

- Mỗi vai trò chỉ thực hiện các bước thuộc trách nhiệm của mình; hệ thống chặn thao tác ngoài phạm vi ngay cả khi người dùng cố mở đường dẫn trực tiếp.
- Phiếu công việc phải đi đúng trình tự; không thể tùy ý bỏ qua các bước bắt buộc.
- Kỹ thuật viên chỉ xem và thao tác các phiếu được giao cho chính mình.
- Mỗi kỹ thuật viên có tài khoản riêng; lịch cá nhân không được phép xem chéo sang kỹ thuật viên khác.
- Phụ tùng chỉ bị trừ tồn khi kho xác nhận đã giao thực tế; ghi nhận số lượng đã dùng không trừ tồn thêm lần nữa.
- Sau khi khách xác nhận, chi phí đã được chốt. Việc mở lại hoặc tạo công việc tiếp theo phải tuân theo quy trình của hệ thống.
- Tên đăng nhập được giữ ổn định sau khi tạo để bảo toàn lịch sử. Chủ sở hữu vẫn có thể cập nhật họ tên hiển thị, mật khẩu và trạng thái tài khoản theo quy tắc bảo vệ tài khoản.
- Các trường bắt buộc được đánh dấu trên biểu mẫu. Nếu còn thiếu dữ liệu, giao diện sẽ chỉ rõ trường cần bổ sung và không gửi thao tác chưa hợp lệ.

## 8. Khi cần hỗ trợ

Khi gặp lỗi, ghi lại **mã hỗ trợ** nếu giao diện có hiển thị, thời điểm xảy ra và thao tác vừa thực hiện. Không cần gửi mật khẩu hoặc thông tin truy cập. Chủ sở hữu có thể dùng **Nhật ký hệ thống** để tra cứu thêm trước khi chuyển thông tin cho người phụ trách kỹ thuật.
