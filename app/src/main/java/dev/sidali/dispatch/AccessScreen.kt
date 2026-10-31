package dev.sidali.dispatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.sidali.dispatch.Access.Action
import dev.sidali.dispatch.Access.Kind
import dev.sidali.dispatch.Access.Status

/**
 * Permissions: what Dispatch may do on this phone, read again each time you come back from Settings.
 * A tap on a row opens the exact Settings page, to turn it on or off: never a nag, never a loop.
 */
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    var rows by remember { mutableStateOf(Access.rows(Access.snapshot(ctx))) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) rows = Access.rows(Access.snapshot(ctx)) }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }

    Screen(stringResource(R.string.permissions), onBack) {
        Card { Line(stringResource(R.string.permissions_intro)) }
        Spacer(Modifier.height(28.dp))
        SectionLabel(stringResource(R.string.permissions_on_this_phone))
        Card(padding = 0.dp) {
            rows.forEachIndexed { i, r ->
                if (i > 0) Divider(inset = 20.dp)
                AccessRow(r) { view.tick(); Access.open(ctx, r.kind) }
            }
        }
        Text(
            stringResource(R.string.permissions_hint), fontSize = 13.sp, lineHeight = 18.sp, color = Tokens.colors.secondaryText,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
        )
        Spacer(Modifier.height(28.dp))
        SectionLabel(stringResource(R.string.permissions_without))
        Card {
            Line(stringResource(R.string.permissions_share_menu), size = 16)
            Spacer(Modifier.height(12.dp))
            Line(stringResource(R.string.permissions_visibility), size = 16)
        }
    }
}

@Composable
private fun AccessRow(r: Access.Row, onClick: () -> Unit) {
    val title = stringResource(
        when (r.kind) { Kind.Permissions -> R.string.access_permissions; Kind.Notifications -> R.string.access_notifications; Kind.Battery -> R.string.access_battery },
    )
    val why = stringResource(
        when (r.kind) { Kind.Permissions -> R.string.access_permissions_why; Kind.Notifications -> R.string.access_notifications_why; Kind.Battery -> R.string.access_battery_why },
    )
    val state = when (r.status) {
        Status.None -> stringResource(R.string.access_none)
        Status.Some -> pluralStringResource(R.plurals.access_granted, r.granted, r.granted)
        Status.On -> stringResource(R.string.access_on)
        Status.Off -> stringResource(R.string.access_off)
        Status.Optimized -> stringResource(R.string.access_optimized)
        Status.Unrestricted -> stringResource(R.string.access_unrestricted)
    }
    val action = stringResource(
        when (r.action) {
            Action.CheckInSettings -> R.string.access_check
            Action.TurnOnInSettings -> if (r.kind == Kind.Battery) R.string.access_change else R.string.access_turn_on
            Action.TurnOffInSettings -> if (r.kind == Kind.Battery) R.string.access_change else R.string.access_turn_off
        },
    )
    val on = r.status == Status.On || r.status == Status.Unrestricted || (r.status == Status.Some && r.granted > 0)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).pressable(action, onClick = onClick)
            .clearAndSetSemantics { contentDescription = "$title. $why"; stateDescription = state }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = Tokens.colors.text)
            Spacer(Modifier.height(2.dp))
            Text(why, fontSize = 14.sp, lineHeight = 19.sp, color = Tokens.colors.secondaryText)
            Spacer(Modifier.height(6.dp))
            Text(action, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Tokens.colors.accentText)
        }
        Spacer(Modifier.width(12.dp))
        // The state, in a capsule: green when something is allowed, neutral when it is not.
        Text(
            state, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (on) Tokens.colors.onSuccess else Tokens.colors.secondaryText,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Tokens.colors.success else Tokens.colors.raised)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
