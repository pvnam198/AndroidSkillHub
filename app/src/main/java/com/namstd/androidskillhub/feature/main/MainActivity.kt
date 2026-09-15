package com.namstd.androidskillhub.feature.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.namstd.androidskillhub.core.ads.AppOpenAds
import com.namstd.androidskillhub.core.ads.NativeAds
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.ui.base.BaseActivity
import com.namstd.androidskillhub.databinding.ActivityMainBinding
import com.namstd.androidskillhub.feature.settings.SettingsActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var feedAdapter: MainFeedAdapter? = null

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater)

    override fun initConfig(savedInstanceState: Bundle?) {
        NativeAds.preload(NativePlacement.HOME_FEED)
    }

    override fun initViews() {
        AppOpenAds.load()
        loadBanner(binding.bannerContainer, collapsible = true)
        val adapter = MainFeedAdapter(this, (1..60).map { "Item $it" })
        feedAdapter = adapter
        binding.mainFeedList.layoutManager = LinearLayoutManager(this)
        binding.mainFeedList.adapter = adapter
    }

    override fun initListeners() {
        binding.ivSetting.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun releaseResources() {
        binding.mainFeedList.adapter = null
        feedAdapter?.release()
        feedAdapter = null
    }
}
