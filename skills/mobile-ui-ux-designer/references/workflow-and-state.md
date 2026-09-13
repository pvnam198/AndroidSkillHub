# Quy trình dự án và trạng thái

Đọc tài liệu này khi bắt đầu hoặc tiếp tục một dự án UI/UX nhiều bước.

## Intake tối thiểu

Tổng hợp dữ liệu đã có trước, chỉ hỏi phần còn thiếu có ảnh hưởng lớn:

- sản phẩm/feature giải quyết vấn đề gì;
- người dùng mục tiêu và bối cảnh sử dụng;
- platform: iOS, Android hay cả hai;
- chức năng cốt lõi và primary outcome;
- User Flow hoặc entry/exit quan trọng;
- mô hình doanh thu: IAA, premium, hybrid hay chưa xác định;
- reference và điều người dùng thích/không thích ở reference;
- Design System hoặc brand constraints hiện có;
- navigation dự kiến;
- phạm vi đầu ra: tài liệu, ảnh, hay cả hai;
- đường dẫn/thư mục project nếu cần ghi artifact.

Không biến intake thành bảng hỏi dài. Gộp tối đa các câu liên quan trong một lượt, ưu tiên câu có thể làm đổi kiến trúc. Nếu người dùng chỉ muốn thiết kế một feature trong project đã có, không khởi động lại toàn bộ quy trình.

## Checkpoint từng bước

Mỗi checkpoint gồm bốn ý ngắn:

1. Trạng thái: thông tin nào đã đủ và điều gì đang được coi là nguồn sự thật.
2. Bước tiếp theo: chỉ một bước.
3. Đầu ra: file/ảnh cụ thể sẽ tạo hoặc sửa.
4. Xác nhận: hỏi có bắt đầu bước đó không.

Sau khi người dùng đồng ý, tạo đúng đầu ra đã nêu. Nếu trong khi làm phát hiện một quyết định lớn chưa được chốt, dừng ở biên an toàn, giải thích tác động và hỏi. Không dùng checkpoint cho tiểu tiết có thể ghi `ĐỀ XUẤT`.

## Trạng thái dự án

Với dự án có file trong workspace, duy trì `docs/ui-ux/project-status.md` (hoặc vị trí người dùng chọn) để lần sau không phải hỏi lại. Nội dung tối thiểu:

- project và phạm vi hiện tại;
- phase hiện tại;
- artifact đã hoàn thành và đường dẫn;
- quyết định `ĐÃ XÁC NHẬN`;
- giả định/`ĐỀ XUẤT` đang dùng;
- `CHƯA XÁC ĐỊNH` và blocker;
- bước kế tiếp đã đề xuất;
- ngày cập nhật.

Chỉ đánh dấu artifact `Approved` khi người dùng thực sự xác nhận. Các trạng thái phù hợp: `Draft`, `Review`, `Approved`, `Superseded`.

## Điều chỉnh thứ tự

Thứ tự mặc định là flow → IAA → visual direction → Design System → screens → audit → handoff. Có thể điều chỉnh khi project đã có artifact được duyệt, người dùng chỉ yêu cầu một màn hình/audit, cần prototype để kiểm chứng direction, monetization không áp dụng hoặc người dùng duyệt làm theo batch.

Nếu prototype trước Design System, tạo “minimum viable Design System” cho screen đó và nói rõ đây chưa phải hệ thống hoàn chỉnh.

## Cách tiếp tục project cũ

1. Đọc project status và artifact đã liên kết.
2. Xác định artifact nào đang là bản hiện hành.
3. Tóm tắt hiểu biết hiện tại, không bắt người dùng kể lại.
4. Chỉ hỏi khi có mâu thuẫn hoặc thiếu quyết định lớn.
5. Tiếp tục từ bước kế tiếp chưa hoàn tất.
