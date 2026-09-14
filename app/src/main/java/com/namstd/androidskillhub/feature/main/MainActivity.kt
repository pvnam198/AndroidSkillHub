package com.namstd.androidskillhub.feature.main

import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.ads.AppOpenAds
import com.namstd.androidskillhub.core.ui.base.BaseActivity
import com.namstd.androidskillhub.databinding.ActivityMainBinding
import com.namstd.androidskillhub.feature.premium.PremiumFragment
import com.namstd.androidskillhub.feature.settings.SettingsFragment

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater)

    override fun initViews() {
        AppOpenAds.load()
        loadBanner(binding.bannerContainer, collapsible = true)
//        loadNative(binding.nativeContainer, NativePlacement.HOME_FEED)
    }

    override fun initListeners() {
        binding.settingsButton.setOnClickListener { navigateTo(SettingsFragment()) }
        binding.premiumButton.setOnClickListener { navigateTo(PremiumFragment()) }
        binding.btnShowInter.setOnClickListener { showInterstitial {} }
    }

    /** Pushes [fragment] on top of the current one, adding it to the back stack. */
    fun navigateTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host, fragment)
            .addToBackStack(null)
            .commit()
    }
}
