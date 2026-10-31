package dev.sidali.dispatch

import android.content.res.AssetManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------- tokens: the one place for colors, type, sizes and springs ---------- */
object Tokens {
    val cardRadius = 22.dp
    val buttonRadius = 14.dp
    val buttonHeight = 54.dp
    val gutter = 20.dp
    val pressedScale = 0.97f
    /** Titles: Bricolage Grotesque Bold, tracked in by 2 percent. */
    val titleTracking = (-0.02).em

    /** The roles the screens use, resolved for light or dark. */
    val colors: Roles
        @Composable @ReadOnlyComposable get() = LocalRoles.current

    /** Bricolage Grotesque, for titles. */
    val display: FontFamily
        @Composable @ReadOnlyComposable get() = LocalDisplay.current
}

/**
 * What each color is for. Screens never pick a raw color: they ask for a role, and the role comes
 * from [Palette], the sidali.dev palette with lavender as the color of Dispatch.
 */
@Immutable
class Roles(
    /** Page behind the cards. */
    val page: Color,
    /** Cards, action tiles. */
    val card: Color,
    /** One step above a card: a lifted row, the close button, an icon placeholder. */
    val raised: Color,
    val hairline: Color,
    val text: Color,
    val secondaryText: Color,
    /** The color of Dispatch, exactly as on the icon: the illustration's Dispatch tile. */
    val brand: Color,
    /** Lavender for small marks (checks, bullets, action glyphs): the brand color, lifted in dark so it reads on the cards. */
    val glyph: Color,
    /** The one filled action per screen: lavender one step deeper, so its label passes AA. */
    val accent: Color,
    val onAccent: Color,
    /** Lavender as text: links and actions inside cards. */
    val accentText: Color,
    /** Tinted secondary actions, capsules and step numbers. */
    val tonal: Color,
    val onTonal: Color,
    /** Step numbers and the preview placeholder. */
    val badge: Color,
    val onBadge: Color,
    /** The receiving app in the illustration: honey, the family color, so the three steps read apart. */
    val target: Color,
    val onTarget: Color,
    /** Switches and allowed states. */
    val success: Color,
    val onSuccess: Color,
    /** Destructive actions (Erase everything). */
    val danger: Color,
    val onDanger: Color,
    val scrim: Color,
)

private val LocalRoles = staticCompositionLocalOf<Roles> { error("DispatchTheme is missing") }
private val LocalDisplay = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/**
 * The sidali.dev palette, "Miel du Champsaur": cream and pine night pages, pine ink, and lavender
 * (#7A6FB0) as the color of Dispatch, the same on the icon and in the app. Fixed colors, in light
 * and dark, never taken from the wallpaper. These are the only literal colors in the app, with the
 * launch window colors in res/values and the icon in res/drawable.
 *
 * Contrast, computed (WCAG 2.1): ink on cream 13.9, secondary 6.9; cream on the filled lavender 4.8;
 * lavender text 6.7 on white, 7.1 on the dark card; secondary on the dark card 6.5; honey under
 * pine ink 6.9. Lavender glyphs (3 needed for graphics): 4.4 on white, 5.6 on the dark card.
 */
private object Palette {
    val lavender = Color(0xFF7A6FB0)
    val lavenderDeep = Color(0xFF7065A8)
    val honey = Color(0xFFE9A23B)
    val ink = Color(0xFF1F2A22)
    val cream = Color(0xFFFBF7EF)

    val light = Roles(
        page = cream, card = Color(0xFFFFFFFF), raised = Color(0xFFF1EBDF), hairline = Color(0xFFE5DDCE),
        text = ink, secondaryText = Color(0xFF4A5A4F),
        brand = lavender, glyph = lavender, accent = lavenderDeep, onAccent = cream, accentText = Color(0xFF5E5392),
        tonal = Color(0xFFE9E5F5), onTonal = Color(0xFF3D3470),
        badge = Color(0xFFE9E5F5), onBadge = Color(0xFF3D3470),
        target = honey, onTarget = ink,
        success = Color(0xFF2F7A50), onSuccess = Color(0xFFFFFFFF),
        danger = Color(0xFFB3261E), onDanger = Color(0xFFFFFFFF),
        scrim = Color(0xFF000000),
    )
    val dark = Roles(
        page = Color(0xFF12201A), card = Color(0xFF1C2E26), raised = Color(0xFF263B31), hairline = Color(0xFF2E453A),
        text = cream, secondaryText = Color(0xFF9FB3A8),
        brand = lavender, glyph = Color(0xFFA59BD8), accent = lavenderDeep, onAccent = cream, accentText = Color(0xFFB9B0E6),
        tonal = Color(0xFF2E2A4A), onTonal = Color(0xFFDCD6F7),
        badge = Color(0xFF2E2A4A), onBadge = Color(0xFFDCD6F7),
        target = honey, onTarget = ink,
        success = Color(0xFF7BC79A), onSuccess = Color(0xFF12201A),
        danger = Color(0xFFFFB4AB), onDanger = Color(0xFF5C0A05),
        scrim = Color(0xFF000000),
    )
    /** The share sheet sits one step above whatever you share from. */
    val darkSheet = Roles(
        page = Color(0xFF172821), card = Color(0xFF22362D), raised = Color(0xFF2C4237), hairline = Color(0xFF344C40),
        text = dark.text, secondaryText = dark.secondaryText,
        brand = dark.brand, glyph = dark.glyph, accent = dark.accent, onAccent = dark.onAccent, accentText = dark.accentText,
        tonal = dark.tonal, onTonal = dark.onTonal, badge = dark.badge, onBadge = dark.onBadge,
        target = dark.target, onTarget = dark.onTarget, success = dark.success, onSuccess = dark.onSuccess,
        danger = dark.danger, onDanger = dark.onDanger, scrim = dark.scrim,
    )

    /** The Material scheme, for the few system components (menus, bottom sheet), built from the same roles. */
    fun scheme(r: Roles, dark: Boolean): ColorScheme {
        val base = if (dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = if (dark) r.accentText else r.accent, onPrimary = if (dark) Color(0xFF2A2350) else r.onAccent,
            primaryContainer = r.tonal, onPrimaryContainer = r.onTonal, inversePrimary = r.accentText,
            secondary = r.accentText, onSecondary = r.onAccent,
            secondaryContainer = r.tonal, onSecondaryContainer = r.onTonal,
            tertiary = r.target, onTertiary = r.onTarget,
            tertiaryContainer = r.target, onTertiaryContainer = r.onTarget,
            error = r.danger, onError = r.onDanger,
            background = r.page, onBackground = r.text, surface = r.page, onSurface = r.text,
            surfaceVariant = r.raised, onSurfaceVariant = r.secondaryText, surfaceTint = Color.Transparent,
            outline = r.secondaryText, outlineVariant = r.hairline, scrim = r.scrim,
            surfaceBright = r.card, surfaceDim = r.page,
            surfaceContainerLowest = r.card, surfaceContainerLow = r.card, surfaceContainer = r.card,
            surfaceContainerHigh = r.card, surfaceContainerHighest = r.raised,
        )
    }
}

@Composable
fun DispatchTheme(sheet: Boolean = false, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val r = when { !dark -> Palette.light; sheet -> Palette.darkSheet; else -> Palette.dark }
    val scheme = remember(r, dark) { Palette.scheme(r, dark) }
    val assets = LocalContext.current.assets
    val (body, display) = remember(assets) { Fonts.body(assets) to Fonts.display(assets) }
    // Figtree everywhere, sizes in sp so the system text size applies; slightly tighter than Material's default.
    val base = MaterialTheme.typography
    fun TextStyle.figtree(tracking: TextUnit) = copy(fontFamily = body, letterSpacing = tracking)
    val typography = base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = display), displayMedium = base.displayMedium.copy(fontFamily = display),
        displaySmall = base.displaySmall.copy(fontFamily = display), headlineLarge = base.headlineLarge.copy(fontFamily = display),
        headlineMedium = base.headlineMedium.copy(fontFamily = display), headlineSmall = base.headlineSmall.copy(fontFamily = display),
        titleLarge = base.titleLarge.figtree(0.sp), titleMedium = base.titleMedium.figtree(0.sp), titleSmall = base.titleSmall.figtree(0.sp),
        bodyLarge = base.bodyLarge.figtree((-0.2).sp), bodyMedium = base.bodyMedium.figtree((-0.1).sp), bodySmall = base.bodySmall.figtree(0.sp),
        labelLarge = base.labelLarge.figtree(0.sp), labelMedium = base.labelMedium.figtree(0.sp), labelSmall = base.labelSmall.figtree(0.sp),
    )
    CompositionLocalProvider(LocalRoles provides r, LocalDisplay provides display) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

/**
 * The two typefaces, embedded (SIL Open Font License, licenses next to them in assets/fonts):
 * Figtree for text, Bricolage Grotesque for titles. Both are variable fonts: each weight is an instance.
 */
private object Fonts {
    private fun instance(path: String, assets: AssetManager, w: Int, vararg extra: FontVariation.Setting) =
        Font(path, assets, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w), *extra))

    fun body(assets: AssetManager) = FontFamily(
        listOf(400, 500, 600, 700).map { instance("fonts/Figtree.ttf", assets, it) },
    )

    fun display(assets: AssetManager) = FontFamily(
        listOf(600, 700, 800).map { instance("fonts/BricolageGrotesque.ttf", assets, it, FontVariation.Setting("opsz", 32f)) },
    )
}

/** Haptics always go through the view, so the system "touch feedback" setting is respected. */
fun View.tick() = performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
fun View.confirm() = performHapticFeedback(HapticFeedbackConstants.CONFIRM)
fun View.longPress() = performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

/** Apple-like press: shrinks to 0.97 on a spring, no Material ripple. */
fun Modifier.pressable(label: String? = null, role: Role = Role.Button, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) Tokens.pressedScale else 1f, spring(dampingRatio = 0.55f, stiffness = 900f), label = "press")
    this
        .graphicsLayer { scaleX = s; scaleY = s; alpha = if (pressed) 0.85f else 1f }
        .clickable(interactionSource = src, indication = null, role = role, onClickLabel = label, onClick = onClick)
}

@Composable
fun Card(modifier: Modifier = Modifier, padding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.cardRadius)).background(Tokens.colors.card).padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), fontSize = 12.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold,
        color = Tokens.colors.secondaryText, modifier = modifier.padding(start = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(Tokens.buttonHeight).pressable(onClick = onClick)
            .clip(RoundedCornerShape(Tokens.buttonRadius)).background(Tokens.colors.accent),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Tokens.colors.onAccent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(Tokens.buttonHeight).pressable(onClick = onClick)
            .clip(RoundedCornerShape(Tokens.buttonRadius)).background(Tokens.colors.tonal),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Tokens.colors.onTonal, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

/** Small tinted capsule, for row actions like "Pin". */
@Composable
fun Capsule(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.pressable(onClick = onClick).clip(RoundedCornerShape(50)).background(Tokens.colors.tonal)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) { Text(text, color = Tokens.colors.onTonal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun GreenSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = Tokens.colors.success, checkedThumbColor = Tokens.colors.onSuccess,
            checkedIconColor = Tokens.colors.success, checkedBorderColor = Color.Transparent,
        ),
    )
}

/* ---------- glyphs: drawn, so no icon font or icon library is needed ---------- */
enum class G { Copy, Image, Save, Close, Chevron, Handle, Link, Text, File, Pin, Check }

@Composable
fun Glyph(g: G, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) { drawGlyph(g, color) }
}

fun DrawScope.drawGlyph(g: G, color: Color) {
    val u = size.minDimension / 24f
    val st = Stroke(width = 2.1f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun rr(x: Float, y: Float, w: Float, h: Float, r: Float) =
        drawRoundRect(color, p(x, y), Size(w * u, h * u), CornerRadius(r * u), style = st)
    fun line(vararg pts: Float) {
        val path = Path().apply { moveTo(pts[0] * u, pts[1] * u); var i = 2; while (i < pts.size) { lineTo(pts[i] * u, pts[i + 1] * u); i += 2 } }
        drawPath(path, color, style = st)
    }
    when (g) {
        G.Copy -> { rr(8f, 8f, 12f, 13f, 3f); line(5f, 15f, 4f, 15f, 4f, 6f, 4f, 5f, 5f, 4f, 13f, 4f, 15f, 4f, 16f, 5f) }
        G.Image -> { rr(3f, 5f, 18f, 14f, 3f); line(3.5f, 17f, 9f, 11.5f, 13f, 15.5f, 15.5f, 13f, 20.5f, 18f); drawCircle(color, 1.8f * u, p(15.5f, 9.5f)) }
        G.Save -> { line(12f, 4f, 12f, 15f); line(7.5f, 10.5f, 12f, 15f, 16.5f, 10.5f); line(4f, 15f, 4f, 19f, 5f, 20f, 19f, 20f, 20f, 19f, 20f, 15f) }
        G.Close -> { line(7f, 7f, 17f, 17f); line(17f, 7f, 7f, 17f) }
        G.Chevron -> line(7f, 9.5f, 12f, 14.5f, 17f, 9.5f)
        G.Handle -> { line(5f, 8f, 19f, 8f); line(5f, 12f, 19f, 12f); line(5f, 16f, 19f, 16f) }
        G.Link -> {
            line(10.5f, 13.5f, 13.5f, 10.5f)
            line(12f, 7f, 14f, 5f, 16.5f, 4.5f, 18.5f, 5.5f, 19.5f, 7.5f, 19f, 10f, 17f, 12f)
            line(12f, 17f, 10f, 19f, 7.5f, 19.5f, 5.5f, 18.5f, 4.5f, 16.5f, 5f, 14f, 7f, 12f)
        }
        G.Text -> { line(5f, 7f, 19f, 7f); line(5f, 12f, 19f, 12f); line(5f, 17f, 14f, 17f) }
        G.File -> { line(14f, 3.5f, 6f, 3.5f, 5f, 4.5f, 5f, 19.5f, 6f, 20.5f, 18f, 20.5f, 19f, 19.5f, 19f, 8.5f, 14f, 3.5f, 14f, 8.5f, 19f, 8.5f) }
        G.Pin -> { line(9f, 4f, 15f, 4f); line(10f, 4f, 9.5f, 10f, 7f, 13f, 17f, 13f, 14.5f, 10f, 14f, 4f); line(12f, 13f, 12f, 20f) }
        G.Check -> line(6f, 12.5f, 10f, 16.5f, 18f, 8f)
    }
}
