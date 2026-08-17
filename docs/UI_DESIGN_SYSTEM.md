# ServiceOps UI Design System

ServiceOps là một operations SaaS: người dùng cần đọc bảng, nhận biết ngoại lệ và thao tác trong thời gian dài. Vì vậy giao diện ưu tiên **hierarchy, scanability, density vừa phải và semantic color**, không ưu tiên hiệu ứng marketing trong khu vực authenticated.

## 1. Principles

1. **Information before decoration.** Trạng thái, deadline, ownership và action phải nổi bật hơn yếu tố trang trí.
2. **Semantic state stays recognizable.** Trạng thái nghiệp vụ cần quét nhanh dùng màu nhất quán: healthy/active = success, in-progress = info, at-risk/waiting = warning, failed/expired = danger; entity identity và metadata vẫn neutral.
3. **Regular-weight body text.** Nội dung bảng/form dùng weight 400; 500–600 chỉ dành cho hierarchy thực sự.
4. **One component language.** Page header, toolbar, table, card, form, drawer và modal dùng chung token/pattern.
5. **8px rhythm.** Khoảng cách chính dùng 8/16/24px, có 4px half-step trong control dày dữ liệu.
6. **No decorative debt.** Không thêm gradient, glow, floating animation hoặc one-off color nếu không có business purpose.

## 2. Core tokens

| Token | Value | Usage |
| --- | --- | --- |
| Primary | `#47789F` | CTA, links, focus, active interaction |
| Primary hover | `#3E6C91` | Hover |
| Primary active | `#345C7B` | Pressed/strong interaction |
| Primary soft | `#EEF5F9` | Selected/supporting surface |
| Accent | `#5D918C` | Limited secondary emphasis |
| Success | `#4B7968` | Completed/positive business state |
| Warning | `#896631` | At-risk/waiting business state |
| Danger | `#9C5050` | Destructive/error/expired state |
| App background | `#F5F7F9` | Main canvas |
| Surface | `#FFFFFF` | Cards/tables/forms |
| Primary text | `#24313D` | Titles and main values |
| Secondary text | `#536371` | Supporting copy |
| Muted text | `#71808C` | Metadata only |
| Border | `#DFE6EB` | Surface separation |
| Strong border | `#CBD6DE` | Inputs/action boundaries |

Tokens in `frontend/src/styles/app/base.css` and the Ant `ConfigProvider` in `frontend/src/main.tsx` are the implementation source of truth.

## 3. Typography

Font stack:

```text
"Segoe UI Variable Text", "Segoe UI Variable", "Segoe UI", system-ui, sans-serif
```

Authenticated ServiceOps uses a **productive type scale** with a deliberately small number of roles. Intermediate one-off values are not allowed for product text.

The authenticated app uses a **compact commercial density**: text remains efficient for data-heavy workflows, but everyday reading text is never compressed below a practical product size. Hierarchy comes from weight, spacing and semantic color rather than oversized typography.

To reduce visual noise, the authenticated product resolves semantic aliases to only **six actual text sizes: 11 / 12 / 13 / 15 / 20 / 22px**. Different component names may share the same size intentionally; hierarchy should not depend on inventing another size.

| Role | Token | Size | Weight |
| --- | --- | --- | --- |
| Caption / technical metadata | `--app-type-caption`, `--app-type-meta` | 11px | 400–500 |
| Product label / body / table / menu / control | `--app-type-label`, `--app-type-body` | 12px | 400–500 |
| Supporting copy / local section title | `--app-type-body-lg`, `--app-type-section-title` | 13px | 400 / 600 |
| Drawer / modal title | `--app-type-panel-title` | 15px | 600 |
| Login title / KPI value | `--app-type-auth-title`, `--app-type-metric` | 20px | 600 |
| Page title | `--app-type-page-title` | 22px | 600 |

Rules:

- Mỗi authenticated route chỉ có một page title 22px; không tạo heading trung gian tùy page.
- Table header 12px/600; table body 12px/400; primary identity tối đa 12px/500.
- Supporting text dài dùng 13px khi cần; timestamp/tag/code/ID dùng 11px theo vai trò.
- Drawer/modal không được tự kế thừa heading scale lớn của Ant Design; title 15px, content 12px, label 12px.
- Authenticated UI không dùng 700/800/900, không ép uppercase và không dùng fractional font size như `11.5px`, `12.75px`, `13.5px`.
- Authenticated CSS không dùng raw pixel `font-size`; text và icon đều đi qua `--app-type-*` / `--app-icon-*` tokens.
- Không dùng low-contrast gray cho nội dung cần đọc.
- Code/ID dùng mono face nhưng kích thước và nền trung tính, không biến thành điểm nhấn chính.
- Không hiển thị enum kỹ thuật (`WORK_ORDER`, `SERVICE_CHANNEL`) trực tiếp cho người dùng; UI phải dịch sang nhãn nghiệp vụ.
- `npm run lint` chạy thêm typography policy check để ngăn type scale bị drift trở lại.

Public landing dùng một expressive scale riêng (`--lp-type-*`) vì mục tiêu đọc khác product UI, nhưng vẫn chỉ có các vai trò caption/small/supporting/body/section/hero thay vì mỗi section tự chọn size.

## 4. Application shell

- Expanded sidebar: `228px`.
- Sticky header: `58px`.
- Desktop content padding: `20px 24px 32px`.
- Sidebar dùng group `Vận hành`, `Danh mục & nguồn lực`, `Quản trị` để giảm một danh sách phẳng dài. Group label luôn dùng sentence case, không ép uppercase.
- Selected route dùng neutral surface + accent rail 2px, không dùng saturated pill.
- Keyboard focus được vẽ trên toàn menu item thay vì tạo box quanh riêng text link.

## 5. Page composition

Default data-heavy page:

```text
PageHeader
  → summary/meta + primary action
Toolbar / search / filter
DataTable
Detail drawer (khi cần)
Focused create/edit/action modal
```

Không thêm summary card nếu nó chỉ lặp lại dữ liệu ngay dưới table. Dashboard bắt đầu bằng KPI cards; không có promotional hero trong authenticated area.

## 6. Tables

- Ưu tiên scan theo chiều ngang và giữ time/status/action trên một dòng.
- Header neutral, compact; body regular-weight.
- Entity identity và metadata không được tô nhiều màu; **business status thì phải giữ semantic color để quét nhanh**.
- `StatusTag`, `PriorityTag`, `WarrantyTag`, `RoleTag` và `AuditActionTag` là semantic primitives dùng chung.
- Healthy/active/available/completed dùng success soft; lifecycle đang xử lý dùng info soft; waiting/at-risk dùng warning soft; expired/cancelled/error dùng danger soft.
- Không dùng màu semantic để trang trí tên entity, role hay audit action thông thường.
- Action dùng icon button + tooltip; destructive action dùng danger state.
- Fixed-right action columns không phải default vì chúng tạo sticky-scrollbar/noise; chỉ dùng khi table thực sự rất rộng và action phải luôn nhìn thấy.
- Horizontal scrolling được giữ cho narrow viewport hoặc table nhiều cột; scrollbar phải mảnh và secondary.
- Quantity trong UI nghiệp vụ dùng grouping bằng khoảng trắng hẹp (`100 002`) và dấu phẩy cho phần thập phân (`12,5`) để tránh nhầm `100.002` là số thập phân hay phân tách hàng nghìn.

## 7. Cards and metrics

- White surface, 1px border, near-flat shadow.
- Metric card có cùng layout; màu chỉ nằm ở icon surface khi semantic value cần phân biệt.
- Không dùng gradient hoặc hover-lift cho informational card.
- Dashboard ưu tiên KPI → health/recent work, tránh duplicated summary hero.

## 8. Forms and actions

- Control height desktop: `36px`.
- Primary action dùng muted blue; secondary action dùng white + neutral border.
- Label 12px/500; input text 12px/400.
- Modal chỉ chứa một focused task. Workflow phức tạp cần component riêng thay vì nhồi thêm field vào page.


## 9. Detail surfaces

Drawer, descriptions, tabs và timeline dùng cùng productive type rhythm:

- Drawer/modal title: 15px/600; local detail heading 13px/600; mã phiếu là mono 11px/500.
- Description label/value: 12px/500 và 12px/400.
- Tab: 12px/400, active 600.
- Timeline actor/note: 12px; timestamp: 11px.
- Timeline color chỉ theo semantic state và dùng palette muted của ServiceOps.
- Detail/action button mặc định compact 32px để không cạnh tranh với dữ liệu.

## 10. Login and demo

- Login hỗ trợ authentication thật và recruiter demo.
- Role selector giữ nền/card neutral; role name rõ, description regular-weight, username là metadata.
- Public demo password là disposable presentation credential, không phải production secret.
- Hero giữ typography vừa phải; không dùng pattern/animation làm giảm contrast.

## 11. Landing page

Public landing được phép có nhiều narrative hơn product UI nhưng vẫn dùng cùng palette/font/radius. Các nguyên tắc bắt buộc:

- Không dùng fake customer logos, testimonial, pricing, conversion rate hoặc integration chưa tồn tại.
- Capability claim phải map được tới code/documentation hiện có.
- Planned capability phải được ghi rõ là roadmap, không trình bày như feature đã triển khai.
- Không dùng gradient/orb/animation chỉ để tạo cảm giác “startup template”.

## 12. Accessibility and responsive rules

- Giữ keyboard focus visible ở cấp component.
- Không truyền đạt trạng thái bằng màu duy nhất; tag luôn có text/dot.
- Không thu text để ép table vừa màn hình; dùng responsive column/overflow khi cần.
- UI phải vẫn dễ hiểu ở browser zoom 100% trên 1366/1440/1600/1920 desktop.
- Tablet/mobile ưu tiên task: stack toolbar/actions, một cột form, compact AI launcher.

## 13. Design references

Các nguyên tắc được đối chiếu với enterprise guidance, không copy UI trực tiếp:

- Ant Design — Data Display: `https://ant.design/docs/spec/data-display/`
- Ant Design — Data List: `https://ant.design/docs/spec/data-list/`
- Ant Design — Proximity: `https://ant.design/docs/spec/proximity/`
- Ant Design — Detail Page: `https://ant.design/docs/spec/detail-page/`
- Atlassian Design System — Foundations/Tokens: `https://atlassian.design/foundations/`
- Carbon Design System — Productive typography: `https://carbondesignsystem.com/elements/typography/type-sets/`
- Carbon Design System — Data table: `https://carbondesignsystem.com/components/data-table/`

ServiceOps giữ token và component ownership riêng; external systems chỉ là design input.
