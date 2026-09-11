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

    // MARK: - 組牌限制例外表（同名可超過4張、複數卡名合計限制）

    @Test
    fun `same name exception allows a fixed limit above four`() {
        val c = card("OVL/S99-047", "黒い仔山羊")
        val rules = NameLimitRules(
            defaultLimit = 4,
            groupByName = mapOf("黒い仔山羊" to NameLimitRules.Group(setOf("黒い仔山羊"), 5)),
        )
        val result = DeckValidator.validate(listOf(CardCount(c, 5)), rules)
        assertTrue(result.namesOK)
        assertEquals(5, DeckValidator.nameLimit(c, rules))
    }

    @Test
    fun `same name exception allows unlimited copies`() {
        val c = card("OVL/S99-090", "ゴブリン軍楽隊")
        val rules = NameLimitRules(
            defaultLimit = 4,
            groupByName = mapOf("ゴブリン軍楽隊" to NameLimitRules.Group(setOf("ゴブリン軍楽隊"), null)),
        )
        val result = DeckValidator.validate(listOf(CardCount(c, 20)), rules)
        assertTrue(result.namesOK)
        assertEquals(null, DeckValidator.nameLimit(c, rules))
    }

    @Test
    fun `combined name limit pools counts across different names`() {
        val base = card("PJS/S109-114", "えむ流？ダンスの極意！ 小豆沢こはね")
        val awakened = card("PJS/S109-999", "Beat Eater/Awake Now")
        val group = NameLimitRules.Group(setOf(base.nameJP, awakened.nameJP), 4)
        val rules = NameLimitRules(
            defaultLimit = 4,
            groupByName = mapOf(base.nameJP to group, awakened.nameJP to group),
        )
        val withinLimit = listOf(CardCount(base, 2), CardCount(awakened, 2))
        assertTrue(DeckValidator.validate(withinLimit, rules).namesOK)

        val overLimit = listOf(CardCount(base, 3), CardCount(awakened, 2))
        assertFalse(DeckValidator.validate(overLimit, rules).namesOK)
    }

    @Test
    fun `name limit defaults to standard without exception`() {
        val c = card("T/X01-001", "普通卡")
        assertEquals(4, DeckValidator.nameLimit(c, NameLimitRules.standard))
    }
}
