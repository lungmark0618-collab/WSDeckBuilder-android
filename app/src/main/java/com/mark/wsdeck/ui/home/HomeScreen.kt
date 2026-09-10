package com.mark.wsdeck.ui.home

import com.mark.wsdeck.ui.shared.SidebarMenuButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.wsdeck.ui.shared.SwipeBackDialog as Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.AnnouncementCenter
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.CardType
import com.mark.wsdeck.data.DeckRepository
import com.mark.wsdeck.data.DeckWithEntries
import com.mark.wsdeck.data.NetworkPolicy
import com.mark.wsdeck.data.NewsCategory
import com.mark.wsdeck.data.NewsCategoryFilterStore
import com.mark.wsdeck.data.PinnedDecksStore
import com.mark.wsdeck.data.WSNewsItem
import com.mark.wsdeck.data.WSNewsRepository
import com.mark.wsdeck.data.coverPrinting
import com.mark.wsdeck.ui.notifications.NotificationBellButton
import com.mark.wsdeck.ui.onboarding.onboardingAnchor
import com.mark.wsdeck.data.OnboardingState
import com.mark.wsdeck.data.OnboardingStep
import com.mark.wsdeck.ui.shared.PolicyGatedCardImage
import kotlinx.coroutines.launch

/**
 * App 開啟後第一眼看到的畫面：官網公告（新商品、卡表更新、大會、規則異動），
 * 取代原本開場就是圖鑑的安排——這是使用者主動要求的首頁。對應 iOS 的 HomeView。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    newsRepo: WSNewsRepository,
    announcements: AnnouncementCenter,
    onboarding: OnboardingState,
    networkPolicy: NetworkPolicy,
    cardRepo: CardRepository,
    deckRepo: DeckRepository,
    pinnedDecks: PinnedDecksStore,
    newsCategoryFilter: NewsCategoryFilterStore,
    onOpenDeck: (String) -> Unit,
) {
    val ui by newsRepo.ui.collectAsStateWithLifecycle()
    val allDecks by deckRepo.observeDecks().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    // 點公告先看我們整理過的重點，不是直接跳出 App 到瀏覽器——
    // 有興趣看完整內容的人，詳情頁裡還有官網連結
    var selectedItem by remember { mutableStateOf<WSNewsItem?>(null) }
    var showingCategoryFilter by remember { mutableStateOf(false) }
    // 搜尋「最新動態」用的關鍵字——比對標題（中日文）跟商品規格重點，
    // 對應 iOS HomeView 的 searchText
    var searchText by remember { mutableStateOf("") }
    val isSearching = searchText.isNotBlank()
    // 依釘選順序排出實際存在的牌組——牌組被刪掉但清理沒跑到的殘影
    // （理論上不會發生，DeckListScreen 刪牌組時已經呼叫 pinnedDecks.remove，
    // 這裡只是多一層防呆）就自然濾掉，不會顯示空卡片
    val pinnedDecksOrdered = remember(allDecks, pinnedDecks.uuids) {
        val byUuid = allDecks.associateBy { it.deck.uuid }
        pinnedDecks.uuids.mapNotNull { byUuid[it] }
    }
    // 套用使用者的分類篩選——輪播跟列表共用同一份結果，免得使用者把某分類
    // 關掉了，卻還在輪播裡看到；再疊上關鍵字搜尋，比對標題（中日文）跟商品
    // 規格重點，不比對分類標籤本身
    val filteredItems = remember(ui.items, newsCategoryFilter.hidden, searchText) {
        val categoryFiltered = ui.items.filter { newsCategoryFilter.isVisible(it) }
        if (!isSearching) return@remember categoryFiltered
        val keyword = searchText.trim()
        categoryFiltered.filter { item ->
            item.titleZH?.contains(keyword, ignoreCase = true) == true ||
                item.titleJP.contains(keyword, ignoreCase = true) ||
                item.highlightsZH.any { it.contains(keyword, ignoreCase = true) }
        }
    }
    // 輪播只挑有配圖、跟商品/卡表有關的公告——參考官網首頁「最新商品」跑馬燈的
    // 做法，規則更新、賽事這類沒有視覺重點的公告不適合放大圖展示
    val heroItems = remember(filteredItems) {
        filteredItems.filter {
            it.imageURL != null && ("商品情報" in it.categories || "カードリスト" in it.categories)
        }.take(6)
    }

    LaunchedEffect(Unit) {
        if (ui.items.isEmpty()) newsRepo.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { SidebarMenuButton() }, title = { Text("首頁") },
                actions = {
                    IconButton(onClick = { showingCategoryFilter = true }) {
                        Icon(
                            if (newsCategoryFilter.hidden.isEmpty()) Icons.Filled.FilterList else Icons.Filled.FilterAlt,
                            contentDescription = "篩選首頁公告",
                        )
                    }
                    NotificationBellButton(
                        announcements,
                        modifier = Modifier.onboardingAnchor(OnboardingStep.NOTIFICATIONS, onboarding),
                        onOpen = { onboarding.notify(OnboardingStep.NOTIFICATIONS) },
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("搜尋最新動態") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 搜尋中就只顯示比對結果，把常用牌組、輪播收起來——
            // 這兩區跟關鍵字無關，留著只會讓人分心找不到搜尋結果在哪
            if (!isSearching && pinnedDecksOrdered.isNotEmpty()) {
                item {
                    PinnedDecksRow(pinnedDecksOrdered, cardRepo, networkPolicy, onOpenDeck)
                }
            }
            if (!isSearching && heroItems.isNotEmpty()) {
                item {
                    HeroCarousel(
                        heroItems, networkPolicy,
                        modifier = Modifier,
                    ) { selectedItem = it }
                }
            }
            item {
                Text(if (isSearching) "搜尋結果" else "最新動態",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                        .onboardingAnchor(OnboardingStep.HOME_INTRO, onboarding))
            }
            if (ui.isLoading && ui.items.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
            } else if (!isSearching && filteredItems.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (ui.items.isEmpty()) "暫時沒有消息" else "沒有符合的公告")
                        TextButton(onClick = {
                            if (ui.items.isEmpty()) scope.launch { newsRepo.refresh() }
                            else showingCategoryFilter = true
                        }) { Text(if (ui.items.isEmpty()) "重新載入" else "調整分類") }
                    }
                }
            }
            ui.errorMessage?.let { message ->
                item {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE68A00),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            if (isSearching && filteredItems.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("沒有符合的消息", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "換個關鍵字試試，或確認分類篩選有沒有把它藏起來。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(filteredItems, key = { it.date + it.titleJP + it.url }) { item ->
                NewsRow(item) { selectedItem = item }
            }
            item {
                TextButton(
                    onClick = { scope.launch { newsRepo.refresh() } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (ui.isLoading) "重新整理中…" else "重新整理") }
            }
        }
    }

    selectedItem?.let { item ->
        NewsDetailDialog(item, networkPolicy) { selectedItem = null }
    }
    if (showingCategoryFilter) {
        NewsCategoryFilterDialog(newsCategoryFilter) { showingCategoryFilter = false }
    }
}

/** 首頁公告分類篩選——關掉不想看的分類，輪播跟列表都會跟著濾掉 */
@Composable
private fun NewsCategoryFilterDialog(store: NewsCategoryFilterStore, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "篩選首頁公告",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                // 同一顆按鈕：全部隱藏時顯示「全選」，全部顯示時變成「全部清除」
                val allHidden = store.hidden.size == NewsCategory.all.size
                TextButton(onClick = { if (allHidden) store.showAll() else store.hideAll(NewsCategory.all) }) {
                    Text(if (allHidden) "全選" else "全部清除")
                }
            }
            Spacer(Modifier.height(12.dp))
            NewsCategory.all.forEach { category ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { store.toggle(category) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .rotate(45f)
                            .background(NewsCategory.color(category), RoundedCornerShape(1.5.dp)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(NewsCategory.labelZH(category), modifier = Modifier.weight(1f))
                    if (!store.isHidden(category)) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("完成") }
        }
    }
}

@Composable
private fun NewsRow(item: WSNewsItem, onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.categories.joinToString(" · ") { NewsCategory.labelZH(it) },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.displayTitle, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(item.date.replace("-", "."), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
    }
}

/** 常用牌組快速列——使用者在「牌組」分頁點釘選圖示，最想順手開的幾副牌組
 *  就不用再多切一次分頁、多找一次。放在輪播上面，因為這是「我自己的東西」，
 *  每次開 App 大概都想先看一眼，比官網公告更優先。 */
@Composable
private fun PinnedDecksRow(
    decks: List<DeckWithEntries>,
    cardRepo: CardRepository,
    networkPolicy: NetworkPolicy,
    onOpen: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "常用牌組",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(decks, key = { it.deck.uuid }) { d ->
                PinnedDeckCard(d, cardRepo, networkPolicy) { onOpen(d.deck.uuid) }
            }
        }
    }
}

@Composable
private fun PinnedDeckCard(
    d: DeckWithEntries,
    cardRepo: CardRepository,
    networkPolicy: NetworkPolicy,
    onClick: () -> Unit,
) {
    val cover = remember(d) { d.coverPrinting(cardRepo) }
    val isClimax = remember(cover) {
        cover?.let { p -> cardRepo.snapshot.cardById[p.id]?.cardType == CardType.CLIMAX } ?: false
    }
    Row(
        Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cover != null) {
            PolicyGatedCardImage(
                url = cover.imageURL,
                contentDescription = null,
                networkPolicy = networkPolicy,
                modifier = Modifier
                    .width(if (isClimax) 60.dp else 42.dp)
                    .aspectRatio(if (isClimax) 88f / 63f else 63f / 88f)
                    .clip(RoundedCornerShape(6.dp)),
            )
        } else {
            Box(
                Modifier
                    .width(42.dp)
                    .aspectRatio(63f / 88f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Style, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                d.deck.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                "${d.totalCount} 張",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
    }
}

/** 首頁保留商品大圖，配圖與標題分區；輪播上方不額外放宣傳文字。 */
@Composable
private fun HeroCarousel(
    items: List<WSNewsItem>,
    networkPolicy: NetworkPolicy,
    modifier: Modifier = Modifier,
    onSelect: (WSNewsItem) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(state = pagerState) { page ->
            val item = items[page]
            HeroSlide(item, networkPolicy) {
                onSelect(item)
            }
        }
        if (items.size > 1) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                items.indices.forEach { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        Modifier
                            .size(width = if (selected) 16.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSlide(item: WSNewsItem, networkPolicy: NetworkPolicy, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(28.dp)),
    ) {
        item.imageURL?.let { url ->
            PolicyGatedCardImage(url = url, contentDescription = item.displayTitle,
                networkPolicy = networkPolicy, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(220.dp).clickable(onClick = onClick))
        }
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.categories.firstOrNull()?.let { NewsCategory.labelZH(it) } ?: "商品資訊",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.displayTitle, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2, maxLines = 2)
            }
            Spacer(Modifier.width(16.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NewsDetailDialog(item: WSNewsItem, networkPolicy: NetworkPolicy, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
            item.bestImageURL?.let { url ->
                // 商品包裝圖官網來源正方形、長方形都有，用 Fit 完整顯示不裁切——
                // 裁切填滿常常把包裝上的字或圖案切掉一半，使用者反映「圖片位置跑掉」
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                ) {
                    PolicyGatedCardImage(
                        url = url,
                        contentDescription = item.displayTitle,
                        networkPolicy = networkPolicy,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.categories.forEach { category ->
                    Text(
                        NewsCategory.labelZH(category),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NewsCategory.color(category),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .background(NewsCategory.color(category).copy(alpha = 0.16f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    item.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(item.displayTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            if (item.highlightsZH.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(14.dp),
                ) {
                    Text(
                        "重點整理",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    item.highlightsZH.forEach { line ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(line, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                // 規則更新、賽事公告這類抓不到規格表的公告，老實說沒有重點可以整理，
                // 不硬湊內容，直接請使用者去官網看
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(14.dp),
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "這則公告沒有可摘要的規格資訊，詳細內容請至官網查看。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { uriHandler.openUri(item.url) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("前往官網查看完整內容")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("關閉") }
        }
    }
}

