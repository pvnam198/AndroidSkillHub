package com.namstd.androidskillhub.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.namstd.androidskillhub.core.ads.AdHandle
import com.namstd.androidskillhub.core.ads.BannerAds
import com.namstd.androidskillhub.core.ads.BannerSlotHandle
import com.namstd.androidskillhub.core.ads.NativeAds
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.config.RemoteConfig
import com.namstd.androidskillhub.core.preferences.AppPreferences

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    private var bannerHandle: BannerSlotHandle? = null
    private var nativeHandle: AdHandle? = null
    private var bannerContainer: ViewGroup? = null
    private var nativeContainer: FrameLayout? = null

    protected val binding: VB
        get() = checkNotNull(_binding) {
            "ViewBinding is only available between onCreateView and onDestroyView"
        }

    protected val prefs: AppPreferences get() = AppPreferences.getInstance(requireContext())

    /**
     * Loads a banner into [container] and destroys it automatically when the view is torn down.
     * [autoReload] / [reloadGapMs] set its reload timer and default to Remote Config's
     * ([RemoteConfig.bannerSlotAutoReload] / [RemoteConfig.bannerSlotReloadGapMs]); a screen can
     * pass its own. Any other reload trigger (resume, tab switch...) is the screen's own code
     * calling [reloadBanner]. For a premium user nothing loads and [container] is hidden.
     */
    protected fun loadBanner(
        container: ViewGroup,
        collapsible: Boolean = false,
        autoReload: Boolean = RemoteConfig.bannerSlotAutoReload,
        reloadGapMs: Long = RemoteConfig.bannerSlotReloadGapMs,
    ) {
        bannerContainer = container
        if (isPremiumUser()) {
            container.isVisible = false
            return
        }
        container.post {
            if (_binding != null && !isPremiumUser()) {
                bannerHandle = BannerAds.load(requireActivity(), container, collapsible, autoReload, reloadGapMs)
            }
        }
    }

    /** Reloads the banner slot now (e.g. on a tab switch) - ignored while it's already loading. */
    protected fun reloadBanner() {
        bannerHandle?.reload()
    }

    /**
     * Loads a native ad for [placement] into [container] and destroys it automatically when the
     * view is torn down. See [NativeAds.subscribe] for [preloadOnShow]. For a premium user nothing
     * loads and [container] is hidden.
     */
    protected fun loadNative(container: FrameLayout, placement: NativePlacement, preloadOnShow: Boolean = false) {
        nativeContainer = container
        if (isPremiumUser()) {
            container.isVisible = false
            return
        }
        container.post {
            if (_binding != null && !isPremiumUser()) {
                nativeHandle = NativeAds.subscribe(requireActivity(), placement, container, preloadOnShow)
            }
        }
    }

    /**
     * Runs on every onResume with whether the user is premium - like the reference app's base - so
     * a screen that was open or in the back stack when the user upgraded drops its ads as soon as
     * they come back to it. The default tears down this screen's [loadBanner] / [loadNative] ads and
     * hides their containers; a screen with ads of its own (a feed, a dialog...) overrides this to
     * hide those too, calling super.
     */
    protected open fun onPremiumUser(premium: Boolean) {
        if (premium) hideAds()
    }

    protected fun isPremiumUser(): Boolean = prefs.isPremium

    private fun hideAds() {
        bannerHandle?.destroy()
        bannerHandle = null
        nativeHandle?.destroy()
        nativeHandle = null
        bannerContainer?.isVisible = false
        nativeContainer?.isVisible = false
    }

    /** Delegates to the hosting [BaseActivity] - single implementation for the whole app, see there. */
    protected fun showInterstitial(onComplete: () -> Unit) {
        (requireActivity() as BaseActivity<*>).showInterstitial(onComplete)
    }

    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    protected open fun initConfig(savedInstanceState: Bundle?) = Unit

    protected open fun initViews() = Unit

    protected open fun initListeners() = Unit

    protected open fun observeData() = Unit

    protected open fun releaseResources() = Unit

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initConfig(savedInstanceState)
        initViews()
        initListeners()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        onPremiumUser(isPremiumUser())
    }

    final override fun onDestroyView() {
        try {
            releaseResources()
        } finally {
            bannerHandle?.destroy()
            bannerHandle = null
            nativeHandle?.destroy()
            nativeHandle = null
            bannerContainer = null
            nativeContainer = null
            _binding = null
            super.onDestroyView()
        }
    }
}
