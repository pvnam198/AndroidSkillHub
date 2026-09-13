package com.namstd.androidskillhub.core.navigation

enum class StartDestination { ONBOARDING, HOME }

object AppRouter {
    /** Where to land after language selection: straight to [StartDestination.HOME] once setup is done, otherwise onboarding. */
    fun afterLanguage(setupCompleted: Boolean): StartDestination =
        if (setupCompleted) StartDestination.HOME else StartDestination.ONBOARDING
}
