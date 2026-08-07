package com.mark.wsdeck.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** 對應 iOS DeckImporterTests 的 testUniqueNameAvoidsOverwrite——同一套避免覆蓋規則。 */
class DeckImporterTest {

    @Test
    fun `no collision keeps the original name`() {
        assertEquals("我的牌", DeckImporter.uniqueName("我的牌", emptyList()))
    }

    @Test
    fun `first collision appends 2`() {
        assertEquals("我的牌 (2)", DeckImporter.uniqueName("我的牌", listOf("我的牌")))
    }

    @Test
    fun `keeps incrementing past existing suffixes`() {
        assertEquals(
            "我的牌 (3)",
            DeckImporter.uniqueName("我的牌", listOf("我的牌", "我的牌 (2)")),
        )
    }

    @Test
    fun `blank name falls back to a default`() {
        assertEquals("匯入的牌組", DeckImporter.uniqueName("  ", emptyList()))
    }
}
