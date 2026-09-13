package com.namstd.androidskillhub.feature.onboarding

import android.content.Intent
import android.view.LayoutInflater
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.ui.base.BaseActivity
import com.namstd.androidskillhub.databinding.ActivityOnboardingBinding
import com.namstd.androidskillhub.feature.main.MainActivity

class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {
    override fun inflateBinding(inflater: LayoutInflater): ActivityOnboardingBinding =
        ActivityOnboardingBinding.inflate(inflater)

    override fun initViews() {
        binding.onboardingPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = PAGE_COUNT
            override fun createFragment(position: Int) =
                if (position == AD_PAGE) OnboardingAdFragment() else OnboardingPageFragment.newInstance(contentIndex(position))
        }
        loadBanner(binding.bannerContainer)
    }

    override fun initListeners() {
        binding.onboardingPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.pageIndicator.text = "${position + 1} / $PAGE_COUNT"
                binding.nextButton.setText(if (position == PAGE_COUNT - 1) R.string.get_started else R.string.next)
            }
        })
        binding.nextButton.setOnClickListener {
            if (binding.onboardingPager.currentItem < PAGE_COUNT - 1) binding.onboardingPager.currentItem += 1 else finishSetup()
        }
    }

    /** Maps a page position to its content index, skipping the full-ad page at [AD_PAGE]. */
    private fun contentIndex(position: Int) = if (position > AD_PAGE) position - 1 else position

    private fun finishSetup() {
        prefs.setupCompleted = true
        showInterstitial {
            if (!isFinishing) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    override fun releaseResources() {
        binding.onboardingPager.adapter = null
    }

    companion object {
        private const val PAGE_COUNT = 4
        private const val AD_PAGE = 2
    }
}
