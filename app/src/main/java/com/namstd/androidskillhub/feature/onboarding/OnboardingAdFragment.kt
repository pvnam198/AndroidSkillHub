package com.namstd.androidskillhub.feature.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.config.RemoteConfig
import com.namstd.androidskillhub.core.ui.base.BaseFragment
import com.namstd.androidskillhub.databinding.FragmentOnboardingAdBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A dedicated onboarding page whose entire content is a native ad. */
class OnboardingAdFragment : BaseFragment<FragmentOnboardingAdBinding>() {
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentOnboardingAdBinding.inflate(inflater, container, false)

    override fun initViews() {
        loadNative(binding.nativeContainer, NativePlacement.ONBOARDING_AD)

        val setup = RemoteConfig.nativeIntroUseIconNext
        val control: View = if (setup.nextStyle == 2) binding.adClose else binding.adNext
        if (setup.loading) {
            binding.adLoading.isVisible = true
            viewLifecycleOwner.lifecycleScope.launch {
                delay(RemoteConfig.waitTimeMs)
                binding.adLoading.isVisible = false
                control.isVisible = true
            }
        } else {
            control.isVisible = true
        }
    }

    override fun initListeners() {
        val advance = { (requireActivity() as OnboardingActivity).advanceOnboarding() }
        binding.adNext.setOnClickListener { advance() }
        binding.adClose.setOnClickListener { advance() }
    }
}
