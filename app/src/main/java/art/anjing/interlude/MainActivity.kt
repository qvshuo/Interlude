package art.anjing.interlude

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import art.anjing.interlude.system.TvActions
import art.anjing.interlude.ui.HomeScreen

class MainActivity : ComponentActivity() {
    private lateinit var model: HomeViewModel
    private lateinit var actions: TvActions
    private var feedback: Toast? = null
    private var networkCapabilities by mutableStateOf<NetworkCapabilities?>(null)
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            model.refresh(forceRefresh = true)
        }
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = updateNetwork()
        override fun onLost(network: Network) = updateNetwork()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = updateNetwork()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[HomeViewModel::class.java]
        actions = TvActions(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            HomeScreen(state, actions, networkCapabilities, model::onFocusChanged, { id, hidden ->
                val app = model.state.value.apps.firstOrNull { it.id == id }
                if (app != null && (id in model.state.value.hiddenIds) != hidden) {
                    model.setHidden(id, hidden)
                    showFeedback("${if (hidden) "Removed from Home" else "Added to Home"}: ${app.label}")
                }
            }, {
                if (!model.toggleHiddenApps()) showFeedback("All apps are already on Home")
            }) { app ->
                if (actions.openApp(app)) model.recordLaunch(app.id) else model.refresh(forceRefresh = true)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(packageReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }, RECEIVER_EXPORTED)
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(networkCallback)
        updateNetwork()
    }

    override fun onResume() {
        super.onResume()
        model.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) model.onHome()
    }

    override fun onStop() {
        unregisterReceiver(packageReceiver)
        getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
        super.onStop()
    }

    private fun showFeedback(message: String) {
        feedback?.cancel()
        feedback = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).also { it.show() }
    }

    private fun updateNetwork() {
        runOnUiThread { networkCapabilities = actions.networkCapabilities() }
    }
}
