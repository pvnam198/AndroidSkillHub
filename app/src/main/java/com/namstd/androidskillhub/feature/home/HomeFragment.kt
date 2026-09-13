package com.namstd.androidskillhub.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import com.namstd.androidskillhub.core.ads.AppOpenAds
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.ui.base.BaseFragment
import com.namstd.androidskillhub.databinding.FragmentHomeBinding
import com.namstd.androidskillhub.feature.main.MainActivity
import com.namstd.androidskillhub.feature.premium.PremiumFragment
import com.namstd.androidskillhub.feature.settings.SettingsFragment

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentHomeBinding.inflate(inflater, container, false)

    override fun initListeners() {
        binding.settingsButton.setOnClickListener {
            (requireActivity() as MainActivity).navigateTo(
                SettingsFragment()
            )
        }
        binding.premiumButton.setOnClickListener {
            (requireActivity() as MainActivity).navigateTo(
                PremiumFragment()
            )
        }

        binding.btnShowInter.setOnClickListener {
            showInterstitial {  }
        }
    }

    override fun initViews() {
        AppOpenAds.load()
        loadBanner(binding.bannerContainer, collapsible = true)
        loadNative(binding.nativeContainer, NativePlacement.HOME_FEED)
    }
}
