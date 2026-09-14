package com.namstd.androidskillhub.feature.premium

import android.view.LayoutInflater
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.ads.Ads
import com.namstd.androidskillhub.core.ui.base.BaseActivity
import com.namstd.androidskillhub.databinding.ActivityPremiumBinding

class PremiumActivity : BaseActivity<ActivityPremiumBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityPremiumBinding =
        ActivityPremiumBinding.inflate(inflater)

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
