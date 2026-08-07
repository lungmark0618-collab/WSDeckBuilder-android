package com.mark.wsdeck.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 對應 iOS 的 CollectionStore（純函式部分）——收藏彙總與缺卡計算。 */
class CollectionStoreTest {

    private fun card(id: String, printingIds: List<String> = listOf(id)) = Card(
        id = id,
        printings = printingIds.map { Printing(id = it, rarity = "C", imageURL = "https://example.com/x.png") },
        nameJP = id,
        nameZH = id,
        cardType = CardType.CHARACTER,
        color = CardColor.RED,
        level = 0,
    )

    @Test
    fun `index only keeps entries with a positive count`() {
        val entries = listOf(
            CollectionEntryEntity(printingId = "A", ownedCount = 2),
            CollectionEntryEntity(printingId = "B", ownedCount = 0),
        )
        assertEquals(mapOf("A" to 2), CollectionStore.index(entries))
    }

    @Test
    fun `owned sums across a card's printings`() {
        val c = card("X-001", listOf("X-001", "X-001S"))
        val index = mapOf("X-001" to 2, "X-001S" to 1)
        assertEquals(3, CollectionStore.owned(c, index))
    }

    @Test
    fun `shortages only include entries still missing copies`() {
        val entries = listOf(
            DeckEntryEntity(deckUuid = "d", printingId = "A-001", count = 4),
            DeckEntryEntity(deckUuid = "d", printingId = "B-001", count = 2),
        )
        val cardById = mapOf(
            "A-001" to card("A-001"),
            "B-001" to card("B-001"),
        )
        val index = mapOf("A-001" to 1, "B-001" to 2)

        val shortages = CollectionStore.shortages(entries, cardById, index)
        assertEquals(1, shortages.size)
        assertEquals("A-001", shortages[0].printing.id)
        assertEquals(3, shortages[0].missing)
    }

    @Test
    fun `tracked includes fully collected entries too`() {
        val entries = listOf(DeckEntryEntity(deckUuid = "d", printingId = "A-001", count = 2))
        val cardById = mapOf("A-001" to card("A-001"))
        val index = mapOf("A-001" to 2)

        val tracked = CollectionStore.tracked(entries, cardById, index)
        assertEquals(1, tracked.size)
        assertEquals(0, tracked[0].missing)
    }

    @Test
    fun `shortageText reports a clean deck when nothing is missing`() {
        assertEquals(
            "【我的牌組】缺卡清單\n（沒有缺卡，全部齊了）",
            CollectionStore.shortageText("我的牌組", emptyList()),
        )
    }

    @Test
    fun `shortageText lists each missing printing with a running total`() {
        val c = card("A-001")
        val shortages = listOf(CollectionStore.Shortage(c.printings[0], c, needed = 4, owned = 1))
        val text = CollectionStore.shortageText("我的牌組", shortages)
        assertTrue(text.contains("缺3"))
        assertTrue(text.contains("已有1/4"))
        assertTrue(text.contains("合計缺 3 張"))
    }
}
