package com.namstd.androidskillhub.feature.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.ui.base.BaseFragment
import com.namstd.androidskillhub.databinding.FragmentOnboardingAdBinding

/** A dedicated onboarding page whose entire content is a native ad. */
class OnboardingAdFragment : BaseFragment<FragmentOnboardingAdBinding>() {
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentOnboardingAdBinding.inflate(inflater, container, false)

    override fun initViews() {
        loadNative(binding.nativeContainer, NativePlacement.ONBOARDING_AD)
    }
}
