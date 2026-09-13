package com.namstd.androidskillhub.core.ui.base

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.namstd.androidskillhub.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaseUiLifecycleTest {

    @Before
    fun setUp() = LifecycleRecord.reset()

    @After
    fun tearDown() = LifecycleRecord.reset()

    @Test
    fun activityRunsHooksInOrderAndPassesSavedState() {
        val scenario = ActivityScenario.launch(BaseUiLifecycleTestActivity::class.java)

        assertEquals(
            listOf("activity.config", "activity.views", "activity.listeners", "activity.observe"),
            LifecycleRecord.calls,
        )
        assertNull(LifecycleRecord.activityBundles.first())
        scenario.recreate()

        assertNotNull(LifecycleRecord.activityBundles.last())
        scenario.close()
        assertTrue(LifecycleRecord.calls.contains("activity.release"))
    }

    @Test
    fun fragmentRunsHooksWithBindingAndClearsItAfterDestroyView() {
        val scenario = ActivityScenario.launch(BaseUiLifecycleTestActivity::class.java)
        lateinit var fragment: RecordingFragment

        scenario.onActivity { activity ->
            fragment = RecordingFragment()
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, "recording-fragment")
                .commitNow()
        }

        assertEquals(
            listOf("fragment.config", "fragment.views", "fragment.listeners", "fragment.observe"),
            LifecycleRecord.calls.filter { it.startsWith("fragment.") },
        )
        assertNull(LifecycleRecord.fragmentBundle)
        scenario.recreate()
        assertNotNull(LifecycleRecord.fragmentBundle)

        scenario.onActivity { activity ->
            fragment = checkNotNull(
                activity.supportFragmentManager.findFragmentByTag("recording-fragment"),
            ) as RecordingFragment
            activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()
        }

        assertTrue(LifecycleRecord.calls.contains("fragment.release"))
        assertFalse(fragment.isBindingAvailable())
        scenario.close()
    }

    @Test
    fun dialogRunsHooksWithBindingAndClearsItAfterDestroyView() {
        val scenario = ActivityScenario.launch(BaseUiLifecycleTestActivity::class.java)
        lateinit var dialog: RecordingDialogFragment

        scenario.onActivity { activity ->
            dialog = RecordingDialogFragment()
            dialog.showNow(activity.supportFragmentManager, "recording-dialog")
        }

        assertEquals(
            listOf("dialog.config", "dialog.views", "dialog.listeners", "dialog.observe"),
            LifecycleRecord.calls.filter { it.startsWith("dialog.") },
        )
        assertNull(LifecycleRecord.dialogBundle)
        scenario.recreate()
        assertNotNull(LifecycleRecord.dialogBundle)

        scenario.onActivity { activity ->
            dialog = checkNotNull(
                activity.supportFragmentManager.findFragmentByTag("recording-dialog"),
            ) as RecordingDialogFragment
            dialog.dismissNow()
        }

        assertTrue(LifecycleRecord.calls.contains("dialog.release"))
        assertFalse(dialog.isBindingAvailable())
        scenario.close()
    }
}
