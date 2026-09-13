# IAA UX

Đọc tài liệu này khi sản phẩm dùng quảng cáo hoặc mô hình doanh thu chưa được chốt.

## Mục tiêu

Cân bằng UX, retention, engagement, ad opportunities và doanh thu dài hạn. Không tối ưu impression ngắn hạn bằng cách phá flow hoặc niềm tin.

Đánh giá Banner, Adaptive Banner, Collapsible Banner, Native Ad, Interstitial và Rewarded Ad. Chọn format theo bối cảnh, không vì format đó tồn tại. Xác định natural break cho interstitial và giá trị trao đổi rõ ràng cho rewarded.

## Placement

Không dùng dark pattern, fake CTA, fake system UI, deceptive styling hoặc placement tạo accidental click. Quảng cáo không được bị nhầm với Continue, Apply, Download, Save, Close, Back, navigation hay chức năng app; không che nội dung quan trọng, phá hierarchy hoặc làm primary action khó dùng.

Native Ad có thể hòa vào content flow nhưng phải nhận diện rõ là quảng cáo. Template phải chịu được creative đa dạng và dành chỗ phù hợp cho ad label/attribution, advertiser hoặc source, app icon, headline, media và CTA.

## Ad state contract

Với mỗi screen có quảng cáo, xác định:

- `FORMAT` và kích thước/biến thể;
- `PLACEMENT` trong layout;
- `LOAD/PRELOAD`;
- `DISPLAY` condition;
- `RELOAD` trigger;
- `FREQUENCY/COOLDOWN` nếu liên quan;
- `LOADING` UI;
- `FAILED/NO FILL` UI;
- `PREMIUM/REMOVE ADS` UI;
- layout khi ad có, đang tải, lỗi và bị loại bỏ.

Dùng reserved space khi phù hợp để tránh layout jump, nhưng không để khoảng trống vô nghĩa kéo dài sau fail. Nêu rõ chính sách collapse hoặc reclaim space.

## IAA components trong Design System

Chỉ đưa vào format dùng thực tế: banner container, adaptive/collapsible banner, native ad small/medium/large, rewarded entry, ad loading, ad failed/unavailable, reserved ad space và premium/remove-ads variant.

Các component tuân visual language chung nhưng vẫn phân biệt quảng cáo với nội dung organic.
