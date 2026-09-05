package com.mark.wsdeck.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

/**
 * 匯出純文字／JSON，對應 iOS 的 DeckExporter。iOS 這邊除了出圖跟缺卡清單，
 * 還有簡潔版牌表、收牌清單（含刷版）、JSON 備份三種格式，之前 Android
 * 只做了出圖跟缺卡清單，這裡補齊落差。
 */
object DeckExporter {

    /** 簡潔版：貼論壇用，不分刷版 */
    fun simpleText(deckName: String, entries: List<DeckEntryEntity>, cardRepo: CardRepository): String {
        val grouped = groupByCard(entries, cardRepo)
        val lines = mutableListOf("【$deckName】")
        for ((levelLabel, items) in byLevel(grouped)) {
            val count = items.sumOf { it.count }
            lines += "$levelLabel ($count)"
            for (item in items) {
                lines += "${item.count}  ${item.card.id}  ${item.card.nameZH}"
            }
        }
        return lines.joinToString("\n")
    }

    /** 收牌版：列出刷版，去卡店對照用 */
    fun collectorText(deckName: String, entries: List<DeckEntryEntity>, cardRepo: CardRepository): String {
        val lines = mutableListOf("【$deckName】收牌清單")
        for (entry in entries.sortedBy { it.printingId }) {
            val card = cardRepo.snapshot.cardById[entry.printingId] ?: continue
            val printing = card.printings.firstOrNull { it.id == entry.printingId } ?: continue
            val paddedId = entry.printingId.padEnd(16)
            val paddedRarity = printing.rarity.padEnd(4)
            lines += "${entry.count}  $paddedId $paddedRarity ${card.nameZH}"
        }
        return lines.joinToString("\n")
    }

    @Serializable
    private data class ExportEntry(val printingID: String, val count: Int)

    @Serializable
    private data class ExportDeck(
        val name: String,
        val note: String,
        val exportedAt: String,
        val entries: List<ExportEntry>,
    )

    private val json = Json { prettyPrint = true }

    /** 完整 JSON（含刷版），供備份與跨機器搬移；跟 iOS 匯出的格式相容，兩邊都能互相匯入 */
    fun json(deckName: String, note: String, entries: List<DeckEntryEntity>): String {
        val export = ExportDeck(
            name = deckName,
            note = note,
            exportedAt = Instant.now().toString(),
            entries = entries.sortedBy { it.printingId }
                .map { ExportEntry(it.printingId, it.count) },
        )
        return json.encodeToString(export)
    }

    /** 寫成暫存 .json 檔並回傳位置，讓分享出去的是真正的 JSON 檔（可再匯入） */
    fun jsonFile(context: Context, deckName: String, jsonText: String): File? {
        val safeName = deckName.replace(Regex("[/\\\\:?%*|\"<>]"), "_").ifEmpty { "deck" }
        val file = File(context.cacheDir, "$safeName.json")
        return try {
            file.writeText(jsonText)
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun byLevel(items: List<CardCount>): List<Pair<String, List<CardCount>>> {
        val result = mutableListOf<Pair<String, List<CardCount>>>()
        for (level in 0..3) {
            val matched = items.filter { it.card.level == level && it.card.cardType != CardType.CLIMAX }
            if (matched.isNotEmpty()) result += "Lv$level" to matched
        }
        val climax = items.filter { it.card.cardType == CardType.CLIMAX }
        if (climax.isNotEmpty()) result += "CX" to climax
        return result
    }
}
