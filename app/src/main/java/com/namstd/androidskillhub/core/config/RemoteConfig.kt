package com.namstd.androidskillhub.core.config

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.namstd.androidskillhub.BuildConfig
import com.namstd.androidskillhub.core.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Các feature flag điều khiển từ xa qua Firebase Remote Config.
 *
 * Tài liệu đầy đủ (mỗi key làm gì, kiểu dữ liệu, fallback, ví dụ giá trị trên Firebase console) nằm ở
 * `docs/remote-config.md`. Thêm một key = 4 chỗ phải sửa trong file này: một hằng `KEY_*`, một dòng seed
 * trong `setDefaultsAsync`, một dòng trong `persistToCache()`, và một getter. Giữ tài liệu đó đồng bộ với file này.
 */
object RemoteConfig {
    // Dùng getter, không phải field: Firebase tự cache instance của nó, giữ instance trong object này sẽ bị lint báo StaticFieldLeak.
    private val remoteConfig get() = Firebase.remoteConfig
    private lateinit var prefs: AppPreferences

    private val _isReady = MutableStateFlow(false)

    /** True sau khi [fetchAndActivate] hoàn tất (thành công hay thất bại) và các flag bên dưới đã settle. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /**
     * [context] dùng để seed [RemoteConfig] bằng các giá trị đã lưu (trong file [AppPreferences] dùng chung
     * của app) từ lần fetch thành công gần nhất, để các getter bên dưới trả về giá trị tốt gần nhất ngay khi
     * app vừa mở, thay vì fallback cứng, trong lúc [fetchAndActivate] còn đang chạy.
     */
    fun initialize(context: Context) {
        prefs = AppPreferences.getInstance(context)
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
            },
        )
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_USE_CUSTOM_INTERSTITIAL to prefs.getCachedBoolean(KEY_USE_CUSTOM_INTERSTITIAL, false),
                KEY_USE_NATIVE_COLLAPSIBLE to prefs.getCachedBoolean(KEY_USE_NATIVE_COLLAPSIBLE, false),
                KEY_BANNER_SLOT_AUTO_RELOAD to prefs.getCachedBoolean(KEY_BANNER_SLOT_AUTO_RELOAD, false),
                KEY_BANNER_SLOT_RELOAD_GAP_SECONDS to
                    prefs.getCachedLong(KEY_BANNER_SLOT_RELOAD_GAP_SECONDS, 0L),
                KEY_INTER_INTER_GAP_SECONDS to prefs.getCachedLong(KEY_INTER_INTER_GAP_SECONDS, 0L),
                KEY_OPEN_OPEN_GAP_SECONDS to prefs.getCachedLong(KEY_OPEN_OPEN_GAP_SECONDS, 0L),
                KEY_INTER_OPEN_GAP_SECONDS to prefs.getCachedLong(KEY_INTER_OPEN_GAP_SECONDS, 0L),
                KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT to prefs.getCachedLong(KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT, 2L),
                KEY_WAIT_TIME to prefs.getCachedLong(KEY_WAIT_TIME, 5L),
                KEY_INTRO_SWIPE to prefs.getCachedBoolean(KEY_INTRO_SWIPE, true),
            ),
        )
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            persistToCache()
            _isReady.value = true
        }
    }

    private fun persistToCache() {
        prefs.putCachedBoolean(KEY_USE_CUSTOM_INTERSTITIAL, useCustomInterstitial)
        prefs.putCachedBoolean(KEY_USE_NATIVE_COLLAPSIBLE, useNativeCollapsible)
        prefs.putCachedBoolean(KEY_BANNER_SLOT_AUTO_RELOAD, bannerSlotAutoReload)
        prefs.putCachedLong(KEY_BANNER_SLOT_RELOAD_GAP_SECONDS, bannerSlotReloadGapMs / 1000)
        prefs.putCachedLong(KEY_INTER_INTER_GAP_SECONDS, interInterGapMs / 1000)
        prefs.putCachedLong(KEY_OPEN_OPEN_GAP_SECONDS, openOpenGapMs / 1000)
        prefs.putCachedLong(KEY_INTER_OPEN_GAP_SECONDS, interOpenGapMs / 1000)
        prefs.putCachedLong(KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT, postInterstitialNativeAdCount.toLong())
        prefs.putCachedLong(KEY_WAIT_TIME, waitTimeMs / 1000)
        prefs.putCachedBoolean(KEY_INTRO_SWIPE, introSwipe)
    }

    /** Khi true, [com.namstd.androidskillhub.core.ui.base.BaseActivity.showInterstitial] sẽ chạy interstitial + break 2 native ad sau đó. */
    val useCustomInterstitial: Boolean
        get() = remoteConfig.getBoolean(KEY_USE_CUSTOM_INTERSTITIAL)

    /**
     * Kiểu collapsible mà [com.namstd.androidskillhub.core.ads.BannerAds.load] dựng: true = native collapsible
     * (1 native ad, to rồi thu nhỏ - xem [com.namstd.androidskillhub.core.ads.NativeCollapseAd]),
     * false = collapsible banner mặc định của AdMob.
     */
    val useNativeCollapsible: Boolean
        get() = remoteConfig.getBoolean(KEY_USE_NATIVE_COLLAPSIBLE)

    /**
     * Banner slot - thứ mà [com.namstd.androidskillhub.core.ads.BannerAds.load] đặt vào ô banner của một màn:
     * một AdMob banner (thường hoặc collapsible), hoặc native collapsible tự dựng khi [useNativeCollapsible]
     * thay nó vào - có tự reload mỗi [bannerSlotReloadGapMs] khi màn đang hiển thị hay không. Không ảnh hưởng
     * tới native ad khác trong app (feed, onboarding, post-interstitial).
     */
    val bannerSlotAutoReload: Boolean
        get() = remoteConfig.getBoolean(KEY_BANNER_SLOT_AUTO_RELOAD)

    /**
     * Timer reload của banner slot: thời gian từ lúc ad được hiển thị (hoặc load lỗi) tới lần reload kế tiếp -
     * xem [bannerSlotAutoReload]. Mặc định 0 = không có timer. Màn nào reload bằng tay thì bỏ qua giá trị này.
     */
    val bannerSlotReloadGapMs: Long
        get() = remoteConfig.getLong(KEY_BANNER_SLOT_RELOAD_GAP_SECONDS).coerceAtLeast(0L) * 1000

    /**
     * Thời gian tối thiểu giữa 2 lần Interstitial - một inter yêu cầu trong khoảng này sẽ bị chặn.
     * Giá trị config tính bằng giây (vd `10`); getter trả về ms. `0` = không có gap.
     * Xem [com.namstd.androidskillhub.core.ads.AdGapGate].
     */
    val interInterGapMs: Long
        get() = remoteConfig.getLong(KEY_INTER_INTER_GAP_SECONDS) * 1000

    /**
     * Thời gian tối thiểu giữa 2 lần App Open. Giá trị config tính bằng giây; getter trả về ms. `0` = không có gap.
     * Xem [com.namstd.androidskillhub.core.ads.AdGapGate].
     */
    val openOpenGapMs: Long
        get() = remoteConfig.getLong(KEY_OPEN_OPEN_GAP_SECONDS) * 1000

    /**
     * Thời gian tối thiểu giữa 1 lần Interstitial và 1 lần App Open, cả 2 chiều (tránh 2 ad full-screen
     * hiện liên tiếp khác định dạng). Giá trị config tính bằng giây; getter trả về ms. `0` = không có gap.
     */
    val interOpenGapMs: Long
        get() = remoteConfig.getLong(KEY_INTER_OPEN_GAP_SECONDS) * 1000

    /** Số native ad hiển thị liên tiếp trong break sau interstitial. Clamp 1-2 - [com.namstd.androidskillhub.feature.ads.PostInterstitialNativeAdDialogFragment] chỉ hỗ trợ flow 2 stage. */
    val postInterstitialNativeAdCount: Int
        get() = remoteConfig.getLong(KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT).toInt().coerceIn(1, 2)

    /**
     * Số giây đếm ngược trên màn native fullscreen sau interstitial
     * ([com.namstd.androidskillhub.feature.ads.PostInterstitialNativeAdDialogFragment]) trước khi
     * icon X (đóng) hiện ra - và cũng là thời gian mỗi stage đứng trước khi nút Next khả dụng.
     * Giá trị config tính bằng giây; getter trả về ms.
     */
    val waitTimeMs: Long
        get() = remoteConfig.getLong(KEY_WAIT_TIME) * 1000

    /**
     * Màn Onboarding ([com.namstd.androidskillhub.feature.onboarding.OnboardingActivity]) có cho phép vuốt
     * (`ViewPager2.isUserInputEnabled`) hay không. `true` (mặc định) = vuốt giữa các trang được;
     * `false` = chỉ chuyển trang bằng nút Next. Fallback mặc định là `true` để giữ đúng hành vi gốc của ViewPager2.
     */
    val introSwipe: Boolean
        get() = remoteConfig.getBoolean(KEY_INTRO_SWIPE)

    private const val KEY_USE_CUSTOM_INTERSTITIAL = "use_custom_interstitial"
    private const val KEY_USE_NATIVE_COLLAPSIBLE = "use_native_collapsible"
    private const val KEY_BANNER_SLOT_AUTO_RELOAD = "banner_slot_auto_reload"
    private const val KEY_BANNER_SLOT_RELOAD_GAP_SECONDS = "banner_slot_reload_gap_seconds"
    private const val KEY_INTER_INTER_GAP_SECONDS = "inter_inter_gap_seconds"
    private const val KEY_OPEN_OPEN_GAP_SECONDS = "open_open_gap_seconds"
    private const val KEY_INTER_OPEN_GAP_SECONDS = "inter_open_gap_seconds"
    private const val KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT = "post_interstitial_native_ad_count"
    private const val KEY_WAIT_TIME = "wait_time"
    private const val KEY_INTRO_SWIPE = "intro_swipe"
}
