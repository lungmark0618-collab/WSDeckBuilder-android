package com.mark.wsdeck.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 把匯入來源（QR 掃出的載荷、貼上的文字、選的檔案）落地成一副牌組。邏輯比照
 * iOS 的 DeckImporter.createDeck：精確刷版優先，查不到刷版但認得出卡號的落到
 * 該卡普卡刷版，兩邊都查不到才算跳過。
 *
 * parse(text) 依序試 JSON 備份格式、純文字牌表（含【牌組名】跟張數標記）、
 * 最後才是沒有張數標記、同一張卡重複幾行代表幾張的純卡號清單——
 * 對應 iOS DeckImporter.parse 依序嘗試 parseJSON/parseText/parseRepeatedIDs 的順序。
 */
object DeckImporter {

    data class Result(
        val deckUuid: String,
        val deckName: String,
        val importedCards: Int,
        val matchedKinds: Int,
        val skipped: List<String>,
    )

    class NoCardsFoundException : Exception("內容裡找不到任何卡號。")

    class UnreadableTextException : Exception(
        "檔案格式無法辨識。請使用本 App 匯出的 JSON、牌表文字，或每行一張卡號的清單。",
    )

    private val bareIdLine = Regex("^[A-Za-z0-9]+/[A-Za-z0-9]+-[A-Za-z0-9]+\$")

    @Serializable
    private data class ImportedEntry(val printingID: String, val count: Int)

    @Serializable
    private data class ImportedDeck(val name: String, val note: String? = null, val entries: List<ImportedEntry>)

    private val json = Json { ignoreUnknownKeys = true }

    /** 依序嘗試 JSON 備份、純文字牌表、純卡號清單三種格式，都不是就報無法辨識 */
    fun parse(text: String): DeckImageExporter.Payload.Parsed =
        parseJson(text) ?: parseText(text) ?: runCatching { parseRepeatedIds(text) }.getOrNull()
            ?: throw UnreadableTextException()

    /** 本 App 匯出的 JSON 備份格式，跟 iOS 的 DeckExporter.json 輸出相容 */
    private fun parseJson(text: String): DeckImageExporter.Payload.Parsed? {
        val decoded = try { json.decodeFromString<ImportedDeck>(text) } catch (e: Exception) { return null }
        return DeckImageExporter.Payload.Parsed(decoded.name, decoded.entries.map { it.printingID to it.count })
    }

    // 張數與卡號之間容許常見寫法：「4 BRD/…」「4x BRD/…」「4. BRD/…」「4、BRD/…」
    private val namePattern = Regex("【([^】]+)】")
    private val entryPattern = Regex("""^\s*(?:缺)?(\d+)\s*[.、,xX×*]?\s*([A-Za-z0-9]+/[A-Za-z0-9]+-[A-Za-z0-9]+)""")
    private val reversePattern = Regex("""([A-Za-z0-9]+/[A-Za-z0-9]+-[A-Za-z0-9]+)\s*[xX×*]\s*(\d+)""")

    /** 純文字牌表：抓「張數 + 卡號」的行，順帶抓【】裡的牌組名，對應 iOS 的 parseText */
    private fun parseText(text: String): DeckImageExporter.Payload.Parsed? {
        var name = "匯入的牌組"
        var nameFound = false
        val entries = mutableListOf<Pair<String, Int>>()

        for (line in text.lines()) {
            if (!nameFound) {
                namePattern.find(line)?.let { match ->
                    name = match.groupValues[1].replace("收牌清單", "").trim()
                    nameFound = true
                }
            }
            val forward = entryPattern.find(line)
            if (forward != null) {
                val count = forward.groupValues[1].toIntOrNull()
                if (count != null) entries += forward.groupValues[2] to count
                continue
            }
            val reverse = reversePattern.find(line)
            if (reverse != null) {
                val count = reverse.groupValues[2].toIntOrNull()
                if (count != null) entries += reverse.groupValues[1] to count
            }
        }
        return if (entries.isEmpty()) null else DeckImageExporter.Payload.Parsed(name, entries)
    }

    /**
     * 貓罐子等工具匯出的純卡號清單：沒有張數標記，同一張卡有幾張就重複幾行。
     * 整份文字必須每一行都是卡號，混雜其他格式一律視為無法辨識。
     */
    fun parseRepeatedIds(text: String): DeckImageExporter.Payload.Parsed {
        val counts = linkedMapOf<String, Int>()
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (!bareIdLine.matches(line)) throw UnreadableTextException()
            counts[line] = (counts[line] ?: 0) + 1
        }
        if (counts.isEmpty()) throw UnreadableTextException()
        return DeckImageExporter.Payload.Parsed("匯入的牌組", counts.map { it.key to it.value })
    }

    suspend fun createDeck(
        parsed: DeckImageExporter.Payload.Parsed,
        cardRepo: CardRepository,
        deckRepo: DeckRepository,
        existingNames: List<String>,
    ): Result {
        val merged = linkedMapOf<String, Int>()
        val skipped = mutableListOf<String>()

        for ((printingId, count) in parsed.entries) {
            if (count <= 0) continue
            val card = cardRepo.snapshot.cardById[printingId]
            when {
                card != null && card.printings.any { it.id == printingId } ->
                    merged[printingId] = (merged[printingId] ?: 0) + count
                card != null ->
                    merged[card.defaultPrinting.id] = (merged[card.defaultPrinting.id] ?: 0) + count
                else -> skipped += printingId
            }
        }
        if (merged.isEmpty()) throw NoCardsFoundException()

        val name = uniqueName(parsed.name, existingNames)
        val uuid = deckRepo.createDeck(name)
        for ((printingId, count) in merged) {
            deckRepo.adjust(uuid, printingId, count, touch = false)
        }

        return Result(
            deckUuid = uuid,
            deckName = name,
            importedCards = merged.values.sum(),
            matchedKinds = merged.size,
            skipped = skipped.distinct().sorted(),
        )
    }

    /** 同名時加上「(2)」「(3)」，不覆蓋既有牌組 */
    fun uniqueName(name: String, existing: List<String>): String {
        val trimmed = name.trim().ifEmpty { "匯入的牌組" }
        if (trimmed !in existing) return trimmed
        var index = 2
        while ("$trimmed ($index)" in existing) index++
        return "$trimmed ($index)"
    }
}
