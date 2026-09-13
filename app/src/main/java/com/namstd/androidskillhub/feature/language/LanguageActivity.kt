package com.namstd.androidskillhub.feature.language

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.namstd.androidskillhub.core.ads.NativeAds
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.language.AppLanguage
import com.namstd.androidskillhub.core.navigation.AppRouter
import com.namstd.androidskillhub.core.navigation.StartDestination
import com.namstd.androidskillhub.core.ui.base.BaseActivity
import com.namstd.androidskillhub.databinding.ActivityLanguageBinding
import com.namstd.androidskillhub.feature.main.MainActivity
import com.namstd.androidskillhub.feature.onboarding.OnboardingActivity

class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    private lateinit var selectedLanguage: AppLanguage

    override fun inflateBinding(inflater: LayoutInflater): ActivityLanguageBinding =
        ActivityLanguageBinding.inflate(inflater)

    override fun initConfig(savedInstanceState: Bundle?) {
        selectedLanguage = AppLanguage.fromCode(prefs.selectedLanguage) ?: AppLanguage.default
        listOf(
            NativePlacement.ONBOARDING_1,
            NativePlacement.ONBOARDING_2,
            NativePlacement.ONBOARDING_3,
            NativePlacement.ONBOARDING_AD,
        ).forEach(NativeAds::preload)
    }

    override fun initViews() {
        binding.languageList.layoutManager = LinearLayoutManager(this)
        binding.languageList.adapter =
            LanguageAdapter(AppLanguage.entries, selectedLanguage) { selectedLanguage = it }
        loadNative(binding.nativeContainer, NativePlacement.LANGUAGE)
    }

    override fun initListeners() {
        binding.applyButton.setOnClickListener {
            prefs.selectedLanguage = selectedLanguage.code
            prefs.applyLocale(selectedLanguage.code)
            showInterstitial { if (!isFinishing) goToNext() }
        }
    }

    private fun goToNext() {
        val next = when (AppRouter.afterLanguage(prefs.setupCompleted)) {
            StartDestination.ONBOARDING -> OnboardingActivity::class.java
            StartDestination.HOME -> MainActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }

    override fun releaseResources() {
        binding.languageList.adapter = null
    }
}
