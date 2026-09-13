package com.namstd.androidskillhub.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {

    private var _binding: VB? = null

    protected val binding: VB
        get() = checkNotNull(_binding) {
            "ViewBinding is only available between onCreateView and onDestroyView"
        }

    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    protected open fun initConfig(savedInstanceState: Bundle?) = Unit

    protected open fun initViews() = Unit

    protected open fun initListeners() = Unit

    protected open fun observeData() = Unit

    protected open fun releaseResources() = Unit

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initConfig(savedInstanceState)
        initViews()
        initListeners()
        observeData()
    }

    final override fun onDestroyView() {
        try {
            releaseResources()
        } finally {
            _binding = null
            super.onDestroyView()
        }
    }
}
