package com.namstd.androidskillhub

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.namstd.androidskillhub.core.ads.Ads
import com.namstd.androidskillhub.core.config.RemoteConfig
import com.namstd.androidskillhub.core.preferences.AppPreferences
import java.lang.ref.WeakReference

class AndroidSkillHubApp : Application(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {
    private var currentActivity = WeakReference<Activity>(null)
    private var hasEnteredForeground = false

    override fun onCreate() {
        super<Application>.onCreate()
        val preferences = AppPreferences.getInstance(this)
        preferences.applyLocale()
        Ads.adsEnabled = !preferences.isPremium
        RemoteConfig.initialize(this)
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (hasEnteredForeground) {
            currentActivity.get()?.let { activity ->
                val preferences = AppPreferences.getInstance(this@AndroidSkillHubApp)
                if (preferences.setupCompleted) {
                    Ads.onReturnToForeground(activity)
                }
            }
        } else hasEnteredForeground = true
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity.get() === activity) currentActivity.clear()
    }
}
