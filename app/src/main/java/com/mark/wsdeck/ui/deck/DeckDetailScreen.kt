package com.mark.wsdeck.ui.deck

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import com.mark.wsdeck.data.*
import kotlinx.coroutines.launch

/**
 * 單一牌組編輯，依等級分組；卡表／統計／缺卡切換，含出圖分享、選擇封面
 * （對應 iOS 的 DeckDetailView）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    uuid: String,
    cardRepo: CardRepository,
    deckRepo: DeckRepository,
    collectionRepo: CollectionRepository,
    networkPolicy: NetworkPolicy,
    onBack: () -> Unit,
) {
    val deckState by deckRepo.observeDeck(uuid).collectAsStateWithLifecycle(initialValue = null)
    val deck = deckState ?: return
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var mode by remember { mutableStateOf(Mode.CARDS) }
    var showRename by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showCoverPicker by remember { mutableStateOf(false) }
    var showQRPresent by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var showCollected by remember { mutableStateOf(false) }
    val prefs = remember { Prefs(context) }
    // 圖片／清單各自記憶，跨牌組共用一個設定（與 iOS 的 deckUsesGrid 一致）
    var usesGrid by remember { mutableStateOf(prefs.deckUsesGrid) }
    // 拖曳排序只在清單模式才有意義，跟「編輯」（加減張數）分開，不互相干擾
    var isReordering by remember { mutableStateOf(false) }

    val items = remember(deck.entries, deck.deck.cardOrder) {
        val grouped = groupByCard(deck.entries, cardRepo)
        val orderedIds = deck.deck.customOrder(grouped.map { it.card.id })
        val orderIndex = orderedIds.withIndex().associate { (i, id) -> id to i }
        grouped.sortedBy { orderIndex[it.card.id] ?: Int.MAX_VALUE }
    }

    // 把某分區拖曳後的新順序，寫回整副牌的排序記錄——其他分區的位置不動
    fun reorderSection(newSectionOrder: List<CardCount>) {
        val allIds = items.map { it.card.id }
        val fullOrder = deck.deck.customOrder(allIds).toMutableList()
        val sectionSet = newSectionOrder.map { it.card.id }.toSet()
        val newValues = newSectionOrder.map { it.card.id }.iterator()
        for (i in fullOrder.indices) {
            if (fullOrder[i] in sectionSet && newValues.hasNext()) {
                fullOrder[i] = newValues.next()
            }
        }
        scope.launch { deckRepo.setCardOrder(deck.deck, fullOrder) }
    }
    val validation = remember(items) { DeckValidator.validate(items) }

    val collection by collectionRepo.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val collectionIndex = remember(collection) { CollectionStore.index(collection) }
    val trackedItems = remember(deck.entries, collectionIndex) {
        CollectionStore.tracked(deck.entries, cardRepo.snapshot.cardById, collectionIndex)
    }

    fun exportShortages() {
        showMenu = false
        val text = CollectionStore.shortageText(deck.deck.name, trackedItems.filter { it.missing > 0 })
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享缺卡清單"))
    }

    fun exportImage() {
        showMenu = false
        if (items.isEmpty() || isExporting) return
        isExporting = true
        scope.launch {
            val file = DeckImageExporter.render(
                context = context,
                deckName = deck.deck.name,
                entries = deck.entries,
                items = items,
                imageLoader = SingletonImageLoader.get(context),
            )
            isExporting = false
            if (file == null) {
                Toast.makeText(context, "出圖失敗", Toast.LENGTH_SHORT).show()
                return@launch
            }
            // FileProvider 換成 content:// URI——直接給 file:// 在新版 Android
            // 會被 FileUriExposedException 擋下來
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享牌組圖片"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deck.deck.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 卡表模式才需要切換圖片／清單
                    if (mode == Mode.CARDS) {
                        IconButton(onClick = {
                            usesGrid = !usesGrid
                            prefs.deckUsesGrid = usesGrid
                        }) {
                            Icon(
                                if (usesGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = if (usesGrid) "改為清單顯示" else "改為圖片顯示",
                            )
                        }
                        // 拖曳排序只在清單模式才有意義（原生格線的排列跟著清單順序走，
                        // 但排序動作要回清單模式做）
                        if (!usesGrid) {
                            IconButton(onClick = { isReordering = !isReordering }) {
                                Icon(
                                    if (isReordering) Icons.Filled.Check else Icons.Filled.SwapVert,
                                    contentDescription = if (isReordering) "完成排序" else "拖曳排序卡表",
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            if (isExporting) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                            }
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("重新命名") }, onClick = {
                                showMenu = false; showRename = true
                            })
                            DropdownMenuItem(
                                text = { Text("選擇封面") },
                                onClick = { showMenu = false; showCoverPicker = true },
                                enabled = items.isNotEmpty(),
                            )
                            DropdownMenuItem(
                                text = { Text("出示 QR 給朋友掃") },
                                onClick = { showMenu = false; showQRPresent = true },
                                enabled = items.isNotEmpty(),
                            )
                            DropdownMenuItem(
                                text = { Text("匯出牌組圖片（可掃回）") },
                                onClick = ::exportImage,
                                enabled = items.isNotEmpty(),
                            )
                            DropdownMenuItem(
                                text = { Text("匯出缺卡清單") },
                                onClick = ::exportShortages,
                            )
                        }
                    }
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
                Mode.CARDS -> DeckCardsTab(
                    deck = deck,
                    items = items,
                    usesGrid = usesGrid,
                    networkPolicy = networkPolicy,
                    isReordering = isReordering,
                    onReorderSection = ::reorderSection,
                    onAdjust = { printingId, delta ->
                        scope.launch { deckRepo.adjust(uuid, printingId, delta) }
                    },
                )
                Mode.STATS -> DeckStatsView(items)
                Mode.SHORTAGE -> DeckShortageTab(
                    items = trackedItems,
                    networkPolicy = networkPolicy,
                    showCollected = showCollected,
                    onToggleShowCollected = { showCollected = !showCollected },
                    onAdjust = { printingId, delta ->
                        scope.launch { collectionRepo.adjust(printingId, delta) }
                    },
                    onFill = { shortage ->
                        scope.launch { collectionRepo.fill(shortage.printing.id, shortage.owned, shortage.needed) }
                    },
                    onFillAll = {
                        scope.launch {
                            for (shortage in trackedItems.filter { it.missing > 0 }) {
                                collectionRepo.fill(shortage.printing.id, shortage.owned, shortage.needed)
                            }
                        }
                    },
                )
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

    if (showCoverPicker) {
        DeckCoverPickerView(deck, cardRepo, deckRepo, networkPolicy) { showCoverPicker = false }
    }

    if (showQRPresent) {
        DeckQRPresentDialog(deckName = deck.deck.name, entries = deck.entries) { showQRPresent = false }
    }
}

private enum class Mode(val label: String) { CARDS("卡表"), STATS("統計"), SHORTAGE("缺卡") }

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
