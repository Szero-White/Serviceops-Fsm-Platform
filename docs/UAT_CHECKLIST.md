# UAT Checklist

Checklist này dùng cho local release candidate hoặc production smoke. Không dùng để thay automated test.

## 1. Authentication và role

- [ ] Login được với tài khoản hợp lệ.
- [ ] Tài khoản inactive không dùng JWT cũ để tiếp tục truy cập.
- [ ] `OWNER` thấy khu vực quản trị/audit.
- [ ] `CUSTOMER_SERVICE` thấy customer/asset/service request/payment.
- [ ] `DISPATCHER` thấy work order/schedule/technician, không có intake/payment settlement.
- [ ] `TECHNICIAN` chỉ thấy công việc/lịch của mình.
- [ ] `WAREHOUSE_STAFF` vào part requests/inventory, không vào operational dashboard/work order.
- [ ] Truy cập URL trái quyền bị backend từ chối, không chỉ bị frontend ẩn menu.

## 2. Customer / Asset / Service Request

- [ ] Tạo Customer; code tự sinh dạng `KH-YYYYMMDD-NNN` và không cho user tự nhập.
- [ ] Tạo Asset đúng Customer; serial number vẫn là field riêng, không bị đổi thành business code.
- [ ] Không tạo Asset mới cho Customer inactive.
- [ ] Tạo Service Request ở `OPEN`.
- [ ] AI draft nếu dùng chỉ thay `title`/`description`, không tự đổi priority/channel.
- [ ] Convert Service Request tạo đúng một Work Order, Service Request chuyển `CONVERTED`.
- [ ] Cancel Service Request giữ record lịch sử, không hard delete.

## 3. Điều phối Work Order

- [ ] Work Order có code `WO-YYYYMMDD-NNN`.
- [ ] Dispatcher assign Technician và lịch hợp lệ.
- [ ] Không xếp hai lịch overlap cho cùng Technician.
- [ ] Reschedule/redispatch hiển thị lý do/thông tin mới đúng.
- [ ] Sau khi field work bắt đầu, action điều phối bị giới hạn theo policy.
- [ ] Technician khác không thao tác được Work Order không thuộc mình.

## 4. Technician và phụ tùng

- [ ] Technician chuyển `ON_THE_WAY → IN_PROGRESS`.
- [ ] Tạo part request không làm thay đổi stock.
- [ ] Warehouse `ISSUE` đúng số lượng làm stock giảm đúng một lần.
- [ ] Không ISSUE vượt stock hoặc double issue.
- [ ] Technician ghi `USED` không làm stock giảm lần hai.
- [ ] `RETURN` phần dư làm stock tăng và không vượt outstanding.
- [ ] Low-stock notification chỉ phát theo threshold/event phù hợp, không spam lặp vô nghĩa.

## 5. Hoàn thành và customer acceptance

- [ ] Technician nhập diagnosis/resolution và complete Work Order.
- [ ] Billing hiển thị đúng labor/part/fee hiện tại.
- [ ] Customer acceptance chuyển sang `CUSTOMER_ACCEPTED` và tạo snapshot.
- [ ] Thay catalog/return part sau acceptance không làm thay đổi snapshot đã xác nhận.
- [ ] Reopen (nếu test) yêu cầu đúng role/lý do và history giữ được repair cycle trước.

## 6. Payment / Receipt / Closure

Chọn ít nhất một flow để smoke; trước release lớn nên kiểm cả ba.

### Transfer

- [ ] Technician báo chuyển khoản → `TRANSFER_PENDING_VERIFICATION`.
- [ ] Customer Service verify → `SETTLED`.

### Cash

- [ ] Technician ghi nhận tiền mặt → `CASH_PENDING_HANDOVER`.
- [ ] Customer Service xác nhận nhận tiền → `SETTLED`.

### Counter

- [ ] Ghi nhận thanh toán tại quầy → `COUNTER_PAYMENT_PENDING`.
- [ ] Customer Service xác nhận thực thu → `SETTLED`.

Sau settlement:

- [ ] Receipt chỉ phát hành khi `SETTLED` và có code `BN-YYYYMMDD-NNN`.
- [ ] Gửi lại action không tạo duplicate receipt/settlement.
- [ ] Closure chỉ thành công khi Work Order `CUSTOMER_ACCEPTED` và payment `SETTLED`.
- [ ] Work Order chuyển `CLOSED`; phần return vật tư còn dư vẫn có thể xử lý theo policy.

## 7. Inventory catalog

- [ ] Tạo Spare Part có code `PT-YYYYMMDD-NNN`.
- [ ] Không nhập business code bằng tay.
- [ ] Import CSV validate row và không commit partial data ngoài behavior đã thiết kế.
- [ ] Active/inactive hoạt động; record lịch sử không bị mất.
- [ ] Stocktake tạo đúng movement/audit khi có chênh lệch.

## 8. History, Audit, Notification

- [ ] History summary lấy đúng số `CUSTOMER_ACCEPTED`, `CLOSED`, `CANCELLED` theo scope/filter.
- [ ] Timeline hiển thị actor/status/note đúng.
- [ ] Audit hiển thị tên nghiệp vụ + business code khi có thể; không chỉ UUID/raw enum.
- [ ] Attachment purpose được hiển thị bằng wording người dùng.
- [ ] Notification đến đúng role/người nhận và unread state cập nhật đúng.
- [ ] CRUD thông thường không tạo notification noise ngoài policy.

## 9. AI Help

- [ ] Câu hỏi trong scope trả hướng dẫn đúng role.
- [ ] Câu hỏi ngoài scope bị từ chối/hướng dẫn an toàn.
- [ ] Không trả system prompt, API key, JWT, secret, env hoặc dữ liệu live không có trong context.
- [ ] Khi Gemini không cấu hình/lỗi, fallback nội bộ vẫn hoạt động.
- [ ] UI phân biệt nguồn `Gemini` và `Nội bộ`.

## 10. Error và UI

- [ ] Validation form chỉ rõ field lỗi.
- [ ] 403/409 business error hiển thị message phù hợp, không lộ SQL/stack trace.
- [ ] Endpoint không tồn tại trả 404 hợp lý.
- [ ] Mutation success/error có feedback; loading/empty/error state không gây hiểu nhầm.
- [ ] Table chính không overlap text ở kích thước desktop thông thường.
- [ ] Search/filter/pagination vẫn giữ logic đúng sau create/update.

## 11. Production smoke sau deploy

- [ ] `/actuator/health/readiness` trả healthy.
- [ ] Frontend tải qua public HTTPS.
- [ ] Login đủ các role cần dùng.
- [ ] Tạo một Customer/Service Request/Work Order mới để xác nhận Flyway + business code trên DB production.
- [ ] Chạy một flow ngắn đến ít nhất `IN_PROGRESS` và kiểm audit/notification.
- [ ] Nếu release có thay payment/inventory, chạy full end-to-end đến `CLOSED`.
- [ ] Kiểm log không có migration error, repeated exception hoặc secret.

Khi checklist production smoke đạt và CI xanh, release được xem là ổn định; không tiếp tục refactor nếu không có bug/yêu cầu mới.
