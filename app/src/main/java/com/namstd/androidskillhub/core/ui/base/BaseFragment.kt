package com.namstd.androidskillhub.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.namstd.androidskillhub.core.ads.AdHandle
import com.namstd.androidskillhub.core.ads.BannerAds
import com.namstd.androidskillhub.core.ads.NativeAds
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.preferences.AppPreferences

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    private var bannerHandle: AdHandle? = null
    private var nativeHandle: AdHandle? = null

    protected val binding: VB
        get() = checkNotNull(_binding) {
            "ViewBinding is only available between onCreateView and onDestroyView"
        }

    protected val prefs: AppPreferences get() = AppPreferences.getInstance(requireContext())

    /** Loads a banner into [container] and destroys it automatically when the view is torn down. */
    protected fun loadBanner(container: ViewGroup, collapsible: Boolean = false) {
        container.post { if (_binding != null) bannerHandle = BannerAds.load(requireActivity(), container, collapsible) }
    }

    /**
     * Loads a native ad for [placement] into [container] and destroys it automatically when the
     * view is torn down. See [NativeAds.subscribe] for [preloadOnShow].
     */
    protected fun loadNative(container: FrameLayout, placement: NativePlacement, preloadOnShow: Boolean = false) {
        container.post {
            if (_binding != null) nativeHandle = NativeAds.subscribe(requireActivity(), placement, container, preloadOnShow)
        }
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

    final override fun onDestroyView() {
        try {
            releaseResources()
        } finally {
            bannerHandle?.destroy()
            bannerHandle = null
            nativeHandle?.destroy()
            nativeHandle = null
            _binding = null
            super.onDestroyView()
        }
    }
}
