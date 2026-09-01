package com.mark.wsdeck.ui.deck

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.DeckImageExporter
import com.mark.wsdeck.data.DeckImporter
import com.mark.wsdeck.data.DeckRepository
import com.mark.wsdeck.data.NetworkPolicy
import com.mark.wsdeck.data.Prefs
import com.mark.wsdeck.ui.shared.PolicyGatedCardImage
import kotlinx.coroutines.launch

/**
 * 掃朋友分享的牌組 QR（系統相機喚起這個 App）之後的預覽畫面，對應 iOS 的
 * DeckImportPreviewSheet：列出牌組內容，使用者按「加入牌組」才真的寫進
 * 資料庫，按「取消」就當作沒發生過。
 */
@Composable
fun DeckImportPreviewDialog(
    parsed: DeckImageExporter.Payload.Parsed,
    cardRepo: CardRepository,
    deckRepo: DeckRepository,
    networkPolicy: NetworkPolicy,
    onDismiss: () -> Unit,
) {
    val decks by deckRepo.observeDecks().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var importError by remember { mutableStateOf<String?>(null) }
    val totalCount = parsed.entries.sumOf { it.second }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("朋友分享的牌組") },
        text = {
            Column {
                Text("${parsed.name}（共 $totalCount 張）", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(parsed.entries) { (printingId, count) ->
                        val card = cardRepo.snapshot.cardById[printingId]
                        val printing = card?.printings?.firstOrNull { it.id == printingId } ?: card?.defaultPrinting
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (printing != null) {
                                    PolicyGatedCardImage(
                                        url = printing.imageURL,
                                        contentDescription = card?.nameZH,
                                        networkPolicy = networkPolicy,
                                        modifier = Modifier.size(width = 32.dp, height = 45.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    card?.nameZH ?: printingId,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text("×$count", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                importError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    try {
                        val result = DeckImporter.createDeck(
                            parsed, cardRepo, deckRepo, decks.map { it.deck.name },
                        )
                        prefs.activeDeckUuid = result.deckUuid
                        onDismiss()
                    } catch (e: Exception) {
                        importError = e.message ?: e.toString()
                    }
                }
            }) { Text("加入牌組") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
