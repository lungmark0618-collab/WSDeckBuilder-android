package com.mark.wsdeck.ui.deck

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.DeckImageImporter
import com.mark.wsdeck.data.DeckImporter
import com.mark.wsdeck.data.DeckRepository
import com.mark.wsdeck.data.DeckWithEntries
import com.mark.wsdeck.data.Prefs
import kotlinx.coroutines.launch

/** 牌組列表：建立、重新命名、刪除、掃圖匯入（對應 iOS 的 DeckListView） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(cardRepo: CardRepository, repo: DeckRepository, onOpen: (String) -> Unit) {
    val decks by repo.observeDecks().collectAsStateWithLifecycle(initialValue = emptyList())
    var showCreate by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DeckWithEntries?>(null) }
    var importResult by remember { mutableStateOf<DeckImporter.Result?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("牌組") }) },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showAddMenu = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "新增牌組")
                }
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("新增空牌組") },
                        leadingIcon = { Icon(Icons.Filled.PostAdd, contentDescription = null) },
                        onClick = { showAddMenu = false; showCreate = true },
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
                    DeckRow(d, onClick = { onOpen(d.deck.uuid) },
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
private fun DeckRow(d: DeckWithEntries, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
