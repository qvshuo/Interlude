package art.anjing.interlude.ui

import android.net.NetworkCapabilities
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.*
import art.anjing.interlude.HomeState
import art.anjing.interlude.R
import art.anjing.interlude.data.AppEntry
import art.anjing.interlude.system.TvActions
import java.util.Locale
import kotlinx.coroutines.delay

private const val AppColumns = 5
private val GridFadeHeight = 52.dp
private val GridFocusInset = 10.dp

private val Ink = Color(0xFFF1F3F8)
private val Muted = Color(0xFF9298A5)
private val Panel = Color(0xFF15181E)
private val Inter = FontFamily(Font(R.font.inter_light, FontWeight.Light), Font(R.font.inter_regular, FontWeight.Normal))
private val InterTextStyle = TextStyle(fontFamily = Inter, platformStyle = PlatformTextStyle(includeFontPadding = false))
private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

private class ConfirmPress {
    var down = false
    var consumed = false
}

@Composable
internal fun HomeScreen(
    state: HomeState,
    actions: TvActions,
    networkCapabilities: NetworkCapabilities?,
    onFocus: (String) -> Unit,
    onHidden: (String, Boolean) -> Unit,
    onToggleHidden: () -> Unit,
    onOpen: (AppEntry) -> Unit,
) {
    MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Panel, onSurface = Ink)) {
        val visibleApps = remember(state.apps, state.hiddenIds) { state.visibleApps }
        val hiddenApps = remember(state.apps, state.hiddenIds) { state.hiddenApps }
        val displayed = remember(visibleApps, hiddenApps, state.showHiddenApps) {
            if (state.showHiddenApps) visibleApps + hiddenApps else visibleApps
        }
        val grid = rememberLazyGridState()
        val requesterPool = remember { mutableMapOf<String, FocusRequester>() }
        val appIds = state.apps.map { it.id }.toSet()
        val requesters = remember(appIds) {
            requesterPool.keys.retainAll(appIds)
            appIds.forEach { requesterPool.getOrPut(it) { FocusRequester() } }
            requesterPool
        }
        val inputs = remember { FocusRequester() }
        val settings = remember { FocusRequester() }
        val networkFocus = remember { FocusRequester() }
        // Key bookkeeping must not recompose the clickable surface between down and up.
        val confirmPress = remember { ConfirmPress() }
        val consumeLongPress = { confirmPress.consumed = confirmPress.down }
        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
            confirmPress.down = false
            confirmPress.consumed = false
        }
        var lastAppFocus by rememberSaveable { mutableStateOf<String?>(null) }
        val focusedApp = displayed.firstOrNull { it.id == state.focusedId }
            ?: displayed.firstOrNull { it.id == lastAppFocus } ?: displayed.firstOrNull()
        val glow by animateColorAsState(Color(focusedApp?.accentColor ?: 0xFF6486BE.toInt()), tween(650), label = "ambient")
        val owner = LocalLifecycleOwner.current
        var initialFocusRequested by rememberSaveable { mutableStateOf(false) }
        var clock by remember { mutableStateOf(clockText()) }
        LaunchedEffect(owner) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    clock = clockText()
                    delay(60_000 - System.currentTimeMillis() % 60_000)
                }
            }
        }

        // Explicit mutations take priority over incidental focus changes while lazy items move.
        LaunchedEffect(state.isLoaded, state.apps, displayed.map { it.id }, state.homeRequest, state.showHiddenApps, state.pendingFocusId) {
            if (!state.isLoaded) return@LaunchedEffect
            val target = state.pendingFocusId
                ?: (if (initialFocusRequested) state.focusedId else null)
                ?: displayed.firstOrNull()?.id ?: "@settings"
            when (target) {
                "@inputs" -> inputs.requestFocus()
                "@network" -> networkFocus.requestFocus()
                "@settings" -> settings.requestFocus()
                else -> {
                    val appIndex = displayed.indexOfFirst { it.id == target }
                    if (appIndex < 0) return@LaunchedEffect
                    val index = appIndex + if (state.showHiddenApps && appIndex >= visibleApps.size) 1 else 0
                    if (!initialFocusRequested || state.focusedId == null) grid.scrollToItem(0)
                    else if (grid.layoutInfo.visibleItemsInfo.none { it.index == index }) grid.scrollToItem(index)
                    withFrameNanos { }
                    requesters.getValue(target).requestFocus()
                }
            }
            onFocus(target)
            initialFocusRequested = true
        }
        LaunchedEffect(state.homeRequest) {
            confirmPress.down = false
            confirmPress.consumed = false
        }
        BackHandler {}

        Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent { event ->
            val confirm = when (event.nativeKeyEvent.keyCode) {
                AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER,
                AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_BUTTON_A -> true
                else -> false
            }
            if (!confirm) false else {
                val consumed = confirmPress.consumed
                confirmPress.down = event.type == KeyEventType.KeyDown
                if (event.type == KeyEventType.KeyUp) confirmPress.consumed = false
                // The long press may remove its focused tile; do not deliver its repeats/up to the next tile.
                consumed
            }
        }) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.28f), glow.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.30f, size.height * 0.52f), radius = size.width * 0.57f,
                ))
            }
            Column(Modifier.fillMaxSize().padding(horizontal = 48.dp).padding(top = 32.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(clock.time, color = Ink, fontSize = 48.sp, fontWeight = FontWeight.Light,
                            style = InterTextStyle.copy(fontFeatureSettings = "tnum, case"), lineHeight = 60.sp,
                            letterSpacing = (-1.5).sp, maxLines = 1)
                        Box(Modifier.padding(horizontal = 16.dp)
                            .width(1.dp).height(26.dp).background(Ink.copy(alpha = 0.2f)))
                        Column(Modifier.weight(1f)) {
                            Text(clock.date.uppercase(Locale.ENGLISH), color = Ink.copy(alpha = 0.75f), fontSize = 14.sp,
                                style = InterTextStyle, lineHeight = 20.sp, letterSpacing = 1.25.sp, maxLines = 1)
                            Text(clock.lunar, color = Ink.copy(alpha = 0.6f), fontSize = 14.sp,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                lineHeight = 20.sp, letterSpacing = 1.sp, maxLines = 1,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeaderButton("Inputs", R.drawable.ic_input,
                            Modifier.focusRequester(inputs).onFocusChanged {
                                if (it.isFocused) onFocus("@inputs")
                            }.focusProperties { down = requesters[focusedApp?.id] ?: settings },
                            onClick = actions::openInputs)
                        HeaderButton("Network", if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) R.drawable.ic_ethernet else R.drawable.ic_wifi,
                            Modifier.focusRequester(networkFocus).onFocusChanged {
                                if (it.isFocused) onFocus("@network")
                            }.focusProperties { down = requesters[focusedApp?.id] ?: settings },
                            onClick = actions::openNetworkSettings,
                            tint = if (networkCapabilities == null) Muted else Ink)
                        HeaderButton("Settings", R.drawable.ic_settings,
                            Modifier.focusRequester(settings).onFocusChanged {
                                if (it.isFocused) onFocus("@settings")
                            }.focusProperties { down = requesters[focusedApp?.id] ?: FocusRequester.Default },
                            onClick = actions::openSettings, onLongClick = {
                                consumeLongPress()
                                onToggleHidden()
                            })
                    }
                }
                if (displayed.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(AppColumns), state = grid,
                        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = GridFadeHeight + GridFocusInset, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                            .drawWithCache {
                                // Cache the fixed mask; keep the focus inset clear for resting tiles.
                                val fade = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black),
                                    startY = 0f,
                                    endY = GridFadeHeight.toPx(),
                                )
                                onDrawWithContent {
                                    drawContent()
                                    drawRect(brush = fade, blendMode = BlendMode.DstIn)
                                }
                            },
                    ) {
                        listOf(visibleApps, if (state.showHiddenApps) hiddenApps else emptyList()).forEachIndexed { section, apps ->
                            if (section == 1 && apps.isNotEmpty()) {
                                // A full-span separator starts a new row without inserting empty cards.
                                item(key = "@hidden", span = { GridItemSpan(maxLineSpan) }, contentType = "section") {
                                    Spacer(Modifier.height(0.dp))
                                }
                            }
                            itemsIndexed(apps, key = { _, app -> app.id }, contentType = { _, _ -> "app" }) { index, app ->
                                val hidden = section == 1
                                AppTile(app, Modifier.animateItem().focusRequester(requesters.getValue(app.id))
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            lastAppFocus = app.id
                                            onFocus(app.id)
                                        }
                                    }
                                    .focusProperties {
                                        if (index < AppColumns && (!hidden || visibleApps.isEmpty())) up = inputs
                                    },
                                    hidden = hidden,
                                    onClick = { onOpen(app) },
                                    onLongClick = {
                                        consumeLongPress()
                                        onHidden(app.id, !hidden)
                                    })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderButton(
    description: String,
    icon: Int,
    modifier: Modifier,
    onClick: () -> Unit,
    tint: Color = Ink,
    onLongClick: (() -> Unit)? = null,
) {
    FocusSurface(onClick, modifier.size(40.dp), onLongClick = onLongClick, circle = true) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(painterResource(icon), description, Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(tint))
        }
    }
}

@Composable
private fun AppTile(
    app: AppEntry,
    modifier: Modifier,
    hidden: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(if (focused) -3f else 0f, tween(180), label = "lift")
    val labelColor by animateColorAsState(if (focused) Ink else Ink.copy(alpha = 0.8f), tween(150), label = "app label")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        FocusSurface(onClick, Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            .onFocusChanged { focused = it.isFocused }.graphicsLayer { translationY = lift.dp.toPx() }
            .semantics { contentDescription = "Open ${app.label}" }, onLongClick = onLongClick) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (app.artwork != null) Image(app.artwork.asImageBitmap(), null,
                    modifier = if (app.isBanner) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(12.dp),
                    contentScale = if (app.isBanner) ContentScale.Crop else ContentScale.Fit,
                    colorFilter = if (hidden) Grayscale else null)
                else Text(app.label.take(1), color = Ink, fontSize = 36.sp, lineHeight = 44.sp)
            }
        }
        Text(app.label, color = labelColor, fontSize = 16.sp, maxLines = 1,
            lineHeight = 24.sp,
            fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            ),
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.paddingFromBaseline(top = 25.sp, bottom = 5.sp))
    }
}

@Composable
private fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    circle: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = if (circle) CircleShape else RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick, onLongClick = onLongClick, modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Panel, focusedContainerColor = Color(0xFF252A33)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = if (circle) 1.05f else 1.1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)), shape = shape),
            focusedBorder = Border(
                BorderStroke(1.dp, Color.White.copy(alpha = if (circle) 0.5f else 0.95f)),
                shape = shape,
            ),
        ),
        content = content,
    )
}
