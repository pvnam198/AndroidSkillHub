package com.namstd.androidskillhub.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouterTest {
    @Test fun firstRunStartsOnboarding() = assertEquals(StartDestination.ONBOARDING, AppRouter.afterLanguage(false))
    @Test fun returningRunStartsHome() = assertEquals(StartDestination.HOME, AppRouter.afterLanguage(true))
}
