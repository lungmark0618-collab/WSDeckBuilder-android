package com.mark.wsdeck.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mark.wsdeck.data.*
import com.mark.wsdeck.ui.deck.DeckCardsTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 圖鑑：作品選單 → 該作品的卡片。與 iOS 的 CardCatalogView 同樣的兩層結構。
 *
 * 搜尋列固定在最上面——一開始打字就跳過作品選單直接顯示跨作品結果，
 * 想直接找卡的人不必先選作品。
 */
@Composable
fun CatalogScreen(repo: CardRepository, deckRepo: DeckRepository) {
    var query by remember { mutableStateOf(SearchQuery()) }
    var results by remember { mutableStateOf<List<Card>>(emptyList()) }
    var detail by remember { mutableStateOf<Card?>(null) }
    var showFilter by remember { mutableStateOf(false) }
    var showDeckQuickView by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    val decks by deckRepo.observeDecks().collectAsStateWithLifecycle(initialValue = emptyList())
    var activeDeckUuid by remember { mutableStateOf(prefs.activeDeckUuid) }
    val activeDeck = decks.firstOrNull { it.deck.uuid == activeDeckUuid }

    val showsGallery = query.keyword.isEmpty() && !query.hasActiveFilters

    // 打字時每個字都重搜會頓；停一下再搜，中途的輸入直接作廢
    LaunchedEffect(query) {
        if (showsGallery) {
            results = emptyList()
            return@LaunchedEffect
        }
        if (query.keyword.isNotEmpty()) delay(180)
        results = repo.search(query)
    }

    Column(Modifier.fillMaxSize()) {
        ActiveDeckPickerRow(
            decks = decks,
            activeDeck = activeDeck,
            onSelect = { uuid ->
                activeDeckUuid = uuid
                prefs.activeDeckUuid = uuid
            },
            onShowContents = { showDeckQuickView = true },
        )
        SearchBarRow(
            keyword = query.keyword,
            pinnedTitle = query.titleCode?.let { code ->
                repo.snapshot.sets.firstOrNull { it.titleCode == code }?.titleNameZH
            },
            hasActiveFilters = query.hasActiveFilters,
            onKeyword = { query = query.copy(keyword = it) },
            onClearTitle = { query = SearchQuery() },
            onOpenFilter = { showFilter = true },
        )
        ActiveFilterBar(query, repo.snapshot.sets) { query = SearchQuery(titleCode = query.titleCode) }

        if (showsGallery) {
            TitleGallery(repo.snapshot.sets, repo.snapshot.cards.size) {
                query = SearchQuery(titleCode = it)
            }
        } else if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("沒有符合的卡片") }
        } else {
            CardGrid(results, activeDeck, deckRepo) { card ->
                detail = card
            }
        }
    }

    detail?.let { card ->
        CardDetailSheet(card, repo.titleCode(card)) { detail = null }
    }

    // 加卡加到一半想確認「現在到底放了哪些」，不必離開圖鑑切去牌組分頁。
    // ⚠ 這不是 iOS 有的功能——iOS 圖鑑只靠格子上的張數徽章跟這裡的 N/50，
    // 沒有現成的清單可看，這是額外補的。
    if (showDeckQuickView && activeDeck != null) {
        ActiveDeckQuickView(activeDeck, repo, deckRepo) { showDeckQuickView = false }
    }

    if (showFilter) {
        FilterSheet(
            query = query,
            sets = repo.snapshot.sets,
            allTraits = repo.snapshot.allTraits,
            onQueryChange = { query = it },
            onDismiss = { showFilter = false },
        )
    }
}

/**
 * 選擇「目前編輯中的牌組」，卡片格子上的＋/－直接作用於它（對應 iOS 的
 * ActiveDeckPicker）。沒有牌組時顯示提示，引導去牌組分頁建立。
 */
@Composable
private fun ActiveDeckPickerRow(
    decks: List<DeckWithEntries>,
    activeDeck: DeckWithEntries?,
    onSelect: (String) -> Unit,
    onShowContents: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Style, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        if (decks.isEmpty()) {
            Text(
                "到「牌組」分頁建立牌組後，可在此直接加卡",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Box {
                Row(
                    Modifier.clickable { expanded = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        activeDeck?.let { "編輯中：${it.deck.name}" } ?: "選擇要編輯的牌組",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Icon(Icons.Filled.ExpandMore, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("（不選擇牌組）") }, onClick = {
                        onSelect(""); expanded = false
                    })
                    decks.forEach { d ->
                        DropdownMenuItem(text = { Text(d.deck.name) }, onClick = {
                            onSelect(d.deck.uuid); expanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            activeDeck?.let {
                // 點張數可以直接看目前放了哪些卡，不必離開圖鑑
                Row(
                    Modifier.clickable(onClick = onShowContents),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = "查看目前牌組內容",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${it.totalCount}/50",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (it.totalCount == 50) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * 加卡加到一半的快速檢視：不離開圖鑑就能看到目前牌組放了哪些卡、直接調整。
 * 沿用牌組詳情頁同一套卡表元件，圖片／清單切換也共用同一個設定。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveDeckQuickView(
    deck: DeckWithEntries,
    cardRepo: CardRepository,
    deckRepo: DeckRepository,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var usesGrid by remember { mutableStateOf(prefs.deckUsesGrid) }
    val scope = rememberCoroutineScope()
    val items = remember(deck.entries) { groupByCard(deck.entries, cardRepo) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(deck.deck.name, style = MaterialTheme.typography.titleMedium)
                Text("${deck.totalCount}/50", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                usesGrid = !usesGrid
                prefs.deckUsesGrid = usesGrid
            }) {
                Icon(if (usesGrid) Icons.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (usesGrid) "改為清單顯示" else "改為圖片顯示")
            }
        }
        Box(Modifier.heightIn(max = 480.dp)) {
            DeckCardsTab(deck, items, usesGrid, editable = true) { printingId, delta ->
                scope.launch { deckRepo.adjust(deck.deck.uuid, printingId, delta) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SearchBarRow(
    keyword: String,
    pinnedTitle: String?,
    hasActiveFilters: Boolean,
    onKeyword: (String) -> Unit,
    onClearTitle: () -> Unit,
    onOpenFilter: () -> Unit,
) {
    Column {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeyword,
                // 鎖定作品時搜尋範圍只在該作品內，提示字要講清楚，
                // 不然使用者會以為是跨作品搜尋卻找不到東西
                placeholder = {
                    Text(if (pinnedTitle != null) "在這部作品裡搜尋" else "卡號、卡名、能力文字")
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onOpenFilter,
                colors = if (hasActiveFilters) {
                    IconButtonDefaults.filledIconButtonColors()
                } else {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            ) { Icon(Icons.Filled.FilterList, contentDescription = "篩選") }
        }
        if (pinnedTitle != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(pinnedTitle, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClearTitle) { Text("回作品選單") }
            }
        }
    }
}

/**
 * 讓人知道結果為何被縮小，並能一鍵解除。標題不重複顯示——
 * 已經有 pinnedTitle 那一列了，這裡只列標題以外的條件。
 */
@Composable
private fun ActiveFilterBar(query: SearchQuery, sets: List<CardSetMeta>, onClear: () -> Unit) {
    val parts = buildList {
        if (query.levels.isNotEmpty()) {
            add("Lv" + query.levels.sorted().joinToString("/"))
        }
        if (query.colors.isNotEmpty()) add(query.colors.joinToString("/") { it.label })
        if (query.types.isNotEmpty()) add(query.types.joinToString("/") { it.label })
        if (query.triggers.isNotEmpty()) add("判定×${query.triggers.size}")
        if (query.traits.isNotEmpty()) add(query.traits.sorted().joinToString("/"))
    }
    if (parts.isEmpty()) return

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onClear) { Text("清除") }
    }
}

@Composable
private fun TitleGallery(
    sets: List<CardSetMeta>,
    totalCount: Int,
    onSelect: (String) -> Unit,
) {
    // 卡多的作品排前面——照代號排等於隨機順序
    val ordered = remember(sets) { sets.sortedByDescending { it.cardCount } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(158.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(ordered, key = { it.titleCode }) { set ->
            TitleTile(set) { onSelect(set.titleCode) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "共 $totalCount 張卡",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun TitleTile(set: CardSetMeta, onClick: () -> Unit) {
    val color = TitlePalette.accent(set.titleCode)
    Column(
        Modifier
            .height(104.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f))))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            set.titleNameZH,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            set.titleNameJP,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth()) {
            Text(
                set.titleCode,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${set.cardCount}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun CardGrid(
    cards: List<Card>,
    activeDeck: DeckWithEntries?,
    deckRepo: DeckRepository,
    onTap: (Card) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(100.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cards, key = { it.id }) { card ->
            CardTile(card, activeDeck, deckRepo, onTap)
        }
    }
}

@Composable
private fun CardTile(
    card: Card,
    activeDeck: DeckWithEntries?,
    deckRepo: DeckRepository,
    onTap: (Card) -> Unit,
) {
    val scope = rememberCoroutineScope()

    // 牌組裡跨刷版的總張數——加了 SR 版之後，普卡格子上的徽章也該反映總數，
    // 不然使用者以為卡片還沒放進牌組
    val countInDeck = remember(activeDeck, card) {
        activeDeck?.entries
            ?.filter { entry -> card.printings.any { it.id == entry.printingId } }
            ?.sumOf { it.count } ?: 0
    }

    Column(Modifier.clickable { onTap(card) }) {
        Box {
            AsyncImage(
                model = card.defaultPrinting.imageURL,
                contentDescription = card.nameZH,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(63f / 88f)
                    .clip(RoundedCornerShape(4.dp)),
            )
            if (countInDeck > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("×$countInDeck", color = Color.White,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text(
            card.nameZH,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // 有選定牌組時才顯示＋/－，免得沒牌組可加時空占版面
        if (activeDeck != null) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            deckRepo.adjust(activeDeck.deck.uuid, card.defaultPrinting.id, -1)
                        }
                    },
                    enabled = countInDeck > 0,
                    modifier = Modifier.size(28.dp),
                ) { Icon(Icons.Filled.Remove, contentDescription = "減少", modifier = Modifier.size(16.dp)) }
                IconButton(
                    onClick = {
                        scope.launch {
                            deckRepo.adjust(activeDeck.deck.uuid, card.defaultPrinting.id, 1)
                        }
                    },
                    modifier = Modifier.size(28.dp),
                ) { Icon(Icons.Filled.Add, contentDescription = "增加", modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardDetailSheet(card: Card, titleCode: String?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(card.nameZH, style = MaterialTheme.typography.titleMedium)
            Text(card.nameJP, style = MaterialTheme.typography.bodySmall)
            Text(
                buildString {
                    append(card.id)
                    card.level?.let { append("  Lv$it") }
                    card.cost?.let { append("／費$it") }
                    card.power?.let { append("  攻擊力$it") }
                    card.soul?.let { append("  魂傷$it") }
                    card.trigger?.let { append("  判定${it.label}") }
                },
                style = MaterialTheme.typography.labelMedium,
            )
            // 《》特徵保留日文，跟卡面一致
            if (card.traitsJP.isNotEmpty()) {
                Text(card.traitsJP.joinToString(" ") { "《$it》" },
                    style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
            card.textLinesZH.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
