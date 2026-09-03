package com.mark.wsdeck.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Newspaper
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    val scope = rememberCoroutineScope()
    // 點公告先看我們整理過的重點，不是直接跳出 App 到瀏覽器——
    // 有興趣看完整內容的人，詳情頁裡還有官網連結
    var selectedItem by remember { mutableStateOf<WSNewsItem?>(null) }

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
        NewsDetailDialog(item) { selectedItem = null }
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

/** 公告詳情：先讓使用者看重點（規格重點或至少標題／分類／日期），
 *  有興趣才點下面的按鈕去官網看完整內容——不是點一下就直接跳出 App。 */
@Composable
private fun NewsDetailDialog(item: WSNewsItem, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
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

