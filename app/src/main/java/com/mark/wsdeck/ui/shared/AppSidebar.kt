package com.mark.wsdeck.ui.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.mark.wsdeck.data.*

enum class SidebarImportAction(val title: String, val icon: ImageVector) {
    CAMERA("相機掃描 QR Code", Icons.Default.QrCodeScanner),
    PHOTO("從圖片匯入", Icons.Default.PhotoLibrary),
    FILE("從檔案匯入", Icons.Default.Folder),
    TEXT("貼上牌表文字", Icons.Default.ContentPaste),
}

class SidebarNavigation(val open: () -> Unit) {
    var titleRequest by mutableStateOf<String?>(null)
    var importRequest by mutableStateOf<SidebarImportAction?>(null)
}
val LocalSidebarNavigation = staticCompositionLocalOf { SidebarNavigation {} }

@Composable
fun SidebarMenuButton() {
    val navigation = LocalSidebarNavigation.current
    val keyboard = LocalSoftwareKeyboardController.current
    IconButton(onClick = { keyboard?.hide(); navigation.open() }) {
        Icon(Icons.Default.Menu, contentDescription = "開啟導覽選單")
    }
}

@Composable
fun AppSidebar(
    decks: List<DeckWithEntries>, activeUuid: String?, pinned: PinnedDecksStore,
    favorites: FavoriteTitlesStore, repo: CardRepository,
    onClose: () -> Unit, onRoute: (String) -> Unit, onDeck: (String) -> Unit,
    onTitle: (String) -> Unit, onImport: (SidebarImportAction) -> Unit, onAI: () -> Unit,
) {
    var importsExpanded by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("WS Deck Builder", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "關閉選單") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            decks.firstOrNull { it.deck.uuid == activeUuid }?.let { active ->
                SidebarHeading("繼續編輯")
                SidebarRow(active.deck.name, Icons.Default.Edit, "${active.totalCount} 張卡片") { onDeck(active.deck.uuid) }
            }
            SidebarHeading("瀏覽")
            SidebarRow("首頁", Icons.Default.Home) { onRoute("home") }
            SidebarRow("圖鑑", Icons.Default.Search) { onRoute("catalog") }
            SidebarRow("我的牌組", Icons.Default.Style) { onRoute("decks") }
            SidebarHeading("常用牌組")
            val pinnedItems = pinned.uuids.mapNotNull { id -> decks.firstOrNull { it.deck.uuid == id } }
            if (pinnedItems.isEmpty()) SidebarHint("在牌組清單釘選牌組，就能從這裡快速開啟。")
            pinnedItems.forEach { deck ->
                SidebarRow(deck.deck.name, Icons.Default.PushPin, "${deck.totalCount} 張卡片") { onDeck(deck.deck.uuid) }
            }
            SidebarHeading("收藏作品")
            if (favorites.titleCodes.isEmpty()) SidebarHint("在圖鑑點作品上的星星，就能從這裡快速開啟。")
            favorites.titleCodes.sorted().forEach { code ->
                SidebarRow(repo.scopeDisplayName(code), Icons.Default.StarBorder) { onTitle(code) }
            }
            SidebarHeading("工具")
            SidebarRow("匯入牌組", if (importsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore) {
                importsExpanded = !importsExpanded
            }
            if (importsExpanded) SidebarImportAction.entries.forEach { action ->
                SidebarRow(action.title, action.icon) { onImport(action) }
            }
            SidebarRow("AI 助手", Icons.Default.ChatBubbleOutline, onClick = onAI)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SidebarRow("外觀與設定", Icons.Default.Settings) { onRoute("settings") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SidebarHeading(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 8.dp))
}
@Composable
private fun SidebarHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
}
@Composable
private fun SidebarRow(title: String, icon: ImageVector, subtitle: String? = null, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Icon(icon, null, Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
