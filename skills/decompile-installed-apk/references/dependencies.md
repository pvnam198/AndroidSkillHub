# Các dependency

Quy trình không bao giờ tự động cài dependency. Hãy chạy lại sau khi lệnh còn thiếu đã có trong `PATH`.

## ADB

Cài Android SDK Platform Tools từ gói chính thức dành cho nhà phát triển Android:

- https://developer.android.com/tools/releases/platform-tools

Bật Tùy chọn nhà phát triển và Gỡ lỗi USB trên thiết bị, chấp nhận yêu cầu cấp quyền rồi kiểm tra bằng `adb devices -l`.

## JADX

Cài công cụ dòng lệnh JADX và môi trường chạy Java 64-bit được hỗ trợ:

- https://github.com/skylot/jadx/releases

Xác nhận cài đặt bằng `jadx --version`. Mọi phiên chạy đều cần JADX.

## Apktool

Chỉ cần Apktool khi dùng `--smali`:

- https://apktool.org/docs/install/

Xác nhận cài đặt bằng `apktool --version`. Quy trình chạy riêng `apktool d --no-res` cho từng APK đã lấy để các split chỉ chứa tài nguyên và split tính năng không ghi đè lên nhau.
