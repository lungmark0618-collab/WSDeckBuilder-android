package com.mark.wsdeck.data

/**
 * 把 QR 掃出的載荷落地成一副牌組。邏輯比照 iOS 的 DeckImporter.createDeck：
 * 精確刷版優先，查不到刷版但認得出卡號的落到該卡普卡刷版，兩邊都查不到才算跳過。
 *
 * iOS 那邊還多了 JSON／純文字牌表／檔案匯入三種來源，這裡先只服務 QR 掃圖——
 * 這是目前兩個平台唯一保證互通的格式，其他匯入管道留到之後有需要再補。
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

    class UnreadableTextException : Exception("檔案格式無法辨識，請確認每一行都是卡號。")

    private val bareIdLine = Regex("^[A-Za-z0-9]+/[A-Za-z0-9]+-[A-Za-z0-9]+\$")

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
