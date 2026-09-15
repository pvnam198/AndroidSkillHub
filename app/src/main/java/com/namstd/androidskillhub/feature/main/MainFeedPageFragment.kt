package com.namstd.androidskillhub.feature.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.namstd.androidskillhub.core.ui.base.BaseFragment
import com.namstd.androidskillhub.databinding.FragmentMainFeedPageBinding

class MainFeedPageFragment : BaseFragment<FragmentMainFeedPageBinding>() {
    private val page get() = requireArguments().getInt(ARG_PAGE)
    private var feedAdapter: MainFeedAdapter? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMainFeedPageBinding.inflate(inflater, container, false)

    override fun initViews() {
        val adapter = MainFeedAdapter(requireActivity(), (1..ITEM_COUNT).map { "Tab ${page + 1} - Item $it" })
        feedAdapter = adapter
        binding.feedList.layoutManager = LinearLayoutManager(requireContext())
        binding.feedList.adapter = adapter
    }

    override fun releaseResources() {
        binding.feedList.adapter = null
        feedAdapter?.release()
        feedAdapter = null
    }

    companion object {
        private const val ITEM_COUNT = 200
        private const val ARG_PAGE = "page"
        fun newInstance(page: Int) = MainFeedPageFragment().apply { arguments = Bundle().apply { putInt(ARG_PAGE, page) } }
    }
}
