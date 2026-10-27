package art.anjing.interlude

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import art.anjing.interlude.data.AppCatalog
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomePerformanceTest {
    @Test fun measureWarmCatalogRefresh() = runBlocking<Unit> {
        val catalog = AppCatalog(InstrumentationRegistry.getInstrumentation().targetContext)
        repeat(5) { catalog.load() }
        val samples = List(40) {
            val start = SystemClock.elapsedRealtimeNanos()
            val apps = catalog.load()
            check(apps.isNotEmpty())
            (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
        }.sorted()
        Log.i("InterludeBenchmark", "Warm catalog refresh: median=${samples[20]}ms p90=${samples[36]}ms samples=$samples")
    }
}
