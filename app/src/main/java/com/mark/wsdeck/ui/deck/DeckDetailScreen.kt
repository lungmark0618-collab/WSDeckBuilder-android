package com.mark.wsdeck.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.*
import kotlinx.coroutines.launch

/**
 * 單一牌組編輯，依等級分組；卡表／統計切換（對應 iOS 的 DeckDetailView，簡化版：
 * 不含收藏比對缺卡、封面選擇、匯出——那些留到後面的階段）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    uuid: String,
    cardRepo: CardRepository,
    deckRepo: DeckRepository,
    onBack: () -> Unit,
) {
    val deckState by deckRepo.observeDeck(uuid).collectAsStateWithLifecycle(initialValue = null)
    val deck = deckState ?: return
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(Mode.CARDS) }
    var showRename by remember { mutableStateOf(false) }

    val items = remember(deck.entries) { groupByCard(deck.entries, cardRepo) }
    val validation = remember(items) { DeckValidator.validate(items) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deck.deck.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showRename = true }) { Text("重新命名") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ValidationHeader(validation)
            TabRow(selectedTabIndex = mode.ordinal) {
                Mode.entries.forEach { m ->
                    Tab(
                        selected = mode == m,
                        onClick = { mode = m },
                        text = { Text(m.label) },
                    )
                }
            }
            when (mode) {
                Mode.CARDS -> CardsTab(deck, items) { printingId, delta ->
                    scope.launch { deckRepo.adjust(uuid, printingId, delta) }
                }
                Mode.STATS -> DeckStatsView(items)
            }
        }
    }

    if (showRename) {
        NameDialog(
            title = "重新命名",
            initial = deck.deck.name,
            onConfirm = { name ->
                scope.launch { deckRepo.renameDeck(deck.deck, name.ifBlank { deck.deck.name }) }
                showRename = false
            },
            onDismiss = { showRename = false },
        )
    }
}

private enum class Mode(val label: String) { CARDS("卡表"), STATS("統計") }

@Composable
private fun ValidationHeader(v: DeckValidator.Result) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RuleChip("${v.totalCount}/50", v.totalOK)
        RuleChip("CX ${v.climaxCount}/8", v.climaxOK)
        if (!v.namesOK) RuleChip("同名超過4張", false)
        if (v.mixedTitles) RuleChip("跨作品混搭", false)
        Spacer(Modifier.weight(1f))
        if (v.isLegal) {
            Text("符合規則", color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RuleChip(text: String, ok: Boolean) {
    val color = if (ok) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    Box(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CardsTab(
    deck: DeckWithEntries,
    items: List<CardCount>,
    onAdjust: (printingId: String, delta: Int) -> Unit,
) {
    val sections = remember(items) { buildSections(items) }
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
            Text("牌組是空的，到「圖鑑」分頁選擇此牌組後加卡",
                style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        sections.forEach { section ->
            item(key = "header-${section.title}") {
                Text(
                    "${section.title} (${section.count})",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            items(section.items, key = { it.card.id }) { item ->
                DeckEntryRow(
                    deck = deck,
                    item = item,
                    totalForName = DeckValidator.nameCount(item.card, items),
                    onAdjust = onAdjust,
                )
            }
        }
    }
}

private data class LevelSection(val title: String, val count: Int, val items: List<CardCount>)

private fun buildSections(items: List<CardCount>): List<LevelSection> {
    val result = mutableListOf<LevelSection>()
    for (level in 0..3) {
        val group = items.filter { it.card.level == level && it.card.cardType != CardType.CLIMAX }
        if (group.isNotEmpty()) {
            result += LevelSection("Lv$level", group.sumOf { it.count }, group)
        }
    }
    val climax = items.filter { it.card.cardType == CardType.CLIMAX }
    if (climax.isNotEmpty()) result += LevelSection("CX", climax.sumOf { it.count }, climax)
    return result
}

@Composable
private fun DeckEntryRow(
    deck: DeckWithEntries,
    item: CardCount,
    totalForName: Int,
    onAdjust: (printingId: String, delta: Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val overLimit = totalForName > DeckValidator.NAME_LIMIT
    val entryByPrinting = remember(deck.entries) {
        deck.entries.associate { it.printingId to it.count }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.card.nameZH,
                    color = if (overLimit) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
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
            Column(Modifier.padding(top = 6.dp)) {
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
                        CountStepper(count) { delta -> onAdjust(printing.id, delta) }
                    }
                }
            }
        }
    }
    HorizontalDivider()
}

@Composable
fun CountStepper(count: Int, onDelta: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onDelta(-1) }, enabled = count > 0,
            modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "減少", modifier = Modifier.size(18.dp))
        }
        Text(
            "$count",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { onDelta(1) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "增加", modifier = Modifier.size(18.dp))
        }
    }
}
