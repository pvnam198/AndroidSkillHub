package com.namstd.androidskillhub.feature.premium

import android.view.LayoutInflater
import android.view.ViewGroup
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.ads.Ads
import com.namstd.androidskillhub.core.ui.base.BaseFragment
import com.namstd.androidskillhub.databinding.FragmentPremiumBinding

class PremiumFragment : BaseFragment<FragmentPremiumBinding>() {
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPremiumBinding.inflate(inflater, container, false)

    override fun initViews() = render()

    override fun initListeners() {
        binding.upgradeButton.setOnClickListener {
            prefs.isPremium = true
            Ads.adsEnabled = false
            render()
        }
    }

    private fun render() {
        binding.premiumStatus.setText(if (prefs.isPremium) R.string.premium_active else R.string.premium_inactive)
        binding.upgradeButton.isEnabled = !prefs.isPremium
    }
}
