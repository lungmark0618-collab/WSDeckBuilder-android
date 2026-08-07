package com.mark.wsdeck.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 對應 iOS 的 DeckValidatorTests——同一套規則，用同樣的案例驗證。 */
class DeckValidatorTest {

    private fun card(id: String, name: String, type: CardType = CardType.CHARACTER) = Card(
        id = id,
        printings = listOf(Printing(id = id, rarity = "C", imageURL = "https://example.com/x.png")),
        nameJP = name,
        nameZH = name,
        cardType = type,
        color = CardColor.RED,
        level = if (type == CardType.CLIMAX) null else 0,
    )

    @Test
    fun `legal deck passes every rule`() {
        // 42 張角色（不同名，避開 4 張上限）+ 8 張 CX（兩種名各 4）
        val items = mutableListOf<CardCount>()
        for (index in 0 until 14) {
            items += CardCount(card("T/X01-0$index", "角色$index"), 3)
        }
        items += CardCount(card("T/X01-098", "CX甲", CardType.CLIMAX), 4)
        items += CardCount(card("T/X01-099", "CX乙", CardType.CLIMAX), 4)

        val result = DeckValidator.validate(items)
        assertEquals(50, result.totalCount)
        assertTrue(result.totalOK)
        assertTrue(result.climaxOK)
        assertTrue(result.namesOK)
        assertTrue(result.isLegal)
    }

    @Test
    fun `total count violation is flagged`() {
        val items = listOf(CardCount(card("T/X01-001", "A"), 4))
        val result = DeckValidator.validate(items)
        assertEquals(4, result.totalCount)
        assertFalse(result.totalOK)
        assertFalse(result.isLegal)
    }

    @Test
    fun `same name across different card ids still counts toward the 4-copy limit`() {
        val booster = card("T/X01-013", "蒼の魔女 シェラザード")
        val trial = card("T/X01-T13", "蒼の魔女 シェラザード")
        val items = listOf(CardCount(booster, 3), CardCount(trial, 2))
        val result = DeckValidator.validate(items)
        assertEquals(listOf("蒼の魔女 シェラザード"), result.overLimitNames)
        assertFalse(result.namesOK)
    }

    @Test
    fun `nameCount sums across printings`() {
        val a = card("T/X01-001", "同名")
        val b = card("T/X01-002", "同名")
        val items = listOf(CardCount(a, 2), CardCount(b, 2))
        assertEquals(4, DeckValidator.nameCount(a, items))
        assertTrue(DeckValidator.validate(items).namesOK)
    }

    @Test
    fun `climax limit is enforced separately from the total`() {
        val climax = card("T/X01-100", "CX甲", CardType.CLIMAX)
        val items = listOf(CardCount(climax, 4))
        val result = DeckValidator.validate(items)
        assertEquals(4, result.climaxCount)
        assertFalse(result.climaxOK)
    }

    @Test
    fun `mixed titles are detected by id prefix`() {
        val items = listOf(
            CardCount(card("BRD/W139-001", "A"), 4),
            CardCount(card("NIK/W67-001", "B"), 4),
        )
        assertTrue(DeckValidator.validate(items).mixedTitles)
    }
}
