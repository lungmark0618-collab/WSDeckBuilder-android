package com.mark.wsdeck.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

/** Room 存取的薄包裝，把「調整張數、歸零即刪」這類規則放在同一個地方 */
class DeckRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext, AppDatabase::class.java, "wsdeck.db",
    ).fallbackToDestructiveMigration(true).build()
    private val dao = db.deckDao()

    fun observeDecks(): Flow<List<DeckWithEntries>> = dao.observeDecks()
    fun observeDeck(uuid: String): Flow<DeckWithEntries?> = dao.observeDeck(uuid)

    suspend fun createDeck(name: String): String {
        val deck = DeckEntity(name = name)
        dao.insertDeck(deck)
        return deck.uuid
    }

    suspend fun deleteDeck(uuid: String) = dao.deleteDeck(uuid)

    suspend fun renameDeck(deck: DeckEntity, name: String) {
        dao.updateDeck(deck.copy(name = name, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setCover(deck: DeckEntity, printingId: String) {
        dao.updateDeck(deck.copy(coverPrintingId = printingId,
                                 updatedAt = System.currentTimeMillis()))
    }

    /**
     * 調整某刷版張數；歸零自動移除該筆（對應 iOS Deck.adjust，§4.4.2）。
     * touch=true 才更新 updatedAt——出圖、統計這類只讀操作不該讓「最近修改」跳動。
     */
    suspend fun adjust(deckUuid: String, printingId: String, delta: Int, touch: Boolean = true) {
        val existing = dao.findEntry(deckUuid, printingId)
        if (existing != null) {
            val newCount = existing.count + delta
            if (newCount <= 0) dao.deleteEntry(existing.id)
            else dao.updateEntry(existing.copy(count = newCount))
        } else if (delta > 0) {
            dao.insertEntry(DeckEntryEntity(deckUuid = deckUuid, printingId = printingId, count = delta))
        }
        if (touch) touchDeck(deckUuid)
    }

    /** 轉換刷版：把 1 張 from 換成 to（長按選單用） */
    suspend fun convert(deckUuid: String, from: String, to: String) {
        val source = dao.findEntry(deckUuid, from) ?: return
        if (source.count <= 0) return
        adjust(deckUuid, from, -1, touch = false)
        adjust(deckUuid, to, 1, touch = false)
        touchDeck(deckUuid)
    }

    private suspend fun touchDeck(deckUuid: String) {
        dao.touch(deckUuid, System.currentTimeMillis())
    }
}

/** 邏輯卡片 + 牌組中的總張數（跨刷版），對應 iOS 的 DeckExporter.CardCount */
data class CardCount(val card: Card, val count: Int)

/** 把 entries 依卡片分組加總，供牌表顯示、統計、匯出共用 */
fun groupByCard(entries: List<DeckEntryEntity>, repo: CardRepository): List<CardCount> {
    val byCard = LinkedHashMap<String, Int>()
    val cardOf = mutableMapOf<String, Card>()
    for (entry in entries) {
        val card = repo.snapshot.cardById[entry.printingId] ?: continue
        byCard[card.id] = (byCard[card.id] ?: 0) + entry.count
        cardOf[card.id] = card
    }
    return byCard.mapNotNull { (id, count) -> cardOf[id]?.let { CardCount(it, count) } }
}

/**
 * 封面刷版：使用者指定優先，否則取等級最高（其次張數多）的一張，
 * 對應 iOS 的 Deck.coverPrinting(database:)。
 */
fun DeckWithEntries.coverPrinting(repo: CardRepository): Printing? {
    if (deck.coverPrintingId.isNotEmpty()) {
        repo.snapshot.cardById[deck.coverPrintingId]
            ?.printings?.firstOrNull { it.id == deck.coverPrintingId }
            ?.let { return it }
    }
    return entries
        .mapNotNull { entry ->
            val card = repo.snapshot.cardById[entry.printingId] ?: return@mapNotNull null
            val printing = card.printings.firstOrNull { it.id == entry.printingId } ?: return@mapNotNull null
            Triple(printing, card.level ?: -1, entry.count)
        }
        .maxWithOrNull(compareBy({ it.second }, { it.third }))
        ?.first
}
