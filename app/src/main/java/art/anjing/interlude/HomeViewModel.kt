package art.anjing.interlude

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import art.anjing.interlude.data.AppEntry
import art.anjing.interlude.data.AppCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class HomeState(
    val apps: List<AppEntry> = emptyList(),
    val hiddenIds: Set<String> = emptySet(),
    val isLoaded: Boolean = false,
    val focusedId: String? = null,
    val homeRequest: Int = 0,
    val showHiddenApps: Boolean = false,
    val pendingFocusId: String? = null,
) {
    val visibleApps: List<AppEntry> get() = apps.filterNot { it.id in hiddenIds }
    val hiddenApps: List<AppEntry> get() = apps.filter { it.id in hiddenIds }
}

internal class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val catalog = AppCatalog(application)
    private val mutableState = MutableStateFlow(HomeState())
    val state = mutableState.asStateFlow()
    private var refreshJob: Job? = null

    fun refresh(forceRefresh: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val apps = catalog.ordered(catalog.load(forceRefresh))
            mutableState.update {
                val hidden = catalog.hiddenIds()
                val mode = it.showHiddenApps && apps.any { app -> app.id in hidden }
                val available = apps.filter { app -> mode || app.id !in hidden }
                val desired = it.pendingFocusId ?: it.focusedId
                val lostFocus = desired != null && !desired.startsWith("@") && available.none { app -> app.id == desired }
                val target = if (lostFocus) available.firstOrNull()?.id ?: "@settings" else it.pendingFocusId
                it.copy(apps = apps, hiddenIds = hidden, isLoaded = true,
                    showHiddenApps = mode, pendingFocusId = target)
            }
        }
    }

    fun onFocusChanged(id: String) {
        mutableState.update {
            if (it.pendingFocusId != null && it.pendingFocusId != id) it
            else it.copy(focusedId = id, pendingFocusId = null)
        }
    }

    fun recordLaunch(id: String) { catalog.markOpened(id) }

    fun setHidden(id: String, hidden: Boolean) {
        val before = mutableState.value
        if (before.apps.none { it.id == id } || (id in before.hiddenIds) == hidden) return
        val normal = before.visibleApps
        val index = normal.indexOfFirst { it.id == id }
        val target = if (hidden) normal.getOrNull(index + 1)?.id ?: normal.getOrNull(index - 1)?.id ?: "@settings" else id
        catalog.setHidden(id, hidden)
        mutableState.update {
            val hiddenIds = catalog.hiddenIds()
            it.copy(hiddenIds = hiddenIds, pendingFocusId = target,
                showHiddenApps = it.showHiddenApps && it.apps.any { app -> app.id in hiddenIds })
        }
    }

    fun toggleHiddenApps(): Boolean {
        if (mutableState.value.showHiddenApps) closeHiddenApps()
        else {
            if (mutableState.value.hiddenApps.isEmpty()) return false
            mutableState.update { it.copy(showHiddenApps = true) }
        }
        return true
    }

    fun closeHiddenApps() {
        mutableState.update {
            val target = it.focusedId?.takeIf { id -> id.startsWith("@") || it.visibleApps.any { app -> app.id == id } } ?: "@settings"
            it.copy(showHiddenApps = false, pendingFocusId = target)
        }
    }

    fun onHome() {
        mutableState.update {
            it.copy(apps = catalog.ordered(it.apps), focusedId = null, pendingFocusId = null,
                homeRequest = it.homeRequest + 1)
        }
    }
}
