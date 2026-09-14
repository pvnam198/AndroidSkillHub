package com.namstd.androidskillhub.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.namstd.androidskillhub.databinding.ActivityLifecycleTestBinding
import com.namstd.androidskillhub.databinding.DialogNativeFullBinding
import com.namstd.androidskillhub.databinding.FragmentPremiumBinding

class BaseUiLifecycleTestActivity : BaseActivity<ActivityLifecycleTestBinding>() {
    override fun inflateBinding(inflater: LayoutInflater): ActivityLifecycleTestBinding =
        ActivityLifecycleTestBinding.inflate(inflater)

    override fun initConfig(savedInstanceState: Bundle?) {
        LifecycleRecord.activityBundles += savedInstanceState
        recordWithBinding("activity.config")
    }

    override fun initViews() = recordWithBinding("activity.views")
    override fun initListeners() = recordWithBinding("activity.listeners")
    override fun observeData() = recordWithBinding("activity.observe")
    override fun releaseResources() = recordWithBinding("activity.release")

    private fun recordWithBinding(call: String) {
        binding.root
        LifecycleRecord.record(call)
    }
}

class RecordingFragment : BaseFragment<FragmentPremiumBinding>() {
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPremiumBinding.inflate(inflater, container, false)

    override fun initConfig(savedInstanceState: Bundle?) {
        LifecycleRecord.fragmentBundle = savedInstanceState
        recordWithBinding("fragment.config")
    }

    override fun initViews() = recordWithBinding("fragment.views")
    override fun initListeners() = recordWithBinding("fragment.listeners")
    override fun observeData() = recordWithBinding("fragment.observe")
    override fun releaseResources() = recordWithBinding("fragment.release")

    fun isBindingAvailable(): Boolean = runCatching { binding }.isSuccess

    private fun recordWithBinding(call: String) {
        binding.root
        LifecycleRecord.record(call)
    }
}

class RecordingDialogFragment : BaseDialogFragment<DialogNativeFullBinding>() {
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        DialogNativeFullBinding.inflate(inflater, container, false)

    override fun initConfig(savedInstanceState: Bundle?) {
        LifecycleRecord.dialogBundle = savedInstanceState
        recordWithBinding("dialog.config")
    }

    override fun initViews() = recordWithBinding("dialog.views")
    override fun initListeners() = recordWithBinding("dialog.listeners")
    override fun observeData() = recordWithBinding("dialog.observe")
    override fun releaseResources() = recordWithBinding("dialog.release")

    fun isBindingAvailable(): Boolean = runCatching { binding }.isSuccess

    private fun recordWithBinding(call: String) {
        binding.root
        LifecycleRecord.record(call)
    }
}

object LifecycleRecord {
    val calls = mutableListOf<String>()
    val activityBundles = mutableListOf<Bundle?>()
    var fragmentBundle: Bundle? = null
    var dialogBundle: Bundle? = null

    fun record(call: String) {
        calls += call
    }

    fun reset() {
        calls.clear()
        activityBundles.clear()
        fragmentBundle = null
        dialogBundle = null
    }
}
