package com.namstd.androidskillhub.feature.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.ui.base.BaseFragment
import com.namstd.androidskillhub.databinding.FragmentOnboardingPageBinding

class OnboardingPageFragment : BaseFragment<FragmentOnboardingPageBinding>() {
    private val page get() = requireArguments().getInt(ARG_PAGE)

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentOnboardingPageBinding.inflate(inflater, container, false)

    override fun initViews() {
        val titles = intArrayOf(R.string.onboarding_title_1, R.string.onboarding_title_2, R.string.onboarding_title_3)
        val bodies = intArrayOf(R.string.onboarding_body_1, R.string.onboarding_body_2, R.string.onboarding_body_3)
        val placements = listOf(NativePlacement.ONBOARDING_1, NativePlacement.ONBOARDING_2, NativePlacement.ONBOARDING_3)
        binding.pageTitle.setText(titles[page])
        binding.pageBody.setText(bodies[page])
        loadNative(binding.nativeContainer, placements[page])
    }

    companion object {
        private const val ARG_PAGE = "page"
        fun newInstance(page: Int) = OnboardingPageFragment().apply { arguments = Bundle().apply { putInt(ARG_PAGE, page) } }
    }
}
