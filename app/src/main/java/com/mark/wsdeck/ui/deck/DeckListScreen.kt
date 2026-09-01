package com.mark.wsdeck.ui.deck

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.CardType
import com.mark.wsdeck.data.DeckImageExporter
import com.mark.wsdeck.data.DeckImageImporter
import com.mark.wsdeck.data.DeckImporter
import com.mark.wsdeck.data.DeckRepository
import com.mark.wsdeck.data.DeckWithEntries
import com.mark.wsdeck.data.NetworkPolicy
import com.mark.wsdeck.data.OnboardingState
import com.mark.wsdeck.data.OnboardingStep
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
    onOpen: (String) -> Unit,
) {
    val decks by repo.observeDecks().collectAsStateWithLifecycle(initialValue = emptyList())
    var showCreate by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DeckWithEntries?>(null) }
    var importResult by remember { mutableStateOf<DeckImporter.Result?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showQRScanner by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

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
                val parsed = DeckImporter.parseRepeatedIds(text)
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
                items(decks, key = { it.deck.uuid }) { d ->
                    DeckRow(d, cardRepo, networkPolicy, onClick = { onOpen(d.deck.uuid) },
                        onDelete = { pendingDelete = d })
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

    pendingDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除「${d.deck.name}」？") },
            text = { Text("牌組內的卡片配置會一併刪除，無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.deleteDeck(d.deck.uuid) }
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
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    // 使用者指定的封面優先，否則自動取等級最高的一張（可在牌組詳情頁「選擇封面」調整）
    val cover = remember(d) { d.coverPrinting(cardRepo) }
    val isClimax = remember(cover) {
        cover?.let { p -> cardRepo.snapshot.cardById[p.id]?.cardType == CardType.CLIMAX } ?: false
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                Text(d.deck.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${d.totalCount}/50",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (d.totalCount == 50) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "刪除")
            }
        }
    }
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
