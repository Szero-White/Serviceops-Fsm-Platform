# Kiến trúc ServiceOps

Tài liệu này giải thích cách ServiceOps được tổ chức ở mức kỹ thuật. Các thuật ngữ kỹ thuật cần thiết vẫn được giữ, nhưng phần mô tả ưu tiên cách viết dễ đọc.

## 1. Kiến trúc tổng thể

ServiceOps dùng mô hình **modular monolith**: backend là một ứng dụng Spring Boot duy nhất, nhưng code được chia thành các module theo từng nhóm nghiệp vụ.

```text
React / TypeScript
       ↓ REST /api/v1
Spring Boot
       ↓
Các module nghiệp vụ
       ↓
Spring Data JPA / JdbcTemplate
       ↓
PostgreSQL
```

Các module backend chính:

- `identity`: người dùng và tài khoản
- `customer`: khách hàng
- `asset`: thiết bị
- `servicerequest`: yêu cầu dịch vụ
- `workorder`: Phiếu công việc
- `scheduling`: lịch và điều phối
- `technician`: kỹ thuật viên
- `inventory`: phụ tùng và tồn kho
- `payment`: thanh toán và biên nhận
- `attachment`: file đính kèm
- `notification`: thông báo
- `audit`: nhật ký thao tác
- `dashboard`: dữ liệu tổng hợp
- `ai`: chức năng hỗ trợ AI
- `tenant`: phạm vi dữ liệu theo tenant

Trong mỗi module, code được tách theo mục đích:

- `web`: nhận/trả HTTP request.
- `application`: xử lý luồng nghiệp vụ, transaction và phối hợp các thành phần.
- `domain`: các quy tắc và đối tượng nghiệp vụ chính.
- repository/query: đọc và ghi dữ liệu.

Frontend được chia theo `src/features/*`. Mỗi feature giữ API, page/component, type và phần hiển thị liên quan đến chức năng đó.

## 2. Đăng nhập, phân quyền và dữ liệu theo tenant

- Spring Security dùng JWT và không lưu session phía server.
- JWT dùng HS256 và kiểm tra issuer.
- `ActiveUserJwtValidator` kiểm tra tài khoản còn hoạt động hay không; tài khoản bị khóa không thể tiếp tục dùng token cũ.
- Mật khẩu được băm bằng BCrypt cost 12.
- Role lấy từ JWT, nhưng backend vẫn kiểm tra thêm người dùng có thật sự được phép thao tác trên dữ liệu đó hay không.
- Với thao tác của kỹ thuật viên, backend lấy kỹ thuật viên từ user đang đăng nhập thay vì tin `technicianId` do frontend tự gửi.
- Dữ liệu nghiệp vụ có `tenantId`; các truy vấn chính được giới hạn theo tenant hiện tại.
- CORS lấy từ `CORS_ALLOWED_ORIGINS`; production không dùng wildcard.
- Lỗi trả về client được lọc để không lộ SQL, stack trace hoặc thông tin hạ tầng.

## 3. Database, Flyway và transaction

JPA dùng `ddl-auto=validate`. Nghĩa là JPA chỉ kiểm tra schema có đúng với code hay không; JPA không tự sửa cấu trúc database.

Flyway quản lý migration. Repository hiện có migration từ `V1` đến `V20`; migration đã chạy không được sửa lại, migration mới phải thêm version mới.

Các thao tác quan trọng dùng transaction và locking khi cần, đặc biệt:

- xếp lịch và đổi người phụ trách;
- cấp/trả phụ tùng;
- xác nhận thanh toán, phát hành biên nhận và đóng Phiếu công việc;
- đồng bộ tài khoản/profile kỹ thuật viên;
- sinh mã nghiệp vụ.

Các mã `KH`, `WO`, `PT`, `BN` do `BusinessCodeGenerator` sinh trong database. PostgreSQL dùng một counter theo `(tenant_id, code_type, business_date)` để tránh hai request cùng lấy một mã.

Ngày dùng để sinh mã lấy theo `BUSINESS_TIME_ZONE`. Thời gian lưu/trao đổi với database dùng UTC để tránh sai lệch timezone.

## 4. Phiếu công việc và trạng thái

Các trạng thái chính của `WorkOrderStatus`:

```text
DRAFT
OPEN
SCHEDULED
ASSIGNED
ON_THE_WAY
IN_PROGRESS
WAITING_FOR_PARTS
COMPLETED
CUSTOMER_ACCEPTED
CLOSED
CANCELLED
REOPENED
```

Backend không cho frontend tự chuyển sang bất kỳ trạng thái nào. Mỗi bước được kiểm tra theo vai trò và trạng thái hiện tại.

Một số thao tác như khách hàng xác nhận, thanh toán và đóng Phiếu công việc được tách thành use case riêng thay vì dùng nút đổi trạng thái chung. Cách này giúp backend kiểm tra đủ điều kiện trước khi thay đổi dữ liệu.

## 5. Chốt chi phí

Khi khách hàng xác nhận hoàn thành, hệ thống lưu một **billing snapshot**: bản chốt số tiền và các dòng chi tiết ở thời điểm đó.

Sau khi đã chốt:

- thay đổi giá trong danh mục không làm đổi hóa đơn đã xác nhận;
- trả phụ tùng về sau không tự sửa số tiền đã chốt;
- thanh toán và biên nhận dựa trên số tiền đã được xác nhận.

Mục tiêu là giữ hồ sơ tài chính của một Phiếu công việc ổn định sau khi khách đã xác nhận.

## 6. Tồn kho và phụ tùng

Hệ thống tách ba khái niệm:

- **Yêu cầu phụ tùng:** kỹ thuật viên cần vật tư gì.
- **Biến động kho:** lúc kho thực sự xuất hoặc nhận lại hàng.
- **Số lượng đã dùng:** kỹ thuật viên đã dùng bao nhiêu cho công việc.

Quy tắc chính:

- `ISSUE` mới là bước làm giảm tồn kho.
- `USED` chỉ ghi nhận lượng đã sử dụng, không giảm tồn kho thêm lần nữa.
- `RETURN` tăng tồn kho trở lại.
- Backend chặn xuất quá tồn, xuất trùng hoặc trả quá số lượng có thể trả.

Các thao tác thay đổi số lượng dùng transaction/locking để tránh sai dữ liệu khi nhiều request xảy ra gần nhau.

## 7. Audit và thông báo

Audit lưu thông tin người thao tác, hành động, đối tượng và nội dung thay đổi để phục vụ tra cứu.

Ở giao diện, hệ thống ưu tiên tên và mã nghiệp vụ dễ hiểu. UUID hoặc raw enum chỉ dùng khi không có thông tin tốt hơn để hiển thị.

Thông báo chỉ được tạo cho các sự kiện cần người khác chú ý hoặc tiếp tục xử lý. Các thao tác CRUD thông thường không tự động gửi thông báo cho mọi người.

Một số handler cô lập lỗi gửi thông báo để lỗi notification không làm hỏng transaction nghiệp vụ chính.

## 8. AI

`AiSuggestionService` chỉ gợi ý `title` và `description` của Yêu cầu dịch vụ. Mức ưu tiên và kênh tiếp nhận vẫn do người dùng chọn.

`AiHelpService`:

- lấy vai trò từ JWT;
- dùng nội dung hướng dẫn phù hợp với vai trò và màn hình;
- không đọc trực tiếp dữ liệu live trong database;
- không tự thay đổi dữ liệu;
- chặn yêu cầu lấy prompt, token, secret hoặc biến môi trường;
- dùng Gemini khi được cấu hình;
- dùng hướng dẫn nội bộ khi Gemini không hoạt động.

Gemini API key chỉ nằm ở backend, không đưa vào frontend.

## 9. File đính kèm

File đi qua `AttachmentService` và một lớp storage riêng. Bản hiện tại lưu file trên filesystem của server.

Backend kiểm tra quyền truy cập, loại file, chữ ký file, kích thước và quota theo tenant.

Cách lưu này phù hợp khi chạy một backend instance. Nếu chạy nhiều instance, cần chuyển sang object storage hoặc nơi lưu trữ dùng chung.

## 10. Frontend

- TanStack Query quản lý dữ liệu lấy từ server.
- Sau mutation, frontend làm mới các query liên quan thay vì tự giữ nhiều bản sao dữ liệu nghiệp vụ.
- Route/menu kiểm soát phần người dùng nhìn thấy, nhưng backend mới là nơi quyết định quyền thật.
- Các enum kỹ thuật được đổi thành nội dung dễ hiểu trước khi hiển thị cho người dùng.
- Các mã nghiệp vụ do backend quản lý được hiển thị ở chế độ chỉ đọc khi phù hợp.

## 11. Môi trường chạy production

Backend production dùng Hikari connection pool, graceful shutdown, forwarded headers và Actuator health endpoint.

Live demo hiện tại chạy theo mô hình:

```text
Internet / HTTPS
      ↓
Nginx
  ├─ React static files
  └─ /api/v1 → Spring Boot :8080
                 ↓
             PostgreSQL
```

Backend chạy bằng `systemd`. Frontend là static build do Nginx phục vụ.

Repository cũng có Docker Compose để dựng PostgreSQL + backend + frontend theo một cấu hình có thể lặp lại trong CI hoặc môi trường self-host.

## 12. Giới hạn kỹ thuật hiện tại

- File upload đang lưu trên local filesystem, phù hợp với một backend instance.
- Bộ đếm đăng nhập sai đang lưu trong bộ nhớ của ứng dụng; nếu chạy nhiều instance nên chuyển sang shared store.
- Kiểm tra tài khoản còn hoạt động cần truy vấn database khi xác thực request.
- Một số truy vấn dashboard/search chưa được benchmark với dữ liệu rất lớn.
- Dự án chưa có load-test benchmark chính thức nên không đưa ra con số về throughput hoặc số người dùng đồng thời.

Các điểm trên chỉ cần tối ưu khi có yêu cầu tải hoặc kiến trúc triển khai cụ thể.
