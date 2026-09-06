package com.mark.wsdeck.ui.deck

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.CardColor
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.CardType
import com.mark.wsdeck.data.DeckEntryEntity
import com.mark.wsdeck.data.DeckImageExporter
import com.mark.wsdeck.data.DeckImageImporter
import com.mark.wsdeck.data.DeckImporter
import com.mark.wsdeck.data.DeckRepository
import com.mark.wsdeck.data.DeckWithEntries
import com.mark.wsdeck.data.NetworkPolicy
import com.mark.wsdeck.data.OnboardingState
import com.mark.wsdeck.data.OnboardingStep
import com.mark.wsdeck.data.PinnedDecksStore
import com.mark.wsdeck.data.Prefs
import com.mark.wsdeck.data.coverPrinting
import com.mark.wsdeck.ui.onboarding.onboardingAnchor
import com.mark.wsdeck.ui.shared.PolicyGatedCardImage
import kotlinx.coroutines.launch

/** 牌組列表：建立、重新命名、刪除、掃圖匯入（對應 iOS 的 DeckListView） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    cardRepo: CardRepository,
    repo: DeckRepository,
    networkPolicy: NetworkPolicy,
    onboarding: OnboardingState,
    pinnedDecks: PinnedDecksStore,
    onOpen: (String) -> Unit,
) {
    val decks by repo.observeDecks().collectAsStateWithLifecycle(initialValue = emptyList())
    var showCreate by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DeckWithEntries?>(null) }
    var importResult by remember { mutableStateOf<DeckImporter.Result?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showQRScanner by remember { mutableStateOf(false) }
    var showPasteImport by remember { mutableStateOf(false) }
    var renamingDeck by remember { mutableStateOf<DeckWithEntries?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    // 哪副牌組現在接圖鑑的加卡動作，對應 iOS 的 activeDeckUUID；讀取沒有訂閱機制，
    // 每次這個畫面重新進場時取最新值就好，跟卡片詳情頁的用法一致
    val activeDeckUuid = prefs.activeDeckUuid

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = try {
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            } catch (e: Exception) {
                null
            }
            if (bitmap == null) {
                importError = "無法讀取這張圖片。"
                return@launch
            }
            when (val parsed = DeckImageImporter.parse(bitmap)) {
                is DeckImageImporter.Result.Success -> try {
                    val result = DeckImporter.createDeck(
                        parsed.parsed, cardRepo, repo, decks.map { it.deck.name },
                    )
                    prefs.activeDeckUuid = result.deckUuid
                    importResult = result
                } catch (e: DeckImporter.NoCardsFoundException) {
                    importError = e.message
                }
                DeckImageImporter.Result.NoCode -> importError = "這張圖片裡沒有找到 QR。"
                DeckImageImporter.Result.Unrecognized ->
                    importError = "這個 QR 不是本 App 匯出的牌組。"
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
            if (text == null) {
                importError = "無法讀取這個檔案。"
                return@launch
            }
            try {
                val parsed = DeckImporter.parse(text)
                val result = DeckImporter.createDeck(
                    parsed, cardRepo, repo, decks.map { it.deck.name },
                )
                prefs.activeDeckUuid = result.deckUuid
                importResult = result
            } catch (e: DeckImporter.NoCardsFoundException) {
                importError = e.message
            } catch (e: DeckImporter.UnreadableTextException) {
                importError = e.message
            }
        }
    }

    fun importPastedText(text: String) {
        showPasteImport = false
        scope.launch {
            try {
                val parsed = DeckImporter.parse(text)
                val result = DeckImporter.createDeck(
                    parsed, cardRepo, repo, decks.map { it.deck.name },
                )
                prefs.activeDeckUuid = result.deckUuid
                importResult = result
            } catch (e: DeckImporter.NoCardsFoundException) {
                importError = e.message
            } catch (e: DeckImporter.UnreadableTextException) {
                importError = e.message
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("牌組") }) },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showAddMenu = true },
                    modifier = Modifier.onboardingAnchor(OnboardingStep.CREATE_DECK, onboarding),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "新增牌組")
                }
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("新增空牌組") },
                        leadingIcon = { Icon(Icons.Filled.PostAdd, contentDescription = null) },
                        onClick = { showAddMenu = false; showCreate = true },
                    )
                    DropdownMenuItem(
                        text = { Text("開啟相機掃描") },
                        leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                        onClick = { showAddMenu = false; showQRScanner = true },
                    )
                    DropdownMenuItem(
                        text = { Text("掃牌組圖片匯入") },
                        leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                        onClick = {
                            showAddMenu = false
                            photoPicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("從檔案匯入牌表") },
                        leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        onClick = {
                            showAddMenu = false
                            filePicker.launch(arrayOf("text/plain", "*/*"))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("貼上牌表文字匯入") },
                        leadingIcon = { Icon(Icons.Filled.ContentPaste, contentDescription = null) },
                        onClick = { showAddMenu = false; showPasteImport = true },
                    )
                }
            }
        },
    ) { padding ->
        if (decks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("還沒有牌組，按右下角建立一個",
                    style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(decks, key = { _, d -> d.deck.uuid }) { index, d ->
                    DeckRow(
                        d, cardRepo, networkPolicy,
                        isActive = d.deck.uuid == activeDeckUuid,
                        onClick = { onOpen(d.deck.uuid) },
                        onDelete = { pendingDelete = d },
                        onRename = { renamingDeck = d },
                        onDuplicate = { scope.launch { repo.duplicateDeck(d) } },
                        isPinned = pinnedDecks.isPinned(d.deck.uuid),
                        onTogglePin = {
                            pinnedDecks.toggle(d.deck.uuid)
                            onboarding.notify(OnboardingStep.PIN_DECKS)
                        },
                        // 教學光圈只該指第一列，不然每一列都圈起來很亂
                        pinAnchor = if (index == 0) {
                            Modifier.onboardingAnchor(OnboardingStep.PIN_DECKS, onboarding)
                        } else Modifier,
                    )
                }
            }
        }
    }

    if (showCreate) {
        NameDialog(
            title = "新牌組",
            initial = "",
            onConfirm = { name ->
                scope.launch { repo.createDeck(name.ifBlank { "新牌組" }) }
                onboarding.notify(OnboardingStep.CREATE_DECK)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }

    renamingDeck?.let { d ->
        NameDialog(
            title = "重新命名",
            initial = d.deck.name,
            onConfirm = { name ->
                scope.launch { repo.renameDeck(d.deck, name.ifBlank { d.deck.name }) }
                renamingDeck = null
            },
            onDismiss = { renamingDeck = null },
        )
    }

    pendingDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除「${d.deck.name}」？") },
            text = { Text("牌組內的卡片配置會一併刪除，無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.deleteDeck(d.deck.uuid) }
                    pinnedDecks.remove(d.deck.uuid)
                    pendingDelete = null
                }) { Text("刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }

    importResult?.let { result ->
        AlertDialog(
            onDismissRequest = { importResult = null },
            title = { Text("匯入完成") },
            text = { Text(importMessage(result)) },
            confirmButton = { TextButton(onClick = { importResult = null }) { Text("好") } },
        )
    }

    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("匯入失敗") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { importError = null }) { Text("好") } },
        )
    }

    if (showQRScanner) {
        DeckQRScannerDialog(
            onDetect = { text ->
                showQRScanner = false
                val parsed = DeckImageExporter.Payload.decode(text)
                if (parsed == null) {
                    importError = "掃到的內容不是本 App 的牌組分享資料。"
                    return@DeckQRScannerDialog
                }
                scope.launch {
                    try {
                        val result = DeckImporter.createDeck(
                            parsed, cardRepo, repo, decks.map { it.deck.name },
                        )
                        prefs.activeDeckUuid = result.deckUuid
                        importResult = result
                    } catch (e: DeckImporter.NoCardsFoundException) {
                        importError = e.message
                    }
                }
            },
            onDismiss = { showQRScanner = false },
        )
    }

    if (showPasteImport) {
        PasteImportDialog(
            onConfirm = ::importPastedText,
            onDismiss = { showPasteImport = false },
        )
    }
}

/** 貼上本 App 匯出的 JSON 備份、牌表文字，或每行一張卡號的清單來新增牌組 */
@Composable
private fun PasteImportDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("貼上牌表文字匯入") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("貼上 JSON 備份、牌表文字，或每行一張卡號") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("匯入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun importMessage(result: DeckImporter.Result): String {
    val lines = mutableListOf(
        "已建立「${result.deckName}」",
        "匯入 ${result.importedCards} 張（${result.matchedKinds} 種）",
    )
    if (result.skipped.isNotEmpty()) {
        val shown = result.skipped.take(5).joinToString("、")
        lines += "略過 ${result.skipped.size} 個查不到的卡號：$shown" +
            if (result.skipped.size > 5) "…" else ""
    }
    return lines.joinToString("\n")
}

@Composable
private fun DeckRow(
    d: DeckWithEntries,
    cardRepo: CardRepository,
    networkPolicy: NetworkPolicy,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    pinAnchor: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    // 使用者指定的封面優先，否則自動取等級最高的一張（可在牌組詳情頁「選擇封面」調整）
    val cover = remember(d) { d.coverPrinting(cardRepo) }
    val isClimax = remember(cover) {
        cover?.let { p -> cardRepo.snapshot.cardById[p.id]?.cardType == CardType.CLIMAX } ?: false
    }
    // 牌組主要作品（張數最多的），跨作品混搭時標示出來，一眼分辨這是哪副牌
    val titleName = remember(d.entries) { titleName(d.entries, cardRepo) }
    // 顏色比例條：跟圖鑑選片一樣，掃視就知道這副牌黃綠紅藍怎麼分配
    val colorCounts = remember(d.entries) {
        val counts = linkedMapOf<CardColor, Int>()
        for (entry in d.entries) {
            val color = cardRepo.snapshot.cardById[entry.printingId]?.color ?: continue
            counts[color] = (counts[color] ?: 0) + entry.count
        }
        counts
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().then(
            if (isActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
            else Modifier,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cover != null) {
                    PolicyGatedCardImage(
                        url = cover.imageURL,
                        contentDescription = null,
                        networkPolicy = networkPolicy,
                        modifier = Modifier
                            .width(if (isClimax) 74.dp else 52.dp)
                            .aspectRatio(if (isClimax) 88f / 63f else 63f / 88f)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                } else {
                    Box(
                        Modifier
                            .width(52.dp)
                            .aspectRatio(63f / 88f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Style, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.deck.name, style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        if (isActive) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "編輯中",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (titleName != null) {
                        Text(titleName, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Text(
                        "${d.totalCount}/50",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (d.totalCount == 50) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 常用牌組手動釘選到首頁，不是自動依使用頻率排序——「順手」由使用者自己決定
                IconButton(onClick = onTogglePin, modifier = pinAnchor) {
                    Icon(
                        if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (isPinned) "取消釘選首頁" else "釘選到首頁",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("重新命名") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text("複製牌組") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = { showMenu = false; onDuplicate() },
                        )
                        DropdownMenuItem(
                            text = { Text("刪除") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = { showMenu = false; onDelete() },
                        )
                    }
                }
            }
            if (colorCounts.isNotEmpty()) {
                DeckColorBar(colorCounts, total = 50, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/** 顏色比例條：底槽代表 50 張的滿額，填色的部分才是目前放的卡 */
@Composable
private fun DeckColorBar(counts: Map<CardColor, Int>, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        for (color in CardColor.entries) {
            val count = counts[color] ?: continue
            Box(
                Modifier
                    .weight(count.toFloat() / total)
                    .fillMaxHeight()
                    .background(deckColorSwatch(color)),
            )
        }
    }
}

private fun deckColorSwatch(color: CardColor): Color = when (color) {
    CardColor.YELLOW -> Color(0xFFFFC107)
    CardColor.GREEN -> Color(0xFF4CAF50)
    CardColor.RED -> Color(0xFFF44336)
    CardColor.BLUE -> Color(0xFF2196F3)
}

/** 牌組主要作品（張數最多的），對應 iOS DeckListView.titleName(for:) */
private fun titleName(entries: List<DeckEntryEntity>, cardRepo: CardRepository): String? {
    val counts = linkedMapOf<String, Int>()
    for (entry in entries) {
        val card = cardRepo.snapshot.cardById[entry.printingId] ?: continue
        val code = cardRepo.snapshot.titleByCardId[card.id] ?: continue
        counts[code] = (counts[code] ?: 0) + entry.count
    }
    if (counts.isEmpty()) return null
    val top = counts.entries.maxByOrNull { it.value }?.key ?: return null
    val name = cardRepo.snapshot.sets.firstOrNull { it.titleCode == top }?.titleNameZH ?: return null
    // 跨作品混搭時標示出來，免得以為只有一個系列
    return if (counts.size > 1) "$name 等 ${counts.size} 個作品" else name
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("確定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
