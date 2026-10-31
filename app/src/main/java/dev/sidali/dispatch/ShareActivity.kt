package dev.sidali.dispatch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * The share target. Opens as a sheet over the app you share from: preview, quick actions, your
 * favorites in your order, then every other app that can receive this content.
 */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val payload = Payload.from(intent)
        if (payload.isEmpty) {
            Toast.makeText(this, R.string.nothing_to_share, Toast.LENGTH_SHORT).show()
            finish(); return
        }
        setContent { DispatchTheme(sheet = true) { ShareSheet(payload, intent) } }
    }
}

private class Lists(val favorites: List<Target>, val others: List<Target>, val last: Target?)

@Composable
private fun ShareSheet(payload: Payload, shared: Intent) {
    val ctx = LocalContext.current
    val act = ctx as ComponentActivity
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var choices by remember { mutableStateOf(Store.choices(ctx)) }
    val candidates by produceState<List<Target>?>(null) { value = withContext(Dispatchers.Default) { Targets.forShare(ctx, shared) } }
    val lists = candidates?.let { c ->
        val a = Arrange.arrange(c, ctx.packageName, choices, Store.last(ctx))
        Lists(a.favorites, a.others, a.last?.takeIf { l -> a.favorites.firstOrNull()?.key != l.key })
    }
    fun update(c: Choices) { choices = c; Store.setChoices(ctx, c) }

    // Sheet motion: slides up on a spring, follows the finger down, closes past a threshold.
    val offset = remember { Animatable(2000f) }
    val scrim by animateFloatAsState(if (offset.targetValue < 1000f) 0.32f else 0f, tween(250), label = "scrim")
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { offset.animateTo(0f, spring(dampingRatio = 0.86f, stiffness = 380f)) }
    fun close(then: () -> Unit = {}) {
        if (closing) return
        closing = true
        scope.launch { offset.animateTo(2000f, tween(220)); then(); act.finish() }
    }
    // Predictive back: the sheet follows the back swipe down, then closes, or springs back if the swipe is cancelled.
    val backPeek = with(density) { 96.dp.toPx() }
    PredictiveBackHandler(enabled = !closing) { events ->
        try {
            events.collect { e -> offset.snapTo(backPeek * e.progress) }
            close()
        } catch (e: CancellationException) {
            scope.launch { offset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f)) }
            throw e
        }
    }

    fun send(t: Target) {
        view.confirm()
        if (Targets.launch(act, shared, t.key)) {
            Store.used(ctx, t.key)
            act.finish()
        } else {
            Toast.makeText(ctx, ctx.getString(R.string.open_failed, t.label), Toast.LENGTH_SHORT).show()
        }
    }

    val saveOne = rememberLauncherForActivityResult(CreateDocument()) { dest ->
        if (dest != null) scope.launch { done(ctx, withContext(Dispatchers.IO) { Saver.one(ctx, payload, dest) }, R.string.saved, R.string.save_failed) { close() } }
    }
    val saveMany = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) scope.launch { done(ctx, withContext(Dispatchers.IO) { Saver.many(ctx, payload, tree) }, R.string.saved, R.string.save_failed) { close() } }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Tokens.colors.scrim.copy(alpha = scrim))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
        )
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars), contentAlignment = Alignment.BottomCenter) {
            val maxH = maxHeight * 0.92f
            Column(
                Modifier
                    .offset { IntOffset(0, offset.value.coerceAtLeast(0f).roundToInt()) }
                    .fillMaxWidth()
                    .heightIn(max = maxH)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Tokens.colors.page)
                    // Touches on the sheet never reach the scrim behind it. Observes only, consumes
                    // nothing, so taps on the items inside still work.
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
            ) {
                // Grabber and preview: drag them down to close.
                val dragState = rememberDraggableState { d -> scope.launch { offset.snapTo((offset.value + d).coerceAtLeast(0f)) } }
                Column(
                    Modifier.fillMaxWidth().draggable(
                        dragState, Orientation.Vertical,
                        onDragStopped = { v ->
                            if (offset.value > with(density) { 120.dp.toPx() } || v > 1500f) close()
                            else offset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                        },
                    ),
                ) {
                    Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(36.dp, 5.dp).clip(CircleShape).background(Tokens.colors.secondaryText.copy(alpha = 0.4f)))
                    }
                    Preview(payload) { close() }
                }
                Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    QuickActions(
                        payload,
                        onCopy = {
                            view.confirm()
                            val r = Links.cleanText(payload.text.orEmpty())
                            Clips.copyText(ctx, ctx.getString(if (payload.isLink) R.string.kind_link else R.string.clip_text), r.text)
                            val msg = if (r.removed > 0) ctx.resources.getQuantityString(R.plurals.copied_cleaned, r.removed, r.removed) else ctx.getString(R.string.copied)
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            close()
                        },
                        onCopyImage = {
                            val uri = payload.single ?: return@QuickActions
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { Clips.copyImage(ctx, ctx.getString(R.string.clip_image), uri, payload.type) }
                                if (ok) view.confirm()
                                done(ctx, ok, R.string.copied, R.string.copy_failed) { close() }
                            }
                        },
                        onSave = {
                            if (payload.uris.size > 1) saveMany.launch(null)
                            else saveOne.launch(Saver.suggestion(ctx, payload))
                        },
                    )
                    Spacer(Modifier.height(20.dp))
                    if (lists != null) TargetSections(lists, choices, ::update, ::send)
                    Spacer(Modifier.height(16.dp))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}

private fun done(ctx: Context, ok: Boolean, okMsg: Int, failMsg: Int, then: () -> Unit) {
    Toast.makeText(ctx, if (ok) okMsg else failMsg, Toast.LENGTH_SHORT).show()
    if (ok) then()
}

@Composable
private fun Preview(p: Payload, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val thumb by produceState<ImageBitmap?>(null, p) {
        val u = p.uris.firstOrNull() ?: return@produceState
        value = withContext(Dispatchers.IO) { ctx.contentResolver.thumbnail(u, 192)?.asImageBitmap() }
    }
    val info by produceState<FileInfo?>(null, p) {
        val u = p.single ?: return@produceState
        value = withContext(Dispatchers.IO) { ctx.contentResolver.info(u) }
    }
    val kind = stringResource(
        when {
            p.isLink -> R.string.kind_link
            p.uris.isEmpty() -> R.string.kind_text
            p.isImage -> R.string.kind_image
            p.isVideo -> R.string.kind_video
            else -> R.string.kind_file
        },
    )
    val title = when {
        p.uris.size > 1 -> pluralStringResource(
            when { p.type.startsWith("image/") -> R.plurals.images_count; p.type.startsWith("video/") -> R.plurals.videos_count; else -> R.plurals.files_count },
            p.uris.size, p.uris.size,
        )
        p.uris.isEmpty() -> p.text.orEmpty().trim().ifEmpty { p.subject.orEmpty() }
        else -> info?.name ?: p.subject ?: kind
    }
    val subtitle = when {
        p.isLink -> Links.host(p.text!!.trim()).removePrefix("www.")
        p.uris.size == 1 && info?.size != null -> stringResource(R.string.kind_and_size, kind, Formatter.formatShortFileSize(ctx, info!!.size!!))
        p.uris.isEmpty() && !p.subject.isNullOrBlank() -> p.subject
        p.uris.size > 1 -> null
        else -> kind
    }?.takeIf { it != title }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                .background(Tokens.colors.badge),
            contentAlignment = Alignment.Center,
        ) {
            val b = thumb
            if (b != null) Image(b, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Glyph(
                when { p.isLink -> G.Link; p.uris.isEmpty() -> G.Text; p.isImage -> G.Image; else -> G.File },
                Tokens.colors.onBadge, Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                overflow = TextOverflow.Ellipsis, color = Tokens.colors.text,
            )
            if (!subtitle.isNullOrBlank()) Text(
                subtitle, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Tokens.colors.secondaryText,
            )
        }
        Spacer(Modifier.width(8.dp))
        val closeLabel = stringResource(R.string.close)
        Box(
            Modifier.size(44.dp).pressable(closeLabel, onClick = onClose).semantics { contentDescription = closeLabel },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Tokens.colors.raised), contentAlignment = Alignment.Center) {
                Glyph(G.Close, Tokens.colors.secondaryText, Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun QuickActions(p: Payload, onCopy: () -> Unit, onCopyImage: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!p.text.isNullOrBlank()) ActionTile(G.Copy, stringResource(R.string.copy), Modifier.weight(1f), onCopy)
        if (p.isImage) ActionTile(G.Image, stringResource(R.string.copy_image), Modifier.weight(1f), onCopyImage)
        ActionTile(G.Save, stringResource(R.string.save), Modifier.weight(1f), onSave)
    }
}

@Composable
private fun ActionTile(g: G, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(72.dp).pressable(onClick = onClick).clip(RoundedCornerShape(16.dp)).background(Tokens.colors.card),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Glyph(g, Tokens.colors.glyph, Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Tokens.colors.text, maxLines = 1)
    }
}

@Composable
private fun TargetSections(lists: Lists, choices: Choices, update: (Choices) -> Unit, send: (Target) -> Unit) {
    val view = LocalView.current
    if (lists.favorites.isEmpty() && lists.others.isEmpty() && lists.last == null) {
        Card { Text(stringResource(R.string.no_targets), fontSize = 15.sp, color = Tokens.colors.secondaryText) }
        return
    }
    val sendLabel = stringResource(R.string.send_to)
    if (lists.last != null) {
        SectionLabel(stringResource(R.string.last_used))
        Card(padding = 0.dp) {
            TargetRow(lists.last, Modifier.pressable(sendLabel) { send(lists.last) })
        }
        Spacer(Modifier.height(20.dp))
    }
    SectionLabel(stringResource(R.string.favorites))
    Card(padding = 8.dp) {
        if (lists.favorites.isEmpty()) {
            Text(
                stringResource(R.string.share_empty_favorites), fontSize = 15.sp, lineHeight = 20.sp,
                color = Tokens.colors.secondaryText, modifier = Modifier.padding(12.dp),
            )
        } else {
            val earlier = stringResource(R.string.move_earlier); val later = stringResource(R.string.move_later)
            val unpin = stringResource(R.string.unpin)
            ReorderGrid(
                lists.favorites, { it.key }, columns = 4, cellHeight = 100.dp,
                onTap = send,
                onReorder = { keys -> update(choices.reorderVisible(keys)) },
                tapLabel = sendLabel,
                actions = { t ->
                    val keys = lists.favorites.map { it.key }; val i = keys.indexOf(t.key)
                    fun swap(j: Int): Boolean {
                        if (j !in keys.indices) return false
                        update(choices.reorderVisible(keys.toMutableList().apply { add(j, removeAt(i)) })); return true
                    }
                    listOf(
                        CustomAccessibilityAction(earlier) { swap(i - 1) },
                        CustomAccessibilityAction(later) { swap(i + 1) },
                        CustomAccessibilityAction(unpin) { update(choices.unpin(t.key)); true },
                    )
                },
            ) { t, _ -> GridCell(t) }
        }
    }
    if (lists.favorites.size > 1) Text(
        stringResource(R.string.share_hint), fontSize = 13.sp, color = Tokens.colors.secondaryText,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp),
    )

    if (lists.others.isEmpty()) return
    Spacer(Modifier.height(20.dp))
    var open by rememberSaveable { mutableStateOf(lists.favorites.isEmpty()) }
    val rot by animateFloatAsState(if (open) 0f else -90f, spring(dampingRatio = 0.8f, stiffness = 600f), label = "chevron")
    Row(
        Modifier.fillMaxWidth().pressable { open = !open; view.tick() }.padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.other_apps).uppercase() + "  " + lists.others.size, fontSize = 12.sp, letterSpacing = 0.6.sp,
            fontWeight = FontWeight.SemiBold, color = Tokens.colors.secondaryText, modifier = Modifier.weight(1f),
        )
        Glyph(G.Chevron, Tokens.colors.secondaryText, Modifier.size(18.dp).rotate(rot))
    }
    AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Card(padding = 0.dp) {
            lists.others.forEachIndexed { i, t -> key(t.key) {
                if (i > 0) Divider()
                var menu by remember { mutableStateOf(false) }
                TargetRow(t, Modifier.pressable(sendLabel) { send(t) }) {
                    Capsule(stringResource(R.string.pin)) { view.tick(); update(choices.pin(t.key)) }
                    Spacer(Modifier.width(4.dp))
                    MoreButton(menu, { menu = it }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.pin)) }, onClick = { menu = false; update(choices.pin(t.key)) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.hide)) }, onClick = { menu = false; update(choices.hide(t.key)) })
                    }
                }
            } }
        }
    }
}

/** "..." button with its menu anchored to it. */
@Composable
fun MoreButton(open: Boolean, setOpen: (Boolean) -> Unit, items: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = Tokens.colors.secondaryText
    Box(Modifier.size(36.dp).pressable(stringResource(R.string.more)) { setOpen(true) }, contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { Box(Modifier.size(4.dp).clip(CircleShape).background(c)) }
        }
        DropdownMenu(open, onDismissRequest = { setOpen(false) }, content = items)
    }
}

/** ACTION_CREATE_DOCUMENT with the type chosen at launch time. */
private class CreateDocument : ActivityResultContract<Pair<String, String>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, String>) =
        Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(input.first).putExtra(Intent.EXTRA_TITLE, input.second)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}

/** "Save" through the Storage Access Framework: the user picks where, Dispatch needs no storage permission. */
private object Saver {
    fun suggestion(ctx: Context, p: Payload): Pair<String, String> {
        val u = p.single
        if (u == null) return "text/plain" to ctx.getString(R.string.default_text_name)
        val name = ctx.contentResolver.info(u).name ?: "Shared file"
        val type = ctx.contentResolver.getType(u) ?: p.type.takeUnless { it.contains('*') } ?: "application/octet-stream"
        return type to name
    }

    fun one(ctx: Context, p: Payload, dest: Uri): Boolean = try {
        ctx.contentResolver.openOutputStream(dest, "w")?.use { out ->
            val u = p.single
            if (u == null) out.write(p.text.orEmpty().toByteArray())
            else ctx.contentResolver.openInputStream(u)?.use { it.copyTo(out) } ?: return false
        } != null
    } catch (_: Exception) { false }

    fun many(ctx: Context, p: Payload, tree: Uri): Boolean {
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        var allOk = true
        p.uris.forEachIndexed { i, u ->
            val name = ctx.contentResolver.info(u).name ?: "Shared file ${i + 1}"
            val type = ctx.contentResolver.getType(u) ?: "application/octet-stream"
            val ok = try {
                val dest = DocumentsContract.createDocument(ctx.contentResolver, parent, type, name)
                dest != null && ctx.contentResolver.openOutputStream(dest, "w")?.use { out ->
                    ctx.contentResolver.openInputStream(u)?.use { it.copyTo(out) }
                } != null
            } catch (_: Exception) { false }
            allOk = allOk && ok
        }
        return allOk
    }
}
