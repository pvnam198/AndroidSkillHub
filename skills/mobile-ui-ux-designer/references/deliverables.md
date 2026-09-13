# Cấu trúc đầu ra và handoff

Đọc trước khi tạo artifact hoặc chuẩn bị bàn giao.

## Cấu trúc thư mục mặc định

Nếu người dùng không chỉ định vị trí, đề xuất cấu trúc sau và chỉ tạo sau khi họ duyệt bước tương ứng:

```text
docs/ui-ux/
├── project-status.md
├── product-brief.md
├── user-flow.md
├── iaa-strategy.md
├── visual-direction.md
├── design-system.md
├── screen-inventory.md
├── screens/
│   ├── <screen-name>.md
│   └── ...
├── consistency-audit.md
├── developer-handoff.md
└── assets/
    ├── design-system/
    │   ├── foundations.png
    │   ├── components.png
    │   └── iaa-components.png
    └── screens/
        ├── <screen-name>.png
        └── ...
```

Không tạo file rỗng hoặc board không áp dụng. Có thể dùng tên file hiện có của project để tránh trùng nguồn sự thật.

## `design-system.md`

Tài liệu phải đủ rõ để developer dùng mà không đoán: scope/platform/principles; token semantic kèm giá trị và cách dùng; typography; spacing/grid/safe area; shape/elevation; icon/image language; component anatomy/variant/state/behavior; accessibility; IAA component nếu áp dụng; decision log gồm confirmed, inferred, proposed và unresolved.

Giá trị token phải có đơn vị và mapping thực thi hợp lý cho platform, nhưng không viết source code trừ khi được yêu cầu.

## Ảnh Design System

Ảnh là UI specification board, không phải poster/moodboard. Mặc định:

- landscape `3:2`; dùng `16:9` nếu nội dung ít và phù hợp hơn;
- grid rõ, đọc trái sang phải và trên xuống dưới;
- nền sạch, whitespace vừa đủ, mật độ cao nhưng scan được;
- không mascot, slogan, header/footer lớn hay trang trí không phục vụ spec;
- token, tên component, mã màu, type spec, spacing và radius phải đọc được.

Chia tối đa ba board mặc định:

1. `FOUNDATIONS`: color, typography, spacing, grid, radius, elevation, icon và image direction.
2. `COMPONENTS`: controls, content, navigation, overlays, feedback và states.
3. `IAA COMPONENTS`: những ad component/state thật sự được dùng.

Chỉ thêm Board 4+ nếu nội dung cần thiết không thể đọc rõ. Tất cả board dùng cùng naming, grid, typography và visual language.

## Screen spec

Mỗi file screen ghi purpose/entry, hierarchy/layout, content, primary/secondary actions, component/token, interaction/navigation, states, accessibility, IAA state contract, responsive behavior, edge cases, điểm chưa xác định và link tới ảnh hiện hành.

## Developer handoff

`developer-handoff.md` là mục lục triển khai, không sao chép lại mọi spec. Bao gồm:

- phạm vi và platform;
- artifact hiện hành và trạng thái duyệt;
- screen/flow map;
- Design System/token source;
- component inventory và component mới;
- navigation/interaction contracts;
- UI state và ad state matrix;
- responsive/accessibility requirements;
- asset inventory;
- analytics events nếu đã được yêu cầu/xác định;
- unresolved decisions, assumption và out-of-scope;
- acceptance checklist cho coding agent/developer.

Chỉ gọi handoff “ready” khi artifact liên kết tồn tại, không mâu thuẫn và các quyết định quan trọng đã được xác nhận hoặc đánh dấu rõ.
