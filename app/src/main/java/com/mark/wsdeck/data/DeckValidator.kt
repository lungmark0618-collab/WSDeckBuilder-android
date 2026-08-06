package com.mark.wsdeck.data

/** WS Neo-Standard 建構規則檢查。純函式，對應 iOS 的 DeckValidator（§4.4.3）。 */
object DeckValidator {
    const val DECK_SIZE = 50
    const val CLIMAX_LIMIT = 8
    const val NAME_LIMIT = 4

    data class Result(
        val totalCount: Int,
        val climaxCount: Int,
        /** 同名超過 4 張的卡名（依 nameJP 分組，跨刷版、跨卡號） */
        val overLimitNames: List<String>,
        /** 混入了不同作品的卡（Neo-Standard 牌組須同一作品） */
        val mixedTitles: Boolean,
    ) {
        val totalOK get() = totalCount == DECK_SIZE
        val climaxOK get() = climaxCount == CLIMAX_LIMIT
        val namesOK get() = overLimitNames.isEmpty()
        val isLegal get() = totalOK && climaxOK && namesOK && !mixedTitles
    }

    fun validate(items: List<CardCount>): Result {
        val total = items.sumOf { it.count }
        val climax = items.filter { it.card.cardType == CardType.CLIMAX }.sumOf { it.count }

        // ⚠ 三層概念：刷版不獨立計算；4 張上限依「卡名」分組，
        // 因為存在不同基礎卡號但同名的卡（補充包與預組重複收錄）
        val byName = mutableMapOf<String, Int>()
        for (item in items) byName[item.card.nameJP] = (byName[item.card.nameJP] ?: 0) + item.count
        val over = byName.filter { it.value > NAME_LIMIT }.keys.sorted()

        // 作品代號 = 卡號 `/` 前的字母（BRD、NIK、BD…）；同代號視為同作品
        val titlePrefixes = items.map { it.card.id.substringBefore("/") }.toSet()

        return Result(total, climax, over, titlePrefixes.size > 1)
    }

    /** 某一張卡（依卡名跨刷版）目前是否已達上限 */
    fun nameCount(card: Card, items: List<CardCount>): Int =
        items.filter { it.card.nameJP == card.nameJP }.sumOf { it.count }
}
