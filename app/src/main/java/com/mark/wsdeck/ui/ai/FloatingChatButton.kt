package com.mark.wsdeck.ui.ai

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** 全域浮動聊天按鈕，疊在畫面右下角、Tab Bar 上方；拖曳可以調整上下位置，
 *  避免擋住畫面裡剛好在那個高度的重要按鈕（對應 iOS 的 FloatingChatButton） */
@Composable
fun FloatingChatButton(onClick: () -> Unit) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    Box(Modifier.padding(end = 16.dp, bottom = 90.dp)) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer { translationY = offsetY }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount.y
                    }
                },
        ) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = "問 AI",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
