# Quy trình phát triển bắt buộc

## 1. Trước khi code

1. Viết user story và acceptance criteria.
2. Xác định role được phép thao tác.
3. Xác định transaction boundary và dữ liệu bị thay đổi.
4. Xác định validation/constraint/index.
5. Cập nhật API/schema trước khi viết UI.

## 2. Khi code backend

- Controller chỉ mapping HTTP và validation.
- Application service điều phối use case và transaction.
- Domain giữ state transition/quy tắc cốt lõi.
- Repository luôn tenant-scoped.
- Không trả JPA entity trực tiếp ra API.
- Không nuốt exception hoặc trả `200` cho lỗi nghiệp vụ.
- Thay đổi schema phải qua Flyway migration mới.

## 3. Khi code frontend

- API tập trung ở `src/api`.
- Kiểu dữ liệu tập trung ở `src/types`.
- Page không tự tạo nhiều Axios instance.
- Mọi request có loading/error/empty state.
- Form có validation và thông báo rõ ràng.
- Role không thấy menu không được dùng; backend vẫn là lớp kiểm soát cuối.

## 4. Definition of Done

Một tính năng chỉ được coi hoàn thành khi:

- Acceptance criteria chạy được từ UI tới database.
- Có validation và error code.
- Có authorization và tenant isolation.
- Có test cho business rule quan trọng.
- Migration chạy trên database mới.
- Swagger/types/frontend đồng bộ.
- `mvn clean test`, `npm run lint`, `npm run build` đều qua.
- README/tài liệu được cập nhật nếu flow thay đổi.
- Không có secret, generated folder hoặc debug code trong commit.

## 5. Git workflow

- Branch: `feature/<name>`, `fix/<name>`, `docs/<name>`.
- Commit nhỏ, một mục đích: `feat(work-order): prevent overlapping appointments`.
- Pull request phải mô tả flow, ảnh UI, test và migration.
- Không force push lên `main`.
