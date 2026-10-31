package dev.sidali.dispatch

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * The three steps, animated: a link card leaves an app (Share), lands in Dispatch where the first
 * row lights up, then flies to the chosen app, which shows a check.
 */
@Composable
fun Hero(modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "hero").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "t",
    )
    // Share in neutral, Dispatch in its lavender, your app in honey: the colors of the sidali.dev palette.
    val c = Tokens.colors
    val measurer = rememberTextMeasurer()
    val labels = listOf(stringResource(R.string.hero_share), stringResource(R.string.app_name), stringResource(R.string.hero_target))
    val description = stringResource(R.string.hero_description)
    val labelStyle = MaterialTheme.typography.bodyMedium.merge(TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.secondaryText))

    Canvas(modifier.semantics { contentDescription = description }) {
        val u = size.width / 340f
        val tile = 74 * u
        val cy = size.height / 2 - 12 * u
        val xs = listOf(size.width * 0.17f, size.width * 0.5f, size.width * 0.83f)
        fun seg(a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
        fun bump(a: Float, b: Float) = sin(seg(a, b) * Math.PI).toFloat()

        val fly1 = FastOutSlowInEasing.transform(seg(0.12f, 0.36f))
        val fly2 = FastOutSlowInEasing.transform(seg(0.52f, 0.76f))
        val press = bump(0.02f, 0.14f)
        val pulse = bump(0.34f, 0.50f)
        val glow = seg(0.38f, 0.46f) - seg(0.62f, 0.70f)
        val land = bump(0.74f, 0.88f)
        val check = seg(0.78f, 0.84f) - seg(0.94f, 1f)
        val card = 1f - seg(0.80f, 0.86f)

        // dotted path between the three
        val dash = PathEffect.dashPathEffect(floatArrayOf(2 * u, 7 * u), 0f)
        for (i in 0..1) drawLine(
            c.secondaryText.copy(alpha = 0.45f), Offset(xs[i] + tile / 2 + 8 * u, cy), Offset(xs[i + 1] - tile / 2 - 8 * u, cy),
            strokeWidth = 2.5f * u, cap = StrokeCap.Round, pathEffect = dash,
        )

        // 1. the app you share from, with its share button
        tileAt(Offset(xs[0], cy), tile, 1f - press * 0.06f, c.card, c.hairline) {
            val g = Offset(0f, 0f)
            drawRoundRect(c.secondaryText.copy(alpha = 0.35f), g + Offset(-22 * u, -24 * u), Size(44 * u, 8 * u), CornerRadius(4 * u))
            drawRoundRect(c.secondaryText.copy(alpha = 0.2f), g + Offset(-22 * u, -10 * u), Size(30 * u, 6 * u), CornerRadius(3 * u))
            // share glyph: tray and arrow
            val s = Stroke(3 * u, cap = StrokeCap.Round)
            drawLine(c.glyph, g + Offset(0f, 18 * u), g + Offset(0f, 2 * u), 3 * u, StrokeCap.Round)
            drawLine(c.glyph, g + Offset(-6 * u, 8 * u), g + Offset(0f, 2 * u), 3 * u, StrokeCap.Round)
            drawLine(c.glyph, g + Offset(6 * u, 8 * u), g + Offset(0f, 2 * u), 3 * u, StrokeCap.Round)
            drawRoundRect(c.glyph, g + Offset(-12 * u, 10 * u), Size(24 * u, 16 * u), CornerRadius(4 * u), style = s)
        }

        // 2. Dispatch: the ordered list, first row lights up
        tileAt(Offset(xs[1], cy), tile, 1f + pulse * 0.08f, c.brand, null) {
            for (r in 0..2) {
                val y = -18 * u + r * 15 * u
                val a = if (r == 0) 0.75f + 0.25f * glow else 0.75f - r * 0.2f
                drawRoundRect(c.onAccent.copy(alpha = a), Offset(-22 * u, y - 5 * u), Size(10 * u, 10 * u), CornerRadius(3 * u))
                drawRoundRect(c.onAccent.copy(alpha = a), Offset(-7 * u, y - 3 * u), Size((if (r == 1) 28 else 22) * u, 6 * u), CornerRadius(3 * u))
            }
            if (glow > 0f) drawRoundRect(
                c.onAccent.copy(alpha = 0.22f * glow), Offset(-28 * u, -26 * u), Size(56 * u, 16 * u), CornerRadius(8 * u),
            )
        }

        // 3. the app you picked
        tileAt(Offset(xs[2], cy), tile, 1f + land * 0.06f, c.target, null) {
            // chat bubble
            drawRoundRect(c.onTarget, Offset(-20 * u, -16 * u), Size(40 * u, 28 * u), CornerRadius(12 * u))
            drawCircle(c.onTarget, 5 * u, Offset(-12 * u, 13 * u))
            if (check > 0f) {
                drawLine(c.target.copy(alpha = check), Offset(-8 * u, -2 * u), Offset(-2 * u, 4 * u), 4 * u, StrokeCap.Round)
                drawLine(c.target.copy(alpha = check), Offset(-2 * u, 4 * u), Offset(9 * u, -8 * u), 4 * u, StrokeCap.Round)
            } else {
                for (i in -1..1) drawCircle(c.target.copy(alpha = 0.6f), 2.6f * u, Offset(i * 9f * u, -2 * u))
            }
        }

        // the shared link card, flying along an arc
        val from: Offset; val to: Offset; val k: Float
        if (t < 0.5f) { from = Offset(xs[0], cy); to = Offset(xs[1], cy); k = fly1 } else { from = Offset(xs[1], cy); to = Offset(xs[2], cy); k = fly2 }
        val visible = (t in 0.10f..0.40f) || (t in 0.50f..0.86f)
        if (visible) {
            val p = Offset(from.x + (to.x - from.x) * k, from.y - 30 * u + (to.y - from.y) * k - sin(k * Math.PI).toFloat() * 26 * u)
            val w = 46 * u; val h = 30 * u
            val alpha = if (t > 0.5f) card else 1f
            // No shadow: a raised card with a hairline, like every card in the app.
            drawRoundRect(c.raised, p + Offset(-w / 2, -h / 2), Size(w, h), CornerRadius(8 * u), alpha = alpha)
            drawRoundRect(c.hairline, p + Offset(-w / 2, -h / 2), Size(w, h), CornerRadius(8 * u), alpha = alpha, style = Stroke(1f * u))
            drawRoundRect(c.glyph.copy(alpha = alpha), p + Offset(-w / 2 + 6 * u, -h / 2 + 7 * u), Size(10 * u, 10 * u), CornerRadius(3 * u))
            drawRoundRect(c.secondaryText.copy(alpha = 0.5f * alpha), p + Offset(-w / 2 + 20 * u, -h / 2 + 8 * u), Size(20 * u, 4 * u), CornerRadius(2 * u))
            drawRoundRect(c.secondaryText.copy(alpha = 0.3f * alpha), p + Offset(-w / 2 + 6 * u, -h / 2 + 21 * u), Size(30 * u, 4 * u), CornerRadius(2 * u))
        }

        // labels
        labels.forEachIndexed { i, s ->
            val m = measurer.measure(s, labelStyle)
            drawText(m, topLeft = Offset(xs[i] - m.size.width / 2f, cy + tile / 2 + 14 * u))
        }
    }
}

private fun DrawScope.tileAt(center: Offset, tile: Float, scale: Float, fill: Color?, border: Color?, content: DrawScope.() -> Unit) {
    val u = tile / 74f
    translate(center.x, center.y) {
        scale(scale, pivot = Offset.Zero) {
            if (fill != null) drawRoundRect(fill, Offset(-tile / 2, -tile / 2), Size(tile, tile), CornerRadius(20 * u))
            if (border != null) drawRoundRect(border, Offset(-tile / 2, -tile / 2), Size(tile, tile), CornerRadius(20 * u), style = Stroke(1.2f * u))
            content()
        }
    }
}
