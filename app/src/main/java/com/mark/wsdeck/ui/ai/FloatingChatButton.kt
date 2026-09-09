package com.mark.wsdeck.ui.ai

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 全域浮動聊天按鈕：可以拖到畫面任何位置（即時跟著手指，上下左右都可以），
 * 放著不動一段時間會收到最近的那個邊緣、只留一點點探出來；點探出來的部分
 * 或再拖它都會展開回來（對應 iOS 的 FloatingChatButton）。
 */
@Composable
fun FloatingChatButton(onClick: () -> Unit) {
    val size = 52.dp
    val peek = 18.dp
    val idleDelayMs = 3_000L
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val sizePx = with(density) { size.toPx() }
        val peekPx = with(density) { peek.toPx() }
        val defaultX = widthPx - sizePx / 2 - with(density) { 16.dp.toPx() }
        val defaultY = heightPx - sizePx / 2 - with(density) { 90.dp.toPx() }

        var posX by remember { mutableStateOf<Float?>(null) }
        var posY by remember { mutableStateOf<Float?>(null) }
        var dragX by remember { mutableStateOf(0f) }
        var dragY by remember { mutableStateOf(0f) }
        // null＝展開；true／false＝收在左邊／右邊
        var collapsedToLeft by remember { mutableStateOf<Boolean?>(null) }
        var lastInteraction by remember { mutableStateOf(0L) }

        val baseX = posX ?: defaultX
        val baseY = posY ?: defaultY
        val restingX = when (collapsedToLeft) {
            true -> -sizePx / 2 + peekPx
            false -> widthPx + sizePx / 2 - peekPx
            null -> baseX
        }
        val animatedX by animateFloatAsState(
            targetValue = restingX + dragX,
            animationSpec = spring(dampingRatio = 0.85f),
            label = "aiButtonX",
        )
        val animatedY = baseY + dragY

        // 放著不動 idleDelayMs 後收到最近的邊緣，不擋畫面
        LaunchedEffect(lastInteraction) {
            delay(idleDelayMs)
            collapsedToLeft = baseX < widthPx / 2
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    translationX = animatedX - sizePx / 2
                    translationY = animatedY - sizePx / 2
                    alpha = if (collapsedToLeft != null) 0.6f else 1f
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            lastInteraction = System.currentTimeMillis()
                            collapsedToLeft = null
                        },
                        onDragEnd = {
                            var nextX = (baseX + dragX).coerceIn(sizePx / 2, widthPx - sizePx / 2)
                            val nextY = (baseY + dragY).coerceIn(sizePx / 2, heightPx - sizePx / 2)
                            posX = nextX
                            posY = nextY
                            dragX = 0f
                            dragY = 0f
                            lastInteraction = System.currentTimeMillis()
                        },
                        onDragCancel = { dragX = 0f; dragY = 0f },
                    ) { change, amount ->
                        change.consume()
                        dragX += amount.x
                        dragY += amount.y
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        lastInteraction = System.currentTimeMillis()
                        if (collapsedToLeft != null) {
                            collapsedToLeft = null
                        } else {
                            onClick()
                        }
                    }
                },
        ) {
            Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.ChatBubble,
                    contentDescription = "問 AI",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
