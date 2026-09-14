package com.namstd.androidskillhub.feature.main

import android.content.Intent
import android.view.LayoutInflater
import com.namstd.androidskillhub.core.ads.AppOpenAds
import com.namstd.androidskillhub.core.ui.base.BaseActivity
import com.namstd.androidskillhub.databinding.ActivityMainBinding
import com.namstd.androidskillhub.feature.settings.SettingsActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater)

    override fun initViews() {
        AppOpenAds.load()
        loadBanner(binding.bannerContainer, collapsible = true)
//        loadNative(binding.nativeContainer, NativePlacement.HOME_FEED)
    }

    override fun initListeners() {
        binding.ivSetting.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }
}
