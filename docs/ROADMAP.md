# Product & Engineering Roadmap

Roadmap này ưu tiên **độ sâu nghiệp vụ và chất lượng vận hành** thay vì tăng số module hoặc thêm hạ tầng chỉ để làm đẹp portfolio. ServiceOps hiện đã có một luồng field-service end-to-end đủ rõ để tiếp tục productize mà không cần đổi kiến trúc modular monolith.

## Baseline hiện có

### Nghiệp vụ

- Customer và installed asset.
- Service request với kênh tiếp nhận cấu hình được.
- Work-order lifecycle/FSM và lịch sử trạng thái.
- Phân công kỹ thuật viên và kiểm tra lịch chồng lấn.
- Inventory current state + transaction ledger + chống âm kho + configurable minimum-stock threshold + stocktake/adjustment + controlled Work Order part return.
- Attachment bằng chứng, invoice/export, CSV import/export.
- Notification, audit trail, dashboard và trợ lý hướng dẫn theo vai trò.
- Năm vai trò nghiệp vụ với tenant isolation; mỗi kỹ thuật viên là một `UserAccount` riêng liên kết 1-1 với `TechnicianProfile`.

### Engineering / production foundation

- Java 21, Spring Boot, JPA/Hibernate, PostgreSQL và Flyway.
- JWT/RBAC, login throttling, demo protection và upload hardening.
- Pessimistic locking cho scheduling/inventory/OWNER invariant.
- Unit/integration tests với JUnit/Mockito/Testcontainers.
- React + TypeScript + Ant Design với feature-oriented frontend.
- GitHub Actions, multi-stage Docker, Nginx, production Compose.
- Health/readiness, Actuator/Prometheus, request correlation.
- PostgreSQL backup/guarded restore scripts.

## Ưu tiên product tiếp theo

### P1 — Dispatch Schedule Board — implemented baseline

Mục tiêu: biến dữ liệu lịch hiện có thành một màn điều phối thực sự hữu dụng cho Dispatcher.

Baseline đã triển khai:

- Lịch tuần theo từng kỹ thuật viên hoạt động.
- Hàng đợi `OPEN` / `REOPENED`, ưu tiên phiếu khẩn/cao trước.
- Mở và đổi kỹ thuật viên/thời gian trực tiếp từ board.
- Dùng lại scheduling transaction + pessimistic technician lock + overlap detection hiện có; conflict tiếp tục trả `409`.
- API board tenant-scoped, giới hạn cửa sổ truy vấn 31 ngày và có index PostgreSQL riêng cho active appointment range.

Các increment chỉ thêm khi có nhu cầu thật: drag/drop, working-hours/leave calendar, travel-time routing hoặc capacity rules.

**Lý do ưu tiên:** scheduling là capability lõi của field service; schedule board làm rõ giá trị sản phẩm hơn việc thêm một module CRUD mới.

### P1 — Technician Personal Schedule — implemented baseline

Mục tiêu: tách rõ workspace của Dispatcher và Technician thay vì dùng một bảng lịch chung cho mọi vai trò.

Baseline đã triển khai:

- `Lịch của tôi` theo tuần cho tài khoản `TECHNICIAN`.
- Backend tự ánh xạ `CurrentUser.userId` → `TechnicianProfile`; API không nhận `technicianId` từ client.
- Hiển thị giờ, work order, khách hàng, địa chỉ, thiết bị, ưu tiên và trạng thái.
- Hai tài khoản kỹ thuật viên demo riêng để kiểm tra schedule isolation.
- Frontend có route-level role guard dùng chung với sidebar; backend method security vẫn là nguồn bảo vệ cuối.

Increment tương lai chỉ khi có nhu cầu hiện trường thật: deep-link trực tiếp vào work order, calendar sync, mobile/PWA và offline queue.

### P1 — SLA / Promised Service Window

- `responseDueAt` / `resolutionDueAt` hoặc service window tương đương.
- SLA state: on-track / at-risk / breached.
- Cảnh báo trên dashboard và danh sách công việc.
- Audit/escalation cho thay đổi deadline quan trọng.

### P2 — Preventive Maintenance / Service Agreements

- Kế hoạch bảo trì định kỳ theo customer/asset.
- Recurrence rule có timezone rõ ràng.
- Job an toàn/idempotent để sinh work order trước ngày thực hiện.
- Lịch sử nguồn gốc: agreement → generated work order.
- Có thể mở rộng invoice định kỳ sau khi nghiệp vụ thật yêu cầu.

### P2 — Technician Mobile/PWA

- Mobile-first agenda cho công việc được giao.
- Thao tác trạng thái, ghi chú, phụ tùng và attachment tối ưu cho hiện trường.
- Offline queue/sync chỉ triển khai khi use case offline được xác nhận.

## Engineering depth trước khi scale kiến trúc

- Query/index review với dữ liệu lớn hơn và `EXPLAIN ANALYZE`.
- API/load test cho work-order list, scheduling và inventory hot paths.
- Sonar/static-analysis quality gate nếu phù hợp workflow CI.
- Structured logs/tracing dashboard khi có môi trường quan sát tập trung.
- Backup restore drill định kỳ thay vì chỉ có script.
- Object storage/SSO chỉ khi deployment thực tế cần chúng.

## Không thêm nếu chưa có yêu cầu thật

- Microservices chỉ để tách service cho đẹp CV.
- Kafka/RabbitMQ khi chưa có asynchronous integration hoặc throughput requirement rõ ràng.
- Kubernetes khi single-node/Compose vẫn đáp ứng môi trường demo hoặc deployment nhỏ.
- Redis nếu chưa có distributed cache/rate-limit/session requirement.
- Elasticsearch nếu PostgreSQL search/index vẫn đáp ứng.
- AI dispatch/route optimization trước khi có dữ liệu, routing constraints và KPI đủ tốt để đánh giá kết quả.

Mỗi hạng mục roadmap phải có business problem, acceptance criteria, test strategy và deployment impact trước khi bắt đầu implementation.
