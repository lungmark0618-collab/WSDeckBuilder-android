package com.mark.wsdeck.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.*
import com.mark.wsdeck.ui.deck.CountStepper
import com.mark.wsdeck.ui.deck.DeckCardsTab
import com.mark.wsdeck.ui.notifications.NotificationBellButton
import com.mark.wsdeck.ui.onboarding.onboardingAnchor
import com.mark.wsdeck.ui.shared.PolicyGatedCardImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 圖鑑：作品選單 → 該作品的卡片。與 iOS 的 CardCatalogView 同樣的兩層結構。
 *
 * 搜尋列固定在最上面——一開始打字就跳過作品選單直接顯示跨作品結果，
 * 想直接找卡的人不必先選作品。
 */
@Composable
fun CatalogScreen(
    repo: CardRepository,
    deckRepo: DeckRepository,
    collectionRepo: CollectionRepository,
    announcements: AnnouncementCenter,
    appearance: AppearanceSettings,
    networkPolicy: NetworkPolicy,
    onboarding: OnboardingState,
    favorites: FavoriteTitlesStore,
) {
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

    val collection by collectionRepo.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val collectionIndex = remember(collection) { CollectionStore.index(collection) }

    val showsGallery = query.keyword.isEmpty() && !query.hasActiveFilters

    // 供 AppearanceSettings 的「強調色跟著作品」模式使用，對應 iOS 的
    // currentTitleCode——拆過彈的話 query.titleCode 是商品代碼（如
    // "SFN/S108"），TitlePalette 認的是原本的 titleCode，要轉一手
    LaunchedEffect(query.titleCode) {
        val scope = query.titleCode
        val resolved = scope?.let { s -> repo.snapshot.browsableSets.firstOrNull { it.id == s }?.titleCode }
            ?: scope ?: ""
        appearance.setCurrentTitleCode(resolved)
    }

    // 打字時每個字都重搜會頓；停一下再搜，中途的輸入直接作廢。收藏狀態不是
    // CardRepository.search() 認得的條件，跟 iOS 一樣在搜完之後另外過濾一次。
    LaunchedEffect(query, collectionIndex) {
        if (showsGallery) {
            results = emptyList()
            return@LaunchedEffect
        }
        if (query.keyword.isNotEmpty()) {
            onboarding.notify(OnboardingStep.SEARCH)
            delay(180)
        }
        val found = repo.search(query)
        results = when (query.ownership) {
            OwnershipFilter.ALL -> found
            OwnershipFilter.OWNED -> found.filter { CollectionStore.owned(it, collectionIndex) > 0 }
            OwnershipFilter.MISSING -> found.filter { CollectionStore.owned(it, collectionIndex) == 0 }
        }
    }

    // 選了作品/商品之後，整個畫面往右滑就退回作品選單——這裡沒有真正的
    // NavController 堆疊可以回退（切作品是靠 query 狀態切換，不是 push
    // 新畫面），用手勢直接把 query 重置回去模擬「回上一頁」的效果，
    // 對應 iOS CardBrowserView 那邊掛在 navigationDestination 上的
    // swipeToGoBack()。只有明顯偏水平的右滑才觸發，不要跟垂直捲動搶手勢。
    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(query.titleCode) {
                if (query.titleCode == null) return@pointerInput
                var totalDrag = 0f
                var totalVertical = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f; totalVertical = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        totalVertical += change.positionChange().y
                        change.consume()
                    },
                    onDragEnd = {
                        if (totalDrag > 80f && kotlin.math.abs(totalVertical) < 60f) {
                            query = SearchQuery()
                        }
                    },
                )
            },
    ) {
        ActiveDeckPickerRow(
            decks = decks,
            activeDeck = activeDeck,
            onSelect = { uuid ->
                activeDeckUuid = uuid
                prefs.activeDeckUuid = uuid
            },
        )
        SearchBarRow(
            keyword = query.keyword,
            pinnedTitle = query.titleCode?.let { code ->
                repo.snapshot.browsableSets.firstOrNull { it.id == code }?.displayNameZH
            },
            hasActiveFilters = query.hasActiveFilters,
            onKeyword = { query = query.copy(keyword = it) },
            onClearTitle = { query = SearchQuery() },
            onOpenFilter = {
                showFilter = true
                onboarding.notify(OnboardingStep.FILTER)
            },
            announcements = announcements,
            onboarding = onboarding,
        )
        ActiveFilterBar(query, repo.snapshot.sets) { query = SearchQuery(titleCode = query.titleCode) }

        // 加卡加到一半想確認「現在到底放了哪些」，不必離開圖鑑切去牌組分頁。
        // ⚠ 這不是 iOS 有的功能——iOS 圖鑑只靠格子上的張數徽章，沒有現成的
        // 縮圖列可看，這是額外補的：直接貼在卡片結果上方，點一下拉出完整清單。
        activeDeck?.let { deck ->
            ActiveDeckStrip(deck, repo, networkPolicy) { showDeckQuickView = true }
        }

        if (showsGallery) {
            TitleGallery(repo.snapshot.browsableSets, repo.snapshot.cards.size, favorites) {
                query = SearchQuery(titleCode = it)
            }
        } else if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("沒有符合的卡片") }
        } else {
            CardGrid(results, activeDeck, deckRepo, collectionIndex, networkPolicy, onboarding) { card ->
                detail = card
            }
        }
    }

    // 引導教學：點開卡片詳情，等於完成了「查看卡片」這一步
    LaunchedEffect(detail) {
        if (detail != null) onboarding.notify(OnboardingStep.VIEW_CARD)
    }

    detail?.let { card ->
        CardDetailSheet(
            card, repo, collectionIndex, collectionRepo, networkPolicy, appearance,
            activeDeck, deckRepo,
            onSelectRelated = { detail = it },
            onDismiss = { detail = null },
        )
    }

    if (showDeckQuickView && activeDeck != null) {
        ActiveDeckQuickView(activeDeck, repo, deckRepo, networkPolicy) { showDeckQuickView = false }
    }

    if (showFilter) {
        FilterSheet(
            query = query,
            repo = repo,
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
        }
    }
}

/**
 * 目前牌組內容的縮圖列，貼在卡片結果正上方——不用再跑去角落找一行小字，
 * 加卡的同時餘光就能看到已經放了哪些。點整列拉出 ActiveDeckQuickView
 * 看完整清單、調整張數。
 */
@Composable
private fun ActiveDeckStrip(
    deck: DeckWithEntries,
    cardRepo: CardRepository,
    networkPolicy: NetworkPolicy,
    onClick: () -> Unit,
) {
    val entryByPrinting = remember(deck.entries) { deck.entries.associate { it.printingId to it.count } }
    val items = remember(deck.entries) { groupByCard(deck.entries, cardRepo) }
    if (items.isEmpty()) return

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                deck.deck.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${deck.totalCount}/50",
                style = MaterialTheme.typography.labelMedium,
                color = if (deck.totalCount == 50) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "拉出查看完整牌組內容",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp).padding(start = 4.dp),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(items, key = { it.card.id }) { cc ->
                // 牌組中實際放的刷版優先顯示縮圖，沒有才退回普卡（跟 DeckEntryRow 同樣邏輯）
                val printing = cc.card.printings.firstOrNull { (entryByPrinting[it.id] ?: 0) > 0 }
                    ?: cc.card.defaultPrinting
                val isClimax = cc.card.cardType == CardType.CLIMAX
                Box {
                    PolicyGatedCardImage(
                        url = printing.imageURL,
                        contentDescription = cc.card.nameZH,
                        networkPolicy = networkPolicy,
                        modifier = Modifier
                            .width(if (isClimax) 62.dp else 44.dp)
                            .aspectRatio(if (isClimax) 88f / 63f else 63f / 88f)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text("${cc.count}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
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
    networkPolicy: NetworkPolicy,
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
                Icon(if (usesGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (usesGrid) "改為清單顯示" else "改為圖片顯示")
            }
        }
        Box(Modifier.heightIn(max = 480.dp)) {
            DeckCardsTab(deck, items, usesGrid, networkPolicy, editable = true) { printingId, delta ->
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
    announcements: AnnouncementCenter,
    onboarding: OnboardingState,
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
                modifier = Modifier.onboardingAnchor(OnboardingStep.FILTER, onboarding),
            ) { Icon(Icons.Filled.FilterList, contentDescription = "篩選") }
            // 只在最上層（沒鎖定作品）放鈴鐺，鎖進單一作品後就不重複顯示，跟 iOS 一致
            if (pinnedTitle == null) {
                NotificationBellButton(
                    announcements,
                    modifier = Modifier.onboardingAnchor(OnboardingStep.NOTIFICATIONS, onboarding),
                    onOpen = { onboarding.notify(OnboardingStep.NOTIFICATIONS) },
                )
            }
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
        if (query.ownership != OwnershipFilter.ALL) add(query.ownership.label)
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
    sets: List<BrowsableSet>,
    totalCount: Int,
    favorites: FavoriteTitlesStore,
    onSelect: (String) -> Unit,
) {
    // 卡多的作品排前面——照代號排等於隨機順序
    val ordered = remember(sets) { sets.sortedByDescending { it.cardCount } }
    val favoriteSets = ordered.filter { favorites.isFavorite(it.id) }
    val otherSets = ordered.filter { !favorites.isFavorite(it.id) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(158.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (favoriteSets.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("已收藏") }
            items(favoriteSets, key = { it.id }) { set ->
                TitleTile(set, favorites) { onSelect(set.id) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionLabel("所有作品", Modifier.padding(top = 8.dp))
            }
        }
        items(otherSets, key = { it.id }) { set ->
            TitleTile(set, favorites) { onSelect(set.id) }
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
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun TitleTile(set: BrowsableSet, favorites: FavoriteTitlesStore, onClick: () -> Unit) {
    val color = TitlePalette.accent(set.titleCode)
    val isFavorite = favorites.isFavorite(set.id)
    Box(
        Modifier
            .height(104.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f))))
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    set.titleNameZH,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    set.titleNameJP,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                // 拆彈的官方彈次標籤（如「Vol.2」）獨立成小徽章，不跟標題文字
                // 擠在一起——之前直接接在標題後面，長一點的官方名稱會很難掃視
                set.waveLabel?.let { wave ->
                    Text(
                        wave,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(Color.White.copy(alpha = 0.22f), CircleShape)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    set.productCode ?: set.titleCode,
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
        // 觸控範圍撐大到 40dp，不然一顆小星星在色塊右上角很難點準
        IconButton(
            onClick = { favorites.toggle(set.id) },
            modifier = Modifier.align(Alignment.TopEnd).size(40.dp),
        ) {
            Icon(
                if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) androidx.compose.ui.graphics.Color(0xFFFFD54F) else Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun CardGrid(
    cards: List<Card>,
    activeDeck: DeckWithEntries?,
    deckRepo: DeckRepository,
    collectionIndex: Map<String, Int>,
    networkPolicy: NetworkPolicy,
    onboarding: OnboardingState,
    onTap: (Card) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(100.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cards, key = { it.id }) { card ->
            CardTile(card, activeDeck, deckRepo, collectionIndex, networkPolicy, onboarding, onTap)
        }
    }
}

@Composable
private fun CardTile(
    card: Card,
    activeDeck: DeckWithEntries?,
    deckRepo: DeckRepository,
    collectionIndex: Map<String, Int>,
    networkPolicy: NetworkPolicy,
    onboarding: OnboardingState,
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
    val ownedCount = remember(collectionIndex, card) { CollectionStore.owned(card, collectionIndex) }

    Column(Modifier.clickable { onTap(card) }) {
        Box {
            PolicyGatedCardImage(
                url = card.defaultPrinting.imageURL,
                contentDescription = card.nameZH,
                networkPolicy = networkPolicy,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(63f / 88f)
                    .clip(RoundedCornerShape(4.dp)),
            )
            if (ownedCount > 0) {
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Inventory2, contentDescription = "已擁有",
                        tint = Color.White, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("$ownedCount", color = Color.White,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
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
                        onboarding.notify(OnboardingStep.ADD_TO_DECK)
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .onboardingAnchor(OnboardingStep.ADD_TO_DECK, onboarding),
                ) { Icon(Icons.Filled.Add, contentDescription = "增加", modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardDetailSheet(
    card: Card,
    repo: CardRepository,
    collectionIndex: Map<String, Int>,
    collectionRepo: CollectionRepository,
    networkPolicy: NetworkPolicy,
    appearance: AppearanceSettings,
    activeDeck: DeckWithEntries?,
    deckRepo: DeckRepository,
    onSelectRelated: (Card) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val appearanceUi by appearance.ui.collectAsStateWithLifecycle()
    // 預設選第一個刷版的大圖；點下面的刷版標籤可以切著看，對應 iOS 詳情頁的大圖＋刷版切換
    var selectedPrinting by remember(card.id) { mutableStateOf(card.defaultPrinting) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PolicyGatedCardImage(
                url = selectedPrinting.imageURL,
                contentDescription = card.nameZH,
                networkPolicy = networkPolicy,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (card.cardType == CardType.CLIMAX) 88f / 63f else 63f / 88f)
                    .clip(RoundedCornerShape(10.dp)),
            )
            if (card.printings.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    card.printings.forEach { printing ->
                        FilterChip(
                            selected = printing.id == selectedPrinting.id,
                            onClick = { selectedPrinting = printing },
                            label = { Text(printing.rarity) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(card.nameZH, style = MaterialTheme.typography.titleMedium)
                    // 未翻譯的卡名兩邊一樣，顯示日文只會重複一次
                    if (appearanceUi.showJapanese && card.nameJP != card.nameZH) {
                        Text(card.nameJP, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // 中日切換：跟卡片文字一樣，開了就多顯示一份日文原文，方便對照 ruling
                IconButton(onClick = { appearance.setShowJapanese(!appearanceUi.showJapanese) }) {
                    Icon(
                        Icons.Filled.Translate,
                        contentDescription = if (appearanceUi.showJapanese) "隱藏日文原文" else "顯示日文原文",
                        tint = if (appearanceUi.showJapanese) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
            if (card.textLinesZH.isEmpty()) {
                Text("（無能力文字）", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                card.textLinesZH.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (appearanceUi.showJapanese) {
                    Spacer(Modifier.height(4.dp))
                    Text("原文（日文）", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    card.textLinesJP.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            val relations = remember(card.id) { repo.relations(card) }
            if (relations.isNotEmpty()) {
                HorizontalDivider()
                RelationsSection(relations, networkPolicy, onSelectRelated)
            }
            HorizontalDivider()
            CollectionControls(card, collectionIndex) { printingId, delta ->
                scope.launch { collectionRepo.adjust(printingId, delta) }
            }
            if (activeDeck != null) {
                HorizontalDivider()
                DeckControls(card, activeDeck) { printingId, delta ->
                    scope.launch { deckRepo.adjust(activeDeck.deck.uuid, printingId, delta) }
                }
            }
        }
    }
}

/** 關聯卡片（羈絆／CX連動／被指名），對應 iOS 的 relationsSection */
@Composable
private fun RelationsSection(
    relations: List<CardRelation>,
    networkPolicy: NetworkPolicy,
    onSelect: (Card) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("關聯卡片", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(relations, key = { it.id }) { relation ->
                Column(
                    Modifier.width(92.dp).clickable { onSelect(relation.card) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PolicyGatedCardImage(
                        url = relation.card.defaultPrinting.imageURL,
                        contentDescription = relation.card.nameZH,
                        networkPolicy = networkPolicy,
                        modifier = Modifier
                            .width(if (relation.card.cardType == CardType.CLIMAX) 84.dp else 60.dp)
                            .aspectRatio(if (relation.card.cardType == CardType.CLIMAX) 88f / 63f else 63f / 88f)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Text(
                        relation.kind.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (relation.kind == CardRelation.Kind.REFERENCED_BY)
                            MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        relation.card.nameZH,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** 加入目前作用中的牌組：跟「我的收藏」同一套 +/- 排版，對應 iOS 的 deckControls */
@Composable
private fun DeckControls(
    card: Card,
    activeDeck: DeckWithEntries,
    onAdjust: (printingId: String, delta: Int) -> Unit,
) {
    val entryByPrinting = remember(activeDeck.entries) {
        activeDeck.entries.associate { it.printingId to it.count }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "加入「${activeDeck.deck.name}」",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        card.printings.forEach { printing ->
            val count = entryByPrinting[printing.id] ?: 0
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(printing.rarity, style = MaterialTheme.typography.bodyMedium,
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
        val total = card.printings.sumOf { entryByPrinting[it.id] ?: 0 }
        if (total > 0) {
            Text(
                "合計 $total / ${DeckValidator.NAME_LIMIT} 上限",
                style = MaterialTheme.typography.labelMedium,
                color = if (total > DeckValidator.NAME_LIMIT) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 我的收藏：實際擁有幾張，依刷版分開記（對應 iOS CardDetailSheet 的 collectionControls） */
@Composable
private fun CollectionControls(
    card: Card,
    collectionIndex: Map<String, Int>,
    onAdjust: (printingId: String, delta: Int) -> Unit,
) {
    val total = remember(collectionIndex, card) { CollectionStore.owned(card, collectionIndex) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Inventory2, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("我的收藏", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (total > 0) {
                Text("共 $total 張", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        card.printings.forEach { printing ->
            val owned = collectionIndex[printing.id] ?: 0
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(printing.rarity, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(48.dp))
                Text(
                    printing.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                CountStepper(owned) { delta -> onAdjust(printing.id, delta) }
            }
        }
    }
}
