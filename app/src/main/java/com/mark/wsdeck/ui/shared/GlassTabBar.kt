package com.mark.wsdeck.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mark.wsdeck.ui.theme.AppSurface

data class GlassTabBarItem<T>(val id: T, val title: String, val icon: ImageVector)

/**
 * 浮動玻璃感分頁列，對應 iOS 的 GlassTabBar.swift。放進 Scaffold 的 bottomBar
 * 插槽即可——不像 iOS 得自己手動算安全區留白，Compose 的 Scaffold 本來就會
 * 依 bottomBar 實際量到的高度自動算 innerPadding，這點比 iOS 单純。
 */
@Composable
fun <T> GlassTabBar(
    items: List<GlassTabBarItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(AppSurface.panel.copy(alpha = 0.92f))
                .border(1.dp, AppSurface.hairline, CircleShape)
                .padding(7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { item ->
                val isSelected = item.id == selected
                Column(
                    Modifier
                        .width(84.dp)
                        .height(66.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(item.id) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.title,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(25.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.title,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
