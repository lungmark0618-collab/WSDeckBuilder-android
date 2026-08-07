package com.mark.wsdeck.data

/** 收藏查詢與缺卡計算的純函式，對應 iOS CollectionStore 裡不碰 ModelContext 的那半。 */
object CollectionStore {

    /** 由 observeAll() 拿到的清單建索引 */
    fun index(entries: List<CollectionEntryEntity>): Map<String, Int> =
        entries.filter { it.ownedCount > 0 }.associate { it.printingId to it.ownedCount }

    /** 某張卡跨刷版的擁有總數 */
    fun owned(card: Card, index: Map<String, Int>): Int =
        card.printings.sumOf { index[it.id] ?: 0 }

    /** 牌組缺卡：某個刷版還差幾張 */
    data class Shortage(
        val printing: Printing,
        val card: Card,
        val needed: Int,
        val owned: Int,
    ) {
        val missing: Int get() = maxOf(0, needed - owned)
    }

    /**
     * 牌組內每個刷版的收藏進度（含已收齊的），只算牌組裡實際放的刷版。
     * 吃 cardById（來自 CardRepository.snapshot）而不是整個 CardRepository，
     * 這樣才是真的純函式——不用真的 Context 也能測。
     */
    fun tracked(entries: List<DeckEntryEntity>, cardById: Map<String, Card>, index: Map<String, Int>): List<Shortage> =
        entries.mapNotNull { entry ->
            val card = cardById[entry.printingId] ?: return@mapNotNull null
            val printing = card.printings.firstOrNull { it.id == entry.printingId } ?: return@mapNotNull null
            Shortage(printing, card, entry.count, index[entry.printingId] ?: 0)
        }.sortedBy { it.printing.id }

    /** 只看還缺的（tracked 的子集） */
    fun shortages(entries: List<DeckEntryEntity>, cardById: Map<String, Card>, index: Map<String, Int>): List<Shortage> =
        tracked(entries, cardById, index).filter { it.missing > 0 }

    /** 缺卡清單文字（去卡店對照用） */
    fun shortageText(deckName: String, shortages: List<Shortage>): String {
        if (shortages.isEmpty()) return "【$deckName】缺卡清單\n（沒有缺卡，全部齊了）"
        val lines = mutableListOf("【$deckName】缺卡清單")
        for (item in shortages) {
            val paddedId = item.printing.id.padEnd(16)
            val paddedRarity = item.printing.rarity.padEnd(4)
            val extra = if (item.owned > 0) "（已有${item.owned}/${item.needed}）" else ""
            lines += "缺${item.missing}  $paddedId $paddedRarity ${item.card.nameZH}$extra"
        }
        lines += "—— 合計缺 ${shortages.sumOf { it.missing }} 張"
        return lines.joinToString("\n")
    }
}
