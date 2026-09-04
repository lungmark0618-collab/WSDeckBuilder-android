package com.mark.wsdeck.ui.home

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Newspaper
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.AnnouncementCenter
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.CardType
import com.mark.wsdeck.data.DeckRepository
import com.mark.wsdeck.data.DeckWithEntries
import com.mark.wsdeck.data.NetworkPolicy
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
    onOpenDeck: (String) -> Unit,
) {
    val ui by newsRepo.ui.collectAsStateWithLifecycle()
    val allDecks by deckRepo.observeDecks().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    // 點公告先看我們整理過的重點，不是直接跳出 App 到瀏覽器——
    // 有興趣看完整內容的人，詳情頁裡還有官網連結
    var selectedItem by remember { mutableStateOf<WSNewsItem?>(null) }
    // 依釘選順序排出實際存在的牌組——牌組被刪掉但清理沒跑到的殘影
    // （理論上不會發生，DeckListScreen 刪牌組時已經呼叫 pinnedDecks.remove，
    // 這裡只是多一層防呆）就自然濾掉，不會顯示空卡片
    val pinnedDecksOrdered = remember(allDecks, pinnedDecks.uuids) {
        val byUuid = allDecks.associateBy { it.deck.uuid }
        pinnedDecks.uuids.mapNotNull { byUuid[it] }
    }
    // 輪播只挑有配圖、跟商品/卡表有關的公告——參考官網首頁「最新商品」跑馬燈的
    // 做法，規則更新、賽事這類沒有視覺重點的公告不適合放大圖展示
    val heroItems = remember(ui.items) {
        ui.items.filter {
            it.imageURL != null && ("商品情報" in it.categories || "カードリスト" in it.categories)
        }.take(6)
    }

    LaunchedEffect(Unit) {
        if (ui.items.isEmpty()) newsRepo.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("首頁") },
                actions = {
                    NotificationBellButton(
                        announcements,
                        modifier = Modifier.onboardingAnchor(OnboardingStep.NOTIFICATIONS, onboarding),
                        onOpen = { onboarding.notify(OnboardingStep.NOTIFICATIONS) },
                    )
                },
            )
        },
    ) { padding ->
        when {
            ui.items.isEmpty() && ui.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding), Alignment.Center,
            ) { CircularProgressIndicator() }

            ui.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Newspaper, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("還沒有公告，下拉重新整理試試看")
                }
            }

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).background(Color(0xFF05060C)).drawBehind {
                    // 全息光暈——呼應集換式卡牌本身的「卡背」質感，
                    // 淡淡三團色暈疊在近黑底色上，不搶內容但讓畫面不死板
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color(0xFFBF5AF2).copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * -0.02f),
                            radius = 480f,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color(0xFFFF9F0A).copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * -0.1f, size.height * 0.10f),
                            radius = 460f,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color(0xFFD95999).copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.9f),
                            radius = 640f,
                        ),
                    )
                },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (pinnedDecksOrdered.isNotEmpty()) {
                    item {
                        PinnedDecksRow(pinnedDecksOrdered, cardRepo, networkPolicy, onOpenDeck)
                    }
                }
                if (heroItems.isNotEmpty()) {
                    item {
                        HeroCarousel(
                            heroItems, networkPolicy,
                            modifier = Modifier.onboardingAnchor(OnboardingStep.HOME_INTRO, onboarding),
                        ) { selectedItem = it }
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
                items(ui.items, key = { it.date + it.titleJP + it.url }) { item ->
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
    }

    selectedItem?.let { item ->
        NewsDetailDialog(item, networkPolicy) { selectedItem = null }
    }
}

@Composable
private fun NewsRow(item: WSNewsItem, onClick: () -> Unit) {
    val foilColor = item.categories.firstOrNull()?.let { categoryColor(it) } ?: Color(0xFF9E9E9E)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF14151F), Color(0xFF0C0D14))),
            )
            .drawBehind {
                // 卡角的全息燙金色塊——這就是「B·全息卡背」跟其他方向的核心差異
                val ribbon = 96.dp.toPx()
                withTransform({
                    translate(size.width, 0f)
                    rotate(45f, pivot = Offset.Zero)
                }) {
                    drawRect(foilColor.copy(alpha = 0.16f), size = androidx.compose.ui.geometry.Size(ribbon, ribbon))
                }
            }
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.categories.forEach { category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    // 小菱形「寶石」取代原本的色塊膠囊，呼應卡牌稀有度標記
                    Box(
                        Modifier
                            .size(6.dp)
                            .rotate(45f)
                            .background(categoryColor(category), RoundedCornerShape(1.5.dp)),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp,
                        color = categoryTint(category),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                item.date.replace("-", "."),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.42f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.displayTitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF5F5F5),
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp),
            )
        }
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
            color = Color.White.copy(alpha = 0.7f),
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
            .background(Color(0xFF1C1C1F))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
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
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Style, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                d.deck.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                "${d.totalCount} 張",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

/** 首頁最上方的大圖輪播——參考官網首頁「最新商品」跑馬燈：整張商品視覺圖
 *  滿版顯示、左右滑動切換、底部疊標題跟日期，比純文字列表更能一眼抓住
 *  「現在有什麼新東西」 */
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
            HeroSlide(item, categoryColor(item.categories.firstOrNull() ?: ""), networkPolicy) {
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
                            .background(if (selected) Color.White else Color.White.copy(alpha = 0.28f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSlide(item: WSNewsItem, accent: Color, networkPolicy: NetworkPolicy, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        item.imageURL?.let { url ->
            PolicyGatedCardImage(
                url = url,
                contentDescription = item.displayTitle,
                networkPolicy = networkPolicy,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.92f)),
                    ),
                ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .rotate(45f)
                        .background(accent, RoundedCornerShape(1.5.dp)),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    item.categories.firstOrNull() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    item.date.replace("-", "."),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Text(
                item.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2,
            )
        }
    }
}

/** 公告詳情：先讓使用者看重點（規格重點或至少標題／分類／日期），
 *  有興趣才點下面的按鈕去官網看完整內容——不是點一下就直接跳出 App。 */
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
                        category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor(category),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .background(categoryColor(category).copy(alpha = 0.16f), CircleShape)
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

/** 卡角燙金色塊、寶石標記共用的飽和色 */
private fun categoryColor(category: String): Color = when (category) {
    "商品情報" -> Color(0xFF2196F3)
    "カードリスト" -> Color(0xFF4CAF50)
    "大会", "イベント" -> Color(0xFFFF9800)
    "ルール" -> Color(0xFF9C27B0)
    "デッキレシピ" -> Color(0xFFE91E63)
    else -> Color(0xFF9E9E9E)
}

/** 分類文字用的淺色調，飽和色直接當文字色在深底上太刺眼 */
private fun categoryTint(category: String): Color = when (category) {
    "商品情報" -> Color(0xFF6DB8FF)
    "カードリスト" -> Color(0xFF7BDF9E)
    "大会", "イベント" -> Color(0xFFFFBD6B)
    "ルール" -> Color(0xFFD9A3FF)
    "デッキレシピ" -> Color(0xFFFF8FAB)
    else -> Color(0xFF9E9E9E)
}

