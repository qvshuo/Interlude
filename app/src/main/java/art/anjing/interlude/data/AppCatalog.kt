package art.anjing.interlude.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

internal data class AppEntry(
    val id: String,
    val component: ComponentName,
    val label: String,
    val artwork: Bitmap?,
    val isBanner: Boolean,
    val accentColor: Int,
)

internal class AppCatalog(private val context: Context) {
    private val preferences by lazy { context.getSharedPreferences("apps", Context.MODE_PRIVATE) }
    private val packageManager = context.packageManager

    private val loadMutex = Mutex()
    private var recordsMigrated = false
    private data class Record(val info: ResolveInfo, val component: ComponentName, val updated: Long, val label: String)
    private var directory: List<Record>? = null
    private var artworkCache = emptyMap<ComponentName, Pair<Long, AppEntry>>()
    private var cachedApps: List<AppEntry>? = null
    private val invalidation = AtomicInteger()
    private var loadedInvalidation = -1
    private var packageSequence = 0
    private var cachedConfiguration = Configuration(context.resources.configuration)

    private fun migrateRecords() {
        if (recordsMigrated) return
        // Older builds keyed records by component; package identity survives entry-activity changes.
        val previousHidden = hiddenIds()
        val hidden = previousHidden.map { it.substringBefore('/') }.toSet()
        val previous = launchTimes()
        val recent = previous.entries.groupBy { it.key.substringBefore('/') }
            .mapValues { (_, entries) -> entries.maxOf { it.value } }
        if (hidden != previousHidden || recent != previous) {
            preferences.edit {
                putStringSet("hidden", hidden)
                previous.keys.forEach { remove("used:$it") }
                recent.forEach { (id, time) -> putLong("used:$id", time) }
            }
        }
        recordsMigrated = true
    }

    @Synchronized
    fun hiddenIds(): Set<String> = preferences.getStringSet("hidden", emptySet())!!.toSet()

    @Synchronized
    fun setHidden(id: String, hidden: Boolean) {
        val next = hiddenIds().toMutableSet().apply { if (hidden) add(id) else remove(id) }
        preferences.edit { putStringSet("hidden", next) }
    }

    private fun launchTimes(): Map<String, Long> = preferences.all.mapNotNull { (key, value) ->
        if (key.startsWith("used:") && value is Long) key.removePrefix("used:") to value else null
    }.toMap()

    fun markOpened(id: String) {
        preferences.edit { putLong("used:$id", AppOrder.nextTime(System.currentTimeMillis(), launchTimes().values)) }
    }

    fun ordered(apps: List<AppEntry>): List<AppEntry> = AppOrder.sort(apps, AppEntry::id, AppEntry::label, launchTimes())

    // Keep invalidation outside the cancellable load so a later ordinary refresh cannot lose it.
    fun invalidate() { invalidation.incrementAndGet() }

    suspend fun load(): List<AppEntry> = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            migrateRecords()
            val configuration = Configuration(context.resources.configuration)
            val resourcesChanged = cachedConfiguration != configuration
            val revision = invalidation.get()
            var changes = packageManager.getChangedPackages(packageSequence)
            val scan = directory == null || resourcesChanged || revision != loadedInvalidation || changes != null
            if (!scan) cachedApps?.let { apps ->
                if (apps.all { it.artwork != null }) return@withLock apps
            }
            var sequence = packageSequence
            val records = if (scan) {
                var result: List<Record>
                do {
                    coroutineContext.ensureActive()
                    // Consume only changes known before this scan. Repeat if packages changed during it.
                    sequence = changes?.sequenceNumber ?: sequence
                    result = scanDirectory()
                    changes = packageManager.getChangedPackages(sequence)
                } while (changes != null)
                result
            } else directory!!
            val previousArtwork = if (resourcesChanged) emptyMap() else artworkCache
            val nextArtwork = mutableMapOf<ComponentName, Pair<Long, AppEntry>>()
            val apps = records.map { record ->
                coroutineContext.ensureActive()
                val cached = previousArtwork[record.component]?.takeIf { it.first == record.updated }?.second
                val entry = if (cached == null) readArtwork(record)
                    else if (cached.label == record.label) cached else cached.copy(label = record.label)
                if (entry.artwork != null) nextArtwork[record.component] = record.updated to entry
                entry
            }
            coroutineContext.ensureActive()
            directory = records
            artworkCache = nextArtwork
            cachedConfiguration = configuration
            loadedInvalidation = revision
            packageSequence = sequence
            if (scan) pruneUninstalled()
            val result = cachedApps?.takeIf { !scan && it == apps } ?: apps
            cachedApps = result
            result
        }
    }

    private suspend fun scanDirectory(): List<Record> {
        val flags = PackageManager.ResolveInfoFlags.of(0)
        val tv = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), flags)
        val other = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), flags)
        return preferredEntries(tv, other).filter { it.activityInfo.packageName != context.packageName }.mapNotNull { info ->
            coroutineContext.ensureActive()
            val component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
            try {
                val updated = packageManager.getPackageInfo(component.packageName, PackageManager.PackageInfoFlags.of(0)).lastUpdateTime
                val label = try {
                    info.loadLabel(packageManager).toString().ifBlank { component.packageName }
                } catch (error: RuntimeException) {
                    Log.w("Interlude", "Cannot load label for ${component.packageName}", error)
                    component.packageName
                }
                Record(info, component, updated, label)
            } catch (_: PackageManager.NameNotFoundException) {
                null // A package can disappear during discovery.
            } catch (error: SecurityException) {
                Log.w("Interlude", "Cannot access ${component.packageName}", error)
                null
            }
        }
    }

    private fun readArtwork(record: Record): AppEntry {
        var isBanner = false
        val image = try {
            val banner = packageManager.getActivityBanner(record.component)
                ?: packageManager.getApplicationBanner(record.component.packageName)
            // Some TV apps declare their square launcher icon as a banner.
            isBanner = banner != null && banner.intrinsicWidth > banner.intrinsicHeight
            (banner ?: record.info.loadIcon(packageManager))?.toArtwork(isBanner)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (error: RuntimeException) {
            Log.w("Interlude", "Cannot load artwork for ${record.component.packageName}", error)
            null
        }
        return AppEntry(record.component.packageName, record.component, record.label, image,
            isBanner, image?.extractAccentColor() ?: Color.rgb(100, 134, 190))
    }

    private fun pruneUninstalled() {
        val removed = (hiddenIds() + launchTimes().keys).filter { id ->
            try {
                packageManager.getApplicationInfo(id, PackageManager.ApplicationInfoFlags.of(0))
                false
            } catch (_: PackageManager.NameNotFoundException) { true }
        }.toSet()
        synchronized(this) {
            if (removed.isNotEmpty()) preferences.edit {
                putStringSet("hidden", hiddenIds() - removed)
                removed.forEach { remove("used:$it") }
            }
        }
    }
}

internal fun Drawable.toArtwork(banner: Boolean): Bitmap {
    val maxWidth = if (banner) 384 else 128
    val maxHeight = if (banner) 216 else 128
    val sourceWidth = intrinsicWidth.takeIf { it > 0 } ?: maxWidth
    val sourceHeight = intrinsicHeight.takeIf { it > 0 } ?: maxHeight
    val scale = minOf(maxWidth.toFloat() / sourceWidth, maxHeight.toFloat() / sourceHeight)
    val width = (sourceWidth * scale).roundToInt().coerceIn(1, maxWidth)
    val height = (sourceHeight * scale).roundToInt().coerceIn(1, maxHeight)
    return createBitmap(width, height).also {
        setBounds(0, 0, width, height)
        draw(Canvas(it))
    }
}

private fun Bitmap.extractAccentColor(): Int {
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0
    for (y in 0 until height step 8) for (x in 0 until width step 8) {
        val pixel = this[x, y]
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        if (Color.alpha(pixel) > 128 && maxOf(r, g, b) > 50 && maxOf(r, g, b) - minOf(r, g, b) > 25) {
            red += r
            green += g
            blue += b
            count++
        }
    }
    return if (count == 0) Color.rgb(100, 134, 190)
    else Color.rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
}

// TV category wins; within each category the lexicographically smallest activity name wins.
internal fun preferredEntries(tv: List<ResolveInfo>, launcher: List<ResolveInfo>): List<ResolveInfo> =
    (tv.sortedBy { it.activityInfo.name } + launcher.sortedBy { it.activityInfo.name })
        .distinctBy { it.activityInfo.packageName }
