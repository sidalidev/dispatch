package dev.sidali.dispatch

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.view.HapticFeedbackConstants

/** The screens of the app, in depth order: a deeper screen slides in from the right. */
enum class Page(val parent: Page?) {
    Home(null), About(Home), Permissions(About), Leaves(About), Privacy(About), WhatsNew(About)
}

/** A sub screen: back button, large title, scrolling content, status bar tinted like the page. */
@Composable
internal fun Screen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val view = LocalView.current
    Surface(color = Tokens.colors.page, modifier = Modifier.fillMaxSize()) { Box {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            val back = stringResource(R.string.back)
            Box(
                Modifier.size(40.dp).pressable(back) { view.tick(); onBack() }.clip(CircleShape).background(Tokens.colors.raised)
                    .semantics { contentDescription = back },
                contentAlignment = Alignment.Center,
            ) { Glyph(G.Chevron, Tokens.colors.text, Modifier.size(20.dp).rotate(90f)) }
            Spacer(Modifier.height(12.dp))
            Text(
                title, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = Tokens.display,
                letterSpacing = Tokens.titleTracking, lineHeight = 40.sp,
                color = Tokens.colors.text, modifier = Modifier.padding(start = 4.dp).semantics { heading() },
            )
            Spacer(Modifier.height(24.dp))
            content()
            Spacer(Modifier.height(28.dp))
        }
        Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(Tokens.colors.page.copy(alpha = 0.94f)))
    } }
}

/** A tappable row inside a card: title, optional detail, chevron when it leads somewhere. */
@Composable
private fun LinkRow(
    title: String,
    detail: String? = null,
    color: Color = Tokens.colors.text,
    chevron: Boolean = true,
    label: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).pressable(label, onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = color)
            if (detail != null) Text(detail, fontSize = 14.sp, lineHeight = 19.sp, color = Tokens.colors.secondaryText)
        }
        if (chevron) {
            Spacer(Modifier.width(12.dp))
            Glyph(G.Chevron, Tokens.colors.secondaryText.copy(alpha = 0.7f), Modifier.size(18.dp).rotate(-90f))
        }
    }
}

@Composable
internal fun Line(text: String, modifier: Modifier = Modifier, color: Color = Tokens.colors.text, size: Int = 17) {
    Text(text, fontSize = size.sp, lineHeight = (size + 6).sp, color = color, modifier = modifier)
}

/** Opens a link in the browser or the mail app: always an explicit tap, and the network is theirs, not Dispatch's. */
private fun open(ctx: Context, uri: String, subject: String? = null) {
    val i = if (uri.startsWith("mailto:")) Intent(Intent.ACTION_SENDTO, Uri.parse(uri)).apply { subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) } }
    else Intent(Intent.ACTION_VIEW, Uri.parse(uri))
    try { ctx.startActivity(i) } catch (_: ActivityNotFoundException) {
        val msg = if (uri.startsWith("mailto:")) ctx.getString(R.string.no_mail_app, Trust.CONTACT) else ctx.getString(R.string.no_browser, uri)
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
    }
}

@Composable
fun AboutScreen(go: (Page) -> Unit, onBack: () -> Unit, onErased: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    var erasing by rememberSaveable { mutableStateOf(false) }

    Screen(stringResource(R.string.about), onBack) {
        SectionLabel(stringResource(R.string.principles))
        Card(padding = 0.dp) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(R.string.principle1, R.string.principle2, R.string.principle3, R.string.principle4).forEach {
                    Row(verticalAlignment = Alignment.Top) {
                        Glyph(G.Check, Tokens.colors.glyph, Modifier.padding(top = 1.dp).size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Line(stringResource(it), Modifier.weight(1f))
                    }
                }
            }
            Divider(inset = 20.dp)
            LinkRow(stringResource(R.string.permissions), detail = stringResource(R.string.permissions_none_detail)) { view.tick(); go(Page.Permissions) }
            Divider(inset = 20.dp)
            LinkRow(stringResource(R.string.what_leaves)) { view.tick(); go(Page.Leaves) }
        }
        Spacer(Modifier.height(28.dp))

        SectionLabel(stringResource(R.string.version_title))
        Card(padding = 0.dp) {
            val version = remember { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName.orEmpty() }
            Line(stringResource(R.string.version, version), Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
            Divider(inset = 20.dp)
            LinkRow(stringResource(R.string.whats_new)) { view.tick(); go(Page.WhatsNew) }
            Divider(inset = 20.dp)
            Line(stringResource(R.string.updates_note), Modifier.padding(horizontal = 20.dp, vertical = 14.dp), color = Tokens.colors.secondaryText, size = 14)
            Divider(inset = 20.dp)
            Line(stringResource(R.string.fonts_credit), Modifier.padding(horizontal = 20.dp, vertical = 14.dp), color = Tokens.colors.secondaryText, size = 14)
        }
        Spacer(Modifier.height(28.dp))

        SectionLabel(stringResource(R.string.security))
        Card(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Line(stringResource(R.string.last_review, Trust.reviewDate(BuildConfig.SECURITY_REVIEW)))
                Line(stringResource(R.string.audit_state), color = Tokens.colors.secondaryText, size = 14)
            }
            Divider(inset = 20.dp)
            val fp = remember { Trust.signingSha256(ctx) }
            var full by rememberSaveable { mutableStateOf(false) }
            LinkRow(
                stringResource(R.string.signing_certificate),
                detail = when { fp == null -> stringResource(R.string.unavailable); full -> Trust.grouped(fp); else -> Trust.short(fp) },
                chevron = false,
                label = stringResource(if (full) R.string.show_less else R.string.show_full),
            ) { if (fp != null) { view.tick(); full = !full } }
            Divider(inset = 20.dp)
            val subject = stringResource(R.string.security_subject)
            LinkRow(stringResource(R.string.report_issue), color = Tokens.colors.accentText) { view.tick(); open(ctx, "mailto:${Trust.CONTACT}", subject) }
        }
        Spacer(Modifier.height(28.dp))

        SectionLabel(stringResource(R.string.links))
        Card(padding = 0.dp) {
            LinkRow(stringResource(R.string.privacy_policy)) { view.tick(); go(Page.Privacy) }
            Divider(inset = 20.dp)
            val subject = stringResource(R.string.app_name)
            LinkRow(stringResource(R.string.contact), detail = Trust.CONTACT) { view.tick(); open(ctx, "mailto:${Trust.CONTACT}", subject) }
        }
        Spacer(Modifier.height(28.dp))

        Card(padding = 0.dp) {
            LinkRow(stringResource(R.string.erase_title), color = Tokens.colors.danger, chevron = false) { view.tick(); erasing = true }
        }
        Text(
            stringResource(R.string.erase_hint), fontSize = 13.sp, lineHeight = 18.sp, color = Tokens.colors.secondaryText,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
        )
        Spacer(Modifier.height(28.dp))

        Text(
            stringResource(R.string.footer), fontSize = 13.sp, color = Tokens.colors.secondaryText, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().pressable(stringResource(R.string.open_site)) { open(ctx, Trust.SITE) }.padding(vertical = 8.dp),
        )
    }

    if (erasing) EraseDialog(onCancel = { erasing = false }) {
        Trust.eraseEverything(ctx)
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        Toast.makeText(ctx, R.string.erased, Toast.LENGTH_SHORT).show()
        erasing = false
        onErased()
    }
}

/** The strong confirmation: the button only works once ERASE is typed. */
@Composable
private fun EraseDialog(onCancel: () -> Unit, onErase: () -> Unit) {
    val word = stringResource(R.string.erase_word)
    var typed by rememberSaveable { mutableStateOf("") }
    val ready = typed.trim() == word
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Dialog(onDismissRequest = onCancel) {
        Card {
            Text(
                stringResource(R.string.erase_confirm_title), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                fontFamily = Tokens.display, letterSpacing = Tokens.titleTracking, color = Tokens.colors.text,
            )
            Spacer(Modifier.height(10.dp))
            Line(stringResource(R.string.erase_confirm_body), size = 15)
            Spacer(Modifier.height(16.dp))
            Line(stringResource(R.string.erase_type, word), color = Tokens.colors.secondaryText, size = 15)
            Spacer(Modifier.height(8.dp))
            val fieldLabel = stringResource(R.string.erase_field)
            BasicTextField(
                typed, { typed = it.uppercase().take(12) }, singleLine = true,
                textStyle = TextStyle(fontSize = 17.sp, color = Tokens.colors.text, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                cursorBrush = SolidColor(Tokens.colors.accentText),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).clip(RoundedCornerShape(12.dp)).background(Tokens.colors.raised)
                    .padding(horizontal = 14.dp, vertical = 14.dp).semantics { contentDescription = fieldLabel },
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.cancel), Modifier.weight(1f), onCancel)
                Box(
                    Modifier.weight(1f).height(Tokens.buttonHeight)
                        .then(if (ready) Modifier.pressable(onClick = onErase) else Modifier)
                        .clip(RoundedCornerShape(Tokens.buttonRadius))
                        .background(if (ready) Tokens.colors.danger else Tokens.colors.raised),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.erase), fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        color = if (ready) Tokens.colors.onDanger else Tokens.colors.secondaryText,
                    )
                }
            }
        }
    }
}

@Composable
fun LeavesScreen(onBack: () -> Unit) {
    Screen(stringResource(R.string.what_leaves), onBack) {
        Card { Line(stringResource(R.string.leaves_nothing)) }
        Spacer(Modifier.height(28.dp))
        SectionLabel(stringResource(R.string.leaves_when_you_choose))
        Bullets(listOf(R.string.leaves_copy, R.string.leaves_save, R.string.leaves_send))
        Spacer(Modifier.height(28.dp))
        SectionLabel(stringResource(R.string.leaves_stays))
        Bullets(listOf(R.string.stays_choices, R.string.stays_last, R.string.stays_images, R.string.stays_backup))
    }
}

@Composable
private fun Bullets(items: List<Int>) {
    Card {
        items.forEachIndexed { i, s ->
            if (i > 0) Spacer(Modifier.height(12.dp))
            Bullet(stringResource(s))
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 9.dp).size(6.dp).clip(CircleShape).background(Tokens.colors.glyph))
        Spacer(Modifier.width(12.dp))
        Line(text, Modifier.weight(1f), size = 16)
    }
}

/** Shows a bundled Markdown document (CHANGELOG.md, PRIVACY.md) in the app's own type. */
@Composable
fun DocumentScreen(title: String, file: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val blocks = remember(file) { Block.parse(Trust.document(ctx, file)) }
    Screen(title, onBack) {
        Column(Modifier.widthIn(max = 640.dp).padding(horizontal = 4.dp)) {
            blocks.forEachIndexed { i, b ->
                when (b) {
                    is Block.Title -> Unit // The screen title already says it.
                    is Block.Heading -> {
                        if (i > 1) Spacer(Modifier.height(20.dp))
                        Text(
                            b.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Tokens.display,
                            letterSpacing = Tokens.titleTracking, color = Tokens.colors.text, modifier = Modifier.semantics { heading() },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    is Block.Bullet -> { Bullet(b.text); Spacer(Modifier.height(8.dp)) }
                    is Block.Para -> { Line(b.text, size = 16); Spacer(Modifier.height(10.dp)) }
                }
            }
        }
    }
}
