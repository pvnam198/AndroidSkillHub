package com.namstd.androidskillhub.feature.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.ads.Ads
import com.namstd.androidskillhub.core.language.AppLanguage
import com.namstd.androidskillhub.core.ui.base.BaseFragment
import com.namstd.androidskillhub.databinding.FragmentSettingsBinding
import com.namstd.androidskillhub.feature.main.MainActivity
import com.namstd.androidskillhub.feature.premium.PremiumFragment

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSettingsBinding.inflate(inflater, container, false)

    override fun initViews() {
        binding.privacyButton.isVisible = Ads.isPrivacyOptionsRequired
        binding.bannerContainer.isVisible = !prefs.isPremium
        if (!prefs.isPremium) loadBanner(binding.bannerContainer)
    }

    override fun initListeners() {
        fun language(language: AppLanguage) { prefs.selectedLanguage = language.code; prefs.applyLocale(language.code) }
        binding.englishButton.setOnClickListener { language(AppLanguage.ENGLISH) }
        binding.vietnameseButton.setOnClickListener { language(AppLanguage.VIETNAMESE) }
        binding.privacyButton.setOnClickListener { Ads.showPrivacyOptions(requireActivity()) }
        binding.resetOnboardingButton.setOnClickListener {
            prefs.resetOnboarding()
            Toast.makeText(requireContext(), R.string.onboarding_reset, Toast.LENGTH_SHORT).show()
        }
        binding.premiumButton.setOnClickListener { (requireActivity() as MainActivity).navigateTo(PremiumFragment()) }
    }
}
