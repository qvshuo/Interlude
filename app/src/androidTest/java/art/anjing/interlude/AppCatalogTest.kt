package art.anjing.interlude

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import art.anjing.interlude.data.AppCatalog
import art.anjing.interlude.data.preferredEntries
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppCatalogTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun hiddenAppsAndRecentLaunchesSurviveCatalogRecreation() = runBlocking {
        val catalog = AppCatalog(context)
        val app = catalog.load().first()
        val wasHidden = app.id in catalog.hiddenIds()
        try {
            catalog.setHidden(app.id, true)
            assertTrue(app.id in AppCatalog(context).hiddenIds())
            catalog.setHidden(app.id, false)
            assertFalse(app.id in AppCatalog(context).hiddenIds())
            catalog.markOpened(app.id)
            assertEquals(app.id, AppCatalog(context).ordered(catalog.load()).first().id)
        } finally {
            catalog.setHidden(app.id, wasHidden)
        }
    }

    @Test fun discoversLaunchableAppsWithoutListingItself() = runBlocking {
        val apps = AppCatalog(context).load()
        assertTrue(apps.isNotEmpty())
        assertTrue(apps.none { it.component.packageName == context.packageName })
        assertEquals(apps.size, apps.map { it.component.packageName }.toSet().size)
        assertTrue(apps.all { it.artwork == null || (it.artwork.width <= 384 && it.artwork.height <= 216) })
    }

    @Test fun unchangedRefreshReusesLoadedArtworkAndEntries() = runBlocking {
        val catalog = AppCatalog(context)
        val original = catalog.load()
        val first = original.associateBy { it.id }
        val second = catalog.load()
        assertTrue(second.isNotEmpty())
        assertEquals(first.keys, second.map { it.id }.toSet())
        second.filter { first[it.id]?.artwork != null }.forEach { assertSame(first[it.id], it) }
        if (original.all { it.artwork != null }) assertSame(original, second)
        catalog.invalidate()
        val forced = catalog.load()
        assertNotSame(second, forced)
        assertEquals(first.keys, forced.map { it.id }.toSet())
    }

    @Test fun packageChangesWhileAwayRefreshTheCachedCatalog() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val fixture = ComponentName(instrumentation.context, Activity::class.java)
        val catalog = AppCatalog(context)
        val manager = context.packageManager
        val previous = manager.getComponentEnabledSetting(fixture)
        assertTrue(catalog.load().any { it.component == fixture })
        instrumentation.uiAutomation.adoptShellPermissionIdentity(Manifest.permission.CHANGE_COMPONENT_ENABLED_STATE)
        try {
            manager.setComponentEnabledSetting(fixture, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            assertTrue(catalog.load().none { it.id == fixture.packageName })
        } finally {
            manager.setComponentEnabledSetting(fixture, previous, PackageManager.DONT_KILL_APP)
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
        assertTrue(catalog.load().any { it.component == fixture })
    }

    @Test fun legacyRecordsMigrateToPackageIdentityAndUninstalledRecordsAreRemoved() = runBlocking {
        val isolated = context.createDeviceProtectedStorageContext()
        val prefs = isolated.getSharedPreferences("apps", Context.MODE_PRIVATE)
        val app = AppCatalog(context).load().first()
        val legacy = app.component.flattenToString()
        val removed = "art.anjing.interlude.missing.fixture"
        try {
            prefs.edit {
                clear()
                putStringSet("hidden", setOf(legacy, removed))
                putLong("used:$legacy", 123L)
                putLong("used:$removed", 456L)
            }
            val catalog = AppCatalog(isolated)
            catalog.load()
            assertEquals(app.component.packageName, app.id)
            assertTrue(app.id in catalog.hiddenIds())
            assertEquals(123L, prefs.getLong("used:${app.id}", 0))
            catalog.load()
            assertTrue(app.id in catalog.hiddenIds())
            assertFalse(removed in catalog.hiddenIds())
            assertFalse(prefs.contains("used:$removed"))
        } finally { prefs.edit { clear() } }
    }

    @Test fun entrySelectionIsStableAcrossScanOrderAndPrefersTv() {
        fun entry(pkg: String, name: String) = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = pkg; this.name = name }
        }
        val tv = listOf(entry("one", "ZTv"), entry("one", "ATv"), entry("two", "OnlyTv"))
        val launcher = listOf(entry("one", "AOrdinary"), entry("three", "ZMain"), entry("three", "AMain"))
        fun selected(a: List<ResolveInfo>, b: List<ResolveInfo>) = preferredEntries(a, b)
            .associate { it.activityInfo.packageName to it.activityInfo.name }
        val expected = mapOf("one" to "ATv", "two" to "OnlyTv", "three" to "AMain")
        assertEquals(expected, selected(tv, launcher))
        assertEquals(expected, selected(tv.reversed(), launcher.reversed()))
        assertEquals(expected, selected(tv.shuffled(), launcher.shuffled()))
    }

    @Test fun forcedInvalidationSurvivesCancellationBeforeRefreshStarts() = runBlocking {
        val catalog = AppCatalog(context)
        val original = catalog.load()
        catalog.invalidate()
        val cancelled = launch(start = CoroutineStart.LAZY) { catalog.load() }
        cancelled.cancelAndJoin()
        val refreshed = catalog.load()
        assertNotSame(original, refreshed)
        assertEquals(original.map { it.id }.toSet(), refreshed.map { it.id }.toSet())
        refreshed.filter { app -> original.any { it.id == app.id && it.artwork != null } }.forEach { app ->
            assertSame(original.first { it.id == app.id }.artwork, app.artwork)
        }
    }
}
