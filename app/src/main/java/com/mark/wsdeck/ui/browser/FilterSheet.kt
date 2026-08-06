package com.mark.wsdeck.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mark.wsdeck.data.*

/**
 * 篩選面板。對應 iOS 的 FilterSheet（§4.4.1：條件間 AND、同條件內 OR）。
 *
 * 「收藏狀態」「收錄來源」兩個區塊沒有搬過來——那兩個依賴 iOS 端的
 * CollectionEntry／CardSource，屬於牌組管理那塊，還沒做。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    query: SearchQuery,
    sets: List<CardSetMeta>,
    allTraits: List<String>,
    onQueryChange: (SearchQuery) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("篩選", style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = { onQueryChange(query.copy(
                        levels = emptySet(), colors = emptySet(), types = emptySet(),
                        triggers = emptySet(), traits = emptySet(), titleCode = null,
                    )) },
                    enabled = query.hasActiveFilters,
                ) { Text("全部清除") }
            }

            FilterSection("作品") {
                ChipRow {
                    Chip("全部", query.titleCode == null) {
                        onQueryChange(query.copy(titleCode = null))
                    }
                    sets.forEach { set ->
                        Chip(set.titleNameZH, query.titleCode == set.titleCode) {
                            onQueryChange(query.copy(titleCode = set.titleCode))
                        }
                    }
                }
            }

            FilterSection("等級") {
                ChipRow {
                    listOf(0, 1, 2, 3).forEach { lv ->
                        val on = lv in query.levels
                        Chip("Lv$lv", on) {
                            onQueryChange(query.copy(
                                levels = if (on) query.levels - lv else query.levels + lv))
                        }
                    }
                }
            }

            FilterSection("顏色") {
                ChipRow {
                    CardColor.entries.forEach { color ->
                        val on = color in query.colors
                        Chip(color.label, on) {
                            onQueryChange(query.copy(
                                colors = if (on) query.colors - color else query.colors + color))
                        }
                    }
                }
            }

            FilterSection("種類") {
                ChipRow {
                    CardType.entries.forEach { type ->
                        val on = type in query.types
                        Chip(type.label, on) {
                            onQueryChange(query.copy(
                                types = if (on) query.types - type else query.types + type))
                        }
                    }
                }
            }

            FilterSection("判定標誌") {
                ChipRow {
                    TriggerIcon.entries.forEach { trigger ->
                        val on = trigger in query.triggers
                        Chip(trigger.label, on) {
                            onQueryChange(query.copy(
                                triggers = if (on) query.triggers - trigger
                                          else query.triggers + trigger))
                        }
                    }
                }
            }

            if (allTraits.isNotEmpty()) {
                FilterSection("特徵") {
                    ChipRow {
                        allTraits.forEach { trait ->
                            val on = trait in query.traits
                            Chip("《$trait》", on) {
                                onQueryChange(query.copy(
                                    traits = if (on) query.traits - trait
                                            else query.traits + trait))
                            }
                        }
                    }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("完成") }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold)
        content()
    }
}

/** 橫向捲動的 chip 列，特徵動輒上百個，換行會佔掉整個畫面 */
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Chip(label: String, isOn: Boolean, onClick: () -> Unit) {
    val color = if (isOn) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isOn) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.bodyMedium)
    }
}
