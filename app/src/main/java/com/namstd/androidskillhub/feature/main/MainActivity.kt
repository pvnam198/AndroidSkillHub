package com.namstd.androidskillhub.feature.main

import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.ui.base.BaseActivity
import com.namstd.androidskillhub.databinding.ActivityMainBinding
import com.namstd.androidskillhub.feature.home.HomeFragment

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater)

    override fun initConfig(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return
        supportFragmentManager.beginTransaction().replace(R.id.nav_host, HomeFragment()).commit()
    }

    /** Pushes [fragment] on top of the current one, adding it to the back stack. */
    fun navigateTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host, fragment)
            .addToBackStack(null)
            .commit()
    }
}
