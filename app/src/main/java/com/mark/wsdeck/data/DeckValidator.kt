package com.mark.wsdeck.data

/**
 * 同名／合併同名卡的組牌上限索引（§4.4.3 的例外表），對應 iOS 的 NameLimitRules。
 * 多數卡同名上限是 4 張，但少數卡的能力文字明講「此卡同名可放 N 張」甚至
 * 「可放任意張」，也有少數兩張不同卡名的卡共用一個合計上限（通常是覺醒/
 * 變身關係的角色）。這份規則從 WSDeckBuilder-data 的 `deck_building_rules.json`
 * 抓回來，純資料結構，方便在測試裡手動建構特定規則。
 */
data class NameLimitRules(
    private val defaultLimit: Int,
    private val groupByName: Map<String, Group>,
) {
    data class Group(
        /** 共用同一個上限的卡名集合，沒有例外規則時就只有卡片自己 */
        val names: Set<String>,
        /** null = 無上限（可放任意張） */
        val limit: Int?,
    )

    companion object {
        /** 沒有任何例外規則時的預設值：人人都是同名 4 張上限，
         *  抓不到例外表或例外表還沒抓回來時用這個，不會把原本合法的牌組誤判 */
        val standard = NameLimitRules(defaultLimit = DeckValidator.NAME_LIMIT, groupByName = emptyMap())
    }

    /** 這個卡名所屬的分組與上限；沒有例外規則就回傳卡名自己＋預設上限 */
    fun group(nameJP: String): Group =
        groupByName[nameJP] ?: Group(names = setOf(nameJP), limit = defaultLimit)
}

/** WS Neo-Standard 建構規則檢查。純函式，對應 iOS 的 DeckValidator（§4.4.3）。 */
object DeckValidator {
    const val DECK_SIZE = 50
    const val CLIMAX_LIMIT = 8
    const val NAME_LIMIT = 4

    /** 組牌限制例外表，由 DeckBuildingRulesRepository 背景抓回來後更新。
     *  抓不到／還沒抓回來時是 [NameLimitRules.standard]（人人同名 4 張上限），
     *  這是安全的預設值，不會把原本合法的牌組誤判成違規 */
    var activeRules: NameLimitRules = NameLimitRules.standard

    data class Result(
        val totalCount: Int,
        val climaxCount: Int,
        /** 同名超過上限的卡名（或合併同名分組），依 nameJP 分組，跨刷版、跨卡號 */
        val overLimitNames: List<String>,
        /** 混入了不同作品的卡（Neo-Standard 牌組須同一作品） */
        val mixedTitles: Boolean,
    ) {
        val totalOK get() = totalCount == DECK_SIZE
        val climaxOK get() = climaxCount == CLIMAX_LIMIT
        val namesOK get() = overLimitNames.isEmpty()
        val isLegal get() = totalOK && climaxOK && namesOK && !mixedTitles
    }

    fun validate(items: List<CardCount>, rules: NameLimitRules = activeRules): Result {
        val total = items.sumOf { it.count }
        val climax = items.filter { it.card.cardType == CardType.CLIMAX }.sumOf { it.count }

        // ⚠ 三層概念：刷版不獨立計算；預設依「卡名」分組（因為存在不同基礎
        // 卡號但同名的卡，補充包與預組重複收錄），少數卡有例外規則——
        // 同名上限不是 4（見卡片能力文字），或跟另一個卡名合計算一組
        // （通常是覺醒/變身關係），都靠 rules.group() 解出正確分組
        val groups = mutableMapOf<Set<String>, Pair<Int?, Int>>()
        for (item in items) {
            val group = rules.group(item.card.nameJP)
            val (limit, total0) = groups[group.names] ?: (group.limit to 0)
            groups[group.names] = limit to (total0 + item.count)
        }
        val over = groups.entries
            .mapNotNull { (names, entry) ->
                val (limit, groupTotal) = entry
                if (limit != null && groupTotal > limit) names.sorted().joinToString("、") else null
            }
            .sorted()

        // 作品代號 = 卡號 `/` 前的字母（BRD、NIK、BD…）；同代號視為同作品
        val titlePrefixes = items.map { it.card.id.substringBefore("/") }.toSet()

        return Result(total, climax, over, titlePrefixes.size > 1)
    }

    /** 某一張卡（依卡名跨刷版）目前的張數，跟同名上限一起用來標紅 */
    fun nameCount(card: Card, items: List<CardCount>): Int =
        items.filter { it.card.nameJP == card.nameJP }.sumOf { it.count }

    /** 這張卡的同名上限（含合併同名分組）；null = 無上限，可放任意張 */
    fun nameLimit(card: Card, rules: NameLimitRules = activeRules): Int? =
        rules.group(card.nameJP).limit
}
