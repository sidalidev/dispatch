package dev.sidali.dispatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DispatchTheme { App() } }
    }
}

/** Home, About and its pages. Back goes up one level, with the system's predictive back animation. */
@Composable
private fun App() {
    var page by rememberSaveable { mutableStateOf(Page.Home) }
    // Bumped by Erase everything: Home is composed afresh, as on first launch.
    var epoch by rememberSaveable { mutableIntStateOf(0) }
    val holder = rememberSaveableStateHolder()
    BackHandler(page != Page.Home) { page = page.parent ?: Page.Home }
    AnimatedContent(
        page,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            val move = spring<IntOffset>(dampingRatio = 1f, stiffness = 500f)
            (slideInHorizontally(move) { w -> if (forward) w / 3 else -w / 3 } + fadeIn(tween(180))) togetherWith
                (slideOutHorizontally(move) { w -> if (forward) -w / 3 else w / 3 } + fadeOut(tween(120)))
        },
        label = "page",
    ) { p ->
        holder.SaveableStateProvider("${p.name}-$epoch") {
            when (p) {
                Page.Home -> Home { page = Page.About }
                Page.About -> AboutScreen(go = { page = it }, onBack = { page = Page.Home }, onErased = { epoch++; page = Page.Home })
                Page.Permissions -> PermissionsScreen { page = Page.About }
                Page.Leaves -> LeavesScreen { page = Page.About }
                Page.Privacy -> DocumentScreen(stringResource(R.string.privacy_policy), "PRIVACY.md") { page = Page.About }
                Page.WhatsNew -> DocumentScreen(stringResource(R.string.whats_new), "CHANGELOG.md") { page = Page.About }
            }
        }
    }
}

@Composable
private fun Home(onAbout: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var choices by remember { mutableStateOf(Store.choices(ctx)) }
    var all by remember { mutableStateOf<List<Target>?>(null) }
    var rememberLast by remember { mutableStateOf(Store.rememberLast(ctx)) }
    var pinSheet by rememberSaveable { mutableStateOf(false) }

    // Apps come and go, and the share screen may have changed the list: refresh on every resume.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                choices = Store.choices(ctx); rememberLast = Store.rememberLast(ctx)
                scope.launch { all = withContext(Dispatchers.Default) { Targets.all(ctx) } }
            }
        }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }
    fun update(c: Choices) { choices = c; Store.setChoices(ctx, c) }

    Surface(color = Tokens.colors.page, modifier = Modifier.fillMaxSize()) { Box {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Hero(Modifier.fillMaxWidth().widthIn(max = 420.dp).height(170.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.app_name), fontSize = 34.sp, fontWeight = FontWeight.Bold,
                fontFamily = Tokens.display, letterSpacing = Tokens.titleTracking, color = Tokens.colors.text,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.tagline), fontSize = 17.sp, lineHeight = 23.sp, textAlign = TextAlign.Center,
                color = Tokens.colors.secondaryText, modifier = Modifier.widthIn(max = 320.dp),
            )
            Spacer(Modifier.height(28.dp))

            Column(Modifier.fillMaxWidth()) {
                Steps()
                Spacer(Modifier.height(14.dp))
                PrimaryButton(stringResource(R.string.pin_button)) { pinSheet = true }
                Spacer(Modifier.height(32.dp))

                val lists = all?.let { Arrange.arrange(it, ctx.packageName, choices) }
                Favorites(lists?.favorites, choices, ::update)
                Spacer(Modifier.height(28.dp))
                if (lists != null && lists.others.isNotEmpty()) {
                    AddApps(lists.others, choices, ::update)
                    Spacer(Modifier.height(28.dp))
                }
                val hidden = all?.filter { it.key in choices.hidden }.orEmpty()
                if (hidden.isNotEmpty()) {
                    Hidden(hidden) { t -> view.tick(); update(choices.unhide(t.key)) }
                    Spacer(Modifier.height(28.dp))
                }

                SectionLabel(stringResource(R.string.options))
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Title(stringResource(R.string.remember_title))
                            Spacer(Modifier.height(2.dp))
                            Text(stringResource(R.string.remember_body), fontSize = 14.sp, lineHeight = 19.sp, color = Tokens.colors.secondaryText)
                        }
                        Spacer(Modifier.width(12.dp))
                        GreenSwitch(rememberLast) { v -> view.tick(); Store.setRememberLast(ctx, v); rememberLast = v }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Card(padding = 0.dp) {
                Row(
                    Modifier.fillMaxWidth().pressable { view.tick(); onAbout() }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Title(stringResource(R.string.about_row))
                        Text(stringResource(R.string.about_row_detail), fontSize = 14.sp, color = Tokens.colors.secondaryText)
                    }
                    Glyph(G.Chevron, Tokens.colors.secondaryText.copy(alpha = 0.7f), Modifier.size(18.dp).rotate(-90f))
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        // Content scrolls under a status bar tinted like the page, so the clock stays readable.
        Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(Tokens.colors.page.copy(alpha = 0.94f)))
    } }

    if (pinSheet) PinSheet { pinSheet = false }
}

@Composable
private fun Steps() {
    SectionLabel(stringResource(R.string.how_title))
    Card {
        val steps = listOf(
            R.string.step1_title to R.string.step1_body,
            R.string.step2_title to R.string.step2_body,
            R.string.step3_title to R.string.step3_body,
        )
        steps.forEachIndexed { i, (title, body) ->
            if (i > 0) Spacer(Modifier.height(16.dp))
            Row {
                StepNumber(i + 1)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Title(stringResource(title))
                    Text(stringResource(body), fontSize = 15.sp, lineHeight = 20.sp, color = Tokens.colors.secondaryText)
                }
            }
        }
    }
}

@Composable
private fun Favorites(favorites: List<Target>?, choices: Choices, update: (Choices) -> Unit) {
    val view = LocalView.current
    SectionLabel(stringResource(R.string.favorites))
    Card(padding = 0.dp) {
        when {
            // While the list loads, keep the room of the pinned rows so the page does not jump.
            favorites == null -> Spacer(Modifier.height(60.dp * choices.pinned.size.coerceIn(1, 8)))
            favorites.isEmpty() -> Text(
                stringResource(R.string.favorites_empty), fontSize = 15.sp, lineHeight = 20.sp,
                color = Tokens.colors.secondaryText, modifier = Modifier.padding(20.dp),
            )
            else -> {
                var menuFor by remember { mutableStateOf<String?>(null) }
                val keys = favorites.map { it.key }
                fun moveBy(key: String, by: Int): Boolean {
                    val i = keys.indexOf(key); val j = i + by
                    if (i < 0 || j !in keys.indices) return false
                    view.tick(); update(choices.reorderVisible(keys.toMutableList().apply { add(j, removeAt(i)) })); return true
                }
                val up = stringResource(R.string.move_up); val down = stringResource(R.string.move_down)
                val unpin = stringResource(R.string.unpin); val more = stringResource(R.string.more)
                ReorderGrid(
                    favorites, { it.key }, columns = 1, cellHeight = 60.dp,
                    onTap = { menuFor = it.key },
                    onReorder = { k -> update(choices.reorderVisible(k)) },
                    tapLabel = more,
                    actions = { t ->
                        listOf(
                            CustomAccessibilityAction(up) { moveBy(t.key, -1) },
                            CustomAccessibilityAction(down) { moveBy(t.key, 1) },
                            CustomAccessibilityAction(unpin) { update(choices.unpin(t.key)); true },
                        )
                    },
                ) { t, lifted ->
                    Box(
                        Modifier.fillMaxSize().background(if (lifted) Tokens.colors.raised else Color.Transparent),
                    ) {
                        TargetRow(t) { Glyph(G.Handle, Tokens.colors.secondaryText.copy(alpha = 0.6f)) }
                        if (keys.first() != t.key && !lifted) Box(Modifier.align(Alignment.TopStart)) { Divider() }
                        DropdownMenu(menuFor == t.key, onDismissRequest = { menuFor = null }) {
                            val i = keys.indexOf(t.key)
                            if (i > 0) DropdownMenuItem(text = { Text(up) }, onClick = { menuFor = null; moveBy(t.key, -1) })
                            if (i < keys.size - 1) DropdownMenuItem(text = { Text(down) }, onClick = { menuFor = null; moveBy(t.key, 1) })
                            DropdownMenuItem(text = { Text(unpin) }, onClick = { menuFor = null; update(choices.unpin(t.key)) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.hide)) }, onClick = { menuFor = null; update(choices.hide(t.key)) })
                        }
                    }
                }
            }
        }
    }
    if (!favorites.isNullOrEmpty()) Text(
        stringResource(R.string.favorites_hint), fontSize = 13.sp, color = Tokens.colors.secondaryText,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp),
    )
}

@Composable
private fun AddApps(others: List<Target>, choices: Choices, update: (Choices) -> Unit) {
    val view = LocalView.current
    var open by rememberSaveable { mutableStateOf(choices.pinned.isEmpty()) }
    val rot by animateFloatAsState(if (open) 0f else -90f, spring(dampingRatio = 0.8f, stiffness = 600f), label = "chevron")
    Row(
        Modifier.fillMaxWidth().pressable { open = !open; view.tick() }.padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.add_apps).uppercase(), fontSize = 12.sp, letterSpacing = 0.6.sp,
                fontWeight = FontWeight.SemiBold, color = Tokens.colors.secondaryText,
            )
            Text(
                pluralStringResource(R.plurals.apps_count, others.size, others.size), fontSize = 13.sp,
                color = Tokens.colors.secondaryText,
            )
        }
        Glyph(G.Chevron, Tokens.colors.secondaryText, Modifier.size(18.dp).rotate(rot))
    }
    AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Column {
            Card(padding = 0.dp) {
                others.forEachIndexed { i, t -> key(t.key) {
                    if (i > 0) Divider()
                    var menu by remember { mutableStateOf(false) }
                    TargetRow(t) {
                        Capsule(stringResource(R.string.pin)) { view.tick(); update(choices.pin(t.key)) }
                        Spacer(Modifier.width(4.dp))
                        MoreButton(menu, { menu = it }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.pin)) }, onClick = { menu = false; update(choices.pin(t.key)) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.hide)) }, onClick = { menu = false; update(choices.hide(t.key)) })
                        }
                    }
                } }
            }
            Text(
                stringResource(R.string.end_of_list), fontSize = 13.sp, color = Tokens.colors.secondaryText,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun Hidden(hidden: List<Target>, onShow: (Target) -> Unit) {
    SectionLabel(stringResource(R.string.hidden))
    Card(padding = 0.dp) {
        hidden.forEachIndexed { i, t ->
            if (i > 0) Divider()
            TargetRow(t) { Capsule(stringResource(R.string.show)) { onShow(t) } }
        }
    }
    Text(
        stringResource(R.string.hidden_body), fontSize = 13.sp, color = Tokens.colors.secondaryText,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Tokens.colors.page) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.pin_title), fontSize = 28.sp, fontWeight = FontWeight.Bold,
                fontFamily = Tokens.display, letterSpacing = Tokens.titleTracking,
                color = Tokens.colors.text, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
            )
            Card {
                listOf(R.string.pin_step1, R.string.pin_step2, R.string.pin_step3).forEachIndexed { i, s ->
                    if (i > 0) Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        StepNumber(i + 1)
                        Spacer(Modifier.width(14.dp))
                        Text(stringResource(s), fontSize = 17.sp, lineHeight = 23.sp, color = Tokens.colors.text, modifier = Modifier.weight(1f))
                    }
                }
            }
            Text(
                stringResource(R.string.pin_note), fontSize = 13.sp, lineHeight = 18.sp, color = Tokens.colors.secondaryText,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.pin_done), Modifier.weight(1f)) {
                    scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }
                }
                PrimaryButton(stringResource(R.string.pin_try), Modifier.weight(1f)) {
                    // Opens the real system share menu with a word of text, so Dispatch can be pinned right away.
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.pin_try_text))
                    ctx.startActivity(Intent.createChooser(send, null))
                }
            }
        }
    }
}
