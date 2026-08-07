package com.mark.wsdeck.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 對應 iOS 的 CardSearchTests——同一套案例，用 Robolectric 讀真正的卡表資產
 * 而不是模擬資料，跟 iOS 那邊「測試 target 以 app 為 host，可直接讀 app bundle
 * 內的卡表」是同樣的精神。
 *
 * 卡表不在這個 repo 裡（著作權考量），CI 跑之前得先用
 * `tools/fetch_published_cards.py --out app/src/main/assets` 抓一份，
 * 本機開發也一樣——沒有卡表這些測試會直接失敗，而不是被跳過。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CardRepositoryTest {

    private lateinit var repo: CardRepository

    @Before
    fun setUp() = runBlocking {
        repo = CardRepository(ApplicationProvider.getApplicationContext())
        val loaded = repo.load()
        check(loaded) { "卡表載入失敗：${repo.loadError}——先跑 tools/fetch_published_cards.py" }
    }

    @Test
    fun `data loads without error`() {
        assertEquals(null, repo.loadError)
        assertFalse(repo.snapshot.cards.isEmpty())
        assertTrue(repo.snapshot.sets.any { it.titleCode == "BRD/W139" })
    }

    @Test
    fun `title filter returns exactly that title's cards`() {
        val meta = repo.snapshot.sets.first { it.titleCode == "BRD/W139" }
        val results = repo.search(SearchQuery(titleCode = "BRD/W139"))
        assertEquals(meta.cardCount, results.size)
        assertTrue(results.all { it.id.startsWith("BRD/") })
    }

    @Test
    fun `every card has a Chinese name`() {
        for (card in repo.snapshot.cards) {
            assertTrue("${card.id} 缺中文卡名", card.nameZH.isNotEmpty())
        }
    }

    @Test
    fun `printing index covers every printing`() {
        for (card in repo.snapshot.cards) {
            for (printing in card.printings) {
                assertEquals(card.id, repo.snapshot.cardById[printing.id]?.id)
            }
        }
    }

    @Test
    fun `card number search ignores case and slashes`() {
        // 忽略大小寫與斜線：輸入 w139075 也能找到 BRD/W139-075
        val results = repo.search(SearchQuery(keyword = "w139075"))
        assertTrue(results.map { it.id }.toString(), results.any { it.id == "BRD/W139-075" })
    }

    @Test
    fun `level and color filters combine`() {
        val results = repo.search(SearchQuery(levels = setOf(0), colors = setOf(CardColor.YELLOW)))
        assertFalse(results.isEmpty())
        assertTrue(results.all { it.level == 0 && it.color == CardColor.YELLOW })
    }

    @Test
    fun `climax cards have no level`() {
        val climaxes = repo.snapshot.cards.filter { it.cardType == CardType.CLIMAX }
        assertFalse(climaxes.isEmpty())
        assertTrue(climaxes.all { it.level == null })
    }

    @Test
    fun `trigger filter matches`() {
        val results = repo.search(SearchQuery(triggers = setOf(TriggerIcon.GATE)))
        assertFalse(results.isEmpty())
        assertTrue(results.all { it.trigger == TriggerIcon.GATE })
    }

    @Test
    fun `default printing belongs to the base card number`() {
        // SP 特典卡（如 -113）只有燙金刷版，不存在無字綴普卡，
        // 因此只要求以基礎卡號開頭且排序最短優先
        for (card in repo.snapshot.cards) {
            assertTrue(
                "${card.id} 的 defaultPrinting 是 ${card.defaultPrinting.id}",
                card.defaultPrinting.id.startsWith(card.id),
            )
            val shortest = card.printings.minOf { it.id.length }
            assertEquals(shortest, card.defaultPrinting.id.length)
        }
    }

    @Test
    fun `search results are sorted by level within a title`() {
        val results = repo.search(SearchQuery(titleCode = "BRD/W139"))
        val levels = results.map { it.level ?: 99 }
        assertEquals(levels.sorted(), levels)
    }
}
