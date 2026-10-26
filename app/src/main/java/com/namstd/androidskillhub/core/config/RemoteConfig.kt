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

/** Giá trị đã parse của key `inter_delay_time`: gap giữa các interstitial (ms) + các loại inter được miễn gap. */
data class InterDelayConfig(val gapMs: Long, val ignore: Set<String>)

/** Giá trị đã parse của key `language_setup` (`next=1,loading=true`). nextStyle: 1=nút chữ, 2=icon. */
data class LanguageSetup(val nextStyle: Int, val loading: Boolean)

/**
 * Giá trị đã parse của key `intro1_setup` (`next=1,ds=1,delay=0,dot=1`).
 * nextStyle: 1=nút chữ, 2=icon. indicatorPosition: 1=indicator trên nút Next, 0=dưới. delayMs: chờ trước khi hiện Next. showDots: hiện chấm tròn.
 */
data class Intro1Setup(val nextStyle: Int, val indicatorPosition: Int, val delayMs: Long, val showDots: Boolean)

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
                KEY_REFRESH_RATE to
                    prefs.getCachedLong(KEY_REFRESH_RATE, 0L),
                KEY_INTER_DELAY_TIME to prefs.getCachedString(KEY_INTER_DELAY_TIME, DEFAULT_INTER_DELAY_TIME),
                KEY_OPEN_OPEN_GAP_SECONDS to prefs.getCachedLong(KEY_OPEN_OPEN_GAP_SECONDS, 0L),
                KEY_INTER_OPEN_GAP_SECONDS to prefs.getCachedLong(KEY_INTER_OPEN_GAP_SECONDS, 0L),
                KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT to prefs.getCachedLong(KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT, 2L),
                KEY_WAIT_TIME to prefs.getCachedLong(KEY_WAIT_TIME, 5L),
                KEY_INTRO_SWIPE to prefs.getCachedBoolean(KEY_INTRO_SWIPE, true),
                KEY_LANGUAGE_DELAY_MILLIS to prefs.getCachedLong(KEY_LANGUAGE_DELAY_MILLIS, 800L),
                KEY_STEP_COUNT to prefs.getCachedLong(KEY_STEP_COUNT, 1L),
                KEY_LANGUAGE_SETUP to prefs.getCachedString(KEY_LANGUAGE_SETUP, DEFAULT_LANGUAGE_SETUP),
                KEY_INTRO1_SETUP to prefs.getCachedString(KEY_INTRO1_SETUP, DEFAULT_INTRO1_SETUP),
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
        prefs.putCachedLong(KEY_REFRESH_RATE, refreshRateMs / 1000)
        prefs.putCachedString(KEY_INTER_DELAY_TIME, remoteConfig.getString(KEY_INTER_DELAY_TIME))
        prefs.putCachedLong(KEY_OPEN_OPEN_GAP_SECONDS, openOpenGapMs / 1000)
        prefs.putCachedLong(KEY_INTER_OPEN_GAP_SECONDS, interOpenGapMs / 1000)
        prefs.putCachedLong(KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT, postInterstitialNativeAdCount.toLong())
        prefs.putCachedLong(KEY_WAIT_TIME, waitTimeMs / 1000)
        prefs.putCachedBoolean(KEY_INTRO_SWIPE, introSwipe)
        prefs.putCachedLong(KEY_LANGUAGE_DELAY_MILLIS, languageDelayMillis)
        prefs.putCachedLong(KEY_STEP_COUNT, stepCount.toLong())
        prefs.putCachedString(KEY_LANGUAGE_SETUP, remoteConfig.getString(KEY_LANGUAGE_SETUP))
        prefs.putCachedString(KEY_INTRO1_SETUP, remoteConfig.getString(KEY_INTRO1_SETUP))
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
     * thay nó vào - có tự reload mỗi [refreshRateMs] khi màn đang hiển thị hay không. Không ảnh hưởng
     * tới native ad khác trong app (feed, onboarding, post-interstitial).
     */
    val bannerSlotAutoReload: Boolean
        get() = remoteConfig.getBoolean(KEY_BANNER_SLOT_AUTO_RELOAD)

    /**
     * Timer reload của banner slot: thời gian từ lúc ad được hiển thị (hoặc load lỗi) tới lần reload kế tiếp -
     * xem [bannerSlotAutoReload]. Mặc định 0 = không có timer. Màn nào reload bằng tay thì bỏ qua giá trị này.
     */
    val refreshRateMs: Long
        get() = remoteConfig.getLong(KEY_REFRESH_RATE).coerceAtLeast(0L) * 1000

    /**
     * Cấu hình gap giữa các Interstitial + danh sách loại inter được MIỄN gap đó.
     * Giá trị config dạng chuỗi `time=10,ignore=ads_splash|inter_language|inter_intro`:
     * `time` = gap giữa 2 inter (giây; getter đổi ra ms), `ignore` = các
     * [com.namstd.androidskillhub.core.ads.InterstitialPlacement.key] luôn được hiện bất kể gap.
     * Thay cho key cũ `inter_inter_gap_seconds`. Xem [com.namstd.androidskillhub.core.ads.AdGapGate].
     */
    val interDelay: InterDelayConfig
        get() {
            var timeSeconds = 0L
            var ignore = emptySet<String>()
            remoteConfig.getString(KEY_INTER_DELAY_TIME).split(",").forEach { part ->
                val pair = part.split("=", limit = 2)
                val value = pair.getOrElse(1) { "" }.trim()
                when (pair[0].trim()) {
                    "time" -> timeSeconds = value.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                    "ignore" -> ignore = value.split("|").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                }
            }
            return InterDelayConfig(gapMs = timeSeconds * 1000, ignore = ignore)
        }

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
     * Thời gian chờ (mili-giây) trước khi nút xác nhận ở màn chọn ngôn ngữ
     * ([com.namstd.androidskillhub.feature.language.LanguageActivity]) hiện ra. `0` = hiện ngay.
     * Giá trị config đã tính bằng ms (không nhân thêm).
     */
    val languageDelayMillis: Long
        get() = remoteConfig.getLong(KEY_LANGUAGE_DELAY_MILLIS).coerceAtLeast(0L)

    /**
     * Tần suất hiển thị interstitial theo số lần yêu cầu: chỉ hiện ở lần yêu cầu chia hết cho giá trị này
     * (`1` = mọi lần; `2` = cứ 2 lần hiện 1 lần...). Bộ đếm lưu qua các lần mở app
     * ([com.namstd.androidskillhub.core.preferences.AppPreferences.interstitialStepCounter]). Clamp tối thiểu 1.
     */
    val stepCount: Int
        get() = remoteConfig.getLong(KEY_STEP_COUNT).toInt().coerceAtLeast(1)

    /**
     * Màn Onboarding ([com.namstd.androidskillhub.feature.onboarding.OnboardingActivity]) có cho phép vuốt
     * (`ViewPager2.isUserInputEnabled`) hay không. `true` (mặc định) = vuốt giữa các trang được;
     * `false` = chỉ chuyển trang bằng nút Next. Fallback mặc định là `true` để giữ đúng hành vi gốc của ViewPager2.
     */
    val introSwipe: Boolean
        get() = remoteConfig.getBoolean(KEY_INTRO_SWIPE)

    /**
     * Cấu hình nút Next màn chọn ngôn ngữ (key `language_setup`, dạng `next=1,loading=true`):
     * `next` = kiểu nút (1=nút chữ, 2=icon), `loading` = có hiện vòng loading trong lúc chờ
     * [languageDelayMillis] trước khi hiện Next hay không.
     */
    val languageSetup: LanguageSetup
        get() {
            val m = parseKeyValues(remoteConfig.getString(KEY_LANGUAGE_SETUP))
            return LanguageSetup(
                nextStyle = m["next"]?.toIntOrNull() ?: 1,
                loading = m["loading"]?.toBoolean() ?: false,
            )
        }

    /**
     * Cấu hình màn Onboarding (key `intro1_setup`, dạng `next=1,ds=1,delay=0,dot=1`):
     * `next` = kiểu nút (1=nút chữ, 2=icon), `ds` = vị trí indicator (1=trên nút Next, 0=dưới),
     * `delay` = số ms chờ trước khi hiện nút Next mỗi trang, `dot` = 1 hiện chấm tròn / 0 hiện text "x/n".
     */
    val intro1Setup: Intro1Setup
        get() {
            val m = parseKeyValues(remoteConfig.getString(KEY_INTRO1_SETUP))
            return Intro1Setup(
                nextStyle = m["next"]?.toIntOrNull() ?: 1,
                indicatorPosition = m["ds"]?.toIntOrNull() ?: 1,
                delayMs = m["delay"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                showDots = m["dot"]?.toIntOrNull() == 1,
            )
        }

    /** Tách chuỗi config dạng `k=v,k=v` (bỏ khoảng trắng thừa) thành map. */
    private fun parseKeyValues(raw: String): Map<String, String> =
        raw.split(",").mapNotNull { part ->
            val pair = part.split("=", limit = 2)
            if (pair.size == 2) pair[0].trim() to pair[1].trim() else null
        }.toMap()

    private const val KEY_USE_CUSTOM_INTERSTITIAL = "use_custom_interstitial"
    private const val KEY_USE_NATIVE_COLLAPSIBLE = "use_native_collapsible"
    private const val KEY_BANNER_SLOT_AUTO_RELOAD = "banner_slot_auto_reload"
    private const val KEY_REFRESH_RATE = "refresh_rate"
    private const val KEY_INTER_DELAY_TIME = "inter_delay_time"
    private const val DEFAULT_INTER_DELAY_TIME = "time=0,ignore="
    private const val KEY_OPEN_OPEN_GAP_SECONDS = "open_open_gap_seconds"
    private const val KEY_INTER_OPEN_GAP_SECONDS = "inter_open_gap_seconds"
    private const val KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT = "post_interstitial_native_ad_count"
    private const val KEY_WAIT_TIME = "wait_time"
    private const val KEY_INTRO_SWIPE = "intro_swipe"
    private const val KEY_LANGUAGE_DELAY_MILLIS = "language_delay_millis"
    private const val KEY_STEP_COUNT = "step_count"
    private const val KEY_LANGUAGE_SETUP = "language_setup"
    private const val DEFAULT_LANGUAGE_SETUP = "next=1,loading=false"
    private const val KEY_INTRO1_SETUP = "intro1_setup"
    private const val DEFAULT_INTRO1_SETUP = "next=1,ds=1,delay=0,dot=0"
}
