package com.mark.wsdeck.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mark.wsdeck.data.CardColor
import com.mark.wsdeck.data.CardCount
import com.mark.wsdeck.data.CardType
import com.mark.wsdeck.data.TriggerIcon

/**
 * 統計檢視：等級曲線、顏色分布、判定標誌分布。對應 iOS 的 DeckStatsView，
 * 用 Compose 原生元件畫比例長條——沒有 Swift Charts 對應的套件，
 * 為了不多引入一個繪圖函式庫，這裡改用等寬長條取代圓環/長條圖表。
 */
@Composable
fun DeckStatsView(items: List<CardCount>) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
            Text("尚無資料", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SummaryRow(items)
        Section("等級曲線") { LevelBars(items) }
        Section("顏色分布") { ColorBars(items) }
        Section("判定標誌") { TriggerBars(items) }
    }
}

@Composable
private fun SummaryRow(items: List<CardCount>) {
    val nonClimax = items.filter { it.card.cardType != CardType.CLIMAX }
    val totalNonClimax = nonClimax.sumOf { it.count }
    val totalCost = nonClimax.sumOf { (it.card.cost ?: 0) * it.count }
    // 「總魂刻數」數的是卡牌右上角印有魂刻判定圖示（trigger 為魂／雙魂）的張數，
    // 不是 card.soul 那個數值欄位——那是攻擊力旁邊的魂傷，跟右上角的判定圖示是兩回事
    val soulCount = items.filter { it.card.trigger == TriggerIcon.SOUL || it.card.trigger == TriggerIcon.SOUL2 }
        .sumOf { it.count }
    val avgCost = if (totalNonClimax > 0) "%.2f".format(totalCost.toDouble() / totalNonClimax) else "-"
    val avgPower = if (totalNonClimax > 0) {
        (nonClimax.sumOf { (it.card.power ?: 0) * it.count } / totalNonClimax).toString()
    } else "-"

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("總張數", "${items.sumOf { it.count }}", Modifier.weight(1f))
        StatTile("平均費用", avgCost, Modifier.weight(1f))
        StatTile("平均攻擊力", avgPower, Modifier.weight(1f))
        StatTile("總魂刻數", "$soulCount", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(title, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(14.dp),
        ) { content() }
    }
}

@Composable
private fun LevelBars(items: List<CardCount>) {
    val buckets = (0..3).map { level ->
        level to items.filter { it.card.level == level && it.card.cardType != CardType.CLIMAX }
            .sumOf { it.count }
    }
    val peak = (buckets.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        buckets.forEach { (level, count) ->
            ProportionalBar("Lv$level", count, peak, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ColorBars(items: List<CardCount>) {
    val counts = CardColor.entries.map { color ->
        color to items.filter { it.card.color == color }.sumOf { it.count }
    }.filter { it.second > 0 }
    val peak = (counts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        counts.forEach { (color, count) ->
            ProportionalBar(color.label, count, peak, swiftColor(color))
        }
    }
}

@Composable
private fun TriggerBars(items: List<CardCount>) {
    val counts = TriggerIcon.entries.map { trigger ->
        trigger to items.filter { it.card.trigger == trigger }.sumOf { it.count }
    }.filter { it.second > 0 }
    if (counts.isEmpty()) {
        Text("牌組中沒有帶判定標誌的卡", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val peak = (counts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        counts.forEach { (trigger, count) ->
            ProportionalBar(trigger.label, count, peak, MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun ProportionalBar(label: String, count: Int, peak: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(48.dp))
        Box(
            Modifier
                .weight(1f)
                .height(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(count.toFloat() / peak)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Text(
            "$count", style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

private fun swiftColor(color: CardColor): Color = when (color) {
    CardColor.YELLOW -> Color(0xFFFFC107)
    CardColor.GREEN -> Color(0xFF4CAF50)
    CardColor.RED -> Color(0xFFF44336)
    CardColor.BLUE -> Color(0xFF2196F3)
}
