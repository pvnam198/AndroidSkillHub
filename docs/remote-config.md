# Remote Config

Toàn bộ feature flag điều khiển từ xa qua **Firebase Remote Config**.

- **File nguồn (single source of truth):** [`app/src/main/java/com/namstd/androidskillhub/core/config/RemoteConfig.kt`](../app/src/main/java/com/namstd/androidskillhub/core/config/RemoteConfig.kt)
- **Khởi tạo:** `RemoteConfig.initialize(this)` gọi 1 lần trong `AndroidSkillHubApp.onCreate()`.
- **Không có file defaults XML** — defaults được seed bằng code từ cache `AppPreferences`.

---

## Cơ chế hoạt động

1. **Seed defaults từ cache** — khi `initialize()`, mỗi key được nạp giá trị **lần fetch thành công gần nhất** (lưu trong `AppPreferences`, file `app_preferences`) qua `setDefaultsAsync`. Nhờ vậy ngay khi app vừa mở (fetch còn đang chạy), đọc flag đã trả về giá trị tốt gần nhất thay vì fallback cứng.
2. **Fallback cứng** — tham số thứ 2 của `prefs.getCachedBoolean(key, <fallback>)` / `getCachedLong(...)` là giá trị dùng ở **lần mở app đầu tiên** khi chưa có gì trong cache.
3. **Fetch + activate** — `fetchAndActivate()` kéo giá trị mới từ Firebase. Khi xong, `persistToCache()` ghi tất cả giá trị hiện tại trở lại `AppPreferences` để làm seed cho lần mở sau.
4. **Readiness gate** — `RemoteConfig.isReady: StateFlow<Boolean>` lật `true` sau khi fetch hoàn tất. `SplashActivity` chờ cờ này trước khi điều hướng, nên các màn sau (Language, Onboarding, Main…) đọc flag là đã settle.
5. **Tần suất fetch** — DEBUG: `minimumFetchIntervalInSeconds = 0` (fetch mỗi lần mở, tiện test). RELEASE: `3600` giây.

### Thêm một key mới cần 4 touch-point trong `RemoteConfig.kt`

1. Khai báo `private const val KEY_... = "..."` (khối cuối file).
2. Thêm dòng seed trong `setDefaultsAsync(mapOf(...))` với fallback phù hợp.
3. Thêm dòng ghi cache trong `persistToCache()`.
4. Thêm getter `val ...` đọc `remoteConfig.getBoolean/getLong(KEY_...)`.

`AppPreferences` **không cần sửa** — dùng chung `getCachedBoolean/putCachedBoolean` và `getCachedLong/putCachedLong`.

---

## Danh sách key

### Quảng cáo — bật/tắt & kiểu hiển thị

| Key | Kiểu | Fallback | Ý nghĩa |
|---|---|---|---|
| `use_custom_interstitial` | Boolean | `false` | Khi `true`, `BaseActivity.showInterstitial` chạy interstitial + break 2 native ad sau đó (thay vì chỉ 1 inter). |
| `use_native_collapsible` | Boolean | `false` | Kiểu collapsible mà `BannerAds.load` dựng: `true` = native collapsible tự dựng (1 native, to rồi thu nhỏ — xem `NativeCollapseAd`); `false` = collapsible banner mặc định của AdMob. |

### Quảng cáo — banner slot auto reload

| Key | Kiểu | Fallback | Ý nghĩa |
|---|---|---|---|
| `banner_slot_auto_reload` | Boolean | `false` | Bật timer tự reload banner slot (ô banner của màn) sau mỗi `banner_slot_reload_gap_seconds` khi màn đang hiển thị. Không ảnh hưởng native khác (feed, onboarding, post-interstitial). |
| `banner_slot_reload_gap_seconds` | Long (giây) | `0` | Chu kỳ reload banner slot. `0` = không có timer. Màn nào reload bằng tay thì bỏ qua giá trị này. Getter nhân ×1000 ra ms. |

### Quảng cáo — khoảng cách (gap) giữa các lần hiển thị

Dùng bởi `AdGapGate`. Getter nhân ×1000 ra ms.

| Key | Kiểu | Fallback | Ý nghĩa |
|---|---|---|---|
| `inter_inter_gap_seconds` | Long (giây) | `0` | Thời gian tối thiểu giữa 2 lần Interstitial. |
| `open_open_gap_seconds` | Long (giây) | `0` | Thời gian tối thiểu giữa 2 lần App Open. |
| `inter_open_gap_seconds` | Long (giây) | `0` | Thời gian tối thiểu giữa 1 Interstitial và 1 App Open (cả 2 chiều). |

### Quảng cáo — break native sau interstitial

Dùng bởi `PostInterstitialNativeAdDialogFragment` (flow 2 stage).

| Key | Kiểu | Fallback | Ý nghĩa |
|---|---|---|---|
| `post_interstitial_native_ad_count` | Long → Int | `2` | Số native ad hiển thị liên tiếp trong break sau inter. **Clamp 1–2** (dialog chỉ hỗ trợ flow 2 stage). |
| `wait_time` | Long (giây) | `5` | Số giây đếm ngược trên màn native fullscreen sau interstitial trước khi icon X (đóng) hiện ra — cũng là thời gian mỗi stage đứng trước khi nút Next khả dụng. Getter nhân ×1000 ra ms. |

### UI luồng mở app

| Key | Kiểu | Fallback | Ý nghĩa |
|---|---|---|---|
| `intro_swipe` | Boolean | `true` | Cho phép vuốt (swipe) ở màn Onboarding (`OnboardingActivity`, `ViewPager2.isUserInputEnabled`). `true` = vuốt được (mặc định); `false` = chỉ chuyển trang bằng nút Next. |
| `language_delay_millis` | Long (**ms**) | `800` | Thời gian chờ trước khi nút xác nhận (`apply_button`) ở màn chọn ngôn ngữ (`LanguageActivity`) hiện ra. `0` = hiện ngay. **Đơn vị đã là mili-giây — getter KHÔNG nhân ×1000** (khác các key gap tính bằng giây). |

---

## Lịch sử triển khai (so với spec PDF "Remote Config")

Spec gốc (file `Remote Config.pdf`) mô tả bộ key legacy khác tên. App **không** dùng tên legacy đó. Tiến độ theo tính năng:

- [x] `intro_swipe` — cho/không cho swipe Onboarding *(26/10/2026)*
- [x] `wait_time` — đếm ngược icon X native fullscreen (sau interstitial; đổi tên từ `post_interstitial_native_ad_duration_seconds`) *(26/10/2026)*
- [x] `language_delay_millis` — delay hiện nút xác nhận màn Language *(26/10/2026)*
- [ ] `step_count` — inter hiện theo bước
- [ ] `inter_delay_time` phần `ignore` — bỏ qua gap cho một số inter
- [ ] `language_setup`, `native_intro_use_icon_next`, `intro1_setup` — config UI Language/Onboarding
- [ ] `enable` — bật/tắt + chọn loại QC theo vị trí
- [ ] `native templete` — chọn layout native động
- [ ] `collap_config` — click-bait native collap
- [ ] Tiering ID `highfloor / medium / all_price`

> Khi làm thêm key mới, cập nhật bảng "Danh sách key" và tick mục tương ứng ở đây.
