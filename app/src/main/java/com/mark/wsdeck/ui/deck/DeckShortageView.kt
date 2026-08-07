package com.mark.wsdeck.ui.deck

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mark.wsdeck.data.CardType
import com.mark.wsdeck.data.CollectionStore

/**
 * 缺卡清單：對照「我的收藏」算出牌組還缺哪些刷版（對應 iOS DeckDetailView 的
 * shortageList）。用一顆「收齊」按鈕取代 iOS 的滑動手勢——Compose 裡滑動列項
 * 要另外接 SwipeToDismissBox，這個場景按鈕更直接。
 */
@Composable
fun DeckShortageTab(
    items: List<CollectionStore.Shortage>,
    showCollected: Boolean,
    onToggleShowCollected: () -> Unit,
    onAdjust: (printingId: String, delta: Int) -> Unit,
    onFill: (CollectionStore.Shortage) -> Unit,
    onFillAll: () -> Unit,
) {
    val shortagesOnly = remember(items) { items.filter { it.missing > 0 } }
    val visible = if (showCollected) items else shortagesOnly
    val totalMissing = remember(shortagesOnly) { shortagesOnly.sumOf { it.missing } }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (totalMissing > 0) "還缺 $totalMissing 張（共 ${shortagesOnly.size} 種）" else "都收齊了",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleShowCollected) {
                Text(if (showCollected) "只看缺的" else "顯示全部")
            }
            if (shortagesOnly.isNotEmpty()) {
                TextButton(onClick = onFillAll) { Text("全部收齊") }
            }
        }
        HorizontalDivider()

        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                Text("這副牌都收齊了，牌組內每張卡的擁有數都足夠",
                    style = MaterialTheme.typography.bodyMedium)
            }
            return
        }

        LazyColumn {
            items(visible, key = { it.printing.id }) { item ->
                ShortageRow(item, onAdjust = onAdjust, onFill = { onFill(item) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ShortageRow(
    item: CollectionStore.Shortage,
    onAdjust: (printingId: String, delta: Int) -> Unit,
    onFill: () -> Unit,
) {
    val done = item.missing == 0
    val isClimax = item.card.cardType == CardType.CLIMAX

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.printing.imageURL,
            contentDescription = item.card.nameZH,
            modifier = Modifier
                .width(if (isClimax) 52.dp else 36.dp)
                .aspectRatio(if (isClimax) 88f / 63f else 63f / 88f)
                .clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.card.nameZH,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                "${item.printing.rarity}  ${item.printing.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (done) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("已收齊", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50))
                }
            } else {
                Text("還缺 ${item.missing}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CountStepper(item.owned) { delta -> onAdjust(item.printing.id, delta) }
            Text(
                "${item.owned}/${item.needed}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!done) {
            TextButton(onClick = onFill) { Text("收齊") }
        }
    }
}
