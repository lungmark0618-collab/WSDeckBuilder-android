package com.mark.wsdeck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 所有共用表面讀取目前主題，背景切換時由 Compose 自動更新。 */
object AppSurface {
    val background: Color @Composable get() = MaterialTheme.colorScheme.background
    val panel: Color @Composable get() = MaterialTheme.colorScheme.surface
    val panelElevated: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val hairline: Color @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val secondaryText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
}
