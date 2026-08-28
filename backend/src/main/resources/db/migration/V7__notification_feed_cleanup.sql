-- Notification center is an attention queue, not a second audit log.
--
-- Older releases persisted routine CRUD/import/status updates to the bell. Runtime code no longer
-- emits those rows: durable history remains available in Audit, Work Order Timeline and Inventory
-- Movements. Remove only those obsolete legacy patterns so existing local/demo databases converge
-- to the current role-aware notification policy and unread counts stay consistent with the drawer.

DELETE FROM notifications
WHERE title IN (
    'Hồ sơ kỹ thuật viên được cập nhật',
    'Tệp đính kèm mới',
    'Yêu cầu dịch vụ mới',
    'Yêu cầu dịch vụ được cập nhật',
    'Yêu cầu dịch vụ đã huỷ',
    'Yêu cầu dịch vụ đã hủy',
    'Yêu cầu dịch vụ đã xoá',
    'Yêu cầu dịch vụ đã xóa',
    'Đã import danh sách khách hàng',
    'Đã import danh sách thiết bị',
    'Đã import danh mục phụ tùng'
)
OR title ~ '^(Người dùng mới|Người dùng được cập nhật|Người dùng đã xoá|Người dùng đã xóa):'
OR title ~ '^(Khách hàng mới|Đã thêm khách hàng|Khách hàng được cập nhật|Thông tin khách hàng đã thay đổi|Khách hàng đã xoá|Khách hàng đã xóa|Đã xóa khách hàng):'
OR title ~ '^(Kênh tiếp nhận mới|Đã thêm kênh tiếp nhận|Kênh tiếp nhận được cập nhật|Thông tin kênh tiếp nhận đã thay đổi|Kênh tiếp nhận đã xoá|Kênh tiếp nhận đã xóa|Đã xóa kênh tiếp nhận):'
OR title ~ '^(Thiết bị mới|Đã thêm thiết bị|Thiết bị được cập nhật|Thông tin thiết bị đã thay đổi|Thiết bị đã xoá|Thiết bị đã xóa|Đã xóa thiết bị):'
OR title ~ '^Phụ tùng mới:'
OR title ~ '^Đã nhập kho(?::| )'
OR title ~ '^Cập nhật WO-[^:]+: .+ (→|->) .+$';
