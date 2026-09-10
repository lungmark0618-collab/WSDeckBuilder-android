package com.mark.wsdeck.ui.shared

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs

internal fun shouldNavigateBack(horizontal: Float, vertical: Float, threshold: Float): Boolean =
    horizontal < -threshold && abs(horizontal) > abs(vertical) * 1.5f

/** Only the right edge owns back. Pagers, sliders and swipe actions retain the content area. */
fun Modifier.swipeBack(enabled: Boolean = true, onBack: () -> Unit): Modifier = composed {
    val latestBack by rememberUpdatedState(onBack)
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val edge = 24.dp.toPx()
        val threshold = 80.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.x < size.width - edge) return@awaitEachGesture
            var offset = androidx.compose.ui.geometry.Offset.Zero
            var cancelled = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                offset = change.position - down.position
                if (event.changes.count { it.pressed } > 1 ||
                    (abs(offset.y) > viewConfiguration.touchSlop && abs(offset.y) > abs(offset.x))) cancelled = true
                if (!cancelled && offset.x < -viewConfiguration.touchSlop) change.consume()
            } while (event.changes.any { it.id == down.id && it.pressed })
            if (!cancelled && shouldNavigateBack(offset.x, offset.y, threshold)) latestBack()
        }
    }
}

@Composable
fun SwipeBackDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Box(Modifier.swipeBack(onBack = onDismissRequest)) { content() }
    }
}

@Composable
fun SwipeBackAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(onDismissRequest = onDismissRequest, confirmButton = confirmButton,
        modifier = modifier.swipeBack(onBack = onDismissRequest), dismissButton = dismissButton,
        icon = icon, title = title, text = text)
}
