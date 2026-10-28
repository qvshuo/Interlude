package art.anjing.interlude

import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.view.ViewConfiguration
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiddenAppsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun model() = ViewModelProvider(compose.activity)[HomeViewModel::class.java]

    private fun awaitApps(): HomeState {
        compose.waitUntil(10_000) { model().state.value.isLoaded }
        compose.waitForIdle()
        return model().state.value
    }

    private fun focus(node: SemanticsNodeInteraction) {
        node.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.waitForIdle()
    }

    private fun hold(node: SemanticsNodeInteraction) {
        node.performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        compose.waitForIdle()
    }

    private fun holdWithRemote(node: SemanticsNodeInteraction) {
        focus(node)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val downTime = SystemClock.uptimeMillis()
        instrumentation.sendKeySync(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        try {
            SystemClock.sleep(ViewConfiguration.getLongPressTimeout().toLong() + 100)
            instrumentation.sendKeySync(KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER, 1, 0, -1, 0, KeyEvent.FLAG_LONG_PRESS))
        } finally {
            instrumentation.sendKeySync(KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_DPAD_CENTER, 0))
        }
        compose.waitForIdle()
    }

    private fun restore(before: HomeState) {
        compose.runOnUiThread {
            before.apps.forEach { model().setHidden(it.id, it.id in before.hiddenIds) }
            model().closeHiddenApps()
            model().onHome()
        }
        compose.waitForIdle()
    }

    @Test fun hideUsesNextThenPreviousAndRestoreFollowsTheApp() {
        val before = awaitApps()
        val normal = before.visibleApps
        try {
            hold(compose.appTile(normal[0].label))
            compose.appTile(normal[0].label).assertDoesNotExist()
            compose.appTile(normal[1].label).assertIsFocused()
            focus(compose.appTile(normal.last().label))
            hold(compose.appTile(normal.last().label))
            compose.appTile(normal[normal.lastIndex - 1].label).assertIsFocused()
            focus(compose.onNodeWithContentDescription("Settings"))
            hold(compose.onNodeWithContentDescription("Settings"))
            compose.appTile(normal[0].label).assertIsFocused()
            hold(compose.appTile(normal[0].label))
            compose.appTile(normal[0].label).assertIsFocused()
            assertFalse(normal[0].id in model().state.value.hiddenIds)
        } finally { restore(before) }
    }

    @Test fun hiddenSectionStartsNewRowAndBackPreservesIt() {
        val before = awaitApps()
        val hidden = before.visibleApps.first()
        try {
            hold(compose.appTile(hidden.label))
            val order = model().state.value.visibleApps.map { it.id }
            focus(compose.onNodeWithContentDescription("Settings"))
            hold(compose.onNodeWithContentDescription("Settings"))
            assertEquals(order, model().state.value.visibleApps.map { it.id })
            compose.appTile(hidden.label).assertIsFocused()
            val normalBounds = compose.appTile(before.visibleApps[1].label).fetchSemanticsNode().boundsInRoot
            val hiddenBounds = compose.appTile(hidden.label).fetchSemanticsNode().boundsInRoot
            assertTrue(hiddenBounds.top > normalBounds.bottom)
            focus(compose.appTile(hidden.label))
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.appTile(hidden.label).assertIsFocused()
            assertTrue(model().state.value.showHiddenApps)
            assertFalse(compose.activity.isFinishing)
            focus(compose.onNodeWithContentDescription("Settings"))
            hold(compose.onNodeWithContentDescription("Settings"))
            assertFalse(model().state.value.showHiddenApps)
            compose.appTile(hidden.label).assertDoesNotExist()
            compose.appTile(before.visibleApps[1].label).assertIsFocused()
            focus(compose.onNodeWithContentDescription("Settings"))
            hold(compose.onNodeWithContentDescription("Settings"))
            assertTrue(model().state.value.showHiddenApps)
        } finally { restore(before) }
    }

    @Test fun realLongPressDoesNotLaunchNextAppOrRepeatHide() {
        val before = awaitApps()
        try {
            holdWithRemote(compose.appTile(before.visibleApps.first().label))
            assertEquals(before.hiddenIds + before.visibleApps.first().id, model().state.value.hiddenIds)
            assertTrue(compose.activity.hasWindowFocus())
            compose.appTile(before.visibleApps[1].label).assertIsFocused()
        } finally { restore(before) }
    }

    @Test fun settingsLongPressMovesFocusWithoutLaunchingOrRemovingAnotherApp() {
        val before = awaitApps()
        val removed = before.apps.last()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }, Instrumentation.ActivityResult(0, null), true)
        try {
            compose.runOnUiThread {
                before.apps.forEach { model().setHidden(it.id, false) }
                model().setHidden(removed.id, true)
            }
            compose.waitForIdle()
            repeat(3) { index ->
                holdWithRemote(compose.onNodeWithContentDescription("Settings"))
                val expanded = index % 2 == 0
                assertEquals(expanded, model().state.value.showHiddenApps)
                val target = if (expanded) removed else model().state.value.visibleApps.first()
                compose.appTile(target.label).assertIsFocused().assertIsDisplayed()
                assertEquals(setOf(removed.id), model().state.value.hiddenIds)
                assertEquals(0, monitor.hits)
                assertTrue(compose.activity.hasWindowFocus())
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            restore(before)
        }
    }

    @Test fun emptyModeDoesNotOpenAndModeIsNotPersisted() {
        val before = awaitApps()
        try {
            compose.runOnUiThread { before.apps.forEach { model().setHidden(it.id, false) } }
            focus(compose.onNodeWithContentDescription("Settings"))
            hold(compose.onNodeWithContentDescription("Settings"))
            assertFalse(model().state.value.showHiddenApps)
            compose.onNodeWithContentDescription("Settings").assertIsFocused()
            compose.runOnUiThread {
                model().setHidden(before.apps.first().id, true)
                model().toggleHiddenApps()
            }
            compose.waitForIdle()
            assertTrue(model().state.value.showHiddenApps)
            val restarted = HomeViewModel(compose.activity.applicationContext as Application)
            compose.runOnUiThread { restarted.refresh() }
            compose.waitUntil(10_000) { restarted.state.value.isLoaded }
            assertFalse(restarted.state.value.showHiddenApps)
            assertTrue(before.apps.first().id in restarted.state.value.hiddenIds)
            compose.runOnUiThread { model().onHome() }
            compose.waitForIdle()
            assertTrue(model().state.value.showHiddenApps)
            compose.runOnUiThread { model().closeHiddenApps() }
            compose.waitForIdle()
            assertFalse(model().state.value.showHiddenApps)
        } finally { restore(before) }
    }

    @Test fun allHiddenAppsCanBeShownAndRestoredFromSettings() {
        val before = awaitApps()
        try {
            before.visibleApps.forEach { hold(compose.appTile(it.label)) }
            compose.onNodeWithContentDescription("Settings").assertIsFocused()
            compose.onNodeWithText("No apps on the home screen").assertDoesNotExist()
            hold(compose.onNodeWithContentDescription("Settings"))
            val first = before.apps.first()
            compose.appTile(first.label).assertIsFocused()
            focus(compose.onNodeWithContentDescription("Settings"))
            hold(compose.onNodeWithContentDescription("Settings"))
            assertFalse(model().state.value.showHiddenApps)
            compose.onNodeWithContentDescription("Settings").assertIsFocused()
            hold(compose.onNodeWithContentDescription("Settings"))
            compose.appTile(first.label).assertIsFocused()
            hold(compose.appTile(first.label))
            assertEquals(listOf(first.id), model().state.value.visibleApps.map { it.id })
            compose.appTile(first.label).assertIsFocused()
        } finally { restore(before) }
    }

    @Test fun shortPressOnHiddenAppLaunchesWithoutRestoringIt() {
        val before = awaitApps()
        val app = before.visibleApps.first()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }, Instrumentation.ActivityResult(0, null), true)
        try {
            hold(compose.appTile(app.label))
            focus(compose.onNodeWithContentDescription("Settings"))
            hold(compose.onNodeWithContentDescription("Settings"))
            focus(compose.appTile(app.label))
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
            compose.waitForIdle()
            assertEquals(1, monitor.hits)
            assertTrue(app.id in model().state.value.hiddenIds)
            assertTrue(model().state.value.showHiddenApps)
        } finally {
            instrumentation.removeMonitor(monitor)
            restore(before)
        }
    }
}
