package art.anjing.interlude

import android.content.Intent
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import art.anjing.interlude.system.TvActions
import art.anjing.interlude.ui.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun model() = ViewModelProvider(compose.activity)[HomeViewModel::class.java]

    private fun awaitApps(): HomeState {
        compose.waitUntil(10_000) { model().state.value.isLoaded }
        compose.waitForIdle()
        return model().state.value
    }

    @Test fun firstAppReceivesInitialFocusAndDpadMovesBetweenTiles() {
        val apps = awaitApps().visibleApps
        compose.appTile(apps.first().label).assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.appTile(apps[1].label).assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        compose.appTile(apps.first().label).assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionUp) }
        compose.onNodeWithContentDescription("Inputs").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.appTile(apps[1].label).assertIsFocused()
    }

    @Test fun gridUsesFiveColumnsAndDpadReachesTheThirdRow() {
        val template = awaitApps().apps.first()
        val apps = List(11) { template.copy(id = "grid-$it", label = "Grid$it") }
        compose.runOnUiThread {
            compose.activity.setContent {
                HomeScreen(
                    state = HomeState(apps = apps, isLoaded = true),
                    actions = TvActions(compose.activity), networkCapabilities = null,
                    onFocus = {}, onHidden = { _, _ -> }, onToggleHidden = {},
                    onOpen = {},
                )
            }
        }
        compose.waitForIdle()
        val firstRow = (0..4).map {
            compose.appTile("Grid$it").fetchSemanticsNode().boundsInRoot
        }
        assertTrue(firstRow.maxOf { it.top } - firstRow.minOf { it.top } < 30f)
        val secondRow = compose.appTile("Grid5").fetchSemanticsNode().boundsInRoot
        assertTrue(secondRow.top > firstRow.maxOf { it.bottom })
        compose.appTile("Grid0").assertIsFocused()
        compose.onAllNodesWithText("Grid0").assertCountEquals(1)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.appTile("Grid5").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.appTile("Grid10").assertIsFocused()
    }

    @Test fun homeIntentResetsFocusButRefreshPreservesFocusedApp() {
        val apps = awaitApps().visibleApps
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnUiThread { model().refresh() }
        compose.waitForIdle()
        compose.appTile(apps[1].label).assertIsFocused()
        compose.runOnUiThread {
            compose.activity.startActivity(Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME).setClass(compose.activity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasContentDescription("Open ${apps.first().label}") and isFocused())
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        compose.appTile(apps.first().label).assertIsFocused()
    }

    @Test fun hiddenSectionStartsItsOwnRowAtFiveColumnBoundaries() {
        val template = awaitApps().apps.first()
        val state = mutableStateOf(HomeState())
        compose.runOnUiThread {
            compose.activity.setContent {
                HomeScreen(state.value, TvActions(compose.activity), null,
                    {}, { _, _ -> }, {}, {})
            }
        }
        for (normalCount in listOf(0, 1, 5, 6)) {
            val apps = List(normalCount + 2) { template.copy(id = "section-$it", label = "Section$it") }
            compose.runOnUiThread {
                state.value = HomeState(apps = apps, isLoaded = true,
                    hiddenIds = apps.takeLast(2).map { it.id }.toSet(), showHiddenApps = true,
                    pendingFocusId = apps[normalCount].id)
            }
            compose.waitForIdle()
            val firstHidden = compose.appTile("Section$normalCount")
            firstHidden.assertIsFocused()
            if (normalCount == 0) {
                compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
                compose.onNodeWithContentDescription("Inputs").assertIsFocused()
            } else {
                val normalBounds = compose.appTile("Section${normalCount - 1}")
                    .fetchSemanticsNode().boundsInRoot
                assertTrue(firstHidden.fetchSemanticsNode().boundsInRoot.top > normalBounds.bottom)
            }
        }
    }

    @Test fun restoringHiddenTileFromBelowTheViewportReturnsFocusToTheFirstRow() {
        val template = awaitApps().apps.first()
        val apps = List(21) { template.copy(id = "restore-$it", label = "Restore$it") }
        val state = mutableStateOf(HomeState(apps = apps, isLoaded = true,
            hiddenIds = setOf(apps.first().id), showHiddenApps = true))
        compose.runOnUiThread {
            compose.activity.setContent {
                HomeScreen(state.value, TvActions(compose.activity), null,
                    {}, { _, _ -> }, {}, {})
            }
        }
        compose.waitForIdle()
        repeat(4) {
            compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
            compose.waitForIdle()
        }
        compose.appTile("Restore0").assertIsFocused()
        compose.runOnUiThread {
            state.value = state.value.copy(hiddenIds = emptySet(), showHiddenApps = false,
                focusedId = apps.first().id, pendingFocusId = apps.first().id)
        }
        compose.waitForIdle()
        compose.appTile("Restore0").assertIsFocused().assertIsDisplayed()
        compose.appTile("Restore1").assertIsDisplayed()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithContentDescription("Inputs").assertIsFocused()
    }

    @Test fun ordinaryBackPreservesFocusAndRepeatedVisibilityUpdatesDoNotMoveIt() {
        val apps = awaitApps().visibleApps
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnUiThread { model().setHidden(apps.first().id, false) }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.appTile(apps[1].label).assertIsFocused()
        assertFalse(compose.activity.isFinishing)
    }

    @Test fun emptyHomeKeepsSettingsReachable() {
        val before = awaitApps()
        try {
            compose.runOnUiThread { before.apps.forEach { model().setHidden(it.id, true) } }
            compose.waitForIdle()
            compose.onNodeWithText("No apps on the home screen").assertDoesNotExist()
            assertEquals("@settings", model().state.value.focusedId)
        } finally {
            compose.runOnUiThread { before.apps.forEach { model().setHidden(it.id, it.id in before.hiddenIds) } }
        }
    }

    @Test fun resumeReordersRecentlyOpenedAppsEvenWithoutAStop() {
        val app = awaitApps().apps.last()
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.runOnUiThread { model().recordLaunch(app.id) }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5_000) { model().state.value.apps.firstOrNull()?.id == app.id }
    }

    @Test fun inputActionOpensSystemPickerAndRestoresHeaderFocusOnBack() {
        awaitApps()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp); pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        assertFalse(compose.activity.isFinishing)
        if (!compose.activity.hasWindowFocus()) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        }
        compose.onNodeWithContentDescription("Inputs").assertIsFocused()
    }
}

internal fun SemanticsNodeInteractionsProvider.appTile(label: String): SemanticsNodeInteraction =
    onNodeWithContentDescription("Open $label")
