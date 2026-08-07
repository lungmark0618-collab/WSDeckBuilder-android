package com.mark.wsdeck.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 對應 iOS DataUpdater.pending(in:base:database:) 的純函式版本。 */
class DataUpdaterTest {

    private val manifestUrl = "https://raw.githubusercontent.com/lungmark0618-collab/WSDeckBuilder-data/main/manifest.json"

    private fun localSet(titleCode: String, version: Int) = CardSetMeta(
        titleCode = titleCode,
        titleNameJP = titleCode,
        titleNameZH = "測試作品",
        cardCount = 10,
        dataVersion = version,
    )

    private fun entry(titleCode: String, version: Int, file: String = "x_cards.json", url: String = "x_cards.json") =
        UpdateManifest.SetEntry(titleCode = titleCode, file = file, dataVersion = version, url = url)

    @Test
    fun `a title with no local match is treated as version 0 and is always pending`() {
        val manifest = UpdateManifest(schemaVersion = 1, sets = listOf(entry("NEW/W1", 1)))
        val pending = computePending(manifest, manifestUrl, emptyList())
        assertEquals(1, pending.size)
        assertEquals(0, pending[0].fromVersion)
        assertEquals(1, pending[0].toVersion)
        assertEquals("NEW/W1", pending[0].titleName) // 沒有本地資料，退回顯示代號
    }

    @Test
    fun `strictly greater remote version is pending`() {
        val manifest = UpdateManifest(schemaVersion = 1, sets = listOf(entry("BRD/W139", 3)))
        val pending = computePending(manifest, manifestUrl, listOf(localSet("BRD/W139", 2)))
        assertEquals(1, pending.size)
        assertEquals(2, pending[0].fromVersion)
        assertEquals(3, pending[0].toVersion)
        assertEquals("測試作品", pending[0].titleName) // 有本地資料，用中文名
    }

    @Test
    fun `equal or lower remote version is not pending`() {
        val manifest = UpdateManifest(
            schemaVersion = 1,
            sets = listOf(entry("BRD/W139", 2), entry("NIK", 1)),
        )
        val pending = computePending(
            manifest, manifestUrl,
            listOf(localSet("BRD/W139", 2), localSet("NIK", 5)),
        )
        assertTrue(pending.isEmpty())
    }

    @Test
    fun `entries whose file isn't a cards json are skipped`() {
        val manifest = UpdateManifest(schemaVersion = 1, sets = listOf(entry("X", 1, file = "readme.txt")))
        assertTrue(computePending(manifest, manifestUrl, emptyList()).isEmpty())
    }

    @Test
    fun `relative urls resolve against the manifest url`() {
        val manifest = UpdateManifest(schemaVersion = 1, sets = listOf(entry("X", 1, url = "x_cards.json")))
        val pending = computePending(manifest, manifestUrl, emptyList())
        assertEquals(
            "https://raw.githubusercontent.com/lungmark0618-collab/WSDeckBuilder-data/main/x_cards.json",
            pending[0].url,
        )
    }

    @Test
    fun `absolute urls in the manifest are kept as-is`() {
        val absolute = "https://cdn.example.com/x_cards.json"
        val manifest = UpdateManifest(schemaVersion = 1, sets = listOf(entry("X", 1, url = absolute)))
        val pending = computePending(manifest, manifestUrl, emptyList())
        assertEquals(absolute, pending[0].url)
    }
}
