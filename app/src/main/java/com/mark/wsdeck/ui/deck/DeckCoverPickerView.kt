package com.mark.wsdeck.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mark.wsdeck.data.Card
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.CardType
import com.mark.wsdeck.data.DeckRepository
import com.mark.wsdeck.data.DeckWithEntries
import com.mark.wsdeck.data.NetworkPolicy
import com.mark.wsdeck.data.Printing
import com.mark.wsdeck.data.coverPrinting
import com.mark.wsdeck.ui.shared.PolicyGatedCardImage
import kotlinx.coroutines.launch

/**
 * 從牌組裡挑一張刷版當封面（對應 iOS 的 DeckCoverPickerView）；不選則沿用自動封面。
 * 用全螢幕 Dialog 而不是 ModalBottomSheet——挑封面要看清楚卡圖，半高的 sheet 太擠。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckCoverPickerView(
    deck: DeckWithEntries,
    cardRepo: CardRepository,
    deckRepo: DeckRepository,
    networkPolicy: NetworkPolicy,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    data class Tile(val card: Card, val printing: Printing, val count: Int)

    val tiles = remember(deck.entries) {
        deck.entries.filter { it.count > 0 }.mapNotNull { entry ->
            val card = cardRepo.snapshot.cardById[entry.printingId] ?: return@mapNotNull null
            val printing = card.printings.firstOrNull { it.id == entry.printingId } ?: return@mapNotNull null
            Tile(card, printing, entry.count)
        }.sortedWith(
            compareBy(
                { if (it.card.cardType == CardType.CLIMAX) 99 else (it.card.level ?: 0) },
                { it.printing.id },
            )
        )
    }

    val activePrintingId = remember(deck) { deck.coverPrinting(cardRepo)?.id }
    val isAuto = deck.deck.coverPrintingId.isEmpty()

    fun setCover(printingId: String?) {
        scope.launch { deckRepo.setCover(deck.deck, printingId ?: "") }
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("選擇封面") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "關閉")
                        }
                    },
                    actions = {
                        TextButton(onClick = { setCover(null) }, enabled = !isAuto) {
                            Text("自動選擇")
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (isAuto) Icons.Filled.AutoAwesome else Icons.Filled.Verified,
                        contentDescription = null,
                        tint = if (isAuto) MaterialTheme.colorScheme.onSurfaceVariant
                              else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isAuto) "目前為自動封面（牌組中等級最高的一張）" else "目前為手動指定的封面",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()

                if (tiles.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                        Text("牌組是空的，先加入卡片才能選擇封面",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    return@Column
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(tiles, key = { it.printing.id }) { tile ->
                        val isSelected = tile.printing.id == activePrintingId
                        Column(
                            Modifier.clickable { setCover(tile.printing.id) },
                        ) {
                            Box {
                                PolicyGatedCardImage(
                                    url = tile.printing.imageURL,
                                    contentDescription = tile.card.nameZH,
                                    networkPolicy = networkPolicy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(63f / 88f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .let {
                                            if (isSelected) {
                                                it.border(3.dp, MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(6.dp))
                                            } else it
                                        },
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "目前封面",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.White, RoundedCornerShape(50))
                                            .size(20.dp),
                                    )
                                }
                            }
                            Text(
                                tile.card.nameZH,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                tile.printing.rarity,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
