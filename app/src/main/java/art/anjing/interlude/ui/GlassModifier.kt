package art.anjing.interlude.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

// Continuous-corner construction adapted from Aerial, Copyright (c) 2026 pengchujin (MIT).
private fun glassTilePath(size: Size, cornerRadius: Float): Path {
    val radius = minOf(cornerRadius, size.minDimension / 3.2f)
    val smoothing = 0.6f
    val extent = radius * (1f + smoothing)
    val arc = (PI / 2 * (1f - smoothing)).toFloat()
    val arcLength = sin(arc / 2) * radius * sqrt(2f)
    val alpha = (PI.toFloat() / 2 - arc) / 2
    val tangent = radius * tan(alpha / 2)
    val beta = PI.toFloat() / 4 * smoothing
    val c = tangent * cos(beta)
    val d = c * tan(beta)
    val b = (extent - arcLength - c - d) / 3f
    val a = 2 * b
    val from = -PI.toFloat() / 4 - arc / 2
    val to = -PI.toFloat() / 4 + arc / 2
    val k = 4f / 3 * tan(arc / 4) * radius
    val x0 = -radius + radius * cos(from)
    val y0 = radius + radius * sin(from)
    val x1 = -radius + radius * cos(to)
    val y1 = radius + radius * sin(to)
    val path = Path()
    fun corner(x: Float, y: Float, ux: Float, uy: Float, vx: Float, vy: Float) {
        fun cubic(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) {
            path.cubicTo(x + ux * ax + vx * ay, y + uy * ax + vy * ay,
                x + ux * bx + vx * by, y + uy * bx + vy * by,
                x + ux * cx + vx * cy, y + uy * cx + vy * cy)
        }
        path.lineTo(x - ux * extent, y - uy * extent)
        cubic(-extent + a, 0f, -extent + a + b, 0f, -extent + a + b + c, d)
        cubic(x0 - k * sin(from), y0 + k * cos(from), x1 + k * sin(to), y1 - k * cos(to), x1, y1)
        cubic(0f, extent - a - b, 0f, extent - a, 0f, extent)
    }
    path.moveTo(extent, 0f)
    corner(size.width, 0f, 1f, 0f, 0f, 1f)
    corner(size.width, size.height, 0f, 1f, -1f, 0f)
    corner(0f, size.height, -1f, 0f, 0f, -1f)
    corner(0f, 0f, 0f, -1f, 1f, 0f)
    path.close()
    return path
}

internal fun Modifier.glass(focus: State<Float>, circle: Boolean): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithCache {
    val path = if (circle) Path().apply { addOval(Rect(0f, 0f, size.width, size.height)) }
        else glassTilePath(size, 16.dp.toPx())
    fun texture(style: Paint.Style, shader: Shader? = null) = createBitmap(
        size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1),
    ).let { bitmap ->
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // A fine inner shade keeps the same glass edge legible over light artwork.
        if (style == Paint.Style.STROKE) canvas.drawPath(path.asAndroidPath(), paint.apply {
            this.style = style
            strokeWidth = 3.dp.toPx()
            color = 0x26000000
        })
        canvas.drawPath(path.asAndroidPath(), paint.apply {
            color = android.graphics.Color.WHITE
            this.style = style
            this.shader = shader
            strokeWidth = 2.dp.toPx()
        })
        bitmap.copy(Bitmap.Config.ARGB_8888, false).also { bitmap.recycle() }.asImageBitmap()
    }
    // Bake antialiased paths once per size; moving focus only composites small textures.
    val mask = texture(Paint.Style.FILL)
    val rim = texture(Paint.Style.STROKE, LinearGradient(0f, 0f, size.width, size.height,
        intArrayOf(0x6BFFFFFF, 0x14FFFFFF, 0x0FFFFFFF, 0x3DFFFFFF), null, Shader.TileMode.CLAMP))
    val focusedRim = texture(Paint.Style.STROKE)
    val sheen = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.14f),
        0.45f to Color.Transparent,
        0.75f to Color.Black.copy(alpha = 0.04f),
        1f to Color.White.copy(alpha = 0.025f),
        start = Offset.Zero, end = Offset(size.width * 0.45f, size.height),
    )
    onDrawWithContent {
        val strength = focus.value
        drawRect(Color(0xFF15181E), alpha = 0.65f + strength * 0.15f)
        drawContent()
        drawRect(sheen, alpha = 0.55f + strength * 0.45f)
        drawImage(rim, alpha = 1f - strength)
        drawImage(focusedRim, alpha = strength * if (circle) 0.65f else 0.95f)
        drawImage(mask, blendMode = BlendMode.DstIn)
    }
}
