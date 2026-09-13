# Quy tắc thiết kế mobile

Đọc tài liệu này trước mọi công việc liên quan tới reference, Design System hoặc screen.

## Visual Direction

Thiết kế phải dựa trên loại sản phẩm, người dùng, reference, platform pattern và monetization. Trích xuất có chọn lọc từ reference: color direction, typography, spacing, density, shape language, icon style, image/illustration style, component style, hierarchy và mức độ tối giản.

Chuyển đặc điểm hữu ích thành rule/token/pattern. Không sao chép nguyên màn hình. Tránh mặc định gradient tím-xanh, glassmorphism, shadow dày, card bo lớn ở mọi nơi, icon generic, dashboard/SaaS web style, container lồng nhau và decoration vô mục đích.

## Design System

Design System là nguồn sự thật dùng chung. Chỉ đưa vào các nhóm thực sự phù hợp.

Foundations có thể gồm semantic color và contrast; typography roles; spacing scale và grid; radius; elevation/shadow; iconography; illustration/image direction.

Components có thể gồm button, input, search, selection controls, slider, chip/tag/badge, card/list item, tab, app bar, bottom navigation, dialog, bottom sheet, snackbar/toast và loading/empty/error.

Với component tương tác, chỉ định các state phù hợp: Default, Pressed, Selected, Unselected, Focused, Disabled, Loading, Error, Success. Không liệt kê state vô nghĩa.

## Mobile screen

Trước khi thiết kế, xác định:

- `PURPOSE`: màn hình giải quyết việc gì;
- `ENTRY`: người dùng đến từ đâu;
- `HIERARCHY`: thông tin ưu tiên;
- `PRIMARY ACTION` và `SECONDARY ACTION`;
- `CONTENT`;
- `NAVIGATION` sau mỗi thao tác;
- `STATES`: loading, empty, error, selected, disabled, success;
- `IAA`: format, placement và behavior nếu có.

Ưu tiên hierarchy rõ, touch target đủ lớn, text dễ đọc, thumb reach hợp lý, CTA không cạnh tranh, pattern quen thuộc, accessibility và khả năng triển khai thực tế. Dùng spacing, typography, alignment, grouping, divider và background để tạo hierarchy; chỉ dùng card/chip/badge/container khi có ý nghĩa.

Màn hình đơn dùng phone frame portrait hiện đại, tỷ lệ gần `9:19.5` đến `9:20`. Tính đủ status bar, safe area, app bar, content, bottom navigation, gesture/navigation area và ad area. Nếu nhiều screen trong một ảnh, dùng frame cùng cỡ và grid đều; chia ảnh khi UI trở nên khó đọc.

Screen phải kế thừa token và component đã chốt. Nếu thiếu component: thiết kế theo visual language hiện hành, đánh dấu `COMPONENT MỚI`, đề xuất cập nhật Design System và không tự đổi các token hệ thống khác.

## Consistency audit

Kiểm tra color, typography, spacing, radius, icon, button, navigation, card/list, ad component, content density và hierarchy. Nếu một screen lệch, ưu tiên sửa screen. Chỉ sửa Design System khi lý do có tính hệ thống và tác động đã được nêu rõ.

## Phân tích đối thủ

Phân tích structure, navigation, component, density, ad placement/format/density, native integration, banner/collapsible behavior, interstitial natural break, rewarded entry và premium behavior nếu quan sát được. Phân biệt `ĐÃ QUAN SÁT`, `SUY LUẬN`, `ĐỀ XUẤT`.
