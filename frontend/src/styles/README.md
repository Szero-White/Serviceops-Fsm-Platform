# Cấu trúc style của frontend

`main.tsx` import các module style theo thứ tự cascade.

- `app/base.css`: reset, màu chữ gốc và nền ứng dụng.
- `app/layout.css`: app shell, sidebar, header, user menu, notification và route fallback.
- `app/components.css`: entrypoint import cho shared component styles.
- `app/components/`: spacing, page header, card, control, table, status/cell và form.
- `app/dashboard.css`: style riêng của dashboard.
- `app/login.css`: màn hình đăng nhập và demo entry.
- `app/responsive.css`: responsive override.

Landing page giữ style riêng trong `pages/landing/styles` và namespace `lp-`.

Quy ước bảo trì:

- `app/components.css` chỉ chứa import; shared UI style đặt trong file tương ứng dưới `app/components/`.
- Style riêng của page đặt trong module style của page đó.
- Không thêm page-specific CSS trực tiếp vào `main.tsx`.
- Xóa selector khi component không còn tham chiếu, trừ selector cần thiết để override Ant Design dưới wrapper đang được sử dụng.
- Chỉ thêm comment khi cần giải thích một ràng buộc layout không hiển nhiên.

## Typography

Authenticated UI dùng các typography token khai báo tại `app/base.css`. Không tự tạo kích thước trung gian như `11.5px`, `12.75px` hoặc `13.5px`.

- `caption` / `meta`: 11px.
- `label` / `body`: 12px.
- `body-lg` / `section-title`: 13px.
- `panel-title`: 15px.
- `auth-title` / `metric`: 20px.
- `page-title`: 22px.

`npm run lint` chạy thêm `scripts/check-ui-typography.mjs` để chặn fractional font size, font weight từ 700 trở lên và forced uppercase trong authenticated UI. Icon size dùng các token `--app-icon-*` để tránh typography/style drift giữa các page.
