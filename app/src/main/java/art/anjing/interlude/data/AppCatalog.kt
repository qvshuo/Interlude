package art.anjing.interlude.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
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
    private var entryCache = emptyMap<ComponentName, Pair<Long, AppEntry>>()
    private var cachedConfiguration = android.content.res.Configuration(context.resources.configuration)

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

    suspend fun load(): List<AppEntry> = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            migrateRecords()
            if (cachedConfiguration != context.resources.configuration) {
                entryCache = emptyMap()
                cachedConfiguration = android.content.res.Configuration(context.resources.configuration)
            }
            val nextCache = mutableMapOf<ComponentName, Pair<Long, AppEntry>>()
            val flags = PackageManager.ResolveInfoFlags.of(0)
            val tv = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), flags)
            val other = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), flags)
            val apps = (tv + other).distinctBy { it.activityInfo.packageName }
                .filter { it.activityInfo.packageName != context.packageName }
                .mapNotNull { info ->
                    try {
                        val component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                        coroutineContext.ensureActive()
                        val updated = packageManager.getPackageInfo(component.packageName, PackageManager.PackageInfoFlags.of(0)).lastUpdateTime
                        val entry = entryCache[component]?.takeIf { it.first == updated }?.second ?: run {
                            val label = try {
                                info.loadLabel(packageManager).toString().ifBlank { component.packageName }
                            } catch (error: RuntimeException) {
                                Log.w("Interlude", "Cannot load label for ${component.packageName}", error)
                                component.packageName
                            }
                            // Broken vendor artwork must not remove an otherwise launchable app.
                            var isBanner = false
                            val image = try {
                                val banner = packageManager.getActivityBanner(component) ?: packageManager.getApplicationBanner(component.packageName)
                                // Some TV apps declare their square launcher icon as a banner.
                                isBanner = banner != null && banner.intrinsicWidth > banner.intrinsicHeight
                                (banner ?: info.loadIcon(packageManager))?.toArtwork(isBanner)
                            } catch (error: RuntimeException) {
                                Log.w("Interlude", "Cannot load artwork for ${component.packageName}", error)
                                null
                            }
                            AppEntry(component.packageName, component, label, image,
                                isBanner, image?.extractAccentColor() ?: Color.rgb(100, 134, 190))
                        }
                        // Retry transient resource failures on the next refresh.
                        if (entry.artwork != null) nextCache[component] = updated to entry
                        entry
                    } catch (_: PackageManager.NameNotFoundException) {
                        null // An app can disappear between discovery and reading its artwork.
                    } catch (error: SecurityException) {
                        Log.w("Interlude", "Cannot access ${info.activityInfo.packageName}", error)
                        null
                    }
                }
            coroutineContext.ensureActive()
            entryCache = nextCache
            pruneUninstalled()
            apps
        }
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
