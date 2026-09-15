package com.namstd.androidskillhub.feature.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.namstd.androidskillhub.core.ads.AppOpenAds
import com.namstd.androidskillhub.core.ads.NativeAds
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.ui.base.BaseActivity
import com.namstd.androidskillhub.databinding.ActivityMainBinding
import com.namstd.androidskillhub.feature.settings.SettingsActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater)

    override fun initConfig(savedInstanceState: Bundle?) {
        NativeAds.preload(NativePlacement.HOME_FEED)
    }

    override fun initViews() {
        AppOpenAds.load()
        loadBanner(binding.bannerContainer, collapsible = true)
        binding.mainFeedPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = TAB_COUNT
            override fun createFragment(position: Int) = MainFeedPageFragment.newInstance(position)
        }
        TabLayoutMediator(binding.mainFeedTabs, binding.mainFeedPager) { tab, position ->
            tab.text = "Tab ${position + 1}"
        }.attach()
    }

    override fun initListeners() {
        binding.ivSetting.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun releaseResources() {
        binding.mainFeedPager.adapter = null
    }

    private companion object {
        const val TAB_COUNT = 3
    }
}
