package art.anjing.interlude

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import art.anjing.interlude.data.AppCatalog
import art.anjing.interlude.ui.clockText
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlatformTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun lunarCalendarCoversNewYearAndLeapMonth() {
        val zone = ZoneId.of("Asia/Shanghai")
        assertEquals("丙午年正月初一", clockText(ZonedDateTime.of(2026, 2, 17, 12, 0, 0, 0, zone)).lunar)
        assertEquals("丙午年正月初一", clockText(ZonedDateTime.of(2026, 2, 17, 0, 1, 0, 0, zone)).lunar)
        assertEquals("乙巳年闰六月初一", clockText(ZonedDateTime.of(2025, 7, 25, 12, 0, 0, 0, zone)).lunar)
        assertEquals("乙巳年九月十六", clockText(ZonedDateTime.of(2025, 11, 5, 12, 0, 0, 0, zone)).lunar)
        assertEquals("TUESDAY 17 FEBRUARY", clockText(ZonedDateTime.of(2026, 2, 17, 12, 0, 0, 0, zone)).date.uppercase())
    }

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
        val first = catalog.load().associateBy { it.id }
        val second = catalog.load()
        assertTrue(second.isNotEmpty())
        assertEquals(first.keys, second.map { it.id }.toSet())
        second.filter { first[it.id]?.artwork != null }.forEach { assertSame(first[it.id], it) }
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
}
