package dev.sidali.dispatch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A small grid (or list, with one column) the user reorders by holding and dragging an item.
 * Tap = [onTap]. Every move under the finger ticks; the new order is committed once, on release,
 * through [onReorder]. Screen readers get the same moves as custom actions ([actions]).
 * Not lazy on purpose: favorites are a handful of items, and a plain layout keeps the drag exact.
 */
@Composable
fun <T> ReorderGrid(
    items: List<T>,
    keyOf: (T) -> String,
    columns: Int,
    cellHeight: Dp,
    onTap: (T) -> Unit,
    onReorder: (List<String>) -> Unit,
    tapLabel: String? = null,
    actions: (T) -> List<CustomAccessibilityAction> = { emptyList() },
    cell: @Composable (item: T, lifted: Boolean) -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val byKey = items.associateBy(keyOf)
    // Local order while dragging, so fast moves never wait for the parent to recompose.
    val order = remember { mutableStateListOf<String>() }
    var dragKey by remember { mutableStateOf<String?>(null) }
    if (dragKey == null && order.toList() != items.map(keyOf)) { order.clear(); order.addAll(items.map(keyOf)) }
    val shown = order.filter { it in byKey }
    val rows = (shown.size + columns - 1) / columns

    BoxWithConstraints(Modifier.fillMaxWidth().height(cellHeight * rows)) {
        val cellW = with(density) { maxWidth.toPx() } / columns
        val cellH = with(density) { cellHeight.toPx() }
        fun slot(i: Int) = IntOffset(((i % columns) * cellW).roundToInt(), ((i / columns) * cellH).roundToInt())
        val latestOnTap by rememberUpdatedState(onTap)
        val latestOnReorder by rememberUpdatedState(onReorder)

        shown.forEachIndexed { index, k ->
            val item = byKey.getValue(k)
            key(k) {
                val anim = remember { Animatable(slot(index), IntOffset.VectorConverter) }
                val scope = rememberCoroutineScope()
                val lifted = dragKey == k
                var pressed by remember { mutableStateOf(false) }
                val target = slot(index)
                LaunchedEffect(target, lifted) {
                    if (!lifted) anim.animateTo(target, spring(dampingRatio = 0.8f, stiffness = 500f))
                }
                val scale by animateFloatAsState(
                    when { lifted -> 1.08f; pressed -> Tokens.pressedScale; else -> 1f },
                    spring(dampingRatio = 0.55f, stiffness = 700f), label = "scale",
                )
                Box(
                    Modifier
                        .offset { anim.value }
                        .size(maxWidth / columns, cellHeight)
                        .zIndex(if (lifted) 1f else 0f)
                        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (pressed && !lifted) 0.85f else 1f }
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            onClick(tapLabel) { latestOnTap(item); true }
                            customActions = actions(item)
                        }
                        .pointerInput(k, cellW, cellH, columns) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                pressed = true
                                val long = awaitLongPressOrCancellation(down.id)
                                if (long == null) {
                                    pressed = false
                                    val up = currentEvent.changes.firstOrNull { it.id == down.id }
                                    // A tap only if the finger lifted where it went down: a swipe that scrolls the page is not a tap.
                                    val still = up != null && (up.position - down.position).getDistance() < viewConfiguration.touchSlop
                                    if (up != null && still && up.changedToUp() && !up.isConsumed) { up.consume(); latestOnTap(item) }
                                    return@awaitEachGesture
                                }
                                // Held: lift the item and follow the finger.
                                view.longPress()
                                dragKey = k
                                var pos = Offset(anim.value.x.toFloat(), anim.value.y.toFloat())
                                drag(long.id) { change ->
                                    pos += change.positionChange()
                                    change.consume()
                                    scope.launch { anim.snapTo(IntOffset(pos.x.roundToInt(), pos.y.roundToInt())) }
                                    val col = ((pos.x + cellW / 2) / cellW).toInt().coerceIn(0, columns - 1)
                                    val rowsNow = (order.size + columns - 1) / columns
                                    val row = ((pos.y + cellH / 2) / cellH).toInt().coerceIn(0, rowsNow - 1)
                                    val to = (row * columns + col).coerceIn(0, order.size - 1)
                                    val from = order.indexOf(k)
                                    if (from >= 0 && to != from) { order.add(to, order.removeAt(from)); view.tick() }
                                }
                                pressed = false
                                dragKey = null
                                latestOnReorder(order.toList())
                            }
                        },
                ) { cell(item, lifted) }
            }
        }
    }
}
