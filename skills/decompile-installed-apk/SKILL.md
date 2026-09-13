---
name: decompile-installed-apk
description: Lấy và dịch ngược ứng dụng Android đã cài trên thiết bị kết nối qua ADB khi người dùng cung cấp package ID. Dùng để lấy APK gốc cùng các split APK và tạo mã nguồn/tài nguyên bằng JADX; chỉ tạo smali khi được yêu cầu rõ. Không dùng để vá, ký, cài APK, trích xuất dữ liệu riêng của ứng dụng hoặc kiểm tra bảo mật.
---

# Dịch ngược APK đã cài đặt

Dùng script đi kèm để thực hiện nhất quán toàn bộ quy trình chọn thiết bị, lấy APK, dịch ngược, ghi log và tạo báo cáo.

## Chạy quy trình

1. Xem app ID được cung cấp là package ID Android. Không thay bằng tên hiển thị của ứng dụng.
2. Xác định thư mục của skill, sau đó chạy script từ thư mục làm việc hiện tại của người dùng để đầu ra mặc định được tạo tại đó:

   ```bash
   python3 <thư-mục-skill>/scripts/decompile_installed_apk.py <package-id>
   ```

3. Chỉ thêm `--smali` khi người dùng yêu cầu rõ smali, mã hợp ngữ Dalvik hoặc đầu ra Apktool.
4. Thêm `--serial <adb-serial>` khi người dùng đã cung cấp serial thiết bị. Nếu có nhiều thiết bị khả dụng nhưng chưa có serial, để script dừng rồi hỏi người dùng chọn thiết bị; không tự đoán.
5. Chỉ dùng `--output-root <thư-mục>` khi người dùng yêu cầu vị trí đầu ra khác.
6. Đọc `REPORT.md` hoặc `report.json` đã tạo và báo lại thư mục phiên chạy, số APK đã lấy, kết quả JADX, kết quả smali nếu có và mọi cảnh báo. Khi phù hợp, giải thích rằng mã Java dịch ngược chỉ là bản xấp xỉ.

Script chỉ đọc đường dẫn package và lấy các tệp APK từ thiết bị. Không nâng quyền root, vượt qua cơ chế kiểm soát truy cập của Android, vá ứng dụng hoặc trích xuất dữ liệu riêng của ứng dụng. Yêu cầu dịch ngược trực tiếp cho phép tạo các tệp đầu ra cục bộ của phiên chạy đó, nhưng không cho phép tự cài phần mềm còn thiếu.

## Khi thiếu công cụ

Nếu script báo thiếu dependency, đọc [references/dependencies.md](references/dependencies.md) và cung cấp lệnh cài đặt hoặc liên kết chính thức phù hợp. Không tải xuống hoặc cài đặt bất kỳ thứ gì nếu người dùng chưa cho phép riêng. Apktool là tùy chọn, trừ khi có yêu cầu `--smali`.
