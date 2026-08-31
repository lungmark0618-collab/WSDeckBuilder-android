package com.mark.wsdeck.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mark.wsdeck.data.*
import com.mark.wsdeck.ui.theme.AppSurface

/**
 * 篩選面板，對應 iOS 的 FilterSheet（§4.4.1：條件間 AND、同條件內 OR）。
 *
 * 滿版半透明疊層取代原本的 ModalBottomSheet——原本用橫向捲動 chip 列，
 * 選項一多（尤其特徵）大半都藏在畫面外側滑才看得到，改成 FlowRow 自動換行、
 * 疊層佔滿整個螢幕，一次能攤開的空間也更大。跟 iOS 同一套設計決定。
 *
 * 「收錄來源」沒有搬過來——那個依賴 iOS 端的 CardSource，卡表資料裡目前
 * 沒有對應欄位。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    query: SearchQuery,
    repo: CardRepository,
    onQueryChange: (SearchQuery) -> Unit,
    onDismiss: () -> Unit,
) {
    var showMoreFilters by remember { mutableStateOf(false) }

    // 只有鎖定某部作品（不論是畫面本來就鎖定，還是使用者在「作品」區塊選了一個）
    // 才會列出來，且只列該作品出現過的特徵——全部特徵動輒上百個，沒有作品當
    // 範圍乾脆整區不顯示，見下面 `if (query.titleCode != null)`
    val availableTraits = remember(query.titleCode) {
        query.titleCode?.let { repo.traits(inScope = it) } ?: emptyList()
    }
    val traitsCardTitle = remember(query.titleCode) {
        val name = query.titleCode?.let { code -> repo.snapshot.browsableSets.firstOrNull { it.id == code }?.displayNameZH }
        if (name != null) "特徵（$name）" else "特徵"
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(AppSurface.background.copy(alpha = 0.92f)),
        ) {
            Column(Modifier.fillMaxSize()) {
                TopBar(query, onQueryChange, onDismiss)
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FilterCard("作品") {
                        var titleSearch by remember { mutableStateOf("") }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = titleSearch,
                                onValueChange = { titleSearch = it },
                                placeholder = { Text("搜尋作品名稱") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (titleSearch.isNotEmpty()) {
                                        IconButton(onClick = { titleSearch = "" }) {
                                            Icon(Icons.Filled.Close, contentDescription = "清除")
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            // 只搜作品名稱，跟上層圖鑑那個搜卡號/卡名/能力文字的搜尋列是兩回事
                            val filteredSets = remember(titleSearch, repo.snapshot.browsableSets) {
                                if (titleSearch.isBlank()) repo.snapshot.browsableSets
                                else repo.snapshot.browsableSets.filter {
                                    it.titleNameZH.contains(titleSearch, ignoreCase = true) ||
                                        it.titleNameJP.contains(titleSearch, ignoreCase = true)
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (titleSearch.isBlank()) {
                                    Chip("全部", query.titleCode == null) {
                                        onQueryChange(query.copy(titleCode = null))
                                    }
                                }
                                filteredSets.forEach { set ->
                                    Chip(set.displayNameZH, query.titleCode == set.id) {
                                        onQueryChange(query.copy(titleCode = set.id))
                                    }
                                }
                            }
                        }
                    }

                    FilterCard("等級") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 1, 2, 3).forEach { lv ->
                                val on = lv in query.levels
                                Chip("Lv$lv", on) {
                                    onQueryChange(query.copy(
                                        levels = if (on) query.levels - lv else query.levels + lv))
                                }
                            }
                        }
                    }

                    FilterCard("顏色") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CardColor.entries.forEach { color ->
                                val on = color in query.colors
                                Chip(color.label, on) {
                                    onQueryChange(query.copy(
                                        colors = if (on) query.colors - color else query.colors + color))
                                }
                            }
                        }
                    }

                    FilterCard("種類") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CardType.entries.forEach { type ->
                                val on = type in query.types
                                Chip(type.label, on) {
                                    onQueryChange(query.copy(
                                        types = if (on) query.types - type else query.types + type))
                                }
                            }
                        }
                    }

                    // 沒鎖定作品時特徵動輒上百個，乾脆整區不顯示——選了作品才彈出來
                    if (query.titleCode != null) {
                        FilterCard(traitsCardTitle) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                availableTraits.forEach { trait ->
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

                    FilterCard("我的收藏") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OwnershipFilter.entries.forEach { filter ->
                                    Chip(filter.label, query.ownership == filter) {
                                        onQueryChange(query.copy(ownership = filter))
                                    }
                                }
                            }
                            Text(
                                "依「我的收藏」記錄的擁有張數篩選，可用來找還沒收到的卡。",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppSurface.secondaryText,
                            )
                        }
                    }

                    MoreFiltersDisclosure(showMoreFilters, { showMoreFilters = !showMoreFilters }) {
                        FilterCard("判定標誌") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    }

                    Spacer(Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
private fun TopBar(query: SearchQuery, onQueryChange: (SearchQuery) -> Unit, onDismiss: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AppSurface.panel)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { onQueryChange(query.copy(
                levels = emptySet(), colors = emptySet(), types = emptySet(),
                triggers = emptySet(), traits = emptySet(), titleCode = null,
                ownership = OwnershipFilter.ALL,
            )) },
            enabled = query.hasActiveFilters,
        ) { Text("全部清除") }
        Text("篩選", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onDismiss) { Text("完成", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun MoreFiltersDisclosure(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        color = AppSurface.panel,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("更多篩選（判定標誌）", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
    }
    if (expanded) {
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun FilterCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurface.panel)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold, color = AppSurface.secondaryText)
        content()
    }
}

@Composable
private fun Chip(label: String, isOn: Boolean, onClick: () -> Unit) {
    val color = if (isOn) MaterialTheme.colorScheme.primary else AppSurface.panelElevated
    val fg = if (isOn) MaterialTheme.colorScheme.onPrimary else Color.White
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
