package art.anjing.interlude.system

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import art.anjing.interlude.data.AppEntry

internal class TvActions(private val activity: Activity) {
    fun openApp(app: AppEntry): Boolean = launch(Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))

    fun openSettings() { launch(Intent(Settings.ACTION_SETTINGS)) }
    fun openNetworkSettings() {
        if (!launch(Intent(Settings.ACTION_WIFI_SETTINGS))) launch(Intent(Settings.ACTION_SETTINGS))
    }

    fun openInputs() { launch(Intent("com.android.tv.action.VIEW_INPUTS")) }

    fun networkCapabilities(): NetworkCapabilities? {
        val manager = activity.getSystemService(ConnectivityManager::class.java)
        return manager.getNetworkCapabilities(manager.activeNetwork)
    }

    private fun launch(intent: Intent): Boolean = try {
        activity.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) { false }
    catch (_: SecurityException) { false }
}
