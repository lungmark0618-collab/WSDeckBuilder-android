package com.mark.wsdeck.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.AnnouncementCenter
import com.mark.wsdeck.data.WSNewsItem
import com.mark.wsdeck.data.WSNewsRepository
import com.mark.wsdeck.ui.notifications.NotificationBellButton
import com.mark.wsdeck.ui.onboarding.onboardingAnchor
import com.mark.wsdeck.data.OnboardingState
import com.mark.wsdeck.data.OnboardingStep
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
) {
    val ui by newsRepo.ui.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

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
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                    NewsRow(item) { uriHandler.openUri(item.url) }
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
}

@Composable
private fun NewsRow(item: WSNewsItem, onClick: () -> Unit) {
    val accent = item.categories.firstOrNull()?.let { categoryColor(it) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            // Row 預設高度是 wrap content，子項目的 fillMaxHeight 沒有邊界可撐，
            // 加這行讓左側色條能撐到跟內文一樣高（Compose 等高 Row 的標準寫法）
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
    ) {
        // 左側色條標出這則公告的分類，掃視列表時比純文字色塊更容易一眼區分
        Box(Modifier.width(4.dp).fillMaxHeight().background(accent.copy(alpha = 0.85f)))
        Column(Modifier.weight(1f).padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 8.dp)) {
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
            Spacer(Modifier.height(6.dp))
            Text(
                item.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp),
        )
    }
}

/** 純粹視覺分類，跟官網分類文字用簡單對照，沒對到的一律灰色 */
private fun categoryColor(category: String): Color = when (category) {
    "商品情報" -> Color(0xFF2196F3)
    "カードリスト" -> Color(0xFF4CAF50)
    "大会", "イベント" -> Color(0xFFFF9800)
    "ルール" -> Color(0xFF9C27B0)
    "デッキレシピ" -> Color(0xFFE91E63)
    else -> Color(0xFF9E9E9E)
}

