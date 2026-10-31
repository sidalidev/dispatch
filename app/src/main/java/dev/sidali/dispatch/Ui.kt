package dev.sidali.dispatch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Icons are rendered once at this size and scaled down where needed. */
private const val ICON_PX = 168

/** App icon, loaded off the main thread; a placeholder of the same size holds its place. */
@Composable
fun AppIcon(key: String, size: Dp) {
    val ctx = LocalContext.current
    // Keyed on the target, so a recycled row never shows the previous target's icon.
    var icon by remember(key) { mutableStateOf(Targets.cachedIcon(key)) }
    LaunchedEffect(key) { if (icon == null) icon = withContext(Dispatchers.IO) { Targets.icon(ctx, key, ICON_PX) } }
    Box(Modifier.size(size)) {
        val bmp = icon
        if (bmp != null) Image(bmp, contentDescription = null, modifier = Modifier.fillMaxSize())
        else Box(Modifier.fillMaxSize().clip(RoundedCornerShape(size / 4)).background(Tokens.colors.raised))
    }
}

/** One cell of the favorites grid: icon and two short lines (activity, then app when different). */
@Composable
fun GridCell(t: Target) {
    Column(Modifier.fillMaxSize().padding(top = 8.dp, start = 2.dp, end = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppIcon(t.key, 56.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            t.label, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, color = Tokens.colors.text,
        )
        if (t.app != t.label) Text(
            t.app, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, color = Tokens.colors.secondaryText,
        )
    }
}

/** A list row: icon, names, and whatever goes on the right. */
@Composable
fun TargetRow(t: Target, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(t.key, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.label, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Tokens.colors.text)
            if (t.app != t.label) Text(
                t.app, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Tokens.colors.secondaryText,
            )
        }
        Spacer(Modifier.width(10.dp))
        trailing()
    }
}

@Composable
fun Divider(inset: Dp = 70.dp) {
    Box(Modifier.padding(start = inset).fillMaxWidth().height(0.5.dp).background(Tokens.colors.hairline))
}

@Composable
fun Title(text: String, size: Int = 17, weight: FontWeight = FontWeight.SemiBold) {
    Text(text, fontSize = size.sp, fontWeight = weight, color = Tokens.colors.text)
}

/** Step number in a tinted disc, from the palette's primary container. */
@Composable
fun StepNumber(n: Int) {
    Box(Modifier.size(28.dp).clip(CircleShape).background(Tokens.colors.badge), contentAlignment = Alignment.Center) {
        Text("$n", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Tokens.colors.onBadge)
    }
}
