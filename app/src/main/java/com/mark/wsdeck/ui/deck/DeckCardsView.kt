package com.mark.wsdeck.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mark.wsdeck.data.*
import com.mark.wsdeck.ui.shared.PolicyGatedCardImage

/**
 * 牌組卡表：依等級分組，圖片格子／文字清單可切換（對應 iOS 的 cardGrid / cardList）。
 * 兩邊都在，不是只有清單模式才有——iOS 連清單模式都帶縮圖，
 * 「純文字清單太難掃視，補一張縮圖當視覺錨點」。
 */
@Composable
fun DeckCardsTab(
    deck: DeckWithEntries,
    items: List<CardCount>,
    usesGrid: Boolean,
    networkPolicy: NetworkPolicy,
    editable: Boolean = true,
    onReorderSection: (List<CardCount>) -> Unit = {},
    onAdjust: (printingId: String, delta: Int) -> Unit,
) {
    val sections = remember(items) { buildLevelSections(items) }
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
            Text("牌組是空的，到「圖鑑」分頁選擇此牌組後加卡",
                style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    if (usesGrid) {
        DeckCardGrid(deck, sections, editable, networkPolicy, onAdjust)
    } else {
        DeckCardList(deck, sections, items, editable, networkPolicy, onReorderSection, onAdjust)
    }
}

internal data class LevelSection(val title: String, val count: Int, val items: List<CardCount>)

internal fun buildLevelSections(items: List<CardCount>): List<LevelSection> {
    val result = mutableListOf<LevelSection>()
    for (level in 0..3) {
        val group = items.filter { it.card.level == level && it.card.cardType != CardType.CLIMAX }
        if (group.isNotEmpty()) result += LevelSection("Lv$level", group.sumOf { it.count }, group)
    }
    val climax = items.filter { it.card.cardType == CardType.CLIMAX }
    if (climax.isNotEmpty()) result += LevelSection("CX", climax.sumOf { it.count }, climax)
    return result
}

// ── 文字清單（含縮圖）───────────────────────────────────────────

@Composable
private fun DeckCardList(
    deck: DeckWithEntries,
    sections: List<LevelSection>,
    allItems: List<CardCount>,
    editable: Boolean,
    networkPolicy: NetworkPolicy,
    onReorderSection: (List<CardCount>) -> Unit = {},
    onAdjust: (String, Int) -> Unit,
) {
    val entryByPrinting = remember(deck.entries) { deck.entries.associate { it.printingId to it.count } }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        sections.forEach { section ->
            item(key = "list-header-${section.title}") { SectionHeader(section) }
            // 清單模式一律可拖曳排序（抓最左邊把手），不用先切排序模式；
            // 用一般 Column（非 lazy）才量得到每列實際位置，一副牌最多
            // 50 張，不用 lazy 回收也不會有效能問題
            item(key = "list-body-${section.title}") {
                ReorderableSection(
                    items = section.items,
                    entryByPrinting = entryByPrinting,
                    allItems = allItems,
                    editable = editable,
                    networkPolicy = networkPolicy,
                    onReordered = onReorderSection,
                    onAdjust = onAdjust,
                )
            }
        }
    }
}

/**
 * 排序跟看詳情／改張數共用同一列，不用切模式：抓最左邊的把手圖示直接拖
 * （不用長按），拖到想要的位置放開；點列的其他地方照常展開看各刷版張數。
 * 只在把手圖示這個小範圍偵測拖曳手勢——不是整列都能拖——這樣才不會跟
 * LazyColumn 本身的上下滑動捲動搶手勢，也不會跟展開的點擊搶手勢；量每列
 * 實際位置（onGloballyPositioned）決定該跟哪一列交換，不依賴 Compose 目前
 * 還沒有的原生 reorder API。
 */
@Composable
private fun ReorderableSection(
    items: List<CardCount>,
    entryByPrinting: Map<String, Int>,
    allItems: List<CardCount>,
    editable: Boolean,
    networkPolicy: NetworkPolicy,
    onReordered: (List<CardCount>) -> Unit,
    onAdjust: (String, Int) -> Unit,
) {
    var order by remember(items) { mutableStateOf(items) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val positions = remember { mutableStateMapOf<String, Pair<Float, Float>>() }
    val expandedIds = remember { mutableStateMapOf<String, Boolean>() }

    Column {
        order.forEach { item ->
            val cardId = item.card.id
            val isDragging = draggingId == cardId
            // key() 讓 Compose 用卡片 id（而不是位置）認節點——順序一變，被拖的那列
            // 若沒有 key 會在新位置被當成「不同節點」重建，手勢偵測的 coroutine
            // 跟著被砍掉，變成收到 onDragCancel 而不是 onDragEnd，onReordered
            // 永遠不會被呼叫，拖曳看起來有動但放開後全部還原（之前抓到的 bug）
            key(cardId) {
            val overLimit = DeckValidator.nameCount(item.card, allItems) > DeckValidator.NAME_LIMIT
            val displayPrinting = item.card.printings.firstOrNull { (entryByPrinting[it.id] ?: 0) > 0 }
                ?: item.card.defaultPrinting
            val expanded = expandedIds[cardId] ?: false
            Column(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        positions[cardId] = coords.positionInParent().y to coords.size.height.toFloat()
                    }
                    .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f }
                    .zIndex(if (isDragging) 1f else 0f)
                    .background(
                        if (isDragging) MaterialTheme.colorScheme.surfaceVariant
                        else Color.Transparent,
                    )
                    .clickable { expandedIds[cardId] = !expanded },
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = "拖曳排序",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            // 手把圖示本身再多留一點觸控範圍，手指不用點得極準
                            .padding(8.dp)
                            .pointerInput(cardId) {
                                detectDragGestures(
                                    onDragStart = { draggingId = cardId; dragOffsetY = 0f },
                                    onDragEnd = {
                                        draggingId = null
                                        dragOffsetY = 0f
                                        onReordered(order)
                                    },
                                    onDragCancel = { draggingId = null; dragOffsetY = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        val (top, _) = positions[cardId] ?: return@detectDragGestures
                                        val height = positions[cardId]?.second ?: 0f
                                        val draggedCenter = top + dragOffsetY + height / 2f
                                        val currentIndex = order.indexOfFirst { it.card.id == cardId }
                                        val targetIndex = order.indexOfFirst { other ->
                                            val (otherTop, otherHeight) = positions[other.card.id]
                                                ?: return@indexOfFirst false
                                            draggedCenter in otherTop..(otherTop + otherHeight)
                                        }
                                        if (targetIndex != -1 && targetIndex != currentIndex) {
                                            // 交換後這一列會落在「原本被換掉那列」的位置——不能用
                                            // 交換後的自己重讀位置來算補償，Compose 這時候還沒
                                            // 重新排版，讀到的還是交換前的舊值，補償永遠算成 0，
                                            // 差一點點就會雪崩式一路連環交換到底（這次抓到的 bug）
                                            val targetTop = positions[order[targetIndex].card.id]?.first ?: top
                                            val moved = order.toMutableList()
                                            val el = moved.removeAt(currentIndex)
                                            moved.add(targetIndex, el)
                                            order = moved
                                            dragOffsetY += top - targetTop
                                        }
                                    },
                                )
                            },
                    )
                    // 純文字清單太難掃視，補一張縮圖當視覺錨點（與 iOS 同樣的理由）
                    val isClimax = item.card.cardType == CardType.CLIMAX
                    PolicyGatedCardImage(
                        url = displayPrinting.imageURL,
                        contentDescription = item.card.nameZH,
                        networkPolicy = networkPolicy,
                        modifier = Modifier
                            .width(if (isClimax) 52.dp else 36.dp)
                            .aspectRatio(if (isClimax) 88f / 63f else 63f / 88f)
                            .clip(RoundedCornerShape(3.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.card.nameZH,
                            color = if (overLimit) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                        )
                        val rarities = item.card.printings
                            .mapNotNull { p -> (entryByPrinting[p.id] ?: 0).takeIf { it > 0 }?.let { "${p.rarity}×$it" } }
                            .joinToString(" ")
                        Text(
                            rarities.ifEmpty { item.card.id },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "×${item.count}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (overLimit) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                if (expanded) {
                    Column(Modifier.padding(start = 62.dp, end = 16.dp, bottom = 6.dp)) {
                        item.card.printings.forEach { printing ->
                            val count = entryByPrinting[printing.id] ?: 0
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(printing.rarity, style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.width(48.dp))
                                Text(
                                    printing.id,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                if (editable) {
                                    CountStepper(count) { delta -> onAdjust(printing.id, delta) }
                                } else {
                                    Text("×$count", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            }
        }
    }
}

// ── 圖片格子（依刷版分格，一張卡放了兩種稀有度就顯示兩格）─────────

@Composable
private fun DeckCardGrid(
    deck: DeckWithEntries,
    sections: List<LevelSection>,
    editable: Boolean,
    networkPolicy: NetworkPolicy,
    onAdjust: (String, Int) -> Unit,
) {
    val entryByPrinting = remember(deck.entries) { deck.entries.associate { it.printingId to it.count } }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        sections.forEach { section ->
            item(key = "grid-header-${section.title}") { SectionHeader(section) }
            item(key = "grid-body-${section.title}") {
                // 把每張卡展開成「牌組中有放的刷版」各一格
                val tiles = section.items.flatMap { item ->
                    item.card.printings.filter { (entryByPrinting[it.id] ?: 0) > 0 }
                        .map { printing -> item.card to printing }
                }
                if (tiles.isEmpty()) return@item
                // 巢狀 LazyVerticalGrid 用固定高度換算，格數依畫面寬度粗抓 3~4 欄
                FlowGrid(tiles) { card, printing ->
                    DeckGridTile(card, printing, entryByPrinting[printing.id] ?: 0, editable, networkPolicy, onAdjust)
                }
            }
        }
    }
}

/** 固定 3 欄的簡易網格——巢狀在 LazyColumn 裡，不能再用 LazyVerticalGrid（測量衝突） */
@Composable
private fun FlowGrid(
    tiles: List<Pair<Card, Printing>>,
    content: @Composable (Card, Printing) -> Unit,
) {
    val columns = 3
    Column(Modifier.padding(horizontal = 12.dp)) {
        tiles.chunked(columns).forEach { rowTiles ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowTiles.forEach { (card, printing) ->
                    Box(Modifier.weight(1f)) { content(card, printing) }
                }
                repeat(columns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DeckGridTile(
    card: Card,
    printing: Printing,
    count: Int,
    editable: Boolean,
    networkPolicy: NetworkPolicy,
    onAdjust: (String, Int) -> Unit,
) {
    Column {
        Box {
            PolicyGatedCardImage(
                url = printing.imageURL,
                contentDescription = card.nameZH,
                networkPolicy = networkPolicy,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(63f / 88f)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text("$count", color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(
            "${printing.rarity} ${card.nameZH}",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (editable) {
            CountStepper(count) { delta -> onAdjust(printing.id, delta) }
        }
    }
}

@Composable
private fun SectionHeader(section: LevelSection) {
    Text(
        "${section.title} (${section.count})",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}
