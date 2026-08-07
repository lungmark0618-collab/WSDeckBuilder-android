package com.mark.wsdeck.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QR 載荷格式是兩個平台唯一保證互通的介面，這裡鎖住格式不被意外改動。
 * 反過來能不能真的跟 iOS 出的圖互通，已經用一張實機出的 deck5.png 手動驗證過；
 * 這裡測的是格式本身的穩定性，不是那次手動驗證的替代品。
 */
class DeckImageExporterPayloadTest {

    @Test
    fun `round trips name and entries`() {
        val entries = listOf(
            DeckEntryEntity(deckUuid = "d", printingId = "BRD/W139-075", count = 2),
            DeckEntryEntity(deckUuid = "d", printingId = "BRD/W139-075S", count = 1),
        )
        val encoded = DeckImageExporter.Payload.encode("8門 棕色塵埃", entries)
        val decoded = DeckImageExporter.Payload.decode(encoded)

        assertEquals("8門 棕色塵埃", decoded?.name)
        assertEquals(
            listOf("BRD/W139-075" to 2, "BRD/W139-075S" to 1),
            decoded?.entries,
        )
    }

    @Test
    fun `deck names with the delimiter get sanitized so parsing still works`() {
        val entries = listOf(DeckEntryEntity(deckUuid = "d", printingId = "X-001", count = 1))
        val encoded = DeckImageExporter.Payload.encode("危險|牌組\n名稱", entries)
        val decoded = DeckImageExporter.Payload.decode(encoded)

        assertEquals("危險／牌組 名稱", decoded?.name)
        assertEquals(1, decoded?.entries?.size)
    }

    @Test
    fun `unrecognized text decodes to null`() {
        assertNull(DeckImageExporter.Payload.decode("not a deck payload"))
        assertNull(DeckImageExporter.Payload.decode("WSD1|missing body"))
    }

    @Test
    fun `entries are sorted by printing id for a stable encoding`() {
        val entries = listOf(
            DeckEntryEntity(deckUuid = "d", printingId = "Z-002", count = 1),
            DeckEntryEntity(deckUuid = "d", printingId = "A-001", count = 1),
        )
        val encoded = DeckImageExporter.Payload.encode("順序", entries)
        assertTrue(encoded.substringAfterLast("|").startsWith("A-001"))
    }
}
